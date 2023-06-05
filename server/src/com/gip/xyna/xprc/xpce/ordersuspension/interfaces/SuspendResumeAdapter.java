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
package com.gip.xyna.xprc.xpce.ordersuspension.interfaces;

import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.NoSuchChildException;
import com.gip.xyna.xprc.xpce.ordersuspension.OrderBackupNotFoundException;
import com.gip.xyna.xprc.xpce.ordersuspension.OrderBackupNotAccessibleException;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension;
import com.gip.xyna.xprc.xpce.ordersuspension.RootSRInformation;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformation;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.DoResume;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 *
 */
public interface SuspendResumeAdapter<C,O> {

  void setSuspendResumeAlgorithm(SuspendResumeAlgorithm<C,O> suspendResumeAlgorithm);
  
  /**
   * Startet den Auftrag neu, indem dieser in den Scheduler eingestellt wird.
   * @param rootSRInformation (bereits unlocked!)
   * @param con
   * @throws PersistenceLayerException
   */
  void startOrder(RootSRInformation<O> rootSRInformation, C con) throws PersistenceLayerException;

  /**
   * Sofortiges Fortsetzen des Auftrags, indem dieser in den Scheduler eingestellt wird. 
   */
  void rescheduleOrder(O order);

  /**
   * Supendieren des Auftrags, indem dieser gebackupt wird //FIXME aufräumen?
   * @param order
   * @param rootOrderSuspension
   * @param suspensionCause
   * @param backup
   * @throws PersistenceLayerException //FIXME
   */
  void suspendOrder(O order, RootOrderSuspension rootOrderSuspension, SuspensionCause suspensionCause, boolean backup) throws PersistenceLayerException;
  
  /**
   * Abbrechen einer fehlgeschlagenen Suspendierung
   * @param rootSRInformation
   * @param orderId
   * @param cause
   */
  void abortOrderSuspension(RootSRInformation<O> rootSRInformation, long orderId, Throwable cause);
  
  /**
   * @param srInformation
   * @param step
   */
  void addAllParallelExecutors( SRInformation srInformation, Step step);

  /**
   * @param orderId
   * @param con
   * @return
   * @throws PersistenceLayerException
   * @throws OrderBackupNotFoundException 
   * @throws OrderBackupNotAccessibleException 
   */
  O readOrder(Long orderId, C con) throws PersistenceLayerException, OrderBackupNotFoundException, OrderBackupNotAccessibleException;
  
  
  void writeOrders(C con, Collection<O> orders) throws PersistenceLayerException;
  
  /**
   * @param order
   * @return
   */
  Long getOrderId(O order);
  
  /**
   * @param order
   * @return
   */
  Long getRootOrderId(O order);
  
  O getRootOrder(O order);

  /**
   * @param srInformation
   * @param order (kann null sein!)
   */
  void fillOrderData( SRInformation srInformation, O order );
  
  /**
   * @param orderId
   * @return
   * @throws PersistenceLayerException 
   * @throws OrderBackupNotFoundException 
   */
  Long getRootId(Long orderId) throws PersistenceLayerException, OrderBackupNotFoundException;
  
  /**
   * @param orderId
   * @param rootSRInformation
   * @return
   * @throws NoSuchChildException
   */
  O extractOrder(Long orderId, RootSRInformation<O> rootSRInformation) throws NoSuchChildException;
    
  /**
   * @param rootSRInformation
   * @param orderId
   * @param con
   * @throws PersistenceLayerException
   */
  void cleanupOrderFamily(RootSRInformation<O> rootSRInformation, Long orderId, C con) throws PersistenceLayerException;

  /**
   * Eintragen der RootOrderSuspension in den XynaProcess, damit eine Suspendierung vom Root beginnend 
   * ausgeführt werden kann.
   * @param rootSRInformation
   * @param rootOrderSuspension
   * @return true, wenn Suspendierung begonnen wurde
   */
  boolean suspendInExecution(RootSRInformation<O> rootSRInformation, RootOrderSuspension rootOrderSuspension);
  
  /**
   * Ssupendierung aller Kindaufträge im Scheduler und Eintragen der ResumeTargets in RootOrderSuspension
   * @param rootSRInformation
   * @param rootOrderSuspension
   * @return
   */
  boolean suspendInScheduler(RootSRInformation<O> rootSRInformation, RootOrderSuspension rootOrderSuspension);

  /**
   * Resume mehrerer Root-Aufträge mit Retries
   * @param rootOrderSuspensions
   * @return
   */
  List<Triple<RootOrderSuspension, String, PersistenceLayerException>> resumeRootOrdersWithRetries(List<RootOrderSuspension> rootOrderSuspensions);

  /**
   * Schickt Interrupts an alle ausführenden Threads
   * @param rootSRInformation
   * @param rootOrderSuspension
   * @param stopForcefully
   */
  void interruptProcess(RootSRInformation<O> rootSRInformation, RootOrderSuspension rootOrderSuspension, boolean stopForcefully);

  /**
   * Wartet, bis RootOrders im OrderBackup zugänglich sind
   * @param retry
   * @param rootId
   * @return
   */
  int waitUntilRootOrderIsAccessible(int retry, Long rootId);

  Pair<ResumeResult, String> abortSuspendedOrder(DoResume<C> doResume, RootSRInformation<O> rootSRInformation, long orderId,
                                                 boolean ignoreResourcesWhenResuming) throws PersistenceLayerException;
    
}
