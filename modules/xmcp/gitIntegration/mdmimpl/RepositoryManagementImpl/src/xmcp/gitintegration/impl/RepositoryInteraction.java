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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.impl.RemovexmomobjectImpl;
import com.gip.xyna.xmcp.xfcli.impl.SavexmomobjectImpl;

import xmcp.gitintegration.Flag;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceObjectManagement;
import xprc.xpce.Workspace;

public class RepositoryInteraction {
  
  private static Logger logger = CentralFactoryLogging.getLogger(RepositoryInteraction.class);
  
  private DeploymentItemStateManagement dism;
  
  private DeploymentItemStateManagement getDeploymentItemMgmt() {
    if(dism == null) {
      dism =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    }
    return dism; 
  }

  private RevisionManagement revisionManagement;
      
 private RevisionManagement getRevisionMgmt() {
   if(revisionManagement == null) {
     revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
   }
   return revisionManagement;
 }
 
 
 private Repository loadRepo(String repository) throws IOException {
   String repositoryAndPostfix = repository + "/.git";
   validateRepository(repositoryAndPostfix);
   
   List<String> openDifferenceListIds = findOpenDifferenceListIds(repository);
   if (!openDifferenceListIds.isEmpty()) {
     throw new RuntimeException("There are open Differences Lists: " + String.join(", ", openDifferenceListIds));
   } else if(logger.isDebugEnabled()) {
     logger.debug("No differences Lists found for connected workspaces");
   }
   
   Repository repo = new FileRepositoryBuilder().setGitDir(new File(repositoryAndPostfix)).build();
   
   if(logger.isDebugEnabled()) {
     logger.debug("tracking branch: " + new BranchConfig(repo.getConfig(), repo.getBranch()).getTrackingBranch());
   }
   return repo;
 }
 
 public void push(String repository, String message, boolean dryrun) throws Exception {
   Repository repo = loadRepo(repository);
   GitDataContainer container;
   
   try (Git git = new Git(repo)) {
     container = fillGitDataContainer(git, repo, repository);
     if(dryrun) {
       print(container);
       return;
     }
     processConflicts(container);
     if(!container.pull.isEmpty()) {
       throw new RuntimeException("pulls required: " + String.join(", ", container.pull));
     }
     processPushs(git, repo, container, message);
   }
 }
 
 private GitDataContainer fillGitDataContainer(Git git, Repository repo, String path) throws Exception {
   GitDataContainer container = new GitDataContainer();
   container.repository = path;
   fetch(git, repo);
   loadLocalDiffs(git, container);
   loadRemoteDiffs(git, repo, container);
   processLocalDiffs(container);
   processRemoteDiffs(container);
   return container;
 }
 

  public GitDataContainer pull(String repository, boolean dryrun) throws Exception {
    Repository repo = loadRepo(repository);
    GitDataContainer container = null;
    
    try (Git git = new Git(repo)) {
      container = fillGitDataContainer(git, repo, repository);
      if(dryrun) {
        print(container);
        return container;
      }
      processConflicts(container);
      processReverts(git, repo, container);
      processPulls(git, repo, container);
      processExecs(container);
    }
    
    return container;
  }
  
  private void print(GitDataContainer container) {
    if(logger.isDebugEnabled()) {
      logger.debug("  Pull: " + container.pull.size() + ": " + String.join(", ", container.pull));
      logger.debug("  Push: " + container.push.size() + ": " + String.join(", ", container.push));
      logger.debug("  Exec: " + container.exec.size() + ": " + String.join(", ", container.exec.stream().map(x -> x.getFirst() + " - " + x.getSecond()).collect(Collectors.toList())));
      logger.debug("  Conf: " + container.conflicts.size() + ": " + String.join(", ", container.conflicts));
      logger.debug("  revt: " + container.revert.size() + ": " + String.join(", ", container.revert));
    }
  }
  
  private void validateRepository(String repository) {
    if(!RepositoryCache.FileKey.isGitRepository(new File(repository), FS.DETECTED)) {
      throw new RuntimeException("Not a repository: " + repository);
    }
    
    //check repository connection storable
    
    if(logger.isDebugEnabled()) {
      logger.debug("valid repository detected at " + repository);
    }
  }
  
