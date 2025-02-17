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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;

public interface MonitoringInterface extends Remote {

  String BINDING_NAME = "MonitoringInterface";

  public SearchResult<?> search(XynaCredentials creds, SearchRequestBean search) throws RemoteException, XynaException;
  
}
