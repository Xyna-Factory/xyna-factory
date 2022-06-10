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
package com.gip.xyna.xfmg.xfctrl.threadmgmt.util;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmState;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithm;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;


public abstract class ManagedLazyAlgorithmExecutionWrapper<A extends Algorithm> implements ManagedAlgorithm {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(ManagedLazyAlgorithmExecutionWrapper.class);
  private final static long TERMINATION_TIMEOUT = 5000;
  
  private final String name;
  
  protected final LazyAlgorithmExecutor<AlgorithmExecutionWrapper<A>> executor;
  protected final List<StringParameter<?>> parameterInformation;
  protected final Optional<Runnable> initialization;

  protected volatile long startTime;
  protected volatile long stopTime;
  protected volatile long lastExecution;
  protected volatile A algorithm;
  protected volatile Map<String, Object> parameter;
  
  
  public ManagedLazyAlgorithmExecutionWrapper(String name, A algorithm) {
    this(name, algorithm, Collections.emptyList(), Optional.empty());
  }
  
  public ManagedLazyAlgorithmExecutionWrapper(String name, A algorithm, List<StringParameter<?>> parameterInformation, Optional<Runnable> initialization) {
    this(name, algorithm, parameterInformation, initialization, 0);
  }
  
  public ManagedLazyAlgorithmExecutionWrapper(String name, A algorithm, List<StringParameter<?>> parameterInformation, Optional<Runnable> initialization, int periodicWakeupInterval) {
    this.name = name;
    this.executor = new LazyAlgorithmExecutor<>(name, periodicWakeupInterval);
    this.algorithm = algorithm;
    this.parameterInformation = parameterInformation;
    this.initialization = initialization;
  }

  
  public String getName() {
    return name;
  }


  public AlgorithmState getStatus() {
    if (!executor.isRunning() ||
        executor.isPaused()) {
      return AlgorithmState.NOT_RUNNING;
    }
    if (executor.threadIsAsleep()) {
      return AlgorithmState.WAITING;  
    }
    return AlgorithmState.RUNNING;
  }


  public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
    startTime = System.currentTimeMillis();
    stopTime = 0;
    this.parameter = parameter;
    if (executor.isRunning()) {
      executor.unPauseExecution();
    } else {
      if (initialization.isPresent()) {
        executor.startNewThread(initialization.get(), new AlgorithmExecutionWrapper<A>(algorithm));
      } else {
        executor.startNewThread(new AlgorithmExecutionWrapper<A>(algorithm));
      }
    }
    return true;
  }


  public boolean stop() {
    stopTime = System.currentTimeMillis();
    // pause would only suppress notification requests...
    //executor.pauseExecution();
    executor.stopThread();
    try {
      executor.awaitTermination(TERMINATION_TIMEOUT);
    } catch (InterruptedException e) {
      logger.debug("Interrupted while waiting for thread termination",e);
      return false;
    }
    return true;
  }


  public Optional<Throwable> getTerminatingException() {
    return Optional.ofNullable(executor.getThreadDeathCause());
  }


  public List<StringParameter<?>> getStartParameterInformation() {
    return parameterInformation;
  }


  public Map<String, Object> getStartParameter() {
    return parameter;
  }
  
  protected A getAlgorithm() {
    return algorithm;
  }
  
  public ManagedAlgorithmInfo getInfo() {
    return new ManagedAlgorithmInfo(name,
                                    getStatus(),
                                    parameterInformation,
                                    parameter,
                                    getTerminatingException(),
                                    startTime,
                                    stopTime,
                                    lastExecution);
  }
  
  
  private final class AlgorithmExecutionWrapper<B extends Algorithm> implements Algorithm {
    
    private final Algorithm innerAlgorithm;
    
    public AlgorithmExecutionWrapper(B innerAlgorithm) {
      this.innerAlgorithm = innerAlgorithm;
    }

    public void exec() {
      innerAlgorithm.exec();
      lastExecution = System.currentTimeMillis();
    }
    
  }
  
}
