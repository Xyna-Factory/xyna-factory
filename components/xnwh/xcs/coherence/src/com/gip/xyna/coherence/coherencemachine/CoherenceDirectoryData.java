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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import com.gip.xyna.coherence.ClusterMemberChangeInformation;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.utils.debugging.Debugger;


/**
 * hält informationen von states auf allen knoten, die das objekt kennen. Speichert auch den Status auf dem lokalen Knoten.
 */
public class CoherenceDirectoryData implements Cloneable, Serializable {

  private static final long serialVersionUID = 3558085878421633710L;
  private static final Debugger debugger = Debugger.getDebugger();

  private static final Random random = new Random();


  /**
   * zuordnung von node-id zu state auf dem knoten. invalid states tauchen in der map nie auf. kann states von knoten
   * enthalten, die bereits nicht mehr bestandteil des clusters sind, weil beim disconnect eines knotens die
   * coherence-states lazy gelöscht werden. siehe auch
   * {@link #removeInvalidClusterMembers(ClusterMemberChangeInformation)
   */
  private final Map<Integer, CoherenceState> remoteStateByClusterNodeID;
  private Integer[] remoteStateArray; //für performance

  //redundant auch in map vorhanden
  private Integer exclusiveNode;
  private Integer modifiedNode;
  private transient int lastUpdatedClusterMemberChangeIndex = -1;
  private CoherenceState localState;


  public CoherenceDirectoryData(CoherenceState localState) {
    // TODO prio4: performance: replace by a map that can handle primitive types as keys
    remoteStateByClusterNodeID = new HashMap<Integer, CoherenceState>();
    this.localState = localState;
  }


  public void setEclusiveOnRemoteNode(int remoteNodeID) {
    remoteStateByClusterNodeID.clear();
    remoteStateByClusterNodeID.put(remoteNodeID, CoherenceState.EXCLUSIVE);
    modifiedNode = null;
    exclusiveNode = remoteNodeID;
    remoteStateArray = null;
  }


  @Override
  public CoherenceDirectoryData clone() {
    CoherenceDirectoryData result = new CoherenceDirectoryData(localState);
    result.exclusiveNode = exclusiveNode;
    result.modifiedNode = modifiedNode;
    result.remoteStateByClusterNodeID.putAll(remoteStateByClusterNodeID);
    // no need to clone the remoteStateArray since that is recreated lazily -> reduce the amount of
    // data that is transferred when a new cluster node loads its initial data
    return result;
  }


  public void setModifiedOnRemoteNode(int remoteNodeID) {
    remoteStateByClusterNodeID.clear();
    remoteStateByClusterNodeID.put(remoteNodeID, CoherenceState.MODIFIED);
    modifiedNode = remoteNodeID;
    exclusiveNode = null;
    remoteStateArray = null;
  }


  /**
   * fügt neuen eintrag in directory data als shared hinzu
   */
  public void addSharingClusterNode(int requestingClusterNodeID) {
    remoteStateByClusterNodeID.put(requestingClusterNodeID, CoherenceState.SHARED);
    remoteStateArray = null;
  }


  public void removeInvalidClusterMembers(ClusterMemberChangeInformation clusterMemberChangeInformation) {
    //TODO ist evtl besser, die clusterMemberChangeInformation als transientes feld zu halten und immer zugriff darauf zu haben?

    int currentClusterMemberChangeIndex = clusterMemberChangeInformation.getCurrentClusterMemberChangeIndex();
    if (lastUpdatedClusterMemberChangeIndex != currentClusterMemberChangeIndex) {
      synchronized (this) {
        if (lastUpdatedClusterMemberChangeIndex != currentClusterMemberChangeIndex) {
          if (debugger.isEnabled()) {
            debugger.debug("removing invalid entries in coherencedata");
          }
          
          Iterator<Integer> it = remoteStateByClusterNodeID.keySet().iterator();
          while (it.hasNext()) {
            final int nodeId = it.next();
            //keine synchronisierung auf das invalidNodes set notwendig, weil es nur während des global
            // locks geändert wird und dann hier keiner darauf zugreift.
            if (clusterMemberChangeInformation.getInvalidNodes().contains(nodeId)) {
              if (debugger.isEnabled()) {
                debugger.debug(new Object() {
                  @Override
                  public String toString() {
                    return "removing invalid entry in coherencedata for nodeId " + nodeId;
                  }
                });
              }
              it.remove();
            }
          }
          if (remoteStateByClusterNodeID.size() == 0 && localState == CoherenceState.SHARED) {
            localState = CoherenceState.EXCLUSIVE;
          }
          
          lastUpdatedClusterMemberChangeIndex = currentClusterMemberChangeIndex;
        }
      }
    }
  }


  public int getIdOfRandomClusterNodeHoldingValidCopy(ClusterMemberChangeInformation clusterMemberChangeInformation) {
    removeInvalidClusterMembers(clusterMemberChangeInformation);

    if (exclusiveNode != null) {
      return exclusiveNode;
    }
    if (modifiedNode != null) {
      return modifiedNode;
    }

    if (remoteStateByClusterNodeID.size() == 0) {
      throw new ClusterInconsistentException("payload of object could not be found in any cluster node.");
    }

    int sizeOfMap = remoteStateByClusterNodeID.size();
    if (remoteStateArray == null) {
      remoteStateArray = remoteStateByClusterNodeID.keySet().toArray(new Integer[sizeOfMap]);
    }

    if (sizeOfMap > 1) {
      int randomIndex = random.nextInt(sizeOfMap);
      return remoteStateArray[randomIndex];
    } else {
      return remoteStateArray[0];
    }

  }


