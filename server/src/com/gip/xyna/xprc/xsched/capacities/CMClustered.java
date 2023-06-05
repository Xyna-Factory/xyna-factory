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



import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.AlreadyUnlockedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface.LockFailedException;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.DatabaseLock;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_ClusterStateChangedException;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.ClusteredScheduler;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntry;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntryInformation;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.Reservations;
import com.gip.xyna.xprc.xsched.scheduling.CapacityDemand;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.FilteredCapacityReservation;



public class CMClustered extends CMAbstract implements ClusteredCapacityManagementInterface, CapacityManagementReservationInterface {

  private final RMIClusterProvider clusterInstance;
  private volatile long clusteredCapManagementInterfaceId;
  private final ClusterContext storableClusterContext;
  private final ClusteredScheduler scheduler;
  private final CapacityReservation capacityReservation;
  private static final long RMI_IS_CLOSED = -1;

  private CMClustered(ODS ods, CapacityCache cache, int ownBinding, 
                     CapacityStorableQueries capacityStorableQueries, DatabaseLock managementLock,
                     ClusterContext rmiClusterContext, ClusterContext storableClusterContext, 
                     ClusteredScheduler scheduler) {
    super(ods, cache, ownBinding, capacityStorableQueries, managementLock);
    this.scheduler = scheduler;
    this.capacityReservation = new FilteredCapacityReservation(this);
    this.clusterInstance = ((RMIClusterProvider) rmiClusterContext.getClusterInstance());
    this.storableClusterContext = storableClusterContext;
  }
  
  public static CMClustered createCMClustered(ODS ods, CapacityCache cache, int ownBinding, 
                     CapacityStorableQueries capacityStorableQueries, DatabaseLock managementLock,
                     ClusterContext rmiClusterContext, ClusterContext storableClusterContext, 
                     ClusteredScheduler scheduler) {
    CMClustered cmc = new CMClustered(ods, cache, ownBinding, capacityStorableQueries, managementLock, rmiClusterContext, storableClusterContext, scheduler);
    //Achtung, Reihenfolge wichtig:
    //1) erst RMI registrieren 
    cmc.clusteredCapManagementInterfaceId = cmc.clusterInstance.addRMIInterface("RemoteCapacityManagement", cmc);
    //2) Jetzt erst darf Scheduler wissen, dass er RMI benutzen darf (ansonsten greift er auf falsche clusteredCapManagementInterfaceId zu )
    scheduler.setCapacityReservation( cmc.capacityReservation );
    if (!XynaFactory.getInstance().isStartingUp()) {
      scheduler.notifyScheduler(); //noch ein notify, damit Scheduler auf jedem Fall einmal mit neuen CapacityReservation läuft
    }
    
    //Wie ist es mit Remote-Aufrufen, wenn CapacityReservation noch nicht im Scheduler ausgetauscht wurde?
    //Nur communicateLocalDemand(...) relevant, alle anderen RMI-Methoden ändern nur Capacities
    //communicateLocalDemand trägt bereits in neue CapacityReservation ein, so dass die Demands nicht verloren gehen.
    return cmc;
  }
  
  
  
  public void close() {
    scheduler.setCapacityReservation(null);
    closeRMI();
  }
  
  public CapacityReservation getCapacityReservation() {
    return this.capacityReservation;
  }

  /**
   * Schließen der RMI-Verbindung: Abmelden der RMIInterface-Implementierung
   */
  private void closeRMI() {
    long temp = clusteredCapManagementInterfaceId;
    if( temp != RMI_IS_CLOSED ) {
      clusteredCapManagementInterfaceId = RMI_IS_CLOSED;
      clusterInstance.removeRMIInterface(temp, 500 );
    }
  }

  @Override
  public boolean isClustered() {
    return true;
  }

