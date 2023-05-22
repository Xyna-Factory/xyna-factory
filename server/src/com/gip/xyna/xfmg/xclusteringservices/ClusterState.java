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

package com.gip.xyna.xfmg.xclusteringservices;



public enum ClusterState {

  /**
   * A cluster is configured on the local node and the connection to the other nodes is established.
   */
  CONNECTED(false, true),
  
  /**
   * wie disconnected, nur dass man zus�tzlich weiss, dass seit startup noch nie connected gewesen ist.
   */
  NEVER_CONNECTED(true, true), //TODO wirklich stable?

  /**
   * Corresponds to 'temporarily disconnected'
   */
  DISCONNECTED(true, false),

  /**
   * A cluster is set up on the local node but no other nodes are known.
   */
  SINGLE(false, true),

  /**
   * This is the state in which no cluster is configured.
   */
  NO_CLUSTER(false, true),

  /**
   * Nodes are disconnected and there is a qualitative difference where the local node is authorized to perform "MASTER"
   * tasks.
   */
  DISCONNECTED_MASTER(true, true),

  /**
   * Counterpart to {@link #DISCONNECTED_MASTER}.
   */
  DISCONNECTED_SLAVE(true, false),
  
  /**
   * Node is starting and does currently not know about its state which it will have soon. Nach Init.
   */
  STARTING(true, false),
  
  /**
   * vor STARTING
   */
  INIT(true, false),
  
  SHUTDOWN(true, false),
  
  /**
   * zwischenzustand zwischen disconnected/never_connected und connected, wenn also keiner der beiden knoten vorher MASTER/SLAVE war.
   */
  SYNC_PARTNER(false, false),
  
  /**
   * zwischenzustand zwischen disconnected_master und connected. knoten war vorher DISC_MASTER
   */
  SYNC_MASTER(false, true), //TODO wirklich stable?
  
  /**
   * zwischenzustand zwischen disconnected_slave und connected. knoten war vorher DISC_SLAVE
   */
  SYNC_SLAVE(false, false);


  private final boolean disconnected;
  private final boolean stable;


  private ClusterState(boolean isDisconnected, boolean stable ) {
    this.disconnected = isDisconnected;
    this.stable = stable;
  }


  /**
   * Liegt eine Trennung vom anderen Knoten vor? (SINGLE und NO_CLUSTER sind nicht getrennt!)
   * @return
   */
  public boolean isDisconnected() {
    return this.disconnected;
  }
  
  /**
   * Liegt ein stabiler Zustand vor? Ohne �u�ere Einwirkung wird sich der Zustand nicht �ndern.
   * @return
   */
  public boolean isStable() {
    return stable;
  }

  public boolean in(ClusterState state) {
    return this == state;
  }
  
  public boolean in(ClusterState state1, ClusterState state2 ) {
    return this == state1 || this == state2;
  }
  
  public boolean in(ClusterState state1, ClusterState state2, ClusterState state3) {
    return this == state1 || this == state2 || this == state3;
  }
  
  public boolean in(ClusterState ... states) {
    for( ClusterState cs : states ) {
      if( this == cs ) {
        return true;
      }
    }
    return false;
  }

}
