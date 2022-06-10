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
package com.gip.xyna.xnwh.persistence.mysql;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;

import com.gip.xyna.utils.db.WrappedResultSet;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Storable;


// FIXME duplicated class from OraclePersistenceLayer
public class ResultSetWrapperReadingZippedBlobs extends WrappedResultSet {

  public static final String UNSUPPORTED_MESSAGE = "unsupported operation";


  //FIXME getBinaryStream überschreiben?
  //FIXME update-Methoden überschreiben
  
  private Class<? extends Storable> storableClass;

  public ResultSetWrapperReadingZippedBlobs(ResultSet rs) {
    super(rs);
  }
  
  public ResultSetWrapperReadingZippedBlobs(ResultSet rs, Class<? extends Storable> storableClass) {
    this(rs);
    this.storableClass = storableClass;
  }


  @Override
  public Blob getBlob(int i) throws SQLException {
    return new WrappedZippedBlob(super.getBlob(i));
  }


  @Override
  public Blob getBlob(String colName) throws SQLException {
    if (storableClass != null) {
      Column[] columns = Storable.getColumns(storableClass);
      for (Column column : columns) {
        if (column.type() == ColumnType.BYTEARRAY &&
            column.name().equalsIgnoreCase(colName)) {
          return super.getBlob(colName);
        }
      }
    }
    return new WrappedZippedBlob(super.getBlob(colName));
  }

  private static class ConfigurableGZIPInputStream extends GZIPInputStream {

    public ConfigurableGZIPInputStream(InputStream in) throws IOException {
      super(in);      
    }
    
  }

  /**
   * unterstützt getBinaryStream zipped, sonst nichts
   */
  private static class WrappedZippedBlob implements Blob {

    private Blob innerBlob;
    private GZIPInputStream s;

    public WrappedZippedBlob(Blob blob) {
      this.innerBlob = blob;
    }


    public InputStream getBinaryStream() throws SQLException {
      if (s == null) {
        try {
          s = new GZIPInputStream(innerBlob.getBinaryStream());
        } catch (IOException e) {
          throw (SQLException) new SQLException("could not create zipinputstream").initCause(e);
        }
      }
      return s;
    }


    public byte[] getBytes(long pos, int length) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public long length() throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public long position(Blob pattern, long start) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public long position(byte[] pattern, long start) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public OutputStream setBinaryStream(long pos) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public int setBytes(long pos, byte[] bytes) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public void truncate(long len) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }


    public void free() throws SQLException {
      if (s == null) {
        return;
      }
      try {
        s.close();
        s = null;
      } catch (IOException e) {
        throw new SQLException("Close of GZIPInputStream failed.", e);
      }
    }


    public InputStream getBinaryStream(long pos, long length) throws SQLException {
      throw new SQLException(UNSUPPORTED_MESSAGE);
    }

  }

}
