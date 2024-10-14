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

package com.gip.xyna.xprc.xsched.capacities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;


/**
 * CapacityCache enthält Informationen zu allen vorhandenen Capacities in einer 
 * Map (CapacityName -&gt; {@link CapacityEntry}) sowie alle derzeit vergebenen Capacities in einer
 * Map (OrderId -&gt; List&lt;{@link CapacityInstance}&gt;).
 *
 * Über die Liste der vergebenen Capacities kann effizient geprüft werden, ob und welche Capacities 
 * freigegeben werden müssen. 
 *
 */
public class CapacityCache implements Iterable<CapacityCache.CapacityEntry> {

  private static Logger logger = CentralFactoryLogging.getLogger(CapacityCache.class);
  
  private final ReentrantLock cacheLock = new ReentrantLock();
  
  /**
   * Zu jedem CapacityNamen gehören mehrere CapacityInstance-Instanzen, je nach Kardinalität.
   * In dieser CapacityInstance wird protokolliert, ob die Capacity verwendbar ist oder von 
   * welchem Auftrag sie verwendet wird. 
   * 
   * Die Capacity kann folgende Statusübergänge machen:
   * <ul>
   * <li> {@link #allocate(long, String, boolean)}</li>
   * <li> {@link #free()}</li>
   * <li> {@link #reserve(int)}</li>
   * <li> {@link #transfer(long, long, String, boolean)}</li>
   * <li> {@link #delete()}</li>
   * </ul>
   * Diese Statusübergänge wirken auf den CapacityEntry zurück.
   */
  private static class CapacityInstance {
    private static enum State {
      Free(false),   //Capacity ist frei
      Used(true),   //Capacity wird benutzt von orderId, orderType
      Transferable(true), //Capacity wird benutzt von orderId, orderType, ist transferierbar
      Reserved(false),  //Capacity ist reserviert für binding
      Deleted(false)    //Capacity ist gelöscht worden (sollte nie bemerkt werden)
      ;
      
      private boolean used;
      private State(boolean used) {
        this.used = used;
      }
      
      public boolean isUsed() {
        return used;
      } 
    }
    
    private CapacityEntry capacityEntry;
    private int binding;
    private volatile State state;
    private long allocationSchedulingRun;
    private OrderInformation orderInformation;
    
    public CapacityInstance(CapacityEntry capacityEntry) {
      this.capacityEntry = capacityEntry;
      this.orderInformation = null;
      this.state = State.Free;
      this.capacityEntry.incFree();
    }

    public String toString() {
      switch( state ) {
      case Free:
      case Deleted:
        return state.name();
      case Used:
      case Transferable:
        return state.name()+"("+orderInformation+","+capacityEntry.capacityName+")";
      case Reserved:
        return state.name()+"("+binding+")";
      default:
        return "Unknown state "+state.name();
      }
    }
    
    public void allocate(OrderInformation orderInformation, boolean transferable, long currentSchedulingRun ) {
      this.state = transferable ? State.Transferable : State.Used;
      this.orderInformation = orderInformation;
      this.capacityEntry.incUsed();
      this.allocationSchedulingRun = currentSchedulingRun;
    }
    
    public void free() {
      freeInternal(false);
    }
    
    public boolean undoAllocation(long currentSchedulingRun) {
      if( this.allocationSchedulingRun == currentSchedulingRun ) {
        freeInternal(true);
        return true;
      } 
      return false;
    }

    private void freeInternal(boolean undoAllocation) {
      this.orderInformation = null;
      this.binding = 0;
      this.allocationSchedulingRun = 0;
      if( state == State.Free ) {
        logger.warn("Tried to free already freed capacity "+this);
        return;
      }
      this.capacityEntry.decUsed();
      if( state != State.Deleted ) {
        this.state = State.Free;
        if( undoAllocation ) {
          this.capacityEntry.incUsable();
        } else {
          this.capacityEntry.incFree();
        }
      }
    }
    
