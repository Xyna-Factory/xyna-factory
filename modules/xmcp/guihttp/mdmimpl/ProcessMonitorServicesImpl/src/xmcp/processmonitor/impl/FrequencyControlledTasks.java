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

package xmcp.processmonitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FrequencyControlledTaskStatistics;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderCreationTaskStatistics;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;

import xmcp.processmonitor.datatypes.CancelFrequencyControlledTaskException;
import xmcp.processmonitor.datatypes.FrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.GraphDatasource;
import xmcp.processmonitor.datatypes.LoadFrequencyControlledTasksException;
import xmcp.processmonitor.datatypes.NoFrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.TaskId;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;

public class FrequencyControlledTasks {
  
  private static final String TABLE_KEY_FCT_ID = "taskId.id";
  private static final String TABLE_KEY_FCT_NAME = "label";
  private static final String TABLE_KEY_FCT_TYPE = "eventCreationInfo";
  private static final String TABLE_KEY_FCT_APPLICATION = "applicationName";
  private static final String TABLE_KEY_FCT_VERSION = "versionName";
  private static final String TABLE_KEY_FCT_WORKSPACE = "workspaceName";
  private static final String TABLE_KEY_FCT_STATUS = "taskStatus";
  private static final String TABLE_KEY_FCT_START_TIME = "startTime";
  private static final String TABLE_KEY_FCT_STOP_TIME = "stopTime";
  private static final String TABLE_KEY_FCT_ALL_EVENTS = "maxEvents";
  private static final String TABLE_KEY_FCT_STARTED_EVENTS = "eventCount";
  private static final String TABLE_KEY_FCT_RUNNING_EVENTS = "runningEvents";
  private static final String TABLE_KEY_FCT_FINISHED_EVENTS = "finishedEvents";
  private static final String TABLE_KEY_FCT_FAILED_EVENTS = "failedEvents";    
  
  private FrequencyControlledTasks() {
    
  }
  
