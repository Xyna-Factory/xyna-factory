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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;  
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.DPPWebserviceManipulationDeniedException;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.Constants;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.LocationTools;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.juno.ws.tools.LocationTools.LocationsRow;
import com.gip.juno.ws.tools.SQLUtilsContainerForLocation;
import com.gip.juno.ws.tools.SQLUtilsContainerForManagement;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.xyna.utils.db.ResultSetReader;


public abstract class PropagationAction<O, R> {
  
  private final static Logger propagationLogger = Logger.getLogger(PropagationAction.class);
  
  private final O inputObject;
  protected final TreeMap<String, String> inputValues;
  private final TreeMap<String, String> primaryKeyMap;
  protected TreeMap<String, String> backupValues;
  private final DBTableInfo table;
  private final WebServiceInvocationIdentifier webServiceInvocationIdentifier;
  protected final ResultSetReader<O> reader;
  private final PropagationActionIdentifier action;
  protected final Logger logger;
  
  PropagationAction(O inputObject, TreeMap<String, String> inputValues, ResultSetReader<O> reader, DBTableInfo table, PropagationActionIdentifier action, WebServiceInvocationIdentifier wsid, Logger logger) {
    this.inputObject = inputObject;
    this.inputValues = inputValues;
    this.table = table;
    this.reader = reader;
    this.action = action;
    this.webServiceInvocationIdentifier = wsid;
    this.logger = logger;
    primaryKeyMap = pruneToPrimaryKeys(inputValues, table);
  }
  
