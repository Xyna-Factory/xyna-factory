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
package com.gip.xyna.xfmg.xfctrl.netconfmgmt;



import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_AmbiguousInetAddressException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateIpName;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidInetAddressException;
import com.gip.xyna.xfmg.exceptions.XFMG_NetworkInterfaceNotFoundException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



/**
 * verwaltung von logischen namen für komponenten des netzwerks, in das die fabrik eingebunden ist.
 * es gibt:
 * netzwerk
 *  - computer
 *    - interfaces
 *      - mac
 *    - hostnames
 *    - addresses (zu interfaces und hostnames zugeordnet)
 *      - ipv4, ipv6
 * 
 *     
 */
public class NetworkConfigurationManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "NetworkConfigurationManagement";

  private ODS ods;
  private ConcurrentMap<String, InternetAddressBean> addressCache;


  public NetworkConfigurationManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {


    if (logger.isTraceEnabled()) {
      logger.trace("Executing " + NetworkConfigurationManagement.class.getSimpleName() + ".init()");
      /*
       * TODO konfiguration der caches, weil ansonsten änderungen /etc/hosts oder ähnliches nicht direkt wirksam werden.
       * dazu gibt es die java-properties:
       *  networkaddress.cache.ttl bzw (abwärtskompatibilität java 1.5?): sun.net.inetaddr.ttl
       *  und
       *  networkaddress.cache.negative.ttl bzw. sun.net.inetaddr.negative.ttl
       *  
       *  mit den werten (-1=cache hält FOREVER, 0 = kein cache, x = x sekunden lang cachen)
       *  
       *  achtung: auf 0 setzen ist gefährlich, weil dann sehr viele dns requests durchgeführt werden. vermutlich ist ein wert im minuten bereich besser
       *  
       *  die settermethoden der werte können nicht ohne einschränkungen zur laufzeit verwendet werden, also entweder 
       *  beim serverstart setzen oder per reflection umsetzen:
       * beispielcode:  
       *  
    Field f = InetAddressCachePolicy.class.getDeclaredField("cachePolicy");
    f.setAccessible(true);
    f.set(null, 0);
     f = InetAddressCachePolicy.class.getDeclaredField("set");
    f.setAccessible(true);
    f.set(null, true);
    f = InetAddressCachePolicy.class.getDeclaredField("negativeCachePolicy");
    f.setAccessible(true);
    f.set(null, 0);
    f = InetAddressCachePolicy.class.getDeclaredField("cachePolicy");
    f.setAccessible(true);
    f.set(null, 0);
    System.out.println(InetAddressCachePolicy.get());
    System.out.println(InetAddressCachePolicy.getNegative());
    InetAddressCachePolicy.setNegativeIfNotSet(5);
    InetAddressCachePolicy.setIfNotSet(5);
       *  
       */
    }

    addressCache = new ConcurrentHashMap<String, InternetAddressBean>();
    
    ods = ODSImpl.getInstance();
    ods.registerStorable(InternetAddressStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<InternetAddressStorable> addresses = con.loadCollection(InternetAddressStorable.class);
      for (InternetAddressStorable address : addresses) {
        try {
          addressCache.put(address.getId(),
                           new InternetAddressBean(address.getId(), InetAddress.getByName(address.getIp()), address
                               .getDocumentation()));
        } catch (UnknownHostException e) {
          logger.warn("Address " + address.getId() + "->" + address.getIp() + " is not valid.", e);
        }
      }
    } finally {
      con.closeConnection();
    }
  }


  @Override
  protected void shutdown() throws XynaException {

  }


  //----------------------------------------------------- NetworkInterfaces -----------------------------------------------------
  //TODO funktioniert derzeit nicht - erweitern!

  public void storeNetworkInterface(String id, String ifName, String ipOrHostname)
      throws XFMG_NetworkInterfaceNotFoundException, PersistenceLayerException {
    if (ifName != null) {
      NetworkInterface ni;
      try {
        ni = NetworkInterface.getByName(ifName);
      } catch (SocketException e) {
        throw new XFMG_NetworkInterfaceNotFoundException(ifName, e);
      }
      if (ni == null) {
        throw new XFMG_NetworkInterfaceNotFoundException(ifName);
      }
    } else if (ipOrHostname != null) {
      NetworkInterface ni;
      try {
        ni = NetworkInterface.getByInetAddress(InetAddress.getByName(ipOrHostname));
      } catch (SocketException e) {
        throw new XFMG_NetworkInterfaceNotFoundException(ipOrHostname, e);
      } catch (UnknownHostException e) {
        throw new XFMG_NetworkInterfaceNotFoundException(ipOrHostname, e);
      }
      if (ni == null) {
        throw new XFMG_NetworkInterfaceNotFoundException(ipOrHostname);
      }
      ifName = ni.getName();
    }
    NetworkInterfaceStorable nis = new NetworkInterfaceStorable(id, ifName);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(nis);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public NetworkInterfaceBean getNetworkInterface(String id, DependencyNode user) {
    return null;
  }


  public NetworkInterfaceBean[] listNetworkInterfaces() {
    return null;
  }


  //----------------------------------------------------- IPs  -----------------------------------------------------


  public InternetAddressBean getInternetAddress(String id, DependencyNode user) {
    //TODO für den user automatisch eine abhängigkeit im dependencyregister anlegen, dass er die id verwendet.
    return addressCache.get(id);
  }


  public InternetAddressBean[] listInternetAddresses() {
    return addressCache.values().toArray(new InternetAddressBean[0]);
  }


  public void storeInternetAddress(String id, String address, String documentation, boolean force)
      throws XFMG_AmbiguousInetAddressException, XFMG_InvalidInetAddressException, PersistenceLayerException, XFMG_DuplicateIpName {
    InetAddress inetAddress;
    try {
      InetAddress[] allByName = InetAddress.getAllByName(address);
      if (allByName.length > 1) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allByName.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(allByName[i].getHostAddress());
        }
        throw new XFMG_AmbiguousInetAddressException(address, sb.toString());
      }
      if (allByName.length == 0) {
        throw new XFMG_InvalidInetAddressException(address);
      }

      inetAddress = allByName[0];
    } catch (UnknownHostException e) {
      throw new XFMG_InvalidInetAddressException(address, e);
    }

    if (!force && addressCache.containsKey(id)) {
      throw new XFMG_DuplicateIpName(id);
    }
    
    InternetAddressStorable nis = new InternetAddressStorable(id, inetAddress.getHostAddress(), documentation);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(nis);
      con.commit();
    } finally {
      con.closeConnection();
    }
    addressCache.put(id, new InternetAddressBean(id, inetAddress, documentation));
  }


  public void removeIpAddress(String id) throws PersistenceLayerException {

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(new InternetAddressStorable(id, null, null));
      con.commit();
    } finally {
      con.closeConnection();
    }

    addressCache.remove(id);

  }

}
