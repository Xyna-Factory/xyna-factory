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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Resolveabandonedorder;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_IncompleteIntentionallyAbandonedOrder;
import com.gip.xyna.xprc.exceptions.XPRC_UnknownIntentionallyAbandonedOrderID;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolvedAbandonedOrdersBean;



public class ResolveabandonedorderImpl extends XynaCommandImplementation<Resolveabandonedorder> {

  private Logger logger = CentralFactoryLogging.getLogger(ResolveabandonedorderImpl.class);


  public void execute(OutputStream statusOutputStream, Resolveabandonedorder payload) throws PersistenceLayerException {

    if (payload.getAll()) {
      ResolvedAbandonedOrdersBean resolved =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
              .resolveAllAbandonedOrders();
      writeLineToCommandLine(statusOutputStream,
                             "Resolved " + resolved.getResolved() + " of " + resolved.getDiscovered()
                                 + " problems with abandoned orders.");
    } else if (payload.getEntryID() != null) {
      Long orderID = Long.valueOf(payload.getEntryID());
      boolean success;
      try {
        success =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
                .resolveAbandonedOrder(orderID, statusOutputStream);
      } catch (XPRC_UnknownIntentionallyAbandonedOrderID e) {
        writeLineToCommandLine(statusOutputStream, "The specified ID is unknown.");
        return;
      } catch (XPRC_IncompleteIntentionallyAbandonedOrder e) {
        logger.info("Failed to resolve abandoned order.", e);
        writeLineToCommandLine(statusOutputStream, "Information on the specified ID is incomplete, "
            + "please refer to the log file for further information.");
        return;
      }

      if (success) {
        writeLineToCommandLine(statusOutputStream, "Abandoned order has been resolved.");
      } else {
        writeLineToCommandLine(statusOutputStream, "Could not resolve abandoned order.");
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "No parameters passed.");
    }

  }

}
