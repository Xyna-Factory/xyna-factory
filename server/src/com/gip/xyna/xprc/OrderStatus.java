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
package com.gip.xyna.xprc;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.manualinteraction.SuspensionCause_ManualInteraction;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceCompensationStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceSuspensionStatus;


public class OrderStatus {

  private static final Logger logger = CentralFactoryLogging.getLogger(OrderStatus.class);
  private OrderArchive orderArchive;
  private StatusChangeProvider statusChangeProvider;


  private OrderArchive getOrderArchive() {
    if (orderArchive == null) {
      orderArchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    }
    return orderArchive;
  }


  private StatusChangeProvider getStatusChangeProvider() {
    if (statusChangeProvider == null) {
      statusChangeProvider = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
                      .getStatusChangeProvider();
    }
    return statusChangeProvider;
  }


  public void changeMasterWorkflowStatus(XynaOrderServerExtension xo, OrderInstanceStatus newState, ODSConnection con)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    getOrderArchive().updateStatus(xo, newState, con);
    getStatusChangeProvider().notifyListeners(xo, newState );
  }

  public void suspendResumeStatus(XynaOrderServerExtension xo, OrderInstanceSuspensionStatus newState, ODSConnection con,
                                  com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause suspensionCause)
                                      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    
    getOrderArchive().updateStatusSuspended(xo, newState, con, suspensionCause);
    if( suspensionCause instanceof SuspensionCause_ManualInteraction ) {
      getStatusChangeProvider().notifyListenersMI(xo, ((SuspensionCause_ManualInteraction)suspensionCause).getOldInstanceStatus() );
    } else {
      getStatusChangeProvider().notifyListeners(xo, newState);
    }
  }

  public void compensationStatus(XynaOrderServerExtension xo, OrderInstanceCompensationStatus newState) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    getOrderArchive().updateStatusCompensation(xo, newState);
    getStatusChangeProvider().notifyListeners(xo, newState);
  }
  
  
  public void changeErrorStatus(XynaOrderServerExtension xo, OrderInstanceStatus newState) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    getOrderArchive().updateStatusOnError(xo, newState);
    getStatusChangeProvider().notifyListeners(xo, newState );
  }


  public void changeMasterWorkflowStatusNoException(XynaOrderServerExtension xo, OrderInstanceStatus newState, ODSConnection con) {
    try {
      changeMasterWorkflowStatus(xo, newState, con);
    } catch (XynaException e) {
      //TODO oder nur an order h�ngen?
      logger.error("error updating status of order " + xo + " to " + newState, e);
    }
  }
  
}
