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
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;


import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.audit.LeasesHandler;
import com.gip.juno.ws.enums.Authentication;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.enums.LookupStyle;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.ResultSetReader;

/**
 * Class used for database operations that do not need explicit input describing all columns of a table
 */
public class QueryTools {

  private static String _aaaSchema = "aaa";
  private static String _dhcpSchema = "dhcp";

  public static class DBStringReader implements ResultSetReader<String> {
    public DBStringReader() { }
    public String read(ResultSet rs) throws SQLException {
      return rs.getString(1);
    }
  }
  
  
  public static class DBLongReader implements ResultSetReader<Long> {
    public DBLongReader() { }
    public Long read(ResultSet rs) throws SQLException {
      return rs.getLong(1);
    }
  }
  
  
  public static class DBIntegerReader implements ResultSetReader<Integer> {
    public DBIntegerReader() { }
    public Integer read(ResultSet rs) throws SQLException {
      return rs.getInt(1);
    }
  }


  /**
   * returns list of distinct values in column
   */
  public static List<String> getColValuesDistinct(String colname, DBTableInfo table, Logger logger)
        throws java.rmi.RemoteException {
    try {
      SQLUtilsContainerForManagement container = SQLUtilsCache.getForManagement(
          table.getSchema(), logger);
      String sql = SQLBuilder.buildSQLGetColValuesDistinct(colname, table);
      DBStringReader reader = new DBStringReader();
      if (sql.equals("")) {
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      List<String> ret = new DBCommands<String>().query(reader, builder, container, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }





  /**
   * returns value that is looked up in second table, by a column corresponding to one
   * in the first table (usually a foreign key constraint);
   * if necessary, separate string with csv-list into several values to be looked up
   * separately
   */
  public static String queryForeignValue(ForeignValueData data, Logger logger)
        throws java.rmi.RemoteException {
    try {
       if (data == null) {
         throw new java.rmi.RemoteException("QueryForeignValue: Parameter null");
       }
       String ret = null;
       if (data.getLookupStyle() == LookupStyle.singleval) {
         ret = querySingleForeignValue(data, logger);
       } else if (data.getLookupStyle() == LookupStyle.csv) {
         String[] allvals = data.getConditionVal().split(",");
         ret = "";
         for (int i = 0; i < allvals.length; i++) {
           data.setConditionVal(allvals[i]);
           ret += querySingleForeignValue(data, logger);
           if (i < allvals.length -1) {
             ret+= ",";
           }
         }
       }
       return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in queryForeignValue ", e);
    }
  }


  /**
   * returns value that is looked up in second table, by a column corresponding to one
   * in the first table (usually a foreign key constraint)
   */
  private static String querySingleForeignValue(ForeignValueData data, Logger logger)
        throws java.rmi.RemoteException {
    if ((data.getTable() == null) && (data.getTable().trim().equals(""))) {
      logger.error("QueryForeignValue: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("QueryForeignValue: Unable to build SQL Command.");
    }
    if ((data.getValueCol() == null) && (data.getValueCol().trim().equals(""))) {
      logger.error("QueryForeignValue: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("QueryForeignValue: Unable to build SQL Command.");
    }
    if ((data.getConditionCol() == null) && (data.getConditionCol().trim().equals(""))) {
      logger.error("QueryForeignValue: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("QueryForeignValue: Unable to build SQL Command.");
    }
    String sql = "SELECT " + data.getValueCol() + " FROM " + data.getTable() + " WHERE " + data.getConditionCol()
        + " = ? ";
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addParam(new ColName(data.getValueCol()), new ColStrValue(data.getConditionVal()), ColType.string);
    String ret = new DBCommands<String>().queryOneRow(data.getSchema(), reader, builder, logger);
    //ret = new DBCommands<String>().queryOneRow(data.getSchema(), new LoggerResultSetReader<String>(), builder);
    return ret;

  }

  /**
   * if column is marked for auto-increment in parameter table,
   * add to map for that column a value that simulates being created by auto-increment
   */
  public static void checkAutoIncrement(Map<String, String> map, DBTableInfo table, Logger logger)
        throws RemoteException {
    try {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (table.getColumns().containsKey(entry.getKey())) {
          ColInfo col = table.getColumns().get(entry.getKey());
          String colval = entry.getValue();
          boolean isnull = false;
          if (colval == null) {
            isnull = true;
          } else if (colval.equals("")) {
            isnull = true;
          }
          if (col.autoIncrement && isnull) {
            String colValDB = "" + getAutoIncrement(table.getSchema(), col.dbname, table.getTablename(),
                logger);
            map.put(entry.getKey(), colValDB);
          }
        }
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  /**
   * query next value for pseudo-auto-increment column
   */
  public static int getAutoIncrement(String schema, String col, String table, Logger logger)
        throws RemoteException {
    try {
      String sql = "SELECT MAX(" + col + ") FROM " + table;
      DBStringReader reader = new DBStringReader();
      SQLCommand builder = new SQLCommand();
      builder.sql = sql;
      String strval = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
      if (strval == null) {
        return 1;
      }
      int ret = Integer.parseInt(strval) +1;
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in getAutoIncrement ", e);
    }
  }


  public static Authentication authenticate(String input, Logger logger) throws java.rmi.RemoteException {
    if ((input == null) || (input.trim().equals(""))) {
      return Authentication.denied;
    }
    String[] parts = input.split(",");
    if (parts.length != 2) {
      return Authentication.denied;
    }
    String password = parts[0];
    String username = parts[1];
    return authenticate(username, password, logger);
  }

  /**
   * check username and password
   */
  public static Authentication authenticate(String username, String password, Logger logger)
        throws java.rmi.RemoteException {
    try {
      if ((username == null) || (username.trim().equals(""))) {
        logger.error("Authentication denied for user " + username);
        return Authentication.denied;
      }
      if (password == null) {
        password = "";
      }
      String dbPassword = queryPassword(username, logger);
      if (dbPassword == null) {
        logger.error("Authentication denied for user " + username);
        return Authentication.denied;
      }
      if (dbPassword.equals(password)) {
        logger.error("Authentication successful for user " + username);
        return Authentication.accepted;
      }
      logger.error("Authentication denied for user " + username);
      return Authentication.denied;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in Authenticate: ", e);
    }
  }

  /**
   * query password for username in database
   */
  private static String queryPassword(String username, Logger logger) throws java.rmi.RemoteException {
    String sql = "SELECT passwort FROM user WHERE name = ? ";
    String schema = _aaaSchema;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addParam(new ColName("name"), new ColStrValue(username), ColType.string);
    return new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
  }

  public static boolean queryConditionIDExists(String id, Logger logger, String schemaForConditionLookup) throws RemoteException {
    boolean ret = false;
    String sql;
    if (schemaForConditionLookup.equals(ConditionalChecker.DEFAULT_SCHEMA_FOR_CONDITION_LOOKUP)) {
     sql = "SELECT COUNT(*) FROM `condition` WHERE conditionID = ?";
    } else { // condition-table is named classcondition in dhcpv6-schema
      sql = "SELECT COUNT(*) FROM classcondition WHERE conditionID = ?";
    }

    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addConditionParam(id);
    String count = new DBCommands<String>().queryOneRow(schemaForConditionLookup, reader, builder, logger);
    if (Integer.parseInt(count.trim()) == 1) {
      ret = true;
    }
    return ret;
  }

  public static String queryNameOfStandortgruppe(String standortid, Logger logger) throws RemoteException {
    String sql = "SELECT name FROM standortgruppe WHERE standortGruppeID = ?";
    String schema = _dhcpSchema;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addConditionParam(standortid);
    String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
    return ret;
  }


  public static String selectColValueByNonEmptyColsCondition(DBTableInfo table, String colName,
      TreeMap<String, String> oldvalmap, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand command = new SQLCommand();
      command.sql = "SELECT " + colName + " FROM " + table.getTablename();
      command.sql += " WHERE ";
      SQLBuilder.addSQLNonEmptyColsCondition(oldvalmap, table, command);
      DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
      DBStringReader reader = new DBStringReader();
      String ret = new DBCommands<String>().queryOneRow(schema, reader, command, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  public static String selectColValueByPkColsCondition(DBTableInfo table, String colName,
      TreeMap<String, String> oldvalmap, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand command = new SQLCommand();
      command.sql = "SELECT " + colName + " FROM " + table.getTablename();
      command.sql += " WHERE ";
      SQLBuilder.addSQLPkColsCondition(oldvalmap, table, command);
      DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
      DBStringReader reader = new DBStringReader();
      String ret = new DBCommands<String>().queryOneRow(schema, reader, command, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  public static boolean permissionsOK(String username, String webservice, String wsMethod, Logger logger)
          throws RemoteException {
    String sql = "SELECT count(*) FROM user, permissions WHERE user.name = ? "
      + " AND user.roleid = permissions.roleid AND permissions.webservice = ? "
      + " AND permissions.wsmethod = ? ";
    String schema = _aaaSchema;
    DBStringReader reader = new DBStringReader();
    SQLCommand builder = new SQLCommand();
    builder.sql = sql;
    builder.addConditionParam(username);
    builder.addConditionParam(webservice);
    builder.addConditionParam(wsMethod);
    String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
    if (ret.trim().equals("1")) {
      return true;
    }
    return false;
  }


  /**
   * returns how often value appears in table column
   */
  public static int countColValue(DBTableInfo table, String colName, String value, Logger logger)
        throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountColValue(table, colName, value);
      DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
      SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(schema, logger);
      return countRows(command, utils, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }



  public static int countAllRows(DBTableInfo table, Logger logger)
        throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountStar(table);
      DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
      SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(schema, logger);
      return countRows(command, utils, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  public static int countRowsWithCondition(DBTableInfo table, TreeMap<String, String> map, Logger logger)
        throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountStarWhere(map, table);
      DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
      SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(schema, logger);
      return countRows(command, utils, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  public static int countCPERowsWithCondition(DBTableInfo table, TreeMap<String, String> map, Logger logger)
                  throws java.rmi.RemoteException {
              try {
                SQLCommand command = SQLBuilder.buildSQLCountStarWhere(map, table);
                if(command.sql.contains("WHERE"))
                {
                  command.sql = command.sql + " AND Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' LIMIT 999";
                }
                else
                {
                  command.sql = command.sql + " WHERE Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' LIMIT 999";
                }

                
                DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
                SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(schema, logger);
                return countRows(command, utils, logger);
              } catch (java.rmi.RemoteException e) {
                throw e;
              } catch (Exception e) {
                logger.error("", e);
                throw new DPPWebserviceDatabaseException(e);
              }
            }

  

  /**
   * count rows without condition for all locations
   */
  public static int countAllRowsAllLocations(DBTableInfo table, LocationSchema schema, Logger logger)
        throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountStar(table);
      return countRowsAllLocations(command, schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  /**
   * count rows with condition for all locations
   */
  public static int countRowsWithConditionAllLocations(DBTableInfo table, LocationSchema schema,
        TreeMap<String, String> map, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountStarWhere(map, table);
      return countRowsAllLocations(command, schema, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  /**
   * count rows with condition for one location
   */
  public static int countRowsWithConditionOfLocation(DBTableInfo table, LocationSchema schema,
        TreeMap<String, String> map, String location, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand command = SQLBuilder.buildSQLCountStarWhere(map, table);
      SQLUtilsContainer utils = SQLUtilsCache.getForLocation(location, schema, logger);
      return countRows(command, utils, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  public static int countRowsAllLocations(SQLCommand command, LocationSchema schema, Logger logger)
        throws java.rmi.RemoteException {
    try {
      String[] conns = LocationData.getInstance(schema, logger).getAllLocations(logger);
      int ret = 0;
      for (String location : conns) {
        SQLUtilsContainer utils = SQLUtilsCache.getForLocation(location, schema, logger);
        ret += countRows(command, utils, logger);
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  public static int countRows(SQLCommand command, SQLUtilsContainer utils, Logger logger)
        throws java.rmi.RemoteException {
    try {
      DBStringReader reader = new DBStringReader();
      String retStr = new DBCommands<String>().queryOneRow(reader, command, utils, logger);
      int ret = Integer.parseInt(retStr);
      logger.info("Count Rows returns: " + ret);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  
  //private final static String queryIpfromLeasesv4 = "SELECT ip FROM leasesxynaandisc WHERE host = ?";
  private final static String queryIpfromLeasesv6 = "SELECT ip FROM leases WHERE host = ?";

  private static final String LEASES_TABLENAME = "leases.conf.tablename";
  //private static final String LEASES_SCHEMA = "leases.conf.schema";
  private static final String XYNA_LEASES_TABLENAME = "xynaleases.conf.tablename";
  private static final String XYNA_ACTIVE = "conf.xynaactive";
  private static final String ISC_ACTIVE = "conf.iscactive";


  /**
   * intended to be called directly by webservice cmManagement or mtamanagement
   */
  public static String queryLeasesIpForV4OrV6ByMac(String mac, boolean ipv6First, Logger logger)
                       throws RemoteException {
    try {
      Properties wsProperties = PropertiesHandler.getWsProperties();
      String tablename = PropertiesHandler.getProperty(wsProperties, LEASES_TABLENAME, logger);
      String xynatablename = PropertiesHandler.getProperty(wsProperties, XYNA_LEASES_TABLENAME, logger);
      
      boolean isc = false;
      boolean xyna = false;
      String iscactive = PropertiesHandler.getProperty(wsProperties, ISC_ACTIVE, logger);
      if(iscactive.equalsIgnoreCase("true"))
      {
        isc=true;
      }
      String xynaactive = PropertiesHandler.getProperty(wsProperties, XYNA_ACTIVE, logger);
      if(xynaactive.equalsIgnoreCase("true"))
      {
        xyna=true;
      }
      if(isc)
      {
        String queryIpfromLeasesv4 = "SELECT ip FROM "+tablename+" WHERE host = ?";
        DBSchema schema = DBSchema.audit;
        String sql = queryIpfromLeasesv4;
        if (ipv6First) {
          schema = DBSchema.auditv6memory;
          sql = queryIpfromLeasesv6;
        }
        DBStringReader reader = new DBStringReader();
        SQLCommand builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);

        if (ret != null) {
          return ret;
        }
        schema = DBSchema.auditv6memory;
        sql = queryIpfromLeasesv6;
        if (ipv6First) {
          schema = DBSchema.audit;
          sql = queryIpfromLeasesv4;
        }
        builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        logger.debug("queryLeasesIpForV4OrV6ByMac(), mac = " + mac + ": Did not find entry in table " +
                     schema + ".leases, going to search in " + schema + ".leases.");
        ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
        if(ret!=null)return ret;
      }
      if(xyna)
      {
        String queryIpfromLeasesv4 = "SELECT ip FROM "+xynatablename+" WHERE host = ?";
        DBSchema schema = DBSchema.auditv4memory;
        String sql = queryIpfromLeasesv4;
        if (ipv6First) {
          schema = DBSchema.auditv6memory;
          sql = queryIpfromLeasesv6;
        }
        DBStringReader reader = new DBStringReader();
        SQLCommand builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);

        if (ret != null) {
          return ret;
        }
        schema = DBSchema.auditv6memory;
        sql = queryIpfromLeasesv6;
        if (ipv6First) {
          schema = DBSchema.auditv4memory;
          sql = queryIpfromLeasesv4;
        }
        builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        logger.debug("queryLeasesIpForV4OrV6ByMac(), mac = " + mac + ": Did not find entry in table " +
                     schema + ".leases, going to search in " + schema + ".leases.");
        ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
        if(ret!=null)return ret;
      }
      return null;

    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  
  /**
   * intended to be called directly by webservice SetLogging
   */
  public static String queryLeasesDppInstanceByMac(String mac, Logger logger) throws RemoteException {
    try {
      Properties wsProperties = PropertiesHandler.getWsProperties();
      String xynatablename = PropertiesHandler.getProperty(wsProperties, XYNA_LEASES_TABLENAME, logger);
      String tablename = PropertiesHandler.getProperty(wsProperties, LEASES_TABLENAME, logger);
      
      boolean isc = false;
      boolean xyna = false;
      String iscactive = PropertiesHandler.getProperty(wsProperties, ISC_ACTIVE, logger);
      if(iscactive.equalsIgnoreCase("true"))
      {
        isc=true;
      }
      String xynaactive = PropertiesHandler.getProperty(wsProperties, XYNA_ACTIVE, logger);
      if(xynaactive.equalsIgnoreCase("true"))
      {
        xyna=true;
      }

      if(xyna)
      {
        //String sql = "SELECT dppinstance FROM leasesxynaandisc WHERE host = ?";
        String sql = "SELECT dppinstance FROM "+xynatablename+" WHERE host = ?";
        DBSchema schema = DBSchema.auditv4memory;
        DBStringReader reader = new DBStringReader();
        SQLCommand builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
        if(ret!=null)return ret;
      }
      if(isc)
      {
        //String sql = "SELECT dppinstance FROM leasesxynaandisc WHERE host = ?";
        String sql = "SELECT dppinstance FROM "+tablename+" WHERE host = ?";
        DBSchema schema = DBSchema.audit;
        DBStringReader reader = new DBStringReader();
        SQLCommand builder = new SQLCommand();
        builder.sql = sql;
        builder.addConditionParam(mac);
        String ret = new DBCommands<String>().queryOneRow(schema, reader, builder, logger);
        if(ret!=null)return ret;
      }
      
      return null;
    }
    catch (java.rmi.RemoteException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

}
