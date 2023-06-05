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


public class RandomAccessArrayListTest extends TestCase {
  
  
  public void testArrayList() {
    
    ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(0);
    try {
      list.set( 3, 11);
      fail("Exception expected");
    } catch( IndexOutOfBoundsException e ) {
      assertEquals( "Index: 3, Size: 1", e.getMessage() );
    }
    try {
      list.add( 5, 17);
      fail("Exception expected");
    } catch( IndexOutOfBoundsException e ) {
      assertEquals( "Index: 5, Size: 1", e.getMessage() );
    }
    try {
      list.get( 7 );
      fail("Exception expected");
    } catch( IndexOutOfBoundsException e ) {
      assertEquals( "Index: 7, Size: 1", e.getMessage() );
    }
    assertEquals( "[0]", list.toString() );
    list.add(1);
    list.add(2);
    list.add(3);
    assertEquals( "[0, 1, 2, 3]", list.toString() );
    list.add(2, 9);
    assertEquals( "[0, 1, 9, 2, 3]", list.toString() );
    
  }
  
  public void testSet() {
    
    RandomAccessArrayList<Integer> list = new RandomAccessArrayList<Integer>();
    
    list.add(0);
    
    list.set( 3, 11);
    assertEquals( "[0, null, null, 11]", list.toString() );
    
    list.set( 5, 17);
    assertEquals( "[0, null, null, 11, null, 17]", list.toString() );
    
    list.set( 4, 9);
    assertEquals( "[0, null, null, 11, 9, 17]", list.toString() );
    
  }

  public void testAdd() {
    
    RandomAccessArrayList<Integer> list = new RandomAccessArrayList<Integer>();
    
    list.add(0);
    
    list.add( 3, 11);
    assertEquals( "[0, null, null, 11]", list.toString() );
    
    list.add( 5, 17);
    assertEquals( "[0, null, null, 11, null, 17]", list.toString() );
    
    list.add( 4, 9);
    assertEquals( "[0, null, null, 11, 9, null, 17]", list.toString() );
  }
  
  public void testGet() {
    
    RandomAccessArrayList<Integer> list = new RandomAccessArrayList<Integer>(Arrays.asList(1,2,3));
    
    assertEquals( "[1, 2, 3]", list.toString() );
    
    assertEquals( Integer.valueOf(2), list.get(1) );
    
    assertNull( list.get(10));
    
   
  }


  
}
