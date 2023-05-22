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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessMarker;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SuspensionCause;



@Persistable(primaryKey = OrderInstance.COL_ID, tableName = OrderInstance.TABLE_NAME)
public class OrderInstance extends Storable<OrderInstance> {

  private static final long serialVersionUID = -1110466715851182759L;


  public static final String TABLE_NAME = "orderarchive";

  public static final String COL_ID = "id";
  public static final String COL_EXCEPTIONS = "exceptions";

  public static final String COL_PARENT_ID = "parentId";
  public static final String COL_EXECUTION_TYPE = "executionType";
  public static final String COL_PRIORITY = "priority";

  public static final String COL_STATUS = "status";
  public static final String COL_STATUS_COMPENSATE = "statusCompensate";
  public static final String COL_SUSPENSION_STATUS = "suspensionStatus";
  public static final String COL_SUSPENSION_CAUSE = "suspensionCause";

  public static final String COL_START_TIME = "startTime";
  public static final String COL_LAST_UPDATE = "lastUpdate";
  public static final String COL_STOP_TIME = "stopTime";

  public static final String COL_ORDERTYPE = "orderType";
  public static final String COL_MONITORING_LEVEL = "monitoringLevel";
  public static final String COL_CUSTOM0 = "custom0";
  public static final String COL_CUSTOM1 = "custom1";
  public static final String COL_CUSTOM2 = "custom2";
  public static final String COL_CUSTOM3 = "custom3";

  public static final String COL_SESSION_ID = "sessionId";

  public static final String COL_INTERNAL_ORDER = "internalorder";
  
  public static final String COL_ROOT_ID = "rootId";
  
  public static final String COL_APPLICATIONNAME = "applicationName";
  public static final String COL_VERISONNAME = "versionName";
  public static final String COL_WORKSPACENAME = "workspaceName";
  public static final String COL_BATCH_PROCESS_ID = "batchProcessId";


  @Column(name = COL_ID, size = 14)
  private long id;
  @Column(name = COL_PARENT_ID, index = IndexType.MULTIPLE, size = 14)
  private long parentId = -1;
  @Column(name = COL_EXECUTION_TYPE, size = 50)
  private String executionType;
  @Column(name = COL_PRIORITY, size = 2)
  private int priority;

  @Column(name = COL_STATUS, size = 50)
  private String status; //TODO besser OrderInstanceStatus statt String, Problem mit Abw�rtskompatibilit�t
  @Column(name = COL_STATUS_COMPENSATE, size = 50)
  private String statusCompensate; //TODO besser OrderInstanceCompensationStatus statt String, Problem mit Abw�rtskompatibilit�t
  @Column(name = COL_SUSPENSION_STATUS, size = 50)
  private String suspensionStatus; //TODO besser OrderInstanceSuspensionStatus statt String, Problem mit Abw�rtskompatibilit�t
  @Column(name = COL_SUSPENSION_CAUSE, size = 100)
  private String suspensionCause;

  @Column(name = COL_START_TIME, size = 14)
  private long startTime = -1; //wann wurde auftrag eingestellt
  @Column(name = COL_LAST_UPDATE, index = IndexType.MULTIPLE, size = 14)
  private long lastUpdate;
  @Column(name = COL_STOP_TIME, size = 14)
  private long stopTime = -1;

  @Column(name = COL_ORDERTYPE, size = 200, index = IndexType.MULTIPLE)
  private String orderType;
  @Column(name = COL_MONITORING_LEVEL, size = 2)
  private int monitoringLevel;
  @Column(name = COL_CUSTOM0, size = 200)
  private String custom0;
  @Column(name = COL_CUSTOM1, size = 200)
  private String custom1;
  @Column(name = COL_CUSTOM2, size = 200)
  private String custom2;
  @Column(name = COL_CUSTOM3, size = 200)
  private String custom3;

  @Column(name = COL_SESSION_ID, size = 256)
  private String sessionId;

  @Column(name = COL_INTERNAL_ORDER, index = IndexType.MULTIPLE)
  private boolean internalOrder;

  @Column(name = COL_ROOT_ID, size = 14, index = IndexType.MULTIPLE)
  private long rootId;
  
