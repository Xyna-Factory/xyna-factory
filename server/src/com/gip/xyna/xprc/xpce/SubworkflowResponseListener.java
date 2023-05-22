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
package com.gip.xyna.xprc.xpce;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.SynchronousResponseListenerForXpce;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;


/**
 *
 */
public class SubworkflowResponseListener extends ResponseListener implements SynchronousResponseListenerForXpce, ResponseListenerWithOrderDeathSupport {

  private static final long serialVersionUID = 1L;
  public static final Logger logger = CentralFactoryLogging.getLogger(SubworkflowResponseListener.class);
  
  private transient CountDownLatch latch;
  private transient GeneralXynaObject response;
  private transient boolean gotAborted;
  private transient boolean needToAbortParentOrder;
  private final XynaOrderServerExtension xo;
  private transient ProcessAbortedException abortionException;
  private transient ProcessSuspendedException suspendedException;
  private transient OrderDeathException orderDeathException = null;
  private transient XynaException[] xynaExceptions;
  private boolean hasNullResponse;

  public SubworkflowResponseListener(CountDownLatch latch, XynaOrderServerExtension xo) {
    this.latch = latch;
    this.gotAborted = false;
    this.xo = xo;
    this.needToAbortParentOrder = false;
  }

  public void onResponse(GeneralXynaObject response, OrderContext ctx) {
    this.response = response;
    if( response == null ) {
      hasNullResponse = true;
    }
    latch.countDown();
  }

  public void onSuspended(ProcessSuspendedException suspendedException) {
    this.suspendedException = suspendedException;
    latch.countDown();
  }
  
  public void onError(XynaException[] e, OrderContext ctx) {
    xynaExceptions = e;
    latch.countDown();
  }

  public void onOrderDeath(OrderDeathException e) {
    this.orderDeathException = e;
    this.latch.countDown();
  }

  public void onOrderAbortion(ProcessAbortedException e) {
    if (xo == null) {
      //deserialisiert und aborted, bevor wieder der thread gestartet wurde
      //TODO in welchen f�llen kann das genau passieren?
      logger.warn("Order has been aborted. ParentOrder can not be notified.", e);
      return;
    }
    boolean needToAbortParentOrder = e.needsToAbortParentOrderEvenIfNotAborted();
    abortionException = e;

    if (xo.hasParentOrder()) {
      gotAborted = true;
      this.needToAbortParentOrder = needToAbortParentOrder;
      latch.countDown();
    } else {
      onError(new XynaException[] {new XPRC_PROCESS_ABORTED_EXCEPTION(xo.getId(), e.getAbortionCause()
                                                                      .getAbortionCauseString(), (Throwable) e)}, xo.getOrderContext());
    }
  }
 

  /**
   * 
   */
  public void handleInterruptionWhileWaitingForLatch() {
    if (xo.isAttemptingSuspension()) {
      if( suspendedException != null ) {
        throw suspendedException;
      }
      throw SuspendResumeManagement.suspendManualOrShutDown(xo.getId(), null);
    }
  }

  public void handleAfterAwait() {
    if (orderDeathException != null) {
      throw orderDeathException;
    }
    
    if( suspendedException != null ) {
      throw suspendedException;
    }
    
    if (gotAborted && needToAbortParentOrder) {
      throw abortionException;
    }
  }

  public GeneralXynaObject getResponse() {
    return response;
  }

  public XynaException[] getXynaExceptions() {
    return xynaExceptions;
  }
  

  /*
   * This ResponseListener (although serializable) shouldn't be persisted. empty read- and write-Methods ensure a
   * nulled Response-Listener for orders using this class This approach is kind of dirty, a deep clone of the order
   * (all childs and selfReferences) that get's these Listeners removed would be preferable
   */
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    // s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    // s.defaultReadObject();
    this.latch = new CountDownLatch(1);
  }

  public boolean hasNullResponse() {
    return hasNullResponse;
  }


 
}
