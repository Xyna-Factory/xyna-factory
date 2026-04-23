/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ManagedSession;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
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
import xmcp.gitintegration.impl.RepositoryManagementImpl.AddRepositoryConnectionResult;
import xmcp.gitintegration.impl.tracking.CollectingTracker;
import xmcp.gitintegration.repository.Branch;
import xmcp.gitintegration.repository.BranchData;
import xmcp.gitintegration.repository.ChangeSet;
import xmcp.gitintegration.repository.Commit;
import xmcp.gitintegration.repository.PullInput;
import xmcp.gitintegration.repository.PullOutput;
import xmcp.gitintegration.repository.Repository;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryConnectionGroup;
import xmcp.gitintegration.repository.RepositoryStatus;
import xmcp.gitintegration.repository.RepositoryUser;
import xmcp.gitintegration.repository.RepositoryUserCreationData;
import xmcp.gitintegration.storage.UserManagementStorage;
import xmcp.gitintegration.ui.IndexedRepository;



public class RepositoryManagementServiceOperationImpl implements ExtendedDeploymentTask, RepositoryManagementServiceOperation {

  private static Logger logger = CentralFactoryLogging.getLogger(RepositoryManagementServiceOperationImpl.class);

  public static final XynaPropertyString DEFAULT_REPO_LOCATION =
      new XynaPropertyString("xmcp.gitintegration.default_repository_path", "../git")
          .setDefaultDocumentation(DocumentationLanguage.EN, "Default location for git repositories.")
          .setDefaultDocumentation(DocumentationLanguage.DE, "Standardverzeichnis für git repositories.");


  public void onDeployment() throws XynaException {
    RepositoryManagementImpl.init();
    UserManagementStorage.init();
    OverallInformationProvider.onDeployment();
    PluginManagement.registerPlugin(this.getClass());
    DEFAULT_REPO_LOCATION.registerDependency(UserType.Service, "GitIntegation");
  }


