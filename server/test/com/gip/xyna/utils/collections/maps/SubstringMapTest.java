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

package com.gip.xyna.utils.collections.maps;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.SubstringMap;

import junit.framework.TestCase;



public class SubstringMapTest extends TestCase {

  public void test1() {
    SubstringMap m = new SubstringMap(true);
    m.add("abcde");
    m.add("bcdef");
    m.add("CDEFG");
    Collection<String> s = m.getSuperStrings("bc");
    assertTrue(s.contains("abcde"));
    assertTrue(s.contains("bcdef"));
    assertFalse(s.contains("CDEFG"));
    m.remove("abcde");
    s = m.getSuperStrings("ab");
    assertEquals(0, s.size());

    s = m.getSuperStrings("fg");
    assertEquals(0, s.size());

    m = new SubstringMap(false);
    m.add("abcde");
    m.add("bcdef");
    m.add("CDEFG");

    s = m.getSuperStrings("CD");
    assertEquals(3, s.size());
    s = m.getSuperStrings("CD");
    m.remove("cdefg");
    assertEquals(3, s.size());
    m.remove("CDEFG");
    s = m.getSuperStrings("CD");
    assertEquals(2, s.size());
  }
  
  private static class SubstringMap2 {

    public SubstringMap2(boolean casesens, int i, float f, float g, int j, int k, int l) {
    }
    
    private final char SPECIAL= '�';
    private StringBuilder sb = new StringBuilder();
    public Collection<String> getSuperStrings2(String string) {
      Pattern p = Pattern.compile("(?:^|" +SPECIAL +")(\\Q" + string + "\\E)" + SPECIAL);
      Matcher m = p.matcher(sb.toString());
      List<String> result = new ArrayList<String>();
      while (m.find()) {
        result.add(m.group(1));
      }
      return result;
    }
    public Collection<String> getSuperStrings(String string) {
      string = string.toLowerCase();
      List<String> result = new ArrayList<String>();
      int idx = 0;
      int len = sb.length();
      while (idx < len) {
        idx = sb.indexOf(string, idx);
        if (idx == -1) {
          break;
        }
        int m = idx;
        while (m>0 && sb.charAt(m) != SPECIAL) {
          m--;
        }
        int n = idx;
        while (n < len && sb.charAt(n) != SPECIAL) {
          n++;
        }
        result.add(sb.substring(m+1, n));
        idx = n+1;
      }

      return result;
    }

    public void add(String string) {
      sb.append(string.toLowerCase()).append(SPECIAL);
    }
    
  }


  public void test2() {
    for (int v = 0; v < 3; v++) {
      int n1 = 5000;
      int avglen = 30;
      int alphabetSize = 80;
      boolean casesens = false;
      long x = System.currentTimeMillis();
      System.out.println("x=" + x);
      Random r = new Random(x);
      String[] arr1 = new String[n1];
      Set<String> uniq = new HashSet<String>();

      for (int i = 0; i < n1; i++) {
        int l = r.nextInt(avglen * 2) + 1;
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < l; j++) {
          s.append((char) ('0' + r.nextInt(alphabetSize)));
        }
        while (!uniq.add(s.toString())) {
          s.append('i');
        }
        arr1[i] = s.toString();
      }

      long start = System.currentTimeMillis();

      // SubstringMap m = SubstringMap.create(casesens, n1, avglen, alphabetSize);
      SubstringMap m = new SubstringMap(casesens, 2, 2f, 0.95f, 100, 500, 16);
      // SubstringMap m = SubstringMap.create(casesens, Arrays.asList(arr1));

      long max = -1;
      for (int i = 0; i < n1; i++) {
        long t1 = System.currentTimeMillis();
        m.add(arr1[i]);
        long t2 = System.currentTimeMillis();
        if (t2 - t1 > max) {
          max = t2 - t1;
        }
      }
      System.out.println("add time=" + (System.currentTimeMillis() - start) + "ms (max=" + max + ")");
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
      }

      int cnt;


      for (int jj = 0; jj < 3; jj++) {
        int n = 2000;

        String[] arr = new String[n];

        for (int i = 0; i < n; i++) {
          int l = r.nextInt(avglen/2) + 1;
          StringBuilder s = new StringBuilder();
          for (int j = 0; j < l; j++) {
            s.append((char) ('0' + r.nextInt(alphabetSize)));
          }
          arr[i] = s.toString();
        }

        cnt = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
          cnt += m.getSuperStrings(arr[i]).size();
            /*    cnt = m.getSuperStrings(arr[i]).size();
             for (String s : arr1) {
          if (contains(s, arr[i], casesens)) {
            cnt--;
          }
             }
             if (cnt != 0) {
          if (true) {
            String p = arr[i];
            System.out.println("p=" + p + "      "  + m.getSuperStrings(p));
            for (String s : m.getSuperStrings(p)) {
              if (!contains(s, p, casesens)) {
                System.out.println("not: " + s);
              }
            }
            cnt = 0;
            for (String s : arr1) {
              if (contains(s, p, casesens)) {
                System.out.println(s);
                cnt++;
              }
            }
            System.out.println("cnt=" + cnt);
          }
             
             
          System.out.println(arr[i]);
          throw new RuntimeException();
             }*/
        }
        System.out.println(cnt + " time=" + (System.currentTimeMillis() - start) + "ms");


