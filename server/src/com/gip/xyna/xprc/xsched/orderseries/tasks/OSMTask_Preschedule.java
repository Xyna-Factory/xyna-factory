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

package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.Iterator;
import java.util.List;

import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;
import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface.Result;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;


/**
 * OSMTask_Preschedule:
 * <br>
 * Im Prescheduler gerufen, um den bereits persistierten SeriesInformationStorable-Eintrag zu 
 * prüfen, ob er valide ist und ob alle Abhängigkeiten erfüllt sind und der zugehörige Auftrag
 * gestartet werden kann.<br>
 * Vorbedingung ist, dass der SeriesInformationStorable-Eintrag bereits transaktionssicher 
 * zusammen mit der XynaOrder in der DB persistiert wurde. Diese Bedingung wird hier nicht 
 * überprüft. Wichtig ist sie aber zur Konsistenzwahrung im Fehlerfall, wenn dieser Task nicht
 * mehr vollständig ausgeführt werden kann.
 * <br><br>
 * <pre>
 * Algorithmus:
 * 1) Lock der correlationId
 * 2) Suche des SeriesInformationStorable
 * 3) Bau des Predecessor-Baums in predecessorTrees, dabei Prüfung auf zyklische Abhängigkeit 
 *    der Predecessoren. Hier werden die Predecessoren gesucht und gelesen
 * 4) Schleife über alle PredecessorCorrIds
 * 4.1) Zugriff auf Predecessor-Baum preTree zu PredecessorCorrId
 * 4.2) Auswertung des Ergebnis des Aufrufs updatePredecessorInternal(preTree), 5 Fälle
 * 4.2.1) NotFound: nichts zu tun
 * 4.2.2) Later:    nichts zu tun
 * 4.2.3) Running:  nichts zu tun
 * 4.2.4) Cancel:   Umtragen von PredecessorCorrIds nach PredecessorOrderIds
 * 4.2.5) Success:  Umtragen von PredecessorCorrIds nach PredecessorOrderIds
 * 5) Update des SeriesInformationStorable
 * 6) Entscheidung, ob Auftrag gestartet werden kann oder abgebrochen werden muss
 * 7) Unlock der correlationId
 * 8) Evtl. Starten des Auftrags über OSMInterface.readyToRun( long orderId, boolean cancel )
 * </pre>
 * <pre>
 * Algorithmus updatePredecessorInternal(preTree):
 * 1) 3 Fälle
 * 1.1) preTree hat keine Daten:     Predecessor existiert noch nicht. 
 *                                   Rückgabe Result.NotFound 
 * 1.2) preTree hat eigenes Binding: Rückgabe Aufruf 
 *                                   OSMLocalImpl.updatePredecessor (siehe dort)
 * 1.3) preTree hat fremdes Binding: Rückgabe Remote-Aufruf 
 *                                   OSMRemoteProxyImpl.updatePredecessor, ruft dort 
 *                                   OSMLocalImpl.updatePredecessor (siehe dort) auf
 * 2) Mögliche Rückgaben:
 * 2.1) NotFound: Predecessor existiert noch nicht
 * 2.2) Later:    Predecessor wurde zwar gefunden, der weitere Status ist jedoch unbekannt, 
 *                da das Lock nicht erhalten wurde. (Grund: Deadlock-Vermeidung, dies kann 
 *                lokal oder remote auftreten) Ein weiterer OSMTask_UpdatePredecessor
 *                klärt den tatsächlichen Zustand ab.
 * 2.3) Running:  Predecessor ist noch nicht gelaufen
 * 2.4) Cancel:   Predecessor ist durch Fehler oder Cancel beendet worden und AutoCancel=true
 * 2.5) Success:  Predecessor ist erfolgreich beendet worden oder ist durch Fehler oder Cancel
 *                beendet worden und AutoCancel=false
 * </pre>
 */
public class OSMTask_Preschedule extends OSMTask {

  private String correlationId;
  private boolean orderStarted;
  
