/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



@Persistable(primaryKey = OrderArchiveStatisticsStorable.COL_ID, tableName = OrderArchiveStatisticsStorable.TABLE_NAME)
public class OrderArchiveStatisticsStorable extends ClusteredStorable<OrderArchiveStatisticsStorable> {

  private static final long serialVersionUID = -2404439121946341673L;

  public static final String TABLE_NAME = "orderstatistics";
  public static final String COL_ID = "id";
  public static final String COL_ORDER_TYPE = "ordertype";
  public static final String COL_APPLICATION_NAME = "applicationname";
  public static final String COL_CALLS = "calls";
  public static final String COL_FINISHED = "finished";
  public static final String COL_ERRORS = "errors";
  public static final String COL_TIMEOUTS = "timeouts";

  @Column(name = COL_ID)
  private Long id;

  @Column(name = COL_ORDER_TYPE)
  private String ordertype;
  
  @Column(name = COL_APPLICATION_NAME)
  private String applicationname;

  @Column(name = COL_CALLS)
  private Long calls = 0L;

  @Column(name = COL_FINISHED)
  private Long finished = 0L;

  @Column(name = COL_ERRORS)
  private Long errors = 0L;

  @Column(name = COL_TIMEOUTS)
  private Long timeouts = 0L;


  private static class OrderArchiveStatisticsResultSetReader implements ResultSetReader<OrderArchiveStatisticsStorable> {

    public OrderArchiveStatisticsStorable read(ResultSet rs) throws SQLException {
      OrderArchiveStatisticsStorable result = new OrderArchiveStatisticsStorable(-1L, -1);
      ClusteredStorable.fillByResultSet(result, rs);
      result.id = rs.getLong(COL_ID);
      result.ordertype = rs.getString(COL_ORDER_TYPE);
      result.calls = rs.getLong(COL_CALLS);
      result.finished = rs.getLong(COL_FINISHED);
      result.errors = rs.getLong(COL_ERRORS);
      result.timeouts = rs.getLong(COL_TIMEOUTS);
      result.applicationname = rs.getString(COL_APPLICATION_NAME);
      return result;
    }

  }


  public static ResultSetReader<OrderArchiveStatisticsStorable> reader = new OrderArchiveStatisticsResultSetReader();


  public OrderArchiveStatisticsStorable(Long id, int binding) {
    super(binding);
    this.id = id;
  }


  public OrderArchiveStatisticsStorable(int binding) {
    super(binding);
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
  }


  public OrderArchiveStatisticsStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  @Override
  public ResultSetReader<? extends OrderArchiveStatisticsStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public String getOrderType() {
    return ordertype;
  }

  
  public String getApplicationname() {
    return applicationname;
  }


  public Long getId() {
    return id;
  }


  public Long getCalls() {
    return calls;
  }


  public Long getFinished() {
    return finished;
  }


  public Long getErrors() {
    return errors;
  }


  public Long getTimeouts() {
    return timeouts;
  }

  
  public void setId(long id) {
    this.id = id;
  }
  

  public void setOrderType(String orderType) {
    this.ordertype = orderType;
  }
  
  
  public void setApplicationname(String applicationname) {
    this.applicationname = applicationname;
  }


  public void setCalls(Long calls) {
    this.calls = calls;
  }


  public void setFinished(Long finished) {
    this.finished = finished;
  }


  public void setErrors(Long errors) {
    this.errors = errors;
  }


  public void setTimeouts(Long timeouts) {
    this.timeouts = timeouts;
  }


  @Override
  public <U extends OrderArchiveStatisticsStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    OrderArchiveStatisticsStorable cast = data;
    this.id = cast.id;
    this.ordertype = cast.ordertype;
    this.calls = cast.calls;
    this.finished = cast.finished;
    this.errors = cast.errors;
    this.timeouts = cast.timeouts;
    this.applicationname = cast.applicationname;
  }

}
