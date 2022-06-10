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
package com.gip.xyna.xmcp.xfcli.impl;



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import com.gip.xyna.xmcp.xfcli.generated.Forcecleanabandonedorder;



public class ForcecleanabandonedorderImpl extends XynaCommandImplementation<Forcecleanabandonedorder> {

  public void execute(OutputStream statusOutputStream, Forcecleanabandonedorder payload) throws XynaException {
    if (payload.getAll_unintentionally()) {
      int forcedCount =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
              .forceCleanAllAbandonedOrders();
      writeLineToCommandLine(statusOutputStream, "Cleaned " + forcedCount
          + " problems with abandoned orders forcefully.");
    } else if (payload.getEntryID() != null) {
      Long entryID = Long.valueOf(payload.getEntryID());

      boolean success = false;

      if (payload.getFamily()) {
        success =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
                .forceCleanAbandonedOrderFamily(entryID);
      } else {
        success =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
                .forceCleanAbandonedOrder(entryID, payload.getUsing_all_detection_rules());
      }

      if (success) {
        writeLineToCommandLine(statusOutputStream, "Abandoned order has been forcefully cleaned.");
      } else {
        writeLineToCommandLine(statusOutputStream, "Could not force clean abandoned order.");
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "Insufficient parameters passed.");
    }
  }

}
