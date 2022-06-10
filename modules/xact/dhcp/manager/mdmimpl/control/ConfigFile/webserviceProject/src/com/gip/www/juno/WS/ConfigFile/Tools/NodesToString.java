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
package com.gip.www.juno.WS.ConfigFile.Tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import xact.dhcp.hashmaputils.HashMapSerializer;

import com.gip.juno.ws.exceptions.DPPWebserviceException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.xyna.xact.trigger.tlvdecoding.util.ByteUtil;
import com.gip.xyna.xact.trigger.tlvencoding.dhcp.DHCPConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeOnlyNode;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TypeWithValueNode;





public class NodesToString {

  
  
  public static String generateDppguid(final String macAddress) {
    if (macAddress == null) {
      throw new IllegalArgumentException("Mac address may not be null.");
    }
//    else if (!macAddress.matches("[0-9A-F]{2}(:[0-9A-F]{2}){5}")) {
//      throw new IllegalArgumentException("Invalid mac address: <" + macAddress + ">.");
//    }

    String input = macAddress.replace(":", "");
    // System.out.println(input);
    long maclong = Long.parseLong(input, 16);

    long res = ((maclong % 1000000000) * 3) ^ 3494692721L;


    long res2 = (1721966L << 32) + res;

    String hexres = Long.toHexString(res2);

    while (hexres.length() < 16) {
      hexres = "0" + hexres;
    }

    StringBuilder sb = new StringBuilder();
    Formatter format = new Formatter(sb);

    format.format("%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c", hexres.charAt(14), hexres.charAt(15), hexres.charAt(8), hexres
                    .charAt(9), hexres.charAt(12), hexres.charAt(13), hexres.charAt(10), hexres.charAt(11), hexres
                    .charAt(6), hexres.charAt(7), hexres.charAt(4), hexres.charAt(5), hexres.charAt(2), hexres
                    .charAt(3), hexres.charAt(0), hexres.charAt(1)); //+1);

    return sb.toString().toUpperCase();
  }

  
  private static String getMACfromOptions(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        ArrayList<Node> inputnodes = new ArrayList(((TypeOnlyNode) relaymsg).getSubNodes());
        for (Node node : inputnodes) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CLIENTID)) {
            ton = (TypeOnlyNode) node;
            ArrayList<Node> subnodes = new ArrayList(ton.getSubNodes());
            for (Node subnode : subnodes) {
              if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDLLT) || subnode.getTypeName()
                              .equalsIgnoreCase(DHCPv6Constants.DUIDLL)) {
                ton2 = (TypeOnlyNode) subnode;
                ArrayList<Node> subsubnodes = new ArrayList(ton2.getSubNodes());
                for (Node subsubnode : subsubnodes) {
                  if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKLAYERADDR)) {
                    twvn = (TypeWithValueNode) subsubnode;
                    return twvn.getValue().substring(2).toLowerCase();
                  }
                }
              }
              else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDEN)) {

                // TODO: wo bekommt man hier die MAC-Adresse des Clients her?

              }
            }
          }
        }
      }
    }

    return null;
  }

  private static String getVendorClassOption(List<? extends Node> inputnodes) throws Exception{


    try {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
          ArrayList<Node> subnodes = new ArrayList(((TypeOnlyNode) node).getSubNodes());
          for (Node subnode : subnodes) {
            if (subnode.getTypeName().contains(DHCPv6Constants.VENDORCLASS)) {
              if (subnode instanceof TypeWithValueNode) {
                String vcdata = ((TypeWithValueNode) subnode).getValue();
                   
                if(!vcdata.toLowerCase().contains("docsis")&&!vcdata.toLowerCase().contains("pktc"))
                   {
                  throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00216").setDescription("VendorClass enthaelt weder docsis noch pktc."));
                   }
                String enterprisenr = subnode.getTypeName().substring(11);
                return enterprisenr + ";" + vcdata;
              }
              if (subnode instanceof TypeOnlyNode) {
                String enterprisenr = subnode.getTypeName().substring(11);
                
                //return enterprisenr + ";";
                throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00216").setDescription("VendorClass enthaelt weder docsis noch pktc."));              }

            }
          }
        }
      }
    }
    catch (Exception e) {
      throw e;
    }

    return null;
  }

  
  private static String getClientIpsFromRequest(List<? extends Node> inputnodes) {

    String res = "";
    String prefixlength = "";
    for (Node node : inputnodes) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        ArrayList<Node> subnodes = new ArrayList(((TypeOnlyNode) node).getSubNodes());
        for (Node subnode : subnodes) {
          if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA) || subnode.getTypeName()
                          .equalsIgnoreCase(DHCPv6Constants.IATA)) {
            ArrayList<Node> ianaSubnodes = new ArrayList(((TypeOnlyNode) subnode).getSubNodes());
            for (Node ianaSubnode : ianaSubnodes) {
              if (ianaSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAADDR)) {
                ArrayList<Node> iaaddrSubnodes = new ArrayList(((TypeOnlyNode) ianaSubnode).getSubNodes());
                for (Node iaaddrSubnode : iaaddrSubnodes) {
                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
                    res = res + ((TypeWithValueNode) iaaddrSubnode).getValue() + ",";
                  }
                }
              }
            }
          }
          else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
            prefixlength = "";
            ArrayList<Node> ianaSubnodes = new ArrayList(((TypeOnlyNode) subnode).getSubNodes());

            for (Node ianaSubnode : ianaSubnodes) {

              if (ianaSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPREF)) {
                ArrayList<Node> iaaddrSubnodes = new ArrayList(((TypeOnlyNode) ianaSubnode).getSubNodes());
                for (Node iaaddrSubnode : iaaddrSubnodes) {
                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.PREFLENGTH)) {
                    prefixlength = ((TypeWithValueNode) iaaddrSubnode).getValue();
                  }

                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
                    res = res + ((TypeWithValueNode) iaaddrSubnode).getValue() + "/" + prefixlength + ",";
                  }
                }
              }
            }


          }
        }
      }
    }
    if (res.length() > 0)
      res = res.substring(0, res.length() - 1);
    return res;
  }


  private static HashMap getVendorSpecificInformationAsHashmap(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    HashMap result = new HashMap();


    try {
      for (Node relaymsg : inputoptions) {
        if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
          ArrayList<Node> inputnodes = new ArrayList(((TypeOnlyNode) relaymsg).getSubNodes());
          for (Node node : inputnodes) {
            if (node.getTypeName().contains(DHCPv6Constants.VENDORSPECINFO)) {
              ton = (TypeOnlyNode) node;
              ArrayList<Node> subnodes = new ArrayList(ton.getSubNodes());
              for (Node subnode : subnodes) {
                if (subnode instanceof TypeWithValueNode) {
                  twvn = (TypeWithValueNode) subnode;
                  result.put(twvn.getTypeName(), twvn.getValue());

                }
                else {
                  HashMap subresult = new HashMap();

                  ton = (TypeOnlyNode) subnode;
                  for (Node newsubnode : ton.getSubNodes()) {
                    subresult.put(newsubnode.getTypeName(), ((TypeWithValueNode) newsubnode).getValue());
                  }
                  result.put(ton.getTypeName(), subresult);
                }

              }
            }
          }
        }
      }
    }
    catch (Exception e) {
    }

    return result;
  }

  
  public static String generateString(List<? extends Node> requestoptions, List<? extends Node> replyoptions) throws Exception {

    String debugmac="";
    debugmac = getMACfromOptions(requestoptions); 



    // InputNodes: falsche Nodes entfernen



    String message = "";

    String[] argu = new String[9];

    // Start der Nachricht
    argu[0] = "Start";

    // Laenge
    argu[1] = ""; // Laenge muss spaeter gesetzt werden

    // Request Type
    argu[2] = "Dhcpd";

    // Client IP
    argu[3] = getClientIpsFromRequest(replyoptions);
    if (argu[3] == null) {
      argu[3] = "";
    }


    // Client Mac
    argu[4] = debugmac;
    if (argu[4] == null) {
      argu[4] = "";
    }


    // Vendor Class Option 16
    argu[5] = getVendorClassOption(requestoptions);
    // if (!(argu[5] == null)) {
    // String hex = argu[5];
    // byte[] tmpbytearray = ByteUtil.toByteArray(hex);
    //
    // String res = "";
    //
    // for (byte b : tmpbytearray) {
    // res = res + String.valueOf((char) b);
    // }
    // argu[5] = res;
    // }

    if (argu[5] == null)
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F").setErrorNumber("00216").setDescription("VendorClass enthaelt weder docsis noch pktc."));
      //throw new Exception("Daten stammen nicht von CM oder MTA. Daher kann kein ConfigFile erstellt werden.");


    // VendorSpecificInformation (17)
    argu[6] = "";
    HashMap map = getVendorSpecificInformationAsHashmap(requestoptions);
    if (map != null) {
      argu[6] = new HashMapSerializer().serialize(map);
    }


    // dppguid

    argu[7] = "";


    argu[7] = generateDppguid(argu[4]) + ".cfg";


    // Ende der Nachricht
    argu[8] = "eol";

    for (String s : argu) // Nachricht zum ersten mal bauen fuer Ermittlung der
    // Laenge
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    String len = Integer.toString(message.length());
    int tmp = Integer.parseInt(len) + 4;
    argu[1] = Integer.toString(tmp);
    
    while(argu[1].length()<4)
    {
      argu[1] = "0"+argu[1];
    }

    message = "";
    for (String s : argu) // Nachricht zum zweiten mal mit Laenge bauen
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";


    return message;
  }
  
  private static Logger logger = Logger.getLogger("ConfigFile");
  private static void printNodes(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> requestoptions){
    for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : requestoptions){
      logger.info("got node: " +node.getTypeName());
    }
  }
  
  /**
   * Dieser Code ist aus DHCPv4ServicesImpl entnommen 
   * - es wird ein String generiert, um den ConfigFileGenerator zu fuettern
   * @param auditEntry 
   * @param encv4 
   */
