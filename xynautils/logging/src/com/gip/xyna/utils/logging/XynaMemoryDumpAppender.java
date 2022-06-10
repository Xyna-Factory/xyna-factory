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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;


/**
 * XynaMemoryDumpAppender - collects log messages in a buffer and dumps them once a log messages with a certain severity arrives 
 * offers the following options:
 * bufferSize: integer default: 5000 - the amount of logging messages buffered
 * dumpLevel: (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) default: ERROR - the level of a log message triggering a dump
 * dumpRate: integer default: 100 - events per second to handle during a dump
 */
@Plugin(name = "XynaMemoryDump", category = "Core", elementType = "appender", printObject = true)
public class XynaMemoryDumpAppender extends XynaAppenderWrapper {
  
  private final static int DEFAULT_EVENT_BUFFER_SIZE = 5000;
  private final static int DEFAULT_DUMP_RATE = 100;

  private LogEvent[] eventRing;
  private AtomicInteger ringIndex = new AtomicInteger();
  
  int bufferSize;
  int dumpRate;
  Level dumpLevel;
  
  public XynaMemoryDumpAppender(String name, Configuration config, AppenderRef[] refs,
                                int bufferSize, int dumpRate, Level dumpLevel) {
    super(name, config, refs);
    this.bufferSize = bufferSize;
    this.dumpRate = dumpRate;
    this.dumpLevel = dumpLevel;
  }
  
  
  @PluginFactory
  public static XynaMemoryDumpAppender createAppender(@PluginAttribute("name") final String name,
                                                      @PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_EVENT_BUFFER_SIZE) final int bufferSize,
                                                      @PluginAttribute(value = "dumpRate", defaultInt = DEFAULT_DUMP_RATE) final int dumpRate,
                                                      @PluginAttribute(value = "dumpLevel", defaultString = "ERROR") final String dumpLevel,
                                                      @PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
                                                      @PluginConfiguration final Configuration config) {
    
    if (name == null) {
      LOGGER.error("No name provided for AsyncAppender");
      return null;
    }
    if (appenderRefs == null) {
      LOGGER.error("No appender references provided to AsyncAppender {}", name);
    }

    return new XynaMemoryDumpAppender(name, config, appenderRefs, bufferSize, dumpRate,  Level.toLevel(dumpLevel));
  }
  
  @Override
  public void start() {
    super.start();
    eventRing = new LogEvent[bufferSize];
  }
  
  
  public void stop() {
    for (AppenderRef appenderRef : refs) {
      config.getAppender(appenderRef.getRef()).stop();
    }
  }


  public boolean requiresLayout() {
    return false;
  }


  public void append(LogEvent event) {
    initEvent(event);
    if (event.getLevel().isMoreSpecificThan(dumpLevel)) {
      dump(event);
    } else {
      addToRing(event);
    }
  }

  
  private void addToRing(LogEvent event) {
    int index = ringIndex.incrementAndGet();
    if (index >= 0) {
      if (index < bufferSize) {
        eventRing[index] = event;
      } else {
        if (ringIndex.compareAndSet(index, 0)) {
          eventRing[0] = event;
        } else {
          addToRing(event);
        }
      }
    } else {
      waitTillRingIndexPositive();
      addToRing(event);
    }
  }
  

  private void dump(LogEvent triggeringEvent) {
    int oldIndex = ringIndex.getAndSet(Integer.MIN_VALUE);
    if (oldIndex >= 0) {
      try {
        long timeBetweenEvents = Math.max(1, 1000 / dumpRate);
        for (int i = oldIndex + 1; i < oldIndex + bufferSize + 1; i++) {
          int adjustedIndex = i % bufferSize;
          LogEvent event = eventRing[adjustedIndex];
          eventRing[adjustedIndex] = null;
          if (event != null) {
            for (AppenderRef appenderRef : refs) {
              Appender appender = config.getAppender(appenderRef.getRef());
              appender.append(event);
            }
            try {
              Thread.sleep(timeBetweenEvents);
            } catch (InterruptedException e) {
              // ntbd
            }
          }
        }
        for (AppenderRef appenderRef : refs) {
          Appender appender = config.getAppender(appenderRef.getRef());
          appender.append(triggeringEvent);
        }
      } finally {
        ringIndex.set(oldIndex);
      }
    } else {
      waitTillRingIndexPositive();
      dump(triggeringEvent);
    }
  }


  private void waitTillRingIndexPositive() {
    int tries = 0;
    while (ringIndex.get() < 0) {
      if (tries > 500) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          // ntbd
        }
      } else if (tries > 100) {
        Thread.yield();
      }
    }
  }


  public int getBufferSize() {
    return bufferSize;
  }

  
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  
  public int getDumpRate() {
    return dumpRate;
  }

  
  public void setDumpRate(int dumpRate) {
    this.dumpRate = dumpRate;
  }

  
  public String getDumpLevel() {
    return dumpLevel.toString();
  }

  
  public void setDumpLevel(String dumpLevel) {
    this.dumpLevel = Level.toLevel(dumpLevel);
  }

}
