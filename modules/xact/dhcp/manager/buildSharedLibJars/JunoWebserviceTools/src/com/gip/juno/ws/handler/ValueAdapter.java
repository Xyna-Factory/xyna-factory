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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.IpMirrored;
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.enums.VirtualCol;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.AdditionalCheck;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.ColInfo.LOOKUP_ON;
import com.gip.juno.ws.tools.ColInfo.LookupQuery;
import com.gip.juno.ws.tools.ConditionalChecker;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.ForeignValueDataBuilder;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.juno.ws.tools.QueryTools.DBIntegerReader;
import com.gip.juno.ws.tools.WSTools;


/**
 * Class that adapts values going in or out of the database
 */
public class ValueAdapter {
  
  
  /**
   * returns value coming from the database, transformed into value to be sent to gui 
   */
  public static String getColValueDBToGui(ResultSet rs, DBTableInfo table, String colname, Logger logger) 
        throws SQLException {
    ColInfo col = table.getColumns().get(colname);
    if (col == null) {
      logger.error("DBTableInfo: Column name " + colname + " cannot be found!");
      throw new SQLException("DBTableInfo: Column name " + colname + " cannot be found!");
    }
    try {
      String colValue = WSTools.getColValue(rs, col.dbname);
      String schema = table.getSchema();
      return adaptValueFromDB(col, colValue, schema, logger);
    } catch (Exception e) {
      throw new SQLException(e);
    }
  }    
  
  public static String adaptValueFromDB(DBTableInfo table, String colname, String colValue, Logger logger)
        throws SQLException {    
    ColInfo col = table.getColumns().get(colname);
    if (col == null) {
      logger.error("DBTableInfo: Column name " + colname + " cannot be found!");
      throw new SQLException("DBTableInfo: Column name " + colname + " cannot be found!");
    }
    String schema = table.getSchema();
    return adaptValueFromDB(col, colValue, schema, logger);
  }  
  
  public static String adaptValueFromDB(ColInfo col, String colValue, String schema, Logger logger)
        throws SQLException {
    if (col == null) {
      throw new SQLException("Unable to adapt column value: Meta Info for column missing.");
    }
    if ((colValue == null) || (colValue.equals(""))) {
      return "";
    }
    String ret = "";
    try {
      if (!col.lookupCol.equals("")) {        
        ret = QueryTools.queryForeignValue(new ForeignValueDataBuilder().setSchema(schema)
            .setConditionCol(col.parentCol).setConditionVal(colValue).setValueCol(col.lookupCol)
            .setTable(col.parentTable).setLookupStyle(col.lookupStyle).build(), logger); 
      } else if (col.ipMirrored == IpMirrored.True) {
        ret = mirrorIp(colValue);
      } else {
        ret = colValue;
      }
      if (col.maskPassword) {
        ret = getMaskString(ret.length());
      }
    } catch (Exception e) {
      throw new SQLException(e);
    }
    ret = replaceControlChars(ret);
    return ret;
  }
  
  private static String getMaskString(int length) {
    return "********";
  }
  
