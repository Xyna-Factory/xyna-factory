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

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;

/**
 * class that executes database commands where connections will not be cached
 */
public class DBCommandsUncached<T> {
  
  public List<T> queryForManagement(ResultSetReader<T> reader, SQLCommand builder, 
        String schemaName, Logger logger) throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(schemaName, logger);
    FailoverFlag flag = FailoverTools.getCurrentFailover(schema, logger);
    //SQLUtils utils = SQLUtilsCacheForManagement.createSQLUtils(schema, flag, logger);
    SQLUtils utils = SQLUtilsCacheForManagement.createSQLUtilsForUncached(schema, flag, logger);
    List<T> ret = query(reader, builder, utils, logger);
    utils.rollback();
    utils.closeConnection();
    return ret;
  }
  
  public List<T> query(ResultSetReader<T> reader, SQLCommand builder, SQLUtils utils, Logger logger) 
        throws java.rmi.RemoteException {
    try {
      logger.info("Entering DBCommandsUncached.query()...");
      if (builder.sql.equals("")) {
        logger.error("DBCommandsUncached: Unable to build SQL Command.");
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      logger.info("DBCommandsUncached - SQL Builder-sql: " + builder.sql);
      int attemptsleft = 2;
      List<T> ret = null;
      try {
        while (attemptsleft >0) {
          ret = utils.query(builder.sql, builder.buildParameter(), reader);
          Exception e = utils.getLastException();
          if (e == null) {
            attemptsleft = 0;
            logger.info("Select returns " + ret.size() + " rows.");          
          } else {
            logger.error("Database Exception, no repeat will be tried :", e);
            throw new DPPWebserviceDatabaseException(e);          
          } 
        }
        return ret;
      } finally {        
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in DBCommandsUncached.query ", e);
    }
  }
  
  
  public T queryOneRowForManagement(ResultSetReader<T> reader, SQLCommand builder, 
      String schemaName, Logger logger) throws java.rmi.RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(schemaName, logger);
    FailoverFlag flag = FailoverTools.getCurrentFailover(schema, logger);
    SQLUtils utils = SQLUtilsCacheForManagement.createSQLUtilsForUncached(schema, flag, logger);    
    T ret = queryOneRow(reader, builder, utils, logger);
    utils.rollback();
    utils.closeConnection();
    return ret;
  }

  public T queryOneRow(ResultSetReader<T> reader, SQLCommand builder, SQLUtils utils, Logger logger)
          throws java.rmi.RemoteException {
    try {
      if (builder.sql.equals("")) {
        logger.error("DBCommands: Unable to build SQL Command.");
        throw new DPPWebserviceDatabaseException("Unable to build SQL Command.");
      }
      logger.info("SQL Builder-sql: " + builder.sql);
      try {
        int attemptsleft = 2;
        T ret = null;
        while (attemptsleft >0) {
          ret = utils.queryOneRow(builder.sql, builder.buildParameter(), reader);
          Exception e = utils.getLastException();
          if (e == null) {
            attemptsleft = 0;          
          } else {
            logger.error("Database Exception, no repeat will be tried :", e);
            throw new DPPWebserviceDatabaseException(e);          
          } 
        }
        return ret;    
      } finally {
        //SQLUtilsCache.release(container, logger);
      }
    } catch (java.rmi.RemoteException e) {
      throw e;
    } catch (Exception e) {
      logger.error("", e);
      throw new DPPWebserviceDatabaseException("Error in DBCommands.queryOneRow: ", e);
    }
  }
  
  
}
