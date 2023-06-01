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
package com.gip.xyna;

import com.gip.xyna.FutureExecution.TaskId;


public abstract class FutureExecutionTask {

  public final static int DEFAULT_PRIORITY = 10;
  
  private static int[] emptyArray = new int[0];
  
  private TaskId id;
  
  private int priority = DEFAULT_PRIORITY;
  private boolean canceled = false;
  
  public FutureExecutionTask(Object id) {
    this.id = new TaskId(id);
  }
  
  public FutureExecutionTask(Object id, int priority) {
    this.id = new TaskId(id);
    this.priority = priority;
  }
  
  public int[] after() {
    return emptyArray;
  }

  public int[] before() {
    return emptyArray;
  }
  
  public TaskId[] afterTasks() {
    int[] after = after();
    TaskId[] afterTasks = new TaskId[after.length];
    for( int i=0; i<after.length; ++i ) {
      afterTasks[i] = new TaskId(after[i]);
    }
    return afterTasks;
  }
  
  public TaskId[] beforeTasks() {
    int[] before = before();
    TaskId[] beforeTasks = new TaskId[before.length];
    for( int i=0; i<before.length; ++i ) {
      beforeTasks[i] = new TaskId(before[i]);
    }
    return beforeTasks;
  }
  

  public abstract void execute();
  
  public boolean waitForOtherTasksToRegister() {
    return true;
  }

  
  TaskId getId() {
    return id;
  }

  
  public boolean isCanceled() {
    return canceled;
  }
  
  public int getPriority() {
    return priority;
  }

  
  public void canceled() {
    this.canceled = true;
  }

  public boolean isMeta() {
    return false;
  }
  public boolean isDeprecated() {
    return false;
  }
  
}
