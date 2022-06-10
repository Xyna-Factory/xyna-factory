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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.rmi.Remote;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.GenericRMIAdapter.URLChooser;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.XynaRMIClientSocketFactory;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryChannelIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RMISSLClientSocketFactory.ClientSocketConnectionParameter;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


public class InterFactoryRMIChannel implements InterFactoryChannel {

  public final static String HOSTNAME_PARAMETER = "hostname";
  public final static String PORT_PARAMETER = "port";
  /*
   *  falls keystore nicht gesetzt, wird der keystore verwendet, der über die xynaproperties RMI_SSL_KEYSTORE_TYPE etc gesetzt ist
   *  falls truststore nicht gesetzt, wird kein truststore verwendet.
   *  falls truststore gesetzt ist, wird server-authentication durchgeführt
   */
  public final static String KEYSTORE_FILE_PARAMETER = "keystorefile";
  public final static String KEYSTORE_TYPE_PARAMETER = "keystoretype";
  public final static String KEYSTORE_PASS_PARAMETER = "keystorepass";
  public final static String TRUSTSTORE_FILE_PARAMETER = "truststorefile";
  public final static String TRUSTSTORE_TYPE_PARAMETER = "truststoretype";
  public final static String TRUSTSTORE_PASS_PARAMETER = "truststorepass";
  
  private String hostname;
  private int registryPort;
  private ClientSocketConnectionParameter conParams;
  private String servername;
  
  public void init(Map<String, String> parameter) {
    this.hostname = parameter.get(HOSTNAME_PARAMETER);
    this.servername = getServerName(this.hostname);
    this.registryPort = Integer.parseInt(parameter.get(PORT_PARAMETER));
    this.conParams = new ClientSocketConnectionParameter(servername, -1,
                                                    parameter.get(KEYSTORE_FILE_PARAMETER),
                                                    parameter.get(KEYSTORE_TYPE_PARAMETER),
                                                    parameter.get(KEYSTORE_PASS_PARAMETER),
                                                    parameter.get(TRUSTSTORE_FILE_PARAMETER),
                                                    parameter.get(TRUSTSTORE_TYPE_PARAMETER),
                                                    parameter.get(TRUSTSTORE_PASS_PARAMETER));
  }


  private String getServerName(String hostname) {
    InternetAddressBean iab = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement().getInternetAddress(hostname, null);
    if (iab == null) {
      return hostname;
    } else {
      return iab.getInetAddress().getHostAddress();
    }
  }


  public InterFactoryChannelIdentifier getIdentifier() {
    return InterFactoryChannelIdentifier.RMI;
  }


  private Map<String, GenericRMIAdapter<?>> rmiadapter = new ConcurrentHashMap<>();


  @SuppressWarnings("unchecked")
  public <I extends Remote> GenericRMIAdapter<I> getInterface(String bindingName) throws RMIConnectionFailureException {
    GenericRMIAdapter<?> adapter = rmiadapter.get(bindingName);
    if (adapter == null) {
      synchronized (this) {
        adapter = rmiadapter.get(bindingName);
        if (adapter == null) {
          URLChooser url = GenericRMIAdapter.getSingleURLChooser(this.servername, this.registryPort, bindingName,
                                                                 new XynaRMIClientSocketFactory(XynaProperty.RMI_IL_SOCKET_TIMEOUT.get()));
          adapter = new GenericRMIAdapter<I>(url);
          rmiadapter.put(bindingName, adapter);
        }
      }
    }
    return (GenericRMIAdapter<I>) adapter;
  }

  
  public void setupCommunication(Duration timeout) {
    //falls der server nicht auf SSL konfiguriert ist, ist das überflüssig. schadet aber nichts
    RMISSLClientSocketFactory.threadLocalConParams.set(Pair.of(timeout, conParams));
  }

  public void tearDownSetup() {
    RMISSLClientSocketFactory.threadLocalConParams.remove();
  }

}
