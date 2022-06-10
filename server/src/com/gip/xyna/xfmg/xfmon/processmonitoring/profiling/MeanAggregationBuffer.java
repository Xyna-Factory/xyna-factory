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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;


// TODO relax locking?
public class MeanAggregationBuffer {
  
  private final long maxMillisBeforeAggregation;
  private SortedSet<MeanAggregationBuffer.TimeAwareData> data;
  private double totalMedian = 0.0;
  private long totalAggregatedValues = 0;
  
  public MeanAggregationBuffer(long maxMillisBeforeAggregation) {
    this.maxMillisBeforeAggregation = maxMillisBeforeAggregation;
    data = Collections.synchronizedSortedSet(new TreeSet<MeanAggregationBuffer.TimeAwareData>());
  }
  
  public void add(long value) {
    data.add(new TimeAwareData(value));
    aggregate();
  }
  
  public MeanAggregationStatisticsValue getTotal() {
    double median;
    long count;
    synchronized (data) {
      if (totalAggregatedValues == 0 && data.size() == 0) {
        return new MeanAggregationStatisticsValue(0.0, 1L);
      }
      median = totalMedian * totalAggregatedValues;
      count  = totalAggregatedValues;
      for (MeanAggregationBuffer.TimeAwareData timeAwareData : data) {
        median += timeAwareData.data;
      }
      median /= (data.size() + totalAggregatedValues);
      count += data.size();
    }
    return new MeanAggregationStatisticsValue(median, count);
  }
  
  
  public MeanAggregationStatisticsValue getFromTimespan(long interval) {
    if (interval > maxMillisBeforeAggregation) {
      return getTotal();
    } else {
      MeanAggregationBuffer.TimeAwareData border = new TimeAwareData(0, System.currentTimeMillis() - interval);
      SortedSet<MeanAggregationBuffer.TimeAwareData> fittingData = data.tailSet(border);
      if (fittingData.size() > 0) {
        double median = 0;
        long count = 1;
        synchronized (data) {
          for (MeanAggregationBuffer.TimeAwareData timeAwareData : fittingData) {
            median += timeAwareData.data;
          }
          median /= fittingData.size();
          count = fittingData.size();
        }
        return new MeanAggregationStatisticsValue(median, count);
      } else {
        return new MeanAggregationStatisticsValue(0.0, 1L);
      }
    }
  }
  
  
  public long getLatest() {
    if (data.size() <= 0) {
      return 0;
    } else {
      return data.last().data;
    }
  }
  
  
  public void aggregate() {
    MeanAggregationBuffer.TimeAwareData border = new TimeAwareData(0, System.currentTimeMillis() - maxMillisBeforeAggregation);
    synchronized (data) {
      SortedSet<MeanAggregationBuffer.TimeAwareData> oldData = data.headSet(border);
      if (oldData.size() > 0) {
        totalMedian *= totalAggregatedValues;
        for (MeanAggregationBuffer.TimeAwareData timeAwareData : oldData) {
          totalMedian += timeAwareData.data;
        }
        totalAggregatedValues += oldData.size();
        totalMedian /= totalAggregatedValues;
        data.removeAll(oldData);
      }
    }
  }
    
  
  private static class TimeAwareData implements Comparable<MeanAggregationBuffer.TimeAwareData> {
    
    private long data;
    private long date;
    
    private TimeAwareData(long data) {
      this(data, System.currentTimeMillis());
    }
    
    private TimeAwareData(long data, long date) {
      this.data = data;
      this.date = date;
    }
    
    public int compareTo(MeanAggregationBuffer.TimeAwareData o) {
      return (int) (date - o.date);
    }
    
  }
  
}
