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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xnwh.persistence.SimpleResultSetMetaData;


public class XMLShellResultSet extends UnsupportingResultSet {
  
  //Mapping from ColumnName to Object
  private Map<String, String> data; 
  private String lastGet;
  
  public XMLShellResultSet() {
    data = new HashMap<String, String>();
  }
  
  private String retrieve(String columnName) throws XynaException {
    if (data.containsKey(columnName)) {
      lastGet = columnName;
      return data.get(columnName);
    }
    lastGet = null;
    throw new XynaException("Specified data not present");
  }
  
  private String retrieve(int columnIndex) {
    if (columnIndex > data.size()) {
      throw new IndexOutOfBoundsException("columnIndex '"+ columnIndex +"' > dataSize in resultSet '"+data.size()+"'");
    }
    Iterator<String> iter = data.values().iterator();
    String next = null;
    for (int i = 0; i < columnIndex; i++) {
      next = iter.next();
    }
    return next;
  }
  
  public void addData(String columnName, String value) {
    data.put(columnName, value);
  }
  
  @Override
  public boolean wasNull() throws SQLException {
    if (lastGet == null) {
      return true;
    } else {
      if (data.get(lastGet) == null) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public Blob getBlob(String columnName) throws SQLException {
    try {
      //check for nulls...
      String base64String;
      try {
        base64String = retrieve(columnName);
      } catch (XynaException e) {
        return null;
      }
      if (base64String == null) {
        return null;
      }
      return new XMLBLOB(Base64.decode(base64String));
    } catch (IOException e) {
      throw new SQLException("IOException while reading blob");
    }
  }
  
  @Override
  public boolean getBoolean(String columnName) throws SQLException {
    try {
      return Boolean.parseBoolean(retrieve(columnName));
    } catch (XynaException e) {
      return false;
    }
  }
  
  @Override
  public double getDouble(String columnName) throws SQLException {
    try {
      return Double.parseDouble(retrieve(columnName));
    } catch (XynaException e) {
      return 0d;
    }
  }
  
  @Override
  public String getString(String columnName) throws SQLException {
    try {
      return retrieve(columnName);
    } catch (XynaException e) {
      return null;
    }
  }
  
  @Override
  public float getFloat(String columnName) throws SQLException {
    try {
      return Float.parseFloat(retrieve(columnName));
    } catch (XynaException e) {
      return 0f;
    }
  }
  
  @Override
  public int getInt(String columnName) throws SQLException {
    try {
      return Integer.parseInt(retrieve(columnName));
    } catch (XynaException e) {
      return 0;
    }
  }
  
  @Override
  public long getLong(String columnName) throws SQLException {
    try {
      return Long.parseLong(retrieve(columnName));
    } catch (XynaException e) {
      return 0l;
    }
  }
  
  @Override
  public Blob getBlob(int columnIndex) throws SQLException {
    try {
      //check for nulls...
      String base64String = retrieve(columnIndex);
      if (base64String == null) {
        return null;
      }
      return new XMLBLOB(Base64.decode(base64String));
    } catch (IOException e) {
      throw new SQLException("IOException while reading blob");
    }
  }
  
  @Override
  public boolean getBoolean(int columnIndex) throws SQLException {
    return Boolean.parseBoolean(retrieve(columnIndex));
  }
  
  @Override
  public double getDouble(int columnIndex) throws SQLException {
    return Double.parseDouble(retrieve(columnIndex));
  }
  
  @Override
  public String getString(int columnIndex) throws SQLException {
    return retrieve(columnIndex);
  }
  
  @Override
  public float getFloat(int columnIndex) throws SQLException {
    return Float.parseFloat(retrieve(columnIndex));
  }
  
  @Override
  public int getInt(int columnIndex) throws SQLException {
    return Integer.parseInt(retrieve(columnIndex));
  }
  
  @Override
  public long getLong(int columnIndex) throws SQLException {
    return Long.parseLong(retrieve(columnIndex));
  }
  
  @Override
  public ResultSetMetaData getMetaData() throws SQLException {    
    return new SimpleResultSetMetaData(data.keySet());
  }
  
  
}
