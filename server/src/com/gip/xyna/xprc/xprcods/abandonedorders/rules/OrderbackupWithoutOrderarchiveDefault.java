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



public class OrderbackupWithoutOrderarchiveDefault extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {

  private static PreparedQuery<OrderInstanceBackup> readOrderbackupsWithRootOrderId;

  public OrderbackupWithoutOrderarchiveDefault() throws PersistenceLayerException {
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
    // orderbackup existiert, aber orderarchive DEFAULT nicht, obwohl monitoringlevel > 5 ist (nur für xynaorder != null)

    int targetBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);

    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();

    int foundCount = 0;
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<OrderInstanceBackup> query =
          con.prepareQuery(new Query<OrderInstanceBackup>("select * from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_XYNAORDER + " is not null and " + OrderInstanceBackup.COL_BINDING + "=?",
                                                          OrderInstanceBackup
                                                              .getReaderWarnIfNotDeserializableNoDetails(), 
                                                              OrderInstanceBackup.TABLE_NAME));

      List<OrderInstanceBackup> orders = con.query(query, new Parameter(targetBinding), Integer.MAX_VALUE);

      for (OrderInstanceBackup order : orders) {
        if (order.getXynaorder() != null && order.getXynaorder().getMonitoringCode() != null && order.getXynaorder().getMonitoringCode() > 5) {
          if (!con.containsObject(new OrderInstance(order.getId()))) {
            foundCount++;
            result.add(new AbandonedOrderDetails(order.getId(), order.getRootId()));
          }
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
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The orderbackup entry with id <" + information.getOrderID() + "> has no entry in orderarchive DEFAULT";
  }


  @Override
  public String getShortName() {
    return "Orderbackup entry without entry in orderarchive DEFAULT";
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
