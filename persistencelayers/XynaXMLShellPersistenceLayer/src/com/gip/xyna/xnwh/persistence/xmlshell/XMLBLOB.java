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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;


class XMLBLOB implements Blob {
  
  private byte[] bytes;
  
  public XMLBLOB(byte[] bytes) {
    this.bytes = bytes.clone();    
  }

  public InputStream getBinaryStream() throws SQLException {
    return new ByteArrayInputStream(bytes);
  }

  public byte[] getBytes(long pos, int length) throws SQLException {
    byte[] returnBytes = new byte[length];
    if (pos > Integer.MAX_VALUE) {
      pos = Integer.MAX_VALUE;
    } else if (pos < Integer.MIN_VALUE) {
      pos = Integer.MIN_VALUE;
    }
    System.arraycopy(bytes, (int)pos, returnBytes, 0, length);
    return returnBytes;
  }

  public long length() throws SQLException {
    return bytes.length;
  }

  public long position(byte[] pattern, long start) throws SQLException {
    throw new SQLException("unsupported feature, position in XMLBLOB");
  }

  public long position(Blob pattern, long start) throws SQLException {
    throw new SQLException("unsupported feature, position in XMLBLOB");
  }

  public OutputStream setBinaryStream(long pos) throws SQLException {
    throw new SQLException("unsupported feature, setBinaryStream in XMLBLOB");
  }

  public int setBytes(long pos, byte[] bytes) throws SQLException {
    throw new SQLException("unsupported feature, setBytes in XMLBLOB");
  }
  
  // TODO You'll have to wait till Java6
  /*@Override
  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    throw new SQLException("unsupported feature, position in XMLBLOB");
  }


  @Override
  public void free() throws SQLException {
    throw new SQLException("unsupported feature, position in XMLBLOB");
  }*/

  public int setBytes(long pos, byte[] newBytes, int offset, int len) throws SQLException {
    if (pos > Integer.MAX_VALUE) {
      pos = Integer.MAX_VALUE;
    } else if (pos < Integer.MIN_VALUE) {
      pos = Integer.MIN_VALUE;
    }
    System.arraycopy(newBytes, 0, bytes, (int)pos, len);
    return len;
  }

  public void truncate(long len) throws SQLException {
    byte[] newBytes = new byte[(int)len];
    if (len > Integer.MAX_VALUE) {
      len = Integer.MAX_VALUE;
    } else if (len < Integer.MIN_VALUE) {
      len = Integer.MIN_VALUE;
    }
    System.arraycopy(bytes, 0, newBytes, 0, (int)len);   
    this.bytes = newBytes;
  }

  public void free() throws SQLException {
  }

  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    throw new SQLException("unsupported feature, setBinaryStream in XMLBLOB");
  }

  
}
