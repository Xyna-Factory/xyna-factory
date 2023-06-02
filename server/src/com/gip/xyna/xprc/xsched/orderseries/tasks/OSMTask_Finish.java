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

import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface.Result;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;


/**
 * OSMTask_Finish:
 * <br>
 * Im XynaCleanup gerufen, um nach dem Beenden des Auftrags den SeriesInformationStorable-Eintrag 
 * noch fertigzustellen und eventuell die Successoren zu starten.<br>
 * Vorbedingung ist, dass in der DB bereits der SeriesInformationStorable-Eintrag transaktionssicher 
 * in den Attributen finished und hadError sowie die XynaOrder angepasst wurde. Diese Vorbedingung 
 * wird hier nicht überprüft, der Update hier überschreibt sogar die bisherigen Änderungen in der DB.
 * Wichtig ist er aber zur Konsistenzwahrung im Fehlerfall, wenn dieser Task nicht mehr vollständig 
 * ausgeführt werden kann.
 * <br><br>
 * <pre>
 * Algorithmus:
 * 1) Lock der correlationId
 * 2) Suche des SeriesInformationStorable
 * 3) Anpassen des OrderStatus
 * 3.1) Falls der Auftrag gecancelt wurde, muss entschieden werden, ob die Successoren laufen dürfen.
 * 3.1.1) Es sind noch nicht alle Predecessoren fertig: nur OrderStatus auf CANCELING setzen und Task beenden
 * 3.1.2) Auftrag war regulär lauffähig: OrderStatus auf CANCELED setzen, weiter mit 4)
 * 4) Nachfolger muss gecancelt werden, wenn OrderStatus.isError=true und AutoCancel=true
 * 5) Schleife über alle SuccessorCorrIds
 * 5.1) Zugriff auf Predecessor-Baum sucTree zu SuccessorCorrId
 * 5.1.1) Falls sucTree nicht existiert, Neubau.
 * 5.2) Auswertung des Ergebnis des Aufrufs updateSuccessorInternal(sucTree), 3 Fälle
 * 5.2.1) NotFound: nichts zu tun
 * 5.2.2) Later:    Umtragen von SuccessorCorrIds nach SuccessorOrderIds
 * 5.2.3) Success:  Umtragen von SuccessorCorrIds nach SuccessorOrderIds
 * 6) Update des SeriesInformationStorable
 * 7) Unlock der correlationId
 * </pre>
 * <pre>
 * Algorithmus updateSuccessorInternal(sucTree):
 * 1) 3 Fälle
 * 1.1) sucTree hat keine Daten:     Successor existiert noch nicht. 
 *                                   Rückgabe Result.NotFound 
 * 1.2) sucTree hat eigenes Binding: Rückgabe Aufruf 
 *                                   OSMLocalImpl.updateSuccessor (siehe dort)
 * 1.3) sucTree hat fremdes Binding: Rückgabe Remote-Aufruf 
 *                                   OSMRemoteProxyImpl.updateSuccessor, ruft dort 
 *                                   OSMLocalImpl.updateSuccessor (siehe dort) auf
 * 2) Mögliche Rückgaben:
 * 2.1) NotFound: Successor existiert noch nicht
 * 2.2) Later:    Successor existiert zwar, der weitere Status ist jedoch unbekannt, 
 *                da das Lock nicht erhalten wurde. (Grund: Deadlock-Vermeidung, dies kann 
 *                lokal oder remote auftreten) Ein weiterer OSMTask_UpdateSuccessor
 *                startet evtl. den Successor
 * 2.3) Success:  Successor ist gefunden und evtl. gestartet
 * </pre>
 */
public class OSMTask_Finish extends OSMTask {
  
