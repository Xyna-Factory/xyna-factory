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
package com.gip.xyna.xmcp;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerManagement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.healthmarketscience.rmiio.RemoteInputStream;


/**
 * @see TriggerManagement
 * das rmi-fizierte interface von xact.triggermanagement
 */
public interface TriggerManagementRMI extends Remote {


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void addTrigger(String user, String password, String name, RemoteInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, long revision) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void deployTrigger(String user, String password, String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter,
                            String description, long revision) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void addFilter(String user, String password, String filterName, RemoteInputStream jarFiles, String fqFilterClassName, String triggerName,
                        String[] sharedLibs, String description, long revision) throws XynaException, RemoteException;


  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public void deployFilter(String user, String password, String filtername, String nameOfFilterInstance, String nameOfTriggerInstance,
                           String description, long revision) throws XynaException, RemoteException;
  
  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public List<FilterInformation> listFilters(XynaCredentials credentials) throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public List<TriggerInformation> listTriggers(XynaCredentials credentials) throws XynaException, RemoteException;
  
  
  @ProxyAccess(right = ProxyRight.TRIGGER_FILTER_MANAGEMENT)
  public boolean modifyTriggerInstanceStatus(XynaCredentials credentials, String triggerInstanceName, boolean enable) throws XynaException, RemoteException;

}
