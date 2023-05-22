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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;


public class WrappedResultSet implements ResultSet {
  
  private ResultSet innerResultSet;
  
  public WrappedResultSet(ResultSet rs) {
    innerResultSet = rs;
  }
  
  public void setInnerResultSet(ResultSet rs) {
    innerResultSet = rs;
  }
  
  public ResultSet getInnerResultSet() {
    return innerResultSet;
  }

  public boolean absolute(int row) throws SQLException {
    return innerResultSet.absolute(row);
  }

  public void afterLast() throws SQLException {
    innerResultSet.afterLast();
  }

  public void beforeFirst() throws SQLException {
    innerResultSet.beforeFirst();
  }

  public void cancelRowUpdates() throws SQLException {
    innerResultSet.cancelRowUpdates();
  }

  public void clearWarnings() throws SQLException {
    innerResultSet.clearWarnings();
  }

  public void close() throws SQLException {
    innerResultSet.close();
  }

  public void deleteRow() throws SQLException {
    innerResultSet.deleteRow();
  }

  public int findColumn(String columnName) throws SQLException {
    return innerResultSet.findColumn(columnName);
  }

  public boolean first() throws SQLException {
    return innerResultSet.first();
  }

  public Array getArray(int i) throws SQLException {
    return innerResultSet.getArray(i);
  }

