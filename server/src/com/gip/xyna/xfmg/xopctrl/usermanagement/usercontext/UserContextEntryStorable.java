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

package com.gip.xyna.xfmg.xopctrl.usermanagement.usercontext;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = UserContextEntryStorable.COL_ID, tableName = UserContextEntryStorable.TABLENAME)
public class UserContextEntryStorable extends Storable<UserContextEntryStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "usercontextentry";
  public static final String COL_ID = "ID";
  public static final String COL_USERNAME = "username";
  public static final String COL_KEY = "key";
  public static final String COL_VALUE = "value";


  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_USERNAME)
  private String userName;

  @Column(name = COL_KEY)
  private String key;

  @Column(name = COL_VALUE, size=Integer.MAX_VALUE)
  private String value;


  public UserContextEntryStorable() {
  }

  public UserContextEntryStorable(long id, String username, String key, String value) {
    this.id = id;
    this.userName = username;
    this.key = key;
    this.value = value;
  }


  public static final ResultSetReader<UserContextEntryStorable> reader =
      new ResultSetReader<UserContextEntryStorable>() {

        public UserContextEntryStorable read(ResultSet rs) throws SQLException {
          UserContextEntryStorable ret = new UserContextEntryStorable();
          ret.id = rs.getLong(COL_ID);
          ret.userName = rs.getString(COL_USERNAME);
          ret.key = rs.getString(COL_KEY);
          ret.value = rs.getString(COL_VALUE);
          return ret;
        }
      };

  @Override
  public ResultSetReader<? extends UserContextEntryStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends UserContextEntryStorable> void setAllFieldsFromData(U data) {
    UserContextEntryStorable cast = data;
    id = cast.id;
    userName = cast.userName;
    key = cast.key;
    value = cast.value;
  }


  public long getId() {
    return id;
  }


  public String getUserName() {
    return userName;
  }


  public String getKey() {
    return key;
  }


  public String getValue() {
    return value;
  }


  public void setValue(String value) {
    this.value = value;
  }

}
