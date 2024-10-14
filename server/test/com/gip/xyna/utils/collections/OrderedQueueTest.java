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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Assert;


public class OrderedQueueTest extends TestCase {

  
  public void testOrdered() {
    
    OrderedQueue<String> oq = new OrderedQueue<String>();
    
    oq.add("C");
    oq.offer("B");
    oq.addAll( Arrays.asList(new String[]{"D","A"}) );
    
    
    Assert.assertEquals( "[A, B, C, D]", oq.toString() );
    Assert.assertEquals( "A", oq.peek() );
    Assert.assertEquals( "A", oq.poll() );
    Assert.assertEquals( "B", oq.poll() );
    Assert.assertEquals( "C", oq.poll() );
    Assert.assertEquals( "D", oq.poll() );
    Assert.assertNull( oq.poll() );
    Assert.assertNull( oq.peek() );
    
  }
  
  private static class VarInt implements Comparable<VarInt> {

    
    private int value;

    public VarInt(int value) {
      this.value = value;
    }

    public int compareTo(VarInt o) {
      return value-o.value;
    }

    public int get() {
      return value;
    }

    public void add(int inc) {
      value += inc;
    }
    
  }
  
  public void testRefresh() {
    OrderedQueue<VarInt> oq = new OrderedQueue<VarInt>();
    oq.offer(new VarInt(1) );
    oq.offer(new VarInt(1) );
    
    ArrayList<Integer> fibonacci = new ArrayList<Integer>();
    for( int i=0; i< 15; ++i ) {
      VarInt first = oq.poll();
      fibonacci.add(first.get());
      int second = oq.peek().get();
      oq.offer(first);
      //System.out.println( first.get() + " " + second);
      first.add(second);
      oq.refresh(); 
    }
    
    Assert.assertEquals( "[1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610]", fibonacci.toString() );
      
  }
  
  
  public void testClear() {
    
    OrderedQueue<String> oq = new OrderedQueue<String>();
    
    oq.add("C");
    oq.offer("B");
    oq.addAll( Arrays.asList(new String[]{"D","A"}) );
    
    Assert.assertEquals( "[A, B, C, D]", oq.toString() );
    oq.clear();
    
    Assert.assertEquals( "[]", oq.toString() );
    
  }
  
  public void testRemove() {
  
    OrderedQueue<String> oq = new OrderedQueue<String>();

    oq.add("C");
    oq.offer("B");
    oq.addAll( Arrays.asList(new String[]{"D","A"}) );

    Assert.assertEquals( "[A, B, C, D]", oq.toString() );
    oq.remove("B");

    Assert.assertEquals( "[A, C, D]", oq.toString() );
  }

  
}
