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
package com.gip.xyna.utils.xml;

import java.util.Hashtable;
import java.util.Iterator;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

// TODO: implement against interface not implementation
/**
 * Compares two or more XML tree. The structure of all trees must be the same
 * only values of text nodes or attribute values may be different.
 * <p>
 * The result is given as a new XML tree. The tree has the same structure as to
 * original. Text node values and attribute values are replaced by a constant
 * from the Result enumeration.
 * <p>
 * If all XML trees have the same value at a specific place the constant
 * ALL_EQUAL is insert there for result.
 * <p>
 * If all XML tress have different values (none of two values are equal) at a
 * specific place the constant MUTUALLY_DISTINCT is insert there for result.
 * <p>
 * If some XML trees but not all have the same value at a specific place the
 * constant PARTIALLY_EQUAL is insert there for result.
 */
public class Comparer {

   public static class Result {

      private Result() {
      }

      /**
       * Indicates that all found values are equal.
       */
      public static final Result ALL_EQUAL = new Result();
      /**
       * Indicates that some of the found values are equal but not all.
       */
      public static final Result PARTIALLY_EQUAL = new Result();
      /**
       * Indicates that all found values are different.
       */
      public static final Result MUTUALLY_DISTINCT = new Result();
   }

   /**
    * �berpr�ft xmlfragmente auf (un)gleichheit und gibt ein genauso
    * strukturiertes xml zur�ck, welches als werte das ergebnis bzgl dem
    * jeweiligen feld hat.
    * 
    * @param inputNodes
    *              zu vergleichende xmls
    * @return Node vom gleichen typ wie die Input-Nodes.
    * @throws Exception
    *               falls die fragmente strukturell nicht gleich sind wird eine
    *               erkl�rende exception geworfen (unterschiedlicher namespace,
    *               unterschiedliche struktur etc)
    */
   public Node compareNodes(Node[] inputNodes) throws Exception {
      if (inputNodes.length == 0) {
         return null;
      }
      // TODO: copy first input node and use it as result node
      Node resultNode = initializeResultNode(inputNodes);
      Hashtable<String, Boolean> attrEquals = new Hashtable<String, Boolean>();
      Hashtable<String, Boolean> attrDifferent = new Hashtable<String, Boolean>();
      // TODO: only compare with result/first node
      for (int i = 0; i < inputNodes.length - 1; i++) {
         if (!haveSameStructure(inputNodes[i], inputNodes[i + 1])) {
            throw new Exception("XML-Struktur unterschiedlich bei Node "
                  + inputNodes[i].getLocalName() + " " + i + " und Node "
                  + inputNodes[i + 1].getLocalName() + " " + (i + 1));
         }
         if (!haveSameNamespace(inputNodes[i], inputNodes[i + 1])) {
            throw new Exception("Element " + inputNodes[i].getNodeName() + " "
                  + i + " hat anderen Namespace als " + (i + 1));
         }
         // compare node values
         String resultValue = compareNodeValues(inputNodes[i],
               inputNodes[i + 1]);
         setResultNodeValue(resultNode, resultValue);

         //HashMap attributeValues = compareAttributes(inputNodes[i], inputNodes[i + 1]);
         // setResultNodeAttributes(resultNode, attributeValues);

         // attribute vergleichen
         NamedNodeMap attr1 = inputNodes[i].getAttributes();
         NamedNodeMap attr2 = inputNodes[i + 1].getAttributes();
         if (attr1 != null && attr2 != null) {
            // namespace-attribute entfernen, die wurden beim
            // namespace-vergleich abgefragt
            cleanAttributes(attr1);
            cleanAttributes(attr2);
            if (attr1.getLength() != attr2.getLength()) {
               throw new Exception("Element " + inputNodes[i].getNodeName()
                     + " " + i + " hat nicht gleichviele Attribute wie "
                     + (i + 1));
            }
            // alle attribute in attr1 checken gegen attr2
            for (int j = 0; j < attr1.getLength(); j++) {
               Node a2Node = attr2.getNamedItem(attr1.item(j).getLocalName());
               if (a2Node == null) {
                  // attribut in attr2 nicht vorhanden
                  throw new Exception("Attribut " + attr1.item(j).getNodeName()
                        + " bei Element " + inputNodes[i + 1].getNodeName()
                        + " " + (i + 1) + " nicht gefunden.");
               }
               if (attr1.item(j).getNodeValue().equals(a2Node.getNodeValue())) {
                  attrDifferent.put(attr1.item(j).getLocalName(), Boolean
                        .valueOf(false));
                  // beachte: wird nur auf false gesetzt. => kein eintrag in
                  // hash entspricht true.
               } else {
                  attrEquals.put(attr1.item(j).getLocalName(), Boolean
                        .valueOf(false));
               }
            }

         } else if ((attr1 != null && attr2 == null)
               || (attr1 == null && attr2 != null)) {
            throw new Exception(
                  "Attribute erwartet aber nicht gefunden bei Element "
                        + inputNodes[i].getNodeName() + " " + i + " & "
                        + (i + 1));
         }
         /*
          * for (int k = 0; k < inputNodes[i].getAttributes().getLength(); k++) {
          * ((XMLElement) resultNode).setAttribute(attrName,
          * getCompareIdentifier(attrEquals.get(attrName) == null ? true :
          * false, false)); }
          */
         // namespaces vergleichen?
      }
      // r�ckgabe node bef�llen
      if (resultNode.getNodeType() == Node.ELEMENT_NODE) {
         // attribute hinzuf�gen
         Iterator<String> it = attrDifferent.keySet().iterator();
         while (it.hasNext()) {
            String attrName = it.next();
            ((XMLElement) resultNode).setAttribute(attrName,
                  getCompareIdentifier(attrEquals.get(attrName) == null ? true
                        : false, false));
         }
         it = attrEquals.keySet().iterator();
         while (it.hasNext()) {
            String attrName = it.next();
            ((XMLElement) resultNode).setAttribute(attrName,
                  getCompareIdentifier(false,
                        attrDifferent.get(attrName) == null ? true : false));
         }
      }
      // rekursion �ber kinder
      for (int i = 0; i < inputNodes[0].getChildNodes().getLength(); i++) {
         Node[] kids = new Node[inputNodes.length];
         for (int j = 0; j < inputNodes.length; j++) {
            kids[j] = inputNodes[j].getChildNodes().item(i);
         }
         Node result = compareNodes(kids);
         if (result == null) {
            return result;
         }
         XMLDocument doc = resultNode.getNodeType() == Node.DOCUMENT_NODE ? (XMLDocument) resultNode
               : (XMLDocument) resultNode.getOwnerDocument();
         resultNode.appendChild(doc.importNode(result, true));
      }
      return resultNode;
   }