  @Override
  protected List<Integer> getAllBindings() {
    CapacityStorable cs = new CapacityStorable();
    try {
      return cs.getClusterInstance(ODSConnectionType.DEFAULT).getAllBindingsIncludingLocal();
    } catch (XNWH_RetryTransactionException e) {
      //Hier kann nun nichts mehr getan werden, der ClusterProvider kennt das Problem
      //und hat den ClusterState bereits gewechselt. 
      throw new RuntimeException("currently not possible", e);
    }
  }
  
  protected int transportReservedCaps(List<Reservations> reservations) {
    //Die Reservierungen wurden ohne ManagementLock ermittelt und an diese Methode übergeben.
    //Daher kann es sein, dass in der Zwischenzeit Änderungen in der DB erfolgt sind, die zu
    //den übergebenen Reservierungen nicht kompatibel sind
    
    try {
      managementLock.lock();
    } catch (LockFailedException e) {
      logger.warn("Could not transportReservedCaps. Lock failed ",e);
      return 0; //Abbruch, da Lock nicht erhalten wurde
    }
    try {
      cacheLock.lock();
      try {
        int transported = 0;
        for( Reservations reservation : reservations ) {
          transported += performTransport(reservation);
        }
        return transported;
      } finally {
        cacheLock.unlock();
      }
    } finally {
      try {
        managementLock.unlock();
      } catch( AlreadyUnlockedException e ) {
        logger.warn("AlreadyUnlockedException ",e); //TODO was nun?
      }
    }
    
  }
  
  private class PerformTransport_WarehouseRetryExecutableNoException implements WarehouseRetryExecutableNoException<Integer> {
    
    private Reservations reservation;

    public PerformTransport_WarehouseRetryExecutableNoException(Reservations reservation) {
      this.reservation = reservation;
    }

    public Integer executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      String failedCause = "";
      String capName = reservation.getCapacityName();
      try {
        CapacityStorables allCs = new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(con, capName),
                                                        ownBinding);
        if (allCs.size() == 0) {
          // can happen e.g. if the capacity has just been removed or something
          failedCause = "nothing found in db";
          return 0;
        }
        CapacityStorable own = allCs.getOwn();

        int transported = 0;
        for (CapacityStorable other : allCs.getOthers()) {
          int reserved = reservation.getReserved(other.getBinding());
          own.setCardinality(own.getCardinality() - reserved);
          other.setCardinality(other.getCardinality() + reserved);
          transported += reserved;
        }
        if( own.getCardinality() < 0 ) {
          failedCause = "impossible reservation "+reservation;
          return 0;
        }
        
        con.persistObject(own);
        con.persistCollection(allCs.getOthers());
        con.commit();
        
