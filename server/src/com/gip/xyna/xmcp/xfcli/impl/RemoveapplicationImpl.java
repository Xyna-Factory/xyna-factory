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
import java.io.PrintStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotLockOperation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Removeapplication;



public class RemoveapplicationImpl extends XynaCommandImplementation<Removeapplication> {

  public void execute(OutputStream statusOutputStream, Removeapplication payload) throws XynaException {
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    RemoveApplicationParameters params = new RemoveApplicationParameters();
    params.setGlobal(payload.getGlobal());
    params.setForce(payload.getForce());
    params.setExtraForce(payload.getExtraforce());
    params.setKeepForAudits(true); //wird in removeapplication umgesetzt, falls application neuer als xmomrepository feature ist
    Workspace parentWorkspace = payload.getParentWorkspace() == null ? RevisionManagement.DEFAULT_WORKSPACE : new Workspace(payload.getParentWorkspace());
    params.setParentWorkspace(parentWorkspace);

    ApplicationName application = new ApplicationName(payload.getApplicationName(), payload.getVersionName());

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long parentRevision = revisionManagement.getRevision(null, null, payload.getParentWorkspace());
    
    if (payload.getVersionName() == null || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .isApplicationDefinition(payload.getApplicationName(), payload.getVersionName(), parentRevision)) {

      TemporarySessionAuthentication tsa =
                      TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("RemoveApplicationDefintion", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, parentRevision,
                                                                                            CommandControl.Operation.APPLICATION_REMOVE_DEFINITION);
      tsa.initiate();
      try {
        params.setUser(tsa.getUsername());
        applicationManagement.removeApplicationVersion(application, params, payload.getVerbose(), new PrintStream(statusOutputStream), new SingleRepositoryEvent(parentRevision), true);
      } finally {
        tsa.destroy();
      }
      return;
    }
    
    try {
      CommandControl.tryLock(Operation.APPLICATION_REMOVE, new Application(payload.getApplicationName(), payload.getVersionName()));
      CommandControl.unlock(Operation.APPLICATION_REMOVE, new Application(payload.getApplicationName(), payload.getVersionName())); //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    } catch (XFMG_CouldNotLockOperation e) {
      if (!payload.getExtraforce()) {
        throw e;
      }
    }
    
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUser("RemoveApplication", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
    tsa.initiate();
    try {
      params.setUser(tsa.getUsername());
      applicationManagement.removeApplicationVersion(application, params, payload.getVerbose(), new PrintStream(statusOutputStream), new EmptyRepositoryEvent(), true);
    } finally {
      tsa.destroy();
    }
  }

}
