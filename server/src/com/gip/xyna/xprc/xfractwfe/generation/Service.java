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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall;

public class Service {

  private static final Pattern SPLIT_AT_DOT_PATTERN = Pattern.compile("\\.");

  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);


  private String serviceName;
  private String servicePath;
  private String documentation = "";

  private String className;
  private String originalFqName;
  private String fqClassName;
  private String varName;
  private String id;
  private DOM dom;
  private WF wf;
  private DatatypeVariable var;
  private boolean isDOMRef;
  protected GenerationBase creator;
  private boolean prototype = false;
  
  private ArrayList<AVariable> inputVars = new ArrayList<AVariable>();
  private ArrayList<AVariable> outputVars = new ArrayList<AVariable>();
  
  
  public Service(GenerationBase creator) {
    this.creator = creator;
  }
  

  public Service(DatatypeVariable v, GenerationBase creator) {
    this(creator);
    var = v;
  }
  

  public void parseXML(Element e, Element functionObjectElement) throws XPRC_InvalidPackageNameException {
    //parse s
    id = e.getAttribute(com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT.ID);
    //falls servicereferenz auf datentyp geht, ist der referencename immer der form <datentyp-name>.<service-name>
    //falls servicereferenz auf workflow (direht) geht, ist der referencename immer der form <workflow-name>
    //falls servicereferenz auf prototypischen nicht real als eigenes xml vorhandenen service geht, wird dies separat behandelt.
    
    String refName = e.getAttribute(GenerationBase.ATT.REFERENCENAME);
    servicePath = e.getAttribute(GenerationBase.ATT.REFERENCEPATH);
    if (functionObjectElement != null) {
      //abstrakter service vorhanden?
      Element serviceEl = XMLUtils.getChildElementByName(functionObjectElement, GenerationBase.EL.SERVICE);
      if (serviceEl != null) {
        if (XMLUtils.isTrue(serviceEl, GenerationBase.ATT.ABSTRACT)) {
          serviceName = refName;
          prototype = true;
          
          Element operation = XMLUtils.getChildElementByName(serviceEl, com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL.OPERATION);
          if (operation != null) {
            inputVars.addAll( parseInputOutput(operation, GenerationBase.EL.OPERATION, GenerationBase.EL.INPUT, creator) );
            outputVars.addAll( parseInputOutput(operation, GenerationBase.EL.OPERATION, GenerationBase.EL.OUTPUT, creator) );
            
            Element metaElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.META);
            if (metaElement != null) {
              Element documentationElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DOCUMENTATION);
              if (documentationElement != null) {
                documentation = XMLUtils.getTextContent(documentationElement);
              }
            }
          }
          return;
        }
      }
    }
    if (refName.contains(".")) {
      isDOMRef = true;
      String[] parts = SPLIT_AT_DOT_PATTERN.split(refName);
      serviceName = parts[1];
      refName = parts[0];
      originalFqName = servicePath + "." + refName;
      dom = creator.getCachedDOMInstanceOrCreate(originalFqName, creator.revision);
      fqClassName = dom.getFqClassName();
    } else {
      serviceName = refName;
      isDOMRef = false;
      originalFqName = servicePath + "." + refName;
      wf = creator.getCachedWFInstanceOrCreate(originalFqName, creator.revision);
      fqClassName = wf.getFqClassName();
    }
    className = GenerationBase.getSimpleNameFromFQName(fqClassName);
    varName = e.getAttribute(com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT.VARIABLENAME);
  }
  
  
  public static List<AVariable> parseInputOutput(Element el, String parentElementName, String elementName, GenerationBase creator) throws XPRC_InvalidPackageNameException {
    return parseInputOutput(el, parentElementName, elementName, creator, false);
  }

  public static List<AVariable> parseInputOutput(Element el, String parentElementName, String elementName, GenerationBase creator, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    Element ioElements = XMLUtils.getChildElementByName(el, elementName);
    if (ioElements == null) {
     return Collections.emptyList();
    }
    List<AVariable> list = new ArrayList<AVariable>();
    for (Element d : XMLUtils.getChildElements(ioElements) ) {
      AVariable v = null;
      if (d.getTagName().equals(GenerationBase.EL.DATA)) {
        v = new ServiceVariable(creator);
      } else if (d.getTagName().equals(GenerationBase.EL.EXCEPTION)) {
        v = new ExceptionVariable(creator);
      } else {
        continue;
      }
      v.parseXML(d, includeNullEntries);
      list.add(v);
    }
    return list;
  }

  public GenerationBase getCreator() {
    return creator;
  }

  /**
   * falls true, sind keine attribute ausser {@link #id} und {@link #serviceName} gesetzt
   */
  public boolean isPrototype() {
    return prototype;
  }

  protected String getFQClassName() {
    return fqClassName;
  }

  public DOM getDom() {
    return dom;
  }
  
  public WF getWF() {
    return wf;
  }
  
  public boolean isDOMRef() {
    return isDOMRef;
  }
  
  public String getLabel() {
    if (isDOMRef) {
      return dom.getLabel();
    } else if (wf != null) {
      return wf.getLabel();
    }
    
    return null;
  }

  public String getId() {
    return id;
  }

  public String getClassName() {
    return className;
  }

  public String getEventuallyQualifiedClassName(HashSet<String> relevantImports) {
    if (relevantImports.contains(fqClassName)) {
      return className;
    } else {
      return fqClassName;
    }
  }

  public DatatypeVariable getVariable() {
    return var;
  }

  public String getServiceName() {
    return serviceName;
  }

  /*
   * returns the service name, including the name of the wrapping data type, if existing
   */
  public String getFullServiceName() {
    if ( (className != null) && (className.length() > 0) ) {
      return className + "." + serviceName;
    } else {
      return serviceName;
    }
  }
  
  
  public String getServicePath() {
    return servicePath;
  }
  
  
  public String getOriginalFqName() {
    return originalFqName;
  }
  
  static Service getRemoteCallService(String id, GenerationBase creator) {
    Service service = new Service(creator);
    service.id = id;
    service.servicePath = "xprc.xpce";
    service.isDOMRef = true;
    service.serviceName = "RemoteCall";
    service.originalFqName = RemoteCall.FQ_XML_NAME;
    try {
      service.dom = creator.getCachedDOMInstanceOrCreate(service.originalFqName, creator.revision);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    service.fqClassName = service.dom.getFqClassName();
    return service;
  }


  static String generateRemoteCallVarName(Step s) {
    return "remoteCall" + s.getIdx();
  }

  public void createEmpty(String id) {
    this.id =  id;
    this.serviceName = "AbstractService";
    this.prototype = true;
  }

  public void createOperation(String id, DOM dom, Operation operation) {
    this.id =  id;
    this.servicePath = dom.getOriginalPath();
    this.isDOMRef = true;
    this.serviceName = dom.getServiceName(operation);
    this.originalFqName = dom.getOriginalFqName();
    this.fqClassName = dom.getFqClassName();
    this.className = GenerationBase.getSimpleNameFromFQName(fqClassName);
    this.dom = dom;
  }
  
  public void createCall(String id, WF wf) {
    this.id = id;
    this.serviceName = wf.getOriginalSimpleName();
    this.servicePath = wf.getOriginalPath();
    this.wf = wf;
  }

  public List<AVariable> getInputVars() {
    return inputVars;
  }

  public ArrayList<AVariable> getOutputVars() {
    return outputVars;
  }

  public String getDocumentation() {
    return documentation;
  }


  @Override
  public String toString() {
    return servicePath + "." + serviceName;
  }

  
}
