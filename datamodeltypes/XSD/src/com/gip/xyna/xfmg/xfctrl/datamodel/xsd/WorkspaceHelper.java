/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.ImportParameter.WorkspaceChangeMode;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;
import com.gip.xyna.xprc.xpce.WorkflowEngine;


public class WorkspaceHelper {
  
  private final static String TEMPORARY_COPY_INTO_WORKSPACE_USER_ROLE = UserManagement.ADMIN_ROLE_NAME;
  
  private DataModelResult dataModelResult;
  private DataModelStorage dataModelStorage;
  private DataModel dataModel;
  private List<String> datatypes;
  private boolean failed;
  private GenerationParameter generationParameter;
  private AdditionalWorkspaceOperations additionalWorkspaceOperations;
  private String username;
  private String sessionId;
  
  public interface AdditionalWorkspaceOperations {

    void initialize(DataModelResult dataModelResult, GenerationParameter generationParameter, long revision);

    void clean(DataModelResult dataModelResult, String dataModelName, long revision);
    
  }
  
  public WorkspaceHelper(DataModelResult dataModelResult, DataModelStorage dataModelStorage, DataModel dataModel,
                         List<String> datatypes, GenerationParameter generationParameter,
                         AdditionalWorkspaceOperations additionalWorkspaceOperations) {
    this.dataModelResult = dataModelResult;
    this.dataModelStorage = dataModelStorage;
    this.dataModel = dataModel;
    this.datatypes = datatypes;
    this.generationParameter = generationParameter;
    this.additionalWorkspaceOperations = additionalWorkspaceOperations;
  }
  
  public void setCredentials(String username, String sessionId) {
    this.username = username;
    this.sessionId = sessionId;
  }

  public boolean addWorkspaces(Collection<String> workspaces) {
    Set<String> existingWorkspaces = new HashSet<String>();
    return configureWorkspaces(existingWorkspaces, workspaces, WorkspaceChangeMode.Add);
  }
  
  public boolean modifyWorkspaces(Collection<String> workspaces, WorkspaceChangeMode workspaceMode) {
    Set<String> existingWorkspaces = DataModelUtils.getWorkspaces(dataModel); //derzeitige Workspaces
    return configureWorkspaces(existingWorkspaces, workspaces, workspaceMode);
  }

  public boolean removeWorkspaces(Collection<String> workspaces) {
    Set<String> existingWorkspaces = DataModelUtils.getWorkspaces(dataModel); //derzeitige Workspaces
    return configureWorkspaces(existingWorkspaces, workspaces, WorkspaceChangeMode.Remove);
  }
  
  private boolean configureWorkspaces( Set<String> existingWorkspaces, Collection<String> workspaces, WorkspaceChangeMode mode ) {
    Set<String> existing = new HashSet<String>();
    Set<String> nonExist = new HashSet<String>();

    for( String wsp : workspaces ) {
      if( existingWorkspaces.contains(wsp) ) {
        existing.add(wsp);
      } else {
        nonExist.add(wsp);
      }
    }
    Collection<String> toAdd = null;
    Collection<String> toRemove = null;

    switch( mode ) {
      case Add:
        //Existing sind zuviel, daher Info ausgeben
        if( ! existing.isEmpty() ) {
          dataModelResult.addMessageGroup("Already existing workspaces", existing);
        }
        toAdd = nonExist; //NonExist anlegen
        break;
      case Remove:
        //NonExist sind zuviel, daher Info ausgeben
        if( ! nonExist.isEmpty() ) {
          dataModelResult.addMessageGroup("Not existing workspaces", nonExist);
        }
        //toRemove = existing; //Existing entfernen 
        toRemove = workspaces; //trotzdem versuchen alle zu entfernen, evtl. war ja DataModelSpecific unvollst�ndig gespeichert
        break;
      case Target:
        toAdd = nonExist;//NonExist anlegen
        //existingWorkspaces enth�lt nun noch evtl. zu l�schende Eintr�ge
        toRemove = new ArrayList<String>(existingWorkspaces);
        toRemove.removeAll(workspaces);
        break;
      default:
        throw new IllegalArgumentException();
    }
    
    //Workspaces hinzuf�gen
    List<String> added = addWorkspacesInternal(toAdd);
    existingWorkspaces.addAll(added);
    
    //Workspaces entfernen
    List<String> removed = removeWorkspacesInternal(toRemove);
    existingWorkspaces.removeAll(removed);
    
    //Speichern der Workspaces
    try {
      List<String> allWorkspaces = new ArrayList<String>(existingWorkspaces);
      Collections.sort(allWorkspaces);
      dataModelStorage.replaceDataModelSpecifics( dataModel,
                                                  DataModelUtils.extractWorkspaces(dataModel), 
                                                  DataModelUtils.listWorkspaces(allWorkspaces) );
    } catch( PersistenceLayerException e ) {
      dataModelResult.fail("Trying to save workspaces in dataModel", e);
      return false;
    }

    return !failed;
  }

  private List<String> addWorkspacesInternal(Collection<String> toAdd) {
    List<String> added = new ArrayList<String>();
    if( toAdd == null ) {
      return added;
    }
    for( String workspace : toAdd ) {
      try {
        //FIXME generationParameter.isOverwrite() verwenden
        copyToWorkspace(dataModelResult, datatypes, workspace);
        if( added == null ) {
          added = new ArrayList<String>();
        }
        added.add(workspace);
      } catch ( XynaException e ) {
        //XFMG_NoSuchRevision, MDMParallelDeploymentException, Ex_FileAccessException, 
        //XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException
        dataModelResult.fail("Could not copy datamodel to workspace \""+workspace+"\"", e);
        failed = true;
        break;
      }
    }
    if( ! added.isEmpty() ) {
      StringBuilder sb = new StringBuilder();
      sb.append("Successfully saved and deployed ").append(datatypes.size()).append(" Data Types to ");
      appendWorkspaces(sb, added);
      dataModelResult.info(sb.toString());
    }
    return added;
  }
  
