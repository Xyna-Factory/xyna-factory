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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;


/**
 * XynaAsyncAppender - accepts log messages that get handled by a background thread so the log-call can return early 
 * offers the following options:
 * queueSize: integer default: 10000 - the amount of logging messages buffered
 * blocking: true/false default: true - blocks logging calls if the queue is full
 * discardedMessageThreshold: integer default: 32 - the amount of messages (messages are only discarded if blocking=false) 
 *                                                  that have to be discarded before an associated log messages is generated 
 * maxEventRate: integer - maximum amount of events that will be processed during one second
 */
@Plugin(name = "XynaAsync", category = "Core", elementType = "appender", printObject = true)
public class XynaAsyncAppender extends XynaAppenderWrapper {

  private static final long serialVersionUID = 1L;

  private final static int DEFAULT_QUEUE_SIZE = 10000;
  private final static boolean DEFAULT_BLOCKING_BEHAVIOUR = true;
  private final static int DEFAULT_DISCARDED_EVENT_THRESHOLD = 32;
  private final static int DEFAULT_MAX_EVENT_RATE = 0;
  
  
  private final AtomicInteger discardedMessages;
  
  private LogEvent firstDiscardedEvent;
  private QueueProcessor<LogEvent> processor;
  private Thread processorThread;
  
  private final AtomicBoolean shutdown;
  
  boolean blocking;
  int queueSize;
  int discardedMessageThreshold;
  int maxEventRate;
  

  public XynaAsyncAppender(String name, boolean blocking, int queueSize, int discardedMessageThreshold,
                           int maxEventRate, Configuration config, AppenderRef[] refs) {
    super(name, config, refs);
    discardedMessages = new AtomicInteger(0);
    shutdown = new AtomicBoolean(false);
    
    this.blocking = blocking;
    this.queueSize = queueSize;
    this.discardedMessageThreshold = discardedMessageThreshold;
    this.maxEventRate = maxEventRate;
  }
  
  @Override
  public void start() {
    super.start();
    final long timeBetweenEvents;
    if (maxEventRate > 0) {
      timeBetweenEvents = determineParkNanos(maxEventRate);
    } else {
      timeBetweenEvents = 0;
    }
    processor = new QueueProcessor<LogEvent>(queueSize, new LoggingEventProcessor(timeBetweenEvents));
    startProcessorThread();
  }
  
  @Override
  public void stop() {
    if (shutdown.compareAndSet(false, true)) {
      processor.stop();
      try {
        processorThread.join(500);
      } catch (InterruptedException e) {
        //ntbd
      }
      for (AppenderRef appenderRef : refs) {
        config.getAppender(appenderRef.getRef()).stop();
      }
    }
  }

  public boolean requiresLayout() {
    return false;
  }


  public void append(LogEvent event) {
    event = Log4jLogEvent.createMemento(event);
    if (!shutdown.get()) {
      initEvent(event);
      checkProcessorThread();
      boolean success = tryOffer(event);
      if (!success) {
        addDiscardedEvent(event);
      }
    }
  }
  
  @PluginFactory
  public static XynaAsyncAppender createAppender(@PluginAttribute("name") final String name,
                                                 @PluginAttribute(value = "blocking", defaultBoolean = DEFAULT_BLOCKING_BEHAVIOUR) final boolean blocking,
                                                 @PluginAttribute(value = "queueSize", defaultInt = DEFAULT_QUEUE_SIZE) final int queueSize,
                                                 @PluginAttribute(value = "discardedMessageThreshold", defaultInt = DEFAULT_DISCARDED_EVENT_THRESHOLD) final int discardedMessageThreshold,
                                                 @PluginAttribute(value = "maxEventRate", defaultInt = DEFAULT_MAX_EVENT_RATE) final int maxEventRate,
                                                 @PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
                                                 @PluginConfiguration final Configuration config) {
  
    if (name == null) {
      LOGGER.error("No name provided for AsyncAppender");
      return null;
    }
    if (appenderRefs == null) {
      LOGGER.error("No appender references provided to AsyncAppender {}", name);
    }
    //System.out.println("ASYNC appender wrapping # " + appenderRefs .length + ": " + Arrays.toString(appenderRefs));
    return new XynaAsyncAppender(name, blocking, queueSize, discardedMessageThreshold, maxEventRate, config, appenderRefs);
  }
  
