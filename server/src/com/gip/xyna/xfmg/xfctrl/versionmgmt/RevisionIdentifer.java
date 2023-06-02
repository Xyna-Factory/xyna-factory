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
package com.gip.xyna.xfmg.xfctrl.versionmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(tableName = RevisionIdentifer.TABLE_NAME, primaryKey = RevisionIdentifer.COL_ID)
public class RevisionIdentifer extends Storable<RevisionIdentifer> {

  private static final long serialVersionUID = -5676546536221747791L;
  
  public static final String TABLE_NAME = "revision";
  public static final String COL_ID = "id";
  public static final String COL_MAX_REVISION = "maxrevision";
  
  public static final String REVISION_ID = "revisionId";
  
  
  @Column(name = COL_ID)
  private String id;
  
  @Column(name = COL_MAX_REVISION)
  private Long maxrevision;
  
  
  public RevisionIdentifer() {
  }
  
  public RevisionIdentifer(String id) {
    this.id = id;
  }
  
  
  private static final ResultSetReader<RevisionIdentifer> reader = new ResultSetReader<RevisionIdentifer>() {

    public RevisionIdentifer read(ResultSet rs) throws SQLException {
      RevisionIdentifer r = new RevisionIdentifer();
      r.id = rs.getString(COL_ID);
      r.maxrevision = rs.getLong(COL_MAX_REVISION);
      return r;
    }
    
  };
  
  
  @Override
  public ResultSetReader<? extends RevisionIdentifer> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends RevisionIdentifer> void setAllFieldsFromData(U data) {
    RevisionIdentifer cast = data;
    id = cast.id;
    maxrevision = cast.maxrevision;    
  }

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  
  public Long getMaxrevision() {
    return maxrevision;
  }

  
  public void setMaxrevision(Long maxrevision) {
    this.maxrevision = maxrevision;
  }

  @Override
  public String toString() {
    return "id=" + id + ", maxrevision=" + maxrevision;
  }

}
