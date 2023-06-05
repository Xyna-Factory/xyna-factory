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
                     de("Name der IP im NetworkConfigurationManagement oder Network-Interface-name (Default:= akzeptiert Connections von allen Interfaces)").
                     en("Name of ip in NetworkConfigurationManagement or network interface name (Default=accept connections from all interfaces)").build() ).
      defaultValue("").build();
  public static final StringParameter<InterfaceProtocolPreference> NETWORK_INTERFACE_PROTOCOL = 
      StringParameter.typeEnum(InterfaceProtocolPreference.class, "protocol", true).
      documentation( Documentation.de("Network-Interface-Protokoll").en("Network interface protocol").build() ).
      defaultValue(InterfaceProtocolPreference.IPV4).build();
  public static final StringParameter<KeyStoreParameter> HTTPS = 
      StringParameter.typeEnum(KeyStoreParameter.class, "https", true).
      documentation( Documentation
                     .de("Wird HTTPS verwendet und wenn, wie wird der SSLContext befüllt. NONE: http, FILE: https und Angabe eines gemeinsamen Key/Trust-Files, KEY_MGMT: https und Angabe eines KeyStores und TrustManagers")
                     .en("Use HTTPS and if so, how will the SSLContext be initialised. NONE: http, FILE: https und parameters for a shared Key/Trust-File, KEY_MGMT: https and para,eters for a key store and a trust manager").build() ).
      defaultValue(KeyStoreParameter.NONE).build();
  public static final StringParameter<ClientAuth> CLIENTAUTH = 
      StringParameter.typeEnum(ClientAuth.class, "clientauth").
      documentation( Documentation.de("Client-Authentication").en("Clientauthentication").build() ).
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
  public static final StringParameter<Boolean> SUPPRESS_LOGGING = StringParameter.typeBoolean("suppressRequestLogging").
                  documentation(Documentation.de("Unterdrücke requestabhängiges Logging").en("Suppress request dependent logging.").build()).
                  optional().defaultValue(false).build();
  
  public static final List<StringParameter<?>> allParameters = 
      StringParameter.asList( PORT, ADDRESS, NETWORK_INTERFACE_PROTOCOL, HTTPS, CLIENTAUTH, KEYSTOREPATH, KEYSTOREPASS, KEYSTORETYPE, KEYSTORE_NAME, TRUSTSTORE_NAME, SUPPRESS_LOGGING);
  
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter#convertToNewParameters(java.util.List)
   */
  @Override
  public List<String> convertToNewParameters(List<String> params) throws XACT_InvalidStartParameterCountException,
      XACT_InvalidTriggerStartParameterValueException {
    List<String> startParams = new ArrayList<String>();
    startParams.add( PORT.toNamedParameterString(params.get(0) ) );
    switch( params.size() ) {
      case 1:
        break;
      case 2:
        startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
        break;
      case 3:
        startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
        startParams.add( NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(params.get(2) ) );
        break;
      case 6:
        startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
        //InterfaceProtocolPreference.IPV4 ist default
        startParams.add( HTTPS.toNamedParameterString(params.get(2) ) );
        startParams.add( CLIENTAUTH.toNamedParameterString(params.get(3) ) );
        startParams.add( KEYSTOREPATH.toNamedParameterString(params.get(4) ) );
        startParams.add( KEYSTOREPASS.toNamedParameterString(params.get(5) ) );
        //KEYSTORETYPE ist DefaultJKS
        break;
      case 7:
        boolean withNETWORK_INTERFACE_PROTOCOL;
        try {
          HTTPS.parse(params.get(2));
          //Param 2 lies sich als boolean parsen -> kein Protocol
          withNETWORK_INTERFACE_PROTOCOL = false; 
        } catch(StringParameterParsingException e) {
          //Param 2 lies sich nicht als boolean parsen -> wahrscheinlich ein Protocol
          withNETWORK_INTERFACE_PROTOCOL = true;
        }
        if( withNETWORK_INTERFACE_PROTOCOL ) {
          startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
          startParams.add( NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(params.get(2) ) );
          startParams.add( HTTPS.toNamedParameterString(params.get(3) ) );
          startParams.add( CLIENTAUTH.toNamedParameterString(params.get(4) ) );
          startParams.add( KEYSTOREPATH.toNamedParameterString(params.get(5) ) );
          startParams.add( KEYSTOREPASS.toNamedParameterString(params.get(6) ) );
          //KEYSTORETYPE ist DefaultJKS
        } else {
          startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
          //InterfaceProtocolPreference.IPV4 ist default
          startParams.add( HTTPS.toNamedParameterString(params.get(2) ) );
          startParams.add( CLIENTAUTH.toNamedParameterString(params.get(3) ) );
          startParams.add( KEYSTOREPATH.toNamedParameterString(params.get(4) ) );
          startParams.add( KEYSTOREPASS.toNamedParameterString(params.get(5) ) );
          startParams.add( KEYSTORETYPE.toNamedParameterString(params.get(6) ) );
        }
        break;
      case 8:
        startParams.add( ADDRESS.toNamedParameterString(params.get(1) ) );
        startParams.add( NETWORK_INTERFACE_PROTOCOL.toNamedParameterString(params.get(2) ) );
        startParams.add( HTTPS.toNamedParameterString(params.get(3) ) );
        startParams.add( CLIENTAUTH.toNamedParameterString(params.get(4) ) );
        startParams.add( KEYSTOREPATH.toNamedParameterString(params.get(5) ) );
        startParams.add( KEYSTOREPASS.toNamedParameterString(params.get(6) ) );
        startParams.add( KEYSTORETYPE.toNamedParameterString(params.get(7) ) );
        break;
      default:
        throw new XACT_InvalidStartParameterCountException();
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

}
