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
package com.gip.xyna;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = BootCountStorable.COL_PK, tableName = BootCountStorable.TABLENAME)
public class BootCountStorable extends Storable<BootCountStorable> {

  private static final long serialVersionUID = 1L;
  public static final String COL_PK = "id";
  public static final String TABLENAME = "bootcount";
  private static final String COL_BOOTCOUNT = "bootcount";

  private static final ResultSetReader<? extends BootCountStorable> reader = new ResultSetReader<BootCountStorable>() {

    public BootCountStorable read(ResultSet rs) throws SQLException {
      BootCountStorable b = new BootCountStorable();
      b.id = rs.getInt(COL_PK);
      b.bootcount = rs.getInt(COL_BOOTCOUNT);
      return b;
    }
  };

  @Column(name = COL_PK)
  private int id;

  @Column(name = COL_BOOTCOUNT)
  private int bootcount;


  public int getId() {
    return id;
  }


  public void setId(int id) {
    this.id = id;
  }


  public int getBootcount() {
    return bootcount;
  }


  public void setBootcount(int bootcount) {
    this.bootcount = bootcount;
  }


  @Override
  public ResultSetReader<? extends BootCountStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends BootCountStorable> void setAllFieldsFromData(U data) {
    this.id = data.getId();
    this.bootcount = data.getBootcount();
  }

}
