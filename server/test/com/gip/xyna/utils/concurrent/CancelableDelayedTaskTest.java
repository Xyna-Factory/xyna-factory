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
package com.gip.xyna.utils.concurrent;

import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.utils.concurrent.CancelableDelayedTask.State;



public class CancelableDelayedTaskTest extends TestCase {
  
 
  public void testExecution() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    cdt.schedule( 1000, new DelayedTask("exec",results) );
    Assert.assertEquals( 0, results.size() );
    Thread.sleep(1010);
    //System.out.println( results );
    Assert.assertEquals( 1, results.size() );
    Assert.assertEquals( "exec", results.get(0) );
  }
  
  public void testJoinExecution() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    long start = System.currentTimeMillis();
    Integer id = cdt.schedule( 257, new DelayedTask("exec",results) );
    State state = cdt.join(id);
    long end = System.currentTimeMillis();
    //System.out.println(results + " after "+ (end-start)+" ms with state " + state );
    Assert.assertTrue( (end-start) >= 257 );
    Assert.assertEquals( State.Executed, state );
  }
  
  
  public void testJoinCancelExecution() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    final CancelableDelayedTask cdt = new CancelableDelayedTask();
    long start = System.currentTimeMillis();
    final Integer id = cdt.schedule( 600, new DelayedTask("exec",results) );
    new Thread( new Runnable(){
      public void run() {
        try {
          Thread.sleep(500);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
        cdt.cancel(id);
      }} ).start();
    State state = cdt.join(id);
    long end = System.currentTimeMillis();
    //System.out.println(results + " after "+ (end-start)+" ms with state " + state);
    Assert.assertTrue( (end-start) >= 500 );
    Assert.assertTrue( (end-start) < 600 );
    Assert.assertEquals( State.Canceled, state );
  }

  public void testMultiExecution() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    cdt.schedule( 1000, new DelayedTask("exec1",results) ); 
    cdt.schedule( 500, new DelayedTask("exec2",results) ); 
    cdt.schedule( 800, new DelayedTask("exec3",results) );
    Thread.sleep(1010);
    System.out.println(results);
    Assert.assertEquals( "[exec2, exec3, exec1]", results.toString() );
  }
  
  public void testCancelExecution() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    int id = cdt.schedule( 500, new DelayedTask("canceled exec",results) );
    State state = cdt.cancel(id);
    System.out.println( "id= "+id+", state= "+state );
    Assert.assertEquals( State.Canceled, state );
    Thread.sleep(1000);
    System.out.println(results);
    Assert.assertEquals( 0, results.size() );
  }
  
  public void testError1() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    int id = cdt.schedule( 15, new DelayedTask("executed!",results) );
    Thread.sleep(15);
    State state = cdt.cancel(id);
    System.out.println( "id= "+id+", state= "+state );
    Thread.sleep(100);
    System.out.println(" -> " +results+"\n");
    
    //FIXME resultat kann variieren, deshalb kein Assert
  }

  public void testError2() throws InterruptedException {
    ArrayList<String> results = new ArrayList<String>();
    CancelableDelayedTask cdt = new CancelableDelayedTask();
    for( int i = 1 ; i< 10 ; ++i ) {
    int id = cdt.schedule( 15, new DelayedTask(""+i,results, 2) );
      Thread.sleep(15);
      State state = cdt.cancel(id);
      cdt.join(id);
      System.out.println( "id= "+id+", state= "+state );
    }
    Thread.sleep(100);
    System.out.println(" -> " +results+"\n");
    
    //FIXME resultat kann variieren, deshalb kein Assert

  }



  private static class DelayedTask implements Runnable {
    ArrayList<String> results;
    private String name;
    private long delay;
    public DelayedTask(String name, ArrayList<String> results ) {
      this(name,results,0); 
    }
    public DelayedTask(String name, ArrayList<String> results, long delay ) {
      this.name = name;
      this.results = results;
      this.delay = delay;
    }
   public void run() {
     if( delay> 0 ) {
       try {
        Thread.sleep(delay);
       }
       catch (InterruptedException e) {
       }
     }
      //System.out.println(name);
      results.add(name);
    }
  }
  
  
}
