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

package com.gip.xyna.xfmg.xfmon.fruntimestats.statistics;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StatisticsPersistenceHandler.StatisticsPersistenceStrategy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class PushStatistics<V extends Serializable, T extends StatisticsValue<V>> extends Statistics<V, T> {


  protected volatile T lastValue;
  protected volatile StatisticsPersistenceHandler handler;

  public PushStatistics(StatisticsPath path) {
    super(path);
  }
  
  public PushStatistics(StatisticsPath path, T initialValue) {
    super(path);
    this.lastValue = initialValue;
  }
  

  public void pushValue(T value) {
    lastValue = value;
    if (hasPersistenceHandling()) {
      try {
        handler.persist(getPath());
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      }
    }
  }
  
  
  public void injectSyncPersistenceHandler(StatisticsPersistenceHandler handler) {
    if (handler.getPersistenceStrategy() == StatisticsPersistenceStrategy.SYNCHRONOUSLY) {
      this.handler = handler;
    }
  }

  
  public boolean hasPersistenceHandling() {
    return handler != null;
  }
  
  
  public StatisticsPersistenceHandler getPersistenceHandler() {
    return handler;
  }
  

  public T getValueObject() {
    return lastValue;
  }
  
  @Override
  public String getDescription() {
    return "";
  }
  

}
