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
package com.gip.xyna.coherence.coherencemachine.interconnect;



import com.gip.xyna.coherence.coherencemachine.interconnect.java.InterconnectCalleeJava;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectCalleeRMI;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectRMIClassLoader;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionParametersServer;



public class InterconnectCalleeProviderFactory {


  private static InterconnectCalleeProviderFactory instance = new InterconnectCalleeProviderFactory();
  private static InterconnectCalleeProvider providerRMI;


  private static class InterconnectCalleeProviderRMI implements InterconnectCalleeProvider {

    private final RMIConnectionParametersServer rmiParameters;
    private final InterconnectRMIClassLoader classloader;


    public InterconnectCalleeProviderRMI(RMIConnectionParametersServer rmiParameters,
                                         InterconnectRMIClassLoader classloader) {
      this.rmiParameters = rmiParameters;
      this.classloader = classloader;
    }


    public InterconnectCallee newCallee(InterconnectProtocol protocol) {
      return new InterconnectCalleeRMI(protocol, rmiParameters, classloader);
    }

  }


  private static InterconnectCalleeProvider providerJava = new InterconnectCalleeProvider() {
    public InterconnectCallee newCallee(InterconnectProtocol protocol) {
      InterconnectCalleeJava temp = new InterconnectCalleeJava(protocol);
      return temp;
    }
  };


  private InterconnectCalleeProviderFactory() {
  }


  public static InterconnectCalleeProviderFactory getInstance() {
    return instance;
  }


  public InterconnectCalleeProvider getRMIProvider(RMIConnectionParametersServer connectionParameters,
                                                   InterconnectRMIClassLoader classloader) {
    return new InterconnectCalleeProviderRMI(connectionParameters, classloader);
  }


  public InterconnectCalleeProvider getJavaProvider() {
    return providerJava;
  }

}
