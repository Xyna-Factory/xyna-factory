/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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



import gip.base.common.OBContextInterface;
import gip.base.common.OBException;
import gip.base.db.drivers.OBDriverInterface;
import gip.base.db.drivers.OBMySQLDriver;
import gip.base.db.drivers.OBOracleDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;



/**
 * ContextFactory
 */
public class XynaContextFactory {
  
  public static enum AvailableDriver {
    Oracle,
    MySQL;
    
    
    
    public OBDriverInterface getOBDriver() {
      if (this==Oracle) {
        return new OBOracleDriver();
      }
      else if (this==MySQL) {
        return new OBMySQLDriver();
      }
      else {
        // Geht nicht
        return null;
      }
    }
  }

  private transient static Logger logger = Logger.getLogger(XynaContextFactory.class);
  
  /**
   * Die zentrale Methode, die benutzt werden soll, um einen OBContext zu erzeugen. Hier in der Variante auf dem
   * Application-Server mit Datasource.
   * 
   * @param projectSchema Projektname. Wird benoetigt, um die korrekten DB-Benutzernamen in den SQLs zu setzen
   * @param dbSchema DB-Benutzername, der in den SQL-Statements verwendet wird.
   * @param datasource Name der zu verwendenden Datasource
   * @param driver Welcher Treiber soll benutzt werden
   * @return Context-Objekt
   * @throws OBException
   */
  public static OBContext createContext(String projectSchema, String dbSchema, String datasource, AvailableDriver driver) throws OBException {
    OBContext context = new OBContext();
    context.setDriver(driver.getOBDriver());
    fillSqlConnections(context, datasource, context.getDriver());
    context.setMessageGenerator(new MessageGenerator());
    context.addProjectSchema(projectSchema, dbSchema);
    return context;
  }


  /**
   * Die zentrale Methode, die benutzt werden soll, um einen OBContext zu erzeugen. Hier in der Variante auf dem
   * Application-Server mit Datasource.
   * 
   * @param projectSchema Projektname. Wird benoetigt, um die korrekten DB-Benutzernamen in den SQLs zu setzen
   * @param dbSchema DB-Benutzername, der in den SQL-Statements verwendet wird.
   * @param url 
   * @param user 
   * @param pw 
   * @param driver Welcher Treiber soll benutzt werden
   * @return Context
   * @throws OBException
   */
  public static OBContext createContext(String projectSchema, String dbSchema,
                                        String url, String user, String pw, 
                                        AvailableDriver driver) throws OBException {
    OBContext context = new OBContext();
    context.setDriver(driver.getOBDriver());
    fillSqlConnections(context, url, user, pw, context.getDriver());
    context.setMessageGenerator(new MessageGenerator());
    context.addProjectSchema(projectSchema, dbSchema);
    return context;

  }


  /**
   * Zentrale Methode, um einen benutzen OBContext wieder zu vernichten
   * 
   * @param context Der zu vernichtende OBContext
   * @throws OBException
   */
  public static void passivateContext(OBContext context) throws OBException {
    if (context!=null) {
      context.unsetVSessionInfo();
    }
    closeConnections(context);
  }


