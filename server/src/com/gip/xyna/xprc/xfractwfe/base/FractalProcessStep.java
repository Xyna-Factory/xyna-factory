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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_ExceptionHandlingFailedException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.RetryException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor.WorkflowThreadDeath;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSourceImpl;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;

/**
 *
 * @param <T> parent scope type
 */
public abstract class FractalProcessStep<T extends Scope> implements Step, Serializable, IProcessStep<T> {

  private static final long serialVersionUID = -9035974834990277420L;
  private static final Logger logger = CentralFactoryLogging.getLogger(FractalProcessStep.class);
  
  //das ist die zahl, die der ausf�hrungsreihenfolge entspricht. 
  private int i;
  protected T parentScope;
  protected XynaProcess parentProcess;
  
  //erste dimension = von der processstep implementierung abh�ngige auswahl (zb bei parallel wird in
  //der implementierung aus unterschiedlichen threads f�r alle i-s getchildren(i) aufgerufen, bei 
  //choice hingegen nur f�r ein i). zweite dimension = sequentiell aufzurufende schritte f�r die 
  //auswahl der ersten dimension
  private FractalProcessStep<?>[][] children; 

  private volatile boolean hasExecutedSuccessfully = false;
  protected volatile boolean hasBegunCompensation = false; // used in the generated code
  protected volatile boolean hasBegunExecution = false; 
  private volatile boolean hasCompensatedSuccessfully = false;

  protected volatile boolean hasEvaluatedToCaughtXynaException = false;
  protected SerializableClassloadedException lastCaughtXynaExceptionContainer;
  
  // this is only exception information and not the full throwable to guarantee persistability
  private XynaExceptionInformation lastUnhandledThrowableInformation = null;

  protected volatile long retryCounter = 0;

  /**
   * aufr�umen aller felder, die bei verwendung der instanz f�r einen neuen auftrag zur�ckgesetzt werden m�ssen.
   * ACHTUNG: wird nicht f�r lazy erstellte steps, also z.b. steps innerhalb von for-eaches aufgerufen
   *          weil diese steps nicht wiederverwendet werden k�nnen.
   */
  protected void reinitialize() {
    hasBegunExecution = false;
    hasExecutedSuccessfully = false;
    hasEvaluatedToCaughtXynaException = false;
    lastCaughtXynaExceptionContainer = null;
    hasCompensatedSuccessfully = false;
    lastUnhandledThrowableInformation = null;
    hasBegunCompensation = false;
  }
  
  public void prepareForRetryRecursivly(boolean resetExecutionCounters) {
    if (!hasBegunExecution) {
      return;
    }
    reinitialize();
    if (resetExecutionCounters && this instanceof IRetryStep) {
      ((IRetryStep) this).resetExecutionsCounter();
    }
    if (children.length > 0) {
      int l = children.length;
      for (int i = 0; i < l; i++) {
        int li = children[i].length;
        for (int j = 0; j < li; j++) {
          children[i][j].prepareForRetryRecursivly(resetExecutionCounters);
        }
      }
    }
  }
  
  public long getRetryCounter() {
    return retryCounter;
  }

  public FractalProcessStep(int i) {   
    this.i = i;
  }

  public int getN() {
    return i;
  }

  public void init(T p) {
    this.parentScope = p;
    Scope scope = p;
    while (scope.getParentScope() != null) {
      scope = scope.getParentScope();
    }
    this.parentProcess = (XynaProcess)scope;
    
    //kinder ermitteln, damit diese nicht zur laufzeit ermittelt werden m�ssen.
    int l = getChildrenTypesLength();
    children = new FractalProcessStep[l][];
    for (int i = 0; i < l; i++) {
      children[i] = getChildren(i);
    }
  }


  protected void executeChildren(int i) throws XynaException {
    int l = children[i].length;
    for (int j = 0; j < l; j++) {
      FractalProcessStep<?> step = children[i][j];
      // check whether the step has already been executed before
      if (!step.hasExecutedSuccessfully) {
        step.execute();
      }
    }
  }


  public T getParentScope() {
    return (T) parentScope;
  }


