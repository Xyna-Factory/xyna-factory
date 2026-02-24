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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listsharedresourcetypes;
import com.gip.xyna.xnwh.sharedresources.SharedResourceTypeStorable;



public class ListsharedresourcetypesImpl extends XynaCommandImplementation<Listsharedresourcetypes> {

  public void execute(OutputStream statusOutputStream, Listsharedresourcetypes payload) throws XynaException {
    List<SharedResourceTypeStorable> list =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement().listSharedResourceTypes();

    if (list.isEmpty()) {
      writeLineToCommandLine(statusOutputStream, "No shared resource type is registered.");
      return;
    } else if (list.size() == 1) {
      writeLineToCommandLine(statusOutputStream, "Registered shared resource type:");
    } else {
      writeLineToCommandLine(statusOutputStream, list.size() + " registered shared resource types:");
    }

    StringBuilder output = new StringBuilder();
    for (SharedResourceTypeStorable storable : list) {
      output.append("  ").append(storable.getSharedResourceTypeIdentifier()).append(" - ");
      if (storable.getSynchronizerInstanceIdentifier() != null) {
        output.append("synchronizer: ").append(storable.getSynchronizerInstanceIdentifier()).append("\n");
      } else {
        output.append("no synchronizer configured\n  ");
      }
    }

    writeToCommandLine(statusOutputStream, output.toString());
  }

}