    public void reserve(int binding) {
      this.state = State.Reserved;
      this.binding = binding;
      this.capacityEntry.incUsed();
    }
    
    
    public void transfer(OrderInformation orderInformation, boolean transferable ) {
      this.state = transferable ? State.Transferable : State.Used;
      this.orderInformation = orderInformation;
    }

    /**
     * 
     */
    public void delete() {
      if( state.isUsed() ) {
        //FIXME sollte nicht vorkommen
        //kein capacityEntry.decUsed(); da diese Cap ja noch verwendet wird und dann beim free capacityEntry.decUsed(); aufgerufen wird
      }
      state = State.Deleted;
    }

    /**
     * @param slotIndex
     * @param privateCapacityIndex
     * @param maxSlotIndex
     * @return
     */
    public CapacityUsageSlotInformation getCapacityUsageSlotInformation(int slotIndex, int privateCapacityIndex,
                                                                        int binding, int maxSlotIndex) {
      CapacityUsageSlotInformation cusi = new CapacityUsageSlotInformation(
        capacityEntry.getCapName(), 
        privateCapacityIndex, 
        state.isUsed(), 
        orderInformation == null ? null : orderInformation.getOrderType(), 
        orderInformation == null ? ExtendedCapacityUsageInformation.ORDER_ID_FOR_UNOCCUPIED_CAPACITY : orderInformation.getOrderId(),
        state == State.Transferable,
        binding,
        slotIndex, 
        maxSlotIndex);
      return cusi;
    }

    /**
     * @return
     */
    public boolean isReserved() {
      return state == State.Reserved;
    }
    
    public boolean isFree() {
      return state == State.Free;
    }
    /*
    public boolean isTransferable() {
      return state == State.Transferable;
    }*/

    public String getCapName() {
      return capacityEntry.getCapName();
    }

    public boolean belongsTo(CapacityEntry ce) {
      return this.capacityEntry == ce;
    }

  }
  
  public static abstract class AbstractCapacityEntryInformation {
    protected String capacityName;
    protected int totalCardinality;
    protected int ownCardinality;
    protected State state;
    protected int usable; //zum Schedulen verwendbare Capacity-Anzahl.
    protected boolean reservedForOtherBinding = false;
    
    public AbstractCapacityEntryInformation() {}
    
    public AbstractCapacityEntryInformation(String capacityName, int totalCardinality, int ownCardinality,
                                    State state, int usable, int reserved) {
      this.capacityName = capacityName;
      this.totalCardinality = totalCardinality;
      this.ownCardinality = ownCardinality;
      this.state = state;
      this.usable = usable;
    }

    public AbstractCapacityEntryInformation(AbstractCapacityEntryInformation ce) {
      this.capacityName = ce.capacityName;
      this.totalCardinality = ce.totalCardinality;
      this.ownCardinality = ce.ownCardinality;
      this.state = ce.state;
      this.usable = ce.usable;
    }

    public abstract int getInUse();
    
    public boolean isDisabled() {
      return state == State.DISABLED;
    }

    public CapacityInformation getCapacityInformation() {
      return new CapacityInformation(capacityName, ownCardinality, getInUse(), state);
    }
    
    public boolean hasReservedForOtherBinding() {
      return reservedForOtherBinding;
    }

    @Override
    public String toString() {
      return "CapacityEntryInformation("+capacityName+","+ownCardinality+","+state+","+getInUse()+")";
    }

    public int getTotalCardinality() {
      return totalCardinality;
    }

    public int getOwnCardinality() {
      return ownCardinality;
    }
    
    public int getOtherCardinality() {
      return totalCardinality - ownCardinality;
    }

    public String getCapName() {
      return capacityName;
    }

    /**
     * @return Anzahl der freien Caps, die zum Schedulen werwendet werden können
     */
    public int getNumberOfFreeCapsForScheduling() {
      return usable;
    }
    
    /**
     * @return Anzahl aller freien Caps
     */
    public int getNumberOfAllFreeCaps() {
      return ownCardinality-getInUse();
    }


  }
  
  public static class CapacityEntryInformation extends AbstractCapacityEntryInformation {

    private int inUse;

