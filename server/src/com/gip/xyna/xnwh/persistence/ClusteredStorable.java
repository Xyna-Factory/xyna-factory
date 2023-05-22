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
package com.gip.xyna.xnwh.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;


public abstract class ClusteredStorable<T extends Storable<T>> extends Storable<T> {

  private static final long serialVersionUID = 1L;

  public static final String COL_BINDING = "binding";

  @Column(name = COL_BINDING)
  private int binding;


  public ClusteredStorable(int binding) {
    this.binding = binding;
  }


  public final int getBinding() {
    return binding;
  }


  public final void setBinding(int binding) {
    this.binding = binding;
  }


  public final ClusterProvider getClusterInstance(ODSConnectionType conType) {
    return ODSImpl.getInstance().getClusterInstance(conType, getClass()); //TODO cachen
  }

  
  public final ClusterState getClusterState(ODSConnectionType conType) {
    ClusterProvider cp = getClusterInstance(conType);
    
    if (cp == null) {
      return ClusterState.NO_CLUSTER;
    } else {
      return cp.getState();
    }
  }
  

  public final boolean isClustered(ODSConnectionType conType) {
    return getClusterInstance(conType) != null; // FIXME cachen bzw. direkt auf die PL-Instanz gehen
  }


  public <U extends ClusteredStorable> void setAllFieldsFromData(U data) {
    this.binding = data.getBinding();
  }


  public static void fillByResultSet(ClusteredStorable<?> cs, ResultSet rs) throws SQLException {
    cs.binding = rs.getInt(COL_BINDING);
  }


  public final int getLocalBinding(ODSConnectionType conType) {
    ClusterProvider clusterInstance = getClusterInstance(conType);
    if (clusterInstance != null) {
      return clusterInstance.getLocalBinding();
    }
    return XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;
  }


  public final long getClusterInstanceId(ODSConnectionType conType) {
    return ODSImpl.getInstance().getClusterInstanceId(conType, getClass());
  }

}
