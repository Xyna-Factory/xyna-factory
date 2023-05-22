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


import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.MetaInfo;
import com.gip.xyna.xact.filter.util.xo.RuntimeContextVisitor;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ErrorInfo;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Parameter;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditInformation;

import xmcp.graphs.datatypes.GraphData;
import xmcp.graphs.datatypes.GraphInfo;
import xmcp.processmonitor.datatypes.Audit;
import xmcp.processmonitor.datatypes.CancelFrequencyControlledTaskException;
import xmcp.processmonitor.datatypes.Error;
import xmcp.processmonitor.datatypes.FrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.GraphDatasource;
import xmcp.processmonitor.datatypes.LoadFrequencyControlledTasksException;
import xmcp.processmonitor.datatypes.LoadGraphDataException;
import xmcp.processmonitor.datatypes.NoAuditData;
import xmcp.processmonitor.datatypes.NoFrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.OrderOverviewEntry;
import xmcp.processmonitor.datatypes.RunningTime;
import xmcp.processmonitor.datatypes.SearchFlag;
import xmcp.processmonitor.datatypes.TaskId;
import xmcp.processmonitor.datatypes.WorkflowRuntimeInfo;
import xmcp.processmonitor.datatypes.response.GetAuditResponse;
import xmcp.tables.datatypes.TableInfo;
import xprc.xpce.CustomFields;
import xprc.xpce.RuntimeContext;


public class MonitorV2 {

  public static String CUSTOM_FIELD_PROPERTY_PREFIX  = "xyna.processmonitor.customColumn";
  public static String CUSTOM_FIELD_PROPERTY_ENABLED = "enabled";
  public static String CUSTOM_FIELD_PROPERTY_LABEL   = "label";

  private static XynaMultiChannelPortal multiChannelPortal = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());
  private static Configuration configuration = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessMonitorServicesServiceOperationImpl.class);
  private static JsonBuilder jsonBuilder;

  private static boolean CONTINUE_ON_EXCEPTION = true;
  
  public void cancelFrequencyControlledTask(TaskId taskId) throws CancelFrequencyControlledTaskException {
    FrequencyControlledTasks.cancelFrequencyControlledTask(taskId);
  }
  
  public List<? extends GraphData> getFrequencyControlledTaskGraphData(GraphInfo graphInfo, GraphDatasource graphDatasource, TaskId taskId) throws LoadGraphDataException {
    return FrequencyControlledTaskGraphData.getFrequencyControlledTaskGraphData(graphInfo, graphDatasource, taskId);   
  }
  
  public List<? extends FrequencyControlledTaskDetails> getFrequencyControlledTasks(TableInfo tableInfo)
      throws LoadFrequencyControlledTasksException {
    return FrequencyControlledTasks.getFrequencyControlledTasks(tableInfo);
  }
  
  public FrequencyControlledTaskDetails getFrequencyControlledTaskDetails(TaskId taskId) throws NoFrequencyControlledTaskDetails {
    return FrequencyControlledTasks.getFrequencyControlledTaskDetails(taskId);
  }
  
  // TODO: doppelter Code mit RMIChannelImpl.getAuditInformationInternally -> an Stelle auslagern, die von beiden genutzt werden kann
  private AuditInformation getAuditInformation(MonitorAudit monitorAudit) throws XynaException {
    if(monitorAudit.getEnhancedAudit() == null) {
      return null;
    }

    ExecutionType type = ExecutionType.valueOf(monitorAudit.getExecutionType());
    AuditInformation ai =
        new AuditInformation(monitorAudit.getEnhancedAudit(), monitorAudit.getEnhancedAudit().getWorkflowContext() != null ? monitorAudit.getEnhancedAudit().getWorkflowContext() : monitorAudit.getRuntimeContext(), type);
    if (type == ExecutionType.SERVICE_DESTINATION) {
      String serviceoperation = monitorAudit.getOrderType().substring(monitorAudit.getOrderType().lastIndexOf('.') + 1);

      String pathAndName = monitorAudit.getAuditDataXml();
      final String SERVICE_TAG = "<Service>";
      final String SERVICE_END_TAG = "</Service>";
      int beginIndex = pathAndName.indexOf(SERVICE_TAG);
      int endIndex = pathAndName.indexOf(SERVICE_END_TAG);
      if (beginIndex < 0 || endIndex < 0) {
        throw new XynaException("No Service-Meta-Tag present");
      }
      beginIndex += SERVICE_TAG.length();
      pathAndName = pathAndName.substring(beginIndex, endIndex);
      int sliceIndex = pathAndName.substring(0, pathAndName.lastIndexOf('.')).lastIndexOf('.');
      String serviceName = pathAndName.substring(sliceIndex + 1);
      String servicepath = pathAndName.substring(0, sliceIndex);
      ai.setServiceDestinationInfo(servicepath, serviceName, serviceoperation);
    }
    return ai;
  }
  
