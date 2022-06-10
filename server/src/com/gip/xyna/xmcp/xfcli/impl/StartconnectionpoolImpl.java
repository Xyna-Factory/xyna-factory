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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import com.gip.xyna.xmcp.xfcli.generated.Startconnectionpool;



public class StartconnectionpoolImpl extends XynaCommandImplementation<Startconnectionpool> {

  public void execute(OutputStream statusOutputStream, Startconnectionpool payload) throws XynaException {
    try {
      if (XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement().startConnectionPool(payload.getName())) {
        writeLineToCommandLine(statusOutputStream, "ConnectionPool " + payload.getName() + " has been successfully started.");
      } else {
        writeLineToCommandLine(statusOutputStream, "ConnectionPool " + payload.getName() + " could not be started.");
      }
    } catch (NoConnectionAvailableException e) {
      throw new RuntimeException(e);
    }
  }

}
