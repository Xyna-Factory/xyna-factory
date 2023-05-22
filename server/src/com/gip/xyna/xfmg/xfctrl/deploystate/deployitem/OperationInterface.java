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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface.MatchableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ProblemType;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface.AvariableNotResolvableException;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.WF;


public class OperationInterface implements MatchableInterface {
  
  private String name;
  private final OperationType type;
  private final ImplementationType implType;
  private final List<TypeInterface> input;
  private final List<TypeInterface> output;
  private final List<TypeInterface> exceptions;
  private final ProblemType problem;
  private boolean isActive = true;


  private OperationInterface(String name, OperationType type, ImplementationType implType, List<TypeInterface> input,
                             List<TypeInterface> output, List<TypeInterface> exceptions) {
    this(name, type, implType, ProblemType.SERVICE_INVOCATION, input, output, exceptions);
  }


  private OperationInterface(String name, OperationType type, ImplementationType implType, List<TypeInterface> input,
                             List<TypeInterface> output, List<TypeInterface> exceptions, boolean isActive) {
    this(name, type, implType, ProblemType.SERVICE_INVOCATION, input, output, exceptions, isActive);
  }
  

  protected OperationInterface(String name, OperationType type, ImplementationType implType, ProblemType problem, 
                               List<TypeInterface> input, List<TypeInterface> output, List<TypeInterface> exceptions) {
    this(name, type, implType, problem, input, output, exceptions, true);
  }


  protected OperationInterface(String name, OperationType type, ImplementationType implType, ProblemType problem, 
                             List<TypeInterface> input, List<TypeInterface> output, List<TypeInterface> exceptions, boolean isActive) {
    this.name = name;
    this.type = type;
    this.implType = implType;
    this.problem = problem;
    this.input = input;
    this.output = output;
    this.exceptions = exceptions;
    this.isActive = isActive;
  }

  public String getName() {
    return name;
  }

  public OperationType getType() {
    return type;
  }
  
  public ImplementationType getImplType() {
    return implType;
  }
  
  public ProblemType getProblemType() {
    return problem;
  }
  
  public List<TypeInterface> getInput() {
    return input;
  }
  
  public List<TypeInterface> getOutput() {
    return output;
  }
  
  public List<TypeInterface> getExceptions() {
    return exceptions;
  }
  
  public List<TypeInterface> getAllTypeInterfaces() {
    List<TypeInterface> typeInterfaces = new ArrayList<TypeInterface>();
    if (input != null) {
      typeInterfaces.addAll(input);
    }
    if (output != null) {
      typeInterfaces.addAll(output);
    }
    if (exceptions != null) {
      typeInterfaces.addAll(exceptions);
    }
    
    return typeInterfaces;
  }


  public boolean resolve() {
    if (resolveLocal()) {
      return true;
    } else {
      InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
      Optional<TypeInterface> superType = resCtx.getLocalSupertype();
      while (superType.isPresent()) {
        DeploymentItemState dis = resCtx.resolveProvider(superType.get());
        if (dis != null && dis.exists()) {
          InterfaceResolutionContext.updateCtx(dis);
          try {
            if (resolveLocal()) {
              return true;
            } else {
              superType = resCtx.getLocalSupertype();
            }
          } finally {
            InterfaceResolutionContext.revertCtx();
          }
        } else {
          return false;
        }
      }
    }
    return false;
  }
  
  
  public boolean resolveLocal() {
    if (InterfaceResolutionContext.resCtx.get().getPublishedInterfaces().containsMatchingOperation(this)) {
      InterfaceResolutionContext.resCtx.get().addOperationInvocationAtTargetSite(this);
      return true;
    } else {
      return false;
    }
  }


  public boolean isActive() {
    return isActive;
  }
  
  public boolean matches(DeploymentItemInterface other) {
    if (other instanceof OperationInterface) {
      OperationInterface otherOperation = (OperationInterface) other;
      if (//type == otherOperation.type &&
          (name == null || otherOperation.name == null || name.equals(otherOperation.name)) &&
          (input == null || otherOperation.input == null || input.size() == otherOperation.input.size()) &&
          (output ==  null || otherOperation.output == null || output.size() == otherOperation.output.size())) {
        // TODO handle exceptions ?
        if (input != null && otherOperation.input != null) {
          for (int i = 0; i < input.size(); i++) {
            // this call implies that the published OperationInterface has to be the one the match is called upon
            if (!input.get(i).isAssignableFrom(otherOperation.input.get(i))) {
              return false;
            }
          }
        }
        if (output != null && otherOperation.output != null) {
          for (int i = 0; i < output.size(); i++) {
            if (!output.get(i).isAssignableFrom(otherOperation.output.get(i))) {
              return false;
            }
          }
        }
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  
  public static enum OperationType {
    WORKFLOW, STATIC_SERVICE, INSTANCE_SERVICE;
  }
  
  
  public static enum ImplementationType {
    ABSTRACT, CONCRETE;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(name, input, output, exceptions);
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OperationInterface)) {
      return false;
    }
    OperationInterface otherOp = (OperationInterface) obj;
    return Objects.equals(name, otherOp.name) && Objects.equals(input, otherOp.input) && Objects.equals(output, otherOp.output)
        && Objects.equals(exceptions, otherOp.exceptions);
  }

  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (output == null) {
      sb.append("? ");
    } else {
      sb.append("[");
      for (int i= 0; i < output.size(); i++) {
        sb.append(output.get(i).getDescription());
        if (i+1 < output.size()) {
          sb.append(", ");
        }
      }
      sb.append("] ");
    }
    sb.append(name).append("(");
    for (int i= 0; i < input.size(); i++) {
      sb.append(input.get(i).getDescription());
      if (i+1 < input.size()) {
        sb.append(", ");
      }
    }
    sb.append(") ");
    if (exceptions == null) {
      sb.append("throws ?");
    } else if (exceptions.size() > 0) {
      sb.append("throws ");
      for (int i= 0; i < exceptions.size(); i++) {
        sb.append(exceptions.get(i).getDescription());
        if (i+1 < exceptions.size()) {
          sb.append(", ");
        }
      }
    }
    return sb.toString();
  }
  
  
  public String getDescription() {
    return toString();
  }


