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
package com.gip.xyna.persistence.xsor;


import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey="count", tableName="dual")
public class CountStorable extends Storable<CountStorable> {

  private static final long serialVersionUID = 2081842362606710916L;
  private static final String COUNT_COLUMN_NAME = "count";
  
  public CountStorable() {
  }
  
  public CountStorable(int count) {
    this();
    this.count = count;
  }
  
  @Column(name=COUNT_COLUMN_NAME)
  private int count;
  
  @Override
  public Object getPrimaryKey() {
    return count;
  }

  @Override
  public ResultSetReader<? extends CountStorable> getReader() {
    return new ResultSetReader<CountStorable>() {

      public CountStorable read(ResultSet rs) throws SQLException {
        return new CountStorable(rs.getInt(COUNT_COLUMN_NAME));
      }
    };
  }

  @Override
  public <U extends CountStorable> void setAllFieldsFromData(U arg0) {
    
  }
  
  public int getCount() {
    return count;
  }

}
