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

package com.gip.xyna.idgeneration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.FutureExecution.FutureExecutionTaskBuilder;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithm;
import com.gip.xyna.xmcp.xfcli.undisclosed.KillThread;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.VetoManagement;


/**
  *
 */
public class IdGeneratorTest extends TestCase {
  
  private static Thread thread;
  
  public void setUp() throws Exception {
    
    
    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();
    EasyMock.expect(fexec.addTask(EasyMock.anyObject(), EasyMock.isA(String.class))).andReturn(new FutureExecutionTaskBuilder(fexec, new Object(), "")).anyTimes();
    fexec.execAsync(EasyMock.isA(FutureExecutionTask.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(fexec);
    
    
    DependencyRegister depReg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(
                    depReg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock
                                    .isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true)
                    .anyTimes();
    EasyMock.replay(depReg);
    
    InfrastructureAlgorithmExecutionManagement iaem = new InfrastructureAlgorithmExecutionManagement() {
      
      private ManagedAlgorithm testAlgo;
      
      @Override
      public boolean registerAlgorithm(ManagedAlgorithm algo) {
        testAlgo = algo;
        return true;
      }
      
      @Override
      public AlgorithmStateChangeResult startAlgorithm(String name) {
        testAlgo.start(Collections.emptyMap(), new OutputStream() { @Override public void write(int b) throws IOException {} });
        System.out.println("Starting logging thread");
        thread = new Thread(new Runnable() {
          
          public void run() {
            while (true) {
              try {
                System.out.println("Running");
                Thread.sleep(10000l);
                Map<String, Object> params = new HashMap<>();
                params.put(IdGenerationAlgorithmUsingBlocksAndClusteredStorable.actionParameter.getName(), IdGenerationAlgorithmUsingBlocksAndClusteredStorable.IDGENERATION_RESTART_OPTIONS.INFO);
                testAlgo.start(params, System.out);
                
                ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
                long[] ids = tbean.getAllThreadIds();
                StringBuilder sb = new StringBuilder();
                ThreadInfo[] tis = tbean.getThreadInfo(ids, 200);
                for (ThreadInfo ti : tis) {
                  if (ti != null) { //null if thread is not active any more
                    if (tbean.isThreadCpuTimeEnabled() || tbean.isThreadContentionMonitoringEnabled()) {
                      sb.append("  Thread.");
                      if (tbean.isThreadCpuTimeEnabled()) {
                        sb.append("cputime=").append(tbean.getThreadCpuTime(ti.getThreadId())/1000000);
                        sb.append("ms\n   .usertime=").append(tbean.getThreadUserTime(ti.getThreadId())/1000000);
                        sb.append("ms\n   ");
                      }
                      if (tbean.isThreadContentionMonitoringEnabled()) {
                        sb.append(".blocked=").append(ti.getBlockedTime());
                        sb.append("ms\n   .waited=").append(ti.getWaitedTime());
                        sb.append("ms\n");
                      }
                    }
                    sb.append(getThreadInfo(ti));
                  }
                }
                System.out.println(sb);
              } catch (InterruptedException e) {
                
              }
            }
          }
        });
        thread.start();
        return super.startAlgorithm(name);
      }
    };
    
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getDependencyRegister()).andReturn(depReg).anyTimes();
    EasyMock.expect(xfctrl.getInfrastructureAlgorithmExecutionManagement()).andReturn(iaem).anyTimes();
    EasyMock.replay(xfctrl);

    final ClusterProvider cpNode1 = EasyMock.createMock(ClusterProvider.class);
    EasyMock.expect(cpNode1.getLocalBinding()).andReturn(1).anyTimes();
    EasyMock.replay(cpNode1);
    final ClusterProvider cpNode2 = EasyMock.createMock(ClusterProvider.class);
    EasyMock.expect(cpNode2.getLocalBinding()).andReturn(2).anyTimes();
    EasyMock.replay(cpNode2);
    
    
    
    XynaClusteringServicesManagement xcsm = EasyMock.createMock(XynaClusteringServicesManagement.class);
    EasyMock.expect(xcsm.getClusterInstance(EasyMock.anyInt())).
             andAnswer(new IAnswer<ClusterProvider>() {
      public ClusterProvider answer() throws Throwable {
        System.err.println( "getClusterInstance("+EasyMock.getCurrentArguments()[0]+")");
        return EasyMock.getCurrentArguments()[0].toString().equals("1") ? cpNode1 : cpNode2;
        }   
    }).anyTimes();
    EasyMock.replay(xcsm);
    
    
    XynaFactoryManagement xfm = EasyMock.createMock(XynaFactoryManagement.class);
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("false").anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    EasyMock.expect(xfm.getXynaClusteringServicesManagement()).andReturn(xcsm).anyTimes();
    EasyMock.replay(xfm);
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    xf.addComponentToBeInitializedLater(EasyMock.isA(VetoManagement.class));
    EasyMock.expectLastCall();
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.replay(xf);
    
    XynaFactory.setInstance(xf);
    
    

    ODS ods = ODSImpl.getInstance();
    //ods.registerPersistenceLayer(42, ClusteredXynaLocalMemoryPersistenceLayer.class);
    ods.registerPersistenceLayer(42, XynaLocalMemoryPersistenceLayer.class);
    // long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
    //                                                  ODSConnectionType.DEFAULT, new String[0]);
    //long id2 = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test2",
    //                                                  ODSConnectionType.HISTORY, new String[0]);
    long id = ods.instantiatePersistenceLayerInstance(42, "test",
      ODSConnectionType.DEFAULT, new String[0]);
    long id2 = ods.instantiatePersistenceLayerInstance(42, "test2",
      ODSConnectionType.HISTORY, new String[0]);
    long id3 = ods.instantiatePersistenceLayerInstance(42, "test3",
      ODSConnectionType.ALTERNATIVE, new String[0]);
    long id4 = ods.instantiatePersistenceLayerInstance(42, "test4",
      ODSConnectionType.INTERNALLY_USED, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id2);
    ods.setDefaultPersistenceLayer(ODSConnectionType.ALTERNATIVE, id3);
    ods.setDefaultPersistenceLayer(ODSConnectionType.INTERNALLY_USED, id4);
    
    XynaProcessingODS xpods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xpods.getODS()).andReturn(ods).anyTimes();
    EasyMock.replay(xpods);

    initIdGen();
    
    ClusteredXynaLocalMemoryPersistenceLayer.isClustered = false;
  }
  
  
  private static String getThreadInfo(ThreadInfo ti) {
    //FIXME code aus java6 ThreadInfo.toString() geklaut. in java5 ist toString() sehr dürftig.
    //leider ist in java6 toString die größe des ausgegebenen stacks auf 8 begrenzt.
    StringBuilder sb =
        new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " " + ti.getThreadState());
    if (ti.getLockName() != null) {
      sb.append(" on " + ti.getLockName());
    }
    if (ti.getLockOwnerName() != null) {
      sb.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
    }
    if (ti.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (ti.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    int i = 0;
    StackTraceElement[] stackTrace = ti.getStackTrace();
    for (; i < stackTrace.length && i < 200; i++) {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && ti.getLockName() != null) {
        Thread.State ts = ti.getThreadState();
        switch (ts) {
          case BLOCKED :
            sb.append("\t-  blocked on " + ti.getLockName());
            sb.append('\n');
            break;
          case WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          case TIMED_WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          default :
        }
      }
    }
    if (i < stackTrace.length) {
      sb.append("\t...");
      sb.append('\n');
    }

    sb.append('\n');
    return sb.toString();
  } 
  
