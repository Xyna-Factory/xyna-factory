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
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.exceptions.DPPWebserviceDBConnectionCreationException;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.SQLUtils;

public class SQLUtilsCacheForManagement {
  
  private HashMap<DBSchema, FailoverStack> _stacks; 
  
  public SQLUtilsCacheForManagement(Logger logger) throws RemoteException {
    ManagementData.reload(logger);
    init(logger);
  }
  
  private void init(Logger logger) {
    logger.info("SQLUtilsCache: Starting initManagement...");
    _stacks = new HashMap<DBSchema, FailoverStack>();
    for (DBSchema schema : DBSchema.values()) {
      FailoverStack stack = new FailoverStack();
      _stacks.put(schema, stack);
    }
  } 
  

  public SQLUtilsContainerForManagement getContainer(DBSchema dbschema, FailoverFlag flag, Logger logger) 
          throws RemoteException {
    logger.info("SQLUtilsCache.getForManagement: DBSchema = " + dbschema.toString());
    
    FailoverStack stack = _stacks.get(dbschema);
    if (stack == null) {
      logger.error("SQLUtilsCache: Requested Stack for admin DBSchema " + dbschema + "does not exist.");
      throw new DPPWebserviceDatabaseException("SQLUtilsCache: Error in getting database connection for admin database " 
          + dbschema);
    }
    SQLUtils utils;
    if (stack.empty(flag)) {      
      //utils = createSQLUtils(dbschema, flag, logger);
      return getFresh(dbschema, flag, logger);
    } else {
      utils = stack.pop(flag);
      logger.info("Fetched existing SQLUtils from Cache for dbschema= " + dbschema.toString() + ", Failover= " 
          + flag.toString()+ "; (Currently are still " + stack.size(flag) + " SQLUtils stored in this stack.)");
    }
    SQLUtilsContainerForManagement ret = new SQLUtilsContainerForManagement(utils, dbschema, flag); 
    return ret;
  }
  
  
  public static SQLUtilsContainerForManagement getFresh(DBSchema schema, FailoverFlag flag, Logger logger) 
        throws RemoteException {    
    logger.debug("in SQLUtilsContainerForManagement.getFresh");
    SQLUtils utils = null;
    FailoverFlag usedFlag = flag;
    try {
      utils = createSQLUtils(schema, usedFlag, logger);
    }
    catch (DPPWebserviceDBConnectionCreationException e) {
      throw new DPPWebserviceDatabaseException(e);
      /*
      usedFlag = FailoverFlag.mirror(flag);
      FailoverTools.forceCheck(schema, logger);
      logger.info("Failed to create new SQLUtils for for DBSchema " + schema.toString() + ", Failover " + flag 
          + ", now trying with Failover " + usedFlag); 
      utils = createSQLUtils(schema, usedFlag, logger);
      */
    }
    SQLUtilsContainerForManagement ret = new SQLUtilsContainerForManagement(utils, schema, usedFlag);
    return ret;
  }
  

  public void release(SQLUtilsContainerForManagement container, Logger logger) throws RemoteException {
    
    logger.info("Releasing SQLUtils to Cache for dbschema " + container.getDBSchema());
    FailoverStack stack = _stacks.get(container.getDBSchema());
    if (stack == null) {
      logger.error("SQLUtilsCache: Requested Stack for dbschema " + container.getDBSchema() + "does not exist.");
    }
    SQLUtilsCache.releaseSQLUtils(container, stack, logger);
  }
  

  private static SQLUtils createSQLUtils(DBSchema schema, FailoverFlag flag, Logger logger) 
        throws RemoteException {
    logger.info("Starting to create new SQLUtils for DBSchema " + schema.toString() + ", Failover "
        + flag.toString());
    ConnectionInfo conndata = ManagementData.get(schema, flag, logger);
    try {
      return SQLUtilsCache.createSQLUtils(conndata, logger);
    }
    catch (DPPWebserviceDBConnectionCreationException e) {
      throw e;      
    }
  }
  
  public static SQLUtils createSQLUtilsForUncached(DBSchema schema, FailoverFlag flag, Logger logger) 
        throws RemoteException {
    return getFresh(schema, flag, logger).getSQLUtils();
  }
  
}
