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
package xact.dhcpv6;


import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import java.util.ArrayList;
import java.util.List;


//import xact.dhcp.DHCPv6MessageType;
import xact.dhcp.Node;
import xact.dhcp.PoolType;
import xact.dhcp.TypeOnlyNode;
import xact.dhcp.TypeWithValueNode;


public class TempMethodsDHCPv6Impl implements DeploymentTask {

  protected TempMethodsDHCPv6Impl() {
  }

  public void onDeployment() {
    // TODO do something on deployment, if required
    // this is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() {
    // TODO do something on undeployment, if required
    // this is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public static Container generateInput(List<? extends IAID> requestedIAIDs, List<? extends IP> requestedIPv6Addresses) {
    //TODO implementation
    //TODO update dependency XML
    ArrayList<Node> inputRequest = new ArrayList<Node>();
    ArrayList<Node> formerOutput = new ArrayList<Node>();
    PoolType pooltype = new PoolType();
    
    String iaidIANA1 = requestedIAIDs.get(0).getIaid();
    String ipIANA1 = requestedIPv6Addresses.get(0).getValue();
    String iaidIANA2 = requestedIAIDs.get(1).getIaid();
    String ipIANA2 = requestedIPv6Addresses.get(1).getValue();
    String iaidIAPD1 = requestedIAIDs.get(2).getIaid();
    String ipIAPD1 = requestedIPv6Addresses.get(2).getValue();
    String iaidIAPD2 = requestedIAIDs.get(3).getIaid();
    String ipIAPD2 = requestedIPv6Addresses.get(3).getValue();
    
    TypeWithValueNode linkAddress = new TypeWithValueNode("LinkAddress", "2001:0000:0000:0001:0000:0000:0000:0010");
    inputRequest.add(linkAddress);
    
  //erzeuge VendorSpecificInformation, die parallel zu Relay-Message angehaengt wird
    ArrayList<Node> vsiSubnodes = new ArrayList<Node>();
    TypeWithValueNode cmMAC = new TypeWithValueNode("CL_CM_MAC_ADDR", "00:1d:cd:70:56:6c");
    TypeWithValueNode cmtsCAP = new TypeWithValueNode("CL_OPTION_DOCS_CMTS_CAP", "0xDOCSISVersionNmber3.0");
    vsiSubnodes.add(cmtsCAP);
    vsiSubnodes.add(cmMAC);
    TypeOnlyNode vendorSpecInfo = new TypeOnlyNode("VendorSpecificInformation4491",vsiSubnodes);
    inputRequest.add(vendorSpecInfo);
    
    //TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xaaaa0000aaaa");// reservedHost
    TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xbbbb0000bbbb");
    ArrayList<Node> DUIDchilds = new ArrayList<Node>();
    DUIDchilds.add(linkLayerAddress);
    TypeOnlyNode DUID_LLT = new TypeOnlyNode("DUID-LLT", DUIDchilds);
    ArrayList<Node> ClientIDchilds = new ArrayList<Node>();
    ClientIDchilds.add(DUID_LLT);
    TypeOnlyNode ClientID = new TypeOnlyNode("ClientID", ClientIDchilds);
    TypeWithValueNode innerType = new TypeWithValueNode("InnerType", "1");
    
//    ArrayList<Node> IANAchilds = new ArrayList<Node>();
//    TypeWithValueNode iaid = new TypeWithValueNode("IAID", iaidIANA1);
//    IANAchilds.add(iaid);
//    TypeWithValueNode T1 = new TypeWithValueNode("T1", "3600");
//    IANAchilds.add(T1);
//    TypeWithValueNode T2 = new TypeWithValueNode("T2", "7200");
//    IANAchilds.add(T2);
//    TypeOnlyNode IANA = new TypeOnlyNode("IA_NA", IANAchilds);
    
    ArrayList<Node> IANAchilds2 = new ArrayList<Node>();
    TypeWithValueNode iaid2 = new TypeWithValueNode("IAID", iaidIANA2);
    IANAchilds2.add(iaid2);
    TypeWithValueNode T12 = new TypeWithValueNode("T1", "3600");
    IANAchilds2.add(T12);
    TypeWithValueNode T22 = new TypeWithValueNode("T2", "7200");
    IANAchilds2.add(T22);
    TypeOnlyNode IANA2 = new TypeOnlyNode("IA_NA", IANAchilds2);
    
//    ArrayList<Node> IAPDchilds = new ArrayList<Node>();
//    TypeWithValueNode iaid_pd = new TypeWithValueNode("IAID", iaidIAPD1);
//    IAPDchilds.add(iaid_pd);
//    TypeWithValueNode T1pd = new TypeWithValueNode("T1", "3600");
//    IAPDchilds.add(T1pd);
//    TypeWithValueNode T2pd = new TypeWithValueNode("T2", "7200");
//    IAPDchilds.add(T2pd);
//    TypeOnlyNode IAPD = new TypeOnlyNode("IA_PD", IAPDchilds);
    
    ArrayList<Node> IAPDchilds2 = new ArrayList<Node>();
    TypeWithValueNode iaid_pd2 = new TypeWithValueNode("IAID", iaidIAPD2);
    IAPDchilds2.add(iaid_pd2);
    TypeWithValueNode T1pd2 = new TypeWithValueNode("T1", "3600");
    IAPDchilds2.add(T1pd2);
    TypeWithValueNode T2pd2 = new TypeWithValueNode("T2", "7200");
    IAPDchilds2.add(T2pd2);
    TypeOnlyNode IAPD2 = new TypeOnlyNode("IA_PD", IAPDchilds2);
    
    //TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0001000200030011");
    // request DNSServer-Option 23
    TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0017");
    
    //erzeuge VendorClass-Option
    TypeWithValueNode vendorClass = new TypeWithValueNode("VendorClass4491", "Siemens");

    ArrayList<Node> relayMessageChilds = new ArrayList<Node>();
    relayMessageChilds.add(innerType);
    relayMessageChilds.add(ClientID);
    //relayMessageChilds.add(IANA);
    relayMessageChilds.add(IANA2);
    //relayMessageChilds.add(IAPD);
    relayMessageChilds.add(IAPD2);
    relayMessageChilds.add(requestedOptions);
    relayMessageChilds.add(vendorClass);
    
    TypeOnlyNode relayMessage = new TypeOnlyNode("RelayMessage", relayMessageChilds);
    
    inputRequest.add(relayMessage);
    
    TypeWithValueNode leaseTime = new TypeWithValueNode("T2", "3600");
    formerOutput.add(leaseTime);
    
    pooltype.setType("CPE-Pool");
    
    return new Container(new XynaObjectList<Node>(inputRequest, Node.class), new XynaObjectList<Node>(formerOutput, Node.class), pooltype);
  }
  
