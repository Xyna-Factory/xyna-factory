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
package com.gip.xyna.xfmg.xopctrl.usermanagement;



import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.utils.concurrent.HashParallelReentrantReadWriteLocks;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_AccessVerificationException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Localization.Type;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User.ChangeReason;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.CreationAlgorithm;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserColumns;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xfmg.xopctrl.usermanagement.usercontext.UserContextEntryStorable;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;



public class UserManagement extends FunctionGroup implements IPropertyChangeListener {

  static {
    addDependencies(UserManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                        new XynaFactoryPath(XynaProcessing.class, XynaProcessingODS.class),
                        new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                            Configuration.class),
                        new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                            DependencyRegister.class)})));
  }


  // We could rename this to something like XynaMultiChannelRights
  public enum Rights {
    TRIGGER_FILTER_MANAGEMENT(true, true), MONITORING_LEVEL_MANAGEMENT(true, true), PERSISTENCE_MANAGEMENT(true, true),
    START_ORDER(true, true), PROCESS_MANUAL_INTERACTION(true, true), EDIT_MDM(true, true), DEPLOYMENT_MDM(true, true), READ_MDM(true, true),
    USER_MANAGEMENT(true, true), ORDERARCHIVE_VIEW(true, true), ORDERARCHIVE_DETAILS(true, true),
    DISPATCHER_MANAGEMENT(true, true), VIEW_MANUAL_INTERACTION(true, true), SESSION_CREATION(false, true), MULTIPLE_SESSION_CREATION(false, false),
    USER_LOGIN(false, true), FREQUENCY_CONTROL_MANAGEMENT(true, true), FREQUENCY_CONTROL_VIEW(true, true),
    USER_MANAGEMENT_EDIT_OWN(true, true),
    KILL_STUCK_PROCESS(false, true), WORKINGSET_MANAGEMENT(true, true), APPLICATION_MANAGEMENT(true, true),
    APPLICATION_ADMINISTRATION(true, true), 
    TOPOLOGY_MODELLER(false, true) //derzeit nur vom RMI-Proxy geprüft, keine weitere Prüfung in der Factory
    ;

    private boolean usedInGUI; //gibt an, ob das Recht in der GUI verwendet wird
    private boolean mandatoryForAdminRole;

    private Rights(boolean usedInGUI, boolean mandatoryForAdminRole) {
      this.usedInGUI = usedInGUI;
      this.mandatoryForAdminRole = mandatoryForAdminRole;
    }

    public boolean usedInGUI() {
      return usedInGUI;
    }

    public boolean isMandatoryForAdminRole() {
      return mandatoryForAdminRole;
    }
  }
  
  private static final String SRP_ANY_ESCAPED_STRING = "/.*/";
  private static final String SRP_ONLY_STAR = "/\\*/";
  
  /*
   *      / .* / steht für beliebige strings, die escaped werden (z.b. ordertypes, die punkte enthalten etc)
   *         dabei kann zwischen den slashes ein regularer ausdruck stehen.
   *         
   *      *       => '/' + RightScope.WILDCARD_PATTERN + '/'
   *      / \\* / => bisher nur * unterstützt, später soll hier evtl die funktion auf / .* / erweitert werden. Deshalb steht hier im enum auch immer ein TODO
   */
  public enum ScopedRight {
    /**
     * Rechtebereich, um Aufträge zu starten und CronLikeOrders zu verwalten, mit folgenden Teilen:
     * ordertype, applicationName und versionName
     */
    START_ORDER("xprc.xpce.StartOrder", new String[] {SRP_ANY_ESCAPED_STRING,SRP_ANY_ESCAPED_STRING,SRP_ANY_ESCAPED_STRING}),
    
    /**
     * Rechtebereich, um Time Controlled Orders (BatchProzesse) zu verwalten, mit folgenden Teilen:
     * durchzuführende Aktion, ordertype des slaves, applicationName und versionName
     */
    TIME_CONTROLLED_ORDER("xfmg.xfctrl.timeControlledOrders", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.enable + ", " + Action.disable + ", " + Action.kill + ", *]", SRP_ANY_ESCAPED_STRING,SRP_ANY_ESCAPED_STRING,SRP_ANY_ESCAPED_STRING}),
    
    /**
     * Rechtebereich für XynaProperties, mit folgenden Teilen:
     * durchzuführende Aktion (read: XynaProperties anzeigen, write: XynaProperties anlegen und ändern,
     * insert: im Moment keine Funktion, delete: XynaProperties löschen oder *: alle Aktionen)
     * und Name der XynaProperty
     */
    XYNA_PROPERTY("xfmg.xfctrl.XynaProperties", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete +", *]",SRP_ANY_ESCAPED_STRING}),
    
    
    /**
     * Rechtebereich, um Applications anzuzeigen und zu verwalten, mit folgenden Teilen:
     * durchzuführende Aktion, applicationName und versionName
     */
    APPLICATION("xfmg.xfctrl.ApplicationManagement", new String[] {"[" + Action.list + ", " + Action.start + ", " + Action.stop + ", " + Action.deploy + ", " + Action.write + ", " + Action.remove + ", " + Action.migrate + ", *]", SRP_ANY_ESCAPED_STRING, SRP_ANY_ESCAPED_STRING}),
    
    /**
     * Rechtebereich, um ApplicationDefinitions anzuzeigen und zu verwalten, mit folgenden Teilen:
     * durchzuführende Aktion, workspaceName und applicationName
     */
    APPLICATION_DEFINITION("xfmg.xfctrl.ApplicationDefinitionManagement", new String[] {"[" + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]", SRP_ANY_ESCAPED_STRING, SRP_ANY_ESCAPED_STRING}),
    
    WORKSPACE("xfmg.xfctrl.WorkspaceManagement", new String[]{"[" + Action.list + ", " + Action.write + ", *]", SRP_ANY_ESCAPED_STRING}),
    
    /**
     * Zugriffsrechte der Datenmodellverwaltung.
     * TODO: Der zweite Parameter schränkt den Namen der Datenmodelle ein.
     */
    DATA_MODEL("xfmg.xfctrl.dataModels", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete +", *]",SRP_ONLY_STAR}),
    
    /**
     * Zugriffsrechte der Deployment Marker (Tasks und Tags).
     */
    DEPLOYMENT_MARKER("xfmg.xfctrl.deploymentMarker", new String[] {"[" + Action.write + ", " + Action.insert + ", " + Action.delete +", *]"}),
    
    /**
     * Lesezugriff auf die Deployment-Items.
     * 
     * TODO:
     * Der zweite Parameter schränkt den Namen der Deployment-Items ein.
     * Der dritte und vierte Parameter steht bei Runtime Applications für Application und Version. Bei Workspace wird nur der dritte Parameter verglichen.
     */
    DEPLOYMENT_ITEM("xfmg.xfctrl.deploymentItems", new String[] {"[" + Action.read + ", *]",SRP_ONLY_STAR,SRP_ONLY_STAR,SRP_ONLY_STAR}),

    /**
     * Zugriffsrechte der Order Input Source-Verwaltung.
     * Die Zugriffsart generate erlaubt das benutzen einer Order Input Source, also das Generieren von Auftragseingabedaten.
     * 
     * TODO
     * Der zweite Parameter schränkt den Namen der Order Input Sources ein.
     * Der dritte und vierte Parameter steht bei Runtime Applications für Application und Version. Bei Workspace wird nur der dritte Parameter verglichen.
     */
    ORDER_INPUT_SOURCE("xfmg.xfctrl.orderInputSources", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", " + Action.generate + ", *]",SRP_ONLY_STAR,SRP_ONLY_STAR,SRP_ONLY_STAR}),
    
    /**
     * Zugriffsrechte der Cron Like Order-Verwaltung.
     * 
     * TODO
     * Der zweite Parameter schränkt den Namen der Cron Like Orders ein.
     * Der dritte und vierte Parameter steht bei Runtime Applications für Application und Version. Bei Workspace wird nur der dritte Parameter verglichen.
     */
    CRON_LIKE_ORDER("xfmg.xfctrl.cronLikeOrders", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]",SRP_ONLY_STAR,SRP_ONLY_STAR,SRP_ONLY_STAR}),

    /**
     * Zugriffsrechte der Order Type-Verwaltung.
     * 
     * TODO
     * Der zweite Parameter schränkt den Namen des Order Types ein.
     * Der dritte und vierte Parameter steht bei Runtime Applications für Application und Version. Bei Workspace wird nur der dritte Parameter verglichen.
     */
    ORDER_TYPE("xfmg.xfctrl.orderTypes", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]",SRP_ONLY_STAR,SRP_ONLY_STAR,SRP_ONLY_STAR}),

    /**
     * Zugriffsrechte der Capacity-Verwaltung.
     * 
     * TODO
     * Der zweite Parameter schränkt den Namen der Capacities ein.
     */
    CAPACITY("xfmg.xfctrl.capacities", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]",SRP_ONLY_STAR}),

    /**
     * Zugriffsrechte der Administrative Veto-Verwaltung.
     * 
     * TODO
     * Der zweite Parameter schränkt den Namen der Administrative Vetos ein.
     */
    VETO("xfmg.xfctrl.administrativeVetos", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]",SRP_ONLY_STAR}),
    
    /**
     * Rechtebereich, für den lokalen dateizugriff:
     * Zugriffsart, absoluter Pfad
     */
    FILE_ACCESS("base.fileaccess", new String[] {"[" + Action.read + ", " + Action.write + ", " + Action.insert + ", " + Action.delete + ", *]",SRP_ANY_ESCAPED_STRING});
    
    
    //FIXME beim Hinzufügen weiterer ScopedRights bitte auch in 
    //com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.createDefaultRightScopeMap() eintragen
    
    
    private String key;
    private String[] partDefinitions;

    private ScopedRight(String key, String[] partDefinitions) {
      this.key = key;
      this.partDefinitions = partDefinitions;
    }
    
    public String getDefinition() {
      StringBuilder sb = new StringBuilder();
      sb.append(key);
      for (String def : partDefinitions) {
        sb.append(SCOPE_SEPERATOR).append(def);
      }
      return sb.toString();
    }
    
    public String allAccess() {
      StringBuilder sb = new StringBuilder();
      sb.append(key);
      for (int i=0; i<partDefinitions.length; i++) {
        sb.append(SCOPE_SEPERATOR).append("*");
      }
      return sb.toString();
    }
    
    public String getKey() {
      return key;
    }
  }

  /**
   * Sichtbarkeitsrechte für GUI-Komponenten
   */
  public enum GuiRight {
    FACTORY_MANAGER("xmcp.xfm.factoryManager"),
    PROCESS_MODELLER("xmcp.xfm.processModeller"),
    PROCESS_MONITOR("xmcp.xfm.processMonitor"),
    ZETA_PROCESS_MONITOR_ORDERMONITOR("xmcp.xfm.processmonitor.ordermonitor"),
    ZETA_PROCESS_MONITOR_MIMONITOR("xmcp.xfm.processmonitor.mimonitor"),
    ZETA_PROCESS_MONITOR_LIVEREPORTING("xmcp.xfm.processmonitor.livereporting"),
    ZETA_PROCESS_MONITOR_RESOURCEMONITOR("xmcp.xfm.processmonitor.resourcemonitor"),
    ZETA_PROCESS_MODELLER_SHOWXML("xmcp.xfm.processmodeller.debug.showxml"),
    ZETA_PROCESS_MODELLER_STEAL_LOCK("xmcp.xfm.processmodeller.stealLock"),
    TEST_FACTORY("xmcp.xfm.testFactory"),
    ACCESS_CONTROL("xmcp.xfm.acm");

    private String key;

    private GuiRight(String key) {
      this.key = key;
    }
    
    public String getKey() {
      return key;
    }
  }
  
  public enum Action {
    read, write, insert, delete, list, start, stop, deploy, remove, migrate, generate,
    enable, disable, kill,
    
    none; //für ProxyAccess als default nötg
  }
  
  public enum RecalculateHash {
    never, on_every_store, on_algorithm_change;
  }
  
  private static final String SCOPE_SEPERATOR = ":";
  
  private static final String RIGHT_PATTERN = "^([a-z][a-z0-9_]*\\.)+[a-zA-Z][a-zA-Z0-9_]*$";
  protected static final Pattern RIGHT_PATTERN_PATTERN = Pattern.compile(RIGHT_PATTERN);
  private static final String RIGHT_PATTERN_DESCRIPTION = "Regular expression " + RIGHT_PATTERN;
  
  
  public static final String DEFAULT_NAME = "User Management";
  
  public static final String ADMIN_ROLE_NAME = "ADMIN";
  public static final String ADMIN_ROLE_ID = "ADMINXYNA";
  public static final String MODELLER_ROLE_NAME = "MODELLER";
  public static final String MODELLER_ROLE_ID = "MODELLERXYNA";

  public static final String PREDEFINED_LOCALDOMAIN_NAME = "XYNA";
  public static final String DEFAULT_PASSWORD_RESTRICTION = ".*";

  private static final String DESCRIPTION_ADMIN_ROLE = "Xyna-Admin is in possession of every Xyna-Right";
  private static final String DESCRIPTION_MODELLER_ROLE =
      "Xyna-Modeller possesses every right necessary to perform all GUI-Operations";

  private static final String DESCRIPTION_LOCAL_DOMAIN =
      "The local domain contains every role that can directly be authenticated by the local "
          + XynaFactoryCommandLineInterface.XYNA_FACTORY;
  
  public static final String INDENT_FOR_DOMAIN_SPECIFIC_DATA = "     ";

  private static final PreparedQueryCache cache = new PreparedQueryCache();

  private PreparedQuery<User> getUsersWithSpecifiedRole;
  private PreparedQuery<User> loadAllUsersForUpdateQuery;
  private PreparedQuery<PasswordHistoryStorable> getPasswordHistoryForUserQuery;
  private PreparedQuery<Localization> getLocalizationQuery;

  private Map<String, Rights> channelFunctionMapping = new HashMap<>();
  private final ReentrantLock channelFunctionMappingLock = new ReentrantLock();


  private final ReentrantReadWriteLock roleLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock userLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock rightLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock domainLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock rightScopeLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock passwordHistoryLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock localizationLock = new ReentrantReadWriteLock();

  private volatile boolean isShuttingDown = false;

  private ODS ods;

  private boolean globalOverwrite;
  private List<String> globalDomain;
  private List<String> defaultDomains;
  private String passwordRestrictions;
  private List<Pattern> passwordRestrictionPatterns;
  
  private CreationAlgorithm persistenceEncryptionAlgorithm;

  private LruCache<String, ScopedRightCache> scopeCache;


  private static final String GET_USER_CONTEXT_VALUE = "SELECT * FROM " + UserContextEntryStorable.TABLENAME
      + " WHERE " + UserContextEntryStorable.COL_USERNAME + " = ? AND " + UserContextEntryStorable.COL_KEY + " = ?";
  private static final String GET_ALL_USER_CONTEXT_VALUES = "SELECT * FROM " + UserContextEntryStorable.TABLENAME
      + " WHERE " + UserContextEntryStorable.COL_USERNAME + " = ?";

  private HashParallelReentrantReadWriteLocks userContextLocks = new HashParallelReentrantReadWriteLocks();


  public UserManagement() throws XynaException {
    super();
    logger = CentralFactoryLogging.getLogger(UserManagement.class);
    fillChannelMapping();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    initPersistence();

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(UserManagement.class, "UserManagement.initAll").
      after(XynaProperty.class).
      execAsync(new Runnable() { public void run() { initAll(); }});
  }

  private void initAll() {
    try {
      refreshScopedRightCache();
      ensurePredefinedExistence();
      initProperties();
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private void initProperties() {
    XynaFactory.getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.PROPERTYNAME_ALLOWED_ENTRIES,
                       DependencySourceType.XYNAFACTORY, DEFAULT_NAME);
    globalDomain = new ArrayList<>();
    defaultDomains = new ArrayList<>();
    getAndSetIfChangedGlobalDomainOverwrite();
    getAndSetIfChangedDefaultDomainsForNewUsers();
    getAndSetIfChangedPasswordRestrictions();

    persistenceEncryptionAlgorithm = XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.get();

    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().addPropertyChangeListener(this);
    
    XynaProperty.PASSWORD_CREATION_HASH_ALGORITHM.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_CREATION_ROUNDS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_CREATION_STATIC_SALT.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCOPED_RIGHTS_CACHE_SIZE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_PERSISTENCE_ROUNDS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_PERSISTENCE_SALT_LENGTH.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_PERSISTENCE_RECALCULATE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_HISTORY_SIZE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_STORED_HISTORY_SIZE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_EXPIRATION_DAYS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_EXPIRATION_CHANGEALLOWED_DURATION_DAYS.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_SETBYADMIN_INVALID.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.PASSWORD_EXPIRATION_EXCPETION_UNIQUE.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
  }

  
  @SuppressWarnings("unchecked")
  private void initPersistence() throws PersistenceLayerException {
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();

    initUserManagementStorable(ods, User.class, Role.class, Right.class, Domain.class, RightScope.class, UserContextEntryStorable.class);
    ods.registerStorable(PasswordHistoryStorable.class); //fuer PasswordHistory wird nur ConnectionType History verwendet
    ods.registerStorable(Localization.class); // for translations of right descriptions
    
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      Query<User> qUser =
          new Query<>("select name from " + User.TABLENAME + " where " + User.COL_ROLE + "=? ",
                          new User.DynamicUserReader(Collections.singletonList(UserColumns.name)));
      getUsersWithSpecifiedRole = con.prepareQuery(qUser, true);
      Query<User> allUsers =
          new Query<>("select * from " + User.TABLENAME + " for update",
                          User.reader);
      loadAllUsersForUpdateQuery = con.prepareQuery(allUsers, true);
    } finally {
      con.closeConnection();
    }
    
    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Query<PasswordHistoryStorable> getPasswordHistoryForUser = 
                      new Query<> ("select * from " + PasswordHistoryStorable.TABLENAME 
                                      + " where " + PasswordHistoryStorable.COL_USERNAME + "=? order by "
                                      + PasswordHistoryStorable.COL_PASSWORD_INDEX + " desc",
                                      PasswordHistoryStorable.reader);
      getPasswordHistoryForUserQuery = hisCon.prepareQuery(getPasswordHistoryForUser, true);
    } finally {
      hisCon.closeConnection();
    }
    
    hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Query<Localization> getLocalization = 
                      new Query<> ("select * from " + Localization.TABLENAME 
                                      + " where " + Localization.COL_TYPE + "=? AND " + Localization.COL_IDENTIFIER + "=? AND " + Localization.COL_LANGUAGE + "=?",
                                      Localization.reader);
      getLocalizationQuery = hisCon.prepareQuery(getLocalization, true);
    } finally {
      hisCon.closeConnection();
    }
  }
  
  
  private void initUserManagementStorable(ODS ods, Class<? extends Storable>... clazzes) throws PersistenceLayerException {
    for (Class<? extends Storable> clazz : clazzes) {
      ods.registerStorable(clazz);
      
      Persistable persi = Storable.getPersistable(clazz);
      
      boolean areHistoryAndDefaultTheSameForUsers =
        ods.isSamePhysicalTable(persi.tableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
      if (!areHistoryAndDefaultTheSameForUsers) {
        logger.debug("Copying " + persi.tableName() + " from HISTORY to DEFAULT");
        ods.copy(clazz, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);
      }
    }
  }
  

  @Override
  protected void shutdown() throws XynaException {
    isShuttingDown = true;
  }


  boolean isShuttingDown() {
    return isShuttingDown;
  }
  
  /**
   * Liefert eine kommaseparierte Liste aller in der GUI verwendeten Rechte.
   * Diese wird im WebService für die Kompatibilitätseigenschaft 'xuserman.right.guiSpecificRights' benötigt.
   * @return
   */
  public static String getGuiSpecificRights() {
    StringBuilder sb = new StringBuilder();
    for(Rights right : Rights.values()) {
      if (right.usedInGUI) {
        sb.append(right).append(",");
      }
    }
    for(GuiRight guiRight : GuiRight.values()) {
      sb.append(guiRight.getKey()).append(",");
    }
    for(ScopedRight scopedRight : ScopedRight.values()) {
      sb.append(scopedRight.getDefinition()).append(",");
    }
    sb.append(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_DEFINITION);
    return sb.toString();
  }

  
  /**
   * creates a role an empty role (without rights) with the specified name
   */
  public boolean createRole(String name) throws XFMG_DomainDoesNotExistException, PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return createRole(name, PREDEFINED_LOCALDOMAIN_NAME);

  }


  public boolean createRole(String name, String domain) throws XFMG_DomainDoesNotExistException,
      PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    
    validateRoleName(name);

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defCon.ensurePersistenceLayerConnectivity(Domain.class);
      domainLock.readLock().lock();
      try {
        Domain domainToBeChecked = new Domain(domain);

        if (!defCon.containsObject(domainToBeChecked)) {
          throw new XFMG_DomainDoesNotExistException(domain);
        }
      } finally {
        domainLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    Role newRole = new Role(name, domain);
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(newRole);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        if (defCon.containsObject(newRole)) {
          logger.warn("Role '" + name + "' could not be created becaus it does already exist");
          return false;
        }

        persistStorable(defCon, newRole);
        persistStorable(hisCon, newRole);
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Role '" + name + "' was successfully created");
    }

    return true;
  }


  /**
   * deletes the role if it is not a Xyna-Role or it is still assigned to a user
   */
  public boolean deleteRole(String name) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
      XFMG_RoleIsAssignedException {
    return deleteRole(name, PREDEFINED_LOCALDOMAIN_NAME);
  }


  public boolean deleteRole(String name, String domain) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException {
    if (isPredefined(PredefinedCategories.ROLE, new StringBuilder().append(name).append(domain).toString())) {
      logger.warn("Role '" + name + "' could not be deleted because it is a Xyna-Role");
      throw new XFMG_PredefinedXynaObjectException(name, "name");
    }

    Role roleToBeDeleted = new Role(name, domain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(roleToBeDeleted);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {

        if (!defCon.containsObject(roleToBeDeleted)) {
          logger.warn("Role '" + name + "' in domain " + domain + " could not be deleted because it did not exist.");
          return false;
        }

        if (isRoleAssignedToUser(defCon, roleToBeDeleted)) {
          logger.warn("Role '" + name + "' could not be deleted because it is assigned to a user");
          throw new XFMG_RoleIsAssignedException(name);
        }
        
        try {
          deleteStorable(defCon, roleToBeDeleted);
          deleteStorable(hisCon, roleToBeDeleted);
        } finally {
          scopeCache.remove(roleToBeDeleted.getId());
        }
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Role '" + name + "' was successfully deleted");
    }

    return true;
  }


  /**
   * creates the a user with the specified id, role and password
   */
  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return createUser(id, roleName, PREDEFINED_LOCALDOMAIN_NAME, password, isPassHashed);
  }


  public boolean createUser(String id, String roleName, String roleDomain, String password, boolean isPassHashed)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return createUser(id, roleName, roleDomain, password, isPassHashed, this.defaultDomains);
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return createUser(id, roleName, PREDEFINED_LOCALDOMAIN_NAME, password, isPassHashed, domains);
  }


  public boolean createUser(String id, String roleName, String roleDomain, String password, boolean isPassHashed,
                            List<String> domains) throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    if (!isPassHashed) {
      validatePasswordAgainstRestrictions(password);
    }
    validateUserName(id);
    Role assignedRole = new Role(roleName, roleDomain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      roleLock.readLock().lock();
      try {
        if (!defCon.containsObject(assignedRole)) {
          throw new XFMG_RoleDoesNotExistException(roleName);
        }
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    User newUser = new User(id, roleName, password, isPassHashed, domains);
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(newUser);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        if (defCon.containsObject(newUser)) {
          logger.warn("User '" + id + "' does already exist");
          return false;
        }
        persistStorable(defCon, newUser);
        persistStorable(hisCon, newUser);
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    //Passwort in die Historie eintragen
    createPasswordHistoryEntry(newUser);
    
    if (logger.isDebugEnabled()) {
      logger.debug("User '" + id + "' was successfully created");
    }

    return true;
  }

  private static final Pattern namePattern = Pattern.compile("^[\\S ]+$"); //keine whitespaces ausser normalen "spaces".

  private static void validateUserName(String userName) throws XFMG_NameContainsInvalidCharacter {
    validateName(userName, "UserName");
  }

  private static void validateName(String name, String type) throws XFMG_NameContainsInvalidCharacter {
    Matcher m = namePattern.matcher(name);
    if (!m.matches()) {
      throw new XFMG_NameContainsInvalidCharacter(type);
    }
  }
  
  private static void validateDomainName(String domainname) throws XFMG_NameContainsInvalidCharacter {
    validateName(domainname, "DomainName");
  }
  
  
  private static void validateRightName(String rightName) throws XFMG_NameContainsInvalidCharacter {
    validateName(rightName, "RightName");
  }


  private static void validateRoleName(String roleName) throws XFMG_NameContainsInvalidCharacter {
    validateName(roleName, "RoleName");
    if (roleName.contains("\"")) {
      throw new XFMG_NameContainsInvalidCharacter("RoleName");
    }
  }
  
  /**
   * Imports a user If the user already exists it's role and password will be updated to the new data
   * @return true if user could be created or updated
   */
  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return importUser(id, roleName, PREDEFINED_LOCALDOMAIN_NAME, passwordhash);
  }


  public boolean importUser(String id, String roleName, String roleDomain, String passwordhash)
      throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    validateUserName(id);
    boolean roleExists = false;
    Role assignedRole = new Role(roleName, roleDomain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      roleLock.readLock().lock();
      try {
        roleExists = defCon.containsObject(assignedRole);
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    if (!roleExists) {
      try {
        createRole(roleName, roleDomain);
      } catch (XynaException e) {
        //lets continue anyway, it could have been a XynaRole...we could treat that later :D FIXME 
      }
    }

    //we persist it directly to circumvent the existence check
    User newUser = new User(id, roleName, passwordhash, true);
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(newUser);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        persistStorable(defCon, newUser);
        persistStorable(hisCon, newUser);
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    //Passwort in die Historie eintragen
    createPasswordHistoryEntry(newUser);
    return true;
  }


  /**
   * deletes the user if it is not a Xyna-User
   */
  public boolean deleteUser(String id) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    User userToBeDeleted = new User(id);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToBeDeleted);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        if (!defCon.containsObject(userToBeDeleted)) {
          logger.warn("User '" + id + "' could not be found");
          return false;
        }

        deleteStorable(defCon, userToBeDeleted);
        deleteStorable(hisCon, userToBeDeleted);

      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    
    //Passworthistorie löschen
    deletePasswordHistory(id);

    // delete all user context values
    resetUserContextValues(id);
    
    userToBeDeleted.setLocked(false);
    //locked-Markierung im SessionManagement aufheben
    SessionManagement sm = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    sm.lockUser(userToBeDeleted);
    sm.quitSessionsForUser(id);
    
    return true;

  }


  /**
   * Grants one right ('xynafactory.sh listrights' for a listing of all registered rights) to a role
   */
  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException,
      XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException {
    return grantRightToRole(roleName, PREDEFINED_LOCALDOMAIN_NAME, right);
  }


  public boolean grantRightToRole(String roleName, String domain, String right) throws PersistenceLayerException,
      XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException {
    boolean asScopedRight = false;
    if (right.contains(":")) {
      asScopedRight = true;
      String scopeName = right.substring(0, right.indexOf(':'));
      RightScope scope = new RightScope(scopeName);
      
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        defCon.queryOneRow(scope);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false; // TODO custom error
      } finally {
        defCon.closeConnection();
      }
      if (!scope.validate(right)) {
        return false; // TODO custom error
      }
    } else {
      Right rightToGrant = new Right(right);
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
  
      try {
        defCon.ensurePersistenceLayerConnectivity(Right.class);
        rightLock.readLock().lock();
        try {
          if (!defCon.containsObject(rightToGrant)) {
            logger.warn("Right '" + right + "' could not be found");
            throw new XFMG_RightDoesNotExistException(right);
          }
        } finally {
          rightLock.readLock().unlock();
        }
      } finally {
        defCon.closeConnection();
      }
    }

    Role roleToBeChanged = new Role(roleName, domain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(roleToBeChanged);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        try {
          defCon.queryOneRow(roleToBeChanged);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Role '" + roleName + "' could not be found");
          throw new XFMG_RoleDoesNotExistException(roleName);
        }

        if (asScopedRight) {
          if (!roleToBeChanged.grantScopedRight(right)) {
            logger.warn("Role '" + roleName + "' already had the scoped right '" + right + "'");
            return false;
          }
        } else {
          if (!roleToBeChanged.grantRight(right)) {
            logger.warn("Role '" + roleName + "' already had the right '" + right + "'");
            return false;
          }
        }
        
        try {
          persistStorable(defCon, roleToBeChanged);
          persistStorable(hisCon, roleToBeChanged);
        } finally {
          if (asScopedRight) {
            scopeCache.remove(roleToBeChanged.getId());
          }
        }
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Right '" + right + "' was successfully granted to role '" + roleName + "'");
    }

    return true;
  }
  


  /**
   * Revokes one right ('xynafactory.sh listrights' for a listing of all defined rights) from a role
   */
  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException,
      XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException {
    return revokeRightFromRole(roleName, PREDEFINED_LOCALDOMAIN_NAME, right);
  }


  public boolean revokeRightFromRole(String roleName, String domain, String right) throws PersistenceLayerException,
      XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException {
    if (isPredefined(PredefinedCategories.ROLE, new StringBuilder().append(roleName).append(domain).toString())) {
      if (isPredefined(PredefinedCategories.RIGHT, right)) {
        logger.warn("Right '" + right + "' could not be removed from Xyna-User because it is a Xyna-Right");
        return false;
      }
    }

    boolean asScopedRight = right.contains(":");
    
    Role roleToBeChanged = new Role(roleName, domain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(roleToBeChanged);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        try {
          defCon.queryOneRow(roleToBeChanged);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Role '" + roleName + "' could not be found");
          throw new XFMG_RoleDoesNotExistException(roleName);
        }
        
        boolean notSuccess;
        if (asScopedRight) {
          notSuccess = !roleToBeChanged.revokeScopedRight(right);
        } else {
          notSuccess = !roleToBeChanged.revokeRight(right);
        }
        if (notSuccess) {
          logger.warn("Role '" + roleName + "' did not have the right '" + right + "'");
          return false;
        }
        try {
          persistStorable(defCon, roleToBeChanged);
          persistStorable(hisCon, roleToBeChanged);
        } finally {
          if (asScopedRight) {
            scopeCache.remove(roleToBeChanged.getId());
          }
        }
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Right '" + right + "' was successfully removed from role '" + roleName + "'");
    }

    return true;
  }


  /**
   * If 'oldPassword' matches the saved password of the specified user, the password will be set to 'newPassword'
   */
  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed)
      throws PersistenceLayerException, XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_UserDoesNotExistException,
      XFMG_PasswordRestrictionViolation {
    if (!isNewPasswordHashed) {
      validatePasswordAgainstRestrictions(newPassword);
    }
    
    List<PasswordHistoryStorable> passwordHistory = getPasswordHistory(id);
    
    User userToBeChanged = new User(id);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon;
    try {
      hisCon = requiredHistoryConnection(userToBeChanged);
    } catch (RuntimeException re) {
      defCon.closeConnection();
      throw re;
    }
    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        try {
          defCon.queryOneRowForUpdate(userToBeChanged);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("User '" + id + "' could not be found");
          throw new XFMG_UserDoesNotExistException(id);
        }

        try {
          if (!userToBeChanged.changePassword(oldPassword, newPassword, true, isNewPasswordHashed, passwordHistory)) {
            return false;
          }
        } catch (XFMG_UserAuthenticationFailedException e) {
          //evtl hat sich locked status geändert
          persistStorable(defCon, userToBeChanged);
          persistStorable(hisCon, userToBeChanged);
          throw e;
        }

        persistStorable(defCon, userToBeChanged);
        persistStorable(hisCon, userToBeChanged);
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    //neues Passwort in die Historie eintragen
    createPasswordHistoryEntry(userToBeChanged);
    
    if (logger.isDebugEnabled()) {
      logger.debug("Password was successfully changed for user '" + id + "'");
    }

    return true;
  }

  private List<PasswordHistoryStorable> getPasswordHistory(String userName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.ensurePersistenceLayerConnectivity(PasswordHistoryStorable.class);
      passwordHistoryLock.readLock().lock();
      try {
        int maxRows = XynaProperty.PASSWORD_HISTORY_SIZE.get();
        return con.query(getPasswordHistoryForUserQuery, new Parameter(userName), maxRows);
      } finally {
        passwordHistoryLock.readLock().unlock();
      }
    } finally {
      con.closeConnection();
    }
  }


  private void deletePasswordHistory(String user) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.ensurePersistenceLayerConnectivity(PasswordHistoryStorable.class);
      passwordHistoryLock.writeLock().lock();
      try {
        List<PasswordHistoryStorable> pwHistory = con.query(getPasswordHistoryForUserQuery, new Parameter(user), -1);
        con.delete(pwHistory);
        con.commit();
      } finally {
        passwordHistoryLock.writeLock().unlock();
      }
    } finally {
      con.closeConnection();
    }
  }


  /**
   * changes the role of a user if it is not a Xyna-User
   */
  //do we wan't to remove this call? user's can always only be assigned a local role  
  public boolean changeRole(String user, String rolename) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException {
    String domain = PREDEFINED_LOCALDOMAIN_NAME; //Users can only have Xyna-Roles

    Role roleToBeAssigned = new Role(rolename, domain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      roleLock.readLock().lock();
      try {

        if (!defCon.containsObject(roleToBeAssigned)) {
          logger.warn("Role '" + rolename + "' could not be found");
          throw new XFMG_RoleDoesNotExistException(rolename);
        }

        User userToBeChanged = new User(user);
        ODSConnection hisCon = requiredHistoryConnection(userToBeChanged);
        try {
          dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
          userLock.writeLock().lock();
          try {

            try {
              defCon.queryOneRowForUpdate(userToBeChanged);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              logger.warn("User '" + user + "' could not be found");
              throw new XFMG_UserDoesNotExistException(user);
            }

            if (userToBeChanged.getRole().equals(roleToBeAssigned.getName())) {
              logger.warn("User '" + user + "' could not be changed to role '" + rolename + "' because he already has it");
              return false;
            }

            logger.debug("changing role to: " + rolename);
            userToBeChanged.setRole(roleToBeAssigned.getName());
            persistStorable(defCon, userToBeChanged);
            persistStorable(hisCon, userToBeChanged);
          } finally {
            userLock.writeLock().unlock();
          }
        } finally {
          if (hisCon != null) {
            hisCon.closeConnection();
          }
        }
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    return true;
  }


  public Collection<Role> getRoles() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      roleLock.readLock().lock();
      try {
        return defCon.loadCollection(Role.class);
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }


  public Role resolveRole(String roleName) throws PersistenceLayerException {
    return resolveRole(roleName, PREDEFINED_LOCALDOMAIN_NAME);
  }


  public Role resolveRole(String roleName, String domain) throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      return resolveRole(roleName, domain, defCon);
    } finally {
      defCon.closeConnection();
    }
  }
  
  public Role resolveRole(String roleName, String domain, ODSConnection defCon) throws PersistenceLayerException {
    Role roleToRetrieve = new Role(roleName, domain);
    roleLock.readLock().lock();
    try {
      defCon.queryOneRow(roleToRetrieve);
      return roleToRetrieve;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    } finally {
      roleLock.readLock().unlock();
    }
  }


  // pretty specific return value for the XynaServerConnection, could be either generalized or just return the objects and the "connections" transform it
  public String listUsers() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(User.class);

      userLock.readLock().lock();
      try {

        Collection<User> users = defCon.loadCollection(User.class);
        StringBuilder usersOut = new StringBuilder();

        for (User user : users) {
          usersOut.append(user.getName());
          usersOut.append(" - ");

          if (user.isLocked()) {
            usersOut.append("LOCKED - ");
          } else {
            usersOut.append("ACTIVE - ");
          }

          usersOut.append(user.getRole());
          usersOut.append(" - domains: ");
          usersOut.append(user.getDomains());
          usersOut.append("\n");
        }

        usersOut.append("*: Xyna-User - only restricted access allowed\n");
        return usersOut.toString();
      } finally {
        userLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }


  public Collection<User> getUsers() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(User.class);
      userLock.readLock().lock();
      try {

        Collection<User> originalInstances = defCon.loadCollection(User.class);
        Collection<User> safeCopy = new ArrayList<>();
        for (User original : originalInstances) {
          User safe = new User();
          safe.setAllFieldsFromData(original);
          safeCopy.add(safe);
        }
        return safeCopy;
      } finally {
        userLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }


  public Collection<Domain> getDomains() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Domain.class);
      domainLock.readLock().lock();
      try {
        return defCon.loadCollection(Domain.class);
      } finally {
        domainLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }
  
  
  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException {
    language = (language != null) ? language : DocumentationLanguage.EN.toString();
    
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    Collection<RightScope> resultList;
    
    try {
      defCon.ensurePersistenceLayerConnectivity(RightScope.class);
      rightScopeLock.readLock().lock();
      try {
        resultList = defCon.loadCollection(RightScope.class);
      } finally {
        rightScopeLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
    
    for (RightScope rightScope : resultList) {
      getLocalizedDescriptionForRightScope(rightScope, language);
    }
    
    return resultList;
  }
  
  public Map<String, RightScope> getRightScopeMap() throws PersistenceLayerException {
    //TODO Cache!
    Map<String, RightScope> rightScopeMap = new HashMap<>();
    Collection<RightScope> rightScopes = getRightScopes(null);
    for (RightScope rightScope : rightScopes) {
      rightScopeMap.put(rightScope.getName(), rightScope);
    }
    return rightScopeMap;
  }


  public User getUser(String identifier) throws PersistenceLayerException {

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      defCon.ensurePersistenceLayerConnectivity(User.class);

      userLock.readLock().lock();
      try {
        User userToRetrieve = new User(identifier);
        defCon.queryOneRow(userToRetrieve);
        return userToRetrieve;
      } finally {
        userLock.readLock().unlock();
      }

    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    } finally {
      defCon.closeConnection();
    }

  }


  public Role getRole(String name) throws PersistenceLayerException {
    return getRole(name, PREDEFINED_LOCALDOMAIN_NAME);
  }


  public Role getRole(String name, String domain) throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      
      roleLock.readLock().lock();
      try {
        Role roleToRetrieve = new Role(name, domain);
        defCon.queryOneRow(roleToRetrieve);
        return roleToRetrieve;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return null;
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }

  
  private Localization getLocalization(Parameter param) throws PersistenceLayerException {
    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    Localization localizationToRetrieve;
    
    try {
      hisCon.ensurePersistenceLayerConnectivity(Localization.class);
      localizationLock.readLock().lock();
      try {
        localizationToRetrieve = hisCon.queryOneRow(getLocalizationQuery, param);
        if (localizationToRetrieve == null || localizationToRetrieve.getText() == null || localizationToRetrieve.getText().trim().length() == 0) {
          param = new Parameter(param.get(0), param.get(1), DocumentationLanguage.EN.toString());
          localizationToRetrieve = hisCon.queryOneRow(getLocalizationQuery, param);
        }
      } finally {
        localizationLock.readLock().unlock();
      }
    } finally {
      hisCon.closeConnection();
    }

    return localizationToRetrieve;
  }
  
  
  private void getLocalizedDescriptionForRight(Right right, String language) throws PersistenceLayerException {
    Localization localizationToRetrieve = getLocalization(new Parameter(Localization.Type.RIGHT.toString(), right.getPrimaryKey(), language));

    if (localizationToRetrieve != null) {
      right.setDescription(localizationToRetrieve.getText());
    } else {
      right.setDescription("");
    }
  }
  
  
  private void getLocalizedDescriptionForRightScope(RightScope rightScope, String language) throws PersistenceLayerException {
    Localization localizationToRetrieve = getLocalization(new Parameter(Localization.Type.RIGHT_SCOPE.toString(), rightScope.getPrimaryKey(), language));
    
    if (localizationToRetrieve != null) {
      rightScope.setDocumentation(localizationToRetrieve.getText());
    } else {
      rightScope.setDocumentation("");
    }
  }



  public Right getRight(String identifier, String language) throws PersistenceLayerException {
    if (identifier.contains(":")) {
      RightScope sr = getScopedRight(identifier.substring(0, identifier.indexOf(':')), language);
      if (sr == null) {
        return null;
      }
      Right r = new Right(sr.getName());
      r.setDescription(sr.getDocumentation());
      return r;
    }
    
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    Right rightToRetrieve;
    
    try {
      defCon.ensurePersistenceLayerConnectivity(Right.class);
      rightLock.readLock().lock();

      rightToRetrieve = new Right(identifier);
      defCon.queryOneRow(rightToRetrieve);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    } finally {
      rightLock.readLock().unlock();
      defCon.closeConnection();
    }
    
    getLocalizedDescriptionForRight(rightToRetrieve, language);
    return rightToRetrieve;
  }


  private RightScope getScopedRight(String name, String language) throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    RightScope sr;
    try {
      defCon.ensurePersistenceLayerConnectivity(RightScope.class);
      rightScopeLock.readLock().lock();
      try {
        sr = new RightScope(name);
        defCon.queryOneRow(sr);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return null;
      } finally {
        rightScopeLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
    getLocalizedDescriptionForRightScope(sr, language);
    return sr;
  }


  public Domain getDomain(String identifier) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      con.ensurePersistenceLayerConnectivity(Domain.class);
      domainLock.readLock().lock();

      Domain domainToRetrieve = new Domain(identifier);
      con.queryOneRow(domainToRetrieve);
      return domainToRetrieve;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    } finally {
      domainLock.readLock().unlock();
      con.closeConnection();
    }
  }


  public Collection<Right> getRights(String language) throws PersistenceLayerException {
    language = (language != null) ? language : DocumentationLanguage.EN.toString();

    List<Right> resultList = new ArrayList<>();

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defCon.ensurePersistenceLayerConnectivity(Right.class);
      rightLock.readLock().lock();
      try {
        Collection<Right> resultCollection = defCon.loadCollection(Right.class);
        if (resultCollection instanceof List) {
          resultList = (List<Right>) resultCollection;
        } else {
          resultList.addAll(resultCollection);
        }
      } finally {
        rightLock.readLock().unlock();
      }

    } finally {
      defCon.closeConnection();
    }

    Collections.sort(resultList, new Comparator<Right>() {
      public int compare(Right o1, Right o2) {
        // name is always non-null for stored rights
        return o1.getName().compareTo(o2.getName());
      }
    });
    
    for (Right right : resultList) {
      getLocalizedDescriptionForRight(right, language);
    }
    
    return resultList;

  }


  private boolean isRoleAssignedToUser(ODSConnection con, Role role) throws PersistenceLayerException {
    if (!role.getDomain().equals(PREDEFINED_LOCALDOMAIN_NAME)) {
      //ExternalRoles can not be assigned to users
      return false;
    }
    if (!userLock.isWriteLockedByCurrentThread()) {
      userLock.readLock().lock();
    }
    try {
      List<User> users = con.query(getUsersWithSpecifiedRole, new Parameter(role.getName()), 1);
      return users != null && !users.isEmpty();
    } finally {
      if (!userLock.isWriteLockedByCurrentThread()) {
        userLock.readLock().unlock();
      }
    }
  }


  private boolean isDomainAssignedToUserOrRole(ODSConnection con, Domain domain) throws PersistenceLayerException {
    if (!roleLock.isWriteLockedByCurrentThread()) {
      roleLock.readLock().lock();
    }
    try {
      Collection<Role> roles = con.loadCollection(Role.class);
      for (Role role : roles) {
        if (role.getDomain().equals(domain.getName())) {
          return true;
        }
      }
    } finally {
      if (!roleLock.isWriteLockedByCurrentThread()) {
        roleLock.readLock().unlock();
      }
    }
    if (!userLock.isWriteLockedByCurrentThread()) {
      userLock.readLock().lock();
    }
    try {
      Collection<User> users = con.loadCollection(User.class);
      for (User user : users) {
        List<String> domains = user.getDomainList();
        if (domains.contains(domain.getName())) {
          return true;
        }
      }
      return false;
    } finally {
      if (!userLock.isWriteLockedByCurrentThread()) {
        userLock.readLock().unlock();
      }
    }
  }


  // generate the mapping from method names to RIGHTS
  private void fillChannelMapping() {
    Method[] methods = XynaMultiChannelPortal.class.getDeclaredMethods();
    channelFunctionMappingLock.lock();
    try {
      for (Method method : methods) {
        if (method.isAnnotationPresent(AccessControlled.class)) {
          Rights right = method.getAnnotation(AccessControlled.class).associatedRight();
          channelFunctionMapping.put(method.getName(), right);
        }
      }
    } finally {
      channelFunctionMappingLock.unlock();
    }
  }


  public boolean hasRight(String right, String role) throws PersistenceLayerException {
    return hasRight(right, role, PREDEFINED_LOCALDOMAIN_NAME);
  }


  /**
   * facade for hasRight(String, Role), retrieves the Role-object and calls the other hasRight-method
   * @param right
   * @param role
   * @return
   * @throws PersistenceLayerException
   */
  public boolean hasRight(String right, String role, String domain) throws PersistenceLayerException {
    if (right.contains(":")) {
      ScopedRightCache rrs = scopeCache.get(role+domain);
      if (rrs != null) {
        return hasScopedRight(right, rrs);
      }
    }
    Role roleToRetrieve = new Role(role, domain);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);
      roleLock.readLock().lock();

      try {
        defCon.queryOneRow(roleToRetrieve);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }
      return hasRight(right, roleToRetrieve);
    } finally {
      roleLock.readLock().unlock();
      defCon.closeConnection();
    }
  }


  /**
   * checks if a role has a right
   * @param right
   * @param role
   * @return
   * @throws PersistenceLayerException
   */
  public boolean hasRight(String right, Role role) throws PersistenceLayerException {
    if (right.contains(":")) {
      return hasScopedRight(right, getRoleRightScope(role));
    } else {
      return role.hasRight(right);
    }
  }

  
  private boolean hasScopedRight(String scopedRight, ScopedRightCache rrs) {
    return rrs.hasRight(scopedRight);
  }
  

  // retrieve the explicit Rights-obj that is associated with that functionName
  public String resolveFunctionToRight(String functionName) {
    if (logger.isTraceEnabled()) {
      logger.trace("resolveFunctionToRight(" + functionName + ")");
    }
    channelFunctionMappingLock.lock();
    try {
      Rights rightResolvedByFunctionName = channelFunctionMapping.get(functionName);
      if (rightResolvedByFunctionName != null) {
        if (logger.isTraceEnabled()) {
          logger.trace("is contained, returning(" + rightResolvedByFunctionName.toString() + ")");
        }
        return rightResolvedByFunctionName.toString();
      }
    } finally {
      channelFunctionMappingLock.unlock();
    }
    logger.trace("not contained, returning null");
    return null;
  }
  
  /**
   * Baut den Rechte-String zusammen
   * @param right
   * @param action
   * @param parts
   * @return
   */
  @Deprecated
  public String getScopedRight(ScopedRight right, Action action, String... parts) {
    return ScopedRightUtils.getScopedRight(right, action, parts);
  }
  
  @Deprecated
  public String getStartOrderRight(DestinationKey dk) {
    return ScopedRightUtils.getStartOrderRight(dk);
  }


  public String getStartOrderRight(Long cronId) throws XPRC_CronLikeOrderStorageException, XFMG_AccessVerificationException {
    CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
    CronLikeOrderInformation clo = cls.getOrderInformation(cronId);
    
    if (clo != null) {
      List<String> scopeParts = new ArrayList<>();
      scopeParts.add(clo.getTargetOrdertype());
      scopeParts.addAll(ScopedRightUtils.getRuntimeContextParts(clo.getRuntimeContext()));
      
      return ScopedRightUtils.getScopedRight(ScopedRight.START_ORDER.key, scopeParts);
    } else {
      throw new XFMG_AccessVerificationException(ScopedRight.START_ORDER.getDefinition(), "Cron Like Order <" + cronId + "> not found");
    }
  }
  
  
  @Deprecated
  public String getManageTCORight(Action action, BatchProcessInput input) {
    return ScopedRightUtils.getManageTCORight(action, input);
  }

  
  public String getManageTCORight(Action action, Long batchProcessId) throws XFMG_AccessVerificationException {
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    BatchProcessInformation batchProcess;
    try {
      batchProcess = bpm.getBatchProcessInformation(batchProcessId);
    } catch (XynaException e) {
      throw new XFMG_AccessVerificationException(ScopedRight.TIME_CONTROLLED_ORDER.getDefinition(), "Time Controlled Order <" + batchProcessId + "> not found");
    }
    
    if (batchProcess != null) {
      List<String> scopeParts = new ArrayList<>();
      scopeParts.add(action.toString());
      scopeParts.add(batchProcess.getSlaveOrdertype());
      scopeParts.addAll(ScopedRightUtils.getRuntimeContextParts(batchProcess.getArchive().getRuntimeContext()));
      
      return ScopedRightUtils.getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER.key, scopeParts);
    } else {
      throw new XFMG_AccessVerificationException(ScopedRight.TIME_CONTROLLED_ORDER.getDefinition(), "Time Controlled Order <" + batchProcessId + "> not found");
    }
  }

  @Deprecated
  public String getReadTCORight(BatchProcessInformation batchProcessInfo) throws XFMG_AccessVerificationException {
    return ScopedRightUtils.getReadTCORight(batchProcessInfo);
  }


  public List<String> getReadTCORights(Role role) {
    List<String> readTCORights = new ArrayList<>();
    for (String scopedRight : role.getScopedRights()) {
      if (scopedRight.startsWith(ScopedRight.TIME_CONTROLLED_ORDER.key + ":" + Action.read)
           || scopedRight.startsWith(ScopedRight.TIME_CONTROLLED_ORDER.key + ":*")) {
        readTCORights.add(scopedRight);
      }
    }
    
    return readTCORights;
  }

  @Deprecated
  public String getXynaPropertyRight(String propertyKey, Action action) {
    return ScopedRightUtils.getXynaPropertyRight(propertyKey, action);
  }

  @Deprecated
  public String getApplicationRight(String applicationName, String versionName, Action action) {
    return ScopedRightUtils.getApplicationRight(applicationName, versionName, action);
  }
  
  @Deprecated
  public String getApplicationDefinitionRight(String applicationName, String workspacename, Action action) {
    return ScopedRightUtils.getApplicationDefinitionRight(applicationName,workspacename,action);
  }
  
  @Deprecated
  public String getWorkspaceRight(String workspacename, Action action) {
    return ScopedRightUtils.getWorkspaceRight(workspacename,action);
  }

  /**
   * Tries to authenticate the user with the specified id
   */
  public User authenticate(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException {
    return authenticate(id, password, false);
  }


  /**
   * Tries to authenticate the user with the specified id
   */
  public User authenticateHashed(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException {
    return authenticate(id, password, true);
  }
  
  
  public User authenticate(String id, String password, boolean passwordHashed)
                  throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, PersistenceLayerException {
    User userToAuthenticate = new User(id);
    XFMG_UserAuthenticationFailedException failedAuthentication = null;
    User authenticatedUser = null;

    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToAuthenticate);
    dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
    try {

      userLock.writeLock().lock();
      try {

        try {
          defCon.queryOneRowForUpdate(userToAuthenticate);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return null;
        }
        boolean lockedBefore = userToAuthenticate.isLocked();
        int failedLoginsBefore = userToAuthenticate.getFailedLogins();
        boolean mustPersistStorable = false;
        try {
          if (userToAuthenticate.checkPassword(password, passwordHashed)) {
            //ueberpruefung, ob das Passwort noch nicht abgelaufen ist:
            PasswordExpiration expiration = PasswordExpiration.calculate(userToAuthenticate);
            if (expiration.getState().isInvalid()) {
              userToAuthenticate.throwPasswordExpiredException();
            }
            
            authenticatedUser = userToAuthenticate;
            
            //Passwort ggf. mit neuem Algorithmus oder neuem Salt hashen
            String oldPassword = authenticatedUser.getPassword();
            recalculateHash(authenticatedUser, password, passwordHashed);
            if (!oldPassword.equals(authenticatedUser.getPassword())) {
              mustPersistStorable = true;
            }
          }
        } catch (XFMG_UserAuthenticationFailedException e) {
          failedAuthentication = e;
          if (lockedBefore != userToAuthenticate.isLocked()) {
            mustPersistStorable = true;
          }
        }
        if (failedLoginsBefore != userToAuthenticate.getFailedLogins()) {
          mustPersistStorable = true;
        }
        
        // we should only arrive at this point if there was a XFMG_UserAuthenticationFailedException
        if (failedAuthentication != null && logger.isDebugEnabled()) {
          logger.debug("Authentication failed: ", failedAuthentication);
        }
        if (mustPersistStorable) {
          //evtl ist nun locked status anders
          persistStorable(defCon, userToAuthenticate);
          persistStorable(hisCon, userToAuthenticate);
        }
        if (failedAuthentication != null) {
          throw failedAuthentication;
        }
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      if (hisCon != null) {
        dualCloseConnection(defCon, hisCon);
      } else {
        defCon.closeConnection();
      }
    }
    return authenticatedUser;
  }

  private void recalculateHash(User user, String password, boolean passwordHashed) {
    switch (XynaProperty.PASSWORD_PERSISTENCE_RECALCULATE.get()) {
      case never:
        break; //nichts tun
      case on_algorithm_change:
        CreationAlgorithm usedAlgo = CreationAlgorithm.extractAlgorithm(user.getPassword());
        CreationAlgorithm currentAlgo = XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.get();
        if (currentAlgo == null || currentAlgo.equals(usedAlgo)) {
          break; //Algorithmus hat sich nicht geaendert, also nichts zu tun
        }
        //fall through
      case on_every_store:
        user.setPassword(password, passwordHashed); //Passwort neu hashen
        break;
      default : 
        throw new RuntimeException("Unexpected value of " + XynaProperty.PASSWORD_PERSISTENCE_RECALCULATE.getPropertyName() + ": " + XynaProperty.PASSWORD_PERSISTENCE_RECALCULATE.get().name());
    }
  }

  public boolean usersExists(String id) throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(User.class);
      userLock.readLock().lock();
      try {

        User userToCheck = new User(id);
        return defCon.containsObject(userToCheck);
      } finally {
        userLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }


  /**
   * Sets the password for the specified User to 'newPassword' if the user is locked (unlocking him in the process)
   */
  public boolean resetPassword(String id, String newPassword) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    validatePasswordAgainstRestrictions(newPassword);
    User userToRetrieve = new User(id);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToRetrieve);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();

      try {
        defCon.queryOneRowForUpdate(userToRetrieve);
        if (!userToRetrieve.resetPassword(newPassword)) {
          logger.warn("Could not unlock user '" + id + "', user not locked");
          return false;
        }

        persistStorable(defCon, userToRetrieve);
        persistStorable(hisCon, userToRetrieve);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("User '" + id + "' could not be found");
        throw new XFMG_UserDoesNotExistException(id);
      } finally {
        userLock.writeLock().unlock();
      }

    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    //neues Passwort in die Historie eintragen
    createPasswordHistoryEntry(userToRetrieve);
    
    //locked-Markierung im SessionManagement aufheben
    SessionManagement sm = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    sm.lockUser(userToRetrieve);
    
    return true;
  }

  /**
   * Sets the password for the specified User to 'newPassword'
   */
  public boolean setPassword(String id, String newPassword) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    validatePasswordAgainstRestrictions(newPassword);
    return setPassword(id, newPassword, false);
  }


  /**
   * Sets the password for the specified User to the hash 'newPassword'
   * @throws XFMG_UserDoesNotExistException
   */
  public boolean setPasswordHash(String id, String newPassword) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException {
    return setPassword(id, newPassword, true);
  }
  
  
  private boolean setPassword(String id, String newPassword, boolean isPassHashed) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException {
    User userToRetrieve = new User(id);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToRetrieve);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      
      userLock.writeLock().lock();
      try {
        defCon.queryOneRowForUpdate(userToRetrieve);

        userToRetrieve.setNewPassword(newPassword, isPassHashed, ChangeReason.SET_PASSWORD);
        persistStorable(defCon, userToRetrieve);
        persistStorable(hisCon, userToRetrieve);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("User '" + id + "' could not be found");
        throw new XFMG_UserDoesNotExistException(id);
      } finally {
        userLock.writeLock().unlock();
      }

    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    //neues Passwort in die Historie eintragen
    createPasswordHistoryEntry(userToRetrieve);
    
    return true;
  }


  public boolean createDomain(String domainname, DomainType type, int maxRetries, int timeout)
      throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    if (type.equals(DomainType.LOCAL) && !domainname.equals(PREDEFINED_LOCALDOMAIN_NAME)) {
      logger.warn("Domain '" + domainname + "' could not be created, only one local domain allowed");
      return false; //FIXME: this should throw an exception
    }
    
    validateDomainName(domainname);

    Domain newDomain = new Domain(domainname, type, maxRetries, timeout);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(newDomain);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);

      domainLock.writeLock().lock();
      try {
        if (defCon.containsObject(newDomain)) {
          logger.warn("Domain '" + domainname + "' could not be created becaus it does already exist");
          return false;
        }

        persistStorable(defCon, newDomain);
        persistStorable(hisCon, newDomain);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean deleteDomain(String domainname) throws PersistenceLayerException, XFMG_DomainIsAssignedException,
      XFMG_PredefinedXynaObjectException {
    if (isPredefined(PredefinedCategories.RIGHT, domainname)) {
      logger.warn("Domain '" + domainname + "' could not be deleted because it is a Xyna-Domain");
      throw new XFMG_PredefinedXynaObjectException(domainname, "domain");
    }

    Domain domainToBeDeleted = new Domain(domainname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(domainToBeDeleted);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);

      domainLock.writeLock().lock();
      try {
        if (!defCon.containsObject(domainToBeDeleted)) {
          logger.warn("Domain '" + domainname + "' could not be deleted because it did not exists");
          return false;
        }

        if (isDomainAssignedToUserOrRole(defCon, domainToBeDeleted)) {
          logger.warn("Domain '" + domainname + "' could not be deleted because it is still assigned to a role or a user");
          throw new XFMG_DomainIsAssignedException(domainname);
        }

        deleteStorable(defCon, domainToBeDeleted);
        deleteStorable(hisCon, domainToBeDeleted);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Domain '" + domainname + "' was successfully deleted");
    }

    return true;
  }


  public boolean modifyDomainFieldMaxRetries(String domainname, int newValue) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException {
    Domain domainToChange = new Domain(domainname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(domainToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);
      domainLock.writeLock().lock();
      try {
        defCon.queryOneRow(domainToChange);
        domainToChange.setMaxRetries(newValue);
        persistStorable(defCon, domainToChange);
        persistStorable(hisCon, domainToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Domain '" + domainname + "' could not be changed because it not already exist");
        throw new XFMG_DomainDoesNotExistException(domainname);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean modifyDomainFieldConnectionTimeout(String domainname, int newValue) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException {
    Domain domainToChange = new Domain(domainname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(domainToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);
      domainLock.writeLock().lock();
      try {
        defCon.queryOneRow(domainToChange);
        domainToChange.setConnectionTimeout(newValue);
        persistStorable(defCon, domainToChange);
        persistStorable(hisCon, domainToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Domain '" + domainname + "' could not be changed because it does not exist");
        throw new XFMG_DomainDoesNotExistException(domainname);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean modifyDomainFieldDomainTypeSpecificData(String domainname, DomainTypeSpecificData newValue)
      throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    Domain domainToChange = new Domain(domainname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(domainToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);
      domainLock.writeLock().lock();
      try {
        defCon.queryOneRow(domainToChange);
        domainToChange.setDomainSpecificData(newValue);
        persistStorable(defCon, domainToChange);
        persistStorable(hisCon, domainToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Domain '" + domainname + "' could not be changed because it does not exist");
        throw new XFMG_DomainDoesNotExistException(domainname, e);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean modifyDomainFieldDescription(String domainname, String newValue) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException {
    Domain domainToChange = new Domain(domainname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(domainToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);
      domainLock.writeLock().lock();
      try {
        defCon.queryOneRow(domainToChange);
        domainToChange.setDescription(newValue);
        persistStorable(defCon, domainToChange);
        persistStorable(hisCon, domainToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Domain '" + domainname + "' could not be changed because it does not exist");
        throw new XFMG_DomainDoesNotExistException(domainname, e);
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public String listDomains() throws PersistenceLayerException {
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Domain.class);
      domainLock.readLock().lock();
      try {
        Collection<Domain> domains = defCon.loadCollection(Domain.class);
        StringBuilder domainsOut = new StringBuilder();
        for (Domain domain : domains) {
          domainsOut.append(domain.getName());
          if (isPredefined(PredefinedCategories.DOMAIN, domain.getName())) {
            domainsOut.append("* - ");
          } else {
            domainsOut.append(" - ");
          }
          domainsOut.append(domain.getDomainTypeAsEnum().toString());
          domainsOut.append(" - ");

          domainsOut.append("retries: ");
          domainsOut.append(domain.getMaxRetries());
          domainsOut.append(" - ");

          domainsOut.append("timeout: ");
          domainsOut.append(domain.getConnectionTimeout());

          if (domain.getDescription() != null && !domain.getDescription().equals("")) {
            domainsOut.append(" - ");

            domainsOut.append(domain.getDescription());
          }
          domainsOut.append("\n");


          if (domain.getDomainSpecificData() != null) {
            domain.getDomainSpecificData().appendInformation(domainsOut);
          }
        }

        domainsOut.append("*: Xyna-Domain - only restricted access allowed\n");
        return domainsOut.toString();
      } finally {
        domainLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }
  }


  public boolean modifyUserFieldDomains(String username, List<String> newValue) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    if (newValue == null || newValue.isEmpty()) {
      throw new IllegalArgumentException(""); //FIXME: throw specific non runtime exception
    }
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Domain.class);

      domainLock.readLock().lock();
      try {
        for (String domainName : newValue) {
          if (!defCon.containsObject(new Domain(domainName))) {
            logger.warn("User '" + username + "' could not be changed because the specified domain '" + domainName + "' does not exist");
            throw new XFMG_DomainDoesNotExistException(domainName);
          }
        }
      } finally {
        domainLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    User userToChange = new User(username);
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        defCon.queryOneRowForUpdate(userToChange);
        userToChange.setDomains(newValue);
        persistStorable(defCon, userToChange);
        persistStorable(hisCon, userToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("User '" + username + "' could not be changed because it does not exist");
        throw new XFMG_UserDoesNotExistException(username, e);
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean modifyUserFieldLocked(String username, boolean newValue) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException {

    User userToChange = new User(username);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(userToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        defCon.queryOneRowForUpdate(userToChange);
        userToChange.setLocked(newValue);
        persistStorable(defCon, userToChange);
        persistStorable(hisCon, userToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("User '" + username + "' could not be changed because it does not exist");
        throw new XFMG_UserDoesNotExistException(username, e);
      } finally {
        userLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    SessionManagement sm = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    //user als locked markieren bzw. Markierung aufheben
    sm.lockUser(userToChange);
    if (newValue) {
      //Sessions beenden
      sm.quitSessionsForUser(username);
    }
    return true;
  }


  public boolean modifyRightFieldDescription(String rightname, String newValue, String language) throws XynaException {
    if (rightname.contains(":")) {
      modifyRightScopeDocumentation(rightname.substring(0, rightname.indexOf(':')), newValue, language);
      return true;
    }
    language = (language != null) ? language : DocumentationLanguage.EN.toString();
    
    Right rightToChange = new Right(rightname);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Right.class);
      rightLock.readLock().lock();
      try {
        //checken, dass right existiert
        defCon.queryOneRow(rightToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Right '" + rightname + "' could not be changed because it does not exist");
        throw new XFMG_RightDoesNotExistException(rightname, e);
      } finally {
        rightLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    storeLocalizedText(Localization.Type.RIGHT, rightname, language, newValue);
    return true;
  }
  
  public void modifyRightScopeDocumentation(String name, String newValue, String language) throws PersistenceLayerException, XFMG_RightDoesNotExistException {
    language = (language != null) ? language : DocumentationLanguage.EN.toString();

    RightScope rs = new RightScope(name);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(RightScope.class);
      rightScopeLock.readLock().lock();
      try {
        //checken, dass right existiert
        defCon.queryOneRow(rs);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XFMG_RightDoesNotExistException(name, e);
      } finally {
        rightScopeLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    storeLocalizedText(Localization.Type.RIGHT_SCOPE, name, language, newValue);
  }


  private void storeLocalizedText(Localization.Type type, String pk, String language, String text) throws PersistenceLayerException {

    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    Localization localizationToChange;
    
    try {
      hisCon.ensurePersistenceLayerConnectivity(Localization.class);
      localizationLock.writeLock().lock();
      try {
        localizationToChange =
            hisCon.queryOneRow(getLocalizationQuery, new Parameter(type.toString(), pk, language));

        if (localizationToChange == null) {
          long id;
          try {
            id = IDGenerator.getInstance().getUniqueId(Localization.TABLENAME);
          } catch (XynaException e) {
            throw new RuntimeException(e);
          }
          localizationToChange = new Localization(id, type.toString(), pk, language, text);
        } else {
          localizationToChange.setText(text);
        }

        persistStorable(hisCon, localizationToChange);
      } finally {
        localizationLock.writeLock().unlock();
      }
    } finally {
      hisCon.closeConnection();
    }
  }


  public boolean modifyRoleFieldDescription(String rolename, String newValue) throws PersistenceLayerException,
      XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return modifyRoleFieldDescription(rolename, PREDEFINED_LOCALDOMAIN_NAME, newValue);
  }


  public boolean modifyRoleFieldDescription(String rolename, String domainName, String newValue)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    Role roleToChange = new Role(rolename, domainName);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(roleToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        defCon.queryOneRow(roleToChange);
        roleToChange.setDescription(newValue);
        persistStorable(defCon, roleToChange);
        persistStorable(hisCon, roleToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Role '" + rolename + "' could not be changed because it does not exist");
        throw new XFMG_RoleDoesNotExistException(rolename, e);
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }


  public boolean modifyRoleFieldAlias(String rolename, String domainName, String newValue)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    if (isPredefined(PredefinedCategories.ROLE, new StringBuilder().append(rolename).append(domainName).toString())) {
      logger.warn("Role '" + rolename + "' could not be modified because it is a Xyna-Role");
      throw new XFMG_PredefinedXynaObjectException(rolename, "role");
    }

    Role internalRole = new Role(newValue, PREDEFINED_LOCALDOMAIN_NAME); // External roles are always mapped to local roles 
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Role.class);

      roleLock.readLock().lock();
      try {
        defCon.queryOneRow(internalRole);
        if (!internalRole.getDomain().equals(PREDEFINED_LOCALDOMAIN_NAME)) {
          // TODO: XFMG_IllegalMappingToExternalRoleDetected
          throw new XNWH_GeneralPersistenceLayerException("Illegal mapping from external role to external role detected");
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XFMG_RoleDoesNotExistException("Mapping to role '" + newValue + "' could not be set because it does not exist", e);
      } finally {
        roleLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    Role roleToChange = new Role(rolename, domainName);
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(roleToChange);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        defCon.queryOneRow(roleToChange);
        roleToChange.setAlias(newValue);
        persistStorable(defCon, roleToChange);
        persistStorable(hisCon, roleToChange);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Role '" + rolename + "' could not be changed because it does not exist");
        throw new XFMG_RoleDoesNotExistException(rolename, e);
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    return true;
  }


  // ensures the existence of all Xyna-Rights, -Roles and -Users
  private void ensurePredefinedExistence() throws PersistenceLayerException {
    Right riCon = new Right();
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(riCon);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Right.class);
      rightLock.writeLock().lock();
      try {
        for (Rights right : Rights.values()) {
          Right predefinedRight = new Right(right.toString());
          if (!defCon.containsObject(predefinedRight)) {
            persistStorable(defCon, predefinedRight);
            persistStorable(hisCon, predefinedRight);
          }
        }

        //Sichtbarkeitsrechte für GUI
        for (GuiRight guiRight : GuiRight.values()) {
          Right predefinedRight = new Right(guiRight.getKey());
          if (!defCon.containsObject(predefinedRight)) {
            persistStorable(defCon, predefinedRight);
            persistStorable(hisCon, predefinedRight);
          }
        }
      } finally {
        rightLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    Domain dCon = new Domain();
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    hisCon = requiredHistoryConnection(dCon);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Domain.class);
      domainLock.writeLock().lock();
      try {
        Domain predefinedDomain = new Domain(PREDEFINED_LOCALDOMAIN_NAME, DomainType.LOCAL, 0, -1);

        if (!defCon.containsObject(predefinedDomain)) {
          predefinedDomain.setDescription(DESCRIPTION_LOCAL_DOMAIN);
          persistStorable(defCon, predefinedDomain);
          persistStorable(hisCon, predefinedDomain);
        }
      } finally {
        domainLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    RightScope rsCon = new RightScope();
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    hisCon = requiredHistoryConnection(rsCon);
    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, RightScope.class);
      rightScopeLock.writeLock().lock();
      try {
        RightScopeBuilder rsb = new RightScopeBuilder();
        RightScope rightScope = rsb.buildRightScope(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_DEFINITION);
  
        if (!defCon.containsObject(rightScope)) {
          persistStorable(defCon, rightScope);
          persistStorable(hisCon, rightScope);
        }
  
        //Rechtebereiche
        for (ScopedRight right : ScopedRight.values()) {
          rsb = new RightScopeBuilder();
          rightScope = rsb.buildRightScope(right.getDefinition());
          
          if (!defCon.containsObject(rightScope)) {
            persistStorable(defCon, rightScope);
            persistStorable(hisCon, rightScope);
          }
        }
      } finally {
        rightScopeLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    
    Role roCon = new Role();
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    hisCon = requiredHistoryConnection(roCon);
    
    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();

      try {
      Role predefinedRole = new Role(ADMIN_ROLE_NAME, PREDEFINED_LOCALDOMAIN_NAME);
      try {
        defCon.queryOneRow(predefinedRole);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        logger.debug("ADMIN role does not exist, creating...");
      }

      predefinedRole.setDescription(DESCRIPTION_ADMIN_ROLE);
      //einfache Rechte
      for (Rights right : Rights.values()) {
        if (!right.mandatoryForAdminRole) {
          continue;
        }

        predefinedRole.grantRight(right.toString());
      }
      //Sichtbarkeitsrechte für GUI
      for (GuiRight guiRight : GuiRight.values()) {
        predefinedRole.grantRight(guiRight.getKey());
      }
      //komplexe Rechte
      predefinedRole.grantScopedRight(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_ALL_ACCESS);
      for (ScopedRight scopedRight : ScopedRight.values()) {
        switch (scopedRight) {
          case START_ORDER :
            //StartOrder rausnehmen, da schon allgemeines START_ORDER Recht vergeben
            break;
          case FILE_ACCESS :
            predefinedRole.grantScopedRight(ScopedRight.FILE_ACCESS.getKey() + 
                                            ScopedRightUtils.SCOPE_SEPERATOR +
                                            "*" +
                                            ScopedRightUtils.SCOPE_SEPERATOR +
                                            FileUtils.getSystemTempDir() +
                                            "*");
            break;
          default :
            predefinedRole.grantScopedRight(scopedRight.allAccess());
            break;
        }
      }
      persistStorable(defCon, predefinedRole);
      persistStorable(hisCon, predefinedRole);


      predefinedRole = new Role(MODELLER_ROLE_NAME, PREDEFINED_LOCALDOMAIN_NAME);
      try {
        defCon.queryOneRow(predefinedRole);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        logger.debug("MODELLER role does not exist, creating...");
      }
      
        predefinedRole.setDescription(DESCRIPTION_MODELLER_ROLE);
        predefinedRole.grantRight(Rights.DEPLOYMENT_MDM.toString());
        predefinedRole.grantRight(Rights.ORDERARCHIVE_VIEW.toString());
        predefinedRole.grantRight(Rights.ORDERARCHIVE_DETAILS.toString());
        predefinedRole.grantRight(Rights.PROCESS_MANUAL_INTERACTION.toString());
        predefinedRole.grantRight(Rights.VIEW_MANUAL_INTERACTION.toString());
        predefinedRole.grantRight(Rights.EDIT_MDM.toString());
        predefinedRole.grantRight(Rights.READ_MDM.toString());
        predefinedRole.grantRight(Rights.START_ORDER.toString());
        predefinedRole.grantRight(Rights.SESSION_CREATION.toString());
        predefinedRole.grantRight(Rights.USER_LOGIN.toString());
        predefinedRole.grantRight(Rights.USER_MANAGEMENT.toString());
        predefinedRole.grantRight(Rights.USER_MANAGEMENT_EDIT_OWN.toString());
        predefinedRole.grantRight(Rights.FREQUENCY_CONTROL_MANAGEMENT.toString());
        predefinedRole.grantRight(Rights.FREQUENCY_CONTROL_VIEW.toString());
        predefinedRole.grantRight(Rights.WORKINGSET_MANAGEMENT.toString());
        predefinedRole.grantRight(Rights.APPLICATION_MANAGEMENT.toString());
        predefinedRole.grantRight(Rights.APPLICATION_ADMINISTRATION.toString());
        predefinedRole.grantRight(GuiRight.FACTORY_MANAGER.getKey());
        predefinedRole.grantRight(GuiRight.PROCESS_MODELLER.getKey());
        predefinedRole.grantRight(GuiRight.PROCESS_MONITOR.getKey());
        predefinedRole.grantRight(GuiRight.TEST_FACTORY.getKey());
        predefinedRole.grantRight(GuiRight.ACCESS_CONTROL.getKey());
        predefinedRole.grantScopedRight(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_ALL_ACCESS);
        predefinedRole.grantScopedRight(ScopedRight.XYNA_PROPERTY.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.APPLICATION.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.APPLICATION_DEFINITION.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.DATA_MODEL.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.DEPLOYMENT_MARKER.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.DEPLOYMENT_ITEM.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.ORDER_INPUT_SOURCE.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.CRON_LIKE_ORDER.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.TIME_CONTROLLED_ORDER.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.ORDER_TYPE.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.CAPACITY.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.VETO.allAccess());
        predefinedRole.grantScopedRight(ScopedRight.WORKSPACE.allAccess());
        persistStorable(defCon, predefinedRole);
        persistStorable(hisCon, predefinedRole);
   
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

  }


  /**
   * creates a right
   * @param rightName
   * @return
   */
  public boolean createRight(String rightName) throws PersistenceLayerException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    if (rightName.contains(":")) {
      return createRightScope(rightName, null, null);
    }
    
    validateRightName(rightName);
    
    Right rCon = new Right();
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(rCon);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Right.class);
      rightLock.writeLock().lock();
      try {
        if (!isPredefined(PredefinedCategories.RIGHT, rightName)) {
          Matcher matcher = RIGHT_PATTERN_PATTERN.matcher(rightName);
          if (!matcher.matches()) {
            logger.debug("Right '" + rightName
                + "' could not be created because it does not follow the naming convention (" + RIGHT_PATTERN_DESCRIPTION + ")");
            throw new XFMG_NamingConventionException(rightName, RIGHT_PATTERN_DESCRIPTION);
          }
        }

        Right newRight = new Right(rightName);

        if (defCon.containsObject(newRight)) {
          logger.debug("Right '" + rightName + "' could not be created because it does already exist");
          return false;
        }

        persistStorable(defCon, newRight);
        persistStorable(hisCon, newRight);
      } finally {
        rightLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    return true;
  }

  public boolean createRightScope(String scopeDefinition, String documentation, String language) throws PersistenceLayerException {
    RightScopeBuilder scopeBuilder = new RightScopeBuilder();
    RightScope scope = scopeBuilder.buildRightScope(scopeDefinition);
    if (scope == null) {
      return false;
    } else {
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
      ODSConnection hisCon = requiredHistoryConnection(scope);
      try {
        dualEnsurePersistenceLayerConnectivity(defCon, hisCon, RightScope.class);
        rightScopeLock.writeLock().lock();
        try {
          if (defCon.containsObject(scope)) {
            logger.debug("RightScope '" + scope.getDefinition() + "' could not be created because it does already exist");
            return false;
          }
          persistStorable(defCon, scope);
          persistStorable(hisCon, scope);
        } finally {
          rightScopeLock.writeLock().unlock();
        }
      } finally {
        dualCloseConnection(defCon, hisCon);
      }
    }
    
    if (documentation != null) {
      if (language == null) {
        language = DocumentationLanguage.EN.toString();
      }
      storeLocalizedText(Type.RIGHT_SCOPE, scope.getName(), language, documentation);
    }
    return true;
  }


  /**
   * deletes a right if it is not a Xyna-Right
   * @param rightName
   * @return
   * @throws XFMG_PredefinedXynaObjectException
   * @throws XFMG_RightDoesNotExistException
   */
  public boolean deleteRight(String rightName) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
      XFMG_RightDoesNotExistException {
    if (rightName.contains(":")) {
      return deleteRightScope(rightName);
    }
    if (isPredefined(PredefinedCategories.RIGHT, rightName)) {
      logger.warn("Right '" + rightName + "' could not be deleted because it is a Xyna-Right");
      throw new XFMG_PredefinedXynaObjectException(rightName, "right");
    }
    Right rightToRemove = new Right(rightName);
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(rightToRemove);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Right.class);
      rightLock.writeLock().lock();
      try {
        if (!defCon.containsObject(rightToRemove)) {
          throw new XFMG_RightDoesNotExistException(rightName);
        }

        deleteStorable(defCon, rightToRemove);
        deleteStorable(hisCon, rightToRemove);
      } finally {
        rightLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }

    Role rCon = new Role();
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    hisCon = requiredHistoryConnection(rCon);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try {
        Collection<Role> roles = defCon.loadCollection(Role.class);
        boolean atLeastOneRoleModified = false;
        for (Role role : roles) {
          if (role.hasRight(rightName)) {
            role.revokeRight(rightName);
            atLeastOneRoleModified = true;
          }
        }
  
        if (atLeastOneRoleModified) {
          persistCollection(defCon, roles);
          persistCollection(hisCon, roles);
        }
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    return true;
  }
  
  
  public boolean deleteRightScope(String scopeDefinitionOrKey) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    int index = scopeDefinitionOrKey.indexOf(':');
    String scopeKey = scopeDefinitionOrKey;
    if (index > 0) {
      scopeKey = scopeDefinitionOrKey.substring(0, index);
    }
    
    if (isPredefined(PredefinedCategories.RIGHTSCOPE, scopeKey)) {
      logger.warn("RightScope '" + scopeKey + "' could not be deleted because it is a Xyna-RightScope");
      throw new XFMG_PredefinedXynaObjectException(scopeKey, "rightscope");
    }
    
    RightScope rightScopeToRemove = new RightScope(scopeKey);
    
    boolean removed = false;
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(rightScopeToRemove);
    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, RightScope.class);
      rightScopeLock.writeLock().lock();
      try {
        if (defCon.containsObject(rightScopeToRemove)) {
          removed = true;
          deleteStorable(defCon, rightScopeToRemove);
          deleteStorable(hisCon, rightScopeToRemove);
        }
      } finally {
        rightScopeLock.writeLock().unlock();  
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    
    Role rCon = new Role();
    defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    hisCon = requiredHistoryConnection(rCon);

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, Role.class);
      roleLock.writeLock().lock();
      try { 
        String scopeKeyWithTrailingSeperator = scopeKey + ":";

        List<Role> modifiedRoles = new ArrayList<Role>();
        Collection<Role> roles = defCon.loadCollection(Role.class);
        for (Role role : roles) {
          boolean modified = false;
          List<String> scopedRightsToRevoke = new ArrayList<String>();
          for (String right : role.getScopedRights()) {
            if (right.startsWith(scopeKeyWithTrailingSeperator)) {
              scopedRightsToRevoke.add(right);
            }
          }
          for (String right : scopedRightsToRevoke) {
            role.revokeScopedRight(right);
            modified = true;
          }
          if (modified) {
            modifiedRoles.add(role);
          }
        }

        if (modifiedRoles.size() > 0) {
          try {
            persistCollection(defCon, modifiedRoles);
            persistCollection(hisCon, modifiedRoles);
          } finally {
            for (Role modifiedRole : modifiedRoles) {
              scopeCache.remove(modifiedRole.getId());
            }
          }
        }
      } finally {
        roleLock.writeLock().unlock();
      }
    } finally {
      dualCloseConnection(defCon, hisCon);
    }
    
    
    
    return removed;
  }


  public UserSearchResult searchUsers(UserSelect select, int maxRows) throws PersistenceLayerException {
    try {
      return searchUsersInternally(select, maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      cache.clear();
      return searchUsersInternally(select, maxRows);
    }
  }


  private UserSearchResult searchUsersInternally(UserSelect select, int maxRows) throws PersistenceLayerException {
    String selectString;
    ResultSetReader<User> reader;
    String selectCountString;
    Parameter paras;
    try {
      selectCountString = select.getSelectCountString();
      selectString = select.getSelectString() + " order by " + User.COL_CREATIONDATE + " desc";
      reader = select.getReader();
      paras = select.getParameter();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("problem with select statement", e);
    }
    int countAll = 0;
    List<User> users = new ArrayList<User>();

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<User> query = cache.getQueryFromCache(selectString, con, reader);
      users.addAll(con.query(query, paras, maxRows));
      if (users.size() >= maxRows) {
        PreparedQuery<? extends UserCount> queryCount =
            cache.getQueryFromCache(selectCountString, con, UserCount.getCountReader());
        UserCount count = con.queryOneRow(queryCount, paras);
        countAll = count.getCount();
      } else {
        countAll = users.size();
      }
    } finally {
      con.closeConnection();
    }

    return new UserSearchResult(users, countAll, users.size());
  }


  public enum PredefinedCategories {
    RIGHT, RIGHTSCOPE, ROLE, DOMAIN
  };


  // check if an Right/Role/User is a Xyna-Right/Role/User
  public boolean isPredefined(PredefinedCategories category, String id) {
    switch (category) {
      case RIGHT :
        Rights right = null;
        try {
          right = Rights.valueOf(id); //<--throws IllegalArgumentException if no value for id
        } catch (IllegalArgumentException e) {
          return false;
        }
        return (right != null && right.isMandatoryForAdminRole());

      case RIGHTSCOPE :
        String scopeKey = id;
        int index = scopeKey.indexOf(':');
        if (index > 0) {
          scopeKey = scopeKey.substring(0, index);
        }
        
        if (XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_DEFINITION.startsWith(scopeKey)) {
          return true;
        } else {
          for (ScopedRight scopedRight : ScopedRight.values()) {
            if (scopedRight.key.equals(scopeKey)) {
              return true;
            }
          }
          return false;
        }
        
      case ROLE :
        if (id.equals(ADMIN_ROLE_ID) || id.equals(MODELLER_ROLE_ID)) {
          return true;
        } else {
          return false;
        }

      case DOMAIN :
        if (id.equals(PREDEFINED_LOCALDOMAIN_NAME)) {
          return true;
        } else {
          return false;
        }
    }
    return false;
  }


  private final void persistStorable(ODSConnection con, Storable obj) throws PersistenceLayerException {
    if (con != null) {
      con.persistObject(obj);
      con.commit();
    }
  }


  /**
   * Legt fuer das aktuelle Passwort des Users einen PasswordHistory-Eintrag an.
   * @param user
   * @throws PersistenceLayerException
   */
  private void createPasswordHistoryEntry(User user) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.ensurePersistenceLayerConnectivity(PasswordHistoryStorable.class);
      
      passwordHistoryLock.writeLock().lock();
      try {
        if (XynaProperty.PASSWORD_HISTORY_SIZE.get() != 0 || XynaProperty.PASSWORD_STORED_HISTORY_SIZE.get() != 0) {
          //neuen Eintrag anlegen
          PasswordHistoryStorable lastEntry = con.queryOneRow(getPasswordHistoryForUserQuery, new Parameter(user.getName()));
          long index = 1;
          if (lastEntry != null) {
            index += lastEntry.getPasswordIndex();
          }
          PasswordHistoryStorable entry = new PasswordHistoryStorable(user.getName(), user.getPassword(), user.getPasswordChangeDate(), index);
          
          persistStorable(con, entry);
        }
        
        //Groesse anpassen
        resizePasswordHistory(con, user.getName());
      } finally {
        passwordHistoryLock.writeLock().unlock();
      }
    } finally {
      con.closeConnection();
    }
  }

  /**
   * Loescht die aeltesten Eintraege aus der PasswordHistory falls deren Groesse beschraenkt ist.
   * @param con
   * @param userName
   * @throws PersistenceLayerException
   */
  private void resizePasswordHistory(ODSConnection con, String userName) throws PersistenceLayerException {
    if (XynaProperty.PASSWORD_HISTORY_SIZE.get() < 0 || XynaProperty.PASSWORD_STORED_HISTORY_SIZE.get() < 0) {
      return; //unendliche Historie
    }
    
    int desiredHistorySize = Math.max(XynaProperty.PASSWORD_HISTORY_SIZE.get(), XynaProperty.PASSWORD_STORED_HISTORY_SIZE.get()); //nicht zu viele Passwoerter wegwerfen
    
    List<PasswordHistoryStorable> history = con.query(getPasswordHistoryForUserQuery, new Parameter(userName), -1);
    if (history.size() > desiredHistorySize) {
      //die aeltesten Eintraege loeschen
      List<PasswordHistoryStorable> toDelete = history.subList(desiredHistorySize, history.size()); 
      con.delete(toDelete);
      con.commit();
    }
  }

  private final <T extends Storable> void persistCollection(ODSConnection con, Collection<T> storableCollection)
      throws PersistenceLayerException {
    if (con != null) {
      con.persistCollection(storableCollection);
      con.commit();
    }
  }


  private final ODSConnection requiredHistoryConnection(Storable obj) {
    boolean areHistoryAndDefaultTheSame;

    try {
      areHistoryAndDefaultTheSame =
          ods.isSamePhysicalTable(obj.getTableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      List<XynaException> list = new ArrayList<XynaException>();
      list.add(e);
      throw new XynaRuntimeException("", null, list);
    }

    if (!areHistoryAndDefaultTheSame) {
      return ods.openConnection(ODSConnectionType.HISTORY);
    } else {
      return null;
    }
  }


  public final <T extends Storable> void dualEnsurePersistenceLayerConnectivity(ODSConnection con1, ODSConnection con2,
                                                                                Class<T> storableClazz)
      throws PersistenceLayerException {
    if (con1 != null) {
      con1.ensurePersistenceLayerConnectivity(storableClazz);
    }

    if (con2 != null) {
      con2.ensurePersistenceLayerConnectivity(storableClazz);
    }
  }


  public final void dualCloseConnection(ODSConnection con1, ODSConnection con2) throws PersistenceLayerException {
    if (con1 != null) {
      con1.closeConnection();
    }

    if (con2 != null) {
      con2.closeConnection();
    }
  }


  private final void deleteStorable(ODSConnection con, Storable obj) throws PersistenceLayerException {
    if (con != null) {
      con.deleteOneRow(obj);
      con.commit();
    }
  }


  public enum USERMANAGEMENTOBJECT_FIELDIDENTIFIER {
    domain_maxretries, domain_connectiontimeout, domain_specificdata, user_lockedstate, user_domains;
  }


  public enum USERMANAGEMENTOBJECT_REGISTEREDTABLENAMES {
    userarchive, rolearchive, rightarchive, domainarchive;
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> watched = new ArrayList<String>();
    watched.add(XynaProperty.GLOBAL_DOMAIN_OVERWRITE);
    watched.add(XynaProperty.DEFAULT_DOMAINS_FOR_NEW_USERS);
    watched.add(XynaProperty.PASSWORD_RESTRICTIONS);
    watched.add(XynaProperty.SCOPED_RIGHTS_CACHE_SIZE_PROPERTY_NAME);
    watched.add(XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.getPropertyName());
    return watched;
  }


  public void propertyChanged() {
    logger.debug("propertyChanged is called");
    getAndSetIfChangedGlobalDomainOverwrite();
    getAndSetIfChangedDefaultDomainsForNewUsers();
    getAndSetIfChangedPasswordRestrictions();
    refreshScopedRightCache();
    encryptPersistedPasswords();
  }


  private void getAndSetIfChangedGlobalDomainOverwrite() {
    String property =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.GLOBAL_DOMAIN_OVERWRITE);
    if (property == null || property.equals("")) {
      if (globalOverwrite) {
        globalOverwrite = false;
      }
    } else {
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

      try {
        defCon.ensurePersistenceLayerConnectivity(Domain.class);
        domainLock.readLock().lock();
        try {
          String[] domains = property.split(",");
          globalDomain.clear();
          for (String domain : domains) {
            Domain domainToCheck = new Domain(domain);
            try {
              if (defCon.containsObject(domainToCheck)) {
                globalOverwrite = true;
                globalDomain.add(domain);
              } else {
                XFMG_DomainDoesNotExistException e = new XFMG_DomainDoesNotExistException(property);
                logger.error("Global domain was set to an invalid value, overwrite deactivated", e);
                globalOverwrite = false;
              }
            } catch (PersistenceLayerException e) {
              logger.error("Error while checking value for global domain, overwrite deactivated");
              globalOverwrite = false;
            }
          }
        } finally {
          domainLock.readLock().unlock();
        }
      } catch (PersistenceLayerException ple) {
        logger.error("Error while checking value for global domain, overwrite deactivated");
        globalOverwrite = false;
      } finally {
        try {
          defCon.closeConnection();
        } catch (PersistenceLayerException ple) {
          logger.warn("Could not close connection!", ple);
        }
      }
    }
  }


  private void getAndSetIfChangedDefaultDomainsForNewUsers() {
    userLock.writeLock().lock(); //getting the writeLock to prevent further users from being written while we parse the property
    try {
      String property =
          XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.DEFAULT_DOMAINS_FOR_NEW_USERS);
      if (property == null || property.equals("")) {
        logger.info("XynaProperty " + XynaProperty.DEFAULT_DOMAINS_FOR_NEW_USERS
            + " is set to invalid value, falling back to default domain : "
            + UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
        defaultDomains.clear();
        defaultDomains.add(UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
      } else {
        String[] possibleDomains = property.split(",");
        ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

        try {
          defCon.ensurePersistenceLayerConnectivity(Domain.class);
          domainLock.readLock().lock();
          try {
            for (String domain : possibleDomains) {
              Domain domainToCheck = new Domain(domain);
              if (!defCon.containsObject(domainToCheck)) {
                XFMG_DomainDoesNotExistException e = new XFMG_DomainDoesNotExistException(property);
                logger.error("Default domains contained an invalid value, falling back to default domain : "
                    + UserManagement.PREDEFINED_LOCALDOMAIN_NAME, e);
                defaultDomains.clear();
                defaultDomains.add(UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
                return;
              }
            }
            defaultDomains.clear();
            for (String domain : possibleDomains) {
              defaultDomains.add(domain);
            }
          } finally {
            domainLock.readLock().unlock();
          }
        } catch (PersistenceLayerException e) {
          logger.error("Global domain was set to an invalid value, falling back to default domain : "
              + UserManagement.PREDEFINED_LOCALDOMAIN_NAME, e);
          return;
        } finally {
          try {
            defCon.closeConnection();
          } catch (PersistenceLayerException ple) {
            logger.warn("Could not close connection!", ple);
          }
        }
      }
    } finally {
      userLock.writeLock().unlock();
    }
  }
  
  
  private void getAndSetIfChangedPasswordRestrictions() {
    String property = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.PASSWORD_RESTRICTIONS);
    if (property == null || property.equals("")) {
      property = DEFAULT_PASSWORD_RESTRICTION;
    }
    if (passwordRestrictions == null || !passwordRestrictions.equals(property)) {
      passwordRestrictions = property;
      //parse and set Patterns
      String[] passwordRestriction = passwordRestrictions.split("(?<!\\\\);");
      List<Pattern> restrictionsPatterns = new ArrayList<Pattern>();
      for (int i = 0; i < passwordRestriction.length; i++) {
        String unescapedRestriction = passwordRestriction[i].replace("\\;", ";");
        restrictionsPatterns.add(Pattern.compile(unescapedRestriction));
      }
      passwordRestrictionPatterns = restrictionsPatterns;
    }
  }
  
  
  private void refreshScopedRightCache() {
    scopeCache = new LruCache<String, ScopedRightCache>(XynaProperty.SCOPED_RIGHTS_CACHE_SIZE.get());
  }

  /**
   * Verschluesselt alle gespeicherten Passwoerter
   */
  private void encryptPersistedPasswords() {
    boolean alreadyEncrypted = persistenceEncryptionAlgorithm != null;
    persistenceEncryptionAlgorithm = XynaProperty.PASSWORD_PERSISTENCE_HASH_ALGORITHM.get();

    if (alreadyEncrypted) {
      return; //sind bereits bei der Persistierung verschluesselt worden
    }
    
    if (persistenceEncryptionAlgorithm == null) {
      return; //sollen nicht extra verschluesselt werden
    }
    
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);
    ODSConnection hisCon = requiredHistoryConnection(new User());

    try {
      dualEnsurePersistenceLayerConnectivity(defCon, hisCon, User.class);
      userLock.writeLock().lock();
      try {
        Collection<User> users = defCon.query(loadAllUsersForUpdateQuery, new Parameter(), -1);

        for (User user : users) {
          String hashed = user.getPassword();
          CreationAlgorithm oldAlgo = CreationAlgorithm.extractAlgorithm(hashed);
          if (persistenceEncryptionAlgorithm.equals(oldAlgo)) {
            logger.warn("It seems the password of user " + user.getName() + " is already encrypted");
            continue;
          }
          user.setPassword(hashed, true); //wird nun nochmals verschluesselt
        }
        persistCollection(defCon, users);
        persistCollection(hisCon, users);
      } finally {
        userLock.writeLock().unlock();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not encrypt persisted passwords.", e);
    } finally {
      try {
        dualCloseConnection(defCon, hisCon);
      }
      catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
  }
  
  
  private void validatePasswordAgainstRestrictions(String password) throws XFMG_PasswordRestrictionViolation {
    if (passwordRestrictionPatterns == null) {
      getAndSetIfChangedPasswordRestrictions();
    }
    for (Pattern pattern : passwordRestrictionPatterns) {
      Matcher matcher = pattern.matcher(password);
      if (!matcher.matches()) {
        throw new XFMG_PasswordRestrictionViolation(pattern.toString());
      }
    }
  }


  public List<Domain> getDomainsForUser(String username) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    List<String> domains;
    if (globalOverwrite) {
      domains = globalDomain;
    } else {
      ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

      try {
        defCon.ensurePersistenceLayerConnectivity(User.class);
        userLock.readLock().lock();
        try {
          User userToRetrieve = new User(username);
          defCon.queryOneRow(userToRetrieve);
          domains = userToRetrieve.getDomainList();
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new XFMG_UserDoesNotExistException(username);
        } finally {
          userLock.readLock().unlock();
        }
      } finally {
        defCon.closeConnection();
      }
    }

    List<Domain> result = new ArrayList<Domain>();
    ODSConnection defCon = ods.openConnection(ODSConnectionType.DEFAULT);

    try {
      defCon.ensurePersistenceLayerConnectivity(Domain.class);
      domainLock.readLock().lock();
      try {
        for (String domainname : domains) {
          Domain domainToCheck = new Domain(domainname);

          try {
            defCon.queryOneRow(domainToCheck);
            result.add(domainToCheck);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new XFMG_DomainDoesNotExistException(domainname);
          }
        }
      } finally {
        domainLock.readLock().unlock();
      }
    } finally {
      defCon.closeConnection();
    }

    return result;
  }
  
  
  public ScopedRightCache getRoleRightScope(Role role) throws PersistenceLayerException {
    ScopedRightCache scopes = scopeCache.get(role.getId());
    if (scopes == null) {
      Role refreshedRole = getRole(role.getName(), role.getDomain());
      scopes = new ScopedRightCache(refreshedRole.getName(), refreshedRole.getScopedRights(), getRightScopeMap() );
      scopeCache.put(refreshedRole.getId(), scopes);
    }
    return scopes;
  }


  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException {
    User user = getUser(userName);
    return PasswordExpiration.calculate(user);
  }


  public void setUserContextValue(String userName, String key, String value) throws PersistenceLayerException {
    userContextWrite(userName, key, value, value == null);
  }


  public boolean deleteUserContextVariable(String userName, String key) throws PersistenceLayerException {
    return userContextWrite(userName, key, null, true);
  }


  public List<UserContextEntryStorable> getUserContextValues(String userName) throws PersistenceLayerException {

    List<UserContextEntryStorable> result;

    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      hisCon.ensurePersistenceLayerConnectivity(UserContextEntryStorable.class);

      // No locking required - if some operation is currently working on this entry, either return old or new value
      PreparedQuery<UserContextEntryStorable> query =
          cache.getQueryFromCache(GET_ALL_USER_CONTEXT_VALUES, hisCon, UserContextEntryStorable.reader);
      result = hisCon.query(query, new Parameter(userName), -1);

    } finally {
      try {
        hisCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }

    Collections.sort(result, new Comparator<UserContextEntryStorable>() {

      public int compare(UserContextEntryStorable o1, UserContextEntryStorable o2) {
        if (o1.getKey() == null) {
          return -1;
        }
        if (o2.getKey() == null) {
          return 1;
        }
        return o1.getKey().compareTo(o2.getKey());
      }
    });

    return result;

  }


  public void resetUserContextValues(final String userName) throws PersistenceLayerException {
    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      hisCon.ensurePersistenceLayerConnectivity(UserContextEntryStorable.class);

      userLock.readLock().lock();
      try {
        userContextLocks.writeLock(userName);
        try {
          PreparedQuery<UserContextEntryStorable> query =
              cache.getQueryFromCache(GET_ALL_USER_CONTEXT_VALUES, hisCon, UserContextEntryStorable.reader);
          List<UserContextEntryStorable> existingEntries =
              hisCon.query(query, new Parameter(userName), -1);
          hisCon.delete(existingEntries);
          hisCon.commit();
        } finally {
          userContextLocks.writeUnlock(userName);
        }
      } finally {
        userLock.readLock().unlock();
      }

    } finally {
      try {
        hisCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }


  private boolean userContextWrite(String userName, String key, String value, boolean delete)
      throws PersistenceLayerException {

    if (userName == null) {
      throw new IllegalArgumentException();
    }
    if (key == null) {
      throw new IllegalArgumentException();
    }

    ODSConnection hisCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      hisCon.ensurePersistenceLayerConnectivity(UserContextEntryStorable.class);

      userLock.readLock().lock();
      try {

        userContextLocks.writeLock(userName);
        try {
          PreparedQuery<UserContextEntryStorable> query =
              cache.getQueryFromCache(GET_USER_CONTEXT_VALUE, hisCon, UserContextEntryStorable.reader);
          UserContextEntryStorable existingEntry = hisCon.queryOneRow(query, new Parameter(userName, key));
          if (delete) {
            return userContextWrite_Delete(hisCon, existingEntry);
          } else if (value != null) {
            return userContextWrite_Set(hisCon, existingEntry, userName, key, value);
          } else {
            throw new IllegalArgumentException();
          }
        } finally {
          userContextLocks.writeUnlock(userName);
        }

      } finally {
        userLock.readLock().unlock();
      }

    } finally {
      try {
        hisCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }

  }


  private boolean userContextWrite_Delete(ODSConnection hisCon, UserContextEntryStorable existingEntry)
      throws PersistenceLayerException {
    if (existingEntry == null) {
      return false;
    } else {
      hisCon.deleteOneRow(existingEntry);
      hisCon.commit();
      return true;
    }
  }


  private boolean userContextWrite_Set(ODSConnection hisCon, UserContextEntryStorable existingEntry, String userName,
                                       String key, String value) throws PersistenceLayerException {
    boolean existed = existingEntry != null;
    if (existingEntry == null) {
      existingEntry =
          new UserContextEntryStorable(XynaFactory.getInstance().getIDGenerator().getUniqueId(), userName, key, value);
    } else {
      existingEntry.setValue(value);
    }
    hisCon.persistObject(existingEntry);
    hisCon.commit();
    return existed;
  }

}
