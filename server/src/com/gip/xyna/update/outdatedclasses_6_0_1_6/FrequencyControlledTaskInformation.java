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
package com.gip.xyna.update.outdatedclasses_6_0_1_6;



import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_STATUS;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation.StatisticsInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskInfoColumn;



@Persistable(primaryKey = FrequencyControlledTaskInformation.COL_TASKID, tableName = FrequencyControlledTaskInformation.TABLE_NAME)
public class FrequencyControlledTaskInformation extends Storable<FrequencyControlledTaskInformation> {

  private static final Logger logger = CentralFactoryLogging.getLogger(FrequencyControlledTaskInformation.class);

  private static final long serialVersionUID = 5126030149816028741L;

  public static final String TABLE_NAME = "fqctrltaskinformation";

  public static final String COL_TASKID = "taskid";
  public static final String COL_TASKLABEL = "tasklabel";
  public static final String COL_EVENT_COUNT = "eventcount";
  public static final String COL_FINISHED_EVENTS = "finishedevents";
  public static final String COL_FAILED_EVENTS = "failedevents";
  public static final String COL_TASK_STATUS = "taskstatus";
  public static final String COL_MAX_EVENTS = "maxevents";
  public static final String COL_EVENT_CREATION_INFORMATION = "eventcreationinfo";
  public static final String COL_STATISTICS_INFORMATION = "statistics";
  
  public static final String COL_START_TIME = "starttime";
  public static final String COL_STOP_TIME = "stoptime";
  
  @Column(name = COL_TASKLABEL)
  private String tasklabel;
  @Column(name = COL_TASKID)
  private long taskId;
  @Column(name = COL_EVENT_COUNT)
  private long eventcount;
  @Column(name = COL_FINISHED_EVENTS)
  private long finishedevents;
  @Column(name = COL_FAILED_EVENTS)
  private long failedevents;
  @Column(name = COL_EVENT_CREATION_INFORMATION)
  private String eventcreationinformation;
  @Column(name = COL_TASK_STATUS)
  private String taskstatus;
  @Column(name = COL_MAX_EVENTS)
  private long maxEvents;
  @Column(name = COL_STATISTICS_INFORMATION, type = ColumnType.BLOBBED_JAVAOBJECT)
  private Map<String, StatisticsInformation> statistics;

  @Column(name = COL_START_TIME)
  private long starttime = -1;
  @Column(name = COL_STOP_TIME)
  private long stoptime = -1;

  private static final Map<Long, Number> emptyHashMap = Collections.unmodifiableMap(new HashMap<Long, Number>());


  public FrequencyControlledTaskInformation() {
  }
  
  FrequencyControlledTaskInformation(long taskId) {
    this.taskId = taskId;
  }
  
  FrequencyControlledTaskInformation(FrequencyControlledTask task, boolean withStatistics) {

    this(task);

    statistics = new HashMap<String, StatisticsInformation>();
    if (withStatistics) {
      for (String statisticsName : task.getStatisticsNames()) {
        StatisticsInformation info = new StatisticsInformation(task.getStatistics(statisticsName),
                                                               task.getStatisticsUnit(statisticsName));
        statistics.put(statisticsName, info);
      }
    } else {
      for (String statisticsName : task.getStatisticsNames()) {
        statistics.put(statisticsName, new StatisticsInformation(emptyHashMap, null));
      }
    }

  }


  FrequencyControlledTaskInformation(FrequencyControlledTask task, String[] includedStatistics) {

    this(task);

    statistics = new HashMap<String, StatisticsInformation>();
    if (includedStatistics != null) {
      for (String statisticsName : task.getStatisticsNames()) {
        boolean included = false;
        inner: for (String includedStatisticsName: includedStatistics) {
          if (statisticsName.equals(includedStatisticsName)) {
            StatisticsInformation info = new StatisticsInformation(task.getStatistics(statisticsName),
                                                                   task.getStatisticsUnit(statisticsName));
            statistics.put(statisticsName, info);
            included = true;
            break inner;
          }
        }
        if (!included) {
          statistics.put(statisticsName, new StatisticsInformation(emptyHashMap, null));
        }
      }
    } else {
      for (String statisticsName : task.getStatisticsNames()) {
        statistics.put(statisticsName, new StatisticsInformation(emptyHashMap, null));
      }
    }

  }


  FrequencyControlledTaskInformation(FrequencyControlledTask task) {
    this.tasklabel = task.getLabel();
    this.taskId = task.getID();
    this.eventcount = task.getEventCount();
    this.finishedevents = task.getFinishedEventCount();
    this.failedevents = task.getFailedEventCount();
    this.eventcreationinformation = task.getEventAlgorithm().getEventGenerationInformation();
    this.taskstatus = task.getStatus().toString();
    this.maxEvents = task.getEventsToLaunch();

    if (task.getTaskStartTime() != null) {
      this.starttime = task.getTaskStartTime();
    }
    if (task.getTaskStopTime() != null) {
      this.stoptime = task.getTaskStopTime();
    }
  }
  
  
  public long getTaskId() {
    return taskId;
  }
  
  
  public String getLabel() {
    return tasklabel;
  }

  
  public long getEventCount() {
    return eventcount;
  }

  
  public long getFinishedEvents() {
    return finishedevents;
  }

  
  public long getFailedEvents() {
    return failedevents;
  }