    public CapacityEntryInformation(String capacityName, int totalCardinality, int ownCardinality,
                                            State state, int usable, int freed, int reserved) {
      super(capacityName, totalCardinality, ownCardinality, state, usable, reserved);
      this.inUse = ownCardinality-usable-freed;
    }

    public CapacityEntryInformation(CapacityEntry ce) {
      super(ce);
      this.inUse = ce.getInUse();
    }

    public CapacityEntryInformation() {
      inUse = 0;
    }

    @Override
    public int getInUse() {
      return inUse;
    }
        
  }

  /**
   * CapacityEntry ist der Eintrag zu jedem CapacityNamen im CapacityCache.
   * CapacityEntry hält alle zu der Capacity bekannten Daten, wie Name, Kardinalität, Status, etc.
   * Es wird eine Liste mit CapacityInstance-Instanzen gehalten, darin finden sich dann Informationen,
   * ob die Capacity gerade von einem Auftrag verwendet wird, frei ist oder für einen anderen Knoten 
   * reserviert ist.
   *
   */
  public static class CapacityEntry extends AbstractCapacityEntryInformation {
    private static final boolean checkInvariant = true;
    private static final boolean checkInvariant_warnLess = false;

    private AtomicInteger usedCounter;
    private AtomicInteger freedCounter;
    private LinkedList<CapacityInstance> capList;
    private Iterator<CapacityInstance> freeCapIter; //effizientere Suche nach freier Cap: gecachter Iterator
    private int binding;
    private CapacityCache cache;
    private long lastFreedCopy; //freigegebene Capacities dürfen nur einmal pro Schedulerlauf den 
                                //verwendbaren Capacities zugeführt werden. Ob dies bereits geschehen ist, wird hier vermerkt.
    
    public CapacityEntry(CapacityCache cache, CapacityStorables capacityStorables) {
      this.cache = cache;
      CapacityStorable own = capacityStorables.getOwn();
      this.capacityName = own.getName();
      this.ownCardinality = own.getCardinality();
      this.state = own.getState();
      this.binding = own.getBinding();
      this.totalCardinality = capacityStorables.getTotalCardinality();
      this.usedCounter = new AtomicInteger(0);
      this.freedCounter = new AtomicInteger(0);
      this.usable = 0;
      this.capList = new LinkedList<CapacityInstance>();
      for( int i=0; i<ownCardinality; ++i ) {
        capList.add( new CapacityInstance(this) );
      }
      this.freeCapIter = capList.iterator();
      
      if( checkInvariant ) checkInvariant();
    }
    
    public void incUsable() {
      ++usable;
    }

    public void decUsed() {
      usedCounter.decrementAndGet();
    }

    public void incUsed() {
      usedCounter.incrementAndGet();
    }

    public void incFree() {
      freedCounter.incrementAndGet();
    }

    private void checkInvariant() {
      for (int i = 0; i < 5; i++) { //racecondition ist selten, dann nochmal probieren
        int sum = usable + usedCounter.get() + freedCounter.get();
        if (sum == ownCardinality) {
          return;
        }
        if (ownCardinality > sum) {
          //wahrscheinlich gibt anderer Thread gerade Capacity frei:
          //dann ist used verringert und freed noch nicht erhöht 
          //um nicht unnötig eine Warnung zu schreiben, wird hier kurz gewartet und dann nochmal gelesen
          if (checkInvariant_warnLess) {
            Thread.yield();
          } else {
            return; //Verringerung der Anzahl unter ownCardinality wird nicht als Problem angesehen
          }
        } else if (i > 1) {
          Thread.yield();
        }
      }
      logger.warn("checkInvariant failed: freed=" + freedCounter.get() + ", usable=" + usable + ", used=" + usedCounter.get()
          + ", ownCardinality=" + ownCardinality, new Exception("called from"));
    }

