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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;


public class LoggingUtils {

  public static void init(boolean debug) {
    
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
    loggerConfig.setLevel(debug ? Level.DEBUG : Level.INFO);
    for (String appender : loggerConfig.getAppenders().keySet()) {
      loggerConfig.removeAppender(appender);
    }
    org.apache.logging.log4j.core.layout.PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout.createLayout("%r XYNA %-5p [%t] (%F:%L) - %x %m%n", null, config, null, null, false, false, null, null);
    ConsoleAppender consoleAppender = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "console", null, null);
    loggerConfig.addAppender(consoleAppender, debug ? Level.DEBUG : Level.INFO, null);
    ctx.updateLoggers();
    
  }

}
