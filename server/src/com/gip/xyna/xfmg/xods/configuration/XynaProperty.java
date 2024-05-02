/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

package com.gip.xyna.xfmg.xods.configuration;



import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDouble;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDurationCompatible;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.priority.PriorityManagement;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.RecalculateHash;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.CreationAlgorithm;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerWithAlterTableSupportHelper;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfqctrl.ConstantRateEventCreationAlgorithm;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xprcods.exceptionmgmt.BlackExceptionCodeManagement.CodeGroupPattern;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.OrderStartupMode;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.scheduling.XynaSchedulerCustomisation;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintManagement;



/**
 * Container class for predefined Xyna Properties.
 */
public interface XynaProperty {


  public static final String XYNA_XPRC_XPCE_STARTUP_ORDERTYPE = "xyna.xprc.xpce.startup.ordertype";
  public static final String CROSS_DOMAIN_XML_PROPERTY = "xyna.server.crossdomain.xml";

  /**
   * Two properties that are responsible for the amount of monitoring at order execution time.
   * <ul>
   * <li> XYNA_DEFAULT_MONITORING_LEVEL: The default monitoring level that is used if nothing else is configured </li>
   * <li>
   * XYNA_GLOBAL_ORDER_CONTEXT_SETTINGS: If this is set to true or false, for every or for no order, respectively, an
   * order context mapping is created such that the context object can be accessed from basically everywhere within the
   * same thread. </li>
   * </ul>
   */
  public static final XynaPropertyInt XYNA_DEFAULT_MONITORING_LEVEL =
      new XynaPropertyInt("xyna.default.monitoringlevel", MonitoringDispatcher.DEFAULT_MONITORING_LEVEL).
        setDefaultDocumentation(DocumentationLanguage.EN, "The default monitoring level that is used if nothing else is configured. Valid values are:\n"
                                                          + "0 = No audit data will be created at all.\n"
                                                          + "5 = Rudimentary audit data will only be created if an error occurs.\n"
                                                          + "10 = Rudimentary audit data will be created. After creation, the only update to the captured data is performed after finishing the Cleanup stage.\n"
                                                          + "15 = Rudimentary audit data will be created. Every Master Workflow state change results in an update to the captured data, especially to the \"last update\" timestamp.\n"
                                                          + "17 = While an order is running, comprehensive audit data will be created similar to monitoring level 20. If no error occurs, all audit data will be removed (similar to monitoring level 0).\n"
                                                          + "18 = While an order is running, comprehensive audit data will be created similar to monitoring level 20. If no error occurs, audit data will be reduced (similar to monitoring level 10 and 15).\n"
                                                          + "20 = Comprehensive audit data including input, output and error information for every single workflow step will be created.");
  
  public static final XynaPropertyBoolean XYNA_GLOBAL_ORDER_CONTEXT_SETTINGS =
      new XynaPropertyBoolean("xyna.global.set.ordercontext", false);

  public static final String XYNA_PERFORM_MDM_UPDATES = "xyna.perform.mdm.updates";
  public static final String XYNA_PERFORM_GENERAL_UPDATES = "xyna.perform.general.updates";
  public static final XynaPropertyBoolean XYNA_CREATE_LOG4J_DIAG_CONTEXT =
      new XynaPropertyBoolean("xyna.create.diag.cont", false);

  /**
   * true =&gt; aufträge die auf scheduling warten (weil ihre capacities belegt sind oder sowas) werden gebackupped. false
   * =&gt; werden nicht gebackupped.
   */
  public static final XynaPropertyBoolean XYNA_BACKUP_ORDERS_WAITING_FOR_SCHEDULING =
      new XynaPropertyBoolean("xyna.scheduler.orderbackupwaitingforscheduling", false);
  
  
  public static final XynaPropertyEnum<OrderStartupMode> XYNA_ORDER_STARTUP_MODE = 
                  XynaPropertyEnum.construct("xyna.xprc.orderstartupmode", OrderStartupMode.UNSAFE );
  
  public static final XynaPropertyBoolean XYNA_BACKUP_DURING_CRON_LIKE_SCHEDULING =
      new XynaPropertyBoolean("xyna.scheduler.orderbackup.duringcronlikescheduling", true);
  public static final XynaPropertyInt XYNA_WORKFLOW_POOL_SIZE =
      new XynaPropertyInt("xyna.wf.poolsize", 100);

  public static final XynaPropertyInt XYNA_BACKUP_STORE_RETRIES = 
      new XynaPropertyInt("xyna.xprc.xprcods.xyna_backup_store_retries", -1);// -1 means no limit
  public static final XynaPropertyDuration XYNA_BACKUP_STORE_RETRY_WAIT =
      new XynaPropertyDuration("xyna.xprc.xprcods.xyna_backup_store_retry_wait", "2 s", TimeUnit.MILLISECONDS );
  public static final XynaPropertyInt XYNA_BACKUP_READ_RETRIES = 
      new XynaPropertyInt("xyna.xprc.xprcods.xyna_backup_read_retries", -1);
  public static final XynaPropertyDuration XYNA_BACKUP_READ_RETRY_WAIT =
      new XynaPropertyDuration("xyna.xprc.xprcods.xyna_backup_read_retry_wait", "2 s", TimeUnit.MILLISECONDS );
 
  // this is probably not a XynaPropertyBoolean since the mechanism does not work during updates before factory initialization
  // this would require a generalization of XynaPropertyBoolean to work in that situation
  public static final String XYNA_DISABLE_XSD_VALIDATION = "xyna.disable.xsd.validation";

  public static final XynaPropertyBoolean REMOVE_GENERATED_FILES =
      new XynaPropertyBoolean("xyna.xprc.xfractwfe.remove_generated_files", true).setHidden(true);
  
  public static final XynaPropertyBoolean XYNAOBJECT_HAS_GENERATED_TOSTRING =
      new XynaPropertyBoolean("xyna.xprc.xfractwfe.xynaobject_has_generated_tostring_method", false);
  
  public static final XynaPropertyDuration DEPLOYMENT_RELOAD_TIMEOUT = 
      new XynaPropertyDuration("xprc.xfractwfe.deployment_timeout", "15 s" );
  
