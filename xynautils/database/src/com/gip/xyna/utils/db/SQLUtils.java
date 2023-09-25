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

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.gip.xyna.utils.db.exception.DBUtilsException;

public class SQLUtils {

   private SQLException lastException = null;
   private SQLUtilsLogger logger = null;
   private Connection connection = null;
   private boolean logException = true;
   private StatementCache statementCache = null;
   private int queryTimeout = -1;
   private String name;
   private boolean includeResultSetReaderNullElements = true;
   
   
   public SQLUtils() {
   }

   public SQLUtils(Connection connection) {
      this(connection, null);
      this.name = "unknown";
   }

   public SQLUtils(Connection connection, SQLUtilsLogger logger) {
      this.connection = connection;
      this.logger = logger;
      this.name = "unknown";
   }

   /**
    * Build up a sql connection to the given datasource. Look up context to
    * receive datasource.
    * 
    * @param context
    *           naming context which knows the datasource
    * @param datasource
    *           name of the datasource
    * @throws NamingException
    * @throws SQLException
    * @deprecated Use SQLUtils( ConnectionFactory.getConnection( context, datasource, clientInfo ) ) instead
    */
   public SQLUtils(Context context, String datasource) throws NamingException,
         SQLException {
      this(context, datasource, null);
   }

   /**
    * Build up a sql connection to the given datasource. Look up context to
    * receive datasource. Use given logger for log information.
    * 
    * @param context
    *           naming context which knows the datasource
    * @param datasource
    *           name of the datasource
    * @throws NamingException
    * @throws SQLException
    * @deprecated Use SQLUtils( ConnectionFactory.getConnection( context, datasource, clientInfo ), logger ) instead
    */
   public SQLUtils(Context context, String datasource, SQLUtilsLogger logger)
         throws NamingException, SQLException {
      DataSource dataSource = (DataSource) context.lookup(datasource);
      this.connection = dataSource.getConnection();
      this.connection.setAutoCommit(false);
      this.logger = logger;
      this.name = datasource;
   }

   @Override
   public String toString() {
     return name;
   }
   
   /**
    * Sets a new string for the toString()-output 
    * @param name
    */
   public void setName(String name) {
      this.name = name;
    }
   
   /**
    * Cachet das PreparedStatement zur späteren Wiederverwendung Achtung: nicht
    * für Callable-Statements verwendbar
    * 
    * @param sql
    */
   public void cacheStatement(String sql) {
      if (statementCache != null) {
         try {
            statementCache.cache(connection, sql);
         } catch (SQLException e) {
            logException(e);
         }
      }
   }

   /**
    * Setzt den QueryTimeout
    * queryTimeout = -1: QueryTimeout wird nicht gesetzt
    * Ansonsten: "seconds the new query timeout limit in seconds; zero means there is no limit"
    * @param queryTimeout 
    * @throws IllegalArgumentException wenn queryTimeout < -1
    */
   public void setQueryTimeout( int queryTimeout) {
      if( queryTimeout < -1 ) {
         throw new IllegalArgumentException("queryTimeout >= -1 expected");
      }
      this.queryTimeout = queryTimeout;
   }
   
   public int getQueryTimeout() {
      return queryTimeout;
   }
      
   /**
    * Bau eines PreparedStatement
    * 
    * @param connection
    * @param sql
    * @return
    * @throws SQLException
    */
   public PreparedStatement prepareStatement(String sql) throws SQLException {
      PreparedStatement ps = null;
      if (statementCache != null) {
         ps = statementCache.getPreparedStatement(sql);
      }
      if (ps == null) {
         ps = connection.prepareStatement(sql);
      }
      if (ps != null && queryTimeout != -1 ) {
         ps.setQueryTimeout(queryTimeout);
      }
      return ps;
   }

   /**
    * Bau eines CallableStatement
    * 
    * @param connection
    * @param sql
    * @return
    * @throws SQLException
    */
   public CallableStatement prepareCall(String sql) throws SQLException {
      CallableStatement cs = connection.prepareCall(sql);
      if( cs != null && queryTimeout != -1 ) {
        cs.setQueryTimeout(queryTimeout);
      }
      return cs;
   }

