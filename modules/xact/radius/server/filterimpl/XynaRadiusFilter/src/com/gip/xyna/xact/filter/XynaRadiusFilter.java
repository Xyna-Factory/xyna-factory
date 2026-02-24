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
package com.gip.xyna.xact.filter;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.XynaRadiusTriggerConnection;
import com.gip.xyna.xact.trigger.tlvencoding.radius.Node;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusConfigurationDecoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusConfigurationEncoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TextConfigTree;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TextConfigTreeReader;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeOnlyNode;
import com.gip.xyna.xact.trigger.tlvencoding.radius.TypeWithValueNode;
import com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil;
import com.gip.xyna.xact.trigger.tlvencoding.util.Md5HMAC;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.radius.Code;
import xact.radius.Identifier;
import xact.radius.RadiusMessage;
import xact.radius.RequestAuthenticator;
import xact.radius.SourceIP;
import xact.radius.SourcePort;



public class XynaRadiusFilter extends ConnectionFilter<XynaRadiusTriggerConnection> {

  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusFilter.class);

  private static XynaPropertyString wfAccessRequest;

  private static String sharedSecretProp = "xact.radius.sharedSecret";
  private final static XynaPropertyString sharedSecretXynaProp = new XynaPropertyString(sharedSecretProp, "sharedSecret", false);


  public void onDeployment(EventListener trigger) {
    super.onDeployment(trigger);

    wfAccessRequest = new XynaPropertyString("xyna.radius.wf.AccessRequest", "xact.radius.RADIUSAccessRequest")
        .setDefaultDocumentation(DocumentationLanguage.DE, "Workflow für den Access-Request, der vom Filter aufgerufen wird.")
        .setDefaultDocumentation(DocumentationLanguage.EN, "Workflow for the access request that will be called by the filter.");
  }


  public void onUndeployment(EventListener trigger) {
  }


  /**
   * analyzes TriggerConnection and creates XynaOrder if it accepts the connection. if this filter does not return a
   * XynaOrder, Xyna Processing will call generateXynaOrder() of the next Filter registered for the Trigger
   *
   * @param tc
   * @return XynaOrder which will be started by Xyna Processing. null if this Filter doesn't accept the connection
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error. results in
   *           onError() being called by Xyna Processing.
   * @throws InterruptedException if onError() should not be called. (e.g. if for a http trigger connection this filter
   *           decides, it wants to return a 500 servererror, and not call any workflow)
   */
  public XynaOrder generateXynaOrder(XynaRadiusTriggerConnection tc) throws XynaException, InterruptedException {
    if (logger.isDebugEnabled())
      logger.debug("Radius packet received!");

    DatagramPacket datagramPacket = tc.getRawPacket();
    int dpLen = datagramPacket.getLength();
    if (dpLen < 20) {
      logger.info("Received Packet too short. Not accepting it.");
      return null;
    }

    byte[] data = datagramPacket.getData();
    String code = String.valueOf(data[0] & 0xFF);
    String id = String.valueOf(data[1] & 0xFF);
    int packetlength = (data[2] & 0xFF) * 256 + (data[3] & 0xFF);
    if (dpLen != packetlength) {
      logger.info("Received Packet has wrong length! Not accepting it.");
      return null;
    }
    if (!code.equals("1")) {
      logger.info("Radius Code in Radius Packet not configured. Not accepting Message.");
      return null; // kein erwarteter Nachrichtentyp => nicht annehmen.
    }

    byte[] authenticator = new byte[16];
    for (int i = 4; i < 20; i++) {
      authenticator[i - 4] = data[i];
    }
    String authenticatorstring = ByteUtil.toHexValue(authenticator);

    if (logger.isDebugEnabled()) {
      logger.debug("Code: " + code);
      logger.debug("Identifier: " + id);
      logger.debug("Length: " + packetlength);
      logger.debug("Authenticator: " + authenticatorstring);
    }

    // nur Attribute an Decoder geben
    List<Byte> optiondata = new ArrayList<Byte>();
    try {
      for (int z = 20; z < packetlength; z++) {
        optiondata.add(data[z]);
      }
    } catch (Exception e) {
      logger.warn("Unexpected Length of Packet!");
    }

    // Liste in Array schreiben (geht sicher besser?)
    byte[] optarg = new byte[optiondata.size()];
    for (int z = 0; z < optiondata.size(); z++) {
      optarg[z] = optiondata.get(z);
    }

    List<? extends xact.radius.Node> momlist = new ArrayList<xact.radius.Node>();
    RadiusConfigurationDecoder dec = tc.getDecoder();
    try {
      String decodedData = dec.decode2(optarg);
      TextConfigTree tree = new TextConfigTreeReader(new StringReader(decodedData)).read();
      List<Node> nodes = new ArrayList<Node>();
      for (Node n : tree.getNodes()) {
        nodes.add(n);
      }

      logNodes(nodes);
      if (logger.isDebugEnabled()) {
        logger.debug("Creating XynaObjects from Radius Packet ...");
      }
      momlist = createMOM(nodes);
      if (logger.isDebugEnabled()) {
        logger.debug("MOMList Size: " + momlist.size());
      }
    } catch (Exception e) {
      logger.warn("Unbekanntes UDP Paket empfangen: ", e);
    }

    XynaObjectList<xact.radius.Node> output = new XynaObjectList<xact.radius.Node>(new ArrayList<xact.radius.Node>(), "xact.radius.Node");

    if (logger.isDebugEnabled()) {
      logger.debug("RADIUSFILTER: Nodes given to Workflow: ");
    }

    for (xact.radius.Node n : momlist) {
      if (n == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("RADIUSFILTER: Nullnode");
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Typename: " + n.getTypeName());
        }
        output.add(n);
      }
    }

    DestinationKey dk = new DestinationKey(wfAccessRequest.get());
    Code xmomcode = new Code(code);
    Identifier xmomidentifier = new Identifier(id);
    RequestAuthenticator xmomauthenticator = new RequestAuthenticator(authenticatorstring);
    SourceIP xmomip = new SourceIP(datagramPacket.getAddress().getHostAddress());
    SourcePort xmomport = new SourcePort(String.valueOf(datagramPacket.getPort()));
    RadiusMessage xmommessage = new RadiusMessage(ByteUtil.toHexValue(data));

    if (logger.isDebugEnabled()) {
      logger.debug("Sending XynaOrder ...");
    }

    return new XynaOrder(dk, xmomcode, xmomidentifier, xmomauthenticator, xmomip, xmomport, output, xmommessage);
  }


  public List<xact.radius.Node> createMOM(List<Node> l) {
    List<xact.radius.Node> moms = new ArrayList<xact.radius.Node>();
    for (Node n : l) {
      moms.add(convertNode(n));
    }

    return moms;
  }


  /**
   * Node => MOM Node
   */
  public xact.radius.Node convertNode(Node n) {
    if (n instanceof TypeWithValueNode) {
      if (!(n.getTypeName().equals("Tlv"))) { // unbekannte Option
        return new xact.radius.TypeWithValueNode(n.getTypeName(), ((TypeWithValueNode) n).getValue());
      } else {
        logger.warn("Received unknown Radius Option (not passed to workflow): " + ((TypeWithValueNode) n).getValue());
        return null;
      }
    } else if (n instanceof TypeOnlyNode) {
      TypeOnlyNode tonode = (TypeOnlyNode) n;
      List<Node> subNodes = tonode.getSubNodes();
      List<xact.radius.Node> convertedSubNodes = new ArrayList<xact.radius.Node>();

      if (subNodes.size() != 0) {
        for (Node z : subNodes) {
          if (convertNode(z) != null) { // null wenn unbekannte Option entdeckt
            convertedSubNodes.add(convertNode(z));
          }
        }
      }
      return new xact.radius.TypeOnlyNode(n.getTypeName(), convertedSubNodes);
    }
    return null;
  }


  /**
   * MOM Node => Node
   */
  public Node convertNode(xact.radius.Node n) {
    if (n instanceof xact.radius.TypeWithValueNode) {
      return new TypeWithValueNode(n.getTypeName(), ((xact.radius.TypeWithValueNode) n).getValue());
    } else if (n instanceof xact.radius.TypeOnlyNode) {
      xact.radius.TypeOnlyNode tonode = (xact.radius.TypeOnlyNode) n;
      List<? extends xact.radius.Node> subNodes = tonode.getSubnodes();
      List<Node> convertedSubNodes = new ArrayList<Node>();

      if (subNodes.size() != 0) {
        for (xact.radius.Node z : subNodes) {
          convertedSubNodes.add(convertNode(z));
        }
      }
      return new TypeOnlyNode(n.getTypeName(), convertedSubNodes);
    }
    return null;
  }


  /**
   * called when above XynaOrder returns successfully.
   *
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, XynaRadiusTriggerConnection tc) {
    if (logger.isDebugEnabled()) {
      logger.debug("Starting to process workflow response ...");
    }

    if (!(response instanceof Container)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Xyna Container expected as output of workflow!");
      }
      return;
    }
    Container respcontainer = (Container) response;
    if (respcontainer.size() != 4) {
      logger.warn("Radius response has to contain Code, Identifier, Authenticator and a list of Attribute Nodes! Aborting answer!");
      return;
    }

    int code = -1;
    int identifier = -1;
    try {
      code = Integer.parseInt(((Code) respcontainer.get(0)).getValue());
      identifier = Integer.parseInt(((Identifier) respcontainer.get(1)).getValue());
    } catch (Exception e) {
      logger.warn("Check Code and Identifier given. Aborting reply!");
      return;
    }

    String sharedsecret = sharedSecretXynaProp.get();
    if (sharedsecret == null || sharedsecret.length() == 0) {
      logger.error("Property " + sharedSecretProp + " not set. No shared secret available for radius answer! Not sending one!");
      return;
    }

    // Liste von Nodes aus Workflowantwort erstellen
    List<Node> resultlist = new ArrayList<Node>();
    boolean addMessageAuthenticator = false;
    for (Object n : (XynaObjectList<?>) respcontainer.get(3)) {
      if (!(n instanceof xact.radius.Node)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Only Nodes expected, but got different XynaObject!");
        }
        continue;
      }
      xact.radius.Node node = (xact.radius.Node) n;
      // Feststellen ob EAP Message Authenticator noetig ist
      if (node.getTypeName().equals("EAP-Message")) {
        addMessageAuthenticator = true;
      }
      try {
        resultlist.add(convertNode(node));
      } catch (Exception e) {
        logger.warn("Problems converting node " + node.getTypeName() + ": ", e);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Received nodes of workflow output succesfully converted and Radius parameters set!");
    }

    RadiusConfigurationEncoder enc = tc.getEncoder();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      enc.encode(resultlist, output);
    } catch (Exception e) {
      logger.info("Encoding of received Radius Options failed!", e);
    }

    // Response Authenticator bauen
    String requestauthenticatorstring = ((RequestAuthenticator) respcontainer.get(2)).getValue();
    byte[] requestauthenticator = ByteUtil.toByteArray(requestauthenticatorstring); // RequestAuthenticator holen
    byte[] options = output.toByteArray();
    byte[] sharedS = sharedsecret.getBytes();
    // Code+ID+Laenge+RequestAuthenticator+Attribute+SharedSecret
    byte[] authenticatorarray = buildRadiusAuthenticator(code, identifier, requestauthenticator, options, sharedS); // MD5 ueber
    if (logger.isDebugEnabled()) {
      logger.debug("Radius Message to Send: ");
      logger.debug("Code: " + code);
      logger.debug("Identifier: " + identifier);
      logger.debug("Authenticator: " + ByteUtil.toHexValue(authenticatorarray));
      if (options.length > 0) {
        logger.debug("Payload: " + ByteUtil.toHexValue(options));
      } else {
        logger.debug("Empty Payload. No Attributes given!");
      }
    }

    byte[] data = {};
    try {
      data = createRadiusMessage(code, identifier, authenticatorarray, options);
      if (addMessageAuthenticator) {
        data = createRadiusMessage(code, identifier, requestauthenticator, options);
        if (logger.isDebugEnabled()) {
          logger.debug("Data before adding MessageAuthenticator: " + ByteUtil.toHexValue(data));
        }
        byte[] messageWithMessageAuthenticator = createMessageAuthenticator(data, sharedsecret);
        if (logger.isDebugEnabled()) {
          logger.debug("Message after adding new MessageAuthenticator: " + ByteUtil.toHexValue(messageWithMessageAuthenticator));
        }
        options = extractOptionsFromData(messageWithMessageAuthenticator);
        if (logger.isDebugEnabled()) {
          logger.debug("Options from new message: " + ByteUtil.toHexValue(options));
        }
        authenticatorarray = buildRadiusAuthenticator(code, identifier, requestauthenticator, options, sharedS); // MD5 ueber
        if (logger.isDebugEnabled()) {
          logger.debug("new generated RadiusAuthenticator: " + ByteUtil.toHexValue(authenticatorarray));
        }
        data = createRadiusMessage(code, identifier, authenticatorarray, options);
        if (logger.isDebugEnabled()) {
          logger.debug("Final Message before sending it: " + ByteUtil.toHexValue(data));
        }
      }
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Creation of Radius Message failed!", e);
      }
    }

    tc.sendUDP(tc.getRawPacket().getAddress().getHostAddress(), data);
  }


  private byte[] extractOptionsFromData(byte[] data) {
    byte[] result = new byte[data.length - 20];
    System.arraycopy(data, 20, result, 0, data.length - 20);
    return result;
  }


  private byte[] buildRadiusAuthenticator(int code, int identifier, byte[] requestauthenticator, byte[] options, byte[] sharedS) {
    byte[] message = createRadiusMessage(code, identifier, requestauthenticator, options);
    ByteArrayOutputStream authenticator = new ByteArrayOutputStream();
    try {
      authenticator.write(message);
      if (sharedS.length > 0)
        authenticator.write(sharedS);
    } catch (IOException e) {
      logger.warn("Error creating response authenticator: ", e);
    }

    return md5(authenticator.toByteArray());
  }


  private byte[] createRadiusMessage(int code, int id, byte[] authenticator, byte[] options) {
    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    // Uebergebene Optionen anhaengen
    long length = options.length + 20; // code (1) id (1) laenge(2) authenticator (16) + optionen
    byte[] len = ByteUtil.toByteArray(length, 2); // auf 2 Bytes bringen
    try {
      message.write(code);
      message.write(id);
      message.write(len);
      message.write(authenticator);
      if (options.length > 0)
        message.write(options);
    } catch (IOException e) {
      logger.warn("Error building datagram to reply: ", e);
    }

    byte[] data = message.toByteArray();
    return data;
  }


  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   *
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, XynaRadiusTriggerConnection tc) {
    for (Exception e1 : e) {
      logger.error("Error in Radiusfilter: ", e1);
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "Filter for Radius access requests, calls the workflow specified in the property: " + wfAccessRequest.getPropertyName();
  }


  public static int unsignedByteToInt(byte b) {
    return (int) b & 0xFF;
  }


  public void logNodes(List<Node> nodes) throws UnsupportedEncodingException {
    if (logger.isDebugEnabled()) {
      logger.debug("==============");

      int count = 0;
      boolean vnode, tnode;
      TypeWithValueNode valuenode = null;
      TypeOnlyNode typenode = null;
      for (Node n : nodes) {
        vnode = true;
        tnode = false;

        count++;
        logger.debug("Option " + count + "\n");
        logger.debug("Name: " + n.getTypeName());

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
          logger.debug("Value: " + valuenode.getValue());

        }
        if (tnode) {
          if (typenode.getSubNodes() != null && !typenode.getSubNodes().isEmpty()) {
            logger.debug("");
            logger.debug("Suboptionen: ");
            logNodes(typenode.getSubNodes());
          }

        }

        logger.debug("");

      }
    }
  }


  private static byte[] md5(byte[] input) {

    byte[] md5 = null;

    if (input == null)
      return null;

    try {
      // Create MessageDigest object for MD5
      MessageDigest digest = MessageDigest.getInstance("MD5");

      // Update input string in message digest
      digest.update(input, 0, input.length);
      md5 = digest.digest();
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Problems building md5sum: " + e);
      }
    }

    return md5;
  }


  public static byte[] createMessageAuthenticator(byte[] message, String sharedSecret) {

    if (logger.isDebugEnabled()) {
      logger.debug("CreateMessageAuthenticator: Inputmessage: " + ByteUtil.toHexValue(message));
      logger.debug("CreateMessageAuthenticator: SharedSecret: " + sharedSecret);
    }

    byte[] eapend = ByteUtil.toByteArray("0x501200000000000000000000000000000000");

    byte[] finalmessage = new byte[message.length + eapend.length];
    System.arraycopy(message, 0, finalmessage, 0, message.length);
    System.arraycopy(eapend, 0, finalmessage, message.length, eapend.length);

    int len = (finalmessage[2] & 0xFF) * 256 + (finalmessage[3] & 0xFF) + 18;
    if (logger.isDebugEnabled()) {
      logger.debug("CreateMessageAuthenticator: Old length: " + ((finalmessage[2] & 0xFF) * 256 + (finalmessage[3] & 0xFF))
          + " new length: " + len);
    }

    byte[] lenbytes = ByteUtil.toByteArray(len, 2);
    System.arraycopy(lenbytes, 0, finalmessage, 2, lenbytes.length);
    System.arraycopy(lenbytes, 0, message, 2, lenbytes.length);
    if (logger.isDebugEnabled()) {
      logger.debug("CreateMessageAuthenticator: Message with Authenticator and new length: " + ByteUtil.toHexValue(finalmessage));
    }

    // Makes the hash

    byte[] hMACMD5 = new byte[18];
    String shsec = sharedSecret;
    int charlength = shsec.length() * 2;
    shsec = "0x" + toHex(shsec).substring(40 - charlength).toUpperCase();
    byte[] sharedSecretBytes = ByteUtil.toByteArray(shsec);
    hMACMD5 = Md5HMAC.hmac(sharedSecretBytes, finalmessage);

    byte[] resultmessage = new byte[message.length + hMACMD5.length + 2];

    byte[] eapstart = ByteUtil.toByteArray("0x5012");

    System.arraycopy(message, 0, resultmessage, 0, message.length);
    System.arraycopy(eapstart, 0, resultmessage, message.length, eapstart.length);
    System.arraycopy(hMACMD5, 0, resultmessage, message.length + eapstart.length, hMACMD5.length);

    return resultmessage;
  }


  public static String intToHexString(int intToParse) {
    String parsedInt = "";
    byte[] array = ByteUtil.toByteArray(intToParse);
    parsedInt = parsedInt + ByteUtil.toHexValue(array);
    return parsedInt;
  }


  public static String toHex(String arg) {
    return String.format("%040x", new BigInteger(arg.getBytes(/* YOUR_CHARSET? */)));
  }

}
