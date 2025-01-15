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

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ManagedSession;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.DeEncoder;
import xmcp.gitintegration.Flag;
import xprc.xpce.Workspace;
import xmcp.gitintegration.RepositoryManagementServiceOperation;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.repository.BranchData;
import xmcp.gitintegration.repository.Commit;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryUser;
import xmcp.gitintegration.storage.UserManagementStorage;



public class RepositoryManagementServiceOperationImpl implements ExtendedDeploymentTask, RepositoryManagementServiceOperation {

  public void onDeployment() throws XynaException {
    RepositoryManagementImpl.init();
    UserManagementStorage.init();
    OverallInformationProvider.onDeployment();
  }


  public void onUndeployment() throws XynaException {
    RepositoryManagementImpl.shutdown();
    UserManagementStorage.shutdown();
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


  private Pair<String, String> getUserNameAndDecodePassword(String encodedPassword, String sessionId) throws PersistenceLayerException {
    SessionManagement sessionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    ManagedSession session = new ManagedSession(sessionId, null, null);
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ODSConnection con = ods.openConnection();
    try {
      con.queryOneRow(session);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Session not found!");
    } finally {
      con.closeConnection();
    }

    String userName = sessionManagement.resolveSessionToUser(sessionId);
    String password = DeEncoder.decode(encodedPassword, sessionId, session.getToken());
    return new Pair<>(userName, password);
  }


  @Override
  public List<? extends RepositoryUser> listAllRepositoryUsers() {
    return new ArrayList<>(new UserManagementStorage().listAllUsers());
  }


  @Override
  public List<? extends RepositoryUser> listUsersOfRepository(String arg0) {
    return new UserManagementStorage().listUsersOfRepo(arg0);
  }


  @Override
  public RepositoryConnection getRepositoryConnection(Workspace workspace) {
    return RepositoryManagementImpl.getRepositoryConnection(workspace.getName());
  }


  @Override
  public void updateRepositoryConnection(RepositoryConnection repositoryConnection) {
    RepositoryManagementImpl.updatetRepositoryConnection(repositoryConnection);
  }


  @Override
  public void addUserToRepository(XynaOrderServerExtension order, String repo, String encodedPassword, String repoUser, String mail) {
    Pair<String, String> usernamePassword;
    try {
      usernamePassword = getUserNameAndDecodePassword(encodedPassword, order.getSessionId());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    new UserManagementStorage().AddUserToRepository(usernamePassword.getFirst(), repoUser, repo, usernamePassword.getSecond(), mail);

  }


  @Override
  public BranchData listBranches(String arg0) {
    try {
      return new RepositoryInteraction().listBranches(arg0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<? extends Commit> listCommits(String arg0, String arg1, int arg2) {
    try {
      return new RepositoryInteraction().listCommits(arg0, arg1, arg2);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void checkout(String arg0, String arg1) {
    try {
      new RepositoryInteraction().checkout(arg0, arg1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
