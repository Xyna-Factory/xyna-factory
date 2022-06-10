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
package com.gip.xyna.xact.trigger;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

public class SFTPTriggerStartParameter implements StartParameter {

  private static final Logger logger = CentralFactoryLogging.getLogger(SFTPTriggerStartParameter.class);

  private String host ="";
  private int port = 0;
  private boolean passwordauth = false;
  private boolean publickeyauth = false;
  private boolean alwaysauth = false;
  
  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public SFTPTriggerStartParameter() {
  }

  public SFTPTriggerStartParameter(String hostinput, int portinput, boolean passwordinput, boolean publickeyinput, boolean alwaysauthenticateinput) {
    host = hostinput;
    port = portinput;
    passwordauth=passwordinput;
    publickeyauth=publickeyinput;
    alwaysauth = alwaysauthenticateinput;
  }

  
  public String getHost()
  {
    return host;
  }
  public int getPort()
  {
    return port;
  }
  public boolean getPasswordAuth()
  {
    return passwordauth;
  }
  public boolean getPublicKeyAuth()
  {
    return publickeyauth;
  }
  public boolean getAlwaysAuth()
  {
    return alwaysauth;
  }
  
  /**
  * Is called by XynaProcessing with the parameters provided by the deployer
  * @return StartParameter Instance which is used to instantiate corresponding Trigger
  */
  public StartParameter build(String ... args) throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    
    if(args == null || args.length!=5)
    {
      throw new RuntimeException("Not enough startparameters for SFTP Trigger. It needs hostaddress, port, passwordauthentication, publickeyauthentication, alwaysauthenticate!");
    }
    String host = args[0];
    int port = -1;
    boolean password = false;
    boolean publickey = false;
    boolean alwauth = false;
    try
    {
      port = Integer.parseInt(args[1]);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Given port invalid: ",e);
    }
    try
    {
      password = Boolean.parseBoolean(args[2]);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Given passwordauthentication boolean invalid: ",e);
    }
    try
    {
      publickey = Boolean.parseBoolean(args[3]);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Given publickeyauthentication boolean invalid: ",e);
    }
    try
    {
      alwauth = Boolean.parseBoolean(args[4]);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Given alwaysauthenticate boolean invalid: ",e);
    }
    
    
    
    return new SFTPTriggerStartParameter(host,port,password,publickey,alwauth);
  }

  /**
  * 
  * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)
  *    are valid, then this method should return new String[]{{"descriptionA", "descriptionB"},
  *     {"descriptionA", "descriptionC", "descriptionD"}}
  */
  public String[][] getParameterDescriptions() {
    return new String[][]{{"hostaddress", "port", "passwordauthentication", "publickeyauthentication", "alwaysauthenticate"}};
  }
  

  public InetAddress getIP() throws XACT_InterfaceNoIPv6ConfiguredException, XACT_NetworkInterfaceNotFoundException,
      XACT_InterfaceNoIPConfiguredException {
    if (host == null || host.equals("")) {
      return null;
    }

    InternetAddressBean iab =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
            .getInternetAddress(host, null);
    if (iab != null) {
      return iab.getInetAddress();
    }
    if (logger.isInfoEnabled()) {
      logger.info("address " + host + " unknown in network configuration management.");
    }

    try {
      return InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      //interfacename?
    }
    //else: abwärtskompatibel:
    boolean ipv6 = true;
    boolean useLocalAddresses = false;
    return NetworkInterfaceUtils.getFirstIpAddressByInterfaceName(host, ipv6, useLocalAddresses);
  }

}
