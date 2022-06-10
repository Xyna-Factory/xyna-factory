/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
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
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public class OrderWithoutOrderbackup extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {

  private static PreparedQuery<OrderInstance> readOrderarchivesWithRootOrderId;


  public OrderWithoutOrderbackup() {
    super(false);
    OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    if (readOrderarchivesWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readOrderarchivesWithRootOrderId =
            con.prepareQuery(new Query<OrderInstance>("select * from " + oa.getAuditAccess().getQueryBackingClass(con).getTableName() + " where "
                + OrderInstance.COL_ROOT_ID + "=?", new OrderInstance().getReader()), true);
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
    // Auftrag steht im orderarchive DEFAULT aber existiert nicht im orderbackup

    int foundCount = 0;
    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();
    OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection();

    try {
      FactoryWarehouseCursor<? extends OrderInstance> cursor =
          con.getCursor("select * from " + oa.getAuditAccess().getQueryBackingClass(con).getTableName(), new Parameter(), new OrderInstance().getReader(),
                        500);

      List<? extends OrderInstance> next = cursor.getRemainingCacheOrNextIfEmpty();

      while (next != null && !next.isEmpty() && foundCount < maxrows) {
        for (OrderInstance oiEntry : next) {
          if (!con.containsObject(new OrderInstanceBackup(oiEntry.getId(), 0))) {
            foundCount++;
            result.add(new AbandonedOrderDetails(oiEntry.getId(), oiEntry.getRootId()));
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
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The order with id <" + information.getOrderID()
        + "> has an entry in order archive but no corresponding entry in order backup.";
  }


  @Override
  public String getShortName() {
    return "Order without order backup entry";
  }


  @Override
  public String describeSolution() {
    return "Auto resolving not supported.";
  }


  @Override
  public void forceClean(AbandonedOrderDetails information) {
    OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      oa.getAuditAccess().delete(con, new OrderInstance(information.getOrderID()));
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order archive entry with order id <"
                       + information.getOrderID() + ">", e);
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
    OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      List<OrderInstance> ois =
          con.query(readOrderarchivesWithRootOrderId, new Parameter(information.getRootOrderID()), Integer.MAX_VALUE);
      oa.getAuditAccess().delete(con, ois);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order archive entries with root order id <"
                       + information.getRootOrderID() + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }

}
