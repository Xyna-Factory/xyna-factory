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
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.*;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;

/**
 * class that stores connection data for the management databases
 * (read from property file)
 * 
 */
public class ManagementData {
  
  private static HashMap<DBSchema, FailoverData> _schemas = null;
  private static boolean isInitFinished = false;
  

  public static String show() {
    StringBuilder ret = new StringBuilder();
    ret.append("ManagementData { ");
    for (Map.Entry<DBSchema, FailoverData> entry : _schemas.entrySet()) {
      ret.append("\n DBSchema: " + entry.getKey().toString() + " -> " + entry.getValue().toString());
    }
    ret.append("} \n");
    return ret.toString();
  }
  
  private static void init(Logger logger) throws RemoteException {
      _schemas = new HashMap<DBSchema, FailoverData>();
      logger.info("DBAdmindata: Starting init...");
      Properties properties = PropertiesHandler.getDBProperties();
      try {
        for (DBSchema schema : DBSchema.values()) {
          for (FailoverFlag flag: FailoverFlag.values()) {
            String propbase = "db." + schema.toString() + "." + flag.toString();
            String userprop = propbase + ".user";
            String passwordprop = propbase + ".password";
            String urlprop = propbase + ".url";
            logger.info("Trying to read property : " + userprop);
            String user = properties.getProperty(userprop);
            if ((user == null) || (user.trim().equals(""))) {
              logger.error("Cannot read property user for DBSchema " + schema.toString());
              throw new RemoteException("Cannot read property user for DBSchema " + schema.toString());
            }
            logger.info(userprop + " = " + user);
            logger.info("Trying to read property : " + passwordprop);
            String password = properties.getProperty(passwordprop);
            if ((password == null) || (user.trim().equals(""))) {
              logger.error("Cannot read property password for DBSchema " + schema.toString());
              throw new RemoteException("Cannot read property password for DBSchema " + schema.toString());
            }
            //logger.info(passwordprop + " = " + password);
            logger.info("Trying to read property : " + urlprop);
            String url = properties.getProperty(urlprop);
            if ((url == null) || (user.trim().equals(""))) {
              logger.error("Cannot read property url for DBSchema " + schema.toString());
              throw new RemoteException("Cannot read property url for DBSchema " + schema.toString());
            }
            logger.info(urlprop + " = " + url);
            ConnectionInfo info = new ConnectionInfo();
            info.user = user;
            info.password = password;
            info.url = url;
            add(schema, info, flag, logger);
          }
        }
        logger.info("Created ManagementData: \n" + show());
      } catch (RemoteException e) {
        logger.error("ManagementData.init: ", e);
        throw e;
      } catch (Exception e) {
        logger.error("ManagementData.init: ", e);
        throw new DPPWebserviceUnexpectedException("Error while initialising management connection data.", e);
      }
      logger.info("Setting isInitFinished to true.");
      isInitFinished = true;
      
  }
  
  private static void checkInit(Logger logger) throws RemoteException {
    synchronized (ManagementData.class) {
      logger.info("Synchronized checking Init ...");
      if (isInitFinished == false) {
        logger.info("isInitFinished false. Starting init ...");
        init(logger);
      }
    }
  }
  
  public static DBSchema translateDBSchemaName(String schemaName, Logger logger) throws RemoteException {
    for (DBSchema schema : DBSchema.values()) {
      if (schema.toString().equals(schemaName)) {
        return schema;
      }
    }
    logger.error("Translate DBSchemaName: DBSchema Name " + schemaName 
        + "does not exist!");
    throw new java.rmi.RemoteException("Translate DBSchemaName: DBSchema Name " + schemaName 
        + "does not exist!");
  }
  
  public static FailoverData get(DBSchema schema, Logger logger) throws RemoteException {
    checkInit(logger);
    return _schemas.get(schema);
  }
  
  public static ConnectionInfo get(DBSchema schema, FailoverFlag flag, Logger logger) throws RemoteException {
    checkInit(logger);
    return _schemas.get(schema).get(flag);
  }
  
  private static void add(DBSchema schema, ConnectionInfo data, FailoverFlag flag, Logger logger) 
        throws RemoteException {
    //checkInit(logger);
    FailoverData targetData = _schemas.get(schema); 
    if (targetData == null) {
      FailoverData failoverdata = new FailoverData();
      failoverdata.set(data, flag);
      _schemas.put(schema, failoverdata);
    } else {
      targetData.set(data, flag);
    }
  }
  
  public static void reload(Logger logger) throws RemoteException {
    synchronized (ManagementData.class) {
      logger.info("Synchronized Reload called");
      //checkInit(logger);
      init(logger);
    }
  }
  
}
