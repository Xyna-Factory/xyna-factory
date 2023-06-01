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
package com.gip.xyna.utils.logging.syslog;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * Enumeration for the syslog level.
 * <p>
 * The severity indicates the severity/importance of a log message.
 * <p>
 * The lower the number of the severity, the more important the message is.
 * <p>
 * Each serverity is mapped to a corresponding log level.
 * <p>
 * <p>
 * EMERGENCY (0, Level.SEVERE + 300)
 * <p>
 * ALERT (1, Level.SEVERE + 200)
 * <p>
 * CRITICAL (2, Level.SEVERE + 100)
 * <p>
 * ERROR (3, Level.SEVERE)
 * <p>
 * WARNING (4, Level.WARNING)
 * <p>
 * NOTICE (5, Level.INFO)
 * <p>
 * INFORMATIONAL (6, Level.CONFIG)
 * <p>
 * DEBUG (7, Level.FINEST)
 */
@Deprecated
public enum Severity {

   /**
    * System is unusable
    */
   EMERGENCY(0, Level.SEVERE, 300),
   /**
    * Action must be taken immediately
    */
   ALERT(1, Level.SEVERE, 200),
   /**
    * Critical conditions
    */
   CRITICAL(2, Level.SEVERE, 100),
   /**
    * Error conditions
    */
   ERROR(3, Level.SEVERE),
   /**
    * Warning conditions
    */
   WARNING(4, Level.WARNING),
   /**
    * Normal but significant conditions
    */
   NOTICE(5, Level.INFO),
   /**
    * Informational messages
    */
   INFORMATIONAL(6, Level.CONFIG),
   /**
    * Debug-level messages
    */
   DEBUG(7, Level.FINEST);

   private int value;
   private Level logLevel;
   private Level syslogLevel;
   private int offset;

   private static HashMap<Level, Severity> levelMap = null;

   private Severity(int value, Level logLevel) {
      this(value, logLevel, 0);
   }

   private Severity(int value, Level logLevel, int offset) {
      this.value = value;
      this.logLevel = logLevel;
      if (offset < 0)
         offset = 0;
      this.offset = offset;
      this.syslogLevel = new SyslogLevel(this.name(), logLevel.intValue()
            + offset);
   }

   /**
    * Get the int value of the syslog level.
    * 
    * @return numerical value of the severity
    */
   public int getValue() {
      return value;
   }

   /**
    * Get a severity by its numerical value.
    * 
    * @param code
    *              numerical value of the severity
    * @return the severity matching the input value
    */
   public static Severity getSeverity(int code) {
      if ((code < 0) || (code >= Severity.values().length)) {
         return null;
      }
      return Severity.values()[code];
   }

   /**
    * Get the severity which matches the given log level.
    * 
    * @param level
    *              a log level
    * @return the severity matching the log level.
    */
   public static Severity getSeverity(Level level) {
      if (level instanceof SyslogLevel) {
         return Enum.valueOf(Severity.class, level.getName());
      }
      if (levelMap == null) {
         initMap();
      }
      if (!levelMap.containsKey(level)) {
         levelMap.put(level, getNextSmaller(level));
      }
      return levelMap.get(level);
   }

   /**
    * Initialize the severity-log level mapping.
    */
   private static void initMap() {
      levelMap = new HashMap<Level, Severity>();
      for (Severity s : Severity.values()) {
         if (s.offset == 0) { // direct mapping
            levelMap.put(s.logLevel, s);
         }
      }
   }

   /**
    * If no entry for the input level is found in the HashMap, a mapping with
    * severity of the next smaller (int value) level is added.
    * 
    * @param level
    *              a log level
    * @return the severity mapped to the next smaller level
    */
   private static Severity getNextSmaller(Level level) {
      Level[] keys = levelMap.keySet().toArray(new Level[0]);
      Level next = null;
      for (Level l : keys) {
         if (l.intValue() < level.intValue()) {
            if ((next == null) || (next.intValue() < l.intValue())) {
               next = l;
            }
         }
      }
      return getSeverity(next);
   }

   /**
    * Get the log level of the facility.
    * 
    * @return a log level
    */
   public Level getLevel() {
      return syslogLevel;
   }

   /**
    * Defines a Log Level for each Severity.
    * 
    * 
    */
   @SuppressWarnings("serial")
   private class SyslogLevel extends Level {

      /**
       * @param name
       * @param value
       */
      private SyslogLevel(String name, int value) {
         super(name, value);
      }

   }

}
