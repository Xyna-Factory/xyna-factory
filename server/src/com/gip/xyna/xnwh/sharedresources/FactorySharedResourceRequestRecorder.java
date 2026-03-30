/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.SumAggregationPushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;



public class FactorySharedResourceRequestRecorder implements SharedResourceRequestRecorder {

  private static final Logger logger = CentralFactoryLogging.getLogger(FactorySharedResourceRequestRecorder.class);
  private static final String requestStartPattern = "Starting Shared Resource Request %d '%s' for resource '%s' against synchronizer '%s'";
  private static final String requestEndPattern = "Finished Shared Resource Request %d in %d ms";
  private static AtomicLong sessionUniqueIdCounter = new AtomicLong();

  private long startTime;
  private long requestId;


  private enum ResultType implements StatisticsPathPart {

    SUCCESS("success"), FAILURE("failure");


    private final String pathPart;


    private ResultType(String pathPart) {
      this.pathPart = pathPart;
    }


    public String getPartName() {
      return pathPart;
    }


    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
  }


  private FactoryRuntimeStatistics getFactoryRuntimeStatistics() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
  }


  private StatisticsPath getStatisticsPath(String resourceType, String requestType, ResultType resultType) {
    StatisticsPath path = PredefinedXynaStatisticsPath.SHAREDRESOURCEMANAGEMENT.append(resourceType).append(requestType);
    if (resultType != null) {
      path = path.append(resultType.getPartName());
    }
    return path;
  }


  @SuppressWarnings("unchecked")
  private PushStatistics<Long, LongStatisticsValue> getOrCreateRequestStatistics(String resourceType, String requestType,
                                                                                 ResultType resultType) {
    try {
      FactoryRuntimeStatistics runtimestats = getFactoryRuntimeStatistics();
      StatisticsPath path = getStatisticsPath(resourceType, requestType, resultType);
      PushStatistics<Long, LongStatisticsValue> statistics = (PushStatistics<Long, LongStatisticsValue>) runtimestats.getStatistic(path);
      if (statistics == null) {
        synchronized (FactorySharedResourceRequestRecorder.class) {
          statistics = (PushStatistics<Long, LongStatisticsValue>) runtimestats.getStatistic(path);
          if (statistics == null) {
            statistics = new SumAggregationPushStatistics<Long, LongStatisticsValue>(path, new LongStatisticsValue(0));
            runtimestats.registerStatistic(statistics);
          }
        }
      }
      return statistics;
    } catch (XFMG_InvalidStatisticsPath | XFMG_StatisticAlreadyRegistered e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void recordStartRequest(String requestType, String resourceType, String synchronizerName) {
    startTime = System.currentTimeMillis();
    requestId = sessionUniqueIdCounter.getAndIncrement();

    if (logger.isDebugEnabled()) {
      logger.debug(String.format(requestStartPattern, requestId, requestType, resourceType, synchronizerName));
    }

    // keep track of number of requests per type and resource
    PushStatistics<Long, LongStatisticsValue> statistics = getOrCreateRequestStatistics(resourceType, requestType, null);
    statistics.pushValue(new LongStatisticsValue(1));
  }


  @Override
  public <T> void recordEndRequest(String requestType, String resourceType, SharedResourceRequestResult<T> result) {
    if (logger.isDebugEnabled()) {
      long endTime = System.currentTimeMillis();
      long requestTime = endTime - startTime;
      logger.debug(String.format(requestEndPattern, requestId, requestTime));
    }

    // keep track of number of successful and failed requests per type and resource
    ResultType resultType = result.isSuccess() ? ResultType.SUCCESS : ResultType.FAILURE;
    PushStatistics<Long, LongStatisticsValue> statistics = getOrCreateRequestStatistics(resourceType, requestType, resultType);
    statistics.pushValue(new LongStatisticsValue(1));
  }

}
