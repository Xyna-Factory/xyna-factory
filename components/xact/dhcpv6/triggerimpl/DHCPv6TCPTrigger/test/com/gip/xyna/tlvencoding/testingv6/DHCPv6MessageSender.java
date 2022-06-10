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



import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import com.gip.xyna.xact.triggerv6.tlvencoding.databasev6.LoadConfigv6Static;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;



public class DHCPv6MessageSender {

  Collection<DHCPv6Encoding> liste; // TODO namen refactorn, damit man versteht, was das ist

  List<Node> nodes = new ArrayList<Node>();
  List<Node> subnodes = new ArrayList<Node>();
  List<Node> subsubnodes = new ArrayList<Node>();
  List<Node> subsubsubnodes = new ArrayList<Node>();
  List<Node> subsubsubsubnodes = new ArrayList<Node>();
  List<Node> subsubsubsubsubnodes = new ArrayList<Node>();

  ByteArrayOutputStream output = new ByteArrayOutputStream();

  DatagramSocket toSocket;


  public static void main(String argh[]) throws Exception {

    
//    new DHCPv6MessageSender(1);
//
//    new DHCPv6MessageSender(2);
//    new DHCPv6MessageSender(3);
//
//    new DHCPv6MessageSender(4);
//    new DHCPv6MessageSender(5);
//    new DHCPv6MessageSender(6);
//
//    new DHCPv6MessageSender(7);
//    new DHCPv6MessageSender(8);
//
    new DHCPv6MessageSender(9);
//
//    new DHCPv6MessageSender(10);
//
//    new DHCPv6MessageSender(11);
//    
//    new DHCPv6MessageSender(12);
//    
//    new DHCPv6MessageSender(13);
//      new DHCPv6MessageSender(14);
//    new DHCPv6MessageSender(15);
//    new DHCPv6MessageSender(16);
//      new DHCPv6MessageSender(17);
//      new DHCPv6MessageSender(18);

  }


