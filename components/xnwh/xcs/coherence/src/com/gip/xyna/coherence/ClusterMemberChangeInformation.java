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
package com.gip.xyna.coherence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.coherence.management.ClusterMember;
import com.gip.xyna.coherence.utils.debugging.Debugger;


public class ClusterMemberChangeInformation {

  private static final Debugger debugger = Debugger.getDebugger();
  
  private Set<Integer> clusterIds = new HashSet<Integer>();

  //FIXME prio3: im auge behalten, dass das ausschliesslich wächst! irgendwann mal leeren und alle directorydaten updaten.
  //achtung dann auch unten bei der methode getNewClusterMemberId
  private Set<Integer> removedClusterIds = new HashSet<Integer>();


  private AtomicInteger memberChangedCount = new AtomicInteger(0);
  private int ownClusterNodeID;
  private int clusterPosition; //unique zu einem zeitpunkt. zwischen 0 und anzahl der members-1


  public ClusterMemberChangeInformation(int ownClusterNodeID) {
    this.ownClusterNodeID = ownClusterNodeID;
  }


  public void update(ClusterMember[] members) {
    if (debugger.isEnabled()) {
      debugger.debug(new Object() {
        private int _ownClusterNodeID = ownClusterNodeID;
        private int _memberChangedCount = memberChangedCount.get();
        @Override
        public String toString() {
          return "updating clustermembers on node " + _ownClusterNodeID + " current value of memberchangedcount = "
              + _memberChangedCount;
        }
      });
    }
    Set<Integer> newClusterIds = new HashSet<Integer>();

    for (int i = 0; i < members.length; i++) {
      if (members[i].getId() == ownClusterNodeID) {
        clusterPosition = i;
      }
      clusterIds.remove(members[i].getId());
      newClusterIds.add(members[i].getId());
    }

    //clusterIds enthält nur noch entfernte ids;
    removedClusterIds.addAll(clusterIds);
    if (debugger.isEnabled()) {
      debugger.debug("invalid cluster ids = " + clusterIds);
    }

    memberChangedCount.incrementAndGet();
    clusterIds = newClusterIds;

  }


  public int getClusterPosition() {
    return clusterPosition;
  }


  public Set<Integer> getInvalidNodes() {
    return removedClusterIds;
  }


  public int getCurrentClusterMemberChangeIndex() {
    return memberChangedCount.get();
  }


  private int getMax(Set<Integer> set) {
    if (set == null || set.size() == 0) {
      return 0;
    }
    return Collections.max(set);
  }


  public int getNewClusterMemberId() {
    return Math.max(getMax(clusterIds), getMax(removedClusterIds)) + 1;
  }

}
