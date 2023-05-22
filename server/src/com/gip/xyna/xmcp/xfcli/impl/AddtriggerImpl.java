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
import java.io.OutputStream;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection;
import com.gip.xyna.xmcp.xfcli.generated.Addtrigger;



public class AddtriggerImpl extends XynaCommandImplementation<Addtrigger> {

  private static final Logger logger = CentralFactoryLogging.getLogger(AddtriggerImpl.class);


  public void execute(OutputStream statusOutputStream, Addtrigger payload) throws XynaException {

    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    if (payload.getWorkspaceName() != null) {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(new Workspace(payload.getWorkspaceName()));
    }

    CommandControl.tryLock(CommandControl.Operation.TRIGGER_ADD, revision);
    try {

      List<File> jarFiles = AddfilterImpl.getFileObjectsFromJarFilesForTriggerOrFilter(payload.getJarFiles());
      jarFiles = copyToSavedIfNecessary(jarFiles, payload.getFqClassName(), revision);

      String[] sharedLibs = XynaFactoryCLIConnection.parseSharedLibs(payload.getSharedLibs());
      XynaFactory.getInstance().getActivation()
          .addTrigger(payload.getTriggerName(), jarFiles.toArray(new File[jarFiles.size()]), payload.getFqClassName(), sharedLibs, revision);
      
      StringBuilder sb = new StringBuilder();
      XynaFactory.getInstance().getActivation().getActivationTrigger()
          .appendTriggerState(sb, payload.getTriggerName(), revision, payload.getVerbose());
      writeLineToCommandLine(statusOutputStream, sb);
    } finally {
      CommandControl.unlock(CommandControl.Operation.TRIGGER_ADD, revision);
    }
  }


  private List<File> copyToSavedIfNecessary(List<File> jarFiles, String fqClassName, Long revision) throws Ex_FileAccessException {
    String targetDirPath = XynaActivationTrigger.getTriggerSavedFolderByTriggerFqClassName(fqClassName, revision);
    return XynaActivationTrigger.copyFilesToTargetFolder(targetDirPath, jarFiles.toArray(new File[jarFiles.size()]));
  }

}
