/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xfmg.oas.generation.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = OasImportHistoryStorable.COL_UNIQUE_ID, tableName = OasImportHistoryStorable.TABLE_NAME)
public class OasImportHistoryStorable extends Storable<OasImportHistoryStorable> {

  
  public static class OasImportHistoryMultiLineReader implements ResultSetReader<OasImportHistoryStorable> {

    public OasImportHistoryStorable read(ResultSet rs) throws SQLException {
      OasImportHistoryStorable result = new OasImportHistoryStorable();
      result.uniqueIdentifier = rs.getLong(COL_UNIQUE_ID);
      result.importType = rs.getString(COL_IMPORT_TYPE);
      result.importDate = rs.getString(COL_IMPORT_DATE);
      result.fileName = rs.getString(COL_FILE_NAME);
      result.importStatus = rs.getString(COL_IMPORT_STATUS);
      return result;
    }
  }
  
  
  public static class OasImportHistoryDetailsReader implements ResultSetReader<OasImportHistoryStorable> {

    public OasImportHistoryStorable read(ResultSet rs) throws SQLException {
      OasImportHistoryStorable result = new OasImportHistoryStorable();
      result.uniqueIdentifier = rs.getLong(COL_UNIQUE_ID);
      result.importType = rs.getString(COL_IMPORT_TYPE);
      result.importDate = rs.getString(COL_IMPORT_DATE);
      result.fileName = rs.getString(COL_FILE_NAME);
      result.specificationFile = (String) result.readBlobbedJavaObjectFromResultSet(rs, COL_SPECIFICATION_FILE);
      result.importStatus = rs.getString(COL_IMPORT_STATUS);
      result.errorMessage = (String) result.readBlobbedJavaObjectFromResultSet(rs, COL_ERROR_MESSAGE);
      return result;
    }
  }
  
  private static OasImportHistoryMultiLineReader multiLineReader = new OasImportHistoryMultiLineReader();
  private static OasImportHistoryDetailsReader detailsReader = new OasImportHistoryDetailsReader();
  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "oasimporthistory";
  
  public static final String COL_UNIQUE_ID = "uniqueIdentifier";
  public static final String COL_IMPORT_TYPE = "importType";
  public static final String COL_IMPORT_DATE = "importDate";
  public static final String COL_FILE_NAME = "fileName";
  public static final String COL_SPECIFICATION_FILE = "specificationFile";
  public static final String COL_IMPORT_STATUS = "importStatus";
  public static final String COL_ERROR_MESSAGE = "errorMessage";
  
  
  @Column(name = COL_UNIQUE_ID)
  private long uniqueIdentifier;
  
  @Column(name = COL_IMPORT_TYPE)
  private String importType;
  
  @Column(name = COL_IMPORT_DATE)
  private String importDate;
  
  @Column(name = COL_FILE_NAME)
  private String fileName;
  
  @Column(name = COL_SPECIFICATION_FILE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private String specificationFile;
  
  @Column(name = COL_IMPORT_STATUS)
  private String importStatus;
  
  @Column(name = COL_ERROR_MESSAGE, type = ColumnType.BLOBBED_JAVAOBJECT)
  private String errorMessage;

  
  @Override
  public ResultSetReader<? extends OasImportHistoryStorable> getReader() {
    return detailsReader;
  }

  
  public static OasImportHistoryMultiLineReader getOasImportHistoryMultiLineReader() {
    return multiLineReader;
  }

  public static OasImportHistoryDetailsReader getOasImportHistoryDetailsReader() {
    return detailsReader;
  }

  
  @Override
  public Object getPrimaryKey() {
    return uniqueIdentifier;
  }


  @Override
  public <U extends OasImportHistoryStorable> void setAllFieldsFromData(U data) {
    OasImportHistoryStorable cast = data;
    this.uniqueIdentifier = cast.uniqueIdentifier;
    this.importType = cast.importType;
    this.importDate = cast.importDate;
    this.fileName = cast.fileName;
    this.specificationFile = cast.specificationFile;
    this.importStatus = cast.importStatus;
    this.errorMessage = cast.errorMessage;
  }
  
  
  public long getUniqueIdentifier() {
    return uniqueIdentifier;
  }

  
  public void setUniqueIdentifier(long uniqueIdentifier) {
    this.uniqueIdentifier = uniqueIdentifier;
  }

  
  public String getImportType() {
    return importType;
  }

  
  public void setImportType(String importType) {
    this.importType = importType;
  }

  
  public String getImportDate() {
    return importDate;
  }

  
  public void setImportDate(String importDate) {
    this.importDate = importDate;
  }


  public String getFileName() {
    return fileName;
  }

  
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  
  public String getSpecificationFile() {
    return specificationFile;
  }

  
  public void setSpecificationFile(String specificationFile) {
    this.specificationFile = specificationFile;
  }

  
  public String getImportStatus() {
    return importStatus;
  }

  
  public void setImportStatus(String importStatus) {
    this.importStatus = importStatus;
  }

  
  public String getErrorMessage() {
    return errorMessage;
  }

  
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
