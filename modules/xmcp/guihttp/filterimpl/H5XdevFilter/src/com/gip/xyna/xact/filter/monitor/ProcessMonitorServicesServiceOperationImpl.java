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
package com.gip.xyna.xact.filter.monitor;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.WorkflowUtils;
import com.gip.xyna.xact.filter.util.xo.MetaInfo;
import com.gip.xyna.xact.filter.util.xo.RuntimeContextVisitor;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.ErrorInfo;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.Parameter;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditInformation;

import xmcp.graphs.datatypes.GraphData;
import xmcp.graphs.datatypes.GraphInfo;
import xmcp.processmonitor.datatypes.Audit;
import xmcp.processmonitor.datatypes.AuditStep;
import xmcp.processmonitor.datatypes.AuditStepGroup;
import xmcp.processmonitor.datatypes.Await;
import xmcp.processmonitor.datatypes.BeginDocument;
import xmcp.processmonitor.datatypes.CancelFrequencyControlledTaskException;
import xmcp.processmonitor.datatypes.Case;
import xmcp.processmonitor.datatypes.Catch;
import xmcp.processmonitor.datatypes.Choice;
import xmcp.processmonitor.datatypes.Compensation;
import xmcp.processmonitor.datatypes.Delete;
import xmcp.processmonitor.datatypes.EndDocument;
import xmcp.processmonitor.datatypes.Error;
import xmcp.processmonitor.datatypes.Foreach;
import xmcp.processmonitor.datatypes.FrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.GraphDatasource;
import xmcp.processmonitor.datatypes.LoadFrequencyControlledTasksException;
import xmcp.processmonitor.datatypes.LoadGraphDataException;
import xmcp.processmonitor.datatypes.ManualInteraction;
import xmcp.processmonitor.datatypes.Mapping;
import xmcp.processmonitor.datatypes.NoAuditData;
import xmcp.processmonitor.datatypes.NoFrequencyControlledTaskDetails;
import xmcp.processmonitor.datatypes.Notify;
import xmcp.processmonitor.datatypes.OrderOverviewEntry;
import xmcp.processmonitor.datatypes.ParallelExecution;
import xmcp.processmonitor.datatypes.PrunedValue;
import xmcp.processmonitor.datatypes.Query;
import xmcp.processmonitor.datatypes.Retry;
import xmcp.processmonitor.datatypes.RetryExecution;
import xmcp.processmonitor.datatypes.Rollback;
import xmcp.processmonitor.datatypes.SearchFlag;
import xmcp.processmonitor.datatypes.Service;
import xmcp.processmonitor.datatypes.Store;
import xmcp.processmonitor.datatypes.TaskId;
import xmcp.processmonitor.datatypes.Template;
import xmcp.processmonitor.datatypes.Throw;
import xmcp.processmonitor.datatypes.TimeSpan;
import xmcp.processmonitor.datatypes.Wait;
import xmcp.tables.datatypes.TableInfo;
import xprc.xpce.CustomFields;
import xprc.xpce.RuntimeContext;


public class ProcessMonitorServicesServiceOperationImpl {

  public static String CUSTOM_FIELD_PROPERTY_PREFIX  = "xyna.processmonitor.customColumn";
  public static String CUSTOM_FIELD_PROPERTY_ENABLED = "enabled";
  public static String CUSTOM_FIELD_PROPERTY_LABEL   = "label";