  public String getTaskStatus() {
    return taskstatus;
  }


  public long getEventsToLaunch() {
    return maxEvents;
  }


  @Override
  public String toString() {
    return new StringBuilder().append("Frequency Controlled Task '").append(tasklabel).append("' [").append(taskId)
                    .append("] - ").append(taskstatus).append("\n").append("Events: ").append(eventcount).append("/")
                    .append(maxEvents).append("\n").append("Finished: ").append(finishedevents).append(" - Failed: ")
                    .append(failedevents).append("\n").append(eventcreationinformation).append("\n").toString();
  }


  @Override
  public Object getPrimaryKey() {
    return taskId;
  }


  private static class TaskInformationReader implements ResultSetReader<FrequencyControlledTaskInformation> {

    public FrequencyControlledTaskInformation read(ResultSet rs) throws SQLException {
      FrequencyControlledTaskInformation info = new FrequencyControlledTaskInformation();
      fillByResultSet(info, rs);
      return info;
    }
    
  }


  private static final TaskInformationReader reader = new TaskInformationReader();


  private static void fillByResultSet(FrequencyControlledTaskInformation info, ResultSet rs) throws SQLException {

    info.taskId = rs.getLong(COL_TASKID);
    info.tasklabel = rs.getString(COL_TASKLABEL);
    info.eventcount = rs.getLong(COL_EVENT_COUNT);
    info.finishedevents = rs.getLong(COL_FINISHED_EVENTS);
    info.failedevents = rs.getLong(COL_FAILED_EVENTS);
    info.eventcreationinformation = rs.getString(COL_EVENT_CREATION_INFORMATION);
    info.taskstatus = rs.getString(COL_TASK_STATUS);
    info.maxEvents = rs.getLong(COL_MAX_EVENTS);
    info.statistics = (Map<String, StatisticsInformation>) info
                    .readBlobbedJavaObjectFromResultSet(rs, COL_STATISTICS_INFORMATION);

    info.starttime = rs.getLong(COL_START_TIME);
    if (info.starttime == 0 && rs.wasNull()) {
      info.starttime = -1;
    }
    info.stoptime = rs.getLong(COL_STOP_TIME);
    if (info.stoptime == 0 && rs.wasNull()) {
      info.stoptime = -1;
    }

    if (logger.isTraceEnabled()) {
      logger.trace("got statistics of type " + info.statistics.getClass().getName() + " loaded by " + info.statistics.getClass().getClassLoader());
    }

  }

  
  @Override
  public ResultSetReader getReader() {
    return reader;
  }


  @Override
  public <U extends FrequencyControlledTaskInformation> void setAllFieldsFromData(U data) {
    FrequencyControlledTaskInformation cast = data;
    this.taskId = cast.taskId;
    this.tasklabel = cast.tasklabel;
    this.eventcount = cast.eventcount;
    this.finishedevents = cast.finishedevents;
    this.failedevents = cast.failedevents;
    this.eventcreationinformation = cast.eventcreationinformation;
    this.taskstatus = cast.taskstatus;
    this.maxEvents = cast.maxEvents;
    this.statistics = cast.statistics;
    this.starttime = cast.starttime;
    this.stoptime = cast.stoptime;
  }


  public Map<Long, Number> getStatistics(String name) {
    return statistics != null && statistics.get(name) != null ? statistics.get(name).getData() : null;
  }


  public String getStatisticsUnit(String name) {
    return statistics != null && statistics.get(name) != null ? statistics.get(name).getUnit() : null;
  }


  public Collection<String> getAvailableStatisticNames() {
    if (statistics != null) {
      return new ArrayList<String>(statistics.keySet());
    } else {
      return new ArrayList<String>();
    }
  }


  public void setTaskId(long taskId) {
    this.taskId = taskId;
  }


  public void setTaskLabel(String tasklabel) {
    this.tasklabel = tasklabel;
  }


  public void setStatus(String taskStatus) {
    this.taskstatus = taskStatus;

  }


  public void setMaxEvents(long maxEvents) {
    this.maxEvents = maxEvents;
  }


  public void setFinishedEvents(long maxEvents) {
    this.finishedevents = maxEvents;
  }


  public void setFailedEvents(long failedEvents) {
    this.failedevents = failedEvents;
  }


  public void setEventCreationInfo(String eventCreationInformation) {
    this.eventcreationinformation = eventCreationInformation;
  }


  public void setEventCount(long eventCount) {
    this.eventcount = eventCount;
  }


