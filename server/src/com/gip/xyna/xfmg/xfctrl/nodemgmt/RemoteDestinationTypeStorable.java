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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


/**
 *
 */
@Persistable(primaryKey = RemoteDestinationTypeStorable.COL_NAME, tableName = RemoteDestinationTypeStorable.TABLENAME)
public class RemoteDestinationTypeStorable extends Storable<RemoteDestinationTypeStorable> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "remotedestinationtype";
  public static final String COL_NAME = "name";
  public static final String COL_FQCLASSNAME = "fqclassname";
  

  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_FQCLASSNAME)
  private String fqClassName;


  public RemoteDestinationTypeStorable() {
  }

  public RemoteDestinationTypeStorable(String name) {
    this.name = name;
  }

  public RemoteDestinationTypeStorable(String name, String fqClassName) {
    this.name = name;
    this.fqClassName = fqClassName;
  }

  @Override
  public ResultSetReader<? extends RemoteDestinationTypeStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public <U extends RemoteDestinationTypeStorable> void setAllFieldsFromData(U data) {
    RemoteDestinationTypeStorable dmts = data;
    this.name = dmts.name;
    this.fqClassName = dmts.fqClassName;
  }
  
  @Override
  public String toString() {
    return "DataModelTypeStorable("+name+","+fqClassName+")";
  }

  
  private static final ResultSetReader<RemoteDestinationTypeStorable> reader =
      new ResultSetReader<RemoteDestinationTypeStorable>() {

    public RemoteDestinationTypeStorable read(ResultSet rs) throws SQLException {
      RemoteDestinationTypeStorable dmts = new RemoteDestinationTypeStorable();
      dmts.name = rs.getString(COL_NAME);
      dmts.fqClassName = rs.getString(COL_FQCLASSNAME);
      return dmts;
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
