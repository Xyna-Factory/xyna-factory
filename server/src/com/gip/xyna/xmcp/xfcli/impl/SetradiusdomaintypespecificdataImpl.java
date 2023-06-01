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
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.radius.PresharedKey;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Setradiusdomaintypespecificdata;



public class SetradiusdomaintypespecificdataImpl extends XynaCommandImplementation<Setradiusdomaintypespecificdata> {

  public void execute(OutputStream statusOutputStream, Setradiusdomaintypespecificdata payload) throws XynaException {

    int countRadiusServer = payload.getServers() == null ? 0 : payload.getServers().length;
    List<RADIUSServer> serverList = new ArrayList<RADIUSServer>();
    for (int i = 0; i<countRadiusServer; i++) {
      String[] serverParts = payload.getServers()[i].split(",");
      if (serverParts.length != 3) {
        throw new XMCP_INVALID_PARAMETERNUMBER("<IPv4/IPv6>,<Port>,<PresharedKey>");
      }
      String ipValue = serverParts[0];
      IP ip = IP.generateIPFromString(ipValue);
      int portValue = Integer.parseInt(serverParts[1]);
      RADIUSServerPort port = new RADIUSServerPort(portValue);
      PresharedKey key = new PresharedKey(serverParts[2]);
            
      serverList.add(new RADIUSServer(ip, port, key));
    }

    RADIUSDomainSpecificData radiusData = new RADIUSDomainSpecificData(payload.getAssociatedOrdertype(), serverList);

    if (factory.getFactoryManagementPortal().setDomainSpecificDataOfDomain(payload.getDomainName(), radiusData)) {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("Domain specific data fo domain ")
                                 .append(payload.getDomainName()).append(" succesfully set").toString());
    } else {
      writeLineToCommandLine(statusOutputStream,
                             new StringBuilder().append("Domain specific data fo domain ")
                                 .append(payload.getDomainName()).append(" could not be set").toString());
    }

  }

}
