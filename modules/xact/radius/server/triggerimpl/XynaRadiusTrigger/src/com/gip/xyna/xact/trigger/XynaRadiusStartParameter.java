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
package com.gip.xyna.xact.trigger;



import java.net.InetAddress;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv6ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;



public class XynaRadiusStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusStartParameter.class);


  private InetAddress localAddress;
  private int port;

  private String address = "";
  private InterfaceProtocolPreference interfacePreference = InterfaceProtocolPreference.IPV4;


  public enum InterfaceProtocolPreference {

    IPV4, IPV6;


    public static InterfaceProtocolPreference getByNameIgnoreCase(String name) {
      if (IPV4.toString().equalsIgnoreCase(name)) {
        return IPV4;
      } else if (IPV6.toString().equalsIgnoreCase(name)) {
        return IPV6;
      } else {
        return null;
      }
    }
  }


  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public XynaRadiusStartParameter() {
  }


  public XynaRadiusStartParameter(InetAddress localAddress, int port) {
    this.localAddress = localAddress;
    this.port = port;
  }


  /**
   * Is called by XynaProcessing with the parameters provided by the deployer
   *
   * @return StartParameter Instance which is used to instantiate corresponding Trigger
   */
  public StartParameter build(String... args)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {

    if (args == null || args.length > 2) {
      throw new XACT_InvalidStartParameterCountException();
    }

    address = args[0];

    InetAddress ip = null;

    try {
      ip = getIP();
    } catch (XACT_InterfaceNoIPv6ConfiguredException e) {
      throw new RuntimeException("Problems building startparameters: no IPv6 configured", e);
    } catch (XACT_NetworkInterfaceNotFoundException e) {
      throw new RuntimeException("Problems building startparameters: network interface not found", e);
    } catch (XACT_InterfaceNoIPConfiguredException e) {
      throw new RuntimeException("Problems building startparameters: no IP configured", e);
    }

    int port = 1812; //RADIUS Port
    try {
      if (args.length == 2)
        port = Integer.parseInt(args[1]);
    } catch (Exception e) {
      throw new RuntimeException("Problems parsing port: ", e);
    }

    return new XynaRadiusStartParameter(ip, port);
  }


  /**
   * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D) are valid,
   *         then this method should return new String[]{{"descriptionA", "descriptionB"}, {"descriptionA",
   *         "descriptionC", "descriptionD"}}
   */
  public String[][] getParameterDescriptions() {
    return new String[][] {{"The local ip address to listen on, or the name of one definied in NetworkConfigurationManagement.",
        "The port to listen on for radius messages (optional, default: 1812)."}};
  }


  public InetAddress getIP()
      throws XACT_InterfaceNoIPv6ConfiguredException, XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException {
    if (address == null || address.equals("")) {
      return null;
    }

    InternetAddressBean iab = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
        .getInternetAddress(address, null);
    if (iab != null) {
      return iab.getInetAddress();
    }
    if (logger.isInfoEnabled()) {
      logger.info("address " + address + " unknown in network configuration management.");
    }

    // else: abwaertskompatibel:
    boolean ipv6 = getProtocolPreference().equals(InterfaceProtocolPreference.IPV6);
    boolean useLocalAddresses = false;
    return NetworkInterfaceUtils.getFirstIpAddressByInterfaceName(address, ipv6, useLocalAddresses);
  }


  public int getPort() {
    return port;
  }


  public InetAddress getAddress() {
    return localAddress;
  }


  public InterfaceProtocolPreference getProtocolPreference() {
    return this.interfacePreference;
  }

}
