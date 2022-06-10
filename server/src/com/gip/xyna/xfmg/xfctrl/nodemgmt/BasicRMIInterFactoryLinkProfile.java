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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;



import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryChannelIdentifier;



public abstract class BasicRMIInterFactoryLinkProfile<R extends Remote> implements InterFactoryLinkProfile {

  protected String nodeName;
  protected InterFactoryRMIChannel channel;
  private Duration timeout;

  public void init(String nodeName, InterFactoryChannel channel, Duration timeout) {
    this.nodeName = nodeName;
    if (channel.getIdentifier().equals(InterFactoryChannelIdentifier.RMI) && channel instanceof InterFactoryRMIChannel) {
      this.channel = (InterFactoryRMIChannel) channel;
    }
    this.timeout = timeout;
  }


  protected interface RMICall<T extends Remote, R> {

    public R exec(T rmi) throws RemoteException, XynaException;
  }


  protected interface RMICallNoException<T extends Remote, R> {

    public R exec(T rmi) throws RemoteException;
  }


  protected <T> T execNoException(final RMICallNoException<R, T> c) throws XFMG_NodeConnectException {
    try {
      return exec(new RMICall<R, T>() {

        @Override
        public T exec(R rmi) throws RemoteException, XynaException {
          return c.exec(rmi);
        }

      });
    } catch (XFMG_NodeConnectException e) {
      throw e;
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  protected <T> T exec(RMICall<R, T> c) throws XFMG_NodeConnectException, XynaException {
    long start = System.currentTimeMillis();
    GenericRMIAdapter<R> rmi = null;
    channel.setupCommunication(timeout);
    try {
      for (int cnt = 0; cnt < 2; cnt++) {
        try {
          rmi = getAdapter();
          return c.exec(rmi.getRmiInterface());
        } catch (RMIConnectionFailureException e) {
          long end = System.currentTimeMillis();
          if (rmi != null) {
            rmi.reconnectOnNextTry();
          }
          throw new XFMG_NodeConnectException(nodeName + " in " + (end - start) + " ms", e);
        } catch (XynaException e) {
          throw e;
        } catch (RuntimeException e) {
          throw e;
        } catch (Error e) {
          throw e;
        } catch (NoSuchObjectException e) {
          long end = System.currentTimeMillis();
          if (rmi != null) {
            rmi.reconnectOnNextTry();
          }
          if (cnt > 0) {
            throw new XFMG_NodeConnectException(nodeName + " in " + (end - start) + " ms", e);
          } //else 1 retry, für den fall, dass das remote objekt referenz einfach nur veraltet war (z.b. durch classloader-reloading)
        } catch (Exception e) { //RemoteException
          long end = System.currentTimeMillis();
          if (rmi != null) {
            rmi.reconnectOnNextTry();
          }
          throw new XFMG_NodeConnectException(nodeName + " in " + (end - start) + " ms", e);
        }
      }
      throw new RuntimeException("unexpected"); //diese zeile sollte nie erreicht werden
    } finally {
      channel.tearDownSetup();
    }
  }


  protected abstract GenericRMIAdapter<R> getAdapter() throws RMIConnectionFailureException;


  public Set<InterFactoryChannelIdentifier> getSupportedChannelIdentifier() {
    return Collections.singleton(InterFactoryChannelIdentifier.RMI);
  }
}
