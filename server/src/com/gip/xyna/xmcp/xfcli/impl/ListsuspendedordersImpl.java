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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listsuspendedorders;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public class ListsuspendedordersImpl extends XynaCommandImplementation<Listsuspendedorders> {

  public void execute(OutputStream statusOutputStream, Listsuspendedorders payload) throws XynaException {

    ODSConnection defaultConnection = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {

      // this uses a cursor to "stream" the data in the case in which many orders are present within the system
      FactoryWarehouseCursor<OrderInstanceBackup> suspendedOrders =
          factory.getProcessingPortal().listSuspendedOrders(defaultConnection);

      List<OrderInstanceBackup> suspendedOrdersList = suspendedOrders.getRemainingCacheOrNextIfEmpty();
      if (suspendedOrdersList == null || suspendedOrdersList.size() == 0) {
        writeLineToCommandLine(statusOutputStream, "No orders are currently suspended.");
        return;
      }

      while (suspendedOrdersList != null && suspendedOrdersList.size() > 0) {
        for (OrderInstanceBackup entry : suspendedOrdersList) {
          writeLineToCommandLine(statusOutputStream, "\t* ID " + entry.getXynaorder().getId() + ": order type '"
              + entry.getXynaorder().getDestinationKey().getOrderType() + "'");
        }
        suspendedOrdersList = suspendedOrders.getRemainingCacheOrNextIfEmpty();
      }
      
    } finally {
      defaultConnection.closeConnection();
    }

  }

}
