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



import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Addcliusertorepository;
import xmcp.gitintegration.storage.UserManagementStorage;



public class AddcliusertorepositoryImpl extends XynaCommandImplementation<Addcliusertorepository> {

  public void execute(OutputStream statusOutputStream, Addcliusertorepository payload) throws XynaException {
    UserManagementStorage storage = new UserManagementStorage();
    String keyFile = payload.getPrivateKeyFile();
    String key = null;
    if(keyFile != null && !keyFile.isEmpty()) {
      try {
        key = Files.readString(Path.of(keyFile));
      } catch (IOException e) {
        writeToCommandLine(statusOutputStream, "Error loading key file...");
        writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
      }
    }
    storage.AddUserToRepository(UserManagementStorage.CLI_USERNAME, 
                                payload.getUsername(), 
                                payload.getRepository(),
                                payload.getPassword(), 
                                key,
                                payload.getKeyPassphrase(),
                                payload.getMail());
  }

}