  /**
   * @see com.gip.xyna.xprc.xsched.XynaScheduler#pauseScheduling(boolean,boolean) XynaScheduler.pauseScheduling
   */
  public static final XynaPropertyDuration XYNA_SCHEDULER_STOP_TIMEOUT_OFFSET =
      new XynaPropertyDuration("xyna.scheduler.stop.timeout.offset", "0 ms", TimeUnit.MILLISECONDS).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Diese Property bewirkt, dass Aufträge vor dem Herunterfahren des Servers bereits "+
                              "ins Timeout laufen, falls die verbleibende Zeit bis zum Timeout kleiner ist "+
                              "als der angegebene Wert.");

  /**
   */
  public static final XynaPropertyDuration TIMEOUT_SUSPENSION = 
      new XynaPropertyDuration("xyna.ordersuspension.workflowstep.timeout", "5 s", TimeUnit.MILLISECONDS).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Maximale Wartezeit auf langlaufende Service-aufrufe (dann werden die Threads "+
                              "interrupted bzw. gekillt). Wird halbiert in zwei Intervalle, das eine ist die "+
                              "maximale Wartezeit bis zum Interrupt, das zweite bis zum Kill.");

  public static final XynaPropertyDuration TIMEOUT_SHUTDOWN_ORDERS_IN_CLEANUP = 
      new XynaPropertyDuration("shutdown.timeout.cleanup", "5 s", TimeUnit.MILLISECONDS).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "So lange maximal auf Aufträge im Cleanup warten, nachdem der Scheduler "+
                              "angehalten wurde.");
  public static final XynaPropertyDuration TIMEOUT_SHUTDOWN_ORDERS_IN_PLANNING = 
      new XynaPropertyDuration("shutdown.timeout.planning", "5 s", TimeUnit.MILLISECONDS).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "So lange maximal auf Aufträge im Planning warten, nachdem der Scheduler "+
                              "angehalten wurde.");

  public static final XynaPropertyDuration TIMEOUT_SHUTDOWN_ACTIVE_OPERATIONS = 
                  new XynaPropertyDuration("shutdown.timeout.activeoperations", "10 s", TimeUnit.MILLISECONDS).
                  setDefaultDocumentation(DocumentationLanguage.DE, 
                                          "So lange maximal auf laufende Operations warten, bis die Xyna Factory heruntergefahren wird.");

  /**
   * boolean properties that define the persistence mode false: the component will only persist on shutdown true: the
   * component will persist every change
   */
  // used in Configuration
  public static final XynaPropertyBoolean CONFIGURATION_DIRECTPERSISTENCE = new 
      XynaPropertyBoolean("xyna.properties.persistence", true );
  //used in CapacityManagement & CapacityMappingDatabase
  public static final String CAPACITIES_DIRECTPERSISTENCE = "xyna.capacities.persistence";
  // use in MonitoringDispatcher
  public static final String MONITORING_DIRECTPERSISTENCE = "xyna.monitoring.persistence";

  //FIXME die gehören in Constants-Klasse
  /**
   * Persistence Constants (used in JavaSerializationPersistenceLayer) PERSISTENCE_DIR: Directory to persist to
   * INDEX_SUFFIX: Suffix of the index file
   */
  public static final String PERSISTENCE_DIR = "persdir";
  public static final String INDEX_SUFFIX = ".index";
  
  /*
   * Properties für BlackExceptionCodeManagement
   */
  public static final XynaPropertyBoolean RELOAD_FROM_STORAGE_EACH_ACTION = 
      new XynaPropertyBoolean("xyna.exceptions.codegroup.storage.reload", true ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              null).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Soll der Inhalt der Tabellen 'codegroup' und 'codepattern' bei jeder "+
                              "Verwendung komplett neu gelesen werden?" );
  //
  public static final XynaPropertyBoolean AUTOMATIC_CODEGROUP_GENERATION_AND_EXTENSION = 
      new XynaPropertyBoolean("xyna.exceptions.codegroup.extension.automatic", true ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              null).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Sollen automatisch neue CodeGroups angelegt werden, wenn keine verwendbare " +
                              "CodeGroup existiert? 'false' zwingt zur manuellen Anlage von CodeGroups");
  //
  public static final XynaPropertyBuilds<CodeGroupPattern> AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPATTERN = 
      new XynaPropertyBuilds<CodeGroupPattern>("xyna.exceptions.codegroup.extension.defaultpattern", new CodeGroupPattern("DEVEL-[[]]") ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              null).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Beim automatischen Anlegen neuer CodeGroups wird der Name " +
                              "durch dieses Pattern bestimmt.");
  //
  public static final XynaPropertyInt AUTOMATIC_CODEGROUP_GENERATION_DEFAULTPADDING =
      new XynaPropertyInt("xyna.exceptions.codegroup.extension.defaultpadding", 5 ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              null).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Beim automatischen Anlegen neuer CodeGroups wird die Anzahl der Ziffern "+
                              "im ExceptionCode durch diese Property bestimmt.");

  
  /**
   * Session-Timeout in Seconds
   */
  public static final XynaPropertyDurationCompatible<Long> GUI_SESSION_TIMEOUT_SECONDS = 
      new XynaPropertyDurationCompatible<Long>("xyna.xfmg.xopctrl.managedsessions.timeout", SessionManagement.DEFAULT_GUI_SESSION_TIMEOUT_SECONDS)
        .setDefaultDocumentation(DocumentationLanguage.DE, "Gültigkeitsdauer einer Sitzung in Sekunden.")
        .setDefaultDocumentation(DocumentationLanguage.EN, "Lifetime of a session in seconds.");
  
  public static final XynaPropertyDurationCompatible<Long> GUI_SESSION_DELETION_INTERVAL =
      new XynaPropertyDurationCompatible<Long>("xyna.xfmg.xopctrl.managedsessions.sessiondeletioninterval", SessionManagement.DEFAULT_SESSION_DELETION_INTERVAL)
        .setDefaultDocumentation(DocumentationLanguage.DE, "Häufigkeit des Aufräumens verwaister Sessions zur Freigabe von von der Session belegten Ressourcen. Eine Session gilt als verwaist, wenn sie abgelaufen ist, aber nicht beendet ist.")
        .setDefaultDocumentation(DocumentationLanguage.EN, "Time interval in which abandonded sessions are cleared and associated resources released. Sessions count as abandoned if they are timed out but not yet cleared.");
  
  public static final XynaPropertyBoolean CREATE_MULTIPLE_SESSIONS_IF_ALLOWED = new XynaPropertyBoolean("xfmg.xopctrl.managedsessions.createMultipleSessionsIfAllowed", false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Falls gesetzt dürfen Benutzer mit einer Rolle mit dem Recht MULTIPLE_SESSION_CREATION mehrere Sitzungen gleichzeitig geöffnet haben.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "When true, users having a role with the right MULTIPLE_SESSION_CREATION can have multiple concurrent sessions.");


  public static final XynaPropertyInt THREADPOOL_PLANNING_MINTHREADS = new XynaPropertyInt("xyna.threadpool.planning.min", 10);
  public static final XynaPropertyInt THREADPOOL_PLANNING_MAXTHREADS = new XynaPropertyInt("xyna.threadpool.planning.max", 100);
  public static final XynaPropertyString THREADPOOL_PLANNING_POOL_SIZE_STRATEGY = 
      new XynaPropertyString("xyna.threadpool.planning.pool_size_strategy", "eager").
      setDefaultDocumentation(DocumentationLanguage.DE, "Konfiguration des Verhaltens bei ausgelastetem ThreadPool, der "+
          "seine maximale Größe jedoch noch nicht erreicht hat: "+
          "'eager': Jeder neue Task erhält sofort einen Thread. "+
          "'lazy_<n>': (Beispiel 'lazy_2') Für jeweils <n> in der Queue wartende Tasks wird ein neuer Thread gestartet. "+
          "'default': Neue Tasks kommen in die Queue").
      setDefaultDocumentation(DocumentationLanguage.EN, null );
  
  public static final XynaPropertyDuration THREADPOOL_PLANNING_KEEP_ALIVE = new XynaPropertyDuration("xyna.threadpool.planning.keepalive.seconds", "30 s");
  public static final XynaPropertyInt THREADPOOL_PLANNING_QUEUE_SIZE = new XynaPropertyInt("xyna.threadpool.planning.queuesize", 1000);
  public static final XynaPropertyBoolean THREADPOOL_PLANNING_USE_RINGBUFFER = new XynaPropertyBoolean("xyna.threadpool.planning.useringbuffer", false);
  public static final XynaPropertyInt THREADPOOL_CLEANUP_MINTHREADS = new XynaPropertyInt("xyna.threadpool.cleanup.min", 10);
  public static final XynaPropertyInt THREADPOOL_CLEANUP_MAXTHREADS = new XynaPropertyInt("xyna.threadpool.cleanup.max", 500);
  public static final XynaPropertyDuration THREADPOOL_CLEANUP_KEEP_ALIVE = new XynaPropertyDuration("xyna.threadpool.cleanup.keepalive.seconds", "30 s");
  public static final XynaPropertyInt THREADPOOL_CLEANUP_QUEUE_SIZE = new XynaPropertyInt("xyna.threadpool.cleanup.queuesize", 0);
  public static final XynaPropertyInt THREADPOOL_EXECUTION_MINTHREADS = new XynaPropertyInt("xyna.threadpool.execution.min", 0);
  public static final XynaPropertyInt THREADPOOL_EXECUTION_MAXTHREADS = new XynaPropertyInt("xyna.threadpool.execution.max", 2000);
  public static final XynaPropertyDuration THREADPOOL_EXECUTION_KEEP_ALIVE = new XynaPropertyDuration("xyna.threadpool.execution.keepalive.seconds", "60 s");
  public static final XynaPropertyInt THREADPOOL_DEPLOYMENT_MAXTHREADS = new XynaPropertyInt("xyna.threadpool.deployment.max", 200);
  public static final XynaPropertyInt THREADPOOL_DEPLOYMENT_KEEP_ALIVE = new XynaPropertyInt("xyna.threadpool.deployment.keepalive.seconds",60);
  public static final XynaPropertyInt THREADPOOL_WAREHOUSE_CURSOR_THREADS = new XynaPropertyInt("xyna.threadpool.warehouse.cursor.threads", 15);
  public static final XynaPropertyDuration THREADPOOL_WAREHOUSE_CURSOR_KEEP_ALIVE = new XynaPropertyDuration("xyna.threadpool.warehouse.cursor.keepalive", "10 s");
  
  /**
   * wie lange wartet ein client auf eine antwort (sekunden)
   */
  public static final XynaPropertyDurationCompatible<Integer> RMI_TIMEOUT = new XynaPropertyDurationCompatible<Integer>("xyna.rmi.timeout", Constants.RMI_DEFAULT_TIMEOUT);
  public static final XynaPropertyDuration RMI_SERVER_SOCKET_TIMEOUT = new XynaPropertyDuration("xyna.rmi.timeout.server", "600 s");
  public static final XynaPropertyInt RMI_PORT_REGISTRY = new XynaPropertyInt("xyna.rmi.port.registry", Registry.REGISTRY_PORT);
  public static final XynaPropertyString RMI_HOSTNAME_REGISTRY = new XynaPropertyString("xyna.rmi.hostname.registry", "localhost");
  
  //XynaMultiChannelPortal
  //TODO Property-Namen anpassen?
  public static final XynaPropertyInt RMI_XMCP_PORT_COMMUNICATION = new XynaPropertyInt("xyna.rmi.port.communication", 0);
  
  //Factory-InterLink TODO Dokumentation
  //nur verwendet vom localem InterFactoryLinkChannel bzw. InterFactoryLinkProfile.OrderExecution
  public static final XynaPropertyString RMI_IL_HOSTNAME_REGISTRY = new XynaPropertyString("xyna.rmi.interlink.hostname.registry", "localhost");
  public static final XynaPropertyInt RMI_IL_PORT_REGISTRY = new XynaPropertyInt("xyna.rmi.interlink.port.registry", Registry.REGISTRY_PORT);
  public static final XynaPropertyInt RMI_IL_PORT_COMMUNICATION = new XynaPropertyInt("xyna.rmi.interlink.port.communication", 0);
  public static final XynaPropertyString RMI_IL_SSL_KEYSTORE_TYPE = new XynaPropertyString("xyna.rmi.interlink.ssl.keystore.type", "JKS");
  public static final XynaPropertyString RMI_IL_SSL_KEYSTORE_PASSPHRASE = new XynaPropertyString("xyna.rmi.interlink.ssl.keystore.passphrase", null);
  public static final XynaPropertyString RMI_IL_SSL_KEYSTORE_FILE = new XynaPropertyString("xyna.rmi.interlink.ssl.keystore.file", null); //falls nicht gesetzt, wird kein ssl verwendet
  public static final XynaPropertyString RMI_IL_SSL_TRUSTSTORE_TYPE = new XynaPropertyString("xyna.rmi.interlink.ssl.truststore.type", "JKS");
  public static final XynaPropertyString RMI_IL_SSL_TRUSTSTORE_PASSPHRASE = new XynaPropertyString("xyna.rmi.interlink.ssl.truststore.passphrase", null);
  public static final XynaPropertyString RMI_IL_SSL_TRUSTSTORE_FILE = new XynaPropertyString("xyna.rmi.interlink.ssl.truststore.file", null);
  //client timeouts, servertimeout ist oben RMI_SERVER_SOCKET_TIMEOUT
  public static final XynaPropertyDuration RMI_IL_SOCKET_TIMEOUT = new XynaPropertyDuration("xyna.rmi.interlink.timeout", "180 s");
  public static final XynaPropertyDuration RMI_IL_SOCKET_TIMEOUT_MONITORING = new XynaPropertyDuration("xyna.rmi.interlink.timeout.monitoring", "180 s");
  public static final XynaPropertyDuration RMI_IL_SOCKET_TIMEOUT_RTC_MGMT = new XynaPropertyDuration("xyna.rmi.interlink.timeout.runtimecontextmgmt", "180 s");
  public static final XynaPropertyDuration RMI_IL_SOCKET_TIMEOUT_FILE_MGMT = new XynaPropertyDuration("xyna.rmi.interlink.timeout.filemgmt", "180 s");
  public static final XynaPropertyBoolean RMI_IL_SOCKET_USE_COMPRESSION =
      new XynaPropertyBoolean("xyna.rmi.interlink.ssl.compression", false).setDefaultDocumentation(DocumentationLanguage.EN,
                                                                                                  "Compress rmi ssl communication?");
  public static final XynaPropertyInt RMI_IL_SOCKET_COMPRESSION_BUFFERSIZE =
      new XynaPropertyInt("xyna.rmi.interlink.ssl.compression.buffersize", 1024*128);


  /**
   * Path to and including grep (for logScanning) Examples: "/bin/grep" "/usr/xpg4/bin/grep"
   */
  public static final String PATH_TO_SCANGREP = "xyna.xfmg.processmonitoring.greppath";
  public static final String PATH_TO_SCANGREP_DEFAULT_VALUE = "grep";

  /**
   * Path to factory-logfile
   */
  public static final String PATH_TO_LOG = "xyna.xfmg.processmonitoring.logpath";
  public static final String PATH_TO_LOG_DEFAULT_VALUE = "/var/log/xyna.log";

  public static final XynaPropertyBoolean XYNA_PROCESS_MONITOR_SHOW_STEP_MILLISECONDS =
      new XynaPropertyBoolean("xyna.xfmg.processmonitoring.show_step_milliseconds", true).setHidden(true);

  /**
   * UserManagement-Stuff
   */
  public static final String PROPERTYNAME_ALLOWED_ENTRIES = "xyna.opctrl.allowedtries";
  public static final String DEFAULT_DOMAINS_FOR_NEW_USERS = "xyna.xfmg.xopctrl.usermanagement.defaultdomains";
  public static final String GLOBAL_DOMAIN_OVERWRITE = "xyna.xfmg.xopctrl.usermanagement.globaloverwrite";
  public static final String PASSWORD_RESTRICTIONS = "xyna.xfmg.xopctrl.usermanagement.passwordrestrictions";
  public static final String SCOPED_RIGHTS_CACHE_SIZE_PROPERTY_NAME = "xyna.xfmg.xopctrl.usermanagement.scopedrights.cachesize";
  
  public static XynaPropertyEnum<CreationAlgorithm> PASSWORD_CREATION_HASH_ALGORITHM = new XynaPropertyEnum<CreationAlgorithm>("xyna.xfmg.xopctrl.usermanagement.login.hashalgorithm", CreationAlgorithm.class, CreationAlgorithm.MD5).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Hash algorithm used for encryption during login. Valid algorithms are: " + Arrays.toString(CreationAlgorithm.values()));
  public static XynaPropertyEnum<CreationAlgorithm> PASSWORD_PERSISTENCE_HASH_ALGORITHM = new XynaPropertyEnum<CreationAlgorithm>("xyna.xfmg.xopctrl.usermanagement.persistence.hashalgorithm", CreationAlgorithm.class, null).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Hash algorithm used for encryption during persistence. Valid algorithms are: " + Arrays.toString(CreationAlgorithm.values()) 
                                          + ". For BCRYPT the column 'password' in table 'userarchive' must have a length of 60.");
  public static XynaPropertyInt PASSWORD_CREATION_ROUNDS = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.login.rounds", null).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Number of rounds for encryption during login.");
  public static XynaPropertyInt PASSWORD_PERSISTENCE_ROUNDS = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.persistence.rounds", 10).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Number of rounds for encryption during persistence.");
  public static XynaPropertyString PASSWORD_CREATION_STATIC_SALT = new XynaPropertyString("xyna.xfmg.xopctrl.usermanagement.login.staticsalt", null, true).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Static salt for encryption using MD5 or SHA256 during login.");
  public static XynaPropertyInt PASSWORD_PERSISTENCE_SALT_LENGTH = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.persistence.salt.length", 16).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Salt length for encryption using MD5 or SHA256 during persistence.");
  public static XynaPropertyEnum<RecalculateHash> PASSWORD_PERSISTENCE_RECALCULATE = new XynaPropertyEnum<RecalculateHash>("xyna.xfmg.xopctrl.usermanagement.persistence.recalculate", RecalculateHash.class, RecalculateHash.never).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Determines if a new hash is to be calculated on storage. Valid values are: " + Arrays.toString(RecalculateHash.values()));
  public static XynaPropertyInt SCOPED_RIGHTS_CACHE_SIZE = new XynaPropertyInt(SCOPED_RIGHTS_CACHE_SIZE_PROPERTY_NAME, 10);
  public static XynaPropertyInt PASSWORD_HISTORY_SIZE = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.password.history.size", 0).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Number of old passwords which must not be reused.");
  public static XynaPropertyInt PASSWORD_STORED_HISTORY_SIZE = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.password.stored.history.size", -1).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Number of stored old passwords. -1 means infinite history.");
  public static XynaPropertyInt PASSWORD_EXPIRATION_DAYS = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.password.expiration.days", -1).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Number of days after which a password has to be renewed. -1 means no expiry date.");
  public static XynaPropertyInt PASSWORD_EXPIRATION_CHANGEALLOWED_DURATION_DAYS = new XynaPropertyInt("xyna.xfmg.xopctrl.usermanagement.password.expiration.changeallowed.duration.days", 0).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Period (in days) in which an expired password can still be changed. -1 means that passwords can be changed always.");
  public static XynaPropertyBoolean PASSWORD_SETBYADMIN_INVALID = new XynaPropertyBoolean("xyna.xfmg.xopctrl.usermanagement.password.setbyadmin.invalid", false).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Determines whether a password must be changed by the user, if it was set by an admin.");
  public static XynaPropertyBoolean PASSWORD_EXPIRATION_EXCPETION_UNIQUE = new XynaPropertyBoolean("xyna.xfmg.xopctrl.usermanagement.password.expiration.exception.unique", true).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Determines whether a unique exception is thrown if an attempt is made to change an expired password.");
  
  
  
  public static final XynaPropertyEnum<ConstantRateEventCreationAlgorithm.HighRateThreadSleepType> FQCTRL_HIGH_RATE_THREAD_SLEEP_TYPE = 
      XynaPropertyEnum.construct("xyna.xfqctrl.rate.threadsleep.type",
                                 ConstantRateEventCreationAlgorithm.HighRateThreadSleepType.NORMALSLEEP);
  public static final XynaPropertyDuration FQCTRL_HIGH_RATE_THREAD_SLEEP_MINVALUE =
      new XynaPropertyDuration("xyna.xfqctrl.rate.threadsleep.minvalue", "30 ms", TimeUnit.MILLISECONDS );


  /**
   * alternative topologies tabelle muss so konfiguriert sein, dass das db-file zur tabelle dem ort entspricht, der in
   * dieser xynaproperty steht, damit export von topologies funktioniert.
   */
  public static final String TOPOLOGY_EXPORT_FILE_LOCATION = "xyna.xdev.topology.persistence.alternative.file.location";

  /**
   * Clustering
   */
  public static final XynaPropertyDurationCompatible<Long> CLUSTERING_TIMEOUT_CAPACITY_MIGRATION =
      new XynaPropertyDurationCompatible<Long>("xyna.xnwh.xcs.timeout.capacity.migration", 10000L, TimeUnit.MILLISECONDS);
  public static final XynaPropertyDurationCompatible<Long> CLUSTERING_TIMEOUT_ORDER_MIGRATION =
      new XynaPropertyDurationCompatible<Long>("xyna.xnwh.xcs.timeout.order.migration", 10000L, TimeUnit.MILLISECONDS);

  /**
   * Project MI-Workflow
   */
  public static final String CUSTOM_MANUAL_INTERACTION_WORFLOW_XMLFQNAMES =
      "xyna.xprc.xpce.manualinteraction.customworkflows";

  /**
   * if set to true will not send OrdertypeManagement.internalOrdertypes as
   */
  public static final String HIDE_INTERNAL_ORDERS = "xyna.xfmg.xods.hideinternalorders";

  /**
   * CronLikeTimer Queue boundaries
   */
  public static final XynaPropertyInt CRON_LIKE_TIMER_UPPER_BOUND = new XynaPropertyInt("xyna.xprc.xsched.queue.max", 250);

  /**
   * Save Strings in OrderInstanceBackup as CLOB or BLOB
   */
  public static final XynaPropertyBoolean ORDER_INSTANCE_BACKUP_STORE_AUDITXML_BINARY =
      new XynaPropertyBoolean("xyna.xprc.xprcods.orderarchive.auditxml.binary", false).
      setDefaultDocumentation(DocumentationLanguage.EN, "").
      setDefaultDocumentation(DocumentationLanguage.DE, "OrderInstanceBackup speichern: true: BLOB, false CLOB");

  /**
   * Specifies how long the await-thread will wait for the notify before it will suspend its thread.
   */
  public static final XynaPropertyDurationCompatible<Integer> XPRC_SYNCHRONIZATION_WAIT_ACTIVELY_FOR_RESPONSE_TIMEOUT_MILLISECONDS =
      new XynaPropertyDurationCompatible<Integer>("xyna.xprc.synchronization.activewait.timeout", 0, TimeUnit.MILLISECONDS);
  
  /**
   * wie lange werden antworten im synchronizationmanagement vorgehalten, für die noch keine anfragen gekommen sind, bevor sie
   * verworfen werden.
   */
  public static final XynaPropertyDurationCompatible<Integer> XPRC_SYNC_ANSWER_TIMEOUT_SECONDS =
      new XynaPropertyDurationCompatible<Integer>("xyna.xprc.synchronization.answer.default_timeout", 120 ); 


  /**
   * maximale anzahl von aufträgen in memory
   */
  public static final XynaPropertyInt MAX_ORDERS_IN_SCHEDULER_MEMORY =
      new XynaPropertyInt("xyna.xprc.xsched.orders.memory.max", 10000);

  /**
   * Sets the log level with with stepwise input and output is logged
   */
  public static final String XYNA_STEP_LOG_HANDLERS_LOGLEVEL = "xyna.xprc.xfractwfe.stephandler.loglevel";


  public static final XynaPropertyInt CONCURRENCY_LISTCAPACITYINFO_CALLS =
      new XynaPropertyInt("xyna.concurrencycalls.listcapacityinfo", 1);
  public static final XynaPropertyInt CONCURRENCY_LISTSCHEDULERINFO_CALLS =
      new XynaPropertyInt("xyna.concurrencycalls.listschedulerinfo", 1);
  public static final XynaPropertyInt CONCURRENCY_LISTORDERSERIESINFO_CALLS =
      new XynaPropertyInt("xyna.concurrencycalls.listorderseriesinfo", 1);
  public static final XynaPropertyInt CONCURRENCY_SEARCHORDERARCHIVE_CALLS =
      new XynaPropertyInt("xyna.concurrencycalls.searchorderarchive", 10);


  /**
   * Verhalten des Schedulers bei unerwarteten Exceptions
   */
  public static final XynaPropertyEnum<XynaSchedulerCustomisation.ExceptionReaction> SCHEDULER_OOM_ERROR_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xsched.reaction.on.oom.error",
                                 XynaSchedulerCustomisation.ExceptionReaction.HaltScheduler);
  public static final XynaPropertyEnum<XynaSchedulerCustomisation.ExceptionReaction> SCHEDULER_GENERAL_EXCEPTION_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xsched.reaction.on.general.exception",
                                 XynaSchedulerCustomisation.ExceptionReaction.HaltScheduler);

  /**
   * Verhalten des Schedulers bei Problemen mit Capacities
   */
  public static final XynaPropertyEnum<CapacityManagement.CapacityProblemReaction> SCHEDULER_UNDEFINED_CAPACITY_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xsched.reaction.on.order.with.undefined.capacity",
                                 CapacityManagement.CapacityProblemReaction.Wait);
  public static final XynaPropertyEnum<CapacityManagement.CapacityProblemReaction> SCHEDULER_UNSUFFICIENT_CAPACITY_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xsched.reaction.on.order.with.too.high.capacity.cardinality",
                                 CapacityManagement.CapacityProblemReaction.Wait);

  
  /**
   * Verhalten des Schedulers bei Problemen mit TimeConstraints
   */
  public static final XynaPropertyEnum<TimeConstraintManagement.TimeConstraintProblemReaction> SCHEDULER_UNDEFINED_TIME_WINDOW_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xsched.reaction.on.order.with.undefined.timewindow",
                                 TimeConstraintManagement.TimeConstraintProblemReaction.Wait).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "Reaction on undefined time window: "
                                  +TimeConstraintManagement.TimeConstraintProblemReaction.documentation(DocumentationLanguage.EN)).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Reaktion auf undefiniertes Zeitfenster: "
                                  +TimeConstraintManagement.TimeConstraintProblemReaction.documentation(DocumentationLanguage.DE)); 
  public static final XynaPropertyDuration SCHEDULER_CLOSED_TIMEWINDOW_REMOVE_TIME_OFFSET = new 
      XynaPropertyDuration("xyna.xprc.xsched.timeduration_before_remove_waiting_order_from_scheduling", "0 ms", TimeUnit.MILLISECONDS ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "Orders waiting for their start time are temporarily removed from the "
                                  +"scheduling procedure and reinserted when their start time has come. "
                                  +"This property defines a time offset how long a waiting order will "
                                  +"remain in the scheduling procedure before being swapped" ).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Aufträge, die auf ihren Startzeitpunkt warten müssen, werden temporär aus dem Scheduling "
                                  +"entfernt und bei Erreichen des Startzeitpunkts wieder eingetragen. "
                                  +"Diese Property definiert die Zeitdauer, die ein auf seine Startzeit wartender Auftrag "
                                  +"im Scheduler verbleiben darf, ohne ausgelagert zu werden."); 

  public static final XynaPropertyInt SCHEDULER_MAX_RETRIES = new 
      XynaPropertyInt("xyna.xprc.xsched.max_retries", 10 ).
      setDefaultDocumentation(DocumentationLanguage.EN,
                              "Maximum number of retries for an order during a single scheduling loop - "
                                  +"after reaching the maximum retry number the scheduling loop continues with "
                                  +"the subsequent orders." ).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Maximale Anzahl Retries, die ein Auftrag in einem Scheduling-Durchlauf versuchen darf - "
                                  +"nach dem Erreichen der maximalen Retry-Anzahl wird der Scheduling-Durchlauf mit "
                                  +"dem nächsten Auftrag fortgesetzt."); 

  public static final XynaPropertyBoolean SCHEDULER_WAIT_FOR_STABLE_TIME_WINDOWS = new
      XynaPropertyBoolean("xyna.xprc.xsched.wait_for_stable_time_windows", true ).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "This property determines for cluster mode, whether orders waiting in the scheduler " +
                              "needing a time window will wait until time windows are present in a stable state " +
                              "(i.e. time windows cannot be changed by the other node without the " +
                              "local node getting notice of it) \u2013 value true \u2013 " +
                              "or if time windows will be reconstructed temporarily with the risk that the " +
                              "other node can make changes to the time windows that are not recognized " +
                              "by the local node \u2013 value false. " +
                              "Default value is true." ).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Diese Property gibt im Cluster-Betrieb vor, ob Aufträge im Scheduler, " +
                              "die ein Zeitfenster benötigen, warten, bis die Zeitfenster stabil vorliegen " +
                              "(d.h. es kann nicht passieren, dass der andere Knoten Änderungen an den " +
                              "Zeitfenstern vornimmt, die der lokale Knoten nicht sieht) \u2013 Wert true \u2013 " +
                              "oder ob die Zeitfenster vorläufig wiederhergestellt " +
                              "werden sollen mit der Gefahr, dass der andere Knoten Änderungen daran vornimmt, " +
                              "die noch nicht bemerkt werden können \u2013 Wert false. " +
                              "Default-Wert ist true."); 

  
  /**
   * Capacity-Demand-Kommunikation im Scheduler
   */
  public static final XynaPropertyLong SCHEDULER_CAPACITY_DEMAND_FOREIGN_PENALTY = 
      new XynaPropertyLong( "xyna.xprc.xsched.capacity.demand.foreign.penalty", 0L );
  public static final XynaPropertyInt SCHEDULER_CAPACITY_DEMAND_MAX_PERCENT = 
      new XynaPropertyInt( "xyna.xprc.xsched.capacity.demand.max.percent", 10 );

  /**
   * Default-Priority für XynaOrder<br>
   * Default -1: {@link PriorityManagement#HARDCODED_DEFAULT_PRIORITY} (derzeit 7) wird verwendet
   */
  public static final XynaPropertyInt CONFIGURABLE_DEFAULT_XYNAORDER_PRIORITY = new XynaPropertyInt("xyna.default.priority", -1)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Priority between " + Thread.MIN_PRIORITY + " (lowest) and " + Thread.MAX_PRIORITY
          + " (highest). Every other value is mapped to " + PriorityManagement.HARDCODED_DEFAULT_PRIORITY + ".");

  /**
   * Properties für OrderSeriesManagement
   */
  public static final XynaPropertyInt ORDER_SERIES_MAX_SI_STORABLES_IN_CACHE =
      new XynaPropertyInt("xyna.xprc.xsched.series.max.si.storables.in.cache", 1000 );
  public static final XynaPropertyInt ORDER_SERIES_MAX_PRE_TREES_IN_CACHE = 
      new XynaPropertyInt("xyna.xprc.xsched.series.max.pre.trees.in.cache", 5000 );
  public static final XynaPropertyInt ORDER_SERIES_LOCK_PARALLELISM =           
      new XynaPropertyInt("xyna.xprc.xsched.series.lock.parallelism", 32 );
  public static final XynaPropertyInt ORDER_SERIES_QUEUE_SIZE = 
      new XynaPropertyInt("xyna.xprc.xsched.series.queue.size", 1000 );
  public static final XynaPropertyBoolean ORDER_SERIES_CLEAN_DATABASE = 
      new XynaPropertyBoolean("xyna.xprc.xsched.series.clean.database", false );
  
  /**
   * Properties für SuspendResumeManagement
   */
  public static final XynaPropertyDuration RESUME_RETRY_DELAY = 
      new XynaPropertyDuration("xyna.xprc.xpce.resume_retry_delay", "10 s");
  public static final XynaPropertyBoolean SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO = 
      new XynaPropertyBoolean("xyna.xprc.xpce.ordersuspension.show_srinformation_lock_info", false );
  
  /**
   * Property bestimmt, ob ein nach einem erfolgreichen Killprozess noch die Kompensation durchgeführt werden soll.
   */
  public static final XynaPropertyBoolean ORDERABORTION_COMPENSATE = 
      new XynaPropertyBoolean("xyna.xprc.xsched.orderabortion.compensate", true );
  
  
  public static final XynaPropertyDuration DEPLOYMENTHANDLER_TIMEOUT = 
      new XynaPropertyDuration("xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout", "5 min", TimeUnit.MILLISECONDS );

  public static XynaPropertyString BUILDMDJAR_JAVA_VERSION = new XynaPropertyString("xyna.target.mdm.jar.javaversion", "Java11");
  
  public static XynaPropertyBoolean TRY_PROCEED_ON_COMPILE_ERROR = new XynaPropertyBoolean("xyna.java.compile.tryproceedonerror", true);
  public static XynaPropertyBoolean NO_SINGLE_COMPILE = new XynaPropertyBoolean("xyna.java.compile.nosinglecompile", false).setHidden(true);
  
  public static XynaPropertyDuration MESSAGE_BUS_FETCH_TIMEOUT = 
      new XynaPropertyDuration("xyna.messagebus.request.timeout.millis", "10 s", TimeUnit.MILLISECONDS);
  
  
  public static XynaPropertyDuration RUNTIME_STATISICS_ASYNC_PERSISTENCE_INTERVAL =
      new XynaPropertyDuration("xyna.xfmg.xfmon.frunstats.async_persistence_interval", "30 s", TimeUnit.MILLISECONDS);
  
  /*
   * Properties für BatchProcesse
   */
  public static XynaPropertyInt BATCH_MAX_PARALLELISM = new XynaPropertyInt("xyna.xprc.xbatchmgmt.max_parallelism", 10).
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Maximale Anzahl der gleichzeitig (in einem Schedulerlauf) startbaren Slaves");
  //
  public static XynaPropertyInt BATCH_INPUT_MAX_ROWS = new XynaPropertyInt("xyna.xprc.xbatchmgmt.input_max_rows", 100).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Maximale Anzahl der vom OnTheFly-InputGenerator im Cache gehaltenen Objekte");
  //
  public static XynaPropertyDuration BATCH_CANCEL_WAIT_TIMEOUT = new XynaPropertyDuration("xyna.xprc.xbatchmgmt.cancel_wait_timeout", "5 s").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Beim Cancel eines Batch Processes wird eine zeitlang gewartet, dass die Slaves fertig laufen "+
                              "können; danach werden weiterhin laufende Slaves abgebrochen. Falls kein ExecutionTimeOut für "+
                              "die Slaves gesetzt ist, wird dieser Timeout hier verwendet.");
  //
  public static XynaPropertyString BATCH_DEFAULT_MASTER = new XynaPropertyString("xyna.xprc.xbatchmgmt.default_master",
                                                                                 "xprc.xbatchmgmt.DefaultEmptyMasterWorkflow").
      setDefaultDocumentation(DocumentationLanguage.DE, 
                              "Workflow, der als Batch Process Master verwendet werden soll, wenn keiner angegeben wird.");
  //
  public static final XynaPropertyEnum<BatchProcessManagement.MissingLimitationReaction> BATCH_NO_LIMITATION_REACTION =
      XynaPropertyEnum.construct("xyna.xprc.xbatchmgmt.reaction_on_missing_limitation",
                                 BatchProcessManagement.MissingLimitationReaction.SlowDown).
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "Reaction on missing limitation (no SlaveExecutionPeriod, no Capacities for Slaves): "
                                  +BatchProcessManagement.MissingLimitationReaction.documentation(DocumentationLanguage.EN)).
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Reaktion auf fehlende Limitierung (weder SlaveExecutionPeriod noch Kapazitäten für die Slaves konfiguriert): "
                                  +BatchProcessManagement.MissingLimitationReaction.documentation(DocumentationLanguage.DE)); 

  /*
   * Properties für Node Management
   */
  public static final XynaPropertyString NODE_SSH_SCRIPT = new XynaPropertyString("xyna.xfmg.xfctrl.nodemgmt.ssh_script", "deploy_remote_workflow.sh").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Skript zum Verwalten von Applications auf Remote-Knoten per SSH");

  public static final XynaPropertyString NODE_SSH_SCRIPT_DIR = new XynaPropertyString("xyna.xfmg.xfctrl.nodemgmt.ssh_script_dir", "scripts").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Verzeichnis, in dem das Skript zum Verwalten von Applications auf Remote-Knoten per SSH liegt. Der Pfad muss relativ zum server-Verzeichnis der Xyna Factory angegeben werden. ");

  /*
   * Properties für MasterWorkflowPostScheduler
   */
  public static final XynaPropertyBoolean CLEANUP_WORKFLOW_BEFORE_FREEING_CAP_VETO = new XynaPropertyBoolean("xyna.xprc.xpce.cleanup_workflow_before_freeing_cap_and_veto", false).
      setDefaultDocumentation(DocumentationLanguage.EN,
                              "Cleanup workflow will be called before freeing capacities and vetos.").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Cleanup-Workflow wird vor der Freigabe von Caapcitys und Vetos ausgeführt.");
 
  
  public static final XynaPropertyDouble CAPACITY_DEMAND_IGNORING_PERCENTAGE = new XynaPropertyDouble("xyna.xprc.xsched.capacitydemand_ignore_percentage", 0.05 ).
      setDefaultDocumentation(DocumentationLanguage.EN,
          "CapacityDemand will be ignored with this probability.").
      setDefaultDocumentation(DocumentationLanguage.DE,
          "CapacityDemand wird mit dieser Wahrscheinlichkeit ignoriert und nicht an den anderen Knoten geschickt.");
  
  /*
   * Properties für FileManagement
   */
  public static final XynaPropertyString FILE_MANAGEMENT_TEMP_DIR = new XynaPropertyString("xyna.xfmg.xfctrl.filemgmt.temp_dir", "/tmp/filemgmt").
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "Directory for temporary files from file management.").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Verzeichnis, in dem das Filemanagement temporäre Dateien und Verzeichnisse ablegt.");
  public static final XynaPropertyDuration FILE_MANAGEMENT_DEFAULT_TIMEOUT = new XynaPropertyDuration("xyna.xfmg.xfctrl.filemgmt.default_timeout", "30 min").
      setDefaultDocumentation(DocumentationLanguage.EN, 
                              "Default timeout for uploaded files.").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Standard Zeitlimit bis zur Löschung einer hochgeladenen Datei.");
  public static final XynaPropertyInt FILE_MANAGEMENT_STATE_TRANSITION_RETRIES = new XynaPropertyInt("xyna.xfmg.xfctrl.filemgmt.transition_retries", 100).
                  setDefaultDocumentation(DocumentationLanguage.EN, 
                                          "Amount of retries for a state transition. Should be adjusted according to the expected concurrent access. (< 0 for unlimited retries)").
                  setDefaultDocumentation(DocumentationLanguage.DE,
                                          "Anzahl der Wiederholungsversuchen eines Statusübergangs."
                                       + " Sollte passend zur erwarteten Menge an konkurrierenden Zugriffen angepasst werden. (< 0 für unbeschränkte Wiederholungen)");

  public static final XynaPropertyString S4E_TMP_DIR = new XynaPropertyString("com.gip.xyna.xdev.xlibdev.supp4eclipse.temp_dir", "./").
      setDefaultDocumentation(DocumentationLanguage.EN, "Directory for temparary files from SupportForEcplipse").
      setDefaultDocumentation(DocumentationLanguage.DE, "Verzeichnis, in dem SupportForEclipse temporäre Dateien und Verzeichnisse ablegt.");
 
  /*
   * Properties für ListFurtherInformationFromStartup
   */
  public static final XynaPropertyInt STARTUP_FURTHER_INFO_MAX = new XynaPropertyInt("xyna.xfmg.max_further_information", 1000).
      setDefaultDocumentation(DocumentationLanguage.EN,
                              "Maximum number of further informations on startup kept for each topic").
      setDefaultDocumentation(DocumentationLanguage.DE,
                              "Maximale Anzahl an FurtherInformations beim Startup, die zu einem Thema aufbewahrt werden ");
  
  public static final XynaPropertyInt NUMBER_OF_CLI_COMMANDS_TO_BE_EXECUTED_NON_POOLED = new XynaPropertyInt("xmcp.xfcli.thread.nonpooled.number", 0).
      setDefaultDocumentation(DocumentationLanguage.EN, "Sets the number of CLI commands to be executed with a non pooled thread.");  

  public static final XynaPropertyBoolean INVALIDATE_WF_EXECUTION =
                  new XynaPropertyBoolean("xyna.xprc.xfractwfe.invalidate_wf_execution", true);

  public static final XynaPropertyBoolean CHECK_DEPLOYMENT_STATE_FOR_BUILD_APPLICATION =
      new XynaPropertyBoolean("xyna.xfmg.xfctrl.appmgmt.deploymentstate.check", true);

  public static final XynaPropertyDuration SERVICE_IMPL_INCONSISTENCY_TIME_LAG = new XynaPropertyDuration("xyna.xfmg.xfctrl.deploystate.service_impl_inconsistency_time_lag", "10 s").
                  setDefaultDocumentation(DocumentationLanguage.DE,
                                          "Maximal erlaubte Zeitdifferenz zwischen dem Speichern des ImplJars und dem Speichern bzw. Deployen einer ServiceGroup oder verwendeter Objekte." +
                                          "Ist das ImplJar älter, geht die ServiceGroup den Zustand INVALID");
  
  public static final XynaPropertyBoolean SUPPRESS_USED_OBJECT_IMPL_INCONSISTENCIES =
                  new XynaPropertyBoolean("xyna.xfmg.xfctrl.deploymentstate.used_object_impl_check", false);
  
  public static final XynaPropertyBoolean SUPPRESS_WARNINGS =
                  new XynaPropertyBoolean("xyna.xfmg.xfctrl.deploymentstate.suppresswarnings", true).setHidden(true);

  /**
   * xmomdiscovery beim Startup
   */
  public static final XynaPropertyBoolean XMOMDISCOVERY_ON_STARTUP = new XynaPropertyBoolean("xyna.xfmg.xfctrl.xmomdatabase.discovery_on_startup", false).
                  setDefaultDocumentation(DocumentationLanguage.DE, "Beim Startup wird ein XMOM-Discovery ausgeführt.");

  /**
   * erlaubte Werte für Deployment-Tags
   */
  public static final XynaPropertyString DEPLOYMENT_TAGS = new XynaPropertyString("xyna.xfmg.xfctrl.deploymentmarker.tags", "root workflow, used for tests, has errors, untested, verified").
                  setDefaultDocumentation(DocumentationLanguage.EN, "Set of allowed deployment tags").
                  setDefaultDocumentation(DocumentationLanguage.DE, "Menge der erlaubten Deployment-Tags");

  public static final XynaPropertyBoolean USE_OLD_EXCEPTIONHANDLING_AUTHENTICATION =
      new XynaPropertyBoolean("xyna.xmcp.xopctrl.authentication.exceptionhandling.old", false).setHidden(true);
  public static final XynaPropertyBoolean AUTHENTICATION_LOCKED_EXCEPTION_DIFFERENT =
      new XynaPropertyBoolean("xyna.xmcp.xopctrl.authentication.exceptionhandling.userlocked.differs", false).setDefaultDocumentation(DocumentationLanguage.EN, "If this property is set to true a different exception is thrown when authentication fails because the user is locked.");
  /*
   * versionierung kostet bei monitoringlevel <= 15 performance, und hat das risiko für bugs. bei monitoringlevel > 15 spart es wiederum performance
   * intern verwendete objekte haben immer versionierung an.
   * 
   * änderung von 0/5 auf anderen wert benötigt erneutes deployment
   * 
   * 5 = immer true generiert
   * 3,4 = abhängig von xynaproperty-value (3 = false, 4 = true)
   * 1,2 = reserviert für konfiguration pro ordertype/order (derzeit = 0)
   *    ACHTUNG: konfiguration pro ordertype/order: es muss immer die gesamte objektinstanz-hierarchie versionierung unterstützen, ansonsten gehen änderungen verloren.
   * 
   * 0 = immer false generiert
   */
  public static final XynaPropertyInt useVersioningConfig = new XynaPropertyInt("xyna.xprc.xfractwfe.objectversioning.mode", 4);
  
  public static final XynaPropertyInt DEFAULT_SIZE_COLUMN_TYPE = new XynaPropertyInt("xyna.xnwh.persistence.dbmodifytable.defaultSize", DatabasePersistenceLayerWithAlterTableSupportHelper.DEFAULT_SIZE_COLUMN_TYPE).
                  setDefaultDocumentation(DocumentationLanguage.EN, "Default size for size dependent database columns generated from storables").
                  setDefaultDocumentation(DocumentationLanguage.DE, "Standard-Grösse für grössenabhängige Datenbankspalten welche für Storables erzeugt werden");
  
  public static final XynaPropertyBoolean THROW_EXCEPTION_ON_DUPLICATE_DESTINATION_RESOLUTION =
                  new XynaPropertyBoolean("xyna.xprc.xpce.planning.throwOnDuplicateOrdertype", true)
                        .setDefaultDocumentation(DocumentationLanguage.EN, "Terminate orders if an OrderType collision in the RuntimeContext-Dependency tree is detected.")
                        .setDefaultDocumentation(DocumentationLanguage.DE, "Aufträge welche eine OrderType-Kollision feststellen terminieren mit einer Fehlermledung.");

  public static final XynaPropertyBuilds<PrettyPrintConfig> TABLE_FORMATTER_PRETTY_PRINT = 
      new XynaPropertyBuilds<>("xyna.utils.misc.table_formatter_pretty_print", new PrettyPrintConfig(), new PrettyPrintConfig() ).
      setDefaultDocumentation(DocumentationLanguage.EN, "Separator für TableFormatter (e.g. CLI output), format: 3 chars as string VHC").
      setDefaultDocumentation(DocumentationLanguage.DE, "Trennzeichen für TableFormatter (z.B CLI-Ausgaben), Format: 3 Zeichen als String VHC");
  
  public static class PrettyPrintConfig implements Builder<PrettyPrintConfig> {
    
    private String ppChars = TableFormatter.MaxCellSizeTableRowFormatter.getDefaultPrettyPrintChars();

    @Override
    public PrettyPrintConfig fromString(String string) throws ParsingException {
      if( string.length() != 3 ) {
        throw new ParsingException("Expected 3 chars!");
      }
      ppChars = string;
      TableFormatter.MaxCellSizeTableRowFormatter.configurePrettyPrintChars(ppChars);
      return this;
    }

    @Override
    public String toString(PrettyPrintConfig value) {
      return value.ppChars;
    }

  }

  public static final XynaPropertyBoolean WORKFLOW_DB_SINGLE_BATCH_DEPLOY =
                  new XynaPropertyBoolean("xyna.xprc.xprcods.workflowdb.singlebatchdeploy", true);

  public static final XynaPropertyInt ZETA_TABLE_LIMIT = new XynaPropertyInt("zeta.table.limit", 100).
      setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Tabellen-Einträgen, die zurück gegeben werden.").
      setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of table entries to be returned.");
  
  public static final XynaPropertyInt WAREHOUSE_JOB_BATCH_SIZE = new XynaPropertyInt("xyna.xnwh.xwarehousejobs.defaultBatchSize", 1000).
                  setDefaultDocumentation(DocumentationLanguage.DE, "Die Grösse der Batches die von einem Warehousejob bearbeitet werden.").
                  setDefaultDocumentation(DocumentationLanguage.EN, "The batch size processed from warehousejobs.");

  public static final XynaPropertyBoolean EXCEPTION_ON_DEPLOY_NO_CLASSFILE_UPDATE = 
      new XynaPropertyBoolean("xyna.xprc.xfracfwe.generation.exceptionOnDeployNoClassFileUpdate", true).
      setDefaultDocumentation(DocumentationLanguage.DE, "Soll eine Exception geworfen werden, wenn erfolgreiches Deployment das .class-File nicht updated?").
      setDefaultDocumentation(DocumentationLanguage.EN, "Should an exception be thrown in case a successful deployment does not update the .class file?");

  
  public static final XynaPropertyString XML_HEADER_COMMENT = new XynaPropertyString("xyna.generation.xml.headercomment", "")
      .setDefaultDocumentation(DocumentationLanguage.EN, "This is put into application.xmls and XMOM files as header comment. Changes take effect immediately. Old files are not updated. Should not include <!-- and -->.");

  public static final XynaPropertyBoolean BC_SINGLE_CHARACTER_WILDCARD = new XynaPropertyBoolean("xyna.xnwh.persistence.support_single_character_wildcard", false)
      .setDefaultDocumentation(DocumentationLanguage.EN, "If set to true, \"_\" is replaced by the persistencelayer-specific single character wildcard.")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Wenn diese Property auf true steht, wird \"_\" duch den Persistenzlayer-spezifische Platzhalter für ein einzelnes Zeichen ersetzt.");

}
