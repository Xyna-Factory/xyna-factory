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
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.tools.ColInfo.LookupQuery;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.WSTools;
import com.gip.juno.ws.tools.QueryTools.DBLongReader;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;

public class StaticHostTools {

  private static TableHandler _handler = new StaticHostHandler();
  
  private static final String _dnsColName = StaticHostHandler.DNS_COLNAME;  
  private static final String _idColName = StaticHostHandler.STATICHOSTID_COLNAME;
  private static final String _subnetIdColName = StaticHostHandler.SUBNETID_COLNAME;
  
  public static TableHandler getHandler() {
    return _handler;
  }
  
  public static void adjustDns(TreeMap<String, String> map, Logger logger) throws RemoteException {
    String dns = map.get(_dnsColName);
    if (dns == null) {
      throw new DPPWebserviceIllegalArgumentException("Entry '" + _dnsColName + "' is missing in Column Map.");
    }
    dns = WSTools.parseIpList(dns);
    if (dns == null) {
      String subnetid = map.get(_subnetIdColName);
      if (subnetid == null) {
        throw new DPPWebserviceIllegalArgumentException("Entry '" + _subnetIdColName + "' is missing in Column Map.");
      }
      dns = queryDns(subnetid, logger);
      dns = WSTools.parseIpList(dns);
    }
    if (dns == null) {
      //throw new DPPWebserviceIllegalArgumentException("Entry '" + _dnsColName + "' may not be empty.");
      dns = "";
    }
    map.put(_dnsColName, dns);
  }
      
  
  private static String queryDns(String subnetId, Logger logger) throws RemoteException {
    String sql = "SELECT cd.cpeDns FROM cpedns cd "
      + " INNER JOIN sharednetwork sn ON sn.cpednsID = cd.cpednsID "
      + " INNER JOIN subnet s ON s.sharedNetworkID = sn.sharedNetworkID "
      + " WHERE s.subnetID = " + subnetId;
    DBSchema schema = DBSchema.dhcp;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    return new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
  }
  
  
  public static boolean editIsAllowed(TreeMap<String, String> map, Logger logger) throws RemoteException {
    String staticHostId = map.get(_idColName);
    if (staticHostId == null) {
      throw new DPPWebserviceIllegalArgumentException("Entry '" + _idColName + "' is missing in Column Map.");
    }
    if (!onlyPoolIdChangesAndPoolIsNull(map, logger)) {
      String sql = "SELECT COUNT(*) FROM statichost WHERE staticHostID = ? AND deployed1 = ? AND deployed2 = ?";
      DBSchema schema = DBSchema.dhcp;
      DBStringReader reader = new DBStringReader();
      SQLCommand builder = new SQLCommand();
      builder.addConditionParam(staticHostId);
      builder.addConditionParam("NO");
      builder.addConditionParam("NO");
      builder.sql = sql;
      String retval = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
      logger.info("StaticHostTools.editIsAllowed: Select returns " + retval);
      return (retval.trim().equals("1"));
    } else {
      return true;
    }
  }
  
  
  private static boolean onlyPoolIdChangesAndPoolIsNull(TreeMap<String, String> map, Logger logger) throws RemoteException {
    /*for (Entry<String, String> entry : map.entrySet()) {
      System.out.println("Checking: " + entry.getKey() + " : " + entry.getValue());
      if (!entry.getKey().equals(StaticHostHandler.STATICHOSTID_COLNAME) &&
          !entry.getKey().equals(StaticHostHandler.ASSIGNEDPOOLID_COLNAME)) {
        if (entry.getValue() != null &&
            entry.getValue().length() > 0) {
          System.out.println("returning false");
          return false;
        }
      }
    }*/
    TreeMap<String, String> modifiedMap = new TreeMap<String, String>();
    for (Entry<String, String> entry : map.entrySet()) {
      if (entry.getKey().equals(StaticHostHandler.ASSIGNEDPOOLID_COLNAME) ||
          entry.getKey().equals(StaticHostHandler.DESIREDPOOLTYPE_COLNAME) || //it's not shown in the gui...hide from select
          entry.getKey().equals(StaticHostHandler.SUBNETID_COLNAME)) { //is referenced from poolLookup
        //modifiedMap.put(entry.getKey(), null);
      } else {
        modifiedMap.put(entry.getKey(), entry.getValue());
      }
    }
    SQLCommand command = SQLBuilder.buildSQLCountStarWhere(modifiedMap, _handler.getDBTableInfo());
    command.sql += " AND "  + StaticHostHandler.ASSIGNEDPOOLID_COLNAME + " IS NULL";
    Long count = new DBCommands<Long>().queryOneRow(DBSchema.dhcp, new DBLongReader(), command, logger);
    if (count.equals(1L)) {
      return true;
    } else {
      return false;
    }
    
  }
  
  
  public static String queryIp(String staticHostId, Logger logger) throws RemoteException {
    if (staticHostId == null) {
      //throw new DPPWebserviceIllegalArgumentException("StaticHostTools.queryIp: Parameter staticHostId may not be null.");
      logger.warn("StaticHostTools.queryIp: Parameter staticHostId may not be null.");
      return "unknown";
    }
    String sql = "SELECT ip FROM statichost WHERE staticHostID = ?";
    DBSchema schema = DBSchema.dhcp;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.addConditionParam(staticHostId);
    builder.sql = sql;
    String retval = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
    return retval;
  }
  
  
  public static void updateCmtsIp(String staticHostId, Logger logger) throws RemoteException {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ").append(StaticHostHandler.ASSIGNEDPOOLID_COLNAME).append(" FROM ").append(_handler.getTablename())
                .append(" WHERE ").append(StaticHostHandler.STATICHOSTID_COLNAME).append("=?");
    String sql = queryBuilder.toString();
    DBSchema schema = DBSchema.dhcp;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.addConditionParam(staticHostId);
    builder.sql = sql;
    String assignedPoolId = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
    
    LookupQuery lookupQuery = StaticHostHandler.cmtsIpLookupByAssignedPoolIdQuery;
    SQLCommand lookup = lookupQuery.getSQLCommand().clone();
    lookup.addConditionParam(assignedPoolId);
    String cmtsIp = new DBCommands<String>().queryOneRow(schema, reader, lookup, logger);
    ColInfo cmtsipColInfo = _handler.getDBTableInfo().getColumns().get(StaticHostHandler.CMTSIP_COLUMN_NAME);
    cmtsIp = lookupQuery.verify(cmtsIp, cmtsipColInfo);
    
    StringBuilder updateBuilder = new StringBuilder();
    updateBuilder.append("UPDATE ").append(_handler.getTablename())
                 .append(" SET ").append(StaticHostHandler.CMTSIP_COLUMN_NAME).append("=? WHERE ")
                 .append(StaticHostHandler.STATICHOSTID_COLNAME).append("=?");
    SQLCommand update = new SQLCommand(updateBuilder.toString());
    update.addConditionParam(cmtsIp);
    update.addConditionParam(staticHostId);
    
    DBCommands.executeDML(schema, update, logger);
  }
  
  
  

}
