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
package com.gip.xyna.xprc.xbatchmgmt;

import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xprc.xsched.capacities.MultiAllocationCapacities;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;


public class BatchProcessSlaveCreatorTask implements ParallelTask {

  private final BatchProcess batchProcess;  
  private final int allocatedCaps;
  private final TransferCapacities transferCapacities;
  private final ParallelExecutor parallelExecutor;
  
  public BatchProcessSlaveCreatorTask(BatchProcess batchProcess, ParallelExecutor parallelExecutor,
                                      Long orderId, MultiAllocationCapacities mac) {
    this.batchProcess = batchProcess;
    this.parallelExecutor = parallelExecutor;
    this.allocatedCaps = mac.getAllocations();
    this.transferCapacities = new TransferCapacities(orderId, mac.getCapacities());
  }

  public int getPriority() {
    return -10; //niedriger als SlaveTask: Es ist wichtiger, dass SlaveTask abgearbeitet werden 
    //als dass neue erstellt werden
  }

  /**
   * Holt sich die nächsten Inputs für die Slaves, legt SlaveTasks zum Starten
   * der Slaves an und führt diese im ParallelExecutor aus.
   */
  public void execute() {
    //SlaveTasks in den ParallelExecutor einstellen
    int numberOfSlaves = allocatedCaps;
    //Wenn die Anzahl der zu startenden Slaves nicht durch Kapazitäten beschränkt ist,
    //sollen "maxParallelism" Slaves gestartet werden
    if (allocatedCaps == 0) {
      numberOfSlaves = batchProcess.getMaxParallelism(); 
    }

    for (int i=0; i<numberOfSlaves; i++) {
      SlaveTask newTask = batchProcess.createSlaveTask(transferCapacities);
      if( newTask == null ) {
        break;
      }
      parallelExecutor.addTask( newTask );
    }
    
    batchProcess.checkNextSlavesCanBeStarted();
  }
  
}
