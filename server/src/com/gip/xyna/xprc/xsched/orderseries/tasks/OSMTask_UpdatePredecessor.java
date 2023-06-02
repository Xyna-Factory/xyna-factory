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

import com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface.Result;
import com.gip.xyna.xprc.xsched.orderseries.PredecessorTrees.TreeNode;


/**
 * OSMTask_UpdatePredecessor:
 * <br>
 * Wird von OSMLocalImpl gerufen, wenn für den Aufruf updatePredecessor das nötige Lock nicht erhalten wurde.
 * <br>
 * Hier in dem separaten Task kann dann solange gewartet werden, bis das Lock erhalten wird. Dann muss
 * erstens der updatePredecessor nachgeholt werden, zweitens aber auch das, was der OSMTask_Preschedule 
 * nicht mehr tun konnte, weil der OSMLocalImpl.updatePredecessor nicht bearbeitet wurde.
 * <br>
 * Im Fall dass der Predecessor bereits fertig oder gecancelt ist, ist das Verhalten dann analog zu 
 * OSMTask_Finish.
 * <br><br>
 * <pre>
 * Algorithmus:
 * 1) Ist Binding==OwnBinding?
 * 1.1) Ja: Aufruf OSMLocalImpl.updatePredecessorWithNormalLock (siehe dort)
 * 1.2) Nein: Aufruf OSMRemoteProxyImpl.updatePredecessor (siehe dort)
 * 2) Auswertung Ergebnis updatePredecessor{,WithNormalLock}
 * 2.1) NotFound: nicht erlaubt -&gt; IllegalStateException
 * 2.2) Later:   Successor wird später benachrichtigt werden, nichts mehr weiter zu tun
 * 3.3) Running:  Predecessor ist noch nicht gelaufen, nichts mehr weiter zu tun
 * 3.4) Cancel, Success: Predecessor ist beendet. Darüber muss nun der Successor informiert werden
 * 3.4.1) Suche der TreeNodes zu Predecessor und Successor
 * 3.4.2) Auswertung des Ergebnis des Aufrufs updateSuccessorInternal(sucTree), 3 Fälle
 * 3.4.2.1) NotFound  nicht erlaubt -&gt; IllegalStateException
 * 3.4.2.2) Later:    nichts zu tun
 * 3.4.2.3) Success:  nichts zu tun, Successor ist bereits im Predecessor eingetragen
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
public class OSMTask_UpdatePredecessor extends OSMTask {

  private int binding;
  private String predecessorCorrId;
  private String successorCorrId;
  private long successorOrderId;
  
  OSMTask_UpdatePredecessor(int binding,
                            String predecessorCorrId, String successorCorrId, long successorOrderId) {
    this.binding = binding;
    this.predecessorCorrId = predecessorCorrId;
    this.successorCorrId = successorCorrId;
    this.successorOrderId = successorOrderId;
  }
  
  @Override
  public String getCorrelationId() {
    return predecessorCorrId;
  }

  @Override
  protected void executeInternal() {
    Result result = null;
    if( binding == ownBinding ) {
      result = localOsm.updatePredecessorWithNormalLock(binding, predecessorCorrId, successorCorrId, successorOrderId);
    } else {
      result = remoteOsm.updatePredecessor(binding, predecessorCorrId, successorCorrId, successorOrderId);
    }
    
    boolean informSuccessor = false;
    switch( result ) {
      case Later: 
        //ok, nichts mehr weiter zu tun, Successor wird später benachrichtigt werden
      case NotFound: 
        //sollte nicht auftreten: Predecessor existierte beim Erzeugen des Tasks!
        throw new IllegalStateException("OSMTask_UpdatePredecessor: updatePredecessorWithNormalLock returned "+result );
      case Running:
        //ok, nichts mehr weiter zu tun, Successor wird später benachrichtigt werden
        break;
      case Cancel:
        //Successor muss informiert werden, da er auf jeden Fall gecancelt wird
        informSuccessor = true;
        break;
      case Success:
        //Successor muss informiert werden, da er evtl. starten kann
        informSuccessor = true;
        break;
    }
    if( informSuccessor ) {
      //Successor muss informiert werden, dass er wahrscheinlich starten kann
      boolean cancel = result == Result.Cancel;
      
      TreeNode sucTree = predecessorTrees.getTree( successorCorrId );
      if( sucTree == null ) {
        //hier muss kein großer Baum gebaut werden, da hier nur die Informationen des TreeNode 
        //selbst gebraucht werden. Falls doch irgendwann der vollständige Baum in 
        //OSMTask_Preschedule gebraucht wird, wird er dort ergänzt werden.
        sucTree = predecessorTrees.buildShortTree(successorCorrId); 
      } else {
        if( ! sucTree.hasData() ) {
          //Daten müssen existieren, da Successor ja vorhanden ist
          sucTree = predecessorTrees.buildShortTree(successorCorrId);
        }
      }
      TreeNode preTree = predecessorTrees.getTree( predecessorCorrId );
      if( preTree == null ) {
        //hier muss kein großer Baum gebaut werden, da hier nur die Informationen des TreeNode 
        //selbst gebraucht werden. Falls doch irgendwann der vollständige Baum in 
        //OSMTask_Preschedule gebraucht wird, wird er dort ergänzt werden.
        preTree = predecessorTrees.buildShortTree(predecessorCorrId); 
      }
      if( ! preTree.hasData() ) {
        throw new IllegalStateException("predecessorCorrId "+predecessorCorrId+" can not be found");
      }
      Result res2 = updateSuccessorInternal(successorCorrId, sucTree, predecessorCorrId, preTree.getOrderId(), cancel);
      switch( res2 ) {
        case Later:
          break; //evtl. durch Remote-Aufruf, erlaubt
        case Success:
          break; //erfolgreich 
        default:
          throw new IllegalStateException("updateSuccessorInternal("+successorCorrId+",...) returned "+res2);
      }
    }
  }
  
  /**
   * Kopie aus OSMTask_Finish
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

}
