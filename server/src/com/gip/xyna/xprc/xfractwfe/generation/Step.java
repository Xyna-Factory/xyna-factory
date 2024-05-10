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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionContainer;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_MissingContextForNonstaticMethodCallException;
import com.gip.xyna.xprc.exceptions.XPRC_MissingServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepFilter;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep.FractalProcessStepMarkerInterface;
import com.gip.xyna.xprc.xfractwfe.base.GenericInputAsContextStep;
import com.gip.xyna.xprc.xfractwfe.base.IProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Scope;
import com.gip.xyna.xprc.xfractwfe.base.StartVariableContextStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlAppendable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public abstract class Step implements XmlAppendable, HasMetaTags {
  
  private static final String _METHODNAME_COMPENSATE_ORIG = "compensate";
  protected static final String METHODNAME_COMPENSATE;
  private static final String _METHODNAME_COMPENSATE_INTERNALLY_ORIG = "compensateInternally";
  protected static final String METHODNAME_COMPENSATE_INTERNALLY;
  private static final String _METHODNAME_GET_CHILDREN_TYPES_LENGTH_ORIG = "getChildrenTypesLength";
  protected static final String METHODNAME_GET_CHILDREN_TYPES_LENGTH;
  private static final String _METHODNAME_GET_CURRENT_INCOMING_VALUES_ORIG = "getCurrentIncomingValues";
  protected static final String METHODNAME_GET_CURRENT_INCOMING_VALUES;
  private static final String _METHODNAME_GET_CURRENT_OUTGOING_VALUES_ORIG = "getCurrentOutgoingValues";
  protected static final String METHODNAME_GET_CURRENT_OUTGOING_VALUES;
  private static final String _METHODNAME_EXECUTE_ORIG = "execute";
  protected static final String METHODNAME_EXECUTE;
  private static final String _METHODNAME_EXECUTE_INTERNALLY_ORIG = "executeInternally";
  protected static final String METHODNAME_EXECUTE_INTERNALLY;
  private static final String _METHODNAME_EXECUTE_CHILDREN_ORIG = "executeChildren";
  protected static final String METHODNAME_EXECUTE_CHILDREN;
  private static final String _METHODNAME_GET_CHILDREN_ORIG = "getChildren";
  protected static final String METHODNAME_GET_CHILDREN;
  private static final String _METHODNAME_INIT_ORIG = "init";
  protected static final String METHODNAME_INIT;
  private static final String _METHODNAME_GET_PARENT_STEP_ORIG = "getParentStep";
  protected static final String METHODNAME_GET_PARENT_STEP;
  private static final String _METHODNAME_SET_PARENT_STEP_ORIG = "setParentStep";
  protected static final String METHODNAME_SET_PARENT_STEP;
  private static final String _METHODNAME_GET_LANE_ID_ORIG = "getLaneId";
  protected static final String METHODNAME_GET_LANE_ID;
  private static final String _METHODNAME_SET_LANE_ID_ORIG = "setLaneId";
  protected static final String METHODNAME_SET_LANE_ID;
  private static final String _METHODNAME_REINITIALIZE_ORIG = "reinitialize";
  protected static final String METHODNAME_REINITIALIZE;
  private static final String _METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY_ORIG = "prepareForRetryRecursivly";
  protected static final String METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY;
  private static final String _METHODNAME_GET_PROCESS_ORIG = "getProcess";
  protected static final String METHODNAME_GET_PROCESS;
  private static final String _METHODNAME_GET_PARENT_SCOPE_ORIG = "getParentScope";
  protected static final String METHODNAME_GET_PARENT_SCOPE;
  private static final String _METHODNAME_GET_LABEL_ORIG = "getLabel";
  protected static final String METHODNAME_GET_LABEL;
  private static final String _METHODNAME_GET_XYNA_CHILD_ORDERS_ORIG = "getChildXynaOrders";
  protected static final String METHODNAME_GET_XYNA_CHILD_ORDERS;
  private static final String _METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK_ORIG = "findMarkedProcessStepInExecutionStack";
  protected static final String METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK;
  private static final String _METHODNAME_STEP_FILTER_MATCHES_ORIG = "matches";
  protected static final String METHODNAME_STEP_FILTER_MATCHES;
  private static final String _METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG = "getLastCaughtXynaExceptionContainer";
  protected static final String METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER;
  private static final String _METHODNAME_GET_CAUGHT_EXCEPTION_ORIG = "getCaughtException";
  protected static final String METHODNAME_GET_CAUGHT_EXCEPTION;
  private static final String _METHODNAME_GET_N_ORIG = "getN";
  protected static final String METHODNAME_GET_N;
  private static final String _FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION_ORIG = "hasEvaluatedToCaughtXynaException";
  protected static final String FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION;
  private static final String _FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG = "lastCaughtXynaExceptionContainer";
  protected static final String FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER;
  private static final String _FIELDNAME_CHILD_ORDER_STORAGE_STACK_ORIG = "childOrderStorageStack";
  protected static final String FIELDNAME_CHILD_ORDER_STORAGE_STACK;
  private static final String _METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD_ORIG = "add";
  protected static final String METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD;
  private static final String _METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE_ORIG = "remove";
  protected static final String METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE;
  private static final String _METHODNAME_INIT_EVENT_SOURCE_ORIG = "initEventSource";
  protected static final String METHODNAME_INIT_EVENT_SOURCE;
  private static final String _METHODNAME_CLEAR_EVENT_SOURCE_ORIG = "clearEventSource";
  protected static final String METHODNAME_CLEAR_EVENT_SOURCE;
  private static final String _METHODNAME_GET_CONTEXT_VARIABLE_ORIG = "getContextVariable";
  protected static final String METHODNAME_GET_CONTEXT_VARIABLE;
  private static final String _METHODNAME_CLEAR_CONTEXT_VARIABLE_ORIG = "clearContextVariable";
  protected static final String METHODNAME_CLEAR_CONTEXT_VARIABLE;
  private static final String METHODNAME_GET_CONTEXT_IDENTIFIER_ORIG = "getContextIdentifier";
  protected static final String METHODNAME_GET_CONTEXT_IDENTIFIER;
  
  public interface StepVisitor {
    public void visit( Step step );
    public void visitStepSerial( StepSerial step );
    public void visitStepMapping( StepMapping step );
    public void visitStepFunction( StepFunction step );
    public void visitStepCatch(StepCatch step);
    public void visitStepAssign( StepAssign step );
    public void visitStepForeach( StepForeach step );
    public void visitStepParallel(StepParallel step);
    public void visitStepChoice(StepChoice step);
    public void visitStepThrow(StepThrow step);
    public void visitStepRetry(StepRetry step);
    public void visitScopeStep(ScopeStep step);
  }
  
  public static class EmptyStepVisitor implements StepVisitor {
    public void visit( Step step ) {}
    public void visitStepSerial( StepSerial step ) {};
    public void visitStepMapping( StepMapping step ) {};
    public void visitStepFunction( StepFunction step ) {};
    public void visitStepCatch( StepCatch step ) {};
    public void visitStepAssign( StepAssign step ) {};
    public void visitStepForeach( StepForeach step ) {};
    public void visitStepParallel(StepParallel step) {};
    public void visitStepChoice(StepChoice step) {};
    public void visitStepThrow(StepThrow step) {};
    public void visitStepRetry(StepRetry step) {};
    public void visitScopeStep(ScopeStep step) {};
  }
  
  public static interface Catchable {
    public Step getProxyForCatch();
    public void setCatchStep(StepCatch catchStep);
  }


  public enum DistinctionType { ConditionalChoice, ConditionalBranch, TypeChoice, ExceptionHandling };
  
  
  public abstract void visit( StepVisitor visitor );
  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    // FractalProcessStep
    try {
      METHODNAME_COMPENSATE = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_COMPENSATE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_COMPENSATE_ORIG + " not found", e);
    }
    try {
      METHODNAME_COMPENSATE_INTERNALLY = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_COMPENSATE_INTERNALLY_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_COMPENSATE_INTERNALLY_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CHILDREN_TYPES_LENGTH = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_CHILDREN_TYPES_LENGTH_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CHILDREN_TYPES_LENGTH_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CURRENT_INCOMING_VALUES = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_CURRENT_INCOMING_VALUES_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CURRENT_INCOMING_VALUES_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CURRENT_OUTGOING_VALUES = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_CURRENT_OUTGOING_VALUES_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CURRENT_OUTGOING_VALUES_ORIG + " not found", e);
    }
    try {
      METHODNAME_EXECUTE = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_EXECUTE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_EXECUTE_ORIG + " not found", e);
    }
    try {
      METHODNAME_EXECUTE_INTERNALLY = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_EXECUTE_INTERNALLY_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_EXECUTE_INTERNALLY_ORIG + " not found", e);
    }
    try {
      METHODNAME_EXECUTE_CHILDREN = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_EXECUTE_CHILDREN_ORIG, int.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_EXECUTE_CHILDREN_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CHILDREN = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_CHILDREN_ORIG, int.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CHILDREN_ORIG + " not found", e);
    }
    try {
      METHODNAME_INIT = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_INIT_ORIG, Scope.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_INIT_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_PARENT_STEP = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_PARENT_STEP_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PARENT_STEP_ORIG + " not found", e);
    }
    try {
      METHODNAME_SET_PARENT_STEP = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_SET_PARENT_STEP_ORIG, FractalProcessStep.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SET_PARENT_STEP_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_LANE_ID = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_LANE_ID_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_LANE_ID_ORIG + " not found", e);
    }
    try {
      METHODNAME_SET_LANE_ID = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_SET_LANE_ID_ORIG, String.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_SET_LANE_ID_ORIG + " not found", e);
    }
    try {
      METHODNAME_REINITIALIZE = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_REINITIALIZE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_REINITIALIZE_ORIG + " not found", e);
    }
    try {
      METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY_ORIG, boolean.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_PREPARE_FOR_RETRY_RECURSIVLY_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_PROCESS = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_PROCESS_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PROCESS_ORIG + " not found", e);
    }
    try {
      METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK_ORIG, Class.class, FractalProcessStepFilter.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_FIND_MARKED_PROCESS_STEP_IN_EXECUTION_STACK_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CAUGHT_EXCEPTION = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_CAUGHT_EXCEPTION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CAUGHT_EXCEPTION_ORIG + " not found", e);
    }
    try {
      METHODNAME_INIT_EVENT_SOURCE = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_INIT_EVENT_SOURCE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_INIT_EVENT_SOURCE_ORIG + " not found", e);
    }
    try {
      METHODNAME_CLEAR_EVENT_SOURCE = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_CLEAR_EVENT_SOURCE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CLEAR_EVENT_SOURCE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_N = FractalProcessStep.class.getDeclaredMethod(_METHODNAME_GET_N_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_N_ORIG + " not found", e);
    }
    
    try {
      FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION = FractalProcessStep.class.getDeclaredField(_FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _FIELDNAME_HAS_EVALUATED_TO_CAUGHT_XYNA_EXCEPTION_ORIG + " not found", e);
    }
    try {
      FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER = FractalProcessStep.class.getDeclaredField(_FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _FIELDNAME_LAST_CAUGHT_XYNA_EXCEPTION_CONTAINER_ORIG + " not found", e);
    }
    
    // IProcessStep
    try {
      METHODNAME_GET_PARENT_SCOPE = IProcessStep.class.getDeclaredMethod(_METHODNAME_GET_PARENT_SCOPE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PARENT_SCOPE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_LABEL = IProcessStep.class.getDeclaredMethod(_METHODNAME_GET_LABEL_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_LABEL_ORIG + " not found", e);
    }
    
    // ChildOrderStorage
    try {
      METHODNAME_GET_XYNA_CHILD_ORDERS = ChildOrderStorage.class.getDeclaredMethod(_METHODNAME_GET_XYNA_CHILD_ORDERS_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_XYNA_CHILD_ORDERS_ORIG + " not found", e);
    }
    try {
      FIELDNAME_CHILD_ORDER_STORAGE_STACK = ChildOrderStorage.class.getDeclaredField(_FIELDNAME_CHILD_ORDER_STORAGE_STACK_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _FIELDNAME_CHILD_ORDER_STORAGE_STACK_ORIG + " not found", e);
    }
    try {
      METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD = ChildOrderStorageStack.class.getDeclaredMethod(_METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD_ORIG, ChildOrderStorage.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD_ORIG + " not found", e);
    }
    try {
      METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE = ChildOrderStorageStack.class.getDeclaredMethod(_METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE_ORIG + " not found", e);
    }
    
    
    // FractalProcessStepFilter
    try {
      METHODNAME_STEP_FILTER_MATCHES = FractalProcessStepFilter.class.getDeclaredMethod(_METHODNAME_STEP_FILTER_MATCHES_ORIG, FractalProcessStepMarkerInterface.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_STEP_FILTER_MATCHES_ORIG + " not found", e);
    }
    
    //StartVariableContextStep
    try {
      METHODNAME_GET_CONTEXT_VARIABLE = StartVariableContextStep.class.getDeclaredMethod(_METHODNAME_GET_CONTEXT_VARIABLE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_CONTEXT_VARIABLE_ORIG + " not found", e);
    }
    try {
      METHODNAME_CLEAR_CONTEXT_VARIABLE = StartVariableContextStep.class.getDeclaredMethod(_METHODNAME_CLEAR_CONTEXT_VARIABLE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CLEAR_CONTEXT_VARIABLE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_CONTEXT_IDENTIFIER = GenericInputAsContextStep.class.getDeclaredMethod(METHODNAME_GET_CONTEXT_IDENTIFIER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + METHODNAME_GET_CONTEXT_IDENTIFIER_ORIG + " not found", e);
    }
  }

  public static final String SERIAL_VERSION_UID_NO_VARS = String.valueOf(GenerationBase.calcSerialVersionUID(new ArrayList<Pair<String, String>>()));
  private String className;
  private String varName;
  protected ScopeStep parentScope;
  private int cntInstance;
  private Integer xmlId;
  protected GenerationBase creator;
  private List<Parameter> parameterList;
  private UnknownMetaTagsComponent unknownMetaTagsComponent = new UnknownMetaTagsComponent();

  public Step(ScopeStep parentScope, GenerationBase creator) {
    this(creator);
    if (parentScope == null) {
      return;
    }

    this.parentScope = parentScope;
    int cnt = parentScope.getStepCount();
    cntInstance = cnt;
    className = "Step" + cnt;
    varName = "step" + cnt;
  }


  public Step(GenerationBase creator) {
    this.creator = creator;
    int cnt = 0;
    cntInstance = cnt;
    className = "Step" + cnt;
    varName = "step" + cnt;
  }

  //parent Scope changes, if ForEach is created/removed
  public void setParentScope(ScopeStep scope) {
    parentScope = scope;
  }

  public void setCreator(GenerationBase creator) {
    this.creator = creator;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append( getClass().getSimpleName() );
    
    if (xmlId != null) {
      sb.append("(").append(xmlId).append(")");
    }
    
    return sb.toString();
  }
  

  public int getIdx() {
    return cntInstance;
  }

  public GenerationBase getCreator() {
    return creator;
  }
  
  
  public List<Parameter> getParameterList()
  {
    return parameterList;
  }

  public void setParameterList(List<Parameter> parameterList) {
    this.parameterList = parameterList;
  }

  @Override
  public List<String> getUnknownMetaTags() {
    return unknownMetaTagsComponent.getUnknownMetaTags();
  }

  @Override
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    unknownMetaTagsComponent.setUnknownMetaTags(unknownMetaTags);
  }

  public Parameter getFirstParameter() {
    if ( (parameterList != null) && (parameterList.size() > 0) ) {
      return parameterList.get(0);
    } else {
      return null;
    }
  }
  
  public Parameter getParameter(List<Integer> foreachIndices, int retryCounter) {
    if ( (foreachIndices == null) && (retryCounter <= 0) ) {
      return getFirstParameter();
    }
    
    if (parameterList == null) {
      return null;
    }
    
    for (Parameter parameter : parameterList) {
      if ( ((foreachIndices == null) || (parameter.foreachIndicesEqual(foreachIndices))) &&
           ((retryCounter < 0) || (parameter.getRetryCounter() == retryCounter)) ) {
        return parameter;
      }
    }
    
    return null;
  }
  
  public List<Parameter> getParameters(List<Integer> foreachIndicesPrefix, int retryCounter) {
    List<Parameter> allParams = getParameterList();
    if (allParams == null) {
      return allParams;
    }
    
    Stream<Parameter> filteredParamsStream = allParams.stream();
    if (foreachIndicesPrefix != null) {
      filteredParamsStream = filteredParamsStream.filter(x -> x.isPrefixOfForeachIndices(foreachIndicesPrefix));
    }
    
    if (retryCounter > -1) {
      filteredParamsStream = filteredParamsStream.filter((x -> x.getRetryCounter() == retryCounter));
    }
    
    return filteredParamsStream.collect(Collectors.toList());
  }
  
  public StepCatch getRetryCatch() {
    Step stepToSearchFor = this instanceof StepCatch ? ((StepCatch)this).getStepInTryBlock() : this;
    
    Step catchCandidate = this;
    while (catchCandidate != null) {
      if (catchCandidate instanceof StepCatch && ((StepCatch)catchCandidate).hasExecutedRetry()) {
        StepCatch stepCatch = (StepCatch)catchCandidate;
        
        for (Step catchLane : stepCatch.getChildSteps()) {
          if (!catchLane.hasBeenExecuted()) {
            continue;
          }
          
          Set<Step> stepsInLane = new HashSet<>();
          stepsInLane.add(catchLane);
          WF.addChildStepsRecursively(stepsInLane, catchLane);
          
          for (Step stepInLane : stepsInLane) {
            if (stepInLane == stepToSearchFor) {
              return stepCatch;
            }
          }
        }
      }
      
      catchCandidate = catchCandidate.getParentStep();
    }
    
    return null;
  }
  
  public boolean hasBeenExecuted() {
    return hasBeenExecuted(true);
  }

  public boolean hasBeenExecuted(boolean includeChildSteps) {
    return hasBeenExecuted(null, -1, includeChildSteps);
  }
  
  public boolean hasBeenExecuted(List<Integer> foreachIndicesPrefix, int retryCounter) {
    return hasBeenExecuted(foreachIndicesPrefix, retryCounter, true);
  }

  public boolean hasBeenExecuted(List<Integer> foreachIndicesPrefix, int retryCounter, boolean includeChildSteps) {
    List<Parameter> parameters = getParameters(foreachIndicesPrefix, retryCounter);
    if (parameters != null) {
      for (Parameter parameter : parameters) {
        if ( (parameter != null) && (parameter.getInputTimeStamp() != null) ) {
          return true;
        }
      }
    }

    if (includeChildSteps && getChildSteps() != null) {
      for (Step childStep : getChildSteps()) {
        if (childStep.hasBeenExecuted(foreachIndicesPrefix, retryCounter)) {
          return true;
        }
      }
    }

    return false;
  }
  
  public long getStartTime() throws ParseException {
    long startTime = Long.MAX_VALUE;
    List<Parameter> parameterList = getParameterList();
    if (parameterList != null) {
      for (Parameter parameter : parameterList) {
        long parameterStartTime = parameter.getInputTimeStampUnix();
        if (parameterStartTime < startTime) {
          startTime = parameterStartTime;
        }
      }
    }
    
    List<Step> childSteps = getChildSteps();
    if (childSteps != null) {
      for (Step childStep : childSteps) {
        long childStartTime = childStep.getStartTime();
        if (childStartTime < startTime) {
          startTime = childStartTime;
        }
      }
    }
    
    return startTime;
  }

  public long getStartTime(List<Integer> foreachIndicesPrefix, int retryCounter) throws ParseException {
    List<Parameter> parameterList = getParameters(foreachIndicesPrefix, retryCounter);
    if (parameterList == null || parameterList.size() == 0) {
      return -1;
    }

    long startTime = Long.MAX_VALUE;
    for (Parameter parameter : parameterList) {
      long parameterStartTime = parameter.getInputTimeStampUnix();
      if (parameterStartTime < startTime) {
       startTime = parameterStartTime;
      }
    }

    List<Step> childSteps = getChildSteps();
    if (childSteps != null) {
      for (Step childStep : childSteps) {
        long childStartTime = childStep.getStartTime(foreachIndicesPrefix, retryCounter);
        if (childStartTime < startTime) {
          startTime = childStartTime;
        }
      }
    }

    return startTime;
  }

  public long getStopTime() throws ParseException {
    long stopTime = Long.MIN_VALUE;
    List<Parameter> parameterList = getParameterList();
    if (parameterList != null) {
      for (Parameter parameter : parameterList) {
        long parameterStopTime = parameter.getOutputTimeStampUnix();
        if (parameterStopTime > stopTime) {
          stopTime = parameterStopTime;
        }
      }
    }
    
    List<Step> childSteps = getChildSteps();
    if (childSteps != null) {
      for (Step childStep : childSteps) {
        long childStopTime = childStep.getStopTime();
        if (childStopTime > stopTime) {
          stopTime = childStopTime;
        }
      }
    }
    
    return stopTime;
  }

  public long getStopTime(List<Integer> foreachIndicesPrefix, int retryCounter) throws ParseException {
    List<Parameter> parameterList = getParameters(foreachIndicesPrefix, retryCounter);
    if (parameterList == null || parameterList.size() == 0) {
      return -1;
    }

    long stopTime = Long.MIN_VALUE;
    for (Parameter parameter : parameterList) {
      long parameterStopTime = parameter.getOutputTimeStampUnix();
      if (parameterStopTime > stopTime) {
        stopTime = parameterStopTime;
      }
    }

    List<Step> childSteps = getChildSteps();
    if (childSteps != null) {
      for (Step childStep : childSteps) {
        long childStopTime = childStep.getStopTime(foreachIndicesPrefix, retryCounter);
        if (childStopTime > stopTime) {
          stopTime = childStopTime;
        }
      }
    }

    return stopTime;
  }

  public boolean isInRetryLoop() {
    if (!hasBeenExecuted()) {
      return false;
    }

    List<Parameter> parameterList = getParameterList();
    if (parameterList != null && parameterList.stream().anyMatch(x -> x.getRetryCounter() > 0)) {
      return true;
    }

    Step parentStep = getParentStep();
    if (parentStep != null) {
      return parentStep.isInRetryLoop();
    }

    return false;
  }

  public Pair<Integer, Integer> getRetryCounterRange() {
    int counterMin = Integer.MAX_VALUE;
    int counterMax = Integer.MIN_VALUE;

    List<Parameter> parameterList = getParameterList();
    if (parameterList != null) {
      for (Parameter parameter : parameterList) {
        counterMin = Math.min(counterMin, parameter.getRetryCounter());
        counterMax = Math.max(counterMax, parameter.getRetryCounter());
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

  public Pair<Integer, Integer> getRetryCounterRange(List<Integer> foreachIndices, boolean includingChildren) {
    List<Integer> retryCounterValues = new ArrayList<Integer>(getRetryCounterValues(foreachIndices, includingChildren));
    if (retryCounterValues.size() > 0) {
      return new Pair<Integer, Integer>(retryCounterValues.get(0), retryCounterValues.get(retryCounterValues.size()-1));
    } else {
      return new Pair<Integer, Integer>(0, 0);
    }
  }

  /**
   * Returns a set of all retry counter values for the given foreach indices
   */
  public Set<Integer> getRetryCounterValues(List<Integer> foreachIndices, boolean includingChildren) {
    List<Parameter> parameterList = new ArrayList<Parameter>();
    if (getParameterList() != null) {
      parameterList.addAll(getParameterList());
    }

    if (includingChildren) {
      Set<Step> recursiveChildren = new HashSet<>();
      WF.addChildStepsRecursively(recursiveChildren, this);
      for (Step childStep : recursiveChildren) {
        if (childStep.getParameterList() != null) {
          parameterList.addAll(childStep.getParameterList());
        }
      }
    }

    Stream<Parameter> filteredParamsStream = parameterList.stream().filter(x -> x.foreachIndicesEqual(foreachIndices));
    List<Parameter> paramsCurForeach = filteredParamsStream.collect(Collectors.toList());

    Set<Integer> retryCounterValues = new TreeSet<Integer>();
    for (Parameter param : paramsCurForeach) {
      retryCounterValues.add(param.getRetryCounter());
    }

    return retryCounterValues;
  }
  
  public boolean hasRetryIterationsLeft(Parameter parameter) {
    List<Parameter> parameterList = getParameterList();
    if (parameterList == null || parameterList.size() == 0) {
      return false;
    }

    for (Parameter curIteration : parameterList) {
      if (curIteration.getRetryCounter() > parameter.getRetryCounter()) {
        return true;
      }
    }

    return false;
  }


  public String getClassName() {
    return className;
  }


  public String getVarName() {
    return varName;
  }


  public ScopeStep getParentScope() {
    return parentScope;
  }
  
  public WF getParentWFObject() {
    ScopeStep scope = parentScope;
    while (scope.getParentScope() != null) {
      scope = scope.getParentScope();
    }
    if (scope instanceof WFStep) {
      return ((WFStep)scope).getWF();
    }
    throw new RuntimeException("wf root object not found in step " + this);
  }
  
  protected void parseId(Element e) {
    try {
      xmlId = Integer.valueOf(e.getAttribute(GenerationBase.ATT.ID));
      creator.addXmlId(xmlId);
    } catch (NumberFormatException e1) {
      xmlId = null;
    }
  }

  protected abstract boolean compareImplementation(Step oldStep);

  //folgende methoden nicht rekursiv!

  /**
   * enthält nicht this
   */
  public abstract List<Step> getChildSteps();
  protected abstract List<ServiceVariable> getServiceVariables();
  protected abstract List<ExceptionVariable> getExceptionVariables();
  protected abstract List<Service> getServices();
  protected abstract List<GenerationBase> getDependencies();
  protected abstract Set<String> getAllUsedVariableIds();
  protected abstract void removeVariable(AVariable var);


  public boolean replaceChild(Step oldChild, Step newChild) {
    throw new UnsupportedOperationException(); //wird in Kindklassen teilweise ausführbar
  }


  /**
   * keine rekursion notwendig
   * die macht wfAsStep
   */
  public abstract void validate() throws XPRC_EmptyVariableIdException, XPRC_InvalidXmlChoiceHasNoInputException,
      XPRC_InvalidXmlMissingRequiredElementException, XPRC_MissingServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_ParsingModelledExpressionException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidXMLMissingListValueException,
      XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED, XPRC_PrototypeDeployment;


  public void setXmlId(Integer id) {
    xmlId = id;
    creator.addXmlId(id);
  }


  public Integer getXmlId() {
    return xmlId;
  }

  public void setLabel(String label) {
    throw new UnsupportedOperationException(); //wird in Kindklassen teilweise ausführbar
  }
  
  public String getLabel() {
    throw new UnsupportedOperationException(); //wird in Kindklassen teilweise ausführbar
  }
  
  protected void generatedGetRefIdMethod(CodeBuffer cb) {
    cb.addLine("public Integer getXmlId() {");
    cb.addLine("return " + xmlId);
    cb.addLine("}").addLB();
  }


  protected final void generateJava(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException {
    generateJavaInternally(cb, importedClassesFqStrings);
  }


  protected abstract void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException, XPRC_MissingContextForNonstaticMethodCallException,
      XPRC_OperationUnknownException, XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException,
      XPRC_InvalidVariableMemberNameException, XPRC_ParsingModelledExpressionException;
  
  protected abstract void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) 
      throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException, 
      XPRC_ParsingModelledExpressionException, XPRC_InvalidServiceIdException, XPRC_OperationUnknownException,
      XPRC_MissingContextForNonstaticMethodCallException;


  protected abstract void getImports(HashSet<String> imports) throws XPRC_OperationUnknownException,
                  XPRC_InvalidServiceIdException, XPRC_InvalidVariableIdException;


  protected abstract void parseXML(Element e) throws XPRC_InvalidPackageNameException;


  @Override
  public void parseUnknownMetaTags(Element element, List<String> knownMetaTags) {
    unknownMetaTagsComponent.parseUnknownMetaTags(element, knownMetaTags);  
  }


  @Override
  public boolean hasUnknownMetaTags() {
    return unknownMetaTagsComponent.hasUnknownMetaTags();
  }


  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {
    unknownMetaTagsComponent.appendUnknownMetaTags(xml);
  }


  protected void parseParameter(Element e) throws XPRC_InvalidPackageNameException {
    List<Element> parameterElements = XMLUtils.getChildElementsByName(e, GenerationBase.EL.PARAMETER);
    if ( (parameterElements != null) && (parameterElements.size() > 0) ) {
      parameterList = new ArrayList<Parameter>();
      for (Element parameterElement : parameterElements) {
        Parameter parameter = new Parameter();
        
        // get instance-id if present (for service calls)
        String instanceIdStr = parameterElement.getAttribute(GenerationBase.ATT.INSTANCE_ID);
        if ( (instanceIdStr != null) && (instanceIdStr.length() > 0) ) {
          try {
            parameter.setInstanceId(Long.parseLong(instanceIdStr));
          } catch (Exception parsingError) {
            // TODO: Sub-steps with multiple order-ids are currently not supported -> PMON-455.
          }
        }
        
        // get parent-order-id if present (for service calls)
        String parentOrderIdStr = parameterElement.getAttribute(GenerationBase.ATT.PARENTORDER_ID);
        if ( (parentOrderIdStr != null) && (parentOrderIdStr.length() > 0) ) {
          parameter.setParentOrderId(Long.parseLong(parentOrderIdStr));
        }
        
        // get foreach-indices if present
        String foreachIndices = parameterElement.getAttribute(GenerationBase.ATT.FOREACH_INDICES);
        if ( (foreachIndices != null) && (foreachIndices.length() > 0) ) {
          List<String> indicesStrList = Arrays.asList(foreachIndices.split(","));
          List<Integer> indicesList = new ArrayList<>();
          for (String indexAsStr : indicesStrList) {
            indicesList.add(Integer.parseInt(indexAsStr));
          }
          parameter.setForeachIndices(indicesList);
        }
        
        // get retry-counter if present
        String retryCounter = parameterElement.getAttribute(GenerationBase.ATT.RETRY_COUNTER);
        if ( (retryCounter != null) && (retryCounter.length() > 0) ) {
          parameter.setRetryCounter(Integer.parseInt(retryCounter));
        }
        
        // get parameter values
        parameter.addInputData(Service.parseInputOutput(parameterElement, null, GenerationBase.EL.INPUT, creator, true));
        parameter.addOutputData(Service.parseInputOutput(parameterElement, null, GenerationBase.EL.OUTPUT, creator, true));
        
        // parse error (filled when step failed)
        parameter.setErrorInfo(parseErrorInfo(parameterElement, GenerationBase.EL.PARAMETER_ERROR));
        
        // parse error data in input (filled for catches to store caught exception)
        parameter.setCaughtExceptionInfo(parseErrorInfo(parameterElement, GenerationBase.EL.INPUT));
        
        // get parameter time stamps
        
        Element inputElement = XMLUtils.getChildElementByName(parameterElement, GenerationBase.EL.INPUT);
        if (inputElement != null) {
          parameter.setInputTimeStamp(inputElement.getAttribute(GenerationBase.ATT.DATE));
        }
        Element outputElement = XMLUtils.getChildElementByName(parameterElement, GenerationBase.EL.OUTPUT);
        if (outputElement != null) {
          parameter.setOutputTimeStamp(outputElement.getAttribute(GenerationBase.ATT.DATE));
        }
        
        Element errorElement = XMLUtils.getChildElementByName(parameterElement, GenerationBase.EL.PARAMETER_ERROR);
        if (errorElement != null) {
          parameter.setErrorTimeStamp(errorElement.getAttribute(GenerationBase.ATT.DATE));
        }
        
        parameterList.add(parameter);
      }
    }
  }


  private ErrorInfo parseErrorInfo(Element parameterElement, String errorElementName) throws XPRC_InvalidPackageNameException {
    List<AVariable> errorList = Service.parseInputOutput(parameterElement, null, errorElementName, creator);
    if ( (errorList == null) || (errorList.size() == 0) ) {
      return null;
    }

    AVariable caughtException = errorList.get(0);
    if (!(caughtException instanceof ExceptionVariable)) {
      // no exception has been caught
      return null;
    }

    ErrorInfo errorInfo = new ErrorInfo();
    errorInfo.setExceptionVariable(caughtException);

    Element errorElement = XMLUtils.getChildElementByName(parameterElement, errorElementName);
    Element exceptionElement = XMLUtils.getChildElementByName(errorElement, GenerationBase.EL.EXCEPTION);
    Element stackTraceElement = XMLUtils.getChildElementByName(exceptionElement, GenerationBase.EL.STACKTRACE);
    if (stackTraceElement != null && stackTraceElement.getFirstChild() != null) {
      errorInfo.setStacktrace(stackTraceElement.getFirstChild().getTextContent());
    }

    Element errorMessageElement = XMLUtils.getChildElementByName(exceptionElement, GenerationBase.EL.ERRORMESSAGE);
    if (errorMessageElement != null && errorMessageElement.getFirstChild() != null) {
      errorInfo.setMessage(errorMessageElement.getFirstChild().getTextContent());
    }

    return errorInfo;
  }


  protected void setSourceAndTargetIds(Parameter parameter) {
    setSourceIds(parameter.getInputData(), getInputVarIds());
    setTargetIds(parameter.getOutputData(), getOutputVarIds());
  }


  protected void setSourceIds(List<AVariable> inputData, String[] inputVarIds) {
    for (int varNo = 0; varNo < Math.min(inputData.size(), inputVarIds.length); varNo++) {
      AVariable input = inputData.get(varNo);
      input.setSourceIds(inputVarIds[varNo]);
    }
  }


  protected void setTargetIds(List<AVariable> outputData, String[] targetIds) {
    for (int varNo = 0; varNo < Math.min(outputData.size(), targetIds.length); varNo++) {
      AVariable output = outputData.get(varNo);
      output.setTargetId(targetIds[varNo]);
    }
  }


  private void setIds(List<AVariable> parameterData, List<AVariable> varsWithIds) {
    for (int varNo = 0; varNo < Math.min(parameterData.size(), varsWithIds.size()); varNo++) {
      AVariable parameter = parameterData.get(varNo);
      parameter.setId(varsWithIds.get(varNo).getId());
    }
  }


  public void addIdsToParameter() {
    if (parameterList == null) {
      return;
    }

    for (Parameter parameter : parameterList) {
      setIds(parameter.getInputData(), getInputVars());
      setIds(parameter.getOutputData(), getOutputVars());
      setSourceAndTargetIds(parameter);
    }
  }


  // to be overriden in sub-classes in case labels need to be set separately
  public void addLabelsToParameter() {
  }


  protected void addLabelsToParameter(List<AVariable> inputVars, List<AVariable> outputVars) {
    List<Parameter> parameterList = getParameterList();
    if (parameterList == null) {
      return;
    }
    
    for (int inputVarNr = 0; inputVarNr < inputVars.size(); inputVarNr++) {
      String label = inputVars.get(inputVarNr).getLabel();
      for (Parameter parameter : parameterList) {
        List<AVariable> inputData = parameter.getInputData();
        if ( (inputData != null) && (inputVarNr < inputData.size()) ) {
          inputData.get(inputVarNr).setLabel(label);
        }
      }
    }
    
    for (int outputVarNr = 0; outputVarNr < outputVars.size(); outputVarNr++) {
      String label = outputVars.get(outputVarNr).getLabel();
      for (Parameter parameter : parameterList) {
        List<AVariable> outputData = parameter.getOutputData();
        if ( (outputData != null) && (outputVarNr < outputData.size()) ) {
          outputData.get(outputVarNr).setLabel(label);
        }
      }
    }
  }


  protected static boolean doesMetaElementDetachedExist(Element e) {
    List<Element> children = XMLUtils.getChildElementsByName(e, GenerationBase.EL.META);
    if (children.size() > 0) {
      if (children.size() > 1) {
        // this should have been caught by the XSD check
        throw new RuntimeException("More than one meta elements for element " + e.getNodeName()
                        + ", only one is allowed.");
      }
      Element meta = children.get(0);
      Element possiblyDetachedElement = XMLUtils.getChildElementByName(meta, GenerationBase.EL.DETACHED);
      if (possiblyDetachedElement != null) {
        return true;
      }
    }
    return false;
  }


  public static boolean isExceptionAndNoXynaObject(AVariable v) {
    return Objects.equals(v.getFQClassName(), Exception.class.getName())
                    || Objects.equals(v.getFQClassName(), XynaException.class.getName());
  }



  protected void generateJavaForIncomingOutgoingValues(String methodName, String[] ids, String[] paths, CodeBuffer cb,
                                                       HashSet<String> importedClassesFqStrings)
                  throws XPRC_InvalidVariableIdException {
    // return the incoming parameter values
    cb.addLine("public ", GeneralXynaObject.class.getSimpleName(), "[] ", methodName, "() {");

    if (ids == null || ids.length == 0) {
      cb.add("return " + XynaObject.class.getSimpleName() + ".EMPTY_XYNA_OBJECT_ARRAY;").addLB();
    } else {
      cb.add("return new " + GeneralXynaObject.class.getSimpleName() + "[] {");

      if (ids.length == 1) {
        VariableIdentification vi = parentScope.identifyVariable(ids[0]);
        AVariable v = vi.variable;
        if (v.isList()) {
          if (v instanceof ExceptionVariable) {
            cb.add("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + vi.getScopeGetter(getParentScope())
                + v.getGetter(paths[0]) + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
                + ".class)");
          } else {
            cb.add("new " + XynaObjectList.class.getSimpleName() + "(" + vi.getScopeGetter(getParentScope())
                + v.getGetter(paths[0]) + ", " + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings)
                + ".class)");
          }
        } else if (isExceptionAndNoXynaObject(v)) {
          cb.add("new " + XynaExceptionContainer.class.getName() + "(" + vi.getScopeGetter(getParentScope())
                          + v.getGetter(paths[0]) + ")");
        } else {
          cb.add(vi.getScopeGetter(getParentScope()) + v.getGetter(paths[0]));
        }
      } else if (ids.length > 1) {

        for (int i = 0; i < ids.length; i++) {
          VariableIdentification vi = parentScope.identifyVariable(ids[i]);
          AVariable v = vi.variable;
          if (v.isList()) {
            if (v instanceof ExceptionVariable) {
              cb.add("new " + GeneralXynaObjectList.class.getSimpleName() + "(" + vi.getScopeGetter(getParentScope())
                     + v.getGetter(paths[i]) + ", "
                     + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
            } else {
              cb.add("new " + XynaObjectList.class.getSimpleName() + "(" + vi.getScopeGetter(getParentScope())
                  + v.getGetter(paths[i]) + ", "
                  + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
            }
          } else if (isExceptionAndNoXynaObject(v)) {
            cb.add("new " + XynaExceptionContainer.class.getName() + "(" + vi.getScopeGetter(getParentScope())
                            + v.getGetter(paths[i]) + ")");
          } else {
            cb.add(vi.getScopeGetter(getParentScope()) + v.getGetter(paths[i]));
          }
          if (i < ids.length - 1) {
            cb.add(", ");
          }

        }
      }
      cb.add("};").addLB(); // End of "return new XynaObject[]{...}"
    }

    cb.addLine("}").addLB();

  }

  /**
   * gibt ids von vars zurück, die in diesem schritt als input gelten. wird von stepcatch benutzt
   */
  public String[] getInputVarIds() {
    return new String[0];
  }

  /**
   * gibt ids von vars zurück, die in diesem schritt als output gelten. wird von stepcatch benutzt
   */
  public String[] getOutputVarIds() {
    return new String[0];
  }
  
  public List<AVariable> getInputVars() {
    return new ArrayList<AVariable>();
  }

  public List<AVariable> getOutputVars() {
    return new ArrayList<AVariable>();
  }
  
  public String[] getInputVarPaths() {
    return new String[0];
  }
  
  public String[] getOutputVarPaths() {
    return new String[0];
  }

  public abstract boolean isExecutionDetached();
  
  
  protected static boolean containsOrIsAssignableFrom(Step step, Class<?> clazz) {
    if (step == null) {
      return false;
    }
    
    if (step.getClass().isAssignableFrom(clazz)) {
      return true;
    }
    if (step.getChildSteps() == null) {
      return false;
    }
    for (Step childStep : step.getChildSteps()) {
      if (containsOrIsAssignableFrom(childStep, clazz)) {
        return true;
      }
    }
    return false;
  }
  
  
  public Step getParentStep() {
    Step possibleParent = getParentScope();
    while (possibleParent != null) {
      if (possibleParent.isDirectChild(this)) {
        return possibleParent;
      } else {
        List<Step> children = possibleParent.getChildSteps();
        possibleParent = null;
        for (Step child : children) {
          if (child.isChild(this)) {
            possibleParent = child;
          }
        }
      }
    }
    return null;
  }


  public Step getContainerStepForGui() {
    Step parent = getParentStep();
    if (parent != null) {
      return parent.getContainerStepForGui();
    }

    return null;
  }


  protected boolean isChild(Step step) {
    if (isDirectChild(step)) {
      return true;
    } else {
      if (getChildSteps() != null) {
        for (Step child : getChildSteps()) {
          if (child.isChild(step)) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  
  protected boolean isDirectChild(Step step) {
    return (getChildSteps() != null &&
            getChildSteps().contains(step));
  }


  protected Set<String> createVariableIdSet(List<String> ids) {
    return new HashSet<String>(ids);
  }

  protected Set<String> createVariableIdSet(String[] ... idArrays) {
    Set<String> s = new HashSet<String>();
    for (String[] idArray : idArrays) {
      for (String id : idArray) {
        s.add(id);
      }
    }
    return s;
  }

  /**
   * Returns all service references from the step and all child steps.
   */
  public Set<Pair<Service, StepFunction>> getAllServiceReferences() throws XPRC_InvalidServiceIdException {
    Set<Pair<Service, StepFunction>> serviceReferences = new HashSet<>();
    collectServiceReferences(serviceReferences);
    
    return serviceReferences;
  }

  /**
   * Recursively runs over all child steps and adds service references to the given set.
   * 
   * The function must be overridden in sub-classes that have service references, adding them to the set.
   * @param serviceReferences the set the found references are added to
   */
  protected void collectServiceReferences(Set<Pair<Service, StepFunction>> serviceReferences) throws XPRC_InvalidServiceIdException {
    List<Step> childSteps = getChildSteps();
    if (childSteps == null) {
      return;
    }
    
    for (Step childStep : childSteps) {
      childStep.collectServiceReferences(serviceReferences);
    }
  }


  public String getStepId() {
    if (xmlId == null) {
      return null;
    }
    return String.valueOf(xmlId);
  }


  public boolean toBeShownInAudit() {
    return true;
  }


  public List<ExceptionVariable> getAllThrownExceptions(boolean considerRetryAsHandled) {
    Set<ExceptionVariable> thrownExceptions = new TreeSet<ExceptionVariable>(new Comparator<ExceptionVariable>() {

      @Override
      public int compare(ExceptionVariable exceptionVar1, ExceptionVariable exceptionVar2) {
        return exceptionVar1.getFQClassName().compareTo(exceptionVar2.getFQClassName());
      }
    });

    List<Step> children = getChildSteps();
    if (children != null) {
      for (Step child : children) {
        thrownExceptions.addAll(child.getAllThrownExceptions(considerRetryAsHandled));
      }
    }

    return new ArrayList<ExceptionVariable>(thrownExceptions);
  }


  protected void appendSource(XmlBuilder xml, String id) {
    appendSource(xml, id, false, false, null, false, new ArrayList<Element>());
  }

  protected void appendSource(XmlBuilder xml, String id, boolean isUserConnected, boolean isConstantConnected, boolean writeEvenWhenEmpty) {
    appendSource(xml, id, isUserConnected, isConstantConnected, null, writeEvenWhenEmpty, new ArrayList<Element>());
  }

  protected void appendSource(XmlBuilder xml, String id, boolean isUserConnected, boolean isConstantConnected, boolean writeEvenWhenEmpty, List<Element> unknownMetaTags) {
    appendSource(xml, id, isUserConnected, isConstantConnected, null, writeEvenWhenEmpty, unknownMetaTags);
  }

  protected void appendSource(XmlBuilder xml, String id, boolean isUserConnected, boolean isConstantConnected, String expectedType, boolean writeEvenWhenEmpty, List<Element> unknownMetaTags) {
    if ( (!writeEvenWhenEmpty) && 
         ( (id == null) || (id.length() == 0) ) ) {
      return;
    }

    xml.startElementWithAttributes(EL.SOURCE); {
      xml.addAttribute(ATT.REFID, id);
      xml.endAttributes();

      // <Meta>
      if (isUserConnected || isConstantConnected || unknownMetaTags.size() > 0 || (expectedType != null && expectedType.length() > 0) ) {
        xml.startElement(EL.META); {
          if (isUserConnected) {
            xml.element(EL.LINKTYPE, EL.LINKTYPE_USER_CONNECTED);
          }
          if (isConstantConnected) {
            xml.element(EL.LINKTYPE, EL.LINKTYPE_CONSTANT_CONNECTED);
          }
          if (expectedType != null && expectedType.length() > 0) {
            xml.element(EL.EXPECTED_TYPE, expectedType);
          }

          for (Element tag : unknownMetaTags) {
            xml.append(tag);
          }
        } xml.endElement(EL.META);
      }
    } xml.endElement(EL.SOURCE);
  }

  protected void appendTarget(XmlBuilder xml, String refId, boolean writeEvenWhenEmpty) {
    appendTarget(xml, refId, null, writeEvenWhenEmpty);
  }

  protected void appendTarget(XmlBuilder xml, String refId, String expectedType, boolean writeEvenWhenEmpty) {
    if ( (!writeEvenWhenEmpty) && 
         ( (refId == null) || (refId.length() == 0) ) ) {
      return;
    }
    
    xml.startElementWithAttributes(EL.TARGET); {
      xml.addAttribute(ATT.REFID, refId);
      xml.endAttributes();
      
      if (expectedType != null) {
        xml.startElement(EL.META); {
          xml.element(EL.EXPECTED_TYPE, expectedType);
        } xml.endElement(EL.META);
      }
    } xml.endElement(EL.TARGET);
  }

}
