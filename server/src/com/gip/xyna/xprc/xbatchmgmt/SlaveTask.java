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
package com.gip.xyna.xprc.xbatchmgmt;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;


/**
 * Task, um einen Slave zu starten.
 *
 */
public class SlaveTask implements ParallelTask {
  
  private static Logger logger = CentralFactoryLogging.getLogger(SlaveTask.class);

  private final BatchProcess batchProcess;
  private final XynaOrderServerExtension xynaOrder;
  private final String inputId;
  private final TransferCapacities transferCapacities;
  
  
  public SlaveTask(BatchProcess batchProcess, XynaOrderServerExtension xynaOrder, String inputId, TransferCapacities transferCapacities) {
    super();
    this.batchProcess = batchProcess;
    this.xynaOrder = xynaOrder;
    this.inputId = inputId;
    this.transferCapacities = transferCapacities;
  }

  public int getPriority() {
    return 0;
  }

  public void execute() {
    try {
      batchProcess.startSlaveOrder(xynaOrder, inputId, transferCapacities);
    }
    catch (Throwable t) {
      logger.error("SlaveTask failed -> pause batch process", t);
      batchProcess.handleThrowable(t);
    }
  }
 
}
