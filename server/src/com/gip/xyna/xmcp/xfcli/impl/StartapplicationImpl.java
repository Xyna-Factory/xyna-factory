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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.EnumSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.StartApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Startapplication;




public class StartapplicationImpl extends XynaCommandImplementation<Startapplication> {

  public void execute(OutputStream statusOutputStream, Startapplication payload) throws XynaException {
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    StartApplicationParameters params = new StartApplicationParameters();
    params.setGlobal(payload.getGlobal());
    params.setForceStartInInconsistentCluster(payload.getForce());
    params.setEnableCrons(payload.getEnableCrons());
    
    String[] enableOrderEntrance = payload.getEnableOrderEntrance();
    if (enableOrderEntrance != null && enableOrderEntrance.length > 0) {
      EnumSet<OrderEntranceType> orderEntranceTypes = EnumSet.noneOf(OrderEntranceType.class);
      for (String type : payload.getEnableOrderEntrance()) {
        try{
          orderEntranceTypes.add(OrderEntranceType.valueOf(type));
        } catch (IllegalArgumentException e) {
          writeLineToCommandLine(statusOutputStream, "Unknown order entrance: '" + type + "'");
          return;
        }
      }
      params.setOnlyEnableOrderEntrance(orderEntranceTypes);
    }
    
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_START, new Application(payload.getApplicationName(), payload.getVersionName()));
    try {
      applicationManagement.startApplication(payload.getApplicationName(), payload.getVersionName(), params);

      StringBuilder sb = new StringBuilder();
      applicationManagement.appendOrderEntryInterfaces(sb, payload.getApplicationName(), payload.getVersionName());
      writeLineToCommandLine(statusOutputStream, "Application started:\n", sb);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_START, new Application(payload.getApplicationName(), payload.getVersionName()));
    }
  }

}
