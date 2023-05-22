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

package com.gip.xyna.xact.filter.monitor;



import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.StepMap.RecursiveVisitor;
import com.gip.xyna.xact.filter.util.QueryUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction;
import com.gip.xyna.xprc.xfractwfe.generation.Distinction.BranchInfo;
import com.gip.xyna.xprc.xfractwfe.generation.ErrorInfo;
import com.gip.xyna.xprc.xfractwfe.generation.Parameter;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepParallel;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;

import xmcp.processmonitor.datatypes.BranchRuntimeInfo;
import xmcp.processmonitor.datatypes.CatchBranchRuntimeInfo;
import xmcp.processmonitor.datatypes.ChoiceBranchRuntimeInfo;
import xmcp.processmonitor.datatypes.ChoiceRuntimeInfo;
import xmcp.processmonitor.datatypes.Error;
import xmcp.processmonitor.datatypes.ForeachIterationContainer;
import xmcp.processmonitor.datatypes.ForeachRuntimeInfo;
import xmcp.processmonitor.datatypes.IterationContainer;
import xmcp.processmonitor.datatypes.IterationEntry;
import xmcp.processmonitor.datatypes.IterationRuntimeInfo;
import xmcp.processmonitor.datatypes.MappingRuntimeInfo;
import xmcp.processmonitor.datatypes.NoAuditData;
import xmcp.processmonitor.datatypes.ParallelExecutionRuntimeInfo;
import xmcp.processmonitor.datatypes.ParallelismBranchRuntimeInfo;
import xmcp.processmonitor.datatypes.QueryRuntimeInfo;
import xmcp.processmonitor.datatypes.RetryIterationContainer;
import xmcp.processmonitor.datatypes.RetryRuntimeInfo;
import xmcp.processmonitor.datatypes.RunningTime;
import xmcp.processmonitor.datatypes.RuntimeInfo;
import xmcp.processmonitor.datatypes.ServiceRuntimeInfo;
import xmcp.processmonitor.datatypes.StepRuntimeInfo;
import xmcp.processmonitor.datatypes.TemplateRuntimeInfo;
import xmcp.processmonitor.datatypes.ThrowRuntimeInfo;
import xmcp.processmonitor.datatypes.WorkflowRuntimeInfo;

import com.gip.xyna.xact.filter.monitor.ProcessMonitorServicesServiceOperationImpl.OrderInstanceGuiStatus;



