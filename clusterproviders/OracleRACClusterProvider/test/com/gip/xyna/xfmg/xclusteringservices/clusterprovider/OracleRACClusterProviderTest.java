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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.IConnectionFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReaderFactory;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import junit.framework.TestCase;



/**
 *
 */
public class OracleRACClusterProviderTest extends TestCase {

  private static final String TIMEOUT = "3000";

  private static Logger logger = Logger.getLogger(OracleRACClusterProviderTest.class);
  
  private static DBConnectionData dbd = DBConnectionData.newDBConnectionData().
  user("").password("").url("").build();
  
  private static class LocalSQLUtilsLogger implements SQLUtilsLogger {
    public void logException(Exception e) {
      e.printStackTrace();
    }
    public void logSQL(String message) {
      System.out.println(message);
    }
  }
  
  private static class TestableOracleRACClusterProvider extends OracleRACClusterProvider {
    
    private static HashMap<Long,OracleRACClusterProviderConfiguration> configs = new HashMap<Long,OracleRACClusterProviderConfiguration>();
    private ArrayList<TestConnection> testConnections = new ArrayList<TestConnection>();
    private boolean canConnect = true;

    public TestableOracleRACClusterProvider(String connectionPoolSuffix) {
      this.poolName = "TestConnectionPool" + connectionPoolSuffix;
    }
    
    protected void initStorable() throws PersistenceLayerException {
    }
    
    protected void persistConfig() {
      logger.info("persistConfig "+ config.getBinding() );
      configs.put( Long.valueOf(config.getBinding()), config);  //Achtung id != binding, aber zum Testen einfacher 
    }
    
    protected void restoreConfig(long id) throws XFMG_ClusterInitializationException {
      config = configs.get(id);
      logger.info("restoreConfig -> "+config);
    }

    protected IConnectionFactory createConnectionFactory(final DBConnectionData dbdata ) {
      return new IConnectionFactory() {
        public Connection createNewConnection() {
          //System.err.println("\n\n\n createNewConnection \n\n\n");
          if( canConnect ) {
            TestConnection tc;
            try {
              tc = new TestConnection(dbdata.createConnection());
            }
            catch (Exception e) {
              throw new RuntimeException(e);
            }
            testConnections.add(tc);
            return tc;
          } else {
            throw new RuntimeException("Test: no Connection");
          }
        }

        public void markConnection(Connection con, String clientInfo) {
          DBConnectionData.markConnection(con, clientInfo);
        }
      };
    }

    /**
     * 
     */
    public void shutdownInterconnect() {
      interconnect.shutdown("shutdown");
    }
    
    public List<TestConnection> getTestConnections() {
      return testConnections;
    }

    /**
     * 
     */
    public void setCanConnect(boolean canConnect) {
      this.canConnect = canConnect;
    }
    
  }

  private static class ClusterStateChangeHandlerImpl implements ClusterStateChangeHandler {
    private volatile ClusterState newClusterState;
    private int binding;
    private boolean readyForConnected;
    private Exception newClusterStateCaller;
    public ClusterStateChangeHandlerImpl(int binding) {
      this.binding = binding;
    }
    public void onChange(ClusterState clusterState) {
      if( newClusterState != null ) {
        //logger.info( "onChange called from " , new Exception() );
        fail("last newClusterState "+newClusterState);
      }
      newClusterState = clusterState;
      newClusterStateCaller = new Exception();
    }
    
    public void expectClusterStateChange(ClusterState clusterState, String calledFrom ) {
      expectClusterStateChange(clusterState, calledFrom, 20);
    }
  
