/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.misc;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.DataRangeCollection.DataSource;

import junit.framework.TestCase;



public class DataRangeCollectionTest extends TestCase {

  public void test1() {
    final long[] dps = new long[] {3, 8, 9};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });

    dc.insertDataPoints(0, 0);
    dc.insertDataPoints(3, 4);
    dc.insertDataPoints(2, 5);
    dc.insertDataPoints(0, 7);
    dc.insertDataPoints(15, 20);


    assertFalse(dc.hasDataPoints(0, 0));
    assertTrue(dc.hasDataPoints(3, 3));
    assertTrue(dc.hasDataPoints(2, 6));
    assertFalse(dc.hasDataPoints(4, 7));
    assertFalse(dc.hasDataPoints(15, 20));

    try {
      assertFalse(dc.hasDataPoints(10, 12));
      fail();
    } catch (RuntimeException e) {
    }

    dc.insertDataPoints(9, 12);
    assertFalse(dc.hasDataPoints(10, 12));
    assertTrue(dc.hasDataPoints(9, 12));
  }


  public void testValues() {
    final long[] dps = new long[] {3, 8, 9};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });
    /*
     * Value:   555888881111199999
     *             |    |    |
     *          ------------------
     *             3    8    9
     */
    dc.updateInterval(5);
    assertEquals(0, dc.getValue(5));
    dc.setValue(5, 8);
    assertEquals(8, dc.getValue(5));
    dc.updateInterval(7);
    assertEquals(8, dc.getValue(7));
    dc.updateInterval(7);
    assertEquals(8, dc.getValue(7));
    assertEquals(8, dc.getValue(6));

    dc.updateInterval(2);
    assertEquals(0, dc.getValue(2));
    assertEquals(8, dc.getValue(6));
    assertEquals(8, dc.getValue(5));
    dc.setValue(2, 5);
    assertEquals(5, dc.getValue(2));
    assertEquals(8, dc.getValue(6));
    assertEquals(8, dc.getValue(7));

    dc.updateInterval(4);
    assertEquals(5, dc.getValue(2));
    assertEquals(8, dc.getValue(4));
    assertEquals(8, dc.getValue(5));

    dc.updateInterval(3);
    assertTrue(dc.hasDataPoints(3, 5));
    assertFalse(dc.hasDataPoints(4, 5));

    dc.insertDataPoints(2, 4);
    dc.insertDataPoints(2, 6);
    dc.insertDataPoints(2, 9);
    dc.insertDataPoints(2, 11);
    dc.setValue(11, 9);
    assertEquals(9, dc.getValue(9));
    assertEquals(0, dc.getValue(8));
  }
  
  public void testMerge() {
    final long[] dps = new long[] {2, 5, 8};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });
    /*
     * Value:   000111113333322222
     *             |    |    |
     *          ------------------
     *             2    5    8
     */
    dc.insertDataPoints(0, 4);
    dc.setValue(3, 1);
    dc.insertDataPoints(6, 9);
    dc.setValue(7, 3);    
    dc.insertDataPoints(5, 5);
    assertTrue(dc.hasDataPoints(5, 5));
    assertFalse(dc.hasDataPoints(6, 6));
    assertEquals(3, dc.getValue(5));
    assertEquals(1, dc.getValue(2));
  }
  

  public void testDPCollection() {
    final long[] dps = new long[] {3, 8, 9};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });

    dc.insertDataPoints(2, 4);
    dc.insertDataPoints(2, 6);
    dc.insertDataPoints(2, 9);
    dc.insertDataPoints(2, 11);

    Set<Long> set = new HashSet<Long>();
    dc.collectExistingDataPoints(2, 8, set);
    assertEquals(2, set.size());
    assertTrue(set.contains(3L));
    assertTrue(set.contains(8L));

    dc.insertDataPoints(11, 15);

    set.clear();
    dc.collectExistingDataPoints(8, 15, set);
    assertEquals(2, set.size());
    assertTrue(set.contains(8L));
    assertTrue(set.contains(9L));
  }
  

  public void testBug19639() {
    final long[] dps = new long[] {0,1,2,3,5,6,7};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        if (start > end) {
          throw new IllegalArgumentException();
        }
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });

    dc.insertDataPoints(1, 1);
    dc.insertDataPoints(3, 3);
    dc.insertDataPoints(7, 7);
    dc.insertDataPoints(1, 8);
    dc.insertDataPoints(4, 5);
    
    dc.hasDataPoints(2, 3);
  }
  
  public void testBug19639b() {
    final long[] dps = new long[] {0,1,2,3,5,6,7};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        if (start > end) {
          throw new IllegalArgumentException();
        }
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });

    dc.insertDataPoints(1, 1);
    dc.insertDataPoints(2, 2);
    dc.insertDataPoints(7, 7);
    dc.insertDataPoints(6, 6);
    dc.insertDataPoints(1, 8);
    dc.insertDataPoints(4, 5);
   
    dc.hasDataPoints(2, 3);
  }
  
  
  public void testBug19639c() {
    final long[] dps = new long[] {0,1,2,3,5,6,7};
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {

      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        if (start > end) {
          throw new IllegalArgumentException();
        }
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });

    dc.insertDataPoints(1, 1);
    dc.updateInterval(3);
    dc.updateInterval(4);
   
    dc.hasDataPoints(2, 4);

    dc.updateInterval(-3);
    dc.updateInterval(-1);
    dc.updateInterval(-4);
    dc.insertDataPoints(0, 0);
    
    dc.hasDataPoints(-1, 2);
  }
  
  
  private DataRangeCollection createDC(Random r, int max, final List<Pair<Integer, Integer>> currentPairs) {
    Set<Long> dpset = new HashSet<Long>();
    int n = r.nextInt(1000);
    for (int i = 0; i<n; i++) {
      dpset.add((long)r.nextInt(max));
    }
    final long[] dps = new long[n];
    int i = 0;
    List<Long> list = new ArrayList<Long>(dpset);
    Collections.sort(list);
    for (Long l : list) {
      dps[i++] = l;
    }
    DataRangeCollection dc = new DataRangeCollection(new DataSource() {
      public void addDataPoints(long start, long end, Set<Long> datapoints) {
        if (start > end) {
          for (Pair<Integer, Integer> pair : currentPairs) {
            System.out.println(pair.getFirst() + ", " + pair.getSecond());
          }
          throw new IllegalArgumentException();
        }
        for (long dp : dps) {
          if (dp >= start && dp <= end) {
            datapoints.add(dp);
          }
        }
      }
    });
    return dc;
  }
  
  
  public void testRandomIntervall() {
    final List<Pair<Integer, Integer>> currentPairs = new ArrayList<Pair<Integer, Integer>>();
    Random rand = new Random();
    DataRangeCollection dc = createDC(rand, 10000, currentPairs);
    for (int i = 0; i < 100000; i++) {
      int one;
      int two;
      if (rand.nextInt(10) == 0) {
        one = rand.nextInt(10000);
      } else {
        one = rand.nextInt(100);
      }
      if (rand.nextInt(10) == 0) {
        two = rand.nextInt(10000);
      } else {
        two = rand.nextInt(100);
      }
      currentPairs.add(Pair.of(Math.min(one, two), Math.max(one, two)));
      dc.insertDataPoints(Math.min(one, two), Math.max(one, two));
      dc.check();
      if (rand.nextInt(20) == 0) {
        dc = createDC(rand, 10000, currentPairs);
        currentPairs.clear();
      }
    }
  }
  
  

}
