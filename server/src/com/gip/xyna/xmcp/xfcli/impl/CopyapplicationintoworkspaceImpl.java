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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import java.io.PrintStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Copyapplicationintoworkspace;



public class CopyapplicationintoworkspaceImpl extends XynaCommandImplementation<Copyapplicationintoworkspace> {

  public void execute(OutputStream statusOutputStream, Copyapplicationintoworkspace payload) throws XynaException {
    String applicationName = payload.getApplicationName();
    String versionName = payload.getVersionName();
    
    Long targetRevision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    Workspace targetWorkspace = RevisionManagement.DEFAULT_WORKSPACE;
    
    if (payload.getWorkspaceName() != null) {
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      targetWorkspace = new Workspace(payload.getWorkspaceName());
      targetRevision = revisionManagement.getRevision(targetWorkspace);
    }
    
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("CopyApplicationIntoWorkspace", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, targetRevision,
                                                                                          CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET);
    tsa.initiate();
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      try {
        ApplicationManagementImpl applicationManagement =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

        CopyApplicationIntoWorkspaceParameters params = new CopyApplicationIntoWorkspaceParameters();
        params.setTargetWorkspace(targetWorkspace);
        params.setComment(payload.getComment());
        params.setOverrideChanges(true);
        params.setUser(tsa.getUsername());
        
        applicationManagement.copyApplicationIntoWorkspace(applicationName, versionName, params, payload.getVerbose(),
                                                            new PrintStream(statusOutputStream));
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(applicationName, versionName));
      }
    } finally {
      tsa.destroy();
    }

  }

}
