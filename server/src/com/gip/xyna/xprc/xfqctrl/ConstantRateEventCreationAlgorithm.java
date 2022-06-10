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
package com.gip.xyna.xprc.xfqctrl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCreationParameters;


public class ConstantRateEventCreationAlgorithm extends FrequenceControlledTaskEventAlgorithm {

  public static final Logger logger = CentralFactoryLogging.getLogger(ConstantRateEventCreationAlgorithm.class);
  
  private final double targetRate; //in 1/ms
  private long startTime;
  private long pauseTime = 0;
  private long currentPauseBegin;

  private final AtomicLong eventIdGenerator = new AtomicLong(0);
  
  public static enum HighRateThreadSleepType {
    NORMALSLEEP, DIVISIBLE10, DIVISIBLE15, DIVISIBLE30, ACTIVEWAIT;
  }

  private HighRateThreadSleepType highRateThreadSleepType;
  private long threadSleepMinValue;
  
  ConstantRateEventCreationAlgorithm(FrequencyControlledTaskCreationParameter creationParams, RateControlledCreationParameter algorithmParams) throws XPRC_InvalidCreationParameters {
    super(creationParams);
    double rateFromParams = algorithmParams.getRate();
    if (rateFromParams <= 0) {
      throw new XPRC_InvalidCreationParameters("Creation of FrequencyControlledTask with load parameter <= 0 detected");
    }
    targetRate = rateFromParams/1000; //auf ms umrechnen
    
    //highRateThreadSleepType aus property lesen
    highRateThreadSleepType = XynaProperty.FQCTRL_HIGH_RATE_THREAD_SLEEP_TYPE.get();
    //threadSleepMinValue aus property lesen
    threadSleepMinValue = XynaProperty.FQCTRL_HIGH_RATE_THREAD_SLEEP_MINVALUE.getMillis();
  }
  
  //ACHTUNG: rundungsfehler in den nächsten beiden methoden können dazu führen, dass
  //die sleepduration zu kurz berechnet wird, und dann gibt es hohe cpu-last durch mehrfaches durchlaufen
  //der while-schleife, ohne dass geschlafen wird und ohne dass neue aufträge gestartet werden können.
  
  //sleepduration ist nicht unbedingt konstant, weil das triggern der events auch zeit kostet und das je nach belastung des systems unterschiedlich lang
  private long calculateSleepDurationForRate() {
    //berechne zeit, wann nächster event gestartet werden sein muss
    //bei 0 sekunden wird der erste event gestartet. der 2. bei 0+1/rate. der 3. bei 0+2/rate etc.
    long nextStartTime = Math.round(startTime + getControlledTask().getEventCount() / targetRate + 0.5); //aufrunden, weil ansonsten evtl keiner gestartet wird 
    
    return nextStartTime - System.currentTimeMillis();
  }
  
  
  private long calculateEventsToFireToAchieveTargetRate() {
    long runTimeInMillis = System.currentTimeMillis() - startTime + pauseTime;
    long estimatedEvents = Math.round(targetRate * runTimeInMillis + 0.5); //aufrunden, weil bei 0 sekunden wird der erste gestartet, bei 1/rate millisekunden der 2te
    if (estimatedEvents < 0) {
      return 1;
    }
    return estimatedEvents - getControlledTask().getEventCount();
  }
  

  @Override
  public synchronized void eventFinished() {
    //we have our constant rate, notifications don't concern us
  }


