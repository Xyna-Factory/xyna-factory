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
package com.gip.xyna.xprc.xfqctrl.ordercreation;

import java.util.List;

import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfqctrl.RateControlledCreationParameter;



public class RateControlledOrderCreationTaskCreationParameter
                extends
                  FrequencyControlledOrderCreationTaskCreationParameter implements RateControlledCreationParameter {

  private static final long serialVersionUID = -9118195145516562730L;

  private double rate;


  public RateControlledOrderCreationTaskCreationParameter(String label, long eventsToLaunch, double rate,
                                                          List<XynaOrderCreationParameter> orderCreationParameter) {
    super(label, eventsToLaunch, orderCreationParameter);
    setAlgorithmParameters(this);
    this.rate = rate;
  }


  public double getRate() {
    return rate;
  }

}
