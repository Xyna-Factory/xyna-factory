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



import com.gip.xyna.coherence.coherencemachine.locking.CoherenceLockState;



public class CoherenceObjectCompleteState {

  private final long objectId;

  private final CoherenceState protocolState;
  private final CoherenceDirectoryData directoryData;
  private final CoherenceLockState lockState;


  public CoherenceObjectCompleteState(long objectId, CoherenceState protocolState,
                                      CoherenceDirectoryData directoryData, CoherenceLockState lockState) {
    this.objectId = objectId;
    this.protocolState = protocolState;
    this.directoryData = directoryData;
    this.lockState = lockState;
  }


  public long getObjectID() {
    return this.objectId;
  }


  public CoherenceState getProtocolState() {
    return this.protocolState;
  }


  public CoherenceDirectoryData getDirectoryData() {
    return directoryData;
  }


  public CoherenceLockState getLockState() {
    return lockState;
  }

}
