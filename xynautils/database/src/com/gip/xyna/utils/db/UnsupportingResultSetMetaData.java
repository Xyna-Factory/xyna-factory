/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.db;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * ResultSetMetaData Implementation, die in allen Methoden eine unsupported Exception wirft. 
 * Geeignet, um davon abzuleiten und nur geringe Featureanzahl zu implementieren.
 */
public class UnsupportingResultSetMetaData implements ResultSetMetaData {

  public static final String UNSUPPORTED_MESSAGE = "unsupported resultSetMetaData operation.";

  private SQLException unsupportedException() {
    return new SQLException(UNSUPPORTED_MESSAGE);
  }

  public int getColumnCount() throws SQLException {
    throw unsupportedException();
  }

  public boolean isAutoIncrement(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isCaseSensitive(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isSearchable(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isCurrency(int column) throws SQLException {
    throw unsupportedException();
  }

  public int isNullable(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isSigned(int column) throws SQLException {
    throw unsupportedException();
  }

  public int getColumnDisplaySize(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getColumnLabel(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getColumnName(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getSchemaName(int column) throws SQLException {
    throw unsupportedException();
  }

  public int getPrecision(int column) throws SQLException {
    throw unsupportedException();
  }

  public int getScale(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getTableName(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getCatalogName(int column) throws SQLException {
    throw unsupportedException();
  }

  public int getColumnType(int column) throws SQLException {
    throw unsupportedException();
  }
  
  public String getColumnTypeName(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isReadOnly(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isWritable(int column) throws SQLException {
    throw unsupportedException();
  }

  public boolean isDefinitelyWritable(int column) throws SQLException {
    throw unsupportedException();
  }

  public String getColumnClassName(int column) throws SQLException {
    throw unsupportedException();
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw unsupportedException();
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw unsupportedException();
  }

}
