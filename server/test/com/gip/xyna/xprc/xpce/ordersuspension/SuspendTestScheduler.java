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

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestFactory.Order.OrderState;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.ResponseListener;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendTestWorkflowSteps.Workflow;


/**
 */
public class SuspendTestScheduler {
  private static Logger logger = Logger.getLogger(SuspendTestScheduler.class);

  static ExecutorService executor = SuspendTestFactory.newThreadPool("schedulerThread-",5);
  private HashSet<String> vetos = new HashSet<String>();
  private Object vetoMutex = new Object();
  
  public SuspendTestScheduler() {
  }
  
  public void addVeto(String veto) {
    vetos.add(veto);
  }

  public void removeVeto(String veto) {
    vetos.remove(veto);
    synchronized (vetoMutex) {
      vetoMutex.notifyAll();
    }
  }

  /**
   * @param wf
   */
  public void startOrderAsynchronous(Workflow wf) {
    Order order = getOrInsertOrder(wf);
    if( wf.getResponseListener() == null ) {
      wf.setResponseListener( new ResponseListenerAsynchronous() );
    }
    add(order);
  }

  public void startOrderSynchronous(Workflow wf) {
    Order order = getOrInsertOrder(wf);
    ResponseListenerWithLatchForSuspend rl = getOrCreateResponseListener(wf);
    add(order);
    if( rl != null ) {
      logger.debug("waiting for workflow "+wf.toString()+ " to finish");
      rl.await();
    }
    if( rl.isSuspended() ) {
      logger.debug("workflow "+wf.toString()+ " is suspended");
      if( wf.getParent() != null) {
        rl.handleSuspension();
      }
    }
    
    if( rl.isFinished() ) {
      logger.debug("workflow "+wf.toString()+ " has finished");
    }
  }
  
  private Order getOrInsertOrder(Workflow wf) {
    Order order = wf.getOrder();
    if( order.getId() == null ) {
      order.setId( SuspendTestFactory.getInstance().getIDGenerator().getId() );
      logger.debug( "setting id "+ order.getId() +" to workflow "+wf.toString() );
      SuspendTestFactory.getInstance().getAllOrders().put( order.getId(), order );
    }
    return order;
    /*
    if( wf.getId() == null ) {
      wf.setId( SuspendTestFactory.getInstance().getIDGenerator().getId() );
      logger.debug( "setting id "+ wf.getId() +" to workflow "+wf.toString() );
      SuspendTestFactory.getInstance().getAllOrders().put( wf.getId(), wf.getOrder() );
    }
    return SuspendTestFactory.getInstance().getAllOrders().get(wf.getId());
    */
  }


  /**
   * Einstellen in den Scheduler, wird hier sofort geschedult...
   * @param wf
   */
  public void add(final Order order) {
    //logger.warn("add Order called from ", new Exception() );
    executor.execute( new Runnable(){
      public void run() {
        /*
        try {
          con.afterCommit();
        } catch (InterruptedException e) {
          logger.error("addOrder waiting for afterCommit failed", e);
          //dann halt sofort
        }*/
        String veto = order.getWorkflow().getVeto();
         if( veto != null ) {
          waitForVeto(veto);
        }
        
        order.setState(OrderState.Running);
        executeOrder(order);
      }});
      
  }

  protected void waitForVeto(String veto) {
    System.err.println( "veto "+veto);
    while( vetos.contains(veto) ) {
      synchronized (vetoMutex) {
        try {
          vetoMutex.wait();
        } catch (InterruptedException e) {
          //ignorieren, while sorgt f�r n�chstes Warten
        }
      }
    }
  }

  public static class Connection {

    private CountDownLatch cdl = new CountDownLatch(1);

    public void afterCommit() throws InterruptedException {
      cdl.await();
    }

    public void commit() {
      cdl.countDown();
    }
    
  }
  

