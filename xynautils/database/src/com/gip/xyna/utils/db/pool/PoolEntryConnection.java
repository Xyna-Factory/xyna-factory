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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation;
import com.gip.xyna.utils.db.ConnectionPool.PooledConnection;
import com.gip.xyna.utils.db.utils.SQLInstrumentedWrappedConnection;

public class PoolEntryConnection extends SQLInstrumentedWrappedConnection {

  
  private static final Logger logger = Logger.getLogger(PoolEntryConnection.class);

  private ConnectionPool pool;
  private boolean closed = true;
  private volatile long lastCheck;
  private volatile boolean lastCheckOk;
  private volatile long lastInitialize; //wann wurde connection herausgegeben
  private volatile long lastCommit;
  private volatile long lastRollback;
  private volatile long cntUsed; //wie oft wurde connection geholt
  private Thread currentThread;
  private StackTraceElement[] stackTraceWhenThreadGotConnection;
  private long configurationCount;
  private volatile boolean closeInnerConnection = false; //flag, ob beim close() auch die innere connection geschlossen wird
  //zuletzt durchgeführtes sql. wird genullt, wenn verbindung neu aufgemacht wird, d.h. ein neuer thread aktiv wird, weil
  //es sonst verwirrend wird, ob der aktive thread zu dem sql gehört.
  private volatile String lastSQL; 
  

  public PoolEntryConnection(Connection con, ConnectionPool pool) {
    super(con);
    this.pool = pool;
    configurationCount = pool.getConfigurationCount();
    lastCheck = System.currentTimeMillis();
    lastCheckOk = true; //zu beginn true, weil die connection gerade neu aufgemacht wurde
  }
  
  @Override
  public String toString() {
    return "PoolEntryConnection("+pool.getId()+","+con+")";
  }
  
  public long getLastCheck() {
    return lastCheck;
  }
  public void setLastCheck(long lastCheck) {
    this.lastCheck = lastCheck;
  }
  public void setLastCheckResult(boolean result) {
    this.lastCheckOk = result;
  }
  
  public PooledConnection initializeForUse(Thread currentThread, long currentTime, String clientInfo) {
    cntUsed++;
    lastInitialize = currentTime;
    this.currentThread = currentThread;
    this.stackTraceWhenThreadGotConnection = currentThread.getStackTrace();
    closed = false;
    lastSQL = null;
    pool.getConnectionBuildStrategy().markConnection(con, clientInfo);
    return new PooledConnection(this);
  }
  
  public void rebuild(Connection newCon ) {
    if( con != null ) {
      try {
        con.close();
      } catch (SQLException e) {
        logger.trace("old connection could not be closed successfully", e);
      }
      con = null;
    }
    lastCheckOk = true; //zu beginn true, weil die connection gerade neu aufgemacht wurde
    this.con = newCon;
    //es folgt Aufruf initializeForUse(...) 
  }
  
  public Connection getInnerConnection() {
    return con;
  }
  
  private void checkThreadUsage(String usage) {
    if( currentThread != null && ! Thread.currentThread().equals(currentThread) ) {
      logger.warn("Connection from pool "+pool.getId()+" was opened from thread "+currentThread+" but ist used for \""+usage+"\" from thread "+Thread.currentThread() );
    }
  }

  /**
   * gibt die Connection zurück an den Pool
   */
  public void close() throws SQLException {
    boolean innerConnectionIsOpen = false;
    try {
      // synchronized, weil das close auch beim {@link ConnectionPool.#recreateAllConnections(boolean)} aufgerufen wird
      // das synchronized darf nicht das "returnConnection(this)" enthalten, wegen deadlockgefahr
      synchronized (this) {
        if( pool.isCheckThreadUsage() ) {
          checkThreadUsage("close");
        }
        if (closed) {
          //diese methode wurde bereits durchlaufen
          return;
        }
        try {
          innerConnectionIsOpen = con != null && !con.isClosed();
          if (innerConnectionIsOpen) {
            con.rollback();
            String lastSql_t = lastSQL; //nicht überschreiben durch markConnection
            pool.getConnectionBuildStrategy().markConnectionNotInUse(con);
            lastSQL = lastSql_t;
            lastCheck = System.currentTimeMillis();
            lastCheckOk = true;
          }
        } finally {
          closed = true;
        }
      }
    } finally {
      this.currentThread = null;
      this.stackTraceWhenThreadGotConnection = null;
      pool.returnConnection(this); //stellt die connection wieder zur verfügung
      if (closeInnerConnection && innerConnectionIsOpen) {
        con.close();
      }
    }
  }
  
  public void reset() {
    long currentTime = System.currentTimeMillis();
    lastCheck = currentTime;
    lastCheckOk = true;
    lastInitialize = 0; //wann wurde connection herausgegeben
    lastCommit = 0;
    lastRollback = 0;
    cntUsed = 0; //wie oft wurde connection geholt
    currentThread = null;
    closeInnerConnection = false;
    lastSQL = "";
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  protected void lastSQL(String sql) throws SQLException {
    lastSQL = sql;
    if (pool.isCollectStatistics() ) {
      pool.storeSQL(sql);
    }
  }

  @Override
  protected void lastCommitOrRollback(boolean commit) throws SQLException {
    if( pool.isCheckThreadUsage() ) {
      checkThreadUsage(commit ? "commit" : "rollback");
    }
    if (pool.isCollectStatistics()) {
      long now = System.currentTimeMillis();
      if( commit ) {
        lastCommit = now;
      } else {
        lastRollback = now;
      }
    }
  }

  public void commit() throws SQLException {
    boolean localOK = false;
    try {
      super.commit();
      localOK = true; //bleibt im fehlerfall auf false
    } finally {
      lastCheckOk = localOK; //soll erst hier nach aussen sichtbar werden 
    }
  }

  public void rollback() throws SQLException {
    boolean localOK = false;
    try {
      super.rollback();
      localOK = true; //bleibt im fehlerfall auf false
    } finally {
      lastCheckOk = localOK; //soll erst hier nach aussen sichtbar werden 
    }
  }

  public ConnectionInformation getConnectionInformation() {
    return new ConnectionInformation(lastCheck, !closed, lastInitialize, lastCommit, lastRollback, 
        cntUsed, lastCheckOk, lastSQL, currentThread, stackTraceWhenThreadGotConnection);
  }

  public void closeInner() throws SQLException {
    closeInnerConnection = true;
    try {
      con.close();
    } catch (SQLException e) {
      logger.debug("Failed to close connection on modification",e);
    }
  }

  public long getConfigurationCount() {
    return configurationCount;
  }

  public ConnectionPool getPool() {
    return pool;
  }

  /**
   * Markiert die Connection, so dass sie bei nächster Gelegenheit geschlossen wird
   */
  public void markToClose() {
    closeInnerConnection = true;
  }

  public boolean markedAsClosed() {
    return closeInnerConnection;
  }

  public boolean getLastCheckOk() {
    return lastCheckOk;
  }


}
