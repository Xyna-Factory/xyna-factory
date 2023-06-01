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

package com.gip.xyna.xprc.xpce.monitoring;



import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xfractwfe.FractalStepHandlerManager;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.execution.XynaExecution;



public class MonitoringDispatcherTest extends TestCase {

  public void testSetAndGetMonitoringDestination() throws XynaException {

    String testOrderType = "testOrderType";
    DestinationKey dk = new DestinationKey(testOrderType);

   FractalStepHandlerManager shm = EasyMock.createMock(FractalStepHandlerManager.class);
//    shm.addHandler(EasyMock.eq(dk), EasyMock.eq(ProcessStepHandlerType.PREHANDLER), (Handler) EasyMock.anyObject());
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(dk), EasyMock.eq(ProcessStepHandlerType.POSTHANDLER), (Handler) EasyMock.anyObject());
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(dk), EasyMock.eq(ProcessStepHandlerType.ERRORHANDLER), (Handler) EasyMock.anyObject());
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(dk), EasyMock.eq(ProcessStepHandlerType.POSTCOMPENSATION), (Handler) EasyMock
//                    .anyObject(), VersionManagement.REVISION_WORKINGSET);
//    EasyMock.expectLastCall().once();
//    shm
//                    .addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.PRECOMPENSATION),
//                                (Handler) EasyMock.anyObject(), VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().once();

    Configuration config = EasyMock.createMock(Configuration.class);
    EasyMock.expectLastCall().once();

    WorkflowEngine wfe = EasyMock.createMock(WorkflowEngine.class);
    EasyMock.expect(wfe.getStepHandlerManager()).andReturn(shm).times(5);

    XynaFactoryManagementODS xfmods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfmods.getConfiguration()).andReturn(config);
    EasyMock.replay(xfmods);

    XynaExecution xExecution = EasyMock.createMock(XynaExecution.class);
    EasyMock.expect(xExecution.getExecutionDestination(EasyMock.isA(DestinationKey.class)))
                    .andReturn(new FractalWorkflowDestination(dk.getOrderType()));
    EasyMock.replay(xExecution);

    DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(
                    depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock
                                    .isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true)
                    .anyTimes();

    XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfacctrl.getDependencyRegister()).andReturn(depreg).anyTimes();

    XynaFactoryManagement xfm = EasyMock.createMock(XynaFactoryManagement.class);
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfmods).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfacctrl).anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("10").anyTimes();
    EasyMock.replay(xfm);

    XynaProcessCtrlExecution xpce = EasyMock.createMock(XynaProcessCtrlExecution.class);
    EasyMock.expect(xpce.getXynaExecution()).andReturn(xExecution).once();
    EasyMock.replay(xpce);

    XynaProcessing xprc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xprc.getWorkflowEngine()).andReturn(wfe).times(5);
    EasyMock.expect(xprc.getXynaProcessCtrlExecution()).andReturn(xpce).once();

    XynaFactory xfac = EasyMock.createMock(XynaFactory.class);
    EasyMock.expect(xfac.getProcessing()).andReturn(xprc).times(6);
    EasyMock.expect(xfac.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.expect(xfac.getFactoryManagement()).andReturn(xfm).anyTimes();  
    xfac.addComponentToBeInitializedLater(EasyMock.isA(MonitoringDispatcher.class));
    EasyMock.expectLastCall().once();

    XynaFactory.setInstance(xfac);

    /*Persistence persistence = EasyMock.createMock(Persistence.class);
    persistence.loadMap(EasyMock.isA(HashMap.class), (FunctionGroup) EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();
    persistence.storeMap(EasyMock.isA(HashMap.class), (FunctionGroup) EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();*/

    EasyMock.replay(depreg, xfacctrl, xfac, xprc, wfe, shm/*, persistence*/);

    //MonitoringDispatcher.setPersistence(persistence);

    // test it
    MonitoringDispatcher monDispatcher = new MonitoringDispatcher();
    monDispatcher.initInternally();

    monDispatcher.setMonitoringLevel(dk, MonitoringCodes.STEP_MONITORING);

    EasyMock.verify(depreg, xfacctrl, xfac, xprc, wfe, shm);
    XynaFactory.setInstance(null);
  }


  public void testMonitoringCodeAcceptance() throws XynaException {

    String testOrderType = "testOrderType";

    FractalStepHandlerManager shm = EasyMock.createMock(FractalStepHandlerManager.class);
//    shm.addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.PREHANDLER), (Handler) EasyMock.anyObject(), VersionManagement.REVISION_WORKINGSET);
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.POSTHANDLER), (Handler) EasyMock.anyObject(), VersionManagement.REVISION_WORKINGSET);
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.ERRORHANDLER), (Handler) EasyMock.anyObject(), VersionManagement.REVISION_WORKINGSET);
//    EasyMock.expectLastCall().once();
//    shm.addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.POSTCOMPENSATION), (Handler) EasyMock
//                    .anyObject(), VersionManagement.REVISION_WORKINGSET);
//    EasyMock.expectLastCall().once();
//    shm
//                    .addHandler(EasyMock.eq(testOrderType), EasyMock.eq(ProcessStepHandlerType.PRECOMPENSATION),
//                                (Handler) EasyMock.anyObject(), VersionManagement.REVISION_WORKINGSET);
    EasyMock.expectLastCall().once();

    XynaExecution xExecution = EasyMock.createMock(XynaExecution.class);
    EasyMock.expect(xExecution.getExecutionDestination(EasyMock.isA(DestinationKey.class)))
                    .andReturn(new FractalWorkflowDestination(testOrderType)).times(6);
    EasyMock.replay(xExecution);

    EasyMock.replay(shm);

    WorkflowEngine wfe = EasyMock.createMock(WorkflowEngine.class);
    EasyMock.expect(wfe.getStepHandlerManager()).andReturn(shm).times(5);

    EasyMock.replay(wfe);

    XynaProcessCtrlExecution xpce = EasyMock.createMock(XynaProcessCtrlExecution.class);
    EasyMock.expect(xpce.getXynaExecution()).andReturn(xExecution).times(5);
    EasyMock.replay(xpce);

    XynaProcessing xprc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xprc.getWorkflowEngine()).andReturn(wfe).times(5);
    EasyMock.expect(xprc.getXynaProcessCtrlExecution()).andReturn(xpce).times(5);

    EasyMock.replay(xprc);

    Configuration config = EasyMock.createMock(Configuration.class);
    EasyMock.expectLastCall().once();

    // zu mockende zeile
    // XynaFactory.getInstance().getFactoryManagement().getClassLoaderDispatcher().instantiateWF(dk.getOrderType());
    XynaFactoryManagementODS ods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(ods.getConfiguration()).andReturn(config).anyTimes();

    EasyMock.replay(ods);


    ClassLoaderDispatcher cld = EasyMock.createMock(ClassLoaderDispatcher.class);

    DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(
                    depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock
                                    .isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true)
                    .anyTimes();

    EasyMock.replay(depreg);

    // mock the xfmctrl
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getClassLoaderDispatcher()).andReturn(cld).anyTimes();
    EasyMock.expect(xfctrl.getDependencyRegister()).andReturn(depreg).anyTimes();

    EasyMock.replay(xfctrl);

    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(ods).anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("xyna.create.diag.cont").anyTimes();
    EasyMock.expect(xfm.getProperty(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getPropertyName()))
                    .andReturn(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getDefaultValue().toString()).anyTimes();
    
    EasyMock.replay(xfm);
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    xf.addComponentToBeInitializedLater((XynaFactoryComponent)EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xprc).times(10);
    
    EasyMock.replay(xf);
    
    XynaFactory.setInstance(xf);
    
   // EasyMock.expect(cld.instantiateWF(testOrderType)).andReturn(new EmptyWorkflow()).anyTimes();
    
    EasyMock.replay(cld);
    
    //conf.init();

    /*Persistence persistence = EasyMock.createMock(Persistence.class);
    persistence.loadMap(EasyMock.isA(HashMap.class), EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();
    persistence.storeMap(EasyMock.isA(HashMap.class), EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();

    EasyMock.replay(persistence);*/

    //MonitoringDispatcher.setPersistence(persistence);


    MonitoringDispatcher monDispatcher = new MonitoringDispatcher();
    monDispatcher.initInternally();

    // verify that the valid monitoring codes are accepted
    try {
      monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"),MonitoringCodes.ERROR_MONITORING);
      monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"),MonitoringCodes.MASTER_WORKFLOW_MONITORING);
      monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"),MonitoringCodes.NO_MONITORING);
      monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"),MonitoringCodes.START_STOP_MONITORING);

      // even step_monitoring should work since the handlers are not directly attached to the workflows but
      // only stored within the StepHandlerManager
      monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"), MonitoringCodes.STEP_MONITORING);
    }
    catch (XynaException e) {
      e.printStackTrace();
      fail("MonitoringDispatcher rejected valid monitoring code: " + e.getMessage());
    }

    // verify that the 50000 first integers are rejected as monitoring codes
    // of course other codes might still be accepted but the test up to Integer.MAX_VALUE would take slightly longer :-)
    for (int i = 0; i < 50000; i++) {
      if (i != MonitoringCodes.ERROR_MONITORING && i != MonitoringCodes.MASTER_WORKFLOW_MONITORING && i != MonitoringCodes.NO_MONITORING && i != MonitoringCodes.START_STOP_MONITORING && i != MonitoringCodes.STEP_MONITORING) {
        try {
          monDispatcher.setMonitoringLevel(new DestinationKey("testOrderType"), i);
        }
        catch (XynaException e) {
          assertEquals(new XPRC_INVALID_MONITORING_TYPE(i).getCode(), e
                          .getCode());
          continue;
        }
        fail("MonitoringDispatcher accepted illegal MonitoringCode <" + i + ">");
      }
    }
    XynaFactory.setInstance(null);
  }


  public void testDispatchCorrectValue() throws XynaException {

    String testOrderType = "testOrderType";

    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGenerator.getUniqueId()).andReturn(longGenerator.getAndIncrement());
    EasyMock.replay(idGenerator);
    IDGenerator.setInstance(idGenerator);

    XynaExecution xExecution = EasyMock.createMock(XynaExecution.class);
    EasyMock.expect(xExecution.getExecutionDestination(EasyMock.isA(DestinationKey.class)))
        .andReturn(new FractalWorkflowDestination(testOrderType)).once();
    EasyMock.replay(xExecution);

    XynaProcessCtrlExecution xpce = EasyMock.createMock(XynaProcessCtrlExecution.class);
    EasyMock.expect(xpce.getXynaExecution()).andReturn(xExecution).once();
    EasyMock.replay(xpce);

    XynaProcessing xprc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xprc.getXynaProcessCtrlExecution()).andReturn(xpce).once();
    EasyMock.replay(xprc);

    DependencyRegister depreg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(
                    depreg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock
                                    .isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true)
                    .anyTimes();
    EasyMock.replay(depreg);

    XynaFactoryControl xfacctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfacctrl.getDependencyRegister()).andReturn(depreg).anyTimes();
    EasyMock.replay(xfacctrl);

    Configuration config = EasyMock.createMock(Configuration.class);
    EasyMock.expectLastCall().once();
    EasyMock.replay(config);

    XynaFactoryManagementODS ods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(ods.getConfiguration()).andReturn(config).anyTimes();
    EasyMock.replay(ods);

    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("xyna.create.diag.cont").anyTimes();
    EasyMock.expect(xfm.getProperty(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getPropertyName()))
                    .andReturn(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getDefaultValue().toString()).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(ods).once();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfacctrl).times(2);
    EasyMock.replay(xfm);

    XynaFactory xfac = EasyMock.createMock(XynaFactory.class);
    xfac.addComponentToBeInitializedLater((XynaFactoryComponent) EasyMock.anyObject());
    EasyMock.expect(xfac.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    EasyMock.expect(xfac.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xfac.getProcessing()).andReturn(xprc).anyTimes();
    XynaFactory.setInstance(xfac);
    EasyMock.replay(xfac);
    
    /*Persistence persistence = EasyMock.createMock(Persistence.class);
    persistence.loadMap(EasyMock.isA(HashMap.class), (FunctionGroup) EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();
    persistence.storeMap(EasyMock.isA(HashMap.class), (FunctionGroup) EasyMock.isA(FunctionGroup.class));
    EasyMock.expectLastCall().once();
    EasyMock.replay(persistence);*/

    //MonitoringDispatcher.setPersistence(persistence);

    // Test
    MonitoringDispatcher monDispatcher = new MonitoringDispatcher();
    monDispatcher.initInternally();

    monDispatcher.setMonitoringLevel(new DestinationKey(testOrderType), MonitoringCodes.MASTER_WORKFLOW_MONITORING);

    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey(testOrderType));
    monDispatcher.dispatch(xo);

    assertEquals("MonitoringDispatcher did not set correct monitoring code",
                 MonitoringCodes.MASTER_WORKFLOW_MONITORING, xo.getMonitoringCode());

    EasyMock.verify(xfac, xprc, xpce, xExecution, depreg, xfacctrl, xfm, config, ods);
    IDGenerator.setInstance(null);

  }

}
