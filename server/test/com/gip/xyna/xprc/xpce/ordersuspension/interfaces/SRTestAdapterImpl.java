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


import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.misc.IndentableStringBuilder;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension;
import com.gip.xyna.xprc.xpce.ordersuspension.RootSRInformation;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformation;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.DoResume;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order.OrderState;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestScheduler.Connection;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.ParallelStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.SubworkflowStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.WFStep;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Workflow;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 *
 */
public class SRTestAdapterImpl implements SuspendResumeAdapter<Connection,Order> {
      
  @SuppressWarnings("unused")
  private SuspendResumeAlgorithm<Connection,Order> suspendResumeAlgorithm;

  public void setSuspendResumeAlgorithm(SuspendResumeAlgorithm<Connection,Order> suspendResumeAlgorithm) {
    this.suspendResumeAlgorithm = suspendResumeAlgorithm;
  }

  public void startOrder(RootSRInformation<Order> rootSRInformation, Connection con) throws PersistenceLayerException {
    SuspendTestFactory.getInstance().getScheduler().startOrderAsynchronous(getRootOrder(rootSRInformation).getWorkflow());     
  }

  private Order getRootOrder(RootSRInformation<Order> rootSRInformation) {
    return rootSRInformation.getOrder();
  }

  public void rescheduleOrder(Order order) {
    SuspendTestFactory.getInstance().getScheduler().add(order);
  }

