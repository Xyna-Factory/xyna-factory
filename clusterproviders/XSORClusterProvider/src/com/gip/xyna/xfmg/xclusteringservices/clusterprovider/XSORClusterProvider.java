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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.cluster.ClusterManagement;
import com.gip.xyna.cluster.ClusterManagementImpl;
import com.gip.xyna.cluster.StateChangeHandler;
import com.gip.xyna.cluster.TimeoutException;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagementInterface.StepStatus;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xclusteringservices.ClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterProviderManagement.PersistenceProcessingState;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.rmi.RMIInterface;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.configuration.*;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xsor.XynaScalableObjectRepositoryImpl;
import com.gip.xyna.xsor.interconnect.InterconnectSender;
import com.gip.xyna.xsor.interconnect.InterconnectServer;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.protocol.WaitManagement;



public class XSORClusterProvider implements ClusterProvider {

  protected static final Logger logger = CentralFactoryLogging.getLogger(XSORClusterProvider.class);

  private static final String TYPE_NAME = XSORClusterProvider.class.getSimpleName();
  private static final String EXTENDEDSTATUS_NAME = "XynaCluster"; //ACHTUNG. bei Änderungen auch in networkavailability demon/checkFactoryState.sh ändern!


  private static final String PARAMETER_DESCRIPTION = "1. Interconnect Port Remote, "
      + "\n2. Interconnect Port Local, " + "\n3. Interconnect Correction Queue Length, "
      + "\n4. Interconnect Hostname Remote, " + "\n5. ClusterManagement Hostname Local, "
      + "\n6. ClusterManagement Hostname Remote, " + "\n7. ClusterManagement Port Communication, "
      + "\n8. ClusterManagement Port Registry Local, " + "\n9. ClusterManagement Port Registry Remote, "
      + "\n10. Persistence Max Batch Size, " + "\n11. Persistence Interval (ms), "
      + "\n12. Synchronous Persistence (true/false), " + "\n13. Node Id, " + "\n14. Node Preference (true/false), "
      + "\n15. Delay of Availability Daemon (ms)";
  private static final String XYNAPROPERTY_NETWORK_AVAILABILITY = "xyna.xcs.networkavailability";
  private static final String XYNAPROPERTY_WAITMANAGEMENT_NOT_STRCITLY_TIMEOUT = "xyna.xsor.waitmanagement.notstrictlytimeoutms";
  private static final String XYNAPROPERTY_WAITMANAGEMENT_STRCITLY_TIMEOUT = "xyna.xsor.waitmanagement.strictlytimeoutms";
  
  

  private enum NETWORK_AVAILABILITY_STATE {
    OK, ERROR;
  }

  private static class InvalidParametersException extends Exception {

    private static final long serialVersionUID = 1L;
  }


  /**
   * konfiguration des clusters
   */
  private XSORClusterInstanceStorable xsorClusterInstanceStorable;

  private ODS ods;

  /**
   * "der" statechangehandler
   */
  private ClusterStateChangeHandler clusterStateChangeHandler;

  /*
   * -------------- CLUSTER MANAGEMENT ---------------
   */
  private ClusterManagement clusterManagement;
  private RMIInterface cmRmiIf;

  /*
   * -------------- XYNA SCALABLE OBJECT REPOSITORY ----------------
   */
  private XynaScalableObjectRepositoryImpl xsor;
  private PersistenceStrategyImpl persistenceStrategy;

  InterconnectSender interconnectSender = null;
  InterconnectServer interconnectServer = null;
  private Thread interconnectServerThread;
  private Thread interconnectSenderThread;


  public XSORClusterProvider() {
  }


  private void initOds() throws PersistenceLayerException {
    if (ods == null) {
      ods = ODSImpl.getInstance();
      ods.registerStorable(XSORClusterInstanceStorable.class);
    }
  }


