/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.gitintegration.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.Repository;

import xmcp.gitintegration.impl.RepositoryManagementImpl;
import xmcp.gitintegration.repository.ChangeSet;
import xmcp.gitintegration.repository.IndexedWorkspaceFileChange;
import xmcp.gitintegration.repository.RepositoryConnection;
import xmcp.gitintegration.repository.RepositoryConnectionGroup;
import xmcp.gitintegration.repository.WorkspaceFileChangeList;


public class LoadChangesTools {

  public ChangeSet loadChanges(String repoPath, Repository repo) throws Exception {
    List<? extends RepositoryConnectionGroup> grouplist = RepositoryManagementImpl.listRepositoryConnectionGroups();
    RepositoryConnectionGroup group = selectGroup(grouplist, repoPath);
    ChangeSet ret = prepareChangeSet(group);
    try (Git git = new Git(repo)) {
      StatusCommand cmd = git.status();
      Status status = cmd.call();
      List<ChangedStatusInfo> changelist = new ArrayList<>();
      for (String str : status.getChanged()) {
        changelist.add(new ChangedStatusInfo(str, ChangeType.CHANGED));
      }
      for (String str : status.getModified()) {
        changelist.add(new ChangedStatusInfo(str, ChangeType.MODIFIED));
      }
      for (String str : status.getMissing()) {
        changelist.add(new ChangedStatusInfo(str, ChangeType.DELETED));
      }
      for (String str : status.getUntracked()) {
        changelist.add(new ChangedStatusInfo(str, ChangeType.NEW));
      }
      Collections.sort(changelist);
      for (ChangedStatusInfo info : changelist) {
        handleFileChange(info.getChangedPath(), info.getChangeType().getChangeTypeString(), ret);
      }
    }
    return ret;
  }
  
  
  private ChangeSet prepareChangeSet(RepositoryConnectionGroup group) {
    ChangeSet ret = new ChangeSet();
    int i = 0;
    for (RepositoryConnection conn : group.getRepositoryConnection()) {
      WorkspaceFileChangeList wfcl = new WorkspaceFileChangeList();
      wfcl.setWorkspacePath(conn.getSubpath());
      wfcl.setWorkspaceName(conn.getWorkspaceName());
      wfcl.setWorkspaceIndex(i);
      ret.addToChanges(wfcl);
      i++;
    }
    return ret;
  }
  
  private RepositoryConnectionGroup selectGroup(List<? extends RepositoryConnectionGroup> grouplist, String repository) {
    for (RepositoryConnectionGroup group : grouplist) {
      String path = group.getRepository().getPath();
      if (repository.equals(path)) {
        return group;
      }
    }
    throw new RuntimeException("Could not find data for repository " + repository);
  }
  

  private void handleFileChange(String path, String typestr, ChangeSet cs) {
    IndexedWorkspaceFileChange change = new IndexedWorkspaceFileChange();
    change.setFileFullPath(path);
    change.setType(typestr);
    for (WorkspaceFileChangeList wfcl : cs.getChanges()) {
      String wspath = wfcl.getWorkspacePath();
      if (path.startsWith(wspath) && (path.length() > wspath.length())) {
        String subpath = path.substring(wspath.length() + 1);
        change.setFileSubpath(subpath);
        if (wfcl.getIndexedFileChangeList() == null) {
          change.setIndex(0);
        }
        else {
          change.setIndex(wfcl.getIndexedFileChangeList().size());
        }
        wfcl.addToIndexedFileChangeList(change);
      }
    }
  }

}
