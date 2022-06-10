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
package com.gip.xyna.xprc.xfqctrl.ordercreation;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_TYPE;



public abstract class FrequencyControlledOrderCreationTaskCreationParameter extends FrequencyControlledTaskCreationParameter {

  private static final long serialVersionUID = -2629943432910304138L;
  
  private List<XynaOrderCreationParameter> orderCreationParameter;
  
  public FrequencyControlledOrderCreationTaskCreationParameter(String label, long eventsToLaunch, List<XynaOrderCreationParameter> orderCreationParameter) {
    super(label, eventsToLaunch);
    this.orderCreationParameter = new ArrayList<XynaOrderCreationParameter>();
    this.orderCreationParameter.addAll(orderCreationParameter);
  }

  
  public void setOrderCreationParameter(List<XynaOrderCreationParameter> orderCreationParameter) {
    this.orderCreationParameter = new ArrayList<XynaOrderCreationParameter>();
    this.orderCreationParameter.addAll(orderCreationParameter);
  }
  
  
  public List<XynaOrderCreationParameter> getOrderCreationParameter() {
    return orderCreationParameter;
  }
  
  @Override
  public FREQUENCY_CONTROLLED_TASK_TYPE getTaskType() {
    return FREQUENCY_CONTROLLED_TASK_TYPE.ORDER_CREATION;
  }
}
