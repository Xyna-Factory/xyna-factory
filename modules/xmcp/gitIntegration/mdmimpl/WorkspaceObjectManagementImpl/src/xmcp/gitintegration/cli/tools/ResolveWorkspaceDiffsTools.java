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

package xmcp.gitintegration.cli.tools;

import java.util.ArrayList;
import java.util.List;

import xmcp.gitintegration.ListId;
import xmcp.gitintegration.WorkspaceContentDifferencesResolution;
import xmcp.gitintegration.impl.ResolveWorkspaceDifferencesParameter;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;


public class ResolveWorkspaceDiffsTools {
    
  public void resolveWorkspaceDifferences(ListId listId, List<? extends WorkspaceContentDifferencesResolution> inputlist) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    List<ResolveWorkspaceDifferencesParameter> paramlist = new ArrayList<>();
      
    for (WorkspaceContentDifferencesResolution res : inputlist) {
      ResolveWorkspaceDifferencesParameter param = new ResolveWorkspaceDifferencesParameter();
      param.setResolution(res.getResolution());
      param.setEntry(res.getEntryId());
      paramlist.add(param);
    }    
    portal.resolveList(listId.getListId(), paramlist);    
  }

}