  public void run() {
    try {
      // Es wird ein Thread verwendet der nach Ablauf einer ausgerechneten Wartezeit mehrere Events auslöst. Zielrate legt dabei Wartezeit & Anzahl fest
      startTime = System.currentTimeMillis();
      //logger.debug("Starting RateEventGeneration @"+startTime);
      while (executionIsPermitted()) {
        long eventsToFire;
        long maxAllowedEventsForTask;
        synchronized (this) {
          eventsToFire = calculateEventsToFireToAchieveTargetRate();
          maxAllowedEventsForTask = eventsToLaunch - getControlledTask().getEventCount();
        }
        if (eventsToFire > 0) {        
          if (logger.isDebugEnabled()) {
            logger.debug("Firing events #" + eventsToFire);
          }
          if (eventsToFire > maxAllowedEventsForTask) {
            eventsToFire = maxAllowedEventsForTask;
            revokeExecutionPermit();
          }
          for (int i = 0; i < eventsToFire; i++) {
            if (executionIsPermitted()) {
              getControlledTask().eventTriggered(eventIdGenerator.incrementAndGet());
            }
          }
        }
        if (executionIsPermitted()) {
          long sleepDuration = calculateSleepDurationForRate();
          if (sleepDuration > 0) {
            sleep(sleepDuration);
          }
        }
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error(getClass().getSimpleName() + " had error", t);
    }
  }
  
  private void sleep(long sleepDuration) {
    //schlafen bis das nächste task gestartet werden kann. schlafen ist bei hohen raten u.u. problematisch. deshalb kann
    //man das verhalten konfigurieren.
    //TODO für noch höhere genauigkeit könnte man die folgende fallunterscheidung einmalig im konstruktor vornehmen und hier nur
    //eine methode eines interfaces aufrufen, welches je nach fallunterscheidung anders gesetzt ist (ähnlich wie im scheduler).
    try {
      if (sleepDuration <= threadSleepMinValue) {
        //=> spezialbehandlung für sehr kurze sleeps!
        
        if (highRateThreadSleepType == HighRateThreadSleepType.NORMALSLEEP) {
          Thread.sleep(sleepDuration);
        } else if (highRateThreadSleepType == HighRateThreadSleepType.ACTIVEWAIT) {
          long endTime = System.currentTimeMillis() + sleepDuration;
          while (System.currentTimeMillis() <= endTime) {
            
          }
        } else {
          //sleepduration auf vielfaches von sleeptype-zahl aufrunden.
          /*
           * on windows:
           * 1. The VM makes a special call to set the interrupt period to 1ms while any Java thread is sleeping,
           *    if it requests a sleep interval that is not a multiple of 10ms.
           * 2. Hotspot assumes that the default interrupt period is 10ms, but on some hardware it is 15ms.
           * ==> To account for both sleep durations will be set to multiples of 30 
           * 3. bugs die dazu führen, dass die systemzeit ungenau wird
           */
          int m = 10;
          if (highRateThreadSleepType == HighRateThreadSleepType.DIVISIBLE15) {
            m = 15;
          } else if (highRateThreadSleepType == HighRateThreadSleepType.DIVISIBLE30) {
            m = 30;
          }
          long sleepRest = sleepDuration % m;
          if (sleepRest == 0) {
            sleepRest = m;
          }
          sleepDuration += m - sleepRest;
          Thread.sleep(sleepDuration);
        }
      } else {
        // => normale sleeplänge benötigt keine sonderbehandlung
        Thread.sleep(sleepDuration);
      }
    } catch (InterruptedException e) {
      List<Throwable> list = new ArrayList<Throwable>();
      throw new XynaRuntimeException("Thread of ConstantRateEventCreation was interrupted", list,
                                     new ArrayList<XynaException>());
    }
  }


  @Override
  public String getEventGenerationInformation() {
    return new StringBuilder().append("Rate (").append(targetRate * 1000).append("Hz)").toString();
  }

  
  @Override
  synchronized void revokeExecutionPermit() {
    if (currentPauseBegin == 0) {
      currentPauseBegin = System.currentTimeMillis();
    }
    super.revokeExecutionPermit();
  }
  
  
  @Override
  synchronized void grantExecutionPermit() {
    if (currentPauseBegin != 0) {
      pauseTime += System.currentTimeMillis() - currentPauseBegin;
      currentPauseBegin = 0;
    }
    super.grantExecutionPermit();
  }
}
