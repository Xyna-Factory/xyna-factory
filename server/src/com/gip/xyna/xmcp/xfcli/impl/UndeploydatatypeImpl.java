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
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Undeploydatatype;



public class UndeploydatatypeImpl extends XynaCommandImplementation<Undeploydatatype> {

  public void execute(OutputStream statusOutputStream, Undeploydatatype payload) throws XynaException {
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    CommandControl.tryLock(CommandControl.Operation.XMOM_DATATYPE_UNDEPLOY, revision);
    try {
      XynaFactory.getInstance().getProcessing().getWorkflowEngine()
          .undeployDatatype(payload.getFqDatatypeName(), false, payload.getIgnore(), revision);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_DATATYPE_UNDEPLOY, revision);
    }
  }

}
