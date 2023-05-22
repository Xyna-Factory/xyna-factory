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
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.tlvencoding.dhcp.Node;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTree;
import com.gip.xyna.xact.tlvencoding.dhcp.TextConfigTreeReader;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode;
import com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.tlvencoding.util.ByteUtil;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class DHCPv6Filter extends ConnectionFilter<DHCPv6TriggerConnection> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPv6Filter.class);

  public static final XynaPropertyBoolean XYNA_PROPERTY_DHCP_HASHV6 = new XynaPropertyBoolean("xact.dhcpv6.hashv6", false);
  public static final XynaPropertyInt XYNA_PROPERTY_DHCP_HASHV6PASSVAL = new XynaPropertyInt("xact.dhcpv6.hashv6passval", 0);

  //TODO xynaproperty-enum verwenden
  public static final XynaPropertyString XYNA_PROPERTY_DHCP_LOCKFILTER = new XynaPropertyString("xact.dhcpv6.lockfilter", "FALSE");

  private static final XynaPropertyString XYNA_PROPERTY_SERVERIDENTIFIER = new XynaPropertyString("xact.dhcpv6.serveridentifier", "unknown");
  private static final XynaPropertyString XYNA_PROPERTY_CLUSTERMODE = new XynaPropertyString("xdnc.dhcpv6.config.clustermode", "unknown");

  private static final String GOD_MODE = "DISCONNECTED_MASTER";
  //private static final String JOINED_MODE = "JOINED";
  //private static final String DISJOINED_MODE = "DISJOINED";

  // private static long workflowtotaltime = 0;
  // private static long timescount = 0;


  // private static AtomicLong filterstartcounter = new AtomicLong(0);
  // private static AtomicLong filterendcounter = new AtomicLong(0);

  public void onDeployment(EventListener trigger)
  {
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
  public XynaOrder generateXynaOrder(DHCPv6TriggerConnection tc) throws XynaException, InterruptedException {

    if (XYNA_PROPERTY_DHCP_LOCKFILTER.get().equalsIgnoreCase("true")) {
        if (logger.isDebugEnabled()) {
          logger.debug("DHCPv6Filter: Filter locked. Rejecting Message!");
        }
        return null;
    }


    if (logger.isDebugEnabled()) {
      logger.debug("Node "+XYNA_PROPERTY_SERVERIDENTIFIER.get()+" running in mode: "+XYNA_PROPERTY_CLUSTERMODE.get());
    }
    
    List<? extends xdnc.dhcp.Node> momlist = new ArrayList<xdnc.dhcp.Node>();
    int msgtype = 0;
    int hops;
    byte[] optarg;
    String linkadd = "";
    String peeradd = "";
    String macstring = "";
    String mac="";
    String clientmac = "";
    
    int innermessagetype = -1;
    byte[] transactionid = new byte[3];

    // if (logger.isDebugEnabled())
    // logger.debug("DHCPv6Filter: DHCPv6 packet received!");
    try {
      DatagramPacket d = tc.getRawPacket();
      byte[] data = d.getData();
      // byte[] packetcopy = Arrays.copyOfRange(data, 0, d.getLength());
      // // Buffer 0en abschneiden
      byte[] packetcopy = new byte[d.getLength()];
      System.arraycopy(data, 0, packetcopy, 0, d.getLength());
      data = packetcopy;

      msgtype = data[0];
      if (msgtype != 12 && msgtype != 14)// && msgtype!=13)
      {
        logger.debug("DHCPv6Filter: Received Message is neither Relay Forward nor LeaseQuery!");
        return null;
      }


      if (msgtype == 12) {
        // if (logger.isDebugEnabled())
        // logger.debug("DHCPv6 Messagetype (Relay): " + msgtype);
        hops = data[1];
        byte[] address = new byte[16];

        // address = Arrays.copyOfRange(data,2,18);
        System.arraycopy(data, 2, address, 0, 16);
        linkadd = InetAddress.getByAddress(address).getHostAddress();

        // address = Arrays.copyOfRange(data,18,34);
        System.arraycopy(data, 18, address, 0, 16);
        peeradd = InetAddress.getByAddress(address).getHostAddress();

       // if (logger.isDebugEnabled()) {
          // logger.debug("Hops: " + hops);
          // logger.debug("LinkAddress: " + linkadd);
          // logger.debug("PeerAddress: " + peeradd);


      //  }

        // Relayteil aus Data rausloeschen

        // byte[] datacopy = Arrays.copyOfRange(data, 34, data.length);
        byte[] datacopy = new byte[data.length - 34];
        System.arraycopy(data, 34, datacopy, 0, data.length - 34);

        data = datacopy;
      }
      else if (msgtype == 14) {
        System.arraycopy(data, 1, transactionid, 0, 3);
        tc.setTransactionidInBytes(transactionid);
        // MsgType und TransactionID aus Data rausloeschen
        byte[] datacopy = new byte[data.length - 4];
        System.arraycopy(data, 4, datacopy, 0, data.length - 4);

        data = datacopy;

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

        TextConfigTree tree = new TextConfigTreeReader(new StringReader(decodedData)).read();

        List<Node> nodes = new ArrayList<Node>();

        // Node msgtypenode=new
        // TypeWithValueNode("MessageType",""+msgtype);
        // Node hopsnode=new TypeWithValueNode("Hops",""+hops);
        if (msgtype == 12) // Linkadresse nur bei Relay Nachricht zu Nodes hinzufuegen
        {
          Node linkaddnode = new TypeWithValueNode("LinkAddress", "" + linkadd);

          nodes.add(linkaddnode);
        }
        // Node peeraddnode=new
        // TypeWithValueNode("PeerAddress",""+peeradd);

        // nodes.add(msgtypenode);
        // nodes.add(hopsnode);
        // nodes.add(peeraddnode);

        // DHCPv6 Options

//        String tempvalue;
//        String nodevalue;
//        String resultvalue = "0x";
        innermessagetype = -1;

        boolean leasequeryoption = false;
        boolean serveridoption = false;

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
          if (no instanceof TypeOnlyNode && no.getTypeName().equals("RelayMessage")) {
            List<Node> subnodes = ((TypeOnlyNode) no).getSubNodes();
            List<Node> newnodes = new ArrayList<Node>();

            for (Node sno : subnodes) {

              if (sno.getTypeName().equals("ClientID")) {
                tc.setClientId(sno);
                newnodes.add(sno);
              }
              else if (sno.getTypeName().equals("ServerID")) {
                serveridoption = true;
                newnodes.add(sno);
                
                
                
              
                for (Node n : ((TypeOnlyNode) sno).getSubNodes()) {
                  if (n.getTypeName().equals("DUID-LLT") || n.getTypeName().equals("DUID-LL")) {
                    for (Node sn : ((TypeOnlyNode) n).getSubNodes()) { 
                      if (sn.getTypeName().equals("LinkLayerAddress")) {
                        macstring = ((TypeWithValueNode) sn).getValue();
                        
                        
                        if(XYNA_PROPERTY_DHCP_LOCKFILTER.get().equalsIgnoreCase("associated"))
                        {
                          if(innermessagetype==63 || (innermessagetype==65 && XYNA_PROPERTY_DHCP_HASHV6.get())) //Request oder Renew bei aktiviertem Hashcheck 
                          {
//                            String servermac = tc.getServerMacAddress();
//                            servermac = servermac.replace(":","");
//                            servermac = "0x"+servermac;
                            if(!macstring.equals(XYNA_PROPERTY_SERVERIDENTIFIER.get())) // nicht an diesen Server gerichtet = verwerfen
                            {
//                              String mac="";
                              try {
                                Node clid = tc.getClientId();
                                Node clidsub = ((TypeOnlyNode) clid).getSubNodes().get(0); // DUID LL oder LLT
                                List<Node> clidsubs = ((TypeOnlyNode) clidsub).getSubNodes();
                                for (Node macnode : clidsubs) {
                                  if (macnode.getTypeName().equals("LinkLayerAddress")) {
                                    mac = ((TypeWithValueNode) macnode).getValue();
                                  }
                                }
                              }
                              catch (Exception e) {
                                if (logger.isDebugEnabled())
                                  logger.debug("Could not extract Mac for Debug Messages in Filter!");
                              }

                              if (logger.isDebugEnabled())
                                logger.debug("("+mac+") ServerIdentifier "+macstring+" not matching. Aborting Processing ...");

                              return null;
                            }
                          }
                        }
                        
                      } // ende von "if(sn.getTypeName().equals("LinkLayerAddress")) {"
                      
                      
                      
                    }
                  }
                }
              }
              else if (sno.getTypeName().equals("TXID")) {
                tc.setTransactionId(sno);
              }
              else if (sno.getTypeName().equals("InnerType")) {
                innermessagetype = Integer.parseInt(((TypeWithValueNode) sno).getValue()) + 60;
                newnodes.add(sno);
              }
              else if (sno.getTypeName().contains("VendorClass") && sno instanceof TypeWithValueNode) {

                String vcdata = ((TypeWithValueNode) sno).getValue();
                if (vcdata.startsWith("0x00") && vcdata.length() > 6) {
                  vcdata = "0x" + vcdata.substring(6);
                  byte[] tmpbyte = ByteUtil.toByteArray(vcdata);
                  try {
                    vcdata = new String(tmpbyte, "UTF-8");
                    // vcdata = "\""+vcdata+"\"";
                  }
                  catch (Exception e) {
                    if (logger.isDebugEnabled())
                      logger.debug("Error while converting vendorclass to utf8. Using OctetString ...");
                    vcdata = ((TypeWithValueNode) sno).getValue();
                  }
                }
                TypeWithValueNode mno = new TypeWithValueNode(sno.getTypeName(), vcdata);


                newnodes.add(mno);
              }

              else {
                newnodes.add(sno);
              }
            }

            relaynode = new TypeOnlyNode("RelayMessage", newnodes);
          }
          else if (no instanceof TypeWithValueNode && no.getTypeName().equals("InterfaceID")) {
            tc.setInterfaceId(no);
            nodes.add(no);
          }
          else if (no instanceof TypeOnlyNode && no.getTypeName().equals("ClientID")) {
            tc.setClientId(no);
            nodes.add(no);
          }
          else if (no instanceof TypeOnlyNode && no.getTypeName().equals("LeaseQuery")) {
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
        if (relaynode != null) {
          nodes.add(relaynode); // modifizierte Relaynode verwenden
        }
        else {
          if (logger.isDebugEnabled()) {
            logger.debug("No RelayMessage Node found!");
          }
        }

        if (innermessagetype != -1) // innermessagetype wird bei Relay Nachricht gesetzt
        {
          if (innermessagetype != 61 && innermessagetype != 63 && innermessagetype != 65 && innermessagetype != 66 && innermessagetype != 68 && innermessagetype != 69) {
            if (logger.isDebugEnabled())
              logger.debug("Received RelayMessage did not contain Solicit, Request, Rebind, Release, Decline or Renew. Aborting ...");
            return null;
          }
          if (logger.isDebugEnabled())
            logger.debug("Messagetype: " + msgtype + " (" + (innermessagetype - 60) + "); LinkAddress: " + linkadd + "; PeerAddress: " + peeradd);
        }
        else // kein innermessagetype gesetzt => LeaseQuery
        {
          if (logger.isDebugEnabled())
            logger.debug("Messagetype: " + msgtype + " (LeaseQuery)");
        }

        if (msgtype == 12) {
          Node actioncodenode = new TypeWithValueNode("ActionCode", Integer.toString(innermessagetype));
          nodes.add(actioncodenode);
        }

        // keine ClientID => keine Bearbeitung
        if (tc.getClientId() == null) {
          if (logger.isDebugEnabled())
            logger.debug("No ClientID Option in Message, aborting ...");
          return null;
        }

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
        
        if(clientmac.length()==14) //0x + mac = 14 Stellen
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
        
        
        
        
        // keine ServerID bei Decline => keine Bearbeitung
        if (serveridoption == false && innermessagetype == 69) {
          if (logger.isDebugEnabled())
            logger.debug("No ServerID Option in Decline Message, aborting ...");
          return null;
        }


        // Keine LQ Option bei Leasequery => keine Bearbeitung
        if (msgtype == 14 && leasequeryoption == false) {
          if (logger.isDebugEnabled())
            logger.debug("No LeaseQuery Option in Lease Query Message, aborting ...");
          return null;

        }

        // hier erst Hashpruefung vor weiterer Bearbeitung

        if (XYNA_PROPERTY_DHCP_HASHV6.get() && (innermessagetype == 61 || innermessagetype == 63)) {
          if (logger.isDebugEnabled())
            logger.debug("("+clientmac+")Performing Hash Check ...");

          int hashres = -1;

          hashres = hashMac(clientmac);
          if (hashres != XYNA_PROPERTY_DHCP_HASHV6PASSVAL.get()) {
                    if (logger.isDebugEnabled())
                      logger.debug("("+clientmac+")Hash Check Result " + hashres + ", aborting processing ...");
                    return null;
          }

          if (logger.isDebugEnabled())
            logger.debug("("+clientmac+")Hash Check Result " + hashres + ", continueing processing ...");

        }

        momlist = createMOM(nodes, clientmac);
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


    if(msgtype!=14)
    {
      // ueberpruefen, ob dieser knoten f�r die bearbeitung der anfrage verantwortlich ist
      boolean doIhaveToDoIt = checkResponsibility(XYNA_PROPERTY_CLUSTERMODE.get(), macstring, (innermessagetype-60), clientmac);
              
      if(logger.isDebugEnabled()) {
        logger.debug("("+clientmac+")DHCPv6Filter: Got DHCP Message (Type "+(innermessagetype-60)+"). Mode : "+XYNA_PROPERTY_CLUSTERMODE.get()+" - I will "+(doIhaveToDoIt?"":"NOT ")+"process it");
      }
      
      if(!doIhaveToDoIt)
        return null;
      
    }

    XynaObjectList<xdnc.dhcp.Node> output = new XynaObjectList<xdnc.dhcp.Node>(momlist, xdnc.dhcp.Node.class.getName()); // workaround

    String whichworkflow = "";

    if (msgtype == 12) // DHCPv6 Relay Forw
    {
      whichworkflow = "xdnc.dhcpv6.LeaseAssignment_v6";
    }
    else if (msgtype == 14) // DHCPv6 LeaseQuery
    {
      whichworkflow = "xdnc.dhcpv6.LeaseQuery_v6";
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


  public List<xdnc.dhcp.Node> createMOM(List<Node> l, String mac) {
    List<xdnc.dhcp.Node> moms = new ArrayList<xdnc.dhcp.Node>();
    for (Node n : l) {
      if (convertNode(n, mac) != null)
        moms.add(convertNode(n, mac)); // bei unbekannter Option = null
    }

    return moms;
  }


  // Node => MOM Node

  public xdnc.dhcp.Node convertNode(Node n, String mac) {
    if (n instanceof TypeWithValueNode) {
      if (!(n.getTypeName().equals("Tlv"))) // unbekannte Option
      {
        return new xdnc.dhcp.TypeWithValueNode(n.getTypeName(), ((TypeWithValueNode) n).getValue());
      }
      else {
        logger.warn("("+mac+")Received unknown DHCPv6 Option (not passed to workflow): " + ((TypeWithValueNode) n).getValue());
        return null;
      }
    }
    else if (n instanceof TypeOnlyNode) {
      TypeOnlyNode tonode = (TypeOnlyNode) n;
      List<Node> subNodes = tonode.getSubNodes();
      List<xdnc.dhcp.Node> convertedSubNodes = new ArrayList<xdnc.dhcp.Node>();

      if (subNodes.size() != 0) {
        for (Node z : subNodes) {
          if (convertNode(z, mac) != null) // null wenn unbekannte Option entdeckt
          {
            convertedSubNodes.add(convertNode(z, mac));
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
  public void onResponse(GeneralXynaObject response, DHCPv6TriggerConnection tc) {

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
        logger.debug("("+mac+")DHCPv6Filter:  Could not extract Mac for Debug Messages in Filter!");
    }

    // if(mac.equals("0x000000000065"))
    // {
    // logger.warn("Filtercounter: "+filterstartcounter.get()+" / "+filterendcounter.get());
    // logger.warn("Factoryfiltercounter: "+XynaFactory.filtercounter.get());
    //
    // }

    // tc.setWorkflowendtime(System.currentTimeMillis());

    // long timeneeded = tc.getWorkflowTotalTime();
    // if(timeneeded!=0)
    // {
    // logger.warn("Time needed by called workflow: "+timeneeded+" ms.");
    // workflowtotaltime = workflowtotaltime + timeneeded;
    // timescount++;
    // }
    // if(timescount>5000)
    // {
    // float average = (float)workflowtotaltime / (float) timescount;
    // logger.warn("Average time for workflow (5000) calls : "+average+ " ms.");
    // timescount=0;
    // workflowtotaltime=0;
    // }

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

      if (tc.getTransactionidInBytes() != null) // TransactionID in Bytes gespeichert => LeaseQuery
      {
        transactionid = tc.getTransactionidInBytes();
        resultlist.add(tc.getClientId());
        //resultlist.add(createServerID(tc.getServerMacAddress()));
//        resultlist.add(createServerID("00:00:00:00:00:0"+String.valueOf(hashMac(mac))));
        resultlist.add(createServerID(XYNA_PROPERTY_SERVERIDENTIFIER.get()));
      }

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

      // Nodeliste durchgehen und DHCPv6 Kopf setzen

      int msgtype = -1;
      String linkadd = "";

      String tempvalue;
      for (Node n : convertedlist) {
        // Aufraeumen von Antwort => Entfernung von als Nodes
        // uebergebenen Kopfdaten
        if (n instanceof TypeWithValueNode) {
          tempvalue = ((TypeWithValueNode) n).getValue();

          if (n.getTypeName().equals("MessageType")) {
            resultlist.remove(n);
            // msgtype = Integer.parseInt(tempvalue);
          }
          if (n.getTypeName().equals("TransactionID")) {
            resultlist.remove(n);
            // transactionid = ByteUtil.toByteArray(tempvalue);
          }
          if (n.getTypeName().equals("PeerAddress")) {
            resultlist.remove(n);
            // peeradd = tempvalue;
          }

          // Elemente entfernen, die in Input reingegeben wurden aber
          // keine echten DHCP Optionen sind

          if (n.getTypeName().equals("Hops")) {
            resultlist.remove(n);

          }

          if (n.getTypeName().equals("LinkAddress")) {
            resultlist.remove(n);

          }

        }

        else if (n instanceof TypeOnlyNode) { // Relay Message um
          // zwischengespeicherte
          // TransactionID und
          // ClientID ergaenzt
          if (n.getTypeName().equals("RelayMessage")) {
            List<Node> subnodes = ((TypeOnlyNode) n).getSubNodes();
            List<Node> newnodes = new ArrayList<Node>();

            try {

              for (Node tn : subnodes) {
                newnodes.add(tn);
              }

              if (newnodes.size() == 0) {
                logger.warn("(" + mac + ") Given Response Relay Message empty (has no subnodes), aborting ...");
                return;

              }

              if (tc.getTransactionId() != null)
                newnodes.add(1, tc.getTransactionId());
              if (tc.getClientId() != null)
                newnodes.add(2, tc.getClientId());


              // Server DUID generieren
//              Node serverid = createServerID("00:00:00:00:00:0"+String.valueOf(hashMac(mac)));
              Node serverid = createServerID(XYNA_PROPERTY_SERVERIDENTIFIER.get());
              newnodes.add(3, serverid);

              resultlist.remove(n);
              resultlist.add(new TypeOnlyNode("RelayMessage", newnodes));
            }
            catch (Exception e) {
              logger.warn("(" + mac + ") Something went wrong while processing Relay Message Response from workflow!" + e);
            }
          }
        }

      }

      if (tc.getTransactionidInBytes() == null) {
        msgtype = 13; // Relay Reply
      }
      else {
        msgtype = 15; // LeaseQuery Reply
      }

      // Linkaddresse holen um zu antworten +restliche Kopfdaten

      DatagramPacket d = tc.getRawPacket();
      byte[] packetdata = d.getData();

      int hops = 0;

      byte[] linkaddress = new byte[16];
      byte[] peeraddress = new byte[16];

      if (msgtype == 13) {

        hops = packetdata[1];

        System.arraycopy(packetdata, 2, linkaddress, 0, 16);

        System.arraycopy(packetdata, 18, peeraddress, 0, 16);

        InetAddress linkipadd = null;
        try {
          linkipadd = InetAddress.getByAddress(linkaddress);
        }
        catch (UnknownHostException e1) {
          logger.error("Problems reading Linkaddress",e1);
        }


        if (linkipadd != null)
          linkadd = linkipadd.getHostAddress();

      }
      // Vom Workflow erhaltene DHCP Optionen von Nodes in Bytestrom
      // konvertieren

      DHCPv6ConfigurationEncoder enc = tc.getEncoder();

      ByteArrayOutputStream output = new ByteArrayOutputStream();

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

      byte[] optionen = output.toByteArray();
      byte[] data = {};

      // if (logger.isDebugEnabled())
      // logger.debug("Received nodes of workflow output succesfully converted and DHCPv6 Header set!");

      try {
        if (msgtype == 13) {
          data = createDHCP6RelayMessage(msgtype, hops, linkaddress, peeraddress, optionen, mac);
        }
        else if (msgtype == 15) {
          data = createDHCP6LeaseQueryReply(msgtype, transactionid, optionen, mac);
        }
      }
      catch (Exception e) {
        // TODO Auto-generated catch block
        logger.warn("(" + mac + ") Creation of DHCPv6 Message failed! " + e);
      }

      if (data.length != 0) {
        if (msgtype == 13) {
          if (linkadd != "" && linkadd != "0.0.0.0") {
            tc.sendUDP(linkadd, data, false); // Antwort an Relay Agent
            // filterendcounter.incrementAndGet();
          }
          else {
            logger.warn("No Response IP given (LinkAddress emtpy or LinkAddress=0.0.0.0), no response message sent!");
          }
        }
        else if (msgtype == 15) {
          String targetaddress = d.getAddress().getHostAddress();
          if (targetaddress != "") {
            tc.sendUDP(targetaddress, data, true); // Antwort an Client, der LeaseQuery verschickt hat
          }
          else {
            logger.warn("No IP Address for LeaseQuery Reply given!");
          }

        }
        else {
          logger.warn("Unknown Messagetype: " + msgtype);
        }
      }
      else {
        logger.warn("DHCP Message to send empty?!");
      }

    }

  }


  private Node createServerID(String servermac) {

    if(!servermac.startsWith("0x")) {
      String parts[] = StringUtils.fastSplit(servermac, ':', -1);
    
      servermac = "0x";
      int tmp;
      String hex;
      for (String p : parts) {
        tmp = Integer.parseInt(p, 16);
        hex = Integer.toHexString(tmp);
        if (hex.length() < 2) {
          servermac = servermac + "0";
        }
        servermac = servermac + hex.toUpperCase();
      }
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
  public void onError(XynaException[] e, DHCPv6TriggerConnection tc) {
    for (XynaException xynaException : e) {
      logger.error("The following problem(s) occurred: ",xynaException);
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "DHCPv6 Filter";
  }


  public byte[] createDHCP6RelayMessage(int type, int hops, byte[] link, byte[] peer, byte[] options, String mac) throws Exception {

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
        logger.debug("("+mac+")DHCPv6Filter: Exception while creating Relay Reply Message: " + e1);
    }

    byte[] data = message.toByteArray();
    return data;
  }


  public byte[] createDHCP6LeaseQueryReply(int type, byte[] transactionid, byte[] options, String mac) throws Exception {

    // DHCPv4 Nachricht generieren ohne Optionen generieren
    ByteArrayOutputStream message = new ByteArrayOutputStream();

    try {
      message.write(type); // DHCPv6 Nachrichtentyp
      message.write(transactionid);

      // Uebergebene Optionen anhaengen
      message.write(options);

    }
    catch (IOException e1) {
      if (logger.isDebugEnabled())
        logger.debug("("+mac+")Exception while creating LeaseQuery Reply Message: " + e1);
    }

    byte[] data = message.toByteArray();
    return data;
  }


  public static int hashMac(byte[] mac, String clientmac) {
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

        logger.debug("("+clientmac+")Last 16 bits of mac: " + first + " " + second);
        logger.debug("("+clientmac+")Nr of 1 bits : " + count);
        logger.debug("("+clientmac+")=> " + result);
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
      return hashMac(macinbytes, mac);
    }

    return -1;
  }


  public static int unsignedByteToInt(byte b) {
    return (int) b & 0xFF;
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
  
  private boolean checkResponsibility(String mode, String serverIdentifier, int msgType, String clientmac) {
    
    // im GODMODE wird alles beantwortet
    if(mode.equals(GOD_MODE))
      return true;
    
    // Solicits werden von beiden beantwortet
    if(msgType==1) {
      return true;
    }
    // Rebind ist 6!
    if(msgType==6) {
//      if(mode.equals(JOINED_MODE)) {
//     // Rebinds m�ssen im Service entschieden werden
//        if (logger.isDebugEnabled())
//          logger.debug("("+clientmac+")DHCPv6Filter: Joined Mode | Got msgType "+msgType+" with "+serverIdentifier+" (mine is "+MY_SERVERIDENTIFIER+")");
//        if(MY_SERVERIDENTIFIER.equals(serverIdentifier))
//          return true;
//        else 
//          return false;
//      } else if(mode.equals(DISJOINED_MODE)) {
     // Rebinds m�ssen im Service entschieden werden
        if (logger.isDebugEnabled())
          logger.debug("("+clientmac+")DHCPv6Filter: Disjoined Mode | Got msgType "+msgType);
        return true;
//      }
    }
    
    // Request / Renew sowie Release / Decline nur bei passendem ServerIdentifier
    if(msgType==3 || msgType==9 || msgType==5 || msgType==8) {      
      if (logger.isDebugEnabled())
        logger.debug("("+clientmac+")DHCPv6Filter: Got msgType "+msgType+" with "+serverIdentifier+" (mine is "+XYNA_PROPERTY_SERVERIDENTIFIER.get()+")");
      if(XYNA_PROPERTY_SERVERIDENTIFIER.get().equals(serverIdentifier))
        return true;
      else {
        if (logger.isInfoEnabled()) {
        logger.info("("+clientmac+")DHCPv6Filter: Got msgType "+msgType+" - Message will be skipped due to server id mismatch ("+serverIdentifier+"!="+XYNA_PROPERTY_SERVERIDENTIFIER.get()+")!");
        }
        return false;
      }
        
    }
    
    if (logger.isInfoEnabled()) {
    logger.info("("+clientmac+")DHCPv6Filter: Got msgType "+msgType+" - Message will be skipped!");
    }
    
    // alles andere wird verworfen
    return false;   
  }  
}
