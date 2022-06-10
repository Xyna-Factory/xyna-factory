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

import java.io.*;
import java.util.logging.*;

/**
 * Diese Klasse dient dem Logging. Sie ist initial auf Konsolen-Logging
 * eingestellt und loggt alle OBLog-Level. Momentan gibt es nur einen zentralen
 * Logger in einer JVM, eventuell müssen hier noch Anpassungen stattfinden,
 * damit der Server mehrere Logger öffnen kann.
 * 
 * 
 */
@Deprecated
public class ConsoleLogger extends Logger {

   // TODO: Logger only for OC4J?
   // statische Variable: es gibt halt pro OC4J nur diesen einen Logger
   public static ConsoleLogger log = null;

   private final static String _loggerString = "gip.xyna";

   private static ConsoleHandler _conHan = null;
   private static FileHandler _fileHandler = null;
   private static Formatter _formatter = null;

   private final static String _sessPre = "[";
   private final static String _sessPost = "]";

   static { // mit Minimal-Konfiguration initialisieren
      setLog(LogLevel.TEST, null, false);
   }

   /**
    * Belegt die Member-Variable _log, falls sie schon belegt ist, wird sie neu
    * belegt.
    * 
    * @param fileName
    *              optionaler Dateiname, wenn null, kein File-Logging
    * @param level
    *              level
    */
   public static void setLog(Level level, String fileName) {
      setLog(level, fileName, true);
   }

   /**
    * Belegt die Member-Variable _log, falls sie schon belegt ist, bleibt die
    * alte Belegung erhalten es sei den force=true.
    * 
    * @param fileName
    *              optionaler Dateiname, wenn null, kein File-Logging
    * @param level
    *              level
    * @param force
    */
   public static void setLog(Level level, String fileName, boolean force) {
      if (log != null && force == false) {
         return;
      }
      // (log==null || force=true)
      System.out.println("Log-System initialize with level: " + level);
      System.out.println("Log-System initialize with logFile: "
            + (fileName == null ? "-" : fileName));
      log = new ConsoleLogger(_loggerString, null);
      log.setLevel(level);
      _formatter = new LogFormatter();

      _conHan = new ConsoleHandler();
      _conHan.setLevel(level);
      _conHan.setFormatter(_formatter);
      log.addHandler(_conHan);

      if (fileName != null && fileName.length() > 0) {
         try {
            _fileHandler = new FileHandler(fileName, 500000, 1, true);
            _fileHandler.setLevel(level);
            _fileHandler.setFormatter(_formatter);
            log.addHandler(_fileHandler);
         } catch (Exception e) {
            System.err.println(getLogPrefix(ConsoleLogger.class.getName())
                  + " Could not open Logfile: " + fileName);
         }
      } else {
         // Kein File-Logging
      }
   }

   /**
    * @param name
    * @param resourceBundleName
    */
   private ConsoleLogger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   /**
    * @see java.util.logging.Logger#setLevel(java.util.logging.Level)
    */
   public void setLevel(Level level) {
      super.setLevel(level);

      // Loglevel der Handler setzen
      Handler[] handlers = getHandlers();
      for (int i = 0; i < handlers.length; i++) {
         handlers[i].setLevel(level);
      }
   }

   /**
    * Ausgabe von kritischen Zuständen/Fehlern.
    * 
    * @param logMarker
    *              prefix for log line
    * @param msg
    */
   public void panic(String logMarker, String msg) {
      log(LogLevel.PANIC, getLogPrefix(logMarker) + msg);
   }

   /**
    * Ausgabe von Fehlern.
    * 
    * @param logMarker
    * @param msg
    */
   public void error(String logMarker, String msg) {
      log(LogLevel.ERROR, getLogPrefix(logMarker) + msg);
   }

   /**
    * Ausgabe von Warnungen.
    * 
    * @param logMarker
    * @param msg
    */
   public void warning(String logMarker, String msg) {
      log(LogLevel.WARNING, getLogPrefix(logMarker) + msg);
   }