  private void processExecs(GitDataContainer container) {
    List<Pair<Boolean, String>> execs = container.exec; //command, path (in repository)
    
    List<Triple<Boolean, String, String>> exceptions = new ArrayList<>();
    
    for(Pair<Boolean, String> exec : execs) {
      Boolean command = exec.getFirst();
      String repoPath = exec.getSecond();
      Pair<String, String> fqnAndWorkspace = getFqnAndWorkspaceFromRepoPath(repoPath, container.repository);
      String fqn = fqnAndWorkspace.getFirst();
      String workspace = fqnAndWorkspace.getSecond();
    
      try {
        updateXmomRegistration(command, workspace, fqn);
      } catch(XynaException e) {
        exceptions.add(new Triple<>(command, workspace, fqn));
      }
    }
    
    if(!exceptions.isEmpty()) {
      String exceptionText = String.format("\n\t", exceptions.stream().map(this::formatXmomRegistrationException).collect(Collectors.toList()));
      throw new RuntimeException("Exceptions occurred in " + exceptions.size() + " objects: \n\t" + exceptionText);
    }
  }
  
  private HashMap<String, List<? extends RepositoryConnectionStorable>> repoCache = new HashMap<String, List<? extends RepositoryConnectionStorable>>();
  
  private RepositoryConnectionStorable getRepoConnectionStorable(String pathInRepo, String repository) {
    if(logger.isDebugEnabled()) {
      logger.debug("getSubPathAndWorkspace for " + repository + " pathInRepo: " + pathInRepo);
    }
    List<? extends RepositoryConnectionStorable> candidates = repoCache.get(repository);
    if(candidates == null) {
      candidates = RepositoryManagementImpl.loadConnectionsForSingleRepository(repository);
      repoCache.put(repository, candidates);
    }
    
    if(logger.isDebugEnabled()) {
      logger.debug("getSubPathAndWorkspace candidates: " + candidates.size());
    }
    
    Optional<? extends RepositoryConnectionStorable> o = candidates.stream().filter(x -> pathInRepo.startsWith(x.getSubpath())).findAny();
    
    if(logger.isDebugEnabled()) {
      System.out.println("getSubPathAndWorkspace result: " + (o.isEmpty() ? "null" : o.get()));
    }
    
    return o.isEmpty() ? null : o.get();
  }
  
  /**
   * returns null if repoPath does not belong to an XMOM object
   */
  private Pair<String, String> getFqnAndWorkspaceFromRepoPath(String pathInRepo, String repository) {

    RepositoryConnectionStorable storable = getRepoConnectionStorable(pathInRepo, repository);
    
    if(storable == null) {
      if(logger.isDebugEnabled()) {
        logger.debug("Could not find entry for repository " + repository + " matching RepoPath: " + pathInRepo);
      }
      return null;
    }

    String workspace = storable.getWorkspacename();
    String subPath = storable.getSubpath();
    int offset = storable.getSavedinrepo() ? 12 : 5; // 5 => /XMOM/, 12 => /saved/XMOM/
    
    if(pathInRepo.length() < subPath.length() + offset) {
      if(logger.isDebugEnabled()) {
        logger.debug("repoPath length to short: " + pathInRepo + " - " + pathInRepo.length() + " < " + subPath.length() + " [subPath]+ " + offset + " [offset]");
      }
      return null;
    }
    
    if(!isXmom(pathInRepo, subPath, storable.getSavedinrepo())) {
      if(logger.isDebugEnabled()) {
        logger.debug("Not a xmom (not in " + (storable.getSavedinrepo() ? "saved/XMOM" : "XMOM)"));
      }
      return null;
    }
    
    String fqn = pathInRepo.substring(subPath.length() + offset + 1).replace('/', '.'); // +1 for "/"
    fqn = fqn.substring(0, fqn.length()-4); //remove .xml
    if(logger.isDebugEnabled()) {
      logger.debug("fqn derived repoPath: " + fqn + " from " + pathInRepo);
    }
    return new Pair<>(fqn, workspace);
  }

  private boolean isXmom(String pathInRepo, String subPath, boolean savedinrepo) {
    String path = pathInRepo.substring(subPath.length());
    
    if(logger.isDebugEnabled()) {
      logger.debug("pathInRepo: " + pathInRepo + ", subPath: " + subPath + " -- path: " + path);
    }
    
    if(savedinrepo && path.startsWith("/saved/XMOM")) {
      return true;
    } else if(!savedinrepo && path.startsWith("/XMOM")) {
      return true;
    }
    
    return false;
  }



