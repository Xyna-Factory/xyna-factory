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

public class TimeSeriesCreationParameter {
  
  public static interface StorageParameter {
    
  }
  
  public enum DataSourceType {
    COUNTER, GAUGE;
  }
  
  public static class DataSourceParameter {
    
    private String datasourceName;
    private double minValue = -1e100;
    private double maxValue = 1e100;
    private DataSourceType dataSourceType = DataSourceType.GAUGE;
    
    public DataSourceParameter(String name) {
      this.datasourceName = name;
    }

    public String getDataSourceName() {
      return datasourceName;
    }
    
    public DataSourceType getDataSourceType() {
      return dataSourceType;
    }

    public double getMinValue() {
      return minValue;
    }

    public double getMaxValue() {
      return maxValue;
    }

    public void setDataSourceType(DataSourceType type) {
      if (type == null) {
        throw new IllegalArgumentException();
      }
      this.dataSourceType = type;
    }
    
    public void setMinValue(double min) {
      this.minValue = min;
    }
    
    public void setMaxValue(double max) {
      this.maxValue = max;
    }
    
  }

  public final StorageParameter[] storageParameter;
  public final DataSourceParameter datasourceParameter;
  
  public TimeSeriesCreationParameter(StorageParameter[] storageParameter, DataSourceParameter datasourceParameter) {
    this.storageParameter = storageParameter;
    this.datasourceParameter = datasourceParameter;
  }
  
}