    /**
     * @param capacityStorables
     */
    public void refresh(CapacityStorables capacityStorables) {
      try {
        CapacityStorable own = capacityStorables.getOwn();
        this.state = own.getState();
        this.binding = own.getBinding();
        this.totalCardinality = capacityStorables.getTotalCardinality();
        int newOwnCardinality = own.getCardinality();
        if( this.ownCardinality != newOwnCardinality ) {
          int change = newOwnCardinality - this.ownCardinality;
          //ownCardinality wurde geändert. Daraus ergeben sich Änderungen
          changeCapList( change );
          if( change > 0 ) {
            //freedCounter ist bereits erhöht worden
          } else {
            //die Anzahl der verwendbaren Caps hat abgenommen, usable kann hier sogar negativ werden!
            usable += change;
          }
          this.ownCardinality = newOwnCardinality;
        }
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }

    /**
     * 
     */
    private void changeCapList(int change) {
      if( change >= 0 ) {
        for( int a=0; a<change; ++a ) {
          capList.add( new CapacityInstance(this) );
        }
        this.freeCapIter = capList.iterator(); //durch add ist Iterator kaputt
      } else {
        //weniger Caps, d.h. freie Caps löschen
        int removed = 0;
        for( int r=0; r <-change; ++r ) {
          CapacityInstance cap = getNextFreeCap();
          if( cap != null ) {
            cap.delete();
            freeCapIter.remove();
            ++removed;
          } else {
            break;
          }
        }
        if( removed < -change ) {
          //Unerwartet: es gibt keine freien Caps, die gelöscht werden könnten, daher auch verwendete entfernen
          logger.warn( "More capacities used than allowed: "+this);
          for( int r=removed; r <-change; ++r ) {
            CapacityInstance cap = capList.remove(0);
            cap.delete();
          }
          this.freeCapIter = capList.iterator(); //durch remove ist Iterator kaputt
        }
      }
    }

    /**
     * Prüft nicht nur, ob genügend Caps vorliegen, sonder besorgt nach Möglichkeit alle vorhandenen
     * @param demand
     * @return
     */
    public boolean checkAllocationPossible(int demand) {
      try {
        if( demand <= usable ) {
          return true; //genügend frei
        } else {
          long csr = cache.getCurrentSchedulingRun();
          if( lastFreedCopy != csr ) {
            lastFreedCopy = csr;
            int freed = freedCounter.getAndSet(0);
            usable += freed;
            //Freed wurde umkopiert auf Usable, daher könnte Allocation nun möglich sein
            return demand <= usable;
          } else {
            //Freed wurde bereits in diesem Scheduling-Lauf umkopiert, dies darf kein zweites Mal geschehen
            return false;
          }
        }
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }

    public boolean allocate(int demand, OrderInformation orderInformation, boolean transferable) {
      try {
        if (logger.isTraceEnabled()) {
          logger.trace("allocate "+capacityName+" for ("+demand+","+orderInformation+","+transferable+")");
        }
        if (checkAllocationPossible(demand)) {
          CapacitesPerOrder usedCaps = cache.getOrCreateUsedCapList(orderInformation.getOrderId());
          CapacityInstance[] allocated = new CapacityInstance[demand];
          for( int d=0; d<demand; ++d ) {
            CapacityInstance cap = getNextFreeCap();
            cap.allocate(orderInformation, transferable, cache.getCurrentSchedulingRun() );
            allocated[d] = cap;
          }
          usedCaps.addAllocatedCapacities(allocated, transferable);
          usable -= demand;
          return true;
        }
        return false;
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }

    /**
     * Reservieren aller zum Schedulen verwendbaren Caps, damit diese nicht für 
     * Aufträge mit niedrigerer Urgency verwendet werden
     */
    public void reserveAllRemainingCaps() {
      try {
        freedCounter.addAndGet(usable);
        usable = 0;
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }
    
    /**
     * @param foreignBinding
     * @param demand
     * @return
     */
    public int reserveForBinding(int foreignBinding, int demand) {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("reserve "+capacityName+" for binding "+foreignBinding);
        }
        if (checkAllocationPossible(demand)) {
          for( int d=0; d<demand; ++d ) {
            getNextFreeCap().reserve(foreignBinding);
          }
          usable -= demand;
          reservedForOtherBinding = true;
          return demand;
        }
        return 0;
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }
   
    /**
     * @return 
     * 
     */
    private CapacityInstance getNextFreeCap() {
      while( freeCapIter.hasNext() ) {
        CapacityInstance cap = freeCapIter.next();
        if( cap.isFree() ) {
          return cap;
        }
      }
      //freeCapIter ist erschöpft, daher Suche von vorne beginnen
      freeCapIter = capList.iterator();
      while( freeCapIter.hasNext() ) {
        CapacityInstance cap = freeCapIter.next();
        if( cap.isFree() ) {
          return cap;
        }
      }
      //unerwartet: es gibt keine freien Caps mehr!
      logger.error( "Expected to find a free capacity "+capacityName);
      logger.error( "capInstances are: "+capList);
      logger.error( "usable="+usable+", used="+usedCounter.get()+", "+this );
      return null;
    }

    public CapacityInformation getCapacityInformation() {
      return new CapacityInformation(capacityName, ownCardinality, usedCounter.get(), state);
    }

    public void fillCapacityUsageSlotInformation(ExtendedCapacityUsageInformation ecui, int privateCapacityIndex) {
      int slotIndex = 0;
      int maxSlotIndex = ownCardinality -1;
      for( CapacityInstance cap : capList ) {
        ecui.addSlotInformation(privateCapacityIndex, cap.getCapacityUsageSlotInformation(slotIndex,privateCapacityIndex,binding,maxSlotIndex) );
        ++slotIndex;
      }
    }

    public long getBinding() {
      return binding;
    }


    public Reservations removeReservations() {
      try {
        Reservations reservations = new Reservations(capacityName);
        for( CapacityInstance cap : capList ) {
          if( cap.isReserved() ) {
            reservations.add(cap.binding);
            cap.free(); //Reservierung aufheben, CapacityInstance wird dann durch ein CapacityEntry.refresh(CapacityStorables) entfernt 
          }
        }
        reservedForOtherBinding = false;
        return reservations;
      } finally {
        if( checkInvariant ) checkInvariant();
      }
    }

    @Override
    public int getInUse() {
      return usedCounter.get();
    }
    
  }
  
  public static class CapacitesPerOrder {
    
    private List<CapacityInstance> transferables;
    private List<CapacityInstance> notTransferables = new ArrayList<CapacityCache.CapacityInstance>(1);

    public void addAllocatedCapacities(CapacityInstance[] allocated, boolean transferable) {
      if (transferable) {
        synchronized (this) {
          if (transferables == null) {
            transferables = new ArrayList<CapacityCache.CapacityInstance>(allocated.length);
          }
          for (CapacityInstance ci : allocated) {
            transferables.add(ci);
          }
        }
      } else {
        for (CapacityInstance ci : allocated) {
          notTransferables.add(ci);
        }
      }
    }

    public synchronized boolean hasTransferableCapacities() {
      return transferables != null && !transferables.isEmpty();
    }


    public synchronized boolean transferCaps(CapacitesPerOrder toCapList, CapacityEntry ce, Capacity cap, 
                                             OrderInformation toOrder, boolean transferable) {
      Iterator<CapacityInstance> iter = transferables.iterator();
      int transfered = 0;
      while (iter.hasNext()) {
        CapacityInstance ci = iter.next();
        if (ci.belongsTo(ce)) {
          iter.remove(); //aus fromCapList entfernen 
          ci.transfer(toOrder, transferable); //intern umtragen
          if (transferable) {
            synchronized (toCapList) {
              if (toCapList.transferables == null) {
                toCapList.transferables = new ArrayList<CapacityCache.CapacityInstance>(cap.getCardinality());
              }
              toCapList.transferables.add(ci);
            }
          } else {
            toCapList.notTransferables.add(ci);
          }
          ++transfered;
          if (transfered == cap.getCardinality()) {
            return true; //gewünschte Anzahl transferiert
          }
        }
      }
      return false; //weniger als gewünschte Anzahl transferiert

    }

    public CapacityInstance[] getNotTransferableCapacities() {
      return notTransferables.toArray(new CapacityInstance[notTransferables.size()]);
    }

    public synchronized CapacityInstance[] getTransferableCapacities() {
      if (transferables == null) {
        return null;
      }
      return transferables.toArray(new CapacityInstance[transferables.size()]);
    }


    public synchronized Map<String, Integer> getAllCapsAsMap() {
      Map<String, Integer> map = new HashMap<String, Integer>();
      if (transferables != null) {
        for (CapacityInstance cap : transferables) {
          Integer existing = map.put(cap.getCapName(), Integer.valueOf(1));
          if (existing != null) { //TODO Allocations mit Cardinality > 1 sind nicht so effizient hier
            map.put(cap.getCapName(), Integer.valueOf(existing.intValue() + 1));
          }
        }
      }
      for (CapacityInstance cap : notTransferables) {
        Integer existing = map.put(cap.getCapName(), Integer.valueOf(1));
        if (existing != null) { //TODO Allocations mit Cardinality > 1 sind nicht so effizient hier
          map.put(cap.getCapName(), Integer.valueOf(existing.intValue() + 1));
        }
      }
      return map;
    }
    
    
    @Override
    public String toString() {
      return "transferables: " + transferables + Constants.LINE_SEPARATOR + "notTransferables: " + notTransferables;
    }

    public synchronized List<CapacityInstance> removeTransferableCapacities() {
      List<CapacityInstance> transferables = this.transferables;
      this.transferables = null;
      return transferables;
    }

    public List<CapacityInstance> removeNotTransferableCapacities() {
      List<CapacityInstance> notTransferables = this.notTransferables;
      this.notTransferables = new ArrayList<CapacityCache.CapacityInstance>(1);
      return notTransferables;
    }

    public int undoAllocation(long currentSchedulingRun) {
      int cnt = 0;
      
      //normale Caps, sollten alle aus dem aktuellen Schedulerlauf stammen
      List<CapacityInstance> notFreed = new ArrayList<CapacityCache.CapacityInstance>(1);
      for( CapacityInstance ci : notTransferables ) {
        boolean freed = ci.undoAllocation(currentSchedulingRun);
        if( freed ) {
          ++cnt;
        } else {
          notFreed.add(ci); //sollte nicht auftreten
        }
      }
      notTransferables = notFreed;

      synchronized (this) {
        //spezielle Caps, könnten auch in anderen Schedulerläufen allokiert worden sein
        if (transferables != null) {
          notFreed = new ArrayList<CapacityCache.CapacityInstance>(1);
          for (CapacityInstance ci : transferables) {
            boolean freed = ci.undoAllocation(currentSchedulingRun);
            if (freed) {
              ++cnt;
            } else {
              notFreed.add(ci);
            }
          }
          transferables = notFreed;
        }
      }
      
      return cnt;
    }
    
  }
  
  public static class Reservations {

    private String capacityName;
    private int binding = Integer.MIN_VALUE;
    private int count = 0;
    
    public Reservations(String capacityName) {
      this.capacityName = capacityName;
    }

    public void add(int binding) {
      if( this.binding == Integer.MIN_VALUE) {
        this.binding = binding;
        count = 1;
      } else {
        if( this.binding == binding ) {
          ++count;
        } else {
          logger.warn("Unexpected binding "+binding+", expected "+this.binding);
          //FIXME für mehrere Bindings implementieren
        }
      }
    }

    public String getCapacityName() {
      return capacityName;
    }

    public int getReserved(int binding) {
      if( this.binding == binding ) {
        return count;
      } else {
        logger.warn("Unexpected binding "+binding+", expected "+this.binding);
        return 0;
      }
    }
    
    @Override
    public String toString() {
      return "Reservations("+capacityName+",binding="+binding+",count="+count+")";
    }
    
  }  
  
  
  private Map<String, CapacityEntry> cacheMap = new HashMap<String, CapacityEntry>();
  private ConcurrentHashMap<Long,CapacitesPerOrder> usedCaps = new ConcurrentHashMap<Long,CapacitesPerOrder>();
  private long currentSchedulingRun;
  
  public CapacityCache() {
  }


  /**
   * @param orderId
   * @return
   */
  public CapacitesPerOrder getOrCreateUsedCapList(Long orderId) {
    CapacitesPerOrder caps = usedCaps.get(orderId);
    if (caps == null) {
      caps = new CapacitesPerOrder();
      CapacitesPerOrder previous = usedCaps.putIfAbsent(orderId, caps);
      if (previous != null) {
        caps = previous;
      }
    }
    return caps;
  }


  /**
   * Refresh aller CapacityStorable -&gt; im Cache befinden sich anschließend nur die übergebenen CapacityStorables
   * @param capStorables
   * @param ownBinding
   */
  public void refresh(List<CapacityStorable> capStorables, int ownBinding) {
    HashMap<String, CapacityEntry> newCacheMap = new HashMap<String, CapacityEntry>();
    
    HashMap<String, CapacityStorables> csMap = new HashMap<String, CapacityStorables>();
    for(CapacityStorable cs : capStorables) {
      String capName = cs.getName();
      CapacityStorables css = csMap.get(capName);
      if( css == null ) {
        css = new CapacityStorables(ownBinding);
        csMap.put(capName, css);
      }
      css.add(cs);
    }
    
    for( Map.Entry<String,CapacityStorables> entry : csMap.entrySet() ) {
      CapacityStorables css = entry.getValue();
      if( css.getOwn() != null ) {
        newCacheMap.put(entry.getKey(), new CapacityEntry( this, css ) );
      } else {
        logger.warn( "No own CapacityStorable-entry found for capacity "+entry.getKey() +" and binding "+ownBinding );
      }
    }
    cacheMap = newCacheMap;
  }
  
  /**
   * Refresh des einen übergebenen CapacityStorable, evtl. Neuanlage
   * @param capacityStorables
   */
  public void refresh(CapacityStorables capacityStorables) {
    String capName = capacityStorables.getOwn().getName();
    CapacityEntry ce = cacheMap.get(capName);
    if( ce != null ) {
      ce.refresh(capacityStorables);
    } else {
      cacheMap.put(capName, new CapacityEntry(this, capacityStorables));
    }
  }

  public boolean checkAllocationPossible( Capacity cap ) {
    CapacityEntry ce = cacheMap.get(cap.getCapName());
    if( ce == null ) {
      return false;
    }
    return ce.checkAllocationPossible(cap.getCardinality());
  }


  public CapacityEntry get(String capName) {
    return cacheMap.get(capName);
  }


  public int getSize() {
    return cacheMap.size();
  }
  
  public Iterator<CapacityEntry> iterator() {
    return cacheMap.values().iterator();
  }
  
  /**
   * 
   */
  public Map<String, CapacityInformation> listCapacities() {
    Map<String, CapacityInformation> result = new HashMap<String, CapacityInformation>();
    for (Map.Entry<String, CapacityEntry> e : cacheMap.entrySet()) {
      result.put( e.getKey(), e.getValue().getCapacityInformation() );
    }
    return result;
  }


  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    ExtendedCapacityUsageInformation ecui = new ExtendedCapacityUsageInformation();
    int privateCapacityIndex = 0;
    for( CapacityEntry ce : cacheMap.values() ) {
      ce.fillCapacityUsageSlotInformation(ecui,privateCapacityIndex);
      ++privateCapacityIndex;
    }
    return ecui;
  }

  /**
   * @param capName
   */
  public boolean contains(String capName) {
    return cacheMap.containsKey(capName);
  }

  /**
   * @param capName
   */
  public void remove(String capName) {
    cacheMap.remove(capName);
  }


  public ReentrantLock getLock() {
    return cacheLock;
  }

  /**
   * @param capName
   * @return
   */
  public CapacityEntryInformation getCapacityEntryInformation(String capName) {
    CapacityEntry ce = cacheMap.get( capName );
    if( ce == null ) {
      logger.warn("Tried to ask hasAllCaps for unknown capacity");
      return new CapacityEntryInformation();
    }
    return new CapacityEntryInformation(ce);
  }

  /**
   * Lesen der Capacity-Daten aus der DB und Refresh des Caches
   * @param ods
   * @param capacityStorableQueries
   * @param ownBinding
   * @throws PersistenceLayerException
   */
  public void refresh(ODS ods, final CapacityStorableQueries capacityStorableQueries, int ownBinding)
      throws PersistenceLayerException {
    List<CapacityStorable> loadedCapacities = null;

    WarehouseRetryExecutableNoException<List<CapacityStorable>> wre =
        new WarehouseRetryExecutableNoException<List<CapacityStorable>>() {

          public List<CapacityStorable> executeAndCommit(ODSConnection defCon) throws PersistenceLayerException {
            capacityStorableQueries.init(defCon);
            return capacityStorableQueries.loadAll(defCon);
          }
        };

    loadedCapacities =
        WarehouseRetryExecutor.buildCriticalExecutor().
        storable(CapacityStorable.class).
        execute(wre);
        
    refresh(loadedCapacities, ownBinding);
  }
  
  @Override
  public String toString() {
    return "CapacityCache("+cacheMap.values()+")";
  }

  public void setCurrentSchedulingRun(long currentSchedulingRun) {
    this.currentSchedulingRun = currentSchedulingRun;
  }

  public long getCurrentSchedulingRun() {
    return currentSchedulingRun;
  }
  
  /**
   * Versucht, die Capacities zu transferieren.
   * Diese Funktion ist tolerant gegenüber fehlenden Cap-Namen und fehlenden transferierbaren Caps. Die normale 
   * Capacity-Allokation muss danach noch benötigte Capacities besorgen bzw. den Fehler des Capacity-Fehlens behandeln. 
   * @return true, wenn alle Caps transferiert wurden, ansonsten false
   */
  public boolean transferCaps( Capacity cap, long fromOrderId, OrderInformation toOrder, boolean transferable ) {
    CapacityEntry ce = cacheMap.get(cap.getCapName());
    if( ce == null ) {
      return false; //komplette Capacity existiert nicht
    }
    CapacitesPerOrder fromCapList = usedCaps.get(fromOrderId);
    if( fromCapList == null || !fromCapList.hasTransferableCapacities() ) {
      return false; //Auftrag fromOrderId hat keine Caps zu vergeben
    }
    CapacitesPerOrder toCapList = getOrCreateUsedCapList(toOrder.getOrderId());
    
    return fromCapList.transferCaps(toCapList, ce, cap, toOrder, transferable);
  }
  
  
  public int undoAllocation(Long orderId) {
    CapacitesPerOrder capList = usedCaps.get(orderId);
    if( capList == null ) {
      return 0;
    } else {
      return capList.undoAllocation(getCurrentSchedulingRun());
    }
  }
  
  
  /**
   * @param orderId
   * @return Anzahl der freigegebenen Caps
   */
  public int freeCapForOrderId(Long orderId) {
    CapacitesPerOrder capList = usedCaps.remove(orderId);
    if (capList == null) {
      return 0;
    } else {
      int cnt = 0;
      List<CapacityInstance> caps = capList.removeTransferableCapacities();
      if (caps != null) {
        for (CapacityInstance capIni : caps) {
          capIni.free();
        }
        cnt = caps.size();
      }
      caps = capList.removeNotTransferableCapacities();
      for (CapacityInstance capIni : caps) {
        capIni.free();
      }
      cnt += caps.size();
      return cnt;
    }
  }
  
  /**
   * Freigeben aller als transferable belegten Capacities
   * @return Anzahl der nicht freigegebenen Caps
   */
  public int freeCapForOrderIdOnlyTransferable(long orderId) {
    CapacitesPerOrder capList = usedCaps.get(orderId);
    if( capList == null ) {
      return 0;
    } else {
      List<CapacityInstance> transferables = capList.removeTransferableCapacities();
      if (transferables != null) {
        for (CapacityInstance capIni : transferables) {
          capIni.free();
        }
        return capList.getNotTransferableCapacities().length;
      } else {
        return 0;
      }
    }
  }
  
  public Map<String,Integer> getAllocatedCapacities(long orderId) {
    CapacitesPerOrder capList = usedCaps.get(orderId);
    if( capList == null ) {
      return Collections.emptyMap();
    } else {
      return capList.getAllCapsAsMap();
    }
  }

}
