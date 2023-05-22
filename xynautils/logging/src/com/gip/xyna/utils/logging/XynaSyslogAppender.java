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
package com.gip.xyna.utils.logging;



import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 */
@Plugin(name="XynaSyslog", category = "Core", elementType = "appender", printObject = true)
public class XynaSyslogAppender extends SyslogAppender {

  private static final long serialVersionUID = 1L;

  //SyslogAppender unterst�tzt nur SyslogLayout und RFC5424Layout, daher hier eigenes PatternLayout.
  PatternLayout patternLayout;
  
  protected XynaSyslogAppender(String name, Layout<? extends Serializable> layout,
                               AbstractSocketManager manager, PatternLayout patternLayout) {
    super(name, layout, null, true, true, manager, null);
    this.patternLayout = patternLayout;
  }


  /**
   * Maximal size of a syslog message.
   */
  public static final int MESSAGE_MAX_SIZE = 768;

  @PluginFactory
  public static XynaSyslogAppender createAppender(
                                  @PluginAttribute("name") final String name,
                                  @PluginAttribute("host") final String host,
                                  @PluginAttribute(value = "port", defaultInt = 514) final int port,
                                  @PluginAttribute(value = "protocol", defaultString = "UDP") final String protocolStr,
                                  @PluginAttribute(value = "facility", defaultString = "LOCAL0") final Facility facility,
                                  @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charsetName,
                                  @PluginElement("PatternLayout") PatternLayout patternLayout) {
    if (name == null) {
      LOGGER.error("No name provided for XynaSyslogAppender");
      return null;
    }
    
    Layout<? extends Serializable> layout = SyslogLayout.createLayout(facility, false, null, charsetName);

    Protocol protocol = EnglishEnums.valueOf(Protocol.class, protocolStr);
    AbstractSocketManager manager = SyslogAppender.createSocketManager(name, protocol, host, port, 0, null, 0, true, layout, Constants.ENCODER_BYTE_BUFFER_SIZE, null);
    
    if (patternLayout == null) {
      patternLayout = PatternLayout.createDefaultLayout();
    }
    
    return new XynaSyslogAppender(name, layout, manager, patternLayout);
  }

  
  @Override
  public void append(LogEvent event) {
    if (event.getThrown() == null) {
      splitMessage(event);
    }
    else {
      splitThrowable(event);
    }
  }


  private void splitMessage(LogEvent event) {
    String message = event.getMessage().getFormattedMessage();
    
    if (message.length() > MESSAGE_MAX_SIZE) {
      LogEvent logEntry = null;
      String sub_message = null;
      for (int i = 0; i < message.length(); i += MESSAGE_MAX_SIZE) {
        if (message.length() < (i + MESSAGE_MAX_SIZE)) {
          sub_message = message.substring(i, message.length());
        }
        else {
          sub_message = message.substring(i, i + MESSAGE_MAX_SIZE);
        }
        logEntry = copyAndAdjustEvent(event, sub_message);
        formatAndAppend(logEntry);
      }
    }
    else {
      formatAndAppend(event);
    }
  }
  
  private void formatAndAppend(LogEvent event) {
    //Die Message mit dem PatternLayout formatieren.
    event = copyAndAdjustEvent(event, patternLayout.toSerializable(event));
    super.append(event);
  }


  private LogEvent copyAndAdjustEvent(LogEvent original, String message) {
    return new Log4jLogEvent(original.getLoggerName(), original.getMarker(),
                             original.getLoggerFqcn(), original.getLevel(),
                             new SimpleMessage(message), null, original.getContextMap(),
                             original.getContextStack(),
                             original.getThreadName(), original.getSource(),
                             original.getTimeMillis());
  }


  private void splitThrowable(LogEvent event) {
    splitMessage(copyAndAdjustEvent(event, event.getMessage().getFormattedMessage()));
    
    List<String> stackTrace = Throwables.toStringList(event.getThrown());
    for (int i = 0; i < stackTrace.size(); i++) {
      splitMessage(copyAndAdjustEvent(event, stackTrace.get(i)));
    }
  }
  
}
