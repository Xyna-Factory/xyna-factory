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
package com.gip.xyna.utils.db.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class BLOB {

  public static final int UNZIPPED = 0;
  public static final int ZIPPED = 1;
  private ByteArrayOutputStream baos;
  private DeflaterOutputStream gos;
  
  private int type = UNZIPPED;
  
  public BLOB() {
    /* Default-Konstruktor */    
  }
  
  /**
   * Einfacher Konstruktor, danach kann dann mit setDataAsString oder getOutputStream().write(..) der BLOB gefüllt werden
   * @param type ZIPPED oder UNZIPPED
   */
  public BLOB( int type ) {
    this.type = type;
  }
 
  /**
   * Schreibt die Daten aus String data mit Standard-Kodierung in den BLOB
   * @param type ZIPPED oder UNZIPPED
   * @param data
   * @throws IOException
   */
  public BLOB( int type, String data ) throws IOException {
    this.type = type;
    getOutputStream().write( data.getBytes() );
  }
  
  /**
   * Schreibt die Daten aus String data mit Kodierung charsetName in den BLOB
   * @param type ZIPPED oder UNZIPPED
   * @param data
   * @param charsetName
   * @throws IOException
   */
  public BLOB( int type, String data, String charsetName ) throws IOException {
    this.type = type;
    getOutputStream().write( data.getBytes( charsetName ) );
  }
  
 
  /**
   * Direktes Übergeben der BLOB-Daten als ByteArrayOutputStream
   * @param data
   */
  public BLOB( ByteArrayOutputStream data) {
    baos = data;
  }

  
  public OutputStream getOutputStream() throws IOException {
    baos = new ByteArrayOutputStream();
    if( type == ZIPPED ) {
      gos = createZippedOutputStream(baos);
      return gos;
    }
   return baos;
  }
  
  /**
   * Kann überschrieben werden, um andere ZipStreams zu verwenden, oder Einstellungen am Stream zu ändern (z.B. Buffersize).
   * Default ist {@link GZIPOutputStream}
   * @param os
   */
  protected DeflaterOutputStream createZippedOutputStream(OutputStream os) throws IOException {
    return new GZIPOutputStream(os);
  }
 
  /**
   * Schreibt die Daten aus String data mit Standard-Kodierung in den BLOB
   * @param data
   * @throws IOException
   */
  public void setDataAsString( String data ) throws IOException {
    getOutputStream().write( data.getBytes() ); 
  }
  
  /**
   * Schreibt die Daten aus String data mit Kodierung charsetName in den BLOB
   * @param data
   * @param charsetName
   * @throws IOException
   */
  public void setDataAsString( String data, String charsetName ) throws IOException {
    getOutputStream().write( data.getBytes( charsetName ) ); 
  }
  
 
  public void setBLOB(PreparedStatement stmt, int i) throws SQLException {
    if( type == ZIPPED ) {
      try {
        gos.close();
      } catch (IOException e) {
        throw (SQLException)new SQLException( e.getMessage() ).initCause(e);
      }
    }
    byte[] bytes = baos.toByteArray();
    
    stmt.setBinaryStream(i, new ByteArrayInputStream( bytes ), bytes.length);
  }
  
  public String toString() {
    return super.toString() + " length=" + baos.size();
  }

  
  
}
