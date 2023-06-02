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
package com.gip.xyna.xprc.xsched;



import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.concurrent.FakedFuture;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.scheduler.Scheduler;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmState;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedLazyAlgorithmExecutionWrapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.CachedConnectionInformation;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.CentralComponentConnectionCacheException;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.DedicatedConnection;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderWaitingForResourceInfo;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeSchedulerFactory;
import com.gip.xyna.xprc.xsched.orderabortion.OrderAbortionManagement;
import com.gip.xyna.xprc.xsched.ordercancel.ICancelResultListener;
import com.gip.xyna.xprc.xsched.ordercancel.OrderCancellationManagement;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.Mode;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.WaitingCause;
import com.gip.xyna.xprc.xsched.scheduling.TryScheduleImpl;
import com.gip.xyna.xprc.xsched.scheduling.TryScheduleImpl.Type;
import com.gip.xyna.xprc.xsched.scheduling.UrgencyCalculators;
import com.gip.xyna.xprc.xsched.scheduling.XynaOrderExecutor;
import com.gip.xyna.xprc.xsched.scheduling.XynaSchedulerCustomisation;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagement;


/**
 * verwaltung von aufträgen die sich vor der ausführung befinden. ein interner thread bestimmt
 * die nächsten auszuführenden aufträge anhand von priorität, FIFO, capacities, serien-abhängigkeiten.
 * 
 * konfiguriert kann werden: 
 * 1. der algorithmus, wie aufträge durchsucht werden (map? list? linkedlist? iterator oder for-schleife? etc)
 * 2. der vergleichsoperator (comparator), der bestimmt, in welcher reihenfolge aufträge durchsucht werden
 * 3. der algorithmus, der bestimmt, ob ein bestimmter auftrag startbar ist, oder ob eine kapazität
 *    etc ihn daran hindert, ob er gecancelt wurde etc. (TrySchedule)
 *    
 * 1. benutzt 2. und 3.
 *    
 * ausserdem gibt es eigene threads für die verwaltung von auftrag-timeouts und cancellistener-timeouts.
 */
public class XynaScheduler extends Section {

  
  public static final String DEFAULT_NAME = "Xyna Scheduler";
  private static final String ALGORITHM_NAME = "Scheduler";
  public static final String SCHEDULER_ALGORITHM_THREAD_NAME = ALGORITHM_NAME + "Thread"; //FIXME das ist in lazyalgorithmexecutor so definiert. wie kann man das sicherer machen?
  private static Logger logger = CentralFactoryLogging.getLogger(XynaScheduler.class);
  

  public static enum SCHEDULERTHREAD_RESTART_OPTIONS {
    NONE, CHECK, REPAIR
  }


  private OrderSeriesManagement orderSeriesManagement;
  private CronLikeScheduler cronLikeScheduler;
  CapacityManagement capacityManagement; //package private für tests
  private VetoManagement vetoManagement;
  private TimeConstraintManagement timeConstraintManagement;
  private PreScheduler preScheduler;  
  private SchedulerExecutor executor;
  private TryScheduleImpl trySchedule;
  private FakedFuture<Void> initializationFuture;
  private OrderAbortionManagement orderAbortionManagement;
  private OrderCancellationManagement orderCancellationManagement;
  private AllOrdersList allOrders;
  private Scheduler<SchedulingOrder,SchedulerInformationBean> scheduler;
  private XynaSchedulerCustomisation schedulerCustomisation;
  
  public XynaScheduler() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public SchedulerInformationBean getInformationBean(SchedulerInformationBean.Mode mode) {
    if( schedulerCustomisation != null ) {
      SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(mode);
      ManagedAlgorithmInfo info = executor.getInfo();
      sib.setSchedulerAlive(info.getStatus() != AlgorithmState.NOT_RUNNING);
      sib.setThreadDeathCause(executor.getTerminatingException().isPresent() ? executor.getTerminatingException().get() : null);
      sib.setThreadDeathTimestamp(executor.getTerminatingException().isPresent() ? info.getStopTime() : 0);
      if( manuallyPaused || pauseCnt.get() != 0 ) {
        sib.setSchedulerStatus(sib.getSchedulerStatus()+" (manually: "+manuallyPaused+", cnt: "+pauseCnt.get()+")" );
      }
      return sib;
    } else {
      SchedulerInformationBean sib = new SchedulerInformationBean();
      sib.setSchedulerStatus("No scheduler Algorithm set");
      sib.setSchedulerAlive(false);
      return sib;
    }
  }

  /**
   * @return
   */
  public StringBuilder listExtendedSchedulerInfo() {
    StringBuilder sb = new StringBuilder();
    allOrders.listExtendedInfo(sb);
    schedulerCustomisation.listExtendedSchedulerInfo(sb);
    return sb;
  }

