/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.coherence.coherencemachine.interconnect;



import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProvider.ConnectionProviderType;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectRMIClassLoader;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.NodeConnectionRMI;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionClientParameters;
import com.gip.xyna.coherence.management.ClusterMember;
import com.gip.xyna.coherence.management.NodeInformation;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;



public class NodeConnectionProviderFactory {

  private static NodeConnectionProviderFactory instance = new NodeConnectionProviderFactory();


  private static class JavaNodeConnectionProvider implements NodeConnectionProvider {

    private InterconnectCalleeJava callee;


    public JavaNodeConnectionProvider(InterconnectCalleeJava callee) {
      this.callee = callee;
    }


    public InterconnectProtocol createConnection() {
      return callee;
    }

  }


  private static class RMIServerConnectionProvider implements NodeConnectionProvider {

    private final RMIConnectionClientParameters clientParameters;
    private final InterconnectRMIClassLoader classloader;


    public RMIServerConnectionProvider(RMIConnectionClientParameters rmiConnectionParameters,
                                       InterconnectRMIClassLoader classloader) {
      this.clientParameters = rmiConnectionParameters;
      this.classloader = classloader;
    }


    public InterconnectProtocol createConnection() {
      try {
        return new NodeConnectionRMI(clientParameters, classloader);
      } catch (RMIConnectionFailureException e) {
        throw new RuntimeException("could not connect", e);
      }
    }

  }


  private NodeConnectionProviderFactory() {
  }


  public static NodeConnectionProviderFactory getInstance() {
    return instance;
  }


  public NodeConnectionProvider getJavaProvider(InterconnectCalleeJava interconnectCalleeJava) {
    return new JavaNodeConnectionProvider(interconnectCalleeJava);
  }


  public NodeConnectionProvider getProvider(ClusterMember member, CacheController controller) {

    NodeInformation nodeInfo = member.getNodeInformation();
    ConnectionProviderType preferredConnection = nodeInfo.getPreferredNodeConnectionType();
    switch (preferredConnection) {
      case JAVA :
        return new JavaNodeConnectionProvider(member.getNodeInformation().getJavaCallee());
      case RMI :
        return new RMIServerConnectionProvider(nodeInfo.getRMIConnectionClientParameters(),
                                               controller.getRMIClassLoader());
      default :
        throw new RuntimeException("Unexpected preferred connection type: " + preferredConnection);
    }

  }


  public NodeConnectionProvider getRMIConnectionProvider(RMIConnectionClientParameters rmiParameters,
                                                         CacheController controller) {
    return new RMIServerConnectionProvider(rmiParameters, controller.getRMIClassLoader());
  }

}