  private void checkProcessorThread() {
    if (!processorThread.isAlive()) {
      startProcessorThread();
    }
  }
  
  
  private void startProcessorThread() {
    if (!shutdown.get()) {
      processorThread = new Thread(processor, "Logging Event Processor");
      processorThread.setDaemon(true);
      processorThread.start();
    }
  }


  private boolean tryOffer(LogEvent event) {
    if (blocking) {
      processor.put(event);
      return true;
    } else {
      return processor.offer(event);
    }
  }


  private void addDiscardedEvent(LogEvent event) {
    int previous = discardedMessages.getAndIncrement();
    if (previous == 0) {
      firstDiscardedEvent = event;
    }
  }


  public void setBlocking(boolean blocking) {
    this.blocking = blocking;
  }
  
  
  public boolean getBlocking() {
    return blocking;
  }
  
  
  public void setQueueSize(int queueSize) {
    this.queueSize = queueSize;
  }
  
  
  public int getQueueSize() {
    return queueSize;
  }
  
  
  public void setMaxEventRate(int maxEventRate) {
    this.maxEventRate = maxEventRate;
  }
  
  
  public int getMaxEventRate() {
    return maxEventRate;
  }
  
  
  
  public void setDiscardedMessageThreshold(int discardedMessageThreshold) {
    this.discardedMessageThreshold = discardedMessageThreshold;
  }
  
  
  public int getDiscardedMessageThreshold() {
    return discardedMessageThreshold;
  }
  
  
  private long determineParkNanos(long targetRate) {
    return Math.round((1000.0 / targetRate) * 1000000.0);
  }

  

  private LogEvent generateDiscardedMessagesEvent() {
    StringBuilder sb = new StringBuilder();
    LogEvent firstDiscardedEvent = XynaAsyncAppender.this.firstDiscardedEvent;
    int discardedAmount = discardedMessages.getAndSet(0);
    long currentStamp = System.currentTimeMillis();
    sb.append(discardedAmount).append(" LoggingEvents have been turned down in the last ")
      .append(currentStamp - firstDiscardedEvent.getTimeMillis()).append("ms.");
    Message message = new SimpleMessage(sb.toString());
    // take less from firstDiscarded ?
    return new Log4jLogEvent(firstDiscardedEvent.getLoggerName(), firstDiscardedEvent.getMarker(),
                             XynaAsyncAppender.class.getName(), Level.WARN,
                             message, firstDiscardedEvent.getThrown(),
                             firstDiscardedEvent.getContextMap(), firstDiscardedEvent.getContextStack(),
                             firstDiscardedEvent.getThreadName(), firstDiscardedEvent.getSource(),
                             System.currentTimeMillis());
  }
  
  
  private class LoggingEventProcessor implements QueueProcessor.ElementProcessor<LogEvent> {
    
    private final long timeBetweenEvents;
    
    private LoggingEventProcessor(long timeBetweenEvents) {
      this.timeBetweenEvents = timeBetweenEvents;
    }

    public void process(LogEvent event) {
      for (AppenderRef appenderRef : refs) {
        Appender appender = config.getAppender(appenderRef.getRef());
        //System.out.println("appending to: " + appender + "   event: " + event + "@" + System.identityHashCode(event));
        appender.append(event);
      }
      if (maxEventRate > 0) {
        LockSupport.parkNanos(timeBetweenEvents);
      }
      if (discardedMessages.get() > discardedMessageThreshold) {
        LogEvent discardedEvent = generateDiscardedMessagesEvent();
        for (AppenderRef appenderRef : refs) {
          Appender appender = config.getAppender(appenderRef.getRef());
          appender.append(discardedEvent);
        }
      }
    }
    
  }
  
}
