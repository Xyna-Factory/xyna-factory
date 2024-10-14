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

package com.gip.xyna.xprc.xpce.manualinteraction;



import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction.ProcessManualInteractionResult;


/**
 * implementierung des MI-Endpunkts für ClusteredMI.
 * wird von RMIManagement beim Deployment neu erstellt, damit die deserialisierung von generalXynaObjects noch funktioniert.
 */
public class MIRemoteInterfaceImpl
    implements
      InitializableRemoteInterface,
      ClusteredManualInteractionManagementInterface {

  private ManualInteractionManagement miMgmt;


  public void init(Object... initParameters) {
    miMgmt = (ManualInteractionManagement) initParameters[0];
  }


  public ProcessManualInteractionResult processManualInteractionEntry(Long id, int binding, String response, Long revision)
      throws RemoteException, XynaException {
    return miMgmt.processManualInteractionEntry(id, binding, response, revision);
  }

}
