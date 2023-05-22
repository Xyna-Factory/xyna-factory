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
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A Formatter class for formating syslog message.
 * <p>
 * Implements the format method inherited from java.util.logging.Formatter.
 * Additionally implements a method for format and split an oversize message.
 * 
 * 
 */
@Deprecated
public class SyslogFormatter extends Formatter {

   /**
    * Indicates that a log entry is split over multiple lines.
    */
   public static final String DEFAULT_SPLIT_INDICATOR = "////";
   /**
    * Maximal size of a syslog message.
    */
   public static final int MESSAGE_MAX_SIZE = 1024;

   private static SimpleDateFormat dateFormatter = new SimpleDateFormat(
         "MMM dd HH:mm:ss");

   private String hostname;
   private Facility facility;
   private String split_indicator = DEFAULT_SPLIT_INDICATOR;

   /**
    * Creates a new SyslogFormatter with the given Facility. Facility is
    * unchangeable.
    * 
    * @param facility
    *              Facility for this formatter instance.
    * @throws UnknownHostException
    */
   public SyslogFormatter(Facility facility) throws UnknownHostException {
      hostname = InetAddress.getLocalHost().getHostName();
      this.facility = facility;
   }

   /**
    * Formats the given record.
    * 
    * @param record
    *              Record to format.
    * @return the formated message.
    */
   public String format(LogRecord record) {
      StringBuffer message = new StringBuffer();
      try {
         // Head
         message.append(createHead(record));
         // Message
         message.append(record.getMessage());
         // Tail
         message.append(createTail(record));
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      // Syslog Nachrichten sind max. 1024 Bytes lang
      if (message.length() > MESSAGE_MAX_SIZE)
         return message.substring(0, MESSAGE_MAX_SIZE);
      return message.toString();
   }

   /**
    * Formats the given record. If the formated string exceeds the maximal
    * allowed message size it's split into multiple message.
    * 
    * @param record
    *              Record to format.
    * @return an Array of formated messages.
    */
   public String[] formatMultipleLines(LogRecord record) {
      String head = createHead(record);
      String tail = createTail(record);
      int max_payload_size = getMaxPayloadSize(head.length() + tail.length());
      String[] payloads = splitMessage(record.getMessage(), max_payload_size);
      String[] messages = new String[payloads.length];
      for (int i = 0; i < messages.length; i++) {
         messages[i] = head + payloads[i] + tail;
      }
      return messages;
   }

   /**
    * Gets the maximal size for payload in a message.
    * 
    * @param overhead_size
    *              The size of the messages overhead.
    * @return maximal allowed size of messages
    */
   private int getMaxPayloadSize(int overhead_size) {
      return MESSAGE_MAX_SIZE - overhead_size;
   }

   /**
    * Returns the number of message that are needed to log the whole input
    * message.
    * 
    * @param message_size
    *              Size of the input message
    * @param max_payload_size
    *              Maximal allowed size of payload
    * @return number of needed messages
    */
   private int getNumberOfNeededMessages(int message_size, int max_payload_size) {
      if (max_payload_size >= message_size) {
         return 1;
      }
      float rest = message_size - max_payload_size; // message 1
      // respect split indicator
      double count = Math.ceil(rest
            / (max_payload_size - split_indicator.length()));
      return Double.valueOf(count).intValue() + 1;
   }

   /**
    * Splits the given message into piece no bigger than the given size.
    * 
    * @param message
    *              Message to split
    * @param max_payload_size
    *              Allowed size for message pieces
    * @return an Array with message pieces
    */
   private String[] splitMessage(String message, int max_payload_size) {
      if (message.length() <= max_payload_size) {
         return new String[] { message };
      }
      int message_count = getNumberOfNeededMessages(message.length(),
            max_payload_size);
      String[] payload = new String[message_count];
      payload[0] = message.substring(0, max_payload_size);
      int current_pos = max_payload_size;
      for (int i = 1; i < payload.length; i++) {
         payload[i] = split_indicator;
         payload[i] += message.substring(current_pos, Math.min(current_pos
               + (max_payload_size - split_indicator.length()), message
               .length()));
         current_pos = current_pos
               + (max_payload_size - split_indicator.length());
      }
      return payload;
   }

   /**
    * Creates a header for syslog messages
    */
   protected String createHead(LogRecord record) {
      StringBuffer buffer = new StringBuffer();
      // Priority
      buffer.append("<");
      buffer.append(getSyslogPriority(facility, record.getLevel()));
      buffer.append(">");
      // Date
      buffer.append(dateFormatter.format(new Date(record.getMillis())));
      buffer.append(" ");
      // Hostname
      buffer.append(hostname);
      buffer.append(" ");
      // Originator
      buffer.append(record.getSourceClassName());
      buffer.append(".");
      buffer.append(record.getSourceMethodName());
      buffer.append(": ");
      // Syslog Level
      buffer.append(getSyslogLevel(record.getLevel()));
      buffer.append(" ");
      return buffer.toString();
   }

   /**
    * Creates a tail for syslog messages.
    */
   protected String createTail(LogRecord record) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(" ");
      // Date
      buffer.append(dateFormatter.format(new Date(record.getMillis())));
      buffer.append("\n");
      return buffer.toString();
   }

   /**
    * Calculates the syslog priority for given facility and given log level.
    */
   private int getSyslogPriority(Facility facility, Level level) {
      return Priority.calculatePriority(facility, Severity.getSeverity(level));
   }

   /**
    * Gets the syslog level of the given log level.
    */
   private Level getSyslogLevel(Level level) {
      return Severity.getSeverity(level).getLevel();
   }

   public String getSplitIndicator() {
      return split_indicator;
   }

   public void setSplitIndicator(String split_indicator) {
      this.split_indicator = split_indicator;
   }

}
