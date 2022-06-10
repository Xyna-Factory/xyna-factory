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

package com.gip.xyna.xfmg.xfmon.fruntimestats;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.TimedTasks;
import com.gip.xyna.utils.timing.TimedTasks.Executor;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler.StatisticsPersistenceStrategy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper.DiscoveryStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.AggregatedStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class RuntimeStatistics extends FunctionGroup implements FactoryRuntimeStatistics, RemoteFactoryRuntimeStatistics, Clustered, Executor<StatisticsPersistenceHandler> {
  
  private final static String DEFAULT_NAME = "Factory Runtime Statistics";
  
  public final static int FUTURE_EXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  
  private List<StatisticsPersistenceHandler> persistenceHandlers = new ArrayList<StatisticsPersistenceHandler>();
  
  private RuntimeStatisticsNode root;
  private long clusterInstanceId;
  private RMIClusterProvider clusterInstance;
  private boolean clustered = false;
  private ClusterState clusterState = ClusterState.NO_CLUSTER;
  private volatile long clusteredInterfaceId;
  private TimedTasks<StatisticsPersistenceHandler> asyncPersistenceTasks = new TimedTasks<StatisticsPersistenceHandler>("RuntimePersistenceAsyncPersistence", this);
  private RepeatedExceptionCheck repeatedExceptionCheck = new RepeatedExceptionCheck();
  
  private ClusterStateChangeHandler clusterStateChangeHandler = new ClusterStateChangeHandler() {

    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }

    public void onChange(ClusterState newState) {
      clusterState = newState;
    }

  };
  
  public RuntimeStatistics() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  @Override
  protected void init() throws XynaException {
    root = RuntimeStatisticsNode.createRootNode();
   
    XynaClusteringServicesManagement.getInstance().registerClusterableComponent(this);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(RuntimeStatistics.class, "RuntimeStatistics" )
         .execAsync();

    XynaProperty.RUNTIME_STATISICS_ASYNC_PERSISTENCE_INTERVAL.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    
  }

  @Override
  protected void shutdown() throws XynaException {
    try {
      for (StatisticsPersistenceHandler handler : persistenceHandlers) {
        if (handler.getPersistenceStrategy() == StatisticsPersistenceStrategy.SHUTDOWN ||
            handler.getPersistenceStrategy() == StatisticsPersistenceStrategy.ASYNCHRONOUSLY) {
          handler.persist(StatisticsPathImpl.ALL_PATH);
        }
      }
    } finally {
      asyncPersistenceTasks.stop();
    }
  }
  
  
  public boolean isClustered() {
    return clustered;
  }

  public long getClusterInstanceId() {
    return clusterInstanceId;
  }

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
                  XFMG_ClusterComponentConfigurationException {
    if (clustered) {
      throw new RuntimeException("already clustered");
    }
    this.clusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);
    if (clusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }
    clusteredInterfaceId = clusterInstance.addRMIInterface("RemoteStatistics", this);
    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, clusterStateChangeHandler);
    clustered = true;
    clusterState = clusterInstance.getState();
  }

  public void disableClustering() {
    clustered = false;
    clusterState = ClusterState.NO_CLUSTER;
    clusterInstance = null;
    clusteredInterfaceId = 0;
    clusterInstanceId = 0;
    
  }

  public String getName() {
    return getDefaultName();
  }

  
  public void registerStatistic(Statistics stats) throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    if (!stats.getPath().isSimple()) {
      throw new RuntimeException("Illegal path");
    }
    if (stats instanceof PushStatistics) {
      for (StatisticsPersistenceHandler handler : persistenceHandlers) {
        if (handler.isResponsible(stats.getPath())) {
          ((PushStatistics) stats).injectSyncPersistenceHandler(handler);
        }
      }
    } else if (stats instanceof AggregatedStatistics) {
      if (StatisticsPathImpl.covers(((AggregatedStatistics)stats).getAggregationPath(), stats.getPath())) {
        throw new RuntimeException("Recursive statistics can not be registered\nAggregation: " + ((AggregatedStatistics)stats).getAggregationPath() + "\nStatisticsPath:"+stats.getPath());
      }
    }
    root.insertStatistic(0, stats);
  }
  
  
  public void unregisterStatistic(StatisticsPath path) throws XFMG_InvalidStatisticsPath {
    for (StatisticsPersistenceHandler handler : persistenceHandlers) {
      if (path.isSimple() && StatisticsPathImpl.covers(handler.getAssociatedStatisticsPath(), path)) { // what to do with non-simple paths
        try {
          handler.remove(path);
        } catch (PersistenceLayerException e) {
          logger.warn("Error while notifying persistenceHandler of statiscs unregistration.", e);
        }
      }
    }
    Collection<Statistics> removedStatistics = root.removeStatistic(0, path);
  }
  
  
  
  public <O extends StatisticsValue<?>> Collection<O> getAggregatedValue(StatisticsAggregator<? extends StatisticsValue<?>, O> aggregation) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic {
    return root.aggregate(aggregation);
  }
  
  
  public <O extends Serializable> StatisticsValue<O> getStatisticsValue(final StatisticsPath path, boolean clustered) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic {
    if (checkRemoteRequestConditions(clustered)) {
      try {
        List<StatisticsValue<O>> nodeResults = 
          RMIClusterProviderTools.executeAndCumulate( clusterInstance, clusteredInterfaceId,
            new RMIRunnable<StatisticsValue<O>, RemoteFactoryRuntimeStatistics, XFMG_UnknownStatistic>() {

              public StatisticsValue<O> execute(RemoteFactoryRuntimeStatistics clusteredInterface)
                              throws XFMG_UnknownStatistic, RemoteException {
                return clusteredInterface.getStatisticsValue(path);
              }
        }, this);
        if (nodeResults.size() <= 0) {
          return null;
        } else {
          Iterator<StatisticsValue<O>> resultIterator = nodeResults.iterator();
          StatisticsValue<O> value = resultIterator.next();
          while (resultIterator.hasNext()) {
            value.merge(resultIterator.next());
          }
          return value;
        }
      } catch (InvalidIDException e) {
        throw new RuntimeException("",e );
      }
    } else {
      return getStatisticsValue(path);
    }
  }
  
  
  public Statistics getStatistic(StatisticsPath path) throws XFMG_InvalidStatisticsPath {
    return root.getStatistic(0, path);
  }
  
  public <O extends Serializable> StatisticsValue<O> getStatisticsValue(StatisticsPath path) {
    Statistics<?, ?> stat = root.getStatistic(0, path);
    if (stat == null) {
      return null;
    }
    return (StatisticsValue<O>) stat.getValueObject();
  }
  

  public Map<String, Serializable> discoverStatistics(boolean clustered) {
    if (checkRemoteRequestConditions(clustered)) {
      try {
          List<Collection<DiscoveryStatisticsValue>> nodeResult = 
            RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredInterfaceId,
               new RMIRunnableNoException<Collection<DiscoveryStatisticsValue>, RemoteFactoryRuntimeStatistics>() {
    
            public Collection<DiscoveryStatisticsValue> execute(RemoteFactoryRuntimeStatistics clusteredInterface)
                            throws RemoteException {
              return clusteredInterface.discoverStatistics();
            }
            
          }, this);
          Map<String, StatisticsValue<?>> result = new HashMap<String, StatisticsValue<?>>();
          for (Collection<DiscoveryStatisticsValue> discoveries : nodeResult) {
            mergeIntoMap(discoveries, result);
          }
          return extractStatisticsValues(result);
        } catch (InvalidIDException e) {
          throw new RuntimeException("",e);
        }
    } else {
      Collection<DiscoveryStatisticsValue> result = discoverStatistics();
      return extractStatisticsValues(mergeIntoMap(result, new HashMap<String, StatisticsValue<?>>()));
    }
  }
  
  
  public Collection<PushStatistics> registerStatisticsPersistenceHandler(StatisticsPersistenceHandler persistenceHandler) throws PersistenceLayerException, XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    if (persistenceHandler.getPersistenceStrategy() != StatisticsPersistenceStrategy.NEVER) {
      persistenceHandlers.add(persistenceHandler);
    }
    Collection<PushStatistics> stats = persistenceHandler.restoreFromPersistence();
    for (PushStatistics pushStatistics : stats) {
      PushStatistics registeredStatistic = (PushStatistics) getStatistic(pushStatistics.getPath());
      if (registeredStatistic == null) {
        registerStatistic(pushStatistics);
      } else {
        registeredStatistic.pushValue(pushStatistics.getValueObject());
      }
    }
    if (persistenceHandler.getPersistenceStrategy() == StatisticsPersistenceStrategy.ASYNCHRONOUSLY) {
      asyncPersistenceTasks.addTask(System.currentTimeMillis() + XynaProperty.RUNTIME_STATISICS_ASYNC_PERSISTENCE_INTERVAL.getMillis(), persistenceHandler);
    }
    return stats;
  }
  
  
  private static Map<String, StatisticsValue<?>> mergeIntoMap(Collection<DiscoveryStatisticsValue> discoveries, Map<String, StatisticsValue<?>> map) {
    for (DiscoveryStatisticsValue discovery : discoveries) {
      String statisticpath = discovery.getValue().getFirst();
      StatisticsValue<?> currentValue = map.get(statisticpath);
      if (currentValue == null) {
        map.put(statisticpath, discovery.getValue().getSecond());
      } else {
        currentValue.merge(discovery.getValue().getSecond());
      }
    }
    return map;
  }
  
  
  private static Map<String, Serializable> extractStatisticsValues(Map<String, StatisticsValue<?>> map) {
    Map<String, Serializable> extraction = new HashMap<String, Serializable>();
    for (Entry<String, StatisticsValue<?>> entry : map.entrySet()) {
      extraction.put(entry.getKey(), entry.getValue().getValue());
    }
    return extraction;
  }

  
  public Collection<DiscoveryStatisticsValue> discoverStatistics() {
    StatisticsAggregator<? extends StatisticsValue<?>, DiscoveryStatisticsValue> discovery = AggregationStatisticsFactory.getDiscoveryAggregator();
    try {
        return getAggregatedValue(discovery);
    } catch (XFMG_UnknownStatistic e) {
      // should not happen as no direct SINGLE-traversal is specified
      throw new RuntimeException("", e);
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    }
  }
  
  
  private boolean checkRemoteRequestConditions(boolean clustered) {
    return clustered && isClustered() && clusterState == ClusterState.CONNECTED;
  }

  
  public void execute(StatisticsPersistenceHandler work) {
    try {
      work.persist(StatisticsPathImpl.ALL_PATH);
      repeatedExceptionCheck.clear();
    } catch (PersistenceLayerException e) {
      // throwing it would just lead to logging in handleThrowable as seen below
      boolean repeated = repeatedExceptionCheck.checkRepeated(e);
      if( repeated ) {
        logger.warn( "Warehouse access failed, retrying: "+repeatedExceptionCheck );
      } else {
        logger.warn( "Warehouse access failed, retrying: "+repeatedExceptionCheck, e);
      }
      logger.warn("Error while handling asnychronous persistence", e);
    } finally {
      asyncPersistenceTasks.addTask(System.currentTimeMillis() + XynaProperty.RUNTIME_STATISICS_ASYNC_PERSISTENCE_INTERVAL.getMillis(), work);
    }
  }

  public void handleThrowable(Throwable executeFailed) {
    logger.warn("Error while handling asnychronous persistence", executeFailed);
  }
  
  
  
}
