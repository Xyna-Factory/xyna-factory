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

package com.gip.xyna.tlvencoding.testingv6;



import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.tlvdecoding.dhcp.DecoderException;
import com.gip.xyna.xact.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.tlvencoding.dhcp.ConfigFileReadException;
import com.gip.xyna.xact.tlvencoding.dhcp.Node;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTree;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTreeReader;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.databasev6.LoadConfigv6Static;



public class DHCPv6MessageReceiver {

  private byte data[];
  Collection<DHCPv6Encoding> liste;
  DatagramSocket toSocket;
  String servername = "localhost";
  int buffer = 1024;


  public static void main(String argh[]) throws DecoderException, ConfigFileReadException, IOException {
    
    
    
    new DHCPv6MessageReceiver();
  }




  public DHCPv6MessageReceiver() throws DecoderException, ConfigFileReadException, IOException {


    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();
    fexec.execAsync(EasyMock.isA(FutureExecutionTask.class));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
    public Object answer() {
        FutureExecutionTask fet = (FutureExecutionTask) EasyMock.getCurrentArguments()[0];
        if( fet.getClass().getSimpleName().equals("FutureExecutionTaskInit") ) {
          fet.execute();
        }
        return null;
    }}).anyTimes();
    EasyMock.replay(fexec);

    XynaFactory xf = EasyMock.createMock(XynaFactory.class);
    XynaFactory.setInstance(xf);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.replay(xf);

    
    LoadConfigv6Static anbindung = new LoadConfigv6Static();
    anbindung.setUp();

    try {
      liste = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      System.out.println("Failed to read from database");
      e.printStackTrace();
    }

    if (liste.size() == 0) {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }


    DHCPv6ConfigurationDecoder dec = new DHCPv6ConfigurationDecoder(new ArrayList<DHCPv6Encoding>(liste));


