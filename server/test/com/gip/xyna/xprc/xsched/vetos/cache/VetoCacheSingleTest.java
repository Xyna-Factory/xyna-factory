/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.AdministrativeVeto;
import com.gip.xyna.xprc.xsched.vetos.VM_SeparateThread;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequestList.ListAllocationMode;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheProcessor.SchedulerNotification;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.TestVetoCachePersistence;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.TestOrder;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.TestVM;

import junit.framework.TestCase;

public class VetoCacheSingleTest extends TestCase {
  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  private void waitFor(Supplier<String> description, BooleanSupplier condition) {
    long deadline = System.currentTimeMillis() + 10000;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > deadline) {
        fail("Timeout waiting for " + description.get());
      }
      sleep(2);
    }
  }

  private boolean isBlockedOn(Thread thread, Object monitor) {
    ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.getId());
    return info != null && info.getThreadState() == Thread.State.BLOCKED && info.getLockInfo() != null
        && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(monitor);
  }
  
  public void test_AllocationFree() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  
  public void test_AllocationFreeForced() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    assertTrue( vm.freeVetosForced(1) );
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }

  public void test_ReleaseAndAllocationAreSerialized() throws Exception {
    final TestVM vm = new TestVM(true);
    final TestOrder owner = new TestOrder(1, "eins").vetos("V").urgency(123);
    vm.schedule(12, owner);
    waitFor(() -> "V Used: " + vm.showVetoCache(), () -> vm.vc.get("V").getState() == State.Used);

    final VetoCacheEntry veto = vm.vc.get("V");

    final CountDownLatch releaseEntered = new CountDownLatch(1);
    final CountDownLatch releaseCanFinish = new CountDownLatch(1);
    final AtomicBoolean allocationFinished = new AtomicBoolean(false);

    Thread releaseThread = new Thread(new Runnable() {
      @Override
      public void run() {
        synchronized (veto) {
          releaseEntered.countDown();
          try {
            releaseCanFinish.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("release thread interrupted");
          }
          assertTrue(vm.freeVetos(owner));
        }
      }
    });
    releaseThread.start();
    assertTrue(releaseEntered.await(5, TimeUnit.SECONDS));

    final TestOrder next = new TestOrder(2, "zwei").vetos("V").urgency(12);
    final AtomicReference<VetoAllocationResult> allocationResult = new AtomicReference<>();
    final Thread allocationThread = new Thread(new Runnable() {
      @Override
      public void run() {
        vm.beginScheduling(13);
        VetoAllocationResult result = vm.allocateVetos(next);
        if (result.isAllocated()) {
          vm.finalizeAllocation(next);
        }
        vm.endScheduling();
        allocationResult.set(result);
        allocationFinished.set(true);
      }
    });
    allocationThread.start();
    waitFor(() -> "allocation thread blocked on veto, state " + allocationThread.getState(),
            () -> isBlockedOn(allocationThread, veto));
    assertFalse("allocation must wait until the release is complete", allocationFinished.get());

    releaseCanFinish.countDown();
    releaseThread.join();
    allocationThread.join();

    assertTrue("allocation must complete after the release", allocationFinished.get());
    // the released veto is Free until the VetoCacheProcessor has persisted the release
    assertFalse("veto must not be handed over while its release is in progress", allocationResult.get().isAllocated());
    assertFalse(vm.showVetoCache().contains("OrderInformation(2,zwei)"));

    // after persisting, the processor either removes V (no waiting order registered yet) or sets it Local
    waitFor(() -> "release of V processed: " + vm.showVetoCache(), () -> {
      VetoCacheEntry e = vm.vc.get("V");
      return e == null || e.getState() == State.Local;
    });
    vm.schedule(14, next);
    waitFor(() -> "V Used by order 2: " + vm.showVetoCache(), () -> {
      VetoCacheEntry e = vm.vc.get("V");
      return e != null && e.getState() == State.Used;
    });
    assertEquals(vm.showVetoCache(), Long.valueOf(2), vm.vc.get("V").getVetoInformation().getUsingOrderId());
  }

  public void test_PartialAllocationIsRolledBack() throws Exception {
    VetoCache vc = new VetoCache() {
      @Override
      public boolean allocate(AllocationRequest req, long urgency, ListAllocationMode allocMode) {
        if ("B".equals(req.getVetoName())) {
          return false; //B changed between check and allocation, e.g. concurrently freed
        }
        return super.allocate(req, urgency, allocMode);
      }
    };
    vc.init(0);
    VCP_Local vcp = new VCP_Local(vc, new TestVetoCachePersistence(0));
    vcp.setSchedulerNotification(new SchedulerNotification() { public void notifyScheduler() {} });
    vc.setVetoCacheProcessor(vcp, true);
    VM_SeparateThread vm = new VM_SeparateThread(vc);

    OrderInformation first = new OrderInformation(1L, 1L, "eins");
    vc.beginScheduling(12);
    VetoAllocationResult result = vm.allocateVetos(first, Arrays.asList("A", "B"), Collections.<String>emptyList(), 123);
    vc.endScheduling();

    assertFalse("order must not run with only a part of its vetos", result.isAllocated());
    VetoCacheEntry a = vc.get("A");
    assertTrue(vc.showVetoCache(), a == null || !a.isUsedBy(1L));
    assertFalse("rolled back vetos must not be remembered for the order", vm.freeVetos(first));

    // A stays Usable for the next run (keepUsable), so no processor action is needed in between
    OrderInformation second = new OrderInformation(2L, 2L, "zwei");
    vc.beginScheduling(13);
    assertTrue(vc.showVetoCache(), vm.allocateVetos(second, Arrays.asList("A"), Collections.<String>emptyList(), 123).isAllocated());
    vm.finalizeAllocation(second, Arrays.asList("A"), Collections.<String>emptyList());
    vc.endScheduling();
  }
  
  public void test_AllocationUndo() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    vm.schedule(12, to1.schedulingUndo() ); //endScheduling gibt Usable noch nicht frei
     
    assertEquals("{V=VetoCacheEntry(Usable: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    
    
    vm.schedule(14); //endScheduling gibt Usable wieder frei
    
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    
    vm.schedule(15, to1.schedulingUndo(false) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }

  public void test_Reservation() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(45);
    
    vm.schedule(12, to1.schedulingUndo(), to2 );
     
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(2,zwei)), 45, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(2,zwei))]", vm.listVetos().toString());
    
    //Derzeit also keine Reservierung!
    
    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }

 
  
  public void test_Waiting_Unused() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(45);
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    vm.schedule(17, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(45, count=1))}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 45, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    
    vm.schedule(19);
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  public void test_Waiting() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(45);
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    vm.schedule(17, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(45, count=1))}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 45, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    
    vm.schedule(19, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(2,zwei)), 45, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(2,zwei))]", vm.listVetos().toString());

    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  public void test_Blocking() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V", "A").urgency(111);
    TestOrder to3 = new TestOrder(3, "drei").vetos("A").urgency(23);
    
    vm.schedule(11, to1, to2, to3);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(111, count=1)), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins)), VetoInformation(A: OrderInformation(3,drei))]", vm.listVetos().toString());

    //Free
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(A: OrderInformation(3,drei))]", vm.listVetos().toString());

    vm.schedule(17, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, WaitingOrder(111, count=1))}", vm.showVetoCache() );
   // assertEquals("{A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), WaitingOrder(111, count=1))}", vm.showVetoCache() );
    assertEquals("[VetoInformation(A: OrderInformation(3,drei))]", vm.listVetos().toString());

    //Free
    assertTrue( vm.freeVetos(to3) );
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Local: A, 111, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());

    vm.schedule(19, to2);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(2,zwei)), 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(2,zwei)), 111, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(2,zwei)), VetoInformation(A: OrderInformation(2,zwei))]", vm.listVetos().toString());

    assertTrue( vm.freeVetos(to2) );
    sleep(50);
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  public void test_AdminVetoBlocking() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    AdministrativeVeto av = new AdministrativeVeto("V", "test", System.currentTimeMillis());
    
    vm.allocateAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: test)]", vm.listVetos().toString());
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: test), WaitingOrder(123, count=1))}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: test)]", vm.listVetos().toString());
    
    vm.freeAdministrativeVeto(av);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Local: V, 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder())}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  public void test_AdminVetoBlocked() throws XynaException {
    TestVM vm = new TestVM(true);
    assertEquals("[]", vm.listVetos().toString());
    
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    AdministrativeVeto av = new AdministrativeVeto("V", "test", System.currentTimeMillis());
    
    vm.schedule(12, to1);
    sleep(50);
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vm.listVetos().toString());
    
    try {
      vm.allocateAdministrativeVeto(av);
      fail("Expected exception");
    } catch ( XPRC_AdministrativeVetoAllocationDenied e ) {
      assertEquals("[V, 1]",Arrays.toString(e.getArgs() ) );
    }
    
    assertTrue( vm.freeVetos(to1) );
    sleep(50);
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  
  
  
  
  public void testStepped_AllocationFree() throws XynaException {
    TestVM vm = new TestVM();
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    //Allocation
    VetoAllocationResult result = vm.allocateVetos(to1);
    assertTrue(result.isAllocated());
    
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
   
    vm.finalizeAllocation(to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    //Free
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(1,eins)), null)}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
  }
  
  public void testStepped_Waiting_Unused() throws XynaException {
    TestVM vm = new TestVM();
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    //Allocation 1
    VetoAllocationResult result = vm.allocateVetos(to1);
    assertTrue(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    vm.finalizeAllocation(to1);
    
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    System.out.println("wd");
    //Allocation 2
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(45);
    result = vm.allocateVetos(to2);
    assertFalse(result.isAllocated());
    
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(45, count=1))}", vm.showVetoCache() );
   
    //Free 1
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(1,eins)), WaitingOrder(45, count=1))}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 45, WaitingOrder())}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    //Schedulerlauf verwendet Veto nicht
    vm.beginScheduling(17);
    assertEquals("{V=VetoCacheEntry(Usable: V, 45, WaitingOrder())}", vm.showVetoCache() );
    vm.endScheduling();
    assertEquals("{V=VetoCacheEntry(Compare: V, WaitingOrder())}", vm.showVetoCache() );
   
    //Freigabe 
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());

  }

  public void testStepped_Waiting() throws XynaException {
    TestVM vm = new TestVM();
    
    Collection<VetoInformation> vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
   
    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    
    //Allocation 1
    VetoAllocationResult result = vm.allocateVetos(to1);
    assertTrue(result.isAllocated());
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    vm.finalizeAllocation(to1);
    
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(V: OrderInformation(1,eins))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );
    
    //Allocation 2
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V").urgency(45);
    result = vm.allocateVetos(to2);
    assertFalse(result.isAllocated());
    
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(45, count=1))}", vm.showVetoCache() );
   
    //Free 1
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(1,eins)), WaitingOrder(45, count=1))}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 45, WaitingOrder())}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
        
    vm.beginScheduling(17);
    assertEquals("{V=VetoCacheEntry(Usable: V, 45, WaitingOrder())}", vm.showVetoCache() );
    
    //Allocation 2
    result = vm.allocateVetos(to2);
    assertTrue(result.isAllocated());
    
    assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(V: OrderInformation(2,zwei)), 45, WaitingOrder())}", vm.showVetoCache() );
    
    vm.finalizeAllocation(to1);
    assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(2,zwei)), 45, WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals("[VetoInformation(V: OrderInformation(2,zwei))]", vetos.toString());
    
    //Used
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(2,zwei)), 45, WaitingOrder())}", vm.showVetoCache() );
    
    //Free
    assertTrue( vm.freeVetos(to2) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());
    
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );
    vetos = vm.listVetos();
    assertEquals( 0, vetos.size());

  }
  
  public void testStepped_Blocking() throws XynaException {
    TestVM vm = new TestVM();

    TestOrder to1 = new TestOrder(1, "eins").vetos("V").urgency(123);
    TestOrder to2 = new TestOrder(2, "zwei").vetos("V", "A").urgency(111);
    TestOrder to3 = new TestOrder(3, "drei").vetos("A").urgency(23);
    
    
    { //Scheduling 1
      //Allocation 1
      VetoAllocationResult result = vm.allocateVetos(to1);
      assertTrue(result.isAllocated());
      vm.finalizeAllocation(to1);
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, null)}", vm.showVetoCache() );

      //Allocation 2
      result = vm.allocateVetos(to2);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(111, count=1))}", vm.showVetoCache() );

      //Allocation 3
      result = vm.allocateVetos(to3);
      assertTrue(result.isAllocated());
      vm.finalizeAllocation(to3);
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(111, count=1)), A=VetoCacheEntry(Scheduled: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );

      //Used
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(1,eins)), 123, WaitingOrder(111, count=1)), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );

      Collection<VetoInformation> vetos = vm.listVetos();
      assertEquals("[VetoInformation(V: OrderInformation(1,eins)), VetoInformation(A: OrderInformation(3,drei))]", vetos.toString());
    }
    
    //Free
    assertTrue( vm.freeVetos(to1) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(1,eins)), WaitingOrder(111, count=1)), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Local: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );
    
    { //Scheduling 2
      vm.beginScheduling(17);
      assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, null)}", vm.showVetoCache() );

      //Allocation 2
      VetoAllocationResult result = vm.allocateVetos(to2);
      assertFalse(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, WaitingOrder(111, count=1))}", vm.showVetoCache() );

      vm.endScheduling();
      assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, WaitingOrder(111, count=1))}", vm.showVetoCache() );
    
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(3,drei)), 23, WaitingOrder(111, count=1))}", vm.showVetoCache() );
    }
    
    //Free
    assertTrue( vm.freeVetos(to3) );
    assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Free: VetoInformation(A: OrderInformation(3,drei)), WaitingOrder(111, count=1))}", vm.showVetoCache() );
    vm.executeVetoCacheProcessor();
    assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Local: A, 111, WaitingOrder())}", vm.showVetoCache() );
    
    { //Scheduling 3
      vm.beginScheduling(37);
      assertEquals("{V=VetoCacheEntry(Usable: V, 111, WaitingOrder()), A=VetoCacheEntry(Usable: A, 111, WaitingOrder())}", vm.showVetoCache() );
      
      //Allocation 2
      VetoAllocationResult result = vm.allocateVetos(to2);
      assertTrue(result.isAllocated());
      assertEquals("{V=VetoCacheEntry(Scheduling: VetoInformation(V: OrderInformation(2,zwei)), 111, WaitingOrder()), A=VetoCacheEntry(Scheduling: VetoInformation(A: OrderInformation(2,zwei)), 111, WaitingOrder())}", vm.showVetoCache() );
      vm.finalizeAllocation(to2);
      assertEquals("{V=VetoCacheEntry(Scheduled: VetoInformation(V: OrderInformation(2,zwei)), 111, WaitingOrder()), A=VetoCacheEntry(Scheduled: VetoInformation(A: OrderInformation(2,zwei)), 111, WaitingOrder())}", vm.showVetoCache() );

      vm.endScheduling();
      vm.executeVetoCacheProcessor();
      assertEquals("{V=VetoCacheEntry(Used: VetoInformation(V: OrderInformation(2,zwei)), 111, WaitingOrder()), A=VetoCacheEntry(Used: VetoInformation(A: OrderInformation(2,zwei)), 111, WaitingOrder())}", vm.showVetoCache() );
    
      Collection<VetoInformation> vetos = vm.listVetos();
      assertEquals("[VetoInformation(V: OrderInformation(2,zwei)), VetoInformation(A: OrderInformation(2,zwei))]", vetos.toString());

    }
    
    //Free
    assertTrue( vm.freeVetos(to2) );
    assertEquals("{V=VetoCacheEntry(Free: VetoInformation(V: OrderInformation(2,zwei)), WaitingOrder()), A=VetoCacheEntry(Free: VetoInformation(A: OrderInformation(2,zwei)), WaitingOrder())}", vm.showVetoCache() );
    vm.executeVetoCacheProcessor();
    assertEquals("{}", vm.showVetoCache() );

    
    
  }
  
  public void test_AllocationFreeLoad() throws XynaException, InterruptedException {
    final TestVM vm = new TestVM(true);
    ExecutorService exec = new ThreadPoolExecutor(20, 30, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
        new RejectedExecutionHandler() {
          @Override
          public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //System.err.println("all threads used");
            r.run();
          }
        }
        );
    vm.setExecutor(exec);
    
    int testSize = 1000;
    final AtomicInteger countFree = new AtomicInteger(0);
    assertEquals("[]", vm.listVetos().toString());
    
    for( int i=0; i< testSize; ++i ) {
      final TestOrder to1 = new TestOrder(i, "eins").vetos("V"+i).urgency(123+i);
      to1.runnable( new Runnable(){
        @Override
        public void run() {
          if( vm.freeVetos(to1) ) {
            countFree.getAndIncrement();
          }
        }} );
      
      
      vm.schedule(12, to1);

    }
    
    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.SECONDS);
    
    sleep(50);
    
    assertEquals("{}", vm.showVetoCache() );
    assertEquals("[]", vm.listVetos().toString());
  }
  

}
