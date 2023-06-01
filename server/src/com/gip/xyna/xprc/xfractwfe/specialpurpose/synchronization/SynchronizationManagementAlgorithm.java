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
package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;

import java.util.Collection;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutAlgorithm;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement.TimeoutResult;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;



public interface SynchronizationManagementAlgorithm {

  String awaitNotification(SynchronizationEntry synchronizationEntry, Long rootOrderId, SuspensionCause suspensionCause,
                           ODSConnection defaultConnection)
      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException, XPRC_TIMEOUT_DURING_SYNCHRONIZATION;

  

  SynchronizationEntry notifyEntryAndDeleteCronJob(String correlationId, String answer, Integer internalStepId,
                                                     XynaOrderServerExtension xo, ODSConnection con)
      throws XPRC_DUPLICATE_CORRELATIONID, PersistenceLayerException;


  Collection<SynchronizationEntry> listCurrentSynchronizationEntries(ODSConnection con) throws PersistenceLayerException;



  TimeoutResult returnTimedoutEntry(final String correlationId, ODSConnection con) throws PersistenceLayerException;


  TimeoutAlgorithm getTimeoutAlgorithm();


  ODSConnectionType getConnectionTypeForFastAwait();
  
  boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con) throws PersistenceLayerException;

  
  
}
