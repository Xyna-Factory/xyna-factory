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

package com.gip.xyna.xact.filter.monitor;



import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.monitor.MonitorSession.MonitorSessionInstance;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer.MissingImport;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer.MissingImportRestorationResult;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.WorkflowUtils;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.EnhancedAudit;





public class MonitorAudit {
  
  public static final String IMPORTED = "Imported ";
  
  private static final Logger logger = CentralFactoryLogging.getLogger(MonitorAudit.class);

  private static XynaMultiChannelPortal multiChannelPortal =
      ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal());
  
  private static FileManagement fileManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  
  private static final DateTimeFormatter dateformatterDot = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
  private static final DateTimeFormatter dateformatterDash = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final DateTimeFormatter[] dateformatters = {
      dateformatterDot,
      dateformatterDash
  };

  private Long orderId;
  private Long parentId = -1L;
  private boolean imported = false;
  private boolean hasAuditData = false;
  private RuntimeContext runtimeContext;
  private String destinationType;
  private Long startTime;
  private Long lastUpdate;
  private String status;
  private Integer priority;
  private Integer monitoringLevel;

  private String auditDataXml;
  private String orderType;
  private String executionType;
  private List<XynaExceptionInformation> exceptions = Collections.emptyList();

  private String custom0;
  private String custom1;
  private String custom2;
  private String custom3;

  private String guiOrderId;
  private String guiParentOrderId;
  
  private String workfowFqn;
  private String workfowName;
  
  private EnhancedAudit enhancedAudit;
  
  private WF workflow;
  private GenerationBaseObject workflowGbo;
  
  private List<MissingImport> missingImports;
  
  public static MonitorAudit fromUploadByOrderId(MonitorSessionInstance session, Long orderId) throws XynaException {
    String fileId = session.getOrderIdToFileIdMap().get(orderId);
    if(fileId != null) {
      return fromUpload(session, fileId);
    }
    throw new Ex_FileAccessException("Upload for orderId " + orderId);
  }
  
  public static MonitorAudit fromUpload(MonitorSessionInstance session, String fileId) throws XynaException {
    MonitorAudit a = new MonitorAudit();
    a.imported = true;
    
    String completeXml;
    String filteredXml;
    try {
      completeXml = Files.readString(Path.of((fileManagement.getAbsolutePath(fileId))));
      filteredXml = AuditPreprocessing.filterAudit(completeXml);
      MissingImportRestorationResult restoreResult = AuditPreprocessing.restoreMissingImports(filteredXml);
      filteredXml = restoreResult.getFilteredXml();
      a.setMissingImports(restoreResult.getRestoredImports());
    } catch (TransformerFactoryConfigurationError | Exception e) {
      throw new RuntimeException("Could not filter audit. ", e);
    }
    
    
    Document document = XMLUtils.parseString(filteredXml);
    a.auditDataXml = XMLUtils.getXMLString(document.getDocumentElement(), false);
    
    Element orderIdElement = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.ORDER_ID);
    if(orderIdElement != null) {
      a.orderId = Long.valueOf(XMLUtils.getTextContent(orderIdElement));
      a.guiOrderId = IMPORTED + a.orderId;
      
      if(a.orderId != null) {
        session.getOrderIdToFileIdMap().put(a.orderId, fileId);
      }
    }
    
    Element parentOrderIdElement = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.PARENT_ID);
    if(parentOrderIdElement != null) {
      Long parentId = Long.valueOf(XMLUtils.getTextContent(parentOrderIdElement));
      a.parentId = parentId >= 0 ? parentId : 0;
      a.guiParentOrderId = IMPORTED + (a.parentId >= 0 ? a.parentId : 0);
    }
    
    
    Element applicationElement = XMLUtils.getChildElementByName(document.getDocumentElement(), EL.APPLICATION);
    Element versionElement = XMLUtils.getChildElementByName(document.getDocumentElement(), EL.APPLICATION_VERSION);
    Element workspaceElement = XMLUtils.getChildElementByName(document.getDocumentElement(), EL.WORKSPACE);
    a.runtimeContext = getRuntimeContext(applicationElement != null ? XMLUtils.getTextContent(applicationElement) : null,
                                         versionElement != null ? XMLUtils.getTextContent(versionElement) : null,
                                         workspaceElement != null ? XMLUtils.getTextContent(workspaceElement) : null);
    
    Element destinationType = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.DESTINATION_TYPE);
    if(destinationType != null) {
      a.executionType = getExecutionType(XMLUtils.getTextContent(destinationType)).name();
    } else {
      a.executionType = ExecutionType.UNKOWN.name();
    }
    
    Element destination = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.DESTINATION);
    if(destination != null) {
      a.orderType = XMLUtils.getTextContent(destination);
    }
    
    Element startTimeElement = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.START_TIME); // 2020.03.25 07:39:50
    if(startTimeElement != null) {
      LocalDateTime startTime = LocalDateTime.from(parseDateTime(XMLUtils.getTextContent(startTimeElement)));
      a.startTime = Timestamp.valueOf(startTime).getTime();
    }
    
    Element lastInteractionElement = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.LAST_INTERACTION); // 2020.03.25 07:39:50
    if(lastInteractionElement != null) {
      LocalDateTime lastInteraction = LocalDateTime.from(parseDateTime(XMLUtils.getTextContent(lastInteractionElement)));
      a.lastUpdate = Timestamp.valueOf(lastInteraction).getTime();
    }
    
    Element status = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.STATUS);
    if(status != null) {
      a.status = XMLUtils.getTextContent(status);
    }
    
    Element priority = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.PRIORITY);
    if(priority != null) {
      a.priority = Integer.valueOf(XMLUtils.getTextContent(priority));
    }
    
    Element monitoringLevel = XMLUtils.getChildElementByName(document.getDocumentElement(), ExportAuditProcessor.EL.MONITORING_LEVEL);
    if(monitoringLevel != null) {
      a.monitoringLevel = Integer.valueOf(XMLUtils.getTextContent(monitoringLevel));
    }
    
    Element audit = XMLUtils.getChildElementByName(document.getDocumentElement(), EL.AUDIT);
    if(audit != null) {
      
      a.hasAuditData = true;
            
      Element service = XMLUtils.getChildElementByName(audit, EL.SERVICE);
      if(service != null) {
        a.workfowFqn = getFqn(service);
        a.workfowName = service.getAttribute(ATT.TYPENAME);
      }
    }
    
    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    a.enhancedAudit = xmlHelper.auditFromXml(a.auditDataXml, false);
    
    a.workflow = createWFObject(a);
    a.workflowGbo = new GenerationBaseObject(new FQName(), a.workflow, new XMOMLoader());
    
    return a;
  }
  

  private static TemporalAccessor parseDateTime(String input) {
    DateTimeFormatter df;
    for (int i = 0; i < dateformatters.length; i++) {
      df = dateformatters[i];
      try {
        return df.parse(input);
      } catch (DateTimeParseException e) {
        continue; //try different format
      }
    }
    throw new RuntimeException("Unsupported DateTimeFormat: " + input);
  }
  
  
  public static MonitorAudit fromLocalOrder(long orderId) throws XynaException {
  
    return fromLocalOrder(orderId, t -> {
      try {
        return AuditPreprocessing.filterAudit(t);
      } catch (ParserConfigurationException | SAXException | TransformerException | TransformerFactoryConfigurationError e) {
        throw new RuntimeException(e);
      }
    });
  }
  
  
  public static MonitorAudit fromLocalOrder(long orderId, Function<String, String> filter) throws XynaException {
    OrderInstanceDetails details = multiChannelPortal.getOrderInstanceDetails(orderId);
    MonitorAudit a = new MonitorAudit();
    a.imported = false;
    a.orderId = details.getId();
    a.guiOrderId = String.valueOf(details.getId());
    a.runtimeContext = details.getRuntimeContext();
    a.orderType = details.getOrderType();
    a.startTime = details.getStartTime();
    a.lastUpdate = details.getLastUpdate();
    a.status = details.getStatusAsString();
    a.priority = details.getPriority();
    try {
      a.monitoringLevel = details.getMonitoringLevel();
    } catch (NullPointerException npe) {
      Utils.logError("NullPointerException at getMonitoringLevel()", null);
    }
    a.parentId = details.getParentId();
    a.guiParentOrderId = String.valueOf(a.parentId >= 0 ? a.parentId : 0);
    a.executionType = details.getExecutionType();
    a.exceptions = details.getExceptions();
    a.custom0 = details.getCustom0();
    a.custom1 = details.getCustom1();
    a.custom2 = details.getCustom2();
    a.custom3 = details.getCustom3();
    
    if(details.getAuditDataAsXML() != null && !details.getAuditDataAsXML().isEmpty()) {
      a.hasAuditData = true;
      try {
        a.auditDataXml = filter.apply(details.getAuditDataAsXML());
      } catch (Exception e) {
        Utils.logError("Audit prefiltering to avoid risk of OOM failed", e);
      }

      AuditXmlHelper xmlHelper = new AuditXmlHelper();
      a.enhancedAudit = xmlHelper.auditFromXml(a.auditDataXml, false);
      if(a.enhancedAudit != null) {
        a.workfowFqn = a.enhancedAudit.getFqn();
        if(a.enhancedAudit.getFqn() != null && !a.enhancedAudit.getFqn().isEmpty()) {
          a.workfowName = a.workfowFqn.substring(a.workfowFqn.lastIndexOf('.') + 1);
        }
      }
    }
    
    a.workflow = createWFObject(a);
    a.workflowGbo = new GenerationBaseObject(new FQName(), a.workflow, new MonitorXMOMLoader(createImportMap(a)));
    
    return a;
  }
  
  public String getWorkflowXml() {
    if(enhancedAudit != null && enhancedAudit.getAudit() != null) {
      return enhancedAudit.getAudit();
    }
    return auditDataXml;
  }
  
  private static WF createWFObject(MonitorAudit monitorAudit) throws XynaException {
    if(monitorAudit.getEnhancedAudit() == null || monitorAudit.getWorkflowFqn() == null) {
      return null;
    }
    
    WF wf = WF.getOrCreateInstanceForAudits(monitorAudit.getWorkflowFqn(), createImportMap(monitorAudit));
    wf.parseGeneration(false, false, false); // TODO: Parameter korrekt?
    WorkflowUtils.prepareWorkflowForMonitor(wf.getWfAsStep());
    addAdditionalDataToParams(wf);
    return wf;
  }
  
  private static Map<String, String> createImportMap(MonitorAudit monitorAudit){
    String mainWfFqn = monitorAudit.getWorkflowFqn();
    Map<String, String> xmlsWfAndImports = new HashMap<>();
    xmlsWfAndImports.put(mainWfFqn, monitorAudit.getWorkflowXml()); // add main Workflow
    
    // add imports
    if(monitorAudit.getEnhancedAudit() != null && monitorAudit.getEnhancedAudit().getImports() != null) {
      for (AuditImport curImport : monitorAudit.getEnhancedAudit().getImports()) {
        xmlsWfAndImports.putIfAbsent(curImport.getFqn(), curImport.getDocument());
      }
    }
    
    return xmlsWfAndImports;
  }
  
  private static void addAdditionalDataToParams(WF wf) {
    wf.getWfAsStep().addLabelsToParameter();
    wf.getWfAsStep().addIdsToParameter();

    for (Step topLevelStep : wf.getWfAsStep().getChildSteps()) {
      for (Step step : com.gip.xyna.xact.filter.monitor.Dataflow.collectAllSteps(topLevelStep)) {
        // labels of variables are not contained in the parameter-tags of the audit and need to be set separately
        step.addLabelsToParameter();
        
        // ids need to be set after parsing since referred variables might not be parsed yet while then step is being parsed
        step.addIdsToParameter();
      }
    }
  }
  
  private static ExecutionType getExecutionType(String destinationType) {
    if(destinationType == null) {
      return ExecutionType.UNKOWN;
    }
    if(destinationType.equals("Workflow")) {
      return ExecutionType.XYNA_FRACTAL_WORKFLOW;
    } else if (destinationType.equals("CodedService")) {
      return ExecutionType.SERVICE_DESTINATION;
    } else if (destinationType.equals("Java")) {
      return ExecutionType.JAVA_DESTINATION;
    } else {
      return ExecutionType.UNKOWN;
    }
  }
  
  private static String getFqn(Element e) {
    if(e != null && e.hasAttributes()) {
      NamedNodeMap attributes = e.getAttributes();
      Node typeName = attributes.getNamedItem(ATT.TYPENAME);
      Node typePath = attributes.getNamedItem(ATT.TYPEPATH);
      if(typePath != null && typeName != null) {
        return typePath.getNodeValue() + "." + typeName.getNodeValue();
      }
    }
    return null;
  }
  
  private static RuntimeContext getRuntimeContext(String application, String version, String workspace) {
    if(application != null && version != null) {
      return new Application(application, version);
    } else if (workspace != null) {
      return new Workspace(workspace);
    }
    return null;
  }


  private MonitorAudit() {

  }
  
  public boolean hasAuditData() {
    return hasAuditData;
  }
  
  public WF getWorkflow() {
    return workflow;
  }

  public GenerationBaseObject getWorkflowGbo() {
    return workflowGbo;
  }

  public boolean isImported() {
    return imported;
  }

  public String getWorkflowFqn() {
    return workfowFqn;
  }

  public String getGuiOrderId() {
    return guiOrderId;
  }


  public String getCustom0() {
    return custom0;
  }


  public String getCustom1() {
    return custom1;
  }


  public String getCustom2() {
    return custom2;
  }


  public String getCustom3() {
    return custom3;
  }


  public List<XynaExceptionInformation> getExceptions() {
    return exceptions;
  }


  public String getExecutionType() {
    return executionType;
  }


  public String getOrderType() {
    return orderType;
  }


  public Long getParentId() {
    return parentId;
  }


  public Long getOrderId() {
    return orderId;
  }


  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }


  public String getDestinationType() {
    return destinationType;
  }


  public Long getStartTime() {
    return startTime;
  }


  public Long getLastUpdate() {
    return lastUpdate;
  }


  public String getStatus() {
    return status;
  }


  public Integer getPriority() {
    return priority;
  }


  public Integer getMonitoringLevel() {
    return monitoringLevel;
  }


  public String getAuditDataXml() {
    return auditDataXml;
  }

  
  public EnhancedAudit getEnhancedAudit() {
    return enhancedAudit;
  }

  
  public String getGuiParentOrderId() {
    return guiParentOrderId;
  }

  
  public String getWorkfowName() {
    return workfowName;
  }

  public List<MissingImport> getMissingImports() {
    return missingImports;
  }

  public void setMissingImports(List<MissingImport> missingImports) {
    this.missingImports = missingImports;
  }
}
