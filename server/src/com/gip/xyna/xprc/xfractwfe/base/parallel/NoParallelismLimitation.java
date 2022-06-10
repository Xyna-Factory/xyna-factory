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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor.ParallelismLimitation;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;


/**
 * Keine Thread-Beschränkung gewünscht 
 */
public class NoParallelismLimitation<S extends Step> implements ParallelismLimitation<S> {
  
  private ParallelExecutor parallelExecutor;
  
  public void setParallelExecutor(ParallelExecutor parallelExecutor) {
    this.parallelExecutor = parallelExecutor;
  }

  public void handleProcessSuspendedException(SuspendableParallelTask<S> suspendableParallelTask,
                                              ProcessSuspendedException suspendedException) {}

  public void addTaskToParallelExecutor(SuspendableParallelTask<S> taskToResume) {
    taskToResume.setPriority(SuspendableParallelTask.PRIORITY_RESUME);
    parallelExecutor.addTask(taskToResume);
  }

  public boolean awaitLimitationNotZero() {
    return true; //sollte nie gerufen werden können
  }

}