  private static void initIdGen() throws Exception {
    IDGenerator idGen = IDGenerator.getInstance();
    Method initStorableMethod = IDGenerator.class.getDeclaredMethod("initStorable");
    initStorableMethod.setAccessible(true);
    initStorableMethod.invoke(idGen);
  }
  
  public static class ClusteredXynaLocalMemoryPersistenceLayer extends XynaLocalMemoryPersistenceLayer implements Clustered {

    public static boolean isClustered = true;
    public static int clusterInstanceId = 1;
    
    public ClusteredXynaLocalMemoryPersistenceLayer() throws PersistenceLayerException {
      super();
    }

    public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
                    XFMG_ClusterComponentConfigurationException {
    }

    public long getClusterInstanceId() {
      return clusterInstanceId;
    }

    public String getName() {
      return "halli-hallo";
    }

    public boolean isClustered() {
      return isClustered;
    }

    public Reader getExtendedInformation(String[] args) {
      return null;
    }

    public void disableClustering() {
    }
    
  }
  
  
  
  public void tearDown() {
    thread.stop();
    
    IDGenerator.setInstance(null);
    
    ODSImpl ods = ODSImpl.getInstance();
    try {
      ods.registerStorable(GeneratedIDsStorable.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      Collection<GeneratedIDsStorable> gidss = con.loadCollection(GeneratedIDsStorable.class);
      con.delete(gidss);
      con.commit();
      con.closeConnection();
      
      ods.removePersistenceLayerInstance(ods.getDefaultPersistenceLayerInstance(ODSConnectionType.DEFAULT).getPersistenceLayerID());
      ods.removePersistenceLayerInstance(ods.getDefaultPersistenceLayerInstance(ODSConnectionType.HISTORY).getPersistenceLayerID());
      ods.removePersistenceLayerInstance(ods.getDefaultPersistenceLayerInstance(ODSConnectionType.ALTERNATIVE).getPersistenceLayerID());
      ods.removePersistenceLayerInstance(ods.getDefaultPersistenceLayerInstance(ODSConnectionType.INTERNALLY_USED).getPersistenceLayerID());
      
      ods.unregisterPersistenceLayer(42);
      
      ODSImpl.clearInstances();
    }
    catch (PersistenceLayerException e) {
      e.printStackTrace();
    } catch (XNWH_PersistenceLayerMayNotBeUndeployedInUseException e) {
      e.printStackTrace();
    }
  
    setXynaFactoryClusterNodeName(null);
  }
  
  
  private void setIDGeneratorID_OFFSET(int value) {
    try { 
      Field field = IDGenerator.class.getDeclaredField("ID_OFFSET");
      field.setAccessible(true);
      field.setInt(null, value);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void setXynaFactoryClusterNodeName(String value) {
    try { 
      Field field = XynaFactory.class.getDeclaredField("clusterNodeName");
      field.setAccessible(true);
      field.set(null, value);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<String> readGeneratedIDsStorables() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();
    ods.registerStorable(GeneratedIDsStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);

    Collection<GeneratedIDsStorable> gidss = con.loadCollection(GeneratedIDsStorable.class);
    
    ArrayList<String> res = new ArrayList<String>();
    for( GeneratedIDsStorable gids : gidss ) {
      res.add( gids.getPrimaryKey()+ " " + gids.getBinding()+ " " + gids.getLastStoredId()+ " " + gids.isResultingFromShutdown() );
    }
    return res;
  }
  

  private void assertEqualsStringList(String string, List<String> stringList ) {
    assertEquals( 1, stringList.size() );
    assertEquals( string, stringList.get(0) );
  }
  
  private void assertEqualsStringList(String[] strings, List<String> stringList ) {
    assertEquals( strings.length, stringList.size() );
    for( int i=0; i<strings.length; ++i ) {
      assertEquals( strings[i], stringList.get(i) );
    }
  }

  
  
  
  public void testIDGenerator() throws XynaException {
    IDGenerator id = IDGenerator.getInstance();
    
    //assertEquals( 0, id.getUniqueId() );
    assertEquals( 1, id.getUniqueId() );
    assertEquals( 2, id.getUniqueId() );
    assertEquals( 3, id.getUniqueId() );
    
    System.out.println( id.idGenerationAlgorithm );
    
    assertEqualsStringList( "XynaFactory 0 1 false", readGeneratedIDsStorables() );
  }

  public void testIDGeneratorShutdownAndRestart() throws XynaException {
    IDGenerator id = IDGenerator.getInstance();
    
    assertEquals( 1, id.getUniqueId() );
    assertEquals( 2, id.getUniqueId() );
    assertEquals( 3, id.getUniqueId() );
    id.shutdown();
    
    assertEqualsStringList( "XynaFactory 0 3 true", readGeneratedIDsStorables() );
    
    try {
      id.getUniqueId();
    } catch( Exception e ) {
      //System.err.println( e.getMessage() );
      assertEquals("com.gip.xyna.xprc.exceptions.XPRC_FACTORY_IS_SHUTTING_DOWN: Didn't find errormessage corresponding to code XYNA-01410. Parameters were: [Generate unique ID].",e.getMessage() );
    }
    
    IDGenerator.setInstance(null);
    
    id = IDGenerator.getInstance();
    assertEquals( 4, id.getUniqueId() );
    assertEquals( 5, id.getUniqueId() );
   
    assertEqualsStringList( "XynaFactory 0 4 false", readGeneratedIDsStorables() );
    
  }
  
  public void testIDGeneratorCrashAndRestart() throws Exception {
    setIDGeneratorID_OFFSET(10);
    IDGenerator id = IDGenerator.getInstance();
    
    assertEquals( 1, id.getUniqueId() );
    assertEquals( 2, id.getUniqueId() );
    assertEquals( 3, id.getUniqueId() );
      
    IDGenerator.setInstance(null);
    
    id = IDGenerator.getInstance();
    initIdGen();
    assertEquals( 11, id.getUniqueId() );
    assertEquals( 12, id.getUniqueId() );
   
    assertEqualsStringList( "XynaFactory 0 11 false", readGeneratedIDsStorables() );
    
  }
  
  public void testIDGeneratorSmallOffset() throws XynaException {
    setIDGeneratorID_OFFSET(5);
    IDGenerator id = IDGenerator.getInstance();
    
    //assertEquals( 0, id.getUniqueId() );
    assertEquals( 1, id.getUniqueId() );
    assertEquals( 2, id.getUniqueId() );
    assertEquals( 3, id.getUniqueId() );
    assertEquals( 4, id.getUniqueId() );
    assertEquals( 5, id.getUniqueId() );
    assertEqualsStringList( "XynaFactory 0 1 false", readGeneratedIDsStorables() );
    assertEquals( 6, id.getUniqueId() );
    assertEqualsStringList( "XynaFactory 0 6 false", readGeneratedIDsStorables() );
    assertEquals( 7, id.getUniqueId() );
    assertEquals( 8, id.getUniqueId() );
    assertEquals( 9, id.getUniqueId() );

    assertEqualsStringList( "XynaFactory 0 6 false", readGeneratedIDsStorables() );
    
  }


  public void testIDGeneratorCluster() throws XynaException {
    
    ClusteredXynaLocalMemoryPersistenceLayer.isClustered = true;

    
    assertEquals( null , XynaFactory.getClusterNodeName() );
    setXynaFactoryClusterNodeName("eins");
    assertEquals( "eins" , XynaFactory.getClusterNodeName() );
    
    
    IDGenerator id = IDGenerator.getInstance();
    
    assertEquals( 1, id.getUniqueId() );
    assertEquals( 2, id.getUniqueId() );
    assertEquals( 3, id.getUniqueId() );
    
    assertEqualsStringList( "XynaCluster_1 1 1 false", readGeneratedIDsStorables() );
  }

  public void testIDGeneratorCluster2Nodes() throws XynaException {
    ClusteredXynaLocalMemoryPersistenceLayer.isClustered = true;
    setIDGeneratorID_OFFSET(5);
    
    ClusteredXynaLocalMemoryPersistenceLayer.clusterInstanceId = 1;
    IDGenerator id1 = IDGenerator.getInstance();
    
    assertEqualsStringList( "XynaCluster_1 1 1 false", readGeneratedIDsStorables() );

    IDGenerator.setInstance(null);
    
    ClusteredXynaLocalMemoryPersistenceLayer.clusterInstanceId = 2;
    IDGenerator id2 = IDGenerator.getInstance();
    
    assertEqualsStringList( new String[]{ "XynaCluster_1 1 1 false", "XynaCluster_2 2 6 false" }, readGeneratedIDsStorables() );

    
    assertEquals( 1, id1.getUniqueId() );
    assertEquals( 2, id1.getUniqueId() );
    assertEquals( 3, id1.getUniqueId() );
    
    assertEquals( 6, id2.getUniqueId() );
    assertEquals( 7, id2.getUniqueId() );
    assertEquals( 8, id2.getUniqueId() );
     
    assertEqualsStringList( new String[]{ "XynaCluster_1 1 1 false", "XynaCluster_2 2 6 false"  }, readGeneratedIDsStorables() );

    assertEquals( 4, id1.getUniqueId() );
    assertEquals( 5, id1.getUniqueId() );
    
    System.err.println( id1.idGenerationAlgorithm );
    System.err.println( id2.idGenerationAlgorithm );
    
    assertEqualsStringList( new String[]{ "XynaCluster_1 1 11 false", "XynaCluster_2 2 6 false" }, readGeneratedIDsStorables() );

    assertEquals( 11, id1.getUniqueId() );
  
  
  
  }

  
  public void testParallelIDs() throws XynaException, InterruptedException, ExecutionException {
    int SIZE = 100000;
    String realm = "network";
    
    IDGenerator idGen = IDGenerator.getInstance();
    CountDownLatch latch = new CountDownLatch(1);
    
    ExecutorService exec = Executors.newFixedThreadPool(20);
    
    List<Future<Long>> futureIds = new ArrayList<>();
    for (int i = 0; i < SIZE; i++) {
      futureIds.add(exec.submit(new IDGetter(idGen, realm, latch)));
    }
    
    latch.countDown();
    
    Set<Long> ids = new HashSet<>();
    for (Future<Long> futureId : futureIds) {
      if (!ids.add(futureId.get())) {
        throw new RuntimeException("Duplicate ID " + futureId.get() +  " | " + ids);
      }
    }
  }
  
  
  public void testOverflowIDs() throws XynaException, InterruptedException, ExecutionException {
    int SIZE = 3000000; 
    String realm = "network";
    
    IDGenerator idGen = IDGenerator.getInstance();
    CountDownLatch latch = new CountDownLatch(1);
    
    ExecutorService exec = Executors.newFixedThreadPool(30);
    
    List<Future<Long>> futureIds = new ArrayList<>();
    for (int i = 0; i < SIZE; i++) {
      futureIds.add(exec.submit(new IDGetter(idGen, realm, latch)));
    }
    
    latch.countDown();
    
    Set<Long> ids = new HashSet<>();
    for (Future<Long> futureId : futureIds) {
      if (!ids.add(futureId.get())) {
        fail("Duplicate ID " + futureId.get() +  " | " + ids);
      }
    }
    
    assertEquals(SIZE, ids.size());
  }
  
  
  private static class IDGetter implements Callable<Long> {
    IDGenerator idGen;
    String realm;
    CountDownLatch latch;
    IDGetter(IDGenerator idGen, String realm, CountDownLatch latch) {
      this.idGen = idGen;
      this.realm = realm;
      this.latch = latch;
    }
    public Long call() throws Exception {
      latch.await();
      return idGen.getUniqueId(realm);
    }
    
  }
  

}
