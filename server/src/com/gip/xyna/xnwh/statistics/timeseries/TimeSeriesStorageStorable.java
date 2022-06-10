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
package com.gip.xyna.xnwh.statistics.timeseries;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = TimeSeriesStorageStorable.COL_ID, tableName = TimeSeriesStorageStorable.TABLE_NAME)
public class TimeSeriesStorageStorable extends Storable<TimeSeriesStorageStorable> {

  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME = "timeseriesstorage";
  public static final String COL_ID = "id";
  public static final String COL_STORAGETYPE = "storagetype";
  public static final String COL_STORAGEID = "storageid";
  public static final String COL_TIMESERIES_ID = "timeseriesid";

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_STORAGETYPE)
  private String storageType;

  @Column(name = COL_STORAGEID)
  private String storageId;

  @Column(name = COL_TIMESERIES_ID, index=IndexType.MULTIPLE)
  private long timeseriesId;

  static final ResultSetReader<TimeSeriesStorageStorable> reader = new ResultSetReader<TimeSeriesStorageStorable>() {

    @Override
    public TimeSeriesStorageStorable read(ResultSet rs) throws SQLException {
      TimeSeriesStorageStorable tsss = new TimeSeriesStorageStorable();
      tsss.id = rs.getLong(COL_ID);
      tsss.storageId = rs.getString(COL_STORAGEID);
      tsss.storageType = rs.getString(COL_STORAGETYPE);
      tsss.timeseriesId = rs.getLong(COL_TIMESERIES_ID);
      return tsss;
    }

  };


  @Override
  public ResultSetReader<? extends TimeSeriesStorageStorable> getReader() {
    return reader;
  }

  public TimeSeriesStorageStorable() {
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends TimeSeriesStorageStorable> void setAllFieldsFromData(U data) {
    id = data.getId();
    storageId = data.getStorageId();
    storageType = data.getStorageType();
    timeseriesId = data.getTimeSeriesId();
  }


  public void setStorageType(String storageType) {
    this.storageType = storageType;
  }


  public void setStorageId(String id) {
    this.storageId = id;
  }


  public String getStorageType() {
    return storageType;
  }


  public String getStorageId() {
    return storageId;
  }


  public long getId() {
    return id;
  }


  public long getTimeSeriesId() {
    return timeseriesId;
  }


  public void setTimeSeriesId(long id) {
    timeseriesId = id;
  }

  public static TimeSeriesStorageStorable create() {
    TimeSeriesStorageStorable ts = new TimeSeriesStorageStorable();
    ts.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    return ts;
  }


}
