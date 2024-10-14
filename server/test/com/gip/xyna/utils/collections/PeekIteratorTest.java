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
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class PeekIteratorTest extends TestCase {

  @Before
  protected void setUp() {}
  
  protected void tearDown() {}
  
  private ArrayList<String> createList(String ... strings) {
    return new ArrayList<String>(Arrays.asList(strings));
  }

  @Test
  public void testIterate() {
    List<String> list = createList("A","B");
    PeekIterator<String> iter = new PeekIterator<String>(list.iterator());
    
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.next() );
    Assert.assertFalse(iter.hasNext() );
  }

  @Test
  public void testPeek() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.iterator());
    
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.peek() );
    Assert.assertEquals("A", iter.peek() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.next() );
    Assert.assertEquals("C", iter.peek() );
    Assert.assertEquals("C", iter.peek() );
    Assert.assertEquals("C", iter.next() );
    Assert.assertFalse(iter.hasNext() );
  }

  @Test
  public void testRemoveUnsupported() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.iterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    
    try {
      iter.remove();
      Assert.fail("Exception expected");
    } catch( UnsupportedOperationException e ) {
      
    }
  }
  
  @Test
  public void testPeekFailedAfterLastEntry() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.next() );
    Assert.assertEquals("C", iter.next() );
    
    Assert.assertFalse(iter.hasNext() );
    
    try {
      iter.peek();
      Assert.fail("Exception expected");
    } catch( NoSuchElementException e ) { 
    }
    
  }
  
  @Test
  public void testRemoveWithPeek() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.peek() );
    
    iter.remove();
    Assert.assertEquals("[B, C]", list.toString() );
  }
 
  @Test
  public void testRemoveFailedWhenOnlyPeeked() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.peek() );
    try {
      iter.remove();
      Assert.fail("Exception expected");
    } catch( NoSuchElementException e ) { 
    }
    
  }
  
  @Test
  public void testRemoveEnd() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.next() );
    Assert.assertEquals("C", iter.next() );
    
    iter.remove();
    Assert.assertEquals("[A, B]", list.toString() );
  
  }
  
  @Test
  public void testRemoveMiddle() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    Assert.assertEquals("B", iter.next() );
    
    iter.remove();
    Assert.assertEquals("[A, C]", list.toString() );
  
  }
  
  @Test
  public void testRemoveFirst() {
    List<String> list = createList("A","B","C");
    PeekIterator<String> iter = new PeekIterator<String>(list.listIterator());
      
    Assert.assertTrue(iter.hasNext() );
    Assert.assertEquals("A", iter.next() );
    
    iter.remove();
    Assert.assertEquals("[B, C]", list.toString() );
  
  }
 
}
