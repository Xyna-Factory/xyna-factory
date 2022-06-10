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
package com.gip.xyna.update;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = UpdateHistoryStorable.TABLE_NAME, primaryKey = UpdateHistoryStorable.COL_ID)
public class UpdateHistoryStorable extends Storable<UpdateHistoryStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "updatehistory";

  public static final String COL_ID = "id";
  public static final String COL_VERSION = "version";
  public static final String COL_TIME = "updatetime";
  public static final String COL_SUCCESS = "success";
  public static final String COL_EXCEPTION = "exception";
  public static final String COL_UPDATE_TYPE = "updatetype";

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_VERSION)
  private String version;

  @Column(name = COL_TIME)
  private long updatetime;

  @Column(name = COL_SUCCESS)
  private boolean success;

  @Column(name = COL_EXCEPTION, size = 4000)
  private String exception;
  
  @Column(name = COL_UPDATE_TYPE)
  private String updatetype;

  final static ResultSetReader<? extends UpdateHistoryStorable> reader = new ResultSetReader<UpdateHistoryStorable>() {

    public UpdateHistoryStorable read(ResultSet rs) throws SQLException {
      long id = rs.getLong(COL_ID);
      String version = rs.getString(COL_VERSION);
      long time = rs.getLong(COL_TIME);
      boolean success = rs.getBoolean(COL_SUCCESS);
      String exception = rs.getString(COL_EXCEPTION);
      String updatetype = rs.getString(COL_UPDATE_TYPE);
      return new UpdateHistoryStorable(id, version, time, success, exception, updatetype);
    }

  };

  public UpdateHistoryStorable() {
    
  }

  public UpdateHistoryStorable(long id, String version, long time, boolean success, String exception, String updatetype) {
    this.id = id;
    this.version = version;
    this.updatetime = time;
    this.success = success;
    this.exception = exception;
    this.updatetype = updatetype;
  }


  @Override
  public ResultSetReader<? extends UpdateHistoryStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends UpdateHistoryStorable> void setAllFieldsFromData(U data) {
    id = data.getId();
    version = data.getVersion();
    updatetime = data.getUpdateTime();
    success = data.getSuccess();
    exception = data.getException();
    updatetype = data.getUpdatetype();
  }


  public long getId() {
    return id;
  }


  public String getVersion() {
    return version;
  }


  public long getUpdateTime() {
    return updatetime;
  }


  public boolean getSuccess() {
    return success;
  }


  public String getException() {
    return exception;
  }

  public String getUpdatetype() {
    return updatetype;
  }

}
