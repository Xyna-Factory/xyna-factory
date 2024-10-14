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
package com.gip.xyna.coherence.coherencemachine;

import java.io.Serializable;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.ClusterMemberChangeInformation;
import com.gip.xyna.coherence.coherencemachine.locking.LockObject;


/**
 * Representation of an object shared in the cluster.
 *
 */
public class CoherenceObject implements Serializable {

  // serializable since it is transmitted at least on cluster connect
  private static final long serialVersionUID = -5614080389623739339L;


  private final long objectId;
  private final CoherenceDirectoryData directoryData;

  /**
   * Lock object for synchronizing accesses to the object
   */
  private LockObject lockObject;
  /**
   * Data/content of the object
   */
  private CoherencePayload payload;
  
  private CoherenceObject(long objectId, CoherenceDirectoryData directoryData) {
    this.objectId = objectId;
    this.directoryData = directoryData;
    lockObject = new LockObject(objectId);
  }

  public CoherenceObject(CoherencePayload payload, long objectId) {
    this(objectId, new CoherenceDirectoryData(CoherenceState.EXCLUSIVE));
    this.payload = payload;
  }


  public static CoherenceObject createNewInvalidObjectForCreate(long objectId, int objectCreatorId) {
    CoherenceDirectoryData dirData = new CoherenceDirectoryData(CoherenceState.INVALID);
    dirData.setEclusiveOnRemoteNode(objectCreatorId);
    CoherenceObject result = new CoherenceObject(objectId, dirData);
    return result;
  }

  /**
   * auf eigene gefahr aufrufen! checkt nicht nochmal auf invalide clustermemberinfos
   */
  public CoherenceState getState() {
    return directoryData.getLocalState();
  }
  
  public CoherenceState getStateWithCheck(ClusterMemberChangeInformation clusterMemberChangeInfo) {
    return directoryData.getLocalStateWithCheck(clusterMemberChangeInfo);
  }


  public CoherenceDirectoryData getDirectoryData() {
    return directoryData;
  }

  public long getId() {
    return objectId;
  }


  public CoherencePayload getPayload() {
    return payload;
  }


  public void setState(CoherenceState state) {
    directoryData.setLocalState(state);
  }

    
  public boolean isInvalid() {
    //clustermemberinvalid update nicht notwendig, weil es nur zwischen shared und exclusive unterscheidet
    return getState() == CoherenceState.INVALID;
  }


  public boolean isModified() {
    //clustermemberinvalid update nicht notwendig, weil es nur zwischen shared und exclusive unterscheidet
    return getState() == CoherenceState.MODIFIED;
  }


  public void setPayload(CoherencePayload payload) {
    this.payload = payload;
  }


  public void onEvent(ObjectChangeEvent event, CacheController controller) {
    final CoherencePayload payload = this.payload; 
    if (payload != null) {
      payload.onChange(event, controller);
    }
  }


  public static CoherenceObject createInvalidCopyWithoutPayload(CoherenceObject o) {
    CoherenceObject result = new CoherenceObject(o.getId(), o.getDirectoryData().clone());
    result.lockObject = o.lockObject.createCopyForNewClusterNode();
    result.setState(CoherenceState.INVALID);
    return result;
  }


  public static CoherenceObject createCopy(CoherenceObject o) {
    CoherenceObject result = new CoherenceObject(o.getId(), o.getDirectoryData().clone());
    result.lockObject = o.lockObject.createCopyForNewClusterNode();
    result.setPayload(o.getPayload());
    return result;
  }


  public boolean isExclusive() {
    return getState() == CoherenceState.EXCLUSIVE;
  }


  public LockObject getLockObject() {
    return lockObject;
  }


  private boolean needToReleaseRemoteLocksOnUnlock;


  public void setNeedToReleaseRemoteLocksOnUnlock(boolean needToReleaseRemoteLocksOnUnlock) {
    this.needToReleaseRemoteLocksOnUnlock = needToReleaseRemoteLocksOnUnlock;
  }


  public boolean needToReleaseRemoteLocksOnUnlock() {
    return needToReleaseRemoteLocksOnUnlock;
  }

}
