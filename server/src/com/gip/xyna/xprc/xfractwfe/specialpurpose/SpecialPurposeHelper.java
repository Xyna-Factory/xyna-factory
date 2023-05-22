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
package com.gip.xyna.xprc.xfractwfe.specialpurpose;



import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;



public class SpecialPurposeHelper extends FunctionGroup {


  public static final String DEFAULT_NAME = "SpecialPurposeHelper";

  PreparedQuery<OrderCount> getCountOfCronLikeOrdersWithRootOrderId;
  PreparedQuery<OrderCount> getCountOfManualInteractionEntriesWithRootOrderId;


  public SpecialPurposeHelper() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(SpecialPurposeHelper.class, DEFAULT_NAME).
      after(CronLikeScheduler.class).after(ManualInteractionManagement.FUTURE_EXECUTION_ID).
      execAsync(new Runnable() { public void run() { initSpecialPurposeHelper(); }});
  }

  public void initSpecialPurposeHelper() {
    ODSConnection con = ODSImpl.getInstance().openConnection();
    try {
      getCountOfCronLikeOrdersWithRootOrderId =
          con.prepareQuery(new Query<OrderCount>("select count(*) from " + CronLikeOrder.TABLE_NAME + " where "
              + CronLikeOrder.COL_ASSIGNED_ROOT_ORDER_ID + "=?", OrderCount.getCountReader()), true);

      getCountOfManualInteractionEntriesWithRootOrderId =
          con.prepareQuery(new Query<OrderCount>("select count(*) from " + ManualInteractionEntry.TABLE_NAME
                               + " where " + ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID + "=?", OrderCount
                               .getCountReader()), true);

    } catch (PersistenceLayerException e) {
      logger.error("Failed to prepare query.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.", e);
      }
    }

  }
  
  
  @Override
  protected void shutdown() throws XynaException {
    // nothing to done?
  }


  public boolean containsResumerForOrder(Long orderId, Long rootOrderId, ODSConnection con)
      throws PersistenceLayerException {
    // MI
    OrderCount count = con.queryOneRow(getCountOfManualInteractionEntriesWithRootOrderId, new Parameter(rootOrderId));
    if (count.getCount() > 0) {
      return true;
    }

    // cron with rootOrderId
    count = con.queryOneRow(getCountOfCronLikeOrdersWithRootOrderId, new Parameter(rootOrderId));
    if (count.getCount() > 0) {
      return true;
    }

    return false;
  }

}
