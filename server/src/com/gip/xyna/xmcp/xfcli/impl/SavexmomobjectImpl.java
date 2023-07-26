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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xpce.WorkflowEngine;

import com.gip.xyna.xmcp.xfcli.generated.Savexmomobject;



public class SavexmomobjectImpl extends XynaCommandImplementation<Savexmomobject> {

  private static final RevisionManagement revisionManagement =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();


  public void execute(OutputStream statusOutputStream, Savexmomobject payload) throws XynaException {
    saveXmomObject(payload.getWorkspaceName(), payload.getFqName(), payload.getDeploy());
  }


  public void saveXmomObject(String workspace, String fqn, boolean deploy) throws XynaException{
    Long revision = revisionManagement.getRevision(null, null, workspace);
    String pathXMOM = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
    String pathObject = fqn.replace('.', Constants.fileSeparator.charAt(0));
    XynaMultiChannelPortal mcp = ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal());

    TemporarySessionAuthentication tsa = TemporarySessionAuthentication
        .tempAuthWithUniqueUserAndOperationLock("savexmomobject", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, revision,
                                                CommandControl.Operation.XMOM_SAVE);
    tsa.initiate();
    try {
      StringBuilder fileName = new StringBuilder().append(pathXMOM).append(Constants.fileSeparator).append(pathObject).append(".xml");
      String fileAsString = FileUtils.readFileAsString(new File(fileName.toString()));

      mcp.saveMDM(fileAsString, true, tsa.getUsername(), tsa.getSessionId(), revision, null, true);

      if (deploy) {
        Document d = XMLUtils.parse(new File(fileName.toString()), true);
        Element rootElement = d.getDocumentElement();
        XMOMType type = XMOMType.getXMOMTypeByRootTag(rootElement.getTagName());
        WorkflowProtectionMode mode = WorkflowProtectionMode.BREAK_ON_USAGE;
        WorkflowEngine wfEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
        if (type == XMOMType.DATATYPE) {
          wfEngine.deployDatatype(fqn, mode, null, null, revision);
        } else if (type == XMOMType.EXCEPTION) {
          wfEngine.deployException(fqn, mode, revision);
        } else if (type == XMOMType.WORKFLOW) {
          wfEngine.deployWorkflow(fqn, mode, revision);
        }
      }
    } finally {
      tsa.destroy();
    }
  }

}
