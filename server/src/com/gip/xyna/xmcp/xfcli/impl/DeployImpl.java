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



import java.io.File;
import java.io.OutputStream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Deploy;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;



public class DeployImpl extends XynaCommandImplementation<Deploy> {

  public void execute(OutputStream statusOutputStream, Deploy payload) throws XynaException {
    
    WorkflowProtectionMode mode = WorkflowProtectionMode.BREAK_ON_USAGE;
    if (payload.getProtectionMode() != null && payload.getProtectionMode().length() > 0) {
      try {
        mode = WorkflowProtectionMode.getByIdentifier(payload.getProtectionMode());
      } catch (IllegalArgumentException e) {
        writeLineToCommandLine(statusOutputStream, "Unknown workflow protection mode: '" + payload.getProtectionMode()
            + "'");
        return;
      }
    }
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    boolean succeeded = false;
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    try {
      File backupXml = null;
      if (payload.getXmlFile() != null) {
        File xmlfile = new File(payload.getXmlFile());
        if (!xmlfile.exists()) {
          throw new Ex_FileAccessException(payload.getXmlFile());
        }
        if (!revisionManagement.isWorkspaceRevision(revision)) {
          File deployedXmlFile =
              new File(GenerationBase.getFileLocationForDeploymentStaticHelper(payload.getFqWorkflowName(), revision) + ".xml");
          backupXml = new File(deployedXmlFile.getAbsolutePath() + ".old");
          FileUtils.copyFile(deployedXmlFile, backupXml);
          FileUtils.copyFile(xmlfile, deployedXmlFile);
        } else {
          String xml = FileUtils.readFileAsString(xmlfile);
          TemporarySessionAuthentication session =
              TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("DeployWorkflow",
                                                                                    TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE,
                                                                                    revision, CommandControl.Operation.XMOM_SAVE);
          session.initiate();
          try {
            String fqName =
                ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal()).saveMDM(xml, true, session.getUsername(),
                                                                                                         session.getSessionId(), revision,
                                                                                                         null, true);
            if (!fqName.equals(payload.getFqWorkflowName())) {
              throw new RuntimeException("XML " + fqName + " has been saved. Different workflow " + payload.getFqWorkflowName() + " will not be deployed.");
            }
          } finally {
            session.destroy();
          }
        }
      }

      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployWorkflow(payload.getFqWorkflowName(), mode, revision);
        succeeded = true;
      } catch (XynaException e) {
        if (backupXml != null) {
          File deployedXmlFile =
              new File(GenerationBase.getFileLocationForDeploymentStaticHelper(payload.getFqWorkflowName(), revision) + ".xml");
          FileUtils.moveFile(backupXml, deployedXmlFile);
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployWorkflow(payload.getFqWorkflowName(), mode, revision);
        }
        throw e;
      }
      if (backupXml != null) {
        backupXml.delete();
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, revision);
    }
    
    if( succeeded ) {
      checkDeploymentItemState((CommandLineWriter)statusOutputStream, payload.getFqWorkflowName(), revision);
    }
  }

  private void checkDeploymentItemState(CommandLineWriter clw, String fqName, Long revision) {
    DeploymentItemState dis = XynaFactory.getInstance().getFactoryManagementPortal().
        getXynaFactoryControl().getDeploymentItemStateManagement().get(fqName, revision);
    DisplayState state = dis.deriveDisplayState();
    if( state != DisplayState.DEPLOYED ) {
      clw.writeLineToCommandLine("Workflow has state " +state +"!");
      clw.close( "call showdeploymentitemdetails to see details.", ReturnCode.XYNA_EXCEPTION);
    }
  }


}
