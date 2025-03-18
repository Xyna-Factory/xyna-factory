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
package xmcp.gitintegration.impl;



import base.File;
import base.Text;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xmcp.gitintegration.Flag;
import xmcp.gitintegration.InfoWorkspaceContentDiffGroupList;
import xmcp.gitintegration.ListId;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceContentDifferencesResolution;
import xprc.xpce.Workspace;
import xmcp.gitintegration.WorkspaceObjectManagementServiceOperation;
import xmcp.gitintegration.WorkspaceXmlCreationConfig;
import xmcp.gitintegration.WorkspaceXmlPath;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.tools.CreateWorkspaceXmlTools;
import xmcp.gitintegration.tools.ResolveWorkspaceDiffsTools;
import xmcp.gitintegration.tools.WorkspaceStatusTools;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;



public class WorkspaceObjectManagementServiceOperationImpl implements ExtendedDeploymentTask, WorkspaceObjectManagementServiceOperation {

  public void onDeployment() throws XynaException {
    //TODO: register @ GuiHttp => new entry in factory manager
    // make sure calling it multiple times behaves well
    WorkspaceDifferenceListStorage.init();
    OverallInformationProvider.onDeployment();
  }


  public void onUndeployment() throws XynaException {
    // TODO unregister @ GuiHttp => new entry in factory manager

    OverallInformationProvider.onUndeployment();
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    //TODO: BehaviorAfterOnUnDeploymentTimeout.EXCEPTION once new entries in factory manager can be registered
    return null;
  }


  @Override
  public WorkspaceContentDifferences compareWorkspaceContent(WorkspaceContent workspaceContent3, WorkspaceContent workspaceContent4) {
    WorkspaceContentComparator comparator = new WorkspaceContentComparator();
    // first parameter: from => XML
    // second parameter: to => current configuration
    WorkspaceContentDifferences result = comparator.compareWorkspaceContent(workspaceContent3, workspaceContent4, true);
    return result;
  }


  @Override
  public WorkspaceContent createWorkspaceContent(Workspace workspace) {
    WorkspaceContentCreator contentCreator = new WorkspaceContentCreator();
    WorkspaceContent result = contentCreator.createWorkspaceContentForWorkspace(workspace.getName());
    return result;
  }


  @Override
  public WorkspaceContent createWorkspaceContentFromFile(File file8) {
    return new WorkspaceStatusTools().createWorkspaceContentFromFile(file8);
  }


  @Override
  public WorkspaceContent createWorkspaceContentFromText(Text text) {
    return new WorkspaceStatusTools().createWorkspaceContentFromText(text);
  }


  @Override
  public List<? extends WorkspaceContentDifferences> listOpenWorkspaceDifferencesLists(Workspace arg0, Flag arg1) {
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    return storage.loadDifferencesLists(arg0.getName(), arg1.getValue());
  }


  @Override
  public void resolveWorkspaceDifferences(ListId listId, List<? extends WorkspaceContentDifferencesResolution> list) {
    new ResolveWorkspaceDiffsTools().resolveWorkspaceDifferences(listId, list);
  }


  @Override
  public void closeWorkspaceDifferencesList(ListId listId) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    portal.closeDifferenceList(listId.getListId());
  }


  @Override
  public void updateWorkspaceContent(WorkspaceXmlCreationConfig conf) {
    new CreateWorkspaceXmlTools().execute(conf.getWorkspaceName());
  }


  @Override
  public InfoWorkspaceContentDiffGroupList adaptWorkspaceDifferenceList(ListId listid) {
    return new WorkspaceStatusTools().adaptWorkspaceDifferenceList(listid);
  }


  @Override
  public WorkspaceXmlPath getPathToWorkspaceXml(RepositoryConnection conn) {
    return new WorkspaceStatusTools().getPathToWorkspaceXml(conn);
  }

}
