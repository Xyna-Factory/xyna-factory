/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.DatabaseLock;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_ClusterStateChangedException;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;



/**
 * CMAbstract ist die Basisimplementierung des CapacityManagementInterface, 
 * die in CMLocal und CMClustered erweitert wird. Diese werden dann zusammen mit CMUnsupported 
 * im CapacityManagement verwendet.
 * 
 * TODO: XynaProperty mit MaxCardinality, damit nicht zu gro�e Capacity-Cardinalities angelegt 
 * oder ge�ndert werden. Grund ist die neue Implementation des CapacityCaches mit CapacityInstances.
 * 
 */
public abstract class CMAbstract implements CapacityManagementInterface {

  public static final String TRIED_TO_AQUIRE_TWICE_EXCEPTION_MESSAGE =
      "Tried to aquire capacities more than once for the following order: ";

  protected CapacityCache cache;
  
  protected Logger logger;
  
  protected ODS ods;
  
  protected int ownBinding;
  
  protected CapacityStorableQueries capacityStorableQueries;
  
  //Locks in dieser Reihenfolge (cacheLock,managementLock) holen! 
  //Leider zuerst das teure managementLock, da sonst �ber die
  //Remote-Aufrufe, die ein CacheLock brauchen, ein DeadLock entstehen kann (Bug 11971)
  //Sicherung gegen �nderungen an Capacity-Namen, Anzahl etc.
  protected DatabaseLock managementLock;
  // threadsicherer zugriff auf interne arrays
  protected ReentrantLock cacheLock;


  public CMAbstract(ODS ods, CapacityCache cache, int ownBinding, CapacityStorableQueries capacityStorableQueries,
                    DatabaseLock managementLock) {
    this.logger = CentralFactoryLogging.getLogger(getClass());
    this.ods = ods;
    this.cache = cache;
    this.cacheLock = cache.getLock();
    this.ownBinding = ownBinding;
    this.capacityStorableQueries = capacityStorableQueries;
    this.managementLock = managementLock;
  }


  protected abstract List<Integer> getAllBindings();


  protected abstract void refreshRemoteCapacityCache(String capName);


