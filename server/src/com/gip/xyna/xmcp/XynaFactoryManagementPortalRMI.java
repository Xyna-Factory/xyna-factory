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
package com.gip.xyna.xmcp;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;


public interface XynaFactoryManagementPortalRMI extends Remote {

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, checks=2)
  public String getProperty(String user, String password, String key) throws RemoteException;

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, nochecks=true)
  public PropertyMap<String, String> getProperties(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, nochecks=true)
  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.read, checks=2)
  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String user, String password, String key) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.write, checks=2)
  public void setProperty(String user, String password, String key, String value) throws RemoteException; 

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.write, checks=2)
  public void setProperty(String user, String password, XynaPropertyWithDefaultValue property) throws RemoteException; 

  @ProxyAccess(right = ProxyRight.XYNA_PROPERTY, action=Action.delete, checks=2, parameterNames={"user","password","key"} )
  public void removeProperty(String user, String password, String key) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.insert, checks=2)
  public void createOrdertype(String user, String password, OrdertypeParameter ordertypeParameter) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.write, checks=2)
  public void modifyOrdertype(String user, String password, OrdertypeParameter ordertypeParameter) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.delete, checks=2)
  public void deleteOrdertype(String user, String password, OrdertypeParameter ordertypeParameter) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, nochecks=true)
  public List<OrdertypeParameter> listOrdertypes(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, checks=1)
  public List<OrdertypeParameter> listOrdertypes(XynaCredentials creds, RuntimeContext runtimeContext) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_TYPE, action=Action.read, checks=1)
  public List<OrdertypeParameter> listOrdertypes(XynaCredentials creds, SearchOrdertypeParameter sop) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.APPLICATION, action=Action.read, nochecks=true)
  public Collection<ApplicationInformation> listApplications(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public List<SerializablePair<String, Boolean>> listTimeZones() throws RemoteException;
  
  //orderinputgenerators: search geht über das allgemeine search
  
  @ProxyAccess(right = ProxyRight.ORDER_INPUT_SOURCE, action=Action.insert, checks=1)
  public void createOrderInputSource(XynaCredentials credentials, OrderInputSourceStorable inputSource) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_INPUT_SOURCE, action=Action.generate, checks=1)
  public RemoteXynaOrderCreationParameter generateOrderInput(XynaCredentials credentials, long inputSourceId) throws RemoteException;

  @ProxyAccess(right = ProxyRight.ORDER_INPUT_SOURCE, action=Action.generate, checks=1)
  public RemoteXynaOrderCreationParameter generateOrderInput(XynaCredentials credentials, long inputSourceId, OptionalOISGenerateMetaInformation parameters) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_INPUT_SOURCE, action=Action.write, checks=1)
  public void modifyOrderInputSource(XynaCredentials credentials, OrderInputSourceStorable inputSource) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.ORDER_INPUT_SOURCE, action=Action.delete, checks=1)
  public void deleteOrderInputSource(XynaCredentials credentials, long inputSourceId) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PUBLIC)
  public Collection<RemoteDestinationInstanceInformation> listRemoteDestinations(XynaCredentials credentials) throws RemoteException;
  
}
