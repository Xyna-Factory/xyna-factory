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
package com.gip.xyna.update.outdatedclasses_7_0_2_13;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.update.outdatedclasses_7_0_2_13.XynaEngineSpecificAuditData.StepAuditDataContainer;
import com.gip.xyna.update.outdatedclasses_7_0_2_13.XynaEngineSpecificAuditData.StepAuditDataContent;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.streams.MemoryEfficientStringWriter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionContainer;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformationThrowable;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.BasicAuditImport;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


public class XMLExtension {

  private static final Logger logger = Logger.getLogger(XMLExtension.class);
  
  private static final Comparator<Integer> comparatorHighToLow = new Comparator<Integer>() {

    public int compare(Integer o1, Integer o2) {
      return o2 - o1;
    }

  };

  private final long revision;
  
  private XMLStreamReader xmlReader;
  private XMLStreamWriter xmlWriter;
  private MemoryEfficientStringWriter sw = new MemoryEfficientStringWriter();
  private final Stack<String> elementStack = new Stack<String>();
  private final Stack<String> idStack = new Stack<String>();
  
  private final XynaEngineSpecificAuditData auditData;
  
  private final SimpleDateFormat sdf;
  private final boolean removeFromAuditDataMap;
  private final GeneralXynaObject.XMLReferenceCache cache;
  private final DeploymentItemStateManagement dsim = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
  private final DependencyRegister depReg = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
  private final RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

  public XMLExtension(XynaEngineSpecificAuditData auditData, long revision, boolean removeFromAuditDataMap) {
    this.auditData = auditData;
    this.revision = revision;
    this.removeFromAuditDataMap = removeFromAuditDataMap;
    this.cache = new GeneralXynaObject.XMLReferenceCache(revision);
    
    if (XynaProperty.XYNA_PROCESS_MONITOR_SHOW_STEP_MILLISECONDS.get()) {
      sdf = Constants.defaultUTCSimpleDateFormatWithMS();
    } else {
      sdf = Constants.defaultUTCSimpleDateFormat();
    }
  }
  
  public String getString() {
    String s = sw.getString();
    sw = null;
    return s;
  }
  
  
  public void createXmlForFractalWorkflow(long instanceId, long parentId, long startTime, long endTime, XynaExceptionInformation exception, StepAuditDataContainer container) throws XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

