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
package com.gip.xyna.utils.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import javax.naming.Context;
import javax.sql.DataSource;

import com.gip.xyna.utils.db.exception.DBUtilsException;

/**
 * ConnectionFactory zum Bauen und Markieren von Connections
 * Die Connections sollten markiert werden, um in der DB prüfen zu können,
 * wer welche Connection geöffnet hat.
 */
public class ConnectionFactory {

    static private boolean ignoreMarkConnection=false;//value==true: Ignore all markConnection calls
/**
   * if value==true: Ignore all markConnection calls
   * @param value
   */ 
    static public void setIgnoreMarkConnection(boolean value){
      ignoreMarkConnection=value;
    }  

  /**
   * Build up a sql connection to the given datasource. Look up context to
   * receive datasource.
   * @param context
   * @param datasource
   * @param clientInfo
   * @return
   */
  public static Connection getConnection( Context context, String datasource, String clientInfo ) {
    Connection connection = null;
    try {
      DataSource dataSource = (DataSource) context.lookup(datasource);
      connection = dataSource.getConnection();
      connection.setAutoCommit(false);
    } catch( Exception e ) {
      throw new DBUtilsException( "Connection could not established.", e );
    }
    markConnection( connection, clientInfo );
    return connection;
  }
 
  /**
   * @param user
   * @param passwd
   * @param db
   * @param clientInfo
   * @return
   */
  public static Connection getConnection( String user, String passwd, String db, String clientInfo ) {
    Connection connection;
    try {
      Class<?> driverClass = Class.forName("oracle.jdbc.OracleDriver", true, ConnectionFactory.class.getClassLoader());
      DriverManager.registerDriver((Driver)driverClass.newInstance());
      connection = DriverManager.getConnection("jdbc:oracle:thin:@"+db, user, passwd );
      connection.setAutoCommit(false);
    } catch (Exception e) {
      throw new DBUtilsException( "Connection could not established.", e );
    }
    markConnection( connection, clientInfo );
    return connection;
  }
  
  /**
   * @param connectString (Beispiel user/passwd@localhost:1521:xynadb)
   * Auch die Falschschreibung user/passwd@localhost:1521/xynadb ist zulässig
   * @param clientInfo
   * @return
   */
  public static Connection getConnection(String connectString, String clientInfo ) {
    int pos1 = connectString.indexOf("/");
    int pos2 = connectString.indexOf("@");
    String user = connectString.substring(0, pos1);
    String passwd = connectString.substring(pos1+1, pos2);
    String db = connectString.substring(pos2+1);
    db = db.replace('/',':'); //evtl. war connectString falsch geschrieben: (Beispiel user/passwd@localhost:1521/xynadb), 
    return getConnection( user, passwd, db, clientInfo );
  }

  
  /**
   * Markieren der Connection mittels der clientInfo
   * @param connection
   * @param clientInfo
   */
  public static void markConnection(Connection connection, String clientInfo) {
    if(ignoreMarkConnection) {
      return;
    }
    PreparedStatement ps=null;
    try{
      ps=connection.prepareStatement("{call DBMS_APPLICATION_INFO.set_client_info(?)}");
      ps.setString(1,clientInfo);
      ps.setQueryTimeout(10);
      ps.execute();
    } catch(Throwable t){
      //Ein Fehler hier ist wenig schön, da die Connection nicht markiert ist, 
      //aber da kein Logger vorhanden ist, wird der Fehler unterdrückt, damit 
      //die eigentliche Bearbeitung fortgesetzt werden kann
    }
    finally {
      try{ps.close();}catch(Throwable t2 ){}
    }
  }
  
  /**
   * @deprecated diese methode ist absichtlich nicht public und gibt es nur aus abwärtskompatibilitätsgründen, um zu erlauben, dass man
   * in den sql utils beim close connection nicht immer das markConnection aufruft.
   * @param connection
   * @param markConnection
   */
  static void closeConnection(Connection connection, boolean markConnection) {
    boolean closed = (null == connection);
    if (closed) {
      return;
    }
    try {
      closed = connection.isClosed();
    } catch (Throwable t) {
      //Fehler hier wird unterdrückt, da schlecht behandelbar, 
      //wenn wirklich ein schwerwiegender Fehler auftritt, wird 
      //unten ein DBUtilsException geworfen.
    }
    if (closed) {
      return;
    }
    if (markConnection) {
      markConnection(connection, "closed " + System.currentTimeMillis()); //Markierung entfernen
    }
    try {
      connection.close();
    } catch (Throwable t) {
      throw new DBUtilsException("Connection could not be closed.", t);
    }
  }
  
  public static void closeConnection( SQLUtils sqlUtils ) {
    boolean closed = (null == sqlUtils || sqlUtils.getConnection() == null );
    if( closed ) {
      return;
    }
    try {
      closed = sqlUtils.getConnection().isClosed();
    } catch( Throwable t ) {
      //Fehler hier wird unterdrückt, da schlecht behandelbar, 
      //wenn wirklich ein schwerwiegender Fehler auftritt, wird 
      //unten ein DBUtilsException geworfen.
    }
    if( closed ) {
      return;
    }
    markConnection(sqlUtils.getConnection(),""); //Markierung entfernen
    sqlUtils.closeConnection();
    if( sqlUtils.getLastException() != null ) {
      throw new DBUtilsException( "Connection could not be closed.", sqlUtils.getLastException() );
    }
  }

  public static void closeConnection(Connection connection) {
    closeConnection(connection, true);
  }

}
