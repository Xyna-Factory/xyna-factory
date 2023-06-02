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
package com.gip.xyna.xdev.map;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey=TypeMappingEntry.COL_PK, tableName=TypeMappingEntry.TABLENAME)
public class TypeMappingEntry extends Storable<TypeMappingEntry> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "typemapping";
  public static final String COL_PK = "pk";
  static final String COL_ID = "id";
  static final String COL_KEY = "keyv";
  private static final String COL_VALUE = "value";
  
  
  static final ResultSetReader<TypeMappingEntry> reader = new ResultSetReader<TypeMappingEntry>() {

    public TypeMappingEntry read(ResultSet rs) throws SQLException {
      TypeMappingEntry t = new TypeMappingEntry();
      t.id = rs.getString(COL_ID);
      t.pk = rs.getLong(COL_PK);
      t.key = rs.getString(COL_KEY);
      t.value = rs.getString(COL_VALUE);
      return t;
    }
  };

  @Column(name=COL_PK)
  private long pk;
  
  @Column(name=COL_ID)
  private String id;
  
  @Column(name=COL_KEY, size=512)
  private String key;
  
  @Column(name=COL_VALUE, size=512)
  private String value;
  
  public TypeMappingEntry() {
    
  }
  
  public TypeMappingEntry(long pk, String id, String key, String value) {
    this.key = key;
    this.pk = pk;
    this.id = id;
    this.value = value;
  }
  
  @Override
  public Object getPrimaryKey() {
    return pk;
  }

  public long getPk() {
    return pk;
  }
  
  public String getId() {
    return id;
  }
  
  public String getKeyv() {
    return key;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public ResultSetReader<? extends TypeMappingEntry> getReader() {
    return reader;
  }

  @Override
  public <U extends TypeMappingEntry> void setAllFieldsFromData(U u) {
    TypeMappingEntry tme = u;
    pk = tme.pk;
    id = tme.id;
    key = tme.key;
    value = tme.value;
  }

  public void setPk(long pk) {
    this.pk = pk;
  }

}