   /**
    * Informelle Ausgaben.
    * 
    * @param logMarker
    * @param msg
    */
   public void info(String logMarker, String msg) {
      log(LogLevel.INFO, getLogPrefix(logMarker) + msg);
   }

   /**
    * Ausgabe von SQL-DML-Befehlen.
    * 
    * @param logMarker
    * @param msg
    */
   public void dml(String logMarker, String msg) {
      log(LogLevel.DML, getLogPrefix(logMarker) + msg);
   }

   /**
    * Zur Ausgabe von SQL-Befehlen die kein DML sind.
    * 
    * @param logMarker
    * @param msg
    */
   public void sql(String logMarker, String msg) {
      log(LogLevel.SQL, getLogPrefix(logMarker) + msg);
   }

   /**
    * Debug-Ausgaben.
    * 
    * @param logMarker
    * @param msg
    */
   public void debug(String logMarker, String msg) {
      log(LogLevel.DEBUG, getLogPrefix(logMarker) + msg);
   }

   /**
    * Test-Logging.
    * 
    * @param logMarker
    * @param msg
    */
   public void test(String logMarker, String msg) {
      log(LogLevel.TEST, getLogPrefix(logMarker) + msg);
   }

   /**
    * Liefert den Stacktrace an der aufrufenden Stelle und schreibt ihn auf die
    * DEBUG-Konsole.
    * 
    * @param logMarker
    * @param msg
    */
   public void stackTrace(String logMarker, String msg) {
      Exception e = new Exception();
      Writer sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      String s = sw.toString();
      log(LogLevel.DEBUG, getLogPrefix(logMarker) + msg
            + " DEBUG-Stacktrace:\n" + s);
   }

   /**
    * Ein Assert, das ein Panic auslöst wenn 'b'=false ist.
    * 
    * @param b
    * @param string
    */
   public void ensure(String logMarker, boolean b, String string) {
      if (!b) {
         panic(logMarker, "ensure failed: " + string);
      }
   }

   /**
    * Liefert die Session-ID zurück, falls der Context gesetzt ist.
    * 
    * @param con
    *              Wenn con==null, dann kann keine Session zurückgeliefert
    *              werden.
    * @return
    */
   private static String getLogPrefix(String logMarker) {
      if ((logMarker != null) && (logMarker.length() > 0)) {
         return _sessPre + logMarker + _sessPost + " ";
      }
      return "";
   }

   /**
    * Gibt Informationen über den aktuellen Speicherverbrauch der VM aus
    * 
    * @param str
    */
   public void memory(String str) {
      Runtime rt = Runtime.getRuntime();
      // alle Werte in KByte
      long processMem = rt.totalMemory() / 1024;
      long freeProcessMem = rt.freeMemory() / 1024;
      long usedProcessMem = processMem - freeProcessMem;
      long maxMem = rt.maxMemory() / 1024;
      long freeMem = maxMem - usedProcessMem;

      ConsoleLogger.log.debug(null, "[Memory-Info] used: " + usedProcessMem
            + " KB" + ", free: " + freeMem + " KB, max: " + maxMem + " KB"
            + " [" + str + "]");
      /*
       * if (freeMem < 15*1024) { // < n MB System.err.println("[Memory-Warning:
       * GarbageCollector called] used: " + usedProcessMem+ " KB" + ", free: " +
       * freeMem +" KB, max: " + maxMem + " KB"+" ["+str+"]" ); rt.gc(); //
       * Garbage Collector anwerfen return; } if (freeMem < 5*1024) { // < n MB //
       * Meldung bringen System.err.println("[Memory-Warning: insufficient free
       * memory] used: " + usedProcessMem+ " KB" + ", free: " + freeMem +" KB,
       * max: " + maxMem + " KB"+" ["+str+"]" ); }
       */
   }
}
