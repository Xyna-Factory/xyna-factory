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

package com.gip.xyna.xprc.xprcods.orderarchive;



import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;

import com.gip.xyna.FileTestUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xnwh.persistence.memory.XynaMemoryPersistenceLayer;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;



public class OrderInstancesDatabaseTest extends TestCase {

  private OrderArchive wfidb;
  private ODSImpl ods;

  public void setUp() throws XynaException {

    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGenerator.getUniqueId()).andReturn(longGenerator.getAndIncrement()).anyTimes();
    EasyMock.replay(idGenerator);
    IDGenerator.setInstance(idGenerator);

    ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(42, XynaLocalMemoryPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test2", ODSConnectionType.HISTORY,
                                                 new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id);
        
    Configuration config = new Configuration();

    // XynaFactoryManagementODS
    XynaFactoryManagementODS xfods = EasyMock.createMock(XynaFactoryManagementODS.class);
    EasyMock.expect(xfods.getConfiguration()).andReturn(config).anyTimes();

    EasyMock.replay(xfods);

    DependencyRegister depReg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(depReg.addDependency(EasyMock.isA(DependencySourceType.class),
                                         EasyMock.isA(String.class),
                                         EasyMock.isA(DependencySourceType.class),
                                         EasyMock.isA(String.class))).andReturn(true).anyTimes();
    
    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getDependencyRegister()).andReturn(depReg).anyTimes();
    EasyMock.replay(xfctrl);
    
    XynaFactoryManagementBase xfm = EasyMock.createMock(XynaFactoryManagementBase.class);
    EasyMock.expect(xfm.getXynaFactoryManagementODS()).andReturn(xfods).anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    EasyMock.expect(xfm.getProperty((String) EasyMock.anyObject())).andReturn(null).anyTimes();
    EasyMock.replay(xfm);

    XynaProcessingODS pods = EasyMock.createMock(XynaProcessingODS.class);
    try {
      EasyMock.expect(pods.getODS()).andReturn(ods).anyTimes();
    } catch (Throwable e) {
      e.printStackTrace();
    }

    EasyMock.replay(pods);

    XynaProcessingBase xproc = EasyMock.createMock(XynaProcessingBase.class);
    EasyMock.expect(xproc.getXynaProcessingODS()).andReturn(pods).anyTimes();

    EasyMock.replay(xproc);

    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();
    xf.addComponentToBeInitializedLater((XynaFactoryComponent) EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(xf.getProcessing()).andReturn(xproc).anyTimes();
    EasyMock.expect(xf.getFutureExecution()).andReturn(new FutureExecution("test")).anyTimes();

    
    XynaFactory.setInstance(xf);
    EasyMock.replay(xf);

    config.init();
    
    OrderArchive.setDoPersistence(false);
    
    if (!new File("bin").exists()) {
      if (!new File("deploy").exists()) {
        Constants.SERVER_CLASS_DIR = "deploy/xynaserver.jar";
      } else {
        Constants.SERVER_CLASS_DIR = "classes";
      }
    }


     wfidb = new OrderArchive();
   //  wfidb.initInternally();
    //wfidb.init();

    // check that there are no wf instances after init since at the moment we
    // do not read from a persistence layer. Once we do that, the test needs
    // check the available information or delete it before testing the class
    assertEquals(0, wfidb.getAllInstances(0, Integer.MAX_VALUE).size());

  }


  public void tearDown() throws XynaException {
    IDGenerator.setInstance(null);
    wfidb.shutDownInternally();
    boolean gotException = false;
    try {
      wfidb.getAllInstances(0, Integer.MAX_VALUE).size();
    }
    catch (RuntimeException e) {
      gotException = true;
    }
    assertTrue("expected that wfidb to be not accessible any more", gotException);
    wfidb = null;

    assertTrue(FileTestUtils.deleteDir(new File(Constants.MDM_CLASSDIR)));
    assertTrue(FileTestUtils.deleteDir(new File(Constants.GENERATION_DIR)));
    
    ODSImpl.clearInstances();
  }

  public void testNormalCycle() throws XynaException {

    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testDestinationKey1"),
                                                               new Container());
    xo.setMonitoringLevel(MonitoringCodes.MASTER_WORKFLOW_MONITORING);

    wfidb.insert(xo);

    assertEquals(1, wfidb.getAllInstances(0, Integer.MAX_VALUE).size());
    assertEquals(xo.getDestinationKey().getOrderType(), wfidb.getCompleteOrder(xo.getId()).getOrderType());
    assertEquals(OrderInstanceStatus.INITIALIZATION, wfidb.getCompleteOrder(xo.getId()).getStatusAsEnum());
    assertNotNull(wfidb.getCompleteOrder(xo.getId()).getStartTime());
    assertNull(wfidb.getCompleteOrder(xo.getId()).getLastUpdate());

    // change the status to some non-final value (i.e., not FINISHED) so that the end time is not
    // set at this point
    wfidb.updateStatus(xo, OrderInstanceStatus.FINISHED_EXECUTION, null);
    assertEquals(OrderInstanceStatus.FINISHED_EXECUTION, wfidb.getCompleteOrder(xo.getId()).getStatusAsEnum());
    assertNotNull(wfidb.getCompleteOrder(xo.getId()).getStartTime());
    assertNull(wfidb.getCompleteOrder(xo.getId()).getLastUpdate());

    // after setting the status FINISHED, the end time is supposed to be set
    wfidb.updateStatus(xo, OrderInstanceStatus.FINISHED, null);

    assertEquals(OrderInstanceStatus.FINISHED, wfidb.getCompleteOrder(xo.getId()).getStatusAsEnum());
    assertNotNull(wfidb.getCompleteOrder(xo.getId()).getStartTime());
    assertNotNull(wfidb.getCompleteOrder(xo.getId()).getLastUpdate());

  }


  public void testErrorCycle() throws XynaException {

    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testDestinationKey2"),
                                                               new Container());
    xo.setMonitoringLevel(MonitoringCodes.ERROR_MONITORING);

    wfidb.insert(xo);

    // since the monitoring level is set to error, this is expected to return zero length list
    assertEquals(0, wfidb.getAllInstances(0, Integer.MAX_VALUE).size());

    // change the status to ERROR and provide an exception
    XynaException e = new XynaException("testErrorCode");
    xo.addException(e);
    wfidb.updateStatusOnError(xo, OrderInstanceStatus.XYNA_ERROR );

    // once an error has been reported, an instance is created
    assertEquals(1, wfidb.getAllInstances(0, Integer.MAX_VALUE).size());
    assertEquals(OrderInstanceStatus.XYNA_ERROR, wfidb.getCompleteOrder(xo.getId()).getStatusAsEnum());

    assertEquals(1, wfidb.getCompleteOrder(xo.getId()).getExceptions().size());
    assertEquals(e.getMessage(), wfidb.getCompleteOrder(xo.getId()).getExceptions().get(0).getMessage());
    assertEquals(e.getCode(), wfidb.getCompleteOrder(xo.getId()).getExceptions().get(0).getCode());

    // check that the newly created instance details contain as much information as possible but
    // do not pretend to know something about the start time
    assertEquals(-1, wfidb.getCompleteOrder(xo.getId()).getStartTime());
    assertNotNull(wfidb.getCompleteOrder(xo.getId()).getLastUpdate());

  }


  public void testNullRejection() throws XynaException {

    boolean failed = true;

    // test illegal argument exception when adding null
    try {
      wfidb.insert(null);
    } catch (IllegalArgumentException e) {
      failed = false;
    }

    if (failed) {
      fail("No exception caught when adding null");
    }

    // test illegal argument exception when setting status to null
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testDestinationKey3"),
                                                               new Container());
    xo.setMonitoringLevel(MonitoringCodes.STEP_MONITORING);
    wfidb.insert(xo);

    failed = true;
    try {
      wfidb.updateStatus(null, OrderInstanceStatus.WAITING_FOR_VETO, null);
    } catch (IllegalArgumentException e) {
      failed = false;
    }

    if (failed) {
      fail("No exception caught when adding null");
    }

    // test return null when requesting id null 
    // xxxxxxxxxx derzeit kann man nur long übergeben! xxxxxxxxxxxx
