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
package com.gip.xyna.xprc.xfqctrl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfint.xnumdav.AggregatableDataStore;
import com.gip.xyna.xfint.xnumdav.StorableAggregatableDataEntry;


public abstract class FrequencyControlledTask {
  
  
  public static final Logger logger = CentralFactoryLogging.getLogger(FrequencyControlledTask.class);
  
  public static enum FREQUENCY_CONTROLLED_TASK_STATUS {
    Creation(false), Running(false), Finished(true), Canceled(true), Error(true), Paused(false), Planned(false);

    private final boolean finished;
    
    private FREQUENCY_CONTROLLED_TASK_STATUS(boolean finished) {
      this.finished = finished;
    }
    
    public boolean isFinished() {
      return finished;
    }
  }
  
  public enum FREQUENCY_CONTROLLED_TASK_TYPE {
    ORDER_CREATION;
  }


  public static final String STATISTICS_UNIT_MILLI_SECONDS = "ms";
  public static final String STATISTICS_UNIT_HERTZ = "Hz";


  public enum FrequencyControlledTaskStatistics {

    FINISHED("Finished", null, false, null),
    FAILED("Failed", null, false, null),
    RUNNING("Running", null, false, null),
    OVERALL_RESPONSE_TIME("Overall response time", STATISTICS_UNIT_MILLI_SECONDS, false, null),
    FINISHED_RATE("Finished rate", STATISTICS_UNIT_HERTZ, true, FINISHED),
    FAILED_RATE("Failed rate", STATISTICS_UNIT_HERTZ, true, FAILED);

    private final String name;
    private final String unit;
    private final boolean isDerivative;
    private final FrequencyControlledTaskStatistics derivativeSource;


    private FrequencyControlledTaskStatistics(String name, String unit, boolean isDerivative,
                                              FrequencyControlledTaskStatistics derivativeSource) {
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


    public FrequencyControlledTaskStatistics getDerivativeSource() {
      if (derivativeSource == null) {
        throw new RuntimeException("Statistics type " + this + " is not a derived type");
      }
      return derivativeSource;
    }

  }
  
  private final long ID;
  private final String label;
  protected FREQUENCY_CONTROLLED_TASK_STATUS status;
  protected final long eventsToLaunch;
  protected final AtomicLong eventCount;
  private final AtomicLong failedEventCount;
  private final AtomicLong finishedEventCount;
  private final AtomicLong currentlyRunningCount;
  private final FrequenceControlledTaskEventAlgorithm eventAlgorithm;

  private Long taskStartTime;
  private Long taskStopTime;


  private volatile boolean startedArchivingThisAfterFinish = false;


  private final boolean withStatistics;
  private Map<FrequencyControlledTaskStatistics, AggregatableDataStore> statisticsMap =
      new HashMap<FrequencyControlledTaskStatistics, AggregatableDataStore>();


  public FrequencyControlledTask(final FrequencyControlledTaskCreationParameter creationParameter,
                                 final FrequenceControlledTaskEventAlgorithm eventAlgorithm) {
    status = FREQUENCY_CONTROLLED_TASK_STATUS.Creation;
    this.eventsToLaunch = creationParameter.getEventsToLaunch();
    this.label = creationParameter.getLabel();
    this.ID = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    eventCount = new AtomicLong(0);
    failedEventCount = new AtomicLong(0);
    finishedEventCount = new AtomicLong(0);
    currentlyRunningCount = new AtomicLong(0);
    this.eventAlgorithm = eventAlgorithm;
    eventAlgorithm.registerTask(this);
    if (creationParameter.getFrequencyControlledTaskStatisticsParameters() != null) {
      withStatistics = true;
      for (FrequencyControlledTaskStatistics statsType : FrequencyControlledTaskStatistics.values()) {
        if (!statsType.isDerivative()) {
          statisticsMap.put(statsType, new AggregatableDataStore(creationParameter
              .getFrequencyControlledTaskStatisticsParameters().getMaximumDatapoints(), creationParameter
              .getFrequencyControlledTaskStatisticsParameters().getInitialDatapointDistance()));
        }
      }
    } else {
      withStatistics = false;
    }
  }


  void start() {
    taskStartTime = System.currentTimeMillis();
    status = FREQUENCY_CONTROLLED_TASK_STATUS.Running;
    if (withStatistics) {
      createFailedAndFinishedStatisticsDatapoint();
    }
    Thread t = new Thread(eventAlgorithm);
    t.start();
  }
  
