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
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xprc.xsched.vetos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.VM_SharedResource.SharedResourceVeto;
import com.gip.xyna.xprc.xsched.vetos.VM_SharedResource.VetoStore;

import junit.framework.TestCase;

public class VM_SharedResourceWorkerTest extends TestCase {

  private FakeVetoStore store;
  private int triggers;
  private int notifications;
  private VM_SharedResource vm;


  @Override
  protected void setUp() {
    store = new FakeVetoStore();
    triggers = 0;
    notifications = 0;
    vm = new VM_SharedResource(store, () -> triggers++, () -> notifications++);
  }


  private static OrderInformation order(long id) {
    return new OrderInformation(id, id, "Order" + id);
  }


  /** Like TryScheduleAbstract.VetoAllocation: a FAILED result is always followed by undo. */
  private VetoAllocationResult allocate(long orderId, List<String> exclusive, List<String> shared) {
    VetoAllocationResult result = vm.allocateVetos(order(orderId), exclusive, shared, 0);
    if (!result.isAllocated()) {
      vm.undoAllocation(order(orderId), exclusive, shared);
    }
    return result;
  }


  private VetoAllocationResult allocateExclusive(long orderId, String... vetos) {
    return allocate(orderId, List.of(vetos), Collections.emptyList());
  }


  private VetoAllocationResult allocateShared(long orderId, String... vetos) {
    return allocate(orderId, Collections.emptyList(), List.of(vetos));
  }


  /** Runs a complete scheduler cycle: allocate, worker, allocate again, finalize on success. */
  private VetoAllocationResult scheduleExclusive(long orderId, String... vetos) {
    allocateExclusive(orderId, vetos);
    vm.processWorkQueue();
    VetoAllocationResult result = allocateExclusive(orderId, vetos);
    if (result.isAllocated()) {
      vm.finalizeAllocation(order(orderId), List.of(vetos), Collections.emptyList());
    }
    return result;
  }


  public void testAllocationIsDoneByWorker() {
    assertFalse(allocateExclusive(1, "V").isAllocated());
    assertEquals(0, store.calls);
    assertEquals(1, triggers);

    vm.processWorkQueue();
    assertEquals(1, notifications);
    assertEquals(Long.valueOf(1), store.get("V").usingOrderId);

    int callsBefore = store.calls;
    assertTrue(allocateExclusive(1, "V").isAllocated());
    assertEquals(callsBefore, store.calls);
  }


  public void testNoDuplicateRequestsWhileInFlight() {
    allocateExclusive(1, "V");
    allocateExclusive(1, "V");
    allocateExclusive(1, "V");
    assertEquals(1, triggers);
    vm.processWorkQueue();
    assertEquals(1, store.updates);
  }


  public void testNormalizedVetoSetIsSameRequest() {
    allocateExclusive(1, "A", "B");
    vm.processWorkQueue();
    assertTrue(allocateExclusive(1, "B", "A", "B").isAllocated());
  }


