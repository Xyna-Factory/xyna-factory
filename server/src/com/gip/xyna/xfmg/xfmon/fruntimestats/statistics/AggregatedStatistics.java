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
package com.gip.xyna.xfmg.xfmon.fruntimestats.statistics;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.RuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;


public class AggregatedStatistics<T extends Serializable, O extends StatisticsValue<T>> extends Statistics<T, O> {

  private final static Logger logger = CentralFactoryLogging.getLogger(AggregatedStatistics.class);
  private final StatisticsAggregator<? extends StatisticsValue<?>, O> aggregation;
  
  public AggregatedStatistics(StatisticsPath path, StatisticsAggregator<? extends StatisticsValue<?>, O> aggregation) {
    super(path);
    this.aggregation = aggregation;
  }

  @Override
  public O getValueObject() {
    StatisticsPath testPath = new StatisticsPathImpl();
    StatisticsAggregator curr = aggregation;
    while (curr != null) {
      testPath = testPath.append(curr.getPathpart());
      curr = curr.getNextAggregationPart();
    }
    
    Collection<O> aggregatedResult;
    try {
      aggregatedResult = getRuntimeStatistics().getAggregatedValue(aggregation);
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("Registered AggregatedStatistics '"+getPath()+"' contains an invalid aggregation.",e);
      return null;
    } catch (XFMG_UnknownStatistic e) {
      throw new RuntimeException("",e);
    }
    if (aggregatedResult.size() == 0) {
      return null; // or some null statistics value or error?
    } else if (aggregatedResult.size() == 1) {
      return aggregatedResult.iterator().next();
    } else {
      Iterator<O> resultIter = aggregatedResult.iterator();
      O result = (O) resultIter.next().deepClone();
      while (resultIter.hasNext()) {
        O next = resultIter.next();
        result.merge(next);
      }
      return result;
    }
  }
  
  
  public StatisticsPath getAggregationPath() {
    return StatisticsPathImpl.aggregationStackToPath(aggregation);
  }
  
  protected FactoryRuntimeStatistics getRuntimeStatistics() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
  }

  @Override
  public String getDescription() {
    return "";
  }

}
