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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import xdnc.dhcp.Node;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;


public class DHCPv6FilterTest {

  @Test
  public void testThreadSafety()
  {
    
    DatagramSocket t=null;
    try {
      t = new DatagramSocket();
//      System.out.println(t.getSendBufferSize());
//
//      t.setSendBufferSize(100);
    }
    catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    byte[] test = {1,2,-2,-128,0,0,0,0,0,0,94,-39,-104,-1,-2,-102,101,36,3,4,5,6,7,8,9,10,11,12,13,14,15,16,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
    
    DatagramPacket p = null;
    try {
      p = new DatagramPacket(test, test.length, InetAddress.getByName("10.0.10.14"), 1547);
    }
    catch (UnknownHostException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    
    
    
    DHCPv6TriggerConnection tc = new DHCPv6TriggerConnection(p, null, null, 1547, 1547, t, null);

    com.gip.xyna.xact.tlvencoding.dhcp.Node fakeclient = new com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode("ClientID",new ArrayList<com.gip.xyna.xact.tlvencoding.dhcp.Node>());
    tc.setClientId(fakeclient);
    

//    for(int i=0;i<100;i++)
//    {
//      try {
//        tc.sendUDP(InetAddress.getByName("10.0.10.14").getHostAddress(), test,false);
//      }
//      catch (UnknownHostException e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
//    }
    
      Map props = new HashMap();
      props.put("xact.dhcp.hashv6","false");
      props.put("xact.dhcp.hashv6passval","0");
      
      XynaFactory.setupAsPropertyProvider(props);


    
      DHCPv6Filter filter = new DHCPv6Filter();
      
      ArrayList<Node> relaymessageoption = new ArrayList<Node>();
      
        for(int i = 0;i<50000;i++)
        {
          Thread t1 = new Thread( new Testexecutor(filter,relaymessageoption, tc) );
          t1.setDaemon(true);
          t1.start();
        }
        
        
        
  }
  
  public class Testexecutor implements Runnable
  {
    DHCPv6Filter ownfilter;
    ArrayList<Node> relaymessageoption;
    DHCPv6TriggerConnection tc;
    
    public Testexecutor(DHCPv6Filter f,ArrayList<Node> n, DHCPv6TriggerConnection tc)
    {
      this.ownfilter = f;
      this.relaymessageoption = n;
      this.tc = tc;
    }
    
    public void run()
    {
      ownfilter.onResponse(new XynaObjectList<Node>(relaymessageoption, Node.class), tc);
    }
  }
  
  
}
