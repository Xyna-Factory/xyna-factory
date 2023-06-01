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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Setmonlvl;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class SetmonlvlImpl extends XynaCommandImplementation<Setmonlvl> {

  public void execute(OutputStream statusOutputStream, Setmonlvl payload) throws XynaException {
    Integer code = null;
    
    try {
      code = Integer.valueOf(payload.getCode());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Cannot parse code '", payload.getCode(), "'");
      return;
    }
    
    // Prüfung, ob Application/Version existiert
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    Long revision = revisionManagement.getRevision(runtimeContext);

    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_MODIFY, revision);
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
          .setMonitoringLevel(new DestinationKey(payload.getOrderType(), runtimeContext), code);
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_MODIFY, revision);
    }
    
  }

}
