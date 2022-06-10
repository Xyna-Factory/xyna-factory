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
package com.gip.juno.ws.tools.multiuser;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceManipulationDeniedException;
import com.gip.juno.ws.exceptions.DPPWebserviceModificationCollisionException;
import com.gip.juno.ws.handler.ReflectionTools;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBLongReader;


public class MultiUserTools {
  
  public final static String ENTRY_MODIFICATION_TIMESTAMP_COLUMN = "modificationTimestamp";
  private final static String GUISESSIONSTATE_SCHEMA_NAME = "aaa";
  private final static String GUISESSIONSTATE_TABLE_NAME = "guisessionstate";
  private final static String GUISESSIONSTATE_SESSIONID_COLUMN_NAME = "sessionid";
  private final static String GUISESSIONSTATE_USERNAME_COLUMN_NAME = "username";
  private final static String GUISESSIONSTATE_TABLE_RETRIEVAL_COLUMN_PREFIX = "";
  private final static String GUISESSIONSTATE_TABLE_RETRIEVAL_COLUMN_SUFFIX = "RetrievalTimestamp";
  private final static String WHERE_CLAUSE_BEGINNING = " WHERE ";
  
  public final static Logger nop_logger = new Logger("DPPWebserviceModificationCollisionException") {
    
    public void addAppender(Appender appender) { }
    
    public void callAppenders(LoggingEvent event) { }
    
    public void assertLog(boolean assertion, String msg) { }
    
    protected Object clone() throws CloneNotSupportedException {
      return this;
    }
    
    public void debug(Object message) { }
    
    public void debug(Object message, Throwable t) { }
    
    public void error(Object message) { }
    
    public void error(Object message, Throwable t) { }
    
    public void fatal(Object message) { }
    
    public void fatal(Object message, Throwable t) { }
    
    public void forcedLog(String fqcn, Priority level, Object message, Throwable t) { }
    
    public boolean getAdditivity() {
      return true;
    }
    
    public Enumeration getAllAppenders() {
      return new Enumeration() {

        public boolean hasMoreElements() {
          return false;
        }

        public Object nextElement() {
          return null;
        }
      };
    }
    
    public Appender getAppender(String name) {
      return null;
    }
    
    public ResourceBundle getResourceBundle() {
      return null;
    }
    
    public void info(Object message) { }
    
    public void info(Object message, Throwable t) { }
    
    public boolean isAttached(Appender appender) {
      return true;
    }
    
    public boolean isDebugEnabled() {
      return false;
    }
    
    public boolean isEnabledFor(Priority level) {
      return true;
    }
    
    public boolean isErrorEnabled() {
      return false;
    }
    
    public boolean isFatalEnabled() {
      return false;
    }
    
    public boolean isInfoEnabled() {
      return false;
    }
    
    public boolean isTraceEnabled() {
      return false;
    }
    
    public boolean isWarnEnabled() {
      return false;
    }
    
    public void l7dlog(Priority arg0, String arg1, Object[] arg2, Throwable arg3) { }
    
    public void l7dlog(Priority arg0, String arg1, Throwable arg2) { }
    
    public void log(Priority arg0, Object arg1) { }
    
    public void log(Priority arg0, Object arg1, Throwable arg2) { }
    
    public void log(String arg0, Priority arg1, Object arg2, Throwable arg3) { }
    
    public void removeAllAppenders() { }
    
    public void removeAppender(Appender appender) { }
    
    public void removeAppender(String name) { }
    
    public void setAdditivity(boolean additivity) { }
    
    public void setLevel(Level level) { }
    
    public void setPriority(Priority priority) { }
    
    public void setResourceBundle(ResourceBundle bundle) { }
    
    public void trace(Object message) { }
    
    public void trace(Object message, Throwable t) { }
    
    public void warn(Object message) { }
    
    public void warn(Object message, Throwable t) { };
    
  };

  

