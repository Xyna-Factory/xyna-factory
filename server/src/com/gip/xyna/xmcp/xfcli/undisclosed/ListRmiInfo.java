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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


/**
 *
 */
public class ListRmiInfo implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    /*RMIInfo rmiInfo = RMIInspection.getRMIInfo();
    if (rmiInfo == null) {
      clw.writeLineToCommandLine("Could not get rmi info");
      return;
    }
    clw.writeLineToCommandLine("Existing RMI registries and bound objects:");
    int cnt = 0;
    for (RegistryInfo ri : rmiInfo.getRegistries()) {
      clw.writeLineToCommandLine("Registry " + (++cnt) + ": " + ri.getHost() + ":" + ri.getPort());
      clw.writeLineToCommandLine("   serverSocketFactory=" + ri.getServerSocketFactory() );
      clw.writeLineToCommandLine("   clientSocketFactory=" + ri.getClientSocketFactory() );
      
      clw.writeLineToCommandLine("   Bound objects:");
      for (Entry<String, RemoteInfo> e : ri.getBoundObjects().entrySet()) {
        clw.writeLineToCommandLine("      o " + e.getKey() + " -> " + e.getValue().getHost() + ":" + e.getValue().getPort());
        String indentation = "        " + e.getKey().replaceAll(".", " ") + "    ";
        clw.writeLineToCommandLine(indentation + "serverSocketFactory=" + e.getValue().getServerSocketFactory() );
        clw.writeLineToCommandLine(indentation + "clientSocketFactory=" + e.getValue().getClientSocketFactory() );
        clw.writeLineToCommandLine(indentation + "objID+CL: " + e.getValue().getObjId() );
        clw.writeLineToCommandLine(indentation + "    loaded by " + e.getValue().getClassloader());
        clw.writeLineToCommandLine(indentation + "remoteObj: " + e.getValue().getRemote());
      }
      clw.writeLineToCommandLine("");
    }*/

  }



}
