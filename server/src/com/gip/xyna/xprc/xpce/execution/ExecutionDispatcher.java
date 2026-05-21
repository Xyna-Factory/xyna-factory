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

package com.gip.xyna.xprc.xpce.execution;



import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecution.TaskId;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.RuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler.StatisticsPersistenceStrategy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler.StatisticsStorableMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler.StorableSpecificHelper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.SumAggregationPushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.dispatcher.ServiceDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;



public class ExecutionDispatcher extends XynaDispatcher {

  public static final String DEFAULT_NAME = "ExecutionDispatcher";

  // FIXME Managing those Maps should not be the responsablility of the ExecutionDispatcher
  //       There could be somekind of ServiceDestinationExecutor sharing a managing Component with the ExecutionProcessor
  //       that way killcommands could be issued to that component which will then delegate it to either a XynaProcess or a ServiceDestination
  private Map<Long, ServiceDestination> executingServiceDestinations = new HashMap<Long, ServiceDestination>();
  private ReentrantReadWriteLock serviceDestinationLock = new ReentrantReadWriteLock();

  private static final String QUERY_BY_APPLICATIONNAME_ORDERTYPE_BINDING = "SELECT * FROM " + OrderArchiveStatisticsStorable.TABLE_NAME + 
                                                                            " WHERE " + OrderArchiveStatisticsStorable.COL_APPLICATION_NAME + "=? AND " +
                                                                                        OrderArchiveStatisticsStorable.COL_ORDER_TYPE + "=? AND " +
                                                                                        ClusteredStorable.COL_BINDING + "=?";
  private static PreparedQuery<OrderArchiveStatisticsStorable> preparedOrderArchiveStatisticsQuery;

  public ExecutionDispatcher() throws XynaException {
    super(DEFAULT_NAME);
  }
  
