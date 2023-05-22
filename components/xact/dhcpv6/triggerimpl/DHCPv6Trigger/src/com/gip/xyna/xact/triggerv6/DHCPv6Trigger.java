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



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.tlvdecoding.dhcpv6.DHCPv6ConfigurationDecoder;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xact.triggerv6.dhcp.DHCPTRIGGER_InitializationException;
import com.gip.xyna.xact.triggerv6.dhcp.DHCPTRIGGER_ReceiveException;
import com.gip.xyna.xact.triggerv6.tlvencoding.databasev6.LoadConfigv6;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.statistics.XynaStatistics.StatisticsReportEntry;
import com.gip.xyna.xfmg.statistics.XynaStatistics.StatisticsReporter;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class DHCPv6Trigger extends EventListener<DHCPv6TriggerConnection, DHCPv6StartParameter>
    implements
      IPropertyChangeListener,
      StatisticsReporter,
      StatisticsReporterLegacy {

  private DatagramSocket datagramSocket;
  private volatile boolean isStopping = false;

  private static Logger logger = CentralFactoryLogging.getLogger(DHCPv6Trigger.class);

  public static final String XYNA_PROPERTY_RESET = "xyna.xact.dhcp.reloadconfig"; // modify xml file when changing this
  public static final String XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE = "xyna.xact.dhcp.receivebuffersize";

  public static final String RELOADOPTIONSV6 = "xdnc.dhcpv6.config.reloadoptionsv6";


  public static final int DEFAULT_BUFFER_SIZE = 2576;
  private volatile int receiveBufferLength = DEFAULT_BUFFER_SIZE;

  private String classDescription;

  private AtomicLong receivedCounter = new AtomicLong(0);
  private AtomicLong rejectCounter = new AtomicLong(0);

  DHCPv6ConfigurationDecoder dec;
  DHCPv6ConfigurationEncoder enc;

  private int replyport; // Port for replies
  private int leasequeryreplyport; // Port for LeaseQueryv6 replies

  private String servermacaddress;


  public DHCPv6Trigger() {
  }


  public void start(DHCPv6StartParameter sp) throws XACT_TriggerCouldNotBeStartedException {

    classDescription = concatenateParameters(sp);

    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
        .addPropertyChangeListener(this);

    Collection<DHCPv6Encoding> list = null;

    LoadConfigv6 loadConfig = new LoadConfigv6();

    try {
      loadConfig.setUp();
      list = loadConfig.loadDHCPEntries();
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Failed to read from database");
      }
    }

    if (list.size() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Dataset from DHCPv6 database empty");
      }
      throw new IllegalArgumentException("Dataset from DHCPv6 database empty");
    }

    this.dec = new DHCPv6ConfigurationDecoder(new ArrayList<DHCPv6Encoding>(list));
    this.enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(list));

    try {
      this.datagramSocket = new DatagramSocket(sp.getRemotePorts()[0], sp.getIP());

    } catch (SocketException e) {
      throw new DHCPTRIGGER_InitializationException(classDescription, e);
    }

    replyport = sp.getRemotePorts()[1]; // Port for replies
    if (sp.getRemotePorts().length > 2) {
      leasequeryreplyport = sp.getRemotePorts()[2];
    } else {
      leasequeryreplyport = 546; // default port for leasequery replies
    }
    servermacaddress = sp.getServerMacAddress();

    //addStatistics
    try {
      registerStatistics();
    } catch (Exception e) {
      logger.info("DHCPv6Trigger Statistics could not be initialized. ", e);
    }

  }


  public static void main(String[] args) throws XACT_TriggerCouldNotBeStartedException, IOException {

    DHCPv6Trigger trigger = new DHCPv6Trigger();
    //trigger.start(new DHCPStartParameter("localhost", new int[] {1547, 1547}));
    trigger.start(new DHCPv6StartParameter(InetAddress.getLocalHost(), new int[] {1547, 1546}, "00:00:00:00:00:00"));
    //trigger.currentDhcpOptionConfiguration = new DhcpOptionDefinition[0];

    DHCPv6TriggerConnection con = trigger.receive();
    //con.parseDhcpPaket();
    System.out.println("done");

  }


  private static String concatenateParameters(DHCPv6StartParameter sp) {
    StringBuilder sb = new StringBuilder();
    sb.append("Local address: '").append(sp.getIP().getHostAddress()).append("', valid remote ports: ");
    for (int i = 0; i < sp.getRemotePorts().length; i++) {
      sb.append("'").append(sp.getRemotePorts()[i]).append("'");
      if (i < sp.getRemotePorts().length - 1) {
        sb.append(", ");
      }
    }
    sb.append("', Server MAC Address: '").append(sp.getServerMacAddress()).append("'");
    return sb.toString();
  }


  public DHCPv6TriggerConnection receive() {
    try {
      // TODO does the access concerning currentDhcpOptionConfiguration have to be synchronized or is volatile enough?
      receivedCounter.incrementAndGet();
      DatagramPacket datagramPacket = new DatagramPacket(new byte[receiveBufferLength], receiveBufferLength);
      datagramSocket.receive(datagramPacket);
      logger.debug("TriggerV6 received Packet from " + datagramPacket.getAddress().toString());
      //return new DHCPTriggerConnection(datagramPacket, currentDhcpOptionConfiguration.clone());
      return new DHCPv6TriggerConnection(datagramPacket, this.dec, this.enc, replyport, leasequeryreplyport,
                                         datagramSocket, servermacaddress);
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
  protected void onProcessingRejected(String cause, DHCPv6TriggerConnection con) {
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
  public void stop() {
    isStopping = true;
    if (datagramSocket != null) {
      datagramSocket.close();
    }

    //removeStatistics
    unregisterStatistics();
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
    .removePropertyChangeListener(this);
  }


  /**
   * called when a triggerconnection generated by this trigger was not accepted by any filter registered to this trigger
   * @param con corresponding triggerconnection
   */
  public void onNoFilterFound(DHCPv6TriggerConnection con) {
    ignoreRequest(con);
  }


  private void ignoreRequest(DHCPv6TriggerConnection con) {
    if (logger.isTraceEnabled()) {
      logger.trace("ignoring dhcp request: ");// + con.getDhcpPacket());
    }
  }


  /**
   * @return description of this trigger
   */
  public String getClassDescription() {
    return "Trigger for DHCPv6 Messages.";
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> list = new ArrayList<String>();
    //    list.add(XYNA_PROPERTY_RESET);
    list.add(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    list.add(RELOADOPTIONSV6);
    return list;
  }


  public void propertyChanged() {

    String rcvBufferString =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XYNA_PROPERTY_DHCP_RECEIVE_BUFFER_SIZE);
    try {
      this.receiveBufferLength = Integer.valueOf(rcvBufferString);
      if (logger.isInfoEnabled()) {
        logger.info(getClass().getSimpleName() + " is using receive buffer size <" + this.receiveBufferLength + ">");
      }
    } catch (NumberFormatException e) {
      this.receiveBufferLength = DEFAULT_BUFFER_SIZE;
      if (logger.isInfoEnabled()) {
        logger.info(getClass().getSimpleName() + " is using default receive buffer size <" + this.receiveBufferLength
            + ">");
      }
    }

    Collection<DHCPv6Encoding> liste = null;

    LoadConfigv6 anbindung = new LoadConfigv6();


    try {
      anbindung.setUp();
      liste = anbindung.loadDHCPEntries();
    } catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("Failed to read from database", e);
    }

    if (liste.size() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Dataset from DHCPv6 database empty");
      }
    }

    try {
      logger.info("DHCPv6Trigger: Reinitializing DHCPv6 Encoder and Decoder ...");
      this.dec = new DHCPv6ConfigurationDecoder(new ArrayList<DHCPv6Encoding>(liste));
      this.enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));

    } catch (Exception e) {
      logger.info("Problems reinitializing DHCPv6 Encoder and Decoder in DHCPv6 Trigger:", e);
    }
  }


  public StatisticsReportEntry[] getStatisticsReport() {
    StatisticsReportEntry[] report = new StatisticsReportEntry[7];

    report[0] = new StatisticsReportEntry() {

      public String getValue() {
        return getTriggerInstanceIdentification().getInstanceName();
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".InstanceName";
      }
    };

    report[1] = new StatisticsReportEntry() {

      public String getValue() {
        return "DHCPv6"; // FIXME: does that make any sense ?
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".Protocol";
      }
    };

    report[2] = new StatisticsReportEntry() {

      public Integer getValue() {
        return (int) DHCPv6Trigger.this.getMaxReceivesInParallel();
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".ConfiguredMaxTriggerEvents";
      }
    };

    report[3] = new StatisticsReportEntry() {

      public Long getValue() {
        return DHCPv6Trigger.this.getCntCurrentActiveEventsReadOnly();
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".CurrentTriggerEvents";
      }
    };

    report[4] = new StatisticsReportEntry() {

      public Long getValue() {
        return receivedCounter.get();
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".PacketsReceived";
      }
    };

    report[5] = new StatisticsReportEntry() {

      public Long getValue() {
        return rejectCounter.get();
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".PacketsRejected";
      }
    };

    report[6] = new StatisticsReportEntry() {

      public Long getValue() {
        return (receivedCounter.get() - rejectCounter.get());
      }


      public String getValuePath() {
        return "XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName() + ".PacketsProcessed";
      }
    };

    return report;
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
  
  
  private enum DHCPTriggerStatisticType implements StatisticsPathPart {
    INSTANCENAME("InstanceName"),
    MAXEVENTS("ConfiguredMaxTriggerEvents"),
    CURRENTEVENTS("CurrentTriggerEvents"),
    RECEIVED("PacketsReceived"),
    REJECTED("PacketsRejected"),
    PROCESSED("PacketsProcessed");
    
    private DHCPTriggerStatisticType(String partname) {
      this.partname = partname;
    }
 
    private final String partname;
    
    public String getPartName() {
      return partname;
    }

    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
    
  }
  
  private StatisticsPath instancePathPath;
  
  private StatisticsPath getInstanceBasePath() {
    if (instancePathPath == null) {
      StatisticsPath path = PredefinedXynaStatisticsPath.DHCPTRIGGER;
      TriggerInstanceIdentification triggerId = getTriggerInstanceIdentification();
      if (triggerId.getRevision() == null || triggerId.getRevision() == -1L) {
        path = path.append("WorkingSet");
      } else {
        try {
          ApplicationName applicationName = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement().getApplicationName(triggerId.getRevision());
          path = path.append("Application-" + applicationName.getName());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Could not find application name for revision " + triggerId.getRevision() + " using WorkingSet");
          path = path.append("WorkingSet");
        }
        
      }
      instancePathPath = path.append(triggerId.getInstanceName());
    } 
    return instancePathPath;
  }
  
 
  

  private void registerStatistics() {
    XynaFactory.getInstance().getFactoryManagement().getXynaStatistics().registerNewStatistic("XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification(), this);
    XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().registerNewStatistic("DHCPv6Trigger", this);
    
    FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    try {
      statistics.registerStatistic(new PullStatistics<String, StringStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.INSTANCENAME)) {
        @Override
        public StringStatisticsValue getValueObject() { return new StringStatisticsValue(getTriggerInstanceIdentification().getInstanceName()); }
        @Override
        public String getDescription() { return "DHCPTrigger instance name"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.MAXEVENTS)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(getReceiveControlAlgorithm().getMaxReceivesInParallel()); }
        @Override
        public String getDescription() { return "Maximum of parallel requests the trigger is configured to handle"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.CURRENTEVENTS)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(getReceiveControlAlgorithm().getCurrentActiveEvents()); }
        @Override
        public String getDescription() { return "Amount of events the trigger is currently handling"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.RECEIVED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(receivedCounter.get()); }
        @Override
        public String getDescription() { return "The amount of received events"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.REJECTED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(rejectCounter.get()); }
        @Override
        public String getDescription() { return "The amount of rejected events"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath().append(DHCPTriggerStatisticType.PROCESSED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(receivedCounter.get() - rejectCounter.get()); }
        @Override
        public String getDescription() { return "The amount of events that were received and not rejected"; }
      });
      
      // register aggregations over applications
      for (DHCPTriggerStatisticType statisticType : DHCPTriggerStatisticType.values()) {
        StatisticsPath ownPath = PredefinedXynaStatisticsPath.DHCPTRIGGER.append(StatisticsPathImpl.simplePathPart("All"))
                                                                         .append(getTriggerInstanceIdentification().getInstanceName())
                                                                         .append(statisticType);
        StatisticsPath pathToAggregate = PredefinedXynaStatisticsPath.DHCPTRIGGER
          .append(new StatisticsPathImpl.BlackListFilter("All"))
          .append(getTriggerInstanceIdentification().getInstanceName())
          .append(statisticType);
        Statistics aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
      throw new RuntimeException("", e);
    }
  }
  
  private void unregisterStatistics() {
    XynaFactory.getInstance().getFactoryManagement().getXynaStatistics().unregisterStatistics("XACT.XTrig.Impls.DHCP." + getTriggerInstanceIdentification().getInstanceName());
    XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().unregisterStatistics("DHCPv6Trigger");
    FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    try {
      statistics.unregisterStatistic(getInstanceBasePath().append(StatisticsPathImpl.ALL));
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("Invalid path supplied when trying to unregister statistics",e);
    }
  }
  
  
}
