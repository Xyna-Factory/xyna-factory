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
package com.gip.xyna.utils.collections;

import org.junit.Assert;

import junit.framework.TestCase;

public class ArrayRingBufferTest extends TestCase {

  public void test1() {
    ArrayRingBuffer<String> rb = new ArrayRingBuffer<>(3);
    Assert.assertArrayEquals(new String[]{}, rb.getOrdered(new String[0]));
    rb.add("1");
    Assert.assertArrayEquals(new String[]{"1"}, rb.getOrdered(new String[0]));
    rb.add("2");
    Assert.assertArrayEquals(new String[]{"1", "2"}, rb.getOrdered(new String[0]));
    rb.add("3");
    Assert.assertArrayEquals(new String[]{"1", "2", "3"}, rb.getOrdered(new String[0]));
    rb.add("4");
    Assert.assertArrayEquals(new String[]{"2", "3", "4"}, rb.getOrdered(new String[0]));
    rb.add("5");
    Assert.assertArrayEquals(new String[]{"3", "4", "5"}, rb.getOrdered(new String[0]));
    rb.add("6");
    Assert.assertArrayEquals(new String[]{"4", "5", "6"}, rb.getOrdered(new String[0]));
    rb.add("7");
    Assert.assertArrayEquals(new String[]{"5", "6", "7"}, rb.getOrdered(new String[0]));
    
  }
  
}
