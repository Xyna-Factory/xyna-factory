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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.xyna.utils.db.ResultSetReader;

/**
 * performs querys on the location databse tables to get information about DPP locations 
 */
public class LocationTools {


  private static class LocationsReader implements ResultSetReader<LocationsRow> {
    private Logger _logger;
    public LocationsReader(Logger logger) {
      _logger = logger;
    }
    public Logger getLogger() {
      return _logger;
    }   
    public LocationsRow read(ResultSet rs) throws SQLException {
      LocationsRow ret = new LocationsRow();
      ret.name = WSTools.getColValue(rs, "name");
      ret.sql_user = WSTools.getColValue(rs, "sql_user");
      ret.sql_password = WSTools.getColValue(rs, "sql_password");
      ret.jdbc_url = WSTools.getColValue(rs, "jdbc_url");
      ret.failover = WSTools.getColValue(rs, "failover");
      ret.ssh_user = WSTools.getColValue(rs, "ssh_user");
      ret.ssh_password = WSTools.getColValue(rs, "ssh_password");
      ret.ssh_ip = WSTools.getColValue(rs, "ssh_ip");
      ret.ssh_rsaKey = WSTools.getColValue(rs, "ssh_rsaKey");
      return ret;
    }
  } 
  
  public static class LocationsRow {
    public String name;
    public String sql_user;
    public String jdbc_url;
    public String sql_password;
    public String failover;
    public String ssh_user;
    public String ssh_ip;
    public String ssh_password;
    public String ssh_rsaKey;
    public String commment;
  }
  


  /**
   * returns list of locations from table locations
   */
  public static List<LocationsRow> getLocations(LocationSchema schema, Logger logger) 
        throws java.rmi.RemoteException {
    try {      
      String locationSchema = getSchemaName(schema);
      /*
      SQLUtilsContainerForManagement container = SQLUtilsCache.getForManagement(
          locationSchema, logger);
      */
      String sql = "SELECT name, sql_user, sql_password, jdbc_url, ssh_user, ssh_password, ssh_ip, "
          + "ssh_rsaKey, failover, comment FROM locations";
      LocationsReader reader = new LocationsReader(logger);      
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      if (schema == LocationSchema.service) {
        builder.sql += " WHERE name != ? ";
        builder.addConditionParam(Constants.managementName);
      }
      //List<LocationsRow> ret = new DBCommands<LocationsRow>().query(reader, builder, container, logger);
      List<LocationsRow> ret = new DBCommandsUncached<LocationsRow>().queryForManagement(reader, builder, 
          locationSchema, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error: " + e.toString());
    }
  }
  
  private static String getSchemaName(LocationSchema schema) throws RemoteException {
    /*
    if (schema == LocationSchema.audit) {
      return Constants.auditSchema;
    } else
    */ 
    if (schema == LocationSchema.service) {
      return Constants.serviceSchema;
    } 
    throw new java.rmi.RemoteException("LocationTools: Requested Location Schema does not exist.");
  }

  /**
   * returns one row from table locations
   */
  public static LocationsRow getManagementRow(FailoverFlag flag, Logger logger) 
        throws java.rmi.RemoteException {    
    return getLocationsRow(Constants.managementName, flag, LocationSchema.service, logger);    
  }
  
  /**
   * returns one row from table locations
   */
  public static LocationsRow getLocationsRow(String location, FailoverFlag flag, LocationSchema schema, 
        Logger logger) throws java.rmi.RemoteException {
    try {
      String locationsSchema = getSchemaName(schema);
      /*
      SQLUtilsContainerForManagement container = SQLUtilsCache.getForManagement(
          locationsSchema, logger);
      */
      //SqlUtils utils = SQLUtilsCacheForManagement.createSQLUtils(schema, flag, logger);
      String sql = "SELECT name, sql_user, sql_password, jdbc_url, ssh_user, ssh_password, ssh_ip, "
          + "ssh_rsaKey, failover, comment FROM locations WHERE name = ? and failover = ?";
      LocationsReader reader = new LocationsReader(logger);      
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      builder.addConditionParam("name", location);
      builder.addConditionParam("failover", flag.toString());    
      //LocationsRow ret = new DBCommands<LocationsRow>().queryOneRow(reader, builder, container, logger);
      LocationsRow ret = new DBCommandsUncached<LocationsRow>().queryOneRowForManagement(reader, builder, 
          locationsSchema, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new java.rmi.RemoteException("Error: " + e.toString());
    }
  }
    
  
}
