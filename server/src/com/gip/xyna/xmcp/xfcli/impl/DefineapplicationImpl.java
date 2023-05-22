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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Defineapplication;



public class DefineapplicationImpl extends XynaCommandImplementation<Defineapplication> {

  public void execute(OutputStream statusOutputStream, Defineapplication payload) throws XynaException {
    
    if(payload.getApplicationName().length() == 0 ) {
      writeLineToCommandLine(statusOutputStream, "applicationName must not be empty");
      return;
    }
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long parentRevision = revisionManagement.getRevision(null, null, payload.getParentWorkspace());

    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("DefineApplication", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, parentRevision,
                                                                                          CommandControl.Operation.APPLICATION_DEFINE);
    tsa.initiate();
    try {
      ApplicationManagementImpl applicationManagement =
          (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

      applicationManagement.defineApplication(payload.getApplicationName(), payload.getComment(), parentRevision, tsa.getUsername());
    } finally {
      tsa.destroy();
    }
  }

}
