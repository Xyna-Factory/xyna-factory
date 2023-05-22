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
package com.gip.xyna.utils.logging.syslog;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.TestCase;

/**
 * 
 */
public class SysLoggerTest extends TestCase {

   private static final String HOST = "gipsun173.gip.local";

   public void testSyslog() throws SocketException,
         SecurityException, UnknownHostException {
      SysLogger logger = SysLogger.getLogger(HOST, SysLogger.DEFAULT_PORT,
            "syslog");
      MockHandler handler = new MockHandler(HOST, SysLogger.DEFAULT_PORT);
      SyslogFormatter formatter = new SyslogFormatter(Facility.LOCAL0);
      handler.setFormatter(formatter);
      // TODO: dont log to remote host
      logger.addHandler(handler);

      Date date = Calendar.getInstance().getTime();
      String msg = "SysLoggerTest";
      LogRecord record = new LogRecord(Severity.NOTICE.getLevel(), msg);
      record.setMillis(date.getTime());
      record.setSourceClassName(this.getClass().getName());
      // TODO: specify source method
      record.setSourceMethodName("");

      logger.syslog(Severity.NOTICE, msg);
      String[] logEntries = handler.getLogEntries();
      assertTrue(logger instanceof Logger);
      assertNotNull(logEntries);
      assertEquals("Number of log entries", 1, logEntries.length);
      assertEquals("LogMessage", formatter.format(record), logEntries[0]);
   }

   // TODO: test log multiple lines (explicit and implicit)

   // TODO: testGetLogger_noHost

   // TODO: test log level

   // TODO: test caller class and method (specially for debug, waring, ...)

}
