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
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Deploymultiple;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;



public class DeploymultipleImpl extends XynaCommandImplementation<Deploymultiple> {

  public void execute(OutputStream statusOutputStream, Deploymultiple payload) throws XynaException {
    RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    WorkflowProtectionMode mode = WorkflowProtectionMode.BREAK_ON_USAGE;
    if (payload.getProtectionMode() != null && payload.getProtectionMode().length() > 0) {
      try {
        mode = WorkflowProtectionMode.getByIdentifier(payload.getProtectionMode());
      } catch (IllegalArgumentException e) {
        writeLineToCommandLine(statusOutputStream, "Unknown workflow protection mode: '" + payload.getProtectionMode() + "'");
        return;
      }
    }

    boolean succeeded = false;
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    try {
      List<GenerationBase> objects = new ArrayList<GenerationBase>();
      if (payload.getFqDatatypeNames() != null) {
        for (String name : payload.getFqDatatypeNames()) {
          objects.add(DOM.getInstance(name, revision));
        }
      }
      if (payload.getFqWorkflowNames() != null) {
        for (String name : payload.getFqWorkflowNames()) {
          objects.add(WF.getInstance(name, revision));
        }
      }
      if (payload.getFqExceptionNames() != null) {
        for (String name : payload.getFqExceptionNames()) {
          objects.add(ExceptionGeneration.getInstance(name, revision));
        }
      }

      for (GenerationBase gb : objects) {
        gb.setDeploymentComment("CLI deploymultiple");
      }
      GenerationBase.deploy(objects, DeploymentMode.codeChanged, false, mode);
      succeeded = true;
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    }
    
    if( succeeded ) {
      checkDeploymentItemState((CommandLineWriter)statusOutputStream, payload, revision);
    }
  }

  private void checkDeploymentItemState(CommandLineWriter clw, Deploymultiple payload, Long revision) {
    DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagementPortal().
        getXynaFactoryControl().getDeploymentItemStateManagement();
    
    boolean allDeployed = true;
    allDeployed = allDeployed && check(clw, dism, "Datatype", payload.getFqDatatypeNames(), revision);
    allDeployed = allDeployed && check(clw, dism, "Workflow", payload.getFqWorkflowNames(), revision);
    allDeployed = allDeployed && check(clw, dism, "Exception", payload.getFqExceptionNames(), revision);
    if( ! allDeployed ) {
      clw.close( "call showdeploymentitemdetails to see details.", ReturnCode.XYNA_EXCEPTION);
    }
  }

  private boolean check(CommandLineWriter clw, DeploymentItemStateManagement dism, String what, String[] fqNames, Long revision) {
    if (fqNames == null) { 
      return true; //nichts zu tun
    }
    boolean allDeployed = true;
    for (String name : fqNames) {
      DeploymentItemState dis = dism.get(name, revision);
      DisplayState state = dis.deriveDisplayState();
      if( state != DisplayState.DEPLOYED ) {
        clw.writeLineToCommandLine(what+ " "+name+" has state " +state +".");
        allDeployed = false;
      }
    }
    return allDeployed;
  }

}
