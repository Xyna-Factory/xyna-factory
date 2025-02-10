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
import base.math.IntegerNumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import xmcp.gitintegration.impl.RepositoryInteraction.GitDataContainer;
import xmcp.gitintegration.repository.Branch;
import xmcp.gitintegration.repository.BranchData;
import xmcp.gitintegration.repository.ChangeSet;
import xmcp.gitintegration.repository.Commit;
import xmcp.gitintegration.repository.Repository;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryConnectionGroup;
import xmcp.gitintegration.repository.RepositoryUser;
import xmcp.gitintegration.repository.RepositoryUserCreationData;
import xmcp.gitintegration.storage.UserManagementStorage;



public class RepositoryManagementServiceOperationImpl implements ExtendedDeploymentTask, RepositoryManagementServiceOperation {

  public void onDeployment() throws XynaException {
    RepositoryManagementImpl.init();
    UserManagementStorage.init();
    OverallInformationProvider.onDeployment();
    PluginManagement.registerPlugin(this.getClass());
  }


  public void onUndeployment() throws XynaException {
    RepositoryManagementImpl.shutdown();
    UserManagementStorage.shutdown();
    OverallInformationProvider.onUndeployment();
    PluginManagement.unregisterPlugin(this.getClass());
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


  public List<? extends RepositoryConnection> listRepositoryConnections() {
    return RepositoryManagementImpl.listRepositoryConnections();
  }


  @Override
  public List<? extends RepositoryConnectionGroup> listRepositoryConnectionGroups() {
    List<RepositoryConnection> connections = RepositoryManagementImpl.listRepositoryConnections();
    List<RepositoryConnectionGroup> result = new ArrayList<>();
    Map<String, List<RepositoryConnection>> groups = new HashMap<>();
    for(RepositoryConnection connection: connections) {
      groups.putIfAbsent(connection.getPath(), new ArrayList<>());
      groups.get(connection.getPath()).add(connection);
    }
    for(String repoGroup : groups.keySet()) {
      Repository repo = new Repository.Builder().path(repoGroup).instance();
      List<RepositoryConnection> conns = groups.get(repoGroup);
      RepositoryConnectionGroup group = new RepositoryConnectionGroup.Builder().repository(repo).repositoryConnection(conns).instance();
      result.add(group);
    }
    return result;
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
  public List<? extends RepositoryUser> listUsersOfRepository(Repository repository) {
    return new UserManagementStorage().listUsersOfRepo(repository.getPath());
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
  public void addUserToRepository(XynaOrderServerExtension order, RepositoryUserCreationData data) {
    String repo = data.getRepository().getPath();
    String encodedPassword = data.getEncodedPassword();
    String repoUser = data.getUsername();
    String mail = data.getMail();
    Pair<String, String> usernamePassword;
    try {
      usernamePassword = getUserNameAndDecodePassword(encodedPassword, order.getSessionId());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    new UserManagementStorage().AddUserToRepository(usernamePassword.getFirst(), repoUser, repo, usernamePassword.getSecond(), mail);
  }

  

  @Override
  public BranchData listBranches(Repository repository) {
    try {
      return new RepositoryInteraction().listBranches(repository.getPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<? extends Commit> listCommits(Repository repo, Branch branch, IntegerNumber count) {
    try {
      return new RepositoryInteraction().listCommits(repo.getPath(), branch.getName(), (int)count.getValue());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void checkout(Branch branch, Repository repository) {
    try {
      new RepositoryInteraction().checkout(branch.getName(), repository.getPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  
  private String getUserFromSession(String session) {
    SessionManagement sessionMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    return sessionMgmt.resolveSessionToUser(session);
  }


  @Override
  public Text pull(XynaOrderServerExtension order, Repository repository) {
    String user = getUserFromSession(order.getSessionId());
    String result;
    try {
      GitDataContainer data = new RepositoryInteraction().pull(repository.getPath(), false, user);
      result = data.toString();
    } catch (Exception e) {
      return new Text("Exception during pull: " + e.getMessage());
    }

    return new Text(result);
  }


  @Override
  public Text push(XynaOrderServerExtension order, Repository arg0, Text arg1, List<? extends File> arg2) {
    String user = getUserFromSession(order.getSessionId());
    try {
      new RepositoryInteraction().push(arg0.getPath(), arg1.getText(), false, user);
    } catch (Exception e) {
      return new Text("Exception during push: " + e.getMessage());
    }

    return new Text("Push successful!");
  }


  @Override
  public ChangeSet loadChangeSet(Repository repository) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
