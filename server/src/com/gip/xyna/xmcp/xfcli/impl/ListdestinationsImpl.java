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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listdestinations;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;



public class ListdestinationsImpl extends XynaCommandImplementation<Listdestinations> {

  public void execute(OutputStream statusOutputStream, Listdestinations payload) throws XynaException {
    DispatcherIdentification di = null;
    try {
      di = DispatcherIdentification.valueOfIgnoreCase(payload.getDispatcherName());
    } catch (IllegalArgumentException e) {
      writeToCommandLine(statusOutputStream, "Invalid dispatcher '" + payload.getDispatcherName()
          + "'; Planning, Execution and Cleanup are valid dispatchers.\n");
      return;
    }

    List<DispatcherEntry> destinations = factory.getXynaMultiChannelPortalPortal().listDestinations(di);
    Collections.sort(destinations, new Comparator<DispatcherEntry>() {

      public int compare(DispatcherEntry o1, DispatcherEntry o2) {
        if (o1 == null || o2 == null) {
          return 1;
        }
        // ignore case when sorting ordertypes
        return o1.getKey().getOrderType().toLowerCase().compareTo(o2.getKey().getOrderType().toLowerCase());
      }
    });
    
    StringBuilder bld = new StringBuilder();
    if(payload.getApplicationName() != null) {
      if (payload.getWorkspaceName() != null) {
        writeLineToCommandLine(statusOutputStream, "'applicationName' and 'workspaceName' may not both be set");
        return;
      }
      if(payload.getVersionName() == null) {
        writeLineToCommandLine(statusOutputStream, "Need version name for application!");
        return;        
      }
      bld.append("Application ").append(payload.getApplicationName());
      bld.append(" ").append(payload.getVersionName()).append(": ");
    }
    
    if(payload.getWorkspaceName() != null) {
      bld.append("Workspace ").append(payload.getWorkspaceName()).append(": ");
    }
    
    List<DispatcherEntry> destinationsVersonized = new ArrayList<DispatcherEntry>();
    for(DispatcherEntry destination : destinations) {
      if(payload.getApplicationName() != null) {
        Application app = new Application(payload.getApplicationName(), payload.getVersionName());
        if (app.equals(destination.getKey().getRuntimeContext())) {
          destinationsVersonized.add(destination);
        }
      } else if (payload.getWorkspaceName() != null) {
        Workspace workspace = new Workspace(payload.getWorkspaceName());
        if (workspace.equals(destination.getKey().getRuntimeContext())) {
          destinationsVersonized.add(destination);
        }
      } else if (destination.getKey().getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        destinationsVersonized.add(destination);
      }
    }
    
    bld.append("Listing ").append(destinationsVersonized.size()).append(" destinations...");
    
    writeLineToCommandLine(statusOutputStream, bld.toString());

    int maxLengthKey = 30;
    int maxLengthValue = 30;
    for (DispatcherEntry de: destinationsVersonized) {
      if (de.getKey().getOrderType().length() > maxLengthKey) {
        maxLengthKey = de.getKey().getOrderType().length();
      }
      if (de.getValue().getFqName().length() > maxLengthValue) {
        maxLengthValue = de.getValue().getFqName().length();
      }
    }

    if (maxLengthKey > 90) {
      maxLengthKey = 90;
    }
    if (maxLengthValue > 90) {
      maxLengthValue = 90;
    }

    String format = " %-" + maxLengthKey + "s  %-" + maxLengthValue + "s ";

    writeLineToCommandLine(statusOutputStream, String.format(format, "Ordertype", "Destination"));
    for (DispatcherEntry de : destinationsVersonized) {
      String output = String.format(format, de.getKey().getOrderType(), de.getValue().getFqName());
      writeLineToCommandLine(statusOutputStream, output);
    }

  }

}
