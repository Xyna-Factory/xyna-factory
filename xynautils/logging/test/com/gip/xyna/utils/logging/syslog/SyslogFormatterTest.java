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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import junit.framework.TestCase;

/**
 * 
 */
public class SyslogFormatterTest extends TestCase {

   public void testFormat() throws UnknownHostException {
      InetAddress localhost = InetAddress.getLocalHost();
      String hostname = localhost.getHostName();

      SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm:ss");
      Date current = Calendar.getInstance().getTime();
      String date = dateFormatter.format(current);

      String msg = "Syslog facility local 0 and LogLevel severe";

      LogRecord record = new LogRecord(Level.SEVERE, msg);
      record.setMillis(current.getTime());
      record.setSourceClassName(this.getClass().getName());
      record.setSourceMethodName(this.getName());

      SyslogFormatter formatter = new SyslogFormatter(Facility.LOCAL0);
      String result = formatter.format(record);
      assertNotNull(result);
      assertEquals("LogMessage", "<131>" + date + " " + hostname + " "
            + this.getClass().getName() + "." + getName() + ": "
            + Severity.getSeverity(Level.SEVERE) + " " + msg + " " + date
            + "\n", result);
   }

   public void testFormatMultipleLines_SingleLine() throws UnknownHostException {
      InetAddress localhost = InetAddress.getLocalHost();
      String hostname = localhost.getHostName();

      SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm:ss");
      Date current = Calendar.getInstance().getTime();
      String date = dateFormatter.format(current);

      String msg = "Single line message";

      LogRecord record = new LogRecord(Level.CONFIG, msg);
      record.setMillis(current.getTime());
      record.setSourceClassName(this.getClass().getName());
      record.setSourceMethodName(this.getName());

      SyslogFormatter formatter = new SyslogFormatter(Facility.LOCAL0);
      String[] results = formatter.formatMultipleLines(record);
      assertNotNull(results);
      assertEquals("Number of messages", 1, results.length);
      String head = "<134>" + date + " " + hostname + " "
            + this.getClass().getName() + "." + getName() + ": "
            + Severity.getSeverity(Level.CONFIG) + " ";
      String tail = " " + date + "\n";
      assertEquals("LogMessage 1", head + msg + tail, results[0]);
   }

   public void testFormatMultipleLines_DoubleLine() throws UnknownHostException {
      InetAddress localhost = InetAddress.getLocalHost();
      String hostname = localhost.getHostName();

      SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm:ss");
      Date current = Calendar.getInstance().getTime();
      String date = dateFormatter.format(current);

      String msg = "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message, "
            + "Double line message, Double line message, Double line message, Double line message";

      LogRecord record = new LogRecord(Level.CONFIG, msg);
      record.setMillis(current.getTime());
      record.setSourceClassName(this.getClass().getName());
      record.setSourceMethodName(this.getName());

      SyslogFormatter formatter = new SyslogFormatter(Facility.LOCAL0);
      String[] results = formatter.formatMultipleLines(record);
      assertNotNull(results);
      assertEquals("Number of messages", 2, results.length);
      String head = "<134>" + date + " " + hostname + " "
            + this.getClass().getName() + "." + getName() + ": "
            + Severity.getSeverity(Level.CONFIG) + " ";
      String tail = " " + date + "\n";
      int message_size = SyslogFormatter.MESSAGE_MAX_SIZE - head.length()
            - tail.length();
      assertEquals("LogMessage 1",
            head + msg.substring(0, message_size) + tail, results[0]);
      assertEquals("LogMessage 2", head + formatter.getSplitIndicator()
            + msg.substring(message_size, msg.length()) + tail, results[1]);
      assertEquals("Msg = Msg1 + Msg2", msg, msg.substring(0, message_size)
            + msg.substring(message_size, msg.length()));
   }

   // TODO: test formatMultipleLines_1024SizeMessage

}
