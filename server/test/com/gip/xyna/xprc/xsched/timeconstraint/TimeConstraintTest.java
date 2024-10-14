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
package com.gip.xyna.xprc.xsched.timeconstraint;

import junit.framework.TestCase;


public class TimeConstraintTest extends TestCase {

  
  public void testAbsRelTime1() {
    AbsRelTime art = new AbsRelTime(1000, true);
    String arts = art.serializeToString();
    assertEquals("rel:1000", arts );
    
    AbsRelTime art2 = AbsRelTime.valueOf(arts);
    assertEquals(1000L, art2.getTime() );
    assertTrue(art2.isRelative() );
    assertFalse(art2.isAbsolute() );
  }
  
  public void testAbsRelTime2() {
    long now = System.currentTimeMillis();
    AbsRelTime art = new AbsRelTime(now, false);
    String arts = art.serializeToString();
    assertEquals("abs:"+now, arts );
    
    AbsRelTime art2 = AbsRelTime.valueOf(arts);
    assertEquals(now, art2.getTime() );
    assertFalse(art2.isRelative() );
    assertTrue(art2.isAbsolute() );
  }
  
  
  
  
  public void testImmediatly() {
    TimeConstraint tc = TimeConstraint.immediately();
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:0)", tcString );
    assertEquals("TimeConstraint.immediately()", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.immediately()", tc2.toString() );
  }
  
  public void testDelayed() {
    TimeConstraint tc = TimeConstraint.delayed(1000);
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:1000)", tcString );
    assertEquals("TimeConstraint.delayed(1000)", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.delayed(1000)", tc2.toString() );
  }
  
  public void testAt() {
    long now = System.currentTimeMillis();
    TimeConstraint tc = TimeConstraint.at(now);
    String tcString = tc.serializeToString();
    
    assertEquals("start(abs:"+now+")", tcString );
    assertEquals("TimeConstraint.at("+now+")", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.at("+now+")", tc2.toString() );
  }
  
  public void testDelayedWithSchedulingTimeout() {
    TimeConstraint tc = TimeConstraint.delayed(1000).withSchedulingTimeout(500);
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:1000)_schedTimeout(rel:500)", tcString );
    assertEquals("TimeConstraint.delayed(1000).withSchedulingTimeout(500)", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.delayed(1000).withSchedulingTimeout(500)", tc2.toString() );
  }

  
  public void testWindow() {
    TimeConstraint tc = TimeConstraint.schedulingWindow("saturday_night");
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:0)_window(saturday_night)_start(rel:0)", tcString );
    assertEquals("TimeConstraint.schedulingWindow(\"saturday_night\")", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.schedulingWindow(\"saturday_night\")", tc2.toString() );
  }
  
  public void testDelayedWindow() {
    TimeConstraint tc = TimeConstraint.delayed(500).withTimeWindow("saturday_night");
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:500)_window(saturday_night)_start(rel:0)", tcString );
    assertEquals("TimeConstraint.delayed(500).withTimeWindow(\"saturday_night\")", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.delayed(500).withTimeWindow(\"saturday_night\")", tc2.toString() );
  }
  
  public void testWindowAndTimeout() {
    TimeConstraint tc = TimeConstraint.schedulingWindow("saturday_night", TimeConstraint.immediately().withSchedulingTimeout(500));
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:0)_window(saturday_night)_start(rel:0)_schedTimeout(rel:500)", tcString );
    assertEquals("TimeConstraint.schedulingWindow(\"saturday_night\",TimeConstraint.immediately().withSchedulingTimeout(500))", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.schedulingWindow(\"saturday_night\",TimeConstraint.immediately().withSchedulingTimeout(500))", tc2.toString() );
  }
  
  public void testDelayedWindowAndTimeout() {
    TimeConstraint tc = TimeConstraint.delayed(500).withTimeWindow("saturday_night", TimeConstraint.immediately().withSchedulingTimeout(500));
    String tcString = tc.serializeToString();
    
    assertEquals("start(rel:500)_window(saturday_night)_start(rel:0)_schedTimeout(rel:500)", tcString );
    assertEquals("TimeConstraint.delayed(500).withTimeWindow(\"saturday_night\",TimeConstraint.immediately().withSchedulingTimeout(500))", tc.toString() );
    
    TimeConstraint tc2 = TimeConstraint.valueOf(tcString);
    assertEquals(tc,tc2);
    assertEquals("TimeConstraint.delayed(500).withTimeWindow(\"saturday_night\",TimeConstraint.immediately().withSchedulingTimeout(500))", tc2.toString() );
  }
 

  
  
}
