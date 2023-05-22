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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;


/**
 *
 * FIXME vollst�ndig implementieren!
 */
public class ClusterAlgorithmMultiNodes extends ClusterAlgorithmAbstract {

  public ClusterState createCluster(SQLUtils sqlUtils, RemoteInterfaceForClusterStateChangesImplAQ interconnect, int binding) {
    throw new UnsupportedOperationException("createCluster is only implemented by ClusterAlgorithmSingleNode");
  }

  public ClusterState join(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, int newBinding, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    //Es gibt schon mehrere Eintr�ge in ClusterSetupRowsForUpdate
    //diese sollten schon CONNECTED sein TODO RAC-Cluster >2 Nodes: dies muss doch nicht so sein? 
    
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }

  public ClusterState restorePrepare(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }
  
  public ClusterState restoreConnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }
 
  public ClusterState changeClusterState(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }

  public ClusterState disconnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }

  public ClusterState shutdown(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }
  
  public ClusterState leaveCluster(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows) {
    // Falls es mehr als 2 Cluster-Knoten gibt, muss der State nicht ge�ndert werden, die anderen
    // bleiben einfach CONNECTED.

    //eigenen Eintrag entfernen
    sqlUtils.executeDML(SQLStrings.DELETE_FOR_BINDING_SQL, new Parameter(rows.getOwn().getBinding()));
    return ClusterState.NO_CLUSTER;
  }

  public ClusterState connect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Multi-node is not implemented");
  }

}
