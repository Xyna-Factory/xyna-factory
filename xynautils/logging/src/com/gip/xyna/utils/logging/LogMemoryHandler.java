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
package com.gip.xyna.utils.logging;

import java.util.logging.*;

/**
 * Handler that buffers requests in a circular buffer in memory. Normally this
 * Handler simply stores incoming LogRecords into its memory buffer and discards
 * earlier records. This buffering is very cheap and avoids formatting costs.
 * 
 * The source is based on java.util.logging.MemoryHandler.
 * 
 * 
 */
@Deprecated
public class LogMemoryHandler extends Handler {

   private final static int DEFAULT_SIZE = 1000;
   private int size;
   private LogRecord buffer[];
   int start, count;

   /**
    * Private method to configure
    */
   private void configure() {
      size = DEFAULT_SIZE;
      setLevel(LogLevel.DEBUG);
      setFilter(null);
      setFormatter(new LogFormatter());
   }

   /**
    * 
    */
   public LogMemoryHandler() {
      configure();
      init();
   }

   /**
    * Initialize. Size is a count of LogRecords.
    */
   private void init() {
      buffer = new LogRecord[size];
      start = 0;
      count = 0;
   }

   /**
    * @param size
    * @param logLevel
    */
   public LogMemoryHandler(int size) {
      if (size <= 0) {
         throw new IllegalArgumentException();
      }
      configure();
      this.size = size;
      init();
   }

   /**
    * Store a <tt>LogRecord</tt> in an internal buffer.
    * <p>
    * If there is a <tt>Filter</tt>, its <tt>isLoggable</tt> method is
    * called to check if the given log record is loggable. If not we return.
    * Otherwise the given record is copied into an internal circular buffer.
    * Then the record's level property is compared with the <tt>pushLevel</tt>.
    * If the given level is greater than or equal to the <tt>pushLevel</tt>
    * then <tt>push</tt> is called to write all buffered records to the target
    * output <tt>Handler</tt>.
    * 
    * @param record
    *              description of the log event
    */
   public synchronized void publish(LogRecord record) {
      if (!isLoggable(record)) {
         return;
      }
      int ix = (start + count) % buffer.length;
      buffer[ix] = record;
      if (count < buffer.length) {
         count++;
      } else {
         start++;
      }
   }

   /**
    * Get the buffered output The buffer is then cleared.
    * 
    * @return
    */
   public synchronized String getBuffer() {
      StringBuffer sb = new StringBuffer(1000);
      for (int i = 0; i < count; i++) {
         int ix = (start + i) % buffer.length;
         LogRecord record = buffer[ix];
         sb.append(record.getMessage()); // NICE: hier kommt momentan nur die
                                          // MSG rein, besser wäre ein
                                          // formatierter OBLog.
      }
      // Empty the buffer.
      start = 0;
      count = 0;
      return sb.toString();
   }

   /**
    * NOP
    */
   public void flush() {
      // NOP
   }

   /**
    * 
    */
   public void close() {
      setLevel(Level.OFF);
   }

   /**
    * Check if this <tt>Handler</tt> would actually log a given
    * <tt>LogRecord</tt> into its internal buffer.
    * <p>
    * This method checks if the <tt>LogRecord</tt> has an appropriate level
    * and whether it satisfies any <tt>Filter</tt>. However it does <b>not</b>
    * check whether the <tt>LogRecord</tt> would result in a "push" of the
    * buffer contents.
    * <p>
    * 
    * @param record
    *              a <tt>LogRecord</tt>
    * @return true if the <tt>LogRecord</tt> would be logged.
    * 
    */
   public boolean isLoggable(LogRecord record) {
      return super.isLoggable(record);
   }

}
