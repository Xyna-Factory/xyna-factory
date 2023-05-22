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
package com.gip.xyna.xfmg.xclusteringservices;



import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.ClusteredStorableConfigChangeHandler;



/**
 * Fasst die f�r einen geclusterten Service notwendigen Daten zusammen.
 */
public class ClusterContext implements ClusteredStorableConfigChangeHandler {

  protected Logger logger = Logger.getLogger(ClusterContext.class);

  public static final ClusterContext NO_CLUSTER = new ClusterContext() {
    public void enableClustering(long clusterInstanceId) {
      throw new UnsupportedOperationException("Constant ClusterContext.NO_CLUSTER cannot be changed");
    }
    public void disableClustering() {
      throw new UnsupportedOperationException("Constant ClusterContext.NO_CLUSTER cannot be changed");
    }    
  };

  private boolean clustered;
  private long clusterInstanceId;
  private ClusterProvider clusterInstance;
  private ArrayList<ClusterStateChangeHandler> clusterStateChangeHandlers = new ArrayList<ClusterStateChangeHandler>();


  public ClusterContext() {
    clustered = false;
  }

  public ClusterContext(ClusterStateChangeHandler clusterStateChangeHandler, Clustered clustered) throws XFMG_ClusterComponentConfigurationException {
    this.clustered = false;
    addClusterStateChangeHandler( clusterStateChangeHandler );
    XynaClusteringServicesManagement.getInstance().registerClusterableComponent(clustered);
  }

  
  public ClusterContext(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException {
    this.clusterInstanceId = clusterInstanceId;
    clusterInstance = getClusterMgmt().getClusterInstance(clusterInstanceId);
    enableClustering(clusterInstanceId);
  }

  public ClusterContext(Class<? extends ClusteredStorable<?>> clusteredStorable, ODSConnectionType odsConnectionType)
      throws XFMG_UnknownClusterInstanceIDException {
    ClusteredStorable<?> csInstance;
    try {
      csInstance = clusteredStorable.newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("ClusteredStorable " + clusteredStorable.getSimpleName()
          + " has no default constructor", e);
    }
    if (csInstance.isClustered(odsConnectionType)) {
      //clusteredStorable ist bereits geclustered, daher wird enableClustering nicht mehr gerufen werden
      long clusterInstanceId = csInstance.getClusterInstanceId(odsConnectionType);
      enableClustering(clusterInstanceId);
    } else {
      clustered = false;
    }
  }


  public void enableClustering(long clusterInstanceId) {
    try {
      this.clusterInstanceId = clusterInstanceId;
      XynaClusteringServicesManagementInterface clusterMgmt = getClusterMgmt();
      try {
        clusterInstance = clusterMgmt.getClusterInstance(clusterInstanceId);
      } catch (XFMG_UnknownClusterInstanceIDException e) {
        throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId, e);
      }
      this.clusterInstanceId = clusterInstanceId;
      for (ClusterStateChangeHandler csch : clusterStateChangeHandlers) {
        clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, csch);
      }
      clustered = true;
    } finally {
      if (logger.isDebugEnabled()) {
        logger.debug("enableClustering " + clusterInstanceId 
          + (clusterInstance==null? "failed":
            ("  -->  " + clusterInstance.getClass().getSimpleName() + " " + clusterInstance.getState()) ) 
        );
      }
    }
  }


  public void disableClustering() {
    XynaClusteringServicesManagementInterface clusterMgmt = getClusterMgmt();
    for (ClusterStateChangeHandler csch : clusterStateChangeHandlers) {
      clusterMgmt.removeClusterStateChangeHandler(clusterInstanceId, csch);
    }
    clustered = false;
    clusterInstance = null;
    clusterInstanceId = 0;
  }


  public boolean isClustered() {
    return clustered;
  }


  public long getClusterInstanceId() {
    if (!clustered) {
      throw new IllegalStateException("Component is not clustered.");
    }
    return clusterInstanceId;
  }


  public void addClusterStateChangeHandler(ClusterStateChangeHandler clusterStateChangeHandler) {
    if (clustered) {
      getClusterMgmt().addClusterStateChangeHandler(clusterInstanceId, clusterStateChangeHandler);
    }
    clusterStateChangeHandlers.add(clusterStateChangeHandler);
  }


  public ClusterState getClusterState() {
    return clusterInstance != null ? clusterInstance.getState() : ClusterState.NO_CLUSTER;
  }


  public ClusterProvider getClusterInstance() {
    return clusterInstance;
  }


  private XynaClusteringServicesManagementInterface getClusterMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
  }


  @Override
  public String toString() {
    return "ClusterContext(clusterInstanceId=" + clusterInstanceId + ",clusterProvider="
        + (clusterInstance == null ? "null" : clusterInstance.getClass().getSimpleName()) + ",clusterState="
        + getClusterState() + ")";
  }

  public void readyForClusterStateChange() {
    XynaClusteringServicesManagementInterface clusterMgmt = getClusterMgmt();
    clusterMgmt.readyForClusterStateChange(clusterInstanceId);
  }

  public FutureExecution getFutureExecution() {
    return getClusterMgmt().getFutureExecutionsOnChangeHandler(clusterInstanceId);
  }
  
}
