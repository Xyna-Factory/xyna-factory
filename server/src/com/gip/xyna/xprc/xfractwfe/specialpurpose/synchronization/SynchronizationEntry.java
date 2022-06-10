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

package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;



@Persistable(tableName = SynchronizationEntry.TABLE_NAME, primaryKey = SynchronizationEntry.COL_CORRELATION_ID)
public class SynchronizationEntry extends Storable<SynchronizationEntry> {

  private static final long serialVersionUID = -218687880825559636L;
  private static final SynchronizationEntryReader reader = new SynchronizationEntryReader();


  public static final String TABLE_NAME = "synchronizationentries";

  public static final String COL_CORRELATION_ID = "correlationid";

  public static final String COL_ROOT_ID_2_RESUME = "roottoresume";
  public static final String COL_ORDER_ID_2_RESUME = "ordertoresume";
  public static final String COL_LANE_ID_2_RESUME = "lanetoresume";

  public static final String COL_TIMEOUT = "timeout";
  public static final String COL_TIMESTAMP = "timestamp";
  public static final String COL_ANSWER = "answer";
  public static final String COL_NOTIFIED = "notified";
  public static final String COL_READY_TO_RESUME = "readytoresume";

  public static final String COL_ORDER_RESUMED = "orderresumed";

  public static final String COL_INTERNAL_XYNA_STEP_ID = "internalxynastepid";
  public static final String COL_CORRESPONDING_RESUME_ORDER_ID = "correspondingresumeorder";

  public static final String COL_RECEIVED_TIMEOUT = "receivedtimeout";
  
  @Column(name = COL_CORRELATION_ID)
  private String correlationId;

  @Column(name = COL_ROOT_ID_2_RESUME)
  private Long rootId;
  @Column(name = COL_ORDER_ID_2_RESUME)
  private Long orderId;
  @Column(name = COL_LANE_ID_2_RESUME)
  private String laneId;

  // This is measured in seconds but is stored as long since it is occasionally multiplied by 1000, avoid overflow
  // especially when it is not possible to take care for that (like in the delete SQL statement)
  @Column(name = COL_TIMEOUT)
  private Long timeout;

  @Column(name = COL_TIMESTAMP)
  private Long timeStamp;
  @Column(name = COL_ANSWER, size = Integer.MAX_VALUE)
  private String answer;
  @Column(name = COL_NOTIFIED)
  private Boolean notified = false;
  @Column(name = COL_READY_TO_RESUME)
  private Boolean readyToResume = false;

  @Column(name = COL_INTERNAL_XYNA_STEP_ID)
  private Integer internalXynaStepId;

  @Column(name = COL_CORRESPONDING_RESUME_ORDER_ID)
  private Long correspondingResumeOrderId;

  @Column(name = COL_ORDER_RESUMED)
  private Boolean orderResumed = false;

  @Column(name = COL_RECEIVED_TIMEOUT)
  private Boolean receivedTimeout = false;


  // for warehouse stuff
  public SynchronizationEntry() {
  }


  public SynchronizationEntry(String correlationId) {
    this.correlationId = correlationId;
  }


  public SynchronizationEntry(String correlationId, String answer, int time) {
    this(correlationId, answer, time, false);
  }


  public SynchronizationEntry(String correlationId, String answer, int time, boolean notified) {
    this.correlationId = correlationId;
    this.timeout = (long) time;
    this.answer = answer;
    this.timeStamp = System.currentTimeMillis();
  }


  public String getCorrelationId() {
    return correlationId;
  }


  public String getAnswer() {
    return answer;
  }


  public boolean gotNotified() {
    return notified;
  }


  public boolean isTimedOut(long now) {
    return now >= (timeStamp + (timeout * 1000));
  }


  public boolean isTimedOut() {
    return isTimedOut(System.currentTimeMillis());
  }


  @Override
  public Object getPrimaryKey() {
    return correlationId;
  }


  @Override
  public ResultSetReader<? extends SynchronizationEntry> getReader() {
    return reader;
  }


  @Override
  public <U extends SynchronizationEntry> void setAllFieldsFromData(U data) {
    SynchronizationEntry cast = data;
    this.correlationId = cast.correlationId;
    this.rootId = cast.rootId;
    this.orderId = cast.orderId;
    this.laneId = cast.laneId;
    this.answer = cast.answer;
    this.timeout = cast.timeout;
    this.timeStamp = cast.timeStamp;
    this.notified = cast.notified;
    this.readyToResume = cast.readyToResume;
    this.orderResumed = cast.orderResumed;
    this.internalXynaStepId = cast.internalXynaStepId;
    this.correspondingResumeOrderId = cast.correspondingResumeOrderId;
    this.receivedTimeout = cast.receivedTimeout;
  }


