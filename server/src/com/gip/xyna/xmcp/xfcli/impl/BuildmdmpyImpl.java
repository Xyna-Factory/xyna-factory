/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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



import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildmdmpy;



public class BuildmdmpyImpl extends XynaCommandImplementation<Buildmdmpy> {

  public void execute(OutputStream statusOutputStream, Buildmdmpy payload) throws XynaException {
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revMgmt.getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    File defaultDir = new File(".");
    File dir = defaultDir;
    if (payload.getTargetDirectory() == null) {
      writeLineToCommandLine(statusOutputStream,
                             "No folder given to store MDM python file to. Using default (" + defaultDir.getPath() + ")");
    } else {
      dir = new File(payload.getTargetDirectory());
    }
    
    
    try {
      XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().exportPythonMdm(revision, dir.getPath());
    } catch (Exception e) {
      writeToCommandLine(statusOutputStream, "Error during generation: ", e);
      try(StringWriter sw = new StringWriter()) {
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        writeToCommandLine(statusOutputStream, sw.toString());
      } catch (IOException e1) {
        
      }
      writeEndToCommandLine(statusOutputStream, ReturnCode.GENERAL_ERROR);
    }
  }

}