   /**
    * Eintragen aller Parameter in das PreparedStatement
    * 
    * @param stmt
    * @param params
    * @throws SQLException
    */
   public void addParameter(PreparedStatement stmt, Parameter params) throws SQLException {
      if (params != null ) {
        params.addParameterTo( stmt );
      }
   }
   
   /**
    * Eintragen eines Parameters in das PreparedStatement
    * 
    * @param stmt
    * @param pos
    * @param param
    * @throws SQLException
    * @deprecated Parameter-Klasse benutzen
    */
   public void addParameter(PreparedStatement stmt, int pos, Object param)
         throws SQLException {
     Parameter p = new Parameter();
     p.addParameterTo(stmt, pos, param);
   }


   /**
    * Ausführung einer DML-Statements (Insert, Update, Delete)
    * 
    * @param stmt
    * @return
    * @throws SQLException
    */
   public int excuteUpdate(PreparedStatement stmt) throws SQLException {
      return stmt.executeUpdate();
   }

   /**
    * Ausführung einer Query
    * 
    * @param stmt
    * @return
    * @throws SQLException
    */
   public ResultSet excuteQuery(PreparedStatement stmt) throws SQLException {
      return stmt.executeQuery();
   }

   /**
    * Lesen der ersten Zeile des ResultSets und Ausgabe der Daten mittels des
    * ResultSetReaders
    * 
    * @param <T>
    * @param rs
    * @param resultSetReader
    * @return
    * @throws SQLException
    */
   public <T> T queryOneRow(ResultSet rs, ResultSetReader<T> resultSetReader)
         throws SQLException {
      if (rs.next()) {
         return resultSetReader.read(rs);
      }
      return null;
   }

   /**
    * Lesen der ersten Zeile des ResultSets und Ausführung der
    * resultSetReaderFunction
    * 
    * @param rs
    * @param resultSetReaderFunction
    * @return false, wenn keine Daten vorlagen
    * @throws SQLException
    */
   public boolean queryOneRow(ResultSet rs,
         ResultSetReaderFunction resultSetReaderFunction) throws SQLException {
      if (rs.next()) {
         resultSetReaderFunction.read(rs);
         return true;
      }
      return false;
   }

   /**
    * Lesen des ResultSets und Ausgabe der Daten mittels des ResultSetReaders in
    * eine ArrayList
    * 
    * @param <T>
    * @param rs
    * @param resultSetReader
    * @return
    * @throws SQLException
    */
   public <T> ArrayList<T> query(ResultSet rs,
         ResultSetReader<T> resultSetReader) throws SQLException {
      ArrayList<T> list = new ArrayList<T>();
      while (rs.next()) {
        T t = resultSetReader.read(rs);
        if (includeResultSetReaderNullElements || t != null) {
          list.add(t);
        }
      }
      return list;
   }

   /**
    * Lesen des ResultSets und Ausführung der resultSetReaderFunction für jede
    * Zeile
    * 
    * @param rs
    * @param resultSetReaderFunction
    * @return Anzahl der gelesenen Zeilen
    * @throws SQLException
    *            , Exception
    */
   public int query(ResultSet rs,
         ResultSetReaderFunction resultSetReaderFunction) throws SQLException {
      int i = 0;
      boolean readMore = true;
      while (readMore && rs.next()) {
         readMore = resultSetReaderFunction.read(rs);
         ++i;
      }
      return i;
   }

   /**
    * Schließen des ResultSets und des PreparedStatements
    * 
    * @param rs
    * @param stmt
    */
   public void finallyClose(ResultSet rs, PreparedStatement stmt) {
      if (rs != null) {
         try {
            rs.close();
         } catch (SQLException e) {
         }
      }
      if (stmt != null) {
         if (statementCache == null || (!statementCache.contains(stmt))) {
            // Statement nur schließen, wenn Stament nicht dem Cache gehört
            try {
               stmt.close();
            } catch (SQLException e) {
            }
         } else {
           clearStatement(stmt);
         }
      }
   }

   /**
    * Commit
    * 
    * @return false, wenn Exception auftrat
    */
   public boolean commit() {
      try {
         logSQL("COMMIT");
         connection.commit();
         return true;
      } catch (SQLException e) {
         logException(e);
         return false;
      }
   }

