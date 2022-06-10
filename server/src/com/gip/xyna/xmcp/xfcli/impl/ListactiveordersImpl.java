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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listactiveorders;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.OrderInfo;



public class ListactiveordersImpl extends XynaCommandImplementation<Listactiveorders> {

  public void execute(OutputStream statusOutputStream, Listactiveorders payload) throws XynaException {
    OrdersInUse ordersInUse;
    if (payload.getApplicationName() != null && payload.getApplicationName().length() > 0) {
      ApplicationManagementImpl applicationManagement =
                      (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                          .getApplicationManagement();
      ordersInUse = applicationManagement.listActiveOrders(payload.getApplicationName(),
                                                           payload.getVersionName(),
                                                           payload.getVerboseOrders(),
                                                           payload.getGlobal());
    } else {
      WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getWorkspaceManagement();
      ordersInUse = workspaceManagement.listActiveOrders(payload.getWorkspaceName(),
                                                         payload.getVerboseOrders(),
                                                         payload.getGlobal());
    }
    
    StringBuilder sb = new StringBuilder();
    Collection<OrderInfo> rootOrders = ordersInUse.getRunningRootOrders();
    sb.append(rootOrders.size() + " root orders found");
    sb.append(listOrders(rootOrders, payload.getVerboseOrders(), payload.getGlobal()));
    sb.append("\n");
    
    Collection<OrderInfo> batchProcesses = ordersInUse.getBatchProcesses();
    sb.append(batchProcesses.size() + " batchProcesses found");
    sb.append(listOrders(batchProcesses, payload.getVerboseTCOs(), payload.getGlobal()));
    sb.append("\n");
    
    Collection<OrderInfo> crons = ordersInUse.getCrons();
    sb.append(crons.size() + " cron like orders found");
    sb.append(listOrders(crons, payload.getVerboseTCOs(), payload.getGlobal()));
    sb.append("\n");
    
    Collection<OrderInfo> fcts = ordersInUse.getFrequencyControlledTasks();
    sb.append(fcts.size() + " frequency controlled tasks found");
    sb.append(listOrders(fcts, payload.getVerboseFCTs(), payload.getGlobal()));
    
    writeLineToCommandLine(statusOutputStream, sb.toString());
  }

  
  private String listOrders(Collection<OrderInfo> orders, boolean verbose, boolean global) {
    StringBuilder sb = new StringBuilder();
    Map<String, ArrayList<OrderInfo>> group1 = OrderInfo.groupByOrderType(orders);
    for (String orderType : group1.keySet()) {
      sb.append("\n").append(" * ").append(orderType);
      Map<String, ArrayList<OrderInfo>> group2 = OrderInfo.groupByStatus(group1.get(orderType));
      for (String status : group2.keySet()) {
        if (status != null) {
          sb.append("\n    ").append(status);
        }
        if (global) {
          Map<Integer, ArrayList<OrderInfo>> group3 = OrderInfo.groupByBinding(group2.get(status));
          for (Integer binding : group3.keySet()) {
            sb.append("\n      binding ").append(binding).append(": ");
            if (verbose) {
              sb.append(OrderInfo.getOrderIds(group3.get(binding)));
            } else {
              sb.append(group3.get(binding).size());
            }
          }
        } else {
          sb.append(": ");
          if (verbose) {
            sb.append(OrderInfo.getOrderIds(group2.get(status)));
          } else {
            sb.append(group2.get(status).size());
          }
        }
      }
    }
    
    return sb.toString();
  }
}
