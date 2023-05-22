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

package com.gip.xyna.xact.filter.monitor;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;

import xmcp.processmonitor.datatypes.OrderOverviewEntry;
import xmcp.processmonitor.datatypes.RuntimeContext;
import xmcp.processmonitor.datatypes.TimeSpan;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;
import xprc.xpce.CustomFields;



public class OrderOverview {

  // WARNING: Read documentation for getOrderInstanceList and OpenProject-ticket 2544 before increasing these values
  public static final int MAX_ROOT_ORDER_SEARCH_ITERATIONS = 8;
  public static final int MAX_ROWS = 26500;

  protected static final String[] APPLICATIONS_TO_FILTER = { "GuiHttp", "GlobalApplicationMgmt" };

  private static final String COLUMN_PATH_ID = "id";
  private static final String COLUMN_PATH_CUSTOMER_1 = "customFields.custom1";
  private static final String COLUMN_PATH_CUSTOMER_2 = "customFields.custom2";
  private static final String COLUMN_PATH_CUSTOMER_3 = "customFields.custom3";
  private static final String COLUMN_PATH_CUSTOMER_4 = "customFields.custom4";
  private static final String COLUMN_PATH_PRIORITY = "priority";
  private static final String COLUMN_PATH_STATUS = "status";
  private static final String COLUMN_PATH_RUNTIME_START = "runTime.start";
  private static final String COLUMN_PATH_RUNTIME_STOP = "runTime.stop";
  private static final String COLUMN_PATH_RTC_WORKSPACE = "runtimeContext.workspace";
  private static final String COLUMN_PATH_RTC_APPLICATION = "runtimeContext.application";
  private static final String COLUMN_PATH_RTC_VERSION = "runtimeContext.version";
  private static final String COLUMN_PATH_TYPE_NAME = "typeName";
  private static final String COLUMN_PATH_MONITORING_LEVEL = "monitoringLevel";
  private static final String COLUMN_PATH_ROOT_ID = "rootId";
  

  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessMonitorServicesServiceOperationImpl.class);
  /*private static SubstringMap orderGuiStatusMap = new SubstringMap(false);
  private static Map<String, String> guiToServerStatusMap = new HashMap<String, String>();*/


  public static List<? extends OrderOverviewEntry> getOrderOverviewEntries(XynaMultiChannelPortal multiChannelPortal, TableInfo searchRequest, boolean onlyRootOrders) {
    if(searchRequest.getLimit() == null || searchRequest.getLimit() == -1)
      searchRequest.setLimit(MAX_ROWS);
    
    TableHelper<OrderOverviewEntry, TableInfo> tableHelper = createTableHelper(searchRequest);        
    SearchRequestBean searchRequestBean = tableHelper.createSearchRequest(ArchiveIdentifier.orderarchive);
    
    if(searchRequestBean.getFilterEntries() == null)
      searchRequestBean.setFilterEntries(new HashMap<String, String>());
    searchRequestBean.getFilterEntries().put(OrderInstanceColumn.C_INTERNAL_ORDER.getColumnName(), Boolean.FALSE.toString()); // hide internal orders
    try {
      OrderInstanceSelect select = (OrderInstanceSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequestBean);

      List<OrderInstance> orders = getOrderInstanceList(multiChannelPortal, select, searchRequest.getLimit(), onlyRootOrders);
      List<OrderOverviewEntry> foundEntries = orders.stream()
          .map(OrderOverview::convert)
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      tableHelper.sort(foundEntries);
      return tableHelper.limit(foundEntries);
    } catch (Exception e) {
      Utils.logError("Could not get order data for Process Monitor", e);
      return Collections.emptyList();
    }
  }
  
  private static TableHelper<OrderOverviewEntry, TableInfo> createTableHelper(TableInfo searchRequest){
    return TableHelper.<OrderOverviewEntry, TableInfo>init(searchRequest)
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
        .addSelectFunction(COLUMN_PATH_ID, OrderOverviewEntry::getId)
        .addSelectFunction(COLUMN_PATH_CUSTOMER_1, e -> e.getCustomFields() != null ? e.getCustomFields().getCustom1() : "")
        .addSelectFunction(COLUMN_PATH_CUSTOMER_2, e -> e.getCustomFields() != null ? e.getCustomFields().getCustom2() : "")
        .addSelectFunction(COLUMN_PATH_CUSTOMER_3, e -> e.getCustomFields() != null ? e.getCustomFields().getCustom3() : "")
        .addSelectFunction(COLUMN_PATH_CUSTOMER_4, e -> e.getCustomFields() != null ? e.getCustomFields().getCustom4() : "")
        .addSelectFunction(COLUMN_PATH_PRIORITY, OrderOverviewEntry::getPriority)
        .addSelectFunction(COLUMN_PATH_STATUS, OrderOverviewEntry::getStatus)
        .addSelectFunction(COLUMN_PATH_RUNTIME_START, e -> e.getRunTime() != null ? e.getRunTime().getStart() : null)
        .addSelectFunction(COLUMN_PATH_RUNTIME_STOP, e -> e.getRunTime() != null ? e.getRunTime().getStop() : null)
        .addSelectFunction(COLUMN_PATH_RTC_WORKSPACE, e -> e.getRuntimeContext() != null ? e.getRuntimeContext().getWorkspace() : "")
        .addSelectFunction(COLUMN_PATH_RTC_APPLICATION, e -> e.getRuntimeContext() != null ? e.getRuntimeContext().getApplication() : "")
        .addSelectFunction(COLUMN_PATH_RTC_VERSION, e -> e.getRuntimeContext() != null ? e.getRuntimeContext().getVersion() : "")
        .addSelectFunction(COLUMN_PATH_TYPE_NAME, OrderOverviewEntry::getTypeName)
        .addSelectFunction(COLUMN_PATH_MONITORING_LEVEL, OrderOverviewEntry::getMonitoringLevel)
        .addSelectFunction(COLUMN_PATH_ROOT_ID, OrderOverviewEntry::getRootId)        
        .addTableToDbMapping(COLUMN_PATH_ID, OrderInstanceColumn.C_ID.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_CUSTOMER_1, OrderInstanceColumn.C_CUSTOM0.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_CUSTOMER_2, OrderInstanceColumn.C_CUSTOM1.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_CUSTOMER_3, OrderInstanceColumn.C_CUSTOM2.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_CUSTOMER_4, OrderInstanceColumn.C_CUSTOM3.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_PRIORITY, OrderInstanceColumn.C_PRIORITY.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_STATUS, OrderInstanceColumn.C_STATUS.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_RUNTIME_START, OrderInstanceColumn.C_START_TIME.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_RUNTIME_STOP, OrderInstanceColumn.C_LAST_UPDATE.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_RTC_WORKSPACE, OrderInstanceColumn.C_WORKSPACENAME.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_RTC_APPLICATION, OrderInstanceColumn.C_APPLICATIONNAME.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_RTC_VERSION, OrderInstanceColumn.C_VERISONNAME.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_TYPE_NAME, OrderInstanceColumn.C_ORDER_TYPE.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_MONITORING_LEVEL, OrderInstanceColumn.C_MONITORING_LEVEL.getColumnName())
        .addTableToDbMapping(COLUMN_PATH_ROOT_ID, OrderInstanceColumn.C_ROOT_ID.getColumnName());
  }
  
  private static OrderOverviewEntry convert(OrderInstance order) {
    OrderOverviewEntry curEntry = new OrderOverviewEntry();
    curEntry.setId(order.getId());
    curEntry.setPriority(order.getPriority());
    curEntry.setStatus(order.getStatusAsString());
//    curEntry.setStatus(sqlToGuiStatus(order.getStatusAsString())); TODO
    curEntry.setTypeName(order.getOrderType());
    curEntry.setMonitoringLevel(order.getMonitoringLevel());
    curEntry.setCustomFields(new CustomFields(order.getCustom0(), order.getCustom1(), order.getCustom2(), order.getCustom3()));

    if ( (order.getWorkspaceName() != null) && (order.getWorkspaceName().length() > 0) ) {
      curEntry.setRuntimeContext(new RuntimeContext(null, null, order.getWorkspaceName()));
    } else {
      curEntry.setRuntimeContext(new RuntimeContext(order.getApplicationName(), order.getVersionName(), null));
    }

    TimeSpan runTime = new TimeSpan();
    runTime.setStart(order.getStartTime());
    runTime.setStop(order.getLastUpdate());

    curEntry.setRunTime(runTime);
    if (order.getId() != order.getRootId()) {
      curEntry.setRootId(Long.toString(order.getRootId()));
    }
    return curEntry;
  }

  /**
   * This returns a list of up to maxRows entries of OrderInstance that meets the criteria specified through select and onlyRootOrders.
   * 
   * Root orders can't be filtered directly, since those are orders where the order-id and the root-id are identical, which is a filter criterion that
   * is not supported by the xyna search order beans. Hence, an iterative process is used instead that queries a number of rows filters the result and
   * queries again with a doubled row-limit in case the number of remaining entries is too low. This is done up to MAX_ROOT_ORDER_SEARCH_ITERATIONS times.
   * 
   * WARNING: Increasing the value for MAX_ROOT_ORDER_SEARCH_ITERATIONS can quickly lead to out of memory-problems, since the potential maximum
   * memory requirement for a search doubles with each iteration.
   */
  private static List<OrderInstance> getOrderInstanceList(XynaMultiChannelPortal multiChannelPortal, OrderInstanceSelect select, int maxRows, boolean onlyRootOrders) throws PersistenceLayerException {
    List<OrderInstance> orders = multiChannelPortal.searchOrderInstances(select, maxRows, SearchMode.FLAT).getResult();

    if (!onlyRootOrders) {
      return orders;
    }

    List<OrderInstance> rootOrders = extractRootOrders(orders);
    int requestedRows = maxRows;
    int searchIteration = 0;

    while ( (rootOrders.size() < maxRows) && (searchIteration < MAX_ROOT_ORDER_SEARCH_ITERATIONS) && requestedRows < MAX_ROWS ) {
      requestedRows *= 2;
      if(requestedRows > MAX_ROWS)
        requestedRows = MAX_ROWS;
      orders = multiChannelPortal.searchOrderInstances(select, requestedRows, SearchMode.FLAT).getResult();
      rootOrders = extractRootOrders(orders);
      searchIteration++;
    }

    return rootOrders.subList(0, Math.min(rootOrders.size(), maxRows));
  }

  private static List<OrderInstance> extractRootOrders(List<OrderInstance> orders) {
    List<OrderInstance> filteredOrders = new ArrayList<OrderInstance>();
    for (OrderInstance order: orders) {
      if (order.getRootId() == order.getId()) {
        filteredOrders.add(order);
      }
    }

    return filteredOrders;
  }
}
