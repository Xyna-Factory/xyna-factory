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
import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.xmcp.xfcli.generated.Removeconnectionpool;



public class RemoveconnectionpoolImpl extends XynaCommandImplementation<Removeconnectionpool> {

  public void execute(OutputStream statusOutputStream, Removeconnectionpool payload) throws XynaException {
    long timeout = ShutdownconnectionpoolImpl.getTimeoutSeconds(payload.getNow());
    try {
      if (XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement().removeConnectionPool(payload.getName(), payload.getNow(), timeout, TimeUnit.SECONDS)) {
        writeLineToCommandLine(statusOutputStream, "ConnectionPool " + payload.getName() + " successfully removed.");
      } else {
        writeLineToCommandLine(statusOutputStream, "ConnectionPool " + payload.getName() + " could not be removed.");
      }
    } catch (ConnectionCouldNotBeClosedException e) {
      throw new RuntimeException(e);
    }
  }

}
