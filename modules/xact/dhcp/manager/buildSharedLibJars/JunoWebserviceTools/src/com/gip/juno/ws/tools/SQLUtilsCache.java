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
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceDatabaseException;
import com.gip.xyna.utils.db.*;

class SQLUtilsCacheLogger implements SQLUtilsLogger {
  private Logger _logger;
  public SQLUtilsCacheLogger(Logger logger) {
    _logger = logger;
  }
  public void logException(Exception e) {
    _logger.error("", e);
  }    
  public void logSQL(String sql) {
    _logger.info("SQL= " + sql);
  } 
}

/**
 * class used to cache open database connections
 */
public class SQLUtilsCache {
  
  /**
   * default maximum number of SQLUtils that may be cached in a stack; 
   * only used after error reading value from property file
   */
  public static final int DBCONN_STACK_MAX_SIZE_DEFAULT = 5;
  
  public static final String DBCONN_STACKSIZE_PROP = "db.connections.cache.stack.size";

  private static class Caches {
    private static Caches _instance;
    public SQLUtilsCacheForLocation serviceCache;
    public SQLUtilsCacheForManagement managementCache;
    private Caches() throws RemoteException { init(); }
    private void init() throws RemoteException {
      managementCache = new SQLUtilsCacheForManagement(_logger);  
      serviceCache = new SQLUtilsCacheForLocation(LocationSchema.service, _logger);
    }
    public static Caches getInstance() throws RemoteException {      
      if (_instance == null) {
        _instance = new Caches();
      }
      if (FailoverTools.popReloadCachesFlag(_logger)) {
        _logger.info("Reload - Flag in FailoverTools is set, going to reload..."); 
        _instance.init();  
      }
      return _instance;
    }    
    public void refreshAll(Logger logger) throws RemoteException {
      init();
    }    
    public void reloadLocations(Logger logger) throws RemoteException {
      getInstance().serviceCache = new SQLUtilsCacheForLocation(LocationSchema.service, logger);
    }
  }
    
  private static Logger _logger = Logger.getLogger("SQLUtils");
  
  private static SQLUtilsCacheForLocation getServiceCache() throws RemoteException {    
    return Caches.getInstance().serviceCache; 
  }

  private static SQLUtilsCacheForManagement getManagementCache() throws RemoteException {    
    return Caches.getInstance().managementCache; 
  }
  
  private static SQLUtilsLogger getLogger(Logger logger) {
    return new SQLUtilsCacheLogger(_logger);
  }
    
  
  public static SQLUtilsContainerForManagement getForManagement(String schemaName, Logger logger) 
          throws RemoteException {
    DBSchema schema = ManagementData.translateDBSchemaName(schemaName, logger);
    return getForManagement(schema, logger);
  }
  
  public static SQLUtilsContainerForManagement getForManagement(DBSchema dbschema, Logger logger) 
          throws RemoteException {
    FailoverFlag flag = FailoverTools.getCurrentFailover(dbschema, logger);
    return getManagementCache().getContainer(dbschema, flag, logger);
  }
  
  public static SQLUtilsContainerForManagement getForManagement(DBSchema dbschema, FailoverFlag flag, Logger logger) 
          throws RemoteException {
    return getManagementCache().getContainer(dbschema, flag, logger);
  }
  
  public static SQLUtilsContainerForLocation getForLocation(String location, LocationSchema schema, Logger logger) 
          throws RemoteException {
    FailoverFlag flag = FailoverTools.getCurrentFailover(location, logger);
    return getForLocation(location, schema, flag, logger);
  }
  
  public static SQLUtilsContainerForLocation getForLocation(String location, LocationSchema schema, FailoverFlag flag, 
          Logger logger) throws RemoteException {   
    if (schema == LocationSchema.service) {
      return getServiceCache().getContainer(location, flag, schema, logger);
    } else {
      throw new DPPWebserviceDatabaseException("SQLUtilsCache: Requested schema does not exist.");
    }
  }
  
  public static SQLUtilsContainer getFresh(SQLUtilsContainer container, Logger logger) 
          throws RemoteException {
    if (container instanceof SQLUtilsContainerForManagement) {
      DBSchema schema = ((SQLUtilsContainerForManagement) container).getDBSchema();
      return getFreshForManagement(schema, container.getFailOverFlag(), logger);
    } else if (container instanceof SQLUtilsContainerForLocation) {
      SQLUtilsContainerForLocation casted = (SQLUtilsContainerForLocation) container;      
      return getFreshForLocation(casted.getLocation(), casted.getSchema(), casted.getFailOverFlag(), logger);
    } else {
      throw new RemoteException("SQLUtilsCache.GetFresh: Illegal container type.");
    }
  }
  
