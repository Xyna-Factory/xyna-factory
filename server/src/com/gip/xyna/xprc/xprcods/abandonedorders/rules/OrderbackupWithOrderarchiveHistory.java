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
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetectionRule;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public class OrderbackupWithOrderarchiveHistory extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {

  private static PreparedQuery<OrderInstanceBackup> readOrderbackupsWithRootOrderId;


  public OrderbackupWithOrderarchiveHistory() throws PersistenceLayerException {
    super(false);

    if (readOrderbackupsWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readOrderbackupsWithRootOrderId =
          con.prepareQuery(new Query<OrderInstanceBackup>("select * from " + OrderInstanceBackup.TABLE_NAME
              + " where " + OrderInstanceBackup.COL_ROOT_ID + "=? and " + OrderInstanceBackup.COL_BINDING + "=?",
                                                          new OrderInstanceBackup().getReader(),
                                                          OrderInstanceBackup.TABLE_NAME), true);
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
  public List<AbandonedOrderDetails> detect(int maxrows) throws PersistenceLayerException {
    // Orderbackup und Orderarchive HISTORY existieren
    // UND
    // Für die rootOrderId zu einem Eintrag in orderbackup gibt es einen Eintrag in orderarchive HISTORY

    int targetBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();

    int foundCount = 0;
    List<OrderInstanceBackup> orders;
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<OrderInstanceBackup> query =
          con.prepareQuery(new Query<OrderInstanceBackup>("select " + OrderInstanceBackup.COL_ID + ","
              + OrderInstanceBackup.COL_ROOT_ID + " from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_BINDING + "=?", OrderInstanceBackup.getSelectiveReader(), 
              OrderInstanceBackup.COL_ID));

      orders = con.query(query, new Parameter(targetBinding), Integer.MAX_VALUE);
    } finally {
      con.closeConnection();
    }
    ODSConnection conHistory = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {

      Set<Long> seenIds = new TreeSet<Long>();

      for (OrderInstanceBackup order : orders) {
        if (isIdInOrderArchiveHistory(conHistory, order.getId(), seenIds)
            || isIdInOrderArchiveHistory(conHistory, order.getRootId(), seenIds)) {
          foundCount++;
          result.add(new AbandonedOrderDetails(order.getId(), order.getRootId()));
        }

        if (foundCount >= maxrows) {
          break;
        }
      }

    } finally {
      conHistory.closeConnection();
    }


    return result;

  }


  private boolean isIdInOrderArchiveHistory(ODSConnection conHistory, long id, Set<Long> seenIds)
      throws PersistenceLayerException {
    // Im Set werden sich nur die Ids gemerkt, die nicht in HISTORY gefunden wurden.
    // Erwartungshaltung ist, dass Auftrag mit der Id nicht in HISTORY gefunden wird!
    if (seenIds.contains(id)) {
      return false;
    }
    if (conHistory.containsObject(new OrderInstance(id))) {
      return true;
    } else {
      seenIds.add(id);
      return false;
    }
  }


  @Override
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The orderbackup entry with id <" + information.getOrderID() + "> is also archived.";
  }


  @Override
  public String getShortName() {
    return "Orderbackup entry also has an entry in orderarchive HISTORY";
  }


  @Override
  public String describeSolution() {
    return "Auto resolving not supported.";
  }


  @Override
  public void forceClean(AbandonedOrderDetails information) {
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    
    try {
      con.deleteOneRow(new OrderInstanceBackup(information.getOrderID(), ownBinding));
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order backup entry with order id <" + information.getOrderID() + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }


  @Override
  public void forceCleanFamily(AbandonedOrderDetails information) {
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      List<OrderInstanceBackup> oibs =
          con.query(readOrderbackupsWithRootOrderId, new Parameter(information.getRootOrderID(), ownBinding), Integer.MAX_VALUE);
      con.delete(oibs);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order backup entries with root order id <" + information.getRootOrderID()
          + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }

}
