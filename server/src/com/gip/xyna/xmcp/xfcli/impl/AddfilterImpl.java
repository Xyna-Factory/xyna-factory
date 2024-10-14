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

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection;
import com.gip.xyna.xmcp.xfcli.generated.Addfilter;



public class AddfilterImpl extends XynaCommandImplementation<Addfilter> {

  private static final Logger logger = CentralFactoryLogging.getLogger(AddfilterImpl.class);


  public void execute(OutputStream statusOutputStream, Addfilter payload) throws XynaException {

    // TODO take into account a description

    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    if (payload.getWorkspaceName() != null) {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(new Workspace(payload.getWorkspaceName()));
    }
    
    CommandControl.tryLock(CommandControl.Operation.FILTER_ADD, revision);
    try {

      List<File> jarFiles = getFileObjectsFromJarFilesForTriggerOrFilter(payload.getJarFiles());
      jarFiles = copyToSavedIfNecessary(jarFiles, payload.getFqClassName(), revision);

      String[] sharedLibs = XynaFactoryCLIConnection.parseSharedLibs(payload.getSharedLibs());
      XynaFactory
          .getInstance()
          .getActivation()
          .getActivationTrigger()
          .addFilter(payload.getFilterName(), jarFiles.toArray(new File[jarFiles.size()]), payload.getFqClassName(),
                     payload.getTriggerName(), sharedLibs, "", revision, new SingleRepositoryEvent(revision));
      
      StringBuilder sb = new StringBuilder();
      XynaFactory.getInstance().getActivation().getActivationTrigger()
          .appendTriggerState(sb, payload.getTriggerName(), revision, payload.getVerbose());
      writeLineToCommandLine(statusOutputStream, sb);
    } finally {
      CommandControl.unlock(CommandControl.Operation.FILTER_ADD, revision);
    }

  }


  private List<File> copyToSavedIfNecessary(List<File> jarFiles, String fqClassName, Long revision) throws Ex_FileAccessException {
    String targetDirPath = XynaActivationTrigger.getFilterSavedFolderByFilterFqClassName(fqClassName, revision);
    return XynaActivationTrigger.copyFilesToTargetFolder(targetDirPath, jarFiles.toArray(new File[jarFiles.size()]));
  }


  static List<File> getFileObjectsFromJarFilesForTriggerOrFilter(String[] jarFileStrings) {
    List<File> result = new ArrayList<File>();
    if (jarFileStrings != null && jarFileStrings.length > 0) {
      for (int i = 0; i < jarFileStrings.length; i++) {
        logger.debug(i + ". file " + jarFileStrings[i]);
        if (jarFileStrings[i].contains(":")) {
          logger.warn("The option -jarFiles for addfilter/addtrigger contained the character ':', this is deprecated."
              + " jarFiles should be seperated by spaces instead of ':'");
          for (String splittedJarFile : jarFileStrings[i].split(":")) {
            result.add(new File(splittedJarFile));
          }
        } else {
          result.add(new File(jarFileStrings[i]));
        }
      }
    }
    for (File f : result) {
      if (!f.exists() || f.isDirectory()) {
        throw new RuntimeException(f.getAbsolutePath() + " is not a jar file.");
      }
    }
    return result;
  }
  
}