            start = System.currentTimeMillis();
        cnt = 0;
        for (int i = 0; i < n; i++) {
          String k = arr[i];
          if (!casesens) {
            k = k.toLowerCase();
          }
          for (String s : arr1) {
            if (casesens) {
              if (s.contains(k)) {
                cnt++;
              }
            } else {
              if (s.toLowerCase().contains(k)) {
                cnt++;
              }
            }
          }
        }
        System.out.println(cnt + " time=" + (System.currentTimeMillis() - start) + "ms");
        
      }
    }
  }


  public void testConcurrencyCorrectness() {
    final SubstringMap m = new SubstringMap(false, 2, 2f, 0.95f, 140, 400, 16);
    final TreeMap<Long, String> entries = new TreeMap<Long, String>();
    int threadcnt = 5;
    final CountDownLatch l = new CountDownLatch(threadcnt);
    long t0 = System.currentTimeMillis();
    for (int v = 0; v < threadcnt; v++) {
      final AtomicInteger size = new AtomicInteger(0);
      final int lenms = 30000;
      final int avglen = 30;
      final int alphabetSize = 10;
      final int x = 2000;
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          Random r = new Random();
          int i = 0;
          while (i < x) {
            i++;
            if (mustdeletefirst()) {
              if (i % 100 == 0) {
                System.out.println(entries.size());
              }
            } else if (r.nextBoolean()) {
              long time = System.currentTimeMillis() * 100;
              //read
              String val = null;
              synchronized (entries) {
                if (entries.size() == 0) {
                  continue;
                }
                long f = entries.firstKey();
                long l = entries.lastKey();
                if (l - f <= 0) {
                  continue;
                }
                f += r.nextInt((int) (l - f));
                Long k = entries.higherKey(f);
                if (k != null) {
                  val = entries.get(k);
                }
              }
              if (val != null) {
                int l = r.nextInt(val.length() - 2);
                List<String> ss = m.getSuperStrings(val.substring(l, Math.min(l + r.nextInt(10) + 1, val.length())));
                for (String s : ss) {
                  long t = Long.valueOf(s.substring(s.indexOf("_") + 1, s.length()));
                  if (t < time - lenms - 15000) {
                    synchronized (entries) {
                      if (entries.get(t) == null) {
                        System.out.println(time + ": " + s);
                      }                      
                    }
                  }
                }
              }
            } else if (r.nextFloat() < size.get() / 200000) {
              //remove
              String val = null;
              synchronized (entries) {
                long f = entries.firstKey();
                long l = entries.lastKey();
                f += r.nextInt((int) (l - f));
                Long k = entries.higherKey(f);
                if (k != null) {
                  val = entries.remove(k);
                }
              }
              if (val != null) {
                size.decrementAndGet();
                m.remove(val);
              }
            } else {
              //add
              int l = r.nextInt(avglen * 2) + 1;
              StringBuilder s = new StringBuilder();
              for (int j = 0; j < l; j++) {
                s.append((char) ('0' + r.nextInt(alphabetSize)));
              }
              long t = System.currentTimeMillis() * 100;              
              s.append("_").append(t);
              String val = s.toString();
              synchronized (entries) {
                while (entries.containsKey(t)) {
                  t++;
                }                
                entries.put(t, val);
              }
              size.incrementAndGet();
              m.add(val);
            }
          }
          l.countDown();
        }


        private boolean mustdeletefirst() {
          String val = null;
          synchronized (entries) {
            if (entries.size() == 0) {
              return false;
            }
            if (entries.firstKey() < System.currentTimeMillis()*100 - lenms) {
              val = entries.remove(entries.firstKey());
            }
          }
          if (val != null) {
            size.decrementAndGet();
            m.remove(val);
            return true;
          }
          return false;
        }

      });
      t.start();
    }

    try {
      l.await();
    } catch (InterruptedException e) {
    }
    System.out.println("took " + (System.currentTimeMillis() - t0) + "ms");
  }


  private boolean contains(String s, String t, boolean casesens) {
    if (casesens) {
      return s.contains(t);
    }
    return s.toLowerCase().contains(t.toLowerCase());
  }

}
