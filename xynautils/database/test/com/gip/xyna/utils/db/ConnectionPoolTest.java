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



import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.ConnectionPool.ConnectionCouldNotBeClosedException;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.db.ConnectionPool.PooledConnection;
import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;

import junit.framework.TestCase;



public class ConnectionPoolTest extends TestCase {
  
  private static final Logger logger = Logger.getLogger(ConnectionPoolTest.class.getName());
  
  private static volatile boolean dbConnectionOkDefault = true;
  private static volatile boolean throwRuntimeExceptionWhenDbDown = false;
  private static final int connectionPoolSize = 170;

  private ConnectionPool cp;
  private final Random random = new Random();


  public void setUp() {
    cp = ConnectionPool.getInstance(new IConnectionFactory() {

      public Connection createNewConnection() {
        if (throwRuntimeExceptionWhenDbDown) {
          throw new RuntimeException("db down");
        }
        return new TestConnection();
      }


      public void markConnection(Connection con, String clientInfo) {

      }

    }, "testid", connectionPoolSize);
  }
  
  public void tearDown() throws Exception {
    ConnectionPool.removePool(cp, true, 1000);
  }


  public void testTimeoutOfWaitingThreads() throws Exception {
    final AtomicInteger timedoutCnt = new AtomicInteger(0);
    final int waitingThreads = 400;
    final CountDownLatch latch = new CountDownLatch(connectionPoolSize + waitingThreads);
    for (int i = 0; i < connectionPoolSize; i++) {
      Thread t = new Thread() {

        public void run() {
          try {
            Thread.sleep(random.nextInt(2000) + 50);
          } catch (InterruptedException e1) {
          }
          try {
            Connection con = cp.getConnection(1000, "");
            try {
              Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            try {
              con.close();
            } catch (SQLException e) {
              fail("could not close connection");
            }
          } catch (NoConnectionAvailableException e) {
            fail("could not get connection");
          } finally {
            latch.countDown();
          }
        }
      };
      t.start();
    }
    for (int i = 0; i < waitingThreads; i++) {
      Thread t = new Thread() {

        public void run() {
          try {
            Thread.sleep(random.nextInt(2000) + 2400);
          } catch (InterruptedException e1) {
          }
          long t1 = System.currentTimeMillis();
          try {
            cp.getConnection(1000, "");
            fail("got connection");
          } catch (NoConnectionAvailableException e) {
            long timedoutAfter = System.currentTimeMillis() - t1;
            if (Math.abs(timedoutAfter - 1000) > 500) { //mehr als 50 % abweichung
              fail("timeout took too long: " + timedoutAfter);
            }
            timedoutCnt.incrementAndGet();
          } finally {
            latch.countDown();
          }
        }
      };
      t.start();
    }
    latch.await();
    assertEquals(waitingThreads, timedoutCnt.get());
  }


  public void testGetDifferentConnectionsFromPool() throws Exception {
    final HashSet<Connection> connections = new HashSet<Connection>();
    final CountDownLatch latch = new CountDownLatch(connectionPoolSize);
    for (int i = 0; i < connectionPoolSize; i++) {
      Thread t = new Thread() {

        public void run() {
          try {
            Thread.sleep(random.nextInt(2000) + 50);
          } catch (InterruptedException e1) {
          }
          try {
            Connection con = cp.getConnection(1000, "");
            synchronized (connections) {
              connections.add(con);
            }
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            try {
              con.close();
            } catch (SQLException e) {
              fail("could not close connection");
            }
          } catch (NoConnectionAvailableException e) {
            fail("could not get connection");
          } finally {
            latch.countDown();
          }
        }
      };
      t.start();
    }
    latch.await();
    assertEquals(connectionPoolSize, connections.size());
  }


  public void testGetConnectionBeforeTimeout() throws Exception {
    //threads warten zufällige zeit, neue threads haben timeout > zufällige zeit und bekommen deshalb connections.
    //andere threads haben timeout < zufällige zeit und bekommen deshalb keine connection
    final long[] waitTimesFirstThreads = new long[connectionPoolSize];
    final boolean[] timeouts = new boolean[connectionPoolSize];
    int expectedGotConnection = 0;
    for (int i = 0; i < waitTimesFirstThreads.length; i++) {
      waitTimesFirstThreads[i] = random.nextInt(500) + 2000;
      timeouts[i] = random.nextBoolean();
      if (!timeouts[i]) {
        expectedGotConnection++;
      }
    }
    final CountDownLatch latch1 = new CountDownLatch(connectionPoolSize);
    final CountDownLatch latch2 = new CountDownLatch(2 * connectionPoolSize);
    final AtomicInteger cntGotConnection = new AtomicInteger(0);
    for (int i = 0; i < connectionPoolSize; i++) {
      final int j = i;
      Thread t = new Thread() {

        public void run() {
          try {
            Connection con = cp.getConnection(1000, "");
            latch1.countDown();
            try {
              Thread.sleep(waitTimesFirstThreads[j]);
            } catch (InterruptedException e) {
            }
            try {
              con.close();
            } catch (SQLException e) {
              fail("could not close connection");
            }
          } catch (NoConnectionAvailableException e) {
            fail("could not get connection");
          } finally {
            latch2.countDown();
          }
        }
      };
      t.start();
    }
    latch1.await();
    for (int i = 0; i < connectionPoolSize; i++) {
      final int j = i;
      Thread t = new Thread() {

        public void run() {
          try {
            long timeout = 0;
            if (timeouts[j]) {
              timeout = 1500;
            } else {
              timeout = 3000;
            }
            Connection con = cp.getConnection(timeout, "");
            cntGotConnection.incrementAndGet();
            try {
              Thread.sleep(4000);
            } catch (InterruptedException e1) {
            }
            try {
              con.close();
            } catch (SQLException e) {
              fail("could not close connection");
            }
          } catch (NoConnectionAvailableException e) {
          } finally {
            latch2.countDown();
          }
        }
      };
      t.start();
    }
    latch2.await();
    assertEquals(expectedGotConnection, cntGotConnection.get());
  }
  
  public void testCloseDownClosesAllConnections() throws Exception {
    final CountDownLatch latch1 = new CountDownLatch(connectionPoolSize + 100);
    final HashSet<Connection> connections = new HashSet<Connection>();
    for (int i = 0; i < connectionPoolSize + 100; i++) {
      Thread t = new Thread() {

        public void run() {
          try {
            latch1.countDown();
            Connection con = cp.getConnection(100000, "");
            synchronized (connections) {
              connections.add(con);
            }
            try {
              Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
            try {
              con.close();
            } catch (SQLException e) {
              fail("could not close connection");
            }
          } catch (NoConnectionAvailableException e) {
          }
        }
      };
      t.start();
    }
    latch1.await();
    Thread.sleep(100);
    try {
      ConnectionPool.closedownPool(cp, true, 1000);
    } catch (ConnectionCouldNotBeClosedException e) {
      fail("could not close connection");
    }
    synchronized (connections) {
      for (Connection con : connections) {
        assertEquals(true, ((ConnectionPool.PooledConnection)con).getInnerConnection().isClosed());
      }
    }
  }
  
  public void testTakenConnectionIsTaken() throws Exception {
    final Connection con = cp.getConnection(1, "");
    final CountDownLatch latch = new CountDownLatch(connectionPoolSize + 100);
    final HashSet<Connection> connections = new HashSet<Connection>();
    for (int i = 0; i < connectionPoolSize + 100; i++) {
      Thread t = new Thread() {

        public void run() {
          try {
            for (int i = 0; i < 100; i++) {
              try {
                Connection con = cp.getConnection(100000, "");
                synchronized (connections) {
                  connections.add(((PooledConnection)con).getInnerConnection());
                }
                try {
                  Thread.sleep(random.nextInt(500) + 200);
                } catch (InterruptedException e) {
                }
                try {
                  con.close();
                } catch (SQLException e) {
                  fail("could not close connection");
                }
              } catch (NoConnectionAvailableException e) {
                fail("could not get connection");
              }
            }
          } finally {
            latch.countDown();
          }
        }
      };
      t.start();
    }
    latch.await();
    assertEquals(false, connections.contains(((PooledConnection)con).getInnerConnection()));
    assertEquals(connectionPoolSize -1, connections.size());
  }

  
  public void testOrderOfConnectionsForThreads() throws Exception {
    //wer als erstes eine connection will, soll sie auch als erstes bekommen!
    
    //erstmal alle connections vergeben. dann mehrere threads auf connection warten lassen. dann wird eine connection frei.
    //dann überprüfen, dass der richtige thread sie bekommt.
    Connection con = cp.getConnection(100, "");
    final CountDownLatch latch = new CountDownLatch(connectionPoolSize-1);
    for (int i = 0; i<connectionPoolSize-1; i++) {
      Thread t = new Thread() {
        public void run() {
          try {
            cp.getConnection(1000, "");
          } catch (NoConnectionAvailableException e) {
            fail("could not get connection");
          } finally {
            latch.countDown();
          }
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
          }          
        }
      };
      t.start();
    }
    latch.await();
    final CountDownLatch latch2 = new CountDownLatch(100);
    final List<String> threadOrder = new ArrayList<String>();
    for (int i = 0; i<100; i++) {
      final int j = i;
      Thread t = new Thread("test" + i) {
        
        public void run() {
          try {
            Thread.sleep(j * 100);
          } catch (InterruptedException e1) {
          }
          Connection con = null;
          try {
            con = cp.getConnection(100000, "");
            synchronized (threadOrder) {
              threadOrder.add(getName());
            }
          } catch (NoConnectionAvailableException e) {
            fail("could not get connection");
          } finally {
            latch2.countDown();
          }
          try {
            con.close();
          } catch (SQLException e1) {
          }
        }
      };
      t.start();
    }
    con.close();
    latch2.await();
    assertEquals(100, threadOrder.size());
    for (int i = 0; i<threadOrder.size(); i++) {
      assertEquals("test" + i, threadOrder.get(i));
    }
  }
  
  
  public void testAutoRepairOfBrokenConnectionsIfDBIsOk() throws Exception {
    HashSet<Connection> connections = new HashSet<Connection>();
    HashSet<Connection> connectionsBefore = new HashSet<Connection>();
    int numberBroken = 0;
    
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(100, "");
      connections.add(((PooledConnection)con).getInnerConnection());
      connectionsBefore.add(con);
    }
    //close and break some
    for (Connection con : connectionsBefore) {
      TestConnection tc = (TestConnection)((PooledConnection)con).getInnerConnection();
      con.close();
      if (random.nextBoolean()) {
        numberBroken ++;
        tc.setDbConnectionOk(false);
      }
    }
    
    cp.setCheckInterval(0);
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(100, "");
      connections.add(((PooledConnection)con).getInnerConnection());
    }

