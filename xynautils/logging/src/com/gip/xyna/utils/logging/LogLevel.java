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

import java.util.HashMap;
import java.util.logging.Level;

/**
 * Definiert eigene Log-Level zu den Standard-Log-Level. Werden die Log-Level
 * geändert sollte man sich auch LogFormatter.java anschauen. Die verfügbaren
 * OBLog-Level:
 * <ul>
 * <li>PANIC (1500)
 * <li>ERROR (1100)
 * <li>SEVERE (1000) (Standard-Log-Level)
 * <li>WARNING (900) (Standard-Log-Level)
 * <li>INFO (800) (Standard-Log-Level)
 * <li>CONFIG (700) (Standard-Log-Level)
 * <li>DML (650)
 * <li>SQL (600)
 * <li>FINE (500) (Standard-Log-Level)
 * <li>FINER (400) (Standard-Log-Level)
 * <li>FINEST (300) (Standard-Log-Level)
 * <li>DEBUG (200)
 * <li>TEST (100)
 * </ul>
 * 
 * 
 */
@Deprecated
@SuppressWarnings("serial")
public class LogLevel extends Level {

   public static final Level PANIC = new LogLevel("PANIC", 1500);
   public static final Level ERROR = new LogLevel("ERROR", 1100);
   public static final Level DML = new LogLevel("DML  ", 650);
   public static final Level SQL = new LogLevel("SQL  ", 600);
   public static final Level DEBUG = new LogLevel("DEBUG", 200);
   public static final Level TEST = new LogLevel("TEST", 100);

   private static HashMap<String, String> map;

   static {
      map = new HashMap<String, String>();
      map.put("100", "TEST");
      map.put("200", "DEBUG");
      map.put("300", "FINEST");
      map.put("400", "FINER");
      map.put("500", "FINE");
      map.put("600", "SQL");
      map.put("650", "DML");
      map.put("700", "CONFIG");
      map.put("800", "INFO");
      map.put("900", "WARNING");
      map.put("1000", "SEVERE");
      map.put("1100", "ERROR");
      map.put("1500", "PANIC");
   }

   /**
    * @param name
    * @param value
    */
   private LogLevel(String name, int value) {
      super(name, value);
   }

   /**
    * @param value
    */
   public LogLevel(String value) {
      this(map.get(value).toString(), Integer.parseInt(value));
   }

}
