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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.local;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecutionInterface;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RMIChannelImpl;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;


public class RemoteOrderExecutionProfileLanding implements InitializableRemoteInterface, RemoteOrderExecutionInterface {
  
  private ConcurrentMap<String, List<Thread>> waitingThreads = new ConcurrentHashMap<String, List<Thread>>();
  private static Logger logger = Logger.getLogger(RemoteOrderExecutionProfileLanding.class);
  
  public void init(Object... initParameters) {
    
  }

  private Role authenticate(XynaCredentials credentials) throws RemoteException {
    return RMIChannelImpl.authenticate(credentials);
  }
  
  /*
   * gib orderid zurück oder fehler, wenn auftrag fehlerhaft war (nicht gestartet/fehler im planning).
   */
  public OrderExecutionResponse createOrder(XynaCredentials credentials, String identifier, 
      final RemoteCallXynaOrderCreationParameter creationParameter, final TransactionMode mode) throws RemoteException {
    Role role = authenticate(credentials);
    try {
      if (!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(Rights.START_ORDER.name(), role)) {
        String scopedRight = ScopedRightUtils.getStartOrderRight(creationParameter.getDestinationKey());
        if (!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(scopedRight, role)) {
          throw new XFMG_ACCESS_VIOLATION(scopedRight, role.getName());
        }
      }

      creationParameter.setTransientCreationRole(role);
      
      if (credentials instanceof XynaPlainSessionCredentials) {
        creationParameter.setSessionId(((XynaPlainSessionCredentials) credentials).getSessionId());
      }

      try {
        return executeInRemoteOrderStorage(identifier, new ValueProcessor<RemoteOrderStorage, OrderExecutionResponse>() {

          public OrderExecutionResponse exec(RemoteOrderStorage v) {
            try {
              return v.createOrderInternal(creationParameter, mode);
            } catch (Throwable t) {
              throw new RuntimeException(t);
            }
          }

        });
      } catch (RuntimeException e) {
        throw e.getCause();
      }
    } catch (Throwable t) {
      // TODO cancel order?
      return new ErroneousOrderExecutionResponse(t, RemoteOrderResponseListener.wrapExceptionInXml);
    }
  }
  
  private <R> R executeInRemoteOrderStorage(String identifier, ValueProcessor<RemoteOrderStorage, R> processor) {
    RemoteDestinationManagement rdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
    return rdm.useRemoteOrderStorage(identifier, processor);
  }

  public List<RemoteData> awaitData(XynaCredentials credentials,
      String identifier, final long timeoutMillis ) throws RemoteException {
    try {
      authenticate(credentials);
      return executeInRemoteOrderStorage(identifier, new ValueProcessor<RemoteOrderStorage, List<RemoteData>>() {

        public List<RemoteData> exec(RemoteOrderStorage v) {
          waitingThreads.putIfAbsent(identifier, new ArrayList<Thread>());
          Thread t = Thread.currentThread();
          if(logger.isDebugEnabled()) {
            logger.debug("waitingThread added: " + t.getName());
          }
          waitingThreads.get(identifier).add(t);
          List<RemoteData> result = v.awaitData(timeoutMillis);
          waitingThreads.get(identifier).remove(t);
          if(logger.isDebugEnabled()) {
            logger.debug("waitingThread removed: " + t.getName());
          }
          return result;
        }

      });
    } catch (Throwable t) {
      throw new RemoteException("awaitOrders failed: "+t.getMessage(), t);
    }
  }

  public int getRunningCount(XynaCredentials credentials, String identifier)
      throws RemoteException {
    try {
      authenticate(credentials);
      return (int) executeInRemoteOrderStorage(identifier, new ValueProcessor<RemoteOrderStorage, Integer>() {

        public Integer exec(RemoteOrderStorage v) {
            return v.getRunningCount();
        }

      });
    } catch (Throwable t) {
      throw new RemoteException("getRunningCount failed: "+t.getMessage(), t);
    }
  }
  
  public List<Long> checkRunningOrders(XynaCredentials credentials, String identifier, final List<Long> orderIds)
      throws RemoteException {
    try {
      authenticate(credentials);
      return executeInRemoteOrderStorage(identifier, new ValueProcessor<RemoteOrderStorage, List<Long>>() {

        public List<Long> exec(RemoteOrderStorage v) {
            return v.checkRunningOrders(orderIds);
        }

      });
    } catch (Throwable t) {
      throw new RemoteException("getRunningCount failed: "+t.getMessage(), t);
    }
  }

  @Override
  public void abortCommunication(XynaCredentials credentials, String identifier) throws RemoteException{
    waitingThreads.putIfAbsent(identifier, new ArrayList<Thread>());
    List<Thread> threads = waitingThreads.get(identifier);
    for(Thread t : threads) {
      t.interrupt();
    }
  }

}