    public void expectClusterStateChange(ClusterState clusterState, String calledFrom, int timeout ) {
      if( newClusterState == null ) {
        //hier kann es leider zu Synchronisationsproblemen zwischen den Threads kommen: Der Test wird einen
        //kurzen Moment vor dem asynchronen onChange-Aufruf ausgeführt. Daher einfach noch einen Moment warten
        int max = timeout;
        while( newClusterState == null ) {
          try {
            System.out.print(".");
            Thread.sleep(10);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
          --max;
          if( max <= 0 ) {
            System.out.println();
            fail( "onChange("+clusterState+") not called");
          }
        }
        logger.info( "expectClusterStateChange wait = "+(timeout-max)*10+" ms for change to "+clusterState);
      }
      assertEquals(clusterState, newClusterState);
      //newClusterStateCaller.printStackTrace();
      String method = newClusterStateCaller.getStackTrace()[1].getMethodName();
      if( method.equals("changeState") ) {
        
        method = newClusterStateCaller.getStackTrace()[2].getMethodName();
        if( method.equals("executeInternal") ) {
          method = newClusterStateCaller.getStackTrace()[7].getMethodName();
          if( method.equals("dbNotReachable") ) {
            method = "dbNotReachable-"+newClusterStateCaller.getStackTrace()[8].getMethodName();
          }
        } else if( method.equals("changeClusterStateInternal") ) {
          method = newClusterStateCaller.getStackTrace()[3].getMethodName();
        }
        
      } else if( method.equals("refreshStateAccordingToSetupTable") ) {
        method = "remote-"+newClusterStateCaller.getStackTrace()[2].getMethodName();
      } else if( method.equals("dbNotReachable") ) {
        method = "dbNotReachable-"+newClusterStateCaller.getStackTrace()[2].getMethodName();
      } else {
        method = "Fehler";
        newClusterStateCaller.printStackTrace();
      }
      if( ! calledFrom.equals(method) ) {
        newClusterStateCaller.printStackTrace();
      }
      assertEquals( calledFrom, method );
      
      newClusterState = null;
    }
    public void close() {
      if( newClusterState != null ) {
        fail("last newClusterState "+newClusterState);
      }
    }
    @Override
    public String toString() {
      return "ClusterStateChangeHandlerImpl("+binding+")";
    }
    public void setReadyForConnected( boolean readyForConnected ) {
      this.readyForConnected = readyForConnected;
    }
    
    public boolean isReadyForChange(ClusterState newState) {
      if( newState == ClusterState.CONNECTED ) {
        return readyForConnected;
      } else {
        fail("isReadyForChange called for "+newState);
        return false;
      }
    }
  }
  
  
  private SQLUtils sqlUtils;
  private TestableOracleRACClusterProvider cp1;
  private TestableOracleRACClusterProvider cp2;
  
  
  protected void setUp() {
    //System.err.println( new File(".").getAbsolutePath() );
    PropertyConfigurator.configure("test/log4j.properties");
    
    XynaPropertyUtils.exchangeXynaPropertySource( new XynaPropertyUtils.AbstractXynaPropertySource() {
      
      public String getProperty(String name) {
        if( name.equals("xyna.xfmg.xcs.db_retry_wait") ) {
          return "2";
        }
        if( name.equals("xyna.xfmg.xcs.db_retry_attempts") ) {
          return "2";
        }
        return null;
      }
    });
    
    sqlUtils = dbd.createSQLUtils(new LocalSQLUtilsLogger() );
    sqlUtils.executeDML("delete from xynaClusterSetup", new Parameter() );
    sqlUtils.commit();
    
    RemoteInterfaceForClusterStateChangesImplAQ.DEQUEUE_TIMEOUT = 3;
    
  }
  
  protected void tearDown() {
    logger.info("\n\n\n\n     TearDown\n\n\n\n");
    
    if( cp1 != null ) {
      try {
        cp1.setCanConnect(true);
        cp1.setClusterStateChangeHandler(null);
        cp1.disconnect();
      } catch( Exception e) {
        logger.error( "Exception in tearDown: "+e.getMessage(),e);
      }
      cp1 = null;
    }
    if( cp2 != null ) {
      try {
        cp2.setCanConnect(true);
        cp2.setClusterStateChangeHandler(null);
        cp2.disconnect();
      } catch( Exception e) {
        logger.error( "Exception in tearDown: "+e.getMessage(),e);
      }
      cp2 = null;
    }
    
    sqlUtils.executeDML("delete from xynaClusterSetup", new Parameter() );
    prepareDB(1,ClusterState.DISCONNECTED_MASTER,false);
    prepareDB(2,ClusterState.DISCONNECTED_SLAVE,false);
    sqlUtils.commit();
    sqlUtils.closeConnection();
  }
 
