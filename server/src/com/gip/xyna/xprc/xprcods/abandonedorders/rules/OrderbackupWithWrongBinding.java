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

import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
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



public class OrderbackupWithWrongBinding extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {


  PreparedQuery<OrderInstanceBackup> querySelectOrderbackupWithNoClusterBinding;
  private static PreparedQuery<OrderInstanceBackup> readOrderbackupsWithRootOrderId;


  public OrderbackupWithWrongBinding() throws PersistenceLayerException {
    super(false);

    ODSConnection defCon = ODSImpl.getInstance().openConnection();
    try {
      querySelectOrderbackupWithNoClusterBinding =
          defCon.prepareQuery(new Query<OrderInstanceBackup>("select " + OrderInstanceBackup.COL_ID + ","
              + OrderInstanceBackup.COL_ROOT_ID + " from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_BINDING + "=?", OrderInstanceBackup.getSelectiveReader()), true);
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

    // Auftrag mit falschem Binding im orderbackup? z.b. 0

    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();

    ClusterState clusterstate = new OrderInstanceBackup().getClusterState(ODSConnectionType.DEFAULT);

    if (clusterstate != ClusterState.NO_CLUSTER) {
      ODSConnection con = ODSImpl.getInstance().openConnection();

      try {
        List<OrderInstanceBackup> orders =
            con.query(querySelectOrderbackupWithNoClusterBinding, new Parameter(0), maxrows);

        for (OrderInstanceBackup oib : orders) {
          result.add(new AbandonedOrderDetails(oib.getId(), oib.getRootId()));
        }
      } finally {
        con.closeConnection();
      }
    }

    return result;

  }


  @Override
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      OrderInstanceBackup oib = new OrderInstanceBackup(information.getOrderID(), -1);
      con.queryOneRowForUpdate(oib);
      oib.setBinding(oib.getLocalBinding(ODSConnectionType.DEFAULT));
      con.persistObject(oib);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Failed to resolve the order backup entry with id <" + information.getOrderID() + ">");
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // ignore
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close exception.", e);
      }
    }
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The order family with root order id <" + information.getOrderID()
        + "> has a wrong binding of 0 in a clustered enviroment.";
  }


  @Override
  public String getShortName() {
    return "Orderbackup with wrong binding";
  }


  @Override
  public String describeSolution() {
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    return "Setting binding to <" + ownBinding + ">";
  }


  @Override
  public void forceClean(AbandonedOrderDetails information) {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    
    try {
      con.deleteOneRow(new OrderInstanceBackup(information.getOrderID(), 0));
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
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      List<OrderInstanceBackup> oibs =
          con.query(readOrderbackupsWithRootOrderId, new Parameter(information.getRootOrderID(), 0), Integer.MAX_VALUE);
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
