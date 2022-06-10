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

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildmdmjar;


public class BuildmdmjarImpl extends XynaCommandImplementation<Buildmdmjar> {

  private static final Logger logger = CentralFactoryLogging.getLogger(BuildmdmjarImpl.class);

  public void execute(OutputStream statusOutputStream, Buildmdmjar payload) throws XynaException {
    File defaultDir = new File(".");
    File dir = defaultDir;
    if (payload.getTargetDirectory() == null) {
      writeLineToCommandLine(statusOutputStream,
                             "No folder given to store MDM jar to. Using default (" + defaultDir.getPath() + ")");
    } else {
      dir = new File(payload.getTargetDirectory());
    }

    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = rm
                    .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    List<Throwable> exceptions = Support4Eclipse.buildMDMJarFileRecursively(dir, revision, payload.getRecursive(), statusOutputStream);
    writeLineToCommandLine(statusOutputStream, "Done.");

    logger.info("MDM jarfile created at " + dir.getPath());

    if (exceptions.size() > 0) {
      writeLineToCommandLine(statusOutputStream, "Exceptions occured while building mdm.jar:");

      for (Throwable xynaException : exceptions) {
        writeLineToCommandLine(statusOutputStream, xynaException.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug(null, xynaException);
        }
      }
    }
  }

}