  private static void closeConnections(OBContext context) throws OBException {
    try {
      if (context != null) {
        if (context.getDataConnection()!=null && context.getDataConnection().isClosed() == false) {
          context.getDataConnection().rollback();
          context.getDataConnection().close();
        }
        if (context.getLockConnection()!=null && context.getLockConnection().isClosed() == false) {
          context.getLockConnection().rollback();
          context.getLockConnection().close();
        }
        if (context.getMessConnection()!=null && context.getMessConnection().isClosed() == false) {
          context.getMessConnection().rollback();
          context.getMessConnection().close();
        }
        context.setDataConnection(null);
        context.setLockConnection(null);
        context.setMessConnection(null);
      }
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "Die DB-Connection konnte nicht geschlossen werden.",e);//$NON-NLS-1$
      throw new OBException("Die DB-Connection konnte nicht geschlossen werden.");//$NON-NLS-1$
    }
  }

  private static void fillSqlConnections(OBContext obc, String datasource, OBDriverInterface driver) throws OBException {
    try {
      obc.setDataConnection(getConnection(null, datasource, driver));
      obc.setMessConnection(getConnection(null, datasource, driver));
      obc.setLockConnection(getConnection(null, datasource, driver));
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(obc) + "Es konnte keine DB-Connection erstellt werden.",e);//$NON-NLS-1$
      throw new OBException("Es konnte keine DB-Connection erstellt werden.");//$NON-NLS-1$
    }
  }

  private static void fillSqlConnections(OBContext obc, String url, String user, String pw, 
                                         OBDriverInterface driver) throws OBException {
    try {
      obc.setDataConnection(getConnection(url, user, pw, driver));
      obc.setMessConnection(getConnection(url, user, pw, driver));
      obc.setLockConnection(getConnection(url, user, pw, driver));
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(obc) + "Es konnte keine DB-Connection erstellt werden.",e);//$NON-NLS-1$
      throw new OBException("Es konnte keine DB-Connection erstellt werden.");//$NON-NLS-1$
    }
  }

  /**
   * Erzeugt eine DB-Connection aus einer Data-Source. Ein initialContext kann uebergeben werden Achtung: ist das
   * Passivieren von Session-EJBs aktiviert, gibt es probleme mit dem InitialContext . (s. a.:
   * http://download-west.oracle.com/otn_hosted_doc/ias/preview/web.1013/b14428/session.htm#i1018838)
   */
  private static OBConnectionInterface getConnection(InitialContext ictx, String datasource, OBDriverInterface driver)
                  throws Exception {
    OBDatabase con = null;

    // Die Connection liefert uns eine DataSource des ApplicationServers,
    // die per Lookup ueber JNDI ermittelt wird.

    // Wir nehmen hier die 'EJB-Location' der Data-Source und verwenden damit auch den OC4J-Connection-Pool.
    // Wird die 'Location' verwendet, wird der JDBC-Connection-Pool verwendet, bei beiden sollte die
    // DataSource-Klasse 'oracle.jdbc.pool.OracleConnectionCacheImpl' sein.
    logger.debug("Using DataSource: " + datasource);//$NON-NLS-1$

    // InitialContext muss uebergeben werden, wenn Methode ausserhalb des
    // ApplicationServers aufgerufen wird
    InitialContext initCtx = ictx != null ? ictx : new InitialContext();
    DataSource dataSource = (DataSource) initCtx.lookup(datasource);
    logger.debug("Got DataSource: " + dataSource.toString());//$NON-NLS-1$
    Connection dsConnection = dataSource.getConnection();
    logger.debug("Got jdbcConnection: " + dsConnection.toString() + "(" + dsConnection.hashCode() + ")");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

    con = new OBDatabase(dsConnection);
    con.setAutoCommit(false);

    // Constraints auf Deferred setzen
    driver.setConstaintsDeferred(con);
    return con;
  }


  /**
   * Erzeugt die Connection direkt aus JDBC
   */
  private static OBConnectionInterface getConnection(String url, String user, String pw, 
                                                     OBDriverInterface driver)
                  throws Exception {
    OBDatabase con = null;
    
    // Create the properties object that holds all database details
    Properties props = new Properties();
    props.put("user", user);//$NON-NLS-1$
    props.put("password", pw);//$NON-NLS-1$
    props.put("SetBigStringTryClob", "true");//$NON-NLS-1$//$NON-NLS-2$

    // Load the Oracle JDBC driver class.
    driver.registerDriver();
 
    // Get the database connection 
    Connection jdbcConnection =  DriverManager.getConnection(url, props);
    con = new OBDatabase(jdbcConnection);
    con.setAutoCommit(false);

    // Constraints auf Deferred setzen
    driver.setConstaintsDeferred(con);
    return con;
  }
  
  public static String getSessionData(OBContextInterface con){
    if (con != null && 
        con.getStaffName() != null && 
        con.getSessionIdentifier() != null){
      return "[" + con.getStaffName() + ":" + con.getSessionIdentifier() + "] "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    else{
      return ""; //$NON-NLS-1$
    }
  }
}
