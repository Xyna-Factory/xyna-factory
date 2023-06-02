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
package com.gip.xyna.xact.trigger;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = OIDNameMappingStorable.COL_ID, tableName = OIDNameMappingStorable.TABLE_NAME)
public class OIDNameMappingStorable extends Storable<OIDNameMappingStorable> {


  private static final long serialVersionUID = 6012578060607019027L;

  public static final String TABLE_NAME = "oidNameMapping";

  public static final String COL_ID = "id";
  public static final String COL_OBJECT_NAME = "objectName";


  @Column(name = COL_ID, size = 256)
  private String id;
  @Column(name = COL_OBJECT_NAME, size = 512, index = IndexType.UNIQUE)
  private String objectName;


  public OIDNameMappingStorable() {
    id = null;
    objectName = null;
  }


  public OIDNameMappingStorable(String oid) {
    id = oid;
    objectName = null;
  }


  public String getId() {
    return id;
  }


  public String getObjectName() {
    return objectName;
  }


  public void setId(String oid) {
    id = oid;
  }


  public void setObjectName(String name) {
    objectName = name;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  protected static void fillByResultSet(OIDNameMappingStorable onm, ResultSet rs) throws SQLException {
    onm.id = rs.getString(COL_ID);
    onm.objectName = rs.getString(COL_OBJECT_NAME);
  }


  private static class OIDNameMappingReader implements ResultSetReader<OIDNameMappingStorable> {

    public OIDNameMappingStorable read(ResultSet rs) throws SQLException {
      OIDNameMappingStorable onm = new OIDNameMappingStorable();
      fillByResultSet(onm, rs);
      return onm;
    }

  }


  private static OIDNameMappingReader reader = new OIDNameMappingReader();


  @Override
  public ResultSetReader<? extends OIDNameMappingStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends OIDNameMappingStorable> void setAllFieldsFromData(U data) {
    OIDNameMappingStorable cast = data;
    id = cast.id;
    objectName = cast.objectName;
  }
}
