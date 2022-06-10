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
package gip.base.db.drivers;

import gip.base.common.OBException;
import gip.base.db.*;

/**
 * Klasse, die einen Treiber verwaltet und die Operationen kapselt.
 */
public class OBDriver {
  
  public static final String INITIALIZATION_ERROR = "Driver not initialized"; //$NON-NLS-1$
  
  public static String getTableName(OBContext context, String schema, String tableName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    
    return context.getDriver().getTableName(schema, tableName);
  }

  public static String getNextKeyVal(OBContext context, String schema, String sequenceName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getNextKeyVal(schema,sequenceName);
  }

  public static String getNextKeyValStatement(OBContext context, String schema, String sequenceName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getNextKeyValStatement(schema,sequenceName);
  }

  public static String getCurrentKeyVal(OBContext context, String schema, String sequenceName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getCurrentKeyVal(schema,sequenceName);
  }
  
  public static String getCurrentKeyValStatement(OBContext context, String schema, String sequenceName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getCurrentKeyValStatement(schema,sequenceName);
  }
  
  public static String getSysDateString(OBContext context) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getSysDateString();
  }
  
  public static String getSysTimeStampString(OBContext context) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getSysTimeStampString();
  }

  public static String getEmptyClob(OBContext context) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getEmptyClob();
  }

  public static String getEmptyBlob(OBContext context) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getEmptyBlob();
  }

  public static void createUser(OBContext context, String userName, String pw) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    context.getDriver().createUser(context, userName, pw);
  }
  
  public static void dropUser(OBContext context, String userName) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    context.getDriver().dropUser(context, userName);
  }

  public static String getMaxRows(OBContext context, int maxRows) throws OBException {
    if (context.getDriver()==null) {
      throw new OBException(OBException.OBErrorNumber.driverNotInitialized);
    }
    return context.getDriver().getMaxRows(maxRows);
  }
}
