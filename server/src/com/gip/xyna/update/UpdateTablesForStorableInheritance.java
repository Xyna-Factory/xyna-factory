/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.tools.JavaFileObject;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TableConfiguration;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.generation.InMemoryStorableClassLoader;
import com.gip.xyna.xprc.exceptions.XPRC_CompileError;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.compile.CompilationResult;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaMemoryObject;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xprc.xfractwfe.generation.compile.InMemoryCompilationSet.TargetKind;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.update.outdatedclasses_8_2_1_0.XMOMODSConfig;

public class UpdateTablesForStorableInheritance extends UpdateJustVersion {

  public UpdateTablesForStorableInheritance(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }
  

  protected void update() throws XynaException {
    Collection<XMOMODSConfig> configs;
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      configs = con.loadCollection(XMOMODSConfig.class);
    } finally {
      con.closeConnection();
    }
    
    Map<String, Map<Long, XMOMODSConfig>>  configMap = new HashMap<>();
    // alle revisions die irgendwelche storables enthalten
    Set<Long> allRevisions = new HashSet<Long>();
    for (XMOMODSConfig config : configs) {
      allRevisions.add(config.getRevision());
      String key = createKey(config.getXmomStorableFqXMLName(), config.getPath());
      Map<Long, XMOMODSConfig> subMap = configMap.get(key);
      if (subMap == null) {
        subMap = new HashMap<Long, XMOMODSConfig>();
        configMap.put(key, subMap);
      }
      subMap.put(config.getRevision(), config);
    }
    
