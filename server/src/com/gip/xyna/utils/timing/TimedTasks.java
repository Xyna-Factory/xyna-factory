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
package com.gip.xyna.utils.timing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.collections.ConvertingIterator;


/**
 * TimedTasks ist eine Speicherung von Work-Objekten, die zu bestimmten Zeiten von einem Executor
 * bearbeitet werden müssen.<br>
 * Im Unterschied zu den üblichen Timer und TaskQueue-Implementierungen werden hier keine Runnable-
 * oder TimerTask-Implementierungen gespeichert, sondern einfachere Work-Objekte. Damit ergibt sich 
 * die Einschränkung, dass nicht völlig verschiedene Algorithmen ausgeführt werden, sondern immer 
 * die gleiche {@link com.gip.xyna.utils.timing.TimedTasks.Executor Executor}-Implementierung für 
 * alle Work-Objekte. Dafür kann das Work-Objekt aber auch so etwas einfaches sein wie ein Long.
 *  
 */
public class TimedTasks<W> implements Iterable<W> {

  /**
   * Executor wird für jedes Work-Objekt aufgerufen, um dieses zu bearbeiten.
   */
  public interface Executor<W> {
    
    /**
     * Ausführen der übergeben Arbeit
     * @param work
     */
    public void execute(W work);
    
    /**
     * Falls beim Ausführen von execute irgenein Fehler auftritt, muss dieser behandelt werden, 
     * da sonst der Thread stirbt.
     * Falls handleThrowable eine weitere Exception wirft, stirbt der Thread.
     * @param executeFailed
     */
    public void handleThrowable(Throwable executeFailed);
    
  }

  /**
   * Filter zum Suchen von Tasks
   */
  public interface Filter<W> {
    /**
     * @param work
     * @return true, wenn gesuchte Work gefunden
     */
    boolean isMatching(W work);
  }

  
  /**
   * Task-Objekt speichert Work und Ausführungszeitpunkt timestamp und kümmert sich um
   * die richtige Sortierung.
   */
  private static class Task<W> implements Comparable<Task<W>> {

    public long timestamp;
    public W work;

    public Task(long at, W work) {
      this.timestamp = at;
      this.work = work;
    }

    public int compareTo(Task<W> o) {
      if( timestamp < o.timestamp) {
        return -1;
      }
      if( timestamp > o.timestamp) {
        return 1;
      }
      //timestamps sind gleich, dann muss work verglichen werden
      return ((work == null) ? 0 : work.hashCode()) - ((o.work == null) ? 0 : o.work.hashCode());
    }
    
    
    
    @Override
    public String toString() {
      return work.toString(); //"Task("+timestamp+","+work+")";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
      result = prime * result + ((work == null) ? 0 : work.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Task<?> other = (Task<?>) obj;
      if (timestamp != other.timestamp)
        return false;
      if (work == null) {
        if (other.work != null)
          return false;
      } else if (!work.equals(other.work))
        return false;
      return true;
    }
    
  }
  
  /**
   * Implementierung des zugrundeliegenden Threads, der solange wartet, bis eine Work ausgeführt werde muss 
   * und dann den Executor diese Work ausführen lässt.
   */
  private class Runner implements Runnable {

    public void run() {
      running = true;
      try {
        while( running ) {
          //warten, bis Tasks vorhanden sind
          waitForTasks();
          //warten, bis aktueller Task ausgeführt werden darf
          Task<W> task = waitForTask();    
          if( ! running ) {
            break;
          }
          //Task erhalten, diesen ausführen
          if( task != null ) {
            try {
              executor.execute(task.work);
            } catch (Throwable t) {
              Department.handleThrowable(t);
              try {
                executor.handleThrowable(t);
              } catch (Throwable e) {
                Department.handleThrowable(e);
                CentralFactoryLogging.getLogger(TimedTasks.class).error("Error while handle throwable.", e);
              }
            }
          }
        }
      } finally {
        running = false;
      }
    }
    
    /**
     * Wartet, bis Tasks verfügbar sind
     */
    private void waitForTasks() {
      synchronized (tasks) {
        while (running && tasks.size() == 0) {
          try {
            tasks.wait();
          }
          catch (InterruptedException e) {
            //dann halt kürzer warten
          }
        }
      }
    }

