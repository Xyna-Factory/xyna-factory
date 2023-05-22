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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import com.gip.xyna.xfmg.extendedstatus.ExtendedStatusInformation;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;


public interface InterFactoryInfrastructureInterface extends Remote {
  
  public final static String BINDING_NAME = "InterFactoryInfrastructureInterface";

  public SessionCredentials createSession(String user, String password) throws RemoteException;
  
  public Collection<ExtendedStatusInformation> getExtendedStatus(XynaCredentials credentials) throws RemoteException;
  
}