    for (Long aRevision : allRevisions) {
      XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(aRevision);
      // alle root typen mit typename updaten
      UpdateRootStorableStructureVisitor urssv = new UpdateRootStorableStructureVisitor();
      // alle ableitungen aus ihren alten Tabellen auslesen, typename setzen und in rootParent-Table speichern
      UpdateExtensionsStorableStructureVisitor uessv = new UpdateExtensionsStorableStructureVisitor(configMap);
      for (XMOMStorableStructureInformation aInfo : cache.getAllStorableStructureInformation()) {
        aInfo.traverse(urssv);
        aInfo.traverse(uessv);
      }
    }
  }
  
  
  public static String createKey(String xmomStorableFqXMLName, String path) {
    return xmomStorableFqXMLName + "#" + path;
  }
  
  private static class UpdateRootStorableStructureVisitor implements StorableStructureVisitor {
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (!current.hasSuper()) {
        updateTypeName(current);
      }
    }

    private void updateTypeName(StorableStructureInformation current) {
      ODS ods = ODSImpl.getInstance();
      Set<ODSConnectionType> conTypes = getRelevantConnectionTypes(current);
      if (conTypes != null) {
        for (ODSConnectionType aConTyp : conTypes) {
          ODSConnection con = ods.openConnection(aConTyp);
          try {
            StorableColumnInformation typenameCol = current.getColInfoByVarType(VarType.TYPENAME);
            if (typenameCol == null) {
              // in case of synthetic storables
              return;
            }
            String statement = new StringBuilder()
                            .append("UPDATE ")
                            .append(current.getTableName())
                            .append(" SET ")
                            .append(typenameCol.getColumnName())
                            .append(" = ? WHERE ")
                            .append(typenameCol.getColumnName())
                            .append(" IS NULL").toString();
            PreparedCommand pc = con.prepareCommand(new Command(statement, current.getTableName()));
            Parameter params;
            if (GenerationBase.isReservedServerObjectByFqOriginalName(current.getFqXmlName())) {
              try {
                params = new Parameter(GenerationBase.transformNameForJava(current.getFqXmlName()));
              } catch (XPRC_InvalidPackageNameException e) {
                logger.warn("Failed to update typename for " + current.getFqXmlName());
                params = new Parameter(current.getFqXmlName());
              }
            } else {
              params = new Parameter(current.getFqXmlName());
            }
            con.executeDML(pc, params);
            con.commit();
          } catch (PersistenceLayerException e) {
            //throw new RuntimeException("Error updating storable data", e);
            logger.warn("Error updating storable data", e);
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              logger.warn("Failed to close connection",e);
            }
          }
        }
      }
    }


    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
  }
  
  
  private static class UpdateExtensionsStorableStructureVisitor implements StorableStructureVisitor {
    
    private XMOMStorableStructureInformation root;
    private final Map<String, Map<Long, XMOMODSConfig>> configs;
    private final Stack<StorableColumnInformation> columnStack;
    
    private UpdateExtensionsStorableStructureVisitor(Map<String, Map<Long, XMOMODSConfig>> configs) {
      columnStack = new Stack<>();
      this.configs = configs;
    }
    
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (columnLink == null) {
        root = (XMOMStorableStructureInformation) current;
      } else {
        columnStack.push(columnLink);
      }
      if (current.hasSuper()) {
        migrateToParentTable(columnLink, current);
      }
    }
    
    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (columnLink != null) {
        columnStack.pop();
      }
    }

    private void migrateToParentTable(StorableColumnInformation columnLink, StorableStructureInformation currentt) {
      // always use declared type
      StorableStructureInformation current;
      if (columnLink == null) {
        current = currentt;
      } else {
       current = columnLink.getStorableVariableInformation();
      }
      
      JavaSourceFromString source = current.getStorableSource();
      String sourcecode = source.getCode();
      
      List<String> configKeys = new ArrayList<>();
      if (columnStack.size() <= 0) {
        configKeys.add(createKey((XMOMStorableStructureInformation) current, columnStack));
      } else {
        configKeys.add(createKey((XMOMStorableStructureInformation) root.getSuperRootStorableInformation(), columnStack));
        for (StorableStructureInformation aSub : root.getSuperRootStorableInformation().getSubEntriesRecursivly()) {
          configKeys.add(createKey((XMOMStorableStructureInformation) aSub, columnStack));  
        }
      }
      
      odsConfig: for (String configKey : configKeys) {
        String modifiedSource = sourcecode;
        if (current.hasSuper()) {
          // reset TABLENAME and insert Persistable-Annotation
          String annotation = 
          "@com.gip.xyna.xnwh.persistence.Persistable(tableName = " + current.getStorableClass().getName() + ".TABLE_NAME, primaryKey = " + current.getSuperRootStorableInformation().getStorableClass().getName() +".COLUMN_PK)";
        
          modifiedSource = sourcecode.replaceFirst(Constants.LINE_SEPARATOR + "public class ", Constants.LINE_SEPARATOR + annotation + Constants.LINE_SEPARATOR + "public class ");  
        }
        
        Map<Long, XMOMODSConfig> subMap = configs.get(configKey);
        if (subMap == null) {
          continue;
        }
        XMOMODSConfig config = subMap.get(current.getDefiningRevision());
        if (config == null) {
          continue;
        }
        modifiedSource = modifiedSource.replaceFirst("public static final String TABLE_NAME = \"[a-zA-Z0-9]+\"",
                                             "public static final String TABLE_NAME = \"" + config.getOdsName() + "\"");
        
        // remove readOut of typename
        modifiedSource = modifiedSource.replaceFirst("result\\.typename = rs\\.getString\\([a-zA-Z0-9_\\.]+\\);\\s*\n.*\n.*\n.*\n", "result.typename = \"" + current.getFqXmlName()+"\";\n");
        
        // compile
        InMemoryStorableClassLoader newCL = compile(root, new JavaSourceFromString(current.getFqClassNameForStorable(), modifiedSource));
        Class<? extends Storable<?>> newClass;
        Storable<?> instance;
        try {
          newClass = (Class<? extends Storable<?>>) newCL.loadClass(newCL.getStorableClassName());
          instance = newClass.getConstructor().newInstance();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        
        ODS ods = ODSImpl.getInstance();
        for (ODSConnectionType aConTyp : getRelevantConnectionTypes(current)) {
          // read
          ODSConnection con = ods.openConnection(aConTyp);
          try {
            FactoryWarehouseCursor<? extends Storable<?>> cursor = con.getCursor("SELECT * FROM " + instance.getTableName(), new Parameter(), instance.getReader(), 10);
            List<? extends Storable<?>> someInstances = cursor.getRemainingCacheOrNextIfEmpty();
            while (someInstances.size() > 0) {
              for (Storable<?> aInstance : someInstances) {
                // transform
                Storable<?> conversion = convert(aInstance, current);
                // persist
                con.persistObject(conversion);
              }
              con.commit();
              someInstances = cursor.getRemainingCacheOrNextIfEmpty();
            }
          } catch (PersistenceLayerException e) {
            // we run with extended tables vs base class tables at times, suppress those errors
            continue odsConfig;
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              logger.warn("Failed to close connection",e);
            }
          }
        }
      }
    }
    
    
    private String createKey(XMOMStorableStructureInformation root, Stack<StorableColumnInformation> columnStack) {
      XMOMStorableStructureInformation currentRoot = root;
      StringBuilder sb = new StringBuilder();
      StorableColumnInformation previous = null;
      for (Iterator<StorableColumnInformation> iterator = columnStack.iterator(); iterator.hasNext();) {
        StorableColumnInformation storableColumnInformation = iterator.next();
        if (previous == null ||
            previous != storableColumnInformation) { // duplicate entries are created from storable hierarchies (same link)
          sb.append(storableColumnInformation.getVariableName());
          if (iterator.hasNext()) {
            sb.append(".");
          }
        }
        previous = storableColumnInformation;
        if (storableColumnInformation.getStorableVariableType() == StorableVariableType.REFERENCE) {
          currentRoot = (XMOMStorableStructureInformation) storableColumnInformation.getStorableVariableInformation();
          sb = new StringBuilder();
        }
      }
      if (sb.length() > 0) {
        if (sb.charAt(sb.length() - 1) == '.') {
          sb.deleteCharAt(sb.length() - 1);
        }
      }
      return XMOMPersistenceManagement.createKey(currentRoot.getFqXmlName(), sb.toString());
    }


    private Storable<?> convert(Storable<?> aInstance, StorableStructureInformation current) {
      try {
        Storable<?> conversion = current.getStorableClass().getConstructor().newInstance();
        for (StorableColumnInformation column : current.getAllRelevantStorableColumnsRecursivly()) {
          conversion.setValueByColumnName(column.getColumnName(), aInstance.getValueByColString(column.getColumnName()));
        }
        return conversion;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    

    private InMemoryStorableClassLoader compile(XMOMStorableStructureInformation root, JavaSourceFromString jsfs) {
      InMemoryCompilationSet set = new InMemoryCompilationSet(false, false, false, TargetKind.MEMORY);
      
      for (JavaSourceFromString source : root.getStorableSourceRecursivly()) {
        if (source.getFqClassName().equals(jsfs.getFqClassName())) {
          set.addToCompile(jsfs);
        } else {
          set.addToCompile(source);
        }
      }
      
      if (set.size() > 0) {
        Map<Long, Map<String, InMemoryStorableClassLoader>> cls = new HashMap<>(); 
        
        CompilationResult result;
        try {
          result = set.compile();
          
          for (JavaFileObject jfo : result.getFiles()) {
            JavaMemoryObject jmo = (JavaMemoryObject) jfo;
            
            String rootObjectName = jmo.getFqClassName();
            if (rootObjectName.contains("$")) {
              rootObjectName = rootObjectName.substring(0, rootObjectName.indexOf('$'));
            }
            
            Map<String, InMemoryStorableClassLoader> subMap = cls.get(jmo.getRevision());
            if (subMap == null) {
              subMap = new HashMap<>();
              cls.put(jmo.getRevision(), subMap);
            }
            
            InMemoryStorableClassLoader pacl;
            if (subMap.containsKey(rootObjectName)) {
              pacl = subMap.get(rootObjectName);
            } else {
              pacl = new InMemoryStorableClassLoader(InMemoryStorableClassLoader.class.getClassLoader(), rootObjectName);
              pacl.setRootXMOMStorable(root);
              subMap.put(rootObjectName, pacl);
            }
            
            pacl.setBytecode(jmo.getFqClassName(), jmo.getClassBytes());
          }
          return cls.get(jsfs.getRevision()).get(jsfs.getFqClassName());
        } catch (XPRC_CompileError e) {
          throw new RuntimeException(e);
        }
      } else {
        return null;
      }
    }
    

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
  }
  
  private static Map<String, Set<ODSConnectionType>> configs = null;
  
  private static Set<ODSConnectionType> getRelevantConnectionTypes(StorableStructureInformation current) {
    if (configs == null) {
      configs = new HashMap<String, Set<ODSConnectionType>>();
      ODS ods = ODSImpl.getInstance();
      TableConfiguration[] tcs = ods.getTableConfigurations();  
      for (TableConfiguration tc : tcs) {
        Set<ODSConnectionType> conTypes = configs.get(tc.getTableName());
        if (conTypes == null) {
          conTypes = new HashSet<ODSConnectionType>();
          configs.put(tc.getTable(), conTypes);  
        }
        for (PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances()) {
          if (plib != null &&
              plib.getPersistenceLayerInstanceID() == tc.getPersistenceLayerInstanceID()) {
            conTypes.add(plib.getConnectionTypeEnum());
            break;
          }
        }
      }
    }
    return configs.get(current.getTableName());
  }
  
  
}
