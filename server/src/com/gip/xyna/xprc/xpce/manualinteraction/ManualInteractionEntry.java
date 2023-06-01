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

package com.gip.xyna.xprc.xpce.manualinteraction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



@Persistable(primaryKey = ManualInteractionEntry.MI_COL_ID, tableName = ManualInteractionEntry.TABLE_NAME)
public class ManualInteractionEntry extends ClusteredStorable<ManualInteractionEntry> {

  private static final long serialVersionUID = 5834454969726774781L;

  public static final String MI_COL_ID = "ID";

  public static final String TABLE_NAME = "miarchive";
  
  public static final String MI_COL_XYNAORDER_ID = "xynaOrderId";
  public static final String MI_COL_XYNAORDER_MON_LVL = "xynaOrderMonitoringLevel";
  public static final String MI_COL_XYNAORDER_SESSION_ID = "xynaOrderSessionId";
  public static final String MI_COL_XYNAORDER_PRIORITY = "xynaOrderPriority";
  public static final String MI_COL_XYNAORDER_PARENT_ID = "parentOrderId";
  public static final String MI_COL_XYNAORDER_PARENT_ORDERTYPE = "parentOrderType";
  public static final String MI_COL_XYNAORDER_ROOT_ID = "rootOrderId";
  public static final String MI_COL_XYNAORDER_REVISION = "revision";

  public static final String MI_COL_INPUT = "input";
  public static final String MI_COL_RESULT = "result";
  public static final String MI_COL_REASON = "reason";
  public static final String MI_COL_WFTRACE = "wfTrace";
  public static final String MI_COL_TYPE = "type";
  public static final String MI_COL_USERGROUP = "userGroup";
  public static final String MI_COL_TODO = "todo";
  public static final String MI_COL_OLDINSTSTATUS = "oldInstanceStatus";
  public static final String MI_COL_ORDERMONITORED = "correlatedOrderIsMonitored";
  public static final String MI_COL_PAUSE = "isPaused";
  public static final String MI_COL_ALLOWED_RESPONSES = "allowedResponses";
  

  @Column(name = MI_COL_RESULT, type = ColumnType.BLOBBED_JAVAOBJECT)
  private GeneralXynaObject result;

  @Column(name = MI_COL_REASON, size = 256)
  private String reason;
  @Column(name = MI_COL_ID)
  private Long ID;

  // FIXME remove this column, the information is potentially redundant with common monitoring information
  //       by the way: this would be more of a order trace anyway
  //       keep in mind backward compatibility when removing
  @Column(name = MI_COL_WFTRACE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private WorkflowStacktrace wfTrace;

  @Column(name = MI_COL_TYPE)
  private String type;
  @Column(name = MI_COL_USERGROUP)
  private String userGroup;
  @Column(name = MI_COL_TODO, size = 1024)
  private String todo;

  @Column(name = MI_COL_OLDINSTSTATUS)
  private String oldInstanceStatus;
  @Column(name = MI_COL_ORDERMONITORED)
  private boolean correlatedOrderIsMonitored;
  @Column(name = MI_COL_PAUSE)
  private boolean isPaused = false;
  
  @Column(name = MI_COL_ALLOWED_RESPONSES)
  private String allowedResponses;


  @Column(name = MI_COL_XYNAORDER_ID)
  private Long xynaOrderId;

  @Column(name = MI_COL_XYNAORDER_PRIORITY)
  private int xynaOrderPriority;

  @Column(name = MI_COL_XYNAORDER_MON_LVL)
  private int xynaOrderMonitoringLevel;

  @Column(name = MI_COL_XYNAORDER_SESSION_ID)
  private String xynaOrderSessionId;

  @Column(name = MI_COL_XYNAORDER_PARENT_ID)
  private Long parentOrderId;

  @Column(name = MI_COL_XYNAORDER_PARENT_ORDERTYPE)
  private String parentOrderType;

  @Column(name = MI_COL_XYNAORDER_ROOT_ID)
  private Long rootOrderId;
  
  @Column(name = MI_COL_XYNAORDER_REVISION)
  private Long revision;
  

  public ManualInteractionEntry() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }
  
  public ManualInteractionEntry(int binding) {
    super(binding);
  }
  
  public ManualInteractionEntry(long id, int binding) {
    super(binding);
    this.ID = id;
  }


  public void setReason(String reason) {
    this.reason = reason;
  }


  public String getReason() {
    return reason;
  }


  public void setID(Long iD) {
    ID = iD;
  }


  public Long getID() {
    return ID;
  }


  public void setWfTrace(WorkflowStacktrace wfTrace) {
    this.wfTrace = wfTrace;
  }


  public WorkflowStacktrace getWfTrace() {
    return wfTrace;
  }


  public void setResult(GeneralXynaObject result) {
    this.result = result;
  }


  public GeneralXynaObject getResult() {
    return result;
  }


  public void setType(String type) {
    this.type = type;
  }


  public String getType() {
    return type;
  }


  public void setUserGroup(String userGroup) {
    this.userGroup = userGroup;
  }


  public String getUserGroup() {
    return userGroup;
  }


  public void setTodo(String todo) {
    this.todo = todo;
  }


