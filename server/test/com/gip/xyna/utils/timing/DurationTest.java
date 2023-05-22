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
package com.gip.xyna.utils.timing;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;


/**
 *
 */
public class DurationTest extends TestCase {


  public void testParse() {
    
    assertEquals("1 s", Duration.valueOf("1").toString());
    assertEquals("1 s", Duration.valueOf("1s").toString());
    assertEquals("1 s", Duration.valueOf("1 s").toString());
    assertEquals("1 s", Duration.valueOf("1", TimeUnit.SECONDS).toString());
    
    assertEquals("2 ms", Duration.valueOf("2", TimeUnit.MILLISECONDS).toString());
    assertEquals("2 ms", Duration.valueOf("2ms").toString());
    assertEquals("2 ms", Duration.valueOf("2 ms").toString());
    
  }

  public void testEquals() {
    Duration d1 = Duration.valueOf("1");
    Duration d2 = Duration.valueOf("1 s");
    Duration d3 = Duration.valueOf("1000 ms");
    assertEquals(d1, d2);
    assertEquals(d1, d3);
    assertEquals(d2, d3);
    
    assertEquals(d1.hashCode(), d2.hashCode());
    assertEquals(d1.hashCode(), d3.hashCode());
    assertEquals(d2.hashCode(), d3.hashCode());
  }
  
  
  
  public void testConvertJava15() {
    if( TimeUnit.values().length != 4 ) {
      return; //Java15 hat 4 Eintr�ge in TimeUnit und kennt MINUTES, HOURS und DAYS nicht
    }
    
    Duration d1 = Duration.valueOf("1 min");
    assertEquals( "60 s", d1.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "60 s", d1.convertToLargestUnitWithoutPrecisionLost().toString() );
    
    Duration d3 = Duration.valueOf("3600000 ms");
    assertEquals( "3600 s", d3.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "3600 s", d3.convertToLargestUnitWithoutPrecisionLost().toString() );
    
    Duration d4 = Duration.valueOf("24 h");
    assertEquals( "86400 s", d4.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "86400 s", d4.convertToLargestUnitWithoutPrecisionLost().toString() );
    
  }
  
  public void testConvertJava16() {
    if( TimeUnit.values().length == 4 ) {
      return; //Java15 hat 4 Eintr�ge in TimeUnit
    }
    TimeUnit MINUTES = Duration.unitOf("min", null);
    TimeUnit HOURS = Duration.unitOf("h", null);
        
    Duration d1 = Duration.valueOf("1 min");
    assertEquals( "60 s", d1.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "0 h", d1.convertTo(HOURS).toString() );
    assertEquals( "1 min", d1.convertToLargestUnitWithoutPrecisionLost().toString() );
    
    
    Duration d2 = Duration.valueOf("60 s");
    assertEquals( "60 s", d2.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "1 min", d2.convertTo(MINUTES).toString() );
    assertEquals( "1 min", d2.convertToLargestUnitWithoutPrecisionLost().toString() );
   
    Duration d3 = Duration.valueOf("3600000 ms");
    assertEquals( "3600 s", d3.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "60 min", d3.convertTo(MINUTES).toString() );
    assertEquals( "1 h", d3.convertTo(HOURS).toString() );
    assertEquals( "1 h", d3.convertToLargestUnitWithoutPrecisionLost().toString() );
    
    Duration d4 = Duration.valueOf("24 h");
    assertEquals( "86400 s", d4.convertTo(TimeUnit.SECONDS).toString() );
    assertEquals( "1440 min", d4.convertTo(MINUTES).toString() );
    assertEquals( "24 h", d4.convertTo(HOURS).toString() );
    assertEquals( "1 d", d4.convertToLargestUnitWithoutPrecisionLost().toString() );

  }
  
  
  public void testParseSum() {
    //Duration d1 = Duration.valueOf("1 min 30s");
    //assertEquals( "90 s", d1.convertTo(TimeUnit.SECONDS).toString() );
    
    Duration d2 = Duration.valueOfSum("1 min 30s");
    assertEquals( "90 s", d2.toString() );
    assertEquals( "90 s", d2.convertTo(TimeUnit.SECONDS).toString() );
    
    Duration d3 = Duration.valueOfSum("1 min 20 ms 30s");
    assertEquals( "90020 ms", d3.toString() );
    
    assertEquals( "90 s", d3.convertTo(TimeUnit.SECONDS).toString() );
   
    long now = 1408350030137L; //System.currentTimeMillis();
    Duration dnow = new Duration(now);
    assertEquals( "1408350030137 ms", dnow.toString() );
    assertEquals( "16300 d 8 h 20 min 30 s 137 ms", dnow.toSumString() );
    Duration dnow2 = Duration.valueOfSum(dnow.toSumString() );
    assertEquals( "1408350030137 ms", dnow2.toString() );
    Duration dnow3 = Duration.valueOfSum("16300d8h20min30s137ms" );
    assertEquals( "1408350030137 ms", dnow3.toString() );
    
    
  }
  
  public void testParseSum2() {
    Duration d = Duration.valueOf("150 s");
    assertEquals( "150 s", d.toString() );
    assertEquals( "2 min 30 s", d.toSumString() );
    
  }
  
  public void testParseSum3() {
    try {
      @SuppressWarnings("unused")
      Duration d = Duration.valueOfSum("3 x");
      fail("Exception expected");
    } catch( Exception e ) {
      assertEquals("\"3 x\" contains unexpected \"x\"", e.getMessage() );
    }
    try {
      Duration d = Duration.valueOfSum("3 min 10 s");
      assertEquals( "190 s", d.toString() );
    } catch( Exception e ) {
      fail("unexpected exception "+ e.getMessage() );
    }
    
    
  }
  
}