   private boolean haveSameStructure(Node firstNode, Node secondNode) {
      return ((areNodeNamesEqual(firstNode, secondNode) || areLocalNodeNamesEqual(
            firstNode, secondNode))
            && areChildCountEqual(firstNode, secondNode) && areNodeTypesEqual(
            firstNode, secondNode));
   }

   private boolean areNodeNamesEqual(Node firstNode, Node secondNode) {
      return firstNode.getNodeName().equals(secondNode.getNodeName());
   }

   private boolean areLocalNodeNamesEqual(Node firstNode, Node secondNode) {
      return firstNode.getLocalName().equals(secondNode.getLocalName());
   }

   private boolean areNodeTypesEqual(Node firstNode, Node secondNode) {
      return firstNode.getNodeType() == secondNode.getNodeType();
   }

   private boolean areChildCountEqual(Node firstNode, Node secondNode) {
      return firstNode.getChildNodes().getLength() == secondNode
            .getChildNodes().getLength();
   }

   private boolean haveSameNamespace(Node firstNode, Node secondNode) {
      if (firstNode.getNodeType() != Node.ELEMENT_NODE) {
         return true;
      }
      if (firstNode.getNamespaceURI().equals(secondNode.getNamespaceURI())) {
         return true;
      }
      return false;
   }

   private String compareNodeValues(Node firstNode, Node secondNode) {
      String v1 = firstNode.getNodeValue() == null ? "" : firstNode
            .getNodeValue();
      String v2 = secondNode.getNodeValue() == null ? "" : secondNode
            .getNodeValue();
      if (v1.equals(v2)) {
         return "allEqual";
      }
      return "allUnique";
   }