//  public static String generateStringInputForConfigFileGenV4(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> requestoptions, List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> replyoptions, AuditDhcpPacketDatatype auditEntry, DHCPConfigurationEncoder encv4) throws Exception {
//    
//    logger.info("request nodes");
//    printNodes(requestoptions);
//    logger.info("reply nodes");
//    printNodes(replyoptions);
//    
//    String message = "";
//
//    String[] argu = new String[9];
//
//    // Start der Nachricht
//    argu[0] = "Start";
//
//    // Laenge
//    argu[1] = ""; // Laenge muss spaeter gesetzt werden
//
//    // Request Type
//    argu[2] = "Dhcpd";
//
//    // Client IP
//    argu[3] = auditEntry.getIp();//getYIAddress(replyoptions);
//    if (argu[3] == null) {
//      argu[3] = "";
//    }
// 
//    // Client Mac
//    argu[4] = getMACfromOptionsV4(auditEntry);//getMACfromOptionsV4(requestoptions);
//    if (argu[4] == null) {
//      argu[4] = "";
//    }
//    
//    // VendorSpecificInformation (43)
//    argu[5] = "";
//    //com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node VSI = getVendorSpecificInformationNode(requestoptions,encv4);
//    String VSI = getVendorSpecificInformation(requestoptions,encv4);
//    if(VSI!=null)
//    {
//      argu[5] = VSI.toLowerCase();//((com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode)VSI).getValue().toLowerCase(); //Hexcodierung der Option
//      argu[5] = argu[5].substring(2); //0x abhacken
//      StringBuilder sb = new StringBuilder();
//      for(int i=0;i<argu[5].length()-1;i=i+2)
//      {
//        sb.append(argu[5].substring(i,i+2)).append(":"); //jede zweite Stelle :
//      }
//      sb.deleteCharAt(sb.length()-1); // letzten : entfernen
//      argu[5]=sb.toString();
//    }
// 
//    // Vendor Class Option 60
//    logger.info("Entering getVendorClassOptionV4");
//    argu[6] = getVendorClassOptionV4(requestoptions);    
//    if (argu[6] == null) {
//      throw new DPPWebserviceException(new MessageBuilder().setDomain("F")
//          .setErrorNumber("00216").setDescription(
//              "VendorClass enthaelt weder docsis noch pktc."));
//    }
//    
//    // dppguid
//    argu[7] = "";
//    argu[7] = getCfgFileName(replyoptions);
//
//    // Ende der Nachricht
//    argu[8] = "eol";
//
//    for (String s : argu) // Nachricht zum ersten mal bauen fuer Ermittlung der
//    // Laenge
//    {
//      message = message + s + "\t";
//    }
//    message = message.substring(0, message.length() - 1);
//    message = message + "\n";
//
//    String len = Integer.toString(message.length());
//    int tmp = Integer.parseInt(len) + 4;
//    argu[1] = Integer.toString(tmp);
//    
//    while(argu[1].length()<4)
//    {
//      argu[1] = "0"+argu[1];
//    }
//
//    message = "";
//    for (String s : argu) // Nachricht zum zweiten mal mit Laenge bauen
//    {
//      message = message + s + "\t";
//    }
//    message = message.substring(0, message.length() - 1);
//    message = message + "\n";
//
//    return message;
//  }
  
