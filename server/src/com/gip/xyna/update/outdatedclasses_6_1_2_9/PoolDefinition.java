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
package com.gip.xyna.update.outdatedclasses_6_1_2_9;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = PoolDefinition.COL_NAME, tableName = PoolDefinition.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
public class PoolDefinition extends Storable<PoolDefinition> {

  private static final long serialVersionUID = 1L;
  
  protected final static String TABLENAME = "pooldefinition";
  protected final static String COL_NAME = "name";
  protected final static String COL_TYPE = "type";
  protected final static String COL_SIZE = "size";
  protected final static String COL_USER = "user";
  protected final static String COL_PASSWORD = "password";
  protected final static String COL_RETRIES = "retries";
  protected final static String COL_ADDITIONAL_PARAMS = "params";
  protected final static String COL_VALIDATION_INTERVAL = "validationinterval";
  protected final static String COL_CONNECTSTRING = "connectstring";
  
  
  @Column(name = COL_NAME, index = IndexType.PRIMARY)
  public String name;
  @Column(name = COL_TYPE)
  public String type;
  @Column(name = COL_SIZE)
  public int size;
  @Column(name = COL_USER)
  public String user;
  @Column(name = COL_PASSWORD)
  public String password;
  @Column(name = COL_RETRIES)
  public int retries;
  @Column(name = COL_ADDITIONAL_PARAMS, type = ColumnType.BLOBBED_JAVAOBJECT)
  public Map<String, String> params;
  @Column(name = COL_VALIDATION_INTERVAL)
  public long validationinterval;
  @Column(name = COL_CONNECTSTRING)
  public String connectstring;
  
  
  @Override
  public ResultSetReader<PoolDefinition> getReader() {
    return new ResultSetReader<PoolDefinition>() {

      public PoolDefinition read(ResultSet rs) throws SQLException {
        PoolDefinition poolDefinition = new PoolDefinition();
        fillByResultSet(poolDefinition, rs);
        return poolDefinition;
      }
    };
  }


  @Override
  public Object getPrimaryKey() {
    return name;
  }


  @Override
  public void setAllFieldsFromData(PoolDefinition data) {
    this.name=data.name;
    this.type=data.type;
    this.size=data.size;
    this.user=data.user;
    this.password=data.password;
    this.retries=data.retries;
    this.params=data.params;
    this.validationinterval=data.validationinterval;
    this.connectstring=data.connectstring;
  }
  
  
  
  @SuppressWarnings("unchecked")
  private static void fillByResultSet(PoolDefinition poolDefinition, ResultSet rs) throws SQLException {
    poolDefinition.name=rs.getString(COL_NAME);
    poolDefinition.type=rs.getString(COL_TYPE);
    poolDefinition.size=rs.getInt(COL_SIZE);
    poolDefinition.user=rs.getString(COL_USER);
    poolDefinition.password=rs.getString(COL_PASSWORD);
    poolDefinition.retries=rs.getInt(COL_RETRIES);
    poolDefinition.params=(Map<String, String>)poolDefinition.readBlobbedJavaObjectFromResultSet(rs, COL_ADDITIONAL_PARAMS);
    poolDefinition.validationinterval=rs.getLong(COL_VALIDATION_INTERVAL);
    poolDefinition.connectstring=rs.getString(COL_CONNECTSTRING);
  }


  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }

  
  public String getType() {
    return type;
  }

  
  public void setType(String type) {
    this.type = type;
  }

  
  public int getSize() {
    return size;
  }

  
  public void setSize(int size) {
    this.size = size;
  }

  
  public String getUser() {
    return user;
  }

  
  public void setUser(String user) {
    this.user = user;
  }

  
  public String getPassword() {
    return password;
  }


  public void setPassword(String password) {
    this.password = password;
  }

  
  public int getRetries() {
    return retries;
  }

  
  public void setRetries(int retries) {
    this.retries = retries;
  }

  
  public Map<String, String> getParams() {
    return params;
  }

  
  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  
  public long getValidationinterval() {
    return validationinterval;
  }

  
  public void setValidationinterval(long validationinterval) {
    this.validationinterval = validationinterval;
  }

  
  public String getConnectstring() {
    return connectstring;
  }

  
  public void setConnectstring(String connectstring) {
    this.connectstring = connectstring;
  }

}
