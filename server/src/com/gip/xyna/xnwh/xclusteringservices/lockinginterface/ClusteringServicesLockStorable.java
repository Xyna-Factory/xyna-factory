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

package com.gip.xyna.xnwh.xclusteringservices.lockinginterface;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



@Persistable(tableName = ClusteringServicesLockStorable.TABLE_NAME, primaryKey = ClusteringServicesLockStorable.COL_NAME)
public class ClusteringServicesLockStorable extends ClusteredStorable<ClusteringServicesLockStorable> {


  private static final long serialVersionUID = 1L;


  public static final int SHARED_LOCKS_GLOBALLY_CONSTANT_BINDING = 1;


  public static final String TABLE_NAME = "clusteringserviceslocks";
  public static final String COL_NAME = "name";


  private static final ResultSetReader<ClusteringServicesLockStorable> reader =
      new ClusteringServicesLockStorableReader();


  @Column(name = COL_NAME)
  private String name;


  public ClusteringServicesLockStorable() {
    super(SHARED_LOCKS_GLOBALLY_CONSTANT_BINDING);
    // required for mem persistence layer
  }


  public ClusteringServicesLockStorable(String name) {
    super(SHARED_LOCKS_GLOBALLY_CONSTANT_BINDING);
    this.name = name;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  @Override
  public ResultSetReader<? extends ClusteringServicesLockStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public <U extends ClusteringServicesLockStorable> void setAllFieldsFromData(U data) {
    ClusteringServicesLockStorable cast = data;
    this.name = cast.name;
  }


  private static void fillByResultSet(ClusteringServicesLockStorable csls, ResultSet rs) throws SQLException {
    csls.name = rs.getString(COL_NAME);
  }


  private static class ClusteringServicesLockStorableReader implements ResultSetReader<ClusteringServicesLockStorable> {

    public ClusteringServicesLockStorable read(ResultSet rs) throws SQLException {
      ClusteringServicesLockStorable csls = new ClusteringServicesLockStorable();
      fillByResultSet(csls, rs);
      return csls;
    }
    
  }

}
