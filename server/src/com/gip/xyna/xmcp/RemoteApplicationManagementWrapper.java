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
package com.gip.xyna.xmcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotExportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.CopyCLOResult;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightCache;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class RemoteApplicationManagementWrapper {

  
  public void defineApplication(Workspace workspace, String applicationName, String comment, String user, Role role)
                  throws XynaException, RemoteException {
    if (workspace == null) {
      workspace = RevisionManagement.DEFAULT_WORKSPACE;
    }
    AccessRightScope.DEFINE_APPLICATION.hasRight(role, applicationName, null, workspace);
    Long parentRevision = getRevision(workspace);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_DEFINE, parentRevision);
    try {
      getAppMgmt().defineApplication(applicationName, comment, parentRevision, user);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_DEFINE, parentRevision);
    }
  }



  public CopyCLOResult copyCronLikeOrders(String applicationName, String sourceVersion, String targetVersion, String id, String[] ordertypes,
                                          boolean move, boolean global, Role role) throws XynaException, RemoteException {
    AccessRightScope.COPY_CRON.hasRight(role, applicationName, targetVersion, null);
    Long revision = getRevision(new Application(applicationName, targetVersion));
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_CRONS, revision);
    try {
      return getAppMgmt().copyCronLikeOrders(applicationName, sourceVersion, targetVersion, null, id, ordertypes, move, false, global);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_CRONS, revision);
    }
  }


  public void copyOrderTypes(String applicationName, String sourceVersion, String targetVersion, Role role)
                  throws XynaException, RemoteException {
    AccessRightScope.COPY_ORDERTYPE.hasRight(role, applicationName, targetVersion, null);
    Long revision = getRevision(new Application(applicationName, targetVersion));
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, revision);
    try {
      getAppMgmt().copyOrderTypes(applicationName, sourceVersion, targetVersion, null);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, revision);
    }
  }


  public void addObjectToApplication(Workspace workspace, String objectName, String applicationName, ApplicationEntryType entryType, Role role)
                  throws XynaException, RemoteException {
    if (workspace == null) {
      workspace = RevisionManagement.DEFAULT_WORKSPACE;
    }
    AccessRightScope.ADD_OBJECT.hasRight(role, applicationName, null, workspace);
    Long parentRevision = getRevision(workspace);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_ADDOBJECT, parentRevision);
    try {
      switch (entryType) {
        case DATATYPE :
        case EXCEPTION :
        case WORKFLOW :
          ((ApplicationManagementImpl)getAppMgmt()).addXMOMObjectToApplication(objectName, applicationName, parentRevision);
          break;
        case TRIGGERINSTANCE :
          ((ApplicationManagementImpl)getAppMgmt()).addTriggerInstanceToApplication(objectName, applicationName, parentRevision, false, null);
          break;
        case FILTERINSTANCE :
          ((ApplicationManagementImpl)getAppMgmt()).addFilterInstanceToApplication(objectName, applicationName, parentRevision, false, null);
          break;
        default :
          ((ApplicationManagementImpl)getAppMgmt()).addNonModelledObjectToApplication(objectName, applicationName, null, entryType, parentRevision, false, null);
          break;
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_ADDOBJECT, parentRevision);
    }
  }

  
  public String exportApplication(String applicationName, String version, ExportApplicationBuildParameter buildParams, Role role) 
                  throws XynaException, RemoteException {
    if (version == null) {
      version = ApplicationManagementImpl.WORKINGSET_VERSION_NAME;
    }
    AccessRightScope.EXPORT_APPLICATION.hasRight(role, applicationName, version, null);
    Long revision = getRevision(new Application(applicationName, version));
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_EXPORT, revision);
    try {
      String exportFileName = applicationName + "_" + version + "_" + System.currentTimeMillis();
      boolean createStub = false;
      try {
        File tmpFile = File.createTempFile(exportFileName, null);
        ((ApplicationManagementImpl)getAppMgmt()).exportApplication(applicationName, version, tmpFile.getPath(),
                                                                    buildParams.getLocalBuild(), buildParams.getNewVersion(),
                                                                    buildParams.getLocal(), false, createStub, null, buildParams.getUser());
      
        FileInputStream fis = new FileInputStream(tmpFile);
        try {
          return getFileMgmt().store(role.getName(), exportFileName, fis);
        } finally {
          fis.close();
          tmpFile.delete();
        }
      } catch (FileNotFoundException e) {
        // Would mean createTempFile failed without throwing an error? 
        throw new RuntimeException("Failed to open stream to created tempFile", e);
      } catch (IOException e) {
        throw new XFMG_CouldNotExportApplication(applicationName, version, e);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_EXPORT, revision);
    }
  }


  public void importApplication(String fileManagementId, ImportApplicationParameter importParams, Role role)
                  throws XynaException, RemoteException {
    AccessRightScope.IMPORT_APPLICATION.hasRight(role, null, null, null);
    try {
      File tmpFile = File.createTempFile("appImport", null);
      TransientFile file = getFileMgmt().retrieve(fileManagementId);
      InputStream is = file.openInputStream();
      try {
        FileUtils.writeStreamToFile(is, tmpFile);
      } finally {
        is.close();
        file = null;
      }
      ((ApplicationManagementImpl)getAppMgmt()).importApplication(tmpFile.getPath(), importParams.getOverride(), importParams.isStopIfExistingAndRunning(),
                                                                  importParams.getXynaProperties() == ApplicationPartImportMode.EXCLUDE,
                                                                  importParams.getCapacities() == ApplicationPartImportMode.EXCLUDE,
                                                                  importParams.getXynaProperties() == ApplicationPartImportMode.ONLY,
                                                                  importParams.getCapacities() == ApplicationPartImportMode.ONLY,
                                                                  !importParams.getLocal(), false, false, importParams.getUser(), null, true, true);
    } catch (IOException e) {
      throw new XFMG_CouldNotImportApplication("FileManagementId: "+fileManagementId);
    }
  }

  
  public void removeObjectFromApplication(Workspace workspace, String applicationName, String objectName, ApplicationEntryType entryType, Role role)
                  throws XynaException, RemoteException {
    if (workspace == null) {
      workspace = RevisionManagement.DEFAULT_WORKSPACE;
    }
    AccessRightScope.REMOVE_OBJECT.hasRight(role, applicationName, null, workspace);
    Long parentRevision = getRevision(workspace);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_REMOVE_OBJECT, parentRevision);
    try {
      switch (entryType) {
        case DATATYPE :
        case EXCEPTION :
        case WORKFLOW :
          ((ApplicationManagementImpl)getAppMgmt()).removeXMOMObjectFromApplication(applicationName, objectName, parentRevision);
          break;
        case TRIGGERINSTANCE :
          ((ApplicationManagementImpl)getAppMgmt()).removeTriggerInstanceFromApplication(applicationName, objectName, parentRevision, false, null);
          break;
        case FILTERINSTANCE :
          ((ApplicationManagementImpl)getAppMgmt()).removeFilterInstanceFromApplication(applicationName, objectName, parentRevision, false, null);
          break;
        default :
          ((ApplicationManagementImpl)getAppMgmt()).removeNonModelledObjectFromApplication(applicationName, null, objectName, entryType, parentRevision, false, null);
          break;
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_REMOVE_OBJECT, parentRevision);
    }
  }

  public void removeApplicationDefinition(String applicationName, RemoveApplicationParameters params, Role role)
                  throws XynaException, RemoteException {
    Workspace workspace = params.getParentWorkspace();
    if (workspace == null) {
      workspace = RevisionManagement.DEFAULT_WORKSPACE;
    }
    AccessRightScope.REMOVE_APPLICATION_DEFINITION.hasRight(role, applicationName, null, workspace);

    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    Long parentRevision = getRevision(workspace);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_REMOVE_DEFINITION, parentRevision);
    try {
      ApplicationName application = new ApplicationName(applicationName, null);
      applicationManagement.removeApplicationVersion(application, params, false, null, new SingleRepositoryEvent(parentRevision), true);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_REMOVE_DEFINITION, parentRevision);
    }
  }


  public SerializablePair<ArrayList<ApplicationEntryStorable>, ArrayList<ApplicationEntryStorable>> 
            listApplicationDetails(Workspace workspace, String applicationName, String version, boolean includingDependencies, Role role)
                            throws XynaException, RemoteException {
    AccessRightScope.LIST_APPLICATION_DETAILS.hasRight(role, applicationName, version == null ? ApplicationManagementImpl.WORKINGSET_VERSION_NAME : version, workspace == null ? RevisionManagement.DEFAULT_WORKSPACE : workspace);
    Long parentRevision = null;
    if (workspace != null) {
      parentRevision = getRevision(workspace);
    }
    List<ApplicationEntryStorable> plainEntries = ((ApplicationManagementImpl)getAppMgmt()).listApplicationDetails(applicationName, version, false, null, parentRevision);
    TreeSet<ApplicationEntryStorable> plainSet = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    if (plainEntries != null) {
      plainSet.addAll(plainEntries);
    }
    TreeSet<ApplicationEntryStorable> dependencySet = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    if (includingDependencies) {
      List<ApplicationEntryStorable> includingDeps = ((ApplicationManagementImpl)getAppMgmt()).listApplicationDetails(applicationName, version, true, null, parentRevision);
      if (includingDeps != null) {
        dependencySet.addAll(includingDeps);
      }
      dependencySet.removeAll(plainSet);
    }
    return SerializablePair.of(new ArrayList<ApplicationEntryStorable>(plainSet), new ArrayList<ApplicationEntryStorable>(dependencySet));
  }


  private ApplicationManagement getAppMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
  }
  
  
  private FileManagement getFileMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  }
  
  
  private Long getRevision(RuntimeContext context) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(context);
  }
  
  
  private static enum AccessRightScope {
    DEFINE_APPLICATION(UserManagement.ScopedRight.APPLICATION_DEFINITION, UserManagement.Action.insert),
    REMOVE_APPLICATION_DEFINITION(UserManagement.ScopedRight.APPLICATION_DEFINITION, UserManagement.Action.delete),
    ADD_OBJECT(UserManagement.ScopedRight.APPLICATION_DEFINITION, UserManagement.Action.write),
    REMOVE_OBJECT(UserManagement.ScopedRight.APPLICATION_DEFINITION, UserManagement.Action.write),
    IMPORT_APPLICATION(UserManagement.ScopedRight.APPLICATION, UserManagement.Action.deploy) {
      @Override
      public boolean hasRight(Role role, String applicationName, String version, Workspace workspace)
                      throws XynaException {
        // we don't have an applicationName to check upon
        // should we check a global AppMgmt-Right for imports?
        return true;
      }
    },
    EXPORT_APPLICATION(UserManagement.ScopedRight.APPLICATION, UserManagement.Action.deploy),
    COPY_CRON(UserManagement.ScopedRight.APPLICATION, UserManagement.Action.migrate),
    COPY_ORDERTYPE(UserManagement.ScopedRight.APPLICATION, UserManagement.Action.migrate),
    LIST_APPLICATION_DETAILS(UserManagement.ScopedRight.APPLICATION, UserManagement.Action.list)
    ;
    
    private final UserManagement.ScopedRight right;    
    private final UserManagement.Action action;
    
    private AccessRightScope(UserManagement.ScopedRight right, UserManagement.Action action) {
      this.right = right;
      this.action = action;
    }
    
    
    private String[] getScopeParts(String applicationName, String version, Workspace workspace) {
      String[] parts = new String[3];
      parts[0] = action.toString();
      switch (right) {
        case APPLICATION:
          parts[1] = applicationName;
          parts[2] = version;
          break;
        case APPLICATION_DEFINITION:
          parts[1] = workspace.getName();
          parts[2] = applicationName;
          break;
        default: 
          throw new IllegalArgumentException("Illegale AccessRightScope definition!");
      }
      return parts;
    }
    
    
    public boolean hasRight(Role role, String applicationName, String version, Workspace workspace) throws XynaException {
      ScopedRightCache rightCache = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRoleRightScope(role);
      if (!rightCache.hasRight(right.getKey(), getScopeParts(applicationName, version, workspace))) {
        throw new XFMG_ACCESS_VIOLATION(this.name() + " app: " + applicationName, rightCache.getRoleName());
      } else {
        return true;
      }
    }
    
  }
}
