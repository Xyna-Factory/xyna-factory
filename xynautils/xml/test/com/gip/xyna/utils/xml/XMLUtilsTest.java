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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLParseException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLUtilsTest extends TestCase {

   private static final String XML_FILE = System.getProperty("user.dir")
         + "/test/DummyRequest.xml";

   private Document document = null;

   public void setUp() throws XMLParseException, FileNotFoundException,
         SAXException, IOException {
      DOMParser parser = new DOMParser();
      parser.parse(new FileReader(XML_FILE));
      document = parser.getDocument();
   }
   
   //TODO: testGetChildren()

   public void testGetTextValue() throws Exception {
      assertEquals("Text", XMLUtils.getTextValue(document.getElementsByTagName(
            "addOrderRequest").item(0)));
      assertEquals("", XMLUtils.getTextValue(document.getElementsByTagName("")
            .item(0)));
      assertEquals("", XMLUtils.getTextValue(document.getElementsByTagName(
            "Order").item(0)));
   }

   public void testSetTextValue() {
      XMLUtils.setTextValue(document.getElementsByTagName("addOrderRequest")
            .item(0), "AnotherText");
      assertEquals("AnotherText", XMLUtils.getTextValue(document
            .getElementsByTagName("addOrderRequest").item(0)));
      XMLUtils
            .setTextValue(document.getElementsByTagName("").item(0), "NoText");
      assertEquals("", XMLUtils.getTextValue(document.getElementsByTagName("")
            .item(0)));
      XMLUtils.setTextValue(document.getElementsByTagName("Order").item(0),
            "NewText");
      assertEquals("NewText", XMLUtils.getTextValue(document
            .getElementsByTagName("Order").item(0)));
   }

}
