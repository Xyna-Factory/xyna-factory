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
package xact.ssh.impl;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;



public class LogAdapter implements com.jcraft.jsch.Logger {
  
  private final Logger logger;
  
  public LogAdapter(Logger logger) {
    this.logger = logger;
  }

  public boolean isEnabled(int code) {
    return logger.isEnabledFor(codeToLogLevel(code));
  }


  public void log(int code, String message) {
    logger.log(codeToLogLevel(code), message);
  }
  
  
  private static Level codeToLogLevel(int code) {
    switch (code) {
      case com.jcraft.jsch.Logger.DEBUG :
        return Level.DEBUG;
      case com.jcraft.jsch.Logger.ERROR :
        return Level.ERROR;
      case com.jcraft.jsch.Logger.FATAL :
        return Level.FATAL;
      case com.jcraft.jsch.Logger.INFO :
        return Level.INFO;
      case com.jcraft.jsch.Logger.WARN :
        return Level.WARN;
      default :
        return Level.DEBUG;
    }
  }
  
  
  public static int logLevelToCode(Level lvl) {
    if (lvl == Level.DEBUG) {
      return com.jcraft.jsch.Logger.DEBUG;
    } else if (lvl == Level.ERROR) {
      return com.jcraft.jsch.Logger.ERROR;
    } else if (lvl == Level.FATAL) {
      return com.jcraft.jsch.Logger.FATAL;
    } else if (lvl == Level.INFO) {
      return com.jcraft.jsch.Logger.INFO;
    } else if (lvl == Level.WARN) {
      return com.jcraft.jsch.Logger.WARN;
    } else {
      return com.jcraft.jsch.Logger.DEBUG;
    }
  }

}
