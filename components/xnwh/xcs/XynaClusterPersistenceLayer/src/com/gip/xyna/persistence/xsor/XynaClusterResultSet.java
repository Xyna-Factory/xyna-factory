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
package com.gip.xyna.persistence.xsor;

import java.io.Serializable;
import java.sql.Array;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import com.gip.xyna.xnwh.persistence.SimpleResultSetMetaData;
import com.gip.xyna.xnwh.persistence.Storable;


public class XynaClusterResultSet extends Java1_6UnsupportingResultSet {

  private final static String SELECT_ALL_PATTERN = "*"; 
  
  private String[] selection; // TODO map those instead of iterating?
  private boolean selectAll = false;
  private Storable backingStorable;
  private boolean open = false;
  private boolean wasNull;
  
  public XynaClusterResultSet(final String[] selection) { 
    this.selection = selection;
  }
  
  
  <S extends Storable> void setBackingStorable(S s) {
    backingStorable = s;
    if (selection.length == 1 &&
        selection[0].equals(SELECT_ALL_PATTERN)) {
      selectAll = true;
    }
    open = true;
    wasNull = false;
  }
     
  
  private void throwSQLExceptionIfClosed() throws SQLException {
    if (!open) {
      throw new SQLException("ResultSetClosed");
    }
  }
  
  
  private void throwSQLExceptionIfNotSelected(String columnLabel) throws SQLException {
    if (selectAll) {
      return;
    } else {
      for (String column : selection) {
        if (column.equalsIgnoreCase(columnLabel)) {
          return;
        }
      }
      throw new SQLException("Unknown column contained in field list");
    }
  }
  
  
  private <V> V convertOrThrow(Class<V> clazz, Serializable value) throws SQLException {
    if (value == null) {
      wasNull = true;
      return null;
    } else if (clazz.isInstance(value)) {
      wasNull = false;
      return clazz.cast(value);
    } else if (clazz.equals(String.class)) {
      wasNull = false;
      if (value instanceof byte[]) {
        return clazz.cast(Arrays.toString((byte[])value));
      } else {
        return clazz.cast(value.toString());
      }
    } else {
      wasNull = false;
      throw new SQLException("Could not convert value: " + value);
    }
  }

  
  public void close() throws SQLException {
    open = false;
  }

  
  public boolean wasNull() throws SQLException {
    return wasNull;
  }

  
  public String getString(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(String.class, value);
  }

  
  public boolean getBoolean(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Boolean.class, value);
  }

  
  public byte getByte(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Byte.class, value);
  }

  
  public short getShort(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Short.class, value);
  }

  
  public int getInt(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Integer.class, value);
  }

  
  public long getLong(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Long.class, value);
  }

  
  public float getFloat(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Float.class, value);
  }

  
  public double getDouble(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Double.class, value);
  }


  public byte[] getBytes(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(byte[].class, value);
  }


  public String getString(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(String.class, value);
  }

  
  public boolean getBoolean(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Boolean.class, value);
  }

  
  public byte getByte(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Byte.class, value);
  }

  
  public short getShort(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Short.class, value);
  }

  
  public int getInt(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Integer.class, value);
  }

  
  public long getLong(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Long.class, value);
  }

  
  public float getFloat(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Float.class, value);
  }

  
  public double getDouble(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Double.class, value);
  }


  public byte[] getBytes(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(byte[].class, value);
  }


  public ResultSetMetaData getMetaData() throws SQLException {
    return new SimpleResultSetMetaData(Arrays.asList(selection));
    
  }

  
  public Object getObject(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Object.class, value);
  }

  
  public Object getObject(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Object.class, value);
  }

  
  public int findColumn(String columnLabel) throws SQLException {
    return Arrays.binarySearch(selection, columnLabel);
  }

 
  public Array getArray(int columnIndex) throws SQLException {
    throwSQLExceptionIfClosed();
    Serializable value = backingStorable.getValueByColString(selection[columnIndex-1]);
    return convertOrThrow(Array.class, value);
  }

  
  public Array getArray(String columnLabel) throws SQLException {
    throwSQLExceptionIfClosed();
    throwSQLExceptionIfNotSelected(columnLabel);
    Serializable value = backingStorable.getValueByColString(columnLabel);
    return convertOrThrow(Array.class, value);
  }


  public boolean isClosed() throws SQLException {
    return !open;
  }

  

  

}
