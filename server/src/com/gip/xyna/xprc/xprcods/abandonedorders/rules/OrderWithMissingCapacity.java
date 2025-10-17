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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetectionRule;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xsched.CapacityStorable;
import com.gip.xyna.xprc.xsched.capacities.CapacityStorables;


public class OrderWithMissingCapacity extends AbandonedOrderDetectionRule<OrderWithMissingCapacityDetails>  {

  private static PreparedQuery<OrderInstance> readOrderarchivesWithRootOrderId;
  
  public OrderWithMissingCapacity() {
    super(false);
    
    if (readOrderarchivesWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
        String tableName = oa.getAuditAccess().getQueryBackingClass(con).getTableName();
        readOrderarchivesWithRootOrderId =
            con.prepareQuery(new Query<OrderInstance>("select * from " + tableName
                + " where " + OrderInstance.COL_ROOT_ID + "=?", new OrderInstance().getReader(), tableName), true);
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
  public List<OrderWithMissingCapacityDetails> detect(int maxrows) throws PersistenceLayerException {
    
    // FIXME eigentlich ist die Abfrage falsch, da hiermit nicht Aufträge gefunden werden,
    // die ihre Capacity-Zuordnung nur über das Planning bekommen

    // Auftrag benötigt Kapazität, die nicht existiert
    
    int foundCount = 0;
    List<OrderWithMissingCapacityDetails> result = new ArrayList<OrderWithMissingCapacityDetails>();
    
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection();
    
    try {
      OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
      PreparedQuery<OrderCount> querySelectOrderinstanceCount = 
            con.prepareQuery(new Query<OrderCount>("select count(*) from " + oa.getAuditAccess().getQueryBackingClass(con).getTableName() + " where " +
                            OrderInstance.COL_ORDERTYPE + "=?", OrderCount.getCountReader(), 
                            oa.getAuditAccess().getQueryBackingClass(con).getTableName()));
      
      PreparedQuery<OrderInstance> querySelectOrderinstances = 
        con.prepareQuery(new Query<OrderInstance>("select * from " + oa.getAuditAccess().getQueryBackingClass(con).getTableName() + " where " +
                        OrderInstance.COL_ORDERTYPE + "=?", new OrderInstance().getReaderWithoutExceptions(), 
                        oa.getAuditAccess().getQueryBackingClass(con).getTableName()));
      
      Collection<CapacityStorable> allCapacities = con.loadCollection(CapacityStorable.class);
      HashMap<String, CapacityStorables> hashedCapacities = new HashMap<String, CapacityStorables>();
      for(CapacityStorable cap : allCapacities) {
        CapacityStorables tmpCap = hashedCapacities.get(cap.getName());
        // Capacities mit unterschiedlichen Binding beachten. 
        if(tmpCap == null) {
          tmpCap = new CapacityStorables(0);
          hashedCapacities.put(cap.getName(), tmpCap);
        }          
        tmpCap.add(cap);
      }
      Collection<CapacityMappingStorable> capacities2orders = XynaFactory.getInstance().getProcessing().getAllCapacityMappings();
      
      for(CapacityMappingStorable c2oEntry : capacities2orders) {
        OrderCount count = con.queryOneRow(querySelectOrderinstanceCount, new Parameter(c2oEntry.getOrderType()));
        if(count != null && count.getCount() > 0) {
          List<Capacity> neededCapacities = c2oEntry.getRequiredCapacities();
          for(Capacity cap : neededCapacities) {
            CapacityStorables capacityStoreables = hashedCapacities.get(cap.getCapName());
            if(capacityStoreables == null || capacityStoreables.getTotalCardinality() < cap.getCardinality()) {
              // OrderIds ermitteln
              List<OrderInstance> orders = con.query(querySelectOrderinstances, new Parameter(c2oEntry.getOrderType()), maxrows - foundCount);
              for(OrderInstance order : orders) {
                result.add(new OrderWithMissingCapacityDetails(cap.getCapName(), c2oEntry.getOrderType(), order.getId(), order.getRootId()));
              }
              foundCount += orders.size();
              if(foundCount >= maxrows) {
                return result;
              }
            }
          }
        }
      }
    } finally {
      con.closeConnection();
    }
    return result;
  }

  @Override
  public void resolve(OrderWithMissingCapacityDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }

  @Override
  public String describeProblem(OrderWithMissingCapacityDetails information) {
    return "The order with id <" + information.getOrderID() + "> has not enough capacities of type <" + information.getCapacityName() + "> for " +
        "ordertype <" + information.getOrderType() + ">";
  }

  @Override
  public String getShortName() {
    return "Order with missing capacity";
  }

  @Override
  public String describeSolution() {
    return "Auto resolving not supported. Create a capacity with the name and cardinality that meets the ordertypes requirements.";
  }

  
  @Override
  public void forceClean(AbandonedOrderDetails information) {
    OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      oa.getAuditAccess().delete(con, new OrderInstance(information.getOrderID()));
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order archive entry with order id <" + information.getOrderID() + ">", e);
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
      logger.error("Error while force cleaning abandoned order archive entries with root order id <" + information.getRootOrderID()
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
