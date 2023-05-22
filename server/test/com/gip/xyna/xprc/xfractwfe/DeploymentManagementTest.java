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
package com.gip.xyna.xprc.xfractwfe;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.WorkflowRevision;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.cleanup.XynaCleanup;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.execution.XynaExecution;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionProcessingRejectionState;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;
import com.gip.xyna.xprc.xpce.planning.XynaPlanning;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrdertypeBean;


public class DeploymentManagementTest extends TestCase {
  
  DeploymentManagement dm;
  
  Map<Long, XynaOrderServerExtension> schedSuspendedMockMap = new HashMap<Long, XynaOrderServerExtension>();
  Collection<XynaOrderServerExtension> schedWaitingMockCol = new HashSet<XynaOrderServerExtension>();
  Collection<XynaOrderServerExtension> plannningOrdersMockCol = new HashSet<XynaOrderServerExtension>();
  Collection<XynaOrderServerExtension> executionOrdersMockCol = new HashSet<XynaOrderServerExtension>();
  Collection<XynaOrderServerExtension> cleanupOrdersMockCol = new HashSet<XynaOrderServerExtension>();
  TreeMap<Long, CronLikeOrder> cronOrdersMockMap = new TreeMap<Long, CronLikeOrder>();
  
  IAnswer<DestinationValue> destAns = new DestinationAnswerer();
  
  SuspendOrdertypeBean susorderTbean = new SuspendOrdertypeBean("", false, VersionManagement.REVISION_WORKINGSET);
  

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ClassLoaderDispatcher cldisp = EasyMock.createMock(ClassLoaderDispatcher.class);
    EasyMock.expect(cldisp.getWFClassLoaderLazyCreate(EasyMock.isA(String.class), EasyMock.isA(String.class), EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET)).andReturn(null).anyTimes();
    EasyMock.expect(cldisp.getExceptionClassLoader(EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET, EasyMock.isA(boolean.class))).andReturn(null).anyTimes();
    EasyMock.expect(cldisp.getMDMClassLoader(EasyMock.isA(String.class), VersionManagement.REVISION_WORKINGSET, EasyMock.isA(boolean.class))).andReturn(null).anyTimes();
    
    XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfacctrl.getClassLoaderDispatcher()).andReturn(cldisp).anyTimes();
    
//    CronLikeOrderDatabase crondb = EasyMock.createMock(CronLikeOrderDatabase.class);
//    EasyMock.expect(crondb.getAllCronLikeOrdersUnsynchronized()).andReturn(cronOrdersMockMap).anyTimes();
    
    ManualInteractionManagement miman = EasyMock.createMock(ManualInteractionManagement.class);
    miman.setProcessingRejectionState(ManualInteractionProcessingRejectionState.DEPLOYMENT);
    EasyMock.expectLastCall().anyTimes();
    miman.setProcessingRejectionState(ManualInteractionProcessingRejectionState.NONE);
    EasyMock.expectLastCall().anyTimes();
    
    XynaProcessingODS xprocods = EasyMock.createMock(XynaProcessingODS.class);
