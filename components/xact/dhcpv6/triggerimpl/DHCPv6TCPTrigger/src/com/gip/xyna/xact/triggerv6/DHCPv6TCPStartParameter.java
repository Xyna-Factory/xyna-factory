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
package com.gip.xyna.xact.triggerv6;

import java.net.Inet6Address;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;

public class DHCPv6TCPStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPv6TCPStartParameter.class);


  private String localAddress;
  private int[] remotePorts;
  private String servermacaddress;
  
  private int remotePort;


  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public DHCPv6TCPStartParameter() {
  }


  public DHCPv6TCPStartParameter(String localAddress, int[] remotePorts, String servermacaddress) {
    this.localAddress = localAddress;
    this.remotePorts = remotePorts;
    this.servermacaddress = servermacaddress;
  }
/*
  public DHCPStartParameter(String localAddress, int remotePort) {
    this.localAddress = localAddress;
    this.remotePort = remotePort;
  }
*/

  /**
   * Is called by XynaProcessing with the parameters provided by the deployer
   * @return StartParameter Instance which is used to instantiate corresponding Trigger
   */
 
  public StartParameter build(String... args) throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    if (args == null || args.length != 3) {
      throw new XACT_InvalidStartParameterCountException();
    }

    String localAddress = args[0];
    try
    {
      InetAddress check = InetAddress.getByName(localAddress);
      
      if(!(check instanceof Inet6Address)&&logger.isDebugEnabled())
      {
        logger.debug("Warning: IPv4 Address used for socket of DHCPv6 Trigger!");
      }
    }
    catch(Exception e)
    {
      throw new IllegalArgumentException("DHCPv6 Trigger Startparameters: No valid IP Address given!"+e);
    }
   
    
    String[] remotePortsAsStrings = args[1].split(",");
    int[] remotePorts = new int[remotePortsAsStrings.length];
    if(remotePorts.length!=2 && remotePorts.length!=3) // dritter optionaler Portparameter fuer LeaseQuery
    {
      throw new XACT_InvalidStartParameterCountException();
    }
    for (int i=0; i<remotePortsAsStrings.length; i++) {
      try
      {
        remotePorts[i] = Integer.valueOf(remotePortsAsStrings[i]);
      }
      catch(Exception e)
      {
        throw new IllegalArgumentException("DHCPv6 Trigger Startparameters: Given ports not numbers! "+e);
      }
    }
    String servermacaddress = args[2];
    if (!servermacaddress.toUpperCase().matches("[0-9A-F]{1,2}(:[0-9A-F]{1,2}){5}")) {
      throw new IllegalArgumentException("DHCPv6 Trigger Startparameters: Invalid MAC address: <" + servermacaddress + ">.");
  }


    return new DHCPv6TCPStartParameter(localAddress, remotePorts, servermacaddress);

  }

  

  /**
   * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D) are valid,
   *         then this method should return new String[]{{"descriptionA", "descriptionB"}, {"descriptionA",
   *         "descriptionC", "descriptionD"}}
   */
  public String[][] getParameterDescriptions() {
    return new String[][] {{"The local ip address to listen on",
                    "A list of valid remote ports separated by commas without spaces, e.g. '547,547'. First port is port to listen, second port is port for replies/sending. A third port can be given to use a custom port for leaseQuery replies. (default:546)",
                    "The MAC Address of the Server to use it in Option 2 / ServerIdentifier DUID"}};
  }


  public String getLocalIpAddress() {
    return localAddress;
  }


  public int[] getRemotePorts() {
    return remotePorts;
  }

  public int getRemotePort() {
    return remotePort;
  }
  
  public String getServerMacAddress()
  {
    return servermacaddress;
  }

  
}
