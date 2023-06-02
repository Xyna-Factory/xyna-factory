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

import com.gip.xyna.utils.xml.parser.OracleDOMCreator;

import junit.framework.TestCase;

import oracle.xml.parser.v2.XMLDocument;

/**
 *
 */
public class SchemaUtilsTest extends TestCase {

  public void testGetMessagesOfXSD() {
    try {
      String xsd = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\" ?>\n" + 
      "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://www.example.org\"\n" + 
      "            targetNamespace=\"http://www.example.org\" elementFormDefault=\"qualified\">\n" + 
      "  <xsd:import namespace=\"testns\" schemaLocation=\"testloc\"/>\n" + 
      "  <xsd:element name=\"test1Request\" type=\"xsd:string\">\n" + 
      "  </xsd:element>\n" + 
      "  <xsd:element name=\"test1Response\" type=\"xsd:string\"/>\n" + 
      "  <xsd:element name=\"test2Request\" type=\"xsd:string\"/>\n" + 
      "  <xsd:element name=\"test2Response\" type=\"xsd:string\"/>\n" + 
      "</xsd:schema>\n";
      XMLDocument xmldoc = OracleDOMCreator.parseXMLString(xsd);
      String[] msgs = SchemaUtils.getMessagesOfXSD(xmldoc);
      assertEquals("anzahl der nachrichten sollte 4 sein", 4, msgs.length);
      assertEquals("erste nachricht sollte test1Request sein", "test1Request", msgs[0]);
      assertEquals("zweite nachricht sollte test1Response sein", "test1Response", msgs[1]);
      assertEquals("dritte nachricht sollte test2Request sein", "test2Request", msgs[2]);
      assertEquals("vierte nachricht sollte test2Response sein", "test2Response", msgs[3]);
    } catch (Exception e) {
      fail();
    }
  }

}
