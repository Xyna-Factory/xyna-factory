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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.coherence.ClusterMemberChangeInformation;

import junit.framework.TestCase;


public class CoherenceDirectoryDataTest extends TestCase {

//FIXME assertEquals(<expected>, <actual>) , nicht andersherum => refactorn!
  
  public void testInvalidCreation() {
    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    assertTrue(data.getAllClusterIDsHoldingValidCopies().isEmpty());
    data.checkConsistency();
  }


  public void testSetShared() {
    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.addSharingClusterNode(5);
    data.addSharingClusterNode(7);
    Iterator<Integer> it = new TreeSet<Integer>(data.getAllClusterIDsHoldingValidCopies()).iterator();
    assertEquals(it.next(), new Integer(5));
    assertEquals(it.next(), new Integer(7));
    data.checkConsistency();
  }


  public void testSetExclusiveRemotelyNotSharedBefore() {
    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.setEclusiveOnRemoteNode(3);
    assertEquals(data.getAllClusterIDsHoldingValidCopies().iterator().next(), new Integer(3));
    data.checkConsistency();
  }


  public void testSetExclusiveRemotelySharedBefore() {
    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.addSharingClusterNode(4);
    data.addSharingClusterNode(6);
    data.addSharingClusterNode(3);
    data.setEclusiveOnRemoteNode(3);
    Set<Integer> remoteNodesHoldingCopy = data.getAllClusterIDsHoldingValidCopies();
    assertEquals(remoteNodesHoldingCopy.size(), 1);
    assertEquals(remoteNodesHoldingCopy.iterator().next(), new Integer(3));
    assertEquals(data.getIdOfRandomClusterNodeHoldingValidCopy(new ClusterMemberChangeInformation(-1)), 3);
    data.checkConsistency();
  }


  public void testSetModifiedRemotelyNotSharedBefore() {
    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.setModifiedOnRemoteNode(3);
    assertEquals(data.getAllClusterIDsHoldingValidCopies().iterator().next(), new Integer(3));
    data.checkConsistency();
  }


  public void testSetModifiedRemotelySharedBefore() {

    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.addSharingClusterNode(4);
    data.addSharingClusterNode(6);
    data.addSharingClusterNode(3);
    data.setModifiedOnRemoteNode(7);

    Set<Integer> remoteNodesHoldingCopy = data.getAllClusterIDsHoldingValidCopies();
    assertEquals(remoteNodesHoldingCopy.size(), 1);
    assertEquals(remoteNodesHoldingCopy.iterator().next(), new Integer(7));
    assertEquals(data.getIdOfRandomClusterNodeHoldingValidCopy(new ClusterMemberChangeInformation(-1)), 7);

    data.checkConsistency();

  }


  public void testSetModifiedLocally() {

    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);
    data.setModifiedOnLocalNode();

    Set<Integer> remoteNodesHoldingCopy = data.getAllClusterIDsHoldingValidCopies();
    assertEquals(remoteNodesHoldingCopy.size(), 0);

    data.checkConsistency();

  }


  public void testSetModifiedLocallySharedBefore() {

    CoherenceDirectoryData data = new CoherenceDirectoryData(CoherenceState.EXCLUSIVE);

    data.addSharingClusterNode(4);
    data.addSharingClusterNode(6);
    data.addSharingClusterNode(3);

    data.setModifiedOnLocalNode();

    Set<Integer> remoteNodesHoldingCopy = data.getAllClusterIDsHoldingValidCopies();
    assertEquals(remoteNodesHoldingCopy.size(), 0);

    data.checkConsistency();

  }

}
