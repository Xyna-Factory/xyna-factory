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
package com.gip.xyna.persistence.xsor.helper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import com.gip.xyna.xsor.protocol.XSORPayload;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey=TestXCStorable.COL_NAME_PK, tableName=TestXCStorable.TABLENAME)
public class TestXCStorable extends Storable<TestXCStorable> implements XSORPayload {

  private static final long serialVersionUID = -3210922972862815107L;
  
  private static final int STRING_COLUMN_SIZE = 100;
  public static final String TABLENAME = "testxcstorable";
  public static final String COL_NAME_PK = "primaryKey";
  public static final String COL_NAME_BOOLEAN = "booleanColumn";
  public static final String COL_NAME_STRING = "stringColumn";
  public static final String COL_NAME_INT = "intColumn";
  public static final String COL_NAME_LONG = "longColumn";
  public static final String COL_NAME_DOUBLE = "doubleColumn";
  public static final String COL_NAME_FLOAT = "floatColumn";
  
  
  @Column(name=COL_NAME_PK)
  private byte[] primaryKey;
  @Column(name=COL_NAME_BOOLEAN)
  private boolean booleanColumn;
  @Column(name=COL_NAME_STRING, size=STRING_COLUMN_SIZE)
  private String stringColumn;
  @Column(name=COL_NAME_INT)
  private int intColumn;
  @Column(name=COL_NAME_LONG)
  private long longColumn;
  @Column(name=COL_NAME_DOUBLE)
  private double doubleColumn;
  @Column(name=COL_NAME_FLOAT)
  private float floatColumn;
  

  public TestXCStorable() {
  }
  
  
  public TestXCStorable(Random random) {
    primaryKey = new byte[16];
    random.nextBytes(primaryKey);
    booleanColumn = random.nextBoolean();
    byte[] stringColumnbytes = new byte[STRING_COLUMN_SIZE];
    random.nextBytes(stringColumnbytes);
    stringColumn = new String(stringColumnbytes);
    intColumn = random.nextInt();
    doubleColumn = random.nextDouble();
    floatColumn = random.nextFloat();
  }
  
  
  public TestXCStorable(byte[] primaryKey, boolean booleanColumn, String stringColumn, int intColumn, long longColumn, double doubleColumn, float floatColumn) {
    this.primaryKey = primaryKey;
    this.booleanColumn = booleanColumn;
    this.stringColumn = stringColumn;
    this.intColumn = intColumn;
    this.longColumn = longColumn;
    this.doubleColumn = doubleColumn;
    this.floatColumn = floatColumn;
  }

  
  public boolean getBooleanColumn() {
    return booleanColumn;
  }


  
  public String getStringColumn() {
    return stringColumn;
  }


  
  public int getIntColumn() {
    return intColumn;
  }


  
  public long getLongColumn() {
    return longColumn;
  }


  
  public double getDoubleColumn() {
    return doubleColumn;
  }


  
  public float getFloatColumn() {
    return floatColumn;
  }


  
  public void setPrimaryKey(byte[] primaryKey) {
    this.primaryKey = primaryKey;
  }


  
  public void setBooleanColumn(boolean booleanColumn) {
    this.booleanColumn = booleanColumn;
  }


  
  public void setStringColumn(String stringColumn) {
    this.stringColumn = stringColumn;
  }


  
  public void setIntColumn(int intColumn) {
    this.intColumn = intColumn;
  }


  
  public void setLongColumn(long longColumn) {
    this.longColumn = longColumn;
  }


  
  public void setDoubleColumn(double doubleColumn) {
    this.doubleColumn = doubleColumn;
  }


  
  public void setFloatColumn(float floatColumn) {
    this.floatColumn = floatColumn;
  }


  public XSORPayload copyFromByteArray(byte[] arg0, int arg1) {
    // irrelevant
    return null;
  }


  public void copyIntoByteArray(byte[] arg0, int arg1) {
    // irrelevant
  }


  public int xcRecordSize() {
    // irrelevant
    return 0;
  }


  @Override
  public byte[] getPrimaryKey() {
    return primaryKey;
  }


  public static ResultSetReader<? extends TestXCStorable> getStaticReader() {
    return new ResultSetReader<TestXCStorable>() {

      public TestXCStorable read(ResultSet rs) throws SQLException {
        return new TestXCStorable(rs.getBytes(COL_NAME_PK),
                                  rs.getBoolean(COL_NAME_BOOLEAN),
                                  rs.getString(COL_NAME_STRING),
                                  rs.getInt(COL_NAME_INT),
                                  rs.getLong(COL_NAME_LONG),
                                  rs.getDouble(COL_NAME_DOUBLE),
                                  rs.getFloat(COL_NAME_FLOAT));
      }
    };
  }
  
  
  @Override
  public ResultSetReader<? extends TestXCStorable> getReader() {
    return TestXCStorable.getStaticReader();
  }


  @Override
  public <U extends TestXCStorable> void setAllFieldsFromData(U data) {
    this.primaryKey = data.primaryKey;
    this.booleanColumn = data.booleanColumn;
    this.stringColumn = data.stringColumn;
    this.intColumn = data.intColumn;
    this.longColumn = data.longColumn;
    this.doubleColumn = data.doubleColumn;
    this.floatColumn = data.floatColumn;
  }

  
  public static TestXCStorable generateTotallyDifferentTestXCStorable(TestXCStorable master, Random random) {
    byte[] differentBytes = new byte[16];
    random.nextBytes(differentBytes);
    while (Arrays.equals(master.getPrimaryKey(), differentBytes)) {
      random.nextBytes(differentBytes);
    }
    byte[] differentStringColumnbytes = new byte[STRING_COLUMN_SIZE];
    random.nextBytes(differentStringColumnbytes);
    while (new String(differentStringColumnbytes).equals(master.getStringColumn())) {
      random.nextBytes(differentStringColumnbytes);
    }
    int differentInt = random.nextInt();
    while (differentInt == master.getIntColumn()) {
      differentInt = random.nextInt();
    }
    long differentLong = random.nextLong();
    while (differentLong == master.getLongColumn()) {
      differentLong = random.nextLong();
    }
    double differentDouble = random.nextDouble();
    while (differentDouble == master.getDoubleColumn()) {
      differentDouble = random.nextDouble();
    }
    float differentFloat = random.nextFloat();
    while (differentFloat == master.getFloatColumn()) {
      differentFloat = random.nextFloat();
    }
    
    return new TestXCStorable(differentBytes, !(master.getBooleanColumn()), new String(differentStringColumnbytes), differentInt, differentLong, differentDouble, differentFloat);
  }


  public byte[] pkToByteArray(Object o) {
    // irrelevant
    return null;
  }


  public Object byteArrayToPk(byte[] ba) {
    // irrelevant
    return null;
  }


}
