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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.Queue;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;
import com.gip.xyna.xprc.xsched.orderseries.tasks.OSMTask;


/**
 * OSMLocalImpl:
 * Implementierung des Interfaces OSMRemoteInterface, die die notwendigen Änderungen an den 
 * SeriesInformationStorable lokal durchführt.  
 *
 * Algorithmus für update{Successor/Predecessor}
 * <pre>
 * 1) tryLock der corrId
 * 1.1) Lock erhalten
 * 1.1.1) Rückgabe des Ergebnis von update{Successor/Predecessor}WithoutLock
 * 1.1.2) Unlock
 * 1.2) Lock nicht erhalten
 * 1.2.1) Einstellen des Tasks OSMTask_Update{Successor/Predecessor} in die Task-Queue
 * 1.2.2) Rückgabe von Result.Later
 * </pre>
 * Algorithmus für updateSuccessorWithoutLock
 * <pre>
 * 1) Suche des SeriesInformationStorable <code>sisSuc</code>
 * 2) Abbruch mit <code>Result.NotFound</code>, wenn kein SeriesInformationStorable gefunden wird
 * 3) Enthält <code>sisSuc</code> <code>predecessorCorrId</code>?
 * 3.1) Ja: <code>predecessorOrderId</code> eintragen, alle Vorkommen von <code>predecessorCorrId</code> entfernen
 * 3.2) Nein: Enthält <code>sisSuc</code> <code>predecessorOrderId</code> bereits?
 * 3.2.1) Ja: Ok
 * 3.2.2) Nein: Predecessor und Successor passen nicht zusammen, Warnung ins Log, <code>predecessorOrderId</code> eintragen
 * 4) aus <code>cancel</code> ergibt sich <code>inheritedCancel</code>, dabei <code>ignoreInheritedCancel</code> beachten
 * 5) Falls OrderStatus = CANCELING und keine Predecessoren mehr vorhanden sind, mit einem OSMTask_Finish
 *    den OrderStatus auf CANCELED setzen und Successoren benachrichtigen
 * 6) Update <code>sisSuc</code>, falls sich Änderungen ergeben haben
 * 7) In PredecessorTrees zu <code>sucTree</code> den Branch <code>predecessorCorrId</code> entfernen
 * 8) Wenn <code>sisSuc</code> bereits <code>finished==true</code> hat und alle <code>{prede;suc}cessorCorrIds</code> 
 *    bereits gefunden wurden: <code>sisSuc</code> aus Cache entfernen
 * 9) OrderState (AlreadyFinished,HasToBeCanceled,CanBeStarted,WaitingForPredecessor) ermitteln 
 * 10) Wenn OrderState != WaitingForPredecessor: Aufruf {@link com.gip.xyna.xprc.xsched.orderseries.OSMInterface#readyToRun(String, long, OrderState, java.util.List)}
 * </pre>
 * Algorithmus für updateSuccessorWithoutLock
 * <pre>
 * 1) Suche des SeriesInformationStorable <code>sisPre</code>
 * 2) Abbruch mit <code>Result.NotFound</code>, wenn kein SeriesInformationStorable gefunden wird
 * 3) Abbruch mit <code>Result.Running</code>, wenn in <code>sisPre</code> <code>finished==false</code>
 * 4) Enthält <code>sisPre</code> <code>successorCorrId</code>?
 * 4.1) Ja: <code>successorOrderId</code> eintragen, alle weiteren Vorkommen von <code>successorCorrId</code> entfernen
 * 4.2) Nein: Enthält <code>sisPre</code> <code>successorOrderId</code> bereits?
 * 4.2.1) Ja: Ok
 * 4.2.2) Nein: Predecessor und Successor passen nicht zusammen, Warnung ins Log, <code>successorOrderId</code> eintragen
 * 5) Update <code>sisPre</code>, falls sich Änderungen ergeben haben
 * 6) Enthält <code>sisPre</code> <code>successorCorrId</code>s?
 * 6.1) Ja: Da kein Successor mehr den Predecessor benötigt, kann er aus PredecessorTrees und OsmCache entfernt werden
 * 7) Wenn <code>sisPre</code> <code>hadError==true</code> und <code>autoCancel==true</code> hat:
 * 7.1) Ja: Rückgabe <code>Result.Cancel</code>
 * 7.2) Nein: Rückgabe <code>Result.Success</code>
 * </pre>
 */

public class OSMLocalImpl implements OSMRemoteInterface {

  private static Logger logger = CentralFactoryLogging.getLogger(OSMLocalImpl.class);
  
  private OSMCache osmCache;
  private Queue<OSMTask> queue;
  private OSMInterface osm;
  private PredecessorTrees predecessorTrees;
  
