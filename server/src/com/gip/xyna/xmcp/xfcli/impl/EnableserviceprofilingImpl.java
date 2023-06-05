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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfmon.processmonitoring.profiling.ServiceIdentifier;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Enableserviceprofiling;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class EnableserviceprofilingImpl extends XynaCommandImplementation<Enableserviceprofiling> {

  public void execute(OutputStream statusOutputStream, Enableserviceprofiling payload) throws XynaException {
    RuntimeContext rtc;
    if (payload.getApplication() != null) {
      rtc = new Application(payload.getApplication(), "irrelevant");
    } else if (payload.getWorkspace() != null) {
      rtc = new Workspace(payload.getWorkspace());
    } else {
      throw new RuntimeException("Either 'application' or 'workspace' parameter must be set");
    }

    if (payload.getOrdertype() == null) {
      
      Set<ServiceIdentifier> toBeProfiled = collectServiceIdentifiers(rtc);
      for (ServiceIdentifier serviceIdentifier : toBeProfiled) {
        try {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getProcessMonitoring()
              .enableServiceProfiling(serviceIdentifier);
          writeLineToCommandLine(statusOutputStream, "Enabled profiling for ordertype '" + serviceIdentifier.getOrdertype() + "' from "
              + serviceIdentifier.getAdjustedApplicationNameForStatistics());
        } catch (XFMG_StatisticAlreadyRegistered e) {
          //ntbd
        }
      }
    } else {
      ServiceIdentifier servId = new ServiceIdentifier(payload.getOrdertype(), rtc);
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getProcessMonitoring().enableServiceProfiling(servId);
    }
  }


  public static Set<ServiceIdentifier> collectServiceIdentifiers(RuntimeContext rtc) throws PersistenceLayerException {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    Set<ServiceIdentifier> toBeProfiled = new HashSet<ServiceIdentifier>();
    List<RuntimeContext> checkRTCList = new ArrayList<>();
    if (rtc instanceof Workspace) {
      checkRTCList.add(rtc);
    } else {
      for (Application app : rm.getApplications()) {
        if (app.getName().equals(rtc.getName())) {
          checkRTCList.add(app);
        }
      }
    }

    for (RuntimeContext rc : checkRTCList) {
      List<OrdertypeParameter> ordertypes =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement().listOrdertypes(rc);
      for (OrdertypeParameter ot : ordertypes) {
        toBeProfiled.add(new ServiceIdentifier(ot.getOrdertypeName(), rtc));
      }
    }
    return toBeProfiled;
  }

}
