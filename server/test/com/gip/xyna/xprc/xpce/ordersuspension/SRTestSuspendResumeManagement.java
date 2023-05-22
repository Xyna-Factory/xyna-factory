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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestScheduler.Connection;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.WFStep;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SRTestAdapterImpl;


/**
 *
 */
public class SRTestSuspendResumeManagement {
  
  private SuspendResumeAlgorithm<Connection,Order> suspendResumeAlgorithm;
  private SRTestAdapterImpl srTestAdapterImpl; 

  
  public SRTestSuspendResumeManagement() {
    srTestAdapterImpl = new SRTestAdapterImpl();
    suspendResumeAlgorithm = new SuspendResumeAlgorithm<Connection,Order>(srTestAdapterImpl);
  }
  
  @Override
  public String toString() {
    return suspendResumeAlgorithm.toString();
  }

  public void removeSuspensionData(Long id) {
    suspendResumeAlgorithm.cleanupSuspensionData(id);
  }


  public void resume(ResumeTarget target) throws PersistenceLayerException {
    suspendResumeAlgorithm.resume(target);
  }

  public boolean handleSuspensionEvent(ProcessSuspendedException suspendedException, Order order) throws PersistenceLayerException {
    Pair<Boolean, List<ResumeTarget>> pair = suspendResumeAlgorithm.handleSuspensionEvent(suspendedException, order.getId(), order, true);
    return pair.getFirst();
  }

  public void handleSuspensionEventInParallelStep(ProcessSuspendedException suspendedException, WFStep step) {
    suspendResumeAlgorithm.handleSuspensionEventInParallelStep(suspendedException, step.getOrderId(), step);
  }

  public Set<String> getLaneIdsToResume(Long orderId, ResumableParallelExecutor parallelExecutor) {
    return suspendResumeAlgorithm.getLaneIdsToResume(orderId,parallelExecutor);
  }

  public void addStartedOrder(Long orderId, Order order) {
    suspendResumeAlgorithm.addStartedOrder(orderId, order);
  }

  public void addParallelExecutor(Long orderId, Order order, ResumableParallelExecutor parallelExecutor) {
    suspendResumeAlgorithm.addParallelExecutor(orderId, order, parallelExecutor);
  }

  public SuspendRootOrderData suspendRootOrders( SuspendRootOrderData suspendRootOrderData) {
    return suspendResumeAlgorithm.suspendRootOrders(suspendRootOrderData);
  }

  public void removeAllOrderBackupLocks() {
    
    Set<Long> orders = suspendResumeAlgorithm.getSRInformationCache().getUnresumeableOrders();
    if( !orders.isEmpty() ) {
      System.err.println( "UnresumeableOrders = "+orders );
      suspendResumeAlgorithm.getSRInformationCache().removeUnresumableOrders( orders);
    }
  }

  public void handleParallelExecutorFinished(Long orderId, ResumableParallelExecutor parallelExecutor) {
    suspendResumeAlgorithm.handleParallelExecutorFinished(orderId, parallelExecutor.getParallelExecutorId());    
  }

}
