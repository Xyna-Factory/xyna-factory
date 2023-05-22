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

import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listwarehousejobs;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJob;
import com.gip.xyna.xnwh.xwarehousejobs.XynaWarehouseJobManagement;


public class ListwarehousejobsImpl extends XynaCommandImplementation<Listwarehousejobs> {

  public void execute(OutputStream statusOutputStream, Listwarehousejobs payload) throws XynaException{


    XynaWarehouseJobManagement jobMgmt =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaWarehouseJobManagement();
    Map<Long, WarehouseJob> jobs = jobMgmt.getJobs();
    if (jobs == null || jobs.size() == 0) {
      writeLineToCommandLine(statusOutputStream, "No registered warehouse jobs.");
      return;
    }

    writeLineToCommandLine(statusOutputStream, "Listing information on " + jobs.size() + " warehouse jobs:");
    for (Entry<Long, WarehouseJob> e : jobs.entrySet()) {
      WarehouseJob next = e.getValue();
      StringBuilder message = new StringBuilder("ID ");
      message.append(e.getKey()).append(":\t'").append(next.getName()).append("' (Type: ")
          .append(next.getScheduleType()).append(", description: ").append(next.getDescription()).append(")");
      writeLineToCommandLine(statusOutputStream, message.toString());

    }

  
  }

}
