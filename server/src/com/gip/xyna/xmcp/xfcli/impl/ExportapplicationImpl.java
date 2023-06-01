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

import java.io.OutputStream;
import java.io.PrintStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Exportapplication;




public class ExportapplicationImpl extends XynaCommandImplementation<Exportapplication> {

  public void execute(OutputStream statusOutputStream, Exportapplication payload) throws XynaException {
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    CommandControl.tryLock(CommandControl.Operation.APPLICATION_EXPORT, new Application(payload.getApplicationName(), payload.getVersionName()));
    try {
      applicationManagement.exportApplication(payload.getApplicationName(), payload.getVersionName(), payload.getFilename(),
                                              payload.getLocalBuild(), payload.getNewVersion(), payload.getLocal(),
                                              payload.getVerbose(), payload.getCreateStub(), new PrintStream(statusOutputStream), null);  
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_EXPORT, new Application(payload.getApplicationName(), payload.getVersionName()));
    }
      
  }

}
