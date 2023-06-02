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
package com.gip.xyna.utils.xml.schema;

import java.net.URL;

import java.util.Vector;

import oracle.xml.parser.schema.XMLSchema;
import oracle.xml.parser.schema.XSDBuilder;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SchemaUtils {
   
   /**
    * erstellt das xmlschema zum xmldocument einer xsd-datei
    * 
    * @param xsdDoc
    * @param url
    *              pfad (relativ oder absolut), in dem die imports und includes
    *              gefunden werden können
    * @return
    * @throws Exception
    */
   public static XMLSchema buildSchema(XMLDocument xsdDoc, URL url)
         throws Exception {
      XSDBuilder builder = new XSDBuilder();
      return builder.build(xsdDoc, url);
   }
   
   /**
   * ermittelt aus einem xsd alle Nachrichten
   * @param xsdDoc
   * @return array aller nachrichtennamen
   * @throws Exception
   */
   public static String[] getMessagesOfXSD(XMLDocument xsdDoc) throws Exception {
     NodeList children = xsdDoc.getChildNodes().item(0).getChildNodes();
     Vector<String> v = new Vector<String>();
     for (int i = 0; i<children.getLength(); i++) {
       Node child = children.item(i);
      // System.out.println(child.getNodeName());
       if (child instanceof XMLElement && child.getLocalName().equals("element")) {
         v.add(child.getAttributes().getNamedItem("name").getNodeValue());
       }
     }
     return v.toArray(new String[]{});
   }

}
