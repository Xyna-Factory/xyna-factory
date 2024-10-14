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
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class OrderbackupWithoutParentAndWithoutXynaOrder extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {


  // FIXME: ... where rootorderid = id ... können dies auch nicht DB-PLs verstehen?
  // BG: Nope, java.lang.NumberFormatException: For input string: "id" für Mem-PL
  private static final String READ_TARGETS_SQL = "select * from " + OrderInstanceBackup.TABLE_NAME + " where "
      + OrderInstanceBackup.COL_ROOT_ID + "=" + OrderInstanceBackup.COL_ID + " and "
      + OrderInstanceBackup.COL_XYNAORDER + " is null and " + OrderInstanceBackup.COL_BINDING + "=? order by "
      + OrderInstanceBackup.COL_ID + " asc";
  private final PreparedQuery<OrderInstanceBackup> readTargetOrderInstanceBackups;

  private static PreparedQuery<OrderInstanceBackup> readOrderbackupsWithRootOrderId;


  public OrderbackupWithoutParentAndWithoutXynaOrder() throws PersistenceLayerException {
    super(false);
    ODSConnection defCon = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      readTargetOrderInstanceBackups =
          defCon.prepareQuery(new Query<OrderInstanceBackup>(READ_TARGETS_SQL, OrderInstanceBackup
                                  .getReaderWarnIfNotDeserializable()), true);
    } finally {
      defCon.closeConnection();
    }

    if (readOrderbackupsWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readOrderbackupsWithRootOrderId =
          con.prepareQuery(new Query<OrderInstanceBackup>("select * from " + OrderInstanceBackup.TABLE_NAME
              + " where " + OrderInstanceBackup.COL_ROOT_ID + "=? and " + OrderInstanceBackup.COL_BINDING + "=?",
                                                          new OrderInstanceBackup().getReader()), true);
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

    int targetBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);

    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();

    ODSConnection defaultConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      List<OrderInstanceBackup> results =
          defaultConnection.query(readTargetOrderInstanceBackups, new Parameter(targetBinding), maxrows);
      if (results.size() > 0) {
        for (OrderInstanceBackup oib : results) {
          boolean broken;
          if (oib.getDetails() == null) {
            // no parent, no order, no details => broken in any case
            broken = true;
          } else if (OrderInstanceStatus.WAITING_FOR_PREDECESSOR == oib.getDetails().getStatusAsEnum() ) {
            // order is part of a series and thus not broken FIXME nur für alte Huckepack-Aufträge 
            broken = false;
          } else {
            // xynaorder = null and no parent is only allowed for waiting series orders
            broken = true;
          }
          if (broken) {
            AbandonedOrderDetails newInfo = new AbandonedOrderDetails(oib.getId(), oib.getRootId());
            result.add(newInfo);
          }
        }
      }
    } finally {
      defaultConnection.closeConnection();
    }

    return result;

  }


  @Override
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The order family with root order id <" + information.getRootOrderID()
        + "> contains insufficient information within orderbackup.";
  }


  @Override
  public String getShortName() {
    return "Orderbackup without parent and without order data";
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