public static String generateStringInputForConfigFileGenV4(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> requestoptions, List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> replyoptions, AuditDhcpPacketDatatype auditEntry, String[] vendorClassAndVendorSpecInfoStrings) throws Exception {
    

    
    String message = "";

    String[] argu = new String[9];

    // Start der Nachricht
    argu[0] = "Start";

    // Laenge
    argu[1] = ""; // Laenge muss spaeter gesetzt werden

    // Request Type
    argu[2] = "Dhcpd";

    // Client IP
    argu[3] = auditEntry.getIp();//getYIAddress(replyoptions);
    if (argu[3] == null) {
      argu[3] = "";
    }
 
    // Client Mac
    argu[4] = getMACfromOptionsV4(auditEntry);//getMACfromOptionsV4(requestoptions);
    if (argu[4] == null) {
      argu[4] = "";
    }
    
    // VendorSpecificInformation (43)
    argu[5] = "";
    //com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node VSI = getVendorSpecificInformationNode(requestoptions,encv4);
    String VSI = vendorClassAndVendorSpecInfoStrings[1];
    if(VSI!=null)
    {
      argu[5] = VSI.toLowerCase();////Hexcodierung der Option
      argu[5] = argu[5].substring(6); //0x abhacken und die ersten Type- und Length-Bytes
      StringBuilder sb = new StringBuilder();
      for(int i=0;i<argu[5].length()-1;i=i+2)
      {
        sb.append(argu[5].substring(i,i+2)).append(":"); //jede zweite Stelle :
      }
      sb.deleteCharAt(sb.length()-1); // letzten : entfernen
      argu[5]=sb.toString();
    }
 
    // Vendor Class Option 60

    argu[6] = getVendorClassOptionV4(requestoptions, vendorClassAndVendorSpecInfoStrings[0]);    
    if (argu[6] == null) {
      throw new DPPWebserviceException(new MessageBuilder().setDomain("F")
          .setErrorNumber("00216").setDescription(
              "VendorClass enthaelt weder docsis noch pktc."));
    }
    
    // dppguid
    argu[7] = "";
    argu[7] = getCfgFileName(replyoptions);
    if (argu[7].equals("")){//einen kuenstlichen ConfigFileName erstellen und verwenden
      logger.info("Kein ConfigFile-Name in Dhcp-Packet angegeben - Verwendung eines kuenstlich generierten Namens");
      String hostAsName = auditEntry.getHost();
      if (hostAsName==null || hostAsName.equals("")){
        hostAsName = "artificialConfigName";
      }
      argu[7] = hostAsName + ".cfg";
    }

    // Ende der Nachricht
    argu[8] = "eol";

    for (String s : argu) // Nachricht zum ersten mal bauen fuer Ermittlung der
    // Laenge
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    String len = Integer.toString(message.length());
    int tmp = Integer.parseInt(len) + 4;
    argu[1] = Integer.toString(tmp);
    
    while(argu[1].length()<4)
    {
      argu[1] = "0"+argu[1];
    }

    message = "";
    for (String s : argu) // Nachricht zum zweiten mal mit Laenge bauen
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    return message;
  }
  
