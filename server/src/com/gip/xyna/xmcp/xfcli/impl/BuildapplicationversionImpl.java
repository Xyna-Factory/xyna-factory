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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildapplicationversion;



public class BuildapplicationversionImpl extends XynaCommandImplementation<Buildapplicationversion> {

  public void execute(OutputStream statusOutputStream, Buildapplicationversion payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long parentRevision = revisionManagement.getRevision(null, null, payload.getParentWorkspace());
    
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("BuildApplicationVersion", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, parentRevision,
                                                                                          CommandControl.Operation.APPLICATION_BUILD);
    tsa.initiate();
    try {
      ApplicationManagementImpl applicationManagement =
          (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

      List<String> excludeSubtypesOf = new ArrayList<String>();
      if (payload.getExcludeSubtypesOf() != null) {
        excludeSubtypesOf.addAll(Arrays.asList(payload.getExcludeSubtypesOf()));
      }
      
      BuildApplicationVersionParameters params = new BuildApplicationVersionParameters();
      params.setComment(payload.getComment());
      params.setExcludeSubtypesOf(excludeSubtypesOf);
      Workspace parentWorkspace = payload.getParentWorkspace() == null ? RevisionManagement.DEFAULT_WORKSPACE : new Workspace(payload.getParentWorkspace());
      params.setParentWorkspace(parentWorkspace);
      params.setUser(tsa.getUsername());
      params.setRemoteStub(payload.getRemoteStub());
      
      applicationManagement.buildApplicationVersion(payload.getApplicationName(), payload.getVersionName(),
                                                    params, payload.getVerbose(), new PrintStream(statusOutputStream));
    } finally {
      tsa.destroy();
    }

  }

}
