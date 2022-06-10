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
package com.gip.xyna.utils.xml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLParseException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class ComparerTest extends TestCase {

   private static final String XML_FILE = System.getProperty("user.dir")
         + "/test/DummyRequest.xml";
   private static final String XML_FILE2 = System.getProperty("user.dir")
         + "/test/DummyRequest2.xml";
   private static final String XML_FILE3 = System.getProperty("user.dir")
         + "/test/DummyRequest3.xml";;

   private Document document = null;
   private Comparer comparer = null;

   public void setUp() throws XMLParseException, FileNotFoundException,
         SAXException, IOException {
      comparer = new Comparer();
      DOMParser parser = new DOMParser();
      parser.parse(new FileReader(XML_FILE));
      document = parser.getDocument();
   }

   public void testCompareNodes_NoNodes() throws Exception {
      assertNull(comparer.compareNodes(new Node[] {}));
   }

   public void testCompareNodes_SingleNode() throws Exception {
      Node requestNode = document.getElementsByTagName("addOrderRequest").item(
            0);
      Node resultNode = comparer
            .compareNodes(new Node[] { requestNode });
      assertNotNull(resultNode);
      // TODO: checkStructure_ResultNode(requestNode, resultNode);
   }

   public void testCompareNodes_EqualNodes() throws Exception {
      Node requestNode = document.getElementsByTagName("addOrderRequest").item(
            0);
      Node resultNode = comparer.compareNodes(new Node[] { requestNode,
            requestNode });
      checkStructure_ResultNode(requestNode, resultNode);
      checkValues_AllEqual(resultNode);
   }

   private void checkStructure_ResultNode(Node requestNode, Node resultNode) {
      // FIXME: assertEquals(requestNode.getLocalName(),
      // resultNode.getLocalName());
      // FIXME: assertEquals(requestNode.getNodeName(),
      // resultNode.getNodeName());
      assertEquals(requestNode.getNodeType(), resultNode.getNodeType());
      assertEquals(null, resultNode.getNodeValue());
      assertEquals(0, resultNode.getAttributes().getLength());
      assertEquals(5, resultNode.getChildNodes().getLength()); // TODO: check
      // Text
      Node resultText = resultNode.getChildNodes().item(0);
      assertEquals(Node.TEXT_NODE, resultText.getNodeType());
      // XynaHeader
      Node requestHeader = requestNode.getChildNodes().item(1);
      Node resultHeader = resultNode.getChildNodes().item(1);
      // FIXME: assertEquals(requestHeader.getLocalName(),
      // resultHeader.getLocalName());
      // FIXME: assertEquals(requestHeader.getNodeName(),
      // resultHeader.getNodeName());
      assertEquals(requestHeader.getNodeType(), resultHeader.getNodeType());
      assertEquals(null, resultHeader.getNodeValue());
      assertEquals(9, resultHeader.getAttributes().getLength());
      checkStructure_HeaderAttributes(resultNode);
      // No Text
      Node resultFirstNoText = resultNode.getChildNodes().item(2);
      assertEquals(Node.TEXT_NODE, resultFirstNoText.getNodeType());
      // Order
      Node requestOrder = requestNode.getChildNodes().item(3);
      Node resultOrder = resultNode.getChildNodes().item(3);
      // FIXME: assertEquals(requestOrder.getLocalName(),
      // resultOrder.getLocalName());
      // FIXME: assertEquals(requestOrder.getNodeName(),
      // resultOrder.getNodeName());
      assertEquals(requestOrder.getNodeType(), resultOrder.getNodeType());
      assertEquals(null, resultOrder.getNodeValue());
      assertEquals(0, resultOrder.getAttributes().getLength());
      // TODO: expand test
   }

   private void checkStructure_HeaderAttributes(Node resultNode) {
      Node resultHeader = resultNode.getChildNodes().item(1);
      NamedNodeMap attributes = resultHeader.getAttributes();
      assertNotNull(attributes.getNamedItem("OrderNumber"));
      assertNotNull(attributes.getNamedItem("OrderType"));
      assertNotNull(attributes.getNamedItem("Department"));
      assertNotNull(attributes.getNamedItem("CreationDate"));
      assertNotNull(attributes.getNamedItem("StartTime"));
      assertNotNull(attributes.getNamedItem("StartTimeSlot"));
      assertNotNull(attributes.getNamedItem("Information"));
      assertNotNull(attributes.getNamedItem("ProcessId"));
      assertNotNull(attributes.getNamedItem("Reference"));
   }

   private void checkValues_AllEqual(Node resultNode) {
      // Text nodes
      assertEquals("allEqual", resultNode.getChildNodes().item(0)
            .getNodeValue());
      assertEquals("allEqual", resultNode.getChildNodes().item(2)
            .getNodeValue());
      // Header attributes
      NamedNodeMap headerAttributes = resultNode.getChildNodes().item(1)
            .getAttributes();
      assertEquals("allEqual", headerAttributes.getNamedItem("OrderNumber")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("OrderType")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("Department")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("CreationDate")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("StartTime")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("StartTimeSlot")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("Information")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("ProcessId")
            .getNodeValue());
      assertEquals("allEqual", headerAttributes.getNamedItem("Reference")
            .getNodeValue());
      // TODO: check other attributes
   }

   public void testCompareNodes_DifferentNodes() throws Exception {
      DOMParser parser = new DOMParser();
      parser.parse(new FileReader(XML_FILE2));
      Document document2 = parser.getDocument();
      Node requestNode = document.getElementsByTagName("addOrderRequest").item(
            0);
      Node requestNode2 = document2.getElementsByTagName("addOrderRequest")
            .item(0);
      Node resultNode = comparer.compareNodes(new Node[] { requestNode,
            requestNode2 });
      checkStructure_ResultNode(requestNode, resultNode);
      checkValues_AllUnique(resultNode);
   }

   private void checkValues_AllUnique(Node resultNode) {
      // Text nodes
      assertEquals("allUnique", resultNode.getChildNodes().item(0)
            .getNodeValue());
      assertEquals("allEqual", resultNode.getChildNodes().item(2)
            .getNodeValue());
      // Header attributes
      Node resultHeader = resultNode.getChildNodes().item(1);
      NamedNodeMap attributes = resultHeader.getAttributes();
      assertEquals("allUnique", attributes.getNamedItem("OrderNumber")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("OrderType")
            .getNodeValue());
      assertEquals("allUnique", attributes.getNamedItem("Department")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("CreationDate")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("StartTime")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("StartTimeSlot")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("Information")
            .getNodeValue());
      assertEquals("allUnique", attributes.getNamedItem("ProcessId")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("Reference")
            .getNodeValue());
      // TODO: check other attributes
   }

   public void testCompareNodes_MoreDifferentNodes() throws Exception {
      DOMParser parser = new DOMParser();
      parser.parse(new FileReader(XML_FILE2));
      Document document2 = parser.getDocument();
      Node requestNode = document.getElementsByTagName("addOrderRequest").item(
            0);
      Node requestNode2 = document2.getElementsByTagName("addOrderRequest")
            .item(0);
      Node resultNode = comparer.compareNodes(new Node[] { requestNode,
            requestNode, requestNode2 });
      checkStructure_ResultNode(requestNode, resultNode);
      checkValues_SomeDifferent(resultNode);
   }

   private void checkValues_SomeDifferent(Node resultNode) {
      // Text nodes
      assertEquals("someDifferent", resultNode.getChildNodes().item(0)
            .getNodeValue());
      assertEquals("allEqual", resultNode.getChildNodes().item(2)
            .getNodeValue());
      // Header attributes
      Node resultHeader = resultNode.getChildNodes().item(1);
      NamedNodeMap attributes = resultHeader.getAttributes();
      assertEquals("someDifferent", attributes.getNamedItem("OrderNumber")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("OrderType")
            .getNodeValue());
      assertEquals("someDifferent", attributes.getNamedItem("Department")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("CreationDate")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("StartTime")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("StartTimeSlot")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("Information")
            .getNodeValue());
      assertEquals("someDifferent", attributes.getNamedItem("ProcessId")
            .getNodeValue());
      assertEquals("allEqual", attributes.getNamedItem("Reference")
            .getNodeValue());
      // TODO: check other attributes
   }

   public void testCompareNodes_DifferentStructure() throws XMLParseException,
         FileNotFoundException, SAXException, IOException {
      DOMParser parser = new DOMParser();
      parser.parse(new FileReader(XML_FILE3));
      Document document3 = parser.getDocument();
      Node requestNode = document.getElementsByTagName("addOrderRequest").item(
            0);
      Node requestNode3 = document3.getElementsByTagName("addOrderRequest")
            .item(0);
      Node resultNode = null;
      boolean exceptionThrown = false;
      try {
         resultNode = comparer.compareNodes(new Node[] { requestNode,
               requestNode3 });
      } catch (Exception e) {
         exceptionThrown = true;
         assertNull(resultNode);
      }
      assertTrue(exceptionThrown);
   }
   
   //TODO: test equal named attributes with different namespaces

}
