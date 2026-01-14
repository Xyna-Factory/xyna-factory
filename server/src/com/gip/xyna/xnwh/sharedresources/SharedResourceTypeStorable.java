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



@Persistable(primaryKey = SharedResourceTypeStorable.COL_SHARED_RESOURCE_TYPE_IDENTIFIER, tableName = SharedResourceTypeStorable.TABLE_NAME)
public class SharedResourceTypeStorable extends Storable<SharedResourceTypeStorable> {

  public static final String TABLE_NAME = "sharedresourcetypes";

  public static final String COL_SHARED_RESOURCE_TYPE_IDENTIFIER = "sahredresourcetypeidentifier";
  public static final String COL_SYNCHRONIZER_INSTANCE_IDENTIFIER = "synchronizerinstanceidentifier";

  private static final long serialVersionUID = 1L;

  @Column(name = COL_SHARED_RESOURCE_TYPE_IDENTIFIER)
  private String sharedResourceTypeIdentifier;
  @Column(name = COL_SYNCHRONIZER_INSTANCE_IDENTIFIER)
  private String synchronizerInstanceIdentifier;


  private static SharedResourceTypeStorableReader reader = new SharedResourceTypeStorableReader();


  public SharedResourceTypeStorable() {
  }


  public SharedResourceTypeStorable(String sharedResourceTypeIdentifier, String synchronizerInstanceIdentifier) {
    this.sharedResourceTypeIdentifier = sharedResourceTypeIdentifier;
    this.synchronizerInstanceIdentifier = synchronizerInstanceIdentifier;
  }


  @Override
  public ResultSetReader<? extends SharedResourceTypeStorable> getReader() {
    return reader;
  }


  private static class SharedResourceTypeStorableReader implements ResultSetReader<SharedResourceTypeStorable> {

    @Override
    public SharedResourceTypeStorable read(ResultSet rs) throws SQLException {
      SharedResourceTypeStorable result = new SharedResourceTypeStorable();
      result.sharedResourceTypeIdentifier = rs.getString(COL_SHARED_RESOURCE_TYPE_IDENTIFIER);
      result.synchronizerInstanceIdentifier = rs.getString(COL_SYNCHRONIZER_INSTANCE_IDENTIFIER);
      return result;
    }

  }


  @Override
  public Object getPrimaryKey() {
    return sharedResourceTypeIdentifier;
  }


  @Override
  public <U extends SharedResourceTypeStorable> void setAllFieldsFromData(U data) {
    SharedResourceTypeStorable cast = data;
    sharedResourceTypeIdentifier = cast.sharedResourceTypeIdentifier;
    synchronizerInstanceIdentifier = cast.synchronizerInstanceIdentifier;
  }


  public String getSharedResourceTypeIdentifier() {
    return sharedResourceTypeIdentifier;
  }


  public void setSharedResourceTypeIdentifier(String sharedResourceTypeIdentifier) {
    this.sharedResourceTypeIdentifier = sharedResourceTypeIdentifier;
  }


  public String getSynchronizerInstanceIdentifier() {
    return synchronizerInstanceIdentifier;
  }


  public void setSynchronizerInstanceIdentifier(String synchronizerInstanceIdentifier) {
    this.synchronizerInstanceIdentifier = synchronizerInstanceIdentifier;
  }

}
