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

import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceDBConnectionCreationException;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.SQLUtils;

public class SQLUtilsCacheForLocation {

  private HashMap<String, FailoverStack> _stacks;
  
  private LocationSchema _schema;
  
  public SQLUtilsCacheForLocation(LocationSchema schema, Logger logger) throws RemoteException {
    init(schema, logger);
  }
  
  private void init(LocationSchema schema, Logger logger) throws RemoteException {
    _schema = schema;
    logger.info("SQLUtilsCacheLocation (schema =" + _schema + "): Starting init...");
    _stacks = new HashMap<String, FailoverStack>();
    
  }
  
  private void initLocation(String location, Logger logger) {
    logger.info("SQLUtilsCacheLocation (schema =" + _schema + "): Starting init for location " + location);
    FailoverStack stack = new FailoverStack();
    _stacks.put(location, stack);
  }

  public SQLUtilsContainerForLocation getContainer(String location, FailoverFlag flag, LocationSchema schema, 
          Logger logger) throws RemoteException {
    logger.info("SQLUtilsCacheLocation (schema =" + _schema + ").getForLocation: Location = " + location);
    
    FailoverStack stack = _stacks.get(location);
    if (stack == null) {      
      initLocation(location, logger);
      stack = _stacks.get(location);
      if (stack == null) {        
        logger.error("Requested Stack for location " + location + "does not exist.");
        throw new DPPWebserviceDatabaseException("Error in getting database connection for location " 
            + location);
      }  
    }    
    SQLUtils utils;
    if (stack.empty(flag)) {
      //utils = createSQLUtils(location, flag, schema, logger);
      return getFresh(location, flag, schema, logger);
    } else {
      utils = stack.pop(flag);
      logger.info("Fetched existing SQLUtils from Cache for location= " + location + ", Failover= " 
          + flag.toString() + "; (Currently are still " + stack.size(flag) + " SQLUtils stored in this stack.)");
    }
    SQLUtilsContainerForLocation ret = new SQLUtilsContainerForLocation(utils, location, flag, 
          schema); 
    return ret;
  }
  

  public SQLUtilsContainerForLocation getFresh(String location, FailoverFlag flag, LocationSchema schema, 
        Logger logger) throws RemoteException {
    SQLUtils utils = null;
    FailoverFlag usedFlag = flag;
    try {
      utils = createSQLUtils(location, usedFlag, schema, logger);
    }
    catch (DPPWebserviceDBConnectionCreationException e) {
      throw new DPPWebserviceDatabaseException(e);
      /*
      usedFlag = FailoverFlag.mirror(flag);
      FailoverTools.forceCheck(location, logger);
      logger.info("Failed to create new SQLUtils for for Location " + location + ", Failover " + flag 
          + ", now trying with Failover " + usedFlag); 
      utils = createSQLUtils(location, usedFlag, schema, logger);
      */
    }
    SQLUtilsContainerForLocation ret = new SQLUtilsContainerForLocation(utils, location, usedFlag, schema);
    return ret;
  }
  

  public void release(SQLUtilsContainerForLocation container, Logger logger) 
        throws RemoteException {
    
    logger.info("Releasing SQLUtils to Cache (schema =" + _schema + ") for location " + container.getLocation());
    FailoverStack stack = _stacks.get(container.getLocation());
    if (stack == null) {
      logger.error("Requested Stack for location " + container.getLocation() + "does not exist.");
    }
    SQLUtilsCache.releaseSQLUtils(container, stack, logger);
  }
  

  private SQLUtils createSQLUtils(String location, FailoverFlag flag, LocationSchema schema, Logger logger) 
        throws RemoteException {
    logger.info("Starting to create new SQLUtils (schema =" + _schema + ") for Location " + location + ", Failover "
        + flag.toString());
    LocationData locationData = LocationData.getInstance(schema, logger);    
    ConnectionInfo conndata = locationData.get(location, flag, logger);
    try {
      return SQLUtilsCache.createSQLUtils(conndata, logger);
    }
    catch (DPPWebserviceDBConnectionCreationException e) {
      throw e;
    }
  }
}
