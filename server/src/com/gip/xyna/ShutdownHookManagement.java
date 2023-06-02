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

package com.gip.xyna;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * Klasse zur Verwaltung von ShutdownHooks. Es können mehrere Tasks, die eigentlich als
 * Shutdown-Hooks ausgeführt werden sollen, registriert werden. Diese werden dann
 * von einem "echten" Shutdown-Hook ausgeführt. Dabei kann durch den ShutdownHookType
 * die Abarbeitungsreihenfolge bestimmt werden.
 *
 */
public final class ShutdownHookManagement {
  
  private static Logger logger = CentralFactoryLogging.getLogger(ShutdownHookManagement.class);
  
  //Achtung: Reihenfolge legt Abarbeitungsreihenfolge fest
  public static enum ShutdownHookType {
    DEFAULT, //beliebige Shutdown-Hooks, werden zuerst ausgeführt
    DELETE_PID, //Löschen des pid files
    CLOSE_SOCKET; //Schließen des CLI Sockets, muss als letztes ausgeführt werden
  }

  private static final ShutdownHookManagement INSTANCE = new ShutdownHookManagement();

  //Tasks die ausgeführt werden sollen
  private static EnumMap<ShutdownHookType, List<Runnable>> tasks = new EnumMap<ShutdownHookType, List<Runnable>>(ShutdownHookType.class);

  
  private ShutdownHookManagement () {
    //Resgistrierung des "echten" Shutdown-Hooks, der dann die Tasks ausführt
    Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook") {
      public void run() {
        if (logger.isDebugEnabled()) {
          logger.debug("execute shutdown hooks");
        }
        for (ShutdownHookType type : tasks.keySet()) {
          for (Runnable task : tasks.get(type)) {
            try {
              task.run();
            } catch (Throwable t) {
              //andere Shutdown-Hooks sollen trotzdem ausgeführt werden
              logger.warn("Could not execute shutdown hook", t);
            }
          }
        }
      }
    });}
  
  public static ShutdownHookManagement getInstance() {
    return INSTANCE;
  }

  /**
   * Registriert einen neuen Task, für den egal ist, wann er ausgeführt wird
   * @param task
   */
  public void addTask(Runnable task) {
    addTask(task, ShutdownHookType.DEFAULT);
  }

  /**
   * Registriert einen neuen Task. Der ShutdownHookType legt fest, wann der Task
   * ausgeführt wird.
   * @param task
   * @param type
   */
  public void addTask(Runnable task, ShutdownHookType type) {
    List<Runnable> list = tasks.get(type);
    if (list == null) {
      list = new ArrayList<Runnable>();
      tasks.put(type, list);
    }
    list.add(task);
  }
}
