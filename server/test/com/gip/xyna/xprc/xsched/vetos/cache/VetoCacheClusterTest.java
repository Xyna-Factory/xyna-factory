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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.ClusteredVM;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.TestOrder;

import junit.framework.TestCase;

public class VetoCacheClusterTest extends TestCase {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VetoCacheClusterTest.class);

  @Override
  protected void setUp() throws Exception {
    //Logger.getLogger("com.gip.xyna.xprc.xsched.vetos.cache.VCP_Clustered").setLevel(Level.TRACE);
    //Logger.getLogger("com.gip.xyna.xprc.xsched.vetos.cache.cluster.VCP_Remote").setLevel(Level.TRACE);
    super.setUp();
  }
  
  

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }
  
  private void assertCleaned(ClusteredVM vm, ClusteredVM vm2) {
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals( "[]", vm.listVetos().toString() );
    assertEquals( "[]", vm2.listVetos().toString() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
  }
  
  public void test_Allocate_Free() throws XynaException {
    
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
    sleep(10);
    assertEquals( "started, clustered, persistent(0 entries)", vm.showInformation() );
    assertEquals( "started, clustered, persistent(0 entries)", vm2.showInformation() );
        
    assertEquals( "[]", vm.listVetos().toString());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
   
    vm.schedule(23, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void test_Allocate_Free_2orders_sameTime() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);

    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);
    
    vm.schedule(23, to1, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=2))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to1, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    vm.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder())}", vm.showVetoCache() );
    //assertEquals("{V=VetoCacheEntry(Local: V, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    
    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  
  
  
  public void test_Allocate_Free_2_orders_sequential() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);

    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);
    
    vm.schedule(23, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    vm.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
     
    vm.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    
    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void test_Allocate_Free_2orders_concurrent() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);

    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);
    
    vm.schedule(23, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    vm2.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder())}", vm2.showVetoCache() );
    
    vm2.schedule(55, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm2.showVetoCache() );
    
    assertTrue( vm2.freeVetos(to2) );
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_Allocate_Free_2_orders_sequential() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, false);
    ClusteredVM vm2 = new ClusteredVM(2, false);
    ClusteredVM.cluster(vm,vm2);

    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);
    
    logger.debug("Step 1");
    vm.schedule(23, to1);
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 2");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 3");
    vm.schedule(24, to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    /*
     //schnellere Ausführung auf 1
    logger.debug("Step 4");
    vm.schedule(37, to2);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 5");
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V, V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 6");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 7"); //ändert nichts
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 8"); //fast wie Step 9 unten
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   */
    

    logger.debug("Step 4");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 5");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 6");
    vm.schedule(37, to2);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 7");
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 8");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    /*
    logger.debug("Step 9a");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    */
    logger.debug("Step 9");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 10");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 11");
    vm.schedule(38, to2);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 12");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 13");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 14");
    assertTrue( vm.freeVetos(to2) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 15");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 16");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 17");
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    assertCleaned(vm, vm2);
  }

  
  
  public void test_AdminVeto() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
    
    AdministrativeVeto av = new AdministrativeVeto("AV", "test");
    vm.allocateAdministrativeVeto(av);
    sleep(50);
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(AV: test)]", vm.listVetos().toString());
    assertEquals("[VetoInformation(AV: test)]", vm2.listVetos().toString());
    
    AdministrativeVeto avc = new AdministrativeVeto("AV", "changed");
    vm.setDocumentationOfAdministrativeVeto(avc);
    sleep(50);
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(AV: changed)]", vm.listVetos().toString());
    assertEquals("[VetoInformation(AV: changed)]", vm2.listVetos().toString());
    
    
    vm.freeAdministrativeVeto(av);
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_AdminVeto() throws XynaException {
    final ClusteredVM vm = new ClusteredVM(1,false, true);
    final ClusteredVM vm2 = new ClusteredVM(2,false, true);
    ClusteredVM.cluster(vm,vm2);
    
    logger.debug("Step 1"); //Administratives Veto anlegen
    AdministrativeVeto av = new AdministrativeVeto("AV", "test");
    vm.allocateAdministrativeVetoInOwnThread(av);
    sleep(20);
    assertEquals("{AV=VetoCacheEntry(Compare: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[AV]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    vm.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Comparing: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
    
    assertEquals("VetoResponse(0,[COMPARE(AV,ADMIN)])", vm.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Remote: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
    
    assertEquals("VetoResponse(1,[SUCCESS(AV)])", vm.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[AV]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(AV: test), null)]]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
    
    assertEquals( "none", vm2.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoPersistence() );
    
    
    logger.debug("Step 2"); //Dokumentation ändern
    AdministrativeVeto avc = new AdministrativeVeto("AV", "changed");
    vm.setDocumentationOfAdministrativeVetoInOwnThread(avc);
    sleep(20);
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[AV]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoPersistence() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)]]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoPersistence() );
    
    assertEquals("VetoResponse(2,[SUCCESS(AV)])", vm.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[AV]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoPersistence() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(AV: changed), null)]]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: test), null)}", vm2.showVetoPersistence() );
    
    assertEquals( "none", vm2.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );
    
    
    
    logger.debug("Step 3"); //Administratives Veto löschen
    vm.freeAdministrativeVetoInOwnThread(av);
    sleep(20);
    assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[AV]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );

    vm.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(AV: changed), null)],s=[]]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );
   
    assertEquals("VetoResponse(3,[SUCCESS(AV)])", vm.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Comparing: AV, null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[AV]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );
    
    boolean testInUse = false;
    if( testInUse ) {
      assertEquals("VetoResponse(4,[IN_USE(AV,Free)])", vm.executeRemoteRequests(true) );
      assertEquals("{AV=VetoCacheEntry(Compare: AV, null)}", vm.showVetoCache() );
      assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
      assertEquals("[AV]-P[]-0", vm.showVetoQueue() );
      assertEquals("[AV]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{}", vm.showVetoPersistence() );
      assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );

      vm.executeVetoCacheProcessor();
      assertEquals("{AV=VetoCacheEntry(Comparing: AV, null)}", vm.showVetoCache() );
      assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[AV]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{}", vm.showVetoPersistence() );
      assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );
    }
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{AV=VetoCacheEntry(Comparing: AV, null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Free: VetoInformation(AV: changed), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(AV: changed), null)],s=[]]-1", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{AV=VetoCacheEntry(Used: VetoInformation(AV: changed), null)}", vm2.showVetoPersistence() );
    
    assertEquals("VetoResponse(0,[SUCCESS(AV)])", vm2.executeRemoteRequests(true) );
    assertEquals("{AV=VetoCacheEntry(Comparing: AV, null)}", vm.showVetoCache() );
    assertEquals("{AV=VetoCacheEntry(Remote: AV, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
    
    assertEquals("VetoResponse(4,[COMPARE(AV,UNUSED)])", vm.executeRemoteRequests(true) );
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );

    assertCleaned(vm, vm2);
  }

  public void test_AdminVeto_Blocking() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
    
    AdministrativeVeto av = new AdministrativeVeto("V", "test");
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.allocateAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(V: test)]", vm.listVetos().toString());
    assertEquals("[VetoInformation(V: test)]", vm2.listVetos().toString());
    
    
    vm.schedule(11, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    
    vm.freeAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    assertEquals("[]", vm2.listVetos().toString());
    
    vm.schedule(11, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertCleaned(vm, vm2);
  }

  public void test_AdminVeto_Blocking_FreeOnOther() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
     
    AdministrativeVeto av = new AdministrativeVeto("V", "test");
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.allocateAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(V: test)]", vm.listVetos().toString());
    assertEquals("[VetoInformation(V: test)]", vm2.listVetos().toString());
    
    
    vm.schedule(11, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    
    vm2.freeAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    assertEquals("[]", vm2.listVetos().toString());
    
    vm.schedule(11, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_AdminVeto_Blocking() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, false);
    ClusteredVM vm2 = new ClusteredVM(2, false);
    ClusteredVM.cluster(vm,vm2);
    
    AdministrativeVeto av = new AdministrativeVeto("V", "test");
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    logger.debug("Step 1");
    vm.allocateAdministrativeVetoInOwnThread(av);
    sleep(100);
    assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 2");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 3");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 4");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   
    assertEquals("[VetoInformation(V: test)]", vm.listVetos().toString());
    assertEquals("[VetoInformation(V: test)]", vm2.listVetos().toString());
    
    //blockierter Auftrag
    logger.debug("Step 5");
    vm.schedule(11, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    
    //free
    logger.debug("Step 6");
    vm.freeAdministrativeVetoInOwnThread(av);
    sleep(20);
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 7");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    /*
    logger.debug("Step 8a");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    */
    
    logger.debug("Step 8");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
   
    logger.debug("Step 9");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 10");
    vm.schedule(11, to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 11");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    
    logger.debug("Step 12");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    logger.debug("Step 13");
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 14");
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 15");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    logger.debug("Step 16");
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    assertCleaned(vm, vm2);
  }

  
  
  
 


  /*
   * FIXME Test kann so nicht stimmen: in allocate werden zuerst die Vetos überprüft. Dabei fällt bei Schedule to2 auf, 
   * dass Veto V bereits vergeben ist. Damit wird Z 
    */
  public void test_Allocate_Two() throws XynaException {
    
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
    
    assertEquals( "[]", vm.listVetos().toString());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V", "Z").urgency(111);
    
    vm.schedule(23, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to2);
    
    sleep(50);
    assertEquals("{Z=VetoCacheEntry(Local: Z, 111, WaitingOrder(111, count=1)), V=VetoCacheEntry(Usable: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{Z=VetoCacheEntry(Remote: Z, null), V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    assertEquals("[]", vm2.listVetos().toString());
    
    
    vm.schedule(38, to2);
    sleep(50);
    assertEquals("{Z=VetoCacheEntry(Used: VetoInformation(1-Z: OrderInformation(2,zwei)), 111, WaitingOrder()), V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 111, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{Z=VetoCacheEntry(Used: VetoInformation(1-Z: OrderInformation(2,zwei)), null), V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-Z: OrderInformation(2,zwei)), VetoInformation(1-V: OrderInformation(2,zwei))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-Z: OrderInformation(2,zwei)), VetoInformation(1-V: OrderInformation(2,zwei))]", vm2.listVetos().toString());
        
    assertTrue( vm.freeVetos(to2) );
     sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals( "[]", vm.listVetos().toString() );
    assertEquals( "[]", vm2.listVetos().toString() );
    
    vm.schedule(40);
    sleep(50);
    assertCleaned(vm, vm2);
  }

 
  
  public void test_Allocate_SameUrgency() throws XynaException {
    
    ClusteredVM vm = new ClusteredVM(1, true);
    ClusteredVM vm2 = new ClusteredVM(2, true);
    ClusteredVM.cluster(vm, vm2);
    assertEquals("started, clustered, persistent(0 entries)", vm.showInformation());
    assertEquals("started, clustered, persistent(0 entries)", vm2.showInformation());
    assertEquals( "[]", vm.listVetos().toString());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(123);
    
    vm.schedule(23, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(37, to1, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vm2.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    assertEquals("[]", vm2.listVetos().toString());
       
    
    vm.schedule(38, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(2,zwei)), null)}", vm2.showVetoCache() );
    assertEquals("[VetoInformation(1-V: OrderInformation(2,zwei))]", vm.listVetos().toString());
    assertEquals("[VetoInformation(1-V: OrderInformation(2,zwei))]", vm2.listVetos().toString());
    
    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    assertCleaned(vm, vm2);
  }

  
  
  
  
  
  
  
 
  
  
  
  
  
  
  
  
  
  
  
  
  
  public void testStepped_Conflict() throws XynaException {
    final ClusteredVM vm = new ClusteredVM(1,false, true);
    final ClusteredVM vm2 = new ClusteredVM(2,false, true);
    ClusteredVM.cluster(vm,vm2);
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);
    
    logger.debug("Step 1");
    vm.schedule(23, to1);
    vm2.schedule(17, to2);
    
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 2");
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    
    logger.debug("Step 3"); //Knoten 1 kann im Zustand Comparing/Comparing nichts ausrichten:
 
    assertEquals("VetoResponse(0,[IN_USE(V,Comparing)])", vm.executeRemoteRequests(true) );
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );

    logger.debug("Step 4"); //Im Zustand Compare/Comparing kann Knoten 2 weiterkommen
    assertEquals("VetoResponse(0,[COMPARE(V,LOCAL)])", vm2.executeRemoteRequests(true) );
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 5"); //aufräumen
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    
    logger.debug("Step 6"); //Knoten 1 belegt Veto nun
    vm.schedule(24, to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    boolean fastFree = false;
    if( ! fastFree ) {

      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())]]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(1,[SUCCESS(V)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))]]-0", vm2.showVetoQueue() );

      assertEquals( "none", vm2.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      logger.debug("Step 7");
      vm.freeVetos(to1);
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      logger.debug("Step 8");  //Free durchführen
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())],s=[]]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(2,[SUCCESS(V)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(3,[IN_USE(V,Free)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Compare: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      //bleibt bei VetoResponse(?,[IN_USE(V,Free)])

      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(37, count=1))],s=[]]-0", vm2.showVetoQueue() );

      assertEquals("none", vm2.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder())}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(4,[COMPARE(V,LOCAL)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder())}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );


    } else {

      logger.debug("Step 7");  //zu schnelles Free
      vm.freeVetos(to1);
      vm2.schedule(25);  //TODO kann leider WaitingOrder(...) nicht entfernen, da remote
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[V, V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      logger.debug("Step 8");  //Free durchführen
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder()), VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())],s=[]]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(1,[SUCCESS(V)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      assertEquals("VetoResponse(2,[COMPARE(V,LOCAL)])", vm.executeRemoteRequests(true) );
      assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    }

    logger.debug("Step 9");
    vm2.schedule(25); 
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Compare: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 10");
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );

    assertEquals("VetoResponse(1,[COMPARE(V,UNUSED)])", vm2.executeRemoteRequests(true) );
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );


    assertCleaned(vm, vm2);
  }


  public void testStepped_AdminVeto_Order_Concurrent() throws XynaException {
    final ClusteredVM vm = new ClusteredVM(1,true);
    final ClusteredVM vm2 = new ClusteredVM(2,true);
    ClusteredVM.cluster(vm,vm2);
    final StringBuffer sb = new StringBuffer();


    final AdministrativeVeto av = new AdministrativeVeto("V", "test");
    final TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    final CountDownLatch cdl = new CountDownLatch(1);
    Thread t2 = new Thread(new Runnable() {
      public void run() {
        try {
          cdl.await();
        } catch (InterruptedException e) {
        }
        sb.append("T2");
        //sleep(16);
        VetoAllocationResult result = vm2.allocateVetos(to1);
        sb.append( result.isAllocated() ? " Allocated " : " Not allocated ");
      }}, "T2");
    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          cdl.await();
        } catch (InterruptedException e) {
        }
        sb.append("T1");
        //sleep(16);
        try {
          vm.allocateAdministrativeVeto(av);
          sb.append(" admin veto allocated" );
        } catch (XPRC_AdministrativeVetoAllocationDenied e) {
          sb.append( Arrays.toString(e.getArgs()) );
        } catch (PersistenceLayerException e) {
          sb.append( e.getMessage() );
        }
        sb.append(" end" );
      }}, "T1");
    t1.start();
    t2.start();
    logger.debug("Step 1: Concurrent admin and normal veto");
    cdl.countDown();


    sleep(70);
    boolean admin = false;
    String tt = sb.toString().substring(0,5);
    if( tt.equals( "T1T2 ") || tt.equals( "T2T1 ") ) {
    } else {
      fail( tt );
    }
    String out = sb.toString().substring(5);
    
    if( out.equals("admin veto allocated end Not allocated " ) || out.equals("Not allocated  admin veto allocated end" ) ) {
      logger.debug("Step 2 a: admin veto");
      //AdminVeto hat gewonnen
      //assertEquals("T1T2 admin veto allocated end not allocated ", sb.toString() );
      
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      //}
      admin = true;
    } else if( out.equals("Not allocated [V, -2] end") || out.equals("Not allocated [V, null] end")) {
      //normales Veto hat gewonnen
      logger.debug("Step 2 b: normal veto");
      assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    } else {
      fail( sb.toString() +"\n" +  vm.showVetoCache()+"\n" +  vm2.showVetoCache() );
    }
    
    //System.err.println( "admin = "+admin);
    if( admin ) {
      logger.debug("Step 3 a: free admin veto");
      vm2.freeAdministrativeVeto(av);
      sleep(30);
      //assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
      //assertEquals("{}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm2.showVetoCache() );
      //assertEquals("{}", vm2.showVetoCache() );
    }
    
    logger.debug("Step 4: schedule order");
    vm2.schedule(12, to1);
    sleep(10);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    //assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );

    vm2.freeVetos(to1);
    
    sleep(50);
    assertCleaned(vm, vm2);
  }

  
  public void testStepped_AdminVeto_Order_Concurrent2() throws XynaException {
    final ClusteredVM vm = new ClusteredVM(1,false, true);
    final ClusteredVM vm2 = new ClusteredVM(2,false, true);
    ClusteredVM.cluster(vm,vm2);
    
    AdministrativeVeto av = new AdministrativeVeto("V", "test");
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    
    logger.debug("Step 1"); //AdministrativeVeto und normales Veto gleichzeitig
    vm.allocateAdministrativeVetoInOwnThread(av);
    vm2.schedule(23, to1);
    sleep(20);
    assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 2"); //Gleichzeitiges oder versetztes Laufen von executeVetoCacheProcessor
    int variant = (int)(Math.random()*4);
    boolean node1First = true;
    
    logger.info("variant = "+variant);
    switch( variant ) {
    case 0: //Knoten 1 zuerst
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Comparing: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      node1First = true;
      break;
    case 1: //Knoten 2 zuerst
      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-1", vm2.showVetoQueue() );
      node1First = false;
      break;
    case 2: //beide Knoten gleichzeitig, Knoten 1 muss warten
      vm.executeVetoCacheProcessor();
      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Comparing: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-1", vm2.showVetoQueue() );
      
      assertEquals("VetoResponse(?,[IN_USE(V,Comparing)])", vm.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-1", vm2.showVetoQueue() );
      node1First = false;
      break;
    case 3: //beide Knoten gleichzeitig, Knoten 2 muss warten
      vm.executeVetoCacheProcessor();
      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Comparing: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Comparing: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-1", vm2.showVetoQueue() );
      
      assertEquals("VetoResponse(?,[IN_USE(V,Comparing)])", vm2.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Comparing: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      node1First = true;
      break;
      default: 
      fail("Unexpected variant"+ variant);
    }
 
    
    if( node1First ) {
      
      assertEquals("VetoResponse(?,[COMPARE(V,ADMIN)])", vm.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-1", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      assertEquals("VetoResponse(?,[SUCCESS(V)])", vm.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[V, V]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1)), VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))]]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      assertEquals("none", vm2.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );
      
    } else {

      assertEquals("VetoResponse(?,[COMPARE(V,LOCAL)])", vm2.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[V]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, 123, WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(V: test), null)]]-1", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      assertEquals("VetoResponse(?,[SUCCESS(V)])", vm.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      vm2.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(V: test), WaitingOrder(123, count=1))]]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{}", vm2.showVetoPersistence() );

      assertEquals("none", vm2.executeRemoteRequests(false) );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      assertEquals("[]-P[]-0", vm2.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

    }
   
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

    
    logger.debug("Step 3"); //Administratives Veto wird entfernt
    vm.freeAdministrativeVetoInOwnThread(av);
    sleep(20);
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(V: test), null)],s=[]]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );
   
    assertEquals("VetoResponse(?,[SUCCESS(V)])", vm.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(V: test), WaitingOrder(123, count=1))],s=[]]-1", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm2.showVetoPersistence() );

    assertEquals("VetoResponse(?,[SUCCESS(V)])", vm2.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-1", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );
 
    assertEquals("VetoResponse(?,[COMPARE(V,LOCAL)])", vm.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );

    logger.debug("Step 4"); //Veto wird verwendet
    vm2.schedule(27, to1);
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())]]-1", vm2.showVetoQueue() );
   
    assertEquals("VetoResponse(?,[SUCCESS(V)])", vm2.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoPersistence() );

    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[f=[],s=[VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), null)]]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoPersistence() );

    assertEquals("none", vm.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoPersistence() );
    
    logger.debug("Step 4"); //Veto wird freigeben
    vm2.freeVetos(to1);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoPersistence() );
   
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), WaitingOrder())],s=[]]-1", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoPersistence() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoPersistence() );

    assertEquals("VetoResponse(?,[SUCCESS(V)])", vm2.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );

    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[f=[VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), null)],s=[]]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );

    assertEquals("none", vm.executeRemoteRequests(false) );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    assertEquals("{}", vm.showVetoPersistence() );
    assertEquals("{}", vm2.showVetoPersistence() );

    assertEquals("VetoResponse(?,[COMPARE(V,UNUSED)])", vm2.executeRemoteRequests(false) );
    
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_Allocate_Free() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, false);
    ClusteredVM vm2 = new ClusteredVM(2, false);
    ClusteredVM.cluster(vm, vm2);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V");
    
    //Allocation
    VetoAllocationResult result = vm.allocateVetos(to1);
    assertFalse(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Compare: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    
    //Local
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.beginScheduling(17);
    assertEquals("{V=VetoCacheEntry(Usable: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    
    
    //Allocation
    result = vm.allocateVetos(to1);
    assertTrue(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    
    vm.finalizeAllocation(to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    //Free
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
   
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_Allocate_FastExec_Free() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1, false);
    ClusteredVM vm2 = new ClusteredVM(2, false);
    ClusteredVM.cluster(vm, vm2);
    
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V");
    
    //Allocation
    VetoAllocationResult result = vm.allocateVetos(to1);
    assertFalse(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Compare: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    
    //Local
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.beginScheduling(17);
    assertEquals("{V=VetoCacheEntry(Usable: V, 0, WaitingOrder(0, count=1))}", vm.showVetoCache() );
    
    
    //Allocation
    result = vm.allocateVetos(to1);
    assertTrue(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    
    vm.finalizeAllocation(to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    //vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 0, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    //vm2.executeVetoCacheProcessor();
    //assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    //Free
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    assertCleaned(vm, vm2);
  }

  public void testStepped_Allocate_Free_2p() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    ClusteredVM.cluster(vm, vm2);
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(37);

    { //Scheduling 1
      //Allocation 1
      VetoAllocationResult result = vm.allocateVetos(to1);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    }
    
    { //Scheduling 2
      //Allocation 2
      VetoAllocationResult result = vm2.allocateVetos(to2);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Compare: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    }

  
    vm.executeVetoCacheProcessor();
    //vm2.executeVetoCacheProcessor();
    ClusteredVM.executeVetoCacheProcessorSimultaneous(vm, vm2); 
    
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    //FIXME ganz selten hier {V=VetoCacheEntry(Remote: V, null)}
    
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    
    vm.schedule(12);
    assertEquals("{V=VetoCacheEntry(Compare: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Local: V, 37, WaitingOrder(37, count=1))}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    
    
    vm2.schedule(12, to2);
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), 37, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    vm2.freeVetos(to2);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[V]-P[]-0", vm2.showVetoQueue() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[V]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-1", vm2.showVetoQueue() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    assertEquals("[]-P[]-0", vm.showVetoQueue() );
    assertEquals("[]-P[]-0", vm2.showVetoQueue() );
    
    assertCleaned(vm, vm2);
  }
  
  
  
  
  
  
  public void testStepped_Allocate_Free_2c() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    ClusteredVM.cluster(vm,vm2);
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(137);
    { //Scheduling 1
      //Allocation 1
      VetoAllocationResult result = vm.allocateVetos(to1);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Compare: V, 137, WaitingOrder(137, count=1))}", vm.showVetoCache() );
    }
    
    //Local
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 137, WaitingOrder(137, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    { //Scheduling 2
      vm.beginScheduling(17);
      assertEquals("{V=VetoCacheEntry(Usable: V, 137, WaitingOrder(137, count=1))}", vm.showVetoCache() );

      //Allocation
      VetoAllocationResult result = vm.allocateVetos(to1);
      assertTrue(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(1-V: OrderInformation(1,eins)), 137, WaitingOrder())}", vm.showVetoCache() );

      vm.finalizeAllocation(to1);
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 137, WaitingOrder())}", vm.showVetoCache() );
    }
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(1-V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 137, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    
    //Allocation 2
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(123);
    { //Scheduling 3
      VetoAllocationResult result = vm2.allocateVetos(to2);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    }
    
    //Free
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), WaitingOrder(123, count=1))}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, 123, WaitingOrder())}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm2.showVetoCache() );
    
    
    { //Scheduling 4
      vm2.beginScheduling(37);
      assertEquals("{V=VetoCacheEntry(Usable: V, 123, WaitingOrder())}", vm2.showVetoCache() );

      //Allocation
      VetoAllocationResult result = vm2.allocateVetos(to2);
      assertTrue(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(2-V: OrderInformation(2,zwei)), 123, WaitingOrder())}", vm2.showVetoCache() );

      vm2.finalizeAllocation(to2);
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(2,zwei)), 123, WaitingOrder())}", vm2.showVetoCache() );
    }
    
    vetos = vm.listVetos();
    assertEquals("[]", vetos.toString());
    vetos = vm2.listVetos();
    assertEquals("[VetoInformation(2-V: OrderInformation(2,zwei))]", vetos.toString());
    
    //Used
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), 123, WaitingOrder())}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(2-V: OrderInformation(2,zwei))]", vetos.toString());
    
    //Free
    assertTrue( vm2.freeVetos(to2) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm2.showVetoCache() );
    
    vetos = vm2.listVetos();
    assertEquals( 0, vetos.size());
    
    vm2.executeVetoCacheProcessor();
    
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, WaitingOrder())}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    assertCleaned(vm, vm2);
  }
  public void testStepped_switchToCluster_UsableUnused() throws XynaException {
    boolean testUnused = true;
    
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    vm.setUnclustered();
    vm2.setUnclustered();
    
    assertEquals( "[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123).schedulingUndo();
    
    vm.schedule(23, to1);
    
    assertEquals("{V=VetoCacheEntry(Usable: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    if( testUnused ) {
      vm.schedule(24 );
      assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
      assertEquals("{}", vm2.showVetoCache() );

      //nun Clustern
      vm.switchClustered(vm2);
      vm.executeVetoCacheProcessorInOwnThread(20);
      //assertEqualsChoice("[V]-P[]-Init", "[V]-P[]-Replicating", vm.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Compare: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
      
      vm2.executeVetoCacheProcessorInOwnThread(20); //muss Replikation anstossen
      
      assertEquals("[]-P[]-0", vm.showVetoQueue() );

    } else {

      //nun Clustern
      vm.switchClustered(vm2);
      vm.executeVetoCacheProcessorInOwnThread(20);
      assertEquals("[]-P[]-Replicating", vm.showVetoQueue() );
      assertEquals("{V=VetoCacheEntry(Usable: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );

      vm2.executeVetoCacheProcessorInOwnThread(20);
      assertEquals("[]-P[]-0", vm.showVetoQueue() );
      
      vm.schedule(24 );

      assertEquals("{V=VetoCacheEntry(Unused: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
      vm.executeVetoCacheProcessor();
    }
    
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    sleep(50);
    assertCleaned(vm, vm2);
  }

  public void testStepped_switchToCluster_ScheduledUsed() throws XynaException {
    boolean testUsed = true;
    
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    vm.setUnclustered();
    vm2.setUnclustered();
    
    assertEquals( "[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
   
    vm.schedule(23, to1);
    
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    if( testUsed ) {
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
      assertEquals("{}", vm2.showVetoCache() );
    }
   
    //nun Clustern
    vm.switchClustered(vm2);
    vm.executeVetoCacheProcessorInOwnThread(20);
    if( testUsed ) {
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    } else {
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    }
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessorInOwnThread(20);
    if( testUsed ) {
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    } else {
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    }
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    if( !testUsed ) {
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    }
    
    
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );

    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );

    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );

    sleep(50);
    assertCleaned(vm, vm2);
  }
  
  public void testStepped_switchToCluster_Free() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    vm.setUnclustered();
    vm2.setUnclustered();
    
   
    assertEquals( "[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.schedule(23, to1);
    
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
   
    assertTrue( vm.freeVetos(to1) );
    
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    //nun Clustern
    vm.switchClustered(vm2);
    vm.executeVetoCacheProcessorInOwnThread(20);
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessorInOwnThread(20);
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );

    sleep(50);
    assertCleaned(vm, vm2);
    
  }
  
  public void testStepped_Relocation() throws XynaException {
    ClusteredVM vm = new ClusteredVM(1,false);
    ClusteredVM vm2 = new ClusteredVM(2,false);
    ClusteredVM.cluster(vm, vm2);
    
    assertEquals( "[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
   
    vm.schedule(23, to1);
    assertEquals("{V=VetoCacheEntry(Compare: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );
   
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
    
    vm.schedule(27, to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Remote: V, null)}", vm2.showVetoCache() );
   
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    //doppeltes Schedulen erzeugt Warnung im Log
    vm.schedule(137, to1);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    
    //Schedulen auf Knoten 2 nach OrderMigration
    vm2.schedule(345, to1);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(1-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
   
    //Free
    assertTrue( vm2.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(2-V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(2-V: OrderInformation(1,eins)), WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm2.showVetoCache() );
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Remote: V, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("{V=VetoCacheEntry(Comparing: V, null)}", vm2.showVetoCache() );
    
    vm2.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("{}", vm2.showVetoCache() );

    sleep(50);
    assertCleaned(vm, vm2);
  }


}
