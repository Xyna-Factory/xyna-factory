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
package com.gip.xyna.xact.trigger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;

import org.apache.log4j.Logger;

public class UDPTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(UDPTriggerConnection.class);

  private transient DatagramPacket datagramPacket;
  private transient DatagramSocket datagramSocket;
  public DatagramPacket getDatagramPacket() {
    return datagramPacket;
}

public DatagramSocket getDatagramSocket() {
    return datagramSocket;
}

public InetAddress getLocalAddress() {
    return localAddress;
}

private InetAddress localAddress;
  
  // arbitrary constructor
  public UDPTriggerConnection() {
  }

  public UDPTriggerConnection(DatagramPacket datagramPacket, DatagramSocket datagramSocket, InetAddress localAddress) {
      this.datagramPacket = datagramPacket;
      this.datagramSocket = datagramSocket;
      this.localAddress = localAddress;
  }
  
}
