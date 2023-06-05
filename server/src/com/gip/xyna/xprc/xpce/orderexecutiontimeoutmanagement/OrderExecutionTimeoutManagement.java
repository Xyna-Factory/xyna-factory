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
package com.gip.xyna.xprc.xpce.orderexecutiontimeoutmanagement;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;



public class OrderExecutionTimeoutManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "OrderExecutionTimeout Management";

  static {
    addDependencies(OrderExecutionTimeoutManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryManagementODS.class,
                                                                           Configuration.class)})));
  }

  private final ScheduledExecutorService cancelTaskScheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {

    public Thread newThread(Runnable r) {
      return new Thread(r, "CancelTask Thread");
    }
  });

  private final ConcurrentMap<Long, ScheduledFuture<KillStuckProcessBean>> scheduledCancelTasks =
      new ConcurrentHashMap<Long, ScheduledFuture<KillStuckProcessBean>>();


  public OrderExecutionTimeoutManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void registerOrderTimeout(XynaOrderServerExtension orderWithTTL) {
    if (scheduledCancelTasks.containsKey(orderWithTTL.getId())) {
      //die map wird nur vom auftrag aufgeräumt (auch wenn er gecancelt wird), nicht von dem cancel-auftrag, damit 
      //nicht ein weiteres canceltask erstellt wird, wenn der auftrag suspendiert und resumed.
      return;
    }
    AbortionTask cancelToSchedule =
        new AbortionTask(orderWithTTL.getId());
    ScheduledFuture<KillStuckProcessBean> pendingCancel =
        cancelTaskScheduler.schedule(cancelToSchedule,
                                     orderWithTTL.getOrderExecutionTimeout()
                                         .getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
    scheduledCancelTasks.put(orderWithTTL.getId(), pendingCancel);
  }


  public void tryUnregisterOrderTimeout(XynaOrderServerExtension orderWithTTL) {
    ScheduledFuture<KillStuckProcessBean> pendingCancel = scheduledCancelTasks.remove(orderWithTTL.getId());
    if (pendingCancel != null) {
      pendingCancel.cancel(false);
    }
  }


  private class AbortionTask implements Callable<KillStuckProcessBean> {

    private final long orderId;


    AbortionTask(long orderId) {
      this.orderId = orderId;
    }


    public KillStuckProcessBean call() throws Exception {
      try {
        KillStuckProcessBean bean = new KillStuckProcessBean(orderId, false, AbortionCause.TIME_TO_LIVE_EXPIRATION);
        ((XynaProcessing) XynaFactory.getInstance().getProcessing()).killStuckProcess(bean, false, null);
        return bean;
      } catch (XynaException e) {
        logger.warn("Order " + orderId + " could not be cancelled.", e);
        return null;
      }
    }
  }


  @Override
  protected void init() throws XynaException {
  }


  @Override
  protected void shutdown() throws XynaException {

  }

}
