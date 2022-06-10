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

package com.gip.xyna;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;


/**
 * hier sollen später logger programmatisch ermittelt und geändert werden können.
 */
public class CentralFactoryLogging {


  private static long logPropertiesChangeDate = System.currentTimeMillis();


  // TODO see log4j implementation: write a LoggerRepository implementation that creates XynaLogger instances
  //  static {
  //    LogManager.setRepositorySelector(new RepositorySelector() {
  //
  //      public LoggerRepository getLoggerRepository() {
  //        return null;
  //      }
  //    }, new Object());
  //  }


  public CentralFactoryLogging() {
  }


  public static Logger getLogger(Class<?> c) {
    Logger logger = Logger.getLogger(c.getName());
    return logger;
  }


  //  public static class XynaLogger extends Logger {
  //
  //    protected XynaLogger(String name) {
  //      super(name);
  //    }
  //    
  //  }

  
  public static Map<String,Level> listLogger(boolean configuredLoggersOnly) {
    Map<String,Level> loggers = new HashMap<String, Level>();
    
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    
    loggers.put("<root>", ctx.getRootLogger().getLevel());
    
    Map<String, LoggerConfig> loggerConfigs = ctx.getConfiguration().getLoggers();
    for (LoggerConfig loggerConfig : loggerConfigs.values()) {
      loggers.put(loggerConfig.getName(), loggerConfig.getLevel());
    }
    
    if (!configuredLoggersOnly) {
      Collection<org.apache.logging.log4j.core.Logger> currentLoggers = ctx.getLoggers();
      for (org.apache.logging.log4j.core.Logger logger : currentLoggers) {
        loggers.put(logger.getName(), logger.getLevel());
      }
    }
    
    return loggers;
  }
  
  
  public static void loadLogProperties() throws XynaException {
    String propertyFilename = System.getProperty(Constants.LOG4J_CONFIGURATION_KEY);
    if (propertyFilename == null) {
      throw new Ex_FileAccessException(propertyFilename);
    }
    loadLogProperties(propertyFilename);
    executeLogChangeListeners();
  }
  
  
  public static void loadLogProperties(String propertyFilename) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    URI uri = new File(propertyFilename).toURI();
    ctx.setConfigLocation(uri);
    
