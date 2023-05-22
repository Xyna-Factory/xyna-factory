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
package com.gip.xyna.utils.xml.schema;

import java.io.ByteArrayOutputStream;

import java.net.URL;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;

/**
 * Represents the schema (root) node in xml schema.
 * <p>
 * It's the start node for creating a schema tree. <br>
 * Example:<br>
 * <code>
 * XynaSchema xsd = new XynaSchema("myNamespace");<br>
 * XynaSchemaElement el = xsd.addElement("element1", XynaSchemaNode.XSD_STRING);<br>
 * XynaSchemaAttribute at = el.addAttribute("attribute1", XynaSchemaNode.XSD_INTEGER);<br>
 * XynaSchemaComplexType ct = xsd.addComplexType("complexType1");<br>
 * ct.addElement("elementct1", XynaSchemaNode.XSD_STRING);<br>
 * String xsdString = xsd.generateXSD();<br>
 * </code>
 */
public class XynaSchema extends XynaSchemaNode {

   /**
    * Konstanten f�r die interne Verwendung beim XSD-Generieren.
    */
   private static final String TARGETNS = "targetNamespace";
   private static final String ELEMENTFORMDEFAULT = "elementFormDefault";
   private static final String QUALIFIED = "qualified";
   protected static final String XMLNS = "xmlns";
   private static final String SCHEMA = "xsd:schema";
   protected static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";
   protected static final String ELEMENT = "xsd:element";
   protected static final String SEQUENCE = "xsd:sequence";
   protected static final String SEQUENCE_NONS = "sequence";
   protected static final String COMPLEXTYPE = "xsd:complexType";
   protected static final String COMPLEXTYPE_NONS = "complexType";
   protected static final String COMPLEXCONTENT = "xsd:complexContent";
   protected static final String SIMPLECONTENT = "xsd:simpleContent";
   protected static final String EXTENSION = "xsd:extension";
   protected static final String EXTENSION_NONS = "extension";
   protected static final String ATTRIBUTE = "xsd:attribute";
   protected static final String IMPORT = "xsd:import";
   protected static final String ATT_NAME = "name";
   protected static final String ATT_TYPE = "type";
   protected static final String ATT_BASE = "base";
   protected static final String ATT_REFERENCE = "ref";
   protected static final String ATT_SCHEMALOC = "schemaLocation";
   protected static final String ATT_NAMESPACE = "namespace";
   protected static final String ATT_MINOCC = "minOccurs";
   protected static final String ATT_MAXOCC = "maxOccurs";
   protected static final String ATT_USE = "use";

   private String targetNS;

   /**
    * Hashtable<schemaloc, namespace>
    */
   private Hashtable<String, String> imports = new Hashtable<String, String>();

   /**
    * Hashtable<namespace, shortNS>
    */
   private Hashtable<String, String> shortNS = new Hashtable<String, String>();

   public XynaSchema(String targetNamespace) throws Exception {
      super(null);
      setRoot(this);
      targetNS = targetNamespace;
      shortNS.put(NS_XSD, "xsd");
      shortNS.put(targetNS, "");
   }

   public String getNamespace() {
      return targetNS;
   }

   public String generateXSD() throws Exception {
      XMLDocument xmldoc = new XMLDocument();
      appendXML(xmldoc, null, null);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmldoc.print(baos);
      return baos.toString();
   }

   /**
    * 
    * @param namespace
    * @throws Exception
    */
   public void addImport(String namespace) throws Exception {
      imports.put("locationOf" + namespace, namespace);
   }

   /**
    * 
    * @param namespace
    * @param url
    *              eigene url
    * @throws Exception
    */
   public void addImport(String namespace, URL url) throws Exception {
      imports.put(url.toString(), namespace);
   }

   public XynaSchemaComplexType addComplexType(String name) throws Exception {
      XynaSchemaComplexType xct = new XynaSchemaComplexType(getRoot());
      xct.setName(name);
      getChildren().add(xct);
      return xct;
   }

   public XynaSchemaElement addElement(String name, String type)
         throws Exception {
      return addElement(name, null, type);
   }

