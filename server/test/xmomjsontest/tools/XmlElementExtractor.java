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


package xmomjsontest.tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import java.util.ArrayList;
import java.util.List;


public class XmlElementExtractor {

  public static class XPathValueData {
    public String xpath;
    public String value;
    public String attributeName;
    public boolean isAttribute = false;
    
    public XPathValueData(String xpath, String value) {
      this.xpath = xpath;
      this.value = value;
    }
    
    public XPathValueData(String xpath, String value, String attribute) {
      this.xpath = xpath;
      this.value = value;
      this.attributeName = attribute;
      this.isAttribute = true;
    }
    
    
    @Override
    public String toString() {
      return xpath + " = " + value;
    }
  }
  
  
  public static List<XPathValueData> extractAllElements(Document doc) {
    List<XPathValueData> pairs = new ArrayList<>();
    Element root = doc.getDocumentElement();
    
    if (root != null) {
      extractElementsRecursive(root, "", pairs);
    }
    
    return pairs;
  }

  
  private static void extractElementsRecursive(Element element, String parentXPath,
                                               List<XPathValueData> pairs) {
    // Build XPath for current element
    String elementXPath = buildXPath(element, parentXPath);
    
    // Add element with its text content
    String textContent = getElementTextContent(element);
    if (textContent != null && !textContent.trim().isEmpty()) {
      pairs.add(new XPathValueData(elementXPath, textContent.trim()));
    }
    
    // Extract all attributes
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attrNode = attributes.item(i);
      String attrXPath = elementXPath + "/@" + attrNode.getNodeName();
      pairs.add(new XPathValueData(attrXPath, attrNode.getNodeValue(), attrNode.getNodeName()));
    }
    
    // Process child elements
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        extractElementsRecursive((Element) child, elementXPath, pairs);
      }
    }
  }

  
  private static String buildXPath(Element element, String parentXPath) {
    String tagName = element.getTagName();
    
    if (parentXPath == null || parentXPath.isEmpty()) {
      return "/" + tagName;
    }
    
    // Find position of this element among siblings with the same name
    int position = 1;
    Node previousSibling = element.getPreviousSibling();
    while (previousSibling != null) {
      if (previousSibling.getNodeType() == Node.ELEMENT_NODE && 
          previousSibling.getNodeName().equals(tagName)) {
        position++;
      }
      previousSibling = previousSibling.getPreviousSibling();
    }
    return parentXPath + "/" + tagName + "[" + position + "]";
  }

  
  private static String getElementTextContent(Element element) {
    StringBuilder textContent = new StringBuilder();
    NodeList children = element.getChildNodes();
    
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE) {
        String text = child.getNodeValue();
        if (text != null) {
          textContent.append(text);
        }
      }
    }
    
    return textContent.toString();
  }

  

  // Example usage
  public static void main(String[] args) throws Exception {
    // Example: parse XML and extract all elements/attributes
    String xml = "<root><element attr='value'>text</element></root>";
    Document doc = XMLUtils.parseString(xml, true);
    
    List<XPathValueData> pairs = extractAllElements(doc);
    for (XPathValueData pair : pairs) {
      System.out.println(pair);
    }
  }
  
}
