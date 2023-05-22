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

package xdnc.dhcpv6.v6constants;

import java.util.HashMap;
import java.util.Map;


public class DHCPv6Constants {

  public static final String CLIENTID = "ClientID";
  public static final String SERVERID = "ServerID";
  public static final String DUIDLLT = "DUID-LLT";
  public static final String DUIDEN = "DUID-EN";
  public static final String DUIDLL = "DUID-LL";
  public static final String HARDWARETYPE = "HardwareType";
  public static final String DUIDTIME = "Time";
  public static final String LINKLAYERADDR = "LinkLayerAddress";
  public static final String MSGTYPE = "InnerType";
  public static final String MSGTYPE_SOLICIT = "1";
  public static final String MSGTYPE_ADVERTISE = "2";
  public static final String MSGTYPE_REQUEST = "3";
  public static final String MSGTYPE_REPLY = "7";
  public static final String MSGTYPE_RENEW = "5";
  public static final String MSGTYPE_REBIND = "6";
  public static final String MSGTYPE_RELEASE = "8";
  public static final String MSGTYPE_DECLINE = "9";
  public static final String TXID = "TXID";
  public static final String RELAYMESSAGE = "RelayMessage";
  public static final String INTERFACEID = "InterfaceID";
  public static final String IAPD = "IA_PD";
  public static final String IANA = "IA_NA";
  public static final String IATA = "IA_TA";
  public static final String IAADDR = "IA_Address";
  public static final String IAPREF = "IAPrefix";
  public static final String PREFLENGTH = "PrefixLength";
  public static final String IPv6 = "IPv6";
  public static final String IAID = "IAID";
  public static final String STATUSCODE = "StatusCode";//Octetstring - die ersten 2 Byte bezeichnen den Status Code, die nachfolgenden die Status-Message, z.B. 0x0002 fuer NoAddrsAvail ohne Message
  public static final String STATUS_NOADDRSAVAIL = "0x0002";
  public static final String STATUS_NOBINDING = "0x0003";
  public static final String VALIDLEASETIME = "T2";//DHCPv6-Option, die als "valid lease time" auftritt
  public static final String PREFLEASETIME = "T1";//DHCPv6-Option, die als "valid lease time" auftritt
  public static final String LINKADDR = "LinkAddress";
  public static final String REQUESTLIST = "RequestList";
  public static final String VENDORSPECINFO = "VendorSpecificInformation";
  public static final String VENDORSPECINFOCODE = "17";
  public static final String VENDORCLASS = "VendorClass";
  public static final String VENDORCLASSCODE = "16";
  public static final String ACTIONCODE = "ActionCode";
  public static final String DNSSERVER = "DNSServer";
  public static final String LEASEQUERYOPTION = "LeaseQuery";
  public static final String LEASEQUERYTYPE = "QueryType";
  public static final String LEASEQUERYLINK = "QueryLinkAddress";
  public static final String DODNS = "DoDNS";
  public static final String IANAT2 = "IA_NA.T2";
  public static final String IATAT2 = "IA_TA.T2";
  public static final String IAPDT2 = "IA_PD.T2";
  public static final String IANAT1 = "IA_NA.T1";
  public static final String IATAT1 = "IA_TA.T1";
  public static final String IAPDT1 = "IA_PD.T1";
  public static final String IAADDRT2 = "IA_Address.T2";
  public static final String IAADDRT1 = "IA_Address.T1";
  public static final String IAPREFIXT2 = "IAPrefix.T2";
  public static final String IAPREFIXT1 = "IAPrefix.T1";
  public static final String IANACODE = "3";
  public static final String IATACODE = "4";
  public static final String IAPDCODE = "25";
  public static final String CMTSRELAYID = "RelayID";
  public static final String CMTSREMOTEID = "RemoteID";
  
  
  
  //Suboptionen f�r Option 17 - VendorSpecific Information
  public static final String SUB1732 = "CL_OPTION_TFTP_SERVERS";
  public static final String SUB1733 = "CL_OPTION_CONFIG_FILE_NAME";
  public static final String SUB1734 = "CL_OPTION_SYSLOG_SERVERS";
  public static final String SUB1737 = "OPTION_RFC868_SERVERS";
  public static final String SUB1738 = "CL_OPTION_TIME_OFFSET";
  public static final String SUB171025 = "CL_OPTION_DOCS_CMTS_CAP";
  public static final String SUB171026 = "CL_CM_MAC_ADDR";
  public static final String SUB172170 = "CL_IPv4ForEMTAs";
  public static final String VENDORCLASSDATA = "Data";
  