  /**
   * f�r i in (0..getChildrenTypesLength()-1) != null, ansonsten null.
   */
  protected abstract FractalProcessStep<?>[] getChildren(int i); //generics nicht T, weil der step selbst eine eigene scope sein k�nnte und dann ist parentscope != parentscope von children

  protected abstract int getChildrenTypesLength();


  /**
   * These two methods are supposed to return pointers to the incoming/outgoing XynaObject
   * instances for this process step. These instances have to be cloned immediately in
   * order to remember the values since they may be altered later within the process.
   */
  public abstract GeneralXynaObject[] getCurrentIncomingValues();
  public abstract GeneralXynaObject[] getCurrentOutgoingValues();

  public String getProcessName() {
    return getProcess().getOriginalName();
  }

  public XynaExceptionInformation getCurrentUnhandledThrowable() {
    return lastUnhandledThrowableInformation;
  }


  private void setCurrentUnhandledThrowable(Throwable t) {
    lastUnhandledThrowableInformation =
        new XynaExceptionInformation(t, -1, GeneralXynaObject.XMLReferenceCache.getCacheObjectWithoutCaching(RevisionManagement.getRevisionByClass(getClass())));
  }

  public SerializableClassloadedException getLastCaughtXynaExceptionContainer() {
    return lastCaughtXynaExceptionContainer;
  }
  
  
  public XynaExceptionInformation getCaughtException() {
    if (hasEvaluatedToCaughtXynaException) {
      return new XynaExceptionInformation(lastCaughtXynaExceptionContainer.getThrowable(), -1, GeneralXynaObject.XMLReferenceCache.getCacheObjectWithoutCaching(RevisionManagement.getRevisionByClass(getClass())));
    } else {
      return null;
    }
  }

  /**
   * The internal process step id may be different from the one that is contained in the XML
   * representation. Each step remembers its XML id, though. 
   * @return xml id
   */
  public abstract Integer getXmlId();

  <E extends Exception> void handleError(E e) throws E {
    setCurrentUnhandledThrowable(e);
    try {
      if (!(e instanceof RetryException)) {
        parentProcess.errorHandler(this);
      }
    } catch (RuntimeException f) { //bei einem fehler nicht den urspr�nglichen fehler vergessen
      throw new RuntimeException(new Ex_ExceptionHandlingFailedException().initCauses(new Throwable[]{f, e}));
    } catch (Error t) {
      Department.handleThrowable(t);
      throw new RuntimeException(new Ex_ExceptionHandlingFailedException().initCauses(new Throwable[]{t, e}));
    }
    throw e;
  }


  boolean needsToBeExecutedEvenIfAborted() {
    // the abortion exception should be thrown as deep within the order stack as possible. if the
    // next step has begun its execution before and is not a java call it is e.g. a subworkflow call
    // or a parallel execution. In this case, the abortion exception needs to be thrown somewhere
    // down the stack.
    //-> das ergibt aussagekr�ftigere stacktraces in der abortionexception
    if (!hasBegunExecution) {
      return false;
    }
    if (this instanceof JavaCall) {
      JavaCall jc = (JavaCall)this;
      List<XynaOrderServerExtension> childOrders = jc.getChildOrders();
      if (childOrders != null) {
        for (XynaOrderServerExtension xo : childOrders) {
          if (xo.getExecutionProcessInstance() != null) {
            if (xo.getExecutionProcessInstance().getState() != XynaProcessState.FINISHED) {
              //es gibt einen laufenden subwf
              return true;
            }
          }
        }
      }
      return false;
    }
    return true;
  }


