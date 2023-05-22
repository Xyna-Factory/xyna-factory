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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.gip.xyna.utils.timing.ExecutionPeriod.FixedDate_CatchUpNotTooLate;


public class ExecutionPeriodTest extends TestCase {
  
  public static long[] TEST_DELAY_LARGE = new long[]{23L, 512L, 785L, 10L, 1450L, 234L, 11L, 3200, 333, 512, 10, 20, 50 };
  //public static long[] TEST_DELAY_PEAK = new long[]{0L, 0L, 1500L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  
  
  public void testFirstNext() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_IgnoreAllMissed, 1000L, 0L);
    assertEquals( "0 1000 1000 1000", execFirstNext(ep, 0) );
    assertEquals( "1000 1000 1000 1000", execFirstNext(ep, 10) );
    assertEquals( "1000 1000 1000 1000", execFirstNext(ep, 610) );
    assertEquals( "1000 2000 2000 2000", execFirstNext(ep, 1000) );
    assertEquals( "2000 2000 2000 2000", execFirstNext(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpImmediately, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 0) );
    assertEquals( "10 1000 2000 3000", execFirstNext(ep, 10) );
    assertEquals( "610 1000 2000 3000", execFirstNext(ep, 610) );
    assertEquals( "1000 1000 2000 3000", execFirstNext(ep, 1000) );
    assertEquals( "1110 1110 2000 3000", execFirstNext(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpNotTooLate, 1000L, 0L);
    assertEquals( "0 1000 0 1000", execFirstNext(ep, 0) );
    assertEquals( "10 1000 10 1000", execFirstNext(ep, 10) );
    assertEquals( "1000 1000 1000 1000", execFirstNext(ep, 610) );
    assertEquals( "1000 2000 1000 2000", execFirstNext(ep, 1000) );
    assertEquals( "1110 2000 1110 2000", execFirstNext(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpInPast, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 0) );
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 10) );
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 610) );
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 1000) );
    assertEquals( "0 1000 2000 3000", execFirstNext(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedInterval, 1000L, 0L);
    assertEquals( "0 1000 1000 1000", execFirstNext(ep, 0) );
    assertEquals( "10 1010 1010 1010", execFirstNext(ep, 10) );
    assertEquals( "610 1610 1610 1610", execFirstNext(ep, 610) );
    assertEquals( "1000 2000 2000 2000", execFirstNext(ep, 1000) );
    assertEquals( "1110 2110 2110 2110", execFirstNext(ep, 1110) );
       
  }
  
  public void testFirstNextReal() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_IgnoreAllMissed, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 0) );
    assertEquals( "1000 2000 3000 4000", execFirstNextReal(ep, 10) );
    assertEquals( "1000 2000 3000 4000", execFirstNextReal(ep, 610) );
    assertEquals( "1000 2000 3000 4000", execFirstNextReal(ep, 1000) );
    assertEquals( "2000 3000 4000 5000", execFirstNextReal(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpImmediately, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 0) );
    assertEquals( "10 1000 2000 3000", execFirstNextReal(ep, 10) );
    assertEquals( "610 1000 2000 3000", execFirstNextReal(ep, 610) );
    assertEquals( "1000 1005 2000 3000", execFirstNextReal(ep, 1000) );
    assertEquals( "1110 1115 2000 3000", execFirstNextReal(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpNotTooLate, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 0) );
    assertEquals( "10 1000 2000 3000", execFirstNextReal(ep, 10) );
    assertEquals( "1000 2000 3000 4000", execFirstNextReal(ep, 610) );
    assertEquals( "1000 2000 3000 4000", execFirstNextReal(ep, 1000) );
    assertEquals( "1110 2000 3000 4000", execFirstNextReal(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpInPast, 1000L, 0L);
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 0) );
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 10) );
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 610) );
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 1000) );
    assertEquals( "0 1000 2000 3000", execFirstNextReal(ep, 1110) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedInterval, 1000L, 0L);
    assertEquals( "0 1005 2010 3015", execFirstNextReal(ep, 0) );
    assertEquals( "10 1015 2020 3025", execFirstNextReal(ep, 10) );
    assertEquals( "610 1615 2620 3625", execFirstNextReal(ep, 610) );
    assertEquals( "1000 2005 3010 4015", execFirstNextReal(ep, 1000) );
    assertEquals( "1110 2115 3120 4125", execFirstNextReal(ep, 1110) );
       
  }
  
  private String execFirstNext(ExecutionPeriod ep, long now) {
    ep.reset();
    long first = ep.nextAndInc(now); 
    long next = ep.nextAndInc(now);
    long nnext = ep.nextAndInc(now);
    long nnnext = ep.nextAndInc(now);
    return first+" "+next+" "+nnext+" "+nnnext;
  }
  
  private String execFirstNextReal(ExecutionPeriod ep, long now) {
    ep.reset();
    long first = ep.nextAndInc(now);
    long next = ep.nextAndInc(first+5);
    long nnext = ep.nextAndInc(next+5);
    long nnnext = ep.nextAndInc(nnext+5);
    return first+" "+next+" "+nnext+" "+nnnext;
  }

  public void testRestart() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_IgnoreAllMissed, 1000L, 0L);
    assertEquals( "[0, 1000, 2000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000]", execRestart(ep, 0, false) );
    assertEquals( "[0, 1000, 2000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000]", execRestart(ep, 0, true) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpImmediately, 1000L, 0L);
    assertEquals( "[0, 1000, 2000, 5333, 5338, 5343, 5348, 5353, 5358, 6000, 7000]", execRestart(ep, 0, false) );
    assertEquals( "[0, 1000, 2000, 5333, 5338, 5343, 6000, 7000, 8000, 9000, 10000]", execRestart(ep, 0, true) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpNotTooLate, 1000L, 0L);
    assertEquals( "[0, 1000, 2000, 5333, 6000, 7000, 8000, 9000, 10000, 11000, 12000]", execRestart(ep, 0, false) );
    assertEquals( "[0, 1000, 2000, 5333, 6000, 7000, 8000, 9000, 10000, 11000, 12000]", execRestart(ep, 0, true) );
    
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpInPast, 1000L, 0L);
    assertEquals( "[0, 1000, 2000, 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000]", execRestart(ep, 0, false) );
    assertEquals( "[0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000]", execRestart(ep, 0, true) );
     
    ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedInterval, 1000L, 0L);
    assertEquals( "[0, 1005, 2010, 5333, 6338, 7343, 8348, 9353, 10358, 11363, 12368]", execRestart(ep, 0, false) );
    assertEquals( "[0, 1005, 2010, 6333, 7338, 8343, 9348, 10353, 11358, 12363, 13368]", execRestart(ep, 0, true) );

  }
  
  private String execRestart(ExecutionPeriod ep, long now, boolean fixCounter) {
    ep.reset();
    List<Long> dates = new ArrayList<Long>();
    long curr; 
    dates.add( curr = ep.nextAndInc(now) ); 
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    ep.reset();
    if( fixCounter ) {
      ep.setCounter(3);
    }
    curr = 5333;
    dates.add( curr = ep.nextAndInc(curr) ); 
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
    dates.add( curr = ep.nextAndInc(curr+5) );
   return dates.toString();
  }
  
  public void testFixedDate_IgnoreAllMissed() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_IgnoreAllMissed, 1000L, 0L);
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 6000, 7000, 8000, 12000, 13000, 14000, 15000, 16000, 17000]", dates.toString());
  }
  
  public void testFixedDate_CatchUpImmediately() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpImmediately, 1000L, 0L);
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 5450, 6000, 7000, 10200, 10533, 11045, 11055, 12000, 13000]", dates.toString());
  }
  
  public void testFixedDate_CatchUpInPast() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpInPast, 1000L, 0L);
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000]", dates.toString());
  }

  public void testFixedDate_CatchUpNotTooLate() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_CatchUpNotTooLate, 1000L, 0L);
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 5450, 6000, 7000, 10200, 11000, 12000, 13000, 14000, 15000]", dates.toString());
  }
  
  public void testFixedDate_CatchUpNotTooLate2() {
    ExecutionPeriod ep  = new ExecutionPeriod( new FixedDate_CatchUpNotTooLate(0L,1000L,500L) );
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 5450, 6000, 7000, 10200, 11000, 12000, 13000, 14000, 15000]", dates.toString());
  }
  public void testFixedDate_CatchUpNotTooLate3() {
    ExecutionPeriod ep  = new ExecutionPeriod( new FixedDate_CatchUpNotTooLate(0L,1000L,250L) );
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1000, 2000, 3000, 4000, 6000, 7000, 8000, 11200, 12000, 13000, 14000, 15000, 16000]", dates.toString());
  }
  
  
  public void testFixedInterval() {
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedInterval, 1000L, 0L);
    List<Long> dates = execute( ep, TEST_DELAY_LARGE );
    assertEquals("[0, 1023, 2535, 4320, 5330, 7780, 9014, 10025, 14225, 15558, 17070, 18080, 19100, 20150]", dates.toString());
  }
  
  
  
  public void testFixedDate_CatchUpNotTooLateXXX() {
    ExecutionPeriod ep  = new ExecutionPeriod( new FixedDate_CatchUpNotTooLate(0L,1000L,500L) );
   
    long n = ep.nextAndInc(-1);
    System.out.println(n);
  }

  
 
  
  

  
  
  
  private List<Long> execute(ExecutionPeriod ep, long[] delay) {
    List<Long> dates = new ArrayList<Long>();
    long next =0;
    long now = 0;
    dates.add(ep.nextAndInc(now));
    for( int d=0; d<delay.length; ++d) {
      now = next + delay[d];
      next = ep.nextAndInc(now);
      //System.out.println( now +" -> next("+(d+1)+") " +next );
      dates.add(next);
    }
    
    return dates;
  }


  public static void main(String[] args) {
    //Repetition.Type.FixedDate_IgnoreAllMissed
    ExecutionPeriod ep  = new ExecutionPeriod(ExecutionPeriod.Type.FixedDate_IgnoreAllMissed, 1000L, 0L);
    //long [] delay = new long[]{23L, 52L, 78L, 10L, 10L, 24L, 11L, 30, 33, 52, 10, 20, 50, 23L, 52L, 78L, 10L, 10L, 24L, 11L, 30, 33, 52, 10, 20, 50, 23L, 52L, 78L, 10L, 10L, 24L, 11L, 30, 33, 52, 10, 20, 50, 23L, 52L, 78L, 10L, 10L, 24L, 11L, 30, 33, 52, 10, 20, 50 };
    long [] delay = new long[]{23L, 52L, 78L, 10L, 10L, 24L, 11L, 30, 33, 52, 10, 20, 50, 0, 0, 0, 0, 0, 0 ,1400, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    //long [] delay = new long[]{23L, 512L, 785L, 10L, 1450L, 234L, 11L, 3200, 333, 512, 10, 20, 50 };
    //ep.start(0L);
    long next =0;
    long now = 0;
    for( int d=0; d<delay.length; ++d) {
      now = next + delay[d];
      next = ep.nextAndInc(now);
      System.out.println( now +" -> next("+(d+1)+") " +next );
    }
    //System.out.println( ep.next(0L) );
    
  }

  
}
