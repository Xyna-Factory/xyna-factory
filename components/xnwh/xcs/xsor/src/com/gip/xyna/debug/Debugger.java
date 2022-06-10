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
package com.gip.xyna.debug;



import org.apache.log4j.Level;
import org.apache.log4j.Logger;



public class Debugger {

  private static final Debugger debugger = new Debugger();
  private boolean log4j = true;
  private static final Logger logger = Logger.getLogger(Debugger.class.getName());
  private static String classname = Debugger.class.getName();
  

  public static Debugger getInstance() {
    return debugger;
  }


  /**
   * calls toString at some point.
   */
  public void debug(Object o) {
    if (log4j) {
      logger.log(classname, Level.DEBUG, o, null);
    }
  }


  public void debug(Object o, Throwable t) {
    if (log4j) {
      logger.log(classname, Level.DEBUG, o, t);
    }
  }


  public void error(Object o, Throwable t) {
    if (log4j) {
      logger.log(classname, Level.ERROR, o, t);
    }
  }


  public void warn(String o, Throwable t) {
    if (log4j) {
      logger.log(classname, Level.WARN, o, t);
    }
  }


  public void info(Object o) {
    if (log4j) {
      logger.log(classname, Level.INFO, o, null);
    }
  }


  public void trace(Object o) {
    if (log4j) {
      logger.log(classname, Level.TRACE, o, null);
    }
  }
  
  public void trace(Object o, Throwable t) {
    if (log4j) {
      logger.log(classname, Level.TRACE, o, t);
    }
  }
}
