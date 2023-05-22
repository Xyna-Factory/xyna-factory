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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.capacities.CMAbstract;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;

import junit.framework.TestCase;


public class CapacityManagementTest extends TestCase {

  private CapacityManagement cm;

  protected void setUp() throws XynaException {

    XynaScheduler xsched = EasyMock.createMock(XynaScheduler.class);

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

    //fexec.execAsync(EasyMock.isA(FutureExecutionTask.class));
    //EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(fexec);
    
    DependencyRegister depReg = EasyMock.createMock(DependencyRegister.class);
    EasyMock.expect(
                    depReg.addDependency(EasyMock.isA(DependencySourceType.class), EasyMock.isA(String.class), EasyMock
                                    .isA(DependencySourceType.class), EasyMock.isA(String.class))).andReturn(true)
                    .anyTimes();
    EasyMock.replay(depReg);

    XynaFactoryControl xfctrl = EasyMock.createMock(XynaFactoryControl.class);
    EasyMock.expect(xfctrl.getDependencyRegister()).andReturn(depReg).anyTimes();
    EasyMock.replay(xfctrl);

    XynaFactoryManagement xfm = EasyMock.createMock(XynaFactoryManagement.class);
    EasyMock.expect(xfm.getProperty(EasyMock.isA(String.class))).andReturn("false").anyTimes();
    EasyMock.expect(xfm.getXynaFactoryControl()).andReturn(xfctrl).anyTimes();
    EasyMock.replay(xfm);
    
    XynaFactory xf = EasyMock.createMock(XynaFactory.class); //erster factorymock, damit der idgenerator initialisiert werden kann
    XynaFactory.setInstance(xf);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.replay(xf);

    IDGenerator idGenerator = EasyMock.createMock(IDGenerator.class);
    final AtomicLong longGenerator = new AtomicLong(1);
    EasyMock.expect(idGenerator.getUniqueId()).andAnswer(new IAnswer<Long>() {
      public Long answer() throws Throwable {
        return longGenerator.getAndIncrement();
      }
    }).anyTimes();
    EasyMock.replay(idGenerator);
    IDGenerator.setInstance(idGenerator);

    xf = EasyMock.createMock(XynaFactory.class); //neuer mock weil der alte bereits replayed wurde
    XynaFactory.setInstance(xf);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fexec).anyTimes();

    xf.addComponentToBeInitializedLater(EasyMock.isA(CapacityManagement.class));
    EasyMock.expectLastCall();
    EasyMock.expect(xf.getFactoryManagement()).andReturn(xfm).anyTimes();
    EasyMock.expect(xf.getFactoryManagementPortal()).andReturn(xfm).anyTimes();

