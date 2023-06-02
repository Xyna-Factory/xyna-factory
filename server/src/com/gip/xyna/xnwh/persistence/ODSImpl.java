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

package com.gip.xyna.xnwh.persistence;



import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.concurrent.DistributedWork;
import com.gip.xyna.utils.concurrent.ExceptionGatheringFutureCollection;
import com.gip.xyna.utils.concurrent.ExceptionGatheringFutureCollection.GatheredException;
import com.gip.xyna.utils.exception.MultipleExceptionHandler;
import com.gip.xyna.utils.exception.MultipleExceptions;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcherFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.PersistenceLayerClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_ConnectionClosedException;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_MissingAnnotationsException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerClassIncompatibleException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownForRegisteredTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceMayNotBeDeletedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceNotRegisteredException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_TooManyDedicatedConnections;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabasePersistenceLayerConnectionWithAlterTableSupport;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer.TransactionMode;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;



/**
 * TODO - classloading beachten, wenn mdm objekte gespeichert/geladen werden (siehe rmi)
 */
public class ODSImpl implements ODS {

  private static final Logger logger = CentralFactoryLogging.getLogger(ODSImpl.class);
  public static final String CONNECTIONCLASSNAME = ODSConnectionImpl.class.getName();
  private static final Logger loggerForSQLLogger = Logger.getLogger("ods.sqllogger"); //in der log4j config separat von dem odsimpl.logger behandelbar //TODO in centralfacvtorylogging integrieren.
  private static final SQLLogger sqlLogger = new SQLLogger(loggerForSQLLogger, CONNECTIONCLASSNAME);
  
  private static final Comparator<PersistenceLayerInstanceBean> COMPARATOR_PLIS = new Comparator<PersistenceLayerInstanceBean>() {

    public int compare(PersistenceLayerInstanceBean o1, PersistenceLayerInstanceBean o2) {
      long i1 = o1.getPersistenceLayerInstanceID();
      long i2 = o2.getPersistenceLayerInstanceID();
      if (i1 == i2) {
        return 0;
      }
      return i1 < i2 ? 1 : -1;
    }

  };
  
  private static class SQLLogger {
    
    private Logger internalLogger;
    private String classname;
    
    public SQLLogger(Logger logger, String classname) {
      this.internalLogger = logger;
      this.classname = classname;
    }

    public void debug(String s) {
      internalLog(Level.DEBUG, s, null);
    }
    
    public void debug(String s, Throwable t) {
      internalLog(Level.DEBUG, s, t);
    }
    
    public void info(String s) {
      internalLog(Level.INFO, s, null);
    }
    
    public void info(String s, Throwable t) {
      internalLog(Level.INFO, s, t);
    }
    
    public void trace(String s) {
      internalLog(Level.TRACE, s, null);
    }
    
    public void trace(String s, Throwable t) {
      internalLog(Level.TRACE, s, t);
    }

    /**
     * Sorgt auf interessante Weise für das richtige Setzen des aufrufenden Zeile: (keine Zeile dieser Datei, sondern
     * der Aufrufer) siehe http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Category.html
     * http://marc.info/?l=log4j-user&m=99859247618691&w=2 That's why you need to provide the fully-qualified classname.
     * If you use the wrapper I showed, you will not have this problem. That is, the class and method name of the
     * wrapper caller, not the wrapper itself, will be logged. The code which determines the logging method looks one
     * past the fully-qualified classname in the callstack.
     * @param level
     * @param str
     * @param t
     */
    private void internalLog(Level level, String str, Throwable t) {
      internalLogger.log(classname, level, "@ods: " + str, t);
    }

    public boolean isDebugEnabled() {
      return internalLogger.isDebugEnabled();
    }

    public boolean isTraceEnabled() {
      return internalLogger.isTraceEnabled();
    }
    

    public boolean isInfoEnabled() {
      return internalLogger.isInfoEnabled();
    }
  }


  public static final String MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.local.XynaLocalMemoryPersistenceLayer";
  public static final String JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.javaserialization.XynaJavaSerializationPersistenceLayer";
  public static final String MYSQL_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.mysql.MySQLPersistenceLayer";
  public static final String ORACLE_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.oracle.OraclePersistenceLayer";
  public static final String XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.xmlshell.XynaXMLShellPersistenceLayer";
  public static final String XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.devnull.XynaDevNullPersistenceLayer";
  public static final String XYNA_XML_PERSISTENCE_LAYER_FQ_CLASSNAME
        = "com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer";

  private static final Map<String, String> defaultPlNames = new HashMap<String, String>();
  static {
    defaultPlNames.put(MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME, "memory");
    defaultPlNames.put(JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME, "javaserialization");
    defaultPlNames.put(MYSQL_PERSISTENCE_LAYER_FQ_CLASSNAME, "mysql"); 
    defaultPlNames.put(ORACLE_PERSISTENCE_LAYER_FQ_CLASSNAME, "oracle");
    defaultPlNames.put(XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME, "xmlshell");
    defaultPlNames.put(XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME, "devnull");
    defaultPlNames.put(XYNA_XML_PERSISTENCE_LAYER_FQ_CLASSNAME, "xml");
  }
  

  private static final long MEMORY_PERSISTENCE_LAYER_ID = 0L;
  private static final long XML_PERSISTENCE_LAYER_ID = 3L;

  private boolean readConfigFromXML;
  private AtomicLong nextSuitablePersistenceLayerId;
  private ConnectionPoolStatistics connectionPoolStats;
  private static boolean preInitialized = false;

  public static boolean isPreInitialized() {
    return preInitialized;
  }

  private ODSImpl(boolean readConfigFromXML) {
    this.readConfigFromXML = readConfigFromXML;
    try {
      init();
    } catch (XynaException e) {
      throw new RuntimeException("could not initialize ODSImpl", e);
    }
  }
  
  private static volatile ODSImpl instance;
  private static volatile ODSImpl instanceNonPersistent;

  public static ODSImpl getInstance() {
    return getInstance(true);
  }
  
  public static ODSImpl getInstance(boolean readConfigFromXML) {
    if (readConfigFromXML) {
      ODSImpl result = instance;
      if (result == null) {
        if (XynaFactory.isFactoryServer()) {
          synchronized (ODSImpl.class) {
            result = instance;
            if (result == null) {
              result = instance = new ODSImpl(readConfigFromXML);
            }
          }
       } else {
          return getInstance(false);
        }
      }
      return result;
    } else {
      ODSImpl result = instanceNonPersistent;
      if (result == null) {
        synchronized (ODSImpl.class) {
          result = instanceNonPersistent;
          if (result == null) {
            result = instanceNonPersistent = new ODSImpl(readConfigFromXML);
          }
        }
      }
      return instanceNonPersistent;
    }
  }


  public static void clearInstances() {// for testing/update purposes only!!
    instance = null;
    instanceNonPersistent = null;
  }


  public void shutdown() throws PersistenceLayerException {
    Set<PersistenceLayer> uniquePersistenceLayerInstances = new HashSet<PersistenceLayer>();
    for (PersistenceLayerInstanceBean bean : registeredPersistenceLayerInstances.values()) {
      uniquePersistenceLayerInstances.add(bean.getPersistenceLayerInstance());
    }
    
    PersistenceLayerException exceptionFromPlShutdown = null;
    if (uniquePersistenceLayerInstances.size() > 0) {
      ExecutorService plStopExecutor = Executors.newFixedThreadPool(10);
      try {
        ((ThreadPoolExecutor)plStopExecutor).setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ExceptionGatheringFutureCollection<Void> plShutdownResults = new ExceptionGatheringFutureCollection<Void>();
        for (final PersistenceLayer pl : uniquePersistenceLayerInstances) {
          plShutdownResults.add(plStopExecutor.submit(new Callable<Void>() {
            public Void call() throws Exception {
              pl.shutdown();
              return null;
            }
          }));
        }
        try {
          plShutdownResults.getWithExceptionGathering();
        } catch (GatheredException e) {
          exceptionFromPlShutdown = new XNWH_GeneralPersistenceLayerException("Not all persistence layer instances could be shut down successfully.");
          exceptionFromPlShutdown.initCauses(e.getCauses().toArray(new Throwable[e.getCauses().size()]));
        }    
      } finally {
        plStopExecutor.shutdownNow();
      }
    }

    FactoryWarehouseCursor.threadPool.shutdownNow();

    if (connectionPoolStats != null) {
      // can be null on shutdown after error during factory startup
      connectionPoolStats.shutdown();
    }
    if (exceptionFromPlShutdown != null) {
      throw exceptionFromPlShutdown;
    }
  }


  public ODSConnectionImpl openConnection() {
    return new ODSConnectionImpl(ODSConnectionType.DEFAULT);
  }


  public ODSConnectionImpl openConnection(ODSConnectionType conType) {
    return new ODSConnectionImpl(conType);
  }
  
  
  public ODSConnectionImpl openDedicatedConnection(ODSConnectionType conType, List<Class<? extends Storable<?>>> storableClazzes) throws XNWH_TooManyDedicatedConnections {
    return new ODSConnectionImpl(conType, true);
  }

  /**
   * Note: this class has a natural ordering that is inconsistent with equals.
   */
  private class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {
    
    private Integer prio;
    private Runnable runnable;
    
    public PriorityRunnable(Runnable runnable, int prio) {
      this.runnable = runnable;
      this.prio = prio;
    }
    
    public PriorityRunnable(Runnable runnable) {
      this(runnable, Thread.NORM_PRIORITY);
    }

    public void run() {
      runnable.run();
    }

    public int compareTo(PriorityRunnable o) {
      return prio.compareTo(o.prio) * (-1); //hohe zahlen bevorzugen -> das entspricht den thread-priority bedeutungen
    }    
  }
  

  private class ODSConnectionImpl implements ODSConnection {

    //nicht-transaktions-bezogene Daten, überdauern eine Transaktion
    private final ODSConnectionType conType;
    private final boolean isDedicated;
    private boolean closed;
    private final Map<String, PersistenceLayerConnection> openConnectionsPerTable; //enthält Connections evtl. mehrfach
    private final Map<Long, PersistenceLayerConnection> openConnectionsPerPLI; //enthält jede Connection nur einfach
    private Set<TransactionProperty> transactionProperties;
    private List<FactoryWarehouseCursor<?>> cursors;
    private PriorityQueue<PriorityRunnable> afterCloseHandlers;
   
    //transaktions-bezogene Daten, werden zu Ende jeder Transaktion geleert
    private final Map<String, PersistenceLayerConnection> usedConnectionsInTransaction; //alle in der Transaktion verwendete Tabellen/Connections
    private PriorityQueue<PriorityRunnable> afterCommitHandlers;
    private PriorityQueue<PriorityRunnable> afterCommitFailsHandlers;
    private PriorityQueue<PriorityRunnable> afterRollbackHandlers;


    private ODSConnectionImpl(ODSConnectionType conType) {
      this(conType, false);
    }

    
    private ODSConnectionImpl(ODSConnectionType conType, boolean dedicated) {
      this.conType = conType;
      isDedicated = dedicated;
      closed = false;
      openConnectionsPerTable = new HashMap<String, PersistenceLayerConnection>();
      openConnectionsPerPLI = new HashMap<Long, PersistenceLayerConnection>();
      usedConnectionsInTransaction = new HashMap<String, PersistenceLayerConnection>();
    }
    

    public boolean isOpen() {
      return !closed;
    }


    /**
     * schliesst die connections in konsistenter reihenfolge (sortiert nach comparator<tabellenname>, default = alphabetisch (TODO oder rückwärts))
     *  TODO wofür ist Reihenfolge nötig?
     *
     */
    public void closeConnection() throws PersistenceLayerException {
      try {
        if (closed) {
          return;
        }
        
        if (sqlLogger.isDebugEnabled()) {
          sqlLogger.debug(conType + " CLOSE "+openConnectionsPerPLI.size()+" connections" );
        }
        
        List<Throwable> closeFailures = null;

        //Cursors schließen
        if (cursors != null) {
          for (FactoryWarehouseCursor<?> cursor : cursors) {
            closeFailures = closeCursor(cursor, closeFailures);
          }
        }
        int failedCursors = closeFailures != null ? closeFailures.size() : 0;

        //Connections schließen
        for (PersistenceLayerConnection con : openConnectionsPerPLI.values()) {
          closeFailures = closeConnection(con,closeFailures);
        }
        int failedCons = (closeFailures != null ? closeFailures.size() : 0 )- failedCursors;
        
        //CloseHandler ausführen
        closeFailures = executeHandler(afterCloseHandlers, closeFailures);
        
        int failedHandler = (closeFailures != null ? closeFailures.size() : 0 )- failedCursors-failedCons;
        
        if (closeFailures != null) {
          if( closeFailures.size() == 1 ) {
            throw castToPersistenceLayerExceptionOrThrow(closeFailures.get(0)); 
          } else {
            //Cause anlegen
            StringBuilder message = new StringBuilder();
            message.append("Close failed for ");
            String sep = "";
            if( failedCursors > 0 ) {
              message.append(failedCursors).append(" of ").append(cursors.size()).append(" cursors");
              sep = " and";
            }
            if( failedCons > 0 ) {
              message.append(sep).append(failedCons).append(" of ").append(openConnectionsPerPLI.size()).append(" connections");
              sep = " and";
            }
            if( failedHandler > 0 ) {
              message.append(sep).append(failedHandler).append(" of ").append(afterCloseHandlers.size()).append(" handlers");
            }
            XNWH_GeneralPersistenceLayerException cause = new XNWH_GeneralPersistenceLayerException(message.toString());
            cause.initCauses( listsToArray(closeFailures,null) );
            //Schlimmsten ExceptionTyp ermitteln, daraus Throwable bauen
            Throwable severest = createSeverest( message.toString(), closeFailures );
            //Cause setzen und werfen
            severest.initCause(cause);
            throw castToPersistenceLayerExceptionOrThrow(severest); 
          }
        }
        
      } finally {
        if (cursors != null) {
          cursors.clear();
        }
        usedConnectionsInTransaction.clear();
        openConnectionsPerTable.clear();
        openConnectionsPerPLI.clear();
        afterCloseHandlers = null;
        closed = true;
      }
        
    }