   /**
    * 
    * @param name
    * @param namespace
    *              NS vom Type. keine Angabe ist �quivalent zum TargetNS.
    * @param typeName
    * @return
    * @throws Exception
    */
   public XynaSchemaElement addElement(String name, String namespace,
         String typeName) throws Exception {
      for (int i = 0; i < getChildren().size(); i++) {
         if (getChildren().get(i) instanceof XynaSchemaElement) {
            XynaSchemaElement brother = (XynaSchemaElement) getChildren()
                  .get(i);
            if (brother.getName() != null && brother.getName().equals(name)) {
               throw new Exception(
                     "Rootelemente m�ssen unterschiedliche Namen haben.");
            }
         }
      }
      XynaSchemaElement xe = new XynaSchemaElement(namespace, getRoot());
      xe.setName(name);
      xe.setType(typeName);
      getChildren().add(xe);
      return xe;
   }

   /**
    * entweder nur ordnungsgebendes Element oder type sp�ter setzen
    * 
    * @param name
    * @return
    * @throws Exception
    */
   public XynaSchemaElement addElement(String name) throws Exception {
      return addElement(name, null, null);
   }

   private String createShortNS(String namespace) throws Exception {
      Matcher ma;
      Pattern p;
      if (namespace.matches(".*?/[a-zA-Z]+[a-zA-Z0-9]*(/[0-9\\.]*)*")) {
         p = Pattern.compile(".*?/([a-zA-Z0-9]*)/*");
         ma = p.matcher(namespace);
         String sub = "";
         if (ma.find()) {
            sub = ma.group(1);
         } else {
            throw new Exception("pattern-problem. sollte nicht vorkommen");
         }
         if (sub.length() > 4) {
            return sub.substring(0, 4);
         }
         return sub;
      }
      p = Pattern.compile("^.*?([a-zA-Z0-9]*).*?$");
      ma = p.matcher(namespace);
      String sub = "";
      if (ma.find()) {
         sub = ma.group(1);
      } else {
         throw new Exception("pattern-problem. sollte nicht vorkommen");
      }
      if (sub.length() > 4) {
         return sub.substring(0, 4);
      }
      return sub;
   }

   protected void appendXML(XMLDocument xmldoc, XynaSchemaNode parent,
         Node parentNode) throws Exception {
      // <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
      // xmlns="myNamespace"
      // targetNamespace="myNamespace" elementFormDefault="qualified">
      XMLElement schemaElement = (XMLElement) xmldoc.createElementNS(NS_XSD,
            SCHEMA);
      schemaElement.setAttribute(TARGETNS, targetNS);
      schemaElement.setAttribute(XMLNS, targetNS);
      schemaElement.setAttribute(ELEMENTFORMDEFAULT, QUALIFIED);
      xmldoc.appendChild(schemaElement);
      // imports
      // <xsd:import schemaLocation="test.xsd" namespace="myNamespace"/>
      for (Iterator<String> it = imports.keySet().iterator(); it.hasNext();) {
         // import-tag
         XMLElement importElement = (XMLElement) xmldoc.createElementNS(NS_XSD,
               IMPORT);
         String loc = it.next();
         String ns = (String) imports.get(loc);
         importElement.setAttribute(ATT_SCHEMALOC, loc);
         importElement.setAttribute(ATT_NAMESPACE, ns);
         schemaElement.appendChild(importElement);
         // abk�rzung
         String sns = createShortNS(ns);
         String num = "";
         int i = 0;
         while (shortNS.values().contains(sns + num)) {
            num = "" + i;
            i++;
         }
         shortNS.put(ns, sns + num);
         schemaElement.setAttribute(XMLNS + ":" + sns + num, ns);
      }
      // kinder
      for (int i = 0; i < getChildren().size(); i++) {
         ((XynaSchemaNode) getChildren().get(i)).appendXML(xmldoc, this, schemaElement);
      }
   }

   protected String getShortNS(String namespace) throws Exception {
      return (String) shortNS.get(namespace);
   }

   public void importFragmentAsChild(XynaSchemaFragment frag) throws Exception {
      if (frag.getNode() instanceof XynaSchema) {
         // Kind-Elemente anh�ngen ?
         throw new Exception("nicht unterst�tzt."); // TODO
      } else if (frag.getNode() instanceof XynaSchemaAttribute
            || frag.getNode() instanceof XynaSchemaElement
            || frag.getNode() instanceof XynaSchemaComplexType) {
         frag.getNode().setRoot(getRoot());
         getChildren().add(frag.getNode());
      }
   }

}
