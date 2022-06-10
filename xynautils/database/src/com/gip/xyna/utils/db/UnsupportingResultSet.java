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


/**
 * ResultSet Implementation die in allen Methoden eine unsupported Exception wirft. Geeignet, um davon abzuleiten und
 * nur geringe Featureanzahl zu implementieren.
 */
public class UnsupportingResultSet implements ResultSet {

  public static final String UNSUPPORTED_MESSAGE = "unsupported resultset operation.";


  private SQLException unsupportedException() {
    return new SQLException(UNSUPPORTED_MESSAGE);
  }


  public boolean absolute(int row) throws SQLException {
    throw unsupportedException();
  }


  public void afterLast() throws SQLException {
    throw unsupportedException();
  }


  public void beforeFirst() throws SQLException {
    throw unsupportedException();
  }


  public void cancelRowUpdates() throws SQLException {
    throw unsupportedException();
  }


  public void clearWarnings() throws SQLException {
    throw unsupportedException();
  }


  public void close() throws SQLException {
    throw unsupportedException();
  }


  public void deleteRow() throws SQLException {
    throw unsupportedException();
  }


  public int findColumn(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public boolean first() throws SQLException {
    throw unsupportedException();
  }


  public Array getArray(int i) throws SQLException {
    throw unsupportedException();
  }


  public Array getArray(String colName) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getAsciiStream(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getAsciiStream(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public BigDecimal getBigDecimal(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    throw unsupportedException();
  }


  public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getBinaryStream(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getBinaryStream(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Blob getBlob(int i) throws SQLException {
    throw unsupportedException();
  }


  public Blob getBlob(String colName) throws SQLException {
    throw unsupportedException();
  }


  public boolean getBoolean(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public boolean getBoolean(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public byte getByte(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public byte getByte(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public byte[] getBytes(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public byte[] getBytes(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Reader getCharacterStream(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public Reader getCharacterStream(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Clob getClob(int i) throws SQLException {
    throw unsupportedException();
  }


  public Clob getClob(String colName) throws SQLException {
    throw unsupportedException();
  }


  public int getConcurrency() throws SQLException {
    throw unsupportedException();
  }


  public String getCursorName() throws SQLException {
    throw unsupportedException();
  }


  public Date getDate(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public Date getDate(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Date getDate(int columnIndex, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public Date getDate(String columnName, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public double getDouble(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public double getDouble(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public int getFetchDirection() throws SQLException {
    throw unsupportedException();
  }


  public int getFetchSize() throws SQLException {
    throw unsupportedException();
  }


  public float getFloat(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public float getFloat(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public int getInt(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public int getInt(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public long getLong(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public long getLong(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public ResultSetMetaData getMetaData() throws SQLException {
    throw unsupportedException();
  }


  public Object getObject(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public Object getObject(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
    throw unsupportedException();
  }


  public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException {
    throw unsupportedException();
  }


  public Ref getRef(int i) throws SQLException {
    throw unsupportedException();
  }


  public Ref getRef(String colName) throws SQLException {
    throw unsupportedException();
  }


  public int getRow() throws SQLException {
    throw unsupportedException();
  }


  public short getShort(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public short getShort(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Statement getStatement() throws SQLException {
    throw unsupportedException();
  }


  public String getString(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public String getString(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Time getTime(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public Time getTime(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Time getTime(int columnIndex, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public Time getTime(String columnName, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public Timestamp getTimestamp(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public Timestamp getTimestamp(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
    throw unsupportedException();
  }


  public int getType() throws SQLException {
    throw unsupportedException();
  }


  public URL getURL(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public URL getURL(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getUnicodeStream(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public InputStream getUnicodeStream(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public SQLWarning getWarnings() throws SQLException {
    throw unsupportedException();
  }


  public void insertRow() throws SQLException {
    throw unsupportedException();
  }


  public boolean isAfterLast() throws SQLException {
    throw unsupportedException();
  }


  public boolean isBeforeFirst() throws SQLException {
    throw unsupportedException();
  }


  public boolean isFirst() throws SQLException {
    throw unsupportedException();
  }


  public boolean isLast() throws SQLException {
    throw unsupportedException();
  }


  public boolean last() throws SQLException {
    throw unsupportedException();
  }


  public void moveToCurrentRow() throws SQLException {
    throw unsupportedException();
  }


  public void moveToInsertRow() throws SQLException {
    throw unsupportedException();
  }


  public boolean next() throws SQLException {
    throw unsupportedException();
  }


  public boolean previous() throws SQLException {
    throw unsupportedException();
  }


  public void refreshRow() throws SQLException {
    throw unsupportedException();
  }


  public boolean relative(int rows) throws SQLException {
    throw unsupportedException();
  }


  public boolean rowDeleted() throws SQLException {
    throw unsupportedException();
  }


  public boolean rowInserted() throws SQLException {
    throw unsupportedException();
  }


  public boolean rowUpdated() throws SQLException {
    throw unsupportedException();
  }


  public void setFetchDirection(int direction) throws SQLException {
    throw unsupportedException();
  }


  public void setFetchSize(int rows) throws SQLException {
    throw unsupportedException();
  }


  public void updateArray(int columnIndex, Array x) throws SQLException {
    throw unsupportedException();
  }


  public void updateArray(String columnName, Array x) throws SQLException {
    throw unsupportedException();
  }


  public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateBlob(int columnIndex, Blob x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBlob(String columnName, Blob x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBoolean(String columnName, boolean x) throws SQLException {
    throw unsupportedException();
  }


  public void updateByte(int columnIndex, byte x) throws SQLException {
    throw unsupportedException();
  }


  public void updateByte(String columnName, byte x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBytes(int columnIndex, byte[] x) throws SQLException {
    throw unsupportedException();
  }


  public void updateBytes(String columnName, byte[] x) throws SQLException {
    throw unsupportedException();
  }


  public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
    throw unsupportedException();
  }


  public void updateClob(int columnIndex, Clob x) throws SQLException {
    throw unsupportedException();
  }


  public void updateClob(String columnName, Clob x) throws SQLException {
    throw unsupportedException();
  }


  public void updateDate(int columnIndex, Date x) throws SQLException {
    throw unsupportedException();
  }


  public void updateDate(String columnName, Date x) throws SQLException {
    throw unsupportedException();
  }


  public void updateDouble(int columnIndex, double x) throws SQLException {
    throw unsupportedException();
  }


  public void updateDouble(String columnName, double x) throws SQLException {
    throw unsupportedException();
  }


  public void updateFloat(int columnIndex, float x) throws SQLException {
    throw unsupportedException();
  }


  public void updateFloat(String columnName, float x) throws SQLException {
    throw unsupportedException();
  }


  public void updateInt(int columnIndex, int x) throws SQLException {
    throw unsupportedException();
  }


  public void updateInt(String columnName, int x) throws SQLException {
    throw unsupportedException();
  }


  public void updateLong(int columnIndex, long x) throws SQLException {
    throw unsupportedException();
  }


  public void updateLong(String columnName, long x) throws SQLException {
    throw unsupportedException();
  }


  public void updateNull(int columnIndex) throws SQLException {
    throw unsupportedException();
  }


  public void updateNull(String columnName) throws SQLException {
    throw unsupportedException();
  }


  public void updateObject(int columnIndex, Object x) throws SQLException {
    throw unsupportedException();
  }


  public void updateObject(String columnName, Object x) throws SQLException {
    throw unsupportedException();
  }


  public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
    throw unsupportedException();
  }


  public void updateObject(String columnName, Object x, int scale) throws SQLException {
    throw unsupportedException();
  }


  public void updateRef(int columnIndex, Ref x) throws SQLException {
    throw unsupportedException();
  }


  public void updateRef(String columnName, Ref x) throws SQLException {
    throw unsupportedException();
  }


  public void updateRow() throws SQLException {
    throw unsupportedException();
  }


  public void updateShort(int columnIndex, short x) throws SQLException {
    throw unsupportedException();
  }


  public void updateShort(String columnName, short x) throws SQLException {
    throw unsupportedException();
  }


  public void updateString(int columnIndex, String x) throws SQLException {
    throw unsupportedException();
  }


  public void updateString(String columnName, String x) throws SQLException {
    throw unsupportedException();
  }


  public void updateTime(int columnIndex, Time x) throws SQLException {
    throw unsupportedException();
  }


  public void updateTime(String columnName, Time x) throws SQLException {
    throw unsupportedException();
  }


  public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    throw unsupportedException();
  }


  public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
    throw unsupportedException();
  }


  public boolean wasNull() throws SQLException {
    throw unsupportedException();
  }

  public int getHoldability() throws SQLException {
    throw unsupportedException();
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    throw unsupportedException();
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    throw unsupportedException();
  }

  public NClob getNClob(int columnIndex) throws SQLException {
    throw unsupportedException();
  }

  public NClob getNClob(String columnLabel) throws SQLException {
    throw unsupportedException();
  }

  public String getNString(int columnIndex) throws SQLException {
    throw unsupportedException();
  }

  public String getNString(String columnLabel) throws SQLException {
    throw unsupportedException();
  }

  public RowId getRowId(int columnIndex) throws SQLException {
    throw unsupportedException();
  }

  public RowId getRowId(String columnLabel) throws SQLException {
    throw unsupportedException();
  }

  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    throw unsupportedException();
  }

  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    throw unsupportedException();
  }

  public boolean isClosed() throws SQLException {
    throw unsupportedException();
  }

  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    throw unsupportedException();
  }

  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    throw unsupportedException();
  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    throw unsupportedException();
  }

  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    throw unsupportedException();
  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    throw unsupportedException();
  }

  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    throw unsupportedException();
  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw unsupportedException();
  }

  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    throw unsupportedException();
  }

  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    throw unsupportedException();
  }

  public void updateNString(int columnIndex, String nString) throws SQLException {
    throw unsupportedException();
  }

  public void updateNString(String columnLabel, String nString) throws SQLException {
    throw unsupportedException();
  }

  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    throw unsupportedException();
  }

  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    throw unsupportedException();
  }

  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    throw unsupportedException();
  }

  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    throw unsupportedException();
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    throw unsupportedException();
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw unsupportedException();
  }

  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    throw unsupportedException();
  }

  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    throw unsupportedException();
  }

}
