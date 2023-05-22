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
package xact.dhcp.client.impl;

import java.io.StringReader;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna.xact.tlvencoding.dhcp.ConfigFileReadException;
import com.gip.xyna.xact.tlvencoding.dhcp.Node;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTree;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTreeReader;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode;

import xact.dhcp.DHCPPacket;
import xact.dhcp.client.Lease;
import xact.dhcp.enums.DHCPMessageType;
import xact.dhcp.options.Option;
import xact.dhcp.options.Option50_RequestedIPAddress;
import xact.dhcp.options.Option51_IPAddressLeaseTime;
import xact.dhcp.options.Option54_ServerIdentifier;

public class DHCPOptionParser {

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPOptionParser.class);

  private TextConfigTree tree;
  private DHCPPacket dhcpPacket;
  
  
  public DHCPOptionParser(DHCPPacket dhcpPacket) {
    this.dhcpPacket = dhcpPacket;
    try {
      StringReader reader = new StringReader(dhcpPacket.getOptions() );
      tree = new TextConfigTreeReader(reader).read();
    }
    catch (ConfigFileReadException e) {
      logger.warn("Could not read options", e);
    }
  }

  public Option getOption50_RequestedIPAddress() {
    return new Option50_RequestedIPAddress( new IPv4( dhcpPacket.getYiaddr() ) );
  }

  public Option getOption51_IPAddressLeaseTime() {
    return new Option51_IPAddressLeaseTime(getLeaseT1());
  }

  public Option getOption54_ServerIdentifier() {
    return new Option54_ServerIdentifier( getValueFor("ServerIdentifier") );
  }

  private String getValueFor(String typename) {
    if( tree != null ) {
      for (Node no : tree.getNodes()) {
        if (no.getTypeName().equals(typename) ) {
          return ((TypeWithValueNode) no).getValue();
        }
      }
    }
    return null;
  }

  public DHCPMessageType getMessageType() {
    return MessageType.instanceFor( getValueFor("DHCPMessageType") );
  }

  public int getLeaseT1() {
    String t1 = getValueFor("LeaseTime");
    return t1 == null ? 0 : Integer.parseInt(t1);
  }

  public Lease getLease() {
    return new Lease( new IPv4( dhcpPacket.getYiaddr() ), getLeaseT1(), 0 );
  }

}
