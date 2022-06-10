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
package com.gip.xyna.xprc.xfractwfe;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ExceptionClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_WFClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.MIAbstractionLayer;
import com.gip.xyna.xprc.RedirectionXynaOrder;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_ErrorDuringReloadOfGeneratedObjects;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MassWorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.exceptions.XPRC_WorkflowProtectionModeViolationException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask;
import com.gip.xyna.xprc.xfractwfe.DeploymentProcess.DeploymentProcessState;
import com.gip.xyna.xprc.xfractwfe.DeploymentProcess.OrderFilterDeployment;
import com.gip.xyna.xprc.xfractwfe.OrderFilterAlgorithmsImpl.OrderFilter;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.FillingMode;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.AuditData.AuditReloader;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrdertypeBean;



/**
 * - bietet Hilfsfunktionen, um prüfen zu können, ob Workflows in Benutzung sind
 * - koordiniert die DeploymentProcesses
 * - jedes Deployment meldet sich mittels addDeployment an
 * - wenn ein DeploymentProcess bereits erzeugt wurde und dieser noch in der Prüfungsphase ist,
 *   wird das Deployment an diesen DeploymentProcess angehängt - im anderen Fall wird ein neuer (wartener)
 *   DeploymentProcess erzeugt
 */
public class DeploymentManagement {


  private static Logger logger = CentralFactoryLogging.getLogger(DeploymentManagement.class);

  private List<DeploymentProcess> deploymentProcessList = new ArrayList<DeploymentProcess>();

  private Map<GenerationBase.WorkflowProtectionMode, Set<WorkflowRevision>> affectedWorkflowsPerProtectionMode;

  private AtomicBoolean newDeploymentDuringPreDeployment = new AtomicBoolean(false);

  private Object bench = new Object();

  private ReentrantLock entranceLock = new ReentrantLock();

  private OrderAndDeploymentCounter orderCounter;

  private static volatile DeploymentManagement instance = null;

  private boolean violationThisRun;

  private ODS ods;
  
  private PreparedQueryCache queryCache = new PreparedQueryCache(3600 * 1000, 3600 * 1000);

  private String getAllCronLikeOrdersIgnoringSerialVersionUId = "select * from " + CronLikeOrder.TABLE_NAME
      + " where " + CronLikeOrder.COL_BINDING + "=?";
  
  private String selectAllLocalBackupIds = "select "+OrderInstanceBackup.COL_ID +" from " + OrderInstanceBackup.TABLE_NAME
      + " where " + OrderInstanceBackup.COL_BINDING + "=?";

  private String selectOrderInstanceBackup = "select * from " + OrderInstanceBackup.TABLE_NAME
      + " where " + OrderInstanceBackup.COL_ID + "=?";

  
  public enum DispatcherType {
    Planning() {
      public DestinationValue getDestinationValue(DestinationKey destinationKey) throws XPRC_DESTINATION_NOT_FOUND {
        return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
            getXynaPlanning().getPlanningDispatcher().getDestination(destinationKey);
      }
      public Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses() {
        return XynaFactory.getInstance().getProcessing().getWorkflowEngine().getPlanningProcessor().getOrdersOfRunningProcesses();
      }
    },
    Execution() {
      public DestinationValue getDestinationValue(DestinationKey destinationKey) throws XPRC_DESTINATION_NOT_FOUND {
        return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
            getXynaExecution().getExecutionDestination(destinationKey);
      }
      public Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses() {
        return XynaFactory.getInstance().getProcessing().getWorkflowEngine().getExecutionProcessor().getOrdersOfRunningProcesses();
      }
    },
    Cleanup() {
      public DestinationValue getDestinationValue(DestinationKey destinationKey) throws XPRC_DESTINATION_NOT_FOUND {
        return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
            getXynaCleanup().getCleanupEngineDispatcher().getDestination(destinationKey);
      }           
      public Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses() {
        return XynaFactory.getInstance().getProcessing().getWorkflowEngine().getCleanupProcessor().getOrdersOfRunningProcesses();
      }
    };

    public abstract DestinationValue getDestinationValue(DestinationKey destinationKey) throws XPRC_DESTINATION_NOT_FOUND;

    public abstract Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses();

  }
  
  public static class WorkflowRevision {

    public String wfFqClassName;
    public Long revision; //revision, wo der workflow definiert ist

    public WorkflowRevision(String wfFqClassName, Long revision) {
      // FIXME : this is a temporary solution until we find the source of the null. We don't use assertions as we want to not disturb projects.
      if ( wfFqClassName == null ) {
        NullPointerException npe = new NullPointerException();
        logger.warn( "WorkflowName must not be null", npe);
      }
      
      if ( revision == null ) {
        NullPointerException npe = new NullPointerException();
        logger.warn( "Revision must not be null", npe);
      }
      
      this.wfFqClassName = wfFqClassName;
      this.revision = revision;
    }


    @Override
    public int hashCode() {
      return wfFqClassName.hashCode() ^ revision.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
      if (obj instanceof WorkflowRevision) {
        WorkflowRevision o = (WorkflowRevision) obj;
        if (o != null) {
          if (wfFqClassName.equals(o.wfFqClassName) && revision.equals(o.revision)) {
            return true;
          }
        }
      }
      return false;
    }
    
    public String toString() {
      return wfFqClassName;
    }
    
    public static WorkflowRevision construct(String fqName, Long revision) throws XPRC_DESTINATION_NOT_FOUND {
      return new WorkflowRevision(fqName, revision);
    }
    
    public static Set<WorkflowRevision> construct(DispatcherType dispatcherType, DestinationKey destinationKey) throws XPRC_DESTINATION_NOT_FOUND {
      DestinationValue dv = dispatcherType.getDestinationValue(destinationKey);
      Set<Long> allRevisions = dv.resolveAllRevisions(destinationKey);
      Set<WorkflowRevision> wfRevs = new HashSet<WorkflowRevision>();
      for (Long aRevision : allRevisions) {
        wfRevs.add(new WorkflowRevision(dv.getFQName(), aRevision));
      }
      return wfRevs;
    }

  }


  protected DeploymentManagement(boolean forTesting) {
    // Konstruktor wird nur im Testfall aufgerufen ... nothing to do
  }


  protected DeploymentManagement() {
    orderCounter = new OrderAndDeploymentCounter();
    ods = ODSImpl.getInstance();
    // This is a quick workaround for bug: 11210
    try {
      ods.registerStorable(OrderInstanceBackup.class);
    } catch (PersistenceLayerException e) {
      logger.warn("Error encountered while trying to register OrderInstanceBackup, server start might"
          + " fail to start if triggers with additional dependencies were deployed and orderBackup-Table"
          + " is configured to a table depending on registration");
    }
  }


  public static DeploymentManagement getInstance() {
    if (instance == null) {
      synchronized (DeploymentManagement.class) {
        if (instance == null)
          instance = new DeploymentManagement();
      }
    }
    return instance;
  }


  public static void setInstance(DeploymentManagement newInstance) { //for testCases
    instance = newInstance;
  }


  public void unlockEntrance(AtomicBoolean entranceLockLocked) {
    entranceLock.unlock();
    entranceLockLocked.set(false);
  }


  private Set<WorkflowRevision> getAllProtectedWorkflows() {
    // TODO cache it? is only called during reload and is quite costly
    Set<WorkflowRevision> allSet = new HashSet<WorkflowRevision>();
    for (Set<WorkflowRevision> set : affectedWorkflowsPerProtectionMode.values()) {
      allSet.addAll(set);
    }
    return allSet;
  }

  public enum Presence {
    Planning, Cleanup, Waiting, Execution, Backup, CLO, FQTasks
  }

  public static enum InUse {
    PLANNING, EXECUTION, CLEANUP, WAITING, SUSPENDED, NOT_INUSE;

    public void throwExceptionIfInUse(String fqName) throws XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE {
      if (this != NOT_INUSE) {
        throw new XPRC_INVALID_UNDEPLOYMENT_WORKFLOW_IN_USE(fqName, toString());
      }
    }
  }


