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
package com.gip.xyna.xprc.xprcods.abandonedorders.rules;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetectionRule;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public class MIEntryWithoutOrderbackup extends AbandonedOrderDetectionRule<MIEntryWithoutOrderbackupDetails> {

  private static PreparedQuery<ManualInteractionEntry> readMIEntryWithOrderId;
  private static PreparedQuery<ManualInteractionEntry> readMIEntriesWithRootOrderId;


  public MIEntryWithoutOrderbackup() {
    super(false);

    if (readMIEntryWithOrderId == null || readMIEntriesWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readMIEntryWithOrderId =
            con.prepareQuery(new Query<ManualInteractionEntry>("select * from " + ManualInteractionEntry.TABLE_NAME
                + " where " + ManualInteractionEntry.MI_COL_XYNAORDER_ID + "=? and "
                + ManualInteractionEntry.COL_BINDING + "=?", new ManualInteractionEntry().getReader()), true);
        readMIEntriesWithRootOrderId =
            con.prepareQuery(new Query<ManualInteractionEntry>("select * from " + ManualInteractionEntry.TABLE_NAME
                + " where " + ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID + "=? and "
                + ManualInteractionEntry.COL_BINDING + "=?", new ManualInteractionEntry().getReader()), true);
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
  public List<MIEntryWithoutOrderbackupDetails> detect(int maxrows) throws PersistenceLayerException {
    // MI-Eintrag existiert, aber kein orderbackup-Eintrag

    int foundCount = 0;
    List<MIEntryWithoutOrderbackupDetails> result = new ArrayList<MIEntryWithoutOrderbackupDetails>();

    int ownBinding = new ManualInteractionEntry().getLocalBinding(ODSConnectionType.DEFAULT);

    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection();
    try {
      FactoryWarehouseCursor<? extends ManualInteractionEntry> cursor =
          con.getCursor("select * from " + ManualInteractionEntry.TABLE_NAME + " where "
                            + ManualInteractionEntry.COL_BINDING + "=?", new Parameter(ownBinding),
                        new ManualInteractionEntry().getReader(), 500);

      List<? extends ManualInteractionEntry> next = cursor.getRemainingCacheOrNextIfEmpty();

      while (next != null && !next.isEmpty() && foundCount < maxrows) {
        for (ManualInteractionEntry miEntry : next) {
          if (!con.containsObject(new OrderInstanceBackup(miEntry.getID(), 0))) {
            foundCount++;
            result.add(new MIEntryWithoutOrderbackupDetails(miEntry.getID(), miEntry.getXynaOrderId(), miEntry
                .getRootOrderId()));
          }
          if (foundCount >= maxrows) {
            break;
          }
        }
        next = cursor.getRemainingCacheOrNextIfEmpty();
      }
    } finally {
      con.closeConnection();
    }
    return result;
  }


  @Override
  public void resolve(MIEntryWithoutOrderbackupDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }


  @Override
  public String describeProblem(MIEntryWithoutOrderbackupDetails information) {
    return "The manual interaction entry <" + information.getManualInteractionEntryID() + "> for order id <"
        + information.getOrderID() + "> has no orderbackup entry.";
  }


  @Override
  public String getShortName() {
    return "MI entry without orderbackup";
  }


  @Override
  public String describeSolution() {
    return "Auto resolving not supported.";
  }


  @Override
  public void forceClean(AbandonedOrderDetails information) {
    int ownBinding = new ManualInteractionEntry().getLocalBinding(ODSConnectionType.DEFAULT);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      ManualInteractionEntry mi =
          con.queryOneRow(readMIEntryWithOrderId, new Parameter(information.getOrderID(), ownBinding));
      con.deleteOneRow(mi);
      con.commit();
    } catch (Exception e) {
      logger.error("Error while force cleaning abandoned order with id <" + information.getOrderID() + ">", e);
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
    int ownBinding = new ManualInteractionEntry().getLocalBinding(ODSConnectionType.DEFAULT);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      List<ManualInteractionEntry> mis =
          con.query(readMIEntriesWithRootOrderId, new Parameter(information.getRootOrderID(), ownBinding),
                    Integer.MAX_VALUE);
      con.delete(mis);
      con.commit();
    } catch (Exception e) {
      logger.error("Error while force cleaning abandoned orders with root order id <" + information.getRootOrderID()
          + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.", e);
      }
    }
  }
}
