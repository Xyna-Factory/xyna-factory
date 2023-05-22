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

package com.gip.xyna.coherence.coherencemachine;



public class ReadObjectAction extends CoherenceAction {

  private static final long serialVersionUID = 7491257448747483434L;

  private final int clusterToProvideReturnData;


  public ReadObjectAction(long objectId, int requestingClusterNodeID, int clusterToProvideReturnData) {
    super(requestingClusterNodeID, objectId);
    this.clusterToProvideReturnData = clusterToProvideReturnData;
  }


  @Override
  public final CoherenceActionType getActionType() {
    return CoherenceActionType.READ;
  }


  public boolean needsToReturnValue(int clusterNodeID) {
    return this.clusterToProvideReturnData == clusterNodeID;
  }


  public int getClusterToProvideReturnData() {
    return this.clusterToProvideReturnData;
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(110);
    sb.append(super.toString()).append(" (clusterIdToProvideData=").append(clusterToProvideReturnData).append(")");
    return sb.toString();
  }

}
