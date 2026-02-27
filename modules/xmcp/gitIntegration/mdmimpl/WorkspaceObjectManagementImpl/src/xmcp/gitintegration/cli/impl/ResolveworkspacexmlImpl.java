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
package xmcp.gitintegration.cli.impl;



import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.ResolveWorkspaceContentDifferencesResult;
import xmcp.gitintegration.cli.generated.Resolveworkspacexml;
import xmcp.gitintegration.impl.ResolveWorkspaceDifferencesParameter;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal.ResolveResult;



public class ResolveworkspacexmlImpl extends XynaCommandImplementation<Resolveworkspacexml> {

  public void execute(OutputStream statusOutputStream, Resolveworkspacexml payload) throws XynaException {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    long listid = Long.valueOf(payload.getId());

    if (payload.getClose()) {
      writeToCommandLine(statusOutputStream, portal.closeDifferenceList(listid));
      return;
    }

    if (payload.getEntry() == null && !payload.getAll()) {
      writeToCommandLine(statusOutputStream, "Either entry or all must be specified.");
      writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
      return;
    }

    ResolveResult resolveResult;
    if (payload.getAll()) {
      resolveResult = portal.resolveAll(listid, Optional.ofNullable(payload.getResolution()));
    } else {
      ResolveWorkspaceDifferencesParameter param = new ResolveWorkspaceDifferencesParameter();
      param.setEntry(Long.valueOf(payload.getEntry()));
      param.setResolution(payload.getResolution());
      resolveResult = portal.resolveList(listid, List.of(param));
    }
    StringBuilder sb = new StringBuilder();
    for (ResolveWorkspaceContentDifferencesResult item : resolveResult.getResults()) {
      String successString = item.getSuccess() ? "" : "not";
      sb.append(String.format("Entry %d was %sresolved successfully %s\n", item.getEntryId(), successString, item.getMessage()));
    }
    if (resolveResult.isClosedList()) {
      sb.append("The list was closed because all entries were resolved.");
    } else {
      int remainingEntries = resolveResult.getRemainingEntries();
      String entryString = remainingEntries == 1 ? "is one entry" : "are " + remainingEntries + " entries";
      sb.append(String.format("There %s remaining in the list.", entryString));
    }
    writeToCommandLine(statusOutputStream, sb.toString());
  }

}
