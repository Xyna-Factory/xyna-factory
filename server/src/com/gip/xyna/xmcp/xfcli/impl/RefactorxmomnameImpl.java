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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter.RefactoringTargetRootType;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringMoveActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.XMOMObjectRefactoringResult;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Refactorxmomname;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class RefactorxmomnameImpl extends XynaCommandImplementation<Refactorxmomname> {

  public void execute(OutputStream statusOutputStream, Refactorxmomname payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.DEFAULT_WORKSPACE;
    if (payload.getWorkspaceName() != null && payload.getWorkspaceName().length() > 0) {
      runtimeContext = new Workspace(payload.getWorkspaceName());
    }
    Long revision = revisionManagement.getRevision(runtimeContext);
    
    String objectType = payload.getTargettype();
    RefactoringTargetRootType targetType;
    if (objectType == null || objectType.length() == 0) {
      XMOMType type = XMOMType.getXMOMTypeByRootTag(GenerationBase.retrieveRootTag(payload.getSourcefqname(), revision, false, false));
      targetType = RefactoringTargetRootType.fromXMOMType(type) ;
    } else {
      if (objectType.equalsIgnoreCase(RefactoringTargetRootType.PATH.name())) {
        targetType = RefactoringTargetRootType.PATH;
      } else {
        targetType = RefactoringTargetRootType.fromXMOMType(XMOMType.getXMOMTypeByString(objectType));
      }
    }
    
    RefactoringMoveActionParameter rmap = new RefactoringMoveActionParameter();
    rmap.setFqXmlNameNew(payload.getTargetfqname())
        .setFqXmlNameOld(payload.getSourcefqname())
        .setTargetLabel(payload.getLabel())
        .setTargetRootType(targetType);
    rmap.setForceDeploy(payload.getIgnoreconflicts());
    rmap.setIgnoreIncompatibleStorables(payload.getIgnorestorables());
    rmap.setRuntimeContext(runtimeContext);

    TemporarySessionAuthentication tsa;
    if (payload.getUsername() == null || payload.getUsername().length() <= 0 ||
        payload.getPassword() == null || payload.getPassword().length() <= 0) {
      tsa =
          TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("Refactoring", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, revision,
                                                                                CommandControl.Operation.XMOM_REFACTORING);
    } else {
      tsa =
          TemporarySessionAuthentication.tempAuthWithExistingUserAndOperationLock(payload.getUsername(), payload.getPassword(), revision,
                                                                                  CommandControl.Operation.XMOM_REFACTORING);
    }
    tsa.initiate();
    try {
      rmap.setSessionId(tsa.getSessionId());
      rmap.setUsername(tsa.getUsername());
      RefactoringResult result = factory.getXynaDevelopmentPortal().refactorXMOM(rmap);
      writeRefactoringReportToCommandLine(result, statusOutputStream);
    } finally {
      tsa.destroy();
    }
  }

  
  private void writeRefactoringReportToCommandLine(RefactoringResult result, OutputStream outputstream) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    writeLineToCommandLine(outputstream, "RefactoringReport:");
    String lineFormat = "  %-8s: %-10s %s";
    for (XMOMObjectRefactoringResult object : result.getRefactoredObjects()) {
      writeLineToCommandLine(outputstream, String
          .format(lineFormat, object.getType(), object.getRefactoringTargetType(),
                  object.getFqXmlNameNew() + "@[" + revisionManagement.getRuntimeContext(object.getRevision()).toString() + "]"));
    }
  }

}
