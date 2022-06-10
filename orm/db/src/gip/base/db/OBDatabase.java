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
package gip.base.db;

import gip.base.common.OBAttribute;
import gip.base.common.OBException;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;

/** 
 * OBConfig enthaelt Daten welche pro Anmeldung spezifisch sind 
 */
public class OBDatabase implements OBConnectionInterface {
  
  private transient static Logger logger = Logger.getLogger(OBDatabase.class);
  
  /** Verbindung(en) zur Datenbank */
  private Connection con; 

  /** */
  private Vector<OBTableObject> validateList;

  
  /**
   * @param _con
   */
  public OBDatabase(Connection _con) {
    this.con = _con;
    validateList = new Vector<OBTableObject>();
  }


  /**
   * Liefert die Orginal-Connection.
   * sd (fuer NativeAQ)
   * @return Connection
   */
  public Connection getCon() { 
    return con; 
  } 
  

  /**
   * Wirft IMMER eine SQLException.
   * @deprecated Use commit(OBContext) instead!
   * @see java.sql.Connection#commit()
   */
  public void commit() throws SQLException {
    throw new SQLException("Don't use commit(), use commit(OBContext)!");//$NON-NLS-1$
  }
  
  
  /**
   * @param context
   * @throws OBException
   * @see java.sql.Connection#commit()
   */
  public void commit(OBContext context) throws OBException {
    validateDeferrable(context);
    
    try {
      // logger.debug("COMMIT"); 
      con.commit();
    }
    catch (SQLException e1) {
      logger.error(XynaContextFactory.getSessionData(context) + "error commiting", e1);//$NON-NLS-1$

      String tmp="";  //$NON-NLS-1$
      try {
        tmp = OBDBObject.handleSQLException(context, e1, this);
        
      }
      catch (OBException e2) {
        logger.error(XynaContextFactory.getSessionData(context) + "error commiting", e2);//$NON-NLS-1$
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {tmp});
    }
  }

  
  /**
   * @see java.sql.Connection#rollback()
   */
  public void rollback() throws SQLException {
    logger.debug("ROLLBACK"); //$NON-NLS-1$
    con.rollback();
    validateList.clear();
  }


  /**
   * Extra Methoden fuer validateDeferrable(.)
   * @param toValidate
   */
  public void registerObject(OBTableObject toValidate) {
    if (toValidate.runValidateDeferrable) {
      int index = validateListContains(toValidate);
      if (index==OBAttribute.NULL) {
        validateList.add(toValidate);
      }
      else {
        try {
          validateList.elementAt(index).copyAll(toValidate);
        }
        catch (OBException e) {
          logger.error("error registering object",e);//$NON-NLS-1$
          validateList.add(toValidate);
        }
      }
    }
  }
  
  private int validateListContains(OBDBObject toValidate) {
    for (int i = 0; i < validateList.size(); i++) {
      OBDBObject tmpObj = validateList.elementAt(i);
      try {
        if (tmpObj.getPrimaryKey()==toValidate.getPrimaryKey() &&
            tmpObj.getTableName().equalsIgnoreCase(toValidate.getTableName())) {
          return i;
        }
      }
      catch (Exception e) {
        logger.debug("error validating", e);//$NON-NLS-1$
      }
    }
    return OBAttribute.NULL;
  }


  /**
   * @param context
   * @throws OBException
   */
  public void validateDeferrable(OBContext context) throws OBException { 
    for (int i=0;i<validateList.size();i++) {
      OBTableObject toValidate = validateList.elementAt(i);
      if (toValidate.runValidateDeferrable) {
        int count=0;
        try {
          count=OBTableObject.count(context,toValidate,new long[] {toValidate.getPrimaryKey()});
        }
        catch (Exception e) {
          logger.debug(XynaContextFactory.getSessionData(context) + "error finding object to validate",e);//$NON-NLS-1$
        }
        if (count>0) {
          // nur wenn das Objekt noch existiert!
          toValidate.validateDeferrable(context);
        }
      }
    }
    // OPTIMIZE validateList = new Vector();
    validateList.clear();
  }


