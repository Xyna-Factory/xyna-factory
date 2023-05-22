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
package gip.base.db;

import gip.base.callback.DecisionContainer;
import gip.base.callback.OBCallbackUtils;
import gip.base.common.OBAttribute;
import gip.base.common.OBConstants;
import gip.base.common.OBDTO;
import gip.base.common.OBException;
import gip.base.common.OBListObject;
import gip.base.common.OBObject;
import gip.base.db.drivers.OBDriver;
import gip.base.db.drivers.OBMySQLDriver;
import gip.base.db.drivers.OBOracleDriver;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/** 
 * Basisklasse, die die Datenbankzugriffe fuer eine Objekt, das mit einer
 * Oracle-Tabelle korrespondiert, kapselt
 */
@SuppressWarnings("serial")
public abstract class OBTableObject extends OBDBObject {

  /** Strings welche SQL-Statements bzw. Teile davon enthalten */
  private StringBuffer tableValues;
  private StringBuffer tableUpdate;
  public boolean runValidateDeferrable = true;

  private transient static Logger logger = Logger.getLogger(OBTableObject.class);
  
  /** 
   * Oeffentlicher Konstruktor
   */
  public OBTableObject () {
    super();
  }
  
  
  /** Sind in den Gen-Klassen implementiert.
   * @return Details
   */
  protected String tableDetails() {
    return ""; //$NON-NLS-1$
  }

  
  /** F&uuml;hrt ein DML-Statement aus (ohne commit).
      Darf nur in Ausnahmef&auml;llen gemacht werden, da keine Validierung stattfindet.
      Grund f&uuml;r die Einf&uuml;rung: Performanz bei Massen-Updates
   * @param context
   * @param sqlStringIn SQL-String f&uuml;r ein DML-Statement
   * @throws OBException
   */
  public static void execSQL(OBContext context, String sqlStringIn) throws OBException {
    String sqlString = replaceProjectSchemata(context, sqlStringIn);
    logger.debug(XynaContextFactory.getSessionData(context) + "SQL: "+ sqlString); //$NON-NLS-1$
    Statement stmt = null;
    try {
      stmt = context.getDataConnection().createStatement();
      stmt.executeUpdate(sqlString);
      stmt.close();
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error executing sql",e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmp = handleSQLException(context,e, context.getDataConnection());
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {tmp});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error executing sql",e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
    }
  }

  
  /** 
   * Fuehrt ein CallableStatement auf einer Connection aus
   * @param con
   * @param sqlString
   * @throws OBException
   */
  public static void execCallableStatement(Connection con, String sqlString) throws OBException {
    logger.debug("PLSQL: "+ sqlString); //$//$NON-NLS-1$ 
    CallableStatement stmt = null;
    try {
      stmt = con.prepareCall(sqlString);
      stmt.execute();
      stmt.close();
    }
    catch (Exception e) {
      logger.error("error executing sql",e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch(Exception exp) {
        logger.error("error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
    }
  } 
  
  
  /** F&uuml;hrt ein DQL-Statement aus (ohne commit).
      Grund f&uuml;r die Einf&uuml;rung: Performanz bei Massen-Updates
   * @param context
   * @param sqlString SQL-String f&uuml;r ein DQL-Statement
   * @return Gefundenen Long-Wert
   * @throws OBException
   */
  public static long execLongQuery(OBContext context, String sqlString) throws OBException {
    logger.debug(XynaContextFactory.getSessionData(context) + "SQL: ExecLongQuery: "+ sqlString); //$NON-NLS-1$
    Statement stmt = null;
    try {
      stmt = context.getDataConnection().createStatement();
      ResultSet rs = stmt.executeQuery(sqlString);
      if (rs.next()) {
        return rs.getLong(1);
      }
      else {
        throw new OBException(OBException.OBErrorNumber.objectNotFound1,new String[] {""}); //$NON-NLS-1$
      }
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error executing sql",e);//$NON-NLS-1$
      String tmp = handleSQLException(context,e, context.getDataConnection());
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {tmp});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error executing sql",e);//$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
    }
    finally {
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
    }
  }

  
  /** Einfuegen eines neuen Datensatzes in die Datenbank.
      Zum Einfuegen werden die aktuellen Daten in den Attributen verwendet.
      Ueber eine sequence wird automatisch einer neuer Schluessel bestimmt.
      Der primary key des eingefuegten Datensatzes wird im zugehoerigen
      Attribut der Tabelle gespeichert, d.h. nach dem insert kann man sofort sowohl
      auf primaryKeyAtt als auch auf das Attribut des primary keys direkt zugreifen.
   * @param context
   * @throws OBException
  */
  private final void insertInternal(OBContext context) throws OBException {
    Statement stmt = null;
    PreparedStatement pStmt = null;
    try {
      OBAttribute[] hasLOBs = getInfo(context,false);
      String SQLString = "INSERT INTO " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),tableDetails()) + " " + tableValues.toString(); //$NON-NLS-1$ //$NON-NLS-2$
      logger.debug(XynaContextFactory.getSessionData(context) + getTableName() +".insert: " + hide(SQLString)); //$//$NON-NLS-1$ 
      
      if (hasLOBs.length==0) {
        stmt = getCorrectConnection(context).createStatement();
        stmt.executeUpdate(SQLString); // keine empty_xlobs() drin, da hasLOBs.length==0
        stmt.close();
      }
      else {
        pStmt = getCorrectConnection(context).prepareStatement(SQLString); // die empty_xlob() sind schon drin!
        pStmt.execute();
        pStmt.close();

        String updateStr = hasLOBs[0].name + "= ?";//$NON-NLS-1$
        String clobString = hasLOBs[0].name;
        for(int i=1; i<hasLOBs.length;i++){
          updateStr += "," + hasLOBs[i].name + "=?";//$NON-NLS-1$ //$NON-NLS-2$
          clobString += "," + hasLOBs[i].name; //$NON-NLS-1$
        }
        SQLString = "UPDATE " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) + " SET " + updateStr + " WHERE " + primaryKey + " = " + getPrimaryKey(); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
        
        pStmt = getCorrectConnection(context).prepareStatement(SQLString);

        if (context.getDriver() instanceof OBOracleDriver) {
          // ORACLE
          PreparedStatement dummyst = getCorrectConnection(context).prepareStatement("SELECT "+ clobString + " FROM "+ OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) + //$NON-NLS-1$ //$NON-NLS-2$
              " WHERE " + primaryKey + " = " + getPrimaryKey() +" FOR UPDATE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          ResultSet rs = dummyst.executeQuery();  
          if (rs.next()){
            for (int i = 0; i < hasLOBs.length; i++) {
              if (hasLOBs[i].type==OBConstants.CLOB) {
                Clob cl1 = rs.getClob(i+1);
                Writer writer=cl1.setCharacterStream(1L);
                writer.write(hasLOBs[i].getValue().toCharArray());
                writer.flush();
                writer.close();
                logger.debug(""); //$NON-NLS-1$
                pStmt.setClob(i+1,cl1);
              }
              else if (hasLOBs[i].type==OBConstants.BLOB) {
                Blob cl1 = rs.getBlob(i+1);
                OutputStream os=cl1.setBinaryStream(1L);
                os.write(hasLOBs[i].getByteArrayValue());
                os.flush();
                os.close();
                logger.debug(""); //$NON-NLS-1$
                pStmt.setBlob(i+1,cl1);
              }
              else {
                throw(new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
              }
            }
          }
          pStmt.executeUpdate();
          pStmt.close();

          try {
            dummyst.close();
          }
          catch(Exception e) {
            logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",e);//$NON-NLS-1$
          }
        }
        else if (context.getDriver() instanceof OBMySQLDriver) {
          // MySQL
          for (int i = 0; i < hasLOBs.length; i++) {
            if (hasLOBs[i].type==OBConstants.CLOB) {
              pStmt.setString(i+1, hasLOBs[i].getValue());
            }
            else if (hasLOBs[i].type==OBConstants.BLOB) {
              ByteArrayInputStream stream = new ByteArrayInputStream(hasLOBs[i].getByteArrayValue());
              pStmt.setBinaryStream(i+1, stream,hasLOBs[i].getByteArrayValue().length);
            }
            else {
              throw(new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
            }
          }
          pStmt.executeUpdate();
          pStmt.close();
        }
        else {
          throw(new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
        }
      }
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error inserting data",e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
        if (pStmt!=null) pStmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmp = handleSQLException(context,e, getCorrectConnection(context));
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{tmp});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error inserting data",e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
        if (pStmt!=null) pStmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
    }
  }

  /**
   * Einfuegen eines neuen Datensatzes in die Datenbank. Zum Einfuegen werden
   * die aktuellen Daten in den Attributen verwendet. Ueber eine sequence wird
   * automatisch einer neuer Schluessel bestimmt. Der primary key des
   * eingefuegten Datensatzes wird im zugehoerigen Attribut der Tabelle
   * gespeichert, d.h. nach dem insert kann man sofort sowohl auf primaryKeyAtt
   * als auch auf das Attribut des primary keys direkt zugreifen.
   * 
   * @param context
   * @throws OBException
   */
  private final void insertInternalPrepared(OBContext context) throws OBException {
    PreparedStatement pStmt = null;
    try {
      ArrayList<String> replArray = new ArrayList<String>();
      ArrayList<Integer> replTypesArray = new ArrayList<Integer>();
      StringBuffer tableValuesOut = new StringBuffer();
      OBAttribute[] hasLOBs = getInfoPreparedInsert(context, tableValuesOut, replArray, replTypesArray);
      String SQLString = "INSERT INTO " + OBDriver.getTableName(context, context.getSchema(getProjectSchema()), tableDetails()) + " " + tableValuesOut.toString(); //$NON-NLS-1$ //$NON-NLS-2$
      
      logger.debug(XynaContextFactory.getSessionData(context) + getTableName()
                   + ".insert: " + getPreparedStatementStringForDebug(SQLString,replArray,null,OBAttribute.NULL,OBAttribute.NULL)); //$//$NON-NLS-1$ 

      pStmt = getCorrectConnection(context).prepareStatement(SQLString); // die empty_xlob() sind schon drin!
      for (int i = 0; replArray!=null &&  i < replArray.size(); i++) {
        if (replArray.get(i).equalsIgnoreCase("null")) {
          pStmt.setNull(i+1, replTypesArray.get(i));
        }
        else {
          if (replTypesArray.get(i)==Types.INTEGER) {
            pStmt.setLong(i+1, Long.parseLong(replArray.get(i)));
          }
          else {
            pStmt.setString(i+1, replArray.get(i));
          }
        }
      }
      pStmt.execute();
      pStmt.close();

      if (hasLOBs.length>0) {
        String updateStr = hasLOBs[0].name + "= ?";//$NON-NLS-1$
        String clobString = hasLOBs[0].name;
        for (int i = 1; i < hasLOBs.length; i++) {
          updateStr += "," + hasLOBs[i].name + "=?";//$NON-NLS-1$ //$NON-NLS-2$
          clobString += "," + hasLOBs[i].name; //$NON-NLS-1$
        }
        SQLString = "UPDATE " + OBDriver.getTableName(context, context.getSchema(getProjectSchema()), getSQLName()) + " SET " + updateStr + " WHERE " + primaryKey + " = " + getPrimaryKey(); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
  
        pStmt = getCorrectConnection(context).prepareStatement(SQLString);
  
        if (context.getDriver() instanceof OBOracleDriver) {
          // ORACLE
          PreparedStatement dummyst = getCorrectConnection(context).prepareStatement("SELECT " + clobString + " FROM " + OBDriver.getTableName(context, context.getSchema(getProjectSchema()), getSQLName()) + //$NON-NLS-1$ //$NON-NLS-2$
                                                                                         " WHERE "
                                                                                         + primaryKey
                                                                                         + " = " + getPrimaryKey() + " FOR UPDATE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          ResultSet rs = dummyst.executeQuery();
          if (rs.next()) {
            for (int i = 0; i < hasLOBs.length; i++) {
              if (hasLOBs[i].type == OBConstants.CLOB) {
                Clob cl1 = rs.getClob(i + 1);
                Writer writer = cl1.setCharacterStream(1L);
                writer.write(hasLOBs[i].getValue().toCharArray());
                writer.flush();
                writer.close();
                logger.debug(""); //$NON-NLS-1$
                pStmt.setClob(i + 1, cl1);
              }
              else if (hasLOBs[i].type == OBConstants.BLOB) {
                Blob cl1 = rs.getBlob(i + 1);
                OutputStream os = cl1.setBinaryStream(1L);
                os.write(hasLOBs[i].getByteArrayValue());
                os.flush();
                os.close();
                logger.debug(""); //$NON-NLS-1$
                pStmt.setBlob(i + 1, cl1);
              }
              else {
                throw (new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
              }
            }
          }
          pStmt.executeUpdate();
          pStmt.close();
  
          try {
            dummyst.close();
          }
          catch (Exception e) {
            logger.error(XynaContextFactory.getSessionData(context)
                         + "error closing statement", e);//$NON-NLS-1$
          }
        }
        else if (context.getDriver() instanceof OBMySQLDriver) {
          // MySQL
          for (int i = 0; i < hasLOBs.length; i++) {
            if (hasLOBs[i].type == OBConstants.CLOB) {
              pStmt.setString(i + 1, hasLOBs[i].getValue());
            }
            else if (hasLOBs[i].type == OBConstants.BLOB) {
              ByteArrayInputStream stream = new ByteArrayInputStream(hasLOBs[i].getByteArrayValue());
              pStmt.setBinaryStream(i + 1, stream,
                                    hasLOBs[i].getByteArrayValue().length);
            }
            else {
              throw (new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
            }
          }
          pStmt.executeUpdate();
          pStmt.close();
        }
        else {
          throw (new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
        }
      }
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context)
                   + "error inserting data", e);//$NON-NLS-1$
      try {
        if (pStmt != null) {
          pStmt.close();
        }
      }
      catch (Exception exp) {
        // System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context)
                     + "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmp = handleSQLException(context, e, getCorrectConnection(context));
      throw new OBException(OBException.OBErrorNumber.sqlException1,
                            new String[] { tmp });
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context)
                   + "error inserting data", e);//$NON-NLS-1$
      try {
        if (pStmt != null) {
          pStmt.close();
        }
      }
      catch (Exception exp) {
        // System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context)
                     + "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1,
                            new String[] { e.getMessage() });
    }
  }

  /** Aendert Datensaetze in der Datenbank.
      Falls key>0 wird der Datensatz mit ID=key geaendert, sonst Exception
      Ein Update erfolgt nur, falls der Wert von lockRow=lock ist.
      Zum Update werden die aktuellen Werte int attVector verwendet.
      Zurueckgegeben wird der Wert des Locks fuer key.
   * @param context
   * @param key
   * @param lock
   * @return
   * @throws OBException
  */
  private int updateInternal(OBContext context, long key, long lock) throws OBException {
    int staffId = getLock(context, key);
    
    if((staffId != -1) && (staffId != context.getStaffId())) {
      String staffName=""; //$NON-NLS-1$
      try {
        staffName = getStaffName(context);
      }
      catch(Exception e) {
        staffName = "unbekannt"; //$NON-NLS-1$
      }
      DecisionContainer decision = new DecisionContainer();
      decision.setMessageText("Das Objekt: "+getTableName()+" identifier: " +getIdentifier() + " ist gesperrt!\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                              "Es wird derzeit benutzt von Benutzer: " +staffName + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                              "Sperre aufheben und mit dem Vorgang fortfahren?"); //$NON-NLS-1$
      decision.setFalseText("Nein"); //$NON-NLS-1$
      decision.setTrueText("Ja"); //$NON-NLS-1$
      decision.setDefaultAnswerBoolean(false);
      if (!OBCallbackUtils.decideBoolean(context, decision)) {
        throw new OBException(OBException.OBErrorNumber.actionAborted);
      }
      setLock(context, key, "", -1); //$NON-NLS-1$
    }

    OBAttribute[] hasLOBs = getInfo(context,true);
    String SQLString = "UPDATE " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) + " SET " + tableUpdate.toString(); //$NON-NLS-1$ //$NON-NLS-2$
    if (key > 0) {
      SQLString = SQLString + " WHERE " + primaryKey + " = " + key; //$NON-NLS-1$ //$NON-NLS-2$
    } 
    else {
      //SQLString = SQLString +" "+ whereClause + " AND lockRow = " + lock;
      throw new OBException (OBException.OBErrorNumber.updateNotSupported);
    }
    
    //logger.debug(this+ "Geaendertes Objekt: " + tableName + ": " + getIdentifier());
    logger.debug(XynaContextFactory.getSessionData(context) + getTableName()+".update: " + hide(SQLString)); //$NON-NLS-1$
    Statement stmt = null;
    PreparedStatement pStmt = null;
    PreparedStatement dummyst = null;
    try {
      if (hasLOBs.length==0) {
        stmt = getCorrectConnection(context).createStatement();
        stmt.executeUpdate(SQLString);
        stmt.close();
      }
      else {        
        // Setzen der CLOB/BLOB auf empty, damit es unten keine NullPointerException gibt
        for(int i=0; i<hasLOBs.length;i++){
          String emptyClob = "UPDATE " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName())+ //$NON-NLS-1$
                             " SET " +hasLOBs[i].name +" = " + //$NON-NLS-1$//$NON-NLS-2$
                             (hasLOBs[i].type==OBConstants.CLOB ? OBDriver.getEmptyClob(context) : OBDriver.getEmptyBlob(context)) +  
                             " WHERE "+ primaryKey + " = " + key; //$NON-NLS-1$//$NON-NLS-2$
          pStmt = getCorrectConnection(context).prepareStatement(emptyClob);
          pStmt.execute();
          pStmt.close();
        }

        String clobString = hasLOBs[0].name;
        for(int i=1; i<hasLOBs.length;i++){
          clobString += "," + hasLOBs[i].name; //$NON-NLS-1$
        }

        pStmt = getCorrectConnection(context).prepareStatement(SQLString);
        dummyst = getCorrectConnection(context).prepareStatement("SELECT "+ clobString + " FROM "+ OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) +//$NON-NLS-1$//$NON-NLS-2$
            " WHERE " + primaryKey + " = " + key+" FOR UPDATE");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        ResultSet rs = dummyst.executeQuery(); 
        if (rs.next()){
          for (int i = 0; i < hasLOBs.length; i++) {
            if (hasLOBs[i].type==OBConstants.CLOB) {
              Clob cl1 = rs.getClob(i+1);
              Writer writer=cl1.setCharacterStream(1L);
              writer.write(hasLOBs[i].getValue().toCharArray());
              writer.flush();
              writer.close();
              cl1.truncate(hasLOBs[i].getValue().length());
              pStmt.setClob(i+1,cl1);
            }
            else if (hasLOBs[i].type==OBConstants.BLOB) {
              Blob cl1 = rs.getBlob(i+1);
              OutputStream os=cl1.setBinaryStream(1L);
              os.write(hasLOBs[i].getByteArrayValue());
              os.flush();
              os.close();
              cl1.truncate(hasLOBs[i].getByteArrayValue().length);
              pStmt.setBlob(i+1,cl1);
            }
          }
          pStmt.executeUpdate();
          pStmt.close();
          dummyst.close();
        } 
        else {
          throw(new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
        }
        
      }
      return staffId;
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error updating data",e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
        if (pStmt!=null) pStmt.close();
        if (dummyst!=null) dummyst.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmpStr = handleSQLException(context,e, getCorrectConnection(context));
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {tmpStr});
    }
    catch( Exception e ) {
      logger.error(XynaContextFactory.getSessionData(context) + "error updating data",e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
        if (pStmt!=null) pStmt.close();
        if (dummyst!=null) dummyst.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[]{e.getMessage()});
    }
  }

  /**
   * Aendert Datensaetze in der Datenbank. Falls key>0 wird der Datensatz mit
   * ID=key geaendert, sonst Exception Ein Update erfolgt nur, falls der Wert
   * von lockRow=lock ist. Zum Update werden die aktuellen Werte int attVector
   * verwendet. Zurueckgegeben wird der Wert des Locks fuer key.
   * 
   * @param context
   * @param key
   * @param lock
   * @return
   * @throws OBException
   */
  private int updateInternalPrepared(OBContext context, long key, long lock) throws OBException {
    int staffId = getLock(context, key);

    if ((staffId != -1) && (staffId != context.getStaffId())) {
      String staffName = ""; //$NON-NLS-1$
      try {
        staffName = getStaffName(context);
      }
      catch (Exception e) {
        staffName = "unbekannt"; //$NON-NLS-1$
      }
      DecisionContainer decision = new DecisionContainer();
      decision.setMessageText("Das Objekt: " + getTableName() + " identifier: " + getIdentifier() + " ist gesperrt!\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                              "Es wird derzeit benutzt von Benutzer: "
                              + staffName + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                              "Sperre aufheben und mit dem Vorgang fortfahren?"); //$NON-NLS-1$
      decision.setFalseText("Nein"); //$NON-NLS-1$
      decision.setTrueText("Ja"); //$NON-NLS-1$
      decision.setDefaultAnswerBoolean(false);
      if (!OBCallbackUtils.decideBoolean(context, decision)) {
        throw new OBException(OBException.OBErrorNumber.actionAborted);
      }
      setLock(context, key, "", -1); //$NON-NLS-1$
    }

    StringBuffer tableUpdate = new StringBuffer();
    ArrayList<String> replArray = new ArrayList<String>();
    ArrayList<Integer> replTypesArray = new ArrayList<Integer>();
    ArrayList<OBAttribute> hasLOBs = getInfoPreparedUpdate(context, tableUpdate, replArray, replTypesArray);
    String SQLString = "UPDATE " + OBDriver.getTableName(context, context.getSchema(getProjectSchema()), getSQLName()) + //$NON-NLS-1$ 
                       " SET " + tableUpdate.toString(); //$NON-NLS-1$
    // LOBS wurden ausgespart, die kommen jetzt
    for (int i=0; i<hasLOBs.size(); i++) {
      SQLString += ", " + hasLOBs.get(i).name + "= ?";
    }
    if (key > 0) {
      SQLString = SQLString + " WHERE " + primaryKey + " = ? " ; //$NON-NLS-1$ //$NON-NLS-2$
    }
    else {
      // SQLString = SQLString +" "+ whereClause + " AND lockRow = " + lock;
      throw new OBException(OBException.OBErrorNumber.updateNotSupported);
    }

    // logger.debug(this+ "Geaendertes Objekt: " + tableName + ": " +
    // getIdentifier());
    logger.debug(XynaContextFactory.getSessionData(context) + getTableName()
                 + ".update: " + getPreparedStatementStringForDebug(SQLString, replArray, new ArrayList<OBAttribute>(), key, lock)); //$NON-NLS-1$
    PreparedStatement pStmt = null;
    PreparedStatement dummyst = null;
    ResultSet dummyRs = null;
    try {
      if (hasLOBs.size()>0) {
        // erstmal alle LOBs auf leere setzen, damit es keien NP-Exceptions gibt
        String emptyClob = "UPDATE " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
        for (int i = 0; i < hasLOBs.size(); i++) {
          if (i>0) {
            emptyClob += ", "; //$NON-NLS-1$
          }
          emptyClob += hasLOBs.get(i).name +" = " + //$NON-NLS-1$
                       (hasLOBs.get(i).type==OBConstants.CLOB ? OBDriver.getEmptyClob(context) : OBDriver.getEmptyBlob(context));
        }
        emptyClob += " WHERE "+ primaryKey + " = ?"; //$NON-NLS-1$//$NON-NLS-2$
        pStmt = getCorrectConnection(context).prepareStatement(emptyClob);
        pStmt.setLong(1, key);
        pStmt.execute();
        pStmt.close();
      }
      pStmt = getCorrectConnection(context).prepareStatement(SQLString);
      // Normale Spalten
      for (int i = 0; replArray!=null &&  i < replArray.size(); i++) {
        if (replArray.get(i).equalsIgnoreCase("null")) {
          pStmt.setNull(i+1, replTypesArray.get(i));
        }
        else {
          if (replTypesArray.get(i)==Types.INTEGER) {
            pStmt.setLong(i+1, Long.parseLong(replArray.get(i)));
          }
          else {
            pStmt.setString(i+1, replArray.get(i));
          }
        }
      }
      // LOBS kommen spaeter!
      // PK
      pStmt.setLong(replArray.size()+hasLOBs.size()+1, key);
      if (hasLOBs.size()>0) {
        // CLOBS zum UPDATEN suchen
        String clobString = hasLOBs.get(0).name;
        for (int i = 1; i < hasLOBs.size(); i++) {
          clobString += "," + hasLOBs.get(i).name; //$NON-NLS-1$
        }

        
        dummyst = getCorrectConnection(context).prepareStatement("SELECT " + clobString + " FROM " + OBDriver.getTableName(context, context.getSchema(getProjectSchema()), getSQLName()) + //$NON-NLS-1$//$NON-NLS-2$
                                                                 " WHERE " + primaryKey
                                                                     + " = ? FOR UPDATE");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        dummyst.setLong(1, key);
        dummyRs = dummyst.executeQuery();
        if (dummyRs.next()) {
          for (int i = 0; i < hasLOBs.size(); i++) {
            if (hasLOBs.get(i).type == OBConstants.CLOB) {
              Clob cl1 = dummyRs.getClob(i + 1);
              Writer writer = cl1.setCharacterStream(1L);
              writer.write(hasLOBs.get(i).getValue().toCharArray());
              writer.flush();
              writer.close();
              cl1.truncate(hasLOBs.get(i).getValue().length());
              pStmt.setClob(i+1+replArray.size(), cl1);
            }
            else if (hasLOBs.get(i).type == OBConstants.BLOB) {
              Blob cl1 = dummyRs.getBlob(i + 1);
              OutputStream os = cl1.setBinaryStream(1L);
              os.write(hasLOBs.get(i).getByteArrayValue());
              os.flush();
              os.close();
              cl1.truncate(hasLOBs.get(i).getByteArrayValue().length);
              pStmt.setBlob(i+1+replArray.size(), cl1);
            }
          }
        }
        else {
          throw (new Exception("Das durfte nicht passieren"));//$NON-NLS-1$
        }
      }
      // Jetzt aber UPDATE
      pStmt.executeUpdate();
      pStmt.close();
      if (dummyRs!=null) {
        dummyRs.close();
      }
      if (dummyst!=null) {
        dummyst.close();
      }
      return staffId;
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error updating data", e);//$NON-NLS-1$
      try {
        if (pStmt != null) {
          pStmt.close();
        }
        if (dummyRs!=null) {
          dummyRs.close();
        }
        if (dummyst != null) {
          dummyst.close();
        }
      }
      catch (Exception exp) {
        // System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmpStr = handleSQLException(context, e,
                                         getCorrectConnection(context));
      throw new OBException(OBException.OBErrorNumber.sqlException1,
                            new String[] { tmpStr });
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context)
                   + "error updating data", e);//$NON-NLS-1$
      try {
        if (pStmt != null) {
          pStmt.close();
        }
        if (dummyRs!=null) {
          dummyRs.close();
        }
        if (dummyst != null) {
          dummyst.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context)
                     + "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      throw new OBException(OBException.OBErrorNumber.unknownError1,
                            new String[] { e.getMessage() });
    }
  }

  /** Loescht das aktuelle Objekt in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      Diese Methode kann ueberschrieben werden, falls beim Loeschen
      mittels XOBDeleteSelect Rueckfragen gestellt werden sollen.
   * @param context
   * @throws OBException
  */
  public void delete(OBContext context) throws OBException{
    if (primaryKeyAtt.isNull()) {
      throw new OBException (OBException.OBErrorNumber.objectNotSet);
    }
    delete(context,getPrimaryKey());  
  }

  /** Loescht Datensaetze in der Datenbank.
      Falls key>0 wird der Datensatz mit ID=key geloescht,
      ansonsten alle Datensaetze gemaess wherClause.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
   * @param context
   * @param key
   * @throws OBException
  */
  private void delete(OBContext context, long key) throws OBException {
    int staffId = getLock(context, key);
    if (staffId == OBConstants.IRREGULAR_INT) {
      logger.warn(XynaContextFactory.getSessionData(context) + "Object not deleted. Primary key not set"); //$NON-NLS-1$
      return;
    }
    else if((staffId != -1) && (staffId != context.getStaffId())) {
      String staffName=""; //$NON-NLS-1$
      try {
        staffName = getStaffName(context);
      }
      catch(Exception e) {
        staffName = "unbekannt"; //$NON-NLS-1$
      }
      DecisionContainer decision = new DecisionContainer();
      decision.setMessageText("Das Objekt: "+getTableName()+" identifier: " +getIdentifier() + " ist gesperrt!\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                              "Es wird derzeit benutzt von Benutzer: " +staffName + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
                              "Sperre aufheben und mit dem Vorgang fortfahren?"); //$NON-NLS-1$
      decision.setFalseText("Nein"); //$NON-NLS-1$
      decision.setTrueText("Ja"); //$NON-NLS-1$
      decision.setDefaultAnswerBoolean(false);
      if (!OBCallbackUtils.decideBoolean(context, decision)) {
        throw new OBException(OBException.OBErrorNumber.actionAborted);
      }
      setLock(context, key, "", -1); //$NON-NLS-1$
    }
    String SQLString = "DELETE FROM "+OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()); //$NON-NLS-1$
    int count = 0;
    if (key > 0) {
      if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
        SQLString = SQLString + " WHERE " + primaryKey + " = " + key + " AND lockRow IN (-1,"+context.getStaffId()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      else {
        SQLString = SQLString + " WHERE " + primaryKey + " = ? AND lockRow IN (-1,?)"; //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    else {
      throw new OBException (OBException.OBErrorNumber.objectNotSet);
    }
    logger.debug(XynaContextFactory.getSessionData(context) + getTableName()+".delete: " + hide(SQLString));//$NON-NLS-1$ 
    Statement stmt = null;
    PreparedStatement pStmt = null;
    try {
      if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
        stmt = getCorrectConnection(context).createStatement();
        count = stmt.executeUpdate(SQLString);
        stmt.close();
      }
      else {
        pStmt = getCorrectConnection(context).prepareStatement(SQLString);
        pStmt.setLong(1, key);
        pStmt.setLong(2, context.getStaffId());
        pStmt.execute();
        count=pStmt.getUpdateCount();
        pStmt.close();
      }
    }
    catch( Exception e ) {
      logger.error(XynaContextFactory.getSessionData(context) + "error deleting data",e);//$$NON-NLS-1$ //$NON-NLS-1$
      //getCorrectConnection(context).rollback();
      try {
        if (stmt!=null) stmt.close();
        if (pStmt!=null) pStmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$$NON-NLS-1$ //$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException);
      }
      String tmpStr = handleSQLException(context, (SQLException)e, getCorrectConnection(context));
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {tmpStr});
    }
    if (key > 0 && count == 0) {
      throw new OBException(OBException.OBErrorNumber.deleteNotPossible2,new String[]{ getTableName(),"" + key}); //$NON-NLS-1$
    }
  }
  
  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   OBDBObject example,
                                   OBDBObject filter) throws OBException {
    OBListObject<OBDBObject> delObjects = findAll(context, example, filter);
    findAndDelete(context, example, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   OBDBObject example,
                                   String whereClause) throws OBException {
    OBListObject<OBDBObject> delObjects = findAll(context, example, whereClause,"",""); //$NON-NLS-1$ //$NON-NLS-2$
    findAndDelete(context, example, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   OBDBObject example,
                                   String whereClause, 
                                   String orderBy) throws OBException {
    OBListObject<OBDBObject> delObjects = findAll(context, example, whereClause, orderBy,""); //$NON-NLS-1$
    findAndDelete(context, example, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param pks ARRAY der Prim&auml;rschl&uuml;ssel
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   OBDBObject example, 
                                   long[] pks) throws OBException {
    OBListObject<OBDBObject> delObjects = findAll(context, example, pks, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
    findAndDelete(context, example, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context
      @param example
      @param delObjects Liste der zu loeschenden Objekte
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   OBDBObject example, 
                                   OBListObject<OBDBObject> delObjects) throws OBException {
    OBTableObject obtabobj;
    if (delObjects.size() != 0) {
      for (int i=0; i<delObjects.size(); i++) {
        obtabobj = (OBTableObject)(delObjects.elementAt(i));
        try {
          obtabobj.delete(context);
        }
        catch (OBException e) {
          throw e;
        }
        catch (Exception e) {
          throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
        }
      }
      if (delObjects.size() == 1) {
        logger.info(XynaContextFactory.getSessionData(context) + example.getTableName()+".findAndDelete " + "1 Object deleted"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      else {
        logger.info(XynaContextFactory.getSessionData(context) + example.getTableName()+".findAndDelete " + delObjects.size() + "Objects deleted"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
  }

  /** Setzt eine oder mehrere Werte der Spalte lockRow auf den Wert lock.
      Ein existierender Lock wird dabei immer ueberschrieben.
      Ist key>0, so wird nur der Datensatz mit ID=key verwendet,
      ansonsten alle Datensaetze gemaess whereClause.
      Die Methode wird auch zum Entlocken mit lock=-1 verwendet.
   * @param context
   * @param key
   * @param whereClause
   * @param lock
   * @throws OBException
  */
  public synchronized void setLock(OBContext context, long key, String whereClause, long lock) throws OBException {
    ArrayList<String> replArray = new ArrayList<String>();
    String SQLString = "UPDATE " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) + " SET " + "lockRow = ";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      SQLString += lock; 
    }
    else {
      SQLString += "?";
      replArray.add(String.valueOf(lock));
    }
    if (key > 0) {
      SQLString = SQLString + " WHERE " + primaryKey + " = "; //$NON-NLS-1$ //$NON-NLS-2$
      if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
        SQLString += key;
      }
      else {
        SQLString += "?";
        replArray.add(String.valueOf(key));
      }
    }
    else if (whereClause!=null && whereClause.length()>0) {
      SQLString = SQLString + whereClause;
    }
    else {
      throw new OBException(OBException.OBErrorNumber.setLockNotPossibleNoKey);
    }

    logger.debug(XynaContextFactory.getSessionData(context) + getTableName()+".setLock: "+hide(SQLString));//$NON-NLS-1$
    context.getLockConnection(); 
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      Statement stmt = null;
      try {
        stmt = context.getLockConnection().createStatement();
        stmt.executeUpdate(SQLString);
        stmt.close();
        context.getLockConnection().commit(context);
      }
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error setting lock",e);//$NON-NLS-1$
        try {
          if (stmt!=null) stmt.close();
        }
        catch(Exception exp) {
          //System.exit(1);
          logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
          throw new OBException(OBException.OBErrorNumber.sqlFatalException);
        }
        throw new OBException(OBException.OBErrorNumber.setLockNotPossible1);
      }
      finally {
        giveBackLockConnection(context.getLockConnection());
      }
    }
    else {
      PreparedStatement pStmt = null;
      try {
        pStmt = context.getLockConnection().prepareStatement(SQLString);
        for (int i = 0; replArray!=null &&  i < replArray.size(); i++) {
          pStmt.setString(i+1, replArray.get(i));
        }
        pStmt.execute();
        pStmt.close();
        context.getLockConnection().commit(context);
      }
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error setting lock",e);//$NON-NLS-1$
        try {
          if (pStmt!=null) pStmt.close();
        }
        catch(Exception exp) {
          //System.exit(1);
          logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
          throw new OBException(OBException.OBErrorNumber.sqlFatalException);
        }
        throw new OBException(OBException.OBErrorNumber.setLockNotPossible1);
      }
      finally {
        giveBackLockConnection(context.getLockConnection());
      }
    }
  }

  
  
  /**
   * @param context
   * @param key
   * @return id des lockenden Users
   * @throws OBException
   */
  public int getLock(OBContext context, long key) throws OBException {
    int lock = OBConstants.IRREGULAR_INT;
    String SQLString = "SELECT lockRow FROM " + OBDriver.getTableName(context,context.getSchema(getProjectSchema()),getSQLName()) +  //$NON-NLS-1$
                       " WHERE " + primaryKey + " = "; //$NON-NLS-1$ //$NON-NLS-2$
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      SQLString += key;
    }
    else {
      SQLString += "?"; //$NON-NLS-1$
    }
    logger.debug(XynaContextFactory.getSessionData(context) + getTableName()+".getLock: " + hide(SQLString));//$NON-NLS-1$
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      Statement stmt = null;
      try {
        stmt = getCorrectConnection(context).createStatement();
        ResultSet rs = stmt.executeQuery(SQLString);
        if (rs.next()) {
          lock = rs.getInt(1);
        }
        else {
          stmt.close();
          return OBConstants.IRREGULAR_INT;
        }
        stmt.close();
      }
      catch(Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error getting lock",e);//$NON-NLS-1$
        try {
          if (stmt!=null) stmt.close();
        }
        catch(Exception exp) {
          //System.exit(1);
          logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
          throw new OBException(OBException.OBErrorNumber.sqlFatalException);
        }
        throw new OBException(OBException.OBErrorNumber.getLockNotPossible1, new String[] {e.getMessage()});
      }
    }
    else {
      PreparedStatement pStmt = null;
      try {
        pStmt = getCorrectConnection(context).prepareStatement(SQLString);
        pStmt.setLong(1, key);
        ResultSet rs = pStmt.executeQuery();
        if (rs.next()) {
          lock = rs.getInt(1);
        }
        else {
          pStmt.close();
          return OBConstants.IRREGULAR_INT;
        }
        pStmt.close();
      }
      catch(Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error getting lock",e);//$NON-NLS-1$
        try {
          if (pStmt!=null) pStmt.close();
        }
        catch(Exception exp) {
          //System.exit(1);
          logger.error(XynaContextFactory.getSessionData(context) + "error closing statement",exp);//$NON-NLS-1$
          throw new OBException(OBException.OBErrorNumber.sqlFatalException);
        }
        throw new OBException(OBException.OBErrorNumber.getLockNotPossible1, new String[] {e.getMessage()});
      }
    }
    return lock;
  }

  /** 
   * Besetzt die SQL-Teil-Statements (Member-Variablen) 
   * VALUES (...) und SET ... f�r insert und update.
   * @param context
   * @param update
   * @return Kommagetrennte Liste der CLobs, falls welche vorhanden sind.
   */
  protected OBAttribute[] getInfo(OBContext context, boolean update) throws OBException {
    ArrayList<OBAttribute> hasLOBS = new ArrayList<OBAttribute>();
    if (tableValues == null) {
      tableValues = new StringBuffer(1000);
    }
    else {
      tableValues.setLength(0);
    }
    if (tableUpdate == null) {
      tableUpdate = new StringBuffer(1000);
    }
    else {
      tableUpdate.setLength(0);
    }
    OBAttribute a;
    int j;
    boolean isInDate = false;
    boolean isPrimaryKey = false;
    boolean isLockRow = false;
    boolean isTimestamp = false;
    String tmpS;

    if (!update) tableValues.append("VALUES ( "); //$NON-NLS-1$

    for (j=0; j< attArr.length; j++) {
      isInDate = false;
      isPrimaryKey = false;
      isLockRow = false;
      isTimestamp = false;

      a= attArr[j];
      //try{
      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG) {
        if (a.name.equals(primaryKey)) {
          isPrimaryKey = true;
        }
        else if (a.name.equals("lockRow")) { //$NON-NLS-1$
          isLockRow = true;
        }
      }
      else if (a.type==OBConstants.DATE) {
        if (a.name.equals("inDate")) { //$NON-NLS-1$
          isInDate = true;
        }
        else if (a.name.equals("timestamp")) { //$NON-NLS-1$
          isTimestamp = true;
        }
      }

      if (!isInDate &&
          !isPrimaryKey &&
          !isLockRow &&
          !isTimestamp ) {
        if (update) tableUpdate.append(a.name).append('=');

      }
      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG || 
          a.type==OBConstants.DOUBLE || a.type==OBConstants.BOOLEAN) {
        if (isPrimaryKey)      {
          if (hasPrimaryKeySequence()) {
//            if (!update) tableValues.append("Key").append(tableName).append(".nextVal,");
            //<hslqdb>if (!update) tableValues.append("IDENTITY(),");
            /*<oracle>*/if (!update) {
              long pk = nextValPK(context);
              tableValues.append(pk).append(","); //$NON-NLS-1$
              setPrimaryKey(pk);
            }
          }
          else {
            //if (!update) tableValues.append("null,"); // Warum null?
            if (!update) tableValues.append(a.value.length() > 0 ? a.value : "null").append(","); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }
        else if (isLockRow) {
          if (!update) tableValues.append(" -1,"); //$NON-NLS-1$
        }
        else  {
          if(a.value.length() > 0) {
            tmpS = a.value;
          }
          else {
            tmpS = "null"; //$NON-NLS-1$
          }
          if (!update) tableValues.append(tmpS).append(',');
          if (update) tableUpdate.append(tmpS).append(',');
        }

      }
      else if (a.type==OBConstants.CLOB) {
        if (!update) tableValues.append(OBDriver.getEmptyClob(context)).append(',');//$NON-NLS-1$
        if (update) tableUpdate.append("?").append(','); //$NON-NLS-1$
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.BLOB) {
        if (!update) tableValues.append(OBDriver.getEmptyBlob(context)).append(',');//$NON-NLS-1$
        if (update) tableUpdate.append("?").append(','); //$NON-NLS-1$
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.DATE) {
        try {
          if( a.name.equals("changeDate") ) { //$NON-NLS-1$
            if (!update) tableValues.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
            if (update) tableUpdate.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (isInDate) {
            if (!update) tableValues.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (isTimestamp) {
            if (!update) tableValues.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            if (!update) tableValues.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
            if (update) tableUpdate.append(" "+OBDriver.getSysDateString(context)+",");  //$NON-NLS-1$ //$NON-NLS-2$
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                setDateFormat(OBConstants.NLS_DATETIME_FORMAT); // mit Uhrzeit!
              }
              tmpS = "'" + a.value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
              tmpS = "null"; //$NON-NLS-1$
            }
            tmpS = "TO_DATE(" + tmpS + ",'" + getDateFormat() + "'),"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (!update) tableValues.append(tmpS);
            if (update) tableUpdate.append(tmpS);
          }
        }
        catch (Exception ex) {
          logger.debug(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else if (a.type==OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
        try {
          if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            if (!update) tableValues.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
            if (update) tableUpdate.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if ("SYSTIMESTAMP".equalsIgnoreCase(a.value)) { //$NON-NLS-1$
              if (!update) tableValues.append(" "+OBDriver.getSysTimeStampString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
              if (update) tableUpdate.append(" "+OBDriver.getSysTimeStampString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (a.getFormatMask()==null || a.getFormatMask().length()==0) {
                if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                  a.setFormatMask(OBConstants.NLS_TIMESTAMP_FORMAT); // mit Uhrzeit!
                }
                else {
                  a.setFormatMask(OBConstants.NLS_DATE_FORMAT); // mit Uhrzeit!
                }
              }
              tmpS = "'" + a.value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
              tmpS = "null"; //$NON-NLS-1$
            }
            tmpS = "TO_TIMESTAMP_TZ(" + tmpS + ",'" + a.getFormatMask() + "'),"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (!update) tableValues.append(tmpS);
            if (update) tableUpdate.append(tmpS);
          }
        }
        catch (Exception ex) {
          logger.error(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else {
        if(a.value.length() > 0) {
          tmpS = '\''+ transformBadCharForDML(a.value)+'\'';
        }
        else {
          tmpS = "null"; //$NON-NLS-1$
        }
        //tmpS = insertEsc(a.value.toString().trim())+"',";
        if (!update) tableValues.append(tmpS).append(","); //$NON-NLS-1$
        if (update) tableUpdate.append(tmpS).append(","); //$NON-NLS-1$
      }

    }

    if (update) tableUpdate.setCharAt(tableUpdate.length()-1,' ');
    if (!update) tableValues.setCharAt(tableValues.length()-1,' ');
    if (!update) tableValues.append(" ) "); //$NON-NLS-1$
    return hasLOBS.toArray(new OBAttribute[hasLOBS.size()]);
  }

  /** 
   * Besetzt die SQL-Teil-Statements (Member-Variablen) 
   * VALUES (...) und SET ... f�r insert und update.
   * @param context
   * @param tableUpdate
   * @param valuesOut Liste der in das PreparedStatement einzufuegenden Werte - Ausgabe-Parameter
   * @param typesOut Liste der Typen der in das PreparedStatement einzufuegenden Werte - Ausgabe-Parameter
   * @return Kommagetrennte Liste der CLobs, falls welche vorhanden sind.
   */
  protected ArrayList<OBAttribute> getInfoPreparedUpdate(OBContext context, StringBuffer tableUpdate, ArrayList<String> valuesOut, ArrayList<Integer> typesOut) throws OBException {
    ArrayList<OBAttribute> hasLOBS = new ArrayList<OBAttribute>();
    OBAttribute a;
    int j;
    boolean isInDate = false;
    boolean isPrimaryKey = false;
    boolean isLockRow = false;
    boolean isTimestamp = false;
    String tmpS;

    for (j=0; j< attArr.length; j++) {
      isInDate = false;
      isPrimaryKey = false;
      isLockRow = false;
      isTimestamp = false;

      a= attArr[j];
      //try{
      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG) {
        if (a.name.equals(primaryKey)) {
          isPrimaryKey = true;
        }
        else if (a.name.equals("lockRow")) { //$NON-NLS-1$
          isLockRow = true;
        }
      }
      else if (a.type==OBConstants.DATE) {
        if (a.name.equals("inDate")) { //$NON-NLS-1$
          isInDate = true;
        }
        else if (a.name.equals("timestamp")) { //$NON-NLS-1$
          isTimestamp = true;
        }
      }

      if (!isInDate &&
          !isPrimaryKey &&
          !isLockRow &&
          !isTimestamp ) {

      }
      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG || 
          a.type==OBConstants.DOUBLE || a.type==OBConstants.BOOLEAN) {
        if (!isPrimaryKey && !isLockRow) {
          if(a.value.length() > 0) {
            tmpS = a.value;
          }
          else {
            tmpS = "null"; //$NON-NLS-1$
          }
          tableUpdate.append(a.name).append("=");
          tableUpdate.append("?,");
          valuesOut.add(tmpS);
          typesOut.add(a.type==OBConstants.DOUBLE? Types.DOUBLE:Types.INTEGER);
        }

      }
      else if (a.type==OBConstants.CLOB) {
//        tableUpdate.append(a.name).append("=").append(OBDriver.getEmptyClob(context)).append(',');
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.BLOB) {
//        tableUpdate.append(a.name).append("=").append(OBDriver.getEmptyBlob(context)).append(',');
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.DATE) {
        try {
          if( a.name.equals("changeDate") ) { //$NON-NLS-1$
            tableUpdate.append(a.name).append("=").append(OBDriver.getSysDateString(context));
            tableUpdate.append(",");
          }
          else if (isInDate) {
            // ntbd
          }
          else if (isTimestamp) {
            // ntbd
          }
          else if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            tableUpdate.append(a.name).append("=");
            tableUpdate.append(OBDriver.getSysDateString(context)).append(",");
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                setDateFormat(OBConstants.NLS_DATETIME_FORMAT); // mit Uhrzeit!
              }
              tmpS =  a.value;
            }
            else {
              tmpS = "null"; //$NON-NLS-1$
            }
            tableUpdate.append(a.name).append("=");
            tableUpdate.append("TO_DATE(?,'" + getDateFormat() + "'),"); //$NON-NLS-1$ //$NON-NLS-2$
            valuesOut.add(tmpS);
            typesOut.add(Types.VARCHAR);
          }
        }
        catch (Exception ex) {
          logger.debug(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else if (a.type==OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
        try {
          if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            tableUpdate.append(a.name).append("=");
            tableUpdate.append("?,");
            valuesOut.add(OBDriver.getSysDateString(context));
            typesOut.add(Types.VARCHAR);
          }
          else if ("SYSTIMESTAMP".equalsIgnoreCase(a.value)) { //$NON-NLS-1$
            tableUpdate.append(a.name).append("=").append(OBDriver.getSysDateString(context));
            tableUpdate.append(",");
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (a.getFormatMask()==null || a.getFormatMask().length()==0) {
                if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                  a.setFormatMask(OBConstants.NLS_TIMESTAMP_FORMAT); // mit Uhrzeit!
                }
                else {
                  a.setFormatMask(OBConstants.NLS_DATE_FORMAT); // mit Uhrzeit!
                }
              }
              tmpS = a.value;
            }
            else {
              tmpS = "null"; //$NON-NLS-1$
              a.setFormatMask(OBConstants.NLS_DATE_FORMAT);
            }
            tableUpdate.append(a.name).append("=");
            tableUpdate.append("TO_TIMESTAMP_TZ(?,'" + a.getFormatMask() + "'),"); //$NON-NLS-1$ //$NON-NLS-2$
            valuesOut.add(tmpS);
            typesOut.add(Types.VARCHAR);
          }
        }
        catch (Exception ex) {
          logger.error(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else {
        if(a.value.length() > 0) {
          tmpS = transformBadCharForDML(a.value);
        }
        else {
          tmpS = "null"; //$NON-NLS-1$
        }
        tableUpdate.append(a.name).append("=");
        tableUpdate.append("?,");
        valuesOut.add(tmpS);
        typesOut.add(Types.VARCHAR);
      }
    }

    tableUpdate.setCharAt(tableUpdate.length()-1,' ');
    
    return hasLOBS;
  }
  /** 
   * Besetzt die SQL-Teil-Statements (Member-Variablen) 
   * VALUES (...) und SET ... f�r insert und update.
   * @param context
   * @param tableValueStringOut
   * @param valuesOut
   * @param typesOut
   * @return Kommagetrennte Liste der CLobs, falls welche vorhanden sind.
   */
  protected OBAttribute[] getInfoPreparedInsert(OBContext context, StringBuffer tableValueStringOut, ArrayList<String> valuesOut, ArrayList<Integer> typesOut) throws OBException {
    ArrayList<OBAttribute> hasLOBS = new ArrayList<OBAttribute>();
    OBAttribute a;
    int j;
    boolean isInDate = false;
    boolean isPrimaryKey = false;
    boolean isLockRow = false;
    boolean isTimestamp = false;
    String tmpS;

    tableValueStringOut.append("VALUES ( "); //$NON-NLS-1$

    for (j=0; j< attArr.length; j++) {
      isInDate = false;
      isPrimaryKey = false;
      isLockRow = false;
      isTimestamp = false;

      a= attArr[j];
      //try{
      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG) {
        if (a.name.equals(primaryKey)) {
          isPrimaryKey = true;
        }
        else if (a.name.equals("lockRow")) { //$NON-NLS-1$
          isLockRow = true;
        }
      }
      else if (a.type==OBConstants.DATE) {
        if (a.name.equals("inDate")) { //$NON-NLS-1$
          isInDate = true;
        }
        else if (a.name.equals("timestamp")) { //$NON-NLS-1$
          isTimestamp = true;
        }
      }

      if (a.type==OBConstants.INTEGER || a.type==OBConstants.LONG || 
          a.type==OBConstants.DOUBLE || a.type==OBConstants.BOOLEAN) {
        if (isPrimaryKey) {
          if (hasPrimaryKeySequence()) {
            long pk = nextValPK(context);
            tableValueStringOut.append("?,"); //$NON-NLS-1$
            valuesOut.add(String.valueOf(pk));
            typesOut.add(Types.INTEGER);
            
            setPrimaryKey(pk);
          }
          else {
            if (a.value.length() > 0) {
              tableValueStringOut.append("?,"); //$NON-NLS-1$
              valuesOut.add(a.value);
              typesOut.add(Types.INTEGER);
            }
          }
        }
        else if (isLockRow) {
          tableValueStringOut.append(" -1,"); //$NON-NLS-1$
        }
        else  {
          if(a.value.length() > 0) {
            tmpS = a.value;
          }
          else {
            tmpS = "null"; //$NON-NLS-1$
          }
          tableValueStringOut.append("?,"); //$NON-NLS-1$
          valuesOut.add(tmpS);
          typesOut.add(a.type==OBConstants.DOUBLE? Types.DOUBLE : Types.INTEGER);
        }
      }
      else if (a.type==OBConstants.CLOB) {
        tableValueStringOut.append(OBDriver.getEmptyClob(context)).append(',');//$NON-NLS-1$
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.BLOB) {
        tableValueStringOut.append(OBDriver.getEmptyBlob(context)).append(',');//$NON-NLS-1$
        hasLOBS.add(a);
      }
      else if (a.type==OBConstants.DATE) {
        try {
          if( a.name.equals("changeDate") ) { //$NON-NLS-1$
            tableValueStringOut.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (isInDate) {
            tableValueStringOut.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (isTimestamp) {
            tableValueStringOut.append(" "+OBDriver.getSysDateString(context)+","); //$NON-NLS-1$ //$NON-NLS-2$
          }
          else if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            tableValueStringOut.append(OBDriver.getSysDateString(context)).append(","); //$NON-NLS-1$
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                setDateFormat(OBConstants.NLS_DATETIME_FORMAT); // mit Uhrzeit!
              }
              valuesOut.add(a.value);
              typesOut.add(Types.VARCHAR);
              tableValueStringOut.append("TO_DATE(?,'" + getDateFormat() + "'),");
            }
            else {
              valuesOut.add("null");
              typesOut.add(Types.VARCHAR);
              tableValueStringOut.append("TO_DATE(?,'" + getDateFormat() + "'),");
              
            }
          }
        }
        catch (Exception ex) {
          logger.debug(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else if (a.type==OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
        try {
          if (a.value.equalsIgnoreCase("SYSDATE")) { //$NON-NLS-1$
            tableValueStringOut.append(OBDriver.getSysDateString(context)).append(","); //$NON-NLS-1$
          }
          else if ("SYSTIMESTAMP".equalsIgnoreCase(a.value)) { //$NON-NLS-1$
            tableValueStringOut.append(OBDriver.getSysTimeStampString(context)).append(","); //$NON-NLS-1$
          }
          else {
            if(a.value.length() > 0) {
              // mit oder ohne Uhrzeit?
              if (a.getFormatMask()==null || a.getFormatMask().length()==0) {
                if (-1 != a.value.indexOf(' ') && -1 != a.value.indexOf(':')) {
                  a.setFormatMask(OBConstants.NLS_TIMESTAMP_FORMAT); // mit Uhrzeit!
                }
                else {
                  a.setFormatMask(OBConstants.NLS_DATE_FORMAT); // ohne Uhrzeit!
                }
              }
              valuesOut.add(a.value);
              typesOut.add(Types.VARCHAR);
              tableValueStringOut.append("TO_TIMESTAMP_TZ(?,'" + a.getFormatMask() + "'),");
            }
            else {
              tableValueStringOut.append(OBDriver.getSysTimeStampString(context)).append(","); //$NON-NLS-1$
            }
          }
        }
        catch (Exception ex) {
          logger.error(XynaContextFactory.getSessionData(context) + "error getting column info",ex);//$NON-NLS-1$
        }
      }
      else {
        if(a.value.length() > 0) {
          valuesOut.add(transformBadCharForDML(a.value));
          tableValueStringOut.append("?,");
          typesOut.add(Types.VARCHAR);
        }
        else {
          valuesOut.add("null");
          tableValueStringOut.append("?,");
          typesOut.add(Types.VARCHAR);
        }
      }

    }

    tableValueStringOut.setCharAt(tableValueStringOut.length()-1,' ');
    tableValueStringOut.append(" ) "); //$NON-NLS-1$
    return hasLOBS.toArray(new OBAttribute[hasLOBS.size()]);
  }

  /** Prueft, ob ein Objekt in Ordnung ist, soweit moeglich.
      Kann in einer abgeleiteten Klasse ueberschrieben werden
   * @param context
      @return true, wenn das Objekt in Ordnung ist, false, wenn nicht
  */
  public boolean isValid(OBContext context) {
    try {
      validate(context);
      return true;
    }
    catch (Exception e) {
      logger.debug(XynaContextFactory.getSessionData(context) + "error validsting data",e);//$NON-NLS-1$
      return false;
    }
  }


  /** Liefert true, wenn eine SQL Sequence fuer die
      Erzeugung des PrimKey vorhanden ist.
      Muss in der abgeleitetetn DB-Klasse ueberschrieben werden.
   * @return true, wenn es ein PK-Sequenz gibt
  */
  public boolean hasPrimaryKeySequence() {
    return true;
  }

  /** Fuehrt insert oder update anhand der aktuell
      gesetzten Objekte durch.
      Beim update muss der PrimaryKey gesetzt sein.
      Vor dem insert wird ein  preValidate durchgefuehrt.
      Am Schluss wird Validate durchgefuehrt.
   * @param context
   * @param update
   * @throws OBException
  */
  public void set(OBContext context, boolean update) throws OBException {
    try {
      validateBeforeSet(context, update);
      if (update) {
        if (primaryKeyAtt.getValue().equals("")) { //$NON-NLS-1$
          logger.fatal(XynaContextFactory.getSessionData(context) + "Primary key not set"); //$NON-NLS-1$
        }
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          updateInternal(context,getPrimaryKey(),-1);
        }
        else {
          updateInternalPrepared(context, getPrimaryKey(), -1);
        }
      }
      else {
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          insertInternal(context);
        }
        else {
          insertInternalPrepared(context);
        }
      }
      // Bug 6351: Indate und ChangeDate nachselektieren
      try {
        OBObject res = getObjectFromDB(context);
        setValueIC("changeDate", res.getValueIC("changeDate"));//$NON-NLS-1$//$NON-NLS-2$
        if (!update) {
          setValueIC("inDate", res.getValueIC("inDate"));//$NON-NLS-1$//$NON-NLS-2$
        }
      }
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error saving data",e);//$NON-NLS-1$
        // ntbd!
      }
      
      validate(context);
      if(getCorrectConnection(context) instanceof OBDatabase) {
        ((OBDatabase) getCorrectConnection(context)).registerObject(this);
      }
      else { // Fremd-Connection: Da deferrable evtl nicht unterstuetzt wird, keine Experimente
        validateDeferrable(context);
      }
    }
    catch (OBException e) {
      throw e;
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error saving data",e);//$NON-NLS-1$
      throw new OBException(e.getMessage());
    }
  }

  /**
   * Inserts the Data into the database. Shortcut for set(context,false)
   * Bugz 10166
   * @param context
   * @return this
   * @throws OBException
   */
  public OBDBObject insert(OBContext context) throws OBException {
    set(context,false);
    return this;
  }
  
  /**
   * Updates the Data in the database. Shortcut for set(context,true)
   * Bugz 10166
   * @param context
   * @return this
   * @throws OBException
   */
  public OBDBObject update(OBContext context) throws OBException {
    set(context,true);
    return this;
  }
  
  /** Diese Methode ist fuer Validierungen am abgeleiteten Tabellen-Objekt
      gedacht. Sie wird vor DB-Aktionen ausgefuehrt.
   * @param context
      @param update Zeigt an ob es sich um ein Update oder ein Insert handelt.
   * @throws OBException Wird per Hand geworfen, wenn Objekt nicht in Ordnung ist
  */
  public void validateBeforeSet(OBContext context, boolean update) throws OBException {
    // Wird ueberschrieben
  }

  /** Prueft, ob ein Objekt in Ordnung ist, soweit moeglich.
      Entspricht datenbanktechnisch den non-deferrable Constraints.
      Kann in einer abgeleiteten Klasse ueberschrieben werden
   * @param context
   * @throws OBException Wird per Hand geworfen, wenn Objekt nicht in Ordnung ist
  */
  public void validate(OBContext context) throws OBException {
    // Wird ueberschrieben
  }

  /** Prueft, ob ein Objekt in Ordnung ist, soweit moeglich.
      Entspricht datenbanktechnisch den deferrable Constraints.
      Kann in einer abgeleiteten Klasse ueberschrieben werden
   * @param context
   * @throws OBException - Wird per Hand geworfen, wenn Objekt nicht in Ordnung ist
  */
  public void validateDeferrable(OBContext context) throws OBException {
    // Wird ueberschrieben
  }


  /** Liefert den nextVal zur Primary Key Sequence. 
   * @param context
   * @return nextVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public long nextValPK(OBContext context) throws OBException {
    throw new OBException(OBException.OBErrorNumber.pkUnkown1, new String[] {getTableName()});
  } 

  /** Liefert den currentVal zur Primary Key Sequence. 
   * @param context
   * @return currentVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public long currentValPK(OBContext context) throws OBException {
    throw new OBException(OBException.OBErrorNumber.pkUnkown1, new String[] {getTableName()});
  } 

  
  /**
   * [sd] fuer CCBErweiterung
   * Um einen Loesch-Vorgang "klammern" zu koennen, wird diese Methode von
   * XOBDeleteSelect zu Beginn und am Ende einer Loesch-Schleife aufgerufen
   * Klassen, die an dieser Information interessiert sind, muessen die
   * Methode ueberschreiben.
   * Verallgemeinert: Beliebige notification Nachrichten
   * 
   * NOTE: Soll diese Methode vom Client (per DTO) an den Server weitergereicht
   * werden MUSS im xml-File der TK-Objekts die <Capability type='NOTIFY'/>
   * gesetzt sein. 
   * @param context 
   * @param type 
   * @param param 
   * 
   * @throws OBException
   */
  public void notifyTk(OBContext context, int type, int param) throws OBException {
     // hier nichts tun, interessierte Klassen muessen ueberschreiben
  } 
  
  
  /**
   * @param context
   * @param dto
   * @return DBSchema.sqlTabellenName
   * @throws OBException
   */
  public static String getSQLRepresentation(OBContext context, OBDTO dto) throws OBException {
    return OBDriver.getTableName(context,context.getSchema(dto.getProjectSchema()), dto.getSQLName());
  }
  
 /**
   * Liefert den im Context gespeicherten Namen des Benutzers oder unbekannt
   * 
   * @param context
   * @param staffId
   * @return
   */
  private String getStaffName(OBContext context) {
    if (context.getStaffName() != null && context.getStaffName().length() > 0) {
      return context.getStaffName();
    }
    else {
      return "unbekannt";//$NON-NLS-1$
    }
  }


}
