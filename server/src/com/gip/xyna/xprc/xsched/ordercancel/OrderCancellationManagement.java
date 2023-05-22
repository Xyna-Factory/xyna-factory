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


package com.gip.xyna.xprc.xsched.ordercancel;



import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResult;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean.CANCEL_RESULT;



public class OrderCancellationManagement extends FunctionGroup
    implements
      Clustered,
      ClusterStateChangeHandler,
      RMIImplFactory<OrderCancellationRemoteInterfaceImpl> {

  public static final String DEFAULT_NAME = "OrderCancellationManagement";
  public static final Logger logger = CentralFactoryLogging.getLogger(OrderCancellationManagement.class);

  public int FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private volatile ClusterState rmiClusterState = ClusterState.NO_CLUSTER;
  private volatile boolean isReadyForChange = true;

  private boolean rmiIsClustered = false;
  private long rmiClusterInstanceId = 0;
  private RMIClusterProvider rmiClusterInstance = null;
  private long clusteredOrderCancellationManagementInterfaceID = 0;
  private ClusterState currentState;

  
  enum SOURCE {
    LOCALHOST, REMOTEHOST
  };

  private ClusterStateChangeHandler clusterStateChangeHandler = new ClusterStateChangeHandler() {

    public boolean isReadyForChange(ClusterState newState) {
      return true; //immer bereit
    }


    public void onChange(ClusterState newState) {
      rmiClusterState = newState;
    }

  };


  public OrderCancellationManagement() throws XynaException {
    super();
  }


  public CANCEL_RESULT processCancellation(final CancelBean bean) {
    boolean success = processCancellationLocally(bean);
    
    if (success) {
      return CANCEL_RESULT.SUCCESS;
    } else {
      // we were not successful locally, so we try the Cancellation on the remote machine
      if (rmiIsClustered && rmiClusterState == ClusterState.CONNECTED) {
        List<Boolean> remoteNodeshandled = null;

        try {
          remoteNodeshandled =
              RMIClusterProviderTools
                  .executeAndCumulate(rmiClusterInstance,
                                      clusteredOrderCancellationManagementInterfaceID,
                                      new RMIRunnable<Boolean, ClusteredOrderCancellationManagementInterface, XynaException>() {

                                        public Boolean execute(ClusteredOrderCancellationManagementInterface clusteredInterface)
                                            throws RemoteException, XynaException {
                                          return clusteredInterface.processCancellation(bean);
                                        }
                                      }, null);
        } catch (InvalidIDException e) {
          throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
        } catch (XynaException e) {
          logger.error("Error while cancelling order remotely.", e);
          throw new RuntimeException("Error while cancelling order " + bean.getIdToBeCanceled() + " remotely", e);
        }

        for (Boolean handled : remoteNodeshandled) {
          if (handled.booleanValue()) {
            return CANCEL_RESULT.SUCCESS;
          }
        }
      }
      
      // we still have no positive result from our remote nodes
      if ( bean.getRelativeTimeout() != null ) {
        return CANCEL_RESULT.WORK_IN_PROGRESS;
      } else {
        return CANCEL_RESULT.FAILED;
      }
    }
  }

  
  // executed on every node
  public boolean processCancellationLocally(CancelBean bean) {
    bean.setResult(CANCEL_RESULT.WORK_IN_PROGRESS);
    
    if (logger.isDebugEnabled()) {
      logger.debug("canceling order " + bean.getIdToBeCanceled() + " ...");
    }
    
    boolean instantSuccess =
      XynaFactory.getInstance().getProcessing().getXynaScheduler().cancelOrder(bean.getIdToBeCanceled(), null);

    return instantSuccess;
  }
  
  
  private class LatchResults {

    private CountDownLatch latch = null;
    private Map<SOURCE, CANCEL_RESULT> results = null;


    LatchResults(int resultCount) {
      this.latch = new CountDownLatch(resultCount);
      results = new HashMap<SOURCE, CANCEL_RESULT>();
    }


    CountDownLatch getLatch() {
      return latch;
    }


    Map<SOURCE, CANCEL_RESULT> getResults() {
      return results;
    }


    void setResult( SOURCE source, CANCEL_RESULT result) {
      results.put( source, result );
    }
  }

  
  private HashMap<Long, LatchResults> orderIDLatchResultsMap = new HashMap<Long, OrderCancellationManagement.LatchResults>();
  

  public CANCEL_RESULT processCancellationAndWait(final CancelBean bean) throws XPRC_UNEXPECTED_ERROR_PROCESS {
    int latchCount = 1;

    if (rmiIsClustered && (rmiClusterState == ClusterState.CONNECTED )) {
        latchCount = 2;
    }

    LatchResults latch = new LatchResults(latchCount);
    orderIDLatchResultsMap.put(bean.getIdToBeCanceled(), latch);

    try {
      boolean success = processCancellationAndWaitLocally(bean);

      if (success) {
        return CANCEL_RESULT.SUCCESS;
      } else {
        // we were not successful locally, so we try the Cancellation on the remote machine
        if (rmiIsClustered && rmiClusterState == ClusterState.CONNECTED) {
          List<Boolean> remoteNodeshandled = null;

          try {
            remoteNodeshandled =
                RMIClusterProviderTools
                    .executeAndCumulate(rmiClusterInstance,
                                        clusteredOrderCancellationManagementInterfaceID,
                                        new RMIRunnable<Boolean, ClusteredOrderCancellationManagementInterface, XynaException>() {

                                          public Boolean execute(ClusteredOrderCancellationManagementInterface clusteredInterface)
                                              throws RemoteException, XynaException {
                                            return clusteredInterface.processCancellationAndWait(bean);
                                          }
                                        }, null);
          } catch (InvalidIDException e) {
            throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
          } catch (XynaException e) {
            logger.error("Error while cancelling order remotely.", e);
            throw new RuntimeException("Error while cancelling order " + bean.getIdToBeCanceled()
                + " remotely", e);
          }

          for (Boolean handled : remoteNodeshandled) {
            if (handled.booleanValue()) {
              return CANCEL_RESULT.SUCCESS;
            }
          }
        }

        // let's wait for all cancel orders to report their results and then evaluate them.
        try {
          boolean gotAllResults = latch.getLatch().await( bean.getRelativeTimeout() + Constants.DEFAULT_CANCEL_TIMEOUT, TimeUnit.MILLISECONDS );
          
          if (!gotAllResults) {
            logger.debug( "Not all nodes responded to the OrderCancellation request! orderID = " + bean.getIdToBeCanceled() );
          }
        } catch (InterruptedException e) {
          if (XynaFactory.getInstance().isShuttingDown()) {
            throw new XPRC_UNEXPECTED_ERROR_PROCESS(DEFAULT_NAME,
                                                    "Cancel was interrupted during shutdown before cancel succeeded", e);
          } else {
            throw new XPRC_UNEXPECTED_ERROR_PROCESS(DEFAULT_NAME, "Cancel was interrupted unexpectedly", e);
          }
        }

        for (Entry<SOURCE, CANCEL_RESULT> result : latch.getResults().entrySet()) {
          if (result.getValue().equals(CANCEL_RESULT.SUCCESS)) {
            return CANCEL_RESULT.SUCCESS;
          }
        }

        return CANCEL_RESULT.FAILED;
      }
    } finally {
      orderIDLatchResultsMap.remove(bean.getIdToBeCanceled());
    }
  }

  
  public void processCancellationListenerResult(Long orderID, SOURCE source, CANCEL_RESULT result) {
    LatchResults lr = orderIDLatchResultsMap.get(orderID);

    if (lr != null) {
      lr.setResult(source, result);
      lr.getLatch().countDown();

      // we can immediate release the latch if there is a success
      if (result.equals(CANCEL_RESULT.SUCCESS)) {
        for (long i = lr.getLatch().getCount(); i > 0; i--) {
          lr.getLatch().countDown();
        }
      }
    }
  }
  

  private boolean processCancellationAndWaitLocally(final CancelBean bean) {
    if (logger.isDebugEnabled()) {
      logger.debug("canceling order " + bean.getIdToBeCanceled() + " ...");
    }

    final ICancelResultListener localListener = new ICancelResultListener() {

      // the results are automatically converted into the cancel order response object, no
      // need to do something here

      @Override
      public void cancelFailed() {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not cancel order " + bean.getIdToBeCanceled());
        }

        processCancellationListenerResult(bean.getIdToBeCanceled(), SOURCE.LOCALHOST, CANCEL_RESULT.FAILED);
      }


      @Override
      public void cancelSucceeded() {
        if (logger.isDebugEnabled()) {
          logger.debug("Successfully canceled order " + bean.getIdToBeCanceled());
        }

        processCancellationListenerResult(bean.getIdToBeCanceled(), SOURCE.LOCALHOST, CANCEL_RESULT.SUCCESS);
      }

    };

    if (bean.getRelativeTimeout() != null) {
      localListener.setAbsoluteCancelTimeout(System.currentTimeMillis() + bean.getRelativeTimeout());
    } else {
      localListener.setAbsoluteCancelTimeout(System.currentTimeMillis() + Constants.DEFAULT_CANCEL_TIMEOUT);
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("cancel timeout set to absolute value <"
          + Constants.defaultUTCSimpleDateFormat().format(new Date(localListener.getAbsoluteCancelTimeout())) + ">");
    }
    
    final boolean instantSuccess =
        XynaFactory.getInstance().getProcessing().getXynaScheduler()
            .cancelOrder(bean.getIdToBeCanceled(), localListener);

    return instantSuccess;
  }
  
  
  // this is executed on a remote node
  private void reportRemoteResult(final CancelBean bean, final CANCEL_RESULT result) {
    if (rmiIsClustered && rmiClusterState == ClusterState.CONNECTED) {
      try {
        RMIClusterProviderTools
            .execute(rmiClusterInstance, clusteredOrderCancellationManagementInterfaceID,
                     new RMIRunnableNoResult<ClusteredOrderCancellationManagementInterface, XynaException>() {

                       public void execute(ClusteredOrderCancellationManagementInterface clusteredInterface)
                           throws XynaException, RemoteException {
                         clusteredInterface.reportCancellationListenerResult(bean.getIdToBeCanceled(),
                                                                             SOURCE.REMOTEHOST, result);
                       }
                     });

      } catch (InvalidIDException e) {
        throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
      } catch (XynaException e) {
        logger.error("Error while reporting cancelliation result.", e);
        throw new RuntimeException("Error while cancelling order " + bean.getIdToBeCanceled() + " remotely", e);
      }
    }
  }
  
  
  //this is executed on a remote node
  public boolean processCancellationAndWaitRemotely(final CancelBean bean) {
    if (logger.isDebugEnabled()) {
      logger.debug("canceling order " + bean.getIdToBeCanceled() + " ...");
    }
    
    final ICancelResultListener remoteListener = new ICancelResultListener() {

      // the results are automatically converted into the cancel order response object, no
      // need to do something here

      @Override
      public void cancelFailed() {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not cancel order " + bean.getIdToBeCanceled());
        }

        reportRemoteResult(bean, CANCEL_RESULT.FAILED);
      }


      @Override
      public void cancelSucceeded() {
        if (logger.isDebugEnabled()) {
          logger.debug("Successfully canceled order " + bean.getIdToBeCanceled());
        }

        reportRemoteResult(bean, CANCEL_RESULT.SUCCESS);
      }

    };
    
    if (bean.getRelativeTimeout() != null) {
      remoteListener.setAbsoluteCancelTimeout(System.currentTimeMillis() + bean.getRelativeTimeout());
    } else {
      remoteListener.setAbsoluteCancelTimeout(System.currentTimeMillis() + Constants.DEFAULT_CANCEL_TIMEOUT);
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("cancel timeout set to absolute value <"
          + Constants.defaultUTCSimpleDateFormat().format(new Date(remoteListener.getAbsoluteCancelTimeout())) + ">");
    }
    
    boolean instantSuccess =
      XynaFactory.getInstance().getProcessing().getXynaScheduler().cancelOrder(bean.getIdToBeCanceled(), remoteListener);

    return instantSuccess;
  }
  
  
  
  public boolean isClustered() {
    return rmiIsClustered;
  }


  public long getClusterInstanceId() {
    if (!rmiIsClustered) {
      throw new IllegalStateException("Component is not clustered.");
    }

    return rmiClusterInstanceId;
  }
  

  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    this.rmiClusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    rmiClusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);

    if (rmiClusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }

    try {
      clusteredOrderCancellationManagementInterfaceID =
          rmiClusterInstance.addRMIInterfaceWithClassReloading("RemoteOrderCancellationManagment", this);
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterComponentConfigurationException(getName(), clusterInstanceId, e);
    }

    rmiIsClustered = true;

    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, clusterStateChangeHandler);

    rmiClusterState = rmiClusterInstance.getState();
  }


  public void disableClustering() {
    rmiIsClustered = false;
    rmiClusterState = ClusterState.NO_CLUSTER;
    rmiClusterInstance = null;
    clusteredOrderCancellationManagementInterfaceID = 0;
    rmiClusterInstanceId = 0;
    XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
        .removeClusterStateChangeHandler(rmiClusterInstanceId, clusterStateChangeHandler);
  }


  public String getName() {
    return getDefaultName();
  }


  public boolean isReadyForChange(ClusterState newState) {
    return isReadyForChange;
    // TODO : zwischen cancel aufrufen auf false setzen, damit nicht alle auf einmal laufen.
  }


  public void onChange(final ClusterState newState) {
    if (logger.isDebugEnabled()) {
      logger.debug("Got notified of state transition " + currentState + " -> " + newState);
    }
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(OrderCancellationManagement.class, OrderCancellationManagement.class.getSimpleName())
         .before(XynaClusteringServicesManagement.class)
         .execAsync(this::initCluster);
  }

  private void initCluster() {
    try {
      XynaClusteringServicesManagement.getInstance().registerClusterableComponent(OrderCancellationManagement.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException("Failed to register " + OrderCancellationManagement.class.getSimpleName() + " as clusterable component.", e);
    }
  }

  @Override
  protected void shutdown() throws XynaException {
  }


  public void init(InitializableRemoteInterface rmiImpl) {
    rmiImpl.init(this);
  }


  public String getFQClassName() {
    return OrderCancellationRemoteInterfaceImpl.class.getName();
  }


  public void shutdown(InitializableRemoteInterface rmiImpl) {
  }

}
