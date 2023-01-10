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
package xmcp.gitintegration.cli.impl;



import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.cli.generated.Removexmomobject;



public class RemovexmomobjectImpl extends XynaCommandImplementation<Removexmomobject> {

  private static final RevisionManagement revisionManagement =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();


  public void execute(OutputStream statusOutputStream, Removexmomobject payload) throws XynaException {
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());

    TemporarySessionAuthentication tsa = TemporarySessionAuthentication
        .tempAuthWithUniqueUserAndOperationLock("addxmomobject", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, revision,
                                                CommandControl.Operation.XMOM_SAVE);
    tsa.initiate();
    try {
      DeploymentItemState deploymentItemState = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
          .getDeploymentItemStateManagement().get(payload.getFqName(), revision);
      if (deploymentItemState == null) {
        throw new IllegalArgumentException("Object unknown.");
      }
      XMOMType type = deploymentItemState.getType();

      ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal())
          .deleteXMOMObject(type, payload.getFqName(), true, true, tsa.getUsername(), tsa.getSessionId(), revision);

    } finally {
      tsa.destroy();
    }
  }

}
