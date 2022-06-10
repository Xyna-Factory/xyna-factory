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



import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;

import xdnc.dhcpv6.DHCPv6ServicesImpl;
import xdnc.dhcpv6.cli.generated.Getdhcpclusterstate;



public class GetdhcpclusterstateImpl extends XynaCommandImplementation<Getdhcpclusterstate> {

  public void execute(OutputStream statusOutputStream, Getdhcpclusterstate payload) throws XynaException {
    writeLineToCommandLine(statusOutputStream, "Current DHCPv6 Cluster State = "
        + DHCPv6ServicesImpl.clusterManagement.getClusterMgmt().getCurrentState());
  }

}
