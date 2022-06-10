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

package com.gip.xyna.coherence.coherencemachine.interconnect.rmi;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;



public class RMIConnectionClientParameters implements Serializable {

  private static final long serialVersionUID = 1L;
  private final int port;
  private InetAddress targetAddress;
  private String rmiBindingName;


  public RMIConnectionClientParameters(RMIConnectionParametersServer parameters, String rmiBindingName) {
    this.port = parameters.getPort();
    try {
      this.targetAddress = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    this.rmiBindingName = rmiBindingName;
  }


  public RMIConnectionClientParameters(int port, InetAddress targetAddress, String rmiBindingName) {
    this.port = port;
    this.targetAddress = targetAddress;
    this.rmiBindingName = rmiBindingName;
  }


  public int getPort() {
    return port;
  }


  public String getHostName() {
    return targetAddress.getHostAddress();
  }


  public String getRMIBindingName() {
    return rmiBindingName;
  }

}