  public static Container GenerateInputForRequest(List<? extends IAID> requestedIAIDs, List<? extends IP> requestedIPv6Addresses) {
    //TODO implementation
    //TODO update dependency XML
    ArrayList<Node> inputRequest = new ArrayList<Node>();
    ArrayList<Node> formerOutput = new ArrayList<Node>();
    PoolType pooltype = new PoolType();
    
    
    String iaidIANA1 = requestedIAIDs.get(0).getIaid();
    String ipIANA1 = requestedIPv6Addresses.get(0).getValue();
    String iaidIANA2 = requestedIAIDs.get(1).getIaid();
    String ipIANA2 = requestedIPv6Addresses.get(1).getValue();
    String iaidIAPD1 = requestedIAIDs.get(2).getIaid();
    String ipIAPD1 = requestedIPv6Addresses.get(2).getValue();
    String iaidIAPD2 = requestedIAIDs.get(3).getIaid();
    String ipIAPD2 = requestedIPv6Addresses.get(3).getValue();
    
    TypeWithValueNode linkAddress = new TypeWithValueNode("LinkAddress", "2001:0000:0000:0001:0000:0000:0000:0010");
    inputRequest.add(linkAddress);
    
    //erzeuge VendorSpecificInformation, die parallel zu Relay-Message angehaengt wird
    ArrayList<Node> vsiSubnodes = new ArrayList<Node>();
    TypeWithValueNode cmMAC = new TypeWithValueNode("CL_CM_MAC_ADDR", "00:1d:cd:70:56:6c");
    TypeWithValueNode cmtsCAP = new TypeWithValueNode("CL_OPTION_DOCS_CMTS_CAP", "0xDOCSISVersionNmber3.0");
    vsiSubnodes.add(cmtsCAP);
    vsiSubnodes.add(cmMAC);
    TypeOnlyNode vendorSpecInfo = new TypeOnlyNode("VendorSpecificInformation4491",vsiSubnodes);
    inputRequest.add(vendorSpecInfo);

    
  //TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xaaaa0000aaaa");// reservedHost
    TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xbbbb0000bbbb");
    ArrayList<Node> DUIDchilds = new ArrayList<Node>();
    DUIDchilds.add(linkLayerAddress);
    TypeOnlyNode DUID_LLT = new TypeOnlyNode("DUID-LLT", DUIDchilds);
    ArrayList<Node> ClientIDchilds = new ArrayList<Node>();
    ClientIDchilds.add(DUID_LLT);
    TypeOnlyNode ClientID = new TypeOnlyNode("ClientID", ClientIDchilds);
    TypeWithValueNode innerType = new TypeWithValueNode("InnerType", "3");
    
    ArrayList<Node> IANAchilds = new ArrayList<Node>();
    TypeWithValueNode iaid = new TypeWithValueNode("IAID", iaidIANA2);
    IANAchilds.add(iaid);
    TypeWithValueNode T1 = new TypeWithValueNode("T1", "3600");
    IANAchilds.add(T1);
    TypeWithValueNode T2 = new TypeWithValueNode("T2", "7200");
    IANAchilds.add(T2);
    
 // Reihenfolge in IA-Address-Option: IPv6 adddress, pref. lifetime, valid lifetime, options
    ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();
    TypeWithValueNode prefLeaseTime = new TypeWithValueNode("T1", "3600");
    TypeWithValueNode validLeaseTime = new TypeWithValueNode("T2", "7200");
    TypeWithValueNode ip = new TypeWithValueNode("IPv6", ipIANA2);//TODO: Format!!!
    iaAddrSubnodes.add(ip);
    iaAddrSubnodes.add(prefLeaseTime);
    iaAddrSubnodes.add(validLeaseTime);
    TypeOnlyNode iaAddr = new TypeOnlyNode("IA_Address", iaAddrSubnodes);
    IANAchilds.add(iaAddr);
    
    TypeOnlyNode IANA = new TypeOnlyNode("IA_NA", IANAchilds);
    
  
    ArrayList<Node> IAPDchilds = new ArrayList<Node>();
    TypeWithValueNode iaid_pd = new TypeWithValueNode("IAID", iaidIAPD2);
    IAPDchilds.add(iaid_pd);
    TypeWithValueNode T1pd = new TypeWithValueNode("T1", "3600");
    IAPDchilds.add(T1pd);
    TypeWithValueNode T2pd = new TypeWithValueNode("T2", "7200");
    IAPDchilds.add(T2pd);
  //IAPrefix: T1, T2, PrefixLength, PrefixAddress, options
    ArrayList<Node> iaAddrSubnodesPD = new ArrayList<Node>();
    TypeWithValueNode prefLeaseTimePD = new TypeWithValueNode("T1", "3600");
    TypeWithValueNode validLeaseTimePD = new TypeWithValueNode("T2", "7200");
    TypeWithValueNode prefixlengthPD = new TypeWithValueNode("PrefixLength", "112");
    TypeWithValueNode ipPD = new TypeWithValueNode("IPv6", ipIAPD2);//TODO: Format!!!
    iaAddrSubnodesPD.add(prefLeaseTimePD);
    iaAddrSubnodesPD.add(validLeaseTimePD);
    iaAddrSubnodesPD.add(prefixlengthPD);
    iaAddrSubnodesPD.add(ipPD);
    TypeOnlyNode iaAddrPD = new TypeOnlyNode("IAPrefix", iaAddrSubnodesPD);
    IAPDchilds.add(iaAddrPD);
    
    TypeOnlyNode IAPD = new TypeOnlyNode("IA_PD", IAPDchilds);
  
    
    
    //TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0001000200030011");
 // request DNSServer-Option 23
    TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0017");
    
  //erzeuge VendorClass-Option
    TypeWithValueNode vendorClass = new TypeWithValueNode("VendorClass4491", "Siemens");

    ArrayList<Node> relayMessageChilds = new ArrayList<Node>();
    relayMessageChilds.add(innerType);
    relayMessageChilds.add(ClientID);
    relayMessageChilds.add(IANA);
    relayMessageChilds.add(IAPD);
    relayMessageChilds.add(requestedOptions);
    relayMessageChilds.add(vendorClass);
    TypeOnlyNode relayMessage = new TypeOnlyNode("RelayMessage", relayMessageChilds);
    inputRequest.add(relayMessage);
    
    TypeWithValueNode leaseTime = new TypeWithValueNode("T2", "3600");
    formerOutput.add(leaseTime);
    
    pooltype.setType("CPE-Pool");
    
    return new Container(new XynaObjectList<Node>(inputRequest, Node.class), new XynaObjectList<Node>(formerOutput, Node.class), pooltype);
  }
  
  
  public static Container generateInputForRenew(List<? extends IAID> requestedIAIDs, List<? extends IP> requestedIPv6Addresses) {

    ArrayList<Node> inputRenew = new ArrayList<Node>();
    ArrayList<Node> formerOutput = new ArrayList<Node>();
    PoolType pooltype = new PoolType();
    

    String iaidIANA1 = requestedIAIDs.get(0).getIaid();
    String ipIANA1 = requestedIPv6Addresses.get(0).getValue();
    String iaidIANA2 = requestedIAIDs.get(1).getIaid();
    String ipIANA2 = requestedIPv6Addresses.get(1).getValue();
    String iaidIAPD1 = requestedIAIDs.get(2).getIaid();
    String ipIAPD1 = requestedIPv6Addresses.get(2).getValue();
    String iaidIAPD2 = requestedIAIDs.get(3).getIaid();
    String ipIAPD2 = requestedIPv6Addresses.get(3).getValue();
    
    TypeWithValueNode linkAddress = new TypeWithValueNode("LinkAddress", "2001:0000:0000:0001:0000:0000:0000:0010");
    inputRenew.add(linkAddress);
    
    //erzeuge VendorSpecificInformation, die parallel zu Relay-Message angehaengt wird
    ArrayList<Node> vsiSubnodes = new ArrayList<Node>();
    TypeWithValueNode cmMAC = new TypeWithValueNode("CL_CM_MAC_ADDR", "00:1d:cd:70:56:6c");
    TypeWithValueNode cmtsCAP = new TypeWithValueNode("CL_OPTION_DOCS_CMTS_CAP", "0xDOCSISVersionNmber3.0");
    vsiSubnodes.add(cmtsCAP);
    vsiSubnodes.add(cmMAC);
    TypeOnlyNode vendorSpecInfo = new TypeOnlyNode("VendorSpecificInformation4491",vsiSubnodes);
    inputRenew.add(vendorSpecInfo);

    
  //TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xaaaa0000aaaa");// reservedHost
    TypeWithValueNode linkLayerAddress = new TypeWithValueNode("LinkLayerAddress", "0xbbbb0000bbbb");
    ArrayList<Node> DUIDchilds = new ArrayList<Node>();
    DUIDchilds.add(linkLayerAddress);
    TypeOnlyNode DUID_LLT = new TypeOnlyNode("DUID-LLT", DUIDchilds);
    ArrayList<Node> ClientIDchilds = new ArrayList<Node>();
    ClientIDchilds.add(DUID_LLT);
    TypeOnlyNode ClientID = new TypeOnlyNode("ClientID", ClientIDchilds);
    TypeWithValueNode innerType = new TypeWithValueNode("InnerType", "5");
    
    
    ArrayList<Node> IANAchilds = new ArrayList<Node>();
    TypeWithValueNode iaid = new TypeWithValueNode("IAID", iaidIANA2);
    IANAchilds.add(iaid);
    TypeWithValueNode T1 = new TypeWithValueNode("T1", "3600");
    IANAchilds.add(T1);
    TypeWithValueNode T2 = new TypeWithValueNode("T2", "7200");
    IANAchilds.add(T2);
    
 // Reihenfolge in IA-Address-Option: IPv6 adddress, pref. lifetime, valid lifetime, options
    ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();
    TypeWithValueNode prefLeaseTime = new TypeWithValueNode("T1", "3600");
    TypeWithValueNode validLeaseTime = new TypeWithValueNode("T2", "7200");
    TypeWithValueNode ip = new TypeWithValueNode("IPv6", ipIANA2);//TODO: Format!!!
    iaAddrSubnodes.add(ip);
    iaAddrSubnodes.add(prefLeaseTime);
    iaAddrSubnodes.add(validLeaseTime);
    TypeOnlyNode iaAddr = new TypeOnlyNode("IA_Address", iaAddrSubnodes);
    IANAchilds.add(iaAddr);
    
    TypeOnlyNode IANA = new TypeOnlyNode("IA_NA", IANAchilds);
    
  
    ArrayList<Node> IAPDchilds = new ArrayList<Node>();
    TypeWithValueNode iaid_pd = new TypeWithValueNode("IAID", iaidIAPD2);
    IAPDchilds.add(iaid_pd);
    TypeWithValueNode T1pd = new TypeWithValueNode("T1", "3600");
    IAPDchilds.add(T1pd);
    TypeWithValueNode T2pd = new TypeWithValueNode("T2", "7200");
    IAPDchilds.add(T2pd);
  //IAPrefix: T1, T2, PrefixLength, PrefixAddress, options
    ArrayList<Node> iaAddrSubnodesPD = new ArrayList<Node>();
    TypeWithValueNode prefLeaseTimePD = new TypeWithValueNode("T1", "3600");
    TypeWithValueNode validLeaseTimePD = new TypeWithValueNode("T2", "7200");
    TypeWithValueNode prefixlengthPD = new TypeWithValueNode("PrefixLength", "112");
    TypeWithValueNode ipPD = new TypeWithValueNode("IPv6", ipIAPD2);//TODO: Format!!!
    iaAddrSubnodesPD.add(prefLeaseTimePD);
    iaAddrSubnodesPD.add(validLeaseTimePD);
    iaAddrSubnodesPD.add(prefixlengthPD);
    iaAddrSubnodesPD.add(ipPD);
    TypeOnlyNode iaAddrPD = new TypeOnlyNode("IAPrefix", iaAddrSubnodesPD);
    IAPDchilds.add(iaAddrPD);
    
    TypeOnlyNode IAPD = new TypeOnlyNode("IA_PD", IAPDchilds);
    
    //TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0001000200030011");
    // request DNSServer-Option 23
    TypeWithValueNode requestedOptions = new TypeWithValueNode("RequestList", "0x0017");
    
  //erzeuge VendorClass-Option
    TypeWithValueNode vendorClass = new TypeWithValueNode("VendorClass4491", "Siemens");

    ArrayList<Node> relayMessageChilds = new ArrayList<Node>();
    relayMessageChilds.add(innerType);
    relayMessageChilds.add(ClientID);
    relayMessageChilds.add(IANA);
    relayMessageChilds.add(IAPD);
    relayMessageChilds.add(requestedOptions);
    relayMessageChilds.add(vendorClass);
    TypeOnlyNode relayMessage = new TypeOnlyNode("RelayMessage", relayMessageChilds);
    
    inputRenew.add(relayMessage);
    
    TypeWithValueNode leaseTime = new TypeWithValueNode("T2", "3600");
    formerOutput.add(leaseTime);
    
    pooltype.setType("CPE-Pool");
    
    return new Container(new XynaObjectList<Node>(inputRenew, Node.class), new XynaObjectList<Node>(formerOutput, Node.class), pooltype);
  }

}
