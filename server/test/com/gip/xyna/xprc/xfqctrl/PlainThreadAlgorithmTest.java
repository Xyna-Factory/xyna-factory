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

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmState;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedPlainThreadFromPausableRunnable;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedPausableRunnable.ErrorHandling;


public class PlainThreadAlgorithmTest extends AlgorithmManagementTest {
  
  
  @Test
  public void testErrorHandlingBehaviour() {
    ErrorHandlingInjectionRunnable ehir = new ErrorHandlingInjectionRunnable();
    ehir.setErrorHandling(ErrorHandling.CONTINUE);
    final String threadName = ehir.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(ehir));
    assertEquals("Runnable should have never been executed", 0, ehir.getCount());
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    assertOngoingExecution(ehir);
    ehir.injectError();
    sleepLongEnoughForECRExecution();
    assertNoExecution(ehir);
    Collection<ManagedAlgorithmInfo> threads = threadMgmt.listManagedAlgorithms();
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, thread.getName());
      assertEquals("Thread should have been RUNNING after throwing the injected exception.", AlgorithmState.RUNNING, ehir.getStatus());
      assertTrue("There should have been a terminating exception", ehir.getTerminatingException().isPresent());
    }
    ehir.setErrorHandling(ErrorHandling.BACKOFF);
    sleepLongEnoughForECRExecution();
    assertNoExecution(ehir);
    threads = threadMgmt.listManagedAlgorithms();
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, thread.getName());
      assertTrue("Thread should have been RUNNING or WAITING after throwing the injected exception.", ehir.getStatus() == AlgorithmState.RUNNING || ehir.getStatus() == AlgorithmState.WAITING);
      assertTrue("There should have been a terminating exception", ehir.getTerminatingException().isPresent());
    }
    ehir.setErrorHandling(ErrorHandling.ABORT);
    sleepLongEnoughForECRExecution();
    assertNoExecution(ehir);
    threads = threadMgmt.listManagedAlgorithms();
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, thread.getName());
      assertEquals("Thread should have been NOT_RUNNING after throwing the injected exception.", AlgorithmState.NOT_RUNNING, ehir.getStatus());
      assertTrue("There should have been a terminating exception", ehir.getTerminatingException().isPresent());
      assertEquals("The injected Exception should have been the termination reason", ErrorInjectionAlgorithm.THROWABLE_MSG, ehir.getTerminatingException().get().getMessage());
    }
    // stop
    assertEquals("Thread should already count as stopped from exception termination", AlgorithmStateChangeResult.ALREADY_IN_STATE, threadMgmt.stopAlgorithm(threadName));
  }


  
  public ExecutionCountingAlgorithm getExecutionCountingAlgorithm() {
    return new ExecutionCounterRunnable();
  }

  
  private static class ExecutionCounterRunnable extends ManagedPlainThreadFromPausableRunnable implements ExecutionCountingAlgorithm {
    
    protected final AtomicInteger count;
    
    
    protected ExecutionCounterRunnable(String name, List<StringParameter<?>> parameter) {
      super(name, parameter);
      count = new AtomicInteger(0);
    }
    
    protected ExecutionCounterRunnable(String name) {
      this(name, List.of());
    }
    
    public ExecutionCounterRunnable() {
      this(ECR_NAME);
    }

    
    public int getCount() {
      return count.get();
    }


    protected void runOnce() {
      count.incrementAndGet();
      try {
        Thread.sleep(ECR_EXECUTION_DURATION);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    
    @Override
    public void run() {
      count.set(0);
      super.run();
    }

    @Override
    protected ErrorHandling handle(Throwable t) {
      return ErrorHandling.ABORT;
    }
  }

  
  public ErrorInjectionAlgorithm getErrorInjectionAlgorithm() {
    return new ErrorInjectionRunnable();
  }

  private static class ErrorInjectionRunnable extends ExecutionCounterRunnable implements ErrorInjectionAlgorithm {
    
    private volatile boolean throwError;
    
    public ErrorInjectionRunnable() {
      super(EIR_NAME);
    }
    
  
    protected void runOnce() {
      if (throwError) {
        throw new IllegalStateException(ErrorInjectionAlgorithm.THROWABLE_MSG);
      }
      super.runOnce();
    }


    public void injectError() {
      throwError = true;
    }
    
  }
  
  
  private static class ErrorHandlingInjectionRunnable extends ErrorInjectionRunnable {
    
    private volatile ErrorHandling handling = ErrorHandling.ABORT;
    
    public ErrorHandlingInjectionRunnable() {
      super();
    }

    public void setErrorHandling(ErrorHandling handling) {
      this.handling = handling;
    }
    
    protected ErrorHandling handle(Throwable t) {
      return handling;
    }
    
  }
  
  
  public StartParameterAlgorithm getStartParameterAlgorithm() {
    return new StartParameterRunnable();
  }
  
  private static class StartParameterRunnable extends ExecutionCounterRunnable implements StartParameterAlgorithm {
    
    private volatile boolean initializedAtLeastOnce;
    
    public StartParameterRunnable() {
      super(SPR_NAME, List.of(INTERVAL, MANDATORY_BOOL));
    }
    
    @Override
    public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
      initializedAtLeastOnce = true;
      return super.start(parameter, statusOutputStream);
    }
    
    public boolean isInitializedAtLeastOnce() {
      return initializedAtLeastOnce;
    }
    
    public boolean isMandatoryBool() {
      return MANDATORY_BOOL.getFromMap(parameterValues);
    }
    
    public int getInterval() {
      return INTERVAL.getFromMap(parameterValues);
    }
    
    @Override
    protected void runOnce() {
      count.getAndAdd(getInterval());
      try {
        Thread.sleep(ECR_EXECUTION_DURATION);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }


}