  /**
   * Scheduler beendet die Ausf�hrung und r�umt im Cleanup auf
   * @param id
   */
  private void finish(Long id) {
    logger.debug("finish "+id);
    SuspendTestFactory.getInstance().getAllOrders().remove(id);
    SuspendTestFactory.getInstance().getSuspendResumeManagement().removeSuspensionData(id);
  }
  
  /**
   * @param wf
   * @return
   */
  private ResponseListenerWithLatchForSuspend getOrCreateResponseListener(Workflow wf) {
    ResponseListenerWithLatchForSuspend rl = null;
    if( wf.getResponseListener() == null ) {
      rl = new ResponseListenerWithLatchForSuspend();
      wf.setResponseListener(rl);
    } else if( wf.getResponseListener() instanceof ResponseListenerWithLatchForSuspend ) {
      rl = (ResponseListenerWithLatchForSuspend) wf.getResponseListener();
      rl.reset();
    } else {
      rl = new ResponseListenerWithLatchForSuspend(); //FIXME alten ResponseListener nicht vernichten!
      wf.setResponseListener(rl);
    }
    return rl;
  }

  /**
   * @param wf
   */
  private void executeOrder(Order order) {
    boolean suspended = false;
    boolean finished = false;
    Connection con = new Connection();
    Workflow wf = order.getWorkflow();
    try {
      logger.debug("executing workflow "+wf.toString());
      wf.executeWF();
      finish(order.getId());
      wf.getResponseListener().finished();
      finished = true;
    } catch( ProcessSuspendedException suspendedException ) {
      try {
        suspended = SuspendTestFactory.getInstance().getSuspendResumeManagement().handleSuspensionEvent(suspendedException, order);
        if( suspended ) {
          wf.getResponseListener().suspended(suspendedException);
        }
      } catch( PersistenceLayerException ple ) {
        logger.error( "Suspend order failed ",ple);
        finished = true;
        finish(order.getId());
        wf.getResponseListener().onError(ple);
      }
    } catch( Exception e ) {
      logger.error( "Executing order failed ",e);
      finished = true;
      finish(order.getId());
      wf.getResponseListener().onError(e);
    } finally {
      String mode = finished ? "finished" : (suspended ? "suspended" : "readded");
      logger.debug(mode + " workflow "+order.getId() + " " + wf.toString());
      con.commit();
    }
  }




  private static class ResponseListenerWithLatchForSuspend implements ResponseListener {
    private volatile CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean suspended = false;
    private volatile boolean finished = false;
    private volatile ProcessSuspendedException suspendedException;

    public void await() {
      try {        
        if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
          if (!latch.await(100, TimeUnit.MILLISECONDS)) { //nochmal probieren, evtl an falschem latch gewartet??
            throw new RuntimeException("responselistener did not get notified");
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    /**
     * 
     */
    public void handleSuspension() {
      if( suspendedException != null ) {
        throw suspendedException;
      }
    }
    
    public boolean isFinished() {
      return finished;
    }
    public boolean isSuspended() {
      return suspendedException != null;
    }
    public void finished() {
      finished = true;
      latch.countDown();
    }

    public void suspended(ProcessSuspendedException suspendedException) {
      this.suspendedException = suspendedException;
      latch.countDown();
    }
    
    public void reset() {
      suspendedException = null;
      suspended = false;
      latch = new CountDownLatch(1);
    }

    public void onError(Exception e) {
      finished = true;
      latch.countDown();
    }
    
  }
  
  private static class ResponseListenerAsynchronous implements ResponseListener {
    private volatile boolean finished = false;
    private volatile boolean suspended = false;

    public boolean isFinished() {
      return finished;
    }
    public boolean isSuspended() {
      return suspended;
    }

    public void finished() {
      finished = true;
    }
    public void suspended(ProcessSuspendedException suspendedException) {
      suspended = true;
    }

    public void onError(Exception e) {
      finished = true;
    }
  }

  
  
}
