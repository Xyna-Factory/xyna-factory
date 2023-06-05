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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.PartitionedList.Transformator;


public class PartitionedListTest extends TestCase {
  
  private static class A {
   
  }
  
  private static class B {
    
  }
  
  public void testAddRemoveIter() {
    PartitionedList<A, B> list = new PartitionedList<A, B>(new Transformator<A, B>() {

      public B transformTo(A object) {
        return new B();
      }

      public B transformTemporarily(A object) {
        return new B();
      }

      public A transformBack(B id) {
        return new A();
      }
      
    }, 1000);
    Random r = new Random();
    for (int i = 0; i<1000; i++) {
      list.add(new A());
    }
    ListIterator<A> iter = list.listIterator();
    for (int i = 0; i<1000; i++) {
      if (r.nextBoolean()) {
        if (iter.hasNext()) {
          iter.next();
        } else {
          iter.previous();
        }
      } else {
        if (iter.hasPrevious()) {
          iter.previous();
        } else {
          iter.next();
        }
      }

      int type;
      //0 => add
      //1 => remove
      if (list.size() > 0) {
        type = r.nextInt(2);
      } else {
        type = r.nextInt(1);
      }
      if (type == 0) {
        iter.add(new A());
        if (list.size() < 600) {
          if (r.nextInt(10) < 3) {
            iter.previous();
            iter.add(new A());
          }
        }
      } else {
        iter.remove();
        if (list.size() > 1100) {
          if (r.nextInt(10) < 3) {
            iter.previous();
            iter.remove();
          }
        }
      }
      if (i % 10000 == 0) {
        System.out.println(list.size());
      }
    }
  }
  
  private static class A1 {
    public A1(B1 id) {
      this.n = id.n;
    }

    public A1(long n) {
      this.n = n;
    }

    private long n;
  }
  
  private static class B1 {
    public B1(A1 object) {
      this.n = object.n;
    }

    private long n;
  }
  
  public void testKeepSorted() {
    PartitionedList<A1, B1> list = new PartitionedList<A1, B1>(new Transformator<A1, B1>() {

      public B1 transformTo(A1 object) {
        return new B1(object);
      }

      public B1 transformTemporarily(A1 object) {
        return new B1(object);
      }

      public A1 transformBack(B1 id) {
        return new A1(id);
      }
      
    }, 1000);
    
    Random random = new Random();
    for (int i = 0; i<950; i++) {
      list.add(new A1(random.nextInt(i+1)));
    }
    Collections.sort(list, new Comparator<A1>() {

      public int compare(A1 o1, A1 o2) {
        return (int) (o1.n - o2.n);
      }
      
    });
    
    for (int i = 1000; i<11000; i++) {
      int t = random.nextInt(3);
      if (t == 0) {
        list.add(new A1(i));
      } else if (t == 1) {
        list.add(0, new A1(-i));
      } else if (t == 2) {
        if (list.size() > 0) {
          if (list.size() > 760) {
            list.remove(random.nextInt(list.size()));
          }
          if (list.size() > 1100) {
            list.remove(random.nextInt(list.size()));
          }
          list.remove(random.nextInt(list.size()));
        }
      }
      checkSorted(list);
    }

    for (int i = 0; i<1000; i++) {
      list.add(new A1(random.nextInt(1000000)));
    }
    Collections.sort(list, new Comparator<A1>() {

      public int compare(A1 o1, A1 o2) {
        return (int) (o1.n - o2.n);
      }
      
    });
    checkSorted(list);
  }

  private void checkSorted(PartitionedList<A1, B1> list) {
    System.out.println(list.size());
    Iterator<A1> iter = list.iterator();
    long last = Long.MIN_VALUE;
    while (iter.hasNext()) {
      A1 next = iter.next();
      if (next.n < last) {
        fail();
      }
      last = next.n;
    }
  }
  
  public void testKeepSorted2() {
    
  }
  
  public void testRemove() {
    PartitionedList<String,String> list = new PartitionedList<String,String>(new Transformator<String,String>() {

      public String transformTo(String object) {
        return object;
      }

      public String transformTemporarily(String object) {
        return object;
      }

      public String transformBack(String id) {
        return id;
      }
      
    }, 2);
    
    list.add("A");
    list.add("B");
    list.add("C");
    list.add("D");
    list.add("E");
    list.add("F");
    System.out.println( list);
    
    list.remove("B");
    System.out.println( list);
    
    list.removeAll( Arrays.asList( new String[]{"A","E"} ) );
    System.out.println( list);
  }

  
  private static class MutableInt {

    int value;

    public MutableInt(MutableInt mutableInt) {
      this.value = mutableInt.value;
    }
    public MutableInt(int value) {
      this.value = value;
    }
    
    @Override
    public String toString() {
      return String.valueOf(value);
    }
    /*
    @Override
    public boolean equals(Object obj) {
      if( obj instanceof MutableInt ) {
        return value == ((MutableInt)obj).value;
      }
      return false;
    }*/
  }
  
  public void testRemove2() {
    PartitionedList<MutableInt,MutableInt> list = new PartitionedList<MutableInt,MutableInt>(new Transformator<MutableInt,MutableInt>() {

      public MutableInt transformTo(MutableInt object) {
        return new MutableInt(object.value +1000);
      }

      public MutableInt transformTemporarily(MutableInt object) {
        return new MutableInt(object);
      }

      public MutableInt transformBack(MutableInt id) {
        return new MutableInt(id.value -1000 );
      }
      
    }, 2);
    
    list.add( new MutableInt(1) );
    list.add( new MutableInt(2) );
    list.add( new MutableInt(3) );
    list.add( new MutableInt(4) );
    list.add( new MutableInt(5) );
    list.add( new MutableInt(6) );
    System.out.println( list);
    System.out.println( list.get(1) );
    
    list.remove( list.get(1) );
    System.out.println( list);
    
  }

 
  
  
  
}
