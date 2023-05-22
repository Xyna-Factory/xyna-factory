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
package com.gip.xyna.xact.trigger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;

public class DHCPClientStartParameter extends EnhancedStartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPClientStartParameter.class);

  public static final StringParameter<Integer> PORT = 
      StringParameter.typeInteger("port").
      documentation( Documentation.de("Port").en("Port").build() ).
      defaultValue(67).build();
  public static final StringParameter<String> ADDRESS = 
      StringParameter.typeString("address").
      documentation( Documentation.
                     de("Name der IP im NetworkConfigurationManagement oder Network-Interface-name (Default:= akzeptiert Connections von allen Interfaces)").
                     en("Name of ip in NetworkConfigurationManagement or network interface name (Default=accept connections from all interfaces)").build() ).
      build();
  public static final StringParameter<String> ORDERTYPE =
      StringParameter.typeString("ordertype").
      documentation( Documentation.
                     de("Name des OrderTypes, der vom Filter gestartet wird. Signatur: (xact.dhcp.DHCPPacket) -> ()").
                     en("Name of ordertype started by filter. signature (xact.dhcp.DHCPPacket) -> ()").build() ).
      defaultValue("xact.dhcp.client.wf.NotifySender").
      build();

  public static final List<StringParameter<?>> ALL_PARAMS = Arrays.<StringParameter<?>>asList(
      PORT,ADDRESS,ORDERTYPE );

  
  @Override
  public List<String> convertToNewParameters(List<String> params)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    //gibt keine alten Parameter
    return params;
  }

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMS;
  }

  @Override
  public DHCPClientStartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    DHCPClientStartParameter param = new DHCPClientStartParameter();
    param.port = PORT.getFromMap(paramMap);
    param.address = ADDRESS.getFromMap(paramMap);
    param.ordertype = ORDERTYPE.getFromMap(paramMap);
    //FIXME
    return param;
  }
  
  
  
  private int port;
  private String address;
  private String ordertype;
  
  public int getPort() {
    return port;
  }

  public String getAddress() {
    return address;
  }
  
  public InetAddress getInetAddress() throws UnknownHostException {
    if (address == null || address.equals("")) {
      return null;
    }

    InternetAddressBean iab =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
        .getInternetAddress(address, null);
    if (iab != null) {
      return iab.getInetAddress();
    }

    return InetAddress.getByName( address );
  }

  public String getOrderType() {
    return ordertype;
  }
 
 
}
