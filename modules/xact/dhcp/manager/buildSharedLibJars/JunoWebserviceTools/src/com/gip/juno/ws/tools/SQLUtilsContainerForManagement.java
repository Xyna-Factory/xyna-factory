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


package com.gip.juno.ws.tools;


import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.xyna.utils.db.SQLUtils;

public class SQLUtilsContainerForManagement implements SQLUtilsContainer {
  private SQLUtils _sqlUtils;
  private FailoverFlag _failoverFlag;  
  private DBSchema _dbschema;
  
  public SQLUtilsContainerForManagement(SQLUtils utils, DBSchema schema, FailoverFlag flag) {
    _sqlUtils = utils;
    _failoverFlag = flag;
    _dbschema = schema;
  }
  
  public SQLUtilsContainerForManagement(SQLUtils utils, DBSchema schema) {
    _sqlUtils = utils;
    _failoverFlag = FailoverFlag.primary;
    _dbschema = schema;
  }
  
  public SQLUtils getSQLUtils() {
    return _sqlUtils;
  }
  private void setSQLUtils(SQLUtils utils) {
    _sqlUtils = utils;
  }
  public FailoverFlag getFailOverFlag() {
    return _failoverFlag;
  }
  private void setFailoverFlag(FailoverFlag flag) {
    _failoverFlag = flag;
  }  
  private void setDBSchema(DBSchema schema) {
    _dbschema = schema;
  }
  public DBSchema getDBSchema() {
    return _dbschema;    
  }
}
