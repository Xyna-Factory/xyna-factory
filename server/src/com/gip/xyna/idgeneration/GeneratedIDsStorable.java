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

package com.gip.xyna.idgeneration;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



@Persistable(primaryKey = GeneratedIDsStorable.COL_ID, tableName = GeneratedIDsStorable.TABLE_NAME)
public class GeneratedIDsStorable extends ClusteredStorable<GeneratedIDsStorable> {

  private static final long serialVersionUID = 1L;


  public static final String TABLE_NAME = "idgeneration";
  public static final String COL_ID = "id";
  public static final String COL_REALM = "realm";

  public static final String COL_LAST_STORED_ID = "laststoredid";
  public static final String COL_RESULTING_FROM_SHUTDOWN = "resultingfromshutdown";


  @Column(name = COL_ID)
  private String id;
  
  @Column(name = COL_REALM)
  private String realm;

  @Column(name = COL_LAST_STORED_ID)
  private long lastStoredId;

  @Column(name = COL_RESULTING_FROM_SHUTDOWN)
  private boolean resultingFromShutdown;


  private static ResultSetReader<GeneratedIDsStorable> reader = new GeneratedIDsResultSetReader();


  public GeneratedIDsStorable(String id, String realm, int binding) {
    super(binding);
    this.id = id;
    this.realm = realm;
  }


  public GeneratedIDsStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public ResultSetReader<? extends GeneratedIDsStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends GeneratedIDsStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    this.id = data.getId();
    this.realm = data.getRealm();
    this.lastStoredId = data.getLastStoredId();
    this.resultingFromShutdown = data.isResultingFromShutdown();
  }


  private static class GeneratedIDsResultSetReader implements ResultSetReader<GeneratedIDsStorable> {

    public GeneratedIDsStorable read(ResultSet rs) throws SQLException {
      GeneratedIDsStorable result = new GeneratedIDsStorable();
      ClusteredStorable.fillByResultSet(result, rs);
      result.id = rs.getString(COL_ID);
      result.lastStoredId = rs.getLong(COL_LAST_STORED_ID);
      result.resultingFromShutdown = rs.getBoolean(COL_RESULTING_FROM_SHUTDOWN);
      result.realm = rs.getString(COL_REALM);
      return result;
    }

  }


  public void setLastStoredId(long lastStoredId) {
    this.lastStoredId = lastStoredId;
  }


  public long getLastStoredId() {
    return lastStoredId;
  }

  public String getId() {
    return id;
  }
  
  public String getRealm() {
    return realm;
  }

  public void setResultingFromShutdown(boolean resultingFromShutdown) {
    this.resultingFromShutdown = resultingFromShutdown;
  }


  public boolean isResultingFromShutdown() {
    return resultingFromShutdown;
  }


  public void setRealm(String realm) {
    this.realm = realm;
  }

}
