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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listwfinstancedetails;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformationThrowable;


public class ListwfinstancedetailsImpl extends XynaCommandImplementation<Listwfinstancedetails> {

  public void execute(OutputStream statusOutputStream, Listwfinstancedetails payload) throws XynaException {

    Long id;
    try {
      id = new Long(payload.getId());
    } catch (NumberFormatException nfe) {
      writeLineToCommandLine(statusOutputStream, "Invalid ID (" + payload.getId() + ").");
      return;
    }

    OrderInstanceDetails wfDetails = null;
    try {
      wfDetails = factory.getXynaMultiChannelPortalPortal().getRunningProcessDetails(id);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      writeLineToCommandLine(statusOutputStream, "No detailed information found for ID " + id);
      return;
    }

    if (wfDetails != null) {
      writeLineToCommandLine(statusOutputStream, "Listing information on workflow instance (ID ", id, ")...");

      SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss.SSS (dd.MM.yyyy)");
      StringBuffer output = new StringBuffer();

      output.append("Name: ").append(wfDetails.getOrderType()).append("\n");
      if(wfDetails.getApplicationName() != null) {
        output.append("Application: ").append(wfDetails.getApplicationName()).append(" ").append(wfDetails.getVersionName()).append("\n");
      }
      if (wfDetails.getWorkspaceName() != null && !wfDetails.getWorkspaceName().equals(RevisionManagement.DEFAULT_WORKSPACE.getName())) {
        output.append("Workspace: ").append(wfDetails.getWorkspaceName()).append("\n");
      }

      if (wfDetails.getStartTime() > 0) {
        output.append("Started: ").append(dateFormatter.format(new Date(wfDetails.getStartTime()))).append("\n");
      } else {
        output.append("Started: Unknown (monitoring level too low)\n");
      }
      output.append("Last Update: ").append(dateFormatter.format(new Date(wfDetails.getLastUpdate()))).append("\n");
      output.append("Master workflow status: ").append(wfDetails.getStatusAsString()).append("\n");
      output.append("Suspension status: ").append(wfDetails.getSuspensionStatus()).append("\n");
      String suspensionCauseAsString = wfDetails.getSuspensionCauseAsString(true);
      if (suspensionCauseAsString != null) {
        output.append("Suspension cause: ").append(suspensionCauseAsString).append("\n");
      }

      if (wfDetails.getExceptions() != null) {
        if (wfDetails.getExceptions().size() > 1) {
          output.append("Several Exceptions occured: \n");
        }
        for (XynaExceptionInformation ex : wfDetails.getExceptions()) {
          output.append(" (");
          output.append("Errorcode: ").append(ex.getCode()).append(", ");
          output.append("Message: ").append(ex.getMessage());
          output.append(")");
          if (payload.getVerbose() || payload.getExtraverbose()) {
            XynaExceptionInformationThrowable tmp = new XynaExceptionInformationThrowable(ex);
            StringWriter writer = new StringWriter();
            tmp.printStackTrace(new PrintWriter(writer));
            output.append(writer.toString());
          }
          output.append("\n");
        }
      }

      output.append("\n");

      writeToCommandLine(statusOutputStream, output.toString());
      if (payload.getExtraverbose()) {
        writeToCommandLine(statusOutputStream, "\n");
        writeToCommandLine(statusOutputStream, wfDetails.getAuditDataAsXML());
      }

    } else {
      writeLineToCommandLine(statusOutputStream, "No detailed information found for ID " + id + ".");
    }
  
  }

}
