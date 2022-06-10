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

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedLazyAlgorithmExecutionWrapper;
import com.gip.xyna.xprc.xsched.Algorithm;

public class LazyAlgorithmExecutorTest extends AlgorithmManagementTest {
  
  
  @Override
  public ExecutionCountingAlgorithm getExecutionCountingAlgorithm() {
    return new ExecutionCountingExecutor();
  }
  
  private class ExecutionCountingExecutor extends ManagedLazyAlgorithmExecutionWrapper<ExecutionCountingAlgorithmImpl> implements ExecutionCountingAlgorithm {

    public ExecutionCountingExecutor() {
      super(ECR_NAME, new ExecutionCountingAlgorithmImpl(), Collections.emptyList(), Optional.empty(), ECR_EXECUTION_DURATION);
    }

    @Override
    public int getCount() {
      return this.getAlgorithm().getCount();
    }
    
    @Override
    public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
      if (super.start(parameter, statusOutputStream)) {
        this.getAlgorithm().counter.set(0);
        return true;
      } else {
        return false;
      }
    }

  }
  
  
  private static class ExecutionCountingAlgorithmImpl implements Algorithm  {

    protected AtomicInteger counter = new AtomicInteger();

    public int getCount() {
      return counter.get();
    }

    public void exec() {
      counter.incrementAndGet();
    }
    
  }


  @Override
  public ErrorInjectionAlgorithm getErrorInjectionAlgorithm() {
    return new ErrorInjectionExecutor();
  }
  
  
  private class ErrorInjectionExecutor extends ManagedLazyAlgorithmExecutionWrapper<ErrorInjectionAlgorithmImpl> implements ErrorInjectionAlgorithm {
    
    public ErrorInjectionExecutor() {
      super(EIR_NAME, new ErrorInjectionAlgorithmImpl(), Collections.emptyList(), Optional.empty(), ECR_EXECUTION_DURATION);
    }

    @Override
    public int getCount() {
      return getAlgorithm().getCount();
    }

    @Override
    public void injectError() {
      getAlgorithm().injectError();
    }

  }
  
  
  private static class ErrorInjectionAlgorithmImpl extends ExecutionCountingAlgorithmImpl {

    private boolean throwError = false;

    public void exec() {
      if (throwError) {
        throw new IllegalStateException(ErrorInjectionAlgorithm.THROWABLE_MSG);
      }
      super.exec();
    }


    public void injectError() {
      throwError = true;
    }
    
  }


  @Override
  public StartParameterAlgorithm getStartParameterAlgorithm() {
    return new StartParameterExecutor();
  }
  
  private class StartParameterExecutor extends ManagedLazyAlgorithmExecutionWrapper<StartParameterAlgorithmImpl> implements StartParameterAlgorithm {
    
    private boolean initializedAtLeastOnce = false;
    
    public StartParameterExecutor() {
      super(SPR_NAME, new StartParameterAlgorithmImpl(0), List.of(INTERVAL, MANDATORY_BOOL), Optional.empty());
    }

    @Override
    public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
      initializedAtLeastOnce = true;
      this.algorithm = new StartParameterAlgorithmImpl(INTERVAL.getFromMap(parameter));
      return super.start(parameter, statusOutputStream);
    }
    
    @Override
    public int getCount() {
      return getAlgorithm().getCount();
    }


    @Override
    public boolean isMandatoryBool() {
      return MANDATORY_BOOL.getFromMap(parameter);
    }

    @Override
    public int getInterval() {
      return INTERVAL.getFromMap(parameter);
    }

    @Override
    public boolean isInitializedAtLeastOnce() {
      return initializedAtLeastOnce;
    }
  }
  
  
  private static class StartParameterAlgorithmImpl extends ExecutionCountingAlgorithmImpl {
    
    private final int increment;
    
    StartParameterAlgorithmImpl(int increment) {
      this.increment = increment;
    }
    
    @Override
    public void exec() {
      counter.getAndAdd(increment);
      try {
        Thread.sleep(ExecutionCountingAlgorithm.ECR_EXECUTION_DURATION);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  
}