   /**
    * Rollback
    * 
    * @return false, wenn Exception auftrat
    */
   public boolean rollback() {
      try {
         logSQL("ROLLBACK");
         connection.rollback();
         return true;
      } catch (SQLException e) {
         logException(e);
         return false;
      }
   }


  /**
   * @deprecated diese methode soll nur ermöglichen, dass closeconnection nicht immer markConnection aufruft.
   * @param markConnection bestimmt ob markConnection aufgerufen wird
   * @return
   */
  protected boolean closeConnection(boolean markConnection) {
    if (connection != null) {
      try {
        if( name != null && ! name.equals("unknown") ) {
          logSQL("CLOSE "+name);
        } else {
          logSQL("CLOSE");
        }
        if (statementCache != null) {
          statementCache.close();
        }
        ConnectionFactory.closeConnection(connection, markConnection); //clientinfo entfernen
        return true;
      } catch (DBUtilsException e) {
        logException((SQLException) (new SQLException("Connection couldn't be closed.").initCause(e)));
        return false;
      }
    }
    return false;
  }


  /**
   * Schließen der Connection
   * @return false, wenn Exception auftrat oder Connection null ist
   */
  public boolean closeConnection() {
    return closeConnection(true);
  }

   /**
    * Loggen einer SQLException
    * 
    * @param e
    */
   public void logException(SQLException e) {
      lastException = e;
      if (logger != null && logException) {
         logger.logException(e);
      }
   }

  protected void logSQL(String sql) {
    if (logger != null) {
      logger.logSQL(sql);
    }
  }


  /**
   * Loggen des SQL-Strings
   * @param sql
   */
  protected void logSQL(String sql, Parameter params) {
    if (logger != null) {
      logger.logSQL(sql + " " + params);
    }
  }

   /**
   * 
   */
   public void logLastException() {
      if (logger != null) {
         logger.logException(lastException);
      }
   }

