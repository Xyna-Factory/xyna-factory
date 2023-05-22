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

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.beans.SlaveExecutionPeriod;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData.InputGeneratorType;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;

@Persistable(primaryKey = BatchProcessRestartInformationStorable.COL_BATCH_PROCESS_ID, tableName = BatchProcessRestartInformationStorable.TABLE_NAME)
public class BatchProcessRestartInformationStorable extends Storable<BatchProcessRestartInformationStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "bprestartinformation";

  public static ResultSetReader<? extends BatchProcessRestartInformationStorable> reader = new BatchProcessRestartInformationStorableReader();
  public static ResultSetReader<Long> idReader = new BatchProcessRestartInformationStorableIdReader();

  public static final String COL_BATCH_PROCESS_ID = "batchProcessId"; //ID des Batch Processes
  public static final String COL_INPUT_GENERATOR_TYPE = "inputGeneratorType"; //Typ des Input-Generators
  public static final String COL_CONSTANT_INPUT = "constantInput"; //XML-Darstellung des Inputs, den der konstante InputGenerator liefern soll
  public static final String COL_INPUT_STORABLE = "inputStorable"; //XMOM Storable, das die Input-Daten liefert
  public static final String COL_INPUT_QUERY = "inputQuery"; //benutzerdefinierte QueryCondition f�r Input-Daten
  public static final String COL_INPUT_SORT_CRITERIA = "inputSortCriteria"; //Sortierkriterien
  public static final String COL_TOTAL = "total"; //Gesamtzahl der zu startenden Subauftr�ge
  public static final String COL_MASTER_SCHEDULING_DATA = "masterSchedulingData"; //SchedulingData f�r den Master
  public static final String COL_SLAVE_ORDER_EXEC_TIMEOUT = "slaveOrderExecTimeout"; //OrderExecutionTimeout der zu startenden Subauftr�ge
  public static final String COL_SLAVE_WORKFLOW_EXEC_TIMEOUT = "slaveWorkflowExecTimeout"; //WorkflowExecutionTimeout der zu startenden Subauftr�ge
  public static final String COL_SLAVE_TIME_CONSTRAINT = "slaveTimeConstraint"; //TimeConstraint der zu startenden Subauftr�ge
  public static final String COL_MAX_PARALLELISM = "maxParallelism"; //maximale Anzahl an gleichzeitig gestarteten Subauftr�gen
  public static final String COL_SLAVE_EXECUTION_PERIOD = "slaveExecutionPeriod"; //Wiederholungen
  public static final String COL_GUI_REPRESENTATION_DATA = "guiRepresentationData"; //beliebiger String, zur freien Verwendung f�r den WebService
  public static final String COL_TIME_WINDOW_DEFINITION = "timeWindowDefinition"; //Definition des Zeitfensters

  @Column(name = COL_BATCH_PROCESS_ID, index = IndexType.PRIMARY)
  private long batchProcessId; //ID des Batch Processes
  
  @Column(name = COL_INPUT_GENERATOR_TYPE)
  private InputGeneratorType inputGeneratorType; //Typ des Input-Generators
  
  @Column(name = COL_CONSTANT_INPUT, size = Integer.MAX_VALUE)
  private String constantInput; //XML-Darstellung des Inputs, den der konstante InputGenerator liefern soll
  
  @Column(name = COL_INPUT_STORABLE)
  private String inputStorable; //XMOM Storable, das die Input-Daten liefert
  
  @Column(name = COL_INPUT_QUERY, size = 4000)
  private String inputQuery; //benutzerdefinierte QueryCondition f�r Input-Daten
  
  @Column(name = COL_INPUT_SORT_CRITERIA)
  private String inputSortCriteria; //Sortierkriterien

  @Column(name = COL_TOTAL)
  private Integer total; //Gesamtzahl der zu startenden Subauftr�ge

  @Column(name = COL_MASTER_SCHEDULING_DATA, type = ColumnType.BLOBBED_JAVAOBJECT)
  private SchedulingData masterSchedulingData; //SchedulingData f�r den Master
  
  @Column(name = COL_SLAVE_ORDER_EXEC_TIMEOUT)
  private AbsRelTime slaveOrderExecTimeout; //OrderExecutionTimeout der zu startenden Subauftr�ge

  @Column(name = COL_SLAVE_WORKFLOW_EXEC_TIMEOUT)
  private AbsRelTime slaveWorkflowExecTimeout; //WorkflowExecutionTimeout der zu startenden Subauftr�ge

  @Column(name = COL_SLAVE_TIME_CONSTRAINT)
  private TimeConstraint slaveTimeConstraint; //TimeConstraint der zu startenden Subauftr�ge

  @Column(name = COL_MAX_PARALLELISM)
  private Integer maxParallelism; //maximale Anzahl an gleichzeitig gestarteten Subauftr�gen

  @Column(name = COL_SLAVE_EXECUTION_PERIOD)
  private SlaveExecutionPeriod slaveExecutionPeriod; //Wiederholungen
  
  @Column(name = COL_GUI_REPRESENTATION_DATA, size=4000)
  private String guiRepresentationData; //beliebiger String, zur freien Verwendung f�r den WebService

  @Column(name = COL_TIME_WINDOW_DEFINITION, size=4000)
  private TimeWindowDefinition timeWindowDefinition; //Definition des Zeitfensters



  public BatchProcessRestartInformationStorable(){
  }

  public BatchProcessRestartInformationStorable(long batchProcessId){
    this.batchProcessId = batchProcessId;
  }

  public BatchProcessRestartInformationStorable(long batchProcessId, BatchProcessInput input){
    this.batchProcessId = batchProcessId;
    inputGeneratorType = input.getInputGeneratorData().getInputGeneratorType();
    constantInput = input.getInputGeneratorData().getConstantInput();
    inputStorable = input.getInputGeneratorData().getStorable();
    inputQuery = input.getInputGeneratorData().getQuery();
    inputSortCriteria = input.getInputGeneratorData().getSortCriteria();
    total = input.getInputGeneratorData().getMaximumInputs();
    slaveOrderExecTimeout = input.getSlaveOrderExecTimeout();
    slaveWorkflowExecTimeout = input.getSlaveWorkflowExecTimeout();
    slaveTimeConstraint = input.getSlaveTimeConstraint();
    maxParallelism = input.getMaxParallelism();
    slaveExecutionPeriod = input.getSlaveExecutionPeriod();
    guiRepresentationData = input.getGuiRepresentationData();
    timeWindowDefinition = input.getTimeWindowDefinition();
  }
  
  public BatchProcessRestartInformationStorable(BatchProcessRestartInformationStorable data) {
    setAllFieldsFromData(data);
  }
  
  @Override
  public ResultSetReader<? extends BatchProcessRestartInformationStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return Long.valueOf(batchProcessId);
  }

  @Override
  public <U extends BatchProcessRestartInformationStorable> void setAllFieldsFromData(U data) {
    BatchProcessRestartInformationStorable cast = data;
    this.batchProcessId = cast.batchProcessId;
    this.inputStorable = cast.inputStorable;
    this.inputQuery = cast.inputQuery;
    this.inputSortCriteria = cast.inputSortCriteria;
    this.inputGeneratorType = cast.inputGeneratorType;
    this.constantInput = cast.constantInput;
    this.total = cast.total;
    this.masterSchedulingData = cast.masterSchedulingData;
    this.slaveOrderExecTimeout = cast.slaveOrderExecTimeout;
    this.slaveWorkflowExecTimeout = cast.slaveWorkflowExecTimeout;
    this.slaveTimeConstraint = cast.slaveTimeConstraint;
    this.maxParallelism = cast.maxParallelism;
    this.slaveExecutionPeriod = cast.slaveExecutionPeriod;
    this.guiRepresentationData = cast.guiRepresentationData;
    this.timeWindowDefinition = cast.timeWindowDefinition;
  }

  private static class BatchProcessRestartInformationStorableIdReader implements ResultSetReader<Long> {
    public Long read(ResultSet rs) throws SQLException {
      return rs.getLong(COL_BATCH_PROCESS_ID);
    }
  }
  
  private static class BatchProcessRestartInformationStorableReader implements ResultSetReader<BatchProcessRestartInformationStorable> {
    public BatchProcessRestartInformationStorable read(ResultSet rs) throws SQLException {
      BatchProcessRestartInformationStorable result = new BatchProcessRestartInformationStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(BatchProcessRestartInformationStorable bpris, ResultSet rs) throws SQLException {
    bpris.batchProcessId = rs.getLong(COL_BATCH_PROCESS_ID);
    bpris.inputStorable = rs.getString(COL_INPUT_STORABLE);
    bpris.inputQuery = rs.getString(COL_INPUT_QUERY);
    bpris.inputSortCriteria = rs.getString(COL_INPUT_SORT_CRITERIA);
    String type = rs.getString(COL_INPUT_GENERATOR_TYPE);
    if (type == null || rs.wasNull()) {
      bpris.inputGeneratorType = null;   
    } else {
      bpris.inputGeneratorType = InputGeneratorType.valueOf(type);
    }
    bpris.constantInput = rs.getString(COL_CONSTANT_INPUT);
    bpris.total = rs.getInt(COL_TOTAL);
    bpris.masterSchedulingData = (SchedulingData) bpris.readBlobbedJavaObjectFromResultSet(rs, COL_MASTER_SCHEDULING_DATA, String.valueOf(bpris.batchProcessId));
    bpris.slaveOrderExecTimeout = AbsRelTime.valueOf(rs.getString(COL_SLAVE_ORDER_EXEC_TIMEOUT));
    bpris.slaveWorkflowExecTimeout = AbsRelTime.valueOf(rs.getString(COL_SLAVE_WORKFLOW_EXEC_TIMEOUT));
    bpris.slaveTimeConstraint = TimeConstraint.valueOf(rs.getString(COL_SLAVE_TIME_CONSTRAINT));
    bpris.maxParallelism = rs.getInt(COL_MAX_PARALLELISM);
    bpris.slaveExecutionPeriod = SlaveExecutionPeriod.valueOf(rs.getString(COL_SLAVE_EXECUTION_PERIOD));
    bpris.guiRepresentationData = rs.getString(COL_GUI_REPRESENTATION_DATA);
    bpris.timeWindowDefinition = TimeWindowDefinition.valueOf(rs.getString(COL_TIME_WINDOW_DEFINITION));
  }

  
  public long getBatchProcessId() {
    return batchProcessId;
  }

  
  public void setBatchProcessId(long batchProcessId) {
    this.batchProcessId = batchProcessId;
  }

  public InputGeneratorType getInputGeneratorType() {
    return inputGeneratorType;
  }
  
  public void setInputGeneratorType(InputGeneratorType inputGeneratorType) {
    this.inputGeneratorType = inputGeneratorType;
  }

  public String getConstantInput() {
    return constantInput;
  }

  public void setConstantInput(String constantInput) {
    this.constantInput = constantInput;
  }

  public String getInputStorable() {
    return inputStorable;
  }

  
  public void setInputStorable(String inputStorable) {
    this.inputStorable = inputStorable;
  }

  
  public String getInputQuery() {
    return inputQuery;
  }

  
  public void setInputQuery(String inputQuery) {
    this.inputQuery = inputQuery;
  }

  
  public String getInputSortCriteria() {
    return inputSortCriteria;
  }

  
  public void setInputSortCriteria(String inputSortCriteria) {
    this.inputSortCriteria = inputSortCriteria;
  }

  
  public Integer getTotal() {
    return total;
  }

  
  public void setTotal(Integer total) {
    this.total = total;
  }

  
  public SchedulingData getMasterSchedulingData() {
    return masterSchedulingData;
  }

  public void setMasterSchedulingData(SchedulingData masterSchedulingData) {
    this.masterSchedulingData = masterSchedulingData;
  }
  
  public AbsRelTime getSlaveOrderExecTimeout() {
    return slaveOrderExecTimeout;
  }

  
  public void setSlaveOrderExecTimeout(AbsRelTime slaveOrderExecTimeout) {
    this.slaveOrderExecTimeout = slaveOrderExecTimeout;
  }

  
  public AbsRelTime getSlaveWorkflowExecTimeout() {
    return slaveWorkflowExecTimeout;
  }

  
  public void setSlaveWorkflowExecTimeout(AbsRelTime slaveWorkflowExecTimeout) {
    this.slaveWorkflowExecTimeout = slaveWorkflowExecTimeout;
  }

  
  public TimeConstraint getSlaveTimeConstraint() {
    return slaveTimeConstraint;
  }

  
  public void setSlaveTimeConstraint(TimeConstraint slaveTimeConstraint) {
    this.slaveTimeConstraint = slaveTimeConstraint;
  }

  
  public Integer getMaxParallelism() {
    return maxParallelism;
  }

  
  public void setMaxParallelism(Integer maxParallelism) {
    this.maxParallelism = maxParallelism;
  }
  
  
  public SlaveExecutionPeriod getSlaveExecutionPeriod() {
    return slaveExecutionPeriod;
  }
  
  public void setSlaveExecutionPeriod(SlaveExecutionPeriod slaveExecutionPeriod) {
    this.slaveExecutionPeriod = slaveExecutionPeriod;
  }
  
  public String getGuiRepresentationData() {
    return guiRepresentationData;
  }
  
  public void setGuiRepresentationData(String guiRepresentationData) {
    this.guiRepresentationData = guiRepresentationData;
  }
  
  public TimeWindowDefinition getTimeWindowDefinition() {
    return timeWindowDefinition;
  }
  
  public void setTimeWindowDefinition(TimeWindowDefinition timeWindowDefinition) {
    this.timeWindowDefinition = timeWindowDefinition;
  }
}
