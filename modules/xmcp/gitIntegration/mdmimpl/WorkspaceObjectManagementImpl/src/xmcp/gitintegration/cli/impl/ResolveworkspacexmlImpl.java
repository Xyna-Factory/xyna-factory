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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Resolveworkspacexml;
import xmcp.gitintegration.impl.ResolveWorkspaceDifferencesParameter;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;


public class ResolveworkspacexmlImpl extends XynaCommandImplementation<Resolveworkspacexml> {

  public void execute(OutputStream statusOutputStream, Resolveworkspacexml payload) throws XynaException {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    long listid = Long.valueOf(payload.getId());
    String result = "";
    if (payload.getClose()) {
      result = portal.closeDifferenceList(listid);
    }
    else if ((payload.getEntry() != null) && !payload.getAll()) {
      result = portal.resolveAll(listid, Optional.ofNullable(payload.getResolution()));
    }
    else {
      ResolveWorkspaceDifferencesParameter param = new ResolveWorkspaceDifferencesParameter();
      param.setEntry(Long.valueOf(payload.getEntry()));
      param.setResolution(payload.getResolution());
      List<ResolveWorkspaceDifferencesParameter> list = new ArrayList<>();
      list.add(param);      
      result = portal.resolveList(listid, list);
    }
    writeToCommandLine(statusOutputStream, result);
  }

}
