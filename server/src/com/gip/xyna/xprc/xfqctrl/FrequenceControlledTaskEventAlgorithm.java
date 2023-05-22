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
package com.gip.xyna.xprc.xfqctrl;



import java.util.concurrent.atomic.AtomicBoolean;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;



public abstract class FrequenceControlledTaskEventAlgorithm implements Runnable {


  private FrequencyControlledTask controlledTask;
  protected final long eventsToLaunch;

  private final AtomicBoolean executionPermit = new AtomicBoolean(true);


  FrequenceControlledTaskEventAlgorithm(FrequencyControlledTaskCreationParameter creationParams) {
    this.eventsToLaunch = creationParams.getEventsToLaunch();
  }


  public final static FrequenceControlledTaskEventAlgorithm createEventCreationAlgorithm(FrequencyControlledTaskCreationParameter creationParams)
      throws XPRC_InvalidCreationParameters {
    FrequenceControlledTaskEventAlgorithm createdEventCreationAlgorithm;
    if (creationParams instanceof LoadControlledCreationParameter) {
      createdEventCreationAlgorithm =
          new ConstantLoadEventCreationAlgorithm(creationParams, (LoadControlledCreationParameter) creationParams);
    } else if (creationParams instanceof RateControlledCreationParameter) {
      createdEventCreationAlgorithm =
          new ConstantRateEventCreationAlgorithm(creationParams, (RateControlledCreationParameter) creationParams);
    } else if (creationParams.getAlgorithmParameters() != null) {
      if (creationParams.getAlgorithmParameters() instanceof LoadControlledCreationParameter) {
        createdEventCreationAlgorithm =
            new ConstantLoadEventCreationAlgorithm(creationParams,
                                                   (LoadControlledCreationParameter) creationParams.getAlgorithmParameters());
      } else if (creationParams.getAlgorithmParameters() instanceof RateControlledCreationParameter) {
        createdEventCreationAlgorithm =
            new ConstantRateEventCreationAlgorithm(creationParams,
                                                   (RateControlledCreationParameter) creationParams.getAlgorithmParameters());
      } else {
        throw new XPRC_InvalidCreationParameters("Unknown EventCreationAlgorithm requested: "
            + creationParams.getAlgorithmParameters().getClass().getName());
      }
    } else {
      throw new XPRC_InvalidCreationParameters("Unknown EventCreationAlgorithm requested: " + creationParams.getClass().getName());
    }
    
    return createdEventCreationAlgorithm;
  }


  synchronized void revokeExecutionPermit() {
    //don't abort if we revoke while already paused, we want to notify in case of paused tasks that are now canceled
    executionPermit.compareAndSet(true, false);
    this.notifyAll();
  }


  synchronized void grantExecutionPermit() {
    // we could catch grants on running task...but who cares anyway?
    if (executionPermit.compareAndSet(false, true)) {
      this.notifyAll();
    }
  }


  // Subtypes might want additional checks
  protected synchronized boolean executionIsPermitted() {
    if (executionPermit.get()) {
      return true;
    } else if (controlledTask.getStatus().isFinished()) {
      return false;
    } else {
      while (!executionPermit.get()) {
        try {
          this.wait(1000);
        } catch (InterruptedException e) {
          // doesn't matter
        }
        if (controlledTask.getStatus().isFinished()) {
          return false;
        }
      }
      return true;
    }

  }


  void registerTask(FrequencyControlledTask task) {
    controlledTask = task;
  }


  public abstract void eventFinished();


  protected FrequencyControlledTask getControlledTask() {
    return controlledTask;
  }


  public abstract String getEventGenerationInformation();


  public long getEventsToLaunch() {
    return eventsToLaunch;
  }

}
