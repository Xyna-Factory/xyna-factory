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

import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Refreshconnectionpool;



public class RefreshconnectionpoolImpl extends XynaCommandImplementation<Refreshconnectionpool> {

  public void execute(OutputStream statusOutputStream, Refreshconnectionpool payload) throws XynaException {
    String conPoolName = payload.getConnectionPoolName();
    boolean force = payload.getForce();
    ConnectionPool[] conPools = ConnectionPool.getAllRegisteredConnectionPools();
    boolean foundPool = false;
    for (ConnectionPool pool : conPools) {
      if (pool.getId().equals(conPoolName)) {
        foundPool = true;
        pool.recreateAllConnections(force);
      }
    }
    if (!foundPool) {
      writeLineToCommandLine(statusOutputStream, "pool not found");
    }
  }

}