  /**
   * adds to map the value coming from the Gui, transformed into value to be stored in database;
   * 
   */
  public static void setColValueGuiToDBInMap(Map<String, String> map, String colname, DBTableInfo table, 
        String colValGui, Logger logger) throws java.rmi.RemoteException {
    try {
      if ((colname == null) || (colname.trim().equals(""))) {
        throw new DPPWebserviceIllegalArgumentException("Column name missing.");
      }
      ColInfo col = table.getColumns().get(colname);      
      if (col == null) {
        logger.error("DBTableInfo: Column name " + colname + " cannot be found!");
        throw new DPPWebserviceIllegalArgumentException("DBTableInfo: Column name " + colname + " cannot be found!");
      }
      if (col.virtual == VirtualCol.True) {
        return;
      } else {
        String colValDB = adaptValueForDB(colname, col, colValGui, table.getSchema(), logger);
        map.put(colname, colValDB);
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceException("Error: ", e);
    } 
  }
     
  
  private static String adaptValueForDB(String colname, ColInfo col, String colValGui, String schema, 
          Logger logger) throws RemoteException {
    try {
      String colValDB = "";
      if (colValGui == null) {
        return "";
      } else {
        colValDB = decode(colValGui);
      } 
      if (!col.lookupCol.equals("")) {
        colValDB = QueryTools.queryForeignValue(new ForeignValueDataBuilder().setSchema(schema)
            .setConditionCol(col.lookupCol).setConditionVal(colValDB).setValueCol(col.parentCol)
            .setTable(col.parentTable).setLookupStyle(col.lookupStyle).build(), logger);
      } 
      if (col.ipMirrored == IpMirrored.True) {
        colValDB = mirrorIp(colValDB);
      } 
      if ((col.endsWithLinebreak) && (!colValDB.equals(""))) {
        if (!colValDB.endsWith("\n")) {
          colValDB = colValDB + "\n";
          System.out.println("val ends now with newline");
        } else {
          System.out.println("val did end with newline");
        }
      }
      if (col.doTrim) {
        colValDB = colValDB.trim();
      }
      if (col.removeSpaces) {
        colValDB = colValDB.replace(" ", "");
      }
      return colValDB;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error(e);
      throw new DPPWebserviceException("Error: ", e);
    }
  }

  

  public static void adjustColValuesForInsert(Map<String, String> map, DBTableInfo table, Logger logger) 
        throws RemoteException {
    adjustColValues(map, table, logger);
    checkColValuesForLookups(map, table, LOOKUP_ON.INSERTION, logger);
  }
  
  public static void adjustColValuesForUpdate(Map<String, String> map, DBTableInfo table, Logger logger) 
        throws RemoteException {
    adjustColValues(map, table, logger);
    checkColValuesForLookups(map, table, LOOKUP_ON.MODIFICATION, logger);
  }
  
  
  public static void checkUniqueForInsert(Map<String, String> map, DBTableInfo table, Logger logger) throws RemoteException {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String colname = entry.getKey();
      String val = entry.getValue();
      ColInfo col = table.getColumns().get(colname);
      if (col.checkUnique) {
        checkUnique(table, colname, val, logger);
      }   
    }
  }

  public static void checkUniqueForUpdate(TreeMap<String, String> map, DBTableInfo table, Logger logger) 
        throws RemoteException {
    checkUniqueForUpdate(map, map, table, true, true, logger);     
  }
  
  public static void checkUniqueForUpdatePk(TreeMap<String, String> oldvalmap, Map<String, String> newvalmap, 
      DBTableInfo table, Logger logger) throws RemoteException {
    checkUniqueForUpdate(oldvalmap, newvalmap, table, true, false, logger);
  }
  
  public static void checkUniqueForUpdatePkIgnoreEmpty(TreeMap<String, String> oldvalmap, 
      Map<String, String> newvalmap, DBTableInfo table, Logger logger) throws RemoteException {
    checkUniqueForUpdate(oldvalmap, newvalmap, table, false, false, logger);
  }

