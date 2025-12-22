/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.session.gb.vars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xact.filter.util.ServiceVarIdentification;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.json.Workflow;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ForEachScopeStep;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.ServiceIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;

public class IdentifiedVariablesStepFunction extends IdentifiedVariablesStepWithWfVars {

  private static final String FQN_SELECTION_MASK = "xnwh.persistence.SelectionMask";
  private static final String SELECTION_MASK_ROOT_TYPE = "rootType";

  private static final Logger logger = CentralFactoryLogging.getLogger(IdentifiedVariablesStepFunction.class);
  private final StepFunction stepFunction;
  private Service service;


  public IdentifiedVariablesStepFunction(ObjectId id, StepFunction stepFunction) {
    super(id, stepFunction);
    this.stepFunction = stepFunction;

    identify();
  }


  @Override
  public void identify() {
    ServiceIdentification serviceIdentification;
    try {
      serviceIdentification = stepFunction.getParentScope().identifyService(stepFunction.getServiceId());
      this.service = serviceIdentification.service;
    } catch (XPRC_InvalidServiceIdException e) {
      this.service = stepFunction.getService();
      if (this.service == null) {
        throw new RuntimeException(e);
      }
    }
    
    if (service.isPrototype()) {
      this.readonly = false;
      this.inputVarIdentifications = fillServiceVars( VarUsageType.input, stepFunction.getInputVarIds(), service.getInputVars() );
      this.outputVarIdentifications = fillServiceVars( VarUsageType.output, stepFunction.getOutputVarIds(), service.getOutputVars() );
      this.thrownExceptions = new ArrayList<AVariableIdentification>();
    } else {
      this.readonly = true;
      List<AVariable> inputVars, outputVars;
      List<ExceptionVariable> thrownExceptionVars;

      if (service.isDOMRef()) {
        Operation operation = getOperation();
        List<AVariable> operationInputs = operation.getInputVars();
        List<AVariable> operationOutputs = operation.getOutputVars();

        // for instance methods of data types, add fake input variable that holds instance of the data type
        if (isInstanceMethod()) {
          operationInputs = addSelfVar(operationInputs, service.getLabel(), service.getDom());
        }

        inputVars = stepFunction.getOrderInputSourceRef() != null && stepFunction.getOrderInputSourceRef().length() > 0 ?
            operationInputs : getConcreteVars(operationInputs, getInputVarCastToFqns());
        outputVars = getConcreteVars(operationOutputs, getReceiveVarCastToFqns());
        thrownExceptionVars = operation.getThrownExceptions();
      } else {
        if(!service.getWF().parsingFinished()) {
          // happens for recursive calls in audits - if parsing is not finished, input variables are not set
          try {
            service.getWF().parseGeneration(false, false, false);
          } catch (Exception e) { 
            if(logger.isDebugEnabled()) {
              logger.debug("exception during parseGeneration of " + service.getWF(), e );
            }
          }
        }
        inputVars = service.getWF().getInputVars();
        if(stepFunction.getOrderInputSourceRef() == null || stepFunction.getOrderInputSourceRef().isEmpty()) {
          inputVars = getConcreteVars(inputVars, getInputVarCastToFqns());
        }
        outputVars = getConcreteVars(service.getWF().getOutputVars(), getReceiveVarCastToFqns());
        thrownExceptionVars = service.getWF().getAllThrownExceptions();
      }

      this.inputVarIdentifications = fillServiceVars(VarUsageType.input, stepFunction.getInputVarIds(), inputVars);
      this.outputVarIdentifications = fillServiceVars(VarUsageType.output, stepFunction.getOutputVarIds(), outputVars);
      this.thrownExceptions = fillDirectVars(VarUsageType.thrown, thrownExceptionVars, null);
    }
  }

