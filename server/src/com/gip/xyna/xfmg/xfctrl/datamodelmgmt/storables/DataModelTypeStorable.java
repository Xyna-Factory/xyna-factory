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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


/**
 *
 */
@Persistable(primaryKey = DataModelTypeStorable.COL_NAME, tableName = DataModelTypeStorable.TABLENAME)
public class DataModelTypeStorable extends Storable<DataModelTypeStorable> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "datamodeltype";
  public static final String COL_NAME = "name";
  public static final String COL_FQCLASSNAME = "fqclassname";
  public static final String COL_PARAMETER = "parameter";
  

  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_FQCLASSNAME)
  private String fqClassName;

  @Column(name = COL_PARAMETER)
  private CSVStringList parameter;

  public DataModelTypeStorable() {
  }

  public DataModelTypeStorable(String name) {
    this.name = name;
  }

  public DataModelTypeStorable(String name, String fqClassName, List<String> parameter) {
    this.name = name;
    this.fqClassName = fqClassName;
    this.parameter = new CSVStringList(parameter);
  }

  @Override
  public ResultSetReader<? extends DataModelTypeStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public <U extends DataModelTypeStorable> void setAllFieldsFromData(U data) {
    DataModelTypeStorable dmts = data;
    this.name = dmts.name;
    this.fqClassName = dmts.fqClassName;
    this.parameter = dmts.parameter;
  }
  
  @Override
  public String toString() {
    return "DataModelTypeStorable("+name+","+fqClassName+","+parameter+")";
  }

  
  private static final ResultSetReader<DataModelTypeStorable> reader =
      new ResultSetReader<DataModelTypeStorable>() {

    public DataModelTypeStorable read(ResultSet rs) throws SQLException {
      DataModelTypeStorable dmts = new DataModelTypeStorable();
      dmts.name = rs.getString(COL_NAME);
      dmts.fqClassName = rs.getString(COL_FQCLASSNAME);
      dmts.parameter = CSVStringList.valueOf(rs.getString(COL_PARAMETER));
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

  public List<String> getParameter() {
    return parameter;
  }

  public void setParameter(List<String> parameter) {
    this.parameter = new CSVStringList(parameter);
  }

}