/*    boolean gotException = false;
    try {
      wfidb.getCompleteOrder(-1);
    }
    catch (PersistenceLayerException e) {
      gotException = true;
    }
    assertTrue("expected exception when querying with id=null", gotException);*/
  }
  
  private void executeUndeploymentCopy(String wfFqClassName) throws XynaException {
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ODSConnection con = ods.openConnection();
    try {
      String tableName = new OrderInstance().getTableName();
      Query<OrderInstanceDetails> q = new Query<OrderInstanceDetails>("select * from "
                      + tableName + " where " + OrderInstanceColumn.C_ORDER_TYPE.toString()
                      + "=?", new OrderInstanceDetails(-1l).getReader(), tableName);
      PreparedQuery<OrderInstanceDetails> getAllInstancesWithOrderType = con.prepareQuery(q);

      List<OrderInstanceDetails> instances = con.query(getAllInstancesWithOrderType, new Parameter(wfFqClassName), -1);

      for (OrderInstanceDetails wid : instances) {
        wid.convertAuditDataToXML(VersionManagement.REVISION_WORKINGSET, false);
        wid.clearAuditDataJavaObjects();
        con.persistObject(wid);
      }
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  public void testUndeployment() throws XynaException {
    
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testDestinationKey1"),
                                                               new Container());
    xo.setMonitoringLevel(MonitoringCodes.MASTER_WORKFLOW_MONITORING);

    wfidb.insert(xo);

    wfidb.updateStatus(xo, OrderInstanceStatus.FINISHED_EXECUTION, null);

    wfidb.updateStatus(xo, OrderInstanceStatus.FINISHED, null);

    
    // read them self to get the non finalized Version
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ODSConnection con = ods.openConnection();
    OrderInstanceDetails deployedValues;
    try {
      deployedValues = new OrderInstanceDetails(xo.getId());
      con.queryOneRow(deployedValues);
      con.commit();
    } finally {
      con.closeConnection();
    }

    // finalize all orders from that WF
    executeUndeploymentCopy("testDestinationKey1");

    // save finalized Values
    con = ods.openConnection();
    OrderInstanceDetails finalizedValues;
    try {
      finalizedValues = new OrderInstanceDetails(xo.getId());
      con.queryOneRow(finalizedValues);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    //compare some stuff
    assertEquals(deployedValues.getParentId(), finalizedValues.getParentId());
    assertEquals(deployedValues.getOrderType(), finalizedValues.getOrderType());
    assertEquals(deployedValues.getStartTime(), finalizedValues.getStartTime());
    assertEquals(deployedValues.getStatusAsEnum(), finalizedValues.getStatusAsEnum());
    assertEquals(deployedValues.getExceptions() == null ? null : (Object) deployedValues.getExceptions().size(),
                 finalizedValues.getExceptions() == null ? null : (Object) finalizedValues.getExceptions().size());
    
    assertNotNull(deployedValues.getAuditDataAsJavaObject());
    assertNull(finalizedValues.getAuditDataAsJavaObject());
    
 /*   assertEquals(true, deployedValues.getMapRefIdToStepId() != null);
    assertEquals(true, deployedValues.getParameterPostStepTimes() != null);
    assertEquals(true, deployedValues.getParameterPostStepValues() != null);
    assertEquals(true, deployedValues.getParameterPreStepTimes() != null);
    assertEquals(true, deployedValues.getParameterPreStepValues() != null);
    assertEquals("XML representation containing instance variables has not been calculated!", deployedValues.getWorkflowStatusXML());

    
    assertEquals(false, finalizedValues.getMapRefIdToStepId() != null);
    assertEquals(false, finalizedValues.getParameterPostStepTimes() != null);
    assertEquals(false, finalizedValues.getParameterPostStepValues() != null);
    assertEquals(false, finalizedValues.getParameterPreStepTimes() != null);
    assertEquals(false, finalizedValues.getParameterPreStepValues() != null);
    assertEquals("", finalizedValues.getWorkflowStatusXML());*/

    
    //those dirs shouldn't be deleted in between tests
    assertTrue(FileTestUtils.deleteDir(new File(Constants.STORAGE_PATH)));
    assertTrue(FileTestUtils.deleteDir(new File(XynaProperty.PERSISTENCE_DIR)));
  }

}
