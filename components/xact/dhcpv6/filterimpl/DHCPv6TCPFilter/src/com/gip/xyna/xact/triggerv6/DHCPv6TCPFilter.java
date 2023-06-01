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
package com.gip.xyna.xact.triggerv6;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTree;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TextConfigTreeReader;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.utilv6.ByteUtil;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;



public class DHCPv6TCPFilter extends ConnectionFilter<DHCPv6TCPTriggerConnection> implements IPropertyChangeListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(DHCPv6TCPFilter.class);

  public static final String XYNA_PROPERTY_DHCP_HASHV6 = "xact.dhcpv6.hashv6";
  public static final String XYNA_PROPERTY_DHCP_HASHV6PASSVAL = "xact.dhcpv6.hashv6passval";

  public static final String XYNA_PROPERTY_DHCP_LOCKFILTER = "xact.dhcpv6.lockfilter";


  private static String hashcheck = XynaFactory.getInstance().getFactoryManagement()
                  .getProperty(XYNA_PROPERTY_DHCP_HASHV6);
  private static String hashpassval = XynaFactory.getInstance().getFactoryManagement()
                  .getProperty(XYNA_PROPERTY_DHCP_HASHV6PASSVAL);
  private static String lockfilter = XynaFactory.getInstance().getFactoryManagement()
                  .getProperty(XYNA_PROPERTY_DHCP_LOCKFILTER);


  private static long workflowtotaltime = 0;
  private static long timescount = 0;


  // private static AtomicLong filterstartcounter = new AtomicLong(0);
  // private static AtomicLong filterendcounter = new AtomicLong(0);

  public void onDeployment(EventListener trigger)
  {
 // Registrieren als PropertyChange-Listener
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                    .addPropertyChangeListener(this);
    super.onDeployment(trigger);
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
  public XynaOrder generateXynaOrder(DHCPv6TCPTriggerConnection tc) throws XynaException, InterruptedException {

    if (lockfilter != null)
      if (lockfilter.toUpperCase().equals("TRUE")) {
        if (logger.isDebugEnabled())
          logger.debug("DHCPv6TCPFilter: Filter locked. Rejecting Message!");
        return null;
      }

    List<? extends xdnc.dhcp.Node> momlist = new ArrayList<xdnc.dhcp.Node>();
    int msgtype = 0;
    int length = 0;
    byte[] optarg;
    byte[] transactionid = new byte[3];

    // if (logger.isDebugEnabled())
    // logger.debug("DHCPv6Filter: DHCPv6 packet received!");
    try {
      byte[] data = tc.getRawPacket();
      // byte[] packetcopy = Arrays.copyOfRange(data, 0, d.getLength());
      // // Buffer 0en abschneiden
      byte[] packetcopy = new byte[data.length];
      System.arraycopy(data, 0, packetcopy, 0, data.length);
      data = packetcopy;

      if(logger.isDebugEnabled())
      {
        String datastring ="0x";
        for(byte b:data)
        {
          datastring = datastring + Integer.toHexString(b); 
        }

        logger.debug("DHCPv6TCPFilter: Data: "+datastring);
      }
      
      length = (data[0]&0xFF)*256 + data[1]&0xFF; // angegebene Laenge steht in ersten beiden Bytes
      
      msgtype = data[2]; // Nachrichttyp im 3. Byte

      if(logger.isDebugEnabled())
      {
        logger.debug("DHCPv6TCPFilter: Length of received message: "+length);
        logger.debug("DHCPv6TCPFilter: Msgtype of received message: "+msgtype);
      }

      
      if (msgtype != 14)// LeaseQuery
      {
        if (logger.isDebugEnabled())
          logger.debug("DHCPv6TCPFilter: Received Message no LeaseQuery!");
        return null;
      }


      System.arraycopy(data, 3, transactionid, 0, 3);
      tc.setTransactionidInBytes(transactionid);
      // MsgType und TransactionID aus Data rausloeschen
      byte[] datacopy = new byte[data.length - 6];
      System.arraycopy(data, 6, datacopy, 0, data.length - 6);

      data = datacopy;

      if(logger.isDebugEnabled())
      {
        logger.debug("DHCPv6TCPFilter: TransactionID: "+transactionid[0]+","+transactionid[1]+","+transactionid[2]);
        String datastring ="0x";
        for(byte b:data)
        {
          datastring = datastring + Integer.toHexString(b); 
        }

        logger.debug("DHCPv6TCPFilter: Data: "+datastring);
      }


      List<Byte> optiondata = new ArrayList<Byte>();

      for (int z = 0; z < data.length; z++) {

        optiondata.add(data[z]);
      }

      // Liste in Array schreiben (geht sicher besser?)
      optarg = new byte[optiondata.size()];
      for (int z = 0; z < optiondata.size(); z++) {
        optarg[z] = optiondata.get(z);
      }

      DHCPv6ConfigurationDecoder dec = tc.getDecoder();

      try {
        String decodedData = dec.decode(optarg);

        StringBuilder builder = new StringBuilder();
        builder.append(decodedData);
        TextConfigTree tree = new TextConfigTreeReader(new StringReader(builder.toString())).read();

        List<Node> nodes = new ArrayList<Node>();

        // Node msgtypenode=new
        // TypeWithValueNode("MessageType",""+msgtype);
        // Node hopsnode=new TypeWithValueNode("Hops",""+hops);
        // Node peeraddnode=new
        // TypeWithValueNode("PeerAddress",""+peeradd);

        // nodes.add(msgtypenode);
        // nodes.add(hopsnode);
        // nodes.add(peeraddnode);

        // DHCPv6 Options

        String tempvalue;
        String nodevalue;
        String resultvalue = "0x";
        int innermessagetype = -1;

        boolean leasequeryoption = false;
        boolean serveridoption = false;

        if (hashcheck == null) {
          hashcheck = "false";
        }

        
        Node relaynode = null;

        for (Node no : tree.getNodes()) {
          // Check nodevalues for " to avoid wrong
          // hexadecimalrepresentation
          // if (no instanceof TypeWithValueNode) {
          // nodevalue = ((TypeWithValueNode) no).getValue();
          // if (nodevalue.contains("\"")) {
          // tempvalue = nodevalue.substring(1, nodevalue.length() - 1);
          // byte[] res = tempvalue.getBytes("UTF-8");
          // resultvalue = ByteUtil.toHexValue(res);
          // no = new TypeWithValueNode(no.getTypeName(), resultvalue);
          //
          // }
          //
          // }
          // entferne und speichere ClientID und TransactionID
          if (no instanceof TypeWithValueNode && no.getTypeName().equals("InterfaceID")) {
            tc.setInterfaceId(no);
            nodes.add(no);
          }
          if (no instanceof TypeOnlyNode && no.getTypeName().equals("ClientID")) {
            tc.setClientId(no);
            nodes.add(no);
          }
          if (no instanceof TypeOnlyNode && no.getTypeName().equals("LeaseQuery")) {
            leasequeryoption = true;
            nodes.add(no);
          }

          // else if (no instanceof TypeWithValueNode && no.getTypeName().equals("Tlv")) { // unbekannte Option
          // logger.warn("Received unknown DHCPv6 Option (not passed to workflow): "+((TypeWithValueNode)no).getValue());
          // }
          else {
            nodes.add(no); // andere Nodes reinschreiben
          }
        }

        if (logger.isDebugEnabled())
          logger.debug("Messagetype: " + msgtype + " (LeaseQuery)");


        // keine ClientID => keine Bearbeitung
        if (tc.getClientId() == null) {
          if (logger.isDebugEnabled())
            logger.debug("No ClientID Option in Message, aborting ...");
          return null;
        }

        String clientmac = "";
        //Client Mac herausholen
        for (Node n : ((TypeOnlyNode) tc.getClientId()).getSubNodes()) {
          if (n.getTypeName().equals("DUID-LLT") || n.getTypeName().equals("DUID-LL")) {
            for (Node sn : ((TypeOnlyNode) n).getSubNodes()) {
              if (sn.getTypeName().equals("LinkLayerAddress")) {
                clientmac = ((TypeWithValueNode) sn).getValue();
              }
            }
          }
        }
        
        if(clientmac.length()>0)
        {
          if(logger.isDebugEnabled())
          {
            logger.debug("ClientMac: "+clientmac);
          }
        }
        else
        {
          logger.error("No valid Clientmac in Message, aborting ...");
          return null;
        }
        
        
        
        
        // Keine LQ Option bei Leasequery => keine Bearbeitung
        if (msgtype == 14 && leasequeryoption == false) {
          if (logger.isDebugEnabled())
            logger.debug("No LeaseQuery Option in Lease Query Message, aborting ...");
          return null;

        }

        momlist = createMOM(nodes);
        // readNodes(nodes);
      }
      catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Unbekanntes UDP Paket empfangen.");
          logger.debug(e);
        }
        return null;
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("DHCPv6FILTERERROR: " + e);
      throw new XynaException("Received Packet invalid!");

    }

    XynaObjectList<xdnc.dhcp.Node> output = new XynaObjectList<xdnc.dhcp.Node>(momlist, xdnc.dhcp.Node.class.getName()); // workaround

    String whichworkflow = "";

    if (msgtype == 14) // DHCPv6 LeaseQuery
    {
      whichworkflow = "xdnc.dhcpv6.BulkLeaseQuery_v6";
    }

    DestinationKey dk = new DestinationKey(whichworkflow);

    // if (logger.isDebugEnabled())
    // logger.debug("DHCPv6Filter: Sending XynaOrder ...");

    XynaOrder ord = new XynaOrder(dk, output);
    ord.setSchedulingTimeout(System.currentTimeMillis() + 60000);

    tc.setWorkflowstarttime(System.currentTimeMillis());
    return ord;
  }


  // private boolean checkForUnknownOptions(Node no) {
  //
  // if(no.getTypeName().equals("Tlv"))
  // {
  // return true;
  // }
  //
  // if(no instanceof TypeOnlyNode)
  // {
  // for(Node sn:((TypeOnlyNode) no).getSubNodes())
  // {
  // if(checkForUnknownOptions(sn))
  // {
  // return true;
  // }
  // }
  // }
  //
  // return false;
  // }


  public List<xdnc.dhcp.Node> createMOM(List<Node> l) {
    List<xdnc.dhcp.Node> moms = new ArrayList<xdnc.dhcp.Node>();
    for (Node n : l) {
      if (convertNode(n) != null)
        moms.add(convertNode(n)); // bei unbekannter Option = null
    }

    return moms;
  }


  // Node => MOM Node

  public xdnc.dhcp.Node convertNode(Node n) {
    if (n instanceof TypeWithValueNode) {
      if (!(n.getTypeName().equals("Tlv"))) // unbekannte Option
      {
        return new xdnc.dhcp.TypeWithValueNode(n.getTypeName(), ((TypeWithValueNode) n).getValue());
      }
      else {
        logger.warn("Received unknown DHCPv6 Option (not passed to workflow): " + ((TypeWithValueNode) n).getValue());
        return null;
      }
    }
    else if (n instanceof TypeOnlyNode) {
      TypeOnlyNode tonode = (TypeOnlyNode) n;
      List<Node> subNodes = tonode.getSubNodes();
      List<xdnc.dhcp.Node> convertedSubNodes = new ArrayList<xdnc.dhcp.Node>();

      if (subNodes.size() != 0) {
        for (Node z : subNodes) {
          if (convertNode(z) != null) // null wenn unbekannte Option entdeckt
          {
            convertedSubNodes.add(convertNode(z));
          }
        }
      }
      return new xdnc.dhcp.TypeOnlyNode(n.getTypeName(), convertedSubNodes);
    }
    return null;
  }


  // MOM Node => Node

  public Node convertNode(xdnc.dhcp.Node n) {
    // if (logger.isDebugEnabled())
    // logger.debug("Nodename: " + n.getTypeName());

    if (n instanceof xdnc.dhcp.TypeWithValueNode) {
      // if (logger.isDebugEnabled())
      // logger.debug("Value: " + ((xact.dhcp.TypeWithValueNode) n).getValue());
      return new TypeWithValueNode(n.getTypeName(), ((xdnc.dhcp.TypeWithValueNode) n).getValue());
    }
    else if (n instanceof xdnc.dhcp.TypeOnlyNode) {

      // if (logger.isDebugEnabled())
      // logger.debug("Value: Subnodes");

      xdnc.dhcp.TypeOnlyNode tonode = (xdnc.dhcp.TypeOnlyNode) n;
      List<? extends xdnc.dhcp.Node> subNodes = tonode.getSubnodes();
      List<Node> convertedSubNodes = new ArrayList<Node>();

      if (subNodes.size() != 0) {
        for (xdnc.dhcp.Node z : subNodes) {
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
  public void onResponse(GeneralXynaObject response, DHCPv6TCPTriggerConnection tc) {

    // filterstartcounter.incrementAndGet();

    byte[] transactionid = new byte[3];

    String mac = "";

    try {
      Node clid = tc.getClientId();
      Node clidsub = ((TypeOnlyNode) clid).getSubNodes().get(0); // DUID LL oder LLT
      List<Node> clidsubs = ((TypeOnlyNode) clidsub).getSubNodes();
      for (Node n : clidsubs) {
        if (n.getTypeName().equals("LinkLayerAddress")) {
          mac = ((TypeWithValueNode) n).getValue();
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("Could not extract Mac for Debug Messages in Filter!");
    }

    if (!(response instanceof XynaObjectList<?>)) {
      logger.warn("(" + mac + ") List of Xyna Objects expected as output of workflow! Aborted.");
      return;
    }
    else {
      if (logger.isDebugEnabled())
        logger.debug("(" + mac + ") Starting to process workflow response ...");

      XynaObjectList xynalist = (XynaObjectList) response;
      List<Node> convertedlist = new ArrayList<Node>();
      List<Node> resultlist = new ArrayList<Node>();

      // Interface ID anhaengen
      if (tc.getInterfaceId() != null)
        resultlist.add(tc.getInterfaceId());


      // MOM Nodes in normale Nodes konvertieren


      // if (logger.isDebugEnabled())
      // logger.debug("Nodes received from Workflow:");

      

      if (xynalist.size() != 0) {
        for (Object n : xynalist) {
          try {
            xdnc.dhcp.Node node = (xdnc.dhcp.Node) n;
            convertedlist.add(convertNode(node));
            resultlist.add(convertNode(node));

          }
          catch (Exception e) {
            logger.warn("(" + mac + ") Only Nodes expected, but got different XynaObject!");
            return;
          }
        }
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("(" + mac + ") Nodelist from Workflow empty! Sending anyway with ClientID and ServerID ...");
        }
      }

      Node bulkLeaseQueryReply = null;
      for(Node n:resultlist)
      {
        if(n.getTypeName().equals("LeaseQueryReply"))bulkLeaseQueryReply = n;
      }
      
      if(bulkLeaseQueryReply==null) // kein LeaseQueryBulk Format mit mehreren LeaseQueryData zu senden
      {
        // Nodeliste durchgehen und DHCPv6 Kopf setzen

        int msgtype = 15; // LeaseQuery Reply
        boolean closesocketaftersend = false;
        
        List<Node> nodesToBeSent = new ArrayList<Node>();
        
        nodesToBeSent.add(tc.getClientId());
        nodesToBeSent.add(createServerID("00:00:00:00:00:0"+String.valueOf(hashMac(mac))));

        for(Node n:resultlist)
        {
          nodesToBeSent.add(n);
        }
        
        // Vom Workflow erhaltene DHCP Optionen von Nodes in Bytestrom
        // konvertieren

        sendBulkLeaseQuery(tc, transactionid, mac, nodesToBeSent, msgtype, closesocketaftersend);
        
        
        // Abschlussnachricht
        
        msgtype=16; // LeaseQUERY-DONE 
        nodesToBeSent.clear();
        closesocketaftersend=true;
        sendBulkLeaseQuery(tc, transactionid, mac, nodesToBeSent, msgtype, closesocketaftersend);

      }
      else
      {
        int msgtype = 15; // LeaseQuery Reply zuerst senden
        boolean closesocketaftersend = false;
        
        List<Node> nodesToBeSent = new ArrayList<Node>();
        
        nodesToBeSent.add(tc.getClientId());
        nodesToBeSent.add(createServerID("00:00:00:00:00:0"+String.valueOf(hashMac(mac))));

        for(Node n:((TypeOnlyNode)bulkLeaseQueryReply).getSubNodes())
        {
          nodesToBeSent.add(n);
        }
        // Vom Workflow erhaltene DHCP Optionen von Nodes in Bytestrom
        // konvertieren
        sendBulkLeaseQuery(tc, transactionid, mac, nodesToBeSent, msgtype, closesocketaftersend);
        
        
        // nun LeaseData Nachrichten senden
        
        for(Node n:resultlist)
        {
          msgtype=17;
          if(n.getTypeName().equals("LeaseQueryData"))
          {
            nodesToBeSent.clear();
            for(Node sn:((TypeOnlyNode)n).getSubNodes())
            {
              nodesToBeSent.add(sn);
            }
            sendBulkLeaseQuery(tc, transactionid, mac, nodesToBeSent, msgtype, closesocketaftersend);
          }
        }
        // Abschlussnachricht
        
        msgtype=16; // LeaseQUERY-DONE 
        nodesToBeSent.clear();
        closesocketaftersend=true;
        sendBulkLeaseQuery(tc, transactionid, mac, nodesToBeSent, msgtype, closesocketaftersend);
        
      }
      
      
      
    }

  }


  private void sendBulkLeaseQuery(DHCPv6TCPTriggerConnection tc, byte[] transactionid, String mac,
                                  List<Node> resultlist, int msgtype, boolean closesocketaftersend) {
    DHCPv6ConfigurationEncoder enc = tc.getEncoder();

    if (tc.getTransactionidInBytes() != null) // TransactionID in Bytes gespeichert => LeaseQuery
    {
      transactionid = tc.getTransactionidInBytes();
    }

    
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    if(resultlist.size()>0)
    {
      if (logger.isDebugEnabled())
        logger.debug("(" + mac + ") Nodelist just before encoding it:");
      printNodes(resultlist, mac);

      try {
        enc.encode(resultlist, output);
      }
      catch (Exception e) {
        // if (logger.isDebugEnabled())
        logger.warn("(" + mac + ") Encoding of received DHCP Options failed! " + e);
      }
    }
    else
    {
      if (logger.isDebugEnabled())
        logger.debug("(" + mac + ") Empty Nodelist to Encode, probably BulkLeaseQuery-DONE");
      
    }

    byte[] optionen = output.toByteArray();
    byte[] data = {};

    byte[] length = ByteUtil.toByteArray(optionen.length+4, 2); //msgtype(1)+transactionid(3)+optionslaenge
    // if (logger.isDebugEnabled())
    // logger.debug("Received nodes of workflow output succesfully converted and DHCPv6 Header set!");

    try {
      data = createDHCP6BulkLeaseQueryReply(length,msgtype, transactionid, optionen);
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      logger.warn("(" + mac + ") Creation of DHCPv6 Bulk Leasequery Message failed! ",e);
    }

    if (data.length != 0) {
        tc.sendTCP(data, closesocketaftersend); // Antwort an Client, der LeaseQuery verschickt hat
    }
    else {
      logger.warn("DHCP Message to send empty?!");
    }
  }


  private Node createServerID(String servermac) {

    String parts[] = servermac.split(":");

    servermac = "0x";
    int tmp;
    String hex;
    for (String p : parts) {
      tmp = Integer.parseInt(p, 16);
      hex = Integer.toHexString(tmp);
      if (hex.length() < 2)
        servermac = servermac + "0";
      servermac = servermac + hex.toUpperCase();
    }

    List<Node> subnodes = new ArrayList<Node>();
    List<Node> subsubnodes = new ArrayList<Node>();

    subsubnodes.add(new TypeWithValueNode("HardwareType", "1"));
    subsubnodes.add(new TypeWithValueNode("Time", "1000"));
    subsubnodes.add(new TypeWithValueNode("LinkLayerAddress", servermac));

    subnodes.add(new TypeOnlyNode("DUID-LLT", subsubnodes));

    Node sid = new TypeOnlyNode("ServerID", subnodes);
    return sid;

  }


  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * 
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, DHCPv6TCPTriggerConnection tc) {
//    StringBuilder errors = new StringBuilder();
//    for (XynaException xynaException : e) {
//      errors.append(xynaException.getCode()).append("  ");
//      // logger.warn(xynaException);
//    }
//    logger.error("The following problem(s) occurred: " + errors.toString());
    
    for (XynaException xynaException : e) {
      logger.error("The following problem(s) occurred: ",xynaException);
    }


  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    // TODO implementation
    // TODO update dependency xml file
    return null;
  }


  /*
   * public byte[] createDHCP6Message(byte[] options, int msgtype, byte[] transactionid) throws Exception { // DHCPv6
   * Nachricht generieren ByteArrayOutputStream message = new ByteArrayOutputStream(); // Uebergebene Paraemter und
   * Optionen anhaengen try { message.write(msgtype); message.write(transactionid); message.write(options); } catch
   * (IOException e1) { // TODO Auto-generated catch block e1.printStackTrace(); } byte [] data = message.toByteArray();
   * return data; }
   */

  public byte[] createDHCP6RelayMessage(int type, int hops, byte[] link, byte[] peer, byte[] options) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    try {
      message.write(type); // DHCPv6 Nachrichtentyp
      message.write(hops);

      message.write(link);
      message.write(peer);

      // Uebergebene Optionen anhaengen
      message.write(options);

    }
    catch (IOException e1) {
      // TODO Auto-generated catch block
      if (logger.isDebugEnabled())
        logger.debug("Exception while creating Relay Reply Message: " + e1);
    }

    byte[] data = message.toByteArray();
    return data;
  }


  public byte[] createDHCP6BulkLeaseQueryReply(byte[] length, int type, byte[] transactionid, byte[] options) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    try {
      message.write(length);
      message.write(type); // DHCPv6 Nachrichtentyp
      message.write(transactionid);

      // Uebergebene Optionen anhaengen
      message.write(options);

    }
    catch (IOException e1) {
      if (logger.isDebugEnabled())
        logger.debug("Exception while creating LeaseQuery Reply Message: " + e1);
    }

    byte[] data = message.toByteArray();
    return data;
  }


  public static int hashMac(byte[] mac) {
    int count = -1;
    int result = -1;
    if (mac.length >= 6) {
      int a = unsignedByteToInt(mac[4]);
      int b = unsignedByteToInt(mac[5]);
      count = Integer.bitCount(a) + Integer.bitCount(b);
      if ((count % 2) == 0) {
        result = 0;
      }
      else {
        result = 1;
      }

      if (logger.isDebugEnabled()) {
        String first = Integer.toBinaryString(a);
        int len = first.length();
        for (int i = 0; i < 8 - len; i++) {
          first = "0" + first;
        }

        String second = Integer.toBinaryString(b);
        len = second.length();
        for (int i = 0; i < 8 - len; i++) {
          second = "0" + second;
        }

        logger.debug("Last 16 bits of mac: " + first + " " + second);
        logger.debug("Nr of 1 bits : " + count);
        logger.debug("=> " + result);
      }
    }

    return result;
  }


  public static int hashMac(String mac) {

    String values = mac.substring(2);
    String tmp = "";
    if (values.length() == 12) {
      byte[] macinbytes = new byte[6];

      for (int i = 0; i < macinbytes.length; i++) {
        tmp = values.substring(i * 2, (i * 2) + 2);
        macinbytes[i] = (byte) (Integer.parseInt(tmp, 16));
      }
      return hashMac(macinbytes);
    }

    return -1;
  }


  public static int unsignedByteToInt(byte b) {
    return (int) b & 0xFF;
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> list = new ArrayList<String>();
    // list.add(XYNA_PROPERTY_RESET);
    list.add(XYNA_PROPERTY_DHCP_HASHV6);
    list.add(XYNA_PROPERTY_DHCP_HASHV6PASSVAL);
    list.add(XYNA_PROPERTY_DHCP_LOCKFILTER);
    return list;
  }


  public void propertyChanged() {
    hashcheck = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_DHCP_HASHV6);
    hashpassval = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_DHCP_HASHV6PASSVAL);
    lockfilter = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_DHCP_LOCKFILTER);

  }


  public void printNodes(List<Node> input, String mac) {
    for (Node n : input) {
      if (logger.isDebugEnabled())
        logger.debug("(" + mac + ") Nodename: " + n.getTypeName());
      if (n instanceof TypeWithValueNode) {
        if (logger.isDebugEnabled())
          logger.debug("(" + mac + ") Value: " + ((TypeWithValueNode) n).getValue());
      }
      else {
        if (logger.isDebugEnabled())
          logger.debug("(" + mac + ") Value: Subnodes: ");
        printNodes(((TypeOnlyNode) n).getSubNodes(), mac);
      }
    }
  }

}