  public static final XynaPropertyBoolean CUSTOM_COLUMN0_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 1. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 1st Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN0_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_LABEL, "Custom 1")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 1. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 1st Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "0." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN1_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 2. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 2nd Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN1_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_LABEL, "Custom 2")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 2. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 2nd Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "1." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN2_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 3. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 3rd Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN2_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_LABEL, "Custom 3")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 3. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 3rd Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "2." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyBoolean CUSTOM_COLUMN3_ENABLED = new XynaPropertyBoolean(CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die 4. Custom Column in der Auftragsübersicht mit " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL + " als Label angezeigt wird.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the 4th Custom Column is shown in Order Overview, using the label defined by " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL + ".");
  public static final XynaPropertyString CUSTOM_COLUMN3_LABEL = new XynaPropertyString(CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_LABEL, "Custom 4")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Das Label, das für die 4. Custom Column in der Order Overview angezeigt werden soll, wenn " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED + " auf true gesetzt ist.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The label to show for 4th Custom Column in Order Overview in case " + CUSTOM_FIELD_PROPERTY_PREFIX + "3." + CUSTOM_FIELD_PROPERTY_ENABLED + " is set to true.");

  public static final XynaPropertyInt ORDEROVERVIEW_LIMIT = new XynaPropertyInt("xyna.processmonitor.orderoverview.limit", 100).
      setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen, die für die Order Overview zurück gegeben wird.").
      setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of table entries to be returned for the Order Overview.");

  private static XynaMultiChannelPortal multiChannelPortal = ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal());
  private static Configuration configuration = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessMonitorServicesServiceOperationImpl.class);

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
  
  private WF getWFObject(MonitorAudit monitorAudit) throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    String mainWfFqn = monitorAudit.getWorkflowFqn();
    Map<String, String> xmlsWfAndImports = new HashMap<String, String>();
    xmlsWfAndImports.put(mainWfFqn, monitorAudit.getWorkflowXml()); // add main Workflow
    
    // add imports
    for (AuditImport curImport : monitorAudit.getEnhancedAudit().getImports()) {
      xmlsWfAndImports.putIfAbsent(curImport.getFqn(), curImport.getDocument());
    }
    
    WF wf = WF.getOrCreateInstanceForAudits(mainWfFqn, xmlsWfAndImports);
    wf.parseGeneration(false, false, false); // TODO: Parameter korrekt?
    WorkflowUtils.prepareWorkflowForMonitor(wf.getWfAsStep());
    return wf;
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
  
  
  private List<Step> collectAllSteps(Step rootStep) {
    List<Step> steps = new ArrayList<Step>();
    steps.add(rootStep);
    
    if (rootStep.getChildSteps() != null) {
      for (Step childStep : rootStep.getChildSteps()) {
        steps.addAll(collectAllSteps(childStep));
      }
    }
    
    return steps;
  }
  
  private List<Step> collectExecutedSteps(Step rootStep, List<Integer> foreachIndices, int retryCounter) {
    List<Step> executedSteps = new ArrayList<Step>();
    List<Step> allSteps = collectAllSteps(rootStep);
    for (Step step : allSteps) {
      if (step.hasBeenExecuted(foreachIndices, retryCounter)) {
        executedSteps.add(step);
      }
    }
    
    return executedSteps;
  }
  
  private List<Step> filterForGuiElements(List<Step> steps) {
    List<Step> filteredSteps = new ArrayList<Step>();
    for (Step step : steps) {
      if (step.toBeShownInAudit()) {
        filteredSteps.add(step);
      }
    }
    
    return filteredSteps;
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
  
  private List<Step> removeSubSteps(List<Step> steps) {
    List<Step> filteredSteps = new ArrayList<Step>();
    
    // recursively determine child elements
    List<Step> childrenOfToplevelElements = collectChildElements(steps);
    List<Step> childrenOfContainerElements = new ArrayList<Step>();
    for (Step step : childrenOfToplevelElements) {
      if ( (step instanceof StepChoice) || (step instanceof StepForeach) || (step instanceof StepParallel) ||
           (step instanceof StepCatch) || (step instanceof StepFunction) ) {
        childrenOfContainerElements.addAll(collectChildElements(step));
      }
    }
    
    // filter out all steps that are also child elements of other steps
    for (Step step : steps) {
      if (!childrenOfContainerElements.contains(step)) {
        filteredSteps.add(step);
      }
    }
    
    return filteredSteps;
  }
  
  private List<Step> removeQueryHelperMappings(List<Step> steps) {
    List<Step> filteredSteps = new ArrayList<Step>();
    
    // find all queries
    List<StepFunction> queries = new ArrayList<StepFunction>();
    for (Step step : steps) {
      if ( (step instanceof StepFunction) &&
           (((StepFunction)step).getOperationName().equals("query")) ) {
        queries.add((StepFunction)step);
      }
    }
    
    for (Step step : steps) {
      if (step instanceof StepMapping) { 
        // only keep mappings that are not internal helpers for the queries
        boolean isHelper = false;
        for (StepFunction query : queries) {
          // only keep mapping when it is not an input of a query
          if (isInputOfQuery((StepMapping)step, query)) {
            isHelper = true;
            break;
          }
        }
        
        if (!isHelper) {
          filteredSteps.add(step);
        }
      } else {
        filteredSteps.add(step);
      }
    }
    
    return filteredSteps;
  }
  
  private boolean isInputOfQuery(StepMapping mapping, StepFunction query) {
    for (String mappingOutputVarId : mapping.getOutputVarIds() ) {
      for (String queryInputVarId : query.getInputVarIds() ) {
        if (mappingOutputVarId.equals(queryInputVarId)) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  public String getJsonFromParameter(Step step, AVariable parameter, List<Integer> foreachIndices, int retryCounter) {
    if (parameter == null) {
      return null;
    }
    
    String json = "null";
    if (parameter != null) {
      JsonBuilder jsonBuilder = new JsonBuilder();
      jsonBuilder.startObject(); // start outer root-element
      convertParameterToJson(jsonBuilder, step, parameter, foreachIndices, retryCounter); // create inner JSON
      jsonBuilder.endObject(); // end outer root-element
      json = jsonBuilder.toString();
      
      if (json.length() == 4) { // only contains start and end with line beaks
        // create empty entry with meta-tag to let GUI know what type element is of
        jsonBuilder = new JsonBuilder();
        jsonBuilder.startObject(); // start outer root-element
        createEmptyJsonObject(jsonBuilder, step, parameter, getLabel(parameter), foreachIndices, retryCounter, false); // create inner JSON
        jsonBuilder.endObject(); // end outer root-element
        json = jsonBuilder.toString();
      }
    }
    
    return json;
  }
  
  private void createEmptyJsonObject(JsonBuilder jsonBuilder, Step step, AVariable parameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    if(isMemberVar) {
      jsonBuilder.addObjectAttribute(subObjectName);
    }
    createMetaJson(jsonBuilder, step, parameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
    if (parameter.isList()) {
      jsonBuilder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
      jsonBuilder.endList();
    }
    if(isMemberVar) {
      jsonBuilder.endObject();
    }
  }
  
  public List<String> getJsonsFromParameterList(Step step, List<AVariable> parameterList, List<Integer> foreachIndices, int retryCounter) { // TODO: zusaetzlicher Parameter In/Out
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
      return null;
    }
    
    return jsonParameterList;
  }
  
  private void convertParameterToJson(JsonBuilder jsonBuilder, Step step, AVariable parameter, List<Integer> foreachIndices, int retryCounter) {
    convertParameterToJson(jsonBuilder, step, parameter, parameter, getLabel(parameter), foreachIndices, retryCounter, false);
  }
  
  private void convertParameterToJson(JsonBuilder jsonBuilder, Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    if ( (parameter.getChildren() == null) || (parameter.getChildren().size() == 0) ) {  
      // parameter is the leaf element of the tree
      createJsonForLeafElement(jsonBuilder, step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar); // TODO: originalParameter?
    } else {
      // parameter is inner knot (or the root)
      createJsonForInnerElement(jsonBuilder, step, parameter, originalParameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
    }
  }
  
  private void createJsonForInnerElement(JsonBuilder jsonBuilder, Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    // start new json-object for element
    if(isMemberVar) {
      jsonBuilder.addObjectAttribute(subObjectName);
    }
    createMetaJson(jsonBuilder, step, parameter, originalParameter, subObjectName, foreachIndices, retryCounter, false);
    
    // recursively create JSON-elements for children
    if (parameter.isList()) {
      jsonBuilder.addListAttribute(XynaObjectVisitor.WRAPPED_LIST_TAG);
      for (AVariable member : parameter.getChildren()) {
        if (member == null) {
          jsonBuilder.addNullObject();
          continue;
        }
        
        if(!member.isJavaBaseType()) {
          jsonBuilder.startObject();
        }
        convertParameterToJson(jsonBuilder, step, member, member, member.getVarName(), foreachIndices, retryCounter, false);
        if(!member.isJavaBaseType()) {
          jsonBuilder.endObject();
        }
      }
      jsonBuilder.endList();
    } else {
      for (AVariable member : parameter.getChildren()) {
        convertParameterToJson(jsonBuilder, step, member, member, member.getVarName(), foreachIndices, retryCounter, true);
      }
    }
    if(isMemberVar) {
      jsonBuilder.endObject();
    }
  }
  
  private void createJsonForLeafElement(JsonBuilder jsonBuilder, Step step, AVariable parameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) { // TODO: AVariable originalParameter?
    if (parameter.isList()) {
      if ( (parameter.getRefInstanceId() != null) && (parameter.getRefInstanceId().length() > 0) ) {
        // parameter values are not stored directly in data element, but in a referred field
        AVariable refParameter = parameterByInstanceId.get(parameter.getRefInstanceId());
        
        if (refParameter == null) {
          writePrunedValue(jsonBuilder, parameter.getRefInstanceId());
          return;
        }
        
        convertParameterToJson(jsonBuilder, step, refParameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
      } else if ( (parameter.getVarName().length() > 0) && (!parameter.getVarName().equals("n/a")) ) {
        // write list to JSON
        if (parameter.getValues() != null) {
          jsonBuilder.addListAttribute(subObjectName);
          for (String value : parameter.getValues()) {
            if (parameter.getJavaTypeEnum() == PrimitiveType.STRING) {
              jsonBuilder.addStringListElement(value);
            } else if (parameter.getJavaTypeEnum() == PrimitiveType.INT || parameter.getJavaTypeEnum() == PrimitiveType.INTEGER || parameter.getJavaTypeEnum() == PrimitiveType.LONG || parameter.getJavaTypeEnum() == PrimitiveType.LONG_OBJ || parameter.getJavaTypeEnum() == PrimitiveType.DOUBLE || parameter.getJavaTypeEnum() == PrimitiveType.DOUBLE_OBJ) {
              if ("NaN".equals(value)) {
                jsonBuilder.addStringListElement(value);
              } else {
                jsonBuilder.addPrimitiveListElement(value);
              }
            } else {
              jsonBuilder.addPrimitiveListElement(value);
            }
          }
          jsonBuilder.endList();
        } else if (!parameter.isJavaBaseType()) {
          // create empty entry with meta-tag to let GUI know what type list is of
          createEmptyJsonObject(jsonBuilder, step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
        }
      }
    } else { // not a list
      if ( (parameter.getRefInstanceId() != null) && (parameter.getRefInstanceId().length() > 0) ) {
        // parameter value is not stored directly in data element, but in a referred field
        AVariable refParameter = parameterByInstanceId.get(parameter.getRefInstanceId());
        
        if (refParameter == null) {
          writePrunedValue(jsonBuilder, parameter.getRefInstanceId());
          return;
        }
        
        convertParameterToJson(jsonBuilder, step, refParameter, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
      } else if ( (parameter.getVarName().length() > 0) && (!parameter.getVarName().equals("n/a")) ) {
        if (parameter.getValue() != null) {
          // write value to JSON
          jsonBuilder.addStringAttribute(subObjectName, parameter.getValue());
        } else if (!parameter.isJavaBaseType()) {
          // create empty entry with meta-tag to let GUI know what type element is of
          createEmptyJsonObject(jsonBuilder, step, parameter, subObjectName, foreachIndices, retryCounter, isMemberVar);
        }
      }
    }
  }
  
  
  private void writePrunedValue(JsonBuilder builder, String instanceId) {
    Application app = Utils.getGuiHttpApplication();
    PrunedValue prunedValue = new PrunedValue.Builder().message("Value was pruned by LazyLoading. Id was " + instanceId).instance();
    builder.addObjectAttribute(XynaObjectVisitor.META_TAG);
    builder.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, prunedValue.getClass().getCanonicalName());
    builder.addObjectAttribute(MetaInfo.RUNTIME_CONTEXT);
    builder.addStringAttribute(RuntimeContextVisitor.APPLICATION_LABEL, app.getName());
    builder.addStringAttribute(RuntimeContextVisitor.VERSION_LABEL, app.getVersionName());
    builder.endObject();
    builder.endObject();
    builder.addStringAttribute("message", prunedValue.getMessage());

  }
  
  public static String TAG_ID = "$id";
  public static String TAG_SOURCE = "$source";
  
  private void createMetaJson(JsonBuilder jsonBuilder, Step step, AVariable parameter, AVariable originalParameter, String subObjectName, List<Integer> foreachIndices, int retryCounter, boolean isMemberVar) {
    // add meta-tag
    String fqn = getFqnOfAVariable(parameter, step, foreachIndices, retryCounter);
    jsonBuilder.addObjectAttribute(XynaObjectVisitor.META_TAG);
    jsonBuilder.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, fqn);
    jsonBuilder.addObjectAttribute(MetaInfo.RUNTIME_CONTEXT);
    
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext = fqnToRtcMap.get(fqn);
    if (runtimeContext == null) {
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.WORKSPACE_LABEL, RevisionManagement.DEFAULT_WORKSPACE.getName());
    } else if (runtimeContext.getType() == com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType.Workspace) {
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.WORKSPACE_LABEL, runtimeContext.getName());
    } else {
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.APPLICATION_LABEL, runtimeContext.getName());
      jsonBuilder.addStringAttribute(RuntimeContextVisitor.VERSION_LABEL, ((Application)runtimeContext).getVersionName());
    }
    jsonBuilder.endObject(); // runtime context
    jsonBuilder.endObject(); // meta tag
  }
  
  private String getFqnOfAVariable(AVariable avar, Step step, List<Integer> foreachIndices, int retryCounter) {
    if(avar.getOriginalPath() != null) {
      return avar.getOriginalPath() + "." + avar.getOriginalName();
    }
    AVariable result = null;
    Parameter p = step.getParameter(foreachIndices, retryCounter);
    int index = p.getInputData().indexOf(avar);
    if(index != -1) {
      result = step.getInputVars().get(index);
    } else {
      index = p.getOutputData().indexOf(avar);
      result = step.getOutputVars().get(index);
    }
    
    return result.getOriginalPath() + "." +result.getOriginalName();
  }
  
  // TODO: remove (see PMON-23 und PMON-119)
  @Deprecated
  private String getLabel(AVariable parameter) { 
    String label = parameter.getLabel();
    if ( (label == null) || (label.length() == 0) ) {
      try {
        // search for the variable in the workflow the parameter contains the values for
        label = parentScope.identifyVariable(parameter.getId()).getVariable().getLabel();
      } catch (Exception e) {
        // fall back to variable name
        label = parameter.getOriginalName();
      }
    }
    
    // use a non-breaking space in case the label is empty to make sure the GUI element keeps its minimal size
    return ( (label != null) && (label.length() > 0) ) ? label : " ";
  }
  
  private Map<String, AVariable> parameterByInstanceId;
  
  private void createInstanceIdLookup(AVariable parameter) {
    if ( (parameter != null) && (parameter.getInstanceId() != null) && (parameter.getInstanceId() != "") ) {
      parameterByInstanceId.put(parameter.getInstanceId(), parameter);
    }
    
    if ( (parameter != null) && (parameter.getChildren() != null) ) {
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
      for (Step step : collectAllSteps(topLevelStep)) {
        // labels of variables are not contained in the parameter-tags of the audit and need to be set separately
        step.addLabelsToParameter();
        
        // ids need to be set after parsing since referred variables might not be parsed yet while then step is being parsed
        step.addIdsToParameter();
      }
    }
  }
  
  private List<Step> filterForAudit(List<Step> steps) {
    List<Step> stepsForAudit = removeQueryHelperMappings(steps); // discard internal mappings that are used to build input of a query
    stepsForAudit = removeSubSteps(stepsForAudit); // discard steps that are sub steps of other steps (e. g. of a choice), since those will be added together with their group
    stepsForAudit = filterForGuiElements(stepsForAudit); // only keep elements that are to be shown in the GUI
    
    return stepsForAudit;
  }
  
  private List<AuditStep> createAuditSteps(WF wf) throws XynaException {
    List<Step> steps = filterForAudit( collectExecutedSteps(wf.getWfAsStep().getChildStep(), null, 0) );
    return createAuditSteps(steps, null, 0);
  }
  
  private List<AuditStepGroup> createRetrySteps(WF wf) throws XynaException {
    List<Step> topLevelSteps = wf.getWfAsStep().getChildSteps();
    if (topLevelSteps.size() == 1) {
      return null; // no catch, hence no retries
    } 
    
    if (!(topLevelSteps.get(1) instanceof StepCatch)) {
      // second top-level step is of unexpected type
      Utils.logError("second top-level step is of unexpected type " + topLevelSteps.get(1).getClass() + " (StepCatch was expected)", null);
      return null;
    }
    
    StepCatch wfCatch = (StepCatch)topLevelSteps.get(1);
    if (!wfCatch.hasRetry()) {
      // workflow has an executed globally caught exception without a retry 
      return null;
    }
    
    // workflow has an executed retry
    List<AuditStepGroup> retries = new ArrayList<AuditStepGroup>();
    Pair<Integer, Integer> retryCounterRange = wfCatch.getRetryCounterRange();
    for (int retryCounter = retryCounterRange.getFirst() + 1; // first index is initial run of workflow, following indices are retries
         retryCounter < retryCounterRange.getSecond() + 1; retryCounter++) {
      AuditStepGroup retry = new AuditStepGroup();
      List<Step> stepsCurIteration = filterForAudit( collectExecutedSteps(wfCatch.getStepInTryBlock(), null, retryCounter) );
      
      retry.setAuditSteps( createAuditSteps(stepsCurIteration, null, retryCounter) );
      retry.setCatches( getCatches(wfCatch, null, retryCounter) );
      retries.add(retry);
    }
    
    return retries;
  }
  
  private List<AuditStep> createAuditSteps(List<Step> steps, List<Integer> foreachIndices, int retryCounter) throws XynaException {
    List<AuditStep> auditSteps = new ArrayList<AuditStep>();
    for (Step step : steps) {
      AuditStep auditStep = createAuditStep(step, foreachIndices, retryCounter);
      if (auditStep != null) {
        auditSteps.add(auditStep);
      }
    }
    
    return (auditSteps.size() > 0) ? auditSteps : null;
  }
  
  private AuditStep createAuditStep(Step step, List<Integer> foreachIndices, int retryCounter) {
    if (isSimpleStep(step)) {
      return createSimpleAuditStep(step, foreachIndices, retryCounter);
    } else if (isStepGroup(step)) {
      return createAuditStepGroup(step, foreachIndices, retryCounter);
    } else if (isForeach(step)) {
      return createForeachStep(step, foreachIndices, retryCounter);
    } else if (isChoice(step)) {
      return createChoiceStep(step, foreachIndices, retryCounter);
    } else if (isCaughtStep(step)) {
      return createCaughtStep(step, foreachIndices);
    }
    
    return null;
  }
  
  private List<Catch> getCatches(WF wf) {
    StepCatch wfCatch = null;
    for (Step topLevelStep : wf.getWfAsStep().getChildSteps()) {
      if (topLevelStep instanceof StepCatch) {
        wfCatch = (StepCatch)topLevelStep;
        break;
      }
    }
    
    if (wfCatch == null) {
      return null;
    }
    
    return getCatches(wfCatch, null, 0); // only store catch for initial workflow run (catches of retries are stored in audit.retries, see createRetrySteps)
  }
  
  List<CompensationStep> executedCompensationSteps;
  
  private Rollback createRollback(WF wf) throws XynaException {
    if (executedCompensationSteps.size() == 0) {
      return null;
    }
    
    List<AuditStep> rollbackSteps = new ArrayList<AuditStep>();
    for (int stepNo = executedCompensationSteps.size()-1; stepNo >= 0; stepNo--) { // compensation steps are in reverse order
      CompensationStep compensationStep = executedCompensationSteps.get(stepNo);
      AuditStep auditStep = createAuditStepGroup(compensationStep.getStep(), compensationStep.getForeachIndices(), compensationStep.getRetryCounter());
      rollbackSteps.add(auditStep);
    }
    
    Rollback rollback = new Rollback();
    rollback.setLabel("Rollback");
    rollback.setAuditSteps(rollbackSteps);
    rollback.setStatus(rollbackSteps.get(rollbackSteps.size()-1).getStatus());
    
    // set run time
    CompensationStep firstCompensationStep = executedCompensationSteps.get(executedCompensationSteps.size()-1);
    CompensationStep lastCompensationStep = executedCompensationSteps.get(0);
    try {
      rollback.setRunTime(new TimeSpan(firstCompensationStep.getStep().getStartTime(), lastCompensationStep.getStep().getStopTime()));
    } catch (ParseException e) {
      Utils.logError("Could not set start/end time for rollback", e);
    }
    
    return rollback;
  }
  
  private boolean isSimpleStep(Step step) {
    return ( (step instanceof StepFunction) || (step instanceof StepMapping) || (step instanceof StepThrow) || (step instanceof StepRetry) ); // TODO: ohne instanceof
  }
  
  private boolean isStepGroup(Step step) {
    return ( (step instanceof StepParallel) || (step instanceof StepSerial) ); // TODO: ohne instanceof
  }
  
  private boolean isForeach(Step step) {
    return (step instanceof StepForeach);
  }
  
  private boolean isCaughtStep(Step step) {
    return (step instanceof StepCatch);
  }
  
  private boolean isChoice(Step step) {
    return (step instanceof StepChoice);
  }
  
  private AuditStep createSimpleAuditStep(Step step, List<Integer> foreachIndices, int retryCounter) {
    Parameter parameter = step.getParameter(foreachIndices, retryCounter);
    return createSimpleAuditStep(step, parameter);
  }
  
  private ScopeStep parentScope = null;
  
  private AuditStep createSimpleAuditStep(Step step, Parameter parameter) {
    AuditStep auditStep;
    if (step instanceof StepFunction) {
      StepFunction stepFunction = ((StepFunction)step);
      String fqn = getFqn(stepFunction);
      
      if (        (fqn.equals("xmcp.manualinteraction.ManualInteraction")) && (stepFunction.getOperationName().equals("ManualInteraction")) ) {
        auditStep = new ManualInteraction();
      } else if ( (fqn.equals("xnwh.persistence.PersistenceServices"))     && (stepFunction.getOperationName().equals("store")) ) {
        auditStep = new Store();
      } else if ( (fqn.equals("xnwh.persistence.PersistenceServices"))     && (stepFunction.getOperationName().equals("query")) ) {
        auditStep = new Query();
      } else if ( (fqn.equals("xnwh.persistence.PersistenceServices"))     && (stepFunction.getOperationName().equals("delete")) ) {
        auditStep = new Delete();
      } else if ( (fqn.equals("xact.templates.TemplateManagement"))        && (stepFunction.getOperationName().equals("start")) ) {
        auditStep = new BeginDocument();
      } else if ( (fqn.equals("xact.templates.TemplateManagement"))        && (stepFunction.getOperationName().equals("stop")) ) {
        auditStep = new EndDocument();
      } else if ( (fqn.equals("xprc.waitsuspend.WaitAndSuspendFeature"))   && (stepFunction.getOperationName().equals("wait")) ) {
        auditStep = new Wait();
      } else if ( (fqn.equals("xprc.waitsuspend.WaitAndSuspendFeature"))   && (stepFunction.getOperationName().equals("suspend")) ) {
        auditStep = new Wait();
        ((Wait)auditStep).setFreeCapacities(true);
      } else if ( (fqn.equals("xprc.synchronization.Synchronization"))     && (stepFunction.getOperationName().equals("awaitNotification")) ) {
        auditStep = new Await();
      } else if ( (fqn.equals("xprc.synchronization.Synchronization"))     && (stepFunction.getOperationName().equals("longRunningAwait")) ) {
        auditStep = new Await();
        ((Await)auditStep).setFreeCapacities(true);
      } else if ( (fqn.equals("xprc.synchronization.Synchronization"))     && (stepFunction.getOperationName().equals("notifyWaitingOrder")) ) {
        auditStep = new Notify();
      } else {
        auditStep = new Service();
      }
      
      ((Service)auditStep).setOrderId(parameter.getInstanceId());
      ((Service)auditStep).setIsDetached(stepFunction.isExecutionDetached());
      ((Service)auditStep).setFqn(fqn);
      
      // if step has an executed compensation, it is stored to be used later during call of method createRollback(...) 
      List<Step> childSteps = stepFunction.getChildSteps();
      if (stepFunction.getChildSteps().size() > 0) {
        Step compensationStep = childSteps.get(0);
//        if (compensationStep.hasBeenExecuted()) {
        if (compensationStep.hasBeenExecuted(parameter.getForeachIndices(), parameter.getRetryCounter())) {
          executedCompensationSteps.add(new CompensationStep(stepFunction, parameter.getForeachIndices(), parameter.getRetryCounter()));
        }
      }
    } else if (step instanceof StepMapping) {
      if ( !((StepMapping)step).isTemplateMapping() ) {
        auditStep = new Mapping();
      } else {
        auditStep = new Template();
      }
    } else if (step instanceof StepThrow) {
      auditStep = new Throw();
    } else if (step instanceof StepRetry) {
      auditStep = new Retry();
      auditStep.setLabel("Retry");
    } else {
      return null;
    }
    
    if (auditStep.getLabel() == null) {
      auditStep.setLabel(getLabel(step, parameter));
    }
    
    auditStep = setRunTimeFromParameter(auditStep, parameter);
    auditStep.setStatus(getStatus(parameter));
    
    return setBasicData(step, auditStep, parameter);
  }
  
  private String getFqn(StepFunction stepFunction) {
    try {
      ServiceIdentification serviceIdentification = stepFunction.getParentScope().identifyService(stepFunction.getServiceId());
      return serviceIdentification.service.getOriginalFqName();
    } catch (XPRC_InvalidServiceIdException e) {
      Utils.logError("Could not determine fqn for " + stepFunction.getOperationName(), e);
    }
    
    return null;
  }
  
  private AuditStep setRunTimeFromParameter(AuditStep auditStep, Parameter parameter) {
    try {
      auditStep.setRunTime(getRunTime(parameter));
    } catch (ParseException e) {
      Utils.logError("Could not convert start/end time to unix time: " + parameter.getInputTimeStamp() + " - " + parameter.getOutputTimeStamp(), e);
    }
    
    return auditStep;
  }
  
  private AuditStep setRunTime(AuditStep auditStep, Step step) {
    try {
      auditStep.setRunTime(new TimeSpan(step.getStartTime(), step.getStopTime()));
    } catch (ParseException e) {
      Utils.logError("Could convert start/end time to unix time for steps " + step.getLabel() + " and " + step.getLabel(), e);
    }
    
    return auditStep;
  }
  
  private AuditStep setBasicData(Step step, AuditStep auditStep, Parameter parameter) {
    if(parameter != null) {
      auditStep.setInputs(getJsonsFromParameterList(step, parameter.getInputData(), parameter.getForeachIndices(), parameter.getRetryCounter()));
      auditStep.setOutputs(getJsonsFromParameterList(step, parameter.getOutputData(), parameter.getForeachIndices(), parameter.getRetryCounter()));
      auditStep.setError(getError(step, parameter.getErrorInfo()));
    }
    
    return auditStep;
  }
  
  private String getLabel(Step step, Parameter parameter) {
    try {
      return (step.getLabel());
    } catch (UnsupportedOperationException e) {
      if (parameter != null && parameter.getErrorInfo() != null) {
        return parameter.getErrorInfo().getExceptionVariable().getOriginalName();
      } else {
        return null;
      }
    }
  }
  
  private TimeSpan getRunTime(Parameter parameter) throws ParseException {
    if(parameter == null) {
      return null;
    }
    if (parameter.getErrorInfo() == null) {
      return new TimeSpan(parameter.getInputTimeStampUnix(), parameter.getOutputTimeStampUnix());
    } else {
      return new TimeSpan(parameter.getInputTimeStampUnix(), parameter.getErrorTimeStampUnix());
    }
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
  
  private Error getError(MonitorAudit monitorAudit) {
    List<XynaExceptionInformation> exceptions = monitorAudit.getExceptions();
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
    if(parameter == null) {
      return OrderInstanceGuiStatus.UNKNOWN.getName();
    }
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
  
  private AuditStepGroup createSpecificStepGroup(Step step, List<Step> executedBranches) {
    if (step instanceof StepParallel) {
      ParallelExecution parallelExecution = new ParallelExecution();
      parallelExecution.setLabel("Parallel Execution");
      
      return parallelExecution;
    } else if (step instanceof StepFunction) {
      // StepFunction contains compensation steps
      Compensation compensation = new Compensation();
      compensation.setLabel(step.getLabel());
      
      return compensation;
    } else if (step.getParentStep() instanceof StepCatch) {
      Catch catchBlock = new Catch();
      catchBlock.setLabel("Catch");
      
      return catchBlock;
    } else {
      return new AuditStepGroup();
    }
  }
  
  private AuditStepGroup createAuditStepGroup(Step step, List<Integer> foreachIndices, int retryCounter) {
    // determine which branch has been executed
    List<Step> executedBranches = determineExecutedBranches(step, foreachIndices, retryCounter);
    
    // add steps of executed branches to step group
    AuditStepGroup auditStepGroup = createSpecificStepGroup(step, executedBranches);
    for (Step executedBranch : executedBranches) {
      AuditStep branch = createAuditStep(executedBranch, foreachIndices, retryCounter);
      if (branch != null) {
        auditStepGroup.addToAuditSteps(branch);
      }
    }
    
    // set inputs and outputs if available for this group
    Parameter parameter = step.getParameter(foreachIndices, retryCounter);
    if (parameter != null) {
      auditStepGroup = (AuditStepGroup)setBasicData(step, auditStepGroup, parameter);
    }
    
    auditStepGroup = (AuditStepGroup)setRunTime(auditStepGroup, step);
    auditStepGroup.setStatus(getWorstStatus(auditStepGroup.getAuditSteps()));
    
    return auditStepGroup;
  }
  
  private AuditStep createForeachStep(Step step, List<Integer> iterationIndices, int retryCounter) {
    Foreach foreach = new Foreach();
    foreach.setLabel("Foreach");
    StepForeach stepForeach = (StepForeach)step;
    foreach.setParallelExecution(stepForeach.getParallelExecution());
    for (AVariable inputVariable : stepForeach.getInputVarsSingle()) {
      if ( (inputVariable.getVarName().length() > 0) && (!inputVariable.getVarName().equals("n/a")) ) {
        foreach.addToVariables(inputVariable.getLabel());
      } else {
        // parameter value is not stored directly in data element, but in a referred field
        AVariable refParameter = parameterByInstanceId.get(inputVariable.getRefInstanceId());
        foreach.addToVariables(refParameter.getLabel());
      }
    }
    
    List<Step> childSteps = step.getChildSteps().get(0).getChildSteps().get(0).getChildSteps();
    if (childSteps.size() > 1) {
      Utils.logError("Foreach loop over a group of steps is not supported.", null);
      // TODO: Exception werfen
    }
    
    Step stepToIterate = childSteps.get(0);
    Step stepWithParameterList = stepToIterate;
    
    if (stepToIterate instanceof StepForeach) {
      // foreach contains a nested foreach
      
      // search step with parameter list for iterations (located in deepest nested foreach)
      while (stepWithParameterList.getParameterList() == null) {
        stepWithParameterList = stepWithParameterList.getChildSteps().get(0).getChildSteps().get(0).getChildSteps().get(0);
      }
      
      // determine number of iterations for current nesting depth
      int iterationDepth = (iterationIndices == null) ? 0 : iterationIndices.size();
      int iterationCount = 0;
      for (Parameter parameter : stepWithParameterList.getParameterList()) {
        int subIterationNo = parameter.getForeachIndices().get(iterationDepth);
        if (subIterationNo+1 > iterationCount) {
          iterationCount = subIterationNo+1;
        }
      }
      
      for (int subIterationNo = 0; subIterationNo < iterationCount; subIterationNo++) {
        // clone iteration indices list to add current iteration number for processing of next iteration
        List<Integer> subIterationIndices = new ArrayList<Integer>();
        if (iterationIndices != null) {
          for (int iterationIndex : iterationIndices) {
            subIterationIndices.add(iterationIndex);
          }
        }
        
        subIterationIndices.add(subIterationNo);
        foreach.addToIterations(createForeachStep(stepToIterate, subIterationIndices, retryCounter));
      }
    } else {
      // foreach iterates over a single step
      for (Parameter parameter : stepWithParameterList.getParameterList()) {
        if ( (isSubIterationOf(parameter.getForeachIndices(), iterationIndices)) && (parameter.getRetryCounter() == retryCounter) ) {
          AuditStep auditStep = createAuditStep(stepToIterate, parameter.getForeachIndices(), parameter.getRetryCounter());
          if (auditStep != null) {
            foreach.addToIterations(auditStep);
          }
        }
      }
    }
    
    try {
      long startTimeFirstStep = stepWithParameterList.getParameterList().get(0).getInputTimeStampUnix();
      long stopTimeLastStep = stepWithParameterList.getParameterList().get( stepWithParameterList.getParameterList().size()-1 ).getOutputTimeStampUnix();
      foreach.setRunTime(new TimeSpan(startTimeFirstStep, stopTimeLastStep));
    } catch (ParseException e) {
      Utils.logError("Could not parse start/stop time of foreach", e);
    }
    
    foreach.setStatus(getWorstStatus(foreach.getIterations()));
    
    return foreach;
  }
  
  private Choice createChoiceStep(Step step, List<Integer> foreachIndices, int retryCounter) {
    Choice choice = new Choice();
    choice.setLabel("Choice");
    StepChoice stepChoice = (StepChoice)step;
    choice.setCondition(stepChoice.getOuterCondition());
    
    // determine which branch has been executed
    List<Step> executedBranches = determineExecutedBranches(step, foreachIndices, retryCounter);
    
    if (executedBranches.size() > 0) {
      // add steps of executed case
      Step executedCase = executedBranches.get(0);
      AuditStep executedAuditStep = createAuditStep(executedCase, foreachIndices, retryCounter);
      Case caseAuditStep;
      if (executedAuditStep instanceof AuditStepGroup) {
        // unwrap group
//        AuditStepGroup group = (AuditStepGroup)executedAuditStep;
//        caseAuditStep = new Case(group.getStatus(), group.getLabel(), group.getInputs(), group.getOutputs(),
//                                 group.getError(), group.getRunTime(), group.getAuditSteps(), null, null);
      } else {
        // add single step to case
        caseAuditStep = new Case();
        caseAuditStep.addToAuditSteps(executedAuditStep);
      }
      
      // set value of choice-condition for executed case
//      CaseInfo caseInfo = stepChoice.getCaseInfo(executedCase);
//      if (stepChoice.getDistinctionType() == DistinctionType.ConditionalBranch) {
//        caseAuditStep.setValue(caseInfo.getComplexName()); // TODO: schoenere Darstellung des Wertes
//        caseAuditStep.setLabel(caseInfo.getName());
//      } else if (stepChoice.getDistinctionType() == DistinctionType.ConditionalChoice) {
//        caseAuditStep.setValue(caseInfo.getName());
//      } else { // DistinctionType.TypeChoice
//        caseAuditStep.setValue(caseInfo.getName());
//      }
//      
//      // add executed case to choice
//      choice.addToCases(caseAuditStep);
    }
    
    // set inputs and outputs if available
    Parameter parameter = step.getParameter(foreachIndices, retryCounter);
    if (parameter != null) {
      choice = (Choice)setBasicData(step, choice, parameter);
    }
    
    choice = (Choice)setRunTime(choice, step);
    choice.setStatus(getWorstStatus(choice.getCases()));
    
    return choice;
  }
  
  private AuditStep createCaughtStep(Step step, List<Integer> foreachIndices) {
    StepCatch stepCatch = ((StepCatch)step);
    if (stepCatch.hasRetry()) {
      RetryExecution retry = new RetryExecution();
      retry.setLabel("Retry");
      
      Pair<Integer, Integer> retryCounterRange = stepCatch.getRetryCounterRange();
      for (int retryCounter = retryCounterRange.getFirst(); retryCounter < retryCounterRange.getSecond()+1; retryCounter++) {
        Service service = createServiceWithCatchBlock(stepCatch, foreachIndices, retryCounter);
        retry.addToIterations(service);
      }
      
      // status of retry is status of last iteration
      List<? extends AuditStep> iterations = retry.getIterations();
      retry.setStatus(iterations.get(iterations.size()-1).getStatus());
      
      // run time of retry is from start time of first iteration to stop time of last iteration
      AuditStep firstIteration = iterations.get(0);
      AuditStep lastIteration = iterations.get(iterations.size()-1);
      retry.setRunTime(new TimeSpan(firstIteration.getRunTime().getStart(), lastIteration.getRunTime().getStop()));
      
      return retry;
    } else {
      return createServiceWithCatchBlock(stepCatch, foreachIndices, -1);
    }
  }
  
  private Service createServiceWithCatchBlock(StepCatch stepCatch, List<Integer> foreachIndices, int retryCounter) {
    Parameter parameterForService = stepCatch.getParameter(foreachIndices, 0);
    
    Service service = (Service)setBasicData(stepCatch, new Service(), parameterForService);
    service.setFqn(getFqn((StepFunction)stepCatch.getStepInTryBlock()));
    service.setIsDetached(stepCatch.isExecutionDetached());
    service = (Service)setRunTimeFromParameter(service, parameterForService);
    if(parameterForService != null) {
      service.setOrderId(parameterForService.getInstanceId());
    }
    service.setLabel(getLabel(stepCatch.getStepInTryBlock(), parameterForService));
    service.setCatches(getCatches(stepCatch, foreachIndices, retryCounter));
    service.setStatus(getStatus(parameterForService));
    
    return service;
  }
  
  private List<Catch> getCatches(StepCatch stepCatch, List<Integer> foreachIndices, int retryCounter) {
    if (stepCatch == null) {
      return null;
    }
    
    Step executedCatch = stepCatch.getExecutedCatch(foreachIndices, retryCounter);
    if (executedCatch == null) {
      return null;
    }
    
    // error occurred and has been handled in catch
    Parameter parameterExecutedCatch = executedCatch.getParameter(foreachIndices, retryCounter);
    Catch catchBlock = (Catch)createAuditStepGroup(executedCatch, foreachIndices, retryCounter);
    catchBlock.addToExceptions(parameterExecutedCatch.getInputData().get(0).getOriginalName());
    List<Catch> catches = new ArrayList<Catch>();
    catches.add(catchBlock);
    
    return catches;
  }
  
  private List<Step> determineExecutedBranches(Step step, List<Integer> foreachIndices, int retryCounter) {
    List<Step> executedBranches = new ArrayList<Step>();
    for (Step branch : step.getChildSteps()) {
      if (branch.hasBeenExecuted(foreachIndices, retryCounter)) {
        executedBranches.add(branch);
      }
    }
    
    return executedBranches;
  }
  
  private boolean isSubIterationOf(List<Integer> subIteration, List<Integer> superIteration) {
    int superIterationSize = (superIteration == null) ? 0 : superIteration.size();
    if (subIteration.size() < superIterationSize) {
      return false;
    }
    
    for (int iterationDepth = 0; iterationDepth < superIterationSize; iterationDepth++) {
      if (!subIteration.get(iterationDepth).equals(superIteration.get(iterationDepth))) {
        return false;
      }
    }
    
    return true;
  }
  
  private String getWorstStatus(List<? extends AuditStep> auditSteps) {
    OrderInstanceGuiStatus status = OrderInstanceGuiStatus.FINISHED;
    if (auditSteps == null) {
      return status.getName();
    }
    
    for (AuditStep auditStep : auditSteps) {
      OrderInstanceGuiStatus statusCurrentStep = OrderInstanceGuiStatus.createOrderInstanceGuiStatus(auditStep.getStatus());
      if (statusCurrentStep.getSeverity() > status.getSeverity()) {
        status = statusCurrentStep;
      }
    }
    
    return status.getName();
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
  
  private String getCustomFieldLabel(int fieldIndex) {
    if (isCustomFieldEnabled(fieldIndex)) {
      XynaPropertyWithDefaultValue labelProperty = configuration.getPropertyWithDefaultValue(CUSTOM_FIELD_PROPERTY_PREFIX + fieldIndex + "." + CUSTOM_FIELD_PROPERTY_LABEL);
      return (labelProperty != null) ? labelProperty.getValueOrDefValue() : null;
    } else {
      return null;
    }
  }
  
  
  //TODO: some duplication with getAudit
  public WF getWorkflowForAudit(MonitorAudit monitorAudit) {
    WF wf = null;
    
    Audit audit = new Audit();
    audit.setOrderId(monitorAudit.getOrderId());
    
    try {
      audit.setRootRtc(getRuntimeContext(monitorAudit.getRuntimeContext()));
      audit.setParentOrderId(monitorAudit.getParentId());
      
      AuditInformation auditInformation = getAuditInformation(monitorAudit);
      if (auditInformation == null) {
        throw new RuntimeException(" not found");
      }
      
      wf = getWFObject(monitorAudit);
      addAdditionalDataToParams(wf);
    }
    catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    return wf;
  }
  
  public Audit getAudit(MonitorAudit monitorAudit) throws NoAuditData {
    parameterByInstanceId = new HashMap<String, AVariable>();
    
    Audit audit = new Audit();
    audit.setOrderId(monitorAudit.getOrderId());
    
    try {
      audit.setRootRtc(getRuntimeContext(monitorAudit.getRuntimeContext()));
      audit.setParentOrderId(monitorAudit.getParentId());
      
      AuditInformation auditInformation = getAuditInformation(monitorAudit);
      if (auditInformation == null) {
        // An error occured before the order entered the execution phase
        audit.setFqn(monitorAudit.getOrderType());
        audit.setLabel(monitorAudit.getOrderType());
        audit.setError(getError(monitorAudit));
        audit.setStatus(OrderInstanceGuiStatus.FAILED.getName());
        audit.setRunTime(new TimeSpan(monitorAudit.getStartTime(), monitorAudit.getLastUpdate()));
        
        return audit;
      }
      
      WF wf = getWFObject(monitorAudit);
      addAdditionalDataToParams(wf);
      parentScope = wf.getWfAsStep();
      createRtcLookup(auditInformation.getAudit().getImports()); // create lookup that translates from fqn to rtc to be used when determining the rtc for element
      createInstanceIdLookup(wf.getWfAsStep()); // create lookup that translates from InstanceId to parameter to be used when filling parameter that refer to other elements
      
      audit.setFqn(auditInformation.getAudit().getFqn());
      audit.setLabel(wf.getLabel());
      
      Parameter parameter = wf.getWfAsStep().getFirstParameter();
      if (parameter != null) {
        audit.setInputs(getJsonsFromParameterList(wf.getWfAsStep(), parameter.getInputData(), null, -1));
        audit.setOutputs(getJsonsFromParameterList(wf.getWfAsStep(), parameter.getOutputData(), null, -1));
        audit.setError(getError(wf.getWfAsStep(), parameter.getErrorInfo()));
        audit.setStatus(getStatus(parameter));
        audit.setRunTime(getRunTime(parameter));
      } else {
        Utils.logError("Could not get parameter information for order " + monitorAudit.getOrderId(), null);
      }
      
      executedCompensationSteps = new ArrayList<CompensationStep>();
      audit.setAuditSteps(createAuditSteps(wf));
      audit.setRetries(createRetrySteps(wf));
      audit.setRollback(createRollback(wf));
      audit.setCatches(getCatches(wf));
    } catch (ParseException e) {
      Utils.logError("Could not parse start/stop time of main workflow", e);
    } catch (XynaException e) {
      Utils.logError("Could not get audit information", e);
      throw new NoAuditData(e);
    }
    
    return audit;
  }
  
  //TODO: cleanup
  public List<? extends OrderOverviewEntry> getOrderOverviewEntries(TableInfo searchCriteria, SearchFlag onlyRootOrders) {
    return OrderOverview.getOrderOverviewEntries(multiChannelPortal, searchCriteria, false);
  }
  
  public CustomFields getCustomFieldLabels() {
    return new CustomFields(getCustomFieldLabel(0), getCustomFieldLabel(1), getCustomFieldLabel(2), getCustomFieldLabel(3));
  }

};
