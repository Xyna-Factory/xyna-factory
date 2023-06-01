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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.TimeWindowChanger;


/**
 * TimeConstraintWindow ist die kapselnde Klasse, die ein oder mehrere zusammengehörige {@link TimeWindow}s,
 * einen Namen und Beschreibung sowie eine Liste aller auf das Zeitfenster wartenden Aufträge 
 * zusammenfasst.
 */
public class TimeConstraintWindow {

  private static Logger logger = CentralFactoryLogging.getLogger(TimeConstraintWindow.class);
  
  private String name;
  private volatile TimeWindowState state;
  private TimeWindow timeWindow;
  private volatile ArrayList<SchedulingOrder> waitingOrders; //TODO wäre ConcurrentHashMap besser? //Lock könnte evtl. entfallen
  private ReentrantLock lock;
  private long openSince;
  private long nextOpen;
  private TimeWindowChanger timeWindowChanger;
  private TimeConstraintWindowDefinition definition;
  
  public static enum TimeWindowState {
    Open, Closed, Removed;
  }
  
  public TimeConstraintWindow(TimeConstraintWindowDefinition definition) {
    this.name = definition.getName();
    this.definition = definition;
    this.state = TimeWindowState.Closed;
    this.openSince = 0;
    this.waitingOrders = new ArrayList<SchedulingOrder>();
    this.lock = new ReentrantLock();
    this.timeWindow = definition.getTimeWindowDefinition().constructTimeWindow();
  }
  
  public String getName() {
    return name;
  }

  public String getDescription() {
    return definition.getDescription();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append(" (").append(getDescription()).append(")");
    if( timeWindow == null ) {
      sb.append(" with no window definition");
      return sb.toString();
    }
    long now = System.currentTimeMillis();
    getNextChange(now);
    sb.append(" using ").append(timeWindow).append(" is ").append(state);
    if( isOpen() ) {
      sb.append(" and will be closed in ").append(timeWindow.getNextClose()-now).append(" ms");
    } else {
      sb.append(" with ").append(waitingOrders.size()).append(" waiting orders");
      sb.append(" and will be opened in ").append(timeWindow.getNextOpen()-now).append(" ms");
    }
    return sb.toString();
  }

  public boolean isOpen() {
    return state == TimeWindowState.Open;
  }
  
  public boolean isClosed() {
    return state == TimeWindowState.Closed;
  }

  /**
   * Zeitfenster wird nicht mehr verwendet
   */
  public void setRemoved() {
    //TODO Warnung, wenn noch wartende Aufträge vorliegen
    this.state = TimeWindowState.Removed;
  }

  public boolean isRemoved() {
    return state == TimeWindowState.Removed;
  }

  /**
   * internes Ersetzen durch die Daten (description, timeWindow) aus other 
   * @param other
   */
  public void replaceDescriptionAndWindowWith(TimeConstraintWindow other) {
    lock.lock();
    try {
      this.definition = other.definition;
      this.timeWindow = other.timeWindow;
      getNextChange(System.currentTimeMillis());
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * @return
   */
  public Long getNextChange(long now) {
    timeWindow.recalculate(now);
    if( timeWindow.isOpen() ) {
      openSince = timeWindow.getSince();
      state = TimeWindowState.Open;
      nextOpen = 0L;
    } else {
      openSince = 0;
      state = TimeWindowState.Closed;
      nextOpen = timeWindow.getNextOpen();
    }
    return timeWindow.getNextChange();
  }
  
  /**
   * Warten derzeit Aufträge?
   * @return
   */
  public boolean hasWaitingOrders() {
    return ! waitingOrders.isEmpty();
  }

  /**
   * Hinzufügen eines wartenden Auftrags
   * (wird innerhalb des Locks gerufen)
   * @param so
   */
  public void addWaitingOrder(SchedulingOrder so) {
    waitingOrders.add(so);
  }

  /**
   * Entfernen eines wartenden Auftrags
   * (wird innerhalb des Locks gerufen)
   * @param so
   * @return true, wenn Auftrag wartend war
   */
  public boolean removeWaitingOrder(SchedulingOrder so) {
    return waitingOrders.remove(so);
  }
  
  /**
   * Entfernen eines wartenden Auftrags
   * (wird innerhalb des Locks gerufen)
   * @param orderId
    * @return wartende SchedulingOrder oder null
   */
  public SchedulingOrder removeWaitingOrder(Long orderId) {
    if( waitingOrders.isEmpty() ) {
      return null;
    }
    Iterator<SchedulingOrder> iter = waitingOrders.iterator();
    while( iter.hasNext() ) {
      SchedulingOrder so = iter.next();
      if( orderId.equals( so.getOrderId() ) ) {
        iter.remove();
        return so;
      }
    }
    return null;
  }


  /**
   * Wiedereinstellen aller wartenden Aufträge in den Scheduler
   * (wird innerhalb des Locks gerufen)
   * @param allOrders
   */
  public void rescheduleWaitingOrders(AllOrdersList allOrders) {
    if( waitingOrders.isEmpty() ) {
      return;
    }
    if( logger.isDebugEnabled() ) {
      logger.debug( "Rescheduling "+waitingOrders.size()+" orders waiting for timewindow "+name );
    }
    for( SchedulingOrder so : waitingOrders ) {
      allOrders.startTimeReached( so, false );
    }
    waitingOrders.clear();
  }
  

  /**
   * Schutz, damit nicht gleichzeitig Zeitfenster geöffnet wird und trotzdem noch 
   * wartende Aufträge angehängt werden.   
   */
  public void lock() {
    lock.lock();
  }

  public void unlock() {
    lock.unlock();
  }


  public long getOpenSince() {
    return openSince;
  }
  
  public long getNextOpen() {
    return nextOpen;
  }

  public void setTimeWindowChanger(TimeWindowChanger timeWindowChanger) {
    this.timeWindowChanger = timeWindowChanger;
  }

  public TimeWindowChanger getTimeWindowChanger() {
    return timeWindowChanger;
  }

  public List<TimeConstraintWindowStorable> toStorables() {
    return definition.toStorables();
  }

  public TimeWindowDefinition getTimeWindowDefinition() {
    return timeWindow.getDefinition();
  }
  
  public boolean isPersistent() {
    return definition.isPersistent();
  }

  public TimeConstraintWindowDefinition getDefinition() {
    return definition;
  }
  
}
