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
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = SharedResourceSynchronizerInstanceStorable.COL_INSTANCE_NAME, tableName = SharedResourceSynchronizerInstanceStorable.TABLE_NAME)
public class SharedResourceSynchronizerInstanceStorable extends Storable<SharedResourceSynchronizerInstanceStorable> {

  public static final String TABLE_NAME = "sharedresourcesynchronizerinstances";

  public static final String COL_INSTANCE_NAME = "instancename";
  public static final String COL_SYNCHRONIZER_TYPE_NAME = "sharedresourcesynchronizertypename";
  public static final String COL_CONFIGURATION = "configuration";
  public static final String COL_STATUS = "status";

  private static final long serialVersionUID = 1L;

  @Column(name = COL_INSTANCE_NAME)
  private String instanceName;

  @Column(name = COL_SYNCHRONIZER_TYPE_NAME)
  private String synchronizerTypeName;

  @Column(name = COL_CONFIGURATION)
  private StringSerializableList<String> configuration = StringSerializableList.autoSeparator(String.class);

  @Column(name = COL_STATUS)
  private String status;

  private static SharedResourceSynchronizerInstanceStorableReader reader = new SharedResourceSynchronizerInstanceStorableReader();


  public SharedResourceSynchronizerInstanceStorable() {
  }


  public SharedResourceSynchronizerInstanceStorable(String instanceName, String synchronizerTypeName, List<String> configuration,
                                                    SharedResourceSynchronizerInstance.Status status) {
    this.instanceName = instanceName;
    this.synchronizerTypeName = synchronizerTypeName;
    if (configuration != null) {
      this.configuration.setValues(configuration);
    }
    this.status = status.toString();
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
      result.configuration.deserializeFromString(rs.getString(COL_CONFIGURATION));
      result.status = rs.getString(COL_STATUS);
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
    configuration.setValues(cast.configuration);
    status = cast.status;
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


  public List<String> getConfiguration() {
    return configuration;
  }


  public void setConfiguration(List<String> configuration) {
    this.configuration.setValues(configuration);
  }


  public String getStatus() {
    return status;
  }


  public void setStatus(String status) {
    this.status = status;
  }

}
