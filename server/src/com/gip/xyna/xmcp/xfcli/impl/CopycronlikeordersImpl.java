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
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xmcp.exceptions.XMCP_InvalidParameterCombinationException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Copycronlikeorders;



public class CopycronlikeordersImpl extends XynaCommandImplementation<Copycronlikeorders> {

  public void execute(OutputStream statusOutputStream, Copycronlikeorders payload) throws XynaException {
    if (payload.getId() != null && payload.getOrdertype() != null && payload.getOrdertype().length > 0) {
      throw new XMCP_InvalidParameterCombinationException("id", "payload");
    }
    ApplicationManagement applicationManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_CRONS, new Application(payload.getApplicationName(), payload.getSourceVersion()));
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_CRONS, new Application(payload.getApplicationName(), payload.getTargetVersion()));
      try {
        applicationManagement.copyCronLikeOrders(payload.getApplicationName(), payload.getSourceVersion(), payload.getTargetVersion(),
                                                 new PrintStream(statusOutputStream), payload.getId(), payload.getOrdertype(),
                                                 payload.getMove(), payload.getVerbose(), payload.getGlobal());
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_CRONS, new Application(payload.getApplicationName(), payload.getTargetVersion()));
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_CRONS, new Application(payload.getApplicationName(), payload.getSourceVersion()));
    }

  }

}
