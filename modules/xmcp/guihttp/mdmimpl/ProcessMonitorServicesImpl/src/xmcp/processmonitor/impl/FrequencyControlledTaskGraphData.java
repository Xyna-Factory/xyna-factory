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

package xmcp.processmonitor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FrequencyControlledTaskStatistics;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderCreationTaskStatistics;

import xmcp.graphs.datatypes.GraphData;
import xmcp.graphs.datatypes.GraphInfo;
import xmcp.graphs.datatypes.GraphSlice;
import xmcp.graphs.datatypes.TimeInterval;
import xmcp.processmonitor.datatypes.GraphDatasource;
import xmcp.processmonitor.datatypes.LoadGraphDataException;
import xmcp.processmonitor.datatypes.TaskId;

public class FrequencyControlledTaskGraphData {
  
  private static class GraphBin {
    private long start;
    private long end;
    private List<Number> numbers = new ArrayList<>();
    private double value = 0.00;
  }
  
  private FrequencyControlledTaskGraphData() {
    
  }
  
  public static List<? extends GraphData> getFrequencyControlledTaskGraphData(GraphInfo graphInfo, GraphDatasource graphDatasource, TaskId taskId) throws LoadGraphDataException {
    List<GraphData> result = new ArrayList<>();
    if(graphInfo.getIntervals() == null || graphInfo.getIntervals().size() != 1)
      throw new LoadGraphDataException("1 Interval is required");
    if(graphInfo.getResolution() == 0) {
      throw new LoadGraphDataException("Resolution is required");
    }
    List<GraphBin> graphBins = generateGraphBins(graphInfo);
    try {
      fillGraphBins(graphDatasource, taskId, graphBins);
      aggregateGraphBins(graphBins, graphDatasource.getType(), graphInfo);
      
      GraphData data = new GraphData();
      List<GraphSlice> slices = new ArrayList<>();
      GraphSlice slice = new GraphSlice();
      slice.setTime(graphInfo.getIntervals().get(0).getTimeAt());
      graphBins.stream().forEach(bin -> {
        slice.addToValues(bin.value);
      });
      slices.add(slice);
      
      data.setSlices(slices);
      result.add(data);
      
      return result;
    } catch (XynaException e) {
      throw new LoadGraphDataException(e.getMessage(), e);
    } 
  }
  
  private static void aggregateGraphBins(List<GraphBin> graphBins, String statisticType, GraphInfo graphInfo) {
    try {
      FrequencyControlledTaskStatistics frequencyControlledTaskStatistics = FrequencyControlledTaskStatistics.valueOf(statisticType);
      aggregateGraphBins(graphBins, frequencyControlledTaskStatistics);
      graphInfo.setYAxisName(frequencyControlledTaskStatistics.getName());
      graphInfo.setYAxisUnit(frequencyControlledTaskStatistics.getUnit());
    } catch (Exception ex) {
      // nothing
    }
    try {
      FrequencyControlledOrderCreationTaskStatistics frequencyControlledOrderCreationTaskStatistics = FrequencyControlledOrderCreationTaskStatistics.valueOf(statisticType);
      aggregateGraphBins(graphBins, frequencyControlledOrderCreationTaskStatistics);
      graphInfo.setYAxisName(frequencyControlledOrderCreationTaskStatistics.getName());
      graphInfo.setYAxisUnit(frequencyControlledOrderCreationTaskStatistics.getUnit());
    } catch (Exception ex) {
      // nothing
    }
  }
  
  private static void aggregateGraphBins(List<GraphBin> graphBins, FrequencyControlledOrderCreationTaskStatistics frequencyControlledOrderCreationTaskStatistics) {
    graphBins.stream().filter(bin -> !bin.numbers.isEmpty()).forEach(bin -> {
      if(frequencyControlledOrderCreationTaskStatistics == FrequencyControlledOrderCreationTaskStatistics.WAITING) {
        // Summe
        for(Number number : bin.numbers) {
          bin.value += number.doubleValue();
        }
      } else if (frequencyControlledOrderCreationTaskStatistics == FrequencyControlledOrderCreationTaskStatistics.EXECUTION_RESPONSE_TIME) {
        // Mittelwert
        double sum = 0;
        for(Number number : bin.numbers) {
          sum += number.doubleValue();
        }
        bin.value = sum / bin.numbers.size();
      }
    });
  }
  
  private static void aggregateGraphBins(List<GraphBin> graphBins, FrequencyControlledTaskStatistics frequencyControlledTaskStatistics) {
    graphBins.stream().filter(bin -> !bin.numbers.isEmpty()).forEach(bin -> {
      switch(frequencyControlledTaskStatistics) {
        case FINISHED:
        case RUNNING:
        case FAILED:
          // Max
          for(Number number : bin.numbers) {
            double value = number.doubleValue();
            if(bin.value < value)
              bin.value = value;
          }
          break;
        case FINISHED_RATE:
        case FAILED_RATE:
        case OVERALL_RESPONSE_TIME:
          // Mittelwert
          double sum = 0.00;
          for(Number number : bin.numbers) {
            sum += number.doubleValue();
          }
          bin.value = sum / bin.numbers.size();
          break;
      }
    });
    switch(frequencyControlledTaskStatistics) {
      case FINISHED:
        double lastPositiveValue = 0.00;
        for (GraphBin bin : graphBins) {
          if(bin.value == 0.00) {
            bin.value = lastPositiveValue;
          } else if(bin.value > 0.00) {
            lastPositiveValue = bin.value;
          }
        }
        break;
      default :
        break;
    }
  }
  
  private static void fillGraphBins(GraphDatasource graphDatasource, TaskId taskId, List<GraphBin> graphBins) throws XynaException {
    FrequencyControlledTaskInformation information = XynaFactory.getPortalInstance().getProcessingPortal().getFrequencyControlledTaskInformation(taskId.getId(), new String[] {graphDatasource.getLabel()});
    Map<Long, Number> x = information.getStatistics(graphDatasource.getLabel());
    if (x != null) {
      for (Entry<Long, Number> e : x.entrySet()) {
        for (GraphBin bin : graphBins) {
          if(e.getKey() >= bin.start && e.getKey() <= bin.end) {
            bin.numbers.add(e.getValue());
          }
        }
      }
    }
  }
  
  private static List<GraphBin> generateGraphBins(GraphInfo graphInfo) {
    TimeInterval interval = graphInfo.getIntervals().get(0);
    long intervalAmount = (interval.getTimeTo() - interval.getTimeAt())  / graphInfo.getResolution();
    List<GraphBin> graphBins = new ArrayList<>();
    for (long i = 0; i < intervalAmount; i++) {
      GraphBin bin = new GraphBin();
      bin.start = interval.getTimeAt() + i * graphInfo.getResolution();
      bin.end = bin.start + graphInfo.getResolution() - 1;
      graphBins.add(bin);
    }
    if(graphBins.isEmpty()) {
      GraphBin bin = new GraphBin();
      bin.start = interval.getTimeAt();
      bin.end = interval.getTimeTo();
      graphBins.add(bin);
    } else {
      GraphBin lastBin = graphBins.get(graphBins.size() - 1);
      if(lastBin.end < interval.getTimeTo()) {
        GraphBin bin = new GraphBin();
        bin.start = lastBin.end + 1;
        bin.end = interval.getTimeTo();
        graphBins.add(bin);
      }
    }
    return graphBins;
  }
  
}
