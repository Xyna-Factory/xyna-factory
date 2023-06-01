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

package com.gip.xyna.xnwh.persistence;



import java.sql.ResultSet;
import java.sql.SQLException;




@Persistable(primaryKey = PersistenceLayerBeanStorable.COLUMN_ID, tableName = PersistenceLayerBeanStorable.TABLE_NAME)
public class PersistenceLayerBeanStorable extends Storable<PersistenceLayerBeanStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "persistencelayers";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_FQ_CLASSNAME = "fullyqualifiedclassname";

  private static final PersistenceLayerConfigReader reader = new PersistenceLayerConfigReader();

  @Column(name = COLUMN_ID)
  private Long id;
  @Column(name = COLUMN_NAME)
  private String name;
  @Column(name = COLUMN_FQ_CLASSNAME, size=100)
  private String fullyQualifiedClassname;


  public PersistenceLayerBeanStorable() {
  }


  public PersistenceLayerBeanStorable(Long id, String name, String fqClassName) {
    this.id = id;
    this.name = name;
    this.fullyQualifiedClassname = fqClassName;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public Long getId() {
    return id;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public String getFullyQualifiedClassName() {
    return fullyQualifiedClassname;
  }


  @Override
  public ResultSetReader<? extends PersistenceLayerBeanStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends PersistenceLayerBeanStorable> void setAllFieldsFromData(U data) {
    PersistenceLayerBeanStorable cast = data;
    id = cast.id;
    name = cast.name;
    fullyQualifiedClassname = cast.fullyQualifiedClassname;
  }


  private static void fillByResultSet(PersistenceLayerBeanStorable plConfig, ResultSet rs) throws SQLException {
    plConfig.id = rs.getLong(COLUMN_ID);
    plConfig.name = rs.getString(COLUMN_NAME);
    plConfig.fullyQualifiedClassname = rs.getString(COLUMN_FQ_CLASSNAME);
  }


  private static class PersistenceLayerConfigReader implements ResultSetReader<PersistenceLayerBeanStorable> {

    public PersistenceLayerBeanStorable read(ResultSet rs) throws SQLException {
      PersistenceLayerBeanStorable plConfig = new PersistenceLayerBeanStorable();
      fillByResultSet(plConfig, rs);
      return plConfig;
    }

  }

}
