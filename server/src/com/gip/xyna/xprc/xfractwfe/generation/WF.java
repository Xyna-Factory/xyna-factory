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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.InterfaceResolutionContext;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InconsistentFileNameAndContentException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.exceptions.XPRC_TOO_FEW_PROCESS_INPUT_PARAMETERS;
import com.gip.xyna.xprc.exceptions.XPRC_TOO_MANY_PROCESS_INPUT_PARAMETERS;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.Scope;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.Step.Catchable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


public class WF extends GenerationBase implements HasDocumentation, HasMetaTags {

  private static Logger logger = CentralFactoryLogging.getLogger(WF.class);
  
  private static final String _METHODNAME_GET_NEEDED_INPUT_VARS_COUNT_ORIG = "getNeededInputVarsCount";
  protected static final String METHODNAME_GET_NEEDED_INPUT_VARS_COUNT;
  private static final String _METHODNAME_INITIALIZE_MEMBER_VARS_ORIG = "initializeMemberVars";
  protected static final String METHODNAME_INITIALIZE_MEMBER_VARS;
  private static final String _METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE_ORIG = "throwExceptionOfMismatchingType";
  protected static final String METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE;
  private static final String _METHODNAME_GET_ORIGINAL_NAME_ORIG = "getOriginalName";
  protected static final String METHODNAME_GET_ORIGINAL_NAME;
  private static final String _METHODNAME_ON_DEPLOYMENT_ORIG = "onDeployment";
  protected static final String METHODNAME_ON_DEPLOYMENT;
  private static final String _METHODNAME_ON_UNDEPLOYMENT_ORIG = "onUndeployment";
  protected static final String METHODNAME_ON_UNDEPLOYMENT;
  private static final String _METHODNAME_IS_ATTEMPTING_SUSPENSION_ORIG = "isAttemptingSuspension";
  protected static final String METHODNAME_IS_ATTEMPTING_SUSPENSION;
  private static final String _METHODNAME_GET_CORRELATED_XYNA_ORDER_ORIG = "getCorrelatedXynaOrder";
  protected static final String METHODNAME_GET_CORRELATED_XYNA_ORDER;
  private static final String _METHODNAME_IS_GENERATED_AS_INVALID_ORIG = "isGeneratedAsInvalid";
  protected static final String METHODNAME_IS_GENERATED_AS_INVALID;
  private static final String _FIELDNAME_RETRY_COUNTER_ORIG = "retryCounter";
  protected static final String FIELDNAME_RETRY_COUNTER;
  private static final String _FIELDNAME_INSTANCE_METHOD_TYPES_ORIG = "instanceMethodTypes";
  protected static final String FIELDNAME_INSTANCE_METHOD_TYPES;
  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_GET_NEEDED_INPUT_VARS_COUNT = XynaProcess.class.getDeclaredMethod(_METHODNAME_GET_NEEDED_INPUT_VARS_COUNT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_NEEDED_INPUT_VARS_COUNT_ORIG + " not found", e);
    }
    try {
      METHODNAME_INITIALIZE_MEMBER_VARS = XynaProcess.class.getDeclaredMethod(_METHODNAME_INITIALIZE_MEMBER_VARS_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_INITIALIZE_MEMBER_VARS_ORIG + " not found", e);
    }
    try {
      METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE = XynaProcess.class.getDeclaredMethod(_METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE_ORIG, int.class, Class.class, Class.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_THROW_EXCEPTION_OF_MISMATCHING_TYPE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_ORIGINAL_NAME = XynaProcess.class.getDeclaredMethod(_METHODNAME_GET_ORIGINAL_NAME_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_ORIGINAL_NAME_ORIG + " not found", e);
    }
    try {
      METHODNAME_ON_DEPLOYMENT = XynaProcess.class.getDeclaredMethod(_METHODNAME_ON_DEPLOYMENT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_ON_DEPLOYMENT_ORIG + " not found", e);
    }
    try {
      METHODNAME_ON_UNDEPLOYMENT = XynaProcess.class.getDeclaredMethod(_METHODNAME_ON_UNDEPLOYMENT_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_ON_UNDEPLOYMENT_ORIG + " not found", e);
    }
    try {
      METHODNAME_IS_ATTEMPTING_SUSPENSION = XynaProcess.class.getDeclaredMethod(_METHODNAME_IS_ATTEMPTING_SUSPENSION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_ATTEMPTING_SUSPENSION_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CORRELATED_XYNA_ORDER = XynaProcess.class.getDeclaredMethod(_METHODNAME_GET_CORRELATED_XYNA_ORDER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CORRELATED_XYNA_ORDER_ORIG + " not found", e);
    }
    try {
      METHODNAME_IS_GENERATED_AS_INVALID = XynaProcess.class.getDeclaredMethod(_METHODNAME_IS_GENERATED_AS_INVALID_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_GENERATED_AS_INVALID_ORIG + " not found", e);
    }
    
    try {
      FIELDNAME_RETRY_COUNTER = XynaProcess.class.getDeclaredField(_FIELDNAME_RETRY_COUNTER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _FIELDNAME_RETRY_COUNTER_ORIG + " not found", e);
    }
    try {
      FIELDNAME_INSTANCE_METHOD_TYPES = XynaProcess.class.getDeclaredField(_FIELDNAME_INSTANCE_METHOD_TYPES_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _FIELDNAME_INSTANCE_METHOD_TYPES_ORIG + " not found", e);
    }
  }

  private ArrayList<AVariable> inputVars = new ArrayList<AVariable>();
  private ArrayList<AVariable> outputVars = new ArrayList<AVariable>();
  private ArrayList<ExceptionVariable> thrownExceptionVariables = new ArrayList<ExceptionVariable>();

  private String documentation = "";
  private UnknownMetaTagsComponent unknownMetaTagsComponent = new UnknownMetaTagsComponent();
  private SpecialPurposeIdentifier specialPurposeIdentifier;

  @Override
  public void parseUnknownMetaTags(Element element, List<String> knownMetaTags) {
    unknownMetaTagsComponent.parseUnknownMetaTags(element, knownMetaTags);
  }

  @Override
  public boolean hasUnknownMetaTags() {
    return unknownMetaTagsComponent.hasUnknownMetaTags();
  }
  

  @Override
  public List<String> getUnknownMetaTags() {
    return unknownMetaTagsComponent.getUnknownMetaTags();
  }

  @Override
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    unknownMetaTagsComponent.setUnknownMetaTags(unknownMetaTags);
  }

  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {
    unknownMetaTagsComponent.appendUnknownMetaTags(xml);
  }

  private WFStep wfAsStep;


  public class WFStep extends ScopeStep implements Catchable {

    private XynaPropertySupport xynaPropertySupport;
    //Xyna Properties of Subscopes must be initialized in that scope, but to be abled to register and unregister them,
    //the static instance of the XynaProperty must be in the parent Scope
    private List<XynaPropertySupport> xynaPropertySupportOfSubscopes = new ArrayList<>();

    public WFStep(AVariable[] inputVars, AVariable[] outputVars, WF creator) {
      super(inputVars, outputVars, creator);
      this.xynaPropertySupport = new XynaPropertySupport(creator);
    }


    public Set<Step> getAllStepsRecursively() {
      Set<Step> steps = new LinkedHashSet<Step>();
      steps.add(this);
      steps.addAll(getChildSteps());
      for (Step s : getChildSteps()) {
        addChildStepsRecursively(steps, s);
      }

      return steps;
    }


    @Override
    protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException,
        XPRC_MissingContextForNonstaticMethodCallException, XPRC_OperationUnknownException, XPRC_InvalidServiceIdException,
        XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {

      List<Step> allLocalSteps = getAllLocalSubSteps(true);

      xynaPropertySupport.checkForXynaProperties(inputVars);
      xynaPropertySupport.checkForXynaProperties(outputVars);
      for (Step s : allLocalSteps) {
        if (s instanceof ForEachScopeStep) {
          xynaPropertySupportOfSubscopes.addAll(((ForEachScopeStep) s).discoverXynaPropertySupportRecursively());
          continue;
        }
        xynaPropertySupport.checkForXynaProperties(s.getServiceVariables());
      }

      super.generateJavaInternally(cb, importedClassesFqStrings);
    }


    @Override
    protected void generateJavaStaticInitialization(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
      xynaPropertySupport.generateXynaProperties(cb);
      for(XynaPropertySupport supp: xynaPropertySupportOfSubscopes) {
        supp.generateXynaProperties(cb);
      }
    }


    @Override
    protected void generateJavaAdditionalInitializeMemberVars(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
      xynaPropertySupport.generateXynaPropertyAssignment(cb);
    }
    
    @Override
    protected void generateJavaAdditionalReadWriteObject(CodeBuffer cb, Set<String> importedClassesFqStrings, boolean read ) {
      xynaPropertySupport.generateJavaReadWriteObject(cb, read);
    }


    @Override
    protected void generateJavaAdditionalPreEnd(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {

      cb.addLB().addLine("public ", String.class.getName(), " ", METHODNAME_GET_ORIGINAL_NAME, "() {");
      cb.addLine("return \"" + getOriginalFqName() + "\"");
      cb.addLine("}").addLB();

      cb.addLine("protected void ", METHODNAME_ON_DEPLOYMENT, "() throws ", XynaException.class.getName(), " {");
      cb.addLine("if (logger.isDebugEnabled()) {");
      for (AVariable v : inputVars) {
        cb.addLine("logger.debug(this + \" \" + " + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
            + ".class.getClassLoader())");
        // cb.addLine(v.getVarName() + ".onDeployment()");
      }
      cb.addLine("}");
      // TODO statische services initialisieren (nur für performance)
      xynaPropertySupport.generateJavaOnDeployment(cb);
      for(XynaPropertySupport supp: xynaPropertySupportOfSubscopes) {
        supp.generateJavaOnDeployment(cb);
      }
      cb.addLine("}").addLB();
      cb.addLine("protected void ", METHODNAME_ON_UNDEPLOYMENT, "() throws ", XynaException.class.getName(), " {");
      // TODO statische services deinitialisieren
      xynaPropertySupport.generateJavaOnUndeployment(cb);
      for(XynaPropertySupport supp: xynaPropertySupportOfSubscopes) {
        supp.generateJavaOnUndeployment(cb);
      }
      cb.addLine("}").addLB();
    }


    @Override
    protected void generateJavaClassHeader(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
      cb.addLB(2).addLine("public class ", getSimpleClassName(), " extends ", XynaProcess.class.getSimpleName(), " {").addLB();
      cb.addLine("private static Logger logger = Logger.getLogger(\"debug\")");
      cb.addLB();
      cb.addLine("private static final long serialVersionUID = ", String.valueOf(calculateSerialVersionUID()), "L");
      cb.addLB();
    }


    @Override
    public String getClassName() {
      return WF.this.getSimpleClassName();
    }


    @Override
    protected void generateJavaConstructor(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
    }


    @Override
    protected void generateJavaStepMethods(CodeBuffer cb, HashSet<String> importedClassesFqStrings) {
      //kein methoden für executeinternally oder compensateinternally die es in step klasse gibt.
    }


    public WF getWF() {
      return WF.this;
    }


    @Override
    public List<Step> getChildSteps() {
      List<Step> children = new ArrayList<Step>();
      /*
       *  WF -> stepserial -> childsteps
       *                   -> catchstep (globalcatch) -> try (-> stepserial (gleiches wie oben!!!))
       *                                              -> catchblöcke
       *                                              
       * TODO: Achtung, wenn man eine Rekursion über alle Schritte des Workflows machen möchte, kommt man über den globalen
       *       catchblock bei allen Steps doppelt vorbei.
       *       Wenn man in der Rekursion ein Set von Steps hat und damit Dupliakte verhindert, schadet das wenig, 
       *       aber es gibt auch Codepfade, die das nicht tun!
       */
      children.add(childStep);
      if (childStep.getProxyForCatch() != null && childStep.getProxyForCatch() != childStep) { //globaler catchblock
        children.add(childStep.getProxyForCatch());
      }
      return children;
    }


    public void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
        XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_InvalidVariableIdException,
        XPRC_ParsingModelledExpressionException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidXMLMissingListValueException,
        XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException,
        XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
      Set<Step> allStepsExcludingThis = getAllStepsRecursively();
      allStepsExcludingThis.remove(this);
      for (Step s : allStepsExcludingThis) {
        s.validate();
      }
    }

    @Override
    public ExceptionVariable[] getExceptionVars() {
      List<ExceptionVariable> exceptionVarList = getWF().getExceptionVars();
      return getWF().getExceptionVars().toArray(new ExceptionVariable[exceptionVarList.size()]);
    }

    @Override
    public String[] getInputVarIds() {
      String[] inputVarIds = new String[inputVars.size()];
      for (int varNo = 0; varNo < inputVars.size(); varNo++) {
        try {
          AVariable var = identifyVariable(inputVars.get(varNo).getId()).getVariable();
          inputVarIds[varNo] = var.getTargetId();
        } catch (XPRC_InvalidVariableIdException e) {
          logger.warn("Couldn't find input variable for id " + outputVars.get(varNo).getId(), e);
        }
      }

      return inputVarIds;
    }

    @Override
    public String[] getOutputVarIds() {
      String[] outputVarIds = new String[outputVars.size()];
      for (int varNo = 0; varNo < outputVars.size(); varNo++) {
        try {
          AVariable var = identifyVariable(outputVars.get(varNo).getId()).getVariable();
          Iterator<String> it = var.getSourceIds().iterator();
          outputVarIds[varNo] = it.hasNext() ? it.next() : null;
        } catch (XPRC_InvalidVariableIdException e) {
          logger.warn("Couldn't find output variable for id " + outputVars.get(varNo).getId(), e);
        }
      }

      return outputVarIds;
    }

    @Override
    protected void setSourceAndTargetIds(Parameter parameter) {
      // Inputs and outputs are reversed for workflow in comparison to a normal step
      setSourceIds(parameter.getOutputData(), getOutputVarIds());
      setTargetIds(parameter.getInputData(), getInputVarIds());
    }

    @Override
    public Step getProxyForCatch() {
      StepSerial childStep = getChildStep();
      if (childStep == null) {
        return null;
      }

      return childStep.getProxyForCatch();
    }

    @Override
    public void setCatchStep(StepCatch catchStep) {
      StepSerial childStep = getChildStep();
      if (childStep == null) {
        return;
      }

      childStep.setCatchStep(catchStep);
    }

    @Override
    public List<ExceptionVariable> getAllThrownExceptions(boolean considerRetryAsHandled) {
      StepSerial childStep = getChildStep();
      if (childStep == null) {
        return null;
      }

      return childStep.getProxyForCatch().getAllThrownExceptions(considerRetryAsHandled);
    }

    @Override
    public Step getContainerStepForGui() {
      return this;
    }

    @Override
    public boolean isInRetryLoop() {
      if (super.isInRetryLoop()) {
        return true;
      }

      List<Step> topLevelSteps = getChildSteps();
      if (topLevelSteps.size() == 1) {
        return false; // no catch, hence no retries
      } 

      if (!(topLevelSteps.get(1) instanceof StepCatch)) {
        // second top-level step is of unexpected type
        logger.error("second top-level step is of unexpected type " + topLevelSteps.get(1).getClass() + " (StepCatch was expected)");
        return false;
      }

      StepCatch wfCatch = (StepCatch)topLevelSteps.get(1);
      if (wfCatch.hasExecutedRetry()) {
        // workflow has a global catch with an executed retry -> check if any iterations occurred
        Set<Step> recursiveChildren = new HashSet<>();
        WF.addChildStepsRecursively(recursiveChildren, this);
        for (Step child : recursiveChildren) {
          List<Parameter> parameterList = child.getParameterList();
          if (parameterList != null && parameterList.stream().anyMatch(x -> x.getRetryCounter() > 0)) {
            return true;
          }
        }
      }

      return false;
    }

  }

  /**
   * Rekursives Sammlen der InputSources:
   * 
   * in referencedOrderInputSources alle stepfunctions die inputsources referenziert, die nicht in
   * - choices/verzweigungen
   * - foreaches
   * - catch blöcken
   * sind.
   * 
   * in allReferencedOrderInputSources alle inputsources
   */
  private static void collectOrderInputSources(Map<String, String> preparableReferencedOrderInputSources, Set<String> allReferencedOrderInputSources, Step step, AtomicInteger cnt) {
    List<Step> childSteps = step.getChildSteps();
    for (Step s : childSteps) {
      if (s instanceof ScopeStep) {
        collectOrderInputSources(preparableReferencedOrderInputSources, allReferencedOrderInputSources, s, cnt);
      } else if (s instanceof StepAssign) {
      } else if (s instanceof StepCatch) {
        StepCatch sc = (StepCatch) s;
        collectOrderInputSources(null, allReferencedOrderInputSources, sc, cnt);
        //wurde bereits oben mitgezählt
        collectOrderInputSources(preparableReferencedOrderInputSources, allReferencedOrderInputSources, sc.getStepInTryBlock(), new AtomicInteger(0));
      } else if (s instanceof StepChoice) {
        collectOrderInputSources(null, allReferencedOrderInputSources, s, cnt);
      } else if (s instanceof StepForeach) {
        collectOrderInputSources(null, allReferencedOrderInputSources, s, cnt);
      } else if (s instanceof StepFunction) {
        StepFunction sf = (StepFunction) s;
        if (sf.getOrderInputSourceRef() != null) {
          if (preparableReferencedOrderInputSources != null) {
            preparableReferencedOrderInputSources.put(sf.getUniqueStepId(), sf.getOrderInputSourceRef());
          }
          allReferencedOrderInputSources.add(sf.getOrderInputSourceRef());
          cnt.incrementAndGet();
        }
        if (sf.getChildSteps().size() > 0) {
          collectOrderInputSources(null, allReferencedOrderInputSources, s, cnt);
        }
      } else if (s instanceof StepMapping) {
      } else if (s instanceof StepRetry) {
      } else if (s instanceof StepSerial) {
        //parallel ebeneso
        collectOrderInputSources(preparableReferencedOrderInputSources, allReferencedOrderInputSources, s, cnt);
      } else if (s instanceof StepThrow) {
      } else {
        throw new RuntimeException("unexpected step type " + s.getClass().getName());
      }
    }
  }

  public static void addChildStepsRecursively(Set<Step> steps, Step s) {
    List<Step> children = s.getChildSteps();
    if (children != null) {
      for (Step child : children) {
        if (steps.add(child)) {
          addChildStepsRecursively(steps, child);
        }
      }
    }
  }


  private WF(String originalName, String wfInputName, Long revision) {
    super(originalName, wfInputName, revision);
  }


  WF(String originalName, String wfInputName, GenerationBaseCache cache, Long revision, String realType) {
    super(originalName, wfInputName, cache, revision, realType);
  }
  
  WF(String originalName, String wfInputName, GenerationBaseCache cache, Long revision, String realType, XMLSourceAbstraction inputSource) {
    super(originalName, wfInputName, cache, revision, realType, inputSource);
  }


  public WF getParentWF() {
    return this;
  }


  static ReentrantLock cacheLockWF = new ReentrantLock();


  public static WF getInstance(String originalWFInputName) throws XPRC_InvalidPackageNameException {
    return getInstance(originalWFInputName, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public static WF getInstance(String originalWFInputName, Long revision) throws XPRC_InvalidPackageNameException {

    String fqClassName = GenerationBase.transformNameForJava(originalWFInputName);
    
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalWFInputName, revision);

    GenerationBase o = GenerationBase.tryGetGlobalCachedInstance(originalWFInputName, revision);
    if (o == null) {
      cacheLockWF.lock();
      try {
        o = GenerationBase.tryGetGlobalCachedInstance(originalWFInputName, revision);
        if (o == null) {
          o = new WF(originalWFInputName, fqClassName, revision);
          GenerationBase.cacheGlobal(o);
        }
      } finally {
        cacheLockWF.unlock();
      }
    }

    if (!(o instanceof WF)) {
      // TODO throw checked
      throw new RuntimeException(new XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH(originalWFInputName, "WF", o.getClass().getSimpleName()));
    }
    return (WF) o;
  }


  public static WF getOrCreateInstance(String originalWFInputName, GenerationBaseCache cache, Long revision)
      throws XPRC_InvalidPackageNameException {
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalWFInputName, revision);
    
    String fqClassName = GenerationBase.transformNameForJava(originalWFInputName);
    WF wf = (WF) cache.getFromCache(originalWFInputName, revision);
    if (wf == null) {
      wf = new WF(originalWFInputName, fqClassName, cache, revision, null);
      cache.insertIntoCache(wf);
    }

    return wf;
  }
  
  public static WF getOrCreateInstanceForAudits(String originalWFInputName, Map<String, String> xmlsWfAndImports)
                  throws XPRC_InvalidPackageNameException {
    String fqClassName = GenerationBase.transformNameForJava(originalWFInputName);
    StringXMLSource inputSource = new StringXMLSource(xmlsWfAndImports);
    
    return new WF(originalWFInputName, fqClassName, new GenerationBaseCache(), StringXMLSource.REVISION, null, inputSource);
  }

  public static WF createNewWorkflow(String originalWFInputName, GenerationBaseCache cache, Long revision) throws XPRC_InvalidPackageNameException {
    String fqClassName = GenerationBase.transformNameForJava(originalWFInputName);
    WF wf = (WF) cache.getFromCache(originalWFInputName, revision);
    if (wf != null) {
      throw new RuntimeException("WF already exists!"); //FIXME
    }
    wf = new WF(originalWFInputName, fqClassName, cache, revision, null);
    cache.insertIntoCache(wf);
    
    wf.wfAsStep = wf.new WFStep(new AVariable[]{}, new AVariable[]{}, wf);
    StepSerial s = new StepSerial(wf.wfAsStep, wf);
    
    StepCatch stepCatch = new StepCatch(s.getParentScope(), s, s.getCreator()); // TODO: wieder rein
    s.setCatchStep(stepCatch);
    
    StepAssign sa = new StepAssign(wf.wfAsStep, wf);
    sa.createEmpty();
    s.addChild(0, sa);
    
    wf.wfAsStep.setChildStep(s);
    s.setXmlId(0);
    
    return wf;
  }
  

  public static WF generateUncachedInstance(String originalWFInputName, boolean fromDeploymentLocation, Long revision)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
      XPRC_MDMDeploymentException {
    revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getRevisionDefiningXMOMObjectOrParent(originalWFInputName, revision);
    WF wf = getOrCreateInstance(originalWFInputName, new GenerationBaseCache(), revision);
    wf.parseGeneration(fromDeploymentLocation, false, false);
    return wf;
  }


  //true = there are Implementation changes
  @Override
  public boolean compareImplementation(GenerationBase oldVersion) {
    if (oldVersion != null && !(oldVersion instanceof WF)) {
      return true;
    }
    WF oldWF = (WF) oldVersion;
    //inputVars
    if (inputVars != null && oldWF.inputVars != null) {
      if (inputVars.size() != oldWF.inputVars.size()) {
        return true;
      }
      for (int i = 0; i < inputVars.size(); i++) {
        if (!Objects.equals(inputVars.get(i).getFQClassName(), oldWF.inputVars.get(i).getFQClassName())) {
          return true;
        }
        if (inputVars.get(i).isList != oldWF.inputVars.get(i).isList) {
          return true;
        }
      }
    } else if (inputVars != null || oldWF.inputVars != null) {
      return true;
    }
    //else beide null oder beide gleich

    //outputVars
    if (outputVars != null && oldWF.outputVars != null) {
      if (outputVars.size() != oldWF.outputVars.size()) {
        return true;
      }
      for (int i = 0; i < outputVars.size(); i++) {
        if (!Objects.equals(outputVars.get(i).getFQClassName(), oldWF.outputVars.get(i).getFQClassName())) {
          return true;
        }
        if (outputVars.get(i).isList != oldWF.outputVars.get(i).isList) {
          return true;
        }
      }
    } else if (outputVars != null || oldWF.outputVars != null) {
      return true;
    }

    //else beide null oder beide gleich
    if (wfAsStep == null) {
      return oldWF.wfAsStep == null;
    } else {
      return wfAsStep.compareImplementation(oldWF.wfAsStep);
    }
  }


  @Override
  protected String[] generateJavaInternally(CodeBuffer cb, boolean compileSafe) throws XPRC_InvalidVariableIdException,
      XPRC_MissingContextForNonstaticMethodCallException, XPRC_OperationUnknownException, XPRC_InvalidServiceIdException,
      XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {

    boolean invalid = false;

    DeploymentItemStateManagement dism = GenerationBase.getDeploymentItemStateManagement();
    if (dism != null) {
      DeploymentItemState dis = dism.get(getOriginalFqName(), revision);
      if (XynaProperty.INVALIDATE_WF_EXECUTION.get() && dis != null) {
        //von SAVED aus schauen. wenn das objekt kopiert wird. es ist zwar schon nach deployed kopiert, aber das deploymentitemstatemanagement noch nicht angepasst
        //das passiert erst im cleanup/onerror
        DeploymentLocation source =
            getDeploymentMode().shouldCopyXMLFromSavedToDeployed() ? DeploymentLocation.SAVED : DeploymentLocation.DEPLOYED;
        Set<DeploymentItemInterface> invalid_sd = dis.getInconsistencies(source, DeploymentLocation.DEPLOYED, false);
        Set<DeploymentItemInterface> invalid_ss;
        if (XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().isApplicationRevision(revision)) {
          // es kann in Applications keine Inkonsistenzen zu Saved-Zuständen geben
          invalid_ss = Collections.emptySet();
        } else {
          invalid_ss = dis.getInconsistencies(source, DeploymentLocation.SAVED, false);
        }
        if (invalid_sd.size() == 0) {
          //checken, ob es probleme gibt mit anderen objekten, die auch nach deployed kopiert werden
          invalid = false;
          for (DeploymentItemInterface diii : invalid_ss) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb = cacheReference.getFromCacheInCorrectRevision(prov.getName(), revision); //ss_invalid: muss gleiche revision sein
                invalid = gb != null && gb.getDeploymentMode().shouldCopyXMLFromSavedToDeployed() && !gb.hasError(); //!hasError, weil sonst backup passieren würde
                if (invalid) {
                  break;
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        } else if (invalid_ss.size() <= 0) {
          //checken, dass nicht alle deployed/deployed inkonsistenzen verschwinden, weil sie durch saved ersetzt werden
          //d.h. es muss mindestens eine deployed-deployed inkonsistenz geben, die nicht nach deployed kopiert wird
          invalid = false;
          for (DeploymentItemInterface diii : invalid_sd) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb;
                try {
                  gb = cacheReference.getFromCacheInCorrectRevision(GenerationBase.transformNameForJava(prov.getName()), revision);
                  if (gb == null || !gb.getDeploymentMode().shouldCopyXMLFromSavedToDeployed() || gb.hasError()) {
                    invalid = true;
                    break;
                  }
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new RuntimeException(e);
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        } else {
          //es gibt saved-saved inkonsistenzen und saved-deployed ebenso. es kann trotzdem sein, dass die mengen disjunkt sind, und deshalb
          //die saved-deployed inkonsistenzen verschwinden
          //es muss also überprüft werden, dass jede s-d inkonsistenz verschwindet
          invalid = false;
          for (DeploymentItemInterface diii : invalid_sd) {
            if (!(diii instanceof UnresolvableInterface)) {
              TypeInterface prov = InterfaceResolutionContext.getProviderType(diii);
              if (prov != null) {
                GenerationBase gb;
                try {
                  gb = cacheReference.getFromCacheInCorrectRevision(GenerationBase.transformNameForJava(prov.getName()), revision);
                  if (gb != null && gb.getDeploymentMode().shouldCopyXMLFromSavedToDeployed() && !gb.hasError()) {
                    //ok, objekt wird nach deployed kopiert - hat es die gleiche inkonsistenz in ss?
                    String descr = diii.getDescription();
                    boolean found = false;
                    for (DeploymentItemInterface diiiss : invalid_ss) {
                      //FIXME performance: alle deploymentiteminterfaces sollten matchableinterfaces sein oder equals implementieren etc 
                      if (!descr.equals(diiiss.getDescription())) {
                        found = true;
                        break;
                      }
                    }
                    if (!found) {
                      invalid = true;
                      break;
                    }
                  } else {
                    //objekt wird nicht repariert
                    invalid = true;
                    break;
                  }
                } catch (XPRC_InvalidPackageNameException e) {
                  throw new RuntimeException(e);
                }
              } else {
                invalid = true;
                break;
              }
            }
          }
        }
      }
    }


    HashSet<String> importedClasseNames = new HashSet<String>();

    {
      // this set is only required for the following import creation. the imported class names set is required below.
      HashSet<String> importedSimpleClasseNames = new HashSet<String>();

      if (!isEmpty(getOriginalPath())) {
        cb.addLine("package ", getPackageNameFromFQName(getFqClassName())).addLB();
      } else {
        cb.addLine("package ", DEFAULT_PACKAGE).addLB();
      }

      // bugz 9525: the workflow name itself is considered "imported"
      importedSimpleClasseNames.add(getSimpleClassName());

      boolean previousContainedJavaPrefix = false;
      boolean previousContainedXynaPrefix = false;
      Set<String> imports = invalid ? getDefaultImports() : getImports();
      for (String i : imports) {
        if (i.contains(".")) {
          String currentSimpleClassName = i.substring(i.lastIndexOf(".") + 1);
          if (!importedSimpleClasseNames.contains(currentSimpleClassName) && !getSimpleClassName().equals(currentSimpleClassName)) {

            if (i.startsWith("java.")) {
              if (!previousContainedJavaPrefix) {
                cb.addLB();
              }
              previousContainedJavaPrefix = true;
              previousContainedXynaPrefix = false;
            } else if (i.startsWith("com.gip.xyna.")) {
              if (!previousContainedXynaPrefix) {
                cb.addLB();
              }
              previousContainedXynaPrefix = true;
              previousContainedJavaPrefix = false;
            } else {
              if (previousContainedJavaPrefix || previousContainedXynaPrefix) {
                cb.addLB();
              }
              previousContainedXynaPrefix = false;
              previousContainedJavaPrefix = false;
            }

            importedSimpleClasseNames.add(currentSimpleClassName);
            importedClasseNames.add(i);
            cb.addLine("import " + i);

          }
        } else {
          cb.addLine("import " ,DEFAULT_PACKAGE, "." + i);
        }
      }

    }

    if (invalid) {
      generateEmptyClass(cb, invalid, importedClasseNames);
      return new String[] {cb.toString()};
    }

    wfAsStep.generateJava(cb, importedClasseNames);
    return new String[] {cb.toString()};
  }


  private void generateEmptyClass(CodeBuffer cb, boolean invalid, HashSet<String> importedClassesFqStrings) {
    wfAsStep.generateJavaClassHeader(cb, importedClassesFqStrings);

    cb.addLine("public void ", ScopeStep.METHODNAME_SET_INPUT_VARS, "(", GeneralXynaObject.class.getSimpleName(), " o) throws ",
                 XynaException.class.getSimpleName(), " {").addLine("}").addLB();

    cb.addLine("public ", GeneralXynaObject.class.getSimpleName(), " ", ScopeStep.METHODNAME_GET_OUTPUT, "() {").addLine("return null").addLine("}").addLB();

    cb.addLine("public ", FractalProcessStep.class.getSimpleName(), "[] ", ScopeStep.METHODNAME_GET_START_STEPS, "() {")
      .addLine("return new ", FractalProcessStep.class.getName(), "[0]").addLine("}").addLB();

    cb.addLine("public " , FractalProcessStep.class.getSimpleName(), "[] ", ScopeStep.METHODNAME_GET_ALL_STEPS, "() {")
      .addLine("return new ", FractalProcessStep.class.getName(), "[0]").addLine("}").addLB();
    
    cb.addLine("public " , FractalProcessStep.class.getSimpleName(), "[] ", ScopeStep.METHODNAME_GET_ALL_LOCAL_STEPS, "() {")
      .addLine("return new ", FractalProcessStep.class.getName(), "[0]").addLine("}").addLB();

    cb.addLB().addLine("protected void ", METHODNAME_INITIALIZE_MEMBER_VARS, "() {")
        .addLine("throw new RuntimeException(\"", getOriginalFqName(), " is invalid\")").addLine("}").addLB();

    cb.addLB().addLine("public String ", METHODNAME_GET_ORIGINAL_NAME, "() {");
    cb.addLine("return \"" ,getOriginalFqName(), "\"");
    cb.addLine("}").addLB();

    cb.addLine("protected void ", METHODNAME_ON_DEPLOYMENT, "() throws ", XynaException.class.getName(), " {").addLine("}").addLB();
    cb.addLine("protected void ", METHODNAME_ON_UNDEPLOYMENT, "() throws ", XynaException.class.getName(), " {").addLine("}").addLB();

    if (invalid) {
      //!!!!!!!!!!! ACHTUNG! absichtlich "+" konkateniert, ansonsten wird am ende ein semikolon angehängt 
      cb.addLB().addLine("@" + Override.class.getName());
      cb.addLine("public boolean ", METHODNAME_IS_GENERATED_AS_INVALID, "() {");
      cb.addLine("return true");
      cb.addLine("}").addLB();
    }
    
    cb.addLine("}").addLB();
  }


  @Override
  public Set<GenerationBase> getDirectlyDependentObjects() {
    Set<GenerationBase> result = new HashSet<GenerationBase>();
    for (AVariable inputVar : inputVars) {
      if (inputVar.getDomOrExceptionObject() != null) {
        result.add(inputVar.getDomOrExceptionObject()); //muss nicht getDependencies aufgerufen werden, weil inputs keine konstant vorbelegten members haben
      }
    }
    for (AVariable outputVar : outputVars) {
      if (outputVar.getDomOrExceptionObject() != null) {
        result.add(outputVar.getDomOrExceptionObject()); //muss nicht getDependencies aufgerufen werden, weil konstante vorbelegung über ein assign passiert
      }
    }

    // if we havent parsed before but want to cleanup, wfAsStep is null
    if (wfAsStep != null && XynaFactory.hasInstance()) {
      for (Step s : wfAsStep.getAllStepsRecursively()) {
        List<GenerationBase> deps = s.getDependencies();
        if (deps != null) {
          for (GenerationBase gb : deps) {
            result.add(gb);
          }
        }
      }
    }
    for (AVariable v : unusedVariables) {
      if (v.getDomOrExceptionObject() != null) {
        result.addAll(v.getDependencies());
      }
    }
    for (ExceptionVariable v : thrownExceptionVariables) {
      if (v.getDomOrExceptionObject() != null) {
        result.add(v.getDomOrExceptionObject());
      }
    }
    return result;
  }


  public void clearUnusedVariables() {
    moveVariablesToPrivateScopes();
    unusedVariables.clear();
  }


  @Override
  protected void parseXmlInternally(Element rootElement) throws XPRC_InvalidPackageNameException,
      XPRC_InconsistentFileNameAndContentException, XPRC_InvalidXmlMissingRequiredElementException {

    validateClassName(rootElement.getAttribute(GenerationBase.ATT.TYPEPATH), rootElement.getAttribute(GenerationBase.ATT.TYPENAME));

    setLabel(rootElement.getAttribute(GenerationBase.ATT.LABEL));

    List<String> knownMetatags = new ArrayList<String>();
    knownMetatags.add(GenerationBase.EL.DOCUMENTATION);
    parseUnknownMetaTags(rootElement, knownMetatags);
    Element metaElement = XMLUtils.getChildElementByName(rootElement, GenerationBase.EL.META);
    if (metaElement != null) {
      Element documentationElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.DOCUMENTATION);
      
      //remove meta element - otherwise wee parse it again (together with services, data and functions
      rootElement.removeChild(metaElement);
      
      if (documentationElement != null) {
        documentation = XMLUtils.getTextContent(documentationElement);
      } else {
        documentation = null;
      }
    }
    
    Element operation = XMLUtils.getChildElementByName(rootElement, com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL.OPERATION);
    if (operation == null) {
      throw new XPRC_InvalidXmlMissingRequiredElementException("root", GenerationBase.EL.OPERATION);
    }
    
    specialPurposeIdentifier = parseSpecialPurposeIdentifier(operation);

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(getOriginalFqName()).append(": parsing input data elements"));
    }
    inputVars.clear();
    inputVars.addAll( Service.parseInputOutput( operation, GenerationBase.EL.OPERATION, GenerationBase.EL.INPUT, this ) );
    
    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(getOriginalFqName()).append(": parsing output data elements of workflow ")
          .append(getSimpleClassName()));
    }
    outputVars.clear();
    outputVars.addAll( Service.parseInputOutput( operation, GenerationBase.EL.OPERATION, GenerationBase.EL.OUTPUT, this ) );
    

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(getOriginalFqName()).append(": parsing exceptions thrown by workflow ")
          .append(getSimpleClassName()));
    }
    thrownExceptionVariables.clear();
    Element throwsElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.THROWS);
    if (throwsElement != null) {
      List<Element> temporaryElementsList = XMLUtils.getChildElementsByName(throwsElement, GenerationBase.EL.EXCEPTION);
      for (Element thrownExceptionElement : temporaryElementsList) {
        ExceptionVariable newThrownException = new ExceptionVariable(this);
        thrownExceptionVariables.add(newThrownException);
        newThrownException.parseXML(thrownExceptionElement);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(new StringBuilder().append(getOriginalFqName()).append(": parsing services, data elements and functions"));
    }
    wfAsStep = new WFStep(inputVars.toArray(new AVariable[0]), outputVars.toArray(new AVariable[0]), this);
    StepSerial s = new StepSerial(wfAsStep, this);
    wfAsStep.setChildStep(s);

    // parse services, data und functions
    wfAsStep.parseXML(operation);

    assignIdsToVariables();
    
    unusedVariables.clear();
    moveVariablesToPrivateScopes();

    preparableReferencedOrderInputSources = new HashMap<String, String>();
    allReferencedOrderInputSources = new HashSet<String>();
    AtomicInteger cnt = new AtomicInteger(0);
    collectOrderInputSources(preparableReferencedOrderInputSources, allReferencedOrderInputSources, wfAsStep, cnt);
    countOfAllReferencedOrderInputSources = cnt.get();

    if (preparableReferencedOrderInputSources.size() == 0) {
      preparableReferencedOrderInputSources = Collections.emptyMap();
    }
    if (allReferencedOrderInputSources.size() == 0) {
      allReferencedOrderInputSources = Collections.emptySet();
    }
  }

  //TODO: merge with Operation
  private SpecialPurposeIdentifier parseSpecialPurposeIdentifier(Element operation) {
    Element metaElement = XMLUtils.getChildElementByName(operation, GenerationBase.EL.META);
    if (metaElement != null) {
      Element specialPurposeElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.SPECIAL_PURPOSE);
      if (specialPurposeElement != null) {
        return SpecialPurposeIdentifier.getSpecialPurposeElementByXmlIdentifier(XMLUtils.getTextContent(specialPurposeElement));
      }
    }
    return null;
  }

  private void assignIdsToVariables() {
    this.getAllVariablesOfWorkflow();
    Set<AVariable> vars = getAllVariablesOfWorkflow();
    for (AVariable v : vars) {
      if (v.getId() == null || v.getId().length() == 0) {
        v.setId(String.valueOf(getNextXmlId()));
      }
    }
  }


  private List<AVariable> unusedVariables = new ArrayList<AVariable>();

  private static final Comparator<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>> COMPARATOR_FOR_MOVED_VARIABLES =
      new Comparator<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>>() {

        public int compare(Triple<? extends Step, ? extends AVariable, ? extends ScopeStep> o1,
                           Triple<? extends Step, ? extends AVariable, ? extends ScopeStep> o2) {
          int c = compareStep(o1.getFirst(), o2.getFirst());
          if (c == 0) {
            c = o1.getSecond().getVarName().compareTo(o2.getSecond().getVarName());
            if (c == 0) {
              c = compareStep(o1.getThird(), o2.getThird());
            }
          }
          return c;
        }


        private int compareStep(Step s1, Step s2) {
          if (s1 == null) {
            if (s2 == null) {
              return 0;
            }
            return 1;
          }
          if (s2 == null) {
            return -1;
          }
          return Integer.valueOf(s1.getIdx()).compareTo(s2.getIdx());
        }
      };


  /**
   * variablen in jeweilige scopes verschieben, wenn sie "privat" zu dieser genutzt werden.
   */
    private void moveVariablesToPrivateScopes() {
      /*
       * für jede variable checken, in welcher scope sie verwendet werden, und sie dann dort deklarieren.
       * entfernt variablen auch vollständig, wenn sie gar nicht verwendet werden.
       */
      Set<Step> steps = wfAsStep.getAllStepsRecursively();
      Set<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>> changes =
          new HashSet<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>>();
      for (Step s : steps) {
        if (s.getExceptionVariables() != null) {
          for (ExceptionVariable ev : s.getExceptionVariables()) {
            ScopeStep parentScope = getParentScopeOrSelf(s);
            ScopeStep usedInScope = identifyScopeUsingVariable(ev);
            if (parentScope != usedInScope) {
              changes.add(Triple.of(s, ev, usedInScope));
            }
          }
        }
        if (s.getServiceVariables() != null) {
          for (ServiceVariable sv : s.getServiceVariables()) {
            ScopeStep parentScope = getParentScopeOrSelf(s);
            ScopeStep usedInScope = identifyScopeUsingVariable(sv);
            if (parentScope != usedInScope) {
              changes.add(Triple.of(s, sv, usedInScope));
            }
          }
        }
      }
      //sortieren, damit variablen immer in der gleichen reihenfolge geaddet werden
      List<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>> list =
          new ArrayList<Triple<? extends Step, ? extends AVariable, ? extends ScopeStep>>();
      list.addAll(changes);
      Collections.sort(list, COMPARATOR_FOR_MOVED_VARIABLES);
      for (Triple<? extends Step, ? extends AVariable, ? extends ScopeStep> t : list) {
        if (t.getThird() != null) {
          t.getThird().addPrivateVariable(t.getSecond());
        } else {
          unusedVariables.add(t.getSecond());
          if (logger.isTraceEnabled()) {
            logger.trace("removing unused variable " + t.getSecond() + " in wf " + getOriginalFqName());
          }
        }
        //auch entfernen, wenn variable nicht verwendet wird
        t.getFirst().removeVariable(t.getSecond());
      }
    }

  private ScopeStep getParentScopeOrSelf(Step s) {
    return s instanceof ScopeStep ? (ScopeStep) s : s.getParentScope();
  }


  private ScopeStep identifyScopeUsingVariable(AVariable ev) {
    Set<Step> steps = wfAsStep.getAllStepsRecursively();
    ScopeStep scope = null;
    for (Step step : steps) {
      if (step.getAllUsedVariableIds().contains(ev.getId())) {
        ScopeStep parentScope = getParentScopeOrSelf(step);
        if (scope == null) {
          scope = parentScope;
        } else {
          if (isScopeParentOf(parentScope, scope)) {
            scope = parentScope;
          }
        }
      }
    }

    return scope;
  }


  private boolean isScopeParentOf(ScopeStep parentScope, ScopeStep childScope) {
    if (parentScope == childScope) {
      return false;
    }
    while (true) {
      childScope = childScope.getParentScope();
      if (childScope == parentScope) {
        return true;
      }
      if (childScope == null) {
        return false;
      }
    }
  }


  @Override
  protected void validateInternally() throws XPRC_InvalidVariableNameException, XPRC_EmptyVariableIdException,
      XPRC_InvalidXmlChoiceHasNoInputException, XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException,
      XPRC_InvalidVariableIdException, XPRC_ParsingModelledExpressionException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment {
    // validiere variablennamen (und servicenamen)
    Set<AVariable> allVars = getAllVariablesOfWorkflow();
    validateVars(allVars);
    wfAsStep.validate();
    
    for (AVariable inputVar : inputVars) {
      if (inputVar.getDomOrExceptionObject() == null) {
        if (inputVar.isPrototype()) {
          throw new XPRC_PrototypeDeployment();
        } else if(inputVar.isJavaBaseType() && inputVar.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          // DO NOTHING! 
        } else {
          throw new RuntimeException("Unexpected error: GenerationBase object is null for input parameter");
        }
      }
    }
    for (AVariable outputVar : outputVars) {
      if (outputVar.getDomOrExceptionObject() == null) {
        if (outputVar.isPrototype()) {
          throw new XPRC_PrototypeDeployment();
        } else if(outputVar.isJavaBaseType() && outputVar.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          // DO NOTHING! 
        } else {
          throw new RuntimeException("Unexpected error: GenerationBase object is null for output parameter");
        }
      }
    }
    for (ExceptionVariable v : thrownExceptionVariables) {
      if (v.getDomOrExceptionObject() == null) {
        if (v.isPrototype()) {
          throw new XPRC_PrototypeDeployment();
        } else if(v.isJavaBaseType() && v.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          // DO NOTHING! 
        } else {
          throw new RuntimeException("Unexpected error: GenerationBase object is null for throws parameter");
        }
      }
    }
  }


  public Set<String> getImports() throws XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException {

    HashSet<String> imports = getDefaultImports();

    for (AVariable sv : inputVars) {
      sv.getImports(imports);
    }
    for (AVariable v : outputVars) {
      v.getImports(imports);
    }
    for (Step s : wfAsStep.getAllStepsRecursively()) {
      s.getImports(imports);
    }
    imports.remove(getFqClassName());

    TreeSet<String> sortedImportsFreeOfDuplicates = new TreeSet<String>();
    for (String s : imports) {
      if (!sortedImportsFreeOfDuplicates.contains(s)) {
        sortedImportsFreeOfDuplicates.add(s);
      }
    }

    return sortedImportsFreeOfDuplicates;

  }


  private HashSet<String> getDefaultImports() {
    HashSet<String> imports = new HashSet<String>();
    imports.add(Handler.class.getName());
    imports.add(FractalProcessStep.class.getName());
    imports.add(XynaProcess.class.getName());
    imports.add(XynaException.class.getName());
    imports.add(ArrayList.class.getName());
    imports.add(List.class.getName());
    imports.add(HashMap.class.getName());
    imports.add(ReentrantLock.class.getName());
    imports.add(Container.class.getName());
    imports.add(XynaObjectList.class.getName());
    imports.add(GeneralXynaObjectList.class.getName());
    imports.add(XynaObject.class.getName());
    imports.add(GeneralXynaObject.class.getName());
    imports.add(XynaOrderServerExtension.class.getName());
    imports.add(XynaProcessing.class.getName());
    imports.add(XynaFactory.class.getName());
    imports.add(Logger.class.getName());
    imports.add(DestinationKey.class.getName());
    imports.add(Arrays.class.getName());
    imports.add(Scope.class.getName());
    imports.add(XynaExceptionBase.class.getName());
    imports.add(XynaPropertyUtils.class.getName());

    imports.add(XPRC_TOO_FEW_PROCESS_INPUT_PARAMETERS.class.getName());
    imports.add(XPRC_TOO_MANY_PROCESS_INPUT_PARAMETERS.class.getName());

    return imports;
  }


  public String getOutputTypeFullyQualified() {
    if (outputVars.size() != 1) {
      return Container.class.getSimpleName();
    } else if (outputVars.get(0).isList()) {
      return XynaObjectList.class.getSimpleName() + "<" + outputVars.get(0).getFQClassName() + ">";
    } else {
      return outputVars.get(0).getFQClassName();
    }
  }


  public List<AVariable> getInputVars() {
    return inputVars;
  }


  public ArrayList<AVariable> getOutputVars() {
    return outputVars;
  }


  public ArrayList<ExceptionVariable> getExceptionVars() {
    return thrownExceptionVariables;
  }


  private Set<AVariable> getAllVariablesOfWorkflow() {
    HashSet<AVariable> vars = new HashSet<AVariable>();
    Set<Step> steps = wfAsStep.getAllStepsRecursively();
    for (Step s : steps) {
      if (s.getExceptionVariables() != null) {
        vars.addAll(s.getExceptionVariables());
      }
      if (s.getServiceVariables() != null) {
        vars.addAll(s.getServiceVariables());
      }
      if (s instanceof ScopeStep) {
        ScopeStep ss = (ScopeStep) s;
        vars.addAll(ss.getInputVars());
        vars.addAll(ss.getOutputVars());
      }
    }
    return vars;
  }


  @Override
  protected void fillVarsInternally() throws XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    Set<AVariable> vars = getAllVariablesOfWorkflow();
    for (AVariable v : vars) {
      v.fillVariableContents();
    }
    for (Step step : wfAsStep.getAllStepsRecursively()) {
      if (step instanceof StepMapping) {
        ((StepMapping)step).reevaluateMappings();
      }
    }
  }


  public List<ExceptionVariable> getAllThrownExceptions() {
    return Collections.unmodifiableList(thrownExceptionVariables);
  }


  private static final List<Step> emptyStepList = Collections.unmodifiableList(new ArrayList<Step>());


  public List<Step> getAllDetachedSteps() {
    ArrayList<Step> result = null;
    if (wfAsStep != null) { // FIXME this check is only required since wfAsStep is null on undeploy due to missing parsing
      for (Step s : wfAsStep.getAllStepsRecursively()) {
        if (s.isExecutionDetached()) {
          if (result == null) {
            result = new ArrayList<Step>();
          }
          result.add(s);
        }
      }
    }
    if (result != null) {
      return result;
    } else {
      return emptyStepList;
    }
  }


  public ScopeStep getWfAsStep() {
    return wfAsStep;
  }


  @Override
  protected String getHumanReadableTypeName() {
    return "Workflow";
  }


  @Override
  public String getDocumentation() {
    return documentation;
  }


  private Map<String, String> preparableReferencedOrderInputSources;
  private Set<String> allReferencedOrderInputSources;
  private int countOfAllReferencedOrderInputSources;

  /**
   * @return map von einer eindeutigen id für den wf-schritt auf den namen der referenzierten inputsource
   */
  public Map<String, String> getPreparableReferencedOrderInputSources() {
    return preparableReferencedOrderInputSources;
  }

  /**
   * @return (Unique!) Set der Namen aller referenzierten Inputsources
   */
  public Set<String> getAllReferencedOrderInputSources() {
    return allReferencedOrderInputSources;
  }

  /**
   * nicht unique gezählt die anzahl der aufgerufenen inputsources
   */
  public int getCountOfAllReferencedOrderInputSources() {
    return countOfAllReferencedOrderInputSources;
  }


  @Override
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  
  public SpecialPurposeIdentifier getSpecialPurposeIdentifier() {
    return specialPurposeIdentifier;
  }

  
  /*
  public Step findStep(Integer xmlId) {
    ScopeStep scope = getWfAsStep();
    for( Step step : scope.getChildSteps() ) {
      Step found = findStep( step, xmlId );
      if( found != null ) {
        return found;
      }
    }
    return null;
  }

  private Step findStep(Step step, Integer xmlId) {
    if( xmlId.equals( step.getXmlId() ) ) {
      return step;
    }
    if( step instanceof StepSerial ) {
      for( Step child : step.getChildSteps() ) {
        Step found = findStep( child, xmlId );
        if( found != null ) {
          return found;
        }
      }
    }
    if( step instanceof StepParallel ) {
      for( Step child : step.getChildSteps() ) {
        Step found = findStep( child, xmlId );
        if( found != null ) {
          return found;
        }
      }
    }
    //FIXME weitere rekursion
    
    return null;
  }*/
}