  public String getTodo() {
    return todo;
  }


  public void setPaused(boolean isPaused) {
    this.isPaused = isPaused;
  }


  public boolean isPaused() {
    return isPaused;
  }
  
  
  public boolean getIsPaused() {
    return isPaused;
  }
  
  
  public void setAllowedResponses(Collection<ManualInteractionResponse> allowedResponsesCollection) {
    StringBuilder responsesBuilder = new StringBuilder();
    int i = 0;
    for(ManualInteractionResponse response : allowedResponsesCollection) {
      responsesBuilder.append(response.getXmlName());
      if (i + 1 < allowedResponsesCollection.size()) {
        responsesBuilder.append(",");
      }
      i++;
    }
    this.allowedResponses = responsesBuilder.toString();
  }
  
  public void setAllowedResponses(String data) {
    allowedResponses = data;
  }
  
  public String getAllowedResponses() {
    return allowedResponses;
  }
  
  
  public void setOldInstanceStatus(String oldInstanceStatus) {
    this.oldInstanceStatus = oldInstanceStatus;
  }


  public String getOldInstanceStatus() {
    return oldInstanceStatus;
  }
  public OrderInstanceStatus getOldInstanceStatusAsEnum() {
    return OrderInstanceStatus.fromString(oldInstanceStatus);
  }


  public void setCorrelatedOrderIsMonitored(boolean correlatedOrderIsMonitored) {
    this.correlatedOrderIsMonitored = correlatedOrderIsMonitored;
  }


  public boolean isCorrelatedOrderIsMonitored() {
    return correlatedOrderIsMonitored;
  }


  public void setXynaOrder(XynaOrderServerExtension xynaOrder) {
    setXynaOrderId(xynaOrder.getId());
    setXynaOrderMonitoringLevel(xynaOrder.getMonitoringCode());
    setXynaOrderPriority(xynaOrder.getPriority());
    setXynaOrderSessionId(xynaOrder.getSessionId());
    if (xynaOrder.hasParentOrder()) {
      setParentOrderId(xynaOrder.getParentOrder().getId());
      setParentOrderType(xynaOrder.getParentOrder().getDestinationKey().getOrderType());
      setRevision(xynaOrder.getParentOrder().getRevision());
    } else {
      setRevision(xynaOrder.getRevision());
    }
    setRootOrderId(xynaOrder.getRootOrder().getId());
  }


  @Override
  public Object getPrimaryKey() {
    return getID();
  }


  public static class DynamicManualInteractionReader implements ResultSetReader<ManualInteractionEntry> {

    private List<ManualInteractionColumn> selectedCols;

    public DynamicManualInteractionReader(List<ManualInteractionColumn> selected) {
      selectedCols = selected;
    }

    public ManualInteractionEntry read(ResultSet rs) throws SQLException {
      ManualInteractionEntry mie = new ManualInteractionEntry();
      if (selectedCols.contains(ManualInteractionColumn.ID)) {
        mie.ID = rs.getLong(ManualInteractionColumn.ID.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.todo)) {
        mie.todo = rs.getString(ManualInteractionColumn.todo.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.type)) {
        mie.type = rs.getString(ManualInteractionColumn.type.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.reason)) {
        mie.reason = rs.getString(ManualInteractionColumn.reason.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.userGroup)) {
        mie.userGroup = rs.getString(ManualInteractionColumn.userGroup.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.result)) {
        mie.result =
            (GeneralXynaObject) mie.readBlobbedJavaObjectFromResultSet(rs, ManualInteractionColumn.result.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.xynaorder)) {
        mie.xynaOrderId = rs.getLong(MI_COL_XYNAORDER_ID);
        mie.xynaOrderPriority = rs.getInt(MI_COL_XYNAORDER_PRIORITY);
        mie.xynaOrderMonitoringLevel = rs.getInt(MI_COL_XYNAORDER_MON_LVL);
        mie.xynaOrderSessionId = rs.getString(MI_COL_XYNAORDER_SESSION_ID);
        mie.parentOrderId = rs.getLong(MI_COL_XYNAORDER_PARENT_ID);
        if (rs.wasNull()) {
          mie.parentOrderId = null;
        }
        mie.parentOrderType = rs.getString(MI_COL_XYNAORDER_PARENT_ORDERTYPE);
        if (rs.wasNull()) {
          mie.parentOrderType = null;
        }
      }
      if (selectedCols.contains(ManualInteractionColumn.allowedResponses)) {
        mie.allowedResponses = rs.getString(ManualInteractionColumn.allowedResponses.toString());
      }
      if (selectedCols.contains(ManualInteractionColumn.revision)) {
        mie.revision = rs.getLong(MI_COL_XYNAORDER_REVISION);
        if (rs.wasNull()) {
          mie.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
        }
      }
      return mie;
    }
  }

  static class ManualInteractionReader implements ResultSetReader<ManualInteractionEntry> {

    public ManualInteractionEntry read(ResultSet rs) throws SQLException {
      ManualInteractionEntry mie = new ManualInteractionEntry();
      fillByResultSet(mie, rs);
      return mie;
    }
  }


