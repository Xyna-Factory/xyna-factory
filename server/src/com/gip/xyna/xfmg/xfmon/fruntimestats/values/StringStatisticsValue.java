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



public class StringStatisticsValue implements StatisticsValue<String> {

  private static final long serialVersionUID = 8326448706201730539L;
  private String value;


  public StringStatisticsValue(String value) {
    this.value = value;
  }


  public String getValue() {
    return value;
  }


  public StatisticsValue<String> deepClone() {
    return new StringStatisticsValue(value);
  }


  public void merge(StatisticsValue<String> otherValue) {
    // ntbd
  }

}
