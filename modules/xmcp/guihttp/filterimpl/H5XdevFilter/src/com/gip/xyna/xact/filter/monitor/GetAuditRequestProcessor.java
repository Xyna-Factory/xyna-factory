/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.monitor.MonitorSession.MonitorSessionInstance;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer.MissingImport;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.Dataflow.InputConnection;
import com.gip.xyna.xact.filter.session.Dataflow.SimpleConnection;
import com.gip.xyna.xact.filter.session.repair.WorkflowRepair;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;

import xmcp.processmodeller.datatypes.Connection;
import xmcp.processmodeller.datatypes.RepairEntry;
import xmcp.processmodeller.datatypes.Workflow;
import xmcp.processmonitor.datatypes.CustomField;
import xmcp.processmonitor.datatypes.Error;
import xmcp.processmonitor.datatypes.NoAuditData;
import xmcp.processmonitor.datatypes.RollbackStep;
import xmcp.processmonitor.datatypes.RunningTime;
import xmcp.processmonitor.datatypes.RuntimeInfo;
import xmcp.processmonitor.datatypes.WorkflowRuntimeInfo;
import xmcp.processmonitor.datatypes.response.GetAuditResponse;
import xmcp.xact.modeller.Hint;
import xprc.xpce.RuntimeContext;



public class GetAuditRequestProcessor {

  private static Configuration config = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();

  public static String CUSTOM_FIELD_PROPERTY_PREFIX  = "xyna.processmonitor.customColumn";
  public static String CUSTOM_FIELD_PROPERTY_ENABLED = "enabled";
  public static String CUSTOM_FIELD_PROPERTY_LABEL   = "label";
  

  public GetAuditResponse processGetAuditRequestFromUpload(MonitorSessionInstance session, String fileId) throws NoAuditData {
    try {
      return createGetAuditResponse(MonitorAudit.fromUpload(session, fileId));
    } catch (Throwable ex) {
      return createGetAuditResponse(-1l, ex);
    }
  }

  public GetAuditResponse processGetAuditRequest(Long orderId) throws NoAuditData {
    try {
      return createGetAuditResponse(MonitorAudit.fromLocalOrder(orderId));
    } catch (Throwable ex) {
      return createGetAuditResponse(orderId, ex);
    }
  }

  private GetAuditResponse createGetAuditResponse(MonitorAudit monitorAudit) {
    
    GetAuditResponse result = new GetAuditResponse();
    result.setOrderId(monitorAudit.getGuiOrderId());
    result.setParentOrderId(monitorAudit.getGuiParentOrderId());
    result.setRevision(0);

    int lazyLoadingLimit = (AuditPreprocessing.LAZY_LOADING_LIMIT.get() != 0) ? AuditPreprocessing.LAZY_LOADING_LIMIT.get() : 1;
    result.setLazyLoadingLimit(lazyLoadingLimit);

    if (monitorAudit.getWorkflow() != null) {
      WorkflowRepair repair = new WorkflowRepair();
      List<RepairEntry> entries = repair.repairAuditWorkflow(monitorAudit.getWorkflow());
      result.setRepairResult(entries);
    }
    
    try {
      result.setInfo(fillWorkflowRuntimeInfo(monitorAudit));
    } catch (Exception e) {
      Utils.logError("Could not determine any runtime info for order " + monitorAudit.getOrderId(), e);
    }

    try {
      result.setRootRtc(getRuntimeContext(monitorAudit.getRuntimeContext()));
    } catch (Exception e) {
      Utils.logError("Could not determine root rtc for order " + monitorAudit.getOrderId(), e);
    }

    try {
      result.setDataflow(fillDataflow(monitorAudit));
    } catch (Exception e) {
      Utils.logError("Could not determine dataflow for order " + monitorAudit.getOrderId(), e);
    }
    
    try {
      result.setWorkflow(fillWorkflow(monitorAudit));
    } catch (Exception e) {
      Utils.logError("Could not determine workflow for order " + monitorAudit.getOrderId(), e);
    }

    try {
      result.setCustomFields(getCustomFields(monitorAudit));
    } catch (Exception e) {
      Utils.logError("Could not determine custom fields for order " + monitorAudit.getOrderId(), e);
    }

    try {
      result.setErrors(createErrors(monitorAudit));
    } catch (Exception e) {
      Utils.logError("Could not determine errors for order " + monitorAudit.getOrderId(), e);
    }
    
    try {
      result.setRollback(new ArrayList<RollbackStep>()); //TODO
    } catch (Exception e) {
      Utils.logError("Could not determine rollback for order " + monitorAudit.getOrderId(), e);
    }

    if (monitorAudit.getMissingImports() != null && monitorAudit.getMissingImports().size() > 0) {
      Hint missingImportsHint = new Hint();
      StringBuilder builder = new StringBuilder();
      builder.append("There are ");
      builder.append(monitorAudit.getMissingImports().size());
      builder.append(" missing Imports in the audit:");
      for (MissingImport mi : monitorAudit.getMissingImports()) {
        builder.append(mi.getTypePath() + "." + mi.getTypeName() + ": " + mi.getOperationName() + ",");
      }
      builder.setLength(builder.length()-1); //remove last ","
      missingImportsHint.setDescription(builder.toString());
      result.setHints(Arrays.asList(new Hint[] {missingImportsHint}));
    }

    return result;
  }

