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

import java.util.*;

import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.enums.Updates;
import com.gip.juno.ws.enums.Visible;
import com.gip.juno.ws.handler.PropagationHandler;

/**
 * Class used to supply information about the columns of a database table
 */
public class DBTableInfo {
  
  private TreeMap<String, ColInfo> _columns = new TreeMap<String, ColInfo>();
  
  private String _tablename = "";
  
  private String _schema = "";
  
  private boolean globalLocking = false;
  
  private PropagationHandler propagationHandler;
      
  private DBTableInfo() {
  }
  
  public DBTableInfo(String name, String schema) {
    _tablename = name;
    _schema = schema;
  }
  

  
  public String getSchema() { return _schema;  }
  public void setSchema(String val) { _schema = val; }
  
  
  public void addColumn(ColInfo col) {
    if (!col.name.equals("")) {
      col.num = _columns.size();
      _columns.put(col.name, col);
    }
  }
  
  public void addColumn(String name, ColType type, Visible visible) {
    ColInfo col = new ColInfo("name");
    col.visible = visible;
    col.type = type;
    col.pk = Pk.False;
    col.num = _columns.size();
    _columns.put(name, col);
  }
  
  public void addColumn(String name, ColType type, Visible visible, Pk pk) {
    ColInfo col = new ColInfo("name");
    col.visible = visible;
    col.type = type;
    col.pk = pk;
    col.num = _columns.size();
    _columns.put(name, col);
  }
  
  public void addColumn(String name, String guiname, ColType type, Visible visible, Pk pk) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = guiname;
    col.visible = visible;
    col.type = type;
    col.pk = pk;
    col.num = _columns.size();
    _columns.put(name, col);
  }
  
  public void addColumn(String name, String guiname, ColType type, Visible visible) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = guiname;
    col.visible = visible;
    col.type = type;
    col.pk = Pk.False;
    col.num = _columns.size();
    _columns.put(name, col);
  }
  
  public void addColumn(String name, String guiname, ColType type, Visible visible, Updates updates) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = guiname;
    col.visible = visible;
    col.type = type;
    col.pk = Pk.False;
    col.updates = updates;
    col.num = _columns.size();
    _columns.put(name, col);
  }

  public void addColumn(String name, String guiname, ColType type, Visible visible, Updates updates, Pk pk) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = guiname;
    col.visible = visible;
    col.type = type;
    col.updates = updates;
    col.pk = pk;
    col.num = _columns.size();
    _columns.put(name, col);
  }
  

  public void addColumn(String name, String guiname, String xmlName, ColType type, Visible visible, Updates updates) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = xmlName;
    col.visible = visible;
    col.type = type;
    col.pk = Pk.False;
    col.updates = updates;
    col.num = _columns.size();
    _columns.put(name, col);
  }

  public void addColumn(String name, String guiname, String xmlName, ColType type, Visible visible, 
          Updates updates, Pk pk) {
    ColInfo col = new ColInfo("name");
    col.guiname = guiname;
    col.xmlName = xmlName;
    col.visible = visible;
    col.type = type;
    col.updates = updates;
    col.pk = pk;    
    col.num = _columns.size();
    _columns.put(name, col);
  }
  

  public void addColumn(String name, ColType type, Visible visible, Updates updates) {
    ColInfo col = new ColInfo("name");
    col.visible = visible;
    col.type = type;
    col.pk = Pk.False;
    col.updates = updates;
    col.num = _columns.size();
    _columns.put(name, col);
  }

  public void addColumn(String name, ColType type, Visible visible, 
          Updates updates, Pk pk) {
    ColInfo col = new ColInfo("name");
    col.visible = visible;
    col.type = type;
    col.updates = updates;
    col.pk = pk;    
    col.num = _columns.size();
    _columns.put(name, col);
  }
    
  public int getMaxColNum() {
    return _columns.size() -1;
  }
  
  public String getTablename() { return _tablename; }
  public void setTablename(String tablename) {  this._tablename = tablename; }
    
  public TreeMap<String, ColInfo> getColumns() { return _columns; }
  
  public int getNumColumns() { return _columns.size(); }
  
  public List<String> getColumnNames() {
    List<String> ret = new ArrayList<String>();
    for (String key : _columns.keySet()) {
      ret.add(key);
    }
    return ret;
  }
  
  
  public boolean needsGlobalLocking() {
    return globalLocking;
  }
  
  
  public void setGlobalLocking(boolean globalLocking) {
    this.globalLocking = globalLocking;
  }
  
  
  public boolean hasPropagationHandling() {
    return propagationHandler != null;
  }
  
  
  public void addPropagationHandler(PropagationHandler propagationHandler) {
    this.propagationHandler = propagationHandler;
  }
  
  public PropagationHandler getPropagationHandler() {
    return this.propagationHandler;
  }
  
  public DBTableInfo clone() {
    DBTableInfo clone = new DBTableInfo(_tablename, _schema);
    clone.globalLocking = this.globalLocking;
    clone.propagationHandler = this.propagationHandler;
    // this is only shallow but should be enough
    clone._columns = new TreeMap<String, ColInfo>(this._columns);
    return clone;
  }
  
}
