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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = OrderInputSourceTypeStorable.COL_NAME, tableName = OrderInputSourceTypeStorable.TABLENAME)
public class OrderInputSourceTypeStorable extends Storable<OrderInputSourceTypeStorable> {

  private static final long serialVersionUID = 1L;
  
  public static final String TABLENAME = "orderinputsourcetype";
  public static final String COL_NAME = "name";
  public static final String COL_FQCLASSNAME = "fqclassname";
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_FQCLASSNAME)
  private String fqClassName;


  public OrderInputSourceTypeStorable(){
  }
  
  public OrderInputSourceTypeStorable(String name) {
    this.name = name;
  }

  public OrderInputSourceTypeStorable(String name, String fqClassName) {
    this.name = name;
    this.fqClassName = fqClassName;
  }

  @Override
  public ResultSetReader<? extends OrderInputSourceTypeStorable> getReader() {
    return reader;
  }
  
  @Override
  public Object getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends OrderInputSourceTypeStorable> void setAllFieldsFromData(U data) {
    OrderInputSourceTypeStorable cast = data;
    this.name = cast.name;
    this.fqClassName = cast.fqClassName;
  }
  
  
  private static final ResultSetReader<OrderInputSourceTypeStorable> reader =
      new ResultSetReader<OrderInputSourceTypeStorable>() {

    public OrderInputSourceTypeStorable read(ResultSet rs) throws SQLException {
      OrderInputSourceTypeStorable oigts = new OrderInputSourceTypeStorable();
      oigts.name = rs.getString(COL_NAME);
      oigts.fqClassName = rs.getString(COL_FQCLASSNAME);
      return oigts;
    }

  };
  
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFqclassname() {
    return fqClassName;
  }

  public void setFqclassname(String fqClassName) {
    this.fqClassName = fqClassName;
  }

}
