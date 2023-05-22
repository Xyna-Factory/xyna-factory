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
package com.gip.xyna.xprc.xsched;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.concurrent.CancelableDelayedTask;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.ForeignDataStore;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.DatabaseLock;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_ClusterStateChangedException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.capacities.CMClustered;
import com.gip.xyna.xprc.xsched.capacities.CMLocal;
import com.gip.xyna.xprc.xsched.capacities.CMUnsupported;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache;
import com.gip.xyna.xprc.xsched.capacities.CapacityChangeListener;
import com.gip.xyna.xprc.xsched.capacities.CapacityManagementInterface;
import com.gip.xyna.xprc.xsched.capacities.CapacityStorableQueries;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;
import com.gip.xyna.xprc.xsched.capacities.UpdateAllCapacityStorables;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;



public class CapacityManagement extends FunctionGroup
    implements
      CapacityManagementInterface,
      Clustered {

  /**
   * Reaktion auf Capacity-Probleme (Cap existiert nicht, Cap liegt nicht in der ben�tigten Cardinality vor)
   */
  public enum CapacityProblemReaction {
    Wait,         //Auftrag wartet, bis Cap-Problem gel�st ist
    Fail,         //Auftrag mit Exception abbrechen
    Schedule;    //trotzdem schedulen
  }

  public static final String DEFAULT_NAME = "Capacity Management";
  
  public static final String MANAGEMENT_LOCK_NAME = DEFAULT_NAME + "-lock";

  /**
   * Cache, auf dem gescheduled wird, um nicht st�ndig auf den PersistenceLayer zugreifen zu m�ssen
   */
  private final CapacityCache cache = new CapacityCache();

  /**
   * Sicherung gegen �nderungen an Capacity-Namen, Anzahl etc.
   */
  private DatabaseLock managementLock;

  private volatile CapacityManagementInterface cmAlgorithm = new CMUnsupported(cache, CMUnsupported.Cause.Unitialized);

  private ODS ods;

  private CapacityStorableQueries capacityStorableQueries;

  private int ownBinding;

  private ClusterContext rmiClusterContext;
  private ClusterContext storableClusterContext;

  private CMClusterStateChangeHandler capacityClusterStateChangeHandler = new CMClusterStateChangeHandler();
  private FactoryShutdownClusterStateChangeHandler factoryShutdownClusterStateChangeHandler = new FactoryShutdownClusterStateChangeHandler();
  private RMIClusterStateChangeHandler rmiClusterStateChangeHandler;
  
  private ForeignDataStore<CapacityInformation> capacityStatisticsStore;
    
  private class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {
    private ClusterState clusterState;
    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }
    public void onChange(ClusterState newState) {
      //Dieser ClusterStateChangeHandler muss fast nichts machen, da alle wichtigen �berg�nge 
      //vom CMClusterStateChangeHandler erledigt werden, da dieser die wesentlich genaueren 
      //Status�berg�nge des StorableClusterContext beobachtet.
      //Lediglich der �bergang nach CONNECTED muss beobachtet werden, da hier das Setzen des 
      //CMAlgorithm erst erfolgen darf, wenn beide ClusterContext im Zustand CONNECTED sind
      if( newState == ClusterState.CONNECTED ) {
        cmAlgorithmBuilder.tryClustered();
        if (logger.isInfoEnabled()) {
          logger.info("CapacityManagement-Algorithm is " + cmAlgorithm.getClass().getSimpleName()
            + " after rmiClusterContext-stateChange " + clusterState + "->" + newState);          
        }
      }
      clusterState = newState;
    }

  };
  
  private class FactoryShutdownClusterStateChangeHandler implements ClusterStateChangeHandler {

    private volatile Timer shutdownTimer = new Timer();
    private volatile boolean shutDownInitialized = false;
    private ClusterState state;
    
    public boolean isReadyForChange(ClusterState newState) {
      if( shutDownInitialized ) {
        return newState == ClusterState.DISCONNECTED_SLAVE; //nur noch DISCONNECTED_SLAVE zulassen
      } else {
        return true;
      }
    }

    public void onChange(ClusterState newState) {
      ClusterState lastState = state;
      if (logger.isDebugEnabled()) {
        logger.debug("FactoryShutdownClusterStateChangeHandler got notified of cluster state change "+lastState +" -> "+ newState );
      }
      if( lastState == newState ) {
        return; //nichts zu tun
      }
      state = newState;
      if( lastState == null ) {
        return; //nichts zu tun
      }

      switch (newState) {
        case CONNECTED :
          // make sure that the shutdown timer is not executed if the transition to DISCONNECTED_SLAVE has just
          // happened.
          /*logger.debug("Trying to abort the shutdown.");
          Timer lastTimer = shutdownTimer;
          shutdownTimer = new Timer();
          lastTimer.cancel();
          break;*/
          break;
        case DISCONNECTED_SLAVE :
          // shut down the factory with a delay to make sure that the other listeners can
          // still be called
          shutdown(newState,2000);
          break;
        default :
          // nothing to be done
      }
    }
    
    private void shutdown(ClusterState newState, long delay) {
      if (logger.isInfoEnabled()) {
        logger.info("Got notified of cluster state change to state " + newState + ". Trying to shut down");
      }
      if (XynaFactory.getInstance().isShuttingDown()) {
        logger.info("XynaFactory is already shutting down.");
        return;
      }
      if (logger.isInfoEnabled()) {
        logger.info("Shutting down in " + delay + " ms");
      }
      
      shutDownInitialized = true; //keine weiteren ClusterState-�berg�nge zulassen
      shutdownTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          XynaFactory.getInstance().shutdown();
        }
      }, delay);
    }
  };
  


  // Refactoring the name will prevent the server from reading the old capacity information when starting using old
  // information
  public static enum State {
    ACTIVE, DISABLED
  }


  static {
    ArrayList<XynaFactoryPath> dependencies = new ArrayList<XynaFactoryPath>();
    // wait for the configuration class to be loaded to be able to read properties for the default configuration
    dependencies.add(new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class));
    addDependencies(CapacityManagement.class, dependencies);
    addDependencies(CapacityManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryControl.class,
                                                                           DependencyRegister.class)})));
  }


  CapacityManagement() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }



  /**
   */
  public void init() throws XynaException {
    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.CAPACITIES_DIRECTPERSISTENCE,
                       DependencySourceType.XYNAFACTORY, DEFAULT_NAME);
    
    XynaProperty.CLUSTERING_TIMEOUT_CAPACITY_MIGRATION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_CAPACITY_DEMAND_FOREIGN_PENALTY.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_CAPACITY_DEMAND_MAX_PERCENT.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    XynaProperty.SCHEDULER_UNSUFFICIENT_CAPACITY_REACTION .registerDependency(UserType.XynaFactory, DEFAULT_NAME);

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("CapacityManagement.initStorables", "CapacityManagement.initStorables").
      after(PersistenceLayerInstances.class).
      before(XynaClusteringServicesManagement.class).
      execAsync(new Runnable() { public void run() { initStorables(); }});
    fExec.addTask(CapacityManagement.class, "CapacityManagement.initUnclustered").
      after("CapacityManagement.initStorables").after(XynaClusteringServicesManagement.class).
      before(WorkflowDatabase.FUTURE_EXECUTION_ID).
      execAsync(new Runnable() { public void run() { initUnclustered(); }});
  }

  private void initStorables() {
    rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
    rmiClusterContext = new ClusterContext();
    rmiClusterContext.addClusterStateChangeHandler( rmiClusterStateChangeHandler );
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + CapacityManagement.class.getSimpleName() + " as clusterable component.", e);
    }
    
    try {
      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
      ods.registerStorable(CapacityStorable.class);

      managementLock =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaClusteringServices().getClusterLockingInterface()
            .createLockIfNonexistent(DEFAULT_NAME,ClusterLockingInterface.DatabaseLockType.InternalConnection );

      storableClusterContext = new ClusterContext( CapacityStorable.class, ODSConnectionType.DEFAULT );
      capacityClusterStateChangeHandler.setClusterContext(storableClusterContext);
      ods.addClusteredStorableConfigChangeHandler( storableClusterContext, ODSConnectionType.DEFAULT, CapacityStorable.class);

      storableClusterContext.addClusterStateChangeHandler( factoryShutdownClusterStateChangeHandler );
      storableClusterContext.addClusterStateChangeHandler( capacityClusterStateChangeHandler );

      capacityStorableQueries = new CapacityStorableQueries();
      ODSConnection defCon = ods.openConnection();
      try {
        capacityStorableQueries.init(defCon);
      } finally {
        defCon.closeConnection();
      }
            
    } catch (PersistenceLayerException e) {
      //FIXME bessere Behandlung?
      throw new RuntimeException(e);
    }
  }
  
  private void initUnclustered() {
    boolean rmiUnclustered = rmiClusterContext.getClusterState() == ClusterState.NO_CLUSTER;
    boolean storableUnclustered = storableClusterContext.getClusterState() == ClusterState.NO_CLUSTER;
    if( rmiUnclustered && storableUnclustered ) {
      //Initialisierung, wenn die Factory nicht geclusteret ist. In diesem Falle wird kein enableClustering gerufen 
      //und es gibt auch keinen Aufruf des ClusterStateChangeHandlers. 
      //Daher muss hier das CapacityManagement auf andere Weise fertig initialisiert werden
      CapacityStorable tmpInstance = new CapacityStorable();
      ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
      cmAlgorithmBuilder.local();
      if (logger.isInfoEnabled()) {
        logger.info("CapacityManagement-Algorithm is " + cmAlgorithm.getClass().getSimpleName()
            + " after unclustered init");
      }
    }

    Map<String, StatisticsPathPart> mapping = new HashMap<String, StatisticsPathPart>();
    mapping.put("Name", StatisticsPathImpl.simplePathPart("Name"));
    mapping.put("Cardinality", StatisticsPathImpl.simplePathPart("Cardinality"));
    mapping.put("Used", StatisticsPathImpl.simplePathPart("Used"));
    mapping.put("State", StatisticsPathImpl.simplePathPart("State"));
    capacityStatisticsStore = new ForeignDataStore<CapacityInformation>(mapping) {
      @Override
      public Object getKey(CapacityInformation holder) { return holder.getName(); }
      @Override
      public StatisticsPath getPathToHolder(CapacityInformation holder) {
        return PredefinedXynaStatisticsPath.CAPACITYMANAGEMENT.append(StatisticsPathImpl.simplePathPart(holder.getName())); 
      }

      @Override
      public Serializable getValueFromHolder(StatisticsPath path) {
        CapacityInformation holder = store.get(path.getPathPart(path.length() - 2).getPartName());
        if (path.getPathPart(path.length() - 1).getPartName().equals("Name")) {
          return holder.getName();
        } else if (path.getPathPart(path.length() - 1).getPartName().equals("Cardinality")) {
          return holder.getCardinality();
        } else if (path.getPathPart(path.length() - 1).getPartName().equals("Used")) {
          return holder.getInuse();
        } else if (path.getPathPart(path.length() - 1).getPartName().equals("State")) {
          return holder.getState();
        } else {
          return null;
        }
      }
      @Override
      public Collection<CapacityInformation> reload() {
        return listCapacities();
      }
    };
    capacityStatisticsStore.refresh();
  }


  public void shutdown() throws XynaException {
  }


  
  
  
  //Implementierung des Interface CapacityManagementInterface

  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData) {
    return cmAlgorithm.allocateCapacities(orderInformation,schedulingData);
  }
  
  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData) {
    cmAlgorithm.undoAllocation(orderInformation,schedulingData);
  }
  
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities) {
    return cmAlgorithm.transferCapacities(xo, transferCapacities);
  }
  
  public CapacityReservation getCapacityReservation() {
    return cmAlgorithm.getCapacityReservation();
  }

  public void close() {
    //cmAlgorithm.close();
    throw new UnsupportedOperationException("close ist not supported");
  }
  
  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED,
  PersistenceLayerException {
    try {
      return cmAlgorithm.addCapacity(name,cardinality,state);
    } finally {
      callCapacityChangedListeners(name);
      capacityStatisticsStore.refresh();
    }
  }
  
  public boolean changeCapacityName(String oldName, String newName) throws PersistenceLayerException {
    int retryCnt = 0;
    XPRC_ClusterStateChangedException lastException = null;
    while (retryCnt++ < 5) {
      try {
        return cmAlgorithm.changeCapacityName(oldName,newName);        
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger.debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
        lastException = e;
      } finally {
        callCapacityChangedListeners(oldName);
        capacityStatisticsStore.refresh();
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.", lastException);
  } 
  

  public boolean changeCardinality(String capName, int newOverallCardinality) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    int retryCnt = 0;
    XPRC_ClusterStateChangedException lastException = null;
    while (retryCnt++ < 5) {
      try {
        return cmAlgorithm.changeCardinality(capName,newOverallCardinality);
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger.debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
        lastException = e;
      } finally {
        callCapacityChangedListeners(capName);
        capacityStatisticsStore.refresh();
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.", lastException);
  }
  
  public boolean changeState(String capName, State newState) throws PersistenceLayerException {
    try {
      return cmAlgorithm.changeState(capName,newState);
    } finally {
      callCapacityChangedListeners(capName);
      capacityStatisticsStore.refresh();
    }
  }

  public boolean removeCapacity(String capName) throws PersistenceLayerException {
    int retryCnt = 0;
    XPRC_ClusterStateChangedException lastException = null;
    while (retryCnt++ < 5) {
      try {
        if ( cmAlgorithm.removeCapacity(capName) ) {
          Integer[] priorities = DeploymentHandling.allPriorities;
          
          for (int i = priorities.length - 1; i >= 0; i--) {
            try {
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                  .executeUndeploymentHandler(priorities[i], new Capacity(capName, 0));
            } catch (XPRC_UnDeploymentHandlerException e) {
              logger.warn("Call of undeployment handler failed.", e);
            }
          }
          
          return true;
        } else {
          return false;
        }
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger
              .debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
        lastException = e;
      } finally {
        callCapacityChangedListeners(capName);
        capacityStatisticsStore.refresh();
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.", lastException);
  }

  public void removeAllCapacities() throws PersistenceLayerException {
    cmAlgorithm.removeAllCapacities();
  }

  public boolean freeCapacities(XynaOrderServerExtension xo) {
    return cmAlgorithm.freeCapacities(xo);
  }
  
  public boolean freeTransferableCapacities(XynaOrderServerExtension xo) {
    return cmAlgorithm.freeTransferableCapacities(xo);
  }
  
  public boolean forceFreeCapacities(long orderId) {
    return cmAlgorithm.forceFreeCapacities(orderId);
  }
  
  public CapacityInformation getCapacityInformation(String capName) {
    int retryCnt = 0;
    while (retryCnt++ < 5) {
      try {
        return cmAlgorithm.getCapacityInformation(capName);
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger.debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.");
  }

  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    int retryCnt = 0;
    while (retryCnt++ < 5) {
      try {
        return cmAlgorithm.getExtendedCapacityUsageInformation();
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger.debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.");
  }

  public List<CapacityInformation> listCapacities() {
    int retryCnt = 0;
    while (retryCnt++ < 5) {
      try {
        return cmAlgorithm.listCapacities();
      } catch (XPRC_ClusterStateChangedException e) {
        //retry
        if (logger.isDebugEnabled()) {
          logger.debug("got " + XPRC_ClusterStateChangedException.class.getSimpleName() + ". retry with " + cmAlgorithm);
        }
      }
    }
    throw new RuntimeException("got clusterstatechangeexception several times.");
  }  
  
  //Implementierung des Interface Clustered
  
  public boolean isClustered() {
    return rmiClusterContext.isClustered();
  }

  public long getClusterInstanceId() {
    return rmiClusterContext.getClusterInstanceId();
  }

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    rmiClusterContext.enableClustering(clusterInstanceId);
  }
  
  public String getName() {
    return getDefaultName();
  }

  public void disableClustering() {
    rmiClusterContext.disableClustering();
  }


  private class CMAlgorithmBuilder {

    private void refreshCapacityCache() {
      CapacityStorable tmpInstance = new CapacityStorable();
      ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
      try {
        cache.refresh(ods, capacityStorableQueries, ownBinding);
        if (logger.isInfoEnabled()) {
          logger.info("read " + cache.getSize() + " capacities for binding " + ownBinding);
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Could not refresh capacity cache", e);
      }
    }

    public synchronized void tryClustered() {
      //Wunsch ist, dass CMClustered eingerichtet werden soll. Dies ist nur m�glich,
      //wenn beide ClusterContext auf CONNECTED stehen
      boolean rmiConnected = rmiClusterContext.getClusterState() == ClusterState.CONNECTED;
      boolean storableConnected = storableClusterContext.getClusterState() == ClusterState.CONNECTED;
      if( rmiConnected && storableConnected ) {
        if( cmAlgorithm instanceof CMClustered ) {
          //nichts zu tun
        } else {
          refreshCapacityCache();
          XynaScheduler scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
          cmAlgorithm = CMClustered.createCMClustered(ods, cache, ownBinding, capacityStorableQueries, 
                                        managementLock, rmiClusterContext, storableClusterContext,
                                        (ClusteredScheduler)scheduler);
        }
      } else {
        if (logger.isInfoEnabled()) {
          logger.info("Could not set CapacityManagement-Algorithm to CMClustered:"
              + (rmiConnected ? "" : " RMI not connected") + (storableConnected ? "" : " Storable not connected"));
        }
        if (cmAlgorithm instanceof CMLocal) {
          //nichts zu tun
        } else {
          refreshCapacityCache();
          cmAlgorithm = new CMLocal(ods, cache, ownBinding, capacityStorableQueries, managementLock);
        }
      }
    }

    public synchronized void local() {
      cmAlgorithm.close();
      refreshCapacityCache();
      cmAlgorithm = new CMLocal(ods, cache, ownBinding, capacityStorableQueries, managementLock);
    }

    public synchronized void unsupported(CMUnsupported.Cause cause) {
      cmAlgorithm.close();
      cmAlgorithm = new CMUnsupported(cache, cause);
    }

  }
  
  private CMAlgorithmBuilder cmAlgorithmBuilder = new CMAlgorithmBuilder();


  private class CMClusterStateChangeHandler implements ClusterStateChangeHandler {

    private volatile boolean isReadyForChange = true;
    private ClusterContext clusterContext;
    private CancelableDelayedTask cdt = new CancelableDelayedTask("CapacityAdoption");
    private Integer cdtId;
    
    public boolean isReadyForChange(ClusterState newState) {
      if( cdtId != null && newState != ClusterState.DISCONNECTED_MASTER) {
        CancelableDelayedTask.State state = cdt.cancel(cdtId);
        if( state == CancelableDelayedTask.State.Canceled ) {
          logger.info("Canceled CapacityAdoption");
        }
        cdtId = null;
      }
      return isReadyForChange;
    }

    public void setClusterContext(ClusterContext clusterContext) {
      this.clusterContext = clusterContext;
    }

    private ClusterState clusterState;
    
    private void init(ClusterState newState) {
      switch (newState) {
        case CONNECTED:
          cmAlgorithmBuilder.tryClustered();
          break;
        case DISCONNECTED_MASTER:
          CapacityStorable tmpInstance = new CapacityStorable();
          ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
          updateAllCapStorablesWithoutManagementLock( UpdateAllCapacityStorables.forOwnUsage(capacityStorableQueries,ownBinding) );
          cmAlgorithmBuilder.local();
          break;
        case STARTING:
          cmAlgorithmBuilder.unsupported(CMUnsupported.Cause.Unitialized);
          break;
        case DISCONNECTED_SLAVE:
          cmAlgorithmBuilder.unsupported(CMUnsupported.Cause.DisconnectedSlave);
          break;
        case DISCONNECTED:
        case SINGLE:
        case NO_CLUSTER:
          cmAlgorithmBuilder.local();
          break;
      }
    }

    public void onChange(final ClusterState newState) {
      ClusterState oldState = clusterState;
      if (oldState == newState) {
        return;
      }
      clusterState = newState;
      if (logger.isDebugEnabled()) {
        logger.debug("Got notified of state transition '" + oldState + "' -> '" + newState + "'");
      }

      if (oldState == ClusterState.NO_CLUSTER) {
        createCluster(newState);
      } else if( oldState == null || oldState == ClusterState.STARTING ) {
        init(newState);
      } else {
        changeState(oldState, newState);
      }
      if (logger.isInfoEnabled()) {
        logger.info("CapacityManagement-Algorithm is " + cmAlgorithm.getClass().getSimpleName() + " after stateChange "
            + oldState + "->" + newState);
      }
    }

    /**
     * @param oldState
     * @param newState
     */
    private void changeState(ClusterState oldState, ClusterState newState) {
      switch (newState) {

        case CONNECTED :
          if (oldState.in(ClusterState.DISCONNECTED, ClusterState.DISCONNECTED_MASTER, ClusterState.DISCONNECTED_SLAVE,
            ClusterState.SINGLE, ClusterState.STARTING )) {
            reconnect(oldState);
          } else {
            throw new RuntimeException("Unconsidered change " + oldState + "->" + newState);
          }
          break;

        case DISCONNECTED :
          if( oldState.in( ClusterState.CONNECTED, ClusterState.STARTING ) ) {
            disconnect(newState);
          } else {
            throw new RuntimeException( "Unconsidered change "+oldState+"->"+newState);
          }

        case SINGLE: 
          //anderer Knoten hat den Cluster verlassen
          if (oldState.in(ClusterState.CONNECTED, ClusterState.DISCONNECTED)) {
            disconnect(newState);
          } else {
            throw new RuntimeException("Unconsidered change " + oldState + "->" + newState);
          }
          break;

        case NO_CLUSTER :
          // FIXME SPS prio3: Es handelt sich offenbar um den Vorgang, dass der Cluster verlassen wird.
          //                  * Wenn man vorher SINGLE war, nimmt man die Kapazit�ten mit und verwendet daf�r dann wieder
          //                    das default-Binding
          //                  * In allen anderen F�llen werden die Kapazit�ten dem Cluster zugeschrieben und auf die anderen
          //                    Knoten aufgeteilt
          cmAlgorithmBuilder.unsupported(CMUnsupported.Cause.UnconsideredClusterStateChange);
          break;
        case DISCONNECTED_MASTER :
          if (oldState.in(ClusterState.CONNECTED, ClusterState.DISCONNECTED, ClusterState.STARTING)) {
            disconnect(newState);
          } else {
            throw new RuntimeException("Unconsidered change " + oldState + "->" + newState);
          }
          break;

        case DISCONNECTED_SLAVE :
          if (oldState.in(ClusterState.CONNECTED, ClusterState.DISCONNECTED, ClusterState.STARTING)) {
            disconnect(newState);
          } else {
            throw new RuntimeException("Unconsidered change " + oldState + "->" + newState);
          }
          break;

        default :
          throw new RuntimeException("Unconsidered change " + oldState + "->" + newState);
      }
    }

    /**
     * @param newState
     */
    private void createCluster(ClusterState newState) {
      switch( newState ) {
        case SINGLE: //createCluster
          //Umtragen der bisher lokal konfigurierten Kapazit�ten
          updateAllCapStorablesWithoutManagementLock( 
            UpdateAllCapacityStorables.changeBinding(capacityStorableQueries, 0, ownBinding) );
          cmAlgorithmBuilder.local();
          break;
        case CONNECTED: //joinCluster
          //Erg�nzen der bisherigen Caps f�r das eigene Binding
          updateAllCapStorablesWithoutManagementLock( 
            UpdateAllCapacityStorables.addCapsForBinding(capacityStorableQueries,ownBinding) );
          cmAlgorithmBuilder.tryClustered();
          break;
        default:
          cmAlgorithmBuilder.unsupported(CMUnsupported.Cause.UnconsideredClusterStateChange);
             
      }
    }

    private void reconnect(ClusterState oldState) {
      switch (oldState) {
        case SINGLE :
          //cache ist immer noch g�ltig
          break;
        case DISCONNECTED_MASTER :
          //cache ist immer noch g�ltig
          break;
        case DISCONNECTED_SLAVE :
          // FIXME SPS prio5: cache neu lesen. Das ist erst mal nicht so wichtig, weil bei SLAVE zun�chst sowieso die komplette
          //                  factory runtergefahren wird. Beim restart wird dann sowieso der Cache gelesen.
          // FIXME SPS prio5: Problem, wenn Caps noch im Cache benutzt sind. Auch das kann nur passieren, wenn die Maschine
          //                  nicht komplett heruntergefahren wird.
          break;
        case DISCONNECTED :
          //cache ist immer noch g�ltig
          break;
        case STARTING :
          //cache ist immer noch g�ltig
          break;
        default :
          throw new RuntimeException(oldState + " is no disconnected state");
      }
      cmAlgorithmBuilder.tryClustered();
    }

    private void disconnect(ClusterState newState) {
      switch (newState) {
        case SINGLE :
          //Caps d�rfen alle benutzt werden, daher dem eigenen Binding zuschlagen. Hier ist kein Timeout n�tig,
          //weil der letzte andere Knoten offenbar gesittet den Cluster verlassen hat.
          updateAllCapStorablesWithoutManagementLock( UpdateAllCapacityStorables.forOwnUsage(capacityStorableQueries,ownBinding) );
          cmAlgorithmBuilder.local();
          break;
        case DISCONNECTED_MASTER :
          //Caps d�rfen alle benutzt werden, daher dem eigenen Binding zuschlagen.
          //Das darf erst nach dem Timeout geschehen, der auch abgewartet wird, 
          //bevor Auftr�ge der anderen Knoten gestartet werden.
          startTaskForCapacityAdoption();
          cmAlgorithmBuilder.local();
          break;
        case DISCONNECTED_SLAVE :
          //Caps d�rfen nicht mehr vergeben werden, Management-Operationen sind Unsupported
          cmAlgorithmBuilder.unsupported(CMUnsupported.Cause.DisconnectedSlave);
          break;
        case DISCONNECTED :
          //Caps k�nnen weiter wie bisher verwendet werden, allerdings ist kein Transfer m�glich
          cmAlgorithmBuilder.local();
          break;
        default :
          throw new RuntimeException(newState + " is no disconnected state");
      }
    }

    private synchronized void startTaskForCapacityAdoption() {
      long timeout = XynaProperty.CLUSTERING_TIMEOUT_CAPACITY_MIGRATION.getMillis();
      cdtId = cdt.schedule(timeout, new Runnable() {
        public void run() {
          isReadyForChange = false;
          try {
            if( Thread.currentThread().isInterrupted() ) {
              return;
            }
            if( clusterContext.getClusterState() == ClusterState.DISCONNECTED_MASTER ) {
              updateAllCapStorablesWithoutManagementLock( UpdateAllCapacityStorables.forOwnUsage(capacityStorableQueries,ownBinding) );
            }
          } finally {
            clusterContext.readyForClusterStateChange();
            isReadyForChange = true;
          }
        }
      });
      
    }

  }
  
  private void updateAllCapStorablesWithoutManagementLock(final UpdateAllCapacityStorables updateAllCapacityStorables) {
    List<CapacityStorable> allCapacities = null;
    // hier wird kein managementLock geholt, weil diese Methode nur aufgerufen wird, wenn entweder nach SINGLE oder nach
    // DISCONNECTED_MASTER �bergegangen wird. In diesen F�llen ist man aber eh der einzige, der auf die Daten zugreifen
    // darf.
    cache.getLock().lock();
    try {
      WarehouseRetryExecutableNoException<List<CapacityStorable>> wre = new WarehouseRetryExecutableNoException<List<CapacityStorable>>() {

        public List<CapacityStorable> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          return updateAllCapacityStorables.performUpdate(con);
        }
      };
      
      try {
        allCapacities =
            WarehouseRetryExecutor
                .executeWithRetriesNoException(wre, ODSConnectionType.DEFAULT,
                                               Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                               Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                               new StorableClassList(CapacityStorable.class));
      } catch (XNWH_RetryTransactionException ctcbe) {
        //ClusterProvider ist schon intern im PersistenceLayer informiert worden,
        //dass die Connection zur DB ein Problem hatte. 
        CapacityStorable tmpInstance = new CapacityStorable();
        ClusterProvider clusterProvider = tmpInstance.getClusterInstance(ODSConnectionType.DEFAULT);
        
        if (clusterProvider.getState() == ClusterState.DISCONNECTED_SLAVE) {
          //es liegt ein ernstes Problem mit der Verbindung zur DB vor -> Abbruch hier
          logger.warn("updateAllCapacityStorables " + updateAllCapacityStorables.getClass().getSimpleName()
              + " failed due to database not reachable: " + ctcbe.getMessage(), ctcbe);
        } else {
          logger.error("updateAllCapStorables " + updateAllCapacityStorables.getClass().getSimpleName()
                       + " failed due to database problems: " + ctcbe.getMessage(), ctcbe);
        }
      } catch (PersistenceLayerException ple) {
        //es sollte nur ein simples SelectForUpdate und anschlie�end ein Update durchgef�hrt werden.
        //beides sollte nie Anlass zu einer PersistenceLayerException geben, daher muss ein schwerwiegender 
        //Fehler aufgetreten sein. 
        //Auch die Art des Fehlers deutet nicht daruaf hin, dass es ein tempor�res Problem ist oder die 
        //Wiederholung ist ebenfalls gescheitert.
        //Daher nun Abbruch
        logger.error("updateAllCapStorables " + updateAllCapacityStorables.getClass().getSimpleName()
            + " failed due to database problems: " + ple.getMessage(), ple);
      }
            
      if( allCapacities != null ) {
        //allCapacities konnte korrekt gelesen werden, daher nun cache neu bauen
        //Refresh des Caches durch �bergabe aller CapacityStorables
        //kein Aufrufe von refresh f�r einzelne Bindings, damit auch gel�schte entfernt werden
        cache.refresh(allCapacities, ownBinding);
        //nachdem sich nun der Cache ge�ndert hat, sollte der Scheduler benachrichtigt werden, da neue 
        //freie Caps hinzugekommen sein k�nnten. 
        if (!XynaFactory.getInstance().isStartingUp()) {
          XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
        }
      }
    } finally {
      cache.getLock().unlock();
    }
  }

  public void finallyCloseConnection(ODSConnection con) {
    if (con != null) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
  }


  private void registerStatistics(CapacityInformation capInfo) {

    

  }

  public CapacityCache getCapacityCache() {
    return cache;
  }


  //r�umt sich automatisch auf
  private Map<CapacityChangeListener, Boolean> capacityChangedListeners =
      new WeakHashMap<CapacityChangeListener, Boolean>();


  /**
   * {@link CapacityChangeListener} wird weakly referenced gespeichert. 
   */
  public void registerCapacityChangedListener(CapacityChangeListener capacityChangedListener) {
    synchronized (capacityChangedListeners) {
      capacityChangedListeners.put(capacityChangedListener, Boolean.TRUE);
    }
  }


  private void callCapacityChangedListeners(String capName) {
    Set<CapacityChangeListener> listeners;
    synchronized (capacityChangedListeners) {
      listeners = new HashSet<CapacityChangeListener>(capacityChangedListeners.keySet());
    }
    for (CapacityChangeListener l : listeners) {
      l.capacityChanged(capName);
    }
  }

}
