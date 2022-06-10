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

package xmcp.processmonitor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xpce.manualinteraction.ExtendedOutsideFactorySerializableManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction.ProcessManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionColumn;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;

import xmcp.processmonitor.datatypes.LoadManualInteractionException;
import xmcp.processmonitor.datatypes.ManualInteractionEntry;
import xmcp.processmonitor.datatypes.ManualInteractionId;
import xmcp.processmonitor.datatypes.ManualInteractionProcessResponse;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;

public class ManualInteractions {
  
  private static final String TABLE_PATH_ID = "id";
  private static final String TABLE_PATH_REASON = "reason";
  private static final String TABLE_PATH_TODO = "todo";
  private static final String TABLE_PATH_TYPE = "type";
  private static final String TABLE_PATH_USER_GROUP = "userGroup";
  private static final String TABLE_PATH_PRIORITY = "priority";
  private static final String TABLE_PATH_START_TIME = "startTime";
  private static final String TABLE_PATH_LAST_UPDATE = "lastUpdate";
  private static final String TABLE_PATH_APPLICATION = "application";
  private static final String TABLE_PATH_VERSION = "version";
  private static final String TABLE_PATH_WORKSPACE = "workspace";
  private static final String TABLE_PATH_ORDER_TYPE = "orderType";
  