  void plan(long delay) {
    taskStartTime = delay;
    status = FREQUENCY_CONTROLLED_TASK_STATUS.Planned;
  }


  //getter f�r alle
  public long getID() {
    return ID;
  }
  
  
  public String getLabel() {
    return label;
  }
  
  
  public FREQUENCY_CONTROLLED_TASK_STATUS getStatus() {
    return status;
  }

  
  public long getEventsToLaunch() {
    return eventsToLaunch;
  }

  
  public long getEventCount() {    
    return eventCount.get();
  }

  
  public long getFailedEventCount() {
    return failedEventCount.get();
  }

  
  public long getFinishedEventCount() {
    return finishedEventCount.get();
  }

  
  public FrequenceControlledTaskEventAlgorithm getEventAlgorithm() {
    return eventAlgorithm;
  }


  //package private setter for failed & finished
  protected abstract FREQUENCY_CONTROLLED_TASK_TYPE getTaskType();


  private ConcurrentHashMap<Long, Long> mapEventIdToStartTime = new ConcurrentHashMap<Long, Long>();


  protected void eventTriggered(long eventId) {

    eventCount.incrementAndGet();
    double currentlyRunning = currentlyRunningCount.incrementAndGet();

    if (withStatistics) {
      long now = System.currentTimeMillis();
      AggregatableDataStore store = statisticsMap.get(FrequencyControlledTaskStatistics.RUNNING);
      StorableAggregatableDataEntry newEntry = new StorableAggregatableDataEntry(now, currentlyRunning);
      store.addEntry(newEntry);

      mapEventIdToStartTime.put(eventId, now);
    }

  }


  private void eventFinished(long eventId, FrequencyControlledTaskStatistics s, long cnt) {
    double currentlyRunning = currentlyRunningCount.decrementAndGet();
    
    try {
      if (withStatistics) {

        long now = System.currentTimeMillis();

        AggregatableDataStore store = statisticsMap.get(s);
        StorableAggregatableDataEntry newEntry = new StorableAggregatableDataEntry(now, cnt);
        store.addEntry(newEntry);

        store = statisticsMap.get(FrequencyControlledTaskStatistics.RUNNING);
        newEntry = new StorableAggregatableDataEntry(now, currentlyRunning);
        store.addEntry(newEntry);

        Long startTime = mapEventIdToStartTime.remove(eventId);
        store = statisticsMap.get(FrequencyControlledTaskStatistics.OVERALL_RESPONSE_TIME);
        if (startTime != null) {
          newEntry = new StorableAggregatableDataEntry(now, now - startTime);
        } else {
          logger.warn("No start time found for event id " + eventId);
          newEntry = new StorableAggregatableDataEntry(now, 0L);
        }
        store.addEntry(newEntry);

      }
    } catch (RuntimeException e) {
      logger.warn("Failed to create statistics information for frequency controlled task with id <" + getID() + ">", e);
    }

    eventPostProcessing();
  }


  protected final void eventFinished(long eventId) {
    long finished = finishedEventCount.incrementAndGet();

    eventFinished(eventId, FrequencyControlledTaskStatistics.FINISHED, finished);
  }


  protected final void eventFailed(long eventId) {
    long failed = failedEventCount.incrementAndGet();

    eventFinished(eventId, FrequencyControlledTaskStatistics.FAILED, failed);
  }


  public boolean cancelTask() {
    taskStopTime = System.currentTimeMillis();
    status = FREQUENCY_CONTROLLED_TASK_STATUS.Canceled;
    eventAlgorithm.revokeExecutionPermit();
    return true;
  }
  
  
  public void pause() {
    eventAlgorithm.revokeExecutionPermit();
  }
  
  
  public void resume() {
    eventAlgorithm.grantExecutionPermit();
  }


  public FrequencyControlledTaskInformation getTaskInformation(boolean withStatistics) {
    return new FrequencyControlledTaskInformation(this, withStatistics && containsStatisticsInformation());
  }


  public FrequencyControlledTaskInformation getTaskInformation(String[] includedStatistics) {
    if (withStatistics) {
      return new FrequencyControlledTaskInformation(this, includedStatistics);
    } else {
      return new FrequencyControlledTaskInformation(this, false);
    }
  }


