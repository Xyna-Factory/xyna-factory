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
package com.gip.xyna.cluster;

/**
 * schnittstelle zwischen zwei cluster-knoten
 */
public interface ClusterNodeRemoteInterface {

  /**
   * synchronisierungs-request, wenn man vorher noch nicht zum anderen knoten verbunden war 
   */
  public SyncResponse syncWasNeverConnectedBefore() throws TimeoutException;

  /**
   * synchronisierungs-request, nachdem man fr�her master gewesen ist, also der andere knoten sich korrekt abgemeldet hat 
   */
  public SyncResponse syncWasMaster() throws TimeoutException;

  /**
   * synchronisierungs-request, nachdem man fr�her einmal connected war und sich nie richtig vom anderen knoten abgemeldet hat
   */
  public SyncResponse syncWasConnectedBefore() throws TimeoutException;
  
  /**
   * setzt den �bergebenen state auf dem knoten 
   */
  public void changeState(ClusterState newState) throws TimeoutException;

  /**
   * benachrichtigen, wenn alle SYNC statechangehandler fertiggelaufen sind. 
   */
  public void syncFinished() throws TimeoutException;

}
