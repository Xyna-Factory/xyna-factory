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



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.trigger.tlvdecoding.radius.RadiusConfigurationDecoder;
import com.gip.xyna.xact.trigger.tlvencoding.database.LoadConfig;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusConfigurationEncoder;
import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;



public class XynaRadiusTrigger extends EventListener<XynaRadiusTriggerConnection, XynaRadiusStartParameter>
    implements
      IPropertyChangeListener {

  private DatagramSocket datagramSocket;
  private volatile boolean isStopping = false;

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusTrigger.class);

  public static final String XYNA_PROPERTY_RESET = "xyna.xact.dhcp.reloadconfig"; // modify xml file when changing this
  public static final String XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE = "xyna.xact.dhcp.receivebuffersize";

  public static final int DEFAULT_BUFFER_SIZE = 2576;
  private volatile int receiveBufferLength = DEFAULT_BUFFER_SIZE;

  RadiusConfigurationDecoder dec;
  RadiusConfigurationEncoder enc;


  public XynaRadiusTrigger() {
  }


  public void start(XynaRadiusStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {

    Collection<RadiusEncoding> list = null;

    LoadConfig loadConfig = new LoadConfig();

    try {
      loadConfig.setUp();
      list = loadConfig.loadRadiusEntries();
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Failed to read from Radius database", e);
      }
    }

    if (list.size() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Dataset from Radius database empty");
      }
      throw new IllegalArgumentException("Dataset from Radius database empty");
    }

    this.dec = new RadiusConfigurationDecoder(new ArrayList<RadiusEncoding>(list));
    this.enc = new RadiusConfigurationEncoder(new ArrayList<RadiusEncoding>(list));

    try {
      this.datagramSocket = new DatagramSocket(sp.getPort(), sp.getAddress());
    } catch (SocketException e) {
      throw new RuntimeException("Error initializing DatagramSocket:", e);
    }
  }


  public static void main(String[] args) throws XACT_TriggerCouldNotBeStartedException, IOException {

    //    XynaRadiusTrigger trigger = new XynaRadiusTrigger();
    //    // trigger.start(new DHCPStartParameter("localhost", new int[] {1547, 1547}));
    //    trigger.start(new XynaRadiusStartParameter(InetAddress.getLocalHost(), 1812));
    //    // trigger.currentDhcpOptionConfiguration = new DhcpOptionDefinition[0];
    //
    //    XynaRadiusTriggerConnection con = trigger.receive();
    //    System.out.println("done");

  }


  private InetAddress localAddress;


  public XynaRadiusTriggerConnection receive() {

    try {
      DatagramPacket datagramPacket = new DatagramPacket(new byte[receiveBufferLength], receiveBufferLength);
      datagramSocket.receive(datagramPacket);
      logger.debug("RadiusTrigger received Packet from " + datagramPacket.getAddress().toString());
      if (localAddress == null) {
        localAddress = datagramSocket.getLocalAddress();
      }
      // return new DHCPTriggerConnection(datagramPacket, currentDhcpOptionConfiguration.clone());
      return new XynaRadiusTriggerConnection(datagramPacket, this.dec, this.enc, datagramSocket, localAddress);
    } catch (IOException e) {
      if (!isStopping) {
        throw new RuntimeException("Error Receiving Radius UDP Packet:", e);
      }
    }

    return null;
  }


  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, XynaRadiusTriggerConnection con) {
    ignoreRequest(con);
  }


  /**
   * called by Xyna Processing to stop the Trigger. should make sure, that start() may be called again directly
   * afterwards. connection instances returned by the method receive() should not be expected to work after stop() has
   * been called.
   */
  public void stop() {
    isStopping = true;
    if (datagramSocket != null) {
      datagramSocket.close();
    }
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   *
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(XynaRadiusTriggerConnection con) {
    ignoreRequest(con);
  }


  private void ignoreRequest(XynaRadiusTriggerConnection con) {
    if (logger.isTraceEnabled()) {
      logger.trace("ignoring radius request!");
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {

    return "Trigger for Radius Messages.";
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> list = new ArrayList<String>();
    // list.add(XYNA_PROPERTY_RESET);
    list.add(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    return list;
  }


  public void propertyChanged() {

    String rcvBufferString = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    try {
      this.receiveBufferLength = Integer.valueOf(rcvBufferString);
      if (logger.isInfoEnabled()) {
        logger.info(getClass().getSimpleName() + " is using receive buffer size <" + this.receiveBufferLength + ">");
      }
    } catch (NumberFormatException e) {
      this.receiveBufferLength = DEFAULT_BUFFER_SIZE;
      if (logger.isInfoEnabled()) {
        logger.info(getClass().getSimpleName() + " is using default receive buffer size <" + this.receiveBufferLength + ">");
      }
    }
  }

}
