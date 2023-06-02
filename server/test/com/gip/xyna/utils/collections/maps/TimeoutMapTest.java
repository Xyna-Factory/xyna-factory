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
package com.gip.xyna.utils.collections.maps;

import junit.framework.TestCase;

public class TimeoutMapTest extends TestCase {
  
  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //dann halt kürzer
    }
  }

  public void testFirst() {
    TimeoutMap<String,String> tm = new TimeoutMap<String,String>();
    
    tm.put("a", "apfel", 100);
    tm.put("b", "banane", 200);
    
    assertEquals( "{a=apfel, b=banane}" , tm.toString() );
    
    
    sleep(150);
    
    assertEquals( "{b=banane}" , tm.toString() );
    
    
    sleep(150);
    
    
    assertEquals( "{}" , tm.toString() );
    
  }
  
}
