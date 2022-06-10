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
package com.gip.juno.ws.tools.migration;


import java.rmi.RemoteException;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.db.tables.dhcp.PoolHandler;
import com.gip.juno.ws.db.tables.dhcp.SharedNetworkHandler;
import com.gip.juno.ws.db.tables.dhcp.StandortHandler;
import com.gip.juno.ws.db.tables.dhcp.StandortgruppeHandler;
import com.gip.juno.ws.db.tables.dhcp.StaticHostHandler;
import com.gip.juno.ws.db.tables.dhcp.SubnetHandler;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBLongReader;



public class MigrationHelper {
  
  private final static Logger logger = Logger.getLogger("Migration");
  
  public enum MigrationState {
    TargetRoot, Target, Source, SourceRoot;
  }
  
  public enum TargetState {
    active, inactive;
  }

  private static TableHandler standortHandler = new StandortHandler();
  //private static TableHandler standortGruppeHandler = new StandortgruppeHandler();
  private static TableHandler sharedNetworkHandler = new SharedNetworkHandler();
  private static TableHandler subnetHandler = new SubnetHandler();
  private static TableHandler poolHandler = new PoolHandler();
  
  private static TableHandler staticHostHandler = new StaticHostHandler();
  
  private final static String MIGRATIONSTATE_COLUMN_NAME = "MigrationState";
  
