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

import java.text.SimpleDateFormat;
import java.util.Date;


import junit.framework.TestCase;


public class TimeWindowTest extends TestCase {

  public static SimpleDateFormat timeWithoutDate = new SimpleDateFormat("dd HH:mm:ss");
  public static String toTime(long timestamp) {
    String time = timeWithoutDate.format(new Date(timestamp));
    if( time.startsWith("10") ) {
      return time.substring(3);
    } else {
      return time.substring(1);
    }
  }
  
  
  public static SimplePeriodicTimeWindow TW1 = new SimplePeriodicTimeWindow("%3600+0",60);
  public static SimplePeriodicTimeWindow TW2 = new SimplePeriodicTimeWindow("%3600+1800",300);
  public static SimplePeriodicTimeWindow TW3 = new SimplePeriodicTimeWindow("%86400+21600",7200);
  
  public static SimplePeriodicTimeWindow TW4 = new SimplePeriodicTimeWindow("%3600+1980",300);
  public static SimplePeriodicTimeWindow TW5 = new SimplePeriodicTimeWindow("%3600+0",1800);
  public static SimplePeriodicTimeWindow TW6 = new SimplePeriodicTimeWindow("%3600+1800",1800);
  
  public static MultiTimeWindow MTW1 = new MultiTimeWindow(TW1,TW2);
  public static MultiTimeWindow MTW2 = new MultiTimeWindow(TW2,TW4);
  public static MultiTimeWindow MTW3 = new MultiTimeWindow(TW5,TW6);
  
  public void testToString() {
    assertEquals("SimplePeriodicTimeWindow(\"%3600+0\",60)", TW1.toString() );
  }
  
  public void testTW1() {
    SimplePeriodicTimeWindow tw = TW1;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("14:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("13:01:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:00:05", toTime(now) );
    assertEquals("15:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("14:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    
  }
  
  public void testTW2() {
    SimplePeriodicTimeWindow tw = TW2;
    
    long now = 1362919657537L; //System.currentTimeMillis();
    
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("14:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:35:00", toTime(tw.getNextClose()) );
    assertEquals("13:35:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:30:05", toTime(now) );
    assertEquals("15:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:35:00", toTime(tw.getNextClose()) );
    assertEquals("14:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    
    now = 1362920400000L;
    
    tw.recalculate(now);
    
    assertEquals("14:00:00", toTime(now) );
    assertEquals("14:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:35:00", toTime(tw.getNextClose()) );
    assertEquals("13:35:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    
  }
 
  public void testTW3() {
    SimplePeriodicTimeWindow tw = TW3;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("1 07:00:00", toTime(tw.getNextOpen()) ); //Achtung Winterzeit UTC+1
    assertEquals("1 09:00:00", toTime(tw.getNextClose()) );
    assertEquals("09:00:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("1 07:00:05", toTime(now) );
    assertEquals("2 07:00:00", toTime(tw.getNextOpen()) );
    assertEquals("1 09:00:00", toTime(tw.getNextClose()) );
    assertEquals("1 07:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
  }
 
  public void testReopenTW1() {
    SimplePeriodicTimeWindow tw = TW1;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("14:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("13:01:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen();
    tw.recalculate(now);
    
    assertEquals("14:00:00", toTime(now) );
    assertEquals("15:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("14:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    
    now += 1;
    assertEquals("14:00:00", toTime(now) );
    assertEquals("15:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("14:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
   
    
  }

  public void testMTW1() {
    TimeWindow tw = MTW1;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("14:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("13:35:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:00:05", toTime(now) );
    assertEquals("14:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:01:00", toTime(tw.getNextClose()) );
    assertEquals("14:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    
    now = tw.getNextClose() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:01:05", toTime(now) );
    assertEquals("14:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:35:00", toTime(tw.getNextClose()) );
    assertEquals("14:01:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:30:05", toTime(now) );
    assertEquals("15:00:00", toTime(tw.getNextOpen()) );
    assertEquals("14:35:00", toTime(tw.getNextClose()) );
    assertEquals("14:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    
    now = tw.getNextClose() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:35:05", toTime(now) );
    assertEquals("15:00:00", toTime(tw.getNextOpen()) );
    assertEquals("15:01:00", toTime(tw.getNextClose()) );
    assertEquals("14:35:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    
  }

  public void testMTW2() {
    MultiTimeWindow tw = MTW2;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("14:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:38:00", toTime(tw.getNextClose()) );
    assertEquals("13:38:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    assertFalse(tw.isOnlyEstimated() );
    
    now = tw.getNextOpen() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:30:05", toTime(now) );
    assertEquals("15:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:38:00", toTime(tw.getNextClose()) );
    assertEquals("14:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    assertEquals(1 ,tw.getOpened(now) );
    assertFalse(tw.isOnlyEstimated() );
    
    now += 240000;
    tw.recalculate(now);
    
    assertEquals("14:34:05", toTime(now) );
    assertEquals("15:30:00", toTime(tw.getNextOpen()) );
    assertEquals("14:38:00", toTime(tw.getNextClose()) );
    assertEquals("14:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    assertEquals(2 ,tw.getOpened(now) );
    assertFalse(tw.isOnlyEstimated() );
    
    now = tw.getNextClose() + 5000;
    tw.recalculate(now);
    
    assertEquals("14:38:05", toTime(now) );
    assertEquals("15:30:00", toTime(tw.getNextOpen()) );
    assertEquals("15:38:00", toTime(tw.getNextClose()) );
    assertEquals("14:38:00", toTime(tw.getSince()) );
    assertFalse(tw.isOpen());
    assertFalse(tw.isOnlyEstimated() );
  }
  
  public void testMTW3() {
    MultiTimeWindow tw = MTW3;
    long now = 1362919657537L; //System.currentTimeMillis();
    tw.recalculate(now);
    
    assertEquals("13:47:37", toTime(now) );
    assertEquals("19:30:00", toTime(tw.getNextOpen()) );
    assertEquals("19:00:00", toTime(tw.getNextClose()) );
    assertEquals("08:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    assertTrue(tw.isOnlyEstimated() );
    
    now = tw.getNextChange();
    tw.recalculate(now);
    assertEquals("19:00:00", toTime(now) );
    assertEquals("1 01:00:00", toTime(tw.getNextOpen()) );
    assertEquals("1 00:30:00", toTime(tw.getNextClose()) );
    assertEquals("14:00:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    assertTrue(tw.isOnlyEstimated() );
    
    now = tw.getNextChange();
    tw.recalculate(now);
    assertEquals("1 00:30:00", toTime(now) );
    assertEquals("1 06:30:00", toTime(tw.getNextOpen()) );
    assertEquals("1 06:00:00", toTime(tw.getNextClose()) );
    assertEquals("19:30:00", toTime(tw.getSince()) );
    assertTrue(tw.isOpen());
    assertTrue(tw.isOnlyEstimated() );
    
  }
  
  
}
