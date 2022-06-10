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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class XElement extends XNode {

  private Element element;
  private XSComplexType schemaElement;
  
  public XElement(Element element, XDocument xDocument) {
    super(xDocument);
    if (xDocument.getSchema() != null) {
      XSchema schema = xDocument.getSchema();
      schemaElement = schema.getElement(element.getNamespaceURI(), element.getLocalName());
    }
    this.element = element;
  }

  public XElement(Element element, XElement xElement) {
    super(xElement.ownerDoc);
    if (ownerDoc.getSchema() != null) {
      schemaElement = xElement.schemaElement.getChildType(element.getNamespaceURI(), element.getLocalName());
    }
    this.element = element;
  }

  public String getNamespaceURI() {
    return element.getNamespaceURI();
  }

  public String getAttribute(String att) {
    return element.getAttribute(att);
  }

  public String getLocalName() {
    return element.getLocalName();
  }

  public List<XElement> getChildElements() {
    List<Element> children = XMLUtils.getChildElements(element);
    List<XElement> xChildren = new ArrayList<XElement>();
    for (Element e : children) {
      xChildren.add(new XElement(e, this));
    }
    return xChildren;
  }

  public NamedNodeMap getAttributes() {
    return element.getAttributes();
  }

  public XElement getChildElementByName(String tagName) {
    List<Element> children = XMLUtils.getChildElements(element);
    for (Element e : children) {
      if (e.getLocalName().equals(tagName)) {
        return new XElement(e, this);
      }
    }
    return null;
  }

  public String getTextContent() {
    return XMLUtils.getTextContent(element);
  }

  public String getTypeNameUsedInTypeMapping() {
    String typeName = schemaElement.getTypeName();
    if(typeName == null){
      //anonymer type -> im typemapping wird als typ der elementname angegeben
      typeName = getLocalName();
    }
    return typeName;
  }

  void initSchema() {
    XSchema schema = ownerDoc.getSchema();
    schemaElement = schema.getElement(element.getNamespaceURI(), element.getLocalName());
  }

  public boolean isComplexType() {
    return schemaElement.isComplexType();
  }

  /**
   * alle Attribute eines Elements entfernen
   */
  public void removeAttributes() {
    NamedNodeMap attributesMap = element.getAttributes();
    for (int i = 0; i < attributesMap.getLength(); i++) {
      Attr attribute = (Attr) attributesMap.item(i);
      element.removeAttributeNode(attribute);
    }
  }
}
