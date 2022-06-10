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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.handler.AuthenticationTools.WebServiceInvocationIdentifier;
import com.gip.juno.ws.handler.ReflectionTools.DBReader;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.LocationTools;
import com.gip.juno.ws.tools.LocationTools.LocationsRow;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.juno.ws.tools.SQLBuilder;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.xyna.utils.db.ResultSetReader;

/**
 * Class whose methods handle a SQL operation for one single database (schema),
 * typically on management instances.
 *  
 * The actual sql operations are performed by calling the class DBCommands.
 * 
 * To do that, the supplied parameters are used to create an instance of SQLCommand
 * which describes the desired SQL operation and is needed to call DBCommands.
 */
public class DBCommandHandler<T> {
   
  public List<T> getAllRows(ResultSetReader<T> reader, DBTableInfo table, Logger logger) 
        throws java.rmi.RemoteException {
    try {
      SQLCommand builder = SQLBuilder.buildSQLSelectAll(table);         
          
      List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);
      if (ret==null) {
        System.out.println("Query empty!");
        return new ArrayList<T>();
      } else {
        System.out.println("select returns rows: "+ ret.size());
        if (ret.size() >= 1000) {
          System.out.println("cutting to long list");
          List<T> retcut = ret.subList(0, 999);          
          ret = retcut;
        }
      }      
      return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  

  public List<T> searchRows(T searchRowsRequest, ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> map, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table);
      List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);   
        if (ret==null) {
          System.out.println("Query empty!");
          return new ArrayList<T>();
        } else {
          System.out.println("select returns rows: "+ ret.size());
          if (ret.size() >= 1000) {
            List<T> retcut = ret.subList(0, 999);
            ret = retcut;
          }
        }      
        return ret;
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }

  public List<T> searchRowsForCPE(T searchRowsRequest, ResultSetReader<T> reader, DBTableInfo table, 
                            TreeMap<String, String> map, Logger logger) throws java.rmi.RemoteException {
                        try {
                          SQLCommand builder = SQLBuilder.buildSQLSelectWhere(map, table, false);
                          // hier fuer CPE modifizieren
                          if(builder.sql.contains("WHERE"))
                          {
                            builder.sql = builder.sql + " AND Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' LIMIT 999";
                          }
                          else
                          {
                            builder.sql = builder.sql + " WHERE Type NOT LIKE '%pktc%' AND Type NOT LIKE '%docsis%' LIMIT 999";
                          }
                          List<T> ret = new DBCommands<T>().query(reader, table, builder, logger);   
                            if (ret==null) {
                              System.out.println("Query empty!");
                              return new ArrayList<T>();
                            } else {
                              System.out.println("select returns rows: "+ ret.size());
                              if (ret.size() >= 1000) {
                                List<T> retcut = ret.subList(0, 999);
                                ret = retcut;
                              }
                            }      
                            return ret;
                        } catch (java.rmi.RemoteException e) {
                          throw e;
                        } catch (Exception e) {
                          logger.error("", e);
                          throw new DPPWebserviceDatabaseException(e);
                        }
                      }

  
  
  public T updateRow(T oneRowRequest, ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> map, WebServiceInvocationIdentifier wsid, Logger logger) throws java.rmi.RemoteException {
    try {
      ValueAdapter.checkUniqueForUpdate(map, table, logger);
      ValueAdapter.adjustColValuesForUpdate(map, table, logger);
      if (table.hasPropagationHandling()) {
        return new PropagationAction.PropagationActionUpdate<T>(oneRowRequest, map, reader, table, wsid, logger).executePropagationAction();
      } else {
        SQLCommand builder = SQLBuilder.buildSQLUpdate(map, table);
        if (wsid.needsCheckCollision()) {
          SQLCommand adjustedCommand = MultiUserTools.appendModifierSubselectAndModificationUpdate(builder, table, wsid.getSessionId());
          int modifiedRows = DBCommands.executeDML(table, adjustedCommand, logger);
          if (modifiedRows == 0) {
            MultiUserTools.handleObjectNotFoundOnModification(wsid.getSessionId(), oneRowRequest, table);
          } else {
            MultiUserTools.handleObjectFoundOnModification(table);
          }
        } else {
          DBCommands.executeDML(table, builder, logger);
        }
        return new DBCommands<T>().queryOneRow(reader, table, map, logger);
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
   
   
  @Deprecated
   public T updateRowPk(ResultSetReader<T> reader, DBTableInfo table, 
                        TreeMap<String, String> conditionsmap, TreeMap<String, String> newvaluesmap, Logger logger) 
                        throws java.rmi.RemoteException {
     try {
       ValueAdapter.checkUniqueForUpdatePk(conditionsmap, newvaluesmap, table, logger);
       ValueAdapter.adjustColValuesForUpdate(newvaluesmap, table, logger);
       SQLCommand builder = SQLBuilder.buildSQLUpdatePk(conditionsmap, newvaluesmap, table);
       DBCommands.executeDML(table, builder, logger);
       return new DBCommands<T>().queryOneRow(reader, table, newvaluesmap, logger);
     } catch (java.rmi.RemoteException e) {
       throw e;
     } catch (Exception e) {
       logger.error("", e);
       throw new DPPWebserviceDatabaseException(e);
     }
   }
  
  
  public T updateRowPk(ResultSetReader<T> reader, DBTableInfo table, 
        T conditions, T newvalues, WebServiceInvocationIdentifier wsid, Logger logger) 
        throws java.rmi.RemoteException {
    TreeMap<String, String> newvaluesmap = new ReflectionTools<T>(newvalues).getRowMap(table, newvalues, logger);
    TreeMap<String, String> conditionsmap = new ReflectionTools<T>(conditions).getRowMap(table, conditions, logger);
    try {
      ValueAdapter.checkUniqueForUpdatePk(conditionsmap, newvaluesmap, table, logger);
      ValueAdapter.adjustColValuesForUpdate(newvaluesmap, table, logger);
      SQLCommand builder = SQLBuilder.buildSQLUpdatePk(conditionsmap, newvaluesmap, table);
      if (wsid != null && wsid.needsCheckCollision()) {
        SQLCommand adjustedCommand = MultiUserTools.appendModifierSubselectAndModificationUpdate(builder, table, wsid.getSessionId());
        int modifiedRows = DBCommands.executeDML(table, adjustedCommand, logger);
        if (modifiedRows == 0) {
          MultiUserTools.handleObjectNotFoundOnModification(wsid.getSessionId(), conditions, table);
        } else {
          MultiUserTools.handleObjectFoundOnModification(table);
        }
      } else {
        DBCommands.executeDML(table, builder, logger);
      }
      return new DBCommands<T>().queryOneRow(reader, table, newvaluesmap, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
  public String updateRowPkIgnoreEmpty(ResultSetReader<T> reader, DBTableInfo table, 
      TreeMap<String, String> conditionsmap, TreeMap<String, String> newvaluesmap, Logger logger) 
      throws java.rmi.RemoteException {
    try {
      ValueAdapter.checkUniqueForUpdatePkIgnoreEmpty(conditionsmap, newvaluesmap, table, logger);
      ValueAdapter.adjustColValuesForUpdate(newvaluesmap, table, logger);
      SQLCommand builder = SQLBuilder.buildSQLUpdatePkIgnoreEmpty(conditionsmap, newvaluesmap, table);
      int numrows = DBCommands.executeDML(table, builder, logger);
      return numrows + " Row(s) were updated.";
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
   

  public T updateRowPkWithNullConditions(ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> conditionsmap, TreeMap<String, String> newvaluesmap, Logger logger) 
        throws java.rmi.RemoteException {
    try {
      ValueAdapter.checkUniqueForUpdatePk(conditionsmap, newvaluesmap, table, logger);
      ValueAdapter.adjustColValuesForUpdate(newvaluesmap, table, logger);
      SQLCommand builder = SQLBuilder.buildSQLUpdatePkWithNullConditions(conditionsmap, newvaluesmap, table);
      DBCommands.executeDML(table, builder, logger);      
      return new DBCommands<T>().queryOneRow(reader, table, newvaluesmap, logger);
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
  
  public T insertRow(T oneRowRequest, ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> map, Logger logger) throws java.rmi.RemoteException {
    try {
      QueryTools.checkAutoIncrement(map, table, logger);
      ValueAdapter.checkUniqueForInsert(map, table, logger);
      ValueAdapter.adjustColValuesForInsert(map, table, logger);
      if (table.hasPropagationHandling()) {
        return new PropagationAction.PropagationActionInsert<T>(oneRowRequest, map, reader, table, logger).executePropagationAction();
      } else {
        SQLCommand builder = SQLBuilder.buildSQLInsert(map, table);
        DBCommands.executeDML(table, builder, logger);
        return new DBCommands<T>().queryOneRow(reader, table, map, logger);
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
  
    
  public String deleteRows(T deleteRowsRequest, ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> map, WebServiceInvocationIdentifier wsid, Logger logger) throws java.rmi.RemoteException {
    try {
      if (table.hasPropagationHandling()) {
        return new PropagationAction.PropagationActionDelete<T>(deleteRowsRequest, map, reader, table, wsid, logger).executePropagationAction();
      } else {
        SQLCommand builder = SQLBuilder.buildSQLDelete(map, table);
        int modifiedRows;
        if (wsid.needsCheckCollision()) {
          SQLCommand adjustedCommand = MultiUserTools.appendNotModifiedAtEndOfTimeToDeletion(builder, table);
          modifiedRows = DBCommands.executeDML(table, adjustedCommand, logger);
          if (modifiedRows == 0) {
            MultiUserTools.handleObjectNotFoundOnDeletion(wsid.getSessionId(), deleteRowsRequest, table);
          } else {
            MultiUserTools.handleObjectFoundOnDeletion(table);
          }
        } else {
          modifiedRows = DBCommands.executeDML(table, builder, logger);
        }
        return modifiedRows + " Rows deleted.";
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
  
  public String deleteRowsWithNullConditions(T deleteRowsRequest, ResultSetReader<T> reader, DBTableInfo table, 
        TreeMap<String, String> map, WebServiceInvocationIdentifier wsid, Logger logger) throws java.rmi.RemoteException {
    try {
      SQLCommand builder = SQLBuilder.buildSQLDeleteWithNullConditions(map, table);
      int modifiedRows;
      if (wsid.needsCheckCollision()) {
        SQLCommand adjustedCommand = MultiUserTools.appendNotModifiedAtEndOfTimeToDeletion(builder, table);
        modifiedRows = DBCommands.executeDML(table, adjustedCommand, logger);
        if (modifiedRows == 0) {
          MultiUserTools.handleObjectNotFoundOnDeletion(wsid.getSessionId(), deleteRowsRequest, table);
        } else {
          MultiUserTools.handleObjectFoundOnDeletion(table);
        }
      } else {
        modifiedRows = DBCommands.executeDML(table, builder, logger);
      }
      return modifiedRows + " Rows deleted.";
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException(e);
    }
  }
  
     
}
