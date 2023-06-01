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

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithm;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.util.ManagedPlainThreadFromPausableRunnable;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStartParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmStateChangeResult;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmState;

import junit.framework.TestCase;

public abstract class AlgorithmManagementTest extends TestCase {
  
  protected InfrastructureAlgorithmExecutionManagement threadMgmt;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    threadMgmt = new InfrastructureAlgorithmExecutionManagement();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    threadMgmt = null;
  }
  
  
  private static final int ECR_EXECUTION_DURATION_SAFTY_MULTIPLIER = 3;
  
  @Test
  public void testSimpleLifecycle() {
    ExecutionCountingAlgorithm ecr = getExecutionCountingAlgorithm();
    final String threadName = ecr.getName();
    assertEquals("Thread should not have been registered", AlgorithmStateChangeResult.NOT_REGISTERED, threadMgmt.startAlgorithm(threadName));
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(ecr));
    Collection<ManagedAlgorithmInfo> threads = threadMgmt.listManagedAlgorithms();
    assertEquals("ThreadMgmt should list our previously registered Thread.", 1, threads.size());
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, thread.getName());
      assertEquals("Thread should be NOT_RUNNING as it has never been started.", AlgorithmState.NOT_RUNNING, ecr.getStatus());
    }
    assertEquals("Runnable should have never been executed", 0, ecr.getCount());
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    assertOngoingExecution(ecr);
    // stop
    assertEquals("Thread should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(threadName));
    assertNoExecution(ecr);
    // stop
    assertEquals("Thread should have already been stopped", AlgorithmStateChangeResult.ALREADY_IN_STATE, threadMgmt.stopAlgorithm(threadName));
  }
  
  
  @Test
  public void testStartStopAndRestart() {
    ExecutionCountingAlgorithm ecr = getExecutionCountingAlgorithm();
    final String threadName = ecr.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(ecr));
    assertEquals("Runnable should have never been executed", 0, ecr.getCount());
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    assertOngoingExecution(ecr);
    // stop
    assertEquals("Thread should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(threadName));
    assertNoExecution(ecr);
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    assertOngoingExecution(ecr);
    // stop
    assertEquals("Thread should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(threadName));
  }
  
  
  @Test
  public void testErrorBehaviour() {
    ErrorInjectionAlgorithm eir = getErrorInjectionAlgorithm();
    final String threadName = eir.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(eir));
    assertEquals("Runnable should have never been executed", 0, eir.getCount());
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    assertOngoingExecution(eir);
    eir.injectError();
    sleepLongEnoughForECRExecution();
    assertNoExecution(eir);
    Collection<ManagedAlgorithmInfo> threads = threadMgmt.listManagedAlgorithms();
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, thread.getName());
      assertEquals("Thread should have been NOT_RUNNING after throwing the injected exception.", AlgorithmState.NOT_RUNNING, eir.getStatus());
      assertTrue("There should have been a terminating exception", eir.getInfo().getTerminationException().isPresent());
      assertEquals("The injected Exception should have been the termination reason", ErrorInjectionAlgorithm.THROWABLE_MSG, eir.getInfo().getTerminationException().get().getMessage());
    }
    // stop
    assertEquals("Thread should already count as stopped from exception termination", AlgorithmStateChangeResult.ALREADY_IN_STATE, threadMgmt.stopAlgorithm(threadName));
  }
  
  
  @Test
  public void testDoubleRegistration() {
    ExecutionCountingAlgorithm ecr1 = getExecutionCountingAlgorithm();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(ecr1));
    ExecutionCountingAlgorithm ecr2 = getExecutionCountingAlgorithm();
    assertFalse("Thread registration should not have been succesfull", threadMgmt.registerAlgorithm(ecr2));
  }
  
  
  @Test
  public void testSeveralThreadRegistrations() {
    final long SLEEP_TIME = 50; 
    ManagedAlgorithm mt1 = new ManagedPlainThreadFromPausableRunnable("mt1") {
      protected void runOnce() { sleepAndFailOnInterrupt(SLEEP_TIME); }
    };
    ManagedAlgorithm mt2 = new ManagedPlainThreadFromPausableRunnable("mt2") {
      protected void runOnce() { sleepAndFailOnInterrupt(SLEEP_TIME); }
    };
    ManagedAlgorithm mt3 = new ManagedPlainThreadFromPausableRunnable("mt3") {
      protected void runOnce() { sleepAndFailOnInterrupt(SLEEP_TIME); }
    };
    ManagedAlgorithm mt4 = new ManagedPlainThreadFromPausableRunnable("mt4") {
      protected void runOnce() { sleepAndFailOnInterrupt(SLEEP_TIME); }
    };
    ManagedAlgorithm mt5 = new ManagedPlainThreadFromPausableRunnable("mt5") {
      protected void runOnce() { sleepAndFailOnInterrupt(SLEEP_TIME); }
    };
    
    assertTrue("Thread registration of mt1 should have been succesfull", threadMgmt.registerAlgorithm(mt1));
    assertTrue("Thread registration of mt2 should have been succesfull", threadMgmt.registerAlgorithm(mt2));
    assertTrue("Thread registration of mt3 should have been succesfull", threadMgmt.registerAlgorithm(mt3));
    assertTrue("Thread registration of mt4 should have been succesfull", threadMgmt.registerAlgorithm(mt4));
    assertTrue("Thread registration of mt5 should have been succesfull", threadMgmt.registerAlgorithm(mt5));
    
    assertEquals("Thread of mt1 should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(mt1.getName()));
    assertEquals("Thread of mt3 should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(mt3.getName()));
    assertEquals("Thread of mt5 should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(mt5.getName()));
    
    sleepAndFailOnInterrupt(SLEEP_TIME * 5);
    
    Collection<ManagedAlgorithmInfo> threads = threadMgmt.listManagedAlgorithms();
    assertEquals("ThreadMgmt should list our previously registered Threads.", 5, threads.size());
    for (ManagedAlgorithmInfo thread : threads) {
      switch (thread.getName()) {
        case "mt1" :
        case "mt3" :
        case "mt5" :
          //((ManagedPlainThreadFromPausableRunnable)thread).printStackTrace(System.err);
          assertTrue("Thread " + thread.getName() + " should have been started.", thread.getStatus() != AlgorithmState.NOT_RUNNING);
          break;
        default :
          assertEquals("Thread " + thread.getName() + " should not be running.", AlgorithmState.NOT_RUNNING, thread.getStatus());
          break;
      }
    }
    
    assertEquals("Thread of mt3 should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(mt3.getName()));
    
    sleepAndFailOnInterrupt(SLEEP_TIME * 5);
    
    threads = threadMgmt.listManagedAlgorithms();
    assertEquals("ThreadMgmt should list our previously registered Threads.", 5, threads.size());
    for (ManagedAlgorithmInfo thread : threads) {
      switch (thread.getName()) {
        case "mt1" :
        case "mt5" :
          //((ManagedPlainThreadFromPausableRunnable)thread).printStackTrace(System.err);
          assertTrue("Thread " + thread.getName() + " should have been started.", thread.getStatus() != AlgorithmState.NOT_RUNNING);
          break;
        default :
          assertEquals("Thread " + thread.getName() + " should not be running.", AlgorithmState.NOT_RUNNING, thread.getStatus());
          break;
      }
    }
    
    assertEquals("Thread of mt1 should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(mt1.getName()));
    assertEquals("Thread of mt5 should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(mt5.getName()));
    
    sleepAndFailOnInterrupt(SLEEP_TIME * 5);
    
    threads = threadMgmt.listManagedAlgorithms();
    assertEquals("ThreadMgmt should list our previously registered Threads.", 5, threads.size());
    for (ManagedAlgorithmInfo thread : threads) {
      assertEquals("Thread " + thread.getName() + " should not be running.", AlgorithmState.NOT_RUNNING, thread.getStatus());
    }
  }
  
  
  @Test
  public void testThreadRestart() {
    ExecutionCountingAlgorithm ecr = getExecutionCountingAlgorithm();
    final String threadName = ecr.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(ecr));
    assertEquals("Runnable should have never been executed", 0, ecr.getCount());
    // start
    assertEquals("Thread should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName));
    int initialExecutionCount = assertOngoingExecution(ecr);
    assertEquals("Thread should have failed to be restarted", AlgorithmStateChangeResult.ALREADY_IN_STATE, threadMgmt.startAlgorithm(threadName));
    int failedRestartExecutionCount = assertOngoingExecution(ecr);
    assertTrue("Restart should have failed and execution should have continued.", initialExecutionCount < failedRestartExecutionCount);
    try {
      assertEquals("Thread should have been restarted", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(threadName, new AlgorithmStartParameter(true)));
      int successfullRestartExecutionCount = assertOngoingExecution(ecr);
      assertTrue("Restart should have succedded and counter should have been reset.\nCurrent: " +successfullRestartExecutionCount+"\nBefore restart: " + failedRestartExecutionCount,
                 successfullRestartExecutionCount < failedRestartExecutionCount);
    } catch (StringParameterParsingException e) {
      fail("ExecutionCounterRunnable does not parse parameters, no reason to fail");
    }
    assertEquals("Thread should have been stopped", AlgorithmStateChangeResult.SUCCESS, threadMgmt.stopAlgorithm(threadName));
  }
  
  
  @Test
  public void testStartParameterListing() {
    StartParameterAlgorithm spr = getStartParameterAlgorithm();
    final String threadName = spr.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(spr));
    
    Collection<ManagedAlgorithmInfo> algos = threadMgmt.listManagedAlgorithms();
    assertEquals("ThreadMgmt should list our previously registered Thread.", 1, algos.size());
    for (ManagedAlgorithmInfo algo : algos) {
      assertEquals("ThreadMgmt should list our previously registered Thread.", threadName, algo.getName());
      assertEquals("Thread should be NOT_RUNNING as it has never been started.", AlgorithmState.NOT_RUNNING, algo.getStatus());
      List<StringParameter<?>> potentialStartParameters =  algo.getAdditionalParameters();
      assertEquals("Potential StartParams should have been listed.", 2, potentialStartParameters.size());
      boolean foundMandatoryParameterInfo = false;
      boolean foundIntervalParameterInfo = false;
      for (StringParameter<?> potentialStartParameter : potentialStartParameters) {
        if (potentialStartParameter.getName().equals(StartParameterAlgorithm.MANDATORY_BOOL.getName())) {
          foundMandatoryParameterInfo = true;
          assertTrue(StartParameterAlgorithm.MANDATORY_BOOL.getName() + " should have been marked as mandatory.", potentialStartParameter.isMandatory());
          assertEquals("Documentation of " + StartParameterAlgorithm.MANDATORY_BOOL.getName() + " should have been preserved.",
                       StartParameterAlgorithm.MANDATORY_BOOL.documentation(DocumentationLanguage.DE),
                       potentialStartParameter.documentation(DocumentationLanguage.DE));
          assertEquals("Documentation of " + StartParameterAlgorithm.MANDATORY_BOOL.getName() + " should have been preserved.",
                       StartParameterAlgorithm.MANDATORY_BOOL.documentation(DocumentationLanguage.EN),
                       potentialStartParameter.documentation(DocumentationLanguage.EN));
          
        }
        if (potentialStartParameter.getName().equals(StartParameterAlgorithm.INTERVAL.getName())) {
          foundIntervalParameterInfo = true;
          assertEquals("Default of " + StartParameterAlgorithm.INTERVAL.getName() + " should have been 1.", 1, potentialStartParameter.getDefaultValue());
          assertEquals("Documentation of " + StartParameterAlgorithm.INTERVAL.getName() + " should have been preserved.",
                       StartParameterAlgorithm.INTERVAL.documentation(DocumentationLanguage.DE),
                       potentialStartParameter.documentation(DocumentationLanguage.DE));
          assertEquals("Documentation of " + StartParameterAlgorithm.INTERVAL.getName() + " should have been preserved.",
                       StartParameterAlgorithm.INTERVAL.documentation(DocumentationLanguage.EN),
                       potentialStartParameter.documentation(DocumentationLanguage.EN));
        }
      }
      assertTrue(StartParameterAlgorithm.MANDATORY_BOOL.getName() + " should have been contained in parameter listing.", foundMandatoryParameterInfo);
      assertTrue(StartParameterAlgorithm.INTERVAL.getName() + " should have been contained in parameter listing.", foundIntervalParameterInfo);
    }
  }
  
  @Test
  public void testInvalidStartParams() {
    final String KEY_VALUE_SEPERATOR = "=";
    StartParameterAlgorithm spr = getStartParameterAlgorithm();
    final String threadName = spr.getName();
    assertTrue("Thread registration should have been succesfull", threadMgmt.registerAlgorithm(spr));

    // start
    try {
      threadMgmt.startAlgorithm(threadName);
      fail("An exception would have been expected.");
    } catch (RuntimeException e) {
      StringParameterParsingException sppe = assertExceptionClassInTrace(e, StringParameterParsingException.class);
      assertTrue("We should have received an exception for a missing mandatory value.",
                 sppe.getMessage().contains(StringParameterParsingException.Reason.Mandatory.getConstantMessagePart()));
    }
    
    AlgorithmStartParameter tsp_invalidBoolean = new AlgorithmStartParameter(false, 
                                                                       List.of(StartParameterAlgorithm.MANDATORY_BOOL.getName() +
                                                                               KEY_VALUE_SEPERATOR +
                                                                               "affe"));
    // start
    try {
      threadMgmt.startAlgorithm(threadName, tsp_invalidBoolean);
      fail("An exception would have been expected.");
    } catch (StringParameterParsingException e) {
      assertTrue("We should have received an exception for an unparsable value.",
                 e.getMessage().contains(StringParameterParsingException.Reason.Parsing.getConstantMessagePart()));
    }
    
    AlgorithmStartParameter tsp_invalidInteger = new AlgorithmStartParameter(false, 
                                                                       List.of(StartParameterAlgorithm.MANDATORY_BOOL.getName() +
                                                                               KEY_VALUE_SEPERATOR +
                                                                               "true",
                                                                               StartParameterAlgorithm.INTERVAL.getName() +
                                                                               KEY_VALUE_SEPERATOR +
                                                                               "affe"));
    // start
    try {
      threadMgmt.startAlgorithm(threadName, tsp_invalidInteger);
      fail("An exception would have been expected.");
    } catch (StringParameterParsingException e) {
      assertTrue("We should have received an exception for an unparsable value.",
                 e.getMessage().contains(StringParameterParsingException.Reason.Parsing.getConstantMessagePart()));
    }
    
    assertEquals("Runnable should have never been executed", 0, spr.getCount());
    assertEquals("Runnable should have never been initialized", false,  spr.isInitializedAtLeastOnce()); 
    
    AlgorithmStartParameter tsp_validBooleanLowInterval = new AlgorithmStartParameter(false, 
                                                                                List.of(StartParameterAlgorithm.MANDATORY_BOOL.getName() +
                                                                                        KEY_VALUE_SEPERATOR +
                                                                                        "true",
                                                                                        StartParameterAlgorithm.INTERVAL.getName() +
                                                                                        KEY_VALUE_SEPERATOR +
                                                                                        "1"));
    // start
    try {
      assertEquals("Thread of spr should have been started", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(spr.getName(), tsp_validBooleanLowInterval));
    } catch (StringParameterParsingException e) {
      fail("No StringParameterParsingException expected: " + e.getMessage());
    }
    assertOngoingExecution(spr);
    assertEquals("Runnable should now have been intialized", true,  spr.isInitializedAtLeastOnce());
    assertEquals("MandatoryBool should have been true", true,  spr.isMandatoryBool());
    assertEquals("Interval should have been set to 1", 1,  spr.getInterval());
    
    AlgorithmStartParameter tsp_validBooleanHighInterval = new AlgorithmStartParameter(false, 
                                                                                 List.of(StartParameterAlgorithm.MANDATORY_BOOL.getName() +
                                                                                         KEY_VALUE_SEPERATOR +
                                                                                         "false",
                                                                                         StartParameterAlgorithm.INTERVAL.getName() +
                                                                                         KEY_VALUE_SEPERATOR +
                                                                                         "1777"));
    // start
    try {
      assertEquals("Thread of spr should have already been running", AlgorithmStateChangeResult.ALREADY_IN_STATE, threadMgmt.startAlgorithm(spr.getName(), tsp_validBooleanHighInterval));
    } catch (StringParameterParsingException e) {
      fail("No StringParameterParsingException expected: " + e.getMessage());
    }
    assertEquals("MandatoryBool should still have been true", true,  spr.isMandatoryBool());
    assertEquals("Interval should still have been set to 1", 1,  spr.getInterval());
    
    AlgorithmStartParameter tsp_validBooleanHighIntervalRestartAllowed = new AlgorithmStartParameter(true, 
                                                                                               List.of(StartParameterAlgorithm.MANDATORY_BOOL.getName() +
                                                                                                       KEY_VALUE_SEPERATOR +
                                                                                                       "false",
                                                                                                       StartParameterAlgorithm.INTERVAL.getName() +
                                                                                                       KEY_VALUE_SEPERATOR +
                                                                                                       "1777"));
    // start
    try {
      assertEquals("Thread of spr should have been successfully restarted", AlgorithmStateChangeResult.SUCCESS, threadMgmt.startAlgorithm(spr.getName(), tsp_validBooleanHighIntervalRestartAllowed));
    } catch (StringParameterParsingException e) {
      fail("No StringParameterParsingException expected: " + e.getMessage());
    }
    assertOngoingExecution(spr);
    assertEquals("MandatoryBool should have been reset to false", false,  spr.isMandatoryBool());
    assertEquals("Interval should have been reset 1777", 1777,  spr.getInterval());
    assertTrue("Runnable counter should have been reset to zero and incremented by 1777\nWas "+spr.getCount()+" instead", (spr.getCount() % 1777) == 0);
  }
  
  
  protected static int assertOngoingExecution(ExecutionCountingAlgorithm ecr) {
    return assertExecutionCount(ecr, (o, c) -> o < c);
  }
  
  protected static int assertNoExecution(ExecutionCountingAlgorithm ecr) {
    return assertExecutionCount(ecr, (o, c) -> o == c);
  }
  
  protected static int assertExecutionCount(ExecutionCountingAlgorithm ecr, ExecutionCounterPredicate predicate) {
    int currentCount = ecr.getCount();
    sleepLongEnoughForECRExecution();
    int nextCount = ecr.getCount();
    assertTrue("Execution count did not match up to predicate.\nOld: " + currentCount + "\nCurrent: " + nextCount, predicate.compare(currentCount, nextCount));
    return nextCount;
  }
  
  protected static void sleepLongEnoughForECRExecution() {
    sleepAndFailOnInterrupt(ExecutionCountingAlgorithm.ECR_EXECUTION_DURATION * ECR_EXECUTION_DURATION_SAFTY_MULTIPLIER);
  }
  
  private static void sleepAndFailOnInterrupt(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("Interrupted while waiting: " + e.getMessage());
    }
  }
  
  public interface ExecutionCounterPredicate {
    
    boolean compare(int oldCount, int currentCount);
    
  }
  
  protected <T extends Throwable> T assertExceptionClassInTrace(Throwable t, Class<T> clazz) {
    Throwable current = t;
    while (!clazz.isAssignableFrom(current.getClass()) &&
           current.getCause() != null &&
           current.getCause() != current) {
      current = current.getCause();
    }
    if (clazz.isAssignableFrom(current.getClass())) {
      return clazz.cast(current);
    }
    fail(clazz.getName() + " could not be found as cause of " + t);
    return null;
  }
  
  
  public interface ExecutionCountingAlgorithm extends ManagedAlgorithm {
    
    public final static String ECR_NAME = "SecondCounterRunnable";
    public final static int ECR_EXECUTION_DURATION = 50;
    
    public int getCount();
    
  }
  
  public abstract ExecutionCountingAlgorithm getExecutionCountingAlgorithm();
  
  public interface ErrorInjectionAlgorithm extends ExecutionCountingAlgorithm {
    
    public final static String EIR_NAME = "ErrorInjectionRunnable";
    public final static String THROWABLE_MSG = "Aborting with throw as desired";
    
    public void injectError();
    
  }
  
  public abstract ErrorInjectionAlgorithm getErrorInjectionAlgorithm();
  

  public interface StartParameterAlgorithm extends ExecutionCountingAlgorithm {
    
    public final static String SPR_NAME = "StartParameterRunnable";
    public final static StringParameter<Integer> INTERVAL = StringParameter.typeInteger("interval")
                                                                           .defaultValue(1)
                                                                           .documentation(Documentation.de("Interval für die Erhöhung des ExecutionCounters")
                                                                                                       .en("Interval for incrementation of the ExecutionCounter")
                                                                                                       .build())
                                                                           .label("ExecutionCounter interval")
                                                                           .build();
    public final static StringParameter<Boolean> MANDATORY_BOOL = StringParameter.typeBoolean("mandatoryBool")
                                                                                 .mandatory()
                                                                                 .documentation(Documentation.de("Verpflichtender boolscher Wert")
                                                                                                             .en("Mandatory boolean value")
                                                                                                             .build())
                                                                                 .label("Important boolean value")
                                                                                 .build();
    
    public boolean isInitializedAtLeastOnce();
    
    public boolean isMandatoryBool();
    
    public int getInterval();
    
  }
  
  public abstract StartParameterAlgorithm getStartParameterAlgorithm();
  

  
}

