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

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class CacheController_Functional_Lowlevel_OnlineTest extends TestCase {

  private static final Logger logger = LoggerFactory.getLogger(CacheController_Functional_Lowlevel_OnlineTest.class);
  private static final SecureRandom random = new SecureRandom();

  private static final long SLEEP_BETWEEN_ACTIONS = 50;


  private static int getRandomThreadPriority() {
    return random.nextInt(Thread.MAX_PRIORITY) + 1;
  }


  public void test_01_OneNodeCreateAndRead() throws ObjectNotInCacheException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();
    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);
    CoherencePayload payload2 = cc1.read(objectId);
    assertTrue(payload1 == payload2);
  }


  public void test_02_OneNodeCreateAndDeleteAndRead() throws ObjectNotInCacheException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();
    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);
    cc1.delete(objectId);
    try {
      cc1.read(objectId);
      fail("Object not successfully deleted");
    } catch (ObjectNotInCacheException e) {
    }
  }


  public void test_03_OneNodeCreateAndUpdateAndDelete() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);

    CoherencePayload payload2 = new CoherencePayload();
    cc1.update(objectId, payload2);

    CoherencePayload payload2Read = cc1.read(objectId);
    assertEquals(payload2, payload2Read);

  }


  public void test_04_OneNodeCreateAndUpdateAndReadAndDeleteAndRead() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);

    CoherencePayload payload2 = new CoherencePayload();
    cc1.update(objectId, payload2);

    CoherencePayload payload2Read = cc1.read(objectId);
    assertEquals(payload2, payload2Read);

    cc1.delete(objectId);

    try {
      cc1.read(objectId);
      fail("Object not successfully deleted");
    } catch (ObjectNotInCacheException e) {
    }

  }


  public void test_05_ThreeNodes_Create1_Read2_Read3() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);

    CoherencePayload payload1Restored2 = cc2.read(objectId);
    assertEquals(payload1, payload1Restored2);

    CoherencePayload payload1Restored3 = cc3.read(objectId);
    assertEquals(payload1, payload1Restored3);

  }


  public void test_06_ThreeNodes_Create2_Update3_Read1() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc1.create(payload1);

    CoherencePayload payload2 = new CoherencePayload();
    cc2.update(objectId, payload2);

    CoherencePayload payload1Restored3 = cc3.read(objectId);
    assertEquals(payload2, payload1Restored3);
    if (payload1.equals(payload1Restored3)) {
      fail("Object has not been overwritten");
    }

  }


  public void test_07_ThreeNodes_Create3_Delete1_Read3_Read2() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payload1 = new CoherencePayload();
    long objectId = cc3.create(payload1);

    cc1.delete(objectId);

    try {
      cc3.read(objectId);
      fail("Object not deleted successfully");
    } catch (ObjectNotInCacheException e) {
    }

    try {
      cc2.read(objectId);
      fail("Object not deleted successfully");
    } catch (ObjectNotInCacheException e) {
    }

  }


  public void test_08_ThreeNodes_Create1_Lock2_Read3_Unlock2() throws ObjectNotInCacheException, InterruptedException {

    logger.debug("Running test 8");

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    final CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    Thread.currentThread().setPriority(getRandomThreadPriority());

    final CoherencePayload payload1 = new CoherencePayload();
    final long objectId = cc1.create(payload1);

    cc2.lock(objectId);

    final AtomicBoolean secondThreadReadDone = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();

    Thread t = new Thread(new Runnable() {

      public void run() {
        try {
          CoherencePayload result = cc3.read(objectId);
          secondThreadFailed.set(!result.equals(payload1));
          secondThreadReadDone.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t.setPriority(getRandomThreadPriority());
    t.start();

    Thread.sleep(SLEEP_BETWEEN_ACTIONS);

    if (secondThreadFailed.get()) {
      fail("Second thread failed");
    } else if (secondThreadReadDone.get()) {
      fail("Second thread accessed object though it was locked");
    }

    cc2.unlock(objectId);

    Thread.sleep(SLEEP_BETWEEN_ACTIONS);

    if (secondThreadFailed.get()) {
      fail("Second thread failed");
    } else if (!secondThreadReadDone.get()) {
      fail("Second thread did not finish though lock was released");
    }

  }


  public void test_09_ThreeNodes_Create2_Lock3_Read1_Read3_Read2_Unlock3() throws ObjectNotInCacheException,
      InterruptedException {
    for (int i = 0; i < 1; i++) {
      ttest09_internally();
    }
  }


  private void ttest09_internally() throws ObjectNotInCacheException, InterruptedException {
    logger.debug("Running test 9");

    final CacheController ccID0 = CacheControllerFactory.newCacheController();
    ccID0.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    ccID0.setupNewCluster(); //neues cluster

    final CacheController ccID1 = CacheControllerFactory.newCacheController();
    ccID1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    final CacheController ccID2 = CacheControllerFactory.newCacheController();
    ccID2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = ccID0.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    ccID1.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                           .get(0)));
    ccID2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                           .get(0)));

    Thread.currentThread().setPriority(getRandomThreadPriority());

    for (int i = 0; i < 10; i++) {

      final CoherencePayload payload1 = new CoherencePayload();
      final long objectId = ccID1.create(payload1);

      ccID2.lock(objectId);

      final AtomicBoolean secondThreadReadDone = new AtomicBoolean();
      final AtomicBoolean secondThreadFailed = new AtomicBoolean();

      final AtomicBoolean thirdThreadReadDone = new AtomicBoolean();
      final AtomicBoolean thirdThreadFailed = new AtomicBoolean();

      Thread t2 = new Thread(new Runnable() {

        public void run() {
          try {
            CoherencePayload result = ccID0.read(objectId);
            secondThreadFailed.set(!result.equals(payload1));
            secondThreadReadDone.set(true);
          } catch (Throwable t) {
            t.printStackTrace();
            secondThreadFailed.set(true);
          }
        }
      });
      t2.setPriority(getRandomThreadPriority());
      t2.start();

      Thread.sleep(SLEEP_BETWEEN_ACTIONS);

      if (secondThreadFailed.get()) {
        fail("Second thread failed");
      } else if (secondThreadReadDone.get()) {
        fail("Second thread accessed object though it was locked");
      }

      CoherencePayload payloadRead3 = ccID2.read(objectId);
      assertEquals(payload1, payloadRead3);

      Thread t3 = new Thread(new Runnable() {

        public void run() {
          try {
            CoherencePayload result = ccID1.read(objectId);
            thirdThreadFailed.set(!result.equals(payload1));
            thirdThreadReadDone.set(true);
          } catch (Throwable t) {
            t.printStackTrace();
            thirdThreadFailed.set(true);
          }
        }
      });
      t3.setPriority(getRandomThreadPriority());
      t3.start();

      Thread.sleep(SLEEP_BETWEEN_ACTIONS);

      if (thirdThreadFailed.get()) {
        fail("Third thread failed");
      } else if (thirdThreadReadDone.get()) {
        fail("Third thread accessed object though it was locked");
      }

      ccID2.unlock(objectId);

      Thread.sleep(SLEEP_BETWEEN_ACTIONS);

      if (secondThreadFailed.get()) {
        fail("Second thread failed");
      }
      if (!secondThreadReadDone.get()) {
        int cnt = 0;
        do {
          if (cnt++ > 50) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            sdf.setLenient(false);
            Exception e = new Exception(t2.getName() + "; " + sdf.format(new Date()));
            e.setStackTrace(t2.getStackTrace());
            e.printStackTrace();
            fail("Second thread did not finish though lock was released");
          }
          Thread.sleep(SLEEP_BETWEEN_ACTIONS);
        } while (!secondThreadReadDone.get());
      }

      if (thirdThreadFailed.get()) {
        fail("Third thread failed");
      } else if (!thirdThreadReadDone.get()) {
        for (StackTraceElement[] t : Thread.getAllStackTraces().values()) {
          Exception e = new Exception();
          e.setStackTrace(t);
          e.printStackTrace();
        }
        fail("Third thread did not finish though lock was released");
      }

    }

    ccID0.shutdown();
    ccID1.shutdown();
    ccID2.shutdown();

  }


  public void test_10_ThreeNodes_Create1_Lock2_Lock3_Read2_Unlock2_Read1_Unlock3() throws ObjectNotInCacheException,
      InterruptedException {

    logger.debug("Running test 10");

    final CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    final CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    final CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    Thread.currentThread().setPriority(getRandomThreadPriority());

    final CoherencePayload payload1 = new CoherencePayload();
    final long objectId = cc1.create(payload1);

    cc2.lock(objectId);

    final AtomicBoolean secondThreadLocked = new AtomicBoolean();
    final AtomicBoolean secondThreadUnlocked = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();

    final CountDownLatch latchToNotifySecondThreadToUnlock = new CountDownLatch(1);

    Thread t2 = new Thread(new Runnable() {

      public void run() {
        try {
          cc3.lock(objectId);
          secondThreadLocked.set(true);
          latchToNotifySecondThreadToUnlock.countDown();
          Thread.sleep(SLEEP_BETWEEN_ACTIONS);
          cc3.unlock(objectId);
          secondThreadUnlocked.set(true);
        } catch (Throwable t) {
          t.printStackTrace();
          secondThreadFailed.set(true);
        }
      }
    });
    t2.setPriority(getRandomThreadPriority());
    t2.start();

    Thread.sleep(SLEEP_BETWEEN_ACTIONS);

    if (secondThreadFailed.get()) {
      fail("Second thread failed");
    } else if (secondThreadLocked.get()) {
      fail("Second thread locked object though it was already locked");
    }

    CoherencePayload payloadRead2 = cc2.read(objectId);
    assertEquals(payload1, payloadRead2);

    cc2.unlock(objectId);

    Thread.sleep(SLEEP_BETWEEN_ACTIONS);

    if (secondThreadFailed.get()) {
      fail("Second thread failed");
    } else if (!secondThreadLocked.get()) {
      fail("Second thread could not get lock");
    }

    latchToNotifySecondThreadToUnlock.await();

    CoherencePayload payloadRead1 = cc1.read(objectId);
    assertEquals(payload1, payloadRead1);

    Thread.sleep(SLEEP_BETWEEN_ACTIONS);

    if (secondThreadFailed.get()) {
      fail("Second thread failed");
    } else if (!secondThreadUnlocked.get()) {
      fail("Second thread could not unlock");
    }
  }


  public void test_11_ThreeNodes_Lock1_Delete1_Unlock1() throws ObjectNotInCacheException, InterruptedException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    final CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) callees.get(0)));

    CoherencePayload payload1 = new CoherencePayload();
    final long objectId = cc1.create(payload1);

    // read the object on node 2 to perform a state transition to SHARED since only then remote locks are involved
    cc2.read(objectId);

    final CountDownLatch latch = new CountDownLatch(1);

    final AtomicBoolean secondThreadSucceeded = new AtomicBoolean();
    final AtomicBoolean secondThreadFailed = new AtomicBoolean();
    Thread t = new Thread(new Runnable() {
      public void run() {
        try {
          latch.await();
          cc2.lock(objectId);
        } catch (ObjectNotInCacheException e) {
          secondThreadSucceeded.set(true);
        } catch (Throwable t) {
          secondThreadFailed.set(true);
        }
      }
    });
    t.start();

    Thread.sleep(50);

    cc1.lock(objectId);

    latch.countDown();

    cc1.delete(objectId);
    cc1.unlock(objectId);

    Thread.sleep(50);

    assertFalse(secondThreadFailed.get());
    assertTrue(secondThreadSucceeded.get());

  }


  public void test_12_DoubleLockAndDelete() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster(); //neues cluster

    final CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) callees.get(0)));

    CoherencePayload payload1 = new CoherencePayload();
    final long objectId = cc1.create(payload1);

    cc1.lock(objectId);
    cc1.lock(objectId);

    cc1.delete(objectId);

    cc1.unlock(objectId);
    cc1.unlock(objectId);

  }

}
