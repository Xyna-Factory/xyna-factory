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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.extendedstatus.ExtendedStatusInformation;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.RMIChannelImpl;


public class InterFactoryInfrastructureLanding implements InitializableRemoteInterface, InterFactoryInfrastructureInterface {


  public void init(Object... initParameters) {
    
  }
  

  public SessionCredentials createSession(String user, String password) throws RemoteException {
    return RMIChannelImpl.staticCreateSession(user, password, true);
  }


  public Collection<ExtendedStatusInformation> getExtendedStatus(XynaCredentials credentials) throws RemoteException {
    RMIChannelImpl.authenticate(credentials);
    return new ArrayList<ExtendedStatusInformation>(XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement().listExtendedStatusInformation());
  }


}
