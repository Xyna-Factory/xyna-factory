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
package com.gip.xyna.xprc.xpce.transaction;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.PooledConnection;
import com.gip.xyna.utils.db.WrappedConnection;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.exceptions.XPRC_FailedToOpenTransaction;
import com.gip.xyna.xprc.exceptions.XPRC_TransactionOperationFailed;
import com.gip.xyna.xprc.exceptions.XPRC_UnknownTransaction;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xpce.transaction.connectionpool.ConnectionPoolTransaction;
import com.gip.xyna.xprc.xpce.transaction.connectionpool.ConnectionPoolTransactionType;
import com.gip.xyna.xprc.xpce.transaction.parameter.DisposalStrategyParameter;
import com.gip.xyna.xprc.xpce.transaction.parameter.OnGarbageCollection;
import com.gip.xyna.xprc.xpce.transaction.parameter.OnOrderTermination;
import com.gip.xyna.xprc.xpce.transaction.parameter.OperationPrevention;
import com.gip.xyna.xprc.xpce.transaction.parameter.SafeguardParameter;
import com.gip.xyna.xprc.xpce.transaction.parameter.TTL;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionOperation;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionParameter;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;

import junit.framework.TestCase;


public class TransactionManagementTest extends TestCase {
  
  private TransactionManagement txMgmt;
  private ConnectionPool testPool;
  private List<CountingConnection> openedConnections;
  private StatusChangeProvider provider;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    openedConnections = new ArrayList<>();
    IDGenerator idGen = EasyMock.createMock(IDGenerator.class);
    final AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGen.getUniqueId("TransactionMgmt")).andAnswer(new IAnswer<Long>() {
      public Long answer() throws Throwable {
        return longGenerator.getAndIncrement();
      }
    }).anyTimes();
    EasyMock.expect(idGen.getUniqueId()).andAnswer(new IAnswer<Long>() {
      public Long answer() throws Throwable {
        return longGenerator.getAndIncrement();
      }
    }).anyTimes();
    IDGenerator.setInstance(idGen);
    
    txMgmt = new TransactionManagement();
    
    Field field = ConnectionPoolTransaction.class.getDeclaredField("validation");
    field.setAccessible(true);
    field.set(null, new ValidationStrategy() {

      @Override
      public boolean isValidationNecessary(long currentTime, long lastcheck) {
        return false;
      }

      @Override
      public Exception validate(Connection con) {
        return null;
      }

      @Override
      public void setValidationInterval(long validationInterval) {
        
      }

      @Override
      public long getValidationInterval() {
        return 1000;
      }

      @Override
      public boolean rebuildConnectionAfterFailedValidation() {
        return false;
      }

    });
    provider = new StatusChangeProvider();
    testPool = EasyMock.createMock(ConnectionPool.class);
    EasyMock.expect(testPool.getConnection(EasyMock.anyLong(), EasyMock.isA(String.class))).andAnswer(new IAnswer<CountingConnection>() {
      public CountingConnection answer() throws Throwable {
        CountingConnection con = new CountingConnection();
        openedConnections.add(con);
        return con;
      }
    }).anyTimes();
    ConnectionPoolManagement conPoolMgmt = EasyMock.createMock(ConnectionPoolManagement.class);
    EasyMock.expect(conPoolMgmt.getConnectionPool(EasyMock.isA(String.class))).andAnswer(new IAnswer<ConnectionPool>() {
      public ConnectionPool answer() throws Throwable {
        return testPool;
      }
    }).anyTimes();
    
    
    XynaProcessCtrlExecution xprce = EasyMock.createMock(XynaProcessCtrlExecution.class);
    EasyMock.expect(xprce.getTransactionManagement()).andReturn(txMgmt).anyTimes();
    EasyMock.expect(xprce.getStatusChangeProvider()).andReturn(provider).anyTimes();
    XynaProcessingBase xprc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xprc.getXynaProcessCtrlExecution()).andReturn(xprce).anyTimes();
    XynaFactoryWarehouseBase xnwh = EasyMock.createMock(XynaFactoryWarehouseBase.class);
    EasyMock.expect(xnwh.getConnectionPoolManagement()).andReturn(conPoolMgmt).anyTimes();
    XynaFactoryBase factory = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(factory.getXynaNetworkWarehouse()).andReturn(xnwh).anyTimes();
    EasyMock.expect(factory.getProcessing()).andReturn(xprc).anyTimes();
    EasyMock.expect(factory.isShuttingDown()).andReturn(Boolean.FALSE).anyTimes();
    EasyMock.expect(factory.getIDGenerator()).andReturn(idGen).anyTimes();
    XynaFactory.setInstance(factory);
    
    EasyMock.replay(idGen, factory, xnwh, xprc, xprce, conPoolMgmt, testPool);
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    txMgmt = null;
  }
  
  @Test
  public void testSimpleLifecycle() throws XPRC_FailedToOpenTransaction, InterruptedException, XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    TransactionParameter tp = generateTransactionParameter(new TTL(5000L, true));
    Long txId = txMgmt.openTransaction(tp);
    assertEquals("There should be 1 connection open", 1, openedConnections.size());
    TransactionAccess access = txMgmt.access(txId);
    ConnectionPoolTransaction transaction = access.getTypedTransaction(ConnectionPoolTransaction.class);
    Connection con = transaction.getConnection();
    assertTrue("Connection should be of type CountingConnection", con instanceof CountingConnection);
    assertEquals("There should be 1 connection open", 1, openedConnections.size());
    txMgmt.commit(txId);
    access = txMgmt.access(txId);
    transaction = access.getTypedTransaction(ConnectionPoolTransaction.class);
    con = transaction.getConnection();
    assertTrue("Connection should be of type CountingConnection", con instanceof CountingConnection);
    assertEquals("There should be 1 connection open", 1, openedConnections.size());
    txMgmt.rollback(txId);
    access = txMgmt.access(txId);
    transaction = access.getTypedTransaction(ConnectionPoolTransaction.class);
    con = transaction.getConnection();
    assertTrue("Connection should be of type CountingConnection", con instanceof CountingConnection);
    assertEquals("There should be 1 connection open", 1, openedConnections.size());
    txMgmt.commit(txId);
    txMgmt.end(txId);
    
    try {
      txMgmt.access(txId);
      fail("Access should have failed, tx is already closed");
    } catch (XPRC_UnknownTransaction e) {
      // ntbd
    }
    
    assertEquals("There should be 1 connection open", 1, openedConnections.size());
    CountingConnection cc = openedConnections.get(0);
    assertEquals("Count does not equal expectation", 2, cc.commits.get());
    assertEquals("Count does not equal expectation", 1, cc.rollbacks.get());
    assertEquals("Count does not equal expectation", 1, cc.ends.get());
  }
  
  
  @Test
  public void testMultiPoolLifecycle() throws XPRC_FailedToOpenTransaction, InterruptedException, XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    TransactionParameter tp = generateTransactionParameter("TP1,TP2,TP3", new TTL(5000L, true));
    Long txId = txMgmt.openTransaction(tp);
    assertEquals("There should be 3 connections open", 3, openedConnections.size());
    TransactionAccess access = txMgmt.access(txId);
    ConnectionPoolTransaction transaction = access.getTypedTransaction(ConnectionPoolTransaction.class);
    Connection unspecifiedCon = transaction.getConnection();
    Connection firstCon = transaction.getConnection(0);
    Connection namedCon = transaction.getConnection("TP1");
    assertTrue("The result of all 3 getConnection should be equal", unspecifiedCon == firstCon && firstCon == namedCon);
    assertTrue("Connection should be of type CountingConnection", unspecifiedCon instanceof CountingConnection);
    txMgmt.commit(txId);
    txMgmt.end(txId);
    
    try {
      txMgmt.access(txId);
      fail("Access should have failed, tx is already closed");
    } catch (XPRC_UnknownTransaction e) {
      // ntbd
    }
    
    CountingConnection cc1 = openedConnections.get(0);
    assertEquals("Count does not equal expectation", 1, cc1.commits.get());
    assertEquals("Count does not equal expectation", 0, cc1.rollbacks.get());
    assertEquals("Count does not equal expectation", 1, cc1.ends.get());
    CountingConnection cc2 = openedConnections.get(1);
    assertEquals("Count does not equal expectation", 1, cc2.commits.get());
    assertEquals("Count does not equal expectation", 0, cc2.rollbacks.get());
    assertEquals("Count does not equal expectation", 1, cc2.ends.get());
    CountingConnection cc3 = openedConnections.get(2);
    assertEquals("Count does not equal expectation", 1, cc3.commits.get());
    assertEquals("Count does not equal expectation", 0, cc3.rollbacks.get());
    assertEquals("Count does not equal expectation", 1, cc3.ends.get());
  }
  
  
  @Test
  public void testTTLGuard() throws XPRC_FailedToOpenTransaction, InterruptedException, XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    TransactionParameter tp = generateTransactionParameter(new TTL(5000L, false));
    Long txId = txMgmt.openTransaction(tp);
    Thread.sleep(2000);
    txMgmt.commit(txId);
    Thread.sleep(2000);
    txMgmt.commit(txId);
    Thread.sleep(2000);
    try {
      txMgmt.commit(txId);
      fail("Commit should have failed, TTLGuard should have removed the tx");
    } catch (XPRC_UnknownTransaction e) {
      // passt
    }
    
    tp = generateTransactionParameter(new TTL(5000L, true));
    txId = txMgmt.openTransaction(tp);
    Thread.sleep(2000);
    txMgmt.commit(txId);
    Thread.sleep(2000);
    txMgmt.commit(txId);
    Thread.sleep(2000);
    try {
      txMgmt.commit(txId);
    } catch (XPRC_UnknownTransaction e) {
      fail("Commit should not have failed, TTLGuard should have refreshed the tx");
    }
    txMgmt.end(txId);
  }
  
  // get's stuck on jenkins?
  /*@Test
  public void testReferenceGuard() throws XPRC_FailedToOpenTransaction, InterruptedException, XPRC_UnknownTransaction, XPRC_TransactionOperationFailed {
    CountDownLatch transactionCreationFinished = new CountDownLatch(1);
    CountDownLatch terminate = new CountDownLatch(1);
    AtomicLong txIdHolder = new AtomicLong(-1);
    
    DetachtedReferenceCreator drc = new DetachtedReferenceCreator(transactionCreationFinished, terminate, txIdHolder, txMgmt);
    Thread t = new Thread(drc);
    t.start();
    
    transactionCreationFinished.await();
    txMgmt.access(txIdHolder.get());
    
    System.gc();
    Thread.sleep(3000);
    System.gc();
    
    txMgmt.access(txIdHolder.get());
    
    terminate.countDown();
    t.join();
    System.gc();
    Thread.sleep(3000);
    System.gc();
    
    try {
      txMgmt.access(txIdHolder.get());
      fail("Access should have failed, ReferenceGuard should have killed the tx");
    } catch (XPRC_UnknownTransaction e) {
      // ntbd
    }
  }*/
  
  
  private final static class DetachtedReferenceCreator implements Runnable {
    
    private final CountDownLatch transactionCreationFinished;
    private final CountDownLatch terminate;
    private final AtomicLong txIdHolder;
    private final TransactionManagement txMgmt;
    
    DetachtedReferenceCreator(CountDownLatch transactionCreationFinished,
                              CountDownLatch terminate, AtomicLong txIdHolder, TransactionManagement txMgmt) {
      this.transactionCreationFinished = transactionCreationFinished;
      this.terminate = terminate;
      this.txIdHolder = txIdHolder;
      this.txMgmt = txMgmt;
    }

    @Override
    public void run() {
      Object reference = new Object();
      TransactionParameter tp = generateTransactionParameter(new OnGarbageCollection(reference));
      try {
        Long txId = txMgmt.openTransaction(tp);
        txIdHolder.set(txId);
        transactionCreationFinished.countDown();
        terminate.await();
      } catch (XPRC_FailedToOpenTransaction e) {
        e.printStackTrace();
        fail("Detachted tx start failed");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
  }
  
  
  @Test
  public void testOrderGuard() throws XPRC_FailedToOpenTransaction, InterruptedException {
    final DestinationKey dk = new DestinationKey("bg.Test", new Workspace("Test"));
    XynaOrder xo = new XynaOrder(dk);
    XynaOrderServerExtension xose = new XynaOrderServerExtension(xo);
    xose.setInformStateTransitionListeners(true);
    TransactionParameter tp = generateTransactionParameter(new OnOrderTermination(xo.getId(), dk));
    Long txId = txMgmt.openTransaction(tp);
    
    try {
      txMgmt.access(txId);
    } catch (XPRC_UnknownTransaction e) {
      fail("Transaction should have been registered");
    }
    
    provider.notifyListeners(xose, OrderInstanceStatus.RUNNING_PLANNING);
    Thread.sleep(1000);
    
    try {
      txMgmt.access(txId);
    } catch (XPRC_UnknownTransaction e) {
      fail("Transaction should have been registered");
    }
    
    provider.notifyListeners(xose, OrderInstanceStatus.RUNNING);
    Thread.sleep(1000);
    
    try {
      txMgmt.access(txId);
    } catch (XPRC_UnknownTransaction e) {
      fail("Transaction should have been registered");
    }
    
    Thread.sleep(1000);
    provider.notifyListeners(xose, OrderInstanceStatus.FINISHED);

    try {
      txMgmt.access(txId);
      fail("Transaction should have been killed");
    } catch (XPRC_UnknownTransaction e) {
      //ntbd
    }
  }
  
  
  @Test
  public void testOperationPrevention() throws XPRC_FailedToOpenTransaction {
    TransactionParameter tp = generateTransactionParameter(new OperationPrevention(Collections.singleton(TransactionOperation.COMMIT), false));
    Long txId = txMgmt.openTransaction(tp);
    
    TransactionAccess access = null;
    try {
      access = txMgmt.access(txId);
    } catch (XPRC_UnknownTransaction e1) {
      fail("Access should have been possible");
    }
    ConnectionPoolTransaction cpt = access.getTypedTransaction(ConnectionPoolTransaction.class);
    Connection c = cpt.getConnection();
    
    assertFalse("Countion should have been wrapped", c instanceof CountingConnection);
    assertTrue("Countion should have been wrapped", c instanceof WrappedConnection);
    try {
      c.commit();
      c.rollback();
      c.close();
    } catch (SQLException e) {
      fail("Operation should have been allowed and tx known");
    }
    
    CountingConnection cc = openedConnections.get(0);
    assertEquals("Count does not equal expectation", 0, cc.commits.get());
    assertEquals("Count does not equal expectation", 1, cc.rollbacks.get());
    assertEquals("Count does not equal expectation", 1, cc.ends.get());
    
    try {
      txMgmt.end(txId);
    } catch (XPRC_TransactionOperationFailed e) {
      fail("Operation should have been allowed and tx known");
    }
    
    
    // with throw
    tp = generateTransactionParameter(new OperationPrevention(Collections.singleton(TransactionOperation.COMMIT), true));
    txId = txMgmt.openTransaction(tp);
    
    access = null;
    try {
      access = txMgmt.access(txId);
    } catch (XPRC_UnknownTransaction e1) {
      fail("Access should have been possible");
    }
    cpt = access.getTypedTransaction(ConnectionPoolTransaction.class);
    c = cpt.getConnection();
    
    assertFalse("Countion should have been wrapped", c instanceof CountingConnection);
    assertTrue("Countion should have been wrapped", c instanceof WrappedConnection);
    
    try {
      c.commit();
      fail("We should have failed with IllegalAccessError");
    } catch (IllegalAccessError e) {
      // expected
    } catch (SQLException e) {
      fail("We should not have failed with SQLEXception");
    }
  }
  

  private static TransactionParameter generateTransactionParameter(DisposalStrategyParameter dsp) {
    return generateTransactionParameter("TestPool", dsp);
  }
  
  private static TransactionParameter generateTransactionParameter(String pools, DisposalStrategyParameter dsp) {
    return generateTransactionParameter(pools, dsp, new OperationPrevention(Collections.<TransactionOperation>emptySet(), false));
  }
  
  private static TransactionParameter generateTransactionParameter(String pools, DisposalStrategyParameter dsp, OperationPrevention op) {
    Map<String, String> specifics = new HashMap<>();
    specifics.put(ConnectionPoolTransactionType.KEY_CLIENT_INFO, "TransactionManagementTest");
    specifics.put(ConnectionPoolTransactionType.KEY_CONNECTION_POOLS, pools);
    specifics.put(ConnectionPoolTransactionType.KEY_CONNECTION_TIMEOUT, "5000");
    SafeguardParameter sp = new SafeguardParameter(dsp, op);
    return new TransactionParameter(ConnectionPoolTransactionType.TRANSACTION_TYPE_NAME, sp, specifics);
  }
  
  private static TransactionParameter generateTransactionParameter(OperationPrevention op) {
    return generateTransactionParameter("TestPool", new TTL(5000L, true), op);
  }
  
  
  private static class CountingConnection extends PooledConnection {

    public final AtomicInteger commits = new AtomicInteger(0);
    public final AtomicInteger rollbacks = new AtomicInteger(0);
    public final AtomicInteger ends = new AtomicInteger(0);
    
    public CountingConnection() {
      super(null);
    }
    
    @Override
    public void commit() throws SQLException {
      commits.incrementAndGet();
    }
    
    @Override
    public void rollback() throws SQLException {
      rollbacks.incrementAndGet();
    }
    
    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
      rollbacks.incrementAndGet();
    }
    
    @Override
    public void close() throws SQLException {
      ends.incrementAndGet();
    }
    
  }
}
