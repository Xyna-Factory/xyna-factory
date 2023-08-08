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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Listrepositoryusers;
import xmcp.gitintegration.repository.RepositoryUser;
import xmcp.gitintegration.storage.UserManagementStorage;



public class ListrepositoryusersImpl extends XynaCommandImplementation<Listrepositoryusers> {

  private static final String USER_STRING = "Repo user: '%s', Factory user: '%s', Mail: '%s', %sCreated: %s\n";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");


  public void execute(OutputStream statusOutputStream, Listrepositoryusers payload) throws XynaException {
    UserManagementStorage storage = new UserManagementStorage();
    boolean all = payload.getRepository() == null || payload.getRepository().length() == 0;
    List<RepositoryUser> users = all ? storage.listAllUsers() : storage.listUsersOfRepo(payload.getRepository());
    for (RepositoryUser user : users) {
      writeToCommandLine(statusOutputStream, userString(user, all));
    }
  }


  private static String userString(RepositoryUser user, boolean printRepo) {
    String dateString = DATE_FORMAT.format(new Date(user.getCreated()));
    String repoString = printRepo ? String.format("Repository: '%s' ", user.getRepository()) : "";
    return String.format(USER_STRING, user.getRepositoryUsername(), user.getFactoryUsername(), user.getMail(), repoString, dateString);
  }

}
