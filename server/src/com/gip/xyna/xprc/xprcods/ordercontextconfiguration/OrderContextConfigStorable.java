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

package com.gip.xyna.xprc.xprcods.ordercontextconfiguration;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = OrderContextConfigStorable.COLUMN_ID, tableName = OrderContextConfigStorable.TABLE_NAME)
public class OrderContextConfigStorable extends Storable<OrderContextConfigStorable> {

  private static final long serialVersionUID = 1L;

  public static final OrderContextConfigResultSetReader reader = new OrderContextConfigResultSetReader();

  public static final String TABLE_NAME = "ordercontextconfig";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_ORDER_TYPE = "ordertype";
  public static final String COLUMN_REVISION = "revision";

  @Column(name = COLUMN_ORDER_TYPE, size=200)
  private String orderType;
  @Column(name = COLUMN_ID)
  private long id;
  @Column(name = COLUMN_REVISION)
  private Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;

  public OrderContextConfigStorable() {
  }


  public OrderContextConfigStorable(String orderType) {
    this.orderType = orderType;
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
  }
  
  
  public OrderContextConfigStorable(String orderType, Long revision) {
    this(orderType);
    if (revision == null) {
      throw new IllegalArgumentException("Revision may not be null");
    }
    this.revision = revision;
  }

  
  // only used from storable conversion update
  public OrderContextConfigStorable(String orderType, Long revision, long id) {
    if(XynaFactory.getInstance().finishedInitialization()) {
      throw new RuntimeException("Unallowed call of constructor.");
    }
    this.id = id;
    if (revision == null) {
      throw new IllegalArgumentException("Revision may not be null");
    }
    this.revision = revision;
    this.orderType = orderType;
  }


  public String getOrderType() {
    return orderType;
  }


  public Long getRevision() {
    return revision;
  }
  
  
  public long getId() {
    return id;
  }
  
  
  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public ResultSetReader<? extends OrderContextConfigStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends OrderContextConfigStorable> void setAllFieldsFromData(U data) {
    OrderContextConfigStorable cast = data;
    orderType = cast.orderType;
    revision = cast.revision;
    id = cast.id;
  }


  private static void setAllFieldsFromResultSet(OrderContextConfigStorable target, ResultSet rs) throws SQLException {
    target.id = rs.getLong(COLUMN_ID);
    target.orderType = rs.getString(COLUMN_ORDER_TYPE);
    target.revision = rs.getLong(COLUMN_REVISION);
  }


  private static class OrderContextConfigResultSetReader implements ResultSetReader<OrderContextConfigStorable> {

    public OrderContextConfigStorable read(ResultSet rs) throws SQLException {
      OrderContextConfigStorable result = new OrderContextConfigStorable();
      setAllFieldsFromResultSet(result, rs);
      return result;
    }

  }

}