    try {
      Long xmlRevision = rcdm.getRevisionDefiningXMOMObject(auditData.getProcess(), revision);
      if (xmlRevision == null) {
        throw new RuntimeException("Owner-Revision not found for " + auditData.getProcess() + ", parentRevision = " + revision);
      }
      //FIXME synchronisieren mit generationbase, weil dort gleichzeitig auch auf das xml schreibend zugegriffen werden könnte.
      xmlReader =
          inputFactory.createXMLStreamReader(new FileInputStream(new File(GenerationBase
                                                                          .getFileLocationForDeploymentStaticHelper(auditData.getProcess(), xmlRevision)
                                                                          + ".xml")));
      try {
        xmlWriter = outputFactory.createXMLStreamWriter(sw);
        try {
          boolean operationFound = false; //es muss genau ein Operation-Element vorhanden sein
  
          //Die Schritte für die compensations umgekehrt sortieren, damit diese in der richtigen reihenfolge ins xml geschrieben werden.
          Map<Integer, Integer> mapStepIdToRefIdForCompensation = new TreeMap<Integer, Integer>(comparatorHighToLow);
          
          while (xmlReader.hasNext()) {
            int xmlType = xmlReader.next();
            switch (xmlType) {
              case XMLStreamReader.PROCESSING_INSTRUCTION :
                xmlWriter.writeProcessingInstruction(xmlReader.getPITarget(), xmlReader.getPIData());
                break;
              case XMLStreamReader.START_ELEMENT :
                String name = xmlReader.getLocalName();
                
                xmlWriter.writeStartElement(name);
                for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
                  xmlWriter.writeAttribute(xmlReader.getAttributeLocalName(i), xmlReader.getAttributeValue(i));
                }
                for (int i = 0; i < xmlReader.getNamespaceCount(); i++) {
                  xmlWriter.writeNamespace(xmlReader.getNamespacePrefix(i), xmlReader.getNamespaceURI(i));
                }
                elementStack.add(name);
                String id = xmlReader.getAttributeValue(null, GenerationBase.ATT.ID);
                idStack.add(id);
                
                //Ids für compensations sammeln
                if (name.equals(GenerationBase.EL.FUNCTION)) {
                  Integer refId = Integer.valueOf(id);
                  Integer stepId = auditData.getStepIdByRefId(refId);
                  if (stepId != null) {
                    mapStepIdToRefIdForCompensation.put(stepId, refId);
                  }
                }
                
                break;
              case XMLStreamReader.END_ELEMENT :
                name = elementStack.pop();
                id = idStack.pop();

                if (id != null && mayContainParameter(name)) {
                  Integer refId = Integer.valueOf(id);
                  addParameter(name, refId);
                }
                
                if (name.equals(GenerationBase.EL.OPERATION)) {
                  if (operationFound) {
                    throw new XPRC_CREATE_MONITOR_STEP_XML_ERROR("More than one operation found in XML for " + auditData.getProcess());
                  } else {
                    operationFound = true;
                    appendParameterToOperation(instanceId, parentId, startTime, endTime, exception, container, mapStepIdToRefIdForCompensation);
                  }
                }
                xmlWriter.writeEndElement();
                break;
              case XMLStreamReader.CDATA :
                xmlWriter.writeCData(xmlReader.getText());
                break;
              case XMLStreamReader.COMMENT :
                //ignore für audit
                break;
              case XMLStreamReader.START_DOCUMENT :
                //processing instruction wird oben bereits geschrieben
                break;
              case XMLStreamReader.END_DOCUMENT :
                xmlWriter.writeEndDocument();
                break;
              case XMLStreamReader.NAMESPACE :
                //ntbd
                break;
              case XMLStreamReader.CHARACTERS : //Zeilenumbrüche
                xmlWriter.writeCharacters(xmlReader.getText());
                break;
              default : throw new RuntimeException("unsupported type: " + xmlType);
            }
          }
          
          if (!operationFound) {
            throw new XPRC_CREATE_MONITOR_STEP_XML_ERROR("No operation found in XML for " + auditData.getProcess());
          }
        } finally {
          xmlWriter.flush();
          xmlWriter.close();
          xmlWriter = null;
        }
      } finally {
        xmlReader.close();
        xmlReader = null;
      }
    } catch (FileNotFoundException e1) {
      //FIXME fehlerbehandlung
      throw new RuntimeException(e1);
    } catch (XMLStreamException e1) {
      throw new RuntimeException(e1);
    }
  }


  public void createXmlForServiceDestination(long instanceId, long parentId, StepAuditDataContainer container) {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    
    try {
      xmlWriter = outputFactory.createXMLStreamWriter(sw);
      try {
        xmlWriter.writeStartElement(GenerationBase.EL.PARAMETER);
        
        xmlWriter.writeAttribute(GenerationBase.ATT.INSTANCE_ID, String.valueOf(instanceId));
        if (parentId > 0) {
          xmlWriter.writeAttribute(GenerationBase.ATT.PARENTORDER_ID, Long.toString(parentId));
        }
        
        if (auditData.getProcess() != null) {
          xmlWriter.writeStartElement(GenerationBase.EL.META);
          xmlWriter.writeStartElement(GenerationBase.EL.SERVICE);
          xmlWriter.writeCharacters(auditData.getProcess());
          xmlWriter.writeEndElement();
          xmlWriter.writeEndElement();
        }
        
       if (container != null) {
          Long startTime = container.getStartTime(null, 0);
          if (startTime != null) {
            createInputParameter(startTime, container,  null, 0, "");
          }
           Long stopTime = container.getStopTime(null, 0);
          if (stopTime != null) {
            createOutputParameter(stopTime, container, null, 0);
          }
          Long errorTime = container.getErrorTime(null, 0);
          if (errorTime != null) {
            //exceptioninformation-objekte enthalten bereits xml darstellung
            createErrorParameter(errorTime, container, null, 0);
          }
        }
        
        xmlWriter.writeEndElement();
      } finally {
        xmlWriter.flush();
        xmlWriter.close();
        xmlWriter = null;
      }
    } catch (XMLStreamException e1) {
      throw new RuntimeException(e1);
    }
  }
 
  /*
   * TODO
   * getdependencies() gibt auch abhängigkeiten zu datentypen, die sich aus instanzenmethoden des typs ergeben. die will man nicht mitnehmen!
   */
  public Set<DependencyNode> getAuditImports() throws Ex_FileWriteException {
    Set<DependencyNode> imports = new HashSet<DependencyNode>();

    //dynamische Anteile (nur Datentypen?) hinzufügen
    for (DependencyNode importNode : cache.getAuditImports()) {
      if (BasicAuditImport.isBasicDataType(importNode.getUniqueName())) {
        continue; //für Modellierung wichtige Datentypen werden dynamisch zum Audit hinzugefügt
      }

      if (imports.add(importNode)) {
        getDependencies(importNode, imports, true); // rekursiv (z.B. um Basisklassen zu finden) 
      }
    }
    
    for (String s : additionalImports) {
      if (BasicAuditImport.isBasicDataType(s)) {
        continue;
      }
      long revisionDefiningObject = rcdm.getRevisionDefiningXMOMObjectOrParent(s, revision);
      DependencyNode n = depReg.getDependencyNode(s, DependencySourceType.DATATYPE, revisionDefiningObject);
      if (n == null) {
        n = depReg.getDependencyNode(s, DependencySourceType.XYNAEXCEPTION, revisionDefiningObject);
      }
      if (n != null) {
        if (imports.add(n)) {
          getDependencies(n, imports, true); // rekursiv (z.B. um Basisklassen zu finden)
        }
      }
    }
    
    //statische Anteile hinzufügen (darf erst nach den dynamischen gemacht werden, da hier (teilweise) keine Rekursion nötig ist)
    DependencyNode rootNode = depReg.getDependencyNode(auditData.getProcess(), DependencySourceType.WORKFLOW, revision);
    if (rootNode == null) {
      long revisionDefiningWF = rcdm.getRevisionDefiningXMOMObjectOrParent(auditData.getProcess(), revision);
      rootNode = depReg.getDependencyNode(auditData.getProcess(), DependencySourceType.WORKFLOW, revisionDefiningWF);
    }
    getDependencies(rootNode, imports, false);

    return imports;
  }
  
  private void getDependencies(DependencyNode rootNode, Set<DependencyNode> imports, boolean recursive) {
    for (DependencyNode node : rootNode.getUsedNodes()) {
      if (BasicAuditImport.isBasicDataType(node.getUniqueName())) {
        continue; //für Modellierung wichtige Datentypen werden dynamisch zum Audit hinzugefügt
      }
      
      boolean isGeneralXO = node.getType().equals(DependencySourceType.DATATYPE) || node.getType().equals(DependencySourceType.XYNAEXCEPTION);
      boolean isWF = node.getType().equals(DependencySourceType.WORKFLOW);
      //datentypen und exceptions müssen auch ihre usedobjects hinzufügen, solange die im audit/import noch referenziert sind
      if (isGeneralXO || isWF) {
        if (imports.add(node)) {
          if (recursive || isGeneralXO) {
            getDependencies(node, imports, recursive || isGeneralXO);
          } else if (isWF) {
            Set<OperationInterface> ops = dsim.get(node.getUniqueName(), node.getRevision()).getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
            if (ops != null && ops.size() > 0) {
              for (TypeInterface ex : ops.iterator().next().getExceptions()) {
                long revisionDefiningException = rcdm.getRevisionDefiningXMOMObjectOrParent(ex.getName(), node.getRevision());
                DependencyNode exNode = depReg.getDependencyNode(ex.getName(), DependencySourceType.XYNAEXCEPTION, revisionDefiningException);
                if (exNode != null) {
                  if (imports.add(exNode)) {
                    getDependencies(exNode, imports, true);
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  
  private static boolean mayContainParameter(String name) {
    if (name.equals(GenerationBase.EL.FUNCTION)) return true;
    if (name.equals(GenerationBase.EL.MAPPINGS)) return true;
    if (name.equals(GenerationBase.EL.CHOICE)) return true;
    if (name.equals(GenerationBase.EL.CATCH)) return true;
    if (name.equals(GenerationBase.EL.CASE)) return true;
//    if (name.equals(GenerationBase.EL.COMPENSATE)) return true; // FIXME this has to be treated somehow different because compensate elements dont have IDs
    if (name.equals(GenerationBase.EL.THROW)) return true;
    if (name.equals(GenerationBase.EL.RETRY)) return true;
 
    return false; 
  }
  
  
  private void addParameter(String name, Integer refId) throws XMLStreamException {
    Integer stepNo = auditData.getStepIdByRefId(refId);
    // if this evaluates to null, the step has not yet been reached, so do nothing
    if (stepNo == null) {
      return;
    }

    Long stepStartTime, stepEndTime, stepErrorTime = null;
    Set<Long> subWorkflowInstanceIds = null;

    StepAuditDataContainer container = auditData.getStepAuditDataContainerByStepId(stepNo, true);
    
    Set<Long> allRetrys = container.getAllRetrys();
    for(Long retryCounter : allRetrys) {
      Set<Integer[]> allForEachIndices = container.getAllForEachIndicesOrdered(retryCounter);
      for (Integer[] forEachIndices : allForEachIndices) {

        stepStartTime = container.getStartTime(forEachIndices, retryCounter);
        stepEndTime = container.getStopTime(forEachIndices, retryCounter);
        stepErrorTime = container.getErrorTime(forEachIndices, retryCounter);
        subWorkflowInstanceIds = container.getSubworkflowIds(forEachIndices, retryCounter);

        if (stepStartTime != null || stepEndTime != null || stepErrorTime != null) {

          xmlWriter.writeStartElement(GenerationBase.EL.PARAMETER);
          
          if (subWorkflowInstanceIds != null) {
            StringBuilder sb = new StringBuilder();
            for (Long id : subWorkflowInstanceIds) {
              if (sb.length() > 0) {
                sb.append(", ");
              }
              sb.append(id);
            }
            xmlWriter.writeAttribute(GenerationBase.ATT.INSTANCE_ID, sb.toString()); //kommaseparierte liste
          }

          if (forEachIndices != null && forEachIndices.length != 0) {
            xmlWriter.writeAttribute(GenerationBase.ATT.FOREACH_INDICES, getForEachIndicesAsString(forEachIndices));
          }
          if (retryCounter > 0) {
            xmlWriter.writeAttribute(GenerationBase.ATT.RETRY_COUNTER, Long.toString(retryCounter));
          }

          if (stepStartTime != null) {
            createInputParameter(stepStartTime, container, forEachIndices, retryCounter, name);
          }
          if (stepEndTime != null) {
            createOutputParameter(stepEndTime, container, forEachIndices, retryCounter);
          }
          if (stepErrorTime != null) {
            createErrorParameter(stepErrorTime, container, forEachIndices, retryCounter);
          }
          
          xmlWriter.writeEndElement();
        }
      }
    }
  }
  
  private static String getForEachIndicesAsString(Integer[] forEachIndices) {
    if (forEachIndices == null) {
      return "";
    } else {
      StringBuilder result = new StringBuilder();
      for (int i=0; i<forEachIndices.length; i++) {
        result.append(forEachIndices[i]);
        if (i < forEachIndices.length - 1) {
          result.append(",");
        }
      }
      return result.toString();
    }
  }
  
  private void writeRaw(String rawXml) throws XMLStreamException {
    xmlWriter.writeCharacters("");
    xmlWriter.flush();

    sw.write(rawXml);
    sw.flush();
  }


  private void createOutputParameter(long endTime, StepAuditDataContainer container, Integer[] forEachIndices, long retryCount)
      throws XMLStreamException {
    Pair<GeneralXynaObject[], Long> p = container.getOutputObjects(forEachIndices, retryCount, removeFromAuditDataMap);
    createParameter(endTime, p, GenerationBase.EL.PARAMETER_OUTPUT);
  }


  private void createInputParameter(long startTime, StepAuditDataContainer container, Integer[] forEachIndices, long retryCount,
                                    String parentNodeName) throws XMLStreamException {
    Pair<GeneralXynaObject[], Long> p = container.getInputObjects(forEachIndices, retryCount, removeFromAuditDataMap);
    createParameter(startTime, p, GenerationBase.EL.PARAMETER_INPUT);
  }


  private void createParameter(long time, Pair<GeneralXynaObject[], Long> gxo, String tagName) throws XMLStreamException {
    xmlWriter.writeStartElement(tagName);

    String date = sdf.format(new Date(time));
    xmlWriter.writeAttribute(GenerationBase.ATT.DATE, date);


    if (gxo != null) {
      GeneralXynaObject[] values = gxo.getFirst();
      if (values != null) {
        for (GeneralXynaObject xo : values) {
          if (xo != null) {
            Long version = gxo.getSecond();
            if (xo instanceof XynaExceptionBase || xo instanceof XynaExceptionContainer) {
              XynaExceptionInformation xei = null;
              if (xo instanceof XynaExceptionBase) {
                xei = new XynaExceptionInformation((XynaExceptionBase) xo, version, cache);
              } else if (xo instanceof XynaExceptionContainer) {
                xei = new XynaExceptionInformation(((XynaExceptionContainer) xo).getException(), version, cache);
              } else {
                throw new RuntimeException("unexpected class"); // kann nicht vorkommen, wegen dem if oben drüber. find
                                                                // bugs ist so aber glücklich
              }
              createExceptionElement(xei);
            } else {
              writeRaw(xo.toXml(null, false, version, cache));
            }
          } else {
            xmlWriter.writeEmptyElement(GenerationBase.EL.DATA);
          }
        }
      }
    }

    xmlWriter.writeEndElement();
  }
  
  private Set<String> additionalImports = new HashSet<String>();
  
  //FIXME: das ist ganz hässlich hier, dass die exceptions bereits als xml serialisiert vorliegen. 
  //weil dadurch wird das referenzierungs-feature (InstanceId=X, RefInstanceId=Y) ausgehebelt.
  //ich bin nicht sicher, ob es notwendig (für deserialisierung) ist, die exceptions schon vorher zu serialisieren.
  private void createExceptionElement(XynaExceptionInformation exception) throws XMLStreamException {
    Document doc;
    try {
      doc = XMLUtils.parseString(exception.getXml());
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
    
    Element exceptionElement = doc.getDocumentElement();

    //imports erweitern (wegen obigem FIXME)    
    //FIXME den runtimecontext der additionalimports kann man hier auch nicht gut ermitteln!
    List<Element> datas = XMLUtils.getChildElementsRecursively(exceptionElement, GenerationBase.EL.DATA);
    List<Element> exceptions = XMLUtils.getChildElementsRecursively(exceptionElement, GenerationBase.EL.EXCEPTION);
    datas.addAll(exceptions);
    datas.add(exceptionElement);
    for (Element el : datas) {
      String path = el.getAttribute(GenerationBase.ATT.REFERENCEPATH);
      String name = el.getAttribute(GenerationBase.ATT.REFERENCENAME);
      if (path.length() > 0 && name.length() > 0) {
        additionalImports.add(path + "." + name);
      }
    }

    // message + stacktrace dazu.
    Element stackTraceElement = doc.createElement(GenerationBase.EL.STACKTRACE);
    XMLUtils.setTextContent(stackTraceElement, getFormattedStackTrace(exception));
    exceptionElement.appendChild(stackTraceElement);
    Element messageElement = doc.createElement(GenerationBase.EL.ERRORMESSAGE);
    XMLUtils.setTextContent(messageElement, exception.getMessage());
    exceptionElement.appendChild(messageElement);

    exceptionElement.setAttribute(GenerationBase.ATT.ERROR_TYPE, OrderInstanceStatus.failed(exception).getName());
    

    writeRaw(XMLUtils.getXMLString(doc.getDocumentElement(), false));
  }
  
  private static String getFormattedStackTrace(XynaExceptionInformation exception) {
    XynaExceptionInformationThrowable tmp = new XynaExceptionInformationThrowable(exception);
    StringWriter writer = new StringWriter();
    tmp.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
  
  
  private void createErrorParameter(long time, StepAuditDataContainer container, Integer[] forEachIndices,
                                      long retryCount) throws XMLStreamException {
    XynaExceptionInformation exception = null;
    if (container != null) {
      exception = container.getException(forEachIndices, retryCount, removeFromAuditDataMap);
    }
    createErrorParameter(time, exception);
  }


  private void createErrorParameter(long time, XynaExceptionInformation exception) throws XMLStreamException {
    xmlWriter.writeStartElement(GenerationBase.EL.PARAMETER_ERROR);

    String endDate = sdf.format(new Date(time));
    xmlWriter.writeAttribute(GenerationBase.ATT.DATE, endDate);

    if (exception == null) {
      exception = new XynaExceptionInformation(new Exception("unknown exception. this is probably caused by a bug."));
    }

    createExceptionElement(exception);

    xmlWriter.writeEndElement();
  }
  
  private static final Integer[] EMPTY_FOR_EACH_INDICES = new Integer[0];


  private void appendParameterToOperation(long instanceId, long parentId, long startTime, long endTime, XynaExceptionInformation exception, StepAuditDataContainer container,
                           Map<Integer, Integer> mapStepIdToRefIdForCompensation) throws XMLStreamException {
    xmlWriter.writeStartElement(GenerationBase.EL.PARAMETER);
    xmlWriter.writeAttribute(GenerationBase.ATT.INSTANCE_ID, Long.toString(instanceId));
    if (parentId > 0) {
      xmlWriter.writeAttribute(GenerationBase.ATT.PARENTORDER_ID, Long.toString(parentId));
    }
    
    if (startTime > 0) {
      createInputParameter(startTime, container, EMPTY_FOR_EACH_INDICES, 0, null);
    }
    
    boolean hasError = exception != null;
    if (endTime > 0) {
      if (hasError) {
        createErrorParameter(endTime, exception);
      } else {
        createOutputParameter(endTime, container, EMPTY_FOR_EACH_INDICES, 0);
      }
    }
    
    if (auditData.containsCompensation()) {
      xmlWriter.writeStartElement(GenerationBase.EL.COMPENSATIONS);
      
      //FIXME der status der compensations sollte über die auftragsdatenbank gelöst werden und nicht im xml stehen.
      xmlWriter.writeAttribute(GenerationBase.ATT.STATUS, "Success");
      
      for (Entry<Integer, Integer> entry : mapStepIdToRefIdForCompensation.entrySet()) {
        addCompensation(entry.getKey(), entry.getValue());
      }
      
      xmlWriter.writeEndElement();
    }
    
    xmlWriter.writeEndElement();
  }
  
  
  private void addCompensation(Integer stepId, Integer refId) throws XMLStreamException {
    StepAuditDataContainer containerWithStepInfoForRelevantStep = auditData.getStepAuditDataContainerByStepId(stepId, false);
    if (containerWithStepInfoForRelevantStep != null) {
      for (StepAuditDataContent x : containerWithStepInfoForRelevantStep.getAllStepAuditDataEntries()) {
        if (x.startCompensateTime != null) {
          xmlWriter.writeStartElement(GenerationBase.EL.COMPENSATEFUNCTION);
          xmlWriter.writeAttribute(GenerationBase.ATT.REFID, String.valueOf(refId));
          if (x.forEachIndices != null && x.forEachIndices.length != 0) {
            xmlWriter.writeAttribute(GenerationBase.ATT.FOREACH_INDICES,
                                     getForEachIndicesAsString(x.forEachIndices));
          }
          if (x.retryCounter > 0) {
            xmlWriter.writeAttribute(GenerationBase.ATT.RETRY_COUNTER,
                                            Long.toString(x.retryCounter));
          }
          if (x.endCompensateTime != null) {
            xmlWriter.writeAttribute(GenerationBase.ATT.STATUS, "Success");
          } else {
            xmlWriter.writeAttribute(GenerationBase.ATT.STATUS, "Running");
          }
          
          xmlWriter.writeEndElement();
        }
      }
    }
  }

}