  public void changeClusterState(ClusterState newState) {
    if (newState == ClusterState.DISCONNECTED_MASTER || newState == ClusterState.SHUTDOWN) {
      clusterManagement.changeState(transFormState(newState));
    } else {
      throw new RuntimeException("state may not be set to " + newState);
    }
  }


  public void checkInterconnect() {
    //ntbd, interconnect wird von externem prozess überprüft.
  }


  private long getInternalClusterId() {
    return 17;
  }


  public long createCluster(String[] startParameters) throws XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException {

    try {
      initOds();
      parseAndStoreParameter(startParameters);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidStartParametersForClusterProviderException(TYPE_NAME, e);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    } catch (InvalidParametersException e) {
      throw new XFMG_InvalidStartParametersForClusterProviderException(TYPE_NAME, e);
    }

    initXSOR();
    restoreClusterConnect();

    return xsorClusterInstanceStorable.getId();
  }


  private void parseAndStoreParameter(String[] startParameters) throws InvalidParametersException,
      NumberFormatException, PersistenceLayerException {
    if (startParameters == null || startParameters.length != 15) {
      throw new InvalidParametersException();
    }
    int icRemotePort = Integer.valueOf(startParameters[0]);
    int icServerPort = Integer.valueOf(startParameters[1]);
    int icCorridQueueLength = Integer.valueOf(startParameters[2]);
    String remote = startParameters[3];
    String hostnameLocal = startParameters[4];
    String hostnameRemote = startParameters[5];
    int portCommunication = Integer.valueOf(startParameters[6]);
    int portRegistryLocal = Integer.valueOf(startParameters[7]);
    int portRegistryRemote = Integer.valueOf(startParameters[8]);
    int persistMaxBatchSize = Integer.valueOf(startParameters[9]);
    long persistIntervalMs = Long.valueOf(startParameters[10]);
    boolean persistSynchronous = Boolean.valueOf(startParameters[11]);
    String nodeId = startParameters[12];
    boolean nodePreference = Boolean.valueOf(startParameters[13]);
    int availabilityDelayMs = Integer.valueOf(startParameters[14]);
    //!!!! ACHTUNG: bei ergänzung auch in konstante PARAMETER_DESCRIPTION anpassen und oben die parameter-länge anpassen!!!
    
    xsorClusterInstanceStorable =
        new XSORClusterInstanceStorable(getInternalClusterId(), remote, icServerPort, icRemotePort,
                                                 icCorridQueueLength, hostnameLocal, hostnameRemote, portCommunication,
                                                 portRegistryLocal, portRegistryRemote, persistMaxBatchSize,
                                                 persistIntervalMs, persistSynchronous, nodeId, nodePreference,
                                                 availabilityDelayMs);

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(xsorClusterInstanceStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }

  }


  public void disconnect() {
    clusterManagement.changeState(com.gip.xyna.cluster.ClusterState.SHUTDOWN);
    cmRmiIf.unregister();
    interconnectSender.shutdown();
    interconnectServer.shutdown();
    try {
      persistenceStrategy.persistRemainingData();
    } catch (PersistenceException e) {
      logger.warn("could not persist remaining data", e);
    }
  }


  public List<Integer> getAllBindingsIncludingLocal() {
    // FIXME : Alle Bindings aus statischem Objekt im Cluster auslesen. Kann nicht gecached werden, weil
    //             in der Zwischenzeit Objekte dazugekommen sein könnten. Alternative: Das zentrale Objekt
    //             wird immer redundant gehalten und es wird ein Trigger an das Objekt gehängt, der dann
    //             aktiv hier den Cache updated. Aus Performance-Sicht wäre das sicherlich die bessere Variante.
    return null;
  }

  public ClusterInformation getInformation() {
    ClusterInformation clusterInformation = new ClusterInformation(xsorClusterInstanceStorable.getId(), TYPE_NAME);
    clusterInformation.setExtendedInformation(TYPE_NAME);
    clusterInformation.setClusterState(getState());
    return clusterInformation;
  }


