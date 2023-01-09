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



import java.io.File;
import java.io.OutputStream;
import java.util.List;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.cli.generated.Compareworkspacexml;
import xmcp.gitintegration.impl.OutputCreator;
import xmcp.gitintegration.impl.WorkspaceContentComparator;
import xmcp.gitintegration.impl.WorkspaceContentCreator;



public class CompareworkspacexmlImpl extends XynaCommandImplementation<Compareworkspacexml> {


  public void execute(OutputStream statusOutputStream, Compareworkspacexml payload) throws XynaException {
    String workspaceName = payload.getWorkspaceName();
    WorkspaceContentCreator creator = new WorkspaceContentCreator();
    File file = creator.determineWorkspaceXMLFile(workspaceName);
    WorkspaceContent xmlConfig = creator.createWorkspaceContentFromFile(file);
    WorkspaceContent factoryConfig = creator.createWorkspaceContent(workspaceName);
    WorkspaceContentComparator comparator = new WorkspaceContentComparator();
    WorkspaceContentDifferences differences = comparator.compareWorkspaceContent(factoryConfig, xmlConfig, true);
    List<? extends WorkspaceContentDifference> diffs = differences.getDifferences();
    if (!diffs.isEmpty()) {
      writeToCommandLine(statusOutputStream, "List has been saved. Id: " + differences.getListId() + ".\n");
      writeToCommandLine(statusOutputStream, "Use resolveworkspacexml to update factory\n");
      writeToCommandLine(statusOutputStream, "Use listworkspacediffs to show open lists\n");
    }
    String differenceString = diffs.size() == 1 ? "is one difference " : "are " + diffs.size() + " differences ";
    writeToCommandLine(statusOutputStream, "There " + differenceString + " between workspace.xml and factory state.\n");
    OutputCreator outputCreator = new OutputCreator();
    String output = outputCreator.createOutput(diffs);
    writeToCommandLine(statusOutputStream, output);
  }

}
