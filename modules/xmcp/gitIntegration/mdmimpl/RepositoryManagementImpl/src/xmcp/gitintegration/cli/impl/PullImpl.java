/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Pull;
import xmcp.gitintegration.impl.RepositoryInteraction;
import xmcp.gitintegration.repository.PullOutput;
import xmcp.gitintegration.storage.UserManagementStorage;



public class PullImpl extends XynaCommandImplementation<Pull> {

  public void execute(OutputStream statusOutputStream, Pull payload) throws XynaException {
    RepositoryInteraction repoInteraction = new RepositoryInteraction();
    try {
      PullOutput result = repoInteraction.pull(payload.getRepository(), payload.getDryrun(), UserManagementStorage.CLI_USERNAME);
      writeToCommandLine(statusOutputStream, pullOutputToString(result));
      if (result.getException() != null && !result.getException().isEmpty()) {
        writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
      } else if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
        writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS_WITH_PROBLEM);
      } else {
        writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private String pullOutputToString(PullOutput output) {
    StringBuilder sb = new StringBuilder();
    List<String> execString = output.getExecutions();
    List<String> localDiffString = output.getLocalChanges();
    List<String> remoteDiffString = output.getRemoteChanges();
    List<String> diffListIds = output.getOpenedWorkspaceDiffLists();
    sb.append("Data for repository: ").append(output.getRepository()).append("\n");
    if(output.getException() != null) {
      sb.append("An exception ocurred: ").append(output.getException()).append("\n");
    }
    appendField(sb, "Ldif", localDiffString);
    appendField(sb, "Rdif", remoteDiffString);
    appendField(sb, "Exec", execString);
    appendField(sb, "Conf", output.getConflicts());
    appendField(sb, "Revt", output.getReverts());
    if (output.getWarnings() != null && !output.getWarnings().isEmpty()) {
      appendField(sb, "Warn", output.getWarnings());
    }
    if(diffListIds != null && !diffListIds.isEmpty()) {
      appendField(sb, "DiffLists", diffListIds);
    }
    return sb.toString();
  }
  
  
  private void appendField(StringBuilder sb, String name, List<String> data) {
    sb.append("  ").append(name).append(": ").append(data.size()).append(": ").append(String.join(", ", data)).append("\n");
  }

}
