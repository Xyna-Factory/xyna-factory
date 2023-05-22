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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring;



import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.BasicRMIInterFactoryLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;



public class MonitoringLinkProfile extends BasicRMIInterFactoryLinkProfile<MonitoringInterface> {

  @Override
  public InterFactoryLinkProfileIdentifier getIdentifier() {
    return InterFactoryLinkProfileIdentifier.Monitoring;
  }


  @Override
  protected GenericRMIAdapter<MonitoringInterface> getAdapter() throws RMIConnectionFailureException {
    return channel.<MonitoringInterface> getInterface(MonitoringInterface.BINDING_NAME);
  }


  public SearchResult<?> search(final XynaCredentials creds, final SearchRequestBean search) throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<MonitoringInterface, SearchResult<?>>() {

      @Override
      public SearchResult<?> exec(MonitoringInterface rmi) throws RemoteException, XynaException {
        return rmi.search(creds, search);
      }
    });
  }
}