  /**
   * checks unique conditions, but only if new value for column is different than old one
   */
  public static void checkUniqueForUpdate(TreeMap<String, String> oldvalmap, Map<String, String> newvalmap, 
          DBTableInfo table, boolean insertEmpty, boolean onlyPkCondition, Logger logger) throws RemoteException {    
    for (Map.Entry<String, String> entry : newvalmap.entrySet()) {
      String colname = entry.getKey();
      ColInfo col = table.getColumns().get(colname);
      if (col.checkUnique) {
        String val = entry.getValue();
        boolean doCheck = true; 
        if (val == null) {
          val = "";
        }
        if (val.equals("")) {
          if (!insertEmpty) {
            doCheck = false;
          }
        }
        if (doCheck) {
          String oldval = "";
          if (onlyPkCondition) {
            oldval = QueryTools.selectColValueByPkColsCondition(table, colname, oldvalmap, logger);
          } else {
            oldval = QueryTools.selectColValueByNonEmptyColsCondition(table, colname, oldvalmap, logger);
          }
          if (oldval == null) {
            oldval = "";
          }
          if (!oldval.equals(val)) {  
            ValueAdapter.checkUnique(table, colname, val, logger);
          }
        }   
      }
    }
  }


  
  /**
   * check unique condition in column,
   * only for management tables
   */
  public static void checkUnique(DBTableInfo table, String colname, String val, Logger logger) 
        throws RemoteException {
    int howOften = QueryTools.countColValue(table, colname, val, logger);
    if (howOften > 0) {
      if (table.getTablename().equals("statichost")) {
        if (colname.equals("Ip")) {
          logger.error("Ip for StaticHost already exists (" + val + ")");
          throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription(
              "Ip for StaticHost already exists (" + val + ")").setErrorNumber("00207").addParameter(val));
        } else if (colname.equals("Cpe_mac")) {
          logger.error("Cpe_Mac for StaticHost already exists (" + val + ")");
          throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription(
              "Cpe_Mac for StaticHost already exists (" + val + ")").setErrorNumber("00208").addParameter(val));
        }            
      }
      logger.error("Value " + val + " already exists in column " + colname + ".");
      throw new DPPWebserviceDatabaseException(new MessageBuilder().setDescription(
          "Value " + val + " already exists in column " + colname + ".")
          .setErrorNumber("00002").addParameter(val));
    }  
  }
  
  public static void adjustColValues(Map<String, String> map, DBTableInfo table, Logger logger) 
        throws RemoteException {    
    for (Map.Entry<String, String> entry : map.entrySet()) {
      boolean checkSyntax = true;
      boolean checkNotNullConstraint = false;      
      String colname = entry.getKey();
      String val = entry.getValue();
      ColInfo col = table.getColumns().get(colname);
      if (val == null) {
        checkSyntax = false;
        checkNotNullConstraint = true;
        entry.setValue("");
        val = "";
      } else if (val.trim().equals("")) {
        checkSyntax = false;
        checkNotNullConstraint = true;
      }      
      if (checkNotNullConstraint) {        
        if (col.ifNoVal == IfNoVal.ConstraintViolation) {
          logger.error("Value for Column " + colname + " may not be empty.");
          throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription("Value for Column " 
              + colname + " may not be empty.").setErrorNumber("00203").addParameter(colname));
        }
      }      
      if (checkSyntax) {
        if (col.checkAttributeSyntax) {
          if (!checkAttributeSyntax(val, logger)) {
            logger.error("Attribute Syntax not correct for column " + colname
                + ", value = " + val);
            throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription(
                "Attribute Syntax not correct for column " + colname + ", value = " + val)
                .setErrorNumber("00201").addParameter(val));
          }
        }
        if (col.checkConditionalSyntax) {
          try {
            val = ConditionalChecker.adjust(val, logger, table.getSchema());
            entry.setValue(val);
          } catch (RemoteException e) {          
            throw e;
          }
        }
      }
      if (col.additionalChecks != null &&
          col.additionalChecks.length > 0) {
        entry.setValue(performAdditionalChecks(val, col, table, logger));
      }
    }
  }
  
  
  private static void checkColValuesForLookups(Map<String, String> map, DBTableInfo table, LOOKUP_ON now, Logger logger) 
        throws RemoteException {
    for (Entry<String, ColInfo> entry : table.getColumns().entrySet()) {
      LookupQuery lookup = entry.getValue().lookquery;
      if (lookup != null &&
          lookup.lookupExecutionAppropriate(now)) {
        SQLCommand query = lookup.getSQLCommand();
        SQLCommand clonedQuery = query.clone();
        clonedQuery.clearParams();
        for (String identifier : lookup.getLocalParameterIdentifiers()) {
          clonedQuery.addConditionParam(map.get(identifier));
        }
        String value = new DBCommands<String>().queryOneRow(new DBStringReader(), table, clonedQuery, logger);
        value = lookup.verify(value, entry.getValue());
        map.put(entry.getKey(), value);
      }
    }
  }
  
  
  private static boolean checkAttributeSyntax(String val, Logger logger) {
    logger.info("Checking attribute syntax of expression : " + val);
    if (val.matches("\\d+[:]?=<[^>]*>(,\\d+[:]?=<[^>]*?>)*")) {
      logger.info("Attribute syntax ok for expression: " + val);
      return true;
    }
    logger.info("Attribute syntax not correct for expression: " + val);
    return false;
  }
  
  private static String decode(String val) {
    String ret = val;
    ret = ret.replaceAll("&amp;", "&");
    ret = ret.replaceAll("&lt;", "<");
    ret = ret.replaceAll("&gt;", ">");
    return ret;
  }
  

  /**
   * returns mirrored IP
   */
  public static String mirrorIp(String ip) {
    String ret = "";
    //if (!Pattern.matches("\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b",
    if (ip.matches("\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")) {    
       String[] parts = ip.split("\\.");
       ret += parts[3];
       for (int i=2; i>=0; i--) {
         ret += ".";
         ret += parts[i];
       }
    } else {
      return ip;
    }
    return ret;
  }
  
  private static final String LIST_SEPERATION_MARKER_ATTRIBUTES = ",";
  private static final String END_ID_MARKER_ATTRIBUTES = "=";
  private static final String END_ID_MARKER_FIXED_ATTRIBUTES = ":";

  
  private static String performAdditionalChecks(String value, ColInfo columnInfo, DBTableInfo table, Logger logger) throws RemoteException {
    if (value == null ||
        value.length() == 0) {
      return null;
    }
    // FIXME currently a little to specfic for our existing checks
    String endMarker = null;
    TableHandler tableToCheck = null; 
    for (AdditionalCheck aCheck : columnInfo.additionalChecks) {
      switch (aCheck) {
        case checkAttributeExistence :
          endMarker = END_ID_MARKER_ATTRIBUTES;
          if (table.getSchema().equals("dhcp")) {
            tableToCheck = new com.gip.juno.ws.db.tables.dhcp.GuiAttributeHandler();
          } else {
            tableToCheck = new com.gip.juno.ws.db.tables.dhcpv6.GuiAttributeHandler();
          }
          break;
        case checkFixedAttributeExistence :
          endMarker = null;
          if (table.getSchema().equals("dhcp")) {
            tableToCheck = new com.gip.juno.ws.db.tables.dhcp.GuiFixedAttributeHandler();
          } else {
            tableToCheck = new com.gip.juno.ws.db.tables.dhcpv6.GuiFixedAttributeHandler();
          }
          break;
        default :
          throw new DPPWebserviceDatabaseException("Unknown additional check specified.");
      }
    }
    Map<Integer, String> idToValueMapping = parseIdsFromSeperatedList(value, LIST_SEPERATION_MARKER_ATTRIBUTES, endMarker);
    Set<Integer> ids = idToValueMapping.keySet();
    Collection<Integer> existingIds = returnExistingIds(ids, tableToCheck.getDBTableInfo(), logger);
    if (ids.retainAll(existingIds)) {
      if (idToValueMapping.size() == 0) {
        return null;
      } else {
        return convertToSeperatedList(idToValueMapping.values(), LIST_SEPERATION_MARKER_ATTRIBUTES);
      }
    } else {
      return value;
    }
  }
  
  static Pattern idPattern = Pattern.compile("([0-9]+)[:]?=.*?>(?=(,|$))");
  
  private static final Map<Integer, String> parseIdsFromSeperatedList(String list, String seperationMarker, String idEndMarker) {
    Map<Integer, String> map = new HashMap<Integer, String>();
    if (idEndMarker != null) {
      Matcher matcher = idPattern.matcher(list);
      while (matcher.find()) {
        map.put(Integer.parseInt(matcher.group(1)), matcher.group(0));
      }
    } else {
      String[] split = list.split(seperationMarker);
      for (String string : split) {
        int id;
        string = string.trim();
        if (string.endsWith(END_ID_MARKER_FIXED_ATTRIBUTES)) {
          id = Integer.parseInt(string.substring(0, string.length() - 1));
        } else {
          id = Integer.parseInt(string);
        }
        map.put(id, string);
      }
    }
    return map;
  }
  
  
  public static void main(String... args) {
    //String list = "1=<604800>,2=<691200>,4=<172.30.51.6,172.30.52.6>,5=<172.30.248.4>,6=<concat(suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,1,1))),2),suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,2,1))),2),suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,3,1))),2),suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,4,1))),2),suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,5,1))),2),suffix(concat(\"0\",binary-to-ascii(16,8,\"\",substring(hardware,6,1))),2))>";
    String list = "6:,7:";
    System.out.println(parseIdsFromSeperatedList(list, LIST_SEPERATION_MARKER_ATTRIBUTES, null));
    //System.out.println(parseIdsFromSeperatedList(list, LIST_SEPERATION_MARKER_ATTRIBUTES, null));
  }
  
  
  
  private static Collection<Integer> returnExistingIds(Collection<Integer> ids, DBTableInfo tableToCheck, Logger logger) throws RemoteException {
    if (ids.size() <= 0) {
      return Collections.emptyList();
    }
    String primaryKeyColumn = retrieveFirstPKColumnName(tableToCheck);
    String primaryKeyList = convertToSeperatedList(ids, ",");
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("SELECT ").append(primaryKeyColumn).append(" FROM ")
                  .append(tableToCheck.getSchema()).append('.').append(tableToCheck.getTablename())
                  .append(" WHERE ").append(primaryKeyColumn).append(" IN (").append(primaryKeyList).append(")");
    SQLCommand query = new SQLCommand();
    query.sql = commandBuilder.toString();
    return new DBCommands<Integer>().query(new DBIntegerReader(), tableToCheck, query, logger);
                  
  }
  
  
  private static String retrieveFirstPKColumnName(DBTableInfo dbInfo) throws RemoteException {
    for (Entry<String, ColInfo> entry : dbInfo.getColumns().entrySet()) {
      if (entry.getValue().pk == Pk.True) {
        return entry.getKey();
      }
    }
    throw new DPPWebserviceDatabaseException("Table without PrimaryKey!");
  }
  
  
  private static String convertToSeperatedList(Collection<?> values, String seperationMarker) {
    StringBuilder listBuilder = new StringBuilder();
    Iterator<?> iterator = values.iterator();
    while (iterator.hasNext()) {
      listBuilder.append(iterator.next());
      if (iterator.hasNext()) {
        listBuilder.append(seperationMarker);
      }
    }
    return listBuilder.toString();
  }
  
  static {
    initArray();
  }
  
  private static final int CONTROL_CHAR_MAPPING = 256;
  private static boolean[] isControlChar;
  
  static void initArray() {
    isControlChar = new boolean[CONTROL_CHAR_MAPPING];
    for (int ch = 0; ch <CONTROL_CHAR_MAPPING; ch++) {
      // ignore all except TAB, LF and CR that are < than SPACE
      // ignore DEL and all characters < NO-BREAK-SPACE except NEXT-LINE
      if((ch < 0x20 && ch != 0x09 && ch != 0x0A && ch != 0x0D) ||
                      (ch >= 0x7F && ch <= 0x9F && ch != 0x85)) {
        isControlChar[ch] = true;
      }
    }
  }
  

  public static String replaceControlChars(String string) {
    final int length = string.length();
    char[] m_charsBuff = new char[length];
    string.getChars(0, length, m_charsBuff, 0);

    for (int i = 0; i < length; i++) {
      if (m_charsBuff[i] <= CONTROL_CHAR_MAPPING && isControlChar[m_charsBuff[i]]) {
        m_charsBuff[i] = 0x23;
      }
    }

    return new String(m_charsBuff);
  }
  
}
