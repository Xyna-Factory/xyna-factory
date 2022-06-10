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
package com.gip.xyna.xdev.map.xmlparser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/**
 * klassen zum kapseln von xmlparsing und xsdparsing in einem, damit man sowas machen kann wie:
 * im geparsten xml zu jeder zeit den typ abfragen, den das element nach dem xsd hat.
 */
public class XDocument {

  private Document doc;
  private XElement docEl;
  private XSchema schema;
  
  public XDocument(String payload) {
    try {
      doc = XMLUtils.parseString(payload, true);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException(e);
    }
    docEl = new XElement(doc.getDocumentElement(), this);
  }

  public XDocument(Element element) {
    doc = element.getOwnerDocument();
    docEl = new XElement(element, this);
  }

  public static XDocument parseString(String payload) {
    return new XDocument(payload);
  }

  public XElement getDocumentElement() {
    return docEl;
  }

  public void setSchema(XSchema schema) {
    this.schema = schema;
    docEl.initSchema();
  }
  
  XSchema getSchema() {
    return schema;
  }

  public static XDocument fromElement(Element element) {
    return new XDocument(element);
  }
  

}
