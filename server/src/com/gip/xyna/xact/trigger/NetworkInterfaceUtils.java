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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv4ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv6ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_NoNetworkInterfacePresentException;




/**
 * Diese Klasse enth�lt Methoden um die zu einer Netzwerk Interfacenummer geh�rende 
 * IP-Adresse und Hostname zu bestimmen.
 * Da die Namen und die Reihenfolge der Netzwerk-Interfaces vom Betriebssystem und der Konfiguration 
 * abh�ngig sind, werden die Netzwerk-Interface-Namen alphabetisch sortiert und beginnend mit 0 durchnummeriert.
 * Loopback Interfaces werden ausgeklammert.
 */
public class NetworkInterfaceUtils {

  private static final Logger logger = CentralFactoryLogging.getLogger(NetworkInterfaceUtils.class);


  public static void main(String[] args) throws XynaException {

    System.out.println("List all network interfaces");

    for (int i = 0; i < 10; i++) {
      try {
        System.out.println(i + ": InterfaceName: " + getInterfaceName(i) + " - IP: " + getIP(i) + " - HostName: "
                        + getHostName(i));
      } catch (XynaException e) {
        // e.printStackTrace();
      }
    }

  }
 
 
  public static Inet4Address getInetAddress(int interfaceNumber) throws XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException  {

    if (interfaceNumber < 0) {
      throw new XACT_NetworkInterfaceNotFoundException(Integer.toString(interfaceNumber));
    }

    SortedSet<String> set = getAllInterfaceNames();
    if (interfaceNumber >= set.size()) {
      throw new XACT_NetworkInterfaceNotFoundException(Integer.toString(interfaceNumber));
    }

    String[] ss = set.toArray(new String[0]);
    NetworkInterface intf;
    try {
      intf = NetworkInterface.getByName(ss[interfaceNumber]);
    } catch (SocketException e) {
      throw new XACT_NetworkInterfaceNotFoundException(Integer.toString(interfaceNumber), e);
    }

    return getIPv4Address(intf);

  }


  public static String getInterfaceName(int interfaceNumber) throws XACT_NetworkInterfaceNotFoundException  {

    if (interfaceNumber < 0)
      throw new XACT_NetworkInterfaceNotFoundException(Integer.toString(interfaceNumber));

    SortedSet<String> set = getAllInterfaceNames();

    if (interfaceNumber >= set.size()) {
      throw new XACT_NetworkInterfaceNotFoundException(Integer.toString(interfaceNumber));
    }

    String[] ss = set.toArray(new String[0]);
    return ss[interfaceNumber];
  }


  public static String getIP(int interfaceNumber) throws XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException {
    return getInetAddress(interfaceNumber).getHostAddress();
  }


  public static String getHostName(int interfaceNumber) throws XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException {
    return getInetAddress(interfaceNumber).getHostName();
  }


  private static SortedSet<String> getAllInterfaceNames() {

    SortedSet<String> set = new TreeSet<String>();
    Enumeration<NetworkInterface> enu;

    try {
      enu = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new RuntimeException(new XACT_NoNetworkInterfacePresentException(e));
    }

    while (enu.hasMoreElements()) {
      NetworkInterface intf = enu.nextElement();
      if (intf != null) {
        //check if interface is a loopback device
        try {
          if (!isLoopback(intf)) {
            set.add(intf.getName());
          }
        } catch (XACT_InterfaceNoIPConfiguredException e) {
          logger.info("", e);
        }
      }
    }
    return set;
  }


  private static boolean isLoopback(NetworkInterface intf) throws XACT_InterfaceNoIPConfiguredException {

    Enumeration<InetAddress> enumeration = intf.getInetAddresses();
    if (enumeration == null) {
      throw new XACT_InterfaceNoIPConfiguredException(intf.getDisplayName());
    }

    if (enumeration.hasMoreElements()) {
      return enumeration.nextElement().isLoopbackAddress();
    } else {
      // interface has no associated inet address. 
      // e.g. dhcp address has not been acquired
      // It is safe to assume, that loopback devices have always a inet adress
      return false;
    }
  }


  private static Inet4Address getIPv4Address(NetworkInterface intf) throws XACT_InterfaceNoIPConfiguredException {

    Enumeration<InetAddress> enumeration = intf.getInetAddresses();
    if (enumeration == null) {
      throw new XACT_InterfaceNoIPConfiguredException(intf.getDisplayName());
    }

    while (enumeration.hasMoreElements()) {
      InetAddress addr = enumeration.nextElement();
      if (addr != null) {
        if (addr instanceof Inet4Address) {
          return (Inet4Address)addr;
        }
      }
    }

    throw new XACT_InterfaceNoIPConfiguredException(intf.getDisplayName());
  }


  public static Inet4Address getFirstIPv4AddressByInterfaceName(String interfaceName)
                  throws XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPConfiguredException {
    return (Inet4Address) getFirstIpAddressByInterfaceName(interfaceName, false, false);
  }


  public static InetAddress getFirstIpAddressByInterfaceName(String interfaceName, boolean ipv6,
                                                             boolean takeIntoAccountLocalAddresses)
                  throws XACT_NetworkInterfaceNotFoundException, XACT_InterfaceNoIPv6ConfiguredException,
                  XACT_InterfaceNoIPConfiguredException {

    if (interfaceName == null) {
      throw new IllegalArgumentException("Cannot obtain interface for interfacename <null>.");
    }

    // obtain interface and corresponding inet addresses
    Enumeration<InetAddress> addresses;
    try {
      NetworkInterface nInterface = NetworkInterface.getByName(interfaceName);
      if (nInterface != null) {
        addresses = nInterface.getInetAddresses();
      } else {
        throw new XACT_NetworkInterfaceNotFoundException(interfaceName);
      }
    } catch (SocketException e) {
      throw new RuntimeException("Failed to obtain IP addresses for interface " + interfaceName, e);
    }

    // find the first matching address
    InetAddress address;
    if (addresses != null && addresses.hasMoreElements()) {
      address = addresses.nextElement();
      while (!isAppropriateAddress(address, ipv6, takeIntoAccountLocalAddresses) && addresses.hasMoreElements()) {
        address = addresses.nextElement();
      }
      if (!isAppropriateAddress(address, ipv6, takeIntoAccountLocalAddresses)) {
        if (ipv6) {
          throw new XACT_InterfaceNoIPv6ConfiguredException(interfaceName);
        } else {
          throw new XACT_InterfaceNoIPv4ConfiguredException(interfaceName);
        }
      }
      while (addresses.hasMoreElements()) {
        if (isAppropriateAddress(addresses.nextElement(), ipv6, takeIntoAccountLocalAddresses)) {
          logger.warn("There is more than one IPv" + (ipv6 ? "6" : "4")
                          + " address associated with the specified interface name '" + interfaceName + "'");
          break;
        }
      }
    } else {
      throw new XACT_InterfaceNoIPConfiguredException(interfaceName);
    }
    return address;

  }


  private static boolean isAppropriateAddress(InetAddress address, boolean ipv6, boolean takeIntoAccountLocalAddresses) {
    return ipv6 && address instanceof Inet6Address && (takeIntoAccountLocalAddresses == address.isLinkLocalAddress())
                    || !ipv6 && address instanceof Inet4Address;
  }

}
