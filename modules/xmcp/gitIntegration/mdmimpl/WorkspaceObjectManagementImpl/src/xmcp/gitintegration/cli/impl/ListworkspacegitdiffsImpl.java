/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xmcp.gitintegration.cli.impl;



import java.io.OutputStream;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.cli.generated.Listworkspacegitdiffs;
import xmcp.gitintegration.impl.OutputCreator;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;



public class ListworkspacegitdiffsImpl extends XynaCommandImplementation<Listworkspacegitdiffs> {

  public void execute(OutputStream statusOutputStream, Listworkspacegitdiffs payload) throws XynaException {
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    StringBuilder sb = new StringBuilder();
    if (payload.getId() != null && !payload.getId().isEmpty()) {
      //specific list
      long id;
      try {
        id = Long.parseLong(payload.getId());
      } catch (NumberFormatException e) {
        writeToCommandLine(statusOutputStream, "Not a long: " + payload.getId());
        return;
      }
      WorkspaceContentDifferences entry = storage.loadDifferences(id);
      if (entry == null || entry.getListId() != id) {
        writeToCommandLine(statusOutputStream,
                           "workspace content differences list not found. Use listworkspacediffs without an id to list all open workspace content differences lists.");
        return;
      }
      sb.append("Showing information for list with id ");
      sb.append(id);
      sb.append(". Entries: ");
      sb.append(entry.getDifferences().size());
      sb.append("\n");
      writeToCommandLine(statusOutputStream, sb.toString());
      List<? extends WorkspaceContentDifference> differences = entry.getDifferences();
      differences.sort((x, y) -> (int) (x.getId() - y.getId()));
      OutputCreator creator = new OutputCreator();
      String output = creator.createOutput(differences);
      writeToCommandLine(statusOutputStream, output);
    } else {
      //all lists
      List<? extends WorkspaceContentDifferences> info = storage.loadAllDifferencesLists();
      info.sort((x, y) -> (int) (x.getListId() - y.getListId()));

      for (WorkspaceContentDifferences entry : info) {
        sb.append("Id: ");
        sb.append(entry.getListId());
        sb.append(" workspace '");
        sb.append(entry.getWorkspaceName());
        sb.append("' Entries: ");
        sb.append(entry.getDifferences().size());
        sb.append("\n");
      }
      writeToCommandLine(statusOutputStream, sb.toString());
    }
  }

}