  DHCPv6MessageSender(int type) throws Exception {

    // String targetoutsidev6="fe80::250:daff:fe20:b1a5";
    String targetoutside = "10.0.0.114";
    String target = "fe80::2e0:52ff:fe9a:c204";

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


    // Datenbankanbindung aufbauen und Eintraege holen
    LoadConfigv6Static anbindung = new LoadConfigv6Static();
    anbindung.setUp();

    try {
      liste = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      System.out.println("Failed to read from database");
    }

    if (liste.size() == 0) {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }

    // Datenbankliste einlesen
    DHCPv6ConfigurationEncoder test = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));


    try {
      toSocket = new DatagramSocket();
    }
    catch (Exception e) {
      System.out.println(e);
    }

    byte[] optionen;

    byte data[];


    if (type == 1)
      this.createDUIDLLTMessage();
    if (type == 2)
      this.createDUIDENMessage();
    if (type == 3)
      this.createDUIDLLMessage();
    if (type == 4)
      this.createIANAMessage();
    if (type == 5)
      this.createIAPDMessage();
    if (type == 6)
      this.createIATAMessage();
    if (type == 7)
      this.createVendorClassMessage();
    if (type == 8)
      this.createMultipleOptionsMessage();
    if (type == 9)
      this.createRelayOptionMessage();
    if (type == 10)
      this.createDNSServerMessage();
    if (type == 11)
      this.createVendorSpecificMessage();
    if (type == 12)
      this.createBugTestMessage();
    if (type == 14)
      this.createLeaseQueryMessage();
    if (type == 15)
      this.createReconfigureAcceptMessage();
    if (type == 16)
      this.createRemoteIDMessage();
    if (type == 17)
      this.createLeaseReplyMessage();
    if (type == 18)
      this.createLeaseReplyMultipleLinkMessage();
    
    
    
    if(type!=13)
    {
      test.encode(nodes, output);
      optionen = output.toByteArray();

    }
    else
    {
      //String wireshark = "075049500001000e00010001000003e80000000000000002000e00010001000003e858594e410401001900294f817c1d00000e1000001c20001a001900004e2000005208782001cafecafe80000000000000000000";
      String wireshark = "0e0080010001000a0003000100015c23163d002c003301000000000000000000000000000000000005001800000000000000000000000000000000000000000000000000060002002f";

//      String tmp = "0x"+wireshark.toUpperCase();

      wireshark = "0x0C01000102030405060708090A0B0C0D0E0F000102030405060708090A0B0C0D0E0F001100160000118B0401000401020300040200060000000000010009015E030496ED0001000A0003000100000000000100060004001100400003000C0000000100000D050000115C0011011F0000118B0001001000200021002200250026087A087B00270002000345434D0003000845434D3A454D54410004000F413641425234343433343030313635000500013000060011372E31302E3130302E4555524F2E5349500007000A322E332E3162657461330008000630303030434100090006544D39303241000A0019417272697320496E7465726163746976652C204C2E4C2E432E0023007A057801010102010303010104010105010106010107010F0801100901000A01010B01180C01010D0200400E0200100F010110040000000411010113010114010115013F1601011701011801041901041A01041B01201C01021D01081E01201F0110200110210102220101230100240100250101260200FF27010100240006001DCD70566C0010000D0000118B646F63736973332E30";
      
      //      wireshark = "0x0C01000102030405060708090A0B0C0D0E0F000102030405060708090A0B0C0D0E0F000900C5070496ED00030028000000010000177000001771000500184001CAFECAFE000000000000000000000000177000001771001100790000118B087A0010000200040A0A02DD000100040A0A02DE0020001020010000000000020000000000000222002200102001CAFEF000000000000000000000010026000400000000002500102001000000000002000000000000022200210014373244304337344336453436314130302E63666700270001000011001400000DE90001000C78796E612E6769702E636F6D";
      
      optionen = ByteUtil.toByteArray(wireshark); 
    }
    
    int msgtype=1;
    
    if(type==14)msgtype=14;
    if(type==17||type==18)msgtype=15;
    data = createDHCP6Message(msgtype, optionen);
    

    if (type == 9) {
      // byte[] link = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
      byte[] peer = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}; 

      InetAddress bla = InetAddress.getByName("fe80::219:d1ff:fe14:7b89"); // gueltige IPv6 Addresse
      byte[] link = bla.getAddress();


      data = createDHCP6RelayMessage(12, 1, link, peer, optionen);
    }

    if(type==13) data= optionen;


    this.printOpt(data);
    this.sendUDP(target, data, toSocket);
    this.sendUDP(targetoutside, data, toSocket);
    System.out.println("=========================");

  }


  public void createLeaseReplyMultipleLinkMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();




    subnodes.clear();
    subsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x333333333333"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));

    subnodes.clear();
    subsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x112233445566"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ServerID", subnodes));


    this.nodes.add(new TypeWithValueNode("ClientLink","0x1111222233334444555566667777888822222222222222222222222222222222"));
    
  }


  public void createLeaseReplyMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();



    this.subsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));

    
    this.subnodes.add(new TypeOnlyNode("IA_Address", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();

    
    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x111111111111"));

    this.subsubnodes.add(new TypeOnlyNode("DUID-LLT", subsubsubnodes));

    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    this.subnodes.add(new TypeWithValueNode("CLTTime","600"));
    
    this.nodes.add(new TypeOnlyNode("ClientData", subnodes));

    subnodes.clear();
    subsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x333333333333"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));

    subnodes.clear();
    subsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x112233445566"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ServerID", subnodes));

    
  }


  public void printOpt(byte[] dat) {
    for (int i = 0; i < dat.length; i++) {
      System.out.println(dat[i]);
    }

  }


  public void sendUDP(String targetaddress, byte[] payload, DatagramSocket toSocket) {
    try {
      InetAddress ia;
      ia = InetAddress.getByName(targetaddress);
      int port = 1547;

      DatagramPacket packet = new DatagramPacket(payload, payload.length, ia, port);
      // DatagramSocket toSocket = new DatagramSocket();
      System.out.println("Sende Nachricht an " + targetaddress + " ...");

      toSocket.send(packet);
    }
    catch (Exception e) {
      System.out.println(e);
    }

  }

  
  public void createBugTestMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();


    this.subnodes.add(new TypeWithValueNode("InnerType", "2"));
    this.subnodes.add(new TypeWithValueNode("TXID", "0xAABBCC"));


    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();
      
    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));

    this.subsubnodes.add(new TypeOnlyNode("DUID-LLT", subsubsubnodes));


    this.subnodes.add(new TypeOnlyNode("ServerID", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();
    
    
    this.subsubnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));
    this.subsubsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubnodes.add(new TypeOnlyNode("IA_Address", subsubsubnodes));


    this.subnodes.add(new TypeOnlyNode("IA_NA", subsubnodes));


    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();
    
    this.subnodes.add(new TypeWithValueNode("DNSServer","0:1:2:3:4:5:6:7"));

    // PROBLEM wenn InnerType 2 mal vorkommt
    //this.subnodes.add(new TypeWithValueNode("InnerType","2"));

    
    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubsubnodes.add(new TypeWithValueNode("T2", "4444"));
    this.subsubsubnodes.add(new TypeWithValueNode("PrefixLength", "16"));
    this.subsubsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));



    this.subsubnodes.add(new TypeOnlyNode("IAPrefix", subsubsubnodes));


    this.subnodes.add(new TypeOnlyNode("IA_PD", subsubnodes));
    
    
    
    
    this.nodes.add(new TypeOnlyNode("RelayMessage", subnodes));

    this.nodes.add(new TypeWithValueNode("InterfaceID", "0xAABBCC"));
    

    
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();
    
    
    
  }

  
  

  public void createRelayOptionMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();


    this.subnodes.add(new TypeWithValueNode("InnerType", "1"));
    this.subnodes.add(new TypeWithValueNode("TXID", "0xAABBCC"));


    this.subsubnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubsubnodes));
    this.subsubnodes.add(new TypeOnlyNode("ClientID", subsubsubnodes));

    this.subnodes.add(new TypeOnlyNode("IA_NA", subsubnodes));

    subsubsubnodes.clear();
    subsubnodes.clear();
    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();

    
    this.subsubnodes.add(new TypeWithValueNode("CL_OPTION_SOFTWARE_VERSION_NUMBER", "0x00"));

    
    this.subsubsubnodes.add(new TypeWithValueNode("PrimaryServerv4Address", "192.168.2.1"));
    this.subsubsubnodes.add(new TypeWithValueNode("SecondaryServerv4Address", "192.168.3.1"));
    
    this.subsubnodes.add(new TypeOnlyNode("CL_OPTION_CCC", subsubsubnodes));

    subsubsubnodes.clear();
    this.subsubsubnodes.add(new TypeWithValueNode("PrimaryServerv6SelectorID", "0xFFFFFFFF"));
    //this.subsubsubnodes.add(new TypeWithValueNode("SecondaryServerv6SelectorID", "0x00000000000000000000000000000000"));
    this.subsubsubnodes.add(new TypeWithValueNode("ServiceProviderProvisioningServerAddress", "0x687474703A2F2F746573742E6D652E636F6D"));
    this.subsubsubnodes.add(new TypeWithValueNode("ServiceProviderKerberosRealmName", "0x06485942524944013200"));
    
    this.subsubnodes.add(new TypeOnlyNode("CL_OPTION_CCCV6", subsubsubnodes));

    
    this.subnodes.add(new TypeOnlyNode("VendorSpecificInformation4491", subsubnodes));
    
    
    subsubnodes.clear();
    
    this.subsubnodes.add(new TypeWithValueNode("ACS-URL", "http://www.example.com:7547/live/CPEManager/CPEs/genericTR69"));
    
    this.subnodes.add(new TypeOnlyNode("VendorSpecificInformation3561", subsubnodes));
    
    this.subnodes.add(new TypeWithValueNode("VendorClass4491","0xAABBCC"));
    
    this.subnodes.add(new TypeWithValueNode("FQDN", "0xAA"));

    subsubsubnodes.clear();
    subsubnodes.clear();
    
    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("RelayID", subsubnodes));

