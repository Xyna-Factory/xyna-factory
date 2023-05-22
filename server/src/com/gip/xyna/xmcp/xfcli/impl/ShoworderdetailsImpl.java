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

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.generated.Showorderdetails;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformationThrowable;



public class ShoworderdetailsImpl extends XynaCommandImplementation<Showorderdetails> {

  public void execute(OutputStream statusOutputStream, Showorderdetails payload) throws XynaException {

    Long id;
    try {
      id = new Long(payload.getId());
    } catch (NumberFormatException nfe) {
      writeLineToCommandLine(statusOutputStream, "Invalid ID: " + payload.getId() + ".");
      return;
    }

    OrderInstanceDetails orderDetails = null;
    try {
      orderDetails = factory.getXynaMultiChannelPortalPortal().getRunningProcessDetails(id);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      writeLineToCommandLine(statusOutputStream, "No audit found for ID " + id);
      return;
    }

    if (orderDetails != null) {
      writeLineToCommandLine(statusOutputStream, "Found information for order ", id, ":");

      SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
      StringBuffer output = new StringBuffer();

      output.append("Order Type:\t\t").append(orderDetails.getOrderType()).append("\n");
      if(orderDetails.getApplicationName() != null) {
        output.append("Application:\t\t").append(orderDetails.getApplicationName()).append(" ").append(orderDetails.getVersionName()).append("\n");
      }
      if (orderDetails.getWorkspaceName() != null && !orderDetails.getWorkspaceName().equals(RevisionManagement.DEFAULT_WORKSPACE.getName())) {
        output.append("Workspace: ").append(orderDetails.getWorkspaceName()).append("\n");
      }

      if (orderDetails.getStartTime() > 0) {
        output.append("Started:\t\t").append(dateFormatter.format(new Date(orderDetails.getStartTime()))).append("\n");
      } else {
        output.append("Started:\t\tUnknown (monitoring level too low)\n");
      }
      output.append("Last Update:\t\t").append(dateFormatter.format(new Date(orderDetails.getLastUpdate()))).append("\n");
      output.append("Master workflow status:\t").append(orderDetails.getStatusAsString()).append("\n");
      output.append("Suspension status:\t").append(orderDetails.getSuspensionStatus()).append("\n");
      String suspensionCauseAsString = orderDetails.getSuspensionCauseAsString(true);
      if (suspensionCauseAsString != null) {
        output.append("Suspension cause: ").append(suspensionCauseAsString).append("\n");
      }

      if (orderDetails.getExceptions() != null && orderDetails.getExceptions().size() > 0) {
        if (orderDetails.getExceptions().size() > 1) {
          output.append("Several exceptions occured:\n");
        } else {
          output.append("One exception occured:\n");
        }
        for (XynaExceptionInformation ex : orderDetails.getExceptions()) {
          output.append(" (");
          output.append("Errorcode: ").append(ex.getCode()).append(", ");
          output.append("Message: ").append(ex.getMessage());
          output.append(")");

          XynaExceptionInformationThrowable tmp = new XynaExceptionInformationThrowable(ex);
          StringWriter writer = new StringWriter();
          tmp.printStackTrace(new PrintWriter(writer));
          output.append(writer.toString());

          output.append("\n");
        }
      }

      output.append("\n");

      writeToCommandLine(statusOutputStream, output.toString());
      if (payload.getVerbose()) {
        writeToCommandLine(statusOutputStream, "\n");
        writeToCommandLine(statusOutputStream, orderDetails.getAuditDataAsXML());
      }

    } else {
      writeLineToCommandLine(statusOutputStream, "No detailed information found for ID " + id + ".");
    }
  
  }

}