    logPropertiesChangeDate = System.currentTimeMillis();
    executeLogChangeListeners();
  }
  
  
  
  public static void setLogLevel(String loggerName, String levelString) { 
    if (loggerName.equalsIgnoreCase("root")) {
      loggerName = LogManager.ROOT_LOGGER_NAME;
    }
    
    Level level = Level.toLevel(levelString);
    
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggers().get(loggerName);
    if (loggerConfig == null) {
      //noch keine Config für diesen Logger vorhanden, daher nun eine neue anlegen,
      //damit nicht die Parent-Config überschrieben wird.
      loggerConfig = new LoggerConfig(loggerName, level, true);
      ctx.getConfiguration().addLogger(loggerName, loggerConfig);
    } else {
      //Level ändern
      loggerConfig.setLevel(level);
    }
    ctx.updateLoggers();
    
    logPropertiesChangeDate = System.currentTimeMillis();
    executeLogChangeListeners();
  }


  public static long getLastLogConfigChangeDate() {
    return logPropertiesChangeDate;
  }
  

  public interface LogChangeListener {
    public void onLogChanged();
  }


  private static final List<LogChangeListener> registeredListeners = new ArrayList<CentralFactoryLogging.LogChangeListener>();


  public static void registerLogChangeListener(LogChangeListener l) {
    synchronized (registeredListeners) {
      registeredListeners.add(l);
    }
  }


  public static void deregisterLogChangeListener(LogChangeListener l) {
    synchronized (registeredListeners) {
      registeredListeners.remove(l);
    }
  }


  public static void executeLogChangeListeners() {
    synchronized (registeredListeners) {
      for (LogChangeListener l : registeredListeners) {
        l.onLogChanged();
      }
    }
  }
  
  private static final class LogMessage {
    
    final String msg;
    final long time;
    
    private LogMessage(String msg, long time) {
      this.msg = msg;
      this.time = time;
    }

    public String toString() {
      return time + "=" + msg;
    }
  }


  private static final Map<Long, List<LogMessage>> logs = new ConcurrentHashMap<Long, List<LogMessage>>();
  private static final XynaPropertyInt logCnt = new XynaPropertyInt("xyna.logging.ordertimings.count", 0);
  private static final int NUMBER_ORDERTIMING_CALLS = 11; //wieviele elemente kann liste maximal haben

  /*
   * TODO suspend-resume unterstützung. 
   *      statistiken pro ordertype
   *      aufräumen von daten: 
   *        - fertiggelaufene aufträge separat von laufenden handhaben
   *        - fertiggelaufene als ringbuffer
   */
  public static void logOrderTiming(long id, String s) {
    int c = logCnt.get();
    if (c == 0) {
      return;
    }
    if (c > 0 && logs.size() > c) {
      logs.clear();
    }  
    logOrderTimingInternal(id, s, System.nanoTime());
  }


  public static void logOrderTiming(long id, String s, long t) {
    int c = logCnt.get();
    if (c == 0) {
      return;
    }
    if (c > 0 && logs.size() > c) {
      logs.clear();
    }
    logOrderTimingInternal(id, s, t);
  }


  private static void logOrderTimingInternal(long id, String s, long t) {
    List<LogMessage> l = logs.get(id);
    if (l == null) {
      l = new ArrayList<CentralFactoryLogging.LogMessage>(NUMBER_ORDERTIMING_CALLS);
      logs.put(id, l);
    }
    if (l.size() < NUMBER_ORDERTIMING_CALLS * 16) { //threadpool voll -> scheduler macht retries, die zu mehrfachen aufrufen führen
      l.add(new LogMessage(s, t));
    }
  }


  public static void printOrderTimingForOrder(Writer w, long id) throws IOException {
    List<LogMessage> l = logs.get(id);
    LogMessage last = null;
    w.write(id + " @ " + l.get(0).time + ":\n");
    for (LogMessage m : l) {
      if (last != null) {
        w.write(String.format(" %,12d %s", m.time - last.time, "ns until <" + m.msg + ">\n"));
      } else {
        w.write("<" + m.msg + ">\n");
      }
      last = m;
    }
  }
  
  private static class Stats {
    private int cnt;
    private long max = 0;
    private long min = Long.MAX_VALUE;
    private long sum;
    private long avg;
  }


  public static void printAllOrderTimings(Writer w) throws IOException {
    Iterator<Entry<Long, List<LogMessage>>> it = logs.entrySet().iterator();
    Map<String, Stats> statistics = new HashMap<String, Stats>();
    while (it.hasNext()) {
      Entry<Long, List<LogMessage>> e = it.next();
      w.write(e.getKey() + ": " + e.getValue() + "\n");

      //stats
      LogMessage last = null;
      List<LogMessage> l = e.getValue();
      int cnt = l.size();
      for (int i = 0; i < cnt; i++) {
        LogMessage m = l.get(i);
        if (last != null) {
          String key = "<" + last.msg + "> to <" + m.msg + ">";
          Stats s = statistics.get(key);
          if (s == null) {
            s = new Stats();
            statistics.put(key, s);
          }
          s.cnt++;
          long diff = m.time - last.time;
          s.max = Math.max(s.max, diff);
          s.sum += diff;
          s.min = Math.min(s.min, diff);
        }
        last = m;
      }
    }
    List<Entry<String, Stats>> entrySet = new ArrayList<Entry<String, Stats>>(statistics.entrySet());
    Collections.sort(entrySet, new Comparator<Entry<String, Stats>>() {

      public int compare(Entry<String, Stats> o1, Entry<String, Stats> o2) {
        long avg1 = getAvg(o1);
        long avg2 = getAvg(o2);
        if (avg1 < avg2) {
          return 1;
        }
        if (avg1 > avg2) {
          return -1;
        }
        return 0;
      }


      private long getAvg(Entry<String, Stats> o1) {
        long avg1 = o1.getValue().avg;
        if (avg1 == 0) {
          avg1 = o1.getValue().sum / o1.getValue().cnt;
          o1.getValue().avg = avg1;
        }
        return avg1;
      }

    });
    w.write("\nStatistics (Average, Min, Max, Cnt, Type):\n");
    for (Entry<String, Stats> e : entrySet) {
      w.write(String.format(" %,12d  %,12d  %,12d  %,7d %s%n", e.getValue().avg, e.getValue().min, e.getValue().max, e.getValue().cnt,
                            e.getKey()));
    }
  }

}
