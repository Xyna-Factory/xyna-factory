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
package com.gip.xyna.utils.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;

/**
 * Processes work units {@link MergableWork} that have an initial merge window as well as a batched execution.
 * Use {@link MergingWorkProcessor#defaultMergingWorkProcessor(String)} for an instance with default construction parameters
 * Construction parameters are:
 * loadFactor {@link MergingWorkProcessor#DEFAUL_LOAD_FACTOR}: load factor controls the lock granularity and internal queue size, a higher load factor scales better but has a higher memory footprint
 * mergePhaseDuration {@link MergingWorkProcessor#DEFAULT_MERGE_COLLECTION_DELAY_MILLIS}: duration of the merge window, from submit to processing. A longer merging phase could result in less processing work
 * executionInterval {@link MergingWorkProcessor#DEFAULT_EXECUTOR_INTERVAL}: interval at which the background worker thread is executed unless an earlier execution is requested
 */
public class MergingWorkProcessor<K,S,R> {

  private final static Logger logger = Logger.getLogger(MergingWorkProcessor.class);
  
  public final static long DEFAULT_MERGE_COLLECTION_DELAY_MILLIS = 250;
  public final static long DEFAULT_EXECUTOR_INTERVAL = 100;
  public final static int DEFAUL_LOAD_FACTOR = 64;
  
  private final ConcurrentMap<K, WorkContainer> mergingSet = new ConcurrentHashMap<K, WorkContainer>();
  private final LazyAlgorithmExecutor<WorkExecutor> executor;
  private final ArrayBlockingQueue<WorkOrchestration> workQueue;
  private final HashParallelReentrantReadWriteLocks locks;
  private final AtomicBoolean running;
  private MergePhaseAlgorithm mergePhase;
  
  

  public MergingWorkProcessor(final String processorName, int loadFactor,long executionInterval) {
    locks = new HashParallelReentrantReadWriteLocks(loadFactor);
    workQueue = new ArrayBlockingQueue<WorkOrchestration>(loadFactor * 2);
    executor = new LazyAlgorithmExecutor<WorkExecutor>(processorName + " Executor", executionInterval);
    executor.startNewThread(new WorkExecutor());
    running = new AtomicBoolean(false);
  }
  
  
  public void start(MergePhaseAlgorithm mergePhase) {
    if (running.compareAndSet(false, true)) {
      this.mergePhase = mergePhase;
    }
  }

  
  public Future<R> submit(MergableWork<K,S,R> work) {
    readlock(work);
    try {
      WorkOrchestration future = tryMergeLocked(work);
      if (future != null) {
        return future;
      }
    } finally {
      readunlock(work);
    }
    writelock(work);
    try {
      Future<R> future = tryMergeLocked(work);
      if (future == null) {
        WorkOrchestration wrappedWork = mergePhase.startMergePhase(work);
        WorkContainer container = mergingSet.putIfAbsent(work.getKey(), new WorkContainer(wrappedWork));
        if (container != null) {
          container.add(wrappedWork);
        }
        future = wrappedWork;
      }
      return future;
    } finally {
      writeunlock(work);
    }
  }
  
  
  // currently we do not ensure the order in which tasks where submitted our users have to either rescheduleNow().get() before submitting a task that depends on the order
  // or implement an appropriate merge that would merge order dependent work with previous 
  public Collection<Future<R>> rescheduleNow(K key) {
    writelock(key);
    try {
      WorkContainer container = mergingSet.get(key);
      if (container == null) {
        return Collections.emptyList();
      } else {
        Collection<Future<R>> futuresToWaitFor = new ArrayList<Future<R>>();
        boolean requestExecution = false;
        for (WorkOrchestration work : container.workload) {
          // TODO insert directly into queue if it could be canceled
          if (work.cancelForExecution()) {
            workQueue.add(work);
            requestExecution = true;
          }
          futuresToWaitFor.add(work);
        }
        if (requestExecution) {
          executor.requestExecution();
        }
        return futuresToWaitFor;
      }
    } finally {
      writeunlock(key);
    }
  }
  
  
  /**
   * returns the futures of all work units that could not be canceled, an empty collection therefore means that either there were no tasks or they could all be successfully canceled
   */
  public Collection<Future<R>> cancel(K key) { 
    writelock(key);
    try {
      WorkContainer container = mergingSet.get(key);
      if (container == null) {
        return Collections.emptyList();
      } else {
        return container.cancel(false);
      }
    } finally {
      writeunlock(key);
    }
  }
  
  
  
