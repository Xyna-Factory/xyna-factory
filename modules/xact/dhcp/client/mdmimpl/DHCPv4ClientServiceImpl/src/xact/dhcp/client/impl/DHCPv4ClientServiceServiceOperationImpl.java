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
package xact.dhcp.client.impl;


import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;

import xact.dhcp.DHCPPacket;
import xact.dhcp.client.DHCPSendException;
import xact.dhcp.client.DHCPServer;
import xact.dhcp.client.DHCPv4ClientServiceServiceOperation;
import xact.dhcp.client.EncoderBuilder;
import xact.dhcp.client.LinkAddress;
import xact.dhcp.client.MACAddress;
import xact.dhcp.enums.DHCPMessageType;
import xact.dhcp.options.Option;
import xprc.synchronization.CorrelationId;


public class DHCPv4ClientServiceServiceOperationImpl implements ExtendedDeploymentTask, DHCPv4ClientServiceServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    EncoderBuilder.onDeployment();
    DHCPRequest.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    DHCPRequest.onUndeployment();
    EncoderBuilder.onUndeployment();
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  @Override
  public CorrelationId sendRequest(List<? extends DHCPServer> dhcpServerList, MACAddress mac, LinkAddress linkAddress,
      DHCPMessageType messageType, List<? extends Option> options) throws DHCPSendException {
    DHCPRequest request = new DHCPRequest( mac, messageType, linkAddress);
    for( Option option : options ) {
      request.addOption(option);
    }
    request.createRequest();
    for( DHCPServer dhcpServer :  dhcpServerList ) {
      request.sendTo(dhcpServer);
    }
    return new CorrelationId(request.getXid());
  }

  @Override
  public Container extractLease(DHCPPacket dhcpPacket) {
    DHCPOptionParser optionParser = new DHCPOptionParser(dhcpPacket);
    return new Container(optionParser.getMessageType(), optionParser.getLease() );
  }

  @Override
  public Container extractLeaseOptionsForRequest(DHCPPacket dhcpPacket) {
    DHCPOptionParser optionParser = new DHCPOptionParser(dhcpPacket);
    List<Option> options = new ArrayList<Option>();
    options.add( optionParser.getOption50_RequestedIPAddress() );
    options.add( optionParser.getOption51_IPAddressLeaseTime() );
    options.add( optionParser.getOption54_ServerIdentifier() );
    return new Container(optionParser.getMessageType(), new XynaObjectList<Option>(options, Option.class));
  }

}
