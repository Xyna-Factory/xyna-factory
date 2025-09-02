/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.oas.fman.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;


public class RtcTools {
  
  public List<RtcData> getAllApps() {
    List<RtcData> ret = new ArrayList<>();
    try {
      ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().
                                                  getApplicationManagement();
      if (appMgmt instanceof ApplicationManagementImpl) {
        List<ApplicationInformation> applist = ((ApplicationManagementImpl) appMgmt).listApplications(true, false);
        for (ApplicationInformation app : applist) {
          RuntimeContext rtc = app.asRuntimeContext();
          if (!(rtc instanceof Application)) { continue; }
          RtcData rtcdata = new RtcData(rtc);
          ret.add(rtcdata);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<RtcData> getAllWorkspaces() {
    List<RtcData> ret = new ArrayList<>();
    try {
      List<WorkspaceInformation> wsplist = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().
                                                       getWorkspaceManagement().listWorkspaces(true);
      for (WorkspaceInformation wsp : wsplist) {
        RtcData rtc = new RtcData(wsp.asRuntimeContext());
        ret.add(rtc);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<RtcData> getAllAppsAndWorkspaces() {
    List<RtcData> ret = new ArrayList<>();
    ret.addAll(getAllApps());
    ret.addAll(getAllWorkspaces());
    return ret;
  }
  
  
  public Set<RtcData> getAllRtcsWhichReferenceRtcDirectly(RtcData rtc) {
    Set<RtcData> ret = new HashSet<>();
    try {
      RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      
      RuntimeDependencyContext rdc = RuntimeContextDependencyManagement.asRuntimeDependencyContext(rtc.getRuntimeContext());
      Collection<RuntimeDependencyContext> deps = rtcDependencyManagement.getParentRuntimeContexts(rdc);
      for (RuntimeDependencyContext dep : deps) {
        RtcData refRtc = new RtcData(dep.asCorrespondingRuntimeContext());
        ret.add(refRtc);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public void getAllRtcsWhichReferenceRtcRecursive(RtcData rtc, Set<RtcData> ret) {
    try {
      RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      Set<Long> parentRevs = new HashSet<>();
      rtcDependencyManagement.getParentRevisionsRecursivly(rtc.getRevision(), parentRevs);
      for (Long rev : parentRevs) {
        ret.add(new RtcData(rev));
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
}
