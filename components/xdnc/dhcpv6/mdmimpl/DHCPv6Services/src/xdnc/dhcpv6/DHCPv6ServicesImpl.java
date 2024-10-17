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
package xdnc.dhcpv6;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import xdnc.dhcp.ConfigFileGeneratorFlag;
import xdnc.dhcp.DNSFlag;
import xdnc.dhcp.IPPool;
import xdnc.dhcp.LeaseTime;
import xdnc.dhcp.NoIpAssigned;
import xdnc.dhcp.Node;
import xdnc.dhcp.PoolType;
import xdnc.dhcp.ReplyStatus;
import xdnc.dhcp.ReservedHost;
import xdnc.dhcp.SubOptionsConfigured;
import xdnc.dhcp.SubOptionsNotConfigured;
import xdnc.dhcp.Successful;
import xdnc.dhcp.TypeOnlyNode;
import xdnc.dhcp.TypeWithValueNode;
import xdnc.dhcp.hashmaputils.HashMapSerializer;
import xdnc.dhcpv6.cluster.DHCPv6ClusterManagement;
import xdnc.dhcpv6.communication.dhcpadapter.DHCPAdapterSender;
import xdnc.dhcpv6.communications.configfilegenerator.ConfigfileGeneratorSender;
import xdnc.dhcpv6.exceptions.DHCPv6AttributeNotFoundForClassException;
import xdnc.dhcpv6.exceptions.DHCPv6InconsistentDataException;
import xdnc.dhcpv6.exceptions.DHCPv6InvalidDBEntriesException;
import xdnc.dhcpv6.exceptions.DHCPv6InvalidOptionException;
import xdnc.dhcpv6.exceptions.DHCPv6MultipleMacAddressesForIPException;
import xdnc.dhcpv6.exceptions.DHCPv6NoOutputOptionsSetException;
import xdnc.dhcpv6.exceptions.DHCPv6NoPoolTypeForClassException;
import xdnc.dhcpv6.exceptions.DHCPv6NoUniqueDppFixedAttributeException;
import xdnc.dhcpv6.exceptions.DHCPv6PooltypeException;
import xdnc.dhcpv6.exceptions.DHCPv6SpecificPropertyNotSetException;
import xdnc.dhcpv6.exceptions.DHCPv6_InvalidMessageTypeException;
import xdnc.dhcpv6.parsing.DHCPPoolsParser;
import xdnc.dhcpv6.parsing.DHCPPoolsParser.DHCPPoolsFormatException;
import xdnc.dhcpv6.tlvdatabase.RestoreOptionsAdm;
import xdnc.dhcpv6.update.DHCPPoolsUpdater;
import xdnc.dhcpv6.utils.BooleanFlag;
import xdnc.dhcpv6.utils.ExceptionCounter;
import xdnc.dhcpv6.utils.IPv4AddressUtil;
import xdnc.dhcpv6.utils.SubnetConfig;
import xdnc.dhcpv6.v6constants.DHCPv6Constants;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna._3._0.XMDM.IPv6;
import com.gip.xyna._3._0.XMDM.MAC;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.tlvdecoding.util.ByteUtil;
import com.gip.xyna.xact.tlvdecoding.util.OctetStringUtil;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6ConfigurationEncoder;
import com.gip.xyna.xact.tlvencoding.dhcpv6.DHCPv6Encoding;
import com.gip.xyna.xact.triggerv6.tlvencoding.databasev6.LoadConfigv6;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xdnc.dhcp.DHCPClusterState;
import com.gip.xyna.xdnc.dhcp.DHCPClusterStateChangeHandler;
import com.gip.xyna.xdnc.dhcpv6.db.storables.Conditional;
import com.gip.xyna.xdnc.dhcpv6.db.storables.DeviceClass;
import com.gip.xyna.xdnc.dhcpv6.db.storables.DppFixedAttribute;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiAttribute;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiAttributeOptionValuerangePair;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiFixedAttribute;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiFixedAttributeOptionValuePair;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiOperator;
import com.gip.xyna.xdnc.dhcpv6.db.storables.GuiParameter;
import com.gip.xyna.xdnc.dhcpv6.db.storables.Host;
import com.gip.xyna.xdnc.dhcpv6.db.storables.Lease;
import com.gip.xyna.xdnc.dhcpv6.db.storables.PoolOption;
import com.gip.xyna.xdnc.dhcpv6.db.storables.SuperPool;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6AddressUtil;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6SubnetUtil;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import com.gip.xyna.xmcp.xfcli.generated.OverallInformationProvider;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;

public class DHCPv6ServicesImpl implements DeploymentTask, IPropertyChangeListener, StatisticsReporterLegacy {

  private static final String TO_BE_SET_WITH_DB_VALUE = "TO BE SET WITH DB VALUE";

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPv6ServicesImpl.class);

  private static final String PROPERTY_POOLSCONFIGFILE = "xdnc.dhcpv6.pools.configfile";
  public static final String DEFAULTRESERVATIONTIME = "xdnc.dhcpv6.config.defaultReservationTime";
  public static final String PARTNERBINDING = "xdnc.dhcpv6.config.partnerbinding";
  public static final String MYNAME = "xdnc.dhcpv6.config.myname";
  public static final String MYETH0 = "xdnc.dhcpv6.config.my_eth0";
  public static final String PARTNERSETH1 = "xdnc.dhcpv6.config.partners_eth1";
  public static final String DOMAINNAME = "xdnc.dhcpv6.config.domainname";
  public static final String MYSTATE = "xdnc.dhcpv6.config.myState";
  public static final String MCLT = "xdnc.dhcp.config.MCLTinSeconds";
  public static final String ENABLEMCLT = "xdnc.dhcp.config.enableMCLT";
  public static final String DISJOINEDTIMEINSECONDS = "xdnc.dhcp.config.disjoinedtimeinseconds";
  
  public static final String RESERVATIONPREFERREDLIFETIME = "xdnc.dhcpv6.config.reservationpreferredlifetime";
  //public static final String HOSTMACTOLEASESTABLE = "xdnc.dhcpv6.config.macToBeWrittenFromHostToLeasestable";
  public static final String LEASETIMEFORSTATICIPS = "xdnc.dhcpv6.config.leasetimeforstaticips";
  public static final String RELOADOPTIONSV6 = "xdnc.dhcpv6.config.reloadoptionsv6";
  public static final String LIMITSMALLLARGEPOOLS = "xdnc.dhcpv6.config.limitsmalllargepools";
  public static final String EXPIREOFFSET = "xdnc.dhcpv6.config.expireoffsetinms";
  
  public static final String BINDINGTOCLEANUP = "xdnc.dhcpv6.config.bindingToCleanup";
  public static final String BINDINGTOSELECTFROM = "xdnc.dhcpv6.config.bindingToSelectFrom";
  public static final String BINDINGTOBESET = "xdnc.dhcpv6.config.bindingToBeSet";
  public static final String BINDINGTOBESETFORSTATICIPS = "xdnc.dhcpv6.config.bindingToBeSetForStaticIPs";

  public static final String MINFREERATIO = "xdnc.dhcpv6.config.minfreeratio";
  public static final String MINFREELEASES = "xdnc.dhcpv6.config.minfreeleases";
  public static final String MINDIFFLEASES = "xdnc.dhcpv6.config.mindiffleases";
  public static final String LEASESLOWWATERMARK = "xdnc.dhcpv6.config.leaseslowwatermark";

  
//weitere Xyna Properties, die beim Wechsel des Cluster-Zustands angepasst werden sollen
  private static String XYNAPROPERTY_LOCKFILTER = "xact.dhcpv6.lockfilter";
  private static String XYNAPROPERTY_HASHV6 = "xact.dhcpv6.hashv6";
  private static String XYNAPROPERTY_CLUSTERMODE = "xdnc.dhcpv6.config.clustermode";
  private static String XYNAPROPERTY_SERVERIDENTIFIER = "xact.dhcpv6.serveridentifier";
  private static String XYNAPROPERTY_HASHV6PASSVAL = "xact.dhcpv6.hashv6passval";
  private static String XYNAPROPERTY_ISPRIMARYSERVER = "xdnc.dhcpv6.isPrimaryServer";
  private static final String XYNAPROPERTY_DOPOOLDEPLOYMENT = "xdnc.dhcp.config.doPoolDeploymentFor";
  private static final String DEPLOYMENT_WORKFLOW = "xdnc.dhcpv6.DeploymentWorkflow";
  private static final String DATAUPDATE_WORKFLOW = "xdnc.dhcpv6.UpdateDataFromGui";
  
  private static final String CAPACITY = "LeaseAssignment_v6Capacity";
  
  public static final String[] DHCPV6_MUST_OPTIONS = {"3","4","5","25","26"};
  
  public static int limitsmalllargepools;
  public static long defaultReservationTime;// = 60000; // in milliseconds
  /**
   * in sekunden
   */
  public static long mclt = 3600;//mclt wird in Sekunden angegeben
  public static long mcltOffsetInSeconds = 60;
  public static boolean enablemclt = false;
  public static long disjoinedtimeinseconds=0;
  public static String partnerbinding;
  public static String bindingToCleanup;
  public static String bindingToSelectFrom;
  public static String bindingToBeSet;
  public static String bindingToBeSetForStaticIPs;
  public static String myname;
  public static String myeth0;
  public static String partnerseth1;
  public static String domainname;
  public static String mystate;
  volatile public static long stateTransitionDate;// Zeit in Millisekunden, zu dem
  // mystate zum letzten Mal geaendert
  // wurde
  public static String myoldstate;
  private static final long toMilliSec = 1000;
  
 
  public static int reservationpreferredlifetime;
  public static long leasetimeforstaticips;
  public static long reloadtime = 0;
  
  public static double minFreeRatio = 0.025;
  public static int minFreeLeases = 50;
  public static int minDiffLeases = 10;
  public static int leasesLowWatermark = 10;
  
  static long expireoffset = 5000;
  //public static String hostmactoleasestable;
  
  // hier kann die zur Berechnung der DPPGUID benoetigte MAC threadlokal gespeichert werden
  private static ThreadLocal<String> macForDPPGUID = new ThreadLocal<String>();
  private static final Pattern DPPGUID_PATTERN = Pattern.compile("DPPGUID");
  
  public static ThreadLocal<Boolean> activateCapacity = new ThreadLocal<Boolean>();
  
  private static Object hostTableLock = new Object();
  //Threadlokales Speichern der Enterprise-Nummer von VendorClass-Option (z.B. 4491 von VendorClass4491)
//  private static ThreadLocal<String> vendorClassWithEnterpriseNumber = new ThreadLocal<String>();
//  private static ThreadLocal<String> vendorSpecInfoWithEnterpriseNumber = new ThreadLocal<String>();
  private static final Pattern VENDORSPECINFO_PATTERN = Pattern
  .compile("^\\s*"+DHCPv6Constants.VENDORSPECINFO+"\\s*$");
  private static final Pattern VENDORCLASS_PATTERN = Pattern
  .compile("^\\s*"+DHCPv6Constants.VENDORCLASS+"\\s*$");
  
  public static final Pattern IPv6ADDRESS_PATTERN = Pattern
  .compile("^([\\-a-fA-F_0-9]{4}+:){7}+[\\-a-fA-F_0-9]{4}+(.*)\\s*$");

  private static SecureRandom rnd = new SecureRandom();
  // private static ThreadLocal<Long> starttime_dynamicleaseallocation = new ThreadLocal<Long>();
  // private static AtomicLong timesum_dynamicleaseallocation = new AtomicLong();
  // private static AtomicLong timecount_dynamicleaseallocation = new AtomicLong();

  // private static List<String> nonAsciiOptions = new ArrayList<String>();


  public static PreparedQueryCache queryCache = new PreparedQueryCache();
//  public static Map<String, OrderedVirtualObjectPool<IP, Lease>> poollist = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>();//REFACTOR_VIRTUALPOOL
  public static final String sqlGetPoolOptions = "SELECT " + PoolOption.COL_OPTIONGUIIDCOLLECTION + "," + PoolOption.COL_VALUECOLLECTION + " FROM " + PoolOption.TABLENAME + " WHERE " + PoolOption.COL_POOLID + " = ?";
  public static final String sqlGetReservedHostForMac = "SELECT * FROM " + Host.TABLENAME + " WHERE " + Host.COL_MAC + " = '?' ";
  public static final String sqlGetReservedHostWithIpForMac = "SELECT * FROM " + Host.TABLENAME + " WHERE " + Host.COL_MAC + " = '?' AND " + Host.COL_IP + " != '?'";
  public static final String sqlGetAllReservedHostsWithIp = "SELECT * FROM " + Host.TABLENAME + " WHERE " + Host.COL_IP + " != ?";
  public static final String sqlGetReservedHostForMacAndIp = "SELECT * FROM " + Host.TABLENAME + " WHERE " + Host.COL_MAC + " = ? AND " + Host.COL_IP + " = ?";
  public static final String sqlGetAllReservedHosts = "SELECT * FROM " + Host.TABLENAME+ " for update";

  public static final String sqlGetLeasestableCount = "SELECT count(*) FROM " + Lease.TABLENAME;
  public static final String sqlLoadGuiSuperpools = "SELECT * FROM " + SuperPool.TABLENAME;


  

  public static final String VendorSpecificInformationRemoteIDString = DHCPv6Constants.VENDORSPECINFO + "." + DHCPv6Constants.SUB171026;

  public static Map<String, String> codeToOptionMap = new HashMap<String, String>();

  static ODS ods;

  private static DHCPv6ConfigurationEncoder enc;

  private static DHCPAdapterSender dhcpadaptersender;
  private static ConfigfileGeneratorSender configfilesender;

  private static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private static ReentrantReadWriteLock propertyReadWriteLock = new ReentrantReadWriteLock();

  
  public static Map<Long,List<SubnetConfig>> superpoolToSubnetsMap = new HashMap<Long,List<SubnetConfig>>();
  
//  //damit timeintervalstrategy erst angelegt wird, wenn statistics initialisiert ist
//  static {
//    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
//    fe.execAsync(new FutureExecutionTask(fe.nextId()) {
//
//      @Override
//      public void execute() {
//        timeintervalstrategy = new TimeIntervalStrategy(persistenceInterval, persistencywarning, persistencyerror);
//        dhcpadaptersender = new DHCPAdapterSender();
//        configfilesender = new ConfigfileGeneratorSender();
//        XynaFactory.getInstance().getFactoryManagement().getStatistics().registerNewStatistic("DHCPv6Leases", statisticshelper);
//        
//      }
//      
//      @Override
//      public int[] after() {
//        return new int[]{XynaStatistics.FUTUREEXECUTION_ID};
//      }
//    });
//  }
  static FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
  private static DHCPv6ServicesImpl statisticshelper; 
  private static DHCPv6ServicesImpl thisinstance;
  
  public static DHCPv6ClusterManagement clusterManagement;
  
  protected DHCPv6ServicesImpl() {
    statisticshelper = this;
  }


  public void onDeployment() {

    if (logger.isDebugEnabled()) {
      logger.debug("Ausfuehrung OnDeployment:");
    }
    
 // Registrieren als PropertyChange-Listener
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                    .addPropertyChangeListener(this);
    thisinstance = this;
    
    // TODO do something on deployment, if required
    // this is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input
    // parameter.
    
    
    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(EXPIREOFFSET)!=null)
    {
      expireoffset = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(EXPIREOFFSET));
    }
    else
    {
      logger.error("EXPIREOFFSET not set!");
    }
    
    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LIMITSMALLLARGEPOOLS)!=null)
    {
      limitsmalllargepools = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(LIMITSMALLLARGEPOOLS));
    }
    else
    {
      logger.error("LIMITSMALLLARGEPOOLS not set!");
    }

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINFREELEASES)!=null)
    {
      minFreeLeases = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(MINFREELEASES));
    }
    else
    {
      logger.error(MINFREELEASES+" not set!");
    }

    
    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINDIFFLEASES)!=null)
    {
      minDiffLeases = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(MINDIFFLEASES));
    }
    else
    {
      logger.error(MINDIFFLEASES+" not set!");
    }

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINFREERATIO)!=null)
    {
      minFreeRatio = Double.parseDouble(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(MINFREERATIO));
    }
    else
    {
      logger.error(MINFREERATIO+" not set!");
    }

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASESLOWWATERMARK)!=null)
    {
      leasesLowWatermark = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                              .getProperty(LEASESLOWWATERMARK));
    }
    else
    {
      logger.error(LEASESLOWWATERMARK+" not set!");
    }

    
    bindingToCleanup = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOCLEANUP);
    bindingToBeSet = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOBESET);
    bindingToBeSetForStaticIPs = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOBESETFORSTATICIPS);
    bindingToSelectFrom = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOSELECTFROM);
    
    defaultReservationTime = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                    .getProperty(DEFAULTRESERVATIONTIME));
    partnerbinding = XynaFactory.getPortalInstance().getFactoryManagementPortal()
                    .getProperty(PARTNERBINDING);
    myname = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MYNAME);
    myeth0 = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MYETH0);
    partnerseth1 = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(PARTNERSETH1);
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    domainname = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(DOMAINNAME);
    myoldstate = mystate;
    mystate = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MYSTATE);
    stateTransitionDate = System.currentTimeMillis();
    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MCLT)!=null)
    {
      mclt = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MCLT));
    }
    else
    {
      logger.error("Property "+MCLT+" not set! Using value 3600");
      mclt=3600;
      
    }

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(ENABLEMCLT)!=null&&XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(ENABLEMCLT).equalsIgnoreCase("true"))
    {
      enablemclt = true;
    }
    else
    {
      enablemclt=false;
    }
    
    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(DISJOINEDTIMEINSECONDS)!=null)
    {
      disjoinedtimeinseconds = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(DISJOINEDTIMEINSECONDS));
    }
    else
    {
      logger.error("Property "+DISJOINEDTIMEINSECONDS+" not set! Using value 0");
      disjoinedtimeinseconds=0;
    }
    
    
    dhcpadaptersender = new DHCPAdapterSender();
    configfilesender = new ConfigfileGeneratorSender();

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RESERVATIONPREFERREDLIFETIME)!=null)
    {
      reservationpreferredlifetime = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RESERVATIONPREFERREDLIFETIME));
    }
    else
    {
      throw new RuntimeException("Property "+RESERVATIONPREFERREDLIFETIME+" not set!");
    }

    if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASETIMEFORSTATICIPS)!=null)
    {
      leasetimeforstaticips = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASETIMEFORSTATICIPS));
    }
    else
    {
      throw new RuntimeException("Property "+LEASETIMEFORSTATICIPS+" not set!");
    }

    
    
    try {
      //ods.registerStorable(Lease.class);
      ods.registerStorable(PoolOption.class);

      if (logger.isDebugEnabled()) {
        logger.debug("Register Storables for gui");
      }
      // storables zum Auslesen der Klassen, GuiAttribute, usw.
      ods.registerStorable(DeviceClass.class);
      ods.registerStorable(DppFixedAttribute.class);
      ods.registerStorable(GuiAttribute.class);
      ods.registerStorable(GuiFixedAttribute.class);
      ods.registerStorable(GuiOperator.class);
      ods.registerStorable(GuiParameter.class);
      ods.registerStorable(com.gip.xyna.xdnc.dhcpv6.db.storables.Condition.class);
      ods.registerStorable(com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType.class);
      ods.registerStorable(Host.class);

      ods.registerStorable(DHCPv6Encoding.class);
      
      ods.registerStorable(SuperPool.class);
      ods.registerStorable(Lease.class);

    }
    catch (PersistenceLayerException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Error registering Storables: "+e);
      }
    }

    Collection<DHCPv6Encoding> liste = null;

    LoadConfigv6 anbindung = new LoadConfigv6();

    try {
      anbindung.setUp();
      liste = anbindung.loadDHCPEntries();
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("Failed to read from database");
    }

    if (liste.size() == 0) {
      if (logger.isDebugEnabled())
        logger.debug("Dataset from DHCPv6 database empty");
      throw new IllegalArgumentException("Dataset from DHCPv6 database empty");//optionsv6-Table nicht da/befüllt
    }

    enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));
//    dhcpadaptersender = new DHCPAdapterSender();
//    configfilesender = new ConfigfileGeneratorSender();


    // nonAsciiOptions.add("CL_OPTION_ORO");
    // nonAsciiOptions.add("CL_OPTION_TFTP_SERVERS");
    // nonAsciiOptions.add("CL_OPTION_SYSLOG_SERVERS");
    // nonAsciiOptions.add("CL_OPTION_MODEM_CAPABILITIES");
    // nonAsciiOptions.add("CL_OPTION_DEVICE_ID");

    
    
//    // addStatistics
//    XynaFactory.getInstance().getFactoryManagement().getStatistics()
//        .registerNewStatistic("DHCPv6Leases", this);
    
    clusterManagement = new DHCPv6ClusterManagement();
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement().registerClusterableComponent(clusterManagement);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException(e);
    }
    if (clusterManagement.isClustered()) {
      //immer nachdem die clusterangehörigkeit das erste mal konfiguriert wurde!
      //d.h. nach der erstinstallation + konfiguration muss man einmal neu-starten.
      clusterManagement.getClusterMgmt().setMCLT(mclt*1000);
      clusterManagement.getClusterMgmt().addClusterStateChangeHandler(new DHCPClusterStateChangeHandler() {
        
        private boolean onChangeHandlerIsRunning = false;
        private Object syncHandler = new Object();
      
        public void onChange(DHCPClusterState oldState,
                DHCPClusterState newState) {
          
          synchronized (syncHandler) {
            onChangeHandlerIsRunning = true;
          }
          try {
              logger.info("clusterstate changed to " + newState
                  + ". oldstate = " + oldState);
               
              int nodeNr = nodeNumberToUse();
              
               if (newState == DHCPClusterState.NEVER_CONNECTED) {
              
                openFilterWithoutHash(newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                enableMclt(newState);
                enableCapacity();
                setClustermode("NEVER_CONNECTED", newState);
                
              } else if (newState == DHCPClusterState.CONNECTED) {
                openFilterWithHash(nodeNr, newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                setMcltConfigurations(false, newState);
                enableCapacity();
                setClustermode("CONNECTED", newState);
                
              } else if (newState == DHCPClusterState.DISJOINED_RUNNING) {
                openFilterWithoutHash(newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                enableMclt(newState);
                if (oldState == DHCPClusterState.CONNECTED){
                  resetDisjoinedTime(System.currentTimeMillis()/toMilliSec, newState);
                }
                setClustermode("DISJOINED_RUNNING", newState);
                
              } else if (newState == DHCPClusterState.DISCONNECTED_MASTER) {
                openFilterWithoutHash(newState);
                setBindingToSelectAndBindingToCleanup("1,2", newState);
                setMcltConfigurations(false, newState);
                enableCapacity();
                setClustermode("DISCONNECTED_MASTER", newState);
                
              } else if (newState == DHCPClusterState.SINGLE_RUNNING) {
                openFilterWithoutHash(newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                enableMclt(newState);//eigentlich nicht noetig, da nur aus DISCONNECTED nach SINGLE RUNNING gewechselt werden kann
                setClustermode("SINGLE_RUNNING", newState);
                
              } else if ( (newState==DHCPClusterState.SYNC_PARTNER) && !(oldState == DHCPClusterState.STARTUP)) {
                openFilterWithHash(nodeNr, newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                disableMclt(newState);
                setClustermode("SYNC_PARTNER", newState);
              }
          } finally {
               synchronized (syncHandler) {
                 onChangeHandlerIsRunning = false;
               }
          }
        }//Ende onChange


        private void enableCapacity() {
          //capacity wieder enablen. das passiert auch, wenn sie jemand manuell disabled hatte => dietrich meint, dass soll so sein.
          CapacityManagement capMgmt =
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement();
          try {
            logger.info("enabling capacity " + CAPACITY + " again");
            capMgmt.changeState(CAPACITY, State.ACTIVE);
          } catch (PersistenceLayerException e) {
            logger.error("could not enable capacity " + CAPACITY, e);
          }
        }


            public boolean isReadyForChange(DHCPClusterState newState) {
              
              if (logger.isDebugEnabled()) {
                logger.debug("isReadyForChange-Handler to clusterstate "
                        + newState + ": starting");
              }
              
              boolean onChangeHandlerStillRunning = false;
              synchronized (syncHandler) {
                onChangeHandlerStillRunning = onChangeHandlerIsRunning;
              }
              while (onChangeHandlerStillRunning) {  
                try {
                  if (logger.isDebugEnabled()) {
                    logger.debug("isReadyForChange-Handler to clusterstate "
                        + newState
                        + ": waiting for previous onChange-Handler to finish");
                  }
                  Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                synchronized (syncHandler) {
                  onChangeHandlerStillRunning = onChangeHandlerIsRunning;
                }
              }
              if (logger.isDebugEnabled()) {
                logger.debug("isReadyForChange-Handler to clusterstate "
                    + newState + ": continue");
              }
                
              int nodeNr = nodeNumberToUse();
              
              if (newState == DHCPClusterState.SYNC_SLAVE) {
                //warten, bis keine aufträge mehr im system sind, und
                // dafür sorgen, dass keine neuen mehr ins system kommen.
                // das ist ein workaround für bugz 13654. ist aber evtl auch fachlich sinnvoll?!
                closeFilter(newState);
                disableCapacityAndWaitForRunningOrders(); 
                
               // binding bleibt wie es ist (DS): setBindingToSelect("", newState);
                setClustermode("SYNC_SLAVE", newState);
              } else if (newState == DHCPClusterState.STARTUP) {
                
                closeFilter(newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ " - using 100 as binding to select from");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                boolean doPoolDeployment = isAutomaticPoolDeploymentEnabled();
                if (doPoolDeployment) {
                  doPoolDeploymentAndDataUpdate(newState);
                } else {
                  logger.info("No pool deployment will be performed due to configuration pattern of property " +XYNAPROPERTY_DOPOOLDEPLOYMENT);
                }
                setClustermode("STARTUP", newState);

              }
              else if (newState == DHCPClusterState.SHUTDOWN){
                closeFilter(newState);
              } else if (newState == DHCPClusterState.SYNC_MASTER) {
                openFilterWithHash(nodeNr, newState);
                if (nodeNr == 0){
                  setBindingToSelectAndBindingToCleanup("1", newState);
                } else if (nodeNr == 1){
                  setBindingToSelectAndBindingToCleanup("2", newState);
                } else {
                  logger.error("Wrong configuration - check property " +XYNAPROPERTY_ISPRIMARYSERVER+ ". Using 100 as binding to select from, this will inhibit assignment of new leases!");
                  setBindingToSelectAndBindingToCleanup("100", newState);
                }
                setMcltConfigurations(false, newState);
                setClustermode("SYNC_MASTER", newState);
              } 
              return true;
            }
            
            private boolean isAutomaticPoolDeploymentEnabled(){
              String config = "";
              try {
                config = XynaFactory.getPortalInstance()
                    .getFactoryManagementPortal().getProperty(
                        XYNAPROPERTY_DOPOOLDEPLOYMENT);
              } catch (Exception e) {
                logger.error("Could not read property "
                    + XYNAPROPERTY_DOPOOLDEPLOYMENT
                    + " - no pool deployment will be performed", e);
                return false;
              }
              if (config != null && config.contains("DHCPv6")) {
                return true;
              }
              return false;         
            }
            
            private void disableCapacityAndWaitForRunningOrders() {
             
              CapacityManagement capMgmt = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement();
              if (capMgmt.getCapacityInformation(CAPACITY).getState() == State.DISABLED) {
                return;
              }
              try {
                logger.info("disabling capacity " + CAPACITY);
                capMgmt.changeState(CAPACITY, State.DISABLED);
              } catch (PersistenceLayerException e) {
                logger.warn("could not disable capacity. waiting 15secs for running orders." , e);
                try {
                  Thread.sleep(15000);
                } catch (InterruptedException e1) {
                } //sicherheitshalber lange warten. filter ist ja bereits gesperrt.
              }
              int inUse = capMgmt.getCapacityInformation(CAPACITY).getInuse();
              int cnt = 0;
              while (inUse > 0) {
                cnt++;
                if (cnt % 20 == 0) {
                  if (logger.isDebugEnabled()) {
                    logger.debug("waiting for " + inUse + " orders that use capacity " + CAPACITY);
                  }
                }
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                inUse = capMgmt.getCapacityInformation(CAPACITY).getInuse();
              }
            }

            private int nodeNumberToUse(){
              int nodeNr = 100;
              try {
                if (XynaFactory.getPortalInstance()
                    .getFactoryManagementPortal().getProperty(
                        XYNAPROPERTY_ISPRIMARYSERVER).equalsIgnoreCase("false")) {
                  nodeNr = 1;
                } else if (XynaFactory.getPortalInstance()
                    .getFactoryManagementPortal().getProperty(
                        XYNAPROPERTY_ISPRIMARYSERVER).equalsIgnoreCase("true")){
                  nodeNr = 0;
                }
              } catch (Exception e) {
                logger
                    .error("Invalid value for Xyna Property "
                        + XYNAPROPERTY_ISPRIMARYSERVER
                        + " - should be true/false!");
              }
              return nodeNr;
            }
            
            
            private void doPoolDeploymentAndDataUpdate(DHCPClusterState newState){
            //Pool-Deployment aufrufen
              if (logger.isDebugEnabled())
                logger.debug("Cluster state change to "+newState+": Performing pool deployment and data update");
              
              XynaOrderCreationParameter xynaOrderPoolDeployment = new XynaOrderCreationParameter(DEPLOYMENT_WORKFLOW, 10);
              XynaOrderCreationParameter xynaOrderUpdateData = new XynaOrderCreationParameter(DATAUPDATE_WORKFLOW, 10);
              try {
                XynaFactory.getPortalInstance().getProcessingPortal().startOrderSynchronously(xynaOrderPoolDeployment);
              } catch (XynaException e) {
                logger.error("Failed to perform pool deployment on cluster state change to new state " +newState,e);
                //TODO
              }
              try {
                XynaFactory.getPortalInstance().getProcessingPortal().startOrderSynchronously(xynaOrderUpdateData);
              } catch (XynaException e) {
                logger.error("Failed to update configuration data on cluster state change to new state " +newState,e);
                //TODO
              }
            }
            
            private void closeFilter(DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Closing filter by setting property " +XYNAPROPERTY_LOCKFILTER+ " = true");
                
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_LOCKFILTER, "true");
              } catch (PersistenceLayerException e) {
                logger.error("Failed to close filter on cluster state change to new state " +newState,e);
              }
            }
            
            private void openFilterWithoutHash(DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Opening filter without using hash by setting property " +XYNAPROPERTY_LOCKFILTER+ " = false and " +XYNAPROPERTY_HASHV6+ " = false");
               
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_LOCKFILTER, "false");
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_HASHV6, "false");
              } catch (PersistenceLayerException e) {
                logger.error("Failed to open filter (without hash) on cluster state change to new state " +newState,e);
              }
            }
            
            private void openFilterWithHash(int hashpassval, DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Opening filter and using hash by setting property " +XYNAPROPERTY_LOCKFILTER+ " = false, " +XYNAPROPERTY_HASHV6+ " = true, " +XYNAPROPERTY_HASHV6PASSVAL+ " = " +hashpassval);
               
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_LOCKFILTER, "false");
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_HASHV6, "true");
                if (hashpassval == 0 || hashpassval == 1){
                  XynaFactory.getPortalInstance().getFactoryManagementPortal()
                  .setProperty(XYNAPROPERTY_HASHV6PASSVAL, ""+hashpassval);
                }//sollte aufgrund einer Fehlkonfiguration hashpassval=100 sein (wenn nodeNumberToUse 100 heausgibt), so werden die alten Werte beibehalten
                
              } catch (PersistenceLayerException e) {
                logger.error("Failed to open filter (with hash) on cluster state change to new state " +newState,e);
              }
            }
            
            private void setBindingToSelectAndBindingToCleanup(String newValue, DHCPClusterState newState){
                setBindingToCleanup(newValue, newState);
                setBindingToSelect(newValue, newState);
            }

            private void setBindingToSelect(String newValue, DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Setting property " +BINDINGTOSELECTFROM+ " =  " +newValue);

                XynaFactory.getPortalInstance().getFactoryManagementPortal().setProperty(BINDINGTOSELECTFROM, newValue);
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to change binding properties on cluster state change to new state " +newState,e);
              }
            }

            private void setBindingToCleanup(String newValue, DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Setting property " +BINDINGTOCLEANUP+ " =  " +newValue);

                XynaFactory.getPortalInstance().getFactoryManagementPortal().setProperty(BINDINGTOCLEANUP, newValue);
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to change binding properties on cluster state change to new state " +newState,e);
              }
            }



            private void resetDisjoinedTime(long timeToSet, DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Setting property " +DISJOINEDTIMEINSECONDS+ " =  " +timeToSet);
               
                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(DISJOINEDTIMEINSECONDS, ""+timeToSet);
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to reset disjoinedTime to 0 on cluster state change to new state " +newState,e);
              }
            }
            
            private void disableMclt(DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Disabling use of MCLT: " +ENABLEMCLT+ " =  false");

                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(ENABLEMCLT, "false");
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to disable use of MCLT on cluster state change to new state " +newState,e);
              }
            }
            
            private void enableMclt(DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Enabling use of MCLT: " +ENABLEMCLT+ " =  true");

                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(ENABLEMCLT, "true");
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to enable use of MCLT on cluster state change to new state " +newState,e);
              }
            }
            
            private void setMcltConfigurations(boolean enableMCLT, DHCPClusterState newState){
              if (enableMCLT){
                
                try {
                  if (logger.isDebugEnabled())
                    logger.debug("Cluster state change to "+newState+": Setting MCLT configurations: " +ENABLEMCLT+ " =  true, " +DISJOINEDTIMEINSECONDS+ " = " +System.currentTimeMillis()/toMilliSec);

                  XynaFactory.getPortalInstance().getFactoryManagementPortal()
                  .setProperty(ENABLEMCLT, "true");
                  XynaFactory.getPortalInstance().getFactoryManagementPortal()
                  .setProperty(DISJOINEDTIMEINSECONDS, ""+System.currentTimeMillis()/toMilliSec);
                } catch (PersistenceLayerException e) {
                  // TODO
                  logger.error("Failed to enable use of MCLT on cluster state change to new state " +newState,e);
                }
                
              } else {
                try {
                  if (logger.isDebugEnabled())
                    logger.debug("Cluster state change to "+newState+": Setting MCLT configurations: " +ENABLEMCLT+ " =  false, " +DISJOINEDTIMEINSECONDS+ " = 0");

                  XynaFactory.getPortalInstance().getFactoryManagementPortal()
                  .setProperty(ENABLEMCLT, "false");
                  XynaFactory.getPortalInstance().getFactoryManagementPortal()
                  .setProperty(DISJOINEDTIMEINSECONDS, "0");
                } catch (PersistenceLayerException e) {
                  // TODO
                  logger.error("Failed to disable use of MCLT on cluster state change to new state " +newState,e);
                }
              }
              
            }
            
            private void setClustermode(String newMode, DHCPClusterState newState){
              try {
                if (logger.isDebugEnabled())
                  logger.debug("Cluster state change to "+newState+": Setting cluster mode: " +XYNAPROPERTY_CLUSTERMODE+ " =  " +newMode);

                XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .setProperty(XYNAPROPERTY_CLUSTERMODE, newMode);
              } catch (PersistenceLayerException e) {
                // TODO
                logger.error("Failed to set Xyna Property "+XYNAPROPERTY_CLUSTERMODE+" on cluster state change to new state " +newState,e);
              }
            }
            
          });
    }

    List<Class<? extends AXynaCommand>> commands;
    try {
      commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        CLIRegistry.getInstance().registerCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
    
    
  }

  
  public void onUndeployment() {

    // TODO do something on undeployment, if required
    // this is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input
    // parameter.

    //removeStatistics
    fe.execAsync(new FutureExecutionTask(fe.nextId()) {

      @Override
      public void execute() {
        XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().unregisterStatistics("DHCPv6Leases");
        //"this" funktioniert nicht, da es auf neuer Instanz aufgerufen wird, d.h. nicht auf derjenigen die registriert wurde
        // Bug 11306
        // Deregistrieren als PropertyChange-Listener
        if (thisinstance != null) {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
              .removePropertyChangeListener(thisinstance);
        }
      }


      @Override
      public int[] after() {
        return new int[] {XynaStatistics.FUTUREEXECUTION_ID};
      }
    });
  }
  

  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> propertyNames = new ArrayList<String>();
    propertyNames.add(DEFAULTRESERVATIONTIME);
    propertyNames.add(PARTNERBINDING);
    propertyNames.add(MYSTATE);
    propertyNames.add(MCLT);
    propertyNames.add(ENABLEMCLT);
    propertyNames.add(DISJOINEDTIMEINSECONDS);
    propertyNames.add(RESERVATIONPREFERREDLIFETIME);
    //propertyNames.add(HOSTMACTOLEASESTABLE);
    propertyNames.add(LEASETIMEFORSTATICIPS);
    propertyNames.add(RELOADOPTIONSV6);
    propertyNames.add(LIMITSMALLLARGEPOOLS);
    propertyNames.add(EXPIREOFFSET);
    propertyNames.add(BINDINGTOBESET);
    propertyNames.add(BINDINGTOBESETFORSTATICIPS);
    propertyNames.add(BINDINGTOSELECTFROM);
    propertyNames.add(BINDINGTOCLEANUP);
    
    propertyNames.add(MINDIFFLEASES);
    propertyNames.add(MINFREELEASES);
    propertyNames.add(MINFREERATIO);
    propertyNames.add(LEASESLOWWATERMARK);
    return propertyNames;
  }


  public void propertyChanged() {
    propertyReadWriteLock.writeLock().lock();
    try {
      
      
      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(EXPIREOFFSET)!=null)
      {
        expireoffset = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(EXPIREOFFSET));
      }
      else
      {
        logger.error("EXPIREOFFSET not set!");
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LIMITSMALLLARGEPOOLS)!=null)
      {
        limitsmalllargepools = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(LIMITSMALLLARGEPOOLS));
      }
      else
      {
        logger.error("LIMITSMALLLARGEPOOLS not set!");
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINFREELEASES)!=null)
      {
        minFreeLeases = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(MINFREELEASES));
      }
      else
      {
        logger.error(MINFREELEASES+" not set!");
      }

      
      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINDIFFLEASES)!=null)
      {
        minDiffLeases = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(MINDIFFLEASES));
      }
      else
      {
        logger.error(MINDIFFLEASES+" not set!");
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MINFREERATIO)!=null)
      {
        minFreeRatio = Double.parseDouble(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(MINFREERATIO));
      }
      else
      {
        logger.error(MINFREERATIO+" not set!");
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASESLOWWATERMARK)!=null)
      {
        leasesLowWatermark = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                                                .getProperty(LEASESLOWWATERMARK));
      }
      else
      {
        logger.error(LEASESLOWWATERMARK+" not set!");
      }

      
      
      defaultReservationTime = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal()
                      .getProperty(DEFAULTRESERVATIONTIME));
      reservationpreferredlifetime = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RESERVATIONPREFERREDLIFETIME));
      

      

      partnerbinding = XynaFactory.getPortalInstance().getFactoryManagementPortal()
                      .getProperty(PARTNERBINDING);

      bindingToBeSet = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOBESET);
      bindingToBeSetForStaticIPs = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOBESETFORSTATICIPS);
      bindingToSelectFrom = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOSELECTFROM);
      bindingToCleanup = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(BINDINGTOCLEANUP);

      
      myoldstate = mystate;
      mystate = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MYSTATE);
      if (mystate == null || myoldstate == null) {
        logger.warn("mystate = " + mystate + ", myoldstate = " + myoldstate);
      } else if ((mystate.equalsIgnoreCase(DHCPv6Constants.STATE_RUNNING) && !(myoldstate
                      .equalsIgnoreCase(DHCPv6Constants.STATE_RUNNING))) || (myoldstate
                      .equalsIgnoreCase(DHCPv6Constants.STATE_RUNNING) && mystate
                      .equalsIgnoreCase(DHCPv6Constants.STATE_NOPARTNER))) {
        stateTransitionDate = System.currentTimeMillis();
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MCLT)!=null)
      {
        mclt = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MCLT));
      }
      else
      {
        logger.error("Property "+MCLT+" not set! Using value 3600");
        mclt=3600;
        
      }
      
      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(ENABLEMCLT)!=null&&XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(ENABLEMCLT).equalsIgnoreCase("true"))
      {
        enablemclt = true;
      }
      else
      {
        enablemclt=false;
      }
      
      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(DISJOINEDTIMEINSECONDS)!=null)
      {
        disjoinedtimeinseconds = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(DISJOINEDTIMEINSECONDS));
      }
      else
      {
        logger.error("Property "+DISJOINEDTIMEINSECONDS+" not set! Using value 0");
        disjoinedtimeinseconds=0;
      }
      
      //hostmactoleasestable = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(HOSTMACTOLEASESTABLE);

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RESERVATIONPREFERREDLIFETIME)!=null)
      {
        reservationpreferredlifetime = Integer.parseInt(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RESERVATIONPREFERREDLIFETIME));
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASETIMEFORSTATICIPS)!=null)
      {
        leasetimeforstaticips = Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(LEASETIMEFORSTATICIPS));
      }

      if(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RELOADOPTIONSV6)!=null)
      {
        if(reloadtime<Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RELOADOPTIONSV6)))
        {
          reloadtime=Long.parseLong(XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(RELOADOPTIONSV6));
          
          Collection<DHCPv6Encoding> liste = null;

          LoadConfigv6 anbindung = new LoadConfigv6();

          try {
            anbindung.setUp();
            liste = anbindung.loadDHCPEntries();
          }
          catch (Exception e) {
            if (logger.isDebugEnabled())
              logger.debug("Failed to read from database",e);
          }

          if (liste.size() == 0) {
            if (logger.isDebugEnabled())
              logger.debug("Dataset from DHCPv6 database empty");
            //throw new IllegalArgumentException("Dataset from DHCPv6 database empty");//optionsv6-Table nicht da/befüllt
          }

          try
          {
            logger.info("DHCPv6Services: Reinitializing DHCPv6 Encoder reading xynadhcpv6.optionsv6 table ...");
            enc = new DHCPv6ConfigurationEncoder(new ArrayList<DHCPv6Encoding>(liste));
            
          }
          catch(Exception e)
          {
            
              logger.info("Problems reinitializing DHCPv6 encoder in DHCPv6Services:",e);
            
          }
        }
      }
      
      
    }
    finally {
      propertyReadWriteLock.writeLock().unlock();
    }
  }

  public static DHCPv6MessageType determineDHCPv6MessageType(List<? extends Node> inputoptions) throws DHCPv6_InvalidMessageTypeException {
    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
       for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.MSGTYPE)) {
            String msgtype = ((TypeWithValueNode) node).getValue();
            if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_SOLICIT)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Received a solicit message");
              }
              XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_SOLICIT);
              return new DHCPv6Solicit();
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_REQUEST)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Received a request message");
              }
              XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_REQUEST);
              return new DHCPv6Request();
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_RENEW)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Received a renew message");
              }
              XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_RENEW);
              return new DHCPv6Renew();
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_REBIND)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Received a rebind message");
              }
              XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_REBIND);
              return new DHCPv6Rebind();
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_DECLINE)) {
              if (logger.isDebugEnabled()) {
                logger.debug("Received a decline message");
              }
              XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_DECLINE);
              return new DHCPv6Decline();
            }

            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_RELEASE)) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Received a release message");
                }
                XynaProcessing.getOrderContext().setCustom1(DHCPv6Constants.TYPE_RELEASE);
                return new DHCPv6Release();

              

            }
            else {
              //logger.error("Invalid DHCPv6 message type " +msgtype);
              throw new DHCPv6_InvalidMessageTypeException(msgtype);
              //throw new XynaException("Invalid DHCPv6-MessageType");
            }
          }
        }
      }
    }
    //logger.error("DHCPv6 message type couldn't be retrieved from input");
    throw new DHCPv6_InvalidMessageTypeException("none - no RelayMessage given in Input");
  }


  public static com.gip.xyna.xact.tlvencoding.dhcp.Node convertNode(Node n) {
    if (n instanceof TypeWithValueNode) {
      return new com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode(n.getTypeName(),
                                                                                  ((TypeWithValueNode) n).getValue());
    }
    else if (n instanceof TypeOnlyNode) {
      TypeOnlyNode tonode = (TypeOnlyNode) n;
      List<? extends Node> subNodes = tonode.getSubnodes();
      List<com.gip.xyna.xact.tlvencoding.dhcp.Node> convertedSubNodes = new ArrayList<com.gip.xyna.xact.tlvencoding.dhcp.Node>();

      if (subNodes.size() != 0) {
        for (Node z : subNodes) {
          convertedSubNodes.add(convertNode(z));
        }
      }
      return new com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode(n.getTypeName(), convertedSubNodes);
    }
    return null;
  }


  public static String encodeDHCPOptionsAsHexString(List<? extends Node> options) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    List<com.gip.xyna.xact.tlvencoding.dhcp.Node> convertedNodes = new ArrayList<com.gip.xyna.xact.tlvencoding.dhcp.Node>();
    List<com.gip.xyna.xact.tlvencoding.dhcp.Node> resultlist = new ArrayList<com.gip.xyna.xact.tlvencoding.dhcp.Node>();
    for (Node n : options) {

      convertedNodes.add(convertNode(n));
    }

    for(com.gip.xyna.xact.tlvencoding.dhcp.Node n : convertedNodes)
    {
      if (n.getTypeName().equals("RelayMessage")) {
        List<com.gip.xyna.xact.tlvencoding.dhcp.Node> subnodes = ((com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode) n).getSubNodes();
        List<com.gip.xyna.xact.tlvencoding.dhcp.Node> newnodes = new ArrayList<com.gip.xyna.xact.tlvencoding.dhcp.Node>();

        for (com.gip.xyna.xact.tlvencoding.dhcp.Node tn : subnodes) {
           newnodes.add(tn);
        }

        newnodes.add(1, new com.gip.xyna.xact.tlvencoding.dhcp.TypeWithValueNode("TXID","0x0496ED"));
        
        resultlist.add(new com.gip.xyna.xact.tlvencoding.dhcp.TypeOnlyNode("RelayMessage", newnodes));
      }  
      else
      {
        resultlist.add(n);
      }
    }
    
    try {
      enc.encode(resultlist, output);
    } catch (Exception e) {
      String debugmac = macForDPPGUID.get();
      if(debugmac==null)debugmac="";
      logger.warn("("+debugmac+") Encoding Nodes to Bytestream in DHCPv6ServiceImpl did not work! " + e);
    }

    byte[] tmp = null;
    if (output != null) {
      tmp = output.toByteArray();
    }
    String result;
    if (tmp != null && tmp.length > 0) {
      result = ByteUtil.toHexValue(tmp);
    }
    else {
      result = "";
    }
    return result;
  }


  public static void start_DHCPAdapter(List<? extends Node> requestoptions, List<? extends Node> replyoptions,
                                       ReservedHost reservedhost, LeaseTime leasetime, DNSFlag dnsFlag) {
    if (leasetime == null)
      return; // bei keiner Leasevergabe, nicht DHCP Adapter anstossen

    String debugmac="";
    debugmac = getMACfromOptions(requestoptions); 
    
    
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX start_DHCPAdapter called!");
    int actioncode = -1;
    // InputNodes: falsche Nodes entfernen und ActionCode auslesen
    List<Node> cleaninputoptions = new ArrayList<Node>();
    for (Node n : requestoptions) {
      if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKADDR)) {
        // entfernen
      }
      else if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.ACTIONCODE)) {
        actioncode = Integer.parseInt(((TypeWithValueNode) n).getValue());
      }
      else if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.INTERFACEID)) {
        // entfernen
      }
      else {
        cleaninputoptions.add(n);
      }
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX Actioncode read: " + actioncode);

    String message = "";

    String[] argu = new String[18];

    // Start der Nachricht
    argu[0] = "Start";

    // Laenge
    argu[1] = ""; // Laenge muss spaeter gesetzt werden

    // Request Type
    argu[2] = "Adapter";

    // Client IP
    argu[3] = getClientIpsFromRequest(replyoptions);
    if (argu[3] == null) {
      argu[3] = "";
      return; // keine ClientIP => nicht DHCPAdapter anstossen
    }
    if(argu[3].length()==0)
    {
      argu[3] = getClientIpsFromRequest(requestoptions);
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX IP:" + argu[3]);

    // Client Mac
    argu[4] = debugmac;
    if (argu[4] == null) {
      argu[4] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX Mac:" + argu[4]);

    argu[5] = leasetime.getStarttime();
    argu[6] = leasetime.getEndtime();

    if (argu[5] == null || argu[6] == null)
      return; // Irgendwas bei Lease falsch gelaufen, nicht noetig DHCP Adapter anzustossen

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX starttime/endtime:" + argu[5] + "/" + argu[6]);

    // Link Layer Address
    argu[7] = getLinkAddress(requestoptions);
    if (argu[7] == null) {
      argu[7] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX linkaddress:" + argu[7]);

    // VendorSpecificInformation (17)
    argu[8] = "";
    HashMap map = getVendorSpecificInformationAsHashmap(requestoptions);
    if (map != null) {
      argu[8] = new HashMapSerializer().serialize(map);
    }
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX vendorspecificinformation:" + argu[8]);

    // InterfaceID
    argu[9] = "";
    // if (getInterfaceID(requestoptions) != null) {
    // argu[9] = getInterfaceID(requestoptions).substring(2); // 0x abhacken
    // }


    String remoteid = "";
    remoteid = getRemoteIdFromVendorSpecificInformation(requestoptions);
    remoteid = StringUtils.fastReplace(remoteid, ":", "", -1);
    

    if (remoteid.length() != 0) {
      argu[9] = remoteid.toLowerCase();
    }


    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX interfaceID:" + argu[9]);

    // Vendor Class Option 16
    argu[10] = getVendorClassOption(requestoptions);
    // if (!(argu[10] == null)) {
    // String hex = argu[10];
    // byte[] tmpbytearray = ByteUtil.toByteArray(hex);
    //  
    // String res = "";
    //  
    // for (byte b : tmpbytearray) {
    // res = res + String.valueOf((char) b);
    // }
    // argu[10] = res;
    // } else {
    // argu[10] = "";
    // }

    if (argu[10] == null)
      argu[10] = "";

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX vendorclass:" + argu[10]);

    if (actioncode == 63) // 63 = Request als Relay Forw
    {
      try {
        argu[11] = encodeDHCPOptionsAsHexString(cleaninputoptions);

        argu[12] = encodeDHCPOptionsAsHexString(replyoptions);
      }
      catch (Exception e) {
        logger.warn("("+debugmac+") Received Message or Reply Message could not be encoded to be sent to DHCP Adapter: " + e);
      }
    }
    else {
      argu[11] = "";
      argu[12] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX request as hexstring:" + argu[11]);
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX reply as hexstring:" + argu[12]);

    // DNS Eintrag
    if (domainname != null && dnsFlag.getDoDNS()) {
      if (!argu[4].equals("") && !domainname.equals("")) {
        String mactmp = argu[4]; // Mac holen
        // mactmp=mactmp.replace(":", ""); // Doppelpunkte entfernen
        argu[13] = mactmp + "." + domainname;
      }
      else {
        argu[13] = "";
      }
    }
    else {
      argu[13] = "";
    }
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX dnsflag:" + argu[13]);

    // is reserved
    if (reservedhost != null) {
      List<Integer> reservedlist = reservedhost.getHostExists();

      String reservedresult = "";
      for (int i : reservedlist) {
        reservedresult = reservedresult + i + ",";
      }

      if (reservedresult.length() > 0)
        reservedresult = reservedresult.substring(0, reservedresult.length() - 1);

      argu[14] = reservedresult;

    }
    else {
      argu[14] = "";
    }
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX reserved:" + argu[14]);


    // DPP Instanz
    if (myname != null) {
      argu[15] = myname;
    }
    else {
      argu[15] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX dppinstance:" + argu[15]);

    // Actioncode
    argu[16] = Integer.toString(actioncode);

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX Actioncode:" + argu[16]);

    // Ende der Nachricht
    argu[17] = "eol";

    for (String s : argu) // Nachricht zum ersten mal bauen fuer Ermittlung der
    // Laenge
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    String len = Integer.toString(message.length());
    int tmp = Integer.parseInt(len) + len.length();
    argu[1] = Integer.toString(tmp);

    message = "";
    for (String s : argu) // Nachricht zum zweiten mal mit Laenge bauen
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") XXX final message:" + message);

    // Nachricht senden
    byte[] packetcontent = message.getBytes();
    dhcpadaptersender.sendToDHCPAdapter(packetcontent);

  }


  private static String getRemoteIdFromVendorSpecificInformation(List<? extends Node> inputoptions) {
    TypeWithValueNode target;

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      for (Node vsi : inputoptions) {
        if (vsi.getTypeName().contains(DHCPv6Constants.VENDORSPECINFO)) {
          for (Node node : ((TypeOnlyNode) vsi).getSubnodes()) {
            if (node.getTypeName().contains(DHCPv6Constants.SUB171026)) {
              target = (TypeWithValueNode) node;
              return target.getValue();
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to read VendorSpecificInformation from Request failed! " + e);
    }

    return "";
  }


  public static void start_ConfigfileGenerator(List<? extends Node> requestoptions, List<? extends Node> replyoptions) {

    String debugmac="";
    debugmac = getMACfromOptions(requestoptions); 


    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") start_ConfigfileGenerator called!");

      
      

    // InputNodes: falsche Nodes entfernen

    List<Node> cleaninputoptions = new ArrayList<Node>();
    for (Node n : requestoptions) {
      if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKADDR)) {
        // entfernen
      }
      else if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.ACTIONCODE)) {
        // entfernen
      }
      else if (n.getTypeName().equalsIgnoreCase(DHCPv6Constants.INTERFACEID)) {
        // entfernen
      }
      else {
        cleaninputoptions.add(n);
      }
    }


    String message = "";

    String[] argu = new String[9];

    // Start der Nachricht
    argu[0] = "Start";

    // Laenge
    argu[1] = ""; // Laenge muss spaeter gesetzt werden

    // Request Type
    argu[2] = "Dhcpd";

    // Client IP
    argu[3] = getClientIpsFromRequest(replyoptions);
    if (argu[3] == null) {
      argu[3] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") IP:" + argu[3]);

    // Client Mac
    argu[4] = debugmac;
    if (argu[4] == null) {
      argu[4] = "";
    }

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") Mac:" + argu[4]);

    // Vendor Class Option 16
    argu[5] = getVendorClassOption(requestoptions);
    // if (!(argu[5] == null)) {
    // String hex = argu[5];
    // byte[] tmpbytearray = ByteUtil.toByteArray(hex);
    //
    // String res = "";
    //
    // for (byte b : tmpbytearray) {
    // res = res + String.valueOf((char) b);
    // }
    // argu[5] = res;
    // }

    if (argu[5] == null)
      argu[5] = "";

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") vendorclass:" + argu[5]);


    // VendorSpecificInformation (17)
    argu[6] = "";
    HashMap map = getVendorSpecificInformationAsHashmap(requestoptions);
    if (map != null) {
      argu[6] = new HashMapSerializer().serialize(map);
    }
    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") vendorspecificinformation:" + argu[6]);


    // dppguid

    argu[7] = "";


    argu[7] = generateDppguid(argu[4]) + ".cfg";

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") dppguid:" + argu[7]);

    // Ende der Nachricht
    argu[8] = "eol";

    for (String s : argu) // Nachricht zum ersten mal bauen fuer Ermittlung der
    // Laenge
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    String len = Integer.toString(message.length());
    int tmp = Integer.parseInt(len) + 4;
    argu[1] = Integer.toString(tmp);
    
    while(argu[1].length()<4)
    {
      argu[1] = "0"+argu[1];
    }

    message = "";
    for (String s : argu) // Nachricht zum zweiten mal mit Laenge bauen
    {
      message = message + s + "\t";
    }
    message = message.substring(0, message.length() - 1);
    message = message + "\n";

    if (logger.isDebugEnabled())
      logger.debug("("+debugmac+") final message:" + message);

    // Nachricht senden
    byte[] packetcontent = message.getBytes();
    configfilesender.sendToConfigfileGenerator(packetcontent);

  }

  
  private static boolean checkFilenameSet(List<? extends Node> inputoptions) {

    TypeOnlyNode ton;
    TypeWithValueNode twvn;
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    
    try {
      for (Node relaymsg : inputoptions) {
        if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
          for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
            if (node.getTypeName().contains(DHCPv6Constants.VENDORSPECINFO)) {
              ton = (TypeOnlyNode) node;
              for (Node subnode : ton.getSubnodes()) {
                if (subnode instanceof TypeWithValueNode) {
                  if (subnode.getTypeName().equals(DHCPv6Constants.SUB1733)) {
                    twvn = (TypeWithValueNode) subnode;
                    if (twvn.getValue().length() != 0)
                      return true;
                  }
                }
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to read VendorSpecificInformation from Reply failed! " + e);
    }


    return false;
  }


  public static String generateDppguid(final String macAddress) {
    if (macAddress == null) {
      throw new IllegalArgumentException("Mac address may not be null.");
    }
//    else if (!macAddress.matches("[0-9A-F]{2}(:[0-9A-F]{2}){5}")) {
//      throw new IllegalArgumentException("Invalid mac address: <" + macAddress + ">.");
//    }

    String input = StringUtils.fastReplace(macAddress, ":", "", -1);
    // System.out.println(input);
    long maclong = Long.parseLong(input, 16);

    long res = ((maclong % 1000000000) * 3) ^ 3494692721L;


    long res2 = (1721966L << 32) + res;

    String hexres = Long.toHexString(res2);

    while (hexres.length() < 16) {
      hexres = "0" + hexres;
    }

    StringBuilder sb = new StringBuilder();
    Formatter format = new Formatter(sb);

    format.format("%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c", hexres.charAt(14), hexres.charAt(15), hexres.charAt(8), hexres
                    .charAt(9), hexres.charAt(12), hexres.charAt(13), hexres.charAt(10), hexres.charAt(11), hexres
                    .charAt(6), hexres.charAt(7), hexres.charAt(4), hexres.charAt(5), hexres.charAt(2), hexres
                    .charAt(3), hexres.charAt(0), hexres.charAt(1)); //+1);

    return sb.toString().toUpperCase();
  }



  /**
   * Wandelt einen String in einen HexString um. Diese Umwandlung wird nur durchgeführt, wenn 
   * der umzuwandelnde String nicht bereits mit 0x... beginnt
   */
  public static String convertStringToHexString(String inp) {
    if (HEX_PATTERN_START.matcher(inp).matches()){
      return inp;
    } else {
      String result = "";
      String debugmac = macForDPPGUID.get();
      if (debugmac == null)
        debugmac = "";

      try {
        byte[] tmp = inp.getBytes();
        result = ByteUtil.toHexValue(tmp);
      } catch (Exception e) {
        if (logger.isDebugEnabled())
          logger
              .debug("("
                  + debugmac
                  + ") Something went wrong in Conversion from String to Hexstring!");
        throw new RuntimeException("(" + debugmac
            + ") Could not convert from string to hexstring");
      }

      return result;
    }
  }


  public static String convertHexStringToString(String hexstring) {
    return OctetStringUtil.toString(com.gip.xyna.xact.tlvencoding.util.ByteUtil.toByteArray(hexstring));
  }


  private static String getLinkAddress(List<? extends Node> inputoptions) {

    for (Node node : inputoptions) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKADDR)) {
        String ip = ((TypeWithValueNode) node).getValue();// hat meistens noch
        // nicht
        // ausgeschriebene
        // Form, i.e.
        // 1234:0000:0001:...
        IPv6AddressUtil ipv6util = IPv6AddressUtil.parse(ip);
        return ipv6util.asLongString();
      }
    }
    return null;
  }


  public static LeaseTime getLeaseTimes(LeaseTime lt, xdnc.dhcpv6.Lease lease) {
    if (lease != null) {
      long starttime = lease.getStarttime();
      long endtime = lease.getExpirationtime();

      String endtimes;

      SimpleDateFormat sdfmt = new SimpleDateFormat();
      sdfmt.applyPattern("yyyy/MM/dd HH:mm:ss");

      Date startdate = new java.util.Date(starttime);
      Date enddate = new java.util.Date(endtime);

      if (lt.getEndtime() != null) {
        if (lt.getEndtime().length() > 0) {
          endtimes = lt.getEndtime() + "," + sdfmt.format(enddate);
        }
        else {
          endtimes = sdfmt.format(enddate);
        }
      }
      else {
        endtimes = sdfmt.format(enddate);
      }

      LeaseTime result = new LeaseTime(sdfmt.format(startdate), endtimes);


      return result;
    }
    else {
      return null;
    }
  }


  public static void getLeaseTimesForReservedHost(LeaseTime leaseTime) {

    long starttime = System.currentTimeMillis();
    long endtime = starttime + reservationpreferredlifetime * toMilliSec;

    String endtimes;

    SimpleDateFormat sdfmt = new SimpleDateFormat();
    sdfmt.applyPattern("yyyy/MM/dd HH:mm:ss");

    Date startdate = new java.util.Date(starttime);
    Date enddate = new java.util.Date(endtime);

    if (leaseTime.getEndtime() != null) {
      if (leaseTime.getEndtime().length() > 0) {
        endtimes = leaseTime.getEndtime() + "," + sdfmt.format(enddate);
      }
      else {
        endtimes = sdfmt.format(enddate);
      }
    }
    else {
      endtimes = sdfmt.format(enddate);
    }


    leaseTime.setStarttime(sdfmt.format(startdate));
    //leaseTime.setEndtime(sdfmt.format(enddate));
    leaseTime.setEndtime(endtimes);

  }


  /**
   * Verarbeitung eines LeaseQueries 
   */

  public static List<? extends Node> processLeaseQuery(List<? extends Node> inputoptions) throws DHCPv6InconsistentDataException, DHCPv6InvalidOptionException, DHCPv6MultipleMacAddressesForIPException, XynaException {

    String debugmac = getMACfromLeaseQuery(inputoptions);
    String queriedip = "";
    String queriedmac = "";
    
    if(debugmac==null)debugmac="";

    String querytypestring = getLeaseQueryType(inputoptions);

    if (querytypestring == null) // Keine LeaseQueryOption gefunden
    {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") LeaseQuery did not contain readable LeaseQuery Option.");
      }
      //throw new DHCPv6InvalidOptionException(DHCPv6Constants.LEASEQUERYTYPE, querytypestring);
      //throw new RuntimeException("LeaseQuery with no readable LeaseQueryOption!");
    }


    int querytype = -1;
    try {
      querytype = Integer.parseInt(querytypestring);
    }
    catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") LeaseQueryType " + querytypestring + " invalid!");
      }
      //throw new DHCPv6InvalidOptionException(DHCPv6Constants.LEASEQUERYTYPE, querytypestring);
      //throw new RuntimeException("LeaseQueryType invalid format!");

    }
    String querylink = getLeaseQueryLinkAddress(inputoptions);
    if (!querylink.equals("0x00000000000000000000000000000000")) {
      querytype = -1; // => unknown Querytype bei angegebener Linkaddresse
    }

    ArrayList<Node> outputoptions = new ArrayList<Node>();

    // Query Typ pruefen und unterscheiden
    if (querytype != 1 && querytype!= 2) {
      TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0007"); // unknown QueryType
      outputoptions.add(status);
    }
    else if(querytype==1){
      TypeOnlyNode iAAdd = new TypeOnlyNode();

      String ipToQueryFor = getIpInQuery(inputoptions, iAAdd);
      
      try {
        ipToQueryFor = IPv6AddressUtil.convertSearchStr2LongSearchStr(ipToQueryFor);
      }
      catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger
                          .debug("Error while trying to convert ipAddress to long search string for database search. (Process LeaseQuery");
        }
      }

      queriedip = ipToQueryFor;
      
      if (iAAdd.getTypeName() != null) {


        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Lease> leasesList = new ArrayList<Lease>();

        ArrayList<Lease> prefixList = new ArrayList<Lease>();

        long currentTimeMillis = System.currentTimeMillis();
        String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_IP + " = ? and expirationTime > ?";
        String prefixsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_PREFIXLENGTH + " < 128 and expirationTime > ?";

        try {
          // nach angefragter Addresse direkt schauen
          // nimm gecachte Anfrage wenn moeglich

          Parameter sqlparameter = new Parameter(ipToQueryFor, currentTimeMillis);
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

          for (Lease l : queryResult) {
            leasesList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length: " + queryResult.size());
          }

          // hier Prefixe aus Datenbank auslesen
          // nimm gecachte Anfrage wenn moeglich

          Parameter sqlparameter2 = new Parameter(currentTimeMillis);
          Collection<? extends Lease> queryResult2 = DHCPv6ODS.queryODS(con, prefixsearch, sqlparameter2, new Lease().getReader(),-1);

          for (Lease l : queryResult2) {
            prefixList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length " + queryResult.size());
          }


        }
        finally {
          con.closeConnection();
        }

        if (leasesList.size() == 0) // keine Mac gefunden
        {
          if (prefixList.size() == 0) // kann auch nicht in Addressbereich liegen
          {
            TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
            outputoptions.add(status);
          }
          else // eventuell noch Hoffnung ...
          {
            IPv6AddressUtil queriedaddress = IPv6AddressUtil.parse(ipToQueryFor);
            IPv6AddressUtil adressindb;
            IPv6AddressUtil subnetindb;
            IPv6AddressUtil subnetqueriedaddress;
            Lease result = null;

            for (Lease le : prefixList) {
              adressindb = IPv6AddressUtil.parse(le.getIp());
              subnetindb = IPv6SubnetUtil.calculateIPv6PrefixAddress(adressindb, le.getPrefixlength());
              subnetqueriedaddress = IPv6SubnetUtil.calculateIPv6PrefixAddress(queriedaddress, le.getPrefixlength());

              if (subnetindb.equals(subnetqueriedaddress)) {
                result = le;
              }
            }

            if (result == null) // auch kein passendes Prefix gefunden
            {
              TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
              outputoptions.add(status);
            }
            else // Adressbereich gefunden
            {
              leasesList.add(result);

            }


          }
        }
        
        if (leasesList.size() > 1) // mehrere Macs gefunden?
        {
          throw new DHCPv6MultipleMacAddressesForIPException("Found multiple macs for one ip address (leaseQueryv6)");
          //throw new XynaException("Found multiple macs for one ip address (leaseQueryv6)");
        }
        if(leasesList.size()==1) {
          ArrayList<Node> subnodes = new ArrayList<Node>();
          ArrayList<Node> subsubnodes = new ArrayList<Node>();
          ArrayList<Node> subsubsubnodes = new ArrayList<Node>();


          Lease currentLease = leasesList.get(0);

          String resulthardwaretype = String.valueOf(currentLease.getHardwareType());
          String resultduidtime = String.valueOf(currentLease.getDUIDTime());

          String resultmac = currentLease.getMac().toUpperCase();
          resultmac = "0x" + StringUtils.fastReplace(resultmac, ":", "", -1);

          resulthardwaretype = "1"; // hardcodiert 1 als Antwort bei LeaseQuery
          
          if(!resultduidtime.equals("-1"))
          {
            subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
            subsubsubnodes.add(new TypeWithValueNode("Time", resultduidtime));
            subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

            subsubnodes.add(new TypeOnlyNode("DUID-LLT", subsubsubnodes));
          }
          else
          {
            subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
            subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

            subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
          }

          subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

          List<Long> transactiontimes = new ArrayList<Long>();

          
          long lastclientransactiontime = ((currentTimeMillis - currentLease.getStartTime()) / 1000L);
          transactiontimes.add(lastclientransactiontime);

          
          // andere Adressen suchen, die unter der Mac vergeben sind
          con = ods.openConnection();
          //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
          con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

          String macToQueryFor = resultmac.substring(2).toLowerCase();
          
          String dbsearchbymac = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_MAC + " = ? and expirationTime > ?";

          Collection<? extends Lease> queryResult = null;
          try {
            Parameter sqlparameter = new Parameter(macToQueryFor, currentTimeMillis);
            queryResult = DHCPv6ODS.queryODS(con, dbsearchbymac, sqlparameter, new Lease().getReader(),-1);
          }
          finally {
            con.closeConnection();
          }
          
          for(Lease res:queryResult)
          {
              if (res.getPrefixlength() == 128) {
                ArrayList<Node> iaaddsubnodes = new ArrayList<Node>();

                iaaddsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));
                iaaddsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                iaaddsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));

                TypeOnlyNode IAAddneu = new TypeOnlyNode(iAAdd.getTypeName(),iaaddsubnodes);
                
                //subnodes.add(iAAdd);
                subnodes.add(IAAddneu);
              }
              else {

                ArrayList<Node> prefixsubnodes = new ArrayList<Node>();


                prefixsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("PrefixLength", Integer.toString(res.getPrefixlength())));
                prefixsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));

                TypeOnlyNode iAPrefneu = new TypeOnlyNode("IAPrefix",prefixsubnodes);
                subnodes.add(iAPrefneu);

              }
              lastclientransactiontime=((currentTimeMillis - res.getStartTime()) / 1000L);
              transactiontimes.add(lastclientransactiontime);

          }
          
          lastclientransactiontime = 0;
          
          for(long l:transactiontimes)
          {
            if(lastclientransactiontime == 0 || l < lastclientransactiontime) lastclientransactiontime = l;
          }

          
          if (lastclientransactiontime < Integer.MAX_VALUE) {
            subnodes.add(new TypeWithValueNode("CLTTime", String.valueOf(lastclientransactiontime)));
          }

          
          outputoptions.add(new TypeOnlyNode("ClientData", subnodes));
        }


      }
      else // keine IA Address
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }
    else if(querytype==2)
    { //Query by Mac
      TypeOnlyNode clientid = new TypeOnlyNode();

      String macToQueryFor = getMacInQuery(inputoptions, clientid);
      
      long currentTimeMillis = System.currentTimeMillis();
      
      queriedmac = macToQueryFor;
      
      if (clientid.getTypeName() != null) {


        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Node> subnodes = new ArrayList<Node>();

        subnodes.add(clientid);


        // andere Adressen suchen, die unter der Mac vergeben sind
        con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

                 
        String dbsearchbymac = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_MAC + " = ? and expirationTime > ?";

        Collection<? extends Lease> queryResult = null;
        try {
          Parameter sqlparameter = new Parameter(macToQueryFor, currentTimeMillis);
          queryResult = DHCPv6ODS.queryODS(con, dbsearchbymac, sqlparameter, new Lease().getReader(),-1);
        }
        finally {
          con.closeConnection();
        }
        
        if (queryResult.size() == 0) // keine Mac gefunden
        {
            TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
            outputoptions.add(status);
        }
        else if(queryResult.size()>0)
        {
          List<Long> transactiontimes = new ArrayList<Long>();
          
          for(Lease res:queryResult)
          {
              if (res.getPrefixlength() == 128) {
                ArrayList<Node> iaaddsubnodes = new ArrayList<Node>();

                iaaddsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));
                iaaddsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                iaaddsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));

                TypeOnlyNode IAAddneu = new TypeOnlyNode(DHCPv6Constants.IAADDR,iaaddsubnodes);
                
                //subnodes.add(iAAdd);
                subnodes.add(IAAddneu);
              }
              else {

                ArrayList<Node> prefixsubnodes = new ArrayList<Node>();


                prefixsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("PrefixLength", Integer.toString(res.getPrefixlength())));
                prefixsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));

                TypeOnlyNode iAPrefneu = new TypeOnlyNode(DHCPv6Constants.IAPREF,prefixsubnodes);
                subnodes.add(iAPrefneu);

              }
              long lastclientransactiontime=((currentTimeMillis - res.getStartTime()) / 1000L);
              transactiontimes.add(lastclientransactiontime);
          }
          
          long lastclientransactiontime = 0;
          
          for(long l:transactiontimes)
          {
            if(lastclientransactiontime == 0 || l < lastclientransactiontime) lastclientransactiontime = l;
          }
          
          if (lastclientransactiontime < Integer.MAX_VALUE && lastclientransactiontime !=0) {
            subnodes.add(new TypeWithValueNode("CLTTime", String.valueOf(lastclientransactiontime)));
          }

          outputoptions.add(new TypeOnlyNode("ClientData", subnodes));
          
        }
      }
      else // keine ClientID
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }

    
    if (logger.isInfoEnabled()) {
      if(queriedip.length()>0)
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#").append(queriedip));
      }
      else if(queriedmac.length()>0)
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#").append(queriedmac));
      }
      else
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#"));
      }
      
    }


    return new XynaObjectList<Node>(outputoptions, Node.class);


  }


  private static String getIpInQuery(List<? extends Node> inputoptions, TypeOnlyNode IAAdd) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    
    try {
      for (Node node : inputoptions) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYOPTION)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAADDR)) {
              IAAdd.setTypeName(subnode.getTypeName());
              IAAdd.setSubnodes((List<Node>) ((TypeOnlyNode) subnode).getSubnodes());
              for (Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes()) {
                if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
                  return ((TypeWithValueNode) subsubnode).getValue();
                }
              }

            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to get IP in LeaseQuery failed! " + e);
    }

    return null;
  }

  
  
  private static String getMacInQuery(List<? extends Node> inputoptions, TypeOnlyNode clientid) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";


    try {
      for (Node node : inputoptions) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYOPTION)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.CLIENTID)) {
              clientid.setTypeName(subnode.getTypeName());
              clientid.setSubnodes((List<Node>) ((TypeOnlyNode) subnode).getSubnodes());
              for (Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes()) {
                if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDLLT) || subsubnode.getTypeName()
                                .equalsIgnoreCase(DHCPv6Constants.DUIDLL)) {
                  TypeOnlyNode duid = (TypeOnlyNode) subsubnode;
                  for (Node duidsubnode : duid.getSubnodes()) {
                    if (duidsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKLAYERADDR)) {
                      TypeWithValueNode linklayer = (TypeWithValueNode) duidsubnode;
                      return linklayer.getValue().substring(2).toLowerCase();
                    }
                  }
                }
                else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDEN)) {

                  // TODO: wo bekommt man hier die MAC-Adresse des Clients her?

                }
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("(" + debugmac + ") Trying to get Mac in LeaseQuery failed! " + e);
    }

    return null;
  }
  

  private static String getLeaseQueryType(List<? extends Node> inputoptions) {
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      for (Node node : inputoptions) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYOPTION)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYTYPE)) {
              return ((TypeWithValueNode) subnode).getValue();
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to get LeaseQueryType failed! " + e);
    }

    return null;
  }


  private static String getLeaseQueryLinkAddress(List<? extends Node> inputoptions) {
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      for (Node node : inputoptions) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYOPTION)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYLINK)) {
              return ((TypeWithValueNode) subnode).getValue();
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to get LeaseQuery LinkAddress failed! " + e);
    }

    return null;
  }


  /**
   * Verarbeitung einer DECLINE-Nachricht.
   */
  public static XynaObjectList<Node> processDecline(List<? extends Node> inputoptions) throws XynaException {//BUGBUG Unterschied DELINE/RELEASE vorhanden?

    String debugmac="";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputoptions);  
    }

    List<Node> outputoptions = new ArrayList<Node>();
    outputoptions.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE, DHCPv6Constants.MSGTYPE_REPLY));


    Map<String, Node> requestedIANAs = new HashMap<String, Node>();// Liste der
    // IANA-options
    // aus
    // Anfrage
    Map<String, Node> requestedIATAs = new HashMap<String, Node>();// Liste der
    // IATA-options
    // aus
    // Anfrage
    Map<String, Node> requestedIAPDs = new HashMap<String, Node>();// Liste der
    // IAPD-options
    // aus
    // Anfrage


    String declinedaddresses = getClientIpsFromRequest(inputoptions);

    String decadds[] = StringUtils.fastSplit(declinedaddresses, ',', 0);

    // doppelte Elemente entfernen
    List<String> list = Arrays.asList(decadds);
    Set<String> set = new HashSet<String>(list);
    String[] result = new String[set.size()];
    set.toArray(result);

    decadds = result;

    List<String> notfound = new ArrayList<String>();

    List<Node> replymessage = new ArrayList<Node>();


    // Durchsuche Tabelle leasestable nach angegebener IP
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


    ArrayList<Lease> leasesList = new ArrayList<Lease>();


    String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_IP + " = ? for update";

    String clientmac = getMACfromOptions(inputoptions);
    String releasedip = "";

    try {

      for (String s : decadds) {
        if (s.contains("/")) // Prefix abhacken fuer Suche
        {
          s = s.substring(0, s.indexOf("/"));
        }
        String ipToQueryFor = s;
        try {
          ipToQueryFor = IPv6AddressUtil.convertSearchStr2LongSearchStr(ipToQueryFor);
        }
        catch (Exception e) {
          if (logger.isDebugEnabled()) {
            logger
                            .debug("Error while trying to convert ipAddress to long search string for database search. (Process LeaseQuery");
          }
        }

        // nach angefragter Addresse direkt schauen
        // nimm gecachte Anfrage wenn moeglich

        Parameter sqlparameter = new Parameter(ipToQueryFor);
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Query with String: " + dbsearch);
          logger.debug("("+debugmac+") Parameter: " + sqlparameter.get(0));
        }
        Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

        for (Lease l : queryResult) {
          leasesList.add(l);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Query result length: " + queryResult.size());
        }
        if (queryResult.size() == 0) {
          notfound.add(ipToQueryFor);
        }
      }



    
    for (Lease lease : leasesList) {
      if (lease.getMac().equals(clientmac)) // abgelehnte IP war ablehnendem Client zugewiesen
      {
        // Mac in Lease auf ungueltige Mac stellen, damit Lease nicht mehr vergeben wird

        releasedip=lease.getIp();
        lease.setMac(DHCPv6Constants.INVALID_MAC);
        
        con.persistObject(lease);
        con.commit();
        
        outputoptions.add(new TypeWithValueNode("StatusCode", "0x0000"));

      }

    }
    }
    finally {
      con.closeConnection();
    }


    if (leasesList.size() < decadds.length) // Zuweisung einiger Adressen nicht gefunden, stehen in notfound
    {
      ArrayList<Integer> counterlist = getAddressAndPrefixRequests(inputoptions, requestedIANAs, requestedIATAs,
                                                                   requestedIAPDs);

      Collection<Node> iananodescoll = requestedIANAs.values();
      Collection<Node> iatanodescoll = requestedIATAs.values();
      Collection<Node> iapdnodescoll = requestedIAPDs.values();

      List<Node> iananodes = new ArrayList<Node>(iananodescoll);
      List<Node> iatanodes = new ArrayList<Node>(iatanodescoll);
      List<Node> iapdnodes = new ArrayList<Node>(iapdnodescoll);

      for (String nf : notfound) {

        for (Node n : iananodes) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n).getSubnodes();

          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAADDR)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(0);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiana = new TypeOnlyNode(DHCPv6Constants.IANA, newsubnodes);
                outputoptions.add(resultiana);
              }
            }
          }
        }

        for (Node n : iatanodes) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n).getSubnodes();
          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAADDR)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(0);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiata = new TypeOnlyNode(DHCPv6Constants.IATA, newsubnodes);
                outputoptions.add(resultiata);
              }
            }
          }
        }

        for (Node n : iapdnodes) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n).getSubnodes();
          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAPREF)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(3);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiata = new TypeOnlyNode(DHCPv6Constants.IAPD, newsubnodes);
                outputoptions.add(resultiata);
              }
            }
          }
        }


      }

    }

    String linkAddress = getLinkAddress(inputoptions);
    
    if (logger.isInfoEnabled()) {
      logger.info(new StringBuilder().append("DC#").append(clientmac).append("#via ").append(linkAddress)
          .append("#RP#").append(releasedip));
    }

    
    replymessage.add(new TypeOnlyNode(DHCPv6Constants.RELAYMESSAGE, outputoptions));
    return new XynaObjectList<Node>(replymessage, Node.class);

  }

  /**
   * Verarbeitung einer Release-Nachricht.
   */
  public static Container processRelease(List<? extends Node> inputoptions) throws XynaException {

    String debugmac="";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputoptions);  
    }

    String cmtsrelayid = getCMTSRelayID(inputoptions);
    String cmtsremoteid = getCMTSRemoteID(inputoptions);


    List<Node> outputoptions = new ArrayList<Node>();
    outputoptions.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE, DHCPv6Constants.MSGTYPE_REPLY));

    LeaseTime leasetime= new LeaseTime();//return parameter

    String releasedIP="";
    
    // Liste der IANA-options aus Anfrage
    Map<String, Node> requestedIANAs = new HashMap<String, Node>();
    // Liste der IATA-options aus Anfrage
    Map<String, Node> requestedIATAs = new HashMap<String, Node>();
    // Liste der IAPD-options aus Anfrage
    Map<String, Node> requestedIAPDs = new HashMap<String, Node>();

    getAddressAndPrefixRequests(inputoptions, requestedIANAs, requestedIATAs,
                                requestedIAPDs);

    List<String> notfound = new ArrayList<String>();

    for (Entry<String, Node> entry:requestedIANAs.entrySet())
    {
      
      String remoteId = getInputOption(VendorSpecificInformationRemoteIDString, inputoptions);
      if (remoteId != null){
        remoteId = StringUtils.fastReplace(remoteId, ":", "", -1).toLowerCase();
      }

      String ipFromRenew = getIpFromIAnode(entry.getValue());
      long currentSystemTime = System.currentTimeMillis();
      String mac = getMACfromOptions(inputoptions);
      
      xdnc.dhcpv6.Lease lease = createLeaseRenew(mac, currentSystemTime/1000L, ipFromRenew, null, remoteId, entry,currentSystemTime/1000L ,currentSystemTime/1000L,0,"",cmtsremoteid,cmtsrelayid);
      lease.setExpirationtime(currentSystemTime);
      lease = getAndSetLeaseByMacAndIAIDAndIPAndPrefixNoSuperPool(lease);//renewLeaseFromPool(pool, condition, action, binding);//TODO_VIRTUALPOOL
      

      if(lease == null)
      {
        notfound.add(ipFromRenew);
      }
      else
      {
        releasedIP = ipFromRenew;
      }
    }

    
    for (Entry<String, Node> entry:requestedIATAs.entrySet())
    {
      
      String remoteId = getInputOption(VendorSpecificInformationRemoteIDString, inputoptions);
      if (remoteId != null){
        remoteId = StringUtils.fastReplace(remoteId, ":", "", -1).toLowerCase();
      }
      String ipFromRenew = getIpFromIAnode(entry.getValue());
      long currentSystemTime = System.currentTimeMillis();
      String mac = getMACfromOptions(inputoptions);
      
      xdnc.dhcpv6.Lease lease = createLeaseRenew(mac, currentSystemTime/1000L, ipFromRenew, null, remoteId, entry,currentSystemTime/1000L ,currentSystemTime/1000L,0,"",cmtsremoteid, cmtsrelayid);
      lease.setExpirationtime(currentSystemTime);
      lease = getAndSetLeaseByMacAndIAIDAndIPAndPrefixNoSuperPool(lease);//renewLeaseFromPool(pool, condition, action, binding);//TODO_VIRTUALPOOL

      if(lease == null)
      {
        notfound.add(ipFromRenew);
      }
      else
      {
        releasedIP = ipFromRenew;
      }
    }

    for (Entry<String, Node> entry:requestedIAPDs.entrySet())
    {
      
      String remoteId = getInputOption(VendorSpecificInformationRemoteIDString, inputoptions);
      if (remoteId != null){
        remoteId = StringUtils.fastReplace(remoteId, ":", "", -1).toLowerCase();
      }
      String ipFromRenew = getIpFromIAnode(entry.getValue());
      long currentSystemTime = System.currentTimeMillis();
      String mac = getMACfromOptions(inputoptions);
      
      xdnc.dhcpv6.Lease lease = createLeaseRenew(mac, currentSystemTime/1000L, ipFromRenew, null, remoteId, entry,currentSystemTime/1000L ,currentSystemTime/1000L,0,"", cmtsremoteid, cmtsrelayid);
      lease.setExpirationtime(currentSystemTime);
      lease = getAndSetLeaseByMacAndIAIDAndIPAndPrefixNoSuperPool(lease);//renewLeaseFromPool(pool, condition, action, binding);//TODO_VIRTUALPOOL

      if(lease == null)
      {
        notfound.add(ipFromRenew);
      }
      else
      {
        releasedIP = ipFromRenew;
      }
    }
    
    List<Node> replymessage = new ArrayList<Node>();



    if (notfound.size()>0) // Zuweisung einiger Adressen nicht gefunden, stehen in notfound
    {


      for (String nf : notfound) {

        for (Entry<String, Node> n:requestedIANAs.entrySet()) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n.getValue()).getSubnodes();

          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAADDR)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(0);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiana = new TypeOnlyNode(DHCPv6Constants.IANA, newsubnodes);
                outputoptions.add(resultiana);
              }
            }
          }
        }

        for (Entry<String, Node> n:requestedIATAs.entrySet()) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n.getValue()).getSubnodes();
          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAADDR)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(0);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiata = new TypeOnlyNode(DHCPv6Constants.IATA, newsubnodes);
                outputoptions.add(resultiata);
              }
            }
          }
        }

        for (Entry<String, Node> n:requestedIAPDs.entrySet()) {
          List<Node> subnodes = (List<Node>) ((TypeOnlyNode) n.getValue()).getSubnodes();
          for (Node sn : subnodes) {
            if (sn.getTypeName().equals(DHCPv6Constants.IAPREF)) {
              Node ipv6 = ((TypeOnlyNode) sn).getSubnodes().get(3);
              String parsedIp = ((TypeWithValueNode) ipv6).getValue();
              try {
                parsedIp = IPv6AddressUtil.convertSearchStr2LongSearchStr(parsedIp);
              }
              catch (Exception e) {
                logger.warn("Could not convert ipv6 address to long form in processDecline!");
              }

              if (parsedIp.equals(nf)) {
                List<Node> newsubnodes = new ArrayList<Node>(subnodes);
                newsubnodes.remove(sn);
                newsubnodes.add(new TypeWithValueNode("StatusCode", "0x0003"));
                Node resultiata = new TypeOnlyNode(DHCPv6Constants.IAPD, newsubnodes);
                outputoptions.add(resultiata);
              }
            }
          }
        }


      }

    }
    
    outputoptions.add(new TypeWithValueNode("StatusCode", "0x0000"));
    
    String mac = getMACfromOptions(inputoptions);
    String linkAddress = getLinkAddress(inputoptions);
    writeLogMessage(mac, linkAddress,releasedIP , "", DHCPv6Constants.TYPE_RELEASE, 128);

    replymessage.add(new TypeOnlyNode(DHCPv6Constants.RELAYMESSAGE, outputoptions));
    return new Container(new XynaObjectList<Node>(replymessage, Node.class), leasetime);
//    return new XynaObjectList<Node>(replymessage, Node.class);

  }

  

  private static xdnc.dhcpv6.Lease getLease(List<? extends Node> inputoptions, String linkAddress, String cmtsrelayid,
                                            String cmtsremoteid, String mac, long leaseTime, String typeVendorClass,
                                            String remoteId, Entry<String, Node> entry, long preferredlifetime,
                                            long hardwaretype, long duidtime, String dyndns,
                                            xdnc.dhcpv6.SuperPool superpool) throws XynaException {
    xdnc.dhcpv6.Lease lease = null;
    
    if(superpool!=null)
    {
      xdnc.dhcpv6.Lease leaseRequest = createSolicitLease(superpool, mac, leaseTime, typeVendorClass, remoteId, entry, preferredlifetime, hardwaretype, duidtime, dyndns, linkAddress, cmtsrelayid, cmtsremoteid);

      setVendorSpecificOptions(inputoptions, leaseRequest);
      
      
      lease = getAndSetLeaseByMacAndIAIDAndTime(leaseRequest);
      
      
      if(lease == null || lease.getIp()==null)
      {
        lease = getAndSetLeaseByMacAndIAID(leaseRequest);
      }
      

      if(lease== null || lease.getIp()==null)
      {
        if(superpool.getLeasecount().length()==0 || superpool.getLeasecount().equals(("0")))
        {
          lease = null;
        }
        else
        {
          if(new BigInteger(superpool.getLeasecount()).compareTo(new BigInteger(String.valueOf(limitsmalllargepools)))<=0)
          {
            lease = getAndSetFreeIPSmallPools(leaseRequest);
          }
          else
          {
            lease = getAndSetFreeIPLargePools(leaseRequest,superpool);
          }
        }
      }

    }
    return lease;
  }


  private static void setVendorSpecificOptions(List<? extends Node> inputoptions, xdnc.dhcpv6.Lease leaseRequest) {
    HashMap map = getVendorSpecificInformationAsHashmap(inputoptions);
    if (map != null && leaseRequest!=null) {
      leaseRequest.setVendorspecificinformation(new HashMapSerializer().serialize(map));
    }
  }


  private static ArrayList<xdnc.dhcpv6.SuperPool> getSuperPool(List<xdnc.dhcpv6.SuperPool> superpools, boolean prefix) throws XynaException {
    
    ArrayList<xdnc.dhcpv6.SuperPool> returnlist = new ArrayList<xdnc.dhcpv6.SuperPool>();
    
    for(xdnc.dhcpv6.SuperPool sp:superpools)
    {
      if(sp.getPrefixlength()==128 && prefix==false)
      {
        returnlist.add(sp);
      }
      if(sp.getPrefixlength()<128 && prefix == true)
      {
        returnlist.add(sp);
      }
    }
    if (logger.isDebugEnabled()){
      if (returnlist.size()==0)
      logger.debug("No SuperPool found (Prefix: "+prefix+")!");
    }
    
    return returnlist;
  }


  private static xdnc.dhcpv6.Lease createSolicitLease(xdnc.dhcpv6.SuperPool superpool, String mac, long leaseTime,
                                                      String typeVendorClass, String cmremoteId,
                                                      Entry<String, Node> entry, long preferredlifetime,
                                                      long hardwaretype, long duidtime, String dyndns, String linkaddress, String cmtsrelayid, String cmtsremoteid) {
    xdnc.dhcpv6.Lease leaseRequest = new xdnc.dhcpv6.Lease();
    leaseRequest.setMac(mac);
    leaseRequest.setIaid(entry.getKey());
    leaseRequest.setBinding(""+bindingToBeSet);
    leaseRequest.setSuperpoolid(superpool.getSuperpoolid());
    leaseRequest.setReservationtime(System.currentTimeMillis()+DHCPv6ServicesImpl.defaultReservationTime);
    leaseRequest.setType(typeVendorClass);
    leaseRequest.setCmremoteid(cmremoteId);
    leaseRequest.setVendorspecificinformation(null);
    leaseRequest.setDppinstance(myname);
    leaseRequest.setPreferredlifetime(preferredlifetime);// in Sekunden
    leaseRequest.setValidlifetime(leaseTime);// in Sekunden
    if(enablemclt)
    {
      leaseRequest.setPreferredlifetime(mclt);// in Sekunden
      leaseRequest.setValidlifetime(mclt+mcltOffsetInSeconds);// in Sekunden
    }
    leaseRequest.setHardwaretype(hardwaretype);
    leaseRequest.setDuidtime(duidtime);
    leaseRequest.setDyndnszone(dyndns);
    leaseRequest.setCmtsip(linkaddress);
    leaseRequest.setCmtsremoteid(cmtsremoteid);
    leaseRequest.setCmtsrelayid(cmtsrelayid);
    return leaseRequest;
  }
  
  

  private static void writeLogMessage(String mac, String linkAddress,
      String ip, String pooltype, String msgtype, int prefixlength) {
    
    String forLogInfoRequest;
    String forLogInfoReply = "#RP#";
    boolean includePooltype = true;
    if (Pattern.matches(DHCPv6Constants.TYPE_SOLICIT, msgtype)){
      forLogInfoRequest = "SO#";
      forLogInfoReply = "#AD#";
    } else if (Pattern.matches(DHCPv6Constants.TYPE_REQUEST, msgtype)){
      forLogInfoRequest = "RQ#";
    } else if (Pattern.matches(DHCPv6Constants.TYPE_RENEW, msgtype)){
      forLogInfoRequest = "RN#";
    } else if (Pattern.matches(DHCPv6Constants.TYPE_RELEASE, msgtype)){
      forLogInfoRequest = "RL#";
      includePooltype = false;
    } else {
      throw new RuntimeException(
      "unknown DHCPv6 message type");
    }
     
    StringBuilder sb = new StringBuilder();
    long timestamp = System.currentTimeMillis();
    sb.append("("+timestamp+")");
    sb.append(forLogInfoRequest).append(mac).append("#via ")
        .append(linkAddress).append(forLogInfoReply).append(ip);
    if (prefixlength != DHCPv6Constants.IPv6ADDRESSLENGTH) {
      sb.append("/").append(prefixlength);
    }
    if (includePooltype){
      sb.append("#").append(pooltype);
    }
    
    if (logger.isInfoEnabled()){
      logger.info(sb);
    }
     
  }

  /**
   * Methode zum Verschicken einer NoAddrsAvail-Nachricht.
   * @throws DHCPv6NoOutputOptionsSetException 
   */
  public static List<? extends Node> sendNoAddrsAvail(List<? extends Node> inputoptions,
      DHCPv6PooltypeException dHCPv6PooltypeException, DHCPv6MessageType messagetype)
      throws DHCPv6_InvalidMessageTypeException, DHCPv6NoOutputOptionsSetException {
    ArrayList<Node> outputoptions = new ArrayList<Node>();
    String msgtypeToSet;
    String forLogInfoRequest;
    String forLogInfoReply = "#RP#";
    if (messagetype instanceof DHCPv6Solicit) {
      msgtypeToSet = DHCPv6Constants.MSGTYPE_ADVERTISE;
      forLogInfoRequest = "SO#";
      forLogInfoReply = "#AD#";
    } else if (messagetype instanceof DHCPv6Request) {
      msgtypeToSet = DHCPv6Constants.MSGTYPE_REPLY;
      forLogInfoRequest = "RQ#";
    } else if ((messagetype instanceof DHCPv6Renew)
        || (messagetype instanceof DHCPv6Rebind)) {
      msgtypeToSet = DHCPv6Constants.MSGTYPE_REPLY;
      forLogInfoRequest = "RN#";
    } else if (messagetype instanceof DHCPv6Decline) {
      msgtypeToSet = DHCPv6Constants.MSGTYPE_DECLINE;
      forLogInfoRequest = "DC#";
    } else if (messagetype instanceof DHCPv6Release) {
      msgtypeToSet = DHCPv6Constants.MSGTYPE_REPLY;
      forLogInfoRequest = "RL#";
    } else {
      throw new DHCPv6_InvalidMessageTypeException(
          "unknown DHCPv6 message type");
    }
    
    String pooltype = dHCPv6PooltypeException.getPooltype();
    if (dHCPv6PooltypeException.getPooltype() == null){
      pooltype = "noMatchingClass";
    }
    
    outputoptions.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE,
        msgtypeToSet));
    outputoptions.add(new TypeWithValueNode(DHCPv6Constants.STATUSCODE,
        DHCPv6Constants.STATUS_NOADDRSAVAIL));
    String mac = getMACfromOptions(inputoptions);
    String linkAddress = getLinkAddress(inputoptions);
    long timestamp = System.currentTimeMillis();
    logger.info(new StringBuilder().append("("+timestamp+")").append(forLogInfoRequest).append(mac)
        .append("#via ").append(linkAddress).append(forLogInfoReply).append(
            "NoAddrsAvail#").append(pooltype));
    ArrayList<Node> relaymessageoption = new ArrayList<Node>();
    relaymessageoption.add(new TypeOnlyNode(DHCPv6Constants.RELAYMESSAGE,
        outputoptions));
    if (outputoptions.size() == 0) {
      throw new DHCPv6NoOutputOptionsSetException("No output options were set for MAC " + mac
          + " on link address " + linkAddress);
    }
    return new XynaObjectList<Node>(relaymessageoption, Node.class);
  }


  private static void updateOptionsmap(Map<String, String> optionsmap,
      List<? extends Node> optionsToUpdate) {
    // Einlesen der bereits vorher gesetzten Klassen- und PoolType-Optionen in
    // die optionsmap
    // die Output-Optionen aus dem vorherigen Schritt sind lauter
    // TypeWithValue-Nodes der Form name=IA_NA.T1, value=...
    // (sie sind also noch nicht in der endgueltigen geschachtelten Form aus
    // Nodes mit Subnodes)
    for (Node n : optionsToUpdate) {
      if (logger.isDebugEnabled()) {
        logger.debug("Got option " + n.getTypeName() + " with value "
            + ((TypeWithValueNode) n).getValue() + " from class determination");
      }
      optionsmap.put(n.getTypeName(), ((TypeWithValueNode) n).getValue());
    }

  }


  /**
   * Gibt preferred und valid lease times in Millisekunden zurueck.
   * 
   * @param t2
   * @param t1
   * @throws DHCPv6InvalidOptionException 
   */
  private static long[] getPreferredAndValidLeaseTime(Map<String, String> optionsmap, BooleanFlag useMCLT, String mac,
                                                      String linkAddress, String t1, String t2) throws DHCPv6InvalidOptionException {

    long leaseTime;
    long prefTime;
    long timestamp = System.currentTimeMillis();
    String ianat2 = optionsmap.get(t2);
    if (ianat2 == null) {
      if (logger.isInfoEnabled()) {
        logger.info("("+timestamp+")SO#" + mac + "#via " + linkAddress);
        // debugmessagewritten = true;
      }
      throw new DHCPv6InvalidOptionException(t2,  "NULL");
    }
    if (useMCLT.getFlagSet()) {
      leaseTime = mclt;
      prefTime = (long) (0.5 * leaseTime);
    }
    else {
      leaseTime = Long.parseLong(ianat2) * toMilliSec;

      if (optionsmap.get(t1) == null) {
        prefTime = (long) (0.5 * leaseTime);
      }
      else {
        prefTime = Long.parseLong(optionsmap.get(t1)) * toMilliSec;// prefTime in
        // Millisekunden
      }

    }

    long[] returnvalues = {leaseTime, prefTime};
    return returnvalues;
  }




  // ############
  private static ArrayList<String> addOutputNodesForReservedHost(BooleanFlag msgtypeset,
                                                                 String mac, xdnc.dhcpv6.Lease lease,
                                                                 PoolType pooltype, String linkAddress,
                                                                 ArrayList<Node> outputoptions, Node ianode,
                                                                 LeaseTime leaseTimeMDM,
                                                                 Map<String, String> optionsmap,
                                                                 ArrayList<String> requestedOptions, String msgTypeToSet)
                  throws PersistenceLayerException {

    ArrayList<String> remainingRequestedOptions = new ArrayList<String>();
    
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    long timestamp = System.currentTimeMillis();

    if (logger.isInfoEnabled()) {
      if ((lease.getPrefixlength() != DHCPv6Constants.IPv6ADDRESSLENGTH)) {
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_ADVERTISE))
          logger.info(new StringBuilder().append("("+timestamp+")").append("SO#").append(mac).append(
              "#via ").append(linkAddress).append("#AD#").append(lease.getIp())
              .append("/").append(lease.getPrefixlength()).append("#").append(
                  pooltype.getType()));
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_REPLY))
          logger.info(new StringBuilder().append("("+timestamp+")").append("RQ#").append(mac).append(
              "#via ").append(linkAddress).append("#RP#").append(lease.getIp())
              .append("/").append(lease.getPrefixlength()).append("#").append(
                  pooltype.getType()));
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_RENEW))
        {
          logger.info(new StringBuilder().append("("+timestamp+")").append("RN#").append(mac).append(
                          "#via ").append(linkAddress).append("#RP#").append(lease.getIp())
                          .append("/").append(lease.getPrefixlength()).append("#").append(
                              pooltype.getType()));
          msgTypeToSet = DHCPv6Constants.MSGTYPE_REPLY;
        }

      } else {
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_ADVERTISE))
          logger.info(new StringBuilder().append("("+timestamp+")").append("SO#").append(mac).append(
              "#via ").append(linkAddress).append("#AD#").append(lease.getIp())
              .append("#").append(pooltype.getType()));
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_REPLY))
          logger.info(new StringBuilder().append("("+timestamp+")").append("RQ#").append(mac).append(
              "#via ").append(linkAddress).append("#RP#").append(lease.getIp())
              .append("#").append(pooltype.getType()));
        if (msgTypeToSet.equals(DHCPv6Constants.MSGTYPE_RENEW))
        {
          logger.info(new StringBuilder().append("("+timestamp+")").append("RN#").append(mac).append(
                          "#via ").append(linkAddress).append("#RP#").append(lease.getIp())
                          .append("#").append(pooltype.getType()));
          msgTypeToSet = DHCPv6Constants.MSGTYPE_REPLY;
        }
      }
    }

    if (!msgtypeset.getFlagSet()) {
      outputoptions.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE, msgTypeToSet));
      // msgtypeset = true;
      msgtypeset.setFlagSet(true);
    }
    // IA_... option setzen
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Building IA option");
    }

    Node iaOption = buildIAoptionsForReservedHost(lease.getIp(), ianode, lease.getPrefixlength());
    getLeaseTimesForReservedHost(leaseTimeMDM);// Lease-Start-/End-Time in Format des DHCP-Adapters
    outputoptions.add(iaOption);
    // if (!additionaloptionsset) {
    // setAdditionalOptions(outputoptions, inputoptions, lease, optionsmap);
    // additionaloptionsset = true;
    // }
    remainingRequestedOptions = setRequestedOptions(outputoptions, requestedOptions, lease, optionsmap);
    return remainingRequestedOptions;
  }


  // ############


  /**
   * Liest alle Eintraege zu der angegebenen mac aus der Tabelle "host". Wenn ipForRenew ungleich null, wird neben der
   * MAC auch auf Uebereinstimmung mit IP geprueft.
   */
  private static ArrayList getReservedHostsForMacAndLinkAddress(String mac, String ipForRenew, String linkaddress) throws PersistenceLayerException {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Reading reserved hosts from DB for MAC " + mac+ " and LinkAddress: "+linkaddress);
    }

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    
    ArrayList<Host> reservedAddressList = new ArrayList<Host>();
    ArrayList<Host> reservedPrefixList = new ArrayList<Host>();
    try {

      // nimm gecachte Anfrage wenn moeglich
      String macRaw = mac;// MAC ohne Doppelpunktnotation
      //PreparedQuery<Host> pq;
      Parameter sqlparameter;


      Collection<? extends Host> queryResult;
      
      con.ensurePersistenceLayerConnectivity(Host.class);
      synchronized (hostTableLock) {
        if (ipForRenew == null) {
          queryResult = DHCPv6ODS.queryODS(con, "SELECT * FROM host WHERE "+Host.COL_MAC+" = ? and "+Host.COL_CMTSIP+" like ?", new Parameter(macRaw,"%"+linkaddress+"%"), new Host().getReader()); 
        }
        else
        {
          queryResult = DHCPv6ODS.queryODS(con, "SELECT * FROM host WHERE "+Host.COL_MAC+" = ? and "+Host.COL_IP+" = ? and "+Host.COL_CMTSIP+" like ?", new Parameter(macRaw, ipForRenew,"%"+linkaddress+"%"), new Host().getReader());
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Query result of length " + queryResult.size());
      }
      // sortiere nach IPv6-Adressen und Prefixes
      for (Host h : queryResult) {
        // prefixlength in host-Tabelle: 64
        if (h.getPrefixlength() == null || h.getPrefixlength().equals("") || h.getPrefixlength()
                        .equals(DHCPv6Constants.IPv6ADDRESSLENGTHSTRING)) {
          reservedAddressList.add(h);
        }
        else {
          reservedPrefixList.add(h);
        }
      }
      // sortieren nach aufsteigender IP - Eintraege ohne IP stehen ganz hinten
      Collections.sort(reservedAddressList);
      Collections.sort(reservedPrefixList);

      // } catch (PersistenceLayerException e) {
      // if (logger.isInfoEnabled()){
      // logger.warn("Error while trying to read reserved hosts: " +e);
      // }
      // }
    }
    finally {
      con.closeConnection();
    }

    ArrayList returnvalue = new ArrayList();
    returnvalue.add(reservedAddressList);
    returnvalue.add(reservedPrefixList);
    return returnvalue;

  }


  /**
   * Transformiert eine MAC der Darstellung 00:12:ab:11:00:12 in die Form 0012AB110012
   */
  private static String getMacAsRaw(String mac) {
    String[] macParts = StringUtils.fastSplit(mac, ':', -1);
    if (macParts.length != 6) {
      throw new RuntimeException("Illegal MAC format!");
    }
    StringBuilder sb = new StringBuilder();
    for (String part : macParts) {
      sb.append(part.toUpperCase());
    }
    return sb.toString();
  }

  /**
   * Transformiert eine MAC der Darstellung 0012ab110012 in die Form 00:12:AB:11:00:12
   */
  private static String getMacFromRaw(String mac) {
    char[] macParts = mac.toCharArray();
    if (macParts.length != 12) {
      throw new RuntimeException("Illegal MAC format!");
    }
    StringBuilder sb = new StringBuilder();
    
    sb.append(""+(char)macParts[0]+(char)macParts[1]+":"+(char)macParts[2]+(char)macParts[3]+":"+(char)macParts[4]+(char)macParts[5]+":"+
              (char)macParts[6]+(char)macParts[7]+":"+(char)macParts[8]+(char)macParts[9]+":"+(char)macParts[10]+(char)macParts[11]);
    return sb.toString().toUpperCase();
  }
  

  /**
   * Erstellen einer Antwort-Nachricht so, dass die requestedOptions schon vorher bestimmt wurden
   */
  private static ArrayList<String> setRequestedOptions(ArrayList<Node> outputoptions,
                                                       ArrayList<String> requestedOptions, xdnc.dhcpv6.Lease lease,
                                                       Map<String, String> optionsmap) throws PersistenceLayerException {

    ArrayList<String> remainingRequestedOptions = new ArrayList<String>();

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    // Setzen/Ueberschreiben von etwaigen neuen Optionen, die zu tatsaechlich
    // vergebenem Pool gehoeren
    long poolIdForLease = lease.getSuperpoolid();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Setting pool-specific options");
    }
    getPoolOptions(poolIdForLease, optionsmap);// sortiert die Pool-Options in
    // Map ein
    if ((requestedOptions != null) && (requestedOptions.size() > 0)) {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Determined the following options to be requested:");
        for (String s : requestedOptions) {
          logger.debug("("+debugmac+") option " + s);
        }
      }
      remainingRequestedOptions = addRequestedOptionsToOutputReturnUnsetOptions(requestedOptions, outputoptions,
                                                                                optionsmap);
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") No Requested Options option received");
      }
    }
    return remainingRequestedOptions;
  }




  private static void checkCapacityActivation(String capName) {
    if ((activateCapacity.get() != null) && activateCapacity.get()){
      doActivateCapacity(capName);
      activateCapacity.set(false);
    }
  }


  private static void doActivateCapacity(String capName) {
    if (XynaFactory.getInstance().getXynaMultiChannelPortal()
        .getCapacityInformation(DHCPv6Constants.CAP_LEASEASSIGNMENT).getState() == State.DISABLED) {
      try {
        XynaFactory.getInstance().getXynaMultiChannelPortal()
            .changeCapacityState(capName, State.ACTIVE);
        logger.info("Activating capacity " +DHCPv6Constants.CAP_LEASEASSIGNMENT+" again");
      } catch (PersistenceLayerException e) {
        logger.error("Error while trying to activate capacity " +DHCPv6Constants.CAP_LEASEASSIGNMENT+ ": "+e);
        throw new RuntimeException(e);
      }
    }
  }


  /**
   * Aus den Inputoptionen der Anfrage werden die inkludierten IA_NA-,IA_TA-,IA_PD-Optionen herausgesucht. Diese geben
   * an, nach was fuer Adressen bzw. Prefixes ueberhaupt angefragt wird.
   * 
   * @param inputoptions
   * @param requestedIANAs
   * @param requestedIATAs
   * @param requestedIAPDs
   */
  private static ArrayList<Integer> getAddressAndPrefixRequests(List<? extends Node> inputoptions,
                                                                Map<String, Node> requestedIANAs,
                                                                Map<String, Node> requestedIATAs,
                                                                Map<String, Node> requestedIAPDs) {

    int counterIANA = 0;
    int counterIATA = 0;
    int counterIAPD = 0;

    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node inputnode : ((TypeOnlyNode) relaymsg).getSubnodes()) {
          if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) {
            // Reihenfolge in IANA: IAID, T1, T2, options
            String iana_iaid = ((TypeWithValueNode) ((TypeOnlyNode) inputnode).getSubnodes().get(0)).getValue();
            requestedIANAs.put(iana_iaid, inputnode);
            counterIANA++;
          }
          else if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IATA)) {
            // Reihenfolge in IATA: IAID, options
            String iata_iaid = ((TypeWithValueNode) ((TypeOnlyNode) inputnode).getSubnodes().get(0)).getValue();
            requestedIATAs.put(iata_iaid, inputnode);
            counterIATA++;
          }
          else if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
            // Reihenfolge in IAPD: IAID, T1, T2, options
            String iapd_iaid = ((TypeWithValueNode) ((TypeOnlyNode) inputnode).getSubnodes().get(0)).getValue();
            requestedIAPDs.put(iapd_iaid, inputnode);
            counterIAPD++;
          }
        }
      }
    }

    ArrayList<Integer> counterlist = new ArrayList<Integer>(3);
    counterlist.add(counterIANA);
    counterlist.add(counterIATA);
    counterlist.add(counterIAPD);
    return counterlist;

  }


  /**
   * Baut rekursiv aus der uebergebenen optionsmap einen Node der zu der angefragten Option passt. optionsmap enthaelt
   * Eintraege der Form (IA_NA.T1,value), (VENDORSPECINFO.SUB1.SUB2,value), (DNSServer, Value)
   */
  private static Node generateSpecificNodeFromOptionsmap(String nameStartsWith, Map<String, String> optionsmap,
                                                         Map<String, String> ignoreList) {
    ArrayList<Node> subnodes = new ArrayList<Node>();

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    // Bestimmt, ob Startwert auch schon in Punktnotation ist, z.B. VendorSpecificInformation.CL_OPTION
    String[] startsWithParts = StringUtils.fastSplit(nameStartsWith, '.', -1);
    int lengthStartName = startsWithParts.length;

    // Loop ueber alle Eintraege der optionsmap, verschiedenen Suboptionen einer
    // Hauptoption zu finden und einzutragen, z.B. IA_NA.T1 und IA_NA.T2
    String matchingOption = null;
    
    for (String option : optionsmap.keySet()) {
      if (ignoreList.get(option) == null) {// nur wenn option nicht bereits in ignoreList steht
        // TODO: Pattern fuer performance
        if (option.startsWith(nameStartsWith)) {
          String[] nameParts = StringUtils.fastSplit(option, '.', -1);
          if (nameParts.length - lengthStartName == 0) {
            // Sonderfall DPPGUID
            if (DPPGUID_PATTERN.matcher(optionsmap.get(option)).matches()) {
              StringBuilder dppguid = new StringBuilder(generateDppguid(macForDPPGUID.get())).append(".cfg");

              if (logger.isDebugEnabled()) {
                logger.debug("("+debugmac+") Returning node for option " + option + " with value " + dppguid.toString());
              }
              ignoreList.put(option, option);
              return new TypeWithValueNode(nameParts[lengthStartName - 1], dppguid.toString());
            }
            if (logger.isDebugEnabled()) {
              logger.debug("("+debugmac+") Returning node for option " + option + " with value " + optionsmap.get(option));
            }
        
            ignoreList.put(option, option);
            return new TypeWithValueNode(nameParts[lengthStartName - 1], optionsmap.get(option));
          }
          else {

            Node newNode;
            //Abfrage unnoetig geworden, da mit konkreter VendorSpecInfoEnterpriseNr gearbeitet wird 
//            if (nameStartsWith.equals(DHCPv6Constants.VENDORSPECINFO)) {
//              newNode = generateSpecificNodeFromOptionsmap(new StringBuilder(nameParts[0]).append(".")
//                              .append(nameParts[lengthStartName]).toString(), optionsmap, ignoreList);
//            }
//            else {
//              newNode = generateSpecificNodeFromOptionsmap(new StringBuilder(nameStartsWith).append(".")
//                              .append(nameParts[lengthStartName]).toString(), optionsmap, ignoreList);
//            }
            newNode = generateSpecificNodeFromOptionsmap(new StringBuilder(nameStartsWith).append(".")
                .append(nameParts[lengthStartName]).toString(), optionsmap, ignoreList);

            if (newNode != null) {
              subnodes.add(newNode);
            }
            matchingOption = option;
            
            ignoreList.put(option, option);
          }
        }
        else {
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") No match for option " + option + " - testing with startstring " + nameStartsWith);
          }
        }
      }
    }
    if (subnodes.size() == 0) {// wenn angefragte Option gar nicht vertreten war
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Requested option " + nameStartsWith + " not found in optionsmap");
      }
      return null;
    }
  //Abfrage unnoetig geworden, da mit konkreter VendorSpecInfoEnterpriseNr gearbeitet wird 
//    if (nameStartsWith.equals(DHCPv6Constants.VENDORSPECINFO)) {
//      String[] matchingOptionParts = PUNKT_PATTERN.split(matchingOption);
//      return new TypeOnlyNode(matchingOptionParts[0], subnodes);
//    }
//    else {
//      return new TypeOnlyNode(startsWithParts[lengthStartName - 1], subnodes);
//    }
    return new TypeOnlyNode(startsWithParts[lengthStartName - 1], subnodes);
  }


  /**
   * Stellt die angefragten Optionen aus der optionsmap zusammen und fuegt sie der Liste der Output-Optionen hinzu; gibt
   * eine Liste von angefragten Optionen zurueck, die nicht gesetzt werden konnten
   */
  private static ArrayList<String> addRequestedOptionsToOutputReturnUnsetOptions(ArrayList<String> requestedOptions,
                                                                                 ArrayList<Node> outputoptions,
                                                                                 Map<String, String> optionsmap) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    ArrayList<String> remainingRequestedOptions = new ArrayList<String>();
    Map<String,String> variousEnterpriseNumbers = new HashMap<String, String>();
    
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Adding requested options to output");
    }
    // in requestedOptions stehen die Option-Codes der angeforderten Optionen
    if(requestedOptions!=null)
    for (String optioncode : requestedOptions) {
      String optionname = codeToOptionMap.get(optioncode);
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Got requested option " + optioncode + " - matching " + optionname);
      }
      if (optionname == null) {
        // TODO
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Option code " + optioncode + " not known.");
        }
      }
      else {
        //Sonderbehandlung der Optionen mit Enterprise-Number am Namen
        if (VENDORSPECINFO_PATTERN.matcher(optionname).matches()
            || VENDORCLASS_PATTERN.matcher(optionname).matches()) {

          variousEnterpriseNumbers = getEnterpriseNumbersPresentInOptionsmap(
              optionsmap, optionname);
          
          if (variousEnterpriseNumbers.keySet().size() == 0){
            remainingRequestedOptions.add(optioncode);//option war hier nicht vertreten, kann aber ggf. bei einem anderen Pool gesetzt sein
          }
          
          for (String optionnameWithEnterpriseNumber : variousEnterpriseNumbers
              .keySet()) {
            
            Map<String, String> ignoreList = new HashMap<String, String>();
            Node optiontoset = generateSpecificNodeFromOptionsmap(optionnameWithEnterpriseNumber,
                optionsmap, ignoreList);
            if (optiontoset != null) {
              outputoptions.add(optiontoset);
            } else {
              remainingRequestedOptions.add(optioncode);
            }
          }

        } else {
          Map<String, String> ignoreList = new HashMap<String, String>();
          Node optiontoset = generateSpecificNodeFromOptionsmap(optionname,
              optionsmap, ignoreList);
          if (optiontoset != null) {
            outputoptions.add(optiontoset);
          } else {
            remainingRequestedOptions.add(optioncode);
          }
        }

      }
    }
    return remainingRequestedOptions;

  }


  private static Map<String,String> getEnterpriseNumbersPresentInOptionsmap(
      Map<String, String> optionsmap, String nameWithoutEnterpriseNumber) {
    
    Map<String,String> namesWithEnterpriseNumbers = new HashMap<String, String>();
    for (String option : optionsmap.keySet()){
      if (option.startsWith(nameWithoutEnterpriseNumber)){
        String[] parts = StringUtils.fastSplit(option, '.', -1);
        namesWithEnterpriseNumbers.put(parts[0], parts[0]);
      }
    }
    
    return namesWithEnterpriseNumbers;
  }


  


private static xdnc.dhcpv6.Lease createRequestLease(
      xdnc.dhcpv6.SuperPool superpool, String mac, long leaseTime,
      String typeVendorClass, String cmremoteId, Entry<String, Node> entry,
      long preferredlifetime, long hardwaretype, long duidtime, String dyndns,
      String linkaddress, String cmtsrelayid, String cmtsremoteid) {
    xdnc.dhcpv6.Lease leaseRequest = new xdnc.dhcpv6.Lease();
    leaseRequest.setMac(mac);
    leaseRequest.setIaid(entry.getKey());
    leaseRequest.setBinding("" + bindingToBeSet);
    leaseRequest.setSuperpoolid(superpool.getSuperpoolid());
    leaseRequest.setExpirationtime(System.currentTimeMillis()
        + (leaseTime * toMilliSec));

    leaseRequest.setStarttime(System.currentTimeMillis());
    leaseRequest.setType(typeVendorClass);
    leaseRequest.setCmremoteid(cmremoteId);
    leaseRequest.setVendorspecificinformation(null);
    leaseRequest.setDppinstance(myname);
    leaseRequest.setPreferredlifetime(preferredlifetime);//in Seconds
    leaseRequest.setValidlifetime(leaseTime);//in Seconds
    if (enablemclt) {
      leaseRequest.setPreferredlifetime(mclt);//in Seconds
      leaseRequest.setValidlifetime(mclt + mcltOffsetInSeconds);//in Seconds
      leaseRequest.setExpirationtime(System.currentTimeMillis()
          + ((mclt + mcltOffsetInSeconds) * toMilliSec) );
    }

    leaseRequest.setHardwaretype(hardwaretype);
    leaseRequest.setDuidtime(duidtime);
    leaseRequest.setDyndnszone(dyndns);
    leaseRequest.setCmtsip(linkaddress);
    leaseRequest.setCmtsremoteid(cmtsremoteid);
    leaseRequest.setCmtsrelayid(cmtsrelayid);

    return leaseRequest;
  }



  private static xdnc.dhcpv6.Lease createLeaseRenew(String mac, long leaseTime,
      String ipFromRenew, String typeVendorClass, String cmremoteId,
      Entry<String, Node> entry, long preftime, long validlifetime,
      long superpoolid, String linkaddress, String cmtsremoteid,
      String cmtsrelayid) {
    
    xdnc.dhcpv6.Lease lease = new xdnc.dhcpv6.Lease();
    lease.setMac(mac);
    //lease.setMacAsNum(macAsNum);
    lease.setExpirationtime(System.currentTimeMillis()+leaseTime*toMilliSec);
    //lease.setStartTime(System.currentTimeMillis());
    //lease.setReservationTime(System.currentTimeMillis()+DHCPv6ServicesImpl.defaultReservationTime);
    if (typeVendorClass != null){
      lease.setType(typeVendorClass);
    }
    if (cmremoteId != null){
      lease.setCmremoteid(cmremoteId);
    }
    
    if(cmtsrelayid!=null && cmtsrelayid.length()>0)
    {
      lease.setCmtsrelayid(cmtsrelayid);
    }

    if(cmtsremoteid!=null && cmtsremoteid.length()>0)
    {
      lease.setCmtsremoteid(cmtsremoteid);
    }

    lease.setVendorspecificinformation(null);
    lease.setDppinstance(myname);
    
    lease.setSuperpoolid(superpoolid);
    lease.setStarttime(Long.MIN_VALUE);  // markiert StartTime, so dass sie mit dem Wert aus der Datenbank gesetzt wird
    lease.setPreferredlifetime(preftime);
    lease.setValidlifetime(validlifetime);
    if(enablemclt)
    {
      lease.setPreferredlifetime(mclt);
      lease.setValidlifetime(mclt+mcltOffsetInSeconds);
      lease.setExpirationtime(System.currentTimeMillis()+(mclt+mcltOffsetInSeconds)*toMilliSec);
    }

    lease.setCmtsip(linkaddress);
//    lease.setStarttime(System.currentTimeMillis());
    lease.setIaid(entry.getKey());
    lease.setIp(ipFromRenew);
    lease.setPrefixlength(DHCPv6Constants.IPv6ADDRESSLENGTH);
    lease.setBinding(""+bindingToBeSet);
    //lease.setLeaseid(-1);
    return lease;
  }


  /**
   * extrahiert aus einem IANA-, IATA- oder einem IAPD-Node die IPv6-Adresse
   */
  private static String getIpFromIAnode(Node ia_node) {

    if (ia_node.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) {
      for (Node ianaSubnode : ((TypeOnlyNode) ia_node).getSubnodes()) {
        if (ianaSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAADDR)) {
          for (Node iaaddrSubnode : ((TypeOnlyNode) ianaSubnode).getSubnodes()) {
            if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
              return IPv6AddressUtil.parse(((TypeWithValueNode) iaaddrSubnode).getValue()).asLongString();
              // return ((TypeWithValueNode) iaaddrSubnode).getValue();
            }
          }
        }
      }
    }
    else if (ia_node.getTypeName().equalsIgnoreCase(DHCPv6Constants.IATA)) {
      for (Node iataSubnode : ((TypeOnlyNode) ia_node).getSubnodes()) {
        if (iataSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAADDR)) {
          for (Node iaaddrSubnode : ((TypeOnlyNode) iataSubnode).getSubnodes()) {
            if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
              return IPv6AddressUtil.parse(((TypeWithValueNode) iaaddrSubnode).getValue()).asLongString();
              // return ((TypeWithValueNode) iaaddrSubnode).getValue();
            }
          }
        }
      }
    }
    else if (ia_node.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
      for (Node iapdSubnode : ((TypeOnlyNode) ia_node).getSubnodes()) {
        if (iapdSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPREF)) {
          for (Node iaaddrSubnode : ((TypeOnlyNode) iapdSubnode).getSubnodes()) {
            if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
              return IPv6AddressUtil.parse(((TypeWithValueNode) iaaddrSubnode).getValue()).asLongString();
              // return ((TypeWithValueNode) iaaddrSubnode).getValue();
            }
          }
        }
      }
    }
    return null;
  }



  private static String getCMTSRelayID(List<? extends Node> inputnodes) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    String result ="";
    try {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CMTSRELAYID)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().contains(DHCPv6Constants.DUIDLLT))
            {
              result = result + "DUIDLLT;";
              for(Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes())
              {
                result=result+subsubnode.getTypeName()+";"+((TypeWithValueNode)subsubnode).getValue()+";";
              }
            }
            if (subnode.getTypeName().contains(DHCPv6Constants.DUIDLL))
            {
              result = result + "DUIDLL;";
              for(Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes())
              {
                result=result+subsubnode.getTypeName()+";"+((TypeWithValueNode)subsubnode).getValue()+";";
              }
            }
            if (subnode.getTypeName().contains(DHCPv6Constants.DUIDEN))
            {
              result = result + "DUIDEN;";
              for(Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes())
              {
                result=result+subsubnode.getTypeName()+";"+((TypeWithValueNode)subsubnode).getValue()+";";
              }

            }
              
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to get CMTSRelayID failed! " + e);
    }
    if(result.length()==0)logger.debug("("+debugmac+") Trying to get CMTSRelayID failed!");
    return result;
  }

  
  private static String getCMTSRemoteID(List<? extends Node> inputnodes) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    String result ="";
    try {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CMTSREMOTEID)) {
          result = ((TypeWithValueNode)node).getValue();
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to get CMTSRemoteID failed! " + e);
    }

    if(result.length()==0)logger.debug("("+debugmac+") Trying to get CMTSRemoteID failed!");
    return result;
  }

  
  
  private static String getVendorClassOption(List<? extends Node> inputnodes) {

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
          for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
            if (subnode.getTypeName().contains(DHCPv6Constants.VENDORCLASS)) {
              if (subnode instanceof TypeWithValueNode) {
                String vcdata = ((TypeWithValueNode) subnode).getValue();
                // if(vcdata.startsWith("0x00") && vcdata.length()>6)
                // {
                // vcdata="0x"+vcdata.substring(6);
                // byte[] tmpbyte = ByteUtil.toByteArray(vcdata);
                // try
                // {
                // vcdata = new String(tmpbyte, "UTF-8");
                // }
                // catch(Exception e)
                // {
                // if(logger.isDebugEnabled())logger.debug("("+debugmac+") Error while converting vendorclass to utf8. Using OctetString ...");
                // vcdata =((TypeWithValueNode) subnode).getValue();
                // }
                // }
                String enterprisenr = subnode.getTypeName().substring(11);
                return enterprisenr + ";" + vcdata;
              }
              if (subnode instanceof TypeOnlyNode) {
                String enterprisenr = subnode.getTypeName().substring(11);
                return enterprisenr + ";";
              }

            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to read VendorClassOption from Reply failed! " + e);
    }

    return null;
  }


  private static String getInterfaceID(List<? extends Node> inputnodes) {

    for (Node node : inputnodes) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.INTERFACEID)) {
        return ((TypeWithValueNode) node).getValue();
      }
    }

    return null;
  }


 
  private static int getPrefixLengthFromIAPDNode(Node inputnode, boolean isRenew) {
    String prefixlength = "";
    if (((TypeOnlyNode) inputnode).getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
      for (Node iapdSubnode : ((TypeOnlyNode) inputnode).getSubnodes()) {
        if (iapdSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPREF)) {
          for (Node iaaddrSubnode : ((TypeOnlyNode) iapdSubnode).getSubnodes()) {
            if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.PREFLENGTH)) {
              prefixlength = ((TypeWithValueNode) iaaddrSubnode).getValue();
            }
          }
        }
      }
    }
    int result = 128;
    
    try
    {
       result = Integer.parseInt(prefixlength);
    }
    catch(Exception e)
    {
      if(isRenew){
        logger.error("PrefixLength could not be read from Renew Request.");
        return 0;
      }
    }

    return result;
  }
  
  
  private static String getClientIpsFromRequest(List<? extends Node> inputnodes) {

    String res = "";
    String prefixlength = "";
    for (Node node : inputnodes) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
          if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA) || subnode.getTypeName()
                          .equalsIgnoreCase(DHCPv6Constants.IATA)) {
            for (Node ianaSubnode : ((TypeOnlyNode) subnode).getSubnodes()) {
              if (ianaSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAADDR)) {
                for (Node iaaddrSubnode : ((TypeOnlyNode) ianaSubnode).getSubnodes()) {
                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
                    res = res + ((TypeWithValueNode) iaaddrSubnode).getValue() + ",";
                  }
                }
              }
            }
          }
          else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
            prefixlength = "";
            for (Node iapdSubnode : ((TypeOnlyNode) subnode).getSubnodes()) {

              if (iapdSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPREF)) {
                for (Node iaaddrSubnode : ((TypeOnlyNode) iapdSubnode).getSubnodes()) {
                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.PREFLENGTH)) {
                    prefixlength = ((TypeWithValueNode) iaaddrSubnode).getValue();
                  }

                  if (iaaddrSubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IPv6)) {
                    res = res + ((TypeWithValueNode) iaaddrSubnode).getValue() + "/" + prefixlength + ",";
                  }
                }
              }
            }


          }
        }
      }
    }
    if (res.length() > 0)
      res = res.substring(0, res.length() - 1);
    return res;
  }


    /**
   * Extrahiert aus der Pooloptions-Tabelle die zur Pool-ID gehoerenden Optionen und schreibt sie in die uebergebene
   * optionsmap.
   * 
   * @return Liste der zu setzenden Optionen
   */
  private static void getPoolOptions(long poolID, Map<String, String> optionsmap) throws PersistenceLayerException {

    ODSConnection conDefault = ods.openConnection();

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      // nimm gecachte Anfrage wenn moeglich
      PreparedQuery<PoolOption> pq = (PreparedQuery<PoolOption>) queryCache
                      .getQueryFromCache(sqlGetPoolOptions, conDefault, new PoolOption()
                                      .getReaderForGUIIdCollAndValueColl());

      Parameter sqlparameter = new Parameter(poolID);
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Query with String: " + sqlGetPoolOptions);
        logger.debug("("+debugmac+") Parameter: " + sqlparameter.get(0));
      }
      Collection<PoolOption> queryResult = conDefault.query(pq, sqlparameter, -1);
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Query result der Options hat Laenge " + queryResult.size());
      }
      for (PoolOption po : queryResult) {
        Collection<String> optioncodes = po.getOptionGuiIdCollection();
        Collection<String> optionvalues = po.getValueCollection();
        Iterator<String> codeIterator = optioncodes.iterator();
        Iterator<String> valueIterator = optionvalues.iterator();
        // nimm Pool-Options in optionsmap auf
        for (int i = 0; i < optioncodes.size(); i++) {
          String code = codeIterator.next();
          String value = valueIterator.next();
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Code = " + code + ", value = " + value);
          }
          optionsmap.put(code, value);
        }
      }

    }
    finally {
      conDefault.closeConnection();
    }

  }


  private static ArrayList<String> getRequestedOptioncodes(List<? extends Node> inputoptions) throws DHCPv6InvalidOptionException {
    ArrayList<String> returnlist = new ArrayList<String>();
    for (Node node : inputoptions) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
          if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.REQUESTLIST)) {
            String octstring = ((TypeWithValueNode) subnode).getValue();
            if (!octstring.startsWith("0x")) { // TODO performance:
              // Pattern.compile(..) zum
              // matchen verwenden
              throw new DHCPv6InvalidOptionException(DHCPv6Constants.REQUESTLIST, octstring);
            }
            String values = octstring.substring(2);
            for (int i = 0; i <= values.length() - 4; i = i + 4) {
              if (logger.isDebugEnabled()) {
                logger
                                .debug("option code " + String.valueOf(Integer.parseInt(values.substring(i, i + 4),
                                                                                           16)) + " (index i = " + i + ", substring = " + values
                                                .substring(i, i + 4) + ")");
              }
              String toadd = String.valueOf(Integer.parseInt(values.substring(i, i + 4), 16));
              if (toadd.equals(DHCPv6Constants.IANACODE) || toadd.equals(DHCPv6Constants.IATACODE) || toadd
                              .equals(DHCPv6Constants.IAPDCODE)) {

              }
              else {
                returnlist.add(toadd);
              }
              // returnlist.add(String.valueOf(Integer.parseInt(values.substring(
              // i, i + 4), 16)));
            }
            return returnlist;
          }
        }
      }

    }
    return null;
  }


  private static Node buildIAoptions_neu(xdnc.dhcpv6.Lease lease, Node ianode, String prefTime, Map<String, String> optionsmap) {

    ArrayList<Node> newSubnodes = new ArrayList<Node>();
    List<? extends Node> subnodes = (List<? extends Node>) ((TypeOnlyNode) ianode).getSubnodes();

    if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) {
      // Subnodes in IANA sind: IAID, T1, T2, options - Reihenfolge der Subnodes
      // ist relevant!
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      TypeWithValueNode t1 = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, optionsmap
                      .get(DHCPv6Constants.IANAT1));
      TypeWithValueNode t2 = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, optionsmap
                      .get(DHCPv6Constants.IANAT2));
      
      if(enablemclt) // bei mclt aktiviert, wird der Wert entsprechend anders gesetzt.
      {
        t1.setValue(String.valueOf(mclt/2));
        t2.setValue(String.valueOf(mclt-mcltOffsetInSeconds));
      }
      
      newSubnodes.add(iaid);
      newSubnodes.add(t1);
      newSubnodes.add(t2);
      // Reihenfolge in IA-Address-Option: IPv6 adddress, pref. lifetime, valid
      // lifetime, options
      ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();

      TypeWithValueNode prefLeaseTime = new TypeWithValueNode(
          DHCPv6Constants.PREFLEASETIME, String.valueOf(lease
              .getPreferredlifetime()));// preferredLifetime ist schon in
                                        // Sekunden
      TypeWithValueNode validLeaseTime = new TypeWithValueNode(
          DHCPv6Constants.VALIDLEASETIME, String.valueOf(lease
              .getValidlifetime()));// validLifetime ist schon in Sekunden
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, String
          .valueOf(lease.getIp()));
      
      iaAddrSubnodes.add(ip);
      iaAddrSubnodes.add(prefLeaseTime);
      iaAddrSubnodes.add(validLeaseTime);
      TypeOnlyNode iaAddr = new TypeOnlyNode(DHCPv6Constants.IAADDR, iaAddrSubnodes);

      newSubnodes.add(iaAddr);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IANA, newSubnodes);
      return newIA;
    }
    else if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IATA)) {
      // Subnodes in IATA sind: IAID, options
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      newSubnodes.add(iaid);
      ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();
      TypeWithValueNode prefLeaseTime = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, String.valueOf(lease
          .getPreferredlifetime()));
      TypeWithValueNode validLeaseTime = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, String.valueOf(lease
          .getValidlifetime()));
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, String.valueOf(lease.getIp()));// TODO:
                                                                                                        // Format!!!
      
      iaAddrSubnodes.add(ip);
      iaAddrSubnodes.add(prefLeaseTime);
      iaAddrSubnodes.add(validLeaseTime);
      TypeOnlyNode iaAddr = new TypeOnlyNode(DHCPv6Constants.IAADDR, iaAddrSubnodes);

      newSubnodes.add(iaAddr);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IATA, newSubnodes);
      return newIA;

    }
    else if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
      // Subnodes in IAPD sind: IAID, T1, T2, options - Reihenfolge der Subnodes
      // ist relevant!
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      TypeWithValueNode t1 = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, optionsmap
                      .get(DHCPv6Constants.IAPDT1));
      TypeWithValueNode t2 = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, optionsmap
                      .get(DHCPv6Constants.IAPDT2));
      
      if(enablemclt) // bei mclt aktiviert, wird der Wert entsprechend anders gesetzt.
      {
        t1.setValue(String.valueOf(mclt/2));
        t2.setValue(String.valueOf(mclt-mcltOffsetInSeconds));
      }

      newSubnodes.add(iaid);
      newSubnodes.add(t1);
      newSubnodes.add(t2);
      // IAPrefix: T1, T2, PrefixLength, PrefixAddress, options

      ArrayList<Node> iaPrefSubnodes = new ArrayList<Node>();

      TypeWithValueNode prefLeaseTime = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, String.valueOf(lease
          .getPreferredlifetime()));// prefTime ist schon in Sekunden
      TypeWithValueNode validLeaseTime = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, String.valueOf(lease
          .getValidlifetime()));
      TypeWithValueNode prefixLength = new TypeWithValueNode(DHCPv6Constants.PREFLENGTH, String.valueOf(lease
                      .getPrefixlength()));
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, String.valueOf(lease.getIp()));// TODO:
                                                                                                        // Format!!!

      iaPrefSubnodes.add(prefLeaseTime);
      iaPrefSubnodes.add(validLeaseTime);
      iaPrefSubnodes.add(prefixLength);
      iaPrefSubnodes.add(ip);

      TypeOnlyNode iaPref = new TypeOnlyNode(DHCPv6Constants.IAPREF, iaPrefSubnodes);

      newSubnodes.add(iaPref);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IAPD, newSubnodes);
      return newIA;

    }

    return null;

  }


  /**
   * Generiert einen IA-Node zu reservierten Hosts
   */
  private static Node buildIAoptionsForReservedHost(String assignedIp, Node ianode, int prefixlength) {

    ArrayList<Node> newSubnodes = new ArrayList<Node>();
    List<? extends Node> subnodes = (List<? extends Node>) ((TypeOnlyNode) ianode).getSubnodes();

    if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) {
      // Subnodes in IANA sind: IAID, T1, T2, options - Reihenfolge der Subnodes
      // ist relevant!
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      TypeWithValueNode t1 = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, String.valueOf(reservationpreferredlifetime));
      TypeWithValueNode t2 = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, String.valueOf(reservationpreferredlifetime));
      newSubnodes.add(iaid);
      newSubnodes.add(t1);
      newSubnodes.add(t2);
      // Reihenfolge in IA-Address-Option: IPv6 adddress, pref. lifetime, valid
      // lifetime, options
      ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();


TypeWithValueNode prefLeaseTime = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME,
                                                              String.valueOf(reservationpreferredlifetime));

      TypeWithValueNode validLeaseTime = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME,
                                                               String.valueOf(reservationpreferredlifetime));
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, assignedIp);// TODO: Format!!!
      iaAddrSubnodes.add(ip);
      iaAddrSubnodes.add(prefLeaseTime);
      iaAddrSubnodes.add(validLeaseTime);
      TypeOnlyNode iaAddr = new TypeOnlyNode(DHCPv6Constants.IAADDR, iaAddrSubnodes);

      newSubnodes.add(iaAddr);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IANA, newSubnodes);
      return newIA;
    }
    else if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IATA)) {
      // Subnodes in IATA sind: IAID, options
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      newSubnodes.add(iaid);
      ArrayList<Node> iaAddrSubnodes = new ArrayList<Node>();
      TypeWithValueNode prefLeaseTime = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME,
                                                              String.valueOf(reservationpreferredlifetime));

      TypeWithValueNode validLeaseTime = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME,
                                                               String.valueOf(reservationpreferredlifetime));
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, assignedIp);// TODO: Format!!!
      iaAddrSubnodes.add(ip);
      iaAddrSubnodes.add(prefLeaseTime);
      iaAddrSubnodes.add(validLeaseTime);
      TypeOnlyNode iaAddr = new TypeOnlyNode(DHCPv6Constants.IAADDR, iaAddrSubnodes);

      newSubnodes.add(iaAddr);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IATA, newSubnodes);
      return newIA;

    }
    else if (ianode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
      // Subnodes in IAPD sind: IAID, T1, T2, options - Reihenfolge der Subnodes
      // ist relevant!
      TypeWithValueNode iaid = new TypeWithValueNode(DHCPv6Constants.IAID, ((TypeWithValueNode) subnodes.get(0))
                      .getValue());
      TypeWithValueNode t1 = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME, String.valueOf(reservationpreferredlifetime));
      TypeWithValueNode t2 = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME, String.valueOf(reservationpreferredlifetime));
      newSubnodes.add(iaid);
      newSubnodes.add(t1);
      newSubnodes.add(t2);
      // IAPrefix: T1, T2, PrefixLength, PrefixAddress, options

      ArrayList<Node> iaPrefSubnodes = new ArrayList<Node>();

      TypeWithValueNode prefLeaseTime = new TypeWithValueNode(DHCPv6Constants.PREFLEASETIME,
                                                              String.valueOf(reservationpreferredlifetime));// prefTime ist schon
                                                                                                  // in Sekunden

      TypeWithValueNode validLeaseTime = new TypeWithValueNode(DHCPv6Constants.VALIDLEASETIME,
                                                               String.valueOf(reservationpreferredlifetime));// Zeiten im lease
                                                                                                   // sind in
                                                                                                   // Millisekunden
      TypeWithValueNode prefixLength = new TypeWithValueNode(DHCPv6Constants.PREFLENGTH, String.valueOf(prefixlength));
      TypeWithValueNode ip = new TypeWithValueNode(DHCPv6Constants.IPv6, assignedIp);// TODO: Format!!!

      iaPrefSubnodes.add(prefLeaseTime);
      iaPrefSubnodes.add(validLeaseTime);
      iaPrefSubnodes.add(prefixLength);
      iaPrefSubnodes.add(ip);

      TypeOnlyNode iaPref = new TypeOnlyNode(DHCPv6Constants.IAPREF, iaPrefSubnodes);

      newSubnodes.add(iaPref);
      TypeOnlyNode newIA = new TypeOnlyNode(DHCPv6Constants.IAPD, newSubnodes);
      return newIA;

    }

    return null;

  }


  /**
   * Bestimmung der zu vergebenden Lease-Zeit aus den vorher gesetzten Klassen-Optionen. Die Lease-Zeit wird in einer
   * IA-Address-Option (innerhalb einer IA-NA bzw. IA-TA-Option) transportiert.
   * 
   * @param outputoptionsFromPoolDetermination
   */
  private static long getLeaseTimeFromOptions(List<? extends Node> options) {
    for (Node node : options) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.VALIDLEASETIME)) {
        return Long.parseLong(((TypeWithValueNode) node).getValue());
      }
    }
    return -1;
  }


  private static HashMap getVendorSpecificInformationAsHashmap(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    HashMap result = new HashMap();

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    try {
      for (Node relaymsg : inputoptions) {
        if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
          for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
            if (node.getTypeName().contains(DHCPv6Constants.VENDORSPECINFO)) {
              ton = (TypeOnlyNode) node;
              for (Node subnode : ton.getSubnodes()) {
                if (subnode instanceof TypeWithValueNode) {
                  twvn = (TypeWithValueNode) subnode;
                  result.put(twvn.getTypeName(), twvn.getValue());

                }
                else {
                  HashMap subresult = new HashMap();

                  ton = (TypeOnlyNode) subnode;
                  for (Node newsubnode : ton.getSubnodes()) {
                    subresult.put(newsubnode.getTypeName(), ((TypeWithValueNode) newsubnode).getValue());
                  }
                  result.put(ton.getTypeName(), subresult);
                }

              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      if (logger.isDebugEnabled())
        logger.debug("("+debugmac+") Trying to read VendorSpecificInformation from Reply failed! " + e);
    }

    return result;
  }


  private static String getMACfromOptions(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CLIENTID)) {
            for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
              if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDLLT) || subnode.getTypeName()
                              .equalsIgnoreCase(DHCPv6Constants.DUIDLL)) {
                for (Node subsubnode : ((TypeOnlyNode) subnode).getSubnodes()) {
                  if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKLAYERADDR)) {
                    twvn = (TypeWithValueNode) subsubnode;
                    return twvn.getValue().substring(2).toLowerCase();
                  }
                }
              }
              else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDEN)) {

                // TODO: wo bekommt man hier die MAC-Adresse des Clients her?

              }
            }
          }
        }
      }
    }

    return null;
  }

  private static String getMACfromLeaseQuery(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    for (Node node : inputoptions) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CLIENTID)) {
            ton = (TypeOnlyNode) node;
            for (Node subnode : ton.getSubnodes()) {
              if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDLLT) || subnode.getTypeName()
                              .equalsIgnoreCase(DHCPv6Constants.DUIDLL)) {
                ton2 = (TypeOnlyNode) subnode;
                for (Node subsubnode : ton2.getSubnodes()) {
                  if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.LINKLAYERADDR)) {
                    twvn = (TypeWithValueNode) subsubnode;
                    return twvn.getValue().substring(2).toLowerCase();
                  }
                }
              }
              else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDEN)) {

                // TODO: wo bekommt man hier die MAC-Adresse des Clients her?

              }
            }
          }
        }

    return null;
  }

  

  private static List<String> getDUIDInfosfromOptions(List<? extends Node> inputoptions) {
    TypeOnlyNode ton;
    TypeOnlyNode ton2;
    TypeWithValueNode twvn;

    List<String> results = new ArrayList<String>();

    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.CLIENTID)) {
            ton = (TypeOnlyNode) node;
            for (Node subnode : ton.getSubnodes()) {
              if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDLLT) || subnode.getTypeName()
                              .equalsIgnoreCase(DHCPv6Constants.DUIDLL)) {
                ton2 = (TypeOnlyNode) subnode;
                for (Node subsubnode : ton2.getSubnodes()) {
                  if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.HARDWARETYPE)) {
                    twvn = (TypeWithValueNode) subsubnode;
                    results.add(twvn.getValue());
                  }
                  if (subsubnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDTIME)) {
                    twvn = (TypeWithValueNode) subsubnode;
                    results.add(twvn.getValue());
                  }

                }
              }
              else if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.DUIDEN)) {

                // TODO: wo bekommt man hier die MAC-Adresse des Clients her?

              }
            }
          }
        }
      }
    }

    return results;
  }


  /**
   * Converts the MAC notation from Octetstring 0x0012ab096c12 to 0012ab096c12
   */
  private static String convertMAC(String mac) {
    // TODO performance: Pattern.compile(..) für das matchen auf "0x"
    // verwenden
    if ((mac.length() != 14) || (!mac.startsWith("0x"))) {
      return null;
    }
    return mac.substring(2);

  }


  /**
   * Converts the MAC notation from Octetstring 0x0012ab096c12 to 00:12:ab:09:6c:12
   */
  private static String convertMACOctetStringToMacAddress(String mac) {
    // TODO performance: Pattern.compile(..) für das matchen auf "0x"
    // verwenden
    if ((mac.length() != 14) || (!mac.startsWith("0x"))) {
      return null;
    }
    mac = mac.substring(2); // 0x entfernen
    String result = "";

    result = mac.substring(0, 2) + ":" + mac.substring(2, 4) + ":" + mac.substring(4, 6) + ":" + mac.substring(6, 8) + ":" + mac
                    .substring(8, 10) + ":" + mac.substring(10, 12);

    return result;


  }
  
  /**
   * Schreibt einen einzelnen Host-Eintrag in die leasestable (anhand der MAC) 
   */
  
  public static void writeHostToLeasesTable(MAC mac) throws DHCPv6InvalidDBEntriesException, PersistenceLayerException
  {
    
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    try {
      Collection<? extends Host> reservedHosts = new ArrayList<Host>();
      Collection<? extends Lease> reservedLeases = new ArrayList<Lease>();
      // Alle Reserved Hosts aus Hosts Tabelle holen, die eine zugewiesene IP
      // Adresse haben und passende MAC

      reservedHosts = DHCPv6ODS.queryODS(con, "SELECT * FROM host WHERE mac = ? and assignedIp!=''", new Parameter(mac.getMac()), new Host().getReader(),-1);

      reservedLeases = DHCPv6ODS.queryODS(con, "SELECT * FROM leasestable WHERE mac = ?", new Parameter(mac.getMac()), new Lease().getReader(),-1);

      
      logger.debug("reservedHosts Size: "+reservedHosts.size());
      //      if (reservedHosts.size() > 1){
//        logger.error("Found more than one host with mac " + mac.getMac() + ".");
//        throw new DHCPv6InvalidDBEntriesException(
//            "Found more than one host with mac " + mac.getMac() + ".");
//      }
      if (reservedHosts.size() == 0){
        Collection<? extends Host> reservedHostsMacOnly =DHCPv6ODS.queryODS(con, "SELECT * FROM host WHERE mac = ?", new Parameter(mac.getMac()), new Host().getReader(),-1); 
                        
        if (reservedHostsMacOnly.size() == 0) {
          logger.info("Found no host with mac "
              + mac.getMac() + ".");
          //throw new DHCPv6InvalidDBEntriesException("Found no host with mac "
             // + mac.getMac() + ".");
        }
        logger.info("Host entry corresponding to mac " + mac.getMac() + " has no assigned IP address - skipping entry into leasestable.");
        if (logger.isDebugEnabled()){
          logger.debug("Host entry corresponding to mac " + mac.getMac() + " has no assigned IP address - skipping entry into leasestable.");
        }
        return;
      }
      boolean cont = false;

      for(Host host : reservedHosts)
      {
        cont = false;
        for(Lease l: reservedLeases)
        {
          if(l.getIp().equals(host.getAssignedIp()))
          {
            cont=true;
          }
        }
        if(cont)continue;
//        long internalpoolid = determineInternalPoolIdCorrespondingToGuiId(host
//            .getAssignedPoolID(), con);
        String dyndns = "";
        if (host.getDynamicDnsActive().equalsIgnoreCase("true"))
          dyndns = domainname;
        String remoteid = host.getAgentRemoteId();
        String linkaddress = host.getCmtsip();
        int prefixlength = 128;
        try
        {
          prefixlength = Integer.parseInt(host.getPrefixlength());
        }
        catch(Exception e)
        {
          logger.debug("Host prefixlength could not be parsed as Integer");
        }
        
        
        
        Lease lease = new Lease(IPv6AddressUtil.parse(host.getAssignedIp()));
        lease.setMac(mac.getMac());
        //lease.setMacAsNum(macAsNum);
        lease.setExpirationtime(System.currentTimeMillis()+leasetimeforstaticips);
        lease.setStartTime(System.currentTimeMillis());
        //lease.setReservationTime(System.currentTimeMillis()+DHCPv6ServicesImpl.defaultReservationTime);
        lease.setDppInstance(myname);
        lease.setPreferredLifetime( (leasetimeforstaticips / 1000L));
        long leasetimeinseconds = leasetimeforstaticips / 1000L;
        lease.setValidLifetime(leasetimeinseconds);
        lease.setType("static");
        lease.setDynDnsZone(dyndns);
        lease.setCMRemoteId(remoteid);
        //lease.setLeaseID(XynaFactory.getInstance().getIDGenerator().getUniqueId());
        lease.setCmtsip(linkaddress);
        lease.setPrefixlength(prefixlength);
        lease.setBinding(bindingToBeSetForStaticIPs);

  
        con.persistObject(lease);
        con.commit();
      }    
    }
    catch(Exception e)
    {
      logger.info("Exception while writing static Host Leasestable: ",e);
      throw new RuntimeException(e);
    }
     finally {
      con.closeConnection();
    }

  }
  
  
  
  
  public static void deleteHostFromLeasesTable(IPv6 ip) throws PersistenceLayerException {
    
    //long internalpoolid = lease.getPoolId();
//    ReservedHostCondition condition = new ReservedHostCondition(lease.getIp(), internalpoolid);
//    OrderedVirtualObjectPool<IP, Lease> vpool = getCachedPool(String.valueOf(internalpoolid));
//    vpool.deleteElements(condition);
    
    ODSConnection con = ods.openConnection();
    try {

      Collection<? extends Lease> leaseToDelete = DHCPv6ODS.queryODS(con, "Select * from leasestable where ip = ? for update", new Parameter(ip.getIP().toLowerCase()), new Lease().getReader(),-1);
      con.delete(leaseToDelete);
      con.commit();
    }
    catch(Exception e)
    {
      logger.info("Exception while deleting static Host from Leasestable: ",e);
      throw new RuntimeException(e);
    }
     finally {
      con.closeConnection();
    }
    
    
    
  }


  public static void restoreOptionsAdm() throws PersistenceLayerException
  {
    RestoreOptionsAdm restore = new RestoreOptionsAdm();
    restore.restore();
    logger.info("Optionsv6Adm restored");
    
  }
  
 
  
  
  private static void getOdsAndRegisterStorables() throws PersistenceLayerException {
    if (ods == null) {

      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS()
          .getODS();
      ods.registerStorable(Lease.class);
      ods.registerStorable(PoolOption.class);
      ods.registerStorable(DeviceClass.class);
      ods.registerStorable(DppFixedAttribute.class);
      ods.registerStorable(GuiAttribute.class);
      ods.registerStorable(GuiFixedAttribute.class);
      ods.registerStorable(GuiOperator.class);
      ods.registerStorable(GuiParameter.class);
      ods.registerStorable(com.gip.xyna.xdnc.dhcpv6.db.storables.Condition.class);
      ods.registerStorable(com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType.class);
      ods.registerStorable(Host.class);
      ods.registerStorable(DHCPv6Encoding.class);

    }
  }
  
  
  
  
  public static void generateIPPools() throws XynaException {
    
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Starting Pool Deployment ...");
    
    try {
      parseFile();
    }
    catch (XynaException e) {
      logger.warn("XynaException within GenerateIPPools: " + e
          + " - aborting pool configuration, continue using former pools");
      throw new RuntimeException(e);
    }
    catch (DHCPPoolsFormatException e) {
      logger.warn("DHCPPoolsFormatException within GenerateIPPools: " + e
          + " - aborting pool configuration, continue using former pools");
  throw new RuntimeException(e);
    }

    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Parsed Pools-Config-File succesfully");

  }






  public static Container generateInputDataForWf() {
    return new Container(new XynaObjectList<Node>(new ArrayList<Node>(), Node.class), new IPPool());
  }


  public static void parseFile() throws XynaException, DHCPPoolsFormatException, DHCPv6SpecificPropertyNotSetException {
    String fileName = XynaFactory.getInstance().getFactoryManagement().getProperty(PROPERTY_POOLSCONFIGFILE);
    if (fileName == null || fileName.length() == 0) {
      throw new DHCPv6SpecificPropertyNotSetException(PROPERTY_POOLSCONFIGFILE, "not set correctly. file not found.");
    }
    File f = new File(fileName);
    if (!f.exists()) {
      throw new DHCPv6SpecificPropertyNotSetException(PROPERTY_POOLSCONFIGFILE,"not set correctly. file not found.");
    }
    DHCPPoolsParser parser;
    try {
      parser = new DHCPPoolsParser(new FileInputStream(f));
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e); // wird oben abgefangen
    }

    try {
      parser.parse();
    }
    catch (IOException e) {
      throw new RuntimeException("file " + f.getAbsolutePath() + " could not be parsed successfully.", e);
    }

    DHCPPoolsUpdater poolsUpdater = new DHCPPoolsUpdater(ODSImpl.getInstance(), f.lastModified(), bindingToBeSet);
    poolsUpdater.updateData(parser.getSharedNetworks());
    // alle IPIntervals, Leases haetten momentan das Binding mybinding
    // // die eingelesenen Daten werden zunaechst gleichmaessig auf beide Server verteilt
    // BalanceParameter[] equalBalanceParameters = {new BalanceParameter(mybinding, 5),
    // new BalanceParameter(partnerbinding, 5)};
    // if (logger.isDebugEnabled()) {
    // logger
    // .debug("## Balancing parsed pool configuration 50-50 between binding "
    // + mybinding + " and binding " + partnerbinding);
    // }
    // for (OrderedVirtualObjectPool<IP, Lease> vpool : poollist.values()){
    // vpool.balanceBindingsOfFreeElements(equalBalanceParameters, 0.1);
    // }
  }

  



  public static final String sqlLoadDeviceClasses = "SELECT * FROM " + DeviceClass.TABLENAME;
  private static ArrayList<DeviceClass> classList = new ArrayList<DeviceClass>();
  public static Map<Integer, Conditional> parsedConditionalsMap = new HashMap<Integer, Conditional>();


  private static void loadDeviceClassesFromDB(ODSConnection con) throws PersistenceLayerException {

    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<DeviceClass> pq = (PreparedQuery<DeviceClass>) queryCache
                    .getQueryFromCache(sqlLoadDeviceClasses, con, new DeviceClass().getReader());

    Parameter sqlparameter = new Parameter();
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadDeviceClasses);
    }
    Collection<DeviceClass> queryResult = con.query(pq, sqlparameter, -1);
    classList = new ArrayList<DeviceClass>(queryResult);
    Collections.sort(classList);
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") classList has size " + classList.size());
    }

    // parsen der Conditionals der Klassen, so dass sie bei Bedarf sofort
    // ausgewertet werden können
    // Speichern der geparsten Conditionals in einer statischen HashMap.
    for (DeviceClass deviceclass : queryResult) {
      Map<String, String> subConditionalHash = new HashMap<String, String>();
      int counter = 0;
      String resultingConditional = DeviceClass.parseConditional(deviceclass.getConditional(), subConditionalHash,
                                                                 counter);
      parsedConditionalsMap.put(deviceclass.getClassID(), new Conditional(deviceclass.getConditional(),
                                                                          resultingConditional, subConditionalHash));
    }

    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Loaded device classes from DB - query result of length " + queryResult.size());
    }
  }


  public static final String sqlLoadConditions = "SELECT * FROM " + com.gip.xyna.xdnc.dhcpv6.db.storables.Condition.TABLENAME;
  public static Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> conditionsMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>();


  private static void loadConditionsFromDB(ODSConnection con) throws PersistenceLayerException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> pq = (PreparedQuery<com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>) queryCache
                    .getQueryFromCache(sqlLoadConditions, con, new com.gip.xyna.xdnc.dhcpv6.db.storables.Condition()
                                    .getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadConditions);
    }
    Collection<com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> queryResult = con.query(pq, sqlparameter, -1);
    for (com.gip.xyna.xdnc.dhcpv6.db.storables.Condition cond : queryResult) {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Adding Condition " + cond.getConditionID() + " to conditionsMap");
      }
      conditionsMap.put(cond.getConditionID(), cond);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Loaded conditions from DB - query result of length " + queryResult.size());
    }
  }


  public static final String sqlLoadGuiOperators = "SELECT * FROM " + GuiOperator.TABLENAME;
  public static Map<String, GuiOperator> operatorsMap = new HashMap<String, GuiOperator>();


  private static void loadGuiOperatorsFromDB(ODSConnection con) throws PersistenceLayerException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<GuiOperator> pq = (PreparedQuery<GuiOperator>) queryCache
                    .getQueryFromCache(sqlLoadGuiOperators, con, new GuiOperator().getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadGuiOperators);
    }
    Collection<GuiOperator> queryResult = con.query(pq, sqlparameter, -1);
    for (GuiOperator op : queryResult) {
      operatorsMap.put(String.valueOf(op.getGuiOperatorID()), op);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Loaded guioperators from DB - query result of length " + queryResult.size());
    }
  }


  public static final String sqlLoadGuiParameters = "SELECT * FROM " + GuiParameter.TABLENAME;
  public static Map<Integer, String> parametersMap = new HashMap<Integer, String>();
  // private static final Pattern PARAMETEROPTION_PATTERN =
  // Pattern.compile("^\\s*Option\\s+(\\d+(?:\\.\\d+)?)\\s*$");
  private static final Pattern PARAMETEROPTION_PATTERN = Pattern
                  .compile("^\\s*option\\s+([\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+)?)?)\\s*$");


  private static void loadGuiParameters(ODSConnection con) throws PersistenceLayerException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<GuiParameter> pq = (PreparedQuery<GuiParameter>) queryCache
                    .getQueryFromCache(sqlLoadGuiParameters, con, new GuiParameter().getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadGuiParameters);
    }
    Collection<GuiParameter> queryResult = con.query(pq, sqlparameter, -1);
    for (GuiParameter p : queryResult) {
      String optionCode = "";
      Matcher matcher = PARAMETEROPTION_PATTERN.matcher(p.getDhcpConf());
      if (matcher.matches()) {
        optionCode = matcher.group(1);
      }
      else {
        // TODO: Fehlerbehandlung
      }
      parametersMap.put(p.getGuiParameterID(), optionCode);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Loaded guiparameters from DB - query result of length " + queryResult.size());
    }
  }


  //public static final String sqlLoadDppFixedAttributes = "SELECT * FROM " + DppFixedAttribute.TABLENAME + " WHERE " + DppFixedAttribute.COL_NAME + " = ?";
  public static final String sqlLoadDppFixedAttributes = "SELECT * FROM " + DppFixedAttribute.TABLENAME + " WHERE " + DppFixedAttribute.COL_ETH0 + " = ?";
  public static final String sqlLoadDppFixedAttributesPartner = "SELECT * FROM " + DppFixedAttribute.TABLENAME + " WHERE " + DppFixedAttribute.COL_ETH1 + " = ?";
  public static Map<String, String> dppFixedAttributesMap = new HashMap<String, String>();


  private static void loadDppFixedAttributes(ODSConnection con) throws PersistenceLayerException, DHCPv6InconsistentDataException, DHCPv6NoUniqueDppFixedAttributeException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<DppFixedAttribute> pq = (PreparedQuery<DppFixedAttribute>) queryCache
                    .getQueryFromCache(sqlLoadDppFixedAttributes, con, new DppFixedAttribute().getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    
    //Parameter sqlparameter = new Parameter(myname);
    myeth0 = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(MYETH0);
    partnerseth1 = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(PARTNERSETH1);
    Parameter sqlparameter = new Parameter(myeth0);
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadDppFixedAttributes + " and parameter " + sqlparameter.get(0));
    }
    Collection<DppFixedAttribute> queryResultMyName = con.query(pq, sqlparameter, -1);
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query result of length " + queryResultMyName.size());
    }
    if (queryResultMyName.size() != 1) {
      logger.warn("No unique DppFixedAttributes given for " + myeth0+ " - aborting");
      throw new DHCPv6NoUniqueDppFixedAttributeException("No unique DppFixedAttributes given for " + myeth0);
    }

    PreparedQuery<DppFixedAttribute> pqPartner = (PreparedQuery<DppFixedAttribute>) queryCache
    .getQueryFromCache(sqlLoadDppFixedAttributesPartner, con, new DppFixedAttribute().getReader());

    Parameter sqlparameterPartner = new Parameter(partnerseth1);
    Collection<DppFixedAttribute> queryResultMyPartner = con.query(pqPartner, sqlparameterPartner, -1);
    if (queryResultMyPartner.size() != 1) {
      logger.warn("No unique DppFixedAttributes given for " + partnerseth1+ " - aborting");
      throw new DHCPv6NoUniqueDppFixedAttributeException("No unique DppFixedAttributes given for " + partnerseth1);
    }

    DppFixedAttribute myFixedAttributes = queryResultMyName.iterator().next();
    DppFixedAttribute myPartnersFixedAttributes = queryResultMyPartner.iterator().next();

    dppFixedAttributesMap.put(DHCPv6Constants.FQDN, myFixedAttributes.getName());

    
    // XEF_FQDN-Codierung findet jetzt im Encoder statt
//    String hexFqdn = encodeHEX_FQDN(myFixedAttributes.getName());// hat noch 0x vorne dran
//    dppFixedAttributesMap.put(DHCPv6Constants.HEX_FQDN, (new StringBuilder("00")).append(hexFqdn.substring(2))
//                    .toString());
    
    dppFixedAttributesMap.put(DHCPv6Constants.HEX_FQDN, convertStringToHexString(myFixedAttributes.getName()));
    
    if (myFixedAttributes.getEth1() != null){
      dppFixedAttributesMap.put(DHCPv6Constants.ETH1, myFixedAttributes.getEth1().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH1, myFixedAttributes.getEth1());
    }
    if (myFixedAttributes.getEth1peer() != null){
      dppFixedAttributesMap.put(DHCPv6Constants.ETH1PEER, myFixedAttributes.getEth1peer().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH1PEER, myFixedAttributes.getEth1peer());
    }
    if (myFixedAttributes.getEth2() != null){
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2, myFixedAttributes.getEth2().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2, myFixedAttributes.getEth2());
    }
    if (myFixedAttributes.getEth2v6() != null) {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2v6, myFixedAttributes
          .getEth2v6().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2v6, myFixedAttributes
          .getEth2v6());
    }
    if (myPartnersFixedAttributes.getEth2() != null) {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2PEER, myPartnersFixedAttributes.getEth2().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2PEER, myPartnersFixedAttributes.getEth2());
    }
    if (myPartnersFixedAttributes.getEth2v6() != null) {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2v6PEER, myPartnersFixedAttributes.getEth2v6().toLowerCase());
    } else {
      dppFixedAttributesMap.put(DHCPv6Constants.ETH2v6PEER, myPartnersFixedAttributes.getEth2v6());
    }
    
    dppFixedAttributesMap.put(DHCPv6Constants.DOMAINNAME, myFixedAttributes.getDomainName());
    // dieser Wert kann erst bei Bedarf unter Zuhilfenahme einer MAC-Adresse
    // ausgerechnet werden
    dppFixedAttributesMap.put(DHCPv6Constants.DPPGUID, DHCPv6Constants.DPPGUID);

    String myEth2String = myFixedAttributes.getEth2();
    if (myEth2String.contains(":")) {// IPv6Adresse
      IPv6AddressUtil myEth2 = IPv6AddressUtil.parse(myFixedAttributes.getEth2().toLowerCase());
      IPv6AddressUtil partnersEth2 = IPv6AddressUtil.parse(myPartnersFixedAttributes.getEth2().toLowerCase());
      if (myEth2.compareTo(partnersEth2) > 0) {
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2L, partnersEth2.asLongString());
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2U, myEth2.asLongString());
      }
      else {
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2L, myEth2.asLongString());
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2U, partnersEth2.asLongString());
      }
    }
    else {// IPv4-Adresse
      IPv4AddressUtil myEth2 = new IPv4AddressUtil(myEth2String);
      IPv4AddressUtil partnersEth2 = new IPv4AddressUtil(myPartnersFixedAttributes.getEth2());
      if (myEth2.compareTo(partnersEth2) > 0) {
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2L, partnersEth2.getIpstring());
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2U, myEth2.getIpstring());
      }
      else {
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2L, myEth2.getIpstring());
        dppFixedAttributesMap.put(DHCPv6Constants.ETH2U, partnersEth2.getIpstring());
      }
    }


  }


  private static final Pattern HEX_PATTERN_START = Pattern.compile("0x.*");


  private static String encodeHEX_FQDN(String name) {
    StringBuilder result = new StringBuilder();
    result.append("0x");
    String[] nameParts = StringUtils.fastSplit(name.toLowerCase(), '.', -1);
    for (String part : nameParts) {
      byte[] nameAsBytearray = Charset.forName("US-ASCII").encode(part).array();
      String hexString = ByteUtil.toHexValue(nameAsBytearray);
      String hexPart = hexString.substring(2);// Abschneiden der
      // vornangestellten 0x

      int partlength = part.length();
      StringBuilder sb = new StringBuilder();
      if (partlength < 16) {
        sb.append("0").append(Integer.toHexString(part.length()).toUpperCase());
      }
      else {
        sb.append(Integer.toHexString(part.length()).toUpperCase());
      }

      result.append(sb.toString()).append(hexPart);
    }
    result.append("00");

    return result.toString();
  }


  public static final String sqlLoadGuiFixedAttributes = "SELECT * FROM " + GuiFixedAttribute.TABLENAME;
  public static Map<Integer, GuiFixedAttributeOptionValuePair> guiFixedAttributesMap = new HashMap<Integer, GuiFixedAttributeOptionValuePair>();

  private static final Pattern OPTION_PATTERN = Pattern
                  .compile("^\\s*option\\s+([\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+)?)?)\\s+<VALUE>\\s*$");


  private static void loadGuiFixedAttributes(ODSConnection con) throws PersistenceLayerException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<GuiFixedAttribute> pq = (PreparedQuery<GuiFixedAttribute>) queryCache
                    .getQueryFromCache(sqlLoadGuiFixedAttributes, con, new GuiFixedAttribute().getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadGuiFixedAttributes);
    }
    Collection<GuiFixedAttribute> queryResult = con.query(pq, sqlparameter, -1);

    for (GuiFixedAttribute attr : queryResult) {
      if (checkValue(attr)) {

        String optionCode = "";
        Matcher matcher = OPTION_PATTERN.matcher(attr.getDhcpConf());
        if (matcher.matches()) {
          optionCode = matcher.group(1);
          // Sonderbehandlung fuer Option 17, da unterschiedliche
          // Enterprise-Nummern direkt am Namen haengen
          if (attr.getOptionEncoding().equals(DHCPv6Constants.VENDORSPECINFOCODE)) {
            codeToOptionMap.put(DHCPv6Constants.VENDORSPECINFOCODE, DHCPv6Constants.VENDORSPECINFO);
          }
          else {
            codeToOptionMap.put(attr.getOptionEncoding(), StringUtils.fastSplit(optionCode, '.', 2)[0]);
          }

        }
        else {
          // TODO: Fehlerbehandlung
        }
        // Wert des FixedAttributes
        String fixedAttrName = attr.getValue();// Bezeichner des
        // FixedAttributes, z.B. FQDN
        // oder ETH_0
        String fixedAttrValue = dppFixedAttributesMap.get(fixedAttrName);
        GuiFixedAttributeOptionValuePair optionValuePair = new GuiFixedAttributeOptionValuePair(optionCode,
                                                                                                fixedAttrValue);
        guiFixedAttributesMap.put(attr.getGuiFixedAttributeID(), optionValuePair);
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Illegal value in guifixedattributes");
        }
        // TODO
      }
    }

  }


  private static boolean checkValue(GuiFixedAttribute attr) {
    return attr.getValueRange().contains(attr.getValue());
  }


  public static final String sqlLoadGuiAttributes = "SELECT * FROM " + GuiAttribute.TABLENAME;
  public static Map<Integer, GuiAttributeOptionValuerangePair> guiAttributesMap = new HashMap<Integer, GuiAttributeOptionValuerangePair>();


  private static void loadGuiAttributes(ODSConnection con) throws PersistenceLayerException {
    
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<GuiAttribute> pq = (PreparedQuery<GuiAttribute>) queryCache
                    .getQueryFromCache(sqlLoadGuiAttributes, con, new GuiAttribute().getReader());

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadGuiAttributes);
    }
    Collection<GuiAttribute> queryResult = con.query(pq, sqlparameter, -1);

    for (GuiAttribute attr : queryResult) {

      String optionCode = "";
      Matcher matcher = OPTION_PATTERN.matcher(attr.getDhcpConf());
      if (matcher.matches()) {
        optionCode = matcher.group(1);

        // Sonderbehandlung fuer Option 17, da unterschiedliche
        // Enterprise-Nummern direkt am Namen haengen
        if (attr.getOptionEncoding().equals(DHCPv6Constants.VENDORSPECINFOCODE)) {
          codeToOptionMap.put(DHCPv6Constants.VENDORSPECINFOCODE, DHCPv6Constants.VENDORSPECINFO);
        }
        else {
          codeToOptionMap.put(attr.getOptionEncoding(), StringUtils.fastSplit(optionCode, '.', 2)[0]);
        }
      }
      else {
        // TODO: Fehlerbehandlung
      }

      GuiAttributeOptionValuerangePair optionValuePair = new GuiAttributeOptionValuerangePair(optionCode, attr
                      .getWerteBereich());
      guiAttributesMap.put(attr.getGuiAttributeID(), optionValuePair);
    }

  }


  private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");
  public static final String sqlLoadPoolTypes = "SELECT * FROM " + com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType.TABLENAME;
  public static Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> classIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>();
  public static Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> poolIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>();


  private static void loadPoolTypes(ODSConnection con) throws PersistenceLayerException {
    // nimm gecachte Anfrage wenn moeglich
    PreparedQuery<com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> pq = (PreparedQuery<com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>) queryCache
                    .getQueryFromCache(sqlLoadPoolTypes, con, new com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType()
                                    .getReader());

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    Parameter sqlparameter = new Parameter();
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Query with String: " + sqlLoadGuiAttributes);
    }
    Collection<com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> queryResult = con.query(pq, sqlparameter, -1);

    for (com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType pt : queryResult) {
      String[] classIDs = StringUtils.fastSplit(pt.getClassIDs(), ',', 0);
      for (String classID : classIDs) {
        Matcher matcher = DIGIT_PATTERN.matcher(classID);
        if (matcher.matches()) {
          classIDtoPoolTypeMap.put(Integer.parseInt(classID), pt);
        }
      }
      poolIDtoPoolTypeMap.put(pt.getPoolTypeID(), pt);
    }

  }


  public static void loadGuiData() throws PersistenceLayerException, DHCPv6NoUniqueDppFixedAttributeException {
    logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Starting update process");
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    readWriteLock.writeLock().lock();
    // Kopien von alten Maps anlegen, auf die man im Fehlerfall zurueckgreifen kann
    Map<String, String> dppFixedAttributesMap_old = new HashMap<String, String>(dppFixedAttributesMap);
    Map<Integer, GuiFixedAttributeOptionValuePair> guiFixedAttributesMap_old = new HashMap<Integer, GuiFixedAttributeOptionValuePair>(
                                                                                                                                      guiFixedAttributesMap);
    Map<Integer, GuiAttributeOptionValuerangePair> guiAttributesMap_old = new HashMap<Integer, GuiAttributeOptionValuerangePair>(
                                                                                                                                 guiAttributesMap);
    ArrayList<DeviceClass> classList_old = new ArrayList<DeviceClass>(classList);
    Map<Integer, Conditional> parsedConditionalsMap_old = new HashMap<Integer, Conditional>(parsedConditionalsMap);
    Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> conditionsMap_old = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>(
                                                                                                                                                            conditionsMap);
    Map<String, GuiOperator> operatorsMap_old = new HashMap<String, GuiOperator>(operatorsMap);
    Map<Integer, String> parametersMap_old = new HashMap<Integer, String>(parametersMap);
    Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> classIDtoPoolTypeMap_old = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                                                                                 classIDtoPoolTypeMap);
    Map<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> poolIDtoPoolTypeMap_old = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                                                                                poolIDtoPoolTypeMap);

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";


    try {
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating DppFixedAttributes");
      loadDppFixedAttributes(con);
      if (logger.isDebugEnabled()) {
        for (Entry entry : dppFixedAttributesMap.entrySet()) {
          logger.debug("("+debugmac+") dppFixedAttribute: key = " + entry.getKey() + " value = " + entry.getValue());
        }
      }

      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating GuiFixedAttributes");
      loadGuiFixedAttributes(con);
      if (logger.isDebugEnabled()) {
        for (Entry<Integer, GuiFixedAttributeOptionValuePair> entry : guiFixedAttributesMap.entrySet()) {
          logger.debug("("+debugmac+") guiFixedAttribute: key = " + entry.getKey() + " value = optioncode " + entry.getValue()
                          .getOptionCode() + " and value " + entry.getValue().getValue());
        }
      }

      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating GuiAttributes");
      loadGuiAttributes(con);
      if (logger.isDebugEnabled()) {
        for (Entry<Integer, GuiAttributeOptionValuerangePair> entry : guiAttributesMap.entrySet()) {
          logger.debug("("+debugmac+") guiAttribute: key = " + entry.getKey() + " value = optioncode " + entry.getValue()
                          .getOptionCode() + " and value range " + entry.getValue().getValueRange());
        }
      }

      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating Classes");
      loadDeviceClassesFromDB(con);
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating ClassConditions");
      loadConditionsFromDB(con);
      if (logger.isDebugEnabled()) {
        for (Entry<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> entry : conditionsMap.entrySet()) {
          logger.debug("("+debugmac+") conditions: key = " + entry.getKey() + " value = " + entry.getValue().getName());
        }
      }
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating GuiOperators");
      loadGuiOperatorsFromDB(con);
      if (logger.isDebugEnabled()) {
        for (Entry<String, GuiOperator> entry : operatorsMap.entrySet()) {
          logger.debug("("+debugmac+") operators: key = " + entry.getKey() + " value = " + entry.getValue().getDhcpConf());
        }
      }

      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating GuiParameters");
      loadGuiParameters(con);
      if (logger.isDebugEnabled()) {
        for (Entry<Integer, String> entry : parametersMap.entrySet()) {
          logger.debug("("+debugmac+") parameters: key = " + entry.getKey() + " value = " + entry.getValue());
        }
      }

      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Updating PoolTypes");
      loadPoolTypes(con);
      if (logger.isDebugEnabled()) {
        for (Entry<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType> entry : classIDtoPoolTypeMap.entrySet()) {
          logger.debug("("+debugmac+") PoolTypes: classID = " + entry.getKey() + " poolType = " + entry.getValue().getName());
        }
      }

      logger.info("(xdnc.dhcp.UpdateDataFromGui) Updating Subnets from SuperPools");
      loadGuiSuperpoolSubnets();

      
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Received the following option codes:");
        for (Entry<String, String> entry : codeToOptionMap.entrySet()) {
          logger.debug("("+debugmac+") option code = " + entry.getKey() + " , option name = " + entry.getValue());
        }
      }

    }
    catch (PersistenceLayerException e) {
      // beim Update der Gui-Daten lief etwas schief - nimm wieder die alten Werte
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Error occured during update - fallback to former values");
      
      dppFixedAttributesMap = new HashMap<String, String>(dppFixedAttributesMap_old);
      guiFixedAttributesMap = new HashMap<Integer, GuiFixedAttributeOptionValuePair>(guiFixedAttributesMap_old);
      guiAttributesMap = new HashMap<Integer, GuiAttributeOptionValuerangePair>(guiAttributesMap_old);
      classList = new ArrayList<DeviceClass>(classList_old);
      parsedConditionalsMap = new HashMap<Integer, Conditional>(parsedConditionalsMap_old);
      conditionsMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>(conditionsMap_old);
      operatorsMap = new HashMap<String, GuiOperator>(operatorsMap_old);
      parametersMap = new HashMap<Integer, String>(parametersMap_old);
      classIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                  classIDtoPoolTypeMap_old);
      poolIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                 poolIDtoPoolTypeMap_old);

      throw new RuntimeException("Error while updating data tables: " + e);
    } catch (RuntimeException e){
   // beim Update der Gui-Daten lief etwas schief - nimm wieder die alten Werte
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Error occured during update - fallback to former values");
      
      dppFixedAttributesMap = new HashMap<String, String>(dppFixedAttributesMap_old);
      guiFixedAttributesMap = new HashMap<Integer, GuiFixedAttributeOptionValuePair>(guiFixedAttributesMap_old);
      guiAttributesMap = new HashMap<Integer, GuiAttributeOptionValuerangePair>(guiAttributesMap_old);
      classList = new ArrayList<DeviceClass>(classList_old);
      parsedConditionalsMap = new HashMap<Integer, Conditional>(parsedConditionalsMap_old);
      conditionsMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>(conditionsMap_old);
      operatorsMap = new HashMap<String, GuiOperator>(operatorsMap_old);
      parametersMap = new HashMap<Integer, String>(parametersMap_old);
      classIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                  classIDtoPoolTypeMap_old);
      poolIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                 poolIDtoPoolTypeMap_old);

      throw new RuntimeException("Error while updating data tables: " + e);
    } catch (DHCPv6InconsistentDataException e) {
      // beim Update der Gui-Daten lief etwas schief - nimm wieder die alten Werte
      logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Error occured during update - fallback to former values");
      
      dppFixedAttributesMap = new HashMap<String, String>(dppFixedAttributesMap_old);
      guiFixedAttributesMap = new HashMap<Integer, GuiFixedAttributeOptionValuePair>(guiFixedAttributesMap_old);
      guiAttributesMap = new HashMap<Integer, GuiAttributeOptionValuerangePair>(guiAttributesMap_old);
      classList = new ArrayList<DeviceClass>(classList_old);
      parsedConditionalsMap = new HashMap<Integer, Conditional>(parsedConditionalsMap_old);
      conditionsMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition>(conditionsMap_old);
      operatorsMap = new HashMap<String, GuiOperator>(operatorsMap_old);
      parametersMap = new HashMap<Integer, String>(parametersMap_old);
      classIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                  classIDtoPoolTypeMap_old);
      poolIDtoPoolTypeMap = new HashMap<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType>(
                                                                                                 poolIDtoPoolTypeMap_old);

      throw new RuntimeException("Error while updating data tables: " + e);
    }
    finally {
      readWriteLock.writeLock().unlock();
      con.closeConnection();
    }
    
    logger.info("(xdnc.dhcpv6.UpdateDataFromGui) Finished update process");

  }


  public static Container determinePooltypeAndClassOptions(List<? extends Node> inputoptions) throws DHCPv6InconsistentDataException, DHCPv6InvalidDBEntriesException, DHCPv6NoPoolTypeForClassException, DHCPv6AttributeNotFoundForClassException {
    ArrayList<Node> outputnodes = new ArrayList<Node>();
    PoolType poolType = new PoolType();

    DNSFlag dnsFlag = new DNSFlag(false);

    String debugmac ="";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputoptions);
    }

    // pruefe der Reihe nach, ob Klassen-Bedingungen erfuellt sind. Abbruch
    // sobald eine passende Klasse gefunden
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Checking class match for " + classList.size() + " registered classes");
    }
    // für Umbau der Workflows: 
    //boolean matchFound = false;
    readWriteLock.readLock().lock();
    try {

      for (DeviceClass dc : classList) {// classList enthaelt die nach
        // Prioritaeten sortierten Klassen aus der
        // DB
        Conditional parsedConditional = parsedConditionalsMap.get(dc.getClassID());
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Evaluating conditional " + parsedConditional.getConditional());
        }
        boolean match;
        try {
          match = evaluateConditionalString(parsedConditional.getParsedConditional(), parsedConditional
                          .getParsedSubconditionals(), inputoptions, new ExceptionCounter(0));
        } catch (ConditionEvaluationException e) {
          match = false;//wenn Parameter nicht im Input gefunden wurde, ist Bedingung nicht erfüllt
        }
        if (match) {
          // Pooltype wird aus dem zugehoerigen PoolType-Eintrag entnommen
          int classID = dc.getClassID();
          com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType pt = classIDtoPoolTypeMap.get(classID);
          if (pt == null) {
            throw new DHCPv6NoPoolTypeForClassException(" No PoolType configured for classID " + classID);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Setting Pool-Type to " + pt.getName());
          }
          poolType.setType(pt.getName());
          XynaProcessing.getOrderContext().setCustom2(pt.getName());

          Map<String, String> classoptionsHash = new HashMap<String, String>();
          getFixedClassAttributes(dc, classoptionsHash);// Auslesen der
          // fixedAttributes der
          // Klasse
          getClassAttributes(dc, classoptionsHash);// Auslesen der Attributes
          // der
          // Klasse
          getPoolTypeAttributes(pt, classoptionsHash);// Auslesen der
          // Pooltype-Attribute
          outputnodes = buildNodesList(classoptionsHash, debugmac);// in der Liste stehen
          // Eintraege wie z.B.
          // ("IA_NA.T1","1200")
          String doDNS = classoptionsHash.get(DHCPv6Constants.DODNS);
          if ((doDNS != null) && (doDNS.equalsIgnoreCase("true"))) {
            dnsFlag.setDoDNS(true);
          }
       // für Umbau der Workflows: 
          //matchFound = true;
          break;
        } 
      }
    }
    finally {
      readWriteLock.readLock().unlock();
    }

// für Umbau der Workflows: 
//    if (!matchFound){
//      return new Container(new XynaObjectList<Node>(outputnodes, Node.class),new NoPoolType("matchedNoPoolType") , dnsFlag);
//    }
    return new Container(new XynaObjectList<Node>(outputnodes, Node.class), poolType, dnsFlag);
  }


  private static void getPoolTypeAttributes(com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType pt,
                                            Map<String, String> classoptionsHash) throws DHCPv6InconsistentDataException, DHCPv6AttributeNotFoundForClassException {

    // fixedAttributes setzen
    String fixedAttrString = pt.getFixedAttributes();
    if (!(fixedAttrString == null)) {
      getFixedAttributes(fixedAttrString, classoptionsHash);
    }
    // attributes setzen
    String attrString = pt.getAttributes();
    getAttributes(attrString, classoptionsHash);
  }


  /**
   * Baut aus der uebergebenen HashMap eine Liste aus TypeWithValueNodes. Name des Nodes ist dabei immer der
   * HashMap-Key, value ist der HashMapValue
   */
  private static ArrayList<Node> buildNodesList(Map<String, String> classoptionsHash, String debugmac) {
    ArrayList<Node> nodelist = new ArrayList<Node>();
    
    for (Entry entry : classoptionsHash.entrySet()) {
      nodelist.add(new TypeWithValueNode((String) entry.getKey(), (String) entry.getValue()));
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Adding option " + entry.getKey() + " with value " + entry.getValue());
      }
    }
    return nodelist;
  }


  private static final Pattern ATTRIBUTEVALUE_PATTERN = Pattern.compile("\\<(.*)\\>");
  private static final Pattern HALFATTRIBUTEVALUE_PATTERN = Pattern.compile("\\<(.*)\\s*$");


  private static void getFixedClassAttributes(DeviceClass dc, Map<String, String> classoptionsHash) throws DHCPv6InconsistentDataException, DHCPv6AttributeNotFoundForClassException {

    String fixedAttrString = dc.getFixedAttributes();
    if (fixedAttrString == null) {
      return;
    }
    getFixedAttributes(fixedAttrString, classoptionsHash);

  }


  private static void getFixedAttributes(String fixedAttrString, Map<String, String> classoptionsHash) throws DHCPv6InconsistentDataException, DHCPv6AttributeNotFoundForClassException {
    if(fixedAttrString==null || classoptionsHash==null) return;
    String[] fixedAttr = StringUtils.fastSplit(fixedAttrString, ',', 0);
    
    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    for (String fixedAttrID : fixedAttr) {
      if(fixedAttrID.length()==0)continue;
      GuiFixedAttributeOptionValuePair optionValuePair = guiFixedAttributesMap.get(Integer.parseInt(fixedAttrID));
      if (optionValuePair == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") GuiFixedAttribute " + fixedAttrID + " was not present in DB when getting data");
        }
        throw new DHCPv6AttributeNotFoundForClassException("Unable to get fixed attributes");
        // TODO Fehlerbehandlung
      }
      else {
        classoptionsHash.put(optionValuePair.getOptionCode(), optionValuePair.getValue());
      }
    }
  }


  private static final Pattern DOMAINNAME_PATTERN = Pattern.compile(".*\\..*");


  private static void getAttributes(String attrString, Map<String, String> classoptionsHash) {
    if(attrString==null || classoptionsHash==null) return;
    String[] attrs = BRACKETKOMMA_PATTERN.split(attrString);

    // Bsp.:
    // 105=<\005BASIC\0011\000>,12=<2001:cafe:cafe:cafe:0000:0000:0000:1,2001:cafe:cafe:cafe:0000:0000:0000:2>,95=<2001::0>
    // split-Pattern ",": 105=<\005BASIC\0011\000>
    // 12=<2001:cafe:cafe:cafe:0000:0000:0000:1
    // 2001:cafe:cafe:cafe:0000:0000:0000:2>
    // 95=<2001::0>
    // split-Pattern: ">,"
    // -> 105=<\005BASIC\0011\000
    // ->
    // 12=<2001:cafe:cafe:cafe:0000:0000:0000:1,2001:cafe:cafe:cafe:0000:0000:0000:2
    // -> 95=<2001::0>

    for (String attr : attrs) {
      if(attr.length()==0)continue;
      String[] attrIDAndValue = StringUtils.fastSplit(attr, '=', -1);
      String attrID = attrIDAndValue[0];
      String attrValue = attrIDAndValue[1];

      // enthaelt Optioncode (z.B. IA_NA.T1), und Wertebereich (z.B. "String",
      // "{\005BASIC\0011\000}", ...)
      
      GuiAttributeOptionValuerangePair pair = guiAttributesMap.get(Integer.parseInt(attrID));
      if(pair==null)
      {
        logger.info("DB probably misconfigured. Attribute with ID "+attrID+ " not found in Attributes Table");
        continue;
      }
      String valueRange = pair.getValueRange();

      Matcher matcher = ATTRIBUTEVALUE_PATTERN.matcher(attrValue);
      if (matcher.matches()) {
        String value = matcher.group(1);
        if (valueRange.equals(DHCPv6Constants.STRING_ATTRIBUTE)) {
          // in Node werden klassische Strings als HexString transportiert
          // domainnammes, z.B. gip.local, werden im Sinne von HEX_FQDN codiert
//          if (DOMAINNAME_PATTERN.matcher(value).matches()) {
//            classoptionsHash.put(pair.getOptionCode(), encodeHEX_FQDN(value));
//          }
//          else {
          // Umwandlung nach HEX_FQDN erfolgt jetzt im Encoder
            classoptionsHash.put(pair.getOptionCode(), convertStringToHexString(value));
//          }
        } else if (valueRange.startsWith("{")){
          classoptionsHash.put(pair.getOptionCode(), encodeProvisionFlow(value));
        }
        else if(valueRange.equals(DHCPv6Constants.IPv6_ATTRIBUTE) || valueRange.equals(DHCPv6Constants.IPv6List_ATTRIBUTE)){
          classoptionsHash.put(pair.getOptionCode(), value.toLowerCase());
        }
        else {
          classoptionsHash.put(pair.getOptionCode(), value);
        }
      }
      else {
        matcher = HALFATTRIBUTEVALUE_PATTERN.matcher(attrValue);
        if (matcher.matches()) {
          String value = matcher.group(1);
          if (valueRange.equals(DHCPv6Constants.STRING_ATTRIBUTE)) {
            // in Node werden klassische Strings als HexString transportiert
            // domainnammes, z.B. gip.local, werden im Sinne von HEX_FQDN codiert
//            if (DOMAINNAME_PATTERN.matcher(value).matches()) {
//              classoptionsHash.put(pair.getOptionCode(), encodeHEX_FQDN(value));
//            }
//            else {
              classoptionsHash.put(pair.getOptionCode(), convertStringToHexString(value));
//            }
          } else if (valueRange.startsWith("{")){
            classoptionsHash.put(pair.getOptionCode(), encodeProvisionFlow(value));
          }
          else if(valueRange.equals(DHCPv6Constants.IPv6_ATTRIBUTE) || valueRange.equals(DHCPv6Constants.IPv6List_ATTRIBUTE)){
            classoptionsHash.put(pair.getOptionCode(), value.toLowerCase());
          }
          else {
            classoptionsHash.put(pair.getOptionCode(), value);
          }
        }
      }
    }
  }


  private static final Pattern PROVFLOW_PATTERN = Pattern.compile("\\\\0");
  /**
   * In der Option "Provisioning Flow" (z.B. Unteroption von 17) stehen Werte der Form \006HYBRID\0012\000
   */
  private static String encodeProvisionFlow(String value) {//Bsp.: value = \006HYBRID\0012\000
    StringBuilder result = new StringBuilder();
    result.append("0x");
    String[] nameParts = PROVFLOW_PATTERN.split(value);//parts = [06HYBRID, 012, 00]
    for (String part : nameParts) {
      if (part.length() > 1) {
        // die ersten zwei Stellen direkt übernehmen
        result.append(part.substring(0, 2));
        if (part.length() > 2) {
          byte[] nameAsBytearray = Charset.forName("US-ASCII").encode(
              part.substring(2)).array();
          String hexString = ByteUtil.toHexValue(nameAsBytearray);
          String hexPart = hexString.substring(2);// Abschneiden der
          // vornangestellten 0x
          result.append(hexPart);
        }
      }
    }

    return result.toString();
  }


  private static final Pattern BRACKETKOMMA_PATTERN = Pattern.compile(">,");


  private static void getClassAttributes(DeviceClass dc, Map<String, String> classoptionsHash) {

    String attrString = dc.getAttributes();
    if (attrString == null) {
      return;
    }
    getAttributes(attrString, classoptionsHash);

  }


  public static final char OPENINGPARANTHESIS = '(';


  /**
   * Wertet einen conditional-String aus
   * @throws ConditionEvaluationException 
   * @throws DHCPv6InvalidDBEntriesException 
   */
//  private static boolean parseAndEvaluateConditional(String conditional, List<? extends Node> inputoptions) {
//
//    Map<String, String> subConditionalHash = new HashMap<String, String>();
//    int counter = 0;
//    String resultingConditional = DeviceClass.parseConditional(conditional, subConditionalHash, counter);
//    System.out.println("Evaluation of " + resultingConditional);
//    boolean fulfilled = evaluateConditionalString(resultingConditional, subConditionalHash, inputoptions);
//    return fulfilled;
//  }


  private static boolean evaluateNOTs(String[] notParts, Map<String, String> subConditionalHash,
                                      List<? extends Node> inputoptions) throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {

    if (notParts.length > 2) {
      throw new RuntimeException("Missing AND/OR statement between NOTs in Conditional");
    }
    boolean notEvaluation = false;
    String notString = notParts[1];
    notEvaluation = evaluateConditionalString(notString, subConditionalHash, inputoptions, new ExceptionCounter(0));

    return (!notEvaluation);
  }


  private static boolean evaluateANDs(String[] andParts, Map<String, String> subConditionalHash,
                                      List<? extends Node> inputoptions) throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {
    boolean andEvaluation = true;
    for (int and = 0; and < andParts.length; and++) {
      String andString = andParts[and];
      if (!andString.equals("")) {
        andEvaluation = evaluateConditionalString(andString, subConditionalHash, inputoptions, new ExceptionCounter(0));
      }

      if (andEvaluation == false)
        break;
    }
    return andEvaluation;
  }


//  private static boolean evaluateORs(String[] orParts, Map<String, String> subConditionalHash,
//                                     List<? extends Node> inputoptions) throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {
//    boolean orEvaluation = false;
//    for (int or = 0; or < orParts.length; or++) {
//
//      String orString = orParts[or];
//      if (!orString.equals("")) {
//        orEvaluation = evaluateConditionalString(orString, subConditionalHash, inputoptions);
//      }
//
//      if (orEvaluation == true) {
//        break;
//      }
//
//    }
//    return orEvaluation;
//  }

  
  private static boolean evaluateConditionalString(String resultingConditional,
      Map<String, String> subConditionalHash, List<? extends Node> inputoptions, ExceptionCounter exceptionCounter)
      throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {

    String trimmed = resultingConditional.trim();// Abschneiden fuehrender und
    // abschliessender Leerzeichen

    String debugmac = "";
    if (logger.isDebugEnabled()) {
      debugmac = getMACfromOptions(inputoptions);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("(" + debugmac + ") Evaluating conditional string "
          + resultingConditional);
    }

    String[] orParts = OR_PATTERN.split(trimmed);

    boolean evaluation = false;
    
    if (orParts.length > 1) {
      ExceptionCounter newExceptionCounter = new ExceptionCounter(0);
      try {
        evaluation = evaluateORs(orParts, subConditionalHash, inputoptions,
            newExceptionCounter);
      } catch (ConditionEvaluationException e) {
        newExceptionCounter.incrementCounter();
      }
      if (newExceptionCounter.getCounter() == orParts.length){
        exceptionCounter.incrementCounter();
        throw new ConditionEvaluationException("None of the OR-conditions could be evaluated");
      }
    } else {
      // wenn ANDs vorhanden sind
      String[] andParts = AND_PATTERN.split(trimmed);
      if (andParts.length > 1) {
        evaluation = evaluateANDs(andParts, subConditionalHash, inputoptions);
      } else {
        // wenn NOTs vorhanden sind
        String[] notParts = NOT_PATTERN.split(trimmed);
        if (notParts.length > 1) {
          evaluation = evaluateNOTs(notParts, subConditionalHash, inputoptions);
        } else {
          // einzelne Eintraege wie (...) oder <..>
          Matcher matcherSubCond = SUBCONDITION_PATTERN.matcher(trimmed);
          Matcher matcherCond = CONDITION_PATTERN.matcher(trimmed);
          if (matcherSubCond.matches()) {// wenn Untercondition drinsteht, d.h.
          // (...)
            String subConditional = subConditionalHash.get(matcherSubCond
                .group(1));
            evaluation = evaluateConditionalString(subConditional,
                subConditionalHash, inputoptions, new ExceptionCounter(0));
          } else if (matcherCond.matches()) {
            // evaluation = Condition.evaluate(matcherCond.group(1),
            // inputoptions);
            evaluation = DHCPv6ServicesImpl.evaluateCondition(matcherCond
                .group(1), inputoptions);
          }
        }
      }
    }
    // System.out.println("evaluation of " +resultingConditional+ " = "
    // +evaluation);
    if (logger.isDebugEnabled()) {
      logger.debug("(" + debugmac + ") Evaluation of " + resultingConditional
          + " = " + evaluation);
    }
    return evaluation;
  }
  
  private static boolean evaluateORs(String[] orParts,
      Map<String, String> subConditionalHash, List<? extends Node> inputoptions, ExceptionCounter exceptionCounter)
      throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {
    
    boolean orEvaluation = false;
    ExceptionCounter newExceptionCounter = new ExceptionCounter(0);
    
    for (int or = 0; or < orParts.length; or++) {

      String orString = orParts[or];
      if (!orString.equals("")) {
        try{
        orEvaluation = evaluateConditionalString(orString, subConditionalHash,
            inputoptions, newExceptionCounter);
        } catch(ConditionEvaluationException e) {
          newExceptionCounter.incrementCounter();
        }
      }

      if (orEvaluation == true) {
        break;
      }

    }
    if (newExceptionCounter.getCounter() == orParts.length){
      exceptionCounter.incrementCounter();
      throw new ConditionEvaluationException("None of the OR-conditions could be evaluated");
    }
    return orEvaluation;
  }

  private static final Pattern NOT_OPTION_PATTERN = Pattern.compile("^\\s*NOT\\s+<\\d+>.*$");
  private static final Pattern NOT_SUBCONDITION_PATTERN = Pattern.compile("^\\s*NOT\\s+\\(\\d+\\).*$");
  private static final Pattern NOT_PATTERN = Pattern.compile("\\s*NOT\\s*");
  private static final Pattern OR_PATTERN = Pattern.compile("\\s*OR\\s*");
  private static final Pattern AND_PATTERN = Pattern.compile("\\s*AND\\s*");
  private static final Pattern CONDITION_PATTERN = Pattern.compile("\\s*<(\\s*\\d+\\s*)>\\s*");
  private static final Pattern SUBCONDITION_PATTERN = Pattern.compile("\\s*\\(\\s*(\\d+)\\s*\\)\\s*");


//  private static boolean evaluateConditionalString(String resultingConditional, Map<String, String> subConditionalHash,
//                                                   List<? extends Node> inputoptions) throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {
//
//    String trimmed = resultingConditional.trim();// Abschneiden fuehrender und
//    // abschliessender Leerzeichen
//
//    String debugmac = "";
//    if(logger.isDebugEnabled())
//    {
//      debugmac = getMACfromOptions(inputoptions);
//    }
//    
//    if (logger.isDebugEnabled()) {
//      logger.debug("("+debugmac+") Evaluating conditional string " + resultingConditional);
//    }
//
//    String[] orParts = OR_PATTERN.split(trimmed);
//
//    boolean evaluation = false;
//    if (orParts.length > 1) {
//      evaluation = evaluateORs(orParts, subConditionalHash, inputoptions);
//    }
//    else {
//      // wenn ANDs vorhanden sind
//      String[] andParts = AND_PATTERN.split(trimmed);
//      if (andParts.length > 1) {
//        evaluation = evaluateANDs(andParts, subConditionalHash, inputoptions);
//      }
//      else {
//        // wenn NOTs vorhanden sind
//        String[] notParts = NOT_PATTERN.split(trimmed);
//        if (notParts.length > 1) {
//          evaluation = evaluateNOTs(notParts, subConditionalHash, inputoptions);
//        }
//        else {
//          // einzelne Eintraege wie (...) oder <..>
//          Matcher matcherSubCond = SUBCONDITION_PATTERN.matcher(trimmed);
//          Matcher matcherCond = CONDITION_PATTERN.matcher(trimmed);
//          if (matcherSubCond.matches()) {// wenn Untercondition drinsteht, d.h.
//            // (...)
//            String subConditional = subConditionalHash.get(matcherSubCond.group(1));
//            evaluation = evaluateConditionalString(subConditional, subConditionalHash, inputoptions);
//          }
//          else if (matcherCond.matches()) {
//            // evaluation = Condition.evaluate(matcherCond.group(1),
//            // inputoptions);
//            evaluation = DHCPv6ServicesImpl.evaluateCondition(matcherCond.group(1), inputoptions);
//          }
//        }
//      }
//    }
//    // System.out.println("evaluation of " +resultingConditional+ " = "
//    // +evaluation);
//    if (logger.isDebugEnabled()) {
//      logger.debug("("+debugmac+") Evaluation of " + resultingConditional + " = " + evaluation);
//    }
//    return evaluation;
//  }


  private static final String STARTSWITH_METHODNAME = "operatorStartsWith";
  private static final String EQUALS_METHODNAME = "operatorEquals";
  private static final String NOTEQUAL_METHODNAME = "operatorNotEqual";
  private static final String CONTAINS_METHODNAME = "operatorContains";
  private static final String NOTCONTAINED_METHODNAME = "operatorNotContained";


  private static boolean evaluateCondition(String condition, List<? extends Node> inputoptions) throws ConditionEvaluationException, DHCPv6InvalidDBEntriesException {
    
    String debugmac ="";
    if(logger.isDebugEnabled())
    {
      debugmac=getMACfromOptions(inputoptions);
    }
    
    if (logger.isDebugEnabled()) {
      for (Entry<Integer, com.gip.xyna.xdnc.dhcpv6.db.storables.Condition> entry : conditionsMap.entrySet()) {
        logger.debug("("+debugmac+") conditionsMap entries: key = " + entry.getKey() + ", value = " + entry.getValue().getName());
      }

    }
    com.gip.xyna.xdnc.dhcpv6.db.storables.Condition c = conditionsMap.get(Integer.parseInt(condition));
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Evaluating condition " + condition);
    }
    if (c == null) {
      // TODO Fehlerbehandlung
      throw new RuntimeException("Couldn't evaluate condition " + condition);
    }
    
    String paramID = c.getParameter();
    String optionCode = parametersMap.get(Integer.parseInt(paramID));
    String inputValue = getInputOption(optionCode, inputoptions);
    
    String operatorID = c.getOperator();
    GuiOperator operator = operatorsMap.get(operatorID);
    if (operator == null){
      throw new DHCPv6InvalidDBEntriesException("unknown guioperator detected");
    }
    String operatorMethod = operator.getDhcpConf();
    
    
    if (inputValue == null) {// benoetigte Option zur Auswertung konnte nicht
                             // aus den Input-Options extrahiert werden ->
      // wenn benoetigte Option nicht gefunden, und Operator == notContained,
      // dann ist alles gut
      if (operatorMethod.equals(NOTCONTAINED_METHODNAME)) {
        return true;
      } else {
        // Condition nicht erfuellt
        if (logger.isDebugEnabled()) {
          logger
              .debug("Option "
                  + optionCode
                  + " could not be retrieved from input for evaluating class condition - aborting");
        }
        return false;// ist die Option nicht vorhanden, wertet sie automatisch zu false aus
//        throw new ConditionEvaluationException("Option " + optionCode
//            + " could not be retrieved from input options");
      }
    }

    
    if (operatorMethod.equals(EQUALS_METHODNAME)) {
      return operatorEquals(inputValue, c.getValue());
    }
    else if (operatorMethod.equals(NOTEQUAL_METHODNAME)) {
      return operatorNotEqual(inputValue, c.getValue());
    }
    else if (operatorMethod.equals(STARTSWITH_METHODNAME)) {
      return operatorStartsWith(inputValue, c.getValue());
    }
    else if (operatorMethod.equals(CONTAINS_METHODNAME)) {
      return operatorContains(inputValue, c.getValue());
    } else if (operatorMethod.equals(NOTCONTAINED_METHODNAME)){
      //wenn man hier hin kommt, ist die Option enthalten
      return false;
    }
    else {
      // TODO Fehlerbehandlung
      throw new DHCPv6InvalidDBEntriesException("unknown guioperator detected");
      //return false;
    }
  }


  private static boolean operatorContains(String inputValue, String value) {
    Pattern STARTS_PATTERN = Pattern.compile(".*" + Pattern.quote(value) + ".*");
    return STARTS_PATTERN.matcher(inputValue).matches();
  }


  private static boolean operatorStartsWith(String inputValue, String value) {
    Pattern STARTS_PATTERN = Pattern.compile("" + Pattern.quote(value) + ".*");
    return STARTS_PATTERN.matcher(inputValue).matches();
  }


  private static boolean operatorNotEqual(String inputValue, String value) {
    return !(inputValue.equals(value));
  }


  private static boolean operatorEquals(String inputValue, String value) {
    return inputValue.equals(value);
  }


  private static String getInputOption(String optionCode, List<? extends Node> inputoptions) {
    // optionCode kann die Form "IA_NA.T1" haben
    String[] suboptionNamesAsArray = StringUtils.fastSplit(optionCode, '.', -1);
    List<String> suboptionNames = new ArrayList<String>();
    for (String part : suboptionNamesAsArray) {
      suboptionNames.add(part);
    }
    return getInputOptionTopDown(suboptionNames, inputoptions);
  }


  /**
   * Extrahiert aus den Input-Optionen den Wert der angefragten Option. Dabei wird beginnend mit dem obersten Level nach
   * der Option gesucht, der erste Treffer wird zuruckgegeben. Bsp.: Top-Level Input-Liste: LinkAddress, RelayMessage,
   * VendorSpecificInfo gesucht: VendorClass (tritt zum Bsp. unterhalb von RelayMessage und dort unterhalb von IA_NA
   * auf) Suche: 1. VendorClass auf Top-Level nicht vertreten 2. Suche unterhalb von RelayMessage 3. VendorClass
   * unterhalb von RelayMessage vertreten TODO: iterative Suche bei Bedarf verallgemeinern
   */
  private static String getInputOptionTopDown(List<String> suboptionNames, List<? extends Node> inputoptions) {

    // suboptionNames basiert auf der Form "IA_NA.T1" haben
    int depth = suboptionNames.size();
    List<? extends Node> inputnodes;

    String debugmac = "";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputoptions);
    }

    // wenn ein TypeOnlyNode gesucht wird
    if (depth > 1) {
      inputnodes = getSubnodesOfSpecificNode(inputoptions, suboptionNames.get(0));
      if (inputnodes != null) {
        return getInputOptionTopDown(suboptionNames.subList(1, depth), inputnodes);
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Node " + suboptionNames.get(0) + " could not be retrieved from input options on this level");
        }
        // return null;// gesuchter Node war nicht vorhanden
      }
    }
    else { // TypeWithValueNode wird gesucht
      String value = getValueOfSpecificNode(inputoptions, suboptionNames.get(0));
      if (value != null) {
        return value;
      }
      else {
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Node " + suboptionNames.get(0) + " could not be retrieved from input optionson this level");
        }
      }

    }
    // Suche unterhalb von Relay Message
    // Extrahiere Nodes unterhalb der RelayMessage
    inputnodes = getSubnodesOfSpecificNode(inputoptions, DHCPv6Constants.RELAYMESSAGE);
    if (inputnodes == null) {
      return null;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Trying to retrieve node " + suboptionNames.get(0) + " out of Relay Message");
    }
    return getInputOptionTopDown(suboptionNames, inputnodes);

  }


  /**
   * Gibt den Inhalt der angegebenen TypeWithValueNode zurueck.
   */
  private static String getValueOfSpecificNode(List<? extends Node> inputnodes, String name) {

    // Sonderbehandlung fuer Optionen, die im Namen die Enterprise-Nr.
    // angehaengt haben
    // allgemein Verwendung von startsWith anstatt equals kann zu Problemen
    // fuehren, falls es Optionen gibt, die den gleichen Anfang haben:
    // Option 1: IAAddress
    // Option 2: IAAddressv4 (Bsp. entspricht nicht der Realitaet)

    // bekannte Optionen mit Enterprise-Nr.: VendorSpecificInformation,
    // VendorClass
    String debugmac = "";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputnodes);
    }

    if (name.startsWith(DHCPv6Constants.VENDORSPECINFO) || name.startsWith(DHCPv6Constants.VENDORCLASS)) {
      for (Node node : inputnodes) {
        if (node.getTypeName().startsWith(name)) {
          if (node instanceof TypeWithValueNode) {

            if (HEX_PATTERN_START.matcher(((TypeWithValueNode) node).getValue()).matches()) {
              return convertHexStringToString(((TypeWithValueNode) node).getValue());
            }
            else {
              return ((TypeWithValueNode) node).getValue();
            }
          }
          else {
            //Sonderfall VendorSpecificInformation: kann leerer TypeOnlyNode sein
            if (logger.isDebugEnabled()) {
              logger.debug("("+debugmac+") Determined TypeOnlyNode for " + name+ " - empty option");
            }
            return "EmptyData";
          }
        }
      }
    }
    else {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(name)) {
          if (node instanceof TypeWithValueNode) {

            if (HEX_PATTERN_START.matcher(((TypeWithValueNode) node).getValue()).matches()) {
              return convertHexStringToString(((TypeWithValueNode) node).getValue());
            }
            else {
              return ((TypeWithValueNode) node).getValue();
            }

          }
          else {
            if (logger.isDebugEnabled()) {
              logger.debug("("+debugmac+") Cannot get value from TypeOnlyNode " + name);
            }
            return null;
          }
        }
      }
    }


    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Requested node " + name + " not present in node list");
    }
    // throw new RuntimeException("Cannot get subnodes from TypeWithValueNode");
    // TODO: was soll dann passieren?
    return null;
  }


  private static List<? extends Node> getSubnodesOfSpecificNode(List<? extends Node> inputnodes, String name) {


    // Sonderbehandlung fuer Optionen, die im Namen die Enterprise-Nr.
    // angehaengt haben
    // allgemein Verwendung von startsWith anstatt equals kann zu Problemen
    // fuehren, falls es Optionen gibt, die den gleichen Anfang haben:
    // Option 1: IAAddress
    // Option 2: IAAddressv4 (Bsp. entspricht nicht der Realitaet)

    // bekannte Optionen mit Enterprise-Nr.: VendorSpecificInformation,
    // VendorClass

    String debugmac = macForDPPGUID.get();
    if(debugmac==null)debugmac="";

    if (name.startsWith(DHCPv6Constants.VENDORSPECINFO) || name.startsWith(DHCPv6Constants.VENDORCLASS)) {
      for (Node node : inputnodes) {
        if (node.getTypeName().startsWith(name)) {
          if (node instanceof TypeOnlyNode) {
            return ((TypeOnlyNode) node).getSubnodes();
          }
          else {
            if (logger.isDebugEnabled()) {
              logger.debug("("+debugmac+") Cannot get subnodes from TypeWithValueNode " + name);
            }
            return null;
          }
        }
      }
    }
    else {
      for (Node node : inputnodes) {
        if (node.getTypeName().equalsIgnoreCase(name)) {
          if (node instanceof TypeOnlyNode) {
            return ((TypeOnlyNode) node).getSubnodes();
          }
          else {
            if (logger.isDebugEnabled()) {
              logger.debug("("+debugmac+") Cannot get subnodes from TypeWithValueNode " + name);
            }
            return null;
          }
        }
      }
    }


    if (logger.isDebugEnabled()) {
      logger.debug("("+debugmac+") Requested node " + name + " not present in node list");
    }
    // throw new RuntimeException("Cannot get subnodes from TypeWithValueNode "
    // +name);
    // TODO: was soll dann passieren?
    return null;
  }


  public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {    
    StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[1];
    report[0] = new StatisticsReportEntryLegacy() {
      
      public Object getValue() {
        ODSConnection con = ods.openConnection();
        Long cnt = -1L;

        try {
          PreparedQuery<OrderCount> pq = (PreparedQuery<OrderCount>) queryCache.getQueryFromCache(sqlGetLeasestableCount, con, OrderCount.getCountReader());
          cnt = (long) con.queryOneRow(pq, null).getCount();
        }
        catch (PersistenceLayerException e) {
          logger.error("could not execute query for statistics: " + sqlGetLeasestableCount, e);
        } finally {
          try {
            con.closeConnection();
          }
          catch (PersistenceLayerException e) {
            logger.error(null, e);
          }
        }
        
        return cnt;
      }
      
    
      public SNMPVarTypeLegacy getType() {
        return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
      }
      
    
      public String getDescription() {
        return "Count leases in the leases table";
      }
    };
    
    return report;
  }
  
  public static class ConditionEvaluationException extends Exception {
    public ConditionEvaluationException(String string) {
      super(string);
    }
  }
  
  
  public static List<xdnc.dhcpv6.SuperPool> convertAndSortSuperPools(Collection<? extends SuperPool> input)
  {
    List<xdnc.dhcpv6.SuperPool> result = new ArrayList<xdnc.dhcpv6.SuperPool>();
    
    for(SuperPool sp:input)
    {
      if(sp.getChecksum().equals(sp.getRanges())&&sp.getChecksum().length()>0) // SuperPools mit keiner Aenderung
      {
        xdnc.dhcpv6.SuperPool neu = convertSuperPool(sp);
        result.add(neu);
      }
    }

//    for(SuperPool sp:input)
//    {
//      if(sp.getRanges().length()==0&&sp.getChecksum().length()>0) // SuperPools, die geloescht werden
//      {
//        xdnc.dhcpv6.SuperPool neu = convertSuperPool(sp);
//        result.add(neu);
//      }
//    }

    for(SuperPool sp:input)
    {
      if(!sp.getChecksum().equals(sp.getRanges())&&sp.getChecksum().length()>0) // SuperPools mit Aenderung
      {
        xdnc.dhcpv6.SuperPool neu = convertSuperPool(sp);
        result.add(neu);
      }
    }

    for(SuperPool sp:input)
    {
      if((sp.getRanges().length()>0)&&sp.getChecksum().length()==0) // SuperPools neu anlegen
      {
        xdnc.dhcpv6.SuperPool neu = convertSuperPool(sp);
        result.add(neu);
      }
    }

    return result;
  }


  private static xdnc.dhcpv6.SuperPool convertSuperPool(SuperPool sp) {
    xdnc.dhcpv6.SuperPool neu = new xdnc.dhcpv6.SuperPool();
    neu.setCfgtimestamp(sp.getCfgtimestamp());
    neu.setChecksum(sp.getChecksum());
    neu.setClusternode(sp.getClusternode());
    neu.setCmtsip(sp.getCmtsip());
    neu.setEnds(sp.getEnds());
    neu.setStartm(sp.getStartm());
    neu.setLeasecount(sp.getLeasecount());
    neu.setPooltype(sp.getPooltype());
    neu.setRanges(sp.getRanges());
    neu.setSuperpoolid(sp.getSuperpoolID());
    neu.setStatus(sp.getStatus());
    neu.setPrefixlength(sp.getPrefixlength());
    neu.setSubnets(sp.getSubnets());
    return neu;
  }
  

  private static SuperPool convertSuperPool(xdnc.dhcpv6.SuperPool sp) {
    SuperPool neu = new SuperPool();
    neu.setCfgtimestamp(sp.getCfgtimestamp());
    neu.setChecksum(sp.getChecksum());
    neu.setClusternode(sp.getClusternode());
    neu.setCmtsip(sp.getCmtsip());
    neu.setEnds(sp.getEnds());
    neu.setStartm(sp.getStartm());
    neu.setLeasecount(sp.getLeasecount());
    neu.setPooltype(sp.getPooltype());
    neu.setRanges(sp.getRanges());
    neu.setSuperpoolID(sp.getSuperpoolid());
    neu.setStatus(sp.getStatus());
    neu.setPrefixlength(sp.getPrefixlength());
    neu.setSubnets(sp.getSubnets());
    return neu;
  }

  
  public static XynaObjectList<xdnc.dhcpv6.SuperPool> sortSuperPools() throws XynaException
  {
    String sqlStatement = "Select * from "+SuperPool.TABLENAME+" where "+SuperPool.COL_CLUSTERNODE+" in ("+bindingToSelectFrom+")";
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    Parameter sqlparameter = new Parameter();
    
    
    try
    {
      Collection<? extends SuperPool> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new SuperPool().getReader(),-1);
      
      List<xdnc.dhcpv6.SuperPool> result = convertAndSortSuperPools(queryResult);
    
      return new XynaObjectList<xdnc.dhcpv6.SuperPool>(result, xdnc.dhcpv6.SuperPool.class);
    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
  }
  
  public static BigInteger randomBigInteger(BigInteger max) 
  {
    BigInteger result = null;
    long counter=0;
    while(result==null || result.compareTo(max)==1)
    {
      counter++;
      result = new BigInteger(max.bitLength(),rnd);
      if(counter>1000000)
      {
        System.out.println("Generating random BigInteger failed!");
      }
    }
    return result;
  }
  
  
  public static void computeSuperPool(List<? extends xdnc.dhcpv6.SuperPool> superpools) throws XynaException
  {
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Starting to compute SuperPools (generating leases)...");
    String binding = bindingToSelectFrom;
    int counter=0;
    String ranges = "";
    int leasecreated=0; 

    for(xdnc.dhcpv6.SuperPool sp:superpools)
    {
      counter++;
//      if(sp.getChecksum().equals(sp.getRanges()))
//      {
//        continue;
//      }
      BigInteger leasesCountInRanges = computeLeasesCountInRanges(sp.getRanges(), sp.getPrefixlength());
      
      sp.setLeasecount(leasesCountInRanges.toString());
      
      ranges = sp.getRanges();
      if(ranges.length()>50)
      {
        ranges = ranges.substring(0, 50);
      }
      logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Computing superpool "+counter+"/"+superpools.size()+" : ID "+sp.getSuperpoolid()+" : leasecount 0/"+sp.getLeasecount()+" : b1nding "+binding+" : ranges "+ranges);

      leasecreated = 0;
      
      if(leasesCountInRanges.compareTo(new BigInteger(String.valueOf(limitsmalllargepools)))< 0)
      {
        leasecreated = computeSuperPoolSmall(sp, binding);   
      }
      else 
      {
        computeSuperPoolLarge(sp, leasesCountInRanges);
      }
      logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Computing superpool "+counter+"/"+superpools.size()+" : ID "+sp.getSuperpoolid()+" : leasecount "+leasecreated+"/"+sp.getLeasecount()+" : b1nding "+binding);
    }
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Computing SuperPools (generating leases) finished...");
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Pool deployment finished! Setting status active and cleaning up leases, which are not within pools ...");

   
  }


  private static void computeSuperPoolLarge(xdnc.dhcpv6.SuperPool sp, BigInteger leasesCountInRanges) {
    
    BigInteger fiftyFiftyLimit = leasesCountInRanges.divide(new BigInteger(String.valueOf(2)));

    BigInteger sumOfLeasesCountInOneRange = BigInteger.ZERO;
    
    BigInteger prefixcount = BigInteger.valueOf(2).pow(128-sp.getPrefixlength());
    
    
    String[] rangelist = StringUtils.fastSplit(sp.getRanges(), ',', 0);
    for(int i=0;i<rangelist.length;i++)
    {
        String range = rangelist[i];
        sumOfLeasesCountInOneRange = sumOfLeasesCountInOneRange.add(computeLeasesCountInOneRange(range, sp.getPrefixlength()));
        if(sumOfLeasesCountInOneRange.compareTo(fiftyFiftyLimit)>=0)
        {
          BigInteger diff = sumOfLeasesCountInOneRange.subtract(fiftyFiftyLimit);
          
          IPv6AddressUtil endAddressInRange = IPv6AddressUtil.parse(StringUtils.fastSplit(range, '-', 3)[1]);
          
          BigInteger product = diff.multiply(prefixcount);
          
          
          
          //IPv6AddressUtil fiftyFiftyAddress = IPv6AddressUtil.minus(endAddressInRange, IPv6AddressUtil.parse(diff));//endAdressInRange-dividend+1-(diff*dividend)
          IPv6AddressUtil fiftyFiftyAddress = IPv6AddressUtil.minus(endAddressInRange, IPv6AddressUtil.parse(prefixcount));
          fiftyFiftyAddress = IPv6AddressUtil.plus(fiftyFiftyAddress, IPv6AddressUtil.parse(BigInteger.ONE));
          fiftyFiftyAddress = IPv6AddressUtil.minus(fiftyFiftyAddress, IPv6AddressUtil.parse(product));
          
          
                          
          fiftyFiftyAddress = IPv6SubnetUtil.calculateIPv6PrefixAddress(fiftyFiftyAddress, sp.getPrefixlength());
          
          //sp.setEnds(fiftyFiftyAddress.asLongString());
          sp.setEnds(fiftyFiftyAddress.asLongString());

          
          if(!endAddressInRange.equals(fiftyFiftyAddress.plus(fiftyFiftyAddress, IPv6AddressUtil.parse(prefixcount.subtract(BigInteger.ONE)))))// fiftyFiftyAddress->   fiftyFiftyAddress+dividend-1
          {
            //sp.setStartm(fiftyFiftyAddress.increment().asLongString());
            BigInteger countprefixaddresses = BigInteger.valueOf(2).pow(128-sp.getPrefixlength());
            IPv6AddressUtil prefixvalue = IPv6AddressUtil.parse(countprefixaddresses);
            sp.setStartm(fiftyFiftyAddress.plus(fiftyFiftyAddress, prefixvalue).asLongString());
          }
          else
          {
            sp.setStartm(IPv6AddressUtil.parse(StringUtils.fastSplit(rangelist[i+1], '-', 2)[0]).asLongString());//hier besser auch  IPv6SubnetUtil.calculateIPv6PrefixAddress(..., sollte aber stimmen
          }
          
          
          ODSConnection con = ods.openConnection();
          //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
          con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

          try
          {
            SuperPool storablesuperpool = convertSuperPool(sp);
            
            con.persistObject(storablesuperpool);
            con.commit();
          }
          catch(Exception e)
          {
            logger.error("",e);
          }
          finally
          {
            try
            {
              con.closeConnection();
            }
            catch (PersistenceLayerException e) {
              logger.error(null, e);
            }
          }
          
          
          
          return;
        }
    }
    
    
    
  }


  private static int computeSuperPoolSmall(xdnc.dhcpv6.SuperPool sp, String binding) throws PersistenceLayerException {
    
    int leasecounter = 0;
    boolean odd=binding.indexOf("1")>=0; 
    boolean even=binding.indexOf("2")>=0; 

    if(!odd && !even) return leasecounter; // fuer leeres Binding keine Leases generieren
    if(sp.getPooltype().contains("Reserved")) return leasecounter; // fuer Reserved keine Leases generieren
    
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    
    try
    {
      Collection<? extends Lease> leasesInRange = getLeasesInRange(sp.getSuperpoolid(),sp.getRanges(),sp.getPrefixlength());

      for(Lease l:leasesInRange)
      {
          IPv6AddressUtil ip = IPv6AddressUtil.parse(l.getIp());
          BigInteger divident = BigInteger.valueOf(2).pow(128-sp.getPrefixlength());
          
          BigInteger ipAsBigInteger = ip.asBigInteger().divide(divident);
          
          if(ipAsBigInteger.mod(new BigInteger("2")).equals(BigInteger.ZERO) && even==false)
          {
            continue;
          }
          if(ipAsBigInteger.mod(new BigInteger("2")).equals(BigInteger.ONE) && odd==false)
          {
            continue;
          }
        Parameter sqlparameter = new Parameter(l.getIp());
        String sqlStatement = "Select * from "+Lease.TABLENAME+" where ip=? for update";
        Collection<? extends Lease> exists = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
          
        if(exists.size()==0) // Lease schreiben, falls es noch nicht existiert
        {
          leasecounter++;
          con.persistObject(l);
          con.commit();
        }
        
      }
      
      
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    return leasecounter;
  }
    
    


  private static BigInteger computeLeasesCountInRanges(String rangesinput, int prefixlength) {
    String[] ranges = StringUtils.fastSplit(rangesinput, ',', 0);
    BigInteger leasesCountInRanges = BigInteger.ZERO;
    if(rangesinput.equals(""))return leasesCountInRanges;
    for(String s:ranges)
    {
      leasesCountInRanges = leasesCountInRanges.add(computeLeasesCountInOneRange(s, prefixlength));
    }
    return leasesCountInRanges;
  }


  private static BigInteger computeLeasesCountInOneRange(String s, int prefixlength) {
    BigInteger leasesCountInRanges;
    String[] startend = StringUtils.fastSplit(s, '-', -1);
    IPv6AddressUtil start = IPv6AddressUtil.parse(startend[0]);
    IPv6AddressUtil end = IPv6AddressUtil.parse(startend[1]);
    leasesCountInRanges = IPv6AddressUtil.minus(end, start).asBigInteger().add(BigInteger.ONE);
    BigInteger divident = BigInteger.valueOf(2).pow(128-prefixlength);
    leasesCountInRanges = leasesCountInRanges.divide(divident);
    return leasesCountInRanges;
  }


  private static Collection<? extends Lease> minus(Collection<? extends Lease> leasesInRange,
                                            Collection<? extends Lease> queryResult) {
    
   Collection<Lease> result = new ArrayList<Lease>();
   
   for(Lease lease:leasesInRange)
   {
     if(!in(lease,queryResult))
     {
       result.add(lease);
     }
   }
    
    
   return result; 
  }

  //BUGBUG : langsam da quadratisch
  private static boolean in(Lease lease,
                            Collection<? extends Lease> leaselist) {
    
    for(Lease l:leaselist)
    {
      if(l.equalsKey(lease)) return true;
    }
    return false;
  }


  private static Collection<? extends Lease> getLeasesInRange(long superpoolid, String ranges, int prefixlength) {
    
    BigInteger divident = BigInteger.valueOf(2).pow(128-prefixlength);
    IPv6AddressUtil dividentaddress = IPv6AddressUtil.parse(divident);
    Collection<Lease> result = new ArrayList<Lease>();
    String[] rangesArray = StringUtils.fastSplit(ranges, ',', 0); 
    if(ranges.equals(""))return result;
    for(String range:rangesArray)
    {
      String[] limitsArray = StringUtils.fastSplit(range, '-', -1);
      IPv6AddressUtil start = IPv6AddressUtil.parse(limitsArray[0]);
      IPv6AddressUtil end = IPv6AddressUtil.parse(limitsArray[1]);
      
      int counter=0;
      while(!(start.compareTo(end.increment())>=0))
      {
        counter++;
        Lease neu = new Lease(start);
        //neu.setLeaseID(XynaFactory.getInstance().getIDGenerator().getUniqueId());
        neu.setPrefixlength(prefixlength);
        neu.setSuperPoolID(superpoolid);
        neu.setMac("");
        neu.setBinding(""+bindingToBeSet); 
        result.add(neu);
        start = IPv6AddressUtil.plus(start, dividentaddress);
        if(counter==limitsmalllargepools)break;
      }
      
    }
    
    return result;
  }  


  public static void processLeases(List<? extends xdnc.dhcpv6.SuperPool> superpools) throws PersistenceLayerException
  {
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Starting to update SuperpoolID of Leases ...");
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    String binding = bindingToSelectFrom;
    
    String sqlStatement = "Select * from " + Lease.TABLENAME + " where "+Lease.COL_BINDING+" in ("+binding+")";
    Parameter sqlparameter = new Parameter();

    try {

      List<Lease> leasesToBeUpdated = new ArrayList<Lease>();

      Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter,new Lease().getReader(),-1);

      logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Got "+queryResult.size()+" Leases from Leasestable with b1nding "+binding+" ..." );

      leasesloop: for(Lease l:queryResult) {
        for(xdnc.dhcpv6.SuperPool sp:superpools) {
          boolean belongsToSuperpool=inRanges(l.getIp(), sp.getRanges());
          if(belongsToSuperpool) {
            if(l.getSuperPoolID()!=sp.getSuperpoolid()) {
              l.setSuperPoolID(sp.getSuperpoolid());
              leasesToBeUpdated.add(l);
            }
            continue leasesloop;//nothing to be done anymore for this lease
          }
        }
        // not contained in any pool, we would have continued the leasesloop
        l.setSuperPoolID(-1L);
        leasesToBeUpdated.add(l);
      }

      sqlStatement = "Select * from " + Lease.TABLENAME + " where "+Lease.COL_BINDING+" in ("+bindingToSelectFrom+") and "+Lease.COL_IP+" = ? for update";
      
      logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Updating " + leasesToBeUpdated.size() + " Leases ...");
      
      for(Lease u:leasesToBeUpdated) {
        sqlparameter = new Parameter(u.getPrimaryKey());
        Collection<? extends Lease> c = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter,new Lease().getReader());
        if (c.size()>0){
          con.persistObject(u);
        }
        con.commit();
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Updating SuperpoolID of Leases finished!");
  }
  
  
  static boolean inRanges(String ip, String ranges) {
      for(String range : StringUtils.fastSplit(ranges, ',', 0)) {
        String[] rangeParts = StringUtils.fastSplit(range, '-', -1);
        IPv6AddressUtil start = IPv6AddressUtil.parse(rangeParts[0]);
        IPv6AddressUtil end = IPv6AddressUtil.parse(rangeParts[1]);
        
        IPv6AddressUtil address = IPv6AddressUtil.parse(ip);
        BigInteger adressAsBigInt = address.asBigInteger();
        if(adressAsBigInt.compareTo(start.asBigInteger())>=0 && adressAsBigInt.compareTo(end.asBigInteger())<=0) {
          return true;
        }
      }
      return false;
  }




  public static int cleanSmallSuperPools_deleteExpiredLeases(xdnc.dhcpv6.SuperPool sp) throws XynaException
  {
    int count = 0;
    long expiredtime = System.currentTimeMillis()-expireoffset;
    String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_SUPERPOOLID+" = ? and ("+Lease.COL_EXPIRATIONTIME
        +" < ? and "+Lease.COL_EXPIRATIONTIME+" > 0) and ("+Lease.COL_RESERVATIONTIME+" < ? and "+Lease.COL_RESERVATIONTIME+" > 0)"+
        " and " + Lease.COL_BINDING + " in ("+bindingToCleanup+")";

    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter(sp.getSuperpoolid(),expiredtime, expiredtime);
    
    
    try
    {
      Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
      Collection<Lease> cleanedResult = new ArrayList<Lease>();
      
      count = queryResult.size();
      for(Lease l:queryResult)
      {
        String adjustedSqlStatement = sqlStatement+" and "+Lease.COL_IP+" = ? for update";
        sqlparameter = new Parameter(sp.getSuperpoolid(),expiredtime, expiredtime,l.getIp());
        try
        {
          if(DHCPv6ODS.queryODS(con, adjustedSqlStatement, sqlparameter,new Lease().getReader()).size()==1) // hier einzeln sperren 
          {
            Lease neu = new Lease(IPv6AddressUtil.parse(l.getIp()));
            //neu.setLeaseID(l.getLeaseID());
            neu.setPrefixlength(l.getPrefixlength());
            neu.setBinding(l.getBinding());
            neu.setSuperPoolID(l.getSuperPoolID());
            //cleanedResult.add(neu);
            con.persistObject(neu);
            con.commit();
          }
        }
        catch(RuntimeException re)
        {
          if (re.getMessage().equals("Could not lock object, invalid XC-State for locking")) 
          {
            //ok, this could happen, if another thread is accessing this  object, just ignore it
          } else {
            throw re;
          }                                       
        }
      }
      
      //con.persistCollection(cleanedResult);
      //con.commit();
    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    return count;
  }  
  

  
  public static void rebalanceSuperPools() throws XynaException
  {
    
// TODO Eventuell vorher sperren?
//    if(cleanlock==true) 
//    {
//      throw new XynaException("Deployment Workflow running: could not clean Pools!");
//    }
    
    String sqlStatement = "Select * from "+SuperPool.TABLENAME+" where "+SuperPool.COL_CLUSTERNODE+" in ("+bindingToSelectFrom+") and "+SuperPool.COL_LEASECOUNT+" < "+limitsmalllargepools;
    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter();
    
    logger.info("Starting Rebal. SuperPools... ");
    
    try
    {
      Collection<? extends SuperPool> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new SuperPool().getReader(),-1);
      for(SuperPool sp:queryResult)
      {
        BigInteger leasescount = new BigInteger(sp.getLeasecount());
          
          
        long freeLeasesThisNode = estimateFreeLeases(sp, bindingToBeSet);
        long freeLeasesOtherNode = estimateFreeLeases(sp, partnerbinding);
        
        logger.debug("rebaSuperPools: freeLeasesThisNode="+freeLeasesThisNode);
        logger.debug("rebalSuperPools: freeLeasesOtherNode="+freeLeasesOtherNode);
        
        
        if(!(freeLeasesThisNode<leasesLowWatermark))
        {
          long allLeasesInPool = leasescount.longValue(); // sollte gehen, da kleiner Pool
          long diffNodes = freeLeasesThisNode - freeLeasesOtherNode;
          logger.debug("rebalSuperPools: diffNodes="+diffNodes);
          logger.debug("rebalSuperPools: allLeasesInPool*minFreeRatio="+allLeasesInPool*minFreeRatio);
          logger.debug("rebalSuperPools: minFreeLeases="+minFreeLeases);
          logger.debug("rebalSuperPools: minDiffLeases="+minDiffLeases);
          
          if(freeLeasesOtherNode < (allLeasesInPool*minFreeRatio) && freeLeasesOtherNode < minFreeLeases && diffNodes>minDiffLeases)
          {
            logger.info("Rebal. SuperPool with Ranges "+sp.getRanges()+" ...");
            rebalanceLeases(sp, diffNodes / 2, bindingToBeSet);
          }
          else
          {
            logger.info("No Rebal. necessary for Superpool with Ranges "+sp.getRanges()+" ...");
          }

          
        }
          
          
        
      }
      logger.info("Rebal. Workflow finished!");    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }

  }

  
  private static void rebalanceLeases(SuperPool sp, long countLeases, String binding) throws XynaException {
    long currentTimeMillis = System.currentTimeMillis();
    String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_SUPERPOOLID+" = ? and "+Lease.COL_BINDING+" in ("+binding+") and "+Lease.COL_EXPIRATIONTIME+" < "+currentTimeMillis+ " and "+Lease.COL_RESERVATIONTIME+" < "+currentTimeMillis;
    
    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter(sp.getSuperpoolID());
    
    logger.debug("RebalancingSuperPools: rebalanceLeases countLeases = "+countLeases);
    
    Collection<? extends Lease> queryResult = null;
    try
    {
      queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
      int counter=0;
      for(Lease l:queryResult)
      {
        counter++;
        if(counter>countLeases)break;
        l.setBinding(String.valueOf(partnerbinding));
        getAndSetFreeIPSmallPoolsRebalancing(convertLease(l));
      }
      
    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    
  }


  private static long estimateFreeLeases(SuperPool sp, String binding) throws XynaException{

    long currentTimeMillis = System.currentTimeMillis();
    String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_SUPERPOOLID+" = ? and "+Lease.COL_BINDING+" = "+binding+" and "+Lease.COL_EXPIRATIONTIME+" < ? and "+Lease.COL_RESERVATIONTIME+" < ?";
    
    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter(sp.getSuperpoolID(),currentTimeMillis,currentTimeMillis);
    
    Collection<? extends Lease> queryResult = null;
    try
    {
      queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
      
    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }

    
    return queryResult.size();
  }


  public static void cleanSuperPools() throws XynaException
  {
    
    logger.info("(xdnc.dhcpv6.CleanAllSuperPools)Starting to cleanup leases with b1nding "+bindingToCleanup+" ...");
    long count = 0;
    String sqlStatement = "Select * from "+SuperPool.TABLENAME+" where "+SuperPool.COL_CLUSTERNODE+" in ("+bindingToCleanup+")";
    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter();
    
    
    try
    {
      Collection<? extends SuperPool> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new SuperPool().getReader(),-1);
      for(SuperPool sp:queryResult)
      {
        BigInteger leasescount = new BigInteger(sp.getLeasecount());
        if(leasescount.compareTo(new BigInteger(String.valueOf(limitsmalllargepools)))<0)
        {
          count = count + cleanSmallSuperPools_deleteExpiredLeases(convertSuperPool(sp));
        }
        else
        {
          count = count + cleanLargeSuperPools_deleteExpiredLeases(convertSuperPool(sp));
        }
        
      }
      resizeSuperPools_deleteExpiredLeases(null);
    
    
    }
    finally
    {
      try
      {
        con.closeConnection();
      }
      catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    logger.info("(xdnc.dhcpv6.CleanAllSuperPools)Finished Cleaning up "+count+" leases ...");
    

  }
  
  
  public static int cleanLargeSuperPools_deleteExpiredLeases(xdnc.dhcpv6.SuperPool sp) throws XynaException
  {
    int count = 0;
    long expiredtime = System.currentTimeMillis()-expireoffset;
    String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_SUPERPOOLID+" = ? and "+Lease.COL_EXPIRATIONTIME
                    +" < ? and "+Lease.COL_RESERVATIONTIME+" < ? and "+Lease.COL_BINDING+" in ("+bindingToCleanup+")";

    ODSConnection con = ods.openConnection();
    Parameter sqlparameter = new Parameter(sp.getSuperpoolid(),expiredtime,expiredtime);
    

    try {
      Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);

      count = queryResult.size();
      
      for(Lease l:queryResult) {
        String adjustedSqlStatement = sqlStatement+" and "+Lease.COL_IP+" = ? for update";
        sqlparameter = new Parameter(sp.getSuperpoolid(),expiredtime,expiredtime,l.getIp());
        try {
          if(DHCPv6ODS.queryODS(con, adjustedSqlStatement, sqlparameter,new Lease().getReader(), 1).size()==1) { // hier einzeln sperren  
            con.deleteOneRow(l);
            con.commit();
          }
        } catch(RuntimeException re) {
          if (re.getMessage().equals("Could not lock object, invalid XC-State for locking")) {
            //ok, this could happen, if another thread is accessing this  object, just ignore it
          } else {
            throw re;
          }                                       
        }
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    return count;
  }  

  
  public static void resizeSuperPools_deleteExpiredLeases(xdnc.dhcpv6.SuperPool sp) throws XynaException
    {
      long expiredtime = System.currentTimeMillis()-expireoffset;
      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_SUPERPOOLID+" = -1 and "+Lease.COL_EXPIRATIONTIME
                      +" < ? and "+Lease.COL_RESERVATIONTIME+" < ? and "+Lease.COL_BINDING+" in ("+bindingToCleanup+")";

      ODSConnection con = ods.openConnection();
      Parameter sqlparameter = new Parameter(expiredtime,expiredtime);
      
      
      try
      {
        Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
        
        for(Lease l:queryResult)
        {
          String adjustedSqlStatement = sqlStatement+" and "+Lease.COL_IP+" = ? for update";
          sqlparameter = new Parameter(expiredtime,expiredtime,l.getIp());
          try
          {
            if(DHCPv6ODS.queryODS(con, adjustedSqlStatement, sqlparameter,new Lease().getReader()).size()==1) // hier einzeln sperren 
            {
              con.deleteOneRow(l);
              con.commit();
            }
          }
          catch(RuntimeException re)
          {
            if (re.getMessage().equals("Could not lock object, invalid XC-State for locking")) 
            {
              //ok, this could happen, if another thread is accessing this  object, just ignore it
            } else {
              throw re;
            }                                       
          }
        }
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.error(null, e);
        }
      }
    }  
    
    

    private static  Collection<Lease> intersection(Collection<? extends Lease> queryResult,
                                     Collection<? extends Lease> leasesInRange) {
      Collection<Lease> result = new ArrayList<Lease>();
      
      for(Lease lease:queryResult)
      {
        if(in(lease,leasesInRange))
        {
          result.add(lease);
        }
      }
       
       
      return result; 
    }

    
    
    public static void setSuperPoolStatus(xdnc.dhcpv6.SuperPool sp, xdnc.dhcpv6.SuperPoolStatus status ) throws XynaException
    {
      ODSConnection con = ods.openConnection();
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

      sp.setStatus(status.getStatus());
      SuperPool neu = convertSuperPool(sp);
      try
      {
        con.persistObject(neu);
        con.commit();
      }
      finally
      {
        try
        {
          con.closeConnection();
        }
        catch (PersistenceLayerException e) {
          logger.error(null, e);
        }
      }
  }

    public static void setSuperPoolChecksum(xdnc.dhcpv6.SuperPool sp) throws XynaException
    {
      ODSConnection con = ods.openConnection();
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

      sp.setChecksum(sp.getRanges());
      SuperPool neu = convertSuperPool(sp);
      try
      {
        con.persistObject(neu);
        con.commit();
      }
      finally
      {
        try
        {
          con.closeConnection();
        }
        catch (PersistenceLayerException e) {
          logger.error(null, e);
        }
      }
  }

    public static Lease convertLease(xdnc.dhcpv6.Lease input)
    {
      Lease result = new Lease(IPv6AddressUtil.parse(input.getIp()));
      
      result.setBinding(input.getBinding());
      result.setDppInstance(input.getDppinstance());
      result.setDUIDTime(input.getDuidtime());
      result.setDynDnsZone(input.getDyndnszone());
      result.setExpirationtime(input.getExpirationtime());
      result.setHardwareType(input.getHardwaretype());
      result.setIaid(input.getIaid());
      //result.setLeaseID(input.getLeaseid());
      result.setMac(input.getMac());
      result.setPreferredLifetime(input.getPreferredlifetime());
      result.setPrefixlength(input.getPrefixlength());
      result.setCMRemoteId(input.getCmremoteid());
      result.setReservationTime(input.getReservationtime());
      result.setStartTime(input.getStarttime());
      result.setSuperPoolID(input.getSuperpoolid());
      result.setType(input.getType());
      result.setValidLifetime(input.getValidlifetime());
      result.setVendorSpecificInformation(input.getVendorspecificinformation());
      result.setCmtsip(input.getCmtsip());
      result.setCmtsrelayid(input.getCmtsrelayid());
      result.setCmtsremoteid(input.getCmtsremoteid());
      
      return result;
      
    }
    

    public static xdnc.dhcpv6.Lease convertLease(Lease input)
    {
      xdnc.dhcpv6.Lease result = new xdnc.dhcpv6.Lease();
      
      result.setBinding(input.getBinding());
      result.setDppinstance(input.getDppInstance());
      result.setDuidtime(input.getDUIDTime());
      result.setDyndnszone(input.getDynDnsZone());
      result.setExpirationtime(input.getExpirationtime());
      result.setHardwaretype(input.getHardwareType());
      result.setIaid(input.getIaid());
      //result.setLeaseid(input.getLeaseID());
      result.setMac(input.getMac());
      result.setPreferredlifetime(input.getPreferredLifetime());
      result.setPrefixlength(input.getPrefixlength());
      result.setCmremoteid(input.getCMRemoteId());
      result.setReservationtime(input.getReservationEnd());
      result.setStarttime(input.getStartTime());
      result.setSuperpoolid(input.getSuperPoolID());
      result.setType(input.getType());
      result.setValidlifetime(input.getValidLifetime());
      result.setVendorspecificinformation(input.getVendorSpecificInformation());
      result.setIp(input.getIp());
      result.setCmtsremoteid(input.getCmtsremoteid());
      result.setCmtsip(input.getCmtsip());
      result.setCmtsrelayid(input.getCmtsrelayid());
      
      return result;
      
    }


    public static xdnc.dhcpv6.Lease getAndSetLeaseByMacAndIAIDAndTime (xdnc.dhcpv6.Lease l) throws XynaException
    {

//      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_MAC+" = '"+l.getMac()+"' and "+Lease.COL_IAID+" = '"+l.getIaid()
//                      +"' and "+Lease.COL_EXPIRATIONTIME+" > "+System.currentTimeMillis()+" and "+Lease.COL_RESERVATIONTIME+" > "+System.currentTimeMillis()+" and "+Lease.COL_SUPERPOOLID+" = "+l.getSuperpoolid()+" for update";

      //neue Version vom 12.03.
      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_MAC+" = ? and "+Lease.COL_IAID+" = ? and ("+Lease.COL_EXPIRATIONTIME+" > ? or "+Lease.COL_EXPIRATIONTIME+" = 0) and "+Lease.COL_SUPERPOOLID+" = ? for update";

      Parameter sqlparameter = new Parameter(l.getMac(),l.getIaid(),System.currentTimeMillis(),l.getSuperpoolid());
      return getAndSetLeaseWithBinding(l, sqlStatement, sqlparameter);

    }

    public static xdnc.dhcpv6.Lease getAndSetLeaseByMacAndIAIDAndTimeRenew (xdnc.dhcpv6.Lease l) throws XynaException
    {

      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_MAC+" = ? and "+Lease.COL_IAID+" = ? and "+Lease.COL_EXPIRATIONTIME+" > ? and "+Lease.COL_IP+" = ? and "+Lease.COL_SUPERPOOLID+" = ? for update";

      Parameter sqlparameter = new Parameter(l.getMac(),l.getIaid(),System.currentTimeMillis(),l.getIp(),l.getSuperpoolid());
      return getAndSetLeaseWithBinding(l, sqlStatement, sqlparameter);

    }

    
    
    public static xdnc.dhcpv6.Lease getAndSetLeaseByMacAndIAID (xdnc.dhcpv6.Lease l) throws XynaException
    {

      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_MAC+" = ? and "+Lease.COL_IAID+" = ? and "+Lease.COL_BINDING+" in ("+bindingToSelectFrom+") and "+Lease.COL_SUPERPOOLID+" = ? for update";
      
      Parameter sqlparameter = new Parameter(l.getMac(),l.getIaid(),l.getSuperpoolid());
      
      if (disjoinedtimeinseconds > 0) {
      long disjoinedtimeinmilliseconds = disjoinedtimeinseconds * toMilliSec;
      sqlStatement = "Select * from " + Lease.TABLENAME + " where "
          + Lease.COL_MAC + " = ? and " + Lease.COL_IAID
          + " = ? and " + Lease.COL_EXPIRATIONTIME + " < ? and " + Lease.COL_BINDING
          + " in (" + bindingToSelectFrom + ") and " + Lease.COL_SUPERPOOLID
          + " = ? for update";
      
      sqlparameter = new Parameter(l.getMac(),l.getIaid(),disjoinedtimeinmilliseconds,l.getSuperpoolid());
      }

      return getAndSetLease(l, sqlStatement, sqlparameter);

  }

    public static xdnc.dhcpv6.Lease getAndSetLeaseByMacAndIAIDAndIPAndPrefixNoSuperPool (xdnc.dhcpv6.Lease l) throws XynaException
    {

      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_MAC+" = ? and "+Lease.COL_IAID+" = ? and "+Lease.COL_BINDING+" in ("+bindingToSelectFrom+") and "+Lease.COL_IP+" = ? and "+Lease.COL_PREFIXLENGTH+" = ? for update";

      Parameter sqlparameter = new Parameter(l.getMac(),l.getIaid(),l.getIp(),l.getPrefixlength());
      return getAndSetLease(l, sqlStatement, sqlparameter);
  }
    
    
    public static xdnc.dhcpv6.Lease getAndSetLeaseByMacAndIAIDAndIPAndPrefix (xdnc.dhcpv6.Lease l) throws XynaException
    {

      String sqlStatement = "Select * from " + Lease.TABLENAME + " where "
        + Lease.COL_MAC + " = ? and " + Lease.COL_IAID
        + " = ? and " + Lease.COL_BINDING + " in ("
        + bindingToSelectFrom + ") and " + Lease.COL_SUPERPOOLID + " = ? and " + Lease.COL_IP + " =  ? and " + Lease.COL_PREFIXLENGTH + " = ? for update";

      Parameter sqlparameter = new Parameter(l.getMac(),l.getIaid(),l.getSuperpoolid(),l.getIp(),l.getPrefixlength());
      
      if (disjoinedtimeinseconds > 0) {
        long disjoinedtimeinmilliseconds = disjoinedtimeinseconds * toMilliSec;

        sqlStatement = "Select * from " + Lease.TABLENAME + " where "
          + Lease.COL_MAC + " = ? and " + Lease.COL_IAID
          + " = ? and " + Lease.COL_EXPIRATIONTIME + " < ? and " + Lease.COL_BINDING + " in ("
          + bindingToSelectFrom + ") and " + Lease.COL_SUPERPOOLID + " = ? and " + Lease.COL_IP + " = ? and " + Lease.COL_PREFIXLENGTH + " = ? for update";

        sqlparameter = new Parameter(l.getMac(),l.getIaid(),disjoinedtimeinmilliseconds,l.getSuperpoolid(),l.getIp(),l.getPrefixlength());
        
      }
      
      return getAndSetLease(l, sqlStatement, sqlparameter);
  }


      private static xdnc.dhcpv6.Lease getAndSetLease(xdnc.dhcpv6.Lease l, String sqlStatement, Parameter sqlparameter) throws PersistenceLayerException {
       ODSConnection con = ods.openConnection();
       //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
       con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

      //Parameter sqlparameter = new Parameter();

      try
      {
        Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),1);

        if(queryResult.size()>0)
        {
          Lease currentLease = queryResult.iterator().next();
          //l.setLeaseid(currentLease.getLeaseID());
          l.setIp(currentLease.getIp());
          //l.setPrefixlength(currentLease.getPrefixlength());
          l.setSuperpoolid(currentLease.getSuperPoolID());
          if(l.getStarttime()==Long.MIN_VALUE) // von Renew zur übernahme markiert
          {
            l.setStarttime(currentLease.getStartTime());
          }
          if(l.getVendorspecificinformation()!=null && l.getVendorspecificinformation().equals(TO_BE_SET_WITH_DB_VALUE)) // von Renew zur übergabe markiert
          {
            if(currentLease.getVendorSpecificInformation()!=null)
            {
              l.setVendorspecificinformation(currentLease.getVendorSpecificInformation());
            }
            else
            {
              l.setVendorspecificinformation("");
            }
          }
          
          Lease result = convertLease(l);
          con.persistObject(result);
          con.commit();
          
        } else {
          l = null;
        }
        
      }
      finally
      {
        try
        {
          con.closeConnection();
        }
        catch (PersistenceLayerException e) {
          logger.error(null, e);
        }
      }
      return l;
    }


  private static xdnc.dhcpv6.Lease getAndSetLeaseWithBinding(xdnc.dhcpv6.Lease l, String sqlStatement,
                                                             Parameter sqlparameter) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    //Parameter sqlparameter = new Parameter();

    if (logger.isDebugEnabled()) {
      logger.debug("getAndSetLeaseWithBinding SQL: " + sqlStatement);
    }

    try {
      Collection<? extends Lease> queryResult;
      try {
        queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(), 1);
      } catch (PersistenceLayerException e) {
        if (XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(XYNAPROPERTY_ISPRIMARYSERVER)
            .equalsIgnoreCase("true")) {
          //retry im rebind-fall, weil dann beide knoten gleichzeitig die gleiche zeile angefragt haben können.
          //in anderen fällen schadet es auch nichts
          queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(), 1);
        } else {
          throw e;
        }
      }

      if (queryResult.size() > 0) {
        Lease currentLease = queryResult.iterator().next();
        //l.setLeaseid(currentLease.getLeaseID());
        l.setIp(currentLease.getIp());
        //l.setPrefixlength(currentLease.getPrefixlength());
        l.setSuperpoolid(currentLease.getSuperPoolID());
        l.setBinding(currentLease.getBinding());
        if (l.getStarttime() == Long.MIN_VALUE) // von Renew zur übernahme markiert
        {
          l.setStarttime(currentLease.getStartTime());
        }
        if (l.getVendorspecificinformation() != null
            && l.getVendorspecificinformation().equals(TO_BE_SET_WITH_DB_VALUE)) // von Renew zur übergabe markiert
        {
          if (currentLease.getVendorSpecificInformation() != null) {
            l.setVendorspecificinformation(currentLease.getVendorSpecificInformation());
          } else {
            l.setVendorspecificinformation("");
          }
        }

        Lease result = convertLease(l);
        con.persistObject(result);
        con.commit();

      } else {
        l = null;
      }

    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
    return l;
  }


    public static xdnc.dhcpv6.Lease getAndSetFreeIPLargePools (xdnc.dhcpv6.Lease l, xdnc.dhcpv6.SuperPool sp) throws XynaException
    {
      ODSConnection con = ods.openConnection();
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

      Parameter sqlparameter = new Parameter();

      try
      {
        BigInteger factor = BigInteger.valueOf(2).pow(128-sp.getPrefixlength());
        BigInteger half = new BigInteger(sp.getLeasecount()).multiply(factor).divide(new BigInteger(String.valueOf(2)));
        
        boolean free=false;
        
        IPv6AddressUtil randomip = null;
        int counter=0;
        while(!free)
        {
          counter++;
          randomip = getRandomIPFromRange(sp, half);
  
          long currentSystemTime = System.currentTimeMillis();
  
          String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_IP+" = ?";
  
          sqlparameter = new Parameter(randomip.asLongString());
          
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, sqlStatement, sqlparameter, new Lease().getReader(),-1);
          
          if(queryResult.size()==0)
          {
            free=true;
          }
          if(counter>100)throw new XynaException("Finding free IP in large SuperPool failed!");
          
        }

        l.setIp(randomip.asLongString());
        //l.setLeaseid(XynaFactory.getInstance().getIDGenerator().getUniqueId());
        //l.setPrefixlength(sp.getPrefixlength());
        l.setBinding(bindingToBeSet);
        
        Lease result = convertLease(l);
        con.persistObject(result);
        con.commit();
      }
      finally
      {
        try
        {
          con.closeConnection();
        }
        catch (PersistenceLayerException e) {
          logger.error(null, e);
        }
        
      }
      
      return l;
      
//      long currentSystemTime = System.currentTimeMillis();
//      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_EXPIRATIONTIME+" < "+currentSystemTime+" and "+Lease.COL_RESERVATIONTIME+" < "+currentSystemTime
//                      +" and "+Lease.COL_BINDING+" = "+l.getBinding()+" and "+Lease.COL_SUPERPOOLID+" = "+l.getSuperpoolid()+" for update";

//      return getAndSetLease(l, sqlStatement);

  }


    private static IPv6AddressUtil getRandomIPFromRange(xdnc.dhcpv6.SuperPool sp, BigInteger half) throws XynaException {
      BigInteger leaseindex = randomBigInteger(half);
      if(!bindingToBeSet.equals("1"))
      {
        leaseindex = leaseindex.add(half).add(BigInteger.ONE);
      }
      IPv6AddressUtil randomip = getIP(sp.getRanges(),leaseindex, sp.getPrefixlength());
      return randomip;
    }
    
    
    private static IPv6AddressUtil getIP(String rangesinput, BigInteger leaseindex, int prefixlength) throws XynaException {
      String[] ranges = StringUtils.fastSplit(rangesinput, ',', 0);
      BigInteger leasesCountInRanges = BigInteger.ZERO;
      if(rangesinput.equals("")) throw new XynaException("Large SuperPool with no Ranges!");
      
      for(String s:ranges)
      {
        BigInteger oldleasesCountInRanges = leasesCountInRanges;
        leasesCountInRanges = leasesCountInRanges.add(computeLeasesCountInOneRange(s, 128)); // hier wollen wir doch alle Leases! daher 128!
        if(leasesCountInRanges.compareTo(leaseindex)>=0)
        {
          leaseindex = leaseindex.subtract(oldleasesCountInRanges);
          IPv6AddressUtil result = IPv6AddressUtil.parse(StringUtils.fastSplit(s, '-', 2)[0]);
          result = IPv6AddressUtil.plus(result, IPv6AddressUtil.parse(leaseindex));
          return IPv6SubnetUtil.calculateIPv6PrefixAddress(result, prefixlength);
          
        }
      }
      return null;

    }


    public static xdnc.dhcpv6.Lease getAndSetFreeIPSmallPoolsRebalancing (xdnc.dhcpv6.Lease l) throws XynaException
    {

      long currentSystemTime = System.currentTimeMillis();
      String sqlStatement = "Select * from "+Lease.TABLENAME+" where "+Lease.COL_EXPIRATIONTIME+" < ? and "+Lease.COL_RESERVATIONTIME+" < ? and "+Lease.COL_BINDING+" in ("+bindingToSelectFrom+") and "+Lease.COL_SUPERPOOLID+" = ? for update";

      Parameter sqlparameter = new Parameter(currentSystemTime,currentSystemTime,l.getSuperpoolid());
      return getAndSetLease(l, sqlStatement, sqlparameter);

  }

    
    
    public static xdnc.dhcpv6.Lease getAndSetFreeIPSmallPools (xdnc.dhcpv6.Lease l) throws XynaException
    {

      long currentSystemTime = System.currentTimeMillis();
      String sqlStatement = "Select * from " + Lease.TABLENAME + " where "
        + Lease.COL_EXPIRATIONTIME + " < ? and "
        + Lease.COL_RESERVATIONTIME + " < ? and "
        + Lease.COL_BINDING + " in (" + bindingToSelectFrom + ") and "
        + Lease.COL_SUPERPOOLID + " = ? for update";

      Parameter sqlparameter = new Parameter(currentSystemTime,currentSystemTime, l.getSuperpoolid());
      
      if(disjoinedtimeinseconds >0)
      {
        long disjoinedtimeinmilliseconds = disjoinedtimeinseconds * toMilliSec;
      sqlStatement = "Select * from " + Lease.TABLENAME + " where "
          + Lease.COL_EXPIRATIONTIME + " < ? and " + Lease.COL_RESERVATIONTIME + " < ? and " + Lease.COL_BINDING + " in ("
          + bindingToSelectFrom + ") and " + Lease.COL_SUPERPOOLID + " = ? for update";
      
      sqlparameter = new Parameter(disjoinedtimeinmilliseconds,currentSystemTime,l.getSuperpoolid());

      }
      return getAndSetLease(l, sqlStatement, sqlparameter);

  }
    
    public static xdnc.dhcpv6.Lease getAndSetLeaseByIPAndFreeSmallPools (xdnc.dhcpv6.Lease l) throws XynaException
    {

      long currentSystemTime = System.currentTimeMillis();
      String sqlStatement = "Select * from " + Lease.TABLENAME + " where "
        + Lease.COL_EXPIRATIONTIME + " < ? and "
        + Lease.COL_RESERVATIONTIME + " < ? and "
        + Lease.COL_IP + " = ? and "
        + Lease.COL_BINDING + " in (" + bindingToSelectFrom + ") and "
        + Lease.COL_SUPERPOOLID + " = ? for update";

      Parameter sqlparameter = new Parameter(currentSystemTime,currentSystemTime,l.getIp(),l.getSuperpoolid());
      
      if(disjoinedtimeinseconds >0)
      {
        long disjoinedtimeinmilliseconds = disjoinedtimeinseconds * toMilliSec;
      sqlStatement = "Select * from " + Lease.TABLENAME + " where "
          + Lease.COL_EXPIRATIONTIME + " < ? and " + Lease.COL_RESERVATIONTIME + " < ? and " + Lease.COL_IP + " = ? and "
          + Lease.COL_BINDING + " in (" + bindingToSelectFrom + ") and "
          + Lease.COL_SUPERPOOLID + " = ? for update";
        
        sqlparameter = new Parameter(disjoinedtimeinmilliseconds,currentSystemTime,l.getIp(),l.getSuperpoolid());
      }
      return getAndSetLease(l, sqlStatement,sqlparameter);

  }
    
    public static Container determineSuperPool(List<? extends Node> inputoptions, PoolType pooltype) throws DHCPv6PooltypeException, XynaException, DHCPv6InvalidOptionException {
      
      //SuperPool result = null;
      String debugmac = "";
      if(logger.isDebugEnabled())
      {
        debugmac = getMACfromOptions(inputoptions);
      }

      if (pooltype.getType() == null) {
        // logger.warn("No Pooltype could be determined - probably the configuration data has not been read from DB");
        throw new DHCPv6PooltypeException(pooltype.getType(),
            "either configuration data has not been read from DB or no matching device class exists");
      }

      // Extrahiere Link-Address und MAC aus Input-Daten
      String linkAddress = getLinkAddress(inputoptions);
      String mac = getMACfromOptions(inputoptions);
      XynaProcessing.getOrderContext().setCustom3(linkAddress);// zur Anzeige im Process Monitor
      XynaProcessing.getOrderContext().setCustom0(mac);
      if (logger.isDebugEnabled()) {
        logger.debug("(" + debugmac + ") Got LinkAddress " + linkAddress
            + " in DHCPv6-Message, PoolType is " + pooltype.getType());
      }

      if (linkAddress == null) {
        logger.error("No valid LinkAddress retrieved from input options");
        throw new DHCPv6InvalidOptionException(linkAddress, null);
      }
      // Durchsuche Tabelle v6poolstable nach allen Pools, die zur LinkAddress
      // passen und den richtigen Pooltype haben
      ODSConnection conDefault = ods.openConnection();// ods.openConnection(ODSConnectionType.HISTORY);
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      conDefault.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

      try {
        // nimm gecachte Anfrage wenn moeglich
        PreparedQuery<? extends SuperPool> pq = queryCache.getQueryFromCache(
            "SELECT * FROM " + SuperPool.TABLENAME + " WHERE " + SuperPool.COL_POOLTYPE + " = ? AND " + SuperPool.COL_CMTSIP + " like ?", conDefault, new SuperPool().getReader());

        Parameter sqlparameter = new Parameter(pooltype.getType(),"%"+linkAddress+"%");
        if (logger.isDebugEnabled()) {
          logger.debug("(" + debugmac + ") Query with String: "
              + "SELECT * FROM " + SuperPool.TABLENAME + " WHERE " + SuperPool.COL_POOLTYPE + " = ? AND " + SuperPool.COL_CMTSIP + " like ?");
          logger.debug("(" + debugmac + ") Parameter: " + sqlparameter.get(0)+","+sqlparameter.get(1));
              
        }
        Collection<? extends SuperPool> queryResult = conDefault.query(pq, sqlparameter, -1);
        if (logger.isDebugEnabled()) {
          logger.debug("(" + debugmac + ") Query result length: "
              + queryResult.size());
        }

        ArrayList<xdnc.dhcpv6.SuperPool> addressPools = new ArrayList<xdnc.dhcpv6.SuperPool>();
        ArrayList<xdnc.dhcpv6.SuperPool> prefixPools = new ArrayList<xdnc.dhcpv6.SuperPool>();

        
        for(SuperPool sp:queryResult)
        {
          if(sp.getStatus() != null && sp.getStatus().equals("active"))
          {
            if (sp.getPrefixlength() == 128){
              addressPools.add(convertSuperPool(sp));
            } else {
              prefixPools.add(convertSuperPool(sp));
            }
          }
        }
        
        return new Container(new XynaObjectList<xdnc.dhcpv6.SuperPool>(addressPools, xdnc.dhcpv6.SuperPool.class),new XynaObjectList<xdnc.dhcpv6.SuperPool>(prefixPools, xdnc.dhcpv6.SuperPool.class));
      
      } 
      finally {
        conDefault.closeConnection();
      }
    
    }
    
    
  //####################################################
    // neue Methoden durch Umbau der Workflows

  public static Container determineAddressAndPrefixRequests(List<? extends Node> inputoptions) {

    ArrayList<Node> IANAs = new ArrayList<Node>();
    ArrayList<Node> IAPDs = new ArrayList<Node>();
    ArrayList<Node> IATAs = new ArrayList<Node>();

    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node inputnode : ((TypeOnlyNode) relaymsg)
                .getSubnodes()) {
          if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) {
            IANAs.add(inputnode);
          } else if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IATA)) {
            IATAs.add(inputnode);
          } else if (inputnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD)) {
            IAPDs.add(inputnode);
          }
        }
      }
    }

    return new Container(new XynaObjectList<Node>(IANAs, Node.class), new XynaObjectList<Node>(IAPDs, Node.class));
  }
  
  /**
   * Fuer alle eingehenden Adressanfragen wird iterativ geprueft, ob es einen
   * reservierten Host gibt. Alle Adressanfragen, die nicht mit einem
   * reservierten Host beantwortet werden koennen, werden als remainingIANAs an
   * die dynamische Lease-Vergabe gegeben
   * @throws XynaException 
   */
  public static Container checkReservedHosts(List<? extends Node> IANodes,
      List<? extends Node> inputoptions,
      List<? extends Node> incomingIaAddressNodes,
      List<? extends Node> outputoptionsFormerStep,
      List<? extends xdnc.dhcpv6.SuperPool> superpoolsA, PoolType pooltype,
      DNSFlag dnsFlag, LeaseTime leaseTimes,
      ReservedHost reservedHostsExistence,
      ReplyStatus replyStatusFromPreviousStep) throws XynaException {
    
  //nichts zu tun, wenn keine IAOptionen angefragt sind
    if (IANodes.size() == 0) {
      if (logger.isDebugEnabled()){
        logger.debug("No incoming requests for reserved lease allocation");
      }
      // Rueckgabe der bisherigen Ergebnisse
      return new Container(new XynaObjectList<Node>(incomingIaAddressNodes,
          Node.class), new XynaObjectList<Node>(outputoptionsFormerStep,
          Node.class), new XynaObjectList<Node>(incomingIaAddressNodes,
              Node.class), pooltype, dnsFlag, leaseTimes, reservedHostsExistence,
          replyStatusFromPreviousStep);

    }
    
    boolean doDNS = dnsFlag.getDoDNS();// hier muesste beim ersten Mal null ankommen
    
    ArrayList<Node> outputoptions = new ArrayList<Node>();
    ArrayList<Node> iaAddressNodes = new ArrayList<Node>(incomingIaAddressNodes);
    ArrayList<Node> remainingIANodes = new ArrayList<Node>();

  //bestimme, ob es sich bei den eingehenden Anfragen um IANAs oder IAPDs handelt
    boolean isIAPD = isIAPD(IANodes.get(0));
  //bei einem Renew muss die eingehende Adresse beruecksichtigt werden 
    boolean isRenew = false;
    String messagetype = determineDHCPv6MessageTypeAsString(inputoptions);
    if (messagetype.equalsIgnoreCase(DHCPv6Constants.TYPE_RENEW)) {
      isRenew = true;
    }
    
    ArrayList<xdnc.dhcpv6.SuperPool> matchingSuperpools = new ArrayList<xdnc.dhcpv6.SuperPool>();

  //Vorbereitung fuer das mergen der neuen Optionen mit den alten - Subnet-Optionen werden weiter unten ausgelesen
    Map<String, String> optionsmap = new HashMap<String, String>();
    updateOptionsmap(optionsmap, outputoptionsFormerStep);
    
    // extrahieren von Informationen aus Input
    String linkAddress = getLinkAddress(inputoptions);
    String mac = getMACfromOptions(inputoptions);// Notation: 000000002a3f
    if (mac == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Could not parse MAC address - invalid format");
      }
      throw new DHCPv6InvalidOptionException(DHCPv6Constants.LINKLAYERADDR, mac);
    }
    
    macForDPPGUID.set(mac);//wird benoetigt, um bei Bedarf Wert von Option CONFIG_FILE (oder so aehnlich) setzten zu koennen //sg: Ist das ok? Was bedeutet der Kommentar?
    
    // lies alle zur MAC gehoerigen Eintraege aus der host-Tabelle, wenn es kein
    // Renew ist - bei einem Renew kann nur fuer jede IAOption separat aus der
    // Host-Tabelle gelesen werden, da die IP-Adresse relevant ist
    ArrayList reservations;
    ArrayList<Host> reservedAddressList = new ArrayList<Host>();
    ArrayList<Host> reservedPrefixList = new ArrayList<Host>();
    Iterator<Host> iteratorToUse = null;
    if (!isRenew) {
      reservations = getReservedHostsForMacAndLinkAddress(mac,
          null, linkAddress);
      // reservations enthaelt eine Liste von Adressen und eine Liste von
      // Prefixes - Listen sind nach aufsteigender IP sortiert - koennen auch leer sein
      reservedAddressList = (ArrayList<Host>) reservations.get(0);
      reservedPrefixList = (ArrayList<Host>) reservations.get(1);
      if ((IANodes.size() > 0) && (IANodes.get(0).getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) ){
        iteratorToUse = reservedAddressList.iterator();
      } else if ((IANodes.size() > 0) && (IANodes.get(0).getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD))){
        iteratorToUse = reservedPrefixList.iterator();
      } else  if(IANodes.size()>0){
        throw new XynaException("Unsupported address option!");
      }
    }

    String ipFromRenew;
    
    outputoptions = new ArrayList<Node>(outputoptionsFormerStep);
    int prefixToUse = 0;
    
    for (Node iana : IANodes) {
      
      if (isRenew){
        ipFromRenew = getIpFromIAnode(iana);
        if (ipFromRenew == null) {
          // TODO: fachlich korrektes Verhalten
          throw new XynaException("No valid IPv6-Address given in DHCPv6-Renew with mac "+mac);
        }

        // lies alle zur MAC und IP gehoerigen Eintraege aus der host-Tabelle
        reservations = getReservedHostsForMacAndLinkAddress(mac, ipFromRenew, linkAddress);
        // Listen sind nach aufsteigender IP sortiert - koennen auch leer sein
        // im Falle eines Renews sollte maximal ein Eintrag enthalten sein
        reservedAddressList = (ArrayList<Host>) reservations.get(0);
        if ((IANodes.size() > 0) && (IANodes.get(0).getTypeName().equalsIgnoreCase(DHCPv6Constants.IANA)) ){
          iteratorToUse = reservedAddressList.iterator();
        } else if ((IANodes.size() > 0) && (IANodes.get(0).getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD))){
          iteratorToUse = reservedPrefixList.iterator();
        } else if(IANodes.size()>0){
          throw new XynaException("Unsupported address option!");
        }
      }
     
      if (iteratorToUse.hasNext()) {
        Host nextHost = iteratorToUse.next();
        
        //Auswertung, ob dynDNS fuer Host vorgegeben
        // dynDNS-Eintraege werden verundet - nur wenn ueberall true, wird dynDNS gesetzt
        doDNS = evaluateDynDNSforReservedHost(nextHost, doDNS);
        
        //vorgegebenen PoolType verwenden, wenn einer angegeben ist
        setPoolTypeAccordingToReservedHost(nextHost, pooltype);

        //Ueberpruefen, ob ip vorgegeben ist
        String assignedIp = nextHost.getAssignedIp();
        
        // keine IP gegeben, evtl. DNS-Vorgabe
        if (assignedIp.equals("") || assignedIp == null) {
          
          if (logger.isDebugEnabled()) {
            logger.debug("(" + mac + ") No assignedIP for " + mac
                + ", dynamicDnsActive = " + nextHost.getDynamicDnsActive());
          }
          //fuer diese IANA muss spaeter dynamische Lease-Vergabe erfolgen
          remainingIANodes.add(iana);
          
        } else {// es gibt assignedIp
  
          //Schaue nach, dass es einen passenden Superpool gibt
          if (isRenew && isIAPD){
            // hier muss der Superpool passend zur anfragenden IAPD gesucht werden
               prefixToUse = getPrefixLengthFromIAPDNode(iana, isRenew);
               if (prefixToUse == 0 || !Integer.toString(prefixToUse).equals(nextHost.getPrefixlength())){
                 logger.warn("Incoming prefix doesn't match reserved Prefix length - ignoring host entry!");
                 //fuer diese IA-Option muss spaeter dynamische Lease-Vergabe erfolgen
                   remainingIANodes.add(iana);
                   continue;
               }
               matchingSuperpools = getAdequateSuperPool(superpoolsA, isIAPD, prefixToUse);
          } else if (isIAPD){
            matchingSuperpools = getAdequateSuperPool(superpoolsA, isIAPD, Integer.parseInt(nextHost.getPrefixlength()));
          } else {
            matchingSuperpools = getAdequateSuperPool(superpoolsA, isIAPD, 0);
          }
          
          if (matchingSuperpools.size() == 0) {
            // Adresse wird nicht vergeben, da anscheinend eine
            // Fehlkonfiguration vorliegt - reservierte Adressen muessen sich in
            // einem entsprechenden Super-Pool befinden
            logger.warn("No corresponding pool found for reserved IP - ignoring host entry!");
          //fuer diese IA-Option muss spaeter dynamische Lease-Vergabe erfolgen
            remainingIANodes.add(iana);
            continue;
          }
          //TODO: Subnet-Optionen
          
          
          //TODO: Subnet-optionen aus superpool
          // dabei outputoptions des vorherigen Schritts mergen - beachten, dass IA-Optionen nicht ueberschrieben werden duerfen!
          outputoptions = new ArrayList<Node>(outputoptionsFormerStep);
          
       // erzeugen der IA_NA-Option
          //Bestimmen von benoetigten Optionen
          String iaid = getIaidFromIanaLikeNode(iana);
          String prefixlength = nextHost.getPrefixlength();
          int prefixlength_int = 0;
          if (prefixlength.equals("") || prefixlength == null) {
            prefixlength_int = DHCPv6Constants.IPv6ADDRESSLENGTH;
          } else {
            prefixlength_int = Integer.parseInt(prefixlength);
          }
          String typeVendorClass = getVendorClassOption(inputoptions);
          String cmremoteId = getInputOption(
              VendorSpecificInformationRemoteIDString, inputoptions);
          cmremoteId = StringUtils.fastReplace(cmremoteId, ":", "", -1).toLowerCase();
          List<String> duidinfo = getDUIDInfosfromOptions(inputoptions);
          long hardwaretype = -1;
          long duidtime = -1;
          String cmtsrelayid = getCMTSRelayID(inputoptions);
          String cmtsremoteid = getCMTSRemoteID(inputoptions);
          if (duidinfo.size() > 0)
            hardwaretype = Long.valueOf(duidinfo.get(0));
          if (duidinfo.size() > 1)
            duidtime = Long.valueOf(duidinfo.get(1));
          String dyndns = "";
          if (domainname != null && doDNS)
            dyndns = domainname;

          //Belegen des Leases
          xdnc.dhcpv6.Lease lease = null;

          lease = setReservedLeaseForHost(assignedIp,
              nextHost.getMac(), iaid, typeVendorClass, cmremoteId,
              hardwaretype, duidtime, dyndns, linkAddress,
              cmtsrelayid, cmtsremoteid, prefixlength_int, inputoptions);
          
        //Subnet-Optionen setzen - dazu ist die IP notwendig
          getSubnetSpecificOptions(lease.getIp(), lease.getSuperpoolid(), optionsmap);
          //IANA-Node bauen und zum Output dazu
          Node iaOption = buildIAoptionsForReservedHost(lease.getIp(), iana, lease.getPrefixlength());
          getLeaseTimesForReservedHost(leaseTimes);// Lease-Start-/End-Time in Format des DHCP-Adapters

          iaAddressNodes.add(iaOption);
          
          
          //Existenz reservierter Hosts vermerken
          reservedHostsExistence.addToHostExists(DHCPv6Constants.RESERVEDHOST_EXISTS);
          replyStatusFromPreviousStep = new Successful("Found reserved ip");
          iteratorToUse.remove();
        }//Ende else: es gibt assignedIP

        
        
      }// Ende if (reservedAddressIterator.hasNext())
      else
      {
        remainingIANodes.add(iana);
      }
    }// Ende for-Schleife ueber IANAs

    dnsFlag.setDoDNS(doDNS);
    
  //behalte von den Klassenoptionen nur diejenigen, die auch angefragt wurden bzw. alle, die Lease-Dauern vorgeben 
    List<String> requestedOptionsList = getRequestedParameters(inputoptions);//enthaelt eine Liste von typeEncodings, wie sie auch in der Spalte optionEncoding von guiattribute auftauchen
    Map<String, String> requestedClassoptions = filterRequestedClassoptions(inputoptions, optionsmap, requestedOptionsList );
    // requestedClassoptions enthaelt Eintraege der Form (IA_NA.T1,1000), d.h. ein reduzierter classoptionsHash
    if(logger.isDebugEnabled()) {
      logger.debug("### Length Filtered Options: "+requestedClassoptions.size());
    }
    //baut aus den Optionen MDM-Nodes, die im Workflow herausgegeben werden koennen
    outputoptions = buildNodesList(optionsmap, mac);
    // Eintraege haben die Form ("IA_NA.T1","1200"), d.h. noch nicht die verschachtelte Struktur
    
    return new Container(new XynaObjectList<Node>(remainingIANodes, Node.class),
        new XynaObjectList<Node>(outputoptions, Node.class),
        new XynaObjectList<Node>(iaAddressNodes, Node.class), pooltype, dnsFlag,
        leaseTimes, reservedHostsExistence, replyStatusFromPreviousStep);
  }
  
  
  private static List<Node> mergeAndUpdateOptions(List<? extends Node> outputoptionsPreviousStep, List<Node> newOptions) {
    // Zusammenführen von optionsListen - newOption hat Prio
    Map<String,Node> newOptionsMap = new HashMap<String, Node>();
    
    // baue eine Map für die neuen Optionen
    // IA-Adress-Nodes (IA_NA, IA_PD) sind hier nicht dabei
    for(Node n : newOptions) {
      newOptionsMap.put(((TypeWithValueNode) n).getTypeName(), n);
    }
    
    List<Node> returnOptions= new ArrayList<Node>();
    
    // Update bestehender Optionen
    for(Node n : outputoptionsPreviousStep) {
      // gibts ein Update für n ?
      if(newOptionsMap.containsKey(n.getTypeName())) {
        // ... dann nimm den entsprechenden Node aus der newOptions-Liste
        returnOptions.add(newOptionsMap.get(n.getTypeName()));
        
        // ... und nehm den Node aus der Liste der neuen raus 
        newOptionsMap.remove(n.getTypeName());
      } else {
        // ... sonst behalte den alten bei
        returnOptions.add(n);
      }
    }
    
    // returnOptions ist jetzt eine upgedatete Kopie von optionsList
    // hänge noch neue Nodes (==die Reste in newOptionsMap) an
    
    for(String k : newOptionsMap.keySet()) {
      returnOptions.add(newOptionsMap.get(k));
    }
    
    return returnOptions;
  }
  

  public static Container setPoolTypeOptions(List<? extends Node> inputoptions,
      List<? extends Node> outputoptions, PoolType poolType,
      ReplyStatus replyStatusFromPreviousStep) throws XynaException {
    
    try{
      //PoolType-Optionen muessen auf jeden Fall gesetzt werden
//    if(!(replyStatusFromPreviousStep instanceof Successful))
//      return new Container(new XynaObjectList<Node>(outputoptions, Node.class), poolType, new SubOptionsNotConfigured(), replyStatusFromPreviousStep);

    
    String debugmac ="";
    if(logger.isDebugEnabled())
    {
      debugmac = getMACfromOptions(inputoptions);
      logger.debug("MAC identified : "+debugmac);
    }
    
    ConfigFileGeneratorFlag cfgGenFlag = null;
    
    Map<String, String> classoptionsHash = new HashMap<String, String>();
   
    
    List<Node> outputnodes = new ArrayList<Node>();
    getPoolTypeAttributes(poolType, classoptionsHash);
    
    List<String> requestedOptionsList = getRequestedParameters(inputoptions);
    Map<String, String> requestedClassoptions = filterRequestedClassoptions(inputoptions, classoptionsHash, requestedOptionsList );
    
    // Pooltype-Attribute              
    outputnodes = buildNodesList(requestedClassoptions, debugmac);// in der Liste stehen
    
    // ### Zusammenführen neuer Optionen mit denen aus vorherigen Schritten  
    outputnodes = mergeAndUpdateOptions(outputoptions, outputnodes);

    
    return new Container(new XynaObjectList<Node>(outputnodes, Node.class), poolType, cfgGenFlag, replyStatusFromPreviousStep);
  
    } catch(Exception e) {
      //writeNegativeLogMessageWithReason(inputoptions, new Failed(e.getMessage()));
      throw new XynaException("Exception during Step SetPooltypeOptions",e);

    }    
   }
  
  
  private static String getIaidFromIanaLikeNode(Node inputnode){
    return ((TypeWithValueNode)((TypeOnlyNode) inputnode).getSubnodes().get(0)).getValue();
  }
  
    
    private static xdnc.dhcpv6.Lease setReservedLeaseForHost(String ip,
      String mac, String iaid, String typeVendorClass, String cmremoteId,
      long hardwaretype, long duidtime, String dyndns, String linkAddress,
      String cmtsrelayid, String cmtsremoteid, int prefixlength_int,
      List<? extends Node> inputoptions)
      throws XynaException {

    xdnc.dhcpv6.Lease lease = null;
    
    long expirationTimeToUse = leasetimeforstaticips;//schon in ms
    String messageType = determineDHCPv6MessageTypeAsString(inputoptions);
    if (messageType.equalsIgnoreCase(DHCPv6Constants.TYPE_SOLICIT)){
      expirationTimeToUse = defaultReservationTime;
    }
    
      ODSConnection con = ods.openConnection();
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


      Collection<? extends Lease> reservedLease = DHCPv6ODS.queryODS(
          con, "SELECT * FROM leasestable WHERE " + Lease.COL_IP
              + " = '" + ip + "'", new Parameter(),
          new Lease().getReader(),-1);

      if (reservedLease.size() > 0) {
        lease = convertLease(reservedLease.iterator().next());
      }

      try {
        if (lease == null) {
          // Fall: reservierte IP war noch nie vergeben und ist
          // daher nicht in leasestable vorhanden
          // dann: drawSpecificLeaseFromInterval
          lease = new xdnc.dhcpv6.Lease();
//          lease.setLeaseid(XynaFactory.getInstance()
//              .getIDGenerator().getUniqueId());
          //TODO: wo kommt in diesem Fall die Superpool-ID her?
        }

        lease.setIp(ip);
        lease.setMac(mac);
        lease.setIaid(iaid);
        lease.setType(typeVendorClass);
        lease.setCmremoteid(cmremoteId);
        lease.setDppinstance(myname);
        lease.setPreferredlifetime(reservationpreferredlifetime);
        // long leasetimeinseconds = reservationpreferredlifetime /
        // 1000L;
        lease.setValidlifetime(reservationpreferredlifetime);
        lease.setHardwaretype(hardwaretype);
        lease.setDuidtime(duidtime);
        lease.setDyndnszone(dyndns);
        lease.setCmtsip(linkAddress);
        lease.setCmtsrelayid(cmtsrelayid);
        lease.setCmtsremoteid(cmtsremoteid);
        lease.setPrefixlength(prefixlength_int);
        lease.setExpirationtime(System.currentTimeMillis()
            + expirationTimeToUse);
        lease.setStarttime(System.currentTimeMillis());
        lease.setBinding(bindingToBeSet);
        lease.setReservationtime(System.currentTimeMillis()
            + defaultReservationTime);

        setVendorSpecificOptions(inputoptions, lease);

        con.persistObject(convertLease(lease));
        con.commit();
        if (logger.isDebugEnabled())
          logger.debug("Adding Element with mac "
              + lease.getMac() + ".");

      } catch (Exception e) {
        throw new XynaException(
            "Problems creating or getting reserved Lease!", e);
      } finally {
        con.closeConnection();
      }
      
      return lease;
    }
    
    

    private static void getSubnetSpecificOptions(String ip, long superpoolId, Map<String, String> classoptionsHash) throws DHCPv6InconsistentDataException, DHCPv6AttributeNotFoundForClassException {
      
      List<Node> options = new ArrayList<Node>();
      SubnetConfig cfg = null;
      
      // finde passendes Subnetz
      if(superpoolToSubnetsMap!=null && superpoolToSubnetsMap.get(superpoolId)!=null)
      for(SubnetConfig snCfg : superpoolToSubnetsMap.get(superpoolId)) {
        cfg = snCfg;
        if(snCfg.getSubnetForIp(ip)!=null) {
          break;
        } else {
          cfg=null;
        }
      }
      
      if(cfg!=null) {     
        
        // setze fixed Attributes
        
        List<Node> tmpList = new ArrayList<Node>();
        //tmpList.add(new TypeWithValueNode(DHCPv6Constants.CLIENT_HW_ADDR, macForDPPGUID.get()));
        getFixedAttributes(cfg.getFixedAttributesString(), classoptionsHash);
        getAttributes(cfg.getAttributesString(), classoptionsHash);
        
        
//        for(Integer a : cfg.getFixedAttributes()) {
//          GuiFixedAttributeOptionValuePair ovPair = guiFixedAttributesMap.get(a);
//          if(ovPair!=null) {
//            options.add(new TypeWithValueNode(ovPair.getOptionCode(), ovPair.getValue()));
//          }
//        }
        
        // setze attributes
      
      
      }
    }
    
    private static final Pattern PATTERN_BRACKET_KOMMA_BRACKET = Pattern.compile(">,<");
    
    private static void loadGuiSuperpoolSubnets() throws PersistenceLayerException {
      
      ODSConnection conDefault = ods.openConnection();
      //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
      conDefault.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


      try {
       // nimm gecachte Anfrage wenn moeglich
          PreparedQuery<SuperPool> pq = (PreparedQuery<SuperPool>) queryCache
                          .getQueryFromCache(sqlLoadGuiSuperpools, conDefault, new SuperPool().getReader());
          
      //    PreparedQuery<? extends SuperPool> pq = queryCache.getQueryFromCache(
      //    "SELECT * FROM " + SuperPool.TABLENAME + " WHERE " + SuperPool.COL_POOLTYPE + " = ? AND " + SuperPool.COL_CMTSIP + " like '%"+giAddress+"%'", conDefault, new SuperPool().getReader());
      //                                                                     
      //    
          
          String debugmac = macForDPPGUID.get();
          if (debugmac == null)
            debugmac = "";
          
          Parameter sqlparameter = new Parameter();
          if (logger.isDebugEnabled()) {
            logger.debug("(" + debugmac + ") Query with String: " + sqlLoadGuiSuperpools);
          }
          Collection<SuperPool> queryResult = conDefault.query(pq, sqlparameter, -1);

      
          for(SuperPool superpool : queryResult) {
            
            if(logger.isDebugEnabled())
              logger.debug("processing SuperPool "+ superpool.getSuperpoolID());
            
            List<SubnetConfig> snCfg = new ArrayList<SubnetConfig>();
            String[] parts = PATTERN_BRACKET_KOMMA_BRACKET.split(StringUtils.fastReplace(superpool.getSubnets(), " ", "", -1));
            for(String subnet : parts) {
              if(logger.isDebugEnabled())
                logger.debug("SuperPool "+ superpool.getSuperpoolID() + " : " + subnet);
              try {
                snCfg.add(new SubnetConfig(subnet));
              } catch(ArrayIndexOutOfBoundsException e) {
                logger.debug("Invalid subnet pattern (superpool id : "+superpool.getSuperpoolID()+"): "+subnet);
              }
            }
            
            superpoolToSubnetsMap.put(superpool.getSuperpoolID(), snCfg);
          }
      } finally {
        conDefault.closeConnection();
      }
    }

//private static HostEntryV6 convertDBHostToMDMHostEntry(Host dbHost) {
//  HostEntryV6 mdmHostEntry = new HostEntryV6();
//  mdmHostEntry.setAgentRemoteId(dbHost.getAgentRemoteId());
//  mdmHostEntry.setAssignedIp(dbHost.getAssignedIp());
//  mdmHostEntry.setAssignedPoolId(dbHost.getAssignedPoolID());
//  mdmHostEntry.setCmtsip(dbHost.getCmtsip());
//  mdmHostEntry.setConfigDescr(dbHost.getConfigDescr());
//  mdmHostEntry.setDesiredPoolType(dbHost.getDesiredPoolType());
//  mdmHostEntry.setDynamicDnsActive(dbHost.getDynamicDnsActive());
//  mdmHostEntry.setHostName(dbHost.getHostName());
//  mdmHostEntry.setMac(dbHost.getMac());
//  mdmHostEntry.setPrefixlength(dbHost.getPrefixlength());
//  mdmHostEntry.setSubnetOfPool(dbHost.getSubnetOfPool());
//  
//  return mdmHostEntry;
//}

  public static Container determinePoolTypeAndSetDeviceClassOptions(
      List<? extends Node> inputoptions)
      throws DHCPv6InconsistentDataException, DHCPv6InvalidDBEntriesException,
      DHCPv6NoPoolTypeForClassException,
      DHCPv6AttributeNotFoundForClassException {
  ArrayList<Node> outputnodes = new ArrayList<Node>();
  PoolType poolType = new PoolType();

  DNSFlag dnsFlag = new DNSFlag(false);
  //TODO: muss in diesem Schritt schon das ConfigFileGeneratorFlag beruecksichtigt werden?

  String debugmac ="";
  if(logger.isDebugEnabled())
  {
    debugmac = getMACfromOptions(inputoptions);
  }

  // pruefe der Reihe nach, ob Klassen-Bedingungen erfuellt sind. Abbruch
  // sobald eine passende Klasse gefunden
  if (logger.isDebugEnabled()) {
    logger.debug("("+debugmac+") Checking class match for " + classList.size() + " registered classes");
  }

  readWriteLock.readLock().lock();
  try {

    for (DeviceClass dc : classList) {// classList enthaelt die nach
        // Prioritaeten sortierten Klassen aus der DB
      Conditional parsedConditional = parsedConditionalsMap.get(dc.getClassID());
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") Evaluating conditional " + parsedConditional.getConditional());
      }
      boolean match;
      try {
        match = evaluateConditionalString(parsedConditional.getParsedConditional(), parsedConditional
                        .getParsedSubconditionals(), inputoptions, new ExceptionCounter(0));
      } catch (ConditionEvaluationException e) {
        match = false;//wenn Parameter nicht im Input gefunden wurde, ist Bedingung nicht erfüllt
      }
      if (match) {
        // Pooltype wird aus dem zugehoerigen PoolType-Eintrag entnommen
        int classID = dc.getClassID();
        com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType pt = classIDtoPoolTypeMap.get(classID);
        if (pt == null) {
          throw new DHCPv6NoPoolTypeForClassException(" No PoolType configured for classID " + classID);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("("+debugmac+") Setting Pool-Type to " + pt.getName());
        }
        poolType.setType(pt.getName());
        poolType.setAttributes(pt.getAttributes());
        poolType.setFixedAttributes(pt.getFixedAttributes());
        XynaProcessing.getOrderContext().setCustom2(pt.getName());

        Map<String, String> classoptionsHash = new HashMap<String, String>();
        getFixedClassAttributes(dc, classoptionsHash);// Auslesen der fixedAttributes der Klasse
        getClassAttributes(dc, classoptionsHash);// Auslesen der Attributes der Klasse
          // PoolType-Attributes werden erst in einem spaeteren Schritt gesetzt,
          // da sich der PoolType ggf. durch reservedHosts noch aendert
          // getPoolTypeAttributes(pt, classoptionsHash);
        
        //behalte von den Klassenoptionen nur diejenigen, die auch angefragt wurden bzw. alle, die Lease-Dauern vorgeben 
        List<String> requestedOptionsList = getRequestedParameters(inputoptions);//enthaelt eine Liste von typeEncodings, wie sie auch in der Spalte optionEncoding von guiattribute auftauchen
        Map<String, String> requestedClassoptions = filterRequestedClassoptions(inputoptions, classoptionsHash, requestedOptionsList );
        // requestedClassoptions enthaelt Eintraege der Form (IA_NA.T1,1000), d.h. ein reduzierter classoptionsHash
        
        if(logger.isDebugEnabled()) {
          logger.debug("### Length Filtered Options: "+requestedClassoptions.size());
        }
        
        //baut aus den Optionen MDM-Nodes, die im Workflow herausgegeben werden koennen
        outputnodes = buildNodesList(classoptionsHash, debugmac);
        // Eintraege haben die Form ("IA_NA.T1","1200"), d.h. noch nicht die verschachtelte Struktur
        
        String doDNS = classoptionsHash.get(DHCPv6Constants.DODNS);
        if ((doDNS != null) && (doDNS.equalsIgnoreCase("true"))) {
          dnsFlag.setDoDNS(true);
        }
        break;//passende Klasse wurde gefunden
      } 
    }
  }
  finally {
    readWriteLock.readLock().unlock();
  }

  ReplyStatus replyStatus = new NoIpAssigned("No IP assigned yet");
  ArrayList<Node> iaAddressNodes = new ArrayList<Node>();
  return new Container(new XynaObjectList<Node>(iaAddressNodes, Node.class),
        new XynaObjectList<Node>(outputnodes, Node.class), poolType, dnsFlag, new LeaseTime(), new ReservedHost(new ArrayList<Integer>()), replyStatus);
}

private static ArrayList<String> getRequestedParameters(List<? extends Node> inputoptions) {
  
  ArrayList<String> returnlist = new ArrayList<String>();
  String octstring = null;
  
  for (Node node : inputoptions) {
    if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
      for (Node subnode : ((TypeOnlyNode) node).getSubnodes()) {
        if (subnode.getTypeName().equalsIgnoreCase(DHCPv6Constants.REQUESTLIST)) {
          octstring = ((TypeWithValueNode) subnode).getValue();
          if (!HEX_PATTERN_START.matcher(octstring).matches()) { 
            if (logger.isInfoEnabled()){
              logger.info("Invalid RequestList in input options - skipping requested options");
            }
          }
          break;
        }//Ende if requestlist
      }//Ende inneres for
    }//Ende if relaymessage
  }//Ende aeusseres for
  
  if(octstring==null)return returnlist;
  String values = octstring.substring(2);
  for (int i = 0; i <= values.length() - 4; i = i + 4) {
    if (logger.isDebugEnabled()) {
      logger
                      .debug("option code " + String.valueOf(Integer.parseInt(values.substring(i, i + 4),
                                                                                 16)) + " (index i = " + i + ", substring = " + values
                                      .substring(i, i + 4) + ")");
    }
    String toadd = String.valueOf(Integer.parseInt(values.substring(i, i + 4), 16));
    if (toadd.equals(DHCPv6Constants.IANACODE) || toadd.equals(DHCPv6Constants.IATACODE) || toadd
                    .equals(DHCPv6Constants.IAPDCODE)) {

    }
    else {
      returnlist.add(toadd);
    }
    // returnlist.add(String.valueOf(Integer.parseInt(values.substring(
    // i, i + 4), 16)));
  }
  
  // Falls "MUST Options" nicht in der Liste ist => anhaengen; dies entspricht
    // allen Optionen, die Lease-Dauern enthalten koennen
    for (String s : DHCPV6_MUST_OPTIONS) {
      if (!returnlist.contains(s)) {
        returnlist.add(s);
      }
    }

  return returnlist;

}

  /**
   * Die zurueckgegebene Liste enthaelt Eintraege der Form [(IA_NA.T1,1000),...]
   */
  private static Map<String, String> filterRequestedClassoptions(
      List<? extends Node> inputoptions, Map<String, String> classoptionsHash,
      List<String> requestedOptionsList) {
    // classoptionsHash beinhaltet Eintaege der Form (IA_NA.T1,1000)
    // requestedOptionsList ist eine Liste der angefragten optionEncodings, z.B. (3,17,...)

    Map<String, String> filteredMap = new HashMap<String, String>();

    for (String optionCode : requestedOptionsList) {
      String optionName = codeToOptionMap.get(optionCode);

      if (optionName == null)
        continue;

      // finde passende optionen unter den classoptionHash - keys
      List<String> matchingKeys = new ArrayList<String>();

      // Menge aller Optionen (keys)
      Set<String> cok = classoptionsHash.keySet();

      // Sonderbehandlungsloop
      for (String s : cok) {
        if (s.startsWith(optionName)) {

          if (logger.isDebugEnabled()) {
            logger.debug("Adding Option Option " + s);
          }

          // Option "s" ist gefordert - also anhaengen
          matchingKeys.add(s);
        }
      }

      // filtere geforderte Optionen aus der Menge aller Optionen
      for (String s : matchingKeys) {
        filteredMap.put(s, classoptionsHash.get(s));

        if (logger.isDebugEnabled())
          logger.debug("Appending option " + optionCode + " : " + optionName
              + " / " + s);

      }
    }

    if (logger.isDebugEnabled()) {
      StringBuffer sb = new StringBuffer();
      for (String key : filteredMap.keySet()) {
        sb.append(key + " ");
      }
      logger.debug("### filtered List : " + sb.toString());
    }
    return filteredMap;
  }

  
  
  
  private static void useNextHost(Host nextHost, boolean doDNS, String mac, PoolType poolType){

    //Auswertung, ob dynDNS fuer Host vorgegeben
    // dynDNS-Eintraege werden verundet - nur wenn ueberall true, wird dynDNS gesetzt
    doDNS = evaluateDynDNSforReservedHost(nextHost, doDNS);
    
    //vorgegebenen PoolType verwenden, wenn einer angegeben ist
    setPoolTypeAccordingToReservedHost(nextHost, poolType);

    
    String assignedIp = nextHost.getAssignedIp();
    
    // keine IP gegeben, evtl. DNS-Vorgabe
    if (assignedIp.equals("") || assignedIp == null) {
      
      if (logger.isDebugEnabled()) {
        logger.debug("(" + mac + ") No assignedIP for " + mac
            + ", dynamicDnsActive = " + nextHost.getDynamicDnsActive());
      }
      //fuer diese IANA muss spaeter dynamische Lease-Vergabe erfolgen
      
      
    } else {// es gibt assignedIp
      
      //superpool heraussuchen, zu dem 
      
      
    }
  }
  
  private static void setPoolTypeAccordingToReservedHost(Host nextHost,PoolType poolType) {

    com.gip.xyna.xdnc.dhcpv6.db.storables.PoolType pt = null;
    readWriteLock.readLock().lock();
    try {
      if (nextHost.getDesiredPoolType() != 0){
        pt = poolIDtoPoolTypeMap.get(nextHost.getDesiredPoolType());
      }
    } finally {
      readWriteLock.readLock().unlock();
    }
    if (pt != null){
      poolType.setType(pt.getName());
      poolType.setAttributes(pt.getAttributes());
      poolType.setFixedAttributes(pt.getFixedAttributes());
    }
    //PoolType-Optionen werden erst spaeter gesetzt
    
  }

  private static boolean evaluateDynDNSforReservedHost(Host nextHost, boolean doDNS){
 // dynDNS-Eintraege werden verundet - nur wenn ueberall true, wird dynDNS gesetzt - initial muss doDNS auf null gesetzt sein
    if ((doDNS != false) && (nextHost.getDynamicDnsActive() != null) && (nextHost.getDynamicDnsActive().equals("true"))) {
      return true;
    } else {
      return false;
    }
  }
  
  public static Container dynamicLeaseAllocation(
      List<? extends Node> remainingIaOptions,
      List<? extends Node> inputoptions,
      List<? extends Node> incomingIaAddressNodes,
      List<? extends Node> outputoptionsFormerStep,
      List<? extends xdnc.dhcpv6.SuperPool> superpools, PoolType pooltype,
      DNSFlag dnsFlag, LeaseTime leaseTimes,
      ReservedHost reservedHostsExistence,
      ReplyStatus replyStatusFromPreviousStep) throws XynaException {

    //nichts zu tun, wenn keine iaOptionen uebrig sind
    if (remainingIaOptions.size() == 0) {
      if (logger.isDebugEnabled()){
        logger.debug("No incoming requests for dynamic lease allocation");
      }
      // Rueckgabe der bisherigen Ergebnisse
      return new Container(new XynaObjectList<Node>(incomingIaAddressNodes,
          Node.class), new XynaObjectList<Node>(outputoptionsFormerStep,
          Node.class), dnsFlag, leaseTimes, reservedHostsExistence,
          replyStatusFromPreviousStep);
    }
    
    ReplyStatus newReplyStatus = replyStatusFromPreviousStep;
    
    ArrayList<Node> iaAddressNodes = new ArrayList<Node>(incomingIaAddressNodes);
    ArrayList<Node> outputoptions = new ArrayList<Node>();
    
    //bestimme, ob es sich bei den eingehenden Anfragen um IANAs oder IAPDs handelt
    boolean isIAPD = isIAPD(remainingIaOptions.get(0));
    
  //bei einem Renew muss die eingehende Adresse beruecksichtigt werden 
    boolean isRenew = false;
    //try {
      String messagetype = determineDHCPv6MessageTypeAsString(inputoptions);
      if (messagetype.equalsIgnoreCase(DHCPv6Constants.TYPE_RENEW) || messagetype.equalsIgnoreCase(DHCPv6Constants.TYPE_REBIND)){
        isRenew = true;
      }
//    } catch (DHCPv6_InvalidMessageTypeException e) {
//      throw new RuntimeException("This should never happen!");
//    }
      
    //zu Debug-Zwecken
      if (messagetype.equals(DHCPv6Constants.TYPE_REQUEST)){
        logger.debug("is Request");
      }
   
    
    //sortiere die eingehenden IAOptions in eine Map (iaid, Node)
    Map<String,Node> remainingIaOptionsMap = new HashMap<String, Node>();
    for (Node iaNode : remainingIaOptions){
      remainingIaOptionsMap.put(getIaidFromIanaLikeNode(iaNode), iaNode);
    }
    
    //extrahiere aus Input benoetigte Informationen
    String mac = getMACfromOptions(inputoptions);// Notation: 000000002a3f
    if (mac == null) {
      if (logger.isInfoEnabled()) {
        logger.info("Could not parse MAC address - invalid format");
      }
      throw new DHCPv6InvalidOptionException(DHCPv6Constants.LINKLAYERADDR, mac);
    }
    String linkAddress = getLinkAddress(inputoptions);
    String cmtsrelayid = getCMTSRelayID(inputoptions);
    String cmtsremoteid = getCMTSRemoteID(inputoptions);
    List<String> duidinfo = getDUIDInfosfromOptions(inputoptions);
    long hardwaretype = -1;
    long duidtime = -1;
    if (duidinfo.size() > 0)
      hardwaretype = Long.valueOf(duidinfo.get(0));
    if (duidinfo.size() > 1)
      duidtime = Long.valueOf(duidinfo.get(1));
    // extrahiere VendorClass (type), VendorSpecInfo.1026 (remoteId) aus
    // Input-Nodes; Option 17 wird derzeit nicht gesetzt
    String typeVendorClass = getVendorClassOption(inputoptions);
    String remoteId = getInputOption(VendorSpecificInformationRemoteIDString,
        inputoptions);
    if (remoteId != null){
      remoteId = StringUtils.fastReplace(remoteId, ":", "", -1).toLowerCase();
    }
    
    // Bestimme Super-Pool, aus dem die Leases vergeben werden sollen
    //je nach eingehender Anfrage (IANA oder IAPD) muss der passende Super-Pool mit oder ohne Prefix-Angabe bestimmt werden
    // im Renew-Fall muss fuer jede IAPD separat der passende Super-Pool bestimmt werden
    ArrayList<xdnc.dhcpv6.SuperPool> matchingSuperpools = new ArrayList<xdnc.dhcpv6.SuperPool>();
    if (!(isRenew && isIAPD)) {
      matchingSuperpools = getAdequateSuperPool(superpools, isIAPD, 0);
      if (matchingSuperpools.size() == 0) {
        if (logger.isDebugEnabled()) {
          logger.debug("No superpool found");
        }
        // Rueckgabe der bisherigen Ergebnisse
        return new Container(new XynaObjectList<Node>(incomingIaAddressNodes,
            Node.class), new XynaObjectList<Node>(outputoptionsFormerStep,
            Node.class), dnsFlag, leaseTimes, reservedHostsExistence,
            newReplyStatus);
      }
    }
    
    
    //Vorbereitung fuer das mergen der neuen Optionen mit den alten - Subnet-Optionen werden weiter unten ausgelesen
    Map<String, String> optionsmap = new HashMap<String, String>();
    updateOptionsmap(optionsmap, outputoptionsFormerStep);

    //TODO
    boolean doDNS = dnsFlag.getDoDNS();
    String dyndns = "";
    if (domainname != null && doDNS)
      dyndns = domainname;
    
    // extrahiere Lease-Dauer aus gesetzten Optionen
    long[] prefAndValidLeaseTime = getAdequatePreferredAndValidLeaseTimes(optionsmap, mac, linkAddress, isIAPD);
    long leaseTime = prefAndValidLeaseTime[0] / toMilliSec;
    long preferredlifetime = prefAndValidLeaseTime[1] / toMilliSec;
    String prefTime = String.valueOf(preferredlifetime);
    
    int prefixToUse = 0;
    xdnc.dhcpv6.SuperPool superpoolToUse = null;
    
    for (Entry<String,Node> entry : remainingIaOptionsMap.entrySet()){
      
      xdnc.dhcpv6.Lease lease = null;
      if (isRenew && isIAPD){
     // hier muss der Superpool passend zur anfragenden IAPD gesucht werden
        prefixToUse = getPrefixLengthFromIAPDNode(entry.getValue(), isRenew);
        if (prefixToUse == 0) continue;
        matchingSuperpools = getAdequateSuperPool(superpools, isIAPD, prefixToUse);
        if (matchingSuperpools.size() == 0) {
          if (logger.isDebugEnabled()) {
            logger.debug("No superpool found");
          }
          continue;

        }
      }
      
      // prinzipiell kann es vorkommen, dass es mehrere passende Superpools
      // gibt, z.B. wenn es zwei Prefix-Pools mit Prefix-Laenge 120 und 124 zur
      // selben cmtsip gibt
      // das ist vielleicht nicht sinnvoll, aber prinzipiell konfigurierbar
      // deswegen: Loop ueber alle verfuegbaren Superpools, bis eine Adresse
      // gefunden wurde
      for (xdnc.dhcpv6.SuperPool superpool : matchingSuperpools) {

        prefixToUse = superpool.getPrefixlength();

        if (isRenew) {
          lease = getLeaseForRenew(inputoptions, linkAddress, cmtsrelayid,
              cmtsremoteid, mac, leaseTime, typeVendorClass, remoteId, entry,
              preferredlifetime, hardwaretype, duidtime, dyndns, superpool,
              isIAPD, prefixToUse);
        } else {
          lease = getLease(inputoptions, linkAddress, cmtsrelayid,
              cmtsremoteid, mac, leaseTime, typeVendorClass, remoteId, entry,
              preferredlifetime, hardwaretype, duidtime, dyndns, superpool,
              isIAPD, prefixToUse);
        }

        if (lease == null) { // kein freies Lease mehr vorhanden
          if (logger.isDebugEnabled()) {
            logger.debug("(" + mac + ") No available lease");
          }
          // nichts zu tun - entweder ReplyStatus ist eh schon
          // failed/NoAssignedIp
          // oder es gibt irgend eine positive Antwort von vorher
        } else {
          
          //Subnet-Optionen setzen - dazu ist die IP notwendig
          getSubnetSpecificOptions(lease.getIp(), lease.getSuperpoolid(), optionsmap);
          
          writeLogMessage(mac, linkAddress, lease.getIp(), pooltype.getType(),
              messagetype, lease.getPrefixlength());
          // Setzen des Message Types fuer die Rueckantwort erst spaeter!

          // IA_NA option setzen
          if (logger.isDebugEnabled()) {
            logger.debug("(" + mac + ") Building IA_NA option");
          }

          Node iaOption = buildIAoptions_neu(lease, entry.getValue(), prefTime,
              optionsmap);
          iaAddressNodes.add(iaOption);

          leaseTimes = getLeaseTimes(leaseTimes, lease);// LeaseTime String
          // starttime,
          reservedHostsExistence.getHostExists().add(
              DHCPv6Constants.RESERVEDHOST_NONEXISTENT);

          newReplyStatus = new xdnc.dhcp.Successful(
              "dynamic lease allocated for mac " + mac);

          break;
        }

      }
    }
   
  //behalte von den Klassenoptionen nur diejenigen, die auch angefragt wurden bzw. alle, die Lease-Dauern vorgeben 
    List<String> requestedOptionsList = getRequestedParameters(inputoptions);//enthaelt eine Liste von typeEncodings, wie sie auch in der Spalte optionEncoding von guiattribute auftauchen
    Map<String, String> requestedClassoptions = filterRequestedClassoptions(inputoptions, optionsmap, requestedOptionsList );
    // requestedClassoptions enthaelt Eintraege der Form (IA_NA.T1,1000), d.h. ein reduzierter classoptionsHash
    if(logger.isDebugEnabled()) {
      logger.debug("### Length Filtered Options: "+requestedClassoptions.size());
    }
    //baut aus den Optionen MDM-Nodes, die im Workflow herausgegeben werden koennen
    outputoptions = buildNodesList(optionsmap, mac);
    // Eintraege haben die Form ("IA_NA.T1","1200"), d.h. noch nicht die verschachtelte Struktur


    return new Container(new XynaObjectList<Node>(iaAddressNodes, Node.class),
        new XynaObjectList<Node>(outputoptions, Node.class), dnsFlag,
        leaseTimes, reservedHostsExistence, newReplyStatus);

  }

  private static long[] getAdequatePreferredAndValidLeaseTimes(
      Map<String, String> optionsmap, String mac, String linkAddress,
      boolean isIAPD) throws DHCPv6InvalidOptionException {
    
 // TODO: Abfrage des ClusterNodeState
    // in Abhaengigkeit vom ClusterNode-Zustand werden die zu verwendenden
    // Bindings, leaseZeiten, und Bedingungen gesetzt
    BooleanFlag useMCLT = new BooleanFlag(false);// im Stoerbetrieb wird MCLT
    // als Lease-Dauer verwendet
    
 // // extrahieren der Lease-Time aus den vorher festgelegten Optionen
    // // in den Optionen sind Lease-Dauern in Sekunden angegeben
    // // Variable leaseTime in Millisekunden
    // Zeiten werden in ms von der Methode zurueckgegeben
    String t1 = DHCPv6Constants.IAADDRT1;
    String t2 = DHCPv6Constants.IAADDRT2;
    if (isIAPD){
      t1 = DHCPv6Constants.IAPREFIXT1;
      t2 = DHCPv6Constants.IAPREFIXT2;
    }
      long[] prefAndValidLeaseTime = getPreferredAndValidLeaseTime(optionsmap,
          useMCLT, mac, linkAddress, t1, t2);

      if (logger.isDebugEnabled()) {
        logger.debug("(" + mac + ") Determined lease time = " + prefAndValidLeaseTime[0] + " ms");
      }
      
      return prefAndValidLeaseTime;
    
  }

  
  /**
   * Ist isIAPD=true und prefixlength=0, so wird ein Superpool gesucht der
   * irgend eine Prefix-Laenge < 128 hat Ist isIAPD=true und prefixlength!=0, so
   * wird ein Superpool mit genau der angegebenen Prefix-Laenge gesucht
   */
  private static ArrayList<xdnc.dhcpv6.SuperPool> getAdequateSuperPool(
      List<? extends xdnc.dhcpv6.SuperPool> superpools, boolean isIAPD,
      int prefixlength) throws XynaException {

    ArrayList<xdnc.dhcpv6.SuperPool> superpoolslist = new ArrayList<xdnc.dhcpv6.SuperPool>(
        superpools);
    if (prefixlength == 0) {
      return getSuperPool(superpoolslist, isIAPD);
    } else {
      return getSuperPoolWithPrefix(superpoolslist, prefixlength);
    }
  }
  
  private static ArrayList<xdnc.dhcpv6.SuperPool> getSuperPoolWithPrefix(
      ArrayList<xdnc.dhcpv6.SuperPool> superpools, int prefixlength) {

    ArrayList<xdnc.dhcpv6.SuperPool> returnlist = new ArrayList<xdnc.dhcpv6.SuperPool>();
    
    for (xdnc.dhcpv6.SuperPool sp : superpools) {
      if (sp.getPrefixlength() == prefixlength) {
        returnlist.add(sp);
      }
    }
    if (logger.isDebugEnabled()){
      if (returnlist.size()==0)
      logger.debug("No SuperPool found (Prefix: "+prefixlength+")!");
    }
    return returnlist;
  }


  private static boolean isIAPD(Node iaOption){
    
    if (iaOption.getTypeName().equalsIgnoreCase(DHCPv6Constants.IAPD))
      return true;
    
    return false;
    
  }
  
  public static Container assembleFinalReplyOptions(
      List<? extends Node> inputoptions,
      List<? extends Node> incomingIaAddressNodes,
      List<? extends Node> outputoptionsFormerStep)
      throws DHCPv6InvalidOptionException, DHCPv6NoOutputOptionsSetException,
      DHCPv6_InvalidMessageTypeException {
    
    ArrayList<Node> finalOutput = new ArrayList<Node>();
    
    // InnerType anhaengen
    String msgTypeToSet = determineReplyMessageTypeToSet(inputoptions);
    finalOutput.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE, msgTypeToSet));
    
    //anhaengen der IA-Optionen
    for (Node ianode : incomingIaAddressNodes){
      finalOutput.add(ianode);
    }
    
    //anhaengen von requestedOptions aus Input
    Map<String, String> optionsmap = new HashMap<String, String>();
    updateOptionsmap(optionsmap, outputoptionsFormerStep);
    //im Gegensatz zu getRequestedParameters kommen hier nur die Optionen zurueck, die echt im Input angefragt werden
    ArrayList<String> requestedOptions = getRequestedOptioncodes(inputoptions);
    ArrayList<String> remainingRequestedOptions = addRequestedOptionsToOutputReturnUnsetOptions(requestedOptions, finalOutput, optionsmap);
    
    macForDPPGUID.remove();//TODO: abschaffen?
    
    //relay message zusammenbauen
    ArrayList<Node> relaymessageoption = new ArrayList<Node>();
    relaymessageoption.add(new TypeOnlyNode(DHCPv6Constants.RELAYMESSAGE, finalOutput));
    if (finalOutput.size() == 0) {
      String mac = getMACfromOptions(inputoptions);// Notation: 000000002a3f
      String linkAddress = getLinkAddress(inputoptions);
      throw new DHCPv6NoOutputOptionsSetException("No output options were set for MAC " + mac + " on link address " + linkAddress);
    }
    
    ConfigFileGeneratorFlag cfgflag = null;
    if (!(checkFilenameSet(relaymessageoption)))
    {
      if (logger.isDebugEnabled()){
        String mac = getMACfromOptions(inputoptions);
        logger.debug("("+mac+") Filename in Option 17 Suboption 33 not set!");
      }
      
      cfgflag = new SubOptionsNotConfigured();
    }
    else
    {
      cfgflag = new SubOptionsConfigured();
    }
    if (logger.isDebugEnabled()){
      String mac = getMACfromOptions(inputoptions);
      logger.debug("("+mac+") ConfigFileGeneratorFlag active: "+(cfgflag instanceof SubOptionsConfigured));
    }

    return new Container(new XynaObjectList<Node>(relaymessageoption, Node.class), cfgflag);

  }

  
  public static List<? extends Node> assembleNoAddrsAvail(
      List<? extends Node> inputoptions,
      List<? extends Node> incomingIaAddressNodes,
      List<? extends Node> outputoptionsFormerStep, PoolType pooltype)
      throws DHCPv6InvalidOptionException, DHCPv6NoOutputOptionsSetException,
      DHCPv6_InvalidMessageTypeException {
    
    ArrayList<Node> finalOutput = new ArrayList<Node>();
    
 // InnerType anhaengen
    String msgTypeToSet = determineReplyMessageTypeToSet(inputoptions);
    finalOutput.add(new TypeWithValueNode(DHCPv6Constants.MSGTYPE, msgTypeToSet));
    
    String mac = getMACfromOptions(inputoptions);// Notation: 000000002a3f
    String linkAddress = getLinkAddress(inputoptions);
    
    finalOutput.add(new TypeWithValueNode(DHCPv6Constants.STATUSCODE, DHCPv6Constants.STATUS_NOADDRSAVAIL));
    long timestamp = System.currentTimeMillis();
    logger.info(new StringBuilder().append("("+timestamp+")SO#").append(mac).append("#via ").append(linkAddress).append("#NoAddrsAvail").append("#").append(pooltype.getType()));
    
    macForDPPGUID.remove();//TODO: abschaffen?
    
    //relay message zusammenbauen
    ArrayList<Node> relaymessageoption = new ArrayList<Node>();
    relaymessageoption.add(new TypeOnlyNode(DHCPv6Constants.RELAYMESSAGE, finalOutput));
    if (finalOutput.size() == 0) {
      throw new DHCPv6NoOutputOptionsSetException("No output options were set for MAC " + mac + " on link address " + linkAddress);
    }
    
    return relaymessageoption;
  }
  
  
  private static String determineReplyMessageTypeToSet(
      List<? extends Node> inputoptions) throws DHCPv6_InvalidMessageTypeException {
    
    String incomingMsgType = determineDHCPv6MessageTypeAsString(inputoptions);
    if (incomingMsgType.equalsIgnoreCase(DHCPv6Constants.TYPE_SOLICIT))
      return DHCPv6Constants.MSGTYPE_ADVERTISE;
    else
      return DHCPv6Constants.MSGTYPE_REPLY;
    
  }
  
  public static String determineDHCPv6MessageTypeAsString(List<? extends Node> inputoptions) throws DHCPv6_InvalidMessageTypeException {
    for (Node relaymsg : inputoptions) {
      if (relaymsg.getTypeName().equalsIgnoreCase(DHCPv6Constants.RELAYMESSAGE)) {
        for (Node node : ((TypeOnlyNode) relaymsg).getSubnodes()) {
          if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.MSGTYPE)) {
            String msgtype = ((TypeWithValueNode) node).getValue();
            if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_SOLICIT)) {
              return DHCPv6Constants.TYPE_SOLICIT;
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_REQUEST)) {
              return DHCPv6Constants.TYPE_REQUEST;
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_RENEW)) {
              return DHCPv6Constants.TYPE_RENEW;
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_REBIND)) {
              return DHCPv6Constants.TYPE_REBIND;
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_DECLINE)) {
              return DHCPv6Constants.TYPE_DECLINE;
            }
            else if (msgtype.equalsIgnoreCase(DHCPv6Constants.MSGTYPE_RELEASE)) {
              return DHCPv6Constants.TYPE_RELEASE;
            }
            else {
              throw new DHCPv6_InvalidMessageTypeException(msgtype);
            }
          }
        }
      }
    }
    //logger.error("DHCPv6 message type couldn't be retrieved from input");
    throw new DHCPv6_InvalidMessageTypeException("none - no RelayMessage given in Input");
  }
  
  private static void getPoolTypeAttributes(
      PoolType mdmPoolType,
      Map<String, String> classoptionsHash)
      throws DHCPv6InconsistentDataException,
      DHCPv6AttributeNotFoundForClassException {

    // fixedAttributes setzen
    String fixedAttrString = mdmPoolType.getFixedAttributes();
    if (!(fixedAttrString == null)) {
      getFixedAttributes(fixedAttrString, classoptionsHash);
    }
    

    // attributes setzen
    String attrString = mdmPoolType.getAttributes();
    getAttributes(attrString, classoptionsHash);
  }
    
  
  private static xdnc.dhcpv6.Lease getLease(List<? extends Node> inputoptions,
      String linkAddress, String cmtsrelayid, String cmtsremoteid, String mac,
      long leaseTime, String typeVendorClass, String remoteId,
      Entry<String, Node> entry, long preferredlifetime, long hardwaretype,
      long duidtime, String dyndns, xdnc.dhcpv6.SuperPool superpool, boolean isIAPD, int prefixToUse)
      throws XynaException {
    
    xdnc.dhcpv6.Lease lease = null;
    String messageType = determineDHCPv6MessageTypeAsString(inputoptions);
    boolean isSolicit = false;
    boolean isRequest = false;

    if (superpool != null) {
      
      if (messageType.equalsIgnoreCase(DHCPv6Constants.TYPE_SOLICIT)){
        isSolicit = true;
      } else if (messageType.equalsIgnoreCase(DHCPv6Constants.TYPE_REQUEST)){
        isRequest = true;
      } else {
        throw new RuntimeException("Invalid message type " + messageType + " received during lease allocation for mac " +mac);
      }
      
      xdnc.dhcpv6.Lease leaseRequest = null;
        
      if (isSolicit){
        leaseRequest = createSolicitLease(superpool, mac,
            leaseTime, typeVendorClass, remoteId, entry, preferredlifetime,
            hardwaretype, duidtime, dyndns, linkAddress, cmtsrelayid,
            cmtsremoteid);
      } else if (isRequest){
        leaseRequest = createRequestLease(superpool, mac, leaseTime, typeVendorClass, remoteId,
            entry, preferredlifetime, hardwaretype, duidtime, dyndns, linkAddress, cmtsrelayid, cmtsremoteid);
      }
      
      leaseRequest.setPrefixlength(prefixToUse);
          
      setVendorSpecificOptions(inputoptions, leaseRequest);     
      
      lease = getAndSetLeaseByMacAndIAIDAndTime(leaseRequest);

      if (lease == null || lease.getIp() == null) {
        lease = getAndSetLeaseByMacAndIAID(leaseRequest);
      }

      if (isSolicit) {
        if (lease == null || lease.getIp() == null) {
          if (superpool.getLeasecount().length() == 0
              || superpool.getLeasecount().equals(("0"))) {
            lease = null;
          } else {
            if (new BigInteger(superpool.getLeasecount())
                .compareTo(new BigInteger(String.valueOf(limitsmalllargepools))) <= 0) {
              lease = getAndSetFreeIPSmallPools(leaseRequest);
            } else {
              lease = getAndSetFreeIPLargePools(leaseRequest, superpool);
            }
          }
        }
      }//Ende if isSolicit
      else if (isRequest){
        if(lease == null || lease.getIp()==null)
        {
          logger.debug("LeaseByMacAndIAID had no IP or ID => no valid lease. Setting lease to null!");
          lease = null;
        }
      }

    }
    if(lease!=null)
    {
      if(hardwaretype!=-1)lease.setHardwaretype(hardwaretype);
      if(duidtime!=-1)lease.setDuidtime(duidtime);
    }
    return lease;
  }
  
  
  private static xdnc.dhcpv6.Lease getLeaseForRenew(
      List<? extends Node> inputoptions, String linkAddress,
      String cmtsrelayid, String cmtsremoteid, String mac, long leaseTime,
      String typeVendorClass, String remoteId, Entry<String, Node> entry,
      long preferredlifetime, long hardwaretype, long duidtime, String dyndns,
      xdnc.dhcpv6.SuperPool superpool, boolean isIAPD, int prefixToUse) throws XynaException {

    xdnc.dhcpv6.Lease lease = null;

    if (superpool != null) {
      
      xdnc.dhcpv6.Lease leaseRequest = null;

      String ipFromRenew = getIpFromIAnode(entry.getValue());
      if (ipFromRenew == null) {
        // TODO: fachlich korrektes Verhalten
        throw new XynaException(
            "No valid IPv6-Address given in DHCPv6-Renew with mac " + mac);
      }
      leaseRequest = createLeaseRenew(mac, leaseTime, ipFromRenew, typeVendorClass,
          remoteId, entry, preferredlifetime, leaseTime, superpool
              .getSuperpoolid(), linkAddress, cmtsremoteid, cmtsrelayid);

   
      leaseRequest.setPrefixlength(prefixToUse);
     
      
      setVendorSpecificOptions(inputoptions, leaseRequest);
      if (leaseRequest.getVendorspecificinformation() == null
          || leaseRequest.getVendorspecificinformation().length() == 0) {
        leaseRequest.setVendorspecificinformation(TO_BE_SET_WITH_DB_VALUE);
      }
      lease = getAndSetLeaseByMacAndIAIDAndTimeRenew(leaseRequest);

      
      if (lease == null || lease.getIp() == null ){//|| lease.getLeaseid() == -1) {
        leaseRequest.setStarttime(System.currentTimeMillis());
        lease = getAndSetLeaseByMacAndIAIDAndIPAndPrefix(leaseRequest);

      }
      
      if (lease == null) {// es gab kein lease mehr, in dem mac und ip
                          // uebereinstimmen - verwende Lease mit dieser MAC aus
                          // Superpool, wenn es frei ist

        
        if (superpool.getLeasecount().length() == 0
            || superpool.getLeasecount().equals(("0"))) {
          lease = null;
        } else {
          if (new BigInteger(superpool.getLeasecount())
              .compareTo(new BigInteger(String.valueOf(limitsmalllargepools))) <= 0) {
            lease = getAndSetLeaseByIPAndFreeSmallPools(leaseRequest);
          } else {
            lease = getAndSetLeaseByIPAndFreeLargePools(leaseRequest, superpool);
          }
        }

      }

    }
    if(lease!=null)
    {
      if(hardwaretype!=-1)lease.setHardwaretype(hardwaretype);
      if(duidtime!=-1)lease.setDuidtime(duidtime);
    }
    return lease;
  }
  
  public static xdnc.dhcpv6.Lease getAndSetLeaseByIPAndFreeLargePools(
      xdnc.dhcpv6.Lease l, xdnc.dhcpv6.SuperPool sp) throws XynaException {
    ODSConnection con = ods.openConnection();
    //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
    con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

    Parameter sqlparameter = new Parameter();

    try {
      BigInteger factor = BigInteger.valueOf(2).pow(128 - sp.getPrefixlength());
      BigInteger half = new BigInteger(sp.getLeasecount()).multiply(factor)
          .divide(new BigInteger(String.valueOf(2)));

      String renewIP = l.getIp();
      sqlparameter = new Parameter(renewIP);

      String sqlStatement = "Select * from " + Lease.TABLENAME + " where "
          + Lease.COL_IP + " = ?";
      

      Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con,
          sqlStatement, sqlparameter, new Lease().getReader(),-1);

      if (queryResult.size() == 0) {
        l.setIp(renewIP);
        //l.setLeaseid(XynaFactory.getInstance().getIDGenerator().getUniqueId())
        // ;
        //l.setPrefixlength(sp.getPrefixlength());
        l.setBinding(bindingToBeSet);
        Lease result = convertLease(l);
        con.persistObject(result);
        con.commit();
      } else {
        l = null;
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }

    }

    return l;
  }

  /**
   * Verarbeitung eines BulkLeaseQueries, der ueber TCP kam 
   */

  public static List<? extends Node> processBulkLeaseQuery(List<? extends Node> inputoptions) throws DHCPv6InconsistentDataException, DHCPv6InvalidOptionException, DHCPv6MultipleMacAddressesForIPException, XynaException {

    String debugmac = getMACfromLeaseQuery(inputoptions);
    String queriedip = "";
    String queriedmac = "";
    
    List<? extends Node> leasequerysubnodes = new ArrayList<Node>();
    
    for (Node node : inputoptions) {
      if (node.getTypeName().equalsIgnoreCase(DHCPv6Constants.LEASEQUERYOPTION)) {
        leasequerysubnodes = ((TypeOnlyNode)node).getSubnodes();
      }
    }

    
    String cmtsrelayid = getCMTSRelayID(leasequerysubnodes);
    String cmtsremoteid = getCMTSRemoteID(leasequerysubnodes);

    
    if(debugmac==null)debugmac="";

    String querytypestring = getLeaseQueryType(inputoptions);

    if (querytypestring == null) // Keine LeaseQueryOption gefunden
    {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") LeaseQuery did not contain readable LeaseQuery Option.");
      }
      //throw new DHCPv6InvalidOptionException(DHCPv6Constants.LEASEQUERYTYPE, querytypestring);
      //throw new RuntimeException("LeaseQuery with no readable LeaseQueryOption!");
    }


    int querytype = -1;
    try {
      querytype = Integer.parseInt(querytypestring);
    }
    catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("("+debugmac+") LeaseQueryType " + querytypestring + " invalid!");
      }
      //throw new DHCPv6InvalidOptionException(DHCPv6Constants.LEASEQUERYTYPE, querytypestring);
      //throw new RuntimeException("LeaseQueryType invalid format!");

    }
    String querylink = getLeaseQueryLinkAddress(inputoptions);
//    if (!querylink.equals("0x00000000000000000000000000000000")) {
//      querytype = -1; // => unknown Querytype bei angegebener Linkaddresse
//    }

    ArrayList<Node> outputoptions = new ArrayList<Node>();

    // Query Typ pruefen und unterscheiden
    if (querytype != 1 && querytype != 2 && querytype != 3 && querytype != 4 && querytype != 5) {
      TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0007"); // unknown QueryType
      outputoptions.add(status);
    }
    else if(querytype==1){
      TypeOnlyNode iAAdd = new TypeOnlyNode();

      String ipToQueryFor = getIpInQuery(inputoptions, iAAdd);
      
      try {
        ipToQueryFor = IPv6AddressUtil.convertSearchStr2LongSearchStr(ipToQueryFor);
      }
      catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger
                          .debug("Error while trying to convert ipAddress to long search string for database search. (Process LeaseQuery");
        }
      }

      queriedip = ipToQueryFor;
      
      if (iAAdd.getTypeName() != null) {


        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Lease> leasesList = new ArrayList<Lease>();

        ArrayList<Lease> prefixList = new ArrayList<Lease>();

        long currentTimeMillis = System.currentTimeMillis();
        String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_IP + " = ? and expirationTime > ?";
        String prefixsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_PREFIXLENGTH + " < 128 and expirationTime > ?";

        try {
          // nach angefragter Addresse direkt schauen
          // nimm gecachte Anfrage wenn moeglich

          Parameter sqlparameter = new Parameter(ipToQueryFor, currentTimeMillis);
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

          for (Lease l : queryResult) {
            leasesList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length: " + queryResult.size());
          }

          // hier Prefixe aus Datenbank auslesen
          // nimm gecachte Anfrage wenn moeglich

          Parameter sqlparameter2 = new Parameter(currentTimeMillis);
          Collection<? extends Lease> queryResult2 = DHCPv6ODS.queryODS(con, prefixsearch, sqlparameter2, new Lease().getReader(),-1);

          for (Lease l : queryResult2) {
            prefixList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length " + queryResult.size());
          }


        }
        finally {
          con.closeConnection();
        }

        if (leasesList.size() == 0) // keine Mac gefunden
        {
          if (prefixList.size() == 0) // kann auch nicht in Addressbereich liegen
          {
            TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
            outputoptions.add(status);
          }
          else // eventuell noch Hoffnung ...
          {
            IPv6AddressUtil queriedaddress = IPv6AddressUtil.parse(ipToQueryFor);
            IPv6AddressUtil adressindb;
            IPv6AddressUtil subnetindb;
            IPv6AddressUtil subnetqueriedaddress;
            Lease result = null;

            for (Lease le : prefixList) {
              adressindb = IPv6AddressUtil.parse(le.getIp());
              subnetindb = IPv6SubnetUtil.calculateIPv6PrefixAddress(adressindb, le.getPrefixlength());
              subnetqueriedaddress = IPv6SubnetUtil.calculateIPv6PrefixAddress(queriedaddress, le.getPrefixlength());

              if (subnetindb.equals(subnetqueriedaddress)) {
                result = le;
              }
            }

            if (result == null) // auch kein passendes Prefix gefunden
            {
              TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
              outputoptions.add(status);
            }
            else // Adressbereich gefunden
            {
              leasesList.add(result);

            }


          }
        }
        
        if (leasesList.size() > 1) // mehrere Macs gefunden?
        {
          throw new DHCPv6MultipleMacAddressesForIPException("Found multiple macs for one ip address (leaseQueryv6)");
          //throw new XynaException("Found multiple macs for one ip address (leaseQueryv6)");
        }
        if(leasesList.size()==1) {
          ArrayList<Node> subnodes = new ArrayList<Node>();
          ArrayList<Node> subsubnodes = new ArrayList<Node>();
          ArrayList<Node> subsubsubnodes = new ArrayList<Node>();


          Lease currentLease = leasesList.get(0);

          String resulthardwaretype = String.valueOf(currentLease.getHardwareType());
          String resultduidtime = String.valueOf(currentLease.getDUIDTime());

          String resultmac = currentLease.getMac().toUpperCase();
          resultmac = "0x" + StringUtils.fastReplace(resultmac, ":", "", -1);

          
          resulthardwaretype = "1"; // hardcodiert 1 als Antwort bei LeaseQuery
          
          if(!resultduidtime.equals("-1"))
          {
            subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
            subsubsubnodes.add(new TypeWithValueNode("Time", resultduidtime));
            subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

            subsubnodes.add(new TypeOnlyNode("DUID-LLT", subsubsubnodes));
          }
          else
          {
            subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
            subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

            subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
          }

          subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

          List<Long> transactiontimes = new ArrayList<Long>();

          
          long lastclientransactiontime = ((currentTimeMillis - currentLease.getStartTime()) / 1000L);
          transactiontimes.add(lastclientransactiontime);

          
          // andere Adressen suchen, die unter der Mac vergeben sind
          con = ods.openConnection();
          //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
          con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

          String macToQueryFor = resultmac.substring(2).toLowerCase();
          
          String dbsearchbymac = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_MAC + " = ? and expirationTime > ?";

          Collection<? extends Lease> queryResult = null;
          try {
            Parameter sqlparameter = new Parameter(macToQueryFor, currentTimeMillis);
            queryResult = DHCPv6ODS.queryODS(con, dbsearchbymac, sqlparameter, new Lease().getReader(),-1);
          }
          finally {
            con.closeConnection();
          }
          
          for(Lease res:queryResult)
          {
              if (res.getPrefixlength() == 128) {
                ArrayList<Node> iaaddsubnodes = new ArrayList<Node>();

                iaaddsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));
                iaaddsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                iaaddsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));

                TypeOnlyNode IAAddneu = new TypeOnlyNode(iAAdd.getTypeName(),iaaddsubnodes);
                
                //subnodes.add(iAAdd);
                subnodes.add(IAAddneu);
              }
              else {

                ArrayList<Node> prefixsubnodes = new ArrayList<Node>();


                prefixsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("PrefixLength", Integer.toString(res.getPrefixlength())));
                prefixsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));

                TypeOnlyNode iAPrefneu = new TypeOnlyNode("IAPrefix",prefixsubnodes);
                subnodes.add(iAPrefneu);

              }
              lastclientransactiontime=((currentTimeMillis - res.getStartTime()) / 1000L);
              transactiontimes.add(lastclientransactiontime);

          }
          
          lastclientransactiontime = 0;
          
          for(long l:transactiontimes)
          {
            if(lastclientransactiontime == 0 || l < lastclientransactiontime) lastclientransactiontime = l;
          }

          
          if (lastclientransactiontime < Integer.MAX_VALUE) {
            subnodes.add(new TypeWithValueNode("CLTTime", String.valueOf(lastclientransactiontime)));
          }

          
          outputoptions.add(new TypeOnlyNode("ClientData", subnodes));
        }


      }
      else // keine IA Address
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }
    else if(querytype==2)
    { //Query by Mac
      TypeOnlyNode clientid = new TypeOnlyNode();

      String macToQueryFor = getMacInQuery(inputoptions, clientid);
      
      long currentTimeMillis = System.currentTimeMillis();
      
      queriedmac = macToQueryFor;
      
      if (clientid.getTypeName() != null) {


        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Node> subnodes = new ArrayList<Node>();

        subnodes.add(clientid);


        // andere Adressen suchen, die unter der Mac vergeben sind
        con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());

                 
        String dbsearchbymac = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_MAC + " = ? and expirationTime > ?";

        Collection<? extends Lease> queryResult = null;
        try {
          Parameter sqlparameter = new Parameter(macToQueryFor, currentTimeMillis);
          queryResult = DHCPv6ODS.queryODS(con, dbsearchbymac, sqlparameter, new Lease().getReader(),-1);
        }
        finally {
          con.closeConnection();
        }
        
        if (queryResult.size() == 0) // keine Mac gefunden
        {
            TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
            outputoptions.add(status);
        }
        else if(queryResult.size()>0)
        {
          List<Long> transactiontimes = new ArrayList<Long>();
          
          for(Lease res:queryResult)
          {
              if (res.getPrefixlength() == 128) {
                ArrayList<Node> iaaddsubnodes = new ArrayList<Node>();

                iaaddsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));
                iaaddsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                iaaddsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));

                TypeOnlyNode IAAddneu = new TypeOnlyNode(DHCPv6Constants.IAADDR,iaaddsubnodes);
                
                //subnodes.add(iAAdd);
                subnodes.add(IAAddneu);
              }
              else {

                ArrayList<Node> prefixsubnodes = new ArrayList<Node>();


                prefixsubnodes.add(new TypeWithValueNode("T1", String.valueOf(res.getPreferredLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("T2", String.valueOf(res.getValidLifetime())));
                prefixsubnodes.add(new TypeWithValueNode("PrefixLength", Integer.toString(res.getPrefixlength())));
                prefixsubnodes.add(new TypeWithValueNode("IPv6", res.getIp()));

                TypeOnlyNode iAPrefneu = new TypeOnlyNode(DHCPv6Constants.IAPREF,prefixsubnodes);
                subnodes.add(iAPrefneu);

              }
              long lastclientransactiontime=((currentTimeMillis - res.getStartTime()) / 1000L);
              transactiontimes.add(lastclientransactiontime);
          }
          
          long lastclientransactiontime = 0;
          
          for(long l:transactiontimes)
          {
            if(lastclientransactiontime == 0 || l < lastclientransactiontime) lastclientransactiontime = l;
          }
          
          if (lastclientransactiontime < Integer.MAX_VALUE && lastclientransactiontime !=0) {
            subnodes.add(new TypeWithValueNode("CLTTime", String.valueOf(lastclientransactiontime)));
          }

          outputoptions.add(new TypeOnlyNode("ClientData", subnodes));
          
        }
      }
      else // keine ClientID
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }

    else if(querytype==3){ // Query by RelayID

      if(cmtsrelayid.length()>0)  // cmtsrelayid muss vorhanden sein, sonst malformed query
      {
        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Lease> leasesList = new ArrayList<Lease>();

        ArrayList<Lease> prefixList = new ArrayList<Lease>();


        String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_CMTSRELAYID+ " = ?";
        Parameter sqlparameter = new Parameter(cmtsrelayid);

        if (!querylink.equals("0x00000000000000000000000000000000")) { //zusaetzlich linkadresse beachten
          dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_CMTSRELAYID+ " = ? and "+Lease.COL_CMTSIP+" = ?";
         
          
          String requestedcmtsip = "";
          try {
            requestedcmtsip = InetAddress.getByAddress(com.gip.xyna.xact.tlvencoding.util.ByteUtil.toByteArray(querylink)).getHostAddress();
            requestedcmtsip = IPv6AddressUtil.convertSearchStr2LongSearchStr(requestedcmtsip);
          }
          catch (Exception e) {
            // TODO Auto-generated catch block
            if(logger.isDebugEnabled())logger.debug("Problemes converting linkaddress in Bulk LeaseQuery: ", e);
          }

          
          sqlparameter = new Parameter(cmtsrelayid, requestedcmtsip);
        }

        try {
          // nach angefragter Addresse direkt schauen
          // nimm gecachte Anfrage wenn moeglich

          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query with String: " + dbsearch);
            logger.debug("("+debugmac+") Parameter: " + sqlparameter.get(0));
          }
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

          for (Lease l : queryResult) {
            leasesList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length: " + queryResult.size());
          }

          // hier Prefixe aus Datenbank auslesen
          // nimm gecachte Anfrage wenn moeglich

        }
        finally {
          con.closeConnection();
        }

        setLeaseQueryReply(outputoptions, leasesList);


      }
      else // keine relayID
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }

    else if(querytype==4){ // Query by Link

      if(querylink.length()>0 && !querylink.equals("0x00000000000000000000000000000000"))  // Linkaddresse muss vorhanden sein, sonst malformed query
      {
        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Lease> leasesList = new ArrayList<Lease>();

        ArrayList<Lease> prefixList = new ArrayList<Lease>();


        String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_CMTSIP+ " = ?";
        Parameter sqlparameter = new Parameter(querylink);

        try {
          // nach angefragter Addresse direkt schauen
          // nimm gecachte Anfrage wenn moeglich

          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query with String: " + dbsearch);
            logger.debug("("+debugmac+") Parameter: " + sqlparameter.get(0));
          }
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

          for (Lease l : queryResult) {
            leasesList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length: " + queryResult.size());
          }

          // hier Prefixe aus Datenbank auslesen
          // nimm gecachte Anfrage wenn moeglich

        }
        finally {
          con.closeConnection();
        }

        setLeaseQueryReply(outputoptions, leasesList);


      }
      else // keine LinkAddresse, sollte eigentlich gar nicht vorkommen hier, oder Linkaddresse 0
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }
    else if(querytype==5){ // Query by RemoteID

      if(cmtsremoteid.length()>0)  // RemoteID muss vorhanden sein, sonst malformed query
      {
        // Durchsuche Tabelle leasestable nach angegebener IP
        ODSConnection con = ods.openConnection();
        //Operationen sollen im Modus "nicht strikt kohaerent" ausgefuehrt werden
        con.setTransactionProperty(TransactionProperty.noSynchronousActiveClusterSynchronizationNeeded());


        ArrayList<Lease> leasesList = new ArrayList<Lease>();

        ArrayList<Lease> prefixList = new ArrayList<Lease>();


        String dbsearch = "SELECT * FROM " + Lease.TABLENAME + " WHERE " + Lease.COL_CMTSREMOTEID+ " = ?";
        Parameter sqlparameter = new Parameter(cmtsremoteid);

        try {
          // nach angefragter Addresse direkt schauen
          // nimm gecachte Anfrage wenn moeglich

          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query with String: " + dbsearch);
            logger.debug("("+debugmac+") Parameter: " + sqlparameter.get(0));
          }
          Collection<? extends Lease> queryResult = DHCPv6ODS.queryODS(con, dbsearch, sqlparameter, new Lease().getReader(),-1);

          for (Lease l : queryResult) {
            leasesList.add(l);
          }
          if (logger.isDebugEnabled()) {
            logger.debug("("+debugmac+") Query result length: " + queryResult.size());
          }

          // hier Prefixe aus Datenbank auslesen
          // nimm gecachte Anfrage wenn moeglich

        }
        finally {
          con.closeConnection();
        }

        setLeaseQueryReply(outputoptions, leasesList);


      }
      else // keine cmtsremoteid
      {
        TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0008"); // malformed Query
        outputoptions.add(status);
      }


    }

    if (logger.isInfoEnabled()) {
      if(queriedip.length()>0)
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#").append(queriedip));
      }
      else if(queriedmac.length()>0)
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#").append(queriedmac));
      }
      else
      {
        logger.info(new StringBuilder().append("LQ#").append(debugmac).append("#RP#"));
      }
    
    }
    return new XynaObjectList<Node>(outputoptions, Node.class);


  }


  private static void setLeaseQueryReply(ArrayList<Node> outputoptions, ArrayList<Lease> leasesList) {
    if (leasesList.size() == 0) // kein Eintrag gefunden
    {
      TypeWithValueNode status = new TypeWithValueNode(DHCPv6Constants.STATUSCODE, "0x0009"); // notConfigured
      outputoptions.add(status);
    }
    else if(leasesList.size()>0)
    {
      // Pruefung ob Daten zu mehreren Clients gefunden wurden
      boolean multipleclients=false;
      String mac=leasesList.get(0).getMac();
      List<String> clientmacs = new ArrayList<String>();
      clientmacs.add(mac);
      for(Lease l:leasesList)
      {
        if(!clientmacs.contains(l.getMac()))
        {
          multipleclients=true;
          clientmacs.add(l.getMac());
        }
      }
      
      if(!multipleclients)
      {
        outputoptions.add(getClientData(leasesList));
      }
      else // Mehrere Clients, LEASEQUERY DATA Format packen
      {
        // LeaseQueryReply Nachricht erstellen
        TypeOnlyNode leasequeryreply = new TypeOnlyNode();
        leasequeryreply.setTypeName("LeaseQueryReply");
        List<Node> leasequeryreplysubnodes = new ArrayList<Node>();
        
        
        List<Lease> leasesOfOneClient = new ArrayList<Lease>();
        for(Lease l:leasesList)
        {
          if(l.getMac().equals(mac))
          {
            leasesOfOneClient.add(l);
          }
        }
        leasequeryreplysubnodes.add(getClientData(leasesOfOneClient));
        leasequeryreply.setSubnodes(leasequeryreplysubnodes);
        clientmacs.remove(mac);

        outputoptions.add(leasequeryreply);
        
        // ab hier fuer andere Clients LEASEQUERY DATA Nodes erstellen
        
        for(String currentMac:clientmacs)
        {
          TypeOnlyNode leasequerydata = new TypeOnlyNode();
          leasequerydata.setTypeName("LeaseQueryData");
          List<Node> leasequerydatasubnodes = new ArrayList<Node>();
          List<Lease> leasesOfClient = new ArrayList<Lease>();

          for(Lease l:leasesList)
          {
            if(l.getMac().equals(currentMac))
            {
              leasesOfClient.add(l);

            }
          }
          leasequerydatasubnodes.add(getClientData(leasesOfClient));
          leasequerydata.setSubnodes(leasequerydatasubnodes);
          outputoptions.add(leasequerydata);
        }
        
      }
      
      
    }
  }


  private static TypeOnlyNode getClientData(List<Lease> leases) {
    
    long currentTimeMillis = System.currentTimeMillis();
    
    ArrayList<Node> subnodes = new ArrayList<Node>();

    List<Long> transactiontimes = new ArrayList<Long>();
    
    for(Lease currentLease:leases)
    {
      long lastclientransactiontime = ((currentTimeMillis - currentLease.getStartTime()) / 1000L);

      transactiontimes.add(lastclientransactiontime);
      
      ArrayList<Node> subsubnodes = new ArrayList<Node>();
      ArrayList<Node> subsubsubnodes = new ArrayList<Node>();

      if (currentLease.getPrefixlength() == 128) {
        ArrayList<Node> iaaddsubnodes = new ArrayList<Node>();

        iaaddsubnodes.add(new TypeWithValueNode("IPv6", currentLease.getIp()));
        iaaddsubnodes.add(new TypeWithValueNode("T1", String.valueOf(currentLease.getPreferredLifetime())));
        iaaddsubnodes.add(new TypeWithValueNode("T2", String.valueOf(currentLease.getValidLifetime())));

        TypeOnlyNode IAAddneu = new TypeOnlyNode(DHCPv6Constants.IAADDR,iaaddsubnodes);
        
        //subnodes.add(iAAdd);
        subnodes.add(IAAddneu);
      }
      else {

        ArrayList<Node> prefixsubnodes = new ArrayList<Node>();


        prefixsubnodes.add(new TypeWithValueNode("T1", String.valueOf(currentLease.getPreferredLifetime())));
        prefixsubnodes.add(new TypeWithValueNode("T2", String.valueOf(currentLease.getValidLifetime())));
        prefixsubnodes.add(new TypeWithValueNode("PrefixLength", Integer.toString(currentLease.getPrefixlength())));
        prefixsubnodes.add(new TypeWithValueNode("IPv6", currentLease.getIp()));

        TypeOnlyNode iAPrefneu = new TypeOnlyNode(DHCPv6Constants.IAPREF,prefixsubnodes);
        subnodes.add(iAPrefneu);

      }
     
    }

    Lease currentLease = leases.get(0);
    
    ArrayList<Node> subsubnodes = new ArrayList<Node>();
    ArrayList<Node> subsubsubnodes = new ArrayList<Node>();

    
    String resulthardwaretype = String.valueOf(currentLease.getHardwareType());
    String resultduidtime = String.valueOf(currentLease.getDUIDTime());

    String resultmac = currentLease.getMac().toUpperCase();
    resultmac = "0x" + StringUtils.fastReplace(resultmac, ":", "", -1);

    resulthardwaretype = "1"; // hardcodiert 1 als Antwort bei LeaseQuery

    
    if(!resultduidtime.equals("-1"))
    {
      subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
      subsubsubnodes.add(new TypeWithValueNode("Time", resultduidtime));
      subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

      subsubnodes.add(new TypeOnlyNode("DUID-LLT", subsubsubnodes));
    }
    else
    {
      subsubsubnodes.add(new TypeWithValueNode("HardwareType", resulthardwaretype));
      subsubsubnodes.add(new TypeWithValueNode("LinkLayerAddress", resultmac));

      subsubnodes.add(new TypeOnlyNode("DUID-LL", subsubsubnodes));
    }

    subnodes.add(new TypeOnlyNode("ClientID", subsubnodes));

    long lastclienttransactiontime = transactiontimes.get(0);
    
    for(long current:transactiontimes)
    {
      if(current<lastclienttransactiontime)
      {
        lastclienttransactiontime = current;
      }
    }
    

    if (lastclienttransactiontime < Integer.MAX_VALUE) {
      subnodes.add(new TypeWithValueNode("CLTTime", String.valueOf(lastclienttransactiontime)));
    }

    

    return new TypeOnlyNode("ClientData", subnodes);
  }
  

}
