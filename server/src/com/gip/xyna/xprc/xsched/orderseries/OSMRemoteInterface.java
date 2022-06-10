/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 *
 */
public interface OSMRemoteInterface extends Remote {

  public static enum Result {
    NotFound, //Datensatz wurde nicht gefunden
    Later,    //Operation muss wiederholt werden
    Cancel,   //Auftrag muss abgebrochen werden (Predecessor mit AutoCancel)
    Success,  //Auftrag ist erfolgreich
    Running;  //Aufrag läuft gerade
  }
  
  /**
   * @param binding
   * @param successorCorrId
   * @param predecessorCorrId
   * @param predecessorOrderId
   * @param cancel
   * @return
   */
  Result updateSuccessor(int binding, 
                         String successorCorrId, 
                         String predecessorCorrId, long predecessorOrderId,
                         boolean cancel) throws RemoteException;

  /**
   * @param binding
   * @param predecessorCorrId
   * @param successorCorrId
   * @param successorOrderId
   * @return
   */
  Result updatePredecessor(int binding, 
                           String predecessorCorrId,
                           String successorCorrId, long successorOrderId) throws RemoteException;
  
  

}