public class StepVisitorForWorkflowRuntimeInfo extends RecursiveVisitor {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(StepVisitorForWorkflowRuntimeInfo.class);
  private static XynaMultiChannelPortal multiChannelPortal =
      ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal());

  private boolean isImported;
  private RuntimeContext rootRTC;
  private List<RuntimeInfo> steps;
  private ProcessMonitorServicesServiceOperationImpl impl;


  public StepVisitorForWorkflowRuntimeInfo(MonitorAudit monitorAudit) {
    isImported = monitorAudit.isImported();
    rootRTC = monitorAudit.getRuntimeContext();
    impl = new ProcessMonitorServicesServiceOperationImpl();
    try {
      impl.getAudit(monitorAudit);
    } catch (NoAuditData e) {
      impl = null;
    } //fill maps //TODO: optimize away

    steps = new LinkedList<RuntimeInfo>();
  }


  public List<RuntimeInfo> getResult() {
    steps.removeIf(Objects::isNull); //remove null entries => created by steps that were never executed.
    return steps;
  }


  @Override
  public void visit(Step step) {
  }


  @Override
  public void visitStepThrow(StepThrow step) {
    RuntimeInfo info = createRuntimeInfo(step, this::createThrowRuntimeInfoForParameter);
    steps.add(info);
    super.visitStepThrow(step);
  }


  @Override
  public void visitStepParallel(StepParallel step) {
    RuntimeInfo info = createParallelExecutionRuntimeInfoForParameter(step);
    steps.add(info);
    super.visitStepParallel(step);
  }


  @Override
  public void visitStepSerial(StepSerial step) {
    Step parentStep = step.getParentStep();
    RuntimeInfo info = null;
    int index = -1;
    ObjectId stepId = null;
    if (parentStep instanceof Distinction) {
      info = createRuntimeInfo(step, this::createDistinctionBranchRuntimeInfoForParameter);
      if(parentStep instanceof StepCatch) {
        if (((StepCatch) parentStep).getStepInTryBlock() == step.getParentWFObject().getWfAsStep().getChildStep()) {
          stepId = ObjectId.createStepId(step.getParentWFObject().getWfAsStep());
        } else {
          stepId = ObjectId.createStepId(((StepCatch) parentStep).getStepInTryBlock());
        }
        index = parentStep.getChildSteps().indexOf(step) - 1; //-1 for try-block
      } else if(parentStep instanceof StepChoice) {
        StepChoice sc = ((StepChoice)parentStep);
        int branchNo = sc.getBranchNo(step);
        stepId = ObjectId.createStepId(step.getParentStep());
        index = branchNo;
      }
    } else if (parentStep instanceof StepParallel) {
      if(step.getChildSteps() != null && step.getChildSteps().size() > 0) {
        BiFunction<Step, Parameter, RuntimeInfo> func = createParallelismBranchRuntimeInfoForParameterWrapper(step);
        Step stepProvidingParameters = findParallelismParentWithParameters(step);
        info = createRuntimeInfo(stepProvidingParameters, func);  
        index = step.getParentStep().getChildSteps().indexOf(step);
      } else {
        //we do not have parameters for lanes of StepParallel without childSteps
        return;
      }
      stepId = ObjectId.createStepId(step.getParentStep());
    } else {
      return;
    }
    
    //update id in interationContainers -> it was set to first child of stepSerial
    
    
    String newId = ObjectId.createBranchId(stepId.getBaseId(), String.valueOf(index));
    updateIdInRuntimeInfo(info, newId);
    steps.add(info); 

    super.visitStepSerial(step);
  }


  private Step findParallelismParentWithParameters(Step step) {
    while(step instanceof StepSerial || step instanceof ScopeStep && !(step instanceof WFStep)) {
      step = step.getParentStep();
    }
    
    return step;
  }


  @Override
  public void visitStepForeach(StepForeach step) {
    RuntimeInfo info = createForeachRuntimeInfo(step);
    steps.add(info);

    super.visitStepForeach(step);
    info = createIterationRuntimeInfo(step.getChildScope().getChildStep());
    steps.add(info);
  }


  @Override
  public void visitStepChoice(StepChoice step) {
    RuntimeInfo info = createRuntimeInfo(step, this::createChoiceRuntimeInfoForParameter);
    steps.add(info);
    super.visitStepChoice(step);
  }


  @Override
  public void visitStepRetry(StepRetry step) {
    RuntimeInfo info = createRuntimeInfo(step, this::createRetryRuntimeInfoForParameter);
    steps.add(info);
    super.visitStepRetry(step);
  }


  @Override
  public void visitStepFunction(StepFunction step) {
    RuntimeInfo info;
    if(isQuery(step)) {
      info = createRuntimeInfo(step, this::createQueryRuntimeInfoForParameter);
    } else {
      info = createRuntimeInfo(step, this::createServiceRuntimeInfoForParameter);
    }
    steps.add(info);
    super.visitStepFunction(step);
  }


  @Override
  public void visitScopeStep(ScopeStep step) {
    if (!(step instanceof WFStep)) {
      super.visitScopeStep(step);
      return;
    }

    RuntimeInfo info = createRuntimeInfo((WFStep) step, this::createWorkflowRuntimeInfoForParameter);
    steps.add(info);

    super.visitScopeStep(step);
  }


  @Override
  public void visitStepMapping(StepMapping step) {

    RuntimeInfo info;
    if (step.isTemplateMapping()) {
      info = createRuntimeInfo(step, this::createTemplateRuntimeInfoForParameter);
    } else if (step.isConditionMapping()) { // Query
      return;
    } else {
      info = createRuntimeInfo(step, this::createMappingRuntimeInfoForParameter);
    }
    steps.add(info);
    super.visitStepMapping(step);
  }


  @Override
  public boolean beforeRecursion(Step parent, Collection<Step> children) {
    return false;
  }


  private <T extends Step> RuntimeInfo createRuntimeInfo(T step, BiFunction<T, Parameter, RuntimeInfo> creator) {
    RuntimeInfo result = null;

    List<Parameter> parameters = step.getParameterList();
    if (parameters == null || parameters.size() == 0) {
      //step was not executed. return null.
      return null;
    }
    Parameter p = step.getFirstParameter();
    List<Integer> loopIndices = p.getLoopIndices();
    if ( (loopIndices == null || loopIndices.size() == 0) && // no loops
         (!step.isInRetryLoop()) ) { //no retries
      //step was executed one time.
      //return a StepRuntimeInfo object
      result = creator.apply(step, p);
    } else {
      //step was executed multiple times.
      IterationContainer iterationContainer = createIterations(step, creator);
      result = iterationContainer;
    }

    return result;
  }


  private void setCommonRuntimeInfoForParameter(Step step, Parameter parameter, StepRuntimeInfo info) {
    info.setId(ObjectId.createStepId(step).getObjectId());
    info.setStatus(getStatus(step, parameter).getName());
    info.setRunningTime(getRunningTime(parameter));
    info.setInputs(createInputs(step, parameter));
    info.setOutputs(createOutputs(step, parameter));
    info.setError(createError(step, parameter.getErrorInfo()));
  }


  private RuntimeInfo createWorkflowRuntimeInfoForParameter(WFStep step, Parameter p) {
    WorkflowRuntimeInfo result = new WorkflowRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, step.getFirstParameter(), result);
    String key = createId(step);
    result.setId(key);
    return result;
  }


  private RuntimeInfo createParallelExecutionRuntimeInfoForParameter(StepParallel step) {
    ParallelExecutionRuntimeInfo result = new ParallelExecutionRuntimeInfo();
    result.setId(ObjectId.createStepId(step).getObjectId());
    
    Set<Step> allSteps = new HashSet<>();
    WF.addChildStepsRecursively(allSteps, step);
    
    result.setStatus(getWorstStatus(allSteps).getName());
    result.setRunningTime(getRunningTime(allSteps));
    
    return result;
  }
  
  private OrderInstanceGuiStatus getWorstStatus(Set<Step> steps) {
    OrderInstanceGuiStatus orderInstanceGuiStatus = OrderInstanceGuiStatus.FINISHED;
    for (Step child : steps) {
      Parameter param = child.getFirstParameter();
      if(param == null) {
        continue;
      }
      OrderInstanceGuiStatus status = getStatus(child, param);
      if(status.getSeverity() > orderInstanceGuiStatus.getSeverity()) {
        orderInstanceGuiStatus = status;
      }
    }
    return orderInstanceGuiStatus;
  }
  
  private RunningTime getRunningTime(Set<Step> steps) {
    if(steps == null) {
      return null;
    }
    long startTimestamp = 0L;
    long endTimestamp = 0L;
    
    for (Step child : steps) {
      Parameter param = child.getFirstParameter();
      if(param == null) {
        continue;
      }
      try {
        if(param.getInputTimeStampUnix() < startTimestamp || startTimestamp == 0L) {
          startTimestamp = param.getInputTimeStampUnix();
        }        
        if(param.getOutputTimeStampUnix() > endTimestamp) {
          endTimestamp = param.getOutputTimeStampUnix();
        }
      } catch (ParseException ex) {
        Utils.logError(ex);
      }
    }
    return new RunningTime(startTimestamp, endTimestamp);
  }


  private RuntimeInfo createThrowRuntimeInfoForParameter(StepThrow step, Parameter p) {
    ThrowRuntimeInfo result = new ThrowRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, p, result);
    return result;
  }


  private <T extends Step> IterationContainer createIterations(T step, BiFunction<T, Parameter, RuntimeInfo> creator) {
    List<Parameter> parameterList = step.getParameterList();
    boolean containsForeachIndices =
        step.getFirstParameter().getForeachIndices() != null && step.getFirstParameter().getForeachIndices().size() > 0;
    int foreachIndexLength = step.getFirstParameter().getForeachIndices().size() - 1;
    boolean containsRetries = step.isInRetryLoop();
    IterationContainer result;

    // determine unique key for GUI to associate all iteration container that below to an iteration
    Step iterationParent;
    if (containsForeachIndices) {
      result = new ForeachIterationContainer();
      iterationParent = getWrappingForeach(step);
    } else {
      result = new RetryIterationContainer();
      iterationParent = getRetriedStep(step);
    }
    if (iterationParent != null) {
      result.setIterationContainerKey(createId(iterationParent));
    }

    result.setId(createId(step));

    for (Parameter p : parameterList) {
      RuntimeInfo info = creator.apply(step, p);
      int index = 0;
      if (containsRetries) {
        // make sure retry counter starts at 0 within a new loop of an enclosing foreach
        index = getRetryIndexForGui(step, p);
      } else {
        index = p.getForeachIndices().get(foreachIndexLength);
      }

      IterationEntry entry = new IterationEntry();
      entry.setRuntimeInfo(info);
      entry.setIndex(index);

      insertIntoContainer(result, entry, step, p, containsRetries, 0);
    }

    if (containsRetries) {
      xbe126Workaround(step, result, parameterList, creator);
    }

    return result;
  }


  private StepForeach getWrappingForeach(Step step) {
    if (step == null) {
      return null;
    }

    Step parentStep = step.getParentStep();
    while (parentStep != null) {
      if (parentStep instanceof StepForeach) {
        return (StepForeach)parentStep;
      }

      parentStep = parentStep.getParentStep();
    }

    return null;
  }


  private Step getRetriedStep(Step step) {
    if (step == null || !step.isInRetryLoop()) {
      return null;
    }

    ScopeStep wfAsStep = (step instanceof WFStep) ? ((WFStep)step).getWF().getWfAsStep() :
      step.getParentWFObject().getWfAsStep();
    if (wfAsStep.isInRetryLoop()) {
      // step is in workflow-retry
      return wfAsStep.getChildStep();
    }

    StepCatch stepCatch = step.getRetryCatch();
    if (stepCatch != null) {
      // step is retried by a retry in on of its catch-blocks
      return stepCatch.getStepInTryBlock();
    }

    return null;
  }


  /*
   * Calculates the index to be sent to the GUI within a retry iteration.
   * 
   * The retry counter in parameter-tags keeps ascending even when a new iteration of an enclosing iteration starts.
   * This method compensates for this, letting the index start at 0 for every new iteration.
   */
   
  private int getRetryIndexForGui(Step step, Parameter parameter) {
    int index = parameter.getRetryCounter();

    StepCatch enclosingStepCatch = step.getRetryCatch();
    if (enclosingStepCatch != null) {
      index -= enclosingStepCatch.getRetryCounterRange(parameter.getForeachIndices(), true).getFirst();
    }

    return index;
  }


  /**
   * Due to a bug in the routine to write the audits, retried steps for which all retries fail won't have any
   * information for the step about their iterations except for the first one. To make sure the GUI is still
   * able to show all iterations, this method adds fake IterationEntries for the missing iterations.
   * 
   * This workaround is still necessary even when XBE-126 is fixed since old audits will still have this issue.
   */
  private <T extends Step> void xbe126Workaround(T step, IterationContainer result, List<Parameter> parameterList, BiFunction<T, Parameter, RuntimeInfo> creator) {
    if (parameterList.size() == 0) {
      return;
    }

    StepCatch stepCatch;
    if (step instanceof Catchable) {
      Step catchProxy = ((Catchable)step).getProxyForCatch();
      if (catchProxy != step) {
        stepCatch = (StepCatch)catchProxy;
      } else {
        return;
      }
    } else if (step instanceof StepCatch) {
      stepCatch = (StepCatch)step;
    } else {
      return;
    }

    List<List<Integer>> allForeachIndices = getUniqueForeachIndices(parameterList);
    for (List<Integer> foreachIndices : allForeachIndices) {
      // get parameters for the current foreach-iteration
      Stream<Parameter> paramFilterStream = parameterList.stream().filter(x -> x.foreachIndicesEqual(foreachIndices));
      List<Parameter> paramsForCurForeachIndices = paramFilterStream.collect(Collectors.toList());
      paramsForCurForeachIndices.sort(new Comparator<Parameter>() {
        public int compare(Parameter o1, Parameter o2) {
          return o1.getRetryCounter() > o2.getRetryCounter() ? 1 : -1;
        }
      });

      // determine number of retry-iterations by finding the highest retry-counter of the executed catches
      Pair<Integer, Integer> retryCounterRange = stepCatch.getRetryCounterRange(foreachIndices, true);

      // clone info from first iteration to use for all missing entries
      Parameter parameterToClone = paramsForCurForeachIndices.get(0);
      StepRuntimeInfo info = (StepRuntimeInfo)creator.apply(step, parameterList.get(0));
      if (!(info instanceof WorkflowRuntimeInfo)) {
        info.setRunningTime(null);
        info.setInputs(null);
        info.setOutputs(null);
        info.setError(null);
        info.setStatus(OrderInstanceGuiStatus.UNKNOWN.getName());
        if (info instanceof ServiceRuntimeInfo) {
          ((ServiceRuntimeInfo)info).setOrderId(0);
        }
      }

      Set<Integer> existingRetryCounters = stepCatch.getRetryCounterValues(foreachIndices, false);
      Set<Integer> desiredRetryCounters = stepCatch.getRetryCounterValues(foreachIndices, true);

      for (Integer desiredRetryCounter : desiredRetryCounters) {
        if (existingRetryCounters.contains(desiredRetryCounter)) {
          continue;
        }

        IterationEntry entry = new IterationEntry();
        entry.setRuntimeInfo(info);
        int guiIdx = desiredRetryCounter - retryCounterRange.getFirst(); // make sure retry counter starts at 0 within a new loop of an enclosing foreach
        entry.setIndex(guiIdx);

        insertIntoContainer(result, entry, step, parameterToClone, true, 0);
      }
    }
  }


  private List<List<Integer>> getUniqueForeachIndices(List<Parameter> parameterList) {
    List<List<Integer>> uniqueForeachIndices = new ArrayList<List<Integer>>();
    for (Parameter param : parameterList) {
      // are these indices already in the list?
      boolean alreadyInSet = false;
      for (List<Integer> foundIndices : uniqueForeachIndices) {
        if (param.getForeachIndices().equals(foundIndices)) {
          alreadyInSet = true;
          break;
        }
      }

      if (!alreadyInSet) {
        uniqueForeachIndices.add(param.getForeachIndices());
      }
    }

    return uniqueForeachIndices;
  }


  //TODO: rewrite to allow retries to become a list as well! => then we can call a method containing the loop twice!
  //entry is a leaf => contains a StepRuntimeInfo and not an IterationContainer
  //if no forEach indices -> insert entry to root
  //if there is one forEach index, but no retries, insert to root
  //if there is one forEach index and retries, insert a RetryIterationContainer to root
  //offset is relevant for ForEach steps. offset is number of ForEach steps that should be ignored here
  //    example: for two ForEach steps with a mapping
  //             Parameter p has two ForEach Indices
  //
  //             the offset when creating the mapping RuntimeInfos is 0
  //             when creating ForeachRuntimeInfos for the inner ForEach it is 1 (ignoring itself)
  //             when creating ForeachRuntimeInfos for the outer ForEach it should be 2 (ignoring itself and all inner ForEach steps)
  //             but unless there are retries the outer ForEach only creates a regular ForeachRuntimeInfo object and no iteration container.
  //   ForEach indices are ignored from right to left (inner indices are ignored first)
  private void insertIntoContainer(IterationContainer root, IterationEntry entry, Step step, Parameter p, boolean containsRetries, int offset) {
    IterationContainer currentContainer = root;
    List<Integer> foreachIndices = p.getForeachIndices();
    
    //create IterationContainer for ForEach
    //start at outermost ForEach (i=0) and ignore inner indices (offset -1)
    for (int i = 0; i < foreachIndices.size() - offset - 1; i++) { //-1 because of root
      int index = foreachIndices.get(i); //index of this iteration for the given (by i) ForEach
      
      if (currentContainer.getIterations() == null) {
        currentContainer.setIterations(new ArrayList<IterationEntry>());
      }
      
      //is there a container for this iteration already? (then info is present)
      //if not (else - info is empty), create a new container 
      Optional<? extends IterationEntry> info = currentContainer.getIterations().stream().filter(x -> x.getIndex() == index).findFirst();
      if (info.isPresent()) {
        //continue down the tree
        currentContainer = (IterationContainer) info.get().getRuntimeInfo();
      } else {
        //create a new IterationContainer
        IterationContainer newContainer = new ForeachIterationContainer();
        newContainer.setIterationContainerKey(currentContainer.getIterationContainerKey()); // TODO: Kann currentContainer hier auch ein RetryIterationContainer sein? Dann w�re das hier falsch.
        newContainer.setId(currentContainer.getId());

        StepForeach wrappingForeach = getWrappingForeach(step);
        if (wrappingForeach != null) {
          wrappingForeach = getWrappingForeach(wrappingForeach);
          if (wrappingForeach != null) {
            currentContainer.setIterationContainerKey(createId(wrappingForeach));
          }
        }

        IterationEntry intermediateEntry = new IterationEntry();
        intermediateEntry.setIndex(index);
        intermediateEntry.setRuntimeInfo(newContainer);
        currentContainer.addToIterations(intermediateEntry);
        currentContainer = newContainer;
      }
    }

    
    //we have to create another container, if
    // - there are retries
    // - we are not ignoring all ForEach indices
    //     foreachIndiceis.size() => how many ForEaches are there?
    //     offset => how many ForEaches do we ignore? (-> descendants are ForEach)
    //     no ForEach indices means we are ignoring all of them
    //     if we ignore all ForEach indices, then root is a RetyIteationContainer
    //basically another iteration in loop above, but handling retry counter instead of ForEach indices
    if (containsRetries && foreachIndices.size() > offset) {
      int index = foreachIndices.get(foreachIndices.size() -offset - 1);
      Optional<? extends IterationEntry> info = Optional.empty();
      if (currentContainer.getIterations() != null) {
        info = currentContainer.getIterations().stream().filter(x -> x.getIndex() == index).findFirst();
      }

      if (info.isPresent()) {
        //we already have a container
        currentContainer = (IterationContainer) info.get().getRuntimeInfo();
      } else {
        //create a new IterationContainer
        IterationContainer newContainer = new RetryIterationContainer();

        Step retriedStep = getRetriedStep(step);
        if (retriedStep != null) {
          newContainer.setIterationContainerKey(createId(retriedStep));
        }

        IterationEntry intermediateEntry = new IterationEntry();
        intermediateEntry.setIndex(index);
        intermediateEntry.setRuntimeInfo(newContainer);
        currentContainer.addToIterations(intermediateEntry);
        currentContainer = newContainer;
      }
    }

    currentContainer.addToIterations(entry);
  }


  private MappingRuntimeInfo createMappingRuntimeInfoForParameter(StepMapping step, Parameter parameter) {
    MappingRuntimeInfo result = new MappingRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, parameter, result);
    return result;
  }


  private TemplateRuntimeInfo createTemplateRuntimeInfoForParameter(StepMapping step, Parameter parameter) {
    TemplateRuntimeInfo result = new TemplateRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, parameter, result);
    return result;
  }


  private RuntimeInfo createRetryRuntimeInfoForParameter(StepRetry step, Parameter parameter) {
    RetryRuntimeInfo result = new RetryRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, parameter, result);
    return result;
  }


  private RuntimeInfo createDistinctionBranchRuntimeInfoForParameter(StepSerial stepSerial, Parameter parameter) {
    BranchRuntimeInfo result;
    Distinction distinction = (Distinction)stepSerial.getParentStep();
    ObjectId stepId;

    if (distinction instanceof StepCatch) {
      result = new CatchBranchRuntimeInfo();
      StepCatch stepCatch = (StepCatch)distinction;
      if (stepCatch.getParentStep() instanceof WFStep) {
        stepId = new ObjectId(ObjectType.workflow, null);
      } else {
        stepId = ObjectId.createStepId(stepCatch.getStepInTryBlock());
      }
      ((CatchBranchRuntimeInfo)result).setCaughtException(createError(stepSerial, parameter.getCaughtExceptionInfo()));
    } else {
      result = new ChoiceBranchRuntimeInfo();
      stepId = ObjectId.createStepId((Step)distinction);
    }

    int id = -1;
    Stream<Parameter> s;
    for (int i = 0; i < distinction.getBranchesForGUI().size(); i++) {
      BranchInfo branch = distinction.getBranchesForGUI().get(i);

      Step executedStep = branch.getExecutedStep();
      if (executedStep == null) {
        // branch has not been executed for any iteration
        continue;
      }

      s = executedStep.getParameterList().stream();
      if (s.anyMatch(x -> x.foreachIndicesEqual(parameter.getForeachIndices()) && x.getRetryCounter() == parameter.getRetryCounter())) {
        // branch has been executed for this iteration
        setCommonRuntimeInfoForParameter(executedStep, parameter, result);
        id = i;
        break;
      }
    }

    result.setId(ObjectId.createBranchId(stepId.getBaseId(), String.valueOf(id)));

    return result;
  }


  private BiFunction<Step, Parameter, RuntimeInfo> createParallelismBranchRuntimeInfoForParameterWrapper(StepSerial step) {
    return (s, p) -> createParallelismBranchRuntimeInfoForParameter(step, s, p);
  }


  
  //assumes parameters are ordered
  private Parameter getLastParameter(List<Parameter> parameters) {
    if(parameters == null || parameters.size() == 0) {
      return null;
    }
    
    return parameters.get(parameters.size()-1);
  }
  
  //step used to determine second parameter might have more foreachIndices!
  private Parameter findLaterParameterForParallelismBranch(StepSerial stepSerial, Parameter parameter) {
    Step stepProvidingP2 = stepSerial.getLastExecutedStep(parameter);
    Parameter p2 = getLastParameter(stepProvidingP2.getParameters(parameter.getForeachIndices(), parameter.getRetryCounter()));
    int iterationCount = 0;
    
    //happens if the last executed step is a stepParallel. (stepParallel does not have any parameters)
    while (p2 == null && stepProvidingP2 instanceof StepSerial && iterationCount < 1000) {
      stepProvidingP2 = ((StepSerial) stepProvidingP2).getLastExecutedStep(parameter);
      p2 = getLastParameter(stepProvidingP2.getParameters(parameter.getForeachIndices(), parameter.getRetryCounter()));
      iterationCount++;
    }
    
    //could not find correct parameter.
    if(p2 == null) {
      if(logger.isDebugEnabled()) {
      logger.debug("could not find a matching parameter for " + stepSerial.getContainerStepForGui()
          + "["+String.join(",", parameter.getForeachIndices().stream().map(x -> x.toString()).collect(Collectors.toList()))+"], " 
          + parameter.getRetryCounter());
      }
      return parameter;
    }

    return p2;
  }


  //parameters come from a child of stepSerial since stepSerial itself does not have parameters
  //id will be updated later anyway, so do not bother setting it here
  //stepSerial belongs to stepParallel
  //stepProvidingParameter is a parent of the StepParallel
  private ParallelismBranchRuntimeInfo createParallelismBranchRuntimeInfoForParameter(StepSerial stepSerial, Step stepProvidingParameters, Parameter parameter) {
    ParallelismBranchRuntimeInfo result = new ParallelismBranchRuntimeInfo();
    Parameter p2 = findLaterParameterForParallelismBranch(stepSerial, parameter);

    Parameter merge = new Parameter();
    merge.setInputTimeStamp(parameter.getInputTimeStamp());
    merge.setOutputTimeStamp(p2.getOutputTimeStamp());
    merge.setErrorInfo(p2.getErrorInfo());

    result.setStatus(getStatus(stepSerial, p2).toString());
    result.setRunningTime(getRunningTime(merge));

    return result;
  }


  private RuntimeInfo createForeachRuntimeInfo(StepForeach step) {

    RuntimeInfo result = null;

    //how many more StepForeach are around us
    //if there are ForEach steps around us, we have to create iteration container objects 
    int depth = getEnclosingForeachCount(step);

    Step executedStep = getExecutedStepInsideForeach(step);

    //if there are retries, we have to create iteration container objects
    boolean containsRetries = doesStepContainRetries(executedStep);
    
    //we only need one result object
    if (depth == 0 && !containsRetries) {
      result = createSingleForeachRuntimeInfo(step, executedStep, null, -1);
    } else {
      //we need iterations
      BiFunction<List<Integer>, Integer, RuntimeInfo> func = (foreachIndices, retry) -> {
        return createSingleForeachRuntimeInfo(step, executedStep, foreachIndices, retry);
      };
      result = createIterationRuntimeInfoForForeach(step, executedStep, containsRetries, func);
    }

    updateIdInRuntimeInfo(result, createId(step));
    return result;
  }


  private RuntimeInfo createIterationRuntimeInfoForForeach(StepForeach step, Step executedStep, boolean containsRetries,
                                                           BiFunction<List<Integer>, Integer, RuntimeInfo> func) {

    //step was not executed.
    if (executedStep.getFirstParameter() == null) {
      return null;
    }

    List<Parameter> parameters = executedStep.getParameterList();
    int totalForeachCount = executedStep.getFirstParameter().getForeachIndices().size();
    int enclosingForeachCount = getEnclosingForeachCount(step);
    int innerForeachCount = totalForeachCount - enclosingForeachCount - 1; //-1 => this ForEach

    int offset = totalForeachCount - innerForeachCount;
    IterationContainer root;

    // determine unique key for GUI to associate all iteration container that below to an iteration
    Step iterationParent;
    if (enclosingForeachCount > 0) {
      root = new ForeachIterationContainer();
      iterationParent = getWrappingForeach(step);
    } else {
      root = new RetryIterationContainer();
      iterationParent = getRetriedStep(step);
    }
    if (iterationParent != null) {
      root.setIterationContainerKey(createId(iterationParent));
    }

    //where to look in getForeachIndices() to find index of iteration
    // - offset to go back one index per forEach we want to ignore (-> forEach steps below us and this forEach)
    // if there are two stepForeach and we are looking at the inner one
    // than there are two foreachIndices and foreachIndexLength is one (-> for us)
    // that means we are only interested in forEachIndices [X, first and last]
    int foreachIndexLength = totalForeachCount - offset;

    IterationEntry entry = null;
    RuntimeInfo info = null;
    int index = -1;
    int totalLength = executedStep.getFirstParameter().getForeachIndices().size();
    List<Integer> setIndices;


    //only one parameter per ForeachRuntimeInfo object (fist)
    //ignore all inner ForEach and ourselves
    Stream<Parameter> s = parameters.stream();
    s = getRelevantParametersForForeach(s, enclosingForeachCount, totalLength);
    List<Parameter> parametersShortened = s.collect(Collectors.toList());

    for (Parameter p : parametersShortened) {
      entry = new IterationEntry();

      index = (containsRetries ? getRetryIndexForGui(executedStep, p) : p.getForeachIndices().get(foreachIndexLength));
      setIndices = p.getLoopIndices().subList(0, enclosingForeachCount);
      info = func.apply(setIndices, containsRetries ? index : -1);
      entry.setRuntimeInfo(info);
      entry.setIndex(index);

      //ignore one index for each inner ForEach and ourselves
      int insertIntoContainerOffset = 1 + innerForeachCount;
      
      insertIntoContainer(root, entry, executedStep, p, containsRetries, insertIntoContainerOffset);
    }

    return root;
  }


  //pass executedStep so we do not need to determine it every iteration
  private ForeachRuntimeInfo createSingleForeachRuntimeInfo(StepForeach step, Step executedStep, List<Integer> setIndices, int retryIndex) {
    ForeachRuntimeInfo result = new ForeachRuntimeInfo();
    int iterations = getIterationCount(executedStep, setIndices, retryIndex);
    RunningTime runningTime = getIterationRunningtime(executedStep, setIndices, retryIndex);
    result.setId(createId(step));
    result.setIterationCount(iterations);
    result.setRunningTime(runningTime);
    return result;
  }


  //pass executedStep so we do not need to determine it every iteration
  private IterationRuntimeInfo createSingleIterationRuntimeInfo(String id, Step executedStep, List<Integer> setIndices, int retryCount) {
    IterationRuntimeInfo result = new IterationRuntimeInfo();
    RunningTime runningTime = getIterationRunningtime(executedStep, setIndices, retryCount);
    result.setId(id);
    result.setRunningTime(runningTime);
    return result;
  }


  private RuntimeInfo createChoiceRuntimeInfoForParameter(StepChoice step, Parameter parameter) {
    ChoiceRuntimeInfo result = new ChoiceRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, parameter, result);
    result.setCaseId(determineExecutedCase(step, parameter.getForeachIndices(), parameter.getRetryCounter()));
    return result;
  }

  
  private boolean doesStepContainRetries(Step step) {  
    Set<Step> allSteps = new HashSet<>();
    WF.addChildStepsRecursively(allSteps, step);
    allSteps.add(step);
    return allSteps.stream()
        .anyMatch(x -> x.getParameterList() != null && x.getParameterList().stream().anyMatch(y -> y.getRetryCounter() > 0));
  }

  private RuntimeInfo createIterationRuntimeInfo(StepSerial step) {
    RuntimeInfo result = null;
    final String id = createId(step);

    Step s = step.getChildSteps().get(0);

    if (s instanceof StepForeach) {
      Step executedStep = getExecutedStepInsideForeach((StepForeach) s);
      boolean retries = doesStepContainRetries(executedStep);

      BiFunction<List<Integer>, Integer, RuntimeInfo> func = (foreachIndices, retry) -> {
        return createSingleIterationRuntimeInfo(id, executedStep, foreachIndices, retry);
      };
      result = createIterationRuntimeInfoForForeach((StepForeach) s, executedStep, retries, func);
      if (result != null) { //result is null if step inside was not executed
        result.setId(id);
      }
    } else {
      // regular path.
      result = createRuntimeInfo(s, (st, p) -> createIterationRuntimeInfo(st, p));
      updateIdInRuntimeInfo(result, id);
    }

    return result;
  }


  private RuntimeInfo createIterationRuntimeInfo(Step step, Parameter parameter) {
    IterationRuntimeInfo result = new IterationRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, parameter, result);
    RunningTime time = getIterationRunningtime(step, parameter.getForeachIndices(), parameter.getRetryCounter());
    result.setRunningTime(time);
    return result;
  }


  private RuntimeInfo createServiceRuntimeInfoForParameter(StepFunction step, Parameter p) {
    ServiceRuntimeInfo result = new ServiceRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, p, result);
    result.setOrderId(p.getInstanceId());
    result.setExecutionRTC(rootRTC.getGUIRepresentation());

    // determine original RTC
    if (!isImported) {
      try {
        OrderInstanceDetails details = multiChannelPortal.getOrderInstanceDetails(p.getInstanceId());
        result.setOriginalRTC(details.getRuntimeContext().getGUIRepresentation());
      } catch (Exception e) {
        Utils.logError("Could not determine original RTC for Order " + p.getInstanceId(), e);
      }
    }

    return result;
  }
  
  private boolean isQuery(StepFunction step) {
    return QueryUtils.findQueryHelperMapping(step) != null;
  }
  
  private RuntimeInfo createQueryRuntimeInfoForParameter(StepFunction step, Parameter p) {
    QueryRuntimeInfo result = new QueryRuntimeInfo();
    setCommonRuntimeInfoForParameter(step, p, result);
    
    StepMapping mapping = QueryUtils.findQueryHelperMapping(step);
    if(mapping != null && mapping.getFirstParameter() != null) {
      try {
        result.getRunningTime().setStart(mapping.getFirstParameter().getInputTimeStampUnix());
      } catch (ParseException e) {
        Utils.logError(e);
      }
    }
    
    return result;
  }


  private OrderInstanceGuiStatus getStatus(Step step, Parameter parameter) {
    if (step instanceof StepSerial) {
      Step lastExecutedStep = ((StepSerial)step).getLastExecutedStep(parameter);
      return getStatus(lastExecutedStep, parameter);
    }

    ErrorInfo errorInfo = parameter.getErrorInfo();
    if (errorInfo != null) {
      return OrderInstanceGuiStatus.FAILED;
    } else if ( (step instanceof StepRetry) ||
                (parameter.getOutputTimeStamp() != null) && (parameter.getOutputTimeStamp().length() > 0) ||
                (step.hasRetryIterationsLeft(parameter)) ) {
      return OrderInstanceGuiStatus.FINISHED;
    } else if ( (parameter.getInputTimeStamp() != null) && (parameter.getInputTimeStamp().length() > 0) ) {
      return OrderInstanceGuiStatus.RUNNING;
    } else {
      return OrderInstanceGuiStatus.UNKNOWN;
    }
  }


  //TODO: remove with "wf" id -> use step instead.
  private String createId(Step s) {
    if (s instanceof WFStep) {
      return "wf";
    } else {
      return ObjectId.createStepId(s).getObjectId();
    }
  }


  private RunningTime getRunningTime(Parameter p) {
    RunningTime result = new RunningTime();
    try {
      result.setStart(p.getInputTimeStampUnix());
      long lastUpdate = p.getErrorInfo() == null ? p.getOutputTimeStampUnix() : p.getErrorTimeStampUnix();
      result.setLastUpdate(lastUpdate);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return result;
  }


  private List<String> createInputs(Step s, Parameter p) {
    if(impl == null) {
      return null;
    }
    List<String> result = impl.getJsonsFromParameterList(s, p.getInputData(), p.getForeachIndices(), p.getRetryCounter());
// TODO: can this be done something like this?
//    List<String> result = new ArrayList<String>();
//    for (AVariable inputDate : p.getInputData()) {
//      try {
//        result = new ArrayList<String>();
//        result.add(Utils.xoToJson(inputDate.getXoRepresentation(), inputDate.getRevision()));
//      } catch (Exception e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
//    }

    return result;
  }


  private List<String> createOutputs(Step s, Parameter p) {
    if(impl == null) {
      return null;
    }
    List<String> result = impl.getJsonsFromParameterList(s, p.getOutputData(), p.getForeachIndices(), p.getRetryCounter());
    return result;
  }


  private Error createError(Step s, ErrorInfo info) {
    Error result = null;
    if (info != null) {
      result = new Error();
      result.setException(info.getExceptionVariable().getFQClassName());
      result.setStacktrace(info.getStacktrace());
      result.setMessage(info.getMessage());
    }

    return result;
  }


  private String determineExecutedCase(StepChoice step, List<Integer> foreachIndices, int retryCounter) {
    for (Step branch : step.getChildSteps()) {
      if (branch.hasBeenExecuted(foreachIndices, retryCounter)) {
        String caseNo = String.valueOf(step.getCaseNo(branch));
        return ObjectId.createCaseId(step.getStepId(), caseNo);
      }
    }

    return null;
  }


  private int getIterationCount(Step step, List<Integer> setIndices, int retryCounter) {
    List<Parameter> parameters = step.getParameterList();
    if (parameters == null || parameters.size() == 0)
      return 0;

    //filter to fit retry counter
    Stream<Parameter> s = retryCounter < 0 ? parameters.stream()
        : parameters.stream().filter(x -> x.getRetryCounter() == retryCounter);

    int depthToCheck = setIndices == null ? 0 : setIndices.size();
    
    //for each set index, filter to parameters that fit
    if (setIndices != null && setIndices.size() > 0) {
      for (int i = 0; i < setIndices.size(); i++) {
        final int setIndex = setIndices.get(i);
        final int index = i;
        s = s.filter(x -> x.getForeachIndices().get(index) == setIndex);
      }
    }
    
    //highest parameter
    Optional<Parameter> max = s.max((p1, p2) -> p1.getForeachIndices().get(depthToCheck) > p2.getForeachIndices().get(depthToCheck) ? 1 : -1);
    if (max.isPresent())
      return max.get().getForeachIndices().get(depthToCheck) + 1; //+1 to translate from index to # of iterations
    return 0;
  }


  //recursive update of id
  private void updateIdInRuntimeInfo(RuntimeInfo info, String newId) {
    if (info == null) {
      return;
    }

    if (info instanceof IterationContainer) {
      info.setId(newId);
      for (IterationEntry entry : ((IterationContainer) info).getIterations()) {
        updateIdInRuntimeInfo(entry.getRuntimeInfo(), newId);
      }
    } else {
      info.setId(newId);
    }
  }


  private Step getExecutedStepInsideForeach(StepForeach step) {
    Step result = step.getChildSteps().get(0).getChildSteps().get(0).getChildSteps().get(0);
    int maxDepth = 10000;
    int count = 0;

    while (result instanceof StepForeach && count < maxDepth) {
      count++;
      result = result.getChildSteps().get(0).getChildSteps().get(0).getChildSteps().get(0);
    }

    return result;
  }


  private int getEnclosingForeachCount(Step step) {
    int result = 0;
    Step genStep = step;

    while (genStep != null) {
      genStep = genStep.getParentStep();
      if (genStep instanceof StepForeach) {
        result++;
      }
    }

    return result;
  }


  //TODO: if step in stepSerial is a forEach, that step does not have parameters.
  //uses earliest and latest times of steps in stepSerial.
  //step is some step inside a stepSerial
  private RunningTime getIterationRunningtime(Step step, List<Integer> foreachIds, int retryCount) {
    RunningTime result = new RunningTime();

    step = step.getParentStep();
    Step firstStep = step.getChildSteps().get(0);
    Step lastStep = step.getChildSteps().get(step.getChildSteps().size() - 1);

    try {
      result.setStart(firstStep.getStartTime(foreachIds, retryCount));
      result.setLastUpdate(lastStep.getStopTime(foreachIds, retryCount));
    } catch (ParseException e) {
    }

    return result;
  }


  //removes all parameters where there is a != 0 entry in foreachIndices with an index > relevantLength
  private Stream<Parameter> getRelevantParametersForForeach(Stream<Parameter> stream, int relevantLength, int totalLength) {

    for (int i = totalLength - 1; i >= relevantLength; i--) {
      final int index = i;
      stream = stream.filter(p -> p.getForeachIndices().get(index) == 0);
    }

    return stream;
  }
}