  public static SQLUtilsContainerForLocation getFreshForLocation(String location, LocationSchema schema, 
        FailoverFlag flag, Logger logger) throws RemoteException {    
    if (schema == LocationSchema.service) {
      return getServiceCache().getFresh(location, flag, schema, logger);
    } else {
      throw new RemoteException("SQLUtilsCache.GetFreshForLocation: Illegal Location Schema.");
    }
  }
  
  public static SQLUtilsContainerForManagement getFreshForManagement(DBSchema schema, FailoverFlag flag, Logger logger) 
        throws RemoteException {
    //return getManagementCache().getFresh(schema, flag, logger);
    return SQLUtilsCacheForManagement.getFresh(schema, flag, logger);
  }
  
  public static void release(SQLUtilsContainer container, Logger logger) 
        throws RemoteException {
    if (container instanceof SQLUtilsContainerForManagement) {
      SQLUtilsContainerForManagement casted = (SQLUtilsContainerForManagement) container;
      getManagementCache().release(casted, logger);
    } else if (container instanceof SQLUtilsContainerForLocation) {
      SQLUtilsContainerForLocation casted = (SQLUtilsContainerForLocation) container;     
      if (casted.getSchema() == LocationSchema.service) {
        getServiceCache().release(casted, logger);
      } else { 
        throw new RemoteException("SQLUtilsCache.release: Illegal Location Schema.");
      }
    } else {
      throw new RemoteException("SQLUtilsCache.release: Illegal container type.");
    }
  }
  
  
  public static void releaseSQLUtils(SQLUtilsContainer container, FailoverStack stack, Logger logger) {
    if (container == null) {
      return;
    }
    if (container.getSQLUtils() == null) {
      return;
    }
    if (stack == null) {
      return;
    }
    try {
      container.getSQLUtils().rollback();
      if (stack.size(container.getFailOverFlag()) < getStackMaxSize(logger)) {
        stack.push(container);
        logger.info("Released SQLUtils, this stack has now " + stack.size(container.getFailOverFlag()) + " elements.");
      } else {
        logger.info("Stack has reached limit with " + stack.size(container.getFailOverFlag()) + " elements"
            + ", not going to store SQLUtils.");
        container.getSQLUtils().closeConnection();
      }
    }
    catch (Exception e) {
      logger.error(e);
    }
  }
   
  private static int getStackMaxSize(Logger logger) {
    try {
      Properties prop = PropertiesHandler.getWsProperties();      
      int ret = Integer.parseInt(prop.getProperty(DBCONN_STACKSIZE_PROP));
      logger.info("Read max stack size = " + ret);
      return ret;
    } 
    catch (Exception e) {
      logger.error(e);
      return DBCONN_STACK_MAX_SIZE_DEFAULT;
    }
  }
    
  public static SQLUtils createSQLUtils(ConnectionInfo conn, Logger logger) throws RemoteException {
    return createSQLUtils(conn.user, conn.password, conn.url, logger);
  }
  
  public static SQLUtils createSQLUtils(String user, String password, String url, Logger logger) 
        throws RemoteException {
    try {
      if ((user==null) || (user.trim().equals(""))) {
        throw new SQLException("Empty username.");
      }
      if ((url==null) || (url.trim().equals(""))) {
        throw new SQLException("Empty JDBC-URL.");
      }
      logger.info("Creating new SQLUtils for user= " + user + ", url = " + url);      
      int connTimeout = 5;
      int socketTimeout = 4;
      try {
        Properties prop = PropertiesHandler.getWsProperties();
        connTimeout = Integer.parseInt(prop.getProperty("db.connect.timeout.seconds"));
        socketTimeout = Integer.parseInt(prop.getProperty("db.socket.timeout.seconds"));
        logger.info("Using property db.connect.timeout.seconds = " + connTimeout);
        logger.info("Using property db.socket.timeout.seconds = " + socketTimeout);
      } catch (Exception e) {        
        logger.warn("Failure trying to read timeout properties. ", e);
      }      
      DBConnectionData dbd = DBConnectionData.newDBConnectionData().user(user)
          .password(password).url(url).connectTimeoutInSeconds(connTimeout).socketTimeoutInSeconds(
          socketTimeout).build();
      SQLUtils sqlUtils = dbd.createSQLUtils(getLogger(logger));
      if (sqlUtils == null) {
        logger.error("Creating SQLUtils has failed.");
        throw new SQLException("Unable to create SQLUtils.");
        //throw new DPPWebserviceDBConnectionCreationException("Unable to create SQLUtils.");
      }
      return sqlUtils;    
    }
    /*
    catch (RemoteException e) {
      throw e;
    } 
    */
    catch (Exception e) {
      logger.error("createSQLUtils: Failure to open DB Connection. User = " + user + ", url = " 
          + url + " : ", e);
      throw new DPPWebserviceDatabaseException("Failure to open DB Connection. ", e);
    }    
  }
  
  public static void reload() throws RemoteException {
    Caches.getInstance().refreshAll(_logger);
  }
}
