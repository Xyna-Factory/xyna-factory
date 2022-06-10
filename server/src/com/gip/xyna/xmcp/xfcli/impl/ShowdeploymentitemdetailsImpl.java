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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ServiceImplInconsistency;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Showdeploymentitemdetails;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class ShowdeploymentitemdetailsImpl extends XynaCommandImplementation<Showdeploymentitemdetails> {

  public void execute(OutputStream statusOutputStream, Showdeploymentitemdetails payload) throws XynaException {
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    boolean verbose = payload.getVerbose();
    long revision =
        rm
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    DeploymentItemState deploymentItemState =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement()
            .get(payload.getObjectName(), revision);
    if (deploymentItemState == null) {
      writeLineToCommandLine(statusOutputStream, "Object unknown.");
      return;
    }
    DeploymentItemStateReport stateReport = deploymentItemState.getStateReport();
    writeLineToCommandLine(statusOutputStream, "Type                : " + stateReport.getType().getNiceName());
    writeLineToCommandLine(statusOutputStream, "Name                : " + stateReport.getFqName());
    writeLineToCommandLine(statusOutputStream, "RuntimeContext      : " + rm.getRuntimeContext(revision));
    writeLineToCommandLine(statusOutputStream, "State               : " + stateReport.getState());
    if (stateReport.getOpenTaskCount() != null && stateReport.getOpenTaskCount() > 0) {
      writeLineToCommandLine(statusOutputStream, "Open Tasks          : " + stateReport.getOpenTaskCount());
    }
    if (stateReport.getInconsitencies() != null && stateReport.getInconsitencies().size() > 0) {
      writeLineToCommandLine(statusOutputStream, "Inconsistencies     : " + stateReport.getInconsitencies().size());
      for (int i = 0; i<stateReport.getInconsitencies().size(); i++) {
        Inconsistency ic = stateReport.getInconsitencies().get(i);
        writeLineToCommandLine(statusOutputStream, " - " + (i+1) + ": " + ic.toFriendlyString());
      }
    }
    if (stateReport.getUnresolvable() != null && stateReport.getUnresolvable().size() > 0) {
      writeLineToCommandLine(statusOutputStream, "Unresolvables       : " + stateReport.getUnresolvable().size());
      for (int i = 0; i<stateReport.getUnresolvable().size(); i++) {
        ResolutionFailure rf = stateReport.getUnresolvable().get(i);
        writeLineToCommandLine(statusOutputStream, " - " + (i+1) + ": " + rf.toFriendlyString());
      }
    }
    if (stateReport.getServiceImplInconsistencies() != null && stateReport.getServiceImplInconsistencies().size() > 0) {
      writeLineToCommandLine(statusOutputStream, "Service Impl Inconsistencies: " + stateReport.getServiceImplInconsistencies().size());
      for (int i = 0; i<stateReport.getServiceImplInconsistencies().size(); i++) {
        ServiceImplInconsistency si = stateReport.getServiceImplInconsistencies().get(i);
        writeLineToCommandLine(statusOutputStream, " - " + (i+1) + ": " + si.toString());
      }
    }
    
    
    if(verbose) {
      StringBuilder sb = new StringBuilder();
      sb.append("\nInterfaces " + stateReport.getFqName() + " publishes in SAVED state:\n");
      PublishedInterfaces pubInterfaces = deploymentItemState.getPublishedInterfaces(DeploymentLocation.SAVED);
      for(DeploymentItemInterface deplInterface:pubInterfaces.getAll()) {
        sb.append("  - " + deplInterface.toString() + "\n");
      }
      
      sb.append("\nInterfaces " + stateReport.getFqName() + " publishes in DEPLOYED state:\n");
      pubInterfaces = deploymentItemState.getPublishedInterfaces(DeploymentLocation.DEPLOYED);
      for(DeploymentItemInterface deplInterface:pubInterfaces.getAll()) {
        sb.append("  - " + deplInterface.toString() + "\n");
      }
      
      sb.append("\nInterfaces " + stateReport.getFqName() + " uses in SAVED state:\n");
      Set<DeploymentItemInterface> emplInterfaces = ((DeploymentItemStateImpl)deploymentItemState).getInterfaceEmployments(DeploymentLocation.SAVED);
      for(DeploymentItemInterface deplInterface:emplInterfaces) {
        sb.append("  - " + deplInterface.toString() + "\n");
      }
      
      sb.append("\nInterfaces " + stateReport.getFqName() + " uses in DEPLOYED state:\n");
      emplInterfaces = ((DeploymentItemStateImpl)deploymentItemState).getInterfaceEmployments(DeploymentLocation.DEPLOYED);
      for(DeploymentItemInterface deplInterface:emplInterfaces) {
        sb.append("  - " + deplInterface.toString() + "\n");
      }
      
      sb.append("\nObjects that use " + stateReport.getFqName() + " in SAVED state:\n");
      Set<DeploymentItemState> callInterfaces = ((DeploymentItemStateImpl)deploymentItemState).getInvocationSites(DeploymentLocation.SAVED);
      for(DeploymentItemState deplInterface:callInterfaces) {
        long rev = ((DeploymentItemStateImpl) deplInterface).getRevision();
        String rtc = "";
        if (rev != revision) {
          rtc = rtcString(rm, rev);
        }
        sb.append("  - " + deplInterface.getName() + rtc + "\n");
      }
      
      sb.append("\nObjects that use " + stateReport.getFqName() + " in DEPLOYED state:\n");
      callInterfaces = ((DeploymentItemStateImpl)deploymentItemState).getInvocationSites(DeploymentLocation.DEPLOYED);
      for(DeploymentItemState deplInterface:callInterfaces) {
        long rev = ((DeploymentItemStateImpl) deplInterface).getRevision();
        String rtc = "";
        if (rev != revision) {
          rtc = rtcString(rm, rev);
        }
        sb.append("  - " + deplInterface.getName() + rtc + "\n");
      }
      
      if (deploymentItemState.getType() == XMOMType.DATATYPE) {
        Map<String, Set<DeploymentItemState>> callersByOperation = ((DeploymentItemStateImpl) deploymentItemState)
            .getInvocationSitesPerOperation(DeploymentLocation.SAVED);
        for (Entry<String, Set<DeploymentItemState>> e : callersByOperation.entrySet()) {
          sb.append("\nObjects that call " + e.getKey() + " in SAVED state:\n");
          for (DeploymentItemState deplInterface : e.getValue()) {
            sb.append("  - " + deplInterface.getName() + "\n");
          }
        }
        callersByOperation = ((DeploymentItemStateImpl) deploymentItemState)
            .getInvocationSitesPerOperation(DeploymentLocation.DEPLOYED);
        for (Entry<String, Set<DeploymentItemState>> e : callersByOperation.entrySet()) {
          sb.append("\nObjects that call " + e.getKey() + " in DEPLOYED state:\n");
          for (DeploymentItemState deplInterface : e.getValue()) {
            sb.append("  - " + deplInterface.getName() + "\n");
          }
        }
      }
      
      writeLineToCommandLine(statusOutputStream, sb.toString());
    }
    
    
    if (stateReport.getBuildException() != null) {
      writeException(statusOutputStream, "Build Exception",stateReport.getBuildException());
    }
    if (stateReport.getRollbackCause() != null) {
      writeException(statusOutputStream, "Deployment Exception",stateReport.getRollbackCause());
    }
    if (stateReport.getRollbackException() != null) {
      writeException(statusOutputStream, "Deployment Rollback Exception",stateReport.getRollbackCause());
    }
  }

  private String rtcString(RevisionManagement rm, long rev) {
    try {
      return " [" + rm.getRuntimeContext(rev).toString() + "]";
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return " [unknown revision " + rev + "]";
    }
  }

  private void writeException(OutputStream s, String exName, SerializableExceptionInformation ex) {
    String spaces = "";
    for (int i = 0; i<20 - exName.length(); i++) {
      spaces += " ";
    }
    writeLineToCommandLine(s, exName + spaces + ": " + ex.getMessage());
    for (StackTraceElement ste : ex.getStackTraceElements()) {
      writeLineToCommandLine(s, "     " + ste.toString());
    }
    if (ex.getCause() != null) {
      writeException(s, "Caused by: ", ex.getCause());
    }
  }

}
