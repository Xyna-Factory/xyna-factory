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

package com.gip.xyna.xfmg.xfctrl.classloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;

/**
 * verwaltet undeploymenthandler, die automatisch ausgef�hrt werden, wenn der classloader eines
 * deploygegenstands entfernt wird
 */
public class AutomaticUnDeploymentHandlerManager extends FunctionGroup {

  public static final String DEFAULT_NAME = "Deployment Handler Manager";
  private static Logger logger = CentralFactoryLogging.getLogger(AutomaticUnDeploymentHandlerManager.class);

  private final Map<String, ArrayList<UndeploymentHandler>> handlers = new HashMap<String, ArrayList<UndeploymentHandler>>();
  private final ReentrantLock handlersLock = new ReentrantLock();


  public AutomaticUnDeploymentHandlerManager() throws XynaException {
    super();
  }


  @Override
  protected void init() throws XynaException {
  }


  @Override
  protected void shutdown() throws XynaException {
    handlersLock.lock();
    try {
      for (String s : new ArrayList<>(handlers.keySet())) {
        notifyUndeployment(s);
      }
      handlers.clear();
    }
    finally {
      handlersLock.unlock();
    }
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void addUnDeploymentHandler(ClassProvider c, UndeploymentHandler handler) {

    logger.debug("Adding " + UndeploymentHandler.class.getSimpleName() + " for class " + c);

    handlersLock.lock();
    try {
      String mapName = c.getContainedClass().getName() + "#" + c.getContainedClassRevision();
      ArrayList<UndeploymentHandler> oldHandlers = handlers.get(mapName);
      if (oldHandlers == null) {
        oldHandlers = new ArrayList<UndeploymentHandler>();
        oldHandlers.add(handler);
      } else {
        oldHandlers.add(handler);
      }
      handlers.put(mapName, oldHandlers);
    }
    finally {
      handlersLock.unlock();
    }

  }


  public void removeUnDeploymentHandler(UndeploymentHandler handler) {

    logger.debug("Removing " + UndeploymentHandler.class.getSimpleName() + " " + handler);

    handlersLock.lock();
    try {
      Iterator<Entry<String, ArrayList<UndeploymentHandler>>> iter = handlers.entrySet().iterator();
      while (iter.hasNext()) {
        Entry<String, ArrayList<UndeploymentHandler>> e = iter.next();
        if (e.getValue() == null || e.getValue().size() == 0) {
          iter.remove();
          continue;
        }
        ArrayList<UndeploymentHandler> list = e.getValue();
        Iterator<UndeploymentHandler> internalIter = list.iterator();
        while (internalIter.hasNext()) {
          if (internalIter.next() == handler) {
            internalIter.remove();
          }
        }
      }
    }
    finally {
      handlersLock.unlock();
    }

  }

  
  public void notifyUndeployment(String className, Long revision) {
    notifyUndeployment(className + "#" + revision);
  }

  
  private void notifyUndeployment(String key) {

    handlersLock.lock();
    try {
      ArrayList<UndeploymentHandler> toBeNotified = handlers.get(key);
      if (toBeNotified != null) {
        toBeNotified = new ArrayList<>(); //neue liste, weil das undeployment handler-liste modifizieren kann
        for (UndeploymentHandler handler : toBeNotified) {
          handler.onUndeployment();
        }
      }
    }
    finally {
      handlersLock.unlock();
    }

  }


}
