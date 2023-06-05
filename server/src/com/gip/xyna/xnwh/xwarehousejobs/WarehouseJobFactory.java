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



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_WarehouseJobRunnableParameterInvalidException;
import com.gip.xyna.xnwh.xwarehousejobs.WarehouseJobScheduleFactory.WarehouseJobScheduleType;



/**
 * This class is supposed to provide predefined {@link WarehouseJob} and {@link Runnable} implementations.
 */
public class WarehouseJobFactory {

  private static final Logger logger = CentralFactoryLogging.getLogger(WarehouseJobFactory.class);


  public static WarehouseJob getCronWarehouseJob(final String name, final WarehouseJobActionType actionType,
                                                 Long interval, Long firstStartupAbsolte, String... actionParameters)
      throws XNWH_WarehouseJobRunnableParameterInvalidException {
    return getCronWarehouseJob(name, actionType, "No description given for warehouse job", interval,
                               firstStartupAbsolte, actionParameters);
  }


  public static WarehouseJob getCronWarehouseJob(final String name, final WarehouseJobActionType actionType,
                                                 final String description, final Long interval,
                                                 final Long firstStartupAbsolte, String... actionParameters)
      throws XNWH_WarehouseJobRunnableParameterInvalidException {

    if (logger.isDebugEnabled()) {
      logger.debug("Creating " + WarehouseJob.class.getSimpleName() + " (" + WarehouseJobScheduleType.CRONLS + ", "
          + description + ")");
    }

    long id = XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaWarehouseJobManagement().getNextJobId();
    WarehouseJob job = new WarehouseJob(id, name);
    job.setDescription(description);

    WarehouseJobRunnable jobRunnable = new WarehouseJobRunnable(job, description, actionType, actionParameters);

    WarehouseJobSchedule jobSchedule =
        WarehouseJobScheduleFactory.getCronSchedule(jobRunnable, interval, firstStartupAbsolte);

    job.setJobSchedule(jobSchedule);
    job.setRunnable(jobRunnable);
    return job;

  }


  public static WarehouseJob getShutdownWarehouseJob(final String name, final WarehouseJobActionType actionType,
                                                     final String description, final String... actionParameters)
      throws XNWH_WarehouseJobRunnableParameterInvalidException {

    if (logger.isDebugEnabled()) {
      logger.debug("Creating " + WarehouseJob.class.getSimpleName() + " (" + WarehouseJobScheduleType.SHUTDOWN + ", "
          + description + ")");
    }

    long id = XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaWarehouseJobManagement().getNextJobId();
    WarehouseJob job = new WarehouseJob(id, name);
    job.setDescription(description);

    WarehouseJobRunnable jobRunnable = new WarehouseJobRunnable(job, description, actionType, actionParameters);
    WarehouseJobSchedule jobSchedule = WarehouseJobScheduleFactory.getShutdownSchedule(jobRunnable);

    job.setJobSchedule(jobSchedule);
    job.setRunnable(jobRunnable);
    return job;

  }

}
