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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;


public enum BatchProcessColumn {
  BATCH_PROCESS_ID(BatchProcessArchiveStorable.COL_ORDER_ID, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning, new String[] {"id"}),
  LABEL(BatchProcessArchiveStorable.COL_LABEL, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning, new String[] {"name"}),
  APPLICATION(BatchProcessArchiveStorable.COL_APPLICATION, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning),
  VERSION(BatchProcessArchiveStorable.COL_VERSION, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning),
  WORKSPACE(BatchProcessArchiveStorable.COL_WORKSPACE, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning, new String[] {"workspacename"}),
  COMPONENT(BatchProcessArchiveStorable.COL_COMPONENT, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning),
  SLAVE_ORDER_TYPE(BatchProcessArchiveStorable.COL_SLAVE_ORDER_TYPE, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning, new String[] {"slaveordertype"}),
  CUSTOM0(BatchProcessArchiveStorable.COL_CUSTOM0, BatchProcessTable.Archive, new String[] {"globalcustomfield0"}),
  CUSTOM1(BatchProcessArchiveStorable.COL_CUSTOM1, BatchProcessTable.Archive, new String[] {"globalcustomfield1"}),
  CUSTOM2(BatchProcessArchiveStorable.COL_CUSTOM2, BatchProcessTable.Archive, new String[] {"globalcustomfield2"}),
  CUSTOM3(BatchProcessArchiveStorable.COL_CUSTOM3, BatchProcessTable.Archive, new String[] {"globalcustomfield3"}),
  CUSTOM4(BatchProcessArchiveStorable.COL_CUSTOM4, BatchProcessTable.Archive, new String[] {"globalcustomfield4"}),
  CUSTOM5(BatchProcessArchiveStorable.COL_CUSTOM5, BatchProcessTable.Archive, new String[] {"globalcustomfield5"}),
  CUSTOM6(BatchProcessArchiveStorable.COL_CUSTOM6, BatchProcessTable.Archive, new String[] {"globalcustomfield6"}),
  CUSTOM7(BatchProcessArchiveStorable.COL_CUSTOM7, BatchProcessTable.Archive, new String[] {"globalcustomfield7"}),
  CUSTOM8(BatchProcessArchiveStorable.COL_CUSTOM8, BatchProcessTable.Archive, new String[] {"globalcustomfield8"}),
  CUSTOM9(BatchProcessArchiveStorable.COL_CUSTOM9, BatchProcessTable.Archive, new String[] {"globalcustomfield9"}),
  TOTAL(BatchProcessArchiveStorable.COL_TOTAL, BatchProcessTable.Archive, BatchProcessTable.Restart, new String[] {"maxexecutions"}),
  STARTED(BatchProcessArchiveStorable.COL_FINISHED + " + " + BatchProcessArchiveStorable.COL_FAILED, BatchProcessTable.Archive, BatchProcessTable.Runtime, new String[] {"numberofstartedorders"}),
  RUNNING(BatchProcessRuntimeInformationStorable.COL_RUNNING, BatchProcessTable.Runtime, BatchProcessTable.Archive, new String[] {"numberofrunningorders"}),
  FINISHED(BatchProcessArchiveStorable.COL_FINISHED, BatchProcessTable.Archive, BatchProcessTable.Runtime, new String[] {"numberoffinishedorders"}),
  FAILED(BatchProcessArchiveStorable.COL_FAILED, BatchProcessTable.Archive, BatchProcessTable.Runtime, new String[] {"numberoffailedorders"}),
  CANCELED(BatchProcessArchiveStorable.COL_CANCELED, BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning, new String[] {"numberofcancelledorders"}),
  STATUS(BatchProcessArchiveStorable.COL_ORDER_STATUS,  BatchProcessTable.Archive, BatchProcessTable.ArchiveRunning,  new String[] {"status"} ),
  CONSTANT_INPUT(BatchProcessRestartInformationStorable.COL_CONSTANT_INPUT, BatchProcessTable.Restart, new String[] {"constantinputdata"})
  ;

  private String columnName;
  private String[] aliases;
  private BatchProcessTable table; //Tabelle in der die Spalte mit dem columnName vorkommt
  private BatchProcessTable additionalTable;   //zusätzliche Tabelle in der auch nach der colum gefiltert werden soll (evtl. mit anderem Spaltennamen)

  private BatchProcessColumn(String columnName, BatchProcessTable table) {
    this(columnName, table, null, new String[0]);
  }
  
  private BatchProcessColumn(String columnName, BatchProcessTable table, BatchProcessTable additionalTable) {
    this(columnName, table, additionalTable, new String[0]);
  }
  
  private BatchProcessColumn(String columnName, BatchProcessTable table, String[] aliases) {
    this(columnName, table, null, aliases);
  }

  private BatchProcessColumn(String columnName, BatchProcessTable table, BatchProcessTable additionalTable, String[] aliases) {
    this.columnName = columnName;
    this.table = table;
    this.additionalTable = additionalTable;
    this.aliases = aliases;
  }
  
  public String getColumnName() {
    return columnName;
  }
  
  public String[] getAliases() {
    return aliases;
  }
  
  public BatchProcessTable getTable() {
    return table;
  }
  
  public BatchProcessTable getAdditionalTable() {
    return additionalTable;
  }
  
  public static BatchProcessColumn getBatchProcessColumnByName(String columnName) {
    for (BatchProcessColumn type : values()) {
      if (type.columnName.equals(columnName)) {
        return type;
      } else {
        for (String alias : type.getAliases()) {
          if (alias.equals(columnName)) {
            return type;
          }
        }
      }
    }
    throw new IllegalArgumentException(columnName);
  }
}
