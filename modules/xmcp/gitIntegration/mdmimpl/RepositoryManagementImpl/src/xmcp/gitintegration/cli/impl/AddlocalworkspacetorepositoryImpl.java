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
import xmcp.gitintegration.cli.generated.Addlocalworkspacetorepository;
import xmcp.gitintegration.impl.RepositoryManagementImpl;
import xmcp.gitintegration.impl.tracking.CliEventTracker;
import xmcp.gitintegration.repository.RepositoryConnection;



public class AddlocalworkspacetorepositoryImpl extends XynaCommandImplementation<Addlocalworkspacetorepository> {

  public void execute(OutputStream statusOutputStream, Addlocalworkspacetorepository payload) throws XynaException {
    RepositoryConnection.Builder builder = new RepositoryConnection.Builder();
    builder.path(payload.getPath());
    builder.savedinrepo(payload.getEntireRevision());
    builder.splittype(payload.getSplit());
    builder.subpath(payload.getSubPath());
    builder.workspaceName(payload.getWorkspace());
    CliEventTracker eventTracker = new CliEventTracker(statusOutputStream);
    boolean success = RepositoryManagementImpl.addLocalWorkspaceToRepository(builder.instance(), eventTracker);
    if (success) {
      writeToCommandLine(statusOutputStream, "Added '" + payload.getWorkspace() + "' to repository.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Failed to add '" + payload.getWorkspace() + "' to repository.");
      writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
    }
  }

}
