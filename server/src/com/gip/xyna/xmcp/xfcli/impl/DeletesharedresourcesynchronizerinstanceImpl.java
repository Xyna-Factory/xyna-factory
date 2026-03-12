/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Deletesharedresourcesynchronizerinstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;



public class DeletesharedresourcesynchronizerinstanceImpl extends XynaCommandImplementation<Deletesharedresourcesynchronizerinstance> {

  public void execute(OutputStream statusOutputStream, Deletesharedresourcesynchronizerinstance payload) throws XynaException {
    String instanceName = payload.getInstanceName();
    SharedResourceManagement srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
    List<String> sharedResourceTypes = srm.deleteSharedResourceSynchronizer(instanceName);
    writeToCommandLine(statusOutputStream, "Successfully deleted shared resource synchronizer instance " + instanceName);
    if (sharedResourceTypes.isEmpty()) {
      writeToCommandLine(statusOutputStream, "It was not configured on any shared resource types");
      return;
    }
    String types = String.join(", ", sharedResourceTypes);
    writeToCommandLine(statusOutputStream, "It was configured to these shared resource types: " + types);
  }

}