  private void prepareDB(int binding, ClusterState clusterState, boolean isOnline) {
    sqlUtils.executeDML("delete from xynaClusterSetup where binding =?", new Parameter(binding) );
    String insert = "insert into xynaClusterSetup (binding,state,isonline) values(?,?,?)";
    sqlUtils.executeDML(insert, new Parameter(binding,clusterState.toString(),isOnline?1:0) );
  }
  
  
  public void testCreateSingleClusterAndShutdown() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException  {
    cp1 = new TestableOracleRACClusterProvider("1");
    
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.SINGLE, "disconnect");
    csch1.close();
    assertEquals("", checkDB(1,ClusterState.SINGLE,false) );
  }

  public void testCreateAndJoinClusterAndShutdown() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException  {
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);

    //Create 1
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    cp1.setClusterStateChangeHandler(csch1);
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    
    //Join 2 (notfies 1)
    csch1.setReadyForConnected(false);
    cp2.joinCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    cp2.setClusterStateChangeHandler(csch2);
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals(ClusterState.STARTING, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    assertEquals("", checkDB(2,ClusterState.STARTING,true) );
    
    //Connect 1
    csch1.setReadyForConnected(true);
    cp1.readyForStateChange();
    csch1.expectClusterStateChange(ClusterState.CONNECTED, "connect", 100 );
    csch2.expectClusterStateChange(ClusterState.CONNECTED, "remote-connect");
    assertEquals(ClusterState.CONNECTED, cp1.getState() );
    assertEquals(ClusterState.CONNECTED, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.CONNECTED,true) );
    assertEquals("", checkDB(2,ClusterState.CONNECTED,true) );
    
