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

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.utils.concurrent.HashParallelReentrantLock;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintExecutor.TimeWindowChanger;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;


/**
 * Speicherung aller TimeConstraintWindows, Überwachung aller Add-, Remove- und Replace-Vorgänge. 
 *
 */
public class AllTimeConstraintWindows {
  //private static Logger logger = CentralFactoryLogging.getLogger(AllTimeConstraintWindows.class);
  
  private TimeConstraintExecutor timeConstraintExecutor; //Zeitsteuerung
  private AllOrdersList allOrders;
  private ConcurrentHashMap<String,TimeConstraintWindow> timeWindows;
  private HashParallelReentrantLock<String> modifyLock = new HashParallelReentrantLock<String>(8);
  
  public AllTimeConstraintWindows(TimeConstraintExecutor timeConstraintExecutor, AllOrdersList allOrders) {
    this.timeConstraintExecutor = timeConstraintExecutor;
    this.allOrders = allOrders;
    this.timeWindows = new ConcurrentHashMap<String,TimeConstraintWindow>();
  }
  
  public void lock(String name) {
    modifyLock.lock(name);
  }

  public void unlock(String name) {
    modifyLock.unlock(name);
  }

  public boolean addTimeWindow(TimeConstraintWindow timeWindow) {
    String name = timeWindow.getName();
    TimeConstraintWindow existing = timeWindows.putIfAbsent( name, timeWindow);
    if( existing == null ) {
      //TimeWindow konnte neu eingetragen werden
      changeTimeConstraintExecutor( timeWindow, false, true);
      return true;
    } else {
      return false;
    }
  }
  
  public void replaceTimeWindow(TimeConstraintWindow timeWindow) {
    String name = timeWindow.getName();
    TimeConstraintWindow existing = timeWindows.get(name);
    if( existing == null ) {
      addTimeWindow(timeWindow);
      return;
    }
    existing.lock(); //Sicherstellen, dass Scheduler nicht gleichzeitig schedulen möchte
    try {
      //kein richtiges replace hier: Scheduler könnte bereits TimeConstraintWindow existing haben
      //deswegen intern existing durch timeWindow ersetzen
      existing.replaceDescriptionAndWindowWith(timeWindow);
      
      //timeConstraintExecutor muss nun angepasst werden
      changeTimeConstraintExecutor( timeWindow, true, true);
      
      //wartende Aufträge wieder in den Scheduler einstellen
      if( existing.isOpen() ) {
        existing.rescheduleWaitingOrders(allOrders);
      }
    } finally {
      existing.unlock();
    }
  }
  
  public void removeTimeWindow(String name) {
    TimeConstraintWindow timeWindow = timeWindows.remove(name);
    if( timeWindow == null ) {
      return; //bereits entfernt
    }
    timeWindow.lock(); //gegen Verwendung im Scheduler schützen
    try {
      timeWindow.setRemoved(); //kann nun nicht mehr verwendet werden
      //timeConstraintExecutor muss sich nicht mehr um dieses Zeitfenster kümmern
      changeTimeConstraintExecutor( timeWindow, true, false);
      //wartende Aufträge wieder in den Scheduler einstellen
      timeWindow.rescheduleWaitingOrders(allOrders);
    } finally {
      timeWindow.unlock(); //evtl. wartet noch jemand am Lock, der sieht nun Status "Removed"
    }
  }

  public boolean isClosed(String windowName) {
    TimeConstraintWindow tw = timeWindows.get(windowName);
    if( tw == null ) {
      return false;
    } else {
      return tw.isClosed();
    }
  }


  public TimeConstraintWindow getLockedTimeWindow(String windowName) {
    do {
      TimeConstraintWindow tw = timeWindows.get(windowName);
      if( tw == null ) {
        return null;
      }
      tw.lock();
      if( tw.isRemoved() ) {
        tw.unlock();
      } else {
        return tw; //üblicher Ausgang
      }
    } while( true );
  }

  public void fillTimeConstraintManagementInformation(TimeConstraintManagementInformation tcmi) {
    tcmi.timeWindows = new ArrayList<String>();
    for( TimeConstraintWindow tw : timeWindows.values() ) {
      tcmi.timeWindows.add( tw.toString() );
    }
  }

  public boolean hasTimeWindow(String name) {
    return timeWindows.containsKey(name);
  }
  
  private void changeTimeConstraintExecutor(TimeConstraintWindow timeWindow, boolean remove, boolean add) {
    TimeWindowChanger twc = timeWindow.getTimeWindowChanger();
    if( remove && twc != null ) {
      timeConstraintExecutor.remove(twc);
    }
    if( add ) {
      if( twc == null ) {
        twc = new TimeWindowChanger(timeWindow);
      }
      timeConstraintExecutor.add(twc);
    }
  }
  
  public TimeConstraintWindowDefinition getDefinition(String windowName) {
    TimeConstraintWindow tw = timeWindows.get(windowName);
    if( tw == null ) {
      return null;
    } else {
      return tw.getDefinition();
    }
  }

}
