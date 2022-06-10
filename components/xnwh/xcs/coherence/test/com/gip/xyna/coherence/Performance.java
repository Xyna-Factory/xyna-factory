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

package com.gip.xyna.coherence;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
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
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.debugging.Debugger;



public class Performance extends TestCase {

  public void testBla() throws IOException, InterruptedException {
/*
    Profile.initProfiler();
    Profile.start();
*/
    try {
      final Random random = new Random();
      for (int k = 1; k < 20; k++) {
        final long maxCount = 3000000;

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

        Thread[] threads = new Thread[8];
        long t0 = System.currentTimeMillis(); 
        for (int i = 0; i < threads.length; i++) {
          threads[i] = new Thread(new Runnable() {

            public void run() {
              long count = totalCounter.get();
              while (!atleastOneThreadFailed.get() && count < maxCount) {

                try {
                 /* if (random.nextLong() % 1024 * 64 == 0) {
                    Thread.currentThread().setPriority(random.nextInt(Thread.MAX_PRIORITY) + 1);
                  }*/
                  if (count % 800000 == 0) {
                    System.out.println(count);
                  }
                  busyCounter.set(0);
                  // FIXME controllers size kann inkorrekt sein, wenn sie verwendet wird
                  CacheController controller = controllers.get(random.nextInt(controllers.size()));

                  int type = 1; // random.nextInt(1);
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
                          e.printStackTrace();
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
                        controller.read(objectId);
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

//                  l.readLock().lock();
//                  try {
//                    if (random.nextInt(15) == 0 && controllers.size() < 7 && count > 80) {
//                      CacheController newController = CacheControllerFactory.newCacheController();
//                      newController.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
//                      int idx = random.nextInt(controllers.size());
//                      try {
//                        newController.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
//                            .getJavaProvider((InterconnectCalleeJava) controllers.get(idx).getCallees().get(0)));
//                        controllers.add(newController);
//                        System.out.println(controllers.size() + " controllers");
//                      } catch (IllegalStateException e) {
//                        //knoten wurde inzwischen heruntergefahren
//                        System.out.println("node down?");
//                      }
//                    }
//                  } finally {
//                    l.readLock().unlock();
//                  }
//                  if (random.nextInt(15) == 0) {
//                    l.writeLock().lock();
//                    try {
//                      if (controllers.size() > 1) {
//                        while (currentlyWaitingForLock.get() > 0) {
//                          Thread.sleep(30);
//                        }
//                        CacheController c = null;
//                        synchronized (payload) {
//                          if (controllers.size() > 1) {
//                            int idx = random.nextInt(controllers.size());
//                            System.out.println("removing " + idx);
//                            c = controllers.get(idx);
//                            controllers.remove(idx);
//                          }
//                        }
//                        if (c != null) {
//                          c.disconnectFromCluster();
//                          c.shutdown();
//                        }
//                      }
//                    } finally {
//                      l.writeLock().unlock();
//                    }
//                  }
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
          threads[i].setName("my thread " + i);
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
          if (totalCounter.get() >= maxCount) {
            break;
          }
          Thread.sleep(50);
          busyCounter.getAndIncrement();
          if (busyCounter.get() > 200) {
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

        //Thread.sleep(1000);

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

        System.out.println("rate: " + (1000.0* maxCount / (System.currentTimeMillis() - t0)) + " Hz" );

        
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
   // dumpThreads();
/*
    Profile.stop();
    Profile.shutdown();
*/
  }


  private void dumpThreads() {
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    long[] ids = tbean.getAllThreadIds();
    ThreadInfo[] tis = tbean.getThreadInfo(ids, 200);
    StringBuilder sb = new StringBuilder();
    for (ThreadInfo ti : tis) {
      if (ti != null) { //null if thread is not active any more
        if (tbean.isThreadCpuTimeEnabled() || tbean.isThreadContentionMonitoringEnabled()) {
          sb.append("  Thread.");
          if (tbean.isThreadCpuTimeEnabled()) {
            sb.append("cputime=").append(tbean.getThreadCpuTime(ti.getThreadId())/1000000);
            sb.append("ms\n   .usertime=").append(tbean.getThreadUserTime(ti.getThreadId())/1000000);
            sb.append("ms\n   ");
          }
          if (tbean.isThreadContentionMonitoringEnabled()) {
            sb.append(".blocked=").append(ti.getBlockedTime());
            sb.append("ms\n   .waited=").append(ti.getWaitedTime());
            sb.append("ms\n");
          }
        }
        sb.append(getThreadInfo(ti));
      }
    }

    System.out.println(sb.toString());

  }


  private String getThreadInfo(ThreadInfo ti) {
    //FIXME code aus java6 ThreadInfo.toString() geklaut. in java5 ist toString() sehr dürftig.
    StringBuilder sb =
        new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " " + ti.getThreadState());
    if (ti.getLockName() != null) {
      sb.append(" on " + ti.getLockName());
    }
    if (ti.getLockOwnerName() != null) {
      sb.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
    }
    if (ti.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (ti.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    int i = 0;
    StackTraceElement[] stackTrace = ti.getStackTrace();
    for (; i < stackTrace.length && i < 200; i++) {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && ti.getLockName() != null) {
        Thread.State ts = ti.getThreadState();
        switch (ts) {
          case BLOCKED :
            sb.append("\t-  blocked on " + ti.getLockName());
            sb.append('\n');
            break;
          case WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          case TIMED_WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          default :
        }
      }
    }
    if (i < stackTrace.length) {
      sb.append("\t...");
      sb.append('\n');
    }

    sb.append('\n');
    return sb.toString();
  }

}
