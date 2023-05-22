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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Cancelbatchprocess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;



public class CancelbatchprocessImpl extends XynaCommandImplementation<Cancelbatchprocess> {

  public void execute(OutputStream statusOutputStream, Cancelbatchprocess payload) throws XynaException {
    Long batchProcessId = null;
    try {
      batchProcessId = Long.valueOf(payload.getOrderId());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, "Could not parse ID.");
      return;
    }
    
    CancelMode mode = CancelMode.DEFAULT;
    if (payload.getCancelMode() != null && payload.getCancelMode().length() > 0) {
      try {
        mode = CancelMode.valueOf(payload.getCancelMode());
      } catch (IllegalArgumentException e) {
        writeLineToCommandLine(statusOutputStream, "Unknown cancel mode: '" + payload.getCancelMode()
            + "'");
        return;
      }
    }
    
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
    boolean success = bpm.cancelBatchProcess(batchProcessId, mode, -1L);

    if (!success) {
      writeLineToCommandLine(statusOutputStream, "Could not cancel batch process " + batchProcessId +".");
    }
  }

}
