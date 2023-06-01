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
package com.gip.xyna.xprc.xprcods.orderarchive.audit;



import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.streams.MemoryEfficientStringWriter;
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class AuditXmlHelper {

  private static final Logger logger = CentralFactoryLogging.getLogger(AuditXmlHelper.class);

  private long currentVersion = 2L;


  private String getFqn(Element auditElement) {
    Element serviceElement = XMLUtils.getChildElementByName(auditElement, EL.SERVICE);
    String typePath = serviceElement.getAttribute(ATT.TYPEPATH);
    String typeName = serviceElement.getAttribute(ATT.TYPENAME);
    
    return typePath + "." + typeName;
  }
  
  /**
   * Parst ein Audit XML
   * @param xml
   * @return
   */
  public EnhancedAudit auditFromXml(String xml) {
    // 
    return auditFromXml(xml, true);
  }
  
  /**
   * Parst ein Audit XML.
   * 
   * @param xml
   * @param mapIntoWfRtc if true, rtc is to the one from the parent workflow for basic and dependent imports (necessary for old GUI)
   * @return
   */
  public EnhancedAudit auditFromXml(String xml, boolean mapIntoWfRtc) {
    if (xml == null) {
      return null;
    }

    Long version;
    String fqn;
    String audit;
    List<AuditImport> imports = new ArrayList<AuditImport>();
    long repositoryRevision = -1;
    RuntimeContext rcWorkflow = null;

    Document doc;
    try {
      doc = XMLUtils.parseString(xml);
    } catch (XPRC_XmlParsingException e1) {
      logger.debug("Could not parse audit", e1);
      return null;
    }
    if (doc.getDocumentElement().getNodeName().equals(EL.ENHANCED_AUDIT)) {
      //v1+
      String v = doc.getDocumentElement().getAttribute(ATT.AUDIT_VERSION);
      version = Long.valueOf(v);
      Element e = XMLUtils.getChildElementByName(doc.getDocumentElement(), EL.AUDIT);
      fqn = getFqn(e);
      
      audit = XMLUtils.getXMLString(XMLUtils.getChildElements(e).get(0), false);

      if (version == 2) {
        rcWorkflow = readRuntimeContextFromAttributes(doc.getDocumentElement());
        int binding = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().getOwnBinding();
        switch (binding) {
          case 1 :
          case 2 :
            String val = doc.getDocumentElement().getAttribute(ATT.REPOSITORY_REVISION + binding);
            if (val != null && val.length() > 0) {
              repositoryRevision = Long.valueOf(val);
              break;
            }
            //fall through -> alter audit
          case 0 :
            repositoryRevision = Long.valueOf(doc.getDocumentElement().getAttribute(ATT.REPOSITORY_REVISION));
            break;
          default :
            throw new RuntimeException();
        }
      }

      List<Element> importList = XMLUtils.getChildElementsByName(doc.getDocumentElement(), EL.IMPORT);
      for (Element imp : importList) {
        if (version == 1) {
          Element documentElement = XMLUtils.getChildElementByName(imp, EL.DOCUMENT);
          String application = getElementValue(documentElement, EL.APPLICATION);
          String applicationVersion = getElementValue(documentElement, EL.VERSION);
          String workspace = getElementValue(documentElement, EL.WORKSPACE);
          RuntimeContext rc = null;
          if (application != null) {
            rc = new Application(application, applicationVersion);
          }
          if (workspace != null) {
            rc = new Workspace(workspace);
          }
          imports.add(new AuditImport(XMLUtils.getXMLString(XMLUtils.getChildElements(documentElement).get(0), false), rc));

        } else if (version == 2) {

          String fqName = imp.getAttribute(ATT.AUDIT_FQNAME);
          RuntimeContext rc = readRuntimeContextFromAttributes(imp);
          if (rc == null) {
            rc = rcWorkflow;
          }
          try {
            String importedXML =
                XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement()
                    .getXMLFromRepository(rc, repositoryRevision, fqName);
            importedXML = readAndOptimizeXml(importedXML);
            
            if (mapIntoWfRtc) {
              String isDep = imp.getAttribute(ATT.AUDIT_RC_IS_DEPENDENCY);
              if (!isDep.equals("false")) { // TODO: nur bei alter GUI
                //indep attribute ist nicht gesetzt oder auf true gesetzt => runtimecontext von workflow übernehmen
                //grund: GUI kennt für audits nicht die rc-dependency-hierarchie. und im xml sind objekte aus der hierarchie in den parametern nicht anders ausgezeichnet
                rc = rcWorkflow;
              }
            }
            
            imports.add(new AuditImport(importedXML, rc, fqName));
          } catch (Exception e1) {
            logger.warn("Could not resolve XMOM XML " + fqName + "@" + repositoryRevision + " referenced in audit", e1);
          }
        }
      }

      //für Modellierung wichtige Datentypen zum Audit hinzufügen
      imports.addAll(BasicAuditImport.getAuditImports(mapIntoWfRtc));
    } else if (doc.getDocumentElement().getNodeName().equals(EL.ORDER_ITEM)) { // Import Flash Audit-Export
      Element e = XMLUtils.getChildElementByName(doc.getDocumentElement(), EL.AUDIT);
      fqn = getFqn(e);
      audit = XMLUtils.getXMLString(XMLUtils.getChildElements(e).get(0), false);

      version = 0L;
      repositoryRevision = 0L;

      List<Element> importList = XMLUtils.getChildElementsByName(doc.getDocumentElement(), EL.IMPORT);
      for (Element imp : importList) {
        Element documentElement = XMLUtils.getChildElementByName(imp, EL.DOCUMENT);

        String application = getElementValue(imp, EL.APPLICATION);
        String applicationVersion = getElementValue(imp, EL.VERSION);
        String workspace = getElementValue(imp, EL.WORKSPACE);

        Element objectElement = XMLUtils.getChildElements(documentElement).get(0);
        String typePath = objectElement.getAttribute(ATT.TYPEPATH);
        String typeName = objectElement.getAttribute(ATT.TYPENAME);

        String objectFqn = typePath + "." + typeName;

        RuntimeContext rc = null;
        if (application != null) {
          rc = new Application(application, applicationVersion);
        }
        if (workspace != null) {
          rc = new Workspace(workspace);
        }
        imports.add(new AuditImport(XMLUtils.getXMLString(objectElement, false), rc, objectFqn));
      }
    } else {
      //davor
      version = 0L;
      fqn = "";
      audit = xml;
    }

    return new EnhancedAudit(version, audit, fqn, imports, repositoryRevision, rcWorkflow);
  }


  private RuntimeContext readRuntimeContextFromAttributes(Element e) {
    String application = e.getAttribute(ATT.APPLICATION);
    String applicationVersion = e.getAttribute(ATT.APPLICATION_VERSION);
    String workspace = e.getAttribute(ATT.WORKSPACE);
    if (application != null && application.length() > 0) {
      return new Application(application, applicationVersion);
    }
    if (workspace != null && workspace.length() > 0) {
      return new Workspace(workspace);
    }
    return null;
  }


  private String getElementValue(Element parent, String elementName) {
    Element e = XMLUtils.getChildElementByName(parent, elementName);
    if (e == null) {
      return null;
    }
    return XMLUtils.getTextContent(e);
  }

  /*
   * TODO
   * Folgende Imports weglassen:
   * Hohe Prio) Resultierend aus Service-Operations einer ServiceGroup, die nicht aufgerufen werden
   * Niedrigere Prio) Resultierend aus Membervariablen, die null sind
   * 
   * Dazu muss sowohl die Ermittlung der Imports angepasst werden als auch das spätere Auswerten der Imports (Service-Groups müssen dann in 
   *   "readAndOptimizeXml" beschnitten werden)
   */

  /**
   * Erzeugt ein EnhancedAudit Xml
   */
  public String auditToXml(String auditXml, long revisionOfAudit, Set<DependencyNode> auditImports) {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext rc;
    try {
      rc = rm.getRuntimeContext(revisionOfAudit);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    RepositoryManagement repm = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryManagement();
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    XmlBuilder xml = new XmlBuilder();
    xml.startElementWithAttributes(EL.ENHANCED_AUDIT);    
    xml.addAttribute(ATT.AUDIT_VERSION, String.valueOf(currentVersion));
    addRuntimeContextAttributes(xml, rc);
    int binding = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().getOwnBinding();
    switch (binding) {
      case 0 :
        xml.addAttribute(ATT.REPOSITORY_REVISION, String.valueOf(repm.getCurrentRepositoryRevision()));
        break;
      case 1 :
        xml.addAttribute(ATT.REPOSITORY_REVISION + 1, String.valueOf(repm.getCurrentRepositoryRevision()));
        xml.addAttribute(ATT.REPOSITORY_REVISION + 2, String
            .valueOf(XynaFactory.getInstance().getIDGenerator().getIdLastUsedByOtherNode(RepositoryManagement.REPOSITORY_IDS_REALM)));
        break;
      case 2 :
        xml.addAttribute(ATT.REPOSITORY_REVISION + 2, String.valueOf(repm.getCurrentRepositoryRevision()));
        xml.addAttribute(ATT.REPOSITORY_REVISION + 1, String
            .valueOf(XynaFactory.getInstance().getIDGenerator().getIdLastUsedByOtherNode(RepositoryManagement.REPOSITORY_IDS_REALM)));
        break;
      default :
        throw new RuntimeException();
    }
    xml.endAttributes();

    xml.element(EL.AUDIT, auditXml);

    for (DependencyNode dn : auditImports) {
      xml.startElementWithAttributes(EL.IMPORT);
      xml.addAttribute(ATT.AUDIT_FQNAME, dn.getUniqueName());
      if (dn.getRevision() != revisionOfAudit) {
        RuntimeContext rce;
        try {
          rce = rm.getRuntimeContext(dn.getRevision());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }
        addRuntimeContextAttributes(xml, rce);
        if (!rcdMgmt.isDependency(revisionOfAudit, dn.getRevision())) {
          xml.addAttribute(ATT.AUDIT_RC_IS_DEPENDENCY, "false");
        }
      }
      xml.endElement();
    }

    xml.endElement(EL.ENHANCED_AUDIT);

    return xml.toString();
  }


  private void addRuntimeContextAttributes(XmlBuilder xml, RuntimeContext rc) {
    if (rc instanceof Application) {
      xml.addAttribute(ATT.APPLICATION, XMLUtils.escapeXMLValue(rc.getName()));
      xml.addAttribute(ATT.APPLICATION_VERSION, XMLUtils.escapeXMLValue(((Application) rc).getVersionName()));
    } else if (rc instanceof Workspace) {
      xml.addAttribute(ATT.WORKSPACE, XMLUtils.escapeXMLValue(rc.getName()));
    }
  }


  /*//ServiceReferences sammeln
  if (name.equals(EL.SERVICEREFERENCE)) {
    String refName = xmlReader.getAttributeValue(null, ATT.REFERENCENAME);
    if (refName.contains(".")) { //Service und nicht Sub-Workflow
      String[] parts = SPLIT_AT_DOT_PATTERN.split(refName);
      refName = parts[0];
      String serviceGoup = xmlReader.getAttributeValue(null, ATT.REFERENCEPATH) + "." + refName;
      String serviceId = xmlReader.getAttributeValue(null, ATT.ID);
      
      serviceReferences.put(serviceGoup, serviceId);
    }
  }
  
  //Operations sammeln
  if (name.equals(EL.INVOKE)) {
    String serviceId = xmlReader.getAttributeValue(null, ATT.SERVICEID);
    Set<String> set = operations.get(serviceId);
    if (set == null) {
      set = new HashSet<String>();
      operations.put(serviceId, set);
    }
    String serviceName = xmlReader.getAttributeValue(null, ATT.INVOKE_OPERATION);
    set.add(serviceName);
  }*/

  private static final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
  private static final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

  public String readAndOptimizeXml(String xml) {
    // Set<String> operations = null; //TODO operations entfernen?!

    boolean write = true;
    String ignoreElement = null;
    DependencySourceType type = null;

    try {
      XMLStreamReader xmlReader;
      synchronized (inputFactory) {
        xmlReader = inputFactory.createXMLStreamReader(new StringReader(xml));
      }
      try {
        MemoryEfficientStringWriter sw = new MemoryEfficientStringWriter();
        XMLStreamWriter xmlWriter;
        synchronized (outputFactory) {
          xmlWriter = outputFactory.createXMLStreamWriter(sw);
        }

        try {
          Stack<String> elementStack = new Stack<String>();

          while (xmlReader.hasNext()) {
            int xmlType = xmlReader.next();
            switch (xmlType) {
              case XMLStreamReader.PROCESSING_INSTRUCTION :
                xmlWriter.writeProcessingInstruction(xmlReader.getPITarget(), xmlReader.getPIData());
                break;
              case XMLStreamReader.START_ELEMENT :
                String name = xmlReader.getLocalName();

                if (write == false && ignoreElement == null) {
                  write = true; //erst hier und nicht schon bei endElement umsetzen, da sonst zu viele Zeilenumbrüche geschrieben werden
                }

                //für Workflows nur Input, Output und Throws behalten
                if (elementStack.size() > 0 && elementStack.firstElement().equals(EL.SERVICE)) {
                  type = DependencySourceType.WORKFLOW;
                  if (elementStack.peek().equals(EL.OPERATION)) {
                    //input, output, throws damit die signatur passt
                    //meta wegen query-spezial workflow in xtf (querywithtestdatasupport), der sonst nicht richtig beim aufrufer angezeigt werden kann
                    if (!name.equals(EL.INPUT) && !name.equals(EL.OUTPUT) && !name.equals(EL.THROWS) && !name.equals(EL.META)) {
                      write = false;
                      ignoreElement = name;
                    }
                  }
                }

                //für ServiceGroups und Instanzmethoden nur verwendete Operations behalten
                /*TODOif (elementStack.size() > 0 && elementStack.firstElement().equals(EL.DATATYPE)) {
                  type = DependencySourceType.DATATYPE;
                  if (elementStack.peek().equals(EL.SERVICE)) {
                    if (name.equals(EL.OPERATION)) {
                      String serviceName = xmlReader.getAttributeValue(null, ATT.OPERATION_NAME);
                      
                      if (operations == null || !operations.contains(serviceName)) {
                        write = false;
                        ignoreElement = name;
                      }
                    }
                  }
                }*/

                if (write) {
                  xmlWriter.writeStartElement(name);                  
                  for (int i = 0; i < xmlReader.getAttributeCount(); i++) {
                    xmlWriter.writeAttribute(xmlReader.getAttributeLocalName(i), xmlReader.getAttributeValue(i));
                  }

                  for (int i = 0; i < xmlReader.getNamespaceCount(); i++) {
                    xmlWriter.writeNamespace(xmlReader.getNamespacePrefix(i), xmlReader.getNamespaceURI(i));
                  }
                }
                elementStack.add(name);
                break;
              case XMLStreamReader.END_ELEMENT :
                name = elementStack.pop();

                if (write) {
                  xmlWriter.writeEndElement();
                }

                if (type == DependencySourceType.WORKFLOW) {
                  if (elementStack.size() > 0) {
                    if (elementStack.peek().equals(EL.OPERATION)) {
                      if (name.equals(ignoreElement)) {
                        ignoreElement = null;
                      }
                    }
                  }
                }

                if (type == DependencySourceType.DATATYPE) {
                  if (elementStack.size() > 0) {
                    if (elementStack.peek().equals(EL.SERVICE)) {
                      if (name.equals(ignoreElement)) {
                        ignoreElement = null;
                      }
                    }
                  }
                }

                break;
              case XMLStreamReader.CDATA :
                if (write) {
                  xmlWriter.writeCData(xmlReader.getText());
                }
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
                if (write) {
                  if (!elementStack.peek().equals(EL.CODESNIPPET)) { //codesnippets braucht man nicht rauszuschreiben
                    xmlWriter.writeCharacters(xmlReader.getText());
                  }
                }
                break;
              default :
                throw new RuntimeException("unsupported type: " + xmlType);
            }
          }

          return sw.getString();
        } finally {
          xmlWriter.flush();
          xmlWriter.close();
          xmlWriter = null;
        }
      } finally {
        xmlReader.close();
        xmlReader = null;
      }
    } catch (XMLStreamException e1) {
      throw new RuntimeException(e1);
    }
  }

}
