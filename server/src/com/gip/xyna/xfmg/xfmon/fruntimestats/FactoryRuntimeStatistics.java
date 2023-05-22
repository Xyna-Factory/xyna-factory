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

package com.gip.xyna.xfmg.xfmon.fruntimestats;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public interface FactoryRuntimeStatistics {
  
  public void registerStatistic(Statistics statistics) throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered;
  
  public void unregisterStatistic(StatisticsPath path) throws XFMG_InvalidStatisticsPath;
  
  public <O extends Serializable> StatisticsValue<O> getStatisticsValue(StatisticsPath path, boolean clustered) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic;
  
  public Statistics getStatistic(StatisticsPath path) throws XFMG_InvalidStatisticsPath;
  
  public <O extends StatisticsValue<?>> Collection<O> getAggregatedValue(StatisticsAggregator<? extends StatisticsValue<?>, O> aggregation) throws XFMG_InvalidStatisticsPath, XFMG_UnknownStatistic;
  
  public Map<String, Serializable> discoverStatistics(boolean clustered);
  
  public Collection<PushStatistics> registerStatisticsPersistenceHandler(StatisticsPersistenceHandler persistenceHandler) throws PersistenceLayerException, XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered;

}
