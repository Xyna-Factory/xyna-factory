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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Killprocess;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;



public class KillprocessImpl extends XynaCommandImplementation<Killprocess> {

  public void execute(OutputStream statusOutputStream, Killprocess payload) throws XynaException {
    long orderId = 0;
    try {
      orderId = Long.valueOf(payload.getOrderId());
    } catch (NumberFormatException e) {
      writeToCommandLine(statusOutputStream, "Error: Could not parse order id '" + payload.getOrderId() + "'");
      return;
    }


    KillStuckProcessBean input =
        new KillStuckProcessBean(orderId, payload.getForce(), AbortionCause.MANUALLY_ISSUED,
                                 payload.getIgnoreResourcesWhenResuming());
    XynaException ex = null;
    try {
      factory.getProcessingPortal().killStuckProcess(input);
    } catch (XynaException e) {
      ex = e;
    }
    writeLineToCommandLine(statusOutputStream, "Result:");
    writeToCommandLine(statusOutputStream, input.getResultMessage());
    if (ex != null) {
      throw ex;
    }
  }

}
