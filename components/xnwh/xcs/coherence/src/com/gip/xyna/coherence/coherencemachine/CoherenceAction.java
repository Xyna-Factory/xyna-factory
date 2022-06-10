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
package com.gip.xyna.coherence.coherencemachine;

import java.io.Serializable;



/**
 * Superclass for actions which can be executed between nodes of a cluster.
 */
public abstract class CoherenceAction implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Id of the initializing node
   */
  private final int requestingClusterNodeID;
  /**
   * Id of the object on which the action should be executed
   */
  private final long targetObjectID;
  /**
   * Size of the current lock circle
   */
  private int lockCircleSize = -1;


  public CoherenceAction(int requestingClusterNodeID, long targetObjectID) {
    this.requestingClusterNodeID = requestingClusterNodeID;
    this.targetObjectID = targetObjectID;
  }


  /**
   * Get the id of the node who initializes the action
   * @return node id
   */
  public final int getRequestingClusterNodeID() {
    return this.requestingClusterNodeID;
  }


  /**
   * Get the id of the object on which the action should be executed
   * @return object id
   */
  public final long getTargetObjectID() {
    return this.targetObjectID;
  }


  public final void setLockCircleSize(int size) {
    this.lockCircleSize = size;
  }


  public final int getLockCircleSize() {
    return this.lockCircleSize;
  }


  public abstract CoherenceActionType getActionType();


  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder(82).append(super.toString()).append(" (object id = ").append(targetObjectID).append(")");
    return sb.toString();
  }

}
