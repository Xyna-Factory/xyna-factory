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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Configuresharedresourcetype;



public class ConfiguresharedresourcetypeImpl extends XynaCommandImplementation<Configuresharedresourcetype> {

  public void execute(OutputStream statusOutputStream, Configuresharedresourcetype payload) throws XynaException {
    String resourceType = payload.getResourceType();
    String synchronizerInstanceName = payload.getDelete() ? null : payload.getSynchronizerInstanceName();

    if (!payload.getDelete() && synchronizerInstanceName == null) {
      writeLineToCommandLine(statusOutputStream, "Error: Synchronizer instance name must be provided.");
      writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
      return;
    }

    try {
      XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement()
          .configureSharedResourceType(resourceType, synchronizerInstanceName);
      if (payload.getDelete()) {
        writeLineToCommandLine(statusOutputStream,
                               String.format("Successfully deleted configuration of shared resource type %s", resourceType));
      } else {
        writeLineToCommandLine(statusOutputStream,
                               String.format("Successfully configured shared resource type %s with synchronizer instance %s", resourceType,
                                             synchronizerInstanceName));
      }
    } catch (Exception e) {
      writeLineToCommandLine(statusOutputStream, "Error configuring shared resource type: " + e.getMessage());
      writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
    }
  }

}