  public void suspendOrder(Order order, RootOrderSuspension rootOrderSuspension, SuspensionCause suspensionCause, boolean backup) {
    order.setState(OrderState.Suspended);
    order.setSuspensionCause(suspensionCause);
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#addAllParallelExecutors(com.gip.xyna.xprc.xpce.ordersuspension.SRInformation, java.lang.Object)
   */
  public void addAllParallelExecutors(SRInformation srInformation, Step step) {
    if( !( step.getParentStep() instanceof Step) ) {
      throw new IllegalStateException("Parent of step should be a ParallelStep but "+step.getParentStep().getClass().getName() );
    }
    
    ParallelStep parent = (ParallelStep) step.getParentStep();
    boolean added = true;
    while( added && parent != null ) {
      added = srInformation.addParallelExecutor( parent.getParallelExecutor() );
      parent = parent.getParentParallelStep();
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#readOrder(java.lang.Long, java.lang.Object)
   */
  public Order readOrder(Long orderId, Connection con) throws PersistenceLayerException {
    Order order = SuspendTestFactory.getInstance().getAllOrders().get(orderId);
    if( order == null ) {
      throw new RuntimeException("Auftrag "+orderId+" nicht gefunden");
    }
    return order;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#getOrderId(java.lang.Object)
   */
  public Long getOrderId(Order order) {
    return order.getId();
  }

  public Long getRootOrderId(Order order) {
    return order.getRootId();
  }

  public Order getRootOrder(Order order) {
    return order.getRootWorkflow().getOrder();
  }
  
  @SuppressWarnings("unchecked")
  public void fillOrderData(SRInformation srInformation, Order order) {
    if( order == null ) {
      return;
    }
    srInformation.setRootId(order.getRootId());
    if( order.getParentId() != null ) {
      srInformation.setParentLaneId(order.getParentLaneId());
      srInformation.setParentId(order.getParentId());
    } else {
      if( srInformation instanceof RootSRInformation ) {
        ((RootSRInformation<Order>)srInformation).setOrder(order);
      }
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#getRootId(java.lang.Long, java.lang.Object)
   */
  public Long getRootId(Long orderId) throws PersistenceLayerException {
    return readOrder(orderId, null).getRootId();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#extractOrder(java.lang.Long, java.lang.Object)
   */
  public Order extractOrder(Long orderId, RootSRInformation<Order> rootSRInformation) {
    long id = orderId.longValue();
    List<Order> allOrders = getRootOrder(rootSRInformation).getOrderAndChildrenRecursively();
    for ( Order o : allOrders ) {
      if (o.getId() == id ) {
        return o;
      }
    }
    throw new RuntimeException("rootOrder has no child with id "+orderId);
  }

  public void cleanupOrderFamily(RootSRInformation<Order> rootSRInformation, Long orderId, Connection con) throws PersistenceLayerException {
    //OrderAbortion wird derzeit nicht getestet
  }

  public SuspensionResult suspend(Long orderId, Order order, SuspensionCause suspensionCause, boolean executing, IndentableStringBuilder suspendInfo) {
    if( executing ) {
      Workflow wf = order.getWorkflow();
      if( wf == null ) {
        suspendInfo.append("wf is null for order ").append(order).linebreak();
        return SuspensionResult.Failed;
      }
      suspendInfo.append("suspending ").append(wf).linebreak();

      //CountDownLatch suspensionLatch = process.suspend(suspensionCause);
      
      List<WFStep> steps = wf.getCurrentExecutingSteps(false);
      int i=0;
      suspendInfo.increment();
      for( WFStep step : steps ) {
        ++i;
        if( step instanceof SubworkflowStep ) {
          SubworkflowStep subworkflow = (SubworkflowStep) step;
          Long childId = subworkflow.getChildOrder().getId();
          suspendInfo.append(i).append(". subworkflow \"").append(step.getLabel()).append("\" with id ").append(childId).linebreak();
          suspendInfo.increment(3);
          
          //XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
          //suspendOrderNew(childId, suspensionCause, suspendInfo);
          suspendInfo.decrement(3);
        } else {
          suspendInfo.append(i).append( ". unexpected active step \"").append(step.getLabel()).append("\": ").append(step).linebreak();
        }
      }
       
      /*
      
      CountDownLatch suspensionLatch = process.suspend(suspensionCause);
      logger.info(" getCurrentExecutingSteps called ");
      List<FractalProcessStep<?>> steps = process.getCurrentExecutingSteps(false);
      logger.info(" getCurrentExecutingSteps finished ");
      int i=0;
      suspendInfo.increment();
      */
    } else {
      //Scheduler...
    }
    return SuspensionResult.Failed;
  }
/*
  public Pair<SuspensionResult, CountDownLatch> suspendInExecution(Order order, SuspensionCause suspensionCause,
                                                                   IndentableStringBuilder suspendInfo) {
    Workflow wf = order.getWorkflow();
    if( wf == null ) {
      suspendInfo.append("wf is null for order ").append(order).linebreak();
      return Pair.of(SuspensionResult.Failed,null);
    }
    suspendInfo.append("suspending ").append(wf).linebreak();

    CountDownLatch suspensionLatch = new CountDownLatch(1);
        //wf.suspend(suspensionCause); FIXME 

    return Pair.of(SuspensionResult.Suspended, suspensionLatch);
  }

  public Map<Long, String> getCurrentExecutingChildren(Order order, IndentableStringBuilder suspendInfo) {
    Workflow wf = order.getWorkflow();
    if( wf == null ) {
      suspendInfo.append("workflow is null for order ").append(order).linebreak();
      return Collections.emptyMap();
    }
    Map<Long,String> childOrders = new HashMap<Long,String>();
    List<WFStep> steps = wf.getCurrentExecutingSteps(false);
    for( WFStep step : steps ) {
      String label = step.getLabel();
      if( step instanceof SubworkflowStep ) {
        SubworkflowStep subworkflow = (SubworkflowStep) step;
        childOrders.put( subworkflow.getChildOrder().getId(), label );
      } else {
        suspendInfo.append( "unexpected active step \"").append(label).append("\": ").append(step).linebreak();
      }
    }
    return childOrders;
  }

  public SuspensionResult suspendInScheduler(Long orderId, SuspensionCause suspensionCause,
                                             IndentableStringBuilder suspendInfo) {
    // TODO Auto-generated method stub
    return SuspensionResult.Failed;
  }
*/

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#undoSuspensions(java.util.List)
   */
  public List<Triple<RootOrderSuspension, String, PersistenceLayerException>> resumeRootOrdersWithRetries(List<RootOrderSuspension> rootOrderSuspensions) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#interruptProcess(com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension)
   */
  public void interruptProcess(RootSRInformation<Order> rootSRInformation, RootOrderSuspension rootOrderSuspension) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#suspendInExecution(java.lang.Object, com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension)
   */
  public boolean suspendInExecution(RootSRInformation<Order> rootSRInformation, RootOrderSuspension rootOrderSuspension) {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#suspendInScheduler(java.lang.Object, com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension)
   */
  public boolean suspendInScheduler(RootSRInformation<Order> rootSRInformation, RootOrderSuspension rootOrderSuspension) {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter#interruptProcess(java.lang.Object, com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension, boolean)
   */
  public void interruptProcess(RootSRInformation<Order> rootSRInformation, RootOrderSuspension rootOrderSuspension, boolean stopForcefully) {
    // TODO Auto-generated method stub
    
  }

  public int waitUntilRootOrderIsAccessible(int retry, Long rootId) {
    // TODO Auto-generated method stub
    return 0;
  }

  public void writeOrders(Connection con, Collection<Order> orders) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void abortOrderSuspension(RootSRInformation<Order> rootSRInformation, long orderId, Throwable cause) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Pair<ResumeResult, String> abortSuspendedOrder(DoResume<Connection> doResume, RootSRInformation<Order> rootSRInformation,
                                                        long orderId, boolean ignoreResourcesWhenResuming)
      throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return null;
  } 
  
  
}
