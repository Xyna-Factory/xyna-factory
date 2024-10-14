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
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;


/**
 * @see IManualInteraction
 * das rmi-fizierte interface xprc.xprcods.manualinteractiondb
 */
public interface ManualInteractionRMI extends Remote {
  
  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public List<ManualInteractionEntry> listManualInteractionEntries(String user, String password) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.PROCESS_MANUAL_INTERACTION)
  public void processManualInteractionEntry(String user, String password, Long id, String response) throws RemoteException, XynaException;
    
  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public ManualInteractionResult searchManualInteractions(String user, String password, ManualInteractionSelect selectMI, int maxRows) throws RemoteException;
  
  @ProxyAccess(right = ProxyRight.VIEW_MANUAL_INTERACTION)
  public ExtendedManualInteractionResult searchExtendedManualInteractions(String user, String password, ManualInteractionSelect selectMI, int maxRows) throws RemoteException;
}