    /**
     * Wartet, bis der Task mit der höchsten Priorität ausgeführt werden darf  
     * @return auszuführender Task<W> oder null
     */
    private Task<W> waitForTask() {
      synchronized (tasks) {
        while( running ) {
          Task<W> next = tasks.peek();
          if( next == null ) {
            return null; //Task wurde entfernt, es gibt keine Tasks mehr
          }
          long now = System.currentTimeMillis();
          long timeToWait = next.timestamp - now;
          if( timeToWait <= 0 ) {
            return tasks.poll();
          } else {
            //warten auf Ausführungszeit
            try {
              tasks.wait(timeToWait);
            }
            catch (InterruptedException e) {
              //dann halt kürzer warten
            }
            //Warten beendet, Gründe sind:
            //1. TimedTaskList wurde gestoppt, running == false ==> Abbruch durch while, return null
            //2. neuer Task wurde eingestellt oder 3. unbeabsichtigtes wecken:
            //=> einfach im nächsten Schleifendurchlauf  tasks.peek() auswerten und weiterwarten oder Task zurückgeben
          }
        }
      }
      return null;
    }
        
  }
  
  
  private PriorityBlockingQueue<Task<W>> tasks = new PriorityBlockingQueue<Task<W>>();
  private Executor<W> executor;
  private Thread thread;
  private volatile boolean running;
  private String threadName;

  /**
   * Anlegen eines neuen TimedTasks: zugrundeliegender Thread wird automatisch gestartet
   * @param threadName
   * @param executor
   */
  public TimedTasks(String threadName, Executor<W> executor) {
    this(threadName, executor, false);
  }
  
  /**
   * Anlegen eines neuen TimedTasks: zugrundeliegender Thread wird automatisch gestartet
   * @param threadName
   * @param executor
   */
  public TimedTasks(String threadName, Executor<W> executor, boolean asDeamon) {
    this.executor = executor;
    this.threadName = threadName;
    restart(asDeamon);
  }

  /**
   * Einstellen eines neuen Work-Objekts work, dass zum Zeitpunkt at bearbeitet werden soll
   * @param at
   * @param work
   */
  public void addTask(long at, W work) {
    synchronized (tasks) {
      tasks.offer( new Task<W>(at,work) );
      tasks.notify();
    }
  }
  
  /**
   * Entfernen eines Work-Objekts, welches equals(..) zu dem übergebenen work ist
   * @param work
   * @return entfernte Work
   */
  public W removeTask(W work) {
    synchronized (tasks) {
      Iterator<Task<W>> iter = tasks.iterator();
      while( iter.hasNext() ) {
        Task<W> t = iter.next();
        if( t.work.equals(work) ) {
          iter.remove();
          return t.work;
        }
      }
    }
    return null;
  }
  
  /**
   * Entfernen des ersten Work-Objekts, welches vom Filter als passend erkannt wird
   * @param filter
   * @return
   */
  public W removeTask( Filter<W> filter ) {
    synchronized (tasks) {
      Iterator<Task<W>> iter = tasks.iterator();
      while( iter.hasNext() ) {
        Task<W> t = iter.next();
        if( filter.isMatching(t.work) ) {
          iter.remove();
          return t.work;
        }
      }
    }
    return null;
  }
 
  /**
   * Entfernen aller Work-Objekte, die vom Filter als passend erkannt werden
   * @param filter
   * @return
   */
  public List<W> removeTasks( Filter<W> filter ) {
    List<Task<W>> tasks = removeTasks( new TaskFilter<W>(Long.MAX_VALUE,filter) );
    ArrayList<W> removed = new ArrayList<W>(tasks.size());
    for( Task<W> task : tasks ) {
      removed.add(task.work);
    }
    return removed;
  }
  
  /**
   * Zählen aller Work-Objekte, die vom Filter als passend erkannt werden
   * @param filter
   * @return
   */
  public int count(Filter<W> filter) {
    return count( new TaskFilter<W>(Long.MAX_VALUE,filter));
  }

