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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;

@Persistable(primaryKey = BatchProcessArchiveStorable.COL_ORDER_ID, tableName = BatchProcessArchiveStorable.TABLE_NAME)
public class BatchProcessArchiveStorable extends Storable<BatchProcessArchiveStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "batchprocessarchive";
  public static ResultSetReader<BatchProcessArchiveStorable> reader = new BatchProcessArchiveStorableReader();
  public static ResultSetReader<Long> idReader = new BatchProcessArchiveStorableIdReader();

  public static final String COL_ORDER_ID = "orderId"; //Order ID des Batch Processes
  public static final String COL_LABEL = "label"; //Bezeichnung des Batch Processes 
  public static final String COL_APPLICATION = "application"; //Application, in der der Batch Process l�uft und in der alle Subauftr�ge gestartet werden
  public static final String COL_VERSION = "version"; //Zugeh�rige Version der Application
  public static final String COL_WORKSPACE = "workspace"; //Workspace, in dem der Batch Process l�uft und in dem alle Subauftr�ge gestartet werden
  public static final String COL_COMPONENT = "component"; //Die zugeh�rige Komponente, z.B. XDNC.XFwMgmt
  public static final String COL_ORDER_STATUS = "orderStatus"; //OrderInstanceStatus des Masters
  public static final String COL_FAILED = "failed"; //Anzahl der fehlgeschlagenen Subauftr�ge
  public static final String COL_FINISHED = "finished"; //Anzahl der erfolgreich fertiggelaufenen Subauftr�ge
  public static final String COL_CANCELED = "canceled"; //Anzahl der Objekte, die aufgrund eines Abbruchs nicht verarbeitet wurden.
  public static final String COL_TOTAL = "total"; //Gesamtzahl der zu startenden Subauftr�ge
  public static final String COL_SLAVE_ORDER_TYPE = "slaveOrdertype"; //Ordertype der zu startenden Subauftr�ge
  public static final String COL_CUSTOM0 = "custom0"; //Erstes Custom-Feld, f�r die freie Verwendung durch Komponenten.
  public static final String COL_CUSTOM1 = "custom1";
  public static final String COL_CUSTOM2 = "custom2";
  public static final String COL_CUSTOM3 = "custom3";
  public static final String COL_CUSTOM4 = "custom4";
  public static final String COL_CUSTOM5 = "custom5";
  public static final String COL_CUSTOM6 = "custom6";
  public static final String COL_CUSTOM7 = "custom7";
  public static final String COL_CUSTOM8 = "custom8";
  public static final String COL_CUSTOM9 = "custom9";

  public static final int NUM_CUSTOM = 10;
  
  
  @Column(name = COL_ORDER_ID, index = IndexType.PRIMARY)
  private long orderId;//Order ID des Batch Processes

  @Column(name = COL_LABEL)
  private String label;//Bezeichnung des Batch Processes 

  @Column(name = COL_APPLICATION)
  private String application;//Application, in der der Batch Process l�uft und in der alle Subauftr�ge gestartet werden

  @Column(name = COL_VERSION)
  private String version; //Zugeh�rige Version der Application
  
  @Column(name = COL_WORKSPACE)
  private String workspace; //Workspace, in dem der Batch Process l�uft und in dem alle Subauftr�ge gestartet werden

  @Column(name = COL_COMPONENT)
  private String component; //Die zugeh�rige Komponente, z.B. XDNC.XFwMgmt

  @Column(name = COL_ORDER_STATUS)
  private OrderInstanceStatus orderStatus; //OrderInstanceStatus des Masters
  
  @Column(name = COL_FAILED)
  private int failed; //Anzahl der fehlgeschlagenen Subauftr�ge

  @Column(name = COL_FINISHED)
  private int finished; //Anzahl der erfolgreich fertiggelaufenen Subauftr�ge

  @Column(name = COL_CANCELED)
  private int canceled; //Anzahl der Objekte, die aufgrund eines Abbruchs nicht verarbeitet wurden.

  @Column(name = COL_TOTAL)
  private Integer total; //Gesamtzahl der zu startenden Subauftr�ge

  @Column(name = COL_SLAVE_ORDER_TYPE)
  private String slaveOrdertype; //Ordertype der zu startenden Subauftr�ge

  @Column(name = COL_CUSTOM0)
  private String custom0; //Erstes Custom-Feld, f�r die freie Verwendung durch Komponenten.

  @Column(name = COL_CUSTOM1)
  private String custom1;
  
  @Column(name = COL_CUSTOM2)
  private String custom2;
  
  @Column(name = COL_CUSTOM3)
  private String custom3;
  
  @Column(name = COL_CUSTOM4)
  private String custom4;
  
  @Column(name = COL_CUSTOM5)
  private String custom5;
  
  @Column(name = COL_CUSTOM6)
  private String custom6;
  
  @Column(name = COL_CUSTOM7)
  private String custom7;
  
  @Column(name = COL_CUSTOM8)
  private String custom8;
  
  @Column(name = COL_CUSTOM9)
  private String custom9;
    
  private ReentrantLock lock = new ReentrantLock();

  public BatchProcessArchiveStorable(){
  }
  
  public BatchProcessArchiveStorable(long batchProcessId) {
    this.orderId = batchProcessId;
  }

  public BatchProcessArchiveStorable(long batchProcessId, BatchProcessInput input) {
    this.orderId = batchProcessId;
    label = input.getLabel();
    if(input.getMasterOrder() != null && input.getMasterOrder().getDestinationKey() != null){
      application = input.getMasterOrder().getDestinationKey().getApplicationName();
      version = input.getMasterOrder().getDestinationKey().getVersionName();
      workspace = input.getMasterOrder().getDestinationKey().getWorkspaceName();
    }
    component = input.getComponent();
    slaveOrdertype = input.getSlaveOrderType();
    orderStatus = OrderInstanceStatus.SCHEDULING;
  }
  
  public BatchProcessArchiveStorable(BatchProcessArchiveStorable from) {
    setAllFieldsFromData(from);
  }

  /**
   * �bernimmt die Counter aus der RuntimeInformation
   * @param runtimeInformation
   * @param terminated Ist der Batch Process bereits abgeschlossen?
   */
  public void updateWithRuntimeInformation(BatchProcessRuntimeInformationStorable runtimeInformation, boolean terminated) {
    this.finished = runtimeInformation.getFinished();
    this.failed = runtimeInformation.getFailed();
    
    //falls der Batch Prozess abgeschlossen ist, aber noch Slaves laufen (zum Beispiel, weil
    //sie beim cancel nicht abgebrochen werden konnten), werden diese als failed gez�hlt
    if (terminated) {
      this.failed += runtimeInformation.getRunning();
    }
  }
  
  public void updateWithCustomization(BatchProcessCustomizationStorable customization) {
    if (StringUtils.isEmpty(this.custom0)) {
      this.custom0 = customization.getCounterAsString(0);
    }
    if (StringUtils.isEmpty(this.custom1)) {
      this.custom1 = customization.getCounterAsString(1);
    }
    if (StringUtils.isEmpty(this.custom2)) {
      this.custom2 = customization.getCounterAsString(2);
    }
    if (StringUtils.isEmpty(this.custom3)) {
      this.custom3 = customization.getCounterAsString(3);
    }
    if (StringUtils.isEmpty(this.custom4)) {
      this.custom4 = customization.getCounterAsString(4);
    }
    if (StringUtils.isEmpty(this.custom5)) {
      this.custom5 = customization.getCounterAsString(5);
    }
    if (StringUtils.isEmpty(this.custom6)) {
      this.custom6 = customization.getCounterAsString(6);
    }
    if (StringUtils.isEmpty(this.custom7)) {
      this.custom7 = customization.getCounterAsString(7);
    }
    if (StringUtils.isEmpty(this.custom8)) {
      this.custom8 = customization.getCounterAsString(8);
    }
    if (StringUtils.isEmpty(this.custom9)) {
      this.custom9 = customization.getCounterAsString(9);
    }
  }
  
  public void updateWithRestartInformation(BatchProcessRestartInformationStorable restartInformation) {
    this.total = restartInformation.getTotal();
  }

  @Override
  public ResultSetReader<? extends BatchProcessArchiveStorable> getReader() {
    return reader;
  }

  @Override
  public Long getPrimaryKey() {
    return Long.valueOf(orderId);
  }

  @Override
  public <U extends BatchProcessArchiveStorable> void setAllFieldsFromData(U data) {
    BatchProcessArchiveStorable cast = data;
    this.orderId = cast.orderId;
    this.label = cast.label;
    this.application = cast.application;
    this.version = cast.version;
    this.workspace = cast.workspace;
    this.component = cast.component;
    this.orderStatus = cast.orderStatus;
    this.failed = cast.failed;
    this.finished = cast.finished;
    this.canceled = cast.canceled;
    this.total = cast.total;
    this.slaveOrdertype = cast.slaveOrdertype;
    this.custom0 = cast.custom0;
    this.custom1 = cast.custom1;
    this.custom2 = cast.custom2;
    this.custom3 = cast.custom3;
    this.custom4 = cast.custom4;
    this.custom5 = cast.custom5;
    this.custom6 = cast.custom6;
    this.custom7 = cast.custom7;
    this.custom8 = cast.custom8;
    this.custom9 = cast.custom9;
  }
  
  private static class BatchProcessArchiveStorableIdReader implements ResultSetReader<Long> {
    public Long read(ResultSet rs) throws SQLException {
      return rs.getLong(COL_ORDER_ID);
    }
  }
  
  private static class BatchProcessArchiveStorableReader implements ResultSetReader<BatchProcessArchiveStorable> {
    public BatchProcessArchiveStorable read(ResultSet rs) throws SQLException {
      BatchProcessArchiveStorable result = new BatchProcessArchiveStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(BatchProcessArchiveStorable bpas, ResultSet rs) throws SQLException {
    bpas.orderId = rs.getLong(COL_ORDER_ID);
    bpas.label = rs.getString(COL_LABEL);
    bpas.application = rs.getString(COL_APPLICATION);
    bpas.version = rs.getString(COL_VERSION);
    bpas.workspace = rs.getString(COL_WORKSPACE);
    bpas.component = rs.getString(COL_COMPONENT);
    if (rs.getString(COL_ORDER_STATUS) != null) {
      bpas.orderStatus = OrderInstanceStatus.fromString(rs.getString(COL_ORDER_STATUS));
    }
    bpas.failed = rs.getInt(COL_FAILED);
    bpas.finished = rs.getInt(COL_FINISHED);
    bpas.canceled = rs.getInt(COL_CANCELED);
    bpas.total = rs.getInt(COL_TOTAL);
    bpas.slaveOrdertype = rs.getString(COL_SLAVE_ORDER_TYPE);
    bpas.custom0 = rs.getString(COL_CUSTOM0);
    bpas.custom1 = rs.getString(COL_CUSTOM1);
    bpas.custom2 = rs.getString(COL_CUSTOM2);
    bpas.custom3 = rs.getString(COL_CUSTOM3);
    bpas.custom4 = rs.getString(COL_CUSTOM4);
    bpas.custom5 = rs.getString(COL_CUSTOM5);
    bpas.custom6 = rs.getString(COL_CUSTOM6);
    bpas.custom7 = rs.getString(COL_CUSTOM7);
    bpas.custom8 = rs.getString(COL_CUSTOM8);
    bpas.custom9 = rs.getString(COL_CUSTOM9);
  }

  
  public long getOrderId() {
    return orderId;
  }

  
  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  
  public String getLabel() {
    return label;
  }

  
  public void setLabel(String label) {
    this.label = label;
  }

  
  public String getApplication() {
    return application;
  }

  
  public void setApplication(String application) {
    this.application = application;
  }

  
  public String getVersion() {
    return version;
  }

  
  public String getWorkspace() {
    return workspace;
  }
  
  
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  
  public RuntimeContext getRuntimeContext() {
    return RevisionManagement.getRuntimeContext(application, version, workspace);
  }
  
  public void setVersion(String version) {
    this.version = version;
  }

  
  public String getComponent() {
    return component;
  }

  
  public void setComponent(String component) {
    this.component = component;
  }

  
  public OrderInstanceStatus getOrderStatus() {
    return orderStatus;
  }

  
  public void setOrderStatus(OrderInstanceStatus orderStatus) {
    this.orderStatus = orderStatus;
  }

  
  public int getFailed() {
    return failed;
  }

  
  public void setFailed(int failed) {
    this.failed = failed;
  }

  
  public int getFinished() {
    return finished;
  }

  
  public void setCanceled(int canceled) {
    this.canceled = canceled;
  }
  
  
  public int getCanceled() {
    return canceled;
  }

  
  public void setFinished(int finished) {
    this.finished = finished;
  }

  
  public Integer getTotal() {
    return total;
  }

  
  public void setTotal(Integer total) {
    this.total = total;
  }

  
  public String getSlaveOrdertype() {
    return slaveOrdertype;
  }

  
  public void setSlaveOrdertype(String slaveOrdertype) {
    this.slaveOrdertype = slaveOrdertype;
  }

  
  public String getCustom0() {
    return custom0;
  }

  
  public void setCustom10(String custom0) {
    this.custom0 = custom0;
  }

  
  public String getCustom1() {
    return custom1;
  }

  
  public void setCustom1(String custom1) {
    this.custom1 = custom1;
  }

  
  public String getCustom2() {
    return custom2;
  }

  
  public void setCustom2(String custom2) {
    this.custom2 = custom2;
  }

  
  public String getCustom3() {
    return custom3;
  }

  
  public void setCustom3(String custom3) {
    this.custom3 = custom3;
  }

  
  public String getCustom4() {
    return custom4;
  }

  
  public void setCustom4(String custom4) {
    this.custom4 = custom4;
  }

  
  public String getCustom5() {
    return custom5;
  }

  
  public void setCustom5(String custom5) {
    this.custom5 = custom5;
  }

  
  public String getCustom6() {
    return custom6;
  }

  
  public void setCustom6(String custom6) {
    this.custom6 = custom6;
  }

  
  public String getCustom7() {
    return custom7;
  }

  
  public void setCustom7(String custom7) {
    this.custom7 = custom7;
  }

  
  public String getCustom8() {
    return custom8;
  }

  
  public void setCustom8(String custom8) {
    this.custom8 = custom8;
  }

  
  public String getCustom9() {
    return custom9;
  }

  
  public void setCustom9(String custom9) {
    this.custom9 = custom9;
  }

  public ReentrantLock getLock() {
    return lock;
  }

  public void setCustom(int i, String value) {
    switch( i ) {
      case 0: custom0 = value; break;
      case 1: custom1 = value; break;
      case 2: custom2 = value; break;
      case 3: custom3 = value; break;
      case 4: custom4 = value; break;
      case 5: custom5 = value; break;
      case 6: custom6 = value; break;
      case 7: custom7 = value; break;
      case 8: custom8 = value; break;
      case 9: custom9 = value; break;
      default:
        throw new IllegalArgumentException("invalid index "+i+" out of range [0,"+NUM_CUSTOM+"[");
    }
  }
  
  public List<String> getCustomsAsList() {
    List<String> customs = new ArrayList<String>(NUM_CUSTOM);
    customs.add(custom0);
    customs.add(custom1);
    customs.add(custom2);
    customs.add(custom3);
    customs.add(custom4);
    customs.add(custom5);
    customs.add(custom6);
    customs.add(custom7);
    customs.add(custom8);
    customs.add(custom9);
    return customs;
  }
  
  
  public DestinationKey getDestinationKey() {
    if (workspace != null) {
      return new DestinationKey(slaveOrdertype, new Workspace(workspace));
    } else {
      return new DestinationKey(slaveOrdertype, application, version);
    }
  }
  
  /**
   * Ermittelt die Revision f�r die Application-Version des Batch Prozesses.
   * @return
   */
  public Long getRevision() {
    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(application, version, workspace);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (application != null) {
        throw new IllegalArgumentException("version '" + version + "' not found in application '" + application +"'", e);
      } else {
        throw new IllegalArgumentException("workspace '" + workspace + "' not found", e);
      }
    }
    return revision;
  }
}
