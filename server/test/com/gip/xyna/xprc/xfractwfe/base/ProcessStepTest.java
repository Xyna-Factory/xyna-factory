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

package com.gip.xyna.xprc.xfractwfe.base;



import java.util.ArrayList;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;



public class ProcessStepTest extends TestCase {

  public void testStepExecutionWithCompensation() throws XynaException {

    ODS ods1 = ODSImpl.getInstance(false);
    long persistenceLayerId = 42;
    ods1.registerPersistenceLayer(persistenceLayerId, XMLPersistenceLayer.class);
    long instanceId = ods1.instantiatePersistenceLayerInstance(persistenceLayerId, XynaFactoryManagement.DEFAULT_NAME,
                                                               ODSConnectionType.DEFAULT,
                                                               new String[] {"Configuration"});
    ods1.setPersistenceLayerForTable(instanceId, XynaPropertyStorable.TABLE_NAME, null);
    ods1.registerStorable(XynaPropertyStorable.class);

    // XynaFactoryManagementODS
    XynaFactoryManagementODS ods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(ods.getConfiguration()).andReturn(new Configuration()).anyTimes();

    EasyMock.replay(ods);

    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(ods).anyTimes();
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("xyna.create.diag.cont").anyTimes();

    EasyMock.replay(xfm);

    Handler preHandler = new Handler() {
      @Override
      public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
        ((MyProcessStep) ((Object) pstep)).preHandle();
      }
    };
    ArrayList<Handler> preHandlerList = new ArrayList<Handler>();
    preHandlerList.add(preHandler);

