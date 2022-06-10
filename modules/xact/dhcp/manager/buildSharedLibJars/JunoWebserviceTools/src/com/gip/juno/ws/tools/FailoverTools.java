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

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;

import java.io.*;
import java.rmi.RemoteException;
import java.util.HashMap;


class LastFailoverData {
  
  public long time = 0;
  public FailoverFlag flag = FailoverFlag.primary;
}

/**
 * class that checks which one of two failover instances should be used;
 * does that by reading appropriate file;
 * but only if 10 seconds have passed since last check, otherwise last value is returned
 * 
 */
public class FailoverTools {
  
  private static final String _path = "/var/run/";
  private static final String _prefix = "failover_";
    
  
  private static HashMap<String, LastFailoverData> _lastFailover = new HashMap<String, LastFailoverData>(); 
    
  private static boolean _reloadCaches = false;
  
  /**
   * returns current value of _reloadCaches, but sets _reloadCaches to false afterwards 
   */
  public static boolean popReloadCachesFlag(Logger logger) {
    boolean ret;    
    ret = _reloadCaches;
    if (ret == true) {
      logger.info("FailoverTools: ReloadCachesFlag = " + _reloadCaches);
    }
    _reloadCaches = false;
    return ret;
  }
  
  public static FailoverFlag getCurrentFailover(DBSchema schema, Logger logger) throws RemoteException {
    if (schema == null) {
      return FailoverFlag.primary;
    }
    return getFailover(getSuffix(schema, logger), logger);
  }
    
  public static FailoverFlag getCurrentFailover(String location, Logger logger) throws RemoteException {
    if ((location == null) || (location.trim().equals(""))) {
      return FailoverFlag.primary;
    }
    return getFailover(getSuffix(location, logger), logger);
  }
  
  public static void forceCheck(DBSchema schema, Logger logger) throws RemoteException {
    String suffix = getSuffix(schema, logger);
    forceCheckForSuffix(suffix, logger);
  }

  public static void forceCheck(String location, Logger logger) throws RemoteException {
    String suffix = getSuffix(location, logger);
    forceCheckForSuffix(suffix, logger);
  }
    
  private static void forceCheckForSuffix(String suffix, Logger logger) throws RemoteException {
    FailoverFlag lastFlag = getLastFailover(suffix, logger).flag;
    FailoverFlag ret = checkFile(suffix, logger);        
    setLastFailover(suffix, ret, logger);
    checkSwitch(lastFlag, ret, logger); 
  }
  
  private static FailoverFlag getFailover(String suffix, Logger logger) throws RemoteException {
    FailoverFlag ret = FailoverFlag.primary;
    logger.info("Requested: Failover for suffix " + suffix);
//    try {
//      LastFailoverData lastFailover = getLastFailover(suffix, logger);
//      FailoverFlag lastFlag = lastFailover.flag;
//      if (newCheckNecessary(lastFailover, logger)) {
//        ret = checkFile(suffix, logger);        
//        setLastFailover(suffix, ret, logger);
//        checkSwitch(lastFlag, ret, logger);        
//      } else {
//        ret = lastFailover.flag;
//      }           
//    } catch (Exception e) {
//      logger.error(e);
//    }
//    if (ret == null) {
//      return FailoverFlag.primary;
//    }
    logger.info("Returned Failover for suffix " + suffix + " = " + ret);
    return ret;
  }
  
  private static void checkSwitch(FailoverFlag oldflag, FailoverFlag newflag, Logger logger) 
        throws RemoteException {
    if (oldflag != newflag) {
      logger.info("Switching to other failover instance, emptying SQLUtils Cache...");
      //SQLUtilsCache.reload();
      reloadAll(logger);
    }
  }
  
  public static void reloadAll(Logger logger) throws RemoteException {
    _reloadCaches = true;
  }
  
  private static boolean newCheckNecessary(LastFailoverData lastFailover, Logger logger) {
    long timeDiff = System.currentTimeMillis() - lastFailover.time;
    logger.info("Milliseconds since last failover check: " + timeDiff);
    if (timeDiff >= 10000) {
      return true;
    }    
    return false;
  }
    
  private static FailoverFlag checkFile(String suffix, Logger logger) throws RemoteException {
    return checkLine(getFirstLineOfFile(getFilename(suffix, logger), logger), logger);
  }
  
  private static FailoverFlag checkLine(String line, Logger logger) throws RemoteException {
    if (line.indexOf("1") >= 0) {
      return FailoverFlag.primary;
    } else if (line.indexOf("2") >= 0) {
      return FailoverFlag.secondary;
    } else {
      logger.error("Both Failover instances may be unreachable, trying primary...");
      reloadAll(logger);
    }
    return FailoverFlag.primary;
  }
  
  private static String getFirstLineOfFile(String filename, Logger logger) throws java.rmi.RemoteException {
    logger.info("Reading file " + filename);
    try {
      BufferedReader fin = new BufferedReader(new FileReader(filename));
      String line = fin.readLine();      
      if (line == null) {
        logger.error("file " + filename + " is empty.");
      }
      fin.close();
      logger.info("Read in Failover file: " + line);
      return line;
    } catch (IOException e) {
      logger.error("", e);
    }
    return "";
  }
  
  private static String getSuffix(String location, Logger logger) {
    if (location.equals(Constants.managementName)) {
      return "mgmt";
    }
    return location;  
  }
  
  private static String getSuffix(DBSchema schema, Logger logger) {
//    if (schema == DBSchema.dns) {
//      return "dns"; 
//    }
    return "mgmt";  
  }
    
  private static String getFilename(String suffix, Logger logger) {
    return _path + _prefix + suffix;
  }
  
  private static void setLastFailover(String suffix, FailoverFlag flag, Logger logger) {
    if ((suffix == null) || (suffix.trim().equals(""))) {
      return;
    }
    if (flag == null) {
      return;
    }
    LastFailoverData data = new LastFailoverData();
    data.flag = flag;
    data.time = System.currentTimeMillis();
    _lastFailover.put(suffix, data);
  }
  

  private static LastFailoverData getLastFailover(String suffix, Logger logger) {
    if ((suffix == null) || (suffix.trim().equals(""))) {
      return new LastFailoverData();
    }
    LastFailoverData data = _lastFailover.get(suffix);
    if (data == null) {
      return new LastFailoverData();
    }
    return data;   
  }
  
  public static void testFailover(Logger logger) throws RemoteException {
    String mngmt = getFirstLineOfFile("/var/run/failover_mgmt", logger);
    logger.info("Read in file: " + mngmt);
  }
  
}
