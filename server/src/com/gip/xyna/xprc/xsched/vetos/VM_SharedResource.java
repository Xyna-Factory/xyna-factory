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
package com.gip.xyna.xprc.xsched.vetos;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SharedResourceInstanceDoesNotExist;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.VetoManagement;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.VM_Cache.VetoFilter;


/**
 * Veto management based on SharedResourceManagement.
 * <p>
 * The SharedResource store is authoritative: a committed allocation in the store is the reservation.
 * To keep costly store access away from the scheduler thread, allocation and undo of allocations are
 * executed by a separate worker thread. The scheduler only enqueues allocation requests and consumes
 * their results on a later scheduling attempt; it gets SUCCESS only after the worker has committed the
 * allocation for exactly the requested vetos.
 */
public class VM_SharedResource implements VetoManagementInterface {

  public static final SharedResourceDefinition<SharedResourceVeto> XYNA_VETO_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(VetoManagement.XYNA_VETO_SR, SharedResourceVeto.class, ArrayList.class);
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_SharedResource.class);
  static final XynaPropertyDuration KNOWN_STATES_MAX_AGE =
      new XynaPropertyDuration("xprc.xsched.vetos.sharedresource.knownStatesMaxAge", "1 s", TimeUnit.MILLISECONDS)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Maximum age of veto states remembered by the SharedResource veto worker. "
                                       + "Releases on this node invalidate them immediately, releases on other nodes "
                                       + "are noticed after this time at the latest.")
          .setDefaultDocumentation(DocumentationLanguage.DE,
                                   "Maximales Alter von gemerkten Veto Zuständen. "
                                       + "Veto Freigaben von diesem Knoten werden sofort erkannt, Freigaben von anderen Knoten "
                                       + "werden spätestens nach dieser Zeitspanne erkannt.");
  private final VetoStore store;
  private final Runnable workerTrigger;
  private final Runnable schedulerNotifier;
  private final LazyAlgorithmExecutor<Algorithm> workerExecutor;

  private final Object stateLock = new Object();
  private final Object workerLock = new Object();
  private final Map<Long, AllocationRequest> requests = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<WorkItem> workQueue = new ConcurrentLinkedQueue<>();
  /** incremented after each release in the store; invalidates the veto states known by the worker */
  private final AtomicLong releaseCounter = new AtomicLong();
  private final LongSupplier knownStatesMaxAge;
  private final LongSupplier clock;

  // veto states known by the worker, guarded by workerLock
  private final Map<String, SharedResourceVeto> knownVetos = new HashMap<>();
  private long knownVetosReleaseCounter;
  private long knownVetosSince;

  /** orders scheduler requests, worker attempts and releases of this node */
  private final AtomicLong sequence = new AtomicLong();
  /** veto name -> sequence of its latest release by this node */
  private final Map<String, Long> releaseSequences = new ConcurrentHashMap<>();
  private static final int MAX_RELEASE_SEQUENCES = 10000;
  private AllocationRequest handOverTo; // guarded by workerLock

  // statistics, shown in showInformation (CLI listextendedschedulerinfo)
  private final AtomicLong statWorkerRuns = new AtomicLong();
  private final AtomicLong statAcquireItems = new AtomicLong();
  private final AtomicLong statSkippedKnownBlocked = new AtomicLong();
  private final AtomicLong statYielded = new AtomicLong();
  private final AtomicLong statStoreAllocations = new AtomicLong();
  private final AtomicLong statAcquired = new AtomicLong();
  private final AtomicLong statReleaseItems = new AtomicLong();
  private final AtomicLong statStoreAllocationMillis = new AtomicLong();
  private final AtomicLong statFreeCalls = new AtomicLong();
  private final AtomicLong statFreeMillis = new AtomicLong();


  public VM_SharedResource() {
    SharedResourceManagement srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    this.store = new SrmVetoStore(srm);
    this.schedulerNotifier = () -> XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
    this.workerExecutor = new LazyAlgorithmExecutor<>("SharedResourceVetoWorker");
    this.workerTrigger = workerExecutor::requestExecution;
    this.knownStatesMaxAge = KNOWN_STATES_MAX_AGE::getMillis;
    this.clock = System::currentTimeMillis;
    workerExecutor.startNewThread(this::processWorkQueue);
    logger.info("Started SharedResourceVetoWorker: vetos are allocated asynchronously, known veto states are cached for at most "
        + knownStatesMaxAge.getAsLong() + " ms");
  }


  /**
   * For tests: no worker thread is started, the worker has to be run via {@link #processWorkQueue()}.
   */
  VM_SharedResource(VetoStore store, Runnable workerTrigger, Runnable schedulerNotifier) {
    this(store, workerTrigger, schedulerNotifier, () -> Long.MAX_VALUE, System::currentTimeMillis);
  }


  VM_SharedResource(VetoStore store, Runnable workerTrigger, Runnable schedulerNotifier, LongSupplier knownStatesMaxAge,
                    LongSupplier clock) {
    this.store = store;
    this.workerTrigger = workerTrigger;
    this.schedulerNotifier = schedulerNotifier;
    this.workerExecutor = null;
    this.knownStatesMaxAge = knownStatesMaxAge;
    this.clock = clock;
  }


  /**
   * Stops the worker thread. Remaining queued work is processed on the calling thread.
   */
  public void shutdown() {
    if (workerExecutor != null) {
      workerExecutor.stopThread();
    }
    processWorkQueue();
  }


  @Override
  @Deprecated
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    return allocateVetos(orderInformation, vetos, Collections.emptyList(), urgency);
  }

  /**
   * Called by the scheduler thread; never accesses the SharedResource store.
   */
  @Override
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos, long urgency) {
    if (orderInformation == null || orderInformation.getOrderId() == null) {
      logger.error("Error allocating shared resource vetos: received empty order information.");
      return VetoAllocationResult.FAILED;
    }
    if (isNullOrEmpty(exclusiveVetos) && isNullOrEmpty(sharedVetos)) {
      return VetoAllocationResult.SUCCESS;
    }
    Long orderId = orderInformation.getOrderId();
    boolean enqueued = false;
    VetoAllocationResult result;
    synchronized (stateLock) {
      AllocationRequest request = requests.get(orderId);
      if (request != null && !request.matches(exclusiveVetos, sharedVetos)) {
        requests.remove(orderId);
        cancel(request, RemovalVariant.IGNORE_PENDING);
        request = null;
      }
      if (request == null) {
        request = new AllocationRequest(orderInformation, exclusiveVetos, sharedVetos);
        requests.put(orderId, request);
        workQueue.add(WorkItem.acquire(request));
        enqueued = true;
        result = VetoAllocationResult.FAILED;
      } else {
        switch (request.state) {
          case ACQUIRED :
            request.state = RequestState.CONSUMED;
            result = VetoAllocationResult.SUCCESS;
            break;
          case CONSUMED :
            result = VetoAllocationResult.SUCCESS;
            break;
          case NOT_ACQUIRED :
            request.state = RequestState.QUEUED;
            workQueue.add(WorkItem.acquire(request));
            enqueued = true;
            result = VetoAllocationResult.FAILED;
            break;
          default : // QUEUED, ACQUIRING
            // the scheduler was notified meanwhile, e.g. because of a release: the worker must not rely on older data
            request.retry = true;
            result = VetoAllocationResult.FAILED;
            break;
        }
      }
      request.asked(urgency, sequence.incrementAndGet());
    }
    if (enqueued) {
      workerTrigger.run();
    }
    return result;
  }


  private static boolean isNullOrEmpty(List<String> list) {
    return list == null || list.isEmpty();
  }


  /**
   * Must be called while holding stateLock; request must already be removed from requests.
   * Requests being processed by the worker are released by the worker after processing.
   */
  private void cancel(AllocationRequest request, RemovalVariant variant) {
    switch (request.state) {
      case QUEUED :
      case ACQUIRING :
        request.cancelled = true;
        request.cancelVariant = variant;
        break;
      case ACQUIRED :
      case CONSUMED :
        workQueue.add(WorkItem.release(request.orderInformation.getOrderId(), variant));
        break;
      case NOT_ACQUIRED :
        if (variant == RemovalVariant.REMOVE_PENDING) {
          workQueue.add(WorkItem.release(request.orderInformation.getOrderId(), variant));
        }
        break;
    }
  }


  /**
   * Worker: processes all queued work items. Each batch of queued items is processed in this order: releases first,
   * then acquires by descending urgency, so that a free veto is offered to the most urgent order first.
   * <p>
   * Veto states read or written by the worker are remembered, so that requests which are known to be blocked
   * (typically many orders waiting for the same veto) are answered without store access. The remembered states
   * are discarded after every release of this node and after {@link #KNOWN_STATES_MAX_AGE}, so that
   * releases on other nodes are noticed as well.
   */
  void processWorkQueue() {
    synchronized (workerLock) {
      long start = System.currentTimeMillis();
      long storeAllocationsBefore = statStoreAllocations.get();
      long skippedBefore = statSkippedKnownBlocked.get();
      long yieldedBefore = statYielded.get();
      int acquireItems = 0;
      int releaseItems = 0;
      int acquired = 0;
      boolean yieldedInRun = false;
      List<WorkItem> batch;
      while (!(batch = nextBatch()).isEmpty()) {
        for (WorkItem item : batch) {
          invalidateOutdatedKnownVetos();
          try {
            if (item.request == null) {
              releaseItems++;
              // no scheduler notification: an undo followed by a notification could cause an acquire/undo busy loop
              freeVetosOfOrder(item.orderId, item.variant);
            } else {
              acquireItems++;
              if (processAcquire(item.request)) {
                acquired++;
                if (logger.isDebugEnabled()) {
                  logger.debug("Acquired vetos for order " + item.orderId + " after "
                      + (System.currentTimeMillis() - item.request.created) + " ms, notifying scheduler");
                }
                // notify immediately, the order should not wait for the remaining work items
                schedulerNotifier.run();
              }
              AllocationRequest target;
              while ((target = handOverTo) != null) {
                handOverTo = null;
                if (handOver(target)) {
                  acquired++;
                  if (logger.isDebugEnabled()) {
                    logger.debug("Acquired vetos for more urgent order " + target.orderInformation.getOrderId() + " after "
                        + (System.currentTimeMillis() - target.created) + " ms, notifying scheduler");
                  }
                  schedulerNotifier.run();
                } else {
                  // the yielding order has to be asked again
                  yieldedInRun = true;
                }
              }
            }
          } catch (RuntimeException e) {
            handOverTo = null;
            knownVetos.clear();
            logger.error("Error processing shared resource veto work item for order " + item.orderId, e);
          }
        }
      }
      if (yieldedInRun) {
        // the more urgent orders have to be asked again by the scheduler
        schedulerNotifier.run();
      }
      if (requests.isEmpty() || releaseSequences.size() > MAX_RELEASE_SEQUENCES) {
        releaseSequences.clear();
      }
      if (acquireItems + releaseItems > 0) {
        statWorkerRuns.incrementAndGet();
        statAcquireItems.addAndGet(acquireItems);
        statReleaseItems.addAndGet(releaseItems);
        statAcquired.addAndGet(acquired);
        if (logger.isDebugEnabled()) {
          logger.debug("Worker run: " + acquireItems + " acquire items (" + acquired + " acquired, "
              + (statStoreAllocations.get() - storeAllocationsBefore) + " store allocations, "
              + (statSkippedKnownBlocked.get() - skippedBefore) + " skipped as known blocked, "
              + (statYielded.get() - yieldedBefore) + " yielded to more urgent orders), " + releaseItems
              + " release items, took " + (System.currentTimeMillis() - start) + " ms");
        }
      }
    }
  }


  /**
   * @return all currently queued work items: releases first, then acquires by descending urgency
   */
  private List<WorkItem> nextBatch() {
    List<WorkItem> batch = new ArrayList<>();
    WorkItem item;
    while ((item = workQueue.poll()) != null) {
      batch.add(item);
    }
    if (batch.size() > 1) {
      synchronized (stateLock) { // urgency is changed by the scheduler while holding stateLock
        batch.sort(WorkItem::compareProcessingOrder);
      }
    }
    return batch;
  }


  /**
   * Must be called while holding workerLock.
   */
  private void invalidateOutdatedKnownVetos() {
    if (knownVetos.isEmpty()) {
      return;
    }
    long releases = releaseCounter.get();
    if (releases != knownVetosReleaseCounter || clock.getAsLong() - knownVetosSince > knownStatesMaxAge.getAsLong()) {
      knownVetos.clear();
    }
  }


  /**
   * Must be called while holding workerLock, before the store is read.
   */
  private void startKnownVetosIfEmpty() {
    if (knownVetos.isEmpty()) {
      knownVetosReleaseCounter = releaseCounter.get();
      knownVetosSince = clock.getAsLong();
    }
  }


  /**
   * @return true if the vetos were acquired for a still current request, i.e. the scheduler should be notified
   */
  private boolean processAcquire(AllocationRequest request) {
    synchronized (stateLock) {
      if (request.cancelled || request.state != RequestState.QUEUED) {
        return false;
      }
      request.state = RequestState.ACQUIRING;
      request.retry = false;
      request.attemptSequence = sequence.incrementAndGet();
    }
    boolean acquired;
    try {
      AllocationRequest moreUrgent;
      if (isKnownToBeBlocked(request, knownVetos)) {
        statSkippedKnownBlocked.incrementAndGet();
        if (logger.isTraceEnabled()) {
          logger.trace("Vetos for order " + request.orderInformation.getOrderId() + " are known to be in use");
        }
        acquired = false;
      } else if ((moreUrgent = findMoreUrgentRequestForReleasedVeto(request)) != null) {
        statYielded.incrementAndGet();
        handOverTo = moreUrgent;
        if (logger.isDebugEnabled()) {
          logger.debug("Order " + request.orderInformation.getOrderId() + " yields released vetos to more urgent order "
              + moreUrgent.orderInformation.getOrderId());
        }
        acquired = false;
      } else {
        statStoreAllocations.incrementAndGet();
        long start = System.currentTimeMillis();
        acquired = allocateVetosInStore(request.orderInformation, request.exclusiveVetos, request.sharedVetos);
        long took = System.currentTimeMillis() - start;
        statStoreAllocationMillis.addAndGet(took);
        if (logger.isTraceEnabled()) {
          logger.trace("Store allocation for order " + request.orderInformation.getOrderId() + ": " + acquired + ", took " + took + " ms");
        }
      }
    } catch (Exception e) {
      knownVetos.clear();
      logger.error("Error allocating shared resource vetos. " + e.getMessage(), e);
      acquired = false;
    }
    RemovalVariant releaseVariant = null;
    synchronized (stateLock) {
      if (request.cancelled) {
        releaseVariant = request.cancelVariant;
      } else if (!acquired && request.retry) {
        // scheduler tried again while the request was processed: retry with current data
        request.retry = false;
        request.state = RequestState.QUEUED;
        workQueue.add(WorkItem.acquire(request));
      } else {
        request.state = acquired ? RequestState.ACQUIRED : RequestState.NOT_ACQUIRED;
      }
    }
    if (releaseVariant != null) {
      if (acquired || releaseVariant == RemovalVariant.REMOVE_PENDING) {
        freeVetosOfOrder(request.orderInformation.getOrderId(), releaseVariant);
      }
      return false;
    }
    return acquired;
  }


  /**
   * A veto released by this node is handed to the most urgent local order first: the scheduler may ask less
   * urgent orders first, because a release can happen at any time during a scheduling run.
   * <p>
   * The request yields, if a more urgent open request needs one of its vetos, this veto was released after the more
   * urgent request was tried last, and the more urgent order is still asked by the scheduler. As more urgent orders
   * are asked first within a scheduling run, the more urgent order has to be asked since the request of this order
   * before the previous one; otherwise it cannot be scheduled for other reasons, e.g. missing capacities.
   * The worker then tries to acquire the vetos for the more urgent request directly, see {@link #handOver}.
   * @return the most urgent such request or null
   */
  private AllocationRequest findMoreUrgentRequestForReleasedVeto(AllocationRequest request) {
    if (releaseSequences.isEmpty()) {
      return null;
    }
    AllocationRequest mostUrgent = null;
    synchronized (stateLock) {
      for (AllocationRequest other : requests.values()) {
        if (other == request || other.cancelled
            || (other.state != RequestState.QUEUED && other.state != RequestState.NOT_ACQUIRED)
            || other.lastAskSequence <= request.beforePreviousAskSequence
            || AllocationRequest.compareUrgency(other, request) >= 0
            || (mostUrgent != null && AllocationRequest.compareUrgency(other, mostUrgent) >= 0)) {
          continue;
        }
        if (isReleasedSinceLastTry(request.exclusiveVetos, other.exclusiveVetos, other)
            || isReleasedSinceLastTry(request.exclusiveVetos, other.sharedVetos, other)
            || isReleasedSinceLastTry(request.sharedVetos, other.exclusiveVetos, other)) {
          mostUrgent = other;
        }
      }
    }
    return mostUrgent;
  }


  private boolean isReleasedSinceLastTry(List<String> vetos, List<String> otherVetos, AllocationRequest other) {
    for (String veto : vetos) {
      if (otherVetos.contains(veto)) {
        Long released = releaseSequences.get(veto);
        if (released != null && released > other.attemptSequence) {
          return true;
        }
      }
    }
    return false;
  }


  private void rememberReleases(Collection<String> vetos) {
    if (vetos.isEmpty()) {
      return;
    }
    Long released = sequence.incrementAndGet();
    for (String veto : vetos) {
      releaseSequences.put(veto, released);
    }
  }


  /**
   * Tries to acquire the vetos for a more urgent request without waiting for the scheduler to ask it again.
   * A queued work item of this request is ignored afterwards, as the request is no longer QUEUED.
   * @return true if the vetos were acquired
   */
  private boolean handOver(AllocationRequest request) {
    synchronized (stateLock) {
      if (request.cancelled || (request.state != RequestState.QUEUED && request.state != RequestState.NOT_ACQUIRED)) {
        return false;
      }
      request.state = RequestState.QUEUED;
    }
    return processAcquire(request);
  }


  /**
   * Executed by the worker thread, holding workerLock.
   * @return true if all vetos have been allocated in the store
   */
  private boolean allocateVetosInStore(OrderInformation orderInformation, List<String> exclusiveVetos,
                                       List<String> sharedVetos) {
    VetoUpdaterComplete updater = new VetoUpdaterComplete(exclusiveVetos, sharedVetos, orderInformation);
    if (updater.getVetoIds().isEmpty()) {
      return true;
    }
    knownVetos.keySet().removeAll(updater.getVetoIds());
    startKnownVetosIfEmpty();
    SharedResourceRequestResult<SharedResourceVeto> readResult = store.read(updater.getVetoIds());
    if (!readResult.isSuccess()) {
      logger.error("Error reading shared resource vetos.", readResult.getException());
      return false;
    }
    logVetos(readResult.getResources(), orderInformation.getOrderId());
    
    // For vetos not yet existing: Insert uninitialized objects so that they too can be handled with update command below
    List<SharedResourceInstance<SharedResourceVeto>> uninitializedList = buildEmptyVetoList(exclusiveVetos, sharedVetos,
                                                                                            readResult);
    if (!uninitializedList.isEmpty()) {
      SharedResourceRequestResult<SharedResourceVeto> createResult = store.create(uninitializedList);
      if (!createResult.isSuccess()) {
        logger.error("Error inserting uninitialized shared resource vetos.", createResult.getException());
        return false;
      }
    }
    
    // First try: Try to update full scope until encountering an existing veto that disallows order start, in that case stop and rollback
    SharedResourceRequestResult<SharedResourceVeto> updateResult = store.update(updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      if (!updater.isOrderStartDisallowed()) {
        handleLogUpdateException(updateResult);
        return false;
      }
    }
    if(logger.isDebugEnabled()) {
      logger.debug("Update Veto Result for " + orderInformation.getOrderId() + ": Return allocation success? " +
                   !updater.isOrderStartDisallowed());
    }
    if (!updater.isOrderStartDisallowed()) {
      rememberVetoStates(knownVetos, orderInformation, exclusiveVetos, sharedVetos, readResult, true);
      return true;
    }
    // Second try, if order start is disallowed: Update only pendingExclusive vetos where possible
    if (allocateOnlyPendingVetos(exclusiveVetos, orderInformation)) {
      rememberVetoStates(knownVetos, orderInformation, exclusiveVetos, sharedVetos, readResult, false);
    }
    return false;
  }


  /**
   * Remembers the veto states after an allocation attempt, derived from the states read before the attempt.
   */
  private static void rememberVetoStates(Map<String, SharedResourceVeto> knownVetos, OrderInformation orderInformation,
                                         List<String> exclusiveVetos, List<String> sharedVetos,
                                         SharedResourceRequestResult<SharedResourceVeto> readResult, boolean allocated) {
    VetoMap existing = new VetoMap(readResult.getResources());
    AllocatonHelper helper = new AllocatonHelper();
    for (String id : exclusiveVetos) {
      SharedResourceVeto veto = readVetoState(existing, id);
      if (veto != null) {
        knownVetos.put(id, allocated ? helper.allocateExclusiveVeto(orderInformation, veto).getVeto()
                                     : helper.allocateOnlyPendingExclusiveVeto(orderInformation, veto).getVeto());
      }
    }
    for (String id : sharedVetos) {
      if (exclusiveVetos.contains(id)) {
        continue;
      }
      SharedResourceVeto veto = readVetoState(existing, id);
      if (veto != null) {
        knownVetos.put(id, allocated ? helper.allocateSharedVeto(orderInformation, veto).getVeto() : veto);
      }
    }
  }


  private static SharedResourceVeto readVetoState(VetoMap existing, String id) {
    SharedResourceInstance<SharedResourceVeto> sri = existing.getVeto(id);
    if (sri == null) {
      return new SharedResourceVeto();
    }
    if (sri.getValue() == null) {
      return null;
    }
    return sri.getValue().doClone();
  }


  /**
   * @return true if the known veto states show that the allocation would fail without changing the store
   */
  private static boolean isKnownToBeBlocked(AllocationRequest request, Map<String, SharedResourceVeto> knownVetos) {
    if (knownVetos.isEmpty()) {
      return false;
    }
    AllocatonHelper helper = new AllocatonHelper();
    OrderInformation orderInformation = request.orderInformation;
    boolean blocked = false;
    for (String id : request.exclusiveVetos) {
      SharedResourceVeto veto = knownVetos.get(id);
      if (veto == null) {
        return false;
      }
      if (helper.allocateExclusiveVeto(orderInformation, veto.doClone()).isOrderStartDisallowed()) {
        blocked = true;
      }
      SharedResourceVeto pending = helper.allocateOnlyPendingExclusiveVeto(orderInformation, veto.doClone()).getVeto();
      if (!Objects.equals(pending.pendingExclusiveOrderId, veto.pendingExclusiveOrderId)) {
        return false; // the store has to be updated to mark the veto as pending
      }
    }
    for (String id : request.sharedVetos) {
      if (request.exclusiveVetos.contains(id)) {
        continue;
      }
      SharedResourceVeto veto = knownVetos.get(id);
      if (veto == null) {
        return false;
      }
      if (helper.allocateSharedVeto(orderInformation, veto.doClone()).isOrderStartDisallowed()) {
        blocked = true;
      }
    }
    return blocked;
  }
  
  
  private boolean allocateOnlyPendingVetos(List<String> exclusiveVetos, OrderInformation orderInformation) {
    VetoUpdaterPendingOnly updater = new VetoUpdaterPendingOnly(exclusiveVetos, orderInformation);
    if (updater.getVetoIds().isEmpty()) {
      return true;
    }
    SharedResourceRequestResult<SharedResourceVeto> updateResult = store.update(updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      handleLogUpdateException(updateResult);
      return false;
    }
    return true;
  }
  
  
  private void handleLogUpdateException(SharedResourceRequestResult<SharedResourceVeto> updateResult) {
    if (updateResult == null) { return; }
    if (updateResult.getException() instanceof XNWH_SharedResourceInstanceDoesNotExist) {
      if (logger.isTraceEnabled()) {
        logger.trace("Veto entry was presumably deleted by other thread");
      }
    } else {
      logger.error("Error updating shared resource vetos.", updateResult.getException());
    }
  }
  
  
  public List<SharedResourceInstance<SharedResourceVeto>> buildEmptyVetoList(
                                                            List<String> exclusiveVetos, List<String> sharedVetos,
                                                            SharedResourceRequestResult<SharedResourceVeto> existingList) {
    long now = System.currentTimeMillis();
    VetoMap existing = new VetoMap(existingList.getResources());
    List<SharedResourceInstance<SharedResourceVeto>> ret = new ArrayList<>();
    if (exclusiveVetos != null) {
      for (String id : exclusiveVetos) {
        if (!existing.contains(id)) {
          SharedResourceInstance<SharedResourceVeto> sri = new SharedResourceInstance<>(id, now, new SharedResourceVeto());
          ret.add(sri);
        }
      }
    }
    if (sharedVetos != null) {
      for (String id : sharedVetos) {
        if (!existing.contains(id)) {
          SharedResourceInstance<SharedResourceVeto> sri = new SharedResourceInstance<>(id, now, new SharedResourceVeto());
          ret.add(sri);
        }
      }
    }
    return ret;
  }
  
  
  @Override
  @Deprecated
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    undoAllocation(orderInformation, vetos, Collections.emptyList());
  }

  /**
   * Called by the scheduler thread; the release in the store is done by the worker.
   * <p>
   * The scheduler also calls undo directly after this algorithm returned FAILED for the same scheduling attempt.
   * In that case the request has to be kept so that the worker can still acquire the vetos. Only an allocation
   * which was handed out to the scheduler (SUCCESS) is released.
   */
  @Override
  public void undoAllocation(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos) {
    Long orderId = orderInformation.getOrderId();
    synchronized (stateLock) {
      AllocationRequest request = requests.get(orderId);
      if (request == null || request.state != RequestState.CONSUMED) {
        return;
      }
      requests.remove(orderId);
      cancel(request, RemovalVariant.IGNORE_PENDING);
    }
    workerTrigger.run();
  }

  @Override
  @Deprecated
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    finalizeAllocation(orderInformation, vetos, Collections.emptyList());
  }

  @Override
  public void finalizeAllocation(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos) {
    // allocation is already persisted in the store, only local request state has to be removed
    synchronized (stateLock) {
      AllocationRequest request = requests.get(orderInformation.getOrderId());
      if (request != null && (request.state == RequestState.CONSUMED || request.state == RequestState.ACQUIRED)) {
        requests.remove(orderInformation.getOrderId());
      }
    }
  }


  @Override
  public boolean freeVetos(OrderInformation orderInformation) {
    return freeVetosForced(orderInformation.getOrderId());
  }


  @Override
  public boolean freeVetosForced(long orderId) {
    synchronized (stateLock) {
      AllocationRequest request = requests.remove(orderId);
      if (request != null && (request.state == RequestState.QUEUED || request.state == RequestState.ACQUIRING)) {
        request.cancelled = true;
        request.cancelVariant = RemovalVariant.REMOVE_PENDING;
      }
    }
    return freeVetosOfOrder(orderId, RemovalVariant.REMOVE_PENDING);
  }


  private boolean freeVetosOfOrder(long orderId, RemovalVariant removalVariant) {
    long start = System.currentTimeMillis();
    List<String> released = new ArrayList<>();
    try {
      return freeVetosOfOrderImpl(orderId, removalVariant, released);
    } catch (Exception e) {
      logger.error("Error trying to free shared resource vetos. " + e.getMessage(), e);
      return false;
    } finally {
      rememberReleases(released);
      releaseCounter.incrementAndGet();
      long took = System.currentTimeMillis() - start;
      statFreeCalls.incrementAndGet();
      statFreeMillis.addAndGet(took);
      if (logger.isDebugEnabled()) {
        logger.debug("Freed vetos of order " + orderId + " (" + removalVariant + ") in " + took + " ms");
      }
    }
  }
  
  
  /**
   * @param released receives the ids of all vetos which are changed by the release
   */
  private boolean freeVetosOfOrderImpl(long orderId, RemovalVariant removalVariant, List<String> released) {
    SharedResourceRequestResult<SharedResourceVeto> readVetosResult = store.readAll();
    if (!readVetosResult.isSuccess()) {
      return false;
    }
    List<SharedResourceInstance<SharedResourceVeto>> allVetos = readVetosResult.getResources();
    logVetos(allVetos, orderId);
    if (allVetos == null) { return true; }
    if (allVetos.isEmpty()) { return true; }

    List<String> exclusiveIds = allVetos.stream().filter(x -> (x.getValue() != null) && Objects.equals(x.getValue().usingOrderId, orderId))
                                        .map(x -> x.getId()).collect(Collectors.toList());
    released.addAll(exclusiveIds);
    if (!exclusiveIds.isEmpty()) {
      SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = store.delete(exclusiveIds);
      if (!deleteVetosResult.isSuccess()) {
        logger.error("Error freeing shared resource vetos.", deleteVetosResult.getException());
        return false;
      }
    }
    VetoRemover remover = new VetoRemover(orderId, removalVariant);
    List<String> updateIds = allVetos.stream().filter(remover).map(x -> x.getId()).collect(Collectors.toList());
    released.addAll(updateIds);
    SharedResourceRequestResult<SharedResourceVeto> updateResult = store.update(updateIds, remover);
    if (!updateResult.isSuccess()) {
      logger.error("Error freeing shared resource vetos.", updateResult.getException());
      return false;
    }
    if (remover.getIdsToDelete().isEmpty()) {
      return true;
    }
    SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = store.delete(remover.getIdsToDelete());
    if (!deleteVetosResult.isSuccess()) {
      logger.error("Error freeing shared resource vetos.", deleteVetosResult.getException());
      return false;
    }
    return true;
  }

  
  private void logVetos(List<SharedResourceInstance<SharedResourceVeto>> list, Long orderId) {
    try {
      if (!logger.isTraceEnabled()) { return; }
      String prefix = "Existing veto list (order = " + orderId + ") ";
      if (list == null) {
        logger.trace(prefix + "No existing vetos");
      }
      for (int i = 0; i < list.size(); i++) {
        SharedResourceInstance<SharedResourceVeto> item = list.get(i);
        if ((item == null) || (item.getValue() == null)) {
          logger.trace(prefix + "# " + i + " -> null");
        }
        logger.trace(prefix + "# " + i + " -> " + item.getValue().asString());
      }
    } catch (Exception e) {}
  }
  
  
  @Override
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    try {
      allocateAdministrativeVetoImpl(administrativeVeto);
    } catch (XPRC_AdministrativeVetoAllocationDenied | PersistenceLayerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error allocating administrative shared resource veto. " + e.getMessage(), e);
    }
  }
  
  
  private void allocateAdministrativeVetoImpl(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    SharedResourceRequestResult<SharedResourceVeto> readResult = store.read(List.of(administrativeVeto.getName()));
    if (!readResult.isSuccess()) {
      logger.error("AllocateAdministrativeVeto: Error reading shared resource vetos.", readResult.getException());
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), null, readResult.getException());
    }
    if ((readResult.getResources() == null) || (readResult.getResources().isEmpty())) {
      createNewAdministrativeVeto(administrativeVeto);
      return;
    }
    Long blockingOrderId = null;
    if ((readResult.getResources().get(0) != null) && (readResult.getResources().get(0).getValue() != null)) {
      SharedResourceVeto veto = readResult.getResources().get(0).getValue();
      if (veto.usingOrderId != null) {
        blockingOrderId = veto.usingOrderId;
      } else if (veto.pendingExclusiveOrderId != null) {
        blockingOrderId = veto.pendingExclusiveOrderId;
      }
      if (blockingOrderId != null) {
        if (AdministrativeVeto.ADMIN_VETO_ORDERID.equals(blockingOrderId)) {
          return;
        }
        throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId);
      }
    }
    throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), null);
  }
  
  
  private void createNewAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoAllocationDenied,
                                                                                         PersistenceLayerException {
    SharedResourceVeto srVeto = new SharedResourceVeto();
    srVeto.documentation = administrativeVeto.getDocumentation();
    srVeto.usingOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
    srVeto.usingOrderType = AdministrativeVeto.ADMIN_VETO_ORDERTYPE;
    srVeto.usingRootOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
    Long now = System.currentTimeMillis();
    SharedResourceInstance<SharedResourceVeto> veto = new SharedResourceInstance<SharedResourceVeto>(administrativeVeto.getName(), now, srVeto);
    SharedResourceRequestResult<SharedResourceVeto> createResult = store.create(List.of(veto));
    if (!createResult.isSuccess()) {
      SharedResourceRequestResult<SharedResourceVeto> readResult = store.read(List.of(veto.getId()));
      Long usingOrderId = -2l;
      if (readResult.isSuccess() && readResult.getResources() != null && !readResult.getResources().isEmpty()) {
        usingOrderId = readResult.getResources().get(0).getValue().usingOrderId;
      }
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), usingOrderId);
    }
  }
  
  
  @Override
  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DocumentationContainer oldDoc = new DocumentationContainer();

    Function<SharedResourceInstance<SharedResourceVeto>, SharedResourceInstance<SharedResourceVeto>> update = (x) -> {
      oldDoc.documentation = x.getValue().documentation;
      x.getValue().documentation = administrativeVeto.getDocumentation();
      return new SharedResourceInstance<SharedResourceVeto>(x.getId(), x.getCreated(), x.getValue());
    };

    SharedResourceRequestResult<SharedResourceVeto> updateResult;
    updateResult = store.update(List.of(administrativeVeto.getName()), update);
    if (!updateResult.isSuccess()) {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(administrativeVeto.getName(), VetoInformationStorable.TABLE_NAME);
    }

    return oldDoc.documentation;
  }


  @Override
  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    try {
      return freeAdministrativeVetoImpl(administrativeVeto);
    } catch (XPRC_AdministrativeVetoDeallocationDenied | PersistenceLayerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error trying to free administrative shared resource vetos. " + e.getMessage(), e);
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName(), e);
    } finally {
      rememberReleases(List.of(administrativeVeto.getName()));
      releaseCounter.incrementAndGet();
    }
  }
  
  
  private VetoInformation freeAdministrativeVetoImpl(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    SharedResourceRequestResult<SharedResourceVeto> readResult = store.read(List.of(administrativeVeto.getName()));
    if (!readResult.isSuccess() || readResult.getResources() == null || readResult.getResources().isEmpty()) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    if (readResult.getResources().size() > 1) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    if (readResult.getResources().get(0) == null) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    SharedResourceVeto existing = readResult.getResources().get(0).getValue();
    if (!Objects.equals(AdministrativeVeto.ADMIN_VETO_ORDERID, existing.usingOrderId)) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = store.delete(List.of(administrativeVeto.getName()));
    if (!deleteVetosResult.isSuccess()) {
      logger.error("Error trying to delete administrative shared resource vetos.", deleteVetosResult.getException());
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    Long created = administrativeVeto.getCreated();
    OrderInformation orderInfo = null;
    if ((existing.usingOrderType != null) && (existing.usingOrderId != null)) {
      orderInfo = new OrderInformation(existing.usingOrderId, existing.usingRootOrderId, existing.usingOrderType);
    }
    VetoInformation info = new VetoInformation(readResult.getResources().get(0).getId(), orderInfo, existing.sharedOrderIds,
                                               existing.pendingExclusiveOrderId, existing.documentation, created, 0);
    return info;
  }


  @Override
  public Collection<VetoInformation> listVetos() {
    List<VetoInformation> result = new ArrayList<>();
    try {
      SharedResourceRequestResult<SharedResourceVeto> vetoData = store.readAll();
      if (!vetoData.isSuccess() || vetoData.getResources() == null) {
        Collections.emptyList();
      }
  
      for (SharedResourceInstance<SharedResourceVeto> instance : vetoData.getResources()) {
        SharedResourceVeto value = instance.getValue();
        OrderInformation orderInfo = null;
        if (value.usingOrderId != null) {
          orderInfo = new OrderInformation(value.usingOrderId, value.usingRootOrderId, value.usingOrderType);
        }
        VetoInformation info = new VetoInformation(instance.getId(), orderInfo, value.sharedOrderIds, value.pendingExclusiveOrderId,
                                                   value.documentation, instance.getCreated(), 0);
        result.add(info);
      }
    } catch (Exception e) {
      logger.error("Error in list shared resource vetos. " + e.getMessage(), e);
    }
    return result;
  }


  @Override
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    List<VetoInformation> filtered = CollectionUtils.filter(listVetos(), new VetoFilter(select));
    int countAll = filtered.size();
    if (maxRows >= 0) {
      filtered = filtered.subList(0, Math.min(countAll, maxRows));
    }
    List<VetoInformationStorable> viss = CollectionUtils.transform(filtered, VetoInformationStorable.fromVetoInformation);
    return new VetoSearchResult(viss, countAll);
  }


  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.SharedResourceManagement;
  }


  @Override
  public String showInformation() {
    StringBuilder sb = new StringBuilder();
    sb.append(getAlgorithmType()).append(": ").append(getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN));
    long storeAllocations = statStoreAllocations.get();
    long freeCalls = statFreeCalls.get();
    sb.append("\n  SharedResourceVetoWorker: ").append(workerExecutor != null ? "running" : "no thread");
    sb.append(", ").append(workQueue.size()).append(" queued work items, ").append(requests.size()).append(" open requests");
    sb.append("\n  ").append(statWorkerRuns.get()).append(" worker runs, ").append(statAcquireItems.get()).append(" acquire items: ")
        .append(statAcquired.get()).append(" acquired, ").append(storeAllocations).append(" store allocations (avg ")
        .append(storeAllocations == 0 ? 0 : statStoreAllocationMillis.get() / storeAllocations).append(" ms), ")
        .append(statSkippedKnownBlocked.get()).append(" skipped as known blocked, ").append(statYielded.get())
        .append(" yielded to more urgent orders");
    sb.append("\n  ").append(statReleaseItems.get()).append(" release items, ").append(freeCalls).append(" frees (avg ")
        .append(freeCalls == 0 ? 0 : statFreeMillis.get() / freeCalls).append(" ms)");
    return sb.toString();
  }

  
  /**
   * Access to the veto shared resource {@link #XYNA_VETO_SR_DEF}.
   */
  interface VetoStore {

    SharedResourceRequestResult<SharedResourceVeto> create(List<SharedResourceInstance<SharedResourceVeto>> data);

    SharedResourceRequestResult<SharedResourceVeto> read(List<String> ids);

    SharedResourceRequestResult<SharedResourceVeto> readAll();

    SharedResourceRequestResult<SharedResourceVeto> update(List<String> ids,
                                                           Function<SharedResourceInstance<SharedResourceVeto>, SharedResourceInstance<SharedResourceVeto>> update);

    SharedResourceRequestResult<SharedResourceVeto> delete(List<String> ids);
  }


  private static class SrmVetoStore implements VetoStore {

    private final SharedResourceManagement srm;

    private SrmVetoStore(SharedResourceManagement srm) {
      this.srm = srm;
    }

    public SharedResourceRequestResult<SharedResourceVeto> create(List<SharedResourceInstance<SharedResourceVeto>> data) {
      return srm.create(XYNA_VETO_SR_DEF, data);
    }

    public SharedResourceRequestResult<SharedResourceVeto> read(List<String> ids) {
      return srm.read(XYNA_VETO_SR_DEF, ids);
    }

    public SharedResourceRequestResult<SharedResourceVeto> readAll() {
      return srm.readAll(XYNA_VETO_SR_DEF);
    }

    public SharedResourceRequestResult<SharedResourceVeto> update(List<String> ids,
                                                                  Function<SharedResourceInstance<SharedResourceVeto>, SharedResourceInstance<SharedResourceVeto>> update) {
      return srm.update(XYNA_VETO_SR_DEF, ids, update);
    }

    public SharedResourceRequestResult<SharedResourceVeto> delete(List<String> ids) {
      return srm.delete(XYNA_VETO_SR_DEF, ids);
    }
  }


  private static enum RequestState {
    QUEUED, ACQUIRING, ACQUIRED, CONSUMED, NOT_ACQUIRED
  }


  /**
   * Allocation request of one order. Fields are guarded by stateLock.
   */
  private static class AllocationRequest {

    private final OrderInformation orderInformation;
    private final List<String> exclusiveVetos;
    private final List<String> sharedVetos;
    private final long created = System.currentTimeMillis();
    private RequestState state = RequestState.QUEUED;
    private boolean cancelled = false;
    private RemovalVariant cancelVariant;
    /** set if the scheduler asked again while the request was queued or being processed */
    private boolean retry = false;
    private long urgency;
    private long lastAskSequence;
    private long previousAskSequence;
    private long beforePreviousAskSequence;
    private long attemptSequence;

    private AllocationRequest(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos) {
      this.orderInformation = orderInformation;
      this.exclusiveVetos = normalize(exclusiveVetos);
      this.sharedVetos = normalize(sharedVetos);
    }

    private void asked(long urgency, long askSequence) {
      this.urgency = urgency;
      this.beforePreviousAskSequence = this.previousAskSequence;
      this.previousAskSequence = this.lastAskSequence;
      this.lastAskSequence = askSequence;
    }

    /**
     * @return negative if a is more urgent than b: higher urgency first, older order first
     */
    private static int compareUrgency(AllocationRequest a, AllocationRequest b) {
      int cmp = Long.compare(b.urgency, a.urgency);
      if (cmp != 0) {
        return cmp;
      }
      return Long.compare(a.orderInformation.getOrderId(), b.orderInformation.getOrderId());
    }

    private static List<String> normalize(List<String> vetos) {
      if (vetos == null) {
        return Collections.emptyList();
      }
      return Collections.unmodifiableList(new ArrayList<>(new TreeSet<>(vetos)));
    }

    private boolean matches(List<String> exclusiveVetos, List<String> sharedVetos) {
      return this.exclusiveVetos.equals(normalize(exclusiveVetos)) && this.sharedVetos.equals(normalize(sharedVetos));
    }
  }


  private static class WorkItem {

    private final long orderId;
    private final AllocationRequest request;
    private final RemovalVariant variant;

    private WorkItem(long orderId, AllocationRequest request, RemovalVariant variant) {
      this.orderId = orderId;
      this.request = request;
      this.variant = variant;
    }

    private static WorkItem acquire(AllocationRequest request) {
      return new WorkItem(request.orderInformation.getOrderId(), request, null);
    }

    private static WorkItem release(long orderId, RemovalVariant variant) {
      return new WorkItem(orderId, null, variant);
    }

    /**
     * Releases first (keeping their order), then acquires by descending urgency.
     */
    private static int compareProcessingOrder(WorkItem a, WorkItem b) {
      if (a.request == null || b.request == null) {
        return Boolean.compare(a.request != null, b.request != null);
      }
      return AllocationRequest.compareUrgency(a.request, b.request);
    }
  }


  public static class SharedResourceVeto {
    public boolean lockedForDelete = false;
    public Long usingOrderId;
    public Long usingRootOrderId;
    public String usingOrderType;
    public String documentation;
    public ArrayList<Long> sharedOrderIds;
    public Long pendingExclusiveOrderId;
    
    public SharedResourceVeto() {}
    
    public SharedResourceVeto(Long usingOrderId, Long usingRootOrderId, String usingOrderType, String documentation,
                              List<Long> sharedOrderIds, Long pendingExclusiveOrderId) {
      this.usingOrderId = usingOrderId;
      this.usingRootOrderId = usingRootOrderId;
      this.usingOrderType = usingOrderType;
      this.documentation = documentation;
      if (sharedOrderIds != null) {
        this.sharedOrderIds = new ArrayList<>(sharedOrderIds);
      }
      this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    }
    
    public SharedResourceVeto buildLockedForDelete() {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.lockedForDelete = true;
      return ret;
    }
    
    public SharedResourceVeto doClone() {
      if (lockedForDelete) {
        return buildLockedForDelete();
      }
      return new SharedResourceVeto(usingOrderId, usingRootOrderId, usingOrderType, documentation,
                                    sharedOrderIds, pendingExclusiveOrderId);
    }

    public boolean isEmpty() {
      if (usingOrderId != null) { return false; }
      if (pendingExclusiveOrderId != null) { return false; }
      if ((sharedOrderIds != null) && (!sharedOrderIds.isEmpty())) {
        return false;
      }
      return true;
    }
    
    public String asString() {
      StringBuilder s = new StringBuilder();
      s.append("{");
      s.append("usingOrderId: ").append(usingOrderId);
      s.append(", pendingExclusiveOrderId: ").append(pendingExclusiveOrderId);
      s.append(", sharedOrderIds: ");
      if (sharedOrderIds == null) { s.append("null"); }
      else {
        s.append("[");
        boolean isfirst = true;
        for (Long val : sharedOrderIds) {
          if (isfirst) { isfirst = false; }
          else { s.append(", "); }
          s.append(val);
        }
        s.append("]");
      }
      s.append(" }");
      return s.toString();
    }
  }
  

  private static class DocumentationContainer {

    private String documentation;
  }
  
  
  public static class VetoMap {
    private Map<String, SharedResourceInstance<SharedResourceVeto>> _map = new HashMap<>();
    
    protected VetoMap() {}
    
    public VetoMap(List<SharedResourceInstance<SharedResourceVeto>> list) {
      if (list == null) { return; }
      for (SharedResourceInstance<SharedResourceVeto> item : list) {
        _map.put(item.getId(), item);
      }
    }
    
    public boolean contains(String id) {
      return _map.containsKey(id);
    }
    
    public SharedResourceInstance<SharedResourceVeto> getVeto(String id) {
      return _map.get(id);
    }
  }
  
  
  public static enum RemovalVariant {
    REMOVE_PENDING, IGNORE_PENDING
  }
  
  
  public static class VetoUpdateData {
    private final SharedResourceVeto veto;
    private boolean orderStartDisallowed = false;
    
    public VetoUpdateData(SharedResourceVeto veto) {
      this.veto = veto;
    }
    public SharedResourceVeto getVeto() {
      return veto;
    }
    public boolean isOrderStartDisallowed() {
      return orderStartDisallowed;
    }
    public VetoUpdateData setOrderStartDisallowed(boolean orderStartDisallowed) {
      this.orderStartDisallowed = orderStartDisallowed;
      return this;
    }
  }
  
  
  public static class AllocatonHelper {

    public VetoUpdateData allocateOnlyPendingExclusiveVeto(OrderInformation orderInfo,
                                                           SharedResourceVeto veto) {
      if (veto.lockedForDelete) {
        return new VetoUpdateData(veto);
      }
      if (veto.usingOrderId != null) {
        return new VetoUpdateData(veto);
      }
      if (veto.pendingExclusiveOrderId != null) {
        return new VetoUpdateData(veto);
      }
      if ((veto.sharedOrderIds != null) && (!veto.sharedOrderIds.isEmpty())) {
        veto.pendingExclusiveOrderId = orderInfo.getOrderId();
        return new VetoUpdateData(veto);
      }
      return new VetoUpdateData(veto);
    }
    
    public VetoUpdateData allocateExclusiveVeto(OrderInformation orderInfo, SharedResourceVeto veto) {
      if (veto.lockedForDelete) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.isEmpty()) {
        return new VetoUpdateData(this.createExclusiveSRVeto(orderInfo));
      }
      if (veto.usingOrderId != null) {
        if (!orderInfo.getOrderId().equals(veto.usingOrderId)) {
          return new VetoUpdateData(veto).setOrderStartDisallowed(true);
        }
        return new VetoUpdateData(veto);
      }
      if ((veto.sharedOrderIds != null) && (!veto.sharedOrderIds.isEmpty())) {
        // pendingExclusiveOrderId will be updated later
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.pendingExclusiveOrderId != null) {
        if (!veto.pendingExclusiveOrderId.equals(orderInfo.getOrderId())) {
          return new VetoUpdateData(veto).setOrderStartDisallowed(true);
        }
        return new VetoUpdateData(this.createExclusiveSRVeto(orderInfo));
      }
      // should not be reachable, something unexpected happened
      return new VetoUpdateData(veto).setOrderStartDisallowed(true);
    }
  
    public VetoUpdateData allocateSharedVeto(OrderInformation orderInfo, SharedResourceVeto veto) {
      if (veto.lockedForDelete) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.isEmpty()) {
        return new VetoUpdateData(this.createSharedSRVeto(orderInfo));
      }
      if (veto.usingOrderId != null) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.pendingExclusiveOrderId != null) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.sharedOrderIds == null) {
        veto.sharedOrderIds = new ArrayList<>();
        veto.sharedOrderIds.add(orderInfo.getOrderId());
        return new VetoUpdateData(veto);
      }
      if (veto.sharedOrderIds.contains(orderInfo.getOrderId())) { 
        return new VetoUpdateData(veto);
      }
      veto.sharedOrderIds.add(orderInfo.getOrderId());
      return new VetoUpdateData(veto);
    }
    
    private SharedResourceVeto createExclusiveSRVeto(OrderInformation orderInformation) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.usingOrderId = orderInformation.getOrderId();
      ret.usingOrderType = orderInformation.getOrderType();
      ret.usingRootOrderId = orderInformation.getRootOrderId();
      return ret;
    }
    
    private SharedResourceVeto createSharedSRVeto(OrderInformation orderInformation) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.sharedOrderIds = new ArrayList<>();
      ret.sharedOrderIds.add(orderInformation.getOrderId());
      return ret;
    }
    
    public SharedResourceInstance<SharedResourceVeto> buildSri(String id, SharedResourceVeto data, long now) {
      return new SharedResourceInstance<>(id, now, data.isEmpty() ? new SharedResourceVeto() : data);
    }
  }
  
  
  public static class VetoRemover implements Function<SharedResourceInstance<SharedResourceVeto>,
                                                      SharedResourceInstance<SharedResourceVeto>>,
                                             Predicate<SharedResourceInstance<SharedResourceVeto>> {
    private final long _orderId;
    private long _now = System.currentTimeMillis();
    private List<String> _idsToDelete = new ArrayList<>();
    private RemovalVariant _variant = RemovalVariant.REMOVE_PENDING;
    
    public VetoRemover(long orderId, RemovalVariant variant) {
      this._orderId = orderId;
      if (variant != null) {
        this._variant = variant;
      }
    }
    
    public List<String> getIdsToDelete() {
      return _idsToDelete;
    }
    
    @Override
    public boolean test(SharedResourceInstance<SharedResourceVeto> sri) {
      if (sri == null) { return false; }
      if (sri.getValue() == null) { return false; }
      SharedResourceVeto veto = sri.getValue();
      if (veto.lockedForDelete) {
        return false;
      }
      if (veto.usingOrderId != null) {
        return false;
      }
      if ((veto.sharedOrderIds != null) &&
          (veto.sharedOrderIds.contains(_orderId))) {
        return true;
      }
      if (_variant != RemovalVariant.IGNORE_PENDING) {
        if (Objects.equals(_orderId, veto.pendingExclusiveOrderId)) {
          return true;
        }
      }
      return false;
    }
    
    @Override
    public SharedResourceInstance<SharedResourceVeto> apply(SharedResourceInstance<SharedResourceVeto> sri) {
      if (sri == null) { return null; }
      if (sri.getValue() == null) { return null; }
      if (sri.getValue().lockedForDelete) {
        // veto was changed elsewhere, should not happen
        return sri;
      }
      if (sri.getValue().usingOrderId != null) {
        // veto was changed elsewhere, should not happen
        return sri;
      }
      SharedResourceVeto veto = sri.getValue().doClone();
      boolean matches = false;
      if ((veto.sharedOrderIds != null) &&
          (veto.sharedOrderIds.contains(_orderId))) {
        veto.sharedOrderIds.removeIf(x -> x == _orderId);
        matches = true;
        if (veto.sharedOrderIds.isEmpty() && Objects.equals(AdministrativeVeto.ADMIN_VETO_ORDERID, veto.pendingExclusiveOrderId)) {
          // change pending administrative veto to exclusive state
          veto.usingOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
          veto.usingRootOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
          veto.usingOrderType = AdministrativeVeto.ADMIN_VETO_ORDERTYPE;
          veto.pendingExclusiveOrderId = null;
        }
      }
      if (_variant != RemovalVariant.IGNORE_PENDING) {
        if (Objects.equals(_orderId, veto.pendingExclusiveOrderId)) {
          veto.pendingExclusiveOrderId = null;
          matches = true;
        }
      }
      if (!matches) {
        return sri;
      }
      if (veto.isEmpty()) {
        veto.lockedForDelete = true;
        _idsToDelete.add(sri.getId());
      }
      return new SharedResourceInstance<>(sri.getId(), _now, veto);
    }
  }
  
  
  public static class VetoUpdaterComplete implements Function<SharedResourceInstance<SharedResourceVeto>,
                                                              SharedResourceInstance<SharedResourceVeto>> {
    private long _now = System.currentTimeMillis();
    private OrderInformation _orderInfo;
    private Set<String> _exclusiveVetos = new HashSet<>();
    private Set<String> _sharedVetos = new HashSet<>();
    private List<String> _vetoIds = new ArrayList<>();
    private boolean _orderStartDisallowed = false;
    private AllocatonHelper _allocator = new AllocatonHelper();
    
    public VetoUpdaterComplete(List<String> exclusiveVetos, List<String> sharedVetos, OrderInformation orderInformation) {
      this._orderInfo = orderInformation;
      if (exclusiveVetos != null) {
        this._vetoIds.addAll(exclusiveVetos);
        this._exclusiveVetos.addAll(exclusiveVetos);
      }
      if (sharedVetos != null) {
        this._vetoIds.addAll(sharedVetos);
        this._sharedVetos.addAll(sharedVetos);
      }
    }
    
    @Override
    public SharedResourceInstance<SharedResourceVeto> apply(SharedResourceInstance<SharedResourceVeto> input) {
      if (input == null) { return null; }
      if (input.getValue() == null) { return null; }
      SharedResourceVeto veto = input.getValue().doClone();
      String id = input.getId();
      VetoUpdateData vud;
      if (_exclusiveVetos.contains(id)) {
        vud = _allocator.allocateExclusiveVeto(_orderInfo, veto);
      } else if (_sharedVetos.contains(id)) {
        vud = _allocator.allocateSharedVeto(_orderInfo, veto);
      } else {
        // something unexpected happened
        return null;
      }
      if (vud == null) { return null; }
      if (vud.isOrderStartDisallowed()) {
        _orderStartDisallowed = true;
        return null;
      }
      return _allocator.buildSri(id, vud.getVeto(), _now);
    }
    
    public List<String> getVetoIds() {
      return _vetoIds;
    }

    public boolean isOrderStartDisallowed() {
      return _orderStartDisallowed;
    }
  }

  
  public static class VetoUpdaterPendingOnly implements Function<SharedResourceInstance<SharedResourceVeto>,
                                                                 SharedResourceInstance<SharedResourceVeto>> {
    private long _now = System.currentTimeMillis();
    private OrderInformation _orderInfo;
    private List<String> _exclusiveVetos = new ArrayList<>();
    private AllocatonHelper _allocator = new AllocatonHelper();
    
    public VetoUpdaterPendingOnly(List<String> exclusiveVetos, OrderInformation orderInformation) {
      this._orderInfo = orderInformation;
      if (exclusiveVetos != null) {
        this._exclusiveVetos.addAll(exclusiveVetos);
      }
    }
    
    @Override
    public SharedResourceInstance<SharedResourceVeto> apply(SharedResourceInstance<SharedResourceVeto> input) {
      if (input == null) { return null; }
      if (input.getValue() == null) { return null; }
      SharedResourceVeto veto = input.getValue().doClone();
      String id = input.getId();
      VetoUpdateData vud = _allocator.allocateOnlyPendingExclusiveVeto(_orderInfo, veto);
      return _allocator.buildSri(id, vud.getVeto(), _now);
    }
    
    public List<String> getVetoIds() {
      return _exclusiveVetos;
    }
  }
  
}
