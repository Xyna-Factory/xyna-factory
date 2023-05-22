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
package com.gip.xyna.xnwh.persistence;



import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.StackLocatorUtil;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;


public abstract class XynaSQLUtilsLogger implements SQLUtilsLogger {
  
  private static final Logger staticLogger = CentralFactoryLogging.getLogger(XynaSQLUtilsLogger.class);
  
  private final Logger logger;
  private final String fqnOfCallingClass;
  private Level loglevel;

//  private long lastSeenLogConfigDate = System.currentTimeMillis();
  
  private static final String warehouseClassName =  WarehouseRetryExecutor.class.getName();
  
  /**
   * if logger is null, the logger is created dynamically using "xyna.sql.&lt;callingClass&gt;".
   */
  public XynaSQLUtilsLogger(Logger logger, String fqnOfCallingClass, Level loglevel) {
    this.logger = logger;
    this.fqnOfCallingClass = fqnOfCallingClass;
    this.loglevel = loglevel;
  }
  
  public void logSQL(String sql) {
    internalLog(loglevel, sql, null);
  }
  
  public void logSQL(LoggingMessageGenerator generator) {
    internalLog(loglevel, generator, null);
  }
  
  /**
   * Sorgt auf interessante Weise f�r das richtige Setzen des aufrufenden Zeile: (keine Zeile dieser Datei, sondern
   * der Aufrufer) siehe http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Category.html
   * http://marc.info/?l=log4j-user&amp;m=99859247618691&amp;w=2 That's why you need to provide the fully-qualified classname.
   * If you use the wrapper I showed, you will not have this problem. That is, the class and method name of the
   * wrapper caller, not the wrapper itself, will be logged. The code which determines the logging method looks one
   * past the fully-qualified classname in the callstack.
   * @param level
   * @param str
   * @param t
   */
  protected void internalLog(Level level, String str, Throwable t) {
    if (logger == null) {

      // TODO: Aufw�ndigen Check nicht machen, wenn man anhand der Konfiguration schon feststellen
      //       kann, dass die Meldung sowieso nicht geloggt werden wird. Vorschlag:
//      long lastFactoryLogConfigDate = CentralFactoryLogging.getLastLogConfigChangeDate();
//      if (lastFactoryLogConfigDate != lastSeenLogConfigDate) {
//        List<Logger> configuredLoggers = CentralFactoryLogging.listLogger(true);
//        for (Logger logger: configuredLoggers) {
//          if (logger.getName().startsWith("xyna.sql")) {
//            
//          }
//        }
//        lastSeenLogConfigDate = lastFactoryLogConfigDate;
//      }

      StackTraceElement ste = StackLocatorUtil.calcLocation(fqnOfCallingClass);
      if (ste == null) {
        staticLogger.log(level, str, t);
      } else {
        String className = ste.getClassName();
        String callingClass = fqnOfCallingClass;
        if (className.equals(warehouseClassName)) { //wenn warehouseretryexecutor genutzt wird, den nicht als sql-verursacher anzeigen
          callingClass = warehouseClassName;
          ste = StackLocatorUtil.calcLocation(callingClass);
          className = ste.getClassName();
        }

        String loggerName = "xyna.sql." + className + "." + ste.getMethodName();
        if (staticLogger.isTraceEnabled()) {
          //wenn man herausfinden will, welches sql von welchem logger geloggt wird...
          staticLogger.trace("using logger " + loggerName);
        }
        Logger localLogger = Logger.getLogger(loggerName);
        localLogger.log(callingClass, level, str, t);
      }
    } else {
      logger.log(fqnOfCallingClass, level, str, t);
    }
  }


  protected void internalLog(Level level, LoggingMessageGenerator generator, Throwable t) {
    if (logger == null) {
      StackTraceElement ste = StackLocatorUtil.calcLocation(fqnOfCallingClass);
      if (ste == null) {
        staticLogger.log(fqnOfCallingClass, level, generator.generateLogMessage(), t);
      } else {

        String className = ste.getClassName();
        String callingClass = fqnOfCallingClass;
        if (className.equals(warehouseClassName)) { //wenn warehouseretryexecutor genutzt wird, den nicht als sql-verursacher anzeigen
          callingClass = warehouseClassName;
          ste = StackLocatorUtil.calcLocation(callingClass);
          className = ste.getClassName();
        }

        String loggerName = "xyna.sql." + className + "." + ste.getMethodName();
        if (staticLogger.isTraceEnabled()) {
          //wenn man herausfinden will, welches sql von welchem logger geloggt wird...
          staticLogger.trace("using logger " + loggerName);
        }
        Logger localLogger = Logger.getLogger(loggerName);
        if (isLoggingEnabled(localLogger)) {
          localLogger.log(callingClass, level, generator.generateLogMessage(), t);
        }
      }
    } else {
      if (isLoggingEnabled(logger)) {
        logger.log(fqnOfCallingClass, level, generator.generateLogMessage(), t);
      }
    }
  }
  
  
  private boolean isLoggingEnabled(Logger logger) {
    if (loglevel == Level.TRACE) {
      return logger.isTraceEnabled();
    } else if (loglevel == Level.DEBUG) {
      return logger.isDebugEnabled();
    } else {
      return logger.isInfoEnabled();
    }
  }
  
  public static interface LoggingMessageGenerator {
    
    public String generateLogMessage();
    
  }

}
