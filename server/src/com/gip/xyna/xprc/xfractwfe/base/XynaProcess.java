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
package com.gip.xyna.xprc.xfractwfe.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xdev.exceptions.XDEV_CLASS_INSTANTIATION_PROBLEM;
import com.gip.xyna.xdev.exceptions.XDEV_UserDefinedDeploymentException;
import com.gip.xyna.xdev.exceptions.XDEV_UserDefinedUnDeploymentException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_ERROR_IN_COMPENSATION;
import com.gip.xyna.xprc.exceptions.XPRC_OrderShouldDieBecauseUnsafe;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xfractwfe.base.parallel.SuspendableParallelTask;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.AbortionOfSuspendedOrderResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.scheduling.XynaOrderExecutor;



public abstract class XynaProcess
                implements
                  Serializable,
                  EngineSpecificProcess,
                  Scope {

  private static final long serialVersionUID = 6768919721885484312L;
  private static Logger logger = CentralFactoryLogging.getLogger(XynaProcess.class);
  
  /**
   * revision -&gt; fqclassname -&gt; operationName -&gt; true, falls workflow, false sonst.
   * 
   * map wird vom deploymenthandler im processmanagement gepflegt
   */
  public static final ConcurrentMap<Long, ConcurrentMap<String, ConcurrentMap<String, Boolean>>> instanceMethodTypes = new ConcurrentHashMap<Long, ConcurrentMap<String, ConcurrentMap<String, Boolean>>>(16, 0.75f, 2);    

  private static Handler[] globalPreHandlers = new Handler[0];
  private static Handler[] globalPostHandlers = new Handler[0];
  private static Handler[] globalErrorHandlers = new Handler[0];
  private static Handler[] globalPreCompensationHandlers = new Handler[0];
  private static Handler[] globalPostCompensationHandlers = new Handler[0];

  private transient Handler[] preHandlers = null;
  private transient Handler[] errorHandlers = null;
  private transient Handler[] postHandlers = null;
  private transient Handler[] preCompensationHandlers = null;
  private transient Handler[] postCompensationHandlers = null;
  
  //generierter code greift hier beim retry-feature drauf zu
  public AtomicLong retryCounter = new AtomicLong(0);
  
  private volatile boolean needsToBeInitializedBeforeNextExecution = true;
  private volatile boolean hasExecutedSuccessfully = false;
  private volatile boolean hasCompensatedSuccessfully = false;
  
  private final AtomicLong nextSuspendedLaneId = new AtomicLong(0);
  
  private volatile boolean calledGlobalCompensatePreHandlersAtLeastOnce = false;

  // the xyna order that (directly) lead to the execution of this workflow
  private XynaOrderServerExtension correlatedXynaOrder;

  //TODO: wann wird das auf true gesetzt?
  //1. processsuspendedexception gefangen wird
  private volatile boolean isSuspended = false;
  private volatile boolean attemptingSuspension = false;

  private volatile boolean compensationHasBegun = false;
  
  private volatile ProcessAbortedException abortionException = null;
  
  /**
   * true falls sofort ein interrupt an einen thread gesendet wird
   */
  private transient volatile boolean isGoingToBeInterrupted = false;
  
  /**
   * true falls ein fehler passiert ist, nachdem {@link #isGoingToBeInterrupted} auf true gesetzt wurde
   */
  private transient volatile boolean hadErrorAfterInterruption = false;
  private boolean varsSet = false;
  
  //achtung: der gleiche thread kann unterschiedliche tasks bearbeiten, wenn parallelit�ten verschachtelt sind
  private volatile transient HashMap<ParallelTask, Thread> threadsActivelyRunningInParallelTask;
  private volatile transient List<Thread> activeThreads = new ArrayList<Thread>();
  
  private transient Throwable throwableThrownBefore;
  
  private volatile XynaProcessState state = XynaProcessState.RUNNING;
  protected volatile transient Set<Thread> abortFailedThreads;
  private volatile transient RootProcessData rootProcessData;
  
  public XynaProcess() {
    if (logger.isTraceEnabled()) {
      logger.trace("created process " + this + " with cl " + getClass().getClassLoader());
    }
  }


  public abstract String getOriginalName();

  public Scope getParentScope() {
    return null;
  }

  /**
   * Ausf�hrung des Prozesses
   */
  protected GeneralXynaObject executeInternally(GeneralXynaObject o) throws XynaException {
    FractalProcessStep<?>[] steps = getStartSteps();
    if (varsSet) {
      // beim resume: nicht die inputvariablen des workflows �berschreiben.
    } else {
      setInputVars(o);
      varsSet = true;
    }
    for (FractalProcessStep<?> step : steps) {
      if (logger.isDebugEnabled()) {
        logger.debug("calling step " + step + ". (executedSuccessfullyBefore = " + step.hasExecutedSuccessfully() + ")");
      }
      
      boolean abort = isProcessAborted();
      
      if (abort && !step.needsToBeExecutedEvenIfAborted()) {
        throw new ProcessAbortedException(abortionException);
      }
      
      if (!step.hasExecutedSuccessfully()) {
        if(correlatedXynaOrder.isLetOrderAbort()) {
          throw new XPRC_OrderShouldDieBecauseUnsafe(correlatedXynaOrder.getId());
        }
        try {
          step.execute();
        } catch (ProcessSuspendedException e) {
          throw e;
        } catch (XynaException e) {
          if ( isProcessAborted() ) {
            Throwable cause = e;
            while (!(cause.getCause() == null || cause.getCause() == cause)) {
              cause = cause.getCause();
            }
            cause.initCause(abortionException);
          }
          throw e;
        }
        
        abort = isProcessAborted();
        
        if (abort) {
          throw new ProcessAbortedException(abortionException);
        }
      }
    }
    return getOutput();
  }


  public void setSuspended() {
    setState(XynaProcessState.SUSPENDED);
    setSuspended(true);
    getRootProcessData().setSuspended(this);
  }
    
  public void checkSuspendingAndThrowProcessSuspendedException(FractalProcessStep<?> step, boolean afterFailure) throws ProcessSuspendedException {
    RootProcessData rpd = getRootProcessData();
    if( rpd.shouldSuspend() ) {
      /*
       * die abfrage nach isGoingToBeInterrupted ist hier notwendig f�r den usecase, dass der root-auftrags-thread hing und ersetzt wurde
       * der ersatz thread startet den workflow dann erneut und soll dann eine suspensionexception mit MI-redirection werfen
       */
      rpd.suspend(this, step, afterFailure || isGoingToBeInterrupted);
    }
  }

  public RootProcessData getRootProcessData() {
    return rootProcessData;
  }

  private void setSuspended(boolean b) {
    isSuspended = b;
    getCorrelatedXynaOrder().setSuspended(b);
  }


  public boolean isSuspended() {
    return isSuspended;
  }
  

  public void setAbortionException(ProcessAbortedException e) {
    abortionException = e;
  }


  public ProcessAbortedException getAbortionException() {
    if (abortionException == null) {
      return new ProcessAbortedException(AbortionCause.UNKNOWN);
    }
    return abortionException;
  }


  public boolean isProcessAborted() {
    return state == XynaProcessState.ABORTING;
  }


  private void setAttemptingSuspension(boolean b) {
    this.attemptingSuspension = b;
    getCorrelatedXynaOrder().setAttemptingSuspension(b);
  }


  public boolean isAttemptingSuspension() {
    return attemptingSuspension;
  }
  
  public boolean hasCompensatedSuccesfully() {
    return hasCompensatedSuccessfully;
  }
  
  
  public boolean hasCalledGlobalCompensatePreHandlersAtLeastOnce() {
    return calledGlobalCompensatePreHandlersAtLeastOnce;
  }
  
  /**
   * Terminiert die Threads, die diesen XynaProcess bearbeiten, entweder per Thread.interrupt() oder Thread.stop().
   * Ruft rekursiv alle Kind-Processe f�r Subworkflows auf.
   * Zur�ckgegeben wird die Anzahl der so terminierten Threads.
   * @param stopForcefully true: Thread.stop, false: Thread.interrupt
   * @return Anzahl terminierter Threads
   */
  public int terminateThreadsOfRunningJavaServiceCalls(boolean stopForcefully) {
    if (stopForcefully) {
      return stopAndSubstituteHangingThreads(true, false);
    }
    
    int interrupted = 0;
    List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.ALL);
    for (FractalProcessStep<?> step : activeSteps) {
      if (step instanceof ParallelExecutionStep) {
        //nichts mehr zu tun: FractalWorkflowParallelExecutor ist bereits gecancelt worden, als erster ParallelStep 
        //eine ProcessSuspensionException gefangen hat und XynaProcess.RootProcessData.shouldSuspend() true war
        //siehe FractalWorkflowParallelExecutor.handleProcessSuspendedException(...);
      } else if( step instanceof SubworkflowCall) {
        if (logger.isDebugEnabled()) {
          logger.debug("Step " + step + " is subworkflowcall");
        }
        //rekursion auf subauftr�ge
        XynaProcess process = ((SubworkflowCall) step).getChildOrder().getExecutionProcessInstance();
        // process can be null if the order has been started but the process instance has not yet been created
        if (process != null && !process.isSuspended()) {
          interrupted += process.terminateThreadsOfRunningJavaServiceCalls(false);
        }
      } else if( step instanceof JavaCall) {
        if (step.isStepListeningOnServiceStepEvents() &&
            step.getServiceStepEventSource().hasHandlerFor(AbortServiceStepEvent.class)) {
          //JavaCall ist Abortable, daher nun abbrechen
          step.getServiceStepEventSource().dispatchEvent(new AbortServiceStepEvent(AbortionCause.SHUTDOWN)); //FIXME das stimmt so nicht immer...
          continue;
        }
        Thread t = getHangingThreadForStep(step, activeSteps.size());
        if( t != null ) {
          killThread(t, false);
          interrupted++;   
        }
      } else {
        //andere Step-Typen, die hier irrelevant sind
      }      
    }
    return interrupted;
  }

  


  private Thread findParallelExecutingThreadForStep(FractalProcessStep<?> stepToBeFound) {    
    if (threadsActivelyRunningInParallelTaskIsEmpty()) {
      return null;
    }
    synchronized (threadsActivelyRunningInParallelTask) {
      for (ParallelTask task : threadsActivelyRunningInParallelTask.keySet()) {
        if( task instanceof SuspendableParallelTask ) {
          Step step = ((SuspendableParallelTask)task).getStep();
          if (stepContainsStepInSameParallelLane(step, stepToBeFound)) {
            return threadsActivelyRunningInParallelTask.get(task);
          }
        }
      }
      return null;
    }
  }
  
  
  private boolean threadsActivelyRunningInParallelTaskIsEmpty() {
    if (threadsActivelyRunningInParallelTask == null) {
      return true;
    }
    
    synchronized (threadsActivelyRunningInParallelTask) {
      return threadsActivelyRunningInParallelTask.size() == 0;
    }
  }

  
  /**
   * gibt true zur�ck, falls der stepToBeFound ein kind oder kindeskind (etc) von parentStep ist und keiner der
   * dazwischen liegenden steps (inkl des parentSteps) ein parallelstep ist. 
   */
  private boolean stepContainsStepInSameParallelLane(Step parentStep, FractalProcessStep<?> stepToBeFound) {
    if (parentStep == stepToBeFound) {
      return true;
    } else if (parentStep instanceof ParallelExecutionStep) {
      return false;
    } else {
      FractalProcessStep<?> parentFps = (FractalProcessStep<?>)parentStep;
      for (int i = 0; i < parentFps.getChildrenTypesLength(); i++) {
        for (FractalProcessStep<?> currentStep : parentFps.getChildren(i))
          if (stepContainsStepInSameParallelLane(currentStep, stepToBeFound)) {
            return true;
          }
      }
    }
    return false;
  }


  private void killThread(Thread thread, boolean threadShouldBeStoppedForcefully) {
    if (threadShouldBeStoppedForcefully) {
      if (logger.isDebugEnabled()) {
        logger.debug(this + " >>> stopping thread " + thread);
      }
      thread.stop();
    } else {
      isGoingToBeInterrupted = true;
      if (logger.isTraceEnabled()) {
        logger.trace(this + " >>> interrupting thread " + thread);
      }
      thread.interrupt();
    }    
  }

  public enum ActiveStepType {
    ALL, SUBWF, JAVACALL, PARALLEL;
  }

  
  /**
   * gibt die gerade aktiven steps zur�ck. das sind alle java-service-call und subwfcall steps, die
   * angefangen sind, aber noch nicht fertig/fehlerhaft. und alle parallelexecutionsteps die nocht nicht
   * fertig/fehlerhaft sind.<p>
   * 
   * falls recursive != NONE und der subwf bereits gestartet wurde, werden anstelle des subwfcall steps 
   * die zugeh�rigen aktiven steps des subwfs zur�ckgegeben.
   * 
   */
  public List<FractalProcessStep<?>> getCurrentExecutingSteps(RecursionType recursive) {
    return getCurrentExecutingSteps(recursive, ActiveStepType.ALL);
  }
  
  public enum RecursionType {
    NONE {

      @Override
      public boolean shouldRecurse(XynaProcess subWf) {
        return false;
      }
    }, EXECUTING {

      @Override
      public boolean shouldRecurse(XynaProcess subWf) {
        return subWf.getState().isExecuting();
      }
    }, INCLUDING_SUSPENDED {

      @Override
      public boolean shouldRecurse(XynaProcess subWf) {
        return subWf.getState().isExecuting() || subWf.getState() == XynaProcessState.SUSPENDED || subWf.getState() == XynaProcessState.SUSPENDED_AFTER_ABORTING;
      }
    };

    public abstract boolean shouldRecurse(XynaProcess subWf);
  }


  public List<FractalProcessStep<?>> getCurrentExecutingSteps(RecursionType recursive, ActiveStepType filter) {
    List<FractalProcessStep<?>> currentExecuting = new ArrayList<FractalProcessStep<?>>();

    FractalProcessStep<?>[] mysteps = getAllSteps();
    if (mysteps != null) {
      for (FractalProcessStep<?> step : mysteps) {
        //step beendet?
        if (step.hasExecutedSuccessfully() || step.hasError() || !step.hasBegunExecution()) {
          continue;
        }
        
        //step detached?
        if (step instanceof DetachedCall) {
          continue;
        }
        
        //step nicht beendet:

        if (step instanceof SubworkflowCall) {
          if (recursive != RecursionType.NONE) {
            XynaOrderServerExtension childOrder = ((SubworkflowCall) step).getChildOrder();
            if (childOrder != null) {
              XynaProcess subWf = childOrder.getExecutionProcessInstance();
              if (subWf != null) {
                if (recursive.shouldRecurse(subWf)) {
                  //aktiver subwf ist am laufen
                  currentExecuting.addAll(subWf.getCurrentExecutingSteps(recursive, filter));
                  continue;
                }
              }
            }
          }
          //subwf ist noch nicht gestartet oder recursive=false
          if (filter == ActiveStepType.ALL || filter == ActiveStepType.SUBWF) {
            currentExecuting.add(step);
          }
          continue;
        }

        if (step instanceof JavaCall) {
          //falls rekursion, rekursion beachten
          boolean foundCurrentlyExecutingSubWf = false;
          List<XynaOrderServerExtension> childOrders = ((JavaCall) step).getChildOrders();
          if (childOrders != null) {
            for (XynaOrderServerExtension child : childOrders) {
              if (child.getExecutionProcessInstance() != null) {
                if (child.getExecutionProcessInstance().getState().isExecuting()) {
                  foundCurrentlyExecutingSubWf = true;
                }                
                if (recursive.shouldRecurse(child.getExecutionProcessInstance())) {
                  currentExecuting.addAll(child.getExecutionProcessInstance().getCurrentExecutingSteps(recursive, filter));
                }
              }
            }
          }

          if (!foundCurrentlyExecutingSubWf && (filter == ActiveStepType.ALL || filter == ActiveStepType.JAVACALL)) {
            //wenn javacall in einem implizit aufgerufenen workflow h�ngt, wird javacall-step nicht als "current executing" zur�ckgegeben.
            currentExecuting.add(step);
          }
          continue;
        }
        
        if (step instanceof ParallelExecutionStep) {
          if (filter == ActiveStepType.ALL || filter == ActiveStepType.PARALLEL) {
            currentExecuting.add(step);
          }
        }
      }
    }
    return currentExecuting;
  }
  
  /**
   * muss statisch gehaltene boolean variable zur�ckgeben, die per default auf true gesetzt wird.
   * 
   * @return
   */
  private static boolean isNotDeployed(Class<? extends XynaProcess> wfClass) {
    return false; //TODO derzeit nicht benutzt
  }


  /**
   * Membervariablen initialisieren, so dass eine Workflow Instanz wiederverwendet werden kann
   */
  protected abstract void initializeMemberVars();

  
  /**
   * wird einmal pro verwendung in auftrag anfangs ausgef�hrt um steps zu initialisieren
   */
  public void tryReinitialize() {
    if (needsToBeInitializedBeforeNextExecution) {
      initializeMemberVars();
      for (FractalProcessStep<?> step : getAllSteps()) {
        step.prepareForRetryRecursivly( true );
      }
      needsToBeInitializedBeforeNextExecution = false;
      prepareForResume();
    }
  }
  

  /**
   * resetted variablen die beim suspend gesetzt wurden, die beim resume nicht mehr gesetzt sein sollen.
   * achtung: bei einem ersetzen des root-auftrags-threads wird die methode nicht aufgerufen.
   */
  public void prepareForResume() {
    setSuspended(false);
    setAttemptingSuspension(false);
    hadErrorAfterInterruption = false;
    isGoingToBeInterrupted = false;
  }

  /**
   * ziele: 
   * - objekt kann nun im objektpool verbleiben 
   * - vorbereitung zur wiederverwendung
   * - wenig speicherverbrauch 
   */
  public void clear() {
    hasExecutedSuccessfully = false;
    hasCompensatedSuccessfully = false;
    threadsActivelyRunningInParallelTask = null;
    activeThreads.clear();
    throwableThrownBefore = null;
    varsSet = false;
    preHandlers = null;
    errorHandlers = null;
    postHandlers = null;
    preCompensationHandlers = null;
    postCompensationHandlers = null;    
    nextSuspendedLaneId.set(0);
    retryCounter.set(0);

    // warning: other components may need to take care not to access the correlated
    // XynaOrder after the instance has been returned to the pool
    correlatedXynaOrder = null;
    rootProcessData = null;
    state = XynaProcessState.RUNNING; //oder FINISHED? RUNNING ist halt der default-wert der variable...
    abortionException = null;
    calledGlobalCompensatePreHandlersAtLeastOnce = false;

    needsToBeInitializedBeforeNextExecution = true; 
  }


  public GeneralXynaObject execute(GeneralXynaObject o, XynaOrderServerExtension xo) throws XynaException {
    setState(XynaProcessState.RUNNING);
    if (xo == null) {
      throw new IllegalArgumentException(XynaProcess.class.getSimpleName()
                      + " may not be executed without providing the XynaOrder");
    }
    
    correlatedXynaOrder = xo;
    tryReinitialize();
    initRootProcessData();
    
    try {
      //braucht u.U. nicht synchronisiert zu sein, f�hrt aber sonst teilweise zu ConcurrentModificationException
      synchronized (activeThreads) {
        activeThreads.add(Thread.currentThread());
      }

      if (throwableThrownBefore != null) {
        throw throwableThrownBefore;
      }
      GeneralXynaObject result = executeInternally(o);
      hasExecutedSuccessfully = true;
      setState(XynaProcessState.FINISHED);      
      
      //das muss in einem finally passieren, aber vor dem compensate, und nicht bei suspension.
      if (correlatedXynaOrder.getOrderExecutionTimeout() != null) {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
            .getOrderExecutionTimeoutManagement().tryUnregisterOrderTimeout(correlatedXynaOrder);
      }
      
      return result;

    } catch (ProcessSuspendedException e) {
      ParameterInheritanceManagement parameterInheritanceMgmt = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
      InheritanceRule rule = parameterInheritanceMgmt.getPreferredSuspensionBackupRule(xo);
      if (rule == null) {
        e.getSuspensionCause().setSuspensionOrderBackupModeIfUnset(SuspensionBackupMode.DEFAULT_ORDERBACKUP_MODE.get());
      } else {
        e.getSuspensionCause().setSuspensionOrderBackupModeIfUnset(SuspensionBackupMode.valueOf(rule.getValueAsString()));
      }
      setSuspended();
      throw e;
    } catch (ProcessAbortedException e) {
      handleException(e, XynaProperty.ORDERABORTION_COMPENSATE.get());
      throw e;
    } catch (OrderDeathException e) {
      if (correlatedXynaOrder.getOrderExecutionTimeout() != null) {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
            .getOrderExecutionTimeoutManagement().tryUnregisterOrderTimeout(correlatedXynaOrder);
      }
      throw e;
    } catch (XynaException e) {
      handleException(e);
      throw e;
    } catch (RuntimeException e) {
      handleException(e);
      throw e;
    } catch (Error e) {
      Department.handleThrowable(e);
      handleException(e);
      throw e;
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //this case should never happen, but is necessary for nicer code.
      throw new RuntimeException(t);
    } finally {
      //braucht u.U. nicht synchronisiert zu sein, f�hrt aber sonst teilweise zu ConcurrentModificationException
      synchronized (activeThreads) {
        activeThreads.remove(Thread.currentThread());
      }
    }
  }
  
  private void handleException(Throwable e) throws XynaException {
    handleException(e, true);
  }
  
  private void handleException(Throwable e, boolean compensate) throws XynaException {
    try {
      if (isAttemptingSuspension()) {
        if (isGoingToBeInterrupted) {
          hadErrorAfterInterruption = true;
        } else {
          throwableThrownBefore = e;
        }
        throw SuspendResumeManagement.suspendManualOrShutDown(getCorrelatedXynaOrder().getId(), null);
      }
      
      if (correlatedXynaOrder.getOrderExecutionTimeout() != null) {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
            .getOrderExecutionTimeoutManagement().tryUnregisterOrderTimeout(correlatedXynaOrder);
      }
      
      //falls der fehler durch eine suspension ausgel�st wurde (zb thread.interrupt), 
      //soll fehler vergessen werden damit der ausl�sende schritt beim resume erneut ausgef�hrt wird.
      // wenn andererseits das suspend erst innerhalb des compensates passiert, muss der fehler vorher gespeichert worden sein.      
      throwableThrownBefore = e;
      if(correlatedXynaOrder.isLetOrderCompensateAfterAbort() && compensate) {
        compensate();
      }
      setState(XynaProcessState.FINISHED);
    } catch (XynaException xe) {
      handleExceptionDuringCompensation(xe, e);
    } catch (ProcessSuspendedException pse) {
      setSuspended();
      throw pse;
    } catch (ProcessAbortedException pae) {
      setState(XynaProcessState.FINISHED);
      throw pae;
    } catch (RuntimeException re) {
      handleExceptionDuringCompensation(re, e);
    } catch (Error err) {
      Department.handleThrowable(err);
      handleExceptionDuringCompensation(err, e);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //this case should never happen, but is necessary for nicer code.
      throw new RuntimeException(t);
    }
  }
  
  private void handleExceptionDuringCompensation(Throwable e, Throwable original) throws XynaException {
    if (isAttemptingSuspension() && isGoingToBeInterrupted) {
      //fehler im compensate, die durch ein interrupt von haengenden services bei suspension ausgel�st wurden, 
      //werden vergessen, damit beim resume die schritte erneut ausgef�hrt werden k�nnen.
      hadErrorAfterInterruption = true;
      setSuspended();
      SuspensionCause suspensionCause = new SuspensionCause_Manual(false, getCorrelatedXynaOrder().getId());
      throw new ProcessSuspendedException(suspensionCause);
    }
    //else: danach passiert nich viel, da kann man beim suspend-request ruhig den wf beenden und muss ihn nicht suspendieren
    setState(XynaProcessState.FINISHED);
    throw new XPRC_ERROR_IN_COMPENSATION(e.getMessage(), original.getMessage())
                    .initCauses(new Throwable[] {original, e});
  }

  private volatile transient boolean compensateBarrier = false;

  public void compensate() throws XynaException {
    while (compensateBarrier) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }
    }
    initRootProcessData();
    setState(XynaProcessState.RUNNING, true);
    compensationHasBegun = true;
    try {
      FractalProcessStep<?>[] steps = getStartSteps();
      for (int j = steps.length - 1; j >= 0; j--) {
        steps[j].compensate(); //intern werden in der richtigen reihenfolge die kinder kompensiert
      }
      hasCompensatedSuccessfully = true;
      setState(XynaProcessState.FINISHED);
    } catch (ProcessSuspendedException e) {
      setState(XynaProcessState.SUSPENDED);
      throw e;
    } catch (XynaException e) {
      setState(XynaProcessState.FINISHED);
      throw e;
    } catch (ThreadDeath t) {
      throw t;
    } catch (RuntimeException e) {
      setState(XynaProcessState.FINISHED);
      throw e;
    } catch (Error e) {
      Department.handleThrowable(e);
      setState(XynaProcessState.FINISHED);
      throw e;
    }
  }


  private void initRootProcessData() {  
    if (rootProcessData == null) {
      if( correlatedXynaOrder != null && correlatedXynaOrder.hasParentOrder() && correlatedXynaOrder.getRootOrder().getExecutionProcessInstance() != null) {
        rootProcessData = correlatedXynaOrder.getRootOrder().getExecutionProcessInstance().getRootProcessData();
      } else {
        //TODO oder k�nnen subworkflows von planningworkflows darauf verzichten? sollte aber nichts schaden
        rootProcessData = new RootProcessData();
      } 
    }
  }


  public List<XynaOrderServerExtension> getAllChildOrders(boolean recursively, boolean activeOnly) {
    // FIXME this is a hack!
    if (needsToBeInitializedBeforeNextExecution) {
      return new ArrayList<XynaOrderServerExtension>();
    }

    FractalProcessStep<?>[] steps = getAllSteps();
    List<XynaOrderServerExtension> list = new ArrayList<XynaOrderServerExtension>();
    for (FractalProcessStep<?> step : steps) {
      if (!step.hasBegunExecution()) {
        continue;
      }
      
      if (activeOnly && (step.hasError() || step.hasExecutedSuccessfully())) {
        continue;
      }
      
      if (step instanceof JavaCall) {
        List<XynaOrderServerExtension> childOrders = ((JavaCall) step).getChildOrders();
        if (childOrders != null) {
          list.addAll(childOrders);
          if (recursively) {
            for (XynaOrderServerExtension child : childOrders) {
              if (child.getExecutionProcessInstance() != null) { // this might be null if the suborder has not yet been scheduled
                list.addAll(child.getExecutionProcessInstance().getAllChildOrdersRecursively());
              }
            }
          }
        }
      }
      
      if (step instanceof SubworkflowCall) {
        XynaOrderServerExtension child = ((SubworkflowCall) step).getChildOrder();
        if (child != null) {
          list.add(child);
          if (recursively) {
            if (child.getExecutionProcessInstance() != null) { // this might be null if the suborder has not yet been scheduled
              list.addAll(child.getExecutionProcessInstance().getAllChildOrdersRecursively());
            }
          }
        }
      }
    }

    return list;
  }

  /**
   * liefert Liste aller Kind und Kindeskind Auftr�ge (von ausgef�hrten oder sich in der Ausf�hrung befindenden
   * Subworkflows - noch nicht ausgef�hrte Subworkflows sind nicht enthalten)
   */
  public List<XynaOrderServerExtension> getAllChildOrdersRecursively() {
    return getAllChildOrders(true, false);
  }


  /**
   * services initialisieren
   */
  protected abstract void onDeployment() throws XynaException;

  protected abstract void onUndeployment() throws XynaException;


  private static final Handler[] EMPTY_HANDLER_ARRAY = new Handler[0];
  private static volatile EngineSpecificStepHandlerManager shm;
  
  private static EngineSpecificStepHandlerManager getSHM() {
    if (shm == null) {
      shm = XynaFactory.getInstance().getProcessing().getWorkflowEngine()
          .getStepHandlerManager();
    }
    return shm;
  }


  public void preHandler(FractalProcessStep<?> pstep) {
    // prozessspezifisch
    if (preHandlers == null) {
      if (pstep.getProcess().getCorrelatedXynaOrder().getExecutionProcessInstance() == this) {
        preHandlers = createArray(getSHM().getHandlers(ProcessStepHandlerType.PREHANDLER, correlatedXynaOrder));
      } else {
        preHandlers = EMPTY_HANDLER_ARRAY;
      }
    }
    handleHandlers(this, pstep, preHandlers);
    // global
    handleHandlers(this, pstep, globalPreHandlers);
  }


  public void errorHandler(FractalProcessStep<?> pstep) {
    // prozessspezifisch
    if (errorHandlers == null) {
      if (pstep.getProcess().getCorrelatedXynaOrder().getExecutionProcessInstance() == this) {
        errorHandlers = createArray(getSHM().getHandlers(ProcessStepHandlerType.ERRORHANDLER, correlatedXynaOrder));
      } else {
        errorHandlers = EMPTY_HANDLER_ARRAY;
      }
    }
    handleHandlers(this, pstep, errorHandlers);
    // global
    handleHandlers(this, pstep, globalErrorHandlers);
  }


  private static Handler[] createArray(List<Handler> list) {
    int size = list.size();
    if (size == 0) {
      return EMPTY_HANDLER_ARRAY;
    }
    Handler[] arr = new Handler[size];
    list.toArray(arr);
    return arr;
  }


  public void postHandler(FractalProcessStep<?> pstep) {
    // prozessspezifisch
    if (postHandlers == null) {
      if (pstep.getProcess().getCorrelatedXynaOrder().getExecutionProcessInstance() == this) {
        postHandlers = createArray(getSHM().getHandlers(ProcessStepHandlerType.POSTHANDLER, correlatedXynaOrder));
      } else {
        postHandlers = EMPTY_HANDLER_ARRAY;
      }
    }
    handleHandlers(this, pstep, postHandlers);
    // global
    handleHandlers(this, pstep, globalPostHandlers);
  }


  public void compensatePreHandler(FractalProcessStep<?> pstep) {
    // prozessspezifisch
    if (preCompensationHandlers == null) {
      if (pstep.getProcess().getCorrelatedXynaOrder().getExecutionProcessInstance() == this) {
        preCompensationHandlers = createArray(getSHM().getHandlers(ProcessStepHandlerType.PRECOMPENSATION, correlatedXynaOrder));
      } else {
        preCompensationHandlers = EMPTY_HANDLER_ARRAY;
      }
    }
    handleHandlers(this, pstep, preCompensationHandlers);
    // global
    handleHandlers(this, pstep, globalPreCompensationHandlers);
    calledGlobalCompensatePreHandlersAtLeastOnce = true;
  }


  public void compensatePostHandler(FractalProcessStep<?> pstep) {
    // prozessspezifisch
    if (postCompensationHandlers == null) {
      if (pstep.getProcess().getCorrelatedXynaOrder().getExecutionProcessInstance() == this) {
        postCompensationHandlers = createArray(getSHM().getHandlers(ProcessStepHandlerType.POSTCOMPENSATION, correlatedXynaOrder));
      } else {
        postCompensationHandlers = EMPTY_HANDLER_ARRAY;
      }
    }
    handleHandlers(this, pstep, postCompensationHandlers);
    // global
    handleHandlers(this, pstep, globalPostCompensationHandlers);
  }


  public static void addGlobalPreHandler(Handler h) {
    globalPreHandlers = addHandlerToArray(h, globalPreHandlers);
  }


  public static void addGlobalErrorHandler(Handler h) {
    globalErrorHandlers = addHandlerToArray(h, globalErrorHandlers);
  }


  public static void addGlobalPostCompensationHandler(Handler h) {
    globalPostCompensationHandlers = addHandlerToArray(h, globalPostCompensationHandlers);
  }
  
  public static void addGlobalPreCompensationHandler(Handler h) {
    globalPreCompensationHandlers = addHandlerToArray(h, globalPreCompensationHandlers);
  }
  
  public static void addGlobalPostHandler(Handler h) {
    globalPostHandlers = addHandlerToArray(h, globalPostHandlers);
  }
  


  // diese methoden erlauben auch das entfernen von handlern

  public static Handler[] getGlobalPreHandlers() {
    return globalPreHandlers;
  }


  public static Handler[] getGlobalErrorhandlers() {
    return globalErrorHandlers;
  }


  public static Handler[] getGlobalPostHandlers() {
    return globalPostHandlers;
  }

  public static Handler[] getGlobalPostCompensationHandlers() {
    return globalPostCompensationHandlers;
  }
  public static Handler[] getGlobalPreCompensationHandlers() {
    return globalPreCompensationHandlers;
  }

  protected static void handleHandlers(XynaProcess process, FractalProcessStep<?> pstep, Handler[] handlers) {
    for (Handler h : handlers) {
      h.handle(process, pstep);
    }
  }

  /*
   * achtung: wird vom generierten code aus aufgerufen
   */
  public XynaOrderServerExtension getCorrelatedXynaOrder() {
    return correlatedXynaOrder;
  }
  
  public static Handler[] removeHandlerFromArray(Handler h, Handler[] oldList) {

    if (oldList.length == 0) {
      logger.info("Tried to remove non-existent Handler, doing nothing");
      return oldList;
    }

    Handler[] newHandlers = new Handler[oldList.length - 1];

    boolean found = false;
    for (int i = 0; i < newHandlers.length; i++) {
      if (oldList[i] == h) {
        found = true;
        continue;
      }

      if (found) {
        newHandlers[i] = oldList[i + 1];
      }
      else
        newHandlers[i] = oldList[i];

    }

    return newHandlers;

  }

  public static Handler[] addHandlerToArray(Handler h, Handler[] oldList) {
    Handler[] newHandlers = new Handler[oldList.length + 1];
    System.arraycopy(oldList, 0, newHandlers, 0, oldList.length);
    newHandlers[oldList.length] = h;
    return newHandlers;
  }

  public void setNeedsReinitialization() {
    needsToBeInitializedBeforeNextExecution = true;
  }
  
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeObject(new SerializableClassloadedException(throwableThrownBefore));
  }
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    activeThreads = new ArrayList<Thread>();
    throwableThrownBefore = ((SerializableClassloadedException)s.readObject()).getThrowable();
  }


  public boolean hadErrorAfterInterruption() {
    return hadErrorAfterInterruption;
  }


  public void addActiveThread(Thread t) {
    synchronized (activeThreads) {
      activeThreads.add(t);
    }
  }


  public void removeActiveThread(Thread t) {
    synchronized (activeThreads) {
      activeThreads.remove(t);
    }
  }


  public void addActiveParallelTaskThread(Thread t, ParallelTask task) {
    lazyCreateThreadsActivelyRunningInParallelTaskMap();
    synchronized (threadsActivelyRunningInParallelTask) {
      threadsActivelyRunningInParallelTask.put(task, Thread.currentThread());
    }
  }


  public void removeActiveParallelTaskThread(ParallelTask task) {
    lazyCreateThreadsActivelyRunningInParallelTaskMap();
    synchronized (threadsActivelyRunningInParallelTask) {
      threadsActivelyRunningInParallelTask.remove(task);
    }
  }

  private void lazyCreateThreadsActivelyRunningInParallelTaskMap() {
    if (threadsActivelyRunningInParallelTask == null) {
      synchronized (this) {
        if (threadsActivelyRunningInParallelTask == null) {
          threadsActivelyRunningInParallelTask = new HashMap<ParallelTask, Thread>();
        }
      }
    }
  }


  void ensureActivelyRunningParallelTasksThreadMappingIsNotNull() {
    if (threadsActivelyRunningInParallelTask == null) { //braucht nicht synchronisiert zu sein
      threadsActivelyRunningInParallelTask = new HashMap<ParallelTask, Thread>();
    }
  }

  /**
   * erzeugt process-instanz-eindeutige ids f�r die lanes aller parallelexecutors
   */
  long getNextSuspendedLaneId() {
    return nextSuspendedLaneId.incrementAndGet();
  }


  public boolean hasExecutedSuccessfully() {
    return hasExecutedSuccessfully;
  }
  
  public enum AbortionResult {
    SUCCESS, UNSUCCESSFUL, 
  }

  /**
   * 
   * @param killJavaCalls wird bei einem aufruf mit false �bergeben, weil erst eine rekursion �ber die hierarchie 
   *                      passieren soll, in der nur der state umgesetzt wird, und parallelit�ten angehalten werden
   */
  private AbortionResult abortRunningWFInternally(boolean killJavaCalls, KillStuckProcessBean bean, int resumeHangsCounter, AtomicBoolean alreadyAbortedBefore) {
    outer : while (true) {
      XynaProcessState s = state;
      switch (s) {
        case ABORTING :
          if (logger.isDebugEnabled()) {
            logger.debug("workflow already in state " + XynaProcessState.ABORTING + " -> recursion to sub wfs");
          }
          //wurde bereits schonmal abgebrochen, evtl h�ngt ein subwf in einer compensation
          for (XynaOrderServerExtension childOrder : getCorrelatedXynaOrder().getDirectChildOrders()) {
            XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderAbortionManagement()
                .abortMasterWorkflow(childOrder, childOrder.getId(), bean, true);
          }
          break;
        case FINISHED :
          if (logger.isDebugEnabled()) {
            logger.debug("workflow in state " + XynaProcessState.FINISHED);
          }
          break;
        case RUNNING :
          if (alreadyAbortedBefore.get()) {
            //evtl bereits in der compensation.
            if (logger.isDebugEnabled()) {
              logger.debug("workflow in state " + XynaProcessState.RUNNING
                  + " and has been aborted before -> abortion successful.");
            }
            return AbortionResult.SUCCESS;
          }
          if (logger.isDebugEnabled()) {
            logger.debug("workflow in state " + XynaProcessState.RUNNING + " -> " + XynaProcessState.ABORTING);
          }
          setCompensationBarrier(true); //damit man nach dem setzen des aborting states nicht rekursion �ber schritte macht, die bereits kompensieren
          try {
            setAbortionException(new ProcessAbortedException(bean.getOrderIdToBeKilled(), bean.getTerminationReason()));
            if (!compareAndSetState(XynaProcessState.RUNNING, XynaProcessState.ABORTING)) {
              setAbortionException(null);
              continue outer; //nochmal checken
            }

            List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.PARALLEL);

            //erst alle parallelsteps pausieren, damit auftr�ge weniger h�ufig im planning sind
            boolean foundParallelStep = false;
            for (FractalProcessStep<?> step : activeSteps) {
              foundParallelStep = true;
              ParallelExecutionStep<?> pStep = (ParallelExecutionStep<?>) step;
              //parallel execution soll keine weiteren tasks starten, wenn parallelit�t beschr�nkt ist
              pStep.getFractalWorkflowParallelExecutor().cancel();
            }

            if (foundParallelStep) {
              //evtl weiteren gestarteten threads eine chance geben, dass man sie bei currentexecutionsteps findet
              try {
                //TODO performance: verbesserungsidee: parallelexecutor.cancel hat r�ckgabewert, an dem man erkennt, ob man hier warten muss
                Thread.sleep(30);
              } catch (InterruptedException e) {
              }
            }

            //rekursion
            for (XynaOrderServerExtension childOrder : getAllChildOrders(false, true)) {
              //FIXME warten, bis childorder != null
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getOrderAbortionManagement()
                  .abortMasterWorkflow(childOrder, childOrder.getId(), bean, false);
            }
          } finally {
            setCompensationBarrier(false);
          }

          if (!killJavaCalls) {
            //w�hrend der rekursion werden nur parallelit�ten angehalten und der state umgesetzt
            return AbortionResult.SUCCESS;
          }
          
          if (logger.isDebugEnabled()) {
            logger.debug("recursion finished, proceeding to search for hanging threads.");
          }
          //nun f�r alle kinder die h�ngenden schritte identifizieren und abbrechen

          //abort der abortable-services kommt direkt
          List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.EXECUTING, ActiveStepType.JAVACALL);
          boolean abortedThread = false;
          for (FractalProcessStep<?> step : activeSteps) {
            if (step.isStepListeningOnServiceStepEvents()
                && step.getServiceStepEventSource().hasHandlerFor(AbortServiceStepEvent.class)) {
              //abort
              if (logger.isDebugEnabled()) {
                logger.debug("aborted service call " + step + " in order "
                    + step.getProcess().getCorrelatedXynaOrder().getId() + ".");
              }
              step.getServiceStepEventSource().dispatchEvent(new AbortServiceStepEvent(bean.getTerminationReason()));
              abortedThread = true;
            }
          }
          if (abortedThread) {
            bean.getResultMessageStringBuilder().append("  * Sent abort signal to running abortable service(s).\n");
          }

          //warten, dass alle kinder nach != ABORTING wechseln
          long timeout = XynaProperty.TIMEOUT_SUSPENSION.getMillis() / 2;
          if (logger.isInfoEnabled()) {
            logger.info("waiting for hanging service calls for " + timeout + " ms.");
          }
          boolean stepSuccessfullyAborted = waitForAllStepsToBeAborted(timeout);

          if (!stepSuccessfullyAborted) {
            if (logger.isDebugEnabled()) {
              logger.debug("some steps could not be aborted.");
            }
            if (interruptHangingThreadsRecursively()) {
              bean.getResultMessageStringBuilder().append("  * Sent interrupt to executing thread(s).\n");
            }

            if (logger.isInfoEnabled()) {
              logger.info("waiting for hanging service calls for " + timeout + " ms after interrupt.");
            }
            stepSuccessfullyAborted = waitForAllStepsToBeAborted(timeout);
            if (!stepSuccessfullyAborted) {

              stopAndSubstituteHangingThreads(bean.forceKill(), true);
              
              //den neu gestarteten threads eine chance geben, den auftrag zu beenden
              if (logger.isInfoEnabled()) {
                logger.info("waiting for threads to finish");
              }
              stepSuccessfullyAborted = waitForAllStepsToBeAborted(5000);
              if (!stepSuccessfullyAborted) {
                /*
                 * es kann sein, dass ein subworkflow auf eine durch das abort gestartete compensation lange braucht und
                 * ein suspend deshalb nicht durchkommt. das muss man aber auch resumen!
                 * 
                 *    parallelit�t
                 *    ------------
                 *    |          |
                 *    v          v
                 *  wait()      kind-auftrag
                 *                   |
                 *                   v
                 *            compensate h�ngt
                 * 
                 */
                cleanupPartiallySuspendedChildren();
              } 
            }
          }

          s = getState();
          //suspended -> bereits am kompensieren.
          if (s != XynaProcessState.SUSPENDED_AFTER_ABORTING) {
            if (logger.isDebugEnabled()) {
              logger.debug("abortion completed for order " + getCorrelatedXynaOrder().getId() + ". wf state=" + s);
            }

            return AbortionResult.SUCCESS;
          }
          //s == SUSPENDED_AFTER_ABORTING
          alreadyAbortedBefore.set(true); //verhindern, dass man ein resume/abort aufs compensate macht
          //fall through: suspended workflow abbrechen (resumes durchf�hren)
          
        case SUSPENDED : //oder SUSPENDED_AFTER_ABORTING (nach RUNNING)
          if (alreadyAbortedBefore.get()) {
            //compensation bereits am laufen und darin suspendiert. ansonsten w�re der state suspended_after_aborting
            //dieser fall kann vorkommen, wenn suspended_after_aborting passiert, dann already resumed wurde (-> retry), und dann die compensation erneut suspendiert.
            if (logger.isDebugEnabled()) {
              logger.debug("workflow in state " + XynaProcessState.SUSPENDED + ", was aborted before -> abortion successful.");
            }
            return AbortionResult.SUCCESS;
          }
          //fall through: suspended_after_aborting macht das gleiche

        case SUSPENDED_AFTER_ABORTING : // fallthrough von oben: oder (SUSPENDED_AFTER_ABORTING nach RUNNING) oder SUSPENDED
          if (logger.isDebugEnabled()) {
            logger.debug("workflow in state " + s + " (resumeHangingCounter=" + resumeHangsCounter + ")");
          }
            try {
              AbortionOfSuspendedOrderResult success =
                  XynaFactory
                      .getInstance()
                      .getProcessing()
                      .getXynaScheduler()
                      .getOrderAbortionManagement()
                      .abortSuspendedWorkflow(getCorrelatedXynaOrder(), bean.isIgnoreResourcesWhenResuming(),
                                              resumeHangsCounter >= 5); //maximal 5 mal versuchen 
            if (success != AbortionOfSuspendedOrderResult.SUCCESS) {
              if (getState() != s) {
                //auftrag scheint noch mit dieser xynaprocessinstanz zu laufen, dann ist alles gut.
                return AbortionResult.SUCCESS; //am kompensieren?
              }
              if (success == AbortionOfSuspendedOrderResult.RESUME_FAILED_RETRY) {
                if (++resumeHangsCounter > 20) {
                  return AbortionResult.UNSUCCESSFUL;
                }
                try {
                  Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                continue; //nochmal probieren
              } else if (success == AbortionOfSuspendedOrderResult.RESUME_FAILED) {
                logger.warn("resume failed");
                if (s == XynaProcessState.SUSPENDED) {
                  //dann hat man den workflow bereits im zustand suspended angetroffen
                  return AbortionResult.UNSUCCESSFUL;
                }
                //s == SUSPENDED_AFTER_ABORTING
                /*
                 * dann ist das suspend nach dem abort gekommen, und deshalb fehlt jetzt wirklich nur noch das resume.
                 * fall A) resume hat nicht funktioniert, weil noch nicht suspendiert. => das w�rde aber zu "resume_failed_retry" f�hren
                 * 
                 * wenn das resume von selbst kommt, findet es den auftrag bereits im zustand aborting und bricht sich dadurch selbst ab.
                 * 
                 * wenn der auftrag allerdings im scheduler wartet, sollte man ihn dort abbrechen
                 */
                return AbortionResult.UNSUCCESSFUL;
              }
            }
          } catch (PersistenceLayerException e) {
            //FIXME
            throw new RuntimeException(e);
          }
          break;
        default :
          throw new RuntimeException();
      }
      break;
    }
    return AbortionResult.SUCCESS;
  }


  private int stopAndSubstituteHangingThreads(boolean stopThreads, boolean forAbort) {
    FailedThreadsInfo processesWithAbortFailedThreads = new FailedThreadsInfo();
    findAndSaveHangingThreadAsAbortFailedThreadsRecursively(processesWithAbortFailedThreads, forAbort);
    if (logger.isTraceEnabled()) {
      logger.trace("failed thread info: " + processesWithAbortFailedThreads);
    }

    int stopped = 0;
    if (stopThreads) {
      stopped = stopAbortFailedThreads(processesWithAbortFailedThreads);
    }

    cleanupAbortFailedThreads(processesWithAbortFailedThreads);
    return stopped;
  }


  private void cleanupPartiallySuspendedChildren() {
    List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.SUBWF);
    XynaProcessState s = getState();
    switch (s) {
      case FINISHED :
      case RUNNING :
      case SUSPENDED :
        //ok, compensating
        break;
      case ABORTING :
        //herausfinden, ob es lanes gibt, die man resumen muss, weil die suspension lokal erzeugt wurde
        break;
      case SUSPENDED_AFTER_ABORTING :
        //resume auf alle suspension sources, deren workflow im state SUSPENDED_AFTER_ABORTION sind, aber nicht im state SUSPENDED.
        //d.h. rekursion
        break;
      default :
        throw new RuntimeException();
    }
  }


  private void setCompensationBarrier(boolean b) {
    compensateBarrier = b;
  }


  /**
   * 
   * diese klasse ist dazu da, sich zu merken, welche h�ngenden threads welche paralleltasks welcher parallelexecutors gestartet haben.<p>
   * 
   * angenommen, der workflow sieht (unter vernachl�ssigung von nicht-parallelit�ten) so aus:
   * <pre>
   * PE = parallel executor
   * H = hangs
   * CO = child order
   * ti = thread id
   *
   *                |
   *              t0|
   *                |
   *               PE1
   *             /  |  \
   *          t0/ t1| t2\
   *          PE2   H    CO
   *        /  |  \
   *     t0/ t3| t4\
   *      H   CO   CO
   * </pre>
   * das bedeutet, t0 ist der main thread von PE1 und PE2 und er h�ngt. wenn also t3 und t4 fertig werden, ist kein
   * thread da, der in richtung PE1 das ergebnis der parallelit�t propagiert.<br>
   * t1 wird bei PE1 einfach ignoriert, also nicht darauf gewartet.
   */
  private class FailedThreadsInfo {

    private Map<XynaProcess, Pair<Boolean, Map<FractalWorkflowParallelExecutor, Set<ParallelTask>>>> info =
        new HashMap<XynaProcess, Pair<Boolean, Map<FractalWorkflowParallelExecutor, Set<ParallelTask>>>>();


    public String toString() {
      return info.toString();
    }
    
    /**
     * @return betroffene workflows
     */
    public Set<XynaProcess> getWorkflows() {
      return info.keySet();
    }


    /**
     * @return true, falls der mainthread des root PEs h�ngt, also der orderthread neu gestartet werden muss
     */
    public boolean rootParallelExecutorHangs(XynaProcess p) {
      return info.get(p).getFirst();
    }

    /**
     * @return liste der parallel executoren, die einen mainthread eines kind-PEs starten sollen.
     */
    public Set<FractalWorkflowParallelExecutor> getParallelExecutorsWithHangingTask(XynaProcess p) {
      Map<FractalWorkflowParallelExecutor, Set<ParallelTask>> map = info.get(p).getSecond();
      if (map != null) {
        return map.keySet();
      }
      return Collections.emptySet();
    }

    /**
     * @return liste der zu startenden tasks
     */
    public Set<ParallelTask> getTasksForMainThreadOfChildExecutor(XynaProcess p, FractalWorkflowParallelExecutor pe) {
      Map<FractalWorkflowParallelExecutor, Set<ParallelTask>> map = info.get(p).getSecond();
      return map.get(pe);
    }

    public void add(XynaProcess p) {
      if (!info.containsKey(p)) {
        Pair<Boolean, Map<FractalWorkflowParallelExecutor, Set<ParallelTask>>> pair = new Pair<Boolean, Map<FractalWorkflowParallelExecutor,Set<ParallelTask>>>(false, null);
        info.put(p, pair);
      }
    }

    public void setRootParallelExecutorHangs(XynaProcess p) {
      Pair<Boolean, Map<FractalWorkflowParallelExecutor, Set<ParallelTask>>> pair = info.get(p); 
      if (pair == null) {
        pair = new Pair<Boolean, Map<FractalWorkflowParallelExecutor,Set<ParallelTask>>>(false, null);
        info.put(p, pair);
      }
      pair.setFirst(true);
    }

    public void addHangingMainThreads(XynaProcess p, FractalWorkflowParallelExecutor parallelExecutor, Set<Thread> hangingChildMainThreads) {
      //parallel tasks zu den threads ermitteln
      Map<Thread, ParallelTask> hangingThreadsForParallelExecutor = getHangingThreadsForParallelExecutor(hangingChildMainThreads, parallelExecutor);
      
      //info map bef�llen
      Pair<Boolean, Map<FractalWorkflowParallelExecutor, Set<ParallelTask>>> pair = info.get(p); 
      if (pair == null) {
        pair = new Pair<Boolean, Map<FractalWorkflowParallelExecutor,Set<ParallelTask>>>(false, null);
        info.put(p, pair);
      }
      Map<FractalWorkflowParallelExecutor, Set<ParallelTask>> map = pair.getSecond();
      if (map == null) {
        map = new HashMap<FractalWorkflowParallelExecutor, Set<ParallelTask>>();
        pair.setSecond(map);
      }
      map.put(parallelExecutor, new HashSet<ParallelTask>(hangingThreadsForParallelExecutor.values()));
    }
    
  }

  /**
   * h�ngende threads analysieren, ob auftr�ge oder main threads von parallelit�ten neu gestartet werden m�ssen 
   * @see FailedThreadsInfo
   */
  private void cleanupAbortFailedThreads(FailedThreadsInfo processesWithAbortFailedThreads) {
    //cleanup (ggfs inkl. compensate) f�r (nicht notwendigerweise kind-)workflows ausf�hren, falls diese 
    //keine laufenden kinder haben
    /*
     * usecase1: auftrag h�ngt in der normalen ausf�hrung -> wird erneut gestartet, sieht state=ABORTING -> bricht ab und kompensiert, falls so konfiguriert.
     * usecase2: auftrag h�ngt in der compensation -> wird erneut gestartet, geht ins compensate wo er war -> bricht ab
     * 
     * danach wird in beiden f�llen das normale masterworkflow cleanup durchgef�hrt
     */
    int cnt = 1;
    int threadStartRetries = 30;
    for (XynaProcess processWithAbortFailedThreads : processesWithAbortFailedThreads.getWorkflows()) {
      //orderthread neu starten, falls nicht auf einen kindauftrag gewartet wird (f�r den fall: keine parallelit�t)
      List<FractalProcessStep<?>> activeSteps =
          processWithAbortFailedThreads.getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.SUBWF);
      boolean foundRunningChildOrder = activeSteps.size() > 0;

      //starte den auftrag auch neu, wenn der root parallelexecutor h�ngt
      if (!foundRunningChildOrder || processesWithAbortFailedThreads.rootParallelExecutorHangs(processWithAbortFailedThreads)) {
        cnt--; //f�hrt dazu, dass bei der ersten whileschleife viele retries passieren, bei folgenden wenigstens ein retry
        XynaOrderServerExtension xo = processWithAbortFailedThreads.getCorrelatedXynaOrder();

        //auftrag muss nicht durch den scheduler, weil er ja noch seine kapazit�ten hat.
        //man kann aber nicht einfach compensate aufrufen, weil dann das cleanup danach nicht passiert.
        //asynchron (in eigenem execution thread starten:

        //beim erneuten laufen funktioniert das abort wie beim resume. der thread sieht state ABORTING und wirft in den h�ngenden steps die exception.
        if (logger.isDebugEnabled()) {
          logger.debug("restarting thread for order " + xo);
        }
        int prio = xo.getPriority();
        
        DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
        xo.setDeploymentCounterMustBeCountDown();
        //auftragsthread muss runterz�hlen
        while (!XynaOrderExecutor.startOrder(xo, prio )) {
          if (++cnt >= threadStartRetries) {
            //wenigstens jeden der nicht kompensierten auftr�ge loggen
            logger.error("Giving up trying to compensate aborted order " + xo + " after " + threadStartRetries
                + " tries to start a thread.");
            
            //auftragsthread z�hlt nicht runter
            DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
            xo.setDeploymentCounterCountDownDone();
            break;
          }
          //im eigenen thread ausf�hren ist gef�hrlich. also warten, bis ein thread frei wird
          logger.warn("Could not start thread for compensation of aborted order " + xo
              + ". Trying again in 1s with higher priority.");
          prio++;
          if (prio > Thread.MAX_PRIORITY) {
            prio = Thread.MAX_PRIORITY;
          }
          Thread.yield();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
        }

      }
      
      //fall: mindestens eine verschachtelte parallelit�t h�ngt, weil ihr main thread h�ngt
      //bei verschachtelten parallelit�ten startet man das task bei der parent-parallelit�t
      for (FractalWorkflowParallelExecutor pe : processesWithAbortFailedThreads
          .getParallelExecutorsWithHangingTask(processWithAbortFailedThreads)) {
        for (ParallelTask task : processesWithAbortFailedThreads
            .getTasksForMainThreadOfChildExecutor(processWithAbortFailedThreads, pe)) {
          cnt--; //f�hrt dazu, dass bei der ersten whileschleife viele retries passieren, bei folgenden wenigstens ein retry
          if (logger.isDebugEnabled()) {
            logger.debug("restarting main thread of child parallel executor " + task + " in " + pe + " of order "
                + processWithAbortFailedThreads.getCorrelatedXynaOrder().getId());
          }
          while (!pe.startTaskWithNewThread(task)) {
            if (++cnt >= threadStartRetries) {
              //wenigstens jeden der nicht kompensierten auftr�ge loggen
              logger.error("Giving up trying to restart main thread of parallel executor after " + threadStartRetries
                  + " tries to start a thread.");
              break;
            }
            //im eigenen thread ausf�hren geht nicht. also warten, bis ein thread frei wird
            logger.warn("Could not start thread for hanging main thread of parallel executor " + task + ".");
            Thread.yield();
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
          }
        }
      }
    }
  }


  private int stopAbortFailedThreads(FailedThreadsInfo processesWithAbortFailedThreads) {
    //threads die vorher in die abortFailedThreads Liste aufgenommen wurden stoppen versuchen
    //dazu ben�tigt man hier eine liste der entsprechenden xynaprocesses
    int cntStopped = 0;
    for (XynaProcess xp : processesWithAbortFailedThreads.getWorkflows()) {
      Set<Thread> localCopy = xp.abortFailedThreads;
      if (localCopy != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("stopping threads of order " + getCorrelatedXynaOrder().getId() + ": " + localCopy);          
        }
        synchronized (localCopy) { //wegen dem zugriff in fractalprocessstep
          for (Thread t : localCopy) {
            logTraceHangingThread(t);
            t.stop();
            cntStopped++;
          }
        }
      }
    }
    return cntStopped;
  }


  private void findAndSaveHangingThreadAsAbortFailedThreadsRecursively(FailedThreadsInfo processesWithHangingThreads, boolean forAbort) {
    List<XynaOrderServerExtension> allChildOrders = getAllChildOrders(false, true);
    if (forAbort && state != XynaProcessState.ABORTING) {
      //�berpr�fen, dass man nicht steps gefunden hat, die bereits aus dem compensate stammen
      return;
    }
    //flag setzen, synchronisieren, so dass processsteps nicht mehr weiterlaufen k�nnen
    //in der synchronisierung die h�ngenden schritte ermitteln, damit diese danach ggfs kompensiert werden k�nnen
    findAndSaveHangingThreadAsAbortFailedThreadsLocally(processesWithHangingThreads, forAbort);
    //rekursion �ber kindauftr�ge
    for (XynaOrderServerExtension childOrder : allChildOrders) {
      if (childOrder.getExecutionProcessInstance() != null) {
        childOrder.getExecutionProcessInstance()
            .findAndSaveHangingThreadAsAbortFailedThreadsRecursively(processesWithHangingThreads, forAbort);
      }
    }
  }


  private void findAndSaveHangingThreadAsAbortFailedThreadsLocally(FailedThreadsInfo processesWithHangingThreads, boolean onlyIfAborting) {
    if (abortFailedThreads == null) {
      Set<Thread> l = new HashSet<Thread>();
      synchronized (l) {
        abortFailedThreads = l;
        addAbortFailedThreads(processesWithHangingThreads, onlyIfAborting);
      }
    } else {
      synchronized (abortFailedThreads) {
        addAbortFailedThreads(processesWithHangingThreads, onlyIfAborting);
      }
    }
  }
  
  /**
   * hierarchie von parallelit�ten im workflow
   */
  private class ParallelStepHierarchy {
    
    private List<ParallelStepHierarchy> children = new ArrayList<XynaProcess.ParallelStepHierarchy>();
    private final FractalProcessStep<?> step;

    /**
     * erstellt hierarchie aus der �bergebenen liste, wobei dieser knoten der rootknoten ist.
     */
    public ParallelStepHierarchy(List<FractalProcessStep<?>> activeParallelSteps) {
      Map<FractalProcessStep<?>, ParallelStepHierarchy> nodes = new HashMap<FractalProcessStep<?>, XynaProcess.ParallelStepHierarchy>();
      ParallelStepHierarchy root = null;
      while (activeParallelSteps.size() > 0) {
        FractalProcessStep<?> step = activeParallelSteps.remove(0);
        ParallelStepHierarchy psh = new ParallelStepHierarchy(step);
        nodes.put(step, psh);
        
        while (true) {
          
          FractalProcessStep<?> parentParallelStep = getParentParallelStep(step);
          if (parentParallelStep == null) {
            //root
            if (root != null && root != psh) {
              throw new RuntimeException("found different root than before: " + root + ", " + psh);
            }
            root = psh;            
            break;
          }

          //parent gefunden
          ParallelStepHierarchy parent;
          if (!activeParallelSteps.remove(parentParallelStep)) {
            //muss bereits aus liste entfernt worden sein.
            parent = nodes.get(parentParallelStep);
            if (parent == null) {
              throw new RuntimeException();
            }
            parent.addChild(psh);
            break;
          }

          parent = new ParallelStepHierarchy(parentParallelStep);
          nodes.put(parentParallelStep, parent);
          parent.addChild(psh);

          //n�chsten parent in liste suchen
          step = parentParallelStep;
          psh = parent;
        }
      }
      
      this.step = root.step;
      this.children = root.children;
    }

    private void addChild(ParallelStepHierarchy psh) {
      children.add(psh);      
    }

    private ParallelStepHierarchy(FractalProcessStep<?> step) {
      this.step = step;
    }

    public FractalWorkflowParallelExecutor getParallelExecutor() {
      return ((ParallelExecutionStep<?>)step).getFractalWorkflowParallelExecutor();
    }

    
    public List<ParallelStepHierarchy> getChildren() {
      return children;
    }
    
  }

  //im synchronized aufzurufen
  private void addAbortFailedThreads(FailedThreadsInfo fti, boolean onlyIfAborting) {
    Set<Thread> hangingThreads = getHangingThreadsLocally(onlyIfAborting);
    if (logger.isDebugEnabled()) {
      logger.debug("found the following threads not responding to interrupt for order " + getCorrelatedXynaOrder().getId() + ": " + hangingThreads);
    }
    
    List<FractalProcessStep<?>> activeParallelSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.PARALLEL);
    if (activeParallelSteps.size() > 0) {
      //h�ngende threads bei den zugeh�rigen parallelexecutoren melden, damit diese nicht ewig auf eine antwort warten!
      //das muss hier im synchronized passieren, damit nicht ein thread bereits wieder aus der abortFailedThread-liste
      //ausgetragen wird, wenn er zur�ckkommt. weil er dann einen ThreadDeath wirft, weiss der PE dann nicht, dass
      //da etwas auszutragen ist
      //das restart der threads passiert dann sp�ter
      
      ParallelStepHierarchy parallelTree = new ParallelStepHierarchy(activeParallelSteps);
      Thread hangingMainThread = fillFailedThreadsInfoRecursively(fti, parallelTree, hangingThreads);
      if (hangingMainThread != null) {
        fti.setRootParallelExecutorHangs(this);
      }
    }
    
    if (hangingThreads.size() > 0) {
      fti.add(this);
    }
    
    abortFailedThreads.addAll(hangingThreads);
    activeThreads.removeAll(abortFailedThreads);

    //die threads auch aus der liste der threads entfernen, die f�r die parallele schritt-ausf�hrung gemerkt wird
    if (threadsActivelyRunningInParallelTask != null) {
      for (Thread t : hangingThreads) {
        synchronized (threadsActivelyRunningInParallelTask) {
          Iterator<Entry<ParallelTask, Thread>> it = threadsActivelyRunningInParallelTask.entrySet().iterator();
          while (it.hasNext()) {
            Entry<ParallelTask, Thread> e = it.next();
            if (e.getValue() == t) {
              it.remove();
            }
          }
        }
      }
    }
  }


  /**
   * bestimmt die zuordnung der h�ngenden mainthreads zum jeweiligen parentparallelexecutor, damit diese neu gestartet werden k�nnen
   * @return h�ngenden mainthread der parallelit�t oder null, falls er nicht h�ngt
   */
  private Thread fillFailedThreadsInfoRecursively(FailedThreadsInfo fti, ParallelStepHierarchy parallelTreeNode,
                                                  Set<Thread> allHangingThreads) {
    Set<Thread> hangingChildMainThreads = new HashSet<Thread>();
    for (ParallelStepHierarchy childParallelTreeNode : parallelTreeNode.getChildren()) {
      Thread hangingChildMainThread = fillFailedThreadsInfoRecursively(fti, childParallelTreeNode, allHangingThreads);
      if (hangingChildMainThread != null) {
        hangingChildMainThreads.add(hangingChildMainThread);
      }
    }
    
    if (hangingChildMainThreads.size() > 0) {
      //h�ngende mainthreads von kind-PEs merken, f�r die m�ssen sp�ter threads neu gestartet werden
      fti.addHangingMainThreads(this, parallelTreeNode.getParallelExecutor(), hangingChildMainThreads);
    }

    //lokal h�ngenden thread ermitteln und zur�ckgeben

    Thread hangingMainThread = null;
    //bestimmung der teilmenge der h�ngenden threads, die in diesem parallel executor h�ngen
    Map<Thread, ParallelTask> hangingThreadsForThisParallelExecutor =
        getHangingThreadsForParallelExecutor(allHangingThreads, parallelTreeNode.getParallelExecutor());

    hangingMainThread =
        parallelTreeNode.getParallelExecutor().isMainThreadHanging(hangingThreadsForThisParallelExecutor.keySet());
    if (logger.isDebugEnabled()) {
      if (hangingMainThread != null) {
        logger.debug("found hanging main thread " + hangingMainThread + " of parallel executor "
            + parallelTreeNode.getParallelExecutor());
      } else {
        logger.debug("main thread of parallel executor " + parallelTreeNode.getParallelExecutor() + " is not hanging");
      }
    }


    return hangingMainThread;
  }


  private Map<Thread, ParallelTask> getHangingThreadsForParallelExecutor(Set<Thread> allHangingThreads,
                                                                         FractalWorkflowParallelExecutor pe) {
    Map<Thread, ParallelTask> hangingThreadsForThisParallelExecutor = new HashMap<Thread, ParallelTask>();
    if (threadsActivelyRunningInParallelTask != null) {
      for (Thread t : allHangingThreads) {
        for (Entry<ParallelTask, Thread> e : threadsActivelyRunningInParallelTask.entrySet()) {
          if (e.getValue() == t) {
            ParallelTask pt = e.getKey();
            if (pe.hasStarted(pt)) {
              hangingThreadsForThisParallelExecutor.put(t, pt);
              break; //n�chster thread, weil ein thread kann nur ein task pro PE ausf�hren
            }
            //hier kein break, es kann noch weitere treffer geben, aber nur f�r andere parallelexecutoren 
          }
        }
      }
    }
    return hangingThreadsForThisParallelExecutor;
  }


  /**
   * sammelt alle zu diesem workflow geh�renden h�ngenden javacalls (bzw zugeh�rige threads).
   * ausserdem werden die error step handler des steps aufgerufen, falls der h�ngende schritt sich
   * innerhalb einer parallelit�t befindet, weil dann das paralleltask nie neu gestartet wird.
   */
  private Set<Thread> getHangingThreadsLocally(boolean onlyIfAborting) {
    Set<Thread> hangingThreads = new HashSet<Thread>();
    List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.JAVACALL);

    if (onlyIfAborting && state != XynaProcessState.ABORTING) {
      //�berpr�fen, dass man nicht steps gefunden hat, die bereits aus dem compensate stammen
      return hangingThreads;
    }
    for (FractalProcessStep<?> step : activeSteps) {
      if (getParentParallelStep(step) != null) {
        //audit details soll abortion exception zeigen
        try {
          step.handleError(getAbortionException());
        } catch (ProcessAbortedException e) {
          //ignore
        } catch (RuntimeException e) {
          //abort fortf�hren
          logger.warn("could not execute process step error handlers successfully for hanging step " + step
              + " in workflow " + this + " for order " + getCorrelatedXynaOrder() + ".", e);
        }
      }
      Thread hangingThread = getHangingThreadForStep(step, activeSteps.size());
      if (hangingThread != null) {
        hangingThreads.add(hangingThread);
      }
    }
    return hangingThreads;
  }


  private boolean interruptHangingThreadsRecursively() {
    List<XynaOrderServerExtension> allChildOrders = getAllChildOrders(false, true);
    if (state != XynaProcessState.ABORTING) {
      //�berpr�fen, dass man nicht steps gefunden hat, die bereits aus dem compensate stammen
      return false;
    }
    boolean interrupted = interruptHangingThreadsLocally();
    
    //rekursion
    for (XynaOrderServerExtension childOrder : allChildOrders) {
      if (childOrder.getExecutionProcessInstance() != null) {
        interrupted |= childOrder.getExecutionProcessInstance().interruptHangingThreadsRecursively();
      }
    }
    return interrupted;
  }


  private Thread getHangingThreadForStep(FractalProcessStep<?> step, int size) {
    Thread hangingThread = null;
    if (threadsActivelyRunningInParallelTaskIsEmpty()) {
      //interrupt/kill f�r "keine parallelit�t vorhanden"          
      if (logger.isDebugEnabled()) {
        logger.debug("Step " + step + " is not running in parallel.");
      }

      synchronized (activeThreads) {
        if (activeThreads.size() == 0) {
          //thread zu activeStep wurde oder hat sich bereits beendet          
        } else if (activeThreads.size() == 1) {
          if (size == 1) {
            hangingThread = activeThreads.get(0);
          } else {
            logger.warn("No parallel execution detected but found more than one active thread in workflow " + this
                + ". activeThreads.size=" + activeThreads.size() + " activeSteps.size=" + size);
          }
        } else {
          logger.warn("No parallel execution detected but found more than one active thread in workflow " + this
              + ". activeThreads.size=" + activeThreads.size() + " activeSteps.size=" + size);
          //sollte nicht vorkommen, weil threadmapping bei parallelit�t gesetzt wird und ansonsten nur ein thread aktiv ist
          //wenn es doch vorkommen sollte, ist es ein programmierfehler, dann wei� man nicht, wieviele threads man killen muss.
          //deshalb wird hier einfach nichts ausser der warnung gemacht.
        }
      }
    } else {
      //h�ngender javacall in einem parallelem thread. 
      if (logger.isDebugEnabled()) {
        logger.debug("step " + step + " is running in parallel");
      }
      synchronized (activeThreads) {
        hangingThread = findParallelExecutingThreadForStep(step);
        if (hangingThread == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("did not find active thread for step " + step + ".");
          }
        }
      }
    }
    return hangingThread;
  }


  private boolean interruptHangingThreadsLocally() {
    List<FractalProcessStep<?>> activeSteps = getCurrentExecutingSteps(RecursionType.NONE, ActiveStepType.JAVACALL);

    if (state != XynaProcessState.ABORTING) {
      //�berpr�fen, dass man nicht steps gefunden hat, die bereits aus dem compensate stammen
      return false;
    }
    boolean killed = false;
    for (FractalProcessStep<?> step : activeSteps) {
      if (step.isStepListeningOnServiceStepEvents()
          && step.getServiceStepEventSource().hasHandlerFor(AbortServiceStepEvent.class)) {
        //wurde bereits abgebrochen
      } else {
        //normaler step
        Thread hangingThread = getHangingThreadForStep(step, activeSteps.size());
        if (hangingThread != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("interrupting thread for order " + getCorrelatedXynaOrder().getId() + ": " + hangingThread);
            logTraceHangingThread(hangingThread);
          }
          killThread(hangingThread, false);
          killed = true;
        }
      }
    }
    return killed;
  }


  private void logTraceHangingThread(Thread hangingThread) {
    if (logger.isTraceEnabled()) {
      StringWriter sw = new StringWriter();
      Exception e = new Exception();
      e.setStackTrace(hangingThread.getStackTrace());
      e.printStackTrace(new PrintWriter(sw));
      logger.trace("stacktrace of hanging thread:", e);
    }
  }


  private boolean waitForAllStepsToBeAborted(long timeoutMs) {
    long tEnd = System.currentTimeMillis() + timeoutMs;
    while (true) {
      //warten, bis dieser step != ABORTING ist. dann sind n�mlich auch alle kinder nicht mehr ABORTING
      if (state != XynaProcessState.ABORTING) {
        return true;
      }
      if (System.currentTimeMillis() >= tEnd) {
        return false;
      }
      //FIXME nicht auf h�ngende compensation von kind-workflows warten, sondern nur auf workflows warten, die in state ABORTING sind
      //      das ist aber nicht trivial, weil manche kinder noch h�ngen k�nnen, andere in der compensation sein k�nnen 
      //      (und h�ngen und nicht abgebrochen werden sollen), etc
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }
    }
  }


  public AbortionResult abortRunningWF(boolean killJavaCalls, KillStuckProcessBean bean, AtomicBoolean alreadyAbortedBefore) {
    return abortRunningWFInternally(killJavaCalls, bean, 0, alreadyAbortedBefore);
  }


  public enum XynaProcessState {
    /**
     * workflow l�uft und f�ngt gleich an zu laufen.
     * evtl wurde er vorher mal abgebrochen und ist in der compensation.
     */
    RUNNING, 
    
    /**
     * workflow l�uft nicht mehr. evtl wird er (falls erfolgreich) sp�ter noch kompensiert, dann geht er nochmal auf RUNNING
     */
    FINISHED, 
    
    /**
     * workflow soll abgebrochen werden und l�uft
     */
    ABORTING, 
    
    /**
     * workflow ist suspendiert und soll nicht abgebrochen werden
     */
    SUSPENDED, 
    
    /**
     * workflow ist abgebrochen und suspendiert
     */
    SUSPENDED_AFTER_ABORTING;

    /**
     * workflow gerade am laufen, also nicht {@link #FINISHED}, nicht {@link #SUSPENDED} und nicht {@link #SUSPENDED_AFTER_ABORTING}
     */
    public boolean isExecuting() {
      return !(this == XynaProcessState.FINISHED || this == XynaProcessState.SUSPENDED || this == XynaProcessState.SUSPENDED_AFTER_ABORTING);
    }
  }


  public XynaProcessState getState() {
    return state;
  }

  public void setState(XynaProcessState state) {
    setState(state, false);
  }
  
  
  /**
   * folgende zustands�berg�nge werden umgebogen:<br>
   * ABORTING -&gt; SUSPENDED =&gt; SUSPENDED_AFTER_ABORTING<br>
   * ABORTING -&gt; RUNNING =&gt; bleibt auf ABORTING<br>
   * SUSPENDED_AFTER_ABORTING -&gt; RUNNING =&gt; ABORTING<br> 
   */
  public void setState(XynaProcessState newState, boolean overrideABORTING) {    
    synchronized (XynaProcessState.RUNNING) {
      if (this.state == newState) {
        return;
      }
      if ((this.state == XynaProcessState.ABORTING || this.state == XynaProcessState.SUSPENDED_AFTER_ABORTING)
          && newState == XynaProcessState.SUSPENDED) {
        //beim compensate wird evtl zweimal hintereinander die suspensionexception gefangen und setState(SUSPENDED) aufgerufen.
        newState = XynaProcessState.SUSPENDED_AFTER_ABORTING;
      } else if (newState == XynaProcessState.ABORTING) {
        if (abortionException == null) {
          abortionException = new ProcessAbortedException(getCorrelatedXynaOrder().getId());
        }
      } else if (!overrideABORTING && this.state == XynaProcessState.ABORTING && newState == XynaProcessState.RUNNING) {
        if (abortionException == null) {
          //sollte nicht der fall sein, schadet aber nichts
          abortionException = new ProcessAbortedException(getCorrelatedXynaOrder().getId());
        }
        //so lassen
        return;
      } else if (this.state == XynaProcessState.SUSPENDED_AFTER_ABORTING && newState == XynaProcessState.RUNNING) {
        if (abortionException == null) {
          abortionException = new ProcessAbortedException(getCorrelatedXynaOrder().getId());
        }
        newState = XynaProcessState.ABORTING;
      }
      if (logger.isTraceEnabled()) {
        logger.trace(this + ": new state = " + newState + ", old state = " + this.state);
      }
      this.state = newState;
    }
  }


  public boolean compareAndSetState(XynaProcessState oldState, XynaProcessState state) {
    synchronized (XynaProcessState.RUNNING) {
      if (this.state == oldState) {
        if (logger.isTraceEnabled()) {
          logger.trace(this + ": new state = " + state + ", old state = " + this.state);
        }
        this.state = state;
        return true;
      }
      return false;
    }
  }


  public String toString() {
    XynaOrderServerExtension xo = getCorrelatedXynaOrder();
    return "XynaProcess("+getOriginalName()+","+(xo != null ? xo.getId() : "unknown orderid")+","+state+")@"+System.identityHashCode(this);
  }


  public FractalProcessStep<?> getParentParallelStep(FractalProcessStep<?> step) {
    while (true) {
      FractalProcessStep<?> parentStep = step.getParentStep();
      if (parentStep == null) {
        return null;
      }
      if (parentStep instanceof ParallelExecutionStep) {
        return parentStep;
      }
      step = parentStep;
    }
  }
  
  /**
   * wieviel InputVars werden ben�tigt? Default ist 0 f�r bestehende Planning-Workflows
   */
  public int getNeededInputVarsCount() {
    return 0;
  }
  
  
  public boolean isGeneratedAsInvalid() {
    return false;
  }
  
  /**
   * RootProcessData wird f�r den XynaProcess der RootOrder neu angelegt und dann alle XynaProcess der 
   * Kind-Auftr�ge weitergereicht, so dass der ganze Auftragsbaum auf einen gemeinsamen RootProcessData
   * zugreifen kann.
   *
   */
  public static class RootProcessData {

    private volatile RootOrderSuspension suspension;
    private Throwable abortionCause; 
    
    /**
     * Vorl�ufige Anfrage, ob suspendiert werden soll. Genauer wird dies in
     * {@link #suspend(XynaProcess, FractalProcessStep, boolean)} untersucht. 
     * @return
     */
    public boolean shouldSuspend() {
      if( suspension != null ) {
        return suspension.shouldSuspend();
      } else {
        return false;
      }
    }

    /**
     * Pr�fen, ob supendiert werden soll, wenn ja: Werfen der ProcessSuspendedException
     * @param xynaProcess
     * @param step
     * @param afterFailure 
     * @throws ProcessSuspendedException
     */
    public void suspend(XynaProcess xynaProcess, FractalProcessStep<?> step, boolean afterFailure) throws ProcessSuspendedException {
      if( suspension != null ) {
        ResumeTarget resumeTarget = new ResumeTarget( xynaProcess.getCorrelatedXynaOrder().getRootOrder().getId(),
                                                      xynaProcess.getCorrelatedXynaOrder().getId(),
                                                      step.getLaneId() );
        ProcessSuspendedException pse = suspension.suspend( resumeTarget );
        if( pse != null ) {
          if( afterFailure ) {
            suspension.addFailedInterruptOrStop(resumeTarget); //TODO Fehler eintragen?
          }
          throw pse;
        }
      }
    }
    
    /**
     * Eintragen, dass XynaProcess nun fertig suspendiert ist
     * @param xynaProcess
     */
    public void setSuspended(XynaProcess xynaProcess) {
      if( suspension != null ) {
        suspension.setSuspended(xynaProcess.getCorrelatedXynaOrder().getId());
      }
    }

    /**
     * Ab nun werden Suspendierungen vorgenommen
     * @param suspension
     */
    public void setRootOrderSuspension(RootOrderSuspension suspension) {
      if( this.suspension != null ) {
        logger.warn("RootOrderSuspension is already set: "+this.suspension+" will be replaced by "+suspension );
      }
      this.suspension = suspension;
    }
    
    @Override
    public String toString() {
      return "RootProcessData("+suspension+")";
    }

    public void setAbortionCause(Throwable cause) {
      abortionCause = cause;
    }

    public Throwable getAbortionCause() {
      return abortionCause;
    }
    
  }


  protected static void throwExceptionOfMismatchingType(int position, Class<?> expected, Class<?> got)
      throws XynaException {
    if (expected.getName().equals(got.getName())) {
      throw new com.gip.xyna.xprc.exceptions.XPRC_InvalidInputParameterClassloader(position + "",
                                                                                   expected.getName(),
                                                                                   expected.getClassLoader().toString(),
                                                                                   got.getClassLoader().toString());
    } else {
      throw new com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE(position + "", expected.getName(),
                                                                               got.getName());
    }
  }


  public static void deploy(Class<? extends XynaProcess> cl) throws XDEV_UserDefinedDeploymentException, XDEV_CLASS_INSTANTIATION_PROBLEM {
    XynaProcess xp;
    try {
      xp = cl.getConstructor().newInstance();
    } catch (Exception e) {
      throw new XDEV_CLASS_INSTANTIATION_PROBLEM(cl.getName(), e);
    } catch (Error e) {
      throw new XDEV_CLASS_INSTANTIATION_PROBLEM(cl.getName(), e);
    }
    try {
      xp.onDeployment();
    } catch (XynaException e) {
      if (e instanceof XDEV_UserDefinedDeploymentException) {
        throw (XDEV_UserDefinedDeploymentException) e;
      } else {
        throw new XDEV_UserDefinedDeploymentException(cl.getName(), e);
      }
    }
  }
  
  public static void undeploy(Class<? extends XynaProcess> cl) throws XDEV_UserDefinedUnDeploymentException, XDEV_CLASS_INSTANTIATION_PROBLEM {
    XynaProcess xp;
    try {
      xp = cl.getConstructor().newInstance();
    } catch (Exception e) {
      throw new XDEV_CLASS_INSTANTIATION_PROBLEM(cl.getName(), e);
    }
    try {
      xp.onUndeployment();
    } catch (XynaException e) {
      if (e instanceof XDEV_UserDefinedUnDeploymentException) {
        throw (XDEV_UserDefinedUnDeploymentException) e;
      } else {
        throw new XDEV_UserDefinedUnDeploymentException(cl.getName(), e);
      }
    }
  }

  //derzeit nicht ben�tigt, vgl auskommentierter aufruf in XynaOrderServerExtension.removeOrderReferenceIfNotNeededForCompensation
  public boolean removeOrderReferenceIfNotNeededForCompensation(int[][] stepCoordinates, long childOrderId) {
    DefaultSubworkflowCall<?> step = findUniqueStep(this, stepCoordinates);
    if (step.compensationRecursive()) {
      return false;
    }
    return true;
  }

  
  /*
   * pfad:
   *       (<uniquestepid in scope>[<dynamicstepindex in childsteps>].)*<uniquestepid in scope>
   * stepCoordinate ist z.b.
   * enth�lt n 1-2elementige arrays. das erste element ist die stepid
   *                                 das zweite element gibt es nur, wenn das erste element auf einen processstepdynamicchildren zeigt
   *                                   und enth�lt dann den index in dessen childsteps-array
   */
  private DefaultSubworkflowCall<?> findUniqueStep(Scope scope, int[][] stepCoordinates) {
    for (int i = 0; i < stepCoordinates.length; i++) {
      int[] scopeCoord = stepCoordinates[i];
      Step[] allSteps = scope.getAllLocalSteps();
      Step s = null;
      for (Step st : allSteps) {
        if (((FractalProcessStep<?>) st).getN() == scopeCoord[0]) {
          s = st;
          break;
        }
      }
      if (scopeCoord.length > 1) {
        ProcessStepDynamicChildren<?> pd = (ProcessStepDynamicChildren<?>) s;
        scope = (ForEachScope) pd.getChildSteps().get(scopeCoord[1]);
      } else {
        return (DefaultSubworkflowCall<?>) s;
      }
    }
    throw new RuntimeException();
  }
  

}
