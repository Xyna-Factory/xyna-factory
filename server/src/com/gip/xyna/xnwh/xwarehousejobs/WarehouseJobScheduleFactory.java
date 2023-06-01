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

package com.gip.xyna.xnwh.xwarehousejobs;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.timing.TimedTasks;
import com.gip.xyna.utils.timing.TimedTasks.Executor;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_WarehouseJobScheduleParameterInvalidException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderStatus;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler.CronLikeOrderPersistenceOption;


public class WarehouseJobScheduleFactory {

  private static final Logger logger = CentralFactoryLogging.getLogger(WarehouseJobScheduleFactory.class);
  private static final Executor<DestinationKey> warehouseJobCleanupExecutor = new WarehouseJobCleanupExecutor();
  private static final TimedTasks<DestinationKey> cleanupTasks = new TimedTasks<DestinationKey>("LateWarehouseJobCleanup", warehouseJobCleanupExecutor, true);

  public enum WarehouseJobScheduleType {
    CRONLS, SHUTDOWN, STARTUP, LIMIT;

    public static WarehouseJobScheduleType fromString(String s) {
      if (s == null) {
        throw new IllegalArgumentException("Argument may not be null");
      } else if (s.equals(CRONLS.toString())) {
        return CRONLS;
      } else if (s.equals(SHUTDOWN.toString())) {
        return SHUTDOWN;
      } else if (s.equals(STARTUP.toString())) {
        return STARTUP;
      } else if (s.equals(LIMIT.toString())) {
        return LIMIT;
      } else {
        throw new IllegalArgumentException(WarehouseJobScheduleType.class.getSimpleName() + " '" + s + "' is unknown.");
      }
    }
  }


  private static AtomicLong cnt = new AtomicLong(0);


  private static final DestinationKey getNewDestinationKey(WarehouseJobScheduleType type) {
    return new DestinationKey(type.toString() + "_Job_" + cnt.getAndIncrement());
  }


  /**
   * Creates a {@link WarehouseJobSchedule} object for a {@link WarehouseJobRunnable} that registers the runnable to be
   * executed on a regular basis.
   * 
   * @param {@link WarehouseJobRunnable} - the job that is executed
   * @return {@link WarehouseJobSchedule} - the schedule object that can perform the registration and unregistration
   */
  static WarehouseJobSchedule getCronSchedule(final WarehouseJobRunnable jobRunnable, final Long interval,
                                              final Long firstStartTime) {

    return new WarehouseJobSchedule() {

      CronLikeOrder underlyingCronLSJob;
      DestinationKey dk;
      private String[] scheduleParameters;


      @Override
      public final void registerInternally() {

        logger.debug("Registering ");

        dk = getNewDestinationKey(WarehouseJobScheduleType.CRONLS);

        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                        .getPlanningDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, true);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                        .getExecutionEngineDispatcher().setDestination(dk, jobRunnable, true);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
                        .getCleanupEngineDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW,
                                                                     true);

        try {
          CronLikeOrderCreationParameter xocp = new CronLikeOrderCreationParameter(dk, firstStartTime, interval);
          underlyingCronLSJob = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                          .createCronLikeOrder(xocp, CronLikeOrderPersistenceOption.REMOVE_ON_SHUTDOWN, null);
        } catch (XPRC_CronLikeSchedulerException e) {
          // TODO throw better exception?
          throw new RuntimeException("Error while registering cron warehouse job.", e);
        } catch (XNWH_RetryTransactionException e) {
          // TODO throw better exception?
          throw new RuntimeException("Error while registering cron warehouse job.", e);
        }

      }


      @Override
      public final void unregisterInternally() {

        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                        .getPlanningDispatcher().removeDestination(dk);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                        .getExecutionEngineDispatcher().removeDestination(dk);
        cleanupTasks.addTask(System.currentTimeMillis() + 500, dk);
        
        if (underlyingCronLSJob != null) {
          try {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                            .removeCronLikeOrder(underlyingCronLSJob);
          } catch (XPRC_CronLikeSchedulerException e) {
            // TODO throw better exception?
            throw new RuntimeException("Error while unregistering cron warehouse job.", e);
          }
        } else {
          logger.warn("Could not unregister " + getType().toString() + "-job '"
                          + jobRunnable.getParentJob().getDescription() + "' because job information was not found");
        }
        

      }