  public final void execute() throws XynaException {
    if (!hasBegunExecution) {
      hasBegunExecution = true;
      retryCounter = parentProcess.retryCounter.get();
      parentProcess.preHandler(this);
    }

    boolean abort = parentProcess.isProcessAborted();

    if (abort && !needsToBeExecutedEvenIfAborted()) {
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      this.<ProcessAbortedException> handleError(new ProcessAbortedException(parentProcess.getAbortionException()));
    }
    boolean exceptionCanBeCausedFromInterruptOrThreadstop = true;
    try {
      parentProcess.checkSuspendingAndThrowProcessSuspendedException(this,false);
      executeInternally();
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      exceptionCanBeCausedFromInterruptOrThreadstop = false;
    } catch (ProcessSuspendedException e) {
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      exceptionCanBeCausedFromInterruptOrThreadstop = false;
      throw e;
    } catch (ProcessAbortedException e) {
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      exceptionCanBeCausedFromInterruptOrThreadstop = false;
      this.<ProcessAbortedException>handleError(e);
    } catch (OrderDeathException e) {
      exceptionCanBeCausedFromInterruptOrThreadstop = false;
      throw e;
    } catch (XynaException e) {
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      exceptionCanBeCausedFromInterruptOrThreadstop = true;
      this.<XynaException>handleError(e);
    } catch (RuntimeException t) {
      parentProcess.getCorrelatedXynaOrder().setHasBeenBackuppedAfterChange(false);
      exceptionCanBeCausedFromInterruptOrThreadstop = true;
      this.<RuntimeException> handleError(t);
    } catch( WorkflowThreadDeath wtd ) {
      exceptionCanBeCausedFromInterruptOrThreadstop = false;
      throw wtd;
    } finally {
      //zuerst Abortion pr�fen, damit diese eine Suspendierung �berholen kann
      if (parentProcess.abortFailedThreads != null) {
        //wirft evtl. WorkflowThreadDeath und ersetzt damit geworfene Exception
        checkProcessShouldAbort();
      }
      if( exceptionCanBeCausedFromInterruptOrThreadstop ) {
        //wirft evtl. ProcessSuspendedException und ersetzt damit geworfene Exception
        try {
          parentProcess.checkSuspendingAndThrowProcessSuspendedException(this,true); //TODO Throwable �bergeben?
        } catch (ProcessSuspendedException e) {
          lastUnhandledThrowableInformation = null; //hasError -> false. damit der step nicht als "fertig gelaufen" festgestellt wird
          throw e;
        }
      }
    }

    hasExecutedSuccessfully = true;

    parentProcess.postHandler(this);

    if (parentProcess.isAttemptingSuspension()) {
      // FIXME das ist nicht sicher. bessere l�sung: beim setzen des attemptingSuspension-Flags gleich
      //       den SuspensionCause mitsetzen
      throw SuspendResumeManagement.suspendManualOrShutDown(null,null);
    }
    
    abort = parentProcess.isProcessAborted();
    
    if (abort) {
      throw parentProcess.getAbortionException();
    }
    
  }
  
