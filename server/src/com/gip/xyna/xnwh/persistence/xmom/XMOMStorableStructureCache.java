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
package com.gip.xyna.xnwh.persistence.xmom;



import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.exception.MultipleExceptionHandler;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils.NameType;
import com.gip.xyna.xnwh.persistence.xmom.generation.InMemoryStorableClassLoader;
import com.gip.xyna.xnwh.persistence.xmom.generation.StorableCodeBuilder;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceInformation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.MaxLengthRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restrictions;



public class XMOMStorableStructureCache {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XMOMStorableStructureCache.class);
  
  private static ConcurrentMap<Long, XMOMStorableStructureCache> instances =
      new ConcurrentHashMap<Long, XMOMStorableStructureCache>();

  /* Datatype fqClassName as key*/
  private Map<String, XMOMStorableStructureInformation> storableStructureInformation =
      new HashMap<String, XMOMStorableStructureCache.XMOMStorableStructureInformation>();


  private XMOMStorableStructureCache(Long revision) {
  }


  public static XMOMStorableStructureCache getInstance(Long revision) {
    if (instances.get(revision) == null) {
      instances.putIfAbsent(revision, new XMOMStorableStructureCache(revision));
    }
    return instances.get(revision);
  }


  public XMOMStorableStructureInformation register(DOM xmomStorable, GenerationBaseCache gbc) {
    
    List<DOM> previousDoms = new ArrayList<DOM>();
    StructureGenerationContext sgc = new StructureGenerationContext();
    sgc.gbc = gbc;
    sgc.pb.enter(xmomStorable, false, true);
    StorableStructureIdentifier id = getOrCreateStructure(xmomStorable, xmomStorable, null, null, null, previousDoms, false, false, sgc);
    try {
      fillStructureRecursively(id, xmomStorable, xmomStorable, "", "", null, null, null, previousDoms, false, false, sgc, HierachyTraversal.INIT);
    } catch (PersistenceLayerException e) {
      // TODO or rethrow?
      logger.warn("Failed to generate Storable for hierarchy of " + xmomStorable.getOriginalFqName(), e);
    }
    sgc.pb.exit();
    
    XMOMStorableStructureInformation info = (XMOMStorableStructureInformation) id.getInfo(); 
    if (info.getColInfosByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER).size() > 1) {
      throw new RuntimeException("Too many unique identifiers in type " 
        + info.getFqXmlName() + " [" + info.getColInfosByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER) + "]");
    } else {
      StorableCodeBuilder scb = new StorableCodeBuilder(info);
      try {
        scb.generateStorableCode();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to generate StorableCode for hierarchy of " + xmomStorable.getOriginalFqName(), e);
      }
        
      return info;
    }
  }


   private StorableStructureIdentifier getOrCreateStructure(DOM xmomStorableBase, DOM currentDom,
                                                            StorableStructureIdentifier possessingStorable, StorableColumnInformation possessingColumn, AVariable possessingVar,
                                                            List<DOM> previousDoms, boolean currentStorableIsList, boolean currentDOMisDefinedAsFlatInParent,
                                                            StructureGenerationContext sgc) {
     StorableStructureInformation newEntry;
     boolean isFlattened = currentDOMisDefinedAsFlatInParent && !currentStorableIsList;
     if (isFlattened) {
       possessingStorable.getInfo().traversalState = TraversalState.INIT;
       return possessingStorable;
     } else {
       if (possessingStorable == null) {
         Map<String, XMOMStorableStructureInformation> correspondingStorableStructureInformation = getInstance(xmomStorableBase.getRevision()).storableStructureInformation;
         Long currentId = DeploymentManagement.getInstance().getLatestDeploymentId();
         if (correspondingStorableStructureInformation.containsKey(xmomStorableBase.getFqClassName()) && 
             currentId.equals(correspondingStorableStructureInformation.get(xmomStorableBase.getFqClassName()).deploymentId)) {
           XMOMStorableStructureInformation info = correspondingStorableStructureInformation.get(currentDom.getFqClassName());
           return new ReferenceStorableStructureIdentifier(info.getDefiningRevision(), info.getFqXmlName(), info.getFqClassNameForDatatype());
         } else {
           newEntry = new XMOMStorableStructureInformation(xmomStorableBase, sgc);
           correspondingStorableStructureInformation.put(xmomStorableBase.getFqClassName(), (XMOMStorableStructureInformation) newEntry);
         }
       } else {
         newEntry = new StorableStructureInformation(xmomStorableBase, currentDom, possessingVar, currentStorableIsList, sgc);
         newEntry.parent = possessingColumn;
       }
     }
     
     StorableStructureIdentifier identifier;
     if (newEntry instanceof XMOMStorableStructureInformation) {
       identifier = new ReferenceStorableStructureIdentifier(newEntry.getDefiningRevision(), newEntry.getFqXmlName(), newEntry.getFqClassNameForDatatype());
     } else {
       identifier = new DirectStorableStructureIdentifier(newEntry);
     }
     
     String fqPath = sgc.pb.getFqPath();
     if (identifier instanceof ReferenceStorableStructureIdentifier) {
       Map<DOM, StorableStructureIdentifier> subSubMap = new HashMap<>();
       subSubMap.put(currentDom, identifier);
       Map<String, Map<DOM, StorableStructureIdentifier>> subMap = new HashMap<>();
       subMap.put(fqPath, subSubMap);
       sgc.visitedExtensionHierarchy.put(xmomStorableBase, subMap);
       if (identifier.getInfo() != null && identifier.getInfo().traversalState == TraversalState.FINISHED) {
         long currentId = DeploymentManagement.getInstance().getLatestDeploymentId();
         if (((XMOMStorableStructureInformation)identifier.getInfo()).deploymentId != currentId) {
           identifier.getInfo().traversalState = TraversalState.INIT; 
         }
       }
       return identifier;
     } else {
       Map<DOM, Map<String, Map<DOM, StorableStructureIdentifier>>> visitedExtensionHierarchy = sgc.visitedExtensionHierarchy;
       if (visitedExtensionHierarchy.containsKey(xmomStorableBase)) {
         Map<String, Map<DOM, StorableStructureIdentifier>> subMap = visitedExtensionHierarchy.get(xmomStorableBase);
         if (subMap.containsKey(fqPath)) {
           Map<DOM, StorableStructureIdentifier> subSubMap = subMap.get(fqPath);
           if (subSubMap.containsKey(currentDom)) {
             return subSubMap.get(currentDom);
           } else {
             subSubMap.put(currentDom, identifier);
             return identifier;
           }
         } else {
           Map<DOM, StorableStructureIdentifier> subSubMap = new HashMap<>();
           subSubMap.put(currentDom, identifier);
           subMap.put(fqPath, subSubMap);
           return identifier;
         }
       } else {
         Map<DOM, StorableStructureIdentifier> subSubMap = new HashMap<>();
         subSubMap.put(currentDom, identifier);
         Map<String, Map<DOM, StorableStructureIdentifier>> subMap = new HashMap<>();
         subMap.put(fqPath, subSubMap);
         visitedExtensionHierarchy.put(currentDom, subMap);
         return identifier;
       }
     }
   }
  
  private static final XynaPropertyBoolean referenceColNamesOldNamingStyle = new XynaPropertyBoolean("xnwh.persistence.xmom.references.namingstyle.legacy", false);
  
  
  private static enum HierachyTraversal {
    INIT, NO_SUBS, FULL_HIERARCHY, ABORT; 
  }
  
  
  private static enum TraversalState {
    INIT, STARTED, FINISHED, ABORTED;
  }
  

  private void fillStructureRecursively(StorableStructureIdentifier structure, DOM xmomStorableBase, DOM currentDom,
                                        String path, String pathRelativeToParentDatatype, 
                                        StorableStructureIdentifier possessingStorable, StorableColumnInformation possessingColumn, AVariable possessingVar,
                                        List<DOM> previousDoms, boolean currentStorableIsList, boolean currentDOMisDefinedAsFlatInParent,
                                        StructureGenerationContext sgc, HierachyTraversal traversal) throws PersistenceLayerException {
    
    StorableStructureInformation newEntry = structure.getInfo();
    boolean isFlattened = currentDOMisDefinedAsFlatInParent && !currentStorableIsList;
    HierachyTraversal traversalForSuper = traversal;
    switch (newEntry.traversalState) {
      case FINISHED :
        return;
      case STARTED :
        return;
      case INIT :
        if (traversal == HierachyTraversal.INIT) {
          if (possessingStorable == null &&
              currentDom.getSuperClassGenerationObject() != null &&
              !currentDom.getSuperClassGenerationObject().isStorableEquivalent()) {
            traversal = HierachyTraversal.ABORT;
            newEntry.traversalState = TraversalState.ABORTED;
            traversalForSuper = HierachyTraversal.FULL_HIERARCHY;
          } else {
            newEntry.traversalState = TraversalState.STARTED;
            traversalForSuper = HierachyTraversal.NO_SUBS;
          }
        } else {
          newEntry.traversalState = TraversalState.STARTED;
        }
        break;
      case ABORTED :
      default :
        newEntry.traversalState = TraversalState.STARTED;
        break;
    }
    
    HierachyTraversal traversalForSubs = HierachyTraversal.FULL_HIERARCHY;
    //structure-graph vorläufig befüllen (für supertypen)
    if (!isFlattened) {
      // basetype
      DOM relevantSuperDom = findRelevantSuperDom(currentDom);
      if (relevantSuperDom != null) {
        DOM newXmomStorableBase = possessingStorable == null ? relevantSuperDom : xmomStorableBase;
        sgc.pb.exchangeType(relevantSuperDom);
        StorableStructureIdentifier superEntry = getOrCreateStructure(newXmomStorableBase, relevantSuperDom,
                                                                 possessingStorable, possessingColumn,
                                                                 possessingVar, previousDoms, currentStorableIsList, currentDOMisDefinedAsFlatInParent, sgc);
        
        fillStructureRecursively(superEntry, possessingStorable == null ? relevantSuperDom : xmomStorableBase,
                        relevantSuperDom, path, pathRelativeToParentDatatype, possessingStorable, possessingColumn,
                        possessingVar, previousDoms, currentStorableIsList, currentDOMisDefinedAsFlatInParent, sgc, traversalForSuper);
        sgc.pb.exchangeType(currentDom);
        newEntry.superEntry = superEntry;
        if (traversal == HierachyTraversal.ABORT) {
          // if descending and parents do ascend don't continue parsing
          return;
        }
      }
    }

   newEntry.isAbstract = currentDom.isAbstract(); 
   
   if (currentDom.hasSuperClassGenerationObject()) {
     Set<String> memVarNames = new HashSet<>();
     for (AVariable aVar : currentDom.getMemberVars()) {
       memVarNames.add(aVar.getVarName());
     }
     Set<String> superMemVarNames = new HashSet<>();
     for (AVariable aVar : currentDom.getSuperClassGenerationObject().getAllMemberVarsIncludingInherited()) {
       superMemVarNames.add(aVar.getVarName());
     }
     Set<String> rootPaths = extractRootPart(currentDom.getPersistenceInformation().getFlattened());
     validatePersistenceInfo(currentDom.getOriginalFqName(), "Flat", rootPaths, memVarNames, superMemVarNames);
     rootPaths = extractRootPart(currentDom.getPersistenceInformation().getReferences());
     validatePersistenceInfo(currentDom.getOriginalFqName(), "Reference", rootPaths, memVarNames, superMemVarNames);
     rootPaths = extractRootPart(currentDom.getPersistenceInformation().getTransients());
     validatePersistenceInfo(currentDom.getOriginalFqName(), "Transient", rootPaths, memVarNames, superMemVarNames);
     rootPaths = extractRootPart(currentDom.getPersistenceInformation().getConstraints());
     validatePersistenceInfo(currentDom.getOriginalFqName(), "Constraint", rootPaths, memVarNames, superMemVarNames);
   }
   
   List<AVariable> variables = currentDom.getMemberVars();
   if (isFlattened) {
     variables = currentDom.getAllMemberVarsIncludingInherited();
   } else if (currentDom.getSuperClassGenerationObject() != null &&
              currentDom.getSuperClassGenerationObject().getPersistenceInformation() != null &&
              !currentDom.getSuperClassGenerationObject().getPersistenceInformation().hasTableRepresentation()) {
     DOM nonRepresented = currentDom.getSuperClassGenerationObject();
     variables = new ArrayList<AVariable>(variables);
     do {
       variables.addAll(nonRepresented.getMemberVars());
       nonRepresented = nonRepresented.getSuperClassGenerationObject();
     } while (!nonRepresented.getPersistenceInformation().hasTableRepresentation());
   }
   // primitive felder
   for (AVariable aVar : variables) {
     String localPath = path;
     String localPathRelativeToParentDatatype = "";
     if (possessingStorable != null &&
         newEntry == possessingStorable.getInfo()) {
       //reingemerged wegen flatheit -> kein neuer datentyp
       localPathRelativeToParentDatatype = pathRelativeToParentDatatype;
     }
     if (possessingStorable != null) {
       localPath += ".";
     }
     if (localPathRelativeToParentDatatype.length() > 0) {
       localPathRelativeToParentDatatype += ".";
     }
     localPath += aVar.getVarName();
     localPathRelativeToParentDatatype += aVar.getVarName();

     if (isTransient(xmomStorableBase, aVar, localPath)) {
       continue;
     }

     
     if (aVar.isJavaBaseType() || aVar.getDomOrExceptionObject() instanceof ExceptionGeneration) {
       
       if (aVar.isJavaBaseType() && aVar.isList()) {
         // unten bei complexe Felder mit machen
       } else {
         StorableColumnInformation info = StorableColumnInformation.createXMOMStorableColumnInformation(aVar, newEntry, sgc);
         if (currentDOMisDefinedAsFlatInParent && !currentStorableIsList) {
           info.typeInformation = new HashSet<PersistenceTypeInformation>();
         }
         info.definedIn = VarDefinitionSite.DATATYPE;
         if (aVar.isJavaBaseType()) {
           info.definedIn = VarDefinitionSite.BOTH;
         }
         
         addPersistenceTypeInformation(newEntry.getParentXMOMStorableInformation(), localPath, info, aVar);
         
         info.simpleType = aVar.getJavaTypeEnum(); //null falls exception
         
         if (newEntry.columnInformation.containsKey(info.getColumnName())) {
           StorableColumnInformation column = newEntry.columnInformation.get(info.getColumnName());
           throw new RuntimeException("Duplicate variable name: " + aVar.getVarName() + "\nVariable from " + currentDom.getOriginalFqName() + " colliding with " + column.getParentStorableInfo().getFqClassNameForDatatype() + "." + (column.isFlattened() ? column.getPath() : column.getVariableName()));
         }
         newEntry.columnInformation.put(info.getColumnName(), info);
         if (currentDOMisDefinedAsFlatInParent) {
           info.path = localPathRelativeToParentDatatype;
         }
       }
     } else {
       //complex
       continue;
     }
   }
   
   boolean usesHistorization;
   if (possessingStorable == null) {
     if (newEntry.hasSuper()) {
       ((XMOMStorableStructureInformation) newEntry).usesHistorization = ((XMOMStorableStructureInformation) newEntry.getSuperRootStorableInformation()).usesHistorization;
     } else {
       if (possessingStorable == null) {
         boolean currentVersionFlagFound = false;
         boolean historizationStampFound = false;
         StorableColumnInformation colInfo = newEntry.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
         if (colInfo != null) {
           currentVersionFlagFound = true;
         }
         colInfo = newEntry.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
         if (colInfo != null) {
           historizationStampFound = true;
         }
         ((XMOMStorableStructureInformation) newEntry).usesHistorization = historizationStampFound && currentVersionFlagFound;
       }
     }
     usesHistorization = ((XMOMStorableStructureInformation) newEntry).usesHistorization;
   } else {
     usesHistorization = possessingStorable.getInfo().getParentXMOMStorableInformation().usesHistorization;
   }
   
   if (!isFlattened) {
     StorableColumnInformation pkInfo;
     if (path.length() == 0) {
       StorableColumnInformation definedPK = newEntry.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
       if (definedPK == null) {
         throw new RuntimeException("No UniqueIdentifier specified in " + currentDom.getOriginalFqName() + "!");
       }
       
       StorableColumnInformation localPK = newEntry.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
       if (localPK != null &&
           newEntry != newEntry.getSuperRootStorableInformation()) {
         throw new RuntimeException("UniqueIdentifier can not be redefined!");
       }
       
       if (definedPK.getPrimitiveType() == null) {
         throw new RuntimeException("primarykey (" + definedPK.getVariableName() + ") must be a java base type, but found: " + definedPK.getStorableVariableInformation().getFqXmlName());
       }
       
       // uidAcrossHistory erzeugen falls historisierung
       if (usesHistorization &&
           !newEntry.hasSuper()) {
         definedPK.type = VarType.DEFAULT;
         
         String uidName = sgc.createUniqueColNameFor("uidAcrossHistory");
         pkInfo = new StorableColumnInformation(null, uidName, null, newEntry);
         pkInfo.definedIn = VarDefinitionSite.STORABLE;
         pkInfo.simpleType = PrimitiveType.STRING;
         newEntry.columnInformation.put(uidName, pkInfo);
       } else {
         pkInfo = definedPK;
       }
       if (!newEntry.hasSuper()) {
         pkInfo.type = VarType.PK;
       }
       
       newEntry.primaryKeyName = pkInfo.getColumnName();
     } else {
       if (!newEntry.hasSuper()) {
         //  Eigene PK spalte erzeugen, basierend auf PK des RootTypes + foreignKey auf parent (relevant für zB. Liste)
         String unidName = sgc.createUniqueColNameFor("unid");
         pkInfo = new StorableColumnInformation(null, unidName, null, newEntry);
         pkInfo.definedIn = VarDefinitionSite.STORABLE;
         pkInfo.simpleType = PrimitiveType.STRING;
         pkInfo.type = VarType.PK;
         newEntry.columnInformation.put(unidName, pkInfo);
   
         String foreignKeyParentName = sgc.createUniqueColNameFor("parentuid");
         StorableColumnInformation fkColumn = new StorableColumnInformation(null, foreignKeyParentName, null, newEntry);
         if (possessingStorable.getInfo() instanceof XMOMStorableStructureInformation) {
           //foreignkey auf basetype
           fkColumn.simpleType = usesHistorization ? PrimitiveType.STRING : possessingStorable.getInfo().getSuperRootStorableInformation().getColInfoByVarType(VarType.PK).simpleType;
         } else {
           //foreignkey auf anderes expandiertes storable
           fkColumn.simpleType = PrimitiveType.STRING;
         }
         fkColumn.type = VarType.EXPANSION_PARENT_FK;
         fkColumn.definedIn = VarDefinitionSite.STORABLE;
         newEntry.columnInformation.put(foreignKeyParentName, fkColumn);
         
         newEntry.primaryKeyName = pkInfo.getColumnName();
       } else {
         newEntry.primaryKeyName = newEntry.getSuperRootStorableInformation().getPrimaryKeyName();
       }
       
     }
         
     if (currentStorableIsList &&
         !newEntry.hasSuper()) {
       String listIdxName = sgc.createUniqueColNameFor("idx");
       StorableColumnInformation info = new StorableColumnInformation(null, listIdxName, null, newEntry);
       info.definedIn = VarDefinitionSite.STORABLE;
       newEntry.columnInformation.put(listIdxName, info);
       info.type = VarType.LIST_IDX;
       info.simpleType = PrimitiveType.INTEGER;
     }
     
     if (newEntry == newEntry.getSuperRootStorableInformation()) {
       String typeNameName = sgc.createUniqueColNameFor(StorableCodeBuilder.COL_TYPENAME);
       StorableColumnInformation typeInfo = new StorableColumnInformation(null, typeNameName, null, newEntry);
       typeInfo.definedIn = VarDefinitionSite.STORABLE;
       typeInfo.simpleType = PrimitiveType.STRING;
       typeInfo.type = VarType.TYPENAME;
       typeInfo.restrictions = new Restrictions();
       typeInfo.restrictions.addRestriction(new MaxLengthRestriction(1024));
       newEntry.columnInformation.put(typeNameName, typeInfo);
     }
   }
     
   if (newEntry.getParentXMOMStorableInformation().usesHistorization &&
       xmomStorableBase.getPersistenceInformation().getConstraints().size() > 0) {
     // Duplicated code, but uses historization is set rather late :-/
     for (AVariable aVar : variables) {
       String localPath = path;
       String localPathRelativeToParentDatatype = "";
       if (possessingStorable != null &&
           newEntry == possessingStorable.getInfo()) {
         //reingemerged wegen flatheit -> kein neuer datentyp
         localPathRelativeToParentDatatype = pathRelativeToParentDatatype;
       }
       if (possessingStorable != null) {
         localPath += ".";
       }
       if (localPathRelativeToParentDatatype.length() > 0) {
         localPathRelativeToParentDatatype += ".";
       }
       localPath += aVar.getVarName();
       localPathRelativeToParentDatatype += aVar.getVarName();

       if (isTransient(xmomStorableBase, aVar, localPath)) {
         continue;
       }
    // End of Duplicated code
       if (xmomStorableBase.getPersistenceInformation().getConstraints().contains(localPath)) {
         // create helper column
         String uidName = sgc.createUniqueColNameFor("uniquehelper");
         StorableColumnInformation uniquehelper = new StorableColumnInformation(null, uidName, null, newEntry);
         uniquehelper.definedIn = VarDefinitionSite.STORABLE;
         uniquehelper.simpleType = PrimitiveType.STRING;
         newEntry.columnInformation.put(uidName, uniquehelper);
         uniquehelper.type = VarType.UNIQUE_HELPER_COL;
         
         StorableColumnInformation uidColumn = newEntry.getColumnInfo(aVar.getVarName());
         uniquehelper.correspondingUniqueIdentifier = uidColumn;
       }
     }
   }
   
   // complexe felder
   for (AVariable aVar : variables) {
     String localPath = path;
     String localPathRelativeToParentDatatype = "";
     if (possessingStorable != null &&
         newEntry == possessingStorable.getInfo()) {
       //reingemerged wegen flatheit -> kein neuer datentyp
       localPathRelativeToParentDatatype = pathRelativeToParentDatatype;
     }
     if (possessingStorable != null) {
       localPath += ".";
     }
     if (localPathRelativeToParentDatatype.length() > 0) {
       localPathRelativeToParentDatatype += ".";
     }
     localPath += aVar.getVarName();
     localPathRelativeToParentDatatype += aVar.getVarName();

     if (isTransient(xmomStorableBase, aVar, localPath)) {
       continue;
     }

     //achtung, für primitive typen werden hier unnötigerweise spaltennamen neu generiert, d.h. die datenhaltung im kontext geändert
     //beim fixen aber auf abwärtskompatibilität achten, also z.b. erst, wenn es das feature gibt, dass spaltennamen-zuordnung persistiert wird
     StorableColumnInformation info = StorableColumnInformation.createXMOMStorableColumnInformation(aVar, newEntry, sgc);
     if (currentDOMisDefinedAsFlatInParent && !currentStorableIsList) {
       info.typeInformation = new HashSet<PersistenceTypeInformation>();
     }
     info.definedIn = VarDefinitionSite.DATATYPE;
     if (aVar.isJavaBaseType()) {
       info.definedIn = VarDefinitionSite.BOTH;
     }
     
     addPersistenceTypeInformation(newEntry.getParentXMOMStorableInformation(), localPath, info, aVar);
     
     boolean storeColumnInformation = true;
     if (aVar.isJavaBaseType() || aVar.getDomOrExceptionObject() instanceof ExceptionGeneration) {
       if (aVar.isJavaBaseType() && aVar.isList()) {
         info.simpleType = aVar.getJavaTypeEnum(); //null falls exception
         StorableStructureInformation synthInfo = StorableStructureInformation.createSyntheticListStructure(xmomStorableBase, aVar, info, newEntry, sgc);
         info.correspondingStorable = new DirectStorableStructureIdentifier(synthInfo);
       } else {
         continue;          
       }
     } else {
       boolean isFlat = (currentDOMisDefinedAsFlatInParent && !isExludedFromFlat(xmomStorableBase, localPath)) || isFlat(xmomStorableBase, localPath);
       if (isStorableReference(xmomStorableBase, localPath)) {
         if (aVar.isList()) {
           //es gibt kein zugehöriges dom für die util-tabelle, deshalb hier die notwendigen sachen setzen
           StorableStructureInformation synthInfo = StorableStructureInformation.createSyntheticListStructure(xmomStorableBase, aVar, info, newEntry, sgc);
           info.correspondingStorable = new DirectStorableStructureIdentifier(synthInfo);
           StorableColumnInformation forwardRef = synthInfo.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
           forwardRef.reference = true;
           forwardRef.correspondingStorable = new ReferenceStorableStructureIdentifier(aVar.getDomOrExceptionObject().getRevision(),
                                                                                       aVar.getDomOrExceptionObject().getOriginalFqName(),
                                                                                       aVar.getDomOrExceptionObject().getFqClassName());
           // keine Datentypspalte, zeigt auf sich selbst
           forwardRef.correspondingReferenceIdColumnName = forwardRef.getColumnName();
         } else {
           info.reference = true;
           info.correspondingStorable = new ReferenceStorableStructureIdentifier(aVar.getDomOrExceptionObject().getRevision(),
                                                                                 aVar.getDomOrExceptionObject().getOriginalFqName(),
                                                                                 aVar.getDomOrExceptionObject().getFqClassName());
           String referenceColumnName;
           if (referenceColNamesOldNamingStyle.get()) {
             String referenceName = "reference";
             int refCount = -1;
             Set<StorableColumnInformation> scis = newEntry.getColumnInfo(false);
             for (StorableColumnInformation sci : scis) {
               if (sci.isStorableVariable() && 
                   sci.getStorableVariableType() == StorableVariableType.REFERENCE) {
                 refCount++;
               }
             }
             if (refCount >= 0) {
               referenceName += refCount;
             }
             referenceColumnName = sgc.createUniqueColNameFor(referenceName);
           } else {
             referenceColumnName = sgc.createUniqueColNameFor("ref_" + aVar.getVarName());
           }
           StorableColumnInformation referenceColumn = new StorableColumnInformation(null, referenceColumnName, null, newEntry);
           referenceColumn.definedIn = VarDefinitionSite.STORABLE;
           PrimitiveType referenceType = PrimitiveType.boxType(((DOM)aVar.getDomOrExceptionObject()).getPersistenceInformation().getPrimaryKeyType());
           referenceColumn.simpleType = referenceType;
           referenceColumn.type = VarType.REFERENCE_FORWARD_FK;
           newEntry.columnInformation.put(referenceColumnName, referenceColumn);
           
           info.correspondingReferenceIdColumnName = referenceColumnName;
         }
         // Structure für Referenzen anlegen
         sgc.pb.enter(aVar.getVarName(), aVar.isList());
         sgc.pb.enter((DOM) aVar.getDomOrExceptionObject(), false, true);
         StorableStructureIdentifier revStructure = getOrCreateStructure((DOM) aVar.getDomOrExceptionObject(), (DOM) aVar.getDomOrExceptionObject(), null, null, null, 
                                                                    previousDoms, false, false, sgc);
         previousDoms.add(currentDom);
         fillStructureRecursively(revStructure, (DOM) aVar.getDomOrExceptionObject(), (DOM) aVar.getDomOrExceptionObject(), "", "", null, null, null, 
                                  previousDoms, false, false, sgc, HierachyTraversal.INIT);
         previousDoms.remove(previousDoms.size() - 1);
         sgc.pb.exit();
         sgc.pb.exit();
       } else if (DOM.foundCycle(currentDom, previousDoms)) {
         //zyklus
         info.isCycle = true;
       } else {
         //expansion
         sgc.pb.enter(aVar.getVarName(), aVar.isList());
         sgc.pb.enter((DOM) aVar.getDomOrExceptionObject(), isFlat && !aVar.isList(), false);
         StorableStructureIdentifier expandedMemberVarInfo =
                         getOrCreateStructure(xmomStorableBase, (DOM) aVar.getDomOrExceptionObject(), structure, info, aVar, 
                                                    previousDoms, aVar.isList(), isFlat, sgc);
         previousDoms.add(currentDom);
         fillStructureRecursively(expandedMemberVarInfo, xmomStorableBase, (DOM) aVar.getDomOrExceptionObject(), localPath, localPathRelativeToParentDatatype, structure, info, aVar, 
                                  previousDoms, aVar.isList(), isFlat, sgc, HierachyTraversal.INIT);
         previousDoms.remove(previousDoms.size() - 1);
         sgc.pb.exit();
         sgc.pb.exit();
         if (isFlat && !aVar.isList()) {
           //kind eingeflacht
           storeColumnInformation = false;
         } else {
           expandedMemberVarInfo.getInfo().parent = info;
           info.correspondingStorable = expandedMemberVarInfo;
           info.columnName = expandedMemberVarInfo.getInfo().getTableName(); // identifiy this datatype only column by it's elements tablename
         }
       }
     }
     
     if (storeColumnInformation) {
       newEntry.columnInformation.put(info.getColumnName(), info);
       if (currentDOMisDefinedAsFlatInParent) {
         info.path = localPathRelativeToParentDatatype;
       }
     }
   }
   
   if (!isFlattened) {
     // ableitungen
     Set<DOM> directSubTypes = getDirectRelevantSubTypes(currentDom, sgc.gbc);
     if (traversal != HierachyTraversal.NO_SUBS) {
       Set<StorableStructureIdentifier> subEntries = new TreeSet<>(STRUCTURE_ID_COMPARATOR);
       for (DOM subType : directSubTypes) {
         DOM newXmomStorableBase = possessingStorable == null ? subType : xmomStorableBase;
         sgc.pb.exchangeType(subType);
         StorableStructureIdentifier subEntry = getOrCreateStructure(newXmomStorableBase, (DOM)subType, possessingStorable, possessingColumn, possessingVar, previousDoms, currentStorableIsList, currentDOMisDefinedAsFlatInParent, sgc);
         fillStructureRecursively(subEntry, newXmomStorableBase, subType, path, pathRelativeToParentDatatype, possessingStorable, possessingColumn, possessingVar, previousDoms, currentStorableIsList, currentDOMisDefinedAsFlatInParent, sgc, traversalForSubs);
         sgc.pb.exchangeType(currentDom);
         subEntries.add(subEntry);
       }
       newEntry.subEntries = subEntries;
     } else {
       newEntry.subEntries = new TreeSet<>(STRUCTURE_ID_COMPARATOR);
     }
   }
   
   newEntry.traversalState = TraversalState.FINISHED;
  }
  
  
  private DOM findRelevantSuperDom(DOM dom) {
    DOM currentDom = dom;
    while (currentDom.hasSuperClassGenerationObject()) {
      DOM superDom = currentDom.getSuperClassGenerationObject();
      if (superDom.isStorableEquivalent()) {
        currentDom = superDom;
      } else {
        return superDom;
      }
    }
    return null;
  }

  
  private Set<DOM> getDirectRelevantSubTypes(DOM dom, GenerationBaseCache gbc) {
    Set<DOM> directSubTypes = getDirectSubTypes(dom, gbc);
    Set<DOM> relevantSubTypes = new HashSet<DOM>();
    for (DOM directSubType : directSubTypes) {
      if (directSubType.isStorableEquivalent()) {
        relevantSubTypes.addAll(getDirectRelevantSubTypes(directSubType, gbc));
      } else {
        relevantSubTypes.add(directSubType);
      }
    }
    return relevantSubTypes;
  }

  private Set<DOM> getDirectSubTypes(DOM dom, GenerationBaseCache gbc) {
    Set<GenerationBase> subTypesGB = dom.getSubTypes(gbc, false);
    List<DOM> subTypes = CollectionUtils.transform(subTypesGB, gb -> (DOM)gb);
    Set<DOM> directSubTypes = new TreeSet<>(GenerationBase.DEPENDENCIES_COMPARATOR); //deterministische reihenfolge
    for (DOM subType : subTypes) {
      DOM domSubType = (DOM) subType;
      if (Objects.equals(domSubType.getSuperClassGenerationObject().getOriginalFqName(), dom.getOriginalFqName()) &&
          Objects.equals(domSubType.getSuperClassGenerationObject().getRevision(), dom.getRevision())) {
        directSubTypes.add(subType);
      }
    }
    return directSubTypes;
  }
  

  private void validatePersistenceInfo(String objectName, String elementName, Set<String> elementPaths, Set<String> memVarNames, Set<String> superMemVarNames) {
    if (!memVarNames.containsAll(elementPaths)) {
      for (String elementPath : elementPaths) {
        if (superMemVarNames.contains(elementPath)) {
          logger.warn(elementName + "-Entry for path '" + objectName + "." + elementPath + "' is trying to redefine an inherited member!");
        }
      }
    }
  }


  private Set<String> extractRootPart(Set<String> paths) {
    Set<String> rootPaths = new HashSet<>();
    for (String path : paths) {
      if (path.contains(".")) {
        rootPaths.add(path);
      } else {
        rootPaths.add(path.split("\\.")[0]);
      }
    }
    return rootPaths;
  }


  private void addPersistenceTypeInformation(XMOMStorableStructureInformation xmomStorableBase, String localPath, StorableColumnInformation info, AVariable aVar) {
    if (xmomStorableBase.getPersistenceInformation().getCustomField0().contains(localPath)) {
      info.typeInformation.add(PersistenceTypeInformation.CUSTOMFIELD_0);
    }
    if (xmomStorableBase.getPersistenceInformation().getCustomField1().contains(localPath)) {
      info.typeInformation.add(PersistenceTypeInformation.CUSTOMFIELD_1);
    }
    if (xmomStorableBase.getPersistenceInformation().getCustomField2().contains(localPath)) {
      info.typeInformation.add(PersistenceTypeInformation.CUSTOMFIELD_2);
    }
    if (xmomStorableBase.getPersistenceInformation().getCustomField3().contains(localPath)) {
      info.typeInformation.add(PersistenceTypeInformation.CUSTOMFIELD_3);
    }
    // UID, Historization, CurrentVersion
    if (aVar.getPersistenceTypes() != null) {
      info.typeInformation.addAll(aVar.getPersistenceTypes());
    }
  }


  public XMOMStorableStructureInformation unregister(DOM xmomStorable) {
    return storableStructureInformation.remove(xmomStorable.getFqClassName());
  }


  public XMOMStorableStructureInformation getStructuralInformation(String xmomStorableClassName) {
    return storableStructureInformation.get(xmomStorableClassName);
  }
  
  
  public Collection<XMOMStorableStructureInformation> getAllStorableStructureInformation() {
    return Collections.unmodifiableCollection(storableStructureInformation.values());
  }


  // for tests
  void addStructuralInformation(String xmomStorableClassName, XMOMStorableStructureInformation info) {
    storableStructureInformation.put(xmomStorableClassName, info);
  }


  public static class StorableStructureInformation {

    protected String originalXmlName;
    protected String tableName;
    
    /**
     * colname (eindeutig) - bei der erzeugung werden alle colnames die zu einer typ-hierarchie gehören eindeutig erzeugt
     */
    protected Map<String, StorableColumnInformation> columnInformation;
    protected String primaryKeyName;
    
    
    protected Class<? extends Storable<?>> storableClazz;
    protected ResultSetReader<?> resultSetReaderForDatatype;
    protected Method transformDatatypeToStorableMethod;
    
    /**
     * parent storablestructureinfo hat eine column, die auf this zeigt. dies ist diese column
     */
    protected StorableColumnInformation parent;
    protected boolean isExpansiveList;
    protected boolean isSynthetic;
    protected Long revision;
    protected Long definingRevision; // xmomStorableBase.revision
    protected String fqClassNameOfStorableContainingDatatype;
    protected String fqClassNameOfDatatype;
    
    /**
     * new storable access code
     */
    protected InMemoryStorableClassLoader storableClassLoader;
    protected JavaSourceFromString source;
    
    protected StorableStructureIdentifier superEntry; 
    protected Set<StorableStructureIdentifier> subEntries;
    
    protected PersistenceAccessDelegator persistenceAccessDelegator;
    protected boolean isAbstract;
    
    protected TraversalState traversalState = TraversalState.INIT;

    protected StorableStructureInformation() {
    }
    
    
    private StorableStructureInformation(DOM xmomStorableBase, DOM current, AVariable possessingColumn, boolean isExpansiveList, StructureGenerationContext sgc) {
      try {
        this.tableName =  sgc.createUniqueTableNameFor();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      revision = current.getRevision();
      definingRevision = xmomStorableBase.getRevision();
      fqClassNameOfStorableContainingDatatype = current.getFqClassName();
      columnInformation = new HashMap<String, StorableColumnInformation>();
      this.isExpansiveList = isExpansiveList;
      originalXmlName = current.getOriginalFqName();
      fqClassNameOfDatatype = current.getFqClassName();
    }


    private static StorableStructureInformation createSyntheticListStructure(DOM xmomStorableBase, AVariable linkingVar, StorableColumnInformation linkingColumn, StorableStructureInformation sourceStructure, StructureGenerationContext sgc) throws PersistenceLayerException {
      DOM current = (DOM) linkingVar.getCreator();
      
      sgc.pb.enter(linkingVar.getVarName(), true);
      
      if (linkingVar.isJavaBaseType()) {
        sgc.pb.enter(linkingVar.getJavaTypeEnum().getFqName(), current.getRevision());
      } else {
        sgc.pb.enter(linkingVar.getDomOrExceptionObject().getOriginalFqName(), current.getRevision());
      }
      
      StorableStructureInformation result = new StorableStructureInformation(xmomStorableBase, current, linkingVar, true, sgc);
      
      result.originalXmlName = current.getOriginalFqName();
      result.fqClassNameOfDatatype = current.getFqClassName();
      result.isSynthetic = true;
      result.parent = linkingColumn;
      
      // pk
      StorableColumnInformation pkColumn = new StorableColumnInformation(null, "pk", null, result);
      pkColumn.columnName = sgc.createUniqueColNameFor("pk");
      pkColumn.simpleType = PrimitiveType.STRING;
      pkColumn.definedIn = VarDefinitionSite.STORABLE;
      pkColumn.type = VarType.PK;
      result.columnInformation.put(pkColumn.getColumnName(), pkColumn);
      result.primaryKeyName = pkColumn.getColumnName();

      boolean usesHistorization = sourceStructure.getParentXMOMStorableInformation().usesHistorization;
      
      // parentUid
      StorableColumnInformation parentUidColumn = new StorableColumnInformation(null, "parentuid", null, result);
      parentUidColumn.columnName = sgc.createUniqueColNameFor("parentuid");
      if (sgc.pb.getPath().indexOf(".") > -1) {
        //foreignkey auf anderes expandiertes storable
        parentUidColumn.simpleType = PrimitiveType.STRING;
      } else {
        //foreignkey auf basetype
        parentUidColumn.simpleType = usesHistorization ? PrimitiveType.STRING : sourceStructure.getSuperRootStorableInformation().getColInfoByVarType(VarType.PK).getPrimitiveType();
      }
      parentUidColumn.definedIn = VarDefinitionSite.STORABLE;
      parentUidColumn.type = VarType.EXPANSION_PARENT_FK;
      result.columnInformation.put(parentUidColumn.getColumnName(), parentUidColumn);
      
      // idx
      StorableColumnInformation idxColumn = new StorableColumnInformation(null, "idx", null, result);
      idxColumn.columnName = sgc.createUniqueColNameFor("idx");
      idxColumn.simpleType = PrimitiveType.INTEGER;
      idxColumn.definedIn = VarDefinitionSite.STORABLE;
      idxColumn.type = VarType.LIST_IDX;
      result.columnInformation.put(idxColumn.getColumnName(), idxColumn);
      
      StorableColumnInformation elementColumn;
      if (linkingVar.isJavaBaseType()) { 
        elementColumn = new StorableColumnInformation(null, linkingColumn.getVariableName().toLowerCase(), null, result);
        elementColumn.columnName = sgc.createUniqueColNameFor(linkingColumn.getVariableName().toLowerCase());
        elementColumn.simpleType = linkingVar.getJavaTypeEnum();
        elementColumn.type = VarType.DEFAULT;
        elementColumn.variableName = linkingVar.getVarName();
      } else {
        elementColumn = new StorableColumnInformation(null, "reference", null, result);
        elementColumn.columnName = sgc.createUniqueColNameFor("reference");
        elementColumn.simpleType = ((DOM)linkingVar.getDomOrExceptionObject()).getPersistenceInformation().getPrimaryKeyType();
        elementColumn.type = VarType.REFERENCE_FORWARD_FK;
        elementColumn.reference = true;
      }
      elementColumn.definedIn = VarDefinitionSite.STORABLE;
      
      result.columnInformation.put(elementColumn.getColumnName(), elementColumn);
      
      sgc.pb.exit();
      sgc.pb.exit();
      
      return result;
    }


    public Set<String> getAllColumnNamesExcludingTransient() {
      Set<String> columnNames = new HashSet<String>();
      for (StorableColumnInformation columnInfo : columnInformation.values()) {
        if (columnInfo.typeInformation == null
            || !columnInfo.typeInformation.contains(PersistenceTypeInformation.TRANSIENCE)) {
          columnNames.add(columnInfo.getColumnName());
        }
      }
      return columnNames;
    }
    
    public Collection<StorableColumnInformation> getAllRelevantStorableColumns() {
      Collection<StorableColumnInformation> columns = new ArrayList<>();
      for (StorableColumnInformation columnInfo : columnInformation.values()) {
        if ((columnInfo.typeInformation == null
            || !columnInfo.typeInformation.contains(PersistenceTypeInformation.TRANSIENCE)) &&
            (columnInfo.getDefinitionSite() == VarDefinitionSite.BOTH ||
             columnInfo.getDefinitionSite() == VarDefinitionSite.STORABLE)) {
          columns.add(columnInfo);
        }
      }
      return columns;
    }
    
    public Collection<StorableColumnInformation> getAllRelevantStorableColumnsForDatatypeReader() {
      Collection<StorableColumnInformation> columns = new ArrayList<>();
      for (StorableColumnInformation columnInfo : columnInformation.values()) {
        if ((columnInfo.typeInformation == null
            || !columnInfo.typeInformation.contains(PersistenceTypeInformation.TRANSIENCE)) &&
            (columnInfo.getDefinitionSite() == VarDefinitionSite.BOTH ||
             columnInfo.getDefinitionSite() == VarDefinitionSite.DATATYPE)) {
          columns.add(columnInfo);
        }
        if (columnInfo.getType() == VarType.UNIQUE_HELPER_COL) {
          columns.add(columnInfo);
        }
      }
      return columns;
    }
    
    public Collection<StorableColumnInformation> getAllComplexColumns() {
      return getAllComplexColumns(false);
    }

    public Collection<StorableColumnInformation> getAllComplexColumns(boolean includeParentTypes) {
      Collection<StorableColumnInformation> columns = new ArrayList<>();
      for (StorableColumnInformation columnInfo : columnInformation.values()) {
        if (columnInfo.isStorableVariable() ||
            columnInfo.isList) {
          columns.add(columnInfo);
        }
      }

      StorableStructureIdentifier parentSSI = getSuperEntry();
      if (parentSSI != null && includeParentTypes) {
        StorableStructureInformation parentSI = parentSSI.getInfo();
        columns.addAll(parentSI.getAllComplexColumns(includeParentTypes));
      }

      return columns;
    }
    
    public Collection<StorableColumnInformation> getAllRelevantStorableColumnsForDatatypeReaderRecursivly() {
      Collection<StorableColumnInformation> columns = new ArrayList<>();
      columns.addAll(getAllRelevantStorableColumnsForDatatypeReader());
      if (hasSuper()) {
        columns.addAll(getSuperEntry().getInfo().getAllRelevantStorableColumnsForDatatypeReaderRecursivly());
      }
      return columns;
    }
    
    public Collection<StorableColumnInformation> getAllRelevantStorableColumnsRecursivly() {
      Collection<StorableColumnInformation> columns = new ArrayList<>();
      columns.addAll(getAllRelevantStorableColumns());
      if (hasSuper()) {
        columns.addAll(getSuperEntry().getInfo().getAllRelevantStorableColumnsRecursivly());
      }
      return columns;
    }


    public StorableColumnInformation getColumnInfo(String variableName) {
      return getColumnInfo(variableName, false);
    }


    public StorableColumnInformation getColumnInfo(String variableName, boolean includeParentTypes) { 
      for (StorableColumnInformation column : columnInformation.values()) {
        if (column.getVariableName() != null &&
            column.getVariableName().equals(variableName)) {
          return column;
        }
      }

      StorableStructureIdentifier parentSSI = getSuperEntry();
      if (parentSSI != null && includeParentTypes) {
        StorableStructureInformation parentSI = parentSSI.getInfo();
        return parentSI.getColumnInfo(variableName, includeParentTypes);
      }

      return null;
    }


    public Set<StorableColumnInformation> getColumnInfosByPathPrefix(String pathPrefix) {
      Set<StorableColumnInformation> columns = new TreeSet<>();
      for (StorableColumnInformation column : columnInformation.values()) {
        if (column.getPath() != null &&
            column.getPath().startsWith(pathPrefix)) {
          columns.add(column);
        }
      }

      return columns;
    }
    
    public StorableColumnInformation getColumnInfoByName(String columnName) { 
      StorableColumnInformation column = columnInformation.get(columnName);
      return column;
    }
    
    public StorableColumnInformation getColumnInfoByNameAcrossHierachy(String columnName) { 
      StorableColumnInformation column = columnInformation.get(columnName);
      if (column == null &&
          hasSuper()) {
        return superEntry.getInfo().getColumnInfoByNameAcrossHierachy(columnName);
      } else {
        return column;
      }
    }


    public Set<StorableColumnInformation> getColumnInfo(boolean recursivly) {
      return getColumnInfo(recursivly ? ColumnInfoRecursionMode.FULL_RECURSIVE : ColumnInfoRecursionMode.ONLY_LOCAL);
    }
    
    public Set<StorableColumnInformation> getColumnInfoAcrossHierarchy() {
      Set<StorableColumnInformation> columns = new TreeSet<StorableColumnInformation>();
      columns.addAll(getColumnInfo(false));
      if (hasSuper()) {
        columns.addAll(getSuperEntry().getInfo().getColumnInfoAcrossHierarchy());
      }
      return columns;
    }
    
    public Set<StorableColumnInformation> getColumnInfo(ColumnInfoRecursionMode recursion) {
      switch (recursion) {
        case FULL_RECURSIVE :
        case IDENTITY_ONLY_FOR_REFERENCES :
          Set<StorableColumnInformation> cols = new TreeSet<StorableColumnInformation>();
          for (StorableColumnInformation column : columnInformation.values()) {
            cols.add(column);
            if (column.isStorableVariable()) {
              if (recursion == ColumnInfoRecursionMode.FULL_RECURSIVE || 
                  column.getStorableVariableType() == StorableVariableType.EXPANSION) {
                cols.addAll(column.getStorableVariableInformation().getColumnInfo(recursion));
              } else {
                XMOMStorableStructureInformation subInfo = (XMOMStorableStructureInformation)column.getStorableVariableInformation();
                cols.add(subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER));
                if (subInfo.usesHistorization()) {
                  cols.add(subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP));
                }
              }
            }
          }
          return cols;
        case ONLY_LOCAL :
          return new TreeSet<StorableColumnInformation>(columnInformation.values());
        case ONLY_LOCAL_TO_XMOMSTORABLE :
          cols = new TreeSet<StorableColumnInformation>();
          for (StorableColumnInformation column : columnInformation.values()) {
            cols.add(column);
            if (column.isStorableVariable()) {
              StorableStructureInformation storableVariableInformation = column.getStorableVariableInformation();
              if (!(storableVariableInformation instanceof XMOMStorableStructureInformation)) {
                cols.addAll(storableVariableInformation.getColumnInfo(recursion));
              }
            }
          }
          return cols;
        default :
          throw new UnsupportedOperationException("");
      }
    }
    
    /**
     * liefert die spalteninfos und den pfad relativ zu this.
     */
    public List<QualifiedStorableColumnInformation> getColumnInfoAndPath(ColumnInfoRecursionMode recursion) {
      List<QualifiedStorableColumnInformation> result = new ArrayList<QualifiedStorableColumnInformation>();
      getColumnInfoAndPathRecursivly(recursion, new ArrayList<StorableColumnInformation>(), result);
      Collections.sort(result, new Comparator<QualifiedStorableColumnInformation>() {

        @Override
        public int compare(QualifiedStorableColumnInformation o1, QualifiedStorableColumnInformation o2) {
          return o1.getColumn().compareTo(o2.getColumn());
        }
        
      });
      return result;
    }
    
    
    private void getColumnInfoAndPathRecursivly(ColumnInfoRecursionMode recursion, List<StorableColumnInformation> path, List<QualifiedStorableColumnInformation> result) {
      switch (recursion) {
        case FULL_RECURSIVE :
        case IDENTITY_ONLY_FOR_REFERENCES :
          for (StorableColumnInformation column : columnInformation.values()) {
            result.add(new QualifiedStorableColumnInformation(column, path));
            if (column.isStorableVariable()) {
              List<StorableColumnInformation> newPath = new ArrayList<StorableColumnInformation>(path);
              newPath.add(column);
              if (recursion == ColumnInfoRecursionMode.FULL_RECURSIVE || 
                  column.getStorableVariableType() == StorableVariableType.EXPANSION) {
                column.getStorableVariableInformation().getColumnInfoAndPathRecursivly(recursion, newPath, result);
              } else {
                XMOMStorableStructureInformation subInfo = (XMOMStorableStructureInformation)column.getStorableVariableInformation();
                result.add(new QualifiedStorableColumnInformation(subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER), newPath));
                if (subInfo.usesHistorization()) {
                  result.add(new QualifiedStorableColumnInformation(subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP), newPath));
                  result.add(new QualifiedStorableColumnInformation(subInfo.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG), newPath));
                  result.add(new QualifiedStorableColumnInformation(subInfo.getColInfoByVarType(VarType.PK), newPath));
                }
              }
            }
          }
          break;
        case ONLY_LOCAL :
          for (StorableColumnInformation column : columnInformation.values()) {
            result.add(new QualifiedStorableColumnInformation(column, path));
          }
          break;
        default :
          throw new IllegalArgumentException("Unknown ColumnInfoRecursionMode: " + recursion);
      }
    }
    
    
    public static enum ColumnInfoRecursionMode {
      ONLY_LOCAL, FULL_RECURSIVE, IDENTITY_ONLY_FOR_REFERENCES, ONLY_LOCAL_TO_XMOMSTORABLE;
    }


    public Class<? extends Storable<?>> getStorableClass() {
      if (storableClazz == null) {
        reloadStorableClass();
      }
      if (storableClazz == null) {
        throw new RuntimeException("StorableClass could not be loaded for " + fqClassNameOfDatatype + " @" + getRevision());
      }
      return storableClazz;
    }


    public String getTableName() {
      if (hasSuper()) {
        return getSuperRootStorableInformation().getTableName();  
      } else {
        return tableName;
      }
    }
    
    
    public String getPrimaryKeyName() {
      if (hasSuper()) {
        return getSuperRootStorableInformation().getPrimaryKeyName();  
      } else {
        return primaryKeyName;
      }
    }


    public ResultSetReader<?> getResultSetReaderForDatatype() {
      if (!isSynthetic && 
          resultSetReaderForDatatype == null) {
        reloadTransformMethods();
      }
      return resultSetReaderForDatatype;
    }


    public Method getTransformDatatypeToStorableMethod() {
      if (transformDatatypeToStorableMethod == null) {
        reloadTransformMethods();
      }
      return transformDatatypeToStorableMethod;
    }

    
    private EnumMap<PersistenceTypeInformation, List<StorableColumnInformation>> colsByPersistenceType = 
      new EnumMap<PersistenceTypeInformation, List<StorableColumnInformation>>(PersistenceTypeInformation.class);

    public List<StorableColumnInformation> getColInfosByPersistenceType(PersistenceTypeInformation type) {
      List<StorableColumnInformation> result = colsByPersistenceType.get(type);
      if (result == null) {
        List<StorableColumnInformation> l = new ArrayList<StorableColumnInformation>();
        for (StorableColumnInformation entry : columnInformation.values()) {
          if (entry.typeInformation != null && entry.typeInformation.contains(type)) {
            l.add(entry);
          }
        }
        if (l.size() == 0) {
          l = Collections.emptyList();
        }
        colsByPersistenceType.put(type, l);
        result = l;
      }
      return result;
    }
    
    public List<StorableColumnInformation> getColInfosByPersistenceTypeAcrossHierachy(PersistenceTypeInformation type) {
      List<StorableColumnInformation> l = new ArrayList<StorableColumnInformation>();
      l.addAll(getColInfosByPersistenceType(type));
      if (hasSuper()) {
        return superEntry.getInfo().getColInfosByPersistenceTypeAcrossHierachy(type);
      }
      return l;
    }
    

    public StorableColumnInformation getColInfoByPersistenceType(PersistenceTypeInformation type) {
      List<StorableColumnInformation> result = getColInfosByPersistenceType(type);
      if (result.size() > 0) {
        return result.get(0);
      } else {
        return null;
      }
    }
    
    public StorableColumnInformation getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation type) {
      StorableColumnInformation result = getColInfoByPersistenceType(type);
      if (result == null &&
          hasSuper()) {
        return superEntry.getInfo().getColInfoByPersistenceTypeAcrossHierachy(type);
      } else {
        return result;
      }
    }
    
    
    private EnumMap<VarType, StorableColumnInformation> colsByVarType = 
      new EnumMap<XMOMStorableStructureCache.VarType, XMOMStorableStructureCache.StorableColumnInformation>(VarType.class);
    

    public StorableColumnInformation getColInfoByVarType(VarType type) {
      StorableColumnInformation result = colsByVarType.get(type);
      if (result == null) {
        for (StorableColumnInformation entry : columnInformation.values()) {
          if (entry.type != null && entry.type == type) {
            colsByVarType.put(type, entry);
            return entry;
          }
        }
        return null;
      } else {
        return result;
      }
    }


    public String getFqXmlName() {
      return originalXmlName;
    }
    
    public StorableColumnInformation getPossessingColumn() {
      return parent;
    }


    public boolean isSuperTypeOf(StorableStructureInformation type) {
      return getSubEntriesRecursivly().contains(type);
    }
    
    
    public boolean isSyntheticStorable() {
      return isSynthetic; //helper-storables
    }
    
    
    protected boolean isReferencedList() {
      if (isSynthetic) {
        for (StorableColumnInformation column : getColumnInfo(false)) {
          if (column.isStorableVariable()) {
            return column.getStorableVariableType() == StorableVariableType.REFERENCE;
          }
        }
        return false;
      } else {
        return false;
      }
    }
    
    
    public StorableStructureInformation getReferencedStorable() {
      if (isSynthetic) {
        for (StorableColumnInformation column : getColumnInfo(false)) {
          if (column.isStorableVariable()) {
            return column.getStorableVariableInformation();
          }
        }
        return null;
      } else {
        return null;
      }
    }
    
    
    public void traverse(StorableStructureVisitor visitor) {
      traverse(null, visitor, new HashSet<StorableStructureInformation>());
    }
    
    
    protected void traverse(StorableColumnInformation comingFrom, StorableStructureVisitor visitor, Set<StorableStructureInformation> visited) {
      visitor.enter(comingFrom, this);
      for (StorableColumnInformation column : this.getColumnInfo(false)) {
        if (column.isStorableVariable()) {
          if (visitor.getRecursionFilter().accept(column) &&
              column.getStorableVariableInformation() != null && 
              !visited.contains(column.getStorableVariableInformation())) {
            visited.add(column.getStorableVariableInformation());
            column.getStorableVariableInformation().traverse(column, visitor, visited);
          }
        }
      }
      if (getSuperEntry() != null) {
        StorableStructureInformation superEntryInfo = checkAndLogNull(getSuperEntry(), "superEntry");
        if (superEntryInfo != null && !visited.contains(superEntryInfo) && visitor.getRecursionFilter().acceptHierarchy(superEntryInfo)) {
          visited.add(superEntryInfo);
          superEntryInfo.traverse(comingFrom, visitor, visited);
        }
      }
      if (getSubEntries() != null) {
        for (StorableStructureIdentifier subEntry : getSubEntries()) {
          StorableStructureInformation subEntryInfo = checkAndLogNull(subEntry, "subEntry");
          if (subEntryInfo != null && !visited.contains(subEntryInfo) && visitor.getRecursionFilter().acceptHierarchy(subEntryInfo)) {
            visited.add(subEntryInfo);
            subEntryInfo.traverse(comingFrom, visitor, visited);
          }
        }
      }
      visitor.exit(comingFrom, this);
    }
    
    
    private StorableStructureInformation checkAndLogNull(StorableStructureIdentifier ref, String type) {
      StorableStructureInformation info = ref.getInfo();
      if (info == null) {
        String rtc;
        try {
          rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision).getGUIRepresentation();
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          rtc = "unknown (" + e.getMessage() + ")";
        }
        String msg = "When traversing xmomstorable " + fqClassNameOfDatatype + " in rev " + revision + "(" + rtc + ") the " + type;
        if (ref instanceof ReferenceStorableStructureIdentifier) {
          ReferenceStorableStructureIdentifier rsub = (ReferenceStorableStructureIdentifier) ref;
          try {
            rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(rsub.referenceRevision).getGUIRepresentation();
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            rtc = "unknown (" + e.getMessage() + ")";
          }
          msg += " " + rsub.fqClassName + " in rev " + rsub.referenceRevision + " (" + rtc + ") could not be looked up. ";
          XMOMStorableStructureCache c = getInstance(rsub.referenceRevision);
          if (c == null) {
            msg += "Cache for revision is null";
          } else {
            msg += "Cache for revision exists. containsRef=" + c.storableStructureInformation.containsKey(rsub.fqClassName);
          }
        } else {
          msg += " was a direct link but empty."; //unexpected
        }
        logger.warn(msg, new Exception());
      }
      return info;
    }


    public Set<StorableStructureIdentifier> getSubEntries() {
      return subEntries;
    }


    public StorableStructureIdentifier getSuperEntry() {
      return superEntry;
    }

    
    public Set<StorableStructureInformation> getSuperEntriesRecursivly() {
      Set<StorableStructureInformation> supers = new TreeSet<>(STRUCTURE_INFO_COMPARATOR);
      if (getSuperEntry() != null) {
        supers.add(getSuperEntry().getInfo());
        supers.addAll(getSuperEntry().getInfo().getSuperEntriesRecursivly());
      }
      return supers;
    }
    
    
    protected void invalidateClassInformation() {
      storableClazz = null;
      transformDatatypeToStorableMethod = null;
      resultSetReaderForDatatype = null;
      for (StorableColumnInformation column : columnInformation.values()) {
        column.setterMethod = null;
      }
    }
    
    
    @SuppressWarnings("unchecked")
    private void reloadStorableClass() {
      try {
        if (storableClassLoader != null) {
          this.storableClazz = (Class<? extends Storable<?>>) storableClassLoader.loadOwnClass();
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("", e);
      }
    }
    
    
    private void reloadTransformMethods() {
      try {
        Class<?> baseTypeStorableClass = getBaseTypeStorableClass();
        if (isExpansiveList || isSynthetic) {
          if (parent != null && parent.getPrimitiveType() != null) {
            transformDatatypeToStorableMethod = getStorableClass().getMethod(DOM.TRANSFORMDATATYPE_METHODNAME, List.class);
          } else if (isReferencedList()) {
            transformDatatypeToStorableMethod = getStorableClass().getMethod(DOM.TRANSFORMDATATYPE_METHODNAME, List.class);
          } else {
            transformDatatypeToStorableMethod = getStorableClass().getMethod(DOM.TRANSFORMDATATYPE_METHODNAME, XynaObject.class, baseTypeStorableClass);
          }
        } else {
          transformDatatypeToStorableMethod = getStorableClass().getMethod(DOM.TRANSFORMDATATYPE_METHODNAME, XynaObject.class, baseTypeStorableClass);
        }
        transformDatatypeToStorableMethod.setAccessible(true);
        if (!isSynthetic) {
          Field f = getStorableClass().getField(DOM.READER_FOR_DATATYPE);
          f.setAccessible(true);
          resultSetReaderForDatatype = (ResultSetReader<?>) f.get(null);
        }
      } catch (SecurityException | NoSuchMethodException | NoSuchFieldException | 
               IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException("", e);
      }
    }
    
    

    private Class<?> getBaseTypeStorableClass() {
      if (this instanceof XMOMStorableStructureInformation) {
        return getSuperRootStorableInformation().getStorableClass();
      } else {
        return parent.getParentXMOMStorableInformation().getSuperRootStorableInformation().getStorableClass();
      }
    }
    
    public Long getRevision() {
      return revision;
    }
    
    public Long getDefiningRevision() {
      return definingRevision;
    }
    
    public InMemoryStorableClassLoader getClassLoaderForStorable() {
      return storableClassLoader;
    }
    
    public void setClassLoaderForStorable(InMemoryStorableClassLoader bacl) {
      storableClassLoader = bacl;
    }
    
    
    public StorableStructureInformation clone() {
      return clone(parent);
    }
    
    public StorableStructureInformation clone(StorableColumnInformation possesingColumn) {
      return clone(possesingColumn, new HashMap<StorableStructureIdentifier, StorableStructureIdentifier>());
    }
    
  public StorableStructureInformation clone(StorableColumnInformation possesingColumn, Map<StorableStructureIdentifier, StorableStructureIdentifier> alreadyCloned) {
      StorableStructureInformation clone = new StorableStructureInformation();
      StorableStructureIdentifier cloneId = new DirectStorableStructureIdentifier(clone);
      StorableStructureIdentifier thisId = new DirectStorableStructureIdentifier(this);
      alreadyCloned.put(thisId, cloneId);
      clone.revision = revision;
      clone.definingRevision = definingRevision;
      clone.originalXmlName = originalXmlName;
      clone.tableName = tableName;
      clone.columnInformation = new HashMap<>();
      for (String columnName : columnInformation.keySet()) {
        StorableColumnInformation column = columnInformation.get(columnName);
        clone.columnInformation.put(columnName, column.clone(clone));
      }
      clone.primaryKeyName = primaryKeyName;
      clone.storableClazz = storableClazz;
      clone.resultSetReaderForDatatype = resultSetReaderForDatatype;
      clone.transformDatatypeToStorableMethod = transformDatatypeToStorableMethod;
      clone.parent = possesingColumn;
      clone.isExpansiveList = isExpansiveList;
      clone.isSynthetic = isSynthetic;
      clone.fqClassNameOfStorableContainingDatatype = fqClassNameOfStorableContainingDatatype;
      clone.fqClassNameOfDatatype = fqClassNameOfDatatype;
      clone.storableClassLoader = storableClassLoader;
      if (superEntry != null) {
        if (alreadyCloned.containsKey(superEntry)) {
          clone.superEntry = alreadyCloned.get(superEntry);      
        } else if (alreadyCloned.containsValue(superEntry)) {
          clone.superEntry = this.superEntry;
        } else {
          clone.superEntry = this.superEntry.clone(possesingColumn, alreadyCloned);
        }
      }
      clone.subEntries = new TreeSet<>(STRUCTURE_ID_COMPARATOR);
      if (subEntries != null) {
        for (StorableStructureIdentifier subEntry : subEntries) {
          if (alreadyCloned.containsKey(subEntry)) {
            clone.subEntries.add(alreadyCloned.get(subEntry));      
          } else if (alreadyCloned.containsValue(subEntry)) {
            clone.subEntries.add(subEntry);
          } else {
            clone.subEntries.add(subEntry.clone(possesingColumn, alreadyCloned));
          }
        }
      }
      return clone;
    }
    
    public void setResultSetReaderForDatatype(ResultSetReader<?> resultSetReaderForDatatype) {
      this.resultSetReaderForDatatype = resultSetReaderForDatatype;
    }
    
    public String getFqClassNameForDatatype() {
      return fqClassNameOfDatatype;
    }
    
    public String getFqClassNameForStorable() {
      if (storableClassLoader != null) {
        return storableClassLoader.getStorableClassName();
      } else {
        return StorableCodeBuilder.getClassNameForStorable(fqClassNameOfStorableContainingDatatype, getSuperRootStorableInformation().getTableName(), definingRevision);
      }
    }
    
    public void setStorableSource(JavaSourceFromString source) {
      this.source = source;
    }
    
    public JavaSourceFromString getStorableSource() {
      return source;
    }


    public boolean hasSuper() {
      return getSuperEntry() != null;
    }


    public XMOMStorableStructureInformation getParentXMOMStorableInformation() {
      if (parent == null) {
        return (XMOMStorableStructureInformation) this; 
      } else {
        if (this instanceof XMOMStorableStructureInformation) {
          return (XMOMStorableStructureInformation) this;
        } else {
          return parent.getParentXMOMStorableInformation();
        }
      }
    }
    
    
    public StorableStructureInformation getSuperRootStorableInformation() {
      if (hasSuper()) {
        return getSuperEntry().getInfo().getSuperRootStorableInformation(); 
      } else {
        return this;
      }
    }
    
    
    protected StorableStructureInformation generateMergedClone(MergeFilter mergeFilter) {
      /*
       * 1) sammle die relevante hierarchy. die beeinhaltet die supertypen und die subtypen von this (rekursiv), aber nicht die anderen subtypen der supertypen ("geschwister-typen" im ableitungsbaum")
       * 2) schmeisse alle einträge aus der hierarchy raus, die nicht zum mergefilter passen
       * 3) erzeuge merged structureinfo:
       * 3.1) alle spalten der hierarchy-typen zum merge hinzufügen
       * 3.2) für komplexe members (inklusive der aus der hierarchy stammenden, die ja in 3.1. bereits hinzugefügt wurden)
       *       die zu joinenden tabellen und daraus auszulesenden spalten bestimmen
       *      dazu für jede komplexe member (isStorableVariable) genauso behandeln wie hier, also rekursion auf generateMergedClone
       * 
       */
      StorableStructureInformation superRoot = getSuperRootStorableInformation();
      Set<StorableStructureInformation> hierarchy = new HashSet<>();
      if (this instanceof XMOMStorableStructureInformation) {
        hierarchy.add(superRoot);
        superRoot.collectHierarchyRecursivly(hierarchy);  
      } else {
        StorableStructureInformation currentRoot = this;
        while (currentRoot != null) {
          hierarchy.add(currentRoot);
          if (currentRoot.getSuperEntry() == null) {
            currentRoot = null;
          } else {
            currentRoot = currentRoot.getSuperEntry().getInfo();
          }
        }
        collectHierarchyRecursivly(hierarchy);  
      }
      
      Iterator<StorableStructureInformation> hierarchyIter = hierarchy.iterator();
      while (hierarchyIter.hasNext()) {
        // this is not quite accurate as we could(should?) abandon scanning the hierarchy if we encounter an unreachable revision
        if (!mergeFilter.accept(hierarchyIter.next())) {
          hierarchyIter.remove();
        }
      }
      
      StorableStructureInformation merger = superRoot.clone();
      merger.superEntry = null;
      merger.subEntries = Collections.emptySet();
      merger.columnInformation = new HashMap<>();
      // persistenceAccess class as requested class...is this correct or should it dispatch?
      merger.storableClassLoader = this.storableClassLoader;
      for (StorableStructureInformation entry : hierarchy) {
        for (String columnName : entry.columnInformation.keySet()) {
          StorableColumnInformation column = entry.columnInformation.get(columnName);
          merger.columnInformation.put(columnName, column.clone(merger));
        }
      }
      
      for (StorableColumnInformation column : merger.columnInformation.values()) {
        if (column.isStorableVariable()) {
          StorableStructureInformation mergedInfo = column.correspondingStorable.getInfo().generateMergedClone(mergeFilter);
          column.correspondingStorable = new DirectStorableStructureIdentifier(mergedInfo);
        }
      }
      
      if (superRoot.isSynthetic) {
        merger.resultSetReaderForDatatype = this.resultSetReaderForDatatype;
        merger.persistenceAccessDelegator = this.persistenceAccessDelegator;
      } else {
        merger.resultSetReaderForDatatype = new StorableTypDecisionReader(superRoot.getColInfoByVarType(VarType.TYPENAME).getColumnName(), hierarchy);
        merger.persistenceAccessDelegator = new PersistenceAccessDelegator(hierarchy);
      }
      
      return merger;
    }


    private void collectHierarchyRecursivly(Set<StorableStructureInformation> hierarchy) {
      if (getSubEntries() != null) {
        for (StorableStructureIdentifier subEntry : getSubEntries()) {
          hierarchy.add(subEntry.getInfo());
          subEntry.getInfo().collectHierarchyRecursivly(hierarchy);
        }
      }
    }


    public PersistenceAccessDelegator getPersistenceAccessDelegator() {
      return persistenceAccessDelegator;
    }

    public Set<StorableStructureInformation> getSubEntriesRecursivly() {
      Set<StorableStructureInformation> subs = new TreeSet<>(STRUCTURE_INFO_COMPARATOR);
      if (getSubEntries() != null) {
        for (StorableStructureIdentifier sub : getSubEntries()) {
          subs.add(sub.getInfo());
          subs.addAll(sub.getInfo().getSubEntriesRecursivly());
        }
      }
      return subs;
    }
    
    public boolean isAbstract() {
      return isAbstract;
    }
    
  }
  
  interface MergeFilter {
    
    public boolean accept(StorableStructureInformation possibleMerge);
    
  }
  
  
  public static class RevisionBasedMergeFilter /*extends AbstractMergeFilter*/ implements MergeFilter {
    
    private final Set<Long> reachableRevisions;
    
    public RevisionBasedMergeFilter(Long rootRevision) {
      reachableRevisions = new HashSet<>();
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(rootRevision, reachableRevisions);
      reachableRevisions.add(rootRevision);
    }

    public boolean accept(StorableStructureInformation possibleMerge) {
      return reachableRevisions.contains(possibleMerge.getDefiningRevision());
    }
    
  }
  
  public static class AcceptAllMergeFilter /*extends AbstractMergeFilter*/ implements MergeFilter {
    public boolean accept(StorableStructureInformation possibleMerge) {
      return true;
    }
  }
  
  
  
  public static interface StorableStructureVisitor {
    
    void enter(StorableColumnInformation columnLink, StorableStructureInformation current);
    
    void exit(StorableColumnInformation columnLink, StorableStructureInformation current);
    
    StorableStructureRecursionFilter getRecursionFilter();
    
  }
  
  
  public static interface StorableStructureRecursionFilter {
    
    boolean accept(StorableColumnInformation columnLink);
    
    boolean acceptHierarchy(StorableStructureInformation declaredType);
    
  }
  
  
  public static final StorableStructureRecursionFilter ALL_RECURSIONS = new StorableStructureRecursionFilter() {
    
    public boolean accept(StorableColumnInformation columnLink) {
      return true;
    }
    
    public boolean acceptHierarchy(StorableStructureInformation declaredType) {
      return false;
    }
    
  };
  
  
  public static final StorableStructureRecursionFilter ALL_RECURSIONS_AND_FULL_HIERARCHY = new StorableStructureRecursionFilter() {
    
    public boolean accept(StorableColumnInformation columnLink) {
      return true;
    }
    
    public boolean acceptHierarchy(StorableStructureInformation declaredType) {
      return true;
    }
    
  };
  

  public static class XMOMStorableStructureInformation extends StorableStructureInformation {

    protected PersistenceInformation persistenceInfo;
    protected boolean usesHistorization;
    protected long deploymentId;
    
    private String preMergeOriginalXmlName;
    private Long preMergeRevision;
    

    private XMOMStorableStructureInformation() {
      super();
      this.deploymentId = DeploymentManagement.getInstance().getLatestDeploymentId();
    }
    
    public XMOMStorableStructureInformation(DOM xmomStorable, StructureGenerationContext sgc) {
      super(xmomStorable, xmomStorable, null, false, sgc);
      this.persistenceInfo = xmomStorable.getPersistenceInformation();
      this.deploymentId = DeploymentManagement.getInstance().getLatestDeploymentId();
    }


    public boolean usesHistorization() {
      return usesHistorization;
    }
    
    
    // TODO cache me (invalidate, when?) 
    public List<StorableColumnInformation> getPossibleReferences() {
      List<StorableColumnInformation> referencesToMe =
        new ArrayList<StorableColumnInformation>();
      Set<Long> parents = new HashSet<Long>();
      parents.add(getDefiningRevision());      
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(getDefiningRevision(), parents);
      for (Long rev : parents) {
        Collection<XMOMStorableStructureInformation> all = XMOMStorableStructureCache.getInstance(rev).storableStructureInformation.values();
        for (XMOMStorableStructureInformation structure : all) {
          for (StorableColumnInformation aColumn : structure.getColumnInfo(ColumnInfoRecursionMode.ONLY_LOCAL_TO_XMOMSTORABLE)) {
            if (aColumn.isStorableVariable() && 
                aColumn.getStorableVariableType() == StorableVariableType.REFERENCE &&
                aColumn.getStorableVariableInformation().getTableName().equals(this.getTableName())) {
              referencesToMe.add(aColumn);
            }
          }
        }
      }
      return referencesToMe;
    }

    
    /**
     * erneuert alle referenzen auf klassen in diesem objekt und allen nicht-referenziellen kindern rekursiv.
     * referenzen auf klassen sind/haben:
     * - {link {@link StorableStructureInformation#storableClazz}
     * - {link {@link StorableStructureInformation#resultSetReaderForDatatype}
     * - {link {@link StorableStructureInformation#transformDatatypeToStorableMethod}
     */
    public void invalidateClassReferencesRecursively() {
      try {
        unregisterStorables();
      } finally {
        this.traverse(new StorableStructureVisitor() {

          public StorableStructureRecursionFilter getRecursionFilter() {
            return new StorableStructureRecursionFilter() {

              public boolean accept(StorableColumnInformation columnLink) {
                return columnLink.getStorableVariableType() == StorableVariableType.EXPANSION;
              }

              public boolean acceptHierarchy(StorableStructureInformation declaredType) {
                return true;
              }
              
              
            };
          }


          public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
            // ntbd
          }


          public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
            current.invalidateClassInformation();
          }
        });
      }
    }


    public void unregisterStorables() {
      List<Class<? extends Storable<?>>> storableClasses = collectStorableClasses();
      MultipleExceptionHandler<PersistenceLayerException> hpl = new MultipleExceptionHandler<>();
      for (Class<? extends Storable<?>> c : storableClasses) {
        try {
          ODSImpl.getInstance().unregisterStorable(c);
        } catch (PersistenceLayerException e) {
          hpl.addException(e);
        } catch (Error e) {
          hpl.addError(e);
        } catch (RuntimeException e) {
          hpl.addRuntimeException(e);
        }
      }
      try {
        hpl.rethrow();
      } catch (PersistenceLayerException | MultipleExceptions e) {
        throw new RuntimeException(e);
      }
    }

    public void registerStorables() {
      List<Class<? extends Storable<?>>> storableClasses = collectStorableClasses();
      for (Class<? extends Storable<?>> c : storableClasses) {
        try {
          ODSImpl.getInstance().registerStorable(c);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("", e);
        }
      }
    }

    private List<Class<? extends Storable<?>>> collectStorableClasses() {
      List<Class<? extends Storable<?>>> classes = new ArrayList<Class<? extends Storable<?>>>();
      collectStorableClassesRecursively(this, classes);
      return classes;
    }


    private static void collectStorableClassesRecursively(StorableStructureInformation storableStructureInfo,
                                                          List<Class<? extends Storable<?>>> classes) {
      if (storableStructureInfo.getStorableClass() == null) {
        logger.warn("No generated storable class found for " + storableStructureInfo.fqClassNameOfDatatype);
      } else {
        classes.add(storableStructureInfo.getStorableClass());        
      }
      for (StorableColumnInformation col : storableStructureInfo.getAllRelevantStorableColumns()) {
        if (col.isStorableVariable() && 
            col.getStorableVariableType() == StorableVariableType.EXPANSION) {
          collectStorableClassesRecursively(col.getStorableVariableInformation(), classes);
        }
      }
    }
    
    
    public XMOMStorableStructureInformation clone() {
      XMOMStorableStructureInformation clone = new XMOMStorableStructureInformation();
      clone.usesHistorization = usesHistorization;
      clone.revision = revision;
      clone.definingRevision = definingRevision;
      clone.originalXmlName = originalXmlName;
      clone.tableName = tableName;
      clone.columnInformation = new HashMap<>();
      for (String columnName : columnInformation.keySet()) {
        StorableColumnInformation column = columnInformation.get(columnName);
        clone.columnInformation.put(columnName, column.clone(clone));
      }
      clone.primaryKeyName = primaryKeyName;
      clone.storableClazz = storableClazz;
      clone.resultSetReaderForDatatype = resultSetReaderForDatatype;
      clone.transformDatatypeToStorableMethod = transformDatatypeToStorableMethod;
      clone.parent = parent;
      clone.isExpansiveList = isExpansiveList;
      clone.isSynthetic = isSynthetic;
      clone.fqClassNameOfStorableContainingDatatype = fqClassNameOfStorableContainingDatatype;
      clone.fqClassNameOfDatatype = fqClassNameOfDatatype;
      clone.storableClassLoader = storableClassLoader;
      return clone;
    }

    public Collection<JavaSourceFromString> getStorableSourceRecursivly() {
      final Collection<JavaSourceFromString> sources = new ArrayList<>();
      traverse(new StorableStructureVisitor() {
        
        @Override
        public StorableStructureRecursionFilter getRecursionFilter() {
          return ALL_RECURSIONS_AND_FULL_HIERARCHY;
        }
        
        @Override
        public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
        }
        
        @Override
        public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
          if (current.getStorableSource() == null) {
           System.out.println("No code?! For " + current.getFqClassNameForDatatype() + " @" + getRevision()); 
          } else {
            sources.add(current.getStorableSource());
          }
        }
      });
      return sources;
    }
    
    public PersistenceInformation getPersistenceInformation() {
      return persistenceInfo;
    }
    
    public XMOMStorableStructureInformation generateMergedClone(Long rootRevision) {
      XMOMStorableStructureInformation xssi = (XMOMStorableStructureInformation) generateMergedClone(new RevisionBasedMergeFilter(rootRevision));
      xssi.preMergeOriginalXmlName = originalXmlName;
      xssi.preMergeRevision = revision;
      return xssi;
    }
    
    public XMOMStorableStructureInformation generateMergedClone() {
      XMOMStorableStructureInformation xssi =  (XMOMStorableStructureInformation) generateMergedClone(new AcceptAllMergeFilter());
      xssi.preMergeOriginalXmlName = originalXmlName;
      xssi.preMergeRevision = revision;
      return xssi;
    }
    
    public XMOMStorableStructureInformation reresolveMergedClone() {
      if (isMergedClone()) {
        return XMOMStorableStructureCache.getInstance(preMergeRevision).getStructuralInformation(preMergeOriginalXmlName);
      } else {
        return this;
      }
    }
    
    private boolean isMergedClone() {
      return preMergeOriginalXmlName != null;
    }
    
  }

  public static enum VarDefinitionSite {
    DATATYPE, STORABLE, BOTH;
  }
  
  public static enum VarType {
    PK, REFERENCE_FORWARD_FK, EXPANSION_PARENT_FK, LIST_IDX, DEFAULT, UTILLIST_PARENT_FK,
    /**
     * helferspalte für uniqueness, die nach aussen nicht sichtbar ist (uniqueness in verbindung mit historization)
     */
    UNIQUE_HELPER_COL, // TODO there can be multiple UNIQUE_HELPER_COLs but they are indexed uniquely in the strcuture info!
    TYPENAME;
  }

  public static class StorableColumnInformation implements Comparable<StorableColumnInformation> {
        
    private String variableName; 
    public String columnName;
    private String path; //bei geflachten typen der pfad vom xmom object aus, welches zum parentstorable gehört.
    private VarType type = VarType.DEFAULT;
    private Set<PersistenceTypeInformation> typeInformation;
    //private StorableStructureIdentifier parent;
    // TODO would be nice, but a call during generateMegeredClone would need to flatten referencedParents to direct
    private StorableStructureInformation parent;
    private PrimitiveType simpleType; //primitive type of column oder null (blob). auch bei primitiven listen gesetzt, um sie unterscheiden zu können von anderen listen
    /**
     * nicht bei referenzen gesetzt.
     * aber auch bei primitiven listen gesetzt - zeigt dann auf die helper-storable-structure
     */
    private StorableStructureIdentifier correspondingStorable;
    private VarDefinitionSite definedIn;
    
    private boolean reference = false; //expansiv oder reference
    private boolean isList = false;
    private boolean isCycle = false;
    private Restrictions restrictions;
    private String className;
    /**
     * belegt, falls this eine datentyp-column ist, die referenziert auf ein anderes xmom storable zeigt.
     * enthält {@link #correspondingReferenceIdColumn} die col-info der storable-column, die dem FK entspricht.<br>
     * bei listenwertigen referenzen ist dies nicht eine col-info des gleichen parent-types, sondern eine, des 
     * util-storables. 
     */
    private String correspondingReferenceIdColumnName;
    private StorableColumnInformation correspondingReferencedIdColumn;
    private Method setterMethod;
    private StorableColumnInformation correspondingUniqueIdentifier;

    StorableColumnInformation(String variableName, String columnName, Set<PersistenceTypeInformation> typeInformation,
                              StorableStructureInformation enclosing) {
      this.variableName = variableName;
      this.columnName = columnName;
      this.typeInformation = typeInformation == null ? new HashSet<PersistenceTypeInformation>() : typeInformation;
      //this.parent = identifierOf(enclosing);
      this.parent = enclosing;
    }


    protected static StorableColumnInformation createXMOMStorableColumnInformation(AVariable var,
                                                                                   StorableStructureInformation enclosing,
                                                                                   StructureGenerationContext sgc) throws PersistenceLayerException {
      Set<PersistenceTypeInformation> types = var.getPersistenceTypes();
      if (types == null || !(enclosing instanceof XMOMStorableStructureInformation)) {
        types = new HashSet<PersistenceTypeInformation>();
      } else {
        types = new HashSet<PersistenceTypeInformation>(types);
      }
      String colName;
      if (var.isJavaBaseType() && 
          !var.isList()) {
        colName = sgc.createUniqueColNameFor(var.getVarName(), var.isList());
      } else {
        colName = var.getVarName().toLowerCase();
      }
      StorableColumnInformation sci = new StorableColumnInformation(var.getVarName(), colName, types, enclosing);
      
      sci.isList = var.isList();
      if (var instanceof DatatypeVariable) {
        DatatypeVariable dVar = (DatatypeVariable) var;
        sci.restrictions = dVar.getRestrictions();
      }
      sci.className = var.getEventuallyQualifiedClassNameWithGenerics(Collections.<String>emptySet(), false);
      return sci;
    }

    
    public String getVariableName() {
      return variableName;
    }
    

    public String getColumnName() {
      return columnName;
    }


    public boolean isStorableVariable() {
      return correspondingStorable != null;
    }


    public StorableVariableType getStorableVariableType() {
      if (!isStorableVariable()) {
        throw new RuntimeException();
      }
      return reference ? StorableVariableType.REFERENCE : StorableVariableType.EXPANSION;
    }


    public StorableStructureInformation getStorableVariableInformation() {
      return correspondingStorable.getInfo();
    }
    
    
    public void setStorableVariableInformation(StorableStructureIdentifier correspondingStorable) {
      this.correspondingStorable = correspondingStorable;
    }


    public StorableStructureInformation getParentStorableInfo() {
      //return parent.getInfo();
      return parent;
    }


    public boolean isList() {
      return isList;
    }
    
    // TODO remove classname? 
    public String getClassName() {
      if (className != null) {
        return className; 
      } else if (simpleType != null) {
        return simpleType.getClassOfType();
      } else {
        return null;
      }
    }
    
    /**
     * ist die variable nur im datentyp definiert oder nur im storable oder in beidem?
     * @return
     */
    public VarDefinitionSite getDefinitionSite() {
      return definedIn;
    }
    
    
    public boolean isPersistenceType(PersistenceTypeInformation type) {
      return typeInformation != null && typeInformation.contains(type);
    }
    
    
    public VarType getType() {
      return type;
    }

    /**
     * belegt, falls this die datentyp-column ist, die referenziert auf ein anderes xmom storable zeigt.
     * @return gibt die col-info der storable-column, die dem FK entspricht zurück
     */
    public StorableColumnInformation getCorrespondingReferenceIdColumn() {
      return parent.getColumnInfoByName(correspondingReferenceIdColumnName);
    }
    
    /**
     * belegt, falls this die UNIQUEHelper_COL ist die erzeugt wird für weitere UniqueIndizes bei historisierung
     */
    public StorableColumnInformation getCorrespondingUniqueIdentifierColumn() {
      return correspondingUniqueIdentifier;
    }

    /**
     * storableclass-settermethode für diese variable/column 
     */
    public Method getStorableSetter() {
      if (setterMethod == null) {
        //setterMethod = getStorableSetter(parent.getInfo().getStorableClass());
        setterMethod = getStorableSetter(parent.getStorableClass());
      }
      return setterMethod;
    }
    
    @SuppressWarnings("unchecked")
    public Method getStorableSetter(Class<? extends Storable<?>> storableClass) {
      Class<? extends Storable<?>> currentStorableClass = storableClass;
      // ugly but ant-compile won't allow direct comparison of classes 
      while (!currentStorableClass.getName().equals(Storable.class.getName())) {
        Method[] declaredMethods = currentStorableClass.getDeclaredMethods();
        for (Method m : declaredMethods) {
          String v = variableName;
          if (v == null) {
            v = columnName;
          }
          if (m.getName().equalsIgnoreCase(GenerationBase.buildSetter(v))) {
            return m;
          }
        }
        currentStorableClass = (Class<? extends Storable<?>>) currentStorableClass.getSuperclass();
      }
      return null;
    }
    
    private String getPathForXynaObjectAccess() {
      if (path != null) {
        return path;
      }
      return variableName;
    }
    
    public Object getFromDatatype(XynaObject datatype) {
      try {
        return datatype.get(getPathForXynaObjectAccess());
      } catch (InvalidObjectPathException e) {
        //throw new RuntimeException(e);
        // TODO hmm...this is ugly
        // because we merge the hierarchy we might try to access vars that are not accessible within a concrete type
        // should we have created a more dynamic merged access or always dispached access to a certain type?
        // for now return null
        return null;
      }
    }
    
    public void setInDatatype(XynaObject datatype, Object value) {
      try {
        XynaObject.set(datatype, getPathForXynaObjectAccess(), value);
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND | InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
    }
    
    public PrimitiveType getPrimitiveType() {
      return simpleType;
    }


    public String getPath() {
      return path;
    }


    public boolean isFlattened() {
      return path != null;
    }


    public XMOMStorableStructureInformation getParentXMOMStorableInformation() {
      StorableStructureInformation parent = getParentStorableInfo();
      while (!(parent instanceof XMOMStorableStructureInformation)) {
        parent = parent.getPossessingColumn().getParentStorableInfo();
      }
      return (XMOMStorableStructureInformation) parent;
    }
    
    
    public StorableColumnInformation clone() {
      return clone(parent);
    }
    
    public StorableColumnInformation clone(StorableStructureInformation parent) {
      StorableColumnInformation clone = new StorableColumnInformation(variableName, columnName, typeInformation, parent);
      clone.path = path;
      clone.type = type;
      clone.simpleType = simpleType;
      if (correspondingStorable != null) {
        clone.correspondingStorable = correspondingStorable.clone(this, new HashMap<>());
      }
      clone.definedIn = definedIn;
      clone.reference = reference;
      clone.isList = isList;
      clone.isCycle = isCycle;
      clone.correspondingReferenceIdColumnName = correspondingReferenceIdColumnName;
      clone.setterMethod = setterMethod;
      clone.restrictions = restrictions;
      return clone;
    }

    public StorableColumnInformation getCorrespondingReferencedIdColumn() {
      return correspondingReferencedIdColumn;
    }


    public Restrictions getRestrictions() {
      return restrictions;
    }


    /**
     * sortiert nach:  
     *   1) falls columnname==null =&gt; name der tabelle, die der spalte zugeordnet ist
     *   2) nach owner-tabellenname
     *   3) ansonsten nach columnname +X
     *   
     * +X, für den fall von kollisionen (subtypen mit gleichnamigen spalten, oder subtypen aus unterschiedlichen runtimecontexten)
     */
    @Override
    public int compareTo(StorableColumnInformation o) {
      if (equals(o)) {
        return 0;
      }
      int c;
      if (columnName == null) {
        if (o.columnName == null) {
          c = correspondingStorable.getInfo().getTableName().compareTo(o.correspondingStorable.getInfo().getTableName());
          return c;        
        }
        return 1;
      } else if (o.columnName == null) {
        return -1;
      }
      String tableName = parent.getTableName();
      String tableNameOther = o.parent.getTableName();
      c = tableName.compareTo(tableNameOther);
      if (c != 0) {
        return c; //sollte eigtl nicht vorkommen, dass columns von verschiedenen tabellen gemischt werden, aber so ist man auf der sicheren seite
      }

      c = columnName.compareTo(o.columnName);
      if (c == 0) {
        String x = parent.getDefiningRevision() + parent.getFqXmlName();
        String x2 = o.parent.getDefiningRevision() + o.parent.getFqXmlName();
        return x.compareTo(x2);
      }
      return c;
    }
    
  }


  public static enum StorableVariableType {
    REFERENCE, EXPANSION;
  }

  
  public static class StructureGenerationContext {
    
    //root-storable -> path -> path-ende-DOM (vereinigung aller subtypen, die sich beim path befinden können)
    //abhängig von verwendung wird die innerste map unterschiedlich befüllt
    public Map<DOM, Map<String, Map<DOM, StorableStructureIdentifier>>> visitedExtensionHierarchy;
    public GenerationBaseCache gbc;
    public PathBuilder pb;
    
    StructureGenerationContext() {
      visitedExtensionHierarchy = new HashMap<>();
      gbc = new GenerationBaseCache(); //FIXME alten gbc wiederverwenden
      pb = new PathBuilder();
    }
    
    public String createUniqueColNameFor(String varName) throws PersistenceLayerException {
      return createUniqueColNameFor(varName, false);
    }
    
    public String createUniqueColNameFor(String varName, boolean isList) throws PersistenceLayerException {
      pb.enter(varName, isList);
      String result = XMOMODSMappingUtils.findUniqueName(pb, NameType.COLUMN);
      pb.exit();
      return result;
    }
    
    public String createUniqueTableNameFor() throws PersistenceLayerException {
      return XMOMODSMappingUtils.findUniqueName(pb, NameType.TABLE);
    }
    
  }
  
  
  public interface StorableStructureIdentifier {
    
    StorableStructureInformation getInfo();
    
    StorableStructureIdentifier clone(StorableColumnInformation possesingColumn, Map<StorableStructureIdentifier, StorableStructureIdentifier> alreadyCloned);
    
  }
  
  
  public static StorableStructureIdentifier identifierOf(StorableStructureInformation info) {
    if (info instanceof XMOMStorableStructureInformation) {
      return new ReferenceStorableStructureIdentifier(info.getDefiningRevision(), info.getFqXmlName(), info.getFqClassNameForDatatype());
    } else {
      return new DirectStorableStructureIdentifier(info);
    }
  }
  //direct==expansiv (dann ist das eigtl ein wrapper, kein identifier)
  public static class DirectStorableStructureIdentifier implements StorableStructureIdentifier {

    private final StorableStructureInformation info;
    
    public DirectStorableStructureIdentifier(StorableStructureInformation info) {
      this.info = info;
    }
    
    public StorableStructureInformation getInfo() {
      return info;
    }

    public StorableStructureIdentifier clone(StorableColumnInformation possesingColumn,
                                             Map<StorableStructureIdentifier, StorableStructureIdentifier> alreadyCloned) {
      return new DirectStorableStructureIdentifier(info.clone(possesingColumn, alreadyCloned));
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj != null &&
          obj instanceof DirectStorableStructureIdentifier) {
        return info.equals(((DirectStorableStructureIdentifier)obj).info);
      } else {
        return false;
      }
      
    }
    
    @Override
    public int hashCode() {
      return info.hashCode();
    }
    
  }
  
  
  public static class ReferenceStorableStructureIdentifier implements StorableStructureIdentifier {
    
    private long referenceRevision;
    public String fqOriginalName;
    public String fqClassName;
    
    
    ReferenceStorableStructureIdentifier(long referenceRevision, String fqOriginalName, String fqClassName) {
      this.referenceRevision = referenceRevision;
      this.fqOriginalName = fqOriginalName;
      this.fqClassName = fqClassName;
    }

    public StorableStructureInformation getInfo() {
      long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(fqOriginalName, referenceRevision);
      return getInstance(revision).getStructuralInformation(fqClassName);
    }

    @Override
    public StorableStructureIdentifier clone(StorableColumnInformation possesingColumn,
                                             Map<StorableStructureIdentifier, StorableStructureIdentifier> alreadyCloned) {
      return new ReferenceStorableStructureIdentifier(referenceRevision, fqOriginalName, fqClassName);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj != null &&
          obj instanceof ReferenceStorableStructureIdentifier) {
        ReferenceStorableStructureIdentifier other = (ReferenceStorableStructureIdentifier) obj;
        return Objects.equals(this.fqOriginalName, other.fqOriginalName) &&
               Objects.equals(this.fqClassName, other.fqClassName) &&
               Objects.equals(this.referenceRevision, other.referenceRevision);
      } else {
        return false;
      }
      
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(fqOriginalName, fqClassName, referenceRevision);
    }
  }
  
  
  public static boolean isTransient(DOM xmomStorableRoot, AVariable var, String path) {
    return (var.getPersistenceTypes() != null && var.getPersistenceTypes().contains(PersistenceTypeInformation.TRANSIENCE))
        || (xmomStorableRoot.getPersistenceInformation().getTransients().contains(path));
  }


  public static boolean isFlat(DOM xmomStorableRoot, String path) {
    if (xmomStorableRoot.getPersistenceInformation().getFlatExclusions().contains(path)) {
      return false;
    }
    return xmomStorableRoot.getPersistenceInformation().getFlattened().contains(path);
  }
  
  private boolean isExludedFromFlat(DOM xmomStorableRoot, String path) {
    return xmomStorableRoot.getPersistenceInformation().getFlatExclusions().contains(path);
  }

  public static boolean isStorableReference(DOM xmomStorableRoot, String path) {
    return xmomStorableRoot.getPersistenceInformation().getReferences().contains(path);
  }
  
  
  public static final Comparator<StorableStructureIdentifier> STRUCTURE_ID_COMPARATOR = new Comparator<>() {

    public int compare(StorableStructureIdentifier ssi1, StorableStructureIdentifier ssi2) {
      return Objects.compare(ssi1.getInfo(),
                             ssi2.getInfo(),
                             STRUCTURE_INFO_COMPARATOR);
    }

  };
  
  
  public static final Comparator<StorableStructureInformation> STRUCTURE_INFO_COMPARATOR = 
                  Comparator.comparingLong(StorableStructureInformation::getRevision)
                            .thenComparingLong(StorableStructureInformation::getDefiningRevision)
                            .thenComparing(StorableStructureInformation::getFqClassNameForStorable);
  
}
