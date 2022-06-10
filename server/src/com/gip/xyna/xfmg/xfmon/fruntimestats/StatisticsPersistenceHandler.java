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
package com.gip.xyna.xfmg.xfmon.fruntimestats;

import java.util.Collection;

import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * Handles persistence of {@link PushStatistics}, there are several {@link StatisticsPersistenceStrategy} available, as well as a   
 * default implementation: {@link StorableAggregationStatisticsPersistenceHandler}
 */
public abstract class StatisticsPersistenceHandler {

  private final StatisticsPersistenceStrategy strategy;
  private final StatisticsPath associatedStatistics;

  public StatisticsPersistenceHandler(StatisticsPersistenceStrategy strategy, StatisticsPath associatedStatistics) {
    this.strategy = strategy;
    this.associatedStatistics = associatedStatistics;
  }
  
  public final StatisticsPersistenceStrategy getPersistenceStrategy() {
    return strategy;
  }
  
  StatisticsPath getAssociatedStatisticsPath() {
    return associatedStatistics;
  }
  
  
  public boolean isResponsible(StatisticsPath path) {
    return StatisticsPathImpl.covers(associatedStatistics, path);
  }
  
  
  /**
   * Persisted data should be read out and the method should return all Statistics that could be reconstructed from the data
   * It will then be registered within the RuntimeStatistics
   */
  @SuppressWarnings("rawtypes")
  public abstract Collection<PushStatistics> restoreFromPersistence() throws PersistenceLayerException;
  
  /**
   * In cases of a {@link StatisticsPersistenceStrategy}.SYNCHRONOUNSLY this method will receive the full path to the statistic where a value was pushed
   * In other strategy cases the method will receive {@link StatisticsPathImpl}.ALL_PATH 
   */
  public abstract void persist(StatisticsPath path) throws PersistenceLayerException;
  

  /**
   * will be called once a statistic associated with this {@link StatisticsPersistenceHandler} is unregistered
   */
  public abstract void remove(StatisticsPath path) throws PersistenceLayerException;
  
  
  public static enum StatisticsPersistenceStrategy {
    NEVER, SHUTDOWN, SYNCHRONOUSLY, ASYNCHRONOUSLY;
  }
  
}
