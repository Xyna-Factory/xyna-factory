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
import java.io.PrintStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Copyordertypes;



public class CopyordertypesImpl extends XynaCommandImplementation<Copyordertypes> {

  public void execute(OutputStream statusOutputStream, Copyordertypes payload) throws XynaException {
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, new Application(payload.getApplicationName(), payload.getSourceVersion()));
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, new Application(payload.getApplicationName(), payload.getTargetVersion()));
      try {
        ApplicationManagement applicationManagement =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        applicationManagement.copyOrderTypes(payload.getApplicationName(), payload.getSourceVersion(), payload.getTargetVersion(),
                                             new PrintStream(statusOutputStream));
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, new Application(payload.getApplicationName(), payload.getTargetVersion()));
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_ORDERTYPES, new Application(payload.getApplicationName(), payload.getSourceVersion()));
    }

  }

}
