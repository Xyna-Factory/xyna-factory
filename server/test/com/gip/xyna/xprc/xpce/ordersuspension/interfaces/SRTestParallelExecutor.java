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
package com.gip.xyna.xprc.xpce.ordersuspension.interfaces;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.parallel.NoParallelismLimitation;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.WFStep;


/**
 *
 */
public class SRTestParallelExecutor extends FractalWorkflowParallelExecutor<WFStep> {
   
  private static final long serialVersionUID = 1L;
  private int totalTasks = 0;
  
  public SRTestParallelExecutor(String parallelExecutorId, List<WFStep> steps) {
    super(parallelExecutorId, steps);
    totalTasks = steps.size();
  }

  /**
   * @return
   */
  protected ParallelExecutor createParallelExecutor() {
    if( parallelExecutor == null ) {
      ThreadPoolExecutor threadPoolExecutor = SuspendTestFactory.getThreadPool();
      this.parallelExecutor = new ParallelExecutor(threadPoolExecutor);
    } else {
      //unerwartet!
      //logger.warn("parallelExecutor already exists!" ); 
    }
    parallelExecutor.setPriorityThreshold(priorityThreshold);
    
    /*
    xynaTaskConsumerPreparator.setMainThread(Thread.currentThread());
    parallelExecutor.setTaskConsumerPreparator(xynaTaskConsumerPreparator);
    */
    parallelismLimitation.setParallelExecutor(parallelExecutor);
    
    return parallelExecutor;
  }

  protected Set<String> getLaneIdsFromSuspendResumeManagement(long orderId) {
    return SuspendTestFactory.getInstance().
    getSuspendResumeManagement().getLaneIdsToResume(orderId, this);
  }
  protected void handleProcessSuspendedException(ProcessSuspendedException suspendedException, long orderId, WFStep step) {
    SuspendTestFactory.getInstance().getSuspendResumeManagement().
    handleSuspensionEventInParallelStep(suspendedException, step);
  }

  public void init(Long orderId, Order order) {
    this.orderId = orderId;
    this.parallelismLimitation = new NoParallelismLimitation<WFStep>();
    SuspendTestFactory.getInstance().getSuspendResumeManagement().addParallelExecutor(orderId, order, this);

  }
  
  @Override
  public String toString() {
    return "ParallelExecutor("+parallelExecutorId+","+totalTasks+"T,"+parallelExecutor.executedTasks()+"F,"
        +parallelExecutor.currentlyExecutingTasks()+"E)";
  }

  @Override
  protected void handleParallelExecutorFinished() {
    SuspendTestFactory.getInstance().getSuspendResumeManagement().handleParallelExecutorFinished(orderId, this);
  }

}
