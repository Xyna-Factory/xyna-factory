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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = OrderInputSourceSpecificStorable.COL_ID, tableName = OrderInputSourceSpecificStorable.TABLENAME)
public class OrderInputSourceSpecificStorable extends Storable<OrderInputSourceSpecificStorable> {

  private static final long serialVersionUID = 1L;
  public static final String TABLENAME = "inputsourcespecific";
  public static final String COL_ID = "id";
  public static final String COL_SOURCEID = "sourceid";
  public static final String COL_KEY = "key";
  public static final String COL_VALUE = "value";

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_SOURCEID, index=IndexType.MULTIPLE)
  private long sourceId;

  @Column(name = COL_KEY)
  private String key;

  @Column(name = COL_VALUE, size=Integer.MAX_VALUE)
  private String value;


  public OrderInputSourceSpecificStorable() {

  }


  public OrderInputSourceSpecificStorable(long id, long sourceId, String key, String value) {
    this.id = id;
    this.sourceId = sourceId;
    this.key = key;
    this.value = value;
  }


  public static final ResultSetReader<OrderInputSourceSpecificStorable> reader =
      new ResultSetReader<OrderInputSourceSpecificStorable>() {

        public OrderInputSourceSpecificStorable read(ResultSet rs) throws SQLException {
          OrderInputSourceSpecificStorable ret = new OrderInputSourceSpecificStorable();
          ret.id = rs.getLong(COL_ID);
          ret.sourceId = rs.getLong(COL_SOURCEID);
          ret.key = rs.getString(COL_KEY);
          ret.value = rs.getString(COL_VALUE);
          return ret;
        }
      };


  @Override
  public ResultSetReader<? extends OrderInputSourceSpecificStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends OrderInputSourceSpecificStorable> void setAllFieldsFromData(U data) {
    OrderInputSourceSpecificStorable cast = data;
    id = cast.id;
    sourceId = cast.sourceId;
    key = cast.key;
    value = cast.value;
  }


  public long getId() {
    return id;
  }


  public long getSourceId() {
    return sourceId;
  }


  public String getKey() {
    return key;
  }


  public String getValue() {
    return value;
  }

}
