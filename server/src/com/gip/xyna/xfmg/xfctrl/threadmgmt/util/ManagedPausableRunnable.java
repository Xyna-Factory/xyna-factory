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
package com.gip.xyna.xfmg.xfctrl.threadmgmt.util;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithm;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;

import org.apache.log4j.Logger;

public abstract class ManagedPausableRunnable extends PausableRunnable implements ManagedAlgorithm {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(ManagedPausableRunnable.class);
  
  protected final List<StringParameter<?>> parameter;
  
  protected volatile Optional<Throwable> terminatingException;
  protected volatile Map<String, Object> parameterValues;
  protected volatile long startTime;
  protected volatile long stopTime; 
  protected volatile long lastExecution;
  
  
  
  protected ManagedPausableRunnable() {
    this(List.of());
  }
  
  protected ManagedPausableRunnable(List<StringParameter<?>> parameter) {
    terminatingException = Optional.empty();
    this.parameter = parameter;
  }
  
  public void run() {
    SleepCounter errorBackoff = new SleepCounter(25, 1000, 4);
    runToggle = true;
    while (runToggle) {
      try {
        runOnce();
        lastExecution = System.currentTimeMillis();
        terminatingException = Optional.empty();
        errorBackoff.reset();
      } catch (Throwable t) {
        terminatingException = Optional.of(t);
        Department.handleThrowable(t);
        ErrorHandling handling = handle(t);
        switch (handling) {
          case CONTINUE :
            continue;
          case BACKOFF :
            try {
              errorBackoff.sleep();
            } catch (InterruptedException e) {
              logger.debug("Interrupted during error backoff",e);
            }
            break;
          case ABORT :
          default :
            logger.error("Unexpected error in " + getName() + ", stopping thread.", t);
            return;
        }
      }
    }
  }
  
  @Override
  public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
    this.parameterValues = parameter;
    init();
    if (super.start()) {
      terminatingException = Optional.empty();
      startTime = System.currentTimeMillis();
      stopTime = 0;
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public boolean stop() {
    boolean result = super.stop();
    if (result) {
      stopTime = System.currentTimeMillis();
    }
    return result;
  }
  
  public Optional<Throwable> getTerminatingException() {
    return terminatingException;
  }
  
  public List<StringParameter<?>> getStartParameterInformation() {
    return parameter;
  }
  
  public Map<String, Object> getStartParameter() {
    return parameterValues;
  }
  
  protected void init() {
  }

  protected abstract ErrorHandling handle(Throwable t);
  
  public static enum ErrorHandling {
    ABORT, CONTINUE, BACKOFF;
  }
  
  @Override
  public ManagedAlgorithmInfo getInfo() {
    return new ManagedAlgorithmInfo(getName(),
                                    getStatus(),
                                    parameter,
                                    parameterValues,
                                    terminatingException,
                                    startTime,
                                    stopTime,
                                    lastExecution);
  }

}
