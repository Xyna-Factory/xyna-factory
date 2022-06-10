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

package com.gip.juno.ws.tools.ssh;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.tools.ConnectionInfo;
import com.gip.juno.ws.tools.LocationData;
import com.gip.xyna.utils.ssh.Ssh;


public class TargetSshConnection {

  private Logger _logger = null;
  private String _location = null;
  private FailoverFlag _failoverFlag = FailoverFlag.primary;
  private Ssh _ssh = null;
  private String _ip = null;


  public TargetSshConnection(String location, FailoverFlag failoverFlag, Logger logger) throws RemoteException {
    _location = location;
    _failoverFlag = failoverFlag;
    _logger = logger;
    init();
  }


  public void close() {
    if (_ssh == null) {
      _logger.debug("TargetSshConnection, location = " + _location + ", failover = " + _failoverFlag
                    + ": Unable to close ssh connection, ssh object is null.");
      return;
    }
    try {
      _ssh.close();
    }
    catch (Exception e) {
      _logger.debug("TargetSshConnection, location = " + _location + ", failover = " + _failoverFlag
                    + ": Error closing ssh connection.", e);
    }
  }


  public static List<TargetSshConnection> getFailoverPairConnections(String location, Logger logger)
                throws RemoteException {
    List<TargetSshConnection> ret = new ArrayList<TargetSshConnection>();
    ret.add(new TargetSshConnection(location, FailoverFlag.primary, logger));
    ret.add(new TargetSshConnection(location, FailoverFlag.secondary, logger));
    return ret;
  }

  public static void closeConnections(List<TargetSshConnection> connections)
                throws RemoteException {
    for (TargetSshConnection con : connections) {
      con.close();
    }
  }

  private void init() throws RemoteException {
    _ssh = SshTools.openSshConnection(_location, _failoverFlag, _logger);
    LocationData instance = LocationData.getInstance(LocationSchema.service, _logger);
    ConnectionInfo info = instance.get(_location, _failoverFlag, _logger);
    _ip = info.ssh_ip;
  }


  public String getLocation() {
    return _location;
  }


  public FailoverFlag getFailoverFlag() {
    return _failoverFlag;
  }


  public Ssh getSsh() {
    return _ssh;
  }


  public String getIp() {
    return _ip;
  }

}
