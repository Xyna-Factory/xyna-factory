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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.Arrays;

import junit.framework.TestCase;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.MultiTimeWindow.MultiTimeWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.SimplePeriodicTimeWindow.SimplePeriodicTimeWindowDefinition;


public class TimeWindowDefinitionTest extends TestCase {

  public void testSimplePeriodic() {
    
    SimplePeriodicTimeWindowDefinition sp = new SimplePeriodicTimeWindowDefinition("%60+10", 10);
    
    assertEquals("TimeWindowDefinition(SimplePeriodic,%60+10,10)", sp.toString());
    assertEquals("SimplePeriodic(%60+10,10)", sp.serializeToString() );
    
    TimeWindowDefinition sp2 = sp.deserializeFromString("SimplePeriodic(%40+15,5)");
    assertEquals("TimeWindowDefinition(SimplePeriodic,%40+15,5)", sp2.toString());
    
   
  }
  
  public void testTimeWindowDefinition() {
    TimeWindowDefinition sp = TimeWindowDefinition.valueOf("SimplePeriodic(%60+10,10)");
    assertEquals("TimeWindowDefinition(SimplePeriodic,%60+10,10)", sp.toString());

    try {
      TimeWindowDefinition sp2 = TimeWindowDefinition.valueOf("Unknown(%60+10,10)");
      fail("IllegalArgumentException expected");
      System.out.println( sp2.toString() );
    } catch( IllegalArgumentException e ) {
      assertEquals("Unknown TimeWindowDefinition-Type \"Unknown\", known: [SimplePeriodic]", e.getMessage() );
    }
     
  }
  
  public void testMulti() {
    SimplePeriodicTimeWindowDefinition sp = new SimplePeriodicTimeWindowDefinition("%60+10", 10);
    SimplePeriodicTimeWindowDefinition sp2 = new SimplePeriodicTimeWindowDefinition("%40+15",5);
    
    MultiTimeWindowDefinition multi = MultiTimeWindowDefinition.construct( Arrays.asList(sp, sp2) );
    assertEquals("TimeWindowDefinition(Multi,2)", multi.toString());
    assertEquals("Multi(2,"+sp.serializeToString()+","+sp2.serializeToString()+")", multi.serializeToString());
    
    TimeWindowDefinition multi2 = multi.deserializeFromString(multi.serializeToString());
    assertEquals("TimeWindowDefinition(Multi,2)", multi2.toString());
    
    assertEquals(multi.serializeToString(), multi2.serializeToString());
  }
  
}
