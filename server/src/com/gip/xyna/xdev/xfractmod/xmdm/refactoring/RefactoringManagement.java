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
package com.gip.xyna.xdev.xfractmod.xmdm.refactoring;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMMovementEvent;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringConflict;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringFault;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Configuration;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DecomposedWork;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.DocumentOrderType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Result;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMLRefactoringUtils.Work;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.AutosaveFilter;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_ObjectNotFoundException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItem;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortal.Identity;
import com.gip.xyna.xmcp.XynaMultiChannelPortalBase;
import com.gip.xyna.xmcp.xguisupport.messagebus.PredefinedMessagePath;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.InUse;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;


public class RefactoringManagement extends FunctionGroup {

  public static final String MODIFIED = "MODIFIED";
  public static final String DELETED = "DELETED";
  public static final String SAVED = "SAVED";

  static final Logger logger = CentralFactoryLogging.getLogger(RefactoringManagement.class);


  public enum RefactoringType {
    MOVE, CHANGE;
  }

  public RefactoringManagement() throws XynaException {
    super();
  }


  @Override
  protected void init() throws XynaException {
  }


  @Override
  protected void shutdown() throws XynaException {
  }
  
  
  
  public RefactoringResult refactorXMOM(RefactoringActionParameter action) throws XDEV_RefactoringConflict, XDEV_RefactoringFault {
    XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new Identity(action.getUsername(), action.getSessionId()));
    try {
      RefactoringResult result;
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision;
      try {
        revision = revisionManagement.getRevision(action.getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new IllegalArgumentException("Illegal runtimeContext " + action.getRuntimeContext(), e);
      }
      switch (action.getRefactoringType()) {
        case MOVE :
          RefactoringMoveActionParameter rmap = (RefactoringMoveActionParameter)action;
          switch (rmap.getTargetRootType()) {
            case WORKFLOW :
            case DATATYPE :
            case EXCEPTION :
              result = moveXMOM(rmap);
              break;
            case OPERATION :
              result = moveServiceOperation(rmap.getFqXmlNameOld(), rmap.getFqXmlNameNew(), rmap.getTargetLabel(),
                                            rmap.doForceDeploy(), action.getSessionId(), action.getUsername(),
                                            revision, rmap.getRuntimeContext().getName());
              break;
            case PATH :
              result = movePath(rmap.getFqXmlNameOld(), rmap.getFqXmlNameNew(), rmap.doForceDeploy(),
                                action.getSessionId(), action.getUsername(), revision,
                                rmap.getRuntimeContext().getName());
              break;
            case SERVICE_GROUP :
              // FIXME check if memberVariables or serveral ServiceGroups, if that's the case we move all operations otherwise we move it as DT
              // For now, just move as DT
              rmap.setFqXmlNameOld(rmap.getFqXmlNameOld().substring(0, rmap.getFqXmlNameOld().lastIndexOf('.')));
              rmap.setFqXmlNameNew(rmap.getFqXmlNameNew().substring(0, rmap.getFqXmlNameNew().lastIndexOf('.')));
              result = moveXMOM(rmap);
              break;
            default :
              throw new IllegalArgumentException("Illegal refactorying target type " + rmap.getTargetRootType());
          }
          break;
        default :
          throw new IllegalArgumentException("Illegal refactorying type " + action.getRefactoringType());
      }
      if (ProjectCreationOrChangeProvider.getInstance().listeneresPresent()) {
        Collection<ProjectCreationOrChangeEvent> events = generateEvents(result, action.getRuntimeContext());
        ProjectCreationOrChangeProvider.getInstance().notify(events, revision, false);
      }
      return result;
    } finally {
      XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
    }
  }