  private String formatXmomRegistrationException(Triple<Boolean, String, String> input) {
    StringBuilder sb = new StringBuilder();
    sb.append("Cound not ").append(input.getFirst() ? "add " : "remove '");
    sb.append(input.getThird()).append("' in workspace '");
    sb.append(input.getSecond()).append("'.");
    return sb.toString();
  }
  
  private void updateXmomRegistration(boolean add, String workspace, String fqn) throws XynaException {
    if (add) {
      SavexmomobjectImpl saveXmom = new SavexmomobjectImpl();
      saveXmom.saveXmomObject(workspace, fqn, false);
    } else {
      RemovexmomobjectImpl removeXmom = new RemovexmomobjectImpl();
      removeXmom.removeXmomObject(workspace, fqn);
    }
  }

  private List<String> findOpenDifferenceListIds(String repository) {
    //TODO: remove additional persistence access
    List<? extends RepositoryConnectionStorable> connections = RepositoryManagementImpl.loadConnectionsForSingleRepository(repository);

    if(logger.isDebugEnabled()) {
      logger.debug("searching for open lists for repository: " + repository + "...");
      logger.debug("found " + connections.size() + " connections...");
    }
    
    List<String> connectedWorkspaces = connections.stream().map(x -> x.getWorkspacename()).collect(Collectors.toList());
    
    if(logger.isDebugEnabled()) {
      logger.debug("found " + connectedWorkspaces.size() + " connected workspaces...");
    }
    
    List<String> openDifferenceListIds = new ArrayList<>();
    for(String connectedWorkspace : connectedWorkspaces) {
      openDifferenceListIds.addAll(listOpenDifferencesLists(connectedWorkspace));
    }
    return openDifferenceListIds;
  }
  
  private void processPulls(Git git, Repository repository, GitDataContainer container) throws Exception {
    if(!container.push.isEmpty()) {
     git.stashCreate().setIncludeUntracked(true).call(); 
    }
    
    git.pull().call();
    
    if(!container.push.isEmpty()) {
      git.stashApply().call();
    }
  }
  
  private void processConflicts(GitDataContainer container) {
    if(!container.conflicts.isEmpty()) {
      throw new RuntimeException("Conflicts found for: " + String.join(", ", container.conflicts));
    }
  }
  