  public static void cancelFrequencyControlledTask(TaskId taskId) throws CancelFrequencyControlledTaskException {
    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().cancelFrequencyControlledTask(taskId.getId());
    } catch (XynaException e) {
      throw new CancelFrequencyControlledTaskException(e.getMessage(), e);
    }
  }
  
  public static FrequencyControlledTaskDetails getFrequencyControlledTaskDetails(TaskId taskId) throws NoFrequencyControlledTaskDetails {
    try {
      FrequencyControlledTaskInformation information = XynaFactory.getPortalInstance().getProcessingPortal().getFrequencyControlledTaskInformation(taskId.getId());
      return FrequencyControlledTasks.convert(information);
    } catch (XynaException e) {
      throw new NoFrequencyControlledTaskDetails(e);
    }
  }
  
  public static List<? extends FrequencyControlledTaskDetails> getFrequencyControlledTasks(TableInfo tableInfo)
      throws LoadFrequencyControlledTasksException {
    TableHelper<FrequencyControlledTaskDetails, TableInfo> tableHelper = TableHelper.<FrequencyControlledTaskDetails, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_FCT_ID, e -> e.getTaskId() != null ? e.getTaskId().getId() : 0)
        .addSelectFunction(TABLE_KEY_FCT_NAME, FrequencyControlledTaskDetails::getLabel)
        .addSelectFunction(TABLE_KEY_FCT_TYPE, FrequencyControlledTaskDetails::getEventCreationInfo)
        .addSelectFunction(TABLE_KEY_FCT_APPLICATION, FrequencyControlledTaskDetails::getApplicationName)
        .addSelectFunction(TABLE_KEY_FCT_VERSION, FrequencyControlledTaskDetails::getVersionName)
        .addSelectFunction(TABLE_KEY_FCT_WORKSPACE, FrequencyControlledTaskDetails::getWorkspaceName)
        .addSelectFunction(TABLE_KEY_FCT_STATUS, FrequencyControlledTaskDetails::getTaskStatus)
        .addSelectFunction(TABLE_KEY_FCT_START_TIME, FrequencyControlledTaskDetails::getStartTime)
        .addSelectFunction(TABLE_KEY_FCT_STOP_TIME, FrequencyControlledTaskDetails::getStopTime)
        .addSelectFunction(TABLE_KEY_FCT_ALL_EVENTS, FrequencyControlledTaskDetails::getMaxEvents)
        .addSelectFunction(TABLE_KEY_FCT_STARTED_EVENTS, FrequencyControlledTaskDetails::getEventCount)
        .addSelectFunction(TABLE_KEY_FCT_RUNNING_EVENTS, FrequencyControlledTaskDetails::getRunningEvents)
        .addSelectFunction(TABLE_KEY_FCT_FINISHED_EVENTS, FrequencyControlledTaskDetails::getFinishedEvents)
        .addSelectFunction(TABLE_KEY_FCT_FAILED_EVENTS, FrequencyControlledTaskDetails::getFailedEvents)
        
        .addTableToDbMapping(TABLE_KEY_FCT_ID, FrequencyControlledTaskInformation.COL_TASKID)
        .addTableToDbMapping(TABLE_KEY_FCT_NAME, FrequencyControlledTaskInformation.COL_TASKLABEL)
        .addTableToDbMapping(TABLE_KEY_FCT_TYPE, FrequencyControlledTaskInformation.COL_EVENT_CREATION_INFORMATION)
        .addTableToDbMapping(TABLE_KEY_FCT_APPLICATION, FrequencyControlledTaskInformation.COL_APPLICATIONNAME)
        .addTableToDbMapping(TABLE_KEY_FCT_VERSION, FrequencyControlledTaskInformation.COL_VERSIONNAME)
        .addTableToDbMapping(TABLE_KEY_FCT_WORKSPACE, FrequencyControlledTaskInformation.COL_WORKSPACENAME)
        .addTableToDbMapping(TABLE_KEY_FCT_STATUS, FrequencyControlledTaskInformation.COL_TASK_STATUS)
        .addTableToDbMapping(TABLE_KEY_FCT_START_TIME, FrequencyControlledTaskInformation.COL_START_TIME)
        .addTableToDbMapping(TABLE_KEY_FCT_STOP_TIME, FrequencyControlledTaskInformation.COL_STOP_TIME)
        .addTableToDbMapping(TABLE_KEY_FCT_ALL_EVENTS, FrequencyControlledTaskInformation.COL_MAX_EVENTS)
        .addTableToDbMapping(TABLE_KEY_FCT_STARTED_EVENTS, FrequencyControlledTaskInformation.COL_EVENT_COUNT)
        .addTableToDbMapping(TABLE_KEY_FCT_FINISHED_EVENTS, FrequencyControlledTaskInformation.COL_FINISHED_EVENTS)
        .addTableToDbMapping(TABLE_KEY_FCT_FAILED_EVENTS, FrequencyControlledTaskInformation.COL_FAILED_EVENTS);
    try {
      SearchRequestBean searchRequest = tableHelper.createSearchRequest(ArchiveIdentifier.fqctrltaskinformation);
      if(tableInfo.getLimit() != null && tableInfo.getLimit() > -1)
        searchRequest.setMaxRows(tableInfo.getLimit());      
      
      FrequencyControlledTaskSelect select = (FrequencyControlledTaskSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);
      FrequencyControlledTaskSearchResult searchResult = XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer()
          .searchFrequencyControlledTasks(select, (tableInfo.getLimit() != null && tableInfo.getLimit() > -1) ? tableInfo.getLimit() : -1);

      List<FrequencyControlledTaskDetails> result = searchResult.getResult().stream()
          .map(FrequencyControlledTasks::convert)
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (XynaException e) {
      throw new LoadFrequencyControlledTasksException(e.getMessage(), e);
    }
  }
  
  public static FrequencyControlledTaskDetails convert(FrequencyControlledTaskInformation information) {
    FrequencyControlledTaskDetails result = new FrequencyControlledTaskDetails();
    result.setApplicationName(information.getApplicationname());
    result.setEventCount(information.getEventcount());
    result.setEventCreationInfo(information.getEventcreationinfo());
    result.setEventsToLaunch(information.getEventsToLaunch());
    result.setFailedEvents(information.getFailedevents());
    result.setFinishedEvents(information.getFinishedevents());
    result.setLabel(information.getLabel());
    result.setStartTime(information.getStarttime());
    result.setStopTime(information.getStoptime());
    result.setTaskId(new TaskId(information.getTaskId()));
    result.setTaskStatus(information.getTaskstatus());
    result.setVersionName(information.getVersionname());
    result.setWorkspaceName(information.getWorkspacename());
    result.setRunningEvents(information.getEventCount() - information.getFinishedEvents() - information.getFailedEvents());
    result.setMaxEvents(information.getMaxevents());
    List<GraphDatasource> graphDatasources = new ArrayList<>();
    information.getAvailableStatisticNames().forEach(name -> {
      try {
        FrequencyControlledOrderCreationTaskStatistics stat = getFrequencyControlledOrderCreationTaskStatisticsByName(name);
        if(stat != null) {
          graphDatasources.add(new GraphDatasource(stat.name(), stat.getName()));
        }
      } catch (Exception ex) {
        // nothing
      }
      try {
        FrequencyControlledTaskStatistics stat = getFrequencyControlledTaskStatisticsByName(name);
        if(stat != null) {
          graphDatasources.add(new GraphDatasource(stat.name(), stat.getName()));
        }
      } catch (Exception ex) {
        // nothing
      }
    });
    result.setDatasources(graphDatasources);
    return result;
  }
  
  private static FrequencyControlledTaskStatistics getFrequencyControlledTaskStatisticsByName(String name){
    for (FrequencyControlledTaskStatistics stat : FrequencyControlledTaskStatistics.values()) {
      if(stat.getName().equals(name)) {
        return stat;
      }
    }
    return null;
  }
  
  private static FrequencyControlledOrderCreationTaskStatistics getFrequencyControlledOrderCreationTaskStatisticsByName(String name){
    for (FrequencyControlledOrderCreationTaskStatistics stat : FrequencyControlledOrderCreationTaskStatistics.values()) {
      if(stat.getName().equals(name)) {
        return stat;
      }
    }
    return null;
  }
  
}
