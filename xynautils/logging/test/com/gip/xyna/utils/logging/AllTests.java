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
package com.gip.xyna.utils.logging;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.gip.xyna.utils.logging.syslog.PriorityTest;
import com.gip.xyna.utils.logging.syslog.SeverityTest;
import com.gip.xyna.utils.logging.syslog.SysLoggerTest;
import com.gip.xyna.utils.logging.syslog.SyslogFormatterTest;
import com.gip.xyna.utils.logging.syslog.SyslogHandlerTest;

/**
 * 
 */
public class AllTests extends TestSuite {

   public static Test suite() {
      TestSuite suite = new TestSuite("Test for com.gip.xyna.utils.logging");
      // $JUnit-BEGIN$
      suite.addTestSuite(XynaSyslogAppenderTest.class);
      suite.addTestSuite(PriorityTest.class);
      suite.addTestSuite(SeverityTest.class);
      suite.addTestSuite(SyslogFormatterTest.class);
      //suite.addTestSuite(SysLoggerTest.class);
      //suite.addTestSuite(SyslogHandlerTest.class);
      // $JUnit-END$
      return suite;
   }

}