  /**
   * checken, ob der aktuelle thread abgebrochen wurde und versterben soll
   */
  private void checkProcessShouldAbort() {
    Set<Thread> localCopy = parentProcess.abortFailedThreads; //threadsicherheit: kopie erstellen, die nicht null werden kann
    if (localCopy != null) {
      synchronized (localCopy) {
        Thread t = Thread.currentThread();
        if (localCopy.contains(t)) {
          if (logger.isInfoEnabled()) {
            logger.info("thread " + t + " was aborted earlier and will now shutdown.");
          }
          localCopy.remove(t);
          if (localCopy.size() == 0) {
            if (logger.isDebugEnabled()) {
              logger.debug("abortFailedThreads size = 0");
            }
            parentProcess.abortFailedThreads = null;
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("abortFailedThreads size = " + localCopy.size());
            }
          }
          throw new WorkflowThreadDeath();
        }
      }
    }
  }

  protected void compensateChildren() throws XynaException {
    int l = children.length;
    for (int i = l - 1; i >= 0; i--) {
      int li = children[i].length;
      for (int j = li - 1; j >= 0; j--) {
        children[i][j].compensate();
      }
    }
  }  

  public void compensate() throws XynaException {
    if (hasCompensatedSuccessfully) {
      return;
    }
    
    boolean compensate = false;
    if ( this instanceof ParallelExecutionStep ) {
      compensate = hasBegunExecution; //Compensation immer vornehmen, da evtl. nicht alle Lanes fehlgeschlagen sind
    } else {
      compensateChildren();
      //Compensation nur vornehmen, wenn dieser Step auch ausgef�hrt wurde
      compensate = hasExecutedSuccessfully && !hasEvaluatedToCaughtXynaException;
    }
    
    if (! compensate) {
      hasCompensatedSuccessfully = true; //nicht nochmal alle kinder checken m�ssen!
      return; //Step ist fehlgeschlagen, daher keine Compensation 
    }
    
    if (!hasBegunCompensation) {
      hasBegunCompensation = true;
      //erster aufruf, nicht beim resume.
      // compensate handlers
      parentProcess.compensatePreHandler(this);
    }
    compensateInternally(); // modelliert
    
    hasCompensatedSuccessfully = true; //nicht nochmal alle kinder checken m�ssen!

    //dieser code wird nur einmal ausgef�hrt, weil hasCompensatedSuccessfully = true.
    // compensate handlers
    parentProcess.compensatePostHandler(this);

    if (parentProcess.isAttemptingSuspension()) {
      throw SuspendResumeManagement.suspendManualOrShutDown(null,null);
    }
 
  }
  

  public boolean hasExecutedSuccessfully() {
    return hasExecutedSuccessfully;
  }
  
  public void setSuccessfullExecution(boolean value) {
    hasExecutedSuccessfully = value;
  }

  public abstract void executeInternally() throws XynaException;

  public abstract void compensateInternally() throws XynaException;

  private static final FractalProcessStep NULLSTEP = new FractalProcessStep(-1) {

    @Override
    protected FractalProcessStep[] getChildren(int i) {
      return null;
    }

    @Override
    protected int getChildrenTypesLength() {
      return 0;
    }

    @Override
    public GeneralXynaObject[] getCurrentIncomingValues() {
      return null;
    }

    @Override
    public GeneralXynaObject[] getCurrentOutgoingValues() {
      return null;
    }

    @Override
    public Integer getXmlId() {
      return null;
    }

    @Override
    public void executeInternally() throws XynaException {
    }

    @Override
    public void compensateInternally() throws XynaException {
    }

  };

  //TODO man sollte dar�ber nachdenken, den parent immer im konstruktor zu �bergeben, weil das immer noch teuer ist
  //wenn man gro�e foreaches hat. Konstruktor ist aufw�ndig, kann nur einmal gemacht werden. Wegen transient muss
  //ParentStep bei jeder Ausf�hrung gesetzt werden.
  private transient FractalProcessStep<?> parentStep = NULLSTEP; //cachen weil teuer. null ist ein g�ltiger wert

  /**
   * Setzen des ParentStep
   * @param parentStep
   */
  public void setParentStep(FractalProcessStep<?> parentStep) {
    this.parentStep = parentStep;
  }
  
  public final FractalProcessStep<?> getParentStep() {
    if (parentStep != NULLSTEP) {
      return parentStep;
    }
    //FIXME diese lazy-Ermittlung des parentStep ist sehr aufw�ndig und funktioniert nicht f�r die 
    //Kinder eines ForEach-Steps. F�r diese wurde nun setParentStep eingef�hrt. 
    //Da diese Ermittlung hier so aufw�ndig ist, sollte sie ausgebaut werden, nachdem alle StepGeneratoren 
    //das Setzen des ParentSteps durchf�hren.
    
    T parentScope = getParentScope();
    FractalProcessStep<?>[] allSteps = parentScope.getAllSteps();
    for (FractalProcessStep<?> possibleParent : allSteps) {
      for (int i = 0; i < possibleParent.getChildrenTypesLength(); i++) {
        for (FractalProcessStep<?> possiblyEqualToThis : possibleParent.getChildren(i)) {
          if (this == possiblyEqualToThis) {
            parentStep = possibleParent;
            return possibleParent;
          }
        }
      }
    }
    //ScopeStep selbst
    if (parentScope instanceof FractalProcessStep) {
      FractalProcessStep<?> parentScopeStep = (FractalProcessStep<?>) parentScope;
      for (int i = 0; i < parentScopeStep.getChildrenTypesLength(); i++) {
        for (FractalProcessStep<?> possiblyEqualToThis : parentScopeStep.getChildren(i)) {
          if (this == possiblyEqualToThis) {
            parentStep = parentScopeStep;
            return parentScopeStep;
          }
        }
      }
    }
    parentStep = null;
    return null;
  }


  public XynaProcess getProcess() {
    return parentProcess;
  }


  public boolean hasError() {
    return lastUnhandledThrowableInformation != null;
  }


  public boolean hasBegunExecution() {
    return hasBegunExecution;
  }

  private static final Integer[][] EMPTY = new Integer[][]{new Integer[0], new Integer[0]};

  public static Integer[][] calculateForEachIndices(FractalProcessStep<?> step) {

    if (step == null) {
      return EMPTY; //TODO der fall scheint nie vorzukommen.
    }

    List<Integer> ids = null;
    List<Integer> parallel = null;

    Scope parentStep = step instanceof Scope ? (Scope) step : step.getParentScope();
    while (parentStep != null) {
      if (parentStep instanceof ForEachScope) {
        if (ids == null) {
          ids = new LinkedList<Integer>();
          parallel = new LinkedList<Integer>();
        }
        ids.add(0, ((ForEachScope) parentStep).getForEachIndex());
        parallel.add(0, ((FractalProcessStep<?>) parentStep).getParentStep() instanceof ParallelExecutionStep ? 1 : 0);
      }
      parentStep = parentStep.getParentScope();
    }

    if (ids == null) {
      return EMPTY;
    } else {
      return new Integer[][] {ids.toArray(new Integer[ids.size()]), parallel.toArray(new Integer[parallel.size()])};
    }

  }
  
  
  protected transient volatile ServiceStepEventSourceImpl stepEventSource;

  protected void initEventSource() {
    stepEventSource = new ServiceStepEventSourceImpl(parentProcess.getCorrelatedXynaOrder()); //this one is for dispatching
    ServiceStepEventHandling.serviceStepEventSource.set(stepEventSource); //that is for aquiring it via Impl
  }
  
  
  protected void clearEventSource() {
    stepEventSource = null;
    ServiceStepEventHandling.serviceStepEventSource.set(null);
  }
  
  public boolean isStepListeningOnServiceStepEvents() {
    return stepEventSource != null;
  }
  
  public ServiceStepEventSourceImpl getServiceStepEventSource() {
    return stepEventSource;
  }
  
  
  
  private static FractalProcessStep<?> getSubworkflowInvocation(long orderId, XynaProcess callingProcess) {
    if (callingProcess == null) {
      return null;
    }
    FractalProcessStep<?>[] allSteps = callingProcess.getAllSteps();
    for (FractalProcessStep<?> fractalProcessStep : allSteps) {
      if (!fractalProcessStep.hasBegunExecution()) {
        continue;
      }
      if (fractalProcessStep instanceof SubworkflowCall) {
        XynaOrderServerExtension xose = ((SubworkflowCall) fractalProcessStep).getChildOrder();
        if (xose.getId() == orderId) {
          return fractalProcessStep;
        }
      }

      if (fractalProcessStep instanceof JavaCall) {
        List<XynaOrderServerExtension> childOrders = ((JavaCall)fractalProcessStep).getChildOrders();
        if (childOrders != null) {
          for (XynaOrderServerExtension xose : childOrders) {
            if (xose.getId() == orderId) {
              return fractalProcessStep;
            }
          }
        }
      }
    }
    return null;
  }
  
  
  public <M extends FractalProcessStepMarkerInterface> M findMarkedProcessStepInExecutionStack(Class<M> markerInterface, FractalProcessStepFilter<M> filter) {
    M step = findMarkedProcessStepInLocalExecutionStack(markerInterface, filter);
    if (step == null) {
      long currentOrderId = getProcess().getCorrelatedXynaOrder().getId();
      XynaOrderServerExtension parentOrder = getProcess().getCorrelatedXynaOrder().getParentOrder();
      while (parentOrder != null) {
        FractalProcessStep<?> localSubInvocation = getSubworkflowInvocation(currentOrderId, parentOrder.getExecutionProcessInstance());
        if (localSubInvocation != null) {
          step = localSubInvocation.findMarkedProcessStepInLocalExecutionStack(markerInterface, filter);
        }
        if (step != null) {
          return step;
        } else {
          currentOrderId = parentOrder.getId();
          parentOrder = parentOrder.getParentOrder();
        }
      }
    }
    return step;
  }
  
  private <M extends FractalProcessStepMarkerInterface> M findMarkedProcessStepInLocalExecutionStack(Class<M> markerInterface, FractalProcessStepFilter<M> filter) {
    FractalProcessStep<?> parentStep = getParentStep();
    int localStepN = getN();
    M mostRecentHit = null;
    ancestry: while (parentStep != null) {
      for (int i =0; i<parentStep.getChildrenTypesLength(); i++) {
        FractalProcessStep<?>[] stepsInLane =  parentStep.getChildren(i);
        for (FractalProcessStep<?> fractalProcessStep : stepsInLane) {
          if (markerInterface.isInstance(fractalProcessStep) && filter.matches((M) fractalProcessStep)) {
            mostRecentHit = (M) fractalProcessStep;
          }
          if (fractalProcessStep.getN() == localStepN) {
            if (mostRecentHit != null) {
              return mostRecentHit;
            } else {
              localStepN = parentStep.getN();
              Scope parentScopeOfParentStep = parentStep.getParentScope();
              parentStep = parentStep.getParentStep();
              if (parentStep == null && parentScopeOfParentStep != null) {
                if (parentScopeOfParentStep instanceof FractalProcessStep) {
                  localStepN = ((FractalProcessStep)parentScopeOfParentStep).getN();
                  parentStep = ((FractalProcessStep)parentScopeOfParentStep).getParentStep();
                }
              } else if (parentStep == null && parentScopeOfParentStep == null) {
                //nicht in workflow gefunden
                break ancestry;
              }
              
              continue ancestry;
            }
          }
        }
      }
      
      // dieser fall tritt ein, wenn die aktuelle step-id in keinen der kindern des parents gefunden wurde
      // das sollte eigtl nie passieren. falls doch, hangelt man sich trotzdem zum parent weiter.
      localStepN = parentStep.getN();
      Scope parentScopeOfParentStep = parentStep.getParentScope();
      parentStep = parentStep.getParentStep();
      if (parentStep == null && parentScopeOfParentStep != null) {
        if (parentScopeOfParentStep instanceof FractalProcessStep) {
          localStepN = ((FractalProcessStep)parentScopeOfParentStep).getN();
          parentStep = ((FractalProcessStep)parentScopeOfParentStep).getParentStep();
        }
      }
    }
    // not found in this process
    return null;
  }
  
  
  public static interface FractalProcessStepFilter<M extends FractalProcessStepMarkerInterface> {
    
    public boolean matches(M step);
    
  }
  
  
  public static interface FractalProcessStepMarkerInterface { }
  
  /**
   * parentStep ist transient, muss aber mit NULLSTEP vorbelegt sein.
   * Methode darf nicht private sein, da nur Tochterklassen deserialisiert werden
   * @return
   */
  protected Object readResolve() {
    parentStep = NULLSTEP;
    return this;
  }
  
  
  private String laneId;
  public String getLaneId() { //getLaneIdParts zusammengesetzt
    FractalProcessStep<?> parent = getParentStep();
    
    if (parent == null) {
      return laneId;
    }
    String recursiveValue = parent.getLaneId();
    if(recursiveValue == null ) {
      return laneId;
    } else {
      if( laneId == null ) {
        return recursiveValue;
      } else {
        return laneId+","+recursiveValue;
      }
    }
  }
  
  public String getLabel() {
    return null;
  }

  public int[][] getCoordinates() {
    List<int[]> coords = new ArrayList<>();
    coords.add(new int[] {getN()});

    FractalProcessStep<?> step = this;
    while (!(step.getParentScope() instanceof XynaProcess)) {
      Scope s = step.getParentScope();
      if (!(s instanceof ForEachScope)) {
        throw new RuntimeException(); //keine anderen scopes unterst�tzt
      }
      ForEachScope fes = (ForEachScope) s;
      int idx = fes.getForEachIndex();
      step = ((FractalProcessStep<?>) s).getParentStep();
      coords.add(0, new int[] {step.getN(), idx});
    }
    return coords.toArray(new int[0][]);
  }

  public void setLaneId(String laneId) { //eigtl besser setLaneIdPart
    this.laneId = laneId;
  }

  /**
   * eindeutige id in diesem workflow. ber�cksichtigt
   * - step-id
   * - retries
   * - foreach
   */
  protected String getUniqueId() {
    //stepid +  retry-index + foreachindexes +
    StringBuilder ret = new StringBuilder();
    ret.append(i);
    ret.append(".").append(getRetryCounter());
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(this)[0];
    if (forEachIndices != null) {
      for (Integer i : forEachIndices) {
        ret.append(".").append(i);
      }
    }
    return ret.toString();
  }


  private static final XynaPropertyBoolean inheritParentRuntimeContext =
      new XynaPropertyBoolean("xprc.xfractwfe.workflow.childorder.context.inheritfromparent", false)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "If set to true, a childorder will inherit its runtime context from its parent order instead of its order type.");


  //wird aus dem generierten code aus (StepFunction) aufgerufen
  protected long getRevisionForOrderType(DestinationKey dk) {
    long revision = ((ClassLoaderBase) getClass().getClassLoader()).getRevision();
    if (inheritParentRuntimeContext.get()) {
      return revision;
    }
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (dk.getApplicationName() == null
        && (dk.getWorkspaceName() == null || dk.getRuntimeContext() == RevisionManagement.DEFAULT_WORKSPACE)) {
      try {
        dk = new DestinationKey(dk.getOrderType(), rm.getRuntimeContext(revision));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }

    //die revision in dem destinationkey, zu dem der destinationvalue im dispatcher urspr�nglich gespeichert wurde, ist die gesuchte
    try {
      revision =
          rm.getRevision(XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
              .getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(dk));
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      //ignore, dann nimmt man halt die revision von dem step
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    return revision;
  }


  protected boolean isWorkflowCall(GeneralXynaObject gxo, String operationName) {
    return isWorkflowCallByClass(gxo.getClass(), operationName);
  }

  
  private static final XynaPropertyBoolean allowWorkflowImplementationOfServerInternalObjects = new XynaPropertyBoolean("xprc.xfractwfe.base.internalobjects.workflowimplementation.allow", false);

  private boolean isWorkflowCallByClass(Class<? extends GeneralXynaObject> clazz, String operationName) {
    long rev = RevisionManagement.getRevisionByClass(clazz);
    if (rev == Integer.MAX_VALUE) {
      if (allowWorkflowImplementationOfServerInternalObjects.get()) {
        //falls das sp�ter mal erlaubt werden soll, kann man das wohl so machen:
        String fqXmlName = clazz.getAnnotation(XynaObjectAnnotation.class).fqXmlName(); 
        XynaOrderServerExtension xose = getProcess().getCorrelatedXynaOrder();
        Long rev2 = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObject(fqXmlName, xose.getRevision());
        if (rev2 == null) {
          return false;
        }
        rev = rev2;
      } else {
        //serverinterne objekte haben keine workflow-implementierung
        return false;
      }
    }
    ConcurrentMap<String, ConcurrentMap<String, Boolean>> concurrentMap = XynaProcess.instanceMethodTypes.get(rev);
    if (concurrentMap != null) {
      ConcurrentMap<String, Boolean> m = concurrentMap.get(clazz.getName());
      if (m != null) {
        Boolean b = m.get(operationName);
        if (b != null) {
          return b;
        }
      }
    }
    Class<?> superclass = clazz.getSuperclass();
    if (GeneralXynaObject.class.isAssignableFrom(superclass)) {
      return isWorkflowCallByClass((Class<? extends GeneralXynaObject>) superclass, operationName);
    }
    return false; //eigtl ein bug, wenn man hier hin kommt.
  }
}
