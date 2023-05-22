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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SRTestParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;



/**
 *
 */
public class SuspendTestWorkflowSteps {
  
  private static Logger logger = Logger.getLogger(SuspendTestWorkflowSteps.class);
  
  
  public interface ResponseListener {
    void finished();
    boolean isFinished();
    void suspended(ProcessSuspendedException suspendedException);
    boolean isSuspended();
    void onError(Exception e);
  }

  public static abstract class Workflow {
    private Workflow parent;
    private ResponseListener responseListener;
    private Order order;
    private ArrayList<Workflow> childWfs = new ArrayList<Workflow>();
    public abstract WFStep getFirstStep();
    @Override
    public String toString() {
      if( parent == null ) {
        return getClass().getSimpleName();
      } else {
        return parent+"_"+getClass().getSimpleName();
      }
    }
    public void setParent(Workflow parent) {
      this.parent = parent;
    }
    public Workflow getParent() {
      return parent;
    }
    public void setResponseListener(ResponseListener responseListener) {
      this.responseListener = responseListener;
    }
    public ResponseListener getResponseListener() {
      return responseListener;
    }
    public Order getOrder() {
      if( order == null ) {
        order = new Order(this);
      }
      return order;
    }
    public void executeWF() {
      SuspendTestFactory.getInstance().getSuspendResumeManagement().addStartedOrder(order.getId(), order);
      getFirstStep().execute();
    }

    public void addChild(Workflow subWf) {
      subWf.setParent(this);
      childWfs.add(subWf);
    }
    public ArrayList<Workflow> getChildWfs() {
      return childWfs;
    }
    public String getVeto() {
      return null;
    }
    public List<WFStep> getCurrentExecutingSteps(boolean recursive) {
      List<WFStep> list = new ArrayList<WFStep>();
      getFirstStep().addCurrentExecutingSteps(list,recursive);
      return list;
    }
    /**
     * @return
     */
    public Workflow getRootWorkflow() {
      if( parent == null ) {
        return this;
      } else {
        return parent.getRootWorkflow();
      }
    }
  }


  public static abstract class WFStep implements Step {
    private static final long serialVersionUID = 1L;
    public volatile boolean started = false;
    public volatile boolean executed = false;
    Workflow wf;
    private WFStep parent;
    private String laneId;
    public abstract boolean executeInternally();
        
    public void addCurrentExecutingSteps(List<WFStep> list, boolean recursive) {
    }

    private void setParent(WFStep step) {
      parent = step;
    }

    public void setWorkflow(Workflow wf) {
      this.wf = wf;
    }

    public void execute() {
      //logger.debug("executing "+getClass().getSimpleName() +" executed="+executed);
      /*
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }*/
      if( ! executed ) {
        started = true;
        executed = executeInternally();
      }
      //logger.debug(" executed="+executed);
      //return executed;
    }
    public void compensate() {
    }
    
    
    public WFStep getParent() {
      return parent;
    }
    
    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
    
    public String getLaneId() {
      if( parent == null || parent.getLaneId() == null ) {
        return laneId;
      } else {
        if( laneId == null ) {
          return parent.getLaneId();
        } else {
          return laneId+","+parent.getLaneId();
        }
      }
    }

    public void setLaneId(String laneId) {
      this.laneId = laneId;
    }
    
    public Long getOrderId() {
      return wf == null ? null : wf.getOrder().getId();
    }
    
    public Order getOrder() {
      return wf == null ? null : wf.getOrder();
    }
    
    public Step getParentStep() {
      return parent;
    }

    public String getLabel() {
      return getClass().getSimpleName();
    }
  }
  
  
  public static class Serial extends WFStep {
    private static final long serialVersionUID = 1L;
    ArrayList<WFStep> steps = new ArrayList<WFStep>();
    
    @Override
    public boolean executeInternally() {
      boolean stepExecuted = true;
      for( WFStep step : steps ) {
        if( step.executed ) {
          continue;
        } else {
          step.execute();
          stepExecuted = step.executed;
          if( stepExecuted ) {
            continue;
          } else {
            break;
          }
        }
      }
      if( stepExecuted ) {
        return true;
      } else {
        System.err.println("Unexpected stepExecuted = false");
        return false;
      }
    }
    
    public void setWorkflow(Workflow wf) {
      this.wf = wf;
      for( WFStep step : steps ) {
        step.setWorkflow(wf);
      }
    }
   
    public Serial append(WFStep step) {
      steps.add(step);
      step.setParent(this);
      step.setWorkflow(wf);
      return this;
    }
    
    public void addCurrentExecutingSteps(List<WFStep> list, boolean recursive) {
      for( WFStep step : steps ) {
        step.addCurrentExecutingSteps(list, recursive);
      }
    }

  }
  
  public static class Await extends WFStep {
    private static final long serialVersionUID = 1L;
    private String correlationId;

    public Await(String correlationId) {
      this.correlationId = correlationId;
    }

    @Override
    public boolean executeInternally() {
      return SuspendTestFactory.getInstance().getSynchronization().
          await(correlationId, wf.getOrder().getResumeTarget(getLaneId()) );
    }
    
    @Override
    public String toString() {
      return "Await "+correlationId;
    }

    
  }
  
