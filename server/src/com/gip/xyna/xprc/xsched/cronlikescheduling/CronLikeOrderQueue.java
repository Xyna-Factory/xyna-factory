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


package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;



public class CronLikeOrderQueue {
  
  /*
   * Priority queue for cron like orders.
   * comparator wird im konstruktor übergeben
   */
  // TODO: wahrscheinlich ist eine sorted Linked List performanter ... vllt. noch ändern
  private final PriorityBlockingQueue<CronLikeOrder> queue;
  
  //Crons, die nicht wieder aus der Datenbank in die Queue geladen werden sollen, weil ...
  private final Set<Long> notToSchedule; //... sie schon aus der Queue, aber noch nicht aus der Datenbank entfernt wurden
  private final Set<Long> deletedOrModified; //... sie inzwischen aus der Datenbank entfernt wurden oder sich der Ausführungszeitpunkt geändert hat, aber der CronLikeTimer sie evtl. bereits ausgelesen hat
  private AtomicBoolean collectDeletedOrModified = new AtomicBoolean(false); //während readNextFromPersistenceLayer müssen die aus der DB gelöschten/geänderten Crons eingesammelt werden
  
  private volatile long latestExecutionTimeInQueue;
  private Integer currentBinding;
  
  private volatile boolean readAllFromDBFlag;
  
  private AtomicBoolean cleared = new AtomicBoolean(false); //gibt an, ob während dem readNextFromPersistenceLayer die Queue geleert wurde
  
  public CronLikeOrderQueue(int initialCapacity, Comparator<CronLikeOrder> comparator) {
    queue = new PriorityBlockingQueue<CronLikeOrder>(initialCapacity, comparator);
    notToSchedule = new HashSet<Long>();
    deletedOrModified = new HashSet<Long>();
    latestExecutionTimeInQueue = -1;
    readAllFromDBFlag = true; //beim einfügen keinen check durchführen, dass der startzeitpunkt des crons größer als latestexecutiontime ist
  }
  
  /**
   * checks:
   * - binding ok?
   * - executiontime weiter in der zukunft als bisherige elemente der queue? (ausser readAllFromDBFlag ist false)
   * - nottoschedule enthält id?
   * 
   */
  public boolean addCronLikeOrderToQueue(CronLikeOrder order) {
    return addCronLikeOrderToQueue(order, false);
  }
  
  public boolean addCronLikeOrderToQueue(CronLikeOrder order, boolean addFromPersistenceLayer) {
    if(currentBinding != null) {
      if(order.getBinding() != currentBinding) {
        return false;
      }
    }
    
    //Crons, die der CronLikeTimer aus der Datenbank ausgelesen hat, sollen auf jeden Fall in die Queue
    //eingefügt werden. Ansonsten nur, wenn alle Crons aus der DB sind auch in der Queue vorhanden sind
    //oder der Ausführungszeitpunkt vor dem letzten in der Queue liegt und die Queue nicht leer ist (latestExecutionTimeInQueue = -1)
    if (!addFromPersistenceLayer) {
      if(!readAllFromDBFlag && latestExecutionTimeInQueue < order.getNextExecution()) {
        return false;
      }
    }
    if(notToSchedule.contains(order.getId())) {
      return false;
    }
    
    //wenn Crons aus der Datenbank wieder in die Queue geladen werden, muss überprüft werden,
    //dass sie nicht bereits gelöscht wurden oder sich ihr Ausführungszeitpunkt geändert hat
    if (addFromPersistenceLayer) {
      if (deletedOrModified.contains(order.getId())) {
        return false;
      }
    }
    
    if (!queue.contains(order)) {
      queue.add(order);
    }
    if(latestExecutionTimeInQueue < order.getNextExecution()) {
      latestExecutionTimeInQueue = order.getNextExecution();
    }
    return true;
  }
  
  public boolean isEmpty() {
    return queue.isEmpty();
  }
  
  public int size() {
    return queue.size();
  }
  
