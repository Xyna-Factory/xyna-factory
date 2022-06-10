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
package xdnc.dhcpv6.cli.impl;

import com.gip.xyna.xdnc.dhcp.DHCPClusterState;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;

import xdnc.dhcpv6.DHCPv6ServicesImpl;
import xdnc.dhcpv6.cli.generated.Setdhcpclusterstate;



public class SetdhcpclusterstateImpl extends XynaCommandImplementation<Setdhcpclusterstate> {

  public void execute(OutputStream statusOutputStream, Setdhcpclusterstate payload) throws XynaException {
    if (payload.getNewState().equalsIgnoreCase(DHCPClusterState.SINGLE_RUNNING.toString())) {
      DHCPv6ServicesImpl.clusterManagement.getClusterMgmt().changeToDisconnectedMaster();
      writeLineToCommandLine(statusOutputStream, "State was set successfully.");
    } else {
      writeLineToCommandLine(statusOutputStream, "State may only be set to " + DHCPClusterState.SINGLE_RUNNING);
    }
  }

}
