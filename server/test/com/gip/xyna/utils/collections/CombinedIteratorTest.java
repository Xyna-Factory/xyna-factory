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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class CombinedIteratorTest extends TestCase {

  public void test1() {
    List<String> l1 = Arrays.asList(new String[]{"1", "2"});
    List<String> l2 = Arrays.asList(new String[]{"3"});
    List<String> l3 = Arrays.asList(new String[]{});
    List<String> l4 = Arrays.asList(new String[]{"4", "5", "6"});
    List<String> l5 = Arrays.asList(new String[]{});
    List<String> l6 = Arrays.asList(new String[]{});
    List<String> l7 = Arrays.asList(new String[]{"7"});
    CombinedIterator<String> s = new CombinedIterator<String>(l1.iterator(), l2.iterator(), l3.iterator(), l4.iterator(), l5.iterator(), l6.iterator(), l7.iterator());
    StringBuilder sb = new StringBuilder();
    while (s.hasNext()) {
      sb.append(s.next());
    }
    assertEquals("1234567", sb.toString());
  }
  
}
