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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.PendingType;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.VetoType;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;

public class VetoCacheEntry {
  
  public static enum RemovalResult {
    IGNORE, EMPTY, STILL_USED
  }
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VetoCacheEntry.class);
  
  private final AtomicEnum<State> state;  
  private final String name;
  private final VetoHistory vetoHistory;
  
  private volatile VetoInformation vetoInformation; //Thread(VCP, S, andere); volatile nur auf Verdacht
  private volatile long urgency;               //Thread(VCP, S, andere); volatile nur auf Verdacht
  private volatile WaitingOrder waitingOrder;  //Thread(VCP, S); volatile nur auf Verdacht
  private boolean keepUsable; //Thread(S). Veto wurde im Status "usable" angetroffen, aber nicht geschedult. 
  //Da es im aktuellen Schedulerlauf fast benötigt worden ist, soll es bis zum nächsten Schedulinglauf 
  //"usable" bleiben
  private volatile boolean replicated; //Thread(VCP), volatile nur auf Verdacht
  
  private VetoCacheEntry(String name, State state, VetoHistory vetoHistory) {
    this.name = name;
    this.state = new AtomicEnum<>(State.class,state);
    this.vetoHistory = vetoHistory;
  }
  
  public VetoCacheEntry(String name, State state) {
    this( name, state, VetoHistory.create(state) );
    this.urgency = Long.MIN_VALUE;
  }
  
  public VetoCacheEntry(String name, State state, long urgency) {
    this( name, state, VetoHistory.create(state) );
    this.urgency = urgency;
  }
  
  public VetoCacheEntry(String name, State state, boolean replicated, VetoInformation vetoInformation) {
    this( name, state, VetoHistory.createReplicated(state, vetoInformation) );
    this.urgency = Long.MIN_VALUE;
    setVetoInformation(vetoInformation);
    this.replicated = replicated;
  }

  //package private
  void setVetoInformation(VetoInformation vetoInformation) {
    if( vetoInformation != null && vetoInformation.isAdministrative() ) {
      this.urgency = Long.MAX_VALUE;
    }
    this.vetoInformation = vetoInformation;
  }
  
  public void addAdministrativeData(VetoInformation vetoInformation) {
    if( vetoInformation.isAdministrative() ) {
      setVetoInformation(vetoInformation);
    }
  }

  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("VetoCacheEntry(").append(state.get()).append(": ");
    VetoInformation vi = vetoInformation;
    sb.append( vi == null ? name : vi.toString() ).append(", ");
    if( urgency != Long.MIN_VALUE ) {
      if( urgency == Long.MAX_VALUE ) {
        //sb.append( "admin, ");
      } else {
        sb.append( urgency).append(", ");
      }
    }
    //sb.append("binding=").append(getBinding());
    sb.append(waitingOrder);
    vetoHistory.appendTo(sb, ", ");
    sb.append(")");
    return sb.toString();
  }

  public VetoInformation getVetoInformation() {
    //gerufen von verschiedenen Threads
    return vetoInformation;
  }

  public String getName() {
    return name;
  }

  public State getState() {
    return state.get();
  }
  
  public boolean isAdministrative() {
    //gerufen von verschiedenen Threads
    VetoInformation vi = vetoInformation;
    if( vi == null ) {
      return false; 
    } else {
      return vi.isAdministrative();
    }
  }
  
 public int getBinding() {
   //gerufen von VetoCacheProcessorThread
   VetoInformation vi = vetoInformation;
   if( vi == null ) {
     return -1; 
   } else {
     return vi.getBinding();
   }
  }
  

  public boolean checkAllocation(AllocationRequest req, long urgency) {
    //gerufen von SchedulerThread
    switch( state.get() ) {
    case Scheduling:
    case Used:
    case Scheduled:
      if (req.getVetoType() == VetoType.SHARED) {
        return checkAllocationForShared(req);
      }
      return checkAllocationForExclusive(req);
    case Usable:
      this.keepUsable = true;
      return true;
    default:
      return false;
    }
  }
  
  
  private boolean checkAllocationForExclusive(AllocationRequest req) {
    VetoInformation vi = vetoInformation;
    if (vi == null) {
      // Unexpected for states Scheduling, Used or Scheduled
      return false;
    }
    if (vi.getUsingOrderId() != null) {
      if (Objects.equals(vi.getUsingOrderId(), req.getOrderInformation().getOrderId())) {
        return true;
      }
      if (vi.getPendingExclusiveOrderId() != null) {
        if (Objects.equals(vi.getPendingExclusiveOrderId(), req.getOrderInformation().getOrderId())) {
          return false;
        }
      }
      req.setPendingType(PendingType.PENDING);
      return false;
    }
    return false;
  }

  
  private boolean checkAllocationForShared(AllocationRequest req) {
    VetoInformation vi = vetoInformation;
    if (vi == null) {
      // Unexpected for states Scheduling, Used or Scheduled
      return false;
    }
    if (vi.getUsingOrderId() != null) { 
      return false;
    }
    if (vi.getPendingExclusiveOrderId() != null) {
      return false;
    }
    if (vi.getSharedOrderIds() == null) {
      // Unexpected, wait for veto processor action
      return false;
    }
    return true;
  }
  
  
  public boolean allocate(VetoInformation vetoInformation, long urgency) {
    //gerufen vom SchedulerThread
    if( compareAndSetState(State.Usable, State.Scheduling )) {
      this.vetoInformation = vetoInformation;
      removeWaiting(urgency);
      this.urgency = urgency;
      return true;
    } else if (state.is(State.Scheduling)) {
      if (this.vetoInformation == null) {
        return false;
      }
      this.vetoInformation = vetoInformation;
      return true;
    } else if ( state.isIn(State.Scheduled, State.Used) ) {
      if( this.vetoInformation == null ) {
        return false; //darf nicht vorkommen
      } else {
        if( this.vetoInformation.getUsingOrderId().equals(vetoInformation.getUsingOrderId() ) ) {
          //Auftrag hat Veto bereits belegt, dies ist erlaubt
          if(  this.vetoInformation.getBinding() != vetoInformation.getBinding() ) {
            this.vetoInformation = vetoInformation; //korrigiert Binding (nach Übernahme vom andern Knoten, bei Restart)
            compareAndSetState(State.Used, State.Scheduled); //nochmal speichern, da Binding geändert
          }
          return true;
        }
      }
      this.vetoInformation = vetoInformation;
      state.set(State.Scheduling);
    }
    return false; //kann eigentlich nicht vorkommen, da nur Scheduler aus Usable entfernen darf
  }
  
  public void undoAllocation(OrderInformation orderInformation) {
    //vom Scheduler-Thread aufgerufen
    VetoInformation vi = vetoInformation;
    if( vi == null ) {
      return; //kann nicht allokiert sein
    }
    if( vi.getUsingOrderId().equals(orderInformation.getOrderId() ) ) {
      compareAndSetState(State.Scheduling, State.Usable);
    }
  }
  
  public void updateWaiting(long urgency, long currentSchedulingRun) {
    //gerufen von SchedulerThread
    WaitingOrder wo = waitingOrder;
    if ( wo == null ) {
      waitingOrder = new WaitingOrder(urgency, currentSchedulingRun);
    } else {
      wo.update(urgency, currentSchedulingRun);
    }
  }
  
  private void removeWaiting(long urgency) {
    //gerufen von SchedulerThread
    if ( waitingOrder != null ) {
      waitingOrder.removeWaiting(urgency);
    }
  }

  

  //package private
  boolean compareAndSetState(State expect, State update) {
    //gerufen von beliebigen Threads
    if( state.compareAndSet(expect, update) ) {
      vetoHistory.update(expect, update);
      return true;
    }
    return false;
  }

  public boolean isUsedBy(long orderIdIn) {
    //wird von beliebigen Threads verwendet!
    VetoInformation vi = vetoInformation;
    if (vi == null) { return false; }
    boolean success = false;
    Long orderId = Long.valueOf(orderIdIn);
    success = Objects.equals(vi.getUsingOrderId(), orderId);
    success = success || Objects.equals(vi.getPendingExclusiveOrderId(), orderId);
    if (vi.getSharedOrderIds() != null) {
      success = success || vi.getSharedOrderIds().contains(orderId);
    }
    return success;
  }
  
  
  public RemovalResult freeOrderId(long orderIdIn) {
    if (!state.isIn(State.Scheduling, State.Scheduled, State.Used)) {
      return RemovalResult.IGNORE;
    }
    if (getVetoInformation() == null) {
      return RemovalResult.IGNORE;
    }
    boolean toChange = false;
    Long orderId = Long.valueOf(orderIdIn);
    VetoInformation vi = getVetoInformation();
    OrderInformation usingOrder = vi.getUsingOrder();
    Long pending = vi.getPendingExclusiveOrderId();
    List<Long> sharedIds = vi.getSharedOrderIds();
    if (Objects.equals(orderId, vi.getUsingOrderId())) {
      usingOrder = null;
      toChange = true;
    }
    if (Objects.equals(orderId, pending)) {
      pending = null;
      toChange = true;
    }
    if (sharedIds != null) {
      if (sharedIds.contains(orderId)) {
        sharedIds = new ArrayList<>(sharedIds);
        sharedIds.remove(orderId);
        toChange = true;
      }
    }
    if (toChange) {
      VetoInformation viNew = new VetoInformation(vi.getName(), usingOrder, sharedIds, pending, vi.getDocumentation(),
                                                System.currentTimeMillis(), vi.getBinding());
      setVetoInformation(viNew);
      vi = viNew;
    }
    if (vi.isAllocatedExclusive() || vi.isAllocatedShared() || vi.isPendingExclusiveAllocation()) {
      return RemovalResult.STILL_USED;
    }
    return RemovalResult.EMPTY;
  }
  
  
  public boolean free() {
    //wird von beliebigen Threads verwendet!
    if( compareAndSetState(State.Used, State.Free) ) {
      //häufigster Fall
      this.urgency = Long.MIN_VALUE;
      return true;
    } else {
      //nochmal alle in der korrekten Reihenfolge, wenn Auftragsbearbeitung schneller war als Scheduler und VetoCacheProcessor
      if( compareAndSetState(State.Scheduling, State.Free) || 
          compareAndSetState(State.Scheduled, State.Free) || 
          compareAndSetState(State.Used, State.Free) ) {
        this.urgency = Long.MIN_VALUE;
        return true;
      }
      //ungültiger Aufruf: Veto kann nicht freigegeben werden
      return false;
    }
  }
  
  public boolean remoteFree() {
    //wird von remote-Thread gerufen, analog zu free() müssen mehrere States untersucht werden
    if( compareAndSetState(State.Scheduled, State.Free) || 
        compareAndSetState(State.Used, State.Free) ) {
      this.urgency = Long.MIN_VALUE;
      return true;
    } else {
      //ungültiger Aufruf: Veto kann nicht freigegeben werden
      return false;
    }
  }
  
  public boolean hasWaiting() {
    //gerufen von VetoCacheProcessorThread
    if( waitingOrder == null ) {
      return false;
    }
    //hier gibt es keine Aktualitätsprüfung, falls der wartende Auftrag nicht mehr existieren sollte.
    //D.h das Veto macht unnötige Übergänge Removing -> Local -> Usable -> Unused -> Removing -> None 
    //statt Removing -> None. 
    //Im Cluster kommt dann noch unnötige Cluster-Kommunikation hinzu:
    return waitingOrder.isWaiting();
  }
  
  public boolean waitingFromTo(State expected, State update) {
    //gerufen von VetoCacheProcessorThread
    if( compareAndSetState(expected, update) ) {
      if( hasWaiting() ) {
        urgency = waitingOrder.remove();
      }
      return true;
    }
    return false;
  }
  

  private static class WaitingOrder {
    private long schedulingRun;
    private volatile long urgency;
    private int count;
    
    public WaitingOrder(long urgency, long currentSchedulingRun) {
      this.urgency = urgency;
      this.schedulingRun = currentSchedulingRun;
      this.count = 1;
    }
    
    @Override
    public String toString() {
      if( count == 0 ) {
        return "WaitingOrder()";
      } else if( urgency == Long.MIN_VALUE ) {
        return "WaitingOrder(count="+count+")";
      } else {
        return "WaitingOrder("+urgency+", count="+count+")";
      }
    }

    public void update(long urgency, long currentSchedulingRun) {
      //wird im Scheduler-Thread aufgerufen (beliebiger State)
      if( currentSchedulingRun > this.schedulingRun) {
        //waiting wurde in früherem Schedulinglauf gesetzt, ist daher evtl. veraltet und kann neu gesetzt werden
        this.schedulingRun = currentSchedulingRun;
        this.urgency = urgency;
        this.count = 1;
      } else {
        //waiting wurde in gleichem Schedulinglauf gesetzt, daher ist bereits höchste urgency eingetragen
        this.count++;
      }
    }

    public void removeWaiting(long urgency) {
      //wird im Scheduler-Thread aufgerufen (State.Scheduling)
      if( this.urgency == urgency ) {
        this.count--;
        this.schedulingRun = 0; //erzwingt baldiges Neusetzen der Urgency
        this.urgency = Long.MIN_VALUE;
      }
    }

    public long remove() {
      //wird im VetoCacheProcessor-Thread aufgerufen (State.Removed)
      this.schedulingRun = 0; //erzwingt baldiges Neusetzen der Urgency
      this.count--;
      long u = this.urgency;
      this.urgency = Long.MIN_VALUE;
      return u;
    }

    public boolean isWaiting() {
      //wird im VetoCacheProcessor-Thread aufgerufen
      return count > 0;
    }
    
  }
  
  public long getUrgency() {
    //gerufen von VetoCacheProcessorThread und RMI
    return urgency;
  }


  public void removeVetoInformation() {
    //gerufen von VetoCacheProcessorThread
    this.vetoInformation = null;
  }
  
  public void setLocalToUsable() {
    //gerufen vom SchedulerThread
    if( compareAndSetState(State.Local, State.Usable) ) {
      keepUsable = false;
    }
  }

  public boolean setUsableToCompare() {
    //gerufen vom SchedulerThread
    if( keepUsable ) {
      keepUsable = false;
      return false;
    }
    if( getState() == State.Usable ) {
      removeWaiting(urgency);
    }
    return prepareCompare(State.Usable, false);
  }

  public boolean setScheduled(State expected, VetoInformation vetoInformation) {
    //gerufen von VetoCacheProcessorThread
    if( compareAndSetState( expected, State.Scheduled ) ) {
      setVetoInformation(vetoInformation);
      vetoHistory.binding(vetoInformation.getBinding());
      return true;
    }
    return false;
  }

  public boolean isReplicated() {
    return replicated;
  }
  
  public VetoHistory getHistory() {
    return vetoHistory;
  }

  public boolean replace(State expected, VetoCacheEntry vetoNew) {
    if( compareAndSetState( expected, vetoNew.getState() ) ) {
      this.vetoInformation = vetoNew.vetoInformation;
      this.urgency = vetoNew.urgency;
      this.waitingOrder = vetoNew.waitingOrder;
      this.keepUsable = vetoNew.keepUsable;
      this.vetoHistory.append("Replaced:"+vetoNew.getState());
      this.replicated = vetoNew.replicated;
      return true;
    }
    return false;
  }
  
  
  public String getVetoHistory() {
    return vetoHistory.toString();
  }

  public boolean prepareCompare(State expect, boolean remote) {
    if( compareAndSetState(expect, remote ? State.Remote : State.Compare) ) {
      if( hasWaiting() ) {
        this.urgency = waitingOrder.remove();
      } else {
        this.urgency = Long.MIN_VALUE;
      }
      return true;
    }
    return false;
  }

}