    //Disconnect 1
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "disconnect");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "remote-disconnect");
    //assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    //Disconnect 2
    cp2.disconnect();
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "disconnect");
    //assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    //assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,false) );
   
    csch1.close();
    csch2.close();
  }

  public void testReconnectSingle() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException {
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.SINGLE, "disconnect");
    csch1.close();

    assertEquals("", checkDB(1,ClusterState.SINGLE,false) );
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.restoreClusterPrepare(1);
    cp1.setClusterStateChangeHandler(csch1);
    cp1.restoreClusterConnect();
    csch1.expectClusterStateChange(ClusterState.SINGLE, "restoreClusterConnect");
    
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    
    cp1.disconnect();
  }
  
  public void testReconnectOne() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
    
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,false) );
     
    cp1 = new TestableOracleRACClusterProvider("1");
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    
    cp1.restoreClusterPrepare(1);
    
    assertEquals(ClusterState.STARTING, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.STARTING,true) );
    
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "disconnect");
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() ); //wegen Connection closed
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    csch1.close();
  }
  
  public void testReconnectSlave() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
   
    System.out.println( "########################################## prepareClusterForReconnect fertig ############");
    
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    //restore 1
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //restore 2
    csch1.setReadyForConnected(false);
    cp2.restoreClusterPrepare(2);
    cp2.restoreClusterConnect();
    //csch1.expectClusterStateChange(ClusterState.CONNECTED);
    cp2.setClusterStateChangeHandler(csch2);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.STARTING, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.STARTING,true) );
    
   
    try {
      Thread.sleep(3000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    
    //connect 1
    csch1.setReadyForConnected(true);
    cp1.readyForStateChange();
    csch2.expectClusterStateChange(ClusterState.CONNECTED, "remote-connect");
    csch1.expectClusterStateChange(ClusterState.CONNECTED, "connect");
    
    
    assertEquals(ClusterState.CONNECTED, cp1.getState() );
    assertEquals(ClusterState.CONNECTED, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.CONNECTED,true) );
    assertEquals("", checkDB(2,ClusterState.CONNECTED,true) );
    
    csch1.close();
    csch2.close();
  }
  
  public void testReconnectMaster() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
   
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    //restore 2
    cp2.restoreClusterPrepare(2);
    cp2.setClusterStateChangeHandler(csch2);
    cp2.restoreClusterConnect();
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "restoreClusterConnect");
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    //restore 1
    csch2.setReadyForConnected(false);
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.STARTING, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.STARTING,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    //connect 2
    csch2.setReadyForConnected(true);
    cp2.readyForStateChange();
    csch2.expectClusterStateChange(ClusterState.CONNECTED, "connect", 100 );
    csch1.expectClusterStateChange(ClusterState.CONNECTED, "remote-connect");
    
    assertEquals(ClusterState.CONNECTED, cp1.getState() );
    assertEquals(ClusterState.CONNECTED, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.CONNECTED,true) );
    assertEquals("", checkDB(2,ClusterState.CONNECTED,true) );
    
    csch1.close();
    csch2.close();
  }
  
  public void testReconnectSimultaneousShutdown() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
   
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    //restore 1
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //restore 2
    csch1.setReadyForConnected(false);
    cp2.restoreClusterPrepare(2);
    cp2.restoreClusterConnect();
    //csch1.expectClusterStateChange(ClusterState.CONNECTED);
    cp2.setClusterStateChangeHandler(csch2);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.STARTING, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.STARTING,true) );
    
    //disconnect 2
    cp2.disconnect();
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "disconnect");
    //assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    //assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
  
    //connect 1
    //FIXME wenn  csch1.setReadyForConnected(true);
    cp1.readyForStateChange();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "remote-disconnect", 20);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
   
    //FIXME dann nötig csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "connect");
    csch1.close();
    csch2.close();
  }
  
  public void testReconnectWithCrashWhileWaiting() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException, SQLException {
    prepareClusterForReconnect();
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    //restore 1
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //restore 2
    csch1.setReadyForConnected(false);
    cp2.restoreClusterPrepare(2);
    cp2.restoreClusterConnect();
    cp2.setClusterStateChangeHandler(csch2);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.STARTING, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.STARTING,true) );
  
    try {
      Thread.sleep(5000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    //crash 1
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.setCanConnect(false);
    
    assertEquals(ClusterState.STARTING, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.STARTING,true) );
    
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run", 2000);
    //csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-waitForReadyToConnect", 200);
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState", 2000);
        
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    csch1.close();
    csch2.close();
    
    cp1.setCanConnect(true);
  }
  
  public void testReconnectWithDisconnectBeforeConnect() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException, SQLException {
    prepareClusterForReconnect();

    cp1 = new TestableOracleRACClusterProvider("1");
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
      
    //restore 1
    cp1.restoreClusterPrepare(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.STARTING, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.STARTING,true) );
   
    //disconnect
    cp1.changeClusterState(ClusterState.DISCONNECTED);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState" );
    
    cp1.restoreClusterConnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "restoreClusterConnect" );
    
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    csch1.close();
  }
  
  public void testReconnectWithCrashedOtherNode() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException, SQLException {
    prepareClusterForReconnect();

    cp1 = new TestableOracleRACClusterProvider("1");
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    
    prepareDB(2,ClusterState.DISCONNECTED_MASTER,true);
    sqlUtils.commit();
    
    //restore 1
    cp1.restoreClusterPrepare(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    cp1.restoreClusterConnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "restoreClusterConnect" );
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,true) );

  }
  
  public void testChangeStateToSlave() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    cp1.changeClusterState(ClusterState.DISCONNECTED_SLAVE);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "changeClusterState");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "remote-disconnect");
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    csch1.close();
    csch2.close();
  }
  
  public void testChangeStateToMaster() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    cp1.changeClusterState(ClusterState.DISCONNECTED_MASTER);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "remote-disconnect");
        
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,true) );
   
    csch1.close();
    csch2.close();
  }
  
  public void testChangeStateToDisconnected() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    cp1.changeClusterState(ClusterState.DISCONNECTED);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "remote-disconnect");
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,true) );
    
    csch1.close();
    csch2.close();
  }
  
  public void testChangeStateToDisconnectedInterconnectDown() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    cp1.shutdownInterconnect();
    cp2.shutdownInterconnect();
    
    cp1.changeClusterState(ClusterState.DISCONNECTED);
    cp2.changeClusterState(ClusterState.DISCONNECTED);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "changeClusterState");
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,true) );
    
    csch1.close();
    csch2.close();
  }

  
  public void testChangeStateToDisconnectedAfterConnectionLost() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException, SQLException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    //Connections kaputt und nicht wieder startbar
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.setCanConnect(false);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run",2000);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run",2000);
    
    //Change ClusterState 1
    cp1.changeClusterState(ClusterState.DISCONNECTED);
    //csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-changeClusterState");
    
    //csch2.expectClusterStateChange(ClusterState.CONNECTED);
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    //csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-getStateInternal", 2000);
    assertEquals(ClusterState.CONNECTED, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.CONNECTED,true) );
    assertEquals("", checkDB(2,ClusterState.CONNECTED,true) );
    
    //Change ClusterState 2
    cp2.changeClusterState(ClusterState.DISCONNECTED);
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "changeClusterState");
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    csch1.close();
    csch2.close();
    
    cp1.setCanConnect(true);
  }
  

  
  public void testReconnectQueueFails() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
   
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //cp1 wird nicht mehr antworten
    cp1.shutdownInterconnect();
    
    System.out.println( "nun Test ");
    
    cp2.restoreClusterPrepare(2);
    cp2.restoreClusterConnect();
    //csch1.expectClusterStateChange(ClusterState.CONNECTED); //wird nun nicht mehr gerufen
    cp2.setClusterStateChangeHandler(csch2);
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
  }

  
  public void testReconnectSingleNoConnection() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException {
    prepareSingleForReconnect();
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.setCanConnect(false);
    
    try {
      cp1.restoreClusterPrepare(1);
      cp1.restoreClusterConnect();
      fail( "Exception expected");
    } catch( XFMG_ClusterInitializationException e ) {
      
    }
  }
  
  
  public void testSingleConnectionClosed() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException {
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
  
    //getState erholt sich, da neue Connections aufgemacht werden können
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close(); //hier werden auch interne Connections geschlossen  -> Geschlossene Anweisung/Closed statement
    }
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    
    //Disconnect erholt sich, da neue Connections aufgemacht werden können
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.SINGLE, "disconnect");
    
    csch1.close();
  }
  
  public void testSingleConnectionClosedAndReopenFails() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException {
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.SINGLE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
  
    //getState liefert Fehler, da Connections kaputt und nicht neu holbar
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.setCanConnect(false);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run",2000);
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.SINGLE,true) );
    //csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run");
    
    //Disconnect scheitert ebenfalls -> DISCONNECTED_SLAVE
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run"); //über disconnect
    
    csch1.close();
    cp1.setCanConnect(true); //damit tearDown besser klappt
  }
  
 
  public void testReconnectClusterNoConnection() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.setCanConnect(false);
    
    try {
      cp1.restoreClusterPrepare(1);
      cp1.restoreClusterConnect();
      fail( "Exception expected");
    } catch( XFMG_ClusterInitializationException e ) {
      
    }
  }

  public void testClusterOneConnectionClosed() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //getState erholt sich, da neue Connections aufgemacht werden können
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
      System.err.println( "close TestConnection");
    }
    /*
    System.out.println( cp1.getState() );
    
    try {
      Thread.sleep(600000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }*/
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    
    
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //Disconnect erholt sich, da neue Connections aufgemacht werden können
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "disconnect", 40);
    
    csch1.close();
  }

  public void testClusterOneConnectionClosedReopenFails() throws XFMG_ClusterInitializationException, XFMG_InvalidStartParametersForClusterProviderException, SQLException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareClusterForReconnect();
    
    cp1 = new TestableOracleRACClusterProvider("1");
    cp1.restoreClusterPrepare(1);
    cp1.restoreClusterConnect();
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
 
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    
    //getState liefert Fehler, da Connections kaputt und nicht neu holbar
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    cp1.setCanConnect(false);
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run", 2000);
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_MASTER,true) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_SLAVE,false) );
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "dbNotReachable-run", 1000); //über getState
    
    //Disconnect scheitert ebenfalls -> DISCONNECTED_SLAVE
    cp1.disconnect();
    //csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "disconnect"); //über disconnect
        
    csch1.close();
    cp1.setCanConnect(true); //damit tearDown besser klappt
  }

  
  public void testParallelGetState() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, SQLException  {
    cp1 = new TestableOracleRACClusterProvider("1");
    
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), "10000" });
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    assertEquals(ClusterState.SINGLE, cp1.getState() );
  
    //Connections kaputt und nicht wieder startbar
    for( TestConnection tc : cp1.getTestConnections() ) {
      tc.close();
    }
    //cp1.setCanConnect(false);
   
    
    Thread[] threads = new Thread[10];
    for( int i=0; i<threads.length; ++i ) {
      threads[i] = new Thread("thread-"+i){
        public void run() {
          long start = System.currentTimeMillis();
          ClusterState state = cp1.getState();
          long end = System.currentTimeMillis();
          System.out.println( getName() +" "+state +" "+ (end-start)+" ms");
        };
      };
    }
    for( int i=0; i<threads.length; ++i ) {
      threads[i].start();
    }
    
    cp1.setCanConnect(true);
  }
    
  public void testSecondDisconnect() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    ClusterStateChangeHandlerImpl csch2 = new ClusterStateChangeHandlerImpl(2);
    
    cp1.setClusterStateChangeHandler(csch1);
    cp2.setClusterStateChangeHandler(csch2);
    
    cp1.disconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "disconnect");
    csch2.expectClusterStateChange(ClusterState.DISCONNECTED_MASTER, "remote-disconnect");
    
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );
    
    cp1.disconnect();
    assertEquals(ClusterState.DISCONNECTED_SLAVE, cp1.getState() );
    assertEquals(ClusterState.DISCONNECTED_MASTER, cp2.getState() );
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,true) );

    
    csch1.close();
    csch2.close();
  }

  
  public void testCheckInterconnect() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException, SQLException {
    RemoteInterfaceForClusterStateChangesImplAQ.DEQUEUE_TIMEOUT = 5;
    prepareCluster();
    System.out.println( "\n\n\n\n\nStart");
    
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    
    //Connections kaputt und nicht wieder startbar
    for( TestConnection tc : cp1.getTestConnections() ) {
      final TestConnection ftc = tc; 
      new Thread(){ 
        public void run(){
          try {
            ftc.close();
          } catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }.start();
    }
    cp1.setCanConnect(false);
//    
// 
    cp1.checkInterconnect();
    csch1.expectClusterStateChange(ClusterState.DISCONNECTED_SLAVE, "checkInterconnect",50);

    
    System.out.println( "Ende\n\n\n\n\n");
    csch1.close();
    cp1.setCanConnect(true);
  }
  

  
  
  
  
  
  
  
  /**
   * @throws XFMG_ClusterInitializationException 
   * @throws XFMG_InvalidStartParametersForClusterProviderException 
   * @throws XFMG_ClusterConnectionException 
   * @throws XFMG_InvalidConnectionParametersForClusterProviderException 
   * 
   */
  private void prepareCluster() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    cp1 = new TestableOracleRACClusterProvider("1");
    cp2 = new TestableOracleRACClusterProvider("2");
    
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), TIMEOUT });
    
    ClusterStateChangeHandlerImpl csch1 = new ClusterStateChangeHandlerImpl(1);
    cp1.setClusterStateChangeHandler(csch1);
    csch1.setReadyForConnected(true);
    cp2.joinCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), TIMEOUT });
     
    csch1.expectClusterStateChange(ClusterState.CONNECTED, "connect", 50); //nötig, um kurz zu warten
    //FIXME
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    //assertEquals("", checkDB(1,ClusterState.CONNECTED,true) );
    //assertEquals("", checkDB(2,ClusterState.CONNECTED,true) );
    
    csch1.close();
  } 
  
 
  /**
   * @throws XFMG_ClusterInitializationException 
   * @throws XFMG_InvalidStartParametersForClusterProviderException 
   * @throws XFMG_ClusterConnectionException 
   * @throws XFMG_InvalidConnectionParametersForClusterProviderException 
   * 
   */
  private void prepareClusterForReconnect() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException, XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    prepareCluster();
    
    cp1.disconnect();
    cp2.disconnect();
    
    assertEquals("", checkDB(1,ClusterState.DISCONNECTED_SLAVE,false) );
    assertEquals("", checkDB(2,ClusterState.DISCONNECTED_MASTER,false) );
    
    cp1 = null;
    cp2 = null;
  }
  
  private void prepareSingleForReconnect() throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException {
    cp1 = new TestableOracleRACClusterProvider("1");
    
    cp1.createCluster(new String[]{dbd.getUser(), dbd.getPassword(), dbd.getUrl(), TIMEOUT });
    
    cp1.disconnect();
    
    assertEquals("", checkDB(1,ClusterState.SINGLE,false) );
  }

  
  

  public void notestCancel() {
    
    //String sql = PLSQLBuilder.buildDequeueBlock();
    String sql = "{call blackjunit.dequeue(?,?,?,?,?,?)}";
    
    //logger.debug( sql );
    CallableStatement cs1 = null;
    try {
      cs1 = sqlUtils.prepareCall(sql);
    }
    catch (SQLException e1) {
      e1.printStackTrace();
    }
    
    final CallableStatement cs = cs1;
    
    new Thread( new Runnable() {
      public void run() {
        try {
          Thread.sleep(2000);
          try {
            logger.debug("cancel");
            cs.cancel();
          }
          catch (SQLException e) {
            e.printStackTrace();
          }
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }} ).start();
    
    
    try {
      logger.debug( "start" );
      getNextMessage(sqlUtils, 400, "", cs);
    }
    catch (SQLException e) {
      logger.debug("getNextMessage : "+e.getMessage());
    }
    
    logger.debug( "getNextMessage fertig" );
  }
  

  /**
   * @param i
   * @param string
   * @param j
   * @return
   */
  private String checkDB(int binding, ClusterState clusterState, boolean isOnline) {
    String[] res = sqlUtils.queryOneRow("select state, isOnline from xynaClusterSetup where binding=?", new Parameter(binding), ResultSetReaderFactory.getStringArrayReader(2) );
    if( res == null ) {
      return "binding "+binding+" not found";
    }
    if( res[0].equals(clusterState.toString()) && res[1].equals(isOnline?"1":"0") ) {
      return "";
    }
    return "state="+res[0]+", isOnline="+res[1].equals("1");
  }

  
  
  
  
  
  private String getNextMessage(SQLUtils sqlUtils, int timeout, String dequeueCondition, CallableStatement cs) throws SQLException {
    
    
    cs.setString(1, "" + timeout);
    cs.setString(2, dequeueCondition);
    cs.setString(3, "blackjunit.interconnectQ" );

    cs.registerOutParameter(4, java.sql.Types.VARCHAR);
    cs.registerOutParameter(5, java.sql.Types.VARCHAR);
    cs.registerOutParameter(6, java.sql.Types.VARCHAR);

    try {
      sqlUtils.excuteUpdate(cs);
    } catch (SQLException e) {
      
      throw e;
    }

    String corrId = cs.getString(4);
    int priority = cs.getInt(5);
    String text = cs.getString(6);
    sqlUtils.commit();
    return corrId;
  }

  
  
  public void notestConnectionClose() throws InterruptedException {
    String insert = "insert into xynaClusterSetup (binding,state,isonline) values(?,?,?)";
    sqlUtils.executeDML(insert, new Parameter(1,"SINGLE",0) );
    sqlUtils.commit();
    
    
    SQLUtils sqlUtils1 = dbd.createSQLUtils(new LocalSQLUtilsLogger() );
    final SQLUtils sqlUtils2 = dbd.createSQLUtils(new LocalSQLUtilsLogger() );
    System.err.println("SID 1 = "+ sqlUtils1.queryOneRow("SELECT sys_context('userenv', 'sid') FROM Dual",null, ResultSetReaderFactory.getStringReader() ) );
    System.err.println("SID 2 = "+ sqlUtils2.queryOneRow("SELECT sys_context('userenv', 'sid') FROM Dual",null, ResultSetReaderFactory.getStringReader() ) );
    
    
    System.err.println( sqlUtils1.hashCode() + " " + sqlUtils2.hashCode() );
    
    System.err.println("lock 1");
    String state = sqlUtils1.queryOneRow("select state from xynaClusterSetup where binding=? for update", 
      new Parameter(1), ResultSetReaderFactory.getStringReader() );
    System.err.println("got lock 1 -> state="+state);
    
    new Thread() {
      public void run() {
        System.err.println("lock 2");
        sqlUtils2.queryOneRow("select state from xynaClusterSetup where binding=? for update", 
          new Parameter(1), ResultSetReaderFactory.getStringReader() );
        System.err.println("got lock 2");
        
        
      };
    }.start();
    
    System.err.println("sleep");
    Thread.sleep(2000);
    
    new Thread() {
      public void run() {
        System.err.println("close 2");
        sqlUtils2.closeConnection();
        System.err.println("close 2 finished");
        
        
      };
    }.start();
   
    System.err.println("sleep");
    Thread.sleep(2000);
   
    System.err.println("close 1");
    sqlUtils1.closeConnection();
    System.err.println("close 1 finished");
    
    
  }
  

  
  
}