      @Override
      public final WarehouseJobScheduleType getType() {
        return WarehouseJobScheduleType.CRONLS;
      }


      @Override
      public String[] getScheduleParameters() {
        if (scheduleParameters == null) {
          scheduleParameters = new String[] {interval + "", firstStartTime + ""};
        }
        return scheduleParameters;
      }


      @Override
      public boolean needsToRunAgain() {
        if (underlyingCronLSJob == null) {
          return false;
        }
        if (CronLikeOrderStatus.RUNNING_SINGLE_EXECUTION.equals(underlyingCronLSJob.getStatus())) {
          return false;
        }
        return true;
      }

    };

  }


  /**
   * Creates a {@link WarehouseJobSchedule} object for a {@link WarehouseJobRunnable} that registers the runnable to be
   * executed every time the warehouse is shutdown.
   * @param {@link WarehouseJobRunnable} - the job that is executed
   * @return {@link WarehouseJobSchedule} - the schedule object that can perform the registration and unregistration
   */
  static WarehouseJobSchedule getShutdownSchedule(final WarehouseJobRunnable jobRunnable) {

    return new WarehouseJobSchedule() {

      XynaOrderServerExtension xo;


      @Override
      public WarehouseJobScheduleType getType() {
        return WarehouseJobScheduleType.SHUTDOWN;
      }


      @Override
      public void registerInternally() {

        DestinationKey dk = getNewDestinationKey(WarehouseJobScheduleType.SHUTDOWN);
        xo = new XynaOrderServerExtension(dk);

        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                        .getPlanningDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, true);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                        .getExecutionEngineDispatcher().setDestination(dk, jobRunnable, true);
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
                        .getCleanupEngineDispatcher().setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, true);

        XynaFactory.getInstance().getXynaNetworkWarehouse().addShutdownWarehouseJobOrder(xo);
      }


      @Override
      public void unregisterInternally() {
        if (xo != null) {
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                          .getPlanningDispatcher().removeDestination(xo.getDestinationKey());
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                          .getExecutionEngineDispatcher().removeDestination(xo.getDestinationKey());
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
                          .getCleanupEngineDispatcher().removeDestination(xo.getDestinationKey());
          XynaFactory.getInstance().getXynaNetworkWarehouse().removeShutdownWarehouseJobOrder(xo);
        } else
          logger.warn("Could not unregister " + getType().toString() + "-job '"
                          + jobRunnable.getParentJob().getDescription() + "' because job information was not found");
      }


      @Override
      public String[] getScheduleParameters() {
        return new String[0];
      }


      @Override
      public boolean needsToRunAgain() {
        return true;
      }

    };

  }


  static WarehouseJobSchedule getScheduleByType(WarehouseJobRunnable runnable, WarehouseJobScheduleType scheduleType,
                                                String... parameters) throws XNWH_WarehouseJobScheduleParameterInvalidException {

    if (scheduleType == WarehouseJobScheduleType.CRONLS) {
      try {
        Long interval = parameters[0] == null || parameters[0].equals("null") ? null : Long.valueOf(parameters[0]);
        try {
          Long firstStartup = parameters[1] == null || parameters[1].equals("null") ? null : Long.valueOf(parameters[1]);
          return getCronSchedule(runnable, interval, firstStartup);
        } catch (NumberFormatException e) {
          throw new XNWH_WarehouseJobScheduleParameterInvalidException("firstStartup", parameters[1], e);
        }
      } catch (NumberFormatException e) {
        throw new XNWH_WarehouseJobScheduleParameterInvalidException("interval", parameters[0], e);
      }
    } else {
      throw new RuntimeException("Unsupported schedule type: " + scheduleType.toString());
    }

  }
  
  
  private static class WarehouseJobCleanupExecutor implements Executor<DestinationKey> {

    public void execute(DestinationKey dk) {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
        .getCleanupEngineDispatcher().removeDestination(dk);
      
    }

    public void handleThrowable(Throwable executeFailed) {
      logger.warn("WarehouseJobCleanup failed there might be left over entries in the CleanupDispatcher", executeFailed);
    }
    
  }

}