  public int getLocalBinding() {
    // FIXME : Lokales Binding aus einem dedizierten (statischen) Objekt auslesen. Kann alternativ auch
    //             lokal gecached werden
    return 0;
  }


  public String getNodeConnectionParameterInformation() {

    return PARAMETER_DESCRIPTION;
  }


  public String getStartParameterInformation() {
    return PARAMETER_DESCRIPTION;
  }


  private ClusterState transformState(com.gip.xyna.cluster.ClusterState state) {
    switch (state) {
      case CONNECTED :
        return ClusterState.CONNECTED;
      case DISCONNECTED :
        return ClusterState.DISCONNECTED;
      case DISCONNECTED_MASTER :
        return ClusterState.DISCONNECTED_MASTER;
      case SHUTDOWN :
        return ClusterState.SHUTDOWN;
      case STARTUP :
        return ClusterState.STARTING;
      case SYNC_MASTER :
        return ClusterState.SYNC_MASTER;
      case SYNC_PARTNER :
        return ClusterState.SYNC_PARTNER;
      case SYNC_SLAVE :
        return ClusterState.SYNC_SLAVE;
      case INIT :
        return ClusterState.INIT;
      case NEVER_CONNECTED :
        return ClusterState.NEVER_CONNECTED;
      default :
        throw new RuntimeException("state " + state + " not supported");
    }
  }


  private com.gip.xyna.cluster.ClusterState transFormState(ClusterState state) {
    switch (state) {
      case CONNECTED :
        return com.gip.xyna.cluster.ClusterState.CONNECTED;
      case DISCONNECTED :
        return com.gip.xyna.cluster.ClusterState.DISCONNECTED;
      case DISCONNECTED_MASTER :
        return com.gip.xyna.cluster.ClusterState.DISCONNECTED_MASTER;
      case SHUTDOWN :
        return com.gip.xyna.cluster.ClusterState.SHUTDOWN;
      case STARTING :
        return com.gip.xyna.cluster.ClusterState.STARTUP;
      case SYNC_MASTER :
        return com.gip.xyna.cluster.ClusterState.SYNC_MASTER;
      case INIT :
        return com.gip.xyna.cluster.ClusterState.INIT;
      case NEVER_CONNECTED :
        return com.gip.xyna.cluster.ClusterState.NEVER_CONNECTED;
      case SYNC_PARTNER :
        return com.gip.xyna.cluster.ClusterState.SYNC_PARTNER;
      case SYNC_SLAVE :
        return com.gip.xyna.cluster.ClusterState.SYNC_SLAVE;
        /* nicht unterstützt:  
        case SINGLE :
        case DISCONNECTED_SLAVE:
        case NO_CLUSTER :
        */
      default :
        throw new RuntimeException("state " + state + " not supported");
    }
  }


  private void createXSOR() throws XMCP_RMI_BINDING_ERROR, PersistenceLayerException {
    xsor = new XynaScalableObjectRepositoryImpl();

    int maxBatchSizeForPersistence = xsorClusterInstanceStorable.getPersistMaxBatchSize();
    long persistenceIntervalMs = xsorClusterInstanceStorable.getPersistIntervalMs();
    boolean synchronous = xsorClusterInstanceStorable.getPersistSynchronous();
    persistenceStrategy =
        new PersistenceStrategyImpl(ODSConnectionType.HISTORY, maxBatchSizeForPersistence, persistenceIntervalMs,
                                    synchronous);

    
    String hostnameRemote = xsorClusterInstanceStorable.getCMHostNameRemote();
    int portRemote = xsorClusterInstanceStorable.getCMPortRegistryRemote();
    String hostnameLocal = xsorClusterInstanceStorable.getCMHostNameLocal();
    int rmiPortRegistryLocal = xsorClusterInstanceStorable.getCMPortRegistryLocal();
    int rmiPortCommunicationLocal = xsorClusterInstanceStorable.getCMPortCommunicationLocal();

    boolean wasMasterBeforeShutdown = xsorClusterInstanceStorable.getWasMaster();
    int availabilityDelayMs = xsorClusterInstanceStorable.getAvailabilityDelayMs();
    
    createClusterManagement(hostnameRemote, portRemote, hostnameLocal, rmiPortRegistryLocal, rmiPortCommunicationLocal,
                            wasMasterBeforeShutdown, availabilityDelayMs);


    String nodeId = xsorClusterInstanceStorable.getNodeId();
    boolean nodePreference = xsorClusterInstanceStorable.getNodePreference();

    xsor.init(nodeId, nodePreference, persistenceStrategy, clusterManagement);
  }


