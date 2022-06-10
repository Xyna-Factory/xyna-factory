/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listexceptions;



public class ListexceptionsImpl extends XynaCommandImplementation<Listexceptions> {

  public void execute(OutputStream statusOutputStream, Listexceptions payload) throws XynaException {
    StringBuilder output = new StringBuilder();
    
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    Set<DependencyNode> doms =
        factory.getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
            .getDependencyNodesByType(DependencySourceType.XYNAEXCEPTION, revision);

    if (doms == null || doms.size() == 0) {
      writeToCommandLine(statusOutputStream, "No exceptions registered for server\n");
      return;
    }

    writeToCommandLine(statusOutputStream, "Listing information for " + doms.size() + " deployed exceptions...\n");
    for (DependencyNode depNode : doms) {
      output.append("Name: " + depNode.getUniqueName() + "\n");
    }
    writeToCommandLine(statusOutputStream, output.toString());
  }

}
