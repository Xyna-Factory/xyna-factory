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
package com.gip.xyna.coherence.management;



import java.io.Serializable;
import java.util.List;

import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProvider.ConnectionProviderType;
import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectCalleeRMI;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionClientParameters;



/**
 * Hält Informationen zu einem Cluster Knoten, z.B. was für Verbindungen kann der Knoten aufmachen, etc
 */
public class NodeInformation implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient InterconnectCalleeJava javaCallee;


  private RMIConnectionClientParameters rmiConnectionParameters;


  public NodeInformation(List<InterconnectCallee> callees) {
    //FIXME prio3: extend for RMI/other callee types
    for (InterconnectCallee callee : callees) {
      if (callee instanceof InterconnectCalleeJava) {
        javaCallee = (InterconnectCalleeJava) callee;
      } else if (callee instanceof InterconnectCalleeRMI) {
        rmiConnectionParameters = ((InterconnectCalleeRMI) callee).getClientParameters();
      }
    }
  }


  public InterconnectCalleeJava getJavaCallee() {
    return javaCallee;
  }


  public RMIConnectionClientParameters getRMIConnectionClientParameters() {
    return rmiConnectionParameters;
  }


  public ConnectionProviderType getPreferredNodeConnectionType() {
    if (javaCallee != null) {
      return ConnectionProviderType.JAVA;
    } else if (rmiConnectionParameters != null) {
      return ConnectionProviderType.RMI;
    } else {
      throw new RuntimeException("no node connection information provided.");
    }
  }


}