    private List<Throwable> closeCursor(FactoryWarehouseCursor<?> cursor, List<Throwable> failures) {
      try {
        cursor.close();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        if (failures == null) {
          failures = new ArrayList<Throwable>();
        }
        failures.add(e);
      }
      return failures;
    }
    
    private List<Throwable> closeConnection(PersistenceLayerConnection plc, List<Throwable> failures) {
      try {
        plc.closeConnection();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        if (failures == null) {
          failures = new ArrayList<Throwable>();
        }
        failures.add(e);
      }
      return failures;
    }

    private void ensureOpen() throws PersistenceLayerException {
      if (closed) {
        throw new XNWH_ConnectionClosedException();
      }
    }


    public void commit() throws PersistenceLayerException {
      commitOrRollback(true);
    }
    
    public void rollback() throws PersistenceLayerException {
      commitOrRollback(false);
    }
    
    /**
     * 1) es werden alle Connections gesucht, auf denen Änderungen vorgenommen wurden.
     * 2) auf diesen wird das Commit oder Rollback ausgeführt, alle Fehler werden gefangen
     * 3) die Handler werden ausgeführt, alle Fehler werden gefangen
     * 4) alle Fehler werden zu einem Fehler zusammengefasst und geworfen
     * @param commit
     * @throws PersistenceLayerException
     */
    private void commitOrRollback(boolean commit) throws PersistenceLayerException {
      ensureOpen();
      int size = usedConnectionsInTransaction.size();
      if (sqlLogger.isDebugEnabled()) {
        StringBuilder sb = new StringBuilder();
        sb.append(conType).append(commit ? " COMMIT " : " ROLLBACK ").
        append(size).append("/").append(openConnectionsPerPLI.size()).append(" connections");
        if( size > 0 ) {
          sb.append(" for tables ").append(new TreeSet<String>(usedConnectionsInTransaction.keySet()) );
        }
        sqlLogger.debug(sb.toString());
      }
      try {
        commitOrRollback_withHandler(commit, size <= 1 ? usedConnectionsInTransaction
            .values() : new HashSet<PersistenceLayerConnection>(usedConnectionsInTransaction.values()));
      } finally {
        afterRollbackHandlers = null;
        afterCommitHandlers = null;
        afterCommitFailsHandlers = null;
        usedConnectionsInTransaction.clear();
      }
    }
    
    /**
     * 2) auf diesen wird das Commit oder Rollback ausgeführt, alle Fehler werden gefangen
     * 3) die Handler werden ausgeführt, alle Fehler werden gefangen
     * 4) alle Fehler werden zu einem Fehler zusammengefasst und geworfen
     * @param commit
     * @param pls
     * @throws PersistenceLayerException
     */
    private void commitOrRollback_withHandler(boolean commit, Collection<PersistenceLayerConnection> plcs) throws PersistenceLayerException {
      List<Throwable> failuresCR = null;
      List<Throwable> failuresH = null;
      try {
        //Commit/Rollback ausführen, Throwables sammeln
        for( PersistenceLayerConnection plc : plcs) {
          failuresCR = commitOrRollback(commit, plc, failuresCR);
        }
      } finally {
        if( failuresCR == null ) {
          //beim Commit/Rollback ist kein Fehler aufgetreten
          failuresH = executeHandler( commit ? afterCommitHandlers : afterRollbackHandlers, failuresH );
        } else {
          //es ist mindestens ein Fehler aufgetreten
          failuresH = executeHandler( commit ? afterCommitFailsHandlers : null, failuresH );
        }
      }
      
      if (failuresCR != null || failuresH != null) {
        throw buildPersistenceLayerException(commit, plcs.size(), failuresCR, failuresH);
      }
    }

    private List<Throwable> commitOrRollback(boolean commit, PersistenceLayerConnection plc, List<Throwable> failures) {
      try {
        if( commit ) {
          plc.commit();
        } else {
          plc.rollback();
        }
      } catch (Throwable e) {
        Department.handleThrowable(e);
        if (failures == null) {
          failures = new ArrayList<Throwable>();
        }
        failures.add(e);
      }
      return failures;
    }

    private List<Throwable> executeHandler(PriorityQueue<PriorityRunnable> handler, List<Throwable> failures) {
      if (handler != null) {
        PriorityRunnable next;
        while((next = handler.poll()) != null) {
          try {
            next.run();
          } catch (Throwable e) {
            /*
             * FIXME
             * wenn im ack ein fehler passiert, soll der auftrag nicht in den scheduler eingesetllt werden. das passiert aber mit dieser behandlung so 
             */
            Department.handleThrowable(e);
            if (failures == null) {
              failures = new ArrayList<Throwable>();
            }
            failures.add(e);
          }  
        }
      }
      return failures;
    }



    /**
     * Bau einer PersistenceLayerException (PLE) aus den aufgetretenen Fehlern in failuresCR und failuresH.
     * 
     * Bei den Fehlern können verschiedene Exceptiontypen vorliegen, die unterschiedlich schwerwiegend sind:
     * (Error -> RuntimeException/Throwable -> PersistenceLayerException -> XNWH_RetryTransactionException)
     * Geworfen werden soll nun immmer der schwerste. 
     * Es sollen immer alle Fehler geworfen werden, indem diese als Causes in eine XNWH_GeneralPersistenceLayerException
     * verpackt werden. 
     * 
     * 1) ein Fehler bei failuresCR, kein Fehler in failuresH:
     *    Fehler aus failuresCR weiterwerfen
     * 2) ansonsten:
     *    message = "Commit/Rollback failed for x of y connections; z failures in handlers", bzw. Teile davon.
     *    cause   = XNWH_GeneralPersistenceLayerException(message).initCauses(failuresCR,failuresH)
     *    exceptiontyp = schlimmster Exceptiontyp aus failuresCR (bzw. failuresH, wenn failuresCR leer)
     *    dann throw new Exceptiontyp(message,cause);
     * 
     * @param commit Wurde Commit(true) oder Rollback(false) ausgeführt?
     * @param failuresCR Fehler, die beim Commit/Rollback entstanden sind
     * @param failuresH Fehler, die beim Ausführen der Handler entstanden sind
     * @return
     */
    private PersistenceLayerException buildPersistenceLayerException(boolean commit, int connections, List<Throwable> failuresCR, List<Throwable> failuresH) {
      int cntCR = failuresCR == null ? 0 : failuresCR.size();
      int cntH = failuresH == null ? 0 : failuresH.size();
      
      if( cntCR == 1 && cntH == 0 ) {
        //einzigen Fehler unmodifiziert weiterwerfen 
        return castToPersistenceLayerExceptionOrThrow( failuresCR.get(0) );
      } else {
        //Message füllen 
        StringBuilder message = new StringBuilder();
        message.append( commit ? "Commit":"Rollback").append(" failed for ");
        if( cntCR > 0 ) {
          message.append(cntCR).append(" of ").append(connections).append(" connections");
        }
        if( cntCR > 0 && cntH > 0) {
          message.append(" and ");
        }
        if( cntH > 0 ) {
          message.append(cntH).append(" handlers");
        }
        //Cause anlegen
        XNWH_GeneralPersistenceLayerException cause = new XNWH_GeneralPersistenceLayerException(message.toString());
        cause.initCauses( listsToArray(failuresCR,failuresH) );
        //Schlimmsten ExceptionTyp ermitteln, daraus Throwable bauen
        Throwable severest = createSeverest( message.toString(), cntCR > 0 ? failuresCR : failuresH );
        //Cause setzen und werfen
        severest.initCause(cause);
        return castToPersistenceLayerExceptionOrThrow(severest); 
      }
    }


    private Throwable createSeverest(String message, List<Throwable> list) {
      int severity = 0;
      for( Throwable t : list ) {
        if( t instanceof XNWH_RetryTransactionException ) {
          severity = Math.max(severity,1);
        } else if( t instanceof PersistenceLayerException) {
          severity = Math.max(severity,2);
        } else if( t instanceof RuntimeException) {
          severity = Math.max(severity,3);
        } else if( t instanceof Error ) {
          severity = Math.max(severity,4);
        } else {
          severity = Math.max(severity,3); //restliche Throwables wie RuntimeException werten
        }
      }
      switch( severity ) {
        case 1: return new XNWH_RetryTransactionException();
        case 2: return new XNWH_GeneralPersistenceLayerException(message);
        case 3: return new RuntimeException(message);
        case 4: return new Error(message);
        default: //unerwartet
          return new RuntimeException(message);
      }
    }


    private PersistenceLayerException castToPersistenceLayerExceptionOrThrow(Throwable throwable) {
      if (throwable instanceof PersistenceLayerException) {
        return (PersistenceLayerException) throwable;
      } else if (throwable instanceof RuntimeException) {
        throw (RuntimeException) throwable;
      } else if (throwable instanceof Error) {
        throw (Error) throwable;
      } else {
        throw new RuntimeException("unexpected exception", throwable);
      }
    }

    private Throwable[] listsToArray(List<Throwable> listA, List<Throwable> listB) {
      ArrayList<Throwable> ret = new ArrayList<Throwable>();
      if( listA != null ) {
        ret.addAll(listA);
      }
      if( listB != null ) {
        ret.addAll(listB);
      }
      return ret.toArray(new Throwable[ret.size()]);
    }



    private boolean isStorableProtected(Storable storable) {
      return isStorableProtected(storable.getClass());
    }
    
    private boolean isStorableProtected(Class<? extends Storable> clazz) {
      StorableProperty[] properties = Storable.getPersistable(clazz).tableProperties();
      for (StorableProperty prop : properties) {
        if (prop.isProtected()) {
          return true;
        }
      }
      return false;
    }
    
