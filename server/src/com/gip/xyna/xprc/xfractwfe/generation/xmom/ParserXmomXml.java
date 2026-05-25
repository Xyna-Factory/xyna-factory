/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class ParserXmomXml {

  public XmomTree build(String xml) {
    try {
      Document doc = XMLUtils.parseString(xml, true);
      return build(doc);
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not parse xmom xml.");
    }
  }
  
  
  public XmomTree build(Document doc) {
    Element elem = doc.getDocumentElement();
    IdMapping idMapping = new IdMapping();
    Optional<XmomNodeInfo> root = handleNode(elem, idMapping);
    if (root.isEmpty()) {
      throw new IllegalArgumentException("Could not parse xmom xml.");
    }
    return new XmomTree(root.get());
  }
  
  
  private Optional<XmomNodeInfo> handleNode(Node node, IdMapping idMapping) {
    XmomNodeInfo ret = null;
    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      ret = new XmomNodeInfo(node.getLocalName(), node.getNodeValue(), idMapping);
    } else if (node.getNodeType() == Node.ELEMENT_NODE) {
      Optional<String> value = readElementValue(node, idMapping);
      ret = new XmomNodeInfo(node.getLocalName(), value, idMapping);
      handleChildren(node, ret, idMapping);
    }
    return Optional.ofNullable(ret);
  }
  
  
  private Optional<String> readElementValue(Node node, IdMapping idMapping) {
    String value = "";
    NodeList children = node.getChildNodes();
    if (children == null) { return null; }
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() != Node.TEXT_NODE) { continue; }
      String text = child.getNodeValue();
      if (text == null) { continue; }
      text = text.trim();
      if (text.isEmpty()) { continue; }
      value += child.getNodeValue();
    }
    if (value.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(value);
  }
  
  
  public void handleChildren(Node node, XmomNodeInfo xmom, IdMapping idMapping) {
    NodeList children = node.getChildNodes();
    if (children != null) {
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        if (child.getNodeType() == Node.ELEMENT_NODE) {
          addChild(xmom, child, idMapping);
        } else if (child.getNodeType() == Node.ATTRIBUTE_NODE) {
          addChild(xmom, child, idMapping);
        }
      }
    }
    NamedNodeMap attributes = node.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attrNode = attributes.item(i);
        addChild(xmom, attrNode, idMapping);
      }
    }
  }
  
  
  private void addChild(XmomNodeInfo parent, Node child, IdMapping idMapping) {
    Optional<XmomNodeInfo> info = handleNode(child, idMapping);
    if (info.isPresent()) {
      parent.addChild(info.get());
    }
  }

}