   /**
    * Calculates the new value of the result node depending on the current value of the
    * result node and the input value.
    */
   private void setResultNodeValue(Node resultNode, String value) {
      if (resultNode.getNodeValue() == null) {
         resultNode.setNodeValue(value);
      } else if (resultNode.getNodeValue().equals("")) {
         resultNode.setNodeValue(value);
      } else if (resultNode.getNodeValue().equals("someDifferent")) {
         // do nothing
      } else if (!resultNode.getNodeValue().equals(value)) {
         resultNode.setNodeValue("someDifferent");
      }
   }

   /*private HashMap<String, String> compareAttributes(Node firstNode, Node secondNode)
         throws Exception {
      NamedNodeMap firstAttributes = firstNode.getAttributes();
      NamedNodeMap secondAttributes = secondNode.getAttributes();
      if (firstAttributes == null && secondAttributes == null) {
         // TODO: is this correct?
         return null;
      }
      if (firstAttributes.getLength() != secondAttributes.getLength()) {
         throw new Exception("different numbers of attributes");
      }
      // namespace-attribute entfernen, die wurden beim
      // namespace-vergleich abgefragt
      cleanAttributes(firstAttributes);
      cleanAttributes(secondAttributes);
      HashMap<String, String> resultValues = new HashMap<String, String>();
      for (int j = 0; j < firstAttributes.getLength(); j++) {
         Node secondAttributeNode = secondAttributes
               .getNamedItem(firstAttributes.item(j).getLocalName());
         if (secondAttributeNode == null) {
            throw new Exception("attribute node expected");
         }
         resultValues.put(firstAttributes.item(j).getLocalName(),
               compareNodeValues(firstAttributes.item(j), secondAttributeNode));
      }
      return resultValues;
   }*/

   /*private void setResultNodeAttributes(Node resultNode, HashMap values) {
      NamedNodeMap attributes = resultNode.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
         setResultNodeValue(attributes.item(i), (String) values.get(attributes
               .item(i).getLocalName()));
      }
   }*/

   /**
    * @param inputNodes
    * @param resultNode
    * @return
    * @throws Exception
    */
   private Node initializeResultNode(Node[] inputNodes) throws Exception {
      if (inputNodes[0].getNodeType() == Node.DOCUMENT_NODE) {
         return new XMLDocument();
      }
      if (inputNodes[0].getNodeType() == Node.ELEMENT_NODE) {
         return new XMLDocument().createElement(inputNodes[0].getLocalName());
      }
      if (inputNodes[0].getNodeType() == Node.TEXT_NODE) {
         return new XMLDocument().createTextNode("");
      }
      if (inputNodes[0].getNodeType() == Node.ATTRIBUTE_NODE) {
         return new XMLDocument().createAttribute(inputNodes[0].getLocalName());
      }
      if (inputNodes[0].getNodeType() == Node.CDATA_SECTION_NODE) {
         return new XMLDocument().createCDATASection("");
      }
      if (inputNodes[0].getNodeType() == Node.COMMENT_NODE) {
         return new XMLDocument().createComment("");
      }
      if (inputNodes[0].getNodeType() == Node.ENTITY_REFERENCE_NODE) {
         return new XMLDocument().createEntityReference(inputNodes[0]
               .getLocalName());
      }
      throw new Exception("unexpected NodeType: " + inputNodes[0].getNodeType());
   }

   private void cleanAttributes(NamedNodeMap attr) throws Exception {
      for (int i = attr.getLength() - 1; i >= 0; i--) {
         if (attr.item(i).getNodeName().matches("^xmlns(:.*)*")) {
            attr.removeNamedItem(attr.item(i).getNodeName());
         }
      }
   }

   private String getCompareIdentifier(boolean allEqual, boolean allDifferent)
         throws Exception {
      if (allEqual && allDifferent) {
         return ""; // throw new Exception("Es k�nnen nicht alle Felder gleich
         // und verschieden sein");
      }
      if (allEqual) {
         return "allEqual";
         // return Result.ALL_EQUAL;
      }
      if (allDifferent) {
         return "allUnique";
         // return Result.MUTUALLY_DISTINCT;
      }
      return "someDifferent";
      // return Result.PARTIALLY_EQUAL;
   }

}
