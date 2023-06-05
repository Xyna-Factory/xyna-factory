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

package com.gip.xyna.coherence.standalone;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectCalleeRMI;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionClientParameters;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionParametersServer;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class CoherenceNodeStandalone {

  private static Logger logger = LoggerFactory.getLogger(CoherenceNodeStandalone.class);


  public static void main(String[] args) throws IOException {

    File runtoggle = new File("runtoggle");
    if (!runtoggle.exists()) {
      runtoggle.createNewFile();
    }

    int port = Integer.valueOf(args[0]);

    CoherenceNode node = new CoherenceNode(port); //FIXME nicht überall den gleichen port
    RMIConnectionParametersServer parameters = new RMIConnectionParametersServer(port);
    node.init(parameters);

    if (args.length == 1) {
      node.createNewCluster();
    } else if (args.length == 2) {
      InetAddress targetAddress = InetAddress.getByName(args[1]);
      RMIConnectionClientParameters target =
          new RMIConnectionClientParameters(port, targetAddress, InterconnectCalleeRMI.RMI_NAME);
      node.connectToClusterRMI(target);
    } else {
      System.out.println("Unexpected number of parameters: <port> [<target address>]");
      System.exit(-1);
    }

    while (runtoggle.exists()) {
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException e) {
        logger.error("Got interrupted unexpectedly", e);
        break;
      }
    }

    System.out.println("shutting down");
    node.shutdown();

  }
}
