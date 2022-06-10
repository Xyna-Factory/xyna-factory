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
package com.gip.juno.ws.handler;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.dhcp.PoolHandler;
import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.db.tables.dhcp.SubnetHandler;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceAuthenticationException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBLongReader;
import com.gip.xyna.utils.db.ResultSetReader;


public class ValidationHandler {

  private final static Logger logger = Logger.getLogger(ValidationHandler.class);
  
  private final static SubnetHandler subnetHandler = new SubnetHandler();
  private final static PoolHandler poolHandler = new PoolHandler();
  private final static StaticHostHandler hostHandler = new StaticHostHandler();
  


  public static void validatePool(String rangeStart, String rangeStop, String subnetId) throws RemoteException {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ").append(SubnetHandler.SUBNET_COLUMN_NAME).append(", ").append(SubnetHandler.SUBNETMASK_COLUMN_NAME)
                .append(" FROM ").append(subnetHandler.getTablename())
                .append(" WHERE ").append(StaticHostHandler.SUBNETID_COLNAME).append("=?");
    DBSchema schema = DBSchema.dhcp;
    DBCommands<Map<String, String>> com = new DBCommands<Map<String, String>>();
    SQLCommand builder = new SQLCommand();
    builder.addConditionParam(subnetId);
    builder.sql = queryBuilder.toString();
    Map<String, String> subnetValues = com.queryOneRow(schema, new MapReader(SubnetHandler.SUBNET_COLUMN_NAME, SubnetHandler.SUBNETMASK_COLUMN_NAME), builder, logger);
    if (!checkInSubnet(subnetValues.get(SubnetHandler.SUBNET_COLUMN_NAME), subnetValues.get(SubnetHandler.SUBNETMASK_COLUMN_NAME), rangeStart) ||
        !checkInSubnet(subnetValues.get(SubnetHandler.SUBNET_COLUMN_NAME), subnetValues.get(SubnetHandler.SUBNETMASK_COLUMN_NAME), rangeStop)) {
      throw new DPPWebserviceAuthenticationException(new MessageBuilder().setDomain("F").setErrorNumber("00225").setDescription("Pool ranges violate the subnet."));
    }
    return ;
  }
  
  
  
