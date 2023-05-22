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

package com.gip.xyna.xfmg.xfmon.fruntimestats.values;

import java.util.concurrent.atomic.AtomicInteger;



public class IntegerStatisticsValue implements NumberStatisticsValue<Integer> {

  private static final long serialVersionUID = -1670098546553227500L;
  private AtomicInteger value;


  public IntegerStatisticsValue(int value) {
    this.value = new AtomicInteger(value);
  }


  public Integer getValue() {
    return value.get();
  }


  public NumberStatisticsValue<Integer> deepClone() {
    return new IntegerStatisticsValue(value.get());
  }


  public void merge(StatisticsValue<Integer> otherValue) {
    value.getAndAdd(otherValue.getValue());
  }

}
