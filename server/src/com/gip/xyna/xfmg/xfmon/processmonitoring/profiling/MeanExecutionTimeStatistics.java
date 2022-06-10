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
package com.gip.xyna.xfmg.xfmon.processmonitoring.profiling;

import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;

public class MeanExecutionTimeStatistics extends PushStatistics<Long, LongStatisticsValue> {
  
  private final MeanAggregationBuffer buffer;
  
  public MeanExecutionTimeStatistics(StatisticsPath path) {
    super(path);
    buffer = new MeanAggregationBuffer(ProcessProfiling.MEAN_BUFFER_TIME_FRAME);
  }
  
  public MeanAggregationBuffer getMeanAggregationBuffer() {
    return buffer;
  }
  
  @Override
  public LongStatisticsValue getValueObject() {
    return new LongStatisticsValue(buffer.getLatest());
  }
  
  @Override
  public void pushValue(LongStatisticsValue value) {
    buffer.add(value.getValue());
  }
}