    private boolean isStorableProtected(String tableName) {
      Class<? extends Storable> clazz = getStorableClass(tableName);
      if (clazz == null) {
        throw new RuntimeException("storable " + tableName + " is not registered!");
      }
      return isStorableProtected(clazz);
    }

    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(storable)) {
        sqlLogger.debug(conType + " CONTAINS " + storable.getPrimaryKey() + " in " + storable.getTableName());
      }
      String table = getTable(storable);
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con, c -> c.containsObject(storable));
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      if (cmd == null) {
        throw new NullPointerException("Prepared command must not be null.");
      }
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(cmd.getTable())) {
        sqlLogger.debug(conType + " EXEC DML " + ((ODSPreparedCommand) cmd).getInnerPreparedCommand().toString() + " "
            + paras);
      }
      //herausfinden, um welche tabelle es sich handelt
      String table = cmd.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con,
                                                         c -> c.executeDML(((ODSPreparedCommand) cmd).getInnerPreparedCommand(), paras));
    }


    private PersistenceLayerConnection getConnectionToTable(String table) throws PersistenceLayerException {
      return getConnectionToTable(table, true);
    }


    private PersistenceLayerConnection getConnectionToTable(String table, boolean markAsUsedInCurrentTransaction)
        throws PersistenceLayerException {
      PersistenceLayerConnection openCon = openConnectionsPerTable.get(table);
      if (openCon == null) {
        openCon = getConnectionToPersistenceLayer( getPersistenceLayer(conType, table) );
        //connection wiederverwenden, auch wenn vorher damit auf eine andere tabelle zugegriffen worden ist
        openConnectionsPerTable.put(table, openCon);
      }

      if (markAsUsedInCurrentTransaction) {
        usedConnectionsInTransaction.put(table, openCon);
      }
      return openCon;
    }


    private PersistenceLayerConnection getConnectionToPersistenceLayer(PersistenceLayerInstanceBean pli) throws PersistenceLayerException {
      Long pliId = pli.getPersistenceLayerInstanceID();
      PersistenceLayerConnection openCon = openConnectionsPerPLI.get(pliId);
      if (openCon == null) {
        PersistenceLayer pl = pli.getPersistenceLayerInstance();

        //neue connection holen
        if (isDedicated) {          
          if (sharedConnections != null) {
            throw new RuntimeException("Connection sharing not supported for dedicated connections.");
          }
          openCon = pl.getDedicatedConnection();
        } else {
          if (sharedConnections != null) {
            PersistenceLayerConnection plc = null;
            for (ODSConnectionImpl con : sharedConnections) {
              plc = con.findShareableConnectionTo(pli);
              if (plc != null && plc.isOpen()) {
                break;
              }
            }
            if (plc != null) {
              openCon = pl.getConnection(plc);
              if(openCon == null || !openCon.isOpen()) {
                openCon = pl.getConnection();
              }
            } else {
              openCon = pl.getConnection();
            }
          } else {
            openCon = pl.getConnection();
          }
        }

        if (transactionProperties != null) {
          for (TransactionProperty tp : transactionProperties) {
            openCon.setTransactionProperty(tp);
          }
        }
        openConnectionsPerPLI.put(pliId, openCon);
      }
      return openCon;
    }


    private PersistenceLayerConnection findShareableConnectionTo(PersistenceLayerInstanceBean pli) {
      for (Entry<Long, PersistenceLayerConnection> e : openConnectionsPerPLI.entrySet()) {
        PersistenceLayerInstanceBean usedPli = registeredPersistenceLayerInstances.get(e.getKey());
        if (usedPli.getPersistenceLayerID() == pli.getPersistenceLayerID()) {
          //kann kompatibel sein -> checken
          if (usedPli.getPersistenceLayerInstance().usesSameConnectionPool(pli.getPersistenceLayerInstance())) {
            return e.getValue();
          }
        }
      }
      return null;
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(storable)) {
        sqlLogger.debug(conType + " PERSIST " + storable.getPrimaryKey() + " in " + storable.getTableName());
      }
      String table = getTable(storable);
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con, c -> c.persistObject(storable));
    }


    private void removeReferencesInSharedConnections() {
      if (sharedConnections == null) {
        return;
      }

      for (ODSConnectionImpl sharedConnection : sharedConnections) {
        sharedConnection.removeReferencesToClosedConnections();
      }
    }


    private void removeReferencesToClosedConnections() {

      Set<String> s = new HashSet<String>(openConnectionsPerTable.keySet());
      for (String t : s) {
        PersistenceLayerConnection co = openConnectionsPerTable.get(t);
        if (co == null || !co.isOpen()) {
          openConnectionsPerTable.remove(t);
        }
      }

      Set<Long> openConKeys = new HashSet<Long>(openConnectionsPerPLI.keySet());
      for (Long key : openConKeys) {
        PersistenceLayerConnection co = openConnectionsPerPLI.get(key);
        if (co == null || !co.isOpen()) {
          openConnectionsPerPLI.remove(key);
        }
      }
    }


    private <T extends Storable> String getTable(T storable) {
      return storable.getTableName().toLowerCase();
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(query.getTable())) {
        sqlLogger.debug(conType + " QUERY " + ((ODSPreparedQuery<E>)query).getInnerPreparedQuery().toString() + " " + parameter + " maxRows=" + maxRows);
      }
      String table = query.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con, c -> c.query(((ODSPreparedQuery<E>) query).getInnerPreparedQuery(), parameter,
                                                                           maxRows));
    }

    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(query.getTable())) {
        sqlLogger.debug(conType + " QUERY " + ((ODSPreparedQuery<E>)query).getInnerPreparedQuery().toString() + " " + parameter + " maxRows=" + maxRows + " reader=" + reader.getClass().getSimpleName());
      }
      String table = query.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con, c -> c.query(((ODSPreparedQuery<E>) query).getInnerPreparedQuery(), parameter,
                                                                           maxRows, reader));
    }
    

    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(storable)) {
        sqlLogger.debug(conType + " QUERY ONE ROW " + storable.getPrimaryKey() + " in " + storable.getTableName());
      }
      String table = getTable(storable);
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        con.queryOneRow(storable);
        return (Void) null;
      });
    }


    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(storable)) {
        sqlLogger.debug(conType + " QUERY ONE ROW FOR UPDATE " + storable.getPrimaryKey() + " in " + storable.getTableName());
      }
      String table = getTable(storable);
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        c.queryOneRowForUpdate(storable);
        return (Void) null;
      });
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(query.getTable())) {
        sqlLogger.debug(conType + " QUERY ONE ROW " + ((ODSPreparedQuery<E>)query).getInnerPreparedQuery().toString() + " " + parameter);
      }
      String table = query.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      return executeConnectionCallHandleClosedConnection(con, c -> c.queryOneRow(((ODSPreparedQuery<E>) query).getInnerPreparedQuery(),
                                                                                 parameter));
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      return prepareCommand(cmd, false);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      return prepareQuery(query, false);
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
        throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled()) {
        boolean allowed = true;
        String table = "unknown";
        if (storableCollection.size() > 0) {
          allowed = !isStorableProtected(storableCollection.iterator().next());
          table = getTable(storableCollection.iterator().next());
        }
        if (allowed) {          
          
          StringBuilder sb = new StringBuilder();
          int cnt = 0;
          for (T t: storableCollection) {
            if (cnt++ > 10) {
              sb.append(" ...");
              break;
            }
            sb.append(t.getPrimaryKey()).append(",");
          }
          sqlLogger.debug(conType + " PERSIST COLL in " + table + " size=" + storableCollection.size() + " " + sb.toString());
        }
      }
      if (storableCollection.size() == 0) {
        return;
      }
      String table = getTable(storableCollection.iterator().next());
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        c.persistCollection(storableCollection);
        return (Void) null;
      });
    }


    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      ensureOpen();
      String table = getTableName(klass);
      boolean debug = sqlLogger.isDebugEnabled() && !isStorableProtected(klass);
      if (debug) {
        sqlLogger.debug(conType + " LOAD COLL for " + table);
      }
      PersistenceLayerConnection con = getConnectionToTable(table);
      Collection<T> coll = executeConnectionCallHandleClosedConnection(con, c -> c.loadCollection(klass));
      if (debug) {
        sqlLogger.debug(conType + " LOADED COLL size=" + coll.size());
      }
      return coll;
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      ensureOpen();
      if (sqlLogger.isDebugEnabled()) {
        boolean allowed = true;
        String table = "unknown";
        if (storableCollection.size() > 0) {
          allowed = !isStorableProtected(storableCollection.iterator().next());
          table = getTable(storableCollection.iterator().next());
        }
        if (allowed) {
          StringBuilder sb = new StringBuilder();
          int cnt = 0;
          for (T t: storableCollection) {
            if (cnt++ > 10) {
              sb.append(" ...");
              break;
            }
            sb.append(t.getPrimaryKey()).append(",");
          }
          sqlLogger.debug(conType + " DELETE COLL from " + table + " size=" + storableCollection.size() + " " + sb.toString());
        }
      }
      if (storableCollection == null || storableCollection.size() == 0) {
        return;
      }
      String table = getTable(storableCollection.iterator().next());
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        c.delete(storableCollection);
        return (Void) null;
      });
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      ensureOpen();
      String table = getTableName(klass);
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(klass)) {
        sqlLogger.debug(conType + " DELETE ALL for " + table);
      }
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        c.deleteAll(klass);
        return (Void) null;
      });
    }


    public PreparedCommand prepareCommand(Command cmd, boolean listenToPersistenceLayerChanges)
                    throws PersistenceLayerException {
      ensureOpen();
      boolean debug = sqlLogger.isDebugEnabled() && !isStorableProtected(cmd.getTable());
      if (debug) {
        sqlLogger.debug(conType + " PREPARE " + cmd.getSqlString());
      }
      String table = cmd.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      ODSPreparedCommand odsPC = new ODSPreparedCommand(cmd, con.prepareCommand(cmd));
      if (listenToPersistenceLayerChanges) {
        synchronized (preparedCommands) {
          List<ODSPreparedCommand> l = preparedCommands.get(conType.getIndex()).get(table);
          if (l == null) {
            l = new ArrayList<ODSPreparedCommand>();
            preparedCommands.get(conType.getIndex()).put(table, l);
          }
          l.add(odsPC);
        }
      }
      if (debug) {
        sqlLogger.debug(conType + " PREPARED " + odsPC.getInnerPreparedCommand().toString());
      }
      return odsPC;

    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query, boolean listenToPersistenceLayerChanges)
                    throws PersistenceLayerException {
      ensureOpen();      
      boolean debug = sqlLogger.isDebugEnabled() && !isStorableProtected(query.getTable());
      if (debug) {
        sqlLogger.debug(conType + " PREPARE " + query.getSqlString());
      }
      String table = query.getTable();
      PersistenceLayerConnection con = getConnectionToTable(table);
      ODSPreparedQuery<E> odsPQ = new ODSPreparedQuery<E>(query, con.prepareQuery(query));
      if (listenToPersistenceLayerChanges) {
        synchronized (preparedQuerys) {
          List<ODSPreparedQuery<?>> l = preparedQuerys.get(conType.getIndex()).get(table);
          if (l == null) {
            l = new ArrayList<ODSPreparedQuery<?>>();
            preparedQuerys.get(conType.getIndex()).put(table, l);
          }
          l.add(odsPQ);
        }
      }
      if (debug) {
        sqlLogger.debug(conType + " PREPARED " + odsPQ.getInnerPreparedQuery().toString());
      }
      return odsPQ;

    }

    /**
     * query muss als letzten parameter (also '?' in statement) einen haben, der für den cursor tauglich ist,
     * d.h. zb 'where pk > ?'
     * FIXME validierung, dass das gegeben ist 
     */
    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(final String sqlQuery, final Parameter parameters, final ResultSetReader<T> rsr, final int cacheSize)
        throws PersistenceLayerException {
      return getCursor(sqlQuery, parameters, rsr, cacheSize, null);
    }
    
    public <T extends Storable<?>> FactoryWarehouseCursor<T> getCursor(final String sqlQuery, final Parameter parameters, final ResultSetReader<T> rsr,
                                                                       final int cacheSize, final PreparedQueryCache cache) throws PersistenceLayerException {
      if (sqlLogger.isDebugEnabled() ) {// && !isStorableProtected(query.getTable())) {
        sqlLogger.debug(conType + " GET CURSOR for query " + sqlQuery + " cachesize=" + cacheSize + " " + parameters);
      }
      
      FactoryWarehouseCursor<T> newCursor = new FactoryWarehouseCursor<T>(this, sqlQuery, parameters, rsr, cacheSize, cache);
      if (cursors == null) {
        cursors = new ArrayList<FactoryWarehouseCursor<?>>();
      }
      cursors.add(newCursor);
      return newCursor;
    }


    public ODSConnectionType getConnectionType() {
      return conType;
    }


    public void setTransactionProperty(TransactionProperty property) {
      for (PersistenceLayerConnection con : openConnectionsPerPLI.values()) {
        con.setTransactionProperty(property);
      }
      if (transactionProperties == null) {
        transactionProperties = new HashSet<TransactionProperty>();
      }
      transactionProperties.add(property);
    }


    public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
      ensureOpen();
      if (toBeDeleted == null) {
        return;
      }
      if (sqlLogger.isDebugEnabled() && !isStorableProtected(toBeDeleted)) {
        sqlLogger.debug(conType + " DELETE ONE ROW " + toBeDeleted.getPrimaryKey() + " in " + toBeDeleted.getTableName());
      }
      String table = getTable(toBeDeleted);
      PersistenceLayerConnection con = getConnectionToTable(table);
      executeConnectionCallHandleClosedConnection(con, c -> {
        c.deleteOneRow(toBeDeleted);
        return (Void) null;
      });
    }

    public synchronized void executeAfterCommit(Runnable runnable) {
      executeAfterCommit(runnable, Thread.NORM_PRIORITY);
    }
    
    public synchronized void executeAfterCommitFails(Runnable runnable) {
      executeAfterCommitFails(runnable, Thread.NORM_PRIORITY);
    }
    
    public synchronized void executeAfterRollback(Runnable runnable) {
      executeAfterRollback(runnable, Thread.NORM_PRIORITY);
    }
    
    public synchronized void executeAfterClose(Runnable runnable) {
      executeAfterClose(runnable, Thread.NORM_PRIORITY);
    }
    
    public synchronized void executeAfterCommit(Runnable runnable, int priority) {
      if (afterCommitHandlers == null) {
        afterCommitHandlers = new PriorityQueue<PriorityRunnable>();
      }
      afterCommitHandlers.add(new PriorityRunnable(runnable, priority));
    }
    
    public synchronized void executeAfterCommitFails(Runnable runnable, int priority) {
      if (afterCommitFailsHandlers == null) {
        afterCommitFailsHandlers = new PriorityQueue<PriorityRunnable>();
      }
      afterCommitFailsHandlers.add(new PriorityRunnable(runnable, priority));
    }
    
    public synchronized void executeAfterRollback(Runnable runnable, int priority) {
      if (afterRollbackHandlers == null) {
        afterRollbackHandlers = new PriorityQueue<PriorityRunnable>();
      }
      afterRollbackHandlers.add(new PriorityRunnable(runnable, priority));
    }
    
    public synchronized void executeAfterClose(Runnable runnable, int priority) {
      if (afterCloseHandlers == null) {
        afterCloseHandlers = new PriorityQueue<PriorityRunnable>();
      }
      afterCloseHandlers.add(new PriorityRunnable(runnable, priority));
    }

    public boolean isInTransaction() {
      return usedConnectionsInTransaction != null && ! usedConnectionsInTransaction.isEmpty();
    }

    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz)
                    throws PersistenceLayerException {
      PersistenceLayerInstanceBean pli = getPersistenceLayer(conType, getTableName(storableClazz));
      ensurePersistenceLayerConnectivity(pli, Arrays.<Class<? extends Storable<?>>>asList((Class<? extends Storable<?>>)storableClazz) );
    }
        
    public void ensurePersistenceLayerConnectivity(List<Class<? extends Storable<?>>> storableClazzez)
        throws PersistenceLayerException {
      if (storableClazzez == null || storableClazzez.size() == 0) {
        return; //nichts zu tun
      }
      if( storableClazzez.size() == 1 ) {
        PersistenceLayerInstanceBean pli = getPersistenceLayer(conType, getTableName(storableClazzez.get(0)));
        ensurePersistenceLayerConnectivity(pli, storableClazzez );
        return;
      }
      
      //Sortieren, um DeadLocks zu vermeiden
      SortedMap<PersistenceLayerInstanceBean, List<Class<? extends Storable<?>>>> persistencelayerInstances =
          new TreeMap<PersistenceLayerInstanceBean, List<Class<? extends Storable<?>>>>(COMPARATOR_PLIS);
      for (Class<? extends Storable<?>> c : storableClazzez) {
        PersistenceLayerInstanceBean pli = getPersistenceLayer(conType, getTableName(c));
        List<Class<? extends Storable<?>>> list = persistencelayerInstances.get(pli);
        if (list == null) {
          list = new ArrayList<Class<? extends Storable<?>>>();
          persistencelayerInstances.put(pli, list);
        }
        list.add(c);
      }
      
      //Connection anlegen und Connectivity prüfen
      for (Entry<PersistenceLayerInstanceBean,List<Class<? extends Storable<?>>>> entry : persistencelayerInstances.entrySet()) {
        ensurePersistenceLayerConnectivity( entry.getKey(), entry.getValue() );
      }
    }
    
    /**
     * @param pli
     * @param storables
     * @throws PersistenceLayerException 
     */
    private void ensurePersistenceLayerConnectivity(PersistenceLayerInstanceBean pli, List<Class<? extends Storable<?>>> storables) throws PersistenceLayerException {
      PersistenceLayerConnection plCon = openConnectionsPerPLI.get(pli.getPersistenceLayerInstanceID());
      if( plCon != null ) {
        //Connection liegt bereits vor. Nimmt sie evtl. bereits an Transaktion teil?
        if( usedConnectionsInTransaction.values().contains(plCon) ) {
          //Connection nimmt schon an Transaktion teil, daher keine weitere Überprüfung
          return;
        }
      } else {
        //PersistenceLayerConnection neu anlegen
        plCon = getConnectionToPersistenceLayer(pli);
        if( isInTransaction() ) {
          logger.info("New persistenceLayerConnection for storables "+storables +" in transaction "+usedConnectionsInTransaction.keySet() , new Exception());
        }
      }
      //Nun noch Connectivity prüfen
      for (Class<? extends Storable<?>> c : storables) {
        plCon.ensurePersistenceLayerConnectivity(c);
        openConnectionsPerTable.put(getTableName(c),plCon); 
      }
    }

    private Set<ODSConnectionImpl> sharedConnections;

    private void shareConnectionPoolsInternally(ODSConnection con, boolean recurse) {
      ODSConnectionImpl otherCon = (ODSConnectionImpl) con;
      if (sharedConnections == null) {
        sharedConnections = new HashSet<ODSConnectionImpl>(1);
      }
      sharedConnections.add(otherCon);
      if (recurse) {
        otherCon.shareConnectionPoolsInternally(this, false);
      }
    }
    
    public void shareConnectionPools(ODSConnection con) {
      if (!(con instanceof ODSConnectionImpl)) {
        throw new IllegalArgumentException("unsupported connectiontype: " + con.getClass().getName());
      }
      shareConnectionPoolsInternally(con, true);
    }


    private <T, R extends Exception> T executeConnectionCallHandleClosedConnection(PersistenceLayerConnection con, PersistenceOperation<T, R> call)
        throws PersistenceLayerException, R {
      try {
        return call.exec(con);
      } catch (PersistenceLayerException e) {
        if (!con.isOpen()) {
          removeReferencesToClosedConnections();
          removeReferencesInSharedConnections();
        }
        throw e;
      }
    }

  }



  

  /*
   * listen von prepared querys und commands merken. beim umstellen einer tabelle auf eine andere persistence
   * layer instance müssen die prepared querys und commands auf der neuen pli neu prepared werden.
   * ODSConnectionIndex => tablename => preparedobjects
   */
  private final List<Map<String, List<ODSPreparedQuery<?>>>> preparedQuerys =
      new ArrayList<Map<String, List<ODSPreparedQuery<?>>>>();
  private final List<Map<String, List<ODSPreparedCommand>>> preparedCommands =
      new ArrayList<Map<String, List<ODSPreparedCommand>>>();

  /**
   * id => plbean
   */
  private final Map<Long, PersistenceLayerBeanMemoryCache> registeredPersistenceLayers =
      new HashMap<Long, PersistenceLayerBeanMemoryCache>();
  private final PersistenceLayerInstanceBean[] defaultPersistenceLayer = new PersistenceLayerInstanceBean[ODSConnectionType
      .values().length];

  /**
   * plinstanceID => plinstanceBean
   */
  private final ConcurrentMap<Long, PersistenceLayerInstanceBean> registeredPersistenceLayerInstances =
      new ConcurrentHashMap<Long, PersistenceLayerInstanceBean>();

  /**
   * mapping von tabelle auf persistencelayerinstance. für jeden ODSConnectionType (arraylist) eine map von (Tabelle ->
   * PersistenceLayerInstance)
   */
  private final List<Map<String, PersistenceLayerInstanceBean>> persistenceLayerInstancesMap =
      new ArrayList<Map<String, PersistenceLayerInstanceBean>>(ODSConnectionType.values().length);

  private final List<TableConfiguration> tableConfiguration = new ArrayList<TableConfiguration>();

  /**
   * tableName -> revision -> storable
   */
  private final Map<String, Map<Long, Class<? extends Storable>>> storables = new HashMap<String, Map<Long, Class<? extends Storable>>>();

  private final ConcurrentMap<Long, Set<DatabaseIndexCollision>> indexCollisions =
                  new ConcurrentHashMap<Long, Set<DatabaseIndexCollision>>();
  

  private void init() throws XynaException {
    //achtung, threadpool ist hier noch nicht konfigurierbar, weil xynaproperties ggf nicht initialisiert sind
    //threadpool wird deshalb unten durch futureexecutiontask überschrieben
    FactoryWarehouseCursor.threadPool =
        new ThreadPoolExecutor(15,
                               15,
                               5, TimeUnit.SECONDS,                              
                               new LinkedBlockingQueue<Runnable>(), new XynaThreadFactory(3));
    FactoryWarehouseCursor.threadPool.allowCoreThreadTimeOut(true);
    
    File dir =
        new File(Constants.PERSISTENCE_GEN_CLASSES_CLASSDIR + Constants.fileSeparator
            + Constants.PERSISTENCE_GEN_CLASSES_PACKAGE.replaceAll("\\.", Constants.fileSeparator));
    FileUtils.deleteDirectoryRecursively(dir);

    for (int i = 0; i < ODSConnectionType.values().length; i++) {
      persistenceLayerInstancesMap.add(new ConcurrentHashMap<String, PersistenceLayerInstanceBean>());
      preparedCommands.add(new HashMap<String, List<ODSPreparedCommand>>());
      preparedQuerys.add(new HashMap<String, List<ODSPreparedQuery<?>>>());
    }

    FutureExecution fExecInit = XynaFactory.getInstance().getFutureExecutionForInit();

    fExecInit.execAsync(new XMLPersistenceLayerInitializer(FUTURE_EXECUTION_ID__PREINIT_XML_PERSISTENCE_LAYER));
    fExecInit.execAsync(new PersistenceLayerInstances(FUTURE_EXECUTION_ID__PREINIT_PERSISTENCE_LAYER_INSTANCES));

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(ConnectionPoolStatistics.class, "ConnectionPoolStatistics").
      after(XynaStatistics.FUTUREEXECUTION_ID).
      after(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION). //FIXME
      execAsync( new Runnable() { public void run() { connectionPoolStats = new ConnectionPoolStatistics(); }});
    
    fExec.addTask(ThreadPoolExecutor.class, "Factory Warehouse Cursor Threadpool Init").
      after(Configuration.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() {

          @Override
          public void run() {
            ThreadPoolExecutor tp =
                new ThreadPoolExecutor(XynaProperty.THREADPOOL_WAREHOUSE_CURSOR_THREADS.get(),
                                       XynaProperty.THREADPOOL_WAREHOUSE_CURSOR_THREADS.get(),
                                       XynaProperty.THREADPOOL_WAREHOUSE_CURSOR_KEEP_ALIVE.getMillis(), TimeUnit.MILLISECONDS,
                                       new LinkedBlockingQueue<Runnable>(), new XynaThreadFactory(3));
            tp.allowCoreThreadTimeOut(true);
            ThreadPoolExecutor old = FactoryWarehouseCursor.threadPool;
            FactoryWarehouseCursor.threadPool = tp;
            old.shutdown();
          }

        });
  }


  private class XMLPersistenceLayerInitializer extends FutureExecutionTask {

    public XMLPersistenceLayerInitializer(int id) {
      super(id);
    }


    @Override
    public void execute() {

      try {
        if (readConfigFromXML) {
          initPersistenceLayers();
          // TODO do we want to externalize these as well?
          registerPersistenceLayer(XML_PERSISTENCE_LAYER_ID, defaultPlNames.get(XMLPersistenceLayer.class.getName()), XMLPersistenceLayer.class);
          loadFromXMLAndInstantiatePersistenceLayers(true);
        } else {
          // TODO do we want to externalize these as well?
          registerPersistenceLayer(XML_PERSISTENCE_LAYER_ID, defaultPlNames.get(XMLPersistenceLayer.class.getName()), XMLPersistenceLayer.class);
        }
      } catch (PersistenceLayerException e) {
        // FIXME exception handling??
        throw new RuntimeException(e);
      }

    }


    @Override
    public boolean waitForOtherTasksToRegister() {
      return false;
    }

  }

  
  // called via reflection from UpdatePoolDefinition
  private void reinit() {
    new PersistenceLayerInstances(FUTURE_EXECUTION_ID__PREINIT_PERSISTENCE_LAYER_INSTANCES).execute();
  }

  public class PersistenceLayerInstances extends FutureExecutionTask {

    public PersistenceLayerInstances(int id) {
      super(id);
    }

    @Override
    public void execute() {

      try {
        if (readConfigFromXML) {
          loadFromXMLAndInstantiatePersistenceLayers(false);
        }

        //falls default-persistencelayers nicht gesetzt sind, auf xmlpersistencelayer initialisieren.
        for (int i = 0; i < defaultPersistenceLayer.length; i++) {
          if (defaultPersistenceLayer[i] == null) {
            ODSConnectionType t = ODSConnectionType.values()[i];
            long id;
            try {
              id = instantiatePersistenceLayerInstance(XML_PERSISTENCE_LAYER_ID, "xnwh", t, new String[] {"default" + t.toString()});
            } catch (XNWH_PersistenceLayerIdUnknownException e) {
              // FIXME exception handling??
              throw new RuntimeException(e);
            } catch (XNWH_PersistenceLayerClassIncompatibleException e) {
              // FIXME exception handling??
              throw new RuntimeException(e);
            }
            setDefaultPersistenceLayer(t, id);
          }
        }
        
        preparedQuerys.add(new HashMap<String, List<ODSPreparedQuery<?>>>());
        FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
        
        fExec.addTask( PersistenceLayerInstances.class, "PersistenceLayerInstances").
        execAsync();
        fExec.addTask( ODS.FUTURE_EXECUTION_ID__PERSISTENCE_LAYER_INSTANCES, "PersistenceLayerInstances").
        after(PersistenceLayerInstances.class).
        execAsync();
      
        ODSImpl.preInitialized = true;
      } catch (PersistenceLayerException e) {
        // FIXME exception handling??
        throw new RuntimeException(e);
      }
    }

  }


  private void initPersistenceLayers() throws PersistenceLayerException {

    final List<Long> usedIds = new ArrayList<Long>();

    XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
    PersistenceLayerConnection con = xmlpers.getConnection();
    try {
      Collection<PersistenceLayerBeanStorable> persistenceLayerBeanCollection = con.loadCollection(PersistenceLayerBeanStorable.class);
      if (persistenceLayerBeanCollection != null && persistenceLayerBeanCollection.size() > 0) {
        Iterator<PersistenceLayerBeanStorable> it = persistenceLayerBeanCollection.iterator();
        while (it.hasNext()) {
          PersistenceLayerBeanStorable pli = it.next();
          Class<? extends PersistenceLayer> persistenceLayerClass;
          try {
            PersistenceLayerClassLoader persLayerClassloader =
                new PersistenceLayerClassLoader(pli.getFullyQualifiedClassName());
            persistenceLayerClass =
                (Class<? extends PersistenceLayer>) persLayerClassloader.loadClass(pli.getFullyQualifiedClassName());

          } catch (ClassNotFoundException e) {
            logger.error("Invalid persistence layer registry: " + pli.getFullyQualifiedClassName()
                + " could not be found", e);
            continue;
          } catch (XFMG_JarFolderNotFoundException e) {
            logger.error("Persistence layer " + pli.getFullyQualifiedClassName() + " could not be initialized.", e);
            continue;
          }
          
          boolean mustUpdate = false;
          if (pli.getName() == null) {
            pli.setName(getDefaultPersistenceLayerName(persistenceLayerClass));
            if (logger.isDebugEnabled()) {
              logger.debug("persistenceLayerName not specified -> use default name: " + pli.getName());
            }
            mustUpdate = true;
          }
          
          registerPersistenceLayer(pli.getId(), pli.getName(), persistenceLayerClass);

          if (persistenceLayerClass.getClassLoader() instanceof PersistenceLayerClassLoader) {
            ClassLoaderDispatcherFactory
                .getInstance()
                .getImpl()
                .registerPersistenceLayerClassLoader(pli.getFullyQualifiedClassName(),
                                                     (PersistenceLayerClassLoader) persistenceLayerClass
                                                         .getClassLoader());
          }
          
          if (mustUpdate) {
            con.persistObject(pli);
            con.commit();
          }
          
          usedIds.add(pli.getId());
        }
      } else {
        tryToGeneratePersistenceLayerConfig();
      }
    } finally {
      con.closeConnection();
    }

    // find out the next suitable id
    long maxValue = -1;
    for (int i = 0; i < usedIds.size(); i++) {
      Long usedId = usedIds.get(i);
      if (usedId > maxValue) {
        maxValue = usedId;
      }
    }

    nextSuitablePersistenceLayerId = new AtomicLong(maxValue < 10 ? 10 : maxValue + 1);
  }


  private static String getDefaultPersistenceLayerName(Class<? extends PersistenceLayer> persistenceLayerClass) {
    if (defaultPlNames.containsKey(persistenceLayerClass.getName())) {
      return defaultPlNames.get(persistenceLayerClass.getName());
    }
    return persistenceLayerClass.getSimpleName().toLowerCase().replace("persistencelayer", "");
  }

  private static String getDefaultPersistenceLayerInstanceName(long persistenceLayerInstanceId, String persistenceLayerName) {
    return persistenceLayerName + "_" + persistenceLayerInstanceId;
  }


  public PersistenceLayerInstanceBean[] getPersistenceLayerInstances() {
    return registeredPersistenceLayerInstances.values().toArray(new PersistenceLayerInstanceBean[] {});
  }


  public PersistenceLayerBeanMemoryCache[] getPersistenceLayers() {
    synchronized (registeredPersistenceLayers) {
      return registeredPersistenceLayers.values().toArray(new PersistenceLayerBeanMemoryCache[] {});
    }
  }

  public PersistenceLayerBeanMemoryCache getPersistenceLayer(Long id) {
    synchronized (registeredPersistenceLayers) {
      return registeredPersistenceLayers.get(id);
    }
  }


  private PersistenceLayerInstanceBean getPersistenceLayer(ODSConnectionType conType, String tableName)
      throws XNWH_NoPersistenceLayerConfiguredForTableException {
    PersistenceLayerInstanceBean pli = persistenceLayerInstancesMap.get(conType.getIndex()).get(tableName);
    if (pli == null) {
      pli = defaultPersistenceLayer[conType.getIndex()];
      if (pli == null) {
        throw new XNWH_NoPersistenceLayerConfiguredForTableException(tableName, conType.toString());
      }
    }
    return pli;
  }


  public long instantiatePersistenceLayerInstance(long persistenceLayerID, String department,
                                                  ODSConnectionType connectionType, String[] connectionParameters) throws XNWH_PersistenceLayerIdUnknownException, PersistenceLayerException, XNWH_PersistenceLayerClassIncompatibleException {
    return instantiatePersistenceLayerInstance(null, persistenceLayerID, department, connectionType, connectionParameters);
  }
  
  public long instantiatePersistenceLayerInstance(String pliName, long persistenceLayerID, String department,
                                                  ODSConnectionType connectionType, String[] connectionParameters)
      throws XNWH_PersistenceLayerIdUnknownException, PersistenceLayerException,
      XNWH_PersistenceLayerClassIncompatibleException {

    PersistenceLayerBeanMemoryCache pl = null;
    synchronized (registeredPersistenceLayers) {
      pl = registeredPersistenceLayers.get(persistenceLayerID);
    }
    if (pl == null) {
      throw new XNWH_PersistenceLayerIdUnknownException(persistenceLayerID);
    }
    
    long id;
    PersistenceLayerInstanceBean b;
    while (true) {
      id = 0;
      for (Long l : registeredPersistenceLayerInstances.keySet()) {
        if (l >= id) {
          id = l + 1;
        }
      }
      if (pliName == null) {
        pliName = getDefaultPersistenceLayerInstanceName(id, pl.getPersistenceLayerName());
        if (logger.isDebugEnabled()) {
          logger.debug("persistenceLayerInstanceName not specified -> use default name: " + pliName);
        }
      }
      b = new PersistenceLayerInstanceBean(id, pliName, pl.getPersistenceLayerID(), connectionParameters, department, connectionType);
      for (PersistenceLayerInstanceBean pli : registeredPersistenceLayerInstances.values()) {
        if (pliName.equals(pli.getPersistenceLayerInstanceName())) {
          throw new IllegalArgumentException("PersistenceLayerInstance '" + pliName + "' already exists.");
        }
      }

      if (null == registeredPersistenceLayerInstances.putIfAbsent(id, b)) {
        break;
      }
    }
    
    boolean gotError = true;
    try {
      b.createInstance(pl);
      gotError = false;
    } finally {
      if (gotError) {
        //compensation
        synchronized (registeredPersistenceLayerInstances) {
          registeredPersistenceLayerInstances.remove(id);
        }
      }
    }
    savePLIsToXML();
    return b.getPersistenceLayerInstanceID();
  }



  private void loadFromXMLAndInstantiatePersistenceLayers(boolean xmlLayerInstances) throws PersistenceLayerException {

    logger.debug("loading persistence layer configuration from xml ...");

    XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
    PersistenceLayerConnection con = xmlpers.getConnection();
    try {
      Collection<PersistenceLayerInstanceBean> pliColl = con.loadCollection(PersistenceLayerInstanceBean.class);
      Iterator<PersistenceLayerInstanceBean> it = pliColl.iterator();
  
      Map<Long, Long> notYetRegisteredPersistenceLayerIDsByInstanceID = new HashMap<Long, Long>();
  
      while (it.hasNext()) {
  
        PersistenceLayerInstanceBean pli = it.next();
        boolean xmlPersistenceLayerFlagMatches =
            !(xmlLayerInstances ^ (pli.getPersistenceLayerID() == XML_PERSISTENCE_LAYER_ID || pli.getPersistenceLayerID() == MEMORY_PERSISTENCE_LAYER_ID));
        if (!xmlPersistenceLayerFlagMatches) {
          notYetRegisteredPersistenceLayerIDsByInstanceID.put(pli.getPersistenceLayerInstanceID(),
                                                              pli.getPersistenceLayerID());
          continue;
        }
  
        registeredPersistenceLayerInstances.put(pli.getPersistenceLayerInstanceID(), pli);
  
        PersistenceLayerBeanMemoryCache memoryBean = registeredPersistenceLayers.get(pli.getPersistenceLayerID());
        if (memoryBean == null) {
          throw new RuntimeException("Configuration error: Persistence layer instance (id "
                          + pli.getPersistenceLayerInstanceID() + ") registered for unknown persistence layer ID <"
                          + pli.getPersistenceLayerID() + ">.");
        }
        try {
          pli.createInstance(memoryBean);
        } catch (XNWH_PersistenceLayerClassIncompatibleException e) {
          throw new RuntimeException(e); //hätte vorher schon auffallen müssen
        }
  
        if (pli.getIsDefault()) {
          if (logger.isDebugEnabled()) {
            logger.debug("setting default persistence layer for " + pli.getConnectionType() + " to persistenceLayerId="
                            + pli.getPersistenceLayerID() + " persistenceLayerInstanceId="
                            + pli.getPersistenceLayerInstanceID());
          }
          defaultPersistenceLayer[pli.getConnectionTypeEnum().getIndex()] = pli;
        }
  
        if (pli.getPersistenceLayerInstanceName() == null || pli.getPersistenceLayerInstanceName().length() == 0) {
          pli.setPersistenceLayerInstanceName(getDefaultPersistenceLayerInstanceName(pli.getPersistenceLayerInstanceID(), memoryBean.getPersistenceLayerName()));
          con.persistObject(pli);
          con.commit();
        }
      }
  
      Collection<TableConfiguration> tableColl = con.loadCollection(TableConfiguration.class);
      Iterator<TableConfiguration> itTables = tableColl.iterator();
      while (itTables.hasNext()) {
        TableConfiguration tc = itTables.next();
  
        Long notYetRegisteredID = notYetRegisteredPersistenceLayerIDsByInstanceID.get(tc.getPersistenceLayerInstanceID());
        boolean xmlPersistenceLayerFlagMatches =
            notYetRegisteredID == null || !(xmlLayerInstances ^ notYetRegisteredID == XML_PERSISTENCE_LAYER_ID);
        if (xmlPersistenceLayerFlagMatches) {
          tableConfiguration.add(tc);
          PersistenceLayerInstanceBean instance =
              registeredPersistenceLayerInstances.get(tc.getPersistenceLayerInstanceID());
  
          if (instance == null) {
            logger.debug("Registered persistenceLayerInstances: " + String.join(", ", registeredPersistenceLayerInstances.keySet().stream().map(x -> x == null ? "null" : x.toString()).collect(Collectors.toList())));
            throw new XNWH_PersistenceLayerInstanceIdUnknownForRegisteredTableException(tc.getPersistenceLayerInstanceID(),
                                                                                        tc.getTable());
          }
          persistenceLayerInstancesMap.get(instance.getConnectionTypeEnum().getIndex()).put(tc.getTable().toLowerCase(), instance);
        }
  
      }
    } finally {
      con.closeConnection();
    }

  }


  private void savePLIsToXML() {
    if (readConfigFromXML) {
      try {
        XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
        PersistenceLayerConnection con = xmlpers.getConnection();
        try {
          con.deleteAll(PersistenceLayerInstanceBean.class);
          con.persistCollection(registeredPersistenceLayerInstances.values());
          con.commit();
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        logger.error("could not save persistencelayerinstance configuration state", e);
      }
    }
  }


  private void saveTCsToXML() {
    if (readConfigFromXML) {
      try {
        XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
        PersistenceLayerConnection con = xmlpers.getConnection();
        try {
          synchronized (tableConfiguration) {
            con.deleteAll(TableConfiguration.class);
            con.persistCollection(tableConfiguration);
            con.commit();
          }
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        logger.error("could not save table configurations", e);
      }
    }
  }


  public void setDefaultPersistenceLayer(ODSConnectionType connectionType, long persistenceLayerInstanceID)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {
    setDefaultPersistenceLayer(connectionType, persistenceLayerInstanceID, true);
  }


  private synchronized void setDefaultPersistenceLayer(ODSConnectionType connectionType, long persistenceLayerInstanceID, boolean withRollback)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {

    long oldPLId = Integer.MIN_VALUE;
    PersistenceLayerInstanceBean pl = null;
    pl = registeredPersistenceLayerInstances.get(persistenceLayerInstanceID);
    if (pl == null && persistenceLayerInstanceID != Integer.MIN_VALUE) {
      throw new XNWH_PersistenceLayerInstanceIdUnknownException(persistenceLayerInstanceID);
    }
    if (defaultPersistenceLayer[connectionType.getIndex()] != null) {
      oldPLId = defaultPersistenceLayer[connectionType.getIndex()].getPersistenceLayerInstanceID();
    }
    defaultPersistenceLayer[connectionType.getIndex()] = pl;
    //es darf nur ein PLI pro connection Type default sein
    for (PersistenceLayerInstanceBean pli : registeredPersistenceLayerInstances.values()) {
      if (pli.getConnectionTypeEnum() == connectionType) {
        pli.setDefault(false);
      }
    }
    if (pl == null) {
      return;
    }
    pl.setDefault(true);

    Throwable t = null;
    try {
      //TODO removetable auf altem PLI
      synchronized (storables) {
        //addtable für alle registrierten storables, die nicht auf ein andere PLI gemappt sind (nur für den connectionType)
        for (String tableName : storables.keySet()) {
          Map<String, PersistenceLayerInstanceBean> map = persistenceLayerInstancesMap.get(connectionType.getIndex());
          boolean addedTable = false;
          if (map != null) {
            PersistenceLayerInstanceBean pli = map.get(tableName);
            if (pli != null) {
              addedTable = true;
            }
          }
          if (!addedTable) {
            addTables(tableName, pl.getPersistenceLayerInstance(), null);
          }
        }
      }

      logger.debug("reinitializing prepared queries and commands on tables configured for default persistence layer.");
      //für alle tabellen, die nach default gehen (=storables X connectiontype, für die kein persistencelayerinstance definiert ist)
      //müssen bestehende prepared objekte neu erstellt werden.
      synchronized (storables) {
        synchronized (tableConfiguration) {
          for (String tableName : storables.keySet()) {
            //gibt es konfiguration für diese tabelle?
            boolean found = false;
            for (TableConfiguration tc : tableConfiguration) {
              if (tc.getTable().toLowerCase().equals(tableName)) {
                if (persistenceLayerInstancesMap.get(connectionType.getIndex()).get(tableName) != null) {
                  found = true;
                  break;
                }
              }
            }
            if (!found) {
              //keine konfiguration, also default
              reinitializePreparedObjects(pl, connectionType, tableName);
            }
          }
        }
      }
    } catch (XNWH_PersistenceLayerInstanceIdUnknownException e) {
      t = e;
      throw e;
    } catch (PersistenceLayerException e) {
      t = e;
      throw e;
    } catch (RuntimeException e) {
      t = e;
      throw e;
    } catch (Error e) {
      Department.handleThrowable(e);
      t = e;
      throw e;
    } finally {
      if (t != null && withRollback) {
        //rollback.
        try {
          setDefaultPersistenceLayer(connectionType, oldPLId, false);
        } catch (Throwable e) {
          logger.error("Rollback failed during setDefaultPersistenceLayer. Original exception:", t);
          throw new RuntimeException("Rollback failed during setDefaultPersistenceLayer. Check original exception in logfile", e);
        }
      }
    }

    savePLIsToXML();
  }

  private void removeTables(String tableName, ODSConnectionType connectionType) throws PersistenceLayerException {
    TableConfiguration tc = getTableConfiguration(tableName, connectionType.getIndex());
    Properties props = null;
    if (tc != null) {
      props = tc.getPropertiesMap();
    }
    
    PersistenceLayerConnection con = getPersistenceLayer(connectionType, tableName).getPersistenceLayerInstance().getConnection();
    try {
      for (Class<? extends Storable> storableClass : storables.get(tableName).values()) {
        con.removeTable(storableClass, props);
      }
    } finally {
      con.closeConnection();
    }
  }

  private void addTables(String tableName, PersistenceLayer persistenceLayerInstance, Properties properties) throws PersistenceLayerException {
    // bei neuem default den table adden
    PersistenceLayerConnection con = persistenceLayerInstance.getConnection();
    try {
      for (Class<? extends Storable> storableClass : storables.get(tableName).values()) {
        con.addTable(storableClass, false, properties);
      }
    } finally {
      con.closeConnection();
    }
  }

  private void reinitializePreparedObjects(PersistenceLayerInstanceBean pl, ODSConnectionType conType, String tableName)
      throws PersistenceLayerException {

    if (logger.isDebugEnabled()) {
      logger.debug("reinitializing prepared queries and commands on table " + tableName + " [" + conType.toString()
          + "].");
    }
    synchronized (preparedCommands) {
      synchronized (preparedQuerys) {
        List<ODSPreparedCommand> lCommands = preparedCommands.get(conType.getIndex()).get(tableName);
        if (lCommands != null) {
          for (ODSPreparedCommand pc : lCommands) {
            PersistenceLayerConnection con = pl.getPersistenceLayerInstance().getConnection();
            try {
              PreparedCommand newPC = con.prepareCommand(pc.getCommand());
              pc.setInnerPreparedCommand(newPC);
            } finally {
              con.closeConnection();
            }
          }
        }
        List<ODSPreparedQuery<?>> lQueries = preparedQuerys.get(conType.getIndex()).get(tableName);
        if (lQueries != null) {
          for (ODSPreparedQuery<?> pq : lQueries) {
            PersistenceLayerConnection con = pl.getPersistenceLayerInstance().getConnection();
            try {
              PreparedQuery<?> newPQ = con.prepareQuery(pq.getQuery());
              pq.setInnerPreparedQuery(newPQ);
            } finally {
              con.closeConnection();
            }
          }
        }
      }
    }
  }


  public void setPersistenceLayerForTable(long persistenceLayerInstanceID, String tableName, String properties)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {
    tableName = tableName.toLowerCase();
    //checken, ob die konfiguration sich geändert hat, damit man nicht fälschlicherweise auf dem gleichen PL zweimal addtable sagt
    TableConfiguration tc = null;
    synchronized (tableConfiguration) {
      //alte tableconfig löschen falls vorhanden
      Iterator<TableConfiguration> it = tableConfiguration.iterator();
      while (it.hasNext()) {
        tc = it.next();
        if (tc.getPersistenceLayerInstanceID() == persistenceLayerInstanceID && tc.getTable().toLowerCase().equals(tableName)) {
          if ((tc.getProperties() == null && properties == null) || (tc.getProperties() != null && tc.getProperties().equals(properties))) {
            //gleiche konfig
            return;
          }
        }
      }
    }    

    PersistenceLayerInstanceBean instance = registeredPersistenceLayerInstances.get(persistenceLayerInstanceID);
    if (instance == null) {
      throw new XNWH_PersistenceLayerInstanceIdUnknownException(persistenceLayerInstanceID);
    }
    
    //removetable
    synchronized (storables) {
      if (storables.get(tableName) != null) {
        removeTables(tableName, instance.getConnectionTypeEnum());
      }
    }

    //umkonfigurieren
    persistenceLayerInstancesMap.get(instance.getConnectionTypeEnum().getIndex()).put(tableName, instance);

    //TODO checken, ob es offene connections gibt

    long tableConfigurationID = 0;
    synchronized (tableConfiguration) {
      //alte tableconfig löschen falls vorhanden
      Iterator<TableConfiguration> it = tableConfiguration.iterator();
      while (it.hasNext()) {
        tc = it.next();
        if (tc.getTableConfigurationID() >= tableConfigurationID) {
          tableConfigurationID = tc.getTableConfigurationID() + 1;
        }
        if (tc.getTable().toLowerCase().equals(tableName)) {
          PersistenceLayerInstanceBean instanceOfIteratedTC = registeredPersistenceLayerInstances.get(tc.getPersistenceLayerInstanceID());
          if (instanceOfIteratedTC.getConnectionTypeEnum() == instance.getConnectionTypeEnum()) {
            it.remove();
          }
        }
      }
      tc = new TableConfiguration(tableConfigurationID, tableName, persistenceLayerInstanceID, properties);
      tableConfiguration.add(tc);
    }

    synchronized (storables) {
      if (storables.get(tableName) != null) {
        addTables(tableName, instance.getPersistenceLayerInstance(), tc.getPropertiesMap());
      }
    }

    //prepared objekte für diese tabelle reinitialisieren
    reinitializePreparedObjects(instance, instance.getConnectionTypeEnum(), tableName);

    saveTCsToXML();
  }

  
  public void removeTableFromPersistenceLayer(long persistenceLayerInstanceID, String tableName) 
                  throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {
    tableName = tableName.toLowerCase();
    PersistenceLayerInstanceBean instance = registeredPersistenceLayerInstances.get(persistenceLayerInstanceID);
    if (instance == null) {
      throw new XNWH_PersistenceLayerInstanceIdUnknownException(persistenceLayerInstanceID);
    }
    
    PersistenceLayerInstanceBean defaultInstance = getDefaultPersistenceLayerInstance(instance.getConnectionTypeEnum());
    
    synchronized (storables) {
      if (storables.get(tableName) != null) {
        removeTables(tableName, instance.getConnectionTypeEnum());
      }
    }

    persistenceLayerInstancesMap.get(defaultInstance.getConnectionTypeEnum().getIndex()).put(tableName, defaultInstance);

    //TODO checken, ob es offene connections gibt
    
    TableConfiguration tc = null;
    synchronized (tableConfiguration) {
      //alte tableconfig löschen falls vorhanden
      Iterator<TableConfiguration> it = tableConfiguration.iterator();
      while (it.hasNext()) {
        tc = it.next();
        if (tc.getTable().toLowerCase().equals(tableName)) {
          PersistenceLayerInstanceBean instanceOfIteratedTC = registeredPersistenceLayerInstances.get(tc.getPersistenceLayerInstanceID());
          if (instanceOfIteratedTC.getConnectionTypeEnum() == instance.getConnectionTypeEnum()) {
            it.remove();
          }
        }
      }
    }
    
    synchronized (storables) {
      if (storables.get(tableName) != null) {
        addTables(tableName, defaultInstance.getPersistenceLayerInstance(), null);
      }
    }
    
    reinitializePreparedObjects(defaultInstance, defaultInstance.getConnectionTypeEnum(), tableName);

    saveTCsToXML();
  }

  public void registerStorable(Class<? extends Storable> tableClass) throws PersistenceLayerException {
    registerStorable(tableClass, false);
  }


  public void registerStorableForceWidening(Class<? extends Storable> tableClass) throws PersistenceLayerException {
    registerStorable(tableClass, true);
  }


  private void registerStorable(Class<? extends Storable> tableClass, boolean forceWidening)
       throws PersistenceLayerException {
    //während der durchführung von addTable kann ein weiteres registerStorable aufgerufen werden
    //und die noch nicht passierten (und nicht das aktive) addTable fortführen.
    //beide thread kehren in dieser methode erst zurück, wenn alle addTables
    //durchgeführt worden sind.
    //damit ist z.b. rekursives registerStorable mit ggfs unterschiedlichen threads möglich, wie es
    //in xynacoherenceclusterprovider verwendet wird.
    // project sagt registerTable(lease) -> clusterpersistencelayer sagt xcc.loadData(lease)
    //  -> xcc sagt persistencestrategy.loadData(lease) -> führt in clusterprovider zu
    //     registerTable(lease) um danach die daten aus zweiter odsconnection zu lesen.
    String tableName = Storable.getTableNameLowerCase(tableClass);
    long revision = getRevision(tableClass);
    DistributedWork work;
    synchronized (storables) {     
      Map<Long, Class<? extends Storable>> subMap = storables.get(tableName);
      if (subMap != null) {
        Class<? extends Storable> existingClass = subMap.get(revision);
        if (existingClass == tableClass) {
          return;
        } else if (existingClass != null) {
          unregisterStorable(existingClass);
        }
      }
      //ansonsten überschreiben/einfügen
      
      work = new DistributedWork(ODSConnectionType.values().length);
      DistributedWork oldWork = registeredTables.putIfAbsent(tableName, work);
      if (oldWork != null) {
        work = oldWork;
      }
    }

    addTableToPersistenceLayers(tableName, tableClass, forceWidening, work);

    try {
      work.waitForCompletion(); //darauf warten, dass ggfs andere threads fertig werden.
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    synchronized (storables) {
      Map<Long, Class<? extends Storable>> subMap = storables.get(tableName);
      if (subMap == null) {
        subMap = new HashMap<Long, Class<? extends Storable>>();
        storables.put(tableName, subMap);
      }
      subMap.put(revision, tableClass);
    }

    registeredTables.remove(tableName);
  }


  private long getRevision(Class<? extends Storable> tableClass) {
    ClassLoader cl = tableClass.getClassLoader();
    if (cl instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl;
      return clb.getRevision();
    }
    return RevisionManagement.REVISION_DEFAULT_WORKSPACE;
  }

  //map für die verteilung der register-table aufgaben an mehrere threads
  private ConcurrentMap<String, DistributedWork> registeredTables = new ConcurrentHashMap<String, DistributedWork>();


  /**
   * für alle diesem storable zugeordneten (spezifisch oder default) persistencelayerinstanzen wird addTable()
   * aufgerufen
   * @param work 
   * @throws XNWH_GeneralPersistenceLayerException 
   */
  private void addTableToPersistenceLayers(String tableName, Class<? extends Storable> tableClass,
                                           boolean forceWidening, DistributedWork work) throws XNWH_GeneralPersistenceLayerException
       {

    PersistenceLayerInstanceBean[] plis = getPersistenceLayerInstanceBeansForTable(tableName);
    //FIXME verhindern, dass die persistencelayerconfig geändert wird, während die addTables durchgeführt werden.

    int nextWorkIdx; //connectiontype-idx
    while (-1 != (nextWorkIdx = work.getAndLockNextOpenTaskIdx())) {
      //bit wurde gesetzt, weil ansosnten continue, d.h. jetzt kann dieser thread in ruhe das addTable durchführen
      TableConfiguration tc = getTableConfiguration(tableName, nextWorkIdx);
      Properties props = null;
      if (tc != null) {
        props = tc.getPropertiesMap();
      }
      
      if (plis[nextWorkIdx] == null) {
        //bei tests der fall
        continue;
      }
      try {
        PersistenceLayerConnection con = plis[nextWorkIdx].getPersistenceLayerInstance().getConnection();
        try {
          con.addTable(tableClass, forceWidening, props);
        } finally {
          work.taskDone(); //auch bei einem fehler sollen die threads nicht warten müssen.
          con.closeConnection();
        }
      } catch( PersistenceLayerException e ) {
        throw new XNWH_GeneralPersistenceLayerException("Could not add table \""+tableName+"\", storable \""+tableClass.getName()+"\" to persistencelayers", e);
      }
      
    }

  }


  private TableConfiguration getTableConfiguration(String tableName, int odsConnectionTypeIndex) {
    PersistenceLayerInstanceBean[] plis = getPersistenceLayerInstanceBeansForTable(tableName);

    synchronized (tableConfiguration) {
      for (TableConfiguration tc : tableConfiguration) {
        if (tc.getTable().toLowerCase().equals(tableName)
            && plis[odsConnectionTypeIndex].getPersistenceLayerInstanceID() == tc.getPersistenceLayerInstanceID()) {
          return tc;
        }
      }
    }
    return null;
  }


  private PersistenceLayerInstanceBean[] getPersistenceLayerInstanceBeansForTable(String tableName) {
    PersistenceLayerInstanceBean[] result = new PersistenceLayerInstanceBean[ODSConnectionType.values().length];
    int cnt = 0;
    for (ODSConnectionType ct : ODSConnectionType.values()) {
      Map<String, PersistenceLayerInstanceBean> map = persistenceLayerInstancesMap.get(ct.getIndex());
      boolean addedTable = false;
      if (map != null) {
        PersistenceLayerInstanceBean pli = map.get(tableName);
        if (pli != null) {
          addedTable = true;
          result[cnt++] = pli;
        }
      }
      if (!addedTable) {
        PersistenceLayerInstanceBean b = defaultPersistenceLayer[ct.getIndex()];
        if (b != null) {
          result[cnt++] = b;
        }
      }
    }
    return result;
  }

  
  /**
   * @return true if the storable existed before and could be unregistered and false otherwise
   * @throws PersistenceLayerException 
   */
  public boolean unregisterStorable(Class<? extends Storable> tableClass) throws PersistenceLayerException {
    if (tableClass == null) {
      throw new NullPointerException("Can not unregister null-storable.");
    }
    synchronized (storables) {
      long revision = getRevision(tableClass);
      Persistable persistable = Storable.getPersistable(tableClass); //kann nicht null sein
      String tableNameLC = persistable.tableName().toLowerCase();
      Map<Long, Class<? extends Storable>> submap = storables.get(tableNameLC);
      boolean ret = true;
      if (submap == null) {
        logger.warn("Could not unregister storable " + tableClass + ", it isn't registered.");
        ret = false;
      } else {
        Class<? extends Storable> oldClass = submap.remove(revision);
        if (oldClass == null) {
          logger.warn("Could not unregister storable " + tableClass + ", it isn't registered.");
          ret = false;
        } else if (oldClass != tableClass) {
          logger.warn("Inconsistent storable found while unregistering storable " + tableClass + ". Previously registered in revision "
              + revision + " was different storable " + oldClass + ".");
          ret = false;
        }
        //else ok, ret = true
      }
      PersistenceLayerInstanceBean[] plibs = getPersistenceLayerInstanceBeansForTable(tableNameLC);
      MultipleExceptionHandler<PersistenceLayerException> hpl = new MultipleExceptionHandler<>();
      for (int i = 0; i < plibs.length; i++) {
        PersistenceLayerInstanceBean plib = plibs[i];
        ODSConnectionType conType = ODSConnectionType.values()[i];

        TableConfiguration tc = getTableConfiguration(tableNameLC, conType.getIndex());
        Properties props = null;
        if (tc != null) {
          props = tc.getPropertiesMap();
        }

        try {
          PersistenceLayerConnection con = plib.getPersistenceLayerInstance().getConnection();
          try {
            con.removeTable(tableClass, props);
          } finally {
            con.closeConnection();
          }
        } catch (LinkageError e ) {
          hpl.addError(e);
        } catch (RuntimeException e) {
          hpl.addRuntimeException(e);
        } catch (PersistenceLayerException e) {
          hpl.addException(e);
        }
      }

      Storable.clearCache(tableClass);
      try {
        hpl.rethrow();
      } catch (MultipleExceptions e) {
        throw new XNWH_GeneralPersistenceLayerException("Exception unregistering storable " + persistable.tableName() + " from persistence layers.", e);
      }
      return ret;
    }
  }


  public void registerPersistenceLayer(long persistenceLayerId, Class<? extends PersistenceLayer> persistenceLayerClass) {
    String persistenceLayerName = persistenceLayerClass.getSimpleName().toLowerCase();
    registerPersistenceLayer(persistenceLayerId, persistenceLayerName, persistenceLayerClass);
  }

  public String registerPersistenceLayer(long persistenceLayerId, String persistenceLayerName, Class<? extends PersistenceLayer> persistenceLayerClass) {
    if (persistenceLayerClass == null) {
      throw new IllegalArgumentException("Cannot register persistence layer for class <null>");
    }
    
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache b : registeredPersistenceLayers.values()) {
        if (persistenceLayerName.equals(b.getPersistenceLayerName())) {
          throw new IllegalArgumentException("persistenceLayer '" + persistenceLayerName + "' already exists");
        }
      }
      PersistenceLayerBeanMemoryCache b = new PersistenceLayerBeanMemoryCache(persistenceLayerId, persistenceLayerName, persistenceLayerClass);
      registeredPersistenceLayers.put(persistenceLayerId, b);
    }
    
    return persistenceLayerName;
  }


  public Long getPersistenceLayerId (String persistenceLayerName) throws XNWH_PersistenceLayerNotRegisteredException {
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache b : registeredPersistenceLayers.values()) {
        if (persistenceLayerName.equals(b.getPersistenceLayerName())) {
          return b.getPersistenceLayerID();
        }
      }
    }
    throw new XNWH_PersistenceLayerNotRegisteredException(persistenceLayerName);
  }


  public Long getPersistenceLayerInstanceId(String persistenceLayerInstanceName) throws XNWH_PersistenceLayerInstanceNotRegisteredException {
    for (PersistenceLayerInstanceBean pli : registeredPersistenceLayerInstances.values()) {
      if (persistenceLayerInstanceName.equals(pli.getPersistenceLayerInstanceName())) {
        return pli.getPersistenceLayerInstanceID();
      }
    }

    throw new XNWH_PersistenceLayerInstanceNotRegisteredException(persistenceLayerInstanceName);
  }


  public String getPersistenceLayerInstanceName(Long persistenceLayerInstanceId) throws XNWH_PersistenceLayerInstanceNotRegisteredException {
    if (registeredPersistenceLayerInstances.containsKey(persistenceLayerInstanceId)) {
      return registeredPersistenceLayerInstances.get(persistenceLayerInstanceId).getPersistenceLayerInstanceName();
    } else {
      throw new XNWH_PersistenceLayerInstanceNotRegisteredException("id=" + persistenceLayerInstanceId);
    }
  }
  
  
  public PersistenceLayerBeanMemoryCache unregisterPersistenceLayer(long persistenceLayerId)
                  throws XNWH_PersistenceLayerMayNotBeUndeployedInUseException {

    for (PersistenceLayerInstanceBean instanceBean : getPersistenceLayerInstances()) {
      if (instanceBean.getPersistenceLayerID() == persistenceLayerId) {
        throw new XNWH_PersistenceLayerMayNotBeUndeployedInUseException(persistenceLayerId,
                                                                        instanceBean.getPersistenceLayerInstanceID());
      }
    }
    synchronized (registeredPersistenceLayers) {
      // FIXME what about active instances of this persistence layer?
      return registeredPersistenceLayers.remove(persistenceLayerId);
    }

  }


  public TableConfiguration[] getTableConfigurations() {
    List<TableConfiguration> tempList = new ArrayList<TableConfiguration>();
    synchronized (storables) {
      synchronized (tableConfiguration) {
        for (String tableName : storables.keySet()) {
          boolean found = false;
          for (TableConfiguration tc : tableConfiguration) {
            if (tc.getTable().toLowerCase().equals(tableName)) {
              found = true;
              break;
            }
          }
          if (!found) {
            TableConfiguration notConfigured = new TableConfiguration(-1, tableName, -1, null);          
            tempList.add(notConfigured);
          }
        }
        tempList.addAll(tableConfiguration);
        return tempList.toArray(new TableConfiguration[] {});
      }
    }
  }


  public PersistenceLayerInstanceBean getDefaultPersistenceLayerInstance(ODSConnectionType connectionType) {
    return defaultPersistenceLayer[connectionType.getIndex()];
  }


  public boolean isTableRegistered(ODSConnectionType connectionType, String tableName) {
    synchronized (tableConfiguration) {
      for (TableConfiguration tc : tableConfiguration) {
        if (tc.getTable().toLowerCase().equals(tableName)) {
          PersistenceLayerInstanceBean bean = registeredPersistenceLayerInstances.get(tc
                          .getPersistenceLayerInstanceID());
          if (bean == null) {
            throw new RuntimeException("Configuration problem: Table is registered for persistence layer instance that does not exist (id "
                                                       + tc.getPersistenceLayerInstanceID() + ").");
          }
          ODSConnectionType existingType = bean.getConnectionTypeEnum();
          if (existingType == connectionType) {
            return true;
          }
        }
      }
    }
    return false;
  }


  public long getJavaPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException {
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache persLayerBean : registeredPersistenceLayers.values()) {
        if (persLayerBean.getPersistenceLayerClass().getName().equals(JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
          return persLayerBean.getPersistenceLayerID();
        }
      }
      throw new XNWH_PersistenceLayerNotRegisteredException(JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }
  }


  public long getMemoryPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException {
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache persLayerBean : registeredPersistenceLayers.values()) {
        if (persLayerBean.getPersistenceLayerClass().getName().equals(MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
          return persLayerBean.getPersistenceLayerID();
        }
      }
      throw new XNWH_PersistenceLayerNotRegisteredException(MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }
  }


  public long getDevNullPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException {
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache persLayerBean : registeredPersistenceLayers.values()) {
        if (persLayerBean.getPersistenceLayerClass().getName().equals(XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
          return persLayerBean.getPersistenceLayerID();
        }
      }
      throw new XNWH_PersistenceLayerNotRegisteredException(XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }
  }


  public long getXmlPersistenceLayerID() throws XNWH_PersistenceLayerNotRegisteredException {
    synchronized (registeredPersistenceLayers) {
      for (PersistenceLayerBeanMemoryCache persLayerBean : registeredPersistenceLayers.values()) {
        if (persLayerBean.getPersistenceLayerClass().getName().equals(XYNA_XML_PERSISTENCE_LAYER_FQ_CLASSNAME)) {
          return persLayerBean.getPersistenceLayerID();
        }
      }
      throw new XNWH_PersistenceLayerNotRegisteredException(XYNA_XML_PERSISTENCE_LAYER_FQ_CLASSNAME);
    }
  }


  public void deployPersistenceLayer(String name, String fqClassName) throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND,
      XFMG_JarFolderNotFoundException, PersistenceLayerException {

    PersistenceLayerClassLoader persLayerClassloader = new PersistenceLayerClassLoader(fqClassName);

    Class<? extends PersistenceLayer> persistenceLayerClass;
    try {
      persistenceLayerClass = (Class<? extends PersistenceLayer>) persLayerClassloader.loadClass(fqClassName);
    } catch (ClassNotFoundException e) {
      StringBuffer urls = new StringBuffer("");
      for (URL url : persLayerClassloader.getURLs()) {
        if (urls.toString().length() != 0) {
          urls.append(", ");
        }
        urls.append(url.getFile());
      }
      throw new XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND(fqClassName, urls.toString(), e);
    }

    if (name == null || name.length() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("persistenceLayerName not specified -> use default name: " + name);
      }
      name = getDefaultPersistenceLayerName(persistenceLayerClass);
    }
    
    PersistenceLayerBeanStorable bean =
        new PersistenceLayerBeanStorable(nextSuitablePersistenceLayerId.getAndIncrement(), name, fqClassName);
    registerPersistenceLayer(bean.getId(), bean.getName(), persistenceLayerClass);

    // Bei classloaderdispatcher registrieren, damit
    //    * Im SerializableClassLoadedObject beim Deserialisieren PL-Classloader aufgelöst werden können
    //    * Man mit listclassloaderinfos über die cli infos über die classloaders bekommen kann
    // Aufpassen: auch wieder deregistrieren, wenn persistencelayer undeployed wird.
    ClassLoaderDispatcherFactory.getInstance().getImpl()
        .registerPersistenceLayerClassLoader(fqClassName, persLayerClassloader);

    XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
    PersistenceLayerConnection con = xmlpers.getConnection();
    try {
      con.persistObject(bean);
      con.commit();
    } finally {
      con.closeConnection();
    }

  }


  private static XMLPersistenceLayer createXMLPLForPersistenceStorage() throws PersistenceLayerException {
    XMLPersistenceLayer xmlpers = new XMLPersistenceLayer();
    xmlpers.init(null, Constants.PERSISTENCE_CONFIGURATION_DIR_WITHIN_STORAGE,  TransactionMode.FULL_TRANSACTION.name(), "false");
    return xmlpers;
  }

  public void undeployPersistenceLayer(String fqClassName) throws PersistenceLayerException,
      XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException {

    long relevantPersistenceLayerId = -1;

    // TODO to implement this we have to iterate over the existing entries by ourselves since the
    // xml persistence layer does not support queries. once that support is added, this can be done a lot simpler
    XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
    PersistenceLayerConnection con = xmlpers.getConnection();
    try {
      Collection<PersistenceLayerBeanStorable> persistenceLayers =
          con.loadCollection(PersistenceLayerBeanStorable.class);
      PersistenceLayerBeanStorable toBeRemoved = null;
      if (persistenceLayers != null && persistenceLayers.size() > 0) {
        for (PersistenceLayerBeanStorable bean : persistenceLayers) {
          if (bean.getFullyQualifiedClassName().equals(fqClassName)) {
            toBeRemoved = bean;
            relevantPersistenceLayerId = bean.getId();
            break;
          }
        }
      }
      if (relevantPersistenceLayerId != -1 && toBeRemoved != null) {
        for (PersistenceLayerInstanceBean instanceBean : getPersistenceLayerInstances()) {
          if (instanceBean.getPersistenceLayerID() == relevantPersistenceLayerId) {
            throw new XNWH_PersistenceLayerMayNotBeUndeployedInUseException(relevantPersistenceLayerId,
                                                                            instanceBean
                                                                                .getPersistenceLayerInstanceID());
          }
        }
        con.delete(Arrays.asList(new PersistenceLayerBeanStorable[] {toBeRemoved}));
        con.commit();
      }
    } finally {
      con.closeConnection();
    }

    if (relevantPersistenceLayerId == -1) {
      PersistenceLayerBeanMemoryCache[] actualPersLayers = getPersistenceLayers();
      for (int i = 0; i < actualPersLayers.length; i++) {
        if (actualPersLayers[i].getPersistenceLayerClass().getName().equals(fqClassName)) {
          relevantPersistenceLayerId = actualPersLayers[i].getPersistenceLayerID();
          break;
        }
      }
    }

    if (relevantPersistenceLayerId == -1) {
      throw new XNWH_PersistenceLayerNotRegisteredException(fqClassName);
    }

    PersistenceLayerBeanMemoryCache existingMemoryEntry = unregisterPersistenceLayer(relevantPersistenceLayerId);
    if (existingMemoryEntry != null) {
      // remove the classloader
      ClassLoaderDispatcherFactory.getInstance().getImpl()
          .unregisterPersistenceLayerClassLoader(existingMemoryEntry.getPersistenceLayerClass().getName());
    } else {
      throw new XNWH_PersistenceLayerNotRegisteredException(fqClassName);
    }

  }


  // TODO this should be part of some appropriate update mechanism or something that is executed
  // everytime the server recognizes that it misses important parts
  private static void tryToGeneratePersistenceLayerConfig() throws PersistenceLayerException {
    Set<Long> usedPersistenceLayerIds = new HashSet<Long>();

    XMLPersistenceLayer xmlpers = createXMLPLForPersistenceStorage();
    Connection readingConnection = xmlpers.getConnection();
    try {
      Collection<PersistenceLayerInstanceBean> instances = readingConnection
                      .loadCollection(PersistenceLayerInstanceBean.class);
      if (instances != null && instances.size() > 0) {
        logger.debug("Found " + instances.size() + " persistence layer instances found");
        for (PersistenceLayerInstanceBean bean : instances) {
          if (!usedPersistenceLayerIds.contains(bean.getPersistenceLayerID())) {
            usedPersistenceLayerIds.add(bean.getPersistenceLayerID());
          }
        }
      } else {
        logger.debug("No persistence layer instances found, nothing to do");
        return;
      }
    } finally {
      readingConnection.closeConnection();
    }

    ArrayList<PersistenceLayerBeanStorable> defaultPersistenceLayers = new ArrayList<PersistenceLayerBeanStorable>();

    for (long idAsLong : usedPersistenceLayerIds) {
      if (idAsLong > Integer.MAX_VALUE) {
        throw new RuntimeException("This update can only be applied for persistence layer instances < "
                        + Integer.MAX_VALUE);
      }
      int id = (int) idAsLong;
      switch (id) {
        case 0 :
          defaultPersistenceLayers.add(new PersistenceLayerBeanStorable(0L, defaultPlNames.get(MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME), ODSImpl.MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;
        case 1 :
          defaultPersistenceLayers.add(new PersistenceLayerBeanStorable(1L, defaultPlNames.get(JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME), ODSImpl.JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;
        case 2 :
          defaultPersistenceLayers.add(new PersistenceLayerBeanStorable(2L, defaultPlNames.get(MYSQL_PERSISTENCE_LAYER_FQ_CLASSNAME), ODSImpl.MYSQL_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;
        case 3 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(XML_PERSISTENCE_LAYER_ID, XMLPersistenceLayer.class.getName()));
          break;
        case 4 :
          defaultPersistenceLayers.add(new PersistenceLayerBeanStorable(4L, defaultPlNames.get(XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME), XYNA_DEVNULL_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;
        case 5 :
          defaultPersistenceLayers
                          .add(new PersistenceLayerBeanStorable(5L, defaultPlNames.get(XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME), ODSImpl.XYNA_XMLSHELL_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;
        case 6 :
          defaultPersistenceLayers.add(new PersistenceLayerBeanStorable(6L, defaultPlNames.get(ORACLE_PERSISTENCE_LAYER_FQ_CLASSNAME), ODSImpl.ORACLE_PERSISTENCE_LAYER_FQ_CLASSNAME));
          break;

        default :
          throw new IllegalStateException("Found persistence layer with unknown ID");
      }
    }

    Connection writingConnection = xmlpers.getConnection();
    try {
      writingConnection.persistCollection(defaultPersistenceLayers);
      writingConnection.commit();
    } finally {
      writingConnection.closeConnection();
    }

  }


  private <STR extends Storable> void copyInternally(Class<STR> storableClazz, ODSConnectionType source,
                                                     ODSConnectionType target, boolean deleteTarget)
      throws XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {

    if (source == target) {
      logger.debug("Attempt to copy table <" + getTableName(storableClazz) + "> but source and"
          + " target persistence layer instances are the same (" + source + ")");
      return;
    }

    // load everything from the source persistence layer
    Collection<STR> toBeCopied = null;
    ODSConnectionImpl connection = openConnection(source);
    try {
      toBeCopied = connection.loadCollection(storableClazz);
    } finally {
      connection.closeConnection();
    }

    if (toBeCopied != null) {
      connection = openConnection(target);
      try {
        if(deleteTarget) {
          connection.deleteAll(storableClazz);
        }
        connection.persistCollection(toBeCopied);
        connection.commit();
      } finally {
        connection.closeConnection();
      }
    }

  }


  public <T extends Storable<?>> long getPersistenceLayerInstanceId(ODSConnectionType connectionType, Class<T> targetStorable) {
    String tableName = Storable.getTableNameLowerCase(targetStorable);
    return getPersistenceLayerInstanceId(connectionType, tableName);
  }
    
  public <T extends Storable<?>> long getPersistenceLayerInstanceId(ODSConnectionType connectionType, String tableName) {
    if (logger.isDebugEnabled()) {
      logger.debug("Trying to get persistence layer id for table " + tableName + " (connection type '" + connectionType + "')");
    }
    HashMap<Long, ODSConnectionType> connectionTypeBePersistenceLayerId = new HashMap<Long, ODSConnectionType>();
    for (PersistenceLayerInstanceBean bean : getPersistenceLayerInstances()) {
      connectionTypeBePersistenceLayerId.put(bean.getPersistenceLayerInstanceID(), bean.getConnectionTypeEnum());
    }
    TableConfiguration[] tableConfigs = getTableConfigurations();
    for (int j = 0; j < tableConfigs.length; j++) {
      boolean tableNameMatches = tableConfigs[j].getTable().toLowerCase().equals(tableName);
      boolean connectionTypeMatches =
          connectionTypeBePersistenceLayerId.get(tableConfigs[j].getPersistenceLayerInstanceID()) == connectionType;
      if (tableNameMatches && connectionTypeMatches) {
        return tableConfigs[j].getPersistenceLayerInstanceID();
      }
    }

    // nothing found, return default
    return defaultPersistenceLayer[connectionType.getIndex()].getPersistenceLayerInstanceID();
  }


  public <U extends Storable> Class<U> getStorableByTableName(String tableName) {
    return (Class<U>) getStorableClass(tableName);
  }


  public boolean isSamePhysicalTable(String tableName, ODSConnectionType type1, ODSConnectionType type2)
      throws XNWH_NoPersistenceLayerConfiguredForTableException {
    if (type1 == type2) {
      throw new IllegalArgumentException("types must be different");
    }
    return getPersistenceLayer(type1, tableName).getPersistenceLayerInstance()
        .describesSamePhysicalTables(getPersistenceLayer(type2, tableName).getPersistenceLayerInstance());
  }


  public void removePersistenceLayerInstance(long persistenceLayerInstanceId) throws PersistenceLayerException {

    PersistenceLayerInstanceBean pli = registeredPersistenceLayerInstances.get(persistenceLayerInstanceId);
    if (pli == null) {
      throw new XNWH_PersistenceLayerInstanceIdUnknownException(persistenceLayerInstanceId);
    }
    for (int i = 0; i < ODSConnectionType.values().length; i++) {
      Map<String, PersistenceLayerInstanceBean> map = persistenceLayerInstancesMap.get(i);
      if (map.values().contains(pli)) {
        throw new XNWH_PersistenceLayerInstanceMayNotBeDeletedInUseException(persistenceLayerInstanceId);
      }
    }
    if (pli.getIsDefault()) {
      throw new XNWH_PersistenceLayerInstanceMayNotBeDeletedInUseException(persistenceLayerInstanceId);
    }
    pli.getPersistenceLayerInstance().shutdown();
    //TODO check, ob es noch offene connections gibt.
    registeredPersistenceLayerInstances.remove(persistenceLayerInstanceId);

    savePLIsToXML();

  }


  public <T extends Storable<?>> ClusterProvider getClusterInstance(ODSConnectionType conType, Class<T> storableClass) {
    String tableName = storableClass.getAnnotation(Persistable.class).tableName();
    PersistenceLayerInstanceBean pli;
    try {
      pli = getPersistenceLayer(conType, tableName);
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e1) {
      return null;
    }
    PersistenceLayer pl = pli.getPersistenceLayerInstance();
    if (pl instanceof Clustered) {
      Clustered cl = (Clustered) pl;
      if (cl.isClustered()) {
        try {
          return XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
              .getClusterInstance(cl.getClusterInstanceId());
        } catch (XFMG_UnknownClusterInstanceIDException e) {
          throw new RuntimeException("internal configuration error", e);
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }


  public void clearPreparedQueryCache() {
    for (Map<String, List<ODSPreparedQuery<?>>> m : preparedQuerys) {
      m.clear();
    }
  }


  public <T extends Storable<?>> long getClusterInstanceId(ODSConnectionType conType, Class<T> storableClass) {
    String tableName = storableClass.getAnnotation(Persistable.class).tableName();
    PersistenceLayerInstanceBean pli;
    try {
      pli = getPersistenceLayer(conType, tableName);
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e1) {
      return XynaClusteringServicesManagement.DEFAULT_CLUSTER_INSTANCE_ID_NOT_CONFIGURED;
    }
    PersistenceLayer pl = pli.getPersistenceLayerInstance();
    if (pl instanceof Clustered) {
      Clustered cl = (Clustered) pl;
      if (cl.isClustered()) {
        return cl.getClusterInstanceId();
      } else {
        return XynaClusteringServicesManagement.DEFAULT_CLUSTER_INSTANCE_ID_NOT_CONFIGURED;
      }
    } else {
      return XynaClusteringServicesManagement.DEFAULT_CLUSTER_INSTANCE_ID_NOT_CONFIGURED;
    }
  }


  /*
   * von OraclePersistenceLayer gerufen!
   */
  public void changeClustering(PersistenceLayer pl, boolean newStateIsClustered, long newClusterInstanceId) {

    long persistenceLayerInstanceID = -1;
    for (Entry<Long, PersistenceLayerInstanceBean> e : registeredPersistenceLayerInstances.entrySet()) {
      if (pl == e.getValue().getPersistenceLayerInstance()) {
        persistenceLayerInstanceID = e.getValue().getPersistenceLayerInstanceID();
        break;
      }
    }

    int conTypeIndex = 0;
    outer: for (Map<String, PersistenceLayerInstanceBean> m: persistenceLayerInstancesMap) {
      for (Entry<String, PersistenceLayerInstanceBean> e: m.entrySet()) {
        if (e.getValue().getPersistenceLayerInstance() == pl) {
          persistenceLayerInstanceID = e.getValue().getPersistenceLayerInstanceID();
          break outer;
        }
      }
      conTypeIndex++;
    }

    if (persistenceLayerInstanceID == -1) {
      throw new RuntimeException("Persistence layer instance bean could not be found");
    }

    List<String> relevantTableNames = null;
    synchronized (tableConfiguration) {
      for (TableConfiguration tc : tableConfiguration) {
        if (tc.getPersistenceLayerInstanceID() == persistenceLayerInstanceID) {
          if (relevantTableNames == null) {
            relevantTableNames = new ArrayList<String>();
          }
          relevantTableNames.add(tc.getTable());
        }
      }
    }

    if (relevantTableNames != null) {
      ODSConnectionType conType = ODSConnectionType.getByIndex(conTypeIndex);
      Lock readLock = clusteredStorableChangeHandlerLock.readLock();
      readLock.lock();
      try {
        Map<String, List<ClusteredStorableConfigChangeHandler>> conTypeMap =
            clusteredStorableChangeHandlers.get(conType);
        if (conTypeMap == null || conTypeMap.isEmpty()) {
          return;
        }
        for (String tableName : relevantTableNames) {
          List<ClusteredStorableConfigChangeHandler> handlers = conTypeMap.get(tableName);
          if (handlers != null) {
            for (ClusteredStorableConfigChangeHandler handler : handlers) {
              if (newStateIsClustered) {
                handler.enableClustering(newClusterInstanceId);
              } else {
                handler.disableClustering();
              }
            }
          }
        }
      } finally {
        readLock.unlock();
      }
    }

  }


  public static interface ClusteredStorableConfigChangeHandler {
    public void enableClustering(long clusterInstanceId);
    public void disableClustering();
  }


  private ReadWriteLock clusteredStorableChangeHandlerLock = new ReentrantReadWriteLock();


  public void addClusteredStorableConfigChangeHandler(ClusteredStorableConfigChangeHandler handler,
                                                      ODSConnectionType conType, Class<? extends Storable> storableClass) {

    Lock writeLock = clusteredStorableChangeHandlerLock.writeLock();
    writeLock.lock();
    try {
      Map<String, List<ClusteredStorableConfigChangeHandler>> conTypeMap = clusteredStorableChangeHandlers.get(conType);
      if (conTypeMap == null) {
        conTypeMap = new HashMap<String, List<ClusteredStorableConfigChangeHandler>>();
        clusteredStorableChangeHandlers.put(conType, conTypeMap);
      }

      String tableName = storableClass.getAnnotation(Persistable.class).tableName();
      List<ClusteredStorableConfigChangeHandler> handlersList = conTypeMap.get(tableName);
      if (handlersList == null) {
        handlersList = new ArrayList<ClusteredStorableConfigChangeHandler>();
        conTypeMap.put(tableName, handlersList);
      }

      handlersList.add(handler);
    } finally {
      writeLock.unlock();
    }

  }


  private Class<? extends Storable> getStorableClass(String tableName) {
    return getStorableClass(tableName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, true); 
  }
  
  private Class<? extends Storable> getStorableClass(String tableName, long revision, boolean useDifferentRevisionIfNotAvailable) {
    Class<? extends Storable> clazz;
    synchronized (storables) {
      Map<Long, Class<? extends Storable>> revMap = storables.get(tableName);
      if (revMap != null && revMap.size() > 0) {
        clazz = revMap.get(revision);
        if (clazz == null && useDifferentRevisionIfNotAvailable) {
          clazz = revMap.values().iterator().next();
        }
      } else {
        clazz = null;
      } 
    }
    return clazz;
  }
  
  
  private Map<ODSConnectionType, Map<String, List<ClusteredStorableConfigChangeHandler>>> clusteredStorableChangeHandlers =
      new HashMap<ODSConnectionType, Map<String, List<ClusteredStorableConfigChangeHandler>>>();

  
  private static String getTableName(Class<?> klass) throws XNWH_MissingAnnotationsException  {
    //if we call ensure that often we might be better of caching this on a classBasis
    Persistable persi = AnnotationHelper.getPersistable(klass);
    if (persi == null) {
      throw new XNWH_MissingAnnotationsException(klass.getName());
    }
    return persi.tableName().toLowerCase();
  }

  
  public <STR extends Storable> void copy(Class<STR> storableClazz, ODSConnectionType sourceConnectionType,
                                          ODSConnectionType targetConnectionType) throws PersistenceLayerException {
    copyInternally(storableClazz, sourceConnectionType, targetConnectionType, false);
  }

  
  public <STR extends Storable> void replace(Class<STR> storableClazz, ODSConnectionType sourceConnectionType,
                                             ODSConnectionType targetConnectionType) throws PersistenceLayerException {
    copyInternally(storableClazz, sourceConnectionType, targetConnectionType, true);
  }

  
  public <STR extends Storable> void delete(Class<STR> storableClazz, List<ODSConnectionType> targetConnectionTypes)
                  throws PersistenceLayerException {
    for (ODSConnectionType odsConnectionType : targetConnectionTypes) {
      ODSConnectionImpl connection = openConnection(odsConnectionType);
      try {
        connection.deleteAll(storableClazz);
        connection.commit();
      } finally {
        connection.closeConnection();
      }
    }
  }
  
  
  public void addIndexCollisions(Long plInstanceId, Set<DatabaseIndexCollision> collisions) {
    Set<DatabaseIndexCollision> forAdd = new HashSet<DatabaseIndexCollision>(collisions);
    Set<DatabaseIndexCollision> previousCollisions = indexCollisions.putIfAbsent(plInstanceId, forAdd);
    if (previousCollisions != null) {
      boolean success = false;
      do {
        previousCollisions = indexCollisions.get(plInstanceId);
        forAdd.addAll(previousCollisions);
        success = indexCollisions.replace(plInstanceId, previousCollisions, forAdd);
      } while (!success);
    }
  }


  public Map<Long, Set<DatabaseIndexCollision>> getIndexCollisions() {
    Map<Long, Set<DatabaseIndexCollision>> copiedCollisions = new HashMap<Long, Set<DatabaseIndexCollision>>();
    for (Entry<Long, Set<DatabaseIndexCollision>> entry : indexCollisions.entrySet()) {
      copiedCollisions.put(entry.getKey(), new HashSet<DatabaseIndexCollision>(entry.getValue()));
    }
    return copiedCollisions;
  }


  public void resolveIndexCollisions(Long plInstanceId, Set<DatabaseIndexCollision> collisions) throws PersistenceLayerException {
    PersistenceLayerInstanceBean plib = registeredPersistenceLayerInstances.get(plInstanceId);
    PersistenceLayerConnection con = plib.getPersistenceLayerInstance().getConnection();
    try {
      if (con instanceof DatabasePersistenceLayerConnectionWithAlterTableSupport) {
        DatabasePersistenceLayerConnectionWithAlterTableSupport dplcwats = (DatabasePersistenceLayerConnectionWithAlterTableSupport) con;
        dplcwats.alterColumns(collisions);
        boolean success = false;
        while (!success) {
          Set<DatabaseIndexCollision> oldCollisions = indexCollisions.get(plInstanceId);
          Set<DatabaseIndexCollision> newCollisions = new HashSet<DatabaseIndexCollision>(oldCollisions);
          newCollisions.removeAll(collisions);
          for (DatabaseIndexCollision collision : collisions) {
            newCollisions.addAll(dplcwats.checkColumns(collision.getPersi(), collision.getKlass(), new Column[] {collision.getColumn()}));
          }
          success = indexCollisions.replace(plInstanceId, oldCollisions, newCollisions);
        }
      } else {
        throw new RuntimeException(con.getClass().getName() + " does not support alter table.");
      }
    } finally {
      con.closeConnection();
    }
  }

  

  private interface PersistenceOperation<T, R extends Exception> {

    T exec(PersistenceLayerConnection con) throws PersistenceLayerException, R;
  }

}