  public boolean hasWork(K key) {
    readlock(key);
    try {
      return mergingSet.containsKey(key);
    } finally {
      readunlock(key);
    }
  }
  
  
  // callers should hold either the read- or write-lock
  private WorkOrchestration tryMergeLocked(MergableWork<K,S,R> work) {
    if (work.isMergeable()) {
      WorkContainer container = mergingSet.get(work.getKey());
      if (container != null) {
        WorkOrchestration future = container.merge(work);
        if (future != null) {
          return future;
        }
      }
    }
    return null;
  }
  
  
  private class WorkProcessor implements Callable<Void> {
    
    private final WorkOrchestration work;
    
    private WorkProcessor(WorkOrchestration work) {
      this.work = work;
    }

    public Void call() throws Exception {
      if (!work.isCancelled()) {
        workQueue.add(work);
      }
      return null;
    }
    
  }
  
  
  // Callers should hold the writelock
  private boolean removeWorkAndFutureLocked(WorkOrchestration work, boolean andCancel) {
    WorkContainer container = mergingSet.get(work.getKey());
    if (container != null) {
      boolean result = container.remove(work, andCancel);
      if (container.isEmpty()) {
        mergingSet.remove(work.getKey());
      }
      return result;
    } else {
      return false;
    }
  }
  
  
  private void readlock(MergableWork<K,S,R> work) {
    readlock(work.getKey());
  }
  
  private void readlock(K key) {
    locks.readLock(key);
  }
  
  private void readunlock(MergableWork<K,S,R> work) {
    readunlock(work.getKey());
  }
  
  private void readunlock(K key) {
    locks.readUnlock(key);
  }
  
  private void writelock(MergableWork<K,S,R> work) {
    writelock(work.getKey());
  }
  
  private void writelock(K key) {
    locks.writeLock(key);
  }
  
  private void writeunlock(MergableWork<K,S,R> work) {
    writeunlock(work.getKey());
  }
  
  private void writeunlock(K key) {
    locks.writeUnlock(key);
  }
  

  /**
   * Contains two futures
   * - the original future from the ExecutorService, needed for cancellation and rescheduling purpose
   * - a SetableFuture which will contain the result, this is the 'working' future the own interface will delegate to
   *  
   *  Synchronization scheme:
   *  Secured by state progression if possible
   *  In cases where the state is checked and actions are taken upon the progressed state synchronize on the instance
   *  exchange of the synchronized future synchronizes on instance as well
   */
  private class WorkOrchestration implements MergableWork<K, S, R>, Future<R> {

    private SetableFuture<R> resultCarrier;
    private Future<Void> scheduledFuture;
    private final MergableWork<K, S, R> innerWork;
    private final AtomicReference<WorkState> state = new AtomicReference<WorkState>(WorkState.WAITING);
    
    
    public WorkOrchestration(MergableWork<K, S, R> work) {
      this.innerWork = work;
      this.resultCarrier = new SetableFuture<R>();
    }
    
    
    public void setMergeFuture(Future<Void> scheduledFuture) {
      this.scheduledFuture = scheduledFuture;
    }
    
    
    public synchronized boolean cancelForExecution() {
      return state.get() == WorkState.WAITING && (scheduledFuture == null ? true : scheduledFuture.cancel(false));
    }
    

    public void setExecutionResult(R result) {
      if (progressState(WorkState.EXECUTING, WorkState.FINISHED)) {
        this.resultCarrier.set(result);
      } // else { } TODO throw IllegalState or just ignore? 
    }
    
    
    public void setExecutionException(Throwable exception) {
      if (progressState(WorkState.EXECUTING, WorkState.FINISHED)) {
        this.resultCarrier.setException(exception);
      } // else { } TODO throw IllegalState or just ignore? 
    }
    
    
    public synchronized boolean progressState(WorkState expected, WorkState update) {
      return state.compareAndSet(expected, update);
    }
    
    
    public K getKey() {
      return innerWork.getKey();
    }
    

    public S initSharedProcessingData(Iterator<MergableWork<K, S, R>> currentWork) {
      return innerWork.initSharedProcessingData(currentWork);
    }
    

    public R process(S sharedProcessingData) throws Exception {
      return innerWork.process(sharedProcessingData);
    }
    

    public void finalizeSharedProcessingData(S sharedData, Iterator<MergableWork<K, S, R>> currentWork) throws Exception {
      innerWork.finalizeSharedProcessingData(sharedData, currentWork);
    }
    

