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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class UpdateQueryMappingEscaping extends UpdateXMOMXMLs {

  private static final Pattern dynamicValueToAdjust = Pattern.compile("(?<=,)%[0-9]+%[^,)]*(?=[,)])");
  private final XPath xpathObj;
  private final XPathExpression findPersistenceServiceId;
  
  
  public UpdateQueryMappingEscaping(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses, false, false, false, new XMOMType[] {XMOMType.WORKFLOW}, XMLLocation.BOTH);
    String findPersistenceServiceID = "//ServiceReference[@ReferenceName='PersistenceServices.PersistenceServices'][@ReferencePath='xnwh.persistence']";
    XPathFactory factory = XPathFactory.newInstance();
    xpathObj = factory.newXPath();
    try {
      findPersistenceServiceId = xpathObj.compile(findPersistenceServiceID);
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  
  // Barcode explanation see Bug 16445 
  private static void adjustMapping(Node node) {
    String textContent = node.getTextContent();
    Matcher m = dynamicValueToAdjust.matcher(textContent);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      m.appendReplacement(sb, "replaceall(replaceall(" + m.group(0) +
                          ",\"\\\\\\\\\\\\\\\\\",\"\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"),\"\\\\\\\"\",\"\\\\\\\\\\\\\\\\\\\\\"\")");
    }
    m.appendTail(sb);
    node.setTextContent(sb.toString());
  }

  
  
  private List<Node> findRelevantMappings(Document document) throws XPathExpressionException {
    Object result = findPersistenceServiceId.evaluate(document, XPathConstants.NODESET);
    NodeList persistenceServiceReferences = (NodeList) result;
    List<Node> mappings = new ArrayList<Node>();
    for (int i = 0; i < persistenceServiceReferences.getLength(); i++) {
      Node node = persistenceServiceReferences.item(i);
      Node refIdAttrib = node.getAttributes().getNamedItem("ID");
      String findQueryInvocationAndSelectPreceding = 
        "//Invoke[@ServiceID='" + refIdAttrib.getNodeValue() + "'][@Operation='query']/../preceding-sibling::*[1][Meta/IsCondition/text()='true']/" + GenerationBase.EL.MAPPING;
      XPathExpression expr = xpathObj.compile(findQueryInvocationAndSelectPreceding);
      result = expr.evaluate(document, XPathConstants.NODESET);
      NodeList queryMappings = (NodeList) result;
      for (int j = 0; j < queryMappings.getLength(); j++) {
        Node mappingNode = queryMappings.item(i);
        mappings.add(mappingNode);
      }
    }
    return mappings;
  }


  @Override
  void adjust(Document doc) {
    List<Node> nodes;
    try {
      nodes = findRelevantMappings(doc);
      for (Node node : nodes) {
        adjustMapping(node);
      }
    } catch (XPathExpressionException e) {
      logger.warn("could not update document", e);
    }
    
  }

}