    Handler postHandler = new Handler() {
      @Override
      public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
        ((MyProcessStep) ((Object) pstep)).postHandle();
      }
    };
    ArrayList<Handler> postHandlerList = new ArrayList<Handler>();
    postHandlerList.add(postHandler);

    EngineSpecificStepHandlerManager stepHandler = EasyMock.createMock(EngineSpecificStepHandlerManager.class);
    EasyMock.expect(stepHandler.getHandlers(EasyMock.eq(ProcessStepHandlerType.PREHANDLER), EasyMock.isA(XynaOrderServerExtension.class)))
                    .andReturn(preHandlerList).times(1);
    EasyMock.expect(stepHandler.getHandlers(EasyMock.eq(ProcessStepHandlerType.POSTHANDLER), EasyMock.isA(XynaOrderServerExtension.class)))
                    .andReturn(postHandlerList).times(1);
    EasyMock.expect(stepHandler.getHandlers(EasyMock.eq(ProcessStepHandlerType.ERRORHANDLER), EasyMock.isA(XynaOrderServerExtension.class)))
                    .andReturn(new ArrayList<Handler>()).times(1);
    EasyMock.expect(stepHandler.getHandlers(EasyMock.eq(ProcessStepHandlerType.PRECOMPENSATION), EasyMock.isA(XynaOrderServerExtension.class)))
                    .andReturn(new ArrayList<Handler>()).times(1);
    EasyMock.expect(stepHandler.getHandlers(EasyMock.eq(ProcessStepHandlerType.POSTCOMPENSATION), EasyMock.isA(XynaOrderServerExtension.class)))
                    .andReturn(new ArrayList<Handler>()).times(1);
    EasyMock.replay(stepHandler);

    WorkflowEngine xfractwfe = EasyMock.createMock(WorkflowEngine.class);
    EasyMock.expect(xfractwfe.getStepHandlerManager()).andReturn(stepHandler).anyTimes();
    EasyMock.replay(xfractwfe);

    XynaProcessing xprc = EasyMock.createMock(XynaProcessing.class);
    EasyMock.expect(xprc.getWorkflowEngine()).andReturn(xfractwfe).anyTimes();
    EasyMock.replay(xprc);


    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xprc).anyTimes();

    XynaFactory.setInstance(xf);
    EasyMock.replay(xf);

    // processstep hat 3 kinder, wobei nur 2 davon ausgef�hrt werden und der zweite davon fehlerhaft
    MyProcessStep step = new MyProcessStep(0, true);
    MyProcess proc = new MyProcess();
    step.init(proc);
    MyProcessStep step1 = step.step1;
    MyProcessStep step2 = step.step2;
    MyProcessStep step3 = step.step3;
    step1.init(proc);
    step2.init(proc);
    step3.init(proc);
    boolean gotException = false;
    try {
      step.execute(); // schritt ausf�hren. ruft intern 2 weitere schritte, einer geht gut, einer nicht
    } catch (XynaException e) {
      gotException = true;
      if (e.getMessage().contains("unexpected")) {
        fail("got unexpected Fault: " + e.getMessage());
      }
    }

    assertEquals("execute should have been failed", true, gotException);
    // step0
    assertEquals("step0 should have been executed", true, step.executed);
    assertEquals("step0 should not have been compensated", false, step.compensated);
     assertEquals("step0s preHandler should have been called", true, step.preHandled);
     assertEquals("step0s postHandler should not have been called", false, step.postHandled);
    // step1
    assertEquals("step1 should have been executed", true, step1.executed);
    assertEquals("step1 should not have been compensated", false, step1.compensated);
     assertEquals("step1s preHandler should have been called", true, step1.preHandled);
     assertEquals("step1s postHandler should have been called", true, step1.postHandled);
    // step2
    assertEquals("step2 should have been executed", true, step2.executed);
    assertEquals("step2 should not have been compensated", false, step2.compensated);
     assertEquals("step2s preHandler should have been called", true, step2.preHandled);
     assertEquals("step2s postHandler should not have been called", false, step2.postHandled);
    // step3
    assertEquals("step3 should not have been executed", false, step3.executed);
    assertEquals("step3 should not have been compensated", false, step3.compensated);
     assertEquals("step3s preHandler should not have been called", false, step3.preHandled);
     assertEquals("step3s postHandler should not have been called", false, step3.postHandled);

    step.compensate();
    // step0
    assertEquals("step0 should not have been compensated", false, step.compensated);
    // step1
    assertEquals("step1 should have been compensated", true, step1.compensated);
    // step2
    assertEquals("step2 should not have been compensated", false, step2.compensated);
    // step3
    assertEquals("step3 should not have been compensated", false, step3.compensated);

    EasyMock.verify(stepHandler);

  }
  
  
  @Override
  protected void tearDown() throws Exception {
    ODSImpl.clearInstances();
    super.tearDown();
  }


  private static class MyProcessStep extends FractalProcessStep<MyProcess> {

    private static final long serialVersionUID = 1L;

    private boolean compensated = false;
    private boolean executed = false;
    private boolean isRoot = false;
    private boolean preHandled = false;
    private boolean postHandled = false;
    private MyProcessStep step1;
    private MyProcessStep step2;
    private MyProcessStep step3;


    /**
     * @param i
     */
    public MyProcessStep(int i, boolean isRoot) {
      super(i);
      this.isRoot = isRoot;
      if (i == 0) {
        step1 = new MyProcessStep(1, false);
        step2 = new MyProcessStep(2, false);
        step3 = new MyProcessStep(3, false);
      }
    }


    @Override
    public void compensateInternally() throws XynaException {
      compensated = true;
    }


    @Override
    public void executeInternally() throws XynaException {
      executed = true;
      if (getN() == 0) {
        executeChildren(0);
      } else if (getN() == 1) {

      } else if (getN() == 2) {
        throw new XynaException("expectedFault");
      } else if (getN() == 3) {
        // sollte nicht ausgef�hrt werden
        fail("processStep 3 should not have been executed");
      } else {
        fail("invalid processStep " + getN());
      }
    }


    @Override
    protected FractalProcessStep<MyProcess>[] getChildren(int i) {
      if (isRoot) {
        if (i == 0) {
          return new FractalProcessStep[] {step1, step2};
        } else if (i == 1) {
          return new FractalProcessStep[] {step3};
        } else {
          return null;
        }
      } else {
        return null;
      }
    }


    @Override
    protected int getChildrenTypesLength() {
      return isRoot ? 2 : 0;
    }


    /**
     * 
     */
    public void postHandle() {
      postHandled = true;
    }


    /**
     * 
     */
    public void preHandle() {
      preHandled = true;
    }


    public XynaObject[] getCurrentOutgoingValues() {
      return null;
    }


    public XynaObject[] getCurrentIncomingValues() {
      return null;
    }


    public Integer getXmlId() {
      return null;
    }


    @Override
    public XynaExceptionInformation getCurrentUnhandledThrowable() {
      return null;
    }


    public void setCurrentThrowable(Throwable t) {
    }

  }

  private static class MyProcess extends XynaProcess {

    private static final long serialVersionUID = -4948251980086033117L;


    public FractalProcessStep<?>[] getAllSteps() {
      return null;
    }


    public Container getOutput() {
      return null;
    }


    public FractalProcessStep<?>[] getStartSteps() {
      return null;
    }

    public FractalProcessStep<?>[] getAllLocalSteps() {
      return null;
    }


    @Override
    protected void onDeployment() throws XynaException {
    }


    @Override
    protected void onUndeployment() throws XynaException {
    }


    public void setInputVars(GeneralXynaObject o)  {
    }


    @Override
    protected void initializeMemberVars() {
    }


    @Override
    public String getOriginalName() {
      return null;
    }

  }

}
