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

package com.gip.xyna.xprc.xprcods.orderarchive;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SubstringMap;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_ABORTED_EXCEPTION;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution.EmptyResponseListener;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher.CallStatsType;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher.DestinationChangedHandler;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;


public class OrderArchive extends FunctionGroup
    implements
      OrderAPI,
      OrderArchiveAPI,
      OrderBackupAPI,
      OrderDBAPI {

  public static final int FUTURE_EXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();


  public enum ProcessStepHandlerType {
    PREHANDLER, POSTHANDLER, ERRORHANDLER, PRECOMPENSATION, POSTCOMPENSATION;
  }
  
  public enum SearchMode {
    FLAT, CHILDREN, HIERARCHY
  }

  public enum SuspensionCause {

    MANUAL(true, "Manual"),
    SHUTDOWN(true, "Shutdown"),
    FORCED(true, "Forced"),
    WAIT_FEATURE(false, "Wait Feature"),
    SUSPEND_FEATURE(true, "Suspension Feature"),
    MANUAL_INTERACTION(true, "Manual Interaction", true),
    AWAITING_SYNCHRONIZATION(false, "Awaiting Synchronization");

    private boolean freesCapacities;
    private String suspensionCauseString;
    private boolean callsStatusChangeListenersManually;


    private SuspensionCause(boolean freesCapacities, String suspensionCauseString) {
      this(freesCapacities, suspensionCauseString, false);
    }


    private SuspensionCause(boolean freesCapacities, String suspensionCauseString,
                            boolean callsStatusChangeListenersManually) {
      this.freesCapacities = freesCapacities;
      this.suspensionCauseString = suspensionCauseString;
    }


    public boolean freesCapacities() {
      return freesCapacities;
    }


    public String getSuspensionCauseString() {
      return suspensionCauseString;
    }


    public boolean callsStatusChangeListenersManually() {
      return callsStatusChangeListenersManually;
    }

  }

  
  /**
   * index ist der index von {@link BackupCause}
   */
  private boolean[] hasHadProblemsDuringArchive = new boolean[OrderInstanceBackup.getMaxIndexOfArchivingProblems()];


  /**
   * liest entries aus orderbackup.alternative und versucht sie zu archivieren. falls es wieder nicht funktioniert,
   * werden weitere eintr�ge mit dem gleichen problemfall (zb kein zugriff auf orderarchive.history m�glich) nicht
   * weiter versucht zu reparieren. (es wird aber sichergestellt, dass der n�chste normale archivierungsversuch wieder
   * die durchf�hrung des algorithms triggert.) erfolgreich archivierte eintr�ge werden aus orderbackup.alternative
   * gel�scht.
   */
  private static class RetryAlgorithm implements Algorithm {

    private OrderArchive oa;
    private ODS ods;


    public RetryAlgorithm(OrderArchive oa) {
      this.oa = oa;
    }


    private void execInternally() throws PersistenceLayerException {
      if (ods == null) {
        ods = oa.ods;
        if (ods == null) {
          oa.logger.warn("ods not set properly in orderarchive.");
          return;
        }
      }
      Collection<OrderInstanceBackup> oibs;
      ODSConnection conAlternative = ods.openConnection(ODSConnectionType.ALTERNATIVE);
      try {
        //TODO als cursor umbauen (funktioniert dann nicht mehr mit xml layer)
        oibs = conAlternative.loadCollection(OrderInstanceBackup.class);
      } finally {
        conAlternative.closeConnection();
      }

      //archivierung erneut probieren
      List<OrderInstanceBackup> successfullyArchived = new ArrayList<OrderInstanceBackup>();
      List<OrderInstanceBackup> listChangedBackupCause = new ArrayList<OrderInstanceBackup>();
      boolean[] hasHadProblemsDuringArchiveRetry = new boolean[OrderInstanceBackup.getMaxIndexOfArchivingProblems()];
      ODSConnection conDefault = ods.openConnection();
      try {
        ODSConnection conHistory = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          for (OrderInstanceBackup oib : oibs) {
            if (hasHadProblemsDuringArchiveRetry[oib.getBackupCauseAsEnum().getIndexArchivingProblem()]) {
              break;
            }
            long orderId = oib.getId();
            OrderInstanceDetails oid = oib.getDetails();
            boolean success = false;
            boolean changedBackupCause = false;
            boolean historyPersistenceSuccessful = true;
            switch (oib.getBackupCauseAsEnum()) {
              //nur breaks im case, wenn abgebrochen werden muss, ansonsten macht archivierung alle schritte nacheinander,
              //ab dem, wo es schiefgegangen war. z.B. falls BackupCause = PROBLEM_DEFAULT:
              //erst default l�schen, dann history schreiben, dann backup l�schen.
              case ARCHIVING_PROBLEM_DEFAULT : {
                try {
                  oid = oa.auditAccess.restore(conDefault, orderId, false);
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                  //hier ist monitoringlevel>0. dieser fall sollte also nur passieren, wenn das insert schon schiefgegangen ist.
                } catch (PersistenceLayerException e) {
                  oa.logger.warn("could not access orderarchive default.", e);
                  break; //unsuccessful retry, switch verlassen
                }

                try {
                  oid.convertAuditDataToXML(oib.getRevision(), true);
                } catch (XPRC_CREATE_MONITOR_STEP_XML_ERROR e) {
                  //trotzdem archivieren
                  oa.logger.warn("could not create xml auditdata", e);
                }
                oid.clearAuditDataJavaObjects();
                try {
                  if (!ods.isSamePhysicalTable(oid.getTableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
                    //nur l�schen, wenn default orderarchive != history orderarchive.
                    //ansonsten w�re es zwar erstmal nicht schlimm, hier zu l�schen, weil hinterher
                    //geaddet wird. aber weil bis dahin noch kein commit stattgefunden hat,
                    //ist die zeile gelockt. 
                    try {
                      oa.auditAccess.delete(conDefault, new OrderInstanceDetails(orderId));
                      conDefault.commit();
                    } catch (PersistenceLayerException e) {
                      oa.logger.warn("could not delete entry from orderarchive (" + ODSConnectionType.DEFAULT.name() + ") for id = " + orderId + ".", e);
                      break; //unsuccessfuly retry. switch verlassen
                    }
                  }
                } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
                  //zumindest ein default persistencelayer sollte ja immer vorhanden sein. ansonsten ist da was stark fehlgeschlagen.
                  throw new RuntimeException(e);
                }

                //wenn bis hierhin alles gut gegangen ist, kann mit history/backup schritten fortgesetzt werden => n�chster switch schritt
              }
              case ARCHIVING_PROBLEM_HISTORY : {
              }
              case ARCHIVING_PROBLEM_HISTORY_AND_BACKUP : {
                historyPersistenceSuccessful = false;
                try {
                  // in dem zeitintervall bis zu dem commit des l�schens ist der gel�schte auftrag ggfs
                  // doppelt sichtbar. in search-orders wird das aber abgefangen.
                  oa.auditAccess.store(conHistory, oid);
                  conHistory.commit();
                  historyPersistenceSuccessful = true;
                } catch (PersistenceLayerException e) {
                  oa.logger.warn("order " + orderId + " could not be written to archive (history).", e);
                }
                if (oib.getBackupCauseAsEnum() == BackupCause.ARCHIVING_PROBLEM_HISTORY) {
                  if (historyPersistenceSuccessful) {
                    success = true;
                  }
                  break; //backup ist bereits gel�scht => switch beenden
                }
                //else backup l�schen muss noch passieren => zum n�chsten case weiter
              }
              case ARCHIVING_PROBLEM_ORDERBACKUP : {
                try {
                  oa.deleteFromBackupInternally(orderId, conDefault);
                  conDefault.commit();
                  if (!historyPersistenceSuccessful) {
                    if (oib.getBackupCauseAsEnum() != BackupCause.ARCHIVING_PROBLEM_HISTORY) {
                      changedBackupCause = true;
                      oib.setBackupCause(BackupCause.ARCHIVING_PROBLEM_HISTORY);
                    }
                  } else {
                    success = true;
                  }
                } catch (PersistenceLayerException e) {
                  oa.logger.warn("could not delete entry from orderbackup for id = " + orderId + ".", e);
                  if (historyPersistenceSuccessful) {
                    if (oib.getBackupCauseAsEnum() != BackupCause.ARCHIVING_PROBLEM_ORDERBACKUP) {
                      changedBackupCause = true;
                      oib.setBackupCause(BackupCause.ARCHIVING_PROBLEM_ORDERBACKUP);
                    }
                  } else {
                    if (oib.getBackupCauseAsEnum() != BackupCause.ARCHIVING_PROBLEM_HISTORY_AND_BACKUP) {
                      changedBackupCause = true;
                      oib.setBackupCause(BackupCause.ARCHIVING_PROBLEM_HISTORY_AND_BACKUP);
                    }
                  }
                }
                break;
              }
              default : //ntbd
            }

            if (success) {
              successfullyArchived.add(oib);
            } else {
              if (changedBackupCause) {
                listChangedBackupCause.add(oib);
              }
              //nicht in diesem durchlauf nochmal gleiche problemf�lle probieren
              hasHadProblemsDuringArchiveRetry[oib.getBackupCauseAsEnum().getIndexArchivingProblem()] = true;
              //es soll aber beim n�chsten mal, wo der problemfall funktioniert, ein token eingestellt werden
              oa.hasHadProblemsDuringArchive[oib.getBackupCauseAsEnum().getIndexArchivingProblem()] = true;
            }

          }
        } finally {
          conHistory.closeConnection();
        }
      } finally {
        conDefault.closeConnection();
      }

      //erfolgreich archiviert => aus orderbackup.ALTERNATIVE entfernen
      //backup_cause updaten, falls notwendig (zb vorher ging default nicht, jetzt geht history nicht)
      if (successfullyArchived.size() > 0) {
        conAlternative = ods.openConnection(ODSConnectionType.ALTERNATIVE);
        try {
          conAlternative.delete(successfullyArchived);
          conAlternative.persistCollection(listChangedBackupCause);
          conAlternative.commit();
        } finally {
          conAlternative.closeConnection();
        }
      }
    }


    public void exec() {
      oa.logger.debug("beginning retry of unsuccessful archives");
      try {
        execInternally();
      } catch (XynaException e) {
        oa.logger.error("error during retry of unsuccessful archives.", e);
      } catch (Throwable e) {
        Department.handleThrowable(e);
        oa.logger.error("error during retry of unsuccessful archives.", e);
      }
      oa.logger.debug("finished retry of unsuccessful archives");
    }

  }


  private RetryAlgorithm archiveRetryAlgorithm = new RetryAlgorithm(this);

  private LazyAlgorithmExecutor<Algorithm> archiveRetryExecutor =
      new LazyAlgorithmExecutor<Algorithm>("ArchiveRetryExecutor");

  public static final String DEFAULT_NAME = "Orders Archive";
  public static final String ORDER_BACKUP_NAME = "Order Backup";

  // this keeps track of root response listeners while the corresponding orders are suspended
  private ConcurrentHashMap<Long, TransientOrderPart> transientOrderParts =
      new ConcurrentHashMap<Long, TransientOrderPart>();

  private static boolean doPersistence = true;

  protected static PreparedQueryCache cache = new PreparedQueryCache();
  
  protected AuditStorageAccess auditAccess = AuditStorageAccess.dedicatedBaseFieldTable;

  protected ODS ods;

  private PreparedCommand deleteOldArchived;
  /**
   * inkl suspendierter auftr�ge
   */
  private PreparedQuery<OrderInstanceBackup> getAllBackupsInRange;
  static PreparedQuery<OrderInstance> getOrderInstanceByIdFromDefault;
  static PreparedQuery<OrderInstance> getOrderInstanceByIdFromHistory;
  static PreparedQuery<OrderInstance> getAllChildOrdersFromDefault;
  static PreparedQuery<OrderInstance> getAllChildOrdersFromHistory;
  static PreparedQuery<OrderInstance> getOrderInstancesByRootIdFromDefault;
  static PreparedQuery<OrderInstance> getOrderInstancesByRootIdFromHistory;
  static PreparedQuery<Long> getRootOrderIdsFromHistory;

  private PreparedCommand updateBackupCauseCmd;


  protected RemoteInterface searchAlgorithm;

  private Integer ownBinding;


  public OrderArchive() throws XynaException {
    super();
    logger = CentralFactoryLogging.getLogger(OrderArchive.class);
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  static void setDoPersistence(boolean b) {
    doPersistence = b;
  }


  public void init() throws XynaException {
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(FUTURE_EXECUTION_ID, "OrderArchive.init").
      after(PersistenceLayerInstances.class).
      before(XynaClusteringServicesManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync(new Runnable() { public void run() { initInternally2(); }});

    if (orderTypeSubstringMapEnabled.get()) {
      fExec.addTask("OrderArchive.updateordertypes", "OrderArchive.updateordertypes").
        after(XynaDispatcher.class).
        execAsync(new Runnable() {

            @Override
            public void run() {
              ExecutionDispatcher ed =
                  XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
              Map<DestinationKey, DestinationValue> destinations = ed.getDestinations();
              for (DestinationKey dk : destinations.keySet()) {
                getOrCreateOrderTypeMap().map.add(dk.getOrderType());
              }
              ed.registerCallbackHandler(new DestinationChangedHandler() {

                public void set(DestinationKey dk, DestinationValue dv) {
                  getOrCreateOrderTypeMap().map.add(dk.getOrderType());
                }


                public void remove(DestinationKey dk) {
                  //nicht entfernen, weil auftr�ge k�nnten ja im orderarchive sein
                }
              });
            }


          });
    }
  }

  public void initInternally2() {

    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    try {
      ods.registerStorable(OrderInstanceDetails.class);
      ods.registerStorable(OrderInstanceBackup.class);
      ods.registerStorable(OrderArchiveStatisticsStorable.class);
      ods.registerStorable(OrderInfoStorable.class);
    } catch (PersistenceLayerException e1) {
      throw new RuntimeException("Failed to register storable", e1);
    }

    ODSConnection defaultCon = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      searchAlgorithm = instantiateSearchAlgorithm();

      getAllBackupsInRange = OrderArchiveQueryHelper.getGetAllBackupsInRangeQuery(defaultCon);

      getAllChildOrdersFromDefault = OrderArchiveQueryHelper.getAllChildOrdersByParentId(this, defaultCon);
      getOrderInstanceByIdFromDefault = OrderArchiveQueryHelper.getOrderInstanceByIdWithoutExceptions(this, defaultCon);

      getOrderInstancesByRootIdFromDefault = OrderArchiveQueryHelper.getGetOrderInstanceByRootIdQuery(this, defaultCon);
      getRootOrderIdsFromHistory = OrderArchiveQueryHelper.getGetRootOrderIdsQuery(defaultCon);
      
      try {
        updateBackupCauseCmd = OrderArchiveQueryHelper.getUpdateOrderInstanceBackupCause(defaultCon);
      } catch (PersistenceLayerException e) {
        logger.warn("No update statements possible for table <" + OrderInstanceBackup.TABLE_NAME
            + ">, this may result in performance issues depending on the usecase.");
        if (logger.isTraceEnabled()) {
          logger.trace(null, e);
        }
      }

    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to perform " + getClass().getSimpleName() + " initialization", e);
    } finally {
      try {
        defaultCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection: " + e.getMessage(), e);
      }
    }
    defaultCon = null;

    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection historyCon) throws PersistenceLayerException {
        deleteOldArchived = OrderArchiveQueryHelper.getDeleteOldArchived(OrderArchive.this, historyCon);
        getAllChildOrdersFromHistory = OrderArchiveQueryHelper.getAllChildOrdersByParentId(OrderArchive.this, historyCon);
        getOrderInstanceByIdFromHistory = OrderArchiveQueryHelper.getOrderInstanceByIdWithoutExceptions(OrderArchive.this, historyCon);
        getOrderInstancesByRootIdFromHistory = OrderArchiveQueryHelper.getGetOrderInstanceByRootIdQuery(OrderArchive.this, historyCon);
      }
    };

    try {
      WarehouseRetryExecutor.executeWithRetriesNoException(wre, ODSConnectionType.HISTORY,
                                                           Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                           new StorableClassList(auditAccess.getAllClasses()));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to access <" + OrderInstance.TABLE_NAME + ">", e);
    }
    
    updateOrderTypesFromDatabase();
    
    XynaProperty.XYNA_BACKUP_READ_RETRIES.registerDependency(ORDER_BACKUP_NAME);
    XynaProperty.XYNA_BACKUP_STORE_RETRIES.registerDependency(ORDER_BACKUP_NAME);
    XynaProperty.XYNA_BACKUP_READ_RETRY_WAIT.registerDependency(ORDER_BACKUP_NAME);
    XynaProperty.XYNA_BACKUP_STORE_RETRY_WAIT.registerDependency(ORDER_BACKUP_NAME);
    XynaProperty.ORDER_INSTANCE_BACKUP_STORE_AUDITXML_BINARY.registerDependency(ORDER_BACKUP_NAME);
    XynaProperty.CLUSTERING_TIMEOUT_ORDER_MIGRATION.registerDependency(ORDER_BACKUP_NAME);
    
    archiveRetryExecutor.startNewThread(archiveRetryAlgorithm);
    archiveRetryExecutor.requestExecution();

  }


  public int getOwnBinding() {
    if (ownBinding == null) {
      ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    }
    return ownBinding;
  }


  protected void setOwnBinding(Integer newbinding) {
    ownBinding = newbinding;
  }


  public void shutdown() throws XynaException {
    if (ods == null) {
      return; //nicht initialisiert
    }

    archiveRetryExecutor.stopThread();
    ods = null;
  }


  public void backupOrderInstanceDetailsOnShutdown() throws PersistenceLayerException {
    if (doPersistence) {
      WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

        
        public void executeAndCommit(ODSConnection defaultConnection) throws PersistenceLayerException {
          @SuppressWarnings("unchecked")
          FactoryWarehouseCursor<OrderInstanceDetails> cursor =
              (FactoryWarehouseCursor<OrderInstanceDetails>) defaultConnection
                  .getCursor(OrderArchiveQueryHelper.allActiveInstancesSQL, new Parameter(),
                             new OrderInstanceDetails().getReader(), 100, cache);
          List<OrderInstanceDetails> nextCache = cursor.getRemainingCacheOrNextIfEmpty();

          while (nextCache != null && nextCache.size() > 0) {

            long minId = nextCache.get(0).getId() - 1;
            long maxId = nextCache.get(nextCache.size() - 1).getId() + 1;

            //FIXME performance ist so nicht gut, und es werden ggfs viel zu viele orderbackup entries geladen!
            //      besser w�re es hier nur die orderbackups zu laden, die passend zu den orderinstancedetails sind.
            
            Collection<OrderInstanceBackup> relevantBackups =
                defaultConnection.query(getAllBackupsInRange, new Parameter(maxId, minId, getOwnBinding()), -1,
                                        OrderInstanceBackup.getReaderWarnIfNotDeserializable());
            // use a sorted map just in case a different factory is shutting down at the same time (which actually
            // should not lead to a concurrent execution of this code!)
            SortedMap<Long, OrderInstanceBackup> relevantBackupsMap = new TreeMap<Long, OrderInstanceBackup>();

            Set<Long> idsToBeSkipped = null;
            for (OrderInstanceBackup nextBackup : relevantBackups) {
              if ((nextBackup.getId() == nextBackup.getRootId() && nextBackup.getXynaorder() == null)
                  || nextBackup.getBackupCauseAsEnum() == BackupCause.SUSPENSION) {
                // order could not be deserialized => skip that entry to not overwrite the order with <null>. This
                // may lose some audit information, though.
                // suspendierte auftr�ge k�nnen auch �bersprungen werden, weil dort die orderinstancedetails bereits up to date sind 
                if (idsToBeSkipped == null) {
                  idsToBeSkipped = new HashSet<Long>();
                }
                idsToBeSkipped.add(nextBackup.getId());
                continue;
              }
              relevantBackupsMap.put(nextBackup.getId(), nextBackup);
            }

            for (OrderInstanceDetails nextDetails : nextCache) {
              OrderInstanceBackup possiblyExistingBackup = relevantBackupsMap.get(nextDetails.getId());

              if (possiblyExistingBackup != null) {
                possiblyExistingBackup.setDetails(nextDetails);
              } else {
                if (idsToBeSkipped == null || !idsToBeSkipped.contains(nextDetails.getId())) {
                  relevantBackupsMap.put(nextDetails.getId(), new OrderInstanceBackup(null, nextDetails,
                                                                                      BackupCause.SHUTDOWN,
                                                                                      getOwnBinding()));
                }
              }
            }

            defaultConnection.persistCollection(relevantBackupsMap.values());
            defaultConnection.commit();
            nextCache = cursor.getRemainingCacheOrNextIfEmpty();
          }

          auditAccess.deleteAll(defaultConnection);
          defaultConnection.commit();
        }
      };

      WarehouseRetryExecutor.executeWithRetriesNoException(wre, ODSConnectionType.DEFAULT,
                                                           Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                           new StorableClassList(OrderInstanceBackup.class));
    }
  }


  private void checkDBState() {
    if (ods == null) {
      throw new IllegalStateException("OrderArchive is not in a state where it can access the underlying ODS.");
    }
  }

  /**
   * gibt false zur�ck, falls das monitoringlevel der methode h�her als das der xynaorder ist, d.h. dass die
   * archive-operation wegen zu niedrigem monitoringlevel nicht ausgef�hrt werden soll. ansonsten true.
   */
  private boolean checkArgAndCheckStateAndCheckMonitoringLevel(XynaOrderServerExtension order,
                                                               int monitoringLevelOfOperation) {
    if (order == null) {
      throw new IllegalArgumentException("Null not allowed for " + XynaOrder.class.getSimpleName());
    }
    checkDBState();
    int monitoringLevel = MonitoringCodes.getMonitoringLevelForRuntime(order);
    if (monitoringLevel < monitoringLevelOfOperation) {
      return false;
    } else {
      return true;
    }
  }


  public void insert(XynaOrderServerExtension order) throws PersistenceLayerException {

    if (order == null) {
      throw new IllegalArgumentException("Cannot insert order 'null'");
    }

    Integer code = order.getMonitoringCode();
    if (code != null && code.compareTo(MonitoringCodes.NO_MONITORING) >= 0) {
      // TODO statspath in destinationkey cachen
      StatisticsPath statsPath = XynaDispatcher.getSpecificCallStatsAttributePath(order.getDestinationKey().getOrderType(),
                                                                                  order.getDestinationKey().getApplicationName(),
                                                                                  CallStatsType.STARTED);
      try {
        PushStatistics<Long, LongStatisticsValue> stats = (PushStatistics<Long, LongStatisticsValue>) getFactoryRuntimeStatistics().getStatistic(statsPath);
        if (stats != null) {
          stats.pushValue(new LongStatisticsValue(1L));
        }
      } catch (XFMG_InvalidStatisticsPath e) { 
        //ntbd
      }
    }

    

    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order,OrderInstanceStatus.INITIALIZATION.getMinimumMonitoringLevel())) {
      return;
    }
    OrderInstanceDetails oi = new OrderInstanceDetails(order);

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      boolean existedBefore = auditAccess.store(con, oi);
      if (existedBefore) {
        // the inconsistency can be detected this way but the old data is overwritten, if the persistence layer
        // does not support transactions
        throw new RuntimeException("Duplicate order detected in <" + OrderInstance.TABLE_NAME + "> with id <"
            + order.getId() + ">");
      }
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public void updateAuditDataAddSubworkflowId(XynaOrderServerExtension order, ProcessStep pstep, long subworkflowId)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order, MonitoringCodes.STEP_MONITORING)) {
      return;
    }
    // Synchronization necessary to prevent simultaneous execution of updateAuditData &
    // backup which lead to ConcurrentModificationExceptions on a HashMap in the OrderInstanceDetails
    synchronized (order) {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        OrderInstanceDetails oi = auditAccess.restore(con, order.getId(), true);
        oi.updateAuditDataAddSubworkflowId(pstep, subworkflowId);

        oi.setLastUpdate(System.currentTimeMillis());

        auditAccess.store(con, oi);
        con.commit();
      } finally {
        con.closeConnection();
      }
    }
  }

  public void updateAuditData(XynaOrderServerExtension order, ProcessStep pstep, ProcessStepHandlerType auditDataType)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order, MonitoringCodes.STEP_MONITORING)) {
      return;
    }
    // Synchronization necessary to prevent simultaneous execution of updateAuditData &
    // backup which lead to ConcurrentModificationExceptions on a HashMap in the OrderInstanceDetails
    synchronized (order) {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        OrderInstanceDetails oi = auditAccess.restore(con, order.getId(), true);
        switch (auditDataType) {
          //TODO wieso wird hier immer die revision �bergeben? die muss doch nicht jedes mal gesetzt werden!?
          case PREHANDLER :
            oi.setAuditDataPreStep(order.getExecutionType(), pstep, order.getRevision());
            break;
          case POSTHANDLER :
            oi.setAuditDataPostStep(order.getExecutionType(), pstep, order.getRevision());
            break;
          case ERRORHANDLER :
            oi.setAuditDataError(order.getExecutionType(), pstep, order.getRevision());
            break;
          case PRECOMPENSATION :
            oi.setAuditDataPreComp(order.getExecutionType(), pstep, order.getRevision());
            break;
          case POSTCOMPENSATION :
            oi.setAuditDataPostComp(order.getExecutionType(), pstep, order.getRevision());
            break;
          default :
            throw new RuntimeException("Unexpected step handler type");
        }

        oi.setLastUpdate(System.currentTimeMillis());

        // custom fields may have changed due to service execution
        oi.setCustom0(order.getCustom0());
        oi.setCustom1(order.getCustom1());
        oi.setCustom2(order.getCustom2());
        oi.setCustom3(order.getCustom3());

        auditAccess.store(con, oi);
        con.commit();
      } finally {
        con.closeConnection();
      }
    }
  }


  public void updateStatus(final XynaOrderServerExtension order, final OrderInstanceStatus status, ODSConnection externalConnection)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (order == null) {
      throw new IllegalArgumentException("Cannot update status for order 'null'");
    }

    if (logger.isTraceEnabled()) {
      logger.trace(new StringBuilder(order.toString()).append(" is updating to status ").append(status).toString());
    }
 
    final boolean statusFinished = status == OrderInstanceStatus.FINISHED;
    
    if (statusFinished) {
      if (order.getDestinationKey().getOrderType() != null) {

        // TODO statspath in destinationkey cachen
        StatisticsPath statsPath = XynaDispatcher.getSpecificCallStatsAttributePath(order.getDestinationKey().getOrderType(),
                                                                                    order.getDestinationKey().getApplicationName(),
                                                                                    CallStatsType.FINISHED);
        try {
          PushStatistics<Long, LongStatisticsValue> stats = (PushStatistics<Long, LongStatisticsValue>) getFactoryRuntimeStatistics().getStatistic(statsPath);
          if (stats != null) {
            stats.pushValue(new LongStatisticsValue(1L));
          }
        } catch (XFMG_InvalidStatisticsPath e) {
          // ntbd
        }
      }
    }

    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order,  status.getMinimumMonitoringLevel() ) ) {
      if (logger.isTraceEnabled()) {
        logger.trace("No need to update orderarchive for order <" + order.getId() + ">");
      }
      return;
    }

    final boolean requiresCommit = (externalConnection == null);

    WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> wre =
        new WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY>() {

          public void executeAndCommit(ODSConnection localConnection) throws PersistenceLayerException,
              XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
            OrderInstanceDetails oi;
            // aus threadsicherheit hier ein "select for update", welches die zeile sperrt, bis sie upgedatet ist. Das ist
            // nur erforderlich f�r die �berg�nge zu Execution und Finished, weil nur in diesen F�llen mehr als triviale Felder
            // gesetzt werden.
            boolean statusRunningExecution = OrderInstanceStatus.RUNNING_EXECUTION == status;
            boolean statusScheduling = OrderInstanceStatus.SCHEDULING == status;
            try {
              oi = auditAccess.restore(localConnection, order.getId(), (statusRunningExecution || statusFinished));
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              // in case of interims no_backup & crash
              oi = new OrderInstanceDetails(order);
            }

            //TODO warum nicht hier immer oi.setLastUpdate(System.currentTimeMillis()); ?
            
            if (statusRunningExecution) {
              if (order.getExecutionType() != XynaOrderServerExtension.ExecutionType.UNKOWN
                  && (oi.getExecutionType() == null || XynaOrderServerExtension.ExecutionType.UNKOWN.toString()
                      .equals(oi.getExecutionType()))) {
                oi.setExecutionType(order.getExecutionType());
                if (order.getExecutionType() == ExecutionType.SERVICE_DESTINATION) {
                  oi.updateProcessInformationForServiceDestination(order);
                }
              }
              if (oi.getSuspensionStatus() != null) {
                oi.setSuspensionStatus(OrderInstanceSuspensionStatus.NOT_SUSPENDED);
                oi.setSuspensionCause(null);
              }
              oi.setLastUpdate(System.currentTimeMillis());
              // at this point ('running execution') the execution type is expected to be set
              oi.updateExecutionInputParametersAfterScheduling(order.getExecutionType(), order.getInputPayload());
            } else if (statusFinished) {
              long stopTime = System.currentTimeMillis();
              oi.setLastUpdate(System.currentTimeMillis());
              oi.setStopTime(stopTime);
              oi.setAuditDataFinished(order);
            } else if (OrderInstanceStatus.FINISHED_EXECUTION == status) {
              oi.setLastUpdate(System.currentTimeMillis());
            } else if (statusScheduling) {
              if (oi.getSuspensionStatus() != null) {
                oi.setSuspensionStatus(OrderInstanceSuspensionStatus.WAITING_FOR_RESUME);
              }
            } else {
              oi.setLastUpdate(System.currentTimeMillis());
            }
            oi.setCustom0(order.getCustom0());
            oi.setCustom1(order.getCustom1());
            oi.setCustom2(order.getCustom2());
            oi.setCustom3(order.getCustom3());
            oi.setMonitoringLevel(order.getMonitoringCode());            
            oi.setStatus(status);
            auditAccess.store(localConnection, oi);
            if (requiresCommit) {
              localConnection.commit();
            }
            if(!(statusScheduling  && OrderInstanceStatus.SCHEDULING == oi.getStatusAsEnum() ) ) {
              order.setHasBeenBackuppedAfterChange(false);
            }
          }
        };


    if (externalConnection != null) {
      wre.executeAndCommit(externalConnection);
    } else {
      WarehouseRetryExecutor.executeWithRetriesOneException(wre, ODSConnectionType.DEFAULT,
                                                            Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                            Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                            new StorableClassList(OrderInstance.class));
    }
  }


  public void delete(Date maxDate) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.executeDML(deleteOldArchived, new Parameter(maxDate.getTime()));
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  /**
   * Liefert alle suspendierten Auftr�ge (ohne Details).
   * Kann die XynaOrder nicht deserialisiert werden, wird der Fehler
   * geloggt und null f�r die XynaOrder zur�ckgegeben.
   * @param revision nur Auftr�ge in dieser Revision suchen. Falls 'null' werden alle Auftr�ge gesucht.
   * @param defaultConnection
   * @return
   * @throws PersistenceLayerException
   */
  public FactoryWarehouseCursor<OrderInstanceBackup> getAllSuspendedNoDetails(Long revision, ODSConnection defaultConnection)
      throws PersistenceLayerException {
    if (defaultConnection.getConnectionType() != ODSConnectionType.DEFAULT) {
      throw new IllegalArgumentException("Unexpected connection type");
    }
    
    Parameter params = new Parameter(getOwnBinding());
    if (revision != null) {
      params.add(revision);
    }
    return defaultConnection
        .getCursor(OrderArchiveQueryHelper.getGetAllOwnSuspendedLogWarningIfNotSerializableWithBeginIndex_XynaOrderOnly
                   + (revision != null ? " AND " + OrderInstanceBackup.COL_REVISION + " = ?" : ""),
                   params, OrderInstanceBackup.getReaderWarnIfNotDeserializableNoDetails(), 100, cache);
  }
  
  /**
   * Auslesen des OrderInstanceBackups zur angegebenen RootOrderId.
   * Falls dabei PersistenceLayerExceptions auftreten, werden �ber XynaProperties konfiguriert Retries versucht.
   * @param rootOrderId
   * @param defaultCon  darf null sein
   * @return
   * @throws PersistenceLayerException
   */
  public OrderInstanceBackup getBackedUpRootOrder(long rootOrderId,
                                                  ODSConnection defaultCon)
      throws PersistenceLayerException {
    boolean needToCloseConnection = false;
    if (defaultCon == null) {
      defaultCon = ods.openConnection(ODSConnectionType.DEFAULT);
      needToCloseConnection = true;
    } else {
      if (defaultCon.getConnectionType() != ODSConnectionType.DEFAULT) {
        throw new RuntimeException("Invalid connectiontype");
      }
    }
    try {
      OrderInstanceBackup oib = new OrderInstanceBackup(rootOrderId, getOwnBinding());

      int numberOfRetries = 0;
      boolean successFullyRead = false;
      while (!successFullyRead) {
        try {
          defaultCon.queryOneRow(oib);
          successFullyRead = true;
        } catch (PersistenceLayerException e) {
          if( maxNumberOfRetriesReached( numberOfRetries, XynaProperty.XYNA_BACKUP_READ_RETRIES.get() ) ) {
            throw e;
          }
          numberOfRetries++;
          sleepIfNoConnectionAvailable(e,  XynaProperty.XYNA_BACKUP_READ_RETRY_WAIT.getMillis(),
                                       OrderInstanceBackup.TABLE_NAME, numberOfRetries);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return null;
        }
      }
      return oib;
    } finally {
      if (needToCloseConnection) {
        defaultCon.closeConnection();
      }
    }
  }

  
  /**
   * Weiderherstellen des ResponseListeners und des OrderContext aus den TransientOrderParts
   * Die TransientOrderParts werden dabei entfernt.
   * @param order
   * @param rootOrderId
   */
  public void restoreTransientOrderParts(XynaOrderServerExtension order, Long rootOrderId) {
    if( order == null ) {
      return;
    }
    if (!(order.getResponseListener() instanceof EmptyResponseListener)) {
      TransientOrderPart transientOrderPart = transientOrderParts.remove(rootOrderId);
      if (transientOrderPart != null) {
        //alle anteile der xynaorder merken, die nach der deserialisierung nicht wieder korrekt hergestellt werden k�nnen

        //responselistener k�nnte zb offenes socket haben
        order.setResponseListener(transientOrderPart.responseListener);
        if (order.getOrderContext() != null) {
          order.getOrderContext().set(EventListener.KEY_CONNECTION, transientOrderPart.triggerConnection);
        }
      }
    }
  }
  
  
  public ResponseListener removeTransientOrderParts(Long rootOrderId) {
    TransientOrderPart top = transientOrderParts.remove(rootOrderId);
    if (top != null) {
      return top.responseListener;
    } else {
      return null;
    }
  }
  
  /**
   * - auftrag speichern, damit er neu gestartet werden kann (nur falls rootauftrag) - orderinstancedetails speichern,
   * weil nur hier die auditdaten gespeichert sind (nicht nur f�r rootauftr�ge)
   */
  public void backup(XynaOrderServerExtension order, BackupCause backupCause) throws PersistenceLayerException {
    backup(order, backupCause, null);
  }


  /**
   * macht kein commit, wenn eine connection != null �bergeben wird
   */
  public void backup(final XynaOrderServerExtension order, BackupCause backupCause, ODSConnection con)
      throws PersistenceLayerException {

    //TODO eigtl sollte hasBeenBackuppedAfterChange nur als kennzeichen daf�r verwendet werden, ob die auftragsdaten gebackupped werden m�ssen
    //     der backup cause sollten unabh�ngig davon sein!
    //     dabei muss man aber aufpassen: es ist nicht immer klar, wo auch das backup der xynaorder notwendig ist.
    //     zb. bei AFTER_SCHEDULING m�ssen evtl auch die ge�nderten flags gespeichert werden, dass kapazit�ten/vetos allokiert wurden?!
    if (order.hasBeenBackuppedAfterChange() || !order.getDestinationKey().isAllowedForBackup()) {
      return;
    }

    boolean needToCloseConnection = false;
    if (con == null) {
      con = ods.openConnection(ODSConnectionType.DEFAULT);
      con.ensurePersistenceLayerConnectivity(OrderInstanceBackup.class);
      needToCloseConnection = true;
    } else if (con.getConnectionType() != ODSConnectionType.DEFAULT) {
      throw new RuntimeException("Invalid connectiontype");
    }

    try {

      //synchronisierung notwendig, damit nicht zb beim serverherunterfahren der runterfahr-thread das backup triggert und gleichzeitig
      //der auftrag selbst zuende l�uft/seine processsuspendedexception verarbeitet...
      // synchronized innerhalb der connection, um connection-deadlocks zu vermeiden
      synchronized (order) {
        if (!order.hasParentOrder() && order.getResponseListener() != null
            && !(order.getResponseListener() instanceof EmptyResponseListener)) {
          TransientOrderPart transientOrderPart = new TransientOrderPart();
          transientOrderPart.responseListener = order.getResponseListener();
          if (order.getOrderContext() != null) {
            transientOrderPart.triggerConnection =
                (TriggerConnection) order.getOrderContext().get(EventListener.KEY_CONNECTION);
          }
          transientOrderParts.put(order.getId(), transientOrderPart);
        }

        if (backupCause == BackupCause.AFTER_SUSPENSION && updateBackupCauseCmd != null) {
          con.executeDML(updateBackupCauseCmd, new Parameter(backupCause.toString(), order.getId()));
        } else {
          // the following is potentially superfluous in the case AFTER_SUSPENSION but the PreparedCommand
          // is not available (e.g. for the XynaMemoryPersistenceLayer that does not support update statements).
          // the details have to be retrieved in this case because the persistObject on the OrderInstanceBackup
          // would otherwhise write <null> to the orderbackup.
          OrderInstanceDetails oid = createOrderInstanceDetails( con, order );

          int numberOfRetries = 0;
          boolean successfullyWroteBackup = false;
          while (!successfullyWroteBackup) {
            try {
              if (order.hasParentOrder()) {
                if (oid != null) {
                  con.persistObject(new OrderInstanceBackup(null, oid, backupCause, getOwnBinding()));
                } //store nothing if child and monitoring too low
              } else {
                con.persistObject(new OrderInstanceBackup(order, oid, backupCause, getOwnBinding()));
              }
              successfullyWroteBackup = true;
            } catch (PersistenceLayerException e) {
              if( maxNumberOfRetriesReached( numberOfRetries, XynaProperty.XYNA_BACKUP_STORE_RETRIES.get() ) ) {
                throw e;
              }
              numberOfRetries++;
              sleepIfNoConnectionAvailable(e, XynaProperty.XYNA_BACKUP_STORE_RETRY_WAIT.getMillis(),
                                           OrderInstanceBackup.TABLE_NAME, numberOfRetries);
            }
          }

        }

        List<BackupAction> additionalBackupActions = order.getAdditionalBackupActions();
        if (additionalBackupActions != null) {
          try {
            for (BackupAction backupAction : additionalBackupActions) {
              backupAction.execute(con);
            }
          } catch (PersistenceLayerException e) {
            logger.warn("Error while trying to execute backup action.", e);
          } finally {
            order.clearAdditionalBackupActions();
          }
        }

        con.executeAfterCommit(new Runnable() {

          public void run() {            
            order.setHasBeenBackuppedAtLeastOnce();
            order.setHasBeenBackuppedAfterChange(true);
          }

        });
        if (needToCloseConnection) {
          con.commit();
        }
      }

    } finally {
      if (needToCloseConnection) {
        con.closeConnection();
      }
    }

  }

  
  /**
   * @param con
   * @param order
   * @return
   * @throws PersistenceLayerException 
   */
  private OrderInstanceDetails createOrderInstanceDetails(ODSConnection con, XynaOrderServerExtension order) throws PersistenceLayerException {
    OrderInstanceDetails oid = null;
    if (order.getMonitoringCode() != null && order.getMonitoringCode() >= MonitoringCodes.START_STOP_MONITORING) {
      oid = new OrderInstanceDetails(order.getId());

      int numberOfRetries = 0;
      boolean successfullyRetrievedDetails = false;
      while (!successfullyRetrievedDetails) {
        try {
          oid = auditAccess.restore(con, order.getId(), false);
          successfullyRetrievedDetails = true;
        } catch (PersistenceLayerException e) {
          if( maxNumberOfRetriesReached(numberOfRetries, XynaProperty.XYNA_BACKUP_STORE_RETRIES.get() ) ) {
            throw e;
          }
          numberOfRetries++;
          sleepIfNoConnectionAvailable(e, XynaProperty.XYNA_BACKUP_STORE_RETRY_WAIT.getMillis(),
                                       OrderInstanceDetails.TABLE_NAME, numberOfRetries);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("order " + oid.getId() + " not found in orderarchive DEFAULT and will thus be saved in backup without details.", e);
          break;
          //TODO hier wird nun fast leere OrderInstanceDetails zur�ckgegeben
          //entweder null?
          //oder ersatzweise aus XynaOrderServerExtension f�llen?
        }
      }
    }
    return oid;
  }


  private boolean maxNumberOfRetriesReached(int numberOfRetries, int maxRetries) {
    return maxRetries > -1 && numberOfRetries > maxRetries;
  }


  /**
   * Is used from the DeploymentManager to store the reloaded objects inside order and audit This method is not supposed
   * to backUp an order that has never been backuped before
   */
  public void renewOrderBackup(OrderInstanceBackup oib) throws PersistenceLayerException {


    if (oib.getDetails() == null && oib.getXynaorder() != null) {
      backup(oib.getXynaorder(), oib.getBackupCauseAsEnum());
    } else {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        int numberOfRetries = 0;
        boolean successfullyWroteBackup = false;
        while (!successfullyWroteBackup) { //FIXME warehouseretryexecutor verwenden (hier und vmtl an allen anderen stellen, wo sleepIfNoConnectionAvaibale verwendet wird)
          try {
            con.persistObject(oib);
            successfullyWroteBackup = true;
          } catch (PersistenceLayerException e) {
            if( maxNumberOfRetriesReached( numberOfRetries, XynaProperty.XYNA_BACKUP_STORE_RETRIES.get() ) ) {
              throw e;
            }
            numberOfRetries++;
            sleepIfNoConnectionAvailable(e, XynaProperty.XYNA_BACKUP_STORE_RETRY_WAIT.getMillis(),
                                         OrderInstanceBackup.TABLE_NAME, numberOfRetries);
          }
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
    }

  }


  private void sleepIfNoConnectionAvailable(PersistenceLayerException e, long timeout, String tableName,
                                            int retryCounter) throws PersistenceLayerException {

    if (!(e instanceof XNWH_RetryTransactionException && e.getCause() instanceof NoConnectionAvailableException)) {
      throw e;
    } 

    if (retryCounter < 2) {
      logger.warn("There are too few connections to persistence layer for table '" + tableName + "', retrying after "
          + (timeout / 1000)
          + " seconds. This will reduce performance, please increase connection pool size or timeout.");
    }
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e1) {
      // wait here, otherwise backup information might get lost
    }

  }


  private void archiveForRetry(OrderInstanceDetails oid, BackupCause cause) {
    hasHadProblemsDuringArchive[cause.getIndexArchivingProblem()] = true;
    if (logger.isInfoEnabled()) {
      logger.info("storing order " + oid.getId() + " for later retry of archive. cause=" + cause.toString());
    }
    try {
      ODSConnection conAlternative = ods.openConnection(ODSConnectionType.ALTERNATIVE);
      try {
        OrderInstanceBackup oib = new OrderInstanceBackup(null, oid, cause, getOwnBinding());
        conAlternative.persistObject(oib);
        conAlternative.commit();
      } finally {
        conAlternative.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("could not store backupinformation for order " + oid.getId() + ".", e);
    } finally {
      // transientOrderParts are no longer needed, they would get removed once the RetryAlgorithm get's around to do it but there's only harm in waiting
      transientOrderParts.remove(oid.getId());
    }
  }


  /**
   * bescheid geben, dass archiving-schritt jetzt funktioniert hat, um eventuell in orderbackup.ALTERNATIVE wartende
   * auftr�ge einen retry machen zu lassen.
   */
  private void triggerArchiveRetry(BackupCause cause) {
    int idx = cause.getIndexArchivingProblem();
    if (hasHadProblemsDuringArchive[idx]) {
      hasHadProblemsDuringArchive[idx] = false;
      archiveRetryExecutor.requestExecution();
    } else {

    }
  }


  private void createAuditXMLInOID(OrderInstanceDetails oid, Long revision, boolean removeAuditData, int monitoringLevel, int runtimeMonLvl) {
    if (runtimeMonLvl == MonitoringCodes.STEP_MONITORING && monitoringLevel < MonitoringCodes.STEP_MONITORING) {
      if (oid.auditData != null) {
        oid.auditData.clearStepData();
      }
    }
    try {
      if (!oid.convertAuditDataToXML(revision, removeAuditData)) {
        return; //bereits konvertiert und auch gespeichert
      }
    } catch (XPRC_CREATE_MONITOR_STEP_XML_ERROR e) {
      //trotzdem archivieren
      logger.warn("Could not create xml auditdata for order " + oid.getId(), e);
    }
    oid.clearAuditDataJavaObjects();
    
    //bis zum commit kann nun eine ganze weile vergehen. in dieser zeit sollte das xml f�r weitere audit-anfragen zur�ckgegeben werden k�nnen.
    //vgl bug 19453
    try {
      if (!ods.isSamePhysicalTable(oid.getTableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        ODSConnection conDefaultTmp = ODSImpl.getInstance().openConnection();
        try {
          auditAccess.store(conDefaultTmp, oid);
          conDefaultTmp.commit();
        } catch (PersistenceLayerException e) {
          //debuglevel weil dieses speichern nicht kritisch ist. nur in verbindung mit einem weiteren fehler beim archivieren kommt es zu problemen.
          logger.debug("could not close connection", e);
        } finally {
          try {
            conDefaultTmp.closeConnection();
          } catch (PersistenceLayerException e) {
            logger.debug("could not close connection", e);
          }
        }
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      //f�llt an anderen stellen auch schon auf. pech
      logger.trace(null, e);
    }
  }


  /**
   * �bergebene connections sollten keine offenen transaktionsschritte haben
   */
  private void internalArchive(final XynaOrderServerExtension xo, ODSConnection conDefault, ODSConnection conHistory,
                               OrderInstanceDetails preparedOid)
      throws PersistenceLayerException, DuplicatedOrderInstanceArchiveException {
    //plan: falls orderarchive.DEFAULT zugriff nicht funktioniert, wird die id und der auftrag gespeichert (orderbackup.ALTERNATIVE) und internalArchive zu einem sp�teren zeitpunkt erneut probiert
    //      falls orderarchive.HISTORY zugriff nicht funktioniert, werden id+orderinstancedetails gespeichert (orderbackup.ALTERNATIVE) und sp�ter erneut probiert. aus orderbackup.DEFAULT kann trotzdem gel�scht werden.    
    //      falls orderbackup.DEFAULT zugriff nicht funktioniert, wird id gespeichert (orderbackup.ALTERNATIVE) und sp�ter erneut probiert zu l�schen.
    //      sp�terer zeitpunkt = sobald die verbindung wieder hergestellt werden kann, d.h. falls ein anderer auftrag erfolgreich da durch ist. alternativ wird alle x sekunden neu versucht (konfigurierbar)
    //      beim speichern in orderbackup.ALTERNATIVE muss unterschieden werden, welcher fall zutrifft. dies geschieht �ber die spalte backupcause.

    long orderId = xo.getId();

    //liest auftrag aus orderdb und schreibt auftrag in orderarchive. auftrag wird aus db und backup gel�scht.
    OrderInstanceDetails oid = new OrderInstanceDetails(xo);
    if (preparedOid != null) {
      oid = preparedOid;
    } else {
      try {
        oid = auditAccess.restore(conDefault, xo.getId(), false);
        triggerArchiveRetry(BackupCause.ARCHIVING_PROBLEM_DEFAULT);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        //hier ist monitoringlevel>0. dieser fall sollte also nur passieren, wenn das insert schon schiefgegangen ist.
      } catch (PersistenceLayerException e) {
        logger.warn("could not access orderarchive default when archiving order " + orderId + ".", e);
        final OrderInstanceDetails localOid = oid;
        conHistory.executeAfterClose(new Runnable() {

          public void run() {
            archiveForRetry(localOid, BackupCause.ARCHIVING_PROBLEM_DEFAULT);
          }
        });
        return;
      }
      int monitoringLevel = MonitoringCodes.getMonitoringLevelForArchiving(xo);
      int runtimeMonLvl = MonitoringCodes.getMonitoringLevelForRuntime(xo);

      createAuditXMLInOID(oid, xo.getRevision(), true, monitoringLevel, runtimeMonLvl);
    }

    try {
      if (!ods.isSamePhysicalTable(oid.getTableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        //nur l�schen, wenn default orderarchive != history orderarchive.
        //ansonsten w�re es zwar erstmal nicht schlimm, hier zu l�schen, weil hinterher
        //geaddet wird. aber weil bis dahin noch kein commit stattgefunden hat,
        //ist die zeile gelockt.
        try {
          auditAccess.delete(conDefault, oid);
          triggerArchiveRetry(BackupCause.ARCHIVING_PROBLEM_DEFAULT);
        } catch (PersistenceLayerException e) {
          logger.warn("could not delete entry from orderarchive (" + ODSConnectionType.DEFAULT.name() + ") for id = " + orderId + ".", e);
          final OrderInstanceDetails localOid = oid;
          conHistory.executeAfterClose(new Runnable() {

            public void run() {
              archiveForRetry(localOid, BackupCause.ARCHIVING_PROBLEM_DEFAULT);
            }
          });
          return;
        }
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      //zumindest ein default persistencelayer sollte ja immer vorhanden sein. ansonsten ist da was stark fehlgeschlagen.
      throw new RuntimeException(e);
    }

    boolean historyPersistenceSuccessful = false;
    try {
      // in dem zeitintervall bis zu dem commit des l�schens ist der gel�schte auftrag ggfs
      // doppelt sichtbar. in search-orders wird das aber abgefangen.
      boolean existedBefore = auditAccess.store(conHistory, oid);
      if (existedBefore && !ods.isSamePhysicalTable(oid.getTableName(), ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        // kann passieren, wenn der server nach dem conHistory.commit und vor dem deleteFromBackup einen fehler hat oder abst�rzt.
        logger.warn("Duplicate entry detected in table <" + OrderInstance.TABLE_NAME + "> with id <" + oid.getId() + ">. ");

        throw new DuplicatedOrderInstanceArchiveException(oid);
      }
      historyPersistenceSuccessful = true;
      triggerArchiveRetry(BackupCause.ARCHIVING_PROBLEM_HISTORY);
    } catch (PersistenceLayerException e) {
      logger.warn("order " + orderId + " could not be written to archive (history).", e);
    }
    if (xo.hasBeenBackuppedAtLeastOnce()) {
      try {
        deleteFromBackupInternally(orderId, conDefault);
        triggerArchiveRetry(BackupCause.ARCHIVING_PROBLEM_ORDERBACKUP);
        if (!historyPersistenceSuccessful) {
          //nicht oben im catchblock von history.persistierung, damit es nicht zu nebenl�ufigkeitsproblemen kommt,
          //weil evtl danach erneut mit anderem backup_cause gespeichert wird
          final OrderInstanceDetails localOid = oid;
          conHistory.executeAfterClose(new Runnable() {

            public void run() {
              archiveForRetry(localOid, BackupCause.ARCHIVING_PROBLEM_HISTORY);
            }
          });
        }
      } catch (PersistenceLayerException e) {
        logger.warn("could not delete entry from orderbackup for id = " + orderId + ".", e);
        final OrderInstanceDetails localOid = oid;
        if (historyPersistenceSuccessful) {
          conHistory.executeAfterClose(new Runnable() {

            public void run() {
              archiveForRetry(localOid, BackupCause.ARCHIVING_PROBLEM_ORDERBACKUP);
            }
          });
        } else {
          conHistory.executeAfterClose(new Runnable() {

            public void run() {
              archiveForRetry(localOid, BackupCause.ARCHIVING_PROBLEM_HISTORY_AND_BACKUP);
            }
          });
        }
      }
    }
  }


  private void ensureConnectivityWithRetries(ODSConnection connection, Class<? extends Storable> klazz)
      throws PersistenceLayerException {
    boolean obtainedConnectivity = false;
    while (!obtainedConnectivity) {
      try {
        connection.ensurePersistenceLayerConnectivity(klazz);
        obtainedConnectivity = true;
      } catch (PersistenceLayerException e) {
        
        if ( (e.getCause() instanceof NoConnectionAvailableException) ) {
          NoConnectionAvailableException ncae = (NoConnectionAvailableException) e.getCause();
          if( ncae.getReason() == NoConnectionAvailableException.Reason.PoolExhausted ) {
            logger.warn(connection.getConnectionType().toString() + " connection pool exhausted, retrying...");
          } else {
            //nicht l�nger warten, evtl. gibt es keine Connection mehr!
            throw e;
          }
        } else {
          //unbekannter Fehler: Archivierung abbrechen 
          throw e;
        }
      }
    }
  }


  private static final XynaPropertyLong threshold =
      new XynaPropertyLong("xprc.xprcods.orderarchive.archiving.memoryprotection.threshold", 1024 * 1024 * 50L)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "During order archiving a partial batch commit of a subset of child orders will be triggered if the sum of their audit xml sizes exceeds this threshold.");
  private static final XynaPropertyInt archivingParallelism =
      new XynaPropertyInt("xprc.xprcods.orderarchive.archiving.parallelism.max", 20)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Maximum number of root orders doing archiving in parallel. Change needs restart of factory.");
  private final Semaphore archivingBarrier = new Semaphore(archivingParallelism.get());

  private class Archiving {

    private ODSConnection conDefault;
    private ODSConnection conHistory;
    private XynaOrderServerExtension order;
    private int accumulatedXMLSize = 0;
    private final Map<Long, OrderInstanceDetails> preparedOrderInstanceDetails = new HashMap<Long, OrderInstanceDetails>();
    boolean firstCommit = true;
    private final long rootId;
    private OrderInstanceDetails rootOID;
    
    public Archiving(XynaOrderServerExtension order) {
      this.order = order;
      this.rootId = order.getId();
    }


    public void archive() throws PersistenceLayerException {
      try {
        archivingBarrier.acquire();
      } catch (InterruptedException e1) {
        throw new RuntimeException("interrupted");
      }
      try {

        List<XynaOrderServerExtension> ordersToArchive = new ArrayList<XynaOrderServerExtension>();
        sortOrdersRecursively(ordersToArchive, order);
        Iterator<XynaOrderServerExtension> it = ordersToArchive.iterator();
        List<XynaOrderServerExtension> nextOrders = new ArrayList<XynaOrderServerExtension>();

        while (it.hasNext()) {
          XynaOrderServerExtension nextOrder = it.next();
          nextOrders.add(nextOrder);

          int monitoringLevel = MonitoringCodes.getMonitoringLevelForArchiving(nextOrder);
          int runtimeMonLvl = MonitoringCodes.getMonitoringLevelForRuntime(nextOrder);
          if ((monitoringLevel >= MonitoringCodes.START_STOP_MONITORING || (monitoringLevel >= MonitoringCodes.ERROR_MONITORING && nextOrder
              .hasError()))) {
            //es wird in orderarchive-history geschrieben =>  audit-xml erzeugen und ggfs batch-commit durchf�hren 
            final OrderInstanceDetails oid;

            ODSConnection conLocalDefault = ods.openConnection();
            try {
              oid = auditAccess.restore(conLocalDefault, nextOrder.getId(), false);
              triggerArchiveRetry(BackupCause.ARCHIVING_PROBLEM_DEFAULT);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              continue;
            } catch (PersistenceLayerException e) {
              logger.warn("Failed to obtain Order Instance Details", e);
              continue;
            } finally {
              conLocalDefault.closeConnection();
            }

            createAuditXMLInOID(oid, nextOrder.getRevision(), true, monitoringLevel, runtimeMonLvl);
            preparedOrderInstanceDetails.put(oid.getId(), oid);
            if (oid.getId() == oid.getRootId()) {
              rootOID = oid;
            }
            if (oid.getAuditDataAsXML() != null) {
              accumulatedXMLSize += oid.getAuditDataAsXML().length();
            }
            if (nextOrder.getCustom0() != null && customField0SubstringMapEnabled.get()) {
              getOrCreateCustomFieldMap(0).map.add(nextOrder.getCustom0());
            }
            if (nextOrder.getCustom1() != null && customField1SubstringMapEnabled.get()) {
              getOrCreateCustomFieldMap(1).map.add(nextOrder.getCustom1());
            }
            if (nextOrder.getCustom2() != null && customField2SubstringMapEnabled.get()) {
              getOrCreateCustomFieldMap(2).map.add(nextOrder.getCustom2());
            }
            if (nextOrder.getCustom3() != null && customField3SubstringMapEnabled.get()) {
              getOrCreateCustomFieldMap(3).map.add(nextOrder.getCustom3());
            }

            if (accumulatedXMLSize > threshold.get()) {
              //batch commit
              archive(nextOrders, it.hasNext());

              accumulatedXMLSize = 0;
              nextOrders.clear();
              preparedOrderInstanceDetails.clear(); //damit k�nnen xmls nun freigegeben werden
              continue; //n�chster batch
            }
          }
        }
        if (nextOrders.size() > 0) {
          archive(nextOrders, false);
          
          preparedOrderInstanceDetails.clear();
          nextOrders.clear();
        }
        order = null;
      } finally {
        archivingBarrier.release();
        if (conHistory != null) {
          conHistory.executeAfterClose(new Runnable() {

            public void run() {
              preCommittedFamilyInfos.lazyCreateGet(rootId).finishedArchiving();
              preCommittedFamilyInfos.cleanup(rootId);
            }

          });
        } else {
          preCommittedFamilyInfos.lazyCreateGet(rootId).finishedArchiving();
          preCommittedFamilyInfos.cleanup(rootId);
        }
      }
    }


    private void archive(List<XynaOrderServerExtension> nextOrders, boolean commit) throws PersistenceLayerException {
      conDefault = ods.openConnection(ODSConnectionType.DEFAULT);
      conHistory = ods.openConnection(ODSConnectionType.HISTORY);

      try {
        
        boolean doEnsureDefaultConnectivity = false;
        boolean doEnsureHistoryConnectivity = false;
        for (XynaOrderServerExtension xose : nextOrders) {
          int monitoringLevel = MonitoringCodes.getMonitoringLevelForArchiving(xose);
          if ((monitoringLevel >= MonitoringCodes.START_STOP_MONITORING || (monitoringLevel >= MonitoringCodes.ERROR_MONITORING && xose.hasError()))) {
            //es muss orderarchive HISTORY geschrieben werden
            doEnsureHistoryConnectivity = true;
          }
          if (xose.hasBeenBackuppedAtLeastOnce()) {
            doEnsureDefaultConnectivity = true;
          }
          int runtimeMonLvl = MonitoringCodes.getMonitoringLevelForRuntime(xose);
          if (runtimeMonLvl >= MonitoringCodes.START_STOP_MONITORING) {
            //es gibt einen orderarchive-DEFAULT eintrag
            doEnsureDefaultConnectivity = true;
          }
        }

        if (doEnsureDefaultConnectivity || doEnsureHistoryConnectivity) {
          conDefault.shareConnectionPools(conHistory);
        }
        if (doEnsureDefaultConnectivity) {
          //ACHTUNG, hier mag es �berfl�ssig sein, die backup connection zu holen, aber wenn man sie nicht holt,
          //werden die connections evtl in der falschen reihenfolge ge�ffnet.
          ensureConnectivityWithRetries(conDefault, OrderInstanceBackup.class);
          ensureConnectivityWithRetries(conDefault, OrderInstanceDetails.class);
        }
        if (doEnsureHistoryConnectivity) {
          ensureConnectivityWithRetries(conHistory, OrderInstanceDetails.class);
        }
        
        long lastDuplicateId = -1;
        family : do {
          Iterator<XynaOrderServerExtension> orderIterator = nextOrders.iterator();
          while (orderIterator.hasNext()) {
            final XynaOrderServerExtension orderToArchive = orderIterator.next();
            try {
              int monitoringLevel = MonitoringCodes.getMonitoringLevelForArchiving(orderToArchive);
              if ((monitoringLevel >= MonitoringCodes.START_STOP_MONITORING || (monitoringLevel >= MonitoringCodes.ERROR_MONITORING && orderToArchive.hasError()))) {
                try {
                  internalArchive(orderToArchive, conDefault, conHistory, preparedOrderInstanceDetails.get(orderToArchive.getId()));
                } catch (DuplicatedOrderInstanceArchiveException doe) {
                  if (lastDuplicateId == doe.oidNew.getId()) {
                    //gleicher auftrag erneut duplikat -> keine endlosschleife erzeugen.
                    //kann auftreten, wenn aus irren gr�nden in der auftragshierarchie die gleiche auftragsid mehrfach vorkommt.
                    logger.warn("Order " + doe.oidNew.getId() + " found as duplicate again. It will not be archived.");
                    orderIterator.remove();
                    lastDuplicateId = -1;
                    continue; //next
                  }
                  lastDuplicateId = doe.oidNew.getId();
                  handleDuplicatedOrderInstanceArchiveException(doe, conDefault, conHistory, orderToArchive, orderIterator);
                  //rollback -> retry.
                  continue family;
                }
              } else {                
                if (orderToArchive.hasBeenBackuppedAtLeastOnce()) {
                  try {
                    deleteFromBackupInternally(orderToArchive.getId(), conDefault);
                  } catch (PersistenceLayerException e) {
                    logger.warn("could not delete entry from orderbackup for id = " + orderToArchive.getId() + ".", e);
                  }
                }
                int runtimeMonLvl = MonitoringCodes.getMonitoringLevelForRuntime(orderToArchive);
                if (runtimeMonLvl >= MonitoringCodes.START_STOP_MONITORING) {
                  try {
                    OrderInstanceDetails oid = new OrderInstanceDetails(orderToArchive);
                    auditAccess.delete(conDefault, oid);
                  } catch (PersistenceLayerException e) {
                    logger.warn("Failed to delete Order Instance Details", e);
                    continue;
                  }
                }
              }
            } finally {
              List<BackupAction> additionalBackupActions = orderToArchive.getAdditionalBackupActions();
              if (additionalBackupActions != null) {
                try {
                  for (BackupAction backupAction : additionalBackupActions) {
                    backupAction.execute(conDefault);
                  }
                } catch (PersistenceLayerException e) {
                  logger.warn("Error while trying to execute backup action.", e);
                } finally {
                  conDefault.executeAfterCommit(new Runnable() {

                    public void run() {
                      orderToArchive.clearAdditionalBackupActions();
                    }
                  });
                }
              }
            }
          }
          break;

        } while (true);

        if (commit) {
          if (firstCommit) {
            preCommittedFamilyInfos.lazyCreateGet(rootId).firstPreCommit();
            preCommittedFamilyInfos.cleanup(rootId);
            firstCommit = false;
          }
          new TwoConnectionBean(conDefault, conHistory, null).commitAndCloseIfOpened();
          // nicht die bereits geschlossenen connections behalten
          conDefault = null;
          conHistory = null;
        }
      } catch (PersistenceLayerException e) {
        closeConnectionsOnError();
        throw e;
      } catch (RuntimeException re) {
        closeConnectionsOnError();
        throw re;
      } catch (Error r) {
        Department.handleThrowable(r);
        closeConnectionsOnError();
        throw r;
      }
    }

    private void sortOrdersRecursively(List<XynaOrderServerExtension> ordersToArchive, XynaOrderServerExtension o) {
      for (XynaOrderServerExtension c: o.getDirectChildOrders()) {
        sortOrdersRecursively(ordersToArchive, c);
      }
      ordersToArchive.add(o);
    }


    private void closeConnectionsOnError() {
      ODSConnection c = conDefault;
      ODSConnection c2 = conHistory;
      conHistory = null;
      conDefault = null;
      try {
        c.closeConnection();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        logger.warn("Failed to close default connection when handling Exception.", e);
      }
      try {
        c2.closeConnection();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        logger.warn("Failed to close history connection when handling Exception.", e);
      }
    }


    public TwoConnectionBean getTwoConnectionBean() {
      return new TwoConnectionBean(conDefault, conHistory, rootOID);
    }

  }

  /**
   * falls nicht rootauftrag: nichts tun
   * falls rootauftrag, passiert f�r gesamte auftragsfamilie falls monitoringlevel es erfordert:
   *       - aufr�umen von default und backup (immer! falls nicht m�glich, wird id gemerkt/geloggt)
   *       - eintrag in history (falls nicht m�glich, wird geloggt, dass eintrag gemerkt und sp�ter 
   *         nochmals versucht wird in history einzuf�gen)
   */
  public TwoConnectionBean archive(XynaOrderServerExtension order) throws PersistenceLayerException {
    if (order.hasParentOrder()) {
      if (!order.removeOrderReferenceIfNotNeededForCompensation()) {
        // auftr�ge f�r compensate aufheben, weil internalarchive nicht zweimal durchgef�hrt werden
        //kann (internal archive l�scht aus default-persistencelayer)
        return EMPTY_TWOCONNECTIONBEAN;
      }      
    }
    Archiving arch = new Archiving(order);
    arch.archive();      
    return arch.getTwoConnectionBean();
  }


  private void handleDuplicatedOrderInstanceArchiveException(DuplicatedOrderInstanceArchiveException duplicateException,
                                                             ODSConnection conDefault, ODSConnection conHistory,
                                                             XynaOrderServerExtension orderToArchive,
                                                             Iterator<XynaOrderServerExtension> orderIterator)
      throws PersistenceLayerException {

    conDefault.rollback();
    conHistory.rollback();
    OrderInstanceDetails oidOld;
    try {
      oidOld = auditAccess.restore(conHistory, orderToArchive.getId(), false);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // retry whole procedure
      return;
    }

    OrderInstanceDetails oidNew = duplicateException.oidNew;

    // next find out whether the two entries are probably the same
    boolean lastUpdateMatches = oidOld.getLastUpdate() == oidNew.getLastUpdate();
    boolean stateMatches = oidOld.getStatusAsEnum() == oidNew.getStatusAsEnum();
    boolean compensateStateMatches =
        (oidOld.getStatusCompensate() == null && oidNew.getStatusCompensate() == null)
            || (oidOld.getStatusCompensate() != null && oidOld.getStatusCompensate().equals(oidNew.getStatusCompensate()));

    boolean oidNewAndOldHaveSimilarProperties = lastUpdateMatches && stateMatches && compensateStateMatches;

    if (oidNewAndOldHaveSimilarProperties) {
      logger.warn("Original entry will be kept, because it seems to be identical");
      if (logger.isDebugEnabled()) {
        logger.debug("old entry: " + oidOld.toStringDetails() + ", new entry: " + oidNew.toStringDetails());
      }
      if (orderToArchive.hasBeenBackuppedAtLeastOnce()) {
        try {
          deleteFromBackupInternally(orderToArchive.getId(), conDefault);
          conDefault.commit();
        } catch (PersistenceLayerException e) {
          logger.warn("could not delete entry from orderbackup for id = " + orderToArchive.getId() + ".",
                      e);
        }
      }
      orderIterator.remove();
    } else {
      logger.warn("Overriding original entry");
      if (logger.isDebugEnabled()) {
        logger.debug("old entry: " + oidOld.toStringDetails() + ", new entry: " + oidNew.toStringDetails());
      }
      try {
        conHistory.deleteOneRow(oidOld);
        conHistory.commit();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not override old entry.", e);
        orderIterator.remove();
      }
    }

  }


  /**
   * @param orderInstanceDetails
   * @throws PersistenceLayerException 
   */
  public void archive(OrderInstanceDetails orderInstanceDetails) throws PersistenceLayerException {
    ODSConnection conHistory = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      ensureConnectivityWithRetries(conHistory, OrderInstanceDetails.class);
      auditAccess.store(conHistory, orderInstanceDetails);
      conHistory.commit();
    } finally {
      conHistory.closeConnection();
    }
  }



  public void deleteFromBackup(XynaOrderServerExtension order) throws PersistenceLayerException {
    if (order.hasBeenBackuppedAtLeastOnce()) {
      ODSConnection conDefault = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        deleteFromBackupInternally(order.getId(), conDefault);
        conDefault.commit();
      } finally {
        conDefault.closeConnection();
      }
    }
  }


  private void deleteFromBackupInternally(long orderId, ODSConnection connection) throws PersistenceLayerException {
    transientOrderParts.remove(orderId); //kann immer gel�scht werden, weil nach dem l�schen aus dem orderbackup wird das nicht mehr ben�tigt
    connection.deleteOneRow(new OrderInstanceBackup(orderId, getOwnBinding()));
  }

  public OrderInstanceResult search(OrderInstanceSelect select, int maxRows) throws PersistenceLayerException {
    return searchOrderInstances(select, maxRows, SearchMode.FLAT);
  }
  
  public static class SubstringMapIndex {
  
    public volatile boolean complete;
    public volatile SubstringMap map;
    
    public SubstringMapIndex(SubstringMap m, boolean complete) {
      map = m;
      this.complete = complete;
    }
  }

  protected volatile SubstringMapIndex customField0SubstringMap;
  protected volatile SubstringMapIndex customField1SubstringMap;
  protected volatile SubstringMapIndex customField2SubstringMap;
  protected volatile SubstringMapIndex customField3SubstringMap;
  private volatile SubstringMapIndex orderTypeMapIndex;
    

  private SubstringMapIndex getOrCreateOrderTypeMap() {
    if (orderTypeMapIndex != null) {
      return orderTypeMapIndex;
    }
    SubstringMap m = SubstringMap.create(false, 1000, 40, 60);
    synchronized (this) {
      if (orderTypeMapIndex == null) {
        orderTypeMapIndex = new SubstringMapIndex(m, false);
      }
    }
    return orderTypeMapIndex;
  }


  private SubstringMapIndex getOrCreateCustomFieldMap(int i) {
    switch (i) {
      case 0 :
        if (customField0SubstringMap != null) {
          return customField0SubstringMap;
        }
        break;
      case 1 :
        if (customField1SubstringMap != null) {
          return customField1SubstringMap;
        }
        break;
      case 2 :
        if (customField2SubstringMap != null) {
          return customField2SubstringMap;
        }
        break;
      case 3 :
        if (customField3SubstringMap != null) {
          return customField3SubstringMap;
        }
        break;
    }
    SubstringMap m = SubstringMap.create(false, 1000, 40, 60);
    synchronized (this) {
      SubstringMapIndex[] arr =
          new SubstringMapIndex[] {customField0SubstringMap, customField1SubstringMap, customField2SubstringMap, customField3SubstringMap};
      if (arr[i] == null) {
        switch (i) {
          case 0 :
            customField0SubstringMap = new SubstringMapIndex(m, false);
            return customField0SubstringMap;
          case 1 :
            customField1SubstringMap = new SubstringMapIndex(m, false);
            return customField1SubstringMap;
          case 2 :
            customField2SubstringMap = new SubstringMapIndex(m, false);
            return customField2SubstringMap;
          case 3 :
            customField3SubstringMap = new SubstringMapIndex(m, false);
            return customField3SubstringMap;
          default :
            throw new RuntimeException();
        }
      } else {
        return arr[i];
      }
    }
  }
  
  
  private static final ResultSetReader<? extends String> reader = new ResultSetReader<String>() {

    @Override
    public String read(ResultSet rs) throws SQLException {
      return rs.getString(1);
    }
    
  };

  private static final XynaPropertyBoolean orderTypeSubstringMapEnabled =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.search.queryexpansion.ordertype.enabled", true)
          .setDefaultDocumentation(DocumentationLanguage.EN, getQueryExpansionDoku("ordertype"));
  private static final XynaPropertyBoolean customField0SubstringMapEnabled =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.search.queryexpansion.custom0.enabled", true)
          .setDefaultDocumentation(DocumentationLanguage.EN, getQueryExpansionDoku("custom0"));
  private static final XynaPropertyBoolean customField1SubstringMapEnabled =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.search.queryexpansion.custom1.enabled", true)
          .setDefaultDocumentation(DocumentationLanguage.EN, getQueryExpansionDoku("custom1"));
  private static final XynaPropertyBoolean customField2SubstringMapEnabled =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.search.queryexpansion.custom2.enabled", true)
          .setDefaultDocumentation(DocumentationLanguage.EN, getQueryExpansionDoku("custom2"));
  private static final XynaPropertyBoolean customField3SubstringMapEnabled =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.search.queryexpansion.custom3.enabled", true)
          .setDefaultDocumentation(DocumentationLanguage.EN, getQueryExpansionDoku("custom3"));
  
  private static String getQueryExpansionDoku(String colName) {
    return "If enabled, orderarchive search queries containing a filter for column " + colName+ " "
        + "will be optimized by using an internal index on top of the used persistence layer. The index will "
        + "use approximately five times the space that would be needed to store "
        + "the possible string values of the " + colName + " column in memory. The server needs to be restarted after changes to this value.";
  }


  private void updateOrderTypesFromDatabase() {
    if (orderTypeSubstringMapEnabled.get() || customField0SubstringMapEnabled.get() || customField1SubstringMapEnabled.get()
        || customField2SubstringMapEnabled.get() || customField3SubstringMapEnabled.get()) {
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
          try {
            try {
              if (orderTypeSubstringMapEnabled.get()) {
                addValuesForSubstringMap(con, OrderInstanceDetails.COL_ORDERTYPE, getOrCreateOrderTypeMap());
              }
              if (customField0SubstringMapEnabled.get()) {
                addValuesForSubstringMap(con, OrderInstanceDetails.COL_CUSTOM0, getOrCreateCustomFieldMap(0));
              }
              if (customField1SubstringMapEnabled.get()) {
                addValuesForSubstringMap(con, OrderInstanceDetails.COL_CUSTOM1, getOrCreateCustomFieldMap(1));
              }
              if (customField2SubstringMapEnabled.get()) {
                addValuesForSubstringMap(con, OrderInstanceDetails.COL_CUSTOM2, getOrCreateCustomFieldMap(2));
              }
              if (customField3SubstringMapEnabled.get()) {
                addValuesForSubstringMap(con, OrderInstanceDetails.COL_CUSTOM3, getOrCreateCustomFieldMap(3));
              }
            } finally {
              con.closeConnection();
            }
            logger.info("finished creating superstring indizes");
          } catch (Exception e) {
            logger.info("could not create superstring-indizes for configured columns", e);
            return;
          }
        }

      }, "QueryExpansion Initialization Thread");
      t.setDaemon(true);
      t.start();
    }
  }


  private static void addValuesForSubstringMap(ODSConnection con, String colName, SubstringMapIndex substringMapIndex) throws PersistenceLayerException {
      PreparedQuery<String> pq =
          con.prepareQuery(new Query<String>("select distinct(" + colName + ") from " + OrderInfoStorable.TABLE_NAME, reader));
      List<String> vals = con.query(pq, new Parameter(), -1, reader);
      substringMapIndex.map.addAll(vals, 5000);
      substringMapIndex.complete = true;
  }


  public OrderInstanceResult searchOrderInstances(OrderInstanceSelect select, int maxRows, SearchMode searchMode)
      throws PersistenceLayerException {
    if (orderTypeSubstringMapEnabled.get() && getOrCreateOrderTypeMap().complete) {
      select.substituteOrderTypeLikeConditions(orderTypeMapIndex.map);
    }
    try {
      return searchOrderInstancesInternally(select, maxRows, searchMode);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      //nochmal probieren, weil der fehler evtl aufgetreten ist, nachdem ein gecachtes query
      //durch eine persistencelayer�nderung nicht mehr funktioniert.
      cache.clear();
      return searchOrderInstancesInternally(select, maxRows, searchMode);
    }
  }


  protected static final XynaPropertyBoolean useOrderArchiveCountQueries =
      new XynaPropertyBoolean("xprc.xprcods.orderarchive.count.use", false)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Should OrderArchive searches use count queries to retrieve the exact counts of filtered orders (true/false).");


  //FIXME duplicate code in ClusteredOrderArchive#searchOrderInstancesInternally
  protected OrderInstanceResult searchOrderInstancesInternally(OrderInstanceSelect select, int maxRows,
                                                               SearchMode searchMode) throws PersistenceLayerException {
    long startTime = System.currentTimeMillis();
    boolean isSamePhysicalTable = ods.isSamePhysicalTable(OrderInstance.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    if (isSamePhysicalTable) {
      select.substituteCustomFieldLikeConditions(customField0SubstringMap, customField1SubstringMap, customField2SubstringMap, customField3SubstringMap);
    }
    Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> searchResult =
        searchAlgorithm.searchConnectionType(select, maxRows, startTime, searchMode, ODSConnectionType.DEFAULT, new ArrayList<OrderInstance>());
    int searchHits = searchResult.getFirst().size();
    if (useOrderArchiveCountQueries.get() && searchHits >= maxRows) {
      searchHits = searchAlgorithm.sendCountQueryForConnectionType(select, ODSConnectionType.DEFAULT);
    }
    if (!isSamePhysicalTable) {
      //bugz 12766: nicht in history suchen, falls das select nur status != finished/failed sucht.
      if (select.doesQueryStatusFinishedOrFailed()) {
        select.substituteCustomFieldLikeConditions(customField0SubstringMap, customField1SubstringMap, customField2SubstringMap, customField3SubstringMap); //nur in history. vorher wird customfield-value nicht in substringmap gespeichert
        Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> historySearchResult =
            searchAlgorithm.searchConnectionType(select, maxRows, startTime, searchMode, ODSConnectionType.HISTORY, searchResult.getSecond());
        mergeSearchResult(searchResult.getFirst(), historySearchResult.getFirst(), searchResult.getSecond(), searchMode);
        if (useOrderArchiveCountQueries.get() && searchResult.getFirst().size() >= maxRows) {
          searchHits += searchAlgorithm.sendCountQueryForConnectionType(select, ODSConnectionType.HISTORY);
        } else {
          searchHits = searchResult.getFirst().size();
        }
      } else {
        if (logger.isInfoEnabled()) {
          try {
            logger.info("Skipping orderarchive history query because it cannot produce any data: "
                + select.getSelectString() + " " + select.getParameter());
          } catch (XNWH_InvalidSelectStatementException e) {
            logger.warn(null, e);
          }
        }
      }
    }
    SortedMap<OrderInstance, Collection<OrderInstance>> trimmedResult = trimResult(searchResult.getFirst(), maxRows);
    List<OrderInstance> result = flattenFamilies(trimmedResult);
    if (useOrderArchiveCountQueries.get()) {
      return new OrderInstanceResult(result, searchHits, trimmedResult.size());
    } else {
      return new OrderInstanceResult(result, -1, -1);
    }
  }


  protected void mergeSearchResult(SortedMap<OrderInstance, Collection<OrderInstance>> m1,
                                   SortedMap<OrderInstance, Collection<OrderInstance>> m2, List<OrderInstance> affectedByPrecommit, SearchMode mode) {
    log("default", m1);
    log("history", m2);
    if (mode == SearchMode.FLAT) {
      for (Entry<OrderInstance, Collection<OrderInstance>> e : m2.entrySet()) {
        m1.put(e.getKey(), e.getValue());
      }
    } else {
      //erst rootid map erstellen, um die sich evtl �berlagenden (wegen precommits) family-selektionen aus default und history zu mergen
      Map<Long, Pair<OrderInstance, Collection<OrderInstance>>> rootIdMap =
          new HashMap<Long, Pair<OrderInstance, Collection<OrderInstance>>>();
      for (Entry<OrderInstance, Collection<OrderInstance>> e : m1.entrySet()) {
        rootIdMap.put(e.getKey().getRootId(), Pair.of(e.getKey(), e.getValue()));
      }
      for (Entry<OrderInstance, Collection<OrderInstance>> e : m2.entrySet()) {
        Pair<OrderInstance, Collection<OrderInstance>> old = rootIdMap.get(e.getKey().getRootId());
        if (old == null) {
          rootIdMap.put(e.getKey().getRootId(), Pair.of(e.getKey(), e.getValue()));
        } else {
          Map<Long, OrderInstance> family = new HashMap<Long, OrderInstance>();
          for (OrderInstance oi : old.getSecond()) {
            family.put(oi.getId(), oi);
          }
          for (OrderInstance oi : e.getValue()) {
            family.put(oi.getId(), oi);
          }
          family.put(e.getKey().getId(), e.getKey());
          rootIdMap.put(e.getKey().getRootId(), Pair.of(e.getKey(), family.values()));
        }
      }
      
      //nun der eigtl merge nach m1.
      if (mode == SearchMode.CHILDREN) {
        for (Entry<OrderInstance, Collection<OrderInstance>> e : m1.entrySet()) {
          Pair<OrderInstance, Collection<OrderInstance>> family = rootIdMap.get(e.getKey().getRootId());
          e.setValue(findChildOrders(e.getKey(), family.getSecond()));
        }
        for (Entry<OrderInstance, Collection<OrderInstance>> e : m2.entrySet()) {
          Pair<OrderInstance, Collection<OrderInstance>> family = rootIdMap.get(e.getKey().getRootId());
          List<OrderInstance> children = findChildOrders(e.getKey(), family.getSecond());
          if (affectedByPrecommit.contains(e.getKey()) && !isComplete(children, e.getKey())) {
            m1.remove(e.getKey());
          } else {
            m1.put(e.getKey(), children);
          }
        }
      } else  if (mode == SearchMode.HIERARCHY) {
        for (Entry<OrderInstance, Collection<OrderInstance>> e : m1.entrySet()) {
          Pair<OrderInstance, Collection<OrderInstance>> family = rootIdMap.get(e.getKey().getRootId());
          e.setValue(family.getSecond());
        }
        for (Entry<OrderInstance, Collection<OrderInstance>> e : m2.entrySet()) {
          Pair<OrderInstance, Collection<OrderInstance>> family = rootIdMap.get(e.getKey().getRootId());
          /*
           * checken, ob family komplett ist, ansonsten ignorieren.
           * inkomplett passiert, wenn das ein neuer auftrag ist (durch where startTime < x ausgeschlossen), oder wenn der auftrag in default wegen maxrow
           * beschr�nkung nicht selektiert wurde, und dann zwischenzeitlich fertiggelaufen ist. dann ist es ok, ihn wegzulassen.
           * 
           * in m1 kann es keine incompletes geben, weil alle partiell archivierten familien aus DEFAULT auch in HISTORY nachselektiert werden
           * 
           * es kann unvollst�ndige familien geben, weil nicht alle auftr�ge der familie hoch genuges monitoringlevel hatten.
           * die m�ssen nat�rlich zur�ckgegeben werden.
           */
          if (affectedByPrecommit.contains(e.getKey()) && !isComplete(family.getSecond(), null)) {
            m1.remove(e.getKey());
          } else {
            m1.put(e.getKey(), family.getSecond());
          }
        }
      }
    }
    log("merged", m1);
  }

  /**
   * @param exception ausnahme. dieses element muss keinen parent haben
   */
  private boolean isComplete(Collection<OrderInstance> family, OrderInstance exception) {
    //komplett = der parent existiert jeweils innerhalb der collection
    //geschwister m�ssen dann auch vorhanden sein, weil diese immer vor dem parent archiviert werden
    Set<Long> ids = new HashSet<Long>();
    ids.add(-1L);
    for (OrderInstance oi : family) {
      ids.add(oi.getId());
    }
    for (OrderInstance oi : family) {
      if (!ids.contains(oi.getParentId())) {
        if (exception == null || oi.getId() != exception.getId()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring incomplete family " + oi.getRootId() + ". Missing parent " + oi.getParentId() + ".");
          }
          return false;
        }
      }
    }
    return true;
  }


  protected List<OrderInstance> findChildOrders(OrderInstance parent, Collection<OrderInstance> familyOrders) {
    //hilfsmap parent->children
    Map<Long, List<OrderInstance>> childOrders = new HashMap<Long, List<OrderInstance>>();
    for (OrderInstance o : familyOrders) {
      List<OrderInstance> children = childOrders.get(o.getParentId());
      if (children == null) {
        children = new ArrayList<OrderInstance>();
        childOrders.put(o.getParentId(), children);
      }
      children.add(o);
    }

    List<OrderInstance> ret = new ArrayList<OrderInstance>();
    List<OrderInstance> currentChildren = new ArrayList<OrderInstance>();
    List<OrderInstance> nextChildren = new ArrayList<OrderInstance>();
    currentChildren.add(parent);

    while (currentChildren.size() > 0) {
      ret.addAll(currentChildren);
      //n�chste kinder bestimmen
      for (OrderInstance o : currentChildren) {
        List<OrderInstance> children = childOrders.get(o.getId());
        if (children != null) {
          nextChildren.addAll(children);
        }
      }

      //swap lists und clear nextChildren
      List<OrderInstance> tmp = currentChildren;
      currentChildren = nextChildren;
      nextChildren = tmp;
      nextChildren.clear();
    }
    return ret;
  }

  protected SortedMap<OrderInstance, Collection<OrderInstance>> trimResult(SortedMap<OrderInstance, Collection<OrderInstance>> untrimmedResults,
                                                                           int maxRows) {
    // trim
    SortedMap<OrderInstance, Collection<OrderInstance>> trimmedResult;
    if (untrimmedResults.size() > maxRows && maxRows > -1) {
      OrderInstance firstUncontainedElement =
          untrimmedResults.keySet().toArray(new OrderInstance[untrimmedResults.size()])[maxRows];
      trimmedResult = untrimmedResults.headMap(firstUncontainedElement);
    } else {
      trimmedResult = untrimmedResults;
    }
    log("trimmed", trimmedResult);

    return trimmedResult;
  }


  private void log(String msg, SortedMap<OrderInstance, Collection<OrderInstance>> map) {
    if (logger.isTraceEnabled()) {
      StringBuilder sb = new StringBuilder(msg);
      sb.append("\n");
      for (Entry<OrderInstance, Collection<OrderInstance>> e : map.entrySet()) {
        sb.append(e.getKey().getId()).append("->{ ");
        for (OrderInstance o : e.getValue()) {
          sb.append(o.getId()).append(" ");
        }
        sb.append("}\n");
      }
      logger.trace(sb.toString());
    }
  }


  protected List<OrderInstance> flattenFamilies(SortedMap<OrderInstance, Collection<OrderInstance>> resultToFlatten) {
    Set<Long> addedOrderIds = new HashSet<Long>();
    List<OrderInstance> flatOrders = new ArrayList<OrderInstance>();
    for (Entry<OrderInstance, Collection<OrderInstance>> orderWithFamily : resultToFlatten.entrySet()) {
      if (!addedOrderIds.contains(orderWithFamily.getKey().getId())) {
        flatOrders.add(orderWithFamily.getKey());
        addedOrderIds.add(orderWithFamily.getKey().getId());
        for (OrderInstance orderInstance : orderWithFamily.getValue()) {
          if (!addedOrderIds.contains(orderInstance.getId())) {
            if (logger.isTraceEnabled()) {
              logger.trace("appending uncontained order from family: " + orderInstance.getId());
            }
            flatOrders.add(orderInstance);
            addedOrderIds.add(orderInstance.getId());
          }
        }
      }
    }
    return flatOrders;
  }


  public OrderInstanceDetails getCompleteOrder(long id) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return searchAlgorithm.getCompleteOrder(id);
  }


  public void updateStatusOnError(XynaOrderServerExtension order, OrderInstanceStatus status) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    boolean schedulingTimeout = status == OrderInstanceStatus.SCHEDULING_TIME_OUT;

    CallStatsType statsType;
    if (schedulingTimeout) {
      statsType = CallStatsType.TIMEOUT;
    } else {
      statsType = CallStatsType.FAILED;
    }
    StatisticsPath statsPath = XynaDispatcher.getSpecificCallStatsAttributePath(order.getDestinationKey().getOrderType(),
                                                                                order.getDestinationKey().getApplicationName(),
                                                                                statsType);
    
    try {
      PushStatistics<Long, LongStatisticsValue> stats = (PushStatistics<Long, LongStatisticsValue>) getFactoryRuntimeStatistics().getStatistic(statsPath);
      if (stats != null) {
        stats.pushValue(new LongStatisticsValue(1L));
      }
    } catch (XFMG_InvalidStatisticsPath e) {
      // ntbd
    }
    

    int monitoringlevel = status.getMinimumMonitoringLevel();
    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order, monitoringlevel)) {
      return;
    }

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      OrderInstanceDetails oi = new OrderInstanceDetails(order.getId());
      if (con.containsObject(oi)) {
        oi = auditAccess.restore(con, order.getId(), false);
        if (order.hasError()) {
          oi.setExceptions(order.getErrors());
        }
      } else {
        oi = new OrderInstanceDetails(order);
      }

      long stopTime = System.currentTimeMillis();
      oi.setLastUpdate(stopTime);
      oi.setStopTime(stopTime);

      oi.setStatus(status);
      oi.setCustom0(order.getCustom0());
      oi.setCustom1(order.getCustom1());
      oi.setCustom2(order.getCustom2());
      oi.setCustom3(order.getCustom3());
      auditAccess.store(con, oi);
      con.commit();
      order.setHasBeenBackuppedAfterChange(false);
    } finally {
      con.closeConnection();
    }
  }


  public Map<Long, OrderInstance> getAllInstances(long offset, int count) throws PersistenceLayerException {
    OrderInstanceResult result;
    try {
      result =
          search(new OrderInstanceSelect().selectAllForOrderInstance().whereId().isBiggerThan(offset).finalizeSelect(OrderInstanceSelect.class), count);
    } catch (XNWH_WhereClauseBuildException e) {
      throw new RuntimeException("problem with select statement", e);
    }
    Map<Long, OrderInstance> map = new TreeMap<Long, OrderInstance>();
    for (OrderInstance oi : result.getResult()) {
      map.put(oi.getId(), oi);
    }
    return map;
  }
  
  public void updateStatusSuspended(XynaOrderServerExtension order, OrderInstanceSuspensionStatus suspendedStatus,
                                    ODSConnection con, com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause suspensionCause)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (order == null) {
      throw new IllegalArgumentException("Cannot update status for order 'null'");
    }
    if (logger.isTraceEnabled()) {
      logger.trace(order + " is updating to suspend status '" + suspendedStatus + "'");
    }
    
    UpdateStatusSuspended uss = new UpdateStatusSuspended(order,suspendedStatus,suspensionCause, this);
    if (con != null) {
      uss.execute(con);
    } else {
      WarehouseRetryExecutor.buildCriticalExecutor().
        storables(uss.getStorableClassList()).
        execute(uss);
    }
    
  }
  
  private static class UpdateStatusSuspended implements WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> {

    private XynaOrderServerExtension order;
    private OrderInstanceSuspensionStatus suspendedStatus;
    private com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause suspensionCause;
    private OrderArchive orderArchive;

    public UpdateStatusSuspended(XynaOrderServerExtension order, OrderInstanceSuspensionStatus suspendedStatus,
                                 com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause suspensionCause,
                                 OrderArchive orderArchive) {
      this.order = order;
      this.suspendedStatus = suspendedStatus;
      this.suspensionCause = suspensionCause;
      this.orderArchive = orderArchive;
    }

    public void execute(ODSConnection con) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      int monitoringlevel = suspendedStatus.getMinimumMonitoringLevel();
      if (orderArchive.checkArgAndCheckStateAndCheckMonitoringLevel(order, monitoringlevel)) {
        // aus threadsicherheit hier ein "select for update", welches die zeile sperrt, bis sie upgedatet ist
        OrderInstanceDetails oi = orderArchive.auditAccess.restore(con, order.getId(), true);
        oi.setLastUpdate(System.currentTimeMillis());
        oi.setSuspensionStatus(suspendedStatus);
        if (suspendedStatus == OrderInstanceSuspensionStatus.NOT_SUSPENDED ) {
          oi.setSuspensionCause(null);
        } else {
          oi.setSuspensionCause(suspensionCause==null ? null : suspensionCause.getName());
        }
        orderArchive.auditAccess.store(con, oi);
      }
    }
 
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      execute(con);
      con.commit();
    }

    public StorableClassList getStorableClassList() {
      return new StorableClassList(OrderInstance.class);
    }
    
  }
  
  
 

  public void updateStatusCompensation(XynaOrderServerExtension order, OrderInstanceCompensationStatus compensationStatus)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (order == null) {
      throw new IllegalArgumentException("Cannot update status for order 'null'");
    }

    if (logger.isDebugEnabled()) {
      logger.debug("updateStatusCompensation: " + order.getId() + " - " + order.getDestinationKey().getOrderType());
      if (logger.isTraceEnabled()) {
        logger.trace(new StringBuilder(order.toString()).append(" is updating to compensation status '")
            .append(compensationStatus).append("'").toString());
      }
    }

    int monitoringlevel = compensationStatus.getMinimumMonitoringLevel();
    if (!checkArgAndCheckStateAndCheckMonitoringLevel(order, monitoringlevel)) {
      logger.debug("Monitoring not high enough");
      return;
    }
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      // aus threadsicherheit hier ein "select for update", welches die zeile sperrt, bis sie upgedatet ist
      OrderInstanceDetails oi =  auditAccess.restore(con, order.getId(), true);
      oi.setLastUpdate(System.currentTimeMillis());
      if (logger.isDebugEnabled()) {
        logger.debug("Updating compensation status to: " + compensationStatus);
      }
      oi.setStatusCompensate(compensationStatus);
      auditAccess.store(con, oi);
      con.commit();
      order.setHasBeenBackuppedAfterChange(false);
    } finally {
      con.closeConnection();
    }
  }

  
  /**
   * Aktualisiert die Application und Version in OrderInstance und macht ein Backup.
   * @param order
   * @param to
   * @param con
   * @throws PersistenceLayerException
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   */
  public void updateApplicationVersion(XynaOrderServerExtension order, RuntimeContext to, ODSConnection con) 
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    if (order == null) {
      throw new IllegalArgumentException("Cannot update status for order 'null'");
    }

    if (logger.isTraceEnabled()) {
      logger.trace(order + " is updating to runtimeContext '" + to + "'");
    }
    
    UpdateRuntimeContext uav = new UpdateRuntimeContext(order, to, this);
    
    if (con != null) {
      uav.execute(con);
    } else {
      WarehouseRetryExecutor.executeWithRetriesOneException(uav, ODSConnectionType.DEFAULT,
                                                            Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                            Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                            uav.getStorableClassList() );
    }
  }
  
  private static class UpdateRuntimeContext implements WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> {

    private XynaOrderServerExtension order;
    private String applicationName;
    private String versionName;
    private String workspaceName;
    private OrderArchive orderArchive;

    public UpdateRuntimeContext(XynaOrderServerExtension order, RuntimeContext to, OrderArchive orderArchive) {
      this.order = order;
      if (to instanceof Application) {
        this.applicationName = ((Application)to).getName();
        this.versionName = ((Application)to).getVersionName();
        this.workspaceName = null;
      } else if (to instanceof Workspace) {
        this.applicationName = null;
        this.versionName = null;
        this.workspaceName = ((Workspace)to).getName();;
      }
      this.orderArchive = orderArchive;
    }

    public void execute(ODSConnection con) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      if (orderArchive.checkArgAndCheckStateAndCheckMonitoringLevel(order, MonitoringCodes.ERROR_MONITORING)) {

     // aus threadsicherheit hier ein "select for update", welches die zeile sperrt, bis sie upgedatet ist
        OrderInstanceDetails oi = orderArchive.auditAccess.restore(con, order.getId(), true);
        oi.setLastUpdate(System.currentTimeMillis());
        oi.setApplicationName(applicationName);
        oi.setVersionName(versionName);
        oi.setWorkspaceName(workspaceName);
        orderArchive.auditAccess.store(con, oi);
      }
      
      //Backup
      order.setHasBeenBackuppedAfterChange(false);
      orderArchive.backup(order, BackupCause.ACKNOWLEDGED, con);
    }
    

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException,
    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      execute(con);
      con.commit();
    }

    public StorableClassList getStorableClassList() {
      return new StorableClassList(OrderInstanceBackup.class, OrderInstance.class);
    }
  }
  
  
  private static class TransientOrderPart {

    private ResponseListener responseListener;
    private TriggerConnection triggerConnection;
  }


  protected RemoteInterface instantiateSearchAlgorithm() {
    return new LocalRemote(ods);
  }


  private static class DuplicatedOrderInstanceArchiveException extends XynaException {

    private static final long serialVersionUID = 2834080676698092083L;
    OrderInstanceDetails oidNew;


    public DuplicatedOrderInstanceArchiveException(OrderInstanceDetails oidNew) {
      this("");
      this.oidNew = oidNew;
    }


    public DuplicatedOrderInstanceArchiveException(String code) {
      super(code);
    }

  }


  public static abstract class BackupAction {

    private final ODSConnectionType neededType;


    public BackupAction(ODSConnectionType neededType) {
      this.neededType = neededType;
    }


    public void execute(ODSConnection con) throws PersistenceLayerException {
      if (con.getConnectionType() == neededType) {
        executeBackupAction(con);
      } else {
        ODSConnection newConnection = ODSImpl.getInstance().openConnection(neededType);
        try {
          executeBackupAction(newConnection);
          newConnection.commit();
        } finally {
          newConnection.closeConnection();
        }
      }
    }


    public abstract void executeBackupAction(ODSConnection con) throws PersistenceLayerException;

  }


  public static TwoConnectionBean EMPTY_TWOCONNECTIONBEAN = new TwoConnectionBean(null, null, null);


  public static class TwoConnectionBean {

    private static final Logger logger = CentralFactoryLogging.getLogger(TwoConnectionBean.class);


    private final ODSConnection defaultConnection;
    private final ODSConnection historyConnection;
    private final OrderInstanceDetails rootOID;


    public TwoConnectionBean(ODSConnection defaultConnection, ODSConnection historyConnection, OrderInstanceDetails rootOID) {
      this.defaultConnection = defaultConnection;
      this.historyConnection = historyConnection;
      this.rootOID = rootOID;
    }
    
    public OrderInstanceDetails getRootOID() {
      return rootOID;
    }

    public void commitAndCloseIfOpened() throws PersistenceLayerException {

      // use various try/finally combinations to make sure that the connections are closed in any case
      boolean committedDefault = false;
      try {
        if (defaultConnection != null && defaultConnection.isOpen()) {
          try {
            defaultConnection.commit();
            committedDefault = true;
          } finally {
            try {
              defaultConnection.closeConnection();
            } catch (Throwable e) {
              Department.handleThrowable(e);
              logger.warn("Failed to close connection", e);
            }
          } 
        } // TODO what if the defaultConnection is not open here? commit HISTORY anyway?
      } finally {
        if (historyConnection != null && historyConnection.isOpen()) {
          try {
            if (committedDefault) {
              historyConnection.commit();
            }
          } finally {
            try {
              historyConnection.closeConnection();
            } catch (Throwable e) {
              Department.handleThrowable(e);
              logger.warn("Failed to close connection", e);
            }
          }
        }
      }

    }


    public void closeUncommited() {
      try {
        try {
          if (defaultConnection != null) {
            defaultConnection.closeConnection();
          }
        } finally {
          if (historyConnection != null) {
            historyConnection.closeConnection();
          }
        }
      } catch (PersistenceLayerException e) {
      }
    }


    public ODSConnection getDefaultConnection() {
      if (defaultConnection != null && defaultConnection.isOpen()) {
        return defaultConnection;
      } else {
        return null;
      }
    }


    public ODSConnection getHistoryConnection() {
      if (historyConnection != null && historyConnection.isOpen()) {
        return historyConnection;
      } else {
        return null;
      }
    }
  }

  public List<Long> listRootOrderIdsFromOrderBackup(ODSConnection con) throws PersistenceLayerException {
    return con.query(getRootOrderIdsFromHistory, new Parameter(), -1);
  }

  public long readRootOrderIdFromOrderBackup(ODSConnection con, long orderId)
      throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    GetRootId gri = new GetRootId(orderId, getOwnBinding());
    if (con != null) {
      return gri.execute(con);
    } else {
      return WarehouseRetryExecutor.executeWithRetriesOneException(gri, ODSConnectionType.DEFAULT,
                                                            Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                            Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                            gri.getStorableClassList() );
    }
  }

  /**
   * TODO Diese Art an die RootId zu kommen ist schlecht, da bei niedrigen MonitoringLeveln keine 
   * OrderInstanceBackup geschrieben werden f�r Nicht-Root-Orders!
   */
  private static class GetRootId implements WarehouseRetryExecutableOneException<Long,XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> {

    private long orderId;
    private int binding;

    public GetRootId(long orderId, int binding) {
      this.orderId = orderId;
      this.binding = binding;
    }

    public StorableClassList getStorableClassList() {
      return new StorableClassList(OrderInstanceBackup.class);
    }

    public long execute(ODSConnection con) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      OrderInstanceBackup oib = new OrderInstanceBackup(orderId, binding);
      con.queryOneRow(oib);
      return oib.getRootId();
    }

    public Long executeAndCommit(ODSConnection con) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      long rootId = execute(con);
      con.commit();
      return rootId;
    }

  }


  public void deleteFromOrderArchive(long orderId) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      con.deleteOneRow(new OrderInstance(orderId));
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public void cleanup(ODSConnection con, List<OrderInstanceDetails> family, String abortionCause)
      throws PersistenceLayerException {
    //aufr�umen
    for (OrderInstanceDetails oid : family) {
      oid.addException(new XPRC_PROCESS_ABORTED_EXCEPTION(oid.getId(), abortionCause));
      oid.setStatus(OrderInstanceStatus.XYNA_ERROR);
      long now = System.currentTimeMillis();
      oid.setLastUpdate(now);
      oid.setStopTime(now);

      try {
        oid.convertAuditDataToXML();
      } catch (Throwable t) {
        logger.warn("Failed to convertAuditDataToXML", t);
      }

      try {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().archive(oid);
      } catch (Throwable t) {
        logger.warn("Could not archive order " + oid.getId() + " from DEFAULT", t);
      }
      transientOrderParts.remove(oid.getId());
    }
    auditAccess.delete(con, family);
  }
  
  
  private FactoryRuntimeStatistics getFactoryRuntimeStatistics() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
  }


  public int countBackuppedOrders(long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      PreparedQuery<Integer> pq =
          con.prepareQuery(new Query<Integer>("select count(*) from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_REVISION + " = ? and " + OrderInstanceBackup.COL_BINDING + " = ?", new ResultSetReader<Integer>() {

            public Integer read(ResultSet rs) throws SQLException {
              return rs.getInt(1);
            }

          }));
      return con.queryOneRow(pq, new Parameter(revision, getOwnBinding()));
    } finally {
      con.closeConnection();
    }
  }


  /**
   * die zur�ckgegebenen orderbackupentries sind nicht vollst�ndig gef�llt.
   * sowohl die xynaorder darin als auch die orderinstancedetails sind leer.
   */
  public FactoryWarehouseCursor<? extends OrderInstanceBackup> getCursorForOrderBackupEntries(ODSConnection con, int blocksize,
                                                                                              long revision, boolean global)
      throws PersistenceLayerException {
    Parameter params = new Parameter(revision);
    if (!global) {
      params.add(getOwnBinding());
    }
    return con.getCursor("select " + OrderInstanceBackup.COL_BACKUP_CAUSE + ", " + OrderInstanceBackup.COL_BOOTCNTID + ", "
                             + OrderInstanceBackup.COL_ID + ", " + OrderInstanceBackup.COL_REVISION + ", "
                             + OrderInstanceBackup.COL_ROOT_ID + ", " + OrderInstanceBackup.COL_BINDING + " from "
                             + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_REVISION + " = ?"
                             + (global ? "" : " AND " + OrderInstanceBackup.COL_BINDING + " = ?"), params,
                         OrderInstanceBackup.getSelectiveReader(),
                         blocksize);
  }

  //Hilfskonstrukt f�r die Suche nach Auftragsteilfamilien, falls diese teilweise in OrderArchive DEFAULT und teilweise in HISTORY sind.
  private static class OrderFamilyMemberList extends ObjectWithRemovalSupport {

    private boolean preCommittedSomeOrders = false;
    private final List<AtomicBoolean> notifications = new ArrayList<AtomicBoolean>(1); 
    
    @Override
    protected synchronized boolean shouldBeDeleted() {
      return notifications.isEmpty() && !preCommittedSomeOrders;
    }
    
    public synchronized void firstPreCommit() {
      preCommittedSomeOrders = true;
      for (AtomicBoolean c : notifications) {
        c.set(true);
      }
      notifications.clear(); //einmal auf true gesetzt, kann man sie entfernen
    }
    
    public synchronized void finishedArchiving() {
      preCommittedSomeOrders = false;
      notifications.clear();
    }
    
    public synchronized void register(AtomicBoolean c) {
      if (preCommittedSomeOrders) {
        c.set(true);
      } else {
        notifications.add(c);
      }
    }

    public synchronized void deregister(AtomicBoolean c) {
      notifications.remove(c);
    }

  }
  
  private final ConcurrentMapWithObjectRemovalSupport<Long, OrderFamilyMemberList> preCommittedFamilyInfos = new ConcurrentMapWithObjectRemovalSupport<Long, OrderFamilyMemberList>() {

    private static final long serialVersionUID = 1L;

    @Override
    public OrderFamilyMemberList createValue(Long key) {
      return new OrderFamilyMemberList();
    }
  };


  public void registerPreCommitNotification(AtomicBoolean preCommitted, long rootId) {
    preCommittedFamilyInfos.lazyCreateGet(rootId).register(preCommitted);
    preCommittedFamilyInfos.cleanup(rootId);
  }


  public void removePreCommitNotification(AtomicBoolean preCommitted, long rootId) {
    if (!preCommitted.get()) { //ansonsten wurde bereits aufger�umt
      preCommittedFamilyInfos.lazyCreateGet(rootId).deregister(preCommitted);
      preCommittedFamilyInfos.cleanup(rootId);
    }
  }


  public AuditStorageAccess getAuditAccess() {
    return auditAccess;
  }

  
}
