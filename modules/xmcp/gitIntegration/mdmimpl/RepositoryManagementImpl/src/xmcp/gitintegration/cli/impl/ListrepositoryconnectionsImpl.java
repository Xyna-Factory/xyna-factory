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
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Listrepositoryconnections;
import xmcp.gitintegration.impl.RepositoryManagementImpl;
import xmcp.gitintegration.repository.RepositoryConnection;



public class ListrepositoryconnectionsImpl extends XynaCommandImplementation<Listrepositoryconnections> {

  public void execute(OutputStream statusOutputStream, Listrepositoryconnections payload) throws XynaException {
    List<RepositoryConnection> data = RepositoryManagementImpl.listRepositoryConnections();
    String format = "Repository: '%s' SubPath: '%s' Workspace: '%s' split: '%s'\n";
    for(RepositoryConnection con : data) {
      String line = String.format(format, con.getPath(), con.getSubpath(), con.getWorkspaceName(), con.getSplittype());
      writeToCommandLine(statusOutputStream, line);
    }
  }

}