  private static SQLCommand ensureTargetSubnetExistsForPool = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                                                .append(subnetHandler.getTablename())
                                                                                                .append(" WHERE ")
                                                                                                .append(SubnetHandler.SUBNETID_COLUMN_NAME)
                                                                                                .append("=?").toString());
  
  private static SQLCommand ensureTargetSharedNetworkExistsForSubnet = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                                                         .append(sharedNetworkHandler.getTablename())
                                                                                                         .append(" WHERE ")
                                                                                                         .append(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME)
                                                                                                         .append("=?").toString());
  
  private static SQLCommand ensureTargetStandorExistsForSharedNetwork = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                                      .append(standortHandler.getTablename())
                                                                                      .append(" WHERE ")
                                                                                      //.append(StandortgruppeHandler.STANDORTGRUPPEID_COLUMN_NAME)
                                                                                      .append(StandortHandler.STANDORTID_COLUMN_NAME)
                                                                                      .append("=?").toString());
  
  private static SQLCommand ensureUnsetMigrationStateForSharedNetwork = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                                .append(sharedNetworkHandler.getTablename())
                                                                                .append(" WHERE ")
                                                                                .append(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME)
                                                                                .append("=? AND ")
                                                                                .append(MIGRATIONSTATE_COLUMN_NAME)
                                                                                .append("= ''").toString());
  
  private static SQLCommand ensureUnsetMigrationStateForSubnet = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                              .append(subnetHandler.getTablename())
                                                                              .append(" WHERE ")
                                                                              .append(SubnetHandler.SUBNETID_COLUMN_NAME)
                                                                              .append("=? AND ")
                                                                              .append(MIGRATIONSTATE_COLUMN_NAME)
                                                                              .append("= ''").toString());
  
  private static SQLCommand ensureUnsetMigrationStateForPool = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                                              .append(poolHandler.getTablename())
                                                                              .append(" WHERE ")
                                                                              .append(PoolHandler.POOLID_COLUMN_NAME)
                                                                              .append("=? AND ")
                                                                              .append(MIGRATIONSTATE_COLUMN_NAME)
                                                                              .append("= ''").toString());

  private static SQLCommand ensureNoStaticHostsForPool = new SQLCommand(new StringBuilder().append("SELECT count(*) FROM ")
                                                             .append(staticHostHandler.getTablename())
                                                             .append(" WHERE ")
                                                             .append(StaticHostHandler.ASSIGNEDPOOLID_COLNAME)
                                                             .append("=?").toString());
  
  private static SQLCommand querySharedNetworkById = new SQLCommand(new StringBuilder().append("SELECT * FROM ")
                                                               .append(sharedNetworkHandler.getTablename())
                                                               .append(" WHERE ")
                                                               .append(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME)
                                                               .append("=?").toString());
  
  private static SQLCommand querySubnetById = new SQLCommand(new StringBuilder().append("SELECT * FROM ")
                                                               .append(subnetHandler.getTablename())
                                                               .append(" WHERE ")
                                                               .append(SubnetHandler.SUBNETID_COLUMN_NAME)
                                                               .append("=?").toString());
  
  private static SQLCommand queryPoolById = new SQLCommand(new StringBuilder().append("SELECT * FROM ")
                                                             .append(poolHandler.getTablename())
                                                             .append(" WHERE ")
                                                             .append(PoolHandler.POOLID_COLUMN_NAME)
                                                             .append("=?").toString());
  
  private static SQLCommand updateSharedNetworkMigrationState = new SQLCommand(new StringBuilder().append("UPDATE ")
                                                                              .append(sharedNetworkHandler.getTablename())
                                                                              .append(" SET ")
                                                                              .append(MIGRATIONSTATE_COLUMN_NAME)
                                                                              .append("=? WHERE ")
                                                                              .append(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME)
                                                                              .append("=?").toString());
  
  private static SQLCommand updateSubnetMigrationState = new SQLCommand(new StringBuilder().append("UPDATE ")
                                                                            .append(subnetHandler.getTablename())
                                                                            .append(" SET ")
                                                                            .append(MIGRATIONSTATE_COLUMN_NAME)
                                                                            .append("=? WHERE ")
                                                                            .append(SubnetHandler.SUBNETID_COLUMN_NAME)
                                                                            .append("=?").toString());
  
  private static SQLCommand updatePoolMigrationState = new SQLCommand(new StringBuilder().append("UPDATE ")
                                                             .append(poolHandler.getTablename())
                                                             .append(" SET ")
                                                             .append(MIGRATIONSTATE_COLUMN_NAME)
                                                             .append("=? WHERE ")
                                                             .append(PoolHandler.POOLID_COLUMN_NAME)
                                                             .append("=?").toString());
  
  private static SQLCommand queryDependentSourcePoolIds = new SQLCommand(new StringBuilder().append("SELECT ")
                                                                         .append(PoolHandler.POOLID_COLUMN_NAME)
                                                                         .append(" FROM ")
                                                                         .append(poolHandler.getTablename())
                                                                         .append(" WHERE ")
                                                                         .append(PoolHandler.SUBNETID_COLUMN_NAME)
                                                                         .append("=?").toString());
  
  private static SQLCommand queryDependentSourceSubnetIds = new SQLCommand(new StringBuilder().append("SELECT ")
                                                                         .append(SubnetHandler.SUBNETID_COLUMN_NAME)
                                                                         .append(" FROM ")
                                                                         .append(subnetHandler.getTablename())
                                                                         .append(" WHERE ")
                                                                         .append(SubnetHandler.SHAREDNETOWRKID_COLUMN_NAME)
                                                                         .append("=?").toString());
  
  private static SQLCommand updatePoolTargetState = new SQLCommand(new StringBuilder().append("UPDATE ")
                                                                      .append(poolHandler.getTablename())
                                                                      .append(" SET ")
                                                                      .append(PoolHandler.TARGETSTATE_COLUMN_NAME)
                                                                      .append("=?")
                                                                      .append(" WHERE ")
                                                                      .append(PoolHandler.POOLID_COLUMN_NAME)
                                                                      .append("=?").toString());
  
                                                                         
        
  
  public static String duplicateSharedNetwork(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    // targetIdentifier is a StandortGruppeID
    // we need to query the current StandortID
    SQLCommand queryStandortId = new SQLCommand(new StringBuilder()
                                              .append("SELECT ")
                                              .append(SharedNetworkHandler.STANDORTID_COLUMN_NAME)
                                              .append(" FROM ")
                                              .append(sharedNetworkHandler.getTablename())
                                              .append(" WHERE ")
                                              .append(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME)
                                              .append("=?").toString());
    queryStandortId.addConditionParam(sourceIdentifier);
    Long standortId = new DBCommands<Long>().queryOneRow(new DBLongReader(), sharedNetworkHandler.getDBTableInfo(), queryStandortId, logger);
    //lookup the name of the standort
    SQLCommand queryStandortName = new SQLCommand(new StringBuilder()
                                                .append("SELECT ")
                                                .append(StandortHandler.NAME_COLUMN_NAME)
                                                .append(" FROM ")
                                                .append(standortHandler.getTablename())
                                                .append(" WHERE ")
                                                .append(StandortHandler.STANDORTID_COLUMN_NAME)
                                                .append("=?").toString());
    queryStandortName.addConditionParam(Long.toString(standortId));
    String standortName = new DBCommands<String>().queryOneRow(new QueryTools.DBStringReader(), standortHandler.getDBTableInfo(), queryStandortName, logger);
    // if there already exists a Standort with the same name for the new StandortGruppe...
    SQLCommand countStandortNameInNewGroupQuery = new SQLCommand(new StringBuilder()
                                                              .append("SELECT count(*) FROM ")
                                                              .append(standortHandler.getTablename())
                                                              .append(" WHERE ")
                                                              .append(StandortHandler.NAME_COLUMN_NAME)
                                                              .append("=? AND ")
                                                              .append(StandortHandler.STANDORTGRUPPEID_COLUMN_NAME)
                                                              .append("=?").toString());
    countStandortNameInNewGroupQuery.addConditionParam(standortName);
    countStandortNameInNewGroupQuery.addConditionParam(targetIdentifier);
    Long countInNewGruppe = new DBCommands<Long>().queryOneRow(new QueryTools.DBLongReader(), standortHandler.getDBTableInfo(), countStandortNameInNewGroupQuery, logger);
    if (countInNewGruppe > 0) {
      // we use that id
      SQLCommand selectStandortIdInNewStandortGruppe = new SQLCommand(new StringBuilder()
                                                                        .append("SELECT ")
                                                                        .append(StandortHandler.STANDORTID_COLUMN_NAME) 
                                                                        .append(" FROM ")
                                                                        .append(standortHandler.getTablename())
                                                                        .append(" WHERE ")
                                                                        .append(StandortHandler.NAME_COLUMN_NAME)
                                                                        .append("=? AND ")
                                                                        .append(StandortHandler.STANDORTGRUPPEID_COLUMN_NAME)
                                                                        .append("=?").toString());
      selectStandortIdInNewStandortGruppe.addConditionParam(standortName);
      selectStandortIdInNewStandortGruppe.addConditionParam(targetIdentifier);
      Long newStandortId = new DBCommands<Long>().queryOneRow(new DBLongReader(), standortHandler.getDBTableInfo(), selectStandortIdInNewStandortGruppe, logger);
      targetIdentifier = Long.toString(newStandortId);
    } else {
      // else we have to create it 
      TreeMap<String, String> newStandortValues = new TreeMap<String, String>();
      newStandortValues.put(StandortHandler.STANDORTID_COLUMN_NAME, "");
      newStandortValues.put(StandortHandler.NAME_COLUMN_NAME, standortName);
      newStandortValues.put(StandortHandler.STANDORTGRUPPEID_COLUMN_NAME, targetIdentifier);
      QueryTools.checkAutoIncrement(newStandortValues, standortHandler.getDBTableInfo(), logger);
      logger.info("targetValues after autoIncrement: " + newStandortValues);
      String newStandortId = newStandortValues.get(StandortHandler.STANDORTID_COLUMN_NAME);
      SQLCommand insertTarget = SQLBuilder.buildSQLInsert(newStandortValues, standortHandler.getDBTableInfo());
      DBCommands.executeDML(standortHandler.getDBTableInfo(), insertTarget, logger);
      targetIdentifier = newStandortId;
    }
    
    return duplicateSharedNetworkAndDependents(sourceIdentifier, MigrationState.SourceRoot, targetIdentifier, MigrationState.TargetRoot);
  }
  
  
  public static String duplicateSubnet(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    return duplicateSubnetAndDependents(sourceIdentifier, MigrationState.SourceRoot, targetIdentifier, MigrationState.TargetRoot);
  }
  
  
  public static String duplicatePool(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    return duplicatePoolAndDependents(sourceIdentifier, MigrationState.SourceRoot, targetIdentifier, MigrationState.TargetRoot);
  }
  
  
  public static void deactivateSharedNetwork(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, sharedNetworkHandler, MigrationState.SourceRoot);
    updateTargetStateForSharedNetwork(sourceIdentifier, TargetState.inactive, false);
  }
  
  
  public static void deactivateSubnet(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, subnetHandler, MigrationState.SourceRoot);
    updateTargetStateForSubnet(sourceIdentifier, TargetState.inactive, false);
  }
  
  
  public static void deactivatePool(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, poolHandler, MigrationState.SourceRoot);
    updateTargetStateForPool(sourceIdentifier, TargetState.inactive, false);
  }
  
  
  public static void activateSharedNetwork(String targetIdentifier) throws RemoteException {
    ensureMigrationState(targetIdentifier, sharedNetworkHandler, MigrationState.TargetRoot);
    updateTargetStateForSharedNetwork(targetIdentifier, TargetState.active, true);
  }
  
  
  public static void activateSubnet(String targetIdentifier) throws RemoteException {
    ensureMigrationState(targetIdentifier, subnetHandler, MigrationState.TargetRoot);
    updateTargetStateForSubnet(targetIdentifier, TargetState.active, true);
  }
  
  
  public static void activatePool(String targetIdentifier) throws RemoteException {
    ensureMigrationState(targetIdentifier, poolHandler, MigrationState.TargetRoot);
    updateTargetStateForPool(targetIdentifier, TargetState.active, true);
  }
  
  
  public static void deleteSharedNetwork(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, sharedNetworkHandler, MigrationState.SourceRoot);
    deleteSharedNetworkAndDependents(sourceIdentifier);
  }
  
  
  public static void deleteSubnet(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, subnetHandler, MigrationState.SourceRoot);
    deleteSubnetAndDependents(sourceIdentifier);
  }
  
  
  public static void deletePool(String sourceIdentifier) throws RemoteException {
    ensureMigrationState(sourceIdentifier, poolHandler, MigrationState.SourceRoot);
    deletePoolAndDependents(sourceIdentifier);
  }
  
  
  
  private static void checkSharedNetworkMigrationInitializationConditions(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    SQLCommand targetQuery = ensureTargetStandorExistsForSharedNetwork.clone();
    targetQuery.addConditionParam(targetIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), standortHandler.getDBTableInfo(), targetQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Target does not exists!");
    }
    // ensure that the source and it's MigrationMode is not set
    SQLCommand sourceMigrationModeQuery = ensureUnsetMigrationStateForSharedNetwork.clone();
    sourceMigrationModeQuery.addConditionParam(sourceIdentifier);
    count = new DBCommands<Long>().queryOneRow(new DBLongReader(), sharedNetworkHandler.getDBTableInfo(), sourceMigrationModeQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Source does already have a migrationState set!");
    }
  }
  
  
  private static void checkSubnetMigrationInitializationConditions(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    // ensure SharedNetwork with targetIdentifier exists
    SQLCommand targetQuery = ensureTargetSharedNetworkExistsForSubnet.clone();
    targetQuery.addConditionParam(targetIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), sharedNetworkHandler.getDBTableInfo(), targetQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Target does not exists!");
    }
    checkSubnetMigrationInitializationConditionsAsDependent(sourceIdentifier);
  }
  
  
  private static void checkSubnetMigrationInitializationConditionsAsDependent(String sourceIdentifier) throws RemoteException {
    // ensure that the source and it's MigrationMode is not set
    SQLCommand sourceMigrationModeQuery = ensureUnsetMigrationStateForSubnet.clone();
    sourceMigrationModeQuery.addConditionParam(sourceIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), poolHandler.getDBTableInfo(), sourceMigrationModeQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Source does already have a migrationState set!");
    }
    
    SQLCommand queryDependents = queryDependentSourcePoolIds.clone();
    queryDependents.addConditionParam(sourceIdentifier);
    List<Long> dependetPoolIds = new DBCommands<Long>().query(new DBLongReader(), poolHandler.getDBTableInfo(), queryDependents, logger);
    for (Long poolId : dependetPoolIds) {
      checkPoolMigrationInitializationConditionsAsDependent(poolId.toString());
    }
  }
  
    
  private static void checkPoolMigrationInitializationConditions(String sourceIdentifier, String targetIdentifier) throws RemoteException {
    // ensure Subnet with targetIdentifier exists
    SQLCommand targetQuery = ensureTargetSubnetExistsForPool.clone();
    targetQuery.addConditionParam(targetIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), subnetHandler.getDBTableInfo(), targetQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Target does not exists!");
    }
    checkPoolMigrationInitializationConditionsAsDependent(sourceIdentifier);
  }
  
  
  private static void checkPoolMigrationInitializationConditionsAsDependent(String sourceIdentifier) throws RemoteException {
    // ensure that the source and it's dependents have no MigrationMode set
    SQLCommand sourceMigrationModeQuery = ensureUnsetMigrationStateForPool.clone();
    sourceMigrationModeQuery.addConditionParam(sourceIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), poolHandler.getDBTableInfo(), sourceMigrationModeQuery, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Source does already have a migrationState set!");
    }
    // ensure that there are no staticHost in all the pools
    SQLCommand noStaticHostsQuery = ensureNoStaticHostsForPool.clone();
    noStaticHostsQuery.addConditionParam(sourceIdentifier);
    count = new DBCommands<Long>().queryOneRow(new DBLongReader(), staticHostHandler.getDBTableInfo(), noStaticHostsQuery, logger);
    if (count > 0) {
      throw new DPPWebserviceException("StaticHosts are not allowed in migrating pools!");
    }
  }
  
  
  private static String duplicatePoolAndDependents(String sourceIdentifier, MigrationState sourceState, String targetIdentifier, MigrationState targetState) throws RemoteException {
    checkPoolMigrationInitializationConditions(sourceIdentifier, targetIdentifier);
    
    SQLCommand sourceQuery = queryPoolById.clone();
    sourceQuery.addConditionParam(sourceIdentifier);
    TreeMap<String, String> sourceValues = new DBCommands<TreeMap<String, String>>().queryOneRow(new TableHandlerBasedRowMapReader(poolHandler),
                                                                                                 poolHandler.getDBTableInfo(), sourceQuery, logger);
    TreeMap<String, String> targetValues = new TreeMap<String, String>(sourceValues);
    targetValues.put(MIGRATIONSTATE_COLUMN_NAME, targetState.toString());
    
    targetValues.put(PoolHandler.POOLID_COLUMN_NAME, "");
    targetValues.put(PoolHandler.TARGETSTATE_COLUMN_NAME, TargetState.inactive.toString());
    targetValues.put(PoolHandler.SUBNETID_COLUMN_NAME, targetIdentifier);
    targetValues.put(PoolHandler.ISDEPLOYED_COLUMN_NAME, "no");
    logger.info("targetValues before autoIncrement: " + targetValues);
    QueryTools.checkAutoIncrement(targetValues, poolHandler.getDBTableInfo(), logger);
    logger.info("targetValues after autoIncrement: " + targetValues);
    String duplicatedId = targetValues.get(PoolHandler.POOLID_COLUMN_NAME);
    SQLCommand insertTarget = SQLBuilder.buildSQLInsert(targetValues, poolHandler.getDBTableInfo());
    DBCommands.executeDML(poolHandler.getDBTableInfo(), insertTarget, logger);
    
    SQLCommand sourceUpdate = updatePoolMigrationState.clone();
    sourceUpdate.addConditionParam(sourceState.toString());
    sourceUpdate.addConditionParam(sourceIdentifier);
    DBCommands.executeDML(poolHandler.getDBTableInfo(), sourceUpdate, logger);
    
    return duplicatedId;
  }
  
  
  private static String duplicateSubnetAndDependents(String sourceIdentifier, MigrationState sourceState, String targetIdentifier, MigrationState targetState) throws RemoteException {
    checkSubnetMigrationInitializationConditions(sourceIdentifier, targetIdentifier);
    
    SQLCommand sourceQuery = querySubnetById.clone();
    sourceQuery.addConditionParam(sourceIdentifier);
    TreeMap<String, String> sourceValues = new DBCommands<TreeMap<String, String>>().queryOneRow(new TableHandlerBasedRowMapReader(subnetHandler),
                                                                                                 subnetHandler.getDBTableInfo(), sourceQuery, logger);
    TreeMap<String, String> targetValues = new TreeMap<String, String>(sourceValues);
    targetValues.put(MIGRATIONSTATE_COLUMN_NAME, targetState.toString());
    
    targetValues.put(SubnetHandler.SUBNETID_COLUMN_NAME, "");
    targetValues.put(SubnetHandler.SHAREDNETOWRKID_COLUMN_NAME, targetIdentifier);
    QueryTools.checkAutoIncrement(targetValues, subnetHandler.getDBTableInfo(), logger);
    String duplicatedId = targetValues.get(SubnetHandler.SUBNETID_COLUMN_NAME);
    SQLCommand insertTarget = SQLBuilder.buildSQLInsert(targetValues, subnetHandler.getDBTableInfo());
    DBCommands.executeDML(subnetHandler.getDBTableInfo(), insertTarget, logger);
    
    SQLCommand sourceUpdate = updateSubnetMigrationState.clone();
    sourceUpdate.addConditionParam(sourceState.toString());
    sourceUpdate.addConditionParam(sourceIdentifier);
    DBCommands.executeDML(subnetHandler.getDBTableInfo(), sourceUpdate, logger);
    
    SQLCommand queryDependents = queryDependentSourcePoolIds.clone();
    queryDependents.addConditionParam(sourceIdentifier);
    List<Long> dependetPoolIds = new DBCommands<Long>().query(new DBLongReader(), poolHandler.getDBTableInfo(), queryDependents, logger);
    for (Long poolId : dependetPoolIds) {
      duplicatePoolAndDependents(Long.toString(poolId), MigrationState.Source, duplicatedId, MigrationState.Target);
    }
    
    return duplicatedId;
  }
  
  
  private static String duplicateSharedNetworkAndDependents(String sourceIdentifier, MigrationState sourceState, String targetIdentifier, MigrationState targetState) throws RemoteException {
    checkSharedNetworkMigrationInitializationConditions(sourceIdentifier, targetIdentifier);
    SQLCommand queryDependents = queryDependentSourceSubnetIds.clone();
    queryDependents.addConditionParam(sourceIdentifier);
    List<Long> dependetSubnetIds = new DBCommands<Long>().query(new DBLongReader(), subnetHandler.getDBTableInfo(), queryDependents, logger);
    for (Long subnetId : dependetSubnetIds) {
      checkSubnetMigrationInitializationConditionsAsDependent(subnetId.toString());
    }
    
    SQLCommand sourceQuery = querySharedNetworkById.clone();
    sourceQuery.addConditionParam(sourceIdentifier);
    TreeMap<String, String> sourceValues = new DBCommands<TreeMap<String, String>>().queryOneRow(new TableHandlerBasedRowMapReader(sharedNetworkHandler),
                                                                                                 sharedNetworkHandler.getDBTableInfo(), sourceQuery, logger);
    TreeMap<String, String> targetValues = new TreeMap<String, String>(sourceValues);
    targetValues.put(MIGRATIONSTATE_COLUMN_NAME, targetState.toString());
    
    targetValues.put(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME, "");
    targetValues.put(SharedNetworkHandler.STANDORTID_COLUMN_NAME, targetIdentifier);
    QueryTools.checkAutoIncrement(targetValues, sharedNetworkHandler.getDBTableInfo(), logger);
    String duplicatedId = targetValues.get(SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME);
    SQLCommand insertTarget = SQLBuilder.buildSQLInsert(targetValues, sharedNetworkHandler.getDBTableInfo());
    DBCommands.executeDML(sharedNetworkHandler.getDBTableInfo(), insertTarget, logger);
    
    SQLCommand sourceUpdate = updateSharedNetworkMigrationState.clone();
    sourceUpdate.addConditionParam(sourceState.toString());
    sourceUpdate.addConditionParam(sourceIdentifier);
    DBCommands.executeDML(sharedNetworkHandler.getDBTableInfo(), sourceUpdate, logger);
    
    
    for (Long subnetId : dependetSubnetIds) {
      duplicateSubnetAndDependents(Long.toString(subnetId), MigrationState.Source, duplicatedId, MigrationState.Target);
    }
    
    return duplicatedId;
  }
  
  
  private static void ensureMigrationState(String sourceIdentifier, TableHandler tableHandler, MigrationState migrationState) throws RemoteException {
    StringBuilder commandBuilder = new StringBuilder();
    commandBuilder.append("SELECT count(*) FROM ")
                  .append(tableHandler.getTablename())
                  .append(" WHERE ")
                  .append(getUniqueIdentifierColumnNameByTableHandler(tableHandler))
                  .append("=? AND ")
                  .append(MIGRATIONSTATE_COLUMN_NAME)
                  .append("='")
                  .append(migrationState.toString())
                  .append("'");
    SQLCommand query = new SQLCommand(commandBuilder.toString());
    query.addConditionParam(sourceIdentifier);
    Long count = new DBCommands<Long>().queryOneRow(new DBLongReader(), tableHandler.getDBTableInfo(), query, logger);
    if (count < 1) {
      throw new DPPWebserviceException("Object has not the right migrationState (" + migrationState.toString() + ")!");
    }
  }
  
  
  private static String getUniqueIdentifierColumnNameByTableHandler(TableHandler tableHandler) throws RemoteException {
    if (tableHandler instanceof PoolHandler) {
      return PoolHandler.POOLID_COLUMN_NAME;
    } else if (tableHandler instanceof SubnetHandler) {
      return SubnetHandler.SUBNETID_COLUMN_NAME;
    } else if (tableHandler instanceof SharedNetworkHandler) {
      return SharedNetworkHandler.SHAREDNETWORKID_COLUMN_NAME;
    } else {
      throw new DPPWebserviceException("Unkown tableHandler!");
    }
  }
  
  private static void updateTargetStateForSharedNetwork(String identifier, TargetState newState, boolean resetMigrationState) throws RemoteException {
    SQLCommand queryDependents = queryDependentSourceSubnetIds.clone();
    queryDependents.addConditionParam(identifier);
    List<Long> dependetSubnetIds = new DBCommands<Long>().query(new DBLongReader(), subnetHandler.getDBTableInfo(), queryDependents, logger);
    for (Long subnetId : dependetSubnetIds) {
      updateTargetStateForSubnet(Long.toString(subnetId), newState, resetMigrationState);
    }
    if (resetMigrationState) {
      resetMigrationState(identifier, sharedNetworkHandler);
    }
  }
  
  
  private static void updateTargetStateForSubnet(String identifier, TargetState newState, boolean resetMigrationState) throws RemoteException {
    SQLCommand queryDependents = queryDependentSourcePoolIds.clone();
    queryDependents.addConditionParam(identifier);
    List<Long> dependetPoolIds = new DBCommands<Long>().query(new DBLongReader(), poolHandler.getDBTableInfo(), queryDependents, logger);
    for (Long poolId : dependetPoolIds) {
      updateTargetStateForPool(Long.toString(poolId), newState, resetMigrationState);
    }
    if (resetMigrationState) {
      resetMigrationState(identifier, subnetHandler);
    }
  }
  
  
  private static void updateTargetStateForPool(String identifier, TargetState newState, boolean resetMigrationState) throws RemoteException {
    SQLCommand update = updatePoolTargetState.clone();
    update.addConditionParam(newState.toString());
    update.addConditionParam(identifier);
    DBCommands.executeDML(poolHandler.getDBTableInfo(), update, logger);
    if (resetMigrationState) {
      resetMigrationState(identifier, poolHandler);
    }
  }
  
  
  private static void deleteSharedNetworkAndDependents(String identifier) throws RemoteException {
    SQLCommand queryDependents = queryDependentSourceSubnetIds.clone();
    queryDependents.addConditionParam(identifier);
    List<Long> dependetSubnetIds = new DBCommands<Long>().query(new DBLongReader(), subnetHandler.getDBTableInfo(), queryDependents, logger);
    for (Long subnetId : dependetSubnetIds) {
      deleteSubnetAndDependents(Long.toString(subnetId));
    }
    deleteSourceObject(identifier, sharedNetworkHandler);
  }
  
  
  private static void deleteSubnetAndDependents(String sourceIdentifier) throws RemoteException {
    SQLCommand queryDependents = queryDependentSourcePoolIds.clone();
    queryDependents.addConditionParam(sourceIdentifier);
    List<Long> dependetPoolIds = new DBCommands<Long>().query(new DBLongReader(), poolHandler.getDBTableInfo(), queryDependents, logger);
    for (Long poolId : dependetPoolIds) {
      deletePoolAndDependents(Long.toString(poolId));
    }
    deleteSourceObject(sourceIdentifier, subnetHandler);
  }
  
  
  private static void deletePoolAndDependents(String sourceIdentifier) throws RemoteException {
    deleteSourceObject(sourceIdentifier, poolHandler);
  }
  
  private static void deleteSourceObject(String sourceIdentifier, TableHandler tableHandler) throws RemoteException {
    SQLCommand deleteCommand = new SQLCommand(new StringBuilder().append("DELETE FROM ")
                                              .append(tableHandler.getTablename())
                                              .append(" WHERE ")
                                              .append(getUniqueIdentifierColumnNameByTableHandler(tableHandler))
                                              .append("=?").toString());
    deleteCommand.addConditionParam(sourceIdentifier);
    DBCommands.executeDML(tableHandler.getDBTableInfo(), deleteCommand, logger);
  }
  
  private static void resetMigrationState(String sourceIdentifier, TableHandler tableHandler) throws RemoteException {
    SQLCommand resetCommand = new SQLCommand(new StringBuilder().append("UPDATE ")
                                              .append(tableHandler.getTablename())
                                              .append(" SET ")
                                              .append(MIGRATIONSTATE_COLUMN_NAME)
                                              .append("='' WHERE ")
                                              .append(getUniqueIdentifierColumnNameByTableHandler(tableHandler))
                                              .append("=?").toString());
    resetCommand.addConditionParam(sourceIdentifier);
    DBCommands.executeDML(tableHandler.getDBTableInfo(), resetCommand, logger);
  }
  
}
