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
package xact.dhcp.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.ByteUtils;
import com.gip.xyna.xact.dhcp.RawBOOTPPacket;
import com.gip.xyna.xact.tlvdecoding.dhcp.DecoderException;
import com.gip.xyna.xact.tlvencoding.dhcp.DHCPConfigurationEncoder;
import com.gip.xyna.xact.tlvencoding.dhcp.Node;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode;

import xact.dhcp.client.DHCPSendException;
import xact.dhcp.client.DHCPServer;
import xact.dhcp.client.DHCPUtils;
import xact.dhcp.client.DHCPUtils.DHCPFields;
import xact.dhcp.client.EncoderBuilder;
import xact.dhcp.client.LinkAddress;
import xact.dhcp.client.MACAddress;
import xact.dhcp.enums.DHCPMessageType;
import xact.dhcp.options.Option;
import xact.dhcp.options.Option50_RequestedIPAddress;
import xact.dhcp.options.Option51_IPAddressLeaseTime;
import xact.dhcp.options.Option54_ServerIdentifier;
import xact.dhcp.options.Option61_ClientIdentifier;
import xact.dhcp.options.Option82_AgentInformation;

public class DHCPRequest {

  private static Random random;
  private static DatagramSocket socket;
  private static DHCPConfigurationEncoder encoder;
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPRequest.class);

  public static void onDeployment() {
    // Random generator to generate MAC addresses and XIDs
    random = new Random();
    encoder = EncoderBuilder.buildEncoder();
  }

  public static void onUndeployment() {
    if (socket != null) {
      socket.close();
    }
  }

  private MessageType messageType;
  private byte[] dhcpRequest;
  private String linkAddress;
  private String macAddress;
  private List<Node> options = new ArrayList<Node>();
  private String xid;
  private String ciaddr;
  
  public DHCPRequest(MACAddress mac, DHCPMessageType messageType, LinkAddress linkAddress) {
    this.messageType = MessageType.valueOf(messageType);
    this.macAddress = mac.getMACAddress();
    this.linkAddress = linkAddress.getLinkAddress();
    this.ciaddr = "0.0.0.0";
    addStandardOptions();
  }

  public void addStandardOptions() {
    // Option 53  DHCP message type
    options.add(new TypeWithValueNode("DHCPMessageType", messageType.getValue() ));
    
    //Option 60 vendor class identifier
    
    //Option 43 Vendor Specific Information
    
    //Option 55 Parameter Request List
    
    //Option 57 Maximum DHCP Message Size
    options.add(new TypeWithValueNode("MaximumDHCPMessageSize", "576"));
  }
 

  
  public void addOption(Option option) {
    
    if( option instanceof Option50_RequestedIPAddress ) {
      Option50_RequestedIPAddress option50 = (Option50_RequestedIPAddress)option;
      
      options.add(new TypeWithValueNode("RequestedAddress", option50.getIPAddress().getValue()));
      
      if( messageType == MessageType.REQUEST_RENEW ) {
        ciaddr = option50.getIPAddress().getValue();
      }
    }
    
    if( option instanceof Option51_IPAddressLeaseTime ) {
      Option51_IPAddressLeaseTime option51 = (Option51_IPAddressLeaseTime)option;
      options.add(new TypeWithValueNode("LeaseTime", "" + option51.getT1()));
    }
    
    if( option instanceof Option54_ServerIdentifier ) {
      Option54_ServerIdentifier option54 = (Option54_ServerIdentifier)option;
      options.add(new TypeWithValueNode("ServerIdentifier", option54.getServerIdentifier() ));
    }
    
    if( option instanceof Option61_ClientIdentifier ) {
      options.add(new TypeWithValueNode("ClientIdentifier", "0x" + macAddress.toUpperCase()));
    }
    
    if( option instanceof Option82_AgentInformation ) {
      Option82_AgentInformation option82 = (Option82_AgentInformation)option;
      List<Node> subnodes = new ArrayList<Node>();
      addNode( subnodes, "AgentCircuitID", option82.getAgentCircuitID());
      addNode( subnodes, "AgentRemoteID", option82.getAgentRemoteID());
      addNode( subnodes, "SubscriberID", option82.getSubscriberID());
      options.add(new TypeOnlyNode("AgentInformation", subnodes));
    }
    
  }
  
  
  
  private void addNode(List<Node> nodes, String name, String value) {
    if( value != null ) {
      nodes.add(new TypeWithValueNode(name, value));
    }
  }

  public byte[] createRequestInternal() throws IOException {
    int transid = random.nextInt(16777000);
    String tid = Integer.toHexString(transid);
    while (tid.length() < 8) {
      tid = "0" + tid;
    }
    this.xid = tid;
    
    // Initialize BootP fields
    byte op = 1; // Message op code / message type. 1 = BOOTREQUEST, 2 = BOOTREPLY
    byte htype = 1; // Hardware address type, see ARP section in "Assigned Numbers" RFC; e.g., '1' = 10mb ethernet
    byte hlen = 6; // Hardware address length (e.g. '6' for 10mb ethernet).
    byte hops = 1; // Client sets to zero, optionally used by relay agents when booting via a relay agent.
    String xidDec = String.valueOf(transid); // Transaction ID, a random number chosen by the client, used
    // by the client and server to associate messages and responses between a client and a server.
    int secs = 0; // Filled in by client, seconds elapsed since client began address acquisition or renewal process.
    boolean flags = false; // allways
    //this.ciaddr  // client Address
    
    String yiaddr = "0.0.0.0"; // client Address
    String siaddr = "0.0.0.0"; // next Server TFTP
    String giaddr = linkAddress; // Relay Address
    byte[] chaddr = ByteUtils.fromHexString(macAddress, 6, false);
    String sname = "";
    String file = "";
    
    RawBOOTPPacket bootpPacket = new RawBOOTPPacket(op, htype, hlen, hops, xidDec, secs, flags,
        InetAddress.getByName(ciaddr), InetAddress.getByName(yiaddr),
        InetAddress.getByName(siaddr), InetAddress.getByName(giaddr),
        chaddr, sname, file);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bootpPacket.writeToOutputStreamWithoutOptions(baos);
   
    // Magic Cookie ergaenzen vor Optionen
    byte[] mcookie = new byte[4];
    mcookie[0] = 99;
    mcookie[1] = (byte) 130;
    mcookie[2] = 83;
    mcookie[3] = 99;
    baos.write(mcookie);
    
    // Uebergebene Optionen anhaengen
    encoder.encode(options, baos);
 
    return baos.toByteArray();
  }
  
  public void createRequest() throws DHCPSendException {
    try {
      dhcpRequest = createRequestInternal();
    } catch (IOException e) {
      DHCPSendException dhcpSendException = new DHCPSendException();
      dhcpSendException.initCause(e);
      throw dhcpSendException;
    }
  }

  public void sendTo(DHCPServer dhcpServer) throws DHCPSendException {
    try {
      
      InetAddress serverAddress = InetAddress.getByName(dhcpServer.getHost());
      int port = dhcpServer.getPort() == null ? 67 :dhcpServer.getPort().intValue(); 

      logger.info("serverAddress "+serverAddress); 
      
      DatagramPacket dataPacket = new DatagramPacket(dhcpRequest, dhcpRequest.length, serverAddress, port);
    
      if (socket == null) {
        socket = new DatagramSocket(68); //FIXME
      }
      synchronized (socket) {
        socket.send(dataPacket);
      }
      
      ByteArrayInputStream inputStream = new ByteArrayInputStream(dhcpRequest);
      RawBOOTPPacket packet = new RawBOOTPPacket(inputStream);
      EnumMap<DHCPFields, Object> map = DHCPUtils.analyzePacket(packet);
      logger.info( map ); 
     
      try {
        logger.info( EncoderBuilder.buildDecoder().decode( DHCPUtils.extractOptions(dhcpRequest) ) );
      } catch (DecoderException e) {
        logger.info( "Could not decode ", e); 
      }
      
    } catch (IOException e) {
      DHCPSendException dhcpSendException = new DHCPSendException();
      dhcpSendException.initCause(e);
      throw dhcpSendException;
    }
  }

  public String getXid() {
    return xid;
  }

  
  
  
}
