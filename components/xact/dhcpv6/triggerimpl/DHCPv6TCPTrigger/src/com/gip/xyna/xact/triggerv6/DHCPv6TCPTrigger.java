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
package com.gip.xyna.xact.triggerv6;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.triggerv6.dhcp.DHCPTRIGGER_InitializationException;
import com.gip.xyna.xact.triggerv6.dhcp.DHCPTRIGGER_ReceiveException;
import com.gip.xyna.xact.triggerv6.dhcp.DHCPTRIGGER_SocketCloseException;
import com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.databasev6.LoadConfigv6;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xnwh.persistence.ODS;

public class DHCPv6TCPTrigger extends EventListener<DHCPv6TCPTriggerConnection, DHCPv6TCPStartParameter> implements IPropertyChangeListener, StatisticsReporterLegacy {

  private ServerSocket socket;
  private volatile boolean isStopping = false;

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPv6TCPTrigger.class);

  public static final String XYNA_PROPERTY_RESET = "xyna.xact.dhcp.reloadconfig"; // modify xml file when changing this
  public static final String XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE = "xyna.xact.dhcp.receivebuffersize";
  
  public static final String RELOADOPTIONSV6 = "xdnc.dhcpv6.config.reloadoptionsv6";

  private volatile boolean reloading = false;
  private volatile boolean needToReloadAgain = false;

  public static final int DEFAULT_BUFFER_SIZE = 2576;
  private volatile int receiveBufferLength = DEFAULT_BUFFER_SIZE;

  private String classDescription;

  private AtomicLong rejectCounter = new AtomicLong(0);

  private ODS ods;

  //private volatile DhcpOptionDefinition[] currentDhcpOptionConfiguration;
  
  DHCPv6ConfigurationDecoder dec;
  DHCPv6ConfigurationEncoder enc;
  
  private int replyport; // Port for replies
  private int leasequeryreplyport; // Port for LeaseQueryv6 replies
  private int listeningport;
  
  private String servermacaddress;
  


  public DHCPv6TCPTrigger() {
  }


  public void start(DHCPv6TCPStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {

    classDescription = concatenateParameters(sp);
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
    .addPropertyChangeListener(this);
    
//    ods = ODSImpl.getInstance();
//    try {
//      ods.registerStorable(DhcpOptionDefinition.class);
//    } catch (PersistenceLayerException e) {
//      throw new DHCPTRIGGER_InitializationException(classDescription, e);
//    }
//
//    try {
//      reloadConfiguration();
//    } catch (Exception e) {
    //      throw new DHCPTRIGGER_InitializationException(classDescription, e);
    //    }

    
    Collection<DHCPv6Encoding> liste = null;
    
    LoadConfigv6 anbindung = new LoadConfigv6();
    
    
    try {
      anbindung.setUp();
      liste = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      if(logger.isDebugEnabled())logger.debug("Failed to read from database");
    }

    if (liste.size() == 0) {
      if(logger.isDebugEnabled())logger.debug("Dataset from DHCPv6 database empty");
      throw new IllegalArgumentException("Dataset from DHCPv6 database empty");
    }

    this.dec = new DHCPv6ConfigurationDecoder(new ArrayList<DHCPv6Encoding>(liste));
    this.enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));
    
    try {
      //this.datagramSocket = new DatagramSocket(1547, InetAddress.getByName(sp.getLocalIpAddress()));
      //this.datagramSocket = new DatagramSocket(1547, InetAddress.getByName("10.0.9.101"));
        this.socket = new ServerSocket(sp.getRemotePorts()[0]);
    }
    catch (IOException e) {
      throw new DHCPTRIGGER_InitializationException(classDescription, e);
    } 
    listeningport = sp.getRemotePorts()[0];
    replyport = sp.getRemotePorts()[1]; // Port for replies
    if(sp.getRemotePorts().length>2)
    {
      leasequeryreplyport=sp.getRemotePorts()[2];
    }
    else
    {
      leasequeryreplyport=546; // default port for leasequery replies
    }
    servermacaddress = sp.getServerMacAddress();
    
    //addStatistics
    try
    {
      XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().registerNewStatistic("DHCPv6TCPTrigger", this);      
    }
    catch(Exception e)
    {
      logger.info("DHCPv6TCPTrigger Statistics could not be initialized. ",e);
    }
    
  }


  public static void main(String[] args) throws XACT_TriggerCouldNotBeStartedException, IOException {

    DHCPv6TCPTrigger trigger = new DHCPv6TCPTrigger();
    //trigger.start(new DHCPStartParameter("localhost", new int[] {1547, 1547}));
    trigger.start(new DHCPv6TCPStartParameter("localhost", new int[] {1547, 1546},"00:00:00:00:00:00"));
    //trigger.currentDhcpOptionConfiguration = new DhcpOptionDefinition[0];

    DHCPv6TCPTriggerConnection con = trigger.receive();
    //con.parseDhcpPaket();
    System.out.println("done");

  }


  private static String concatenateParameters(DHCPv6TCPStartParameter sp) {
    StringBuilder sb = new StringBuilder();
    sb.append("Local address: '").append(sp.getLocalIpAddress()).append("', valid remote ports: ");
    for (int i = 0; i < sp.getRemotePorts().length; i++) {
      sb.append("'").append(sp.getRemotePorts()[i]).append("'");
      if (i < sp.getRemotePorts().length - 1) {
        sb.append(", ");
      }
    }
    sb.append("', Server MAC Address: '").append(sp.getServerMacAddress()).append("'");
    return sb.toString();
  }


  /*
  private static String concatenateParameters(DHCPStartParameter sp) {
    StringBuilder sb = new StringBuilder();
    sb.append("Local address: '").append(sp.getLocalIpAddress()).append("', valid remote port: ");
      sb.append("'").append(sp.getRemotePort()).append("'");
    return sb.toString();
  }
*/
  
  
  public DHCPv6TCPTriggerConnection receive() {

    
    try {

      Socket connectionSocket = socket.accept();
      connectionSocket.setTcpNoDelay(true);
      DataInputStream input = new DataInputStream(connectionSocket.getInputStream());
      //DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

      
      List<Byte> inputlist = new ArrayList<Byte>();
      
      boolean readit=true;
      while(readit)
      {
        try
        {
          int length = input.readUnsignedShort();
          inputlist.add((byte)(length/256));
          inputlist.add((byte)(length%256));
          
          logger.info("DHCPv6TCPTrigger: Length of incoming BulkLeaseQuery stream: "+length);
          
          for(int i=0;i<length;i++)
          {
            inputlist.add(input.readByte());
          }
          readit = false;
          
        }
        catch(EOFException eof)
        {
          readit = false;
        }
        catch(Exception e)
        {
          logger.error("TCP Bulk LeaseQuery Stream could not be read.",e);
          readit = false;
        }
      }
      
      byte[] inputresult = new byte[inputlist.size()];
      for(int i=0;i<inputresult.length;i++)
      {
        inputresult[i] = inputlist.get(i);
      }

      return new DHCPv6TCPTriggerConnection(inputresult, this.dec, this.enc, replyport, leasequeryreplyport, connectionSocket, servermacaddress);
    } catch (IOException e) {
      if (!isStopping) {
        // TODO maybe the receive exception should be a runtime exception itself
        throw new RuntimeException(new DHCPTRIGGER_ReceiveException("TODO", e));
      }
    }

    return null;

  }


  /**
   * Called by Xyna Processing if there are not enough system capacities to process the request.
   */
  protected void onProcessingRejected(String cause, DHCPv6TCPTriggerConnection con) {
    ignoreRequest(con);
    rejectCounter.incrementAndGet();
    //String mac = "";
    //byte[] macbytes = con.getRawPacket().getData();
    // funktioniert nur, wenn ClientID vom Typ DUID LLT und an erster Stelle in Relay Message
    //if(macbytes.length>60) mac = mac + Integer.toHexString(macbytes[54])+":"+Integer.toHexString(macbytes[55])+":"+Integer.toHexString(macbytes[56])+":"+Integer.toHexString(macbytes[57])+":"+Integer.toHexString(macbytes[58])+":"+Integer.toHexString(macbytes[59]);
    logger.debug("MAC rejected!"); // : "+mac);
  }


  /**
   * called by Xyna Processing to stop the Trigger. should make sure, that start() may be called again directly
   * afterwards. connection instances returned by the method receive() should not be expected to work after stop() has
   * been called.
   */
  public void stop() throws DHCPTRIGGER_SocketCloseException {
    isStopping = true;
    if (socket != null) {
      try {
        socket.close();
      }
      catch (IOException e) {
        throw new DHCPTRIGGER_SocketCloseException(listeningport);
      }
    }
    
    //removeStatistics
//    XynaFactory.getInstance().getFactoryManagement().getXynaStatistics().unregisterStatistics("DHCPv6TCPTrigger" + instanceName); #
    XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().unregisterStatistics("DHCPv6Trigger");
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(DHCPv6TCPTriggerConnection con) {
    ignoreRequest(con);
  }


  private void ignoreRequest(DHCPv6TCPTriggerConnection con) {
    if (logger.isTraceEnabled()) {
      logger.trace("ignoring dhcp request: ");// + con.getDhcpPacket());
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    //return classDescription;
    return "Trigger for DHCPv6 Messages.";
  }

/*
  private synchronized void reloadConfiguration() throws PersistenceLayerException {

    ODSConnection defaultConnection = ods.openConnection(ODSConnectionType.DEFAULT);
    Collection<DhcpOptionDefinition> storedOptionDefinitions = null;
    try {
      storedOptionDefinitions = defaultConnection.loadCollection(DhcpOptionDefinition.class);
    } finally {
      defaultConnection.closeConnection();
    }

    validateDhcpOptions(storedOptionDefinitions);

    //currentDhcpOptionConfiguration = storedOptionDefinitions.toArray(new DhcpOptionDefinition[storedOptionDefinitions
    //                .size()]);

  }


  private void validateDhcpOptions(Collection<DhcpOptionDefinition> optionDefinitions) {
    // TODO implement this
  }
*/

  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> list = new ArrayList<String>();
//    list.add(XYNA_PROPERTY_RESET);
    list.add(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    list.add(RELOADOPTIONSV6);
    return list;
  }


  public void propertyChanged() {

    String rcvBufferString = XynaFactory.getInstance().getFactoryManagement()
                    .getProperty(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    try {
      this.receiveBufferLength = Integer.valueOf(rcvBufferString);
      logger.info(getClass().getSimpleName() + " is using receive buffer size <" + this.receiveBufferLength + ">");
    } catch (NumberFormatException e) {
      this.receiveBufferLength = DEFAULT_BUFFER_SIZE;
      logger.info(getClass().getSimpleName() + " is using default receive buffer size <" + this.receiveBufferLength
                      + ">");
    }

    Collection<DHCPv6Encoding> liste = null;
    
    LoadConfigv6 anbindung = new LoadConfigv6();
    
    
    try {
      anbindung.setUp();
      liste = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      if(logger.isDebugEnabled())logger.debug("Failed to read from database",e);
    }

    if (liste.size() == 0) {
      if(logger.isDebugEnabled())logger.debug("Dataset from DHCPv6 database empty");
      //throw new IllegalArgumentException("Dataset from DHCPv6 database empty");
    }

    try
    {
      logger.info("DHCPv6Trigger: Reinitializing DHCPv6 Encoder and Decoder ...");
      this.dec = new DHCPv6ConfigurationDecoder(new ArrayList<DHCPv6Encoding>(liste));
      this.enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));
      
    }
    catch(Exception e)
    {
      logger.info("Problems reinitializing DHCPv6 Encoder and Decoder in DHCPv6 Trigger:",e);
    }

    
    
//    synchronized (this) {
//      if (reloading) {
//        needToReloadAgain = true;
//        return;
//      }
//    }
//
//    while (needToReloadAgain) {
//      needToReloadAgain = false;
//      String reset = XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_RESET);
//      if (reset != null && Boolean.valueOf(reset)) {
//        try {
//          reloadConfiguration();
//        } catch (PersistenceLayerException e1) {
//          logger.error("Failed to reload configuration for " + getClass().getSimpleName()
//                          + ", using old configuration.");
//        }
//        try {
//          XynaFactory.getInstance().getFactoryManagement().setProperty(XYNA_PROPERTY_RESET, "false");
//        } catch (PersistenceLayerException e) {
//          logger.warn("Failed to set property " + XYNA_PROPERTY_RESET + " after reloading "
//                          + getClass().getSimpleName() + " configuration.");
//        }
//      }
//
//      synchronized (this) {
//        if (!needToReloadAgain) {
//          reloading = false;
//          return;
//        }
//      }
//    }

  }
  
  
  public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {
    StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[1];
    report[0] = new StatisticsReportEntryLegacy() {

      public Object getValue() {
        return rejectCounter.get();
      }


      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }


      public String getDescription() {
        return "Count of rejected trigger events";
      }
    };

    return report;
  }

}
