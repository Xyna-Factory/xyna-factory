/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna;



import java.io.File;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.exceptions.Ex_InvalidPolicyFileException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.update.Updater;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivation;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xdev.XynaDevelopment;
import com.gip.xyna.xdev.XynaDevelopmentBase;
import com.gip.xyna.xdev.XynaDevelopmentPortal;
import com.gip.xyna.xdev.exceptions.XDEV_CYCLIC_DEPENDENCY;
import com.gip.xyna.xdev.exceptions.XDEV_ERRONEOUS_DEPENDENCY;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface;
import com.gip.xyna.xnwh.XynaFactoryWarehouse;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.startup.Startup;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;



public final class XynaFactory implements XynaFactoryBase {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaFactory.class);

  public static final String CLUSTER_NODE_SYS_PROP_KEY = "xynaclusternodename";
  
  private static final String FUTURE_EXECUTION_PROCESSING_THREAD = "FutureExecutionProcessingThread";

  static {
    GenericRMIAdapter.setLogger(CentralFactoryLogging.getLogger(GenericRMIAdapter.class));
  }
  


  private static volatile XynaFactoryBase factoryInstance = null;
  private static volatile XynaFactoryBase createdFactoryInstance = null;
  private static volatile String clusterNodeName = null;

  private XynaProcessingBase xprc;
  private XynaFactoryManagementBase xfmg;

  private static volatile boolean begunInitialization = false;
  private volatile boolean finishedInitialization = false;
  private volatile boolean isShuttingDown = false;
  private volatile boolean isStartingUp = false;

  private Map<Class<? extends XynaFactoryComponent>, XynaFactoryComponent> componentsToBeInitializedLaterOn =
      new HashMap<Class<? extends XynaFactoryComponent>, XynaFactoryComponent>();
  private Thread thisThread;

  private IDGenerator idGenerator;
  
  // statisch, damit die Mock-Factory beim Update nicht falche FE-Ids verteilt. 
  public final static FutureExecution futureExecutionInstance = new FutureExecution("factory", true);
  private final static FutureExecution futureExecutionInstanceInit = new FutureExecution("factoryInit", true);
  private long bootCntId;
  private int bootCount = -1;
  public static final long STARTTIME = System.currentTimeMillis();

  public Exception instantiatedAt;
  
  private final ReadWriteLock shutdownLock = new ReentrantReadWriteLock();
  private String shutdownLockCause;
  private static final String FACTORY_SHUTDOWN = "Factory-Shutdown";

  private XynaFactory() {
    instantiatedAt = new Exception("instantiated at");
  }


  public static XynaFactoryBase getInstance() {
    //TODO für bestimmte aufrufer nicht erlauben  Ist dies noch nötig?
    if (factoryInstance == null) {
      synchronized (XynaFactory.class) {
        if (createdFactoryInstance == null) {
          //hier sollte kein Aufruf innerhalb der Factory hingelangen können.  
          logger.trace( "XynaFactory instantiated as mock", new Exception("called from") );
          factoryInstance = new XynaFactory(); //FIXME diese Zeile sollte raus! nur wegen Abwärtskompatibilität hier, sollte hoffentlich nie gerufen werden  
        } else {
          factoryInstance = createdFactoryInstance;
        }
      }
    }
    return factoryInstance;
  }
  
  public static XynaFactoryBase createServerInstance() {
    //TODO Prüfen dass nur von XynaFactoryCommandLineInterface.init aufgerufen wird
    if (factoryInstance == null) {
      synchronized (XynaFactory.class) {
        if (createdFactoryInstance == null) {
          createdFactoryInstance = new XynaFactory(); //einzige Stelle, bei der regulär die Factory instantiiert wird
        }
        factoryInstance = createdFactoryInstance;
      }
    }
    return factoryInstance;
  }
  
  public static XynaFactoryPortal getPortalInstance() {
    return getInstance();
  }


  /**
   * Ersetzen der Factory-Instance durch einen Mock
   * @param xfi
   */
  public static void setInstance(XynaFactoryBase xfi) {
    //zum testen! //TODO überprüfen, dass man das darf
    factoryInstance = xfi;
  }
  
  
  /**
   * Zurücksetzen der Instance auf die bei Serverstart angelegte Instance
   */
  public static void resetInstance() {
    factoryInstance = createdFactoryInstance;
  }
  
  
  /**
   * Ist die Instance gerade durch einen Mock ersetzt?
   */
  //ist z.B. während der Updatephase true, wenn da UpdateGeneratedClasses o.ä. läuft
  public static boolean isInstanceMocked() {
    return factoryInstance != createdFactoryInstance;
  }


  /**
   * gibt true zurück, wenn die fabrik instanz existiert und kein mock ist
   */
  public static boolean isFactoryServer() {
    return factoryInstance != null && factoryInstance instanceof XynaFactory
        && createdFactoryInstance == factoryInstance;
  }


  public static boolean hasInstance() {
    return factoryInstance != null;
  }


  public XynaFactoryManagementBase getFactoryManagement() {
    return xfmg;
  }


  private void checkDirs() throws XynaException {
    File mdmclasses = new File(Constants.MDM_CLASSDIR);
    if (mdmclasses.exists()) {
      if (mdmclasses.isDirectory()) {
        return;
      } else {
        throw new RuntimeException(Constants.MDM_CLASSDIR + " must be a directory");
      }
    } else {
      mdmclasses.mkdirs();
    }
  }
  
  /*
   * nur für performance
   */
  private static class SecMan1 extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
    }


    @Override
    public void checkPermission(Permission perm, Object context) {
    }


    @Override
    public void checkCreateClassLoader() {
    }


    @Override
    public void checkAccess(Thread t) {
    }


    @Override
    public void checkAccess(ThreadGroup g) {
    }


    @Override
    public void checkExit(int status) {
    }


    @Override
    public void checkExec(String cmd) {
    }


    @Override
    public void checkLink(String lib) {
    }


    @Override
    public void checkRead(FileDescriptor fd) {
    }


    @Override
    public void checkRead(String file) {
    }


    @Override
    public void checkRead(String file, Object context) {
    }


    @Override
    public void checkWrite(FileDescriptor fd) {
    }


    @Override
    public void checkWrite(String file) {
    }


    @Override
    public void checkDelete(String file) {
    }


    @Override
    public void checkConnect(String host, int port) {
    }


    @Override
    public void checkConnect(String host, int port, Object context) {
    }


    @Override
    public void checkListen(int port) {
    }


    @Override
    public void checkMulticast(InetAddress maddr) {
    }


    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
    }


    @Override
    public void checkPropertiesAccess() {
    }


    @Override
    public void checkPropertyAccess(String key) {
    }


    @Override
    public void checkPackageAccess(String pkg) {
    }


    @Override
    public void checkPackageDefinition(String pkg) {
    }


    @Override
    public void checkSetFactory() {
    }


    @Override
    public void checkSecurityAccess(String target) {
    }

  }


  public void init() throws XynaException {
    
    GenerationBase.removeFromCache = false; //für updates und wf-database
    isStartingUp = true;

    //bugz 11042: fehler im server.policy file frühzeitig entdecken
    SecurityManager securityManager = System.getSecurityManager();
    if (securityManager == null) {
      System.setSecurityManager(Constants.NORMAL_SECURITY_MANAGER ? new SecurityManager() : new SecMan1());
      securityManager = System.getSecurityManager();
    }    
    try {
      securityManager.checkPermission(new XynaUsagePermission(null));
    } catch (SecurityException e) {
      throw new Ex_InvalidPolicyFileException(Constants.SERVER_POLICY, XynaUsagePermission.class.getName(), e);
    }
    
    setSecurityProvider();

    thisThread = Thread.currentThread();
    synchronized (this) {
      // at the moment we statically limit the number of instances to one
      if (begunInitialization) {
        throw new RuntimeException("Xyna Factory is already starting");
      }
      begunInitialization = true;
    }

    // initialize the builderfactory instances etc
    try {
      XMLUtils.initialize();
    } catch (Throwable t) {
      Department.handleThrowable(t);
      throw new XynaException("Failed to initialize the XML utilities", t);
    }
    
    //Bug 17897 ClassLoader-Deadlock? hier Single-threaded laden
    TimeConstraint.immediately();
    
    //ConnectionPools sollen das Cleanup über das ShutdownHookManagement ausführen
    ConnectionPool.setCleanupWrapper(new ConnectionPool.CleanupWrapper() {
      public void registerCleanup(Runnable hook) {
        ShutdownHookManagement.getInstance().addTask(hook);
      }
    });
    
    ODSImpl.getInstance();

    XynaClusteringServicesManagement.getInstance();
    
    ConnectionPoolManagement.getInstance();

    //zwei getrennte future-execution instanzen, eine für die tasks die bis hierhin passieren sollen
    //und eine für die danach
    futureExecutionInstanceInit.finishedRegistrationProcess();

    clusterNodeName = System.getProperty(CLUSTER_NODE_SYS_PROP_KEY);

    boolean updateError = false;
    XynaException updateException = null;
    try {
      Updater.getInstance().checkUpdate();
    } catch (XynaException e) {
      updateError = true;
      updateException = e;
      logger.warn("Update failed, server might be in inconsistent state. Update will be retried at next "
          + Constants.FACTORY_NAME + " startup.", e);
    }

    //objekte wurden evtl mit regenerateDeployed deployed
    // => dann sind sie im state cleanup, haben aber keine deploymenthandler ausgeführt. das muss dann noch geschehen.
    GenerationBase.clearGlobalCache();  

    // instantiate IDGenerator so that it registers itself as a late init component
    idGenerator = IDGenerator.getInstance();
    //durch ein Update wurde in XynaOrder evtl. für den idGenerator 'null' gechached, daher hier noch einmal explizit setzen
    XynaOrder.setIDGenerator(idGenerator);
    
    //bootCntId ermitteln
    getFutureExecution().addTask("XynaFactory.getBootCntId","XynaFactory.getBootCntId").
      after(IDGenerator.class).
      execAsync(new Runnable() {
        public void run() { 
          bootCntId = idGenerator.getUniqueId();
        }
      });
    setBootCount();
  
    //Prüfung des MDM-Verzeichnisses
    checkDirs();
    if (logger.isDebugEnabled()) {
      logger.debug("classloader: " + getClass().getClassLoader());
    }
    // First initialize Xyna Factory Management and Xyna Processing
    xfmg = new XynaFactoryManagement();
    xprc = new XynaProcessing();

    xfmg.getXynaExtendedStatusManagement().registerStep(StepStatus.STARTUP, "XynaFactory", "starting ...");
    
    // Create Xyna Multi-Channel Portal (Xyna Order Entrance etc.)
    new XynaMultiChannelPortalSecurityLayer();

    // Create Xyna Activation
    new XynaActivation();

    new XynaDevelopment();

    new XynaFactoryWarehouse();
    

    logger.debug("Initializing late initializers...");

    HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> allDependencies = XynaFactoryComponent
                    .getDependencies();

    // TODO this should be called three separate times (first departments, then sections, then function groups)
    initLateInitComponents(allDependencies); //FutureExecution wird unten im StartSuspendedOrdersThread fertiggestellt.

    // Finally: Startup workflow
    ((Startup) xprc.getSection(XynaProcessCtrlExecution.DEFAULT_NAME).getFunctionGroup(Startup.DEFAULT_NAME))
        .executeStartupWorkflow();

    if (updateError) {
      // remind the user that at least one update failed because the debug output might have flooded the screen
      logger.warn("Some updates failed, note that the server might be in an inconsistent state.");
      if (updateException != null) {
        logger.warn("Error during update: " + updateException.getMessage());
      }
    }

    final CountDownLatch futureExecutionsProcessed = new CountDownLatch(1);
    final AtomicReference<Throwable> futureExecutionFailedException = new AtomicReference<Throwable>();
    
    Thread t = new Thread(FUTURE_EXECUTION_PROCESSING_THREAD) {
      @Override
      public void run() {
        //Starten der FutureExecutions
        try {
          getFutureExecution().finishedRegistrationProcess();
          XynaClusteringServicesManagement.getInstance().setGlobalReadyForChange(true);
        } catch (Throwable t) {
          futureExecutionFailedException.set(t);
          Department.handleThrowable(t);
          logger.error("Error while executing future executions.", t);
        } finally {
          futureExecutionsProcessed.countDown();
        }
      }
    };
    t.start();

    try {
      futureExecutionsProcessed.await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Got interrupted unexpectedly waiting for future executions", e);
    }
    if (futureExecutionFailedException.get() != null ) {
      throw new RuntimeException("Critical factory components could not be initialized. See log file for further information", 
          futureExecutionFailedException.get() );
    }
    
    getProcessing().getXynaScheduler().checkInitialization();

    finishedInitialization = true;
    isStartingUp = false;
    GenerationBase.removeFromCache = true;
    GenerationBase.clearGlobalCache(); 
    
    xfmg.getXynaExtendedStatusManagement().deregisterStep("XynaFactory");
  }


  private void setSecurityProvider() {
    String secprovclazz = System.getProperty("security.provider.class");
    if (secprovclazz != null) {
      try {
        Class<? extends Provider> c = (Class<? extends Provider>) Class.forName(secprovclazz);
        Provider secprov = c.getConstructor().newInstance();
        Security.insertProviderAt(secprov, 1);
      } catch (Throwable t) {
        logger.error("Could not set Security Provider", t);
      }
    }
  }


  private void setBootCount() throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(BootCountStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      BootCountStorable b = new BootCountStorable();
      b.setId(1);
      try {
        con.queryOneRow(b);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        b.setBootcount(0);
      }
      b.setBootcount(b.getBootcount() + 1);
      con.persistObject(b);
      con.commit();
      bootCount = b.getBootcount();
    } finally {
      con.closeConnection();
    }
  }


  public void shutdown() {
    if (!shutdownLock.readLock().tryLock()) {
      logger.info("XynaFactory is already shutting down.");
      return;
    }
    shutdownLockCause = FACTORY_SHUTDOWN;
    try {
      if (XynaFactory.getInstance().isShuttingDown()) {
        logger.info("XynaFactory is already shutting down.");
        return;
      }

      while (XynaFactory.getInstance().isStartingUp()) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          return;
        }
      }

      logger.info("Shutting down now");
      XynaFactoryCommandLineInterface.shutdown(); //ruft über XynaFactoryCommandLineInterface-Thread shutdownComponents() auf
      while (!XynaFactory.getInstance().isShuttingDown()) {
        try {
          Thread.sleep(20);
        } catch (InterruptedException e) {
        }
      }
    } finally {
      shutdownLock.readLock().unlock();
    }
  }

  
  
  public void shutdownComponents() throws XynaException {
    isShuttingDown = true;

    //Shutdown-Reihenfolge ist hier wichtig:
    //1. eigene laufende Workflows im Processing beenden: stopGracefully
    //2. dann erst den Cluster verlassen: dann wird anderer Knoten DISCONNECTED_MASTER und übernimmt Aufträge, 
    //   die in stopGracefully hoffentlich beendet wurden.
    //3. TODO weitere Reihenfolge überdenken: sollte CronLikeScheduler nich früher beendet werden?
        
    
    //damit nicht jemand das shutdownlock holen kann, während die komponenten bereits am runterfahren sind.
    //falls jemand das lock bereits hält, muss er es freigeben, bevor das shutdown fortsetzen darf.
    //TODO für den fall, dass die application irgendwie hängt, die das lock hält, sollte man hier noch einen weg 
    //ermöglichen, dass der shutdown irgendwann forciert fortgesetzt wird. timeout ist gefährlich. vielleicht ein cli-befehl?
    if (!shutdownLock.writeLock().tryLock()) {
      if( shutdownLockCause.equals(FACTORY_SHUTDOWN) ) {
        //Lock-Übergabe an XynaFactoryCommandLineInterface-Thread sollte innerhalb weniger Millisekunden stattfinden.
        //Siehe shutdown()-Methode oben
      } else {
        logger.info("shutdown is temporarily suspended by some application thread ("+shutdownLockCause+") and will continue as soon as it has finished.");
      }
      shutdownLock.writeLock().lock();
    }
    
    try {

      //Workflows im Processing beenden
      try {
        if (finishedInitialization) {
          getProcessing().stopGracefully();
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("Failed to stop processing gracefully: <" + t.getMessage()
            + ">. Trying to shut down departments anyway.", t);
      }

      //Cluster verlassen 
      if (xfmg != null) {
        XynaClusteringServicesManagementInterface xcsm = xfmg.getXynaClusteringServicesManagement();
        if (xcsm != null) {
          try {
            ((XynaClusteringServicesManagement) xcsm).shutdownDirectly();
          } catch (Throwable t) {
            logger.error("Error shutting down Clustering Services Management", t);
          }
        }
      }

      // for the following two commands any Throwable should be caught and handled since the departments
      // should be shutdown anyway
      // TODO insert a mechanism to define the order in which the components are shut down?
      // => FutureExecution verwenden

      try {
        if (xfmg == null) {
          return;
        }
        ArrayList<Department> departments = xfmg.getComponents().getDepartmentsAsList();

        for (Department d : departments) {
          if (!(d instanceof XynaFactoryManagement)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Shutting down Department " + d.getDefaultName() + "...");
            }
            try {
              d.shutDownInternally();
            } catch (Throwable t) {
              Department.handleThrowable(t);
              logger.error("Error while shutting down Department <" + d.getDefaultName() + ">", t);
            }
          }
        }

        // Xyna Factory Management zum Schluss
        if (logger.isDebugEnabled()) {
          logger.debug("Shutting down Department " + xfmg.getDefaultName() + "...");
        }
        xfmg.shutdown();

      } finally {
        shutdownIDGenerator();
        shutdownODS();
        factoryInstance = null;
      }
    } finally {
      shutdownLock.writeLock().unlock();
    }
  }


  private void shutdownIDGenerator() {
    try {
      if (idGenerator != null) {
        idGenerator.shutdown();
      } else if (logger.isInfoEnabled()) {
        logger.info("Cannot shut down " + IDGenerator.class.getSimpleName()
            + ", it has not been added to the factory during startup");
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Failed to shut down " + IDGenerator.class.getSimpleName(), t);
    }
  }


  private void shutdownODS() {
    ODS ods = ODSImpl.getInstance();
    try {
      ods.shutdown();
    } catch (Throwable t) {
      logger.error("Failed to shut down ods.", t);
    }
  }


  public XynaActivation getActivation() {
    XynaFactoryManagementBase fmg = getFactoryManagement();
    if (fmg != null && fmg.getComponents() != null) {
      return (XynaActivation) fmg.getComponents().getDepartment(XynaActivation.DEFAULT_NAME);
    } else {
      logger.warn("Tried to access unregistered department " + XynaActivation.DEFAULT_NAME);
      return null;
    }
  }


  public XynaProcessingBase getProcessing() {

    if (xprc != null) {
      return xprc;
    }
    else {
      XynaFactoryManagementBase fmg = getFactoryManagement();
      if (fmg != null && fmg.getComponents() != null) {
        return (XynaProcessingBase) fmg.getComponents().getDepartment(XynaProcessing.DEFAULT_NAME);
      } else {
        logger.warn("Tried to access unregistered department " + XynaProcessing.DEFAULT_NAME);
        return null;
      }

    }

  }


  public XynaFactoryWarehouseBase getXynaNetworkWarehouse() {
    XynaFactoryManagementBase fmg = getFactoryManagement();
    if (fmg != null && fmg.getComponents() != null) {
      return (XynaFactoryWarehouseBase) fmg.getComponents()
                      .getDepartment(XynaFactoryWarehouse.DEFAULT_NAME);
    } else {
      logger.warn("Tried to access unregistered department " + XynaFactoryWarehouse.DEFAULT_NAME);
      return null;
    }
  }


  public XynaMultiChannelPortal getXynaMultiChannelPortal() {
    XynaFactoryManagementBase fmg = getFactoryManagement();
    if (fmg != null && fmg.getComponents() != null) {
      return (XynaMultiChannelPortal) fmg.getComponents()
                      .getDepartment(XynaMultiChannelPortal.DEFAULT_NAME);
    } else {
      logger.warn("Tried to access unregistered department " + XynaMultiChannelPortal.DEFAULT_NAME);
      return null;
    }
  }

  public XynaDevelopmentBase getXynaDevelopment() {
    XynaFactoryManagementBase fmg = getFactoryManagement();
    if (fmg != null && fmg.getComponents() != null) {
      return (XynaDevelopmentBase) fmg.getComponents().getDepartment(XynaDevelopment.DEFAULT_NAME);
    } else {
      logger.warn("Tried to access unregistered department " + XynaDevelopment.DEFAULT_NAME);
      return null;
    }
  }


  public void addComponentToBeInitializedLater(XynaFactoryComponent lateInitComponent) {

    if (Thread.currentThread() != thisThread &&
        !Thread.currentThread().getName().equals(FUTURE_EXECUTION_PROCESSING_THREAD)) {
      logger.error("late initializers cannot be registered outside the main thread, ignoring request, component " + lateInitComponent
                                      .getClass().getSimpleName() + " not scheduled for later loading!");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("adding late initializer " + lateInitComponent.getDefaultName());
    }
    if (!(lateInitComponent instanceof FunctionGroup) && !(lateInitComponent instanceof Section) && !(lateInitComponent instanceof Department))
      throw new RuntimeException(new XDEV_UNSUPPORTED_FEATURE("only functiongroup, section or deparments may be added as late initializers"));

    // TODO if we want to support dependencies of sections and departments, the existing mechanism
    // has to be extended because at the moment, only the factory initializes the late ones. In the
    // extended case the function groups of a late section have not even registered as depending on
    // a different component so that would require three steps: first initialize the departments,
    // then sections and then function groups

    componentsToBeInitializedLaterOn.put(lateInitComponent.getClass(), lateInitComponent);

  }


  public void initLateInitComponents(HashMap<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> allDependenciesOfAllComponents)
      throws XynaException {

    boolean foundHigherDependency = false;
    while (allDependenciesOfAllComponents.size() > 0) {

      for (Entry<Class<? extends XynaFactoryComponent>, List<XynaFactoryPath>> e : allDependenciesOfAllComponents
          .entrySet()) {
        List<XynaFactoryPath> pathes = e.getValue();

        foundHigherDependency = false;

        for (XynaFactoryPath path : pathes) {

          if (allDependenciesOfAllComponents.containsKey(path.getDepartment())) {
            if (logger.isDebugEnabled())
              logger.debug("Found other dependency for " + e.getKey() + ": Department " + path.getDepartment()
                              + " has more dependencies");
            foundHigherDependency = true;
          } else if (allDependenciesOfAllComponents.containsKey(path.getSection())) {
            if (logger.isDebugEnabled())
              logger.debug("Found other dependency for " + e.getKey() + ": Section " + path.getSection()
                              + " has more dependencies");
            foundHigherDependency = true;
          } else if (allDependenciesOfAllComponents.containsKey(path.getFunctionGroup())) {
            if (logger.isDebugEnabled())
              logger.debug("Found other dependency for " + e.getKey() + ": FunctionGroup " + path.getFunctionGroup()
                              + " has more dependencies");
            foundHigherDependency = true;
          }

        }

        if (!foundHigherDependency) {
          XynaFactoryComponent nextToBeInitialized = componentsToBeInitializedLaterOn.remove(e.getKey());
          if (nextToBeInitialized != null) {
            if (logger.isDebugEnabled()) {
              logger.debug("Did not find higher dependencies for " + e.getKey() + ", performing initialization...");
            }
            nextToBeInitialized.initInternally();
            allDependenciesOfAllComponents.remove(e.getKey());
            break;
          } else {
            //null kann nur passieren, wenn eine komponente mehrfach als abhängig von der gleichen komponente angegeben wurde.

            //FIXME das kann auch passieren, wenn die klasse, die dependencies hat (statisch gesetzt), nicht die gleiche ist, wie die 
            //instanz der klasse, die initialisiert wird.
            //beispiel: section rmichannelimplsessionextension ist von rmichannelimpl abgeleitet. rmichannelimpl könnte abhängigkeiten haben, dann gibt es diesen fehler.
            throw new XDEV_ERRONEOUS_DEPENDENCY(e.getKey().getSimpleName());
          }
        }

      }

      if (foundHigherDependency) {
        StringBuffer message = new StringBuffer();
        for (Class<? extends XynaFactoryComponent> xfc : allDependenciesOfAllComponents.keySet()) {
          StringBuffer deps = new StringBuffer();
          for (XynaFactoryPath xfp : allDependenciesOfAllComponents.get(xfc)) {
            StringBuffer dep = new StringBuffer("\n    o ");
            if (xfp.getDepartment() != null) {
              dep.append(xfp.getDepartment().getSimpleName());
            }
            if (xfp.getSection() != null) {
              dep.append(">").append(xfp.getSection().getSimpleName());
            }
            if (xfp.getFunctionGroup() != null) {
              dep.append(">").append(xfp.getFunctionGroup().getSimpleName());
            }
            deps.append(dep);
          }
          message.append(xfc.getSimpleName()).append(" => ").append(deps).append("\n");
        }
        throw new XDEV_CYCLIC_DEPENDENCY(message.toString());
      }

    }

  }


  public boolean isShuttingDown() {
    return isShuttingDown;
  }


  public boolean isStartingUp() {
    return isStartingUp;
  }


  public boolean finishedInitialization() {
    return finishedInitialization;
  }


  public XynaActivationPortal getActivationPortal() {
    return getActivation();
  }


  public XynaFactoryManagementPortal getFactoryManagementPortal() {
    return getFactoryManagement();
  }


  public XynaProcessingPortal getProcessingPortal() {
    return getProcessing();
  }


  public XynaDevelopmentPortal getXynaDevelopmentPortal() {
    return getXynaDevelopment();
  }


  public Channel getXynaMultiChannelPortalPortal() {
    return getXynaMultiChannelPortal();
  }


  public XynaMultiChannelPortalSecurityLayer getXynaMultiChannelPortalSecurityLayer() {
    XynaFactoryManagementBase fmg = getFactoryManagement();
    if (fmg != null && fmg.getComponents() != null)
      return (XynaMultiChannelPortalSecurityLayer) fmg.getComponents()
          .getDepartment(XynaMultiChannelPortalSecurityLayer.DEFAULT_NAME);
    else {
      logger.warn("Tried to access unregistered department " + XynaMultiChannelPortalSecurityLayer.DEFAULT_NAME);
      return null;
    }
  }


  public XynaFactoryWarehousePortal getXynaNetworkWarehousePortal() {
    return getXynaNetworkWarehouse();
  }

  public static ReturnCode getStatusCodeSLESLike() {
    if (factoryInstance == null) {
      return ReturnCode.STATUS_STOPPING; // kann beim startup eine sehr sehr kurze Phase null sein und beim Shutdown eine 
                                                              // längere Phase (wenn Shutdown-Hooks sehr lange brauchen zBsp)
    }
    if (factoryInstance.isShuttingDown()) {
      return ReturnCode.STATUS_STOPPING; // "service stopping"
    }
    if (factoryInstance.isStartingUp()) {
      return ReturnCode.STATUS_STARTING; // "service starting"
    }
    if (factoryInstance.finishedInitialization()) {
      return ReturnCode.STATUS_UP_AND_RUNNING; // "service up and running"
    }

    return ReturnCode.STATUS_SERVICE_NOT_RUNNING; // "service status unkown :-("
  }


  public IDGenerator getIDGenerator() {
    if (this.idGenerator == null) {
      throw new RuntimeException("Tried to get ID generator when it has not been instantiated yet.");
    }
    return this.idGenerator;
  }


  public static void setupAsPropertyProvider(Map<String, String> initialProperties) {
    if (initialProperties == null) {
      throw new IllegalArgumentException("Properties map may not be null.");
    }
    
    if (factoryInstance != null) {
      if( factoryInstance instanceof XynaFactoryPropertiesOnly ) {
        //dopplete Initialisierung der XynaFactoryPropertiesOnly ist halbwegs OK
        logger.warn("setupAsPropertyProvider called again, combining properties");
        XynaFactoryPropertiesOnly xfpo = (XynaFactoryPropertiesOnly)factoryInstance;
        xfpo.addProperties(initialProperties);
      } else {
        throw new IllegalStateException("Factory may only be used as a property provider outside of a running factory.");
      }
    }

    Map<String, String> concurrentMapClone = new ConcurrentHashMap<String, String>(initialProperties);
    try {
      setInstance(new XynaFactoryPropertiesOnly(concurrentMapClone));
    } catch (XynaException e) {
      throw new RuntimeException("Unexpected exception while setting up factory as simple properties provider: "
          + e.getMessage(), e);
    }

  }


  public static String getClusterNodeName() {
    return clusterNodeName;
  }


  public FutureExecution getFutureExecution() {
    return futureExecutionInstance;
  }
  
  public FutureExecution getFutureExecutionForInit() {
    return futureExecutionInstanceInit;
  }
  
  public int getBootCount() {
    return bootCount;
  }
  
  public long getBootCntId() {
    return bootCntId;
  }


  public boolean lockShutdown(String cause) {
    boolean locked = shutdownLock.readLock().tryLock();
    if( locked ) {
      shutdownLockCause = cause;
      if (logger.isInfoEnabled()) {
        logger.info("shutdown locked by <" + cause + ">");
      }
    }
    return locked;
  }


  public void unlockShutdown() {
    shutdownLock.readLock().unlock();
    logger.info("shutdown unlocked");
  }


  
  
}
