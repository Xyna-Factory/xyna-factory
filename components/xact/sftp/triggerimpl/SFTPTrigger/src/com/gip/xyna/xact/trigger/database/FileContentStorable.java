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
package com.gip.xyna.xact.trigger.database;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = FileContentStorable.TABLE_NAME, primaryKey = FileContentStorable.COL_ID)
public class FileContentStorable extends Storable<FileContentStorable> {

  private static final long serialVersionUID = 9077661986329920786L;

  public final static String TABLE_NAME = "sftpfilecontent";
  public final static String COL_ID = "id";
  public final static String COL_FILE = "file";
  public final static String COL_CONTENT = "content";
  public final static String COL_FILETYPE = "filetype";
  

  @Column(name = COL_ID)
  private long id;


  // @Column(name = COL_NAME, index = IndexType.MULTIPLE)
  @Column(name = COL_FILE)
  private String file;

  @Column(name = COL_CONTENT)
  private String content;

  @Column(name = COL_FILETYPE)
  private String filetype;


  public FileContentStorable() {
  }


  public FileContentStorable(String file, String content, String filetype) {
    this.file = file;
    this.content = content;
    this.filetype = filetype;
    try {
      this.id = IDGenerator.getInstance().getUniqueId();
    }
    catch (XynaException e) {
      throw new RuntimeException("Could not generate unique id", e);
    }
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public long getId() {
    return id;
  }


  public void setIdentifier(long id) {
    this.id = id;
  }


  public String getFile() {
    return file;
  }


  public void setFile(String file) {
    this.file = file;
  }

  public String getFileType() {
    return filetype;
  }


  public void setFileType(String filetype) {
    this.filetype = filetype;
  }


  public String getContent() {
    return content;
  }


  public void setContent(String content) {
    this.content = content;
  }


  public static final ResultSetReader<FileContentStorable> reader = new FileContentStorableResultSetReader();


  @Override
  public ResultSetReader<? extends FileContentStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends FileContentStorable> void setAllFieldsFromData(U dataIn) {
    FileContentStorable data = dataIn;
    this.id = data.id;
    this.file = data.file;
    this.content = data.content;
    this.filetype = data.filetype;
  }


  private static class FileContentStorableResultSetReader implements ResultSetReader<FileContentStorable> {

    public FileContentStorable read(ResultSet rs) throws SQLException {
      FileContentStorable result = new FileContentStorable();
      result.id = rs.getLong(COL_ID);
      result.file = rs.getString(COL_FILE);
      result.content = rs.getString(COL_CONTENT);
      result.filetype = rs.getString(COL_FILETYPE);
      return result;
    }
  }
}
