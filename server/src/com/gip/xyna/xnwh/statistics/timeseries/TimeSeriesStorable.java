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
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = TimeSeriesStorable.COL_ID, tableName = TimeSeriesStorable.TABLE_NAME)
public class TimeSeriesStorable extends Storable<TimeSeriesStorable> {

  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME = "timeseries";
  public static final String COL_ID = "id";
  public static final String COL_DATASOURCE_NAME = "datasourcename";
  public static final String COL_DATASOURCE_TYPE = "datasourcetype";
  public static final String COL_MIN_VALUE = "minvalue";
  public static final String COL_MAX_VALUE = "maxvalue";

  @Column(name = COL_ID)
  private long id;

  @Column(name = COL_DATASOURCE_NAME)
  private String datasourceName;

  @Column(name = COL_DATASOURCE_TYPE)
  private String datasourceType;

  @Column(name= COL_MIN_VALUE)
  private double minvalue;

  @Column(name = COL_MAX_VALUE)
  private double maxvalue;


  public TimeSeriesStorable(long id) {
    this.id = id;
  }


  public TimeSeriesStorable() {
  }


  private static final ResultSetReader<TimeSeriesStorable> reader = new ResultSetReader<TimeSeriesStorable>() {

    @Override
    public TimeSeriesStorable read(ResultSet rs) throws SQLException {
      TimeSeriesStorable ts = new TimeSeriesStorable();
      ts.id = rs.getLong(COL_ID);
      ts.datasourceName = rs.getString(COL_DATASOURCE_NAME);
      ts.datasourceType = rs.getString(COL_DATASOURCE_TYPE);
      ts.minvalue = rs.getDouble(COL_MIN_VALUE);
      ts.maxvalue = rs.getDouble(COL_MAX_VALUE);
      return ts;
    }

  };


  @Override
  public ResultSetReader<? extends TimeSeriesStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends TimeSeriesStorable> void setAllFieldsFromData(U data) {
    id = data.getId();
    datasourceName = data.getDatasourceName();
    datasourceType = data.getDatasourceType();
    minvalue = data.getMinvalue();
    maxvalue = data.getMaxvalue();
  }


  public long getId() {
    return id;
  }


  public static TimeSeriesStorable create() {
    long id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    TimeSeriesStorable ts = new TimeSeriesStorable(id);
    return ts;
  }


  
  public String getDatasourceName() {
    return datasourceName;
  }


  
  public void setDatasourceName(String datasourceName) {
    this.datasourceName = datasourceName;
  }


  
  public String getDatasourceType() {
    return datasourceType;
  }


  
  public void setDatasourceType(String datasourceType) {
    this.datasourceType = datasourceType;
  }


  
  public double getMinvalue() {
    return minvalue;
  }


  
  public void setMinvalue(double minvalue) {
    this.minvalue = minvalue;
  }


  
  public double getMaxvalue() {
    return maxvalue;
  }


  
  public void setMaxvalue(double maxvalue) {
    this.maxvalue = maxvalue;
  }


}
