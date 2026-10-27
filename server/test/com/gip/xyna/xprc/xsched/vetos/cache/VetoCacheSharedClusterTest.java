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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VCP_Clustered;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.ClusteredVM;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheTestHelper.TestOrder;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.ReplicateVetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoResponse.VetoResponseEntry.Response;

import junit.framework.TestCase;

public class VetoCacheSharedClusterTest extends TestCase {

  private ClusteredVM first;
  private ClusteredVM second;
  private long schedulingRun;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    first = new ClusteredVM(1, false);
    second = new ClusteredVM(2, false);
    ClusteredVM.cluster(first, second);
  }

  public void testStepped_SimultaneousReadersOnBothNodes() {
    TestOrder firstReader = reader(1);
    TestOrder secondReader = reader(2);

    boolean firstGranted = tryAllocate(first, firstReader, true);
    boolean secondGranted = tryAllocate(second, secondReader, true);
    for (int attempt = 0; attempt < 12 && (!firstGranted || !secondGranted); attempt++) {
      processBoth();
      if (!firstGranted) {
        firstGranted = tryAllocate(first, firstReader, true);
      }
      if (!secondGranted) {
        secondGranted = tryAllocate(second, secondReader, true);
      }
    }
    processBoth();

    assertTrue("First node must grant its shared reader", firstGranted);
    assertTrue("Second node must grant its shared reader without releasing the first; first=" + first.showVetoCache() +
           ", second=" + second.showVetoCache() + ", queues=" + first.showVetoQueue() + "/" + second.showVetoQueue(),
           secondGranted);
    assertSharedHolders(first, 1L, 2L);
    assertSharedHolders(second, 1L, 2L);
  }

  public void testStepped_PartialReleaseUpdatesBothReplicasAndPersistence() {
    seedSharedHolders(1L, 2L);

    assertTrue("Releasing the first reader must succeed", first.freeVetosForced(1));
    processBoth();

    assertSharedHolders(first, 2L);
    assertSharedHolders(second, 2L);
    assertPersistenceMatchesCache(first);
    assertPersistenceMatchesCache(second);

    assertTrue("The remaining reader must still be releasable on its node", second.freeVetosForced(2));
    processUntilClean();
    assertEquals("{}", first.showVetoCache());
    assertEquals("{}", second.showVetoCache());
    assertEquals("{}", first.showVetoPersistence());
    assertEquals("{}", second.showVetoPersistence());
  }

  public void testStepped_PendingWriterBlocksNewReadersOnBothNodes() {
    seedSharedHolders(1L);
    TestOrder writer = new TestOrder(3, "writer").vetos("V").urgency(200);

    assertFalse("Writer must wait for the existing reader", tryAllocate(second, writer, true));
    second.undoAllocation(writer);
    processBoth();

    assertSharedHolders(first, 1L);
    assertSharedHolders(second, 1L);
    assertEquals("Writer reservation must reach the authority", Long.valueOf(3),
                 first.vc.get("V").getVetoInformation().getPendingExclusiveOrderId());
    assertEquals("Writer reservation must be retained by the requesting node", Long.valueOf(3),
                 second.vc.get("V").getVetoInformation().getPendingExclusiveOrderId());
    assertFalse("Authority must reject readers behind the writer", tryAllocate(first, reader(4), true));
    assertFalse("Partner must reject readers behind the writer", tryAllocate(second, reader(5), true));
    processBoth();
    assertSharedHolders(first, 1L);
    assertSharedHolders(second, 1L);

    assertTrue("Existing reader must be able to drain", first.freeVetosForced(1));
    processBoth();
    assertEventuallyGranted(second, writer, true);
    processBoth();
    for (ClusteredVM node : Arrays.asList(first, second)) {
      VetoInformation information = node.vc.get("V").getVetoInformation();
      assertEquals("Waiting writer must win after readers drain", Long.valueOf(3), information.getUsingOrderId());
      assertTrue("Writer must not overlap any shared holder", information.getSharedOrderIds().isEmpty());
      assertNull("Granted writer must no longer be pending", information.getPendingExclusiveOrderId());
      assertPersistenceMatchesCache(node);
    }
  }

  public void testStepped_CancelPendingWriterPreservesReadersAndClearsBarrier() {
    seedSharedState(3L, 1L);

    assertTrue("Forced termination must withdraw the pending writer", second.freeVetosForced(3));
    processBoth();

    assertSharedHolders(first, 1L);
    assertSharedHolders(second, 1L);
    assertNull("Authority must clear the cancelled writer", first.vc.get("V").getVetoInformation().getPendingExclusiveOrderId());
    assertNull("Partner must clear the cancelled writer", second.vc.get("V").getVetoInformation().getPendingExclusiveOrderId());
    assertPersistenceMatchesCache(first);
    assertPersistenceMatchesCache(second);

    assertEventuallyGranted(first, reader(4), true);
    processBoth();
    assertSharedHolders(first, 1L, 4L);
    assertSharedHolders(second, 1L, 4L);
  }

  public void testStepped_UnfinalizedReaderBlocksRemoteWriter() {
    TestOrder sharedReader = reader(1);
    TestOrder writer = new TestOrder(3, "writer").vetos("V").urgency(200);
    assertEventuallyGranted(first, sharedReader, false);

    assertFalse("Writer must wait for reader finalization before processor execution", tryAllocate(second, writer, true));
    second.undoAllocation(writer);

    second.executeVetoCacheProcessor();
    assertFalse("Processing writer requests must not bypass the reader", tryAllocate(second, writer, true));
    second.undoAllocation(writer);

    first.executeVetoCacheProcessor();
    assertFalse("Processing reader-node work must not bypass the reader", tryAllocate(second, writer, true));
    second.undoAllocation(writer);
    assertSharedHolders(first, 1L);

    first.finalizeAllocation(sharedReader);
    processBoth();
    assertSharedHolders(first, 1L);
    assertSharedHolders(second, 1L);
    assertFalse("Finalization must not make the reader available to a writer", tryAllocate(second, writer, true));
  }

  public void testStepped_RollbackReaderPreservesOtherHolder() {
    seedSharedHolders(1L);
    TestOrder rolledBackReader = reader(2);
    assertEventuallyGranted(second, rolledBackReader, false);

    second.undoAllocation(rolledBackReader);
    processBoth();

    assertSharedHolders(first, 1L);
    assertSharedHolders(second, 1L);
    assertPersistenceMatchesCache(first);
    assertPersistenceMatchesCache(second);

    assertEventuallyGranted(second, rolledBackReader, true);
    processBoth();
    assertSharedHolders(first, 1L, 2L);
    assertSharedHolders(second, 1L, 2L);
  }

  public void testStepped_FailedNodeHolderKeepsBlockingWriterUntilProvenGone() {
    seedSharedHolders(1L, 2L);
    TestOrder writer = new TestOrder(3, "writer").vetos("V").urgency(200);

    // First node is lost: it never runs again; the survivor continues without a partner.
    second.setUnclustered();
    VetoInformation fencedSnapshot = sharedInformation(Arrays.asList(1L, 2L), 1, 2, 5L);
    second.vc.get("V").setVetoInformation(fencedSnapshot);
    second.persistence.persist(Collections.singletonList(second.vc.get("V")), Collections.<VetoCacheEntry>emptyList());
    second.executeVetoCacheProcessor();

    assertFalse("Writer must wait while both readers are live", tryAllocate(second, writer, true));
    second.undoAllocation(writer);

    assertTrue("Local reader finishes", second.freeVetosForced(2));
    second.executeVetoCacheProcessor();
    assertSharedHolders(second, 1L);
    assertFalse("Unproven holder of the failed node must keep blocking the writer", tryAllocate(second, writer, true));
    second.undoAllocation(writer);
    second.executeVetoCacheProcessor();
    assertSharedHolders(second, 1L);

    // Stand-in for verified fencing and recovery proving order 1 is gone.
    assertTrue("Proven-dead holder is released", second.freeVetosForced(1));
    second.executeVetoCacheProcessor();
    assertEventuallyGrantedLocally(second, writer);
    VetoInformation granted = second.vc.get("V").getVetoInformation();
    assertEquals(Long.valueOf(3), granted.getUsingOrderId());
    assertTrue(granted.getSharedOrderIds().isEmpty());
  }

  public void testStaleAuthorityScheduledUpdateIsRejected() {
    VetoInformation current = sharedInformation(Arrays.asList(2L), 1, 2, 5L);
    VetoCacheEntry currentEntry = new VetoCacheEntry("V", VetoCache.State.Used);
    currentEntry.setVetoInformation(current);
    assertNull(second.vc.putIfAbsent(currentEntry));

    VetoInformation stale = sharedInformation(Arrays.asList(1L, 3L), 1, 1, 4L);
    VetoCacheEntry staleEntry = new VetoCacheEntry("V", VetoCache.State.Scheduled);
    staleEntry.setVetoInformation(stale);
    VetoRequest request = new VetoRequest(71, 1);
    request.scheduled(staleEntry);

    VetoResponse response = ((VCP_Clustered)second.vcp).getLocal().processRemoteVetoRequest(request);

    assertEquals(Response.STALE_AUTHORITY, response.getResponses().get(0).getResponse());
    assertEquals("V", response.getResponses().get(0).getName());
    assertEquals(2, current.getAuthorityBinding());
    assertEquals(5L, current.getAuthorityEpoch());
    assertEquals(Arrays.asList(2L), current.getSharedOrderIds());
  }

  public void testDelayedSameEpochSnapshotCannotRestoreReleasedHolder() {
    VetoInformation current = sharedInformation(Arrays.asList(2L), 1, 2, 5L, 9L);
    VetoCacheEntry currentEntry = new VetoCacheEntry("V", VetoCache.State.Used);
    currentEntry.setVetoInformation(current);
    assertNull(second.vc.putIfAbsent(currentEntry));

    VetoInformation delayed = sharedInformation(Arrays.asList(1L, 2L), 1, 2, 5L, 8L);
    VetoCacheEntry delayedEntry = new VetoCacheEntry("V", VetoCache.State.Scheduled);
    delayedEntry.setVetoInformation(delayed);
    VetoRequest request = new VetoRequest(72, 2);
    request.scheduled(delayedEntry);

    VetoResponse response = ((VCP_Clustered)second.vcp).getLocal().processRemoteVetoRequest(request);

    assertEquals(Response.STALE_AUTHORITY, response.getResponses().get(0).getResponse());
    assertEquals(9L, current.getAuthorityVersion());
    assertEquals(Arrays.asList(2L), current.getSharedOrderIds());
  }

  public void testRejoiningNodeAdoptsCurrentAuthorityAndCannotReclaimIt() {
    VetoInformation stale = sharedInformation(Arrays.asList(1L), 1, 1, 4L);
    VetoCacheEntry rejoiningEntry = new VetoCacheEntry("V", VetoCache.State.Used);
    rejoiningEntry.setVetoInformation(stale);
    assertNull(first.vc.putIfAbsent(rejoiningEntry));

    VetoInformation current = sharedInformation(Arrays.asList(2L), 1, 2, 5L);
    VetoCacheEntry authorityEntry = new VetoCacheEntry("V", VetoCache.State.Used);
    authorityEntry.setVetoInformation(current);
    assertNull(second.vc.putIfAbsent(authorityEntry));

    ReplicateVetoRequest currentSnapshot = new ReplicateVetoRequest(2);
    currentSnapshot.replicate(authorityEntry);
    ((VCP_Clustered)first.vcp).getLocal().replicate(currentSnapshot);

    VetoInformation adopted = first.vc.get("V").getVetoInformation();
    assertEquals(2, adopted.getAuthorityBinding());
    assertEquals(5L, adopted.getAuthorityEpoch());
    assertEquals("Execution binding is distinct from authority", 1, adopted.getBinding());
    assertEquals(Arrays.asList(2L), adopted.getSharedOrderIds());
    VetoInformation restored = VetoInformationStorable.toVetoInformation.transform(
      VetoInformationStorable.fromVetoInformation.transform(adopted));
    assertEquals(2, restored.getAuthorityBinding());
    assertEquals(5L, restored.getAuthorityEpoch());
    assertEquals(adopted.getAuthorityVersion(), restored.getAuthorityVersion());
    assertEquals(Arrays.asList(2L), restored.getSharedOrderIds());

    VetoCacheEntry staleRejoinSnapshot = new VetoCacheEntry("V", VetoCache.State.Used);
    staleRejoinSnapshot.setVetoInformation(stale);
    ReplicateVetoRequest staleSnapshot = new ReplicateVetoRequest(1);
    staleSnapshot.replicate(staleRejoinSnapshot);
    assertTrue(second.vc.get("V").compareAndSetState(VetoCache.State.Used, VetoCache.State.Compare));
    ((VCP_Clustered)second.vcp).getLocal().replicate(staleSnapshot);

    VetoInformation retained = second.vc.get("V").getVetoInformation();
    assertEquals(2, retained.getAuthorityBinding());
    assertEquals(5L, retained.getAuthorityEpoch());
    assertEquals(1, retained.getBinding());
    assertEquals(Arrays.asList(2L), retained.getSharedOrderIds());
  }

  private VetoInformation sharedInformation(java.util.List<Long> holders, int executionBinding,
                                           int authorityBinding, long authorityEpoch) {
    return new VetoInformation("V", null, holders, null, null, 0L, executionBinding,
                               authorityBinding, authorityEpoch);
  }

  private VetoInformation sharedInformation(java.util.List<Long> holders, int executionBinding,
                                           int authorityBinding, long authorityEpoch, long authorityVersion) {
    return new VetoInformation("V", null, holders, null, null, 0L, executionBinding,
                               authorityBinding, authorityEpoch, authorityVersion);
  }

  private void assertEventuallyGrantedLocally(ClusteredVM node, TestOrder order) {
    for (int attempt = 0; attempt < 12; attempt++) {
      if (tryAllocate(node, order, true)) {
        return;
      }
      node.executeVetoCacheProcessor();
    }
    fail("Node " + node.vc.getOwnBinding() + " did not grant order " + order.getOrderInformation().getOrderId());
  }

  private void seedSharedHolders(Long... orderIds) {
    seedSharedState(null, orderIds);
  }

  private void seedSharedState(Long pendingWriterId, Long... orderIds) {
    for (ClusteredVM node : Arrays.asList(first, second)) {
      VetoCacheEntry entry = new VetoCacheEntry("V", VetoCache.State.Used);
      entry.setVetoInformation(new VetoInformation("V", null, new ArrayList<Long>(Arrays.asList(orderIds)),
                                                  pendingWriterId, null, 0L, first.vc.getOwnBinding()));
      assertNull(node.vc.putIfAbsent(entry));
      node.persistence.persist(Collections.singletonList(entry), Collections.<VetoCacheEntry>emptyList());
    }
  }

  private void assertPersistenceMatchesCache(ClusteredVM node) {
    VetoCacheEntry persisted = new VetoCacheEntry("V", VetoCache.State.Used);
    persisted.setVetoInformation(node.vc.get("V").getVetoInformation());
    String expected = VetoCacheTestHelper.normalizeVetoInformationTimestamps("{V=" + persisted + "}");
    assertEquals("Persisted holders must match the cache on node " + node.vc.getOwnBinding(),
                 expected, node.showVetoPersistence());
  }

  private TestOrder reader(long orderId) {
    return new TestOrder(orderId, "reader").sharedVetos("V").urgency(100);
  }

  private void assertEventuallyGranted(ClusteredVM node, TestOrder order, boolean finalize) {
    for (int attempt = 0; attempt < 12; attempt++) {
      if (tryAllocate(node, order, finalize)) {
        return;
      }
      processBoth();
    }
    fail("Node " + node.vc.getOwnBinding() + " did not grant order " + order.getOrderInformation().getOrderId());
  }

  private boolean tryAllocate(ClusteredVM node, TestOrder order, boolean finalize) {
    node.beginScheduling(++schedulingRun);
    try {
      VetoAllocationResult result = node.allocateVetos(order);
      if (result.isAllocated() && finalize) {
        node.finalizeAllocation(order);
      }
      return result.isAllocated();
    } finally {
      node.endScheduling();
    }
  }

  private void processBoth() {
    first.executeVetoCacheProcessor();
    second.executeVetoCacheProcessor();
    first.executeVetoCacheProcessor();
    second.executeVetoCacheProcessor();
  }

  private void processUntilClean() {
    for (int attempt = 0; attempt < 12 &&
        (!"{}".equals(first.showVetoCache()) || !"{}".equals(second.showVetoCache()) ||
         !"{}".equals(first.showVetoPersistence()) || !"{}".equals(second.showVetoPersistence())); attempt++) {
      processBoth();
    }
  }

  private void assertSharedHolders(ClusteredVM node, Long... expectedIds) {
    VetoCacheEntry entry = node.vc.get("V");
    assertNotNull("Shared veto must exist on node " + node.vc.getOwnBinding(), entry);
    VetoInformation information = entry.getVetoInformation();
    assertNotNull("Shared veto must carry holder information", information);
    assertNull("Shared grants must not coexist with an exclusive holder", information.getUsingOrderId());
    assertFalse("Shared grants must not coexist with an administrative grant", information.isAdministrative());
    assertNotNull("Shared holder list must be present", information.getSharedOrderIds());
    assertEquals(new HashSet<Long>(Arrays.asList(expectedIds)), new HashSet<Long>(information.getSharedOrderIds()));
    assertEquals("Shared holders must not be duplicated", expectedIds.length, information.getSharedOrderIds().size());
  }
}