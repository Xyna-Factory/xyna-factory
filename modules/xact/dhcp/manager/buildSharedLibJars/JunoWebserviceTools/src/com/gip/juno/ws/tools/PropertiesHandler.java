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

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.gip.juno.ws.exceptions.DPPWebserviceException;

/**
 * loads property files and generates respective Property class
 * (caches the content if necessary)
 */
public class PropertiesHandler {

  private static Logger _logger = Logger.getLogger("PropertiesHandler");

  private static final boolean _doCacheProperties = false;

  private static final String _dbFilename = "/xyna.db.properties";
  private static final String _wsFilename = "/xyna.ws.properties";

  private static Properties _dbProperties;

  public static Properties getWsProperties() throws java.rmi.RemoteException {
    return new PropertiesHandler().loadProperties(_wsFilename);
  }

  public static Properties getDBProperties() throws java.rmi.RemoteException {
    if (_doCacheProperties) {
      if (_dbProperties == null) {
        _dbProperties = new PropertiesHandler().loadProperties(_dbFilename);
      } else {
        _logger.info("Referencing cached properties...");
      }
      return _dbProperties;
    }
    return new PropertiesHandler().loadProperties(_dbFilename);
  }

  private Properties loadProperties(String filename) throws java.rmi.RemoteException {
    try {
      String path = "/etc/opt/xyna/environment";
      if(!Files.exists(Paths.get(path + filename))) {
        String homedir = System.getenv("HOME");
        path = homedir + "/environment";
        _logger.info("Choose path \"" + path + "\"");
        if(!Files.exists(Paths.get(path + filename))) {
          path = "";
          _logger.info("Choose path \"" + path + "\"");
        }
      }
      //InputStream instr = this.getClass().getResourceAsStream(filename);  //former version
      InputStream instr = new FileInputStream(path+filename);
      _logger.info("Input Stream read from \"" + path+filename + "\"");
      
      Properties prop = new Properties();
      prop.load(instr);
      instr.close();
      _logger.info("Successfully loaded properties from file.");
      return prop;
    } catch (Exception e) {
      _logger.error("Error while trying to load properties file" + filename, e);
      throw new DPPWebserviceException("Error while trying to load properties file " + filename, e);
    }
  }


  public static int getIntProperty(Properties properties, String key, Logger logger)
                throws java.rmi.RemoteException {
    String val = properties.getProperty(key);
    if (val == null) {
      throw new DPPWebserviceException("Property not found in file: " + key);
    }
    logger.info("Read property " + key + " : " + val);
    try {
      int ret = Integer.parseInt(val);
      return ret;
    }
    catch (Exception e) {
      throw new DPPWebserviceException("Could not parse property value (expected integer) for key " + key
                                       + ", value = " + val);
    }
  }
  
  
  public static boolean getBooleanProperty(Properties properties, String key, Logger logger)
                  throws java.rmi.RemoteException {
    String val = properties.getProperty(key);
    if (val == null) {
      throw new DPPWebserviceException("Property not found in file: " + key);
    }
    logger.info("Read property " + key + " : " + val);
    try {
      boolean ret = Boolean.parseBoolean(val);
      return ret;
    } catch (Exception e) {
      throw new DPPWebserviceException("Could not parse property value (expected integer) for key " + key + ", value = " + val);
    }
  }


  public static String getProperty(Properties properties, String key, Logger logger)
                throws java.rmi.RemoteException {
    String val = properties.getProperty(key);
    if (val == null) {
      throw new DPPWebserviceException("Property not found in file: " + key);
    }
    logger.info("Read property " + key + " : " + val);
    return val;
  }


}