//    this.subnodes.add(new TypeWithValueNode("Option1014711","0xCCBBAA"));
//    this.subnodes.add(new TypeWithValueNode("Option1014712","0xFFEEDD"));

    subsubnodes.clear();
//    this.subsubnodes.add(new TypeWithValueNode("Testoption1","0xAAAAAA"));
    this.subnodes.add(new TypeOnlyNode("VendorClass872", subsubnodes));

    
    
    this.nodes.add(new TypeOnlyNode("RelayMessage", subnodes));

    this.nodes.add(new TypeWithValueNode("InterfaceID", "0xAABBCC"));
    

    
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();
    
    
    this.subnodes.add(new TypeWithValueNode("CL_OPTION_SOFTWARE_VERSION_NUMBER", "0x00"));

    
    this.subsubnodes.add(new TypeWithValueNode("PrimaryServerv4Address", "192.168.2.1"));
    this.subsubnodes.add(new TypeWithValueNode("SecondaryServerv4Address", "192.168.3.1"));
    
    this.subnodes.add(new TypeOnlyNode("CL_OPTION_CCC", subsubnodes));

    subsubnodes.clear();
    this.subsubnodes.add(new TypeWithValueNode("PrimaryServerv6SelectorID", "0xFFFFFFFF"));
    //this.subsubsubnodes.add(new TypeWithValueNode("SecondaryServerv6SelectorID", "0x00000000000000000000000000000000"));
    this.subsubnodes.add(new TypeWithValueNode("ServiceProviderProvisioningServerAddress", "0x006769702E636F6D"));
    this.subsubnodes.add(new TypeWithValueNode("ServiceProviderKerberosRealmName", "0x06485942524944013200"));
    
    this.subnodes.add(new TypeOnlyNode("CL_OPTION_CCCV6", subsubnodes));

    
    this.nodes.add(new TypeOnlyNode("VendorSpecificInformation4491", subnodes));
    
    
    subnodes.clear();
    
    this.subnodes.add(new TypeWithValueNode("ACS-URL", "http://test.me.com"));
    
