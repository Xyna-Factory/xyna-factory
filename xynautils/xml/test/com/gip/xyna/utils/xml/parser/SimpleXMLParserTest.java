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
package com.gip.xyna.utils.xml.parser;

import junit.framework.TestCase;

/**
 * 
 */
public class SimpleXMLParserTest extends TestCase {

   private SimpleXMLParser parser = null;

   public void setUp() {
      parser = new SimpleXMLParser();
   }

   public void testParse_emptyMessage() {
      parser.parse("");
      assertEquals(0, parser.getNum());
   }

   public void testParse_Encoding() {
      String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      parser.parse(xmlMessage);
      assertEquals(0, parser.getNum());
   }

   public void testParse_AttributedElement() {
      String attributedElement = "<Request id=\"1\" operation=\"add\"/>";
      String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + attributedElement;
      parser.parse(xmlMessage);
      assertEquals(1, parser.getNum());
      // TODO: assertEquals(attributedElement, parser.getKey(0));
   }

   public void testParse_ChildElements() {
      String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<AnotherTag><SubTag></SubTag><SubTag></SubTag></AnotherTag>";
      parser.parse(xmlMessage);
      // TODO: assertEquals(3, parser.getNum());
   }

   // TODO: testParse_invalidXML
}
