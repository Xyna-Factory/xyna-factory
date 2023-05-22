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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider.rmi;



import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.cluster.ClusterManagement;
import com.gip.xyna.cluster.ClusterState;
import com.gip.xyna.cluster.SyncResponse;
import com.gip.xyna.cluster.TimeoutException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterProviderManagement;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterProviderManagement.PersistenceProcessingState;
import com.gip.xyna.xfmg.xclusteringservices.clusterprovider.PersistenceStrategyImpl;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplProxy;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xsor.persistence.PersistenceException;



public class RMIInterface {

  private static final Logger logger = CentralFactoryLogging.getLogger(RMIInterface.class);
  private static final String BINDINGNAME = "clusterManagementRemote";
  private RMIImplProxy<ClusterProviderMangementRMI> rmiImplProxyClusterMgmt;

  private String hostnameRemote;
  private int portRemote;
  private String hostnameLocal;
  private int rmiPortRegistryLocal;
  private int rmiPortCommunicationLocal;
  private RMIManagement rmiManagement;


  public RMIInterface(String hostnameRemote, int portRemote, String hostnameLocal, int rmiPortRegistryLocal,
                      int rmiPortCommunicationLocal) {
    rmiManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
    this.hostnameLocal = hostnameLocal;
    this.hostnameRemote = hostnameRemote;
    this.rmiPortCommunicationLocal = rmiPortCommunicationLocal;
    this.rmiPortRegistryLocal = rmiPortRegistryLocal;
    this.portRemote = portRemote;
  }


  private static class RmiRunnableChangeState
      implements
        RMIRunnable<String, ClusterProviderMangementRMI, XynaException> {

    private final ClusterState newState;


    public RmiRunnableChangeState(ClusterState newState) {
      this.newState = newState;
    }


    @Override
    public String execute(ClusterProviderMangementRMI cm) throws XynaException, RemoteException {
      cm.changeState(newState);
      return null;
    }

  };

  private static class RMIExecutor<R, I extends Remote> extends RMIRetryExecutor<R, I, XynaException> {

    private RMIRunnable<R, I, XynaException> runnable;


    public RMIExecutor(RMIRunnable<R, I, XynaException> runnable) {
      this.runnable = runnable;
      this.name = runnable.getClass().getName();
      noConHandler(new RMIConnectionNotAvailableHandler() {

        @Override
        public <I extends Remote> I getRmiImpl(RMIClusterProvider arg0, GenericRMIAdapter<I> arg1)
            throws com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionDownException {
          //TODO: �berlegen, ob mehr retries als der default notwendig sind, vgl defaultimplementierung von rmiconnectionnotavailablehandler
          //      der macht aber zugriff auf rmiclusterprovider, was hier nicht passt.
          throw new com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionDownException();
        }
      });
    }