//    System.out.println("TMP:");
//    
//    String bla = "0x001100160000118B0401000401020300040200060000000000010009015E030496ED0001000A0003000100000000000100060004001100400003000C0000000100000D050000115C0011011F0000118B0001001000200021002200250026087A087B00270002000345434D0003000845434D3A454D54410004000F413641425234343433343030313635000500013000060011372E31302E3130302E4555524F2E5349500007000A322E332E3162657461330008000630303030434100090006544D39303241000A0019417272697320496E7465726163746976652C204C2E4C2E432E0023007A057801010102010303010104010105010106010107010F0801100901000A01010B01180C01010D0200400E0200100F010110040000000411010113010114010115013F1601011701011801041901041A01041B01201C01021D01081E01201F0110200110210102220101230100240100250101260200FF27010100240006001DCD70566C0010000D0000118B646F63736973332E30";
//    
//    byte[] test = ByteUtil.toByteArray(bla);
//    
//    System.out.println(dec.decode(test));
//      
//    
//    System.out.println("==========================");
    
    // UDP Server aufmachen
    int port = 1547;
    try {
      toSocket = new DatagramSocket(port);
    }
    catch (Exception e) {
      System.out.println(e);
    }


    while (true) {
      
      byte[] buffer = new byte[1024];

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      byte[] optarg;
      byte[] packetcopy;
      
      
      try {
        toSocket.receive(packet);
        data = packet.getData();
        packetcopy = new byte[packet.getLength()];
        //packetcopy = Arrays.copyOfRange(data, 0, packet.getLength());
        System.arraycopy(data, 0, packetcopy, 0, packet.getLength());
        data = packetcopy;
      }
      catch (Exception e) {

      }

      
      // Nachrichtentyp ausgeben
      
      
      System.out.println("==============");
      int msgtype = data[0];
      
      
      // Relayausnahme
      if(msgtype==12 || msgtype==13)
      {
        System.out.println("DHCPv6 Nachrichtentyp (Relay): "+msgtype);
        int hops = data[1];
        String linkadd = "";
        String peeradd = "";
        
        byte[] address = new byte[16];
        
        System.arraycopy(data, 2, address, 0, 16);
        //address = Arrays.copyOfRange(data,2,18);
        linkadd = InetAddress.getByAddress(address).getHostAddress();

        System.arraycopy(data, 18, address, 0, 16);
        //address = Arrays.copyOfRange(data,18,34);
        peeradd = InetAddress.getByAddress(address).getHostAddress();
        
        
        
        
        System.out.println("Hops:" +hops);
        System.out.println("LinkAddress:"+linkadd);
        System.out.println("PeerAddress:"+peeradd);
      
        // Relayteil aus Data rausloeschen
        
        //byte[] datacopy = Arrays.copyOfRange(data, 34, data.length);
        byte[] datacopy = new byte[data.length-34];
        System.arraycopy(data, 34, datacopy, 0, data.length-34);
        data = datacopy;

        List<Byte> optiondata = new ArrayList<Byte>();
        List<Byte> resultlist = new ArrayList<Byte>();

        
        
        for (int z = 0; z < data.length; z++) {
          optiondata.add(data[z]);
        }
        
        if(resultlist.size()>0) optiondata=resultlist; // falls Ende durch 0en festgestellt, wird optiondata durch Liste ohne 0en ersetzt
        
  
        // Liste in Array schreiben (geht sicher besser?)
        optarg = new byte[optiondata.size()];
        for (int z = 0; z < optiondata.size(); z++) {
          optarg[z] = optiondata.get(z);
        }
      }
      else
      {
        System.out.println("DHCPv6 Nachrichtentyp: "+msgtype);
        String transactionid = "0x"+byteToHex(data[1]) + byteToHex(data[2]) + byteToHex(data[3]);
        System.out.println("TransactionID: "+transactionid);
        
        //byte[] datacopy = Arrays.copyOfRange(data, 4, data.length);
        byte[] datacopy = new byte[data.length-4];
        System.arraycopy(data, 4, datacopy, 0, data.length-4);
        data = datacopy;
        
        
        // Kopf der Nachricht abhacken und nur Optionen behalten
        List<Byte> optiondata = new ArrayList<Byte>();
  
        
        for (int z = 0; z < data.length; z++) {
          optiondata.add(data[z]);
        }
        
        
  
        // Liste in Array schreiben (geht sicher besser?)
        optarg = new byte[optiondata.size()];
        for (int z = 0; z < optiondata.size(); z++) {
          optarg[z] = optiondata.get(z);
        }
      }

      try
      {
        String decodedData = dec.decode(optarg);
      

    
        StringBuilder builder = new StringBuilder();
        builder.append(decodedData);
        TextConfigTree  tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();
      
        List <Node> nodes = tree.getNodes();
      
        readNodes(nodes);
      }
      catch(Exception e)
      {
        System.out.println("Unbekanntes UDP Paket empfangen.");
        System.out.println(e);
      }
    
    }
  }
  
  public void readNodes(List<Node> nodes) throws UnsupportedEncodingException
  {
    System.out.println("==============");
    
    int count=0;
    boolean vnode, tnode;
    TypeWithValueNode valuenode = null;
    TypeOnlyNode typenode=null;
    for(Node n : nodes)
    {
      vnode=true;
      tnode=false;
      
      count++;
      System.out.println("Option "+count+"\n");
      System.out.println("Name: "+n.getTypeName());
    
      try
      {
        valuenode = (TypeWithValueNode)n;
      }
      catch(Exception e)
      {
        vnode=false;
        tnode=true;
        try
        {
            typenode = (TypeOnlyNode)n;
        }
        catch(Exception ee)
        {
            tnode=false;
        }
      }   
        
      if(vnode)
      {
        System.out.println("Value: "+valuenode.getValue());
        
      }
      if(n.getTypeName().contains("ParameterRequestList"))
      {
        // Ab hier parsen eines OctetStrings, um die ParameterRequestList auszulesen

        String t = "";

        if (valuenode.getValue().contains("\"")) {
          t= valuenode.getValue().substring(1, valuenode.getValue().length()-1);
          byte[] res = t.getBytes("UTF-8");

          System.out.println("Requested Parameter entsprechen folgenden Optionen:");

          for (byte r : res) {
            System.out.println(r);
          }
        }
        else if (valuenode.getValue().contains("0x")) {
          t= valuenode.getValue().substring(2, valuenode.getValue().length());

          
          if (t.length() % 2 == 0) {
            System.out.println("Requested Parameter entsprechen folgenden Optionen:");

            String sub = "";
            for (int i = 0; i < t.length(); i = i + 2) {
              sub = sub + t.charAt(i);
              sub = sub + t.charAt(i + 1);

              System.out.println(Integer.parseInt(sub, 16));
              sub = "";


            }
          }

        }

      }
      
      if(tnode)
      {
          if(typenode.getSubNodes()!=null && !typenode.getSubNodes().isEmpty())
          {
            System.out.println();
            System.out.println("Suboptionen: ");
            readNodes(typenode.getSubNodes());
          }
          
      }
      
      
      System.out.println("");
      
    }
  }

  public static String byteToHex(byte b){
    int i = b & 0xFF;
    String res = Integer.toHexString(i);
    while(res.length()<2)
    {
      res = "0"+res;
    }
    return res;
  }
  


}
