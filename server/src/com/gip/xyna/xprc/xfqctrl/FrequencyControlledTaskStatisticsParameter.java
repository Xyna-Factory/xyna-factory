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

package com.gip.xyna.xprc.xfqctrl;

import java.io.Serializable;


public class FrequencyControlledTaskStatisticsParameter implements Serializable{


  private static final long serialVersionUID = -1309727928574822816L;

  public static final int MAXIMUM_DATAPOINTS = 1000000;
  public static final int MINIMUM_DATAPOINTS = 2;

  private int maximumDatapoints;
  private Long minimumDatapointDistance;


  public FrequencyControlledTaskStatisticsParameter(int maximumDatapoints, Long minimumDatapointDistance) {
    if (maximumDatapoints > MAXIMUM_DATAPOINTS) {
      throw new RuntimeException("Too many maximum datapoints, maximum is <" + MAXIMUM_DATAPOINTS + ">");
    }
    if (maximumDatapoints < MINIMUM_DATAPOINTS) {
      throw new RuntimeException("Too few maximum datapoints, minimum is <" + MINIMUM_DATAPOINTS + ">");
    }
    if (minimumDatapointDistance < 1) {
      throw new RuntimeException("Minimum datapoint distance must be positive!");
    }
    this.maximumDatapoints = maximumDatapoints;
    this.minimumDatapointDistance = minimumDatapointDistance;
  }


  public int getMaximumDatapoints() {
    return maximumDatapoints;
  }


  public Long getInitialDatapointDistance() {
    return minimumDatapointDistance;
  }

}