  public void shrink(int newSize) {
    if(queue.size() <= newSize) {
      return;
    }
    ArrayList<CronLikeOrder> tmpList = new ArrayList<CronLikeOrder>(newSize);
    queue.drainTo(tmpList, newSize);
    queue.clear();
    
    latestExecutionTimeInQueue = tmpList.get(tmpList.size() - 1).getNextExecution();
    queue.addAll(tmpList);
  }
  
  
  /**
  * Retrieves and removes the head of this queue, or returns null if this queue is empty.
  */
  public CronLikeOrder poll() {
    if(queue.size() == 1) {
      latestExecutionTimeInQueue = -1;
    }
    return queue.poll();
  }
  
  /**
   * Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
   */
  public CronLikeOrder peek() {
    return queue.peek();
  }
  
  public void clear() {
    queue.clear();
    latestExecutionTimeInQueue = -1;
    cleared.set(true);
  }

  // TODO Performance!!!
  public boolean removeFromQueue(Long orderId) {
    boolean removed = false;
    long newLatestExecutionTime = -1;

    Iterator<CronLikeOrder> iter = queue.iterator(); //achtung: reihenfolge nicht sichergestellt    
    while (iter.hasNext()) {
      CronLikeOrder clo = iter.next();

      //objekt entfernen
      if (!removed && clo.getId().equals(orderId)) {
        iter.remove();
        removed = true;
        if (clo.getNextExecution() != latestExecutionTimeInQueue) {
          //nicht das letzte element => latestexecutiontime muss nicht neu bestimmt werden
          newLatestExecutionTime = latestExecutionTimeInQueue;
          break;
        }
        //latestexecutiontime muss neu bestimmt werden
      }

      //latestexecutiontime neu berechnen (falls notwendig)
      if (clo.getNextExecution() > newLatestExecutionTime) {
        newLatestExecutionTime = clo.getNextExecution();
      }
    }

    latestExecutionTimeInQueue = newLatestExecutionTime;

    return removed;
  }

  
  /**
   * alle crons entfernen, die ein anderes binding haben. 
   */
  public void changeToBinding(Integer binding) {
    currentBinding = binding;

    if (binding != null) {
      long newLatestExecutionTime = -1;
      
      Iterator<CronLikeOrder> iter = queue.iterator();
      while (iter.hasNext()) {
        CronLikeOrder clo = iter.next();

        if (clo.getBinding() != binding) {
          iter.remove();
          continue;
        }

        if (clo.getNextExecution() > newLatestExecutionTime) {
          newLatestExecutionTime = clo.getNextExecution();
        }
      }
      
      latestExecutionTimeInQueue = newLatestExecutionTime;
    }
  }
  
  
  public void markCronLikeOrderAsNotToSchedule(Long orderId) {
    notToSchedule.add(orderId);
  }
  
  public List<CronLikeOrder> getQueue() {
    List<CronLikeOrder> queueList = new ArrayList<CronLikeOrder>(queue);
    Collections.sort(queueList, queue.comparator());
    return queueList;
  }
  
  public void unmarkCronLikeOrderAsNotToSchedule(Long orderId) {
    if (collectDeletedOrModified.get()) {
      //beim Auslesen aus der Datenbank (CronLikeScheduler.readNextFromPersistenceLayer()) darf
      //der Auftrag nicht noch einmal in die Queue eingetragen werden, da bereits gelöscht oder geändert
      deletedOrModified.add(orderId);
    }
    notToSchedule.remove(orderId);
  }
  
  public long getLatestExecutionTimeInQueue() {
    return latestExecutionTimeInQueue;
  }

  
  public void setReadAllFromDBFlag(boolean readAllFromDBFlag) {
    this.readAllFromDBFlag = readAllFromDBFlag;
  }

  /**
   * Startet das Einsammeln der Crons, die bereits aus der DB gelöscht / geändert wurden
   * und setzt cleared auf false.
   */
  public void prepareReadNext() {
    if (!collectDeletedOrModified.get()) {
      deletedOrModified.clear();
      collectDeletedOrModified.set(true); //darf erst nach dem clear auf true gesetzt werden, da ab jetzt neue Crons eingetragen werden können
    }
    cleared.set(false);
  }

  /**
   * Stoppt das Einsammeln der Crons, die bereits aus der DB gelöscht / geändert wurden.
   */
  public void finishReadNext() {
    collectDeletedOrModified.set(false);
  }
  
  public boolean isCleared() {
    return cleared.get();
  }
}