//    EasyMock.expect(xprocods.getCronLikeOrderDatabase()).andReturn(crondb).anyTimes();
    EasyMock.expect(xprocods.getManualInteractionManagement()).andReturn(miman).anyTimes();
    
    XynaFactoryManagementBase xfacman = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfacman.getXynaFactoryControl()).andReturn(xfacctrl).anyTimes();
    
    PlanningDispatcher plandisp = EasyMock.createMock(PlanningDispatcher.class);
    EasyMock.expect(plandisp.getDestination(EasyMock.isA(DestinationKey.class))).andAnswer(destAns).anyTimes();
    
    ExecutionDispatcher execdisp = EasyMock.createMock(ExecutionDispatcher.class);
    EasyMock.expect(execdisp.isPredefined(EasyMock.isA(DestinationKey.class))).andReturn(false).anyTimes();
    
    CleanupDispatcher cleandisp = EasyMock.createMock(CleanupDispatcher.class);
    EasyMock.expect(cleandisp.getDestination(EasyMock.isA(DestinationKey.class))).andAnswer(destAns).anyTimes();
    
    XynaPlanning xplan = EasyMock.createMock(XynaPlanning.class);
    EasyMock.expect(xplan.getPlanningDispatcher()).andReturn(plandisp).anyTimes();
    
    XynaExecution xexec = EasyMock.createMock(XynaExecution.class);
    EasyMock.expect(xexec.getExecutionEngineDispatcher()).andReturn(execdisp).anyTimes();
    EasyMock.expect(xexec.getExecutionDestination(EasyMock.isA(DestinationKey.class))).andAnswer(destAns).anyTimes();
    
    
    XynaCleanup xclean = EasyMock.createMock(XynaCleanup.class);
    EasyMock.expect(xclean.getCleanupEngineDispatcher()).andReturn(cleandisp).anyTimes();
    
    XynaProcessCtrlExecution xprcctrlexec = EasyMock.createMock(XynaProcessCtrlExecution.class);
    EasyMock.expect(xprcctrlexec.getXynaPlanning()).andReturn(xplan).anyTimes();
    EasyMock.expect(xprcctrlexec.getXynaExecution()).andReturn(xexec).anyTimes();
    EasyMock.expect(xprcctrlexec.getXynaCleanup()).andReturn(xclean).anyTimes();
    
    EngineSpecificWorkflowProcessor planProc = EasyMock.createMock(EngineSpecificWorkflowProcessor.class);
    EasyMock.expect(planProc.getOrdersOfRunningProcesses()).andReturn(plannningOrdersMockCol).anyTimes();
    EngineSpecificWorkflowProcessor execProc = EasyMock.createMock(EngineSpecificWorkflowProcessor.class);
    EasyMock.expect(execProc.getOrdersOfRunningProcesses()).andReturn(executionOrdersMockCol).anyTimes();
    EngineSpecificWorkflowProcessor cleanProc = EasyMock.createMock(EngineSpecificWorkflowProcessor.class);
    EasyMock.expect(cleanProc.getOrdersOfRunningProcesses()).andReturn(cleanupOrdersMockCol).anyTimes();
    
    WorkflowEngine xfractwfe = EasyMock.createMock(WorkflowEngine.class);
    EasyMock.expect(xfractwfe.getPlanningProcessor()).andReturn(planProc).anyTimes();
    EasyMock.expect(xfractwfe.getExecutionProcessor()).andReturn(execProc).anyTimes();
    EasyMock.expect(xfractwfe.getCleanupProcessor()).andReturn(cleanProc).anyTimes();
    
    XynaScheduler xsched = EasyMock.createMock(XynaScheduler.class);
