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
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Rebuildfailedcomponent;



public class RebuildfailedcomponentImpl extends XynaCommandImplementation<Rebuildfailedcomponent> {

  public void execute(OutputStream statusOutputStream, Rebuildfailedcomponent payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());

    CodeAccess ca = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment()
      .getCodeAccessManagement().getCodeAccessInstance(revision);
    if (ca == null) {
      writeLineToCommandLine(statusOutputStream, "No CodeAccess defined.");
      return;
    }
    
    if (payload.getComponentname() == null) {
      Set<String> rebuild = ca.retryAllBuilds(payload.getDeploy());
      if (rebuild.size() > 0) {
        writeLineToCommandLine(statusOutputStream, "Rebuild successfull for:");
        for (String componentName : rebuild) {
          writeLineToCommandLine(statusOutputStream, componentName);
        }
      } else {
        writeLineToCommandLine(statusOutputStream, "No rebuild was succesfully executed");
      }
    } else {
      if (ca.retryBuild(payload.getComponentname(), payload.getDeploy())) {
        writeLineToCommandLine(statusOutputStream, "Rebuild successfull");
      } else {
        writeLineToCommandLine(statusOutputStream, "Rebuild failed");
      }
    }
  }

}
