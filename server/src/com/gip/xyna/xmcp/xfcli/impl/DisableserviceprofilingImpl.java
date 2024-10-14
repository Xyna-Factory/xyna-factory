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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfmon.processmonitoring.profiling.ServiceIdentifier;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Disableserviceprofiling;



public class DisableserviceprofilingImpl extends XynaCommandImplementation<Disableserviceprofiling> {

  public void execute(OutputStream statusOutputStream, Disableserviceprofiling payload) throws XynaException {
    RuntimeContext rtc;
    if (payload.getApplication() != null) {
      rtc = new Application(payload.getApplication(), "irrelevant");
    } else if (payload.getWorkspace() != null) {
      rtc = new Workspace(payload.getWorkspace());
    } else {
      throw new RuntimeException("Either 'application' or 'workspace' parameter must be set");
    }

    if (payload.getOrdertype() == null) {
      Set<ServiceIdentifier> toBeProfiled = EnableserviceprofilingImpl.collectServiceIdentifiers(rtc);
      for (ServiceIdentifier serviceIdentifier : toBeProfiled) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getProcessMonitoring()
            .disableServiceProfiling(serviceIdentifier);
        writeLineToCommandLine(statusOutputStream, "Disabled profiling for ordertype '" + serviceIdentifier.getOrdertype() + "' from "
            + serviceIdentifier.getAdjustedApplicationNameForStatistics());
      }
    } else {
      ServiceIdentifier servId = new ServiceIdentifier(payload.getOrdertype(), rtc);
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getProcessMonitoring().disableServiceProfiling(servId);
    }
  }

}
