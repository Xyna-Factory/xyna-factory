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
package com.gip.xyna.utils.misc;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.misc.EventHandler.ShutdownReason;


public class EventHandling<E extends Event, H extends EventHandler<E>> {

  private final static Logger logger = CentralFactoryLogging.getLogger(EventHandling.class); 
                  
  protected final Map<String, H> handlers = new HashMap<>();
  
  
  public boolean registerHandler(H handler) {
    synchronized (EventHandling.class) {
      H oldHandler = handlers.get(handler.getName());
      if (oldHandler == null) {
        handlers.put(handler.getName(), handler);
        return true;
      } else {
        if (oldHandler.getVersion().isStrictlyGreaterThan(handler.getVersion())) {
          handler.shutdown(ShutdownReason.NEWER_VERSION_PRESENT);
          return false;
        } else {
          try {
            oldHandler.shutdown(ShutdownReason.DISPLACED_BY_NEWER_VERSION);
          } finally {
            handlers.put(handler.getName(), handler);
          }
          return true;
        }
      }
    }
  }
  
  public boolean unregisterHandler(String name, Version version) {
    synchronized (EventHandling.class) {
      H oldHandler = handlers.get(name);
      if (oldHandler == null) {
        return false;
      } else {
        if (oldHandler.getVersion().equals(version)) {
          handlers.remove(name);
          oldHandler.shutdown(ShutdownReason.MANUAL);
          return true;
        } else {
          return false;
        }
      }
    }
  }
  
  public void triggerEvent(E event) {
    synchronized (EventHandling.class) {
      for (H handler : handlers.values()) {
        try {
          handler.handleEvent(event);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.warn("EventHandler " + handler.getName() + "@" + handler.getVersion().getString() + " failed.",t);
        }
      }
    }
  }
  
  public void shutdown() {
    synchronized (EventHandling.class) {
      for (H handler : handlers.values()) {
        try {
          handler.shutdown(ShutdownReason.FACTORY_SHUTDOWN);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.warn("EventHandler " + handler.getName() + "@" + handler.getVersion().getString() + " failed.",t);
        }
      }
    }
  }
  
}