  public void testBlockedAllocationIsRetriedOnNextSchedulerTry() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());

    assertFalse(allocateExclusive(2, "V").isAllocated());
    vm.processWorkQueue();
    assertEquals(1, notifications); // only from order 1
    assertFalse(allocateExclusive(2, "V").isAllocated()); // requeued
    assertEquals(3, triggers);

    assertTrue(vm.freeVetos(order(1)));
    vm.processWorkQueue();
    assertEquals(2, notifications);
    assertTrue(allocateExclusive(2, "V").isAllocated());
    assertEquals(Long.valueOf(2), store.get("V").usingOrderId);
  }


  public void testSharedAndPendingExclusive() {
    allocateShared(1, "S");
    allocateShared(2, "S");
    vm.processWorkQueue();
    assertTrue(allocateShared(1, "S").isAllocated());
    assertTrue(allocateShared(2, "S").isAllocated());

    allocateExclusive(3, "S");
    vm.processWorkQueue();
    assertFalse(allocateExclusive(3, "S").isAllocated());
    assertEquals(Long.valueOf(3), store.get("S").pendingExclusiveOrderId);

    allocateShared(4, "S");
    vm.processWorkQueue();
    assertFalse(allocateShared(4, "S").isAllocated());
  }


  public void testUndoAfterFailedAllocationKeepsRequest() {
    VetoAllocationResult result = vm.allocateVetos(order(1), List.of("V"), Collections.emptyList(), 0);
    assertFalse(result.isAllocated());
    vm.undoAllocation(order(1), List.of("V"), Collections.emptyList());

    vm.processWorkQueue();
    assertEquals(Long.valueOf(1), store.get("V").usingOrderId);
    assertEquals(1, notifications);

    // undo after a FAILED attempt while ACQUIRED keeps the allocation as well
    vm.undoAllocation(order(1), List.of("V"), Collections.emptyList());
    vm.processWorkQueue();
    assertEquals(Long.valueOf(1), store.get("V").usingOrderId);
    assertTrue(allocateExclusive(1, "V").isAllocated());
  }


  public void testUndoIsReleasedByWorkerWithoutNotification() {
    allocateExclusive(1, "V");
    vm.processWorkQueue();
    assertTrue(allocateExclusive(1, "V").isAllocated());
    int callsBefore = store.calls;

    vm.undoAllocation(order(1), List.of("V"), Collections.emptyList());
    assertEquals(callsBefore, store.calls);
    assertNotNull(store.get("V"));

    vm.processWorkQueue();
    assertNull(store.get("V"));
    assertEquals(1, notifications);

    // next scheduler try needs a new allocation
    assertFalse(allocateExclusive(1, "V").isAllocated());
  }


  public void testChangedVetoSetReleasesOldAllocation() {
    allocateExclusive(1, "A");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(1), store.get("A").usingOrderId);

    assertFalse(allocateExclusive(1, "B").isAllocated());
    vm.processWorkQueue();
    assertNull(store.get("A"));
    assertEquals(Long.valueOf(1), store.get("B").usingOrderId);
    assertTrue(allocateExclusive(1, "B").isAllocated());
  }


  public void testFreeOfQueuedRequestSkipsAllocation() {
    allocateExclusive(1, "V");
    vm.freeVetos(order(1));
    vm.processWorkQueue();
    assertNull(store.get("V"));
    assertEquals(0, store.updates);
    assertEquals(0, notifications);
  }


  public void testFreeDuringAcquireReleasesLateCommit() {
    store.beforeUpdate = () -> {
      store.beforeUpdate = null;
      vm.freeVetosForced(1);
    };
    allocateExclusive(1, "V");
    vm.processWorkQueue();
    assertNull(store.get("V"));
    assertEquals(0, notifications);
    assertFalse(allocateExclusive(1, "V").isAllocated());
  }


  public void testStoreErrorIsRetried() {
    store.failing = true;
    allocateExclusive(1, "V");
    vm.processWorkQueue();
    assertEquals(0, notifications);
    assertFalse(allocateExclusive(1, "V").isAllocated());

    store.failing = false;
    vm.processWorkQueue();
    assertEquals(1, notifications);
    assertTrue(allocateExclusive(1, "V").isAllocated());
  }


  public void testFinalizeRemovesLocalStateAndFreeClearsStore() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    assertEquals(Long.valueOf(1), store.get("V").usingOrderId);

    assertTrue(vm.freeVetos(order(1)));
    assertNull(store.get("V"));
    assertFalse(allocateExclusive(1, "V").isAllocated());
  }


  public void testKnownStatesAreKeptAcrossWorkerRuns() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    allocateExclusive(2, "V");
    vm.processWorkQueue();
    int callsBefore = store.calls;
    // like on a real factory: the worker processes each item as soon as the scheduler enqueues it
    for (int round = 0; round < 5; round++) {
      for (long id = 2; id <= 20; id++) {
        allocateExclusive(id, "V");
        vm.processWorkQueue();
      }
    }
    assertEquals(callsBefore, store.calls);
  }


  public void testKnownStatesExpire() {
    long[] now = {0};
    vm = new VM_SharedResource(store, () -> triggers++, () -> notifications++, () -> 100, () -> now[0]);
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    allocateExclusive(2, "V");
    vm.processWorkQueue();
    int callsBefore = store.calls;
    allocateExclusive(2, "V");
    vm.processWorkQueue();
    assertEquals(callsBefore, store.calls);

    // released on another node: not noticed before the known states expire
    store.data.remove("V");
    now[0] = 101;
    allocateExclusive(2, "V");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(2), store.get("V").usingOrderId);
    assertTrue(allocateExclusive(2, "V").isAllocated());
  }


  public void testShutdownProcessesRemainingWork() {
    allocateExclusive(1, "V");
    vm.shutdown();
    assertEquals(Long.valueOf(1), store.get("V").usingOrderId);
  }


  public void testWaitingOrdersAreAnsweredWithoutStoreAccess() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    for (long id = 2; id <= 100; id++) {
      allocateExclusive(id, "V");
    }
    int callsBefore = store.calls;
    vm.processWorkQueue();
    assertTrue("store calls: " + (store.calls - callsBefore), store.calls - callsBefore <= 3);

    assertTrue(vm.freeVetos(order(1)));
    for (long id = 2; id <= 100; id++) {
      assertFalse(allocateExclusive(id, "V").isAllocated());
    }
    callsBefore = store.calls;
    int notificationsBefore = notifications;
    vm.processWorkQueue();
    assertTrue("store calls: " + (store.calls - callsBefore), store.calls - callsBefore <= 4);
    assertEquals(notificationsBefore + 1, notifications);
    assertEquals(Long.valueOf(2), store.get("V").usingOrderId);
    assertTrue(allocateExclusive(2, "V").isAllocated());
    for (long id = 3; id <= 100; id++) {
      assertFalse(allocateExclusive(id, "V").isAllocated());
    }
  }


  public void testPendingExclusiveIsStillMarkedForSkippedRequests() {
    allocateShared(1, "S");
    allocateExclusive(2, "S");
    allocateExclusive(3, "S");
    vm.processWorkQueue();
    assertTrue(allocateShared(1, "S").isAllocated());
    assertEquals(Long.valueOf(2), store.get("S").pendingExclusiveOrderId);

    assertTrue(vm.freeVetos(order(1)));
    allocateExclusive(2, "S");
    allocateExclusive(3, "S");
    vm.processWorkQueue();
    assertTrue(allocateExclusive(2, "S").isAllocated());
    assertFalse(allocateExclusive(3, "S").isAllocated());
  }


  public void testEachSuccessIsNotifiedImmediately() {
    allocateExclusive(1, "A");
    allocateExclusive(2, "B");
    vm.processWorkQueue();
    assertEquals(2, notifications);
  }


  public void testReleaseDuringAcquireIsRetried() {
    // allocated by another node, so the worker has to access the store
    store.data.put("V", new SharedResourceInstance<>("V", 0L, new SharedResourceVeto(1L, 1L, "Order1", null, null, null)));
    allocateExclusive(2, "V");
    store.beforeUpdate = () -> {
      store.beforeUpdate = null;
      // order 1 finishes while the worker is processing order 2, the scheduler tries order 2 again
      vm.freeVetos(order(1));
      assertFalse(allocateExclusive(2, "V").isAllocated());
    };
    vm.processWorkQueue();
    assertEquals(Long.valueOf(2), store.get("V").usingOrderId);
    assertTrue(allocateExclusive(2, "V").isAllocated());
  }


  private VetoAllocationResult allocateUrgent(long orderId, long urgency, String... vetos) {
    List<String> list = List.of(vetos);
    VetoAllocationResult result = vm.allocateVetos(order(orderId), list, Collections.emptyList(), urgency);
    if (!result.isAllocated()) {
      vm.undoAllocation(order(orderId), list, Collections.emptyList());
    }
    return result;
  }


  private Long user(String veto) {
    SharedResourceVeto v = store.get(veto);
    return v == null ? null : v.usingOrderId;
  }


  public void testQueuedRequestsAreProcessedByUrgency() {
    allocateUrgent(3, 1, "V");
    allocateUrgent(4, 5, "V");
    allocateUrgent(2, 1, "V");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(4), user("V"));
  }


  public void testReleasedVetoIsHandedToMoreUrgentOrder() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    allocateUrgent(10, 100, "V");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();

    assertTrue(vm.freeVetos(order(1)));
    // the release happened during a scheduling run: the less urgent order is asked first
    allocateUrgent(20, 50, "V");
    int notificationsBefore = notifications;
    vm.processWorkQueue();
    // handed over directly, without another scheduling run
    assertEquals(Long.valueOf(10), user("V"));
    assertEquals(notificationsBefore + 1, notifications);

    // next scheduling run
    assertTrue(allocateUrgent(10, 100, "V").isAllocated());
    assertFalse(allocateUrgent(20, 50, "V").isAllocated());
  }


  public void testFailedHandOverLetsYieldingOrderRetry() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    allocateUrgent(10, 100, "V", "B");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();
    // B is allocated by another node, not known by the worker
    store.data.put("B", new SharedResourceInstance<>("B", 0L, new SharedResourceVeto(99L, 99L, "Order99", null, null, null)));

    assertTrue(vm.freeVetos(order(1)));
    allocateUrgent(20, 50, "V");
    int notificationsBefore = notifications;
    vm.processWorkQueue();
    assertNull(user("V"));
    assertEquals(notificationsBefore + 1, notifications);

    // next scheduling run: order 10 tried after the release
    allocateUrgent(10, 100, "V", "B");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(20), user("V"));
  }


  public void testNoYieldToMoreUrgentOrderWhichIsNotAskedAnymore() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    allocateUrgent(10, 100, "V");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();

    // order 10 is not asked in the next scheduling runs, e.g. because of missing capacities
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();

    assertTrue(vm.freeVetos(order(1)));
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(20), user("V"));
  }


  public void testNoYieldToMoreUrgentOrderWhichTriedAfterRelease() {
    assertTrue(scheduleExclusive(1, "V").isAllocated());
    assertTrue(scheduleExclusive(2, "B").isAllocated());
    allocateUrgent(10, 100, "V", "B");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();

    assertTrue(vm.freeVetos(order(1)));
    // order 10 is still blocked by B
    allocateUrgent(10, 100, "V", "B");
    allocateUrgent(20, 50, "V");
    vm.processWorkQueue();
    assertEquals(Long.valueOf(20), user("V"));
    assertEquals(Long.valueOf(2), user("B"));
  }


  /**
   * In-memory store with the same all-or-nothing update semantics as the SQL synchronizer.
   */
  private static class FakeVetoStore implements VetoStore {

    private final Map<String, SharedResourceInstance<SharedResourceVeto>> data = new LinkedHashMap<>();
    private int calls;
    private int updates;
    private boolean failing;
    private Runnable beforeUpdate;


    private SharedResourceVeto get(String id) {
      SharedResourceInstance<SharedResourceVeto> sri = data.get(id);
      return sri == null ? null : sri.getValue();
    }


    private SharedResourceRequestResult<SharedResourceVeto> failure() {
      return new SharedResourceRequestResult<>(false, new Exception("store failure"), null);
    }


    private static SharedResourceInstance<SharedResourceVeto> copy(SharedResourceInstance<SharedResourceVeto> sri) {
      return new SharedResourceInstance<>(sri.getId(), sri.getCreated(), sri.getValue().doClone());
    }


    public synchronized SharedResourceRequestResult<SharedResourceVeto> create(List<SharedResourceInstance<SharedResourceVeto>> instances) {
      calls++;
      if (failing) {
        return failure();
      }
      for (SharedResourceInstance<SharedResourceVeto> sri : instances) {
        if (data.containsKey(sri.getId())) {
          return failure();
        }
      }
      for (SharedResourceInstance<SharedResourceVeto> sri : instances) {
        data.put(sri.getId(), copy(sri));
      }
      return new SharedResourceRequestResult<>(true, null, Collections.emptyList());
    }


    public synchronized SharedResourceRequestResult<SharedResourceVeto> read(List<String> ids) {
      calls++;
      if (failing) {
        return failure();
      }
      List<SharedResourceInstance<SharedResourceVeto>> result = new ArrayList<>();
      for (String id : ids) {
        if (data.containsKey(id)) {
          result.add(copy(data.get(id)));
        }
      }
      return new SharedResourceRequestResult<>(true, null, result);
    }


    public synchronized SharedResourceRequestResult<SharedResourceVeto> readAll() {
      calls++;
      if (failing) {
        return failure();
      }
      List<SharedResourceInstance<SharedResourceVeto>> result = new ArrayList<>();
      for (SharedResourceInstance<SharedResourceVeto> sri : data.values()) {
        result.add(copy(sri));
      }
      return new SharedResourceRequestResult<>(true, null, result);
    }


    public synchronized SharedResourceRequestResult<SharedResourceVeto> update(List<String> ids,
                                                                               Function<SharedResourceInstance<SharedResourceVeto>, SharedResourceInstance<SharedResourceVeto>> update) {
      calls++;
      updates++;
      if (beforeUpdate != null) {
        beforeUpdate.run();
      }
      if (failing) {
        return failure();
      }
      Map<String, SharedResourceInstance<SharedResourceVeto>> changed = new LinkedHashMap<>();
      for (String id : ids) {
        SharedResourceInstance<SharedResourceVeto> current = data.get(id);
        if (current == null) {
          return failure();
        }
        SharedResourceInstance<SharedResourceVeto> updated = update.apply(copy(current));
        if (updated == null) {
          return failure();
        }
        changed.put(id, copy(updated));
      }
      data.putAll(changed);
      return new SharedResourceRequestResult<>(true, null, new ArrayList<>(changed.values()));
    }


    public synchronized SharedResourceRequestResult<SharedResourceVeto> delete(List<String> ids) {
      calls++;
      if (failing) {
        return failure();
      }
      for (String id : ids) {
        data.remove(id);
      }
      return new SharedResourceRequestResult<>(true, null, Collections.emptyList());
    }
  }

}
