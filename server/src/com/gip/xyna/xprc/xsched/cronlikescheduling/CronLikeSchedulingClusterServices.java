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

import java.util.List;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xprcods.orderarchive.ClusteredOrderArchive;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.CronLikeOrderReader;



public abstract class CronLikeSchedulingClusterServices extends FunctionGroup
                implements
                  Clustered,
                  ClusteredCronLikeSchedulerInterface,
                  RMIImplFactory<ClusteredCronLikeSchedulerRemoteInterfaceImpl> {
  
  
  public static final String CLUSTERABLE_COMPONENT = "ClusteredCronLikeScheduler";
  
  public int FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  
  protected volatile CronLikeTimer cronLikeTimer;
  
  protected ClusterContext rmiClusterContext;
  protected ClusterContext storableClusterContext;
  protected ClusteredCLSchedulerChangeHandler cloClusterStateChangeHandler;
  protected RMIClusterStateChangeHandler rmiClusterStateChangeHandler;
  
  protected long clusteredInterfaceId;
  protected int currentOwnBinding =XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;
  
  protected transient PreparedQueryCache cursorQueryCache = new PreparedQueryCache();
  
  
  
  public CronLikeSchedulingClusterServices() throws XynaException {
    super();
  }
  
  
  protected RMIClusterStateChangeHandler newRMIClusterStateChangeHandler() {
    return new RMIClusterStateChangeHandler();
  }
  
  protected static class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {
    
    
    private ClusterState clusterState;
    
    
    public RMIClusterStateChangeHandler() {
    }

    public boolean isReadyForChange(ClusterState newState) {
      return true; // immer bereit
    }

    public void onChange(ClusterState newState) {
      // reaktion auf cluster state änderungen, die den direkten RMI interconnect betreffen und
      // z.B. das removeCronLikeOrder beeinträchtigen
      if (clusterState == newState)
        return;
      clusterState = newState;
    }
  };

  
  protected Object getQueueLock() {
    return cronLikeTimer.getBlockingObject();
  }
  
  public String getName() {
    return CLUSTERABLE_COMPONENT;
  }
  
  protected class ClusteredCLSchedulerChangeHandler implements ClusterStateChangeHandler {

    private volatile boolean isReadyForChange = true;
    private ClusterContext clusterContext;
    private ClusterState clusterState;
    
    public ClusteredCLSchedulerChangeHandler(ClusterContext clusterContext) {
      this.clusterContext = clusterContext;
    }
    
    public boolean isReadyForChange(ClusterState newState) {
      return isReadyForChange;
    }
    
    public void onChange(ClusterState newState) {
      // Reaktion auf state changes, die das ClusterStorable betreffen
      if (clusterState == newState) {
        return;
      }
      
      if(logger.isDebugEnabled()) {
        logger.debug("Got notified of state transition '" + clusterState + "' -> '" + newState + "'.");
      }
      
      if (newState == ClusterState.STARTING) {
        // ignoriere alle STARTINGs-States
        clusterState = newState;
        return;
      }
      ClusterState lastState = clusterState;
      
      if( newState.isDisconnected() ) {
        clusterState = disconnect(lastState, newState);
      } else {
        clusterState = connect(lastState, newState);
      }
      
      
    }
    
    public ClusterState disconnect(ClusterState lastState, final ClusterState newState) {
      switch (newState) {
      case DISCONNECTED_SLAVE:
        // TODO FIXME was ist hier zu tun? der Scheduler wird ja erst beim
        // Shutdown gestoppt.
        break;
      case DISCONNECTED_MASTER:
        clusterContext.getFutureExecution()
            .addTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID, "CronLikeScheduler-ClusterState." + newState)
            .after(ClusteredOrderArchive.FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID).
            // FIXME diese After-abhängigkeit (und die entsprechende in
            // ManualInteractionManagement) nur dann setzen
            // wenn beide auf die gleiche cluster-instanz konfiguriert sind.
            execAsync(new Runnable() {
              public void run() {
                if (XynaFactory.getInstance().isShuttingDown()) {
                  // Wenn Server gerade runterfährt, sollte dieser
                  // Statusübergang ignoriert werden, da die Migration auch
                  // nicht statt finden wird!
                  return;
                }
                CronLikeTimer cltCopy = cronLikeTimer;
                if (cltCopy != null) {
                  cltCopy.changeToBinding(null);
                  cltCopy.recreateQueue();
                }
              }
            });
        break;
      default:
        logger.warn("Unimplemented ClusterState change from " + lastState + " to " + newState);
      }
      return newState;
    }
    
    public ClusterState connect(ClusterState lastState, ClusterState newState) {
      CronLikeOrder tmpInstance = new CronLikeOrder();
      currentOwnBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
      switch (newState) {
      case SINGLE:
        if (lastState == ClusterState.NO_CLUSTER) {
          // alle mit binding 0 auf eigenes binding stellen
          isReadyForChange = false;
          clusterContext.getFutureExecution()
              .addTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID, "CronLikeScheduler-ClusterState.SINGLE")
              .execAsync(new Runnable() {
                public void run() {
                  try {
                    changeToOwnBinding();
                  } finally {
                    isReadyForChange = true;
                  }
                }
              });
        }
        break;
      case CONNECTED:
        if (lastState == ClusterState.NO_CLUSTER) {
          // alle mit binding 0 loggen ... solche sollte es nicht mehr geben!
          isReadyForChange = false;
          clusterContext.getFutureExecution()
              .addTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID, "CronLikeScheduler-ClusterState.CONNECTED")
              .execAsync(new Runnable() {
                public void run() {
                  try {
                    logUnclusteredBindings();
                  } finally {
                    isReadyForChange = true;
                  }
                }
              });
        }
        if (lastState == ClusterState.DISCONNECTED_MASTER) {
          // resing from jobs with different binding by removing those jobs from
          // the memory queue
          isReadyForChange = false;
          clusterContext.getFutureExecution()
              .addTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID, "CronLikeScheduler-ClusterState.CONNECTED")
              .execAsync(new Runnable() {
                public void run() {
                  try {
                    CronLikeTimer cltCopy = cronLikeTimer;
                    if (cltCopy != null) {
                      cltCopy.changeToBinding(currentOwnBinding);
                    }
                  } finally {
                    isReadyForChange = true;
                  }
                }
              });
        }
        break;
      default:
        logger.warn("Unimplemented ClusterState change from "+lastState+" to "+newState);
      }
      return newState;
    }
    
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
                  XFMG_ClusterComponentConfigurationException {
    rmiClusterContext.enableClustering(clusterInstanceId);

    try {
      clusteredInterfaceId = ((RMIClusterProvider) rmiClusterContext.getClusterInstance())
                      .addRMIInterfaceWithClassReloading(CLUSTERABLE_COMPONENT, this);
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterComponentConfigurationException(getName(), clusteredInterfaceId, e);
    }
    
  }


  public void disableClustering() {
    rmiClusterContext.disableClustering();
    clusteredInterfaceId = 0;
  }
  
  public long getClusterInstanceId() {
    return rmiClusterContext.getClusterInstanceId();
  }


  public boolean isClustered() {
    return rmiClusterContext.isClustered();
  }
  
  
  public void init(InitializableRemoteInterface rmiImpl) {
    rmiImpl.init(this);
  }


  public String getFQClassName() {
    return ClusteredCronLikeSchedulerRemoteInterfaceImpl.class.getName();
  }


  public void shutdown(InitializableRemoteInterface rmiImpl) {
  }
  
  
  private void logUnclusteredBindings() {
    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {

        FactoryWarehouseCursor<CronLikeOrder> cursor = new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetCronLikeOrdersForBinding,
                                                                  new Parameter(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER),
                                                                  new CronLikeOrderReader(), 250, cursorQueryCache);

        List<CronLikeOrder> clordersdefault = cursor.getRemainingCacheOrNextIfEmpty();
        while (!clordersdefault.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          sb.append("The following cronlikeorders have binding = ")
                .append(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER)
                .append(" and will be ignored: ");
          
          for (CronLikeOrder clo : clordersdefault) {
            sb.append(clo.getLabel()).append(" ");
          }
          logger.warn(sb.toString());
          clordersdefault = cursor.getRemainingCacheOrNextIfEmpty();
        }
      }
    };
    try {
      WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
    } catch (PersistenceLayerException e) {
      logger.warn("Could not retrieve cronLikeOrders to log cronlikeorders with binding = " + XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER,
                  e);
    }
  }

  private void changeToOwnBinding() {
    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        con.ensurePersistenceLayerConnectivity(CronLikeOrder.class);
        synchronized (getQueueLock()) { // ensures that the CLOs are modified reinserted into the queue and persisted while they
                               // are not running
          
          FactoryWarehouseCursor<CronLikeOrder> cursor = new FactoryWarehouseCursor<CronLikeOrder>(con, CronLikeOrderHelpers.sqlGetCronLikeOrdersForBinding,
                                                                    new Parameter(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER),
                                                                    new CronLikeOrderReader(), 250, cursorQueryCache);
          
          List<CronLikeOrder> clordersdefault = cursor.getRemainingCacheOrNextIfEmpty();
          while(!clordersdefault.isEmpty()) {
            for (CronLikeOrder clo : clordersdefault) {
              clo.setBinding(currentOwnBinding);
            }
            CronLikeOrderHelpers.store(clordersdefault, con);
            con.commit();
            clordersdefault = cursor.getRemainingCacheOrNextIfEmpty();
          }
          cronLikeTimer.recreateQueue();
        }
      }
    };
    try {
      WarehouseRetryExecutor.buildCriticalExecutor().storable(CronLikeOrder.class).execute(wre);
    } catch (PersistenceLayerException e) {
      logger.error("Could not retrieve cronLikeOrders to set own binding = " + currentOwnBinding + 
                   " to cronlikeorders with binding = " + XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER,
                   e);
    }

  }
  
}
