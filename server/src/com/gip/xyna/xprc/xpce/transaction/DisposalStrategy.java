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
package com.gip.xyna.xprc.xpce.transaction;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xprc.exceptions.XPRC_TransactionOperationFailed;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.statustracking.IStatusChangeListener;
import com.gip.xyna.xprc.xpce.statustracking.StatusChangeProvider;
import com.gip.xyna.xprc.xpce.transaction.parameter.DisposalStrategyParameter;
import com.gip.xyna.xprc.xpce.transaction.parameter.OnGarbageCollection;
import com.gip.xyna.xprc.xpce.transaction.parameter.OnOrderTermination;
import com.gip.xyna.xprc.xpce.transaction.parameter.TTL;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionOperation;
import com.gip.xyna.xprc.xpce.transaction.parameter.TransactionParameter;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;

public abstract class DisposalStrategy {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(DisposalStrategy.class);
  
  protected final Long transactionId;
  
  protected DisposalStrategy(Long transactionId) {
    this.transactionId = transactionId;
  }

  
  public abstract void notifyOfOperation(TransactionOperation operation);
  
  protected abstract void init();
  
  public final static DisposalStrategy NO_GUARD = new NoDisposalGuard();
  
  public final static class NoDisposalGuard extends DisposalStrategy {
    
    protected NoDisposalGuard() {
      super(null);
    }

    public void init() { }
    
    public void notifyOfOperation(TransactionOperation operation) { }
    
  }
  
  public final static class ReferenceGuard extends DisposalStrategy {
    
    private final static ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final static BijectiveMap<Long, PhantomReference<Object>> referenceMap = new BijectiveMap<>();
    private static volatile Thread referenceGuardThread;
    
    protected ReferenceGuard(Long transactionId, OnGarbageCollection onGarbageCollection) {
      super(transactionId);
      PhantomReference<Object> ref = new PhantomReference<Object>(onGarbageCollection.getReference(), queue);
      referenceMap.put(transactionId, ref);
    }
    
    public void notifyOfOperation(TransactionOperation operation) {
      switch (operation) {
        case END :
          referenceMap.remove(transactionId);
          break;
          
        default :
          break;
      }
    }
    

    protected synchronized void init() {
      synchronized (ReferenceGuard.class) {
        if (referenceGuardThread == null) {
          final XynaFactoryBase factory = XynaFactory.getInstance();
          referenceGuardThread = new Thread(new Runnable() {

            @SuppressWarnings("unchecked")
            public void run() {
              while (!factory.isShuttingDown()) {
                Reference<? extends Object> phantom = queue.poll();
                if (phantom instanceof PhantomReference) {
                  Long txId = referenceMap.getInverse((PhantomReference<Object>)phantom);
                  if (txId == null) {
                    // ntbd, this is the case if the transaction was closed before the collection
                  } else {
                    try {
                      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getTransactionManagement().end(transactionId);
                    } catch (XPRC_TransactionOperationFailed e) {
                      logger.warn("ReferenceGuard failed to end the transaction " + transactionId, e);
                    } catch (Throwable t) {
                      Department.handleThrowable(t);
                      logger.error("ReferenceGuard failed to end the transaction " + transactionId, t);
                    }
                  }
                }
              }
            }
            
          });
          referenceGuardThread.setDaemon(true);
          referenceGuardThread.setName("ReferenceGuard - PhantomPoller");
          referenceGuardThread.start();
        }
      }
    }
    
  }
  
  
  
  public final static class TTLGuard extends DisposalStrategy implements Callable<Void> {

    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    
    private final boolean refresh;
    private final long timeout;
    private final AtomicBoolean terminated;
    private ScheduledFuture<Void> scheduledExecution;
    
    
    
    private TTLGuard(Long transactionId, TTL ttl) {
      super(transactionId);
      this.refresh = ttl.isRefresh();
      this.timeout = ttl.getTimeout();
      this.terminated = new AtomicBoolean(false);
    }
    
    
    public void notifyOfOperation(TransactionOperation operation) {
      switch (operation) {
        case END :
          if (terminated.compareAndSet(false, true)) {
            this.scheduledExecution.cancel(true);
          }
          break;
        default :
          if (refresh) {
            this.scheduledExecution.cancel(true);
            this.scheduledExecution = executor.schedule(this, timeout, TimeUnit.MILLISECONDS);
          }
          break;
      }
    }
    
    protected void init() {
      scheduledExecution = executor.schedule(this, timeout, TimeUnit.MILLISECONDS);
    }
      
    public Void call() throws Exception {
      if (terminated.compareAndSet(false, true)) {
        try {
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getTransactionManagement().end(transactionId);
        } catch (XPRC_TransactionOperationFailed e) {
          logger.warn("TTLGuard failed to end the transaction " + transactionId, e);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.error("TTLGuard failed to end the transaction " + transactionId,t);
        }
      }
      return null;
    }
      

  }
  
  
  public final static class OrderGuard extends DisposalStrategy implements IStatusChangeListener {
    
    private final ArrayList<DestinationKey> destinationKeyAsList;
    private final Long orderId;
    
    protected OrderGuard(Long transactionId, OnOrderTermination onOrderTermination) {
      super(transactionId);
      this.destinationKeyAsList = new ArrayList<>(1);
      destinationKeyAsList.add(onOrderTermination.getDestinationKey());
      this.orderId = onOrderTermination.getOrderId();
    }
    
    protected void init() {
      StatusChangeProvider provider = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider();
      provider.addStatusChangeListener(this);
    }

    public void notifyOfOperation(TransactionOperation operation) {
      switch (operation) {
        case END :
          StatusChangeProvider provider = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider();
          provider.removeStatusChangeListener(this);
          break;
          
        default :
          break;
      }
    }

    public void statusChanged(Long orderId, String newState, Long sourceId) {
      if (this.orderId.equals(orderId)) {
        try {
          OrderInstanceStatus ois = OrderInstanceStatus.fromString(newState);
          switch (ois.getStatusGroup()) {
            case Succeeded :
            case Failed :
              try {
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getTransactionManagement().end(transactionId);
              } catch (XPRC_TransactionOperationFailed e) {
                logger.warn("OrderGuard failed to end the transaction " + transactionId, e);
              } catch (Throwable t) {
                Department.handleThrowable(t);
                logger.error("OrderGuard failed to end the transaction " + transactionId, t);
              } 
              break;
            default :
              break;
          }
        } catch (IllegalArgumentException e) {
          // ntbd, methode wird z.b. auch f�r suspension zustands�berg�nge aufgerufen, die interessieren uns nicht
        }
      }
    }

    public ArrayList<DestinationKey> getWatchedDestinationKeys() {
      return destinationKeyAsList;
    }
  
  }


  public static DisposalStrategy create(Long transactionId, TransactionParameter tp) {
    DisposalStrategy guard;
    if (tp == null ||
        tp.getSafeguardParameter() == null ||
        tp.getSafeguardParameter().getDisposalStrategy() == null) {
      guard = NO_GUARD;
    } else {
      DisposalStrategyParameter strat = tp.getSafeguardParameter().getDisposalStrategy();
      if (strat instanceof OnGarbageCollection) {
        guard = new ReferenceGuard(transactionId, (OnGarbageCollection) strat);
      } else if (strat instanceof OnOrderTermination) {
        guard =  new OrderGuard(transactionId, (OnOrderTermination) strat);
      } else if (strat instanceof TTL) {
        guard =  new TTLGuard(transactionId, (TTL) strat);
      } else {
        guard =  NO_GUARD;
      }
    }
    guard.init();
    return guard;
  }
  
  
}
