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
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listsharedresourcesynchronizertypes;



public class ListsharedresourcesynchronizertypesImpl extends XynaCommandImplementation<Listsharedresourcesynchronizertypes> {

  public void execute(OutputStream statusOutputStream, Listsharedresourcesynchronizertypes payload) throws XynaException {
    List<PluginDescription> list =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement().listSharedResourceSynchronizerDescriptions();


    if (list.size() == 0) {
      writeLineToCommandLine(statusOutputStream, "No shared resource synchronizer type is registered.");
      return;
    } else if (list.size() == 1) {
      writeLineToCommandLine(statusOutputStream, "Registered resource synchronizer type:");
    } else {
      writeLineToCommandLine(statusOutputStream, list.size() + " registered resource synchronizer types:");
    }

    StringBuilder output = new StringBuilder();
    for (PluginDescription pd : list) {
      output.append(pd.getName()).append("\n  ");
      PluginDescriptionUtils.append(output, pd, DocumentationLanguage.EN, ParameterUsage.Create, "new shared resource synchronizer");
      output.append("\n");
    }

    writeToCommandLine(statusOutputStream, output.toString());
  }

}
