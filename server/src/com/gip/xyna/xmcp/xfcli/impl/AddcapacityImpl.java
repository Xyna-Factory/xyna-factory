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
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Addcapacity;
import com.gip.xyna.xprc.xsched.CapacityManagement;



public class AddcapacityImpl extends XynaCommandImplementation<Addcapacity> {

  public void execute(OutputStream statusOutputStream, Addcapacity payload) throws XynaException {

    Integer cardinality = null;
    try {
      cardinality = Integer.valueOf(payload.getCardinality());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream,
                             "Could not parse parameter 'cardinality' ('" + payload.getCardinality() + "')");
      return;
    }

    String state = payload.getState();
    CapacityManagement.State enumState = null;
    if (state != null) {
      enumState = CapacityManagement.State.valueOf(state);
    } else {
      enumState = CapacityManagement.State.ACTIVE;
    }

    if (factory.getXynaMultiChannelPortalPortal().addCapacity(payload.getName(), cardinality, enumState)) {
      writeLineToCommandLine(statusOutputStream, "Successfully added capacity");
    } else {
      writeLineToCommandLine(statusOutputStream, "Could not add capacity");
    }

  }

}
