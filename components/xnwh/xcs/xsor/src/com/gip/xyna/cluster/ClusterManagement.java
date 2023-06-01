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
package com.gip.xyna.cluster;



public interface ClusterManagement {

  public ClusterState getCurrentState();


  public void changeState(ClusterState newState);


  public void registerStateChangeHandler(StateChangeHandler handler);


  /**
   * muss aufgerufen werden, um die erreichbarkeit des anderen knotens zu signalisieren. das clustermanagement hat keine eigene
   * implementierung, die die erreichbarkeit des anderen knotens prüft.
   * heisst in spec: onReachabilityChange
   */
  public void setOtherNodeAvailable(boolean available);


  /**
   * wenn dieser knoten vom remote knoten aufgerufen wird, sollen diese methoden aufgerufen werden 
   */
  public ClusterNodeRemoteInterface getThisNodeImpl();


  public boolean getWasMaster();


  public void shutdown();


  public SyncType getOtherNodesSyncType();


  public void registerSyncFinishedCondition();


  public void notifySyncFinishedCondition();
}
