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
package com.gip.xyna.xprc.xprcods.orderarchive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;

public enum AuditStorageAccess {
  // Previous implementation, using orderarchive only
  orderarchive() {
    
    public boolean store(ODSConnection con, OrderInstanceDetails oid) throws PersistenceLayerException {
      return con.persistObject(oid);
    }

    public OrderInstanceDetails restore(ODSConnection con, long id, boolean forUpdate) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      OrderInstanceDetails oid = new OrderInstanceDetails(id);
      if (forUpdate) {
        con.queryOneRowForUpdate(oid);
      } else {
        con.queryOneRow(oid);
      }
      return oid;
    }

    public OrderInstance getQueryBackingClass(ODSConnection con) {
      return new OrderInstance(0l);
    }

    public void delete(ODSConnection con, OrderInstance oid)
                    throws PersistenceLayerException {
      con.deleteOneRow(oid);
    }

    public void deleteAll(ODSConnection con) throws PersistenceLayerException {
      con.deleteAll(OrderInstanceDetails.class);
    }

    @Override
    public void delete(ODSConnection con, Collection<? extends OrderInstance> oids) throws PersistenceLayerException {
      con.delete(oids);
    }

    @Override
    public List<Class<? extends Storable<?>>> getAllClasses() {
      return Collections.<Class<? extends Storable<?>>>singletonList(OrderInstance.class);
    }
    
  },
  /*
   * default-cons use orderarchive as before
   * history cons store complete orderinstancedetails in orderarchive and baseFields in orderinfo
   *   orderInstance-queries are send to that dedicated table
   */
  dedicatedBaseFieldTable() {

    public boolean store(ODSConnection con, OrderInstanceDetails oid) throws PersistenceLayerException {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        return orderarchive.store(con, oid);
      } else {
        OrderInfoStorable saoi = new OrderInfoStorable(oid.getId());
        saoi.setAllFieldsFromData(oid);
        
        boolean ret = con.persistObject(saoi);
        if ((oid.getAuditDataAsXML() != null && oid.getAuditDataAsXML().length() > 0)
            || (oid.getExceptions() != null && oid.getExceptions().size() > 0)) {
          con.persistObject(oid);
        }
        return ret;
      }
    }

    public OrderInstanceDetails restore(ODSConnection con, long id, boolean forUpdate) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        return orderarchive.restore(con, id, forUpdate);
      } else {
        OrderInstanceDetails oid = new OrderInstanceDetails(id);
        if (forUpdate) {
          con.queryOneRowForUpdate(oid);
        } else {
          try {
            con.queryOneRow(oid);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            OrderInfoStorable ois = new OrderInfoStorable(oid.getId());
            con.queryOneRow(ois);
            oid.setAllFieldsFromData(ois);
          }
        }
        return oid;
      }
    }
    
    public OrderInstance getQueryBackingClass(ODSConnection con) {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        return orderarchive.getQueryBackingClass(con);
      } else {
        return new OrderInfoStorable(0l);
      }
    }

    @Override
    public void delete(ODSConnection con, OrderInstance oid) throws PersistenceLayerException {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        orderarchive.delete(con, oid);
      } else {
        con.deleteOneRow(oid);
        
        OrderInfoStorable saoi = new OrderInfoStorable(oid.getId());
        saoi.setAllFieldsFromData(oid);
        
        con.deleteOneRow(saoi);
      }
    }

    @Override
    public void deleteAll(ODSConnection con) throws PersistenceLayerException {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        orderarchive.deleteAll(con);
      } else {
        con.deleteAll(OrderInstanceDetails.class);
        con.deleteAll(OrderInfoStorable.class);
      }
    }

    @Override
    public void delete(ODSConnection con, Collection<? extends OrderInstance> oids) throws PersistenceLayerException {
      if (con.getConnectionType() == ODSConnectionType.DEFAULT) {
        orderarchive.delete(con, oids);
      } else {
        con.delete(oids);
        
        Collection<OrderInfoStorable> transformed = CollectionUtils.transform((Collection<OrderInstance>)oids, new Transformation<OrderInstance, OrderInfoStorable>() {

          @Override
          public OrderInfoStorable transform(OrderInstance from) {
            OrderInfoStorable saoi = new OrderInfoStorable(from.getId());
            saoi.setAllFieldsFromData(from);
            return saoi;
          }
          
        });
        
        con.delete(transformed);
      }
    }

    @Override
    public List<Class<? extends Storable<?>>> getAllClasses() {
      List<Class<? extends Storable<?>>> allClasses = new ArrayList<>();
      allClasses.add(OrderInstance.class);
      allClasses.add(OrderInfoStorable.class);
      return allClasses;
    }
  };
  
  
  public abstract boolean store(ODSConnection con, OrderInstanceDetails oid) throws PersistenceLayerException;
  
  public abstract OrderInstanceDetails restore(ODSConnection con, long id, boolean forUpdate) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
  
  public abstract void delete(ODSConnection con, OrderInstance oid) throws PersistenceLayerException;
  
  public abstract void delete(ODSConnection con, Collection<? extends OrderInstance> oids) throws PersistenceLayerException;
  
  public abstract void deleteAll(ODSConnection con) throws PersistenceLayerException;

  public abstract OrderInstance getQueryBackingClass(ODSConnection con);
  
  public PreparedQuery<OrderInstance> prepareQuery(ODSConnection con, OrderInstanceSelect select) throws PersistenceLayerException {
    select.setBackingClass(getQueryBackingClass(con));
    try {
      return OrderArchive.cache.getQueryFromCache(select.getSelectString(), con, select.getReader());
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("problem with select statement", e);
    }
  }

  public abstract List<Class<? extends Storable<?>>> getAllClasses();
  
}