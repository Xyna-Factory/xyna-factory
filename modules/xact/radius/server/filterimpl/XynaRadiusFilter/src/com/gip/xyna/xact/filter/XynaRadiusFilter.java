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

    List<? extends xact.radius.Node> momlist = new ArrayList<xact.radius.Node>();

    if (logger.isDebugEnabled())
      logger.debug("Radius packet received!");

    RadiusConfigurationDecoder dec = tc.getDecoder();

    DatagramPacket d = tc.getRawPacket();
    byte[] data = d.getData();

    if (d.getLength() < 20) {
      logger.info("Received Packet too short. Not accepting it.");
      return null;
    }

    String code = String.valueOf(data[0] & 0xFF);
    String id = String.valueOf(data[1] & 0xFF);
    int packetlength = (data[2] & 0xFF) * 256 + (data[3] & 0xFF);

    if (d.getLength() != packetlength) {
      logger.info("Received Packet has wrong length! Not accepting it.");
      return null;
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

    xact.radius.Code xmomcode = new xact.radius.Code(code);
    xact.radius.Identifier xmomidentifier = new xact.radius.Identifier(id);
    xact.radius.RequestAuthenticator xmomauthenticator = new xact.radius.RequestAuthenticator(authenticatorstring);

    xact.radius.SourceIP xmomip = new xact.radius.SourceIP(d.getAddress().getHostAddress());
    xact.radius.SourcePort xmomport = new xact.radius.SourcePort(String.valueOf(d.getPort()));

    byte[] packet = new byte[d.getLength()];
    System.arraycopy(data, 0, packet, 0, d.getLength());
    xact.radius.RadiusMessage xmommessage = new xact.radius.RadiusMessage(ByteUtil.toHexValue(packet));

    String whichworkflow = "";

    if (code.equals("1")) {
      whichworkflow = wfAccessRequest.get();
    } else {
      logger.info("Radius Code in Radius Packet not configured. Not accepting Message.");
      return null; // kein erwarteter Nachrichtentyp => nicht annehmen.
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

    try {
      String decodedData = dec.decode2(optarg);

      StringBuilder builder = new StringBuilder();
      builder.append(decodedData);
      TextConfigTree tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();

      List<Node> nodes = new ArrayList<Node>();

      for (Node n : tree.getNodes()) {
        nodes.add(n);
      }

      logNodes(nodes);

      if (logger.isDebugEnabled())
        logger.debug("Creating XynaObjects from Radius Packet ...");

      momlist = createMOM(nodes);
      if (logger.isDebugEnabled())
        logger.debug("MOMList Size: " + momlist.size());

    } catch (Exception e) {
      logger.warn("Unbekanntes UDP Paket empfangen: ", e);
    }

    XynaObjectList<xact.radius.Node> output = new XynaObjectList<xact.radius.Node>(new ArrayList<xact.radius.Node>(), "xact.radius.Node");

    if (logger.isDebugEnabled())
      logger.debug("RADIUSFILTER: Nodes given to Workflow: ");

    for (xact.radius.Node n : momlist) {
      if (n == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("RADIUSFILTER: Nullnode");
        }
      }
      if (n != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Typename: " + n.getTypeName());
        }
        output.add(n);
      }
    }

    if (logger.isDebugEnabled())
      logger.debug("Workflow: " + whichworkflow);

    DestinationKey dk = new DestinationKey(whichworkflow);

    if (logger.isDebugEnabled())
      logger.debug("Sending XynaOrder ...");

    return new XynaOrder(dk, xmomcode, xmomidentifier, xmomauthenticator, xmomip, xmomport, output, xmommessage);
  }


  public List<xact.radius.Node> createMOM(List<Node> l) {
    List<xact.radius.Node> moms = new ArrayList<xact.radius.Node>();
    for (Node n : l) {
      moms.add(convertNode(n));
    }

    return moms;
  }


  // Node => MOM Node


  public xact.radius.Node convertNode(Node n) {
    if (n instanceof TypeWithValueNode) {
      if (!(n.getTypeName().equals("Tlv"))) // unbekannte Option
      {
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
          if (convertNode(z) != null) // null wenn unbekannte Option entdeckt
          {
            convertedSubNodes.add(convertNode(z));
          }
        }
      }
      return new xact.radius.TypeOnlyNode(n.getTypeName(), convertedSubNodes);
    }
    return null;
  }


  // MOM Node => Node


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

    if (!(response instanceof Container)) {
      if (logger.isDebugEnabled())
        logger.debug("Xyna Container expected as output of workflow!");
    } else {
      if (logger.isDebugEnabled())
        logger.debug("Starting to process workflow response ...");

      Container respcontainer = (Container) response;

      if (respcontainer.size() != 4) {
        logger.warn("Radius Response has to contain Code, Identifier, Authenticator and a list of Attribute Nodes! Aborting Answer!");
        return;
      }

      xact.radius.Code xmomcode = (xact.radius.Code) respcontainer.get(0);
      xact.radius.Identifier xmomidentifier = (xact.radius.Identifier) respcontainer.get(1);
      xact.radius.RequestAuthenticator xmomauthenticator = (xact.radius.RequestAuthenticator) respcontainer.get(2);

      XynaObjectList<?> xynalist = (XynaObjectList<?>) respcontainer.get(3);

      List<xact.radius.Node> inputnodelist = new ArrayList<xact.radius.Node>();
      List<Node> resultlist = new ArrayList<Node>();

      boolean addMessageAuthenticator = false;

      // Liste von Nodes aus Workflowantwort erstellen
      for (Object n : xynalist) {
        try {
          xact.radius.Node node = (xact.radius.Node) n;
          inputnodelist.add(node);
          // Feststellen ob EAP Message Authenticator noetig ist
          if (node.getTypeName().equals("EAP-Message"))
            addMessageAuthenticator = true;

        } catch (Exception e) {
          if (logger.isDebugEnabled())
            logger.debug("Only Nodes expected, but got different XynaObject!");
        }
      }

      // MOM Nodes in normale Nodes konvertieren
      for (xact.radius.Node n : inputnodelist) {
        try {
          resultlist.add(convertNode(n));
        } catch (Exception e) {
          logger.warn("Problems converting node " + n.getTypeName() + ": ", e);
        }
      }

      int code = -1;
      int identifier = -1;
      try {
        code = Integer.parseInt(xmomcode.getValue());
        identifier = Integer.parseInt(xmomidentifier.getValue());
      } catch (Exception e) {
        logger.warn("Check Code and Identifier given. Aborting Reply!");
        return;
      }
      String requestauthenticatorstring = xmomauthenticator.getValue();
      String sharedsecret = sharedSecretXynaProp.get();
      if (sharedsecret == null || sharedsecret.length() == 0) {
        logger.error("Property " + sharedSecretProp + " not set. No shared secret available for radius answer! Not sending one!");
        return;
      }

      if (logger.isDebugEnabled())
        logger.debug("Received nodes of workflow output succesfully converted and Radius parameters set!");

      RadiusConfigurationEncoder enc = tc.getEncoder();

      ByteArrayOutputStream output = new ByteArrayOutputStream();

      try {
        enc.encode(resultlist, output);
      } catch (Exception e) {
        logger.info("Encoding of received Radius Options failed!", e);
      }

      // Response Authenticator bauen

      byte[] requestauthenticator = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil.toByteArray(requestauthenticatorstring); // RequestAuthenticator holen

      byte[] optionen = output.toByteArray();
      byte[] sharedS = sharedsecret.getBytes();

      byte[] authenticatorarray = buildRadiusAuthenticator(code, identifier, requestauthenticator, optionen, sharedS); // MD5 ueber
      // Code+ID+Laenge+RequestAuthenticator+Attribute+SharedSecret

      byte[] data = {};
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Radius Message to Send: ");
          logger.debug("Code: " + code);
          logger.debug("Identifier: " + identifier);
          logger.debug("Authenticator: " + ByteUtil.toHexValue(authenticatorarray));
          if (optionen.length > 0) {
            logger.debug("Payload: " + ByteUtil.toHexValue(optionen));
          } else {
            logger.debug("Empty Payload. No Attributes given!");
          }
        }

        data = createRadiusMessage(code, identifier, optionen, authenticatorarray);
        if (addMessageAuthenticator) {
          data = createRadiusMessage(code, identifier, optionen, requestauthenticator);
          logger.debug("Data before adding MessageAuthenticator: " + ByteUtil.toHexValue(data));
          byte[] messageWithMessageAuthenticator = createMessageAuthenticator(data, sharedsecret);
          logger.debug("Message after adding new MessageAuthenticator: " + ByteUtil.toHexValue(messageWithMessageAuthenticator));
          optionen = extractOptionsFromData(messageWithMessageAuthenticator);
          logger.debug("Options from new message: " + ByteUtil.toHexValue(optionen));
          authenticatorarray = buildRadiusAuthenticator(code, identifier, requestauthenticator, optionen, sharedS); // MD5 ueber
          logger.debug("new generated RadiusAuthenticator: " + ByteUtil.toHexValue(authenticatorarray));
          data = createRadiusMessage(code, identifier, optionen, authenticatorarray);
          logger.debug("Final Message before sending it: " + ByteUtil.toHexValue(data));
        }
      } catch (Exception e) {
        if (logger.isDebugEnabled())
          logger.debug("Creation of Radius Message failed!", e);
      }

      tc.sendUDP(tc.getRawPacket().getAddress().getHostAddress(), data);
    }
  }


  private byte[] extractOptionsFromData(byte[] data) {
    byte[] result = new byte[data.length - 20];
    System.arraycopy(data, 20, result, 0, data.length - 20);
    return result;
  }


  private byte[] buildRadiusAuthenticator(int code, int identifier, byte[] requestauthenticator, byte[] optionen, byte[] sharedS) {
    ByteArrayOutputStream authenticator = new ByteArrayOutputStream();

    long length = optionen.length + 20; // code (1) id (1) laenge(2) authenticator (16) + optionen

    byte[] len = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil.toByteArray(length, 2); // auf 2 Bytes bringen

    try {
      authenticator.write(code);
      authenticator.write(identifier);
      authenticator.write(len);
      authenticator.write(requestauthenticator);
      if (optionen.length > 0)
        authenticator.write(optionen);
      if (sharedS.length > 0)
        authenticator.write(sharedS);

    } catch (IOException e) {
      logger.warn("Error creating Response Authenticator: ", e);
    }
    logger.debug("Response Authenticator before MD5: " + ByteUtil.toHexValue(authenticator.toByteArray()));

    byte[] authenticatorarray = md5(authenticator.toByteArray());
    return authenticatorarray;
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


  public byte[] createRadiusMessage(int code, int id, byte[] options, byte[] authenticator) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    // Uebergebene Optionen anhaengen

    long length = options.length + 20; // code (1) id (1) laenge(2) authenticator (16) + optionen

    byte[] len = com.gip.xyna.xact.trigger.tlvencoding.util.ByteUtil.toByteArray(length, 2); // auf 2 Bytes bringen

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
