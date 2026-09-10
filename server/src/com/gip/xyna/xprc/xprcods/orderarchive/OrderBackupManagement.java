/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xprcods.orderarchive;



import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.sharedresources.SharedResourceConfigurationChangeListener;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceSynchronizer;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;



public class OrderBackupManagement extends Section {

  public static final String DEFAULT_NAME = "Order Backup Management";
  public static final String XYNA_ORDERBACKUP_SR = "xyna.orderbackup";

  private final OrderBackupSharedResourceChangeListener listener;
  private final OrderBackupSharedResourceProcessing sharedResourceProcessing;

  private boolean sharedResourceConfigured;


  public OrderBackupManagement() throws XynaException {
    super();
    listener = new OrderBackupSharedResourceChangeListener(this);
    sharedResourceProcessing = new OrderBackupSharedResourceProcessing();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("startPersistedOrders", "startPersistedOrders") //
        .after(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION) //
        .after(OrderStartupAndMigrationManagement.class) //
        .after(SharedResourceManagement.FUTURE_EXECUTION_ID) //
        .execAsync(this::startPersistedOrders);
  }


  @Override
  protected void shutdown() throws XynaException {
    if(sharedResourceConfigured) {
      sharedResourceProcessing.stop();
    }
  }


  private void startPersistedOrders() {
    SharedResourceManagement srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    srm.addSharedResource(XYNA_ORDERBACKUP_SR, listener);
    sharedResourceConfigured = srm.hasConfiguredSynchronizer(XYNA_ORDERBACKUP_SR);
    if (sharedResourceConfigured) {
      enableSharedResourceManagement();
      return;
    }

    //load persisted and paused orders in a separate thread
    ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ClusterState clusterState = getClusterState(ods);
    OrderStartupAndMigrationManagement.getInstance().startLoadingAtStartup(clusterState);
    if (clusterState == ClusterState.DISCONNECTED_MASTER) {
      OrderStartupAndMigrationManagement.getInstance().startMigrating(clusterState, 0);
    }
  }


  private ClusterState getClusterState(ODS ods) {
    long clusterInstanceId = ods.getClusterInstanceId(ODSConnectionType.DEFAULT, OrderInstanceBackup.class);
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    try {
      return clusterMgmt.getClusterInstance(clusterInstanceId).getState();
    } catch (XFMG_UnknownClusterInstanceIDException e) {
      return ClusterState.NO_CLUSTER;
    }
  }


  private void enableSharedResourceManagement() {
    sharedResourceConfigured = true;
    sharedResourceProcessing.start();
  }


  private void disableSharedResourcemanagement() {
    sharedResourceConfigured = false;
    sharedResourceProcessing.stop();
  }


  private static class OrderBackupSharedResourceChangeListener implements SharedResourceConfigurationChangeListener {

    private final OrderBackupManagement mgmt;


    public OrderBackupSharedResourceChangeListener(OrderBackupManagement mgmt) {
      this.mgmt = mgmt;
    }


    @Override
    public void configurationChanged(SharedResourceSynchronizer from, SharedResourceSynchronizer to, boolean copyContent) {
      if (from == null && to != null) {
        mgmt.enableSharedResourceManagement();
      } else {
        mgmt.disableSharedResourcemanagement();
      }
    }

  }
}