  public static ManualInteractionReader reader = new ManualInteractionReader();


  @Override
  public ResultSetReader<? extends ManualInteractionEntry> getReader() {
    return reader;
  }


  @Override
  public <U extends ManualInteractionEntry> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    ID = data.getID();
    this.xynaOrderId = data.getXynaOrderId();
    this.xynaOrderMonitoringLevel = data.getXynaOrderMonitoringLevel();
    this.xynaOrderPriority = data.getXynaOrderPriority();
    this.xynaOrderSessionId = data.getXynaOrderSessionId();
    this.parentOrderId = data.getParentOrderId();
    this.parentOrderType = data.getParentOrderType();
    this.rootOrderId = data.getRootOrderId();

    result = data.getResult();
    reason = data.getReason();
    wfTrace = data.getWfTrace();

    type = data.getType();
    userGroup = data.getUserGroup();
    todo = data.getTodo();

    oldInstanceStatus = data.getOldInstanceStatus();
    correlatedOrderIsMonitored = data.isCorrelatedOrderIsMonitored();
    isPaused = data.isPaused();
    allowedResponses = data.getAllowedResponses();
    revision = data.getRevision();
  }


  private static final void fillByResultSet(ManualInteractionEntry newEntry, ResultSet rs) throws SQLException {

    ClusteredStorable.fillByResultSet(newEntry, rs);
    newEntry.setID(rs.getLong(MI_COL_ID));

    newEntry.xynaOrderId = rs.getLong(MI_COL_XYNAORDER_ID);
    newEntry.xynaOrderPriority = rs.getInt(MI_COL_XYNAORDER_PRIORITY);
    newEntry.xynaOrderMonitoringLevel = rs.getInt(MI_COL_XYNAORDER_MON_LVL);
    newEntry.xynaOrderSessionId = rs.getString(MI_COL_XYNAORDER_SESSION_ID);
    newEntry.parentOrderId = rs.getLong(MI_COL_XYNAORDER_PARENT_ID);
    if (rs.wasNull()) {
      newEntry.parentOrderId = null;
    }
    newEntry.parentOrderType = rs.getString(MI_COL_XYNAORDER_PARENT_ORDERTYPE);
    if (rs.wasNull()) {
      newEntry.parentOrderType = null;
    }
    
    newEntry.rootOrderId = rs.getLong(MI_COL_XYNAORDER_ROOT_ID);
    if (rs.wasNull()) {
      newEntry.rootOrderId = null;
    }
 
    newEntry.revision = rs.getLong(MI_COL_XYNAORDER_REVISION);
    if(newEntry.revision == 0 && rs.wasNull()) {
      newEntry.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }    
    
    newEntry.setResult((GeneralXynaObject) newEntry.readBlobbedJavaObjectFromResultSet(rs, MI_COL_RESULT));
    newEntry.setReason(rs.getString(MI_COL_REASON));
    newEntry.setWfTrace((WorkflowStacktrace) newEntry.readBlobbedJavaObjectFromResultSet(rs, MI_COL_WFTRACE));

    newEntry.setType(rs.getString(MI_COL_TYPE));
    newEntry.setUserGroup(rs.getString(MI_COL_USERGROUP));
    newEntry.setTodo(rs.getString(MI_COL_TODO));

    newEntry.setOldInstanceStatus(rs.getString(MI_COL_OLDINSTSTATUS));
    newEntry.setCorrelatedOrderIsMonitored(rs.getBoolean(MI_COL_ORDERMONITORED));
    newEntry.setPaused(rs.getBoolean(MI_COL_PAUSE));
    
    newEntry.setAllowedResponses(rs.getString(MI_COL_ALLOWED_RESPONSES));
 
  }


  public boolean hasBeenProcessed() {
    return result != null;
  }


  public Long getXynaOrderId() {
    return xynaOrderId;
  }


  private void setXynaOrderId(Long id) {
    this.xynaOrderId = id;
  }

  public void setXynaOrderPriority(int prio) {
    this.xynaOrderPriority = prio;
  }

  public int getXynaOrderPriority() {
    return xynaOrderPriority;
  }

  public void setXynaOrderMonitoringLevel(int lvl) {
    this.xynaOrderMonitoringLevel = lvl;
  }

  public int getXynaOrderMonitoringLevel() {
    return xynaOrderMonitoringLevel;
  }

  public void setXynaOrderSessionId(String sessionId) {
    this.xynaOrderSessionId = sessionId;
  }

  public String getXynaOrderSessionId() {
    return xynaOrderSessionId;
  }

  public void setParentOrderId(long id) {
    this.parentOrderId = id;
  }

  public Long getParentOrderId() {
    return parentOrderId;
  }

  public void setParentOrderType(String ordertype) {
    this.parentOrderType = ordertype;
  }

  public String getParentOrderType() {
    return parentOrderType;
  }

  
  public Long getRootOrderId() {
    return rootOrderId;
  }

  
  public void setRootOrderId(Long rootOrderId) {
    this.rootOrderId = rootOrderId;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  public ResumeTarget getResumeTarget() {
    return new ResumeTarget(rootOrderId, xynaOrderId);
  }

}
