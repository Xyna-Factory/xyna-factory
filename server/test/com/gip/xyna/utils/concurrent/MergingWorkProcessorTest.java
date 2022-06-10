/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import junit.framework.TestCase;

import org.junit.Test;

import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;


public class MergingWorkProcessorTest extends TestCase {

  private final static int TEST_LOAD_FACTOR = 8;
  private final static long TEST_MERGE_COLLECTION_DELAY_MILLIS = 500;
  private final static long TEST_EXECUTOR_INTERVAL = 100;
  
  
  private static <K, S, R> MergingWorkProcessor<K, S, R> createTestMWP() {
    return MergingWorkProcessor.newMergingWorkProcessor("Test", TEST_LOAD_FACTOR, TEST_EXECUTOR_INTERVAL, TEST_MERGE_COLLECTION_DELAY_MILLIS);
  }
  
  @Test
  public void testCountingWork() throws InterruptedException, ExecutionException {
    MergingWorkProcessor<Integer, SharedData, Long> processor = createTestMWP();
    Long result = processor.submit(new CountMergesWork()).get();
    assertEquals(new Long(1), result);
    
    FutureCollection<Long> futures = new FutureCollection<Long>();
    futures.add(processor.submit(new CountMergesWork()));
    futures.add(processor.submit(new CountMergesWork()));
    futures.add(processor.submit(new CountMergesWork()));
    futures.add(processor.submit(new CountMergesWork()));
    List<Long> results = futures.get();
    for (Long res : results) {
      assertEquals(new Long(4), res);
    }
    
    futures = new FutureCollection<Long>();
    futures.add(processor.submit(new CountMergesWork(false)));
    futures.add(processor.submit(new CountMergesWork(false)));
    futures.add(processor.submit(new CountMergesWork(false)));
    futures.add(processor.submit(new CountMergesWork(false)));
    results = futures.get();
    for (Long res : results) {
      assertEquals(new Long(1), res);
    }
  }
  
  
  @Test
  public void testCountingCancelation() throws InterruptedException, ExecutionException {
    MergingWorkProcessor<Integer, SharedData, Long> processor = createTestMWP();
    Future<Long> future1 = processor.submit(new CountMergesWork());
    Future<Long> future2 = processor.submit(new CountMergesWork());
    Future<Long> future3 = processor.submit(new CountMergesWork());
    Future<Long> future4 = processor.submit(new CountMergesWork());
    
    final long FUTURE_COMPLETION_OFFSET = (TEST_MERGE_COLLECTION_DELAY_MILLIS + TEST_EXECUTOR_INTERVAL) * 2; 
    
    future4.cancel(false);
    
    try {
      future1.get(FUTURE_COMPLETION_OFFSET, TimeUnit.MILLISECONDS);
      fail();
    } catch (CancellationException e) {
      // success
    } catch (TimeoutException e) {
      fail("Future should have been completed in " + FUTURE_COMPLETION_OFFSET + " " + TimeUnit.MILLISECONDS);
    }
    
    future1 = processor.submit(new CountMergesWork());
    future2 = processor.submit(new CountMergesWork());
    future3 = processor.submit(new CountMergesWork(false));
    future4 = processor.submit(new CountMergesWork());
    
    future3.cancel(false);
    
    try {
      future3.get(FUTURE_COMPLETION_OFFSET, TimeUnit.MILLISECONDS);
      fail();
    } catch (CancellationException e) {
      // success
    } catch (TimeoutException e) {
      fail("Future should have been completed in " + FUTURE_COMPLETION_OFFSET + " "  + TimeUnit.MILLISECONDS);
    }
    
    assertEquals(new Long(3), future1.get());
    assertEquals(new Long(3), future2.get());
    assertEquals(new Long(3), future4.get());
  }
  
  
  @Test
  public void testExecutionTimeRecording() throws InterruptedException, ExecutionException {
    MergingWorkProcessor<Integer, Void, Long> processor = createTestMWP();
    
    FutureCollection<Long> futures = new FutureCollection<Long>();
    futures.add(processor.submit(new ExecutiontimeWork(true)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL + 1);
    futures.add(processor.submit(new ExecutiontimeWork(true)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL);
    futures.add(processor.submit(new ExecutiontimeWork(true)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL);
    futures.add(processor.submit(new ExecutiontimeWork(true)));
    List<Long> results = futures.get();
    Long firstResult = results.get(0);
    for (Long res : results) {
      assertEquals(firstResult, res);
    }
    
    processor = createTestMWP(); // recreation for deterministic executionAlgorithm state
    futures = new FutureCollection<Long>();
    futures.add(processor.submit(new ExecutiontimeWork(false)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL + 1);
    futures.add(processor.submit(new ExecutiontimeWork(false)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL);
    futures.add(processor.submit(new ExecutiontimeWork(false)));
    Thread.sleep(TEST_EXECUTOR_INTERVAL);
    futures.add(processor.submit(new ExecutiontimeWork(false)));
    results = futures.get();
    Set<Long> resultAsSet = new HashSet<Long>(results);
    assertSame(results.size(), resultAsSet.size());
    
  }
  
  
  @Test
  public void testReschedule() throws InterruptedException, ExecutionException {
    final MergingWorkProcessor<Integer, Void, Long> processor = createTestMWP();
    
    final long FUZZY_EXECUTION_TIME_OFFSET = 20;
    
    long starttime = System.currentTimeMillis();
    Future<Long> future = processor.submit(new ExecutiontimeWork(false));
    long duration = future.get() - starttime;
    assertTrue(duration >= TEST_MERGE_COLLECTION_DELAY_MILLIS);
    assertTrue(duration <= TEST_MERGE_COLLECTION_DELAY_MILLIS + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    
    final long SLEEP_BEFORE_RESCHEDULE = 100;
    
    final ExecutiontimeWork workToReschedule = new ExecutiontimeWork(false);
    starttime = System.currentTimeMillis();
    future = processor.submit(workToReschedule);
    Thread.sleep(SLEEP_BEFORE_RESCHEDULE);
    Collection<Future<Long>> results = processor.rescheduleNow(workToReschedule.getKey());
    duration = future.get() - starttime;
    assertTrue(duration >= SLEEP_BEFORE_RESCHEDULE);
    assertTrue(duration <= SLEEP_BEFORE_RESCHEDULE + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(1, results.size());
    assertEquals(future.get(), results.iterator().next().get());
    
    
    final AtomicLong rescheduleTime = new AtomicLong();
    starttime = System.currentTimeMillis();
    future = processor.submit(workToReschedule);
    new Thread(new Runnable() {
      
      public void run() {
        try {
          Thread.sleep(SLEEP_BEFORE_RESCHEDULE);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        rescheduleTime.set(System.currentTimeMillis());
        processor.rescheduleNow(workToReschedule.getKey());
      }
    }).start();
    duration = future.get() - starttime;
    assertTrue(duration >= SLEEP_BEFORE_RESCHEDULE);
    assertTrue(duration <= SLEEP_BEFORE_RESCHEDULE + FUZZY_EXECUTION_TIME_OFFSET);
    assertTrue(future.get() >= rescheduleTime.get());
    assertTrue(future.get() <= rescheduleTime.get() + FUZZY_EXECUTION_TIME_OFFSET);
    
    final long TO_MUCH_SLEEP_BEFORE_RESCHEDULE = TEST_MERGE_COLLECTION_DELAY_MILLIS /*- TEST_RESCHEDULE_CANCELLATION_THRESHHOLD_MILLIS + 1*/;
    starttime = System.currentTimeMillis();
    future = processor.submit(workToReschedule);
    Thread.sleep(TO_MUCH_SLEEP_BEFORE_RESCHEDULE);
    results = processor.rescheduleNow(workToReschedule.getKey());
    duration = future.get() - starttime;
    assertTrue(duration >= TEST_MERGE_COLLECTION_DELAY_MILLIS);
    assertTrue(duration <= TEST_MERGE_COLLECTION_DELAY_MILLIS + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(1, results.size());
    assertEquals(future.get(), results.iterator().next().get());
  }
  
  
  
  @Test
  public void testSharedData() throws Exception {
    final long FUZZY_EXECUTION_TIME_OFFSET = 20;
    
    MergingWorkProcessor<Integer, SharedData, Long> processor = createTestMWP();
    
    SharedData data = new SharedData();

    FutureCollection<Long> futures = new FutureCollection<Long>();
    
    pauseExecutorAlgorithm(processor);
    try {
      futures.add(processor.submit(new CountMergesWork(data)));
      futures.add(processor.submit(new CountMergesWork(data)));
      futures.add(processor.submit(new CountMergesWork(data)));
      futures.add(processor.submit(new CountMergesWork(data)));
      Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS + FUZZY_EXECUTION_TIME_OFFSET);
    } finally {
      unpauseExecutorAlgorithm(processor);
    }
    List<Long> results = futures.get();
    for (Long res : results) {
      assertEquals(new Long(4), res);
    }
    assertEquals(1, data.uses.get());
      
    futures = new FutureCollection<Long>();
    pauseExecutorAlgorithm(processor);
    try {
      futures.add(processor.submit(new CountMergesWork(1, false, data)));
      futures.add(processor.submit(new CountMergesWork(2, false, data)));
      futures.add(processor.submit(new CountMergesWork(3, false, data)));
      futures.add(processor.submit(new CountMergesWork(4, false, data)));
      Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS + FUZZY_EXECUTION_TIME_OFFSET);
    } finally {
      unpauseExecutorAlgorithm(processor);
    }
    
    results = futures.get();
    for (Long res : results) {
      assertEquals(new Long(1), res);
    }
    assertEquals(4, data.uses.get());
    
  }
  
  
  // kann sporadisch scheitern da offset und park-Zeiten nur empirisch ermittelt wurden
  @Test
  public void testContinousSubmits() throws Exception {
    final long SUBMITS = 10000;
    
    MergingWorkProcessor<Integer, SharedData, Long> processor = createTestMWP();
    
    SharedData data = new SharedData();

    FutureCollection<Long> futures = new FutureCollection<Long>();
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - 20);
    for (int i = 0; i < SUBMITS; i++) {
      futures.add(processor.submit(new CountMergesWork(data)));
      LockSupport.parkNanos(11500);
    }
    
    Map<Long, Integer> batchMap = new HashMap<Long, Integer>();
    for (int i = 0; i < SUBMITS; i++) {
      Long value = futures.get().get(i);
      if (batchMap.containsKey(value)) {
        batchMap.put(value, batchMap.get(value) + 1);
      } else {
        batchMap.put(value, 1);
      }
    }
    assertEquals(SUBMITS, futures.get().size());
    Set<Long> keySet = batchMap.keySet();
    assertEquals(2, keySet.size());
    long mergeCount = 0;
    for (Long keyValue : keySet) {
      mergeCount += keyValue;
    }
    assertEquals(SUBMITS, mergeCount);
    
    futures = new FutureCollection<Long>();
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - 10);
    for (int i = 0; i < SUBMITS; i++) {
      futures.add(processor.submit(new CountMergesWork(data)));
      LockSupport.parkNanos(4500);
    }
    
    batchMap = new HashMap<Long, Integer>();
    for (int i = 0; i < SUBMITS; i++) {
      Long value = futures.get().get(i);
      if (batchMap.containsKey(value)) {
        batchMap.put(value, batchMap.get(value) + 1);
      } else {
        batchMap.put(value, 1);
      }
    }
    assertEquals(SUBMITS, futures.get().size());
    keySet = batchMap.keySet();
    assertEquals(2, keySet.size());
    mergeCount = 0;
    for (Long keyValue : keySet) {
      mergeCount += keyValue;
    }
    assertEquals(SUBMITS, mergeCount);
    
    
    
    futures = new FutureCollection<Long>();
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - 5);
    for (int i = 0; i < SUBMITS; i++) {
      futures.add(processor.submit(new CountMergesWork(data)));
      LockSupport.parkNanos(2800);
    }
    
    batchMap = new HashMap<Long, Integer>();
    for (int i = 0; i < SUBMITS; i++) {
      Long value = futures.get().get(i);
      if (batchMap.containsKey(value)) {
        batchMap.put(value, batchMap.get(value) + 1);
      } else {
        batchMap.put(value, 1);
      }
    }
    assertEquals(SUBMITS, futures.get().size());
    keySet = batchMap.keySet();
    assertEquals(2, keySet.size());
    mergeCount = 0;
    for (Long keyValue : keySet) {
      mergeCount += keyValue;
    }
    assertEquals(SUBMITS, mergeCount);
    
    
    futures = new FutureCollection<Long>();
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - 3);
    for (int i = 0; i < SUBMITS; i++) {
      futures.add(processor.submit(new CountMergesWork(data)));
      LockSupport.parkNanos(2100);
    }
    
    batchMap = new HashMap<Long, Integer>();
    for (int i = 0; i < SUBMITS; i++) {
      Long value = futures.get().get(i);
      if (batchMap.containsKey(value)) {
        batchMap.put(value, batchMap.get(value) + 1);
      } else {
        batchMap.put(value, 1);
      }
    }
    assertEquals(SUBMITS, futures.get().size());
    keySet = batchMap.keySet();
    assertEquals(2, keySet.size());
    mergeCount = 0;
    for (Long keyValue : keySet) {
      mergeCount += keyValue;
    }
    assertEquals(SUBMITS, mergeCount);
  }
  
  
  @Test
  public void testCancelDuringDifferentPhases() throws Exception {
    final long FUZZY_EXECUTION_TIME_OFFSET = 20;
    
    MergingWorkProcessor<String, Void, Long> processor = createTestMWP();

    final Long SLEEP_TIME = 3000L;
    AtomicBoolean finishedFlag = new AtomicBoolean(false);
    SleepMergingWork work = new SleepMergingWork("baum", SLEEP_TIME, finishedFlag);
    
    // normal execution without cancel
    finishedFlag.set(false);
    Future<Long> future = processor.submit(work);
    assertEquals(true, processor.hasWork(work.getKey()));
    assertEquals(false, finishedFlag.get());
    Thread.sleep(SLEEP_TIME + TEST_MERGE_COLLECTION_DELAY_MILLIS - FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    Thread.sleep(FUZZY_EXECUTION_TIME_OFFSET + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(true, finishedFlag.get());
    assertEquals(SLEEP_TIME, future.get());
    assertEquals(false, processor.hasWork(work.getKey()));
    
    
    // cancel finished
    assertFalse(future.cancel(false));
    assertEquals(SLEEP_TIME, future.get());
    assertEquals(false, processor.hasWork(work.getKey()));
    assertFalse(future.cancel(true));
    assertEquals(SLEEP_TIME, future.get());
    assertEquals(false, processor.hasWork(work.getKey()));
    
    // cancel during waiting
    finishedFlag.set(false);
    future = processor.submit(work);
    assertEquals(true, processor.hasWork(work.getKey()));
    assertEquals(false, finishedFlag.get());
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    assertTrue(future.cancel(false));
    Thread.sleep(SLEEP_TIME + FUZZY_EXECUTION_TIME_OFFSET + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    try {
      future.get();
      fail();
    } catch (CancellationException e) {
      // expected
    }
    assertEquals(false, processor.hasWork(work.getKey()));
    
    // cancel with interrupt during waiting
    finishedFlag.set(false);
    future = processor.submit(work);
    assertEquals(true, processor.hasWork(work.getKey()));
    assertEquals(false, finishedFlag.get());
    Thread.sleep(TEST_MERGE_COLLECTION_DELAY_MILLIS - FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    assertTrue(future.cancel(true));
    Thread.sleep(SLEEP_TIME + FUZZY_EXECUTION_TIME_OFFSET + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    try {
      future.get();
      fail();
    } catch (CancellationException e) {
      // expected
    }
    assertEquals(false, processor.hasWork(work.getKey()));
    
    // cancel during execution
    finishedFlag.set(false);
    future = processor.submit(work);
    assertEquals(true, processor.hasWork(work.getKey()));
    assertEquals(false, finishedFlag.get());
    Thread.sleep(SLEEP_TIME + TEST_MERGE_COLLECTION_DELAY_MILLIS - FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    assertFalse(future.cancel(false));
    Thread.sleep(FUZZY_EXECUTION_TIME_OFFSET + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(true, finishedFlag.get());
    try {
      future.get();
    } catch (CancellationException e) {
      // unexpected without mayInterruptRunning = false
      fail();
    }
    assertEquals(false, processor.hasWork(work.getKey()));
    
    // cancel with interrupt during execution
    finishedFlag.set(false);
    future = processor.submit(work);
    assertEquals(true, processor.hasWork(work.getKey()));
    assertEquals(false, finishedFlag.get());
    Thread.sleep(SLEEP_TIME + TEST_MERGE_COLLECTION_DELAY_MILLIS - FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(false, finishedFlag.get());
    assertTrue(future.cancel(true));
    Thread.sleep(FUZZY_EXECUTION_TIME_OFFSET + TEST_EXECUTOR_INTERVAL + FUZZY_EXECUTION_TIME_OFFSET);
    assertEquals(true, finishedFlag.get());
    try {
      future.get();
      fail();
    } catch (CancellationException e) {
      // expected
    }
    assertEquals(false, processor.hasWork(work.getKey()));
    
  }
  
  // TODO test cancel of rescheduled
  // TODO test execution exceptions (including during sharedData init & finalize)
  
  
  private static void pauseExecutorAlgorithm(MergingWorkProcessor processor) throws Exception {
    Field field = processor.getClass().getDeclaredField("executor");
    field.setAccessible(true);
    LazyAlgorithmExecutor executor = (LazyAlgorithmExecutor) field.get(processor);
    executor.stopThread();
    Thread.sleep(100);
  }
  
  
  private static void unpauseExecutorAlgorithm(MergingWorkProcessor processor) throws Exception {
    Field field = processor.getClass().getDeclaredField("executor");
    field.setAccessible(true);
    LazyAlgorithmExecutor executor = (LazyAlgorithmExecutor) field.get(processor);
    Class[] innerClasses = MergingWorkProcessor.class.getDeclaredClasses();
    Algorithm alg= null;
    for (Class clazz : innerClasses) {
      if (Algorithm.class.isAssignableFrom(clazz)) {
        Constructor constructor = clazz.getDeclaredConstructor(MergingWorkProcessor.class);
        constructor.setAccessible(true);
        alg = Algorithm.class.cast(constructor.newInstance(processor)) ;
      }
    }
    executor.startNewThread(alg);
    executor.requestExecution();
  }
  
  
  static class CountMergesWork implements MergableWork<Integer, SharedData, Long> {

    private AtomicLong value = new AtomicLong(1);
    private boolean mergable;
    private SharedData data;
    private Integer key;
    
    CountMergesWork() {
      this(new SharedData());
    }
    
    CountMergesWork(SharedData data) {
      this(true, data);
    }
    
    CountMergesWork(boolean mergable) {
      this(mergable, new SharedData());
    }
    
    CountMergesWork(boolean mergable, SharedData data) {
      this(1, mergable, data);
    }
    
    
    CountMergesWork(Integer key, boolean mergable, SharedData data) {
      this.mergable = mergable;
      this.data = data;
      this.key = key;
    }

    public Long process(SharedData sharedProcessingData) {
      sharedProcessingData.uses.incrementAndGet();
      return value.get();
    }

    public boolean merge(MergableWork<Integer, SharedData, Long> other) {
      value.incrementAndGet();
      return true;
    }
    

    public boolean isMergeable() {
      return mergable;
    }

    public Integer getKey() {
      return key;
    }

    public SharedData initSharedProcessingData(Iterator<MergableWork<Integer, SharedData, Long>> workloadIter) {
      data.uses.set(0);
      return data;
    }

    public void finalizeSharedProcessingData(SharedData sharedData, Iterator<MergableWork<Integer, SharedData, Long>> workloadIter) throws Exception {
    }
    
  }
  
  
  static class ExecutiontimeWork extends MergableWork.UnsharedMergableWork<Integer, Long> {

    private boolean mergable;
    
    ExecutiontimeWork(boolean mergable) {
      this.mergable = mergable;
    }


    public boolean merge(MergableWork<Integer, Void, Long> other) {
      return true;
    }
    
    public boolean isMergeable() {
      return mergable;
    }

    public Integer getKey() {
      return 1;
    }

    public Long process() throws Exception {
      return System.currentTimeMillis();
    }

  }
  
  
  
  private static class SleepMergingWork extends MergableWork.UnsharedMergableWork<String, Long> {
    
    String key;
    long sleepTime;
    AtomicBoolean finished;
    
    public SleepMergingWork(String key, long sleepTime, AtomicBoolean finished) {
      this.key = key;
      this.sleepTime = sleepTime;
      this.finished = finished;
    }

    public String getKey() {
      return key;
    }

    public boolean merge(MergableWork<String, Void, Long> other) {
      SleepMergingWork otherWork = (SleepMergingWork) other;
      this.sleepTime += otherWork.sleepTime;
      return true;
    }

    public boolean isMergeable() {
      return true;
    }

    @Override
    public Long process() throws Exception {
      finished.set(false);
      Thread.sleep(sleepTime);
      finished.set(true);
      return sleepTime;
    }
    
  }
  
  
  
  
  static class SharedData {
    
    private AtomicInteger uses;
    
    public SharedData() {
      this.uses = new AtomicInteger(0);
    }
    
  }
  
  
}
