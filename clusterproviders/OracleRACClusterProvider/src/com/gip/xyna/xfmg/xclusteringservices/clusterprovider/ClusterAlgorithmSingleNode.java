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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;


/**
 *
 */
public class ClusterAlgorithmSingleNode extends ClusterAlgorithmAbstract {

  public ClusterState createCluster(SQLUtils sqlUtils, RemoteInterfaceForClusterStateChangesImplAQ interconnect, int binding) {
    Parameter insertionParameters = new ExtendedParameter(
      binding, ClusterState.SINGLE.toString(), new BooleanWrapper(true));

    sqlUtils.executeDML(SQLStrings.INSERT_INTO_CLUSTERSETUP_SQL, insertionParameters);
    
    try {
      interconnect.start(binding);
    } catch (DBNotReachableException e) {
      //RemoteInterfaceForClusterStateChangesImplAQ konnte nicht gestartet werden,
      //damit wird nun createCluster komplett abgebrochen
      logger.error( "Could not start RemoteInterfaceForClusterStateChangesImplAQ: "+e.getMessage(), e);
      throw new SQLRuntimeException(e);
    }
    return ClusterState.SINGLE;
  }
  
  public ClusterState join(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, int newBinding, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Single node can't join");
  }

  public ClusterState restorePrepare(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    setStateInternally(sqlUtils, rows.getOwn().getBinding(), ClusterState.SINGLE, true);
    return ClusterState.SINGLE;
  }
  
  public ClusterState restoreConnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    return ClusterState.SINGLE;
  }

  public ClusterState changeClusterState(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    if( newState == ClusterState.SINGLE ) {
      return newState;
    }
    throw new IllegalArgumentException("Single node can't change cluster state");
  }
  
  public ClusterState disconnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    throw new UnsupportedOperationException("Node cannot be disconnected when running in single mode");
  }

  public ClusterState shutdown(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    setStateInternally(sqlUtils, rows.getOwn().getBinding(), ClusterState.SINGLE, false);
    return ClusterState.SINGLE;
  }
  
  public ClusterState leaveCluster(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows) {
    //eigenen Eintrag entfernen
    sqlUtils.executeDML(SQLStrings.DELETE_FOR_BINDING_SQL, new Parameter(rows.getOwn().getBinding()));
    return ClusterState.NO_CLUSTER;
  }

  public ClusterState connect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    throw new UnsupportedOperationException("Single node can't connect");
  }

}