  public InUse isInUse(WorkflowRevision workflowRevision) throws XPRC_InvalidPackageNameException, XPRC_DESTINATION_NOT_FOUND {

    
    if (logger.isDebugEnabled()) {
      logger.debug("Checking usage of <" + workflowRevision.wfFqClassName + ">");
    }

    Set<WorkflowRevision> workflowIdentifiers = getWorkflowIdentifiersFromOrdersInPlanning();
    if (!workflowIdentifiers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("PLANNING: ").append(workflowRevision.wfFqClassName).append(" inside ")
            .append(workflowIdentifiers).toString());
      }
      if (workflowIdentifiers.contains(workflowRevision)) { //somebody in planning
        return InUse.PLANNING;
      }
    }

    workflowIdentifiers = getWorkflowIdentifiersFromOrdersInExecution();
    if (!workflowIdentifiers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("EXECUTION: ").append(workflowRevision.wfFqClassName).append(" inside ")
            .append(workflowIdentifiers).toString());
      }
      if (workflowIdentifiers.contains(workflowRevision)) { //somebody in execution
        return InUse.EXECUTION;
      }
    }

    try {
      Map<WorkflowRevision, List<Long>> ordersFromBackup = getWorkflowIdentifiersAndOrderIdsFromOrdersInSuspension(workflowRevision.revision);
      workflowIdentifiers = ordersFromBackup.keySet();
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to obtain running orders from orderbackup, aborting", e);
      return InUse.SUSPENDED;
    }
    if (!workflowIdentifiers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("SUSPENSION: ").append(workflowRevision.wfFqClassName).append(" inside ")
            .append(workflowIdentifiers).toString());
      }
      if (workflowIdentifiers.contains(workflowRevision)) { //somebody in suspension
        return InUse.SUSPENDED;
      }
    }

    workflowIdentifiers = getWorkflowIdentifiersFromWaitingOrders();
    if (!workflowIdentifiers.isEmpty()) {
      if (logger.isDebugEnabled()) {
        logger.debug(new StringBuilder().append("WAITING: ").append(workflowRevision.wfFqClassName).append(" inside ")
            .append(workflowIdentifiers).toString());
      }
      if (workflowIdentifiers.contains(workflowRevision)) { //somebody in waiting
        return InUse.WAITING;
      }
    }

    return InUse.NOT_INUSE;

  }
  

  public long getLatestDeploymentId() {
    return orderCounter.getCurrentDeploymentCount();
  }


  public void countOrderThatKnowsAboutDeployment(long id) {
    orderCounter.countUp(id);
  }


  public void countDownOrderThatKnowsAboutDeployment(long id) {
    orderCounter.countDown(id);
  }


  public String getOrderAndDeploymentCounterState(String action) {
    return orderCounter.getCounterState(action);
  }


  public long propagateDeployment() {
    return orderCounter.propagateNewDeployment();
  }


  public void waitForUnreachableOrders() throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException {
    orderCounter.hideTillItsSafe(XynaProperty.DEPLOYMENT_RELOAD_TIMEOUT.getMillis());
  }


  DeploymentProcess getActiveDeploymentProcess() {
    int activeIndex = 0;
    while (deploymentProcessList.size() > activeIndex) {
      return deploymentProcessList.get(activeIndex);
    }
    return null;
  }


  public void waitTillDeploymentPropagated() throws XPRC_WorkflowProtectionModeViolationException {
    logger.debug("waitTillDeploymentPropagated");
    long start = System.currentTimeMillis();
    while( true ) {
      if (orderCounter.isItSafeToDeploy()) {
        logger.debug("it's safe to deploy");
        checkDeploymentConditions();
        if( checkNoNewDeployment() ) {
          //kein neues Deployment -> Deployment ist beendet worden
          return;
        } else {
          //Deployment noch nicht möglich, weiter probieren
          long now = System.currentTimeMillis();
          if( now > start + 10 * XynaProperty.DEPLOYMENT_RELOAD_TIMEOUT.getMillis() ) {
            XPRC_TimeoutWhileWaitingForUnaccessibleOrderException e = new XPRC_TimeoutWhileWaitingForUnaccessibleOrderException();
            getActiveDeploymentProcess().abortDeploymentProcess(e, "<unspecified>"); //wirft immer Exception
          }
        }
      } else {
        try {
          logger.debug("it's not safe going to hide");
          waitForUnreachableOrders();
        } catch (XPRC_TimeoutWhileWaitingForUnaccessibleOrderException e) {
          getActiveDeploymentProcess().abortDeploymentProcess(e, "<unspecified>"); //wirft immer Exception
          return; //kann nicht vorkommen
        }
      } 
    }
  }
  
  public Pair<Map<Presence,Set<WorkflowRevision>>, Map<WorkflowRevision, List<Long>>> getWorkflowsPresentInSystem() throws XPRC_WorkflowProtectionModeViolationException,
      XPRC_DESTINATION_NOT_FOUND {
    Map<Presence,Set<WorkflowRevision>> workflowsPresentInSystem = new EnumMap<Presence,Set<WorkflowRevision>>(Presence.class);
    Map<WorkflowRevision, List<Long>> ordersFromBackup = null;
    try {
      ordersFromBackup = getWorkflowIdentifiersAndOrderIdsFromOrdersInSuspension(null);
      workflowsPresentInSystem.put( Presence.CLO, getWorkflowIdentifiersFromOrdersInCLO() );
      workflowsPresentInSystem.put( Presence.FQTasks, getWorkflowIdentifiersFromOrdersInFQTasks() );
      workflowsPresentInSystem.put( Presence.Planning, getWorkflowIdentifiersFromOrdersInPlanning());
      workflowsPresentInSystem.put( Presence.Cleanup, getWorkflowIdentifiersFromOrdersInCleanup());
      workflowsPresentInSystem.put( Presence.Waiting, getWorkflowIdentifiersFromWaitingOrders());
      workflowsPresentInSystem.put( Presence.Execution, getWorkflowIdentifiersFromOrdersInExecution() );
      workflowsPresentInSystem.put( Presence.Backup, ordersFromBackup.keySet() );
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      throw e;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      // we could retrieve the highest WorkflowProtectionMode and abort if it's below FORCE
      logger.error("Error while retrieving active orderTypes for workflow protection", t);
      getActiveDeploymentProcess()
          .abortDeploymentProcess(new RuntimeException("Error while retrieving active orderTypes for workflow protection",
                                                    t), "unknown");
    }
    return Pair.of(workflowsPresentInSystem, ordersFromBackup);
  }


  private Set<WorkflowRevision> getWorkflowIdentifiersFromOrdersInFQTasks() throws XPRC_DESTINATION_NOT_FOUND {
    Map<Long, List<DestinationKey>> fqWorkflows = getActiveFQTaskWorkflows();
    HashSet<DestinationKey> destinationKeys = new HashSet<DestinationKey>();
     for( List<DestinationKey> dks : fqWorkflows.values() ) {
       destinationKeys.addAll(dks);
    }
    return destinationKeysToWorkflowRevision(destinationKeys);
  }


  private Set<WorkflowRevision> getWorkflowIdentifiersFromOrdersInCLO() throws XPRC_DESTINATION_NOT_FOUND, PersistenceLayerException {
    Set<DestinationKey> executionOrderTypesPresent = new HashSet<DestinationKey>();

    ODSConnection con = ods.openConnection();
    try {
      FactoryWarehouseCursor<CronLikeOrder> cursor =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
              .getCursorForRelevantCronLikeOrders(con, 250);

      Collection<CronLikeOrder> cronLikeOrders;
      cronLikeOrders = cursor.getRemainingCacheOrNextIfEmpty();

      while (!cronLikeOrders.isEmpty()) {
        for (CronLikeOrder cronLikeOrder : cronLikeOrders) {
          executionOrderTypesPresent.add(cronLikeOrder.getCreationParameters().getDestinationKey());
        }
        cronLikeOrders = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.", e);
      }
    }
    
    return destinationKeysToWorkflowRevision( executionOrderTypesPresent );
  }
  
  
  private Set<WorkflowRevision> destinationKeysToWorkflowRevision(Collection<DestinationKey> destinationKeys) throws XPRC_DESTINATION_NOT_FOUND {
    Set<WorkflowRevision> workflowsPresentInSystem = new HashSet<WorkflowRevision>();
    for (DestinationKey execOT : destinationKeys) {
      try {
        Long revision =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                .getRevision(execOT.getRuntimeContext());
        workflowsPresentInSystem.addAll(getWorkflowIdentifersForOrderType(execOT));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // ignore ... im Clusterfall kann dies passieren, wenn auf anderem Knoten eine Application installiert ist welche auf diesem Knoten nicht installiert ist
        logger.warn("Could not find revision for application " + execOT.getApplicationName() + " "
                        + execOT.getVersionName(), e);
      }
    }
    return workflowsPresentInSystem;
  }



  private Map<WorkflowRevision, List<Long>> getWorkflowIdentifiersAndOrderIdsFromOrdersInSuspension(final Long revision) throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND {
    WarehouseRetryExecutableOneException<Map<WorkflowRevision, List<Long>>, XPRC_DESTINATION_NOT_FOUND> wre =
        new WarehouseRetryExecutableOneException<Map<WorkflowRevision, List<Long>>, XPRC_DESTINATION_NOT_FOUND>() {

          public Map<WorkflowRevision, List<Long>> executeAndCommit(ODSConnection con) throws PersistenceLayerException,
              XPRC_DESTINATION_NOT_FOUND {
            return getWorkflowIdentifiersAndOrderIdsFromOrdersInSuspension(revision, con);
          }
        };

    return WarehouseRetryExecutor.buildCriticalExecutor().
           connection(ODSConnectionType.DEFAULT).
           storable(OrderInstanceBackup.class).
           execute(wre);
  }
  

  //could this be nice/cleaner (more modularized)
  private void checkDeploymentConditions() throws XPRC_WorkflowProtectionModeViolationException {

    if (logger.isDebugEnabled()) {
      logger.debug("Checking deployment conditions");
    }

    try {
      failFastIfOrdertypeInOrderArchive();
      
      //Pausieren der FQTasks TODO erst hier? oder zeitlich noch früher?
      List<Long> activeFQTasks = getAffectedFqTasks(affectedWorkflowsPerProtectionMode.get(WorkflowProtectionMode.BREAK_ON_USAGE));
      XynaFactory.getInstance().getProcessing().getFrequencyControl().pauseFrequencyControlledTasks(activeFQTasks);
      getActiveDeploymentProcess().addPausedTaskIds(activeFQTasks);

      //Welche Aufträge befinden sich derzeit im System?
      Pair<Map<Presence, Set<WorkflowRevision>>, Map<WorkflowRevision, List<Long>>> pair = getWorkflowsPresentInSystem();
      Map<Presence, Set<WorkflowRevision>> workflowsPresentInSystem = pair.getFirst();
      Map<WorkflowRevision, List<Long>> suspendedOrders = pair.getSecond();
     
      //Prüfen, ob die WorkflowProtectionMode eingehalten werden
      for( WorkflowProtectionMode wpm : WorkflowProtectionMode.getWorkflowProtectionModesOrderdByForce(true) ) {
        for( Map.Entry<Presence, Set<WorkflowRevision>> entry : workflowsPresentInSystem.entrySet() ) {
          WorkflowRevision failed = checkProtectionModeViolation(entry.getValue(), wpm);
          if( failed != null ) {
            callProtectionModeViolation(failed);
          }
        }
      }
      
      //Ermitteln der laufenden OrderTypes
      HashSet<WorkflowRevision> toSuspend = new HashSet<WorkflowRevision>();
      toSuspend.addAll(workflowsPresentInSystem.get(Presence.Execution));
      toSuspend.addAll(workflowsPresentInSystem.get(Presence.Backup)); //könnte wegen Resume schon wieder laufen
      logger.debug("all running OrderTypes "+toSuspend);
      toSuspend.retainAll( getAllProtectedWorkflows() );
      
      //Sammeln der betroffenen suspendierten Aufträge im OrderBackup
      HashSet<Long> suspendedOrderIds = new HashSet<Long>();
      for( WorkflowRevision wr : toSuspend ) {
        List<Long> list = suspendedOrders.get(wr);
        if( list != null ) {
          suspendedOrderIds.addAll(list);
        }
      }
      //Sperren der suspendierten Aufträge im SuspendResumeManagement
      logger.info("OrderIds prevented from resuming: "+suspendedOrderIds);
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().addUnresumeableOrders(suspendedOrderIds);
      //FIXME ist das hier evtl. schon zu spät?
      
      //Im SuspendResumeManagement gesperrte Aufträge zum Entsperren speichern
      getActiveDeploymentProcess().addPausedResumes(suspendedOrderIds);
      
      //Suspendieren der laufenden OrderTypes
      logger.info("Suspending OrderTypes "+toSuspend);
      for( WorkflowProtectionMode wpm : WorkflowProtectionMode.getWorkflowProtectionModesOrderdByForce(true) ) {
        WorkflowRevision failed = suspendOrderTypes(toSuspend, suspendedOrderIds, wpm);
        if( failed != null ) {
          callProtectionModeViolation(failed);
        }
      }
      
    } catch (XPRC_DESTINATION_NOT_FOUND e) { //will get wrapped in a XPRC_WorkflowProtectionModeViolationException 
      getActiveDeploymentProcess().abortDeploymentProcess(e, e.getOrderType());
    }
  }

  private boolean checkNoNewDeployment() throws XPRC_WorkflowProtectionModeViolationException {
    entranceLock.lock();
    try {
      logger.debug("All Conditions were checked, was there a new Deployment?");
      if (newDeploymentDuringPreDeployment.compareAndSet(true, false) || violationThisRun) {
        logger.debug("there is either a new deployment during Pre_Deployment or a non-leader violation");
        violationThisRun = false;
        regenerateMapping();
        return false;
      } else {

        logger.debug("There was no new Deployment, progressing state and resuming deployment");
        getActiveDeploymentProcess().progressState();
        getActiveDeploymentProcess().resumeDeployment();
        return true;
      }
    } finally {
      entranceLock.unlock();
    }
  }
  

  private WorkflowRevision checkProtectionModeViolation(Set<WorkflowRevision> workflowsPresentInSystem,
                                                        WorkflowProtectionMode workflowProtectionMode) throws XPRC_DESTINATION_NOT_FOUND, XPRC_WorkflowProtectionModeViolationException {
    Set<WorkflowRevision> subSet = affectedWorkflowsPerProtectionMode.get(workflowProtectionMode);
    if (subSet == null || subSet.size() == 0) {
      return null; //nichts zu tun, nichts fehlgeschlagen
    }
   
    for (WorkflowRevision workflowIdentifier : workflowsPresentInSystem) {
      if (subSet.contains(workflowIdentifier)) {
        switch( workflowProtectionMode ) {
          case BREAK_ON_USAGE:
            return workflowIdentifier;
          case BREAK_ON_INTERFACE_CHANGES:
            if (getActiveDeploymentProcess().caresForInterfaceChanges()
                && getActiveDeploymentProcess().isWorkflowInterfaceDependent(workflowIdentifier)) {
              return workflowIdentifier;
            }
            break;
          case FORCE_DEPLOYMENT:
            break;
          case FORCE_KILL_DEPLOYMENT:
            break;
        }
      }
    }
    return null; //alle Tests bestanden
  }

  /**
   * suspendierbare OrderTypen suspendieren, damit sie beim Deployment nicht stören
   * @param workflowsPresentInSystem
   * @param suspendedOrderIds 
   * @param wpm
   * @return
   */
  private WorkflowRevision suspendOrderTypes(Set<WorkflowRevision> workflowsPresentInSystem, HashSet<Long> suspendedOrderIds, WorkflowProtectionMode workflowProtectionMode) {
    Set<WorkflowRevision> subSet = affectedWorkflowsPerProtectionMode.get(workflowProtectionMode);
    if (subSet == null || subSet.size() == 0) {
      return null; //nichts zu tun, nichts fehlgeschlagen
    }
   
    for (WorkflowRevision workflowIdentifier : workflowsPresentInSystem) {
      try {
        SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
        SuspendOrdertypeBean returnedBean = srm.suspendOrdertype(workflowIdentifier.wfFqClassName, 
                                                                 workflowProtectionMode == WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT,
                                                                 workflowIdentifier.revision, true);
        getActiveDeploymentProcess().addResumeTargets(returnedBean.getResumeTargets());
        suspendedOrderIds.addAll( returnedBean.getSuspendedRootOrderIds() );
        if (!returnedBean.wasSuccessfull()) {
          return workflowIdentifier;
        }
      } catch (XynaException e) {
        logger.warn("Exception while suspending ordertype", e);
        return workflowIdentifier;
      }
    }
    return null; //alle suspendiert
  }

  private void callProtectionModeViolation(WorkflowRevision workflowIdentifier)
      throws XPRC_WorkflowProtectionModeViolationException {
    entranceLock.lock();
    try {
      logger.debug( "ProtectionModeViolation for "+workflowIdentifier
                    +", newDeploymentDuringPreDeployment="+newDeploymentDuringPreDeployment.get());
      if (!newDeploymentDuringPreDeployment.get()) {
        getActiveDeploymentProcess().protectionModeViolationForWorkflow(workflowIdentifier);
        violationThisRun = true;
      }
    } finally {
      entranceLock.unlock();
    }
  }

  
  /**
   * Sucht alle Paare (FQTaskId -> List(DestinationKey)) aller aktiven FrequencyControlledTasks
   * @return
   */
  private Map<Long,List<DestinationKey>> getActiveFQTaskWorkflows() {
    Map<Long,List<DestinationKey>> fqWorkflows = new HashMap<Long,List<DestinationKey>>();
    Collection<FrequencyControlledTask> fqTasks =
        XynaFactory.getInstance().getProcessing().getFrequencyControl().getActiveFrequencyControlledTasks();
    if (fqTasks == null || fqTasks.size() == 0) {
      return fqWorkflows;
    }
    for (FrequencyControlledTask fct : fqTasks) {
      if (fct instanceof FrequencyControlledOrderCreationTask) {
        fqWorkflows.put( fct.getID(), ((FrequencyControlledOrderCreationTask) fct).getWatchedDestinationKeys() );
      }
    }
    return fqWorkflows;
  }
  
  private List<Long> getAffectedFqTasks(Set<WorkflowRevision> protectedWorkflows) throws XPRC_DESTINATION_NOT_FOUND {
    if (protectedWorkflows != null && protectedWorkflows.size() > 0) {
      List<Long> tasks = new ArrayList<Long>();
      Map<Long,List<DestinationKey>> fqWorkflows = getActiveFQTaskWorkflows();
      for( Map.Entry<Long,List<DestinationKey>> entry : fqWorkflows.entrySet() ) {
        for( DestinationKey dk : entry.getValue() ) {
          Set<WorkflowRevision> resolved = WorkflowRevision.construct(DispatcherType.Execution, dk);
          for (WorkflowRevision workflowRevision : resolved) {
            if (protectedWorkflows.contains(workflowRevision)) {
              tasks.add(entry.getKey());
              break;
            }
          }
        }
      }
      return tasks;
    } else {
      return Collections.emptyList();
    }
  }

  public void regenerateMapping() {
    affectedWorkflowsPerProtectionMode = getActiveDeploymentProcess().generateMapping();
  }


  private Collection<WorkflowRevision> getWorkflowIdentifersForOrderType(DestinationKey destinationForOrderType)
      throws XPRC_DESTINATION_NOT_FOUND {

    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    if (XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
        .getExecutionEngineDispatcher().isPredefined(destinationForOrderType)) {
      return workflowIdentifiers; //we could do the same for the DestinationValues to catch EmptyPlanning, CleanUp and EmptyWF
      // but thats rather costly, it won't hurt if we just add them, there won't be any dependencies for them
    }
    try {
      workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Planning, destinationForOrderType) );
      workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Execution, destinationForOrderType) );
      workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Cleanup, destinationForOrderType) );
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      if (getActiveDeploymentProcess() != null) {
        WorkflowProtectionMode lowestMode = getActiveDeploymentProcess().getLowestDeploymentMode();
        switch (lowestMode) {
          case FORCE_KILL_DEPLOYMENT :
          case FORCE_DEPLOYMENT :
            return Collections.emptyList();
          default :
            throw e;
        }
      } else {
        throw e;
      }
    }
    return workflowIdentifiers;
  }
  
  private static Collection<WorkflowRevision> getWorkflowIdentifersForDestination(DestinationKey destination)
      throws XPRC_DESTINATION_NOT_FOUND {

    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    if (XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
        .getExecutionEngineDispatcher().isPredefined(destination)) {
      return workflowIdentifiers; //we could do the same for the DestinationValues to catch EmptyPlanning, CleanUp and EmptyWF
      // but thats rather costly, it won't hurt if we just add them, there won't be any dependencies for them
    }
    workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Planning, destination) );
    workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Execution, destination) );
    workflowIdentifiers.addAll( WorkflowRevision.construct(DispatcherType.Cleanup, destination) );
    return workflowIdentifiers;
  }

  private FactoryWarehouseCursor<SerialVersionIgnoringCronLikeOrder> getCronLikeOrderCursor(ODSConnection connection,
                                                                                            int cacheSize)
      throws PersistenceLayerException {
    return connection.getCursor(getAllCronLikeOrdersIgnoringSerialVersionUId,
                                new Parameter(new CronLikeOrder().getLocalBinding(connection.getConnectionType())),
                                SerialVersionIgnoringCronLikeOrder.getSerialVersionIgnoringReader(), cacheSize, queryCache);
  }


  /**
   * Neuladen der betroffenen XynaOrders: teilweise in Memory, teilweise im OrderBackup 
   * @param fromThisProcess
   */
  public void reloadAffectedOrders(DeploymentProcess fromThisProcess) {
    //1. Analysieren aller Aufträge im Scheduler
    //   a) betroffene Aufträge:
    //      in Wartezustand WaitingCause.Deployment versetzen
    //      UrgencyOrder merken für ReloadOrderBackup und zum Aufheben des WaitingCause
    //   b) nicht betroffene Aufträge:
    //      Id merken für ReloadOrderBackup
    //2. Warten auf nicht-neuladbare Aufträge
    //   Nun muss gewartet werden, dass keine Aufträge mehr an Stellen vorkommen, dan denen sie nicht
    //   neu geladen werden können. Dies ist der Fall, wenn sie vom Planning oder Cleanup ausgeführt
    //   werden oder wenn der OrderCount Aufträge in unsichtbaren Bereichen meldet.
    //3. Analysieren aller an OrderFilter wartenden Aufträge
    //   a) betroffene Aufträge:
    //      XynaOrder merken für ReloadOrderBackup
    //   b) nicht betroffene Aufträge:
    //      Id merken für ReloadOrderBackup
    //4. ReloadOrderBackup
    //   Suchen aller Backup-OrderIds
    //   FallUnterscheidung
    //   a) nicht betroffene Auftrag aus obigen Analysen -> nichts zu tun
    //   b) betroffene Aufträge aus obigen Analysen -> reloadOrder(XynaOrder) in Memory und Backup
    //   c) unbekannte Aufträge: -> Lesen des Backups mit SerialVersionUIDIgnoringReader, neues Backup falls betroffen
    //   Rückgabe der XynaOrders, die nicht im Backup gefunden wurden
    //5. ReloadOrdersNotInBackup
    //   reloadOrder(XynaOrder) in Memory
    //6. weitere Reloads CronLikeOrders, FQ-Tasks
    //7. Es könnten nun neue Aufträge am Planning-OrderFilter warten. Es ist jedoch unwahrscheinlich, dass
    //   diese noch nicht mit den neuen Klassen erstellt wurden. Daher werden diese jetzt nicht nochmal angeschaut.
    
    HashMap<DestinationKey,Boolean> affectedDestinationKeys = new HashMap<DestinationKey,Boolean>();
    
    Map<Long,XynaOrderServerExtension> affectedOrders = new HashMap<Long,XynaOrderServerExtension>();
    Set<Long> unaffectedOrderIds = new HashSet<Long>();
    
    //1. Analysieren aller Aufträge im Scheduler
    analyzeAllOrdersFromScheduler(affectedDestinationKeys, affectedOrders, unaffectedOrderIds);
    fromThisProcess.addPausedScheduling(affectedOrders.keySet());
    //FIXME wieso erst im scheduler entnehmen und dann auf unreachable aufträge warten? wasist mit aufträgen,
    //   die im scheduler ankommen, nachdem wir (im nächsten schritt) auf nicht-reloadable aufträge gewartet haben?
    //2. Warten auf nicht-neuladbare Aufträge
    waitUntilAllOrdersAreReloadable(affectedDestinationKeys);
    
    //3. Analysieren aller an OrderFilter wartenden Aufträge -> Ermittlung der Affected Orders
    analyzeAllOrdersForReload(fromThisProcess, affectedDestinationKeys, affectedOrders, unaffectedOrderIds);

    //4. ReloadOrderBackup
    Collection<XynaOrderServerExtension> affectedOrdersNotInBackup = reloadOrderBackup( unaffectedOrderIds, affectedOrders );
    
    //5. ReloadOrdersNotInBackup
    reloadOrdersNotInBackup(affectedOrdersNotInBackup);
    
    //6. weitere Reloads CronLikeOrders, FQ-Tasks 
    reloadCronLikeOrders(fromThisProcess);
    reloadFQTasks(fromThisProcess);
  }

  /**
   * Wartet, bis alle Aufträge reloadbar sind oder ein (langes) Timeout erreicht wurde.
   * Das Reload darf nicht einfach abgebrochen werden, das würde nur mehr Probleme machen als ein
   * zu früh ausgeführtes Reload.
   *
   * @param affectedDestinationKeys
   */
  private void waitUntilAllOrdersAreReloadable(HashMap<DestinationKey, Boolean> affectedDestinationKeys) {
    long timeout = XynaProperty.DEPLOYMENT_RELOAD_TIMEOUT.getMillis();
    //TODO Timeout aufteilen?
    for( int i=0; i< 10; ++i ) { //TODO war vorher auch schon so...   
      if( ! waitUntilNoRunningProcessIsAffected(affectedDestinationKeys, DispatcherType.Planning, timeout ) ) {
        //Timeout abgelaufen, daher Abbruch 
        continue;
      }
      if( ! waitUntilNoRunningProcessIsAffected(affectedDestinationKeys, DispatcherType.Cleanup, 0 ) ) {
        //Timeout abgelaufen, daher Abbruch 
        continue;
      }
       //warten, bis OrderCount 0
      try {
        orderCounter.hideTillItsSafe(timeout);
      } catch (XPRC_TimeoutWhileWaitingForUnaccessibleOrderException e) {
        continue;
      }
      
      return; //Warten war erfolgreich!
    }
    logger.error("some orders could not be found to reload after deployment.");
  }


  private boolean waitUntilNoRunningProcessIsAffected(HashMap<DestinationKey, Boolean> affectedDestinationKeys,
                                                      DispatcherType dispatcherType, long timeout) {
    boolean affected = isRunningProcessAffected(affectedDestinationKeys,dispatcherType);
    if( !affected ) {
      return true; //kein Process betroffen, daher fertig
    }
    if( timeout <= 0 ) {
      return false; //Prozesse betroffen, aber kein Warten gewünscht
    }
    long start = System.currentTimeMillis();
    long sleep = Math.min(1000, timeout/10); //bei langen Timeouts jede Sekunde prüfen, bei kurzen Timeouts häufiger
    do {
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
        //dann halt kürzer warten
      }
      affected = isRunningProcessAffected(affectedDestinationKeys,dispatcherType);
      if( !affected ) {
        return true; //kein Process betroffen, daher fertig
      }      
    } while( System.currentTimeMillis() < start+timeout );
    return false; //Timeout abgelaufen
  }


  private boolean isRunningProcessAffected(HashMap<DestinationKey, Boolean> affectedDestinationKeys,
                                           DispatcherType dispatcherType) {
    for (XynaOrderServerExtension xo : dispatcherType.getOrdersOfRunningProcesses() ) {
      if( isDestinationKeyAffected( affectedDestinationKeys, xo.getDestinationKey() ) ) {
        return true;
      }
    }
    return false;
  }


  private void analyzeAllOrdersFromScheduler(HashMap<DestinationKey, Boolean> affectedDestinationKeys,
                                             Map<Long,XynaOrderServerExtension> affectedOrders,
                                             Set<Long> unaffectedOrderIds) {
    AllOrdersList allOrdersList = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    List<XynaOrderInfo> orders = allOrdersList.getAllOrders();
   
    for(XynaOrderInfo xoi : orders) {
      boolean affected = isDestinationKeyAffected( affectedDestinationKeys, xoi.getDestinationKey() );
      if( affected ) {
        XynaOrderServerExtension xo = allOrdersList.waitForDeployment(xoi.getOrderId());
        if (xo != null) {
          affectedOrders.put( xoi.getOrderId(), xo);
        } //else: scheduled oder wegen OOM-schutz im backup
      } else {
        unaffectedOrderIds.add(xoi.getOrderId());
      }
    }
  }
  
  private void analyzeAllOrdersForReload(DeploymentProcess fromThisProcess,
                                         HashMap<DestinationKey, Boolean> affectedDestinationKeys,
                                         Map<Long,XynaOrderServerExtension> affectedOrders, Set<Long> unaffectedOrderIds) {
    
    Collection<XynaOrderServerExtension> orders = getAllOrdersForReload(fromThisProcess);
    for( XynaOrderServerExtension xo : orders ) {
      boolean affected = isDestinationKeyAffected( affectedDestinationKeys, xo.getDestinationKey() );
      if( affected ) {
        affectedOrders.put( xo.getId(), xo );
      } else {
        unaffectedOrderIds.add(xo.getId());
      }
    }
  }

  /**
   * Update der im OrderBackup gefundenen Aufträge
   * @param unaffectedOrderIds
   * @param affectedOrders
   * @return 
   */
  private Collection<XynaOrderServerExtension> reloadOrderBackup(Set<Long> unaffectedOrderIds, Map<Long, XynaOrderServerExtension> affectedOrders) {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      //Liste aller OrderIds im Backup: Es können keine weiteren betroffenen Aufträge ins Backup kommen außer
      //den bereits bekannten aus Scheduler und OrderFiltern
      List<Long> orderBackupIds = selectAllOrderBackupIds(con);
      int numberOfBackups = orderBackupIds.size();
      
      //nicht betroffene orderBackups müssen nicht weiter beachtet werden
      orderBackupIds.removeAll(unaffectedOrderIds);

      logger.info(orderBackupIds.size() +" orderbackups out of "+numberOfBackups+" are affected");  
      
      int batchSize = 50; //für Batch-Commit
      for( int i=0; i<orderBackupIds.size(); ++i ) {
        Long orderId = orderBackupIds.get(i);
        reloadOrder(con, affectedOrders.get(orderId), orderId);

        //Batch-Commit
        if( (i+1)%batchSize == 0 ) {
          con.commit();
        }
      }
      //Commit für Rest
      con.commit();
      
      //alle bekannten betroffenen Aufträge ohne Backup rückmelden für Reload in Memory
      Set<Long> affectedNotInBackup = CollectionUtils.differenceSet(affectedOrders.keySet(), orderBackupIds);
      return CollectionUtils.valuesForKeys(affectedOrders, affectedNotInBackup);
     
    } catch (PersistenceLayerException e) {
      // don't abort, but we could need to delete the now probably corrupted entries
      logger.error("Error while retrieving orderBackup for reload", e);
      //alle bekannten betroffenen Aufträge in Memory neuladen, um Schaden zu minimieren
      return affectedOrders.values();
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        // don't abort
        logger.warn("Error while closing connection for orderBackup reload", e);
      }
    }    
  }
    
  private List<Long> selectAllOrderBackupIds(ODSConnection con) throws PersistenceLayerException {
    int binding = new OrderInstanceBackup().getLocalBinding(con.getConnectionType());
    Parameter parameter = new Parameter(binding);
    StringBuilder queryBuilder = new StringBuilder(selectAllLocalBackupIds);
    Set<WorkflowRevision> allProtected = getAllProtectedWorkflows();
    if (allProtected.size() > 0) {
      queryBuilder.append(" AND (");
      Set<Long> revisions = new HashSet<Long>();
      for (WorkflowRevision wr : allProtected) {
        revisions.add(wr.revision);
      }
      Iterator<Long> revIter = revisions.iterator();
      while (revIter.hasNext()) {
        Long revision = revIter.next();
        queryBuilder.append(OrderInstanceBackup.COL_REVISION)
                    .append(" =  ?");
        parameter.add(revision);
        if (revIter.hasNext()) {
          queryBuilder.append(" OR ");
        }
      }
      queryBuilder.append(")");
    }
    PreparedQuery<Long> query = con.prepareQuery(new Query<Long>(
        queryBuilder.toString(), new ResultSetReader<Long>() {
          public Long read(ResultSet rs) throws SQLException {
            return rs.getLong(OrderInstanceBackup.COL_ID);
          }
        }));
    return con.query( query, parameter, -1);
  }
  
  

  /**
   * Reload der XynaOrder, mit zwei Fällen:
   * 1) XynaOrder in Memory gefunden -> reloadOrder und normales Backup
   * 2) XynaOrder nicht in Memory gefunden -> Austausch nur im OrderBackup
   * Es ist kein SELECT_FOR_UPDATE nötig, da es keinen Thread geben kann und darf, der
   * gleichzeitig auf das OrderBackup zugreift:
   * a) Resume ist gesperrt; b) laufende Prozesse sind suspendiert; c) Scheduler schedult nicht
   * d) OrderFilter blockieren weitere Stellen
   * @param con
   * @param xo
   * @param orderId
   * @throws PersistenceLayerException 
   */
  private void reloadOrder(ODSConnection con, XynaOrderServerExtension xo, Long orderId) throws PersistenceLayerException {
    if( xo == null ) {
      //Auftrag im Backup lesen und geändert backuppen, falls betroffen
      PreparedQuery<? extends SerialVersionIgnoringOrderInstanceBackup> oibQuery =
          queryCache.getQueryFromCache(selectOrderInstanceBackup, con,
                                       SerialVersionIgnoringOrderInstanceBackup.getSerialVersionIgnoringReader());
      SerialVersionIgnoringOrderInstanceBackup oib = con.queryOneRow(oibQuery, new Parameter(orderId));
      if (oib == null) {
        //Auftrag ist anscheinend fertig geworden, daher fehlt Backup nun
        return;
      }
      try {
        WorkflowRevision workflowIdentifier = getWorkflowRevision(oib);
        if (workflowIdentifier != null && getActiveDeploymentProcess().isWorkflowInterfaceDependent(workflowIdentifier)) {
          if (oib.getXynaorder() != null) {
            reloadOrder(oib.getXynaorder());
          }
          if (oib.getDetails() != null) {
            
            oib.getDetails().getAuditDataAsJavaObject().reloadGeneratedObjectsInsideAuditIfNecessary(new DeploymentAuditReloader(oib.getRevision()));
          }
          if (oib.getRootId() == oib.getId() && oib.getXynaorder() == null) {
            logger.warn("Not rewriting orderbackup due to missing XynaOrder");
          } else {
            renewOrderBackup(oib,con);
          }
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("Error during reload of OrderInstanceBackup", t);
      }
    } else {
      //Auftrag in Memory teilweise austauschen
      reloadOrder(xo);
      //Backup erstellen 
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
          .backup(xo, BackupCause.WAITING_FOR_CAPACITY, con);
    }
  }


  private WorkflowRevision getWorkflowRevision(OrderInstanceBackup oib) throws XPRC_DESTINATION_NOT_FOUND,
      XPRC_InvalidPackageNameException {
    if (oib.getXynaorder() != null && oib.getXynaorder().getDestinationKey() != null) {
      if (oib.getXynaorder().getExecutionProcessInstance() != null &&
          oib.getXynaorder().getExecutionProcessInstance().getClass().getClassLoader() instanceof ClassLoaderBase) {
        return new WorkflowRevision(oib.getXynaorder().getExecutionProcessInstance().getClass().getName(),
                                    ((ClassLoaderBase)oib.getXynaorder().getExecutionProcessInstance().getClass().getClassLoader()).getRevision());
      } else {
        Set<WorkflowRevision> all = WorkflowRevision.construct(DispatcherType.Execution, oib.getXynaorder().getDestinationKey());
        if (all.size() <= 0) {
          return null;
        } else {
          return all.iterator().next();
        }
      }
    } else if (oib.getDetails() != null && oib.getDetails().getAuditDataAsJavaObject() != null) {
      String originalName = oib.getDetails().getAuditDataAsJavaObject().getProcessName();
      if (originalName != null && originalName.trim().length() != 0) {
        String wfName = GenerationBase.transformNameForJava(originalName);
        if (wfName != null && wfName.trim().length() != 0) {
          //revision vom workflow bestimmen
          long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
              .getRevisionDefiningXMOMObjectOrParent(originalName, oib.getRevision());
          return new WorkflowRevision(wfName, revision);
        }
      }
    }
    return null; //keine WorkflowRevision gefunden
  }


  public void renewOrderBackup(OrderInstanceBackup oib, ODSConnection con) throws PersistenceLayerException {
    if (oib.getDetails() == null && oib.getXynaorder() != null) {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
          .backup(oib.getXynaorder(), oib.getBackupCauseAsEnum(), con);
    } else {
      con.persistObject(oib);
    }
  }


  private void reloadOrdersNotInBackup(Collection<XynaOrderServerExtension> affectedOrdersNotInBackup) {
    for (XynaOrderServerExtension xo : affectedOrdersNotInBackup) {
      reloadOrder(xo);
    }
  }


  private boolean isDestinationKeyAffected(HashMap<DestinationKey, Boolean> affectedDestinationKeys,
                                           DestinationKey destinationKey)  {
    Boolean affected = affectedDestinationKeys.get(destinationKey);
    if( affected == null ) {
      affected = isDestinationKeyAffected( DispatcherType.Execution, destinationKey );
      if( ! affected ) {
        affected = isDestinationKeyAffected( DispatcherType.Planning, destinationKey );
      }
      if( ! affected ) {
        affected = isDestinationKeyAffected( DispatcherType.Cleanup, destinationKey );
      }
      affectedDestinationKeys.put(destinationKey, affected);
    }
    return affected;
  }


  private Boolean isDestinationKeyAffected(DispatcherType dispatcherType, DestinationKey destinationKey) {
    Set<WorkflowRevision> wrs;
    try {
      wrs = WorkflowRevision.construct(dispatcherType,destinationKey);
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      logger.warn(dispatcherType+ "-destination not found for "+destinationKey);
      return Boolean.FALSE;
    }
    for (Set<WorkflowRevision> set : affectedWorkflowsPerProtectionMode.values()) {
      for (WorkflowRevision wr : wrs) {
        if( set.contains(wr) ) {
          return Boolean.TRUE;
        }
      }
    }
    return Boolean.FALSE;
  }


  private Collection<XynaOrderServerExtension> getAllOrdersForReload(DeploymentProcess fromThisProcess) {
    if (fromThisProcess.getOrderFilters().size() > 0) {
      Collection<XynaOrderServerExtension> orders = new HashSet<XynaOrderServerExtension>();
      for (OrderFilter of : fromThisProcess.getOrderFilters()) {
        orders.addAll(OrderFilterAlgorithmsImpl.getInstance().getOrdersHeldAtProcessors(of));
      }
      Set<XynaOrderServerExtension> allOrders = new HashSet<XynaOrderServerExtension>();

      for (XynaOrderServerExtension order : orders) {
        if (order.getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
          allOrders.addAll(((RedirectionXynaOrder) order).getRedirectedOrder().getOrderAndChildrenRecursively());
        } else {
          allOrders.addAll(order.getOrderAndChildrenRecursively());
        }
      }

      return allOrders;
    } else {
      return Collections.emptySet();
    }
  }


  public void addDeployment(Set<WorkflowRevision> myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode) throws XPRC_WorkflowProtectionModeViolationException {
    addDeployment(myAffectedWorkflows, myProtMode, null);
  }


  public void addDeployment(Set<WorkflowRevision>  myAffectedWorkflows, GenerationBase.WorkflowProtectionMode myProtMode,
                            Set<WorkflowRevision>  myUsedWFs) throws XPRC_WorkflowProtectionModeViolationException {
    if (logger.isDebugEnabled()) {
      logger.debug("myAffectedWorkflows: " + myAffectedWorkflows);
      logger.debug("myUsedWFs: " + myUsedWFs);
    }

    // FIXME innen drin werden connections geholt, das kann zu deadlocks führen, wenn jemand mit einer connection
    //       das lock haben will
    entranceLock.lock();
    AtomicBoolean entranceLockLocked = new AtomicBoolean(true);
    try {
      // depending on state:
      // Pre_deployment: activedeployment.add
      // CHECK_CONDITIONS: add and set a boolean to signalize the DeploymentProcess to run the checks again
      // Deployment: wait till Deployments are finished and start a new Process
      // again wait at an object and get notified?
      // Finished: activeProcess should be null, a new Process can be begun
      if (getActiveDeploymentProcess() == null) {
        logger.debug("Deployment received and there was no active process, creating a new one");
        deploymentProcessList.add(new DeploymentProcess(myAffectedWorkflows, myProtMode, myUsedWFs));
        entranceLock.unlock();
        entranceLockLocked.set(false);
        leaderWaitsTillDeploymentPropagated();
      } else if (getActiveDeploymentProcess().getState() == DeploymentProcessState.PRE_DEPLOYMENT) {
        logger.debug("Deployment received during DeploymentProcessState.PRE_DEPLOYMENT, adding it to the active process");
        long id = getActiveDeploymentProcess().addDeployment(myAffectedWorkflows, myProtMode, myUsedWFs);
        newDeploymentDuringPreDeployment.compareAndSet(false, true);
        getActiveDeploymentProcess().putToSleep(id, entranceLockLocked); //hier passiert entranceLock.unlock
      } else {
        logger.debug("Deployment received during DeploymentProcessState." + getActiveDeploymentProcess().getState() + ", it will be hold until that is finished");
        try { //you have to wait till it's finished
          synchronized (bench) {
            entranceLock.unlock();
            entranceLockLocked.set(false);
            bench.wait();
          }
        } catch (InterruptedException e) {
          throw new RuntimeException("Deployment-Thread got interrupted while waiting to proceed", e);
        }
        //recall addDeployment, there could be several waiting
        addDeployment(myAffectedWorkflows, myProtMode, myUsedWFs);
      }
    } finally {
      if (entranceLockLocked.get()) {
        entranceLock.unlock();
      }
    }
  }


  void leaderWaitsTillDeploymentPropagated() throws XPRC_WorkflowProtectionModeViolationException {
    try {
      regenerateMapping();
      waitTillDeploymentPropagated();
    } catch (RuntimeException e) {
      getActiveDeploymentProcess().abortDeploymentProcess(e, "<unspecified>");
    } catch (Error e) {
      getActiveDeploymentProcess().abortDeploymentProcess(e, "<unspecified>");
    }
  }


  // this will be called from GenerationBase after the actual deployment finished
  public void cleanupIfLast() {
    logger.debug("cleanupIfLast is called");
    DeploymentProcess activeDeployment = getActiveDeploymentProcess();
    try {
      if (activeDeployment != null &&
          //wenn das deployment mit dem aktuellen thread bereits abgebrochen wurde (z.b. wegen laufender aufträge), ist nichts mehr zu tun
          activeDeployment.participatingThreads.contains(Thread.currentThread().getId()) && 
          //evtl ist bereits ein cleanup am laufen, dann muss dieser thread nichts tun (kann das passieren? eigtl sollten ja alle an der cyclic barrier warten)
          !activeDeployment.someoneWantsToCleanUp.get() &&
          //auf die anderen deployments warten und dann einen thread fürs cleanup auswählen
          activeDeployment.isDesignatedToCleanup()) {
        reloadAndCleanup(activeDeployment.getOrderFilters());
      }
    } catch (InterruptedException e) {
      logger.debug("Was calling cleanUp put received InterruptedException");
      if (activeDeployment.someoneWantsToCleanUp.compareAndSet(false, true)) {
        //if we don't have one yet to do it, you have to
        reloadAndCleanup(activeDeployment.getOrderFilters());
      }
    } catch (BrokenBarrierException e) {
      logger.debug("Was calling cleanUp put received BrokenBarrierException");
      if (activeDeployment.someoneWantsToCleanUp.compareAndSet(false, true)) {
        //if we don't have one yet to do it, you have to
        reloadAndCleanup(activeDeployment.getOrderFilters());
      }
    }
    //TODO noch auf den cleanup-thread warten, bis er damit fertig ist, falls man das selbst nicht war
  }

  public void reloadAndCleanup( List<OrderFilterDeployment> ofs) {
    try {
      DeploymentProcess processToCleanup = getActiveDeploymentProcess();
      reloadAffectedOrders(processToCleanup);
    } finally {
      cleanup(ofs);
    }
  }

  public void cleanup(List<OrderFilterDeployment> ofs) {
    DeploymentProcess processToCleanup = null;
    try {
      processToCleanup = getActiveDeploymentProcess();

      orderCounter.cleanup(processToCleanup.getParticipatedDeploymentIds());
      for (OrderFilterDeployment of : ofs) {
        OrderFilterAlgorithmsImpl.getInstance().removeOrderFilter(of);
      }

      processToCleanup.cleanup();
    } finally {
      entranceLock.lock();
      try {
        //this will open up the entrance to other deployments (as soon as the lock is unlocked)
        if (processToCleanup != null) {
          deploymentProcessList.remove(processToCleanup);
        }
        synchronized (bench) {
          bench.notifyAll();
        }
      } finally {
        entranceLock.unlock();
    }
    }
  }


  public void reloadOrder(XynaOrderServerExtension xo) {

    if (logger.isDebugEnabled()) {
      logger.debug("Received order " + xo.getId() + " for reload");
    }
    // update the deploymentId for all reachable orders
    xo.setIdOfLatestDeploymentKnownToOrder(getLatestDeploymentId());
    
    try {
      if (xo.getExecutionProcessInstance() != null) {
        xo.setExecutionProcessInstance(checkClassloaderVersionAndReloadIfNecessary(xo.getExecutionProcessInstance(),
                                                                                   xo.getRootOrder().getRevision()));
      }
    } catch (XynaException e) {
      // in case of ClassNotFound- or IOException we'll continue with the old objects
      logger.warn("Error while trying to reload ProcessInstance", e);
    }
    try {
      if (xo.getInputPayload() != null) {
        xo.setInputPayload(checkClassloaderVersionAndReloadIfNecessary(xo.getInputPayload(), xo.getRootOrder().getRevision()));
      }
    } catch (XynaException e) {
      logger.warn("Error while trying to reload InputPayload", e);
    }
    try {
      if (xo.getOutputPayload() != null) {
        xo.setOutputPayload(checkClassloaderVersionAndReloadIfNecessary(xo.getOutputPayload(), xo.getRootOrder().getRevision()));
      }
    } catch (XynaException e) {
      logger.warn("Error while trying to reload OutputPayload", e);
    }
    try {
      if (xo.hasError()) {
        Collection<XynaException> reloadedErrors = new ArrayList<XynaException>();
        for (XynaException xe : xo.getErrors()) {
          //build a new collection
          reloadedErrors.add(checkClassloaderVersionAndReloadIfNecessary(xe, xo.getRootOrder().getRevision()));
        }
        //clear the old erros
        xo.clearErrors();
        //add the new ones 
        for (XynaException xe : reloadedErrors) {
          xo.addException(xe, ProcessingStage.OTHER);
        }
      }
    } catch (XynaException e) {
      logger.warn("Error while trying to reload XynaExceptions", e);
    }

  }


  private void reloadCronLikeOrders(DeploymentProcess fromThisProcess) {
    // cronLikeOrders in the queue (only reload)
    Collection<CronLikeOrder> cronOrderReferences = new HashSet<CronLikeOrder>();
    for (CronLikeOrder queuedOrder : XynaFactory.getInstance().getProcessing().getXynaScheduler()
        .getCronLikeScheduler().getAllQueuedOrders()) {
      cronOrderReferences.add(queuedOrder);
    }

    // xynaOrders that were held from the algorithm (only reload)
    for (OrderFilter of : fromThisProcess.getOrderFilters()) {
      cronOrderReferences.addAll(OrderFilterAlgorithmsImpl.getInstance().getUnstartedAffectedCrons(of));
    }

    Iterator<CronLikeOrder> iterCron = cronOrderReferences.iterator();
    cronOrderIteration : while (iterCron.hasNext()) {
      CronLikeOrder cron = iterCron.next();
      // update the deploymentId for all reachable orders
      cron.getCreationParameters().setIdOfLatestDeploymentKnownToOrder(getLatestDeploymentId());
      try {
        Collection<WorkflowRevision> workflowIdentifiers =
            getWorkflowIdentifersForOrderType(cron.getCreationParameters().getDestinationKey());
        for (WorkflowRevision workflowIdentifier : workflowIdentifiers) {
          if (getAllProtectedWorkflows().contains(workflowIdentifier)) {
            reloadXynaOrderCreationParameter(cron.getCreationParameters(), cron.getRevision());
            continue cronOrderIteration;
          }
        }
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        //we ignore it during cleanup and do as much as we can
        continue cronOrderIteration;
      }
    }

    // cronLikeOrders in persistence (reload and persist)
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<CronLikeOrder> ordersToPersist = new ArrayList<CronLikeOrder>();
      // would be better if we get them with a 'select for update'
      FactoryWarehouseCursor<SerialVersionIgnoringCronLikeOrder> cursor = getCronLikeOrderCursor(con, 50);
      try {
        while (true) {
          List<? extends CronLikeOrder> list = cursor.getRemainingCacheOrNextIfEmpty();
          if (list == null || list.size() == 0) {
            break;
          }
          for (CronLikeOrder clo : list) {
            // For orders from persistence there is no need to reload for classloaders, as they already are loaded from a new one
            // but they have to be renewed for the serialversionuid

            if (clo.getCreationParameters() != null && clo.getCreationParameters().getDestinationKey() != null
                && clo.getCreationParameters().getDestinationKey().getOrderType() != null) {
              try {
                for (WorkflowRevision workflowidentifier : getWorkflowIdentifersForOrderType(clo.getCreationParameters().getDestinationKey())) {
                  if (getAllProtectedWorkflows().contains(workflowidentifier)) {
                    ordersToPersist.add(clo);
                    break;
                  }
                }
              } catch (XPRC_DESTINATION_NOT_FOUND e) {
                ordersToPersist.add(clo);
              }
            }
          }
          list = cursor.getRemainingCacheOrNextIfEmpty();
        }
        if (logger.isDebugEnabled()) {
          logger.debug("going to persist: " + ordersToPersist.size()+" cron like orders");
        }
      } finally {
        cursor.close();
      }
      con.persistCollection(ordersToPersist);
      con.commit();
    } catch (PersistenceLayerException e) {
      // don't abort, but we could need to delete the now probably corrupted entries
      logger.error("Error while retrieving orderBackup for reload");
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        // don't abort
        logger.warn("Error while closing connection for orderBackup reload", e);
      }
    }


  }

  private void reloadFQTasks(DeploymentProcess fromThisProcess) {
    for (Long taskId : fromThisProcess.getPausedTaskIds()) {
      Collection<XynaOrderCreationParameter> ocps =
          ((FrequencyControlledOrderCreationTask) XynaFactory.getInstance().getProcessing().getFrequencyControl()
              .getActiveFrequencyControlledTask(taskId)).getOrderCreationParameters();
      for (XynaOrderCreationParameter xocp : ocps) {
        Long revision = getRevisionOrNull(xocp.getDestinationKey());
        if( revision == null ) {
          logger.error("Can't find revision for application " + xocp.getDestinationKey().getApplicationName());
          continue; // es ist davon auszugehen, dass es zu ClassCastException kommen wird, wenn wir die Payload mit irgendeinem ClassLoader erneuern
        }
        reloadXynaOrderCreationParameter(xocp, revision);
      }
    }
  }


  private void reloadXynaOrderCreationParameter(XynaOrderCreationParameter xocp, Long revision) {
    try {
      xocp.setInputPayloadDirectly(checkClassloaderVersionAndReloadIfNecessary(xocp.getInputPayload(), revision));
    } catch (Throwable e) {
      // in case of ClassNotFound- or IOException we'll continue with the old objects
      logger.warn("Error while trying to reload XynaOrderCreationParameter-InputPayload", e);
    }
  }


  public Set<WorkflowRevision> getWorkflowIdentifiersFromOrdersInPlanning() throws XPRC_DESTINATION_NOT_FOUND {
    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    Collection<XynaOrderServerExtension> orders = DispatcherType.Planning.getOrdersOfRunningProcesses();
    for (XynaOrderServerExtension order : orders) {
      List<XynaOrderServerExtension> childOrders;
      if (order.getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
        childOrders = ((RedirectionXynaOrder) order).getRedirectedOrder().getOrderAndChildrenRecursively();
      } else {
        childOrders = order.getOrderAndChildrenRecursively();
      }
      for (XynaOrderServerExtension childOrder : childOrders) {
        // TODO this really depends on the execution time, if there already is a processInstance, we should derive WorkflowRevision from it
        // if we haven't started execution we're fine with a dispatcherLookup
        workflowIdentifiers.addAll(getWorkflowIdentifersForOrderType(childOrder.getDestinationKey()));
      }
    }

    return workflowIdentifiers;

  }


  public Set<WorkflowRevision> getWorkflowIdentifiersFromOrdersInExecution() throws XPRC_DESTINATION_NOT_FOUND {
    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    Collection<XynaOrderServerExtension> orders = DispatcherType.Execution.getOrdersOfRunningProcesses();
    for (XynaOrderServerExtension order : orders) {
      List<XynaOrderServerExtension> childOrders;
      if (order.getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
        childOrders = ((RedirectionXynaOrder) order).getRedirectedOrder().getOrderAndChildrenRecursively();
      } else {
        childOrders = order.getOrderAndChildrenRecursively();
      }
      for (XynaOrderServerExtension childOrder : childOrders) {
        workflowIdentifiers.addAll(getWorkflowIdentifersForOrderType(childOrder.getDestinationKey()));
      }
    }
    return workflowIdentifiers;
  }


  public Set<WorkflowRevision> getWorkflowIdentifiersFromOrdersInCleanup() throws XPRC_DESTINATION_NOT_FOUND {

    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    Collection<XynaOrderServerExtension> orders = DispatcherType.Cleanup.getOrdersOfRunningProcesses();
    for (XynaOrderServerExtension order : orders) {
      List<XynaOrderServerExtension> childOrders;
      if (order.getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
        childOrders = ((RedirectionXynaOrder) order).getRedirectedOrder().getOrderAndChildrenRecursively();
      } else {
        childOrders = order.getOrderAndChildrenRecursively();
      }
      for (XynaOrderServerExtension childOrder : childOrders) {
        workflowIdentifiers.addAll(getWorkflowIdentifersForOrderType(childOrder.getDestinationKey()));
      }
    }

    return workflowIdentifiers;

  }


  public Map<WorkflowRevision,List<Long>> getWorkflowIdentifiersAndOrderIdsFromOrdersInSuspension(Long revision, ODSConnection defaultConnection)
      throws XPRC_DESTINATION_NOT_FOUND, PersistenceLayerException {

    Map<WorkflowRevision,List<Long>> workflows = null;
    FactoryWarehouseCursor<OrderInstanceBackup> suspendedMap =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .getAllSuspendedNoDetails(revision, defaultConnection);
    List<OrderInstanceBackup> suspendedInstances = suspendedMap.getRemainingCacheOrNextIfEmpty();

    while (suspendedInstances != null && suspendedInstances.size() > 0) {
      for (OrderInstanceBackup order : suspendedInstances) {
        List<XynaOrderServerExtension> childOrders;

        if (order.getXynaorder() == null) {
          //kann passieren, falls z.b. die xynaorder nicht deserialisierbar ist.
          //das sollte aber dann hier nicht zu npes führen, schliesslich wird absichtlich
          //der orderbackup-reader verwendet, der damit zurecht kommt.

          //so ein fehler wurde bereits vom factorywarehousecursor geloggt.
        } else {
          if (order.getXynaorder().getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
            childOrders =
                ((RedirectionXynaOrder) order.getXynaorder()).getRedirectedOrder().getOrderAndChildrenRecursively();
          } else {
            childOrders = order.getXynaorder().getOrderAndChildrenRecursively();
          }
          if( childOrders != null && ! childOrders.isEmpty() ) {
            if (workflows == null) {
              workflows = new HashMap<WorkflowRevision,List<Long>>();
            }
            for (XynaOrderServerExtension childOrder : childOrders) {
              addWorkflows( workflows, childOrder );
            }
          }
        }
      }

      suspendedInstances = suspendedMap.getRemainingCacheOrNextIfEmpty();
    }

    if (workflows == null) {
      return Collections.emptyMap();
    }

    return workflows;
  }


  private void addWorkflows(Map<WorkflowRevision, List<Long>> workflows, XynaOrderServerExtension childOrder) throws XPRC_DESTINATION_NOT_FOUND {
    Collection<WorkflowRevision> workflowIdentifiers = 
        getWorkflowIdentifersForOrderType(childOrder.getDestinationKey());
    for( WorkflowRevision wr : workflowIdentifiers ) {
      //TODO warum hier auch Planning- und Cleanup-Destination?
      List<Long> list = workflows.get(wr);
      if( list == null ) {
        list = new ArrayList<Long>();
        workflows.put(wr,list);
      }
      list.add( childOrder.getId() );
    }
  }


  public Set<WorkflowRevision> getWorkflowIdentifiersFromWaitingOrders() throws XPRC_DESTINATION_NOT_FOUND {
    Set<WorkflowRevision> workflowIdentifiers = new HashSet<WorkflowRevision>();
    
    List<XynaOrderInfo> rootOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList().getRootOrders();
    for( XynaOrderInfo ro : rootOrders ) {
      if ( ro.getDestinationKey().getOrderType().equals(MIAbstractionLayer.ORDERTYPE)) {
        //TODO was nun?
      } else {
        workflowIdentifiers.addAll(getWorkflowIdentifersForDestination(ro.getDestinationKey()));
      }
    }
    return workflowIdentifiers;
  }

  /**
   * Überprüft, ob das übergebene objekt (inkl dem daran hängenden objektgraph von modellierten objekten) mit richtigem classloader geladen ist.
   * der richtige classloader ist einer, der von der übergebenen rootRevision aus erreichbar ist.<p>
   * fall 1. bestehender classloader ist alt (passt zwar von der revision her, ist aber nicht aktuell)<p>
   * fall 2. bestehender classloader gehört zu nicht erreichbarer revision<p>
   * <br>
   * falls classloader nicht richtig ist, wird das objekt neu mit dem passenden classloader geladen.
   */
  public static <O extends Serializable> O checkClassloaderVersionAndReloadIfNecessary(O object, long rootRevision) throws XynaException {
    if (object == null) {
      return object;
    } 

    //versuchen, nicht den gesamten objektgraphen neu zu laden, wenn auch ein teilgraph ausreicht
    //das geht nur rekursiv, wenn man die im referenzierten objekte gut finden kann.
    boolean recursionPossible = object instanceof Container || object instanceof GeneralXynaObjectList;
    boolean recursionImPossible = !recursionPossible /*damit generalxynaobject nicht alles trifft*/
         && (object instanceof GeneralXynaObject || object instanceof XynaProcess);

    if (recursionImPossible
        || isUsedClassLoaderTheCurrentlyResponsibleForGivenRevision(object, rootRevision) //immer false (aber falls man an recursion(Im)Possible mal etwas ändert, passt es so immer noch
    ) {
      SerializableClassloadedObject wrapper =
          new SerializableClassloadedObject(object, getFactoryClassLoaderForObject(object, rootRevision));
      ByteArrayOutputStream serializedBytes = null;
      ObjectOutputStream objectOutStream = null;
      try {
        serializedBytes = new ByteArrayOutputStream();
        objectOutStream = new ObjectOutputStream(serializedBytes);
        try {
          objectOutStream.writeObject(wrapper);
          objectOutStream.flush();
        } finally {
          objectOutStream.close();
        }

        ByteArrayInputStream bytesToDeserialize;
        ObjectInputStream objectInStream = null;

        bytesToDeserialize = new ByteArrayInputStream(serializedBytes.toByteArray());
        serializedBytes = null;
        objectInStream = new SerialVersionIgnoringObjectInputStream(bytesToDeserialize, rootRevision);
        try {
          SerializableClassloadedObject deserialisedWrapper = (SerializableClassloadedObject) objectInStream.readObject();
          return (O) deserialisedWrapper.getObject();
        } finally {
          objectInStream.close();
        }
      } catch (IOException e) {
        throw new XPRC_ErrorDuringReloadOfGeneratedObjects("Error while writing or reading generated code into/from buffers: ", e);
      } catch (ClassNotFoundException e) {
        throw new XPRC_ErrorDuringReloadOfGeneratedObjects("Could not resolve class of reloaded object", e);
      }
    } else {
      // not outdated, rekursion

      if (object instanceof GeneralXynaObjectList) {
        GeneralXynaObjectList<?> xol = (GeneralXynaObjectList<?>) object;
        GeneralXynaObjectList xolNew = new GeneralXynaObjectList(null, xol.getOriginalXmlName(), xol.getOriginalXmlPath());
        for (GeneralXynaObject xo : xol) {
          xolNew.add(checkClassloaderVersionAndReloadIfNecessary(xo, rootRevision));
        }
        return (O) xolNew;
      } else if (object instanceof Container) {
        Container container = (Container) object;
        Container newContainer = new Container();
        for (int i = 0; i < container.size(); i++) {
          newContainer.add(checkClassloaderVersionAndReloadIfNecessary(container.get(i), rootRevision));
        }
        return (O) newContainer;
        //} else if (object instanceof GeneralXynaObject) {
        //Achtung, bei Rekursion müssen auch oldVersions von MemberVars berücksichtigt werden
        //} else if (object instanceof XynaProcess) {
        //Achtung, bei Rekursion müssen auch ScopeSteps (For-Eaches) berücksichtigt werden. Evtl noch mehr?
      }

      return object;
    }
  }


  public static boolean isUsedClassLoaderTheCurrentlyResponsibleForGivenRevision(Object object, Long rootRevision) throws XynaException {
    ClassLoader usedClassLoader = object.getClass().getClassLoader();
    if (logger.isDebugEnabled()) {
      logger.debug("usedClassLoader: " + usedClassLoader);
    }
    if (usedClassLoader instanceof ClassLoaderBase) {
      ClassLoaderBase usedClassLoaderBase = (ClassLoaderBase) usedClassLoader;
      String identifier = object.getClass().getCanonicalName();
      if (object instanceof XynaProcess) {
        ClassLoader factoryClassLoader =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                .getWFClassLoader(identifier, rootRevision, true);
        if (logger.isDebugEnabled()) {
          logger.debug("XynaProcess: " + factoryClassLoader);
        }
        return !(usedClassLoader == factoryClassLoader);
      } else if (object instanceof XynaObject) {
        ClassLoader factoryClassLoader =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                .getMDMClassLoader(identifier, rootRevision, true);
        if (logger.isDebugEnabled()) { 
          logger.debug("XynaObject: " + factoryClassLoader);
        }
        return !(usedClassLoader == factoryClassLoader);
      } else if (object instanceof XynaExceptionBase) {
        ClassLoader factoryClassLoader =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                .getExceptionClassLoader(identifier, rootRevision, true);
        if (logger.isDebugEnabled()) {
          logger.debug("XynaExceptionBase: " + factoryClassLoader);
        }
        return !(usedClassLoader == factoryClassLoader);
      } else {
        //if it's not generated it should not be outdated
        if (logger.isDebugEnabled()) {
          logger.debug("not generated: " + object);
        }
        return false;
      }
    } else {
      //if it's not generated it should not be outdated
      if (logger.isDebugEnabled()) {
        logger.debug("not generated: " + object);
      }
      return false;
    }

  }


  private static ClassLoader getFactoryClassLoaderForObject(Serializable object, Long revision)
      throws XFMG_MDMObjectClassLoaderNotFoundException, XFMG_ExceptionClassLoaderNotFoundException,
      XFMG_WFClassLoaderNotFoundException {
    String identifier = object.getClass().getCanonicalName();
    if (GenerationBase.isReservedServerObjectByFqClassName(identifier)) {
      return GenerationBase.class.getClassLoader();
    }
    ClassLoader factoryClassLoader = null;
    if (object instanceof XynaProcess) {
      factoryClassLoader =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .getWFClassLoader(identifier, revision, true);

    } else if (object instanceof XynaObject) {
      factoryClassLoader =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .getMDMClassLoader(identifier, revision, true);
      return factoryClassLoader;
    } else if (object instanceof XynaExceptionBase) {
      factoryClassLoader =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .getExceptionClassLoader(identifier, revision, true);
      return factoryClassLoader;
    }
    return factoryClassLoader;
  }


  // FIXME querying default only does not help if default & history are the same physical table, status condition is missing
  private void failFastIfOrdertypeInOrderArchive() throws XPRC_WorkflowProtectionModeViolationException {
    Set<WorkflowRevision> affectedBreakers =
        affectedWorkflowsPerProtectionMode.get(WorkflowProtectionMode.BREAK_ON_USAGE);
    if (affectedBreakers != null && affectedBreakers.size() > 0) {
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        Parameter params = new Parameter();
        StringBuilder queryBuilder = new StringBuilder("select ");
        Set<OrderInstanceColumn> relevantColumns = new HashSet<>(Arrays.asList(new OrderInstanceColumn[] {OrderInstanceColumn.C_APPLICATIONNAME,
                                                                                                          OrderInstanceColumn.C_ID,
                                                                                                          OrderInstanceColumn.C_ORDER_TYPE,
                                                                                                          OrderInstanceColumn.C_VERISONNAME,
                                                                                                          OrderInstanceColumn.C_WORKSPACENAME}));
        for (OrderInstanceColumn column : relevantColumns) {
          queryBuilder.append(column.getColumnName());
          queryBuilder.append(", ");
        }
        queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length());
        queryBuilder.append(" from ")
                    .append(oa.getAuditAccess().getQueryBackingClass(con).getTableName())
                    .append(" where ");
        Iterator<WorkflowRevision> affectedIterator = affectedBreakers.iterator();
        while (affectedIterator.hasNext()) {
          queryBuilder.append(OrderInstance.COL_ORDERTYPE).append(" = ?");
          params.add(affectedIterator.next().wfFqClassName);
          if (affectedIterator.hasNext()) {
            queryBuilder.append(" or ");
          }
        }
        try {
          PreparedQuery<? extends OrderInstance> countQuery =
              queryCache.getQueryFromCache(queryBuilder.toString(), con, new OrderInstance.DynamicOrderInstanceReader(relevantColumns));
          Collection<? extends OrderInstance> orders = con.query(countQuery, params, -1);
          int globalCount = 0;
          Map<String, Pair<Integer, List<Long>>> ordertypeCount = new HashMap<>();
          for (OrderInstance order : orders) {
            globalCount++;
            Pair<Integer, List<Long>> entry = ordertypeCount.get(order.getOrderType());
            if (entry == null) {
              entry = new Pair<Integer, List<Long>>(0, new ArrayList<Long>());
              ordertypeCount.put(order.getOrderType(), entry);
            }
            Integer count = entry.getFirst();
            if (count == null) {
              count = 0;
            }
            count++;
            entry.setFirst(count);
            entry.getSecond().add(order.getId());
          }
          if (globalCount > 0) {
            StringBuilder exceptionOrdertypeBuilder = new StringBuilder(Constants.LINE_SEPARATOR);
            exceptionOrdertypeBuilder.append(globalCount)
                                     .append(" violations detected.")
                                     .append(Constants.LINE_SEPARATOR);
            for (Entry<String, Pair<Integer, List<Long>>> entry : ordertypeCount.entrySet()) {
              exceptionOrdertypeBuilder.append("  ")
                                       .append(entry.getValue().getFirst())
                                       .append(" ")
                                       .append(entry.getKey())
                                       .append(": ");
              if (entry.getValue().getSecond().size() > 10) {
                exceptionOrdertypeBuilder.append(entry.getValue().getSecond().subList(0, 10).toString())
                                         .append(" ...");
              } else {
                exceptionOrdertypeBuilder.append(entry.getValue().getSecond().toString());
              }
                                       
              exceptionOrdertypeBuilder.append(Constants.LINE_SEPARATOR);
            }
            exceptionOrdertypeBuilder.delete(exceptionOrdertypeBuilder.length() - 1, exceptionOrdertypeBuilder.length());
            getActiveDeploymentProcess().abortDeploymentProcess(new XPRC_MassWorkflowProtectionModeViolationException(exceptionOrdertypeBuilder.toString()), exceptionOrdertypeBuilder.toString());
          }
        } catch (XPRC_WorkflowProtectionModeViolationException e) {
          throw e;
        } catch (Throwable e) {
          logger.warn("Could not use fail fast mechanism for deployment: " + e.getMessage());
          // no reason to fail, we just can't fail fast
        }
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Failed to close connection", e);
        }
      }
    }
  }

  /**
   * filter für alle aufträge einer revision, abzüglich interner aufträge ({@link XynaDispatcher#INTERNAL_ORDER_TYPES}
   */
  private static class RevisionOrderFilter implements OrderFilter {

    private final long revision;


    public RevisionOrderFilter(long revision) {
      this.revision = revision;
    }


    private boolean isOrderToBeFiltered(XynaOrderServerExtension xo) {
      return isToBeFiltered(xo.getRevision(), xo.getDestinationKey());
    }
    
    
    private boolean isToBeFiltered(long revision, DestinationKey destinationKey) {
      if (this.revision == revision) {
        if (!OrdertypeManagement.internalOrdertypes.contains(destinationKey.getOrderType())) {
          return true;
        }
      }
      return false;
    }


    public boolean filterForAddOrderToScheduler(XynaOrderServerExtension xo) {
      return isOrderToBeFiltered(xo);
    }


    public boolean filterForCheckOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type) {
      return isOrderToBeFiltered(xo);
    }


    public boolean startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp, ResponseListener rl) {
      return isToBeFiltered(cronLikeOrder.getRevision(), clocp.getDestinationKey());
    }


    public void continueOrderReadyForProcessing(XynaOrderServerExtension xo) {
      //ntbd
    }

  }


  /**
   * auftragseingangsschnittstellen müssen bereits geschlossen sein.
   * internalorders werden ignoriert, weil sie keine deploybaren objekte verwenden.
   * @return true, falls in der revision auf diesem knoten irgendein auftrag am laufen ist
   */
  public boolean isInUse(final long revision) throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException, PersistenceLayerException {
    RevisionOrderFilter of = new RevisionOrderFilter(revision);
    blockWorkflowProcessingForDeployment(of);
    long myID = DeploymentManagement.getInstance().propagateDeployment();
    try {
      //processoren
      for (XynaOrderServerExtension xo : DispatcherType.Execution.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          return true;
        }
      }

      for (XynaOrderServerExtension xo : DispatcherType.Planning.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          return true;
        }
      }

      for (XynaOrderServerExtension xo : DispatcherType.Cleanup.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          return true;
        }
      }

      //scheduler
      RuntimeContext runtimeContext;
      try {
        runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // TODO was nun?
        throw new RuntimeException("No workspace or application found for revision "+revision, e);
      }
      List<XynaOrderInfo> ordersInScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList().getOrdersInRuntimeContext(runtimeContext);
      if( ! ordersInScheduler.isEmpty() ) {
        return true;
      }

      //frequencycontrolled tasks
      Map<Long, List<DestinationKey>> fqWorkflows = getActiveFQTaskWorkflows();
      for( Map.Entry<Long, List<DestinationKey>> entry : fqWorkflows.entrySet() ) {
        for (DestinationKey dk : entry.getValue()) {
          if (of.isToBeFiltered(getRevisionOrNull(dk), dk)) {
            return true;
          }
        }
      }

      //crons
      int cnt = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().countCronLikeOrders(revision);
      if (cnt > 0) {
        return true;
      }

      //orderbackup checken
      cnt = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().countBackuppedOrders(revision);
      if (cnt > 0) {
        return true;
      }

      //warten, bis keine aufträge mehr in den zwischenräumen sind und deploymentalgorithm alle solchen aufträge kennt.
      waitForUnreachableOrders();

      //deploymentalgorithms checken (als letztes, weil aufträge könnten sich ja von oben geprüften stellen hierhin bewegen)
      if (OrderFilterAlgorithmsImpl.getInstance().countFilteredBy(of) > 0) {
        return true;
      }
    } finally {
      unblockWorkflowProcessingForDeployment(of, myID);
    }
    return false;
  }


  public void unblockWorkflowProcessingForDeployment(OrderFilter of, long myID) {
    orderCounter.cleanup(Arrays.asList(new Long[] {myID}));
    OrderFilterAlgorithmsImpl.getInstance().removeOrderFilter(of);
  }


  public void blockWorkflowProcessingForDeployment(OrderFilter of) {
    OrderFilterAlgorithmsImpl.getInstance().addFilter(of);
  }


  /**
   * gibt alle gerade laufenden aufträge (inkl der suspendierten) zurück und alle crons und alle frequencycontrolled tasks.
   * internalorders und crons die internalorders starten werden ignoriert.
   * alles nur für die übergebene revision und ohne die aufträge auf dem anderen knoten.
   */
  public OrdersInUse getInUse(long revision, FillingMode fillingMode) throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException, PersistenceLayerException {
    OrdersInUse result = new OrdersInUse(fillingMode);
    RevisionOrderFilter of = new RevisionOrderFilter(revision);
    int binding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);

    //laufende Batch Prozesse bestimmen
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    List<BatchProcess> batchProcesses = bpm.getBatchProcesses(revision);
    for (BatchProcess bp : batchProcesses) {
      result.addBatchProcess(bp, binding);
    }
    
    blockWorkflowProcessingForDeployment(of);
    long myID = DeploymentManagement.getInstance().propagateDeployment();
    try {
      //processoren
      for (XynaOrderServerExtension xo : DispatcherType.Execution.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          XynaOrderInfo oi = new XynaOrderInfo(xo, OrderInstanceStatus.RUNNING_EXECUTION);
          result.addRootOrder(oi, binding);
        }
      }

      for (XynaOrderServerExtension xo : DispatcherType.Planning.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          XynaOrderInfo oi = new XynaOrderInfo(xo, OrderInstanceStatus.RUNNING_PLANNING);
          result.addRootOrder(oi, binding);
        }
      }

      for (XynaOrderServerExtension xo : DispatcherType.Cleanup.getOrdersOfRunningProcesses()) {
        if (of.isOrderToBeFiltered(xo)) {
          XynaOrderInfo oi = new XynaOrderInfo(xo, OrderInstanceStatus.RUNNING_CLEANUP);
          result.addRootOrder(oi, binding);
        }
      }

      //scheduler
      RuntimeContext runtimeContext;
      try {
        runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // TODO was nun?
        throw new RuntimeException("No workspace or application found for revision "+revision, e);
      }
      for (XynaOrderInfo oi : XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList().getOrdersInRuntimeContext(runtimeContext) ) {
        result.addRootOrder(oi, binding);
      }

      //frequencycontrolled tasks
      result.addOrders(getFrequencyControlledTasks(revision, fillingMode, binding));

      //crons
      CronLikeScheduler cls = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();
      int cnt = cls.countCronLikeOrders(revision);
      if (cnt > 0) {
        ODSConnection con = ods.openConnection();
        try {
          FactoryWarehouseCursor<CronLikeOrder> cursor = cls.getCursorForCronLikeOrders(con, 50, revision, null, false);
          List<CronLikeOrder> clos = cursor.getRemainingCacheOrNextIfEmpty();
          while (clos.size() > 0) {
            for (CronLikeOrder clo : clos) {
              if (of.isToBeFiltered(clo.getRevision(), clo.getCreationParameters().getDestinationKey())) {
                result.addCron(clo);
              }
            }

            clos = cursor.getRemainingCacheOrNextIfEmpty();
          }
        } finally {
          con.closeConnection();
        }
      }

      //orderbackup checken
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      cnt = oa.countBackuppedOrders(revision);
      if (cnt > 0) {
        ODSConnection con = ods.openConnection();
        try {
          FactoryWarehouseCursor<? extends OrderInstanceBackup> cursor = oa.getCursorForOrderBackupEntries(con, 50, revision, false);
          List<? extends OrderInstanceBackup> oibs = cursor.getRemainingCacheOrNextIfEmpty();
          while (oibs.size() > 0) {
            for (OrderInstanceBackup oib : oibs) {
              result.addRootOrder(oib);
            }

            oibs = cursor.getRemainingCacheOrNextIfEmpty();
          }
        } finally {
          con.closeConnection();
        }
      }

      //warten, bis keine aufträge mehr in den zwischenräumen sind und deploymentalgorithm alle solchen aufträge kennt.
      waitForUnreachableOrders();

      //deploymentalgorithms checken (als letztes, weil aufträge könnten sich ja von oben geprüften stellen hierhin bewegen)
      for (XynaOrderServerExtension xo : OrderFilterAlgorithmsImpl.getInstance().getOrdersHeldAtProcessors(of)) {
        if (of.isOrderToBeFiltered(xo)) {
          //Status anhand der XynaOrder bestimmen
          OrderInstanceStatus status = getStatusFromXynaOrder(xo);
          XynaOrderInfo oi = new XynaOrderInfo(xo, status);
          result.addRootOrder(oi, binding);
        }
      }

      for (CronLikeOrder clo : OrderFilterAlgorithmsImpl.getInstance().getUnstartedAffectedCrons(of)) {
        if (of.isToBeFiltered(clo.getRevision(), clo.getCreationParameters().getDestinationKey())) {
          result.addCron(clo);
        }
      }
    } finally {
      unblockWorkflowProcessingForDeployment(of, myID);
    }
    return result;
  }

  private OrderInstanceStatus getStatusFromXynaOrder(XynaOrderServerExtension order) {
    if (order.getExecutionProcessInstance() == null) {
      return OrderInstanceStatus.RUNNING_PLANNING;
    }
    if (order.getOutputPayload() == null){
      return OrderInstanceStatus.RUNNING_EXECUTION;
    }
    
    return OrderInstanceStatus.RUNNING_CLEANUP;
  }
  
  private static Long getRevisionOrNull(DestinationKey dk) {
    try {
       return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
  }

  
  public OrdersInUse getFrequencyControlledTasks(long revision, FillingMode fillingMode, int binding) {
    RevisionOrderFilter of = new RevisionOrderFilter(revision);
    OrdersInUse result = new OrdersInUse(fillingMode);
    Map<Long, List<DestinationKey>> fqWorkflows = getActiveFQTaskWorkflows();
    for( Map.Entry<Long, List<DestinationKey>> entry : fqWorkflows.entrySet() ) {
      for (DestinationKey dk : entry.getValue()) {
        if (of.isToBeFiltered(getRevisionOrNull(dk), dk)) {
          result.addFrequencyControlledTask( entry.getKey(), dk.getOrderType(), binding);
        }
      }
    }
    return result;
  }
  
  
  public static class DeploymentAuditReloader implements AuditReloader {
    
    private final Long revision;
    
    public DeploymentAuditReloader(Long revision) {
      this.revision = revision;
    }

    public GeneralXynaObject reload(GeneralXynaObject gxo) throws XynaException {
      return DeploymentManagement.checkClassloaderVersionAndReloadIfNecessary(gxo, revision);
    }
    
  }

   
}
