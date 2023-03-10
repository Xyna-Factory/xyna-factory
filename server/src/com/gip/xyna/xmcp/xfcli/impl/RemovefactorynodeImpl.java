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



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Removefactorynode;



public class RemovefactorynodeImpl extends XynaCommandImplementation<Removefactorynode> {

  public void execute(OutputStream statusOutputStream, Removefactorynode payload) throws XynaException {
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    nodeMgmt.removeNode(payload.getName());
    FactoryNodeCaller caller = nodeMgmt.removeFactoryNodeCaller(payload.getName());

    if (caller != null) {
      try {
        caller.getRemoteOrderExecution().abortCommunication();
      } catch (XFMG_NodeConnectException e) {
        writeToCommandLine(statusOutputStream, "Exception during shutdown of communication with " + payload.getName() + ". " + e.getMessage());
      }
      caller.shutdown(false);
    }
  }

}
