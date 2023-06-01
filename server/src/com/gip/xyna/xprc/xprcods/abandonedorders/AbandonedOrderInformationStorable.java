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

package com.gip.xyna.xprc.xprcods.abandonedorders;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = AbandonedOrderInformationStorable.TABLE_NAME, primaryKey = AbandonedOrderInformationStorable.COL_ID)
public class AbandonedOrderInformationStorable extends Storable<AbandonedOrderInformationStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "abandonedorders";
  public static final String COL_ID = "id";
  public static final String COL_ORDER_ID = "orderid";
  public static final String COL_ROOT_ORDER_ID = "rootorderid";
  public static final String COL_CAUSING_RULE_NAME = "rulename";
  public static final String COL_DETAILS = "details";

  static final ResultSetReader<AbandonedOrderInformationStorable> reader = new AbandonedOrderInfoResultSetReader();


  @Column(name = COL_ID)
  private Long id;

  @Column(name = COL_ORDER_ID)
  private Long orderid;
  
  @Column(name = COL_ROOT_ORDER_ID)
  private Long rootorderid;
  
  @Column(name = COL_CAUSING_RULE_NAME)
  private String rulename;

  @Column(name = COL_DETAILS, type = ColumnType.BLOBBED_JAVAOBJECT)
  private AbandonedOrderDetails details;


  public AbandonedOrderInformationStorable() {
  }

  
  public AbandonedOrderInformationStorable(Long id) {
    this.id = id;
  }

  
  public AbandonedOrderInformationStorable(Long orderID, Long rootOrderID, String ruleName, AbandonedOrderDetails details) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.orderid = orderID;
    this.rootorderid = rootOrderID;
    this.rulename = ruleName;
    this.details = details;
  }


  @Override
  public ResultSetReader<? extends AbandonedOrderInformationStorable> getReader() {
    return reader;
  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends AbandonedOrderInformationStorable> void setAllFieldsFromData(U data) {
    AbandonedOrderInformationStorable cast = data;
    this.id = cast.id;
    this.orderid = cast.orderid;
    this.rootorderid = cast.rootorderid;
    this.rulename = cast.rulename;
    this.details = cast.details;
  }


  private static class AbandonedOrderInfoResultSetReader implements ResultSetReader<AbandonedOrderInformationStorable> {

    public AbandonedOrderInformationStorable read(ResultSet rs) throws SQLException {
      AbandonedOrderInformationStorable result = new AbandonedOrderInformationStorable();
      result.id = rs.getLong(COL_ID);
      result.orderid = rs.getLong(COL_ORDER_ID);
      result.rootorderid = rs.getLong(COL_ROOT_ORDER_ID);
      result.rulename = rs.getString(COL_CAUSING_RULE_NAME);
      result.details = (AbandonedOrderDetails) result.readBlobbedJavaObjectFromResultSet(rs, COL_DETAILS);
      return result;
    }

  }


  public Long getId() {
    return this.id;
  }
  
  
  public Long getOrderId() {
    return this.orderid;
  }
  
  
  public Long getRootOrderId() {
    return this.rootorderid;
  }


  public String getRulename() {
    return this.rulename;
  }


  public AbandonedOrderDetails getDetails() {
    return this.details;
  }

}