  private String correlationId;
  private OrderStatus orderStatus;
  private int countStartedSuccessors;
  
  
  OSMTask_Finish(String correlationId, OrderStatus orderStatus ) {
    if( correlationId == null ) {
      IllegalArgumentException iae = new IllegalArgumentException("correlationId must not be null");
      logger.warn( "correlationId is null", iae );
      throw iae;
    }
    this.correlationId = correlationId;
    this.orderStatus = orderStatus;
    this.countStartedSuccessors = 0;
  }
  
  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  protected void executeInternal() {
    
    osmCache.lock(correlationId);
    try {
      SeriesInformationStorable sis = osmCache.get(correlationId);
      if( sis == null ) {
        throw new IllegalStateException("osmCache has no entry for "+correlationId);
      }
      
      //Auftrag wurde gecancelt. Nun entscheiden, ob Successoren starten dürfen
      if( orderStatus == OrderStatus.CANCELING ) {
        if( sis.getPredecessorCorrIds().isEmpty() ) {
          //Auftrag war regulär lauffähig, d.h. der Auftrag ist fertig und Nachfolger dürfen starten
          orderStatus = OrderStatus.CANCELED;
        } else {
          //nur den Status CANCELING speichern. Successoren dürfen noch nicht benachrichtigt werden
          sis.setOrderStatus(orderStatus);
          osmCache.update(sis);
          return;
        }
      }
      
      sis.setOrderStatus(orderStatus);
      
      //Muss Nachfolger gecancelt werden?
      boolean cancel = false;
      if( sis.isAutoCancel() ) {
        cancel = sis.getOrderStatus().isError();
      }
    
      //Successor füllen
      Iterator<String> iter = sis.getSuccessorCorrIds().iterator();
      while( iter.hasNext() ) {
        String successorCorrId = iter.next();
        TreeNode sucTree = predecessorTrees.getTree( successorCorrId );
        if( sucTree == null ) {
          //hier muss kein großer Baum gebaut werden, da hier nur die Informationen des TreeNode 
          //selbst gebraucht werden. Falls doch irgendwann der vollständige Baum in 
          //OSMTask_Preschedule gebraucht wird, wird er dort ergänzt werden.
          sucTree = predecessorTrees.buildShortTree(successorCorrId); 
        } else {
          if( ! sucTree.hasData() ) {
            //Daten ergänzen, da Successor ja benachrichtigt werden muss 
            sucTree = predecessorTrees.buildShortTree(successorCorrId);
          }
        }
        Result result = updateSuccessorInternal( successorCorrId, sucTree, sis.getCorrelationId(), sis.getId(), cancel );
        switch( result ) {
          case NotFound:
            //Successor existiert noch nicht
            break;
          case Later:
            //Successor kann derzeit nicht bearbeitet werden, Operation wird später wiederholt
            sis.getSuccessorOrderIds().add( sucTree.getOrderId() );
            ++countStartedSuccessors;
            iter.remove();
            break;
          case Success:
            //Successor gefunden und angepasst
            sis.getSuccessorOrderIds().add( sucTree.getOrderId() );
            ++countStartedSuccessors;
            iter.remove();
            break;
          default:
            throw new IllegalStateException("unexpected state "+ result );
        }
      }
    
      osmCache.update(sis);
           
      //Wenn Auftrag keine weiteren Successoren hat, muss er nicht mehr im OSMCache und 
      //in predecessorTrees aufbewahrt werden
      if( sis.getSuccessorCorrIds().isEmpty() ) {
        osmCache.remove(sis.getCorrelationId());
        predecessorTrees.finish(correlationId);
      }
      
      
    } finally {
      osmCache.unlock(correlationId);
    }

  }
  
  
  /**
   * Kopie aus OSMTask_UpdatePredecessor
   * @param successorCorrId
   * @param predecessorCorrId
   * @param predecessorOrderId
   * @param cancel
   * @return
   */
  private Result updateSuccessorInternal(String successorCorrId, TreeNode sucTree,  String predecessorCorrId, long predecessorOrderId, boolean cancel) {
    if( sucTree.hasData() ) {
      //logger.trace( Thread.currentThread().getName() + " sucTree="+sucTree +" ownBinding="+ownBinding );
      int binding = sucTree.getBinding();
      if( binding == ownBinding ) {
        //Successor gehört zum eigenen Binding
        return localOsm.updateSuccessor(binding, successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
      } else {
        //Successor gehört zu einem anderen Binding
        return remoteOsm.updateSuccessor(binding, successorCorrId,predecessorCorrId,predecessorOrderId,cancel);
      }
    } else {
      //Successor wurde noch nicht eingestellt, daher keine Daten vorhanden
      return Result.NotFound;
    }
  }

  public int getCountStartedSuccessors() {
    return countStartedSuccessors;
  }




}
