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
package com.gip.xyna.xprc.xfqctrl;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalStateForTaskArchiving;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidFrequencyControlledTaskId;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_STATUS;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_TYPE;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreation;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderInputSourceUsingTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskInfoColumn;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;



public class XynaFrequencyControl extends Section {

  public static final String DEFAULT_NAME = "Xyna Frequency Control";

  static {
    addDependencies(XynaFrequencyControl.class, new ArrayList<XynaFactoryPath>(Arrays
        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaProcessing.class, XynaProcessingODS.class)})));
  }

  private FrequencyControlledOrderCreation frequencyControlledOrderCreation;

  private static PreparedQueryCache cache = new PreparedQueryCache();
  private ODS ods;

  //only protected for testcases
  protected Map<Long, FrequencyControlledTask> tasks = new ConcurrentHashMap<Long, FrequencyControlledTask>();
  protected Map<String, ScheduledFuture<?>> delayedTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();


  public XynaFrequencyControl() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  // Init Scheduled Executor for delayed Tasks
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


  @Override
  protected void init() throws XynaException {
    frequencyControlledOrderCreation = new FrequencyControlledOrderCreation();
    deployFunctionGroup(frequencyControlledOrderCreation);

    XynaProperty.FQCTRL_HIGH_RATE_THREAD_SLEEP_TYPE.registerDependency(DEFAULT_NAME);
    XynaProperty.FQCTRL_HIGH_RATE_THREAD_SLEEP_MINVALUE.registerDependency(DEFAULT_NAME);

    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(FrequencyControlledTaskInformation.class);

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();

    fExec.addTask("XynaFrequencyControl.initDelayedTaskInfo", "XynaFrequencyControl.initDelayedTaskInfo")
        .after(OrderInputSourceManagement.class).execAsync(new Runnable() {

          public void run() {
            initDelayedTaskInfo();
          }
        });

  }


  private void initDelayedTaskInfo() {

    ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);

    // Load delayed Tasks
    try {
      ods.registerStorable(DelayedFrequencyControlledTaskInformation.class);

      Collection<DelayedFrequencyControlledTaskInformation> delayedTasksInfo =
          con.loadCollection(DelayedFrequencyControlledTaskInformation.class);
      for (DelayedFrequencyControlledTaskInformation delayedTask : delayedTasksInfo) {

        con.deleteOneRow(delayedTask);
        con.commit();

        if (Long.parseLong(delayedTask.getDelay()) - System.currentTimeMillis() >= 0) {

          FrequencyControlledTask task = createNewTask(delayedTask.getCreationParams());
          tasks.put(task.getID(), task);
          task.plan(Long.parseLong(delayedTask.getDelay()));
          delayedTask.setId(String.valueOf(task.getID()));
          scheduleDelayedTask(delayedTask, scheduledExecutorService);
          con.persistObject(delayedTask);
          con.commit();

        }
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Failed to close connection", e);
      }
    }

  }


  private void scheduleAndStoreDelayedTask(DelayedFrequencyControlledTaskInformation delayedTask,
                                           ScheduledExecutorService scheduledExecutorService) {

    scheduleDelayedTask(delayedTask, scheduledExecutorService);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    
    try {
      con.persistObject(delayedTask);
      con.commit();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Error while trying to store Delayed Task!", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Failed to close connection", e);
      }
    }
  }


  private void scheduleDelayedTask(final DelayedFrequencyControlledTaskInformation delayedTask,
                                   ScheduledExecutorService scheduledExecutorService) {

    long delay = Long.parseLong(delayedTask.getDelay()) - System.currentTimeMillis();

    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(new Callable<Object>() {

      public Object call() throws Exception {

        FrequencyControlledTask task;
        ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);

        try {
          con.deleteOneRow(delayedTask);
          con.commit();
          task = tasks.get(Long.parseLong(delayedTask.getId()));
          tasks.remove(task.getID());
          delayedTasks.remove(String.valueOf(delayedTask.getId()));
          task.start();
          tasks.put(task.getID(), task);
        }

        finally {
          try {
            con.closeConnection();
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("Failed to close connection", e);
          }
        }
        
        return task.getID();
      }
      
    }, delay, TimeUnit.MILLISECONDS);

    
    delayedTasks.put(delayedTask.getId(), scheduledFuture);

  }


  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParams) throws XynaException {

    convertTimestampToUTC(creationParams);

    FrequencyControlledTask task = null;

    if (creationParams.getDelay() == null || creationParams.getDelay().equals("null")) {

      task = createNewTask(creationParams);
      task.start();
      tasks.put(task.getID(), task);
      return task.getID();

    } else {
      // Long retId = IDGenerator.getInstance().getUniqueId();
      task = createNewTask(creationParams);
      task.plan(Long.parseLong(creationParams.getDelay()));
      tasks.put(task.getID(), task);

      DelayedFrequencyControlledTaskInformation delayedTask = new DelayedFrequencyControlledTaskInformation();
      delayedTask.setId(String.valueOf(task.getID()));
      delayedTask.setDelay(creationParams.getDelay());
      creationParams.setDelay("null");
      delayedTask.setCreationParams(creationParams);
      scheduleAndStoreDelayedTask(delayedTask, scheduledExecutorService);

      return task.getID();
    }

  }


  private FrequencyControlledTask createNewTask(FrequencyControlledTaskCreationParameter creationParams) throws XynaException {

    FrequenceControlledTaskEventAlgorithm eventAlgorithm =
        FrequenceControlledTaskEventAlgorithm.createEventCreationAlgorithm(creationParams);
    FrequencyControlledTask task;
    FREQUENCY_CONTROLLED_TASK_TYPE type = creationParams.getTaskType();
    if (type == null) {
      throw new XPRC_InvalidCreationParameters("TaskType is null");
    }
    switch (type) {
      case ORDER_CREATION :
        task = frequencyControlledOrderCreation
            .generateFrequencyControlledOrderCreationTask((FrequencyControlledOrderCreationTaskCreationParameter) creationParams,
                                                          eventAlgorithm);
        break;
      default :
        throw new XPRC_InvalidCreationParameters(new StringBuilder().append("Unkown TaskType: ")
            .append(creationParams.getTaskType().toString()).toString());
    }

    //Applications auf Running setzen
    if (creationParams instanceof FrequencyControlledOrderCreationTaskCreationParameter) {
      FrequencyControlledOrderCreationTaskCreationParameter cp = (FrequencyControlledOrderCreationTaskCreationParameter) creationParams;
      setApplicationsRunning(cp);
      if (!(creationParams instanceof FrequencyControlledOrderInputSourceUsingTaskCreationParameter)) {
        // die orderinputsource wurde maximal initial verwendet um den input einmalig zu
        // erzeugen.
        for (XynaOrderCreationParameter xocp : cp.getOrderCreationParameter()) {
          xocp.setOrderInputSourceId(-1);
        }
      }
    }

    return task;
  }


  private void convertTimestampToUTC(FrequencyControlledTaskCreationParameter creationParams) {

    if (creationParams.getDelay() != null && !creationParams.getDelay().equals("null") && !creationParams.getTimezone().equals("UTC")) {

      long timestamp = Long.parseLong(creationParams.getDelay());
      TimeZone tz = TimeZone.getTimeZone(creationParams.getTimezone());
      timestamp -= tz.getOffset(timestamp);

      if (timestamp - System.currentTimeMillis() <= 0L) {
        throw new RuntimeException("Date lies in the past!");
      }

      creationParams.setDelay(String.valueOf(timestamp));
      creationParams.setTimezone("UTC");

    }
  }


  /**
   * Setzt den Status der Applications der DestinationsKeys auf Running
   */
  private void setApplicationsRunning(FrequencyControlledOrderCreationTaskCreationParameter cp) {
    DestinationKey dkey = null;
    if (cp.getOrderCreationParameter().size() > 0) {
      for (XynaOrderCreationParameter xocp : cp.getOrderCreationParameter()) {
        dkey = xocp.getDestinationKey();
        ApplicationManagementImpl applicationManagement =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        if (dkey.getApplicationName() != null) {
          applicationManagement.changeApplicationState(dkey.getApplicationName(), dkey.getVersionName(), ApplicationState.RUNNING);
        }
      }
    }
  }


  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId, String[] selectedStatistics)
      throws PersistenceLayerException, XPRC_InvalidFrequencyControlledTaskId {

    FrequencyControlledTaskInformation info = null;

    FrequencyControlledTask task = tasks.get(taskId);
    if (task != null) {
      if (selectedStatistics != null && selectedStatistics.length > 0) {
        info = task.getTaskInformation(selectedStatistics);
      } else {
        return task.getTaskInformation(false);
      }
    }

    if (info == null) {
      info = new FrequencyControlledTaskInformation(taskId);

      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.queryOneRow(info);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XPRC_InvalidFrequencyControlledTaskId(taskId);
      } finally {
        con.closeConnection();
      }

      // copy the information, just removing the unnecessary statistics information may be a problem
      // when HISTORY is configured to memory since then the underlying data is removed and will be lost.
      return info.createCopyWithReducedStatisticsInformation(selectedStatistics);

    }

    return info;

  }


  public boolean cancelFrequencyControlledTask(long taskId)
      throws XPRC_InvalidFrequencyControlledTaskId, XPRC_IllegalStateForTaskArchiving, PersistenceLayerException {

    return cancelFrequencyControlledTask(taskId, true);

  }


  public boolean cancelFrequencyControlledTask(long taskId, boolean forShutdown)
      throws XPRC_InvalidFrequencyControlledTaskId, XPRC_IllegalStateForTaskArchiving, PersistenceLayerException {

    // if exists: delete Delayed Task Entry in DataBase
    if (!forShutdown) {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      Collection<DelayedFrequencyControlledTaskInformation> delayedTasksInfo =
          con.loadCollection(DelayedFrequencyControlledTaskInformation.class);
      for (DelayedFrequencyControlledTaskInformation delayedTask : delayedTasksInfo) {
        if (delayedTask.getId().equals(String.valueOf(taskId))) {
          con.deleteOneRow(delayedTask);
          con.commit();
          // Cancel scheduledFuture
          ScheduledFuture<?> scheduledFuture = delayedTasks.get(delayedTask.getId());
          scheduledFuture.cancel(true);
          // Remove scheduledFuture from Map
          delayedTasks.remove(delayedTask.getId());
        }
      }
      con.closeConnection();
    }

    FrequencyControlledTask task = tasks.get(taskId);
    if (task == null) {
      ODSConnection historyCon = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        FrequencyControlledTaskInformation info = new FrequencyControlledTaskInformation(taskId);
        try {
          historyCon.queryOneRow(info);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new XPRC_InvalidFrequencyControlledTaskId(taskId);
        }
        throw new XPRC_IllegalStateForTaskArchiving(info.getTaskStatus());
      } finally {
        historyCon.closeConnection();
      }
    }
    try {
      task.customPostTaskProcessing();
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Failed to perform task post processing", t);
    }

    if (task.cancelTask()) {
      archiveFinishedTask(task.getID());
      return true;
    } else {
      tasks.remove(task.getID());
      return false;
    }

  }


  public FrequencyControlledTask getActiveFrequencyControlledTask(long taskId) {
    return tasks.get(taskId);
  }


  public void archiveFinishedTask(long taskId)
      throws XPRC_IllegalStateForTaskArchiving, XPRC_InvalidFrequencyControlledTaskId, PersistenceLayerException {
    FrequencyControlledTask task = tasks.get(taskId);
    if (task == null) {
      throw new XPRC_InvalidFrequencyControlledTaskId(taskId);
    }

    if (task.getStatus().equals(FREQUENCY_CONTROLLED_TASK_STATUS.Running)) {
      throw new XPRC_IllegalStateForTaskArchiving(task.getStatus().toString());
    }

    FrequencyControlledTaskInformation taskInformation = task.getTaskInformation(true);

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(taskInformation);
      con.commit();
    } finally {
      try {
        con.closeConnection();
      } finally {
        tasks.remove(task.getID());
      }
    }

  }


  public FrequencyControlledOrderCreation getFrequenceyControlledOrderCreation() {
    return frequencyControlledOrderCreation;
  }


  protected void shutdown() throws XynaException {
    for (long taskId : tasks.keySet()) {
      try {
        cancelFrequencyControlledTask(taskId);
      } catch (XynaException e) {
        logger.warn("Ignored error while canceling all tasks on shutdown", e);
      }
    }
    super.shutdown();
  }


  public FrequencyControlledTaskSearchResult searchFrequencyControlledTasks(FrequencyControlledTaskSelect select, int maxRows)
      throws PersistenceLayerException {

    try {
      return searchFrequencyControlledTasksInternally(select, maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      cache.clear();
      return searchFrequencyControlledTasksInternally(select, maxRows);
    }

  }


  private FrequencyControlledTaskSearchResult searchFrequencyControlledTasksInternally(FrequencyControlledTaskSelect select, int maxRows)
      throws PersistenceLayerException {
    String selectString;
    ResultSetReader<FrequencyControlledTaskInformation> reader;
    String selectCountString;
    Parameter paras;
    try {
      selectCountString = select.getSelectCountString();
      selectString = select.getSelectString() + " order by " + FrequencyControlledTaskInfoColumn.ID.getColumnName() + " desc";
      reader = select.getDynamicReader();
      paras = select.getParameter();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("problem with select statement", e);
    }

    int countAll = 0;
    List<FrequencyControlledTaskInformation> infos = new ArrayList<FrequencyControlledTaskInformation>();

    // the following is a bit of a hack to get the memory information in a form where it can be accessed by queries
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      con.ensurePersistenceLayerConnectivity(FrequencyControlledTaskInformation.class);
      synchronized (this) {

        if (tasks.size() > 0) {
          Collection<FrequencyControlledTaskInformation> infosToBeDeleted = new HashSet<FrequencyControlledTaskInformation>();
          for (Entry<Long, FrequencyControlledTask> runningTask : tasks.entrySet()) {
            FrequencyControlledTaskInformation info = runningTask.getValue().getTaskInformation(false);
            infosToBeDeleted.add(info);
            con.persistObject(info);
            con.commit();
          }

          PreparedQuery<FrequencyControlledTaskInformation> query = cache.getQueryFromCache(selectString, con, reader);
          infos.addAll(con.query(query, paras, maxRows));
          if (infos.size() > maxRows) {
            PreparedQuery<? extends OrderCount> queryCount = cache.getQueryFromCache(selectCountString, con, OrderCount.getCountReader());
            countAll = con.queryOneRow(queryCount, paras).getCount();
          } else {
            countAll = infos.size();
          }

          con.delete(infosToBeDeleted);
          con.commit();
        }

      }
    } finally {
      con.closeConnection();
    }

    int newMaxRows;
    if (countAll > maxRows && maxRows > -1) {
      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        PreparedQuery<? extends OrderCount> queryCount = cache.getQueryFromCache(selectCountString, con, OrderCount.getCountReader());
        countAll += con.queryOneRow(queryCount, paras).getCount();
      } finally {
        con.closeConnection();
      }
      return new FrequencyControlledTaskSearchResult(infos, countAll);
    } else {
      newMaxRows = maxRows - countAll;
    }

    boolean needToSearchHistory;
    try {
      needToSearchHistory =
          !ods.isSamePhysicalTable(FrequencyControlledTaskInformation.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      needToSearchHistory = true;
    }

    if (needToSearchHistory) {
      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        PreparedQuery<FrequencyControlledTaskInformation> query = cache.getQueryFromCache(selectString, con, reader);
        infos.addAll(con.query(query, paras, newMaxRows));
        if (infos.size() >= maxRows) {
          PreparedQuery<? extends OrderCount> queryCount = cache.getQueryFromCache(selectCountString, con, OrderCount.getCountReader());
          countAll += con.queryOneRow(queryCount, paras).getCount();
        } else {
          countAll = infos.size();
        }
      } finally {
        con.closeConnection();
      }
    }
    return new FrequencyControlledTaskSearchResult(infos, countAll, infos.size());
  }


  public Collection<FrequencyControlledTask> getActiveFrequencyControlledTasks() {
    return tasks.values();
  }


  public void pauseFrequencyControlledTasks(long taskId) {
    FrequencyControlledTask fqTask = tasks.get(taskId);
    fqTask.pause();
  }


  public void pauseFrequencyControlledTasks(List<Long> taskIds) {
    for (Long taskId : taskIds) {
      FrequencyControlledTask fqTask = tasks.get(taskId);
      fqTask.pause();
    }
  }


  public void resumeFrequencyControlledTasks(long taskId) {
    FrequencyControlledTask fqTask = tasks.get(taskId);
    fqTask.resume();
  }


  public void pauseAllFrequencyControlledTasks() {
    for (FrequencyControlledTask fct : tasks.values()) {
      fct.pause();
    }
  }


  public void resumeAllFrequencyControlledTasks() {
    for (FrequencyControlledTask fct : tasks.values()) {
      fct.resume();
    }
  }

}
