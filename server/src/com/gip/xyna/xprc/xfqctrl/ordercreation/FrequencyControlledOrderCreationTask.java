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
package com.gip.xyna.xprc.xfqctrl.ordercreation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfint.xnumdav.AggregatableDataStore;
import com.gip.xyna.xfint.xnumdav.StorableAggregatableDataEntry;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xprc.MiscellaneousDataBean;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;
import com.gip.xyna.xprc.xfqctrl.FrequenceControlledTaskEventAlgorithm;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskEventAlgorithmParameter;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.statustracking.IStatusChangeListener;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class FrequencyControlledOrderCreationTask extends FrequencyControlledTask implements IStatusChangeListener {

  public static class FrequencyControlledOrderInputSourceUsingTaskCreationParameter
      extends
        FrequencyControlledOrderCreationTaskCreationParameter {

    private static final long serialVersionUID = 1L;
    private final long[] orderInputSourceIds;


    public FrequencyControlledOrderInputSourceUsingTaskCreationParameter(String label, long eventsToLaunch, long[] orderInputSourceIds,
                                                                         FrequencyControlledTaskEventAlgorithmParameter algorithmParameters)
        throws XynaException {
      super(label, eventsToLaunch, new ArrayList<XynaOrderCreationParameter>());
      setAlgorithmParameters(algorithmParameters);
      this.orderInputSourceIds = orderInputSourceIds;
    }


    public long[] getOrderInputSourceIds() {
      return orderInputSourceIds;
    }

  }

  public static class FrequencyControlledOrderInputSourceUsingTask extends FrequencyControlledOrderCreationTask {

    private final long[] orderInputSourceIds;
    private final Role role;
    private final OrderInputSourceManagement xoism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

    FrequencyControlledOrderInputSourceUsingTask(FrequencyControlledOrderInputSourceUsingTaskCreationParameter creationParameter,
                                                 FrequenceControlledTaskEventAlgorithm eventAlgorithm) throws XynaException {
      //die ordercreationparameter m�ssen vorher initialisiert werden, weil z.b. f�r die zugeh�rigen ordertypes statechangelistener registriert werden
      super(prepare(creationParameter), eventAlgorithm);
      this.orderInputSourceIds = creationParameter.getOrderInputSourceIds();
      this.role = creationParameter.getTransientCreationRole();
    }


    private static FrequencyControlledOrderCreationTaskCreationParameter prepare(FrequencyControlledOrderInputSourceUsingTaskCreationParameter creationParameter)
        throws XynaException {
      List<XynaOrderCreationParameter> l = new ArrayList<XynaOrderCreationParameter>();
      for (long orderInputSourceId : creationParameter.getOrderInputSourceIds()) {
        OrderInputSourceManagement xoism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        XynaOrderCreationParameter xocp = xoism.generateOrderInput(orderInputSourceId);
        xocp.setTransientCreationRole(creationParameter.getTransientCreationRole());
        l.add(xocp);
      }
      creationParameter.setOrderCreationParameter(l);
      return creationParameter;
    }


    @Override
    protected XynaOrderCreationParameter getNextOrderCreationParameter(long eventIdx) {
      int idx = (int) (eventIdx % orderInputSourceIds.length);
      long orderInputSourceId = orderInputSourceIds[idx];
      try {
        XynaOrderCreationParameter generateOrderInput = xoism.generateOrderInput(orderInputSourceId);
        generateOrderInput.setTransientCreationRole(role);
        prepareXynaOrderCreationParameter(generateOrderInput);
        return generateOrderInput;
      } catch (XynaException e) {
        throw new RuntimeException("Could not create XynaOrderCreationParameter from orderinputsource.", e);
      }
    }

  }


  private final List<XynaOrderCreationParameter> orderCreationParameters;

  private final AtomicLong currentlyWaitingOrders = new AtomicLong(0);
  private final XynaProcessCtrlExecution xpctrl = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
  private final IDGenerator idgen = XynaFactory.getInstance().getIDGenerator();

  public enum FrequencyControlledOrderCreationTaskStatistics {

    WAITING("Waiting", null, false, null),
    EXECUTION_RESPONSE_TIME("Execution response time",FrequencyControlledTask.STATISTICS_UNIT_MILLI_SECONDS, false, null);
//    EXECUTION_RATE("Execution rate", STATISTICS_UNIT_HERTZ, true, EXECUTION_RESPONSE_TIME)
    

    private final String name;
    private final String unit;
    private final boolean isDerivative;
    private final FrequencyControlledOrderCreationTaskStatistics derivativeSource;


    private FrequencyControlledOrderCreationTaskStatistics(String name,
                                                           String unit,
                                                           boolean isDerivative,
                                                           FrequencyControlledOrderCreationTaskStatistics derivativeSource) {
      this.name = name;
      this.unit = unit;
      if (isDerivative && derivativeSource == null || !isDerivative && derivativeSource != null) {
        throw new RuntimeException();
      }
      this.isDerivative = isDerivative;
      this.derivativeSource = derivativeSource;
    }


    public String getName() {
      return name;
    }


    public String getUnit() {
      return unit;
    }


    public boolean isDerivative() {
      return isDerivative;
    }


    public FrequencyControlledOrderCreationTaskStatistics getDerivativeSource() {
      return derivativeSource;
    }

  }


  private final Map<FrequencyControlledOrderCreationTaskStatistics, AggregatableDataStore> additionalStatisticsMap
        = new ConcurrentHashMap<FrequencyControlledOrderCreationTaskStatistics, AggregatableDataStore>();


  private final ConcurrentHashMap<Long, Long> mapEventIdToExecutionStartTime = new ConcurrentHashMap<Long, Long>();
  private final ConcurrentHashMap<Long, Long> mapOrderIdToEventId = new ConcurrentHashMap<Long, Long>();
  private final ConcurrentHashMap<Long, Boolean> mapOrderIdToWasWaiting = new ConcurrentHashMap<Long, Boolean>();


  FrequencyControlledOrderCreationTask(FrequencyControlledOrderCreationTaskCreationParameter creationParameter,
                                       FrequenceControlledTaskEventAlgorithm eventAlgorithm) throws XPRC_InvalidCreationParameters {
    super(creationParameter, eventAlgorithm);
    List<XynaOrderCreationParameter> creationParams = creationParameter.getOrderCreationParameter();
    if (creationParams == null || creationParams.size() == 0) {
      throw new XPRC_InvalidCreationParameters("No OrderCreationParameters specified");
    }
    for (XynaOrderCreationParameter xocp : creationParams) {
      prepareXynaOrderCreationParameter(xocp);
      xocp.setTransientCreationRole(creationParameter.getTransientCreationRole());
    }
    this.orderCreationParameters = new ArrayList<XynaOrderCreationParameter>(creationParameter
                    .getOrderCreationParameter());
    if (creationParameter.getFrequencyControlledTaskStatisticsParameters() != null) {
      for (FrequencyControlledOrderCreationTaskStatistics statsType : FrequencyControlledOrderCreationTaskStatistics
          .values()) {
        if (!statsType.isDerivative()) {
          additionalStatisticsMap.put(statsType, new AggregatableDataStore(creationParameter
              .getFrequencyControlledTaskStatisticsParameters().getMaximumDatapoints(), creationParameter
              .getFrequencyControlledTaskStatisticsParameters().getInitialDatapointDistance()));
        }
      }
    }
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
                    .addStatusChangeListener(this);
  }

  protected void prepareXynaOrderCreationParameter(XynaOrderCreationParameter xocp) throws XPRC_InvalidCreationParameters {
    MiscellaneousDataBean misc = xocp.getDataBean();
    if (misc == null) {
      misc = new MiscellaneousDataBean();
    }
    misc.setFrequencyTaskId(getID());
    xocp.setDataBean(misc);
    if (xocp instanceof RemoteXynaOrderCreationParameter && ((RemoteXynaOrderCreationParameter) xocp).getInputPayloadAsXML() != null) {
      try {
        ((RemoteXynaOrderCreationParameter) xocp).convertInputPayload();
      } catch (XynaException e) {
        throw new XPRC_InvalidCreationParameters("InputPayload invalid", e);
      }
    }
  }
 

  @Override
  public void eventTriggered(final long eventId) {
    logger.debug("received event trigger");

    final long eventidx = getEventCount(); 
    // launch it
    final CountDownLatch latch = new CountDownLatch(1);
    XynaRunnable runnable = new XynaRunnable("FrequencyControlledOrderCreation") {

      public void run() {
        long orderId = idgen.getUniqueId();
        CentralFactoryLogging.logOrderTiming(orderId, "frequencycontrolledtask start");
        mapOrderIdToEventId.put(orderId, eventId);
        try {
          FrequencyControlledOrderCreationTask.super.eventTriggered(eventId);
        } finally {
          latch.countDown(); //n�chsten event starten
        }
        try {
          XynaOrderCreationParameter xocp = getNextOrderCreationParameter(eventidx); //evtl �berschrieben
          OrderContextServerExtension ctx = xpctrl.createAndPrepareOrderAndContext(xocp, orderId);
          xpctrl.startOrder(ctx.getXynaOrder(), new OrderCreationResponseListener(FrequencyControlledOrderCreationTask.this), ctx);
        } catch (Throwable t) {
          orderFailed(orderId);
          logger.warn("Error creating order context", t);
        }
      }


    };
    boolean started = false;
    while (!started) {
      try {
        XynaExecutor.getInstance().executeRunnableWithUnprioritizedPlanningThreadpool(runnable);
        started = true;
      } catch (RejectedExecutionException e) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e1) {
          logger.warn("frequency controlled task was interrupted while waiting for threadpool.");
          return;
        }
      }
    }
    //warten, bis super.eventTriggered auch aufgerufen wurde und damit der auftrag nicht mehr im threadpool wartet
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  protected XynaOrderCreationParameter getNextOrderCreationParameter(long eventIdx) {
    final int indexOfOrderToLaunch = (int) (eventIdx % orderCreationParameters.size());
    return orderCreationParameters.get(indexOfOrderToLaunch);
  }


  void orderFailed(long orderId) {
    long eventId = mapOrderIdToEventId.remove(orderId);
    eventFailed(eventId);
  }
  
  void orderFinished(long orderId) {
    long eventId = mapOrderIdToEventId.remove(orderId);
    eventFinished(eventId);
  }

  
  protected FREQUENCY_CONTROLLED_TASK_TYPE getTaskType() {
    return FREQUENCY_CONTROLLED_TASK_TYPE.ORDER_CREATION;
  }
  

  private static class OrderCreationResponseListener extends ResponseListener {

    private static final long serialVersionUID = -8432340925246862814L;

    private transient FrequencyControlledOrderCreationTask taskToNotify;
    private final long taskId;


    OrderCreationResponseListener(final FrequencyControlledOrderCreationTask taskToNotify) {
      this.taskToNotify = taskToNotify;
      this.taskId = taskToNotify.getID();
    }


    @Override
    public void onError(XynaException[] e, OrderContext ctx) {
      if (taskToNotify != null) {
        taskToNotify.orderFailed(ctx.getOrderId());
      }
    }


    @Override
    public void onResponse(GeneralXynaObject response, OrderContext ctx) {
      if (taskToNotify != null) {
        taskToNotify.orderFinished(ctx.getOrderId());
      }
    }


    private void writeObject(java.io.ObjectOutputStream s) throws IOException {
      s.defaultWriteObject();
    }


    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
      s.defaultReadObject();

      FrequencyControlledTask task = XynaFactory.getInstance().getProcessing().getFrequencyControl()
                      .getActiveFrequencyControlledTask(taskId);
      if (task == null || !(task instanceof FrequencyControlledOrderCreationTask)) {
        logger.warn(new StringBuilder().append("Could not restore FrequencyControlledTask (id:").append(taskId)
                        .append("), most likely due to a server restart during task execution.").toString());
      } else {
        taskToNotify = (FrequencyControlledOrderCreationTask) task;
      }

    }
  }


  @Override
  protected Set<String> getAdditionalStatisticNames() {
    Set<String> statisticsNames = new HashSet<String>();
    for (FrequencyControlledOrderCreationTaskStatistics stats : FrequencyControlledOrderCreationTaskStatistics.values()) {
      statisticsNames.add(stats.getName());
    }
    return statisticsNames;
  }


  @Override
  protected Collection<StorableAggregatableDataEntry> getAdditionalStatistics(String statisticsName) {
    for (FrequencyControlledOrderCreationTaskStatistics statistics : FrequencyControlledOrderCreationTaskStatistics
        .values()) {
      if (statistics.getName().equals(statisticsName)) {
        AggregatableDataStore store = additionalStatisticsMap.get(statistics);
        if (!statistics.isDerivative()) {
          return store.getEntries();
        } else {
          store = additionalStatisticsMap.get(statistics.getDerivativeSource());
          return store.getDerivatives();
        }
      }
    }
    return null;
  }


  public ArrayList<DestinationKey> getWatchedDestinationKeys() {
    ArrayList<DestinationKey> watchedKeys = new ArrayList<DestinationKey>();
    for (XynaOrderCreationParameter parameter: orderCreationParameters) {
      watchedKeys.add(parameter.getDestinationKey());
    }
    return watchedKeys;
  }


  public void statusChanged(Long orderId, String newState, Long sourceId) {
    if (containsStatisticsInformation()) {
      Long eventId = mapOrderIdToEventId.get(orderId);
      if (eventId != null) { //orderId soll beobachtet werden
        OrderInstanceStatus newStatus;
        try {
          newStatus = OrderInstanceStatus.fromString(newState);
        } catch( IllegalArgumentException e ) {
          return; //nicht zust�ndig f�r diesen Statuswechsel, evtl. OrderInstanceCompensationStatus oder OrderInstanceSuspensionStatus
        }
        if (OrderInstanceStatus.SCHEDULING == newStatus) {
          if (mapOrderIdToWasWaiting.putIfAbsent(orderId, Boolean.TRUE) == null) {
            long currentlyWaiting = currentlyWaitingOrders.incrementAndGet();
            updateWaiting(currentlyWaiting, System.currentTimeMillis());
          }
          
        } else if (OrderInstanceStatus.RUNNING_EXECUTION == newStatus) {
          long now = System.currentTimeMillis();
          if (mapOrderIdToWasWaiting.remove(orderId) != null) {
            long currentlyWaiting = currentlyWaitingOrders.decrementAndGet();
            updateWaiting(currentlyWaiting, now);
          }
          
          // remember the start time (nur das erste mal, wo er hier vorbei kommt. sp�ter kann das nochmal passieren wegen resume)
          mapEventIdToExecutionStartTime.putIfAbsent(eventId, now);
          
        } else if (newStatus.isFinished() || newStatus.isFailed()) {
          long now = System.currentTimeMillis();
          if (mapOrderIdToWasWaiting.remove(orderId) != null) {
            //fehler nach statusupdate auf scheduling oder timeout/cancel im scheduler
            long currentlyWaiting = currentlyWaitingOrders.decrementAndGet();
            
            updateWaiting(currentlyWaiting, now);
          }
          
          Long startTime = mapEventIdToExecutionStartTime.remove(eventId);

          // add a new datapoint with the calculated execution time
          AggregatableDataStore store =
              additionalStatisticsMap.get(FrequencyControlledOrderCreationTaskStatistics.EXECUTION_RESPONSE_TIME);
          StorableAggregatableDataEntry newEntry;
          if (startTime != null) {
            newEntry = new StorableAggregatableDataEntry(now, now - startTime);
          } else {
            newEntry = new StorableAggregatableDataEntry(now, 0L);
          }
          store.addEntry(newEntry);

        }
      }
    }
  }


  private void updateWaiting(long currentlyWaiting, long now) {
    // add a new datapoint with the increased waiting count
    AggregatableDataStore store =
        additionalStatisticsMap.get(FrequencyControlledOrderCreationTaskStatistics.WAITING);
    StorableAggregatableDataEntry newEntry = new StorableAggregatableDataEntry(now, (double) currentlyWaiting);
    store.addEntry(newEntry);
  }


  protected void customPostTaskProcessing() {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
                    .removeStatusChangeListener(this);
  }


  @Override
  protected String getAdditionalStatisticsUnit(String statisticsName) {
    for (FrequencyControlledOrderCreationTaskStatistics statistics: FrequencyControlledOrderCreationTaskStatistics.values()) {
      if (statistics.getName().equals(statisticsName)) {
        return statistics.getUnit();
      }
    }
    return null;
  }
  
  public List<XynaOrderCreationParameter> getOrderCreationParameters() {
    return orderCreationParameters;
  }

}
