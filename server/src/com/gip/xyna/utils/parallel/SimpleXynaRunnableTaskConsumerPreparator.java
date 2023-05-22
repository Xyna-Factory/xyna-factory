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
package com.gip.xyna.utils.parallel;

import com.gip.xyna.utils.parallel.ParallelExecutor.TaskConsumer;
import com.gip.xyna.utils.parallel.ParallelExecutor.TaskConsumerPreparator;
import com.gip.xyna.xprc.XynaRunnable;

/**
 * SimpleXynaRunnableTaskConsumerPreparator modifiziert den ParallelExecutor und liefert mit
 * {@link #prepareExecutorRunnable(TaskConsumer)} ein Runnable,
 * welches im XynaExecutor lauff�hig ist (XynaRunnable).
 */
public class SimpleXynaRunnableTaskConsumerPreparator implements TaskConsumerPreparator {

    private ParallelExecutor parallelExecutor;
    private final boolean mayCallThreadPoolRejectionHandler;

    public SimpleXynaRunnableTaskConsumerPreparator(boolean mayCallThreadPoolRejectionHandler) {
      this.mayCallThreadPoolRejectionHandler = mayCallThreadPoolRejectionHandler;
    }

    public TaskConsumer newTaskConsumer() {
      return new TaskConsumer(parallelExecutor);
    }

    public Runnable prepareExecutorRunnable(TaskConsumer tc) {
      return new XynaExecutorSlaveRunnable(tc, mayCallThreadPoolRejectionHandler);
    }

    public void setParallelExecutor(ParallelExecutor parallelExecutor) {
      this.parallelExecutor = parallelExecutor;
    }
    
    
    public static class XynaExecutorSlaveRunnable extends XynaRunnable {

      private final TaskConsumer tc;
      private final boolean mayCallThreadPoolRejectionHandler;

      public XynaExecutorSlaveRunnable(TaskConsumer tc, boolean mayCallThreadPoolRejectionHandler) {
        this.tc = tc;
        this.mayCallThreadPoolRejectionHandler = mayCallThreadPoolRejectionHandler;
      }

      public void run() {
        tc.run();
      }

      @Override
      public boolean mayCallRejectionHandlerOnRejection() {
        tc.finish();
        return mayCallThreadPoolRejectionHandler;
      }
    }
}
