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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Arrays;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Createuser;



public class CreateuserImpl extends XynaCommandImplementation<Createuser> {

  public void execute(OutputStream statusOutputStream, Createuser payload) throws XynaException {
    if (payload.getDomainName() == null) {
      if (factory.getFactoryManagementPortal().createUser(payload.getUserName(), payload.getRoleName(),
                                                          payload.getPassword(), payload.getEncrypted())) {
        writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' was succesfully created\n");
      } else {
        writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' could not be created\n");
      }
    } else {
      if (factory.getFactoryManagementPortal().createUser(payload.getUserName(), payload.getRoleName(),
                                                          payload.getPassword(), payload.getEncrypted(), Arrays.asList(payload.getDomainName()))) {
        writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' was succesfully created\n");
      } else {
        writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' could not be created\n");
      }
    }
    
  }

}
