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
package com.gip.xyna.utils.xml;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import com.gip.xyna.utils.xml.parser.SimpleXMLParserTest;

public class AllTests {

   public static Test suite() {
      TestSuite suite = new TestSuite("Test for com.gip.xyna.utils.xml");
      // $JUnit-BEGIN$
      suite.addTestSuite(ComparerTest.class);
      suite.addTestSuite(XMLUtilsTest.class);
      suite.addTestSuite(SimpleXMLParserTest.class);
      suite.addTest(com.gip.xyna.utils.xml.schema.AllTests.suite());
      // $JUnit-END$
      return suite;
   }

   public static void main(String[] args) {
      TestRunner.main(new String[] { "com.gip.xyna.utils.xml.AllTests" });
   }
}
