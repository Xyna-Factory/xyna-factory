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
package com.gip.xyna.xprc.xfqctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;


public class ConstantLoadEventCreationAlgorithm extends FrequenceControlledTaskEventAlgorithm {

  public static final Logger logger = CentralFactoryLogging.getLogger(ConstantRateEventCreationAlgorithm.class);
  
  private final long maxLoad;
  private final long smoothingWait;
  private final AtomicLong eventIdGenerator = new AtomicLong(0);


  ConstantLoadEventCreationAlgorithm(FrequencyControlledTaskCreationParameter creationParams,
                                     LoadControlledCreationParameter algorithmParams) throws XPRC_InvalidCreationParameters {
    super(creationParams);
    long inputMaxLoad = algorithmParams.getMaxLoad();
    this.smoothingWait = algorithmParams.getSmoothingWait();
    if (inputMaxLoad < 1) {
      throw new XPRC_InvalidCreationParameters("Creation of FrequencyControlledTask with load parameter < 1 detected");
    }
    this.maxLoad = inputMaxLoad;
  }


  @Override
  public void eventFinished() {
    synchronized (this) {
      //logger.debug("notifying this now");
      this.notify();
    }
  }
  

  public void run() {
    try {
      // Mit maximaler Rate, nicht mehr Events gleichzeitig als maxLoad
      final FrequencyControlledTask controlledTask = getControlledTask();
      while (executionIsPermitted()) {
        long eventsToFire;
        final long remainingAllowedEventsForTask;
        synchronized (this) {
          eventsToFire = maxLoad - controlledTask.getAmountOfCurrentlyExecutingEvents();
          remainingAllowedEventsForTask = eventsToLaunch - controlledTask.getEventCount();
        }
        //logger.debug("eventsToFire " + eventsToFire);
        if (eventsToFire > 0) {
          //logger.debug("maxAllowedEventsForTask " + maxAllowedEventsForTask);
          if (eventsToFire >= remainingAllowedEventsForTask) {
            eventsToFire = remainingAllowedEventsForTask;
            if (logger.isDebugEnabled()) {
              logger.debug("restricting eventsToFire to " + remainingAllowedEventsForTask + "\n Stopping afterwards");
            }
            revokeExecutionPermit();
          }
          if (logger.isDebugEnabled()) {
            logger.debug("now firing event #" + eventsToFire);
          }
          if( smoothingWait <= 5) {
            //keine glättende Wartezeit
            for (int i = 0; i < eventsToFire; i++) {
              controlledTask.eventTriggered(eventIdGenerator.incrementAndGet());
            }
          } else {
            if( eventsToFire > 0 ) {
              controlledTask.eventTriggered(eventIdGenerator.incrementAndGet());
              for (int i = 1; i < eventsToFire; i++) {
                try {
                  Thread.sleep(smoothingWait);
                } catch( InterruptedException e ) {
                  //dann halt kürzer warten
                }
                controlledTask.eventTriggered(eventIdGenerator.incrementAndGet());
              }
            }
          }
          synchronized (this) {
            eventsToFire = maxLoad - controlledTask.getAmountOfCurrentlyExecutingEvents();
            while (eventsToFire == 0) {
              try {
                //logger.debug("going to sleep now");
                this.wait(); //TODO: timeout & fail event?           
                eventsToFire = maxLoad - controlledTask.getAmountOfCurrentlyExecutingEvents();
              } catch (InterruptedException e) {
                List<Throwable> list = new ArrayList<Throwable>();
                throw new XynaRuntimeException("Thread of LoadRateEventCreation was interrupted", list,
                                               new ArrayList<XynaException>());
              }
            }
          }
        }
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.warn(getClass().getSimpleName() + " had error", t);
    }
  }


  @Override
  public String getEventGenerationInformation() {
    return new StringBuilder().append("Load (").append(this.maxLoad).append(")").toString();
  }


}
