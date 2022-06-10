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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


@Persistable(primaryKey = BatchProcessRuntimeInformationStorable.COL_BATCH_PROCESS_ID, tableName = BatchProcessRuntimeInformationStorable.TABLE_NAME)
public class BatchProcessRuntimeInformationStorable extends Storable<BatchProcessRuntimeInformationStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "bpruntimeinformation";
  
  public static ResultSetReader<? extends BatchProcessRuntimeInformationStorable> reader = new BatchProcessRuntimeInformationStorableReader();
  public static ResultSetReader<Long> idReader = new BatchProcessRuntimeInformationStorableIdReader();

  public static final String COL_BATCH_PROCESS_ID = "batchProcessId"; //ID des Batch Processes
  public static final String COL_NEXT_EXECUTION_TIME = "nextExecutionTime"; //Nächster Zeitpunkt, zu dem frühestens ein Scheduling möglich sein wird; -1 falls die Ausführung zu diesem Zeitpunkt rein durch Kapazitäten beschränkt ist
  public static final String COL_STATE = "state"; //aktueller Status des BatchProcesses
  public static final String COL_RUNNING = "running"; //Anzahl der gerade laufenden Subaufträge
  public static final String COL_FAILED = "failed"; //Anzahl der fehlgeschlagenen Subaufträge
  public static final String COL_FINISHED = "finished"; //Anzahl der erfolgreich fertiggelaufenen Subaufträge
  public static final String COL_LAST_INPUT_ID = "lastInputGeneratorID"; //Irgendeine Form von ID, die dem Batch Process sagt, welche Inputs er bereits verbraucht hat.
  public static final String COL_OPEN_DATA_PLANNING = "openDataPlanning"; //Inputs, die bereits ausgegeben wurden, aber deren Slave noch nicht gebackupt wurde
  public static final String COL_PAUSE_CAUSE = "pauseCause"; //Ursache für das Pausieren des Batch Processes
  
  private transient ReentrantLock lock = new ReentrantLock();
  
  private volatile boolean updateAllowed = true;

  
  public enum BatchProcessState implements StringSerializable<BatchProcessState>{
    RUNNING, PAUSED, CANCELED, TIMEOUT;
    
    
    public BatchProcessState deserializeFromString(String string) {
      return BatchProcessState.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }
  
  
  @Column(name = COL_BATCH_PROCESS_ID, index = IndexType.PRIMARY)
  private long batchProcessId;//ID des Batch Processes
  
  @Column(name = COL_NEXT_EXECUTION_TIME)
  private String nextExecutionTime; //Nächster Zeitpunkt, zu dem frühestens ein Scheduling möglich sein wird; -1 falls die Ausführung zu diesem Zeitpunkt rein durch Kapazitäten beschränkt ist

  @Column(name = COL_STATE)
  private BatchProcessState state; //aktueller Status des BatchProcesses

  @Column(name = COL_RUNNING)
  private int running; //Anzahl der gerade laufenden Subaufträge

  @Column(name = COL_FAILED)
  private int failed; //Anzahl der fehlgeschlagenen Subaufträge

  @Column(name = COL_FINISHED)
  private int finished; //Anzahl der erfolgreich fertiggelaufenen Subaufträge

  @Column(name = COL_LAST_INPUT_ID)
  private String lastInputGeneratorID; //Irgendeine Form von ID, die dem Batch Process sagt, welche Inputs er bereits verbraucht hat.

  @Column(name = COL_OPEN_DATA_PLANNING, size=4000)
  private final StringSerializableList<String> openDataPlanning  //Inputs, die bereits ausgegeben wurden, aber deren Slave noch nicht gebackupt wurde
    = StringSerializableList.separator(String.class);
  
  @Column(name = COL_PAUSE_CAUSE, size=4000)
  private String pauseCause; //Ursache für das Pausieren des Batch Processes
 

  public BatchProcessRuntimeInformationStorable(){
  }

  public BatchProcessRuntimeInformationStorable(long batchProcessId){
    this.batchProcessId = batchProcessId;
    state = BatchProcessState.RUNNING;
  }
  
  public BatchProcessRuntimeInformationStorable(BatchProcessArchiveStorable data) {
    this.batchProcessId = data.getOrderId();
    this.running = 0;
    this.failed = data.getFailed();
    this.finished = data.getFinished();
  }

  public BatchProcessRuntimeInformationStorable(BatchProcessRuntimeInformationStorable data) {
    setAllFieldsFromData(data);
  }

  @Override
  public ResultSetReader<? extends BatchProcessRuntimeInformationStorable> getReader() {
    return reader;
  }

  @Override
  public Long getPrimaryKey() {
    return Long.valueOf(batchProcessId);
  }

  @Override
  public <U extends BatchProcessRuntimeInformationStorable> void setAllFieldsFromData(U data) {
    BatchProcessRuntimeInformationStorable cast = data;
    this.batchProcessId = cast.batchProcessId;
    this.nextExecutionTime = cast.nextExecutionTime;
    this.state = cast.state;
    this.running = cast.running;
    this.failed = cast.failed;
    this.finished = cast.finished;
    this.lastInputGeneratorID = cast.lastInputGeneratorID;
    this.openDataPlanning.setValues(cast.openDataPlanning);
    this.pauseCause = cast.pauseCause;
  }
  
  private static class BatchProcessRuntimeInformationStorableIdReader implements ResultSetReader<Long> {
    public Long read(ResultSet rs) throws SQLException {
      return rs.getLong(COL_BATCH_PROCESS_ID);
    }
  }
  
  private static class BatchProcessRuntimeInformationStorableReader implements ResultSetReader<BatchProcessRuntimeInformationStorable> {
    public BatchProcessRuntimeInformationStorable read(ResultSet rs) throws SQLException {
      BatchProcessRuntimeInformationStorable result = new BatchProcessRuntimeInformationStorable();
      fillByResultset(result, rs);
      return result;
    }
  }
  
  private static void fillByResultset(BatchProcessRuntimeInformationStorable bpris, ResultSet rs) throws SQLException {
    bpris.batchProcessId = rs.getLong(COL_BATCH_PROCESS_ID);
    bpris.nextExecutionTime = rs.getString(COL_NEXT_EXECUTION_TIME);
    bpris.state = BatchProcessState.valueOf(rs.getString(COL_STATE));
    bpris.running = rs.getInt(COL_RUNNING);
    bpris.failed = rs.getInt(COL_FAILED);
    bpris.finished = rs.getInt(COL_FINISHED);
    bpris.lastInputGeneratorID = rs.getString(COL_LAST_INPUT_ID);
    bpris.openDataPlanning.deserializeFromString(rs.getString(COL_OPEN_DATA_PLANNING));
    bpris.pauseCause = rs.getString(COL_PAUSE_CAUSE);
  }

  
  public long getBatchProcessId() {
    return batchProcessId;
  }

  
  public void setBatchProcessId(long batchProcessId) {
    this.batchProcessId = batchProcessId;
  }

  
  public String getNextExecutionTime() {
    return nextExecutionTime;
  }

  
  public void setNextExecutionTime(String nextExecutionTime) {
    this.nextExecutionTime = nextExecutionTime;
  }

  
  public BatchProcessState getState() {
    return state;
  }

  
  public void setState(BatchProcessState state) {
    this.state = state;
  }

  
  public int getRunning() {
    return running;
  }
  
  
  public void setRunning(int running) {
    this.running = running;
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

  
  public void setFinished(int finished) {
    this.finished = finished;
  }

  
  public String getLastInputGeneratorID() {
    return lastInputGeneratorID;
  }

  
  public void setLastInputGeneratorID(String lastInputGeneratorID) {
    this.lastInputGeneratorID = lastInputGeneratorID;
  }

  
  public List<String> getOpenDataPlanning() {
    return openDataPlanning;
  }

  
  public void setOpenDataPlanning(List<String> openDataPlanning) {
    this.openDataPlanning.setValues(openDataPlanning);
  }

  
  public String getPauseCause() {
    return pauseCause;
  }

  
  public void setPauseCause(String pauseCause) {
    this.pauseCause = pauseCause;
  }
  
  
  public ReentrantLock getLock() {
    return lock;
  }


  public boolean isUpdateAllowed() {
    return updateAllowed;
  }

  public void setUpdateAllowed(boolean updateAllowed) {
    this.updateAllowed = updateAllowed;
  }
  
  /**
   * Anzahl der gestarteten Slaves berechnen
   */
  public int getStarted() {
    return running + finished + failed;
  }
  
  
  /**
   * Zählt running um eins runter, falls der Slave bereits gebackupt wurde
   */
  public void decrRunningIfBackuped(OrderInstanceStatus slaveState) {
    //Running nur runterzählen, falls der Slave im Planning keinen Fehler hatte
    //(da running erst beim Backup hochgezählt wird)
    if (slaveState == OrderInstanceStatus.SCHEDULING
          || slaveState == OrderInstanceStatus.RUNNING) {
      running--;
    }
  }

  public void addOpenDataPlanning(String currentInputId) {
    if (!openDataPlanning.contains(currentInputId)) {
      openDataPlanning.add(currentInputId);
    }
  }
  
}
