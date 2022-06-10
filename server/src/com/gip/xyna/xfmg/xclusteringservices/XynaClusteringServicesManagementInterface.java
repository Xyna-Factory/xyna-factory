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

package com.gip.xyna.xfmg.xclusteringservices;



import java.util.Map;
import java.util.Set;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterProviderFilesNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterProviderException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public interface XynaClusteringServicesManagementInterface {

  public static class ClusterParameterInformation {

    public String name;
    public String initializationParameterInformation;
    public String connectionParameterInformation;
  }


  @Deprecated //Use XynaClusteringServicesManagement.class
  public static final int FUTURE_EXECUTION_ID__CLUSTERING_SERVICES_CLUSTERED_COMPONENTS =
      XynaFactory.getInstance().getFutureExecution().nextId();
  public static final int FUTURE_EXECUTION_ID__START_CLUSTERPROVIDERS_FINISHED =
      XynaFactory.getInstance().getFutureExecution().nextId();
  public static final int FUTURE_EXECUTION_PREINIT_ID = XynaFactory.getInstance().getFutureExecutionForInit().nextId();


  public long joinCluster(String clusterType, String[] parameter, String description)
      throws XFMG_UnknownClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException,
      XFMG_ClusterConnectionException, PersistenceLayerException;


  public long setupNewCluster(String clusterType, String[] parameter, String description)
      throws XFMG_UnknownClusterProviderException, XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException, PersistenceLayerException;


  /**
   * für jeden unterstützten clustertype (z.B. oracle RAC, direct RMI-communication, etc) ein string, der beschreibt,
   * welche parameter {@link #joinCluster(String, String[], String)} und
   * {@link #setupNewCluster(String, String[], String)} haben.
   */
  public ClusterParameterInformation[] getInformationForSupportedClusterTypes();


  public void leaveCluster(long clusterId) throws XFMG_UnknownClusterInstanceIDException;


  public Map<Long, ClusterInformation> getClusterInstancesInformation();
  
  public FutureExecution getFutureExecutionsOnChangeHandler(long clusterInstanceId);


  // public long getClusterStateHash(long clusterId) throws XFMG_UnknownClusterInstanceIDException;

  /**
   * @param clusterType im verzeichnis server/clusterproviders muss ein unterverzeichnis mit dem übergebenen namen
   *          existieren und darin ein jar-file, welches eine klasse {@link Constants#CLUSTER_PROVIDER_BASE_PACKAGE}
   *          /&lt;clusterType&gt; enthält.
   */
  public void registerClusterProvider(String clusterType) throws XFMG_ClusterProviderFilesNotFoundException,
      PersistenceLayerException;


  public ClusterProvider getClusterInstance(long clusterId) throws XFMG_UnknownClusterInstanceIDException;


  public void configureForCluster(String clusterableComponent, long clusterId)
      throws XFMG_UnknownClusterInstanceIDException, XFMG_ClusterComponentConfigurationException,
      PersistenceLayerException;


  public Set<Clustered> listClusterableComponents();

  /**
   * @throws XFMG_ClusterComponentConfigurationException falls früher bereits configured war, und clustered den fehler bei enableCluster wirft
   */
  public void registerClusterableComponent(Clustered clusterableComponent) throws XFMG_ClusterComponentConfigurationException;


  public void addClusterStateChangeHandler(long clusterInstanceId, ClusterStateChangeHandler clusterStateChangeHandler);


  public void removeClusterStateChangeHandler(long clusterInstanceId,
                                              ClusterStateChangeHandler clusterStateChangeHandler);
  
  public void readyForClusterStateChange(long clusterInstanceId);

}
