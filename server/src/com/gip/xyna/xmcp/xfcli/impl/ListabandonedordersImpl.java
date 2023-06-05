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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listabandonedorders;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.AbandonedOrderInformationBean;



public class ListabandonedordersImpl extends XynaCommandImplementation<Listabandonedorders> {

  public void execute(OutputStream statusOutputStream, Listabandonedorders payload) throws XynaException {

    boolean verbose = payload.getVerbose();
    List<AbandonedOrderInformationBean> aodList =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getAbandonedOrdersManagement()
            .listAbandonedOrders();

    if (aodList.size() == 0) {
      writeLineToCommandLine(statusOutputStream, "No abandoned orders found.");
      writeLineToCommandLine(statusOutputStream, "It may be necessary to perform a discovery first by calling './"
          + Constants.SERVER_SHELLNAME + " discoverabandonedorders'.");
    } else {
      writeLineToCommandLine(statusOutputStream, "Listing abandoned orders:");
      writeLineToCommandLine(statusOutputStream,
                             String.format("%10s  %10s  %10s  %s\n", "entryID", "orderID", "rootOrderID", "Reason for abandoning the order"));

      for (AbandonedOrderInformationBean aoib : aodList) {
        String outputLine = String.format("%10d  %10d  %10d  %15s\n", aoib.getID(), aoib.getOrderID(), aoib.getRootOrderID(), aoib.getShortDescription());

        if (verbose) {
          outputLine += String.format("%10s  %10s  %10s  %s\n", "", "", "", aoib.getProblemDescription());
          outputLine += String.format("%10s  %10s  %10s  %s\n", "", "", "", aoib.getProposedSolution());
        }

        writeToCommandLine(statusOutputStream, outputLine);
      }
    }
  }

}