  public void onUndeployment() throws XynaException {
    RepositoryManagementImpl.shutdown();
    UserManagementStorage.shutdown();
    OverallInformationProvider.onUndeployment();
    PluginManagement.unregisterPlugin(this.getClass());
    DEFAULT_REPO_LOCATION.unregister();
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


  public Text addRepositoryConnection(Text path, Workspace workspace, Flag full, Flag setup) {
    AddRepositoryConnectionResult result;
    result = RepositoryManagementImpl.addRepositoryConnection(path.getText(), workspace.getName(), full.getValue(), setup.getValue());
    return new Text(result.getSuccess() + result.getErrorMsg());
  }


  public List<? extends RepositoryConnection> listRepositoryConnections() {
    return RepositoryManagementImpl.listRepositoryConnections();
  }


  @Override
  public List<? extends RepositoryConnectionGroup> listRepositoryConnectionGroups() {
    return RepositoryManagementImpl.listRepositoryConnectionGroups();
  }


  public Text removeRepositoryConnection(Workspace workspace, Flag full, Flag delete) {
    return new Text(RepositoryManagementImpl.removeRepositoryConnection(workspace.getName(), full.getValue(), delete.getValue()));
  }


  private String getToken(String sessionId) throws PersistenceLayerException {
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

    return session.getToken();
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
    SessionManagement sessionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    String repo = data.getRepository().getPath();
    String encodedPassword = data.getEncodedPassword();
    String encodedKey = data.getEncodedKey();
    String encodedKeyPhrase = data.getEncodedKeyPassphrase();
    String repoUser = data.getUsername();
    String mail = data.getMail();
    String sessionId = order.getSessionId();
    String username = sessionManagement.resolveSessionToUser(sessionId);
    String password = null;
    String key = null;
    String keyPhrase = null;
    try {
      String token = getToken(order.getSessionId());
      password = DeEncoder.decode(encodedPassword, sessionId, token);
      key = DeEncoder.decode(encodedKey, sessionId, token);
      keyPhrase = DeEncoder.decode(encodedKeyPhrase, sessionId, token);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    new UserManagementStorage().AddUserToRepository(username, repoUser, repo, password, key, keyPhrase, mail);
  }


  @Override
  public RepositoryUser getCurrentRepositoryUser(XynaOrderServerExtension order, Repository repository) {
    String user = getUserFromSession(order.getSessionId());
    try {
      return new UserManagementStorage().loadUser(user, repository.getPath());
    } catch (Exception e) {
      return new RepositoryUser();
    }
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
  public PullOutput pull(XynaOrderServerExtension order, PullInput input) {
    String user = getUserFromSession(order.getSessionId());
    PullOutput result;
    RepositoryInteraction interaction = new RepositoryInteraction();
    try {
      String repoPath = input.getRepository().getPath();
      boolean dryRun = input.getDryrun();
      result = interaction.pull(repoPath, dryRun, user);
    } catch (Exception e) {
      PullOutput.Builder builder = new PullOutput.Builder();
      return builder.exception(e.getMessage()).repository(input.getRepository().getPath()).dryrun(input.getDryrun()).instance();
    }

    return result;
  }


  @Override
  public Text push(XynaOrderServerExtension order, Repository arg0, Text arg1, List<? extends File> arg2) {
    String user = getUserFromSession(order.getSessionId());
    try {
      List<String> adapted = (arg2 == null) ? new ArrayList<String>() :
                             arg2.stream().filter(Objects::nonNull).map(x -> x.getPath()).collect(Collectors.toList());
      new RepositoryInteraction().push(arg0.getPath(), arg1.getText(), false, user, adapted);
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
      return new Text("Exception during push: " + e.getMessage());
    }

    return new Text("Push successful!");
  }


  @Override
  public ChangeSet loadChangeSet(Repository repository) {
    if (repository == null) { throw new IllegalArgumentException("Parameter repository is empty."); }
    try {
      return new RepositoryInteraction().loadChanges(repository.getPath());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<? extends Text> getFileContentInCurrentOriginBranch(Repository repository, File file) {
    try {
      return new RepositoryInteraction().getFileContentInCurrentOriginBranch(repository.getPath(), file.getPath());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public RepositoryStatus status(Repository repository) {
    try {
      return new RepositoryInteraction().getStatus(repository.getPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<? extends Workspace> listUnconnectedWorkspaces() {
    return new UnconnectedWorkspaceLister().listUnconnectedWorkspaces();
  }


  @Override
  public void addLocalWorkspaceToRepository(XynaOrderServerExtension order, RepositoryConnection repositoryConnection) {
    CollectingTracker eventTracker = new CollectingTracker();
    RepositoryManagementImpl.addLocalWorkspaceToRepository(repositoryConnection, eventTracker);

    String creator = order.getSessionId();
    List<SerializablePair<String, String>> payload = new ArrayList<>();
    String msg;
    if (eventTracker.getErrorMessages().isEmpty()) {
      msg = "Could not add " + repositoryConnection.getWorkspaceName() + " to repository";
    } else {
      msg = "Successfully added " + repositoryConnection.getWorkspaceName() + " to repository";
    }
    payload.add(new SerializablePair<>("Message", msg));

    String correlation = "AddLocalWorkspaceToRepository " + repositoryConnection.getWorkspaceName();
    MessageInputParameter mip = new MessageInputParameter("GuiHttp", "Notification", correlation, creator, payload, false);
    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(mip);
    } catch (XynaException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not publish addLocalWorkspaceToRepository event.", e);
      }
    }

    if (logger.isDebugEnabled()) {
      String actions = String.join("\n\t", eventTracker.getInfoMessages());
      logger.debug("Actions taken to add workspace " + repositoryConnection.getWorkspaceName() + " to repository: " + actions);
      if (!eventTracker.getErrorMessages().isEmpty()) {
        String errors = String.join("\n\t", eventTracker.getErrorMessages());
        logger.debug("Errors adding workspace to repository: " + errors);
      }
    }
  }


  @Override
  public List<? extends RepositoryConnection> listConnectionsOfRepository(Repository repository) {
    List<? extends RepositoryConnectionStorable> data = RepositoryManagementImpl.loadConnectionsForSingleRepository(repository.getPath());
    List<RepositoryConnection> result = new ArrayList<>();
    for (RepositoryConnectionStorable instance : data) {
      result.add(RepositoryManagementImpl.convert(instance));
    }
    return result;
  }


  @Override
  public RepositoryConnection normalizePath(RepositoryConnection connection) {
    RepositoryConnection result = connection.clone();
    Path absoluteDefault = Path.of(DEFAULT_REPO_LOCATION.get()).toAbsolutePath();
    Path repoPath = Path.of(result.getPath());
    Path repoDir = repoPath.getParent();
    try {
      if (Files.isSameFile(repoDir, absoluteDefault)) {
        result.unversionedSetPath(repoPath.getFileName().toString());
      }
    } catch (IOException e) {
      //ignore exception and keep old path
    }
    return result;
  }


  @Override
  public List<? extends IndexedRepository> listKnownRepositories() {
    List<IndexedRepository> result = new ArrayList<>();

    List<? extends RepositoryConnection> existingConnections = listRepositoryConnections();
    Set<String> repositoryNames = new HashSet<>();
    for (RepositoryConnection connection : existingConnections) {
      repositoryNames.add(connection.getPath());
    }

    Path absoluteDefault = Path.of(DEFAULT_REPO_LOCATION.get()).toAbsolutePath();
    List<String> repositoriesInDefaulLocation = RepositoryManagementImpl.listRepositories(absoluteDefault);
    for (String repo : repositoriesInDefaulLocation) {
      repositoryNames.add(repo);
    }
    List<String> ordered = repositoryNames.stream().sorted().collect(Collectors.toList());
    int index = 0;
    for (String repoPath : ordered) {
      result.add(new IndexedRepository.Builder().index(index++).repository(repoPath).instance());
    }
    return result;
  }
}