    public synchronized boolean merge(MergableWork<K, S, R> other) {
      if (state.get() == WorkState.WAITING) {
        return innerWork.merge(other);
      } else {
        return false;
      }
    }
    

    public boolean isMergeable() {
      return innerWork.isMergeable();
    }
    

    // for external cancelation
    public boolean cancel(boolean mayInterruptIfRunning) {
      return cancel(mayInterruptIfRunning, true);
    }
    
    
    // method would need to be synchronized if cancellation was not an end state 
    public boolean cancel(boolean mayInterruptIfRunning, boolean andRemove) {
      boolean canceledStateAttained = progressToCanceledState(mayInterruptIfRunning);
      if (canceledStateAttained) {
        if (andRemove) {
          writelock(this);
          try {
            removeWorkAndFutureLocked(this, false);
            resultCarrier.setException(new CancellationException());
          } finally {
            writeunlock(this);
          }
        }
        if (scheduledFuture != null && 
            !scheduledFuture.isDone()) {
          scheduledFuture.cancel(mayInterruptIfRunning);
        }
        return true;
      } else {
        return false;
      }
    }
    
    
    private synchronized boolean progressToCanceledState(boolean mayInterruptIfRunning) {
      if (progressState(WorkState.WAITING, WorkState.CANCELED)) {
        return true;
      } else if (mayInterruptIfRunning && progressState(WorkState.EXECUTING, WorkState.CANCELED)) {
        return true;
      } else {
        return false;
      }
    }

    public boolean isCancelled() {
      return state.get() == WorkState.CANCELED;
    }

    public synchronized boolean isDone() {
      return state.get() == WorkState.CANCELED || state.get() == WorkState.FINISHED;
    }

