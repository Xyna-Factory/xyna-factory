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
package com.gip.xyna.xact.trigger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.ByteUtils;
import com.gip.xyna.xact.dhcp.RawBOOTPPacket;
import com.gip.xyna.xact.tlvdecoding.dhcp.DHCPConfigurationDecoder;
import com.gip.xyna.xact.tlvdecoding.dhcp.DecoderException;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

import xact.dhcp.client.DHCPUtils;

public class DHCPClientTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPClientTriggerConnection.class);

  private final DatagramPacket rawPacket;
  
  private DHCPClientStartParameter parameter;
  private DHCPConfigurationDecoder decoder;

  private RawBOOTPPacket packet;


  public DHCPClientTriggerConnection(DatagramPacket packet, DHCPConfigurationDecoder decoder,
      DHCPClientStartParameter parameter) {
    this.rawPacket = packet;
    this.parameter = parameter;
    this.decoder = decoder;
  }
  
  public void parseDhcpPacket() throws IOException {

    byte data[] = rawPacket.getData();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

    if(logger.isDebugEnabled())logger.debug("Got Data and trying to create RawBOOTPPacket!");

    try {
      this.packet = new RawBOOTPPacket(inputStream);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Error while creating RawBOOTPPacket!", e);
    }
  }
  
  public RawBOOTPPacket getDhcpPacket() {
    if (this.packet == null) {
      throw new IllegalStateException("BOOTP packet has not been parsed.");
    }
    return this.packet;
  }

  public DatagramPacket getRawPacket() {
    return this.rawPacket; 
  }


  public String getOrderType() {
    return parameter.getOrderType();
  }

  public String getXid() {
    return ByteUtils.toHexString(packet.getXIDRaw(), false, "");
  }

  public String getOptionsAsString() {
    try {
      return decoder.decode( DHCPUtils.extractOptions( rawPacket.getData() ) );
    }
    catch (DecoderException e) {
      logger.warn("Could not decode options", e);
      return "";
    }
  }


}