  /**
   * @see java.sql.Connection#createStatement(int, int)
   */
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    return con.createStatement(resultSetType, resultSetConcurrency);
  }


  /**
   * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
   */
  public PreparedStatement prepareStatement(String stmt, int a, int b) throws SQLException {
    return con.prepareStatement(stmt,a , b);
  }


  /**
   * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
   */
  public CallableStatement prepareCall(String call, int a, int b) throws SQLException {
    return con.prepareCall(call, a, b);
  }
  
  /*--- <Methoden fuer Java 1.4> ---------------------------------------------

  /*<Java1.4/>*/public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
  /*<Java1.4/>*/  return con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public PreparedStatement prepareStatement(String stmt, int a, int b, int c) throws SQLException {
  /*<Java1.4/>*/  return con.prepareStatement(stmt, a, b, c);
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public PreparedStatement prepareStatement(String stmt, int a) throws SQLException {    
  /*<Java1.4/>*/  return con.prepareStatement(stmt, a);
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public PreparedStatement prepareStatement(String stmt, int[] a) throws SQLException {    
  /*<Java1.4/>*/  return con.prepareStatement(stmt, a);
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public PreparedStatement prepareStatement(String stmt, String[] a) throws SQLException {    
  /*<Java1.4/>*/  return con.prepareStatement(stmt, a);
  /*<Java1.4/>*/}
  
  /*<Java1.4/>*/public CallableStatement prepareCall(String call, int a, int b , int c) throws SQLException {
  /*<Java1.4/>*/  return con.prepareCall(call, a, b, c);
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public Savepoint setSavepoint(String savePoint) throws SQLException {
  /*<Java1.4/>*/  return con.setSavepoint(savePoint);
  /*<Java1.4/>*/}
  
  /*<Java1.4/>*/public Savepoint setSavepoint() throws SQLException {
  /*<Java1.4/>*/  return con.setSavepoint();
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public int getHoldability() throws SQLException {
  /*<Java1.4/>*/  return con.getHoldability();
  /*<Java1.4/>*/} 

  /*<Java1.4/>*/public void rollback(Savepoint sp) {
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public void releaseSavepoint(Savepoint sp) {
  /*<Java1.4/>*/}

  /*<Java1.4/>*/public void setHoldability(int holdability) {
  /*<Java1.4/>*/}
   
  //--- </Methoden fuer Java-1.4> ---------------------------------------*/
  
  
  /**
   * @see java.sql.Connection#getTypeMap()
   */
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return con.getTypeMap();
  }

  
  /**
   * @see java.sql.Connection#setTypeMap(java.util.Map)
   */
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    con.setTypeMap(map);
  }

  
  /**
   * @see java.sql.Connection#createStatement()
   */
  public Statement createStatement() throws SQLException {
    //logger.debug("OBDatabase.createStatement");
    return con.createStatement();
  }

  
  /**
   * @see java.sql.Connection#prepareStatement(java.lang.String)
   */
  public PreparedStatement prepareStatement(String stmt) throws SQLException {
    return con.prepareStatement(stmt);
  }

  
  /**
   * @see java.sql.Connection#prepareCall(java.lang.String)
   */
  public CallableStatement prepareCall(String call) throws SQLException {
    return con.prepareCall(call);
  }

  
  /**
   * @see java.sql.Connection#nativeSQL(java.lang.String)
   */
  public String nativeSQL(String s) throws SQLException {
    return con.nativeSQL(s);
  }
  
  
  /**
   * @see java.sql.Connection#setAutoCommit(boolean)
   */
  public void setAutoCommit(boolean b) throws SQLException {
    con.setAutoCommit(b);
  }

  
  /**
   * @see java.sql.Connection#getAutoCommit()
   */
  public boolean getAutoCommit() throws SQLException {
    return con.getAutoCommit();
  }
  
  
  /**
   * @see java.sql.Connection#close()
   */
  public void close() throws SQLException {
    con.close();
  }
  
  
  /**
   * @see java.sql.Connection#isClosed()
   */
  public boolean isClosed()  throws SQLException {
    if (con==null) {
      return true;
    }
    return con.isClosed();
  }
  
  
  /**
   * @see java.sql.Connection#getMetaData()
   */
  public DatabaseMetaData getMetaData() throws SQLException {
    return con.getMetaData();
  }
  
  
  /**
   * @see java.sql.Connection#setReadOnly(boolean)
   */
  public void setReadOnly(boolean b) throws SQLException {
    con.setReadOnly(b);
  }
  
  
  /**
   * @see java.sql.Connection#isReadOnly()
   */
  public boolean isReadOnly() throws SQLException {
    return con.isReadOnly();
  }
  
  
  /**
   * @see java.sql.Connection#setCatalog(java.lang.String)
   */
  public void setCatalog(String s) throws SQLException {
    con.setCatalog(s);
  }
  
  
  /**
   * @see java.sql.Connection#getCatalog()
   */
  public String getCatalog() throws SQLException {
    return con.getCatalog();
  }
  
  
  /**
   * @see java.sql.Connection#getTransactionIsolation()
   */
  public int getTransactionIsolation() throws SQLException {
    return con.getTransactionIsolation();
  }
  
  
  /**
   * @see java.sql.Connection#getWarnings()
   */
  public SQLWarning getWarnings() throws SQLException {
    return con.getWarnings();
  }
  
  
  /**
   * @see java.sql.Connection#clearWarnings()
   */
  public void clearWarnings() throws SQLException {
    con.clearWarnings();
  }
  
  
  /**
   * @see java.sql.Connection#setTransactionIsolation(int)
   */
  public void setTransactionIsolation(int i) throws SQLException {
    con.setTransactionIsolation(i);
  }

  public Clob createClob() throws SQLException {
    return con.createClob();
  }


  public Blob createBlob() throws SQLException {
    return con.createBlob();
  }


  public NClob createNClob() throws SQLException {
    return con.createNClob();
  }


  public SQLXML createSQLXML() throws SQLException {
    return con.createSQLXML();
  }


  public boolean isValid(int timeout) throws SQLException {
    return con.isValid(timeout);
  }


  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    con.setClientInfo(name,value);
  }


  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    con.setClientInfo(properties);
  }


  public String getClientInfo(String name) throws SQLException {
    return con.getClientInfo(name);
  }


  public Properties getClientInfo() throws SQLException {
    return con.getClientInfo();
  }


  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return con.createArrayOf(typeName, elements);
  }


  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return con.createStruct(typeName, attributes);
  }


  public <T> T unwrap(Class<T> iface) throws SQLException {
    return con.unwrap(iface);
  }


  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return con.isWrapperFor(iface);
  }

/*

  public void setSchema(String schema) throws SQLException {
    con.setSchema(schema);
    
  }


  public String getSchema() throws SQLException {
    return con.getSchema();
  }


  public void abort(Executor executor) throws SQLException {
    con.abort(executor);
  }


  public void setNetworkTimeout(Executor executor,
                                int milliseconds) throws SQLException {
    con.setNetworkTimeout(executor, milliseconds);
  }


  public int getNetworkTimeout() throws SQLException {
    return con.getNetworkTimeout();
  }
*/
}