  private GetAuditResponse createGetAuditResponse(Long orderId, Throwable exception) {
    GetAuditResponse result = new GetAuditResponse();
    result.setOrderId(Long.toString(orderId));

    XynaExceptionInformation xei = new XynaExceptionInformation(exception);
    Error e = createErrorObject(xei);
    List<Error> errors = new ArrayList<>();
    errors.add(e);
    result.setErrors(errors);

    List<OrderInstance> oil = null;
    try {
      SearchRequestBean srb = new SearchRequestBean(ArchiveIdentifier.orderarchive, 1);
      srb.setFilterEntries(Map.of(OrderInstanceColumn.C_ID.getColumnName(), Long.toString(orderId)));
      OrderInstanceSelect ois = (OrderInstanceSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      XynaMultiChannelPortal xmcp = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());
      OrderInstanceResult oir = xmcp.searchOrderInstances(ois, 1, SearchMode.FLAT);
      oil = oir.getResult();
    } catch (Exception ex) {
      xei = new XynaExceptionInformation(ex);
      e = createErrorObject(xei);
      errors.add(e);
    }

    if (oil != null && oil.size() > 0) {
      OrderInstance oi = oil.get(0);
      result.setParentOrderId(Long.toString(oi.getParentId()));

      try {
        result.addToCustomFields(new CustomField(getCustomFieldLabel(0), oi.getCustom0()));
        result.addToCustomFields(new CustomField(getCustomFieldLabel(1), oi.getCustom1()));
        result.addToCustomFields(new CustomField(getCustomFieldLabel(2), oi.getCustom2()));
        result.addToCustomFields(new CustomField(getCustomFieldLabel(3), oi.getCustom3()));
      } catch (Exception ex) {
        Utils.logError("Could not determine custom fields for order " + orderId, ex);
      }

      try {
        WorkflowRuntimeInfo wri = new WorkflowRuntimeInfo();
        wri.setId("wf");
        wri.setRunningTime(new RunningTime(oi.getStartTime(), oi.getStopTime()));
        wri.setStatus(oi.getStatusAsString());
        result.setInfo(List.of(wri));
      } catch (Exception ex) {
        Utils.logError("Could not determine any runtime info for order " + orderId, ex);
      }

      try {
        if (oi.getWorkspaceName() != null && oi.getWorkspaceName().length() > 0) {
          result.setRootRtc(new xprc.xpce.Workspace(oi.getWorkspaceName()));
        } else if (oi.getApplicationName() != null && oi.getApplicationName().length() > 0) {
          result.setRootRtc(new xprc.xpce.Application(oi.getApplicationName(), oi.getVersionName()));
        }
      } catch (Exception ex) {
        Utils.logError("Could not determine root rtc for order " + orderId, ex);
      }
    }

    return result;
  }