  public abstract boolean isClustered();


  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData) {
    internalCheckAllocate( orderInformation, schedulingData, this, logger );

    if( ! schedulingData.needsCapacities() ) {
      schedulingData.setHasAcquiredCapacities(true);
      return CapacityAllocationResult.SUCCESS;
    }
 
    cacheLock.lock(); //Lock, um �nderungen am CapacityCache durch andere Threads auszuschlie�en, bis Allocation fertig ist
    try {
      if( schedulingData.getTransferCapacities() != null ) {
        transferCapacities(schedulingData.getTransferCapacities(),orderInformation);
      }
      
      Map<String, Integer> previouslyAllocated = cache.getAllocatedCapacities(orderInformation.getOrderId());
      
      CapacityAllocationResult car = allocateCapacities(previouslyAllocated,schedulingData.getCapacities(),orderInformation);
      if( car != null ) {
        return car;
      }
                
      if( schedulingData.getMultiAllocationCapacities() != null ) {
        logger.trace( "MultiAllocationCapacities set");
        
        MultiAllocationCapacities mac = schedulingData.getMultiAllocationCapacities();
        car = multiAllocateCapacities(mac,orderInformation);
        
        if( car != null ) {
          return car;
        }
      }
      
      schedulingData.setHasAcquiredCapacities(true);
    } finally {
      cacheLock.unlock();
    }

    if (logger.isTraceEnabled()) {
      logger.trace("allocated capacities for order [hasAquiredCaps=" + schedulingData.isHasAcquiredCapacities() + "]" + orderInformation);
    }
    return CapacityAllocationResult.SUCCESS;

  }

  //package private, nur f�r CMUnsupported und lokal
  static void internalCheckAllocate(OrderInformation orderInformation, SchedulingData schedulingData, CapacityManagementInterface cmi, Logger logger ) {
    if (schedulingData == null) {
      throw new IllegalArgumentException("SchedulingData may not be null when allocating capacity");
    }
   
    if (schedulingData.isHasAcquiredCapacities() && schedulingData.mustAcquireCapacitiesOnlyOnce() ) {
      logger.warn( TRIED_TO_AQUIRE_TWICE_EXCEPTION_MESSAGE + orderInformation );
      cmi.forceFreeCapacities( orderInformation.getOrderId() );
      schedulingData.setHasAcquiredCapacities(false);
    }

  }
  
  /**
   * @param previouslyAllocated
   * @param capacities
   * @param orderInformation
   * @return
   */
  private CapacityAllocationResult allocateCapacities(Map<String, Integer> previouslyAllocated, 
                                                      List<Capacity> capacities, OrderInformation orderInformation) {
    CapacityAllocationResult car = null;
    
    //Allocation f�llen
    ArrayList<CapacityAllocation> allocList = new ArrayList<>();
    for( Capacity cap : capacities ) {
      CapacityAllocation allocation = new CapacityAllocation(cap, previouslyAllocated.get(cap.getCapName()));
      car = allocation.initCache(cache);
      if( car != null ) {
        return car;
      }
      allocList.add(allocation);
    }
    //TODO allocList k�nnte nun transient in schedulingData stehen, dann m�sste dieses Anlegen 
    //nur einmal geschehen. Problem: was wenn Cap aus Cache verschwindet oder hinzukommt oder disabled wird?
    
    //�berpr�fen
    for( CapacityAllocation allocation : allocList ) {
      car = allocation.checkAllocationPossible();
      if( car != null ) {
        return car;
      }
    }

    //Belegen
    for( CapacityAllocation allocation : allocList ) {
      allocation.allocate( orderInformation, false );
    }
    return null;
  }

  private boolean transferCapacities(TransferCapacities transferCapacities, OrderInformation orderInformation) {
    boolean ret = true;
    for( Capacity cap : transferCapacities.getCapacities() ) {
      boolean transfer = cache.transferCaps(cap, transferCapacities.getFromOrderId(), orderInformation, false); //TODO transferable 
      ret = ret && transfer;
    }
    return ret;
  }


  /**
   * @param mac
   * @param orderType
   * @param orderId
   * @return
   */
  private CapacityAllocationResult multiAllocateCapacities(MultiAllocationCapacities mac, OrderInformation orderInformation) {
    CapacityAllocationResult car = null;
    
    //Allocation f�llen
    ArrayList<CapacityAllocation> allocList = new ArrayList<>();
    for( Capacity cap : mac.getCapacities() ) {
      CapacityAllocation allocation = new CapacityAllocation(cap);
      car = allocation.initCache(cache);
      if( car != null ) {
        return car;
      }
      allocList.add(allocation);
    }
    //TODO allocList k�nnte nun transient in schedulingData stehen, dann m�sste dieses Anlegen 
    //nur einmal geschehen. Problem: was wenn Cap aus Cache verschwindet oder hinzukommt oder disabled wird?
    
    for( int allocations=1; allocations<=mac.getMaxAllocation(); ++allocations ) {

      //�berpr�fen
      for( CapacityAllocation allocation : allocList ) {
        car = allocation.checkAllocationPossible();
        if( car != null ) {
          if( allocations <= mac.getMinAllocation() ) {
            return car;
          } else {
            return null;
          }
        }
      }

      //Belegen
      for( CapacityAllocation allocation : allocList ) {
        allocation.allocate( orderInformation, mac.isTransferable() );
      }
      
      mac.setAllocations(allocations);
    }
    return null;
  }
  
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities ) {
    cacheLock.lock();
    try {
      return transferCapacities(transferCapacities, new OrderInformation(xo));
    } finally {
      cacheLock.unlock();
    }
  }

  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED,
  PersistenceLayerException {
    assertCapacityNameNotNull(name);

    if (cardinality < 0) {
      throw new IllegalArgumentException("Cardinality may not be negative");
    }

    if (name.length() == 0) {
      throw new IllegalArgumentException("Empty string is not allowed as capacity name");
    }

    cacheLock.lock();
    try {
      if (cache.get(name) != null) {
        throw new XPRC_CAPACITY_ALREADY_DEFINED(name);
      }
    } finally {
      cacheLock.unlock();
    }

    // Neue CapacityStorables au�erhalb des Locks ziehen, um den Scheduler m�glichst wenig zu beeintr�chtigen

    List<Integer> allBindings = getAllBindings();
    
    CapacityStorables allCs = new CapacityStorables( ownBinding );
    
     int cardForeign = isClustered() ? (cardinality / allBindings.size()) : 0;  // Cardinality der fremden Bindings, 
    //dabei faires Teilen nur im Clustered-Zustand 
    int cardOwn = cardinality - cardForeign * (allBindings.size() - 1); //Rest f�r eigene Cardinality

    for (Integer binding : allBindings) {
      long newId = XynaFactory.getInstance().getIDGenerator().getUniqueId(); // langsam: IDGenerator braucht potentiell IO calls

      CapacityStorable newCapStorable = new CapacityStorable(newId, binding);
      newCapStorable.setName(name);
      newCapStorable.setCardinality(ownBinding == binding.intValue() ? cardOwn : cardForeign);
      newCapStorable.setState(state);
      allCs.add(newCapStorable);
    }

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    boolean closedConnection = false;
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);

      managementLock.lock(); //throws LockFailedException ok, da Aufruf �ber CLI
      try {
        cacheLock.lock();
        try {

          //lokal existierte die Capacity nicht, das wurde schon oben im Cache gepr�ft.
          //Ist sie bereits f�r ein anderes Binding eingerichtet?
          Collection<CapacityStorable> existingCapacities = capacityStorableQueries.loadAllByName(defCon, name);
          if (existingCapacities != null && !existingCapacities.isEmpty()) {
            throw new XPRC_CAPACITY_ALREADY_DEFINED(name);
          }
          for (CapacityStorable newCapStorable : allCs) {
            defCon.persistObject(newCapStorable);
          }
          defCon.commit();
          finallyClose(defCon);
          closedConnection = true;

          cache.refresh(allCs);
          if (!allCs.getOthers().isEmpty()) {
            refreshRemoteCapacityCache(name);
          }

        } finally {
          cacheLock.unlock();
        }
      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          refreshLocalCapacityCache(name);
          throw e;
        }
      }

    } finally {
      if (!closedConnection) {
        finallyClose(defCon);
      }
    }

    notifyScheduler();
    return true;

  }


  private void assertCapacityNameNotNull(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Capacity name may not be null");
    }
  }


  /**
   * Change the name of a capacity
   * @param oldName name identifying the capacity before the transition
   * @param newName name identifying the capacity after the transition
   * @return true if the capacity existed and the name could be changed and false otherwise
   * @throws XPRC_ClusterStateChangedException
   */
  public boolean changeCapacityName(String oldName, String newName) throws PersistenceLayerException,
      XPRC_ClusterStateChangedException {

    if (oldName == null) {
      throw new IllegalArgumentException("Old capacity name may not be null");
    }

    if (newName == null) {
      throw new IllegalArgumentException("New capacity name may not be null");
    }

    if (newName.length() == 0) {
      throw new IllegalArgumentException("Empty string is not allowed as capacity name");
    }

    if (newName.equals(oldName)) {
      return true;
    }

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);
      managementLock.lock(); //throws LockFailedException ok, da Aufruf �ber CLI
      try {

        cacheLock.lock();
        try {

          if (!cache.contains(oldName)) {
            throw new IllegalArgumentException("Unknown capacity: <" + oldName + ">");
          }

          if (cache.contains(newName)) {
            throw new IllegalArgumentException("Could not set capacity name to '" + newName + "' (name already exists)");
          }

          ReliableCapacityInformation rci = new ReliableCapacityInformation(oldName, this);
          CapacityInformation reliableCi = rci.getCapacityInformation(defCon);
          if (reliableCi.getInuse() != 0) {
            logger.info("Tried to rename a capacity that is in use");

            //Wenn der bisherige Status ACTIVE war, mu� der Status nochmal zur�ckgesetzt werden
            rci.resetState(defCon);
            return false;
          }

          //Ok, die Capacity existiert und wird derzeit nicht verwendet, daher umbenennen

          CapacityStorables allCs =
              new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(defCon, oldName), ownBinding);

          if (allCs.isEmpty()) {
            throw new IllegalArgumentException("Unknown capacity: <" + oldName + ">");
          }

          for (CapacityStorable cs : allCs) {
            cs.setName(newName);
            cs.setState(rci.getPreviousState());
          }

          defCon.persistCollection(allCs);
          defCon.commit();
          finallyClose(defCon);

          //fr�here Capacity aus Cache entfernen
          cache.remove(oldName);
          refreshRemoteCapacityCache(oldName);
          //aktuelle Capacity neu laden
          cache.refresh(allCs);
          if (!allCs.getOthers().isEmpty()) {
            refreshRemoteCapacityCache(newName);
          }

          return true;
        } catch (PersistenceLayerException e) {
          // TODO exception handling, logging
          throw e;
        } finally {
          cacheLock.unlock();
        }

      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          refreshLocalCapacityCache(oldName);
          refreshLocalCapacityCache(newName);
          throw e;
        }
      }

    } finally {
      finallyClose(defCon);
    }

  }


  /**
   * Change the cardinality of a capacity
   * @param capName identifying the capacity
   * @param newOverallCardinality after the transition
   * @return true if the capacity existed and the cardinality could be changed and false otherwise
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState
   * @throws XPRC_ClusterStateChangedException
   */
  public boolean changeCardinality(String capName, int newOverallCardinality) throws PersistenceLayerException,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException {
    assertCapacityNameNotNull(capName);

    if (newOverallCardinality < 0) {
      throw new IllegalArgumentException("Capacity cardinality may not be negative");
    }

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);

      managementLock.lock(); //throws LockFailedException ok, da Aufruf �ber CLI
      try {
        cacheLock.lock();
        try {

          if (!cache.contains(capName)) {
            throw new IllegalArgumentException("Unknown capacity: <" + capName + ">");
          }

          CapacityStorables allCs =
              new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(defCon, capName), ownBinding);

          if (allCs.isEmpty()) {
            throw new IllegalArgumentException("Unknown capacity: <" + capName + ">");
          }

          int currentCardinality = allCs.getTotalCardinality();


          if (newOverallCardinality == currentCardinality) {
            //nichts zu tun
          } else if (newOverallCardinality > currentCardinality) {
            //Erh�hen der Cardinality ist immer m�glich
            increaseCaps(defCon, allCs, capName, newOverallCardinality - currentCardinality);
            notifyScheduler();
          } else {
            //Verringern der Cardinality ist nicht immer m�glich -> Abbruch durch Werfen einer Exception
            decreaseCaps(defCon, allCs, capName, currentCardinality - newOverallCardinality);
          }
          return true;
        } catch (PersistenceLayerException e) {
          throw e;
        } finally {
          cacheLock.unlock();
        }
      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          refreshLocalCapacityCache(capName);
          throw e;
        }
      }

    } finally {
      finallyClose(defCon);
    }

  }


  /**
   * Erh�hen der Gesamt-Cardinality um addCard
   * @param defCon
   * @param allCaps
   * @param capName
   * @param addCard
   * @throws PersistenceLayerException
   */
  protected abstract void increaseCaps(ODSConnection defCon, CapacityStorables allCaps, String capName, int addCard)
      throws PersistenceLayerException;


  /**
   * Verringern der Gesamt-Cardinality um removeCard
   * @param defCon
   * @param allCaps
   * @param capName
   * @param removeCard
   * @throws PersistenceLayerException
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain
   * @throws XPRC_ClusterStateChangedException
   */
  protected abstract void decreaseCaps(ODSConnection defCon, CapacityStorables allCaps, String capName, int removeCard)
      throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException;


  /**
   * Changes the state of a capacity
   * @param name identifying the capacity
   * @param newState the target state after the transition
   * @return true, if the capacity exists and the state could be changed and false otherwise
   */
  public boolean changeState(String name, State newState) throws PersistenceLayerException {
    assertCapacityNameNotNull(name);

    if (newState == null) {
      throw new IllegalArgumentException("New state may not be null");
    }

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);

      // TODO suspend/resume orders that use this capacity
      //Besser 3 Zust�nde: ACTIVE, PAUSED, DISABLED
      //ACTIVE wie bisher
      //PAUSED wie bisheriger DISABLED: keine neuen Auftr�ge gescheduledt, aber bisherige d�rfen weiterlaufen
      //                                -> alle Stellen hier anpassen, die momentan DISABLED benutzen
      //DISABLED: Cap darf nicht mehr verwendet werden und alles Auftr�ge, die diese Cap verwenden, 
      //          m�ssen ebenfalls angehalten werden
      managementLock.lock(); //throws LockFailedException ok, da Aufruf �ber CLI
      try {
        cacheLock.lock();
        try {
          if (!cache.contains(name)) {
            // TODO im XML definierte Exception werfen
            throw new IllegalArgumentException("Unknown capacity: <" + name + ">");
          }
          return changeStateInternal(defCon, name, newState);
        } catch (PersistenceLayerException e) {
          logger.error("Could not changeState to " + newState + " for capacity " + name + ": " + e.getMessage(), e);
          return false;
        } finally {
          cacheLock.unlock();
        }
      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          refreshLocalCapacityCache(name);
          throw e;
        }
      }

    } finally {
      finallyClose(defCon);
    }

  }


  /**
   * Internal: alle Locks sind bereits geholt
   * @param name
   * @param newState
   * @throws PersistenceLayerException
   */
  private boolean changeStateInternal(ODSConnection defCon, String name, State newState)
      throws PersistenceLayerException {

    CapacityStorables allCs =
        new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(defCon, name), ownBinding);
    if (allCs.isEmpty()) {
      throw new IllegalArgumentException("Unknown capacity: <" + name + ">");
    }
    for (CapacityStorable cs : allCs) {
      cs.setState(newState);
    }
    defCon.persistCollection(allCs);
    defCon.commit();
   
    cache.refresh(allCs);
    if (!allCs.getOthers().isEmpty()) {
      refreshRemoteCapacityCache(name);
    }

    if (newState == State.ACTIVE) {
      // A capacity has been activated, the orders in the scheduler might want to hear about it
      notifyScheduler();
    }

    return true;

  }


  /**
   * Removes a capacity based on its name
   * @return true, if the capacity existed and could be removed and false otherwise
   * @throws XPRC_ClusterStateChangedException
   */
  public boolean removeCapacity(String capName) throws PersistenceLayerException, XPRC_ClusterStateChangedException {

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);

      managementLock.lock(); //throws LockFailedException ok, da Aufruf �ber CLI
      try {

        cacheLock.lock();
        try {

          if (!cache.contains(capName)) {
            throw new IllegalArgumentException("Unknown capacity: <" + capName + ">");
          }

          ReliableCapacityInformation rci = new ReliableCapacityInformation(capName, this);
          CapacityInformation reliableCi = rci.getCapacityInformation(defCon);
          if (reliableCi.getInuse() != 0) {
            logger.info("Tried to remove a capacity that is in use");

            //Wenn der bisherige Status ACTIVE war, mu� der Status nochmal zur�ckgesetzt werden
            rci.resetState(defCon);
            return false;
          }

          //Ok, die Capacity existiert und wird derzeit nicht verwendet, daher l�schen

          Collection<CapacityStorable> lockedEntriesToBeDeleted =
              capacityStorableQueries.loadAllByNameForUpdate(defCon, capName);

          if (lockedEntriesToBeDeleted == null || lockedEntriesToBeDeleted.isEmpty()) {
            throw new IllegalArgumentException("Capacity <" + capName + "> not found in db");
          }

          if (logger.isDebugEnabled()) {
            debugRemoveCapacity(capName, lockedEntriesToBeDeleted);
          }

          defCon.delete(lockedEntriesToBeDeleted);
          defCon.commit();
          finallyClose(defCon);

          //Eintrag aus den Caches entfernen
          cache.remove(capName);
          refreshRemoteCapacityCache(capName);

          return true;

        } catch (PersistenceLayerException e) {
          // TODO exception handling, logging
          throw e;
        } finally {
          cacheLock.unlock();
        }

      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          refreshLocalCapacityCache(capName);
          throw e;
        }
      }

    } finally {
      finallyClose(defCon);
    }

  }


  private void debugRemoveCapacity(String name, Collection<CapacityStorable> lockedEntriesToBeDeleted) {
    StringBuilder removeCapDebugString = new StringBuilder("Removing capacity '").append(name).append("'");
    Iterator<CapacityStorable> iter = lockedEntriesToBeDeleted.iterator();
    CapacityStorable cs = iter.next();
    if (cs.isClustered(ODSConnectionType.DEFAULT)) {
      removeCapDebugString.append(" for ").append(lockedEntriesToBeDeleted.size()).append(" binding");
      if (lockedEntriesToBeDeleted.size() > 1) {
        removeCapDebugString.append("s");
      }
      removeCapDebugString.append(": ").append(cs.getBinding());
      while (iter.hasNext()) {
        removeCapDebugString.append(", ");
        removeCapDebugString.append(iter.next().getBinding());
      }
    }
    logger.debug(removeCapDebugString);
  }


  public void removeAllCapacities() throws PersistenceLayerException {

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(CapacityStorable.class);

      managementLock.lock(); //throws LockFailedException ok, da Aufruf bislang nur �ber JUnitTest
      try {

        cacheLock.lock();
        try {
          defCon.deleteAll(CapacityStorable.class);
          defCon.commit();
          finallyClose(defCon);

          cache.refresh(new ArrayList<CapacityStorable>(), ownBinding);
          notifyScheduler();
        } finally {
          cacheLock.unlock();
        }

      } finally {
        try {
          managementLock.unlock();
        } catch (AlreadyUnlockedException e) {
          //FIXME alle wieder lesen
          throw e;
        }
      }

    } finally {
      finallyClose(defCon);
    }

  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.capacities.CapacityManagementInterface#freeTransferableCapacities(com.gip.xyna.xprc.XynaOrderServerExtension)
   */
  public boolean freeTransferableCapacities(XynaOrderServerExtension xo) {
    cache.freeCapForOrderIdOnlyTransferable(xo.getId());
    return internalFreeCapacities(xo,cache,logger,true,false);
  }
  
  /**
   * Free up capacities that have previously been allocated to that XynaOrder.
   * unabh�ngig vom r�ckgabewert hat der auftrag danach keine capacities mehr.
   * @return true, falls caps freigegeben wurden, ansonsten false. kann nur false zur�ckgeben, falls auftrag keine capacities entnommen hatte.
   */
  public boolean freeCapacities(final XynaOrderServerExtension xo) {
    return internalFreeCapacities(xo,cache,logger,false,false);
  }
  
  
  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData) {
    internalFreeCapacities(orderInformation,schedulingData,cache,logger,false,true);
  }
  

  //package private, nur f�r CMUnsupported und lokal
  static boolean internalFreeCapacities(XynaOrderServerExtension xo,
                                        CapacityCache cache, Logger logger, 
                                        boolean onlyTransferable, boolean undoAllocation ) {
    if (xo == null) {
      throw new IllegalArgumentException("Cannot free capacities if " +
        XynaOrderServerExtension.class.getSimpleName()+" is null");
    }
    return internalFreeCapacities( new OrderInformation(xo), xo.getSchedulingData(), cache, logger, onlyTransferable, undoAllocation);
  }
  
  //package private, nur f�r CMUnsupported und lokal
  static boolean internalFreeCapacities(OrderInformation orderInformation, SchedulingData schedulingData, 
                                        CapacityCache cache, Logger logger, 
                                        boolean onlyTransferable, boolean undoAllocation ) {

    Long orderId = orderInformation.getOrderId();
    if (logger.isDebugEnabled()) {
      String msg = (undoAllocation?"Undo allocation of ":"Freeing ")+(onlyTransferable?"transferable ":"")+"capacities for order";
      if (logger.isDebugEnabled()) {
        logger.debug( msg + " id " + orderId);
      }
    }
    
    if( undoAllocation ) {
      cache.undoAllocation(orderId);
      schedulingData.setHasAcquiredCapacities(false);
      return true;
    }
 
    //Kein Lock n�tig, cache.freeCapForOrderIdXXX ist threadsafe.
    if( onlyTransferable ) {
      cache.freeCapForOrderIdOnlyTransferable(orderId);
    } else {
      int freed = cache.freeCapForOrderId(orderId);
      if( schedulingData.isHasAcquiredCapacities()) {
        schedulingData.setHasAcquiredCapacities(false);
      } else {
        //eigentlich nichts zu tun, daher nur pr�fen, ob das stimmt
        if( freed == 0 ) {
          if (logger.isDebugEnabled()) {
            logger.debug("No capacities to be freed for order id " + orderId );
          }
          return false;
        } else {
          logger.warn("No capacities to be freed for order id "+orderId+" but freed "+freed+" caps");
        }
      }
    }
    return ! schedulingData.isHasAcquiredCapacities(); //fast immer true="Caps zur�ckgegeben", au�er bei onlyTransferable=true    
  }


  public boolean forceFreeCapacities(final long orderId) {
    return CMAbstract.internalForceFreeCapacities(orderId, cache, logger);
  }


  //package private, nur f�r CMUnsupported und lokal
  static boolean internalForceFreeCapacities(long orderId, CapacityCache cache, Logger logger) {
    
    //Kein Lock n�tig, cache.freeCapForOrderId ist threadsafe.
    return cache.freeCapForOrderId(orderId) != 0;
  }


  protected void finallyClose(ODSConnection con) {
    if (con != null) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection", e);
      }
    }
  }


  protected void notifyScheduler() {
    logger.debug("notifyScheduler");
    XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
  }


  public Map<String, CapacityInformation> listLocalCapacities() {
    cacheLock.lock();
    try {
      return cache.listCapacities();
    } finally {
      cacheLock.unlock();
    }
  }


  public CapacityInformation getLocalCapacityInformation(String capName) {
    cacheLock.lock();
    try {
      if (cache.contains(capName)) {
        CapacityInformation ci = cache.get(capName).getCapacityInformation();
        ci.binding = ownBinding;
        return ci;
      } else {
        throw new IllegalArgumentException("Unknown capacity: <" + capName + ">"); //FIXME vergl CapacityParallelismLimitation.java
      }
    } finally {
      cacheLock.unlock();
    }
  }


  public ExtendedCapacityUsageInformation getLocalExtendedCapacityUsageInformation() {
    cacheLock.lock();
    try {
      return cache.getExtendedCapacityUsageInformation();
    } finally {
      cacheLock.unlock();
    }
  }


  /**
   * ReliableCapacityInformation bietet eine "verl�ssliche" CapacityInformation an. Verl�sslich hei�t, dass sich die
   * Anzahl der benutzten Capacities nicht erh�hen kann. Dies kann bei einer Clustere-Implementierung nur erreicht
   * werden, indem die Capacity-Vergabe kurz angehalten wird (State wird auf DISABLED gesetzt). Es wird davon
   * ausgegangen, dass die lokalen Locks beide schon geholt wurden.
   */
  protected static class ReliableCapacityInformation {

    private String capName;
    private CMAbstract cmAlgorithm;
    private State previousState;


    public ReliableCapacityInformation(String capName, CMAbstract cmAbstract) {
      this.capName = capName;
      this.cmAlgorithm = cmAbstract;
    }


    public void resetState(ODSConnection defCon) throws PersistenceLayerException {
      CapacityInformation localCi = cmAlgorithm.getLocalCapacityInformation(capName);
      if (localCi.getState() != previousState) {
        cmAlgorithm.changeStateInternal(defCon, capName, previousState);
      }
    }


    public State getPreviousState() {
      return previousState;
    }


    public CapacityInformation getCapacityInformation(ODSConnection defCon) throws PersistenceLayerException,
        XPRC_ClusterStateChangedException {
      CapacityInformation localCi = cmAlgorithm.getLocalCapacityInformation(capName);
      previousState = localCi.getState();
      if (cmAlgorithm.isClustered()) {
        if (previousState == State.ACTIVE) {
          //aktive Caps k�nnen noch vom Scheduler remote vergeben werden, lokal nicht mehr
          //daher nun Statuswechsel auf DISABLED, damit auch remote die Capacity nicht vergeben wird
          cmAlgorithm.changeStateInternal(defCon, capName, State.DISABLED);
        }
        //nun kann sicher die CapacityInformation gelesen werden
        return cmAlgorithm.getCapacityInformation(capName);
      } else {
        return localCi;
      }
    }

  }


  public void refreshLocalCapacityCache(String capName) {

    //Achtung: kein managementLock, da keine �nderung in der DB; Aufrufer hat (evtl. Remote) bereits 
    //das managementLock erhalten
    if (logger.isDebugEnabled()) {
      logger.debug("refreshLocalCapacityCache(" + capName + ")");
    }
    CapacityStorables allCs;
    try {
      allCs = readCap(capName);
    } catch (Exception e) {
      logger.error("Could not read capacityStorable-entry \"" + capName + "\", possibly capacity-loss in cache", e);
      return;
    }

    cacheLock.lock();
    try {
      if (allCs != null) {
        if (allCs.isEmpty()) {
          logger.debug("Read no capacities, assuming they are removed");
          cache.remove(capName);
          return;
        }
        if (allCs.getOwn() == null) {
          logger
              .error("Could not read own capacityStorable-entry \"" + capName + "\", possibly capacity-loss in cache");
          return;
        }

        cache.refresh(allCs);
        if (logger.isDebugEnabled()) {
          logger.debug("refreshLocalCapacityCache -> " + cache.get(capName));
        }
        notifyScheduler();
      } else {
        cache.remove(capName);
        //Scheduler muss nicht benachrichtigt werden
      }
    } finally {
      cacheLock.unlock();
    }
  }


  private CapacityStorables readCap(String capName) throws Exception {
    int retry = 0;
    Exception retryException;
    do {
      ODSConnection con = ods.openConnection();
      try {
        //normaler Ausstieg hier
        return new CapacityStorables(capacityStorableQueries.loadAllByName(con, capName), ownBinding);
      } catch (XNWH_RetryTransactionException e) { // do not use WarehouseRetryExecutor as we are retrying forever with special conditions and exception handling
        retryException = e;
      } catch (PersistenceLayerException e) {
        if (e.getCause() instanceof NoConnectionAvailableException) {
          retryException = e;
        } else {
          throw e;
        }
      } finally {
        finallyClose(con);
      }

      if (retry == 0) {
        logger.error("Could not read capacityStorable-entry \"" + capName + "\" -> retry: "
            + retryException.getMessage());
      }

      if (retryReadCap(retry)) {
        //Eintrag in DB sollte eigentlich lesbar sein, d.h. weiter Retries
        Thread.sleep((long) (Math.random() * 1000));
        ++retry;
      } else {
        throw retryException;
      }

    } while (true);
  }


  protected abstract boolean retryReadCap(int retry);



  public void close() {
    //nichts zu tun
  }
  
  public CapacityReservation getCapacityReservation() {
    return null; //sinnvoller Default f�r nicht geclusteret: keine CapacityReservation
  }

  
}
