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
import java.util.Map;
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

  public static enum RecursiveDepthMode {
    LIMITED, UNLIMITED
  }
  
  
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
  
  
  public void getAllRtcsWhichReferenceRtcRecursive(RtcData rtc, Set<RtcData> set) {
    getAllRtcsWhichReferenceRtcRecursiveImpl(rtc, set, -1, 0, RecursiveDepthMode.UNLIMITED);
  }
  
  
  public Set<RtcData> getAllRtcsWhichReferenceRtcRecursive(RtcData rtc) {
    Set<RtcData> ret = new HashSet<>();
    getAllRtcsWhichReferenceRtcRecursiveImpl(rtc, ret, -1, 0, RecursiveDepthMode.UNLIMITED);
    return ret;
  }
    
    
  public Set<RtcData> getAllRtcsWhichReferenceRtcRecursiveLimited(RtcData rtc, int maxDepth) {
    Set<RtcData> ret = new HashSet<>();
    getAllRtcsWhichReferenceRtcRecursiveImpl(rtc, ret, maxDepth, 0, RecursiveDepthMode.LIMITED);
    return ret;
  }
  
  
  private void getAllRtcsWhichReferenceRtcRecursiveImpl(RtcData rtc, Set<RtcData> ret, int maxDepth,
                                                        int depth, RecursiveDepthMode mode) {
    RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> map = rtcDependencyManagement.getAllDependencies();
    getAllRtcsWhichReferenceRtcRecursiveStep(rtc, ret, maxDepth, depth, mode, map);
  }
  
  
  private void getAllRtcsWhichReferenceRtcRecursiveStep(RtcData rtc, Set<RtcData> ret, int maxDepth,
                                                        int depth, RecursiveDepthMode mode,
                                                        Map<RuntimeDependencyContext,
                                                            Collection<RuntimeDependencyContext>> dependencies) {
    if (ret.contains(rtc)) { return; }
    if (mode == RecursiveDepthMode.UNLIMITED) {
      if (depth > 0) {
        ret.add(rtc);
      }
    } else {
      if (depth > maxDepth) {
        return;
      } else if (depth == maxDepth) {
        ret.add(rtc);
      }
    }
    try {
      RuntimeContext runtimeContext = rtc.getRuntimeContext();
      List<RtcData> matched = new ArrayList<>();
      for (Map.Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
        for (RuntimeDependencyContext dep : entry.getValue()) {
          RuntimeContext tmpRtc = dep.asCorrespondingRuntimeContext();
          if (tmpRtc.equals(runtimeContext)) {
            RtcData refRtc = new RtcData(entry.getKey().asCorrespondingRuntimeContext());
            matched.add(refRtc);
          }
        }
      }
      for (RtcData refRtc : matched) {
        getAllRtcsWhichReferenceRtcRecursiveImpl(refRtc, ret, maxDepth, depth + 1, mode);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
}