  @Column(name = COL_APPLICATIONNAME)
  private String applicationName;
  @Column(name = COL_VERISONNAME)
  private String versionName;
  @Column(name = COL_WORKSPACENAME) //FIXME index definieren - vgl bugz 19043
  private String workspaceName;

  @Column(name = COL_BATCH_PROCESS_ID)
  private Long batchProcessId;
  
  @Column(name = COL_EXCEPTIONS, type = ColumnType.BLOBBED_JAVAOBJECT)
  protected List<XynaExceptionInformation> exceptions;
  
  public OrderInstance() {
  }

  /**
   * wirft fehler, falls xynaexceptioninformation transformation (xml erstellung) nicht funktionert
   */
  public OrderInstance(XynaOrder order) {
    id = order.getId();
    priority = order.getPriority();
    status = OrderInstanceStatus.INITIALIZATION.getName();
    startTime = order.getEntranceTimestamp();
    lastUpdate = startTime;
    orderType = order.getDestinationKey().getOrderType();
    if (order.getMonitoringCode() != null) {
      monitoringLevel = order.getMonitoringCode();
    }
    custom0 = order.getCustom0();
    custom1 = order.getCustom1();
    custom2 = order.getCustom2();
    custom3 = order.getCustom3();
    sessionId = order.getSessionId();
    
    internalOrder = OrdertypeManagement.internalOrdertypes.contains(order.getDestinationKey().getOrderType());
  }


