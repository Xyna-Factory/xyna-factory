/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;

public class MeanAggregationStatisticsValue implements StatisticsValue<Double> {

  private static final long serialVersionUID = 3501896583349230468L;
  private Double mean;
  private Long count;
  
  public MeanAggregationStatisticsValue(Double mean, Long count) {
    this.mean = mean;
    this.count = count;
  }
  
  public Double getValue() {
    return mean;
  }

  public void merge(StatisticsValue<Double> otherValue) {
    Double otherMean = otherValue.getValue();
    Long otherCount;
    if (otherValue instanceof MeanAggregationStatisticsValue) {
      otherCount = ((MeanAggregationStatisticsValue) otherValue).count;
    } else {
      otherCount = 1L;
    }
    mean *= count;
    mean += (otherMean * otherCount);
    mean /= (count + otherCount);
    count += otherCount; 
  }

  public StatisticsValue<Double> deepClone() {
    return new MeanAggregationStatisticsValue(mean, count);
  }
  
  @Override
  public String toString() {
    return Double.toString(mean);
  }
  
}
