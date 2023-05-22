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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import org.apache.log4j.NDC;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.ParallelExecutor.BeforeAndAfterExecution;
import com.gip.xyna.utils.parallel.ParallelExecutor.TaskConsumer;
import com.gip.xyna.utils.parallel.ParallelExecutor.TaskConsumerPreparator;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;


/**
 * XynaTaskConsumerPreparator modifiziert den ParallelExecutor folgenderma�en:
 * 
 * 1) {@link #newTaskConsumer()} baut TaskConsumer, der vor und nach der Ausf�hrung 
 * des SupendableParallelTasks diesen im XynaProcess an und abmeldet<br>
 * 2) {@link #prepareExecutorRunnable(TaskConsumer)} liefert ein Runnable, welches im 
 * XynaExecutor lauff�hig ist (XynaRunnable). Dieses XynaExecutorRunnable meldet den Thread
 * im XynaProcess an und ab und setzt das NDC-Logging
 */
public class XynaTaskConsumerPreparator implements TaskConsumerPreparator {

  private boolean createLoggingContext = false;
  private String ndcString;
  private boolean createOrderContextMapping = false;
  private OrderContextServerExtension orderContext;
  private XynaProcess process;
  private Thread mainThread;
  private ParallelExecutor parallelExecutor;
  
  
  public TaskConsumer newTaskConsumer() {
    return new TaskConsumer(parallelExecutor);
  }
  
  public Runnable prepareExecutorRunnable(TaskConsumer tc) {
    return new XynaExecutorRunnable(tc);
  }
  
  public void createOrderContextMapping( OrderContextServerExtension orderContext ) {
    if( orderContext != null ) {
      this.createOrderContextMapping = true;
      this.orderContext = orderContext;
    } else {
      this.createOrderContextMapping = false;
      this.orderContext = null;
    }
  }
  
  public void createLoggingContext(String ndcString) {
    if( ndcString != null ) {
      this.createLoggingContext = true;
      this.ndcString = ndcString;
    } else {
      this.createLoggingContext = false;
      this.ndcString = null;
    }
  }
  
  public void setProcess(XynaProcess process) {
    this.process = process;
  }
  public void setParallelExecutor(ParallelExecutor parallelExecutor) {
    this.parallelExecutor = parallelExecutor;
  }
  public void setMainThread(Thread mainThread) {
    this.mainThread = mainThread;
  }
  
  private class XynaExecutorRunnable extends XynaRunnable implements BeforeAndAfterExecution {
    private TaskConsumer tc;    
    private Thread currentThread;
    private String ndcStringBefore;
    
    public XynaExecutorRunnable(TaskConsumer tc) {
      this.tc = tc;
    }    
    
    public void run() {
      currentThread = Thread.currentThread();
      tc.setBeforeAndAfterExecution(this);
      try {
        beforeExecutionOfTaskConsumer();
        tc.run();
      } finally {
        afterExecutionOfTaskConsumer();
      }
    }
    
    /**
     * Wird vor jeder Task-Ausf�hrung gerufen
     */
    public void beforeExecution(ParallelTask task) {
      process.addActiveParallelTaskThread(currentThread, task);
    }

    /**
     * Wird nach jeder Task-Ausf�hrung gerufen
     */
    public void afterExecution(ParallelTask task) {
      process.removeActiveParallelTaskThread(task);
    }

    /**
     * Wird vor Ausf�hrung des TaskConsumer aufgerufen:
     * NDC-Logging und Registrierung des Threads in XynaProcess
     */
    private void beforeExecutionOfTaskConsumer() {
      if( createLoggingContext ) {
        if( NDC.getDepth() != 0 ) {
          ndcStringBefore = NDC.pop();
        }
        NDC.push(ndcString);
      }
      if( currentThread != mainThread ) {
        if (createOrderContextMapping) {
         XynaFactory.getInstance().getProcessing().getWorkflowEngine().setOrderContext(orderContext);
        }
        process.addActiveThread(currentThread);
      }
    }

    /**
     * Wird nach Ausf�hrung des TaskConsumer aufgerufen:
     * NDC-Logging und Deregistrierung des Threads in XynaProcess
     * @param currentThread 
     * @param suspendableParallelTask 
     * 
     */
    public void afterExecutionOfTaskConsumer() {
      try {
        if( currentThread != mainThread ) {
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().removeOrderContext();
          process.removeActiveThread(currentThread);
        }
      } finally {
        if (createLoggingContext) {
          NDC.pop();
          if( ndcStringBefore != null ) {
            NDC.push(ndcStringBefore);
          }
        }
      }
    }
    
  }

  /**
   * @return
   */
  public Thread getMainThread() {
    return mainThread;
  }



  
}
