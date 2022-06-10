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


import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.enums.LocationSchema;
import com.gip.xyna.utils.db.SQLUtils;

public class SQLUtilsContainerForLocation implements SQLUtilsContainer {
  private SQLUtils _sqlUtils;
  private FailoverFlag _failoverFlag;  
  private String _location;
  private LocationSchema _schema;
  
  SQLUtilsContainerForLocation(SQLUtils utils, String location, FailoverFlag flag, LocationSchema schema) {
    _sqlUtils = utils;
    _location = location;
    _failoverFlag = flag;
    _schema = schema;
  }
  
  SQLUtilsContainerForLocation(SQLUtils utils, String location, LocationSchema schema) {
    _sqlUtils = utils;
    _location = location;
    _failoverFlag = FailoverFlag.primary;
    _schema = schema;
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
  private void setLocation(String location) {
    _location = location;
  }  
  public String getLocation() {
    return _location;
  }
  public LocationSchema getSchema() { return _schema; }
}
