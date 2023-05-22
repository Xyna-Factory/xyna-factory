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

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.logging.log4j.util.StackLocator;


/**
 * Class for logging syslog message.
 * <p>
 * Syslog is a logging system mostly used on unix systems. It's a standard for
 * forwarding log messages in an IP network.
 * 
 * 
 */
@Deprecated
public class SysLogger extends Logger {

   /**
    * Default port for syslog is 514.
    */
   public static final int DEFAULT_PORT = 514;
   /**
    * Default host for syslog is localhost
    */
   public static final String DEFAULT_HOST = "localhost";

   private static SysLogger me = null;
   protected static final Facility DEFAULT_FACILITY = Facility.LOCAL0;

   // private Formatter formatter;

   /**
    * Get a SysLogger instance which communicates with a syslog process on the
    * given host. Uses local0 as defautl facility.
    * 
    * @param host
    *              Target machine with syslog process
    * @param port
    *              Syslog port
    * @param log_name
    *              name of the logger
    * @return SysLogger instance
    * @throws SyslogException
    */
   // TODO: respect parameters
   public static synchronized SysLogger getLogger(String host, int port,
         String log_name) {
      if (me == null) {
         me = new SysLogger(host, port, log_name, DEFAULT_FACILITY);
      }
      return me;
   }

   /**
    * Get a SysLogger instance which communicates with a syslog process on the
    * given host.
    * 
    * @param host
    *              Target machine with syslog process
    * @param port
    *              Syslog port
    * @param log_name
    *              name of the logger
    * @param facility
    *              The facility of the logger
    * @return SysLogger instance
    * @throws SyslogException
    */
   public static synchronized SysLogger getLogger(String host, int port,
         String log_name, Facility facility) {
      if (me == null) {
         me = new SysLogger(host, port, log_name, facility);
      }
      return me;
   }

   /**
    * Initialize SysLogger with default values and respectively get the current
    * SysLogger instance.
    * 
    * @return SysLogger instance
    * @throws SyslogException
    */
   public static synchronized SysLogger getLogger() {
      if (me == null) {
         me = new SysLogger(DEFAULT_HOST, DEFAULT_PORT, "", DEFAULT_FACILITY);
         // me = new SysLogger("");
         // me.addHandler(new ConsoleHandler());
         // me.setLevel(Level.ALL);
      }
      return me;
   }

   /**
    * Initialize SysLogger. If it is not possible to connect to syslog the log
    * message will be written to System.err.
    * <p>
    * Use SyslogHandler and SyslogFormatter.
    */
   protected SysLogger(String host, int port, String log_name, Facility facility) {
      this(log_name);
      // TODO: is SocketHandler possible?
      try {
         Handler handler = new SyslogHandler(host, port);
         handler.setFormatter(new SyslogFormatter(facility));
         addHandler(handler);
      } catch (Exception e) {
         System.err.println("Error during initialization of SysLogger! ("
               + e.getClass() + " " + e.getMessage() + ")");
         System.err
               .println("ConsoleHandler will be used instead of SyslogHandler");
         addHandler(new ConsoleHandler());
      }
   }

   /**
    * Set a formatter for all handlers.
    * 
    * @param formatter
    *              Formatter to use
    */
   public void setFormatter(Formatter formatter) {
      Handler[] handlers = me.getHandlers();
      for (Handler handler : handlers) {
         if (handler instanceof SyslogHandler) {
            handler.setFormatter(formatter);
            break;
         }
      }
   }

   /**
    * Initialize SysLogger.
    * <p>
    * No Handler or Formatter will be defined.
    */
   protected SysLogger(String log_name) {
      super(log_name, null);
      setUseParentHandlers(false);
      setLevel(Level.ALL);
   }

   /**
    * Log message with the given severity.
    * 
    * @param severity
    *              Syslog level to log
    * @param log_message
    *              Message to log
    */
   public void syslog(Severity severity, String log_message) {
      super.log(severity.getLevel(), log_message);
   }

   /**
    * Log message with the given severity. Includes given parameters into the
    * log message.
    * 
    * @param severity
    *              Syslog level to log
    * @param log_message
    *              Message to log
    * @param params
    *              parameters for the log message
    */
   public void syslog(Severity severity, String log_message, Object[] params) {
      super.log(severity.getLevel(), log_message, params);
   }

   /**
    * Log exceptions with the given severity.
    * 
    * @param severity
    *              Syslog level to log
    * @param log_message
    *              Message to log
    * @param thrown
    *              Exception to log
    */
   public void syslog(Severity severity, String log_message, Throwable thrown) {
      super.log(severity.getLevel(), log_message, thrown);
   }

