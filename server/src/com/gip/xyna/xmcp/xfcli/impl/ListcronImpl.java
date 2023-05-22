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
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcron;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderStatus;


public class ListcronImpl extends XynaCommandImplementation<Listcron> {

  @Override
  public void execute(OutputStream statusOutputStream, Listcron payload) throws XynaException{

    if (payload.getOnlymemory()) {
      List<CronLikeOrder> ordersInMemory =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().getAllQueuedOrders();
      if (ordersInMemory.size() == 0) {
        writeLineToCommandLine(statusOutputStream, "No cron like orders in memory.");
      } else {
        writeLineToCommandLine(statusOutputStream, ordersInMemory.size(), " cron like orders in memory:");
        SimpleDateFormat format = Constants.defaultUTCSimpleDateFormat();
        for (CronLikeOrder order : ordersInMemory) {
          writeLineToCommandLine(statusOutputStream, "\t* ", order.getId(), ": ", order.getCreationParameters()
                                     .getDestinationKey().getOrderType(), ", next execution time: ",
                                 order.getNextExecution(), "(",
                                 format.format(new Date(order.getNextExecution())), ")");
        }
      }
      return;
    }

    ProcessMonitoring procMon = factory.getFactoryManagementPortal().getProcessMonitoring();
    Map<Long, CronLikeOrderInformation> cronLikeOrders = procMon.getAllCronLikeOrders(Long.MAX_VALUE);
    Iterator<Entry<Long, CronLikeOrderInformation>> iter = cronLikeOrders.entrySet().iterator();

    if (iter.hasNext()) {
      writeLineToCommandLine(statusOutputStream, "Found information on ", cronLikeOrders.size(), " cron like order"
          + (cronLikeOrders.size() == 1 ? ":" : "s:"));

      if (payload.getAsTable()) {
        writeLineToCommandLine(statusOutputStream,
                               String
                                   .format("%10s  %30s  %30s  %20s  %25s  %20s  %25s  %12s  %7s  %26s  %28s  %12s  %12s  %12s",
                                           "id", "OrderType", "label", "execution type", "initial start time",
                                           "time zone", "next execution time", "consider DST", "enabled",
                                           "behaviour in case of error", "status", "application", "versionName", "workspace"));
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "No cron like orders found.");
    }

    while (iter.hasNext()) {
      Entry<Long, CronLikeOrderInformation> next = iter.next();

      if (payload.getAsTable()) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        dateFormatter.setTimeZone(TimeZone.getTimeZone(next.getValue().getTimeZoneID()));
        dateFormatter.setLenient(false);

        String output;

        if (next.getValue().getStatus().equals(CronLikeOrderStatus.ERROR)) {
          output =
              String.format("%10d  %30s  %30s  %20s  %25s  %20s  %25s  %12b  %7b  %26s  %28s (%s)  %12s  %12s  %12s", next
                                .getKey(), next.getValue().getTargetOrdertype(), next.getValue().getLabel(), (next
                                .getValue().getInterval().longValue() == 0) ? "single execution" : "interval "
                                + next.getValue().getInterval(),
                            dateFormatter.format(new Date(next.getValue().getStartTime())), next.getValue()
                                .getTimeZoneID(), dateFormatter.format(new Date(next.getValue().getNextExecution())),
                            next.getValue().getConsiderDaylightSaving(), next.getValue().isEnabled(), next.getValue()
                                .getOnError(), next.getValue().getStatus(), next.getValue().getErrorMessage(), next
                                .getValue().getApplicationName(), next.getValue().getVersionName(), next.getValue().getWorkspaceName());
        } else {
          output =
              String.format("%10d  %30s  %30s  %20s  %25s  %20s  %25s  %12b  %7b  %26s  %28s  %12s  %12s  %12s", next
                                .getKey(), next.getValue().getTargetOrdertype(), next.getValue().getLabel(), (next
                                .getValue().getInterval().longValue() == 0) ? "single execution" : "interval "
                                + next.getValue().getInterval(),
                            dateFormatter.format(new Date(next.getValue().getStartTime())), next.getValue()
                                .getTimeZoneID(), dateFormatter.format(new Date(next.getValue().getNextExecution())),
                            next.getValue().getConsiderDaylightSaving(), next.getValue().isEnabled(), next.getValue()
                                .getOnError(), next.getValue().getStatus(), next.getValue().getApplicationName(), next
                                .getValue().getVersionName(), next.getValue().getWorkspaceName());
        }

        writeLineToCommandLine(statusOutputStream, output);
      } else {
        //id
        StringBuilder output = new StringBuilder("Cron like order found with ID ").append(next.getKey()).append(": ");
        //orderType
        output.append("OrderType '").append(next.getValue().getTargetOrdertype());
        //label
        if (next.getValue().getLabel() != null) {
          output.append("', label '").append(next.getValue().getLabel());
        }
        //interval
        if (next.getValue().getInterval() != null) {
          if (next.getValue().getInterval().longValue() == 0) {
            output.append("', single execution '");
          } else {
            output.append("', interval '").append(next.getValue().getInterval());
          }
        }
        SimpleDateFormat dateFormatter = Constants.defaultUTCSimpleDateFormat();
        //first start time
        if (next.getValue().getStartTime() != null && next.getValue().getStartTime() > 0) {
          output.append("', initial start time '").append(dateFormatter
                                                              .format(new Date(next.getValue().getStartTime())));
        }
        //next execution time
        if (next.getValue().getNextExecution() != null && next.getValue().getNextExecution() > 0) {
          output.append("', next execution time '").append(dateFormatter.format(new Date(next.getValue()
                                                               .getNextExecution())));
        }
        //enabled
        if (next.getValue().isEnabled() != null) {
          if (next.getValue().isEnabled()) {
            output.append("', is enabled ");
          } else {
            output.append("', is disabled ");
          }
        }
        //onError
        if (next.getValue().getOnError() != null) {
          output.append(", behaviour in case of error '").append(next.getValue().getOnError());
        }
        //status
        if (next.getValue().getStatus() != null) {
          output.append("', status '").append(next.getValue().getStatus()).append("'");
          if (next.getValue().getStatus().equals(CronLikeOrderStatus.ERROR)) {
            output.append(" (").append(next.getValue().getErrorMessage()).append(")");
          }
        } else {
          output.append("', status unknown");
        }
        // application
        if (next.getValue().getApplicationName() != null) {
          output.append(", application '").append(next.getValue().getApplicationName()).append("' versionName '")
              .append(next.getValue().getVersionName()).append("'");
        }
        
        // workspace
        if (next.getValue().getWorkspaceName() != null && !next.getValue().getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
          output.append(", workspace '").append(next.getValue().getWorkspaceName()).append("'");
        }
        
        output.append(".");
        writeLineToCommandLine(statusOutputStream, output);
      }
    }
  }
}
