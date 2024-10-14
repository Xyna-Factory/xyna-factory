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
package com.gip.xyna.xprc.xsched;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.mock.XynaFactoryMock;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.Veto;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VM_SeparateThread;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheProcessor;

import junit.framework.TestCase;


/**
 *
 *
 */
public class VetoManagementTest extends TestCase {
  
  private VetoManagement vm;
  private VetoCacheProcessor vcpa;
  private VetoCache vetoCache;
  
  
  protected void setUp() throws XynaException {
    XynaFactoryMock xfm = new XynaFactoryMock();
    
    VetoManagement.VM_ALGORITHM_TYPE.set(VetoManagementAlgorithmType.SeparateThread);
    vm = new VetoManagement();
    vm.init();
    
    xfm.startFutureExecution(XynaClusteringServicesManagement.class, XynaProperty.class, PersistenceLayerInstances.class);
    
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      vetoCache = ((VM_SeparateThread)vm.getVMAlgorithm()).getVetoCache();
      
      //vcpa = ((VM_SeparateThread)vm.getVMAlgorithm()).getVetoCache().getVetoCacheProcessorAlgorithm();
     
    }
  }
  
  private SchedulerBean createSchedulerBean(List<Veto> requestedVetos) {
    SchedulerBean bean = new SchedulerBean(Collections.<Capacity>emptyList(), requestedVetos);
    return bean;
  }
  
  private VetoAllocationResult allocateVetos(XynaOrderServerExtension xo) {
    OrderInformation orderInformation = new OrderInformation(xo);
    List<String> vetos = xo.getSchedulingData().getVetos();
    long urgency = 1L;
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      VetoAllocationResult var = vm.allocateVetos(orderInformation, vetos, urgency); //erstes Scheduling: Aufnahme als New
      if( ! var.isAllocated() ) {
        sleep(50); //kurz auf VetoCacheProcessor warten
        var = vm.allocateVetos(orderInformation, vetos, urgency); //eigentliches Scheduling
      }
      if( var.isAllocated() ) {
        vm.finalizeAllocation(orderInformation, vetos);
      }
      return var;
    } else {
      return vm.allocateVetos(orderInformation, vetos, urgency);
    }
  }

  private boolean freeVetos(XynaOrderServerExtension xo) {
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      boolean result = vm.freeVetos(xo);
      sleep(50); //kurz auf VetoCacheProcessor warten
      return result;
    } else {
      return vm.freeVetos(xo);
    }
  }
  
  private boolean forceFreeVetos(long orderid) {
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      boolean result = vm.forceFreeVetos(orderid);
      sleep(50); //kurz auf VetoCacheProcessor warten
      return result;
    } else {
      return vm.forceFreeVetos(orderid);
    }
  }
  
  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //ignorieren
    }
  }

  private void checkVetoCacheFree() {
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      assertEquals( "{}", vetoCache.toString() );
    }
  }

  private void schedule() {
    if( VetoManagement.VM_ALGORITHM_TYPE.get() == VetoManagementAlgorithmType.SeparateThread ) {
      vetoCache.beginScheduling(37);
      vetoCache.endScheduling();
      sleep(50); //kurz auf VetoCacheProcessor warten
    }
  }

  public void testVetoAquisitionAndRevoke() {
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));
    Long orderId = xo.getId();
    ArrayList<Veto> requestedVetos = new ArrayList<Veto>();
    Veto testVeto = new Veto("TestVeto");
    requestedVetos.add(testVeto);
    SchedulerBean bean = createSchedulerBean(requestedVetos);
    xo.setSchedulerBean(bean);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    VetoAllocationResult result = allocateVetos( xo );
    assertTrue("Allocation of veto should have been succesfull", result.isAllocated());
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", orderId, vetos.iterator().next().getUsingOrderId());
    
    assertTrue("Freeing of caps and vetos should have suceeded", freeVetos(xo));
    vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    checkVetoCacheFree();
  }
  
  

  

  public void testBlockingVeto() {
    String VETONAME = "BlockingVeto";
    XynaOrderServerExtension blockingXo = new XynaOrderServerExtension(new DestinationKey("blockingOrder"));
    Long blockingId = blockingXo.getId();
    ArrayList<Veto> requestedVetos = new ArrayList<Veto>();
    Veto blockingVeto = new Veto(VETONAME);
    requestedVetos.add(blockingVeto);
    SchedulerBean bean = createSchedulerBean(requestedVetos);
    blockingXo.setSchedulerBean(bean);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    VetoAllocationResult result = allocateVetos(blockingXo);
    assertTrue("Allocation of veto should have been succesfull", result.isAllocated());
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", blockingId, vetos.iterator().next().getUsingOrderId());
    
    XynaOrderServerExtension blockedXo = new XynaOrderServerExtension(new DestinationKey("blockedOrder"));
    
    blockedXo.setSchedulerBean(bean); //same SchedulerBean as 'blockingOrder'
    result = allocateVetos(blockedXo);
    assertFalse("Allocation of veto should not have succeded", result.isAllocated());
    
    vetos = vm.listVetos(); //still hold by blockingOrder?
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", blockingId, vetos.iterator().next().getUsingOrderId());
    
    assertTrue("Freeing of caps and vetos should have suceeded", freeVetos(blockingXo));
    vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    schedule();
    schedule();
    checkVetoCacheFree();
  }
  
  
  public void testVetoRollbackDuringVetoAquisition() {
    String VETONAME = "BlockingVeto";
    XynaOrderServerExtension blockingXo = new XynaOrderServerExtension(new DestinationKey("blockingOrder"));
    Long blockingId = blockingXo.getId();
    ArrayList<Veto> requestedVetos = new ArrayList<Veto>();
    Veto blockingVeto = new Veto(VETONAME);
    requestedVetos.add(blockingVeto);
    SchedulerBean bean = createSchedulerBean(requestedVetos);
    blockingXo.setSchedulerBean(bean);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    VetoAllocationResult result = allocateVetos(blockingXo);
    assertTrue("Allocation of veto should have been succesfull", result.isAllocated());
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", blockingId, vetos.iterator().next().getUsingOrderId());
    assertEquals("Blocking Veto ist not used", VETONAME, vetos.iterator().next().getName() );
    
    XynaOrderServerExtension blockedXo = new XynaOrderServerExtension(new DestinationKey("blockedOrder"));
    
    requestedVetos = new ArrayList<Veto>();
    requestedVetos.add(new Veto("aquireableTestVeto1"));
    requestedVetos.add(new Veto("aquireableTestVeto2"));
    requestedVetos.add(new Veto("aquireableTestVeto3"));
    requestedVetos.add(blockingVeto);
    bean = createSchedulerBean(requestedVetos);
    blockedXo.setSchedulerBean(bean);
    
    result = allocateVetos(blockedXo);
    assertFalse("Allocation of veto should not have succeded", result.isAllocated());
    
    vetos = vm.listVetos(); // aquireableTestVetos should not be acquired
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", blockingId, vetos.iterator().next().getUsingOrderId());
    
    assertTrue("Freeing of caps and vetos should have suceeded", freeVetos(blockingXo));
    vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    schedule();
    schedule();
    checkVetoCacheFree();
  }
  
  /*
  public void testVetoRollbackDuringCapacityAquisition() throws PersistenceLayerException {
    String DISABLED_CAPACITY_NAME = "testCapacity";
    try {
      vm.addCapacity(DISABLED_CAPACITY_NAME, 10, CapacityManagement.State.DISABLED);
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
    
    VetoAllocationResult result = vm.allocateVetos(blockedXo);
    assertFalse("Allocation of veto should not have succeded", result.isAllocated());
    assertEquals("Result should have contained the unallocatable resource", DISABLED_CAPACITY_NAME, result.getCapName());
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should be empty if capacity could not be aquired", 0, vetos.size());
  }
  */
  
  public void testForceFreeVeto() {
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey("testXynaOrder"));
    Long orderId = xo.getId();
    ArrayList<Veto> requestedVetos = new ArrayList<Veto>();
    Veto testVeto = new Veto("TestVeto");
    requestedVetos.add(testVeto);
    SchedulerBean bean = createSchedulerBean(requestedVetos);
    xo.setSchedulerBean(bean);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    VetoAllocationResult result = allocateVetos(xo);
    assertTrue("Allocation of veto should have been succesfull", result.isAllocated());
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Using orderId did not match created testOrder", orderId, vetos.iterator().next().getUsingOrderId());
    
    assertTrue("Freeing of caps and vetos should have freed a veto", forceFreeVetos(orderId));
    vetos = vm.listVetos();
    assertEquals("Vetos should be empty after forceFreeCapacitiesAndVetos", 0, vetos.size());
    
    checkVetoCacheFree();
  }

  public void testAdministrativeVeto() throws PersistenceLayerException {
    checkVetoCacheFree();
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    
    AdministrativeVeto adminVeto = new AdministrativeVeto("AV", "for testing");
    try {
      vm.allocateAdministrativeVeto(adminVeto);
    } catch (XPRC_AdministrativeVetoAllocationDenied e) {
      fail("Unexpected exception "+e);
    }
   
    try {
      vm.allocateAdministrativeVeto(new AdministrativeVeto("AV", "failed") );
      fail("Expected exception XPRC_AdministrativeVetoAllocationDenied" );
    } catch (XPRC_AdministrativeVetoAllocationDenied e) {
      assertEquals( "Das Veto 'AV' konnte nicht administrativ belegt werden, da es bereits von Auftrag -1 gehalten wird.", e.getMessage() );
    }
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Veto name does not match created AdministrativeVeto", adminVeto.getName(), vetos.iterator().next().getName() );
    assertEquals("Veto documentation does not match created AdministrativeVeto", adminVeto.getDocumentation(), vetos.iterator().next().getDocumentation() );
    
    try {
      vm.setDocumentationOfAdministrativeVeto(new AdministrativeVeto("AV", "changed") );
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      fail("Unexpected exception "+e);
      assertEquals( "Das Veto 'AV' konnte nicht administrativ belegt werden, da es bereits von Auftrag -1 gehalten wird.", e.getMessage() );
    }
    try {
      vm.setDocumentationOfAdministrativeVeto(new AdministrativeVeto("NONE", "changed") );
      fail("Expected exception XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY" );
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      assertEquals( "No object found with primarykey 'NONE' in table 'vetos'", e.getMessage() );
    }
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Veto name does not match create AdministrativeVeto name", adminVeto.getName(), vetos.iterator().next().getName() );
    assertEquals("Veto name does not match create AdministrativeVeto name", "changed", vetos.iterator().next().getDocumentation() );
   
    try {
      vm.freeAdministrativeVeto(adminVeto);
    } catch (XPRC_AdministrativeVetoDeallocationDenied e) {
      fail("Unexpected exception "+e);
    }
    
    try {
      vm.freeAdministrativeVeto(new AdministrativeVeto("NONE", "failed"));
      fail("Expected exception XPRC_AdministrativeVetoDeallocationDenied" );
    } catch (XPRC_AdministrativeVetoDeallocationDenied e) {
      assertEquals( "The Veto 'NONE' could not be deallocated, it is either not held administratively or not held at all.", e.getMessage() );
    }
    
    checkVetoCacheFree();
  }
 

  public void testAdministrativeVeto_Veto() throws PersistenceLayerException {
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    AdministrativeVeto adminVeto = new AdministrativeVeto("AV", "for testing");
    try {
      vm.allocateAdministrativeVeto(adminVeto);
    } catch (XPRC_AdministrativeVetoAllocationDenied e) {
      fail("Unexpected exception "+e);
    }

    XynaOrderServerExtension blockedXo = new XynaOrderServerExtension(new DestinationKey("blockedOrder"));
    
    ArrayList<Veto> requestedVetos = new ArrayList<Veto>();
    requestedVetos.add(new Veto("aquireableTestVeto"));
    requestedVetos.add(new Veto("AV")) ;
    SchedulerBean bean = createSchedulerBean(requestedVetos);
    blockedXo.setSchedulerBean(bean);
    
    VetoAllocationResult result = allocateVetos(blockedXo);
    assertFalse("Allocation of veto should not have succeded", result.isAllocated());
    
    vetos = vm.listVetos();
    assertEquals("Vetos should have been contained", 1, vetos.size());
    assertEquals("Veto name does not match created AdministrativeVeto", adminVeto.getName(), vetos.iterator().next().getName() );
    assertEquals("Veto documentation does not match created AdministrativeVeto", adminVeto.getDocumentation(), vetos.iterator().next().getDocumentation() );

    
    System.out.println("Free");
    try {
      vm.freeAdministrativeVeto(adminVeto);
      if( vetoCache != null ) {
        vetoCache.beginScheduling(123);
      }
    } catch (XPRC_AdministrativeVetoDeallocationDenied e) {
      fail("Unexpected exception "+e);
    }
    
    result = allocateVetos(blockedXo); //nötig, da Scheduler hier nicht richtig funktioniert 
    assertTrue("Allocation of veto should have succeded", result.isAllocated());
    
    vetos = vm.listVetos();
    List<VetoInformation> vetoList = new ArrayList<VetoInformation>(vetos);
    assertEquals("Vetos should have been contained", 2, vetoList.size());
    
    assertEquals("Using orderId did not match created testOrder", (Long)blockedXo.getId(), vetoList.get(0).getUsingOrderId());
    assertEquals("Using orderId did not match created testOrder", (Long)blockedXo.getId(), vetoList.get(1).getUsingOrderId());
    
    String allocatedVetos = sortStrings( " ", vetoList.get(0).getName(), vetoList.get(1).getName());
    assertEquals("AV aquireableTestVeto", allocatedVetos );
    
    System.out.println("Free2");
    assertTrue("Freeing of caps and vetos should have suceeded", freeVetos(blockedXo));
    vetos = vm.listVetos();
    assertEquals("Vetos should have been empty after setup", 0, vetos.size());
    
    schedule();
    checkVetoCacheFree();
  }

  


  private String sortStrings(String separator, String ... strings) {
    List<String> list = Arrays.asList(strings);
    Collections.sort(list);
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( String s : list ) {
      sb.append(sep).append(s);
      sep = separator;
    }
    return sb.toString(); 
  }

  
}
