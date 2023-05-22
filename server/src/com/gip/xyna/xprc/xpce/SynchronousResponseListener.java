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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.SynchronousResponseListenerForXpce;


/**
 *
 */
public class SynchronousResponseListener extends ResponseListener implements SynchronousResponseListenerForXpce, ResponseListenerWithOrderDeathSupport {

  private static final long serialVersionUID = 1L;
  private transient CountDownLatch latch;
  private ResponseListener baseRl;
  private transient GeneralXynaObject response;
  private transient XynaException[] xynaExceptions;
  private transient OrderDeathException orderDeathException;
  private boolean hasNullResponse;


  public SynchronousResponseListener(CountDownLatch latch, XynaOrderServerExtension xo) {
    this.latch = latch;
    ResponseListener rl;
    if (xo == null) {
      rl = null;
    } else {
      rl = xo.getResponseListener();
      while (rl instanceof SynchronousResponseListener) {
        rl = ((SynchronousResponseListener) rl).baseRl;
      }
    }
    this.baseRl = rl;
  }


  @Override
  public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
    try {
      this.response = response;
      if( response == null ) {
        hasNullResponse = true;
      }
      if( baseRl != null ) {
        baseRl.onResponse(response, ctx);
      }
    } finally {
      latch.countDown();
    }
  }

  @Override
  public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
    try {
      xynaExceptions = e; //TODO durch Kopie sch�tzen?
      if( baseRl != null ) {
        baseRl.onError(e, ctx);
      }
    } finally {
      latch.countDown();
    }
  }
  
  public void onOrderDeath(OrderDeathException e) {
    this.orderDeathException = e;
    this.latch.countDown();
  }

  public void handleAfterAwait() {
    if (orderDeathException != null) {
      throw orderDeathException; //TODO besser verpacken
    }
  }

  public void handleInterruptionWhileWaitingForLatch() {
    //nichts zu tun
  }

  public GeneralXynaObject getResponse() {
    return response;
  }

  public XynaException[] getXynaExceptions() {
    return xynaExceptions;
  }
  

  /**
   * Der SynchronousResponseListener muss hier serialisiert werden, obwohl er als synchroner ResponseListener
   * nach einem Deserialisieren kaputt ist. Der eingebettete ResponseListener {@link #baseRl} kann aber 
   * evtl. noch sinnvoll verwendet werden. 
   */
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    this.latch = new CountDownLatch(1);
  }

  public boolean hasNullResponse() {
    return hasNullResponse;
  }


}