//    this.nodes.add(new TypeOnlyNode("VendorSpecificInformation3561", subnodes));
    
    this.nodes.add(new TypeWithValueNode("VendorClass4491","0xAABBCC"));
    
    
    
    
//    this.nodes.add(new TypeWithValueNode("Option1014711","0xCCBBAA"));
//    this.nodes.add(new TypeWithValueNode("Option1014712","0xFFEEDD"));
    
    subnodes.clear();
    
//    this.nodes.add(new TypeOnlyNode("VendorSpecificInformation5000", subnodes));
//
//    this.subnodes.add(new TypeWithValueNode("Testoption1","0xAAAAAA"));
    this.nodes.add(new TypeOnlyNode("VendorClass872", subnodes));

    subnodes.clear();

//    this.subnodes.add(new TypeWithValueNode("Testoption1","0xAAAAAA"));
//    this.nodes.add(new TypeOnlyNode("VendorClass4713", subnodes));

    
    
    
    
    
    //this.nodes.add(new TypeWithValueNode("VendorSpecificInformation4711","0xAAAAAA"));
    

    
    
    
  }


  public void createDNSServerMessage() {
    nodes.clear();
    subnodes.clear();


    this.nodes.add(new TypeWithValueNode("DNSServer", "0:1:2:3:4:5:6:7,8:9:10:11:12:13:14:15"));


  }

  public void createReconfigureAcceptMessage() {
    nodes.clear();
    subnodes.clear();


    this.nodes.add(new TypeOnlyNode("ReconfigureAccept", subnodes));


  }

  public void createLeaseQueryMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();

    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x333333333333"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));

    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));


    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();

    
    this.subnodes.add(new TypeWithValueNode("QueryType","1"));
    
    this.subnodes.add(new TypeWithValueNode("QueryLinkAddress","0x00000000000000000000000000000000"));

    this.subsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "0"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "0"));

    
    this.subnodes.add(new TypeOnlyNode("IA_Address", subsubnodes));
    
    this.subnodes.add(new TypeWithValueNode("RequestList","0x002F"));
    
    this.nodes.add(new TypeOnlyNode("LeaseQuery", subnodes));



    
    
  }

  
  
  public void createVendorSpecificMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();

    
    subnodes.add(new TypeWithValueNode("CL_OPTION_ORO","0x00200021002200250026087A087B0027"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_DEVICE_TYPE","0x45434D"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_EMBEDDED_COMPONENTS_LIST","0x45434D3A454D5441"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_DEVICE_SERIAL_NUMBER","0x413641425234343433343030313635"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_HARDWARE_VERSION_NUMBER","0x30"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_SOFTWARE_VERSION_NUMBER","0x372E31302E3130302E4555524F2E534950"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_BOOT_ROM_VERSION","0x322E332E316265746133"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_VENDOR_OUI","0x303030304341"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_MODEL_NUMBER","0x544D39303241"));
    subnodes.add(new TypeWithValueNode("CL_OPTION_VENDOR_NAME","0x417272697320496E7465726163746976652C204C2E4C2E432E"));
    
    subnodes.add(new TypeWithValueNode("CL_OPTION_MODEM_CAPABILITIES","0x"+"057801010102010303010104010105010106010107010f0801100901000a01010b01180c01010d0200400e0200100f010110040000000411010113010114010115013f1601011701011801041901041a01041b01201c01021d01081e01201f0110200110210102220101230100240100250101260200ff270101".toUpperCase()));
    subnodes.add(new TypeWithValueNode("CL_OPTION_DEVICE_ID","00:1D:CD:70:56:6C"));
    subnodes.add(new TypeWithValueNode("CL_CM_MAC_ADDR","00:1D:CD:70:56:6C"));

    
    
    
    this.subsubnodes.add(new TypeWithValueNode("PrimaryServerv4Address", "192.168.2.1"));
    this.subsubnodes.add(new TypeWithValueNode("SecondaryServerv4Address", "192.168.3.1"));
    
    this.subnodes.add(new TypeOnlyNode("CL_OPTION_CCC", subsubnodes));

    subsubnodes.clear();
    this.subsubnodes.add(new TypeWithValueNode("PrimaryServerv6SelectorID", "0x00"));
    this.subsubnodes.add(new TypeWithValueNode("SecondaryServerv6SelectorID", "0x01"));
    this.subsubnodes.add(new TypeWithValueNode("ServiceProviderProvisioningServerAddress", "0x03"));
    this.subsubnodes.add(new TypeWithValueNode("ServiceProviderKerberosRealmName", "0x04"));
    
    this.subnodes.add(new TypeOnlyNode("CL_OPTION_CCCV6", subsubnodes));

    
    
    
    this.nodes.add(new TypeOnlyNode("VendorSpecificInformation4491", subnodes));
    
    
    subnodes.clear();
    
    this.subnodes.add(new TypeWithValueNode("ACS-URL", "http://test.me.com"));
    
    this.nodes.add(new TypeOnlyNode("VendorSpecificInformation3561", subnodes));
    
    
  }


  public void createVendorClassMessage() {
    nodes.clear();
    subnodes.clear();


    this.nodes.add(new TypeWithValueNode("VendorClass4491", "0x646F63736973332E3001"));

    this.nodes.add(new TypeOnlyNode("VendorClass872", subnodes));
    this.nodes.add(new TypeWithValueNode("VendorClass311", "0x0000013700084D53465420352E30"));
    
    this.nodes.add(new TypeWithValueNode("VendorClass4491", "Hallo"));
    this.nodes.add(new TypeWithValueNode("VendorClass4491", "\"Hallo\""));
    


  }
  
  public void createRemoteIDMessage() {
    nodes.clear();
    subnodes.clear();


    this.nodes.add(new TypeWithValueNode("RemoteID", "0x00015C23FB0110000122"));
    


  }
  


  public void createMultipleOptionsMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("EnterpriseNr", "4491"));
    this.subsubnodes.add(new TypeWithValueNode("Identifier", "0x0A"));

    this.subnodes.add(new TypeOnlyNode("DUID-EN", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));

    subnodes.clear();
    subsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ServerID", subnodes));


  }


  public void createDUIDENMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("EnterpriseNr", "4491"));
    this.subsubnodes.add(new TypeWithValueNode("Identifier", "0x0A"));

    this.subnodes.add(new TypeOnlyNode("DUID-EN", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));
  }


  public void createDUIDLLTMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));

    this.subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));
  }


  public void createDUIDLLMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));

    this.subnodes.add(new TypeOnlyNode("DUID-LL", subsubnodes));


    this.nodes.add(new TypeOnlyNode("ClientID", subnodes));
  }


  public void createIANAMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();


    this.subnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));
    this.subnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subnodes.add(new TypeWithValueNode("T2", "4444"));

    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubsubnodes));
    this.subsubnodes.add(new TypeOnlyNode("ClientID", subsubsubnodes));

    this.subnodes.add(new TypeOnlyNode("IA_Address", subsubnodes));


    this.nodes.add(new TypeOnlyNode("IA_NA", subnodes));
  }


  public void createIAPDMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();

    this.subnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));
    this.subnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));
    this.subsubnodes.add(new TypeWithValueNode("PrefixLength", "16"));
    this.subsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));

    this.subsubsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubsubnodes));
    this.subsubnodes.add(new TypeOnlyNode("ClientID", subsubsubnodes));


    this.subnodes.add(new TypeOnlyNode("IAPrefix", subsubnodes));


    subsubnodes.clear();
    subsubsubnodes.clear();

    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    this.nodes.add(new TypeOnlyNode("IA_PD", subnodes));
  }


  public void createIATAMessage() {
    nodes.clear();
    subnodes.clear();
    subsubnodes.clear();
    subsubsubnodes.clear();


    this.subnodes.add(new TypeWithValueNode("IAID", "0x0000000A"));

    this.subsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    this.subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    subsubnodes.clear();
    subsubsubnodes.clear();
    subsubsubsubnodes.clear();


    this.subsubnodes.add(new TypeWithValueNode("IPv6", "0:1:2:3:4:5:6:7"));
    this.subsubnodes.add(new TypeWithValueNode("T1", "3333"));
    this.subsubnodes.add(new TypeWithValueNode("T2", "4444"));


    this.subsubsubsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    this.subsubsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", "0x0A2233445566"));
    this.subsubsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubsubnodes));
    this.subsubnodes.add(new TypeOnlyNode("ClientID", subsubsubnodes));

    this.subnodes.add(new TypeOnlyNode("IA_Address", subsubnodes));


    this.nodes.add(new TypeOnlyNode("IA_TA", subnodes));
  }


  public byte[] createDHCP6Message(int type, byte[] options) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    message.write(type); // DHCPv6 Nachrichtentyp
    message.write(1); // TransactionID 3 Byte
    message.write(2);
    message.write(3);

    // Uebergebene Optionen anhaengen

    try {
      message.write(options);
    }
    catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    byte[] data = message.toByteArray();
    return data;
  }


  public byte[] createDHCP6RelayMessage(int type, int hops, byte[] link, byte[] peer, byte[] options) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    message.write(type); // DHCPv6 Nachrichtentyp
    message.write(hops);


    try {
      message.write(link);
      message.write(peer);

      // Uebergebene Optionen anhaengen
      message.write(options);

    }
    catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    byte[] data = message.toByteArray();
    return data;
  }


}
