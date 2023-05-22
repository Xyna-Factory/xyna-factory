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

package com.gip.xyna.xprc.xsched.scheduling;



import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;



public class OrderStartTimeJavaDestination extends JavaDestination {

  private static final long serialVersionUID = 1L;

  public static final String ORDER_START_TIME_DESTINATION = "com.gip.xyna.OrderStartTime";


  public OrderStartTimeJavaDestination() {
    super(ORDER_START_TIME_DESTINATION);
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XPRC_INVALID_INPUT_PARAMETER_TYPE {

    if (!(input instanceof OrderStartTimeBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", OrderStartTimeBean.class.getName(), input.getClass()
          .getName());
    }

    OrderStartTimeBean bean = (OrderStartTimeBean) input;

    bean.setRequestSucceeded(true);

    return bean;

  }

}