    EasyMock.expect(xf.getIDGenerator()).andReturn(idGenerator).anyTimes();
    GetProcessingAnswer getProcessingAnswer = new GetProcessingAnswer();
    EasyMock.expect(xf.getProcessing()).andAnswer(getProcessingAnswer).anyTimes();
    EasyMock.replay(xf);

    
    ODS ods = ODSImpl.getInstance(false);
    ods.registerPersistenceLayer(42, XynaLocalMemoryPersistenceLayer.class);
    long id = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test",
                                                      ODSConnectionType.DEFAULT, new String[0]);
    long id2 = ods.instantiatePersistenceLayerInstance(ods.getMemoryPersistenceLayerID(), "test2",
                                                      ODSConnectionType.HISTORY, new String[0]);
    ods.setDefaultPersistenceLayer(ODSConnectionType.DEFAULT, id);
    ods.setDefaultPersistenceLayer(ODSConnectionType.HISTORY, id2);
    
    XynaProcessingODS xpods = EasyMock.createMock(XynaProcessingODS.class);
    EasyMock.expect(xpods.getODS()).andReturn(ods).anyTimes();
    EasyMock.replay(xpods);
    
    
    XynaProcessing xprc = EasyMock.createMock(XynaProcessing.class);
    getProcessingAnswer.myBase = xprc;
    EasyMock.expect(xprc.getXynaScheduler()).andReturn(xsched).anyTimes();
    EasyMock.expect(xprc.getXynaProcessingODS()).andReturn(xpods).anyTimes();
    EasyMock.replay(xprc);
    
    
    try {
      cm = new CapacityManagement();
      // the capacity management needs to be initialized manually since usually it is initialized after the scheduler as
      // it registers itself as a late initializer component
      cm.initInternally();
    } catch (XynaException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  


  
  private static class GetProcessingAnswer implements IAnswer<XynaProcessingBase> {
    
    public volatile XynaProcessingBase myBase = null;
    
    public XynaProcessingBase answer() throws Throwable {
      return myBase;
    }
    
  }

  protected void tearDown() throws XynaException {
    cm.removeAllCapacities();
    ODSImpl.getInstance(false).clearPreparedQueryCache();
    ODSImpl.clearInstances();
  }


  public void testAddCapacity() {
    int capNum = cm.listCapacities().size();

    try {
      cm.addCapacity("testCapacity1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCapacity2", 200, CapacityManagement.State.DISABLED);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    Collection<CapacityInformation> caps = cm.listCapacities();

    assertEquals("Could not verify the number of added capacities", capNum + 2, caps.size());

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCapacity2")) {
        assertEquals("Could not verify cardinality of added capacity", 200, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        // fail("Found unknown name when checking for added capacities");
      }
    }

  }


  public void testRemoveAll() throws XynaException {
    // int capNum = cm.listCapacities().size();

    try {
      cm.addCapacity("testCapacity1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCapacity2", 200, CapacityManagement.State.ACTIVE);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    cm.removeAllCapacities();

    Collection<CapacityInformation> caps = cm.listCapacities();
    assertEquals("Not all capacities were gone after removing them", 0, caps.size());

  }


  public void testRemoveCapacity() throws XynaException {
    int capNum = cm.listCapacities().size();
    try {
      // check two entries to make sure that it does not just work for the special case with one capacity
      cm.addCapacity("testCapacity1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCapacity2", 200, CapacityManagement.State.DISABLED);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    // check again that both entries have arrived
    Collection<CapacityInformation> caps = cm.listCapacities();
    assertEquals("Could not verify the number of added capacities", capNum + 2, caps.size());

    // remove one
    cm.removeCapacity("testCapacity1");

    caps = cm.listCapacities();
    assertEquals("Could not verify the number of defined capacities after removing 1 of 2", capNum + 1, caps.size());
    assertEquals("Could not verify cardinality of remaining capacity", 200, caps.iterator().next().getCardinality());
    assertEquals("Could not verify state of remaining capacity", CapacityManagement.State.DISABLED, caps.iterator()
        .next().getState());

    // remove the second
    cm.removeCapacity("testCapacity2");

    caps = cm.listCapacities();
    assertEquals("Could not verify the number of defined capacities after removing 2 of 2", capNum, caps.size());

  }


  public void testInvalidParameterRejections() {

    boolean failed = false;

    try {
      cm.addCapacity(null, 1, CapacityManagement.State.ACTIVE);
      failed = true;
    } catch (IllegalArgumentException e) {
      // this is supposed to happen so do nothing
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Unexpected error while adding capacity: " + e.getMessage());
    }

    assertFalse(CapacityManagement.class.getSimpleName() + " did not reject name <null>", failed);

    try {
      cm.addCapacity("testCapacity", -1, CapacityManagement.State.ACTIVE);
      failed = true;
    } catch (IllegalArgumentException e) {
      // this is supposed to happen so do nothing
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Unexpected error while adding capacity: " + e.getMessage());
    }

    assertFalse(CapacityManagement.class.getSimpleName() + " did not negative cardinality ", failed);

  }


  public void testChangeCapacityName() throws XynaException {
    int capNum = cm.listCapacities().size();
    try {
      cm.addCapacity("testCapacity1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCapacity2", 200, CapacityManagement.State.DISABLED);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    // Check that renaming to an existing name is not allowed
    try {
      cm.changeCapacityName("testCapacity1", "testCapacity2");
      fail(CapacityManagement.class.getSimpleName() + " did not reject changing the name to an already existing value");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Verify that there really have been no changes
    Collection<CapacityInformation> caps = cm.listCapacities();
    assertEquals("Could not verify the number of added capacities", capNum + 2, caps.size());

    for (CapacityInformation cap : caps) {
      System.out.println("cap:"+cap.getName());
      if (cap.getName().equals("testCapacity1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCapacity2")) {
        assertEquals("Could not verify cardinality of added capacity", 200, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // Now request a legitimate new name
    if (!cm.changeCapacityName("testCapacity1", "testCapacity1New")) {
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate renaming request");
    }
    try {
      cm.changeCapacityName("testCapacity2", "testCapacity1New");
      fail(CapacityManagement.class.getSimpleName() + " did not reject renaming to already existing value");
    } catch (IllegalArgumentException e) {
      // expected
    }
    if (!cm.changeCapacityName("testCapacity2", "testCapacity2New")) {
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate renaming request");
    }

    // Verify that the changes have taken place
    caps = cm.listCapacities();
    assertEquals("Could not verify the number of added capacities", capNum + 2, caps.size());

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity1New")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCapacity2New")) {
        assertEquals("Could not verify cardinality of added capacity", 200, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

  }


  public void testChangeCapacityCardinalityAndState() throws XynaException {
    int capNum = cm.listCapacities().size();
    try {
      cm.addCapacity("testCapacity1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCapacity2", 200, CapacityManagement.State.DISABLED);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    // Check that changing to negative cardinality is rejected
    try {
      cm.changeCardinality("testCapacity1", -1);
      fail(CapacityManagement.class.getSimpleName() + " did not reject negative cardinality");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Verify that there really have been no changes
    Collection<CapacityInformation> caps = cm.listCapacities();
    assertEquals("Could not verify the number of added capacities", 2, caps.size());

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCapacity2")) {
        assertEquals("Could not verify cardinality of added capacity", 200, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // Now request a legitimate new name
    if (!cm.changeCardinality("testCapacity1", 101))
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate renaming request");
    if (!cm.changeCardinality("testCapacity2", 201))
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate renaming request");

    // Verify that the changes have taken place
    caps = cm.listCapacities();
    assertEquals("Could not verify the number of added capacities", capNum + 2, caps.size());

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity1")) {
        assertEquals("Could not verify cardinality of added capacity", 101, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCapacity2")) {
        assertEquals("Could not verify cardinality of added capacity", 201, cap.getCardinality());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

  }

  private CapacityAllocationResult allocate(XynaOrderServerExtension xo) {
    return cm.allocateCapacities(new OrderInformation(xo), xo.getSchedulingData());
  }

  public void testCapacityAllocation() throws PersistenceLayerException {

    // create a XynaOrder to allocate capacities
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    // create a scheduler bean that defines what capacities are required
    ArrayList<Capacity> requestedCaps = new ArrayList<Capacity>();
    Capacity cap1 = new Capacity("testCap1", 30);
    Capacity cap2 = new Capacity("testCap2", 50);
    requestedCaps.add(cap1);
    requestedCaps.add(cap2);
    SchedulerBean bean = new SchedulerBean(requestedCaps);

    xo.setSchedulerBean(bean);

    // before the capacities have been defined, they may not be allocated
    if (allocate(xo).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " did not reject capacity allocation for undefined capacities");

    try {
      cm.addCapacity("testCap1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCap2", 50, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCap3", 75, CapacityManagement.State.ACTIVE);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Unexpected error when adding capacity: " + e.getMessage());
    }

    // after they are defined, they have to be allocated
    if (!allocate(xo).isAllocated()) {
      fail(CapacityManagement.class.getSimpleName() + " rejected capacity allocation even though they are available");
    }

    // verify the correct usage numbers
    Collection<CapacityInformation> caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCap1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 30, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap2")) {
        assertEquals("Could not verify cardinality of added capacity", 50, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap3")) {
        assertEquals("Could not verify cardinality of added capacity", 75, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    try {
      allocate(xo);
      fail("Second capacity allocation did not fail");
    } catch (RuntimeException e) {
      assertTrue("unexpected error message",
                 e.getMessage().startsWith(CMAbstract.TRIED_TO_AQUIRE_TWICE_EXCEPTION_MESSAGE));
    }

    // Use a second order
    XynaOrderServerExtension xo2 = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    // create a scheduler bean that defines what capacities are required
    ArrayList<Capacity> requestedCaps2 = new ArrayList<Capacity>();
    Capacity cap1_2 = new Capacity("testCap3", 76);
    requestedCaps2.add(cap1_2);
    SchedulerBean bean2 = new SchedulerBean(requestedCaps2);

    xo2.setSchedulerBean(bean2);

    if (allocate(xo2).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " did not reject capacity allocation for overbooked capacities");

    cap1_2.setCardinality(75);

    if (!allocate(xo2).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate capacity allocation");

  }


  public void testBlockedCapacitites() {

    // Try a third order that uses a blocked capacity
    XynaOrderServerExtension xo3 = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    // create a scheduler bean that defines what capacities are required
    ArrayList<Capacity> requestedCaps3 = new ArrayList<Capacity>();
    Capacity cap1_3 = new Capacity("testCap4", 5);
    requestedCaps3.add(cap1_3);
    SchedulerBean bean3 = new SchedulerBean(requestedCaps3);

    xo3.setSchedulerBean(bean3);


    try {
      cm.addCapacity("testCap4", 10, CapacityManagement.State.DISABLED);
    } catch (XynaException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    // verify the correct usage numbers
    Collection<CapacityInformation> caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCap4")) {
        assertEquals("Could not verify cardinality of added capacity", 10, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.DISABLED, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // Check rejection because of the state
    if (allocate(xo3).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " did not reject allocation of blocked capacity");

  }


  public void testFreeCapacitites() throws PersistenceLayerException {

    // create a XynaOrder to allocate capacities
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    // create a scheduler bean that defines what capacities are required
    ArrayList<Capacity> requestedCaps = new ArrayList<Capacity>();
    Capacity cap1 = new Capacity("testCap1", 30);
    Capacity cap2 = new Capacity("testCap2", 50);
    requestedCaps.add(cap1);
    requestedCaps.add(cap2);
    SchedulerBean bean = new SchedulerBean(requestedCaps);
    xo.setSchedulerBean(bean);

    // Use a second order
    XynaOrderServerExtension xo2 = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    // create a scheduler bean that defines what capacities are required
    ArrayList<Capacity> requestedCaps2 = new ArrayList<Capacity>();
    Capacity cap1_2 = new Capacity("testCap3", 70);
    requestedCaps2.add(cap1_2);
    SchedulerBean bean2 = new SchedulerBean(requestedCaps2);
    xo2.setSchedulerBean(bean2);


    try {
      cm.addCapacity("testCap1", 100, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCap2", 50, CapacityManagement.State.ACTIVE);
      cm.addCapacity("testCap3", 75, CapacityManagement.State.ACTIVE);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Unexpected error when adding capacity: " + e.getMessage());
    }

    // after they are defined, they have to be allocated
    if (!allocate(xo).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " rejected capacity allocation even though they are available");
    if (!allocate(xo2).isAllocated())
      fail(CapacityManagement.class.getSimpleName() + " rejected legitimate capacity allocation");

    // verify the correct usage numbers
    Collection<CapacityInformation> caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCap1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 30, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap2")) {
        assertEquals("Could not verify cardinality of added capacity", 50, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap3")) {
        assertEquals("Could not verify cardinality of added capacity", 75, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 70, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    cm.freeCapacities(xo);

    // verify the correct usage numbers
    caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCap1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap2")) {
        assertEquals("Could not verify cardinality of added capacity", 50, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap3")) {
        assertEquals("Could not verify cardinality of added capacity", 75, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 70, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // check that only the correct capacities are cleared
    try {
      allocate(xo2);
    } catch (RuntimeException e) {
      assertTrue("unexpected error message",
                 e.getMessage().startsWith(CMAbstract.TRIED_TO_AQUIRE_TWICE_EXCEPTION_MESSAGE));
    }

    cm.freeCapacities(xo2);

    // verify the correct usage numbers
    caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCap1")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap2")) {
        assertEquals("Could not verify cardinality of added capacity", 50, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else if (cap.getName().equals("testCap3")) {
        assertEquals("Could not verify cardinality of added capacity", 75, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 0, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }
  }


  // @Bug 8401
  public void testRemoveRenameReduceUsedCapacity() throws XynaException {
    try {
      // create capacity with 100 slots
      cm.addCapacity("testCapacity", 100, CapacityManagement.State.ACTIVE);
    } catch (XynaException e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }

    // assign 50 slots of the capacity
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));

    ArrayList<Capacity> requestedCaps = new ArrayList<Capacity>();
    Capacity testCap = new Capacity("testCapacity", 50);
    requestedCaps.add(testCap);
    SchedulerBean bean = new SchedulerBean(requestedCaps);

    xo.setSchedulerBean(bean);
    allocate(xo);

    Collection<CapacityInformation> caps = cm.listCapacities();
    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // try to remove the capacity
    if (cm.removeCapacity("testCapacity")) {
      fail("A capacity that was in use got deleted");
    }

    caps = cm.listCapacities();
    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // try to rename the capacity
    if (cm.changeCapacityName("testCapacity", "testCap")) {
      fail("A capacity that was in use got renamed");
    }

    caps = cm.listCapacities();
    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }

    // try to reduce cardinality below freeSlots
    try {
      cm.changeCardinality("testCapacity", 25);
      fail("A capacity that was in use got reduced below it's freeSlots");
    } catch( XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState e) {
    }

    caps = cm.listCapacities();

    for (CapacityInformation cap : caps) {
      if (cap.getName().equals("testCapacity")) {
        assertEquals("Could not verify cardinality of added capacity", 100, cap.getCardinality());
        assertEquals("Could not verify used entries of capacity", 50, cap.getInuse());
        assertEquals("Could not verify state of added capacity", CapacityManagement.State.ACTIVE, cap.getState());
      } else {
        fail("Found unknown name when checking for added capacities after rejected attempt to change the name");
      }
    }
  }
  
  
    
/*  
  public void testVetoRollbackDuringCapacityAquisition() throws PersistenceLayerException {
    String DISABLED_CAPACITY_NAME = "testCapacity";
    try {
      cm.addCapacity(DISABLED_CAPACITY_NAME, 10, CapacityManagement.State.DISABLED);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Could not add capacity: " + e.getMessage());
    }
    
    XynaOrderServerExtension blockedXo = new XynaOrderServerExtension(new DestinationKey("blockingOrder"));
    List<Veto> requestedVetos = new ArrayList<Veto>();
    requestedVetos.add(new Veto("aquireableTestVeto1"));
    requestedVetos.add(new Veto("aquireableTestVeto2"));
    requestedVetos.add(new Veto("aquireableTestVeto3"));
    List<Capacity> requestedCapacities = new ArrayList<Capacity>();
    requestedCapacities.add(new Capacity(DISABLED_CAPACITY_NAME, Integer.MAX_VALUE));
    SchedulerBean bean = new SchedulerBean(requestedCapacities, requestedVetos);
    blockedXo.setSchedulerBean(bean);
    
    CapacityAllocationResult result = cm.allocateCapacitiesAndVetos(blockedXo);
    assertFalse("Allocation of veto should not have succeded", result.isAllocated());
    assertEquals("Result should have contained the unallocatable resource", DISABLED_CAPACITY_NAME, result.getCapName());
    
    Collection<VetoInformationStorable> vetos = cm.listVetos();
    assertEquals("Vetos should be empty if capacity could not be aquired", 0, vetos.size());
  }
 */ 
    
  
}
