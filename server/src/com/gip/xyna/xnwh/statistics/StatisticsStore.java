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
package com.gip.xyna.xnwh.statistics;

import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.statistics.timeseries.TimeSeriesManagement;

public class StatisticsStore extends Section {
  
 private TimeSeriesManagement timeSeriesManagement;

  public StatisticsStore() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return "StatisticsStore";
  }

  @Override
  protected void init() throws XynaException {
    timeSeriesManagement = new TimeSeriesManagement();
    deployFunctionGroup(timeSeriesManagement);
  }

  public TimeSeriesManagement getTimeSeriesManagement() {
    return timeSeriesManagement;
  }

}
