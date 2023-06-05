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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMObjectSet;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMState;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


class RelationManagementMethods {
  
  
  private static final Logger logger = CentralFactoryLogging.getLogger(RelationManagementMethods.class);
  
  /*
   * Template Method
   */
  static abstract class UpdateBackwardRelationsTemplateMethod<F extends XMOMDatabaseEntry, B extends XMOMDatabaseEntry> {
    
    private RuntimeContextDependencyManagement rcdm;

    protected abstract String retrieveForwardRelationsFromStorable(F entry);

    /**
     * müssen immer in der gleichen reihenfolge zurückgegeben werden.
     * DATATYPE, EXCEPTION; WORKFLOW, OPERATION
     * @return
     */
    protected abstract List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects();

    protected abstract void modifyRelations(F cacheEntryContainingNewForwardRelations, B cacheEntryContainingOldBackwardRelations);

    
    public void execute(ODSConnection con, F entryToProcess, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException {
      // retrieve primary keys of forward relations
      String forwardRelations = retrieveForwardRelationsFromStorable(entryToProcess);
      if (forwardRelations == null || forwardRelations.equals("")) { 
        return;
      }
      List<String> primaryKeysOfObjectsFromForwardRelation = Arrays
                      .asList(forwardRelations.split(XMOMDatabaseEntry.SEPERATION_MARKER));
      if (primaryKeysOfObjectsFromForwardRelation == null || primaryKeysOfObjectsFromForwardRelation.size() == 0) {
        return;
      }
      RuntimeContextDependencyManagement rcdm_local = rcdm;
      if (rcdm_local == null) {
        rcdm_local = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        if (!XynaFactory.getInstance().isStartingUp()) {
          rcdm = rcdm_local;
        }
      }
      
      Collections.sort(primaryKeysOfObjectsFromForwardRelation); //immer gleiche reihenfolge von objekten
      
      // some relations might affect different archives, retrieve XMOMDatabaseTypes per PK
      List<XMOMDatabaseType> XMOMDatabaseTypes = getXMOMDatabaseTypesContainingBackwardRelatedObjects();

      keyLoop : for (String primaryKey : primaryKeysOfObjectsFromForwardRelation) {
        B entry;
        for (XMOMDatabaseType xmomDatabaseType : XMOMDatabaseTypes) {
          // get a instance from XMOMDatabaseType
          String fqXmlName = primaryKey;
          if (xmomDatabaseType == XMOMDatabaseType.OPERATION) {
            //servicename und operationname hinten abschneiden versuchen
            if (fqXmlName.contains(".")) {
              fqXmlName = GenerationBase.getPackageNameFromFQName(fqXmlName);
              if (fqXmlName.contains(".")) {
                fqXmlName = GenerationBase.getPackageNameFromFQName(fqXmlName);
              } else {
                continue keyLoop;
              }
            } else {
              continue keyLoop;
            }
          }
          Long rev = rcdm_local.getRevisionDefiningXMOMObjectOrParent(fqXmlName, entryToProcess.getRevision());          
          if (!entryToProcess.getRevision().equals(rev)) {
            //rückwärtsbeziehungen in andere revisions nicht eintragen, weil da fehlt uns die möglichkeit, die revision mit zu speichern.
            //wenn objekt noch nicht gefunden werden kann, ist es in der lokalen revision ok. typischer usecase: application-import, weil dort
            //deploymentitemstates erst im cleanup vom deployment behandelt wird, und damit erst nach dem eintrag in die xmomdatabase. 
            continue keyLoop;
          }
          entry = (B) xmomDatabaseType.generateInstanceOfArchiveStorableWithPrimaryKey(primaryKey, rev);
          // query it to restore values
          try {
            con.queryOneRowForUpdate(entry);
            modifyRelations(entryToProcess, entry);
            if (entry.getTimestamp() == null || entry.getTimestamp().equals(XMOMState.missing_xml.getTimestamp())) {
              entry.setTimestamp(XMOMState.missing_xml_but_backward_relations.getTimestamp());
            }
            con.persistObject(entry);
            continue keyLoop;
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            //nächsten typ probieren
          }
        }
        //objekt in keinem xmomdb type gefunden
        handleObjectNotFoundInAllArchives(con, XMOMDatabaseTypes, entryToProcess, primaryKey, xmomObjectsToFinishLater);
      }
    }
    

    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes, F entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xmomObjectsToFinishLater) throws PersistenceLayerException {
      if (allowedXMOMDatabaseTypes.contains(XMOMDatabaseType.DATAMODEL) || allowedXMOMDatabaseTypes.contains(XMOMDatabaseType.SERVICEGROUP)) {
        return;
      }
      //falls das xml nicht existiert, soll das objekt möglichst trotzdem in die xmomdb eingetragen werden, aber dann ohne label.
      //falls das xml existiert, dann soll es korrekt/vollständig eingetragen werden
      XMOMDatabaseType archive = null;
      String rootTag = null;
      String fqName = primaryKeyOfUnregisteredObject;
      try {
        try {
          rootTag = GenerationBase.retrieveRootTag(primaryKeyOfUnregisteredObject, entryToProcess.getRevision(), false, false);
        } catch (Ex_FileAccessException e) {
          if (allowedXMOMDatabaseTypes.contains(XMOMDatabaseType.OPERATION)) {
            //cut away .Service.operation
            fqName = primaryKeyOfUnregisteredObject;
            boolean ok = true;
            for (int i = 0; i < 2; i++) {
              int idxOfDot = fqName.lastIndexOf(".");
              if (idxOfDot <= 0) {
                ok = false;
                //xml not found
                break;
              }
              fqName = fqName.substring(0, idxOfDot);
            }

            if (ok) {
              try {
                rootTag = GenerationBase.retrieveRootTag(fqName, entryToProcess.getRevision(), false, false);
              } catch (Ex_FileAccessException e2) {
                //xml not found
              }
            }
          } else {
            //xml not found
          }
        }
        if (rootTag != null) {
          List<XMOMDatabaseType> possibleXMOMDatabaseTypes =
              XMOMDatabaseType.getXMOMDatabaseTypeByXMOMType(XMOMType.getXMOMTypeByRootTag(rootTag));
          possibleXMOMDatabaseTypes.retainAll(allowedXMOMDatabaseTypes);
          if (possibleXMOMDatabaseTypes.size() != 1) {
            throw new RuntimeException("This should not be possible. Found types: " + Arrays.toString(possibleXMOMDatabaseTypes.toArray())
                + " for " + primaryKeyOfUnregisteredObject);
          }
          archive = possibleXMOMDatabaseTypes.get(0);
        } else {
          //trägt evtl in datatype ein, anstatt exception...
          archive = allowedXMOMDatabaseTypes.get(0);
        }
      } catch (Throwable e) {
        Department.handleThrowable(e);
        logger.warn("could not create xmomdb entry for " + primaryKeyOfUnregisteredObject, e);
        return;
      }
      B entry = (B) archive.generateInstanceOfArchiveStorableWithPrimaryKey(primaryKeyOfUnregisteredObject, entryToProcess.getRevision());
      if (rootTag != null) {
        switch (archive) {
          case DATATYPE :
          case EXCEPTION :
          case WORKFLOW :
          case OPERATION :
            if (xmomObjectsToFinishLater != null) {
              xmomObjectsToFinishLater.add(fqName, rootTag);
            }
            break;          
          case FORMDEFINITION :
            //gibt keine forwardrelations auf forms -> nichts zu tun
          case SERVICEGROUP :
          case DATAMODEL :
            //oben behandelt
            
          case SERVICE :
          case GENERIC :
            //oberklassen
            return;
        }
      }
      modifyRelations(entryToProcess, entry);
      con.persistObject(entry);
      //FIXME: was, wenn objekt bereits vorhanden war! unerwartet -> rollback und nochmal? geht nicht gut wegen der schleife über die objekte...
    }
  
  }
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertPossessedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomOrExceptionDatabaseEntry entry) {
      return entry.getPossesses();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMDomOrExceptionDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedPossessedByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getPossessedBy(),
                                                             cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setPossessedBy(updatedPossessedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertExtendedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomOrExceptionDatabaseEntry entry) {
      return entry.getExtends();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMDomOrExceptionDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedExtendedByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getExtendedBy(),
                                                            cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setExtendedBy(updatedExtendedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertNeededByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getNeeds();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedNeededByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getNeededBy(),
                                                            cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setNeededBy(updatedNeededByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertProducedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getProduces();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedProducedByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getProducedBy(),
                                                            cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setProducedBy(updatedProducedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMExceptionDatabaseEntry> insertThrownByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getExceptions();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedThrownByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getThrownBy(),
                                                            cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setThrownBy(updatedThrownByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMServiceDatabaseEntry> insertCalledByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMServiceDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getCalls();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.WORKFLOW, XMOMDatabaseType.OPERATION});
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMServiceDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedCalledByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getCalledBy(),
                                                            cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setCalledBy(updatedCalledByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertInstancesUsedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getUsesInstancesOf();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedInstancesUsedByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getInstancesUsedBy(),
                                                                 cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setInstancesUsedBy(updatedInstancesUsedByRelation);
    }
  };

  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDataModelDatabaseEntry> insertDataModelsUsedByRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDataModelDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getUsesDataModels();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATAMODEL});
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDataModelDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedDatamodelUsedByRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getUsedBy(),
                                                                 cacheEntryContainingNewForwardRelations.getId());
      cacheEntryContainingOldBackwardRelations.setUsedBy(updatedDatamodelUsedByRelation);
    }
  };

  final static UpdateBackwardRelationsTemplateMethod<XMOMDomDatabaseEntry, XMOMWorkflowDatabaseEntry> insertInstanceServiceReferenceOfRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMDomDatabaseEntry, XMOMWorkflowDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomDatabaseEntry entry) {
      return entry.getInstanceServiceReferences();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.WORKFLOW});
    }
    @Override
    protected void modifyRelations(XMOMDomDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMWorkflowDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedInstanceServiceReferenceOfRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getInstanceServiceReferenceOf(),
                                                                       cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setInstanceServiceReferenceOf(updatedInstanceServiceReferenceOfRelation);
    }
  };

  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> insertUsedInImplOfRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getImplUses();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedUsedInImplOfRelation = addToSeperatedList(cacheEntryContainingOldBackwardRelations.getUsedInImplOf(),
                                                                       cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setUsedInImplOf(updatedUsedInImplOfRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removePossessedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomOrExceptionDatabaseEntry entry) {
      return entry.getPossesses();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMDomOrExceptionDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMDomOrExceptionDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedPossessedByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getPossessedBy(),
                                                                  cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setPossessedBy(updatedPossessedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removeExtendedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMDomOrExceptionDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomOrExceptionDatabaseEntry entry) {
      return entry.getExtends();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMDomOrExceptionDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMDomOrExceptionDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedExtendedByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getExtendedBy(),
                                                                 cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setExtendedBy(updatedExtendedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removeNeededByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getNeeds();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMServiceDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedNeededByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getNeededBy(),
                                                               cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setNeededBy(updatedNeededByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removeProducedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getProduces();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMServiceDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedProducedByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getProducedBy(),
                                                                 cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setProducedBy(updatedProducedByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMExceptionDatabaseEntry> removeThrownByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getExceptions();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMServiceDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedThrownByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getThrownBy(),
                                                               cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setThrownBy(updatedThrownByRelation);
    }
  };
  
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMServiceDatabaseEntry> removeCalledByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMServiceDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getCalls();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.WORKFLOW, XMOMDatabaseType.OPERATION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMWorkflowDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMServiceDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedCalledByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getCalledBy(),
                                                               cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setCalledBy(updatedCalledByRelation);
    }
  };
  
  
  final static UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removeInstancesUsedByRelations =
    new UpdateBackwardRelationsTemplateMethod<XMOMServiceDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMServiceDatabaseEntry entry) {
      return entry.getUsesInstancesOf();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMServiceDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMServiceDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedInstancesUsedByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getInstancesUsedBy(),
                                                                      cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setInstancesUsedBy(updatedInstancesUsedByRelation);
    }
  };

  
  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDataModelDatabaseEntry> removeDataModelsUsedByRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDataModelDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getUsesDataModels();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATAMODEL});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMWorkflowDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDataModelDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedInstancesUsedByRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getUsedBy(),
                                                                      cacheEntryContainingNewForwardRelations.getId());
      cacheEntryContainingOldBackwardRelations.setUsedBy(updatedInstancesUsedByRelation);
    }
  };

  final static UpdateBackwardRelationsTemplateMethod<XMOMDomDatabaseEntry, XMOMWorkflowDatabaseEntry> removeInstanceServiceReferenceOfRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMDomDatabaseEntry, XMOMWorkflowDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMDomDatabaseEntry entry) {
      return entry.getInstanceServiceReferences();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.WORKFLOW});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMDomDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMDomDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMWorkflowDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedInstanceServiceReferenceOfRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getInstanceServiceReferenceOf(),
                                                                      cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setInstanceServiceReferenceOf(updatedInstanceServiceReferenceOfRelation);
    }
  };

  final static UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDomOrExceptionDatabaseEntry> removeUsedInImplOfRelations =
                  new UpdateBackwardRelationsTemplateMethod<XMOMWorkflowDatabaseEntry, XMOMDomOrExceptionDatabaseEntry>() {
    @Override
    protected String retrieveForwardRelationsFromStorable(XMOMWorkflowDatabaseEntry entry) {
      return entry.getImplUses();
    }
    @Override
    protected List<XMOMDatabaseType> getXMOMDatabaseTypesContainingBackwardRelatedObjects() {
      return Arrays.asList(new XMOMDatabaseType[] {XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION});
    }
    @Override
    protected void handleObjectNotFoundInAllArchives(ODSConnection con, List<XMOMDatabaseType> allowedXMOMDatabaseTypes,
                                                     XMOMWorkflowDatabaseEntry entryToProcess,
                                                     String primaryKeyOfUnregisteredObject, XMOMObjectSet xomObjectsToFinishLater) throws PersistenceLayerException {
      ;// nothing to do in remove cases 
    }
    @Override
    protected void modifyRelations(XMOMWorkflowDatabaseEntry cacheEntryContainingNewForwardRelations,
                                   XMOMDomOrExceptionDatabaseEntry cacheEntryContainingOldBackwardRelations) {
      String updatedUsedInImplOfRelation = removeFromSeperatedList(cacheEntryContainingOldBackwardRelations.getUsedInImplOf(),
                                                                   cacheEntryContainingNewForwardRelations.getFqname());
      cacheEntryContainingOldBackwardRelations.setUsedInImplOf(updatedUsedInImplOfRelation);
    }
  };
  
  
  private static final String addToSeperatedList(String seperatedList, String entry) {
    if (XMOMDatabaseEntry.isValidFQName(entry)) {
      return StringUtils.addToSeperatedList(seperatedList, entry, XMOMDatabaseEntry.SEPERATION_MARKER, true);
    } else {
      return seperatedList;
    }
  }
  
  private static final String removeFromSeperatedList(String seperatedList, String entry) {
    if (XMOMDatabaseEntry.isValidFQName(entry)) {
      return StringUtils.removeFromSeperatedList(seperatedList, entry.trim(), XMOMDatabaseEntry.SEPERATION_MARKER, false);
    } else {
      return seperatedList;
    }
  }
  
  
  
}