  Set<Integer> getAllClusterIDsHoldingValidCopies() {
    //FIXME invalid ids entfernen
    return remoteStateByClusterNodeID.keySet();
  }


  public void setShared(int[] nodesHoldingSharedObject, int ownId) {
    remoteStateByClusterNodeID.clear();
    exclusiveNode = null;
    modifiedNode = null;
    for (int nodeId : nodesHoldingSharedObject) {
      if (nodeId != ownId) {
        remoteStateByClusterNodeID.put(nodeId, CoherenceState.SHARED);
      }
    }
    remoteStateArray = null;
  }


  public void addDirectoryData(int remoteCusterNodeId, CoherenceState state) {
    switch (state) {
      case EXCLUSIVE :
        setEclusiveOnRemoteNode(remoteCusterNodeId);
        break;
      case MODIFIED :
        setModifiedOnRemoteNode(remoteCusterNodeId);
        break;
      case SHARED :
        addSharingClusterNode(remoteCusterNodeId);
        break;
      default :
    }
  }


  /**
   * fügt node als shared hinzu und convertiert ggfs eine derzeitig modified/exclusive node zu shared.
   */
  public void convertToShared(int requestingClusterNodeID) {
    if (modifiedNode != null) {
      remoteStateByClusterNodeID.put(modifiedNode, CoherenceState.SHARED);
      modifiedNode = null;
    }
    if (exclusiveNode != null) {
      remoteStateByClusterNodeID.put(exclusiveNode, CoherenceState.SHARED);
      exclusiveNode = null;
    }
    remoteStateByClusterNodeID.put(requestingClusterNodeID, CoherenceState.SHARED);
    remoteStateArray = null;
  }


  public void setModifiedOnLocalNode() {
    modifiedNode = null;
    exclusiveNode = null;
    remoteStateByClusterNodeID.clear();
    remoteStateArray = null;
  }


  void checkConsistency() {

    if (modifiedNode != null) {
      if (exclusiveNode != null) {
        throw new ClusterInconsistentException("modified and exclusive node are set");
      }
      if (remoteStateByClusterNodeID.get(modifiedNode) != CoherenceState.MODIFIED) {
        throw new ClusterInconsistentException("modified node is set inconsistently");
      }
    }
    if (exclusiveNode != null) {
      // modifiedNode cannot be null here since that has been checked above
      if (remoteStateByClusterNodeID.get(exclusiveNode) != CoherenceState.EXCLUSIVE) {
        throw new ClusterInconsistentException("exclusive node is set inconsistently");
      }
    }

    boolean foundShared = false, foundExclusive = false, foundModified = false;
    for (Entry<Integer, CoherenceState> e : remoteStateByClusterNodeID.entrySet()) {
      switch (e.getValue()) {
        case MODIFIED :
          if (foundModified || foundShared) {
            throw new ClusterInconsistentException("object directory data corrupted");
          }
          if (modifiedNode == null) {
            throw new ClusterInconsistentException("object directory data corrupted");
          }
          foundModified = true;
          break;
        case EXCLUSIVE :
          if (foundExclusive || foundShared) {
            throw new ClusterInconsistentException("object directory data corrupted");
          }
          if (exclusiveNode == null) {
            throw new ClusterInconsistentException("object directory data corrupted");
          }
          foundExclusive = true;
          break;
        case SHARED :
          if (foundExclusive || foundModified) {
            throw new ClusterInconsistentException("object directory data corrupted");
          }
          foundShared = true;
          break;
        case INVALID :
          throw new ClusterInconsistentException("object directory data corrupted");
        default :
          throw new RuntimeException("unexpected state: " + e.getValue());
      }
    }

  }


  CoherenceState getStateOnRemoteNode(int clusterNodeID) {
    if (clusterNodeID == exclusiveNode.intValue()) {
      return CoherenceState.EXCLUSIVE;
    } else if (clusterNodeID == modifiedNode.intValue()) {
      return CoherenceState.MODIFIED;
    }
    CoherenceState remoteState = remoteStateByClusterNodeID.get(clusterNodeID);
    if (remoteState == null) {
      return CoherenceState.INVALID;
    } else {
      return remoteState;
    }
  }


  public boolean isModifiedExclusiveOnSpecificNode(int clusterNodeId) {
    return (modifiedNode != null && modifiedNode == clusterNodeId)
        || (exclusiveNode != null && exclusiveNode == clusterNodeId);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(40);
    sb.append("modified=").append(modifiedNode).append(" exclusive=").append(exclusiveNode).append(" states=")
        .append(remoteStateByClusterNodeID);
    return sb.toString();
  }


  public CoherenceState getLocalStateWithCheck(ClusterMemberChangeInformation clusterMemberChangeInfo) {
    removeInvalidClusterMembers(clusterMemberChangeInfo);
    return localState;
  }


  public void setLocalState(CoherenceState state) {
    localState = state;
  }


  public CoherenceState getLocalState() {
    return localState;
  }

}
