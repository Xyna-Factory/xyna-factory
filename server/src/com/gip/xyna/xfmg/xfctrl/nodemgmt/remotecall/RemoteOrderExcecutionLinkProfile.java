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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall;



import java.rmi.RemoteException;
import java.util.List;

import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.BasicRMIInterFactoryLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.local.RemoteOrderExecutionProfileLanding;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecutionInterface.TransactionMode;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;



public class RemoteOrderExcecutionLinkProfile extends BasicRMIInterFactoryLinkProfile<RemoteOrderExecutionInterface> {

  public InterFactoryLinkProfileIdentifier getIdentifier() {
    return InterFactoryLinkProfileIdentifier.OrderExecution;
  }


  protected GenericRMIAdapter<RemoteOrderExecutionInterface> getAdapter() throws RMIConnectionFailureException {
    return channel.<RemoteOrderExecutionInterface> getInterface(RemoteOrderExecutionProfileLanding.BINDING_NAME);
  }


  public OrderExecutionResponse createOrder(final XynaCredentials credentials, final String identifier,
                                            final RemoteCallXynaOrderCreationParameter creationParameter, final TransactionMode mode)
      throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<RemoteOrderExecutionInterface, OrderExecutionResponse>() {

      @Override
      public OrderExecutionResponse exec(RemoteOrderExecutionInterface rmi) throws RemoteException {
        return rmi.createOrder(credentials, identifier, creationParameter, mode);
      }

    });
  }


  public List<RemoteData> awaitData(final XynaCredentials credentials, final String identifier, final long timeoutMillis)
      throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<RemoteOrderExecutionInterface, List<RemoteData>>() {

      @Override
      public List<RemoteData> exec(RemoteOrderExecutionInterface rmi) throws RemoteException {
        return rmi.awaitData(credentials, identifier, timeoutMillis);
      }

    });
  }


  public int getRunningCount(final XynaCredentials credentials, final String identifier) throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<RemoteOrderExecutionInterface, Integer>() {

      @Override
      public Integer exec(RemoteOrderExecutionInterface rmi) throws RemoteException {
        return rmi.getRunningCount(credentials, identifier);
      }

    });
  }


  public List<Long> checkRunningOrders(final XynaCredentials credentials, final String identifier, final List<Long> orderIds)
      throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<RemoteOrderExecutionInterface, List<Long>>() {

      @Override
      public List<Long> exec(RemoteOrderExecutionInterface rmi) throws RemoteException {
        return rmi.checkRunningOrders(credentials, identifier, orderIds);
      }

    });
  }

  public void abortCommunication(final XynaCredentials credentials, final String identifier) throws XFMG_NodeConnectException{
    execNoException(new RMICallNoException<RemoteOrderExecutionInterface, Void>() {

      @Override
      public Void exec(RemoteOrderExecutionInterface rmi) throws RemoteException {
        rmi.abortCommunication(credentials, identifier);
        return null;
      }
      
    });
  }

}
