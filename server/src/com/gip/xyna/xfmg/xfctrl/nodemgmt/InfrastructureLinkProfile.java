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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.rmi.RemoteException;
import java.util.Collection;

import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.extendedstatus.ExtendedStatusInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;


public class InfrastructureLinkProfile extends BasicRMIInterFactoryLinkProfile<InterFactoryInfrastructureInterface> {

  public InterFactoryLinkProfileIdentifier getIdentifier() {
    return InterFactoryLinkProfileIdentifier.Infrastructure;
  }

  
  protected GenericRMIAdapter<InterFactoryInfrastructureInterface> getAdapter() throws RMIConnectionFailureException {
    return channel.<InterFactoryInfrastructureInterface>getInterface(InterFactoryInfrastructureInterface.BINDING_NAME);
  }
  

  public SessionCredentials createSession(final String user, final String password) throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<InterFactoryInfrastructureInterface, SessionCredentials>() {

      @Override
      public SessionCredentials exec(InterFactoryInfrastructureInterface rmi) throws RemoteException {
        return rmi.createSession(user, password);
      }
      
    });
  }
  
  public Collection<ExtendedStatusInformation> getExtendedStatus(final XynaCredentials credentials) throws XFMG_NodeConnectException {
    return execNoException(new RMICallNoException<InterFactoryInfrastructureInterface, Collection<ExtendedStatusInformation>>() {

      @Override
      public Collection<ExtendedStatusInformation> exec(InterFactoryInfrastructureInterface rmi) throws RemoteException {
        return rmi.getExtendedStatus(credentials);
      }
      
    });
  }
  

}
