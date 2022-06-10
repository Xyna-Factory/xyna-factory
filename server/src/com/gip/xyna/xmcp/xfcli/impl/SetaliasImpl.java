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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Setalias;



public class SetaliasImpl extends XynaCommandImplementation<Setalias> {

  public void execute(OutputStream statusOutputStream, Setalias payload) throws XynaException {
    if (factory.getFactoryManagementPortal().setAliasOfRole(payload.getRoleName(), payload.getDomainName(),
                                                            payload.getAlias())) {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("External role ").append(payload.getRoleName())
                                 .append(" succesfully mapped to internal role ").append(payload.getAlias()).toString());
    } else {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("External role ").append(payload.getRoleName())
                                 .append(" could not be mapped to internal role ").append(payload.getAlias())
                                 .toString());
    }
  }

}
