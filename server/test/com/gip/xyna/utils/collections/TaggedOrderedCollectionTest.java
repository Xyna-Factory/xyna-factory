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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Assert;

import com.gip.xyna.utils.collections.PartitionedList.Transformator;
import com.gip.xyna.utils.collections.TaggedOrderedCollection.TaggedElementsListCreator;



/**
 *
 */
public class TaggedOrderedCollectionTest extends TestCase {

  private List<String> OBST = Arrays.asList(new String[]{"Apfel", "Birne", "Clementine", "Dattel"});
  private List<String> ZEICHEN = Arrays.asList(new String[]{"Alpha", "Beta", "Gamma", "Delta"});
  
  
  private TaggedOrderedCollection<String> createTOC(String ... strings) {
    return createTOC( Arrays.asList(strings) );
  }
  
  private TaggedOrderedCollection<String> createTOC(List<String> entries) {
    TaggedOrderedCollection<String> toc = new TaggedOrderedCollection<String>();
    toc.addAll( entries );
    return toc;
  }
  
  private void tag(TaggedOrderedCollection<String> toc, List<String> entries, String tag) {
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      if( entries.contains(str) ) {
        iter.tag(tag);
      }
    }
  }

  
  public void testOrder() {
    
    TaggedOrderedCollection<String> toc = new TaggedOrderedCollection<String>();
    
    toc.add("B");
    toc.add("A");
    Assert.assertEquals("[A, B]", toc.toString() );
    Assert.assertEquals("{untagged=[A, B]}", toc.toString2() );
  }
  
  public void testTag() {
    TaggedOrderedCollection<String> toc = createTOC(OBST );
    Assert.assertEquals("[Apfel, Birne, Clementine, Dattel]", toc.toString() );
    toc.addAll( ZEICHEN );
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    
    tag(toc, OBST, "Obst");
    
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    Assert.assertEquals("{untagged=[Alpha, Beta, Delta, Gamma], Obst=[Apfel, Birne, Clementine, Dattel]}", toc.toString2() );
  
  }

  
  
  public void testGetTag() {
    TaggedOrderedCollection<String> toc = createTOC(OBST );
    
    Assert.assertEquals( "[]", toc.getTags().toString() );
    
    toc.addAll( ZEICHEN );
    Assert.assertEquals( "[]", toc.getTags().toString() );
    
    tag(toc, OBST, "Obst");
    
    Assert.assertEquals( "[Obst]", toc.getTags().toString() );
    
    tag(toc, ZEICHEN, "Zeichen");
    Assert.assertEquals( "[Obst, Zeichen]", toc.getTags().toString() );
    
  }

  
  
  public void testHideShow() {
    TaggedOrderedCollection<String> toc = createTOC(OBST );
    toc.addAll( ZEICHEN );
    tag(toc, OBST, "Obst");
    
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    
    toc.hide("Obst");
    Assert.assertEquals("[Alpha, Beta, Delta, Gamma]", toc.toString() );
    
    toc.show("Obst");
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    
    toc.hide(null);
    Assert.assertEquals("[Apfel, Birne, Clementine, Dattel]", toc.toString() );
    
    toc.show(null);
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    
    toc.hide("Obst");
    toc.hide(null);
    
    Assert.assertEquals("[]", toc.toString() );
    
  }

  public void testIteratorHide() {
    TaggedOrderedCollection<String> toc = createTOC(OBST );
    toc.addAll( ZEICHEN );
    tag(toc, OBST, "Obst");
    tag(toc, ZEICHEN, "Zeichen");
    
    Assert.assertEquals("{untagged=[], Obst=[Apfel, Birne, Clementine, Dattel], Zeichen=[Alpha, Beta, Delta, Gamma]}", toc.toString2() );

    
    ArrayList<String> iteratedStrings = new ArrayList<String>();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      iteratedStrings.add(str);
      if( str.equals("Birne") ) {
        iter.hide("Obst");
      }
    }
    
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Delta, Gamma]", iteratedStrings.toString() );
    
    iteratedStrings.clear();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      iteratedStrings.add(str);
      if( str.equals("Alpha") ) {
        iter.hide("Zeichen");
      }
    }
    
    Assert.assertEquals("[Alpha, Apfel, Birne, Clementine, Dattel]", iteratedStrings.toString() );
    
  }
  
  
  public void testTagAndHide() {
    
    TaggedOrderedCollection<String> toc = new TaggedOrderedCollection<String>();
    toc.add("A1"); toc.add("A2"); toc.add("A3"); 
    toc.add("B1"); toc.add("B2"); toc.add("B3"); 
    toc.add("C1"); toc.add("C2"); toc.add("C3"); 
    toc.add("D1"); toc.add("D2"); toc.add("D3"); 
    
    Assert.assertEquals("[A1, A2, A3, B1, B2, B3, C1, C2, C3, D1, D2, D3]", toc.toString() );
   
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      if( str.equals("A2") ) {
        iter.tag("2");
      }
      if( str.equals("C2") ) {
        iter.tag("2");
      }
      if( str.equals("D2") ) {
        iter.tag("2");
      }
    }
    //System.err.println( toc.toString2() );
    
    Assert.assertEquals("{untagged=[A1, A3, B1, B2, B3, C1, C3, D1, D3], 2=[A2, C2, D2]}", toc.toString2() );
    ArrayList<String> iteratedStrings = new ArrayList<String>();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      iter.hide("2");
      String str = iter.next();
      if( str.equals("B2") ) {
        iter.tag("2");
      }
      iteratedStrings.add(str);
    }
    Assert.assertEquals("[A1, A3, B1, B2, B3, C1, C3, D1, D3]", iteratedStrings.toString() );
    
    //System.err.println( toc.toString2() );
    Assert.assertEquals("{untagged=[A1, A3, B1, B3, C1, C3, D1, D3], 2=[A2, B2, C2, D2]}", toc.toString2() );
    
  }
  
  public void testRetag() {
  
    TaggedOrderedCollection<String> toc = new TaggedOrderedCollection<String>();
    toc.add("A1"); toc.add("A2"); toc.add("A3"); 
    toc.add("B1"); toc.add("B2"); toc.add("B3"); 
    toc.add("C1"); toc.add("C2"); toc.add("C3"); 
    toc.add("D1"); toc.add("D2"); toc.add("D3");
    
    Assert.assertEquals("[A1, A2, A3, B1, B2, B3, C1, C2, C3, D1, D2, D3]", toc.toString() );
    
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      if( str.endsWith("1")) {
        iter.tag("1");
      }
      if( str.endsWith("3")) {
        iter.tag("3");
      }
    }
    //System.err.println( toc.toString2() );
    Assert.assertEquals("{untagged=[A2, B2, C2, D2], 3=[A3, B3, C3, D3], 1=[A1, B1, C1, D1]}", toc.toString2() );
    
    ArrayList<String> iteratedStrings = new ArrayList<String>();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      if( str.equals("A1") ) {
        iter.tag("3");
      }
      if( str.equals("B2") ) {
        iter.tag("3");
      }
      if( str.equals("C3") ) {
        iter.tag("1");
      }
      if( str.equals("D2") ) {
        iter.tag("1");
      }
      iteratedStrings.add(str);
    }
    
    Assert.assertEquals("[A1, A2, A3, B1, B2, B3, C1, C2, C3, D1, D2, D3]", iteratedStrings.toString() );
    
    Assert.assertEquals("{untagged=[A2, C2], 3=[A1, A3, B2, B3, D3], 1=[B1, C1, C3, D1, D2]}", toc.toString2() );
   
  }
  
  public void testTagAgain() {
    
    TaggedOrderedCollection<String> toc = createTOC(OBST );
    toc.addAll( ZEICHEN );
    
    
    ArrayList<String> iteratedStrings = new ArrayList<String>();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      iteratedStrings.add(str);
    }
    Assert.assertEquals("[Alpha, Apfel, Beta, Birne, Clementine, Dattel, Delta, Gamma]", toc.toString() );
    
    tag(toc, OBST, "Obst");
    tag(toc, ZEICHEN, "Zeichen");
    
    Assert.assertEquals("{untagged=[], Obst=[Apfel, Birne, Clementine, Dattel], Zeichen=[Alpha, Beta, Delta, Gamma]}", toc.toString2() );

    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String str = iter.next();
      iter.tag(str.substring(0,1) );
    }
    
    Assert.assertEquals("{untagged=[], D=[Dattel, Delta], Obst=[], A=[Alpha, Apfel], C=[Clementine], B=[Beta, Birne], Zeichen=[], G=[Gamma]}", toc.toString2() );

    toc.removeEmptyTagLists();
    Assert.assertEquals("{untagged=[], D=[Dattel, Delta], A=[Alpha, Apfel], C=[Clementine], B=[Beta, Birne], G=[Gamma]}", toc.toString2() );

  }
  
  
  static Random random = new Random();
  private static String createRandomString() {
    int length = random.nextInt(10)+2;
    StringBuilder sb = new StringBuilder(length);
    for( int i=0; i<length; ++i ) {
      sb.append( "abcdefghijklmnopqrstuvwxyz".charAt(random.nextInt(26)) );
    }
    return sb.toString();
  }

  
  public void testPerformance() {
    ArrayList<String> al = new ArrayList<String>();
    LinkedList<String> ll = new LinkedList<String>();
    final TaggedOrderedCollection<String> toc = new TaggedOrderedCollection<String>();
    long start, end;
    
    int testSize = 1*1000*1000;
    
    start = System.currentTimeMillis();
    for( int i=0; i<testSize; ++i ) {
      al.add( createRandomString() );
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to create "+testSize+" strings" );
        
    start = System.currentTimeMillis();
    ll.addAll(al);
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to add all string to LinkedList" );
    
    start = System.currentTimeMillis();
    toc.addAll(al);
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to add all string to TOC" );

    
    start = System.currentTimeMillis();
    TaggedOrderedCollection<String>.Iterator iter1 = toc.iterator();
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to create TOC.iterator (has to sort all entries)" );
    
    start = System.currentTimeMillis();
    TaggedOrderedCollection<String>.Iterator iter2 = toc.iterator();
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to create TOC.iterator again (no sort needed)" );
    
    
    
    start = System.currentTimeMillis();
    for( Iterator<String> iter = al.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over ArrayList and extract second char" );
    
    start = System.currentTimeMillis();
    for( Iterator<String> iter = ll.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over LinkedList and extract second char" );
    
    start = System.currentTimeMillis();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over TOC and extract second char" );
    
    start = System.currentTimeMillis();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
      iter.tag(secondChar);
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over TOC and extract and tag second char" );
    
    start = System.currentTimeMillis();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      if( entry.length() > 2 ) {
        String thirdChar = entry.substring(2,3);
        iter.tag(thirdChar);
      } else {
        iter.tag(null);
      }
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over TOC and extract and tag third char" );

    start = System.currentTimeMillis();
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
      iter.tag(secondChar);
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over TOC and extract and tag second char" );
    
    start = System.currentTimeMillis();
    int count =0;
    for( TaggedOrderedCollection<String>.Iterator iter = toc.iterator(); iter.hasNext(); ) {
      String entry = iter.next();
      String secondChar = entry.substring(1,2);
      iter.tag(secondChar);
      iter.hide(secondChar);
      ++count;
    }
    end = System.currentTimeMillis();
    System.out.println( (end-start)+" ms to iterate over TOC and extract and tag and hide second char ->"+count );
    
  }

  private static class Long2 implements Comparable<Long2> {
    private long l;

    public Long2(long m) {
      l = m;
    }

    public int compareTo(Long2 o) {
      if (o.l == l) {
        return 0;
      }
      return o.l < l ? -1 : 1;
    }
    
    public String toString() {
      return "" + l;
    }
    
  }

  public void testSorted() {
    TaggedOrderedCollection<Long2> list = new TaggedOrderedCollection<Long2>(new TaggedElementsListCreator<Long2>() {

      public List<Long2> createList() {
        return new PartitionedList<Long2, Long2>(new Transformator<Long2, Long2>(){

          public Long2 transformBack(Long2 id) {
            return id;
          }

          public Long2 transformTemporarily(Long2 o) {
            return o;
          }

          public Long2 transformTo(Long2 o) {
            return o;
          }
          
        }, 10);
      }
      
    }, null);
    for (int k = 0; k < 3; k++) {
      System.out.println("");
      Random r = new Random();
      for (int i = 0; i < 1000; i++) {
        if (r.nextBoolean()) {
          list.add(new Long2((long) (k*1000 + i)));
        } else {
          list.add(new Long2((long) -(k*1000 + i)));
        }
      }

      TaggedOrderedCollection<Long2>.Iterator iter = list.iterator();
      for (int i = 0; i < list.size(); i++) {
        if (!iter.hasNext()) {
          break;
        }
        iter.next();
        if (r.nextInt(5) == 0) {
          continue;
        }
        int t = r.nextInt(5);
        iter.tag("" + t);
        if( t == k ) {
          iter.hide("" + t);
        }
      }

      iter = list.iterator();
      long old = Long.MAX_VALUE;      
      while (iter.hasNext()) {
        Long2 next = iter.next();
        //System.out.println(next.l);
        if (old < next.l) {
          System.out.println("bla "+old+ "<"+next.l);
        }
        old = next.l;
      }
      System.out.println(list.toString());
      System.out.println(list.toString2());
    }
  }
  
}
