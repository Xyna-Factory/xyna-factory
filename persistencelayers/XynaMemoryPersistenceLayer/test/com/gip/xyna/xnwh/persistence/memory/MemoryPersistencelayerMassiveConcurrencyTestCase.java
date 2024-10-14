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

package com.gip.xyna.xnwh.persistence.memory;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;


public abstract class MemoryPersistencelayerMassiveConcurrencyTestCase extends TestCase {

  private ThreadPoolExecutor threadpool = 
    new ThreadPoolExecutor(1, 5, 20, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());


  public abstract PersistenceLayer getPersistenceLayer();


  public abstract int getCountForConcurrentInsertAndDeleteAndSelect();
  public abstract int getCountForConcurrentQueryForUpdateGEQ();
  public abstract int getCountForConcurrentQueryForUpdateEQ();


  public static abstract class TestRunnable implements Runnable {

    private PersistenceLayerConnection connection;
    private Queue<TestRunnable> finishedRunnables;

    private Throwable failed = null;

    public TestRunnable(PersistenceLayerConnection connection, Queue<TestRunnable> finishedRunnables) {
      this.connection = connection;
      this.finishedRunnables = finishedRunnables;
    }

    public PersistenceLayerConnection getConnection() {
      return connection;
    }

    public Throwable isFailed() {
      return failed;
    }

    public void failed(Throwable t) {
      this.failed = t;
    }

    public void run() {
      try {
        executeInternally();
      } finally {
        finishedRunnables.add(this);
      }
    }

    protected abstract void executeInternally();

  }

  
  public static class PersistObjectRunnable extends TestRunnable {

    private long id;
    private long trivialId;


    public PersistObjectRunnable(long id, long trivialId, PersistenceLayerConnection connection,
                                 Queue<TestRunnable> finishedRunnables) {
      super(connection, finishedRunnables);
      this.id = id;
      this.trivialId = trivialId;
    }

    public void executeInternally() {
      try {
        getConnection().persistObject(new TestStorable(id, trivialId));
        getConnection().commit();
        getConnection().closeConnection();
      } catch (PersistenceLayerException e) {
        failed(e);
      }
    }
    
  }


  public static class DeleteRunnable extends TestRunnable {

    private long id;

    public DeleteRunnable(long id, PersistenceLayerConnection connection, Queue<TestRunnable> finishedRunnables) {
      super(connection, finishedRunnables);
      this.id = id;
    }

    @Override
    protected void executeInternally() {
      try {
        getConnection().deleteOneRow(new TestStorable(id));
        getConnection().commit();
        getConnection().closeConnection();
      } catch (PersistenceLayerException e) {
        failed(e);
      }
    }
    
  }


  public static class ForUpdateRunnable extends TestRunnable {

    private long id;

    public ForUpdateRunnable(long id, PersistenceLayerConnection connection, Queue<TestRunnable> finishedRunnables) {
      super(connection, finishedRunnables);
      this.id = id;
    }

    public void executeInternally() {
      try {
        getConnection().queryOneRowForUpdate(new TestStorable(id));
        getConnection().commit();
        getConnection().closeConnection();
      } catch (PersistenceLayerException e) {
        failed(e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        try {
          getConnection().closeConnection();
        } catch (PersistenceLayerException e1) {
          failed(e);
        }
      }
    }

  }


  public void atest_01_concurrentInserts() throws PersistenceLayerException, InterruptedException {

    Random random = new Random();

    ConcurrentLinkedQueue<TestRunnable> runnables =
        new ConcurrentLinkedQueue<MemoryPersistencelayerMassiveConcurrencyTestCase.TestRunnable>();

    final int numberOfRunnables = getCountForConcurrentInsertAndDeleteAndSelect();

    for (int i = 0; i < numberOfRunnables; i++) {
      PersistenceLayerConnection nextConnection = getPersistenceLayer().getConnection();
      TestRunnable r;
      int nextInt = random.nextInt(10);
      if (nextInt < 3) {
        r = new PersistObjectRunnable(random.nextInt(100), random.nextLong(), nextConnection, runnables);
      } else if (nextInt < 5) {
        r = new DeleteRunnable(random.nextInt(100), nextConnection, runnables);
      } else {
        r = new ForUpdateRunnable(random.nextInt(100), nextConnection, runnables);
      }
      boolean executed = false;
      while (!executed) {
        try {
          threadpool.execute(r);
          executed = true;
        } catch (RejectedExecutionException e) {
          Thread.sleep(10);
        }
      }
    }

    int counter = 0;
    while (runnables.size() < numberOfRunnables) {
      Thread.sleep(100);
      counter++;
      if (counter > numberOfRunnables / 100 && counter % (numberOfRunnables/100) == 0) {
        for (TestRunnable tr : runnables) {
          Throwable nextThrowable = tr.isFailed();
          if (nextThrowable != null) {
            nextThrowable.printStackTrace();
            fail(nextThrowable.getMessage());
          }
        }
      }
      if (counter > numberOfRunnables / 10) {
        fail("timeout?");
      }
    }

    boolean failed = false;
    for (TestRunnable tr : runnables) {
      Throwable nextThrowable = tr.isFailed();
      if (nextThrowable != null) {
        nextThrowable.printStackTrace();
        failed = true;
      }
    }
    if (failed) {
      fail();
    }

  }