  private static XynaMultiChannelPortal multiChannelPortal = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());
  private static ManualInteractionManagement manualInteractionManagement = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement();
  
  private ManualInteractions() {

  }
  
  public static List<? extends ManualInteractionProcessResponse> processMI(List<? extends ManualInteractionId> manualInteractionIds, xmcp.processmonitor.datatypes.ManualInteractionResponse manualInteractionResponse) {
    List<ManualInteractionProcessResponse> response = new ArrayList<>(manualInteractionIds.size());
    for (ManualInteractionId manualInteractionId : manualInteractionIds) {
      ManualInteractionProcessResponse miResponse = new ManualInteractionProcessResponse();
      miResponse.setManualInteractionId(manualInteractionId);
      try {
        GeneralXynaObject xynaResponse = ManualInteractionResponse.getManualInteractionResponseFromXmlName(manualInteractionResponse.getResponse()).getMDMRepresentation();
        ProcessManualInteractionResult processResult = manualInteractionManagement.processManualInteractionEntry(manualInteractionId.getId(), xynaResponse);
        switch(processResult) {
          case SUCCESS:
            miResponse.setSuccess(true);
            break;
          case FOREIGN_BINDING:
            miResponse.setSuccess(false);
            miResponse.setReason("FOREIGN_BINDING");
            break;
          case NOT_FOUND:
            miResponse.setSuccess(false);
            miResponse.setReason("Manual Interaction not found");
            break;
            
        }
      } catch (Exception t) {
        miResponse.setSuccess(false);
        miResponse.setReason(t.getMessage());
      }
      response.add(miResponse);
    }
    return response;
  }
  
  public static List<ManualInteractionEntry> getMIEntries(TableInfo tableInfo) throws LoadManualInteractionException {
    TableHelper<ManualInteractionEntry, TableInfo> tableHelper = createTableHelper(tableInfo);
    SearchRequestBean srb = createMiSearchRequestBean(tableInfo);
    try {
      ManualInteractionSelect mis = (ManualInteractionSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      ExtendedManualInteractionResult mir = manualInteractionManagement.searchExtended(mis, tableInfo.getLimit());
      srb = createOrderArchivSearchRequestBean(tableInfo, mir.getResult());
      OrderInstanceSelect ois = (OrderInstanceSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      OrderInstanceResult oir = multiChannelPortal.search(ois, 100);
      List<ManualInteractionEntry> result = createResult(oir.getResult(), mir.getResult()).stream().filter(tableHelper.filter()).collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (XNWH_NoSelectGivenException | XNWH_WhereClauseBuildException | XNWH_SelectParserException | PersistenceLayerException e) {
      throw new LoadManualInteractionException(e.getMessage(), e);
    }
  }
  
  private static TableHelper<ManualInteractionEntry, TableInfo> createTableHelper(TableInfo searchRequest){
    return TableHelper.<ManualInteractionEntry, TableInfo>init(searchRequest)
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
        .addSelectFunction(TABLE_PATH_ID, ManualInteractionEntry::getId)
        .addSelectFunction(TABLE_PATH_REASON, e -> e.getReason() != null ? e.getReason() : "")
        .addSelectFunction(TABLE_PATH_TODO, e -> e.getTodo() != null ? e.getTodo() : "")
        .addSelectFunction(TABLE_PATH_TYPE, e -> e.getType() != null ? e.getType() : "")
        .addSelectFunction(TABLE_PATH_USER_GROUP, e -> e.getUserGroup() != null ? e.getUserGroup() : "")
        .addSelectFunction(TABLE_PATH_PRIORITY, ManualInteractionEntry::getPriority)
        .addSelectFunction(TABLE_PATH_START_TIME, e -> e.getStartTime() != null ? e.getStartTime() : "")
        .addSelectFunction(TABLE_PATH_LAST_UPDATE, e -> e.getLastUpdate() != null ? e.getLastUpdate() : "")
        .addSelectFunction(TABLE_PATH_APPLICATION, e -> e.getApplication() != null ? e.getApplication() : null)
        .addSelectFunction(TABLE_PATH_VERSION, e -> e.getVersion() != null ? e.getVersion() : "")
        .addSelectFunction(TABLE_PATH_WORKSPACE, e -> e.getWorkspace() != null ? e.getWorkspace() : "")
        .addSelectFunction(TABLE_PATH_ORDER_TYPE, e -> e.getOrderType() != null ? e.getOrderType() : "");
  }
  
  private static List<ManualInteractionEntry> createResult(List<OrderInstance> orderInstances, List<ExtendedOutsideFactorySerializableManualInteractionEntry> manualInteractionEntries){
    List<ManualInteractionEntry> result = new ArrayList<>(orderInstances.size());
    manualInteractionEntries.forEach(mi -> {
      ManualInteractionEntry e = new ManualInteractionEntry();
      e.setAllowAbort(false);
      e.setAllowContinue(false);
      e.setAllowRetry(false);
      
      if(mi.getAllowedResponses() != null) {
        String[] allowedResponses = mi.getAllowedResponses().split(",");
        for (String response : allowedResponses) {
          if(ManualInteractionResponse.ABORT.getXmlName().equals(response)) {
            e.setAllowAbort(true);
          } else if (ManualInteractionResponse.CONTINUE.getXmlName().equals(response)) {
            e.setAllowContinue(true);
          } else if (ManualInteractionResponse.RETRY.getXmlName().equals(response)) {
            e.setAllowRetry(true);
          }
        }
      } else {
        e.setAllowAbort(true);
        e.setAllowContinue(true);
      }
      e.setApplication(mi.getApplication());
      e.setId(new ManualInteractionId(mi.getId()));
      e.setMonitoringCode(mi.getMonitoringCode());
      e.setOrderType(mi.getOrdertype());
      e.setParentId(mi.getParentId());
      e.setPriority(mi.getPriority());
      e.setReason(mi.getReason());
      e.setSessionId(mi.getSessionId());
      e.setTodo(mi.getTodo());
      e.setType(mi.getType());
      e.setUserGroup(mi.getUserGroup());
      e.setVersion(mi.getVersion());
      e.setWorkspace(mi.getWorkspace());
      result.add(e);
    });
    orderInstances.forEach(oi -> {
      Optional<ManualInteractionEntry> opt = result.stream().filter(mi -> oi.getId() == mi.getParentId()).findFirst();
      if(opt.isPresent()) {
        ManualInteractionEntry mi = opt.get();
        mi.setMonitoringCode(oi.getMonitoringLevel());
        mi.setStartTime(oi.getStartTime());
        mi.setLastUpdate(oi.getLastUpdate());
        mi.setPriority(oi.getPriority());
      }
    });
    return result;
  }
  
  private static SearchRequestBean createOrderArchivSearchRequestBean(TableInfo tableInfo, List<ExtendedOutsideFactorySerializableManualInteractionEntry> foundMIs) {
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.orderarchive);
    srb.setMaxRows(tableInfo.getLimit());
    srb.setSelection(selectAllRelevantOrderInstanceFieldsForManualInteractionSearch());
    Map<String, String> filter = new HashMap<>();
    tableInfo.getColumns().stream().filter(tc -> {
      return !tc.getDisableFilter() && tc.getPath() != null && tc.getFilter() != null && tc.getFilter().length() > 0;
    }).forEach(tc -> {
      switch(tc.getPath()) {
        case TABLE_PATH_PRIORITY:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_PRIORITY.getColumnName(), f));
          break;
        case TABLE_PATH_START_TIME:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_START_TIME.getColumnName(), f));
          break;
        case TABLE_PATH_LAST_UPDATE:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_LAST_UPDATE.getColumnName(), f));
          break;
        case TABLE_PATH_APPLICATION:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_APPLICATIONNAME.getColumnName(), f));
          break;
        case TABLE_PATH_VERSION:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_VERISONNAME.getColumnName(), f));
          break;
        case TABLE_PATH_WORKSPACE:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_WORKSPACENAME.getColumnName(), f));
          break;
        case TABLE_PATH_ORDER_TYPE:
          TableHelper.prepareQueryFilter(tc.getFilter()).forEach(f -> filter.put(OrderInstanceColumn.C_ORDER_TYPE.getColumnName(), f));
          break;
        default:
          break;
      }
    });
    StringBuilder idFilterBuilder = new StringBuilder();
    if (!filter.containsKey(OrderInstanceColumn.C_ID.getColumnName()) && foundMIs != null && !foundMIs.isEmpty()) {
      idFilterBuilder.append("(");
      for (int i = 0; i < foundMIs.size(); i++) {
        long id = foundMIs.get(i).getParentId();
        if (id == 0) {
          id = foundMIs.get(i).getId();
        }
        idFilterBuilder.append(id);
        if (i+1<foundMIs.size()) {
          idFilterBuilder.append(" OR ");
        }
      }
      idFilterBuilder.append(")");
      filter.put(OrderInstanceColumn.C_ID.getColumnName(), idFilterBuilder.toString());
    }    
    srb.setFilterEntries(filter);
    return srb;
  }
  
  private static String selectAllRelevantOrderInstanceFieldsForManualInteractionSearch() {
    StringBuilder selectionBuilder = new StringBuilder();
    selectionBuilder.append(OrderInstanceColumn.C_ID.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_LAST_UPDATE.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_MONITORING_LEVEL.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_ORDER_TYPE.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_PARENT_ID.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_STATUS.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_SESSION_ID.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_STATUS_COMPENSATE.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_START_TIME.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_STOP_TIME.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_PRIORITY.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_APPLICATIONNAME.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_VERISONNAME.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_WORKSPACENAME.getColumnName());
    selectionBuilder.append(",");
    selectionBuilder.append(OrderInstanceColumn.C_EXECUTION_TYPE.getColumnName());
    return selectionBuilder.toString();
  }
  
  private static SearchRequestBean createMiSearchRequestBean(TableInfo tableInfo) {
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.miarchive);
    // Limit deaktiviert, damit das Filtern über den TableHelper korrekt funktioniert
    //srb.setMaxRows(tableInfo.getLimit());
    srb.setSelection(selectAllForManualInteractionEntry());
    // Filter deaktiviert, damit das Filtern über den TableHelper korrekt funktioniert.
    /*
    Map<String, String> filter = new HashMap<>();
    tableInfo.getColumns().stream().filter(tc -> 
      !tc.getDisableFilter() && tc.getPath() != null && tc.getFilter() != null && tc.getFilter().length() > 0
    ).forEach(tc -> {
      switch(tc.getPath()) {
        case TABLE_PATH_ID:
          filter.put(ManualInteractionColumn.ID.name(), TableHelper.prepareQueryFilter(tc.getFilter()));
          break;
        case TABLE_PATH_REASON:
          filter.put(ManualInteractionColumn.reason.name(), prepareQueryLikeFilter(tc.getFilter()));
          break;
        case TABLE_PATH_TODO:
          filter.put(ManualInteractionColumn.todo.name(), prepareQueryLikeFilter(tc.getFilter()));
          break;
        case TABLE_PATH_TYPE:
          filter.put(ManualInteractionColumn.type.name(), prepareQueryLikeFilter(tc.getFilter()));
          break;
        case TABLE_PATH_USER_GROUP:
          filter.put(ManualInteractionColumn.userGroup.name(), prepareQueryLikeFilter(tc.getFilter()));
          break;
        default:
          break;
      }
    });
    srb.setFilterEntries(filter);
    */
    return srb;
  }

  
  private static String selectAllForManualInteractionEntry() {
    StringBuilder selectionBuilder = new StringBuilder();
    for (ManualInteractionColumn column : ManualInteractionColumn.values()) {
      selectionBuilder.append(column.toString());
      selectionBuilder.append(",");
    }
    return selectionBuilder.toString().substring(0, selectionBuilder.toString().length() -1);
  }
}
