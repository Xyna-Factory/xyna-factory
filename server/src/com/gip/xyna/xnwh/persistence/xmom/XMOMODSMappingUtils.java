/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.xmom.PathBuilder.ColumnElement;
import com.gip.xyna.xnwh.persistence.xmom.PathBuilder.ModelledTypeElement;
import com.gip.xyna.xnwh.persistence.xmom.PathBuilder.TypeElement;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceInformation;

public class XMOMODSMappingUtils {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XMOMODSMappingUtils.class);
  
  public static final String SAME_ACCESS_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_FQXMLNAME + " = ? AND " 
                                                                                                         + XMOMODSMapping.COL_PATH  + " = ?";
  public static final String TABLENAME_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_TABLENAME + " LIKE ? AND "
                                                                                                       + XMOMODSMapping.COL_COLUMNNAME + " IS NULL";
  public static final String COLUMNNAME_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_TABLENAME + " = ? AND "
                                                                                                        + XMOMODSMapping.COL_COLUMNNAME + " LIKE ?";
  public static final String SAME_REVISION_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_REVISION + " = ?";
  public static final String TYPE_AND_REVISION_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_FQXMLNAME + " = ? AND "
                                                                                                               + XMOMODSMapping.COL_REVISION + " = ?";
  private final static String ROOTNAME_REVISION_AND_FQPATH_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_FQXMLNAME + " = ? AND "
                                                                                                                           + XMOMODSMapping.COL_REVISION + " = ? AND "
                                                                                                                           + XMOMODSMapping.COL_FQPATH + " = ?";
  private final static String ALL_BY_REVISION_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_REVISION + " = ?";
  public static final String ALL_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME;
  private final static String ALL_ROOTS_BY_REVISION_QUERY = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_REVISION + " = ? AND " 
                                                                                                                    + XMOMODSMapping.COL_COLUMNNAME + " IS NULL AND " 
                                                                                                                    + XMOMODSMapping.COL_PATH + " = ?";
  private final static String ALL_COLUMNS_FOR_TABLENAME = "SELECT * FROM " + XMOMODSMapping.TABLENAME + " WHERE " + XMOMODSMapping.COL_FQXMLNAME + " = ? AND "
                                                                                                                  + XMOMODSMapping.COL_TABLENAME + " = ? AND " 
                                                                                                                  + XMOMODSMapping.COL_COLUMNNAME + " IS NOT NULL";
  
  
  private static final PreparedQueryCache pqCache = new PreparedQueryCache();
  
  public static final XynaPropertyInt maximumTablenameLength =
                  new XynaPropertyInt("xnwh.persistence.xmom.maximumtablenamelength", 64)
                        .setDefaultDocumentation(DocumentationLanguage.EN,
                          "Maximum amount of characters a tablename may not exceed.");
  public static final XynaPropertyInt maximumColumnnameLength =
                  new XynaPropertyInt("xnwh.persistence.xmom.maximumcolumnnamelength", 64)
                        .setDefaultDocumentation(DocumentationLanguage.EN,
                          "Maximum amount of characters a columnname may not exceed.");
  
  private static final XynaPropertyBoolean columnCollisionOldNamingStyle = new XynaPropertyBoolean("xnwh.persistence.xmom.columncount.namingstyle.legacy", false);

   
  public static enum NameType {
    TABLE {

      public Comparator<XMOMODSMapping> identityComperator() {
        return new Comparator<XMOMODSMapping>() {

          public int compare(XMOMODSMapping o1, XMOMODSMapping o2) {
            String table1 = o1.getTablename() == null ? "" : o1.getTablename();
            String table2 = o2.getTablename() == null ? "" : o2.getTablename();
            return table1.compareTo(table2);
          }
          
        };
      }

      protected String getUniqueName(PathBuilder path) {
        ColumnElement lastColumnElement = path.getLastColumnElement();
        String tableName;
        if (lastColumnElement == null) {
          tableName = GenerationBase.getSimpleNameFromFQName(path.getLastTypeName()); 
        } else {
          String lastColumn = lastColumnElement.getColumn();
          TypeElement<?> te = path.getLastType(true, 0);
          if (te instanceof ModelledTypeElement) {
            DOM type = ((ModelledTypeElement) te).dom;
            while (type != null) {
              if (type.getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
                break;
              }
              type = type.getSuperClassGenerationObject();
            }
            if (type != null) {
              tableName = GenerationBase.getSimpleNameFromFQName(path.getLastTypeName());
            } else {
              tableName = lastColumn;
            }
          } else {
            tableName = lastColumn;
            if (lastColumnElement.isList()) {
              tableName = "list" + tableName;
            }
          }
        }
         
        //Tablename abschneiden, falls er länger ist als erlaubt
        if(tableName.length() > maximumTablenameLength.get() - 3) {
          tableName = tableName.substring(0, maximumTablenameLength.get() - 3); //Leave 3 characters space for a suffix
        }
        return tableName.toLowerCase();
      }
      
      
      protected void appendAdditionalParam(Parameter params, PathBuilder path) {
      }
      
      protected Parameter createOdsParams(ODSRegistrationParameter params) {
        return new Parameter(params.getOdsName());
      }

      protected String getRelevantField(XMOMODSMapping mapping) {
        return mapping.getTablename();
      }
      
      protected PreparedQuery<XMOMODSMapping> getIdentifierQuerySQL(ODSConnection con)
                      throws PersistenceLayerException {
        return pqCache.getQueryFromCache(TABLENAME_QUERY, con, XMOMODSMapping.reader);
      }

    },
    
    COLUMN {

      public Comparator<XMOMODSMapping> identityComperator() {
        return new Comparator<XMOMODSMapping>() {
          
          private final Comparator<XMOMODSMapping> tableCompare = TABLE.identityComperator();

          public int compare(XMOMODSMapping o1, XMOMODSMapping o2) {
            int tableComparison = tableCompare.compare(o1, o2);
            if (tableComparison == 0) {
              if (o1.getColumnname() == null || o2.getColumnname() == null) {
                if (o1.getColumnname() == null && o2.getColumnname() == null) {
                  return 0;
                } else {
                  return -1;
                }
              }
              return o1.getColumnname().compareTo(o2.getColumnname());
            } else {
              return tableComparison;
            }
          }
          
        };
      }

      protected String getUniqueName(PathBuilder path) {
        String columnName = path.getLastColumnElement().getColumn();
        
        if(columnName.length() > maximumColumnnameLength.get() - 3) {
          columnName = columnName.substring(0, maximumColumnnameLength.get() - 3); //Leave 3 characters space for a suffix
        }
        return columnName.toLowerCase();
      }
      
      protected void appendAdditionalParam(Parameter params, PathBuilder path) {
        params.add(path.getLastType(false, 0).getTableName()); 
      }
      
      protected Parameter createOdsParams(ODSRegistrationParameter params) {
        return new Parameter(params.getTableName(), params.getOdsName());
      }

      protected String getRelevantField(XMOMODSMapping mapping) {
        return mapping.getColumnname();
      }

      protected PreparedQuery<XMOMODSMapping> getIdentifierQuerySQL(ODSConnection con)
                      throws PersistenceLayerException {
        return pqCache.getQueryFromCache(COLUMNNAME_QUERY, con, XMOMODSMapping.reader);
      }
      
    };

    public abstract Comparator<XMOMODSMapping> identityComperator();

    protected abstract String getUniqueName(PathBuilder path);

    protected abstract void appendAdditionalParam(Parameter params, PathBuilder path);

    protected abstract Parameter createOdsParams(ODSRegistrationParameter params);
    
    protected abstract String getRelevantField(XMOMODSMapping mapping);

    protected abstract PreparedQuery<XMOMODSMapping> getIdentifierQuerySQL(ODSConnection con) throws PersistenceLayerException;
    
  }
  
  
  
  public static String findUniqueName(PathBuilder path, NameType nameType) throws PersistenceLayerException {
    List<XMOMODSMapping> previousMapping = findSameTypes(path, nameType);
    if (previousMapping.size() <= 0) {
      String uniqueName = createUniqueName(path, nameType);
      createMapping(path, nameType, uniqueName);
      return uniqueName;
    } else {
      for (XMOMODSMapping mapping : previousMapping) {
        if (mapping.getFqpath().equals(path.getFqPath())) {
          if (nameType == NameType.TABLE) {
            // cache table name in path
            TypeElement<?> te = path.getLastType(false, 0);
            te.setTableName(nameType.getRelevantField(mapping));
          }
          return nameType.getRelevantField(mapping);
        }
      }
      String uniqueName = createUniqueName(path, nameType);
      createMapping(path, nameType, uniqueName);
      return uniqueName;
    }
  }


  // Suche nach Eintrag mit gleichem fqName & unqualifiziertem Pfad
  public static List<XMOMODSMapping> findSameTypes(PathBuilder path, NameType type) throws PersistenceLayerException {
    TypeElement<?> te = path.getLastPathRoot();
    String fqTypeName = te.getOrignalFqName();
    Long revision = te.getRevision();
    String simplePath = path.getPath();
    if (type == NameType.TABLE &&
        simplePath.isEmpty() &&
        te instanceof ModelledTypeElement) {
      // inherit name from root
      DOM superRoot = getSuperRoot(((ModelledTypeElement)te).dom);
      fqTypeName = superRoot.getOriginalFqName();
      revision = superRoot.getRevision();
    }
    
    XMOMPersistenceManagement persistenceMgmt = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Parameter params = new Parameter(fqTypeName, simplePath);
      List<XMOMODSMapping> result = executeQuery(con, SAME_ACCESS_QUERY, params);
      if (result.size() > 0) {
        List<XMOMODSMapping> fqSubResult = new ArrayList<>();
        for (XMOMODSMapping unqualifiedResult : result) {
          if (unqualifiedResult.getFqpath().equals(path.getFqPath())) {
            fqSubResult.add(unqualifiedResult);
          }
        }
        for (XMOMODSMapping mapping : fqSubResult) {
          if (revision != null && 
              revision.equals(mapping.getRevision())) {
            return Collections.singletonList(mapping);
          }
        }
        Set<XMOMODSMapping> dbIdentitySet = new TreeSet<XMOMODSMapping>(type.identityComperator());
        dbIdentitySet.addAll(fqSubResult);
        if (dbIdentitySet.size() > 1) {
          // different revisions have different tables => return null to trigger new name generation
          return Collections.emptyList();
        } else {
          // store with own revision
          if (fqSubResult.size() > 0) {
            XMOMODSMapping mapping = fqSubResult.iterator().next();
            mapping.setId(persistenceMgmt.genId());
            mapping.setRevision(revision);
            con.persistObject(mapping);
            con.commit();
          }
          return fqSubResult;
        }
      } else {
        return Collections.emptyList(); 
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.info("Error while closing connection.", e);
      }
    }
  }
  
  
  private static String createUniqueName(PathBuilder path, NameType nameType) throws PersistenceLayerException {
    TypeElement<?> te = path.getLastType(false, 0);
    if (nameType == NameType.TABLE) {
      if (te.getTableName() != null) {
        // return cached table name from path
        return te.getTableName();
      }
    }
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      DOM lastRoot = ((ModelledTypeElement)path.path.get(path.getIndexOfLastRoot())).dom;
      PreparedQuery<XMOMODSMapping> pq = nameType.getIdentifierQuerySQL(con);
      String simpleName = nameType.getUniqueName(path);
      Parameter params = new Parameter();
      nameType.appendAdditionalParam(params, path);
      params.add(simpleName+ "%");
      List<XMOMODSMapping> result = con.query(pq, params, 100);
      Set<String> takenNames = calculateTakenNames(nameType, result, lastRoot);
      for (XMOMODSMapping mapping : result) {
        takenNames.add(nameType.getRelevantField(mapping));
      }
      checkForOwnFlattenedCollision(nameType, takenNames, path.getPath(), lastRoot);
      String newName = findFreeName(nameType, takenNames, simpleName);
      if (nameType == NameType.TABLE) {
        // cache table name in path
        te.setTableName(newName);
      }
      return newName;
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.info("Error while closing connection.", e);
      }
    }
  }

  
  private static void checkForOwnFlattenedCollision(NameType nameType, Set<String> takenNames, String pathString, DOM dom) {
    if (nameType == NameType.COLUMN) {
      String[] pathParts = pathString.split("\\.");
      if (pathParts.length >= 2 &&
          pathParts[pathParts.length - 1].equalsIgnoreCase(pathParts[pathParts.length - 2])) {
        String[] shortenedPathParts = Arrays.copyOfRange(pathParts, 0, pathParts.length - 1);
        String shortenedPath = String.join(".", shortenedPathParts);
        if (isFlat(dom.getPersistenceInformation(), shortenedPath)) {
          findFreeName(nameType, takenNames, pathParts[pathParts.length -1].toLowerCase());
        }
      }
    }
  }
  
  private static boolean isFlat(PersistenceInformation persInfo, String path) {
    String processedPath = path.replaceAll("\\[\\]", "");
    return isFlatRecursivly(persInfo, processedPath);
  }
  
  private static boolean isFlatRecursivly(PersistenceInformation persInfo, String path) {
    if (persInfo.getFlatExclusions().contains(path)) {
      return false;
    }
    if (persInfo.getFlattened().contains(path)) {
      return true;
    }
    String[] pathParts = path.split("\\.");
    if (pathParts.length > 2) {
      String[] shortenedPathParts = Arrays.copyOfRange(pathParts, 0, pathParts.length - 2);
      String shortenedPath = String.join(".", shortenedPathParts);
      return isFlatRecursivly(persInfo, shortenedPath);
    } else {
      return false;
    }
  }
  
  
  private static boolean isReference(PersistenceInformation persInfo, String path) {
    String processedPath = path.replaceAll("\\[\\]", "");
    return isReferenceRecursivly(persInfo, processedPath);
  }
  
  
  private static boolean isReferenceRecursivly(PersistenceInformation persInfo, String path) {
    if (persInfo.getReferences().contains(path)) {
      return true;
    }
    String[] pathParts = path.split("\\.");
    if (pathParts.length > 2) {
      String[] shortenedPathParts = Arrays.copyOfRange(pathParts, 0, pathParts.length - 2);
      String shortenedPath = String.join(".", shortenedPathParts);
      return isReferenceRecursivly(persInfo, shortenedPath);
    } else {
      return false;
    }
  }
  

  private static String findFreeName(NameType nameType, Set<String> takenNames, String simpleName) {
    if (takenNames.add(simpleName)) {
      return simpleName;
    }
    int count;
    int increment;
    if (nameType == NameType.TABLE || 
        !columnCollisionOldNamingStyle.get()) {
      count = 0;
      increment = 1;
    } else {
      count = 1;
      increment = 2;
    }
    while (true) {
      if (takenNames.add(simpleName + count)) {
        return simpleName + count;
      } else {
        count += increment;
      }
    }
  }

  
  private static Set<String> calculateTakenNames(NameType nameType, List<XMOMODSMapping> mappings, DOM dom) {
    Set<String> takenNames = new HashSet<String>();
    for (XMOMODSMapping mapping : mappings) {
      takenNames.add(nameType.getRelevantField(mapping));
      if (nameType == NameType.COLUMN) {
        checkForOwnFlattenedCollision(NameType.COLUMN, takenNames, mapping.getPath(), dom);
      }
    }
    return takenNames;
  }
  
  
  private static void createMapping(PathBuilder path, NameType nameType, String uniqueName) throws PersistenceLayerException {
    TypeElement<?> pathDefinition = path.getLastPathRoot();
    TypeElement<?> tableDefinition = path.getLastType(false, 0);
    XMOMPersistenceManagement persistenceMgmt = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    XMOMODSMapping mapping = new XMOMODSMapping();
    mapping.setId(persistenceMgmt.genId());
    mapping.setPath(path.getPath());
    mapping.setFqpath(path.getFqPath());
    mapping.setFqxmlname(pathDefinition.getOrignalFqName());
    mapping.setRevision(pathDefinition.getRevision());
    switch (nameType) {
      case TABLE :
        mapping.setTablename(uniqueName);
        mapping.setColumnname(null);
        break;
      case COLUMN :
        mapping.setTablename(tableDefinition.getTableName());
        mapping.setColumnname(uniqueName);
        break;
      default :
        new IllegalArgumentException("Unexpected NameType: " + nameType);
    }
    mapping.setUserdefined(false);
    mapping.setUserdefined(false);
    XMOMODSMappingUtils.storeMapping(mapping);
  }


  public static void storeMapping(XMOMODSMapping mapping) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    XMOMPersistenceManagement persistenceMgmt = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    try {
      persistenceMgmt.storeConfigAndSetPersistenceLayers(mapping, con);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.info("Error while closing connection.", e);
      }
    }
  }


  public static Set<String> discoverPaths(Set<DOM> domsThatCacheSubtypes, DOM dom, GenerationBaseCache parseAdditionalCache) {
    DOM currentRoot = getSuperRoot(dom);
    Set<String> paths = new HashSet<String>();
    PathBuilder pb = new PathBuilder();
    Set<GenerationBase> visited = new HashSet<GenerationBase>();
    pb.enter(dom, false, true);
    try {
      visited.add(currentRoot);
      discoverPathsRecursivly(domsThatCacheSubtypes, currentRoot, currentRoot.getPersistenceInformation(), pb, paths, visited, parseAdditionalCache);
    } finally {
      pb.exit();
    }
    return paths;
  }
  
  
  private static void discoverPathsRecursivly(Set<DOM> domsThatCacheSubtypes, DOM currentDom, PersistenceInformation persInfo, PathBuilder pb, Collection<String> fqPaths, Set<GenerationBase> visited, GenerationBaseCache parseAdditionalCache) {
    for (AVariable aVar : currentDom.getMemberVars()) {
      pb.enter(aVar.getVarName(), aVar.isList());
      if (aVar.isJavaBaseType()) {
        // enter and generate path
        fqPaths.add(pb.getFqPath());
      } else {
        //descend
        DOM rootDom = getSuperRoot((DOM) aVar.getDomOrExceptionObject());
        if (visited.add(rootDom)) {
          pb.enter(rootDom, isFlat(persInfo, pb.getPath()), isReference(persInfo, pb.getPath()));
          discoverPathsRecursivly(domsThatCacheSubtypes, rootDom, persInfo, pb, fqPaths, visited, parseAdditionalCache);
          pb.exit();
        }
      }
      pb.exit();
    }
    if (domsThatCacheSubtypes.add(currentDom)) {
      currentDom.setCacheSubTypes(true);  
    }
    for (GenerationBase subType : currentDom.getSubTypes(parseAdditionalCache)) {
      DOM subDom = (DOM) subType;
      if (visited.add(subDom)) {
        pb.exchangeType(subDom);
        if (subDom.isInheritedFromStorable()) {
          persInfo = subDom.getPersistenceInformation();
        }
        discoverPathsRecursivly(domsThatCacheSubtypes, subDom, persInfo, pb, fqPaths, visited, parseAdditionalCache);
      }
    }
  }
  
  public static DOM getSuperRoot(DOM dom) {
    DOM currentRoot = dom;
    while (currentRoot != null && currentRoot.hasSuperClassGenerationObject() &&
           !currentRoot.getSuperClassGenerationObject().isStorableEquivalent() ) {
      currentRoot = currentRoot.getSuperClassGenerationObject();
    }
    return currentRoot;
  }
  
  
  public static class DiscoveryResult {
    
    private String fqPath;
    private String path;
    private Stack<String> typeStack;
    
    private DiscoveryResult(String fqPath, String path, Stack<String> typeStack) {
      this.fqPath = fqPath;
      this.path = path;
      this.typeStack = typeStack;
    }
    
    public String getFqPath() {
      return fqPath;
    }
    
    public String getPath() {
      return path;
    }
    
    private Stack<String> getTypeStack() {
      return typeStack;
    }
    
    public String getType() {
      return typeStack.pop();
    }
    
  }
  
  
  public static Collection<DiscoveryResult> discoverFqPathsForPath(String fqXMLName, Long revision, String path, GenerationBaseCache parseAdditionalCache) {
    if (path == null || 
        path.isBlank()) {
      Stack<String> typeStack = new Stack<String>();
      typeStack.push(fqXMLName);
      return Collections.singleton(new DiscoveryResult("", "", typeStack));
    } else {
      DOM dom;
      try {
        dom = DOM.generateUncachedInstance(fqXMLName, true, revision);
      } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | 
               AssumedDeadlockException | XPRC_MDMDeploymentException e) {
        throw new RuntimeException(e);
      }
      return discoverFqPathsForPath(dom, path, parseAdditionalCache);
    }
  }
  
  
  public static Collection<DiscoveryResult> discoverFqPathsForPath(DOM dom, String path, GenerationBaseCache parseAdditionalCache) {
    if (path == null || 
        path.isBlank()) {
      Stack<String> typeStack = new Stack<String>();
      typeStack.push(dom.getOriginalFqName());
      return Collections.singleton(new DiscoveryResult("", "", typeStack));
    } else {
      PathBuilder pb = new PathBuilder();
      pb.enter(dom.getOriginalFqName(), dom.getRevision());
      String[] pathParts = path.split("\\.");
      Set<String> visitedNameRevisionPairs = new HashSet<>();
      return discoverFqPathsRecursivly(pathParts, 0, dom, pb, parseAdditionalCache, visitedNameRevisionPairs);
    }
  }
  
  
  private static Collection<DiscoveryResult> discoverFqPathsRecursivly(String[] parts, int index, DOM dom, PathBuilder pb, GenerationBaseCache parseAdditionalCache, Set<String> visitedNameRevisionPairs) {
    String visitationName = dom.getOriginalFqName() + "@" + dom.getRevision();
    if (!visitedNameRevisionPairs.add(visitationName)) {
      return new ArrayList<>();
    }
    Set<DOM> columnHolders = findAllTypesForColumn(dom, parts[index], parseAdditionalCache);
    Collection<DiscoveryResult> paths = new ArrayList<>();
    for (DOM columnHolder : columnHolders) {
      if (index > 0) {
        pb.enter(columnHolder.getOriginalFqName(), columnHolder.getRevision());
      }
      for (AVariable aVar : columnHolder.getMemberVars()) {
        if (aVar.getVarName().equals(parts[index])) {
          pb.enter(parts[index], aVar.isList());
          if (index +1 < parts.length) {
            Collection<DiscoveryResult> newPaths = discoverFqPathsRecursivly(parts, index + 1, (DOM) aVar.getDomOrExceptionObject(), pb.clone(), parseAdditionalCache, visitedNameRevisionPairs);
            newPaths.forEach(p -> p.getTypeStack().push(columnHolder.getOriginalFqName()));
            paths.addAll(newPaths);  
          } else {
            if (!aVar.isJavaBaseType()) {
              Stack<String> typeStack = new Stack<String>();
              typeStack.push(columnHolder.getOriginalFqName());
              pb.enter(aVar.getDomOrExceptionObject().getOriginalFqName(), aVar.getDomOrExceptionObject().getRevision());
              paths.add(new DiscoveryResult(pb.getFqPath(), pb.getPath(), typeStack));
              pb.exit();
            } else if (aVar.isList()) { // primitive liste ist auch "complexer" 
              String listname = aVar.getJavaTypeEnum().getFqName();
              Stack<String> typeStack = new Stack<String>();
              typeStack.push(columnHolder.getOriginalFqName());
              pb.enter(listname, columnHolder.getRevision());
              paths.add(new DiscoveryResult(pb.getFqPath(), pb.getPath(), typeStack));
              pb.exit();
            } else { // auch primitive spalten discovern
              Stack<String> typeStack = new Stack<String>();
              paths.add(new DiscoveryResult(pb.getFqPath(), pb.getPath(), typeStack));
            }
          }
          pb.exit();          
        }
      }
      if (index > 0) {
        pb.exit();
      }
    }
    return paths;
  }
  
  
  private static Set<DOM> findAllTypesForColumn(DOM dom, String part, GenerationBaseCache parseAdditionalCache) {
    DOM current = dom;
    if (current == null) {
      return Collections.emptySet();
    }
    while (current.hasSuperClassGenerationObject() &&
           !current.getSuperClassGenerationObject().isStorableEquivalent()) {
      current = current.getSuperClassGenerationObject();
    }
    return findAllTypesForColumnRecursivly(current, part, parseAdditionalCache);
  }
  
  
  private static Set<DOM> findAllTypesForColumnRecursivly(DOM dom, String part, GenerationBaseCache parseAdditionalCache) {
    Set<DOM> types = new HashSet<DOM>();
    for (AVariable aVar : dom.getMemberVars()) {
      if (aVar.getVarName().contentEquals(part)) {
        types.add(dom);  
      }
    }
    for (GenerationBase sub : dom.getSubTypes(parseAdditionalCache, false)) {
      types.addAll(findAllTypesForColumnRecursivly((DOM) sub, part, parseAdditionalCache));
    }
    return types;
  }


  public static Collection<XMOMODSMapping> getAllMappingsForRevision(Long target) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return executeQuery(con, SAME_REVISION_QUERY, new Parameter(target));
    } finally {
      con.closeConnection();
    }
  }


  public static Collection<XMOMODSMapping> getAllMappingsForRootType(String name, Long revision) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return executeQuery(con, TYPE_AND_REVISION_QUERY, new Parameter(name, revision));
    } finally {
      con.closeConnection();
    }
  }
  
  
  public static XMOMODSMapping getByNameRevisionFqPath(String name, Long revision, String fqPath) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<XMOMODSMapping> result = executeQuery(con, ROOTNAME_REVISION_AND_FQPATH_QUERY, new Parameter(name, revision, fqPath));
      if (result.size() > 0) {
        return result.get(0);
      } else {
        return null;
      }
    } finally {
      con.closeConnection();
    }
  }


  public static void removeForRevisionNameAndFqPath(Long revision, String name, String fqPath) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<XMOMODSMapping> result = executeQuery(con, ROOTNAME_REVISION_AND_FQPATH_QUERY, new Parameter(name, revision, fqPath));
      if (result.size() > 0) {
        con.delete(result);
        con.commit();
      }
    } finally {
      con.closeConnection();
    }
  }


  public static void removeAllForRevision(long revision) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      removeAllForRevision(con, revision);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  public static void removeAllForRevision(ODSConnection con, long revision) throws PersistenceLayerException {
    Collection<XMOMODSMapping> toDelete = executeQuery(con, ALL_BY_REVISION_QUERY, new Parameter(revision));
    con.delete(toDelete);
  }
  

  private static List<XMOMODSMapping> executeQuery(ODSConnection con, String query, Parameter params) throws PersistenceLayerException {
    try {
      PreparedQuery<XMOMODSMapping> pq = pqCache.getQueryFromCache(query, con, XMOMODSMapping.reader);
      return con.query(pq, params, -1);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      pqCache.clear();
      PreparedQuery<XMOMODSMapping> pq = pqCache.getQueryFromCache(query, con, XMOMODSMapping.reader);
      return con.query(pq, params, -1);
    }
  }


  public static FactoryWarehouseCursor<XMOMODSMapping> getCursorOverAll(ODSConnection con) throws PersistenceLayerException {
    final int CACHE_SIZE = 100;
    return con.getCursor(ALL_QUERY, new Parameter(), XMOMODSMapping.reader, CACHE_SIZE, pqCache);
  }
  
  
  public static Collection<XMOMODSMapping> getAllRootObjectsForRevision(ODSConnection con, Long target) throws PersistenceLayerException {
    return executeQuery(con, ALL_ROOTS_BY_REVISION_QUERY, new Parameter(target, ""));
  }
  
  private static final Pattern SINGLE_PATH_PART = Pattern.compile("[a-zA-Z0-9_]+");
  
  public static Collection<XMOMODSMapping> getAllColumnsForTable(ODSConnection con, XMOMODSMapping table, String oldTableName) throws PersistenceLayerException {
    Collection<XMOMODSMapping> columns = executeQuery(con, ALL_COLUMNS_FOR_TABLENAME, new Parameter(table.getFqxmlname(), oldTableName));
    Iterator<XMOMODSMapping>  iter = columns.iterator();
    while (iter.hasNext()) {
      XMOMODSMapping current = iter.next();
      if (current.isTableConfig()) {
        iter.remove();
        continue;
      }
      String pathPart;
      if (current.getPath().startsWith(table.getPath())) {
        pathPart = current.getPath().substring(table.getPath().length()+1);
      } else {
        iter.remove();
        continue;
      }
      Matcher matcher = SINGLE_PATH_PART.matcher(pathPart);
      if (!matcher.matches()) {
        iter.remove();
        continue;
      }
    }
    return columns;
  }
  
}