  public void test_02_concurrentQueries() throws PersistenceLayerException, InterruptedException {
    queryTest(getCountForConcurrentQueryForUpdateGEQ(), "select * from " + TestStorable.TABLE_NAME + " where "
        + TestStorable.COL_TRIVIAL_ID + " > ? for update");
  }


  private void queryTest(final int count, String queryString) throws PersistenceLayerException, InterruptedException {

    PersistenceLayerConnection initConnection = getPersistenceLayer().getConnection();

    final Random rand = new Random();
    for (int k = 0; k < count / 10; k++) {
      for (int i = 0; i < 10; i++) {
        long num = k* 10 + i;
        initConnection.persistObject(new TestStorable(num, num));
      }
      initConnection.commit();
    }

    final AtomicBoolean failed = new AtomicBoolean();
    final AtomicInteger finishedCounter = new AtomicInteger();

    final PreparedQuery<TestStorable> pq =
        initConnection.prepareQuery(new Query<TestStorable>(queryString, TestStorable.reader));
    initConnection.closeConnection();

    long before = System.currentTimeMillis();
    for (int i = 0; i < count; i++) {
      final PersistenceLayerConnection nextConnection = getPersistenceLayer().getConnection();
      Runnable r = new Runnable() {

        public void run() {
          int trivialIdCap;
          synchronized (rand) {
            trivialIdCap = rand.nextInt((int) count);
          }
          if (trivialIdCap >= count -2) {
            trivialIdCap--;
          }
          Parameter param = new Parameter((long) trivialIdCap);
          try {
            if (nextConnection.query(pq, param, -1).size() == 0) {
              fail("query is empty");
            }
            nextConnection.commit();
            nextConnection.closeConnection();
            finishedCounter.getAndIncrement();
          } catch (PersistenceLayerException e) {
            e.printStackTrace();
            failed.set(true);
          } catch (Throwable t) {
            CentralFactoryLogging.getLogger(MemoryPersistencelayerMassiveConcurrencyTestCase.class).error(null, t);
            t.printStackTrace();
            failed.set(true);
          }
        }
      };
      boolean executed = false;
      do {
        try {
          threadpool.execute(r);
          executed = true;
        } catch (RejectedExecutionException e) {
          Thread.sleep(10);
        }
      } while (!executed);
      if (failed.get()) {
        fail("Failed");
      }
    }

    int waitCount = 0;
    while (!failed.get() && finishedCounter.get() < count) {
      Thread.sleep(100);
      waitCount++;
      if (waitCount % 10 == 0) {
        System.out.println(finishedCounter.get());
      }
    }

    if (failed.get()) {
      fail("Failed");
    }

    long duration = System.currentTimeMillis() - before;
    double durationInSeconds = (double) duration / 1000;
    double rate = count / durationInSeconds;
    double averageSearchTime = ((double) duration / count);
    System.out.println("took " + durationInSeconds + " seconds for " + count + " search queries => "
        + rate + "Hz, average search time " + averageSearchTime + "ms");

  }


  public static void main(String[] args) throws InterruptedException {
//    long before = System.currentTimeMillis();
//    Thread.sleep(50);
//    long duration = System.currentTimeMillis() - before;
//    double durationInSeconds = (double) duration / 1000;
//    System.out.println(durationInSeconds);
    System.out.println(1000 % 100);
  }


  public void test_03_testPerformance() throws PersistenceLayerException, InterruptedException {

    queryTest(getCountForConcurrentQueryForUpdateEQ(), "select * from " + TestStorable.TABLE_NAME + " where "
        + TestStorable.COL_TRIVIAL_ID + " = ? for update");

  }

}
