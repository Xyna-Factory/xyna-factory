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

import base.Text;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.gitintegration.Flag;
import xprc.xpce.Workspace;
import xmcp.gitintegration.RepositoryManagementServiceOperation;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.repository.BranchData;
import xmcp.gitintegration.repository.Commit;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryUser;


public class RepositoryManagementServiceOperationImpl implements ExtendedDeploymentTask, RepositoryManagementServiceOperation {

  public void onDeployment() throws XynaException {
    RepositoryManagementImpl.init();
    OverallInformationProvider.onDeployment();
  }

  public void onUndeployment() throws XynaException {
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
    return null;
  }

  public Text addRepositoryConnection(Text path, Workspace workspace, Flag full) {
    return new Text(RepositoryManagementImpl.addRepositoryConnection(path.getText(), workspace.getName(), full.getValue()));
  }

  public Text listRepositoryConnections() {
    return new Text(RepositoryManagementImpl.listRepositoryConnections());
  }

  public Text removeRepositoryConnection(Workspace workspace, Flag full, Flag delete) {
    return new Text(RepositoryManagementImpl.removeRepositoryConnection(workspace.getName(), full.getValue(), delete.getValue()));
  }

  @Override
  public void addUserToRepository(XynaOrderServerExtension arg0, String arg1, String arg2, String arg3, String arg4) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public List<? extends RepositoryUser> listAllRepositoryUsers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BranchData listBranches(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<? extends Commit> listCommits(String arg0, String arg1, int arg2) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<? extends RepositoryUser> listUsersOfRepository(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RepositoryConnection getRepositoryConnection(Workspace workspace) {
     return RepositoryManagementImpl.getRepositoryConnection(workspace.getName());
  }
  
  @Override
  public void updateRepositoryConnection(RepositoryConnection repositoryConnection) {
    RepositoryManagementImpl.updatetRepositoryConnection(repositoryConnection);
  }

}
