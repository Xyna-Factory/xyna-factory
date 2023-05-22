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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.gip.xyna.utils.timing.SlidingDataWindow;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.HistogramColumn;


public class XynaSchedulerStatistics {
  
  private SlidingDataWindow schedulerRunsLast5Min = SlidingWindowSizes.last5Min.create();
  private SlidingDataWindow schedulerRunsLast60Min = SlidingWindowSizes.last60Min.create();

  private EnumMap<HistogramColumn,SlidingDataWindow> slidingWindows_last60Min;
  
  public XynaSchedulerStatistics() {
    slidingWindows_last60Min = new EnumMap<HistogramColumn,SlidingDataWindow>(HistogramColumn.class);
    for( HistogramColumn hc : HistogramColumn.values() ) {
      if( ! hc.isValue() ) continue;
      slidingWindows_last60Min.put(hc, SlidingWindowSizes.last60Min.create());
    }
  }
  
  public SchedulerInformationBean getInformationBeanHistogram() {
    SchedulerInformationBean sib = new SchedulerInformationBean();
    long now = System.currentTimeMillis();
    sib.setHistogram(SlidingWindowSizes.last60Min.createHistogram(now,slidingWindows_last60Min));
    return sib;
  }

  private static enum SlidingWindowSizes {
    last5Min(5*60*10, 100), //100ms genauigkeit, 5 min l�nge
    last60Min(60*60*2, 500); // 500ms genauigkeit, 60 minuten l�nge
    //custom(); //TODO aus Property einmal zu Serverstart
    
    private int number;
    private long width;

    SlidingWindowSizes(int number, long width) {
      this.number = number;
      this.width = width;
    }

    public SlidingDataWindow create() {
      return new SlidingDataWindow(number, width);
    }

    public List<EnumMap<HistogramColumn,Number>> createHistogram(long now, EnumMap<HistogramColumn,SlidingDataWindow> slidingDataWindows) {
      List<EnumMap<HistogramColumn,Number>> histogram = new ArrayList<EnumMap<HistogramColumn,Number>>();
      for( HistogramColumn hc : HistogramColumn.values() ) {
        if( hc.isValue() ) {
          slidingDataWindows.get(hc).size(now); //SlidingDataWindow aktualisieren auf aktuelle Zeit
        }
      }
      long timestamp = now;
      boolean hasData = true;
      do {
        timestamp -= width;
        EnumMap<HistogramColumn,Number> histoData = new EnumMap<HistogramColumn,Number>(HistogramColumn.class);
        histoData.put(HistogramColumn.Timestamp, timestamp);
        histoData.put(HistogramColumn.Width, width);
        for( HistogramColumn hc : HistogramColumn.values() ) {
          if( hc.isValue() ) {
            double value = slidingDataWindows.get(hc).get(timestamp);
            if( value == Double.MIN_VALUE ) {
              hasData = false;
            }
            histoData.put( hc, value );
          }
        }
        if( hasData ) {
          histogram.add(histoData);
        }
      } while ( hasData );
      return histogram;
    }
    
  }

  public void addToSlidingWindows(HistogramColumn hc, long timestamp, int value) {
    slidingWindows_last60Min.get(hc).add(timestamp,value);
  }

  public void start(long start, long totalSchedulerRuns) { 
    schedulerRunsLast5Min.increment(start);
    schedulerRunsLast60Min.increment(start);
  }

  public void addSchedulerRuns(SchedulerInformationBean sib) {
    long currentTime = System.currentTimeMillis();
    sib.setSchedulerRunsLast5Minutes((int) schedulerRunsLast5Min.size(currentTime));
    sib.setSchedulerRunsLast60Minutes((int) schedulerRunsLast60Min.size(currentTime));
  }

}
