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

package com.gip.xyna.coherence;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import junit.framework.TestCase;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.debugging.Debugger;



public class CacheController_Functional_Load_OnlineTest extends TestCase {


  private static final long TEST_01_MAX_COUNT = 10000;
  private static final long TEST_02_MAX_COUNT = 10000;
  private static final long TEST_03_MAX_COUNT = 10000;
  private static final long TEST_04_MAX_COUNT = 10000;
  private static final long TEST_05_MAX_COUNT = 10000;

  /**
   * Maximal number of requests send in test_06 
   */
  private static final long TEST_06_MAX_COUNT = 10000000;
  private static final long TEST_07_MAX_COUNT = 10000;

  public static void main(String[] args) throws InterruptedException, IOException {
    new CacheController_Functional_Load_OnlineTest().test_06_ConcurrentReadsAndDeletes();
  }


  public void atest_01_CreateInconsistencies() throws InterruptedException, IOException {

    for (int r = 0; r < 1; r++) {

      long t1 = System.currentTimeMillis();
      long diffTime = 0;
      final int controllerCount = 4;
      final CacheController[] controllers = new CacheController[controllerCount];
      try {
        //-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y

        final Random random = new Random();

        final CacheController ccID0 = CacheControllerFactory.newCacheController();
        ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        ccID0.setupNewCluster(); //neues cluster

        controllers[0] = ccID0;
        for (int i = 1; i < controllerCount; i++) {
          controllers[i] = CacheControllerFactory.newCacheController();
          controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
          controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
              .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
        }

        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
        final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
        final AtomicInteger busyCounter = new AtomicInteger();
        t1 = System.currentTimeMillis();

        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long count = totalCounter.get();
              while (!atleastOneThreadFailed.get() && count < TEST_01_MAX_COUNT) {
                try {
                  if (random.nextLong() % 1024 * 64 == 0) {
                    Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                  }
                  if (count % 10000 == 0) {
                    System.out.println(count);
                  }
                  busyCounter.set(0);
                  CacheController controller = controllers[random.nextInt(controllers.length)];
                  controller.create(new CoherencePayload());
                } catch (Throwable t) {
                  t.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(t);
                }
                count = totalCounter.incrementAndGet();
              }
            }
          });
          threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
          threads[i].setDaemon(true);
          threads[i].start();
        }

        while (true) {
          if (atleastOneThreadFailed.get()) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail("One thread failed after " + totalCounter.get() + " events");
          }
          if (totalCounter.get() >= TEST_01_MAX_COUNT) {
            break;
          }
          Thread.sleep(100);
          busyCounter.getAndIncrement();
          if (busyCounter.get() > 50) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
                + " events have finished");
          }
        }

        Thread.sleep(100);

        int count = 0;
        while (true) {
          if (currentlyWaitingForLock.get() == 0) {
            break;
          }
          if (count++ > 100) {
            fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
          }
          Thread.sleep(50);
        }
        diffTime = (System.currentTimeMillis() - t1 - 100);
      } finally {
        System.out.println("finished " + diffTime);
        FileOutputStream fos = new FileOutputStream(new File("log_0.txt"));
        try {
          Debugger.getDebugger().writeCSVToStream(fos);
        } finally {
          fos.close();
        }
        for (CacheController controller : controllers) {
          if (controller != null) {
            controller.shutdown();
          }
        }
      }

    }

  }


  public void atest_02_LockAndUnlockDeadlocks() throws InterruptedException, IOException {
    long t1 = System.currentTimeMillis();
    long diffTime = 0;
    final int controllerCount = 2;
    final CacheController[] controllers = new CacheController[controllerCount];
    try {
      //-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y

      final Random random = new Random();

      final CacheController ccID0 = CacheControllerFactory.newCacheController();
      ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
      ccID0.setupNewCluster(); //neues cluster

      controllers[0] = ccID0;
      for (int i = 1; i < controllerCount; i++) {
        controllers[i] = CacheControllerFactory.newCacheController();
        controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
            .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
      }


      final CoherencePayload payload = new CoherencePayload();
      final long objectId = ccID0.create(payload);

      final AtomicLong totalCounter = new AtomicLong();
      final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
      final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
      final AtomicInteger busyCounter = new AtomicInteger();
      final AtomicInteger threadsWithinLockCounter = new AtomicInteger();
      t1 = System.currentTimeMillis();

      Thread[] threads = new Thread[4];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new Runnable() {

          public void run() {
            long count = totalCounter.get();
            while (!atleastOneThreadFailed.get() && count < TEST_02_MAX_COUNT) {
              try {
                if (random.nextLong() % 1024 * 64 == 0) {
                  Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                }
                if (count % 10000 == 0) {
                  System.out.println(count);
                }
                busyCounter.set(0);
                CacheController controller = controllers[random.nextInt(controllers.length)];
                currentlyWaitingForLock.incrementAndGet();
                controller.lock(objectId);
                if (threadsWithinLockCounter.incrementAndGet() > 1) {
                  throw new RuntimeException("More than one thread within locked code area");
                }
                threadsWithinLockCounter.decrementAndGet();
                controller.unlock(objectId);
                currentlyWaitingForLock.getAndDecrement();
              } catch (ObjectNotInCacheException e) {
                e.printStackTrace();
                atleastOneThreadFailed.set(true);
                throw new RuntimeException(e);
              } catch (Throwable t) {
                t.printStackTrace();
                atleastOneThreadFailed.set(true);
                throw new RuntimeException(t);
              }
              count = totalCounter.incrementAndGet();
            }
          }
        });
        threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
        threads[i].setDaemon(true);
        threads[i].start();
      }

      while (true) {
        if (atleastOneThreadFailed.get()) {
          for (int i = 0; i < threads.length; i++) {
            Exception e = new Exception();
            e.setStackTrace(threads[i].getStackTrace());
            e.printStackTrace();
          }
          fail("One thread failed after " + totalCounter.get() + " events");
        }
        if (totalCounter.get() >= TEST_02_MAX_COUNT) {
          break;
        }
        Thread.sleep(100);
        busyCounter.getAndIncrement();
        if (busyCounter.get() > 50) {
          for (int i = 0; i < threads.length; i++) {
            Exception e = new Exception();
            e.setStackTrace(threads[i].getStackTrace());
            e.printStackTrace();
          }
          fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
              + " events have finished");
        }
      }

      Thread.sleep(100);

      int count = 0;
      while (true) {
        if (currentlyWaitingForLock.get() == 0) {
          break;
        }
        if (count++ > 100) {
          fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
        }
        Thread.sleep(50);
      }
      diffTime = (System.currentTimeMillis() - t1 - 100);
    } finally {
      for (CacheController controller : controllers) {
        if (controller != null) {
          controller.shutdown();
        }
      }
      System.out.println("finished " + diffTime);
      FileOutputStream fos = new FileOutputStream(new File("log.txt"));
      try {
        Debugger.getDebugger().writeCSVToStream(fos);
      } finally {
        fos.close();
      }
    }

  }


  public void atest_03_AddNodesDuringRuntime() throws Exception {
    try {
      final Random random = new Random();
      for (int k = 0; k < 1; k++) {

        final CacheController ccID0 = CacheControllerFactory.newCacheController();
        ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        ccID0.setupNewCluster(); //neues cluster

        final List<CacheController> controllers = new Vector<CacheController>();
        controllers.add(ccID0);

        final CoherencePayload payload = new CoherencePayload();
        final long objectId = ccID0.create(payload);

        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
        final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
        final AtomicInteger busyCounter = new AtomicInteger();
        final AtomicInteger threadsWithinLockCounter = new AtomicInteger();

        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long counter = totalCounter.get();
              while (!atleastOneThreadFailed.get() && counter < TEST_03_MAX_COUNT) {
                try {
                  if (random.nextLong() % 1024 * 64 == 0) {
                    Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                  }
                  if (counter % 1000 == 0) {
                    System.out.println(counter);
                  }
                  busyCounter.set(0);
                  CacheController controller = controllers.get(random.nextInt(controllers.size()));
                  currentlyWaitingForLock.incrementAndGet();
                  controller.lock(objectId);
                  if (threadsWithinLockCounter.incrementAndGet() > 1) {
                    throw new RuntimeException("More than one thread within locked code area");
                  }
                  threadsWithinLockCounter.decrementAndGet();
                  controller.unlock(objectId);
                  currentlyWaitingForLock.getAndDecrement();
                  if (random.nextInt(15) == 0 && controllers.size() < 7 && counter > 80) {
                    CacheController newController = CacheControllerFactory.newCacheController();
                    newController.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
                    newController.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
                        .getJavaProvider((InterconnectCalleeJava) ccID0.getCallees().get(0)));

                    controllers.add(newController);
                    System.out.println(controllers.size() + " controllers");
                  }
                  counter = totalCounter.incrementAndGet();
                } catch (ObjectNotInCacheException e) {
                  e.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(e);
                } catch (Throwable t) {
                  t.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(t);
                }
              }
            }
          });
          threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
          threads[i].start();
        }

        while (true) {
          if (atleastOneThreadFailed.get()) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail("One thread failed after " + totalCounter.get() + " events");
          }
          if (totalCounter.get() >= TEST_03_MAX_COUNT) {
            break;
          }
          Thread.sleep(500);
          busyCounter.getAndIncrement();
          if (busyCounter.get() > 20) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
                + " events have finished");
          }
        }

        Thread.sleep(100);

        int count = 0;
        while (true) {
          if (currentlyWaitingForLock.get() == 0) {
            break;
          }
          if (count++ > 100) {
            fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
          }
          Thread.sleep(50);
        }
        
        for (CacheController controller : controllers) {
          controller.shutdown();
        }
      }
    } finally {
      System.out.println("finished");
      FileOutputStream fos = new FileOutputStream(new File("log2.txt"));
      try {
        Debugger.getDebugger().writeCSVToStream(fos);
      } finally {
        fos.close();
      }
    }
  }


  public void atest_04_AddAndRemoveNodesDuringRuntime() throws InterruptedException, IOException {
    try {
      final Random random = new Random();
      for (int k = 0; k < 1; k++) {

        final CacheController ccID0 = CacheControllerFactory.newCacheController();
        ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        ccID0.setupNewCluster(); //neues cluster

        final List<CacheController> controllers = new Vector<CacheController>();
        controllers.add(ccID0);

        final CoherencePayload payload = new CoherencePayload();
        final long objectId = ccID0.create(payload);

        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
        final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
        final AtomicInteger busyCounter = new AtomicInteger();
        final AtomicInteger threadsWithinLockCounter = new AtomicInteger();

        final ReentrantReadWriteLock l = new ReentrantReadWriteLock();
        
        Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long count = totalCounter.get();
              while (!atleastOneThreadFailed.get() && count < TEST_04_MAX_COUNT) {

                try {
                  if (random.nextLong() % 1024 * 64 == 0) {
                    Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                  }
                  if (count % 1000 == 0) {
                    System.out.println(count);
                  }
                  busyCounter.set(0);

                  CacheController controller = null;
                  while (controller == null) {
                    try {
                      controller = controllers.get(random.nextInt(controllers.size()));
                    } catch (ArrayIndexOutOfBoundsException e) {
                      // controllers size kann inkorrekt sein, wenn sie verwendet wird
                    }
                  }

                  int type = random.nextInt(4);
                  switch (type) {
                    case 0 :
                      l.readLock().lock();
                      try {
                        currentlyWaitingForLock.incrementAndGet();
                        try {
                          controller.lock(objectId);
                          if (threadsWithinLockCounter.incrementAndGet() > 1) {
                            throw new RuntimeException("More than one thread within locked code area");
                          }
                          threadsWithinLockCounter.decrementAndGet();
                          controller.unlock(objectId);
                        } catch (IllegalStateException e) {
                          //knoten wurde inzwischen heruntergefahren
                          System.out.println("node down?");
                        }
                        currentlyWaitingForLock.getAndDecrement();
                      } finally {
                        l.readLock().unlock();
                      }
                      break;
                    case 1 :
                      l.readLock().lock();
                      try {
                        if (controller.read(objectId) == null) {
                          fail("payload was null");
                        }
                      } catch (IllegalStateException e) {
                        //knoten wurde inzwischen heruntergefahren
                        System.out.println("node down?");
                      } finally {
                        l.readLock().unlock();
                      }
                      break;
                    case 2 :
                      l.readLock().lock();
                      try {
                        controller.update(objectId, new CoherencePayload());
                      } catch (IllegalStateException e) {
                        //knoten wurde inzwischen heruntergefahren
                        System.out.println("node down?");
                      } finally {
                        l.readLock().unlock();
                      }
                      break;
                    case 3 :
                      l.readLock().lock();
                      try {
                        controller.read(objectId);
                      } catch (IllegalStateException e) {
                        //knoten wurde inzwischen heruntergefahren
                        System.out.println("node down?");
                      } finally {
                        l.readLock().unlock();
                      }
                      break;
                    default :
                  }

                  l.readLock().lock();
                  try {
                    if (random.nextInt(15) == 0 && controllers.size() < 7 && count > 80) {
                      CacheController newController = CacheControllerFactory.newCacheController();
                      newController.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
                      int idx = random.nextInt(controllers.size());
                      try {
                        newController.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
                            .getJavaProvider((InterconnectCalleeJava) controllers.get(idx).getCallees().get(0)));
                        controllers.add(newController);
                        System.out.println(controllers.size() + " controllers");
                      } catch (IllegalStateException e) {
                        //knoten wurde inzwischen heruntergefahren
                        System.out.println("node down?");
                      }
                    }
                  } finally {
                    l.readLock().unlock();
                  }
                  if (random.nextInt(15) == 0) {
                    l.writeLock().lock();
                    try {
                      if (controllers.size() > 1) {
                        while (currentlyWaitingForLock.get() > 0) {
                          Thread.sleep(30);
                        }
                        CacheController c = null;
                        synchronized (payload) {
                          if (controllers.size() > 1) {
                            int idx = random.nextInt(controllers.size());
                            System.out.println("removing " + idx);
                            c = controllers.get(idx);
                            controllers.remove(idx);
                          }
                        }
                        if (c!= null) {
                          c.disconnectFromCluster();
                          c.shutdown();
                        }
                      }
                    } finally {
                      l.writeLock().unlock();
                    }
                  }
                } catch (ObjectNotInCacheException e) {
                  e.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(e);
                } catch (Throwable t) {
                  t.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(t);
                }

                count = totalCounter.incrementAndGet();

              }
            }
          });
          threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
          threads[i].start();
        }

        while (true) {
          if (atleastOneThreadFailed.get()) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail("One thread failed after " + totalCounter.get() + " events");
          }
          if (totalCounter.get() >= TEST_04_MAX_COUNT) {
            break;
          }
          Thread.sleep(500);
          busyCounter.getAndIncrement();
          if (busyCounter.get() > 20) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              System.out.println(threads[i].getName());
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
                + " events have finished");
          }
        }

        Thread.sleep(100);

        int count = 0;
        while (true) {
          if (currentlyWaitingForLock.get() == 0) {
            break;
          }
          if (count++ > 10) {
            fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
          }
          Thread.sleep(500);
        }
        
        for (CacheController controller : controllers) {
          controller.shutdown();
        }
      }
    } finally {
      System.out.println("finished");
      FileOutputStream fos = new FileOutputStream(new File("log2.txt"));
      try {
        Debugger.getDebugger().writeCSVToStream(fos);
      } finally {
        fos.close();
      }
    }
  }


  public void atest_05_ConcurrentReads() throws InterruptedException, IOException {

    long t1 = System.currentTimeMillis();
    long diffTime = 0;
    final int controllerCount = 2;
    final CacheController[] controllers = new CacheController[controllerCount];
    try {
      //-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y

      final Random random = new Random();

      final CacheController ccID0 = CacheControllerFactory.newCacheController();
      ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
      ccID0.setupNewCluster(); //neues cluster

      controllers[0] = ccID0;
      for (int i = 1; i < controllerCount; i++) {
        controllers[i] = CacheControllerFactory.newCacheController();
        controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
            .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
      }


      final CoherencePayload payload = new CoherencePayload();
      final long objectId = ccID0.create(payload);

      final AtomicLong totalCounter = new AtomicLong();
      final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
      final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
      final AtomicInteger busyCounter = new AtomicInteger();
      t1 = System.currentTimeMillis();

      Thread[] threads = new Thread[4];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(new Runnable() {

          public void run() {
            long count = totalCounter.get();
            while (!atleastOneThreadFailed.get() && count < TEST_05_MAX_COUNT) {
              try {
                if (random.nextLong() % 1024 * 64 == 0) {
                  Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                }
                if (count % 10000 == 0) {
                  System.out.println(count);
                }
                busyCounter.set(0);
                CacheController controller = controllers[random.nextInt(controllers.length)];
                currentlyWaitingForLock.incrementAndGet();
                if (random.nextInt(10) < 3) {
                  controller.update(objectId, new CoherencePayload());
                } else {
                  CoherencePayload notNull = controller.read(objectId);
                  if (notNull == null) {
                    throw new RuntimeException("may not be null");
                  }
                }
                currentlyWaitingForLock.getAndDecrement();
              } catch (ObjectNotInCacheException e) {
                e.printStackTrace();
                atleastOneThreadFailed.set(true);
                throw new RuntimeException(e);
              } catch (Throwable t) {
                t.printStackTrace();
                atleastOneThreadFailed.set(true);
                throw new RuntimeException(t);
              }
              count = totalCounter.incrementAndGet();
            }
          }
        });
        threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
        threads[i].setDaemon(true);
        threads[i].start();
      }

      while (true) {
        if (atleastOneThreadFailed.get()) {
          for (int i = 0; i < threads.length; i++) {
            Exception e = new Exception();
            e.setStackTrace(threads[i].getStackTrace());
            e.printStackTrace();
          }
          fail("One thread failed after " + totalCounter.get() + " events");
        }
        if (totalCounter.get() >= TEST_05_MAX_COUNT) {
          break;
        }
        Thread.sleep(500);
        busyCounter.getAndIncrement();
        if (busyCounter.get() > 10) {
          for (int i = 0; i < threads.length; i++) {
            Exception e = new Exception();
            e.setStackTrace(threads[i].getStackTrace());
            e.printStackTrace();
          }
          fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
              + " events have finished");
        }
      }

      Thread.sleep(100);

      int count = 0;
      while (true) {
        if (currentlyWaitingForLock.get() == 0) {
          break;
        }
        if (count++ > 100) {
          fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
        }
        Thread.sleep(50);
      }
      diffTime = (System.currentTimeMillis() - t1 - 100);
    } finally {
      for (CacheController controller : controllers) {
        if (controller != null) {
          controller.shutdown();
        }
      }
      System.out.println("finished " + diffTime);
      FileOutputStream fos = new FileOutputStream(new File("log.txt"));
      try {
        Debugger.getDebugger().writeCSVToStream(fos);
      } finally {
        fos.close();
      }
    }

  }


  /**
   * Create (controllerCount + 1) cluster nodes, create a number of threads which send TEST_06_MAX_COUNT requests to the nodes.
   * Request types are randomly generated.
   * @throws InterruptedException
   * @throws IOException
   */
  public void test_06_ConcurrentReadsAndDeletes() throws InterruptedException, IOException {

    // the number of iterations that the overall number is split into
    final int factor = 1;

    int iteration = 0;
    while (iteration < factor) {
      iteration++;
      Debugger.getDebugger().clear();

      final boolean waitForSlowThreads = false;

      long t1 = System.currentTimeMillis();
      long diffTime = 0;
      final int controllerCount = 3;
      final CacheController[] controllers = new CacheController[controllerCount];
      try {
        //-Xrunhprof:cpu=samples,format=a,file=hprof.txt,cutoff=0,depth=12,thread=y

        final Random random = new Random();

        //initialize cluster
        final CacheController ccID0 = CacheControllerFactory.newCacheController();
        ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        ccID0.setupNewCluster(); //neues cluster

        //create and connect cluster nodes 
        controllers[0] = ccID0;
        for (int i = 1; i < controllerCount; i++) {
          controllers[i] = CacheControllerFactory.newCacheController();
          controllers[i].addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
          controllers[i].connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
              .getJavaProvider((InterconnectCalleeJava) controllers[random.nextInt(i)].getCallees().get(0)));
        }


        final CoherencePayload payload = new CoherencePayload();
        final AtomicLong objectId = new AtomicLong(ccID0.create(payload));

        //total number of requests send by all threads
        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
        //number of thread which wait for lock
        final AtomicInteger currentlyWaitingForLock = new AtomicInteger();

        //threads which send requests to the cluster nodes
        final Thread[] threads = new Thread[2];
        //count for each thread the inactive iterations
        final AtomicInteger[] busyCounter = new AtomicInteger[threads.length];
        t1 = System.currentTimeMillis();

        final AtomicLong[] perThreadCounters = new AtomicLong[threads.length];
        final long[] currentlyWorkedObjectId = new long[threads.length];

        for (int i = 0; i < threads.length; i++) {
          busyCounter[i] = new AtomicInteger();
          perThreadCounters[i] = new AtomicLong();
        }

        //define requests send by thread (work done by thread)
        for (int i = 0; i < threads.length; i++) {
          final int copyOfI = i;
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long count = totalCounter.get();
              //while (no thread failed and still requests left)
              outer : while (!atleastOneThreadFailed.get() && count < TEST_06_MAX_COUNT/factor
                  && !atleastOneThreadFailed.get()) {
                try {
                  //set thread priority randomly
                  if (random.nextLong() % 1024 * 64 == 0) {
                    Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                  }
                  perThreadCounters[copyOfI].incrementAndGet();
                  if (waitForSlowThreads) {
                    boolean foundSlowThread = true;
                    while (foundSlowThread) {
                      foundSlowThread = false;
                      for (int k = 0; k < threads.length; k++) {
                        if (k != copyOfI) {
                          if (perThreadCounters[k].get() < perThreadCounters[copyOfI].get() - 10) {
                            foundSlowThread = true;
                            break;
                          }
                        }
                      }
                      if (foundSlowThread) {
                        Thread.sleep(10);
                      }
                    }
                  }
                  //reset busy counter
                  busyCounter[copyOfI].set(0);
                  //select a cluster node randomly
                  CacheController controller = controllers[random.nextInt(controllers.length)];
                  currentlyWaitingForLock.incrementAndGet();
                  try {
                    int rand = random.nextInt(12);
                    long targetId = objectId.get();
                    currentlyWorkedObjectId[copyOfI] = targetId;
                    
                    //random number of lock to get on the target object
                    final int numberOfLocks = random.nextInt(2);

                    //get all locks on target object
                    for (int i = 0; i < numberOfLocks; i++) {
                      try {
                        controller.lock(targetId);
                      } catch (ObjectNotInCacheException e) {
                        if (Debugger.getDebugger().isEnabled()) {
                          Debugger.getDebugger().debug("object <" + targetId + "> has already been deleted");
                        }
                        if (i == 0) {
                          continue outer;
                        } else {
                          throw e;
                        }
                      }
                    }
                    //execute update and release all locks on the object
                    if (rand < 3) {
                      try {
                        controller.update(targetId, new CoherencePayload());
                      } catch (ObjectNotInCacheException e) {
                        if (numberOfLocks > 0) {
                          throw e;
                        }
                        if (Debugger.getDebugger().isEnabled()) {
                          Debugger.getDebugger().debug("object <" + targetId + "> has already been deleted");
                        }
                        continue;
                      }
                      for (int i = 0; i < numberOfLocks; i++) {
                        controller.unlock(targetId);
                      }
                      //execute read and release all locks on the object
                    } else if (rand < 8) {
                      try {
                        CoherencePayload notNull = controller.read(targetId);
                        if (notNull == null) {
                          throw new ClusterInconsistentException("may not be null");
                        }
                      } catch (ObjectNotInCacheException e) {
                        if (numberOfLocks > 0) {
                          throw e;
                        }
                        if (Debugger.getDebugger().isEnabled()) {
                          Debugger.getDebugger().debug("object <" + targetId + "> has already been deleted");
                        }
                        continue;
                      }
                      for (int i = 0; i < numberOfLocks; i++) {
                        controller.unlock(targetId);
                      }
                      //execute delete, create new object and release all locks on the object
                    } else if (rand < 9) {
                      try {
                        controller.delete(targetId);
                      } catch (ObjectNotInCacheException e) {
                        if (numberOfLocks == 0) {
                          continue outer;
                        } else {
                          throw e;
                        }
                      }
                      objectId.set(controller.create(new CoherencePayload()));
                      for (int i = 0; i < numberOfLocks; i++) {
                        controller.unlock(targetId);
                      }
                      //try get another lock and release all locks on the object 
                    } else if (rand < 11) {
                      boolean gotLock = false;
                      try {
                        gotLock = controller.tryLock(targetId, random.nextInt(1000) * 1000); //0-1000 microseconds
                      } catch (ObjectNotInCacheException e) {
                      }
                      if (gotLock) {
                        controller.unlock(targetId);
                      }
                      for (int i = 0; i < numberOfLocks; i++) {
                        controller.unlock(targetId);
                      }
                    } else {
                      // do nothing, just unlock
                      for (int i = 0; i < numberOfLocks; i++) {
                        controller.unlock(targetId);
                      }
                    }
                  } finally {
                    currentlyWaitingForLock.getAndDecrement();
                  }
                } catch (Throwable t) {
                  t.printStackTrace();
                  atleastOneThreadFailed.set(true);
                  throw new RuntimeException(t);
                }
                count = totalCounter.incrementAndGet();
                if (count % 10000 == 0) {
                  System.out.println(count);
                }
              }
            }
          });
          threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
          threads[i].setDaemon(true);
          threads[i].start();
        }


        //iterate till max number of requests is reached or a thread is inactive for too long
        while (true) {
          //check for failed thread
          if (atleastOneThreadFailed.get()) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            throw new ClusterInconsistentException("One thread failed after " + totalCounter.get() + " events");
          }
          int sleeptime = 60;
          int maxSleepCnt = 20;
          //pause between each (while) iteration 
          Thread.sleep(sleeptime);
         
          for (int k = 0; k < busyCounter.length; k++) {
            AtomicInteger counter = busyCounter[k];
            //increment inactive time
            //check inactive time (fail if a thread is inactive for too long)
            if (counter.incrementAndGet() > maxSleepCnt) {
            

              for (int i = 0; i < threads.length; i++) {
                Exception e = new Exception();
                e.setStackTrace(threads[i].getStackTrace());
                e.printStackTrace();
              }
              System.out.println(currentlyWaitingForLock.get() + " threads are waiting for a lock, "
                  + totalCounter.get() + " events have finished, thread " + threads[k] + " has been inactive for "
                  + (counter.get() * sleeptime) + "ms, working on object id <" + currentlyWorkedObjectId[k] + ">");
              throw new ClusterInconsistentException(currentlyWaitingForLock.get()
                  + " threads are waiting for a lock, " + totalCounter.get() + " events have finished, thread "
                  + threads[k] + " has been inactive for " + (counter.get() * sleeptime) + "ms");
            }
          }
          //stop if max number of requests reached
          if (totalCounter.get() >= TEST_06_MAX_COUNT/factor) {
            break;
          }
        }

        //cleanup
        //give threads time to release locks
        Thread.sleep(1000);

        int count = 0;
        while (true) {
          //check if no thread waits for lock
          if (currentlyWaitingForLock.get() == 0) {
            break;
          }
          if (count++ > 100) {
            fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
          }
          Thread.sleep(50);
        }
        diffTime = (System.currentTimeMillis() - t1 - 100);
      } finally {
        //shutdown all cluster nodes
        for (CacheController controller : controllers) {
          if (controller != null) {
            controller.shutdown();
          }
        }
        System.out.println("finished " + diffTime);
     /*   FileOutputStream fos = new FileOutputStream(new File("log.txt"));
        try {
          Debugger.getDebugger().writeCSVToStream(fos);
        } finally {
          fos.close();
        }*/
      }

    }

  }


  public void atest_07_manyCreates() throws InterruptedException, IOException {

    for (int r = 0; r < 10; r++) {

      long t1 = System.currentTimeMillis();
      long diffTime = 0;
      final CacheController controller = CacheControllerFactory.newCacheController();
      try {
        final Random random = new Random();

        controller.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
        controller.setupNewCluster(); //neues cluster
        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean atleastOneThreadFailed = new AtomicBoolean();
        final AtomicInteger currentlyWaitingForLock = new AtomicInteger();
        final AtomicInteger busyCounter = new AtomicInteger();
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long count = totalCounter.get();
              while (!atleastOneThreadFailed.get() && count < TEST_07_MAX_COUNT) {
                busyCounter.set(0);

                if (count % 200000 == 0) {
                  System.out.println(count);
                }
                controller.create(new CoherencePayload());
                count = totalCounter.incrementAndGet();
              }
            }

          });
          threads[i].setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
          threads[i].setDaemon(true);
          threads[i].start();
        }


        while (true) {
          if (atleastOneThreadFailed.get()) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail("One thread failed after " + totalCounter.get() + " events");
          }
          if (totalCounter.get() >= TEST_07_MAX_COUNT) {
            break;
          }
          Thread.sleep(500);
          busyCounter.getAndIncrement();
          if (busyCounter.get() > 20) {
            for (int i = 0; i < threads.length; i++) {
              Exception e = new Exception();
              System.out.println(threads[i].getName());
              e.setStackTrace(threads[i].getStackTrace());
              e.printStackTrace();
            }
            fail(currentlyWaitingForLock.get() + " threads are waiting for a lock and only " + totalCounter.get()
                + " events have finished");
          }
        }
        Thread.sleep(100);


        int count = 0;
        while (true) {
          if (currentlyWaitingForLock.get() == 0) {
            break;
          }
          if (count++ > 100) {
            fail(currentlyWaitingForLock.get() + " threads are still waiting for lock");
          }
          Thread.sleep(50);
        }
        diffTime = (System.currentTimeMillis() - t1 - 100);
      } finally {
        controller.shutdown();
      }
      System.out.println("finished " + diffTime);
      FileOutputStream fos = new FileOutputStream(new File("log.txt"));
      try {
        Debugger.getDebugger().writeCSVToStream(fos);
      } finally {
        fos.close();
      }
    }
  }
  

}