  @Override
  public void init() throws XynaException {

    allOrders = new AllOrdersList();
    
    capacityManagement = CapacityManagementFactory.createCapacityManagement();
    deployFunctionGroup(capacityManagement);

    vetoManagement = new VetoManagement();
    deployFunctionGroup(vetoManagement);
    
    timeConstraintManagement = new TimeConstraintManagement();
    deployFunctionGroup(timeConstraintManagement);
    
    preScheduler = PreSchedulerFactory.createPreScheduler();
    deployFunctionGroup(preScheduler);

    cronLikeScheduler = CronLikeSchedulerFactory.createCronLikedScheduler();
    deployFunctionGroup(cronLikeScheduler);

    orderSeriesManagement = OrderSeriesManagementFactory.createOrderSeriesManagement();
    deployFunctionGroup(orderSeriesManagement);
    
    orderAbortionManagement = new OrderAbortionManagement();
    deployFunctionGroup(orderAbortionManagement);
    
    orderCancellationManagement = new OrderCancellationManagement();
    deployFunctionGroup(orderCancellationManagement);
    
    trySchedule = new TryScheduleImpl(this, Type.Normal);
    
    schedulerCustomisation = new XynaSchedulerCustomisation(trySchedule,
                                                            null,
                                                            allOrders,
                                                            UrgencyCalculators.createPriorityUrgencyCalculator(),
                                                            capacityManagement.getCapacityCache());
    
    scheduler = new Scheduler<SchedulingOrder,SchedulerInformationBean>(schedulerCustomisation);
    allOrders.setScheduler(scheduler);
   
    //Scheduler ggf. pausieren
    if (Constants.PAUSE_SCHEDULER_AT_STARTUP) {
      pauseSchedulingManually(); //Scheduler soll über die CLI wieder fortgesetzt werden können 
    }
    
    // add dependency for used XynaProperties
    XynaProperty.XYNA_SCHEDULER_STOP_TIMEOUT_OFFSET.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.XYNA_BACKUP_ORDERS_WAITING_FOR_SCHEDULING.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_MAX_RETRIES.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_OOM_ERROR_REACTION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_GENERAL_EXCEPTION_REACTION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask( XynaScheduler.class, "XynaScheduler.startSchedulerInit" ).
      after(AllOrdersList.class, CapacityManagement.class, VetoManagement.class, 
            OrderSeriesManagement.class, TimeConstraintManagement.class).
      execAsync(new Runnable() { public void run() { startSchedulerInit(); } });
  }
    
  private void startSchedulerInit() {
                
    initializationFuture = new FakedFuture<Void>();
    
    final Runnable innerSchedulerRunnableInitialization = new Runnable() {

      public void run() {
        try {
          try {
            if (!isInitialized(DedicatedConnection.XynaScheduler)) {
              CentralComponentConnectionCache.getInstance()
                                             .openCachedConnection(ODSConnectionType.DEFAULT,
                                                                   DedicatedConnection.XynaScheduler,
                                                                   new StorableClassList(VetoInformationStorable.class,
                                                                                         CapacityStorable.class));
            }
            if (!isInitialized(DedicatedConnection.SchedulerOOMProtectionBackups)) {
              CentralComponentConnectionCache.getInstance()
                                             .openCachedConnection(ODSConnectionType.DEFAULT,
                                                                   DedicatedConnection.SchedulerOOMProtectionBackups,
                                                                   new StorableClassList(OrderInstanceBackup.class,
                                                                                         OrderInstanceDetails.class));
            }
            initializationFuture.set(null);
          } catch (XNWH_TooManyDedicatedConnections e) {
            throw new RuntimeException("Connection limit exceeded while trying to open dedicated connection for SchedulerThread.", e);
          } catch (CentralComponentConnectionCacheException e) {
            throw new RuntimeException(e);
          }
        } catch (Error er) {
          initializationFuture.injectException(er);
          throw er;
        } catch (RuntimeException re) {
          initializationFuture.injectException(re);
          throw re;
        }
      }

      private boolean isInitialized(DedicatedConnection user) {
        List<CachedConnectionInformation> cons = CentralComponentConnectionCache.getInstance().getConnectionCacheInformation();
        for (CachedConnectionInformation con : cons) {
          if (con.getIdentifier() == user.name()) {
            return true;
          }
        }
        return false;
      }
    };

    executor = new SchedulerExecutor(innerSchedulerRunnableInitialization);
    
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.registerAlgorithm(executor);
    tm.startAlgorithm(executor.getName());
    
    registerStatistics();

  }


