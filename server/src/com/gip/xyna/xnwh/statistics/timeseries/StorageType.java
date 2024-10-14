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
package com.gip.xyna.xnwh.statistics.timeseries;

import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.DataSourceParameter;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesCreationParameter.StorageParameter;

public interface StorageType {
  
  public String create(StorageParameter[] parameter, DataSourceParameter datasourceParameter);
  
  public void addData(String id, long timeMS, Number value);
  
  /**
   * @see TimeSeries#getData(long, long, boolean, long, AggregationType)
   */
  public FetchedData getData(String id, long starttimeMS, long endtimeMS, boolean calculateDiffs, long resolutionMS, AggregationType aggregationType);
  
  public StoredMetaData getMetaData(String id);
  
  public void delete(String id);

  public StorageParameter[] getParameter(String id);
  
  public void shutdown();
  
}
