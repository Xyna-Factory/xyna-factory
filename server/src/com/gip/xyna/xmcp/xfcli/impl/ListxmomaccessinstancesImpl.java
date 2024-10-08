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
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listxmomaccessinstances;



public class ListxmomaccessinstancesImpl extends XynaCommandImplementation<Listxmomaccessinstances> {

  public void execute(OutputStream statusOutputStream, Listxmomaccessinstances payload) throws XynaException {
    Map<Long, XMOMAccess> xmomAccessInstances =
                    XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getXMOMAccessManagement()
                        .listXMOMAccessInstances();
    writeLineToCommandLine(statusOutputStream, xmomAccessInstances.size() + " xmomAccessInstances defined"
                    + (xmomAccessInstances.size() > 0 ? ":" : "."));
    
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

    for (Entry<Long, XMOMAccess> xmomAccessInstance : xmomAccessInstances.entrySet()) {
      Long revision = xmomAccessInstance.getKey();
      RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
      
      writeLineToCommandLine(statusOutputStream, " - " + xmomAccessInstance.getValue().getName() + " (" + runtimeContext + ", RepositoryAccess '" + xmomAccessInstance.getValue().getRepositoryAccess().getName() + "')");
    }
  }
}