private static String getMACfromOptionsV4(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions) {
    for(com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node chaddr : inputoptions) {
      if(chaddr.getTypeName().equalsIgnoreCase(DHCPv4Constants.CLIENT_HW_ADDR)) {
         return ((com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode) chaddr).getValue();
      }
    }
    return null;   
  }

private static String getMACfromOptionsV4(AuditDhcpPacketDatatype auditEntry) {
  String host = auditEntry.getHost();
  if ((host != null) && (!host.equals("")) && (host.length() == 12)){
   return host.substring(0, 2) + ":" + host.substring(2, 4) + ":" + host.substring(4, 6) + ":" + host.substring(6, 8) + ":" + host.substring(8, 10) + ":" + host.substring(10, 12);
  }
  return null;   
}

private static String getYIAddress(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions) {
  
  for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : inputoptions) {
    if (node.getTypeName().equalsIgnoreCase(DHCPv4Constants.YIADDR)) {
      return ((com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode)node).getValue();
    }
  }
  
  return null;
}

//private static com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node getVendorSpecificInformationNode(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions, DHCPConfigurationEncoder encv4)
private static String getVendorSpecificInformation(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions, DHCPConfigurationEncoder encv4)
{
  
  for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : inputoptions) {
    if (node.getTypeName().contains(DHCPv4Constants.VENDORSPECINFO)) {
      byte[] vsiAsBytes = encodeNode(node,encv4);

      String result = "";
      result = ByteUtil.toHexValue(vsiAsBytes);


      return result;
    }
  }
  
