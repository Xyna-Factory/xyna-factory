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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = OracleRACClusterProviderConfiguration.TABLE_NAME, primaryKey = OracleRACClusterProviderConfiguration.COL_ID)
public class OracleRACClusterProviderConfiguration extends Storable<OracleRACClusterProviderConfiguration> {


  private static final long serialVersionUID = 1L;


  public static final String TABLE_NAME = "oracleracclusterproviderparameters";

  public static final String COL_ID = "id";
  public static final String COL_USER = "user";
  public static final String COL_PASSWORD = "password";
  public static final String COL_URL = "url";
  public static final String COL_TIMEOUT = "timeout";
  public static final String COL_BINDING = "binding";


  private static final ResultSetReader<OracleRACClusterProviderConfiguration> reader =
      new OracleRACClusterProviderConfigurationReader();


  @Column(name = COL_ID)
  private long id;
  @Column(name = COL_USER)
  private String user;
  @Column(name = COL_PASSWORD)
  private String password;
  @Column(name = COL_URL)
  private String url;
  @Column(name = COL_TIMEOUT)
  private long timeout;

  @Column(name = COL_BINDING, index = IndexType.UNIQUE)
  private int binding;


  public OracleRACClusterProviderConfiguration() {

  }


  public OracleRACClusterProviderConfiguration(long id) {
    this.id = id;
  }


  @Override
  public ResultSetReader<? extends OracleRACClusterProviderConfiguration> getReader() {
    return reader;
  }


  @Override
  public <U extends OracleRACClusterProviderConfiguration> void setAllFieldsFromData(U data2) {
    OracleRACClusterProviderConfiguration data = (OracleRACClusterProviderConfiguration)data2;
    this.id = data.id;
    this.user = data.user;
    this.password = data.password;
    this.url = data.url;
    this.timeout = data.timeout;
    this.binding = data.binding;
  }


  private static void fillByResultSet(OracleRACClusterProviderConfiguration orcpc, ResultSet rs) throws SQLException {
    orcpc.id = rs.getLong(COL_ID);
    orcpc.user = rs.getString(COL_USER);
    orcpc.password = rs.getString(COL_PASSWORD);
    orcpc.url = rs.getString(COL_URL);
    orcpc.timeout = rs.getLong(COL_TIMEOUT);
    orcpc.binding = rs.getInt(COL_BINDING);
  }


  private static class OracleRACClusterProviderConfigurationReader
      implements
        ResultSetReader<OracleRACClusterProviderConfiguration> {

    public OracleRACClusterProviderConfiguration read(ResultSet rs) throws SQLException {
      OracleRACClusterProviderConfiguration orcpc = new OracleRACClusterProviderConfiguration();
      fillByResultSet(orcpc, rs);
      return orcpc;
    }

  }


  public long getId() {
    return id;
  }


  public String getUser() {
    return user;
  }


  public String getPassword() {
    return password;
  }


  public long getTimeout() {
    return timeout;
  }


  public String getUrl() {
    return url;
  }


  public void setId(long id) {
    this.id = id;
  }


  public void setUser(String user) {
    this.user = user;
  }


  public void setPassword(String password) {
    this.password = password;
  }


  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }


  public void setUrl(String url) {
    this.url = url;
  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  public void setBinding(int binding) {
    this.binding = binding;
  }


  public int getBinding() {
    return binding;
  }

}
