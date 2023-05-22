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

package com.gip.xyna.xprc.xsched;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeSchedulerFactory;
import com.gip.xyna.xprc.xsched.ordercancel.ICancelResultListener;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;




public class CancelTest extends TestCase {

  private static final Logger logger = Logger.getLogger(CancelTest.class);
  
  private XynaScheduler xsched;
  private XynaFactory xf;
  private XynaProcessing xprc;
  private XynaProcessCtrlExecution xpctrl;
  private StatusChangeProvider shp;
  private Configuration config;
  private XynaFactoryManagementODS xfmods;
  private XynaFactoryManagement xfm;

  private IDGenerator idGenerator;


  public void setUp() {
    try {

      // First mock some classes

      idGenerator = EasyMock.createMock(IDGenerator.class);
      EasyMock.expect(idGenerator.getUniqueId()).andAnswer(new IAnswer<Long>() {

        private AtomicLong idGeneratorMock = new AtomicLong(0);

        public Long answer() throws Throwable {
          return idGeneratorMock.getAndIncrement();
        }
      }).anyTimes();
      EasyMock.replay(idGenerator);

      shp = EasyMock.createMock(StatusChangeProvider.class);
      shp.notifyListeners(EasyMock.isA(XynaOrderServerExtension.class), EasyMock.eq(OrderInstanceStatus.SCHEDULING));
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(shp);
      
      config = EasyMock.createMock(Configuration.class);
      config.addPropertyChangeListener(EasyMock.isA(IPropertyChangeListener.class));
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(config);

      OrderArchive wfidb = EasyMock.createMock(OrderArchive.class);
      wfidb.updateStatus(org.easymock.EasyMock.isA(XynaOrderServerExtension.class), EasyMock.eq(OrderInstanceStatus.SCHEDULING), EasyMock.isA(ODSConnection.class));
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(wfidb);

      xpctrl = EasyMock.createMock(XynaProcessCtrlExecution.class);
      EasyMock.expect(xpctrl.getStatusChangeProvider()).andReturn(shp).anyTimes();
     // xpctrl.finishOrderExecution(EasyMock.isA(XynaOrderServerExtension.class));
    //  EasyMock.expectLastCall().once();
      EasyMock.replay(xpctrl);
      
      xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
      EasyMock.expect(xfmods.getConfiguration()).andReturn(config).times(2);
      EasyMock.replay(xfmods);

      XynaProcessingODS xprcods = EasyMock.createMock(XynaProcessingODS.class);
      EasyMock.expect(xprcods.getOrderArchive()).andReturn(wfidb);
      EasyMock.replay(xprcods);


      DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
      EasyMock.expect(depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class),
                                           EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class)))
                      .andReturn(true).anyTimes();
      EasyMock.replay(depreg);

      XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
      EasyMock.expect(xfacctrl.getDependencyRegister()).andReturn(depreg).anyTimes();
      EasyMock.replay(xfacctrl);

      xfm = EasyMock.createMock(XynaFactoryManagement.class);
      EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfmods).anyTimes();
      EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfacctrl).anyTimes();
      EasyMock.expect(xfm.getProperty(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.toString())).andReturn("false").anyTimes();
      EasyMock.expect(xfm.getProperty(XynaProperty.XYNA_BACKUP_ORDERS_WAITING_FOR_SCHEDULING.toString())).andReturn("false")
                      .anyTimes();
      EasyMock.replay(xfm);


      CapacityManagement capManagement = EasyMock.createMock(CapacityManagement.class);
      EasyMock.expect(capManagement.allocateCapacities(EasyMock.isA(OrderInformation.class),EasyMock.isA(SchedulingData.class)))
                      .andReturn(new CapacityAllocationResult("capName"));
      EasyMock.expect(capManagement.getDefaultName()).andReturn(CapacityManagement.DEFAULT_NAME).times(2);
      EasyMock.replay(capManagement);
      CapacityManagementFactory.setInstance(capManagement);

      CronLikeScheduler cronLikeSched = EasyMock.createMock(CronLikeScheduler.class);
      EasyMock.expect(cronLikeSched.getDefaultName()).andReturn(CronLikeScheduler.DEFAULT_NAME).times(2);
      EasyMock.replay(cronLikeSched);
      CronLikeSchedulerFactory.setInstance(cronLikeSched);

      PreScheduler preSched = EasyMock.createMock(PreScheduler.class);
      EasyMock.expect(preSched.getDefaultName()).andReturn(PreScheduler.DEFAULT_NAME).times(2);
      EasyMock.replay(preSched);
      PreSchedulerFactory.setInstance(preSched);

      OrderSeriesManagement oSeriesManagement = EasyMock.createMock(OrderSeriesManagement.class);
      EasyMock.expect(oSeriesManagement.getDefaultName()).andReturn(OrderSeriesManagement.DEFAULT_NAME).times(2);
      EasyMock.replay(oSeriesManagement);
      OrderSeriesManagementFactory.setInstance(oSeriesManagement);


      
      xf = EasyMock.createMock(XynaFactory.class);
      XynaFactory.setInstance(xf);


      OrderStatus orderStatus = EasyMock.createMock(OrderStatus.class);
      orderStatus.changeMasterWorkflowStatus(EasyMock.isA(XynaOrderServerExtension.class), EasyMock.isA(OrderInstanceStatus.class), EasyMock.isA(ODSConnection.class));
      EasyMock.expectLastCall();
      orderStatus.changeMasterWorkflowStatusNoException(EasyMock.isA(XynaOrderServerExtension.class), EasyMock.isA(OrderInstanceStatus.class), EasyMock.isA(ODSConnection.class));
      EasyMock.expectLastCall();
      EasyMock.replay(orderStatus);


      xprc = EasyMock.createMock(XynaProcessing.class);
      EasyMock.expect(xprc.getXynaProcessCtrlExecution()).andReturn(xpctrl).anyTimes();
      EasyMock.expect(xprc.getXynaProcessingODS()).andReturn(xprcods).anyTimes();
      EasyMock.expect(xprc.getOrderStatus()).andReturn(orderStatus).anyTimes();
      
      
      EasyMock.expect(xf.getProcessing()).andReturn(xprc).anyTimes();
      EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
      EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
      EasyMock.expect(xf.isStartingUp()).andReturn(false).anyTimes();
      EasyMock.expect(xf.getIDGenerator()).andReturn(idGenerator).anyTimes();
      
      EasyMock.replay(xf);

      xsched = new XynaScheduler();    

      EasyMock.expect(xprc.getXynaScheduler()).andReturn(xsched).anyTimes();
      EasyMock.replay(xprc);
      
      xsched.init();
      // By now we have a fully working instance of XynaScheduler

      
    } catch (XynaException xe) {
      fail("error occurred: " + xe.getMessage());
    }
  }
  
  public void tearDown() {
    // Finally, shutdown the scheduler again
    xsched.shutdownRunnables();
    IDGenerator.setInstance(null);
    EasyMock.verify(xf, xprc, xpctrl, shp, config, xfmods, xfm);
  }
  
  public void testCancelPresentOrderSuccess() throws XynaException {

    DestinationKey dk = new DestinationKey("testDestinationKey");
    XynaOrderServerExtension xo = new XynaOrderServerExtension(dk);
    xo.setMonitoringLevel(MonitoringCodes.NO_MONITORING);

    xsched.addOrder(xo, null, true);
    assertTrue(xsched.cancelOrder(xo.getId(), null));

  }


  public void testCancelUnpresentOrderFailure() throws XynaException {

    DestinationKey dk = new DestinationKey("testDestinationKey");
    XynaOrderServerExtension xo = new XynaOrderServerExtension(dk);
    xo.setMonitoringLevel(MonitoringCodes.NO_MONITORING);

    // xsched.addOrder(xo);
    assertFalse(xsched.cancelOrder(xo.getId(), null));

  }


  public void testCancelNotYetPresentOrderSuccess() throws XynaException {

    final ArrayList<String> resultList = new ArrayList<String>();
    ICancelResultListener listener = new ICancelResultListener() {

      @Override
      public void cancelFailed() {
        fail("Cancel failed but was expected to succeed");
      }

      @Override
      public void cancelSucceeded() {
        resultList.add("success");
      }
      
    };
    listener.setAbsoluteCancelTimeout(System.currentTimeMillis() + new Long(5000));

    DestinationKey dk = new DestinationKey("testDestinationKey");
    XynaOrderServerExtension xo = new XynaOrderServerExtension(dk);
    xo.setMonitoringLevel(MonitoringCodes.NO_MONITORING);

    boolean instantSuccess = xsched.cancelOrder(xo.getId(), listener);

    assertFalse("Did not expect instant success", instantSuccess);

    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      // ignore
    }

    xsched.addOrder(xo, null, true);

    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      // ignore
    }

    if (resultList.size() == 0)
      fail ("No success response received");

  }


  public void testCancelNotYetPresentOrderTimeout() throws XynaException {

    final ArrayList<String> resultList = new ArrayList<String>();
    ICancelResultListener listener = new ICancelResultListener() {

      @Override
      public void cancelFailed() {
        resultList.add("success");
      }

      @Override
      public void cancelSucceeded() {
        fail("Cancel succeeded but was expected to fail");
      }

    };

    listener.setAbsoluteCancelTimeout(System.currentTimeMillis() + new Long(2000));

    DestinationKey dk = new DestinationKey("testDestinationKey");
    XynaOrderServerExtension xo = new XynaOrderServerExtension(dk);
    xo.setMonitoringLevel(MonitoringCodes.NO_MONITORING);

    boolean instantSuccess = xsched.cancelOrder(xo.getId(), listener);

    assertFalse("Did not expect instant success", instantSuccess);

    try {
      Thread.sleep(2200);
    }
    catch (InterruptedException e) {
      // ignore
    }

    xsched.addOrder(xo, null, true);

    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      // ignore
    }

    if (resultList.size() == 0)
      fail ("No 'fail' response received");
  }


  public void testCancelAlreadyStartedOrderTimeout() throws XynaException {
    logger.debug("starting test testCancelAlreadyStartedOrderTimeout");


    CapacityManagement capManagement = EasyMock.createMock(CapacityManagement.class);
    EasyMock.expect(capManagement.allocateCapacities(EasyMock.isA(OrderInformation.class),EasyMock.isA(SchedulingData.class)))
        .andReturn(CapacityAllocationResult.SUCCESS);
    EasyMock.expect(capManagement.getDefaultName()).andReturn(CapacityManagement.DEFAULT_NAME);
    EasyMock.replay(capManagement);
    CapacityManagementFactory.setInstance(capManagement);
    xsched.capacityManagement = capManagement;
    
    final ArrayList<String> resultList = new ArrayList<String>();
    ICancelResultListener listener = new ICancelResultListener() {

      @Override
      public void cancelFailed() {
        logger.debug("time (got cancelFailed) = " + System.currentTimeMillis());
        resultList.add("success");
      }

      @Override
      public void cancelSucceeded() {
        logger.debug("time (got cancelSucceeded) = " + System.currentTimeMillis());
        fail("Cancel succeeded but was expected to fail");
      }

    };

    DestinationKey dk = new DestinationKey("testDestinationKey");
    XynaOrderServerExtension xo = new XynaOrderServerExtension(dk);
    xo.setMonitoringLevel(MonitoringCodes.NO_MONITORING);

    // now also mock the executor
    XynaExecutor exec = EasyMock.createMock(XynaExecutor.class);
    exec.executeRunnableWithExecutionThreadpool(EasyMock.isA(XynaRunnable.class), EasyMock.eq(xo.getPriority()));
    EasyMock.expectLastCall().once();
    EasyMock.replay(exec);
    XynaExecutor.setInstance(exec);

    logger.debug("time (added to scheduler) = " + System.currentTimeMillis());
    xsched.addOrder(xo, null, true);

    try {
      Thread.sleep(100);
    }
    catch (InterruptedException e) {
      // ignore
    }

    listener.setAbsoluteCancelTimeout(System.currentTimeMillis() + 2000);
    boolean instantSuccess = xsched.cancelOrder(xo.getId(), listener);
    assertFalse("Did not expect instant success", instantSuccess);

    try {
      Thread.sleep(2200);
    }
    catch (InterruptedException e) {
      // ignore
    }

    if (resultList.size() == 0)
      fail ("No 'fail' response received");

  }

}
