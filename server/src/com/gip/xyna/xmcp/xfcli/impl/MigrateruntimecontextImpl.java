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
import java.util.Arrays;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Migrateruntimecontext;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.ActiveOrderType;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationContext;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsBean;



public class MigrateruntimecontextImpl extends XynaCommandImplementation<Migrateruntimecontext> {

  public void execute(OutputStream statusOutputStream, Migrateruntimecontext payload) throws XynaException {
    RuntimeDependencyContext from = RuntimeContextDependencyManagement.getRuntimeDependencyContext(payload.getFromApplicationName(), payload.getFromVersionName(), payload.getFromWorkspaceName());
    RuntimeDependencyContext to = RuntimeContextDependencyManagement.getRuntimeDependencyContext(payload.getToApplicationName(), payload.getToVersionName(), payload.getToWorkspaceName());
    
    MigrationContext context = MigrateRuntimeContext.migrateRuntimeContext(from, to, Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()), payload.getForce());
    if (context.activeOrdersFound()) {
      writeLineToCommandLine(statusOutputStream, "Migration aborted. Caused by active orders, use -f to force the migration.");
      if (context.getActiveOrderIds(ActiveOrderType.CRON).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.CRON).size(), " affected Crons");
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.CRON));
      }
      if (context.getActiveOrderIds(ActiveOrderType.BATCH).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.BATCH).size(), " affected BatchProcesses");
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.BATCH));
      }
      if (context.getActiveOrderIds(ActiveOrderType.ORDER).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.ORDER).size(), " affected orders");
        writeLineToCommandLine(statusOutputStream, "  ", context.getActiveOrderIds(ActiveOrderType.ORDER));
      }
    } else {
      writeLineToCommandLine(statusOutputStream, "Migration finished");
      if (context.getAbortedOrderIds(ActiveOrderType.CRON).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.CRON).size(), " aborted Crons");
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.CRON));
      }
      if (context.getAbortedOrderIds(ActiveOrderType.BATCH).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.BATCH).size(), " aborted BatchProccesses");
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.BATCH));
      }
      if (context.getAbortedOrderIds(ActiveOrderType.ORDER).size() > 0) {
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.ORDER).size(), " aborted Orders");
        writeLineToCommandLine(statusOutputStream, "  ", context.getAbortedOrderIds(ActiveOrderType.ORDER));
      }
      Pair<SuspendRevisionsBean, XPRC_ResumeFailedException> resumeInfo = context.getResumeInformation();
      if (resumeInfo.getFirst().getSuspendedRootOrderIds().size() > 0) {
        writeToCommandLine(statusOutputStream, "  ", resumeInfo.getFirst().getSuspendedRootOrderIds().size(), " RootOrders were suspended");
        if (resumeInfo.getSecond() == null) {
          writeLineToCommandLine(statusOutputStream, " and resumed.");
        } else {
          writeLineToCommandLine(statusOutputStream, ".");
          writeLineToCommandLine(statusOutputStream, resumeInfo.getFirst().getSuspendedRootOrderIds());
          writeLineToCommandLine(statusOutputStream, "There has been an error during resume");
          throw resumeInfo.getSecond();
        }
      }
    }
  }

}