  public Array getArray(String colName) throws SQLException {
    return innerResultSet.getArray(colName);
  }

  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    return innerResultSet.getAsciiStream(columnIndex);
  }

  public InputStream getAsciiStream(String columnName) throws SQLException {
    return innerResultSet.getAsciiStream(columnName);
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    return innerResultSet.getBigDecimal(columnIndex, scale);
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return innerResultSet.getBigDecimal(columnIndex);
  }

  public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
    return innerResultSet.getBigDecimal(columnName, scale);
  }

  public BigDecimal getBigDecimal(String columnName) throws SQLException {
    return innerResultSet.getBigDecimal(columnName);
  }

  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    return innerResultSet.getBinaryStream(columnIndex);
  }

  public InputStream getBinaryStream(String columnName) throws SQLException {
    return innerResultSet.getBinaryStream(columnName);
  }

  public Blob getBlob(int i) throws SQLException {
    return innerResultSet.getBlob(i);
  }

  public Blob getBlob(String colName) throws SQLException {
    return innerResultSet.getBlob(colName);
  }

  public boolean getBoolean(int columnIndex) throws SQLException {
    return innerResultSet.getBoolean(columnIndex);
  }

  public boolean getBoolean(String columnName) throws SQLException {
    return innerResultSet.getBoolean(columnName);
  }

  public byte getByte(int columnIndex) throws SQLException {
    return innerResultSet.getByte(columnIndex);
  }

  public byte getByte(String columnName) throws SQLException {
    return innerResultSet.getByte(columnName);
  }

  public byte[] getBytes(int columnIndex) throws SQLException {
    return innerResultSet.getBytes(columnIndex);
  }

  public byte[] getBytes(String columnName) throws SQLException {
    return innerResultSet.getBytes(columnName);
  }

  public Reader getCharacterStream(int columnIndex) throws SQLException {
    return innerResultSet.getCharacterStream(columnIndex);
  }

  public Reader getCharacterStream(String columnName) throws SQLException {
    return innerResultSet.getCharacterStream(columnName);
  }

  public Clob getClob(int i) throws SQLException {
    return innerResultSet.getClob(i);
  }

  public Clob getClob(String colName) throws SQLException {
    return innerResultSet.getClob(colName);
  }

  public int getConcurrency() throws SQLException {
    return innerResultSet.getConcurrency();
  }

  public String getCursorName() throws SQLException {
    return innerResultSet.getCursorName();
  }

  public Date getDate(int columnIndex, Calendar cal) throws SQLException {
    return innerResultSet.getDate(columnIndex, cal);
  }

  public Date getDate(int columnIndex) throws SQLException {
    return innerResultSet.getDate(columnIndex);
  }

  public Date getDate(String columnName, Calendar cal) throws SQLException {
    return innerResultSet.getDate(columnName, cal);
  }

  public Date getDate(String columnName) throws SQLException {
    return innerResultSet.getDate(columnName);
  }

  public double getDouble(int columnIndex) throws SQLException {
    return innerResultSet.getDouble(columnIndex);
  }

  public double getDouble(String columnName) throws SQLException {
    return innerResultSet.getDouble(columnName);
  }

  public int getFetchDirection() throws SQLException {
    return innerResultSet.getFetchDirection();
  }

  public int getFetchSize() throws SQLException {
    return innerResultSet.getFetchSize();
  }

  public float getFloat(int columnIndex) throws SQLException {
    return innerResultSet.getFloat(columnIndex);
  }

  public float getFloat(String columnName) throws SQLException {
    return innerResultSet.getFloat(columnName);
  }

  public int getInt(int columnIndex) throws SQLException {
    return innerResultSet.getInt(columnIndex);
  }

  public int getInt(String columnName) throws SQLException {
    return innerResultSet.getInt(columnName);
  }

  public long getLong(int columnIndex) throws SQLException {
    return innerResultSet.getLong(columnIndex);
  }

  public long getLong(String columnName) throws SQLException {
    return innerResultSet.getLong(columnName);
  }

  public ResultSetMetaData getMetaData() throws SQLException {
    return innerResultSet.getMetaData();
  }

  public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
    return innerResultSet.getObject(i, map);
  }

  public Object getObject(int columnIndex) throws SQLException {
    return innerResultSet.getObject(columnIndex);
  }

  public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException {
    return innerResultSet.getObject(colName, map);
  }

  public Object getObject(String columnName) throws SQLException {
    return innerResultSet.getObject(columnName);
  }

  public Ref getRef(int i) throws SQLException {
    return innerResultSet.getRef(i);
  }

  public Ref getRef(String colName) throws SQLException {
    return innerResultSet.getRef(colName);
  }

  public int getRow() throws SQLException {
    return innerResultSet.getRow();
  }

  public short getShort(int columnIndex) throws SQLException {
    return innerResultSet.getShort(columnIndex);
  }

  public short getShort(String columnName) throws SQLException {
    return innerResultSet.getShort(columnName);
  }

  public Statement getStatement() throws SQLException {
    return innerResultSet.getStatement();
  }

  public String getString(int columnIndex) throws SQLException {
    return innerResultSet.getString(columnIndex);
  }

  public String getString(String columnName) throws SQLException {
    return innerResultSet.getString(columnName);
  }

  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    return innerResultSet.getTime(columnIndex, cal);
  }

  public Time getTime(int columnIndex) throws SQLException {
    return innerResultSet.getTime(columnIndex);
  }

  public Time getTime(String columnName, Calendar cal) throws SQLException {
    return innerResultSet.getTime(columnName, cal);
  }

  public Time getTime(String columnName) throws SQLException {
    return innerResultSet.getTime(columnName);
  }

  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    return innerResultSet.getTimestamp(columnIndex, cal);
  }

  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    return innerResultSet.getTimestamp(columnIndex);
  }

  public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
    return innerResultSet.getTimestamp(columnName, cal);
  }

  public Timestamp getTimestamp(String columnName) throws SQLException {
    return innerResultSet.getTimestamp(columnName);
  }

  public int getType() throws SQLException {
    return innerResultSet.getType();
  }

  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    return innerResultSet.getUnicodeStream(columnIndex);
  }

  public InputStream getUnicodeStream(String columnName) throws SQLException {
    return innerResultSet.getUnicodeStream(columnName);
  }

  public URL getURL(int columnIndex) throws SQLException {
    return innerResultSet.getURL(columnIndex);
  }

  public URL getURL(String columnName) throws SQLException {
    return innerResultSet.getURL(columnName);
  }

  public SQLWarning getWarnings() throws SQLException {
    return innerResultSet.getWarnings();
  }

  public void insertRow() throws SQLException {
    innerResultSet.insertRow();
  }

  public boolean isAfterLast() throws SQLException {
    return innerResultSet.isAfterLast();
  }

  public boolean isBeforeFirst() throws SQLException {
    return innerResultSet.isBeforeFirst();
  }

  public boolean isFirst() throws SQLException {
    return innerResultSet.isFirst();
  }

  public boolean isLast() throws SQLException {
    return innerResultSet.isLast();
  }

  public boolean last() throws SQLException {
    return innerResultSet.last();
  }

  public void moveToCurrentRow() throws SQLException {
    innerResultSet.moveToCurrentRow();
  }

  public void moveToInsertRow() throws SQLException {
    innerResultSet.moveToInsertRow();
  }

  public boolean next() throws SQLException {
    return innerResultSet.next();
  }

  public boolean previous() throws SQLException {
    return innerResultSet.previous();
  }

  public void refreshRow() throws SQLException {
    innerResultSet.refreshRow();
  }

  public boolean relative(int rows) throws SQLException {
    return innerResultSet.relative(rows);
  }

  public boolean rowDeleted() throws SQLException {
    return innerResultSet.rowDeleted();
  }

  public boolean rowInserted() throws SQLException {
    return innerResultSet.rowInserted();
  }

  public boolean rowUpdated() throws SQLException {
    return innerResultSet.rowUpdated();
  }

  public void setFetchDirection(int direction) throws SQLException {
    innerResultSet.setFetchDirection(direction);
  }

  public void setFetchSize(int rows) throws SQLException {
    innerResultSet.setFetchSize(rows);
  }

  public void updateArray(int columnIndex, Array x) throws SQLException {
    innerResultSet.updateArray(columnIndex, x);
  }

  public void updateArray(String columnName, Array x) throws SQLException {
    innerResultSet.updateArray(columnName, x);
  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    innerResultSet.updateAsciiStream(columnIndex, x, length);
  }

  public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
    innerResultSet.updateAsciiStream(columnName, x, length);
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    innerResultSet.updateBigDecimal(columnIndex, x);
  }

  public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
    innerResultSet.updateBigDecimal(columnName, x);
  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    innerResultSet.updateBinaryStream(columnIndex, x, length);
  }

  public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
    innerResultSet.updateBinaryStream(columnName, x, length);
  }

  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    innerResultSet.updateBlob(columnIndex, x);
  }

  public void updateBlob(String columnName, Blob x) throws SQLException {
    innerResultSet.updateBlob(columnName, x);
  }

  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    innerResultSet.updateBoolean(columnIndex, x);
  }

  public void updateBoolean(String columnName, boolean x) throws SQLException {
    innerResultSet.updateBoolean(columnName, x);
  }

  public void updateByte(int columnIndex, byte x) throws SQLException {
    innerResultSet.updateByte(columnIndex, x);
  }

  public void updateByte(String columnName, byte x) throws SQLException {
    innerResultSet.updateByte(columnName, x);
  }

  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    innerResultSet.updateBytes(columnIndex, x);
  }

  public void updateBytes(String columnName, byte[] x) throws SQLException {
    innerResultSet.updateBytes(columnName, x);
  }

  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    innerResultSet.updateCharacterStream(columnIndex, x, length);
  }

  public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
    innerResultSet.updateCharacterStream(columnName, reader, length);
  }

  public void updateClob(int columnIndex, Clob x) throws SQLException {
    innerResultSet.updateClob(columnIndex, x);
  }

  public void updateClob(String columnName, Clob x) throws SQLException {
    innerResultSet.updateClob(columnName, x);
  }

  public void updateDate(int columnIndex, Date x) throws SQLException {
    innerResultSet.updateDate(columnIndex, x);
  }

  public void updateDate(String columnName, Date x) throws SQLException {
    innerResultSet.updateDate(columnName, x);
  }

  public void updateDouble(int columnIndex, double x) throws SQLException {
    innerResultSet.updateDouble(columnIndex, x);
  }

  public void updateDouble(String columnName, double x) throws SQLException {
    innerResultSet.updateDouble(columnName, x);
  }

  public void updateFloat(int columnIndex, float x) throws SQLException {
    innerResultSet.updateFloat(columnIndex, x);
  }

  public void updateFloat(String columnName, float x) throws SQLException {
    innerResultSet.updateFloat(columnName, x);
  }

  public void updateInt(int columnIndex, int x) throws SQLException {
    innerResultSet.updateInt(columnIndex, x);
  }

  public void updateInt(String columnName, int x) throws SQLException {
    innerResultSet.updateInt(columnName, x);
  }

  public void updateLong(int columnIndex, long x) throws SQLException {
    innerResultSet.updateLong(columnIndex, x);
  }

  public void updateLong(String columnName, long x) throws SQLException {
    innerResultSet.updateLong(columnName, x);
  }

  public void updateNull(int columnIndex) throws SQLException {
    innerResultSet.updateNull(columnIndex);
  }

  public void updateNull(String columnName) throws SQLException {
    innerResultSet.updateNull(columnName);
  }

  public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
    innerResultSet.updateObject(columnIndex, x, scale);
  }

  public void updateObject(int columnIndex, Object x) throws SQLException {
    innerResultSet.updateObject(columnIndex, x);
  }

  public void updateObject(String columnName, Object x, int scale) throws SQLException {
    innerResultSet.updateObject(columnName, x, scale);
  }

  public void updateObject(String columnName, Object x) throws SQLException {
    innerResultSet.updateObject(columnName, x);
  }

  public void updateRef(int columnIndex, Ref x) throws SQLException {
    innerResultSet.updateRef(columnIndex, x);
  }

  public void updateRef(String columnName, Ref x) throws SQLException {
    innerResultSet.updateRef(columnName, x);
  }

  public void updateRow() throws SQLException {
    innerResultSet.updateRow();
  }

  public void updateShort(int columnIndex, short x) throws SQLException {
    innerResultSet.updateShort(columnIndex, x);
  }

  public void updateShort(String columnName, short x) throws SQLException {
    innerResultSet.updateShort(columnName, x);
  }

  public void updateString(int columnIndex, String x) throws SQLException {
    innerResultSet.updateString(columnIndex, x);
  }

  public void updateString(String columnName, String x) throws SQLException {
    innerResultSet.updateString(columnName, x);
  }

  public void updateTime(int columnIndex, Time x) throws SQLException {
    innerResultSet.updateTime(columnIndex, x);
  }

  public void updateTime(String columnName, Time x) throws SQLException {
    innerResultSet.updateTime(columnName, x);
  }

  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    innerResultSet.updateTimestamp(columnIndex, x);
  }

  public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
    innerResultSet.updateTimestamp(columnName, x);
  }

  public boolean wasNull() throws SQLException {
    return innerResultSet.wasNull();
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    return innerResultSet.unwrap(iface);
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return innerResultSet.isWrapperFor(iface);
  }

  public RowId getRowId(int columnIndex) throws SQLException {
    return innerResultSet.getRowId(columnIndex);
  }

  public RowId getRowId(String columnLabel) throws SQLException {
    return innerResultSet.getRowId(columnLabel);
  }

  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    innerResultSet.updateRowId(columnIndex, x);
  }

  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    innerResultSet.updateRowId(columnLabel, x);
  }

  public int getHoldability() throws SQLException {
    return innerResultSet.getHoldability();
  }

  public boolean isClosed() throws SQLException {
    return innerResultSet.isClosed();
  }

  public void updateNString(int columnIndex, String nString) throws SQLException {
    innerResultSet.updateNString(columnIndex, nString);
  }

  public void updateNString(String columnLabel, String nString) throws SQLException {
    innerResultSet.updateNString(columnLabel, nString);
  }

  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    innerResultSet.updateNClob(columnIndex, nClob);
  }

  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    innerResultSet.updateNClob(columnLabel, nClob);
  }

  public NClob getNClob(int columnIndex) throws SQLException {
    return innerResultSet.getNClob(columnIndex);
  }

  public NClob getNClob(String columnLabel) throws SQLException {
    return innerResultSet.getNClob(columnLabel);
  }

  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    return innerResultSet.getSQLXML(columnIndex);
  }

  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    return innerResultSet.getSQLXML(columnLabel);
  }

  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    innerResultSet.updateSQLXML(columnIndex, xmlObject);
  }

  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    innerResultSet.updateSQLXML(columnLabel, xmlObject);
  }

  public String getNString(int columnIndex) throws SQLException {
    return innerResultSet.getNString(columnIndex);
  }

  public String getNString(String columnLabel) throws SQLException {
    return innerResultSet.getNString(columnLabel);
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    return innerResultSet.getNCharacterStream(columnIndex);
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    return innerResultSet.getNCharacterStream(columnLabel);
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    innerResultSet.updateNCharacterStream(columnIndex, x, length);
  }

  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    innerResultSet.updateNCharacterStream(columnLabel, reader, length);
  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    innerResultSet.updateAsciiStream(columnIndex, x, length);
  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    innerResultSet.updateBinaryStream(columnIndex, x, length);
  }

  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    innerResultSet.updateCharacterStream(columnIndex, x, length);
  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    innerResultSet.updateAsciiStream(columnLabel, x, length);
  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    innerResultSet.updateBinaryStream(columnLabel, x, length);
  }

  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    innerResultSet.updateCharacterStream(columnLabel, reader, length);
  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    innerResultSet.updateBlob(columnIndex, inputStream, length);
  }

  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    innerResultSet.updateBlob(columnLabel, inputStream, length);
  }

  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    innerResultSet.updateClob(columnIndex, reader, length);
  }

  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    innerResultSet.updateClob(columnLabel, reader, length);
  }

  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    innerResultSet.updateNClob(columnIndex, reader, length);
  }

  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    innerResultSet.updateNClob(columnLabel, reader, length);
  }

  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    innerResultSet.updateNCharacterStream(columnIndex, x);
  }

  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    innerResultSet.updateNCharacterStream(columnLabel, reader);
  }

  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    innerResultSet.updateAsciiStream(columnIndex, x);
  }

  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    innerResultSet.updateBinaryStream(columnIndex, x);
  }

  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    innerResultSet.updateCharacterStream(columnIndex, x);
  }

  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    innerResultSet.updateAsciiStream(columnLabel, x);
  }

  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    innerResultSet.updateBinaryStream(columnLabel, x);
  }

  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    innerResultSet.updateCharacterStream(columnLabel, reader);
  }

  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    innerResultSet.updateBlob(columnIndex, inputStream);
  }

  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    innerResultSet.updateBlob(columnLabel, inputStream);
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    innerResultSet.updateClob(columnIndex, reader);
  }

  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    innerResultSet.updateClob(columnLabel, reader);
  }

  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    innerResultSet.updateNClob(columnIndex, reader);
  }

  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    innerResultSet.updateNClob(columnLabel, reader);
  }

  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    return innerResultSet.getObject(columnIndex, type);
  }

  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    return innerResultSet.getObject(columnLabel, type);
  }


}