   /**
    * @see java.util.logging.Logger#log(LogRecord)
    */
   public void log(LogRecord record) {
      record.setLevel(Severity.getSeverity(record.getLevel()).getLevel());
      record.setSourceClassName(getCallerClassName());
      record.setSourceMethodName(getCallerMethodName());
      super.log(record);
   }

   /**
    * Log a message with throwable information. Allow parameters for the
    * message.
    * 
    * @param severity
    * @param log_message
    * @param thrown
    * @param params
    */
   public void syslog(Severity severity, String log_message, Throwable thrown,
         Object[] params) {
      LogRecord record = new LogRecord(severity.getLevel(), log_message);
      record.setThrown(thrown);
      record.setParameters(params);
      super.log(record);
   }

   /**
    * Log an exception. Class name, message and cause of the exception will be
    * stored.
    * 
    * @param e
    *              Exception to log
    */
   public void logException(Throwable e) {
      try {
         error(e.getClass().getName() + " " + e.getMessage());
         for (int i = 0; i < e.getStackTrace().length; i++) {
            debug("   at " + e.getStackTrace()[i]);
         }
         if (e.getCause() != null) {
            error("caused by :");
            logException(e.getCause());
         }
      } catch (Exception f) {
         f.printStackTrace();
      }
   }

   /**
    * Log EMERGENCY message
    * 
    * @param log_message
    *              The log message
    */
   public void emergency(String log_message) {
      syslog(Severity.EMERGENCY, log_message);
   }

   /**
    * Log EMERGENCY message.
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void emergency(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.EMERGENCY, log_message);
      }
      syslog(Severity.EMERGENCY, log_message, params);
   }

   /**
    * Log ALERT message
    * 
    * @param log_message
    *              The log message
    */
   public void alert(String log_message) {
      syslog(Severity.ALERT, log_message);
   }

   /**
    * Log ALERT message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void alert(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.ALERT, log_message);
      }
      syslog(Severity.ALERT, log_message, params);
   }

   /**
    * Log CRITICAL message
    * 
    * @param log_message
    *              The log message
    */
   public void critical(String log_message) {
      syslog(Severity.CRITICAL, log_message);
   }

   /**
    * Log CRITICAL message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void critical(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.CRITICAL, log_message);
      }
      syslog(Severity.CRITICAL, log_message, params);
   }

   /**
    * Log ERROR message
    * 
    * @param log_message
    *              The log message
    */
   public void error(String log_message) {
      syslog(Severity.ERROR, log_message);
   }

   /**
    * Log ERROR message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void error(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.ERROR, log_message);
      }
      syslog(Severity.ERROR, log_message, params);
   }

   /**
    * Log WARNING message
    * 
    * @param log_message
    *              The log message
    */
   public void warning(String log_message) {
      syslog(Severity.WARNING, log_message);
   }

   /**
    * Log WARNING message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void warning(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.WARNING, log_message);
      }
      syslog(Severity.WARNING, log_message, params);
   }

   /**
    * Log NOTICE message
    * 
    * @param log_message
    *              The log message
    */
   public void notice(String log_message) {
      syslog(Severity.NOTICE, log_message);
   }

   /**
    * Log NOTICE message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void notice(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.NOTICE, log_message);
      }
      syslog(Severity.NOTICE, log_message, params);
   }

   /**
    * Log INFORMATIONAL message
    * 
    * @param log_message
    *              The log message
    */
   public void informational(String log_message) {
      syslog(Severity.INFORMATIONAL, log_message);
   }

   /**
    * Log INFORMATIONAL message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void informational(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.INFORMATIONAL, log_message);
      }
      syslog(Severity.INFORMATIONAL, log_message, params);
   }

   /**
    * Log DEBUG message
    * 
    * @param log_message
    *              The log message
    */
   public void debug(String log_message) {
      syslog(Severity.DEBUG, log_message);
   }

   /**
    * Log DEBUG message
    * 
    * @param log_message
    *              The log message
    * @param params
    *              parameters for the log message
    */
   public void debug(String log_message, Object... params) {
      if (params == null) {
         syslog(Severity.DEBUG, log_message);
      }
      syslog(Severity.DEBUG, log_message, params);
   }

   private String getCallerClassName() {
      int stackPosition = 2;
      StackLocator locator = StackLocator.getInstance();
      String caller = locator.getCallerClass(stackPosition).getName();
      // TODO: check against all subclasses of Logger
      while (caller.equals(this.getClass().getName())
            || caller.equals("java.util.logging.Logger")) {
         caller = locator.getCallerClass(++stackPosition).getName();
      }
      return caller;
   }

   private String getCallerMethodName() {
      // TODO: implement getCallerMethodName
      return "";
   }

}