  public static class DynamicFrequencyControlledTaskInfoReader
                  implements
                    ResultSetReader<FrequencyControlledTaskInformation> {

    private List<FrequencyControlledTaskInfoColumn> selectedCols;


    public DynamicFrequencyControlledTaskInfoReader(List<FrequencyControlledTaskInfoColumn> selected) {
      selectedCols = selected;
    }


    public FrequencyControlledTaskInformation read(ResultSet rs) throws SQLException {
      FrequencyControlledTaskInformation mie = new FrequencyControlledTaskInformation();
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.ID)) {
        mie.setTaskId(rs.getLong(FrequencyControlledTaskInfoColumn.ID.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.EVENT_COUNT)) {
        mie.setEventCount(rs.getLong(FrequencyControlledTaskInfoColumn.EVENT_COUNT.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.EVENT_CREATION_INFO)) {
        mie.setEventCreationInfo(rs.getString(FrequencyControlledTaskInfoColumn.EVENT_CREATION_INFO.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.FAILED_EVENTS)) {
        mie.setFailedEvents(rs.getLong(FrequencyControlledTaskInfoColumn.FAILED_EVENTS.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.FINISHED_EVENT)) {
        mie.setFinishedEvents(rs.getLong(FrequencyControlledTaskInfoColumn.FINISHED_EVENT.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.MAX_EVENTS)) {
        mie.setMaxEvents(rs.getLong(FrequencyControlledTaskInfoColumn.MAX_EVENTS.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.STATISTICS)) {
        mie.setStatistics((Map<String, StatisticsInformation>) mie
                        .readBlobbedJavaObjectFromResultSet(rs, FrequencyControlledTaskInfoColumn.STATISTICS.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.STATUS)) {
        mie.setStatus(rs.getString(FrequencyControlledTaskInfoColumn.STATUS.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.TASK_LABEL)) {
        mie.setTaskLabel(rs.getString(FrequencyControlledTaskInfoColumn.TASK_LABEL.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.START_TIME)) {
        mie.setStarttime(rs.getLong(FrequencyControlledTaskInfoColumn.START_TIME.getColumnName()));
      }
      if (selectedCols.contains(FrequencyControlledTaskInfoColumn.STOP_TIME)) {
        mie.setStoptime(rs.getLong(FrequencyControlledTaskInfoColumn.STOP_TIME.getColumnName()));
      }
      return mie;
    }
  }


  public void setStatistics(Map<String, StatisticsInformation> statistics) {
    this.statistics = statistics;
  }


  public void setStarttime(long starttime) {
    this.starttime = starttime;
  }


  public long getStarttime() {
    return starttime;
  }


  public void setStoptime(long stoptime) {
    this.stoptime = stoptime;
  }


  public long getStoptime() {
    return stoptime;
  }


  public long getMaxevents() {
    return this.maxEvents;
  }


  public Map<String, StatisticsInformation> getStatistics() {
    return this.statistics;
  }


  public String getTasklabel() {
    return this.tasklabel;
  }


  public long getEventcount() {
    return this.eventcount;
  }


  public long getFinishedevents() {
    return this.finishedevents;
  }


  public long getFailedevents() {
    return this.failedevents;
  }


  public String getEventcreationinfo() {
    return this.eventcreationinformation;
  }


  public String getTaskstatus() {
    return this.taskstatus;
  }

  public FREQUENCY_CONTROLLED_TASK_STATUS getStatusAsEnum() {
    if (taskstatus == null){
      return null;
    }
    return FREQUENCY_CONTROLLED_TASK_STATUS.valueOf(taskstatus);
  }
  

//  public static class StatisticsInformation implements Serializable {
//
//    private static final long serialVersionUID = -5441544090034575843L;
//
//    private final Map<Long, Number> data;
//    private final String unit;
//
//
//    public StatisticsInformation(Map<Long, Number> data, String unit) {
//      this.data = data;
//      this.unit = unit;
//    }
//
//
//    public Map<Long, Number> getData() {
//      return data;
//    }
//
//
//    public String getUnit() {
//      return unit;
//    }
//
//  }


  public FrequencyControlledTaskInformation createCopyWithReducedStatisticsInformation(String[] selectedStatistics) {

    FrequencyControlledTaskInformation result = new FrequencyControlledTaskInformation(this.taskId);
    result.eventcount = this.eventcount;
    result.eventcreationinformation = this.eventcreationinformation;
    result.failedevents = this.failedevents;
    result.finishedevents = this.finishedevents;
    result.maxEvents = this.maxEvents;
    result.starttime = this.starttime;
    result.stoptime = this.stoptime;
    result.tasklabel = this.tasklabel;
    result.taskstatus = this.taskstatus;

    result.statistics = new HashMap<String, StatisticsInformation>();

    for (String s : getAvailableStatisticNames()) {
      boolean found = false;
      if (selectedStatistics != null) {
        inner : for (String selectedStatisticsName : selectedStatistics) {
          if (s.equals(selectedStatisticsName)) {
            found = true;
            result.getStatistics().put(s, getStatistics().get(s));
            break inner;
          }
        }
      }
      if (!found) {
        result.getStatistics().put(s, new StatisticsInformation(new HashMap<Long, Number>(), null));
      }
    }

    return result;

  }

}
