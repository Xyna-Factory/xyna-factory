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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// what are the benefits over using ScheduledExecutorService.schedule(command, delay, timeunit) ?
/**
 * CancelableDelayedTask kann Runnable-Implementierungen speichern und nach 
 * einer vorher definierten Wartezeit ausführen. Dabei können die eingestellten 
 * Tasks über cancel() abgebrochen werden oder ein Thread kann über join() auf 
 * die Ausführung warten.
 * Es können mehrere Runnables gleichzeitig aktiv sein
 * 
 *  CancelableDelayedTask cdt = new CancelableDelayedTask("Threadname");
 *  int id = cdt.schedule( 500, new Runnable() { public void run(){
 *   //exec
 *    } );
 *  if( cdt.cancel(id) != State.Canceled ) {
 *    //Task konnte nicht mehr gecancelt werden, daher auf vollständige Ausführung warten
 *    cdt.Join(id);
 *  }
 */
public class CancelableDelayedTask {
  
  private ThreadPoolExecutor pool;
  
  public CancelableDelayedTask() {
    this("CancelableDelayedTask-Thread");
  }
  
  /**
   * @param name Name der ausführenden Threads
   */
  public CancelableDelayedTask(final String name) {
    pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
      60L, TimeUnit.SECONDS,
      new SynchronousQueue<Runnable>(), new ThreadFactory() {
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r, name);
          t.setDaemon(true);
          return t;
        }
      });
  }

  private AtomicInteger idGenerator = new AtomicInteger(0);
  private Map<Integer,Task> tasks = Collections.synchronizedMap( new HashMap<Integer,Task>() );
  
  public static enum State { Waiting, Canceled, Running, Executed; }
  
  private class Task implements Runnable {
    long delay;
    Integer id;
    volatile State state = State.Waiting;
    Future<?> future;
    Runnable task;
    
    public Task(long delay, Runnable task, Integer id) {
       this.delay = delay;
       this.task = task;
       this.id = id;
    }

    public void run() {
      try {
        Thread.sleep(delay);
      }
      catch (InterruptedException e) {
        state = State.Canceled;
        tasks.remove(id);
        return;
      }
      state = State.Running;
      if( Thread.currentThread().isInterrupted() ) {
        state = State.Canceled;
        tasks.remove(id);
        return;
      }
      
      task.run();
      tasks.remove(id);
      state = State.Executed;
    }
  }
  
  /**
   * Einstellen eines neuen Runnable
   * @param delay
   * @param task
   * @return Id, mit der cancel oder join aufgerufen werden können
   */
  public Integer schedule( long delay, Runnable task ) {
    if( delay < 0 ) {
      throw new IllegalArgumentException("delay may not be negative");
    }
    Integer id = idGenerator.incrementAndGet();
    Task innerTask = new Task( delay, task, id);
    tasks.put( id, innerTask );
    innerTask.future = pool.submit( innerTask );
    
    return id;
  }
  
  /**
   * Abbrechen des über schedule(...) eingestellten Runnable. Achtung: ausführender Thread 
   * wird interupted, dies kann auch das ausgeführte Runnable treffen.
   * @param id
   * @return Status, ob Abbrechen erfolgreich war
   */
  public State cancel(Integer id) {
    Task task = tasks.get(id);
    if( task == null ) {
      return State.Executed;
    }
    if( task.future.cancel(true) ) {
      tasks.remove(id);
      if( task.state == State.Waiting ) {
        return State.Canceled;
      }
      return task.state;
    }
    return State.Running;
  }
  
  /**
   * Warten auf die Beendiugn des eingestellten Runnables
   * @param id
   * @return Status, ob Auftrag ausgeführt wurde
   * @throws InterruptedException
   */
  public State join(Integer id) throws InterruptedException {
    Task task = tasks.get(id);
    if( task == null ) {
      return State.Executed;
    }
    try {
      task.future.get();
      return State.Executed;
    }
    catch (ExecutionException e) {
      return State.Canceled;
    }
    catch (CancellationException e) {
      return State.Canceled;
    }
  }

}
