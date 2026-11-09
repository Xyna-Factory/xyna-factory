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
package com.gip.xyna.xprc.xsched.capacities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gip.xyna.xnwh.exceptions.XNWH_SharedResourceInstanceDoesNotExist;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.capacities.CMSharedResource.CapacityStore;
import com.gip.xyna.xprc.xsched.capacities.CMSharedResource.SharedResourceCapacity;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;

import junit.framework.TestCase;

public class CMSharedResourceTest extends TestCase {

  private FakeCapacityStore store;
  private long now;
  private long maxAge;
  private List<CMSharedResource> instances;
  private CMSharedResource cm;


  @Override
  protected void setUp() {
    store = new FakeCapacityStore();
    now = 1000;
    maxAge = 1000;
    instances = new ArrayList<>();
    cm = newInstance();
  }


  @Override
  protected void tearDown() {
    for (CMSharedResource instance : instances) {
      instance.close();
    }
  }


  private CMSharedResource newInstance() {
    CMSharedResource instance = new CMSharedResource(store, () -> maxAge, () -> now);
    instances.add(instance);
    return instance;
  }


  private static OrderInformation order(long id) {
    return new OrderInformation(id, id, "Order" + id);
  }


  private static SchedulingData data(Capacity... capacities) {
    SchedulingData sd = new SchedulingData(0);
    try {
      Field field = SchedulingData.class.getDeclaredField("capacities");
      field.setAccessible(true);
      field.set(sd, capacities.length == 0 ? Collections.emptyList() : new ArrayList<>(List.of(capacities)));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return sd;
  }


  private static Capacity cap(String name, int cardinality) {
    return new Capacity(name, cardinality);
  }


  /** Like TryScheduleAbstract.CapacityAllocation: an unsuccessful result is always followed by undo. */
  private CapacityAllocationResult prepare(CMSharedResource instance, long orderId, SchedulingData sd) {
    CapacityAllocationResult result = instance.prepareCapacityAllocation(order(orderId), sd);
    if (!result.isAllocated()) {
      instance.undoAllocation(order(orderId), sd);
    }
    return result;
  }


  /** Like TryScheduleAbstract.Schedule: commit before the order is started, undo on failure. */
  private CapacityAllocationResult schedule(CMSharedResource instance, long orderId, SchedulingData sd) {
    CapacityAllocationResult result = prepare(instance, orderId, sd);
    if (!result.isAllocated()) {
      return result;
    }
    result = instance.commitCapacityAllocation(order(orderId), sd);
    if (!result.isAllocated()) {
      instance.undoAllocation(order(orderId), sd);
    }
    return result;
  }


  public void testVetoBlockedOrdersDoNotWrite() {
    store.add("C", 10, State.ACTIVE);
    store.resetCounters();
    for (long orderId = 1; orderId <= 300; orderId++) {
      SchedulingData sd = data(cap("C", 1));
      assertTrue(prepare(cm, orderId, sd).isAllocated());
      //veto not available -> undo
      cm.undoAllocation(order(orderId), sd);
      assertFalse(sd.isHasAcquiredCapacities());
    }
    assertEquals(1, store.reads);
    assertEquals(0, store.readAlls);
    assertEquals(0, store.updates);
    assertEquals(0, store.get("C").inuse);
  }


  public void testCommitAllocatesAndUndoReleases() {
    store.add("C", 2, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd).isAllocated());
    assertEquals(0, store.get("C").inuse);
    assertFalse(sd.isHasAcquiredCapacities());

    assertTrue(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    assertEquals(1, store.get("C").inuse);
    assertTrue(sd.isHasAcquiredCapacities());

    //e.g. no free execution thread
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.get("C").inuse);
    assertTrue(store.get("C").orders.isEmpty());
    assertFalse(sd.isHasAcquiredCapacities());
  }


