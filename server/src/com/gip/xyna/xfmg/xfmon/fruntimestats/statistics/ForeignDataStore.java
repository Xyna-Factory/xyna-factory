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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;


public abstract class ForeignDataStore<H> {

  private final static Logger logger = CentralFactoryLogging.getLogger(ForeignDataStore.class);
  
  protected Map<Object, H> store;
  private Map<String, StatisticsPathPart> mapping;  
  
  public ForeignDataStore(Map<String, StatisticsPathPart> mapping) {
    store = new HashMap<Object, H>();
    if (logger.isDebugEnabled()) {
      logger.debug("store #" + store.size());
    }
    this.mapping = mapping;
  }
  
  
  public abstract Object getKey(H holder);
  
  public abstract StatisticsPath getPathToHolder(H holder);
  
  public abstract Serializable getValueFromHolder(StatisticsPath path);
  
  public abstract Collection<H> reload();
  
  public void refresh() {
    Collection<H> reload = reload();
    Set<Object> presentKeys = new HashSet<Object>(store.keySet()); // defensive copy
    Set<Object> reloadedKeys = new HashSet<Object>();
    for (H holder : reload) {
      Object newKey = getKey(holder);
      reloadedKeys.add(newKey);
      if (presentKeys.contains(newKey)) {
        store.put(newKey, holder); // just overwrite new values
      } else {
        try {
          addToStore(holder);  // register new
        } catch (XFMG_InvalidStatisticsPath e) {
          throw new RuntimeException("", e);
        } catch (XFMG_StatisticAlreadyRegistered e) {
        }
      }
    }
    // there might now be some to remove
    presentKeys.removeAll(reloadedKeys);
    for (Object key : presentKeys) {
      removeFromStore(key);
    }
  }
  
  public void addToStore(H holder) throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    store.put(getKey(holder), holder);
    registerPuller(holder);
  }
  
  
  public H removeFromStore(Object key) {
    H holder = store.get(key);
    try {
      getRuntimeStatistics().unregisterStatistic(getPathToHolder(holder).append(StatisticsPathImpl.ALL));
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("",e);
    }
    store.remove(getKey(holder));
    return holder;
  }
  
  
  public StatisticsValue getDataFromStore(StatisticsPath path) {
    Serializable value = getValueFromHolder(path);
    StatisticsValue statValue;
    if (value instanceof Integer) {
      statValue = new IntegerStatisticsValue((Integer) value);
    } else if (value instanceof Long) {
      statValue = new LongStatisticsValue((Long) value);
    } else {
      // warn if not string?
      statValue = new StringStatisticsValue(value.toString());
    }
    return statValue;
  }
  
  public void registerPuller(H holder) throws XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    StatisticsPath pathToHolder = getPathToHolder(holder);
    for (Entry<String, StatisticsPathPart> holdValue : mapping.entrySet()) {
      ForeignDataPuller puller = new ForeignDataPuller(pathToHolder.append(holdValue.getValue()));
      getRuntimeStatistics().registerStatistic(puller);
    }
  }
  
  
  protected FactoryRuntimeStatistics getRuntimeStatistics() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
  }
  
  
  private class ForeignDataPuller extends PullStatistics {

    public ForeignDataPuller(StatisticsPath path) {
      super(path);
    }

    @Override
    public StatisticsValue getValueObject() {
      return getDataFromStore(path);
    }

    @Override
    public String getDescription() {
      return "";
    }
    
  }
  
  
}