    public R get() throws InterruptedException, ExecutionException {
      return resultCarrier.get();
    }

    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return resultCarrier.get(timeout, unit);
    }

    
  }
  
  
  private class WorkContainer {
    
    private List<WorkOrchestration> workload;
    
    private WorkContainer(WorkOrchestration work) {
      workload = new ArrayList<WorkOrchestration>();
      add(work);
    }
    
    
    /**
     *  trys to cancel all Work units in the container and returns those that could not be canceled
     */
    public Collection<Future<R>> cancel(boolean mayInterruptRunning) {
      Collection<Future<R>> notCancelled = new ArrayList<Future<R>>();
      Iterator<WorkOrchestration> iterator = workload.iterator();
      while(iterator.hasNext()) {
        WorkOrchestration work = iterator.next();
        if (!work.cancel(mayInterruptRunning, false)) {
          notCancelled.add(work);
        } else {
          iterator.remove();
        }
      }
      return notCancelled;
    }


    private WorkOrchestration merge(MergableWork<K,S,R> work) {
      for (WorkOrchestration otherWork : workload) {
        synchronized (otherWork) {
          if (otherWork.isMergeable() &&  otherWork.merge(work)) {
            return otherWork;
          }
        }
      }
      return null;
    }
    
    
    private boolean remove(WorkOrchestration work, boolean andCancel) {
      Iterator<WorkOrchestration> iterator = workload.iterator();
      while(iterator.hasNext()) {
        WorkOrchestration currentWork = iterator.next();
        if (currentWork == work) {
          iterator.remove();
          if (andCancel) {
            return currentWork.cancel(false, false);
          } else {
            return true;
          }
        }
      }
      return false;
    }
    
    
    public void add(WorkOrchestration work) {
      workload.add(work);
    }
    
    
    private boolean isEmpty() {
      return workload.size() == 0;
    }
    
  }
  
  
  private class WorkExecutor implements Algorithm {

    @SuppressWarnings("unchecked")
    public void exec() {
      List<WorkOrchestration> works = new ArrayList<WorkOrchestration>();
      workQueue.drainTo(works);
      if (works.size() > 0) {
        S sharedProcessingData = null;
        WorkOrchestration sharedInitializer = null;
        ExecutionData[] executionData = new ExecutionData[works.size()];
        Throwable finalizationException = null;
        ListIterator<WorkOrchestration> workIterator = works.listIterator();
        while (workIterator.hasNext()) {
          int index = workIterator.nextIndex();
          executionData[index] = new ExecutionData();
          WorkOrchestration work = workIterator.next();
          if (work.progressState(WorkState.WAITING, WorkState.EXECUTING)) {
            if (sharedProcessingData == null) { // init shared
              sharedProcessingData = work.initSharedProcessingData(new UnwrappingIterator(works));
              sharedInitializer = work;
            }
            try { // process
              R result = work.process(sharedProcessingData);
              executionData[index].result = result;
            } catch (Throwable t) {
              executionData[index].throwable = t;
            } finally {
              executionData[index].executed = true;
            }
          } // else: nothing to do it should have already gotten it's cancellation exception
        }
        if (sharedProcessingData != null && sharedInitializer != null) {
          try { // finalize
            sharedInitializer.finalizeSharedProcessingData(sharedProcessingData, new UnwrappingIterator(works));
          } catch (Throwable t) {
            finalizationException = t;
          }
        }
        for (int i=0; i < works.size(); i++) {
          WorkOrchestration work = works.get(i);
          writelock(work);
          try { // set results and remove from storage
            if (!removeWorkAndFutureLocked(work, false)) {
              if (logger.isDebugEnabled()) {
                // happens when canceled during execution (if mayInterruptIfRunning=true)
                logger.debug("Processed work is no longer registered as workload: " + work);
              }
            } 
          } finally {
            writeunlock(work);
          }
          if (executionData[i].executed) {
            if (executionData[i].throwable == null) {
              if (finalizationException == null) {
                work.setExecutionResult((R) executionData[i].result);
              } else {
                work.setExecutionException(finalizationException);
              }
            } else {
              work.setExecutionException(executionData[i].throwable);
            }
          }
        }
      }
    }
  }
  
  
  private static class ExecutionData {
    boolean executed = false;
    Object result;
    Throwable throwable;
  }
  
  
  private class UnwrappingIterator implements Iterator<MergableWork<K, S, R>> {
    
    private final Iterator<WorkOrchestration> innerInterator;
    
    private UnwrappingIterator(Collection<WorkOrchestration> works) {
      innerInterator = works.iterator();
    }

    public boolean hasNext() {
      return innerInterator.hasNext();
    }

    public MergableWork<K, S, R> next() {
      return innerInterator.next().innerWork;
    }

    public void remove() {
      throw new UnsupportedOperationException("Removal not supported for initialization iteration on workload");
    }
    
  }
  
  
  private static enum WorkState {
    WAITING, EXECUTING, CANCELED, FINISHED;
  }
  
  
  private abstract class MergePhaseAlgorithm {
    
    protected final ScheduledExecutorService mergePhaseProcessor;
    
    private MergePhaseAlgorithm(final String processorName) {
      mergePhaseProcessor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r);
          t.setDaemon(true);
          t.setName(processorName + " SchedulerThread");
          return t;
        }
      });
    }
    
    public abstract WorkOrchestration startMergePhase(MergableWork<K, S, R> work);
    
  }
  

  private class ConstantOffsetMergingWorkProcessor extends MergePhaseAlgorithm {
    
    private final long mergeDelay;
    private final TimeUnit unit;
    
    private ConstantOffsetMergingWorkProcessor(String processorName, long mergeDelay, TimeUnit unit) {
      super(processorName);
      this.mergeDelay = mergeDelay;
      this.unit = unit;
    }
    

    public WorkOrchestration startMergePhase(MergableWork<K, S, R> work) {
      WorkOrchestration wrappedWork = new WorkOrchestration(work);
      Future<Void> scheduledFuture;
      if (mergeDelay >= 0) {
        scheduledFuture = mergePhaseProcessor.schedule(new WorkProcessor(wrappedWork), mergeDelay, unit);
        wrappedWork.setMergeFuture(scheduledFuture);
      } else {
        workQueue.add(wrappedWork);
      }
      return wrappedWork;
    }

  }
  
  
  public static <K, S, R> MergingWorkProcessor<K, S, R> defaultMergingWorkProcessor(String processorName) {
    MergingWorkProcessor<K, S, R> processor = new MergingWorkProcessor<K, S, R>(processorName, DEFAUL_LOAD_FACTOR, DEFAULT_EXECUTOR_INTERVAL);
    processor.start(processor.new ConstantOffsetMergingWorkProcessor(processorName, DEFAULT_MERGE_COLLECTION_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    return processor;
  }
  
  
  public static <K, S, R> MergingWorkProcessor<K, S, R> newMergingWorkProcessor(String processorName, int loadFactor,long executionInterval, long mergeDelay) {
    MergingWorkProcessor<K, S, R> processor = new MergingWorkProcessor<K, S, R>(processorName, loadFactor, executionInterval);
    processor.start(processor.new ConstantOffsetMergingWorkProcessor(processorName, mergeDelay, TimeUnit.MILLISECONDS));
    return processor;
  }
  
  
}
