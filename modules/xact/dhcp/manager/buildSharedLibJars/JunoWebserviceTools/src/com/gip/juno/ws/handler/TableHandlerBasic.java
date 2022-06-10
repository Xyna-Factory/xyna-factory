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


package com.gip.juno.ws.handler;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.LocationSchema;
import com.gip.juno.ws.tools.DBTableInfo;

public abstract class TableHandlerBasic implements TableHandler {
  
  protected String _tablename;
  protected Logger _logger;
  protected DBTableInfo _table;
  protected LocationSchema _locationSchema;
  protected boolean supportsCollisionDetection; 
  protected boolean globalLocking; // TODO merge those values into a LockingMethod-enum?
  
  private TableHandlerBasic() {}
  
  protected TableHandlerBasic(String name, boolean supportsCollisionDetection) {
    this(name, supportsCollisionDetection, false);
  }
  
  protected TableHandlerBasic(String name, boolean supportsCollisionDetection, boolean globalLocking) {
    this.supportsCollisionDetection = supportsCollisionDetection;
    this.globalLocking = globalLocking;
    init(name, LocationSchema.notApplicable); 
  }
  
  protected TableHandlerBasic(String name, LocationSchema schema, boolean supportsCollisionDetection) {
    this(name, schema, supportsCollisionDetection, false);
  }
  
  protected TableHandlerBasic(String name, LocationSchema schema, boolean supportsCollisionDetection, boolean globalLocking) {
    this.supportsCollisionDetection = supportsCollisionDetection;
    this.globalLocking = globalLocking;
    init(name, schema);
  }

  protected void init(String name, LocationSchema schema) {
    _tablename = name;
    initLogger(name);
    _table = initDBTableInfo();
    _table.setGlobalLocking(globalLocking);
    _locationSchema = schema;
  }
  
  protected void initLogger(String name) {
    _logger = Logger.getLogger(name);
  }
    
  public Logger getLogger() { return _logger; }
  
  public DBTableInfo getDBTableInfo() {
    return _table;
  }
  
  protected abstract DBTableInfo initDBTableInfo();
  
  public String getTablename() {
    return _tablename;
  }
  
  public LocationSchema getLocationSchema() {
    return _locationSchema;
  }
  
  
  public boolean supportsCollisionDetection() {
    return supportsCollisionDetection;
  }
  

  public boolean needsGlobalLock() {
    return globalLocking;
  }
  
    
}
