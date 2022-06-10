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


package com.gip.xyna.XMOM.base.net.internal;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.MaxListOfIPsExceededException;
import com.gip.xyna.XMOM.base.net.exception.NetworkNotMatchesNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.NoFreeIPFoundException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;


public class IPv6SubnetData implements Serializable {

  private static final long serialVersionUID = 1L;
  private final IPv6Address _network;
  private final IPv6NetmaskData _mask;

  private static XynaPropertyInt _maxIpList = new XynaPropertyInt("xprv.base.net.max.iplist", 256).
                                              setDefaultDocumentation(DocumentationLanguage.EN, 
                                                                  "Maximum number of ip addresses in output" +
                                                                  " of method 'IPv4Subnet.allIPsInSubnet()'"); 

  public IPv6SubnetData(IPv6Address network, IPv6NetmaskData mask) throws ValidationException {
    _network = network;
    _mask = mask;
  }


  public IPv6Address getNetworkAddress() {
    return _network;
  }

  public IPv6NetmaskData getNetmask() {
    return _mask;
  }

  
  public boolean ipWithinSubnet(IPv6Address ipIn) {
    BigInteger ip = ipIn.asBigInteger();
    BigInteger and = ip.and(this.getNetmask().asBigIntegerIp());
    if (and.equals(this.getNetworkAddress().asBigInteger())) {
      return true;
    }
    return false;
  }


  public IPv6Address getBroadcastAddress() throws FormatException, ValidationException {
    validate();
    BigInteger inverted = this.getNetmask().getIPInverted();
    BigInteger or = inverted.or(this.getNetworkAddress().asBigInteger());
    return new IPv6Address(or);
  }

  
  private void validate() throws ValidationException {
    if (!_network.isNetworkAddress(_mask)) {
      throw new NetworkNotMatchesNetmaskException(_network.toShortHexRepresentation() + "/" + _mask.getLength());
    }
  }


  public IPv6Address getGatewayAddress() throws FormatException, ValidationException {
    validate();
    BigInteger or = BigInteger.ONE.or(this.getNetworkAddress().asBigInteger());
    return new IPv6Address(or);
  }

  
  public List<IPv6Address> getAllIPsInSubnet() throws FormatException, ValidationException {
    validate();
    int max = _maxIpList.get();
    List<IPv6Address> ret = new ArrayList<IPv6Address>();
    IPv6Address ip = this.getNetworkAddress().inc();
    IPv6Address broadcast = this.getBroadcastAddress();
    
    while (!broadcast.equals(ip)) {
      ret.add(ip);      
      if (ret.size() > max) {
        throw new MaxListOfIPsExceededException();
      }
      ip = ip.inc();
    }
    return ret;
  }
  
  
  public IPv6Address getNextFreeIP(List<IPv6Address> list) throws FormatException, ValidationException {    
    validate();
    IPv6Address ip = this.getNetworkAddress().inc();
    IPv6Address broadcast = this.getBroadcastAddress();
    
    while (!broadcast.equals(ip)) {
      boolean isInList = false;
      for (IPv6Address addr : list) {
        if (ip.equals(addr)) {
          isInList = true;
          break;
        }
      }
      if (!isInList) {
        return ip;
      }
      ip = ip.inc();      
    }
    throw new NoFreeIPFoundException("");
  }
  
}