  /**
   * @param correlationId
   */
  OSMTask_Preschedule(String correlationId) {
    this.correlationId = correlationId;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  protected void executeInternal() {
    OrderState orderState = null;
    SeriesInformationStorable sis;
    List<String> cycle = null;
    boolean canBeStarted = false;
    
    osmCache.lock( correlationId );
    try {
      sis = osmCache.get(correlationId);
      if( sis == null ) {
        throw new IllegalStateException("SeriesInformationStorable not found");
      }
      long start = System.currentTimeMillis();
      boolean hasCycle = predecessorTrees.buildTree( sis.getCorrelationId() );    
      TreeNode tree = predecessorTrees.getTree( sis.getCorrelationId() );
      long end = System.currentTimeMillis();
      if (logger.isDebugEnabled()) {
        logger.debug("Took " + (end - start) + " ms to build Tree of depth " + tree.getDepth(1000) + " and size "
            + tree.getSize(1000) + " for correlationId " + sis.getCorrelationId() + " hasCycle=" + hasCycle);
      }
      
      //Predecessor füllen
      Iterator<String> iter = sis.getPredecessorCorrIds().iterator();
      while( iter.hasNext() ) {
        String predecessorCorrId = iter.next();
        TreeNode preTree = tree.getBranch(predecessorCorrId);
 
        boolean predecessorFinished = false;
        Result result = updatePredecessorInternal( predecessorCorrId, preTree, sis.getCorrelationId(), sis.getId() );
        switch( result ) {
          case Cancel:
            //Predecessor wurde mit Fehler beendet, der aktuelle Auftrag muss abgebrochen werden.
            sis.setInheritedCancel(true);
            predecessorFinished = true;          
            break;
          case Success:
            //Predecessor ist erfolgreich gelaufen oder mit einem Fehler beendet, der aber 
            //nicht dazu führt, dass der aktuelle Auftrag abgebrochen werden muss.
            predecessorFinished = true;
            break;
          case NotFound:
            //Predecessor existiert noch nicht
            break;
          case Later:
            //Predecessor kann derzeit nicht bearbeitet werden, Operation wird später wiederholt
            break;
          case Running:
            //Predecessor wurde zwar gefunden( result.oderId != null ), darf aber noch nicht
            //eingetragen werden, damit aktueller Auftrag noch wartet. Eingetragen wird er 
            //später in finish(OSMTask task)
            break;
        }
        if( predecessorFinished ) {
          sis.getPredecessorOrderIds().add( preTree.getOrderId() );
          iter.remove();
          tree.removeBranch(predecessorCorrId);
        }
                
      }
      
      if( sis.isInheritedCancel() && sis.isIgnoreInheritedCancel() ) {
        //inheritedCancel wird durch ignoreInheritedCancel überschrieben
        sis.setInheritedCancel(false);
      }
      
      if( sis.getOrderStatus() == OrderStatus.WAITING ) {
        if( sis.getPredecessorCorrIds().size() == 0 ) {
          canBeStarted = true;
          orderState = OrderState.CanBeStarted;
        } else if( sis.isInheritedCancel() ) {
          canBeStarted = true;
          orderState = OrderState.HasToBeCanceled;
        } else if( hasCycle ) {
          cycle = predecessorTrees.getCycle(correlationId);
          if( logger.isInfoEnabled() ) {
            logger.info( "Found circle "+ cycle);
          }
          if( cycle.contains(correlationId) ) {               
            canBeStarted = true;
            orderState = OrderState.HasCyclicDependencies;
          } else {
            //der aktuelle Auftrag hängt zwar an einem Cycle, ist aber selbst nicht schuld daran.
            //Dies sollte nicht auftreten können. Wenn doch, muss manuell aufgeräumt werden
            logger.warn( "Found existing circle "+ cycle+" which must be manually cleared");
            canBeStarted = false;
            orderState = OrderState.HasCyclicDependencies;            
          }
        }
      }
      
      if( canBeStarted ) {
        sis.setOrderStatus( OrderStatus.RUNNING );
      }
      osmCache.update(sis);
      
    } finally {
      osmCache.unlock( correlationId );
    }
    if( canBeStarted ) {
      osm.readyToRun( sis.getCorrelationId(), sis.getId(), orderState, cycle );
      orderStarted = true;
    }
  }

  /**
   * Update des Predecessor in Cache und DB: Eintragen des Successors
   * Rückgabe, ob Predecessor bekannt ist, erfolgreich war oder ob er
   * über AutoCancel den Successor abbricht.
   * @param predecessorCorrId
   * @param successorCorrId
   * @param successorOrderId
   * @return
   */
  private Result updatePredecessorInternal(String predecessorCorrId, TreeNode preTree, String successorCorrId, long successorOrderId) {
    if( preTree.hasData() ) {
      int binding = preTree.getBinding();
      if( binding == ownBinding ) {
        //Predeccessor gehört zum eigenen Binding
        return localOsm.updatePredecessor(binding, predecessorCorrId,successorCorrId,successorOrderId);
      } else {
        //Predeccessor gehört zu einem anderen Binding
        return remoteOsm.updatePredecessor(binding, predecessorCorrId,successorCorrId,successorOrderId);
      }
    } else {
      //Predecessor wurde noch nicht eingestellt, daher keine Daten vorhanden
      return Result.NotFound;
    }
  }

  public boolean isOrderStarted() {
    return orderStarted;
  }
  
}