  public static class Increment extends WFStep {
    private static final long serialVersionUID = 1L;
    private AtomicInteger ai;
    private int increment;

    public Increment(AtomicInteger ai) {
      this.ai = ai;
      this.increment = 1;
    }
    
    public Increment(AtomicInteger ai, int increment) {
      this.ai = ai;
      this.increment = increment;
    }
    
    @Override
    public boolean executeInternally() {
      logger.debug("Increment");
      if( increment == 1 ) {
        ai.incrementAndGet();
      } else {
        ai.addAndGet(increment);
      }
      return true;
    }
    
  }
    
  public static class SetBool extends WFStep {
    private static final long serialVersionUID = 1L;
    private AtomicBoolean ab;
    private boolean val;

    public SetBool(AtomicBoolean ab, boolean val) {
      this.ab = ab;
      this.val = val;
    }
    @Override
    public boolean executeInternally() {
      ab.set(val);
      return true;
    }
    
  }
  
  public static class Sleep extends WFStep {
    private static final long serialVersionUID = 1L;
    private long millis;

    public Sleep(long millis) {
      this.millis = millis;
    }
    @Override
    public boolean executeInternally() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        //dann halt k�rzer
      }
      return true;
    }
    
  }
 
  public static class SubworkflowStep extends WFStep {
    private static final long serialVersionUID = 1L;

    private Workflow subWf;

    public SubworkflowStep(Workflow subWf) {
      this.subWf = subWf;
    }

    @Override
    public boolean executeInternally() {
      logger.debug("sub");
      wf.addChild(subWf);
      subWf.getOrder().setParentLaneId( getLaneId() );
      SuspendTestFactory.getInstance().getScheduler().startOrderSynchronous(subWf);
      return subWf.getResponseListener().isFinished();
    }
    
    public void addCurrentExecutingSteps(List<WFStep> list, boolean recursive) {
      list.add(this);
      if( recursive ) {
        list.addAll( subWf.getCurrentExecutingSteps(recursive) );
      }
    }

    public Order getChildOrder() {
      return subWf.getOrder();
    }
    @Override
    public String getLabel() {
      return subWf.getClass().getSimpleName();
    }
    
  }
  
  public static class ParallelStep extends WFStep {
    private static final long serialVersionUID = 1L;
    
    private List<WFStep> steps;
    private SRTestParallelExecutor srtPe;
    private String id;
    
    public ParallelStep(String id, WFStep ... steps) {
      this.id = id;
      this.steps = Arrays.asList(steps);
      for( WFStep step : steps ) {
        step.setParent(this);
        step.setWorkflow(wf);
      }
    }
    
    @Override
    public boolean executeInternally() {
      if( srtPe == null ) {
        srtPe = new SRTestParallelExecutor(id, steps);
        srtPe.init(getOrderId(), getOrder());
      }
      try {
        srtPe.execute();
      } catch (XynaException e) {
        //hier nicht erwartet
        throw new RuntimeException(e);
      }
      return true; //entweder true oder exception
    }
    
    public void setWorkflow(Workflow wf) {
      super.setWorkflow(wf);
      for( WFStep step : steps ) {
        step.setWorkflow(wf);
      }
    }

    /**
     * @return
     */
    public ResumableParallelExecutor getParallelExecutor() {
      return srtPe;
    }

    public ParallelStep getParentParallelStep() {
      WFStep parent = getParent();
      while( parent != null ) {
        if( parent instanceof ParallelStep ) {
          return (ParallelStep) parent;
        }
        parent = parent.getParent();
      }
      return null;
    }

  }
    
  
  public static class LogLaneId extends WFStep {
    private static final long serialVersionUID = 1L;
    public LogLaneId() { 
    }

    @Override
    public boolean executeInternally() {
      StringBuilder sb = new StringBuilder();
      
      WFStep parent = this;
      while( parent != null ) {
        sb.append(parent.toString()).append("<-");
        parent = parent.getParent();
      }
      
     System.err.println( " laneId "+ sb.toString() +" ## "+getLaneId());
     
     
      return true;
    }
    
  }  
  
  public static class Wait extends WFStep implements Runnable {
    private static final long serialVersionUID = 1L;
    private long millis;
    private ResumeTarget resumeTarget;
    private boolean first = true;
    
    public Wait(long millis) {
      this.millis = millis;
    }
    
    public static class SuspensionCauseWait extends SuspensionCause {
      private static final long serialVersionUID = 1L;

      public SuspensionCauseWait(String laneId) {
        this.laneId = laneId;
      }

      @Override
      public String getName() {
        return "WAIT";
      }
    }
    
    @Override
    public boolean executeInternally() {
      if( first ) {
        resumeTarget = wf.getOrder().getResumeTarget(getLaneId());
        first = false;
        new Thread(this).start();
        try {
          Thread.sleep(60);
        } catch (InterruptedException e) {
          //dann halt k�rzer
        }
        throw new ProcessSuspendedException(new SuspensionCauseWait(resumeTarget.getLaneId()));
      }
      return true;
    }

    public void run() {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        //dann halt k�rzer
      }
      try {
        SuspendTestFactory.getInstance().getSuspendResumeManagement().resume(resumeTarget);
      } catch (PersistenceLayerException e) {
        //unerwartet hier im Test
      }
    }
    
  }
  
}
