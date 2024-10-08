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
package com.gip.xyna.xact.trigger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.tlvdecoding.dhcp.DHCPConfigurationDecoder;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;

import xact.dhcp.client.EncoderBuilder;

public class DHCPClientTrigger extends EventListener<DHCPClientTriggerConnection, DHCPClientStartParameter> {

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPClientTrigger.class);

  private InetAddress localAddress;
  private DatagramSocket datagramSocket;
  private volatile boolean isStopping = false;
  private int receiveBufferLength;
  private DHCPConfigurationDecoder decoder;
  private DHCPClientStartParameter parameter;
    
  public DHCPClientTrigger() {
  }

  public void start(DHCPClientStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {
    this.parameter = sp;
    try {
      if( sp.getAddress() == null ) {
        this.datagramSocket = new DatagramSocket(sp.getPort() );
      } else {
        this.datagramSocket = new DatagramSocket(sp.getPort(), sp.getInetAddress() );
      }
    }
    catch (Exception e) { //SocketException, UnknownHostException
      throw new XACT_TriggerCouldNotBeStartedException( e) {
        private static final long serialVersionUID = 1L;
        };
    }
    
    this.decoder = EncoderBuilder.buildDecoder();

    
    
    this.receiveBufferLength = 2576;
    this.localAddress = null; //FIXME
  }

  public DHCPClientTriggerConnection receive() {
    try {
      DatagramPacket datagramPacket = new DatagramPacket(new byte[receiveBufferLength], receiveBufferLength);
      datagramSocket.receive(datagramPacket);
      
      
      logger.warn(getClassDescription() + " received Packet from "+datagramPacket.getAddress().toString());
      
      if( logger.isDebugEnabled() ) {
        logger.debug(getClassDescription() + " received Packet from "+datagramPacket.getAddress().toString());
      }
      if (localAddress == null) {
        localAddress = datagramSocket.getLocalAddress();
      }
      return new DHCPClientTriggerConnection(datagramPacket, this.decoder, this.parameter );
      //, this.enc, replyport, relayport, datagramSocket, localAddress);
    }
    catch (IOException e) {
      if (!isStopping) {
        // TODO maybe the receive exception should be a runtime exception itself
        throw new RuntimeException("Failed to receive datagram packet", e);
      }
    }

    return null;
  }

  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, DHCPClientTriggerConnection con) {
    ignoreRequest(con);
  }

  /**
   * called by Xyna Processing to stop the Trigger.
   * should make sure, that start() may be called again directly afterwards. connection instances
   * returned by the method receive() should not be expected to work after stop() has been called.
   */
  public void stop() throws XACT_TriggerCouldNotBeStoppedException {
    isStopping = true;
    if (datagramSocket != null) {
      datagramSocket.close();
    }
  }

  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter
   * registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(DHCPClientTriggerConnection con) {
    ignoreRequest(con);
  }

  private void ignoreRequest(DHCPClientTriggerConnection con) {
    if (logger.isTraceEnabled()) {
      logger.trace("ignoring dhcp request: " + con.getDhcpPacket());
    }
  }

  
  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "DHCPv4 Client Trigger";
  }

}
