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
package com.gip.xyna.utils.xml.parser;

import java.io.File;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import oracle.xml.parser.schema.XMLSchema;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.gip.xyna.utils.xml.schema.SchemaUtils;

public class OracleDOMCreator {
   
   /**
    * parst den xml-String und gibt das zugehörige xmldocument zurück
    * 
    * @param xml
    * @return
    * @throws Exception
    */
   public static XMLDocument parseXMLString(String xml) throws Exception {
      // TODO: check input String
      DOMParser parser = new DOMParser();
      parser.setValidationMode(DOMParser.NONVALIDATING);
      parser.parse(new StringReader(xml));
      return parser.getDocument();
   }

   /**
    * parst das xml in dem übergebenen filenamen und gibt das zugehörige
    * xmldocument zurück
    * 
    * @param file
    * @return
    * @throws Exception
    */
   public static XMLDocument getXMLDocumentFromFile(String file)
         throws Exception {
      DOMParser parser = new DOMParser();
      parser.setValidationMode(DOMParser.NONVALIDATING);
      parser.parse(createURL(file));
      return parser.getDocument();
   }
   
   /**
    * fügt eine referenz auf den nachrichtennamen aus dem xsd-file filename in
    * das xsd xsd hinzu (innerhalb der ersten sequenz)
    * 
    * @param xsd
    * @param fileName
    * @param messageName
    * @return
    * @throws Exception
    */
   public static XMLDocument mergeMessage(XMLDocument xsd, String fileName,
         String messageName) throws Exception {
      XMLDocument xml = getXMLDocumentFromFile(fileName);
      XMLSchema schema = SchemaUtils
            .buildSchema(xml, createURL(fileName));

      // import file tag
      XMLElement importElement = (XMLElement) xsd.createElementNS(
            "http://www.w3.org/2001/XMLSchema", "import");
      importElement
            .setAttribute("schemaLocation", new File(fileName).getName());
      importElement.setAttribute("namespace", schema.getSchemaTargetNS());
      xsd.getElementsByTagName("schema").item(0).insertBefore(importElement,
            xsd.getElementsByTagName("element").item(0));

      // namespace abkürzung definieren
      NamedNodeMap attributes = ((XMLElement) xsd
            .getElementsByTagName("schema").item(0)).getAttributes();
      String shortNS = "";
      for (int i = 0; i < attributes.getLength(); i++) {
         int max = -1;
         String[] xmlns = attributes.item(i).getNodeName().split(":");
         if (attributes.item(i).getNodeValue().equals(
               schema.getSchemaTargetNS())) {
            shortNS = xmlns.length > 1 ? xmlns[1] : "";
            break;
         } else if (xmlns.length > 1) {
            if (xmlns[1].matches("^ns\\d+$")) {
               max = Math.max(max, new Integer(xmlns[1].substring(2))
                     .intValue());
            }
         }
         shortNS = "ns" + (max + 1);
      }
      ((XMLElement) xsd.getElementsByTagName("schema").item(0)).setAttribute(
            "xmlns:" + shortNS, schema.getSchemaTargetNS());

      // referenz in sequence einfügen
      Node root = xsd.getElementsByTagName("sequence").item(0);
      XMLElement ref = (XMLElement) xsd.createElementNS(
            "http://www.w3.org/2001/XMLSchema", "element");
      ref.setAttribute("ref", shortNS + ":" + messageName);
      root.appendChild(ref);

      return xsd;
   }

   /**
    * erstellt eine URL aus einer datei + pfad
    * 
    * @param fileName
    * @return
    */
   public static URL createURL(String fileName) {
      URL url = null;
      try {
         url = new URL(fileName);
      } catch (MalformedURLException ex) {
         File f = new File(fileName);
         try {
            String path = f.getAbsolutePath();
            // This is a bunch of weird code that is required to
            // make a valid URL on the Windows platform, due
            // to inconsistencies in what getAbsolutePath returns.
            String fs = System.getProperty("file.separator");
            if (fs.length() == 1) {
               char sep = fs.charAt(0);
               if (sep != '/')
                  path = path.replace(sep, '/');
               if (path.charAt(0) != '/')
                  path = '/' + path;
            }
            path = "file://" + path;
            url = new URL(path);
         } catch (MalformedURLException e) {
            System.out.println("Cannot create url for: " + fileName);
            System.exit(0);
         }
      }
      return url;
   }


}