  private Operation getOperation() {
    if (!service.isDOMRef()) {
      return null;
    }

    try {
      return service.getDom().getOperationByName(stepFunction.getOperationName());
    } catch (XPRC_OperationUnknownException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Gives the input/output variables of the step function.
   * 
   * These are either the list of input/output variables of the called service or - when defined - instances of the expected types (dynamic typing). 
   */
  private List<AVariable> getConcreteVars(List<AVariable> serviceSignatureVars, String[] varCastToTypes) {
    List<AVariable> concreteVars = new ArrayList<AVariable>();
    for (int varIdx = 0; varIdx < serviceSignatureVars.size(); varIdx++) {
      AVariable signatureVar = serviceSignatureVars.get(varIdx);
      if (varCastToTypes[varIdx] == null || varCastToTypes[varIdx].length() == 0) {
        concreteVars.add(signatureVar);
      } else {
        try {
          // create an instance of the expected type to be used for dataflow connections (dynamic typing)
          AVariable expectedTypeVar = createVariable(varCastToTypes[varIdx], null, signatureVar.isList(), signatureVar instanceof ExceptionVariable);
          concreteVars.add(expectedTypeVar);
        } catch (Exception e) {
          Utils.logError("Failed to create variable for dynamic typing. Falling back to original variable.", e);
          concreteVars.add(signatureVar);
        }
      }
    }
    
    return concreteVars;
  }

  public AVariable createVariable(String fqn, String label, boolean isList, boolean isException) throws XPRC_InvalidPackageNameException {
    AVariable var = isException ? new ExceptionVariable(service.getCreator()) : new ServiceVariable(service.getCreator());
    if (DatatypeVariable.ANY_TYPE.equals(fqn)) {
      ((ServiceVariable) var).create(fqn);
    } else {
      long revision = stepFunction.getParentWFObject().getRevision();
      DomOrExceptionGenerationBase domOrException = isException ?
          ExceptionGeneration.getOrCreateInstance(fqn, service.getCreator().getCacheReference(), revision) :
          DOM.getOrCreateInstance(fqn, service.getCreator().getCacheReference(), revision);

      try {
        domOrException.parse(false);
      } catch (Exception e) {
        throw new RuntimeException("Could not parse " + fqn);
      }

      if (label == null) {
        label = domOrException.getLabel();
      }

      var.createDomOrException(label, domOrException);
    }

    var.setIsList(isList);

    return var;
    
  }

  @Override
  public Pair<String, String> getSignaturePathAndName(VarUsageType usage, int index) {
    if (service.isPrototype()) {
      return super.getSignaturePathAndName(usage, index);
    }

    List<AVariable> vars;
    switch (usage) {
      case input:
        if (service.isDOMRef()) {
          vars = getOperation().getInputVars();
        } else {
          vars = service.getWF().getInputVars();
        }

        if (isInstanceMethod()) {
          vars = addSelfVar(vars, service.getLabel(), service.getDom());
        }
        break;
      case output:
        if (service.isDOMRef()) {
          vars = getOperation().getOutputVars();
        } else {
          vars = service.getWF().getOutputVars();
        }
        break;
      default:
        return null;
    }

    AVariable var;
    if (!Workflow.isAudit.get()) {
      var = vars.get(index);
    } else {
      // missing variables may occur in audits when the called workflow is missing in the XMOM repository
      // in this case, use fake variables to still be able to show the call with prototypes and runtime information
      var = (vars.size() > index) ? vars.get(index) : createPrototypeVar();
    }

    return new Pair<String, String>(var.getOriginalPath(), var.getOriginalName());
  }

  private AVariable createPrototypeVar() {
    AVariable var = new ServiceVariable(service.getWF());
    var.createPrototype("<Unknown>");

    return var;
  }

  public String getVarCastToFqn(VarUsageType usage, int index) {
    switch (usage) {
      case input:
        String[] inputVarCastToTypes = getInputVarCastToFqns();
        return (inputVarCastToTypes != null && inputVarCastToTypes.length > index && inputVarCastToTypes[index] != null && inputVarCastToTypes[index].length() > 0) ?
            inputVarCastToTypes[index] : null;
      case output:
        String[] receiveVarCastToTypes = getReceiveVarCastToFqns();
        return (receiveVarCastToTypes != null && receiveVarCastToTypes.length > index && receiveVarCastToTypes[index] != null && receiveVarCastToTypes[index].length() > 0) ?
            receiveVarCastToTypes[index] : null;
      default:
        break;
    }

    return null;
  }

  public void setVarCastToFqn(VarUsageType usage, int index, String castToFqn) throws UnsupportedOperationException {
    switch (usage) {
      case input:
        stepFunction.getInputVarCastToType()[index] = castToFqn;
        break;
      case output:
        if (stepFunction.isQueryStorable()) {
          // for queries, the type to cast to is always the storable the query is used on and can't be changed via dynamic typing
          new UnsupportedOperationException(UnsupportedOperationException.OPERATION_DYNAMIC_TYPING, UnsupportedOperationException.DYNAMIC_TYPING_QUERY_ONLY_IMPLICITLY);
        }

        // replace referred global variable with a new instance
        try {
          String newOutputVarFqn = null;
          if (castToFqn != null) {
            // dynamic typing is added -> new global variable needs to be of the type to be casted to
            newOutputVarFqn = castToFqn;
          } else {
            // dynamic typing is removed -> new global variable needs to be of the type of the service signature
            newOutputVarFqn = getOriginalVarFqn(usage, index);
          }

          AVariable oldOutputVar = stepFunction.getParentScope().identifyVariable(stepFunction.getOutputVarIds()[index]).getVariable();
          AVariable newOutputVar = createVariable(newOutputVarFqn, "", oldOutputVar.isList(), oldOutputVar instanceof ExceptionVariable);
          String label = newOutputVar.getDomOrExceptionObject() == null ? "AnyType" : newOutputVar.getDomOrExceptionObject().getLabel();
          
          newOutputVar.setLabel(label);
          newOutputVar.setId(oldOutputVar.getId());
          
          StepSerial surroundingStepSerial = (StepSerial)stepFunction.getParentStep().getParentStep();
          replaceCastedVariable(oldOutputVar, newOutputVar, surroundingStepSerial, newOutputVarFqn);

          // store type to be casted to
          stepFunction.getReceiveVarCastToType()[index] = castToFqn;
        } catch (Exception e) {
          String errorMessage = "Could not set dynamic type for variable " + getVariable(usage, index).internalGuiId.createId();
          Utils.logError(errorMessage, e);
          throw new RuntimeException(errorMessage, e);
        }
        break;
      default:
        throw new UnsupportedOperationException(UnsupportedOperationException.OPERATION_DYNAMIC_TYPING, UnsupportedOperationException.DYNAMIC_TYPING_ONLY_FOR_INPUTS_OUTPUTS);
    }

    identify();
  }
  
  
  private void replaceCastedVariable(AVariable oldVariable, AVariable newVariable, StepSerial stepSerial, String newOutputVarFqn) {
    if(stepSerial.getParentScope() instanceof ForEachScopeStep) {
      StepForeach foreachHoldingVariable = (StepForeach) stepSerial.getParentScope().getParentStep();
      int index = foreachHoldingVariable.getOutputVarsSingle(false).indexOf(oldVariable);
      if(index != -1) {
        //replace single variable
        AVariable[] vars = foreachHoldingVariable.getOutputVarsSingle();
        vars[index] = newVariable;

        //replace list variable
        try {
          AVariable listVar = stepSerial.getParentScope().identifyVariable(foreachHoldingVariable.getOutputListRefs()[index]).getVariable();
          AVariable newListVar = createVariable(newOutputVarFqn, newVariable.getLabel(), listVar.isList(), listVar instanceof ExceptionVariable);
          StepSerial foreachSerial = (StepSerial) foreachHoldingVariable.getParentStep();
          newListVar.setId(listVar.getId());

          replaceCastedVariable(listVar, newListVar, foreachSerial, newOutputVarFqn);
        } catch (XPRC_InvalidVariableIdException | XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
      } else {
        //variable does not participate in stepForeach output
        stepSerial.replaceVar(oldVariable, newVariable);
      }
    } else {
    //regular StepSerial - not inside
    stepSerial.replaceVar(oldVariable, newVariable);
    }
  }
  
  public String getOriginalVarFqn(VarUsageType usage, int index) {
    AVariable serviceSignatureVar = null;
    if (service.isDOMRef()) {
      switch(usage) {
        case input:
          // for instance methods of data types, add fake input variable that holds instance of the data type
          List<AVariable> operationInputs = getOperation().getInputVars();
          if (isInstanceMethod()) {
            operationInputs = addSelfVar(operationInputs, service.getLabel(), service.getDom());
          }
          serviceSignatureVar = operationInputs.get(index);
          break;
        case output:
          serviceSignatureVar = getOperation().getOutputVars().get(index);
          break;
        case thrown:
          serviceSignatureVar = getOperation().getThrownExceptions().get(index);
        break;            
      }
    } else {
      switch(usage) {
        case input:
          serviceSignatureVar = service.getWF().getInputVars().get(index);
          break;
        case output:
          serviceSignatureVar = service.getWF().getOutputVars().get(index);
          break;
        case thrown:
          serviceSignatureVar = service.getWF().getExceptionVars().get(index);
        break;            
      }
    }
    if(serviceSignatureVar != null) {
      return serviceSignatureVar.getOriginalPath() + "." + serviceSignatureVar.getOriginalName();
    } else {
        return null;
    }
  }

  private String[] getInputVarCastToFqns() {
    return stepFunction.getInputVarCastToType();
  }

  private String[] getReceiveVarCastToFqns() {
    if (!stepFunction.isQueryStorable()) {
      return stepFunction.getReceiveVarCastToType();
    }

    // step is a query -> an implicit cast of the output variable to the data type specified in selectionMask.rootType is done

    String queryCastToType = null;
    try {
      for (String inputVarId : stepFunction.getInputVarIds()) {
        AVariable inputVar = stepFunction.getParentScope().identifyVariable(inputVarId).getVariable();
        if (!FQN_SELECTION_MASK.equals(inputVar.getFQClassName())) {
          continue;
        }

        for (AVariable member : inputVar.getChildren()) {
          if (SELECTION_MASK_ROOT_TYPE.equals(member.getVarName())) {
            queryCastToType = member.getValue();
            break;
          }
        }
      }
    } catch (Exception e) {
      Utils.logError("Output type of query could not be dermined for dynamic casting. Falling back to considering output as Storable.", e);
    }

    String[] receiveVarCastToType = Arrays.copyOf(stepFunction.getReceiveVarCastToType(), stepFunction.getReceiveVarCastToType().length);
    if (queryCastToType != null) {
      for (int varIdx = 0; varIdx < receiveVarCastToType.length; varIdx++) {
        receiveVarCastToType[varIdx] = queryCastToType;
      }
    }

    return receiveVarCastToType;
  }

  private List<AVariableIdentification> fillServiceVars(final VarUsageType usage, String[] varIds, List<AVariable> vars) {
    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    if( stepFunction.getOrderInputSourceRef() == null && varIds.length != vars.size() ) { // bei orderinputsources Verwendung sind varIds leer
      if (Workflow.isAudit.get()) {
        // missing variables may occur in audits when the called workflow is missing in the XMOM repository
        // in this case, use fake variables to still be able to show the call with prototype variables and runtime information 
        for( int idx = 0; idx < varIds.length; ++idx ) {
          vars.add(createPrototypeVar());
        }
      } else {
        throw new IllegalStateException("varIds.length=" + varIds.length + ", serviceVars.size()=" + vars.size() + " for " + stepFunction);
      }
    }

    for( int idx = 0; idx < vars.size(); ++idx ) {
      AVariable aVar = vars.get(idx);

      final ServiceVarIdentification svi = ServiceVarIdentification.of(service, aVar);
      setFunctions(svi, usage, list);
      setFlags(svi, usage);
      list.add(svi);
    }

    return list;
  }

  @Override
  protected void setFunctions(final ReferencedVarIdentification rvi, final VarUsageType usage, final List<AVariableIdentification> list) {
    super.setFunctions(rvi, usage, list);

    if (usage == VarUsageType.input) {
      rvi.connectedness = new Connectedness() {

        @Override
        public boolean isUserConnected() {
          return (stepFunction.getOrderInputSourceRef() != null) ? false : 
              getInputConnections().getUserConnected()[indexOfNoEquals(list, rvi)];
        }

        @Override
        public String getConnectedVariableId() {
          return (stepFunction.getOrderInputSourceRef() != null) ? null :
            getInputConnections().getVarIds()[indexOfNoEquals(list, rvi)];
        }

        @Override
        public boolean isConstantConnected() {
          return (stepFunction.getOrderInputSourceRef() != null) ? false :
            getInputConnections().getConstantConnected()[indexOfNoEquals(list, rvi)];
        }
      };
    }
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    super.setFlags(var, usage);

    if (usage == VarUsageType.input && stepFunction.getOrderInputSourceRef() != null) {
      var.setConstPermission(ConstPermission.NEVER);
    } else if (usage == VarUsageType.output && !var.getIdentifiedVariable().isPrototype()) {
      var.setConstPermission(ConstPermission.FOR_BRANCHES);
    }

    if (service.isPrototype()) {
      var.setDeletable(true);
      var.setReadonly(false);
    } else {
      var.setDeletable(false);
      var.setReadonly(true);
    }
  }
  
  private boolean isInstanceMethod() {
    try {
      return service.isDOMRef() && (!service.getDom().getOperationByName(stepFunction.getOperationName()).isStatic());
    } catch (XPRC_OperationUnknownException e) {
      throw new IllegalStateException("Could not determine whether operation " + stepFunction.getOperationName() + " is instance method.", e);
    }
  }

  @Override
  public InputConnections getInputConnections() {
    return stepFunction.getInputConnections();
  }

  @Override
  public List<AVariable> getInputVars() {
    return service.getInputVars();
  }

  @Override
  public List<AVariable> getOutputVars() {
    return service.getOutputVars();
  }

  @Override
  public String[] getOutputVarIds() {
    return stepFunction.getOutputVarIds();
  }

  @Override
  public void addOutputVarId(int index, String id) {
    stepFunction.addOutputVarId(index, id);
  }

  @Override
  public void removeOutputVarId(int index) {
    stepFunction.removeOutputVarId(index);
  }

  @Override
  public ReferencedVarIdentification createVarIdentification(AVariable var) {
    return ServiceVarIdentification.of(service, var);
  }
  
  @Override
  public Step getStep() {
    return stepFunction;
  }

  @Override
  public AVariableIdentification getVariable(VarUsageType usage, int index) {
    if (usage != VarUsageType.input || index < getVariables(usage).size()) {
      return super.getVariable(usage, index);
    }

    if (index == Integer.MAX_VALUE) {
      // create fake variable for formula in query
      ServiceVariable storableVar = (ServiceVariable)outputVarIdentifications.get(0).getIdentifiedVariable();
      DatatypeVariable fakeVar = new ServiceVariable(new ServiceVariable(storableVar));
      fakeVar.setIsList(false);
      AVariableIdentification varIdent = DirectVarIdentification.of(fakeVar);
      varIdent.idprovider = () -> fakeVar.getId();

      return varIdent;
    }

    StepCatch stepCatch = ((StepCatch)stepFunction.getProxyForCatch());
    AVariable var = stepCatch.getCaughtExceptionVars().get(index - getVariables(usage).size());
    AVariableIdentification varIdent = DirectVarIdentification.of(var);
    varIdent.idprovider = () -> var.getId();

    return varIdent;
  }

}
