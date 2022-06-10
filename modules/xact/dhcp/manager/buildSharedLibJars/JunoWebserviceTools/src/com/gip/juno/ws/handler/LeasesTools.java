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

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.LeasesType;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.ManagementData;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.Constants;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainerForManagement;

/**
 * class with special operations needed by the Leases webservice
 */
public class LeasesTools<T> {

  private static final String LEASES_TABLENAME = "leases.conf.tablename";
  private static final String LEASES_SCHEMA = "leases.conf.schema";
  private static final String LEASESCPE_TABLENAME = "leasescpe.conf.tablename";
  private static final String LEASESCPE_SCHEMA = "leasescpe.conf.schema";
  private static final String XYNA_LEASES_TABLENAME = "xynaleases.conf.tablename";
  private static final String XYNA_LEASES_SCHEMA = "xynaleases.conf.schema";
  private static final String XYNA_ACTIVE = "conf.xynaactive";
  private static final String ISC_ACTIVE = "conf.iscactive";

  
  static Logger logger = Logger.getLogger("LeasesTools");

  
  private DBTableInfo _leasesCpeInfo = getDBTableInfoLeasesCpe();
  
  private DBTableInfo _leasesInfo = getDBTableInfoLeases();

  private DBTableInfo _leasesXynaInfo = getDBTableXynaInfoLeases();

  private DBTableInfo _leasesv6Info = getDBTableInfoLeasesv6();
  private DBTableInfo _leasesv6PrefixInfo = getDBTableInfoLeasesv6Prefixes();
  private DBTableInfo _leasesv6LongInfo = getDBTableInfoLeasesv6Long();
  private DBTableInfo _leasesv6LongPrefixInfo = getDBTableInfoLeasesv6PrefixesLong();
  
  private static boolean isc=false;
  private static boolean xyna=false;
  
