/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusConfigurationDecoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusConfigurationEncoder;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;



public class XynaRadiusTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -4844846661721673746L;

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusTriggerConnection.class);


  private final DatagramPacket rawPacket;


  private RadiusConfigurationDecoder dec;
  private RadiusConfigurationEncoder enc;
  private DatagramSocket toSocket;
  private InetAddress localAddress;


  public XynaRadiusTriggerConnection(DatagramPacket packet, RadiusConfigurationDecoder d, RadiusConfigurationEncoder e, DatagramSocket t,
                                     InetAddress localAddress) {
    this.rawPacket = packet;
    this.dec = d;
    this.enc = e;
    this.toSocket = t;
    this.localAddress = localAddress;
  }


  public RadiusConfigurationDecoder getDecoder() {
    return this.dec;
  }


  public RadiusConfigurationEncoder getEncoder() {
    return this.enc;
  }


  public DatagramPacket getRawPacket() {
    return this.rawPacket;
  }


  public void sendUDP(String targetaddress, byte[] payload) {
    try {
      InetAddress ia;
      ia = InetAddress.getByName(targetaddress);

      DatagramPacket packet = new DatagramPacket(payload, payload.length, ia, rawPacket.getPort());
      if (logger.isDebugEnabled()) {
        logger.debug("Sending Message to " + targetaddress + " Port " + rawPacket.getPort() + " ...");
      }
      toSocket.send(packet);
    } catch (Exception e) {
      logger.warn("", e);
    }

  }


  public String getServerIP() {
    if (logger.isDebugEnabled())
      logger.debug("getServerIP : " + localAddress);
    return localAddress.getHostAddress();
  }


  protected void handleInvalidPacket() throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("invalid packet received");
    }
  }


}
