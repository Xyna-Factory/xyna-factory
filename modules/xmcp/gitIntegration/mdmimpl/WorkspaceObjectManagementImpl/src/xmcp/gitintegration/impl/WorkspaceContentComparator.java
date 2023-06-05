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
package xmcp.gitintegration.impl;



import java.util.List;

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;



public class WorkspaceContentComparator {

  public WorkspaceContentDifferences compareWorkspaceContent(WorkspaceContent c1, WorkspaceContent c2, boolean persist) {
    WorkspaceContentDifferences result = new WorkspaceContentDifferences();
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    List<WorkspaceContentDifference> itemDifferences = portal.compare(c1, c2);

    //expand here: compare workspace names
    if (c1.getWorkspaceName() == null || c2.getWorkspaceName() == null || !c1.getWorkspaceName().equals(c2.getWorkspaceName())) {
      //add warning: names do not match
    }

    long id = -1l;
    
    result.setDifferences(itemDifferences);
    result.setWorkspaceName(c2.getWorkspaceName());
    result.setListId(id);
    
    if (persist && !itemDifferences.isEmpty()) {
      WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
      storage.persist(result);
    }
    
    return result;
  }
}
