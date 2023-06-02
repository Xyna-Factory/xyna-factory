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
package com.gip.xyna.xprc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.ResponseOfOrderFailedAbortOrder;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.ResponseOfOrderFailedArchiveAsFailed;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.TwoConnectionBean;


/**
 * wird im processing registriert, um code bei empfang eines auftragsergebnisses
 * auszuführen.
 * beispiel: eventlistener triggert einen auftrag und gibt synchron mittels eines
 * responselisteners eine antwort zurück (das connectionobjekt kann so behalten werden)
 */
public abstract class ResponseListener implements Serializable {

  private static final long serialVersionUID = 1L;
  private transient ODSConnection defaultCon = null;
  private transient ODSConnection historyCon = null;
  @Deprecated
  private boolean hasBeenExecuted = false; //eigentlich obsolet durch executionCount, kann aber wegen Serializable nicht entfernt werden
  private AtomicInteger executionCount = new AtomicInteger(0);
  protected boolean singleExecutionOnly = true;
  
  /**
   * vom Processing bei einer Antwort aufgerufen.
   * Über den OrderContext können offene Connections weiterbenutzt werden o.ä.
   * @param response
   * @param ctx
   */
  public abstract void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException;
  
  /**
   * ruft standardmässig auf:
   * <code>onResponse(xo.getOutputPayload(), xo.getOrderContext());</code>
   * @param xo
   */
  public void onResponse(XynaOrder xo) throws XNWH_RetryTransactionException {
    int ec = executionCount.getAndIncrement();
    if( singleExecutionOnly && ec > 0 ) {
      //nur ein einziges Mal aufrufen
      return;
    }
    onResponse(xo.getOutputPayload(), xo.getOrderContext());
  }
  
  
  public ResponseListenerResponse internal_onResponseWithReply(XynaOrder xo) throws XNWH_RetryTransactionException {
    try {
      onResponse(xo);
      return ResponseListenerResponse.finish();
    } catch (ResponseOfOrderFailedAbortOrder e) {
      return ResponseListenerResponse.abortOrder(e);
    } catch (ResponseOfOrderFailedArchiveAsFailed e) {
      return ResponseListenerResponse.archiveOrderAsFailed(e);
    }
  }
  
  public static enum OrderArchiveHandling {
    CONTINUE,
    ABORT_ORDER,
    ARCHIVE_AS_FAILED;
  }
  
  /**
   * ResponseListenerResponse can communicate whether you want the order to:
   * - commit it's already prepared orderarchive entry (<code>ResponseListenerResponse.finish</code>)
   * - abort processing the order so another cluster node can take it over (<code>ResponseListenerResponse.abortOrder</code>)
   * - archive the order as failed (<code>ResponseListenerResponse.archiveOrderAsFailed</code>)
   *
   * UseCases for archiving an order as failed are:
   * - the order had an error during archiving that is not represented in it's current data (check with <code>OrderContext ctx.hadErrorsDuring(ProcessingStage.ARCHIVING)</code>)
   * - there occurred an error during response listener notification that warrants being persistet as part of the order execution
   */
  public static class ResponseListenerResponse {

    private final static ResponseListenerResponse CONTINUE_INSTANCE = new ResponseListenerResponse(OrderArchiveHandling.CONTINUE);
    
    private OrderArchiveHandling handling;
    private Throwable cause;
    
    ResponseListenerResponse(OrderArchiveHandling handling) {
      this.handling = handling;
    }
    
    ResponseListenerResponse(OrderArchiveHandling handling, Throwable cause) {
      this(handling);
      this.cause = cause;
    }
    
    public static ResponseListenerResponse abortOrder(String reason) {
      return abortOrder(new ResponseOfOrderFailedAbortOrder(reason));
    }
    
    public static ResponseListenerResponse abortOrder(Throwable cause) {
      return new ResponseListenerResponse(OrderArchiveHandling.ABORT_ORDER, cause);
    }
    
    public static ResponseListenerResponse archiveOrderAsFailed(String reason) {
      return abortOrder(new ResponseOfOrderFailedArchiveAsFailed(reason));
    }
    
    public static ResponseListenerResponse archiveOrderAsFailed(Throwable cause) {
      return new ResponseListenerResponse(OrderArchiveHandling.ARCHIVE_AS_FAILED, cause);
    }
    
    public static ResponseListenerResponse finish() {
      return CONTINUE_INSTANCE;
    }
    
    public OrderArchiveHandling getHandling() {
      return handling;
    }

    public Throwable getCause() {
      return cause;
    }
    
  }

  /**
   * wird vom Processing aufgerufen, falls die Prozessausführung in einem
   * Fehler geendet hat.
   * @param e
   * @param ctx
   */
  public abstract void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException;

  /**
   * @param xo
   */
  public void onError(XynaOrder xo) throws XNWH_RetryTransactionException {
    int ec = executionCount.getAndIncrement();
    if( singleExecutionOnly && ec > 0 ) {
      //nur ein einziges Mal aufrufen
      return;
    }
    onError(xo.getErrors(), xo.getOrderContext());
  }
  
  public ResponseListenerResponse internal_onErrorWithReply(XynaOrder xo) throws XNWH_RetryTransactionException {
    try {
      onError(xo);
      return ResponseListenerResponse.finish();
    } catch (ResponseOfOrderFailedAbortOrder e) {
      return ResponseListenerResponse.abortOrder(e);
    } catch (ResponseOfOrderFailedArchiveAsFailed e) {
      return ResponseListenerResponse.archiveOrderAsFailed(e);
    }
  }

  protected int getExecutionCount() {
    return executionCount.get();
  }
  
  protected void setSingleExecutionOnly( boolean singleExecutionOnly ) {
    this.singleExecutionOnly = singleExecutionOnly;
  }
  
  
  
  /**
   * Stores the passed in TwoConnectionBean for reusing the connections within all Classes inherited from the
   * ResponseListener
   * @param cons The TwoConnectionBean containing the Default and History connection
   */
  public void setConnections(TwoConnectionBean cons) {
    defaultCon = cons.getDefaultConnection();
    historyCon = cons.getHistoryConnection();
  }


  /**
   * Gets the DEFAULT connection
   * @return The default connection container for use within the Listener
   */
  ODSConnection getDefaultConnection() {
    return defaultCon;
  }


  /**
   * Gets the HISTORY connection
   * @return The history connection container for use within the Listener
   */
  ODSConnection getHistoryConnection() {
    return historyCon;
  }
  
  /**
   * Führt das Runnable nach dem Commit/Close auf die HISTORY-Connection aus
   * @param runnable
   * @throws IllegalStateException wenn keine HISTORY-Connection vorhanden ist
   */
  protected void executeAfterHistoryClose(Runnable runnable) {
    if( historyCon != null ) {
      historyCon.executeAfterClose(runnable);
    } else {
      throw new IllegalStateException("historyCon is not available");
    }
  }
  
  
  private void readObject(ObjectInputStream ois)
                  throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    if( executionCount == null ) {
      executionCount = new AtomicInteger(0);
    }
    if( hasBeenExecuted ) {
      executionCount.getAndIncrement();
    }
    hasBeenExecuted = false; //wird nicht mehr verwendet
  }
  
}
