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
package com.gip.xyna.xprc.xsched.orderseries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;


/**
 *
 */
@Persistable(tableName = SeriesInformationStorable.TABLE_NAME, primaryKey = SeriesInformationStorable.COL_ID)
public class SeriesInformationStorable extends ClusteredStorable<SeriesInformationStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "seriesinformation";

  public static final int SIZE_ID_LIST = 100000; //viel zu groß, soll Speicherung als Clob bewirken
  
  public static final String COL_ID = "id";
  public static final String COL_CORRELATION_ID = "correlationId";
  public static final String COL_ORDER_STATUS = "orderStatus";
  public static final String COL_AUTO_CANCEL = "autoCancel";
  public static final String COL_INHERITED_CANCEL = "inheritedCancel";
  public static final String COL_IGNORE_INHERITED_CANCEL = "ignoreInheritedCancel";
  public static final String COL_PREDECESSOR_CORR_IDS = "predecessorCorrIds";
  public static final String COL_SUCCESSOR_CORR_IDS = "successorCorrIds";
  public static final String COL_PREDECESSOR_ORDER_IDS = "predecessorOrderIds";
  public static final String COL_SUCCESSOR_ORDER_IDS = "successorOrderIds";
  
  public static ResultSetReader<? extends SeriesInformationStorable> reader = new SeriesInformationStorableReader();
  
  public enum OrderStatus implements StringSerializable<OrderStatus> {
    WAITING(false,false),
    RUNNING(false,false),
    CANCELING(false,false), //kein Fehler, da Successoren noch nicht gecancelt werden dürfen
    SUCCEEDED(true,false),
    FAILED(true,true),
    CANCELED(true,true);

    private boolean finished;
    private boolean error;

    private OrderStatus(boolean finished, boolean error) {
      this.finished = finished;
      this.error = error;
    }

    /**
     * Hat Auftrag einen Fehler?
     * @return
     */
    public boolean isError() {
      return error;
    }

    /**
     * Ist Auftrag beendet?
     * @return
     */
    public boolean isFinished() {
      return finished;
    }

    public OrderStatus deserializeFromString(String string) {
      return OrderStatus.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }
  
  
  @Column(name = COL_ID, index = IndexType.PRIMARY)
  private long id;

  @Column(name = COL_CORRELATION_ID, index = IndexType.UNIQUE)
  private String correlationId;

  @Column(name = COL_ORDER_STATUS)
  private OrderStatus orderStatus;
  
  @Column(name = COL_AUTO_CANCEL)
  private boolean autoCancel;
  
  @Column(name = COL_INHERITED_CANCEL)
  private boolean inheritedCancel;
  
  @Column(name = COL_IGNORE_INHERITED_CANCEL)
  private boolean ignoreInheritedCancel;
 
  @Column(name = COL_PREDECESSOR_CORR_IDS, size = SIZE_ID_LIST)
  private final StringSerializableList<String> predecessorCorrIds = StringSerializableList.separator(String.class);
 
  @Column(name = COL_SUCCESSOR_CORR_IDS, size = SIZE_ID_LIST)
  private final StringSerializableList<String> successorCorrIds = StringSerializableList.separator(String.class);

  @Column(name = COL_PREDECESSOR_ORDER_IDS, size = SIZE_ID_LIST)
  private final StringSerializableList<Long> predecessorOrderIds = StringSerializableList.separator(Long.class);

  @Column(name = COL_SUCCESSOR_ORDER_IDS, size = SIZE_ID_LIST)
  private final StringSerializableList<Long> successorOrderIds = StringSerializableList.separator(Long.class);
 
  
  public SeriesInformationStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }
  
  /**
   * Angabe des PrimaryKey
   * @param id
   */
  public SeriesInformationStorable(long id) {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
    this.id = id;
  }


  
  /**
   * @param binding
   */
  public SeriesInformationStorable(int binding, String correlationId ) {
    super(binding);
    this.correlationId = correlationId;
    this.orderStatus = OrderStatus.WAITING;
  }

  
  /**
   * Copy-Konstruktor
   * @param sis
   */
  public SeriesInformationStorable(SeriesInformationStorable sis) {
    super(sis.getBinding());
    setAllFieldsFromData(sis);
  }


  @Override
  public ResultSetReader<? extends SeriesInformationStorable> getReader() {
    return reader;
  }

  @Override
  public Long getPrimaryKey() {
    return Long.valueOf(id);
  }

  @Override
  public <U extends SeriesInformationStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    SeriesInformationStorable cast = data;
    this.id = cast.id;
    this.correlationId = cast.correlationId;
    this.orderStatus = cast.orderStatus;
    this.autoCancel = cast.autoCancel;
    this.inheritedCancel = cast.inheritedCancel;
    this.ignoreInheritedCancel = cast.ignoreInheritedCancel;
    setPredecessorCorrIds(data.getPredecessorCorrIds());
    setSuccessorCorrIds(data.getSuccessorCorrIds());
    setPredecessorOrderIds(data.getPredecessorOrderIds());
    setSuccessorOrderIds(data.getSuccessorOrderIds());
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(100);
    sb.append("SeriesInformationStorable(").append(getId()).append(",").append(getCorrelationId());
    sb.append(",pre(").append(getPredecessorCorrIds()).append(",").append(getPredecessorOrderIds()).append(")");
    sb.append(",suc(").append(getSuccessorCorrIds()).append(",").append(getSuccessorOrderIds()).append(")");
    sb.append(",orderStatus=").append(getOrderStatus());
    sb.append(",autoCancel=").append(isAutoCancel()).append(",inheritedCancel=").append(isInheritedCancel());
    sb.append(")");
    return sb.toString();
  }
  
  private static class SeriesInformationStorableReader implements ResultSetReader<SeriesInformationStorable> {
    public SeriesInformationStorable read(ResultSet rs) throws SQLException {
      SeriesInformationStorable result = new SeriesInformationStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(SeriesInformationStorable sis, ResultSet rs) throws SQLException {
    ClusteredStorable.fillByResultSet(sis, rs);
    sis.id = rs.getLong(COL_ID);
    sis.correlationId = rs.getString(COL_CORRELATION_ID);
    sis.orderStatus = OrderStatus.valueOf(rs.getString(COL_ORDER_STATUS));
    sis.autoCancel = rs.getBoolean(COL_AUTO_CANCEL);
    sis.inheritedCancel = rs.getBoolean(COL_INHERITED_CANCEL);
    sis.ignoreInheritedCancel = rs.getBoolean(COL_IGNORE_INHERITED_CANCEL);
    
    sis.predecessorCorrIds.deserializeFromString( rs.getString(COL_PREDECESSOR_CORR_IDS) );
    sis.successorCorrIds.deserializeFromString( rs.getString(COL_SUCCESSOR_CORR_IDS) );
    sis.predecessorOrderIds.deserializeFromString( rs.getString(COL_PREDECESSOR_ORDER_IDS) );
    sis.successorOrderIds.deserializeFromString( rs.getString(COL_SUCCESSOR_ORDER_IDS) );
  }
  

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public boolean isHadErrorX() {
    return orderStatus.isError();
  }

  public boolean isFinishedX() {
    return orderStatus.isFinished();
  }

  public boolean isAutoCancel() {
    return autoCancel;
  }

  public void setAutoCancel(boolean autoCancel) {
    this.autoCancel = autoCancel;
  }

  public boolean isInheritedCancel() {
    return inheritedCancel;
  }

  public void setInheritedCancel(boolean inheritedCancel) {
    this.inheritedCancel = inheritedCancel;
  }

  public List<String> getPredecessorCorrIds() {
    return predecessorCorrIds;
  }

  public void setPredecessorCorrIds(List<String> predecessorCorrIds) {
    this.predecessorCorrIds.setValues(predecessorCorrIds);
  }

  public List<String> getSuccessorCorrIds() {
    return successorCorrIds;
  }

  public void setSuccessorCorrIds(List<String> successorCorrIds) {
    this.successorCorrIds.setValues(successorCorrIds);
  }

  public List<Long> getPredecessorOrderIds() {
    return predecessorOrderIds;
  }

  public void setPredecessorOrderIds(List<Long> predecessorOrderIds) {
    this.predecessorOrderIds.setValues(predecessorOrderIds);
  }

  public List<Long> getSuccessorOrderIds() {
    return successorOrderIds;
  }

  public void setSuccessorOrderIds(List<Long> successorOrderIds) {
    this.successorOrderIds.setValues(successorOrderIds);
  }

  public boolean isIgnoreInheritedCancel() {
    return ignoreInheritedCancel;
  }
  
  public void setIgnoreInheritedCancel(boolean ignoreInheritedCancel) {
    this.ignoreInheritedCancel = ignoreInheritedCancel;
  }

  public OrderStatus getOrderStatus() {
    return orderStatus;
  }
  
  public void setOrderStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }
  
}
