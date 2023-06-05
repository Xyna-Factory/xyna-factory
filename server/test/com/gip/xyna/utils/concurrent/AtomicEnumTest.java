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
package com.gip.xyna.utils.concurrent;

import junit.framework.TestCase;


public class AtomicEnumTest extends TestCase {

  
  private static enum TestEnum {A,B,C};
  
  
  public void testGet() {
    AtomicEnum<TestEnum> ae = new AtomicEnum<TestEnum>(TestEnum.class, TestEnum.B);
    
    TestEnum b = ae.get();
    assertEquals( TestEnum.B, b);
  }

  
  public void testSet() {
    AtomicEnum<TestEnum> ae = new AtomicEnum<TestEnum>(TestEnum.class);
    
    ae.set(TestEnum.C);
    assertEquals( TestEnum.C, ae.get());
  }
  
  
  public void testGetAndSet() {
    AtomicEnum<TestEnum> ae = new AtomicEnum<TestEnum>(TestEnum.class, TestEnum.B);
    
    TestEnum b = ae.getAndSet(TestEnum.A);
    assertEquals(TestEnum.B, b);
    assertEquals(TestEnum.A, ae.get());
  }
  
  
  public void testCompareAndSet() {
    AtomicEnum<TestEnum> ae = new AtomicEnum<TestEnum>(TestEnum.class, TestEnum.B);
    
    boolean b1 = ae.compareAndSet(TestEnum.B, TestEnum.A);
    assertEquals( true, b1 );
    
    boolean b2 = ae.compareAndSet(TestEnum.B, TestEnum.A);
    assertEquals( false, b2 );
    
    assertEquals( TestEnum.A, ae.get() );
  }
  
  
}
