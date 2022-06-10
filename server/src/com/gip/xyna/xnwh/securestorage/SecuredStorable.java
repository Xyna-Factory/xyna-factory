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
package com.gip.xyna.xnwh.securestorage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;


@Persistable(primaryKey="id", tableName=SecuredStorable.TABLE_NAME, tableProperties = {StorableProperty.PROTECTED})
public class SecuredStorable extends Storable<SecuredStorable> {
  
  private static final long serialVersionUID = 240942976467242027L;
  
  public static final String TABLE_NAME = "securestorage";
  
  public static enum DataType {
    JAVAOBJECT, STRING, INTEGER, LONG, BYTE, SHORT, FLOAT, DOUBLE;
  }
  
  @Column(name="id", size=255) //achtung bei höheren werten. bei mysql können indizes maximal 767 bytes lang sein (encoding utf8 kann die stringlänger vervielfachen)
  private String id;
  @Column(name="encryptedData", type=ColumnType.BYTEARRAY)
  private byte[] encryptedData;
  @Column(name="dataType", size=10)
  private String dataType;

  public SecuredStorable(String id, byte[] encryptedData, DataType type) {
    this.id = id;
    this.encryptedData = encryptedData;
    dataType = type.toString();
  }
  
  public SecuredStorable() {
    //für storable benötigt
  }
  
  
  public SecuredStorable(String identifier) {
    this.id = identifier;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  private static ResultSetReader<? extends SecuredStorable> reader = new ResultSetReader<SecuredStorable>() {

    public SecuredStorable read(ResultSet rs) throws SQLException {
      SecuredStorable ret = new SecuredStorable();
      ret.id = rs.getString("id");
      ret.encryptedData = readByteArrayDirectlyFromResultSet(rs, "encryptedData");
      ret.dataType = rs.getString("dataType");
      if (rs.wasNull()) {
        ret.dataType = DataType.JAVAOBJECT.toString();
      }
      return ret;
    }
    
  };
  
  @Override
  public ResultSetReader<? extends SecuredStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends SecuredStorable> void setAllFieldsFromData(U data) {
    SecuredStorable cast = data;
    id = cast.id;
    encryptedData = cast.encryptedData;
    dataType = cast.dataType;
  }

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  public String getDataType() {
    return dataType;
  }
  
  public DataType getDataTypeEnum() {
    return DataType.valueOf(dataType);
  }
  
  public byte[] getEncryptedData() {
    return encryptedData;
  }

  
  public void setEncryptedData(byte[] encryptedData) {
    this.encryptedData = encryptedData;
  }
  

  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    if (dataType == null) {
      dataType = DataType.JAVAOBJECT.toString();
    }
  }
  
}