  public ClusterState getState() {
    return transformState(clusterManagement.getCurrentState());
  }


  public String getTypeName() {
    return TYPE_NAME;
  }


  public boolean isConnected() {
    // TODO: was genau bedeutet connected hier? wozu gibt es die methode, wenn sie äquivalent zu clsuterstate connected ist? evtl methode rauswerfen?
    com.gip.xyna.cluster.ClusterState cs = clusterManagement.getCurrentState();
    return cs == com.gip.xyna.cluster.ClusterState.CONNECTED || cs == com.gip.xyna.cluster.ClusterState.SYNC_MASTER
        || cs == com.gip.xyna.cluster.ClusterState.SYNC_PARTNER || cs == com.gip.xyna.cluster.ClusterState.SYNC_SLAVE;
  }


  public long joinCluster(String[] connectionParameters)
      throws XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    try {
      initOds();
      parseAndStoreParameter(connectionParameters);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(TYPE_NAME, e);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterConnectionException(e);
    } catch (InvalidParametersException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(TYPE_NAME, e);
    }

    try {
      initXSOR();
    } catch (XFMG_ClusterInitializationException e) {
      throw new XFMG_ClusterConnectionException(e);
    }

    restoreClusterConnect();

    return xsorClusterInstanceStorable.getId();
  }


  public void leaveCluster() {
    clusterManagement.changeState(com.gip.xyna.cluster.ClusterState.SHUTDOWN);
    cmRmiIf.unregister();
  }


  public void setClusterStateChangeHandler(final ClusterStateChangeHandler handler) {
    clusterManagement.registerStateChangeHandler(new StateChangeHandler() {

      public void onChange(com.gip.xyna.cluster.ClusterState oldState, com.gip.xyna.cluster.ClusterState newState) {
        handler.onChange(transformState(newState));
      }


      public boolean readyForStateChange(com.gip.xyna.cluster.ClusterState oldState,
                                         com.gip.xyna.cluster.ClusterState newState) {
        return handler.isReadyForChange(transformState(newState));
      }

    });

  }

  private void createClusterManagement(String hostnameRemote, int portRemote, String hostnameLocal,
                                       int rmiPortRegistryLocal, int rmiPortCommunicationLocal,
                                       boolean wasMasterBeforeShutdown, int availabilityDelayMs)
      throws XMCP_RMI_BINDING_ERROR {

    //über futureexecutiontask in xynaclusteringservicesmanagement ist sichergestellt, dass rmimanagement zu dem zeitpunkt bereits initialisiert wurde.
    cmRmiIf =
        new RMIInterface(hostnameRemote, portRemote, hostnameLocal, rmiPortRegistryLocal, rmiPortCommunicationLocal);
    final ClusterProviderManagement remoteRMIIf = cmRmiIf.createRMIAdapter();
    clusterManagement = new ClusterManagementImpl(remoteRMIIf, wasMasterBeforeShutdown, availabilityDelayMs);
    clusterManagement.registerSyncFinishedCondition();
    clusterManagement.registerStateChangeHandler(new StateChangeHandler() {

      public boolean readyForStateChange(com.gip.xyna.cluster.ClusterState oldState,
                                         com.gip.xyna.cluster.ClusterState newState) {
        if (oldState.isSync() && newState == com.gip.xyna.cluster.ClusterState.CONNECTED) {
          //auf remote persistierung warten
          cmRmiIf.createRMIAdapter();
        } else if (newState.isSync()) {
          persistenceCounterBefore = getBackingStoreState().numberOfPersistedRequests;
        }
        return true;
      }


      private volatile Thread waitingForRemoteBackingstoreThread;
      private volatile long persistenceCounterBefore;


      private PersistenceProcessingState getBackingStoreState() {
        try {
          return remoteRMIIf.persistenceFinished();
        } catch (TimeoutException e) {
          //falls anderer knoten nicht erreichbar, wartet der thread auf clusterstatechange.
          if (logger.isDebugEnabled()) {
            logger.debug("timeout calling persistenceFinished.", e);
          }
          PersistenceProcessingState ret = new PersistenceProcessingState();
          ret.numberOfPersistedRequests = 0;
          ret.currentlyWaitingRequests = 80000; //entspricht 2 sek wait
          return ret;
        }
      }


      public void onChange(com.gip.xyna.cluster.ClusterState oldState, com.gip.xyna.cluster.ClusterState newState) {
        if (newState == com.gip.xyna.cluster.ClusterState.STARTUP || newState == com.gip.xyna.cluster.ClusterState.INIT) {
          XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
              .updateStep(StepStatus.STARTUP, EXTENDEDSTATUS_NAME, newState.name());
        } else {
          XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
              .updateStep(StepStatus.POSTSTARTUP, EXTENDEDSTATUS_NAME, newState.name());
        }
        try {
          ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
          try {
            xsorClusterInstanceStorable.setClusterState(newState.name());
            xsorClusterInstanceStorable.setWasMaster(clusterManagement.getWasMaster());
            con.persistObject(xsorClusterInstanceStorable);
            con.commit();
          } finally {
            con.closeConnection();
          }
        } catch (PersistenceLayerException e) {
          logger.warn("could not persist current state", e);
        }
        if (newState.isSync()) {
          if (newState == com.gip.xyna.cluster.ClusterState.SYNC_SLAVE) {
            clusterManagement.notifySyncFinishedCondition();
          } else {
            //warten, dass remote der backingstore (fast) alle synchronisierungsdaten geschrieben hat
            //bei slave gibt es remote keine sync-daten zu schreiben
            waitingForRemoteBackingstoreThread = new Thread(new Runnable() {

              public void run() {
                try {
                  PersistenceProcessingState remoteState = getBackingStoreState();
                  while (clusterManagement.getCurrentState().isSync()

                  //abbruch, wenn diese bedingungen gemeinsam erfüllt sind
                      && !(remoteState.currentlyWaitingRequests < 100 && remoteState.numberOfPersistedRequests
                          - persistenceCounterBefore >= xsor.getNumberOfMessagesSentLastSync() - 100)
                          
                      && !(remoteState.currentlyWaitingRequests < 100 && xsor.isSyncFinished())) {

                    //ist nur dann zuviel, wenn man mit mehr als 40000 hz persistiert
                    long waitTime = Math.max(remoteState.currentlyWaitingRequests/40, 250);
                    if (logger.isDebugEnabled()) {
                      logger.debug("waiting " + waitTime + "ms for remote backingstore (waitingRequests="
                          + remoteState.currentlyWaitingRequests + ", expecting="
                          + +xsor.getNumberOfMessagesSentLastSync() + ")");
                    }
                    if (waitingForRemoteBackingstoreThread != null) {
                      try {
                        Thread.sleep(waitTime);
                      } catch (InterruptedException e) {
                      }
                      remoteState = getBackingStoreState();
                    }
                  }
                } finally {
                  if (clusterManagement.getCurrentState().isSync()) {
                    if (logger.isDebugEnabled()) {
                      logger.debug("finished waiting for remote backingstore (" + xsor.getNumberOfMessagesSentLastSync() + ").");
                    }
                    clusterManagement.notifySyncFinishedCondition();
                  } else {
                    if (logger.isDebugEnabled()) {
                      logger.debug("stopped waiting for remote backingstore because clusterstate changed.");
                    }
                  }
                  waitingForRemoteBackingstoreThread = null;
                }
              }


            }, "WaitForRemoteBackingStoreThread");
            waitingForRemoteBackingstoreThread.setDaemon(true);
            waitingForRemoteBackingstoreThread.start();
          }
        } else {
          Thread t = waitingForRemoteBackingstoreThread;
          if (t != null) {
            t.interrupt();
            waitingForRemoteBackingstoreThread = null;
          }
        }
      }

    });
    cmRmiIf.createRMIImpl(clusterManagement, persistenceStrategy);
  }


  @Override
  public void readyForStateChange() {
  }


  public InterconnectSender getInterconnectSender() {
    return interconnectSender;
  }


  public InterconnectServer getInterconnectServer() {
    return interconnectServer;
  }


  @Override
  public void restoreClusterConnect() {
    //FIXME threads beim shutdown ordentlich beenden. threadnamen setzen.
    //FIXME muss queue-inhalt beim runterfahren irgendwie behandelt werden?
    if (interconnectServer == null) {
      interconnectServer =
          new InterconnectServer(xsorClusterInstanceStorable.getServerPort(),
                                 xsorClusterInstanceStorable.getIcCorrQueueLength());
      interconnectServerThread =
          new Thread(interconnectServer, interconnectServer.getClass().getSimpleName() + "-Thread");
      interconnectServerThread.setDaemon(true);
      interconnectServerThread.start();
    }

    if (interconnectSender == null) {
      interconnectSender =
          new InterconnectSender(xsorClusterInstanceStorable.getHostname(),
                                 xsorClusterInstanceStorable.getRemotePort(),
                                 xsorClusterInstanceStorable.getIcCorrQueueLength());
      interconnectSenderThread =
          new Thread(interconnectSender, interconnectSender.getClass().getSimpleName() + "-Thread");
      interconnectSenderThread.setDaemon(true);
      interconnectSenderThread.start();
    }
    xsor.setInterconnect(interconnectSender, interconnectServer);

    XynaFactory.getInstance().getFutureExecution()
        .execAsync(new FutureExecutionTask(XynaFactory.getInstance().getFutureExecution().nextId()) {

          @Override
          public void execute() {
            //führt dazu, dass automatisch versucht wird nach startup zu gehen, wenn das 
            //deployment fertig ist. danach je nach availability des anderen knoten nach connected/disconnected/sync, etc
            clusterManagement.changeState(com.gip.xyna.cluster.ClusterState.STARTUP);
          }


          @Override
          public int[] after() {
            return new int[] {WorkflowDatabase.FUTURE_EXECUTION_ID, XynaActivationTrigger.FUTUREEXECUTION_ADDTRIGGER_ID};
          }


          @Override
          public boolean waitForOtherTasksToRegister() {
            return false;
          }

        });
  }


  @Override
  public void restoreClusterPrepare(long internalClusterId) throws XFMG_ClusterInitializationException {
    try {
      initOds();
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaExtendedStatusManagement()
        .registerStep(StepStatus.STARTUP, EXTENDEDSTATUS_NAME, ClusterState.INIT.name());

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      xsorClusterInstanceStorable = new XSORClusterInstanceStorable(internalClusterId);
      con.queryOneRow(xsorClusterInstanceStorable);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("connection could not be closed", e);
      }
    }

    initXSOR();
  }


  private void initXSOR() throws XFMG_ClusterInitializationException {
    try {
      createXSOR();
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(TYPE_NAME, e);
    }

    //nach createXSOR aufrufen, damit availability initial in xsor gesetzt wird
    addPropertyChangeListener();
  }


  private void addPropertyChangeListener() {
    final Configuration conf =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    try {
      //property beim serverstart hier immer auf nicht-erreichbar setzen, weil die property
      //über die cli nicht gesetzt werden kann, während der server hochfährt. der wert von
      //vor dem serverstart könnte OK sein, was hier dann nicht passend ist.
      conf.setProperty(XYNAPROPERTY_NETWORK_AVAILABILITY, NETWORK_AVAILABILITY_STATE.ERROR.name());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XFMG_IllegalPropertyValueException e) {
      throw new RuntimeException(e);
    }

    IPropertyChangeListener ipc = new IPropertyChangeListener() {

      private final XynaPropertyString xpsNetworkAvailability = new XynaPropertyString(XYNAPROPERTY_NETWORK_AVAILABILITY,
                                                                                       NETWORK_AVAILABILITY_STATE.ERROR.name());

      private final XynaPropertyLong xpsWaitManagementNotStrictlyTimeout = new XynaPropertyLong(XYNAPROPERTY_WAITMANAGEMENT_NOT_STRCITLY_TIMEOUT, 100L);

      private final XynaPropertyLong xpsWaitManagementStrictlyTimeout = new XynaPropertyLong(XYNAPROPERTY_WAITMANAGEMENT_STRCITLY_TIMEOUT, 365L*24L*60L*60L*1000L);

      private ArrayList<String> props =
          new ArrayList<String>(Arrays.asList(new String[] {XYNAPROPERTY_NETWORK_AVAILABILITY, 
                          XYNAPROPERTY_WAITMANAGEMENT_NOT_STRCITLY_TIMEOUT, 
                          XYNAPROPERTY_WAITMANAGEMENT_STRCITLY_TIMEOUT}));


      @Override
      public void propertyChanged() {
        if (xpsNetworkAvailability.get().equals(NETWORK_AVAILABILITY_STATE.ERROR.name())) {
          clusterManagement.setOtherNodeAvailable(false);
        } else {
          clusterManagement.setOtherNodeAvailable(true);
        }
        WaitManagement.setTimeouts(xpsWaitManagementNotStrictlyTimeout.get(),xpsWaitManagementStrictlyTimeout.get());
        
      }


      @Override
      public ArrayList<String> getWatchedProperties() {
        return props;
      }
    };
    conf.addPropertyChangeListener(ipc);

    XynaFactory
    .getInstance()
    .getFactoryManagementPortal()
    .getXynaFactoryControl()
    .getDependencyRegister()
    .addDependency(DependencySourceType.XYNAPROPERTY, XYNAPROPERTY_WAITMANAGEMENT_NOT_STRCITLY_TIMEOUT,
                   DependencySourceType.XYNAFACTORY, XSORClusterProvider.class.getName());
    
    XynaFactory
    .getInstance()
    .getFactoryManagementPortal()
    .getXynaFactoryControl()
    .getDependencyRegister()
    .addDependency(DependencySourceType.XYNAPROPERTY, XYNAPROPERTY_WAITMANAGEMENT_STRCITLY_TIMEOUT,
                   DependencySourceType.XYNAFACTORY, XSORClusterProvider.class.getName());
    
    
    
    ipc.propertyChanged();
    
  }


  public XynaScalableObjectRepositoryImpl getXynaScalableObjectRepository() {
    return xsor;
  }


  protected void changeState(ClusterState clusterState, String method) {
    if (clusterStateChangeHandler != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(method + " finished; calling onChange for clusterState=" + clusterState);
      }
      while (!clusterStateChangeHandler.isReadyForChange(clusterState)) {
        try {
          Thread.sleep(30);
        } catch (InterruptedException e) {
          throw new RuntimeException("got interrupted unexpectedly", e);
        }
      }
      clusterStateChangeHandler.onChange(clusterState);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug(method + " finished with clusterState = " + clusterState);
      }
    }
  }


  @Override
  public boolean fastCheckIsMediumReachable() {
    throw new RuntimeException("unsupported");
  }


}