  public static String LEASES_V6_TABLENAME = "leases";
  public static String LEASES_V6_PREFIX_TABLENAME = "prefixes";
  public static String LEASES_V6_LONG_TABLENAME = "leaseslong";
  public static String LEASES_V6_PREFIX_LONG_TABLENAME = "prefixeslong";
  
  
  public static DBTableInfo getDBTableInfoLeases() {

    String schema = "";
    String tablename = "";

    Properties wsProperties;
    try {
      logger.info("Getting Schema and Tablename for ISC ...");
      wsProperties = PropertiesHandler.getWsProperties();
      tablename = PropertiesHandler.getProperty(wsProperties, LEASES_TABLENAME, logger);
      schema = PropertiesHandler.getProperty(wsProperties, LEASES_SCHEMA, logger);
      String iscactive = PropertiesHandler.getProperty(wsProperties, ISC_ACTIVE, logger);
      if(iscactive.equalsIgnoreCase("true"))
      {
        isc=true;
      }
    }
    catch (RemoteException e) {
      // TODO Auto-generated catch block
      logger.error("Problems getting configurable table/schema for leases webservice: ",e);
    }

        
    //DBTableInfo table = new DBTableInfo("leasesxynaandisc", "audit");
    DBTableInfo table = new DBTableInfo(tablename, schema);

    table.addColumn(new ColInfo("Host").setType(ColType.string).setVisible(true).setPk());
    table.addColumn(new ColInfo("Ip").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("IpNum").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("StartTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("EndTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Type").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("RemoteId").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("DppInstance").setType(ColType.string).setVisible(true).setUpdates(false));
    return table;
  }

  public static DBTableInfo getDBTableXynaInfoLeases() {

    String schema = "";
    String tablename = "";

    Properties wsProperties;
    try {
      logger.info("Getting Schema and Tablename for XYNA ...");
      wsProperties = PropertiesHandler.getWsProperties();
      tablename = PropertiesHandler.getProperty(wsProperties, XYNA_LEASES_TABLENAME, logger);
      schema = PropertiesHandler.getProperty(wsProperties, XYNA_LEASES_SCHEMA, logger);
      String xynaactive = PropertiesHandler.getProperty(wsProperties, XYNA_ACTIVE, logger);
      if(xynaactive.equalsIgnoreCase("true"))
      {
        xyna=true;
      }

    }
    catch (RemoteException e) {
      // TODO Auto-generated catch block
      logger.error("Problems getting configurable table/schema for leases webservice: ",e);
    }

        
    //DBTableInfo table = new DBTableInfo("leasesxynaandisc", "audit");
    DBTableInfo table = new DBTableInfo(tablename, schema);

    table.addColumn(new ColInfo("Host").setType(ColType.string).setVisible(true).setPk());
    table.addColumn(new ColInfo("Ip").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("IpNum").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("StartTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("EndTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Type").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("RemoteId").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("DppInstance").setType(ColType.string).setVisible(true).setUpdates(false));
    return table;
  }

  
  public static DBTableInfo getDBTableInfoLeasesCpe() {

    String schema = "";
    String tablename = "";

    Properties wsProperties;
    try {
      wsProperties = PropertiesHandler.getWsProperties();
      tablename = PropertiesHandler.getProperty(wsProperties, LEASESCPE_TABLENAME, logger);
      schema = PropertiesHandler.getProperty(wsProperties, LEASESCPE_SCHEMA, logger);
    }
    catch (RemoteException e) {
      // TODO Auto-generated catch block
      logger.error("Problems getting configurable table/schema for leases webservice: ",e);
    }

    
    //DBTableInfo table = new DBTableInfo("leasescpexynaandisc", "audit");
    DBTableInfo table = new DBTableInfo(tablename, schema);

    table.addColumn(new ColInfo("Host").setType(ColType.string).setVisible(true).setPk());
    table.addColumn(new ColInfo("Ip").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("IpNum").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("StartTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("EndTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Type").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("RemoteId").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("DppInstance").setType(ColType.string).setVisible(true).setUpdates(false));
    return table;
  }
  
  
  public static DBTableInfo getDBTableInfoLeasesv6() {
    DBTableInfo table = new DBTableInfo(LEASES_V6_TABLENAME, "auditv6memory");
    return addDefaultv6ColumnsToDBTableInfo(table);
  }
  
  
  public static DBTableInfo getDBTableInfoLeasesv6Prefixes() {
    DBTableInfo table = new DBTableInfo(LEASES_V6_PREFIX_TABLENAME, "auditv6memory");
    addDefaultv6ColumnsToDBTableInfo(table);
    table.addColumn(new ColInfo("Prefixlength").setType(ColType.integer).setVisible(true).setUpdates(false).setOptional());
    return table;
  }
  
  
  public static DBTableInfo getDBTableInfoLeasesv6Long() {
    DBTableInfo table = new DBTableInfo(LEASES_V6_LONG_TABLENAME, "auditv6memory");
    return addDefaultv6ColumnsToDBTableInfo(table);
  }
  
  
  public static DBTableInfo getDBTableInfoLeasesv6PrefixesLong() {
    DBTableInfo table = new DBTableInfo(LEASES_V6_PREFIX_LONG_TABLENAME, "auditv6memory");
    addDefaultv6ColumnsToDBTableInfo(table);
    table.addColumn(new ColInfo("Prefixlength").setType(ColType.integer).setVisible(true).setUpdates(false).setOptional());
    return table;
  }
  
  private static DBTableInfo addDefaultv6ColumnsToDBTableInfo(DBTableInfo table) {
    table.addColumn(new ColInfo("Host").setType(ColType.string).setVisible(true).setPk());
    table.addColumn(new ColInfo("Ip").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("IpNum").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("StartTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("EndTime").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("Duration").setType(ColType.integer).setVisible(false).setUpdates(false));
    table.addColumn(new ColInfo("Type").setType(ColType.string).setVisible(true).setUpdates(false));
    table.addColumn(new ColInfo("RemoteId").setType(ColType.string).setVisible(true).setUpdates(false).setOptional());
    table.addColumn(new ColInfo("DppInstance").setType(ColType.string).setVisible(true).setUpdates(false).setOptional());
    return table;
  }
  
    
  public List<T> searchLeases(T ref, String type, Logger logger, boolean leasesv6)
        throws java.rmi.RemoteException {
    if (leasesv6) {
      return searchLeasesv6(ref, type, logger);
    } else {
      return searchLeasesv4(ref, type, logger);
    }
  }
  
  
  private List<T> searchLeasesv6(T ref, String type, Logger logger) throws java.rmi.RemoteException {
    DBTableInfo table = _leasesv6Info;
    DBReader<T> reader = new ReflectionTools.DBReader<T>(table, ref, logger);
    SQLCommand builder = SQLBuilder.buildSQLSelectAll(table, false);
    logger.info("SearchLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Search Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    if (type.equals("MTA")) {
      builder.sql += " WHERE type LIKE '%pktc%' ";
    } else if (type.equals("CM")) {
      builder.sql += " WHERE type LIKE '%docsis%' ";
    } else if (type.equals("CPE")) {
      builder.sql += " WHERE NOT (type LIKE '%docsis%' OR type LIKE '%pktc%')";
    } else {
      logger.error("Wrong input in SearchLeasesRequest.");
      throw new DPPWebserviceIllegalArgumentException("Wrong input in SearchLeasesRequest.");
    }
    builder.sql += " LIMIT 999 ";
    List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);
    if (ret.size() > 1000) {
      return ret.subList(0, 999);
    }
    //ret = mergeLists(ret, new DBCommands<T>().query(reader, _leasesv6LongInfo, builder, logger));
    //if (ret.size() < 1000) {
      ret = mergeLists(ret, new DBCommands<T>().query(reader, _leasesv6PrefixInfo, builder, logger));
    /*  if (ret.size() < 1000) {
        ret = mergeLists(ret, new DBCommands<T>().query(reader, _leasesv6LongPrefixInfo, builder, logger));  
      }
    }*/
    return ret;
  }
  
  
  private List<T> searchLeasesv4(T ref, String type, Logger logger) throws java.rmi.RemoteException {
    logger.info("SearchLeasesv4 called ...");
    try {
      List<T> ret = null;
      List<T> retxyna = null;
      if(isc)
      {
        logger.info("ISC active, searching leases for ISC ...");
        if (type.equals("CPE")) {
          ret = getAllRows(ref, _leasesCpeInfo, logger);
        } else if ((type.equals("MTA")) || (type.equals("CM"))) {
          ret = searchLeasesWithCondition(ref, type, logger); 
        } else {
          throw new DPPWebserviceIllegalArgumentException("Illegal parameter for SearchLeases: " + type);
        }
      }
      if(xyna)
      {
        logger.info("XYNA active, searching leases for XYNA ...");
        if (type.equals("CPE")) {
          retxyna = searchForCPE(ref, _leasesXynaInfo, logger);
        } else if ((type.equals("MTA")) || (type.equals("CM"))) {
          retxyna = searchXynaLeasesWithCondition(ref, type, logger); 
        } else {
          throw new DPPWebserviceIllegalArgumentException("Illegal parameter for SearchLeases: " + type);
        }
      }
      
      ret = mergeLists(ret, retxyna);
      return ret;      
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in LeasesTools.searchLeases. ", e);
    }
  }
  
  
  private List<T> searchLeasesWithCondition(T ref, String type, Logger logger) throws RemoteException {
    logger.info("SearchLeasesWithCondition called ...");
    DBTableInfo table = _leasesInfo;
    DBReader<T> reader = new ReflectionTools.DBReader<T>(table, ref, logger);
    SQLCommand builder = SQLBuilder.buildSQLSelectAll(table, false);
    logger.info("SearchLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Search Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    if (type.equals("MTA")) {
      builder.sql += " WHERE type LIKE '%pktc%' ";
    } else if (type.equals("CM")) {
      builder.sql += " WHERE type LIKE '%docsis%' ";
    } else {
      logger.error("Wrong input in SearchLeasesRequest.");
      throw new DPPWebserviceIllegalArgumentException("Wrong input in SearchLeasesRequest.");
    }
    builder.sql += " LIMIT 999 ";
    List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);
    if (ret.size() > 1000) {
      return ret.subList(0, 999);
    }
    return ret;
  }

  private List<T> searchXynaLeasesWithCondition(T ref, String type, Logger logger) throws RemoteException {
    logger.info("SearchXynaLeasesWithCondition called ...");
    DBTableInfo table = _leasesXynaInfo;
    DBReader<T> reader = new ReflectionTools.DBReader<T>(table, ref, logger);
    SQLCommand builder = SQLBuilder.buildSQLSelectAll(table, false);
    logger.info("SearchLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Search Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    if (type.equals("MTA")) {
      builder.sql += " WHERE type LIKE '%pktc%' ";
    } else if (type.equals("CM")) {
      builder.sql += " WHERE type LIKE '%docsis%' ";
    } else {
      logger.error("Wrong input in SearchLeasesRequest.");
      throw new DPPWebserviceIllegalArgumentException("Wrong input in SearchLeasesRequest.");
    }
    builder.sql += " LIMIT 999 ";
    List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);
    if (ret.size() > 1000) {
      return ret.subList(0, 999);
    }
    return ret;
  }

  
  private String getTypeValue(T input) throws RemoteException {
    ReflectionTools<T> reftools = new ReflectionTools<T>(input); 
    Method getter = reftools.getStringGetter(Constants.leasesTypeXmlName);
    String ret = reftools.callStringGetter(input, getter);
    return ret;
  }
  
  private LeasesType translateLeasesType(String input) {
    if (input.indexOf("pktc") == 0) {
      return LeasesType.MTA;
    } else if (input.indexOf("docsis") == 0) {
      return LeasesType.CM;
    }
    return LeasesType.CPE;
  }
  
  public String getLocationValue(T input) throws RemoteException {
    String type = getTypeValue(input);
    if ((type == null) || (type.trim().equals(""))) {
      return Constants.managementName;
    }
    if (translateLeasesType(type) != LeasesType.CPE) {
      return Constants.managementName;
    }
    return "";
  }  
  

  public List<T> getAllRows(T ref, Logger logger, boolean leasesv6) 
          throws RemoteException {
    logger.info("getAllRows with leasesv6 = "+leasesv6+" called ...");

    if (leasesv6) {
      return getAllRowsv6(ref, logger);
    } else {
      return getAllRowsv4(ref, logger);
    }
  }
  
  private List<T> getAllRowsv4(T ref, Logger logger) throws RemoteException {
    logger.info("Getting all Leases (v4) ...");
    try {
      List<T> ret = null;
      List<T> xynaret = null;
      if(isc)
      {
        logger.info("ISC active, getting all leases for ISC (v4)");
        ret = getAllRows(ref, _leasesInfo, logger);
        if (ret.size() < 1000) {
          ret = mergeLists(ret, getAllRows(ref, _leasesCpeInfo, logger));
        }      
      }
      if(xyna)
      {
        logger.info("XYNA active, getting all leases for XYNA (v4)");
        xynaret = getAllRows(ref, _leasesXynaInfo, logger);
        ret = mergeLists(ret, xynaret);
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRows", e);
    }
  }
  
  
  private List<T> getAllRowsv6(T ref, Logger logger) throws RemoteException {
    try {
      List<T> ret = getAllRows(ref, _leasesv6Info, logger);
      if (ret.size() < 1000) {
        ret = mergeLists(ret, getAllRows(ref, _leasesv6PrefixInfo, logger));
        /*if (ret.size() < 1000) {
          ret = mergeLists(ret, getAllRows(ref, _leasesv6LongInfo, logger));
          if (ret.size() < 1000) {
            ret = mergeLists(ret, getAllRows(ref, _leasesv6LongPrefixInfo, logger));
          }
        }*/
      }      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRows", e);
    }
  }
      
  private List<T> getAllRows(T ref, DBTableInfo table, Logger logger) 
          throws RemoteException {
    logger.info("getAllRows called ...");

    try {
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, ref, logger);    
      List<T> ret = new DBCommandHandler<T>().getAllRows(reader, table, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in GetAllRows", e);
    }
  }
    
  
  public List<T> searchRows(T input, List<DBTableInfo> tabled, Logger logger) throws RemoteException {
    List<T> ret = new ArrayList<T>();
    try {
      for (DBTableInfo dbTableInfo : tabled) {
        mergeLists(ret, searchRows(input, dbTableInfo, logger)); 
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in searchRows", e);
    }
    return ret;
  }
  
  
  public List<T> searchRows(T input, String table, Logger logger) 
          throws RemoteException {
    try {
      List<T> ret = null;
      List<T> xynaret = null;
      if(isc)
      {
        logger.info("ISC active, getting all leases for ISC (v4)");
        if (table.trim().equals("leasescpe")) {
          ret = searchRows(input, _leasesCpeInfo, logger);
        } else if (table.trim().equals("leases")) {
          ret = searchRows(input, _leasesInfo, logger);
        } else if (table.trim().equals("both")) {      
          ret = searchRows(input, _leasesInfo, logger);
          if (ret.size() < 1000) {
            ret = mergeLists(ret, searchRows(input, _leasesCpeInfo, logger));
          }
        } else {
          throw new DPPWebserviceIllegalArgumentException("The parameter table must be one of the following values: "
              + "leases, leasescpe, both.");
        }
      }
      if(xyna)
      {
        logger.info("XYNA active, getting all leases for XYNA (v4)");
        if (table.trim().equals("leasescpe")) {
          xynaret = searchRowsForCPE(input, _leasesXynaInfo, logger);
        } else if (table.trim().equals("leases")) {
          xynaret = searchRows(input, _leasesXynaInfo, logger);
        } else if (table.trim().equals("both")) {      
          xynaret = searchRows(input, _leasesXynaInfo, logger);
        } else {
          throw new DPPWebserviceIllegalArgumentException("The parameter table must be one of the following values: "
              + "leases, leasescpe, both.");
        }
        //xynaret = searchRows(input, _leasesXynaInfo, logger);
      }
      ret = mergeLists(ret, xynaret);
      
      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }
    
  
  public List<T> searchLeasesForCPEs(T input, List<DBTableInfo> tables, Logger logger) throws RemoteException {
    List<T> ret = new ArrayList<T>();
    try {
      for (DBTableInfo dbTableInfo : tables) {
        mergeLists(ret, searchForCPE(input, dbTableInfo, logger));
        if (ret.size() >= 1000) {
          break;
        }
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }
  
  
  private List<T> searchForCPE(T input, DBTableInfo table, Logger logger) throws RemoteException {
    try {      
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);
      SQLCommand builder = SQLBuilder.buildSQLSelectWhere(new ReflectionTools<T>(input).getRowMap(table, input, logger), table);
      String command = builder.sql;
      StringBuilder modifiedCommand = new StringBuilder(command.substring(0, command.indexOf("LIMIT")));
      if (command.indexOf("WHERE") >= 0) {
        modifiedCommand.append("AND ");
      } else {
        modifiedCommand.append("WHERE ");
      }
      modifiedCommand.append("Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' ");
      modifiedCommand.append(command.substring(command.indexOf("LIMIT")));
      builder.sql = modifiedCommand.toString();
      logger.info("SearchForCPE in Xyna called, returning command: "+builder.sql);
      List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);   
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }

  public int countWithConditionsForCPEs(T input, List<DBTableInfo> tables, Logger logger) throws RemoteException {
    int ret = 0;
    try {
      for (DBTableInfo dbTableInfo : tables) {
        ret += countWithConditionForCPEs(input, logger, dbTableInfo);
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }
  
  
  private int countWithConditionForCPEs(T input, Logger logger, DBTableInfo table) throws RemoteException {
    SQLCommand builder = SQLBuilder.buildSQLCountStarWhere(new ReflectionTools<T>(input).getRowMap(table, input, logger), table);
    String command = builder.sql;
    StringBuilder modifiedCommand = new StringBuilder(command);
    if (command.indexOf("WHERE") >= 0) {
      modifiedCommand.append(" AND ");
    } else {
      modifiedCommand.append(" WHERE ");
    }
    modifiedCommand.append("Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' ");
    builder.sql = modifiedCommand.toString();
    DBSchema schema = ManagementData.translateDBSchemaName(table.getSchema(), logger);
    SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(schema, logger);
    return QueryTools.countRows(builder, utils, logger);
  }
  
  
  
    
  private List<T> searchRows(T input, DBTableInfo table, Logger logger) 
          throws RemoteException {
    try {      
      DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);
      TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
      List<T> ret = new DBCommandHandler<T>().searchRows(input, reader, table, map, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
    }
  }

  private List<T> searchRowsForCPE(T input, DBTableInfo table, Logger logger) 
                  throws RemoteException {
            try {      
              DBReader<T> reader = new ReflectionTools.DBReader<T>(table, input, logger);
              TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(table, input, logger);
              List<T> ret = new DBCommandHandler<T>().searchRowsForCPE(input, reader, table, map, logger);
              return ret;
            } catch (java.rmi.RemoteException e) {
              throw e;
            } catch (Exception e) {
              logger.error(e);
              throw new DPPWebserviceUnexpectedException("Error in SearchRows.", e);
            }
          }

  
  public List<T> mergeLists(List<T> ret, List<T> addlist) {
    if (ret == null) {
      ret = new ArrayList<T>();
    }
    if (addlist == null) {
      return ret; 
    }
    ret.addAll(addlist);
    if (ret.size() >= 1000) {
        ret = ret.subList(0,999);
    }
    return ret;
  }
  
  /**
   * count rows analog to getAllRows (but without limit)
   */
  public int countAllRows(Logger logger, boolean leasesv6) 
          throws RemoteException {
    if (leasesv6) {
      return countAllRowsv6(logger);
    } else {
      return countAllRowsv4(logger);
    }
    
  }
  
  
  private int countAllRowsv4(Logger logger) throws RemoteException {
    try {
      int ret = 0;
      if(isc)
      {
        ret += QueryTools.countAllRows(_leasesInfo, logger);
        ret += QueryTools.countAllRows(_leasesCpeInfo, logger);            
      }
      if(xyna)
      {
        ret += QueryTools.countAllRows(_leasesXynaInfo, logger);
      }
      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  private int countAllRowsv6(Logger logger) throws RemoteException {
    try {
      int ret = QueryTools.countAllRows(_leasesv6Info, logger);
      //ret += QueryTools.countAllRows(_leasesv6LongInfo, logger);
      ret += QueryTools.countAllRows(_leasesv6PrefixInfo, logger);
      //ret += QueryTools.countAllRows(_leasesv6LongPrefixInfo, logger);
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }


  /**
   * count rows analog to searchRows (but without limit)
   */
  public int countRowsWithCondition(T input, String table, Logger logger, boolean leasesv6) 
          throws RemoteException {
    if (leasesv6) {
      return countRowsWithConditionv6(input, table, logger);
    } else {
      return countRowsWithConditionv4(input, table, logger);
    }
  }
  
  
  private int countRowsWithConditionv4(T input, String table, Logger logger) throws RemoteException {
    logger.info("countRowsWithConditionv4 called ...");
    try {
      int ret = 0;      
      if(isc)
      {
        if (table.trim().equals("leasescpe")) {
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesCpeInfo, input, logger);
          ret = QueryTools.countRowsWithCondition(_leasesCpeInfo, map, logger);
        } else if (table.trim().equals("leases")) {
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesInfo, input, logger);
          ret = QueryTools.countRowsWithCondition(_leasesInfo, map, logger);
        } else if (table.trim().equals("both")) {      
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesCpeInfo, input, logger);
          ret = QueryTools.countRowsWithCondition(_leasesCpeInfo, map, logger);
          map = new ReflectionTools<T>(input).getRowMap(_leasesInfo, input, logger);
          ret += QueryTools.countRowsWithCondition(_leasesInfo, map, logger);
        } else {
          throw new DPPWebserviceIllegalArgumentException("The parameter table must be one of the following values: "
              + "leases, leasescpe, both.");
        }
      }
      if(xyna)
      {
        if (table.trim().equals("leasescpe")) {
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesXynaInfo, input, logger);
          ret += QueryTools.countCPERowsWithCondition(_leasesXynaInfo, map, logger);
        } else if (table.trim().equals("leases")) {
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesXynaInfo, input, logger);
          ret += QueryTools.countRowsWithCondition(_leasesXynaInfo, map, logger);
        } else if (table.trim().equals("both")) {      
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesXynaInfo, input, logger);
          ret += QueryTools.countRowsWithCondition(_leasesXynaInfo, map, logger);
        } else {
          throw new DPPWebserviceIllegalArgumentException("The parameter table must be one of the following values: "
              + "leases, leasescpe, both.");
        }

//        TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesXynaInfo, input, logger);
//        ret += QueryTools.countRowsWithCondition(_leasesXynaInfo, map, logger);
        
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  
  private int countRowsWithConditionv6(T input, String table, Logger logger) throws RemoteException {
    try {
      int ret = 0;      
      if (table.trim().equals(LEASES_V6_TABLENAME)) {
        TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesv6Info, input, logger);
        ret = QueryTools.countRowsWithCondition(_leasesv6Info, map, logger);
      } else if (table.trim().equals(LEASES_V6_PREFIX_TABLENAME)) {
          TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesv6PrefixInfo, input, logger);
          ret = QueryTools.countRowsWithCondition(_leasesv6PrefixInfo, map, logger);
      /* } else if (table.trim().equals(LEASES_V6_LONG_TABLENAME)) {
        TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesv6LongInfo, input, logger);
        ret = QueryTools.countRowsWithCondition(_leasesv6LongInfo, map, logger);
      } else if (table.trim().equals(LEASES_V6_PREFIX_LONG_TABLENAME)) {
        TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesv6LongPrefixInfo, input, logger);
        ret = QueryTools.countRowsWithCondition(_leasesv6LongPrefixInfo, map, logger);*/
      } else if (table.trim().equals("both")) {
        TreeMap<String, String> map = new ReflectionTools<T>(input).getRowMap(_leasesv6Info, input, logger);
        ret = QueryTools.countRowsWithCondition(_leasesv6Info, map, logger);
        map = new ReflectionTools<T>(input).getRowMap(_leasesv6PrefixInfo, input, logger);
        ret += QueryTools.countRowsWithCondition(_leasesv6PrefixInfo, map, logger);
        /*map = new ReflectionTools<T>(input).getRowMap(_leasesv6LongInfo, input, logger);
        ret += QueryTools.countRowsWithCondition(_leasesv6LongInfo, map, logger);
        map = new ReflectionTools<T>(input).getRowMap(_leasesv6LongPrefixInfo, input, logger);
        ret += QueryTools.countRowsWithCondition(_leasesv6LongPrefixInfo, map, logger);*/
      } else {
        throw new DPPWebserviceIllegalArgumentException("The parameter table must be one of the following values: "
            + "leases, leasescpe, both.");
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
  

  /**
   * count rows analog to searchLeases (but without limit)
   */
  public int countLeases(String type, Logger logger, boolean leasesv6) throws java.rmi.RemoteException {
    if (leasesv6) {
      return countLeasesv6(type, logger);
    } else {
      return countLeasesv4(type, logger);
    }
  }
  
  private int countLeasesv4(String type, Logger logger) throws java.rmi.RemoteException {
    logger.info("countLeasesv4 called ...");
    try {
      int ret = 0;
      if(isc)
      {
        if (type.equals("CPE")) {
          ret += QueryTools.countAllRows(_leasesCpeInfo, logger);
        } else if ((type.equals("MTA")) || (type.equals("CM"))) {
          ret += countLeasesWithCondition(type, logger); 
        } else {
          throw new DPPWebserviceIllegalArgumentException("Illegal parameter for countLeases: " + type);
        }
      }
      if(xyna)
      {
        if (type.equals("CPE")) {
          ret += countXynaCPELeases(type, logger);
        } else if ((type.equals("MTA")) || (type.equals("CM"))) {
          ret += countXynaLeasesWithCondition(type, logger); 
        } else {
          throw new DPPWebserviceIllegalArgumentException("Illegal parameter for countLeases: " + type);
        }
        //ret += QueryTools.countAllRows(_leasesXynaInfo, logger);
      }
      return ret;      
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in LeasesTools.countLeases. ", e);
    }
  }
  
  
  private int countLeasesv6(String type, Logger logger) throws java.rmi.RemoteException {
    try {
      int ret = 0;
      if (type.equals("CPE")) {
        ret = QueryTools.countAllRows(_leasesCpeInfo, logger);
      } else if ((type.equals("MTA")) || (type.equals("CM"))) {
        ret = countLeasesWithCondition(type, logger); 
      } else {
        throw new DPPWebserviceIllegalArgumentException("Illegal parameter for countLeases: " + type);
      }
      return ret;      
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in LeasesTools.countLeases. ", e);
    }
  }
  
  
  private int countLeasesWithCondition(String type, Logger logger) throws RemoteException {    
    DBTableInfo table = _leasesInfo;
    SQLCommand builder = SQLBuilder.buildSQLCountStar(table);
    logger.info("CountLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Count Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    if (type.equals("MTA")) {
      builder.sql += " WHERE type LIKE '%pktc%' ";
    } else if (type.equals("CM")) {
      builder.sql += " WHERE type LIKE '%docsis%' ";
    } else {
      logger.error("Wrong input in CountLeasesRequest.");
      throw new DPPWebserviceIllegalArgumentException("Wrong input in CountLeasesRequest.");
    }
    SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
    int ret = QueryTools.countRows(builder, utils, logger);    
    return ret;
  }
  
  private int countXynaLeasesWithCondition(String type, Logger logger) throws RemoteException {    
    DBTableInfo table = _leasesXynaInfo;
    SQLCommand builder = SQLBuilder.buildSQLCountStar(table);
    logger.info("CountLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Count Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    if (type.equals("MTA")) {
      builder.sql += " WHERE type LIKE '%pktc%' ";
    } else if (type.equals("CM")) {
      builder.sql += " WHERE type LIKE '%docsis%' ";
    } else {
      logger.error("Wrong input in CountLeasesRequest.");
      throw new DPPWebserviceIllegalArgumentException("Wrong input in CountLeasesRequest.");
    }
    SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
    int ret = QueryTools.countRows(builder, utils, logger);    
    return ret;
  }

  private int countXynaCPELeases(String type, Logger logger) throws RemoteException {    
    DBTableInfo table = _leasesXynaInfo;
    SQLCommand builder = SQLBuilder.buildSQLCountStar(table);
    logger.info("CountLeasesRequest->type = " + type);
    if (builder.sql.equals("")) {
      logger.error("Count Leases: Unable to build SQL Command.");
      throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
    }
    builder.sql += " WHERE Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' ";
    SQLUtilsContainerForManagement utils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
    int ret = QueryTools.countRows(builder, utils, logger);    
    return ret;
  }

  
  public int countRowsWithCondition(T input, List<DBTableInfo> tables, Logger logger) throws RemoteException {
    try {
      int ret = 0;
      TreeMap<String, String> map;
      for (DBTableInfo dbTableInfo : tables) {
        map = new ReflectionTools<T>(input).getRowMap(dbTableInfo, input, logger);
        ret += QueryTools.countRowsWithCondition(dbTableInfo, map, logger);
      }
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error.", e);
    }
  }
  
   
}