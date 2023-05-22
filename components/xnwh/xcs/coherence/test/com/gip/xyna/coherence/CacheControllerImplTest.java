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
package com.gip.xyna.coherence;



import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.junit.Test;

import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;



public class CacheControllerImplTest extends TestCase {

  @Test
  public void testCacheControllerNeedsInit() {
    CacheController cc = CacheControllerFactory.newCacheController();
    try {
      cc.create(new CoherencePayload());
      assertTrue("should have failed with illegalstateexception", false);
    } catch (IllegalStateException e) {
    }
  }


  @Test
  public void testConnectToCluster() {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());

    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));
    cc1.shutdown();
    cc2.shutdown();

  }
  
  public void testReconnect() throws IOException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());

    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) callees.get(0)));
    cc1.disconnectFromCluster();
    cc1.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc2.getCallees().get(0)));
    cc1.shutdown();
    cc2.shutdown();
  }


  public void testPushObjectWhenDisconnect() throws ObjectNotInCacheException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc1.getCallees().get(0)));
    
    CoherencePayload payloadOrig = new CoherencePayload();
    long objectId = cc2.create(payloadOrig);
    cc2.disconnectFromCluster();
    CoherencePayload payload = cc1.read(objectId);
    assertEquals(payload, payloadOrig);
    
    cc1.shutdown();
    cc2.shutdown();
  }
  
  public void testPushObjectWhenDisconnect2() throws ObjectNotInCacheException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc1.getCallees().get(0)));
    
    CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc2.getCallees().get(0)));

    CoherencePayload payloadOrig = new CoherencePayload();
    long[] objects = new long[10];
    for (int i = 0; i < 10; i++) {
      objects[i] = cc3.create(payloadOrig);
    }
  //node 1 und 2 wissen, dass die payload bei node 3 zu finden ist.
    
    cc3.disconnectFromCluster(); //payloads werden auf node 1 und 2 aufgeteilt
    
    for (int i = 0; i<10; i++) {
      cc1.read(objects[i]);
    }
    
    cc3.shutdown();
    cc2.shutdown();
    cc1.shutdown();
  }
  
  public void testDirectoryDataWhenDisconnect() throws ObjectNotInCacheException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc1.getCallees().get(0)));
    
    CacheController cc3 = CacheControllerFactory.newCacheController();
    cc3.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc3.connectToClusterLocally(NodeConnectionProviderFactory.getInstance()
        .getJavaProvider((InterconnectCalleeJava) cc2.getCallees().get(0)));

    CoherencePayload payloadOrig = new CoherencePayload();
    long[] objects = new long[10];
    for (int i = 0; i < 10; i++) {
      objects[i] = cc1.create(payloadOrig);
    }
    
    for (int i = 0; i < 10; i++) {
      cc3.read(objects[i]);
    }
    //objekte sind jetzt shared zwischen 1 und 3
    
    cc3.disconnectFromCluster(); 
    
    for (int i = 0; i<10; i++) {
      cc2.read(objects[i]); //nur noch von node 1 holen, nicht von 3
    }
    
    cc3.shutdown();
    cc2.shutdown();
    cc1.shutdown();
  }
  
  
  
  

  @Test
  public void testRoundTripWithoutCluster() throws ObjectNotInCacheException {
    
  }


  @Test
  public void testObjectReadInOtherNode() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();
    
    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payloadOrig = new CoherencePayload();
    long objectId = cc1.create(payloadOrig);
    CoherencePayload payloadFromSelf = cc1.read(objectId);

    CoherencePayload payloadFromOtherNode = cc2.read(objectId);
    assertEquals(payloadFromSelf, payloadFromOtherNode);

    cc1.disconnectFromCluster();
    cc1.shutdown();
    cc2.shutdown();

  }


  @Test
  public void testObjectReadInSecondAndWriteinThirdAndReadInFirst() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

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

    CoherencePayload payloadOrig = new CoherencePayload();
    long objectId = cc1.create(payloadOrig);
    CoherencePayload payloadFromSelf = cc1.read(objectId);

    CoherencePayload payloadFromOtherNode = cc2.read(objectId);
    assertEquals(payloadFromSelf, payloadFromOtherNode);

    CoherencePayload updatedPayload = new CoherencePayload();
    cc3.update(objectId, updatedPayload);

    assertEquals(updatedPayload, cc1.read(objectId));

    cc1.disconnectFromCluster();
    cc1.shutdown();
    cc2.shutdown();

  }


  @Test
  public void testDeleteObjectLocally() throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CoherencePayload payloadOrig = new CoherencePayload();
    long objectId = cc1.create(payloadOrig);
    CoherencePayload payloadFromSelf = cc1.read(objectId);

    assertEquals(payloadFromSelf, payloadOrig);

    cc1.delete(objectId);

    try {
      cc1.read(objectId);
      fail("Object was not deleted successfully");
    } catch (ObjectNotInCacheException e) {
    }
    cc1.shutdown();

  }


  @Test
  public void testDeleteObjectRemoteAndReadBefore() throws ObjectNotInCacheException {
    deleteTestInternally(true);
  }


  @Test
  public void testDeleteObjectRemoteAndDontReadBefore() throws ObjectNotInCacheException {
    deleteTestInternally(false);
  }


  private void deleteTestInternally(boolean readBeforeDelete) throws ObjectNotInCacheException {

    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    assertEquals(1, callees.size());
    assertEquals(InterconnectCalleeJava.class, callees.get(0).getClass());
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payloadOrig = new CoherencePayload();
    long objectId = cc1.create(payloadOrig);

    CoherencePayload payloadFromSelf = cc1.read(objectId);
    assertEquals(payloadFromSelf, payloadOrig);

    if (readBeforeDelete) {
      CoherencePayload payloadFromRemote = cc2.read(objectId);
      assertEquals(payloadFromRemote, payloadOrig);
    }

    cc2.delete(objectId);

    try {
      cc1.read(objectId);
      fail("Object was not deleted successfully");
    } catch (ObjectNotInCacheException e) {
    }

    try {
      cc2.read(objectId);
      fail("Object was not deleted successfully");
    } catch (ObjectNotInCacheException e) {
    }

    cc1.shutdown();
    cc2.shutdown();
  }
  
  public void testTryLock() throws ObjectNotInCacheException, InterruptedException {
    CacheController cc1 = CacheControllerFactory.newCacheController();
    cc1.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());
    cc1.setupNewCluster();

    final CacheController cc2 = CacheControllerFactory.newCacheController();
    cc2.addCallee(InterconnectCalleeProviderFactory.getInstance().getJavaProvider());

    List<InterconnectCallee> callees = cc1.getCallees();
    cc2.connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getJavaProvider((InterconnectCalleeJava) callees
                                                                                         .get(0)));

    CoherencePayload payloadOrig = new CoherencePayload();
    final long objectId = cc1.create(payloadOrig);
    cc1.lock(objectId);    
    final CountDownLatch latch = new CountDownLatch(1);
    System.out.println(System.nanoTime());
    long t0 = System.currentTimeMillis();
    Thread t = new Thread(new Runnable() {

      public void run() {
        try {
          if (cc2.tryLock(objectId, TimeUnit.SECONDS.toNanos(2))) {
            fail("lock should not have succeeded");
          } else {
            System.out.println("lock did not succeed");
          }
        } catch (ObjectNotInCacheException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        } finally {
          latch.countDown();
        }
      }
      
    });
    t.start();

    latch.await();
    assertTrue("locking try failed too fast", System.currentTimeMillis() - t0 > 2000);
    assertTrue("locking try failed to slow", System.currentTimeMillis() - t0 < 2500);
    
    cc1.unlock(objectId);
    cc1.lock(objectId);
  }

}