//    EasyMock.expect(xsched.getSuspendedOrders()).andReturn(schedSuspendedMockMap).anyTimes(); // FIXME this call needs to be repaired
                                                                                                //       using a FactoryWarehouseCursor
    //EasyMock.expect(xsched.getWaitingNotBackupedOrders()).andReturn(schedWaitingMockCol).anyTimes(); //TODO existiert nicht mehr
    
    SuspendOrdertypeBean suspBean = EasyMock.createMock(SuspendOrdertypeBean.class);
    
    XynaProcessing xproc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xproc.getXynaScheduler()).andReturn(xsched).anyTimes();
    EasyMock.expect(xproc.getWorkflowEngine()).andReturn(xfractwfe).anyTimes();
    EasyMock.expect(xproc.getXynaProcessCtrlExecution()).andReturn(xprcctrlexec).anyTimes();
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(xprocods).anyTimes();
    //EasyMock.expect(xproc.suspendOrdertype(EasyMock.isA(String.class), EasyMock.anyBoolean())).andReturn(susorderTbean).anyTimes();
    //EasyMock.expect(xproc.suspendOrdertype(EasyMock.isA(String.class), EasyMock.isA(Boolean.class))).andReturn(suspBean).anyTimes();
    
    XynaFactory xf = EasyMock.createMock(XynaFactory.class);
    xf.addComponentToBeInitializedLater((XynaFactoryComponent) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfacman).anyTimes();
    XynaFactory.setInstance(xf);

    EasyMock.replay(cldisp, xfacctrl,
                    //                    crondb,
                    miman, xprocods, xfacman, plandisp, execdisp, cleandisp, xplan, xclean, xexec, xprcctrlexec,
                    planProc, execProc, cleanProc, xfractwfe, xsched, suspBean, xproc, xf);

  }
  
  
  
  public void testSingleDeployments() throws XynaException, InterruptedException {

    Set<WorkflowRevision> testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    
    DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.BREAK_ON_USAGE);
    DeploymentManagement.getInstance().cleanupIfLast();
    
    XynaOrderServerExtension testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF1"));
    executionOrdersMockCol.add(testOrder);
    
    try {
      DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.BREAK_ON_USAGE);
      fail("We should have thrown an exception");
    } catch (XPRC_WorkflowProtectionModeViolationException e) {
      ;
    }
    
    executionOrdersMockCol.clear();
    DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
    DeploymentManagement.getInstance().cleanupIfLast();
    
    executionOrdersMockCol.add(testOrder); // we did not encounter an Interface change, we should succeed
    DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);
    DeploymentManagement.getInstance().cleanupIfLast();
    
    try {
      DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, testSet);
      fail("We should have thrown an exception");
    } catch (XPRC_WorkflowProtectionModeViolationException e) {
      ;
    }
    
    DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.FORCE_DEPLOYMENT);
    DeploymentManagement.getInstance().cleanupIfLast();
    
    DeploymentManagement.getInstance().addDeployment(testSet, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
    DeploymentManagement.getInstance().cleanupIfLast();
  }
  
  
  public void testSynchronousDeployments() throws InterruptedException {
    //add several and see if they finish
    CountDownLatch latch = new CountDownLatch(1);
    Set<WorkflowRevision> testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    
    DeploymentRunnable test1 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    DeploymentRunnable test2 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    DeploymentRunnable test3 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    new Thread(test1).start();
    new Thread(test2).start();
    new Thread(test3).start();
    
    Thread.sleep(1000);
    latch.countDown();
        
    
    //test main deployment that fails and other deployment that takes over and succeed / fails
    Thread.sleep(3000);
    XynaOrderServerExtension testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF1"));
    executionOrdersMockCol.add(testOrder);
    
    Thread.sleep(1000);
    testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    test1 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    test2 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, testSet, latch);
    test3 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    new Thread(test1).start();
    new Thread(test2).start();
    new Thread(test3).start();
    
    Thread.sleep(1000);
    latch.countDown();
    
    Thread.sleep(5000);
    assertFalse(test1.sucess);
    assertFalse(test2.sucess);
    assertFalse(test3.sucess);
    


    
    //test several Deployments with some that fail
    Set<WorkflowRevision> testSet2 = new HashSet<WorkflowRevision>();
    testSet2.add(new WorkflowRevision("de.test.WF2", VersionManagement.REVISION_WORKINGSET));
    test1 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch); //fails
    test2 = new DeploymentRunnable(testSet2, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    test3 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);  //fails
    DeploymentRunnable test4 = new DeploymentRunnable(testSet2, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    DeploymentRunnable test5 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);  //fails
    
   // new Thread(test1).start();
    new Thread(test2).start();
    new Thread(test3).start();
    new Thread(test4).start();
    new Thread(test5).start();
    
    //CountUp to wait to have them waiting
    long neverRechableId = 665l;
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+2); //we need at least 2 buckets to prevent the deployment from running
    
    Thread.sleep(2000);
    latch.countDown();
    //let's sleep some more to make sure all are added
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+2);
    
    Thread.sleep(3000);
    assertFalse(test1.sucess);
    assertTrue(test2.sucess);
    assertFalse(test3.sucess);
    assertTrue(test4.sucess);
    assertFalse(test5.sucess);
  }
  
  
  public void testModeOverwrite() throws InterruptedException {
    
    XynaOrderServerExtension testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF1"));
    executionOrdersMockCol.add(testOrder);
    Thread.sleep(1000);
    
    //test mode overwrite, same affacted WF but different Modes
    CountDownLatch latch = new CountDownLatch(1);
    Set<WorkflowRevision> testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));  
    
    DeploymentRunnable test1 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    DeploymentRunnable test2 = new DeploymentRunnable(testSet, WorkflowProtectionMode.FORCE_DEPLOYMENT, null, latch);
    
    new Thread(test1).start();
    new Thread(test2).start();
    
    //CountUp to wait to have them waiting
    long neverRechableId = 665l;
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+2); //we need at least 2 buckets to prevent the deployment from running
    
    Thread.sleep(3000);
    latch.countDown();
    //let's sleep some more to make sure all are added
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+2);
    
    Thread.sleep(3000);
    assertTrue(test1.sucess); //this will only succeed if they get added to the same process, which might be true in rare cases
    assertTrue(test2.sucess);
  }
  
  
  
  public void testDeploymentCascade() throws InterruptedException {

    //Test cascading failures
    executionOrdersMockCol.clear();
    XynaOrderServerExtension testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF1"));    
    executionOrdersMockCol.add(testOrder);
    testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF2"));    
    executionOrdersMockCol.add(testOrder);
    testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF3"));    
    executionOrdersMockCol.add(testOrder);
    Thread.sleep(1000);
    
    //make sure the suspenders fail
    susorderTbean.setSuccess(false);
    
    CountDownLatch latch = new CountDownLatch(1);
    Set<WorkflowRevision> testSet1 = new HashSet<WorkflowRevision>();
    testSet1.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    Set<WorkflowRevision> testSet2 = new HashSet<WorkflowRevision>();
    testSet2.add(new WorkflowRevision("de.test.WF2", VersionManagement.REVISION_WORKINGSET));
    Set<WorkflowRevision> testSet3 = new HashSet<WorkflowRevision>();
    testSet3.add(new WorkflowRevision("de.test.WF3", VersionManagement.REVISION_WORKINGSET));
    Set<WorkflowRevision> testSet4 = new HashSet<WorkflowRevision>();
    testSet4.add(new WorkflowRevision("de.test.WF4", VersionManagement.REVISION_WORKINGSET));
    
    DeploymentRunnable test1 = new DeploymentRunnable(testSet1, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    DeploymentRunnable test2 = new DeploymentRunnable(testSet2, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES, null, latch);
    DeploymentRunnable test3 = new DeploymentRunnable(testSet3, WorkflowProtectionMode.FORCE_DEPLOYMENT, null, latch);
    DeploymentRunnable test4 = new DeploymentRunnable(testSet4, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT, null, latch);
    
    new Thread(test1).start();
    new Thread(test2).start();
    new Thread(test3).start();
    new Thread(test4).start();
    
    //CountUp to wait to have them waiting
    long neverRechableId = 665l;
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+2); //we need at least 2 buckets to prevent the deployment from running
    
    Thread.sleep(2000);
    latch.countDown();
    //let's sleep some more to make sure all are added
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+2);
    
    Thread.sleep(3000);
    assertFalse(test1.sucess);
    assertFalse(test2.sucess);
    assertFalse(test3.sucess);
    assertTrue(test4.sucess);

  }
  
  
  public void testErrors() throws InterruptedException {

    //test timeOut
    CountDownLatch latch = new CountDownLatch(1);
    
    Set<WorkflowRevision> testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    DeploymentRunnable test = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    new Thread(test).start();
    
    long neverRechableId = 665l;
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(neverRechableId+2); //we need at least 2 buckets to prevent the deployment from running
    
    Thread.sleep(2000);
    latch.countDown();
    
    Thread.sleep(20000); //Timeout is 15sec atm
    assertFalse(test.sucess);
    
    
    destAns = new InvalidDestinationAnswerer();
 
    //test unresolveable destination
    
    latch = new CountDownLatch(1);
    DeploymentRunnable test2 = new DeploymentRunnable(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch);
    new Thread(test2).start();
    
    Thread.sleep(1000);    
    latch.countDown();    
    
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+1);
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(neverRechableId+2);
        
    Thread.sleep(4000);
    assertFalse(test.sucess);
    
    destAns = new DestinationAnswerer(); //restore previous behaviour
  }
  
  
  public void testDeploymentChaining() throws InterruptedException {

    // 1. runs through and chains another; 2. fails and chains another; 3. runs through
    XynaOrderServerExtension testOrder = new XynaOrderServerExtension();
    testOrder.setDestinationKey(new DestinationKey("de.test.WF1"));    
    executionOrdersMockCol.add(testOrder);
    CountDownLatch latch = new CountDownLatch(1);
    Set<WorkflowRevision> testSet = new HashSet<WorkflowRevision>();
    testSet.add(new WorkflowRevision("de.test.WF1", VersionManagement.REVISION_WORKINGSET));
    
    DeploymentRunnable test3 = new DeploymentRunnable(testSet, WorkflowProtectionMode.FORCE_DEPLOYMENT, null, latch);
    DeploymentRunnableChainer test2 = new DeploymentRunnableChainer(testSet, WorkflowProtectionMode.BREAK_ON_USAGE, null, latch, test3);
    DeploymentRunnableChainer test1 = new DeploymentRunnableChainer(testSet, WorkflowProtectionMode.FORCE_DEPLOYMENT, null, latch, test2);
    
    new Thread(test1).start();
    latch.countDown();
    
    Thread.sleep(8000);
    assertTrue(test1.sucess);
    assertFalse(test2.sucess);
    assertTrue(test3.sucess);
  }
  
  
  class DeploymentRunnableChainer extends DeploymentRunnable {

    private DeploymentRunnable chainedDeployment;
    
    DeploymentRunnableChainer(Set<WorkflowRevision> affactedSet, WorkflowProtectionMode mode, Set<WorkflowRevision> interfaceSet,
                              CountDownLatch latch, DeploymentRunnable chainedDeployment) {
      super(affactedSet, mode, interfaceSet, latch);
      this.chainedDeployment = chainedDeployment;
    }
    
    @Override
    public void run() {
      try {
        latch.await();
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
      
      if (interfaced == null) {
        try {
          DeploymentManagement.getInstance().addDeployment(affacted, mode);
          new Thread(chainedDeployment).start();
          try {
            Thread.sleep(2000); //sleep a bit to let it arrive
          } catch (InterruptedException e) {
            fail(e.getMessage());
          }
          DeploymentManagement.getInstance().cleanupIfLast();
          sucess = true;
        } catch (XynaException e) {
          new Thread(chainedDeployment).start();
          //e.printStackTrace();
          sucess = false;
        }    
      } else {
        try {
          DeploymentManagement.getInstance().addDeployment(affacted, mode, interfaced);
          new Thread(chainedDeployment).start();
          try {
            Thread.sleep(2000); //sleep a bit to let it arrive
          } catch (InterruptedException e) {
            fail(e.getMessage());
          }
          DeploymentManagement.getInstance().cleanupIfLast();
          sucess = true;
        } catch (XynaException e) {
          new Thread(chainedDeployment).start();
          //e.printStackTrace();
          sucess = false;
        } 
      }
    }
    
  }
  
  class DeploymentRunnable implements Runnable {

    protected Set<WorkflowRevision> affacted;
    protected Set<WorkflowRevision> interfaced;
    protected WorkflowProtectionMode mode;
    protected CountDownLatch latch;
    
    public boolean sucess;
    
    DeploymentRunnable(Set<WorkflowRevision> affactedSet, WorkflowProtectionMode mode, Set<WorkflowRevision> interfaceSet, CountDownLatch latch) {
      affacted = affactedSet;      
      interfaced = interfaceSet;
      this.mode = mode;
      this.latch = latch;
    }
    
    public void run() {
      try {
        latch.await();
      } catch (InterruptedException e) {
        fail(e.getMessage());
      }
   
      if (interfaced == null) {
        try {
          DeploymentManagement.getInstance().addDeployment(affacted, mode);
          DeploymentManagement.getInstance().cleanupIfLast();
          sucess = true;
        } catch (XynaException e) {
          //e.printStackTrace();
          sucess = false;
        }    
      } else {
        try {
          DeploymentManagement.getInstance().addDeployment(affacted, mode, interfaced);
          DeploymentManagement.getInstance().cleanupIfLast();
          sucess = true;
        } catch (XynaException e) {
          //e.printStackTrace();
          sucess = false;
        } 
      }      
    }    
  }
  
  
  class DestinationAnswerer implements IAnswer<DestinationValue> {

    //this will always resolve a DestinationValue to a DestinationKey containing the same String
    public DestinationValue answer() throws Throwable {
      Object[] args = EasyMock.getCurrentArguments();
      DestinationKey arg = (DestinationKey)args[0];      
      return new FractalWorkflowDestination(arg.getOrderType());
    }
  }
  
  class InvalidDestinationAnswerer implements IAnswer<DestinationValue> {

    //this will always resolve a DestinationValue to a DestinationKey containing the same String
    public DestinationValue answer() throws Throwable {
      throw new XPRC_DESTINATION_NOT_FOUND("yourOrderType", "whateverDispatcher");
    }
  }

}