//  for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : inputoptions) {
//    if (node.getTypeName().contains(DHCPv4Constants.VENDORSPECIFICSTRING)) {
//      return node;
//    }
//  }
  return null;
}

  private static byte[] encodeNode(
    com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node,
    DHCPConfigurationEncoder encv4) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> list= new ArrayList<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node>();
    list.add(node);
    try {
      encv4.encode(list, output);
    } catch (IOException e) {
      e.printStackTrace();
        logger.error("Encoding of VendorSpecificInformation failed!");
        throw new RuntimeException("Encoding of VendorSpecificInformation failed!",e);
    }
    
    byte[] option = output.toByteArray();
  return option;
}


  private static String getVendorClassOptionV4(
      List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions, String vendorClassString) {

    String typeversion = vendorClassString.split(":")[0];

    String hashmap = "";
    try {
      for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : inputoptions) {
        if (node.getTypeName().contains(DHCPv4Constants.VENDORCLASSIDENTIFIER)) {
          
          hashmap = new HashMapSerializer().serialize(getNodeAsHashmap(node));
        }
      }
      return "\"" + typeversion + ":" + hashmap + "\"";
    } catch (Exception e) {
      return null;
    }
}

  
  
  
  private static HashMap getNodeAsHashmap(
      com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node inputnode) {
    com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeOnlyNode ton;
    com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode twvn;

    HashMap result = new HashMap();

    
    try {
      ton = (com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeOnlyNode) inputnode;
      //ArrayList<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> subnodes = (ArrayList<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node>) ton.getSubNodes();
      List<com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> subnodes = ton.getSubNodes();
     
      for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node subnode : subnodes) {

        if (subnode instanceof com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode) {
          twvn = (com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode) subnode;
          
          result.put(twvn.getTypeName(), twvn.getValue());

        } else {
          HashMap subresult = new HashMap();
          
          ton = (com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeOnlyNode) subnode;
          
          for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node newsubnode : ton.getSubNodes()) {
            
            subresult
                .put(
                    newsubnode.getTypeName(),
                    ((com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode) newsubnode)
                        .getValue());
          }
          
          result.put(ton.getTypeName(), subresult);
        }
      }
    } catch (Exception e) {
      logger.error("Exception occurred", e);
    }

    return result;
  }

  
  private static final Pattern QUOTE_PATTERN = Pattern.compile("\"(.*)\"");

  private static String getCfgFileName(List<? extends com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node> inputoptions) {
    for (com.gip.xyna.xact.trigger.tlvencoding.dhcp.Node node : inputoptions) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv4Constants.DHCP_OPTION_BOOTFILENAME)) {
        String filename = ((com.gip.xyna.xact.trigger.tlvencoding.dhcp.TypeWithValueNode)node).getValue();
        
        Matcher matcher = QUOTE_PATTERN.matcher(filename);
        if (matcher.matches()){
          filename = matcher.group(1);
        }
 
        return filename;
      }
    }
    return "";
  }

}
