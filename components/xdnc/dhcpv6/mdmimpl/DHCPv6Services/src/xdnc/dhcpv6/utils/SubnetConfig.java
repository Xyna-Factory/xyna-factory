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

package xdnc.dhcpv6.utils;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import xdnc.dhcpv6.DHCPv6ServicesImpl;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6AddressUtil;


public class SubnetConfig {

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPv6ServicesImpl.class);
  private String addrRangeFrom;
  private String addrRangeTo;
  
  private Map<String,String> attributes;
  private List<Integer> fixedAttributes;
  private String fixedAttributesString;
  private String attributesString;
  
  private BigInteger hashFrom;
  private BigInteger hashTo;
  
  private static String checkAddress(String address) {
    String ret = "invalid:"+address;
        
    try {
      if(InetAddress.getByName(address) instanceof Inet6Address)
        return address;
    }
    catch (UnknownHostException e) {
      return ret;
    }
        
    return ret;
  }
  
  public SubnetConfig(String strRangeOptions) {
    //String tmp = strRangeMaskRouter.replaceAll("[^\\.^0-9-,^#^=^<^>^a-z^A-Z]", "").replaceAll("^\\<|>,$", "");
    String tmp = strRangeOptions.substring(1, strRangeOptions.length()-2);
    
    if(logger.isDebugEnabled())logger.debug("Processing Subnet: "+tmp);
    
    String[] parts = tmp.split("#");
    
    String range[] = parts[0].split("-");  
    
    addrRangeFrom = checkAddress(range[0]);
    addrRangeTo = checkAddress(range[1]);
    
    if(parts.length>1) {
      setFixedAttributes(parseFixedAttributes(parts[1]));
      fixedAttributesString = parts[1];
    } else {
      setFixedAttributes(new ArrayList<Integer>());
    }
    
    if(parts.length>2) {
      setAttributes(parseAttributes(parts[2]));
      attributesString = parts[2];
    } else {
      setAttributes(new HashMap<String,String>());
    }
    
    if(logger.isDebugEnabled())
    {
      logger.debug("Startaddress Subnet: "+addrRangeFrom);
      logger.debug("Endaddress Subnet: "+addrRangeTo);
      logger.debug("FixedAttributes: "+fixedAttributesString);
      logger.debug("Attributes: "+attributesString);
    }
    

    if(!addrRangeFrom.contains("invalid"))
      hashFrom  = IPv6AddressUtil.parse(addrRangeFrom).asBigInteger();
    else
      hashFrom = BigInteger.ZERO;
    
    if(!addrRangeFrom.contains("invalid"))
      hashTo  = IPv6AddressUtil.parse(addrRangeTo).asBigInteger();
    else
      hashTo = BigInteger.ZERO;
  }


  private Map<String, String> parseAttributes(String attrString) {
    
    this.attributesString=attrString;
    
    Map<String, String> attrMap = new HashMap<String, String>();
    
    if(attrString==null || attrString.length()==0) {
      return attrMap;
    }
    
    String[] kvStrings = attrString.split(">,");
    
    for(String s : kvStrings) {
      String [] kvPair = s.split("=");
      attrMap.put(kvPair[0], kvPair[1].replaceAll("^\\<|\\>$", ""));
    }
    
    return attrMap;
  }

  private List<Integer> parseFixedAttributes(String fixedAttrString) {
    
    List<Integer> fixedAttrList = new ArrayList<Integer>();
    
    if(fixedAttrString==null || fixedAttrString.length()==0) {
      return fixedAttrList;
    }
    for(String s : fixedAttrString.split(",")) {
      fixedAttrList.add(Integer.parseInt(s));
    }  
    
    return fixedAttrList;
  }

  public Map<String, String> getSubnetForIp(String ip) {
    BigInteger hashIp = IPv6AddressUtil.parse(ip).asBigInteger();
    if(hashIp.compareTo(hashFrom)>=0 && hashIp.compareTo(hashTo)<=0) {
      Map<String,String> ret = new HashMap<String,String>();
      
      return ret;
      
      
    } else return (Map<String, String>) null;
      
  }
  
  
  
  /**
   * @return the addrRangeFrom
   */
  public String getAddrRangeFrom() {
    return addrRangeFrom;
  }


  
  /**
   * @param addrRangeFrom the addrRangeFrom to set
   */
  public void setAddrRangeFrom(String addrRangeFrom) {
    this.addrRangeFrom = addrRangeFrom;
  }


  
  /**
   * @return the addrRangeTo
   */
  public String getAddrRangeTo() {
    return addrRangeTo;
  }


  
  /**
   * @param addrRangeTo the addrRangeTo to set
   */
  public void setAddrRangeTo(String addrRangeTo) {
    this.addrRangeTo = addrRangeTo;
  }


  /**
   * @return the attributes
   */
  public Map<String,String> getAttributes() {
    return attributes;
  }

  /**
   * @param attributes the attributes to set
   */
  public void setAttributes(Map<String,String> attributes) {
    this.attributes = attributes;
  }

  /**
   * @return the fixedAttributes
   */
  public List<Integer> getFixedAttributes() {
    return fixedAttributes;
  }

  /**
   * @return the fixedAttributesString
   */
  public String getFixedAttributesString() {
    return fixedAttributesString;
  }
  
  /**
   * @return the AttributesString
   */
  public String getAttributesString() {
    return attributesString;
  }

  /**
   * @param fixedAttributes the fixedAttributes to set
   */
  public void setFixedAttributes(List<Integer> fixedAttributes) {
    this.fixedAttributes = fixedAttributes;
  }
  
  
}
