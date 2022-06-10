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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;


/**
 * XynaAppenderWrapper supports wrapping Appenders through the default mechanism by implementing AppenderAttachable and a xml-configuration
 * as well as by property-configuration of rootAppendersToWrap by detaching the given appenders from the rootLogger and attaching them to himself
 */
public abstract class XynaAppenderWrapper extends AbstractAppender {

  private static final long serialVersionUID = 1L;


  protected Configuration config;
  protected AppenderRef[] refs;
  
  
  XynaAppenderWrapper(String name, Configuration config, AppenderRef[] refs) {
    super(name, null, null);
    this.config = config;
    this.refs = refs;
  }
  
  @Override
  public void start() {
    if (refs == null || refs.length == 0) {
       // we can't really log that can't we
      // either we just don't log without anyone knowing or we do this:
      throw new IllegalArgumentException("No appenders were attached");
    }
    
    super.start();
  }
  
  
  protected void initEvent(LogEvent event) {
    // Set all relevant LoggingEvent fields that were not set at event creation time.
    event.getContextStack(); //NDC
    event.getThreadName();
    event.getContextMap(); //MDC
    event.getSource(); //locationInformation
    event.getMessage();
    event.getThrownProxy();
  }
  
}
