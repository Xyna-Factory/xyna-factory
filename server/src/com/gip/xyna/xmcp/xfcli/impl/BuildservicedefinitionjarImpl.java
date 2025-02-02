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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ServiceImplementationTemplate;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildservicedefinitionjar;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class BuildservicedefinitionjarImpl extends XynaCommandImplementation<Buildservicedefinitionjar> {

  public void execute(OutputStream statusOutputStream, Buildservicedefinitionjar payload) throws XynaException {
    String datatypeName = payload.getFqDatatypeName();
    String targetDirectory;
    if (payload.getTargetDirectory() != null && payload.getTargetDirectory().length() > 0) {
      targetDirectory = payload.getTargetDirectory();
    } else {
      targetDirectory = GenerationBase.getSimpleNameFromFQName(datatypeName) + "_" + System.currentTimeMillis();
      writeLineToCommandLine(statusOutputStream, "Target directory unspecified, generating to: " + targetDirectory);
    }
    File targetDir = new File(targetDirectory);
    if (!targetDir.exists()) {
      targetDir.mkdir();
    }

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());
    
    File generation;
    CommandControl.tryLock(CommandControl.Operation.BUILD_SERVICEDEFINITION_JAR, revision);
    try {
      generation = new ServiceImplementationTemplate(datatypeName, revision).buildServiceDefinitionJarFile(targetDir);
    } finally {
      CommandControl.unlock(CommandControl.Operation.BUILD_SERVICEDEFINITION_JAR, revision);
    }
    writeLineToCommandLine(statusOutputStream, "Service definition library generated as " + generation.getPath());
  }

}
