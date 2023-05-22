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
import java.util.Collection;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement.OrdertypeFormatLength;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listordercontextmappings;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class ListordercontextmappingsImpl extends XynaCommandImplementation<Listordercontextmappings> {

  public void execute(OutputStream statusOutputStream, Listordercontextmappings payload) throws XynaException {

    Collection<DestinationKey> configuredDestinationKeys =
        factory.getProcessingPortal().getAllDestinationKeysForWhichAnOrderContextMappingIsCreated();
    
    OrdertypeFormatLength formatLengths = OrdertypeManagement.calculateOrdertypeFormatParameter(configuredDestinationKeys);
    String format = " %-" + formatLengths.ordertypeLength + "s  %-" + formatLengths.applicationLength + "s %-" + formatLengths.versionLength + "s %-" + formatLengths.workspaceLength + "s";
    
    if (configuredDestinationKeys != null && configuredDestinationKeys.size() > 0) {
      writeLineToCommandLine(statusOutputStream, "Order context information is made available for the following "
          + configuredDestinationKeys.size() + " order types:");
      writeLineToCommandLine(statusOutputStream, String.format(format, "Ordertype", "ApplicationName","VersionName", "WorkspaceName"));
      for (DestinationKey dk : configuredDestinationKeys) {
        writeLineToCommandLine(statusOutputStream, String.format(format, dk.getOrderType(),
                                                                 dk.getApplicationName() == null ? "" : dk.getApplicationName(),
                                                                 dk.getVersionName() == null ? "" : dk.getVersionName(),
                                                                 dk.getWorkspaceName() == null ? "" : dk.getWorkspaceName()));
      }
    } else {
      writeLineToCommandLine(statusOutputStream,
                             "Currently for no order type the order context information is made available.");
    }
    writeLineToCommandLine(statusOutputStream, "Note that this result might be overriden by the property "
        + XynaProperty.XYNA_GLOBAL_ORDER_CONTEXT_SETTINGS + ".");
  }

}