        cache.refresh(allCs);
        refreshRemoteCapacityCache(capName);
        return transported;
      } finally {
        if (logger.isDebugEnabled()) {
          if (failedCause.length() > 0) {
            logger.debug("performTransport(" + capName + ") " + "failed due to " + failedCause);
          } else {
            logger.debug("performTransport(" + capName + ") " + "succeeded");
          }
        }
      }
    }
  }
  
  /**
   * @param reservation
   * @return
   */
  private int performTransport(Reservations reservation) {
    try {
      return WarehouseRetryExecutor.buildCriticalExecutor().
          connectionDedicated(DedicatedConnection.XynaScheduler).
          storable(CapacityStorable.class).
          execute(new PerformTransport_WarehouseRetryExecutableNoException(reservation));
    } catch ( Exception e) { //PersistenceLayerException ...
      // ... oder RuntimeException bei "Reopening a cached connection failed" durch CentralComponentConnectionCache
      logger.warn("Failed to transport capacity " + reservation.getCapacityName(), e);
      return 0;
    }
  }


  @Override
  protected void refreshRemoteCapacityCache(String capName) {
    try {
      RMIClusterProviderTools.executeNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                 new RefreshLocalCapacityCacheRunnable(capName));
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    }
  }


  private void handleInvalidIDException(InvalidIDException e) {
    if (clusteredCapManagementInterfaceId == RMI_IS_CLOSED) {
      //closed rmi        
    } else {
      //nicht erwartet
      throw new RuntimeException(e);
    }
  }

  protected void increaseCaps(ODSConnection defCon, CapacityStorables allCs,
                              String capName, int addCard) throws PersistenceLayerException {
    //addCard Capacities werden hinzugefügt, dies ist immer möglich.
    //Wie sollen diese nun verteilt werden? 
    //a) Knoten mit geringer Cardinality werden bevorzugt -> gerecht
    //b) neue Caps werden proportional zu den existierenden vergeben -> entspricht dem Bedarf
    //c) gleichmäßig vergeben -> am einfachsten, deswegen verwendet

    int cardForeign = addCard / allCs.size(); // Cardinality der fremden Bindings
    int cardOwn = addCard - cardForeign * (allCs.size() - 1); // eigene Cardinality

    allCs.getOwn().setCardinality( allCs.getOwn().getCardinality() + cardOwn );
    
    for (CapacityStorable cs : allCs.getOthers() ) {
      cs.setCardinality( cs.getCardinality() + cardForeign );
    }
    
    defCon.persistCollection(allCs);
    defCon.commit();

    //Eintrag im Cache aktualisieren
    cache.refresh(allCs);
    refreshRemoteCapacityCache(capName);
  }


  /**
   * Verringern der Gesamt-Cardinality um removeCard
   */
  protected void decreaseCaps(ODSConnection defCon, CapacityStorables allCs,
                              String capName, int removeCard) throws PersistenceLayerException,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException {
    //removeCard Capacities sollen entfernt werden, dies ist unter Umständen nicht möglich
    //hier werden 3 Fälle unterschieden, alle erfordern einen anderen Algorithmus
    //a) Cardinality kann lokal verringert werden
    //b) Capacity ist ACTIVE
    //c) Capacity ist DISABLED
    CapacityInformation ci = getLocalCapacityInformation(capName);

    int free = ci.getCardinality() - ci.getInuse();
    if (free >= removeCard) {
      decreaseCapsLocal(defCon, allCs, capName, removeCard);
    } else {
      if( ci.getState() == State.ACTIVE ) {
        decreaseCapsActive(defCon,allCs,capName,removeCard);
      } else {
        decreaseCapsDisabled(defCon,allCs,capName,removeCard);
      }
    }    
  }

  private void decreaseCapsLocal(ODSConnection defCon, CapacityStorables allCs,
                                 String capName, int removeCard) throws PersistenceLayerException {
    //Verringern der lokalen Cardinality
    allCs.getOwn().setCardinality( allCs.getOwn().getCardinality() - removeCard );
    
    defCon.persistObject(allCs.getOwn());
    defCon.commit();

    //Eintrag im Cache aktualisieren
    cache.refresh(allCs);
  }


  private void decreaseCapsActive(ODSConnection defCon, CapacityStorables allCs,
                                  String capName, int removeCard) throws PersistenceLayerException,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState {
    //Capacities werden laufend neu vergeben, daher ist die genaue Anzahl der verwendeten 
    //Caps nie bekannt -> was nun?
    
    //Alle Knoten müssen alle ihre Capacities des Typs capName abgeben, dann wird dies lokal ausgeglichen
    //defCon.persistCollection(allCs); //SelectForUpdate-Lock zurückgeben
    if( logger.isDebugEnabled() ) {
      logger.debug("current capacities: "+allCs);
    }
    defCon.rollback(); //SelectForUpdate-Lock zurückgeben
    
    CapacityStorables allDecreasedCs = null;
    try {
      RMIClusterProviderTools.executeNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                      new MoveAllFreeCapacitiesToBinding(capName, ownBinding));
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    } finally {
      //In jedem Fall die Capacity-Verteilung aktualisieren, da sonst Capacities im Cache fehlen würden
      allDecreasedCs = new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(defCon, capName), ownBinding );
      cache.refresh(allDecreasedCs);
    }
    if( logger.isDebugEnabled() ) {
      logger.debug(" after moveAllFreeCapacitiesToBinding: "+ allDecreasedCs);
    }
    
    int free = cache.get(capName).getNumberOfAllFreeCaps();
    if( free >= removeCard ) {
      //ok, es sind genügend freigeworden
      CapacityStorable own = allDecreasedCs.getOwn();
      own.setCardinality( own.getCardinality() - removeCard );
      
      defCon.persistCollection(allDecreasedCs);
      defCon.commit();
      cache.refresh(allDecreasedCs);
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("Can not change cardinality to the desired value, " + removeCard
            + " capacities should be removed, but only " + free + " were free");
      }
      throw new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState();
    }
  }
  
  public void moveAllFreeCapacitiesToBinding(String capName, int binding) throws RemoteException {
    //alle freien Capacities müssen abgegeben werden
    cacheLock.lock();
    try {
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        CapacityStorables allCs = new CapacityStorables(capacityStorableQueries.loadAllByNameForUpdate(defCon, capName), ownBinding );
   
        CapacityStorable own = allCs.getOwn();
        CapacityStorable other = allCs.getBinding(binding);
        if( own == null ) {
          logger.warn("Own CapacityStorable not found ");
          return;
        }
        if( other == null ) {
          logger.warn("CapacityStorable for binding "+binding+" not found ");
          return;
        }
        
        int free = cache.get(capName).getNumberOfAllFreeCaps();
        
        own.setCardinality( own.getCardinality() - free ); 
        other.setCardinality( other.getCardinality() + free ); 
        
        defCon.persistCollection(allCs);
        defCon.commit();
        
        cache.refresh(allCs);
      } catch ( PersistenceLayerException e ) {
        logger.warn("Ignored exception ",e);
      } finally {
        finallyClose(defCon);
      }
        
    } finally {
      cacheLock.unlock();
    }
  }


  private void decreaseCapsDisabled(ODSConnection defCon, CapacityStorables allCs,
                                    String capName, int removeCard) throws PersistenceLayerException,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException {
    //Ermitteln der derzeitigen Capacity-Verwendung
    List<CapacityInformation> ciList;
    try {
      ciList = RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                            new GetLocalCapacityInformationRunnable(capName), this);
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
      throw (XPRC_ClusterStateChangedException) new XPRC_ClusterStateChangedException().initCause(e);
    }

    //int inuse = 0;
    //int currentCardinality = 0;
    int size = ciList.size();
    int[] free = new int[size];
    int[] bindings = new int[size];
    int totalFree = 0;
    for( int c=0; c<ciList.size(); ++c ) {
      CapacityInformation ci = ciList.get(c);
      //inuse += ci.getInuse();
      //currentCardinality += ci.getCardinality();
      free[c] = ci.getCardinality() - ci.getInuse();
      totalFree += free[c];
      bindings[c] = ci.binding;
    }
    
    if( removeCard > totalFree ) {
      logger.info("Can not change cardinality to the desired value, too many capacities in use");
      throw new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain();
    }

    //ok, es gibt genügend unbenutzte Capacities, diese nun entfernen
    //wieviele Caps sollen von jedem Ci-Eintrag entfernt werden?
    int[] remCard = new int[size];
    
    //Vergabeschwelle: bevorzugt da Capacities entfernen, wo viele frei sind
    int maxFree = 0;
    for( int r =0; r < size; ++r ) {
      if( free[r] > maxFree ) {
        maxFree = free[r];
      }
    } 
      
    int toRemove = removeCard;
    while( toRemove > 0 ) {
      for( int r =0; r < size; ++r ) {
        if( maxFree <= free[r] ) { //Vergabeschwelle berücksichtigen
          if( remCard[r] < free[r] ) { //sind noch Caps frei?
            ++remCard[r]; //Cap verringern
            --toRemove;
            if( toRemove == 0 ) {
              break; //genügend Caps gefunden
            }
          }
        }
      }
      --maxFree; //Vergabeschwelle verringern
    }
    
    //Cardinality in CapacityStorable verringern
    for (int r = 0; r < size; ++r) {
      CapacityStorable cs = allCs.getBinding(bindings[r]);
      cs.setCardinality(cs.getCardinality() - remCard[r]);
    }

    defCon.persistCollection(allCs);
    defCon.commit();

    //Eintrag im Cache aktualisieren
    cache.refresh(allCs);
    refreshRemoteCapacityCache(capName);
  }


  public CapacityInformation getCapacityInformation(String capacityName) throws XPRC_ClusterStateChangedException {
    managementLock.lock(); //throws LockFailedException ok, da Aufruf über CLI
    try {
      cacheLock.lock();
      try {
        // FIXME check connection?
        List<CapacityInformation> ciList;
        try {
          ciList =
              RMIClusterProviderTools
                  .executeAndCumulateNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                 new GetLocalCapacityInformationRunnable(capacityName), this);
        } catch (InvalidIDException e) {
          handleInvalidIDException(e);
          throw (XPRC_ClusterStateChangedException) new XPRC_ClusterStateChangedException().initCause(e);
        }

        int cardinality = 0;
        int inuse = 0;
        for (CapacityInformation ci : ciList ) {
          cardinality += ci.getCardinality();
          inuse += ci.getInuse();
        }
        return new CapacityInformation(capacityName, cardinality, inuse, ciList.get(0).getState() );
      } finally {
        cacheLock.unlock();
      }
    } finally {
      managementLock.unlock(); //throws AlreadyUnlockedException ok, da Aufruf über CLI
    }
  }


  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation()
      throws XPRC_ClusterStateChangedException {

    getManagementLockForExternalRequests();
    try {
      cacheLock.lock();
      try {
        // FIXME check connection? isOpen for example
        List<ExtendedCapacityUsageInformation> ecuiList;
        try {
          ecuiList =
              RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                                    getLocalExtendedCapacityUsageInfoRunnable, this);
        } catch (InvalidIDException e) {
          handleInvalidIDException(e);
          throw (XPRC_ClusterStateChangedException) new XPRC_ClusterStateChangedException().initCause(e);
        }

        return ExtendedCapacityUsageInformation.merge(ecuiList);
      } finally {
        cacheLock.unlock();
      }
    } finally {
      managementLock.unlock(); //throws AlreadyUnlockedException ok, da Aufruf über CLI
    }

  }


  private void getManagementLockForExternalRequests() {
    while (true) {
      try {
        managementLock.lock(); //throws LockFailedException ok, da Aufruf über CLI
        break;
      } catch (LockFailedException e) {
        logger.debug("Failed to obtain management lock (" + e.getMessage()
            + (e.getCause() != null ? (", " + e.getCause().getMessage()) : "") + ")");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          // not expected
          throw new RuntimeException(e1);
        }
      }
    }
  }


  public List<CapacityInformation> listCapacities() throws XPRC_ClusterStateChangedException {

    getManagementLockForExternalRequests();
    try {
      cacheLock.lock();
      try {
        Map<String, CapacityInformation> result = listLocalCapacities();

        List<Map<String, CapacityInformation>> remoteInformation;
        try {
          remoteInformation =
              RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                                    listLocalCapacitiesRunnable, null);
        } catch (InvalidIDException e) {
          handleInvalidIDException(e);
          throw (XPRC_ClusterStateChangedException) new XPRC_ClusterStateChangedException().initCause(e);
        }

        for (Map<String, CapacityInformation> m : remoteInformation) {
          if (m == null)
            continue; //Fehler bei Remote-Verbindung
          for (Entry<String, CapacityInformation> e : m.entrySet()) {
            CapacityInformation toBeUpdated = result.get(e.getKey());
            if (toBeUpdated == null) {
              result.put(e.getKey(), e.getValue());
            } else {
              toBeUpdated.setCardinality(toBeUpdated.getCardinality() + e.getValue().getCardinality());
              toBeUpdated.setInuse(toBeUpdated.getInuse() + e.getValue().getInuse());
              if (toBeUpdated.getState() != e.getValue().getState()) {
                logger.warn("Capacity state is not consistent in cluster");
              }
            }
          }
        }

        return new ArrayList<CapacityInformation>(result.values());

      } finally {
        cacheLock.unlock();
      }
    } finally {
      managementLock.unlock(); //throws AlreadyUnlockedException ok, da Aufruf über CLI
    }

  }

  protected boolean retryReadCap(int retry) {
    ClusterState clusterState = storableClusterContext.getClusterState();
    if( clusterState == ClusterState.CONNECTED ) {
      //Eintrag in DB sollte eigentlich lesbar sein, d.h. weiter Retries
      return true;
    } else {
      logger.warn("ClusterState is now "+clusterState+", giving up to retry after "+retry+" retries");
      return false;
    }
  }
  
  public int reserveCapForForeignBinding(int binding, Capacity capacity) {
    String capName = capacity.getCapName();
    String failedCause = "";
    int reserved = 0;
    //Zuerst schneller Test ob Cap noch frei ist
    cacheLock.lock();
    try {
      if (!cache.contains(capName)) {
        failedCause = "unknown capacity";
        return 0;
      }
      
      int capsToReserve = capacity.getCardinality();
      if (capsToReserve == 0) {
        failedCause = "capsToReserve == 0";
        return 0;
      }

      CapacityEntry ce = cache.get(capName);
      if( ce.isDisabled() ) {
        failedCause = "cap is disabled";
        return 0;
      }
      
      if( ! ce.checkAllocationPossible(capsToReserve) ) {
        int numFree = ce.getNumberOfFreeCapsForScheduling();
        if (numFree == 0) {
          failedCause = "numFree == 0 ";
          return 0;
        }
        capsToReserve = Math.min(numFree, capacity.getCardinality());
      }
      
      reserved = ce.reserveForBinding(binding, capsToReserve);
      return reserved;
    } finally {
      cacheLock.unlock();
      if (logger.isDebugEnabled()) {
        if (failedCause.length() > 0) {
          logger.debug("reserveCapForForeignBinding(" + capName + "," + binding + ") "
              + "failed due to " + failedCause);
        } else {
          logger.debug("reserveCapForForeignBinding(" + capName + "," + binding + ") "
              + "sucessfully reserved "+reserved+" caps");
        }
      }
    }
  }

  public int transportReservedCaps() {

    List<Reservations> reservations = null;
    cacheLock.lock();
    try {
      for (CapacityEntry ce : cache) {
        if (!ce.hasReservedForOtherBinding()) {
          continue;
        }
        if (reservations == null) {
          reservations = new ArrayList<Reservations>();
        }
        //es gibt Reservierungen, diese aus dem Cache austragen und sammeln
        reservations.add(ce.removeReservations());
      }
    } finally {
      cacheLock.unlock();
    }

    if (reservations != null && !reservations.isEmpty()) {
      return transportReservedCaps(reservations);
    } else {
      return 0;
    }
  }

  public void communicateLocalDemand(int binding, List<CapacityDemand> demand) {
    if (binding == ownBinding ) {
      return; //eigenen Demand ignorieren
    }
    if (logger.isDebugEnabled()) {
      logger.debug("communicateLocalDemand " + demand.size() + " demands for binding " + binding);
    }
    capacityReservation.setForeignDemand(binding,demand);
    scheduler.notifyScheduler();
  }
  
  public boolean communicateDemand(int binding, List<CapacityDemand> demand) {
    if (demand.size() > 0 ) {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("communicateDemand " + demand.size() + " demands for binding " + binding);
        }
        CommunicateDemandRunnable communicateDemandRunnable = new CommunicateDemandRunnable(binding,demand);
        RMIClusterProviderTools.executeNoException(clusterInstance, clusteredCapManagementInterfaceId,
                                                   communicateDemandRunnable);
        return true;
      } catch (InvalidIDException e) {
        handleInvalidIDException(e);
      }
    }
    return false;
  }

  public int getOwnBinding() {
    return ownBinding;
  }

  public CapacityEntryInformation getCapacityEntryInformation(String capName) {
    cache.getLock().lock();
    try {
      return cache.getCapacityEntryInformation(capName);
    } finally {
      cache.getLock().unlock();
    }
  }


  
  
  
  private static class CommunicateDemandRunnable implements
  RMIRunnableNoResultNoException<ClusteredCapacityManagementInterface> {
    private List<CapacityDemand> demand;
    private int binding;
    public CommunicateDemandRunnable(int binding, List<CapacityDemand> demand) {
      this.binding = binding;
      this.demand = demand;
    }
    public void execute(ClusteredCapacityManagementInterface clusteredInterface) throws RemoteException {
      clusteredInterface.communicateLocalDemand(binding, demand);
    }
  }


  
  private static class GetLocalCapacityInformationRunnable implements
        RMIRunnableNoException<CapacityInformation, ClusteredCapacityManagementInterface> {

    private String capacityName;

    public GetLocalCapacityInformationRunnable(String capacityName) {
      this.capacityName = capacityName;
    }

    public CapacityInformation execute(ClusteredCapacityManagementInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.getLocalCapacityInformation(capacityName);
    }
  }

  private static class RefreshLocalCapacityCacheRunnable implements
        RMIRunnableNoResultNoException<ClusteredCapacityManagementInterface> {
    private String capName;
    public RefreshLocalCapacityCacheRunnable(String capName) {
      this.capName = capName;
    }
    public void execute(ClusteredCapacityManagementInterface clusteredInterface) throws RemoteException {
      clusteredInterface.refreshLocalCapacityCache(capName);
    }
  }
  
  private static class MoveAllFreeCapacitiesToBinding implements
  RMIRunnableNoResultNoException<ClusteredCapacityManagementInterface> {
    private String capName;
    private int binding;
    public MoveAllFreeCapacitiesToBinding(String capName,int binding) {
      this.capName = capName;
      this.binding = binding;
    }
    public void execute(ClusteredCapacityManagementInterface clusteredInterface) throws RemoteException {
      clusteredInterface.moveAllFreeCapacitiesToBinding(capName,binding);
    }
  }



  private static RMIRunnableNoException<ExtendedCapacityUsageInformation, ClusteredCapacityManagementInterface> getLocalExtendedCapacityUsageInfoRunnable =
      new RMIRunnableNoException<ExtendedCapacityUsageInformation, ClusteredCapacityManagementInterface>() {

        public ExtendedCapacityUsageInformation execute(ClusteredCapacityManagementInterface clusteredInterface)
            throws RemoteException {
          return clusteredInterface.getLocalExtendedCapacityUsageInformation();
    }
  };
  
  private static RMIRunnableNoException<Map<String, CapacityInformation>, ClusteredCapacityManagementInterface> listLocalCapacitiesRunnable =
    new RMIRunnableNoException<Map<String, CapacityInformation>, ClusteredCapacityManagementInterface>() {

    public Map<String, CapacityInformation> execute(ClusteredCapacityManagementInterface clusteredInterface)
    throws RemoteException {
      return clusteredInterface.listLocalCapacities();
    }
  };

  public void refreshLocalCapacityCache( String capName ) {
    super.refreshLocalCapacityCache(capName);
    capacityReservation.refreshCapacity(capName);
    scheduler.notifyScheduler();
  }
  
}
