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
package com.gip.xyna.xfmg.xods.configuration;



import java.rmi.RemoteException;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResult;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ClusteredConfiguration extends Configuration implements Clustered, ClusteredChangeNotifier, ClusterStateChangeHandler {

  public ClusteredConfiguration() throws XynaException {
    super();
  }


  private static String CLUSTERABLE_COMPONENT = "ClusteredConfiguration";

  private long clusterInstanceId;
  private long clusteredInterfaceId;
  private RMIClusterProvider clusterInstance;

  private ClusterState currentClusterState = ClusterState.NO_CLUSTER;


  @Override
  public void init() throws XynaException {
    super.init();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(ClusteredConfiguration.class, ClusteredConfiguration.class.getSimpleName())
         .before(XynaClusteringServicesManagement.class)
         .execAsync(this::initCluster);
    
  }
  
  
  private void initCluster() {
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(ClusteredConfiguration.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + ClusteredConfiguration.class.getSimpleName() + " as clusterable component.", e);
    }
  }


  public boolean isClustered() {
    return currentClusterState != ClusterState.NO_CLUSTER;
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {

    if (currentClusterState != ClusterState.NO_CLUSTER) {
      // FIXME SPS prio5: von einem cluster auf ein anderes umkonfigurieren?
      throw new RuntimeException("already clustered");
    }

    this.clusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);
    if (clusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }
    clusteredInterfaceId = ((RMIClusterProvider) clusterInstance).addRMIInterface(CLUSTERABLE_COMPONENT, this);

    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, this);

    currentClusterState = clusterInstance.getState();
  }
  
  public void disableClustering() {
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterMgmt.removeClusterStateChangeHandler(clusterInstanceId, this);
    clusterInstanceId = 0;
    clusteredInterfaceId = 0;
    clusterInstance = null;
    currentClusterState = ClusterState.NO_CLUSTER;
  }


  public String getName() {
    return CLUSTERABLE_COMPONENT;
  }


  @Override
  protected void setProperty(String key, String value, boolean isFactoryComponent, boolean clusterwide)
      throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    super.setProperty(key, value, isFactoryComponent, clusterwide);
    
    if (clusterwide) {
      initiateRemoteSet(key, value, isFactoryComponent );
    }
  }


  @Override
  public void removeProperty(String key, boolean clusterwide) throws PersistenceLayerException {
    super.removeProperty(key, clusterwide);
    
    if (clusterwide) {
      initiateRemoteRemove(key);
    }
  }


  private void initiateRemoteSet(final String key, final String value, final boolean isFactoryComponent) throws XFMG_IllegalPropertyValueException {
    if (isClustered()) { // && new XynaPropertyStorable().isClustered(ODSConnectionType.DEFAULT)) {
      try {
        RMIClusterProviderTools.execute(clusterInstance, clusteredInterfaceId,
                                                   new RMIRunnableNoResult<ClusteredChangeNotifier,XFMG_IllegalPropertyValueException>() {

                                                     public void execute(ClusteredChangeNotifier clusteredInterface)
                                                         throws RemoteException, XFMG_IllegalPropertyValueException {
                                                       clusteredInterface.setPropertyRemotely(key, value, isFactoryComponent);
                                                     }
                                                   });
      } catch (InvalidIDException e) {
        throw new RuntimeException(e); //sollte nicht passieren, weil kein removeRmi aufgerufen wird
      }
    }
  }
  
  
  private void initiateRemoteRemove(final String key) {
    if (isClustered()) { // && new XynaPropertyStorable().isClustered(ODSConnectionType.DEFAULT)) {
      try {
        RMIClusterProviderTools.executeNoException(clusterInstance, clusteredInterfaceId,
                                                   new RMIRunnableNoResultNoException<ClusteredChangeNotifier>() {

                                                     public void execute(ClusteredChangeNotifier clusteredInterface)
                                                         throws RemoteException {
                                                       clusteredInterface.removePropertyRemotely(key);
                                                     }
                                                   });
      } catch (InvalidIDException e) {
        throw new RuntimeException(e); //sollte nicht passieren, weil kein removeRmi aufgerufen wird
      }
    }
  }
  
  
  public boolean isReadyForChange(ClusterState newState) {
    return true; //immer bereit
  }
  
  public void onChange(ClusterState newState) {
    this.currentClusterState = newState;
  }


  public void removePropertyRemotely(String key) throws RemoteException {
    try {
      super.removeProperty(key);
    } catch (PersistenceLayerException e) {
      logger.error( "Was unable to remotely remove property '" + key + "'.", e);
    }
  }


  public void setPropertyRemotely(String key, String value, boolean isFactoryComponent) throws RemoteException, XFMG_IllegalPropertyValueException {
    try {
      super.setProperty(key, value, isFactoryComponent, false);
    } catch (PersistenceLayerException e) {
      logger.error( "Was unable to remotely set property '" + key + "'.", e);
    }
  }
}
