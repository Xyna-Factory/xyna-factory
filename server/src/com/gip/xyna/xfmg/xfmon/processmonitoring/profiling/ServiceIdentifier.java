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
package com.gip.xyna.xfmg.xfmon.processmonitoring.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;


public class ServiceIdentifier {
  
  static final String WORKSPACE_PREFIX = "Workspace-";
  static final String APPLICATION_NAME_PREFIX = "Application-";
  
  private final String ordertype;
  private final RuntimeContext rtc;
  
  public ServiceIdentifier(String ordertype, RuntimeContext rtc) {
    this.ordertype = ordertype;
    this.rtc = rtc;
  }
  
  
  public boolean isWorkflowIdentifier() {
    return ordertype != null;
  }
  
  
  public String getOrdertype() {
    return ordertype;
  }
  
  public RuntimeContext getRuntimeContext() {
    return rtc;
  }
  
  public String getAdjustedApplicationNameForStatistics() {
    return adjustApplicationNameForStatistics(rtc);
  }
  
  
  public static String adjustApplicationNameForStatistics(RuntimeContext rtc) {
    if (rtc instanceof Application) {
      return APPLICATION_NAME_PREFIX + rtc.getName();
    }
    return WORKSPACE_PREFIX + rtc.getName();
  }
  
  
  Collection<DestinationKey> getDestinationKeysForWorkflowIdentifier() { //ordertype != null
    if (rtc instanceof Workspace) {
      return Collections.singletonList(new DestinationKey(ordertype, rtc));
    } else {
      try {
        //alle versionen mit dem gleichen namen suchen
        List<ApplicationInformation> applications = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().listApplications();
        ExecutionDispatcher executionDispatcher = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
        Collection<DestinationKey> dks = new ArrayList<DestinationKey>(); 
        for (ApplicationInformation appInfo : applications) {
          if (appInfo.getName().equals(rtc.getName())) {
            DestinationKey dk = new DestinationKey(ordertype, new Application(rtc.getName(), appInfo.getVersion()));
            if (executionDispatcher.isDefined(dk)) {
              dks.add(dk);
            }
          }
        }
        return dks;
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not retrieve appInfo",e);
      }
    }
  }

  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof ServiceIdentifier)) {
      return false;
    }
    ServiceIdentifier other = (ServiceIdentifier) obj;
    if (ordertype == null) {
      if (other.ordertype != null) {
        return false;
      }
    } else if (!ordertype.equals(other.ordertype)) {
      return false;
    }
    //nur der applicationname zählt, die version nicht
    return rtc.getClass() == other.rtc.getClass() && rtc.getName().equals(other.rtc.getName());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(ordertype, rtc.getName());
  }
}