  private List<Error> createErrors(MonitorAudit monitorAudit){
    if(monitorAudit.getExceptions() != null) {
      return monitorAudit.getExceptions().stream().map(ex -> createErrorObject(ex)).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  private Error createErrorObject(XynaExceptionInformation ex) {
    Error e = new Error();
    e.setMessage(ex.getMessage());
    e.setException(ex.getClass().getName());

    StringBuilder sb = new StringBuilder();
    sb.append(ex.getClass().getName())
      .append(": ").append(ex.getMessage()).append("\n");
    for (StackTraceElement traceElement : ex.getStacktrace()) {
      sb.append(traceElement).append("\n");
    }
    e.setStacktrace(sb.toString());

    return e;
  }

  private RuntimeContext getRuntimeContext(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext) {
    RuntimeContext rtc = null;
    if (runtimeContext instanceof Workspace) {
      Workspace workspace = (Workspace)runtimeContext;
      rtc = new xprc.xpce.Workspace(workspace.getName());
    } else if (runtimeContext instanceof Application) {
      Application application = (Application)runtimeContext;
      rtc = new xprc.xpce.Application(application.getName(), application.getVersionName());
    }
    
    return rtc;
  }
  
  private List<CustomField> getCustomFields(MonitorAudit monitorAudit) {
    List<CustomField> result = new ArrayList<>();
    
    CustomField customField = new CustomField();
    String label = getCustomFieldLabel(0);
    String value = monitorAudit.getCustom0();
    label = (label == null || label.length() == 0) ? "Custom 1" : label;
    value = (value == null) ? "" : value;
    customField.setLabel(label);
    customField.setValue(value);
    result.add(customField);
    
    customField = new CustomField();    
    label = getCustomFieldLabel(1);
    value = monitorAudit.getCustom1();
    label = (label == null || label.length() == 0) ? "Custom 2" : label; 
    value = (value == null) ? "" : value;
    customField.setLabel(label);
    customField.setValue(value);
    result.add(customField);
    
    customField = new CustomField();
    label = getCustomFieldLabel(2);
    value = monitorAudit.getCustom2();
    label = (label == null || label.length() == 0) ? "Custom 3" : label;
    value = (value == null) ? "" : value;
    customField.setLabel(label);
    customField.setValue(value);
    result.add(customField);
    
    customField = new CustomField();
    label = getCustomFieldLabel(3);
    value = monitorAudit.getCustom3();
    label = (label == null || label.length() == 0) ? "Custom 4" : label;
    value = (value == null) ? "" : value;
    customField.setLabel(label);
    customField.setValue(value);
    result.add(customField);
    
    return result;
  }


  private List<RuntimeInfo> fillWorkflowRuntimeInfo(MonitorAudit monitorAudit) {
    StepVisitorForWorkflowRuntimeInfo visitor = new StepVisitorForWorkflowRuntimeInfo(monitorAudit);
    Set<Step> allSteps = ((WFStep)monitorAudit.getWorkflowGbo().getWFStep()).getAllStepsRecursively();
    for (Step s : allSteps) {
      try {
        s.visit(visitor);
      } catch (Exception e) {
        Utils.logError("Could not determine runtime info for step " + s.getXmlId(), e);
      }
    }

    return visitor.getResult();
  }


  private List<Connection> fillDataflow(MonitorAudit monitorAudit) {
    List<Connection> result = new ArrayList<>();
    Dataflow dataflow = new Dataflow(monitorAudit.getWorkflowGbo());
    Map<AVariableIdentification, InputConnection> data = dataflow.analyzeDataflow(monitorAudit.getWorkflow());

    for (Entry<AVariableIdentification, InputConnection> kvp : data.entrySet()) {
      for (SimpleConnection sCon : kvp.getValue().getConnectionsPerLane()) {
        Connection c = new Connection();

        c.setTargetId(kvp.getKey().internalGuiId.createId());
        c.setBranchId(sCon.getBranchId());
        c.setConstant(sCon.getConstant());
        c.setType(sCon.getLinkState().toString());
        for (AVariableIdentification v : sCon.getInputVars()) {
          Connection cpy = c.clone();
          cpy.setSourceId(v.internalGuiId.createId());
          result.add(cpy);
        }

        //entries for not connection variables. Should not happen for audits
        if (sCon.getInputVars() == null || sCon.getInputVars().size() == 0)
          result.add(c);
      }
    }


    return result;
  }


  private Workflow fillWorkflow(MonitorAudit monitorAudit) {
    com.gip.xyna.xact.filter.xmom.workflows.json.Workflow jsonWorkflow = new com.gip.xyna.xact.filter.xmom.workflows.json.Workflow(monitorAudit.getWorkflowGbo());
    return (Workflow) jsonWorkflow.getXoRepresentation();
  }


  private boolean isCustomFieldEnabled(int fieldIndex) {
    String propName = String.format("%s%d.%s", CUSTOM_FIELD_PROPERTY_PREFIX, fieldIndex, CUSTOM_FIELD_PROPERTY_ENABLED);
    XynaPropertyWithDefaultValue enabledProperty = config.getPropertyWithDefaultValue(propName);
    if (enabledProperty == null || enabledProperty.getValueOrDefValue() == null) {
      return false;
    }
    return Boolean.TRUE.toString().equalsIgnoreCase(enabledProperty.getValueOrDefValue());
  }


  private String getCustomFieldLabel(int fieldIndex) {
    if (!isCustomFieldEnabled(fieldIndex)) {
      return null;
    }
    String propName = String.format("%s%d.%s", CUSTOM_FIELD_PROPERTY_PREFIX, fieldIndex, CUSTOM_FIELD_PROPERTY_LABEL);
    XynaPropertyWithDefaultValue labelProperty = config.getPropertyWithDefaultValue(propName);
    return (labelProperty != null) ? labelProperty.getValueOrDefValue() : null;
  }
}