  public OrderInstance(XynaOrderServerExtension order) {
    this((XynaOrder) order);
    if (order.getParentOrder() != null) {
      parentId = order.getParentOrder().getId();
    }
    executionType = order.getExecutionType().toString();
    rootId = order.getRootOrder().getId();
    BatchProcessMarker bpm = order.getRootOrder().getBatchProcessMarker();
    if( bpm != null ) {
      batchProcessId = bpm.getBatchProcessId();
    }
    try {
      RuntimeContext rc = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(order.getRevision());
      if (rc instanceof Application) {
        applicationName = rc.getName();
        versionName = ((Application) rc).getVersionName();
      } else if (rc instanceof Workspace) {
        workspaceName = rc.getName();
      } else {
        applicationName = order.getDestinationKey().getApplicationName();
        versionName = order.getDestinationKey().getVersionName();
        workspaceName = order.getDestinationKey().getWorkspaceName();  
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      applicationName = order.getDestinationKey().getApplicationName();
      versionName = order.getDestinationKey().getVersionName();
      workspaceName = order.getDestinationKey().getWorkspaceName();
    }
  }


  public OrderInstance(long id) {
    this.id = id;
    status = OrderInstanceStatus.INITIALIZATION.getName();
  }


  public long getId() {
    return id;
  }
  
  
  public long getRootId() {
    return rootId;
  }


  // this method is dangerous and is only used for update reasons
  public void setRootId( long rootID ) {
    this.rootId = rootID;
  }
  
  
  public long getParentId() {
    return parentId;
  }


  public String getExecutionType() {
    return executionType;
  }


  public int getPriority() {
    return priority;
  }


  /**
   * @deprecated use getStatusAsString or getStatusAsEnum
   */
  @Deprecated
  public String getStatus() {
    return status;
  }
  public String getStatusAsString() {
    return status;
  }
  public OrderInstanceStatus getStatusAsEnum() {
    return OrderInstanceStatus.fromString(status);
  }


  /**
   * @deprecated use setStatusAsString(String) or setStatus(OrderInstanceStatus) 
   */
  @Deprecated
  public void setStatus(String status) {
    this.status = status;
  }
  public void setStatusAsString(String status) {
    this.status = status;
  }
  public void setStatus(OrderInstanceStatus status) {
    this.status = status.getName();
  }


  public long getStartTime() {
    return startTime;
  }


  public long getStopTime() {
    return stopTime;
  }


  public void setStopTime(long stopTime) {
    this.stopTime = stopTime;
  }


  public long getLastUpdate() {
    return lastUpdate;
  }


  public void setLastUpdate(long lastUpdate) {
    this.lastUpdate = lastUpdate;
  }


  public String getOrderType() {
    return orderType;
  }


  public int getMonitoringLevel() {
    return monitoringLevel;
  }


  public void setMonitoringLevel(int monitoringLevel) {
    this.monitoringLevel = monitoringLevel;
  }



  public String getCustom0() {
    return custom0;
  }


  public String getCustom1() {
    return custom1;
  }


  public String getCustom2() {
    return custom2;
  }


  public String getCustom3() {
    return custom3;
  }
  
  
  public String getSessionId() {
    return sessionId;
  }
  
  
  public void setCustom0(String custom0) {
    this.custom0 = custom0;
  }


  public void setCustom1(String custom1) {
    this.custom1 = custom1;
  }


  public void setCustom2(String custom2) {
    this.custom2 = custom2;
  }


  public void setCustom3(String custom3) {
    this.custom3 = custom3;
  }

  
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public void setExecutionType(ExecutionType executionType) {
    this.executionType = executionType.toString();
  }


  protected static void fillByResultSet(OrderInstance oi, ResultSet rs)
      throws SQLException {

    oi.id = rs.getLong(OrderInstanceColumn.C_ID.getColumnName());
    oi.parentId = rs.getLong(OrderInstanceColumn.C_PARENT_ID.getColumnName());

    oi.executionType = rs.getString(OrderInstanceColumn.C_EXECUTION_TYPE.getColumnName());
    oi.priority = rs.getInt(OrderInstanceColumn.C_PRIORITY.getColumnName());

    oi.status = rs.getString(OrderInstanceColumn.C_STATUS.getColumnName());
    oi.statusCompensate = rs.getString(OrderInstanceColumn.C_STATUS_COMPENSATE.getColumnName());
    oi.suspensionStatus = rs.getString(OrderInstanceColumn.C_SUSPENSION_STATUS.getColumnName());
    oi.suspensionCause = rs.getString(OrderInstanceColumn.C_SUSPENSION_CAUSE.getColumnName());

    oi.startTime = rs.getLong(OrderInstanceColumn.C_START_TIME.getColumnName());
    oi.stopTime = rs.getLong(OrderInstanceColumn.C_STOP_TIME.getColumnName());
    oi.lastUpdate = rs.getLong(OrderInstanceColumn.C_LAST_UPDATE.getColumnName());
    oi.orderType = rs.getString(OrderInstanceColumn.C_ORDER_TYPE.getColumnName());
    oi.monitoringLevel = rs.getInt(OrderInstanceColumn.C_MONITORING_LEVEL.getColumnName());

    oi.custom0 = rs.getString(OrderInstanceColumn.C_CUSTOM0.getColumnName());
    oi.custom1 = rs.getString(OrderInstanceColumn.C_CUSTOM1.getColumnName());
    oi.custom2 = rs.getString(OrderInstanceColumn.C_CUSTOM2.getColumnName());
    oi.custom3 = rs.getString(OrderInstanceColumn.C_CUSTOM3.getColumnName());
    oi.sessionId = rs.getString(OrderInstanceColumn.C_SESSION_ID.getColumnName());
    
    oi.internalOrder = rs.getBoolean(COL_INTERNAL_ORDER);
    oi.rootId = rs.getLong(COL_ROOT_ID);
    oi.applicationName = rs.getString(COL_APPLICATIONNAME);
    oi.versionName = rs.getString(COL_VERISONNAME);
    oi.workspaceName = rs.getString(COL_WORKSPACENAME);
    long bpi = rs.getLong(COL_BATCH_PROCESS_ID);
    if( ! rs.wasNull() ) {
      oi.batchProcessId = Long.valueOf(bpi);
    }
    
  }


  private static class OrderInstanceReader implements ResultSetReader<OrderInstance> {

    private final boolean includeExceptions;

    private OrderInstanceReader(boolean includeExceptionsAndExecType) {
      this.includeExceptions = includeExceptionsAndExecType;
    }

    public OrderInstance read(ResultSet rs) throws SQLException {
      OrderInstance oi = new OrderInstance();
      fillByResultSet(oi, rs);
      return oi;
    }

  }


  private static OrderInstanceReader reader = new OrderInstanceReader(true);
  private static OrderInstanceReader readerWithoutExceptions = new OrderInstanceReader(false);

  @Override
  public ResultSetReader<? extends OrderInstance> getReader() {
    return reader;
  }


  public ResultSetReader<? extends OrderInstance> getReaderWithoutExceptions() {
    return readerWithoutExceptions;
  }


  @Override
  public <U extends OrderInstance> void setAllFieldsFromData(U data) {
    OrderInstance cast = data;
    id = data.getId();
    parentId = cast.parentId;

    executionType = cast.executionType;
    priority = cast.priority;

    status = cast.status;
    statusCompensate = cast.statusCompensate;
    suspensionStatus = cast.suspensionStatus;
    suspensionCause = cast.suspensionCause;

    startTime = cast.startTime;
    stopTime = cast.stopTime;
    lastUpdate = cast.lastUpdate;
    orderType = cast.orderType;
    monitoringLevel = cast.monitoringLevel;

    custom0 = cast.custom0;
    custom1 = cast.custom1;
    custom2 = cast.custom2;
    custom3 = cast.custom3;
    sessionId = cast.sessionId;

    internalOrder = cast.internalOrder;
    rootId = cast.rootId;
    applicationName = cast.applicationName;
    versionName = cast.versionName;
    workspaceName = cast.workspaceName;
    
    batchProcessId = cast.batchProcessId;
  }

  
  @SuppressWarnings("unused")
  private void setStartTime(long startTime) {
    //startTime wurde im Konstruktor sinnvoll gesetzt und sollte nicht umgesetzt werden!
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public static class DynamicOrderInstanceReader implements ResultSetReader<OrderInstance> {

    private Set<OrderInstanceColumn> selectedCols;
    public DynamicOrderInstanceReader(Set<OrderInstanceColumn> selected) {
      selectedCols = selected;
    }

    public OrderInstance read(ResultSet rs) throws SQLException {
      OrderInstance oi = new OrderInstance();
      //TODO performance
      if (selectedCols.contains(OrderInstanceColumn.C_ID)) {
        oi.id = rs.getLong(OrderInstanceColumn.C_ID.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_PARENT_ID)) {
        oi.parentId = rs.getLong(OrderInstanceColumn.C_PARENT_ID.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_EXECUTION_TYPE)) {
        oi.executionType = rs.getString(OrderInstanceColumn.C_EXECUTION_TYPE.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_PRIORITY)) {
        oi.priority = rs.getInt(OrderInstanceColumn.C_PRIORITY.getColumnName());
      }

      if (selectedCols.contains(OrderInstanceColumn.C_STATUS)) {
        oi.status = rs.getString(OrderInstanceColumn.C_STATUS.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_STATUS_COMPENSATE)) {
        oi.statusCompensate = rs.getString(OrderInstanceColumn.C_STATUS_COMPENSATE.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_SUSPENSION_STATUS)) {
        oi.suspensionStatus = rs.getString(OrderInstanceColumn.C_SUSPENSION_STATUS.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_SUSPENSION_CAUSE)) {
        oi.suspensionCause = rs.getString(OrderInstanceColumn.C_SUSPENSION_CAUSE.getColumnName());
      }

      if (selectedCols.contains(OrderInstanceColumn.C_START_TIME)) {
        oi.startTime = rs.getLong(OrderInstanceColumn.C_START_TIME.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_STOP_TIME)) {
        oi.stopTime = rs.getLong(OrderInstanceColumn.C_STOP_TIME.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_LAST_UPDATE)) {
        oi.lastUpdate = rs.getLong(OrderInstanceColumn.C_LAST_UPDATE.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_ORDER_TYPE)) {
        oi.orderType = rs.getString(OrderInstanceColumn.C_ORDER_TYPE.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_MONITORING_LEVEL)) {
        oi.monitoringLevel = rs.getInt(OrderInstanceColumn.C_MONITORING_LEVEL.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_CUSTOM0)) {
        oi.custom0 = rs.getString(OrderInstanceColumn.C_CUSTOM0.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_CUSTOM1)) {
        oi.custom1 = rs.getString(OrderInstanceColumn.C_CUSTOM1.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_CUSTOM2)) {
        oi.custom2 = rs.getString(OrderInstanceColumn.C_CUSTOM2.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_CUSTOM3)) {
        oi.custom3 = rs.getString(OrderInstanceColumn.C_CUSTOM3.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_SESSION_ID)) {
        oi.sessionId = rs.getString(OrderInstanceColumn.C_SESSION_ID.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_INTERNAL_ORDER)) {
        oi.internalOrder = rs.getBoolean(OrderInstanceColumn.C_INTERNAL_ORDER.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_ROOT_ID)) {
        oi.rootId = rs.getLong(OrderInstanceColumn.C_ROOT_ID.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_APPLICATIONNAME)) {
        oi.applicationName = rs.getString(OrderInstanceColumn.C_APPLICATIONNAME.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_VERISONNAME)) {
        oi.versionName = rs.getString(OrderInstanceColumn.C_VERISONNAME.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_WORKSPACENAME)) {
        oi.workspaceName = rs.getString(OrderInstanceColumn.C_WORKSPACENAME.getColumnName());
      }
      if (selectedCols.contains(OrderInstanceColumn.C_BATCH_PROCESS_ID)) {
        long bpi = rs.getLong(OrderInstanceColumn.C_BATCH_PROCESS_ID.getColumnName());
        if( ! rs.wasNull() ) {
          oi.batchProcessId = Long.valueOf(bpi);
        }
      }
      
      return oi;
    }

   
  }
  
  public String toString() {
    return Long.toString(id);
  }

  public String getStatusCompensate() {
    return statusCompensate;
  }

  
  public void setStatusCompensate(String statusCompensate) {
    this.statusCompensate = statusCompensate;
  }
  public void setStatusCompensate(OrderInstanceCompensationStatus statusCompensate) {
    this.statusCompensate = statusCompensate.getName();
  }

  
  public String getSuspensionStatus() {
    return suspensionStatus;
  }

  
  public void setSuspensionStatus(String suspended) {
    this.suspensionStatus = suspended;
  }
  public void setSuspensionStatus(OrderInstanceSuspensionStatus suspensionStatus) {
    this.suspensionStatus = suspensionStatus.getName();
  }

  
  public String getSuspensionCause() {
    return suspensionCause;
  }

  
  public void setSuspensionCause(String suspensionCause) {
    this.suspensionCause = suspensionCause;
  }


  public String getSuspensionCauseAsString(boolean humanReadable) {

    if (suspensionCause == null) {
      return null;
    } else if (suspensionCause.contains(",")) {

      String[] parts = suspensionCause.split(",");
      StringBuilder result = new StringBuilder();
      for (int i = parts.length - 1; i > -1; i--) {
        try {
          if (humanReadable) {
            result.append(parts[i]);
          } else {
            result.append(SuspensionCause.valueOf(parts[i]));
          }
        } catch (IllegalArgumentException e) {
          result.append("unknown suspension cause: " + parts[i]);
        }
        if (i > 0) {
          if (humanReadable) {
            result.append(", ");
          } else {
            result.append(",");
          }
        }
      }

      return result.toString();

    } else {

      try {
        return SuspensionCause.valueOf(suspensionCause).getSuspensionCauseString();
      } catch (IllegalArgumentException e) {
        return "unknown suspension cause: " + suspensionCause;
      }

    }
  }


  public void setInternalOrder(boolean internalOrder) {
    this.internalOrder = internalOrder;
  }

  public boolean isInternalOrder() {
    return internalOrder;
  }

  
  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  
  public String getVersionName() {
    return versionName;
  }
  
  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  
  public String getWorkspaceName() {
    return workspaceName;
  }
  
  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }
  
  public Long getBatchProcessId() {
    return batchProcessId;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    if (applicationName != null) {
      return new Application(applicationName, versionName);
    }
    
    if (workspaceName != null) {
      return new Workspace(workspaceName);
    }
    
    return RevisionManagement.DEFAULT_WORKSPACE;
  }
  
  
  public List<XynaExceptionInformation> getExceptions() {
    return exceptions;
  }


  public void addException(XynaException exception) {
    if (exceptions == null) {
      exceptions = new ArrayList<XynaExceptionInformation>();
    }
    if (exception instanceof GeneralXynaObject) {
      long revision;
      try {
        revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(getApplicationName(), getVersionName(), getWorkspaceName());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      exceptions
          .add(new XynaExceptionInformation(exception, -1, GeneralXynaObject.XMLReferenceCache.getCacheObjectWithoutCaching(revision)));
    } else {
      exceptions.add(new XynaExceptionInformation(exception));
    }
  }
  
  
  public void setExceptions(XynaException[] errors) {
    if (exceptions == null) {
      exceptions = new ArrayList<XynaExceptionInformation>();
    } else {
      exceptions.clear();
    }
    for (XynaException e: errors) {
      addException(e);
    }
  }
  
  
}
