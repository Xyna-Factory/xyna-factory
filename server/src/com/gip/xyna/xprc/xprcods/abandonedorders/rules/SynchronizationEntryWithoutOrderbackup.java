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
package com.gip.xyna.xprc.xprcods.abandonedorders.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetectionRule;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;


public class SynchronizationEntryWithoutOrderbackup extends AbandonedOrderDetectionRule<SynchronizationEntryWithoutOrderbackupDetails> {

  private static PreparedQuery<SynchronizationEntry> readSynchronizationEntryWithOrderId;
  private static PreparedQuery<SynchronizationEntry> readSynchronizationEntriesWithRootOrderId;
  
  public SynchronizationEntryWithoutOrderbackup() {
    super(false);
    
    if (readSynchronizationEntryWithOrderId == null || readSynchronizationEntriesWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readSynchronizationEntryWithOrderId =
            con.prepareQuery(new Query<SynchronizationEntry>("select * from " + SynchronizationEntry.TABLE_NAME
                                 + " where " + SynchronizationEntry.COL_ORDER_ID_2_RESUME + "=?",
                                                               new SynchronizationEntry().getReader()), true);
//        readSynchronizationEntriesWithRootOrderId =
//            con.prepareQuery(new Query<SynchronizationEntry>("select * from " + SynchronizationEntry.TABLE_NAME
//                                 + " where " + SynchronizationEntry.COL_ROOT_ORDER_ID_2_RESUME + "=?",
//                                                               new SynchronizationEntry().getReader()), true);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Failed to prepare query. ", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to close connection.", e);
        }
      }
    }
  }
  
  
  @Override
  public List<SynchronizationEntryWithoutOrderbackupDetails> detect(int maxrows) throws PersistenceLayerException {
    // SynchronizationEntry zu einem await existiert, aber kein orderbackup-Eintrag (nicht zu einem "notify", das darf alleine stehen!)
  
    int foundCount = 0;
    List<SynchronizationEntryWithoutOrderbackupDetails> result = new ArrayList<SynchronizationEntryWithoutOrderbackupDetails>();
    
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection();
    try {
      // FIXME: woher wei� Knoten, ob SynchronizationEntry ihm oder dem anderen Knoten geh�rt???
      Collection<SynchronizationEntry> synchronizationEntries = ((XynaFractalWorkflowEngine) XynaFactory.getInstance()
                      .getProcessing().getWorkflowEngine()).getSynchronizationManagement()
                      .listCurrentSynchronizationEntries();

      for (SynchronizationEntry synchronizationEntry : synchronizationEntries) {
        if (synchronizationEntry.getAnswer() == null && !con
                        .containsObject(new OrderInstanceBackup(synchronizationEntry.getOrderId(), 0))) {
          foundCount++;
          result.add(new SynchronizationEntryWithoutOrderbackupDetails(synchronizationEntry.getCorrelationId(), synchronizationEntry.getOrderId()));
        }
        if (foundCount >= maxrows) {
          break;
        }
      }

    } finally {
      con.closeConnection();
    }
    return result;
  }

  @Override
  public void resolve(SynchronizationEntryWithoutOrderbackupDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }

  @Override
  public String describeProblem(SynchronizationEntryWithoutOrderbackupDetails information) {
    return "The synchronization entry for order <" + information.getOrderID() + "> has no orderbackup entry.";
  }

  @Override
  public String getShortName() {
    return "Synchronization entry without orderbackup";
  }

  @Override
  public String describeSolution() {
    return "Auto resolving not supported.";
  }

  @Override
  public void forceClean(AbandonedOrderDetails information) {
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      SynchronizationEntry se = con.queryOneRow(readSynchronizationEntryWithOrderId, new Parameter(information.getOrderID()));
      con.deleteOneRow(se);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Failed to force clean synchronization entry for order <" + information.getOrderID() + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.", e);
      }
    }
  }

  
  @Override
  public void forceCleanFamily(AbandonedOrderDetails information) {
    logger.warn( "forceCleanFamily not supported for " + this.getClass().getSimpleName() );
    //throw new RuntimeException( "forceCleanFamily not supported for " + this.getClass().getSimpleName() );
  } 
}
