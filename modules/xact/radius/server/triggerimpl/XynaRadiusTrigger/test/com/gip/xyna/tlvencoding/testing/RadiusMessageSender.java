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

package com.gip.xyna.tlvencoding.testing;



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
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xact.trigger.tlvencoding.database.LoadConfigStatic;
import com.gip.xyna.xact.trigger.tlvencoding.radius.Node;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusConfigurationEncoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeOnlyNode;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeWithValueNode;
import com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil;



public class RadiusMessageSender {


  int packetcounter = 0;

  Collection<RadiusEncoding> radiustlvs;

  List<Node> nodes = new ArrayList<Node>();
  List<Node> subnodes = new ArrayList<Node>();

  ByteArrayOutputStream output = new ByteArrayOutputStream();

  DatagramSocket toSocket;

  RadiusConfigurationEncoder enc;

  String target = "localhost";


  RadiusMessageSender(int type) throws Exception {

    FutureExecution fexec = EasyMock.createMock(FutureExecution.class);
    EasyMock.expect(fexec.nextId()).andReturn(1).anyTimes();
    fexec.execAsync(EasyMock.isA(FutureExecutionTask.class));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

      public Object answer() {
        FutureExecutionTask fet = (FutureExecutionTask) EasyMock.getCurrentArguments()[0];
        if (fet.getClass().getSimpleName().equals("FutureExecutionTaskInit")) {
          fet.execute();
        }
        return null;
      }
    }).anyTimes();
    EasyMock.replay(fexec);

    XynaFactoryBase xf = EasyMock.createMock(XynaFactoryBase.class);
    XynaFactory.setInstance(xf);
    EasyMock.expect(xf.getFutureExecution()).andReturn(fexec).anyTimes();
    EasyMock.expect(xf.getFutureExecutionForInit()).andReturn(fexec).anyTimes();
    EasyMock.replay(xf);

    // Datenbankanbindung aufbauen und Eintraege holen
    LoadConfigStatic anbindung = new LoadConfigStatic();
    anbindung.setUp();

    try {
      radiustlvs = anbindung.loadRadiusEntries();
    } catch (Exception e) {
      System.out.println("Failed to read from database");
    }

    if (radiustlvs.size() == 0) {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }

    try {
      toSocket = new DatagramSocket();
    } catch (Exception e) {
      System.out.println(e);
    }

    if (type == 1)
      this.createAccessRequest();
    if (type == 2)
      this.createAccessAccept();
    if (type == 3)
      this.createAccessReject();

    // Datenbankliste einlesen
    enc = new RadiusConfigurationEncoder(new ArrayList<RadiusEncoding>(radiustlvs));

    if (!(type == 4))
      sendPacket();

    if (type == 4) //Radius Testsequenz verschicken (Windows XP Client))
    {
      sendTestForWindowsXpClient();
    }

  }


  public void sendTestForWindowsXpClient() throws Exception {

    String id = "36";
    String state = "";
    String eapmessage = transformToByteString("0202000a01636973636f");
    String messageauthenticator = transformToByteString("3a67b92393a6d8a888536cc9cbe825e5");
    String authenticator = transformToByteString("6a2f5e2b287993f84865bfbf8d9b6252");
    //identifier,EAPMessage,MessageAuthenticator,State
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "37";
    state = transformToByteString("e498e1911812df0c1fbdda0b0a84c1a1");
    eapmessage = transformToByteString("020300060319");
    messageauthenticator = transformToByteString("5e9460be4130f805b3529d18b9715f90");
    authenticator = transformToByteString("91b5d09209f26c6f079d1648f234d8eb");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "38";
    eapmessage =
        transformToByteString("0204005719800000004d160301004801000044030151e7a6849890030d74088318094054c274d6bbb4ae07612dfe047c0a85e142b000001600040005000a0009006400620003000600130012006301000005ff01000100");
    messageauthenticator = transformToByteString("0cfcea6e1646bf4cc9d389fceb86712e");
    authenticator = transformToByteString("144c382af96a93aa81c6bbea506a1ba2");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "39";
    eapmessage = transformToByteString("020500061900");
    messageauthenticator = transformToByteString("39bff17ed9098352d229163f4436ac53");
    authenticator = transformToByteString("50d16eade5cc2ba920aaabb86c2b523b");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "40";
    eapmessage =
        transformToByteString("02060140198000000136160301010610000102010048bd23d52ebe3b15fd07ca4a6880af7f4177f3f048e58172990fc05c5a2189b0d9014ef8ae2cd59db01eb9b80da6e3ac1ba2fed9b2880e1f8905da570b77c90bf1a75a32bf44cde500cf50f7ba7f4e672d34007a8ca7dd27472ab4d9f4b9f971b410219dcbadfb903dfcdebfc89517818ebbc2da63bc251f764f8ce5b47663fd30d0471f408c755b7b461be5ff36b89f06386f81c89ecdf26ac6e599ba21cb49213664beac6f994eba2add991c3deb4d5ae426876c5534e68292608884f689deea36470b4c4f2f2816093abf3901547d5edf123dd243ad450b3873389e7fc7d892f1919b693e8a728e8b22979e202d257d07f89e574697acd42c4200a49987b514030100010116030100200526e91e700ab177ece9be0a9675f5b6258b5df359c18e1823857a2dc9dc9e59");
    messageauthenticator = transformToByteString("173c66333b155c49d1b25d054bcb4ef2");
    authenticator = transformToByteString("9aa761f376897e3684bbf163695ae7df");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "41";
    eapmessage = transformToByteString("020700061900");
    messageauthenticator = transformToByteString("778c02b3eb66db03a9c849dfaf54c1ef");
    authenticator = transformToByteString("94cdc906e7e831f5e0026381eb4852ac");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "42";
    eapmessage = transformToByteString("0208002119001703010016af794eb5fc44ac0166260afa65339fdf87ed6436958f");
    messageauthenticator = transformToByteString("021e0502ba79adc44c9a43390ab608b6");
    authenticator = transformToByteString("2051585ae612fdefe15b7ae8f3cc6231");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "43";
    eapmessage =
        transformToByteString("020900571900170301004cbf16baadb12664329f3c0a5729c6bfeb709f70940f27d058f2726dbc9f0b0757291923d97b91c2b86289332e015fe413df09ee51403e0e0318c3b2a33302a820603a0d078f8b66a4983b9221");
    messageauthenticator = transformToByteString("6382499b93710f41ac576f180f97f866");
    authenticator = transformToByteString("b8f2d2e0447291484653cf70d220bb38");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "44";
    eapmessage = transformToByteString("020a001d190017030100128151b10f3d9492f32ca5e0ec6944d99be0bf");
    messageauthenticator = transformToByteString("30e751eaac78a4a48fd04836232c32b6");
    authenticator = transformToByteString("8a839029d8b280725abd4962d0446638");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

    id = "45";
    eapmessage = transformToByteString("020b00261900170301001b63fe0591ea09674cf233e6429ee0edab1f72266e90ba7e6d3df7e0");
    messageauthenticator = transformToByteString("c6b3c9bfc1ac71bb5f2f84b7ee28a7cf");
    authenticator = transformToByteString("fdc4680dd8025b2c6af00a3dd61b24c1");
    this.createAccessRequestEAP(id, authenticator, eapmessage, messageauthenticator, state);
    sendPacket();

  }


  public String transformToByteString(String input) {
    if (input.startsWith("0x"))
      return input;

    return "0x" + input.toUpperCase();

  }


  public void sendPacket() throws Exception {
    int code = -1;
    int id = -1;
    byte[] authenticator = null;
    byte[] optionen;
    byte data[];

    TypeWithValueNode codenode = null;
    TypeWithValueNode identifiernode = null;
    TypeWithValueNode authenticatornode = null;

    for (Node n : nodes) {
      if (n.getTypeName().equalsIgnoreCase("code")) {
        codenode = (TypeWithValueNode) n;
      }
      if (n.getTypeName().equalsIgnoreCase("identifier")) {
        identifiernode = (TypeWithValueNode) n;
      }
      if (n.getTypeName().equalsIgnoreCase("authenticator")) {
        authenticatornode = (TypeWithValueNode) n;
      }
    }

    if (codenode != null && identifiernode != null && authenticatornode != null) {
      code = Integer.parseInt(codenode.getValue());
      nodes.remove(codenode);

      id = Integer.parseInt(identifiernode.getValue());
      nodes.remove(identifiernode);

      authenticator = ByteUtil.toByteArray(authenticatornode.getValue());
      nodes.remove(authenticatornode);

    } else {
      throw new RuntimeException("Missing parameters code and identifier!");
    }

    output = new ByteArrayOutputStream();
    enc.encode(nodes, output);
    optionen = output.toByteArray();
    data = createRadiusMessage(code, id, optionen, authenticator);

    this.sendUDP(target, data, toSocket);
  }


  public void printOpt(byte[] dat) {
    for (int i = 236; i < dat.length; i++) {
      System.out.println(dat[i]);
    }

  }


  public void sendUDP(String targetaddress, byte[] payload, DatagramSocket toSocket) {
    packetcounter++;
    try {
      InetAddress ia;
      ia = InetAddress.getByName(targetaddress);
      int port = 1812;

      DatagramPacket packet = new DatagramPacket(payload, payload.length, ia, port);
      System.out.println("[Paket " + packetcounter + "] Sende Nachricht an " + targetaddress + " ...");

      toSocket.send(packet);
    } catch (Exception e) {
      System.out.println(e);
    }

    try {
      byte[] buffer = new byte[4096];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      System.out.println("[Paket " + packetcounter + "] Warte auf Antwort vom Server ...");
      toSocket.receive(packet);
      System.out.println("[Paket " + packetcounter + "] Paket erhalten von " + packet.getAddress().getHostAddress());
    } catch (Exception e) {
      System.out.println("Problems receiving: " + e);
    }

  }


  public void createAccessRequest() {
    nodes.clear();
    subnodes.clear();

    this.nodes.add(new TypeWithValueNode("Code", "1"));
    this.nodes.add(new TypeWithValueNode("Identifier", "0"));
    this.nodes.add(new TypeWithValueNode("Authenticator", "0x0F403F9473978057BD83D5CB98F4227A"));

    this.nodes.add(new TypeWithValueNode("User-Name", "nemo"));
    this.nodes.add(new TypeWithValueNode("User-Password", "0x0DBE708D93D413CE3196E43F782A0AEE"));
    this.nodes.add(new TypeWithValueNode("NAS-IP", "192.168.1.16"));
    this.nodes.add(new TypeWithValueNode("NAS-Port", "3"));

  }


  public void createAccessAccept() {
    nodes.clear();
  }


  public void createAccessReject() {
    nodes.clear();
  }


  public void createAccessRequestEAP(String identifier, String authenticator, String EAPMessage, String messageAuthenticator,
                                     String state) {
    nodes.clear();
    subnodes.clear();

    this.nodes.add(new TypeWithValueNode("Code", "1"));
    this.nodes.add(new TypeWithValueNode("Identifier", identifier));
    this.nodes.add(new TypeWithValueNode("Authenticator", authenticator));

    this.nodes.add(new TypeWithValueNode("Calling-Station-Id", "0x" + "30303a31383a64653a36343a62333a3433".toUpperCase()));
    this.nodes
        .add(new TypeWithValueNode("Called-Station-Id", "0x" + "38383a37353a35363a34643a39323a66303a536d617274416363657373".toUpperCase()));
    this.nodes.add(new TypeWithValueNode("NAS-Port", "2"));

    this.nodes.add(new TypeOnlyNode("Vendor-Specific9", subnodes));

    this.nodes.add(new TypeWithValueNode("NAS-IP", "127.0.0.1"));
    this.nodes.add(new TypeWithValueNode("NAS-Identifier", "0x" + "436973636f5f64623a31643a3634".toUpperCase()));

    subnodes.clear();
    this.subnodes.add(new TypeWithValueNode("blackstormnetworksoption", "0x00000006"));
    this.nodes.add(new TypeOnlyNode("Vendor-Specific14179", subnodes));

    this.nodes.add(new TypeWithValueNode("Service-Type", "2"));

    this.nodes.add(new TypeWithValueNode("Framed-MTU", "0x00000514"));
    this.nodes.add(new TypeWithValueNode("NAS-Port-Type", "19"));

    this.nodes.add(new TypeWithValueNode("Tunnel-Type", "13"));
    this.nodes.add(new TypeWithValueNode("Tunnel-Medium-Type", "6"));

    this.nodes.add(new TypeWithValueNode("Tunnel-Private-Group-Id", "0x31373330"));


    if (EAPMessage.length() < 256) {
      this.nodes.add(new TypeWithValueNode("EAP-Message", EAPMessage));
    } else {
      this.nodes.add(new TypeWithValueNode("EAP-Message", EAPMessage.substring(0, 508)));
      this.nodes.add(new TypeWithValueNode("EAP-Message", "0x" + EAPMessage.substring(508, EAPMessage.length())));

    }
    if (state.length() > 0)
      this.nodes.add(new TypeWithValueNode("State", state));
    this.nodes.add(new TypeWithValueNode("Message-Authenticator", messageAuthenticator));


  }


  public byte[] createRadiusMessage(int code, int id, byte[] options, byte[] authenticator) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    // Uebergebene Optionen anhaengen

    long length = options.length + 20; //code (1) id (1) laenge(2) authenticator (16) + optionen

    byte[] len = ByteUtil.toByteArray(length, 2); //auf 2 Bytes bringen

    try {
      message.write(code);
      message.write(id);
      message.write(len);
      message.write(authenticator);
      message.write(options);
    } catch (IOException e1) {
      System.out.println("Problems creating Radius Message: " + e1);
    }

    byte[] data = message.toByteArray();
    return data;

  }

}
