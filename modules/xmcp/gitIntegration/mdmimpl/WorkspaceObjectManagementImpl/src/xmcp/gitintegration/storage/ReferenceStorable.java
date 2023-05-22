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
package xmcp.gitintegration.storage;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = ReferenceStorable.COL_INDEX, tableName = ReferenceStorable.TABLE_NAME)
public class ReferenceStorable extends Storable<ReferenceStorable> {

  private static final long serialVersionUID = -1L;

  public static final String TABLE_NAME = "workspacediffreference";

  public static final String COL_INDEX = "index";
  public static final String COL_PATH = "path";
  public static final String COL_REFTYPE = "reftype";
  public static final String COL_OBJECTTYPE = "objecttype";
  public static final String COL_WORKSPACE_REVISION = "revision";
  public static final String COL_OBJECTNAME = "objectname";

  @Column(name = COL_INDEX)
  private String index;

  @Column(name = COL_PATH)
  private String path;

  @Column(name = COL_REFTYPE)
  private String reftype;
  
  @Column(name = COL_OBJECTTYPE)
  private String objecttype;

  @Column(name = COL_WORKSPACE_REVISION)
  private Long revision;

  @Column(name = COL_OBJECTNAME)
  private String objectName;


  public String getIndex() {
    return index;
  }


  public void setIndex(String index) {
    this.index = index;
  }


  public String getPath() {
    return path;
  }


  public void setPath(String path) {
    this.path = path;
  }


  public String getReftype() {
    return reftype;
  }


  public void setReftype(String reftype) {
    this.reftype = reftype;
  }

  public String getObjecttype() {
    return objecttype;
  }
  
  public void setObjecttype(String objecttype) {
    this.objecttype = objecttype;
  }

  public Long getWorkspace() {
    return revision;
  }


  public void setWorkspace(Long workspace) {
    this.revision = workspace;
  }


  public String getObjectName() {
    return objectName;
  }


  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }


  private static ReferenceStorableReader reader = new ReferenceStorableReader();


  @Override
  public ResultSetReader<? extends ReferenceStorable> getReader() {
    return reader;
  }


  private static class ReferenceStorableReader implements ResultSetReader<ReferenceStorable> {

    @Override
    public ReferenceStorable read(ResultSet rs) throws SQLException {
      ReferenceStorable result = new ReferenceStorable();
      result.setIndex(rs.getString(COL_INDEX));
      result.setObjectName(rs.getString(COL_OBJECTNAME));
      result.setPath(rs.getString(COL_PATH));
      result.setReftype(rs.getString(COL_REFTYPE));
      result.setObjecttype(rs.getString(COL_OBJECTTYPE));
      result.setWorkspace(rs.getLong(COL_WORKSPACE_REVISION));
      return result;
    }

  }


  @Override
  public Object getPrimaryKey() {
    return index;
  }


  @Override
  public <U extends ReferenceStorable> void setAllFieldsFromData(U data) {
    ReferenceStorable cast = data;
    index = cast.index;
    objectName = cast.objectName;
    path = cast.path;
    reftype = cast.reftype;
    objecttype = cast.objecttype;
    revision = cast.revision;
  }

}
