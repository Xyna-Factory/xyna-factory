/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = SharedResourceSynchronizerInstanceStorable.COL_INSTANCE_NAME, tableName = SharedResourceSynchronizerInstanceStorable.TABLE_NAME)
public class SharedResourceSynchronizerInstanceStorable extends Storable<SharedResourceSynchronizerInstanceStorable> {

  public static final String TABLE_NAME = "sharedresourcesynchronizerinstances";

  public static final String COL_INSTANCE_NAME = "instancename";
  public static final String COL_SYNCHRONIZER_TYPE_NAME = "sharedresourcesynchronizertypename";

  private static final long serialVersionUID = 1L;

  @Column(name = COL_INSTANCE_NAME)
  private String instanceName;

  @Column(name = COL_SYNCHRONIZER_TYPE_NAME)
  private String synchronizerTypeName;

  private static SharedResourceSynchronizerInstanceStorableReader reader = new SharedResourceSynchronizerInstanceStorableReader();


  public SharedResourceSynchronizerInstanceStorable() {
  }


  public SharedResourceSynchronizerInstanceStorable(String instanceName, String synchronizerTypeName) {
    this.instanceName = instanceName;
    this.synchronizerTypeName = synchronizerTypeName;
  }


  @Override
  public ResultSetReader<? extends SharedResourceSynchronizerInstanceStorable> getReader() {
    return reader;
  }


  private static class SharedResourceSynchronizerInstanceStorableReader
      implements
        ResultSetReader<SharedResourceSynchronizerInstanceStorable> {

    @Override
    public SharedResourceSynchronizerInstanceStorable read(ResultSet rs) throws SQLException {
      SharedResourceSynchronizerInstanceStorable result = new SharedResourceSynchronizerInstanceStorable();
      result.instanceName = rs.getString(COL_INSTANCE_NAME);
      result.synchronizerTypeName = rs.getString(COL_SYNCHRONIZER_TYPE_NAME);
      return result;
    }

  }


  @Override
  public Object getPrimaryKey() {
    return instanceName;
  }


  @Override
  public <U extends SharedResourceSynchronizerInstanceStorable> void setAllFieldsFromData(U data) {
    SharedResourceSynchronizerInstanceStorable cast = data;
    instanceName = cast.instanceName;
    synchronizerTypeName = cast.synchronizerTypeName;
  }


  public String getInstanceName() {
    return instanceName;
  }


  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }


  public String getSynchronizerTypeName() {
    return synchronizerTypeName;
  }


  public void setSynchronizerTypeName(String synchronizerTypeName) {
    this.synchronizerTypeName = synchronizerTypeName;
  }

}
