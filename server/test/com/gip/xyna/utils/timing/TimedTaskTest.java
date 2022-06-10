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
package com.gip.xyna.utils.timing;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.gip.xyna.utils.timing.TimedTasks.Filter;


/**
 *
 */
public class TimedTaskTest extends TestCase {

  private static TimedTasks<?> timedTask;
  
  public void tearDown() {
    if( timedTask != null ) {
      timedTask.stop();
    }
  }
  
  private static class AddToListExecutor<T> implements TimedTasks.Executor<T> {
    ArrayList<T> list;
    public AddToListExecutor(ArrayList<T> list) {
      this.list = list;
    }
    public void execute(T work) {
      //System.err.println("AddToListExecutor.execute("+work+")");
      list.add(work);
    }
    public void handleThrowable(Throwable executeFailed) {
    }
  }
  
  /**
   * @param list
   * @return
   */
  private <T> TimedTasks<T> createNewTimedTasks(ArrayList<T> list) {
    TimedTasks<T> tt = new TimedTasks<T>( "TT-Test", new AddToListExecutor<T>(list) );
    timedTask = tt;
    return tt;
  }
  
  private void sleep(long sleep) {
    try {
      Thread.sleep(sleep);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private ArrayList<String> getThreadNames() {
    int num = Thread.activeCount();
    Thread[] threads = new Thread[num];
    Thread.enumerate(threads);
    ArrayList<String> names = new ArrayList<String>();
    for( Thread t : threads ) {
      names.add( t.getName() );
    }
    return names;
  }
  
  
  public void testThread() {
    String threadName = "TT-Test";
    ArrayList<String> list = new ArrayList<String>();
    
    TimedTasks<String> tt = new TimedTasks<String>( threadName, new AddToListExecutor<String>(list) );
    
    sleep(10); //warten, dass Thread wirklich gestartet ist
    //System.err.println( getThreadNames() );
    Assert.assertTrue( getThreadNames().contains(threadName));
    
    tt.stop();
    long start = System.currentTimeMillis();
    long end = start;
    while ((end -start) < 1000 ) {
      sleep(5);
      end = System.currentTimeMillis();
      List<String> names = getThreadNames();
      if( ! names.contains(threadName) ) {
        System.err.println( "after "+(end -start)+" ms thread has stopped" );
        break;
      }
      if( (end -start) > 400 ) {
        int num = Thread.activeCount();
        Thread[] threads = new Thread[num];
        Thread.enumerate(threads);
        for( Thread t : threads ) {
          if( t.getName().equals(threadName) ) {
            Exception e = new Exception();
            e.setStackTrace( t.getStackTrace() );
            e.printStackTrace();
          }
        }
        break;
      }
      
    }
   
    System.err.println( getThreadNames() );
    Assert.assertFalse( getThreadNames().contains(threadName));
    
    tt.restart();
    Assert.assertTrue( getThreadNames().contains(threadName));
    
  }
  
  
  
  public void testAddTask() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+10, "A" );
    tt.addTask( now+30, "C" );
    tt.addTask( now+20, "B" );
    
    Assert.assertEquals(3, tt.size() );
    System.err.println( tt );
    String ttString = tt.toString();
    
    Assert.assertTrue(ttString.endsWith("ms, [A, B, C])") );
    
  }
  
  public void testExecute() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+10, "A" );
    tt.addTask( now+30, "C" );
    tt.addTask( now+20, "B" );
    
    
    //Test sollte meistens erfolgreich sein, könnte aber sporadisch wegen Timing-Problemen scheitern
    sleep(5);
    Assert.assertEquals( "[]", list.toString() );
    sleep(10);
    Assert.assertEquals( "[A]", list.toString() );
    sleep(10);
    Assert.assertEquals( "[A, B]", list.toString() );
    sleep(10);
    Assert.assertEquals( "[A, B, C]", list.toString() );
    
  }
  
  public void testRemove() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+10, "A" );
    tt.addTask( now+30, "C" );
    tt.addTask( now+20, "B" );
   
    tt.removeTask("B");
    
    Assert.assertEquals(2, tt.size() );
    Assert.assertTrue(tt.toString().endsWith("ms, [A, C])") );
  }
  
  public void testExecuteAllR() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+100, "A" );
    tt.addTask( now+300, "C" );
    tt.addTask( now+200, "B" );
   
    tt.executeAllUntil( now+220 );
    sleep(20);
    Assert.assertEquals( "[A, B]", list.toString() );
    Assert.assertTrue(tt.toString().endsWith("ms, [C])") );
  }
  
  public void testRemoveFilter() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+100, "A" );
    tt.addTask( now+300, "C" );
    tt.addTask( now+200, "B" );
   
    Filter<String> filter = new Filter<String>() {
      public boolean isMatching(String work) {
        return ! work.equals("B");
      } 
    };
    
    List<String> removed = tt.removeTasks(filter);
    Assert.assertEquals( "[A, C]", removed.toString() );
    Assert.assertTrue(tt.toString().endsWith("ms, [B])") );
  }
  
  public void testExecuteAllFilter() {
    ArrayList<String> list = new ArrayList<String>();
    TimedTasks<String> tt = createNewTimedTasks( list );
    long now = System.currentTimeMillis();
    tt.addTask( now+100, "A" );
    tt.addTask( now+300, "C" );
    tt.addTask( now+200, "B" );
   
    Filter<String> filter = new Filter<String>() {
      public boolean isMatching(String work) {
        return work.equals("B");
      } 
    };
    
    tt.executeAllUntil( now+220, filter );
    sleep(20);
    Assert.assertEquals( "[B]", list.toString() );
    Assert.assertTrue(tt.toString().endsWith("ms, [A, C])") );
    
    
  }
  
 
  

  
  
public static void main(String[] args) throws InterruptedException {
    
    
    TimedTasks<String> ttl = new TimedTasks<String>( "TimedTaskList-Thread", new TimedTasks.Executor<String>(){
      public void execute(String work) {
        System.out.println( work);   
      }
      public void handleThrowable(Throwable executeFailed) {
      }} );
    
    long now = System.currentTimeMillis();
    
    ttl.addTask( now + 10, "A" );
    
    ttl.addTask( now + 200, "B" );
    
    ttl.addTask( now + 100, "C" );
    System.out.println( ttl );
    
    ttl.removeTask("A");
    
    System.out.println( ttl );
    
    Thread.sleep(100);
    
    System.out.println( ttl );
    
    Thread.sleep(200);
    System.out.println( ttl );
    ttl.stop();
    
    
  }

  
  
  
}
