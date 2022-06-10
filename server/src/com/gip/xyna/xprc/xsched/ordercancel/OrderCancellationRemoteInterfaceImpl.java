/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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


package com.gip.xyna.xprc.xsched.ordercancel;



import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean.CANCEL_RESULT;
import com.gip.xyna.xprc.xsched.ordercancel.OrderCancellationManagement.SOURCE;



public class OrderCancellationRemoteInterfaceImpl
    implements
      InitializableRemoteInterface,
      ClusteredOrderCancellationManagementInterface {

  private OrderCancellationManagement ocMgmt;


  public boolean processCancellation(CancelBean bean) throws RemoteException, XynaException {
    return ocMgmt.processCancellationLocally(bean);
  }


  public boolean processCancellationAndWait(CancelBean bean) throws RemoteException, XynaException {
    return ocMgmt.processCancellationAndWaitRemotely(bean);
  }


  public void reportCancellationListenerResult(Long orderID, SOURCE source, CANCEL_RESULT result)
      throws RemoteException {
    ocMgmt.processCancellationListenerResult(orderID, source, result);
  }


  public void init(Object... initParameters) {
    ocMgmt = (OrderCancellationManagement) initParameters[0];
  }

}