  /**
   * Stoppen des ausführenden Threads
   */
  public void stop() {
    synchronized (tasks) {
      running = false;
      tasks.notify();
    }
  }
  
  
  /**
   * Starten des ausführenden Threads (in einem neuen Thread)
   */
  public void restart() {
    restart(false);
  }
  
  /**
   * Starten des ausführenden Threads (in einem neuen Thread)
   */
  public void restart(boolean asDeamon) {
    if( ! running ) {
      this.thread = new Thread(new Runner(), threadName );
      if (asDeamon) {
        this.thread.setDaemon(asDeamon);
      }
      this.thread.start(); 
    }
  }
  
  @Override
  public String toString() {
    Task<W> next = tasks.peek();
    if( next == null ) {
      return "TimedTaskList("+threadName+", [])";
    } else {
      long now = System.currentTimeMillis();
      return "TimedTaskList("+threadName+", next task in "+(next.timestamp-now)+" ms, "+getTaskList()+")";
    }
  }

  public Iterator<W> iterator() {
    List<Task<W>> data = getTaskList();
    return new ConvertingIterator<Task<W>,W>(data.iterator()){
      @Override
      protected W convert(Task<W> next) {
        return next.work;
      }
    };
    
  }
 
  private List<Task<W>> getTaskList() {
    ArrayList<Task<W>> data = null;
    synchronized (tasks) {
      data = new ArrayList<Task<W>>(tasks.size());
      for( Task<W> task: tasks ) {
        data.add(task);
      }
    }
    Collections.sort(data);
    return data;
  }
  
  /**
   * Anzahl der eingestellten Works
   * @return
   */
  public int size() {
    return tasks.size();
  }

  
  /**
   * Ausführen aller Works bis zum übergebenen Zeitstempel 
   * @param timestamp
   */
  public void executeAllUntil( long timestamp ) {
    long now = System.currentTimeMillis();
    synchronized (tasks) {
      for( Task<W> task: tasks ) {
        if( task.timestamp < timestamp ) {
          task.timestamp = now; //kann hier trivial umgesetzt werden, da Reihenfolge erhalten bleibt!
        }
      }
      tasks.notify(); //Wecken des ausführenden Threads
    }
  }

  /**
   * Ausführen aller vom Filter getroffenen Works bis zum übergebenen Zeitstempel
   * @param timestamp
   */
  public void executeAllUntil( long timestamp, Filter<W> filter ) {
    synchronized (tasks) {
      List<Task<W>> removed = removeTasks(new TaskFilter<W>(timestamp, filter));
      if( ! removed.isEmpty() ) {
        long now = System.currentTimeMillis();
        for( Task<W> task : removed ) {
          task.timestamp = now;
        }
        tasks.addAll(removed);
      }
      tasks.notify(); //Wecken des ausführenden Threads
    }
  }

  
  private List<Task<W>> removeTasks(TaskFilter<W> filter) {
    ArrayList<Task<W>> removed = new ArrayList<Task<W>>();
    //Remove über iterator und iterator.remove ist O(N^2)!
    synchronized (tasks) {
      ArrayList<Task<W>> allTasks = new ArrayList<Task<W>>(tasks.size());
      tasks.drainTo(allTasks); //O(N logN)
      for( Task<W> task : allTasks ) { //O(N)
        if( filter.isMatching(task) ) {
          removed.add(task);
        } else {
          tasks.add(task); //wieder eintragen O(logN), mit for also //O(N logN)
        }
      }
    }
    return removed;
  }
  
  private int count(TaskFilter<W> filter) {
    int cnt =0;
    synchronized (tasks) {
      for( Task<W> task : tasks ) {
        if( filter.isMatching(task) ) {
          ++cnt;
        }
      }
    }
    return cnt;
  }


  private static class TaskFilter<W> {

    private long maxTimestamp;
    private Filter<W> filter;

    public TaskFilter(long maxTimestamp, Filter<W> filter) {
      this.maxTimestamp = maxTimestamp;
      this.filter = filter;
    }
    
    public boolean isMatching( Task<W> task ) {
      return task.timestamp < maxTimestamp && filter.isMatching(task.work);
    }

  }
  
  
}