  public static void validateStatichost(String ip, String poolId, String staticHostId) throws RemoteException {
    if (ip == null || ip.length() <= 0) {
      return;
    }
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ").append(PoolHandler.RANGESTART_COLUMN_NAME).append(", ").append(PoolHandler.RANGESTOP_COLUMN_NAME)
                .append(", ").append(PoolHandler.SUBNETID_COLUMN_NAME)
                .append(" FROM ").append(poolHandler.getTablename())
                .append(" WHERE ").append(PoolHandler.POOLID_COLUMN_NAME).append("=?");
    DBSchema schema = DBSchema.dhcp;
    DBCommands<Map<String, String>> com = new DBCommands<Map<String, String>>();
    SQLCommand builder = new SQLCommand();
    builder.addConditionParam(poolId);
    builder.sql = queryBuilder.toString();
    Map<String, String> poolValues = com.queryOneRow(schema, new MapReader(PoolHandler.RANGESTART_COLUMN_NAME, PoolHandler.RANGESTOP_COLUMN_NAME, PoolHandler.SUBNETID_COLUMN_NAME), builder, logger);
    
    if (!checkIpContainedInPool(poolValues.get(PoolHandler.RANGESTART_COLUMN_NAME), poolValues.get(PoolHandler.RANGESTOP_COLUMN_NAME), ip)) {
      throw new DPPWebserviceAuthenticationException(new MessageBuilder().setDomain("F").setErrorNumber("00226").setDescription("IP is not inside the assigned pool."));
    }
    if (!checkIpIsFree(ip, staticHostId)) {
      throw new DPPWebserviceAuthenticationException(new MessageBuilder().setDomain("F").setErrorNumber("00227").setDescription("IP is already assigned to a different Host."));
    }
    queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ").append(SubnetHandler.SUBNETMASK_COLUMN_NAME).append(" FROM ").append(subnetHandler.getTablename())
                .append(" WHERE ").append(StaticHostHandler.SUBNETID_COLNAME).append("=?");
    builder = new SQLCommand();
    builder.addConditionParam(poolValues.get(PoolHandler.SUBNETID_COLUMN_NAME));
    builder.sql = queryBuilder.toString();
    Map<String, String> subnetValues = com.queryOneRow(schema, new MapReader(SubnetHandler.SUBNETMASK_COLUMN_NAME), builder, logger);
    if (checkIsBroadcast(ip, subnetValues.get(SubnetHandler.SUBNETMASK_COLUMN_NAME))) {
      throw new DPPWebserviceAuthenticationException(new MessageBuilder().setDomain("F").setErrorNumber("00228").setDescription("IP is a broadcast address within the subnet."));
    }
  }
  
  
  private static int ipv4ToInt(String ip) {
    int ipInt = 0;
    int index = 0;
    for (int i = 0; i < ip.length(); i++) {
      if (ip.charAt(i) == '.') {
        int ipVal = Integer.parseInt(ip.substring(index, i));
        ipInt = (ipInt << 8) + ipVal;
        index = i + 1;
      }
    }
    ipInt = (ipInt << 8) + Integer.parseInt(ip.substring(index));
    return ipInt;
  }
  
  
  private static long ipv4ToLong(String ip) {
    long ipInt = 0;
    int index = 0;
    for (int i = 0; i < ip.length(); i++) {
      if (ip.charAt(i) == '.') {
        int ipVal = Integer.parseInt(ip.substring(index, i));
        ipInt = (ipInt << 8) + ipVal;
        index = i + 1;
      }
    }
    ipInt = (ipInt << 8) + Integer.parseInt(ip.substring(index));
    return ipInt;
  }
  
  
  private static boolean checkIsBroadcast(String ip, String subnetMask) {
    int mask = ipv4ToInt(subnetMask);
    int ipAsInt = ipv4ToInt(ip);
    int broadcast = -1; // same as ipv4ToInt("255.255.255.255")
    return (ipAsInt | mask) == broadcast;
  }
  
  
  private static boolean checkIpIsFree(String ip, String staticHostId) throws RemoteException {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT count(*) ").append(" FROM ").append(hostHandler.getTablename())
                .append(" WHERE ").append(StaticHostHandler.IP_COLUMN_NAME).append("=?");
    if (staticHostId != null && staticHostId.length() > 0) {
      queryBuilder.append(" AND NOT ").append(StaticHostHandler.STATICHOSTID_COLNAME).append("=?");
    }
    DBSchema schema = DBSchema.dhcp;
    DBCommands<Long> com = new DBCommands<Long>();
    SQLCommand builder = new SQLCommand();
    builder.addConditionParam(ip);
    if (staticHostId != null && staticHostId.length() > 0) {
      builder.addConditionParam(staticHostId);
    }
    builder.sql = queryBuilder.toString();
    Long count = com.queryOneRow(schema, new DBLongReader(), builder, logger);
    return count <= 0;
  }
  
  
  private static boolean checkInSubnet(String subnetIp, String subnetMask, String ip) {
    int subnet = ipv4ToInt(subnetIp);
    int mask = ipv4ToInt(subnetMask);
    int ipToCheck = ipv4ToInt(ip);
    return (ipToCheck & mask) == subnet;
  }
  
  
  private static boolean checkIpContainedInPool(String rangeStart, String rangeStop, String ip) {
    long lower = ipv4ToLong(rangeStart);
    long upper = ipv4ToLong(rangeStop);
    long ipToCheck = ipv4ToLong(ip);
    return ipToCheck >= lower && ipToCheck <= upper;
  }
  


  private static class MapReader implements ResultSetReader<Map<String, String>> {

    private final String[] columns;
    
    private MapReader(String... columns) {
      this.columns = columns;
    }
    
    @Override
    public Map<String, String> read(ResultSet rs) throws SQLException {
      Map<String, String> result = new HashMap<String, String>();
      for (String column : columns) {
        String value = rs.getString(column);
        result.put(column, value);
      }
      return result;
    }
    
  }
  
}
