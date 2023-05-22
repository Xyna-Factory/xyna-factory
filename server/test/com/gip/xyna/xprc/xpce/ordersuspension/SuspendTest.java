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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.gip.xyna.utils.misc.IndentableStringBuilder;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.DebugConcurrency;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Synchronization;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Await;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Increment;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.LogLaneId;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.ParallelStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.ResponseListener;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Serial;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.SetBool;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Sleep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.SubworkflowStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.WFStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Wait;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Workflow;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;

import junit.framework.TestCase;


/**
  *
 */
public class SuspendTest extends TestCase {
  
  private static Logger logger = Logger.getLogger(SuspendTest.class);
  private static final boolean LOG_DEBUG = false;
  private static final boolean EXECUTE_LOAD_TESTS = false;
  
  public void setUp() {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
    loggerConfig.setLevel(LOG_DEBUG ? Level.DEBUG : Level.INFO);
    for (String appender : loggerConfig.getAppenders().keySet()) {
      loggerConfig.removeAppender(appender);
    }
    org.apache.logging.log4j.core.layout.PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout.createLayout("%r XYNA %-5p [%t] (%F:%L) - %x %m%n", null, config, null, null, false, false, null, null);
    ConsoleAppender consoleAppender = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "console", null, null);
    loggerConfig.addAppender(consoleAppender, LOG_DEBUG ? Level.DEBUG : Level.INFO, null);
    ctx.updateLoggers();