    assertEquals(connectionPoolSize + numberBroken, connections.size());
  }
  
  public void testErrorBehaviourWhenDBisDownOrNotReachable() throws Exception {
    HashSet<Connection> connections = new HashSet<Connection>();
    HashSet<Connection> connectionsBefore = new HashSet<Connection>();
    int numberBroken = 0;
    
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(100, "");
      connections.add(((PooledConnection)con).getInnerConnection());
      connectionsBefore.add(con);
    }
    
    //close and break some
    for (Connection con : connectionsBefore) {
      TestConnection tc = (TestConnection)((PooledConnection)con).getInnerConnection();
      con.close();
      if (random.nextBoolean()) {
        numberBroken ++;
        tc.setDbConnectionOk(false);
      }
      
    }
    
    cp.setCheckInterval(0);
    dbConnectionOkDefault = false; //neue connections sind fehlerhaft
    int cntNoConnectionAvailable = 0;
    try {
      for (int i = 0; i<connectionPoolSize; i++) {
        try {
          Connection con = cp.getConnection(100, "");
          connections.add(((PooledConnection)con).getInnerConnection());
        } catch (NoConnectionAvailableException e) {
          cntNoConnectionAvailable ++;
        }
      }

      assertEquals(numberBroken, cntNoConnectionAvailable);
          
    } finally {
      dbConnectionOkDefault = true;   
    }

    //jetzt is db wieder verfügbar
    int cntNoConnectionAvailable2 = 0;
    for (int i = 0; i<cntNoConnectionAvailable; i++) {
      try {
        Connection con = cp.getConnection(100000, "");
        connections.add(((PooledConnection)con).getInnerConnection());
      } catch (NoConnectionAvailableException e) {
        cntNoConnectionAvailable2 ++;
      }
    }
    assertEquals(0, cntNoConnectionAvailable2);
    assertEquals(connectionPoolSize + numberBroken, connections.size());
  }
  
  public void testGetNewConnectionThrowsRuntimeExceptionWhenDbDown() throws Exception {
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(100, "");
      TestConnection tc = (TestConnection)((PooledConnection)con).getInnerConnection();
      con.close();
      tc.setDbConnectionOk(false);
    }
    
    
    boolean gotException = false;
    cp.setCheckInterval(0);
    dbConnectionOkDefault = false; //neue connections sind fehlerhaft
    throwRuntimeExceptionWhenDbDown = true;
    try {      
      cp.getConnection(100, "");
    } catch (NoConnectionAvailableException e) {
      logger.error("", e);
      gotException = true;
    } finally {
      dbConnectionOkDefault = true;
      throwRuntimeExceptionWhenDbDown = false;
    }
    
    assertEquals(true, gotException);      
  }  
  
  public void testStatistics() throws Exception {
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(connectionPoolSize, connectionPoolSize, 1, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    final CountDownLatch latch = new CountDownLatch(100 * connectionPoolSize);
    final AtomicInteger cntNotSleepingThreads = new AtomicInteger(0);
    final AtomicInteger cntSleepingThreads = new AtomicInteger(0);
    Runnable r = new Runnable() {

      public void run() {
        Connection con = null;
        try {
          con = cp.getConnection(100000, "");
        } catch (NoConnectionAvailableException e) {
          fail("could not get connection");
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        try {
          con.prepareCall("abc");
          con.commit();
          con.close();
        } catch (SQLException e1) {
        }
        latch.countDown();
      }
    };
    //CounterMap<String> cm = new CounterMap<String>();
    for (int i = 0; i < 100 * connectionPoolSize; i++) {
      while (true) {
        ConnectionInformation[] conInfo = cp.getConnectionStatistics();
        for (ConnectionInformation ci : conInfo) {
          ThreadInformation thread = ci.getCurrentThread();
          if (thread != null) {
            StackTraceElement[] st = thread.getStackTrace();
            if (!"sleep".equals(st[0].getMethodName())) {
              cntNotSleepingThreads.incrementAndGet();
            } else {
              cntSleepingThreads.incrementAndGet();
            }
            //cm.increment(st[0].getMethodName());
          }
        }
        try {
          tpe.execute(r);
          break;
        } catch (RejectedExecutionException e) {
        }
      }
    }
    latch.await();
    //System.out.println( cm.entryListSortedByCount(true) );
    //[sleep=169642, park=431, getObjectVolatile=99, currentTimeMillis=81, currentThread=61, compareAndSwapObject=12, returnConnection=11, isTraceEnabled=8, getTask=8, compareAndSwapInt=6, awaitFulfill=5, take=5, unlock=4, close=3, transfer=3, hash=3, <init>=2, get=2, releaseShared=2, tryReleaseShared=2, snode=2, countDown=1, isDebugEnabled=1, debug=1, valueOf=1, linkLast=1, isGreaterOrEqual=1, <clinit>=1, release=1, nonfairTryAcquire=1, runWorker=1]

    logger.warn("not sleeping threads: " + cntNotSleepingThreads.get());
    double sleepRatio = 1.*cntNotSleepingThreads.get()/cntSleepingThreads.get();
    System.out.println( "most threads should have been sleeping "+ cntNotSleepingThreads.get()+ "/" +cntSleepingThreads.get()+" = "+ sleepRatio);
    if (sleepRatio > 0.01) {
      fail("most threads should have been sleeping "+ cntNotSleepingThreads.get()+ "/" +cntSleepingThreads.get()+" > 0.01" );
    }
    ConnectionInformation[] statistics = cp.getConnectionStatistics();
    assertEquals(connectionPoolSize, statistics.length);
    int cntUsed = 0;
    for (ConnectionInformation ci : statistics) {
      if (ci.getAquiredLast() - System.currentTimeMillis() > 1000) {
        fail("connection wurde zu lange nicht genutzt");        
      }
      cntUsed += ci.getCntUsed();
      if (ci.getLastCheck() - System.currentTimeMillis() > 1000) {
        fail("connection wurde zu lange nicht genutzt");        
      }
      if (ci.getLastCommit() - System.currentTimeMillis() > 1000) {
        fail("connection wurde zu lange nicht genutzt");        
      }
      //wird beim close gemacht
      if (ci.getLastRollback() - System.currentTimeMillis() > 1000) {
        fail("connection wurde zu lange nicht genutzt");        
      }
      assertEquals("abc", ci.getLastSQL());
      assertNull(ci.getCurrentThread());
    }
    assertEquals(100 * connectionPoolSize, cntUsed);
  }
  
  public void testRefreshConnectionPoolForceClose() throws Exception {
    HashSet<Connection> connections = new HashSet<Connection>();
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(1000, "");
      TestConnection tc = (TestConnection)((PooledConnection)con).getInnerConnection();
      connections.add(tc);
      if (random.nextBoolean()) {
        con.close();
      }
    }
    cp.recreateAllConnections(true);
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(1000, "");
      TestConnection tc = (TestConnection)((PooledConnection)con).getInnerConnection();
      connections.add(tc);
    }
    assertEquals(connectionPoolSize * 2, connections.size());
    
    int cntClosedInnerConnections = 0;
    for (Connection con : connections) {
      if ( ((TestConnection)con).closed) {
        cntClosedInnerConnections ++;
      }
      con.close();
    }
    assertEquals(connectionPoolSize, cntClosedInnerConnections);
  }
  
  public void testRefreshConnectionPoolNoForceClose() throws Exception {
    HashSet<Connection> connections = new HashSet<Connection>();
    HashSet<Connection> connectionsActive = new HashSet<Connection>();
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(1000, "");
      connections.add(con);
      if (random.nextBoolean()) {
        con.close();
      } else {
        connectionsActive.add(con);
      }
    }
    cp.recreateAllConnections(false);
    
    //testen, dass genau poolSize neue connections zur verfügung stehen
    for (int i = 0; i<connectionPoolSize; i++) {
      Connection con = cp.getConnection(1000, "");
      connections.add(con);
      con.prepareStatement("abc");
    }
    //keine weitere
    NoConnectionAvailableException ex = null;
    try {
      cp.getConnection(100, "");
    } catch (NoConnectionAvailableException e) {
      ex = e; 
    }
    assertNotNull(ex);
    assertEquals(connectionPoolSize * 2, connections.size());

    
    //testen, dass connections die in benutzung sind noch funktionieren
    for (Connection con : connectionsActive) {
      con.prepareStatement("cde");
      con.commit();
      con.close();
    }
  }
  
  /*
  public void testThreadWantsSeveralConnections() {
    
  }


*/
  
  /*
   * #######################################################################################
   * jdbc klassen für die tests 
   */
  
  private static class TestRS extends UnsupportingResultSet {

    public boolean absolute(int row) throws SQLException {
      return false;
    }

    public void afterLast() throws SQLException {
    }

    public void beforeFirst() throws SQLException {
    }

    public void cancelRowUpdates() throws SQLException {
    }

    public void clearWarnings() throws SQLException {
    }

    public void close() throws SQLException {
    }

    public void deleteRow() throws SQLException {
    }

    public int findColumn(String columnName) throws SQLException {
      return 0;
    }

    public boolean first() throws SQLException {
      return false;
    }

    public Array getArray(int i) throws SQLException {
      return null;
    }

    public Array getArray(String colName) throws SQLException {
      return null;
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
      return null;
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
      return null;
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
      return null;
    }

    public BigDecimal getBigDecimal(String columnName) throws SQLException {
      return null;
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
      return null;
    }

    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
      return null;
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
      return null;
    }

    public InputStream getBinaryStream(String columnName) throws SQLException {
      return null;
    }

    public Blob getBlob(int i) throws SQLException {
      return null;
    }

    public Blob getBlob(String colName) throws SQLException {
      return null;
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
      return false;
    }

    public boolean getBoolean(String columnName) throws SQLException {
      return false;
    }

    public byte getByte(int columnIndex) throws SQLException {
      return 0;
    }

    public byte getByte(String columnName) throws SQLException {
      return 0;
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
      return null;
    }

    public byte[] getBytes(String columnName) throws SQLException {
      return null;
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
      return null;
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
      return null;
    }

    public Clob getClob(int i) throws SQLException {
      return null;
    }

    public Clob getClob(String colName) throws SQLException {
      return null;
    }

    public int getConcurrency() throws SQLException {
      return 0;
    }

    public String getCursorName() throws SQLException {
      return null;
    }

    public Date getDate(int columnIndex) throws SQLException {
      return null;
    }

    public Date getDate(String columnName) throws SQLException {
      return null;
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
      return null;
    }

    public Date getDate(String columnName, Calendar cal) throws SQLException {
      return null;
    }

    public double getDouble(int columnIndex) throws SQLException {
      return 0;
    }

    public double getDouble(String columnName) throws SQLException {
      return 0;
    }

    public int getFetchDirection() throws SQLException {
      return 0;
    }

    public int getFetchSize() throws SQLException {
      return 0;
    }

    public float getFloat(int columnIndex) throws SQLException {
      return 0;
    }

    public float getFloat(String columnName) throws SQLException {
      return 0;
    }

    public int getInt(int columnIndex) throws SQLException {
      return 0;
    }

    public int getInt(String columnName) throws SQLException {
      return 0;
    }

    public long getLong(int columnIndex) throws SQLException {
      return 0;
    }

    public long getLong(String columnName) throws SQLException {
      return 0;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
      return null;
    }

    public Object getObject(int columnIndex) throws SQLException {
      return null;
    }

    public Object getObject(String columnName) throws SQLException {
      return null;
    }

    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
      return null;
    }

    public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException {
      return null;
    }

    public Ref getRef(int i) throws SQLException {
      return null;
    }

    public Ref getRef(String colName) throws SQLException {
      return null;
    }

    public int getRow() throws SQLException {
      return 0;
    }

    public short getShort(int columnIndex) throws SQLException {
      return 0;
    }

    public short getShort(String columnName) throws SQLException {
      return 0;
    }

    public Statement getStatement() throws SQLException {
      return null;
    }

    public String getString(int columnIndex) throws SQLException {
      return null;
    }

    public String getString(String columnName) throws SQLException {
      return null;
    }

    public Time getTime(int columnIndex) throws SQLException {
      return null;
    }

    public Time getTime(String columnName) throws SQLException {
      return null;
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
      return null;
    }

    public Time getTime(String columnName, Calendar cal) throws SQLException {
      return null;
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
      return null;
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {
      return null;
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
      return null;
    }

    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
      return null;
    }

    public int getType() throws SQLException {
      return 0;
    }

    public URL getURL(int columnIndex) throws SQLException {
      return null;
    }

    public URL getURL(String columnName) throws SQLException {
      return null;
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
      return null;
    }

    public InputStream getUnicodeStream(String columnName) throws SQLException {
      return null;
    }

    public SQLWarning getWarnings() throws SQLException {
      return null;
    }

    public void insertRow() throws SQLException {
      
    }

    public boolean isAfterLast() throws SQLException {
      return false;
    }

    public boolean isBeforeFirst() throws SQLException {
      return false;
    }

    public boolean isFirst() throws SQLException {
      return false;
    }

    public boolean isLast() throws SQLException {
      return false;
    }

    public boolean last() throws SQLException {
      return false;
    }

    public void moveToCurrentRow() throws SQLException {
      
    }

    public void moveToInsertRow() throws SQLException {
      
    }

    public boolean next() throws SQLException {
      return false;
    }

    public boolean previous() throws SQLException {
      return false;
    }

    public void refreshRow() throws SQLException {
      
    }

    public boolean relative(int rows) throws SQLException {
      return false;
    }

    public boolean rowDeleted() throws SQLException {
      return false;
    }

    public boolean rowInserted() throws SQLException {
      return false;
    }

    public boolean rowUpdated() throws SQLException {
      return false;
    }

    public void setFetchDirection(int direction) throws SQLException {
      
    }

    public void setFetchSize(int rows) throws SQLException {
      
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
      
    }

    public void updateArray(String columnName, Array x) throws SQLException {
      
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
      
    }

    public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
      
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
      
    }

    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
      
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    }

    public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException {
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
    }

    public void updateByte(String columnName, byte x) throws SQLException {
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    }

    public void updateBytes(String columnName, byte[] x) throws SQLException {
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    }

    public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
    }

    public void updateDate(String columnName, Date x) throws SQLException {
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
    }

    public void updateDouble(String columnName, double x) throws SQLException {
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
    }

    public void updateFloat(String columnName, float x) throws SQLException {
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
    }

    public void updateInt(String columnName, int x) throws SQLException {
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
    }

    public void updateLong(String columnName, long x) throws SQLException {
    }

    public void updateNull(int columnIndex) throws SQLException {
    }

    public void updateNull(String columnName) throws SQLException {
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
    }

    public void updateObject(String columnName, Object x) throws SQLException {
    }

    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
    }

    public void updateObject(String columnName, Object x, int scale) throws SQLException {
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
    }

    public void updateRef(String columnName, Ref x) throws SQLException {
    }

    public void updateRow() throws SQLException {
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
    }

    public void updateShort(String columnName, short x) throws SQLException {
    }

    public void updateString(int columnIndex, String x) throws SQLException {
    }

    public void updateString(String columnName, String x) throws SQLException {
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
    }

    public void updateTime(String columnName, Time x) throws SQLException {
    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    }

    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
    }

    public boolean wasNull() throws SQLException {
      return false;
    }
    
  }
  
  private static class TestPS implements PreparedStatement {

    public void addBatch() throws SQLException {
    }

    public void clearParameters() throws SQLException {
    }

    public boolean execute() throws SQLException {
      return false;
    }

    public ResultSet executeQuery() throws SQLException {
      return new TestRS();
    }

    public int executeUpdate() throws SQLException {
      return 0;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
      return null;
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
      return null;
    }

    public void setArray(int i, Array x) throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    public void setBlob(int i, Blob x) throws SQLException {
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    }

    public void setClob(int i, Clob x) throws SQLException {
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
    }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
    }

    public void setRef(int i, Ref x) throws SQLException {
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
    }

    public void setString(int parameterIndex, String x) throws SQLException {
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    }

    public void addBatch(String sql) throws SQLException {
    }

    public void cancel() throws SQLException {
    }

    public void clearBatch() throws SQLException {
    }

    public void clearWarnings() throws SQLException {
    }

    public void close() throws SQLException {
    }

    public boolean execute(String sql) throws SQLException {
      return false;
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
      return false;
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
      return false;
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
      return false;
    }

    public int[] executeBatch() throws SQLException {
      return null;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
      return new TestRS();
    }

    public int executeUpdate(String sql) throws SQLException {
      return 0;
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
      return 0;
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
      return 0;
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
      return 0;
    }

    public Connection getConnection() throws SQLException {
      return null;
    }

    public int getFetchDirection() throws SQLException {
      return 0;
    }

    public int getFetchSize() throws SQLException {
      return 0;
    }

    public ResultSet getGeneratedKeys() throws SQLException {
      return null;
    }

    public int getMaxFieldSize() throws SQLException {
      return 0;
    }

    public int getMaxRows() throws SQLException {
      return 0;
    }

    public boolean getMoreResults() throws SQLException {
      return false;
    }

    public boolean getMoreResults(int current) throws SQLException {
      return false;
    }

    public int getQueryTimeout() throws SQLException {
      return 0;
    }

    public ResultSet getResultSet() throws SQLException {
      return null;
    }

    public int getResultSetConcurrency() throws SQLException {
      return 0;
    }

    public int getResultSetHoldability() throws SQLException {
      return 0;
    }

    public int getResultSetType() throws SQLException {
      return 0;
    }

    public int getUpdateCount() throws SQLException {
      return 0;
    }

    public SQLWarning getWarnings() throws SQLException {
      return null;
    }

    public void setCursorName(String name) throws SQLException {
      
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
      
    }

    public void setFetchDirection(int direction) throws SQLException {
      
    }

    public void setFetchSize(int rows) throws SQLException {
    }

    public void setMaxFieldSize(int max) throws SQLException {
    }

    public void setMaxRows(int max) throws SQLException {
    }

    public void setQueryTimeout(int seconds) throws SQLException {
      
    }

    public boolean isClosed() throws SQLException {
      return false;
    }

    public void setPoolable(boolean poolable) throws SQLException {
    }

    public boolean isPoolable() throws SQLException {
      return false;
    }

    public void closeOnCompletion() throws SQLException {
    }

    public boolean isCloseOnCompletion() throws SQLException {
      return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    }
    
  }

  
  private static class TestConnection implements Connection {
    
    private boolean closed = false;
    
    private volatile boolean dbConnectionOk = dbConnectionOkDefault;
    
    public void clearWarnings() throws SQLException {
      checkDBConnectionState();
    }
    
    public void setDbConnectionOk(boolean ok) {
      dbConnectionOk = ok;
    }


    public void close() throws SQLException {
      closed = true;
    }


    public void commit() throws SQLException {
      checkDBConnectionState();
    }


    public Statement createStatement() throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public boolean getAutoCommit() throws SQLException {
      checkDBConnectionState();
      return false;
    }


    public String getCatalog() throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public int getHoldability() throws SQLException {
      checkDBConnectionState();
      return 0;
    }


    public DatabaseMetaData getMetaData() throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public int getTransactionIsolation() throws SQLException {
      checkDBConnectionState();
      return 0;
    }


    public Map<String, Class<?>> getTypeMap() throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public SQLWarning getWarnings() throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public boolean isClosed() throws SQLException {
      return closed;
    }


    public boolean isReadOnly() throws SQLException {
      checkDBConnectionState();
      return false;
    }


    public String nativeSQL(String sql) throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public CallableStatement prepareCall(String sql) throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
      checkDBConnectionState();
      return null;
    }

    
    private void checkDBConnectionState() throws SQLException {
      if (!dbConnectionOk) {
        throw new SQLException("dbConnection down");
      }
      if (closed) {
        throw new SQLException("connection is closed");
      }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
      checkDBConnectionState();
      return new TestPS();
    }


    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
      checkDBConnectionState();
    }


    public void rollback() throws SQLException {
      checkDBConnectionState();
    }


    public void rollback(Savepoint savepoint) throws SQLException {
      checkDBConnectionState();
    }


    public void setAutoCommit(boolean autoCommit) throws SQLException {
      checkDBConnectionState();
    }


    public void setCatalog(String catalog) throws SQLException {
      checkDBConnectionState();
    }


    public void setHoldability(int holdability) throws SQLException {
      checkDBConnectionState();
    }


    public void setReadOnly(boolean readOnly) throws SQLException {
      checkDBConnectionState();
    }


    public Savepoint setSavepoint() throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public Savepoint setSavepoint(String name) throws SQLException {
      checkDBConnectionState();
      return null;
    }


    public void setTransactionIsolation(int level) throws SQLException {
      checkDBConnectionState();
    }


    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
      checkDBConnectionState();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      checkDBConnectionState();
      return false;
    }

    public Clob createClob() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public Blob createBlob() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public NClob createNClob() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public SQLXML createSQLXML() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public boolean isValid(int timeout) throws SQLException {
      checkDBConnectionState();
      return false;
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
      try {
        checkDBConnectionState();
      } catch (SQLException e) {
        throw new SQLClientInfoException();
      }
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
      try {
        checkDBConnectionState();
      } catch (SQLException e) {
        throw new SQLClientInfoException();
      }
    }

    public String getClientInfo(String name) throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public Properties getClientInfo() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public void setSchema(String schema) throws SQLException {
      checkDBConnectionState();
    }

    public String getSchema() throws SQLException {
      checkDBConnectionState();
      return null;
    }

    public void abort(Executor executor) throws SQLException {
      checkDBConnectionState();
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
      checkDBConnectionState();
    }

    public int getNetworkTimeout() throws SQLException {
      checkDBConnectionState();
      return 0;
    }

  }

}
