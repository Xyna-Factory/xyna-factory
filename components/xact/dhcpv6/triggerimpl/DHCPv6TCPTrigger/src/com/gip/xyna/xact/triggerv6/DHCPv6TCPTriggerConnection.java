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
package com.gip.xyna.xact.triggerv6;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

public class DHCPv6TCPTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -4844846661721673746L;

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPv6TCPTriggerConnection.class);


  private final byte[] rawPacket;
  //private RawBOOTPPacket packet;

  //private boolean parsingError = false;
  
  private DHCPv6ConfigurationDecoder dec;
  private DHCPv6ConfigurationEncoder enc;
  private int replyport;
  private int leasequeryreplyport;
  private Socket toSocket;
  private String servermacaddress;
  private Node transactionid;
  private Node clientid;
  private Node interfaceid;
  private long workflowstarttime = 0;
  private long workflowendtime = 0;
  private byte[] transactionidInBytes;


/*
  public DHCPTriggerConnection(DatagramPacket packet, DhcpOptionDefinition[] options) {
    this.rawPacket = packet;
  }

*/
  public DHCPv6TCPTriggerConnection(byte[] packet, DHCPv6ConfigurationDecoder d, DHCPv6ConfigurationEncoder e, int r, int lqr, Socket t, String mac) {
    this.rawPacket = packet;
    this.dec = d;
    this.enc = e;
    this.replyport=r;
    this.leasequeryreplyport=lqr;
    this.toSocket=t;
    this.servermacaddress = mac;
}

  public Node getTransactionId()
  {
    return this.transactionid;
  }
  
  public void setTransactionId(Node tid)
  {
    this.transactionid = tid;
  }

  public Node getClientId()
  {
    return this.clientid;
  }
  
  public void setClientId(Node cid)
  {
    this.clientid = cid;
  }

  public Node getInterfaceId()
  {
    return this.interfaceid;
  }
  
  public void setInterfaceId(Node ifid)
  {
    this.interfaceid = ifid;
  }

  
  
  public DHCPv6ConfigurationDecoder getDecoder()
  {
    return this.dec;
  }

  public DHCPv6ConfigurationEncoder getEncoder()
  {
    return this.enc;
  }

  public String getServerMacAddress()
  {
    return this.servermacaddress;
  }

  public byte[] getRawPacket()
  {
    return this.rawPacket; 
  }
  
  private String getClientMac()
  {
    String result="";
    if(this.clientid!=null)
    {
      List<Node> suboptions = ((TypeOnlyNode)this.clientid).getSubNodes();
      for(Node n:suboptions)
      {
        if(n.getTypeName().equals("DUID-LL") || n.getTypeName().equals("DUID-LLT"))
        {
          List<Node> subsuboptions=((TypeOnlyNode)n).getSubNodes();
          for(Node sn:subsuboptions)
          {
            if(sn.getTypeName().equals("LinkLayerAddress"))
            {
              result=((TypeWithValueNode)sn).getValue();
            }
          }
        }
      }
      
    }
    return result;
  }
  
  
  public void sendTCP(byte[] payload, boolean closesocket)
  {
    try
    {
      
      int port = replyport;
      
     
      
      //DatagramPacket packet = new DatagramPacket( payload, payload.length, ia, port );
      DataOutputStream output = new DataOutputStream(toSocket.getOutputStream());
      //DatagramSocket toSocket = new DatagramSocket(replyport);
      if(logger.isDebugEnabled())
      {
        logger.debug("("+this.getClientMac()+") Sending TCP message via port "+port);
        logger.debug("("+this.getClientMac()+") Packet Payloadlength: "+payload.length);
        String result = "0x";
        for(int i=0;i<payload.length;i++)
        {
          String hex = Integer.toHexString(((payload[i]+256)%256));
          if(hex.length()<2) hex = "0"+hex;
          result = result + hex.toUpperCase();
        }
        logger.debug("Packet Payload ("+this.getClientMac()+"): "+result);
      }
      
      output.write(payload);
      output.flush();
      if(closesocket)toSocket.close();
    }
    catch (Exception e)
    {
      logger.warn("Error when sending TCP Packet: "+e);
    }

  }
  


  protected void handleInvalidPacket() throws IOException {
    // TODO do something? counter? logging?
  }

  public void setWorkflowstarttime(long workflowstarttime) {
    this.workflowstarttime = workflowstarttime;
  }


  public void setWorkflowendtime(long workflowendtime) {
    this.workflowendtime = workflowendtime;
  }

  public long getWorkflowTotalTime()
  {
    if(this.workflowstarttime!=0 && this.workflowendtime!=0)
    {
      return (this.workflowendtime-this.workflowstarttime);
    }
    else return 0;
  }

  public void setTransactionidInBytes(byte[] transactionidInBytes) {
    this.transactionidInBytes = transactionidInBytes;
  }

  public byte[] getTransactionidInBytes() {
    return transactionidInBytes;
  }

}
