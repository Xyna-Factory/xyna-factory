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

package com.gip.xyna.xprc.xsched.cronlikescheduling;



import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.db.InList;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResult;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutable;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CronCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderAlreadyExistsException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.CronLikeOrderFailSafeReader;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderColumn;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;



/**
 * Folgende Anforderungen gibt es für den Cluster-Betrieb an den Cron Like Scheduler:<br>
 * * In den Zuständen CONNECTED, SINGLE und DISCONNECTED werden nur Cron Like Orders für das eigene Binding verarbeitet.<br>
 * * Im Zustand DISCONNECTED_MASTER werden Cron Like Orders mit beliebigen Bindings verarbeitet.<br>
 * * Im Zustand DISCONNECTED_SLAVE werden gar keine Cron Like Orders verarbeitet.
 */
public class CronLikeScheduler extends CronLikeSchedulingClusterServices implements IPropertyChangeListener {


  static {
    ArrayList<XynaFactoryPath> dependencies = new ArrayList<XynaFactoryPath>();
    dependencies.add(new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class, CapacityManagement.class));
    dependencies.add(new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class, XynaDispatcher.class));
    addDependencies(CronLikeScheduler.class, dependencies);
  }


  public enum CronLikeOrderPersistenceOption {
    REMOVE_ON_SHUTDOWN, REMOVE_ON_SHUTDOWN_IF_NO_CLUSTER, DONT_REMOVE_ON_SHUTDOWN
  }
  
  public static final String DEFAULT_NAME = "Cron Like Scheduler";

  private ODS ods;
  private volatile boolean schedulingStopped = false;
  private volatile boolean hideInternalOrders = true;


  public CronLikeScheduler() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
   
    fExec.addTask("CronLikeScheduler.initStorables", "CronLikeScheduler.initStorables").
      after(PersistenceLayerInstances.class).
      before(XynaClusteringServicesManagement.class).
      execAsync(new Runnable() { public void run() { initStorables(); }});
 
    fExec.addTask(CronLikeScheduler.class, "CronLikeScheduler").
      after("CronLikeScheduler.initStorables").
      after( WorkflowDatabase.FUTURE_EXECUTION_ID ). //zum Lesen der serialisierten XMOM-Objekte
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).//Aufträge könnten CLOs erstellen
      execAsync(new Runnable() { public void run() { initCronLikeScheduler(); }});

    fExec.addTask("CronLikeScheduler.startTimerThread", "CronLikeScheduler.startTimerThread").
      after(CronLikeScheduler.class).
      after(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION). //CLOs können Aufträge starten
      execAsync(new Runnable() { public void run() { startTimerThread(); }});

    XynaProperty.XYNA_BACKUP_DURING_CRON_LIKE_SCHEDULING.registerDependency(UserType.XynaFactory,DEFAULT_NAME);

  }


  @Override
  public void shutdown() throws XynaException {
    if (ods == null) {
      return; // nicht initialisiert
    }
    stopScheduling();
    if (storableClusterContext != null) {
      deleteMarkedOrdersFromDefault();
    }
    copyCronLikeOrdersBetweenLayers(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
    ods = null;
  }


  /**
   * Initialize cron like timer.
   */
  private void initializeTimer() {
    cronLikeTimer = new CronLikeTimer(this);
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.registerAlgorithm(cronLikeTimer);
  }


  /**
   * Start cron like timer.
   */
  @Deprecated
  public void startTimerThread() {
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.startAlgorithm(CronLikeTimer.CRONLIKETIMER_THREAD_NAME);
  }


  @Deprecated
  public void stopScheduling() {
    if (cronLikeTimer == null) {
      return;
    }

    logger.info("Stopping Cron like timer thread");
    schedulingStopped = true;

    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    tm.stopAlgorithm(CronLikeTimer.CRONLIKETIMER_THREAD_NAME);
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void initStorables() {
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(CronLikeScheduler.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException(e);
    }
    
    rmiClusterContext = new ClusterContext();
    rmiClusterStateChangeHandler = newRMIClusterStateChangeHandler();
    rmiClusterContext.addClusterStateChangeHandler( rmiClusterStateChangeHandler );

    try {
      ods = ODSImpl.getInstance();
      ods.registerStorable(CronLikeOrder.class);
      
      storableClusterContext = new ClusterContext( CronLikeOrder.class, ODSConnectionType.DEFAULT );
      cloClusterStateChangeHandler = new ClusteredCLSchedulerChangeHandler(storableClusterContext);
      
      ods.addClusteredStorableConfigChangeHandler( storableClusterContext, ODSConnectionType.DEFAULT, CapacityStorable.class);

      storableClusterContext.addClusterStateChangeHandler( cloClusterStateChangeHandler );
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }
  
  public void initCronLikeScheduler() {
      CronLikeOrder.setAlgorithm(DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.singleInstance);
      initializeTimer();
      try {
        copyCronLikeOrdersBetweenLayers(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);

        WarehouseRetryExecutableNoResultOneException<XPRC_CronRemovalException> wre =
            new WarehouseRetryExecutableNoResultOneException<XPRC_CronRemovalException>() {
          
          public void executeAndCommit(ODSConnection con) throws PersistenceLayerException, XPRC_CronRemovalException {
            
            CronLikeOrderFailSafeReader reader = new CronLikeOrderFailSafeReader();
            PreparedCommand updateCommand = null;
            try {
              updateCommand = con.prepareCommand(new Command("update " + CronLikeOrder.TABLE_NAME + " set " + CronLikeOrder.COL_ENABLED 
                                           + " = ? where " + CronLikeOrder.COL_ID + " = ?"));
            } catch(PersistenceLayerException e) {
              // ignore
            }
            
            FactoryWarehouseCursor<CronLikeOrder> cursor = getCursorForRelevantCronLikeOrders(con, 250, reader);
            if(cursor != null) {
              
              List<CronLikeOrder> entryList = cursor.getRemainingCacheOrNextIfEmpty();
              while(!entryList.isEmpty()) {
                if (logger.isDebugEnabled()) {
                  logger.debug("Found " + entryList.size() + " old cron like order" + (entryList.size() == 1 ? "" : "s") + ".");
                }
                cleanupCronLikeOrdersAtStartUp(entryList, con);
                entryList = cursor.getRemainingCacheOrNextIfEmpty();
              }
              if (!reader.getFailedIds().isEmpty()) {
                if(updateCommand != null) {
                  for(Long id : reader.getFailedIds()) {
                    logger.warn("Disable cron like order with id = " + id + " because the input payload could not be deserialized.");
                     XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME, "Cron Like Order with id " + id
                            + " is disabled because the input payload could not be deserialized.");
                    con.executeDML(updateCommand, new Parameter(Boolean.FALSE, id));
                  }
                  con.commit();
                } else {
                  StringBuilder str = new StringBuilder("Failed to deserialize cron like orders with the following ids (unable to disable the cron like orders): ");
                  Iterator<Long> iter = reader.getFailedIds().iterator();
                  while(iter.hasNext()) {
                    str.append(iter.next());
                    if(iter.hasNext()) {
                      str.append(", ");
                    }
                  }
                  logger.error(str.toString());
                  throw new RuntimeException("Failed to load cron like order database.");
                }                
              }
            }
          }
        };

        WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);

        
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      } catch (XPRC_CronRemovalException e) {
        // ist eigentlich auch nur ne gekapselte PersistenceLayerException
        throw new RuntimeException(e);
      }
  }



  // ##### Fachlich relevante Methoden #####

  /**
   * Create a new cron like order
   * 
   * @param clocp creation parameters of the cron like order
   * @return the newly created cron like order
   * @throws XPRC_CronLikeSchedulerException
   * @throws XNWH_RetryTransactionException
   */
  public CronLikeOrder createCronLikeOrder(CronLikeOrderCreationParameter clocp, ODSConnection con)
                  throws XPRC_CronLikeSchedulerException, XNWH_RetryTransactionException {
    return createCronLikeOrder(clocp, CronLikeOrderPersistenceOption.DONT_REMOVE_ON_SHUTDOWN, con);
  }

  /**
   * Create a new cron like order, add it to the order queue and store it in default connection.
   * 
   * @param clocp creation parameters of the cron like order
   * @return the newly created cron like order
   * @throws XPRC_CronCreationException if order creation failed (e.g. creation parameters are null)
   * @throws XNWH_RetryTransactionException
   * @throws XPRC_InvalidCronLikeOrderParametersException 
   */
  public CronLikeOrder createCronLikeOrder(CronLikeOrderCreationParameter clocp, CronLikeOrderPersistenceOption removeOnShutdownOption,
                                           ODSConnection con) throws XPRC_CronLikeOrderStorageException,
                  XPRC_CronCreationException, XNWH_RetryTransactionException, XPRC_InvalidCronLikeOrderParametersException {
    return createCronLikeOrder(clocp, removeOnShutdownOption, con, Thread.NORM_PRIORITY );
  }


  private void verifyCronLikeOrderCreationParameter(CronLikeOrderCreationParameter clocp)
      throws XPRC_InvalidCronLikeOrderParametersException {
    if (!CronLikeOrderCreationParameter.verifyTimeZone(clocp.getTimeZoneID())) {
      throw new XPRC_InvalidCronLikeOrderParametersException("Invalid time zone specified: " + clocp.getTimeZoneID());
    }

    if (clocp.getConsiderDaylightSaving()
        && !CronLikeOrderCreationParameter.verifyIntervalQualifiesForDST(clocp.getInterval())) {
      throw new XPRC_InvalidCronLikeOrderParametersException(
                                                             "Daylight saving time can only be used for intervals that are multiple of whole days (24h).");
    }

    if (clocp.getConsiderDaylightSaving()
        && !CronLikeOrderCreationParameter.verifyTimeZoneHasDST(clocp.getTimeZoneID())) {
      throw new XPRC_InvalidCronLikeOrderParametersException("The provided time zone " + clocp.getTimeZoneID()
          + " does not have daylight saving time but the flag for considering daylight saving was set.");
    }
  }
  
  /**
   * Überprüft, dass die Application nicht im Zustand AUDIT_MODE ist
   * @param DestinationKey
   */
  private void checkApplicationState(DestinationKey destinationKey) throws XPRC_InvalidCronLikeOrderParametersException {
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    String applicationName = destinationKey.getApplicationName();
    String versionName = destinationKey.getVersionName();
    if (applicationName != null && versionName != null) {
      try{
        ApplicationState state = applicationManagement.getApplicationState(applicationName, versionName);
        if (state == ApplicationState.AUDIT_MODE) {
          throw new XPRC_InvalidCronLikeOrderParametersException("invalid application state: " + state);
        }
      } catch(PersistenceLayerException e) {
        throw new XPRC_InvalidCronLikeOrderParametersException("Could not get state of application");
      }
    }
  }
  
  /**
   * Create a new cron like order, add it to the order queue and store it in default connection.
   * 
   * @param clocp creation parameters of the cron like order
   * @param prio Priority in executeAfterCommitAction
   * @return the newly created cron like order
   * @throws XPRC_CronCreationException if order creation failed (e.g. creation parameters are null)
   * @throws XNWH_RetryTransactionException
   * @throws XPRC_InvalidCronLikeOrderParametersException 
   */
  public CronLikeOrder createCronLikeOrder(CronLikeOrderCreationParameter clocp, CronLikeOrderPersistenceOption removeOnShutdownOption,
                                           ODSConnection con, final int prio) throws XPRC_CronLikeOrderStorageException,
                  XPRC_CronCreationException, XNWH_RetryTransactionException, XPRC_InvalidCronLikeOrderParametersException {
    try {
      return createCronLikeOrder(clocp, removeOnShutdownOption, con, prio, null);
    } catch( XPRC_CronLikeOrderAlreadyExistsException e ) {
      throw new RuntimeException(e); //unerwartet, da uniqueKeys==null
    }
  }
  
    /**
     * Create a new cron like order, add it to the order queue and store it in default connection.
     * 
     * @param clocp creation parameters of the cron like order
     *        Achtung: startTime wird von der GUI immer in UTC übergeben. Diese wird hier zusammen
     *        mit der übergebenen Zeitzone konvertiert.
     * @param prio Priority in executeAfterCommitAction
     * @param uniqueKeys Set@lt;CronLikeOrderColumn&gt;: Diese Spalten müssen einen eindeutigen Wert 
     *        haben, d.h. nicht die gleichen Daten wie die neue CLO.
     *        Achtung: Dies gewährleistet keine echte Eindeutigkeit gegenüber bereits gelaufenen Crons 
     *                 und zeitgleich angelegten!
     * @return the newly created cron like order
     * @throws XPRC_CronCreationException if order creation failed (e.g. creation parameters are null)
     * @throws XPRC_CronLikeOrderStorageException
     * @throws XNWH_RetryTransactionException
     * @throws XPRC_InvalidCronLikeOrderParametersException
     * @throws XPRC_CronLikeOrderAlreadyExistsException Suche nach den uniqueKeys hat Einträge gefunden
     */
    public CronLikeOrder createCronLikeOrder(CronLikeOrderCreationParameter clocp, CronLikeOrderPersistenceOption removeOnShutdownOption,
                                             ODSConnection con, final int prio,
                                             EnumSet<CronLikeOrderColumn> uniqueKeys) throws XPRC_CronLikeOrderStorageException,
                    XPRC_CronCreationException, XNWH_RetryTransactionException, XPRC_InvalidCronLikeOrderParametersException, XPRC_CronLikeOrderAlreadyExistsException{
    if (clocp == null) {
      throw new XPRC_CronCreationException((Long) null,
                     new IllegalArgumentException("Cron like order creation parameters may not be null."));
    }
    
    //überprüfen, dass die Application nicht im Zustand AUDIT_MODE ist
    checkApplicationState(clocp.getDestinationKey());
    
    verifyCronLikeOrderCreationParameter(clocp);

    Long convertedStartTime = convertStartTimeUsingTimeZone(clocp.getTimeZoneID(), clocp.getStartTime());
    clocp.setStartTime(convertedStartTime);
    
    CronLikeOrder newCronLikeOrder = new CronLikeOrder(clocp, currentOwnBinding);
    
    switch (removeOnShutdownOption) {
      case DONT_REMOVE_ON_SHUTDOWN:
        newCronLikeOrder.setRemoveOnShutdown(false);
        break;
      case REMOVE_ON_SHUTDOWN:
        newCronLikeOrder.setRemoveOnShutdown(true);
        break;
      case REMOVE_ON_SHUTDOWN_IF_NO_CLUSTER:
        newCronLikeOrder.setRemoveOnShutdown(!isClustered());
        break;
    }
    
    if (storableClusterContext.getClusterState() == ClusterState.DISCONNECTED_SLAVE || schedulingStopped) {
      // we are in slave mode or the scheduler is stopped, so we just write the new order into the database
      handleSchedulingNotPossible(newCronLikeOrder, con);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Creating CronLikeOrder " + newCronLikeOrder.getId());
      }
      boolean usingForeignConnection = (con != null);
      CLOStore cloStore = new CLOStore(cronLikeTimer,newCronLikeOrder,prio,!usingForeignConnection,uniqueKeys);

      try {
        if (usingForeignConnection) {
          cloStore.executeAndCommit(con);
        } else {
          WarehouseRetryExecutor.buildCriticalExecutor().
          connection(ODSConnectionType.DEFAULT).
          storable(CronLikeOrder.class).
          execute(cloStore);
        }
      } catch (XNWH_RetryTransactionException ctcbe) {
        if (usingForeignConnection) {
          throw ctcbe;
        } else {
          throw new XPRC_CronLikeOrderStorageException(newCronLikeOrder.getId(), ctcbe);
        }
      } catch (PersistenceLayerException e) {
        throw new XPRC_CronLikeOrderStorageException(newCronLikeOrder.getId(), e);
      }
    }

    //Application auf RUNNING setzen
    setApplicationRunning(newCronLikeOrder);
    
    return newCronLikeOrder;
  }


  /**
   * startTime is in UTC, but if originTimeZoneId is not UTC, it has to be converted.
   */
  private Long convertStartTimeUsingTimeZone(String originTimeZoneId, Long startTime) {
    if(startTime == null) {
      return null;
    }
    ZonedDateTime sourceZDT = Instant.ofEpochMilli(startTime).atZone(ZoneId.of("UTC"));
    Instant destinationInstant = sourceZDT.withZoneSameLocal(ZoneId.of(originTimeZoneId)).toInstant();
    return destinationInstant.toEpochMilli();
  }


  /**
   * startTime is correct, but GUI expects it to be offset from UTC by time difference defined by the timeZone (since that is how it was created)
   */
  private Long convertStartTimeBackToUTC(CronLikeOrderInformation info) {
    if(info == null || info.getStartTime() == null) {
      return null;
    }
    ZonedDateTime sourceZDT = Instant.ofEpochMilli(info.getStartTime()).atZone(ZoneId.of(info.getTimeZoneID()));
    Instant destinationInstant = sourceZDT.withZoneSameLocal(ZoneId.of("UTC")).toInstant();
    return destinationInstant.toEpochMilli();
  }


  private static class CLOStore implements WarehouseRetryExecutableNoResultOneException<XPRC_CronLikeOrderAlreadyExistsException> {
    
    private CronLikeTimer cronLikeTimer;
    private CronLikeOrder cronLikeOrder;
    private int prio;
    private boolean doCommit;
    private EnumSet<CronLikeOrderColumn> uniqueKeys;

    public CLOStore(CronLikeTimer cronLikeTimer, CronLikeOrder cronLikeOrder, int prio, boolean doCommit, EnumSet<CronLikeOrderColumn> uniqueKeys) {
      this.cronLikeTimer = cronLikeTimer; 
      this.cronLikeOrder = cronLikeOrder;
      this.prio = prio;
      this.doCommit = doCommit;
      this.uniqueKeys = uniqueKeys;
    }

    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException, XPRC_CronLikeOrderAlreadyExistsException {

      if( uniqueKeys != null ) {
        if( CronLikeOrderHelpers.countKeys(uniqueKeys,cronLikeOrder,con) > 0 ) {
          EnumMap<CronLikeOrderColumn,Object> map = new EnumMap<CronLikeOrderColumn,Object>(CronLikeOrderColumn.class);
          for( CronLikeOrderColumn cloc : uniqueKeys ) {
            map.put(cloc, cronLikeOrder.get(cloc));
          }
          throw new XPRC_CronLikeOrderAlreadyExistsException(map.toString());
        }
      }
      
      cronLikeTimer.markAsNotToSchedule(cronLikeOrder.getId());

      con.executeAfterCommit(new Runnable() {
        public void run() {
          cronLikeTimer.tryAddNewOrder(cronLikeOrder);
        }
      }, prio);
      con.executeAfterCommitFails(new Runnable() {
        public void run() {
          cronLikeTimer.unmarkAsNotToSchedule(cronLikeOrder.getId());
        }
      }, prio);
      CronLikeOrderHelpers.store(cronLikeOrder, con);

      if(doCommit) {
        con.commit();
      }

    }
    
  }
    

  private void handleSchedulingNotPossible(final CronLikeOrder newCronLikeOrder, ODSConnection con)
                  throws XPRC_CronLikeOrderStorageException, XNWH_RetryTransactionException {
    if (logger.isDebugEnabled()) {
      logger.debug("scheduling stopped or clusterstate is DISCONNECTED_SLAVE - persist only the cronlikeorder " + newCronLikeOrder
                      .getId());
    }

    final boolean usingConnection = con != null;

    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        CronLikeOrderHelpers.store(newCronLikeOrder, con);

        if (!usingConnection) {
          con.commit();
        }
      }
    };

    try {
      if (usingConnection) {
        wre.executeAndCommit(con);
      } else {
        WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
      }
    } catch (XNWH_RetryTransactionException ctcbe) {
      if (usingConnection) {
        throw ctcbe;
      } else {
        throw new XPRC_CronLikeOrderStorageException(newCronLikeOrder.getId(), ctcbe);
      }
    } catch (PersistenceLayerException e) {
      throw new XPRC_CronLikeOrderStorageException(newCronLikeOrder.getId(), e);
    }
  }


  /**
   * Remove all cron like orders from default connection which are marked as 'remove on shutdown'
   */
  private void deleteMarkedOrdersFromDefault() throws PersistenceLayerException {

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor = getCursorForRelevantCronLikeOrders(con, 250, new CronLikeOrder.CronLikeOrderReader());
      if(cursor != null) {
        List<CronLikeOrder> orders = cursor.getRemainingCacheOrNextIfEmpty();
        
        while(!orders.isEmpty()) {
          Collection<CronLikeOrder> toBeRemovedOnShutdown = null;
          for (CronLikeOrder order : orders) {
            if (order.getRemoveOnShutdown()) {
              if (toBeRemovedOnShutdown == null) {
                // lazily create the hashset
                toBeRemovedOnShutdown = new HashSet<CronLikeOrder>();
              }
              toBeRemovedOnShutdown.add(order);
            }
          }
          if (toBeRemovedOnShutdown != null && toBeRemovedOnShutdown.size() > 0) {
            if (logger.isDebugEnabled()) {
              logger.debug("Deleting " + toBeRemovedOnShutdown.size() + " cron like orders from default persistence layer");
            }
            con.delete(toBeRemovedOnShutdown);
            con.commit();
          }
          orders = cursor.getRemainingCacheOrNextIfEmpty();
        }
      }
    } finally {
      con.closeConnection();
    }
  }


  public boolean removeCronLikeOrder(ODSConnection externalConnection, final Long id) throws PersistenceLayerException,
      XPRC_CronRemovalException {

    final boolean requiresCommit = (externalConnection == null);

    WarehouseRetryExecutableOneException<Boolean, XPRC_CronRemovalException> wre =
        new WarehouseRetryExecutableOneException<Boolean, XPRC_CronRemovalException>() {

          public Boolean executeAndCommit(ODSConnection localConnection) throws PersistenceLayerException,
              XPRC_CronRemovalException {
            CronLikeOrder clo = new CronLikeOrder(id);
            try {
              localConnection.queryOneRow(clo);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
              if (logger.isDebugEnabled()) {
                logger.debug("Trying to remove a Cron Like Order <" + id + "> that does not exist");
              }
              return false;
            }
            // if locally bound just super
            if (isResponsibleForBinding(clo.getBinding())) {
              boolean existedInDefault = removeCronLikeOrderLocaly(localConnection, id);
              if (requiresCommit) {
                localConnection.commit();
              }
              return existedInDefault;
            } else if (!storableClusterContext.getClusterState().isDisconnected()) { // else forward to corresponding if is connected
              try {
                initiateRemoteRemoveCronLikeOrder(id);
              } catch (RemoteException e) {
                throw new XPRC_CronRemovalException(id, e);
              } catch (RMIConnectionFailureException e) {
                throw new XPRC_CronRemovalException(id, e);
              }
              return true;
            } else {
              // FIXME Cause anhängen, der erkennen lässt, was das Problem ist. Ist das sogar eine RuntimeException? Oder
              //       nur ein Race während der Disconnect erkannt wird?
              throw new XPRC_CronRemovalException(id);
            }
          }
        };

    try {
      if (externalConnection != null) {
        return wre.executeAndCommit(externalConnection);
      } else {
        return WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
      }
    } catch (XNWH_RetryTransactionException e) {
      if (externalConnection != null) {
        throw e;
      } else {
        return false;
      }
    }
  }


  /**
   * @return true, falls auftrag entfernt werden konnte
   */
  public boolean removeCronLikeOrder(CronLikeOrder order) throws XPRC_CronLikeOrderStorageException,
                  XPRC_CronRemovalException {

    if (order == null) {
      throw new IllegalArgumentException("Order to be removed may not be null");
    }

    return removeCronLikeOrderWithRetryIfConnectionIsBroken(order.getId());
  }


  /**
   * @return true, falls die Order existierte und entfernt werden konnte
   * @throws XPRC_CronRemovalException
   */
  public boolean removeCronLikeOrderWithRetryIfConnectionIsBroken(Long id) throws XPRC_CronRemovalException {
    try {
      return removeCronLikeOrder(null, id);
    } catch (XNWH_RetryTransactionException e) {
      logger.warn("could not remove cronlikeorder " + id, e);
      return false;
    } catch (PersistenceLayerException e) {
      throw new XPRC_CronRemovalException(id, e);
    }
  }


  /**
   * Entfernt und stoppt eine CronLikeOrder
   * @param con
   * @return false, falls auftrag nicht entfernt werden konnte, true falls auftrag nicht existierte oder entfernt werden
   *         konnte
   * @throws PersistenceLayerException
   * @throws XPRC_CronRemovalException
   */
  private boolean removeCronLikeOrderLocaly(ODSConnection con, final Long id) throws XPRC_CronRemovalException,
      PersistenceLayerException {

    final boolean localConnection = con == null;

    final CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    
    if (!schedulingStopped) {
      copyOfCronLikeTimer.markAsNotToScheduleAndRemoveFromQueue(id);

      WarehouseRetryExecutableOneException<Boolean, XPRC_CronRemovalException> wre =
          new WarehouseRetryExecutableOneException<Boolean, XPRC_CronRemovalException>() {

            public Boolean executeAndCommit(ODSConnection con) throws PersistenceLayerException,
                XPRC_CronRemovalException {
              synchronized (copyOfCronLikeTimer.getBlockingObject()) {
                con.executeAfterCommitFails(new Runnable() {

                  public void run() {
                    copyOfCronLikeTimer.unmarkAsNotToSchedule(id);
                    copyOfCronLikeTimer.recreateQueue();
                  }
                });

                con.executeAfterCommit(new Runnable() {

                  public void run() {
                    copyOfCronLikeTimer.unmarkAsNotToSchedule(id);
                  }
                });

                boolean existedInDefault = CronLikeOrderHelpers.delete(id, con);
                if (localConnection) {
                  con.commit();
                }
                return existedInDefault;
              }
            }

          };
      if (localConnection) {
        return WarehouseRetryExecutor.buildUserInteractionExecutor().storable(CronLikeOrder.class).execute(wre);
      } else {
        return wre.executeAndCommit(con);
      }
    } else {
      return false;
    }
  }


  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(final Long id, final String label, DestinationKey destination,
                                           final GeneralXynaObject payload, final Long firstStartupTime,
                                           final Long interval, final Boolean enabled, final OnErrorAction onError)
      throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTime, Constants.DEFAULT_TIMEZONE, interval, null,
                               false, enabled, onError, null, null, null, null);
  }

  public CronLikeOrder modifyCronLikeOrder(final Long id, final String label, final DestinationKey destination,
                                           final GeneralXynaObject payload, final Long firstStartupTime,
                                           final String timeZoneID, final Long interval, final Boolean useDST,
                                           final Boolean enabled, final OnErrorAction onError, final String cloCustom0,
                                           final String cloCustom1, final String cloCustom2, final String cloCustom3)
                                                           throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload,
                               firstStartupTime, timeZoneID, interval, null,
                               useDST, enabled, onError, cloCustom0,
                               cloCustom1, cloCustom2, cloCustom3);
  }

  public CronLikeOrder modifyTimeControlledOrder(final Long id, final CronLikeOrderCreationParameter clocp)
                                                           throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, clocp.getLabel(), clocp.getDestinationKey(), clocp.getInputPayload(),
                               clocp.getStartTime(), clocp.getTimeZoneID(), clocp.getInterval(), clocp.getCalendarDefinition(),
                               clocp.getConsiderDaylightSaving(), clocp.isEnabled(), clocp.getOnError(), clocp.getCronLikeOrderCustom0(),
                               clocp.getCronLikeOrderCustom1(), clocp.getCronLikeOrderCustom2(), clocp.getCronLikeOrderCustom3());
  }

  /**
   * Expects firstStartupTime to be offset by time difference defined in timeZoneID
   */
  public CronLikeOrder modifyCronLikeOrder(final Long id, final String label, final DestinationKey destination,
                                           final GeneralXynaObject payload, final Long firstStartupTime,
                                           final String timeZoneID, final Long interval, final String calendarDefinition, final Boolean useDST,
                                           final Boolean enabled, final OnErrorAction onError, final String cloCustom0,
                                           final String cloCustom1, final String cloCustom2, final String cloCustom3)
      throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    CronLikeOrder result = null;
    
    
    //convert firstStartupTime
    Long convertedFirstStartupTime = convertStartTimeUsingTimeZone(timeZoneID, firstStartupTime);
    
    WarehouseRetryExecutable<CronLikeOrder, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException> wre = new WarehouseRetryExecutable<CronLikeOrder, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException>() {

      public CronLikeOrder executeAndCommit(ODSConnection internalConnection) throws PersistenceLayerException,
                      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
        // query, check binding
        try {

          CronLikeOrder clo = CronLikeOrderHelpers.find(id, internalConnection);
          // if locally bound just super
          if (isResponsibleForBinding(clo.getBinding())) {
                clo =
                    modifyCronLikeOrder(id, label, destination, payload, convertedFirstStartupTime, timeZoneID, interval, calendarDefinition, useDST,
                                        enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, clo,
                                        internalConnection);
            internalConnection.commit();
            return clo;
          } else if (!storableClusterContext.getClusterState().isDisconnected()) { // else forward to corresponding
            try {
              initiateRemoteModifyCronLikeOrder(id, label, destination, payload, convertedFirstStartupTime, timeZoneID, interval, calendarDefinition, useDST, enabled,
                                                onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, clo.getBinding());
            } catch (RemoteException e) {
              throw new XPRC_CronLikeOrderStorageException(id, e);
            } catch (RMIConnectionFailureException e) {
              throw new XPRC_CronLikeOrderStorageException(id, e);
            }
            // read again to return modified values
            clo = CronLikeOrderHelpers.find(id, internalConnection);
            internalConnection.commit();
            return clo;
          } else {
            throw new XPRC_CronLikeOrderStorageException(id);
          }
        } catch (PersistenceLayerException e) {
          throw new XPRC_CronLikeOrderStorageException(id, e);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new XPRC_CronLikeOrderStorageException(id, e);
        }
      }
    };

    try {
      result = WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return result;
  }


  public CronLikeOrder modifyCronLikeOrder(final Long id, final String label, DestinationKey destination,
                                           final GeneralXynaObject payload,
                                           final Calendar firstStartupTimeWithTimeZone, final Long interval,
                                           final Boolean useDST, final Boolean enabled, final OnErrorAction onError,
                                           final String cloCustom0, final String cloCustom1, final String cloCustom2,
                                           final String cloCustom3) throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone.getTimeInMillis(),
                               firstStartupTimeWithTimeZone.getTimeZone().getID(), interval, null, useDST, enabled, onError,
                               cloCustom0, cloCustom1, cloCustom2, cloCustom3);
  }
  

  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(final Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Long firstStartupTime, Long interval, Boolean enabled,
                                           OnErrorAction onError, final CronLikeOrder clo, ODSConnection con)
      throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTime, Constants.DEFAULT_TIMEZONE, interval, null,
                               false, enabled, onError, null, null, null, null, clo, con);
  }


  public CronLikeOrder modifyCronLikeOrder(final Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Long firstStartupTime, String timeZoneID, Long interval, String calendarDefinition,
                                           Boolean useDST, Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3,
                                           final CronLikeOrder clo, ODSConnection con)
      throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {

    //überprüfen, dass die (neue) Application nicht im Zustand AUDIT_MODE ist
    if (destination != null) {
      checkApplicationState(destination);
    }
    
    final CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;

    if (!schedulingStopped) {
      synchronized (copyOfCronLikeTimer.getBlockingObject()) {
        copyOfCronLikeTimer.markAsNotToScheduleAndRemoveFromQueue(id);
        
        con.executeAfterCommitFails(new Runnable() {

          public void run() {
            copyOfCronLikeTimer.tryAddNewOrder(clo);
          }
        });

        try {
          try {
            CronLikeOrderHelpers.selectForUpdate(clo, con);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // TODO kann das nicht durch races passieren?
            throw new RuntimeException(e);
          }
          clo.update(label, destination, payload, firstStartupTime, timeZoneID, interval, calendarDefinition, useDST, enabled, onError,
                     cloCustom0, cloCustom1, cloCustom2, cloCustom3, con, true);
          final CronLikeOrder updatedClo = clo;
          con.executeAfterCommit(new Runnable() {

            public void run() {
              copyOfCronLikeTimer.tryAddNewOrder(updatedClo);
            }
          });
        } catch (PersistenceLayerException e) {
          throw new XPRC_CronLikeOrderStorageException(clo.getId(), e);
        }
      }
    }
    
    //Application auf RUNNING setzen
    if (enabled != null && enabled) {
      setApplicationRunning(clo);
    }
    
    return clo;

  }


  public CronLikeOrder modifyCronLikeOrder(final Long id, String label, DestinationKey destination, GeneralXynaObject payload,
                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3,
                                           final CronLikeOrder clo, ODSConnection con)
      throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    return modifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone.getTimeInMillis(),
                               firstStartupTimeWithTimeZone.getTimeZone().getID(), interval, null, useDST, enabled, onError,
                               cloCustom0, cloCustom1, cloCustom2, cloCustom3, clo, con);
  }


  private CronLikeOrder modifyCronLikeOrderLocaly(final Long id, final String label, final DestinationKey destination,
                                                  final GeneralXynaObject payload, final Long firstStartupTime, final String timeZoneID,
                                                  final Long interval, final String calendarDefinition, final Boolean useDST, final Boolean enabled,
                                                  final OnErrorAction onError, final String cloCustom0, final String cloCustom1, final String cloCustom2, final String cloCustom3)
                  throws XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    CronLikeOrder result = null;
    WarehouseRetryExecutable<CronLikeOrder, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException> wre = new WarehouseRetryExecutable<CronLikeOrder, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException>() {

      public CronLikeOrder executeAndCommit(ODSConnection internalConnection) throws PersistenceLayerException,
                      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
        CronLikeOrder requested = null;
        internalConnection.ensurePersistenceLayerConnectivity(CronLikeOrder.class);

        synchronized (cronLikeTimer.getBlockingObject()) {
          try {
            requested = CronLikeOrderHelpers.find(id, internalConnection);
          } catch (PersistenceLayerException e) {
            throw new XPRC_CronLikeOrderStorageException(id, e);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new XPRC_CronLikeOrderStorageException(id, e);
          }

          if (requested == null) {
            return null;
          }

          CronLikeOrder result = modifyCronLikeOrder(id, label, destination, payload, firstStartupTime, timeZoneID, interval, calendarDefinition, useDST,
                                                     enabled, onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, requested, internalConnection);
          internalConnection.commit();
          return result;
        }
      }
    };


    try {
      result = WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    return result;
  }


  @Deprecated
  public void initiateRemoteModifyCronLikeOrder(final Long id, final String label, final DestinationKey destination,
                                                final GeneralXynaObject payload, final Long firstStartupTime,
                                                final Long interval, final Boolean enabled,
                                                final OnErrorAction onError, int binding) throws RemoteException,
      XPRC_CronLikeOrderStorageException, RMIConnectionFailureException, XPRC_InvalidCronLikeOrderParametersException {
    initiateRemoteModifyCronLikeOrder(id, label, destination, payload, firstStartupTime, Constants.DEFAULT_TIMEZONE,
                                      interval, null, false, enabled, onError, null, null, null, null, binding);
  }
  

  public void initiateRemoteModifyCronLikeOrder(final Long id, final String label, final DestinationKey destination,
                                                final GeneralXynaObject payload, final Long firstStartupTime,
                                                final String timeZoneID, final Long interval,
                                                final String calendarDefinition, final Boolean useDST,
                                                final Boolean enabled, final OnErrorAction onError,
                                                final String cloCustom0, final String cloCustom1,
                                                final String cloCustom2, final String cloCustom3, int binding)
      throws RemoteException, XPRC_CronLikeOrderStorageException, RMIConnectionFailureException, XPRC_InvalidCronLikeOrderParametersException {
    if (isClustered()) {
      try {
        RMIClusterProviderTools
                        .execute(((RMIClusterProvider) rmiClusterContext.getClusterInstance()),
                                 clusteredInterfaceId,
                                 new RMIRunnableNoResult<ClusteredCronLikeSchedulerInterface, XPRC_CronLikeSchedulerException>() {

                                   public void execute(ClusteredCronLikeSchedulerInterface clusteredInterface)
                                                   throws XPRC_CronLikeSchedulerException, RemoteException {

                                     String payloadXML = null;
                                     long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
                                     if(payload != null) {
                                       payloadXML = payload.toXml();
                                       if(payload.getClass().getClassLoader() instanceof ClassLoaderBase) {
                                         revision = ((ClassLoaderBase)payload.getClass().getClassLoader()).getRevision();
                                       }
                                     }
                                     
                         clusteredInterface.modifyCronLikeOrderRemotely(id, label, destination, payloadXML, revision,
                                                                        firstStartupTime, timeZoneID, interval, calendarDefinition, useDST,
                                                                        enabled, onError, cloCustom0, cloCustom1,
                                                                        cloCustom2, cloCustom3);
                                   }

                                 });
      } catch (XPRC_CronLikeSchedulerException e) {
        if (e instanceof XPRC_InvalidCronLikeOrderParametersException) {
          throw (XPRC_InvalidCronLikeOrderParametersException) e;
        } else if (e instanceof XPRC_CronLikeOrderStorageException) {
          throw (XPRC_CronLikeOrderStorageException) e;
        } else {
          throw new RuntimeException( "Unexpected exception", e);
        }
      } catch (InvalidIDException e) {
        handleInvalidIDException(e);
      }
    }
  }


  public void initiateRemoteModifyCronLikeOrder(final Long id, final String label, final DestinationKey destination,
                                                final GeneralXynaObject payload,
                                                final Calendar firstStartupTimeWithTimeZone, final Long interval,
                                                final String calendarDefinition, final Boolean useDST, final Boolean enabled,
                                                final OnErrorAction onError, final String cloCustom0,
                                                final String cloCustom1, final String cloCustom2,
                                                final String cloCustom3, int binding) throws RemoteException,
      XPRC_CronLikeOrderStorageException, RMIConnectionFailureException, XPRC_InvalidCronLikeOrderParametersException {
    initiateRemoteModifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone.getTimeInMillis(),
                                      firstStartupTimeWithTimeZone.getTimeZone().getID(), interval, calendarDefinition, useDST, enabled,
                                      onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3, binding);
  }


  private void handleInvalidIDException(InvalidIDException e) {
    // der clustered cronls entfernt nie das registrierte remote interface => das kann nicht passieren.
    throw new RuntimeException(e);
  }


  public void initiateRemoteRemoveCronLikeOrder(final Long id) throws RemoteException, RMIConnectionFailureException {
    if (isClustered()) {
      try {
        RMIClusterProviderTools
                        .executeNoException(((RMIClusterProvider) rmiClusterContext.getClusterInstance()), 
                                            clusteredInterfaceId,
                                            new RMIRunnableNoResultNoException<ClusteredCronLikeSchedulerInterface>() {

                                              public void execute(ClusteredCronLikeSchedulerInterface clusteredInterface)
                                                              throws RemoteException {

                                                clusteredInterface.removeCronLikeOrderRemotely(id);
                                              }
                                            });
      } catch (InvalidIDException e) {
        handleInvalidIDException(e);
      }
    }
  }

  
  @Deprecated
  public void modifyCronLikeOrderRemotely(Long id, String label, String ordertype, String payloadXML, Long revision,
                                          Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws RemoteException, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    modifyCronLikeOrderRemotely(id, label, new DestinationKey(ordertype), payloadXML, revision, firstStartupTime,
                                Constants.DEFAULT_TIMEZONE, interval, null, false, enabled, onError, null, null, null, null);
  }


  public void modifyCronLikeOrderRemotely(Long id, String label, DestinationKey destination, String payloadXML, Long revision,
                                          Long firstStartupTime, String timeZoneID, Long interval, String calendarDefinition, Boolean useDST,
                                          Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                          String cloCustom2, String cloCustom3) throws RemoteException,
      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {

    CronLikeOrder clo;

    try {
      clo = CronLikeOrderHelpers.find(id, null);
    } catch (PersistenceLayerException e) {
      // weiterwerfen ... zum Modifizieren muss Entry im 2-Knotenfall hier sein
      throw new XPRC_CronLikeOrderStorageException(id, e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // weiterwerfen ... zum Modifizieren muss Entry im 2-Knotenfall hier sein
      throw new XPRC_CronLikeOrderStorageException(id, e);
    }

    if (clo.getBinding() == currentOwnBinding) {
      
      GeneralXynaObject payload;
      try {
        payload = XynaObject.generalFromXml(payloadXML, revision);
      } catch (XynaException e) {
        throw new RemoteException("Could not create payload from xml.", e);
      }
      modifyCronLikeOrderLocaly(id, label, destination, payload, firstStartupTime, timeZoneID, interval, calendarDefinition, useDST, enabled,
                                onError, cloCustom0, cloCustom1, cloCustom2, cloCustom3);
    }

  }


  public void modifyCronLikeOrderRemotely(Long id, String label, DestinationKey destination, String payloadXML, Long revision,
                                          Calendar firstStartupTimeWithTimeZone, Long interval, String calendarDefinition, Boolean useDST,
                                          Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                          String cloCustom2, String cloCustom3) throws RemoteException,
      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException {
    modifyCronLikeOrderRemotely(id, label, destination, payloadXML, revision,
                                firstStartupTimeWithTimeZone.getTimeInMillis(), firstStartupTimeWithTimeZone
                                    .getTimeZone().getID(), interval, calendarDefinition, useDST, enabled, onError, cloCustom0, cloCustom1,
                                cloCustom2, cloCustom3);
  }


  public void removeCronLikeOrderRemotely(Long id) throws RemoteException {

    CronLikeOrder clo;

    try {
      clo = CronLikeOrderHelpers.find(id, null);
    } catch (PersistenceLayerException e) {
      // weiterwerfen ...
      throw new RemoteException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // kann ignoriert werden: wenn Cronlikeorder gelöscht werden soll, aber gar nicht da ist, ist es wohl schon
      // gelöscht
      return;
    }

    if (clo.getBinding() == currentOwnBinding) {
      try {
        removeCronLikeOrderWithRetryIfConnectionIsBroken(id);
      } catch (XPRC_CronRemovalException e) {
        throw new RemoteException(e.getMessage(), e);
      }
    }
  }
  
  public CronLikeOrderSearchResult searchCronLikeOrders(CronLikeOrderSelectImpl select, int maxRows) throws PersistenceLayerException {
    try {
      return searchInternally(select, maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      cursorQueryCache.clear();
      return searchInternally(select, maxRows);
    }
  }
  
  
  private CronLikeOrderSearchResult searchInternally(CronLikeOrderSelectImpl select, int maxRows) throws PersistenceLayerException {
    String selectString;
    ResultSetReader<CronLikeOrder> reader = select.getReader();
    String selectCountString;
    Parameter paras = select.getParameter();
    try {
      selectString = select.getSelectString();
      selectCountString = select.getSelectCountString();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("Problem with select statement: " + e.getMessage(), e);
    }
    int countAll = 0;
    List<CronLikeOrderInformation> crons = new ArrayList<CronLikeOrderInformation>();

    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<CronLikeOrder> query = cursorQueryCache.getQueryFromCache(selectString, con, reader);
      List<CronLikeOrder> queryResult = con.query(query, paras, maxRows);
      for(CronLikeOrder clo : queryResult) {
        crons.add(new CronLikeOrderInformation(clo));
      }
      if (crons.size() >= maxRows) {
        PreparedQuery<? extends OrderCount> queryCount = cursorQueryCache.getQueryFromCache(selectCountString, con,
                                                                                 OrderCount.getCountReader());
        OrderCount count = con.queryOneRow(queryCount, paras);
        countAll = count.getCount();
      } else {
        countAll = crons.size();
      }
    } finally {
      con.closeConnection();
    }
    
    for(CronLikeOrderInformation cron : crons) {
      cron.setStartTime(convertStartTimeBackToUTC(cron));
    }
    
    return new CronLikeOrderSearchResult(crons, countAll);
  }

  private int countCronLikeOrders(ODSConnection con) throws PersistenceLayerException {

    switch (storableClusterContext.getClusterState()) {
      case DISCONNECTED_MASTER :
      case NO_CLUSTER :
        return CronLikeOrderHelpers.countCronLikeOrders(con);

      case DISCONNECTED_SLAVE :
        return 0;

      case CONNECTED :
      case SINGLE :
      case DISCONNECTED :
      default :
        return CronLikeOrderHelpers.countCronLikeOrdersForBinding(currentOwnBinding, con);
    }

  }
  
  public FactoryWarehouseCursor<CronLikeOrder> getCursorForCronLikeOrdersWithOtherBinding(int binding, ODSConnection con, int blockSize)
      throws PersistenceLayerException {
    
    return new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetCronLikeOrdersWithDifferentBinding, new Parameter(binding),
        new CronLikeOrder.CronLikeOrderReader(), blockSize,
        cursorQueryCache);
  }
  
      
  public FactoryWarehouseCursor<CronLikeOrder> getCursorForRelevantCronLikeOrders(ODSConnection con, int blockSize)
      throws PersistenceLayerException {
    
    if (!isClustered() && XynaFactory.getInstance().isShuttingDown()) {
      logger.warn("Cannot load orders when shutting down, returning empty list");
      return null;
    }
    
    return getCursorForRelevantCronLikeOrders(con, blockSize, new CronLikeOrder.CronLikeOrderReader());
  }

  public FactoryWarehouseCursor<CronLikeOrder> getCursorForRelevantCronLikeOrders(ODSConnection con, int blockSize, ResultSetReader<CronLikeOrder> reader)
                  throws PersistenceLayerException {

    switch (storableClusterContext.getClusterState()) {
      case NO_CLUSTER :
      case DISCONNECTED_MASTER :
        return new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetNextCronLikeOrders, new Parameter(),
                                                         reader, blockSize, cursorQueryCache);

      case DISCONNECTED_SLAVE :
        return null;

      case CONNECTED :
      case SINGLE :
      case DISCONNECTED :
      default :
        return new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetNextCronLikeOrdersForBinding,
                                                         new Parameter(currentOwnBinding),
                                                         reader, blockSize, cursorQueryCache);
    }

  }
  

  private FactoryWarehouseCursor<CronLikeOrder> getCursorForRelevantCronLikeOrdersForQueue(ODSConnection con,
                                                                                           int blockSize)
      throws PersistenceLayerException {

    if (!isClustered() && XynaFactory.getInstance().isShuttingDown()) {
      logger.warn("Cannot load orders when shutting down, returning empty list");
      return null;
    }

    switch (storableClusterContext.getClusterState()) {
      case NO_CLUSTER :
      case DISCONNECTED_MASTER :
        return new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetNextEnabledCronLikeOrders,
                                                         new Parameter(Boolean.TRUE),
                                                         new CronLikeOrder.CronLikeOrderReader(), blockSize,
                                                         cursorQueryCache);

      case DISCONNECTED_SLAVE :
        return null;

      case CONNECTED :
      case SINGLE :
      case DISCONNECTED :
      default :
        return new FactoryWarehouseCursor<CronLikeOrder>(con,
                                                         CronLikeOrderHelpers.sqlGetNextEnabledCronLikeOrdersForBinding,
                                                         new Parameter(currentOwnBinding, Boolean.TRUE),
                                                         new CronLikeOrder.CronLikeOrderReader(), blockSize,
                                                         cursorQueryCache);
    }

  }


  public boolean isResponsibleForBinding(int binding) {
    switch (storableClusterContext.getClusterState()) {
      case DISCONNECTED_MASTER :
      case NO_CLUSTER :
      case SINGLE :
        return true;

      case DISCONNECTED_SLAVE :
        return false;

      case CONNECTED :
      case DISCONNECTED :
      default :
        return (currentOwnBinding == binding);
    }
  }

  
  public void tryAddNewOrders(List<CronLikeOrder> orders) {
    CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if(!schedulingStopped && !orders.isEmpty()) {
      copyOfCronLikeTimer.tryAddNewOrders(orders);
    }
  }
  
  public void tryAddNewOrder(CronLikeOrder order) {
    CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if(!schedulingStopped) {
      copyOfCronLikeTimer.tryAddNewOrder(order);
    }
  }
  
  public void recreateQueue() {
    CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if(!schedulingStopped) {
      copyOfCronLikeTimer.recreateQueue();
    }
  }
  
  public void markAsNotToScheduleAndRemoveFromQueue(Long orderId) {
    CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if(!schedulingStopped) {
      copyOfCronLikeTimer.markAsNotToScheduleAndRemoveFromQueue(orderId);
    }
  }
  
  public void unmarkAsNotToSchedule(Long orderId) {
    CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if(!schedulingStopped) {
      copyOfCronLikeTimer.unmarkAsNotToSchedule(orderId);
    }
  }

  /*
   * wird benutzt von CreateDeliveryItem, RMI, CLI ....
   */
  public Collection<CronLikeOrder> getAllCronLikeOrdersFlat(long maxRows) throws XPRC_CronLikeSchedulerException {
    try {
      return CronLikeOrderHelpers.findAll(maxRows);
    } catch (PersistenceLayerException e) {
      throw new XPRC_CronLikeSchedulerException(e);
    }
  }
  

  public Collection<CronLikeOrder> getAllCronLikeOrdersForRootOrderIdFlat(Long rootOrderId, ODSConnection con) throws PersistenceLayerException {
      return CronLikeOrderHelpers.findAllWithRootOrderId(currentOwnBinding, rootOrderId, con);
  }


  /**
   * Get all orders currently store in the queue
   */
  public List<CronLikeOrder> getAllQueuedOrders() {
    return cronLikeTimer.getQueueList();
  }
  
  
  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrderInformation(long maxRows)
                  throws XPRC_CronLikeSchedulerException {

    Collection<CronLikeOrder> orders = getAllCronLikeOrdersFlat(maxRows);
    Map<Long, CronLikeOrderInformation> result = new TreeMap<Long, CronLikeOrderInformation>();
    if (orders != null && !orders.isEmpty()) {
      for (CronLikeOrder order : orders) {
        if (hideInternalOrders) {
          if (!OrdertypeManagement.internalOrdertypes.contains(order.getCreationParameters().getOrderType())) {
            result.put(order.getId(), new CronLikeOrderInformation(order));
          }
        } else {
          result.put(order.getId(), new CronLikeOrderInformation(order));
        }
      }
    }

    return result;

  }


  public CronLikeOrderInformation getOrderInformation(Long id) throws XPRC_CronLikeOrderStorageException {
    CronLikeOrder requested;

    try {
      requested = CronLikeOrderHelpers.find(id, null);
    } catch (PersistenceLayerException e) {
      throw new XPRC_CronLikeOrderStorageException(id, e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
    return new CronLikeOrderInformation(requested);

  }


  private boolean isOutdatedSingleExecutionOrder(CronLikeOrder order) {
    return order.isSingleExecution() && order.getStartTime() < System.currentTimeMillis();
  }


  protected void readNextFromPersistenceLayer(ODSConnection con, int readsize)
                  throws PersistenceLayerException {

    if (cronLikeTimer == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("schedulerRunnable not available! - it makes no sense to read new cronLikeOrders");
      }
      return;
    }

    final CronLikeTimer copyOfCronLikeTimer = cronLikeTimer;
    if (!schedulingStopped) {
      do {
        //Da gleichzeitig andere Threads im Acknowledge ankommen können, kann folgendes vorkommen:
        //Der Cron wird von diesem Thread noch in der Datenbank (im alten Zustand) gefunden und anschließend
        //entfernt der andere Thread ihn aus notToSchedule. Dann würde er bei tryAddNewOrderWithoutUnmarkOrder
        //aber fälschlicherweise wieder in die Queue eingefügt werden.
        //Daher werden ab nun alle Aufträge, die aus notToSchedule ausgetragen werden in einem neuen 
        //Set "deletedOrModified" gesammelt und nur in die Queue hinzugefügt, wenn sie in keinem
        //der beiden Sets vorhanden sind.
        //Außerdem wird das cleared-FLag auf false gesetzt.
        copyOfCronLikeTimer.prepareReadNext();
        
        //Crons aus der DB auslesen
        FactoryWarehouseCursor<CronLikeOrder> cursor = getCursorForRelevantCronLikeOrdersForQueue(con, readsize);
        if(cursor == null) {
          return;
        }

        try {
          List<CronLikeOrder> cursorList = cursor.getRemainingCacheOrNextIfEmpty();
          int added = 0;
          
          logger.debug( "read " + cursorList.size() + " CronLikeOrders and adding them into the queue" );
          
          while(added < readsize && !cursorList.isEmpty() && !copyOfCronLikeTimer.isCleared()) {
            Iterator<CronLikeOrder> iter = cursorList.iterator();
            while(iter.hasNext() && added < readsize) {
              CronLikeOrder order = iter.next();
              if (order.isEnabled()) {
                synchronized (copyOfCronLikeTimer.getBlockingObject()) {
                  if (copyOfCronLikeTimer.isCleared()) {
                    //falls ein anderer Thread die Queue geleert hat, muss wieder von
                    //vorne angefangen werden und erneut alle Crons aus der DB ausgelesen werden
                    if (logger.isDebugEnabled()) {
                      logger.debug("queue cleared by another thread -> read again from persistence layer");
                    }
                    break;
                  }
                  //Cron in die Queue einfügen
                  if (copyOfCronLikeTimer.tryAddNewOrderWithoutUnmarkOrder(order, true)) {
                    added++;
                  } else {
                    copyOfCronLikeTimer.setReadAllFromDBFlag(false);
                  }
                }
              }
            }
            //weitere Crons holen
            cursorList = cursor.getRemainingCacheOrNextIfEmpty();
          }
          
          //das Einsammeln der gelöschten und geänderten Crons kann nun gestoppt werden
          copyOfCronLikeTimer.finishReadNext();

          if (logger.isTraceEnabled()) {
            logger.trace( "added " + added + " items into the queue" );
          }
          
          synchronized (copyOfCronLikeTimer.getBlockingObject()) {
            //falls genausoviele Crons aus der DB in die Queue eingefügt wurden, wie
            //reinpassen, dann sind wahrscheinlich noch mehr Crons in der DB vorhanden,
            //daher das flag auf false setzen
            if(added == readsize) {
              copyOfCronLikeTimer.setReadAllFromDBFlag(false);
            }
          }
        } finally {
          // Cursor wird nur bei Connection-close geschlossen. Da aber eine dedizierte Connection verwendet wird, kann es
          // bis zum Server-Shutdown dauern, bis Connection geschlossen wird. Also schließen wir Cursor schonmal hier.
          cursor.close();
        }
      } while(copyOfCronLikeTimer.isCleared()); //nochmal von vorne beginnen, weil die Queue geleert wurde
    }
  }

  // ##### Private Methoden zur Initalisierung #####
  /**
   * Copy all cron like orders from one connection type to another.
   */
  private void copyCronLikeOrdersBetweenLayers(ODSConnectionType sourceType, ODSConnectionType targetType)
  throws PersistenceLayerException {
    boolean areHistoryAndDefaultTheSame = ods.isSamePhysicalTable(CronLikeOrder.TABLE_NAME, sourceType, targetType);
    CronLikeOrderFailSafeReader reader = null;

    if (!areHistoryAndDefaultTheSame) {
      logger.debug("Copying cron like order information from " + sourceType + " to " + targetType);
      try {
        CronLikeOrder.makeReaderFailSafe( true );
        reader = (CronLikeOrderFailSafeReader) new CronLikeOrder().getReader();
        ods.copy(CronLikeOrder.class, sourceType, targetType);
      } catch (XNWH_RetryTransactionException e) {
        // this should not happen as in cluster mode both connection types have be the same
        throw new RuntimeException(e);
      } finally {
        if ( (reader != null) && !reader.getFailedIds().isEmpty()) {
          Iterator<Long> iter = reader.getFailedIds().iterator();
          StringBuilder str = new StringBuilder("Failed to deserialize cron like orders with the following ids (unable to disable the cron like orders): ");
          
          while(iter.hasNext()) {
            str.append(iter.next());

            if(iter.hasNext()) {
              str.append(", ");
            }
          }
          
          // get all those cron like orders that could not have been read from the source persistencelayer
          List<CronLikeOrder> toBeDeleted = new ArrayList<CronLikeOrder>();

          for (Long failedId : reader.getFailedIds()) {
            toBeDeleted.add(new CronLikeOrder(failedId));
          }

          CronLikeOrder.makeReaderFailSafe( false );

          long persistenceLayerInstanceID = ods.getPersistenceLayerInstanceId( sourceType, CronLikeOrder.class );
          long persistenceLayerID = 0L;
          String persistenceLayerType = "UNKNOWN";
          
          for (PersistenceLayerInstanceBean instanceBean : ods.getPersistenceLayerInstances()) {
            if (instanceBean.getPersistenceLayerInstanceID() == persistenceLayerInstanceID) {
              persistenceLayerType = instanceBean.getPersistenceLayerInstance().getInformation();
              persistenceLayerID = instanceBean.getPersistenceLayerID();
              break;
            }
          }
          
          logger.error(str.toString());
          logger.error( "Reading these cron like orders will be retried upon the next reboot of the Xyna Factory." );
          logger.error( "You need to manually remove those cron like orders from the persistencelayer instance : " + Storable.getPersistable(CronLikeOrder.class).tableName() + " " + sourceType.toString() + " PL-ID: " + persistenceLayerInstanceID + " (PL: " + persistenceLayerID + " = " + persistenceLayerType + ")" );
          
          // remove all those cron like orders that have been written as dummies to the target persistencelayer
          ODSConnection con = ods.openConnection(targetType);
          con.delete(toBeDeleted);
          con.commit();
          con.closeConnection();
          con = null;
        }
      }
    }

    // TODO delete all from default. beim init kann man dann daran erkennen, ob shutdown ausgeführt wurde, oder ob der
    // server gekillt wurde
  }


  private void cleanupCronLikeOrdersAtStartUp(final List<CronLikeOrder> orders, ODSConnection con) throws XPRC_CronRemovalException, PersistenceLayerException {
    Iterator<CronLikeOrder> iter = orders.iterator();
    while (iter.hasNext()) {
      final CronLikeOrder order = iter.next();
      if(order.getCreationParameters() == null) {
        // offensichtlich konnte die CronLikeOrder nicht richtig geladen werden ... sie wird hier erstmal ignoriert und im Aufrufer behandelt,
        iter.remove();
        continue;
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Found interrupted cron like order for " + order.getCreationParameters().getDestinationKey() + " (ID " + order
                        .getId() + "), checking parameters...");
      }

      if (order.getRemoveOnShutdown()) {
        logger.warn("Found cron like order that was supposed to be deleted on shutdown (ID " + order.getId() + "), expecting server crash. Removing order.");

        try {
          CronLikeOrderHelpers.delete(order.getId(), con);
        } catch (XNWH_RetryTransactionException e) {
          throw new XPRC_CronRemovalException(order.getId(), e);
        }
        iter.remove();
      } else {
        if (order.getNextExecution() == null) {
          if (logger.isInfoEnabled()) {
            logger.info("Found cron like order <" + order.getId() + "> that was supposed to have executed before (interrupted by crash or shutdown).");
          }
          order.setNextExecutionTime(System.currentTimeMillis());
        } else if (isOutdatedSingleExecutionOrder(order)) {
          order.setNextExecutionTime(System.currentTimeMillis());
          if (logger.isDebugEnabled()) {
            logger.debug("Cron like order <" + order.getId() + "> is an overdue single execution cron, trying to schedule.");
          }
        } else {
          if (order.getNextExecution() < System.currentTimeMillis()) {
            long nextExecBefore = order.getNextExecution();
            order.calculateNextFutureExecutionTime();
            if (logger.isDebugEnabled()) {
              logger.debug("Overriding starttime within the past ("
                  + Constants.defaultUTCSimpleDateFormat().format(new Date(nextExecBefore)) + ") to new value "
                  + Constants.defaultUTCSimpleDateFormat().format(new Date(order.getNextExecution())) + ".");
            }
          }

          if (logger.isDebugEnabled()) {
            logger.debug("Parameters of cron like order <" + order.getId() + "> are valid, restarting order.");
          }
        }
      }
    }
    
    CronLikeOrderHelpers.store(orders, con);
    con.commit();    
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> props = new ArrayList<String>();
    props.add(XynaProperty.HIDE_INTERNAL_ORDERS);
    return props;
  }


  public void propertyChanged() {
    String value = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.HIDE_INTERNAL_ORDERS);
    if (value == null || value.equals("")) {
      return;
    } else {
      hideInternalOrders = Boolean.parseBoolean(value);
    }
  }

  /**
   * @return gibt CLO oder null zurück, falls nicht gefunden
   */
  public CronLikeOrder getCronLikeOrder(ODSConnection con, Long id) throws PersistenceLayerException {
    CronLikeOrder clo = new CronLikeOrder(id);
    try {
      con.queryOneRow(clo);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
    return clo;
  }

  
  /**
   * alle crons, die dem ordertype, der revision und falls gewünscht dem lokalen binding entsprechen 
   */
  public FactoryWarehouseCursor<CronLikeOrder> getCursorForCronLikeOrders(ODSConnection con, int blockSize,
                                                                          Long revSource, String[] ordertypes,
                                                                          boolean allBindings)
      throws PersistenceLayerException {
    StringBuilder sql =
        new StringBuilder("SELECT * FROM " + CronLikeOrder.TABLE_NAME + " WHERE " + CronLikeOrder.COL_REVISION + " = ?");
    Parameter parameter = new Parameter(revSource);
    if (!allBindings) {
      sql.append(" AND " + CronLikeOrder.COL_BINDING + " = ?");
      parameter.add(new CronLikeOrder().getLocalBinding(con.getConnectionType()));
    }

    if (ordertypes != null) {
      InList inList;
      try {
        inList = new InList(ordertypes);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      sql.append(" AND ").append(inList.getSQL(CronLikeOrder.COL_ORDERTYPE));
      com.gip.xyna.utils.db.Parameter params = inList.getParams();
      for (int i = 0; i < params.size(); i++) {
        Object param = params.getParameter(i + 1); //fängt bei 1 an zu zählen
        parameter.add(param);
      }
    }
    sql.append(" ORDER BY " + CronLikeOrder.COL_ID);
    return new FactoryWarehouseCursor<CronLikeOrder>(con, sql.toString(), parameter,
                                                     new CronLikeOrder.CronLikeOrderReader(), blockSize,
                                                     cursorQueryCache);
  }


  public int countCronLikeOrders(long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      PreparedQuery<Integer> pq =
          con.prepareQuery(new Query<Integer>("select count(*) from " + CronLikeOrder.TABLE_NAME + " where " + CronLikeOrder.COL_REVISION
              + " = ? and " + CronLikeOrder.COL_BINDING + " = ?", new ResultSetReader<Integer>() {

            public Integer read(ResultSet rs) throws SQLException {
              return rs.getInt(1);
            }

          }));
      int cnt = con.queryOneRow(pq, new Parameter(revision, currentOwnBinding));
      return cnt;
    } finally {
      con.closeConnection();
    }
  }

  /**
   * Setzt den Status der Application der Cron Like Order auf Running
   */
  private void setApplicationRunning(CronLikeOrder clo){
    if (clo.isEnabled()) {
      RuntimeContext runtimeContext = clo.getRuntimeContext();
      if (runtimeContext instanceof Application) {
        ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                        .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        applicationManagement.changeApplicationState(runtimeContext.getName(), ((Application) runtimeContext).getVersionName(), ApplicationState.RUNNING);
      }
    }
  }
}