  public long getAmountOfCurrentlyExecutingEvents() {
    return currentlyRunningCount.get();
  }


  protected abstract Set<String> getAdditionalStatisticNames();


  public Set<String> getStatisticsNames() {
    Set<String> statisticsNames = new HashSet<String>();
    for (FrequencyControlledTaskStatistics stats : FrequencyControlledTaskStatistics.values()) {
      statisticsNames.add(stats.getName());
    }
    statisticsNames.addAll(getAdditionalStatisticNames());
    return statisticsNames;
  }


  protected abstract Collection<StorableAggregatableDataEntry> getAdditionalStatistics(String statisticsName);


  public Map<Long, Number> getStatistics(String statisticsName) {

    if (!withStatistics) {
      throw new IllegalStateException("Statistics have not been calculated");
    }

    Collection<StorableAggregatableDataEntry> entries = null;
    for (FrequencyControlledTaskStatistics parameter : FrequencyControlledTaskStatistics.values()) {
      if (parameter.getName().equals(statisticsName)) {
        AggregatableDataStore store = statisticsMap.get(parameter);
        if (!parameter.isDerivative()) {
          entries = store.getEntries();
        } else {
          store = statisticsMap.get(parameter.getDerivativeSource());
          entries = store.getDerivatives();
        }
        break;
      }
    }
    if (entries == null) {
      entries = getAdditionalStatistics(statisticsName);
      if (entries == null) {
        return null;
      }
    }
    //TODO wieso weiss man hier, dass die dataentries x-werte longs sind?
    Map<Long, Number> map = new TreeMap<Long, Number>();
    for (StorableAggregatableDataEntry entry : entries) {
      Long xvalue = (Long) entry.getValueX();
      if (xvalue != null) {
        map.put(xvalue, entry.getValue());
      }
    }

    return map;

  }


  public boolean containsStatisticsInformation() {
    return withStatistics;
  }


  protected abstract void customPostTaskProcessing();


  private void eventPostProcessing() {

    getEventAlgorithm().eventFinished();

    boolean taskDone = getFailedEventCount() + getFinishedEventCount() == getEventsToLaunch()
                    && getEventCount() == getEventsToLaunch();
    if (taskDone) {

      synchronized (this) {
        if (startedArchivingThisAfterFinish) {
          return;
        } else {
          startedArchivingThisAfterFinish = true;
        }
      }

      try {
        customPostTaskProcessing();
      } catch (RuntimeException e) {
        logger.error("Error in task post processing for frequency controlled task with id <" + getID() + ">", e);
      }

      taskStopTime = System.currentTimeMillis();
      status = FREQUENCY_CONTROLLED_TASK_STATUS.Finished;

      if (logger.isDebugEnabled()) {
        logger.debug("task <" + getID() + "> finished.");
      }

      createFailedAndFinishedStatisticsDatapoint();
      try {
        XynaFactory.getInstance().getProcessing().getFrequencyControl().archiveFinishedTask(getID());
      } catch (XynaException e) {
        logger.error("Failed to archive finished task", e);
      }
    }

  }


  protected final void createFailedAndFinishedStatisticsDatapoint() {

    if (!containsStatisticsInformation()) {
      return;
    }

    long now = System.currentTimeMillis();

    // make sure a datapoint for the current data concerning failed and finished counters is present 
    AggregatableDataStore store = statisticsMap.get(FrequencyControlledTaskStatistics.FAILED);
    StorableAggregatableDataEntry entry = new StorableAggregatableDataEntry(now, failedEventCount.get());
    store.addEntry(entry);

    store = statisticsMap.get(FrequencyControlledTaskStatistics.FINISHED);
    entry = new StorableAggregatableDataEntry(now, finishedEventCount.get());
    store.addEntry(entry);

  }


  public Long getTaskStartTime() {
    return taskStartTime;
  }


  public Long getTaskStopTime() {
    return taskStopTime;
  }


  public String getStatisticsUnit(String statisticsName) {
    if (!withStatistics) {
      throw new IllegalStateException("Statistics have not been calculated");
    }
    for (FrequencyControlledTaskStatistics parameter : FrequencyControlledTaskStatistics.values()) {
      if (parameter.getName().equals(statisticsName)) {
        return parameter.getUnit();
      }
    }
    return getAdditionalStatisticsUnit(statisticsName);
  }


  protected abstract String getAdditionalStatisticsUnit(String statisticsName);

}
