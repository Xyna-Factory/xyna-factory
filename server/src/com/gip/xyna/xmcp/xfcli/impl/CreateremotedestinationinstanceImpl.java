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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Createremotedestinationinstance;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class CreateremotedestinationinstanceImpl extends XynaCommandImplementation<Createremotedestinationinstance> {

  public void execute(OutputStream statusOutputStream, Createremotedestinationinstance payload) throws XynaException {
    RemoteDestinationManagement rdm =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();

    List<String> parameters = toList(payload.getParameters());
    if (parameters.contains("help")) {
      String help = PluginDescriptionUtils.help(rdm.getRemoteDestinationTypeDescription(payload.getRemotedestinationtype()), parameters,
                                                ParameterUsage.Create, "create");
      writeToCommandLine(statusOutputStream, help);
    } else {
      createRemoteDestinationType(rdm, statusOutputStream, parameters, payload);
    }
  }
  
  
  private void createRemoteDestinationType(RemoteDestinationManagement rdm, OutputStream statusOutputStream,
                                           List<String> parameters, Createremotedestinationinstance payload) throws PersistenceLayerException {
    Map<String, String> params = new HashMap<String, String>();
    for (String parameter : parameters) {
      int splitIdx = parameter.indexOf('=');
      params.put(parameter.substring(0, splitIdx), parameter.substring(splitIdx + 1));
    }
    Duration executionTimeout = null;
    if (payload.getExecutiontimeout() != null &&
        !payload.getExecutiontimeout().isEmpty()) {
      executionTimeout = Duration.valueOf(payload.getExecutiontimeout());
    }
    rdm.createRemoteDestinationInstance(payload.getRemotedestinationtype(), payload.getInstancedescription(), payload.getInstancename(), executionTimeout, params, false);
  }


  private List<String> toList(String[] strings) {
    if (strings == null || strings.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(strings);
  }

}
