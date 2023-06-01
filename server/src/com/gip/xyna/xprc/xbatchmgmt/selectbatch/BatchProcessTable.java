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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessCustomizationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;


public enum BatchProcessTable {
  //abgeschlossene Aufträge aus der Tabelle BatchProcessArchive
  Archive(BatchProcessArchiveStorable.TABLE_NAME, BatchProcessArchiveStorable.COL_ORDER_ID, BatchProcessArchiveStorable.idReader) {
    @Override
    public String convertColumnName(String columnName) {
      if (columnName.equals(BatchProcessRuntimeInformationStorable.COL_RUNNING)) {
        return "0";  //running gibt es im Archive nicht als Spalte, aber für abgeschlossene wäre sie immer "0"
      }
      
      return columnName;
    }
  },

  //laufende Aufträge aus der Tabelle BatchProcessArchive
  ArchiveRunning(BatchProcessArchiveStorable.TABLE_NAME, BatchProcessArchiveStorable.COL_ORDER_ID, BatchProcessArchiveStorable.idReader),
  
  //Tabelle BPRestartInformation
  Restart(BatchProcessRestartInformationStorable.TABLE_NAME, BatchProcessRestartInformationStorable.COL_BATCH_PROCESS_ID, BatchProcessRestartInformationStorable.idReader),
  
  //Tabelle BPRuntimeInformation
  Runtime(BatchProcessRuntimeInformationStorable.TABLE_NAME, BatchProcessRuntimeInformationStorable.COL_BATCH_PROCESS_ID, BatchProcessRuntimeInformationStorable.idReader){
    @Override
    public String convertColumnName(String columnName) {
      if (columnName.equals(BatchProcessArchiveStorable.COL_FINISHED + " + " + BatchProcessArchiveStorable.COL_FAILED)) {
        return BatchProcessRuntimeInformationStorable.COL_RUNNING + " + " 
               + BatchProcessRuntimeInformationStorable.COL_FINISHED + " + " 
               + BatchProcessRuntimeInformationStorable.COL_FAILED;
      }
      
      return columnName;
    }
  },
  
  //Tabelle BPCustomization
  Custom(BatchProcessCustomizationStorable.TABLE_NAME, BatchProcessCustomizationStorable.COL_BATCH_PROCESS_ID, BatchProcessCustomizationStorable.idReader) {
    @Override
    public String convertColumnName(String columnName) {
      return columnName;
    }
  };
  
  private String tableName; //Name der Tabelle
  private String batchProcessIdColumn; //Name Spalten mit der BatchProcessId
  private ResultSetReader<Long> idReader; //Reader, um nur die BatchProcessId auszulesen

  
  private BatchProcessTable(String tableName, String batchProcessIdColumn, ResultSetReader<Long> idReader) {
    this.tableName = tableName;
    this.batchProcessIdColumn = batchProcessIdColumn;
    this.idReader = idReader;
  }
  
  /**
   * Konvertiert den übergebenen columName in den entsprechenden Spaltennamen der Tabelle
   * @param columnName
   * @return
   */
  public String convertColumnName(String columnName) {
    return columnName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getBatchProcessIdColumn() {
    return batchProcessIdColumn;
  }

  public ResultSetReader<Long> getIdReader() {
    return idReader;
  }

}