  public void testTwoInstancesCannotExceedCardinality() {
    store.add("C", 1, State.ACTIVE);
    CMSharedResource other = newInstance();
    SchedulingData sd1 = data(cap("C", 1));
    SchedulingData sd2 = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd1).isAllocated());
    assertTrue(prepare(other, 2, sd2).isAllocated());

    assertTrue(cm.commitCapacityAllocation(order(1), sd1).isAllocated());
    CapacityAllocationResult result = other.commitCapacityAllocation(order(2), sd2);
    assertFalse(result.isAllocated());
    assertNull(result.getXynaException());
    other.undoAllocation(order(2), sd2);

    assertEquals(1, store.get("C").inuse);
    assertTrue(store.get("C").orders.containsKey(1L));
    assertFalse(store.get("C").orders.containsKey(2L));
  }


  public void testInsufficientCapacityIsDetectedWithoutStoreAccess() {
    store.add("C", 1, State.ACTIVE);
    assertTrue(schedule(cm, 1, data(cap("C", 1))).isAllocated());

    assertFalse(prepare(cm, 2, data(cap("C", 1))).isAllocated());
    store.resetCounters();
    for (int i = 0; i < 100; i++) {
      CapacityAllocationResult result = prepare(cm, 2, data(cap("C", 1)));
      assertFalse(result.isAllocated());
      assertEquals(OrderInstanceStatus.SCHEDULING_CAPACITY, result.getOrderInstanceStatus());
    }
    assertEquals(0, store.calls());
  }


  public void testLocalFreeIsNoticedImmediately() {
    store.add("C", 1, State.ACTIVE);
    assertTrue(schedule(cm, 1, data(cap("C", 1))).isAllocated());
    assertFalse(prepare(cm, 2, data(cap("C", 1))).isAllocated());

    assertTrue(cm.forceFreeCapacities(1L));
    assertTrue(schedule(cm, 2, data(cap("C", 1))).isAllocated());
    assertEquals(1, store.get("C").inuse);
  }


  public void testRemoteDisableIsHonoredAtCommit() {
    store.add("C", 1, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd).isAllocated());
    store.get("C").stateString = State.DISABLED.toString();

    CapacityAllocationResult result = cm.commitCapacityAllocation(order(1), sd);
    assertFalse(result.isAllocated());
    assertEquals(OrderInstanceStatus.WAITING_FOR_CAPACITY, result.getOrderInstanceStatus());
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.get("C").inuse);
  }


  public void testRemoteCardinalityReductionIsHonoredAtCommit() {
    store.add("C", 1, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd).isAllocated());
    store.get("C").cardinality = 0;

    assertFalse(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.get("C").inuse);
  }


  public void testRemoteDeletionIsHonoredAtCommit() {
    store.add("C", 1, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd).isAllocated());
    store.data.remove("C");

    assertFalse(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    cm.undoAllocation(order(1), sd);
    assertFalse(sd.isHasAcquiredCapacities());
  }


  public void testMissingCapacityWaitsWithoutWrite() {
    //default reaction on undefined capacities is Wait
    CapacityAllocationResult result = prepare(cm, 1, data(cap("C", 1)));
    assertFalse(result.isAllocated());
    assertEquals(OrderInstanceStatus.WAITING_FOR_CAPACITY, result.getOrderInstanceStatus());
    assertEquals(0, store.updates);

    store.resetCounters();
    assertFalse(prepare(cm, 1, data(cap("C", 1))).isAllocated());
    assertEquals(0, store.calls());
  }


  public void testRememberedStatesExpire() {
    store.add("C", 10, State.ACTIVE);
    store.resetCounters();
    SchedulingData sd = data(cap("C", 1));
    prepare(cm, 1, sd);
    cm.undoAllocation(order(1), sd);
    assertEquals(1, store.reads);

    now += maxAge - 1;
    prepare(cm, 1, sd);
    cm.undoAllocation(order(1), sd);
    assertEquals(1, store.reads);

    now += 1;
    prepare(cm, 1, sd);
    cm.undoAllocation(order(1), sd);
    assertEquals(2, store.reads);
    assertEquals(0, store.updates);
  }


  public void testRemoteFreeIsNoticedAfterExpiry() {
    store.add("C", 1, State.ACTIVE);
    assertTrue(schedule(newInstance(), 1, data(cap("C", 1))).isAllocated());
    assertFalse(prepare(cm, 2, data(cap("C", 1))).isAllocated());

    store.get("C").inuse = 0;
    store.get("C").orders.clear();
    assertFalse(prepare(cm, 2, data(cap("C", 1))).isAllocated());
    now += maxAge;
    assertTrue(schedule(cm, 2, data(cap("C", 1))).isAllocated());
  }


  public void testZeroMaxAgeAlwaysReads() {
    maxAge = 0;
    store.add("C", 10, State.ACTIVE);
    store.resetCounters();
    for (int i = 0; i < 3; i++) {
      SchedulingData sd = data(cap("C", 1));
      prepare(cm, 1, sd);
      cm.undoAllocation(order(1), sd);
    }
    assertEquals(3, store.reads);
  }


  public void testLocalChangeDuringReadIsNotRemembered() {
    store.add("C", 10, State.ACTIVE);
    store.afterRead = () -> {
      store.afterRead = null;
      cm.changeState("C", State.DISABLED);
    };
    //the read returns the old state, which must not be remembered
    assertTrue(prepare(cm, 1, data(cap("C", 1))).isAllocated());
    cm.undoAllocation(order(1), data(cap("C", 1)));

    store.resetCounters();
    CapacityAllocationResult result = prepare(cm, 1, data(cap("C", 1)));
    assertEquals(1, store.reads);
    assertFalse(result.isAllocated());
    assertEquals(OrderInstanceStatus.WAITING_FOR_CAPACITY, result.getOrderInstanceStatus());
  }


  public void testCommitFailureLeavesNoState() {
    store.add("C", 1, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(prepare(cm, 1, sd).isAllocated());
    store.get("C").inuse = 1;
    assertFalse(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    store.resetCounters();
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.calls());
    assertEquals(1, store.get("C").inuse);

    store.get("C").inuse = 0;
    assertTrue(schedule(cm, 1, sd).isAllocated());
    assertEquals(1, store.get("C").inuse);
    //after a successful commit, undo releases the capacities
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.get("C").inuse);
  }


  public void testDemandListsDoNotGrow() {
    store.add("C", 10, State.ACTIVE);
    store.add("M", 10, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    MultiAllocationCapacities multi = new MultiAllocationCapacities(cap("M", 1));
    multi.setMinAllocation(1);
    multi.setMaxAllocation(1);
    sd.setMultiAllocationCapacities(multi);
    for (int i = 0; i < 5; i++) {
      prepare(cm, 1, sd);
      cm.undoAllocation(order(1), sd);
    }
    assertEquals(1, sd.getCapacities().size());
    assertEquals(1, multi.getCapacities().size());
  }


  public void testMultiAllocationOnly() {
    store.add("M", 10, State.ACTIVE);
    SchedulingData sd = data();
    MultiAllocationCapacities multi = new MultiAllocationCapacities(cap("M", 1));
    multi.setMinAllocation(1);
    multi.setMaxAllocation(3);
    sd.setMultiAllocationCapacities(multi);
    assertTrue(schedule(cm, 1, sd).isAllocated());
    assertEquals(3, multi.getAllocations());
    assertEquals(3, store.get("M").inuse);
    assertTrue(sd.getCapacities().isEmpty());
  }


  public void testRepeatedCapacityNameIsSummed() {
    store.add("C", 2, State.ACTIVE);
    assertTrue(schedule(cm, 1, data(cap("C", 1), cap("C", 1))).isAllocated());
    assertEquals(2, store.get("C").inuse);
    assertFalse(prepare(cm, 2, data(cap("C", 1))).isAllocated());
  }


  public void testOrderWithoutCapacitiesNeedsNoStore() {
    SchedulingData sd = data();
    assertTrue(cm.prepareCapacityAllocation(order(1), sd).isAllocated());
    assertTrue(sd.isHasAcquiredCapacities());
    assertTrue(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    assertEquals(0, store.calls());
  }


  public void testEagerAllocationStillWorks() {
    store.add("C", 1, State.ACTIVE);
    SchedulingData sd = data(cap("C", 1));
    assertTrue(cm.allocateCapacities(order(1), sd).isAllocated());
    assertEquals(1, store.get("C").inuse);
    assertTrue(cm.commitCapacityAllocation(order(1), sd).isAllocated());
    assertEquals(1, store.get("C").inuse);
    cm.undoAllocation(order(1), sd);
    assertEquals(0, store.get("C").inuse);
  }


  /**
   * Transactional in-memory store which, like the SQL synchronizer, works on detached copies.
   */
  private static class FakeCapacityStore implements CapacityStore {

    private final Map<String, SharedResourceInstance<SharedResourceCapacity>> data = new LinkedHashMap<>();
    private int reads;
    private int readAlls;
    private int updates;
    private int creates;
    private int deletes;
    private Runnable afterRead;


    private void add(String name, int cardinality, State state) {
      data.put(name, new SharedResourceInstance<>(name, 0L, new SharedResourceCapacity(cardinality, state.toString())));
    }


    private SharedResourceCapacity get(String id) {
      SharedResourceInstance<SharedResourceCapacity> sri = data.get(id);
      return sri == null ? null : sri.getValue();
    }


    private void resetCounters() {
      reads = 0;
      readAlls = 0;
      updates = 0;
      creates = 0;
      deletes = 0;
    }


    private int calls() {
      return reads + readAlls + updates + creates + deletes;
    }


    private static SharedResourceInstance<SharedResourceCapacity> copy(SharedResourceInstance<SharedResourceCapacity> sri) {
      return new SharedResourceInstance<>(sri.getId(), sri.getCreated(), CMSharedResource.copy(sri.getValue()));
    }


    public SharedResourceRequestResult<SharedResourceCapacity> create(List<SharedResourceInstance<SharedResourceCapacity>> instances) {
      creates++;
      for (SharedResourceInstance<SharedResourceCapacity> sri : instances) {
        if (data.containsKey(sri.getId())) {
          return new SharedResourceRequestResult<>(false, new Exception("exists"), null);
        }
      }
      for (SharedResourceInstance<SharedResourceCapacity> sri : instances) {
        data.put(sri.getId(), copy(sri));
      }
      return new SharedResourceRequestResult<>(true, null, null);
    }


    public SharedResourceRequestResult<SharedResourceCapacity> read(List<String> ids) {
      reads++;
      List<SharedResourceInstance<SharedResourceCapacity>> result = new ArrayList<>();
      for (String id : ids) {
        if (data.containsKey(id)) {
          result.add(copy(data.get(id)));
        }
      }
      if (afterRead != null) {
        afterRead.run();
      }
      return new SharedResourceRequestResult<>(true, null, result);
    }


    public SharedResourceRequestResult<SharedResourceCapacity> readAll() {
      readAlls++;
      List<SharedResourceInstance<SharedResourceCapacity>> result = new ArrayList<>();
      for (SharedResourceInstance<SharedResourceCapacity> sri : data.values()) {
        result.add(copy(sri));
      }
      return new SharedResourceRequestResult<>(true, null, result);
    }


    public SharedResourceRequestResult<SharedResourceCapacity> update(List<String> ids,
                                                                      Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update) {
      updates++;
      if (ids.size() != ids.stream().distinct().count()) {
        return new SharedResourceRequestResult<>(false, new Exception("duplicate ids " + ids), null);
      }
      List<String> missing = new ArrayList<>();
      for (String id : ids) {
        if (!data.containsKey(id)) {
          missing.add(id);
        }
      }
      if (!missing.isEmpty()) {
        return new SharedResourceRequestResult<>(false, new XNWH_SharedResourceInstanceDoesNotExist("capacity", missing,
                                                                                                    missing.toString()), null);
      }
      List<SharedResourceInstance<SharedResourceCapacity>> updated = new ArrayList<>();
      for (String id : ids) {
        SharedResourceInstance<SharedResourceCapacity> result = update.apply(copy(data.get(id)));
        if (result == null) {
          return new SharedResourceRequestResult<>(false, null, null);
        }
        updated.add(result);
      }
      for (SharedResourceInstance<SharedResourceCapacity> sri : updated) {
        data.put(sri.getId(), copy(sri));
      }
      return new SharedResourceRequestResult<>(true, null, updated);
    }


    public SharedResourceRequestResult<SharedResourceCapacity> delete(List<String> ids) {
      deletes++;
      ids.forEach(data::remove);
      return new SharedResourceRequestResult<>(true, null, null);
    }
  }
}
