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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public enum PersistenceTypeInformation implements XmlAppendable {

  UNIQUE_IDENTIFIER("UniqueIdentifier"),
  HISTORIZATION_TIMESTAMP("HistorizationTimeStamp"),
  CURRENTVERSION_FLAG("CurrentVersionFlag"),
  CUSTOMFIELD_0("CustomField0"),
  CUSTOMFIELD_1("CustomField1"),
  CUSTOMFIELD_2("CustomField2"),
  CUSTOMFIELD_3("CustomField3"),
  TRANSIENCE("Transience"),
  AUTO_INCREMENT("AutoIncrement"); // TODO sic
  
  private final static String PERSISTENCETYYPE_META_TAG = "Type";
  
  private final String xmlRepresentation;
  
  private PersistenceTypeInformation(String xmlRepresentation) {
    this.xmlRepresentation = xmlRepresentation;
  }
  
  
  protected static Set<PersistenceTypeInformation> parse(Element metaElement) {
    Element persistenceElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.PERSISTENCE);
    if (persistenceElement != null) {
      List<Element> types = XMLUtils.getChildElementsByName(persistenceElement, PERSISTENCETYYPE_META_TAG);
      Set<PersistenceTypeInformation> persTypes = new HashSet<PersistenceTypeInformation>();
      for (Element typeElement : types) {
        PersistenceTypeInformation type = getPersistenceTypeInformationByXmlRepresentation(XMLUtils.getTextContent(typeElement));
        if (type != null) {
          persTypes.add(type);
        }
      }
      if (persTypes.size() > 0) {
        if ((persTypes.size() == 2 && !persTypes.contains(TRANSIENCE)) ||  persTypes.size() > 2) { // TODO allow other type combinations
          throw new RuntimeException("Invalid PersistenceType combination: " + persTypes);
        }
        return persTypes;
      }
    }
    return null;
  }
  
  
  public static PersistenceTypeInformation getPersistenceTypeInformationByXmlRepresentation(String xmlRepresentation) {
    for (PersistenceTypeInformation type : values()) {
      if (type.xmlRepresentation.equals(xmlRepresentation)) {
        return type;
      }
    }
    return null;
  }


  @Override
  public void appendXML(XmlBuilder xml) {
    xml.element(PERSISTENCETYYPE_META_TAG, XMLUtils.escapeXMLValueAndInvalidChars(xmlRepresentation, false, false));
  }

}
