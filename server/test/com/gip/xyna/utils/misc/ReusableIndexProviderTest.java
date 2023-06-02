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
package com.gip.xyna.utils.misc;

import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;

public class ReusableIndexProviderTest extends TestCase {
  
  public void test1() {
    ReusableIndexProvider rip = new ReusableIndexProvider(1.5f, 10);
    Random r = new Random();
    HashSet<Integer> els = new HashSet<Integer>();
    int max = -1;
    for (int i = 0; i<100000; i++) {
      if (r.nextFloat() < 0.001) {
        for (Integer idx : els) {
          rip.returnIdx(idx);
        }
        els.clear();
      }
      if (els.size() < 100) {
        int idx = rip.getNextFreeIdx();
        if (idx > max) {
          max = idx;
        }
        assertFalse(els.contains(idx));
        els.add(idx);
      }
      if (r.nextBoolean()) {
        //remove
        int idx = (Integer)els.toArray()[r.nextInt(els.size())];
        if (!els.remove(idx)) {
          throw new RuntimeException();
        }
        rip.returnIdx(idx);
      } else {
        //add
        int idx = rip.getNextFreeIdx();
        if (idx > max) {
          max = idx;
        }
        assertFalse(els.contains(idx));
        els.add(idx);
      }
      
    }
    assertTrue(max < 500);
    System.out.println("finished: " + max);
  }

}
