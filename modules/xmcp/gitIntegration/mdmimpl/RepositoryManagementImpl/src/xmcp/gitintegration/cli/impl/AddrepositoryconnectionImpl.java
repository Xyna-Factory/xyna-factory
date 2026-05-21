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
package xmcp.gitintegration.cli.impl;



import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Addrepositoryconnection;
import xmcp.gitintegration.impl.RepositoryManagementImpl;
import xmcp.gitintegration.impl.RepositoryManagementImpl.AddRepositoryConnectionResult;
import xmcp.gitintegration.impl.RepositoryManagementImpl.AddRepositoryConnectionResult.Success;



public class AddrepositoryconnectionImpl extends XynaCommandImplementation<Addrepositoryconnection> {

  public void execute(OutputStream statusOutputStream, Addrepositoryconnection payload) throws XynaException {
    AddRepositoryConnectionResult result = RepositoryManagementImpl.addRepositoryConnection(payload.getPath(), payload.getWorkspace(), payload.getFull(), payload.getSetup());
    if(result.getActionsPerformed().isEmpty()) {
      writeToCommandLine(statusOutputStream, "No actions were performed.\n");
    } else {
      writeToCommandLine(statusOutputStream, "Actions performed:\n\t");
      writeToCommandLine(statusOutputStream, String.join("\n\t", result.getActionsPerformed()));
      writeToCommandLine(statusOutputStream, "\n");
    }
    if(result.getErrorMsg() != null && !result.getErrorMsg().isEmpty()) {
      writeToCommandLine(statusOutputStream, String.format("Error: %s", result.getErrorMsg()));
    }
    if(result.getSuccess() == Success.FULL) {
      writeToCommandLine(statusOutputStream, "Successfully added repository connection" + (payload.getFull() ? "s":""));
    }
    writeEndToCommandLine(statusOutputStream, result.getSuccess() == Success.FULL ? ReturnCode.SUCCESS : ReturnCode.GENERAL_ERROR);
  }

}
