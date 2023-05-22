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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Collection of helper methods.
 */
public class XMLUtils {

   /**
    * Get all child elements with given tag and given namespace.
    * 
    * @param parent
    *              parent node of the requested children
    * @param tag
    *              only nodes with this tag are added to the result list
    * @param namespace
    *              only nodes with this namespace are added to the result list
    * @return list of child nodes
    * @see Node
    */
   public static List<Node> getChildren(Node parent, String tag,
         String namespace) {
      ArrayList<Node> children = new ArrayList<Node>();
      NodeList list = parent.getChildNodes();
      for (int i = 0; i < list.getLength(); i++) {
         if ((list.item(i).getNodeType() == Node.ELEMENT_NODE)
               && list.item(i).getLocalName().equals(tag)
               && list.item(i).getNamespaceURI().equals(namespace)) {
            children.add(list.item(i));
         }
      }
      return children;
   }

   /**
    * Get the CDATA value of the specified xml element.
    * 
    * @param element
    *              the element which text value is requested
    * @return the text value of the element
    */
   public static String getTextValue(Node element) {
      if (element == null) {
         return "";
      }
      NodeList nodelist = element.getChildNodes();
      for (int i = 0; i < nodelist.getLength(); i++) {
         if (nodelist.item(i).getNodeType() == Node.TEXT_NODE) {
            return nodelist.item(i).getNodeValue() == null ? "" : nodelist
                  .item(i).getNodeValue().trim();
         }
      }
      return "";
   }

   /**
    * Set the CDATA value of the specified xml element.
    * @param element the element which text value should be changed
    * @param value the new text value
    */
   public static void setTextValue(Node element, String value) {
      if (element == null) {
         return;
      }
      NodeList nodes = element.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
         if (nodes.item(i).getNodeType() == Node.TEXT_NODE) {
            nodes.item(i).setNodeValue(value);
            break;
         }
      }
   }

   
   
   
   private static final Pattern patternEncodeAmp = Pattern.compile("&");
   private static final Pattern patternEncodeApos = Pattern.compile("'");
   private static final Pattern patternEncodeGt = Pattern.compile(">");
   private static final Pattern patternEncodeLt = Pattern.compile("<");
   private static final Pattern patternEncodeQuot = Pattern.compile("\"");
  
   private static final Pattern patternDecodeAmp = Pattern.compile("&amp;");
   private static final Pattern patternDecodeApos = Pattern.compile("&apos;");
   private static final Pattern patternDecodeGt = Pattern.compile("&gt;");
   private static final Pattern patternDecodeLt = Pattern.compile("&lt;");
   private static final Pattern patternDecodeQuot = Pattern.compile("&quot;");

   
   /**
    * Encode all not allowed characters (Entity-References).
    * 
    * @param string
    *              String with not allowed characters
    * @return an encoded version of the input String
    */
   public static String encodeString(String string) {
      if (string == null || string.length() == 0) {
         return string;
      }
      string = patternEncodeAmp.matcher(string).replaceAll("&amp;");
      string = patternEncodeApos.matcher(string).replaceAll("&apos;");
      string = patternEncodeGt.matcher(string).replaceAll("&gt;");
      string = patternEncodeLt.matcher(string).replaceAll("&lt;");
      string = patternEncodeQuot.matcher(string).replaceAll("&quot;");
      return string;
   }
  
   /**
   * Encode all not allowed characters (Entity-References).
    * 
    * @param string
    *              String with not allowed characters
    * @return a decoded version of the input String
    */
   public static String decodeString(String string) {
      if (string == null || string.length() == 0) {
         return string;
      }
      string = patternDecodeApos.matcher(string).replaceAll("'");
      string = patternDecodeGt.matcher(string).replaceAll(">");
      string = patternDecodeLt.matcher(string).replaceAll("<");
      string = patternDecodeQuot.matcher(string).replaceAll("\"");
      string = patternDecodeAmp.matcher(string).replaceAll("&");
      return string;
   }

  /**
   * inhalt des tags als string, inklusive kindelementen falls vorhanden mitsamt
   * tags. muss nicht unbedingt ein g�ltiges xml ergeben, weil mehrere kindelemente
   * vorhanden sein k�nnen
   * @param el
   * @return
   */
  public static String getContentWithTags(XMLElement el) throws IOException {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < el.getChildNodes().getLength(); i++) {
      if (el.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
        sb.append(getXMLElementAsString((XMLElement)el.getChildNodes().item(i)));
      } else if (el.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
        sb.append(el.getChildNodes().item(i).getNodeValue());
      }
    }
    return sb.toString();
  }

  public static ArrayList<Node> getChildrenByType(XMLElement el,
                                                  short nodeType) {
    ArrayList<Node> nodes = new ArrayList<Node>();
    NodeList nl = el.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      if (nl.item(i).getNodeType() == nodeType) {
        nodes.add(nl.item(i));
      }
    }
    return nodes;
  }

  /**
   * util methode
   *
   * @param el
   * @return
   * @throws Exception
   */
  public static String getXMLElementAsString(XMLElement el) throws IOException {
    StringWriter sw = new StringWriter();
    el.print(sw);
    return sw.toString();
  }

  public static void addAttributeIfNotNullOrEmpty(Element el, String name,
                                                  String value) {
    if (value != null) {
      el.setAttribute(name, value);
    }
  }

  public static XMLElement parseAndImport(String xmlString,
                                          XMLDocument parentDoc) throws Exception {
    DOMParser parser = new DOMParser();
    parser.retainCDATASection(true);
    try {
      parser.parse(new StringReader(xmlString));
    } catch (Exception e) {
      throw new Exception("XML konnte nicht erfolgreich geparst werden. Grund: " +
                          e.getMessage(), e);
    }
    XMLDocument docPayload = parser.getDocument();
    NodeList children = docPayload.getChildNodes();
    if (children.getLength() == 0) {
      return null;
    }
    XMLElement rootEl = null;
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        rootEl = (XMLElement)n;
        break;
      }
    }
    if (rootEl == null) {
      return null;
    }
    return (XMLElement)parentDoc.importNode(rootEl, true);
  }

}
