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
import com.gip.xyna.xmcp.xfcli.generated.Changecapacitystate;
import com.gip.xyna.xprc.xsched.CapacityManagement;



public class ChangecapacitystateImpl extends XynaCommandImplementation<Changecapacitystate> {

  public void execute(OutputStream statusOutputStream, Changecapacitystate payload) throws XynaException {
    String name = payload.getName();
    String newState = payload.getState();

    CapacityManagement.State newStateEnum = null;
    if (newState.equals(CapacityManagement.State.ACTIVE.toString())) {
      newStateEnum = CapacityManagement.State.ACTIVE;
    } else if (newState.equals(CapacityManagement.State.DISABLED.toString())) {
      newStateEnum = CapacityManagement.State.DISABLED;
    } else {
      writeLineToCommandLine(statusOutputStream, "Unknown state '" + newState + "'. Valid values are '"
          + CapacityManagement.State.ACTIVE.toString() + "' and '" + CapacityManagement.State.DISABLED + "'.");
      return;
    }

    boolean result = factory.getProcessingPortal().changeCapacityState(name, newStateEnum);

    if (result) {
      writeLineToCommandLine(statusOutputStream, "Successfully changed state for capacity '" + name + "' to '"
          + newState + "'.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Could not change state for capacity '" + name + "' to '" + newState
          + "'.");
    }
  }

}
