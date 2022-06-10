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
package com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;


/**
 * Some type unsafe predefined {@link StatisticsReducer}. Offer a typeCast via 
 * {@link PredefinedStatisticsReducer}.typeCast(Class&lt;O&gt; clazzOut)
 */
public enum PredefinedStatisticsReducer implements StatisticsReducer<StatisticsValue<?>> {
  
  /**
   * Returns the collection as is
   */
  NO_REDUCTION {
    @Override
    public Collection<StatisticsValue<?>> reduce(Collection<StatisticsValue<?>> in) {
      return in;
    }
  },
  /**
   * Performs default reduction by calling {@link StatisticsValue}.deepClone on the first
   * element and then merging the clone with all received values.
   */
  DEFAULT {
    @Override
    public Collection<StatisticsValue<?>> reduce(Collection<StatisticsValue<?>> in) {
      if (in.size() <= 1) {
        return in;
      } else {
        Iterator<StatisticsValue<?>> iterator = in.iterator();
        
        StatisticsValue result = iterator.next();
        StatisticsValue clone = result.deepClone();
        while (iterator.hasNext()) {
          clone.merge(iterator.next());
        }
        Collection<StatisticsValue<?>> resultCollection = new ArrayList<StatisticsValue<?>>();
        resultCollection.add(clone);
        return resultCollection;
      }
    }
  };
  
  public <O extends StatisticsValue<?>> StatisticsReducer<O> typeCast(Class<O> clazzIn) {
    return (StatisticsReducer<O>) this;
  }

  public abstract Collection<StatisticsValue<?>> reduce(Collection<StatisticsValue<?>> in);

}
