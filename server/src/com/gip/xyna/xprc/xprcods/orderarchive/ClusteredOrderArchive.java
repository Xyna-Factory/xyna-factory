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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.ClusteredStorableConfigChangeHandler;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupAndMigrationManagement.MigrationAbortedWithErrorException;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;



public class ClusteredOrderArchive extends OrderArchive implements Clustered, ClusterStateChangeHandler {

  static {
    addDependencies(ClusteredOrderArchive.class,
                    new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                                    new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                                        Configuration.class),
                                    new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                                        DependencyRegister.class)})));
  }


  public ClusteredOrderArchive() throws XynaException {
    super();
  }


  private void initStorables() {
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(ClusteredOrderArchive.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + ClusteredOrderArchive.class.getSimpleName() + " as clusterable component.", e);
    } 
    
      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
      try {
        ods.registerStorable(OrderInstanceDetails.class);
        ods.registerStorable(OrderInstanceBackup.class);
      } catch (PersistenceLayerException e1) {
        throw new RuntimeException("Failed to register storable", e1);
      }

      ods.addClusteredStorableConfigChangeHandler(new ClusteredStorableConfigChangeHandler() {

        public void enableClustering(long clusterInstanceId) {
          try {
            currentStorableState = XynaClusteringServicesManagement.getInstance().getClusterInstance(clusterInstanceId)
                            .getState();
          } catch (XFMG_UnknownClusterInstanceIDException e) {
            logger.error("clusterinstanceid " + clusterInstanceId + " unknown", e);
            throw new RuntimeException(e);
          }
          clusterStoreableInstanceId = clusterInstanceId;

          XynaClusteringServicesManagement.getInstance().addClusterStateChangeHandler(clusterInstanceId,
                                                                                      ClusteredOrderArchive.this);
          setOwnBinding(null);
        }


        public void disableClustering() {
          XynaClusteringServicesManagement.getInstance().removeClusterStateChangeHandler(clusterInstanceId,
                                                                                         ClusteredOrderArchive.this);
          currentStorableState = ClusterState.NO_CLUSTER;
          setOwnBinding(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
        }

      }, ODSConnectionType.DEFAULT, OrderInstanceBackup.class);

  }


  @Override
  public void init() throws XynaException {
    super.init();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(FUTURE_EXECUTION_ID_CLUSTERED_ORDER_ARCHIVE, "ClusteredOrderArchice.initStorables").
      before(OrderArchive.FUTURE_EXECUTION_ID).
      before(XynaClusteringServicesManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      after(PersistenceLayerInstances.class).
      execAsync(this::initStorables);

  }

  private static String CLUSTERABLE_COMPONENT = "ClusteredOrderArchive";

  public static int FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID = XynaFactory.getInstance().getFutureExecution().nextId();


  public static int FUTURE_EXECUTION_ID_CLUSTERED_ORDER_ARCHIVE = XynaFactory.getInstance().getFutureExecution()
                  .nextId();

  private long clusterStoreableInstanceId;
  private long clusterInstanceId;
  private long clusteredInterfaceId;
  private RMIClusterProvider clusterInstance;
  private boolean clustered = false;
  private boolean flagTryToConnectOtherNodes = false;
  ClusterState currentStorableState;


  private ClusterStateChangeHandler rmiClusterStateChangeHandler = new ClusterStateChangeHandler() {

    public boolean isReadyForChange(ClusterState newState) {
      return true; // immer bereit
    }


    public void onChange(ClusterState newState) {
      if (newState.isDisconnected()) {
        flagTryToConnectOtherNodes = false;
      } else {
        flagTryToConnectOtherNodes = true;
      }
    }

  };


  public boolean isFlagTryToConnectOtherNodes() {
    return flagTryToConnectOtherNodes;
  }


  public boolean isClustered() {
    return clustered;
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }


  private ClusteredOrderArchiveRemote clusteredSearchAlgo;


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
                  XFMG_ClusterComponentConfigurationException {
    if (clustered) {
      // FIXME SPS prio5: von einem cluster auf ein anderes umkonfigurieren?
      throw new RuntimeException("already clustered");
    }
    this.clusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt = XynaFactory.getInstance().getFactoryManagement()
                    .getXynaClusteringServicesManagement();
    clusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);
    if (clusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }
    clusteredSearchAlgo = new ClusteredOrderArchiveRemote(ods);
    clusteredInterfaceId = ((RMIClusterProvider) clusterInstance).addRMIInterface(CLUSTERABLE_COMPONENT,
                                                                                  clusteredSearchAlgo);

    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);

    clustered = true;
    flagTryToConnectOtherNodes = !clusterInstance.getState().isDisconnected();
    setOwnBinding(null);
  }


  public void disableClustering() {
    XynaClusteringServicesManagementInterface clusterMgmt = XynaFactory.getInstance().getFactoryManagement()
                    .getXynaClusteringServicesManagement();
    clusterMgmt.removeClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);
    flagTryToConnectOtherNodes = false;
    clustered = false;
    clusteredInterfaceId = 0;
    clusterInstanceId = 0;
    clusterInstance = null;
  }


  public String getName() {
    return CLUSTERABLE_COMPONENT;
  }


  //FIXME duplicate code in OrderArchive#searchOrderInstancesInternally
  @Override
  protected OrderInstanceResult searchOrderInstancesInternally(OrderInstanceSelect select, int maxRows, SearchMode searchMode)
                  throws PersistenceLayerException {
    long startTime = System.currentTimeMillis();
    boolean isSamePhysicalTable = ods.isSamePhysicalTable(OrderInstance.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    if (isSamePhysicalTable) {
      select.substituteCustomFieldLikeConditions(customField0SubstringMap, customField1SubstringMap, customField2SubstringMap, customField3SubstringMap);
    }
    Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> searchResult = searchConnectionTypeForAllNodes(select,
                                                                                                       maxRows, startTime, searchMode, ODSConnectionType.DEFAULT, new ArrayList<OrderInstance>());
    int searchHits = searchResult.getFirst().size();
    if (useOrderArchiveCountQueries.get() && searchHits >= maxRows) {
      searchHits = sendCountRequestsForAllNodes(select, ODSConnectionType.DEFAULT);
    }
    if (!isSamePhysicalTable) {
      //bugz 12766: nicht in history suchen, falls das select nur status != finished/failed sucht.
      if (select.doesQueryStatusFinishedOrFailed()) {
        select.substituteCustomFieldLikeConditions(customField0SubstringMap, customField1SubstringMap, customField2SubstringMap, customField3SubstringMap); //nur in history. vorher wird customfield-value nicht in substringmap gespeichert
        Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> historySearchResult =
            searchConnectionTypeForAllNodes(select, maxRows, startTime, searchMode, ODSConnectionType.HISTORY, searchResult.getSecond());
        mergeSearchResult(searchResult.getFirst(), historySearchResult.getFirst(), searchResult.getSecond(), searchMode);
        if (useOrderArchiveCountQueries.get() && searchResult.getFirst().size() >= maxRows) {
          searchHits += sendCountRequestsForAllNodes(select, ODSConnectionType.HISTORY);
        } else {
          searchHits = searchResult.getFirst().size();
        }
      } else {
        if (logger.isInfoEnabled()) {
          try {
            logger.info("Skipping orderarchive history query because it cannot produce any data: "
                + select.getSelectString() + " " + select.getParameter());
          } catch (XNWH_InvalidSelectStatementException e) {
            logger.warn(null, e);
          }
        }
      }
    }
    SortedMap<OrderInstance, Collection<OrderInstance>> trimmedResult = trimResult(searchResult.getFirst(), maxRows);
    List<OrderInstance> result = flattenFamilies(trimmedResult);
    if (useOrderArchiveCountQueries.get()) {
      return new OrderInstanceResult(result, searchHits, trimmedResult.size());
    } else {
      return new OrderInstanceResult(result, -1, -1);
    }
  }



  private Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> searchConnectionTypeForAllNodes(final OrderInstanceSelect select,
                                                                                                                         final int maxRows,
                                                                                                                         final long startTime,
                                                                                                                         final SearchMode searchMode,
                                                                                                                         final ODSConnectionType connectionType, 
                                                                                                                         final List<OrderInstance> selectedPreCommittedOrdersFromDEFAULT)
      throws PersistenceLayerException {
    Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> result =
        searchAlgorithm.searchConnectionType(select, maxRows, startTime, searchMode, connectionType, selectedPreCommittedOrdersFromDEFAULT);
    if (isFlagTryToConnectOtherNodes() && ods.getClusterInstance(connectionType, OrderInstance.class) == null) {

      // 2. Condition equals new OrderInstance.isClustered
      RMIRunnable<Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>>, ClusteredOrderArchiveRemoteInterface, PersistenceLayerException> rmiRunnable = new RMIRunnable<Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>>, ClusteredOrderArchiveRemoteInterface, PersistenceLayerException>() {

        public Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> execute(ClusteredOrderArchiveRemoteInterface clusteredInterface)
                        throws PersistenceLayerException, RemoteException {
          return clusteredInterface.searchConnectionTypeRemotly(select, maxRows, startTime, searchMode, connectionType, selectedPreCommittedOrdersFromDEFAULT);
        }
      };
      List<Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>>> rmiResults;
      try {
        rmiResults = RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId, rmiRunnable,
                                                                null);
      } catch (InvalidIDException e) {
        throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
      }

      for (Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> rmiResult : rmiResults) {
        result.getFirst().putAll(rmiResult.getFirst());
        //FIXME mehr als einen remote knoten für die rmizugriffe unterstützen für die rückgabe der selectedPreCommittedOrdersFromDEFAULT. besser wäre es, wenn man hier die schnittstellen zu den knoten so ändert, dass der remoteknoten bereits über die connectiontypes aggregiert.
      }

    }
    return result;
  }


  private int sendCountRequestsForAllNodes(final OrderInstanceSelect select, final ODSConnectionType connectionType)
                  throws PersistenceLayerException {
    int countAll = searchAlgorithm.sendCountQueryForConnectionType(select, connectionType);
    if (isFlagTryToConnectOtherNodes() && ods.getClusterInstance(connectionType, OrderInstance.class) == null) {

      RMIRunnable<Integer, ClusteredOrderArchiveRemoteInterface, PersistenceLayerException> rmiRunnable = new RMIRunnable<Integer, ClusteredOrderArchiveRemoteInterface, PersistenceLayerException>() {

        public Integer execute(ClusteredOrderArchiveRemoteInterface clusteredInterface)
                        throws PersistenceLayerException, RemoteException {
          return clusteredInterface.sendCountQueryForConnectionTypeRemotly(select, connectionType);
        }
      };
      List<Integer> rmiResults;
      try {
        rmiResults = RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId, rmiRunnable,
                                                                null);
      } catch (InvalidIDException e) {
        throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
      }

      for (Integer rmiResult : rmiResults) {
        countAll += rmiResult;
      }
    }
    return countAll;
  }


  @Override
  public OrderInstanceDetails getCompleteOrder(final long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    try {
      return super.getCompleteOrder(id);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (isFlagTryToConnectOtherNodes() && ods.getClusterInstance(ODSConnectionType.DEFAULT, OrderInstance.class) == null) {

        // search remotely. Use an empty dummy algorithm to avoid searching remotely again.
        // TODO generalize this behavior for the RMIClusterProviderTools: first search locally and only if
        // that returns null, search remotely
        // TODO the following code only works for two nodes. the remote calls may not throw a ...NOT_FOUND... exception
        // because that signals problems with the call itself
        List<OrderInstanceDetails> result;
        try {
          result = RMIClusterProviderTools
                          .executeAndCumulate(clusterInstance,
                                              clusteredInterfaceId,
                                              new RMIRunnable<OrderInstanceDetails, ClusteredOrderArchiveRemoteInterface, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY>() {

                                                public OrderInstanceDetails execute(ClusteredOrderArchiveRemoteInterface clusteredInterface)
                                                                throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY,
                                                                RemoteException {
                                                  try {
                                                    return clusteredInterface.getCompleteOrderRemotly(id);
                                                  } catch (PersistenceLayerException e) {
                                                    throw new RemoteException(
                                                                              "Failed to search for order <" + id + ">: " + e
                                                                                              .getMessage(), e);
                                                  }
                                                }
                                              }, null);
        } catch (InvalidIDException e1) {
          throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
        }
        if (result.size() == 1) {
          return result.get(0);
        } else if (result.size() > 1) {
          throw new RuntimeException("Unexpected number of results: " + result.size());
        } else {
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(Long.toString(id), OrderInstanceDetails.TABLE_NAME);
        }

      } else {
        throw e;
      }
    }
  }


  @Override
  protected RemoteInterface instantiateSearchAlgorithm() {
    return new ClusteredOrderArchiveRemote(ods);
  }


  public boolean isReadyForChange(ClusterState newState) {
    if(newState == currentStorableState) {
      if(logger.isDebugEnabled()) {
        logger.debug("Get isReadyForChange but the new state is the current state. Current state is <" + currentStorableState + ">");
      }
      // das ist immer ok - wobei es nicht passieren sollte.
      return true;
    }
    OrderStartupAndMigrationManagement orderBackupStartupAndMigrationHelperInstance =
        OrderStartupAndMigrationManagement.getInstance(getOwnBinding());
    try {
      if (orderBackupStartupAndMigrationHelperInstance.isMigrationRunning()) {
        // Versuch, die Migration zu stoppen ... 
        orderBackupStartupAndMigrationHelperInstance.stopMigrating();
      }
      // solange Migration läuft, sind keine ClusterState-Änderungen gewünscht
      return !orderBackupStartupAndMigrationHelperInstance.isMigrationRunning();
    } catch (MigrationAbortedWithErrorException e) {
      // wenn Migration eh abgebrochen wurde, kann auch Status geändert werden.
      orderBackupStartupAndMigrationHelperInstance.clearError(); //dann ist der andere knoten wieder erreichbar, dann kann der fehler vergessen werden.
      logger.error("Migration aborted with error:", e);
      return true;
    }
  }


  public void onChange(final ClusterState newState) {
    if (logger.isDebugEnabled()) {
      logger.debug("Got notified of state transition -> '" + newState + "', current state: " + currentStorableState);
    }

    final ClusterState finalCurrentState = currentStorableState;

    FutureExecution fe =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
            .getFutureExecutionsOnChangeHandler(clusterStoreableInstanceId);
    // das futureexecutiontask immer erstellen, auch wenn es gar nicht notwendig wäre, damit nachfolgende
    // onChangeHandler  die möglichkeit haben, davon abhängige futureexecution tasks einzustellen
    ClusterOnChangeFutureExecutionTask onchangeFET =
        new ClusterOnChangeFutureExecutionTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID, finalCurrentState, newState);
    fe.execAsync(onchangeFET);

    currentStorableState = newState;
  }


  private class ClusterOnChangeFutureExecutionTask extends FutureExecutionTask {

    private ClusterState finalCurrentState;
    private ClusterState newState;


    public ClusterOnChangeFutureExecutionTask(int id, ClusterState currentClusterstate, ClusterState newClusterstate) {
      super(id);
      finalCurrentState = currentClusterstate;
      newState = newClusterstate;
    }


    @Override
    public void execute() {
      if (ClusterState.DISCONNECTED_MASTER.equals(newState) && !finalCurrentState.equals(newState)
          && finalCurrentState != ClusterState.STARTING) {
        if(XynaFactory.getInstance().isShuttingDown()) {
          logger.debug("Server is shutting down. Ignore request for migration.");
          return;
        }
        long timeout = XynaProperty.CLUSTERING_TIMEOUT_ORDER_MIGRATION.getMillis();
        OrderStartupAndMigrationManagement.getInstance(getOwnBinding()).startMigrating(newState, timeout);
      }
    }
  }


}