  public static SQLCommand appendModifierSubselectAndModificationUpdate(SQLCommand unadjustedCommand, DBTableInfo table, String sessionid) throws RemoteException {
    // FIXME do we need to parse anything...how do our commands look at this point
    // is there always an where ID = ? or might they be terminated by anything different then an a where-clause
    // FIXME this could only be used if both tables are actually on the same server
    /*String guiSessionStateColum = generateGuiSessionStateColumnForTable(table);
    SQLCommand adjustedCommand = unadjustedCommand.clone();
    int insertionIndex = unadjustedCommand.sql.indexOf(WHERE_CLAUSE_BEGINNING);
    StringBuilder commandBuilder = new StringBuilder(unadjustedCommand.sql.substring(0, insertionIndex));
    commandBuilder.append(", ")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                  .append("=")
                  .append(System.currentTimeMillis())
                  .append(unadjustedCommand.sql.substring(insertionIndex))
                  .append(" AND ")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                  .append(" <= (SELECT ").append(guiSessionStateColum).append(" from ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME)
                  .append(" WHERE ")
                  .append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append("='").append(sessionid)
                  .append("')");
    adjustedCommand.sql = commandBuilder.toString();
    return adjustedCommand;*/
    return appendModifierSelectAndSelectRetrievalTimeStamp(unadjustedCommand, table, sessionid);
  }
  
