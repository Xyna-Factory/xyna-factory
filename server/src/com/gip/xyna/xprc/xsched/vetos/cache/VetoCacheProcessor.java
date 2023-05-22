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
package com.gip.xyna.xprc.xsched.vetos.cache;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;

public abstract class VetoCacheProcessor implements Algorithm {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VetoCacheProcessor.class);

  protected VetoCache vetoCache;
  private static final int MAX_BATCH_SIZE = 100;
  private SchedulerNotification schedulerNotification = new XynaSchedulerNotification();
  
  public VetoCacheProcessor(VetoCache vetoCache) {
    this.vetoCache = vetoCache;
  }
  
  public void setSchedulerNotification(SchedulerNotification schedulerNotification) {
    this.schedulerNotification = schedulerNotification;
  }
  
  public void notifyScheduler() {
    schedulerNotification.notifyScheduler();
  }
  
  public void exec() {
    int processed = processVetos();
    if( processed > 0 ) {
      notifyScheduler();
      //evtl zuviel, wenn Vetos nicht in Zustand "Local" gelangt sind. Scheduler wird aber eh h�ufig gerufen...
      if( logger.isDebugEnabled() ) {
        logger.debug( "Processed "+processed+" vetos");
      }
    }
    if( hasVetosToProcess() ) {
      vetoCache.notifyProcessor();
    }
  }
  
  

  protected boolean hasVetosToProcess() {
    return vetoCache.hasVetosToProcess();
  }

  private int processVetos() {
    int processed = 0;
    startBatch();
    try {
      for( int v=0; v<MAX_BATCH_SIZE;++v ) {
        String vetoName = vetoCache.getVetoToProcess();
        if( vetoName == null ) {
          break;
        }
        VetoCacheEntry veto = vetoCache.get(vetoName);
        if( veto != null ) {
          processVeto(veto);
          ++processed;
        }
      }
    } finally {
      endBatch();
    }
    return processed;
  }

  protected abstract void startBatch();
  
  protected abstract void endBatch();

  /**
   * @param veto
   * @return True, wenn Scheduler wieder laufen kann, wird allerdings nicht verwendet. 
   * R�ckgabe dient eher zur Kontrolle, ob alle Pfade implementiert wurden
   */
  private boolean processVeto(VetoCacheEntry veto) {
    State state = veto.getState();
    switch( state ) {
    case Free:
      return processFree(veto);
    case Local:
      return true; //Status nur f�r Scheduler �nderbar
    case Compare:
      return processCompare(veto);
    case Comparing:
      return processComparing(veto);
    case None:
      //sollte nicht existieren, remove nachholen
      vetoCache.remove(veto, State.None);
      return false;
    case Remote:
      return processRemote(veto);
    case Scheduled:
      return processScheduled(veto);
    case Scheduling:
      return false; //Status nur f�r Scheduler �nderbar
    case Usable:
      return true; //Status nur f�r Scheduler �nderbar
    case Used:
      //aktuell verwendetes Veto, hier nichts zu tun
      return false;
     default:
       logger.warn("Unexpected state "+ state );
       return false;
    }
  }
  
  
  
  protected abstract boolean processFree(VetoCacheEntry veto);
  protected abstract boolean processCompare(VetoCacheEntry veto);
  protected abstract boolean processComparing(VetoCacheEntry veto);
  protected abstract boolean processRemote(VetoCacheEntry veto);
  protected abstract boolean processScheduled(VetoCacheEntry veto);
  
  protected boolean processAgain(VetoCacheEntry veto, String cause) {
    logger.warn("needs to process again "+ veto+": "+cause);
    vetoCache.process(veto);
    return false;
  }
  
  public abstract VetoCacheEntry createNewVeto(String vetoName, long urgency);

  public interface SchedulerNotification {

    void notifyScheduler();
    
  }
  
  private static class XynaSchedulerNotification implements SchedulerNotification {
    
    public void notifyScheduler() {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
    }
    
  }

  public abstract String showBatch();

  /**
   * Ausgabe in CLI listExtendedSchedulerInfo
   * @return
   */
  public abstract String showInformation();

  public boolean canAllocate() {
    return true;
  }

}