   /**
    * Kapselung für ein Select, welches einen einzigen Integer zurückliefert
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return Integer, null bei Lesefehler
    */
   public Integer queryInt(String sql, Parameter params) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         rs = stmt.executeQuery();
         logSQL(sql, params);
         if (rs.next()) {
            return Integer.valueOf(rs.getInt(1));
         }
      } catch (SQLException e) {
         logException(e);
      } finally {
         finallyClose(rs, stmt);
      }
      return null;
   }

   /**
    * Kapselung für ein Select, welches eine einzige Zeile zurückliefert
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return Rückgabe des resultSetReader, null bei Lesefehler
    */
   public <T> T queryOneRow(String sql, Parameter params,
         ResultSetReader<T> resultSetReader) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         rs = stmt.executeQuery();
         return queryOneRow(rs, resultSetReader);
      } catch (SQLException e) {
         logException(e);
         return null;
      } finally {
         finallyClose(rs, stmt);
      }
   }

   /**
    * Kapselung für ein Select, welches eine einzige Zeile zurückliefert
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return Boolean, null oder false bei Lesefehler
    */
   public Boolean queryOneRow(String sql, Parameter params,
         ResultSetReaderFunction resultSetReaderFunction) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         rs = stmt.executeQuery();
         return queryOneRow(rs, resultSetReaderFunction);
      } catch (SQLException e) {
         logException(e);
         return null;
      } finally {
         finallyClose(rs, stmt);
      }
   }

   /**
    * Kapselung für ein Select, welches mehrere Zeilen zurückliefert
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return ArrayList mit Rückgaben des resultSetReader, null bei Lesefehler
    */
   public <T> ArrayList<T> query(String sql, Parameter params,
         ResultSetReader<T> resultSetReader) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         rs = stmt.executeQuery();
         return query(rs, resultSetReader);
      } catch (SQLException e) {
         logException(e);
         return null;
      } finally {
         finallyClose(rs, stmt);
      }
   }
   

   private void clearStatement(PreparedStatement stmt) {
     try {
       stmt.clearParameters();
       boolean moreResults = stmt.getMoreResults();
       while (moreResults) {
         moreResults = stmt.getMoreResults();
       }
     } catch (SQLException e) {
       logException(e);
     } catch (NullPointerException ne) {
       logException(ne);
     }
   }

   /**
    * Kapselung für ein Select, welches mehrere Zeilen zurückliefert, für jede
    * Zeile wird die ResultSetReaderFunction aufgerufen
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return Anzahl der gelesenen Zeile, null bei Lesefehler
    */
   public Integer query(String sql, Parameter params,
         ResultSetReaderFunction resultSetReader) {
      PreparedStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         rs = stmt.executeQuery();
         return query(rs, resultSetReader);
      } catch (SQLException e) {
         logException(e);
         return null;
      } finally {
         finallyClose(rs, stmt);
      }
   }

   /**
    * Kapselung für ein DML-Statement (Insert, Update, Delete)
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return Anzahl der betroffenen Zeilen, -1 bei Fehler, -2 bei
    *         UniqueConstraintViolation
    */
   public int executeDML(String sql, Parameter params ) {
      PreparedStatement stmt = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         return stmt.executeUpdate();
      } catch (SQLException e) {
         logException(e);
         if (e.getErrorCode() == 1) {
            return -2; // ORA-00001: unique constraint (...) violated
         }
         return -1;
      } finally {
         finallyClose(null, stmt);
      }
   }
   

  /**
   * kapselt DML-Statements, die mehrfach mit unterschiedlichen
   * Parametern ausgefuehrt werden sollen. 
   * @return Falls ein Fehler aufgetreten ist, wird ein Array der Groesse 
   * 1 zurueckgegeben, welches den Fehlercode -2 (für UniqueConstraintViolation), -1 bei sonstigen Fehlern
   * enthaelt. Ohne Fehler wird ein Array der Groesse entsprechend der Anzahl der uebergebenen
   * Parameter zurueckgegeben, wobei jeder Eintrag das Ergebnis der entsprechenden
   * Operation enthält (Anzahl der modifizierten Zeilen).
   */
  public int[] executeDMLBatch(String sql, Parameter... params) {
    PreparedStatement stmt = null;
    lastException = null;
    try {
      stmt = prepareStatement(sql);
      for (Parameter param : params) {
        addParameter(stmt, param);
        stmt.addBatch();
        logSQL(sql, param);
      }
      return stmt.executeBatch();
    } catch (SQLException e) {
      logException(e);
      if (e.getErrorCode() == 1) {
        return new int[] {-2}; // ORA-00001: unique constraint (...) violated
      }
      return new int[] {-1};
    } finally {
      finallyClose(null, stmt);
    }
  }


   /**
    * Kapselung für ein DDL-Statement
    * 
    * @param sql
    * @param params
    *           Parameter, die dem PreparedStatement übergeben werden sollen
    * @return true bei erfolgreicher Ausführung
    */
   public boolean executeDDL(String sql, Parameter params) {
      PreparedStatement stmt = null;
      lastException = null;
      try {
         stmt = prepareStatement(sql);
         addParameter(stmt, params);
         logSQL(sql, params);
         return stmt.executeUpdate() == 0;
      } catch (SQLException e) {
         logException(e);
         return false;
      } finally {
         finallyClose(null, stmt);
      }
   }

   /**
    * Kapselung für einen Aufruf einer Stored-Prozedure/Function
    * 
    * @param sql
    * @param params
    *           Array der dem PreparedStatement zu übergebenden Parameter
    * @return true bei erfolgreicher Ausführung (keine Exception)
    */
   public boolean executeCall(String sql, Parameter params) {
      if( ! sql.trim().startsWith("{") ) {
        sql = "{"+sql+"}";
        logSQL("executeCall: Call-String muss in {}-Klammern eingeschlossen werden. Bitte fixen!");
      }
      return internalCall(sql,params);
   }
   
  /**
   * Kapselung für einen Aufruf eines Code-Blocks
   * @param sql
   * @param params
   * @return
   */
  public boolean executeBlock(String sql, Parameter params) {
     return internalCall(sql,params);
   }
  

   private boolean internalCall(String sql, Parameter params) {      
      CallableStatement stmt = null;
      ResultSet rs = null;
      lastException = null;
      try {
         stmt = prepareCall(sql);
         if (params != null) {
            for (int i = 1; i <= params.size(); ++i) {
               if (params.isOutputParam(i)) {
                 registerOutputParameter(stmt, i, (OutputParam<?>) params.getParameter(i) );
               } else {
                 params.addParameterTo(stmt, i, params.getParameter(i));
               }
            }
         }
         logSQL(sql, params);
         stmt.execute();
         setOutput( params, stmt);
         
         return true;
      } catch (SQLException e) {
         logException(e);
         return false;
      } finally {
         finallyClose(rs, stmt);
      }
   }

   private void setOutput(Parameter params, CallableStatement stmt) throws SQLException {
     if (params == null) {
       return;
     }
     for (int i = 1; i <= params.size(); ++i) {
       if (params.isOutputParam(i)) {
         OutputParam<?> output = (OutputParam<?>) params.getParameter(i);
         output.set(stmt.getObject(i));
       }
     }
   }

  private void registerOutputParameter(CallableStatement stmt, int i, OutputParam<?> output) throws SQLException {
     stmt.registerOutParameter(i, output.getSQLType());
     if (output.get() != null) { // für IN-OUT-Parameter
       stmt.setObject(i, output.get());
     }
   }
   

  /**
    * Lesen eines CLOBs aus dem ResultSet
    * 
    * @param rs
    * @param i
    * @return
    * @throws SQLException
    * @deprecated Use ResultSetUtils.getClob(rs,i) instead
    */
   public String getClob(ResultSet rs, int i) throws SQLException {
      Clob clob = rs.getClob(i);
      return clob.getSubString(1, (int) clob.length());
   }

   /**
    * @param logger
    */
   public void setLogger(SQLUtilsLogger logger) {
      this.logger = logger;
   }
   public SQLUtilsLogger getLogger() {
     return this.logger;
   }

   
   /**
    * Rückgabe der zuletzt aufgetretenen SQLException, null falls keine
    * Exception auftrat
    * 
    * @return
    */
   public SQLException getLastException() {
      return lastException;
   }
   
   /**
    * Setzen der LastException in Sonderfällen: Exception ist außerhalb der SQLUtils 
    * aufgetreten, soll aber intern sein, z.B. für LogLastException 
    * @param exception
    */
   public void setLastException(SQLException exception) {
     lastException = exception; 
   }

   public Connection getConnection() {
      return connection;
   }

   public void setConnection(Connection connection) {
      this.connection = connection;
   }

   public boolean isLogException() {
      return logException;
   }

   public void setLogException(boolean logException) {
      this.logException = logException;
   }

   public StatementCache getStatementCache() {
      return statementCache;
   }

   public void setStatementCache(StatementCache statementCache) {
      this.statementCache = statementCache;
   } 
   
   /**
    * Sollen bei einem Query Null-Elemente in der Liste zurückgegeben werden, falls der ResultSetReader Null-Elemente erzeugt?
    */
   public void setIncludeResultSetReaderNullElements(boolean includeResultSetReaderNullElements) {
     this.includeResultSetReaderNullElements = includeResultSetReaderNullElements;
   }

   public List<Column> desc(String table) {
     String sql = "SELECT * FROM "+table;//+" WHERE 1=0";
     PreparedStatement stmt = null;
     ResultSet rs = null;
     lastException = null;
     try {
       stmt = prepareStatement(sql);
       logSQL(sql);
       rs = stmt.executeQuery();
       ResultSetMetaData rmd = rs.getMetaData();
       List<Column> cols = new ArrayList<Column>();
       for( int c=1; c<=rmd.getColumnCount(); ++c ) {
         cols.add(new Column(rmd, c));
       }
       return cols;
     } catch (SQLException e) {
       logException(e);
       return null;
     } finally {
       finallyClose(rs, stmt);
     }
  }
  
   public static class Column {
     private String name;
     private String type;
     private int index;

     public Column(String name, String type, int index) {
       this.index = index;
       this.name = name;
       this.type = type;
     }

     public Column(ResultSetMetaData rmd, int index) throws SQLException {
       this.index = index;
       this.name = rmd.getColumnName(index);
       this.type = rmd.getColumnTypeName(index);
     }

     @Override
     public String toString() {
       return name+":"+type;
     }

     public int getIndex() {
       return index;
     }
   }

}