//  private WF getWFObject(MonitorAudit monitorAudit) throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
//    String mainWfFqn = monitorAudit.getWorkflowFqn();
//    Map<String, String> xmlsWfAndImports = new HashMap<String, String>();
//    xmlsWfAndImports.put(mainWfFqn, monitorAudit.getWorkflowXml()); // add main Workflow
//    
//    // add imports
//    for (AuditImport curImport : monitorAudit.getEnhancedAudit().getImports()) {
//      xmlsWfAndImports.putIfAbsent(curImport.getFqn(), curImport.getDocument());
//    }
//    
//    WF wf = WF.getOrCreateInstanceForAudits(mainWfFqn, xmlsWfAndImports);
//    wf.parseGeneration(false, false, false); // TODO: Parameter korrekt?
//    WorkflowUtils.prepareWorkflowForMonitor(wf.getWfAsStep());
//    return wf;
//  }
  
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
  
  private List<Step> collectChildElements(Step step) {
    List<Step> childElements = new ArrayList<Step>();
    if (step.getChildSteps() != null) {
      childElements.addAll(step.getChildSteps());
      childElements.addAll(collectChildElements(step.getChildSteps()));
    }
    
    
    return childElements;
  }
  
  private List<Step> collectChildElements(List<Step> steps) {
    List<Step> childElements = new ArrayList<Step>();
    for (Step step : steps) {
      if (step.getChildSteps() == null) {
        continue;
      }
      
      childElements.addAll(step.getChildSteps());
      childElements.addAll(collectChildElements(step));
    }
    
    return childElements;
  }
  
  
  private String getJsonFromParameter(Step step, AVariable parameter, List<Integer> foreachIndices, int retryCounter) {
    if (parameter == null) {
      return null;
    }
    
    jsonBuilder = new JsonBuilder();
    jsonBuilder.startObject(); // start outer root-element
    convertParameterToJson(step, parameter, foreachIndices, retryCounter); // create inner JSON
    jsonBuilder.endObject(); // end outer root-element
    String json = jsonBuilder.toString();
    
    if (json.length() == 4) { // only contains start and end with line beaks
      // create empty entry with meta-tag to let GUI know what type element is of
      jsonBuilder = new JsonBuilder();
      jsonBuilder.startObject(); // start outer root-element
      createEmptyJsonObject(step, parameter, getLabel(parameter), foreachIndices, retryCounter, false); // create inner JSON
      jsonBuilder.endObject(); // end outer root-element
      json = jsonBuilder.toString();
    }
    
    return json;
  }
  
  private void createEmptyJsonObject(Step step, AVariable parameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    createJson(step, parameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
    if (parameter.isList()) {
      jsonBuilder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
      jsonBuilder.endList();
    }
    
    jsonBuilder.endObject(); // end created json-object
  }
  
  private List<String> getJsonsFromParameterList(Step step, List<AVariable> parameterList, List<Integer> foreachIndices, int retryCounter) { // TODO: zusaetzlicher Parameter In/Out
    List<String> jsonParameterList = new ArrayList<String>();
    
    try {
      for (AVariable parameter : parameterList) {
        String json = getJsonFromParameter(step, parameter, foreachIndices, retryCounter);
        if ( (json != null) && (json.length() > 0) ) {
          jsonParameterList.add(json);
        }
      }
    } catch (Exception e) {
      Utils.logError("Parameters for step " + step + " could not be determined:", e);
      if (!CONTINUE_ON_EXCEPTION) {
        throw e;
      }
    }
    
    return (jsonParameterList.size() > 0) ? jsonParameterList : null;
  }
  
  private void convertParameterToJson(Step step, AVariable parameter, List<Integer> foreachIndices, int retryCounter) {
    convertParameterToJson(step, parameter, parameter, getLabel(parameter), foreachIndices, retryCounter, false);
  }
  
  private void convertParameterToJson(Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    if ( (parameter.getChildren() == null) || (parameter.getChildren().size() == 0) ) {  
      // parameter is the leaf element of the tree
      createJsonForLeafElement(step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar); // TODO: originalParameter?
    } else {
      // parameter is inner knot (or the root)
      createJsonForInnerElement(step, parameter, originalParameter, subObjectName, foreachIndices, retryCounter);
    }
  }
  
  private void createJsonForInnerElement(Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter) {
    // start new json-object for element
    createJson(step, parameter, originalParameter, subObjectName, foreachIndices, retryCounter, false);
    
    // recursively create JSON-elements for children
    if (parameter.isList()) {
      jsonBuilder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
      for (AVariable member : parameter.getChildren()) {
        convertParameterToJson(step, member, member, member.getVarName(), foreachIndices, retryCounter, true);
      }
      jsonBuilder.endList();
    } else {
      for (AVariable member : parameter.getChildren()) {
        convertParameterToJson(step, member, member, member.getVarName(), foreachIndices, retryCounter, true);
      }
    }
    
    jsonBuilder.endObject(); // end created json-object
  }
  
  private void createJsonForLeafElement(Step step, AVariable parameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) { // TODO: AVariable originalParameter?
    if (parameter.isList()) {
      if ( (parameter.getRefInstanceId() != null) && (parameter.getRefInstanceId().length() > 0) ) {
        // parameter values are not stored directly in data element, but in a referred field
        AVariable refParameter = parameterByInstanceId.get(parameter.getRefInstanceId());
        convertParameterToJson(step, refParameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
      } else if ( (parameter.getVarName().length() > 0) && (!parameter.getVarName().equals("n/a")) ) {
        // write list to JSON
        if (parameter.getValues() != null) {
          jsonBuilder.addListAttribute(subObjectName);
          for (String value : parameter.getValues()) {
            if (parameter.getJavaTypeEnum() == PrimitiveType.STRING) { // TODO: Was ist mit numbers?
              jsonBuilder.addStringListElement(value);
            } else {
              jsonBuilder.addPrimitiveListElement(value);
            }
          }
          jsonBuilder.endList();
        } else if (!parameter.isJavaBaseType()) {
          // create empty entry with meta-tag to let GUI know what type list is of
          createEmptyJsonObject(step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
        }
      }
    } else { // not a list
      if ( (parameter.getRefInstanceId() != null) && (parameter.getRefInstanceId().length() > 0) ) {
        // parameter value is not stored directly in data element, but in a referred field
        AVariable refParameter = parameterByInstanceId.get(parameter.getRefInstanceId());
        convertParameterToJson(step, refParameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
      } else if ( (parameter.getVarName().length() > 0) && (!parameter.getVarName().equals("n/a")) ) {
        if (parameter.getValue() != null) {
          // write value to JSON
          jsonBuilder.addStringAttribute(subObjectName, parameter.getValue());
        } else if (!parameter.isJavaBaseType()) {
          // create empty entry with meta-tag to let GUI know what type element is of
          createEmptyJsonObject(step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
        }
      }
    }
  }
  
  public static String TAG_ID = "$id";
  public static String TAG_SOURCE = "$source";
  
  private void createJson(Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    if ( (subObjectName == null) || (subObjectName.length() == 0) || (subObjectName.equals("n/a")) ) {
      jsonBuilder.startObject();
    } else {
      jsonBuilder.addObjectAttribute(subObjectName);
    }
    
    // add meta-tag
    String fqn = parameter.getOriginalPath() + "." + parameter.getOriginalName();
    jsonBuilder.addObjectAttribute(XynaObjectVisitor.META_TAG);
    jsonBuilder.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, fqn);
    jsonBuilder.addObjectAttribute(MetaInfo.RUNTIME_CONTEXT);
    
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext = fqnToRtcMap.get(fqn);
    if (runtimeContext.getType() == com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType.Workspace) {
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.WORKSPACE_LABEL, runtimeContext.getName());
    } else {
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.APPLICATION_LABEL, runtimeContext.getName());
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.VERSION_LABEL, ((Application)runtimeContext).getVersionName());
    }
    jsonBuilder.endObject(); // runtime context
    jsonBuilder.endObject(); // meta tag
    
    String id = Dataflow.createDataflowId(step, originalParameter);
    jsonBuilder.addStringAttribute(TAG_ID, id);
    
    if (!isMemberVar) {
      String sourceId;
      if (originalParameter instanceof DatatypeVariable) {
        Iterator<String> it = ((DatatypeVariable) originalParameter).getSourceIds().iterator();
        sourceId = it.hasNext() ? it.next() : null;
      } else if (originalParameter instanceof ExceptionVariable) {
        sourceId = null; //FIXME
      } else {
        throw new RuntimeException();
      }
      if (sourceId != null) {
        Pair<AVariable, Step> source = null;
        try {
          source = Dataflow.determineSource(parentScope, originalParameter, step, sourceId, foreachIndices, retryCounter);
        } catch (Exception e) {
          Utils.logError("Source for variable " + sourceId + " could not be determined:", e);
          if (!CONTINUE_ON_EXCEPTION) {
            throw e;
          }
        }

        if ((source != null) && (source.getSecond() instanceof StepForeach)) {
          // since dataflow-connections to loops are currently not supported, show connection to looped step, instead
          // source = determineSource(parentScope, source.getFirst(), null, "");
        }

        if ((source != null) && (source.getFirst() != null) && (source.getFirst().getId() != null)) {
          sourceId = Dataflow.createDataflowId(source.getSecond(), source.getFirst());
          jsonBuilder.addStringAttribute(TAG_SOURCE, sourceId);
        }
      } //else outputparameter
    }
  }
  
  private String getLabel(AVariable parameter) { 
    String label = parameter.getLabel();
    if ( (label == null) || (label.length() == 0) ) {
      try {
        // search for the variable in the workflow the parameter contains the values for
        label = parentScope.identifyVariable(parameter.getId()).getVariable().getLabel();
      } catch (Exception e) {
        Utils.logError("Couldn't find variable " + parameter.getId(), e);
        
        // fall back to variable name
        label = parameter.getOriginalName();
      }
    }
    
    // use a non-breaking space in case the label is empty to make sure the GUI element keeps its minimal size
    return ( (label != null) && (label.length() > 0) ) ? label : "�";
  }
  
  private Map<String, AVariable> parameterByInstanceId;
  
  private void createInstanceIdLookup(AVariable parameter) {
    if ( (parameter != null) && (parameter.getInstanceId() != null) && (parameter.getInstanceId() != "") ) {
      parameterByInstanceId.put(parameter.getInstanceId(), parameter);
    }
    
    if (parameter.getChildren() != null) {
      for (AVariable parameterChild : parameter.getChildren()) {
        createInstanceIdLookup(parameterChild);
      }
    }
  }
  
  private void createInstanceIdLookup(Step step) {
    List<Parameter> parameterList = step.getParameterList();
    if ( (parameterList != null) ) {
      for (Parameter parameter : parameterList) {
        if (parameter.getInputData() != null) {
          for (AVariable input : parameter.getInputData()) {
            createInstanceIdLookup(input);
          }
        }
        
        if (parameter.getOutputData() != null) {
          for (AVariable output : parameter.getOutputData()) {
            createInstanceIdLookup(output);
          }
        }
      }
    }
    
    if (step.getChildSteps() != null) {
      for (Step childStep : step.getChildSteps()) {
        createInstanceIdLookup(childStep);
      }
    }
  }
  
  /**
   * Adds data to params that is not contained in paramater-tags and can only be obtained when all steps have already been parsed, since cross-referencing is needed to obtain data.
   */
  private void addAdditionalDataToParams(WF wf) {
    wf.getWfAsStep().addLabelsToParameter();
    wf.getWfAsStep().addIdsToParameter();

    for (Step topLevelStep : wf.getWfAsStep().getChildSteps()) {
      for (Step step : Dataflow.collectAllSteps(topLevelStep)) {
        // labels of variables are not contained in the parameter-tags of the audit and need to be set separately
        step.addLabelsToParameter();
        
        // ids need to be set after parsing since referred variables might not be parsed yet while then step is being parsed
        step.addIdsToParameter();
      }
    }
  }
  
  private ScopeStep parentScope = null;
  
  private RunningTime getRunningTime(Parameter parameter) throws ParseException {
    RunningTime result = new RunningTime();
    result.setStart(parameter.getInputTimeStampUnix());
    if (parameter.getErrorInfo() == null) {
      result.setLastUpdate(parameter.getOutputTimeStampUnix());
    } else {
      result.setLastUpdate(parameter.getErrorTimeStampUnix());
    }
    return result;
  }
  
  public enum OrderInstanceGuiStatus {
    FINISHED("Finished", 0),
    RUNNING("Running", 1),
    FAILED("Failed", 2),
    UNKNOWN("Unknown", 3);
    
    private String name;
    private int severity;
    
    private OrderInstanceGuiStatus(String name, int severity) {
      this.name = name;
      this.severity = severity;
    }
    
    String getName() {
      return name;
    }
    
    int getSeverity() {
      return severity;
    }
    
    public static OrderInstanceGuiStatus createOrderInstanceGuiStatus(String name) {
      if (name.equals(OrderInstanceGuiStatus.FINISHED.getName())) {
        return OrderInstanceGuiStatus.FINISHED;
      } if (name.equals(OrderInstanceGuiStatus.RUNNING.getName())) {
        return OrderInstanceGuiStatus.RUNNING;
      } if (name.equals(OrderInstanceGuiStatus.FAILED.getName())) {
        return OrderInstanceGuiStatus.FAILED;
      } else {
        return OrderInstanceGuiStatus.UNKNOWN;
      }
    }
  }
  
  private Error getError(Step step, ErrorInfo errorInfo) {
    if (errorInfo == null) {
      return null;
    }
    
    Error error = new Error();
    try {
      error.setException(getJsonFromParameter(step, errorInfo.getExceptionVariable(), null, -1));
      error.setStacktrace(errorInfo.getStacktrace());
      error.setMessage(errorInfo.getMessage());
    } catch (Exception e) {
      Utils.logError("Error for step " + step + " could not be determined:", e);
      if (!CONTINUE_ON_EXCEPTION) {
        throw e;
      }
    }
    
    return error;
  }
  
  private Error getError(List<XynaExceptionInformation> exceptions) {
    if (exceptions == null || exceptions.size() == 0) {
      return null;
    }
    
    XynaExceptionInformation exception = exceptions.get(0); // TODO: handle cases with more exceptions (OP 3462)
    
    Error error = new Error();
    error.setMessage(exception.getMessage());
    
    String stackTraceStr = "";
    for (StackTraceElement element : exception.getStacktrace()) {
      if (stackTraceStr.length() > 0) {
        stackTraceStr += "\n";
      }
      stackTraceStr += element.toString();
    }
    error.setStacktrace(stackTraceStr);
    
    // TODO: set exception (OP 3463)
    
    return error;
  }
  
  private String getStatus(Parameter parameter) {
    ErrorInfo errorInfo = parameter.getErrorInfo(); 
    if (errorInfo != null) {
      return OrderInstanceGuiStatus.FAILED.getName();
    } else if ( (parameter.getOutputTimeStamp()) != null && (parameter.getOutputTimeStamp().length() > 0) ) {
      return OrderInstanceGuiStatus.FINISHED.getName();
    } else if ( (parameter.getInputTimeStamp()) != null && (parameter.getInputTimeStamp().length() > 0) ) {
      return OrderInstanceGuiStatus.RUNNING.getName();
    } else {
      return OrderInstanceGuiStatus.UNKNOWN.getName();
    }
  }
  
  Map<String, com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext> fqnToRtcMap = new HashMap<String, com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext>();
  
  private void createRtcLookup(List<AuditImport> auditImports) {
    for (AuditImport auditImport : auditImports) {
      fqnToRtcMap.put(auditImport.getFqn(), auditImport.getRuntimeContext());
    }
  }
  
  private boolean isCustomFieldEnabled(int fieldIndex) {
    XynaPropertyWithDefaultValue enabledProperty = configuration.getPropertyWithDefaultValue(CUSTOM_FIELD_PROPERTY_PREFIX + fieldIndex + "." + CUSTOM_FIELD_PROPERTY_ENABLED);
    if (enabledProperty == null || enabledProperty.getValueOrDefValue() == null) {
      return false;
    } else {
      return Boolean.TRUE.toString().toLowerCase().equals(enabledProperty.getValueOrDefValue().toLowerCase());
    }
  }
  
  public String getCustomFieldLabel(int fieldIndex) {
    if (isCustomFieldEnabled(fieldIndex)) {
      XynaPropertyWithDefaultValue labelProperty = configuration.getPropertyWithDefaultValue(CUSTOM_FIELD_PROPERTY_PREFIX + fieldIndex + "." + CUSTOM_FIELD_PROPERTY_LABEL);
      return (labelProperty != null) ? labelProperty.getValueOrDefValue() : null;
    } else {
      return null;
    }
  }
  
  
  //TODO: some duplication with getAudit
  public WF getWorkflowForAudit(MonitorAudit monitorAudit) throws NoAuditInformationException{
    WF wf = null;
    
    Audit audit = new Audit();
    audit.setOrderId(monitorAudit.getOrderId());
    
    try {
      audit.setRootRtc(getRuntimeContext(monitorAudit.getRuntimeContext()));
      audit.setParentOrderId(monitorAudit.getParentId());
      
      AuditInformation auditInformation = getAuditInformation(monitorAudit);
      if (auditInformation == null) {
        throw new NoAuditInformationException(monitorAudit.getExceptions());
      }
      
      addAdditionalDataToParams(monitorAudit.getWorkflow());
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    return wf;
  }
  
  
  public RuntimeContext getRootRTC(long orderId) {
    RuntimeContext result = null;
    try {
      OrderInstanceDetails details = multiChannelPortal.getOrderInstanceDetails(orderId);
      result = getRuntimeContext(details.getRuntimeContext());
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    return result;
  }
  
  
  public GetAuditResponse getWorkflowRuntimeInfo(MonitorAudit monitorAudit) throws NoAuditData {
    parameterByInstanceId = new HashMap<String, AVariable>();
    
    GetAuditResponse auditResponse = new GetAuditResponse();
    WorkflowRuntimeInfo workflowRuntimeInfo = new WorkflowRuntimeInfo();
//    auditResponse.setInfo(workflowRuntimeInfo);
    
    try {
      auditResponse.setRootRtc(getRuntimeContext(monitorAudit.getRuntimeContext()));
      auditResponse.setParentOrderId(monitorAudit.getGuiParentOrderId());
      
      AuditInformation auditInformation = getAuditInformation(monitorAudit);
      if (auditInformation == null) {
        // An error occured before the order entered the execution phase
        workflowRuntimeInfo.setError(getError(monitorAudit.getExceptions()));
        workflowRuntimeInfo.setStatus(OrderInstanceGuiStatus.FAILED.getName());
        RunningTime runningTime = new RunningTime(monitorAudit.getStartTime(), monitorAudit.getLastUpdate());
        workflowRuntimeInfo.setRunningTime(runningTime);
        return auditResponse;
      }
      
      addAdditionalDataToParams(monitorAudit.getWorkflow());
      parentScope = monitorAudit.getWorkflow().getWfAsStep();
      createRtcLookup(auditInformation.getAudit().getImports()); // create lookup that translates from fqn to rtc to be used when determining the rtc for element
      createInstanceIdLookup(monitorAudit.getWorkflow().getWfAsStep()); // create lookup that translates from InstanceId to parameter to be used when filling parameter that refer to other elements
      
      Parameter parameter = monitorAudit.getWorkflow().getWfAsStep().getFirstParameter();
      if (parameter != null) {
        workflowRuntimeInfo.setInputs(getJsonsFromParameterList(monitorAudit.getWorkflow().getWfAsStep(), parameter.getInputData(), null, -1));
        workflowRuntimeInfo.setOutputs(getJsonsFromParameterList(monitorAudit.getWorkflow().getWfAsStep(), parameter.getOutputData(), null, -1));
        workflowRuntimeInfo.setError(getError(monitorAudit.getWorkflow().getWfAsStep(), parameter.getErrorInfo()));
        workflowRuntimeInfo.setStatus(getStatus(parameter));
        workflowRuntimeInfo.setRunningTime(getRunningTime(parameter));
      } else {
        Utils.logError("Could not get parameter information for order " + monitorAudit.getOrderId(), null);
      }
      
//      executedCompensationSteps = new ArrayList<CompensationStep>();
//      workflowRuntimeInfo.setStepsRuntimeInfo(createRuntimeInfoForSteps(wf));
//      workflowRuntimeInfo.setRetryIterations(createRetryRuntimeInfos(wf));
//      auditResponse.setRollback(createRollbackRuntimeInfos(wf));
      //audit.setCatches(getCatches(wf));
    } catch (ParseException e) {
      Utils.logError("Could not parse start/stop time of main workflow", e);
    } catch (XynaException e) {
      Utils.logError("Could not get audit information", e);
      throw new NoAuditData(e);
    }
    
    return auditResponse;
  }
  
  //TODO: cleanup
  public List<? extends OrderOverviewEntry> getOrderOverviewEntries(TableInfo searchCriteria, SearchFlag onlyRootOrders) {
    return OrderOverview.getOrderOverviewEntries(multiChannelPortal, searchCriteria, false);
  }
  
  public CustomFields getCustomFieldLabels() {
    return new CustomFields(getCustomFieldLabel(0), getCustomFieldLabel(1), getCustomFieldLabel(2), getCustomFieldLabel(3));
  }

};
