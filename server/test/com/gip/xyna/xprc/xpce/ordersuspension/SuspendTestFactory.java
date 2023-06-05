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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Workflow;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Multiple;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;


/**
 *
 */
public class SuspendTestFactory {

  private static Logger logger = Logger.getLogger(SuspendTestFactory.class);
  
  
  private static SuspendTestFactory instance;

  private SuspendTestScheduler scheduler;
  private HashMap<Long, Workflow> orderBackup;
  private IDGenerator idGenerator;
  private Synchronization synchronization;
  private SRTestSuspendResumeManagement suspendResumeManagement;
  private HashMap<Long, Order> allOrders;
  
  public static SuspendTestFactory getInstance() {
    if( instance == null ) {
      instance = new SuspendTestFactory();
    }
    return instance;
  }
  
  public static SuspendTestFactory newInstance() {
    instance = new SuspendTestFactory();
    return instance;
  }
 
  private SuspendTestFactory() {
    idGenerator = new IDGenerator();
    orderBackup = new HashMap<Long, Workflow>();
    allOrders = new HashMap<Long, Order>();
    scheduler = new SuspendTestScheduler();
    synchronization = new Synchronization();
    suspendResumeManagement = new SRTestSuspendResumeManagement();
  }

  public SuspendTestScheduler getScheduler() {
    return scheduler;
  }

  public HashMap<Long, Workflow> getOrderBackupX() {
    return orderBackup;
  }
  
  public HashMap<Long, Order> getAllOrders() {
    return allOrders;
  }
  
  
  public IDGenerator getIDGenerator() {
    return idGenerator;
  }
  
  public Synchronization getSynchronization() {
    return synchronization;
  }

  public SRTestSuspendResumeManagement getSuspendResumeManagement() {
    return suspendResumeManagement;
  }
  
  public static class IDGenerator {
    private AtomicLong id = new AtomicLong(0);
    public Long getId() {
      return id.incrementAndGet();
    }
    public void reset() {
      id.set(0);
    }
  }
  

  public static class Synchronization {
    
    public static class SuspensionCauseAwait extends SuspensionCause {
      private static final long serialVersionUID = 1L;

      public SuspensionCauseAwait(String laneId) {
        this.laneId = laneId;
      }

      @Override
      public String getName() {
        return "AWAITING_SYNCHRONIZATION";
      }
    }
    
    public static class SynchronizationEntry {
      private boolean notified;
      private ResumeTarget resumeTarget;
      public SynchronizationEntry(boolean notified) {
        this.notified = notified;
      }
      public SynchronizationEntry(ResumeTarget resumeTarget) {
        this.resumeTarget = resumeTarget;
        this.notified = false;// orderId.longValue() == -1L ? true : false;
      }
      @Override
      public String toString() {
        if( resumeTarget == null ) {
          return notified?"N":"";
        } else {
          if( resumeTarget.getLaneId() == null ) {
            return resumeTarget.getOrderId()+(notified?"N":"");
          } else {
            return resumeTarget.getOrderId()+","+resumeTarget.getLaneId()+(notified?"N":"");
          }
        }
      }
      public Long getRootId() {
        return resumeTarget.getRootId();
      }
      public ResumeTarget getResumeTarget() {
        return resumeTarget;
      }
    }
    private ConcurrentHashMap<String, SynchronizationEntry> entries = new ConcurrentHashMap<String, SynchronizationEntry>();

    private static final SynchronizationEntry NOTIFICATON = new SynchronizationEntry(true);
    
    public void notify(String correlationId) {
      SynchronizationEntry se = entries.get(correlationId);
      logger.debug("notify "+correlationId +" -> "+se );
      if( se != null ) {
        se.notified = true;
        try {
          //Order order = SuspendTestFactory.getInstance().getAllOrders().get(se.orderId);
          SuspendTestFactory.getInstance().getSuspendResumeManagement().resume(se.getResumeTarget());
        } catch (PersistenceLayerException e) {
          logger.error("Unexpected exception ", e);
        }
      } else {
        entries.put(correlationId, NOTIFICATON);  
      }
    }
    
    /**
     * @param correlationId
     * @return
     */
    public boolean hasNotification(String correlationId) {
      SynchronizationEntry se = entries.get(correlationId);
      return se != null && se.notified;
    }

  
    /**
     * @param correlationId
     * @return
     */
    public boolean await(String correlationId, ResumeTarget resumeTarget ) {
      if( entries.containsKey(correlationId) ) {
        SynchronizationEntry se = entries.get(correlationId);
        if( se.notified ) {
          logger.debug( "Remove SynchronizationEntry "+ correlationId);
          entries.remove(correlationId);
          return true;
        } else {
          //alten, unerfüllten SynchronizationEntry beibehalten
          throw new ProcessSuspendedException(new SuspensionCauseAwait(resumeTarget.getLaneId()));
        }
      } else {
        SynchronizationEntry se = new SynchronizationEntry(resumeTarget);
        entries.put(correlationId, se);
        throw new ProcessSuspendedException(new SuspensionCauseAwait(resumeTarget.getLaneId()));
      }
    }
    