    @Override
    public R execute(I clusteredInterface) throws RemoteException {
      try {
        return runnable.execute(clusteredInterface);
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
    }


    public R execute(GenericRMIAdapter<I> rmiAdapter) throws TimeoutException {
      try {
        return executeWithRetries(rmiAdapter);
      } catch (com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionDownException e) {
        throw new TimeoutException(e);
      } catch (RemoteException e) {
        throw new TimeoutException(e);
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
    }

  }


  public ClusterProviderManagement createRMIAdapter() {
    final GenericRMIAdapter<ClusterProviderMangementRMI> rmiAdapter;
    try {
      rmiAdapter = rmiManagement.createRMIAdapter(hostnameRemote, portRemote, BINDINGNAME);
      rmiAdapter.setClassLoader(getClass().getClassLoader());
    } catch (RMIConnectionFailureException e) {
      throw new RuntimeException(e); //runtimeException, weil hier die connection noch nicht versucht wird!
    }
    return new ClusterProviderManagement() {


      private final RMIExecutor<SyncResponse, ClusterProviderMangementRMI> executionSyncWasConnectedBefore =
          new RMIExecutor<SyncResponse, ClusterProviderMangementRMI>(
                                                                     new RMIRunnable<SyncResponse, ClusterProviderMangementRMI, XynaException>() {

                                                                       @Override
                                                                       public SyncResponse execute(ClusterProviderMangementRMI cm)
                                                                           throws XynaException, RemoteException {
                                                                         return cm.syncWasConnectedBefore();
                                                                       }

                                                                     });


      @Override
      public SyncResponse syncWasConnectedBefore() throws TimeoutException {
        logger.debug("calling over rmi: syncWasConnectedBefore");
        return executionSyncWasConnectedBefore.execute(rmiAdapter);
      }


      private final RMIExecutor<SyncResponse, ClusterProviderMangementRMI> executionSyncWasNeverConnectedBefore =
          new RMIExecutor<SyncResponse, ClusterProviderMangementRMI>(
                                                                     new RMIRunnable<SyncResponse, ClusterProviderMangementRMI, XynaException>() {

                                                                       @Override
                                                                       public SyncResponse execute(ClusterProviderMangementRMI cm)
                                                                           throws XynaException, RemoteException {
                                                                         return cm.syncWasNeverConnectedBefore();
                                                                       }

                                                                     });


      @Override
      public SyncResponse syncWasNeverConnectedBefore() throws TimeoutException {
        logger.debug("calling over rmi: syncWasNeverConnectedBefore");
        return executionSyncWasNeverConnectedBefore.execute(rmiAdapter);
      }


      private final RMIExecutor<SyncResponse, ClusterProviderMangementRMI> executionSyncWasMaster =
          new RMIExecutor<SyncResponse, ClusterProviderMangementRMI>(
                                                                     new RMIRunnable<SyncResponse, ClusterProviderMangementRMI, XynaException>() {

                                                                       @Override
                                                                       public SyncResponse execute(ClusterProviderMangementRMI cm)
                                                                           throws XynaException, RemoteException {
                                                                         return cm.syncWasMaster();
                                                                       }

                                                                     });


      @Override
      public SyncResponse syncWasMaster() throws TimeoutException {
        logger.debug("calling over rmi: syncWasMaster");
        return executionSyncWasMaster.execute(rmiAdapter);
      }


      @Override
      public void changeState(ClusterState newState) throws TimeoutException {
        logger.debug("calling over rmi: changeState");
        new RMIExecutor<String, ClusterProviderMangementRMI>(new RmiRunnableChangeState(newState)).execute(rmiAdapter);
      }


      private final RMIExecutor<String, ClusterProviderMangementRMI> executionSyncFinished =
          new RMIExecutor<String, ClusterProviderMangementRMI>(
                                                               new RMIRunnable<String, ClusterProviderMangementRMI, XynaException>() {

                                                                 @Override
                                                                 public String execute(ClusterProviderMangementRMI cm)
                                                                     throws XynaException, RemoteException {
                                                                   cm.syncFinished();
                                                                   return null;
                                                                 }


                                                               });


      @Override
      public void syncFinished() throws TimeoutException {
        logger.debug("calling over rmi: syncFinished");
        executionSyncFinished.execute(rmiAdapter);
      }


      private final RMIExecutor<PersistenceProcessingState, ClusterProviderMangementRMI> executionPersistenceFinished =
          new RMIExecutor<PersistenceProcessingState, ClusterProviderMangementRMI>(
                                                                                   new RMIRunnable<PersistenceProcessingState, ClusterProviderMangementRMI, XynaException>() {

                                                                                     @Override
                                                                                     public PersistenceProcessingState execute(ClusterProviderMangementRMI cm)
                                                                                         throws XynaException,
                                                                                         RemoteException {
                                                                                       return cm.persistenceFinished();
                                                                                     }


                                                                                   });


      @Override
      public PersistenceProcessingState persistenceFinished() throws TimeoutException {
        logger.debug("calling over rmi: persistenceFinished");
        return executionPersistenceFinished.execute(rmiAdapter);
      }

    };
  }


  public void createRMIImpl(final ClusterManagement clusterManagement,
                            final PersistenceStrategyImpl persistenceStrategyImpl) throws XMCP_RMI_BINDING_ERROR {

    rmiImplProxyClusterMgmt =
        rmiManagement.<ClusterProviderMangementRMI> createRMIImplProxy(new ClusterProviderMangementRMI() {

          @Override
          public SyncResponse syncWasNeverConnectedBefore() throws RemoteException {
            logger.debug("got called over rmi: syncWasNeverConnectedBefore");
            try {
              return clusterManagement.getThisNodeImpl().syncWasNeverConnectedBefore();
            } catch (TimeoutException e) {
              throw new RuntimeException(e); //sollte nicht passieren
            }
          }


          @Override
          public SyncResponse syncWasMaster() throws RemoteException {
            logger.debug("got called over rmi: syncWasMaster");
            try {
              return clusterManagement.getThisNodeImpl().syncWasMaster();
            } catch (TimeoutException e) {
              throw new RuntimeException(e); //sollte nicht passieren
            }
          }


          @Override
          public SyncResponse syncWasConnectedBefore() throws RemoteException {
            logger.debug("got called over rmi: syncWasConnectedBefore");
            try {
              return clusterManagement.getThisNodeImpl().syncWasConnectedBefore();
            } catch (TimeoutException e) {
              throw new RuntimeException(e); //sollte nicht passieren
            }
          }


          @Override
          public void changeState(ClusterState newState) throws RemoteException {
            logger.debug("got called over rmi: changeState");
            try {
              clusterManagement.getThisNodeImpl().changeState(newState);
            } catch (TimeoutException e) {
              throw new RuntimeException(e); //sollte nicht passieren
            }
          }


          @Override
          public void syncFinished() throws RemoteException {
            logger.debug("got called over rmi: syncFinished");
            try {
              clusterManagement.getThisNodeImpl().syncFinished();
            } catch (TimeoutException e) {
              throw new RuntimeException(e); //sollte nicht passieren
            }
          }


          @Override
          public PersistenceProcessingState persistenceFinished() {
            logger.debug("got called over rmi: persistenceFinished");
            PersistenceProcessingState ret = new PersistenceProcessingState();
            ret.numberOfPersistedRequests = persistenceStrategyImpl.getOverallCounter();
            ret.currentlyWaitingRequests =
                persistenceStrategyImpl.getQueueProcessor().getCurrentlyWaitingRequests()
                    + persistenceStrategyImpl.getRequestsCurrentlyWorkedOn();
            if (logger.isTraceEnabled()) {
              logger.trace("returning numberOfPersistedRequests=" + ret.numberOfPersistedRequests
                  + ", currentlyWaitingRequests=" + ret.currentlyWaitingRequests);
            }
            try {
              persistenceStrategyImpl.getQueueProcessor().persistRemainingDataASynchronously();
            } catch (PersistenceException e) {
              throw new RuntimeException(e);
            }
            return ret;
          }


        }, BINDINGNAME, hostnameLocal, rmiPortRegistryLocal, rmiPortCommunicationLocal, false);
  }


  public void unregister() {
    rmiImplProxyClusterMgmt.unregister(true);
  }


}
