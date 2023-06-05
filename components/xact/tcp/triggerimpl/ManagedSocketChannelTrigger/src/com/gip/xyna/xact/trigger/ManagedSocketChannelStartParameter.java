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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import org.apache.log4j.Logger;

import xact.tcp.SocketChannelCreationParameter;
import xact.tcp.SocketChannelManagement;
import xact.tcp.StaticMessageKeepAliveHandler;

public class ManagedSocketChannelStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(ManagedSocketChannelStartParameter.class);

  private SocketChannelCreationParameter creationParams;
  private Selector selector;
  private String socketMgmtName;
  
  
  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public ManagedSocketChannelStartParameter() {
  }
  
  
  public ManagedSocketChannelStartParameter(SocketChannelCreationParameter creationParams, Selector selector, String socketMgmtName) {
    this.creationParams = creationParams;
    this.socketMgmtName = socketMgmtName;
    this.selector = selector;
  }

  /**
  * Is called by XynaProcessing with the parameters provided by the deployer
  * @return StartParameter Instance which is used to instantiate corresponding Trigger
  */
  public StartParameter build(String ... args) throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    if (args.length < 2) {
      throw new XACT_InvalidStartParameterCountException();
    }
    SocketChannelCreationParameter creationParams;
    InternetAddressBean bean = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement().getInternetAddress(args[0], null);
    String host;
    if (bean == null) {
      host = args[0];
    } else {
      host = bean.getInetAddress().getHostAddress();
    }
    String socketMgmtName = host + ":" + args[1];
    try {
      creationParams = SocketChannelCreationParameter.create().address(new InetSocketAddress(host, Integer.parseInt(args[1])))
                                                              .name(socketMgmtName);
    } catch (NumberFormatException e) {
      throw new XACT_InvalidTriggerStartParameterValueException(args[1], e);
    }
    try {
      Selector selector = Selector.open();
      creationParams.selector(selector, SelectionKey.OP_READ);
      return new ManagedSocketChannelStartParameter(creationParams, selector, socketMgmtName);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public String getSocketMgmtName() {
    return socketMgmtName;
  }
  
  
  public Selector getSelector() {
    return selector;
  }
  
  
  public SocketChannelCreationParameter getSocketChannelCreationParameter() {
    return creationParams;
  }
  

  /**
  * 
  * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)
  *    are valid, then this method should return new String[]{{"descriptionA", "descriptionB"},
  *     {"descriptionA", "descriptionC", "descriptionD"}}
  */
  public String[][] getParameterDescriptions() {
    return new String[][]{{"hostname - resolution as symbolic name from IpManagement will be tried", "port"}};
  }

}
