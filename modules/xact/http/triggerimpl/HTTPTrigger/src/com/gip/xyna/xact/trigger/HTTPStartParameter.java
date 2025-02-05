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
package com.gip.xyna.xact.trigger;



import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InterfaceNoIPv6ConfiguredException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_NetworkInterfaceNotFoundException;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;



public class HTTPStartParameter extends EnhancedStartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(HTTPStartParameter.class);

  private int port;
  private String address;
  private KeyStoreParameter keystoreParameter;
  private ClientAuth clientAuth;
  private String keyStorePath;
  private String keyStoreType;
  private String keyStorePassword;
  private InterfaceProtocolPreference interfacePreference;
  private String keyStoreName;
  private String trustStoreName;
  private String sslContextAlgorithm;
  private boolean suppressRequestLogging = false;

  public enum ClientAuth { //in anlehnung an apache
    require, optional, none;
  }

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

  public static enum KeyStoreParameter {

    NONE(false),
    FILE(true),
    KEY_MGMT(true),
    // for deserializtion of old entries
    FALSE(false),
    TRUE(true);


    private final boolean useHTTPS;

    KeyStoreParameter(boolean useHTTPS) {
      this.useHTTPS = useHTTPS;
    }

    public static KeyStoreParameter getByNameIgnoreCase(String name) {
      if (NONE.toString().equalsIgnoreCase(name) ||
          Boolean.FALSE.toString().equalsIgnoreCase(name)) {
        return NONE;
      } else if (FILE.toString().equalsIgnoreCase(name) ||
                 Boolean.TRUE.toString().equalsIgnoreCase(name)) {
        return FILE;
      } else if (KEY_MGMT.toString().equalsIgnoreCase(name)) {
        return KEY_MGMT;
      } else {
        return null;
      }
    }

    public boolean useHTTPS() {
      return useHTTPS;
    }

  }

  public static final StringParameter<Integer> PORT =
      StringParameter.typeInteger("port").
      documentation( Documentation.de("Port").en("Port").build() ).
      mandatory().build();
  public static final StringParameter<String> ADDRESS =
      StringParameter.typeString("address").
      documentation( Documentation.
                     de("Name der IP im NetworkConfigurationManagement oder Network-Interface-name (Default: Akzeptiert Verbindungen von allen Interfaces)").
                     en("Name of ip in NetworkConfigurationManagement or network interface name (Default: Accept connections from all interfaces)").build() ).
      defaultValue("").build();
  public static final StringParameter<InterfaceProtocolPreference> NETWORK_INTERFACE_PROTOCOL =
      StringParameter.typeEnum(InterfaceProtocolPreference.class, "protocol", true).
      documentation( Documentation.de("Network-Interface-Protokoll").en("Network interface protocol").build() ).
      defaultValue(InterfaceProtocolPreference.IPV4).build();
  public static final StringParameter<KeyStoreParameter> HTTPS =
      StringParameter.typeEnum(KeyStoreParameter.class, "https", true).
      documentation( Documentation
                     .de("Verwende HTTP oder HTTPS mit SSL-Context, NONE: http, FILE: https und Angabe eines gemeinsamen Key/Trust-Files, KEY_MGMT: https und Angabe eines KeyStores und TrustManagers")
                     .en("Use HTTP or HTTPS with SSL context, NONE: http, FILE: https and parameters for a shared Key/Trust-File, KEY_MGMT: https and parameters for a key store and a trust manager").build() ).
      defaultValue(KeyStoreParameter.NONE).build();
  public static final StringParameter<ClientAuth> CLIENTAUTH =
      StringParameter.typeEnum(ClientAuth.class, "clientauth").
      documentation( Documentation.de("Client-Authentifizierung").en("Clientauthentication").build() ).
      mandatoryFor(HTTPS, KeyStoreParameter.FILE).
      mandatoryFor(HTTPS, KeyStoreParameter.KEY_MGMT).
      build();
  public static final StringParameter<String> KEYSTOREPATH =
      StringParameter.typeString("keystorepath").
      documentation( Documentation.de("Pfad zum Keystore").en("Keystore Path").build() ).
      mandatoryFor(HTTPS, KeyStoreParameter.FILE).
      build();
  /*
   * TODO keystore-PW und key-PW unterscheiden
   * vgl
   * http://stackoverflow.com/questions/25488203/unrecoverablekeyexception-cannot-recover-key
    da steht:
    If you really want to use two distinct passwords, you'll need to implement getPrivateKey(String alias) in your custom X509KeyManager
    to take this into account. In particular, it will have to load the keys from your KeyStore instance with the right password for
     each alias (see getKey(String alias, char[] password)).
   */
  public static final StringParameter<String> KEYSTOREPASS =
      StringParameter.typeString("keystorepasswd").
      documentation( Documentation.de("Keystore-Passwort").en("Keystore Password").build() ).
      mandatoryFor(HTTPS, KeyStoreParameter.FILE).
      build();
  public static final StringParameter<String> KEYSTORETYPE =
      StringParameter.typeString("keystoretype").
      documentation( Documentation.de("Keystore-Typ").en("Keystore Type").build() ).
      defaultValue("JKS").build();
  public static final StringParameter<String> KEYSTORE_NAME =
                  StringParameter.typeString("keystorename").
                  documentation( Documentation.de("Keystore-Name").en("Keystore Name").build() ).
                  mandatoryFor(HTTPS, KeyStoreParameter.KEY_MGMT).build();
  public static final StringParameter<String> TRUSTSTORE_NAME =
                  StringParameter.typeString("trustmanagername").
                  documentation( Documentation.de("TrustManager-Name").en("TrustManager Name").build() ).
                  optional().build();
  public static final StringParameter<String> SSL_CONTEXT_ALGORITHM =
      StringParameter.typeString("ssl")
          .documentation(Documentation.de("TLS-Protokoll").en("TLS protocol").build())
          .defaultValue("TLS").pattern("^TLS(v1(\\.[1-3])?)?$").build();
  public static final StringParameter<Boolean> SUPPRESS_LOGGING = StringParameter.typeBoolean("suppressRequestLogging").
                  documentation(Documentation.de("Unterdrücke requestabhängiges Logging").en("Suppress request dependent logging.").build()).
                  optional().defaultValue(false).build();

  public static final List<StringParameter<?>> allParameters =
      StringParameter.asList(PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE,
                             KEYSTORE_NAME, TRUSTSTORE_NAME, SSL_CONTEXT_ALGORITHM, SUPPRESS_LOGGING);


  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter#convertToNewParameters(java.util.List)
   */
  @Override
  public List<String> convertToNewParameters(List<String> params)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParams = new ArrayList<String>();
    int paramSize = params.size();
    boolean withNETWORK_INTERFACE_PROTOCOL = true;
    boolean withSSL_CONTEXT_ALGORITHM = true;
    if (paramSize == 7 || paramSize == 8) {
      try {
        HTTPS.parse(params.get(2));
        withNETWORK_INTERFACE_PROTOCOL = false;
      } catch (StringParameterParsingException e) {
        withNETWORK_INTERFACE_PROTOCOL = true;
      }
      try {
        SSL_CONTEXT_ALGORITHM.parse(params.get(6));
        withSSL_CONTEXT_ALGORITHM = false;
      } catch (StringParameterParsingException e) {
        withSSL_CONTEXT_ALGORITHM = true;
      }
    }

    try {
      startParams.add(PORT.toNamedParameterString(PORT.parse(params.get(0)).toString()));
      switch (params.size()) {
        case 1 :
          break;
        case 2 : // PORT, ADDRESS
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          break;
        case 3 : // PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          startParams.add(NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(NETWORK_INTERFACE_PROTOCOL.parse(params.get(2)).toString()));
          break;
        case 6 : // HTTPS or CLIENTAUTH
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(2)).toString()));
          startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(3)).toString()));
          startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(4)).toString()));
          startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(5)).toString()));
          break;
        case 7 :
          if (withNETWORK_INTERFACE_PROTOCOL) { // PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS
            startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
            startParams.add(NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(NETWORK_INTERFACE_PROTOCOL.parse(params.get(2)).toString()));
            startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(3)).toString()));
            startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(4)).toString()));
            startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(5)).toString()));
            startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(6)).toString()));
            break;
          }
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(2)).toString()));
          startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(3)).toString()));
          startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(4)).toString()));
          startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(5)).toString()));
          if (withSSL_CONTEXT_ALGORITHM) { // PORT, ADDRESS, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, SSL_CONTEXT_ALGORITHM
            startParams.add(SSL_CONTEXT_ALGORITHM.toNamedParameterString(SSL_CONTEXT_ALGORITHM.parse(params.get(6)).toString()));
          } else { // PORT, ADDRESS, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE
            startParams.add(KEYSTORETYPE.toNamedParameterString(KEYSTORETYPE.parse(params.get(6)).toString()));
          }
          break;
        case 8 :
          if (!withNETWORK_INTERFACE_PROTOCOL) { // PORT, ADDRESS, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE, SSL_CONTEXT_ALGORITHM
            startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
            startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(2)).toString()));
            startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(3)).toString()));
            startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(4)).toString()));
            startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(5)).toString()));
            startParams.add(KEYSTORETYPE.toNamedParameterString(KEYSTORETYPE.parse(params.get(6)).toString()));
            startParams.add(SSL_CONTEXT_ALGORITHM.toNamedParameterString(SSL_CONTEXT_ALGORITHM.parse(params.get(7)).toString()));
            break;
          }
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          startParams.add(NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(NETWORK_INTERFACE_PROTOCOL.parse(params.get(2)).toString()));
          startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(3)).toString()));
          startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(4)).toString()));
          startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(5)).toString()));
          startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(6)).toString()));
          if (withSSL_CONTEXT_ALGORITHM) { // PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, SSL_CONTEXT_ALGORITHM
            startParams.add(SSL_CONTEXT_ALGORITHM.toNamedParameterString(SSL_CONTEXT_ALGORITHM.parse(params.get(7)).toString()));
          } else { // PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE
            startParams.add(KEYSTORETYPE.toNamedParameterString(KEYSTORETYPE.parse(params.get(7)).toString()));
          }
          break;
        case 9 : //  PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE, SSL_CONTEXT_ALGORITHM
          startParams.add(ADDRESS.toNamedParameterString(ADDRESS.parse(params.get(1)).toString()));
          startParams.add(NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(NETWORK_INTERFACE_PROTOCOL.parse(params.get(2)).toString()));
          startParams.add(HTTPS.toNamedParameterString(HTTPS.parse(params.get(3)).toString()));
          startParams.add(CLIENTAUTH.toNamedParameterString(CLIENTAUTH.parse(params.get(4)).toString()));
          startParams.add(KEYSTOREPATH.toNamedParameterString(KEYSTOREPATH.parse(params.get(5)).toString()));
          startParams.add(KEYSTOREPASS.toNamedParameterString(KEYSTOREPASS.parse(params.get(6)).toString()));
          startParams.add(KEYSTORETYPE.toNamedParameterString(KEYSTORETYPE.parse(params.get(7)).toString()));
          startParams.add(SSL_CONTEXT_ALGORITHM.toNamedParameterString(SSL_CONTEXT_ALGORITHM.parse(params.get(8)).toString()));
          break;
        default :
          throw new XACT_InvalidStartParameterCountException();
      }
    } catch (StringParameterParsingException e) {
      throw new XACT_InvalidTriggerStartParameterValueException(params.toString());
    }

    return startParams;
  }


  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return allParameters;
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter#build(java.util.Map)
   */
  @Override
  public StartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    HTTPStartParameter param = new HTTPStartParameter();
    param.port = PORT.getFromMap(paramMap);
    param.address = ADDRESS.getFromMap(paramMap);
    param.interfacePreference = NETWORK_INTERFACE_PROTOCOL.getFromMap(paramMap);
    param.keystoreParameter = HTTPS.getFromMap(paramMap);
    param.clientAuth = CLIENTAUTH.getFromMap(paramMap);
    param.keyStorePath = KEYSTOREPATH.getFromMap(paramMap);
    param.keyStoreType = KEYSTORETYPE.getFromMap(paramMap);
    param.keyStorePassword = KEYSTOREPASS.getFromMap(paramMap);
    param.keyStoreName = KEYSTORE_NAME.getFromMap(paramMap);
    param.trustStoreName = TRUSTSTORE_NAME.getFromMap(paramMap);
    param.sslContextAlgorithm = SSL_CONTEXT_ALGORITHM.getFromMap(paramMap);
    param.suppressRequestLogging = SUPPRESS_LOGGING.getFromMap(paramMap);
    return param;
  }



  public int getPort() {
    return port;
  }


  public String getAddress() {
    return address;
  }


  public boolean useHTTPs() {
    return keystoreParameter.useHTTPS();
  }

  public KeyStoreParameter getKeyStoreParameter() {
    return keystoreParameter;
  }

  public ClientAuth getClientAuth() {
    return clientAuth;
  }


  public String getKeyStorePath() {
    return keyStorePath;
  }


  public String getKeyStorePassword() {
    return keyStorePassword;
  }


  public String getKeyStoreType() {
    return keyStoreType;
  }

  public String getKeyStoreName() {
    return keyStoreName;
  }

  public String getTrustStoreName() {
    return trustStoreName;
  }


  public String getSSLContextAlgorithm() {
    return sslContextAlgorithm;
  }


  public boolean suppressRequestLogging() {
    return suppressRequestLogging;
  }

  public InterfaceProtocolPreference getProtocolPreference() {
    return this.interfacePreference;
  }

  public InetAddress getIP() throws XACT_InterfaceNoIPv6ConfiguredException, XACT_NetworkInterfaceNotFoundException,
      XACT_InterfaceNoIPConfiguredException {
    if (address == null || address.equals("")) {
      return null;
    }

    InternetAddressBean iab =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
            .getInternetAddress(address, null);
    if (iab != null) {
      return iab.getInetAddress();
    }
    if (logger.isInfoEnabled()) {
      logger.info("address " + address + " unknown in network configuration management.");
    }

    //else: abwärtskompatibel:
    boolean ipv6 = interfacePreference == InterfaceProtocolPreference.IPV6;
    boolean useLocalAddresses = false;
    return NetworkInterfaceUtils.getFirstIpAddressByInterfaceName(address, ipv6, useLocalAddresses);
  }

  @Override
  public String toString() {
    return String.format("[Address: '%s' Port:'%d']", address, port);
  }
}