  public static Map<String, String> buildCodeToOptionMap(){
    Map<String, String> codeToOptionMap = new HashMap<String, String>();
    
    codeToOptionMap.put("1", DHCPv6Constants.CLIENTID);
    codeToOptionMap.put("2", DHCPv6Constants.SERVERID);
    codeToOptionMap.put("3", DHCPv6Constants.IANA);
    codeToOptionMap.put("3.2", DHCPv6Constants.PREFLEASETIME);
    codeToOptionMap.put("3.3", DHCPv6Constants.VALIDLEASETIME);
    codeToOptionMap.put("4", DHCPv6Constants.IATA);
    codeToOptionMap.put("4.2", DHCPv6Constants.PREFLEASETIME);
    codeToOptionMap.put("4.3", DHCPv6Constants.VALIDLEASETIME);
    codeToOptionMap.put("5.2", DHCPv6Constants.PREFLEASETIME);
    codeToOptionMap.put("5.3", DHCPv6Constants.VALIDLEASETIME);
    codeToOptionMap.put("25", DHCPv6Constants.IAPD);
    codeToOptionMap.put("6", DHCPv6Constants.REQUESTLIST);
    codeToOptionMap.put("17", DHCPv6Constants.VENDORSPECINFO);
    codeToOptionMap.put("23", DHCPv6Constants.DNSSERVER);
    
    return codeToOptionMap;
  }
  
  //moegliche GuiFixedAttribute-Werte
  public static final String FQDN = "FQDN";
  public static final String HEX_FQDN = "HEX_FQDN";
  public static final String ETH1 = "ETH1";
  public static final String ETH1PEER = "ETH1_PEER";
  public static final String ETH2 = "ETH2";
  public static final String ETH2PEER = "ETH2_PEER";
  public static final String ETH2v6 = "ETH2v6";
  public static final String ETH2v6PEER = "ETH2v6_PEER";
  public static final String DOMAINNAME = "DOMAIN_NAME";
  public static final String ETH2L = "ETH2_L";
  public static final String ETH2U = "ETH2_U";
  public static final String DPPGUID = "DPPGUID";
  
  //moegliche GuiAttribute-Wertebereiche
  public static final String STRING_ATTRIBUTE = "String";
  public static final String IPv6_ATTRIBUTE = "IPv6";
  public static final String IPv6List_ATTRIBUTE = "Liste von IPv6s";
  
  //weitere Konstanten
  public static final String TYPE_SOLICIT = "Solicit";
  public static final String TYPE_REQUEST = "Request";
  public static final String TYPE_RENEW = "Renew";
  public static final String TYPE_REBIND = "Rebind";
  public static final String TYPE_DECLINE = "Decline";
  public static final String TYPE_RELEASE = "Release";

  public static final int IPv6ADDRESSLENGTH = 128;
  public static final String IPv6ADDRESSLENGTHSTRING = "128";
  public static final String MAXUNSIGNEDINTEGER = String.valueOf(Integer.MAX_VALUE);//;"4294967295";
  public static final int RESERVEDHOST_EXISTS = 1;
  public static final int RESERVEDHOST_NONEXISTENT = 0;
  
  public static final int PREFIXREQUEST = 1;
  
  public static final String INVALID_MAC = "020000000000";
  
  public static final String POOLTYPE_RESERVEDIP = "Reserved IPs";
  
  //Konstanten fuer die Cluster-Node-Zustaende
  public static final String STATE_RUNNING = "running";
  public static final String STATE_NOPARTNER = "running w/o partner";
  public static final String STATE_RESYNC = "resync";
  public static final String STATE_SINGLE = "single mode";
  public static final String STATE_GOD = "god mode";
  public static final String STATE_DOWN = "down";
  public static final String STATE_SHUTDOWN = "shutdown";
  public static final String STATE_STARTUP = "startup";
  
  //Konstanten fuer interne Workflow-Aufrufe (verwendet in onDeployment)
  public static final String WF_UPDATEGUIDATA = "xact.dhcpv6.UpdateDataFromGui";
  public static final String WF_READPOOLSCONFIG = "xact.dhcp.GenerateIPPools";
  
  // Capacities
  public static final String CAP_LEASEASSIGNMENT = "LeaseAssignment_v6Capacity";
  
}
