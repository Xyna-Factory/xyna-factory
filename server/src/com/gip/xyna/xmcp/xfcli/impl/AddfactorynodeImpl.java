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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryChannelIdentifier;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Addfactorynode;



public class AddfactorynodeImpl extends XynaCommandImplementation<Addfactorynode> {

  public void execute(OutputStream statusOutputStream, Addfactorynode payload) throws XynaException {
    
    Integer instanceId = null;
    try {
      instanceId = Integer.valueOf(payload.getInstanceId());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream,
                             "Could not parse parameter 'instanceId' ('" + payload.getInstanceId() + "')");
      return;
    }
    
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    InterFactoryChannelIdentifier channelIdentifier = InterFactoryChannelIdentifier.valueOf(payload.getLinkchanneltype());
    Map<String, String> params = new HashMap<String, String>();
    for (String string : payload.getLinkchannelparameter()) {
      String[] split = string.split("=");
      params.put(split[0], split[1]);
    }
    Set<InterFactoryLinkProfileIdentifier> profiles = new HashSet<InterFactoryLinkProfileIdentifier>();
    for (String profile : payload.getLinkprofiles()) {
      profiles.add(InterFactoryLinkProfileIdentifier.valueOf(profile));
    }
    nodeMgmt.addNode(payload.getName(), payload.getComment(), instanceId, channelIdentifier, params, profiles);
  }

}
