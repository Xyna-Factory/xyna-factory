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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * AbstractLoggerWrapper ist eine Basis für Wrapper der Klasse org.apache.log4j.Logger.
 * 
 * Leider kann nicht ohne Probleme eine Subklasse von Logger gebaut werden, 
 * dies wird in der Dokumentation sogar ausdrücklich nicht empfohlen, stattdessen
 * sollten solche Wrapper verwendet werden.
 * 
 * AbstractLoggerWrapper bildet viele Methoden, die für einen Nachbau der Logger-Schnittstelle
 * auf drei Basisfunktionen ab.
 *
 */
public abstract class AbstractLoggerWrapper {
  protected static String FQCN = AbstractLoggerWrapper.class.getName();

  protected Logger logger;
  
  public AbstractLoggerWrapper(String name) {
    this.logger = Logger.getLogger(name);
  }
 
  protected abstract void logInternal(Level level, Object message, Throwable t);
  protected abstract void logInternal( String fqcn, Level level, Object message, Throwable t);
  public abstract boolean isEnabledFor( Level level);
  
  
  public void trace(Object message) {
    logInternal( Level.TRACE, message, null );
  }
  public void trace(Object message, Throwable t) {
    logInternal( Level.TRACE, message, t );
  }
  
  public void debug(Object message) {
    logInternal( Level.DEBUG, message, null );
  }
  public void debug(Object message, Throwable t) {
    logInternal( Level.DEBUG, message, t );
  }
 
  public void info(Object message) {
    logInternal( Level.INFO, message, null );
  }
  public void info(Object message, Throwable t) {
    logInternal( Level.INFO, message, t );
  }
 
  public void warn(Object message) {
    logInternal( Level.WARN, message, null );
  }
  public void warn(Object message, Throwable t) {
    logInternal( Level.WARN, message, t );
  }
 
  public void error(Object message) {
    logInternal( Level.ERROR, message, null );
  }
  public void error(Object message, Throwable t) {
    logInternal( Level.ERROR, message, t );
  }
 
  public void fatal(Object message) {
    logInternal( Level.FATAL, message, null );
  }
  public void fatal(Object message, Throwable t) {
    logInternal( Level.FATAL, message, t );
  }
  
  public void log( Level level, Object message) {
    logInternal( level, message, null );
  }
  public void log( Level level, Object message, Throwable t) {
    logInternal( level, message, t );
  }
  
  public boolean isTraceEnabled() {
    return isEnabledFor( Level.TRACE );
  }
  public boolean isDebugEnabled() {
    return isEnabledFor( Level.DEBUG );
  }
  public boolean isInfoEnabled() {
    return isEnabledFor( Level.INFO );
  }
  
  public void log(String fqcn, Level level, Object message, Throwable t) {
    logInternal( fqcn, level, message, t );
  }


}