    SuspendTestFactory.newInstance();
    SuspendTestFactory.getInstance().getSuspendResumeManagement().removeAllOrderBackupLocks();
    SuspendTestScheduler.executor.execute(new Runnable() {

      @Override
      public void run() {
      }
      
    });
  }
  
  public void tearDown() {
  }
  
  private static void assertSynchronization(String expected) {
    assertEquals( "Synchronization", expected, SuspendTestFactory.getInstance().getSynchronization().toString() );
  }
  private static void assertOrders(String expected) {
    assertEquals( "AllOrders", expected, SuspendTestFactory.getInstance().getAllOrders().toString() );
  }
  private static void assertOrders(Object ... map) {
    HashMap<String,String> expected = toMap(map);
    HashMap<String,String> actual = mapToStringMap( SuspendTestFactory.getInstance().getAllOrders() );    
    assertEquals( "AllOrders", expected.toString(), actual.toString());
  }

  private static HashMap<String,String> toMap(Object[] data) {
    HashMap<String,String> map = new HashMap<String,String>();
    for( int i=0; i<data.length; i+=2 ) {
      map.put( String.valueOf(data[i]), String.valueOf(data[i+1]) );
    }
    return map;
  }
  private static HashMap<String,String> mapToStringMap(HashMap<?,?> objectMap) {
    HashMap<String,String> map = new HashMap<String,String>();
    for( Map.Entry<?,?> entry : objectMap.entrySet() ) {
      map.put( String.valueOf(entry.getKey()), String.valueOf(entry.getValue()) );
    }
    return map;
  }

  private static void assertSuspensions(String expected) {
    //FIXME nach einigen Umbauten k�nnen die erwarteten Daten nicht mehr ausgegeben werden!
    //assertEquals( "Suspensions", expected, SuspendTestFactory.getInstance().getSuspendResumeManagement().toString() );
    System.out.println(SuspendTestFactory.getInstance().getSuspendResumeManagement().toString());
  }
  private static void assertAllEmpty() {
    assertSynchronization("{}");
    assertOrders("{}");
    assertSuspensions("{}{}");
    assertEquals(0, SuspendTestFactory.getThreadPool().getActiveCount() );
  }
  
  
  
  
  

  /**
   * 
   */
  public static void test1a_NotifyAfterAwait() {
    TestWorkflow1 wf = new TestWorkflow1();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow1}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "1 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  /**
   * 
   */
  public static void test1b_AwaitAfterNotify() {
    TestWorkflow1 wf = new TestWorkflow1();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    assertSynchronization("{xy=N}");
    assertOrders("{}");
    assertSuspensions("{}{}");
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "1 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  /**
   * 
   */
  public static void test2a_Subworkflow() {
    TestWorkflow2a wf = new TestWorkflow2a();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=2}");
    assertOrders(1, "Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow2a", 2, "Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow2a_TestWorkflow1");
    assertSuspensions("{2=SuspensionEntry(I=2,R=1,P=1), 1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "1 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  /**
   * 
   */
  public static void test2b_SubworkflowInParallel() {
    TestWorkflow2b wf = new TestWorkflow2b();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    waitUntilSynchronizationHasEntries(1, 200, 2);
    waitUntilWorkflowCountIs(2, wf, 200, 2);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=2}");
    assertOrders(1, "Running_TestWorkflow2b", 2,"Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow2b_TestWorkflow1");
    assertSuspensions("{2=SuspensionEntry(I=2,R=1,P=1,L=P1-2)}{1={P1=ParallelExecutor(P1,3T,2F,1E)}#null}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowCountIs(102, wf, 1000, 2); //notify startet Thread und kehrt sofort zur�ck, daher hier kurz warten
    assertEquals( "102 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow2b}");
    assertSuspensions("{2=SuspensionEntry(I=2,R=1,P=1,L=P1-2)}{1={P1=ParallelExecutor(P1,3T,3F,1E)}#[Lane(P1-2,Resumed)]}");
    
    waitUntilSynchronizationHasEntries(0, 1000, 2);
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "103 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
    
  }
 
  /**
   * 
   */
  public static void test3a_AwaitAfterParallel() {
    TestWorkflow3a wf = new TestWorkflow3a();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "3 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow3a}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test3b_AwaitInParallel() {
    TestWorkflow3b wf = new TestWorkflow3b();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-1}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow3b}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test3c_TwoAwaitInParallel() {
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P1-0, xy=1,P1-2}");
    assertOrders("{1=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilSynchronizationHasEntries(1, 200, 2);
    sleep(50);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P1-0}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("ab");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();    
  }
  
  public static void test3d_NoSuspensionInParallel() {
    TestWorkflow3d wf = new TestWorkflow3d();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(10);
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow3d}");
    assertSuspensions("{}{}");
    
    sleep(200);
    assertEquals( "13 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test3e_TwoAwaitInParallel_SimultaneousResume() {
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P1-0, xy=1,P1-2}");
    assertOrders("{1=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    sleep(10);
    SuspendTestFactory.getInstance().getSynchronization().notify("ab");
    
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();    
  }

  
  public static void test4a_AwaitInParallelWithSleep() {
    TestWorkflow4 wf = new TestWorkflow4();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Running_TestWorkflow4}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,2F,1E)}#null}");

    sleep(1500);
    assertEquals( "102 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow4}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "103 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();        
  }
  
  public static void xxtest4b() { //FIXME
    TestWorkflow4 wf = new TestWorkflow4();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Running_TestWorkflow4}");
    assertSuspensions("{}{1-P1=ParallelExecutor(P1,3 tasks)}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilSynchronizationHasEntries(1, 200, 2);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1N}");
    assertOrders("{}");
    assertSuspensions("{}{}");
    
    sleep(1500);
    assertEquals( "3 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1}");
    assertOrders("{1=TestWorkflow4}");
    assertSuspensions("{}{}");
    
    assertEquals( "4 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();        
  }
  
  public static void test5a_ResumeWithLaneId() {
    TestWorkflow5 wf = new TestWorkflow5();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "11 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Running_TestWorkflow5}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,4T,3F,1E)}#null}");

    sleep(500);
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
        
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    sleep(100);
    assertEquals( "112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{1={P1=ParallelExecutor(P1,4T,4F,1E)}#[Lane(P1-2,Resumed)]}");
    
    sleep(500);
    assertEquals( "113 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test5b_ResumeWithoutLaneId() {
    TestWorkflow5 wf = new TestWorkflow5();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(500);
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,4T,3F,1E)}#null}");
    assertEquals( "11 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Running_TestWorkflow5}");

    sleep(500);
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().modifyAwaitForTest("xy", new ResumeTarget(1L, 1L) );
    assertSynchronization("{xy=1}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    sleep(100);
    assertEquals( "112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{1={P1=ParallelExecutor(P1,4T,7F,1E)}#[Lane(all,Resumed)]}");
    
    sleep(500);
    assertEquals( "113 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }

  public static void test5c_ResumeWithoutLaneId_NoNotify() throws PersistenceLayerException {
    TestWorkflow5 wf = new TestWorkflow5();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(500);
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,4T,3F,1E)}#null}");
    assertEquals( "11 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Running_TestWorkflow5}");

    sleep(500);
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    
    SuspendTestFactory.getInstance().getSuspendResumeManagement().resume(new ResumeTarget(1L, 1L));
    
    sleep(100);
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-2}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    sleep(100);
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    sleep(100);
    assertEquals( "112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow5}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{1={P1=ParallelExecutor(P1,4T,7F,1E)}#[Lane(all,Resumed)]}");
    
    sleep(500);
    assertEquals( "113 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }


  public static void test6a_AwaitInParallelInParallel_Suspended() {
    long sleepXY = 500;
    long sleepP12 = 1000;
    long sleepAB = 500;
    TestWorkflow6 wf = new TestWorkflow6(sleepXY,sleepP12,sleepAB);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow6}");
    //assertSuspensions("{}{1={P1=ParallelExecutor(P1,3 tasks,MultiThread(3/3/2))}}");
    assertSuspensions("{}{}");
    
    sleep(500); //sleepXY ist abgelaufen
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Running_TestWorkflow6}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,2F,1E)}#null}");
   
    sleep(500); //sleepP12 ist abgelaufen
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P2-1,P1-2, xy=1,P1-0}");
    assertOrders("{1=Running_TestWorkflow6}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,2F,1E), P2=ParallelExecutor(P2,2T,1F,1E)}#null}");
    
    sleep(600); //sleepAB ist abgelaufen
    assertEquals( "112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P2-1,P1-2, xy=1,P1-0}");
    assertOrders("{1=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow6}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    SuspendTestFactory.getInstance().getSynchronization().notify("ab");
    waitUntilSynchronizationHasEntries(1, 200, 2);
    sleep(50);
    assertEquals( "1112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow6}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "1113 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();        
  }
  
  public static void test6b_AwaitInParallelInParallel_Running() {
    long sleepXY = 1500;
    long sleepP12 = 500;
    long sleepAB = 500;
    TestWorkflow6 wf = new TestWorkflow6(sleepXY,sleepP12,sleepAB);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow6}");
    //assertSuspensions("{}{1={P1=ParallelExecutor(P1,3 tasks,MultiThread(3/3/2))}}"); TODO
    assertSuspensions("{}{}");
     
    sleep(500); //sleepP12 ist abgelaufen
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab=1,P2-1,P1-2}");
    assertOrders("{1=Running_TestWorkflow6}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,1F,2E), P2=ParallelExecutor(P2,2T,1F,1E)}#null}");
    
    SuspendTestFactory.getInstance().getSynchronization().notify("ab");
    waitUntilSynchronizationHasEntries(0, 200, 2); //notify startet Thread und kehrt sofort zur�ck, daher hier kurz warten
    assertEquals( "12 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow6}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,1F,2E), P2=ParallelExecutor(P2,2T,2F,1E)}#[Lane(P2-1,P1-2,Resumed)]}");
    
    sleep(500); //sleepAB ist abgelaufen
    assertEquals( "1112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Running_TestWorkflow6}");
    assertSuspensions("{}{1={P1=ParallelExecutor(P1,3T,2F,1E), P2=ParallelExecutor(P2,2T,3F,0E)}#[Lane(P2-1,P1-2,Resumed)]}");

    sleep(500); //sleepXY ist abgelaufen
    assertEquals( "1112 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{xy=1,P1-0}");
    assertOrders("{1=Suspended(AWAITING_SYNCHRONIZATION)_TestWorkflow6}");
    assertSuspensions("{1=SuspensionEntry(I=1,R=1)}{}");

    SuspendTestFactory.getInstance().getSynchronization().notify("xy");
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "1113 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();        
  }
  
  public static void test7a_LongWaitInSingleLane() {
    TestWorkflow7 wf = new TestWorkflow7(1000,false);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    assertOrders("{1=Suspended(WAIT)_TestWorkflow7}");
    assertSuspensions("{}{}");
    
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "2 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test7b_LongWaitInParallelLane() {
    TestWorkflow7 wf = new TestWorkflow7(1000,true);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(500);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{}");
    sleep(100);
    assertOrders("{1=Suspended(WAIT)_TestWorkflow7}");
    //assertSuspensions("{}{1={P1=ParallelExecutor(P1,3 tasks,MultiThread(3/3/2))}}"); TODO
    assertSuspensions("{}{}");
    
    waitUntilWorkflowIsFinished(wf, 1000, 2);
    assertEquals( "2 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
  
  public static void test7c_ShortWaitInSingleLane() {
    TestWorkflow7 wf = new TestWorkflow7(50,false);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
 
  public static void test7d_ShortWaitInParallelLane() {
    TestWorkflow7 wf = new TestWorkflow7(50,true);
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(100);
    assertEquals( "2 true", wf.getCount() + " "+ wf.isFinished() );
    assertAllEmpty();
  }
 
 
  public static void test8a_Veto() {
    TestWorkflow8 wf = new TestWorkflow8();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    SuspendTestFactory.getInstance().getScheduler().addVeto("Veto");
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(300);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertOrders(2, "Planning_TestWorkflow8_TestWorkflow8a", 1, "Running_TestWorkflow8");
    assertSuspensions("{}{}");
    
    SuspendTestFactory.getInstance().getScheduler().removeVeto("Veto");
    sleep(500);
        
    assertEquals( "1 true", wf.getCount() + " "+ wf.isFinished() );
    //assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    assertAllEmpty();
  }
  
  public static void test8_Suspend() {
    TestWorkflow8 wf = new TestWorkflow8();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    SuspendTestFactory.getInstance().getScheduler().addVeto("Veto");
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    sleep(300);
    assertEquals( "1 false", wf.getCount() + " "+ wf.isFinished() );
    assertOrders(2, "Planning_TestWorkflow8_TestWorkflow8a", 1, "Running_TestWorkflow8");
    assertSuspensions("{}{}");
    
    Long orderId = wf.getOrder().getId();
    IndentableStringBuilder suspendInfo = new IndentableStringBuilder();
    
    SuspendRootOrderData srod = new SuspendRootOrderData();
    srod.addRootOrderId(orderId);
    srod.suspensionCause(new ExperimentalSuspensionCause());
    srod.timeout(1000, TimeUnit.MILLISECONDS);
    srod.suspensionFailedAction(SuspensionFailedAction.StopSuspending);
    SuspendTestFactory.getInstance().getSuspendResumeManagement().suspendRootOrders(srod);
    
    System.err.println( suspendInfo.toString() );
    //FIXME hier nicht fertig..... 
    
    assertOrders(2, "Planning_TestWorkflow8_TestWorkflow8a", 1, "Running_TestWorkflow8");
    assertSuspensions("{}{}");
    
    SuspendTestFactory.getInstance().getScheduler().removeVeto("Veto");
    sleep(500);
        
    assertEquals( "1 true", wf.getCount() + " "+ wf.isFinished() );
    //assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    assertAllEmpty();
  }

  public static class ExperimentalSuspensionCause extends SuspensionCause {
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
      return "EXPERIMENTAL";
    }
  }
 
  /**
   * 
   */
  public static void testC1_TwoAwaitInParallel_ConcurrentResumes() {
    final CountDownLatch cdl = new CountDownLatch(1);
    SuspendResumeAlgorithm.testConcurrency = new DebugConcurrency(){
      public void resume() {
        try {
          cdl.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      public void suspend() {
      }
      public void suspendInParallel() {
      }
    };
    
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    Long id = wf.getOrder().getId();
    
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{"+id+"=SuspensionEntry(I="+id+",R="+id+")}{}");

    notifyInOwnThread("xy");
    notifyInOwnThread("ab");
    cdl.countDown();

    waitUntilWorkflowIsFinished(wf,600, 2);
    
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    //sleep(5);
    assertAllEmpty();    
  }

  /**
   * 
   */
  public static void testC1_TwoAwaitInParallel_ConcurrentSuspendResume() {
    final CountDownLatch cdl = new CountDownLatch(1);
    SuspendResumeAlgorithm.testConcurrency = new DebugConcurrency(){
      volatile boolean firstResume = true;
      volatile boolean firstSuspend = true;
      public void resume() {
        if( firstResume ) {
          firstResume = false; //erstes resume soll klappen
          return;
        }
        try {
          logger.debug("cdl await resume");
          cdl.await();
          //sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      public void suspend() {
        if( firstSuspend ) {
          firstSuspend = false; //erstes suspend soll klappen
          return;
        }
        try {
          logger.debug("cdl await suspend");
          cdl.await();
          //sleep(1);
          logger.debug("continue suspend");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      public void suspendInParallel() {
      }
      };
    
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(wf);
    Long id = wf.getOrder().getId();
    
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{"+id+"=SuspensionEntry(I="+id+",R="+id+")}{}");

    notifyInOwnThread("xy");
    sleep(100);
    
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0}");
    assertOrders("{"+id+"=Running_TestWorkflow3c}");
    assertSuspensions("{"+id+"=SuspensionEntry(I="+id+",R="+id+")}{1={P1=ParallelExecutor(P1,3T,4F,0E)}#[Lane(P1-2,Resumed)]}");

    notifyInOwnThread("ab");
    sleep(10);
    logger.debug("countDown latch");
    cdl.countDown();
    
    waitUntilWorkflowIsFinished(wf,500, 2);
    
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    //assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    //sleep(5);
    assertAllEmpty();    
  }


  /**
   * 
   */
  public static void testC1_TwoAwaitInParallel_ConcurrentSuspendParallels() {
    final CountDownLatch cdl = new CountDownLatch(1);
    SuspendResumeAlgorithm.testConcurrency = new DebugConcurrency(){
      public void resume() {
      }
      public void suspend() {
      }
      public void suspendInParallel() {
        try {
          cdl.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
    
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    
    waitUntilWorkflowCountIs(2, wf, 600, 2);
    waitUntilSynchronizationHasEntries(2, 100, 2);
    Long id = wf.getOrder().getId();
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Running_TestWorkflow3c}");
    assertSuspensions("{}{}");

    cdl.countDown();
    waitUntilWorkflowIsSuspended(wf, 100, 2);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Suspended(2*AWAITING_SYNCHRONIZATION)_TestWorkflow3c}");
    assertSuspensions("{"+id+"=SuspensionEntry(I="+id+",R="+id+")}{}");

    notifyInOwnThread("xy");
    notifyInOwnThread("ab");
    
    waitUntilWorkflowIsFinished(wf,600, 2);
    
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    //sleep(5);
    assertAllEmpty();    
  }

  /**
   * 
   */
  public static void testC1_TwoAwaitInParallel_ConcurrentSuspendParallelResume() {
    final AwaitCountingLatch acl = new AwaitCountingLatch();
    SuspendResumeAlgorithm.testConcurrency = new DebugConcurrency(){
      private volatile boolean firstSuspend = false;
      public void resume() {
        try {
          logger.debug("cdl resume");
          acl.await();
          //Thread.sleep(100);
          logger.debug("resume starts");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      public void suspend() {
      }
      public void suspendInParallel() {
        if( firstSuspend  ) {
          firstSuspend = false; //erstes suspend soll klappen
          return;
        }
        try {
          logger.debug("cdl suspendInParallel");
          acl.await();
          logger.debug("suspendInParallel starts");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
    
    TestWorkflow3c wf = new TestWorkflow3c();
    assertEquals( "0 false", wf.getCount() + " "+ wf.isFinished() );
    
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(wf);
    
    waitUntilWorkflowCountIs(2, wf, 600, 2);
    waitUntilSynchronizationHasEntries(2, 100, 2);
    Long id = wf.getOrder().getId();
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Running_TestWorkflow3c}");
    assertSuspensions("{}{}");

    notifyInOwnThread("xy");
    notifyInOwnThread("ab");
    
    waitUntilAwaitCountIs( 4, acl, 200, 2 );
    acl.release();
    
    /*
    sleep(100);
    logger.debug("auswerten");
    /*
    //sleep(50); //waitUntilCountDownLatchAwaits( 3, 100, 2);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0N, xy="+id+",P1-2N}");
    assertOrders("{"+id+"=Running_TestWorkflow3c}");
    assertSuspensions("{}{}");

    //waitUntilWorkflowIsFinished(wf,600, 2);
    
    sleep(1000);
    logger.debug("auswerten");
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0N, xy="+id+",P1-2N}");
    assertOrders("{"+id+"=Running_TestWorkflow3c}");
    assertSuspensions("{}{}");
 
    
   /*
    //waitUntilWorkflowIsSuspended(wf, 100, 2);
    assertEquals( "2 false", wf.getCount() + " "+ wf.isFinished() );
    assertSynchronization("{ab="+id+",P1-0, xy="+id+",P1-2}");
    assertOrders("{"+id+"=Suspended_TestWorkflow3c}");
    assertSuspensions("{"+id+"=SuspensionEntry(I="+id+",R="+id+")}{}");
*/
    waitUntilWorkflowIsFinished(wf,600, 2);
    
    assertEquals( "3 true", wf.getCount() + " "+ wf.isFinished() );
    //sleep(5);
    assertAllEmpty();    
  }


 
  private static class AwaitCountingLatch {

    private CountDownLatch cdl = new CountDownLatch(1);
    private AtomicInteger awaits = new AtomicInteger();
    
    public void await() throws InterruptedException {
      awaits.incrementAndGet();
      cdl.await();
    }
    public int getAwaits() {
      return awaits.get();
    }
    public void release() {
      cdl.countDown();
    }
     
  }
  

  public static void testC1L_TwoAwaitInParallel_ConcurrentResumes() {
    if( EXECUTE_LOAD_TESTS ) {
      for( int i=0; i< 1000; ++i ) {
        logger.info("Testrun "+(i+1));
        testC1_TwoAwaitInParallel_ConcurrentResumes();
      }
    }
  }

  public static void testC1L_TwoAwaitInParallel_ConcurrentSuspendResume() {
    if( EXECUTE_LOAD_TESTS ) {
      for( int i=0; i< 1000; ++i ) {
        logger.info("Testrun "+(i+1));
        testC1_TwoAwaitInParallel_ConcurrentResumes();
      }
    }
  }

  public static void testC1L_TwoAwaitInParallel_ConcurrentSuspendParallels() {
    if( EXECUTE_LOAD_TESTS ) { 
      for( int i=0; i< 1000; ++i ) {
        logger.info("Testrun "+(i+1));
        testC1_TwoAwaitInParallel_ConcurrentSuspendParallels();
      }
    }
  }

  public static void testC1L_TwoAwaitInParallel_ConcurrentSuspendParallelResume() {
    if( EXECUTE_LOAD_TESTS ) {
      for( int i=0; i< 1000; ++i ) {
        logger.info("Testrun "+(i+1));
        testC1_TwoAwaitInParallel_ConcurrentSuspendParallelResume();
      }
    }
  }





  
  private abstract static class CountFinishWorkflow extends Workflow {
    AtomicInteger count = new AtomicInteger(0);
    AtomicBoolean finished = new AtomicBoolean(false);
    Serial wf;
 
    public boolean isFinished() {
      if( getResponseListener() == null ) {
        return false;
      } else {
        return getResponseListener().isFinished();
      }
      //return finished.get();
    }
    
    public int getCount() {
      return count.get();
    }

    @Override
    public WFStep getFirstStep() {
      if( wf == null ) {
        wf = new Serial();
        wf.setWorkflow(this);
        wf.append(new Increment(count) );
        appendSteps(wf);
        wf.append(new SetBool(finished,true));
      }
      return wf;
    }

    protected abstract void appendSteps(Serial wf);
    
  }

  private static class TestWorkflow1 extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new Await("xy"));
    }
  }
  
  private static class TestWorkflow2a extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new SubworkflowStep(new TestWorkflow1()));
    }
  }
  
  private static class TestWorkflow2b extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", 
                                  new Sleep(1000),
                                  new Increment(count),
                                  new Serial().append(new SubworkflowStep(new TestWorkflow1())).
                                               append(new Increment(count, 100)) ) );
      wf.append(new Increment(count));
    }
  }
  
  private static class TestWorkflow3a extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", new Increment(count), new Increment(count)));
      wf.append(new Await("xy"));
      wf.append(new LogLaneId());
    }
  }
  
  private static class TestWorkflow3b extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", new Increment(count), new Await("xy") ) );
      wf.append(new Increment(count));
    }
  }
  private static class TestWorkflow3c extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", new Await("ab"), new Increment(count), new Await("xy") ) );
      wf.append(new Increment(count));
    }
  }
  private static class TestWorkflow3d extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", new Sleep(100), new Increment(count), new Increment(count, 10) ) );
      wf.append(new Increment(count));
    }
  }

  private static class TestWorkflow4 extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", 
                                  new Await("xy"),
                                  new Increment(count),
                                  new Serial().append(new Sleep(1000)).
                                               append(new Increment(count, 100)).
                                               append(new LogLaneId()) ) );
      wf.append(new Increment(count));
      wf.append(new LogLaneId());
    }
  }

  private static class TestWorkflow5 extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", 
                                  new Serial().append(new Sleep(500)).append(new Increment(count)),
                                  new LogLaneId(),
                                  new Serial().append(new Await("xy")).
                                               append(new Increment(count, 100)).
                                               append(new Sleep(500)),
                                  new Increment(count,10)             
                                ) );
      wf.append(new Increment(count));
      wf.append(new LogLaneId());
    }
  }


  
  private static class TestWorkflow6 extends CountFinishWorkflow {
    
    private long sleepXY;
    private long sleepP12;
    private long sleepAB;
    
    public TestWorkflow6(long sleepXY, long sleepP12, long sleepAB) {
      this.sleepXY = sleepXY;
      this.sleepP12 = sleepP12;
      this.sleepAB = sleepAB;
    }
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new ParallelStep( "P1", 
                                  new Serial().append(new Sleep(sleepXY)).
                                               append(new Await("xy")), 
                                  new Increment(count),
                                  new Serial().append(new Sleep(sleepP12)).
                                               append(new Increment(count, 10)).
                                               append(new ParallelStep( "P2",
                                                                        new Serial().append(new Sleep(sleepAB)).
                                                                                     append(new Increment(count,100)), 
                                                                        new Await("ab")
                                                                      )).
                                               append(new Increment(count,1000)).
                                               append(new LogLaneId()) ) );
      wf.append(new Increment(count));
      wf.append(new LogLaneId());
    }
  }
  
  private static class TestWorkflow7 extends CountFinishWorkflow {
    
    private long sleep;
    private boolean parallel;
    
    public TestWorkflow7(long sleep, boolean parallel ) {
      this.sleep = sleep;
      this.parallel = parallel;
    }
    @Override
    protected void appendSteps(Serial wf) {
      if( parallel ) {
        wf.append(new ParallelStep( "P1", 
                                    new Serial().append(new LogLaneId()),
                                    new Serial().append(new Wait(sleep))
                                                .append(new Increment(count))
                                                .append(new LogLaneId())
                                  )
                 );
      } else {
        wf.append(new Wait(sleep));
        wf.append(new Increment(count));
        wf.append(new LogLaneId());
      }
    }
  }
  
  private static class TestWorkflow8a extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new Sleep(200));
    }
    public String getVeto() {
      return "Veto";
    }
  }
  private static class TestWorkflow8 extends CountFinishWorkflow {
    @Override
    protected void appendSteps(Serial wf) {
      wf.append(new Sleep(200)).append(new SubworkflowStep(new TestWorkflow8a()));
    }
  }

  
  /**
   * @param i
   */
  private static void sleep(int i) {
    try {
      Thread.sleep(i);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void waitUntilWorkflowIsFinished(CountFinishWorkflow wf, long timeout, long sleep) {
    WaitUntil wu = new WaitUntil(timeout,sleep);
    logger.debug( "Waiting for workflow to finish");
    final ResponseListener rl = wf.getResponseListener();
    wu.waitUntil( new WaitUntil.Until() {
      public boolean until() {
        return rl.isFinished();
      }
      public String name() {
        return "Workflow is finished";
      }} );    
    if( rl.isFinished() ) {
      logger.debug( "Workflow has finished after "+wu.getDuration()+" ms" );
    } else {
      logger.info( "Workflow has not finished after "+wu.getDuration()+" ms: " + wf.isFinished()  );
      //ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      //ThreadInfo[] ti = bean.getThreadInfo(bean.getAllThreadIds(), true, true);
      Map<Thread,StackTraceElement[]> map = Thread.getAllStackTraces();
      for (Map.Entry<Thread, StackTraceElement[]> threadEntry : map.entrySet()) {
          logger.info("Thread:"+threadEntry.getKey().getName()+":"+threadEntry.getKey().getState());
          for (StackTraceElement element : threadEntry.getValue()) {
              logger.info("--> "+element);
          }
      }
      
      //throw new RuntimeException("waitUntilWorkflowIsFinished timed out "+(now-start) );
    }
  }

  private static void waitUntilWorkflowCountIs(final int count, final CountFinishWorkflow wf, long timeout, long sleep) {
    WaitUntil wu = new WaitUntil(timeout,sleep);
    logger.debug( "Waiting for count =="+count);
    wu.waitUntil( new WaitUntil.Until() {
      public boolean until() {
        return count == wf.getCount();
      }
      public String name() {
        return "Workflow has count "+count;
      }} );    
    int cnt = wf.getCount();
    if( count != cnt ) {
      logger.debug( "Workflow count is "+cnt+", expected "+count+" after "+wu.getDuration()+" ms" );
    }
  }
  
  private static class WaitUntil {

    public interface Until {
      boolean until();
      String name();
    }
    
    private long timeout;
    private long sleep;
    private long start;
    private long now;
    
    public WaitUntil(long timeout, long sleep) {
      this.timeout = timeout;
      this.sleep = sleep;
    }

    public long getDuration() {
      return now-start;
    }

    public boolean waitUntil(WaitUntil.Until until) {
      boolean result = false;
      try {
        start = System.currentTimeMillis();
        now = start;
        while ( now < start + timeout ) {
          if( until.until() ) {
            result = true;
            break;
          }
          try {
            Thread.sleep(sleep);
          } catch (InterruptedException e) {
            //dann halt k�rzer warten
          }
          now = System.currentTimeMillis();
        }
        return result;
      } finally {
        String message = "waitUntil "+until.name() +(result?" succeeded":" timed out")+" after "+getDuration()+" ms";
        if( result ) {
          logger.debug(message);
        } else {
          logger.info(message);
        }
      }
    }
    
  }
  
  private static void waitUntilWorkflowIsSuspended(CountFinishWorkflow wf, long timeout, long sleep) {
    WaitUntil wu = new WaitUntil(timeout,sleep);
    logger.debug( "Waiting for workflow to suspend");
    final ResponseListener rl = wf.getResponseListener();
    wu.waitUntil( new WaitUntil.Until() {
      public boolean until() {
        return rl.isSuspended();
      }
      public String name() {
        return "Workflow is suspended";
      }} );    
    if( rl.isSuspended() ) {
      logger.debug( "Workflow is suspended after "+wu.getDuration()+" ms" );
    } else {
      logger.info( "Workflow is not suspended after "+wu.getDuration()+" ms: " + wf.isFinished()  );
      //ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      //ThreadInfo[] ti = bean.getThreadInfo(bean.getAllThreadIds(), true, true);
      Map<Thread,StackTraceElement[]> map = Thread.getAllStackTraces();
      for (Map.Entry<Thread, StackTraceElement[]> threadEntry : map.entrySet()) {
          logger.info("Thread:"+threadEntry.getKey().getName()+":"+threadEntry.getKey().getState());
          for (StackTraceElement element : threadEntry.getValue()) {
              logger.info("--> "+element);
          }
      }
    }
  }


  private static void waitUntilSynchronizationHasEntries(final int count, long timeout, long sleep) {
    WaitUntil wu = new WaitUntil(timeout,sleep);
    logger.debug( "Waiting for synchronization-count =="+count);
    final Synchronization sync = SuspendTestFactory.getInstance().getSynchronization();
    wu.waitUntil( new WaitUntil.Until() {
      public boolean until() {
        return count == sync.size();
      }
      public String name() {
        return "Synchronization has "+count+" entries";
      }} );
    int cnt = sync.size();
    if( count != cnt ) {
      logger.info( "Synchronization count is "+cnt+", expected "+count+" after "+wu.getDuration()+" ms " );
    }

  }

  private static void waitUntilAwaitCountIs(final int count, final AwaitCountingLatch acl, long timeout, long sleep) {
    WaitUntil wu = new WaitUntil(timeout,sleep);
    logger.debug( "Waiting for await-count =="+count);
    wu.waitUntil( new WaitUntil.Until() {
      public boolean until() {
        return count == acl.getAwaits();
      }
      public String name() {
        return "AwaitCountingLatch has "+count+" awaits";
      }} );
    int cnt = acl.getAwaits();
    if( count != cnt ) {
      logger.info( "AwaitCountingLatch count is "+cnt+", expected "+count+" after "+wu.getDuration()+" ms " );
    }
  }


  
  static ExecutorService executor = SuspendTestFactory.newThreadPool("notify-", 2);
  
  private static void notifyInOwnThread(final String correlationId) {
    executor.execute(new Runnable(){
      public void run() {
        SuspendTestFactory.getInstance().getSynchronization().notify(correlationId);
      }
    });
  }


}
