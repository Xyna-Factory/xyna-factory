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

package com.gip.xyna.xfmg.xfmon.fruntimestats.values;

import java.util.concurrent.atomic.AtomicLong;


public class LongStatisticsValue implements NumberStatisticsValue<Long> {

  private static final long serialVersionUID = -8074283265978139586L;
  private AtomicLong value;


  public LongStatisticsValue(long value) {
    this.value = new AtomicLong(value);
  }
  

  public Long getValue() {
    return value.get();
  }


  public NumberStatisticsValue<Long> deepClone() {
    return new LongStatisticsValue(value.get());
  }


  public void merge(StatisticsValue<Long> otherValue) {
    value.getAndAdd(otherValue.getValue());
  }

}
