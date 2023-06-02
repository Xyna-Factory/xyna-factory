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

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.timing.TimedTasks;
import com.gip.xyna.utils.timing.TimedTasks.Executor;
import com.gip.xyna.utils.timing.TimedTasks.Filter;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;


/**
 * TimeConstraintExecutor startet den Thread, der sich um die rechtzeitige Bearbeitung von 
 * zeitabhängigen Aufgaben kümmert.
 * Diese Aufgaben sind
 * <ul>
 * <li> {@link SchedulingTimeout}</li>
 * <li> {@link StartTime}</li>
 * <li> {@link TimeWindowChanger}</li>
 * </ul>
 */
public class TimeConstraintExecutor implements Executor<TimeConstraintExecutor.TimeConstraintTask> {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TimeConstraintExecutor.class);
  private TimeConstraintManagement timeConstraintManagement;
  private AllOrdersList allOrders;
  private TimedTasks<TimeConstraintTask> timedTasks;
  
  
  public TimeConstraintExecutor(TimeConstraintManagement timeConstraintManagement, AllOrdersList allOrders) {
    this.timeConstraintManagement = timeConstraintManagement;
    this.allOrders = allOrders;
    this.timedTasks = new TimedTasks<TimeConstraintTask>("TimeConstraintExecutorThread", this );
  }

  /**
   * Hinzufügen eines neuen Tasks
   * @param task
   */
  public void add(TimeConstraintTask task) {
    timedTasks.addTask( task.getTimestamp(), task ); 
  }
  
  public TimeConstraintTask remove(TimeConstraintTask task) {
    return timedTasks.removeTask(task);
  }

  
  public void execute(TimeConstraintTask work) {
    work.execute( timeConstraintManagement, allOrders );
  }
  
  public void handleThrowable(Throwable executeFailed) {
    logger.warn( "TimeConstraintExecutor.execute(TimeConstraint) failed ", executeFailed );
  }
  
  public void stop() {
    timedTasks.stop();
  }

  /**
   * Frühzeitiger Timeout aller Aufträge, die ihren regulären Timeout innerhalb der nächsten timeout ms hätten
   * @param timeout
   */
  public void earlyTimeout(long timeout) {
    timedTasks.executeAllUntil(System.currentTimeMillis()+timeout, SchedulingTimeout.filter ); 
  }
  
  /**
   * Entfernen des auf Startzeitpunkt wartenden Auftrags
   * @param orderId
   * @return
   */
  public SchedulingOrder removeWaitingForStartTime(Long orderId) {
    return removeTimeConstraintTaskWithOrder( orderId, TimeConstraintTaskWithOrder.Type.StartTime );
  }
  
  /**
   * Entfernen des SchedulingTimeout für den wartenden Auftrag
   * @param orderId
   * @return
   */
  public SchedulingOrder removeSchedulingTimeout(Long orderId) {
    return removeTimeConstraintTaskWithOrder( orderId, TimeConstraintTaskWithOrder.Type.SchedulingTimeout );
  }
  
  private SchedulingOrder removeTimeConstraintTaskWithOrder(Long orderId, TimeConstraintTaskWithOrder.Type type ) {
    TimeConstraintTask removed = timedTasks.removeTask( new TimeConstraintTaskWithOrder.OrderFilter(orderId, type) );
    if( removed != null ) {
      TimeConstraintTaskWithOrder tctwo = (TimeConstraintTaskWithOrder)removed;
      tctwo.remove();
      return tctwo.so;
    }
    return null;
  }

  
  /**
   * Basisklasse für alle hier verwendeten {@link TimedTasks}. 
   * Die vollständige Implementation dieser Klasse ist sowohl der TimedTasks-Task als auch 
   * über die execute-Methode die eigentliche {@link TimedTasks.Executor}-Implementierung.
   * (Die übliche Trennung im TimedTasks zwischen einem Executor und simplen Task-Objekten 
   * musste hier aufgegeben werden, damit die derzeit drei verschiedenen Task-Typen in einem 
   * TimedTasks laufen können.
   */
  public static abstract class TimeConstraintTask {

    public abstract void execute(TimeConstraintManagement timeConstraintManagement, AllOrdersList allOrders);

    public abstract Long getTimestamp();
    
  }
  
  public static abstract class TimeConstraintTaskWithOrder extends TimeConstraintTask { 
    
    public enum Type {
      SchedulingTimeout, StartTime;
    }
    public static EnumMap<Type,AtomicInteger> counter;
    static {
      counter = new EnumMap<Type,AtomicInteger>(Type.class);
      for( Type t : Type.values() ) {
        counter.put( t, new AtomicInteger() );
      }
    }

    protected SchedulingOrder so;
    private Type type;

    public TimeConstraintTaskWithOrder(SchedulingOrder so, Type type) {
      this.so = so;
      this.type = type;
      counter.get(type).incrementAndGet();
    }

    public static class OrderFilter implements Filter<TimeConstraintTask> {
      private Long orderId;
      private Type type;
      
      public OrderFilter(Long orderId, Type type) {
        this.orderId = orderId;
        this.type = type;
      }
      public boolean isMatching(TimeConstraintTask work) {
        if( work instanceof TimeConstraintTaskWithOrder ) {
          TimeConstraintTaskWithOrder tctwo = (TimeConstraintTaskWithOrder) work;
          if( tctwo.type == type ) {
            return false;
          }
          return orderId.equals( tctwo.so.getOrderId() );
        }
        return false;
      }
    };
        
    public void remove() {
      counter.get(type).decrementAndGet();
    }

    public static int getCounter(Type type) {
      return counter.get(type).get();
    }
  }
  
  /**
   * Abbrechen der Aufträge mit Timeout
   * Achtung, diese TimeConstraintTasks werden derzeit nicht aus der TaskListe entfernt, da 
   * das Suchen in der TaskListe aufwändig ist, die überflüssige Arbeit dagegen gering ist 
   * und die unnötige Referenz auf die SchedulingOrder nur wenig Speicher benötigt.
   */
  public static class SchedulingTimeout extends TimeConstraintTaskWithOrder {


    public static final Filter<TimeConstraintTask> filter = new Filter<TimeConstraintTask>() {
      public boolean isMatching(TimeConstraintTask work) {
        return work instanceof SchedulingTimeout;
      }
    };
    
    private Long timestamp;

    public SchedulingTimeout(SchedulingOrder so, TimeConstraintData tcd) {
      super(so, Type.SchedulingTimeout);
      this.timestamp = tcd.getSchedulingTimeout();
      if (logger.isDebugEnabled()) {
        logger.debug("adding Order "+so+" with schedulingTimeout in "+(timestamp-System.currentTimeMillis())+" ms "+timestamp);
      }
      tcd.setSchedulingTimeoutMonitored(true);
    }
    
    @Override
    public String toString() {
      return "SchedulingTimeout("+so.getOrderId()+","+timestamp+")";
    }

    @Override
    public void execute(TimeConstraintManagement timeConstraintManagement, AllOrdersList allOrders) {
      remove();
      if( so.isAlreadyScheduled() ) {
        //Auftrag wurde bereits gestartet, daher nichts mehr machen
      } else {
        allOrders.timeoutOrder(so);
      }
    }

    @Override
    public Long getTimestamp() {
      return timestamp;
    }
    
  }

  /**
   * Aufträge, die auf ihren Startzeitpunkt warten, werden hierüber wieder in den Scheduler eingestellt.
   *
   */
  public static class StartTime extends TimeConstraintTaskWithOrder {
    
    private Long timestamp;

    public StartTime(SchedulingOrder so, long startTimestamp) {
      super(so, Type.StartTime);
      this.timestamp = startTimestamp;
      logger.debug("adding Order "+so+" with startTime in "+(timestamp-System.currentTimeMillis())+" ms");
    }

    @Override
    public String toString() {
      return "StartTime("+so.getOrderId()+","+timestamp+")";
    }
    
    @Override
    public void execute(TimeConstraintManagement timeConstraintManagement, AllOrdersList allOrders) {
      remove();     
      allOrders.startTimeReached(so,false);
    }

    @Override
    public Long getTimestamp() {
      return timestamp;
    }
    
  }

  public int size() {
    return timedTasks.size();
  }

  /**
   * TimeWindows werden über diesen TimeConstraintTask geöffnet und geschlossen.
   *
   */
  public static class TimeWindowChanger extends TimeConstraintTask {

    private TimeConstraintWindow timeWindow;
    private Long timestamp;
    
    public TimeWindowChanger(TimeConstraintWindow timeWindow) {
      this.timeWindow = timeWindow;
      timeWindow.setTimeWindowChanger(this);
      this.timestamp = timeWindow.getNextChange(System.currentTimeMillis());
    }

    @Override
    public String toString() {
      return "TimeWindowChanger("+timeWindow.getName()+","+timestamp+")";
    }
    
    @Override
    public void execute(TimeConstraintManagement timeConstraintManagement, AllOrdersList allOrders) {
      timeWindow.lock(); //gegen gleichzeitige Verwendung im Scheduler schützen
      try {
        this.timestamp = timeWindow.getNextChange(System.currentTimeMillis());
        if( timeWindow.isOpen() ) {
          timeWindow.rescheduleWaitingOrders(allOrders);
        }
        if( ! timeWindow.isRemoved() ) {
          timeConstraintManagement.readd(this);
        }
      } finally {
        timeWindow.unlock();
      }
    }

    @Override
    public Long getTimestamp() {
      return timestamp;
    }
    
  }

}
