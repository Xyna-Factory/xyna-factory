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

package com.gip.xyna.xfmg.xfmon.fruntimestats.statistics;



import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class SumAggregationPushStatistics<N extends Number, T extends StatisticsValue<N>> extends PushStatistics<N, T> {


  public SumAggregationPushStatistics(StatisticsPath path, T initialValue) {
    super(path, initialValue);
  }


  @Override
  public final void pushValue(T value) {
    lastValue.merge(value);
    if (hasPersistenceHandling()) {
      try {
        handler.persist(getPath());
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      }
    }
  }


}
