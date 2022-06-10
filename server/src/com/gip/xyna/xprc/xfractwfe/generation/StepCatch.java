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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.RetryException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.IRetryStep;
import com.gip.xyna.xprc.xfractwfe.base.ProcessStepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformationThrowable;


/**
 * entspricht mehreren zusammengehörenden xmlelementen &lt;catch&gt;.
 * daraus wird ein java step generiert der form
 * execute() {
 *   try {
 *     execChild();
 *   } catch (...) {
 *     execChild();
 *   } catch (...) {
 *     execChild();
 *   } ...
 * }
 */
public class StepCatch extends Step implements Distinction {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Service.class);

  private static final String _METHODNAME_GET_REGULAR_EXECUTION_STEP_ORIG = "getRegularExecutionStep";
  protected static final String METHODNAME_GET_REGULAR_EXECUTION_STEP;
  private static final String _METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN_ORIG = "suspendManualOrShutDown";
  protected static final String METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN;

  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    // ProcessStepCatch
    try {
      METHODNAME_GET_REGULAR_EXECUTION_STEP = ProcessStepCatch.class.getDeclaredMethod(_METHODNAME_GET_REGULAR_EXECUTION_STEP_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_REGULAR_EXECUTION_STEP_ORIG + " not found", e);
    }
    // SuspendResumeManagement
    try {
      METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN = SuspendResumeManagement.class.getDeclaredMethod(_METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN_ORIG, Long.class, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN_ORIG + " not found", e);
    }
  }
  
  /**
   * sortiert exceptions derart, dass die abgeleiteten exceptions vor ihren parents kommen.
   *
   */
  public static class ExceptionHierarchyComparator implements Comparator<ExceptionVariable> {

    public int compare(ExceptionVariable o1, ExceptionVariable o2) {
      if (o1.getFQClassName().equals(o2.getFQClassName())) {
        return 0;
      }
      int depth1 = getInheritanceDepth(o1);
      int depth2 = getInheritanceDepth(o2);
      if (depth1 == depth2) {        
        return o1.getFQClassName().compareTo(o2.getFQClassName());
      }
      if (depth1 > depth2) {
        //o1 soll vor o2 kommen
        return -1;
      }
      return 1;
    }

    private Map<ExceptionVariable, Integer> cache = new HashMap<>();

    private int getInheritanceDepth(ExceptionVariable ev) {
      Integer cached = cache.get(ev);
      if (cached != null) {
        return cached;
      }
      ExceptionGeneration parent = ev.getExceptionGeneration().getSuperClassExceptionGeneration();
      int depth = 0;
      while (parent != null) {       
        parent = parent.getSuperClassExceptionGeneration();
        depth ++;
      }
      cache.put(ev, depth);
      return depth;
    }
    
  }


  public class CatchBranchInfo extends BranchInfo {
    private Step triedStep;


    public CatchBranchInfo(Step triedStep) {
      super();
      this.triedStep = triedStep;
    }


    @Override
    public Step getMainStep() {
      return triedStep;
    }
  }


  private Map<String, Step> exceptionVariableIdsToCatch;
  private Map<String, CaseInfo> exceptionVariableIdsToCase;
  private Step stepReferenceToExecuteInTryBlock;
  private Set<String> exceptionHandlersWithRetry;


  public StepCatch(ScopeStep parentScope, Step stepToExecuteInTryBlock, GenerationBase creator) {
    super(parentScope, creator);
    this.stepReferenceToExecuteInTryBlock = stepToExecuteInTryBlock;
    exceptionVariableIdsToCatch = new HashMap<>();
    exceptionVariableIdsToCase = null;
    exceptionHandlersWithRetry = new HashSet<>();
  }
  
  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepCatch( this );
  }

  @Override
  /**
   * e = objekt mit catch-element(en): either function object or global serial operation
   */
  public void parseXML(Element functionObjectElement) throws XPRC_InvalidPackageNameException {
    // this gets the same XML id as the function object. for the global catch step this is null.
    parseId(functionObjectElement);
    
    List<Element> catchElements = XMLUtils.getChildElementsByName(functionObjectElement, GenerationBase.EL.CATCH);
    if (catchElements != null && catchElements.size() > 0) {
      for (int i = 0; i < catchElements.size(); i++) {
        Element catchEl = catchElements.get(i);
        String id = catchEl.getAttribute(GenerationBase.ATT.EXCEPTION_ID);
        if (id.trim().length() == 0) {
          id = "_unknown_" + i;
        }
        StepSerial step = new StepSerial(getParentScope(), new ArrayList<String>(Arrays.asList(new String[] {id})),creator);
        step.parseXML(catchEl);
        exceptionVariableIdsToCatch.put(id, step.getProxyForCatch());
        if (Step.containsOrIsAssignableFrom(step, StepRetry.class)) {
          exceptionHandlersWithRetry.add(id);
        }
      }
    }
    
    parseParameter(functionObjectElement);
  }
 

  @Override
  protected void getImports(HashSet<String> imports) throws XPRC_InvalidVariableIdException {
    imports.add(ProcessStepCatch.class.getName());
    imports.add(XynaExceptionInformationThrowable.class.getName());
    imports.add(SerializableClassloadedException.class.getName());
    imports.add(RetryException.class.getName());
    imports.add(IRetryStep.class.getName());
    imports.add(ProcessSuspendedException.class.getName());
    imports.add(SuspensionCause_Manual.class.getName());
    imports.add(SuspensionCause_ShutDown.class.getName());
    imports.add(XynaFactory.class.getName());
    imports.add(SuspensionCause.class.getName());    
    imports.add(SuspendResumeManagement.class.getName());    
    for (String id : exceptionVariableIdsToCatch.keySet()) {
      AVariable v = getParentScope().identifyVariable(id).variable;
      imports.add(v.getFQClassName());
    }
  }


  @Override
  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableIdException {
    sortCatchExceptionStepsFromSpecificToGeneral();

    cb.addLine("private static class ", getClassName(), " extends ", FractalProcessStep.class.getSimpleName(), "<",
               getParentScope().getClassName(), "> implements ", ProcessStepCatch.class.getSimpleName(), "<",
               getParentScope().getClassName(), "> {");
    cb.addLB();
    
    cb.addLine("private static final long serialVersionUID = ", Step.SERIAL_VERSION_UID_NO_VARS, "L");

    cb.addLine("public ", getClassName(), "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();
    
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {");
    // keine compensation notwendig
    cb.addLine("}").addLB();

    String[] inputVarIds = stepReferenceToExecuteInTryBlock.getInputVarIds();
    String[] outputVarIds = stepReferenceToExecuteInTryBlock.getOutputVarIds();
    String[] inputVarPaths = stepReferenceToExecuteInTryBlock.getInputVarPaths();
    String[] outputVarPaths = stepReferenceToExecuteInTryBlock.getOutputVarPaths();
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, inputVarIds, inputVarPaths, cb,
                                          importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, outputVarIds, outputVarPaths, cb,
                                          importedClassesFqStrings);

    generatedGetRefIdMethod(cb);

    cb.addLine("public ", FractalProcessStep.class.getSimpleName(), "<", getParentScope().getClassName(),
               "> ", METHODNAME_GET_REGULAR_EXECUTION_STEP, "() {");
    cb.addLine("return ", METHODNAME_GET_PARENT_SCOPE, "().", stepReferenceToExecuteInTryBlock.getVarName());
    cb.addLine("}").addLB();

    cb.addLine("protected ", FractalProcessStep.class.getSimpleName(), "<" + getParentScope().getClassName()
        + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    cb.addLine("if (i == 0) {");
    cb.add("return new " + FractalProcessStep.class.getSimpleName() + "[]{");
    cb.addListElement(METHODNAME_GET_PARENT_SCOPE + "()." + stepReferenceToExecuteInTryBlock.getVarName());
    cb.add("};").addLB().addLine("}");
    int i = 0;
    for (Step step : exceptionVariableIdsToCatch.values()) {
      i++;
      cb.addLine("if (i == " + i + ") {");
      cb.add("return new " + FractalProcessStep.class.getSimpleName() + "[]{");
      cb.addListElement(METHODNAME_GET_PARENT_SCOPE + "()." + step.getVarName());
      cb.add("};").addLB().addLine("}");
    }
    cb.addLine("return null");
    cb.addLine("}").addLB();

    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return " + (exceptionVariableIdsToCatch.size() + 1));
    cb.addLine("}").addLB();

    cb.addLine("}").addLB();
  }
    
  /**
   * verwendet {@link SuspendResumeManagement#suspendManualOrShutDown(Long, String)}
   */
  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws " + XynaException.class.getSimpleName() + " {");

    String retryId = "R"+getIdx();
    
    if (exceptionHandlersWithRetry.size() > 0) {
      cb.addLine("boolean caughtRetryException;");
      cb.addLine("do {");
      cb.addLine(METHODNAME_SET_LANE_ID, "(\"",retryId,"-\"+parentProcess.", WF.FIELDNAME_RETRY_COUNTER, ".get())");//laneId soll für jeden Retry eindeutig werden
      cb.addLine("caughtRetryException = false;");
    }
    cb.addLine("try {");
    cb.addLine("if (", FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION, ") {");

    String[] exceptions = new String[] {XynaException.class.getName(), RuntimeException.class.getName(),
                    Error.class.getName()};
    int cnt = 0;
    for (String exception : exceptions) {
      if (cnt > 0) {
        cb.add("else ");
      }
      cb.add("if (", FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, ".getThrowable() instanceof " + exception + ") {").addLB();
      cb.addLine("throw (" + exception + ") ", FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, ".getThrowable()");
      cb.addLine("}");
      cnt++;
    }
    cb.addLine("else {");
    cb.addLine("throw new " + RuntimeException.class.getSimpleName() + "(\"Unexpected error: Tried to rethrow unknown exception\", lastCaughtXynaExceptionContainer.getThrowable())");
    cb.addLine("}");
    cb.addLine("} else {");
    //eigentlicher funktionsaufruf
    cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(0)");
    cb.addLine("}");

    //catchblöcke
    int i = 0;
    for (String id : exceptionVariableIdsToCatch.keySet()) {
      i++;
      VariableIdentification vi = getParentScope().identifyVariable(id);
      ExceptionVariable v = (ExceptionVariable) vi.variable;
      String catchClass;
      if (v.getFQClassName().equals(Exception.class.getName())) {
        //exceptions die nicht gefangen werden können sollen behandeln:
        cb.addLine("} catch (", ProcessSuspendedException.class.getName(), " e) {");
        cb.addLine("throw e");
        cb.addLine("} catch (", OrderDeathException.class.getName(), " e) {");
        cb.addLine("throw e");
        cb.addLine("} catch (", ProcessAbortedException.class.getName(), " e) {");
        cb.addLine("throw e");
        catchClass = Exception.class.getName();
      } else {
        catchClass = v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings);
      }
      cb.addLine("} catch (" , catchClass , " e) {");
      cb.addLine("if (!", FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION, ") {");
      cb.addLine(FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER, " = new ", SerializableClassloadedException.class.getSimpleName(),
                 "(e)");
      cb.addLine(FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION, " = true");
      //   cb.addLine("getProcess().errorHandler(this)");
      cb.addLine("}");
      cb.addLine(vi.getScopeGetter(getParentScope()), v.getSetter("e", null));

      //suspenden bevor kinder ausgeführt werden
      cb.addLine("if (", ScopeStep.getScopeGetter(getParentScope(), getParentWFObject().getWfAsStep()), WF.METHODNAME_IS_ATTEMPTING_SUSPENSION, "()) {");
      // FIXME das ist nicht sicher. bessere lösung: beim setzen des attemptingSuspension-Flags gleich
      //       den SuspensionCause mitsetzen
      cb.addLine("throw ",SuspendResumeManagement.class.getSimpleName(), ".", METHODNAME_SUSPEND_MANUAL_OR_SHUTDOWN, "(null,getLaneId())");
      cb.addLine("}");
      
      if (exceptionHandlersWithRetry.contains(id)) {
        cb.addLine("try {");
      }
      cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(" + i + ")");
      if (exceptionHandlersWithRetry.contains(id)) {
        cb.addLine("} catch ("+ RetryException.class.getSimpleName() +" retry) {");
        cb.addLine(METHODNAME_COMPENSATE, "()");
        cb.addLine(METHODNAME_REINITIALIZE, "()");
        cb.addLine("parentProcess.", WF.FIELDNAME_RETRY_COUNTER, ".incrementAndGet()");
        cb.addLine("for (", FractalProcessStep.class.getSimpleName(), " step : ", METHODNAME_GET_CHILDREN, "(" + i + ")) {");
        cb.addLine("step.", METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY, "(false);"); //the exceptionHandler
        cb.addLine("}");
        cb.addLine("for (", FractalProcessStep.class.getSimpleName(), " step : ", METHODNAME_GET_CHILDREN, "(0)) {");
        cb.addLine("step.", METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY, "(true);"); //step inside try-block
        cb.addLine("}");
        cb.addLine("caughtRetryException = true;");
        cb.addLine("}");
      }
    }
    cb.addLine("}");
    
    if (exceptionHandlersWithRetry.size() > 0) {
      cb.addLine("} while (caughtRetryException);");
    }

    cb.addLine("}").addLB();
  }

  /**
   * is v1 some parent of v2
   */
  public static boolean isParentOf(ExceptionVariable v1, ExceptionVariable v2) {
    ExceptionGeneration parent = v2.getExceptionGeneration().getSuperClassExceptionGeneration();
    while (parent != null) {
      if (parent.getFqClassName().equals(v1.getFQClassName())) {
        return true;
      }
      parent = parent.getSuperClassExceptionGeneration();
    }
    return false;
  }
  
  private void sortCatchExceptionStepsFromSpecificToGeneral() throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableIdException {
    final Map<String, ExceptionVariable> idToVar = new HashMap<String, ExceptionVariable>();
    for (String id : exceptionVariableIdsToCatch.keySet()) {
      AVariable v = getParentScope().identifyVariable(id).variable;
      if (v instanceof ExceptionVariable) {
        idToVar.put(id, (ExceptionVariable)v);
      } else {
        throw new XPRC_InvalidVariableIdException(id);
      }
    }
    
    Map<String, Step> map = new TreeMap<String, Step>(new Comparator<String>() {
      
      private ExceptionHierarchyComparator comp = new ExceptionHierarchyComparator();

      public int compare(String id1, String id2) {
        ExceptionVariable v1 = idToVar.get(id1);
        ExceptionVariable v2 = idToVar.get(id2);
        return comp.compare(v1, v2);
      }
      
    });
    map.putAll(exceptionVariableIdsToCatch);
    exceptionVariableIdsToCatch = map;    
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    return null;
  }


  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
  }


  @Override
  protected List<Service> getServices() {
    return null;
  }


  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return null;
  }

  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }

  
  @Override
  public List<Step> getChildSteps() {
    List<Step> ret = new ArrayList<Step>();
    ret.add(stepReferenceToExecuteInTryBlock);
    for (Step s : exceptionVariableIdsToCatch.values()) {
      ret.add(s);
    }
    return ret;
  }


  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    if (stepReferenceToExecuteInTryBlock == oldChild) {
      stepReferenceToExecuteInTryBlock = newChild;
      return true;
    }

    return false;
  }


  public Step getStepInTryBlock() {
    return stepReferenceToExecuteInTryBlock;
  }
  
  public List<Step> getExecutedCatches() {
    List<Step> catches = new ArrayList<Step>();
    for (Step step : exceptionVariableIdsToCatch.values()) {
      if (step.hasBeenExecuted()) {
        catches.add(step);
      }
    }
    
    return catches;
  }
  
  @Override
  public Pair<Integer, Integer> getRetryCounterRange() {
    int counterMin = Integer.MAX_VALUE;
    int counterMax = Integer.MIN_VALUE;
    List<Step> executedCatches = getExecutedCatches();
    for (Step executedCatch : executedCatches) {
      List<Parameter> parameterList = executedCatch.getParameterList();
      for (Parameter parameter : parameterList) {
        if (parameter.getRetryCounter() < counterMin) {
          counterMin = parameter.getRetryCounter();
        }
        
        if (parameter.getRetryCounter() > counterMax) {
          counterMax = parameter.getRetryCounter();
        }
      }
    }
    
    if (counterMin == Integer.MAX_VALUE) {
      counterMin = 0;
    }
    if (counterMax == Integer.MIN_VALUE) {
      counterMax = 0;
    }
    
    return new Pair<Integer, Integer>(counterMin, counterMax);
  }
  
  public Step getExecutedCatch(List<Integer> foreachIndices, int retryCounter) {
    List<Step> executedCatches = getExecutedCatches();
    for (Step executedCatch : executedCatches) {
      Parameter parameter = executedCatch.getParameter(foreachIndices, retryCounter);
      if (parameter != null) {
        return executedCatch;
      }
    }
    
    return null;
  }
  
  public boolean hasRetry() {
    return ( (exceptionHandlersWithRetry != null) && (exceptionHandlersWithRetry.size() > 0) );
  }
  
  public boolean hasExecutedRetry() {
    if (exceptionHandlersWithRetry == null) {
      return false;
    }

    for (String exceptionId : exceptionHandlersWithRetry) {
      Step catchBlock = exceptionVariableIdsToCatch.get(exceptionId);
      Set<Step> recursiveChildren = new HashSet<>();
      WF.addChildStepsRecursively(recursiveChildren, catchBlock);
      for (Step child : recursiveChildren) {
        if (child instanceof StepRetry && child.hasBeenExecuted()) {
          return true;
        }
      }
    }

    return false;
  }
  
  @Override
  public boolean isExecutionDetached() {
    return false;
  }
  
  
  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepCatch)) {
      return true;
    }
    
    StepCatch oldCatchStep = (StepCatch)oldStep;
    
    if (exceptionVariableIdsToCatch != null && oldCatchStep.exceptionVariableIdsToCatch != null) {
      for (Entry<String,Step> catchEntry : exceptionVariableIdsToCatch.entrySet()) {
        Step oldCatchEntry = oldCatchStep.exceptionVariableIdsToCatch.get(catchEntry.getKey());
        if (oldCatchEntry == null) {
          return true;
        }
        if (catchEntry.getValue().compareImplementation(oldCatchEntry)) {
          return true;
        }
      }
    } else if (exceptionVariableIdsToCatch == null ^ oldCatchStep.exceptionVariableIdsToCatch == null) {
      return true;
    }
    
    return false;
  }


  @Override
  public Set<String> getAllUsedVariableIds() {
    return exceptionVariableIdsToCatch.keySet(); // TODO: Reihenfolge immer gleich?
  }


  public String[] getInputVarIds() {
    return getStepInTryBlock().getInputVarIds();
  }


  public String[] getOutputVarIds() {
    return getStepInTryBlock().getOutputVarIds(); // TODO: Das stimmt nur bei erfolgreichem Durchlaufen (beim 1. Mal oder bei Retry)
  }


  public List<AVariable> getInputVars() {
    return getStepInTryBlock().getInputVars();
  }


  public List<AVariable> getOutputVars() {
    return getStepInTryBlock().getOutputVars(); // TODO: Das stimmt nur bei erfolgreichem Durchlaufen (beim 1. Mal oder bei Retry)
  }


  @Override
  public long getStartTime(List<Integer> foreachIndices, int retryCounter) throws ParseException {
    return getStepInTryBlock().getStartTime(foreachIndices, retryCounter);
  }


  @Override
  public long getStopTime(List<Integer> foreachIndices, int retryCounter) throws ParseException {
    return getStepInTryBlock().getStopTime(foreachIndices, retryCounter);
  }


  @Override
  public void validate() throws XPRC_EmptyVariableIdException {
    for (String id : exceptionHandlersWithRetry) {
      if (id.startsWith("_unknown_")) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.ATT.EXCEPTION_ID);
      }
    }
  }

  @Override
  public List<CaseInfo> getHandledCases() {
    if (exceptionVariableIdsToCase == null) {
      initializeCases();
    }

    List<CaseInfo> caseInfos = new ArrayList<>();
    for (String exceptionId : getAllUsedVariableIds()) {
      caseInfos.add(exceptionVariableIdsToCase.get(exceptionId));
    }

    return caseInfos;
  }

  private void initializeCases() {
    exceptionVariableIdsToCase = new HashMap<>();
    for (String id : exceptionVariableIdsToCatch.keySet()) {
      ExceptionVariable caughtException = getExceptionById(id);
      exceptionVariableIdsToCase.put(id, createCaseInfo(caughtException));
    }
  }

  public List<ExceptionVariable> getCaughtExceptionVars() {
    List<ExceptionVariable> caughtExceptions = new ArrayList<>();
    for (String exceptionId : getAllUsedVariableIds()) {
      try {
        caughtExceptions.add((ExceptionVariable)getParentScope().identifyVariable(exceptionId).getVariable());
      } catch (XPRC_InvalidVariableIdException e) {
        logger.error(e);
        throw new RuntimeException("Couldn't determine exception var for id " + exceptionId, e);
      }
    }

    return caughtExceptions;
  }

  @Override
  public List<CaseInfo> getUnhandledCases(boolean considerRetryAsHandled) {
    List<CaseInfo> unhandledCases = new ArrayList<CaseInfo>();
    try {
      for (ExceptionVariable unhandledException : getUnhandledTypes(true, considerRetryAsHandled)) {
        String fqn = unhandledException.getOriginalPath() + "." + unhandledException.getOriginalName();
        unhandledCases.add(new CaseInfo(creator.getNextXmlId().toString(), unhandledException.getLabel(), fqn, "", this, false));
      }
    } catch (Exception e) {
      logger.error(e);
      throw new RuntimeException("Couldn't determine unhandled cases.", e);
    }

    return unhandledCases;
  }

  private List<ExceptionVariable> getUnhandledTypes(boolean includeBasicExceptions, boolean considerRetryAsHandled) throws XPRC_InvalidServiceIdException, XPRC_InvalidPackageNameException {
    List<ExceptionVariable> unhandledTypes = new ArrayList<ExceptionVariable>();

    // get all thrown exceptions of stepReferenceToExecuteInTryBlock
    List<ExceptionVariable> stepExceptions = stepReferenceToExecuteInTryBlock.getAllThrownExceptions(considerRetryAsHandled);

    // add catches for exceptions the step is throwing
    ExceptionVariable uncaughtException;
    for (ExceptionVariable stepException : stepExceptions) {
      if (!doesCaseExist(stepException.getOriginalPath(), stepException.getOriginalName(), considerRetryAsHandled)) {
        // clone ExceptionVariable and add to the unhandled exceptions
        uncaughtException = new ExceptionVariable(stepException);
        uncaughtException.setId(getCreator().getNextXmlId().toString());
        uncaughtException.setLabel(stepException.getLabel());

        if (getStepId() != null) {
          uncaughtException.setSourceIds(getStepId());
        }
        unhandledTypes.add(uncaughtException);
      }
    }

    if (includeBasicExceptions) {
      // add catches for basic exceptions (Server Exception and Exception)
      if (!doesCaseExist(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH, GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME, considerRetryAsHandled)) {
        uncaughtException = new ExceptionVariable(creator);
        uncaughtException.init(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH, GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME);
        uncaughtException.setLabel(SERVER_EXCEPTION_LABEL);
        uncaughtException.setId(getCreator().getNextXmlId().toString());
        if (getStepId() != null) {
          uncaughtException.setSourceIds(getStepId());
        }
        unhandledTypes.add(uncaughtException);
      }

      if (!doesCaseExist(BASE_EXCEPTION_PATH, BASE_EXCEPTION_NAME, considerRetryAsHandled)) {
        uncaughtException = new ExceptionVariable(creator);
        uncaughtException.init(BASE_EXCEPTION_PATH, BASE_EXCEPTION_NAME);
        uncaughtException.setLabel(BASE_EXCEPTION_LABEL);
        uncaughtException.setId(getCreator().getNextXmlId().toString());
        if (getStepId() != null) {
          uncaughtException.setSourceIds(getStepId());
        }
        unhandledTypes.add(uncaughtException);
      }
    }

    return unhandledTypes;
  }

  @Override
  public List<ExceptionVariable> getAllThrownExceptions(boolean considerRetryAsHandled) {
    try {
      List<ExceptionVariable> result = new ArrayList<ExceptionVariable>(getAllUncaughtExceptions(false, considerRetryAsHandled));

      for (Step s : getChildSteps()) {
        if (s == stepReferenceToExecuteInTryBlock) {
          continue; //already processed
        }

        List<ExceptionVariable> candidates = s.getAllThrownExceptions(considerRetryAsHandled);
        for (ExceptionVariable candidate : candidates) {
          if (result.stream().anyMatch(x -> x.getDomOrExceptionObject().equals(candidate.getDomOrExceptionObject()))) {
            continue;
          }
          result.add(candidate);
        }
      }
      return result;
    } catch (Exception e) {
      logger.error(e);
      throw new RuntimeException("Couldn't determine unhandled cases.", e);
    }
  }
  
  
  private List<ExceptionVariable> getAllUncaughtExceptions(boolean includeBasicExceptions, boolean considerRetryAsHandled) {
    List<ExceptionVariable> result = new ArrayList<ExceptionVariable>();
    List<ExceptionVariable> candidates = null;
    try {
      candidates = getUnhandledTypes(includeBasicExceptions, considerRetryAsHandled);
    } catch (Exception e) {
      logger.error("Could determine exception types.", e);
      throw new RuntimeException("Could determine exception types.", e);
    }

    //if baseTypes of Exception are caught, 
    if (doesCaseExist(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH, GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME, considerRetryAsHandled)
        || doesCaseExist(BASE_EXCEPTION_PATH, BASE_EXCEPTION_NAME, considerRetryAsHandled)) {
      return Collections.emptyList();
    }

    List<ExceptionVariable> handled = getCaughtExceptionVars();
    for (ExceptionVariable candidate : candidates) {
      if (!isCaught(candidate, handled)) {
        result.add(candidate);
      }
    }

    return result;
  }


  //returns false even if only a superClass of candidate is in handledExceptions
  private boolean isCaught(ExceptionVariable candidate, List<ExceptionVariable> handledExceptions) {
    DomOrExceptionGenerationBase candidateDoE = candidate.getDomOrExceptionObject();
    for (ExceptionVariable handled : handledExceptions) {
      if (DomOrExceptionGenerationBase.isSuperClass(handled.getDomOrExceptionObject(), candidateDoE)) {
        return true;
      }
    }
    return false;
  }


  @Override
  public void addBranch(int branchNo, String expression, String name) {
    try {
      for (ExceptionVariable unhandledType : getUnhandledTypes(true, true)) {
        String fqn = unhandledType.getOriginalPath() + "." + unhandledType.getOriginalName();
        if (expression.equals(fqn)) {
          addException(unhandledType);
          return;
        }
      }
    } catch (Exception e) {
      logger.error("Could determine exception types that are allowed to be added to exception handling.", e);
      throw new RuntimeException("Could determine exception types that are allowed to be added to exception handling.", e);
    }

    throw new RuntimeException("Exception type " + expression + " is not among the currently unhandled exceptions.");
  }

  @Override
  public void addMissingBranches() {
    try {
      createExceptionBranches(false);
    } catch (Exception e) {
      logger.error(e);
    }
  }

  private static final String BASE_EXCEPTION_PATH    = "core.exception";
  private static final String BASE_EXCEPTION_NAME    = "Exception";
  private static final String BASE_EXCEPTION_LABEL   = "Exception";
  private static final String SERVER_EXCEPTION_LABEL = "Server Exception";

  private void createExceptionBranches(boolean clearExisting) throws XPRC_InvalidServiceIdException, XPRC_InvalidPackageNameException {
    if (clearExisting) {
      // TODO
    }

    for (ExceptionVariable unhandledType : getUnhandledTypes(true, true)) {
      addException(unhandledType);
    }
  }


  public void addCaughtException(ExceptionVariable caughtException) {
    addException(caughtException);
  }


  private void addException(ExceptionVariable newCaughtException) {
    StepSerial catchBlock = new StepSerial(getParentScope(), creator);
    catchBlock.createEmpty();
    exceptionVariableIdsToCatch.put(newCaughtException.getId(), catchBlock);
    catchBlock.addChild(catchBlock.getChildSteps().size(), new StepAssign(getParentScope(), getCreator()));
    
    StepSerial exceptionVarContainer = getExceptionVarContainer();
    exceptionVarContainer.addExceptionVariable(newCaughtException);
    if (exceptionVariableIdsToCase == null ) {
      initializeCases();
    }
    exceptionVariableIdsToCase.put(newCaughtException.getId(), createCaseInfo(newCaughtException));
  }

  private StepSerial getExceptionVarContainer() {
    return getParentWFObject().getWfAsStep().getChildStep();
  }

  private boolean doesCaseExist(String path, String name, boolean considerRetryAsHandled) {
    for (String exceptionId : getAllUsedVariableIds()) {
      try {
        ExceptionVariable caughtException = (ExceptionVariable)getParentScope().identifyVariable(exceptionId).getVariable();
        if (caughtException.getOriginalPath().equals(path) && caughtException.getOriginalName().equals(name)) {
          return ( considerRetryAsHandled || (!exceptionHandlersWithRetry.contains(caughtException.getId())) );
        }
      } catch (XPRC_InvalidVariableIdException e) {
        logger.error("Could identify exception determine exception variable for exception " + exceptionId, e);
      }
    }

    return false;
  }

  @Override
  public BranchInfo removeBranch(int index) {
    BranchInfo branchToRemove = getBranchesForGUI().get(index);
    String fqnToBeRemoved = branchToRemove.getMainCase().getComplexName();
    for (String exceptionId : getAllUsedVariableIds()) {
      ExceptionVariable caughtException;
      try {
        caughtException = (ExceptionVariable)getParentScope().identifyVariable(exceptionId).getVariable();
      } catch (XPRC_InvalidVariableIdException e) {
        logger.error("Could not determine exception types that are caught for step " + stepReferenceToExecuteInTryBlock, e);
        throw new RuntimeException("Could not determine exception types that are caught for step " + stepReferenceToExecuteInTryBlock, e);
      }
      String fqn = caughtException.getOriginalPath() + "." + caughtException.getOriginalName();
      if (fqn.equals(fqnToBeRemoved)) {
        exceptionVariableIdsToCatch.remove(exceptionId);
        StepSerial exceptionVarContainer = getExceptionVarContainer();
        exceptionVarContainer.removeExceptionVariable(caughtException);

        return branchToRemove;
      }
    }

    return null;
  }

  @Override
  public List<BranchInfo> getBranchesForGUI() {
    if (exceptionVariableIdsToCase == null) {
      initializeCases();
    }

    List<BranchInfo> branchInfos = new ArrayList<>();
    for (String exceptionId : getAllUsedVariableIds()) {
      Step catchStep = exceptionVariableIdsToCatch.get(exceptionId);
      BranchInfo branchInfo = new CatchBranchInfo(catchStep);
      branchInfo.addCase(exceptionVariableIdsToCase.get(exceptionId));
      branchInfos.add(branchInfo);
    }

    return branchInfos;
  }


  private ExceptionVariable getExceptionById(String exceptionId) {
    try {
      return (ExceptionVariable) getParentScope().identifyVariable(exceptionId).getVariable();
    } catch (XPRC_InvalidVariableIdException e) {
      throw new RuntimeException("Could not determine exception variable for id " + exceptionId, e);
    }
  }


  private CaseInfo createCaseInfo(ExceptionVariable caughtException) {
    String fqn = caughtException.getOriginalPath() + "." + caughtException.getOriginalName();
    return new CaseInfo(creator.getNextXmlId().toString(), caughtException.getLabel(), fqn, "", this, false);
  }


  @Override
  public String getOuterConditionForGUI() {
    return getOuterCondition();
  }

  @Override
  public DistinctionType getDistinctionType() {
    return DistinctionType.ExceptionHandling;
  }

  @Override
  public String getOuterCondition() {
    return "";
  }

  @Override
  protected void collectServiceReferences(Set<Service> serviceReferences) throws XPRC_InvalidServiceIdException {
    for (String exceptionId : exceptionVariableIdsToCatch.keySet()) {
      exceptionVariableIdsToCatch.get(exceptionId).collectServiceReferences(serviceReferences);
    }

    super.collectServiceReferences(serviceReferences);
  }

  public void createEmpty() {
    setXmlId(creator.getNextXmlId());
  }

  public void updateRetryHandlers() {
    exceptionHandlersWithRetry.clear();

    for (String id : exceptionVariableIdsToCatch.keySet()) {
      for (Step laneStep : exceptionVariableIdsToCatch.get(id).getChildSteps()) {
        if (Step.containsOrIsAssignableFrom(laneStep, StepRetry.class)) {
          exceptionHandlersWithRetry.add(id);
          break;
        }
      }
    }
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    getStepInTryBlock().appendXML(xml);
  }

  public void appendCatchAreas(XmlBuilder xml) {
    for (String exceptionId : getAllUsedVariableIds()) {
      xml.startElementWithAttributes(EL.CATCH); {
        Step catchStep = exceptionVariableIdsToCatch.get(exceptionId);
        String catchId = catchStep.getXmlId().toString();
        xml.addAttribute(ATT.ID, catchId);
        xml.addAttribute(ATT.EXCEPTION_ID, exceptionId);
        xml.endAttributes();

        catchStep.appendXML(xml);
      } xml.endElement(EL.CATCH);
    }
  }

}
