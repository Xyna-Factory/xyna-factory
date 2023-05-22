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
package com.gip.xyna.xprc.xprcods.orderarchive;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;


public interface OrderArchiveRMI extends Remote {
  
  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public OrderInstanceResult search(String user, String password, OrderInstanceSelect select, int maxRows) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public OrderInstanceResult searchOrderInstances(String user, String password, OrderInstanceSelect select, int maxRows, SearchMode searchMode) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_DETAILS)
  public String getCompleteOrder(String user, String password, long id) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public OrderInstanceDetails getOrderInstanceDetails(String user, String password, long id) throws RemoteException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public Triple<String, String, String> getAuditWithApplicationAndVersion(String user, String password, long id) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public SerializablePair<String, RuntimeContext> getAuditWithRuntimeContext(XynaCredentials credentials, long id) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDERARCHIVE_VIEW)
  public AuditInformation getAuditInformation(XynaCredentials credentials, long id) throws RemoteException;
}
