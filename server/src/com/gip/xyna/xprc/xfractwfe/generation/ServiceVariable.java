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
package com.gip.xyna.xprc.xfractwfe.generation;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

public class ServiceVariable extends DatatypeVariable {

  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);

  private Service service;


  public ServiceVariable(GenerationBase creator) {
    super(creator);
  }
  
  public ServiceVariable(ServiceVariable original) {
    super(original);
  }
  
  /**
   * Creates a ServiceVariable based on the given DatatypeVariable
   */
  public ServiceVariable(DatatypeVariable datatypeVariable) {
    super(datatypeVariable.getCreator());
    
    // copy content from DatatypeVariable via serialization/deserialization
    try {
      XmlBuilder xml = new XmlBuilder();
      datatypeVariable.appendXML(xml);
      Document document = XMLUtils.parseString(xml.toString());
      Element root = document.getDocumentElement();
      parseXML(root);
    } catch (Exception e) {
      logger.error(e);
    }
  }
  
  // d object mit ggfs s drin
  @Override
  public void parseXML(Element e, boolean includeNullEntries) throws XPRC_InvalidPackageNameException {
    // ggfs service.parse()
    Element s = XMLUtils.getChildElementByName(e, GenerationBase.EL.SERVICEREFERENCE);
    if (s != null) {
      service = new Service(this, creator);
      service.parseXML(s, null);
    }
    // var.parse()
    super.parseXML(e, includeNullEntries);
  }
  
  public Service getService() {
    return service;
  }

}
