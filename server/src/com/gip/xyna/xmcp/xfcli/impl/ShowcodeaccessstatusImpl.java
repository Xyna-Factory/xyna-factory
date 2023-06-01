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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Showcodeaccessstatus;



public class ShowcodeaccessstatusImpl extends XynaCommandImplementation<Showcodeaccessstatus> {

  public void execute(OutputStream statusOutputStream, Showcodeaccessstatus payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());

    CodeAccess ca = XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment()
                    .getCodeAccessManagement().getCodeAccessInstance(revision);
    if (ca == null) {
      writeLineToCommandLine(statusOutputStream, "No CodeAccess defined.");
      return;
    }
    
    List<String> params = new ArrayList<String>();
    if (payload.getVerbose()) {
      params.add(CodeAccess.CLI_VERBOSE_IDENTIFIER);
    }
    if (payload.getRepository()) {
      params.add(CodeAccess.CLI_REPOSITORY_IDENTIFIER);
    }
    Reader reader = ca.getExtendedInformation(params.toArray(new String[params.size()]));
    writeReaderToCommandLine(statusOutputStream, reader);
  }

}
