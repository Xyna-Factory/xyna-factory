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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccess;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildfilterproject;



public class BuildfilterprojectImpl extends XynaCommandImplementation<Buildfilterproject> {

  public void execute(OutputStream statusOutputStream, Buildfilterproject payload) throws XynaException {
    String targetString = payload.getTargetDirectory();
    String filterName = payload.getFilterName();
    String triggerName = payload.getTriggerName();

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());
    
    if (targetString == null) {
      CodeAccess ca =
          XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getCodeAccessManagement()
              .getCodeAccessInstance(revision);
      if (ca == null) {
        writeLineToCommandLine(statusOutputStream, "No CodeAccess defined. Continuing building in local directory ...");
        targetString = ".";
      } else {
        ProjectCreationOrChangeProvider.getInstance().notify(new ProjectCreationOrChangeProvider.FilterCreationEvent(filterName, triggerName), revision, true);
        return;
      }
    }

    writeLineToCommandLine(statusOutputStream, "Using target location " + payload.getTargetDirectory()
        + ", filterName " + payload.getFilterName() + " and triggerName " + payload.getTriggerName() + ".");
    writeLineToCommandLine(statusOutputStream, "Building project...");

    CommandControl.tryLock(CommandControl.Operation.BUILD_FILTERTEMPLATE, revision);
    File f;
    try {
      InputStream is = factory.getXynaMultiChannelPortalPortal().getFilterImplTemplate(FileUtils.generateRandomFilename(targetString,"t",""), filterName, triggerName, revision, true, true);
      f = FileUtils.generateUniqueFileIncrementally(targetString, payload.getFilterName(), ".zip");
      FileUtils.saveToFile(is, f);
      is.close();
    } catch (IOException e) {
      throw new Ex_FileAccessException(targetString, e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.BUILD_FILTERTEMPLATE, revision);
    }

    writeLineToCommandLine(statusOutputStream, "zipfile stored to " + f.getAbsolutePath());
    writeLineToCommandLine(statusOutputStream, "Done.");
  }

}