  private List<String> removeWorkspacesInternal(Collection<String> toRemove) {
    List<String> removed = new ArrayList<String>();
    if( toRemove == null ) {
      return removed;
    }
    if( toRemove != null ) {
      for( String workspace : toRemove ) {
        boolean success = removeFromWorkspace(workspace);
        if( success ) {
          if( removed == null ) {
            removed = new ArrayList<String>();
          }
          removed.add(workspace);
        }
        //TODO Fehler beim L�schen?
      }
    }
    if( ! removed.isEmpty() ) {
      StringBuilder sb = new StringBuilder();
      sb.append("Successfully undeployed and deleted ").append(datatypes.size()).append(" Data Types from ");
      appendWorkspaces(sb, removed);
      dataModelResult.info(sb.toString());
    }
    return removed;
  }

  private boolean removeFromWorkspace(String workspace) {
    Long revision; 
    try {
      revision = revisionFor(workspace);
    } catch( XFMG_NoSuchRevision e ) {
      dataModelResult.warn("Workspace \""+workspace+"\" does not exist");
      return true; //diesen Workspace �berspringen
    }
    
    try {
      return removeFromWorkspace(revision);
    } catch ( XynaException e ) {
      //XPRC_ExclusiveDeploymentInProgress, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      //XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT
      dataModelResult.fail("Could not delete XMOMObjects from workspace \""+workspace+"\"", e );
      return false; //Abbruch
    }
  }
  
  public boolean removeFromWorkspace(long revision) throws XynaException {
    //TODO wie ermitteln, ob noch in Verwendung? dies kann derzeit nicht gepr�ft werden!
    WorkflowEngine workflowEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    
    EnumMap<XMOMType,List<String>> xmomObjects = new EnumMap<XMOMType,List<String>>(XMOMType.class);
    xmomObjects.put(XMOMType.DATATYPE,datatypes);
    
    if( additionalWorkspaceOperations != null ) {
      additionalWorkspaceOperations.clean(dataModelResult, dataModel.getType().getLabel(), revision);
    }
    
    workflowEngine.deleteXMOMObjects(xmomObjects, false, DependentObjectMode.INVALIDATE, true, revision);
    
    return true;
  }

  private static void appendWorkspaces(StringBuilder sb, List<String> workspaces) {
    sb.append("workspace");
    String sep = workspaces.size() == 1 ? " ": "s ";
    for( String w : workspaces ) {
      sb.append(sep).append("\"").append(w).append("\"");
      sep = ", ";
    }
  }

  private boolean copyToWorkspace(DataModelResult dataModelResult, List<String> datatypes, String toWorkspace) 
      throws XFMG_NoSuchRevision, MDMParallelDeploymentException, Ex_FileAccessException, XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException, PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter, XFMG_PredefinedXynaObjectException, XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_DuplicateSessionException {
    WorkflowEngine workflowEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    
    Long toRevision = revisionFor(toWorkspace);
    if( toRevision == null ) {
      dataModelResult.warn("Workspace \""+toWorkspace+"\" does not exist");
      return true; //diesen Workspace �berspringen
    }

    createBaseTypeIfNotExists(toRevision);
    if( additionalWorkspaceOperations != null ) {
      additionalWorkspaceOperations.initialize(dataModelResult,generationParameter,toRevision);
    }
    
    EnumMap<XMOMType,List<String>> xmomObjects = new EnumMap<XMOMType,List<String>>(XMOMType.class);
    xmomObjects.put(XMOMType.DATATYPE,datatypes);
    
    String comment = "Copy Datamodel to Workspace";
    if (username != null && sessionId != null) {
      workflowEngine.copyToRevisionAndDeploy(xmomObjects, RevisionManagement.REVISION_DATAMODEL, toRevision, 
                                             DeploymentMode.codeChanged, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, false, username, sessionId, comment);
    } else {
      TemporarySessionAuthentication tempAuth = createTempAuth(toRevision);
      tempAuth.initiate();
      try {
        workflowEngine.copyToRevisionAndDeploy(xmomObjects, RevisionManagement.REVISION_DATAMODEL, toRevision, 
                                               DeploymentMode.codeChanged, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, false,
                                               tempAuth.getUsername(), tempAuth.getSessionId(), comment);
      } finally {
        tempAuth.destroy();
      }
    }
    return true;
  }
  

  private TemporarySessionAuthentication createTempAuth(Long revision) {
    return TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("XSD_Import", TEMPORARY_COPY_INTO_WORKSPACE_USER_ROLE, revision,
                                                                                 Operation.XMOM_SAVE);
  }


  private static Long revisionFor(String workspace) throws XFMG_NoSuchRevision {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      return rm.getRevision(new Workspace(workspace));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(workspace, e);
    }
  }

  public static void createBaseTypeIfNotExists(long revision) throws XFMG_NoSuchRevision, Ex_FileWriteException {
    Datatype datatype = createBaseType();
    XmomGenerator xmomGenerator = XmomGenerator.inRevision(revision).build();
    if( ! xmomGenerator.exists(datatype) ) {
      xmomGenerator.add(datatype);
      xmomGenerator.save();
    }
  }
  
  public static Datatype createBaseType() {
    return new Datatype(Constants.getBase_XmomType());
  }

}
