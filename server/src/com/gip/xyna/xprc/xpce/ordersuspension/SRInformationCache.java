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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.AdministrativeParallelLock;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformation.SRState;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter;


/**
 *
 */
public class SRInformationCache<C,O> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(SRInformationCache.class);
  
  private volatile ConcurrentHashMap<Long,SRInformation> srInformations;
  private SuspendResumeAdapter<C, O> srAdapter;
  private AdministrativeParallelLock<Long> orderBackupLock; //Lock, mit dem Lesezugriffe auf OrderBackup gesperrt werden, 
  //solange dort keine verl�sslichen Daten lesbar sind

  /**
   * @param srAdapter
   */
  public SRInformationCache(SuspendResumeAdapter<C, O> srAdapter) {
    this.srAdapter = srAdapter;
    srInformations = new ConcurrentHashMap<Long, SRInformation>();
    orderBackupLock = new AdministrativeParallelLock<Long>();
  }
  
  @Override
  public String toString() {
    return srInformations.toString();
  }
  
  @SuppressWarnings("unchecked")
  private RootSRInformation<O> castToRoot(SRInformation srInformation) {
    return (RootSRInformation<O>)srInformation;
  }

  /**
   * Gibt die SRInformation zur angegebenen OrderId aus oder erzeugt eine neue SRInformation.
   * Das Erzeugen ist sicher gegen konkurrierende Aufrufe: beide Aufrufe geben das gleiche Objekt zur�ck.
   * Wenn SRInformation neu angelegt wird, ist der status SRState.Unknown, wenn eine Order �bergeben wird,
   * wird der Status auf den �bergebenen Status gesetzt (nur der Aufrufer wei�, warum er die Order hat).
   * @param orderId
   * @param order
   * @param state  
   * @return
   */
  public SRInformation getOrCreateLocked(Long orderId, O order, SRState state) {
    SRInformation srInformation = getLockedOrNull(orderId);
    if( srInformation == null ) {
      //nicht gefunden, daher neu anlegen
      if( order != null ) {
        Long rootOrderId = srAdapter.getRootOrderId(order);
        RootSRInformation<O> rootSRInformation = getOrCreateLockedRootNotInvalid(rootOrderId, srAdapter.getRootOrder(order), state);
        try {
          return getOrCreateLocked( orderId, rootSRInformation, state);
        } catch (NoSuchChildException e) {
          throw new RuntimeException(e); //sollte nie auftreten k�nnen
        } finally {
          rootSRInformation.unlock();
        }
      } else {
        //TODO dieser fall kann nicht vorkommen?
        return getOrCreateLocked(orderId,state);
      }
    }
    return srInformation;
  }

  public SRInformation getOrCreateLocked(Long orderId, SRState state) {
    SRInformation srInformation = getLockedOrNull(orderId);
    if( srInformation == null ) {
      SRInformation newSRInformation = new SRInformation(orderId,state);
      newSRInformation.lock();
      srInformation = srInformations.putIfAbsent(orderId, newSRInformation );
      if( srInformation == null ) {
        srInformation = newSRInformation; //neue SRInformation ist sicher eingetragen
      } else {
        //konkurrierende Ausf�hrung hat SRInformation zuerst eintragen k�nnen, daher 
        //eigenen "newSRInformation" verwerfen und fremden "srInformation" zur�ckgeben
        srInformation.lock();
        newSRInformation.unlock();
      }
    }
    return srInformation;
  }
  
  public RootSRInformation<O> getOrCreateLockedRootNotInvalid(Long rootOrderId, O rootOrder, SRState state) {
    RootSRInformation<O> rootSRInformation = null;
    SleepCounter sleepCounter = null;
    while( true ) {
      rootSRInformation = getLockedRootOrNullIfInvalid(rootOrderId);
      if( rootSRInformation != null ) {
        srAdapter.fillOrderData(rootSRInformation, rootOrder);
        return rootSRInformation;
      }
      
      //nicht gefunden oder ung�ltig, daher neu anlegen
      rootSRInformation = new RootSRInformation<O>(rootOrderId,state);
      rootSRInformation.lock();
      if (rootOrder != null) {
        srAdapter.fillOrderData( rootSRInformation, rootOrder );
      }
      
      //nun eintragen
      SRInformation existing = srInformations.putIfAbsent(rootOrderId, rootSRInformation );
      if( existing == null ) {
        return rootSRInformation; //neue SRInformation ist sicher eingetragen
      }
      
      //konkurrierende Ausf�hrung hat SRInformation zuerst eintragen k�nnen 
      //oder es besteht ein ung�ltiger Eintrag
      rootSRInformation.unlock();
      existing.lock();
      try {
        //existing ist nun nicht in Verwendung, da Lock hier gehalten wird
        //Evtl. kann existing-Eintrag ersetzt werden
        if( existing.getState() == SRState.Invalid ) {
        } else if( existing instanceof RootSRInformation ) {
          //RootSRInformation muss konkurrierend eingetragen worden sein, daher nochmal probieren
        } else {
          //merkw�rdiger Zustand
          logger.warn("Found unexpected root SR Information: " + existing.getState());
        }
      } finally {
        existing.unlock();
      }
      
      //warten, dass es nicht mehr invalid ist
      if (sleepCounter == null) {
        sleepCounter = new SleepCounter(5, 50, 10);
      }
      try {
        sleepCounter.sleep();
      } catch (InterruptedException e) { 
        //offenbar f�r l�ngere zeit invalid gewesen als erwartet.
        throw new RuntimeException(e);
      }
    }
  }
  

  private SRInformation getLockedOrNull(Long orderId) {
    SRInformation srInformation = srInformations.get(orderId);
    if( srInformation != null ) {
      srInformation.lock();
    }
    return srInformation;
  }

  private SRInformation getOrCreateLocked(Long orderId, RootSRInformation<O> rootSRInformation, SRState state) throws NoSuchChildException {
    O order = null;
    boolean isRootOrderId = rootSRInformation.getOrderId().equals(orderId);
    if (!isRootOrderId) {
      //vor dem lock ermitteln
      order = srAdapter.extractOrder(orderId, rootSRInformation);
    }
    SRInformation srInformation = getOrCreateLocked(orderId,state);
    if( srInformation.getRootId() == null ) {
      if (isRootOrderId) {
        //Order entspricht RootOrder, d.h keine weitere Initialisierung n�tig, da bereits in getOrCreateRootSRInformation geschehen
      } else {
        //eingebetteten Auftrag in der RootOrder suchen
        boolean unlock = true;
        try {
          srAdapter.fillOrderData(srInformation, order);
          srInformation.setState(state);
          unlock = false;
        } finally {
          if( unlock ) {
            srInformation.unlock();
          }
        }
      }
    }
    return srInformation;
  }
  
  

  /**
   * Gibt die SRInformation zur RootOrder der angegebenen OrderId aus oder erzeugt eine neue SRInformation.
   * Das Erzeugen ist sicher gegen konkurrierende Aufrufe: beide Aufrufe geben das gleiche Objekt zur�ck.
   * Die zum Ausf�llen der SRInformation ben�tigten Daten werden aus der DB ermittelt.
   * @param target
   * @return
   * @throws PersistenceLayerException 
   * @throws OrderBackupNotFoundException 
   * @throws ResumeLockedException 
   */
  public RootSRInformation<O> getOrCreateLockedRoot(ResumeTarget target) throws PersistenceLayerException, OrderBackupNotFoundException, ResumeLockedException {
    //rootId ermitteln
    Long rootId = target.getRootId();
    if( rootId == null ) {
      rootId = srAdapter.getRootId(target.getOrderId());
    }
    RootSRInformation<O> rootSRInformation = null;
    int retry = 0;
    while( true ) {
      try {
        rootSRInformation = getOrCreateLockedRoot(rootId);
        break;
      } catch( OrderBackupNotAccessibleException obnae) {
        logger.info("waitUntilRootOrderIsAccessible "+rootId+" with retry "+retry);
        retry = srAdapter.waitUntilRootOrderIsAccessible(retry, rootId);
      }
    }
    return rootSRInformation;
  }
  
  public RootSRInformation<O> getOrCreateLockedRoot(O rootOrder) {
    Long rootOrderId = srAdapter.getOrderId(rootOrder);
    RootSRInformation<O> rootSRInformation = getLockedRootOrNullIfInvalid(rootOrderId);
    if( rootSRInformation != null ) {
      srAdapter.fillOrderData(rootSRInformation, rootOrder);
      return rootSRInformation;
    }
    return getOrCreateLockedRootNotInvalid(rootOrderId, rootOrder, SRState.Suspended);
  }
  
  public RootSRInformation<O> getOrCreateLockedRoot(Long rootOrderId) throws PersistenceLayerException, OrderBackupNotFoundException, ResumeLockedException, OrderBackupNotAccessibleException {
    RootSRInformation<O> rootSRInformation = getLockedRootOrNullNotInvalid(rootOrderId);

    if (rootSRInformation == null) {
      rootSRInformation = getOrCreateLockedRootNotInvalid(rootOrderId, null, SRState.Suspended);
    }

    //Daten zum Root sind unvollst�ndig, da Root auch supendiert ist. Daher RootOrder lesen
    if (rootSRInformation.getOrder() == null) {
      boolean needsUnlock = true; //Unlock der SRInformation, falls Exception geworfen wird
      try {
        if( orderBackupLock.tryLock(rootOrderId, true) ) {
          try {
            if (rootSRInformation.getOrder() == null) {
              O rootOrder = srAdapter.readOrder(rootOrderId, null);
              srAdapter.fillOrderData(rootSRInformation, rootOrder);
            }
            needsUnlock = false; //keine Exception, alles erfolgreich
          } finally {
            orderBackupLock.unlock(rootOrderId);
          }
        } else {
          throw new ResumeLockedException();
        }
      } finally {
        if( needsUnlock ) {
          rootSRInformation.unlock();
        }
      }
    }
    return rootSRInformation;
  }


  public static class ResumeLockedException extends Exception {
    private static final long serialVersionUID = 1L;
    
    /**
     * gibt aus performancegr�nden immer this zur�ck, macht aber nichts
     * <p>
     * Diese Exception ist kein echter Fehler, daher wird kein Stacktrace ben�tigt
     * <p>
     * siehe zb hier: http://www.javaspecialists.eu/archive/Issue129.html
     */
    public Throwable fillInStackTrace() {
      return this;
    }

  }

  public SRInformation getOrCreateLockedNotInvalid(ResumeTarget target, RootSRInformation<O> rootSRInformation) throws NoSuchChildException {
    SleepCounter sleepCounter = null;
    while (true) {
      SRInformation srInformation = lockedOrNullIfInvalid( getOrCreateLocked(target.getOrderId(), rootSRInformation, SRState.Suspended), true );
      if( srInformation != null ) {
        return srInformation;
      }
      //invalid: ist nur kurze zeit der fall
      sleepCounter = waitIfInvalid(sleepCounter, target.getOrderId());
    }
  }

  public RootSRInformation<O> getOrCreateLockedRootNotInvalid(ResumeTarget target) throws PersistenceLayerException, OrderBackupNotFoundException, ResumeLockedException {
    SleepCounter sleepCounter = null;
    while (true) {
      RootSRInformation<O> rootSRInformation = lockedOrNullIfInvalid( getOrCreateLockedRoot(target), true );
      if( rootSRInformation != null ) {
        return rootSRInformation;
      }
      //invalid: ist nur kurze zeit der fall
      sleepCounter = waitIfInvalid(sleepCounter, target.getOrderId());
    }
  }
  
  public RootSRInformation<O> getLockedRootOrNullIfInvalid(Long rootOrderId) {
    SRInformation srInformation = lockedOrNullIfInvalid( srInformations.get(rootOrderId), false );
    if( srInformation == null ) {
      return null;
    } else if( srInformation instanceof RootSRInformation ) {
      return castToRoot(srInformation);
    } else {
      srInformation.unlock();
      return null;
    }
  }
  
  private RootSRInformation<O> getLockedRootOrNullNotInvalid(Long rootOrderId) {
    SleepCounter sleepCounter = null;
    while (true) {
      SRInformation srInformation = srInformations.get(rootOrderId);
      if( srInformation == null ) {
        return null;
      } else {
        if( srInformation instanceof RootSRInformation ) {
          srInformation = lockedOrNullIfInvalid( srInformation, false);
        } else {
          throw new RuntimeException("Found SRInformation is no RootSRInformation for id="+rootOrderId);
        }
      }
      if( srInformation != null ) {
        return castToRoot(srInformation);
      }
      //invalid: ist nur kurze zeit der fall
      sleepCounter = waitIfInvalid(sleepCounter, rootOrderId);
    }
  }

  /**
   * Lockt die srInformation. Falls diese Status Invalid hat, wird Lock entfernt und null zur�ckgegeben
   * @param srInformation
   * @return gelockte srInformation oder null 
   */
  private <S extends SRInformation> S lockedOrNullIfInvalid(S srInformation, boolean alreadyLocked) {
    if( srInformation == null ) {
      return null;
    }
    if( !alreadyLocked ) {
      srInformation.lock();
    }
    if( srInformation.getState() != SRState.Invalid ) {
      return srInformation; //g�ltigen Eintrag gelockt zur�ckgeben
    } else {
      //srInformation ist unbrauchbar, daher freigeben
      srInformation.unlock();
      return null; //nun nochmal probieren
    }
  }
  
  private SleepCounter waitIfInvalid(SleepCounter sleepCounter, Long orderId) {
    if (sleepCounter == null) {
      sleepCounter = new SleepCounter(5, 50, 10);
    }
    try {
      sleepCounter.sleep();
    } catch (InterruptedException e) { 
      //offenbar f�r l�ngere zeit invalid gewesen als erwartet. 
      throw new RuntimeException("interrupted during wait for non invalid SR Information, orderId=" + orderId);
    }
    return sleepCounter;
  }

  /**
   * Nur f�r Ausgabe �ber ListSuspendResumeInfo
   * @param id
   * @return
   */
  public String getSRInformationAsString(Long id) {
    SRInformation sri = srInformations.get(id);
    if( sri == null ) {
      return "unknown orderId";
    } else {
      return sri.asCompleteString();
    }
  }

  /**
   * @param orderId
   */
  public void remove(Long orderId) {
    srInformations.remove(orderId);
  }
  
  /**
   * Nur f�r Ausgabe �ber ListSuspendResumeInfo
   * @return
   */
  public Map<Long, String> getRunningOrders() {
    HashMap<Long, String> map = new HashMap<Long, String>();
    for( Map.Entry<Long, SRInformation> entry : srInformations.entrySet() ) {
      map.put( entry.getKey(), String.valueOf(entry.getValue().getState()) );
    }
    return map;
  }
  
  @SuppressWarnings("unchecked")
  public Collection<RootSRInformation<O>> getSuspendedRootOrderInformation() {
    Collection<RootSRInformation<O>> infos = new ArrayList<RootSRInformation<O>> ();
    for (SRInformation info : srInformations.values()) {
      if (info.getState() == SRState.Suspended &&
          info instanceof RootSRInformation &&
          ((RootSRInformation<O>)info).getOrder() != null) {
        infos.add((RootSRInformation<O>)info);
      }
    }
    return infos;
  }
  
  /**
   * Hinzuf�gen von rootOrderIds, die eine zeitlang nicht im OrderBackup gelesen werden d�rfen  
   * @param orderIds
   */
  public void addUnresumeableOrders(Collection<Long> orderIds) {
    orderBackupLock.administrativeLock(orderIds);
  }

  public void removeUnresumableOrders(Collection<Long> orderIds) {
    orderBackupLock.administrativeUnlock(orderIds);
  }
  
  public Set<Long> getUnresumeableOrders() {
    return orderBackupLock.getAdministrativeLocks();
  }


  @SuppressWarnings("unchecked")
  public Pair<RootSRInformation<O>,SRState> getRootLockedInWantedStateOrNullOtherwise(Long rootOrderId, EnumSet<SRState> wantedStates ) {
    SRInformation srInformation = srInformations.get(rootOrderId);
    RootSRInformation<O> rootSRInformation;
    if( srInformation instanceof RootSRInformation ) {
      rootSRInformation = (RootSRInformation<O>)srInformation;
    } else {
      if( srInformation == null ) {
        //keine srInformation gefunden. D.h. dieser RootOrder ist nicht mehr am laufen, entweder suspendiert oder bereits fertig.
        //Genauer kann das nun nur �ber eine Suche im OrderBackup ermittelt werden
        return Pair.of(null,SRState.Unknown);
      } else {
        //srInformation ist keine RootSRInformation. Das sollte f�r eine rootOrderId nicht auftreten d�rfen!
        return Pair.of(null,SRState.Unknown);
      }
    }
    boolean unlock = true;
    srInformation.lock();
    try {
      SRState state = rootSRInformation.getState();
      if( wantedStates.contains( state ) ) {
        unlock = false;
        return Pair.of(rootSRInformation, state);
      } else {
        return Pair.of(null, state);
      }
    } finally {
      if( unlock ) {
        rootSRInformation.unlock();
      }
    }
  }

  public List<O> getSuspendedOrdersInMemory() {
    List<O> list = new ArrayList<>();
    for (SRInformation sri : srInformations.values()) {
      if (sri instanceof RootSRInformation) {
        @SuppressWarnings("unchecked")
        RootSRInformation<O> rsri = (RootSRInformation<O>) sri;
        if (rsri.getState() == SRState.Suspended) {
          O order = rsri.getOrder();
          if (order != null) {
            list.add(order);
          }
        }
      }
    }
    return list;
  }

}