  public R executePropagationAction() throws RemoteException {
    lockManagementData(webServiceInvocationIdentifier);
    try {
      backupValues = retrieveBackup(primaryKeyMap);
      SQLUtilsContainer sqlUtils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
      ensureNoOtherPropagationEntryExists(primaryKeyMap);
      R result = executeAction(inputValues, table, sqlUtils);
      insertPropagationEntry(backupValues, action);
      propagateActionOnDppPairs();
      return result;
    } finally { 
      unlockManagementData();
    }
  }
  
  
  protected abstract R executeAction(TreeMap<String, String> inputValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException;
  
  protected abstract void executeRollback(TreeMap<String, String> backupValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException;
  
  protected void lockManagementData(WebServiceInvocationIdentifier wsid) throws RemoteException {
    TreeMap<String, String> updateValues = new TreeMap<String, String>(primaryKeyMap);
    StringBuilder lockBuilder = new StringBuilder();
    lockBuilder.append("UPDATE ")
               .append(table.getSchema())
               .append('.')
               .append(table.getTablename())
               .append(" SET WHERE");
    SQLCommand lockingUpdate = new SQLCommand();
    lockingUpdate.sql = lockBuilder.toString(); 
    SQLBuilder.addSQLPkColsCondition(updateValues, table, lockingUpdate);
    if (wsid.getSessionId() != null) {
      lockingUpdate = MultiUserTools.appendModifierSelectAndSelectRetrievalTimeStamp(lockingUpdate, table, wsid.getSessionId(), Long.MAX_VALUE);
    } else {
      lockingUpdate = MultiUserTools.appendModificationTimestampUpdate(lockingUpdate, Long.MAX_VALUE, Long.MAX_VALUE);
    }
    int index = lockingUpdate.sql.indexOf("SET,");
    if (index >= 0) {
      lockingUpdate.sql = lockingUpdate.sql.substring(0, index+3) + lockingUpdate.sql.substring(index+4);
    }
    int modifiedRows = DBCommands.executeDML(table, lockingUpdate, logger);
    try {
      if (modifiedRows == 0) {
        try {
          MultiUserTools.handleObjectNotFoundOnModification(wsid.getSessionId(), inputObject, table);
        } catch (DPPWebserviceException e) {
          handleDPPWebServiceExceptionOnObjectNotFoundForLocking(e);
        }
      } else {
        MultiUserTools.handleObjectFoundOnModification(table);
      }
    } catch (RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
  protected void handleDPPWebServiceExceptionOnObjectNotFoundForLocking(DPPWebserviceException e) throws DPPWebserviceException {
    throw e;
  }
  
  protected TreeMap<String, String> retrieveBackup(TreeMap<String, String> primaryKeyMap) throws RemoteException {
    SQLCommand queryPrevious = SQLBuilder.buildSQLSelectAllWhere(primaryKeyMap, table, true);
    O backupObject = new DBCommands<O>().queryOneRow(reader, table, queryPrevious, logger);
    ReflectionTools<O> tools = new ReflectionTools<O>(backupObject);
    return tools.getRowMap(table, backupObject, logger);
  }
  
  protected void insertPropagationEntry(TreeMap<String, String> values, PropagationActionIdentifier actionIdentifier) throws RemoteException {
    try {
      TreeMap<String, String> enrichedValues = new TreeMap<String, String>(values);
      enrichedValues.put(table.getPropagationHandler().getPropagationActionColumnName(),
                         actionIdentifier.getActionIdentifier());
      SQLCommand insertNew = SQLBuilder.buildSQLInsert(enrichedValues, table.getPropagationHandler().getPropagationDBTableInfo());
      DBCommands.executeDML(table.getPropagationHandler().getPropagationDBTableInfo(), insertNew, logger);
    } catch (Throwable t) {
      SQLUtilsContainer sqlUtils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
      executeRollback(backupValues, table, sqlUtils);
      throwAsDPPWebserviceException(t);
    }
  }
  
  protected void ensureNoOtherPropagationEntryExists(TreeMap<String, String> primaryKeyMap) throws RemoteException {
    SQLCommand countPropagationEntriesWithPk = SQLBuilder.buildSQLCountStarWhere(primaryKeyMap, table.getPropagationHandler().getPropagationDBTableInfo());
    Integer count = new DBCommands<Integer>().queryOneRow(new QueryTools.DBIntegerReader(), table.getPropagationHandler().getPropagationDBTableInfo(), countPropagationEntriesWithPk, logger);
    if (count > 0) {
      propagationLogger.info("Propagation will be denied because another propagation entry could be found with: " + countPropagationEntriesWithPk.sql + " and " + primaryKeyMap);
      throw new DPPWebserviceManipulationDeniedException();
    }
  }
  
  protected void propagateActionOnDppPairs() throws RemoteException {
    Collection<DPPPair> dppPairs = retrieveAllDPPPairs();
    Collection<LocationsRow> successfullExecutions = new HashSet<LocationsRow>();
    Collection<LocationsRow> failedRollbacks = new HashSet<LocationsRow>();
    try {
      for (DPPPair dppPair : dppPairs) {
        try {
          propagationLogger.info("Propagation on primary: " + dppPair.name + " is starting.");
          SQLUtilsContainer sqlUtils = getCachedConnectionForLocation(dppPair.primary);
          executeAction(inputValues, table, sqlUtils);
          successfullExecutions.add(dppPair.primary);
        } catch (Throwable t) {
          propagationLogger.info("Propagation on primary failed for DPP-Pair: " + dppPair.name + ", going to try secondary.", t);
          SQLUtilsContainer sqlUtils = getCachedConnectionForLocation(dppPair.secondary);
          try {
            executeAction(inputValues, table, sqlUtils);
          } catch (Throwable tt) {
            propagationLogger.info("Propagation on secondary failed for DPP-Pair: " + dppPair.name + ", going to start rollback.", t);
            throw tt;
          }
          successfullExecutions.add(dppPair.secondary);
        }
      }
    } catch (Throwable t) {
      propagationLogger.error("Propagation on primary and secondary failed for a DPP-Pair, rolling back " + successfullExecutions.size() + " succesfull propagations.", t);
      try {
        for (LocationsRow locationsRow : successfullExecutions) {
          try {
            SQLUtilsContainer sqlUtils = getCachedConnectionForLocation(locationsRow);
            propagationLogger.info("PropagationRollback on: " + locationsRow.name + " is starting.");
            executeRollback(backupValues, table, sqlUtils);
          } catch (Throwable tt) {
            propagationLogger.error("PropagationRollback failed for " + locationsRow.name + ".", t);
            failedRollbacks.add(locationsRow);
          }
        }
      } finally {
        SQLUtilsContainer sqlUtils = SQLUtilsCache.getForManagement(table.getSchema(), logger);
        try {
          executeRollback(backupValues, table, sqlUtils);
        } catch (Throwable tt) {
          propagationLogger.error("PropagationRollback on Management failed.", tt);
          LocationsRow managementRow = new LocationsRow();
          managementRow.name = Constants.managementName;
          managementRow.failover = sqlUtils.getFailOverFlag().toString();
          failedRollbacks.add(managementRow);
        }
        if (failedRollbacks.size() > 0) {
          tryToAdjustPropagationEntry(failedRollbacks);
        }
        throwAsDPPWebserviceException(t);
      }
    }
    deletePropagationEntry();
  }
  
  
  protected void tryToAdjustPropagationEntry(Collection<LocationsRow> failedLocations) {
    StringBuilder updateBuilder = new StringBuilder();
    try {
      updateBuilder.append("UPDATE ")
                   .append(table.getPropagationHandler().getPropagationDBTableInfo().getSchema())
                   .append(".")
                   .append(table.getPropagationHandler().getPropagationDBTableInfo().getTablename())
                   .append(" SET ")
                   .append(table.getPropagationHandler().getRollbackFailureColumnName())
                   .append("='");
      Iterator<LocationsRow> iter = failedLocations.iterator();
      while (iter.hasNext()) {
        LocationsRow current = iter.next();
        updateBuilder.append(current.name)
                     .append("_")
                     .append(current.failover);
        if (iter.hasNext()) {
          updateBuilder.append(",");
        }
      }
      updateBuilder.append("' WHERE");
      SQLCommand update = new SQLCommand();
      update.sql = updateBuilder.toString();
      SQLBuilder.addSQLPkColsCondition(primaryKeyMap, table.getPropagationHandler().getPropagationDBTableInfo(), update);
      DBCommands.executeDML(table.getPropagationHandler().getPropagationDBTableInfo(), update, logger);
    } catch (Throwable t) {
      propagationLogger.error("Error occured while trying to enrich Propagation entry with rollback failures.", t);
      logger.warn("Error occured while trying to enrich propagation entry with rollback failures.");
      logger.warn(updateBuilder.toString());
      logger.error(t);
    }
  }
  
  
  protected void deletePropagationEntry() throws RemoteException {
    propagationLogger.info("trying to delete propagationEntry");
    try {
      SQLCommand deletePropagation = SQLBuilder.buildSQLDelete(primaryKeyMap, table.getPropagationHandler().getPropagationDBTableInfo());
      DBCommands.executeDML(table.getPropagationHandler().getPropagationDBTableInfo(), deletePropagation, logger);
    } catch (RemoteException e) {
      propagationLogger.error("Failed to delete PropagationEntry!", e);
      throw e;
    }
  }
  
  
  protected void unlockManagementData() throws RemoteException {
    TreeMap<String, String> updateValues = new TreeMap<String, String>(primaryKeyMap);
    StringBuilder lockBuilder = new StringBuilder();
    lockBuilder.append("UPDATE ")
               .append(table.getSchema())
               .append('.')
               .append(table.getTablename())
               .append(" SET WHERE");
    SQLCommand lockingUpdate = new SQLCommand();
    lockingUpdate.sql = lockBuilder.toString(); 
    SQLBuilder.addSQLPkColsCondition(updateValues, table, lockingUpdate);
    lockingUpdate = MultiUserTools.appendModificationTimestampUpdate(lockingUpdate, Long.MAX_VALUE, System.currentTimeMillis());
    int index = lockingUpdate.sql.indexOf("SET,");
    if (index >= 0) {
      lockingUpdate.sql = lockingUpdate.sql.substring(0, index+3) + lockingUpdate.sql.substring(index+4);
    }
    try {
      DBCommands.executeDML(table, lockingUpdate, logger);
    } catch (RemoteException e) {
      propagationLogger.error("Failed to unlock management data!", e);
    }
  }
  
  
  private SQLUtilsContainer getCachedConnectionForLocation(LocationsRow location) throws RemoteException {
    LocationSchema schema = LocationSchema.valueOf(table.getSchema());
    FailoverFlag failover = FailoverFlag.valueOf(location.failover);
    return SQLUtilsCache.getForLocation(location.name,
                                        schema,
                                        failover,
                                        logger);
  }
  
  
  private Collection<DPPPair> retrieveAllDPPPairs() throws RemoteException {
    List<LocationsRow> allLocations = LocationTools.getLocations(LocationSchema.service, logger);
    Set<DPPPair> pairs = new HashSet<DPPPair>();
    for (LocationsRow primary : allLocations) {
      if (primary.failover.equals("primary") &&
          !primary.name.equals(Constants.managementName)) { //exclude management
        for (LocationsRow secondary : allLocations) {
          if (primary.name.equals(secondary.name) && 
              secondary.failover.equals("secondary")) {
            pairs.add(new DPPPair(primary, secondary));
          }
        }
      }
    }
    return pairs;
  }
    
  
  private TreeMap<String, String> pruneToPrimaryKeys(TreeMap<String, String> allValues, DBTableInfo table) {
    TreeMap<String, String> pkValueMap = new TreeMap<String, String>();
    for (Entry<String, ColInfo> entry : table.getColumns().entrySet()) {
      if (entry.getValue().pk == Pk.True &&
          allValues.containsKey(entry.getKey())) {
        pkValueMap.put(entry.getKey(), allValues.get(entry.getKey()));
      }
    }
    return pkValueMap;
  }
  
  
  private static void throwAsDPPWebserviceException(Throwable t) throws DPPWebserviceException {
    if (t instanceof DPPWebserviceException) {
      throw (DPPWebserviceException)t;
    } else {
      throw new DPPWebserviceException("", t);
    }
  }
  
  
  static class PropagationActionInsert<O> extends PropagationAction<O, O> {
    
    private boolean rolledBackManagementData = false;
    
    PropagationActionInsert(O inputObject, TreeMap<String, String> inputValues, ResultSetReader<O> reader, DBTableInfo table, Logger logger) {
      super(inputObject, inputValues, reader, table,
            PropagationActionIdentifier.INSERT,
            AuthenticationTools.WebServiceInvocationIdentifier.DUMMY_WEBSERVICE_IDENTIFIER, logger);
    }

    @Override
    protected void lockManagementData(WebServiceInvocationIdentifier wsid) {
      ; //locking impossible due to object not being present yet
    }
    
    @Override
    protected TreeMap<String, String> retrieveBackup(TreeMap<String, String> primaryKeyMap) throws RemoteException {
      return null;
    }
    
    @Override
    protected void insertPropagationEntry(TreeMap<String, String> values, PropagationActionIdentifier actionIdentifier)
                    throws RemoteException {
      super.insertPropagationEntry(inputValues, actionIdentifier);
    }
    
    public O executeAction(TreeMap<String, String> inputValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException {
      try {
        SQLCommand builder = SQLBuilder.buildSQLInsert(inputValues, table);
        if (sqlUtils instanceof SQLUtilsContainerForLocation) {
          builder.sql = builder.sql.replaceFirst("INSERT", "REPLACE");
        }
        builder = MultiUserTools.appendTimestampIntoInsertionCommand(builder, Long.MAX_VALUE); // append lock
        DBCommands.executeDML(builder, sqlUtils, logger);
        return new DBCommands<O>().queryOneRow(reader, table, inputValues, logger);
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        logger.error("", e);
        throw new DPPWebserviceDatabaseException(e);
      }
    }

    @Override
    protected void executeRollback(TreeMap<String, String> backupValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException {
      SQLCommand builder = SQLBuilder.buildSQLDelete(inputValues, table);
      DBCommands.executeDML(builder, sqlUtils, logger);
      if (sqlUtils instanceof SQLUtilsContainerForManagement) {
        rolledBackManagementData = true;
      }
    };
    
    
    @Override
    protected void unlockManagementData() throws RemoteException {
      if (!rolledBackManagementData) { //if we rolledback there will be nothing to unlock
        super.unlockManagementData();
      }
    }
  }


  static class PropagationActionUpdate<O> extends PropagationAction<O, O> {
    
    PropagationActionUpdate(O inputObject, TreeMap<String, String> inputValues, ResultSetReader<O> reader, DBTableInfo table, WebServiceInvocationIdentifier wsid, Logger logger) {
      super(inputObject, inputValues, reader, table, PropagationActionIdentifier.UPDATE, wsid, logger);
    }

    @Override
    protected void insertPropagationEntry(TreeMap<String, String> backupValues, PropagationActionIdentifier actionIdentifier) throws RemoteException {
      super.insertPropagationEntry(backupValues, PropagationActionIdentifier.BACKUP);
      super.insertPropagationEntry(inputValues, actionIdentifier);
    } 
    
    public O executeAction(TreeMap<String, String> inputValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException {
      try {
        SQLCommand builder = SQLBuilder.buildSQLUpdate(inputValues, table);
        builder = MultiUserTools.appendModificationTimestampUpdate(builder, Long.MAX_VALUE, Long.MAX_VALUE); // don't override the lock
        DBCommands.executeDML(builder, sqlUtils, logger);
        SQLCommand query = SQLBuilder.buildSQLSelectAllWhere(inputValues, table, true); 
        return new DBCommands<O>().queryOneRow(reader, query, sqlUtils, logger);
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        logger.error("", e);
        throw new DPPWebserviceDatabaseException(e);
      }
    }

    @Override
    protected void executeRollback(TreeMap<String, String> backupValues, DBTableInfo table, SQLUtilsContainer sqlUtils)
                    throws RemoteException {
      SQLCommand builder = SQLBuilder.buildSQLUpdate(backupValues, table);
      DBCommands.executeDML(builder, sqlUtils, logger);
    };
  }


  static class PropagationActionDelete<O> extends PropagationAction<O, String> {
    
    private boolean gotBackup = false;
  
    PropagationActionDelete(O inputObject, TreeMap<String, String> inputValues, ResultSetReader<O> reader, DBTableInfo table, WebServiceInvocationIdentifier wsid, Logger logger) {
      super(inputObject, inputValues, reader, table, PropagationActionIdentifier.UPDATE, wsid, logger);
    }
    
    public String executeAction(TreeMap<String, String> inputValues, DBTableInfo table, SQLUtilsContainer sqlUtils) throws RemoteException {
      try {
        SQLCommand builder = SQLBuilder.buildSQLDelete(inputValues, table);
        int modifiedRows = DBCommands.executeDML(builder, sqlUtils, logger);
        return modifiedRows + " Rows deleted.";
      } catch (java.rmi.RemoteException e) {
        throw e;
      } catch (Exception e) {
        logger.error("", e);
        throw new DPPWebserviceDatabaseException(e);
      }
    }
    
    @Override
    protected void unlockManagementData() throws RemoteException {
      ; //object will either be delete or reinserted with previous timestamp 
    }

    @Override
    protected void executeRollback(TreeMap<String, String> backupValues, DBTableInfo table, SQLUtilsContainer sqlUtils)
                    throws RemoteException {
      if (gotBackup) {
        SQLCommand builder = SQLBuilder.buildSQLInsert(inputValues, table);
        DBCommands.executeDML(builder, sqlUtils, logger);
      } else {
        propagationLogger.warn("DeletePropagation will skip rollback, no backup values.");
      }
    };
    
    @Override
    protected void handleDPPWebServiceExceptionOnObjectNotFoundForLocking(DPPWebserviceException e)
                    throws DPPWebserviceException {
      propagationLogger.warn("DeletePropagation ignoring error during locking of managemenet data",e);
    }
    
    @Override
    protected TreeMap<String, String> retrieveBackup(TreeMap<String, String> primaryKeyMap) throws RemoteException {
      try {
        TreeMap<String, String> backup = super.retrieveBackup(primaryKeyMap);
        gotBackup = true;
        return backup;
      } catch (Throwable t) {
        propagationLogger.warn("DeletePropagation ignoring error during backup retrieval",t);
        return primaryKeyMap;
      }
    }
    
    @Override
    public void ensureNoOtherPropagationEntryExists(TreeMap<String, String> primaryKeyMap) throws RemoteException {
      deletePropagationEntry();
    }
  }


  private static class DPPPair {
    final String name;
    final LocationsRow primary;
    final LocationsRow secondary;

    DPPPair(LocationsRow primary, LocationsRow secondary) {
      this.name = primary.name;
      this.primary = primary;
      this.secondary = secondary;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof DPPPair)) {
        return false;
      }
      return this.name.equals(((DPPPair) obj).name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }
  
  
  public enum PropagationActionIdentifier {

    INSERT("insert"),
    UPDATE("update"),
    BACKUP("backup"),
    DELETE("delete");
    
    
    private final String actionIdentifier;
    
    
    PropagationActionIdentifier(String actionIdentifier) {
      this.actionIdentifier = actionIdentifier;
    }
    
    
    public String getActionIdentifier() {
      return actionIdentifier;
    }
    
    
    public static PropagationActionIdentifier getPropagationActionByIdentifier(String identifier) {
      for (PropagationActionIdentifier propagationAction : values()) {
        if (propagationAction.actionIdentifier.equals(identifier)) {
          return propagationAction;
        }
      }
      throw new IllegalArgumentException("There is no PropagationAction with identifier " + identifier);
    }
    
  }
  
}



