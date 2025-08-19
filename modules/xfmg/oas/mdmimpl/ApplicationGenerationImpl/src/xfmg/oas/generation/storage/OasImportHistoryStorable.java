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

  private static OasImportHistoryStorableReader reader = new OasImportHistoryStorableReader();
  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "oasimporthistory";
  
  public static final String COL_UNIQUE_ID = "uniqueIdentifier";
  public static final String COL_TYPE = "type";
  public static final String COL_DATE = "date";
  public static final String COL_FILE_NAME = "fileName";
  public static final String COL_SPECIFICATION_FILE = "specificationFile";
  public static final String COL_IMPORT_STATUS = "importStatus";
  public static final String COL_ERROR_MESSAGE = "errorMessage";
  
  
  @Column(name = COL_UNIQUE_ID)
  private long uniqueIdentifier;
  
  @Column(name = COL_TYPE)
  private String type;
  
  @Column(name = COL_DATE)
  private String date;
  
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
    return reader;
  }

  
  public static OasImportHistoryStorableReader getOasImportHistoryStorableReader() {
    return reader;
  }

  
  private static class OasImportHistoryStorableReader implements ResultSetReader<OasImportHistoryStorable> {

    public OasImportHistoryStorable read(ResultSet rs) throws SQLException {
      OasImportHistoryStorable result = new OasImportHistoryStorable();
      result.uniqueIdentifier = rs.getLong(COL_UNIQUE_ID);
      result.type = rs.getString(COL_TYPE);
      result.date = rs.getString(COL_DATE);
      result.fileName = rs.getString(COL_FILE_NAME);
      result.specificationFile = (String) result.readBlobbedJavaObjectFromResultSet(rs, COL_SPECIFICATION_FILE);
      result.importStatus = rs.getString(COL_IMPORT_STATUS);
      result.errorMessage = (String) result.readBlobbedJavaObjectFromResultSet(rs, COL_ERROR_MESSAGE);
      return result;
    }
  }


  @Override
  public Object getPrimaryKey() {
    return uniqueIdentifier;
  }


  @Override
  public <U extends OasImportHistoryStorable> void setAllFieldsFromData(U data) {
    OasImportHistoryStorable cast = data;
    this.uniqueIdentifier = cast.uniqueIdentifier;
    this.type = cast.type;
    this.date = cast.date;
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


  public String getType() {
    return type;
  }

  
  public void setType(String type) {
    this.type = type;
  }

  
  public String getDate() {
    return date;
  }

  
  public void setDate(String date) {
    this.date = date;
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
