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


/**
 * OSMTask_UpdateSuccessor:
 * <br>
 * Wird von OSMLocalImpl gerufen, wenn für den Aufruf updateSuccessor das nötige Lock nicht erhalten wurde,
 * oder von OSMRemoteProxyImpl, wenn anderer Knoten nicht erreichbar ist
 * <br>
 * Hier in dem separaten Task kann dann solange gewartet werden, bis das Lock erhalten wird. Dann muss
 * erstens der updatePredecessor nachgeholt werden, zweitens aber auch das, was der OSMTask_Preschedule 
 * nicht mehr tun konnte, weil der OSMLocalImpl.updatePredecessor nicht bearbeitet wurde.
 * <br>
 * <pre>
 * Algorithmus:
 * 1) Aufruf OSMLocalImpl.updateSuccessorWithNormalLock (siehe dort)
 * 2) Auswertung Ergebnis updateSuccessorWithNormalLock
 * 2.1) Success:  Successor ist gefunden und evtl. gestartet. 
 *                Nichts weiter zu tun, Predecessor ist bereits angepasst.
 * 2.1) later:    Operation wird später wiederholt, nichts weiter zu tun
 * 2.2) sonstige: nicht erlaubt -&gt; IllegalStateException
 * </pre>
 *
 */
public class OSMTask_UpdateSuccessor extends OSMTask {

  private int binding;
  private String successorCorrId;
  private String predecessorCorrId;
  private long predecessorOrderId;
  private boolean cancel;

  OSMTask_UpdateSuccessor(int binding,
                          String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                          boolean cancel) {
    this.binding = binding;
    this.successorCorrId = successorCorrId;
    this.predecessorCorrId = predecessorCorrId;
    this.predecessorOrderId = predecessorOrderId;
    this.cancel = cancel;
 }
  
  @Override
  public String getCorrelationId() {
    return successorCorrId;
  }

  @Override
  protected void executeInternal() {
    Result result = null;
    if( binding == ownBinding ) {
      result = localOsm.updateSuccessorWithNormalLock(binding, successorCorrId, predecessorCorrId, predecessorOrderId, cancel);
    } else {
      result = remoteOsm.updateSuccessor(binding, successorCorrId, predecessorCorrId, predecessorOrderId, cancel);
    }
    switch( result ) {
      case Success: //Operation war erfolgreich
        break;
      case Later: //Operation wird automatisch wiederholt werden
        break;
      default: //andere Results sollten nicht vorkommen
        throw new IllegalStateException("OSMTask_UpdateSuccessor: updateSuccessorWithNormalLock returned "+result );
    }
  }

}
