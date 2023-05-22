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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importuser;



public class ImportuserImpl extends XynaCommandImplementation<Importuser> {

  public void execute(OutputStream statusOutputStream, Importuser payload) throws XynaException {
    if (factory.getFactoryManagementPortal().importUser(payload.getUserName(), payload.getRoleName(),
                                                        payload.getPasswordHash())) {
      writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' was succesfully imported\n");
    } else {
      writeToCommandLine(statusOutputStream, "User '" + payload.getUserName() + "' could not be imported\n");
    }
  }

}
