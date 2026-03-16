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



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;

import xmcp.gitintegration.repository.RepositoryConnection;
import xprc.xpce.Workspace;



public class UnconnectedWorkspaceLister {

  public List<Workspace> listUnconnectedWorkspaces() {
    List<Workspace> result = new ArrayList<>();

    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace> allWorkspaces = rm.getWorkspaces();
    List<RepositoryConnection> connections = RepositoryManagementImpl.listRepositoryConnections();

    for (Entry<Long, com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace> entry : allWorkspaces.entrySet()) {
      if (entry.getKey() == -1l) {
        continue; // skip default workspace
      }
      String workspaceName = entry.getValue().getName();
      boolean isConnected = connections.stream().anyMatch(c -> Objects.equals(c.getWorkspaceName(), workspaceName));
      if (isConnected) {
        continue;
      }
      result.add(new Workspace.Builder().name(workspaceName).instance());
    }

    return result;
  }
}