  @Override
  public void init() throws XynaException {
    super.init();
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask( DEFAULT_NAME+".initializeStatistics", DEFAULT_NAME+".initializeStatistics").
      after(XynaDispatcher.class, RuntimeStatistics.class).
      before( XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() { initializeStatistics(); }});
  }

  public void initializeStatistics() {
        try {
          ODSImpl.getInstance().registerStorable(OrderArchiveStatisticsStorable.class);
          StatisticsStorableMapper<OrderArchiveStatisticsStorable> callStatsMapper =
            new StatisticsStorableMapper<OrderArchiveStatisticsStorable>(OrderArchiveStatisticsStorable.class) {
            @Override
            protected PushStatistics instantiateStatistics(StatisticsPath path, StatisticsValue value, OrderArchiveStatisticsStorable storable) {
              if (path.getPathPart(path.length() - 1).getPartName().equals(ORDERTYPE_STATISTICS_PATH_PART_NAME)) {
                Statistics alreadyRegistered;
                try {
                  alreadyRegistered = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(path);
                } catch (XFMG_InvalidStatisticsPath e) {
                  throw new RuntimeException("", e);
                }
                if (alreadyRegistered != null && alreadyRegistered instanceof CallStatsIdentifierStatistics) {
                  ((CallStatsIdentifierStatistics)alreadyRegistered).setStorableid(storable.getId());
                }
                return new CallStatsIdentifierStatistics(path, (StringStatisticsValue) value, storable.getId());
              } else {
                return super.instantiateStatistics(path, value, storable);
              }
            }
          };
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_ORDER_TYPE, ORDERTYPE_STATISTICS_PATH_PART_NAME, false);
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_APPLICATION_NAME, APPLICATIONNAME_STATISTICS_PATH_PART_NAME, false);
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_ERRORS, CallStatsType.FAILED, true);
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_FINISHED, CallStatsType.FINISHED, true);
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_CALLS, CallStatsType.STARTED, true);
          callStatsMapper.addMapping(OrderArchiveStatisticsStorable.COL_TIMEOUTS, CallStatsType.TIMEOUT, true);
          StorableSpecificHelper<OrderArchiveStatisticsStorable> callStatsHelper = new StorableSpecificHelper<OrderArchiveStatisticsStorable>() {
            
            public StatisticsPath generatePathToStorableValues(OrderArchiveStatisticsStorable storable) {
              if (storable.getApplicationname() == null) {
                storable.setApplicationname(XynaDispatcher.WORKING_SET_APPLICATION_NAME);
              }
              return getSpecificCallStatsPath(storable.getOrderType(), storable.getApplicationname());
            }

            public void injectPrimaryKey(OrderArchiveStatisticsStorable storable) {
              storable.setBinding(getCurrentOwnStorableBinding());
              StatisticsPath storableIdPath = getSpecificCallStatsPath(storable.getOrderType(), storable.getApplicationname()).append(ORDERTYPE_STATISTICS_PATH_PART_NAME);
              CallStatsIdentifierStatistics statistic;
              try {
                statistic = (CallStatsIdentifierStatistics)
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(storableIdPath);
              } catch (XFMG_InvalidStatisticsPath e) {
                throw new RuntimeException("", e);
              }
              if (statistic == null || statistic.getStorableid() < 0) {
                try {
                  storable.setId(IDGenerator.getInstance().getUniqueId());
                  if (statistic != null) {
                    statistic.setStorableid(storable.getId());
                  }
                } catch (XynaException e) {
                  throw new RuntimeException("",e);
                }
              } else {
                storable.setId(statistic.getStorableid());
              }
            }
          };
          StatisticsPersistenceHandler handler = 
            new StorableAggregationStatisticsPersistenceHandler<OrderArchiveStatisticsStorable>(StatisticsPersistenceStrategy.ASYNCHRONOUSLY,
                            PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, AGGREGATION_APPLICATION_NAME))
                                                                        .append(StatisticsPathImpl.ALL)
                                                                        .append(StatisticsPathImpl.ALL),
                            ODSConnectionType.HISTORY, callStatsMapper, callStatsHelper) {
            @Override
            protected Collection<OrderArchiveStatisticsStorable> loadAll(ODSConnection con)
                            throws PersistenceLayerException {
              String queryByBinding = "SELECT * FROM " + OrderArchiveStatisticsStorable.TABLE_NAME + 
                                        " WHERE " + ClusteredStorable.COL_BINDING + "=" + getCurrentOwnStorableBinding();
              try {
                PreparedQuery<OrderArchiveStatisticsStorable> query = con.prepareQuery(new Query<>(queryByBinding, OrderArchiveStatisticsStorable.reader,  
                    OrderArchiveStatisticsStorable.TABLE_NAME));
                return con.query(query, Parameter.EMPTY_PARAMETER, -1);
              } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
                // obviously not clustered
                return con.loadCollection(OrderArchiveStatisticsStorable.class);
              }
            }
          };

          Collection<PushStatistics> registeredStatistics;
          try {
            registeredStatistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatisticsPersistenceHandler(handler);
          } catch (PersistenceLayerException e) {
            throw new RuntimeException(e);
          }
          for (PushStatistics statistic : registeredStatistics) {
            
            if (statistic.getPath().getPathPart(statistic.getPath().length() - 1).getPartName()
                            .equals(ORDERTYPE_STATISTICS_PATH_PART_NAME)) {
              
              try {
                String ordertype = (String) statistic.getValueObject().getValue();
                StatisticsPath ownPath = getSpecificCallStatsPath(ordertype, AGGREGATION_APPLICATION_NAME)
                                .append(ORDERTYPE_STATISTICS_PATH_PART_NAME);
                StatisticsPath pathToAggregate = getBaseCallStatsPath()
                                .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, AGGREGATION_APPLICATION_NAME))
                                .append(ordertype)
                                .append(StatisticsPathImpl.simplePathPart(ORDERTYPE_STATISTICS_PATH_PART_NAME));
                
                Statistics aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
                XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);

                for (CallStatsType type : CallStatsType.values()) {
                  ownPath = getSpecificCallStatsPath(ordertype, AGGREGATION_APPLICATION_NAME).append(type);
                  pathToAggregate = getBaseCallStatsPath().append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, AGGREGATION_APPLICATION_NAME)).append(ordertype).append(type);
                  aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);
                }
                
              } catch (XFMG_StatisticAlreadyRegistered e) {
                // aggregation could already be registered...ignore for now
              }
            }
          }
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }
        
        for( Map.Entry<DestinationKey, DestinationValue[]> entry :  allDestinations.entrySet() ) {
          setDestination( entry.getKey(), entry.getValue()[INDEX_EXECUTION], true );
        }
  }
  
  private Integer currentOwnStorableBinding = null;
  
  private Integer getCurrentOwnStorableBinding() {
    if(currentOwnStorableBinding == null) {
      OrderArchiveStatisticsStorable tmpInstance = new OrderArchiveStatisticsStorable(-1L, -1);
      currentOwnStorableBinding = tmpInstance.getLocalBinding(ODSConnectionType.HISTORY);
    }
    return currentOwnStorableBinding;
  }
  
  
  @Override
  public void dispatch(XynaOrderServerExtension xo) throws XynaException {
    DestinationValue dv = xo.getExecutionDestination();
    
    GeneralXynaObject result;
    switch (dv.getDestinationType()) {
      case XYNA_FRACTAL_WORKFLOW :
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getExecutionProcessor().process(dv, xo);
        break;
      case JAVA_DESTINATION :
        result = ((JavaDestination) dv).exec(xo, xo.getInputPayload());
        xo.setOutputPayload(result);
        break;
      case SERVICE_DESTINATION :
        serviceDestinationLock.writeLock().lock();
        try {
          executingServiceDestinations.put(xo.getId(), (ServiceDestination) dv);
        } finally {
          serviceDestinationLock.writeLock().unlock();
        }
        DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
        try {
          result = ((ServiceDestination) dv).exec(xo);
          if (logger.isDebugEnabled()) {
            logger.debug("Order left Service-Execution: " + xo + " >>> " + xo.getDestinationKey().getOrderType());
          }
        } finally {
          DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
          serviceDestinationLock.writeLock().lock();
          try {
            executingServiceDestinations.remove(xo.getId());
          } finally {
            serviceDestinationLock.writeLock().unlock();
          }
        }
        xo.setOutputPayload(result);
        break;
      default :
        throw new RuntimeException("Unexpected destination type: '" + dv.getDestinationType() + "'");
    }

  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public DestinationValue getDestination(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND {
    return super.getDestination(dk);
  }


  public boolean containsServiceExecutionsFromOrder(long orderId) {
    serviceDestinationLock.readLock().lock();
    try {
      return executingServiceDestinations.containsKey(orderId);
    } finally {
      serviceDestinationLock.readLock().unlock();
    }
  }


  public void terminateThreadOfRunningServiceExecution(long orderId, boolean threadShouldBeStoppedForcefully, AbortionCause reason) {
    ServiceDestination executingDestination;
    serviceDestinationLock.readLock().lock();
    try {
      executingDestination = executingServiceDestinations.get(orderId);
    } finally {
      serviceDestinationLock.readLock().unlock();
    }

    if (executingDestination != null) {
      executingDestination.terminateThreadOfRunningServiceExecution(orderId, threadShouldBeStoppedForcefully, reason);
    }
    // if it was interrupted the Destination should return from exec with an error and be removed from the map
    if (threadShouldBeStoppedForcefully) {
      serviceDestinationLock.writeLock().lock();
      try {
        executingDestination = executingServiceDestinations.get(orderId);
      } finally {
        serviceDestinationLock.writeLock().unlock();
      }
    }
  }


  public void terminateAllThreadsOfRunningServiceExecution(boolean threadShouldBeStoppedForcefully) {
    serviceDestinationLock.readLock().lock();
    try {
      for (Entry<Long, ServiceDestination> entry : executingServiceDestinations.entrySet()) {
        entry.getValue().terminateThreadOfRunningServiceExecution(entry.getKey(), threadShouldBeStoppedForcefully, AbortionCause.SHUTDOWN);
      }
    } finally {
      serviceDestinationLock.readLock().unlock();
    }
  }


  @Override
  protected void registerStatisticsIfNecessary(final DestinationKey dk) {

    FactoryRuntimeStatistics runtimeStats =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();

    StatisticsPath basePath = getSpecificCallStatsPath(dk.getOrderType(), dk.getApplicationName());
    Statistics existingStatistics;
    try {
      existingStatistics = runtimeStats.getStatistic(basePath.append("OrderType"));
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    }
    if (existingStatistics == null) {
      PushStatistics<String, StringStatisticsValue> ordertypeStats = new CallStatsIdentifierStatistics(basePath.append("OrderType"), new StringStatisticsValue(dk.getOrderType()), -1);
      try {
        runtimeStats.registerStatistic(ordertypeStats);
      
        PushStatistics<String, StringStatisticsValue> applicationnameStats = new PushStatistics<String, StringStatisticsValue>(basePath.append("ApplicationName"));
        applicationnameStats.pushValue(new StringStatisticsValue(dk.getApplicationName() == null ? WORKING_SET_APPLICATION_NAME : dk.getApplicationName()));
        runtimeStats.registerStatistic(applicationnameStats);
        
        for (CallStatsType type : CallStatsType.values()) {
          StatisticsPath completePath = basePath.append(type);
          runtimeStats.registerStatistic(new SumAggregationPushStatistics<Long, LongStatisticsValue>(completePath, new LongStatisticsValue(0L)));
        }
        
        //runtimeStats.registerStatistic(createRunningStatististics(basePath));
        
        
        StatisticsPath ownPath = getSpecificCallStatsPath(dk.getOrderType(), AGGREGATION_APPLICATION_NAME).append(ORDERTYPE_STATISTICS_PATH_PART_NAME);
        StatisticsPath pathToAggregate = getBaseCallStatsPath()
          .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, AGGREGATION_APPLICATION_NAME))
          .append(dk.getOrderType())
          .append(StatisticsPathImpl.simplePathPart(ORDERTYPE_STATISTICS_PATH_PART_NAME));
        Statistics aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);
        
        for (CallStatsType type : CallStatsType.values()) {
          ownPath = getSpecificCallStatsPath(dk.getOrderType(), AGGREGATION_APPLICATION_NAME).append(type);
          pathToAggregate = getBaseCallStatsPath()
            .append(new StatisticsPathImpl.BlackListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL, AGGREGATION_APPLICATION_NAME)).append(dk.getOrderType()).append(type);
          aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);
        }
      
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        // ntbd
      }
    }
    
    
    
  }
  
  
  private static class CallStatsIdentifierStatistics extends PushStatistics<String, StringStatisticsValue> {

    private long storableId = -1;
    
    public CallStatsIdentifierStatistics(StatisticsPath path, StringStatisticsValue initialValue, long storableId) {
      super(path, initialValue);
      this.storableId = storableId;
    }
    
    public CallStatsIdentifierStatistics(StatisticsPath path) {
      super(path);
    }
    
    public long getStorableid() {
      return storableId;
    }    
    
    public void setStorableid(long storableId) {
      this.storableId = storableId;
    }
    
    
  }

}
