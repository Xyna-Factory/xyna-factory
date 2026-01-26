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



import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;


import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusConfigurationDecoder;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.DecoderException;
import com.gip.xyna.xact.trigger.tlvencoding.database.LoadConfigStatic;
import com.gip.xyna.xact.trigger.tlvencoding.radius.ConfigFileReadException;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;
import com.gip.xyna.xact.trigger.tlvencoding.radius.Node;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TextConfigTree;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TextConfigTreeReader;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeOnlyNode;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeWithValueNode;
import com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil;



public class RadiusMessageReceiver {

  private byte data[];
  Collection<RadiusEncoding> liste;
  DatagramSocket toSocket;
  String servername = "localhost";
  int buffer = 1024;


  public RadiusMessageReceiver(byte[] d) throws DecoderException, IOException {

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

    this.data = d;
    // Zeitmessung
    LoadConfigStatic anbindung = new LoadConfigStatic();
    anbindung.setUp();

    try {
      liste = anbindung.loadRadiusEntries();
    } catch (Exception e) {
      System.out.println("Failed to read from database");
    }

    if (liste.size() == 0) {
      System.out.println("Dataset from database empty");
      throw new IllegalArgumentException();
    }

    RadiusConfigurationDecoder dec = new RadiusConfigurationDecoder(new ArrayList<RadiusEncoding>(liste));

    String code = String.valueOf(data[0] & 0xFF);
    String id = String.valueOf(data[1] & 0xFF);
    int packetlength = (data[2] & 0xFF) * 16 + (data[3] & 0xFF);

    byte[] authenticator = new byte[16];
    for (int i = 4; i < 20; i++) {
      authenticator[i - 4] = data[i];
    }
    String authenticatorstring = ByteUtil.toHexValue(authenticator);

    System.out.println("Code: " + code);
    System.out.println("Identifier: " + id);
    System.out.println("Length: " + packetlength);
    System.out.println("Authenticator: " + authenticatorstring);

    // nur Attribute an Decoder geben
    List<Byte> optiondata = new ArrayList<Byte>();

    for (int z = 20; z < packetlength; z++) {
      optiondata.add(data[z]);
    }

    // Liste in Array schreiben (geht sicher besser?)
    byte[] optarg = new byte[optiondata.size()];
    for (int z = 0; z < optiondata.size(); z++) {
      optarg[z] = optiondata.get(z);
    }

    try {
      String decodedData = dec.decode2(optarg);

      StringBuilder builder = new StringBuilder();
      builder.append(decodedData);
      TextConfigTree tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();

      List<Node> nodes = tree.getNodes();

      readNodes(nodes);
    } catch (Exception e) {
      System.out.println("Unbekanntes UDP Paket empfangen.");
      System.out.println(e);
    }
  }


  public RadiusMessageReceiver() throws DecoderException, ConfigFileReadException, IOException {

    // UDP Server aufmachen
    int port = 1812;
    try {
      toSocket = new DatagramSocket(port);
    } catch (Exception e) {
      System.out.println(e);
    }

    byte[] buffer = new byte[1024];

    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    while (true) {
      try {
        System.out.println("================================");
        System.out.println("Listening for RADIUS Packets ...");
        toSocket.receive(packet);
        System.out.println("Received RADIUS Paket!");
        System.out.println("================================");

        data = packet.getData();
        new RadiusMessageReceiver(data);
      } catch (Exception e) {
        System.out.println("Exception while receiving: " + e);
      }
    }

  }


  public void readNodes(List<Node> nodes) throws UnsupportedEncodingException {
    System.out.println("==============");

    int count = 0;
    boolean vnode, tnode;
    TypeWithValueNode valuenode = null;
    TypeOnlyNode typenode = null;
    for (Node n : nodes) {
      vnode = true;
      tnode = false;

      count++;
      System.out.println("Option " + count + "\n");
      System.out.println("Name: " + n.getTypeName());

      try {
        valuenode = (TypeWithValueNode) n;
      } catch (Exception e) {
        vnode = false;
        tnode = true;
        try {
          typenode = (TypeOnlyNode) n;
        } catch (Exception ee) {
          tnode = false;
        }
      }

      if (vnode) {
        System.out.println("Value: " + valuenode.getValue());

      }
      if (tnode) {
        if (typenode.getSubNodes() != null && !typenode.getSubNodes().isEmpty()) {
          System.out.println();
          System.out.println("Suboptionen: ");
          readNodes(typenode.getSubNodes());
        }

      }

      System.out.println("");

    }
  }


}
