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
import java.util.Optional;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Resolveworkspacexml;
import xmcp.gitintegration.impl.ResolveWorkspaceDifferencesParameter;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;



public class ResolveworkspacexmlImpl extends XynaCommandImplementation<Resolveworkspacexml> {

  public void execute(OutputStream statusOutputStream, Resolveworkspacexml payload) throws XynaException {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
    ResolveWorkspaceDifferencesParameter param = new ResolveWorkspaceDifferencesParameter();
    param.setWorkspaceDifferenceListId(Long.valueOf(payload.getId()));
    param.setEntry(payload.getEntry() == null ? Optional.empty() : Optional.of(Long.valueOf(payload.getEntry())));
    param.setResolution(payload.getResolution() == null ? Optional.empty() : Optional.of(payload.getResolution()));
    param.setAll(payload.getAll());
    param.setClose(param.getClose());
    String result = portal.resolve(param);
    writeToCommandLine(statusOutputStream, result);
  }

}
