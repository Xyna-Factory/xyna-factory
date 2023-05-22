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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.local;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RMIChannelImpl;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.SuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccessfullRemoteOrderExecutionResponse;
import com.gip.xyna.xmcp.WrappingType;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.ResponseListenerWithConnectionAccess;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.OrderContext;

public class RemoteOrderResponseListener extends ResponseListenerWithConnectionAccess {

  private static final long serialVersionUID = 1L;
  
  private Long orderId;
  private OrderExecutionResponse response;
  private transient CountDownLatch cdl = new CountDownLatch(1);
  private transient RemoteOrderStorage remoteOrderStorage;
  
  public static ResultController wrapExceptionInXml;
  static {
    wrapExceptionInXml = new ResultController();
    wrapExceptionInXml.setDefaultWrappingTypeForExceptions(WrappingType.XML);
  }
  
  public RemoteOrderResponseListener(RemoteOrderStorage remoteOrderStorage, long orderId) {
    this.remoteOrderStorage = remoteOrderStorage;
    this.orderId = orderId;
  }

  @Override
  public void onResponse(com.gip.xyna.xprc.XynaOrder xo) throws XNWH_RetryTransactionException {
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(((XynaOrderServerExtension)xo).getIdOfLatestDeploymentFromOrder());
    try {
      super.onResponse(xo);
    } finally {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(((XynaOrderServerExtension)xo).getIdOfLatestDeploymentFromOrder());  
    }
  }
  
  public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
    SuccesfullOrderExecutionResponse soer = new SynchronousSuccessfullRemoteOrderExecutionResponse(response, orderId, ctx.getRevision());
    try {
      awaitPickup(soer);
    } catch (PersistenceLayerException e) {
      throw new XNWH_RetryTransactionException(e);
    }
  }


  @Override
  public void onError(com.gip.xyna.xprc.XynaOrder xo) throws XNWH_RetryTransactionException {
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(((XynaOrderServerExtension)xo).getIdOfLatestDeploymentFromOrder());
    try {
      super.onError(xo);
    } finally {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(((XynaOrderServerExtension)xo).getIdOfLatestDeploymentFromOrder());  
    }
  };
  
  public void onError(XynaException[] e, OrderContext ctx)
      throws XNWH_RetryTransactionException {
    
    Throwable t = null;
    if (e.length == 1) {
      t = e[0];
    } else if (e.length > 0) {
      t = MultipleExceptions.create(Arrays.asList(e));
    }

    ErroneousOrderExecutionResponse eoer = new ErroneousOrderExecutionResponse(t, wrapExceptionInXml);
    eoer.setOrderId(orderId);
   
    try {
      awaitPickup(eoer);
    } catch (PersistenceLayerException e1) {
      throw new XNWH_RetryTransactionException(e1);
    }
  }


  private AtomicBoolean storeResponse = new AtomicBoolean(false);
  private static final XynaPropertyDuration synchWaitForPickup =
      new XynaPropertyDuration("xfmg.xfctrl.nodemgmt.response.pickup.waitsync.duration", new Duration(50))
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "The maximum wait time for synchronous pickup of a Xyna Order response (of a remote call) by the client factory node."
                                       + " During the waiting time the order may occupy persistence layer connections for orderarchive purposes. "
                                       + "If the order is not successfully synchronously picked up within this time, it will be stored by a StoredResponse storable.");


  /**
   * Warten auf Abholung durch awaitOrders
   */
  private void awaitPickup(OrderExecutionResponse oer) throws PersistenceLayerException {
    this.response = oer;
    //antwort zur abholung freigeben
    remoteOrderStorage.finished(orderId);
    //kurz warten ob antwort vom client abgeholt ist. ansonsten persistieren 
    try {
      if (cdl.await(synchWaitForPickup.getMillis(), TimeUnit.MILLISECONDS)) {
        //antwort wurde abgeholt
        return;
      }
    } catch( InterruptedException e ) {
      //dann halt nicht mehr auf Abholung warten
    }
    if (storeResponse.compareAndSet(false, true)) {
      //antwort speichern, damit sie beim servershutdown/-crash nicht verloren geht
      remoteOrderStorage.storeResponse(orderId, response, getHistoryConnection());
    }
  }


  public OrderExecutionResponse finishOrder() {
    cdl.countDown();
    if (!storeResponse.compareAndSet(false, true)) {
      //antwort aus db l�schen
      remoteOrderStorage.removeResponse(orderId);
    }
    return response;
  }

  private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject(); 
    stream.writeObject(remoteOrderStorage.getIdentifier());
  }

  private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    String identifier = (String) stream.readObject();
    
    RemoteDestinationManagement rdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
    remoteOrderStorage = rdm.getRemoteOrderStorage(identifier);
    cdl = new CountDownLatch(1);
    
  }
  
  private Object readResolve() throws ObjectStreamException {
     Object resolution = remoteOrderStorage.resolveRemoteOrderResponseListener(this);
     if (resolution == null) {
       return this;
     } else {
       return resolution;
     }
  }

  public Long getOrderId() {
    return orderId;
  }
  
  
}
