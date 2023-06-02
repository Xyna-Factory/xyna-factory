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
package com.gip.xyna.xsor.indices.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xsor.indices.definitions.ColumnFunction;
import com.gip.xyna.xsor.indices.definitions.IndexedColumnDefinition;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class TestObject implements XSORPayload {
  
  public final static String TABLE_NAME = "table";
  public final static String PRIMARYKEY_NAME = "primaryKey";
  private Map<String, Object> valueMap = new HashMap<String, Object>();
  private byte[] primaryKey;
  private int internalId;
  
  public TestObject(byte[] primaryKey, AtomicInteger idGenerator) {
    this.primaryKey = primaryKey;
    internalId = idGenerator.incrementAndGet();
  }
  
  public TestObject(byte[] primaryKey, int id) {
    this.primaryKey = primaryKey;
    internalId = id;
  }
  
  public TestObject(Map<String, Object> valueMap) {
    this.valueMap = valueMap;
  }
  
  public TestObject(Map<String, Object> valueMap, AtomicInteger idGenerator) {
    this(valueMap);
    internalId = idGenerator.incrementAndGet();
  }
  
  public TestObject(Map<String, Object> valueMap, int id) {
    this(valueMap);
    internalId = id;
  }
  
  public Object get(String key) {
    return valueMap.get(key);
  }
  
  public void set(String key, Object value) {
    valueMap.put(key, value);
  }
  
  public Set<String> keySet() {
    return valueMap.keySet();
  }
  
  public Map<String, Object> getAll() {
    return valueMap;
  }
  
  public TestObject cloneRestricted(String[] restriction) {
    Map<String, Object> cloneMap = new HashMap<String, Object>();
    for (String key : restriction) {
      cloneMap.put(key, get(key));
    }
    return new TestObject(cloneMap);
  }

  public byte[] getPrimaryKey() {
    return primaryKey;
  }

  public String getTableName() {
    return TABLE_NAME;
  }

  public int getObjectIndex() {
    return internalId;
  }
  
  public void setObjectIndex(int internalId) {
    this.internalId = internalId;
  }

  public void copyIntoByteArray(byte[] ba, int offset) {
    // irrelevant
  }

  public XSORPayload copyFromByteArray(byte[] ba, int offset) {
    // irrelevant
    return null;
  }

  public int xcRecordSize() {
    // irrelevant
    return 0;
  }

  public byte[] pkToByteArray(Object o) {
    // irrelevant
    return null;
  }

  public Object byteArrayToPk(byte[] ba) {
    // irrelevant
    return null;
  }
  
  
  public SearchValue[] generateSearchValuesFromColumnNames(IndexedColumnDefinition[] columns) {
    SearchValue[] values = new SearchValue[columns.length];
    for (int i=0; i < columns.length; i++) {
      IndexedColumnDefinition column = columns[i];
      if (column.isDefinedAsColumnFunction()) {
        ColumnFunction function = column.getColumnFunction();
        List<Object> objects = new ArrayList<Object>();
        for (String columnName : function.getColumns()) {
          objects.add(valueMap.get(columnName));
        }
        values[i] = function.executeFunction(objects);
      } else {
        values[i] = new SearchValue(valueMap.get(column.getColumnName()));
      }
    }
    return values;
  }
  

}
