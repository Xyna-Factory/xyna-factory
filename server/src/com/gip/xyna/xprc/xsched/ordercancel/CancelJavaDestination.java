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

package com.gip.xyna.xprc.xsched.ordercancel;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean.CANCEL_RESULT;



public final class CancelJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -6419362756143480614L;

  public static final String CANCEL_DESTINATION = "com.gip.xyna.Cancel";


  public CancelJavaDestination() {
    super(CANCEL_DESTINATION);
  }


  @Override
  public CancelBean exec(XynaOrderServerExtension xose, final GeneralXynaObject input)
      throws XPRC_INVALID_INPUT_PARAMETER_TYPE, XPRC_UNEXPECTED_ERROR_PROCESS {

    if (!(input instanceof CancelBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", CancelBean.class.getName(), input == null ? "null" : input
          .getClass().getName());
    }

    // clone the data to make sure no one can change the input after execution to avoid inconsistencies
    final CancelBean bean = ((CancelBean) input).clone();
    CANCEL_RESULT result = CANCEL_RESULT.WORK_IN_PROGRESS;
    bean.setResult(result);

    if (bean.isWaitForTimeout()) {
      result =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderCancellationManagement()
              .processCancellationAndWait(bean);
    } else {
      result =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderCancellationManagement()
              .processCancellation(bean);
    }

    bean.setResult(result);

    return bean;
  }

}
