/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import gip.base.db.OBContext;
import gip.base.db.OBDatabase;
import gip.base.db.OBTableObject;
import gip.base.db.XynaContextFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.apache.log4j.Logger;

/**
 * OBOracleDriver
 */
public class OBMySQLDriver implements OBDriverInterface {

  private transient static Logger logger = Logger.getLogger(OBMySQLDriver.class);
  
  
  /** Uebersetzung des Datenmodell-Tabellennamens in den SQL-Namen
   * @param schema
   * @param tableName Der Tabellen-Name aus dem Daten-Modell
   * @return tableName, wie er bei DB-Operationen angegeben werden muss, z.B. USER.iTabelName
   * @throws OBException
   */
  public String getTableName(String schema, String tableName) throws OBException {
    if (schema == null) return tableName;
    return schema + "." + tableName; //$NON-NLS-1$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#getNextKeyVal(java.lang.String, java.lang.String)
   */
  public String getNextKeyVal(String schema, String sequenceName) throws OBException {
    return schema +".nextVal('"+sequenceName+"')"; //$NON-NLS-1$ //$NON-NLS-2$
  }


  /**
   * @see gip.base.db.drivers.OBDriverInterface#getCurrentKeyVal(java.lang.String, java.lang.String)
   */
  public String getCurrentKeyVal(String schema, String sequenceName) throws OBException {
    return schema + ".currVal('"+sequenceName+"')"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#getNextKeyValStatement(java.lang.String, java.lang.String)
   */
  public String getNextKeyValStatement(String schema, String sequenceName) throws OBException {
    return "SELECT "+ getNextKeyVal(schema, sequenceName); //$NON-NLS-1$ //$NON-NLS-2$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#getCurrentKeyValStatement(java.lang.String, java.lang.String)
   */
  public String getCurrentKeyValStatement(String schema, String sequenceName) throws OBException {
    return "SELECT " + getCurrentKeyVal(schema, sequenceName); //$NON-NLS-1$ //$NON-NLS-2$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#getSysDateString()
   */
  public String getSysDateString() throws OBException {
    return "now()"; //$NON-NLS-1$
  }
  
  public String getSysTimeStampString() throws OBException {
     return "now()"; //$NON-NLS-1$
  }

  public String getEmptyClob() throws OBException {
    return "''"; //$NON-NLS-1$
  }

  public String getEmptyBlob() throws OBException {
    return "NULL"; //$NON-NLS-1$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#createUser(gip.base.db.OBContext, java.lang.String, java.lang.String)
   */
  public void createUser(OBContext context, String userName, String pw) throws OBException {
    OBTableObject.execSQL(context, "CREATE USER " + userName + " IDENTIFIED BY " + pw + //$NON-NLS-1$ //$NON-NLS-2$
                          " DEFAULT TABLESPACE DATA " + //$NON-NLS-1$
                          " TEMPORARY TABLESPACE TEMPORARY_DATA"); //$NON-NLS-1$
    OBTableObject.execSQL(context, "GRANT CREATE SESSION TO " + userName); //$NON-NLS-1$
    OBTableObject.execSQL(context, "GRANT IPNET TO " + userName);     //$NON-NLS-1$

  }
  
  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#dropUser(gip.base.db.OBContext, java.lang.String)
   */
  public void dropUser(OBContext context, String userName) throws OBException {
    OBTableObject.execSQL(context, "DROP USER " + userName); //$NON-NLS-1$
  }

  
  /**
   * @see gip.base.db.drivers.OBDriverInterface#getMaxRows(int)
   */
  public String getMaxRows(int maxRows) throws OBException {
    return " LIMIT " + maxRows; //$NON-NLS-1$
  }


  /**
   * Liefert die JDBC-Version zur�ck.
   * @param context
   * @return JDBC-Version
   * @throws OBException
   */
  public static String getJDBCVersion(OBContext context) throws OBException {
    try {
      // Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
      DatabaseMetaData dmd = context.getDataConnection().getMetaData();
      logger.debug(XynaContextFactory.getSessionData(context) + "JDBC Driver version: " + dmd.getDriverVersion());//$NON-NLS-1$
      return dmd.getDriverVersion();
    }
    catch(Exception e) {
      logger.debug(XynaContextFactory.getSessionData(context) + " Exception" , e );//$NON-NLS-1$
    }
    return ""; //$NON-NLS-1$
  }


  /**
   * Register Driver
   */
  @Override
  public void registerDriver() {
    try {
      Class.forName("com.mysql.jdbc.Driver").newInstance();
    }
    catch (Exception e) {
      logger.error("com.mysql.jdbc.Driver not found", e);
    }
  }

  /** Set Constraints on initially deferred. Not possible for MySQL
   * @param con Connection 
   */
  @Override
  public void setConstaintsDeferred(OBDatabase con) {
    // Do Nothing
  }
  
  /**
   * Setzt das Feld v$session.client_info fuer eine Connections nach dem Schema
   * <staffName>:<sessionId>:<xyzCon>
   * @throws OBException
   */
  @Override
  public void setSessionInfo(Connection con, String info) {
    // Do Nothing
  }
  
  /**
   * Setzt die Felder v$session.module und v$session.action fuer die Connection.
   * @param module 
   * @param action 
   * @throws OBException
   */
  @Override
  public void setVSessionActionInfo(Connection con, String module, String action) throws OBException {
    // Do Nothing
  }
}
