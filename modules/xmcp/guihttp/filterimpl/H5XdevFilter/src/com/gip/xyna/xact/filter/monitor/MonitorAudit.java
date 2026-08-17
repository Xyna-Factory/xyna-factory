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



import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
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

  private static XynaMultiChannelPortal multiChannelPortal =
      ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal());
  
  private static FileManagement fileManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();

  private static RepositoryManagement repositoryManagement =
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement();
  
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
  private boolean onlyParentRuntimeInfo = false;
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
    for (DateTimeFormatter df : dateformatters) {
      try {
        return df.parse(input);
      } catch (DateTimeParseException e) {
        continue; //try different format
      }
    }
    throw new RuntimeException("Unsupported DateTimeFormat: " + input);
  }


  public static MonitorAudit fromLocalOrder(long orderId, Long parentId) throws XynaException {

    return fromLocalOrder(orderId, parentId, t -> {
      try {
        return AuditPreprocessing.filterAudit(t);
      } catch (ParserConfigurationException | SAXException | TransformerException | TransformerFactoryConfigurationError e) {
        throw new RuntimeException(e);
      }
    });
  }


  public static MonitorAudit fromLocalOrder(long orderId, Long parentOrderId, Function<String, String> filter) throws XynaException {
    OrderInstanceDetails childDetails = null;
    try {
      childDetails = multiChannelPortal.getOrderInstanceDetails(orderId);
    } catch (XynaException e) {
      if(parentOrderId == null || parentOrderId <= 0 || parentOrderId == orderId) {
        throw e;
      }
    }

    MonitorAudit a = new MonitorAudit();
    a.imported = false;
    a.orderId = orderId;
    a.guiOrderId = String.valueOf(a.orderId);
    a.parentId = childDetails != null ? childDetails.getParentId() : parentOrderId;
    a.guiParentOrderId = String.valueOf(a.parentId >= 0 ? a.parentId : 0);

    if(childDetails != null) {
      setOrderDetails(a, childDetails);
      if(childDetails.getAuditDataAsXML() != null && !childDetails.getAuditDataAsXML().isEmpty()) {
        loadAudit(a, childDetails.getAuditDataAsXML(), filter);
      }
    }

    if(a.enhancedAudit == null && parentOrderId != null && parentOrderId > 0 && parentOrderId != orderId) {
      OrderInstanceDetails parentDetails = multiChannelPortal.getOrderInstanceDetails(parentOrderId);
      loadChildAuditFromParent(a, orderId, parentDetails, filter);
    }
    
    a.workflow = createWFObject(a);
    a.workflowGbo = new GenerationBaseObject(new FQName(), a.workflow, new MonitorXMOMLoader(createImportMap(a)));
    
    return a;
  }

  private static void setOrderDetails(MonitorAudit audit, OrderInstanceDetails details) {
    audit.runtimeContext = details.getRuntimeContext();
    audit.orderType = details.getOrderType();
    audit.startTime = details.getStartTime();
    audit.lastUpdate = details.getLastUpdate();
    audit.status = details.getStatusAsString();
    audit.priority = details.getPriority();
    try {
      audit.monitoringLevel = details.getMonitoringLevel();
    } catch (NullPointerException npe) {
      Utils.logError("NullPointerException at getMonitoringLevel()", null);
    }
    audit.executionType = details.getExecutionType();
    audit.exceptions = details.getExceptions();
    audit.custom0 = details.getCustom0();
    audit.custom1 = details.getCustom1();
    audit.custom2 = details.getCustom2();
    audit.custom3 = details.getCustom3();
  }

  private static void loadAudit(MonitorAudit audit, String auditXml, Function<String, String> filter) {
    audit.hasAuditData = true;
    try {
      audit.auditDataXml = filter.apply(auditXml);
    } catch (Exception e) {
      Utils.logError("Audit prefiltering to avoid risk of OOM failed", e);
      audit.auditDataXml = auditXml;
    }

    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    audit.enhancedAudit = xmlHelper.auditFromXml(audit.auditDataXml, false);
    setWorkflowName(audit);
  }

  private static void loadChildAuditFromParent(MonitorAudit audit, long childOrderId, OrderInstanceDetails parentDetails,
                                                Function<String, String> filter) throws XynaException {

    audit.onlyParentRuntimeInfo = true;

    if (parentDetails == null) {
      return;
    }

    String parentAuditXml = parentDetails.getAuditDataAsXML();
    if(parentAuditXml == null || parentAuditXml.isEmpty()) {
      return;
    }

    String filteredParentAuditXml;
    try {
      filteredParentAuditXml = filter.apply(parentAuditXml);
    } catch (Exception e) {
      Utils.logError("Parent audit prefiltering to avoid risk of OOM failed", e);
      filteredParentAuditXml = parentAuditXml;
    }

    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    EnhancedAudit parentAudit = xmlHelper.auditFromXml(filteredParentAuditXml, false);
    if(parentAudit == null || parentAudit.getAudit() == null) {
      return;
    }

    Document parentWorkflow = XMLUtils.parseString(parentAudit.getAudit());
    Element childParameter = findParameterForOrder(parentWorkflow.getDocumentElement(), childOrderId);
    Element function = childParameter != null && childParameter.getParentNode() instanceof Element
        ? (Element) childParameter.getParentNode() : null;
    if(function == null || !EL.FUNCTION.equals(getElementName(function))) {
      return;
    }

    Element invoke = XMLUtils.getChildElementByName(function, EL.INVOKE);
    String serviceId = invoke != null ? invoke.getAttribute(ATT.SERVICEID) : null;
    Element serviceReference = XMLUtils.getChildElementsRecursively(parentWorkflow.getDocumentElement(), EL.SERVICEREFERENCE).stream()
        .filter(element -> serviceId != null && serviceId.equals(element.getAttribute(ATT.ID)))
        .findFirst().orElse(null);
    String childWorkflowFqn = getReferenceFqn(serviceReference);
    audit.orderType = childWorkflowFqn;
    if(childWorkflowFqn == null) {
      return;
    }

    RuntimeContext childRuntimeContext = getImportRuntimeContext(parentAudit, childWorkflowFqn);
    if(childRuntimeContext == null) {
      childRuntimeContext = parentAudit.getWorkflowContext() != null ? parentAudit.getWorkflowContext() : parentDetails.getRuntimeContext();
    }
    audit.runtimeContext = childRuntimeContext;

    String childWorkflowXml = repositoryManagement.getXMLFromRepository(childRuntimeContext,
                                                                         parentAudit.getRepositoryRevision(),
                                                                         childWorkflowFqn);
    if (childWorkflowXml == null || childWorkflowXml.isEmpty()) {
      return;
    }

    Document childWorkflow = XMLUtils.parseString(childWorkflowXml);
    Element operation = XMLUtils.getChildElementByName(childWorkflow.getDocumentElement(), EL.OPERATION);
    if(operation == null) {
      return;
    }
    Element rootParameter = (Element) childWorkflow.importNode(childParameter, true);
    materializeObjectReferences(rootParameter, getObjectsByInstanceId(parentWorkflow), new HashSet<>());
    rootParameter.setAttribute(ATT.INSTANCE_ID, Long.toString(childOrderId));
    rootParameter.setAttribute(ATT.PARENTORDER_ID, Long.toString(parentDetails.getId()));
    operation.appendChild(rootParameter);

    audit.auditDataXml = XMLUtils.getXMLString(childWorkflow.getDocumentElement(), false);
    audit.enhancedAudit = new EnhancedAudit(parentAudit.getVersion(), audit.auditDataXml, childWorkflowFqn,
                                            parentAudit.getImports(), parentAudit.getRepositoryRevision(), childRuntimeContext);
    audit.hasAuditData = true;
    audit.executionType = ExecutionType.XYNA_FRACTAL_WORKFLOW.name();
    setWorkflowName(audit);
  }

  private static Element findParameterForOrder(Element root, long orderId) {
    String orderIdString = Long.toString(orderId);
    return XMLUtils.getChildElementsRecursively(root, EL.PARAMETER).stream()
        .filter(parameter -> Stream.of(parameter.getAttribute(ATT.INSTANCE_ID).split(","))
            .map(String::trim)
            .anyMatch(orderIdString::equals))
        .findFirst().orElse(null);
  }

  private static Map<String, Element> getObjectsByInstanceId(Document parentWorkflow) {
    return Stream.concat(XMLUtils.getChildElementsRecursively(parentWorkflow.getDocumentElement(), EL.DATA).stream(),
                         XMLUtils.getChildElementsRecursively(parentWorkflow.getDocumentElement(), EL.EXCEPTION).stream())
        .filter(element -> !element.getAttribute(ATT.OBJECT_ID).isEmpty())
        .collect(Collectors.toMap(element -> element.getAttribute(ATT.OBJECT_ID), Function.identity(), (first, duplicate) -> first));
  }

  private static void materializeObjectReferences(Element element, Map<String, Element> objectsByInstanceId,
                                                  Set<String> resolvingReferences) {
    String referenceId = element.getAttribute(ATT.OBJECT_REFERENCE_ID);
    if(!referenceId.isEmpty()) {
      Element referencedObject = objectsByInstanceId.get(referenceId);
      if(referencedObject == null || !resolvingReferences.add(referenceId)) {
        return;
      }
      Element resolvedObject = (Element) element.getOwnerDocument().importNode(referencedObject, true);
      materializeObjectReferences(resolvedObject, objectsByInstanceId, resolvingReferences);
      NamedNodeMap resolvedAttributes = resolvedObject.getAttributes();
      for(int i = 0; i < resolvedAttributes.getLength(); i++) {
        Node attribute = resolvedAttributes.item(i);
        element.setAttributeNS(attribute.getNamespaceURI(), attribute.getNodeName(), attribute.getNodeValue());
      }
      while(element.hasChildNodes()) {
        element.removeChild(element.getFirstChild());
      }
      while(resolvedObject.hasChildNodes()) {
        element.appendChild(resolvedObject.getFirstChild());
      }
      element.removeAttribute(ATT.OBJECT_REFERENCE_ID);
      XMLUtils.getChildElements(element).forEach(child -> materializeObjectReferences(child, objectsByInstanceId, resolvingReferences));
      resolvingReferences.remove(referenceId);
      return;
    }
    XMLUtils.getChildElements(element).forEach(child -> materializeObjectReferences(child, objectsByInstanceId, resolvingReferences));
  }

  private static String getReferenceFqn(Element serviceReference) {
    if(serviceReference == null) {
      return null;
    }
    String typePath = serviceReference.getAttribute(ATT.REFERENCEPATH);
    String typeName = serviceReference.getAttribute(ATT.REFERENCENAME);
    if(typePath.isEmpty() || typeName.isEmpty()) {
      return null;
    }
    return typePath + "." + typeName;
  }

  private static String getElementName(Element element) {
    return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
  }

  private static RuntimeContext getImportRuntimeContext(EnhancedAudit parentAudit, String fqn) {
    return parentAudit.getImports().stream()
        .filter(auditImport -> fqn.equals(auditImport.getFqn()))
        .findFirst()
        .map(AuditImport::getRuntimeContext)
        .orElse(null);
  }

  private static void setWorkflowName(MonitorAudit audit) {
    if(audit.enhancedAudit == null) {
      return;
    }
    audit.workfowFqn = audit.enhancedAudit.getFqn();
    if(audit.workfowFqn != null && !audit.workfowFqn.isEmpty()) {
      audit.workfowName = audit.workfowFqn.substring(audit.workfowFqn.lastIndexOf('.') + 1);
    }
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
      for (Step step : collectAllSteps(topLevelStep)) {
        // labels of variables are not contained in the parameter-tags of the audit and need to be set separately
        step.addLabelsToParameter();
        
        // ids need to be set after parsing since referred variables might not be parsed yet while then step is being parsed
        step.addIdsToParameter();
      }
    }
  }


  private static List<Step> collectAllSteps(Step rootStep) {
    List<Step> steps = new ArrayList<>();
    steps.add(rootStep);

    if (rootStep.getChildSteps() != null) {
      for (Step childStep : rootStep.getChildSteps()) {
        steps.addAll(collectAllSteps(childStep));
      }
    }

    return steps;
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

  public boolean hasOnlyParentRuntimeInfo() {
    return onlyParentRuntimeInfo;
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
