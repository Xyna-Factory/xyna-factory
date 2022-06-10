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
package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReaderFactory;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.oracle.OraclePersistenceLayer;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;


/**
 *
 */
public class ClusterLockingInterfaceTest extends TestCase {

  private static final Logger logger = CentralFactoryLogging.getLogger(ClusterLockingInterfaceTest.class);

  private static final String LOCK_NAME_INTERNAL = "internal";
  private static final String LOCK_NAME_EXTERNAL = "external";
  
  private static DBConnectionData dbd = DBConnectionData.newDBConnectionData().
  user("").password("").url("").build();

  private ODS ods;
  
  private ClusterLockingInterface clusterLockingInterface;

  private SQLUtils sqlUtils;
 
  public static class OracleMockPersistenceLayer extends OraclePersistenceLayer {
    
    public ConnectionPool getPool() {
      try {
        Field f = OraclePersistenceLayer.class.getDeclaredField("pool");
        f.setAccessible(true);
        return (ConnectionPool) f.get(this);
      } catch( Exception e ) {
        e.printStackTrace();
        return null;
      }
    }
  }
  
  private OracleMockPersistenceLayer oracleMockPersistenceLayer;
  
  
  public void setUp() {
    System.setProperty("exceptions.storage", "./Exceptions.xml" );
    //System.err.println( new File(".").getAbsolutePath() );
    PropertyConfigurator.configure("test/log4j.properties");
    
    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();
    fexec.execAsync(EasyMock.isA(FutureExecutionTask.class));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
    public Object answer() {
        FutureExecutionTask fet = (FutureExecutionTask) EasyMock.getCurrentArguments()[0];
        if( fet.getClass().getSimpleName().equals("FutureExecutionTaskInit") ) {
          fet.execute();
        }
        return null;
    }}).anyTimes();
    EasyMock.replay(fexec);
    
    XynaFactory xf1 = EasyMock.createMock(XynaFactory.class);
    XynaFactory.setInstance(xf1);
    EasyMock.expect(xf1.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf1.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.replay(xf1);

    
    
    ClusterProvider cp = EasyMock.createMock(ClusterProvider.class);
    EasyMock.expect(cp.getState()).andReturn(ClusterState.CONNECTED).anyTimes();
    cp.checkInterconnect();
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(cp);
    
    XynaClusteringServicesManagementInterface xcsm = EasyMock.createMock(XynaClusteringServicesManagementInterface.class);
    try {
      EasyMock.expect(xcsm.getClusterInstance(EasyMock.anyInt())).andReturn(cp).anyTimes();
    }
    catch (XFMG_UnknownClusterInstanceIDException e1) {
      e1.printStackTrace();
    }
    xcsm.addClusterStateChangeHandler(EasyMock.anyInt(), EasyMock.<ClusterStateChangeHandler>anyObject() );
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(xcsm);
    
    XynaFactoryManagementBase xfmb = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfmb.getXynaClusteringServicesManagement()).andReturn(xcsm).anyTimes();
    EasyMock.replay(xfmb);
    
    XynaFactory xf = EasyMock.createMock(XynaFactory.class);
    XynaFactory.setInstance(xf);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfmb).anyTimes();
    EasyMock.replay(xf);
    
    ods = ODSImpl.getInstance(true);
    try {
      ods.registerPersistenceLayer(123, OracleMockPersistenceLayer.class);
      long id = ods.instantiatePersistenceLayerInstance(123 /*ods.getMemoryPersistenceLayerID()*/, "test",
        ODSConnectionType.DEFAULT, new String[]{dbd.getUser(),dbd.getPassword(),dbd.getUrl(), "10", "5000" } );
      ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    
      PersistenceLayer pl = ods.getDefaultPersistenceLayerInstance(ODSConnectionType.DEFAULT).getPersistenceLayerInstance();
      if( pl instanceof Clustered ) {
        ((Clustered)pl).enableClustering(1);
      }
      oracleMockPersistenceLayer = (OracleMockPersistenceLayer)pl;
      
      
    } catch (XynaException e) {
      logger.error("", e);
      fail(e.getMessage());
    }
    
    sqlUtils = dbd.createSQLUtils( new SQLUtilsLogger() {
      public void logSQL(String sql) {
        System.out.println(sql);
      }
      public void logException(Exception e) {
        e.printStackTrace();
      }
    });
    
    try {
      clusterLockingInterface = new ClusterLockingInterface();
      clusterLockingInterface.init();
    }
    catch (XynaException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    
    
    
  }
  
  public void tearDown() {
    String sql = "DELETE FROM ClusteringServicesLocks WHERE name=?";
    String lockName = LOCK_NAME_INTERNAL;
    DatabaseLock lock = clusterLockingInterface.getLock(lockName);
    if( lock != null ) {
      lock.shutdown();
    }
    sqlUtils.executeDML(sql, new Parameter(lockName) );
    
    
    lockName = LOCK_NAME_EXTERNAL;
    lock = clusterLockingInterface.getLock(lockName);
    if( lock != null ) {
      lock.shutdown();
    }
    sqlUtils.executeDML(sql, new Parameter(lockName) );
    sqlUtils.commit();
    ODSImpl.clearInstances();
  }
  
  private boolean checkLockExists(String name) {
    String sql = "SELECT count(*) FROM ClusteringServicesLocks WHERE name=?";
    return sqlUtils.queryInt(sql, new Parameter(name) ) > 0;
  }

  private boolean checkLockIsLocked(String name) {
    String sql = "SELECT name FROM ClusteringServicesLocks WHERE name=? FOR UPDATE WAIT 0";
    try {
      sqlUtils.setLogException(false);
      sqlUtils.query(sql, new Parameter(name), ResultSetReaderFactory.getStringReader() );
      if( sqlUtils.getLastException() == null ) {
        return false; //es gab kein Lock
      } else {
        if( sqlUtils.getLastException().getErrorCode() == 54 ) {
          //ORA-00054: Versuch, mit NOWAIT eine bereits belegte Ressource anzufordern.
          return true; //gelockt
        } else {
          //unerwartete Exception
          sqlUtils.logLastException();
          return false;
        }
      }
    } finally {
      sqlUtils.setLogException(true);
      sqlUtils.rollback();
    }
  }

  

  public void testCreateAndGetAndLock_Int() throws XynaException {
    String lockName = LOCK_NAME_INTERNAL;
    
    assertFalse( "lock should not exist", checkLockExists(lockName) );
    
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.InternalConnection);
    
    assertTrue( "lock should exist", checkLockExists(lockName) );
    
    assertSame(databaseLock, clusterLockingInterface.getLock(lockName) );
    
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    databaseLock.lock();
    try {
      assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
    } finally {
      databaseLock.unlock();
    }
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    
  }
  
  
  public void testCreateAndGetAndLock_Ext() throws XynaException {
    String lockName = LOCK_NAME_EXTERNAL;

    assertFalse( "lock should not exist", checkLockExists(lockName) );

    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.ExternalConnection);
    
    assertTrue( "lock should exist", checkLockExists(lockName) );
    
    assertSame(databaseLock, clusterLockingInterface.getLock(lockName) );
    
    try {
      databaseLock.lock();
    } catch( LockFailedException e ) {
      assertEquals("lock with Connection can not be used to set internal connection", e.getMessage());
    }
      
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    databaseLock.lock(ods.openConnection(ODSConnectionType.DEFAULT));
    try {
      assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
    } finally {
      databaseLock.unlock();
    }
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
  }
  
  
  public void testLockIsReentrant_Int() {
    String lockName = LOCK_NAME_INTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.InternalConnection);
     
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    databaseLock.lock();
    try {
      databaseLock.lock();
      try {
        assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
        databaseLock.lock();
        try {
          assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
        } finally {
          databaseLock.unlock();
        }
        assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
     } finally {
        databaseLock.unlock();
      }
      assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
    } finally {
      databaseLock.unlock();
    }
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );

  }
  
  public void testLockIsReentrant_Ext() {
    String lockName = LOCK_NAME_EXTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.ExternalConnection);
     
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    databaseLock.lock(ods.openConnection(ODSConnectionType.DEFAULT));
    try {
      databaseLock.lock();
      try {
        assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
        databaseLock.lock();
        try {
          assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
        } finally {
          databaseLock.unlock();      
        }
        assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
     } finally {
        databaseLock.unlock();
      }
      assertTrue( "lock should be locked", checkLockIsLocked(lockName) );
    } finally {
      databaseLock.unlock();
    }
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );

  }
  
  private static class LockingThread implements Runnable {
    private DatabaseLock databaseLock;
    private long sleep;
    private String name;
    private List<String> actions;

    public LockingThread(DatabaseLock databaseLock, long sleep, String name, List<String> actions) {
      this.databaseLock = databaseLock;
      this.sleep = sleep;
      this.name = name;
      this.actions = actions;
    }

    public void run() {
      actions.add( name+" starts" );
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      databaseLock.lock();
      try {
        actions.add( name+" has got lock" );
        Thread.sleep(sleep);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        databaseLock.unlock();
        actions.add( name+" has unlocked" );
      }
    }
    
  }
  
  
  public void testLockIsLocking_Int() {
    String lockName = LOCK_NAME_INTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.InternalConnection);
    DatabaseLock databaseLock2 = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.InternalConnection);
     
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
    
    List<String> actions =  Collections.synchronizedList(new ArrayList<String>() );
    
    Thread ta = new Thread( new LockingThread(databaseLock, 1000, "A", actions ) );
    Thread tb = new Thread( new LockingThread(databaseLock, 1000, "B", actions ) );
    //ta und tb können wegen gleichem ReentrantLock im databaseLock nie gleichzeitig locken
    
    Thread tc = new Thread( new LockingThread(databaseLock2, 1000, "C", actions ) );
    //tc hat nun eigenständiges ReentrantLock, damit wird DB für korrektes Lock benötigt
    
    ta.start();
    tb.start();
    tc.start();
    try {
      ta.join();
      tb.join();
      tc.join();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
   
    System.err.println( actions );
    //actions sollte jetzt so ähnlich aussehen, Reihenfolge der A, B, C kann variieren
    //"[A starts, C starts, B starts, B has got lock, B has unlocked, A has got lock, A has unlocked, C has got lock, C has unlocked]"

    //Stimmen die 3 Starts?
    HashSet<String> expected = new HashSet<String>();
    HashSet<String> actual = new HashSet<String>();
    actual.add( actions.get(0) ); expected.add( "A starts" );
    actual.add( actions.get(1) ); expected.add( "B starts" );
    actual.add( actions.get(2) ); expected.add( "C starts" );
    assertEquals(expected, actual);
    
    actual.clear(); expected.clear();
    
    actual.add( actions.get(3)+" - "+actions.get(4) ); expected.add( "A has got lock - A has unlocked" );
    actual.add( actions.get(5)+" - "+actions.get(6) ); expected.add( "B has got lock - B has unlocked" );
    actual.add( actions.get(7)+" - "+actions.get(8) ); expected.add( "C has got lock - C has unlocked" );
    assertEquals(expected, actual);
    
  }
  
  
  public void testLockConnectionBroken_Int() throws SQLException {
    String lockName = LOCK_NAME_INTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.InternalConnection);
    
    databaseLock.lock();
    try {

      ConnectionPool pool = oracleMockPersistenceLayer.getPool();
      pool.recreateAllConnections(true);
      
      assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
      
    } finally {
      try {
        databaseLock.unlock();
        fail( "Exception expected");
      } catch( AlreadyUnlockedException e ) {
        assertEquals("com.gip.xyna.xnwh.persistence.oracle.OraclePersistenceLayer$SQLRuntimeException2: java.sql.SQLException: Getrennte Verbindung", e.getMessage() );
      }
    }
    
  }
  
  public void testLockConnectionBroken_Ext() throws SQLException {
    String lockName = LOCK_NAME_EXTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.ExternalConnection);
    
    databaseLock.lock(ods.openConnection(ODSConnectionType.DEFAULT));
    try {

      ConnectionPool pool = oracleMockPersistenceLayer.getPool();
      pool.recreateAllConnections(true);
      
      assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
      
    } finally {
      try {
        databaseLock.unlock();
        fail( "Exception expected");
      } catch( AlreadyUnlockedException e ) {
        assertEquals("com.gip.xyna.xnwh.persistence.oracle.OraclePersistenceLayer$SQLRuntimeException2: java.sql.SQLException: Getrennte Verbindung", e.getMessage() );
      }
    }
    
  }
  
  public void testLockConnectionCommited_Ext() {
    String lockName = LOCK_NAME_EXTERNAL;
    DatabaseLock databaseLock = clusterLockingInterface.createLockIfNonexistent( lockName, ClusterLockingInterface.DatabaseLockType.ExternalConnection);
    
    
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    databaseLock.lock(con);
    try {
      con.commit();
      
      
      assertFalse( "lock should not be locked", checkLockIsLocked(lockName) );
      
    }
    catch (PersistenceLayerException e) {
      e.printStackTrace();
    } finally {
      try {
        databaseLock.unlock();
        fail( "Exception expected");
      } catch( AlreadyUnlockedException e ) {
        e.printStackTrace();
        assertTrue( e.getMessage().startsWith( "Connection was committed ") );
      }
    }
    
  }
  
  

  
  
  
  
  
  
  
  
  
  
  
  

  /**
   * @throws PersistenceLayerException 
   * 
   */
  private void showLocks() throws PersistenceLayerException {
    
    ods.registerStorable(ClusteringServicesLockStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    Collection<ClusteringServicesLockStorable> locks = con.loadCollection(ClusteringServicesLockStorable.class);
    
    System.err.println(locks.size());
    for( ClusteringServicesLockStorable lock : locks ) {
      System.err.println(lock);
    }
  }
  
   
  
}