  public static SQLCommand appendModifierSelectAndSelectRetrievalTimeStamp(SQLCommand unadjustedCommand, DBTableInfo table, String sessionid) throws RemoteException {
    return appendModifierSelectAndSelectRetrievalTimeStamp(unadjustedCommand, table, sessionid, null);
  }

  
  public static SQLCommand appendModifierSelectAndSelectRetrievalTimeStamp(SQLCommand unadjustedCommand, DBTableInfo table, String sessionid, Long timestampUdate) throws RemoteException {
    String guiSessionStateColum = generateGuiSessionStateColumnForTable(table);
    SQLCommand queryRetrievalTimestamp = new SQLCommand();
    StringBuilder commandBuilder = new StringBuilder("SELECT ").append(guiSessionStateColum).append(" from ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME)
                  .append(" WHERE ")
                  .append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append("='").append(sessionid).append("'");
    queryRetrievalTimestamp.sql = commandBuilder.toString();
    DBCommands<Long> dbCommandsIni = new DBCommands<Long>();
    List<Long> timestamps = dbCommandsIni.query(new DBLongReader(), DBSchema.aaa, queryRetrievalTimestamp, nop_logger);
    Long timestamp;
    if (timestamps.size() > 0 && timestamps.size()!= 1) {
      timestamp = timestamps.get(0);
    } else if (timestamps.size() == 1) {
      timestamp = timestamps.get(0);
    } else {
      timestamp = 0l;
    }
    if (timestampUdate == null) {
      return appendModificationTimestampUpdate(unadjustedCommand, timestamp, System.currentTimeMillis());
    } else {
      return appendModificationTimestampUpdate(unadjustedCommand, timestamp, timestampUdate);
    }
  }
  
  
  public static SQLCommand appendModificationTimestampUpdate(SQLCommand unadjustedCommand, Long ownTimestamp, Long timeStampUpdate) throws RemoteException {
    SQLCommand adjustedCommand = unadjustedCommand.clone();
    int insertionIndex = unadjustedCommand.sql.indexOf(WHERE_CLAUSE_BEGINNING);
    StringBuilder commandBuilder = new StringBuilder(unadjustedCommand.sql.substring(0, insertionIndex));
    commandBuilder.append(", ")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                  .append("=")
                  .append(timeStampUpdate)
                  .append(unadjustedCommand.sql.substring(insertionIndex));
    if (ownTimestamp != Long.MAX_VALUE) {
      commandBuilder.append(" AND ")
                    .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                    .append(" <= ").append(ownTimestamp);
    }
    adjustedCommand.sql = commandBuilder.toString();
    return adjustedCommand;
  }
  

  public static SQLCommand appendTimestampIntoInsertionCommand(SQLCommand unadjustedCommand, Long timestamp) {
    SQLCommand adjustedCommand = unadjustedCommand.clone();
    int columnInsertionIndex = unadjustedCommand.sql.indexOf(")");
    int valueInsertionIndex = unadjustedCommand.sql.lastIndexOf(")");
    StringBuilder commandBuilder = new StringBuilder(unadjustedCommand.sql.substring(0, columnInsertionIndex));
    commandBuilder.append(", ")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                  .append(unadjustedCommand.sql.substring(columnInsertionIndex, valueInsertionIndex))
                  .append(", ")
                  .append(timestamp)
                  .append(unadjustedCommand.sql.substring(valueInsertionIndex));
    adjustedCommand.sql = commandBuilder.toString();
    return adjustedCommand;
  }

  
  public static SQLCommand checkGlobalModification(SQLCommand unadjustedCommand, TableHandler table, String sessionid) throws RemoteException {
    //String guiSessionStateColum = generateGuiSessionStateColumnForTable(table.getDBTableInfo());
    
    //SQLCommand checkGlobalLockQuery = new SQLCommand();
    //select (SELECT MAX(modificationTimestamp) from service.text_config_template) <= (select service_text_config_templateRetrievalTimestamp from aaa.guisessionstate where aaa.guisessionstate.sessionid='0480618168.001323355406542') from dual;
    /*StringBuilder commandBuilder = new StringBuilder("SELECT (SELECT MAX(")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN).append(") FROM ").append(table.getDBTableInfo().getSchema())*/
    
    return unadjustedCommand;
  }
  
  
  public static <T> void handleObjectNotFoundOnModification(String sessionid, T collisionObject, DBTableInfo table) throws InstantiationException, IllegalAccessException, RemoteException {
    ReflectionTools<T> tools = new ReflectionTools<T>(collisionObject);
    TreeMap<String, String> primaryKeyMap = tools.getRowMapPkColsOnly(table, collisionObject, nop_logger);
    if (isModifiedAtEndOfTime(primaryKeyMap, table, nop_logger)) {
      throw new DPPWebserviceManipulationDeniedException();
    } else {
      SQLCommand command = SQLBuilder.buildSQLSelectAllWhere(primaryKeyMap, table, true);
      List<T> result = new DBCommands<T>().query(new ReflectionTools.DBReader<T>(table, collisionObject, nop_logger), table, command, nop_logger);
      if (result.size() == 1) {
        SQLCommand retrievalUpdate = generateTableRetrievalTimestampUpdate(table, sessionid);
        DBCommands.executeDML(DBSchema.aaa, retrievalUpdate, nop_logger);
        throw new DPPWebserviceModificationCollisionException(result.get(0), table);
      } else {
        throw new DPPWebserviceException("Tried to modify an object that has already been deleted.");
      }
    }
  }
  
  
  public static boolean isModifiedAtEndOfTime(TreeMap<String, String> primaryKeyMap, DBTableInfo table, Logger logger) throws RemoteException {
    SQLCommand unadjustedQuery = SQLBuilder.buildSQLCountStarWhere(primaryKeyMap, table);
    StringBuilder queryBuilder = new StringBuilder(unadjustedQuery.sql);
    queryBuilder.append(" AND ")
                .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                .append(" = ")
                .append(Long.MAX_VALUE);
    SQLCommand queryObjectAtEndOfTime = unadjustedQuery.clone();
    queryObjectAtEndOfTime.sql = queryBuilder.toString();
    String result = new DBCommands<String>().queryOneRow(new DBStringReader(), table, queryObjectAtEndOfTime, logger);
    int ret = Integer.parseInt(result);
    return ret > 0;
  }
  
  
  public static void handleObjectFoundOnModification(DBTableInfo table) throws InstantiationException, IllegalAccessException, RemoteException {
    if (table.needsGlobalLocking()) {
      SQLCommand updateModificationTimestamps = new SQLCommand();
      StringBuilder commandBuidler = new StringBuilder();
      commandBuidler.append("UPDATE ").append(table.getSchema()).append(".").append(table.getTablename())
                    .append(" SET ").append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN).append("=").append(System.currentTimeMillis());
      updateModificationTimestamps.sql = commandBuidler.toString();
      DBCommands.executeDML(table, updateModificationTimestamps, nop_logger);
    }
  }
  
  
  public static SQLCommand generateTableRetrievalTimestampUpdate(DBTableInfo table, String sessionid) {
    String guiSessionStateColum = generateGuiSessionStateColumnForTable(table);
    SQLCommand updateCommand = new SQLCommand();
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("UPDATE ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME)
                  .append(" SET ").append(guiSessionStateColum).append("='").append(System.currentTimeMillis())
                  .append("' WHERE ").append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append(" ='").append(sessionid).append("'");
    updateCommand.sql = commandBuilder.toString();
    return updateCommand;
  }
  
  
  public static SQLCommand generateTableRetrievalTimestampUpdateForMultipleColumns(List<TableHandler> tables, String sessionid) {
    List<String> columnsForUpdate = new ArrayList<String>();
    for (TableHandler tableHandler : tables) {
      if (tableHandler.supportsCollisionDetection()) {
        columnsForUpdate.add(generateGuiSessionStateColumnForTable(tableHandler.getDBTableInfo()));
      }
    }

    long timestamp = System.currentTimeMillis();
    SQLCommand updateCommand = new SQLCommand();
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("UPDATE ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME)
                  .append(" SET ");
    Iterator<String> iterator = columnsForUpdate.listIterator();
    while (iterator.hasNext()) {
      String column = iterator.next();
      commandBuilder.append(column).append("='").append(timestamp).append('\'');
      if (iterator.hasNext()) {
        commandBuilder.append(", ");
      }
    }
    commandBuilder.append(" WHERE ").append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append(" ='").append(sessionid).append("'");
    updateCommand.sql = commandBuilder.toString();
    return updateCommand;
  }
  
  
  public static SQLCommand generateInsertGuiSession(String username, String sessionid) {
    SQLCommand updateCommand = new SQLCommand();
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("INSERT INTO ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME)
                  .append(" (").append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append(',').append(GUISESSIONSTATE_USERNAME_COLUMN_NAME)
                  .append(") VALUES ('").append(sessionid).append("\',\'").append(username).append("')");
    updateCommand.sql = commandBuilder.toString();
    return updateCommand;
  }
  
  
  private static String generateGuiSessionStateColumnForTable(DBTableInfo table) {
    return generateGuiSessionStateColumnForTable(table.getSchema(), table.getTablename());
  }
  
  private final static Pattern NEGATION_OF_ALLOWED_SIGNS = Pattern.compile("[^a-zA-Z0-9_-]");
  
  private static String generateGuiSessionStateColumnForTable(String schemaname, String tablename) {
    String cleanSchemaName = NEGATION_OF_ALLOWED_SIGNS.matcher(schemaname).replaceAll("");
    String cleanTableName = NEGATION_OF_ALLOWED_SIGNS.matcher(tablename).replaceAll("");
    return new StringBuilder(GUISESSIONSTATE_TABLE_RETRIEVAL_COLUMN_PREFIX)
                  .append(cleanSchemaName)
                  .append('_')
                  .append(cleanTableName)
                  .append(GUISESSIONSTATE_TABLE_RETRIEVAL_COLUMN_SUFFIX).toString();
  }
  
  
  public static String lookupUserForSession(String sessionId) throws RemoteException {
    SQLCommand query = new SQLCommand();
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("SELECT ").append(GUISESSIONSTATE_USERNAME_COLUMN_NAME).append(" FROM ")
                  .append(GUISESSIONSTATE_SCHEMA_NAME).append('.').append(GUISESSIONSTATE_TABLE_NAME).append(" WHERE ")
                  .append(GUISESSIONSTATE_SESSIONID_COLUMN_NAME).append("=\'").append(sessionId).append('\'');
    query.sql = commandBuilder.toString();
    List<String> usernames = new DBCommands<String>().query(new DBStringReader(), DBSchema.aaa, query, nop_logger);
    return usernames.get(0);
  }
  
  
  //Deletion
  public static SQLCommand appendNotModifiedAtEndOfTimeToDeletion(SQLCommand unadjustedCommand, DBTableInfo table) {
    
    StringBuilder commandBuilder = new StringBuilder(unadjustedCommand.sql);
    commandBuilder.append(" AND NOT ")
                  .append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN)
                  .append(" = ")
                  .append(Long.MAX_VALUE);
    SQLCommand adjustedCommand = unadjustedCommand.clone();
    adjustedCommand.sql = commandBuilder.toString();
    return adjustedCommand;
  }
  
  public static <T> void handleObjectNotFoundOnDeletion(String sessionid, T collisionObject, DBTableInfo table) throws InstantiationException, IllegalAccessException, RemoteException {
    ReflectionTools<T> tools = new ReflectionTools<T>(collisionObject);
    TreeMap<String, String> primaryKeyMap = tools.getRowMapPkColsOnly(table, collisionObject, nop_logger);
    if (isModifiedAtEndOfTime(primaryKeyMap, table, nop_logger)) {
      throw new DPPWebserviceManipulationDeniedException();
    } else {
      SQLCommand command = SQLBuilder.buildSQLSelectAllWhere(primaryKeyMap, table, true);
      List<T> result = new DBCommands<T>().query(new ReflectionTools.DBReader<T>(table, collisionObject, nop_logger), table, command, nop_logger);
      if (result.size() == 1) {
        SQLCommand retrievalUpdate = generateTableRetrievalTimestampUpdate(table, sessionid);
        DBCommands.executeDML(DBSchema.aaa, retrievalUpdate, nop_logger);
        throw new DPPWebserviceModificationCollisionException(result.get(0), table);
      } else {
        throw new DPPWebserviceException("Tried to delete an object that has already been deleted.");
      }
    }
  }
  
  public static void handleObjectFoundOnDeletion(DBTableInfo table) throws InstantiationException, IllegalAccessException, RemoteException {
    if (table.needsGlobalLocking()) {
      SQLCommand updateModificationTimestamps = new SQLCommand();
      StringBuilder commandBuidler = new StringBuilder();
      commandBuidler.append("UPDATE ").append(table.getSchema()).append(".").append(table.getTablename())
                    .append(" SET ").append(ENTRY_MODIFICATION_TIMESTAMP_COLUMN).append("=").append(System.currentTimeMillis());
      updateModificationTimestamps.sql = commandBuidler.toString();
      DBCommands.executeDML(table, updateModificationTimestamps, nop_logger);
    }
  }
  
  
  private final static int GLOBALLOCK_RETRY_COUNT = 100;
  private final static long GLOBALLOCK_BACKOFF_TIME = 250;
  
  public static abstract class GlobalLockRetryAction<O> {
    
    public O executeRetryAction() throws RemoteException {
      int retries = 0;
      while (retries <= GLOBALLOCK_RETRY_COUNT) {
        try {
          return performAction();
        } catch (DPPWebserviceManipulationDeniedException e) {
          try {
            Thread.sleep(GLOBALLOCK_BACKOFF_TIME);
          } catch (InterruptedException e1) {
            throw new DPPWebserviceException("Got interrupted while waiting for Global_Device-Lock.",e1);
          }
        }
        retries++;
      }
      throw new DPPWebserviceException("Invocation timed out while waiting for Global_Device-Lock.");
    }
    
    public abstract O performAction() throws RemoteException;
    
  }
 
}
