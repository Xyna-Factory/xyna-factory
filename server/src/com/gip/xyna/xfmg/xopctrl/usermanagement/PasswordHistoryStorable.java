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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = PasswordHistoryStorable.COL_ID, tableName = PasswordHistoryStorable.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
public class PasswordHistoryStorable extends Storable<PasswordHistoryStorable>{

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "passwordhistory";
  public static final String COL_ID = "id";
  public static final String COL_USERNAME = "userName";
  public static final String COL_PASSWORD = "password";
  public static final String COL_CHANGEDATE = "changeDate";
  public static final String COL_PASSWORD_INDEX = "passwordIndex";
  

  @Column(name = COL_ID)
  private String id;
  @Column(name = COL_USERNAME, size = 50) //gleiche Größe wie bei User
  private String userName;
  @Column(name = COL_PASSWORD, size = 100) //gleiche Größe wie bei User
  private String password;
  @Column(name = COL_CHANGEDATE)
  private long changeDate;
  @Column(name = COL_PASSWORD_INDEX)
  private long passwordIndex;
  
  
  
  public PasswordHistoryStorable() {
  }

  public PasswordHistoryStorable(String userName, String password, long changeDate, long passwordIndex) {
    this.id = userName + "#" + passwordIndex;
    this.userName = userName;
    this.password = password;
    this.changeDate = changeDate;
    this.passwordIndex = passwordIndex;
  }


  public static final ResultSetReader<PasswordHistoryStorable> reader = new ResultSetReader<PasswordHistoryStorable>() {

    public PasswordHistoryStorable read(ResultSet rs) throws SQLException {
      PasswordHistoryStorable ret = new PasswordHistoryStorable();
      ret.id = rs.getString(COL_ID);
      ret.userName = rs.getString(COL_USERNAME);
      ret.password = rs.getString(COL_PASSWORD);
      ret.changeDate = rs.getLong(COL_CHANGEDATE);
      ret.passwordIndex = rs.getLong(COL_PASSWORD_INDEX);
      return ret;
    }
  };
  
  @Override
  public ResultSetReader<? extends PasswordHistoryStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends PasswordHistoryStorable> void setAllFieldsFromData(U data) {
    PasswordHistoryStorable cast = data;
    id = cast.id;
    userName = cast.userName;
    password = cast.password;
    changeDate = cast.changeDate;
    passwordIndex = cast.passwordIndex;
  }

  
  public String getId() {
    return id;
  }
  
  
  public String getUserName() {
    return userName;
  }
  
  
  public String getPassword() {
    return password;
  }
  
  
  public long getChangeDate() {
    return changeDate;
  }
  
  
  public long getPasswordIndex() {
    return passwordIndex;
  }
}