    public void modifyAwaitForTest(String correlationId, ResumeTarget resumeTarget) {
      SynchronizationEntry se = new SynchronizationEntry(resumeTarget);
      entries.put(correlationId, se);
    }
    
    
    @Override
    public String toString() {
      return entries.toString();
    }

    /**
     * @return
     */
    public int size() {
      return entries.size();
    }

    public ConcurrentHashMap<String, SynchronizationEntry> getEntries() {
      return entries;
    }
    
  }
  
  public static class Order {
    Workflow workflow;
    OrderState state;
    Long id;
    private String parentLaneId;
    private SuspensionCause suspensionCause;
    
    public enum OrderState {
      Planning,
      Running,
      Suspended;
    }

    public Order(Workflow wf) {
      this.workflow = wf;
      this.state = OrderState.Planning;
    }

    public Workflow getWorkflow() {
      return workflow;
    }

    public void setState(OrderState state) {
      this.state = state;
    }
    
    @Override
    public String toString() {
      if( state == OrderState.Suspended ) {
        if( suspensionCause instanceof SuspensionCause_Multiple ) {
          SuspensionCause_Multiple scm = (SuspensionCause_Multiple)suspensionCause;
          return state.toString()+"("+scm.getNameCounted()+")_"+workflow.toString();
        } else {
          return state.toString()+"("+suspensionCause+")_"+workflow.toString();
        }
      } else {
        return state.toString()+"_"+workflow.toString();
      }
    }

    public OrderState getState() {
      return state;
    }

    public Long getId() {
      return id;
    }

    public Long getRootId() {
      return getRootId(workflow);
    }
    
    private Long getRootId(Workflow wf) {
      Workflow parent = wf.getParent();
      if( parent != null ) {
        return getRootId(parent);
      } else {
        return wf.getOrder().getId();
      }
    }
    
    public Long getParentId() {
      if( workflow.getParent() != null ) {
        return workflow.getParent().getOrder().getId();
      }
      return null;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public void setParentLaneId(String parentLaneId) {
      this.parentLaneId = parentLaneId;
    }
    
    public String getParentLaneId() {
      return parentLaneId;
    }

    public void setSuspensionCause(SuspensionCause suspensionCause) {
      this.suspensionCause = suspensionCause;
    }

    /**
     * @return
     */
    public List<Order> getOrderAndChildrenRecursively() {
      List<Order> ret = new ArrayList<Order>();
      addOrderAndChildrenRecursivelyInternal(ret);
      return ret;
    }
    
    private void addOrderAndChildrenRecursivelyInternal(List<Order> list) {
      list.add(this);
      List<Workflow> childWFs = getWorkflow().getChildWfs();
      for( Workflow wf : childWFs ) {
        wf.getOrder().addOrderAndChildrenRecursivelyInternal(list);
      }
    }

    public ResumeTarget getResumeTarget(String laneId) {
      return new ResumeTarget(getRootId(), id, laneId);
    }

    public Workflow getRootWorkflow() {
      return workflow.getRootWorkflow();
    }
    
  }

  /**
   * @param string
   * @param i
   * @return
   */
  public static ExecutorService newThreadPool(String name, int nThreads) {
    return Executors.newFixedThreadPool(nThreads, new NamedThreadFactory(name) );
  }
  
  public static class NamedThreadFactory implements ThreadFactory {
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    NamedThreadFactory(String namePrefix) {
      SecurityManager s = System.getSecurityManager();
      group = (s != null)? s.getThreadGroup() :
        Thread.currentThread().getThreadGroup();
      this.namePrefix = namePrefix;
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, 
                            namePrefix + threadNumber.getAndIncrement(),
                            0);
      if (t.isDaemon())
        t.setDaemon(false);
      if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
      return t;
    }
  }

  static ThreadPoolExecutor tpe = null;
  public static ThreadPoolExecutor getThreadPool() {
    if ( tpe == null ) {
      tpe = getThreadPool(5);
    }
    return tpe;
  }

  /**
   * @param i
   */
  private static ThreadPoolExecutor getThreadPool(int numberOfThreads) {
     return new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 100, TimeUnit.SECONDS,
                           new SynchronousQueue<Runnable>(), new XynaThreadFactory(1));
  }

  
}