  private void registerStatistics() {
    StatisticsPath basePath = PredefinedXynaStatisticsPath.SCHEDULER;
    
    PullStatistics<String, StringStatisticsValue> schedStateStats =
        new PullStatistics<String, StringStatisticsValue>(basePath.append("SchedulerState")) {

          @Override
          public StringStatisticsValue getValueObject() {
            String state = "unknown";
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              state = sib.getSchedulerStatus();
            }
            return new StringStatisticsValue(state);
          }

          @Override
          public String getDescription() {
            return "Current state of the scheduler.";
          }
        };
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(schedStateStats);
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
      //ntbd
    }

    PullStatistics<String, StringStatisticsValue> lastDecisionTimeStats =
        new PullStatistics<String, StringStatisticsValue>(basePath.append("LastSchedulingDecisionTime")) {

          @Override
          public StringStatisticsValue getValueObject() {
            String isoTime = "unknown";
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              isoTime = Constants.defaultUTCSimpleDateFormat().format(new Date(sib.getLastScheduled()));
            }
            return new StringStatisticsValue(isoTime);
          }

          @Override
          public String getDescription() {
            return "ISO-Timestamp of the beginning of the last scheduling activity.";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(lastDecisionTimeStats);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        //ntbd
      }

    PullStatistics<Long, LongStatisticsValue> lastDecisionDurationStats =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("LastSchedulingDuration")) {

          @Override
          public LongStatisticsValue getValueObject() {
            Long duration = 0L;
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              duration = sib.getLastSchedulingTook();
            }
            return new LongStatisticsValue(duration);
          }

          @Override
          public String getDescription() {
            return "Duration of the last scheduling in milliseconds [ms].";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(lastDecisionDurationStats);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        //ntbd
      }

    PullStatistics<Integer, IntegerStatisticsValue> waitingStats =
        new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append("WaitingOrders")) {

          @Override
          public IntegerStatisticsValue getValueObject() {
            Integer waiting = 0;
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              waiting = sib.getWaitingForCapacity() + sib.getWaitingForVeto() + sib.getWaitingForUnknown();
            }
            return new IntegerStatisticsValue(waiting);
          }

          @Override
          public String getDescription() {
            return "Number of orders waiting to be scheduled.";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(waitingStats);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        //ntbd
      }

    PullStatistics<Long, LongStatisticsValue> schedulingsLast5MinsStats =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("ScheduledLast5Min")) {

          @Override
          public LongStatisticsValue getValueObject() {
            Long count = 0L;
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              count = sib.getSchedulerRunsLast5Minutes();
            }
            return new LongStatisticsValue(count);
          }

          @Override
          public String getDescription() {
            return "Number of orders that have been scheduled within the last 5 minutes.";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(schedulingsLast5MinsStats);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        //ntbd
      }

    PullStatistics<Long, LongStatisticsValue> schedulingsLast60MinsStats = 
      new PullStatistics<Long, LongStatisticsValue>(basePath.append("ScheduledLast60Min")) {

          @Override
          public LongStatisticsValue getValueObject() {
            Long count = 0L;
            if( schedulerCustomisation != null ) {
              SchedulerInformationBean sib = schedulerCustomisation.getInformationBean(SchedulerInformationBean.Mode.Basic);
              count = sib.getSchedulerRunsLast60Minutes();
            }
            return new LongStatisticsValue(count);
          }

          @Override
          public String getDescription() {
            return "Number of orders that have been scheduled within the last 60 minutes.";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(schedulingsLast60MinsStats);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        //ntbd
      }

  }



  public void startScheduler() {
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.startAlgorithm(executor.getName());
  }


  public XynaSchedulerCustomisation getSchedulerCustomisation() {
    return schedulerCustomisation;
  }
  

  private class GatherBrokenOrders implements Transformation<SchedulingOrder, SchedulingOrder>{

    public SchedulingOrder transform(SchedulingOrder from) {
      if( from.isMarkedAsRemove() ) {
        return null; //diese Aufträge sind entfernt worden (Cancel, Suspend, etc) und daher hier uninteressant
      }
      if( from.isAlreadyScheduled() ) {
        return null; //diese Aufträge sind bereits gelaufen und daher hier uninteressant
      }
      XynaOrderServerExtension xynaOrder = allOrders.getXynaOrder(from);
      if (xynaOrder == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("incomplete xynaorder " + from.getOrderId());
        }
        return from;
      }
      boolean willViolate = willOrderViolateAllocationConstraints(xynaOrder);
      if (logger.isDebugEnabled()) {
        logger.debug("xynaOrder " + xynaOrder + " -> " + willViolate);
      }
      if (willViolate) {
        if (logger.isDebugEnabled()) {
          SchedulingData sd = xynaOrder.getSchedulingData();
          logger.debug("xynaOrder " + xynaOrder + " " + sd.isHasAcquiredCapacities() + " "
              + sd.isNeedsToAcquireCapacitiesOnNextScheduling());
        }
        return from;
      }
      return null;
    }
    
    private boolean willOrderViolateAllocationConstraints(XynaOrderServerExtension order) {
      if (order == null) {
        return true;
      } else if (order.getSchedulingData() == null) {
        return true;
      } else if (order.getSchedulingData().isHasAcquiredCapacities() &&
                 order.getSchedulingData().isNeedsToAcquireCapacitiesOnNextScheduling()) {
        return true;
      } else {
        return false;
      }
    }
  }
  
  private void repairOrders(List<SchedulingOrder> orders, OutputStream statusOutputStream) {
    for (SchedulingOrder so : orders) {
      XynaOrderServerExtension xose = allOrders.getXynaOrder(so);
      if (xose == null) {
        //XynaOrder konnte nicht gelesen werden FIXME noch behandeln
        continue;
      }
      SchedulingData sd = xose.getSchedulingData();
      if (sd == null) {
        //SchedulingData fehlt, daher Auftrag aus dem Schdeuler entfernen FIXME besser mit Fehler beenden
        removeOrderFromScheduler(xose.getId());
        continue;
      }
      if (sd.isHasAcquiredCapacities() && sd.isNeedsToAcquireCapacitiesOnNextScheduling()) {
        freeCapacitiesAndVetos(xose, true, false);
      }
    }
  }
  
  private static final String NOSCHEDULERBEAN = " did not contain any SchedulingInformations from Planning.\n";
  private static final String MORETHANONCE = " would try to aquire capacities more than once.\n";
  private static final String NULLEDORDERS = " orders were null.\n";
  
  private void reportBrokenOrders(List<SchedulingOrder> orders, OutputStream statusOutputStream) {
    int nulledOrders = 0;
    for (SchedulingOrder so : orders) {
      XynaOrderServerExtension xose = allOrders.getXynaOrder(so);
      if (xose == null) {
        nulledOrders++;
      } else if (xose.getSchedulingData() == null) {
        StringBuilder sb = new StringBuilder();
        sb.append(xose.getId()).append(": ").append(xose.getDestinationKey().getOrderType()).append(NOSCHEDULERBEAN);
        try {
          statusOutputStream.write(sb.toString().getBytes(Constants.DEFAULT_ENCODING));
        } catch (IOException e) {
          logger.debug(sb.toString());
        }
      } else {
        StringBuilder sb = new StringBuilder();
        sb.append(xose.getId()).append(": ").append(xose.getDestinationKey().getOrderType()).append(MORETHANONCE);
        try {
          statusOutputStream.write(sb.toString().getBytes(Constants.DEFAULT_ENCODING));
        } catch (IOException e) {
          logger.debug(sb.toString());
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append(nulledOrders).append(NULLEDORDERS);
    try {
      statusOutputStream.write(sb.toString().getBytes(Constants.DEFAULT_ENCODING));
    } catch (IOException e) {
      logger.debug(sb.toString());
    }
  }

  /**
   * Override the super method to remove the property change listener and shutdown the runnables
   */
  @Override
  public void shutdown() throws XynaException {
    shutdownRunnables();
    // why should we?
    //XynaFactory.getInstance().getFactoryManagement().getXynaStatistics().unregisterStatistics("XPRC.XSched.Core");
    super.shutdown();
  }


  /**
   * Package private shutdown class to stop the runnables without touching any factory stuff
   */
  void shutdownRunnables() {
    if (allOrders != null) {
      logger.debug("Stopping allOrders");
      allOrders.stop();
    }
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.stopAlgorithm(executor.getName());
  }

  public CapacityManagement getCapacityManagement() {
    return capacityManagement;
  }
  
  public VetoManagement getVetoManagement() {
    return vetoManagement;
  }
 
  public TimeConstraintManagement getTimeConstraintManagement() {
    return timeConstraintManagement;
  }

  public OrderSeriesManagement getOrderSeriesManagement() {
    return orderSeriesManagement;
  }
  
  public OrderAbortionManagement getOrderAbortionManagement() {
    return orderAbortionManagement;
  }
  
  public OrderCancellationManagement getOrderCancellationManagement() {
    return orderCancellationManagement;
  }
  
  public PreScheduler getPreScheduler() {
    return preScheduler;
  }

  public CronLikeScheduler getCronLikeScheduler() {
    return cronLikeScheduler;
  }
  
  public AllOrdersList getAllOrdersList() {
    return allOrders;
  }
  
  public void addOrder(XynaOrderServerExtension xo, ODSConnection con, boolean exceptionsAreHandledWithOrderCleanup)
      throws XPRC_OrderEntryCouldNotBeAcknowledgedException {
    if( logger.isDebugEnabled() ) {
      logger.debug( "addOrder("+xo.getId()+","
                    +(con==null?"null":"con")+","
                    +(xo.getExecutionProcessInstance()==null?"new":"suspended")
                    + ")");
    }
    xo.setHasBeenBackuppedInScheduler(false);
    if (xo.getDestinationKey().isCompensate()) {
      boolean isCanceled = allOrders.isCanceled(xo);
      if (isCanceled) {
        XynaOrderExecutor.cancelOrder(xo);
        return;
      }
      addOrderIntoAllOrdersEtc(con, xo);
      return;
    }
    // TODO validate schedulerbean
    
    //Muss Backup und Acknowledge durchgeführt werden?
    boolean hasToBeAcknowledged = xo.getOrderContext() != null && xo.getOrderContext().hasAck();
    if (logger.isDebugEnabled()) {
      logger.debug("Adding to scheduler: " + xo.getId() +" hasToBeAcknowledged="+hasToBeAcknowledged);
      xo.logDetailsOnTrace();
    }
    WarehouseRetryExecutorBuilder wreb = 
        WarehouseRetryExecutor.buildCriticalExecutor().
        connection(con).
        storable(OrderInstance.class);
    
    if (hasToBeAcknowledged) {
      wreb.storables(xo.getOrderContext().getAcknowledgableObject().backupStorables());
      //Normalfall: bei Auftragseingang liegt keine externe Connection vor: Backup mit AckConnection oder neuer
      //Connection, wenn im AcknowledgableObject keine Con enthalten ist
      wreb.connection(xo.getOrderContext().getAckConnection());
      if( con != null ) {
        logger.warn("addOrder with AcknowledgableObject and external connection!");
      }
    }
    if( xo.isInOrderSeries() && orderSeriesManagement.hasToSeparateSeries(xo) ) {
      wreb.storable(OrderInstanceBackup.class);
    }

    // to be precise we might probably remove the following requirement if the order does not have any audit information
    wreb.storable(OrderInstance.class);
    if( xo.getBatchProcessMarker() != null && ! xo.getBatchProcessMarker().isBatchProcessMaster() ) {
      wreb.storable(BatchProcessRuntimeInformationStorable.class);
    }    
    try {
      //Auftrag wird mit dem Commit in den Scheduler aufgenommen
      wreb.execute(new AddOrderInternal(xo, hasToBeAcknowledged, exceptionsAreHandledWithOrderCleanup));
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to add order "+xo+" to scheduler", e);
      throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
    }
    
  }
 
  private class AddOrderInternal implements WarehouseRetryExecutableNoResultOneException<XPRC_OrderEntryCouldNotBeAcknowledgedException> {

    private final XynaOrderServerExtension xo;
    private final boolean hasToBeAcknowledged;
    private final boolean exceptionsAreHandledWithOrderCleanup;

    public AddOrderInternal(XynaOrderServerExtension xo, boolean hasToBeAcknowledged, boolean exceptionsAreHandledWithOrderCleanup) {
      this.xo = xo;
      this.hasToBeAcknowledged = hasToBeAcknowledged;
      this.exceptionsAreHandledWithOrderCleanup = exceptionsAreHandledWithOrderCleanup;
    }

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException,
        XPRC_OrderEntryCouldNotBeAcknowledgedException {

      if( logger.isDebugEnabled() ) {
        logger.debug("AddOrderInternal "+xo+", hasToBeAcknowledged="+hasToBeAcknowledged);
      }
      
      if( xo.isInOrderSeries() && orderSeriesManagement.hasToSeparateSeries(xo) ) {
        orderSeriesManagement.separateSeries(xo,con,hasToBeAcknowledged);
      }

      //falls cleanup nach einem fehler im acknowledge (o.ä.) dazu führt, dass der auftrag weitere statusupdates in einer anderen connection schreibt, muss
      //dieses statusupdate vorher committed sein. (vgl bug 15144)
      try {
        OrderStatus orderStatus = XynaFactory.getInstance().getProcessing().getOrderStatus();
        orderStatus.changeMasterWorkflowStatus(xo, OrderInstanceStatus.SCHEDULING, exceptionsAreHandledWithOrderCleanup ? null : con);
      } catch( Throwable t ) {
        Department.handleThrowable(t);
        //TODO Exception nicht loggen, sondern als Warnung an XynaOrder anhängen
        logger.warn("Could not write orderinstance status "+OrderInstanceStatus.SCHEDULING+" for "+xo, t);
      }
      //TODO besser erst, wenn Auftrag tatsächlich geschedult wird, nicht bei wartenden?

      boolean isCanceled = allOrders.isCanceled(xo);
      if (isCanceled) {
        XynaOrderExecutor.cancelOrder(xo);
      } else {
        addOrderIntoAllOrdersEtc(con, xo);
        //Nun zum Schluss das Acknowledge, welches das Commit auf der evtl. verwendeten AckConnection ausführt.
        //Dies darf erst nach dem addOrder(...) geschehen, damit das ExecuteAfterCommit-Runnable gesetzt ist.
        if (hasToBeAcknowledged) {
          xo.getOrderContext().setAckConnection(con);
          xo.getOrderContext().acknowledgeSchedulerOrderEntry();
        }
      }
    }
    
  }
  

  private class AddOrderIntoAllOrdersEtc implements Runnable {
    private XynaOrderServerExtension xo;
    public AddOrderIntoAllOrdersEtc(XynaOrderServerExtension xo) {
      this.xo = xo;
    }
    public void run() {
      addOrderIntoAllOrdersEtc(xo);
    }
  }
  
  private void addOrderIntoAllOrdersEtc(ODSConnection con, XynaOrderServerExtension xo) {
    if (con != null) {
      //Priorität ist so angepasst: 
      // 1) nach AfterCommit-Handler aus Acknowledge-Object 
      // 2) vor dem Start der CronLikeOrder
      con.executeAfterCommit(new AddOrderIntoAllOrdersEtc(xo), Thread.MIN_PRIORITY+1); 
    } else {
      addOrderIntoAllOrdersEtc(xo);
    }
  }


  private void addOrderIntoAllOrdersEtc(XynaOrderServerExtension xo) {
    EnumSet<WaitingCause> waitingCauses = EnumSet.noneOf(WaitingCause.class);
    boolean wasNeverScheduled = xo.wasNeverScheduled();
    if( wasNeverScheduled ) {
      if( getTimeConstraintManagement().hasToWaitForStartTime(xo.getSchedulingData()) ) {
       //Auftrag muss auf Startzeit warten //NICE schöner wäre xo.getTimeConstraint() != null...
        waitingCauses.add(WaitingCause.StartTime);
      }
      if( xo.isInOrderSeries() ) {
        //Auftrag muss evtl. auf Serie warten
        waitingCauses.add(WaitingCause.Series);
      }
    }
    if (logger.isTraceEnabled()) {
      logger.trace("addOrder("+xo.getId()
                   +",wasNeverScheduled="+wasNeverScheduled
                   +",waitingCause="+waitingCauses+")");
    }
    //Eintragen in allOrders, Erzeugen der SchedulingOrder
    SchedulingOrder so = allOrders.addOrder(xo, waitingCauses);
    CentralFactoryLogging.logOrderTiming(xo.getId(), "allOrders.add (planning->scheduler)");
    
    if( wasNeverScheduled ) {
      timeConstraintManagement.addWaitingOrder( so, true );
      //Eintragen in TimeConstraintManagement und OrderSeriesManagement, ändert evtl. WaitingCause in der SchedulingOrder
      orderSeriesManagement.addWaitingOrder( so );
      //in BatchProcessManagement eintragen 
      getBatchProcessManagement().addWaitingOrder( so );
    }
    if (xo.mustDeploymentCounterBeCountDown()) {
      //das pendant zu XynaProcessCtrlExecution.startOrder
      DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      xo.setDeploymentCounterCountDownDone();
    }
    
    //Eintragen in Scheduler nach Prüfung, ob lauffähig
    allOrders.scheduleOrder(so);
  }



  private BatchProcessManagement getBatchProcessManagement() {
    return XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
  }


  /**
   * Starts a scheduling cycle if it not running already
   */
  public void notifyScheduler() {
    if (executor != null) {
      executor.requestExecution();
    }
  }


  /*
   * ##### CANCEL #####
   */

  /**
   * Tries to abort an order immediately and schedules it for later canceling if an order with the given ID arrives
   * some time later.
   * 
   * @param orderid
   * @param maxAbortInFuture was für eine haltbarkeit hat der Abort-request, falls der auftrag nicht gefunden wird? (in relativen ms)
   * @param ignoreResourcesWhenResuming
   * @return true, if the order could be canceled immediately and false otherwise
   */
  public boolean abortOrder(final Long orderid, final long maxAbortInFuture, boolean ignoreResourcesWhenResuming) {
    ICancelResultListener listener = new ICancelResultListener() {

      @Override
      public void cancelSucceeded() {
      }


      @Override
      public void cancelFailed() {
      }
    };
    //auch falls maxabort 0 ist den listener verwenden, weil sonst das ergebnis nicht unbedingt stimmen muss
    listener.setAbsoluteCancelTimeout(System.currentTimeMillis() + maxAbortInFuture);
    return cancelAbortOrder(orderid, listener, true, ignoreResourcesWhenResuming);
  }

  public XynaOrderServerExtension removeOrderFromScheduler(Long orderId) {
    return allOrders.removeOrder(orderId);
  }


  /**
   * Tries to cancel an order immediately and schedules it for later canceling if an order with the given ID arrives
   * some time later. Additionally, a listener is registered that performs a user-defined action once it is clear
   * whether canceling succeeded, failed or timed out.
   * @return true, if the order could be canceled immediately and false otherwise
   */
  public boolean cancelOrder(Long orderId, ICancelResultListener listener) {
    return cancelAbortOrder(orderId, listener, false, false);
  }

  /**
   * @return true, falls cancel/abort bereits erfolgreich
   * ACHTUNG: listener=null -&gt; der zurückgegebene boolean wert stimmt nicht unbedingt.
   * nämlich für den fall, dass der abzubrechende auftrag resuming ist und ignoreResourcesWhenResuming=false 
   */
  public boolean cancelAbortOrder(Long orderId, ICancelResultListener listener, boolean cancelCompensationsAndResumes,
                             boolean ignoreResourcesWhenResuming) {
    XynaOrderServerExtension removedOrder =
        allOrders.cancelOrder(orderId, listener, cancelCompensationsAndResumes,
                              ignoreResourcesWhenResuming);
    if (removedOrder != null) {
      //erfolgreiches cancel
      if (listener != null) {
        listener.callCancelSucceededAndSetSuccessFlag();
      }
      XynaOrderExecutor.cancelOrder(removedOrder, ignoreResourcesWhenResuming);
      return true;
    } else if (listener != null) {
      // listener bereits erfolgreich gelaufen?
      // kann durch racecondition sein oder weil der auftrag nur im scheduler auf abgebrochen gesetzt wurde
      // (resuming und ignoreResourcesWhenResuming = false)
      return listener.cancelSuccess();
    }
    return false;
  }

  /*
   * ##### MANAGEMENT INTERFACE #####
   */



  /**
   * 1. scheduler anhalten (pause-aufträge müssen noch laufen dürfen!)
   *   (implementierung durch austausch des scheduler algorithmus, der nur noch suspendaufträge durchlässt)
   * 2. aufträge die bald einen timeout haben mit fehler beantworten
   * 3. Laufende workflows dürfen zu ende laufen. Dazu müssen Subworkflows weiterhin
   *    gestartet werden. <br>
   *    
   * Diese Methode ist für die interne Verwendung gedacht.
   * Für CLI Aufrufe verwende {@link #pauseSchedulingManually()}.
   * 
   * Bessere Doku bei {@link #pauseScheduling(boolean, boolean)}
   */
  public void pauseScheduling(boolean changeTimeoutOffset) {
    pauseScheduling(changeTimeoutOffset, false, false);
  }

  /**
   * Scheduler anhalten (implementierung durch austausch des scheduler algorithmus).
   * Der pauseCnt wird hochgezählt.<br>
   * 
   * Diese Methode ist für die interne Verwendung gedacht.
   * Für CLI Aufrufe verwende {@link #pauseSchedulingManually()}.
   * 
   * @param changeTimeoutOffset wenn true: aufträge die bald einen timeout haben mit fehler beantworten
   * @param forShutdown wenn true: nur SuspendAllOrders darf noch laufen, ansonsten nur Cancel und Timeout
   */
  public void pauseScheduling(boolean changeTimeoutOffset, boolean forShutdown) {
    pauseScheduling(changeTimeoutOffset, forShutdown, false);
  }
  
  
  /**
   * Scheduler anhalten. Das manuallyPaused-Flag wird auf true gesetzt.<br>
   * Diese Methode ist für den CLI-Aufruf gedacht.
   * Für interne Aufrufe verwende {@link #pauseScheduling(boolean)}.
   */
  public void pauseSchedulingManually() {
    pauseScheduling(false, false, true);
  }
  
  private final AtomicInteger pauseCnt = new AtomicInteger(0); //für internes Pausieren (z.B. chackForActiveOrders)
  private boolean manuallyPaused = false; //für CLI
  
  
  private void pauseScheduling(boolean changeTimeoutOffset, boolean forShutdown, boolean manually) {
    synchronized (pauseCnt) {
      if (manually) {
        manuallyPaused = true;
      } else {
        pauseCnt.incrementAndGet();
      }
      if (forShutdown) {
        trySchedule.useType(TryScheduleImpl.Type.Shutdown);
      } else {
        trySchedule.useType(TryScheduleImpl.Type.Pause);
      }
      if (changeTimeoutOffset) {
        timeConstraintManagement.earlyTimeout(XynaProperty.XYNA_SCHEDULER_STOP_TIMEOUT_OFFSET.getMillis());
      }
    }
  }



  /**
   * Resumes normal scheduling by setting the original scheduling algorithm.
   * Der pauseCnt wird heruntergezählt. Diese Methode ist für die interne Verwendung
   * gedacht. Für CLI Aufrufe verwende {@link #resumeSchedulingManually()}.
   */
  public void resumeScheduling() {
    resumeScheduling(false);
  }
  
  /**
   * Resumes normal scheduling by setting the original scheduling algorithm.<br>
   * 
   * Das manuallyPaused-Flag wird auf false gesetzt. Diese Methode ist für den CLI-Aufruf
   * gedacht. Für interne Aufrufe verwende {@link #resumeScheduling()}.
   * 
   * @return true, wenn resumed werden konnte; false, wenn der Scheduler noch durch eine
   * interne Anwendung pausiert ist
   */
  public boolean resumeSchedulingManually() {
    return resumeScheduling(true);
  }
  
  
  private boolean resumeScheduling(boolean manually) {
    synchronized (pauseCnt) {
      if (manually) {
        //falls über CLI aufgerufen, das manuallyPaused-Flag umsetzen
        manuallyPaused = false;
        if (0 != pauseCnt.get()) {
          return false;
        }
      } else {
        //falls intern aufgerufen, den pauseCnt runterzählen
        int cnt = pauseCnt.decrementAndGet();
        if( cnt < 0 ) {
          logger.warn("Scheduler pauseCnt is lower than zero: "+cnt);
          pauseCnt.set(0);
        }
        if( manuallyPaused || cnt > 0 ) {
          //Scheduler bleibt weiter gestoppt
          return false; 
        }
      }
      //resume ausführen
      trySchedule.useType(TryScheduleImpl.Type.Normal);
      notifyScheduler();
      return true;
    }
  }

  /**
   * befreit caps und vetoes falls nicht bereits geschehen, und notified scheduler.<br>
   * setzt needsToAquireCaps/-Vetos-Flags in XynaOrder.
   */
  public void freeCapacitiesAndVetos(XynaOrderServerExtension xo, boolean freeCapacities, boolean freeVetos) {
    boolean freedCapacities = false;
    boolean freedVetos = false;
    SchedulingData sd = xo.getSchedulingData();
    try {
      if (freeCapacities) {
        freedCapacities = capacityManagement.freeCapacities(xo);
      }
      sd.setNeedsToAcquireCapacitiesOnNextScheduling(freeCapacities);
    } finally {
      if (freeVetos) {
        freedVetos = vetoManagement.freeVetos(xo);
      }
      sd.setNeedsToAcquireVetosOnNextScheduling(freeVetos);
      if (freedVetos) {
        if (this instanceof ClusteredScheduler) {
          ((ClusteredScheduler) this).notifyRemoteScheduler();
        }
        notifyScheduler();
      } else if (freedCapacities) {
        notifyScheduler();
      }
    }
  }

  public void checkInitialization() {
    try {
      initializationFuture.get();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while checking initialization of XynaScheduler.", e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public enum ChangeSchedulingParameterStatus {
    NotFound, 
    Unschedulable,
    Success;
  }
  
  /**
   * @param orderId
   * @param timeConstraint 
   */
  public ChangeSchedulingParameterStatus changeSchedulingParameter(Long orderId, TimeConstraint timeConstraint) {
    SchedulingData schedulingData = new SchedulingData(System.currentTimeMillis());
    schedulingData.setTimeConstraint(timeConstraint);
    return changeSchedulingParameter(orderId, schedulingData, false);
  }

  public ChangeSchedulingParameterStatus changeSchedulingParameter(Long orderId, SchedulingData schedulingData, boolean replace) {
    SchedulingOrder so = allOrders.getSchedulingOrder(orderId);
    if( so == null ) {
      return ChangeSchedulingParameterStatus.NotFound;
    }
    boolean startTimeReached = false;
    
    allOrders.lock(so);
    try {
      if( ! so.canBeScheduled() ) {
        return ChangeSchedulingParameterStatus.Unschedulable;
      }

      if( schedulingData.getTimeConstraint() != null ) {
        startTimeReached = timeConstraintManagement.rescheduleOrder(so, schedulingData.getTimeConstraint() );
        logger.info(orderId+": startTimeReached="+startTimeReached);
      }

      if( replace ) {
        so.replaceSchedulingData(schedulingData);
      }
      
      //Durch Ändern von Startzeitpunkt und Priority hat sich Urgency geändert, daher neu berechnen
      if( startTimeReached ) {
        allOrders.startTimeReached(so, true); 
      } else {
        allOrders.reschedule(so,true);
      }

    } finally {
      allOrders.unlock(so);
    }
    return ChangeSchedulingParameterStatus.Success;
  }
  
  
  public final static StringParameter<SCHEDULERTHREAD_RESTART_OPTIONS> actionParameter = StringParameter.typeEnum(SCHEDULERTHREAD_RESTART_OPTIONS.class, "action", true).build();
  
  private class SchedulerExecutor extends ManagedLazyAlgorithmExecutionWrapper<Scheduler<SchedulingOrder,SchedulerInformationBean>> {

    public SchedulerExecutor(Runnable innerSchedulerRunnableInitialization) {
      super(SCHEDULER_ALGORITHM_THREAD_NAME,
            scheduler,
            Collections.singletonList(actionParameter),
            Optional.of(innerSchedulerRunnableInitialization));
    }
    
    public void requestExecution() {
      executor.requestExecution();
    }

    
    public Scheduler<SchedulingOrder,SchedulerInformationBean> getAlgorithm() {
      return algorithm;
    }
    
    @Override
    public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
      if (parameter.containsKey(actionParameter.getName())) {
        List<SchedulingOrder> orders = null;
        SCHEDULERTHREAD_RESTART_OPTIONS restartOption = actionParameter.getFromMap(parameter);
        switch( restartOption ) {
          case NONE:
            return super.start(parameter, statusOutputStream);
          case CHECK:
            orders = allOrders.getFilteredOrders( new GatherBrokenOrders() );
            reportBrokenOrders(orders, statusOutputStream);
            if ( orders.size() == 0) {
              return super.start(parameter, statusOutputStream);
            } else {
              return false;
            }
          case REPAIR:
            orders = allOrders.getFilteredOrders( new GatherBrokenOrders() );
            reportBrokenOrders(orders, statusOutputStream);
            repairOrders(orders, statusOutputStream);
            return super.start(parameter, statusOutputStream);
          default:
            logger.warn( "SCHEDULERTHREAD_RESTART_OPTIONS "+restartOption+" not implemented");
            return false;
        }
      } else {
        return super.start(parameter, statusOutputStream);
      }
    }

  }

  public enum ResourceType { CAPACITY, VETO }

  public static class ResourceInfo {

    private String name;
    private ResourceType type;

    public ResourceInfo(String name, ResourceType type) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public ResourceType getType() {
      return type;
    }

    @Override
    public boolean equals(Object other) {
      if (super.equals(other)) {
        return true;
      }

      if (!(other instanceof ResourceInfo)) {
        return false;
      }

      ResourceInfo otherResourceInfo = (ResourceInfo)other;
      return Objects.equals(getName(), otherResourceInfo.getName()) && getType() == otherResourceInfo.getType();
    }

    @Override
    public int hashCode() {
      return Objects.hash(getName(), getType());
    }
  }

  /**
   * Determines all orders waiting for a resource (capacity or veto).
   */
  public Map<ResourceInfo, Set<XynaOrderWaitingForResourceInfo>> getOrdersWaitingForResources() {
    Map<ResourceInfo, Set<XynaOrderWaitingForResourceInfo>> waitingOrders = new HashMap<>();
    for (XynaOrderInfo xoi : getInformationBean(Mode.Orders).getOrdersInScheduler()) {
      if (xoi instanceof XynaOrderWaitingForResourceInfo) {
        XynaOrderWaitingForResourceInfo xowfri = (XynaOrderWaitingForResourceInfo)xoi;
        ResourceType resourceType = xowfri.getStatus() == OrderInstanceStatus.SCHEDULING_CAPACITY ? ResourceType.CAPACITY : ResourceType.VETO;
        ResourceInfo resourceInfo = new ResourceInfo(xowfri.getResourceName(), resourceType);

        Set<XynaOrderWaitingForResourceInfo> waitingForCurResource = waitingOrders.get(resourceInfo);
        if (waitingForCurResource == null) {
          waitingForCurResource = new HashSet<>();
          waitingOrders.put(resourceInfo, waitingForCurResource);
        }

        waitingForCurResource.add(xowfri);
      }
    }

    return waitingOrders;
  }

}
