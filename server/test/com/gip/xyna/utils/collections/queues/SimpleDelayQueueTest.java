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
package com.gip.xyna.utils.collections.queues;

import junit.framework.TestCase;

public class SimpleDelayQueueTest extends TestCase {

  //TODO Reihenfolgen bei toString() sind nicht definiert -> evtl. Testfehler
  
  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //dann halt kürzer
    }
  }
  
  public void testNormalQueue() throws InterruptedException {
    
    SimpleDelayQueue<String> sdq = new SimpleDelayQueue<String>();
    
    sdq.add("A");
    sleep(10); //Für Reihenfolge
    sdq.add("B");
    
    assertEquals("[A, B]", sdq.toString() );
    
    assertEquals("A", sdq.peek() );
    assertEquals("[A, B]", sdq.toString() );
    assertEquals("A", sdq.poll() );
    assertEquals("[B]", sdq.toString() );
    sdq.clear();
    assertEquals("[]", sdq.toString() );
    
  }
  
  public void testTimedRelative() throws InterruptedException {
    SimpleDelayQueue<String> sdq = new SimpleDelayQueue<String>();
    
    sdq.add("A", 100);
    sdq.offer("B", 200);
   
    assertEquals("[A, B]", sdq.toString() );
    
    assertEquals("A", sdq.peek() );
    assertNull( sdq.poll() );
    
    sleep(110);
    
    
    assertEquals("A", sdq.peek() );
    assertEquals("[A, B]", sdq.toString() );
    assertEquals("A", sdq.poll() );
    assertEquals("[B]", sdq.toString() );
    sdq.clear();
    assertEquals("[]", sdq.toString() );
    
  }
 
  public void testTimedAbsolute() throws InterruptedException {
    long now = System.currentTimeMillis();
    SimpleDelayQueue<String> sdq = new SimpleDelayQueue<String>();
    
    sdq.addAbsolute("A", now + 100);
    sdq.offerAbsolute("B", now + 200);
   
    assertEquals("[A, B]", sdq.toString() );
    
    assertEquals("A", sdq.peek() );
    assertNull( sdq.poll() );
    
    sleep(110);
    
    
    assertEquals("A", sdq.peek() );
    assertEquals("[A, B]", sdq.toString() );
    assertEquals("A", sdq.poll() );
    assertEquals("[B]", sdq.toString() );
    sdq.clear();
    assertEquals("[]", sdq.toString() );
    
  }
  
  public void testRemove() {
    SimpleDelayQueue<String> sdq = new SimpleDelayQueue<String>();
    
    sdq.add("A", 100);
    sdq.offer("B", 200);
    sdq.add("C", 150);
    sdq.offer("D", 300);
   
    
    assertEquals("[A, B, C, D]", sdq.toString() );
    
    sdq.remove("B");
    
    assertEquals("[A, D, C]", sdq.toString() );
    
  }
  
  public void testList() {
    SimpleDelayQueue<String> sdq = new SimpleDelayQueue<String>();
    long now = System.currentTimeMillis();
    
    sdq.addAbsolute("A", now + 100);
    sdq.offerAbsolute("B", now + 200);
    sdq.addAbsolute("C", now + 150);
    sdq.offerAbsolute("D", now + 300);
    

    assertEquals( "[Pair(A,100), Pair(C,150), Pair(B,200), Pair(D,300)]" , sdq.listAllEntriesOrdered(now).toString() );
    
     
    sdq.remove("B");
    
    
    assertEquals( "[Pair(A,50), Pair(C,100), Pair(D,250)]" , sdq.listAllEntriesOrdered(now+50).toString() );
    
   
  }

  
  
  
  

}