  private Collection<ProjectCreationOrChangeEvent> generateEvents(RefactoringResult results, RuntimeContext runtimeContext) {
    long revision = -5;
    Collection<ProjectCreationOrChangeEvent> events = new ArrayList<>();
    for (XMOMObjectRefactoringResult result : results.getRefactoredObjects()) {
      if (result.targetType == RefactoringTargetType.APPLICATION) {
        events.add(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINITION_CHANGE, result.getFqXmlNameOld()));
        continue;
      }
      if (result.refactoringType == RefactoringType.CHANGE) {
        events.add(new XMOMChangeEvent(result.getFqXmlNameNew(), result.getRefactoringTargetType().getXMOMType()));
      } else if (result.refactoringType == RefactoringType.MOVE) {
        if (revision == -5) {
          try {
            revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(runtimeContext);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          }
        }
        events.add(new XMOMMovementEvent(result.getFqXmlNameNew(), result.getFqXmlNameOld(), result.getRefactoringTargetType().getXMOMType(), revision));
      }
    }
    return events;
  }


  private RefactoringResult moveXMOM(RefactoringMoveActionParameter rmap) throws XDEV_RefactoringFault, XDEV_RefactoringConflict {
    String fqXmlNameOld = rmap.getFqXmlNameOld();
    String fqXmlNameNew = rmap.getFqXmlNameNew();
    String newLabel = rmap.getTargetLabel();
    XMOMType typeToMove = rmap.getTargetRootType().getXmomRootType();
    boolean forceDeploy = rmap.doForceDeploy();
    String sessionId = rmap.getSessionId();
    String creator = rmap.getUsername();
    boolean ignoreStorableUsage = rmap.ignoreIncompatibleStorables();
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision;
    try {
      revision = revisionManagement.getRevision(rmap.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
    }
    
    Configuration config = new Configuration(true, true, false);
    Configuration savedConfig = new Configuration(false, true, true);

    File targetFile = savedConfig.getFileLocation(fqXmlNameOld, revision);

    String oldLabel = null;
    String rootElementName = null;
    try {
      XMLStreamReader reader =
          XMLUtils.defaultXmlInputFactory().createXMLStreamReader(new FileInputStream(targetFile));
      outer: while (reader.hasNext()) {
        int event = reader.next();
        switch (event) {
          case XMLStreamConstants.START_ELEMENT :
            rootElementName = reader.getLocalName();
            oldLabel = reader.getAttributeValue(null, GenerationBase.ATT.LABEL);
            break outer;
        }
      }
      if (rootElementName == null) {
        throw new RuntimeException("No root element found");
      }
    } catch (FileNotFoundException | XMLStreamException e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
    }

    XMOMType xmomType = XMOMType.getXMOMTypeByRootTag(rootElementName);
    if (xmomType != typeToMove) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(),
                                      new RuntimeException(fqXmlNameOld + " is a " + xmomType + " instead of a " + typeToMove));
    }

    RefactoringTargetType type = RefactoringTargetType.fromXMOMType(typeToMove);
    RefactoringElement refactoring = new RefactoringElement(fqXmlNameOld, fqXmlNameNew, newLabel, oldLabel, type);
    if (!ignoreStorableUsage && typeToMove == XMOMType.DATATYPE) {
      if (isUsedInStorableHierarchy(Collections.singletonList(refactoring), revision)) {
        throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(),
                                        new RuntimeException(fqXmlNameOld + " is used in a storable hierarchy"));
      }
    }
    
    Work refactoringWorkInSaved = getMoveWork(type, savedConfig, refactoring);
    RefactoringContext context = new RefactoringContext(refactoring, new WorkBasedAutosaveFilter(refactoringWorkInSaved), sessionId, creator, revision);
    RefactoringLock lock = lock(context);
    try {
      //check vorziehen, ob ziel objekt im saved-dir bereits existiert. wenn das dort bereits existierende ziel-objekt
      //bereits existiert, nur nicht deployed ist, merkt man es hier nicht.
      checkExistenceInSavedDir(fqXmlNameNew, fqXmlNameOld, typeToMove, revision);
      RefactoringResult resultDeployed = executeRefactoring(refactoring.fqXmlNameOld, getMoveWork(type, config, refactoring), forceDeploy, config, revision, context);
      boolean success = false;
      try {
        RefactoringResult resultSaved = executeRefactoring(refactoring.fqXmlNameOld, refactoringWorkInSaved, forceDeploy, savedConfig, revision, context);
        success = true;
        publishRefactoringResult(creator, resultSaved, rmap.getRuntimeContext().getName());
        return mergeResults(resultDeployed, resultSaved);
      } finally {
        if (!success) {
          try {
            rollbackRefactoringOfDeploymentDir(resultDeployed, forceDeploy, revision, context);
          } catch (RuntimeException e) {
            logger.error("Exception during refactoring rollback", e);
          } catch (XDEV_RefactoringFault e) {
            logger.error("Exception during refactoring rollback", e);
          }
        } else {
          resultDeployed.cleanupBackuppedFiles();
        }
      }
    } finally {
      lock.unlock();
    }
  }


  private RefactoringResult executeRefactoring(String sourceFqName, Work xmlRefactoring, boolean forceDeploy,
                                               Configuration config, Long revision, RefactoringContext context) throws XDEV_RefactoringFault,
      XDEV_RefactoringConflict {
    File f = config.getFileLocation(sourceFqName, revision);
    if (!f.exists()) {
      if (config.exceptionIfSourceFileDoesntExist) {
        throw new XDEV_RefactoringFault(sourceFqName, XMOMType.DATATYPE.toString(), RefactoringType.MOVE.toString(),
                                        new RuntimeException("source <" + f.getAbsolutePath() + "> doesn't exist"));
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Object " + sourceFqName + " did not exist here: " + f.getAbsolutePath() + ". continuing ...");
        }
        return new RefactoringResult();
      }
    } else {
      if (config.refactorInDeploymentDir) {
        return refactorDeploymentDir(xmlRefactoring, forceDeploy, config, revision, context);
      } else {
        return refactorSavedDir(xmlRefactoring, config, revision, context);
      }
    }
  }
  
  

  private Work getMoveWork(RefactoringTargetType type, Configuration config, RefactoringElement... refactoring) {
    switch (type) {
      case DATATYPE :
        return new DecomposedWork(new MoveDatatypeWork(Arrays.asList(refactoring), config));
      case EXCEPTION :
        return new DecomposedWork(new MoveExceptionWork(Arrays.asList(refactoring), config));
      case WORKFLOW :
        return new DecomposedWork(new MoveWorkflowWork(Arrays.asList(refactoring), config));
      default :
        throw new IllegalArgumentException("Could not getRefactoringWork for RefactoringTargetType " + type);
    }
  }


  private RefactoringResult moveServiceOperation(String fqXmlNameOld, String fqXmlNameNew, String targetLabel,
                                                 boolean forceDeploy, String sessionId, String creator, Long revision,
                                                 String workspaceName)
      throws XDEV_RefactoringFault, XDEV_RefactoringConflict {
    
    String[] oldNames = splitOperationFqName(fqXmlNameOld);
    String datatypeFqXmlNameOld = oldNames[0];
    String serviceGroupNameOld = oldNames[1];
    String operationNameOld = oldNames[2];
    
    Configuration config = new Configuration(true, true, false);
    Configuration savedConfig = new Configuration(false, true, true);
    
    // TODO this is the type given to the context, if we give it OPERATION instead we could restrict the locking to callers only
    //      at least for saved dependencies
    XMOMType typeToMove = XMOMType.DATATYPE; 
    
    String oldLabel = null;
    boolean isStatic = true;
    boolean found = false;
    try {
      Document doc = XMLUtils.parse(savedConfig.getFileLocation(datatypeFqXmlNameOld, revision));
      XMOMType type = XMOMType.getXMOMTypeByRootTag(doc.getDocumentElement().getTagName());
      if (type != XMOMType.DATATYPE) {
        throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(),
                                        new RuntimeException(fqXmlNameOld + " is not a datatype."));
      }
      List<Element> serviceElements = XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), GenerationBase.EL.SERVICE);
      for (Element serviceElement : serviceElements) {
        if (serviceElement.getAttribute(GenerationBase.ATT.TYPENAME).equals(serviceGroupNameOld)) {
          List<Element> operationElements = XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), GenerationBase.EL.OPERATION);
          for (Element operationElement : operationElements) {
            if (operationElement.getAttribute(GenerationBase.ATT.OPERATION_NAME).equals(operationNameOld)) {
              oldLabel = operationElement.getAttribute(GenerationBase.ATT.LABEL);
              isStatic = XMLUtils.isTrue(operationElement, GenerationBase.ATT.ISSTATIC);
              found = true; 
            }
          }
        }
      }
    } catch (Ex_FileAccessException e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
    } catch (XPRC_XmlParsingException e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
    }
    
    if (!found) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), new RuntimeException("Source operation could not be found!"));
    }

    OperationRefactoringElement refactoring = new OperationRefactoringElement(fqXmlNameOld, fqXmlNameNew, targetLabel, oldLabel);
    Collection<OperationRefactoringElement> refactorings;
    if (isStatic) {
      refactorings = Collections.singleton(refactoring);
    } else {
      refactorings = new ArrayList<OperationRefactoringElement>();
      try {
        // TODO Can we do better than passing <null> as a Context object? This may at least decrease overall performance
        // because the context is used to reduce the number of XML files that will be parsed
        refactorings.addAll(XMLRefactoringUtils.discoverOperationRenameRefactoringTargets(refactoring, config, revision, null));
        refactorings.addAll(XMLRefactoringUtils.discoverOperationRenameRefactoringTargets(refactoring, savedConfig, revision, null));
      } catch (Ex_FileAccessException e) {
        throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
      } catch (XPRC_XmlParsingException e) {
        throw new XDEV_RefactoringFault(fqXmlNameOld, typeToMove.toString(), RefactoringType.MOVE.toString(), e);
      }
    }
    Work refactoringWorkInSaved = new DecomposedWork(new MoveOperationWork(refactorings, savedConfig, revision));
    RefactoringContext context = new RefactoringContext(refactorings, new WorkBasedAutosaveFilter(refactoringWorkInSaved), sessionId, creator, revision);
    context.setRefactorOperation(true);
    RefactoringLock lock = lock(context);
    try {
      //check vorziehen, ob ziel objekt im saved-dir bereits existiert. wenn das dort bereits existierende ziel-objekt
      //bereits existiert, nur nicht deployed ist, merkt man es hier nicht.
      checkExistenceInSavedDir(fqXmlNameNew, fqXmlNameOld, typeToMove, revision);
      RefactoringResult resultDeployed = executeRefactoring(refactoring.fqXmlNameOld, new DecomposedWork(new MoveOperationWork(refactorings, config, revision)),
                                                            forceDeploy, config, revision, context);

      boolean success = false;
      try {
        RefactoringResult resultSaved = executeRefactoring(refactoring.fqXmlNameOld, refactoringWorkInSaved, forceDeploy, savedConfig, revision, context);
        success = true;
        publishRefactoringResult(creator, resultSaved, workspaceName);
        return mergeResults(resultDeployed, resultSaved);
      } finally {
        if (!success) {
          rollbackRefactoringOfDeploymentDir(resultDeployed, forceDeploy, revision, context);
        } else {
          resultDeployed.cleanupBackuppedFiles();
        }
      }
    } finally {
      lock.unlock();
    }
  }


  static String[] splitOperationFqName(String fqOperationName) {
    int lastDot = fqOperationName.lastIndexOf('.');
    String serviceGroupName = fqOperationName.substring(0, lastDot);
    int secondToLastDot = serviceGroupName.lastIndexOf('.');
    String datatypeName = fqOperationName.substring(0, secondToLastDot);
    serviceGroupName = serviceGroupName.substring(secondToLastDot + 1);
    String operationName = fqOperationName.substring(lastDot + 1);
    return new String[] {datatypeName, serviceGroupName, operationName};
  }
  
  
  private RefactoringResult movePath(String fqXmlNameOld, String fqXmlNameNew,
                                     boolean forceDeploy, String sessionId, String creator,
                                     Long revision, String workspaceName) throws XDEV_RefactoringFault, XDEV_RefactoringConflict {
    
    // TODO ensure the path exists
    Set<RefactoringElement> refactorings = new HashSet<RefactoringElement>();
    try {
      refactorings.addAll(XMLRefactoringUtils.discoverPathRefactoringTargets(fqXmlNameOld, fqXmlNameNew, new Configuration(false, false, false), revision));
      refactorings.addAll(XMLRefactoringUtils.discoverPathRefactoringTargets(fqXmlNameOld, fqXmlNameNew, new Configuration(true, false, false), revision));
    } catch (Ex_FileAccessException e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, "Path", RefactoringType.MOVE.toString(), e);
    } catch (XPRC_XmlParsingException e) {
      throw new XDEV_RefactoringFault(fqXmlNameOld, "Path", RefactoringType.MOVE.toString(), e);
    }
    Configuration config = new Configuration(true, true, false);
    Configuration savedConfig = new Configuration(false, true, true);
    
    Work refactoringWorkInSaved = RenamePathWork.getAsWork(refactorings, savedConfig);
    RefactoringContext context = new RefactoringContext(refactorings, new WorkBasedAutosaveFilter(refactoringWorkInSaved), sessionId, creator, revision);
    RefactoringLock lock = lock(context);
    try {
      RefactoringResult resultDeployed = refactorDeploymentDir(RenamePathWork.getAsWork(refactorings, config), 
                                                               forceDeploy, config, revision, context);

      boolean success = false;
      try {
        RefactoringResult resultSaved = refactorSavedDir(refactoringWorkInSaved, savedConfig, revision, context);
        success = true;
        publishRefactoringResult(creator, resultSaved, workspaceName);
        return mergeResults(resultDeployed, resultSaved);
      } finally {
        if (!success) {
          try {
            rollbackRefactoringOfDeploymentDir(resultDeployed, forceDeploy, revision, context);
          } catch (RuntimeException e) {
            logger.error("Exception during refactoring rollback", e);
          } catch (XDEV_RefactoringFault e) {
            logger.error("Error during rollback", e);
          }
        } else {
          resultDeployed.cleanupBackuppedFiles();
        }
      }
    } finally {
      lock.unlock();
    }
  }


  private void publishRefactoringResult(String creator, RefactoringResult resultSaved, String workspaceName) {
    List<SerializablePair<String, String>> updateList = new ArrayList<SerializablePair<String,String>>();
    for (XMOMObjectRefactoringResult object : resultSaved.getRefactoredObjects()) {
      switch (object.refactoringType) {
        case CHANGE : // TODO values are currently only placeholders until the gui knows/decides what it want's to receive
                      // [10:20:25] *SomeGUI-Person*: Solange man keine eingeschränkten Updates machen kann, ist mir die Nachricht erstmal egal.
          updateList.add(SerializablePair.of(object.fqXmlNameOld, MODIFIED));
          break;
        case MOVE :
          updateList.add(SerializablePair.of(object.fqXmlNameOld, DELETED));
          updateList.add(SerializablePair.of(object.fqXmlNameNew, SAVED));
          break;
        default :
          throw new IllegalArgumentException("Unknown refactoringType " + object.refactoringType);
      }
    }
    PredefinedMessagePath updatePath = PredefinedMessagePath.XYNA_MODELLER_UPDATE;
    MessageInputParameter mip = new MessageInputParameter(updatePath.getProduct(),
                                                          updatePath.getContext(),
                                                          workspaceName,
                                                          creator, 
                                                          updateList,
                                                          updatePath.isPersistent());
    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(mip);
    } catch (XynaException e) {
      logger.error("Failed to notify gui of refactoring result", e);
    }
  }
  
  private static final XynaPropertyBoolean checkTarget = new XynaPropertyBoolean("xdev.xfractmod.xmdm.refactoring.checktarget", true).setHidden(true);


  private void checkExistenceInSavedDir(String fqXmlNameNew, String fqXmlNameOld, XMOMType type, Long revision) throws XDEV_RefactoringFault {
    String fileLocationOfXmlNameForSaving = GenerationBase.getFileLocationOfXmlNameForSaving(fqXmlNameNew, revision);
    if (checkTarget.get() && new File(fileLocationOfXmlNameForSaving + ".xml").exists() && !fqXmlNameNew.equals(fqXmlNameOld)) {
      //wenn name gleich ist, ist hoffentlich das label unterschiedlich - falls nicht, wird das erst später abgefangen.
      //eigtl sollte das aber die gui bereits abfangen.
      throw new XDEV_RefactoringFault(fqXmlNameOld, type.toString(), RefactoringType.MOVE.toString(),
                                      new RuntimeException("target already exists"));
    }
  }


  private void rollbackRefactoringOfDeploymentDir(RefactoringResult resultDeployed, boolean forceDeploy, Long revision, RefactoringContext context)
      throws XDEV_RefactoringFault {
    //deploykram, so wie in der hinrichtung, aber ohne den refactoring-schritt. changed und moved workflows nimm aus dem refactoring-result
    if (logger.isInfoEnabled()) {
      logger.info("rolling back refactoring of deployment dir.");
    }
    resultDeployed.rollbackFileChanges();

    //0. serviceLibFolder wiederherstellen
    //1. verschobene objekte wieder herstellen
    //2. geänderte objekte redeployen
    //3. config von verschobenen objekten wieder herstellen
    //4. neu hinzugefügte objekte undeployen

    //0.
    for (XMOMObjectRefactoringResult refactored : resultDeployed.getRefactoredObjects()) {
      if (refactored.getType() == RefactoringType.MOVE) {
        undoMoveServiceLibs(refactored.getFqXmlNameOld(), refactored.getFqXmlNameNew(), true, revision);
      }
    }
    
    
    //1.
    for (XMOMObjectRefactoringResult refactored : resultDeployed.getRefactoredObjects()) {
      if (refactored.getType() == RefactoringType.MOVE && refactored.getRefactoringTargetType().hasCorrespondingGenerationBaseRepresentation()) {
        try {
          GenerationBase gb = getGenerationBaseInstance(refactored.getFqXmlNameOld(), refactored.getRefactoringTargetType(), revision);
          try {
            gb.setDeploymentComment("Rollback during refactoring of " + context.getRefactoringElements().iterator().next());
            gb.deploy(DeploymentMode.regenerateDeployedAllFeaturesXmlChanged, WorkflowProtectionMode.FORCE_DEPLOYMENT);
          } catch (XPRC_DeploymentDuringUndeploymentException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          } catch (XPRC_InheritedConcurrentDeploymentException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          } catch (XPRC_MDMDeploymentException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          }
        } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
          throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
        }
      }
    }

    //2.

    List<GenerationBase> deploymentBatch = new ArrayList<GenerationBase>();

    for (XMOMObjectRefactoringResult refactored : resultDeployed.getRefactoredObjects()) {
      if (refactored.getType() == RefactoringType.CHANGE) {
        if (refactored.getRefactoringTargetType() == RefactoringTargetType.FILTER) {
          //Filter neu adden
          try {
            XynaFactory.getInstance().getActivation().getActivationTrigger()
                .reAddExistingFilterWithExistingParameters(refactored.getFqXmlNameOld(), revision);
          } catch (XynaException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          }
        } else if (refactored.getRefactoringTargetType().hasCorrespondingGenerationBaseRepresentation()) {
          try {
            GenerationBase gb = getGenerationBaseInstance(refactored.getFqXmlNameNew(), refactored.getRefactoringTargetType(), revision);
            gb.setDeploymentComment("Rollback during refactoring of " + context.getRefactoringElements().iterator().next());
            deploymentBatch.add(gb);
          } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          }
        } else {
          throw new RuntimeException("unsuspported type: " + refactored.getRefactoringTargetType());
        }
      }
    }

    if (deploymentBatch.size() > 0) {
      WorkflowProtectionMode protectionMode;
      if (forceDeploy) {
        protectionMode = WorkflowProtectionMode.FORCE_DEPLOYMENT;
      } else {
        protectionMode = WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES;
      }
      try {
        GenerationBase.deploy(deploymentBatch, DeploymentMode.regenerateDeployedAllFeaturesXmlChanged, false, protectionMode);
      } catch (MDMParallelDeploymentException e) { // TODO more specfic
        e.generateSerializableFailedObjects();
        throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                        RefactoringType.MOVE.toString(), e);
      } catch (XPRC_DeploymentDuringUndeploymentException e) {
        throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                        RefactoringType.MOVE.toString(), e);
      }
    }

    //3.
    for (XMOMObjectRefactoringResult refactored : resultDeployed.getRefactoredObjects()) {
      if (refactored.getType() == RefactoringType.MOVE) {
        try {
          if (refactored.getRefactoringTargetType() == RefactoringTargetType.WORKFLOW) {
            copyWorkflowConfiguration(refactored.getFqXmlNameNew(), refactored.getFqXmlNameOld(), new RefactoringResult(), revision);
          } else if (refactored.getRefactoringTargetType() == RefactoringTargetType.DATATYPE) {
            refactorOrderInputSources(DependencySourceType.DATATYPE, refactored.getFqXmlNameNew(), refactored.getFqXmlNameOld(),
                                                new RefactoringResult(), revision);
          }
        } catch (XynaException e) {
          throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
        }
      }
    }

    //4.
    try {
      for (XMOMObjectRefactoringResult refactored : resultDeployed.getRefactoredObjects()) {
        if (refactored.getType() == RefactoringType.MOVE) {
          if (refactored.getFqXmlNameNew().equals(refactored.getFqXmlNameOld())) {
            continue;
          }
          try {
            if (!forceDeploy) {
              InUse usage = DeploymentManagement.getInstance().isInUse(new WorkflowRevision(GenerationBase.transformNameForJava(refactored.getFqXmlNameNew()), revision));
              try {
                usage.throwExceptionIfInUse(refactored.getFqXmlNameNew());
              } catch (XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE e) {
                throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
              }
            }
            GenerationBase gb;
            switch (refactored.getRefactoringTargetType()) {
              case DATATYPE :
                gb = DOM.getInstance(refactored.getFqXmlNameNew(), revision);
                break;
              case WORKFLOW :
                gb = WF.getInstance(refactored.getFqXmlNameNew(), revision);
                break;
              case EXCEPTION :
                gb = ExceptionGeneration.getInstance(refactored.getFqXmlNameNew(), revision);
                break;
              default :
                gb = null;
            }
            if (gb != null) {
              gb.throwExceptionIfNotExists();
              gb.undeployRudimentarily(true);
            }
          } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            throw new XDEV_RefactoringFault(refactored.getFqXmlNameOld(), refactored.getType().toString(), RefactoringType.MOVE.toString(), e);
          }
        }
      }
    } finally {
      GenerationBase.finishUndeploymentHandler();
    }
  }


  private RefactoringLock lock(RefactoringContext context) throws XDEV_RefactoringFault {
    RefactoringLock lock = new RefactoringLock(context);
    lock.lock();
    return lock;
  }


  private RefactoringResult mergeResults(RefactoringResult resultDeployed, RefactoringResult resultSaved) {
    TreeSet<XMOMObjectRefactoringResult> set =
        new TreeSet<XMOMObjectRefactoringResult>(new Comparator<XMOMObjectRefactoringResult>() {

          public int compare(XMOMObjectRefactoringResult o1, XMOMObjectRefactoringResult o2) {
            int r = o1.fqXmlNameNew.compareTo(o2.fqXmlNameNew);
            if (r == 0) {
              //evtl heisst ein filter genauso wie ein xmom objekt oder sowas
              r = o1.targetType.compareTo(o2.targetType);
            }
            if (r == 0) {
              r = Long.compare(o1.getRevision(), o2.getRevision());
            }
            return r;
          }

        });
    for (XMOMObjectRefactoringResult o : resultDeployed.getRefactoredObjects()) {
      set.add(o);
    }
    for (XMOMObjectRefactoringResult o : resultSaved.getRefactoredObjects()) {
      if (set.contains(o)) {
        //merge
        //bestehendes objekt aus set rausholen
        SortedSet<XMOMObjectRefactoringResult> subset = set.tailSet(o);
        XMOMObjectRefactoringResult existingObject = subset.first();
        existingObject.mergeWith(o);
      } else {
        set.add(o);
      }
    }

    RefactoringResult r = new RefactoringResult();
    for (XMOMObjectRefactoringResult o : set) {
      r.add(o);
    }
    return r;
  }
  
  private class RefactorSavedDirProgress{
    public boolean isStartedUpdateDeploymentItemState() {
      return startedUpdateDeploymentItemState;
    }
    public void setStartedUpdateDeploymentItemState(boolean startedUpdateDeploymentItemState) {
      this.startedUpdateDeploymentItemState = startedUpdateDeploymentItemState;
    }
    public boolean isStartedUpdateDeploymentItemMarker() {
      return startedUpdateDeploymentItemMarker;
    }
    public void setStartedUpdateDeploymentItemMarker(boolean startedUpdateDeploymentItemMarker) {
      this.startedUpdateDeploymentItemMarker = startedUpdateDeploymentItemMarker;
    }
    public boolean isStartedMoveServiceLibs() {
      return startedMoveServiceLibs;
    }
    public void setStartedMoveServiceLibs(boolean startedMoveServiceLibs) {
      this.startedMoveServiceLibs = startedMoveServiceLibs;
    }
    public boolean isStartedUpdateApplicationEntries() {
      return startedUpdateApplicationEntries;
    }
    public void setStartedUpdateApplicationEntries(boolean startedUpdateApplicationEntries) {
      this.startedUpdateApplicationEntries = startedUpdateApplicationEntries;
    }
    public boolean isStartedDeleteXMOMObjects() {
      return startedDeleteXMOMObjects;
    }
    public void setStartedDeleteXMOMObjects(boolean startedDeleteXMOMObjects) {
      this.startedDeleteXMOMObjects = startedDeleteXMOMObjects;
    }
    public boolean isStartedSaveMDM() {
      return startedSaveMDM;
    }
    public void setStartedSaveMDM(boolean startedSaveMDM) {
      this.startedSaveMDM = startedSaveMDM;
    }
    public boolean isFinished() {
      return finished;
    }
    public void setFinished(boolean finished) {
      this.finished = finished;
    }
    private boolean startedUpdateDeploymentItemState;
    private boolean startedUpdateDeploymentItemMarker;
    private boolean startedMoveServiceLibs;
    private boolean startedUpdateApplicationEntries;
    private boolean startedDeleteXMOMObjects;
    private boolean startedSaveMDM;
    private boolean finished;
  }


  private RefactoringResult refactorSavedDir(Work xmlRefactoring, Configuration config, Long revision, RefactoringContext context) throws XDEV_RefactoringConflict,
      XDEV_RefactoringFault {
    RefactorSavedDirProgress progress = new RefactorSavedDirProgress();
    boolean success = false;
    RefactoringResult refactoringResult = new RefactoringResult();

    //Execute Refactoring; Rollback needed
    Result result;
    try {
      result = executeRefactoring(xmlRefactoring, config, revision, context);
      success = true;
    } catch (Ex_FileAccessException e) {
      throw new XDEV_RefactoringFault("Unknown", "Unknown", RefactoringType.MOVE.toString(), e);
    } catch (XPRC_XmlParsingException e) {
      throw new XDEV_RefactoringFault("Unknown", "Unknown", RefactoringType.MOVE.toString(), e);
    } finally {
      if (!success) {
        //rollback
        if (logger.isInfoEnabled()) {
          logger.info("rolling back partially refactored saved dir.");
        }
        config.rollbackFileChanges();
      }
    }
    
    WorkflowEngine workflowEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    XynaMultiChannelPortalBase xynaMultiChannelPortal = XynaFactory.getInstance().getXynaMultiChannelPortal();
    DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
    DeploymentMarkerManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getDeploymentMarkerManagement();
    
    List<XMLRefactoringUtils.XMOMObjectRefactoringResult> changedOrMoved = new ArrayList<XMLRefactoringUtils.XMOMObjectRefactoringResult>();
    for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
      changedOrMoved.add(changed);
    }
    for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
      changedOrMoved.add(moved);
    }
    
    try {
      

      // Get Results: No Rollback needed
      if (XMLRefactoringUtils.dryRunRefactorings.get()) {
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          refactoringResult.add(new XMOMObjectRefactoringResult(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(),
              moved.getType(), RefactoringType.MOVE, moved.getLabelInformation(), moved.getRevision()));
        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          refactoringResult.add(new XMOMObjectRefactoringResult(changed.getFqXmlNameNew(), changed.getFqXmlNameOld(),
              changed.getType(), RefactoringType.CHANGE, changed.getLabelInformation(), changed.getRevision()));
        }
        config.cleanupBackuppedFiles();
        return refactoringResult;
      }
      
      progress.setStartedUpdateDeploymentItemState(true);

      // Update deploymentItemstate; Rollback needed
      if (dism != null) {
        // Erstes Update: Updatet alle Interfaces etc. Es kann aber noch nicht sicher
        // validiert werden,
        // weil noch nicht alle anderen Objekte geupdatet sind.
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          updateDeploymentItemState(moved, moved.getRevision(), DeploymentLocation.SAVED, dism, config);
        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          updateDeploymentItemState(changed, changed.getRevision(), DeploymentLocation.SAVED, dism, config);
        }
        // Alle Objekte sind geupdatet, jetzt kann nochmal validiert werden
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          ((DeploymentItemStateImpl) (dism.get(moved.getFqXmlNameNew(), moved.getRevision()))).validate(DeploymentLocation.SAVED);

        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          ((DeploymentItemStateImpl) (dism.get(changed.getFqXmlNameNew(), changed.getRevision())))
              .validate(DeploymentLocation.SAVED);
        }
      }
      progress.setStartedUpdateDeploymentItemMarker(true);

      // DeploymentMarker updaten; Rollback needed
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        updateDeploymentMarker(moved, moved.getRevision(), dmm);
      }
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
        updateDeploymentMarker(changed, changed.getRevision(), dmm);
      }

      progress.setStartedMoveServiceLibs(true);

      

      // Move service Libs; Rollback needed
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        moveServiceLibs(moved.getFqXmlNameOld(), moved.getFqXmlNameNew(), config.refactorInDeploymentDir, moved.getRevision());
      }

      progress.setStartedUpdateApplicationEntries(true);

      // ApplicationEntries anpassen; Rollback needed
      // Vor dem Löschen der alten XMOM Objekte machen, sonst kann nicht mehr
      // rausgefunden werden in welchen Applications die Objekte waren
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        try {
          copyApplicationInfo(moved.getFqXmlNameOld(), moved.getFqXmlNameNew(), moved.getType(), refactoringResult,
                              moved.getRevision());
        } catch (PersistenceLayerException e) {
          throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(),
              RefactoringType.MOVE.toString(), e);
        }
      }

      progress.setStartedDeleteXMOMObjects(true);

      // Delete XMOM Objects; Rollback needed
      // TODO rollback für den fall, dass mehrere workflows moved wurden (erstmal
      // nicht der fall)
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        if (moved.getType().hasCorrespondingGenerationBaseRepresentation()) {
          DependentObjectMode mode = DependentObjectMode.UNDEPLOY; // sollte keiner mehr verwenden
          try {
            workflowEngine.deleteXMOMObject(moved.getFqXmlNameOld(), moved.getType().getXMOMType(), true, mode, false,
                                            moved.getRevision());
          } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().name(),
                RefactoringType.MOVE.toString(), e);
          }
        }
        refactoringResult.add(new XMOMObjectRefactoringResult(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(),
            moved.getType(), RefactoringType.MOVE, moved.getLabelInformation(), moved.getRevision()));
      }

      progress.setStartedSaveMDM(true);
      

      // Save MDM; Rollback needed
      RepositoryEvent repositoryEvent = new EmptyRepositoryEvent();

      for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : changedOrMoved) {
        if (changed.getType().hasCorrespondingGenerationBaseRepresentation()) {
          try {
            xynaMultiChannelPortal.saveMDM(
                FileUtils.readFileAsString(new File(
                    GenerationBase.getFileLocationForSavingStaticHelper(changed.getFqXmlNameNew(), changed.getRevision()) + ".xml")),
                changed.getRevision(), repositoryEvent);
          } catch (XynaException e) {
            throw new XDEV_RefactoringFault(changed.getFqXmlNameNew(), changed.getType().name(),
                RefactoringType.MOVE.toString(), e);
          }
        }
        refactoringResult.add(new XMOMObjectRefactoringResult(changed.getFqXmlNameNew(), changed.getFqXmlNameOld(),
            changed.getType(), RefactoringType.CHANGE, changed.getLabelInformation(), changed.getRevision()));
      }
      progress.setFinished(true);

    } finally {
      if (!progress.isFinished()) {
        // Rollback Refactoring
        config.rollbackFileChanges();
        
        // Rollback saved MDM
        if (progress.isStartedSaveMDM()) {
          RepositoryEvent repositoryEvent = new EmptyRepositoryEvent();
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult r: changedOrMoved) {
            if (r.getType().hasCorrespondingGenerationBaseRepresentation()) {
              try {
                xynaMultiChannelPortal.saveMDM(
                    FileUtils.readFileAsString(new File(
                        GenerationBase.getFileLocationForSavingStaticHelper(r.getFqXmlNameOld(), r.getRevision()) + ".xml")),
                    r.getRevision(), repositoryEvent);
              } catch (XynaException e) {
                throw new XDEV_RefactoringFault(r.getFqXmlNameOld(), r.getType().toString(),
                    RefactoringType.MOVE.toString(), e);
              }
            }
          }
        }
        // Rollback Rollback Move Service Libs
        if (progress.isStartedMoveServiceLibs()) {
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
            moveServiceLibs(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(), config.refactorInDeploymentDir, moved.getRevision());
          }
        }

        // Rollback DeleteXMOM
        if (progress.isStartedDeleteXMOMObjects()) {
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
            if (moved.getType().hasCorrespondingGenerationBaseRepresentation()) {
              DependentObjectMode mode = DependentObjectMode.UNDEPLOY; // sollte keiner mehr verwenden
              try {
                workflowEngine.deleteXMOMObject(moved.getFqXmlNameNew(), moved.getType().getXMOMType(), true, mode,
                    false, moved.getRevision());
              } catch (XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT e) {
                throw new XDEV_RefactoringFault(moved.getFqXmlNameNew(), moved.getType().name(),
                    RefactoringType.MOVE.toString(), e);
              }
            }
            refactoringResult.add(new XMOMObjectRefactoringResult(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(),
                moved.getType(), RefactoringType.MOVE, moved.getLabelInformation(), moved.getRevision()));
          }
        }
        // Rollback Update Application Entries
        if (progress.isStartedUpdateApplicationEntries()) {
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
            try {
              copyApplicationInfo(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(), moved.getType(), refactoringResult,
                                  moved.getRevision());
            } catch (PersistenceLayerException e) {
              throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(),
                  RefactoringType.MOVE.toString(), e);
            }
          }
        }
        // Rollback Deploymentitem Marker
        if (progress.isStartedUpdateDeploymentItemMarker()) {
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
            updateDeploymentMarker(moved.getInverse(), moved.getRevision(), dmm);
          }
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
            updateDeploymentMarker(changed.getInverse(), changed.getRevision(), dmm);
          }
        }

        // Rollback Deploymentitemstate
        if (progress.isStartedUpdateDeploymentItemState()) {
          if (dism != null) {
            // Erstes Update: Updatet alle Interfaces etc. Es kann aber noch nicht sicher
            // validiert werden,
            // weil noch nicht alle anderen Objekte geupdatet sind.
            for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
              updateDeploymentItemState(moved.getInverse(), moved.getRevision(), DeploymentLocation.SAVED, dism, config);
            }
            for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
              updateDeploymentItemState(changed.getInverse(), changed.getRevision(), DeploymentLocation.SAVED, dism, config);
            }
            // Alle Objekte sind geupdatet, jetzt kann nochmal validiert werden
            for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
              ((DeploymentItemStateImpl) (dism.get(moved.getInverse().getFqXmlNameNew(), moved.getRevision())))
                  .validate(DeploymentLocation.SAVED);

            }
            for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
              ((DeploymentItemStateImpl) (dism.get(changed.getInverse().getFqXmlNameNew(), changed.getRevision())))
                  .validate(DeploymentLocation.SAVED);
            }
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("rolling back partially refactored saved dir.");
        }
      } else {
        config.cleanupBackuppedFiles();
      }
    }

    return refactoringResult;
  }
  
  
  private static boolean isUsedInStorableHierarchy(List<RefactoringElement> refactorings, Long revision) {
    XMOMStorableStructureCache storCache = XMOMStorableStructureCache.getInstance(revision);
    Collection<XMOMStorableStructureInformation> all = storCache.getAllStorableStructureInformation();
    UsageScanner us = new UsageScanner(refactorings);
    for (XMOMStorableStructureInformation xssi : all) {
      xssi.traverse(us);
      if (us.foundUsage()) {
        return true;
      }
    }
    return false;
  }
  
  
  private static class UsageScanner implements StorableStructureVisitor {

    private final Set<String> oldFqXmlNames;
    private boolean usage = false;
    
    
    public UsageScanner(List<RefactoringElement> refactorings) {
      oldFqXmlNames = new HashSet<String>();
      for (RefactoringElement refactoring : refactorings) {
        oldFqXmlNames.add(refactoring.fqXmlNameOld);
      } 
    }
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (oldFqXmlNames.contains(current.getFqXmlName())) {
        usage = true;
      }
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) { /* ntbd */ }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS;
    }
    
    
    public boolean foundUsage() {
      return usage;
    }
    
  }



  /**
   * hilfsklasse für das rollback des refactoring des deployment dirs
   */
  private static class RefactorDeploymentDirProgress {

    private boolean deployMovedDone = false;
    private int redeployChangedDone = 0;
    private boolean copyConfigurationDone = false;
    private boolean moveServiceLibsDone = false;
    private boolean finished = false;


    public void deployMovedDone() {
      deployMovedDone = true;
    }

    public void redeployChangedDone() {
      redeployChangedDone++;
    }

    public void copyConfigurationDone() {
      copyConfigurationDone = true;
    }

    public void finished() {
      finished = true;
    }

    public boolean isFinished() {
      return finished;
    }

    public boolean isCopyConfigurationDone() {
      return copyConfigurationDone;
    }

    public boolean hasRedeployChangedStarted() {
      return redeployChangedDone > 0;
    }

    public int lastRedeployChanged() {
      return redeployChangedDone;
    }

    public boolean isDeployMovedDone() {
      return deployMovedDone;
    }

    public void moveServiceLibsDone() {
      moveServiceLibsDone = true;
    }
    
    public boolean isMoveServiceLibsDone() {
      return moveServiceLibsDone;
    }

  }


  private RefactoringResult refactorDeploymentDir(Work xmlRefactoring, boolean forceDeploy, Configuration config, Long revision, RefactoringContext context) throws XDEV_RefactoringConflict,
      XDEV_RefactoringFault {
    RefactorDeploymentDirProgress progress = new RefactorDeploymentDirProgress();
    Result result = null;
    RefactoringResult refactoringResult = new RefactoringResult();

    refactoringResult.setConfigDeployedDir(config);
    boolean success = false;
    try {
      result = executeRefactoring(xmlRefactoring, config, revision, context);
      success = true;
    } catch (Ex_FileAccessException e) {
      throw new XDEV_RefactoringFault("Unknown", "Unknown", RefactoringType.MOVE.toString(), e);
    } catch (XPRC_XmlParsingException e) {
      throw new XDEV_RefactoringFault("Unknown", "Unknown", RefactoringType.MOVE.toString(), e);
    } finally {
      if (!success) {
        config.rollbackFileChanges();
      }
    }
    
    try {
      if (XMLRefactoringUtils.dryRunRefactorings.get()) {
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          refactoringResult.add(new XMOMObjectRefactoringResult(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(),
                                                     moved.getType(), RefactoringType.MOVE, moved.getLabelInformation(), moved.getRevision()));
        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          refactoringResult.add(new XMOMObjectRefactoringResult(changed.getFqXmlNameNew(), changed.getFqXmlNameOld(),
                                                     changed.getType(), RefactoringType.CHANGE, changed.getLabelInformation(), changed.getRevision()));
        }
        return refactoringResult;
      }
      
      DeploymentItemStateManagement dism = getDeploymentItemStateManagement();
      if (dism != null) {
        //Erstes Update: Updatet alle Interfaces etc. Es kann aber noch nicht sicher validiert werden,
        //weil noch nicht alle anderen Objekte geupdatet sind.
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          updateDeploymentItemState(moved, moved.getRevision(), DeploymentLocation.DEPLOYED, dism, config);
        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          updateDeploymentItemState(changed, changed.getRevision(), DeploymentLocation.DEPLOYED, dism, config);
        }
        //Alle Objekte sind geupdatet, jetzt kann nochmal validiert werden
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          ((DeploymentItemStateImpl)(dism.get(moved.getFqXmlNameNew(), moved.getRevision()))).validate(DeploymentLocation.DEPLOYED);
          
        }
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
          ((DeploymentItemStateImpl)(dism.get(changed.getFqXmlNameNew(), changed.getRevision()))).validate(DeploymentLocation.DEPLOYED);
        }
      }
      
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        moveServiceLibs(moved.getFqXmlNameOld(), moved.getFqXmlNameNew(), config.refactorInDeploymentDir, moved.getRevision());
      }
      progress.moveServiceLibsDone();
      
      List<GenerationBase> deploymentBatch = new ArrayList<GenerationBase>();
      
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        GenerationBase gb;
        try {
          gb = getGenerationBaseInstance(moved.getFqXmlNameNew(), moved.getType(), moved.getRevision());
        } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
          throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(), RefactoringType.MOVE.toString(), e);
        } catch (XPRC_InvalidPackageNameException e) {
          throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(), RefactoringType.MOVE.toString(), e);
        }
        gb.setDeploymentComment("Refactoring " + context.getRefactoringElements().iterator().next());
        deploymentBatch.add(gb);
        refactoringResult.add(new XMOMObjectRefactoringResult(moved.getFqXmlNameNew(), moved.getFqXmlNameOld(),
                                                   moved.getType(), RefactoringType.MOVE, moved.getLabelInformation(), moved.getRevision()));
      }
      
      if (deploymentBatch.size() > 0) {
        try {
          GenerationBase.deploy(deploymentBatch, DeploymentMode.regenerateDeployedAllFeaturesXmlChanged, false, WorkflowProtectionMode.FORCE_DEPLOYMENT);
        } catch (MDMParallelDeploymentException e) { // TODO more specfic
          e.generateSerializableFailedObjects();
          throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                          RefactoringType.MOVE.toString(), e);
        } catch (XPRC_DeploymentDuringUndeploymentException e) {
          throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                          RefactoringType.MOVE.toString(), e);
        }
      }
      
      progress.deployMovedDone();

      deploymentBatch = new ArrayList<GenerationBase>();
      WorkflowProtectionMode protectionMode = WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES;
      //TODO if all or none are forced use batch, single deployment otherwise (could there be cyclic dependent refactorings that just don't work single?)
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
        if (changed.getType() == RefactoringTargetType.FILTER) {
          //Filter neu adden
          try {
            XynaFactory.getInstance().getActivation().getActivationTrigger()
                .reAddExistingFilterWithExistingParameters(changed.getFqXmlNameOld(), changed.getRevision());
          } catch (XynaException e) {
            throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
          }
        } else if (changed.getType() == RefactoringTargetType.WORKFLOW ||
                   changed.getType() == RefactoringTargetType.DATATYPE ||
                   changed.getType() == RefactoringTargetType.EXCEPTION) {
          try {
            GenerationBase gb = getGenerationBaseInstance(changed.getFqXmlNameNew(), changed.getType(), changed.getRevision());
            if (forceDeploy) {
              protectionMode = WorkflowProtectionMode.FORCE_DEPLOYMENT;
            }
            gb.setDeploymentComment("Refactoring " + context.getRefactoringElements().iterator().next());
            deploymentBatch.add(gb);
          } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
            throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
          }
          refactoringResult.add(new XMOMObjectRefactoringResult(changed.getFqXmlNameNew(), changed.getFqXmlNameOld(),
                                                     changed.getType(), RefactoringType.CHANGE, changed.getLabelInformation(), changed.getRevision()));
          progress.redeployChangedDone();
        } else {
          throw new RuntimeException("unsuspported type: " + changed.getType());
        }
      }
      
      if (deploymentBatch.size() > 0) {
        try {
          GenerationBase.deploy(deploymentBatch, DeploymentMode.regenerateDeployedAllFeaturesXmlChanged, false, protectionMode);
        } catch (MDMParallelDeploymentException e) { // TODO more specfic
          throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                          RefactoringType.MOVE.toString(), e);
        } catch (XPRC_DeploymentDuringUndeploymentException e) {
          throw new XDEV_RefactoringFault(deploymentBatch.iterator().next().getOriginalFqName(), deploymentBatch.iterator().next().getClass().getSimpleName(),
                                          RefactoringType.MOVE.toString(), e);
        }
      }
      
      for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
        if (moved.getType() == RefactoringTargetType.WORKFLOW) {
          try {
            copyWorkflowConfiguration(moved.getFqXmlNameOld(), moved.getFqXmlNameNew(), refactoringResult, moved.getRevision());
          } catch (XynaException e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameNew(), DependencySourceType.WORKFLOW.getName(),
                                            RefactoringType.MOVE.toString(), e);
          }
        } else if (moved.getType() == RefactoringTargetType.DATATYPE) {
          try {
            refactorOrderInputSources(DependencySourceType.DATATYPE, moved.getFqXmlNameOld(), moved.getFqXmlNameNew(),
                                                refactoringResult, moved.getRevision());
          } catch (XynaException e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameNew(), DependencySourceType.DATATYPE.getName(),
                                            RefactoringType.MOVE.toString(), e);
          }
        }
      }
      progress.copyConfigurationDone();
   
      //es können noch subworkflows von laufenden aufträgen gestartet werden, das macht aber nichts.
      //weil wir pausieren den scheduler nur, weil wir verhindern wollen, dass nach dem "isInUse"-check noch aufträge
      //gestartet werden. zu dem zeitpunkt sind aber die parent-workflows bereits refactored deployed und
      //rufen den workflow nicht mehr auf.
      //folglich kann der refactorte workflow nur als mainworkflow gestartet werden
      XynaFactory.getInstance().getProcessing().getXynaScheduler().pauseScheduling(false);
      try {
        for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
          if (moved.getFqXmlNameNew().equals(moved.getFqXmlNameOld())) {
            //usecase: labeländerung bei workflows
            continue;
          }
          try {
            if (!forceDeploy) {
              InUse usage = DeploymentManagement.getInstance().isInUse(new WorkflowRevision(GenerationBase.transformNameForJava(moved.getFqXmlNameOld()), moved.getRevision()));
              try {
                usage.throwExceptionIfInUse(moved.getFqXmlNameOld());
              } catch (XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE e) {
                throw new XDEV_RefactoringConflict(moved.getFqXmlNameOld(), moved.getType().toString(),
                                                   RefactoringType.MOVE.toString(), e);
              }
            }
            GenerationBase gb = getGenerationBaseInstance(moved.getFqXmlNameOld(), moved.getType(), moved.getRevision());
            gb.undeployRudimentarily(true);
          } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(),
                                            RefactoringType.MOVE.toString(), e);
          }
        }
      } finally {
        try {
          GenerationBase.finishUndeploymentHandler();
        } finally {
          XynaFactory.getInstance().getProcessing().getXynaScheduler().resumeScheduling();
        }
      }
      progress.finished();

      return refactoringResult;

    } finally {
      if (!progress.isFinished()) {
        //rollback: möglichst den ursprünglichen zustand wieder herstellen

        //xmls umgekehrt refactorn immer, auch wenn der schritt selbst einen fehler hat:
        if (logger.isInfoEnabled()) {
          logger.info("rolling back refactored deployed dir.");
        }
        config.rollbackFileChanges();

        if (progress.isCopyConfigurationDone()) {
          //TODO dann gab es wohl einen fehler beim undeploy. ein redeployment würde unter umständen gut tun?

          //config muss nicht (zurück-)kopiert werden, weil das undeploy nicht durchgeführt wurde!
        }

        if (progress.hasRedeployChangedStarted()) {
          int cnt = 0;
          for (XMLRefactoringUtils.XMOMObjectRefactoringResult changed : result.changed()) {
            if (++cnt > progress.lastRedeployChanged()) {
              break;
            }

            if (changed.getType() == RefactoringTargetType.FILTER) {
              //Filter neu adden
              try {
                XynaFactory.getInstance().getActivation().getActivationTrigger().reAddExistingFilterWithExistingParameters(changed.getFqXmlNameOld(), changed.getRevision());
              } catch (XynaException e) {
                throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), XMOMType.WORKFLOW.toString(),
                                                RefactoringType.MOVE.toString(), e);
              }
            } else if (changed.getType() == RefactoringTargetType.WORKFLOW || 
                       changed.getType() == RefactoringTargetType.DATATYPE ||
                       changed.getType() == RefactoringTargetType.EXCEPTION) {
              try {
                GenerationBase gb = getGenerationBaseInstance(changed.getFqXmlNameNew(), changed.getType(), changed.getRevision());
                WorkflowProtectionMode protectionMode;
                //auch mit force deployen, wenn das vorher der fall gewesen ist, sonst funktioniert es ggf nicht
                if (forceDeploy) {
                  protectionMode = WorkflowProtectionMode.FORCE_DEPLOYMENT;
                } else {
                  protectionMode = WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES;
                }
                try { // TODO batch?
                  gb.setDeploymentComment("Rollback during refactoring of " + context.getRefactoringElements().iterator().next());
                  gb.deploy(DeploymentMode.regenerateDeployedAllFeaturesXmlChanged, protectionMode);
                } catch (XPRC_DeploymentDuringUndeploymentException e) {
                  throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
                } catch (XPRC_InheritedConcurrentDeploymentException e) {
                  throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
                } catch (XPRC_MDMDeploymentException e) {
                  throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
                }
              } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
                throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
              } catch (XPRC_InvalidPackageNameException e) {
                throw new XDEV_RefactoringFault(changed.getFqXmlNameOld(), changed.getType().toString(), RefactoringType.MOVE.toString(), e);
              }
            }
          }

          if (progress.isDeployMovedDone()) {
            try {
              //undeploy des geänderten workflows. wenn das deploy geklappt hat, dann auch das undeploy!
              for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
                if (moved.getFqXmlNameNew().equals(moved.getFqXmlNameOld())) {
                  //usecase: labeländerung bei workflows
                  continue;
                }
                try {
                  GenerationBase gb = getGenerationBaseInstance(moved.getFqXmlNameNew(), moved.getType(), moved.getRevision());
                  gb.undeployRudimentarily(true);
                } catch (XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH e) {
                  throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(), RefactoringType.MOVE.toString(), e);
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new XDEV_RefactoringFault(moved.getFqXmlNameOld(), moved.getType().toString(), RefactoringType.MOVE.toString(), e);
                }
              }

            } finally {
              GenerationBase.finishUndeploymentHandler();
            }
          }
          if (progress.isMoveServiceLibsDone()) {
            for (XMLRefactoringUtils.XMOMObjectRefactoringResult moved : result.moved()) {
              undoMoveServiceLibs(moved.getFqXmlNameOld(), moved.getFqXmlNameNew(), config.refactorInDeploymentDir, moved.getRevision());
            }
          }
        }
      }
    }
  }
  
  
  private void updateDeploymentItemState(XMLRefactoringUtils.XMOMObjectRefactoringResult item, long revision,
                                         DeploymentLocation location, DeploymentItemStateManagement dism,
                                         Configuration config) {
    XMOMType type;
    switch (item.getType()) {
      case DATATYPE :
        type = XMOMType.DATATYPE;
        break;
      case EXCEPTION :
        type = XMOMType.EXCEPTION;
        break;
      case WORKFLOW :
        type = XMOMType.WORKFLOW;
        break;
      default :
        return;
      
    }
    if (!item.getFqXmlNameOld().equals(item.getFqXmlNameNew())) {
      DeploymentItemState dis = dism.get(item.getFqXmlNameOld(), revision);
      if (dis != null && dis.exists()) {
        dism.delete(item.getFqXmlNameOld(), DeploymentContext.dummy(), revision);
      }
    }
    try {
      Optional<DeploymentItem> di = DeploymentItemBuilder.build(item.getFqXmlNameNew(), Optional.<XMOMType>of(type), Collections.singleton(location), revision, false, config.getDeploymentItemCache());
      if (di.isPresent()) {
        dism.update(di.get(), Collections.singleton(location), revision);
      } else {
        logger.debug("Failed to build DeploymentItem for refactoring update of " + item.getFqXmlNameNew());
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.debug("Failed to build DeploymentItem for refactoring update of " + item.getFqXmlNameNew(), t);
    }
  }

  private void updateDeploymentMarker(XMLRefactoringUtils.XMOMObjectRefactoringResult item, long revision, DeploymentMarkerManagement dmm) {
    XMOMType type;
    switch (item.getType()) {
      case DATATYPE :
        type = XMOMType.DATATYPE;
        break;
      case EXCEPTION :
        type = XMOMType.EXCEPTION;
        break;
      case WORKFLOW :
        type = XMOMType.WORKFLOW;
        break;
      default :
        return;
    }
    
    DeploymentItemIdentifier oldDeploymentItem = new DeploymentItemIdentificationBase(type, item.getFqXmlNameOld());
    DeploymentItemIdentifier newDeploymentItem = new DeploymentItemIdentificationBase(type, item.getFqXmlNameNew());
    try {
      dmm.moveDeploymentMarker(oldDeploymentItem, newDeploymentItem, revision);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to move deploymentMarker for refactoring of " + item.getFqXmlNameOld(), e);
    }

  }
  
  private void moveServiceLibs(String fqXmlNameOld, String fqXmlNameNew, boolean deployed, Long revision) {
    String baseServicesPath = RevisionManagement.getPathForRevision(PathType.SERVICE, revision, deployed) + Constants.fileSeparator;
    try {
      String sourceServiceLibPath = baseServicesPath + GenerationBase.transformNameForJava(fqXmlNameOld);
      File sourceServiceLibDir = new File(sourceServiceLibPath);
      if (sourceServiceLibDir.exists()) {
        String targetServiceLibPath = baseServicesPath + GenerationBase.transformNameForJava(fqXmlNameNew);
        File targetServiceLibDir = new File(targetServiceLibPath);
        FileUtils.copyRecursivelyWithFolderStructure(sourceServiceLibDir, targetServiceLibDir);
        final String oldJarName = GenerationBase.getSimpleNameFromFQName(fqXmlNameOld) + "Impl.jar";
        final String newJarName = GenerationBase.getSimpleNameFromFQName(fqXmlNameNew) + "Impl.jar";
        File[] libraries = targetServiceLibDir.listFiles(new FilenameFilter() {

          public boolean accept(File dir, String name) {
            return name.equals(oldJarName);
          }
          
        });
        // throw if more then 1?
        for (File library : libraries) {
          library.renameTo(new File(targetServiceLibDir, newJarName));
        }
      }
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
    
  }
  
  
  private void undoMoveServiceLibs(String fqXmlNameOld, String fqXmlNameNew, boolean deployed, Long revision) {
    String baseServicesPath = RevisionManagement.getPathForRevision(PathType.SERVICE, revision, deployed) + Constants.fileSeparator;
    try {
      String targetServiceLibPath = baseServicesPath + GenerationBase.transformNameForJava(fqXmlNameNew);
      File targetServiceLibDir = new File(targetServiceLibPath);
      if (targetServiceLibDir.exists()) {
        FileUtils.deleteDirectoryRecursively(targetServiceLibDir);
      }
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }
  
  private Result executeRefactoring(Work xmlRefactoring, Configuration config, Long revision, RefactoringContext context)
      throws Ex_FileAccessException, XPRC_XmlParsingException, XDEV_RefactoringFault {
    Set<Long> parents = new HashSet<>();

    if (config.refactorInParentRuntimeContexts()) {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(revision, parents);
    }
    parents.add(revision);
    Result result = new Result();
    for (Long rev : parents) {
      Result r = XMLRefactoringUtils.executeWork(xmlRefactoring, config, rev, context);
      result.merge(r);
    }
    return result;
  }


  /**
   * - application-zugehörigkeit des workflows refactorn
   * - konfiguration, die am default-ordertype hängt, umhängen
   * - alle custom-ordertypes, die auf den workflow zeigen, umhängen
   * - alle crons, die den default-ordertype verwenden umziehen 
   * - inputsources
   */
  private void copyWorkflowConfiguration(String fqWFNameOld, String fqWFNameNew, RefactoringResult result, Long revision)
      throws XynaException {
    String fqClassNameOld;
    try {
      fqClassNameOld = GenerationBase.transformNameForJava(fqWFNameOld);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    String fqClassNameNew;
    try {
      fqClassNameNew = GenerationBase.transformNameForJava(fqWFNameNew);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }

    //2. ordertype-config: kopieren der config des zum workflow passenden default-ordertypes falls es ein execution-wf ist
    copyDefaultOrderTypeConfig(fqClassNameOld, fqClassNameNew, fqWFNameOld, result, revision);


    //3. ordertype-config2: alle nicht-default-ordertypes, die auf den workflow zeigen umziehen
    copyOrderTypeConfigForWorkflow(fqClassNameOld, fqClassNameNew, result, revision);

    //4. crons umkonfigurieren, die auf den default-ordertype zeigen
    //   andere crons sind durch die umzieherei von ordertype-configs bereits berücksichtigt
    changeOrderTypesOfCrons(fqClassNameOld, fqClassNameNew, result, revision);
    
    refactorOrderInputSources(DependencySourceType.ORDERTYPE, fqClassNameOld, fqClassNameNew, result, revision);
  }


  private void refactorOrderInputSources(DependencySourceType refactoredType, String fqClassNameOld, String fqClassNameNew,
                                                   RefactoringResult result, Long revision) throws XynaException {
    OrderInputSourceManagement orderInputSourceManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    //FIXME orderinputsources in parent-revisions refactorn
    String[] namesOfInputSources = orderInputSourceManagement.refactor(refactoredType, fqClassNameOld, fqClassNameNew, revision);
    for (String name : namesOfInputSources) {
      result.add(new XMOMObjectRefactoringResult(name, name, RefactoringTargetType.INPUTSOURCE, RefactoringType.CHANGE, null, revision));
    }
  }


  private void changeOrderTypesOfCrons(String fqClassNameOld, String fqClassNameNew, final RefactoringResult result, Long revision) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      final CronLikeScheduler crs = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
      FactoryWarehouseCursor<CronLikeOrder> cursorForCronLikeOrders =
          crs.getCursorForCronLikeOrders(con, 100, revision,
                                         new String[] {fqClassNameOld}, false);
      List<CronLikeOrder> remainingCacheOrNextIfEmpty;
      while (!(remainingCacheOrNextIfEmpty = cursorForCronLikeOrders.getRemainingCacheOrNextIfEmpty()).isEmpty()) {
        List<CronLikeOrder> changedOrders = new ArrayList<CronLikeOrder>();

        for (CronLikeOrder clo : remainingCacheOrNextIfEmpty) {
          final Long cronLikeOrderId = clo.getId();
          final CronLikeOrder cloToSave = new CronLikeOrder(cronLikeOrderId);
          crs.markAsNotToScheduleAndRemoveFromQueue(cronLikeOrderId);
          con.executeAfterCommitFails(new Runnable() {

            public void run() {
              crs.unmarkAsNotToSchedule(cronLikeOrderId);
            }
          });

          try {
            con.queryOneRowForUpdate(cloToSave);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // offensichtlich ist CLO schon ausgeführt wurden? Jedenfalls ist nichts zu tun.
            con.executeAfterCommit(new Runnable() {

              public void run() {
                crs.unmarkAsNotToSchedule(cronLikeOrderId);
              }
            });
            if (logger.isDebugEnabled()) {
              logger.debug("cron like order " + cronLikeOrderId
                  + " could not be changed because it has already been executed.");
            }
            continue; //nächste clo
          }

          //double check, falls inzwischen geändert...
          if (cloToSave.getCreationParameters().getOrderType().equals(fqClassNameOld)) {
            cloToSave.getCreationParameters().setOrderType(fqClassNameNew);
            if (cloToSave.getLabel().equals(fqClassNameOld)) {
              cloToSave.setLabel(fqClassNameNew);
            }
            cloToSave.setOrdertype(fqClassNameNew);
            if (logger.isTraceEnabled()) {
              logger.trace("cron like order " + cronLikeOrderId + " changing ordertype to " + fqClassNameNew);
            }
            con.executeAfterCommit(new Runnable() {

              public void run() {
                String cloId = "id=" + cloToSave.getId() + " label=" + cloToSave.getLabel();
                result.add(new XMOMObjectRefactoringResult(cloId, cloId, RefactoringTargetType.CRONJOB,
                                                           RefactoringType.CHANGE, null, cloToSave.getRevision()));
              }

            });
          } else {
            //speichern schadet nichts. müsste man nicht machen. kommt aber so selten vor, dass hier extra code nicht lohnt
          }

          con.persistObject(cloToSave);
          changedOrders.add(cloToSave);
        }
        //batch commit
        con.commit();

        //geänderte clos wieder aktivieren
        crs.tryAddNewOrders(changedOrders);
      }
    } finally {
      con.closeConnection();
    }
  }


  private void copyOrderTypeConfigForWorkflow(String fqClassNameOld, String fqClassNameNew, RefactoringResult result, Long revision)
      throws PersistenceLayerException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext;
    try {
      runtimeContext = revisionManagement.getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    FractalWorkflowDestination dv = new FractalWorkflowDestination(fqClassNameNew);
    XynaDispatcher[] dispatchers =
        new XynaDispatcher[] {
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                .getPlanningDispatcher(),
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
                .getCleanupEngineDispatcher(),
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                .getExecutionEngineDispatcher()};
    for (XynaDispatcher dispatcher : dispatchers) {
      Map<DestinationKey, DestinationValue> destinations = dispatcher.getDestinations();
      for (Entry<DestinationKey, DestinationValue> dest : destinations.entrySet()) {
        //alle im runtimeContext definierten destinations umbiegen, die keine default-destinations sind
        if (dest.getValue().getFQName().equals(fqClassNameOld) && !dest.getKey().getOrderType().equals(fqClassNameOld)
            && dest.getKey().getRuntimeContext().equals(runtimeContext)) {
          //ordertype, der auf den alten workflow zeigt soll jetzt auf den neuen zeigen
          dispatcher.setCustomDestination(dest.getKey(), dv);
          result.add(new XMOMObjectRefactoringResult(dest.getKey().getOrderType(), dest.getKey().getOrderType(),
                                                     RefactoringTargetType.ORDERTYPE_CONFIG, RefactoringType.CHANGE,
                                                     null, revision));
        }
      }
    }
  }


  /**
   * Kopieren der Config des zum Workflow passenden Default-Ordertypes, falls es ein Execution-WF ist
   * @param fqClassNameOld
   * @param fqClassNameNew
   * @param fqWFNameOld
   * @param result
   * @param revision
   * @throws PersistenceLayerException
   */
  private void copyDefaultOrderTypeConfig(String fqClassNameOld, String fqClassNameNew, String fqWFNameOld, RefactoringResult result, Long revision)
      throws PersistenceLayerException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext;
    try {
      runtimeContext = revisionManagement.getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    DestinationKey dk = new DestinationKey(fqClassNameOld, runtimeContext);
    DestinationValue destinationExecution = null;
    try {
      destinationExecution =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
              .getExecutionDestination(dk);
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      //kann passieren, wenn workflow nicht richtig deployed war, das xml aber vorhanden war
      logger.warn("workflow " + fqWFNameOld + " was not deployed correctly.", e);
    }


    if (destinationExecution != null && destinationExecution.getFQName().equals(fqClassNameOld)) {
      //ist also der defaultordertype für den workflow und der workflow ist kein planning-workflow
      //wenn der workflow ein planningworkflow ist, hat der zugehörige defaultordertype keine konfiguration
      //TODO das ist unsicher, man könnte natürlich auch komisch konfiguriert haben

      OrdertypeManagement orderTypeManagement =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
      try {
        OrdertypeParameter ordertypeInfo = orderTypeManagement.getOrdertype(fqClassNameOld, runtimeContext);
        ordertypeInfo.setOrdertypeName(fqClassNameNew);
        ordertypeInfo.setExecutionDestinationValue(null); //neu setzen: null = default = (workflow = ordertype)
        try {
          orderTypeManagement.modifyOrdertype(ordertypeInfo);
          result.add(new XMOMObjectRefactoringResult(fqClassNameNew, fqClassNameOld,
                                                     RefactoringTargetType.ORDERTYPE_CONFIG, RefactoringType.MOVE,
                                                     null, revision));
        } catch (XFMG_InvalidCapacityCardinality | XFMG_InvalidModificationOfUnexistingOrdertype e) {
          throw new RuntimeException(e);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("workflow " + fqWFNameOld + " was not deployed correctly.", e);
        return;
      }
    }
    //else der defaultordertype wird für irgendwas anderes verwendet. dann braucht man auch nichts umzuziehen.
  }


  private void copyApplicationInfo(String fqNameOld, String fqNameNew, RefactoringTargetType type,
                                   RefactoringResult result, Long revision) throws PersistenceLayerException {
    if (!type.hasCorrespondingApplicationEntryType()) {
      return;
    }
    
    ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getApplicationManagement();
    
    String[] applications =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement()
            .getApplicationsContainingObject(fqNameOld, type.getApplicationEntryType(), revision);
    for (String application : applications) {
      try {
        appMgmt.addXMOMObjectToApplication(fqNameNew, application, revision, new EmptyRepositoryEvent());
        appMgmt.removeXMOMObjectFromApplication(application, fqNameOld, revision, new EmptyRepositoryEvent());
        result.add(new XMOMObjectRefactoringResult(application, application, RefactoringTargetType.APPLICATION,
                                                   RefactoringType.CHANGE, null, revision));
      } catch (XFMG_FailedToAddObjectToApplication e) {
        throw new RuntimeException(e);
      } catch (XFMG_FailedToRemoveObjectFromApplication e) {
        throw new RuntimeException(e);
      } catch (XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects e) {
        throw new RuntimeException(e);
      } catch (XFMG_ObjectNotFoundException e) {
        //ok, wurde inzwischen gelöscht
      }
    }

    if (ApplicationEntryType.WORKFLOW.equals(type.getApplicationEntryType())) {
      String fqClassNameOld;
      String fqClassNameNew;
      try {
        fqClassNameOld = GenerationBase.transformNameForJava(fqNameOld);
        fqClassNameNew = GenerationBase.transformNameForJava(fqNameNew);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
      
      applications = appMgmt.getApplicationsContainingObject(fqClassNameOld, ApplicationEntryType.ORDERTYPE, revision);

      for (String application : applications) {
        try {
          appMgmt.addNonModelledObjectToApplication(fqClassNameNew, application, null,
                                         ApplicationEntryType.ORDERTYPE, revision,
                                         new EmptyRepositoryEvent(), false, null);
          appMgmt.removeNonModelledObjectFromApplication(application, null, fqClassNameOld,
                                             ApplicationEntryType.ORDERTYPE, revision,
                                             new EmptyRepositoryEvent(), false, null);
          result.add(new XMOMObjectRefactoringResult(application, application, RefactoringTargetType.APPLICATION,
                                                     RefactoringType.CHANGE, null, revision));
        } catch (XFMG_FailedToAddObjectToApplication e) {
          throw new RuntimeException(e);
        } catch (XFMG_FailedToRemoveObjectFromApplication e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


  public static final String DEFAULT_NAME = "RefactoringManagement";


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }
  
  
  private GenerationBase getGenerationBaseInstance(String fqXmlName, RefactoringTargetType type, Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    switch (type) {
      case WORKFLOW :
        return WF.getInstance(fqXmlName, revision);
      case DATATYPE :
        return DOM.getInstance(fqXmlName, revision);
      case EXCEPTION :
        return ExceptionGeneration.getInstance(fqXmlName, revision);
      default :
        throw new IllegalArgumentException("illegal type for get GenerationBase instance " + type.toString());
    }
  }
  
  
  private static class WorkBasedAutosaveFilter implements AutosaveFilter {
    
    private Work currentRefactoring;
    
    private WorkBasedAutosaveFilter(Work currentRefactoring) {
      this.currentRefactoring = currentRefactoring;
    }

    public boolean detect(Path path, String type, String payload) {
      Document doc;
      try {
        doc = XMLUtils.parseString(payload);
        DocumentOrder order = currentRefactoring.work(doc);
        if (order.type != DocumentOrderType.NOTHING) {
          String fileLocation = GenerationBase.getFileLocationOfXmlNameForSaving(path.getPath(), path.getRevision()) + ".xml";
          Document savedDoc = XMLUtils.parse(fileLocation);
          DocumentOrder savedOrder = currentRefactoring.work(savedDoc);
          if (savedOrder.type == DocumentOrderType.NOTHING) {
            return true;
          }
        }
      } catch (XPRC_XmlParsingException e) {
        // deny unparseable autosaves during refactoring
        return true;
      } catch (Ex_FileAccessException e) {
        return true;
      }
      return false;
    }
    
  }
  
  
  static DeploymentItemStateManagement getDeploymentItemStateManagement() {
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    if (dism != null && dism.isInitialized()) {
      return dism;
    } else {
      return null;
    }
  }
  

}
