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

package com.gip.xyna.xnwh.persistence.memory;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(tableName = TestStorable.TABLE_NAME, primaryKey = TestStorable.COL_ID)
public class TestStorable extends Storable<TestStorable> {


  private static final long serialVersionUID = 5739549583513539516L;

  static ResultSetReader<TestStorable> reader = new TestStorableResultSetReader();

  public static final String COL_ID = "id";
  public static final String COL_TRIVIAL_ID = "trivialId";
  public static final String COL_STRINGCOL = "stringcol";

  public static final String TABLE_NAME = "testStorable";


  @Column(name = COL_ID, index = IndexType.PRIMARY)
  private Long id;

  @Column(name = COL_TRIVIAL_ID, index = IndexType.MULTIPLE)
  private long trivialId;
  
  @Column(name = COL_STRINGCOL, index = IndexType.MULTIPLE)
  private String stringcol;

  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public TestStorable() {
  }


  public TestStorable(long id) {
    this.id = id;
  }


  public TestStorable(long id, long trivialId) {
    this.id = id;
    this.trivialId = trivialId;
  }


  public TestStorable(long id, long trivialId, String stringcol) {
    this.id = id;
    this.trivialId = trivialId;
    this.stringcol = stringcol;
  }


  private static class TestStorableResultSetReader implements ResultSetReader<TestStorable> {

    public TestStorable read(ResultSet rs) throws SQLException {
      TestStorable result = new TestStorable();
      fillByResultSet(rs, result);
      return result;
    }

  }


  private static void fillByResultSet(ResultSet rs, TestStorable target) throws SQLException {
    target.id = rs.getLong(COL_ID);
    target.trivialId = rs.getLong(COL_TRIVIAL_ID);
    target.stringcol = rs.getString(COL_STRINGCOL);
  }


  @Override
  public ResultSetReader<? extends TestStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends TestStorable> void setAllFieldsFromData(U data2) {
    TestStorable data = data2;
    this.id = data.id;
    this.trivialId = data.trivialId;
    this.stringcol = data.stringcol;
  }


  public Long getId() {
    return id;
  }


  public long getTrivialId() {
    return trivialId;
  }


  final void setId(long id) {
    this.id = id;
  }


  final void setTrivialId(long trivialId) {
    this.trivialId = trivialId;
  }
  
  public String getStringcol() {
    return stringcol;
  }
  
  public void setStringcol(String stringcol) {
    this.stringcol = stringcol;
  }

}
