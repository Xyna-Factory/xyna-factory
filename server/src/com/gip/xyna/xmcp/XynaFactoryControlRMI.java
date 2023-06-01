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

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.CopyCLOResult;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.ClearWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;


public interface XynaFactoryControlRMI extends Remote {

  @Deprecated
  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void buildApplicationVersion(XynaCredentials credentials, String applicationName, String versionName, String comment) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void buildApplicationVersion(XynaCredentials credentials, String applicationName, String versionName, BuildApplicationVersionParameters params) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void copyApplicationIntoWorkingSet(XynaCredentials credentials, String applicationName, String versionName, String comment, boolean overrideChanges) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void copyApplicationIntoWorkspace(XynaCredentials credentials, String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params) throws XynaException, RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void clearWorkingSet(XynaCredentials credentials, boolean ignoreRunningOrders) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void clearWorkspace(XynaCredentials credentials, Workspace workspace, ClearWorkspaceParameters params) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.WORKSPACE, action=Action.list, nochecks=true)
  public List<WorkspaceInformation> listWorkspaces(XynaCredentials credentials, boolean includeProblems) throws RemoteException;

  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<ApplicationDefinitionInformation> listApplicationDefinitions(XynaCredentials credentials, boolean includeProblems) throws RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public CreateWorkspaceResult createWorkspace(XynaCredentials credentials, Workspace workspace) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public CreateWorkspaceResult createWorkspace(XynaCredentials credentials, Workspace workspace, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.WORKSPACE)
  public void removeWorkspace(XynaCredentials credentials, Workspace workspace, RemoveWorkspaceParameters params) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION_DEFINITION, action=Action.insert, checks={1,2})
  public void defineApplication(XynaCredentials credentials, Workspace workspace, String applicationName, String comment) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.APPLICATION_DEFINITION, action=Action.insert, checks={1,2})
  public void defineApplication(XynaCredentials credentials, Workspace workspace, String applicationName, String comment, String user) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.APPLICATION_DEFINITION, action=Action.write, checks={1,2})
  public void removeApplicationDefinition(XynaCredentials credentials, String applicationName, RemoveApplicationParameters params) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.migrate, checks={1,3})
  public CopyCLOResult copyCronLikeOrders(XynaCredentials credentials, String applicationName, String sourceVersion, String targetVersion,
                                          String id, String[] ordertypes, boolean move, boolean global) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.migrate, checks={1,3})
  public void copyOrderTypes(XynaCredentials credentials, String applicationName, String sourceVersion, String targetVersion) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.list, checks={2,3})
  public <L extends Serializable & Collection<ApplicationEntryStorable>> SerializablePair<L, L>
           listApplicationDetails(XynaCredentials credentials, Workspace workspace,
                                  String applicationName, String version, boolean includingDependencies) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION_DEFINITION, action=Action.write, checks={1,3})
  public void addObjectToApplication(XynaCredentials credentials, Workspace workspace, String objectName,
                                     String applicationName, ApplicationEntryType entryType) throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.deploy, checks={1,2})
  public String /* fileManagementId */ exportApplication(XynaCredentials credentials, String applicationName, String versionName,
                                                         ExportApplicationBuildParameter buildParams) throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.deploy, checks=1)
  public void importApplication(XynaCredentials credentials, String fileManagementId, ImportApplicationParameter importParams) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION_DEFINITION, action=Action.write, checks={1,2})
  public void removeObjectFromApplication(XynaCredentials credentials, Workspace workspace, String applicationName, String objectName,
                                          ApplicationEntryType entryType, boolean force) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MARKER, action=Action.insert, checks=1)
  public DeploymentMarker createDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws XynaException, RemoteException;

  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MARKER, action=Action.delete, checks=1)
  public void deleteDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.DEPLOYMENT_MARKER, action=Action.write, checks=1)
  public void modifyDeploymentMarker(XynaCredentials credentials, DeploymentMarker marker) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.RUNTIMECONTEXT_MANAGEMENT, action=Action.write, checks=1)
  public void modifyRuntimeContextDependencies(XynaCredentials credentials, RuntimeDependencyContext owner, List<RuntimeDependencyContext> newDependencies, boolean force, String user) throws XynaException, RemoteException;

}
