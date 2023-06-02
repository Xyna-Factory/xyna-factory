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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Processmanualinteraction;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction.ProcessManualInteractionResult;



public class ProcessmanualinteractionImpl extends XynaCommandImplementation<Processmanualinteraction> {

  public void execute(OutputStream statusOutputStream, Processmanualinteraction payload) throws XynaException {

    Long id;
    try {
      id = new Long(payload.getId());
    } catch (NumberFormatException nfe) {
      writeLineToCommandLine(statusOutputStream, "Invalid ID (", payload.getId(), ").");
      return;
    }

    String command = payload.getResponse();

    if (command.equalsIgnoreCase("Abort") || command.equalsIgnoreCase("Continue") || command.equalsIgnoreCase("Retry")) {
      StringBuilder xmlStringBuffer = new StringBuilder("<payload><");
      xmlStringBuffer.append(GenerationBase.EL.DATA).append(" ");
      xmlStringBuffer.append(GenerationBase.ATT.REFERENCENAME).append("=\"").append(command).append("\" ");
      xmlStringBuffer.append(GenerationBase.ATT.REFERENCEPATH).append("=\"xmcp.manualinteraction\" ");
      xmlStringBuffer.append(GenerationBase.ATT.VARIABLENAME).append("=\"v\">");
      xmlStringBuffer.append("</").append(GenerationBase.EL.DATA).append("></payload>");
      GeneralXynaObject response = XynaObject.generalFromXml(xmlStringBuffer.toString(), RevisionManagement.REVISION_DEFAULT_WORKSPACE); //TODO andere revisions

      ProcessManualInteractionResult result =
          factory.getXynaMultiChannelPortalPortal().processManualInteraction(id, response);
      if (result != ProcessManualInteractionResult.SUCCESS) {
        if (result == ProcessManualInteractionResult.NOT_FOUND) {
          writeLineToCommandLine(statusOutputStream, "No entry found for ID '", id, "'.");
        } else if (result == ProcessManualInteractionResult.FOREIGN_BINDING) {
          writeLineToCommandLine(statusOutputStream, "No node is responsible for MI entry <" + id + ">");
        } else {
          writeLineToCommandLine(statusOutputStream, "Unexpected result: " + result);
        }
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "Unknown command '" + command + "'.");
    }
  }

}
