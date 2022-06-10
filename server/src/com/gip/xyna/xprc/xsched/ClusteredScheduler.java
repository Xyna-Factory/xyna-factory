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
package com.gip.xyna.xprc.xsched;



import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.CancelableDelayedTask;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement.ResumeOrderRemotelyRMIRunnable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.ClusteredSchedulerRemoteInterface;


public class ClusteredScheduler extends XynaScheduler implements Clustered, ClusteredSchedulerRemoteInterface {
  
  //FIXME eigtl sollte die abhängigkeit zu XynaScheduler. siehe xynafactory-FIXME
  static {
    addDependencies(ClusteredScheduler.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryControl.class,
                                                                           DependencyRegister.class)})));
    addDependencies(ClusteredScheduler.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryManagementODS.class,
                                                                           Configuration.class)})));
  }

  public static final String CLUSTERABLE_COMPONENT = "Scheduler";
  private static Logger logger = CentralFactoryLogging.getLogger(ClusteredScheduler.class);
  private static final long REMOVED_RMI = -1;
  
  private long clusterInstanceId;
  private volatile long clusteredSchedulerInterfaceId;
  private RMIClusterProvider clusterInstance;
  private boolean clustered = false;
  private ClusterState clusterState = ClusterState.NO_CLUSTER;
  private LazyAlgorithmExecutor<RemoteSchedulerNotificationAlgorithm> remoteSchedulerNotificationExecutor;
  private RMIClusterStateChangeHandler rmiClusterStateChangeHandler;

  public ClusteredScheduler() throws XynaException {
    super();
  }

  @Override
  public void init() throws XynaException {
    super.init();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask( ClusteredScheduler.class, "ClusteredScheduler.initClusterable" ).
      before(XynaClusteringServicesManagement.class).
      execAsync(new Runnable() { public void run() { initClusterable(); } });
 
    remoteSchedulerNotificationExecutor =
        new LazyAlgorithmExecutor<RemoteSchedulerNotificationAlgorithm>("remoteSchedulerNotificationExecutor");
    remoteSchedulerNotificationExecutor.startNewThread(new RemoteSchedulerNotificationAlgorithm());
  }

  private void initClusterable() {
    //TODO was ist, wenn man mehrere schedulers hat, und die dann mehrfach registriert werden?
    // => getName() muss unique gemacht werden
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement().registerClusterableComponent(ClusteredScheduler.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException(e); //passiert nicht, wegen futureexecution before-beziehung.
    }
  }
  

  @Override
  public void shutdown() throws XynaException {
    try {
      super.shutdown();
    } finally {
      if (remoteSchedulerNotificationExecutor != null) {
        logger.debug("stopping " + RemoteSchedulerNotificationAlgorithm.class.getSimpleName() + " thread.");
        remoteSchedulerNotificationExecutor.stopThread();
      }
    }
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }


  public boolean isClustered() {
    return clustered;
  }


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    if (clustered) {
      // FIXME SPS prio5: von einem cluster auf ein anderes umkonfigurieren?
      throw new RuntimeException("already clustered");
    }
    this.clusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);
    if (clusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }
    clusteredSchedulerInterfaceId = clusterInstance.addRMIInterface("RemoteScheduler", this);
    rmiClusterStateChangeHandler = new RMIClusterStateChangeHandler();
    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);
    clustered = true;
    clusterState = clusterInstance.getState();
  }

  public void disableClustering() {
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterMgmt.removeClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);
    clusteredSchedulerInterfaceId = REMOVED_RMI;
    clusterInstance.removeRMIInterface(clusteredSchedulerInterfaceId, 0);
    clustered = false;
    clusterInstance = null;
    clusterInstanceId = 0;
    clusterState = ClusterState.NO_CLUSTER;
  }

  private class RMIClusterStateChangeHandler implements ClusterStateChangeHandler {
  
    private CancelableDelayedTask cdt = new CancelableDelayedTask("ClusteredScheduler-NotifyStorableClusterProvider");
    private Integer cdtId;
    public boolean isReadyForChange(ClusterState newState) {
      if( cdtId != null && newState == ClusterState.CONNECTED) {
        CancelableDelayedTask.State state = cdt.cancel(cdtId);
        if( state == CancelableDelayedTask.State.Canceled ) {
          logger.info("Canceled NotifyStorableClusterProvider");
        }
        cdtId = null;
      }
      return true; //immer bereit
    }

    public void onChange(ClusterState newState) {
      if (logger.isDebugEnabled()) {
        logger.debug("Got notified of state transition '" + clusterState + "' -> '" + newState + "'");
      }
      ClusterState lastState = clusterState;
      if( lastState == newState ) {
        return; //nichts zu tun;
      }
      clusterState = newState;
      if (newState.isDisconnected()) {
        changeToDisconnected(lastState, newState);
      } else if( newState.in(ClusterState.CONNECTED)) {
        changeToConnected(lastState, newState);
      }
      notifySchedulerLocally();
    }
    
    private void changeToDisconnected(ClusterState lastState, ClusterState newState ) {
        //abhängigkeit zu capacities und vetos tabelle: diese sollen jetzt
        //nicht mehr geclustered vergeben werden, d.h. ein knoten soll in godmode übergehen
        NotifyStorableClusterProvider nscp = new NotifyStorableClusterProvider(newState);
        if( lastState == ClusterState.STARTING ) {
          cdtId = cdt.schedule(10000, nscp ); //Timeout-Konstante ist unwichtig, da im Falle STARTING 
          //der Status CONNECTED rasch erreicht wird oder 
          //der StorableClusterProvider bereits auf DISCONNECTED_MASTER ist und bleibt
        } else {
          nscp.run(); //sofortigen Übergang veranlassen
        }

        if (newState.in(ClusterState.DISCONNECTED_SLAVE)) {
          //in diesem Zustand sollen keine Aufträge mehr laufen, damit jedoch notwendige Aufträge
          //vor dem Shutdown ausgeführt werden, wird das Schdeuling nicht komplett unterbunden
          pauseScheduling(false);
        }
    }

    private void changeToConnected(ClusterState lastState, ClusterState newState) {
      if( lastState.in(ClusterState.DISCONNECTED_SLAVE)) {
        //Rückkehr zum normalen Scheduling
        resumeScheduling();
      }
    }

    private class NotifyStorableClusterProvider implements Runnable {
      private final ClusterState newState;
      NotifyStorableClusterProvider(ClusterState newState) {
        this.newState = newState;
      }

      public void run() {
        if (logger.isDebugEnabled()) {
          logger.debug( "NotifyStorableClusterProvider.run() called");
        }
        //abhängigkeit zu capacities und vetos tabelle: diese sollen jetzt
        //nicht mehr geclustered vergeben werden, d.h. ein knoten soll in godmode übergehen
        HashSet<ClusterProvider> clusterProviders = new HashSet<ClusterProvider>();
        clusterProviders.add( getClusterProvider( new VetoInformationStorable() ) );
        clusterProviders.add( getClusterProvider( new CapacityStorable() ) );
        for( ClusterProvider cp : clusterProviders ) {
          if( cp != null ) {
            if( clusterProviderHasToBeNotified(cp) ) {
              if (logger.isDebugEnabled()) {
                logger.debug("Notifying cluster provider <" + cp + "> of state transition to '" + newState + "'");
              }
              cp.changeClusterState(newState);
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("Cluster provider <" + cp + "> is already disconnected '" + cp.getState() + "'");
              }
            }
          }
        }
      }

      private boolean clusterProviderHasToBeNotified(ClusterProvider cp) {
        ClusterState cpClusterState = cp.getState();
        boolean notifyCpToChange = false;
        if( cpClusterState == newState) {
          //Status stimmt schon überein
          notifyCpToChange = false;
        } else if(cpClusterState.isDisconnected()) {
          if( cpClusterState == ClusterState.STARTING ) {
            //STARTING zählt als disconnected, muss hier aber anders behandelt werden:
            //Wenn RMIClusterProvider sicher erkennt, dass keine Verbindung zustandekommt, muss der
            //ClusterProvider der Storables zum Disconnect gezwungen werden, selbst wenn er noch 
            //nicht mit seiner Initialisierung fertig ist.
            notifyCpToChange = true;
          } else {
            //ClusterProvider der Storables ist bereits Disconnected, daher kein weiterer StateChange
            notifyCpToChange = false;
          }
        } else {
          //Im Connected-Fall muss ClusterProvider der Storables zum Disconnect gezwungen werden.
          notifyCpToChange = true;
        }
        return notifyCpToChange;
      }

      private ClusterProvider getClusterProvider(ClusteredStorable<?> clusteredStorable) {
        if( ! clusteredStorable.isClustered(ODSConnectionType.DEFAULT) ) {
          return null;
        }
        return clusteredStorable.getClusterInstance(ODSConnectionType.DEFAULT);
      }
    }
  }


  private static RMIRunnableNoResultNoException<ClusteredSchedulerRemoteInterface> notifySchedulerRunnable =
      new RMIRunnableNoResultNoException<ClusteredSchedulerRemoteInterface>() {

        public void execute(ClusteredSchedulerRemoteInterface clusteredInterface) throws RemoteException {
          clusteredInterface.notifySchedulerRemotely();
        }

      };


  private class RemoteSchedulerNotificationAlgorithm implements Algorithm {

    public void exec() {
      try {
        if (clusterState == ClusterState.CONNECTED) {
          RMIClusterProviderTools.executeNoException(clusterInstance, clusteredSchedulerInterfaceId,
                                                     notifySchedulerRunnable);
        }

      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.warn("got unexpected exception.", t);
      }
    }

  }


  public void notifyRemoteScheduler() {
    //TODO den lazyalgoexecutor noch verbessern, damit erst der nächste remoterequest gemacht wird, wenn der
    //remote scheduler tatsächlich nicht mehr läuft. => "lazyremotealgorithmexecutor"
    remoteSchedulerNotificationExecutor.requestExecution();
  }


  public void notifySchedulerLocally() {
    if (!XynaFactory.getInstance().isStartingUp()) {
      super.notifyScheduler();
    }
  }


  public void notifySchedulerRemotely() throws RemoteException {
    notifySchedulerLocally();
  }


  public String getName() {
    return CLUSTERABLE_COMPONENT;
  }

  
  
  /**
   * FIXME in geclustertes SuspendResumeManagement mit Enum NotResponsible,Resumed,Retried,Failed
   * Grund: besseres Logging beim Aufrufer
   * 
   * @see com.gip.xyna.xprc.xsched.scheduling.ClusteredSchedulerRemoteInterface#resumeOrderRemotely(int, ResumeTarget)
   */
  public Boolean resumeOrderRemotely(int binding, ResumeTarget target) throws RemoteException {
    Logger logger = CentralFactoryLogging.getLogger(SuspendResumeManagement.class);
    
    if (logger.isDebugEnabled()) {
      logger.debug("resumeOrderRemotely for "+target+" and binding " + binding);
    }
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    
    if(binding == ownBinding) {
      SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
      Pair<ResumeResult, String> result = null;
      try {
        result = srm.resumeOrder(target);
        if( result.getFirst() == ResumeResult.Resumed ) {
          return true; //Resumed
        } else {
          return retryAsynchronously(srm, target, result, null);
        } 
      } catch (PersistenceLayerException e) {
        return retryAsynchronously(srm, target, null, e);
      }
    } else {
      //nicht zuständig
      return false; //NotResponsible
    }
  }
  
  private boolean retryAsynchronously(SuspendResumeManagement srm, ResumeTarget target, 
                                      Pair<ResumeResult, String> result, PersistenceLayerException ple) {
    Exception asyncFailedException = null;
    Long resumeOrderId = null;
    try {
      resumeOrderId = srm.resumeOrderAsynchronouslyDelayed(target, 0, null, false /* ist bereits remote aufruf */);
      return true; //Retried
    } catch( Exception e ) {
      asyncFailedException = e;
      return false; //Failed
    } finally {
      //logging
      StringBuilder sb = new StringBuilder();
      sb.append("Could not resume ").append(target);
      if( result != null ) {
        sb.append(": ").append(result);
      }
      if( resumeOrderId != null ) {
        sb.append(", trying to resume asynchronously with ResumeOrder ").append(resumeOrderId);
      } else {
        sb.append(", tried to resume asynchronously but failed");
      }
      if( ple == null ) {
        if( asyncFailedException == null ) {
          if( result != null && result.getFirst() == ResumeResult.Unresumeable && SuspendResumeManagement.UNRESUMABLE_LOCKED.equals(result.getSecond()) ) {
            //Dies ist kein ungewöhnlicher Zustand: z.B. Resume kam recht schnell, bevor Suspend fertig ist.
            logger.debug(sb.toString());
          } else {
            logger.warn(sb.toString());
          }
        } else {
          logger.warn(sb.toString(), asyncFailedException);
        }
      } else {
        if( asyncFailedException == null ) {
          logger.warn(sb.toString(), ple);
        } else {
          logger.warn(sb.toString(), ple);
          logger.warn("resume asynchronously failed", asyncFailedException);
        }
      }
    }
  }

  public boolean resumeOrderRemotely(ResumeOrderRemotelyRMIRunnable resumeOrderRemotely) {
    if (clusterState == ClusterState.CONNECTED) {
      try {
        List<Boolean> results = RMIClusterProviderTools
            .executeAndCumulateNoException(clusterInstance, clusteredSchedulerInterfaceId, resumeOrderRemotely, null, Boolean.FALSE);
        for(Boolean result : results) {
          if(result) {
            return true; //ein anderer Knoten hat das Resume ausführen können
          }
        }
      } catch (InvalidIDException e) {
        if (clusteredSchedulerInterfaceId != REMOVED_RMI) {
          throw new RuntimeException(e); //es wurde noch kein disableClustering aufgerufen
        }
      }
    }
    return false;
  }


  public void setCapacityReservation(CapacityReservation capacityReservation) {
    getSchedulerCustomisation().setCapacityReservation(capacityReservation);
  }
  
  @Override
  public ChangeSchedulingParameterStatus changeSchedulingParameter(Long orderId, SchedulingData schedulingData,
                                                                   boolean replace) {
    ChangeSchedulingParameterStatus status = null;
    //erst lokal versuchen
    status = super.changeSchedulingParameter(orderId, schedulingData, replace);
    if( status == ChangeSchedulingParameterStatus.NotFound && clusterState == ClusterState.CONNECTED) {
      //liegt nicht lokal vor, evtl. auf dem anderen Knoten
      ChangeSchedulingParameterRemotelyRMIRunnable cspr = new ChangeSchedulingParameterRemotelyRMIRunnable(orderId,schedulingData,replace);
      try {
        List<ChangeSchedulingParameterStatus> results = RMIClusterProviderTools
            .executeAndCumulateNoException(clusterInstance, clusteredSchedulerInterfaceId, cspr, null, ChangeSchedulingParameterStatus.NotFound);
        for(ChangeSchedulingParameterStatus remote_status : results) {
          switch( remote_status ) {
            case NotFound:
              break;
            case Success:
              status = remote_status;
              break;
            case Unschedulable:
              status = remote_status;
              break;
          }
        }
      } catch (InvalidIDException e) {
        if (clusteredSchedulerInterfaceId != REMOVED_RMI) {
          throw new RuntimeException(e); //es wurde noch kein disableClustering aufgerufen
        }
      }
    }
    
    return status;
  }
  
  public ChangeSchedulingParameterStatus changeSchedulingParameterRemotely(Long orderId, SchedulingData schedulingData,
                                                                           boolean replace) {
    return super.changeSchedulingParameter(orderId, schedulingData, replace);
  }

  
  public static class ChangeSchedulingParameterRemotelyRMIRunnable implements RMIRunnableNoException<ChangeSchedulingParameterStatus, ClusteredSchedulerRemoteInterface> {
    Long orderId;
    SchedulingData schedulingData;
    boolean replace;
    
    public ChangeSchedulingParameterRemotelyRMIRunnable(Long orderId, SchedulingData schedulingData, boolean replace) {
      this.orderId = orderId;
      this.schedulingData = schedulingData;
      this.replace = replace;
    }

    public ChangeSchedulingParameterStatus execute(ClusteredSchedulerRemoteInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.changeSchedulingParameterRemotely(orderId, schedulingData, replace);
    }
    
  }

  
}