  private static void fillByResultSet(SynchronizationEntry entry, ResultSet rs) throws SQLException {

    entry.correlationId = rs.getString(COL_CORRELATION_ID);

    entry.rootId = rs.getLong(COL_ROOT_ID_2_RESUME);
    if (entry.rootId == 0 && rs.wasNull()) {
      entry.rootId = null;
    }
    entry.orderId = rs.getLong(COL_ORDER_ID_2_RESUME);
    if (entry.orderId == 0 && rs.wasNull()) {
      entry.orderId = null;
    }
    entry.laneId = rs.getString(COL_LANE_ID_2_RESUME);

    entry.timeout = rs.getLong(COL_TIMEOUT);
    if (entry.timeout == 0 && rs.wasNull()) {
      entry.timeout = null;
    }

    entry.timeStamp = rs.getLong(COL_TIMESTAMP);
    if (entry.timeStamp == 0 && rs.wasNull()) {
      entry.timeStamp = null;
    }

    entry.answer = rs.getString(COL_ANSWER);

    entry.notified = rs.getBoolean(COL_NOTIFIED);
    entry.readyToResume = rs.getBoolean(COL_READY_TO_RESUME);
    entry.orderResumed = rs.getBoolean(COL_ORDER_RESUMED);

    entry.internalXynaStepId = rs.getInt(COL_INTERNAL_XYNA_STEP_ID);
    entry.correspondingResumeOrderId = rs.getLong(COL_CORRESPONDING_RESUME_ORDER_ID);

    entry.receivedTimeout = rs.getBoolean(COL_RECEIVED_TIMEOUT);

  }


  private static class SynchronizationEntryReader implements ResultSetReader<SynchronizationEntry> {
    public SynchronizationEntry read(ResultSet rs) throws SQLException {
      SynchronizationEntry entry = new SynchronizationEntry();
      fillByResultSet(entry, rs);
      return entry;
    }
  }


  public void setNotified() {
    this.notified = true;
  }

  
  public boolean getNotified() {
    return notified;
  }
  

  public boolean isReadyToResume() {
    return this.readyToResume;
  }


  public void setReadyForNotify() {
    this.readyToResume = true;
  }


  public boolean isOrderResumed() {
    return this.orderResumed;
  }


  public void setOrderResumed(boolean b) {
    this.orderResumed = b;
  }


  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }


  public Long getOrderId() {
    return orderId;
  }
  
  
  public Long getOrdertoresume() {
    return orderId;
  }


  public void setLaneId(String laneId) {
    this.laneId = laneId;
  }


  public String getLaneId() {
    return laneId;
  }
  
  public String getLanetoresume() {
    return laneId;
  }
  
  public void setInternalXynaStepId(Integer internalXynaStepId) {
    this.internalXynaStepId = internalXynaStepId;
  }


  public Integer getInternalXynaStepId() {
    return internalXynaStepId;
  }


  public void setCorrespondingResumeOrderId(Long correspondingResumeOrderId) {
    this.correspondingResumeOrderId = correspondingResumeOrderId;
  }


  public Long getCorrespondingResumeOrderId() {
    return correspondingResumeOrderId;
  }

  
  public Long getCorrespondingresumeorder() {
    return correspondingResumeOrderId;
  }

  /**
   * @return the creation time in milliseconds
   */
  public Long getTimeStamp() {
    return timeStamp;
  }


  /**
   * @return the relative timeout in milliseconds
   */
  public Long getTimeout() {
    return timeout * 1000L;
  }

  public void setTimeoutInSeconds(long timeout) {
    this.timeout = timeout;
  }

  public void setTimestamp(Long firstExecutionTime) {
    this.timeStamp = firstExecutionTime;
  }


  public void setAnswer(String answer) {
    this.answer = answer;
  }


  public boolean receivedTimeout() {
    return this.receivedTimeout;
  }
  
  public boolean getReceivedTimeout() {
    return receivedTimeout;
  }


  public void setReceivedTimeout(boolean timedout) {
    this.receivedTimeout = timedout;
  }

  
  public Long getRootId() {
    return rootId;
  }
  
  
  public Long getRoottoresume() {
    return rootId;
  }
  
  
  public void setRootId(long rootId) {
    this.rootId = rootId;
  }


  public ResumeTarget getResumeTarget() {
    return new ResumeTarget(rootId, orderId, laneId);
  }

  
}
