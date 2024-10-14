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

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.AuditDestinationFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.AuditRuntimeContextFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.ComponentBasedAuditFilter;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.LocalAuditImportsFilterComponent;
import com.gip.xyna.xact.filter.monitor.auditFilterComponents.RepositoryRevisionFilterComponent;
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.BasicAuditImport;



public class ExportAuditProcessor {
  
  public static interface EL {
    
    public static final String ORDER_ITEM = "OrderItem";
    public static final String ORDER_ID = "OrderID";
    public static final String PARENT_ID = "ParentID";
    public static final String DESTINATION_TYPE = "DestinationType";
    public static final String DESTINATION = "Destination";
    public static final String START_TIME = "StartTime";
    public static final String LAST_INTERACTION = "LastInteraction";
    public static final String STATUS = "Status";
    public static final String PRIORITY = "Priority";
    public static final String MONITORING_LEVEL = "MonitoringLevel";
    
  }
  
  private static final RuntimeContextDependencyManagement rtcDependencyManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  
  private static final RevisionManagement revisionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  
  private static final RepositoryManagement repositoryManagement = 
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement();
  
  private static final DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
  
  private static XynaMultiChannelPortal multiChannelPortal =
      ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal());
  
  public static class MetaData {
    
    private Long orderId;
    private String name;
    
    public String getXmlFileName(){
      if(name != null) {
        return "Order_" + orderId + "_" + name + ".xml";
      }
      return "Order_" + orderId + ".xml";
    }
    
    public Long getOrderId() {
      return orderId;
    }
    
    public String getName() {
      return name;
    }
    
  }
  
  private final Long orderId;
  private MetaData metaData;
  
  
  public ExportAuditProcessor(Long orderId) throws XynaException {
    this.orderId = orderId;
  }
  
  public String createExportXML() {
    XmlBuilder result = new XmlBuilder();
    OrderInstanceDetails details = null;
    
    
    try {
      details = multiChannelPortal.getOrderInstanceDetails(orderId);
    } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("Audit could not be loaded.");
    }
    
    AuditInfo workflowFqnAndRtc = determineWorkflowFqnAndRtc(details.getAuditDataAsXML());
    String workflowFqn = workflowFqnAndRtc.getFqn();
    RuntimeContext wfRtc = workflowFqnAndRtc.getRtc();
    long repoRevision = workflowFqnAndRtc.getRepositoryRevision();
    
    result.startElement(EL.ORDER_ITEM);
    result.element(EL.ORDER_ID, String.valueOf(details.getId()));
    if(details.getParentId() > 0) {
      result.element(EL.PARENT_ID, String.valueOf(details.getParentId()));
    }
    writeRtc(result, details.getRuntimeContext());
    result.element(EL.DESTINATION_TYPE, convertExecutionType(details.getExecutionType()));
    result.element(EL.DESTINATION, workflowFqn);
    
    Timestamp startTime = new Timestamp(details.getStartTime());
    result.element(EL.START_TIME, dateformatter.format(startTime.toLocalDateTime()));
    startTime = null;
    
    Timestamp lastInteraction = new Timestamp(details.getLastUpdate());
    result.element(EL.LAST_INTERACTION, dateformatter.format(lastInteraction.toLocalDateTime()));
    lastInteraction = null;
    
    result.element(EL.STATUS, details.getStatusAsString());
    result.element(EL.PRIORITY, String.valueOf(details.getPriority()));
    result.element(EL.MONITORING_LEVEL, String.valueOf(details.getMonitoringLevel()));

    String auditDataXML = details.getAuditDataAsXML();
    details = null;
    int endIndex = appendAudit(result, auditDataXML);
    
    // shorten auditDataXML. -- do not remove repositoryID
    if(endIndex > -1) {
      auditDataXML = auditDataXML.substring(endIndex);
      auditDataXML = "<" + GenerationBase.EL.ENHANCED_AUDIT + "><" + GenerationBase.EL.AUDIT + ">" + auditDataXML;
    }
    
    appendImports(result, auditDataXML, wfRtc, repoRevision);
    
    result.endElement(EL.ORDER_ITEM);
    

    metaData = new MetaData();
    metaData.orderId = orderId;
    metaData.name = workflowFqn;
    
    
    return result.toString();
  }
  
  
  //if necessary, could be extended to exit early
  //currently iterates over entire audit
  private AuditInfo determineWorkflowFqnAndRtc(String auditDataAsXML) {

    ComponentBasedAuditFilter filter = new ComponentBasedAuditFilter();
    AuditDestinationFilterComponent destinationComponent = new AuditDestinationFilterComponent();
    AuditRuntimeContextFilterComponent rtcComponent = new AuditRuntimeContextFilterComponent();
    RepositoryRevisionFilterComponent repoRevComponent = new RepositoryRevisionFilterComponent();
    filter.addAuditFilterComponent(destinationComponent);
    filter.addAuditFilterComponent(rtcComponent);
    filter.addAuditFilterComponent(repoRevComponent);
    
    try {
      filter.parse(new InputSource(new StringReader(auditDataAsXML)));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    
    AuditInfo result = new AuditInfo();
    result.setFqn(destinationComponent.getDestination());
    result.setRtc(rtcComponent.getResult());
    result.setRepositoryRevision(repoRevComponent.getRepositoryRevision());
    
    return result;
  }

  
  private int appendAudit(XmlBuilder result, String xmlIn) {
    //add <audit> ... </audit> from xmlIn to result
    //but do it in a way that removes it from xmlIn as well!
    //FIXME: we cannot really afford to have it in memory twice.
    
    int beginIndex = xmlIn.indexOf("<" + GenerationBase.EL.AUDIT + ">");
    int endIndex = xmlIn.indexOf("</" + GenerationBase.EL.AUDIT + ">");
    
    if(beginIndex > -1 && endIndex > -1) {
      result.append(xmlIn.substring(beginIndex, endIndex + GenerationBase.EL.AUDIT.length() + 3)); // + </>
    }
    return endIndex;
  }

  //SAX over details
  private void appendImports(XmlBuilder result, String xmlIn, RuntimeContext auditRtc, long repoRevision) {
    ComponentBasedAuditFilter filter = new ComponentBasedAuditFilter();
    LocalAuditImportsFilterComponent importsComponent = new LocalAuditImportsFilterComponent(auditRtc);
    filter.addAuditFilterComponent(importsComponent);
    
    try {
      filter.parse(new InputSource(new StringReader(xmlIn)));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }

    xmlIn = null;
    
    List<AuditImport> imports = importsComponent.getImports();
    
    //append basic imports
    imports.addAll(BasicAuditImport.getAuditImports(false));
    
    Long auditRevision = -1l;
    try {
      auditRevision = revisionManagement.getRevision(auditRtc);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    Set<Long> dependencies = new HashSet<>();    
    rtcDependencyManagement.getDependenciesRecursivly(auditRevision, dependencies);
    
    RuntimeContext rcWorkflow = auditRtc;
    int length = imports.size();
    for(int i=0; i<length; i++) {
      String completeImport = completeImport(imports.get(0), repoRevision, rcWorkflow, dependencies);
      imports.remove(0);
      result.append(completeImport);
    }
  }
  
  
  private String completeImport(AuditImport shortImport, long repositoryRevision, RuntimeContext rcWorkflow, Set<Long> dependencies) {
    String fqName = shortImport.getFqn();
    RuntimeContext rc = shortImport.getRuntimeContext();
    if (rc == null) {
      rc = rcWorkflow;
    }
    
    long rcRev = -11l;
    try {
      rcRev = revisionManagement.getRevision(rc);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      //could not determine revision.
    }
    
    try {
      String importedXML = shortImport.getDocument(); //basic Imports have document set already
      if(importedXML == null || importedXML.length() == 0) {
        importedXML = repositoryManagement.getXMLFromRepository(rc, repositoryRevision, fqName);

        AuditXmlHelper helper = new AuditXmlHelper();
        importedXML = helper.readAndOptimizeXml(importedXML);
      } else {
        Document document = XMLUtils.parseString(shortImport.getDocument());
        importedXML = XMLUtils.getXMLString(document.getDocumentElement(), false);
      }


      if(dependencies.contains(rcRev)) {
        rc = rcWorkflow;
      }
      
      
      XmlBuilder builder = new XmlBuilder();
      builder.startElement(GenerationBase.EL.IMPORT);
      
      writeRtc(builder, rc);
      
      builder.startElement(GenerationBase.EL.DOCUMENT);
      builder.append(importedXML);
      builder.endElement(GenerationBase.EL.DOCUMENT);
      
      builder.endElement(GenerationBase.EL.IMPORT);
      
      return builder.toString();
    } catch(XynaException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeRtc(XmlBuilder builder, RuntimeContext runtimeContext) {
    if(runtimeContext == null) {
      return;
    }
    if(runtimeContext instanceof Workspace) {
      Workspace workspace = (Workspace) runtimeContext;
      builder.element(GenerationBase.EL.WORKSPACE, workspace.getName());
    } else if (runtimeContext instanceof Application) {
      Application application = (Application) runtimeContext;
      builder.element(GenerationBase.EL.APPLICATION, application.getName());
      builder.element(GenerationBase.EL.APPLICATION_VERSION, application.getVersionName());
    } else {
      throw new RuntimeException("Unsupported RuntimeContext " + runtimeContext.getClass().getSimpleName());
    }
  }
  
  private String convertExecutionType(String executionType) {
    ExecutionType type = ExecutionType.valueOf(executionType);
    switch(type) {
      case JAVA_DESTINATION:
        return "Java";
      case SERVICE_DESTINATION:
        return "CodedService";
      case UNKOWN:
        return "";
      case XYNA_FRACTAL_WORKFLOW:
        return "Workflow";
    }
    return "";
  }

  public MetaData getMetaData() {
    return metaData;
  }
  
  
  private static class AuditInfo {
    private String fqn;
    private RuntimeContext rtc;
    private long repositoryRevision;
    
    public String getFqn() {
      return fqn;
    }
    
    public void setFqn(String fqn) {
      this.fqn = fqn;
    }
    
    public RuntimeContext getRtc() {
      return rtc;
    }
    
    public void setRtc(RuntimeContext rtc) {
      this.rtc = rtc;
    }
    
    public long getRepositoryRevision() {
      return repositoryRevision;
    }
    
    public void setRepositoryRevision(long repositoryRevision) {
      this.repositoryRevision = repositoryRevision;
    }
  }
}
