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
package com.gip.juno.ws.tools;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.*;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;

/**
 * abstract class; provides functionality to store data about connections to DPP-locations
 */
public abstract class LocationData {

  protected HashMap<String, FailoverData> _locations = null;

  protected LocationData(Logger logger) throws RemoteException {
    init(logger);
  }

  protected abstract LocationSchema getLocationSchema();

  public String show() {
    StringBuilder ret = new StringBuilder();
    ret.append("LocationData (" + getLocationSchema().toString() + ") { ");
    for (Map.Entry<String, FailoverData> entry : _locations.entrySet()) {
      ret.append("\n Location: " + entry.getKey() + " -> " + entry.getValue().toString());
    }
    ret.append("} \n");
    return ret.toString();
  }


  public static LocationData getInstance(LocationSchema schema, Logger logger) throws RemoteException {
    if (schema == LocationSchema.service) {
      return LocationDataForService.getInstance(logger);
    }
    logger.error("LocationData.getInstance: Illegal Schema requested: " + schema);
    throw new DPPWebserviceIllegalArgumentException("LocationData.getInstance: Illegal Schema requested: " + schema);
  }


  public static void reloadAll(Logger logger) throws RemoteException {
    //LocationDataForAudit.getInstance(logger).reload(logger);
    LocationDataForService.getInstance(logger).reload(logger);
  }

  protected void init(Logger logger) throws RemoteException {
    try {
      _locations = new HashMap<String, FailoverData>();
      logger.info("LocationData (" + getLocationSchema().toString() + ") : Starting init...");

      List<LocationTools.LocationsRow> allLocations = LocationTools.getLocations(getLocationSchema(), logger);
      for (LocationTools.LocationsRow location : allLocations) {
        if ((location.name == null) || (location.name.trim().equals(""))) {
        } else {
          ConnectionInfo info = new ConnectionInfo();
          info.password = location.sql_password;
          info.user = location.sql_user;
          info.url = location.jdbc_url;
          info.ssh_ip = location.ssh_ip;
          FailoverFlag flag = WSTools.translateFailoverFlag(location.failover);
          if (_locations.get(location.name) == null) {
            FailoverData failover = new FailoverData();
            failover.set(info, flag);
            _locations.put(location.name, failover);
          } else {
            _locations.get(location.name).set(info, flag);
          }
        }
      }
      logger.info("Created LocationData: \n " + show());
    } catch (Exception e) {
      logger.error("Error while initialising data for locations: ", e);
      throw new DPPWebserviceException("Error while initialising data for locations", e);
    }
  }

  public FailoverData get(String location, Logger logger) throws RemoteException {
    return _locations.get(location);
  }

  public ConnectionInfo get(String location, FailoverFlag flag, Logger logger) throws RemoteException {
    FailoverData data = _locations.get(location);
    if (data == null) {
      throw new DPPWebserviceException("LocationData: Requested location " + location + " could not be found.");
    }
    ConnectionInfo ret = data.get(flag);
    if (ret == null) {
      throw new DPPWebserviceException("LocationData: Requested location " + location
          + " has no failover data for failover flag " + flag);
    }
    return ret;
  }

  protected void add(String location, FailoverData data, Logger logger) throws RemoteException {
    _locations.put(location, data);
  }

  protected void add(String location, ConnectionInfo data, FailoverFlag flag, Logger logger)
        throws RemoteException {
    FailoverData failoverdata = new FailoverData();
    failoverdata.set(data, flag);
    _locations.put(location, failoverdata);
  }

  public void reload(Logger logger) throws RemoteException {
    init(logger);
  }

  public String[] getAllLocations(Logger logger) throws RemoteException {
    reload(logger);
    Set<String> keyset = _locations.keySet();
    TreeSet<String> tmp = new TreeSet<String>(keyset);
    return tmp.toArray(new String[tmp.size()]);
  }

  public List<ConnectionInfo> getAllConnections(Logger logger) throws RemoteException {
    reload(logger);
    List<ConnectionInfo> ret = new ArrayList<ConnectionInfo>();
    for (FailoverData failover : _locations.values()) {
      ConnectionInfo conn1 = failover.get(FailoverFlag.primary);
      ConnectionInfo conn2 = failover.get(FailoverFlag.secondary);
      if ((conn1 == null) || (conn2 == null)) {
        throw new DPPWebserviceException("LocationData: Found ConnectionInfo for Failover instance that is null. " +
                                         "Maybe an entry is missing in the database locations table?");
      }
      ret.add(conn1);
      ret.add(conn2);
    }
    return ret;
  }

  public List<NamedConnectionInfo> getAllNamedConnections(Logger logger) throws RemoteException {
    reload(logger);
    List<NamedConnectionInfo> ret = new ArrayList<NamedConnectionInfo>();
    for (Map.Entry<String, FailoverData> entry : _locations.entrySet()) {
      FailoverData failover = entry.getValue();
      String location = entry.getKey();
      NamedConnectionInfo named1 = new NamedConnectionInfo();
      ConnectionInfo conn = failover.get(FailoverFlag.primary);
      if (conn == null) {
        throw new DPPWebserviceException("LocationData: Requested location " + location
                                         + " has no failover data for failover flag " + FailoverFlag.primary);
      }
      named1.location = location;
      named1.ssh_ip = conn.ssh_ip;
      ret.add(named1);
      conn = failover.get(FailoverFlag.secondary);
      if (conn == null) {
        throw new DPPWebserviceException("LocationData: Requested location " + location
                                         + " has no failover data for failover flag " + FailoverFlag.secondary);
      }
      NamedConnectionInfo named2 = new NamedConnectionInfo();
      named2.location = location;
      named2.ssh_ip = conn.ssh_ip;
      ret.add(named2);
    }
    return ret;
  }
}
