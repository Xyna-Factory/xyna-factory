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

import java.util.List;


/**
 *
 */
public interface OSMInterface {

  public enum OrderState {
    WaitingForPredecessor,  //Auftrag wartet noch auf Predecessoren
    CanBeStarted,           //Auftrag kann gestartet werden
    HasToBeCanceled,        //Auftrag muss abgebrochen werden wegen AutoCancel des Vorgängers
    HasCyclicDependencies,  //Auftrag hat zyklische Predecessoren und muss deshalb abgebrochen werden
    NotFound,               //Auftrag wurd nicht mehr gefunden
    AlreadyFinished;        //Auftrag ist bereits fertig. Dies kann passieren, wenn ein Predecessor fertig wird,
                            //sein Successor aber bereits gelaufen ist (z.B. gecancelt wurde) 
  }
  
  /**
   * Auftrag mit angegebener ID muss nicht länger warten und kann durch den Scheduler ausgeführt werden
   * oder muss abgebrochen werden
   * @param correlationId
   * @param id
   * @param orderState
   * @param cycle
   */
  void readyToRun(String correlationId, long id, OrderState orderState, List<String> cycle );

  /**
   * Was ist das lokale Binding?
   * @return
   */
  int getBinding();
  
  
}
