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

package com.gip.xyna.coherence.management;

import java.util.List;

import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProvider;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProvider;


/**
 * pattern of usage:<br>
 * 1. create Instance of Implementation of {@link XynaCoherenceClusterManagement}<br>
 * 2. n x {@link #addCallee(InterconnectCalleeProvider)}<br>
 * 3. either {@link #setupNewCluster()} or {@link #connectToClusterLocally(NodeConnectionProvider)}. both is not
 * possible.<br>
 * <p>
 * 4. do stuff<br>
 * <p>
 * optional 10. {@link #disconnectFromCluster()} -&gt; goto step 2<br>
 * 11. {@link #shutdown()}<br>
 */
public interface XynaCoherenceClusterManagement {


  /**
   * Adds an {@link InterconnectCallee} that will be used by other nodes to contact the local node. May not be called after 
   * initialization with either {@link #setupNewCluster()} or {@link #connectToClusterLocally(NodeConnectionProvider)}.
   */
  public void addCallee(InterconnectCalleeProvider provider);


  /**
   * Get a list of all callees that are provided by the local cluster node.
   */
  public List<InterconnectCallee> getCallees();


  /**
   * Connect to an existing cluster. The {@link NodeConnectionProvider} is used to establish the first connection to a
   * remote node. Note that this is only possible while not being part of another cluster.
   */
  public void connectToClusterLocally(NodeConnectionProvider nodeConnectionProvider);
  
  /**
   * Connect to the cluster that this node was connected to before, if any. 
   */
  public void connectToCluster();


  /**
   * Sets up the node as a new cluster. This is only possible while not being part of another cluster.
   */
  public void setupNewCluster();


  /**
   * Disconnects from an existing cluster. If the node is the only node in the cluster, the cluster will be shut down.
   * All {@link InterconnectCallee}'s will be reinitialized, so that {@link #setupNewCluster()} or
   * {@link #connectToClusterLocally(NodeConnectionProvider)} can be called again. Basically the state will be reset to
   * the same as after step 2 from the documentation of this interface (more callees may be added).
   */
  public void disconnectFromCluster();

  /**
   * if not disconnected from cluster, {@link #disconnectFromCluster()} is called internally.<br>
   * shutdown of threadpools, callees, etc.
   * instance may not be reused after the call of this method. 
   */
  public void shutdown();

  /**
   * Pauses all access on the cluster globally. The method wont return until there are no more threads active within
   * cluster methods on any local or remote node.
   */
  public void pauseCluster();


  /**
   * Resumes a previously paused cluster.
   */
  public void resumeCluster();


  /**
   * @return Information on the state of the local cluster node. This does not require any interaction with remote
   *         nodes.
   */
  public ClusterNodeState getLocalClusterNodeState();


  /**
   * @return Information on the state of all cluster nodes (including the local one).
   */
  public ClusterState getCompleteClusterState();


  /**
   * Collects information on the whole object pool. If the flag {@code includePerObjectInformation} is set, information
   * on every single object is included. Node that the latter requires pausing the whole cluster while collecting the
   * information.
   */
  public ObjectPoolInformation getObjectPoolInformation(boolean includePerObjectInformation);

}
