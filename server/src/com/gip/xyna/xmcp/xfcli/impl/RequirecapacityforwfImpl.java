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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Requirecapacityforwf;


public class RequirecapacityforwfImpl extends XynaCommandImplementation<Requirecapacityforwf> {

  public void execute(OutputStream statusOutputStream, Requirecapacityforwf payload) throws XynaException {

    String workflowName = payload.getWorkflowName();
    String capName = payload.getCapacityName();

    Integer cardinality = null;
    try {
      cardinality = Integer.valueOf(payload.getCardinality());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream,
                             "Could not parse parameter 'cardinality' ('" + payload.getCardinality() + "')");
      return;
    }

    CommandControl.tryLock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    try {
      factory.getXynaMultiChannelPortalPortal().requireCapacityForWorkflow(workflowName, capName, cardinality);
    } finally {
      CommandControl.unlock(CommandControl.Operation.ORDERTYPE_CAPACITY_MAPPING_CREATE);
    }

    writeLineToCommandLine(statusOutputStream,
                           "Deprecated: Capacity requirements should be assigned to ordertypes instead of workflows. "
                               + "This command will require the requested capacity for all ordertypes pointing at the "
                               + "specified workflow.");
    writeLineToCommandLine(statusOutputStream, "Successfully changed capacity '" + capName
        + "' requirement for workflow '" + workflowName + "' to '" + cardinality + "'");
  }

}
