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
import java.util.ArrayList;
import java.util.List;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Instantiatesharedresourcesynchronizer;
import com.gip.xyna.xnwh.sharedresources.SharedResourceSynchronizerInstance;



public class InstantiatesharedresourcesynchronizerImpl extends XynaCommandImplementation<Instantiatesharedresourcesynchronizer> {

  public void execute(OutputStream statusOutputStream, Instantiatesharedresourcesynchronizer payload) throws XynaException {
    String typeName = payload.getSynchronizerType();
    String instanceName = payload.getInstanceName();
    List<String> configuration = payload.getConfiguration() == null ? new ArrayList<>() : List.of(payload.getConfiguration());
    SharedResourceSynchronizerInstance.Status status = SharedResourceSynchronizerInstance.Status.Stop;

    String startOption = payload.getStartOption();
    if (startOption != null) {
      try {
        status = SharedResourceSynchronizerInstance.Status.fromValue(startOption);
      } catch (IllegalArgumentException e) {
        writeLineToCommandLine(statusOutputStream, "Invalid start option: " + startOption);
        return;
      }
    }

    try {
      XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement()
          .createSharedResourceSynchronizer(typeName, instanceName, configuration, status);
      writeLineToCommandLine(statusOutputStream, String
          .format("SharedResourceSynchronizer \"%s\" for type \"%s\" successfully instantiated.", instanceName, typeName));
    } catch (Exception e) {
      writeLineToCommandLine(statusOutputStream, String
          .format("Could not instantiate SharedResourceSynchronizer \"%s\" for type \"%s\": %s", instanceName, typeName, e.getMessage()));
      return;
    }

  }
}
