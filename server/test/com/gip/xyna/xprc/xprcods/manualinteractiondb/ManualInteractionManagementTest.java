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

package com.gip.xyna.xprc.xprcods.manualinteractiondb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xpce.manualinteraction.WorkflowStacktrace;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;



public class ManualInteractionManagementTest extends TestCase {
  
  private static Logger logger = Logger.getLogger(ManualInteractionManagementTest.class);

  private ODS ods;


  public void testEntryCycle() throws XynaException, InterruptedException {

    ods = ODSImpl.getInstance();

    OrderInstanceDetails wid = new OrderInstanceDetails();

    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    AtomicLong longGenerator = new AtomicLong(1);
    for (int i = 0; i < 1; i++) {
      EasyMock.expect(idGenerator.getUniqueId()).andReturn(longGenerator.getAndIncrement());
    }
    EasyMock.replay(idGenerator);
    IDGenerator.setInstance(idGenerator);

    //ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();

    final XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("bla"), new Container());

    OrderArchive wfidb = EasyMock.createMock(OrderArchive.class);
    EasyMock.expect(wfidb.getCompleteOrder(xo.getId())).andReturn(wid).anyTimes();
    // wfidb.updateStatus(xo, OrderInstanceStatus.INITIALIZATION);
    //  EasyMock.expectLastCall().once();
    List<OrderInstance> list = new ArrayList<OrderInstance>();
    list.add(new OrderInstance(xo));
    OrderInstanceResult oir = new OrderInstanceResult(list, 1, 1);
    EasyMock.expect(wfidb.search(EasyMock.isA(OrderInstanceSelect.class), EasyMock.eq(1))).andReturn(oir);
    wfidb.updateStatus(xo, OrderInstanceStatus.RUNNING, null);
    EasyMock.expectLastCall().once();

    EasyMock.replay(wfidb);

    XynaProcessingODS xpods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xpods.getOrderArchive()).andReturn(wfidb).anyTimes();
    EasyMock.expect(xpods.getODS()).andReturn(ods).anyTimes();

    OrderStatus orderstatus = EasyMock.createMock(OrderStatus.class);
    // TODO
//    orderstatus.miStatus(xo, OrderInstanceStatus.INITIALIZATION, true);
//    EasyMock.expectLastCall().once();
//    orderstatus.miStatus(EasyMock.isA(XynaOrderServerExtension.class), EasyMock
//                    .eq(OrderInstanceStatus.MANUAL_INTERACTION), EasyMock.eq(false));
//    EasyMock.expectLastCall().once();

    EasyMock.replay(xpods, orderstatus);

    XynaProcessingBase xprc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xprc.getXynaProcessingODS()).andReturn(xpods).anyTimes();
    EasyMock.expect(xprc.getOrderStatus()).andReturn(orderstatus).once();
    
    EasyMock.replay(xprc);
    
    XynaProcessingPortal xpp = EasyMock.createMock(XynaProcessingPortal.class);
    //EasyMock.expect(xpp.resumeOrder(EasyMock.isA(Long.class))).andReturn(null).anyTimes(); FIXME
    
    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getProcessing()).andReturn(xprc).anyTimes();
    EasyMock.expect(xf.getProcessingPortal()).andReturn(xpp).anyTimes();
    xf.addComponentToBeInitializedLater((XynaFactoryComponent)EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
                    
    EasyMock.replay(xf);
    
    XynaFactory.setInstance(xf);
     

    
    final ManualInteractionManagement midb = new ManualInteractionManagement();
    midb.initInternally(); //wegen late initializer
    Thread t = new Thread() {
      public void run() {
        ProcessSuspendedException ex = null;
        try {          
          midb.waitForMI(xo, "reason", "type", "userGroup", "todo", null);
        } catch (ProcessSuspendedException e) {
          ex = e;
          /*
          try {
            midb.suspensionCaptured(e, null, xo, null);
          } catch (XPRC_FeatureRelatedExceptionDuringSuspensionHandling e1) {
            logger.error("", e1);
            fail(e1.getMessage());
          }
          */
        } catch (Throwable e) {
          logger.error("", e);
          fail(e.getMessage());
        }
        assertNotNull("No exception caught", ex);
        //mi beendet
        try {
          assertEquals(1, midb.listManualInteractionEntries().size());
        } catch (PersistenceLayerException e) {
          e.printStackTrace();
          fail();
        }
      }
    };
    t.start();

    // waiting for mi
    Thread.sleep(1000);

    assertEquals(1, midb.listManualInteractionEntries().size());
    assertEquals(xo.getId(), midb.listManualInteractionEntries().get(new Long(xo.getId())).getID().longValue());
    WorkflowStacktrace wfst = midb.listManualInteractionEntries().get(xo.getId()).getWfTrace();
    assertEquals(1, wfst.getEntries().size());
    assertEquals("bla", wfst.getEntries().get(0));
    //assertEquals(WorkflowInstanceStatus.MANUAL_INTERACTION, XynaFactory.getInstance().getProcessing().getXynaProcessingODS()
    //               .getWorkflowInstancesDatabase().getInstanceDetails(xo.getId()).getStatus());
    //mi beenden
    midb.processManualInteractionEntry(xo.getId(), new Container());
  }
  //todo test workflowstacktrace rootordertype
  
}