  private void processReverts(Git git, Repository repo, GitDataContainer container) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
    if(container.revert.isEmpty()) {
      return;
    }
    git.checkout().addPaths(container.revert).call();
    if(logger.isDebugEnabled()) {
      logger.debug("Reverted " + container.revert.size() + " files");
    }
  }
  
  private void processRemoteDiffs(GitDataContainer container) {
    for(DiffEntry entry : container.remoteDiffs) {
      Optional<DiffEntry> localCandidate = container.localDiffs.stream().filter(x -> pathMatch(entry, x)).findAny();
      if(!localCandidate.isEmpty()) {
        continue; //=> if there is a match, we processed it already;
      }
      

      String path = entry.getChangeType() == ChangeType.ADD ? entry.getNewPath() : entry.getOldPath();
      if(entry.getChangeType() == ChangeType.ADD || entry.getChangeType() == ChangeType.DELETE) {
        Boolean command = entry.getChangeType() == ChangeType.ADD;
        container.exec.add(new Pair<Boolean, String>(command, path));
      }
      container.pull.add(path);
    }
  }
  
  private void processLocalDiffs(GitDataContainer container) {
    
    if(logger.isDebugEnabled()) {
      logger.debug("Processing " + container.localDiffs.size() + " local diffs");
    }
    
    for(DiffEntry entry : container.localDiffs) {
      Optional<DiffEntry> remoteCandidate = container.remoteDiffs.stream().filter(x -> pathMatch(entry, x)).findAny();
      String repoPath = entry.getChangeType() == ChangeType.DELETE ? entry.getOldPath() : entry.getNewPath();
      if(remoteCandidate.isEmpty()) {
        container.push.add(repoPath);
        if(logger.isDebugEnabled()) {
          logger.debug("push: " + entry);
        }
        continue;
      }
      DiffEntry remoteEntry = remoteCandidate.get();
      
      if(logger.isDebugEnabled()) {
        logger.debug("Match localEntry: " + entry + " with remote entry: " + remoteEntry);
        logger.debug("  " + entry.getNewPath() + " -- old: " + entry.getOldPath());
      }
      
      boolean saved_equals_deployed = false;
      Pair<String, String> fqnAndWorkspace = getFqnAndWorkspaceFromRepoPath(repoPath, container.repository);
      if(fqnAndWorkspace != null) {
        Long revision = getRevision(fqnAndWorkspace.getSecond());
        saved_equals_deployed = calc_savedEqualsDeployed(fqnAndWorkspace.getFirst(), revision);
      }
      switch(entry.getChangeType()) {
        case ADD: 
          processLocalAdded(entry, remoteEntry, saved_equals_deployed, container);
          break;
        case MODIFY: 
          processLocalModified(entry, remoteEntry, saved_equals_deployed, container);
          break;
        case DELETE: 
          processLocalDeleted(entry, remoteEntry, saved_equals_deployed, container);
          break;
        default:
          //remote unchanged
          break;
      }
    }
  }
  
  private Long getRevision(String workspaceName) {
    try {
      return getRevisionMgmt().getRevision(null, null, workspaceName);
    } catch(XynaException e) {
      throw new RuntimeException(e);
    }
  }
  
  private boolean calc_savedEqualsDeployed(String fqName, Long revision) {
    DeploymentItemState deploymentItemState = getDeploymentItemMgmt().get(fqName, revision);
    
    if(deploymentItemState == null) {
      if(logger.isDebugEnabled()) {
        logger.debug("unknown object: " + fqName + " in revision " + revision);
      }
      return false;
    }
    
    DeploymentItemStateReport report = deploymentItemState.getStateReport();
    if(report == null) {
      if(logger.isDebugEnabled()) {
        logger.debug("unknown object: " + fqName + " in revision " + revision);
      }
      return false;
    }
    return DisplayState.DEPLOYED.equals(report.getState());
  }
 
  private void processLocalDeleted(DiffEntry localEntry, DiffEntry remoteEntry, boolean saved_equals_deployed, GitDataContainer container) {
    switch(remoteEntry.getChangeType()) {
      case MODIFY:
        container.conflicts.add(remoteEntry.getNewPath());
        break;
      case DELETE:
        container.pull.add(localEntry.getOldPath());
        break;
        default: 
          throw new RuntimeException("Inconsistency. " + localEntry + " - " + remoteEntry);
    }
  }
  
  private void processLocalModified(DiffEntry localEntry, DiffEntry remoteEntry, boolean saved_equals_deployed, GitDataContainer container) {
    switch(remoteEntry.getChangeType()) {
      case MODIFY:
        if(saved_equals_deployed) {
          container.revert.add(localEntry.getNewPath());
          container.pull.add(localEntry.getNewPath());
        } else {
          if(logger.isDebugEnabled()) {
            logger.debug("Conflict for: " + localEntry.getNewPath() + " - local Modified, remote modified, saved != deployed");
          }
          container.conflicts.add(localEntry.getNewPath());
        }
        break;
      case DELETE: 
        if(logger.isDebugEnabled()) {
          logger.debug("Conflict for: " + localEntry.getNewPath() + " - local Modified, remote deleted");
        }
        container.conflicts.add(localEntry.getNewPath());
        break;
      default:
        throw new RuntimeException("Inconsistency. " + localEntry + " - " + remoteEntry);
    }
  }
  
  private void processLocalAdded(DiffEntry localEntry, DiffEntry remoteEntry, boolean saved_equals_deployed, GitDataContainer container) {
    if(remoteEntry.getChangeType() != ChangeType.ADD) {
      if(logger.isDebugEnabled()) {
       logger.debug("local added, remote " + remoteEntry.getChangeType()); 
      }
      return;
    }
    if(!saved_equals_deployed) {
      if(logger.isDebugEnabled()) {
        logger.debug("Conflict for: " + localEntry.getNewPath() + " - local Added, remote added, saved != deployed");
      }
      container.conflicts.add(localEntry.getNewPath());
      return;
    } else {
      container.revert.add(localEntry.getNewPath());
    }
    
  }
  
  
  private boolean pathMatch(DiffEntry e1, DiffEntry e2) {
    return (e1.getOldPath() != null && e1.getOldPath() != DiffEntry.DEV_NULL &&
        (Objects.equals(e1.getOldPath(), e2.getOldPath())) ||
        (Objects.equals(e1.getOldPath(), e2.getNewPath()))) || 
        
        (e1.getNewPath() != null && e1.getNewPath() != DiffEntry.DEV_NULL &&
        (Objects.equals(e1.getNewPath(), e2.getOldPath())) ||
        (Objects.equals(e1.getNewPath(), e2.getNewPath())));
  }
  
  private void loadLocalDiffs(Git git, GitDataContainer container) throws Exception {
    List<DiffEntry> diff = git.diff().call();
    container.localDiffs.addAll(diff);
    if(logger.isDebugEnabled()) {
      logger.debug("added " + diff.size() + " local diffs.");
    }
  }
  
  private void loadRemoteDiffs(Git git, Repository repository, GitDataContainer container) throws Exception {
    CanonicalTreeParser treeParser = new CanonicalTreeParser();
    ObjectId treeId = repository.resolve( "HEAD^{tree}" );
    
    if(logger.isDebugEnabled()) {
      logger.debug("local treeId: " + treeId.getName());
    }
    
    try( ObjectReader reader = repository.newObjectReader() ) {
      treeParser.reset( reader, treeId );
    }
 
    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
    String revStr = getTrackingBranch(repository) + "^{tree}";
    ObjectId oldTreeId = repository.resolve(revStr);
    if(logger.isDebugEnabled()) {
      logger.debug("remote treeId: " + oldTreeId.getName() + " from " + revStr);
    }
    try( ObjectReader reader = repository.newObjectReader() ) {
      newTreeParser.reset( reader, oldTreeId );
    } 

    List<DiffEntry> diff = git.diff().setCached(false).setOldTree(treeParser).setNewTree(newTreeParser).call();
    container.remoteDiffs = diff;
    
    if(logger.isDebugEnabled()) {
      logger.debug("added " + diff.size() + " remote diffs. " + String.join(", ", diff.stream().map(x -> x.toString()).collect(Collectors.toList())));
    }
  }
  
  private String getTrackingBranch(Repository repository) throws Exception {
    return new BranchConfig(repository.getConfig(), repository.getBranch()).getTrackingBranch();
  }
  

  private void fetch(Git git, Repository repository) throws Exception {
    FetchResult result = git.fetch().call();
    if (logger.isDebugEnabled()) {
      logger.debug("executed fetch. " + String.join(", ", result.getAdvertisedRefs().stream().map(x -> x.getName()).collect(Collectors.toList())));
    }
  }
  
  
  private void processPushs(Git git, Repository repository, GitDataContainer container, String msg) throws Exception {
    git.add().addFilepattern(".").call();
    git.commit().setMessage(msg).call();
    git.push().call();
    if(logger.isDebugEnabled()) {
      logger.debug("executed push.");
    }
  }

  private List<String> listOpenDifferencesLists(String connectedWorkspace) {
    List<? extends WorkspaceContentDifferences> list = WorkspaceObjectManagement.listOpenWorkspaceDifferencesLists(new Workspace(connectedWorkspace), new Flag(false));
    List<String> result = list.stream().map(x -> String.valueOf(x.getListId())).collect(Collectors.toList());
    return result;
  }
  
  public static class GitDataContainer {
    private String repository;
    private List<DiffEntry> localDiffs = new ArrayList<>();
    private List<DiffEntry> remoteDiffs = new ArrayList<>();
    private List<String> conflicts = new ArrayList<>();
    private List<String> revert = new ArrayList<>();
    private List<String> pull = new ArrayList<>();
    private List<String> push = new ArrayList<>();
    private List<Pair<Boolean, String>> exec = new ArrayList<>(); //command => true=add, false=remove, path
  
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      List<String> execString = exec.stream().map(x -> x.getFirst() + " - " + x.getSecond()).collect(Collectors.toList());
      List<String> localDiffString = localDiffs.stream().map(x -> x.toString()).collect(Collectors.toList());
      List<String> remoteDiffString = remoteDiffs.stream().map(x -> x.toString()).collect(Collectors.toList());
      sb.append("Data for repository: ").append(repository).append("\n");
      sb.append("  Ldif: ").append(localDiffs.size()).append(": ").append(String.join(", ", localDiffString)).append("\n");
      sb.append("  Rdif: ").append(remoteDiffs.size()).append(": ").append(String.join(", ", remoteDiffString)).append("\n");
      sb.append("  Pull: ").append(pull.size()).append(": ").append(String.join(", ", pull)).append("\n");
      sb.append("  Push: ").append(push.size()).append(": ").append(String.join(", ", push)).append("\n");
      sb.append("  Exec: ").append(exec.size()).append(": ").append(String.join(", ", execString)).append("\n");
      sb.append("  Conf: ").append(conflicts.size()).append(": ").append(String.join(", ", conflicts)).append("\n");
      sb.append("  revt: ").append(revert.size()).append(": ").append(String.join(", ", revert)).append("\n");
      return sb.toString();
    }
  }
}
