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
package com.gip.xyna.utils.collections.queues;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class RandomizingBlockingQueueTest extends TestCase {
  
  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //dann halt k�rzer
    }
  }
  
  public void testNormalQueue() throws InterruptedException {
    
    RandomizingBlockingQueue<String> rbq = new RandomizingBlockingQueue<String>();
    
    rbq.add("A");
    rbq.add("B");
    rbq.add("C");
    
    assertEquals("[A, B, C]", rbq.toString() );
    
    List<String> drawn = new ArrayList<String>();
    drawn.add( rbq.poll() );
    drawn.add( rbq.poll() );
    drawn.add( rbq.poll() );
    drawn.add( rbq.poll() );
    
  
    //assertEquals("[B, A, C, null]", drawn.toString() ); stimmt nur manchmal...
    
    assertNull( rbq.poll() );
    assertEquals("[]", rbq.toString() );
    
    rbq.offer("D");
    assertEquals("[D]", rbq.toString() );
    assertEquals("D", rbq.poll() );
    
  }

  public void testBlockingQueue() throws InterruptedException {
    final StringBuilder sb = new StringBuilder();
    
    final RandomizingBlockingQueue<String> rbq = new RandomizingBlockingQueue<String>();
    Thread t1 = new Thread() {
       public void run() {
         sb.append("taking ");
         try {
          sb.append(rbq.take() );
        } catch (InterruptedException e) {
          
        }
         sb.append(".");
       }
    };
    
    t1.start();
    
    sleep(200);
    
    assertEquals("taking ", sb.toString() );
    
    rbq.offer("X");
    
    t1.join();
    
    assertEquals("taking X.", sb.toString() );
    
  }
  
}
