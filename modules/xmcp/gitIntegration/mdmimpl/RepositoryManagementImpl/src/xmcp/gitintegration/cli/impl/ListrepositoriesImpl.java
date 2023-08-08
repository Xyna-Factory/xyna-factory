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
import xmcp.gitintegration.cli.generated.Listrepositories;
import xmcp.gitintegration.impl.RepositoryConnectionStorable;
import xmcp.gitintegration.impl.RepositoryManagementImpl;
import xmcp.gitintegration.repository.RepositoryUser;
import xmcp.gitintegration.storage.UserManagementStorage;



public class ListrepositoriesImpl extends XynaCommandImplementation<Listrepositories> {

  public void execute(OutputStream statusOutputStream, Listrepositories payload) throws XynaException {
    
    UserManagementStorage userStorage = new UserManagementStorage();
    List<? extends RepositoryConnectionStorable> connections = RepositoryManagementImpl.loadRepositoryConnections();
    List<RepositoryUser> users = userStorage.listAllUsers();
    List<? extends RepositoryConnectionStorable> repos = RepositoryManagementImpl.loadRepositoryConnections();
    for(RepositoryConnectionStorable repo : repos) {
      writeToCommandLine(statusOutputStream, createRepoData(repo, connections, users));
    }
  }

  private String createRepoData(RepositoryConnectionStorable repo, List<? extends RepositoryConnectionStorable> connections, List<RepositoryUser> users) {
    StringBuilder sb = new StringBuilder();
    sb.append(repo.getPath());
    sb.append(" has ");
    sb.append(connections.stream().filter(x-> x.getPath().equals(repo.getPath())).count());
    sb.append(" connected workspaces and ");
    sb.append(users.stream().filter(x -> x.getRepository().equals(repo.getPath())).count());
    sb.append(" configured users.\n");
    return sb.toString();
  }

}