  public static OperationInterface of(WF wf) throws AvariableNotResolvableException {
    List<TypeInterface> input = new ArrayList<TypeInterface>();
    for (AVariable aVar : wf.getInputVars()) {
      input.add(TypeInterface.of(aVar));
    }
    List<TypeInterface> output = new ArrayList<TypeInterface>();
    for (AVariable aVar : wf.getOutputVars()) {
      output.add(TypeInterface.of(aVar));
    }
    List<TypeInterface> exceptions = new ArrayList<TypeInterface>();
    for (AVariable aVar : wf.getAllThrownExceptions()) {
      exceptions.add(TypeInterface.of(aVar, XMOMType.EXCEPTION));
    }
    return new OperationInterface(wf.getOriginalSimpleName(), OperationType.WORKFLOW, ImplementationType.CONCRETE, input, output, exceptions);
  }


  public static OperationInterface of(TypeInterface providerType, Operation operation) throws AvariableNotResolvableException {
    boolean isActive = true;
    if (operation instanceof JavaOperation) {
      isActive = ((JavaOperation) operation).isActive();
    }
    List<TypeInterface> input = new ArrayList<TypeInterface>();
    if (!operation.isStatic()) {
      // instance operation invocations will appear to receive the type as first param 
      input.add(providerType);
    }
    for (AVariable aVar : operation.getInputVars()) {
      input.add(TypeInterface.of(aVar));
    }
    List<TypeInterface> output = new ArrayList<TypeInterface>();
    for (AVariable aVar : operation.getOutputVars()) {
      output.add(TypeInterface.of(aVar));
    }
    List<TypeInterface> exceptions = new ArrayList<TypeInterface>();
    for (AVariable aVar : operation.getThrownExceptions()) {
      exceptions.add(TypeInterface.of(aVar, XMOMType.EXCEPTION));
    }
    // TODO versioning?
    return new OperationInterface(operation.getName(),
                                  operation.isStatic() ? OperationType.STATIC_SERVICE : OperationType.INSTANCE_SERVICE,
                                  operation.isAbstract() ? ImplementationType.ABSTRACT : ImplementationType.CONCRETE, 
                                  input, output, exceptions, isActive);
  }


  public static DeploymentItemInterface of(StepFunction step, ServiceIdentification sid) {
    ScopeStep scope = step.getParentScope();
    List<TypeInterface> input = new ArrayList<TypeInterface>();
    for (String inputVarId : step.getInputVarIds()) {
      try {
        VariableIdentification varId = scope.identifyVariable(inputVarId);
        //nicht als input charakterisieren. es ist in der serviceoperation/wf als input deklariert, hier ist es aus der sicht des workflows "verwendung"
        input.add(TypeInterface.of(varId.getVariable(), TypeOfUsage.EMPLOYMENT));
      } catch (XPRC_InvalidVariableIdException e) {
        if (inputVarId == null || inputVarId.trim().isEmpty()) {
          return new UnresolvableInterface.MissingVarId(TypeOfUsage.EMPLOYMENT, step.getXmlId());
        } else {
          return UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, inputVarId, step.getXmlId());
        }
      } catch (AvariableNotResolvableException e) {
        return UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, inputVarId, step.getXmlId());
      }
    }
    List<TypeInterface> output = new ArrayList<TypeInterface>();
    for (String outputVarId : step.getOutputVarIds()) {
      try {
        VariableIdentification varId = scope.identifyVariable(outputVarId);
        output.add(TypeInterface.of(varId.getVariable(), TypeOfUsage.EMPLOYMENT));
      } catch (XPRC_InvalidVariableIdException e) {
        if (outputVarId == null || outputVarId.trim().isEmpty()) {
          return new UnresolvableInterface.MissingVarId(TypeOfUsage.EMPLOYMENT, step.getXmlId());
        } else {
          return UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, outputVarId, step.getXmlId());
        }
      } catch (AvariableNotResolvableException e) {
        return UnresolvableInterface.get(TypeOfUsage.EMPLOYMENT, outputVarId, step.getXmlId());
      }
    }
    return of(step.getOperationName(), input, output);
  }
  
  
  public static OperationInterface of(String operationName, List<TypeInterface> input, List<TypeInterface> output) {
    return of(operationName, null, null, input, output, null);
  }

  public static OperationInterface of(OperationInterface oi, ProblemType problem) {
    return new OperationInterface(oi.getName(), oi.getType(), oi.getImplType(), problem, oi.getInput(), oi.getOutput(), oi.getExceptions()); 
  }

  public static OperationInterface of(String operationName, OperationType type, ImplementationType implType, List<TypeInterface> inputTypes,
                                       List<TypeInterface> outputTypes, List<TypeInterface> exceptionTypes) {
    return new OperationInterface(operationName, type, implType, inputTypes, outputTypes, exceptionTypes);
  }

  public void setName(String fqName) {
    this.name = fqName;
  }

  
  /**
   * cloned insbesondere die input-liste
   */
  public OperationInterface clone() {
    return new OperationInterface(name, type, implType, problem, input == null ? null : new ArrayList<TypeInterface>(input), output,
                                  exceptions, isActive);
  }

}
