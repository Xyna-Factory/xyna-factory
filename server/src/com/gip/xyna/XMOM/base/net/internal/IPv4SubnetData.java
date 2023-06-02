/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.MaxListOfIPsExceededException;
import com.gip.xyna.XMOM.base.net.exception.NetworkNotMatchesNetmaskException;
import com.gip.xyna.XMOM.base.net.exception.NoFreeIPFoundException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;


public class IPv4SubnetData implements Serializable {

  private static final long serialVersionUID = 1L;

  private static Logger _logger = CentralFactoryLogging.getLogger(IPv4SubnetData.class);
  
  private final IPv4Address _network;
  private final IPv4NetmaskData _mask;

  private static XynaPropertyInt _maxIpList = new XynaPropertyInt("xprv.base.net.max.iplist", 256).
                                              setDefaultDocumentation(DocumentationLanguage.EN, 
                                                                  "Maximum number of ip addresses in output" +
                                                                  " of method 'IPv4Subnet.allIPsInSubnet()'"); 


  public IPv4SubnetData(IPv4Address network, IPv4NetmaskData mask) throws ValidationException {
    _network = network;
    _mask = mask;
  }


  public IPv4Address getNetworkAddress() {
    return _network;
  }

  public IPv4NetmaskData getNetmask() {
    return _mask;
  }

  public boolean ipWithinSubnet(IPv4Address ip) {
    long ipLong = ip.getAsLong();
    long and = ipLong & this.getNetmask().getIPv4Address().getAsLong();
    if (and == this.getNetworkAddress().getAsLong()) {
      return true;
    }
    return false;
  }


  public IPv4Address getBroadcastAddress() throws FormatException, ValidationException {
    validate();
    long inverted = this.getNetmask().getIPInvertedAsLong();
    long or = inverted | this.getNetworkAddress().getAsLong();
    return new IPv4Address(or);
  }

  private void validate() throws ValidationException {
    if (!_network.isNetworkAddress(_mask)) {
      throw new NetworkNotMatchesNetmaskException(_network.toDotDecimalString() + "/" + _mask.getLength());
    }
  }


  public IPv4Address getGatewayAddress() throws FormatException, ValidationException {
    validate();
    long or = 1 | this.getNetworkAddress().getAsLong();
    return new IPv4Address(or);
  }
  
  
  public List<IPv4Address> getAllIPsInSubnet() throws FormatException, ValidationException {
    validate();
    int max = _maxIpList.get();
    List<IPv4Address> ret = new ArrayList<IPv4Address>();
    IPv4Address ip = this.getNetworkAddress().inc();
    IPv4Address broadcast = this.getBroadcastAddress();
    
    while (!broadcast.equals(ip)) {
      ret.add(ip);
      _logger.trace("Added ip to list: " + ip.toDotDecimalString());
      
      if (ret.size() > max) {
        throw new MaxListOfIPsExceededException();
      }
      ip = ip.inc();
    }
    return ret;
  }
  
  
  public IPv4Address getNextFreeIP(List<IPv4Address> list) throws FormatException, ValidationException {
    validate();
    IPv4Address ip = this.getNetworkAddress().inc();
    IPv4Address broadcast = this.getBroadcastAddress();
    
    while (!broadcast.equals(ip)) {
      boolean isInList = false;
      for (IPv4Address addr : list) {
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
