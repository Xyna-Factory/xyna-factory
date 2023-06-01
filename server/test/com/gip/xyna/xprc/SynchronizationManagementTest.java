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
package com.gip.xyna.xprc;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement;


public class SynchronizationManagementTest extends TestCase {

  private SynchronizationManagement sm = null;
  private List<XynaException> exceptionStorage = new ArrayList<XynaException>();
  private List<String> answerStorage = new ArrayList<String>();
  
  protected void setUp() throws Exception {
    super.setUp();
    sm = new SynchronizationManagement();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    exceptionStorage.clear();
    answerStorage.clear();
    sm = null;
  }

  private class TestWaiter extends Thread {
    
    private String id;
    private int time;
    
    public TestWaiter(String correlationId, int timeout) {
      id = correlationId;
      time = timeout;
    }
    
    @Override
    public void run() {
      try {
        String myAnswer;
        // FIXME pass something other than null for the XynaOrder
        myAnswer = sm.awaitNotification(id, time, 1, System.currentTimeMillis(), null, false, "laneId");

        answerStorage.add(myAnswer);
        
      } catch (XynaException e) {
        exceptionStorage.add(e);
      }
    }
  }
  
  private class TestSolver extends Thread {
    
    private String id;
    private String answer;
    private int time;
    
    public TestSolver(String correlationId, String incomingAnswer) {
      id = correlationId;
      answer = incomingAnswer;
    }
    
    @Override
    public void run() {
      try {
        // FIXME pass something other than null for the XynaOrder
        sm.notifyWaiting(id, answer, 1, null);
      } catch (XynaException e) {
        exceptionStorage.add(e);
      }
    }
  }
  
  public void testNormalProcedure() {
    TestWaiter waiter = new TestWaiter("OurIDToSyncOn", 120);
    TestSolver mailman = new TestSolver("OurIDToSyncOn", "testAnswer");
    
    waiter.start();    
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(true, waiter.isAlive());
    
    mailman.start();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    
    assertEquals(false, waiter.isAlive());
    assertEquals(false, mailman.isAlive());
    assertEquals(true, exceptionStorage.isEmpty());
    assertEquals(true, answerStorage.contains("testAnswer"));
  }
  
  public void testEarlyAnswer() {
    TestWaiter waiter = new TestWaiter("OurIDToSyncOn", 120);
    TestSolver mailman = new TestSolver("OurIDToSyncOn", "testAnswer");
    
    mailman.start();    
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, mailman.isAlive());
    
    waiter.start();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, waiter.isAlive());
    assertEquals(true, exceptionStorage.isEmpty());
    assertEquals(true, answerStorage.contains("testAnswer"));
  }
  

  public void testInvalidAnswer() {
  //  sm.ANSWER_TIMEOUT = 1; TODO umbauen, wenn die tests anderweitig auch repariert werden
    
    TestWaiter waiter1 = new TestWaiter("OurIDToSyncOn", 120);
    TestWaiter waiter2 = new TestWaiter("OurIDToSyncOn", 120);
    TestSolver mailman = new TestSolver("OurIDToSyncOn", "testAnswer");
    TestSolver impatientMailman1 = new TestSolver("OurIDToSyncOn", "testAnswer");
    TestSolver impatientMailman2 = new TestSolver("OurIDToSyncOn", "testAnswer");

    // this sleep was short, answer should be received
    impatientMailman1.start();
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, impatientMailman1.isAlive());

    waiter1.start();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, waiter1.isAlive());

    // this sleep was longer, answer should be too old
    impatientMailman2.start();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, impatientMailman2.isAlive());

    waiter2.start();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    //still waiting
    assertEquals(true, waiter2.isAlive());


    mailman.start();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, mailman.isAlive());
    assertEquals(false, waiter2.isAlive());
  }

  
  public void testTimeout() {
    TestWaiter waiter = new TestWaiter("OurIDToSyncOn", 1);
    
    waiter.start();    
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(true, waiter.isAlive());
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    assertEquals(false, waiter.isAlive());
    assertEquals(true, answerStorage.isEmpty());
    assertEquals(false, exceptionStorage.isEmpty());
    assertEquals(new XPRC_TIMEOUT_DURING_SYNCHRONIZATION("OurIDToSyncOn").getCode(), exceptionStorage.get(0).getCode());   
  }
  
  //StressTest
  public void testStress() {
    int stressLevel = 100;
    List<TestWaiter> waiters = new ArrayList<TestWaiter>();
    List<TestSolver> mailmen = new ArrayList<TestSolver>();
    for (int i = 0; i < stressLevel; i++) {
      waiters.add(new TestWaiter("mySyncId"+i, 120));
      mailmen.add(new TestSolver("mySyncId"+i, "answer#"+i));      
    }
    
    for (TestWaiter testWorker : waiters) {
      testWorker.start();
    }
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    
    for (TestWaiter testWorker : waiters) {
      assertEquals(true, testWorker.isAlive());
    }
    for (TestSolver testWorker : mailmen) {
      testWorker.start();
    }
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail();
    }
    
    for (TestSolver testWorker : mailmen) {
      assertEquals(false, testWorker.isAlive());
    }
    for (TestWaiter testWorker : waiters) {
      assertEquals(false, testWorker.isAlive());
    }
    assertEquals(true, exceptionStorage.isEmpty());
    assertEquals(stressLevel, answerStorage.size());
    for (int i = 0; i < stressLevel; i++) {
      assertEquals(true, answerStorage.contains("answer#"+i));
    }
  }

}