  public OSMLocalImpl( OSMCache osmCache, Queue<OSMTask> queue, OSMInterface osm, PredecessorTrees predecessorTrees ) {
    this.osmCache = osmCache;
    this.queue = queue;
    this.osm = osm;
    this.predecessorTrees = predecessorTrees;
  }
  
  /**
   * Warten mit tryLock, falls Lock nicht erhalten wird: Auftrag in Queue einstellen und später probieren
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updateSuccessor(int, java.lang.String, java.lang.String, long, boolean)
   */
  public Result updateSuccessor(int binding, String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                              boolean cancel) {
  
    //Mit tryLock probieren, da Locks verschachtelt geholt werden
    if( osmCache.tryLock(successorCorrId) ) {
      try {
        return updateSuccessorWithoutLock(successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
        
      } finally {
        osmCache.unlock(successorCorrId);
      }
    } else {
      //Lock nicht erhalten. Da keine zeitnahe Abarbeitung wirklich erforderlich ist,
      //wird dieser Update in einen OSMTask verpackt, der dann später nochmal vom 
      //OSMTaskConsumer verarbeitet wird. 
      queue.add( OSMTask.updateSuccessor(binding,successorCorrId,predecessorCorrId,predecessorOrderId,cancel) );
      return Result.Later;
    }

  }
  
  /**
   * Warten mit tryLock, falls Lock nicht erhalten wird: Auftrag in Queue einstellen und später probieren
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updatePredecessor(int, java.lang.String, java.lang.String, long)
   */
  public Result updatePredecessor(int binding, String predecessorCorrId, String successorCorrId, long successorOrderId) {
    //Mit tryLock probieren, da Locks verschachtelt geholt werden
    if( osmCache.tryLock(predecessorCorrId) ) {
      try {
        return updatePredecessorWithoutLock(predecessorCorrId,successorCorrId,successorOrderId);
      } finally {
        osmCache.unlock(predecessorCorrId);
      }
    } else {
      //Lock nicht erhalten. Da keine zeitnahe Abarbeitung wirklich erforderlich ist,
      //wird dieser Update in einen OSMTask verpackt, der dann später nochmal vom 
      //OSMTaskConsumer verarbeitet wird. 
      queue.add( OSMTask.updatePredecessor(binding,predecessorCorrId,successorCorrId,successorOrderId) );
      return Result.Later;      
    }
  }

  
  
  /**
   * Update des Successors; normaler Lock =&gt; warten auf Bearbeitung
   * @param binding
   * @param successorCorrId
   * @param predecessorCorrId
   * @param predecessorOrderId
   * @param cancel
   * @return
   */
  public Result updateSuccessorWithNormalLock(int binding, String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                boolean cancel) {
    osmCache.lock(successorCorrId);
    try {
      return updateSuccessorWithoutLock(successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
    } finally {
      osmCache.unlock(successorCorrId);
    }
  }

  /**
   * Update des Predecessors; normaler Lock =&gt; warten auf Bearbeitung 
   * @param binding
   * @param predecessorCorrId
   * @param successorCorrId
   * @param successorOrderId
   * @return
   */
  public Result updatePredecessorWithNormalLock(int binding, String predecessorCorrId, String successorCorrId, long successorOrderId) {
    osmCache.lock(predecessorCorrId);
    try {
      return updatePredecessorWithoutLock(predecessorCorrId,successorCorrId,successorOrderId);
    } finally {
      osmCache.unlock(predecessorCorrId);
    }
  }
  
  /**
   * Eigentlicher Update des Successor ohne Berücksichtigung der Locks
   * (Auftragsende)
   * @param successorCorrId
   * @param predecessorCorrId
   * @param predecessorOrderId
   * @param cancel
   * @return
   */
  private Result updateSuccessorWithoutLock(String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                            boolean cancel) {
    if( logger.isTraceEnabled() ) {
      logger.trace( "updateSuccessorWithoutLock("+successorCorrId+","+predecessorCorrId+","+predecessorOrderId+","+cancel+")" );
    }
    
    SeriesInformationStorable sisSuc = osmCache.get(successorCorrId);
    if( sisSuc == null ) {
      return Result.NotFound;
    }
    boolean changed = false;
    if( sisSuc.getPredecessorCorrIds().remove(predecessorCorrId) ) {
      sisSuc.getPredecessorOrderIds().add( predecessorOrderId );
      changed = true;
      while ( sisSuc.getPredecessorCorrIds().remove(predecessorCorrId) ) {} 
    } else {
      if( sisSuc.getPredecessorOrderIds().contains(predecessorOrderId) ) {
        if( logger.isTraceEnabled() ) {
          logger.trace( "updateSuccessorWithoutLock: predecessor is already transfered");
        }
      } else {
        //das sollte nicht passieren. Laut Predecessor sollte dies der Successor sein -> Warnung ins Log
        logger.warn( "updateSuccessor: predecessor "+predecessorCorrId+" not found for successor "+successorCorrId);
        //predecessorOrderId trotzdem nachträglich in Successor eintragen
        sisSuc.getPredecessorOrderIds().add( predecessorOrderId );
      }
    }
    if( cancel ) {
      if( ! sisSuc.isIgnoreInheritedCancel() ) {
        if( ! sisSuc.isInheritedCancel() ) {
          sisSuc.setInheritedCancel(true);
          changed = true;
        }
      }
    }
    if( sisSuc.getOrderStatus() == OrderStatus.CANCELING ) {
      if( sisSuc.getPredecessorCorrIds().isEmpty() ) {
        //keine weiteren Vorgänger vorhanden, daher nun mit einem OSMTask_Finish den
        //Status auf Canceled umsetzen und die Successoren benachrichtigen
        OSMTask finish = OSMTask.finish(successorCorrId, OrderStatus.CANCELED);
        queue.add(finish);
      }
    }
    
    boolean canBeStarted = false;
    if( sisSuc.getOrderStatus() == OrderStatus.WAITING ) {
      if( sisSuc.getPredecessorCorrIds().size() == 0 || sisSuc.isInheritedCancel() ) {
        canBeStarted = true;
        sisSuc.setOrderStatus( OrderStatus.RUNNING );
        //changed = true ist bereits gesetzt
      }
    }
    
    if( changed ) {
      osmCache.update( sisSuc );
    }
    
    //Predecessor ist fertig, daher muss er nicht mehr im Tree aufbewahrt werden
    TreeNode sucTree = predecessorTrees.getTree(successorCorrId);
    if( sucTree != null ) {
      sucTree.removeBranch(predecessorCorrId);
      if( logger.isTraceEnabled() ) {
        logger.trace( "removed "+predecessorCorrId+" from " + sucTree);
      }
    }
    if( sisSuc.getOrderStatus().isFinished() 
                    && sisSuc.getPredecessorCorrIds().isEmpty() 
                    && sisSuc.getSuccessorCorrIds().isEmpty() ) {
      //sisSuc sollte von niemandem mehr benötigt werden, daher aus dem Cache entfernen
      osmCache.remove(sisSuc.getCorrelationId());
    }
    
    if( canBeStarted ) {
      OrderState orderState = null;
      if( sisSuc.isInheritedCancel() ) {
        orderState = OrderState.HasToBeCanceled;
      } else {
        orderState = OrderState.CanBeStarted;
      }
      osm.readyToRun( sisSuc.getCorrelationId(), sisSuc.getId(), orderState, null );
    }
   
    return Result.Success;
  }

  /**
   * Eigentlicher Update des Predecessor ohne Berücksichtigung der Locks
   * (Auftragseingang)
   * @param predecessorCorrId
   * @param successorCorrId
   * @param successorOrderId
   */
  private Result updatePredecessorWithoutLock(String predecessorCorrId, String successorCorrId, long successorOrderId) {
    if( logger.isTraceEnabled() ) {
      logger.trace( "updatePredecessorWithoutLock("+predecessorCorrId+","+successorCorrId+","+successorOrderId+")" );
    }
    SeriesInformationStorable sisPre = osmCache.get(predecessorCorrId);
    if( sisPre == null ) {
      return Result.NotFound;
    }
    if( ! sisPre.getOrderStatus().isFinished() ) {
      return Result.Running;
    }
    boolean changed = false;

    if( sisPre.getSuccessorCorrIds().remove(successorCorrId) ) {
      sisPre.getSuccessorOrderIds().add( successorOrderId );
      changed = true;
    } else {
      if( sisPre.getSuccessorOrderIds().contains(successorOrderId) ) {
        if( logger.isTraceEnabled() ) {
          logger.trace("updatePredecessorWithoutLock: successor is already transfered");
        }
      } else {
        //das sollte nicht passieren. Laut Successor sollte dies der Predecessor sein -> Warnung ins Log
        logger.warn( "updatePredecessor: successor "+successorCorrId+" not found for predecessor "+predecessorCorrId);
        //successorOrderId trotzdem nachträglich in Predecessor eintragen
        sisPre.getSuccessorOrderIds().add( successorOrderId );
      }
    }
    if( changed ) {
      osmCache.update( sisPre );
    }
    if( sisPre.getSuccessorCorrIds().isEmpty() ) {
      //Auf diesen Predecessor warten keine weiteren Successoren mehr
      //Daher kann der Eintrag aus den predecessorTrees und osmCache entfernt werden
      predecessorTrees.removeTree(predecessorCorrId);
      osmCache.remove(predecessorCorrId);
    }
    if( sisPre.getOrderStatus().isError() && sisPre.isAutoCancel() ) {
      return Result.Cancel;
    } else {
      return Result.Success;
    }
  }
  
}
