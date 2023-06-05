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
package com.gip.xyna.xact.filter.session.gb.vars;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

public class IdentifiedVariablesService extends IdentifiedVariables {

  private final Operation operation;

  public IdentifiedVariablesService(ObjectId id, Operation operation, DOM dom) {
    super(id);

    this.operation = operation;
    this.readonly = operation.isInheritedOrOverriden(dom);

    identify();
  }

  @Override
  public void identify() {
    this.inputVarIdentifications = fillServiceVars(VarUsageType.input, operation.getInputVars(), null);
    this.outputVarIdentifications = fillServiceVars(VarUsageType.output, operation.getOutputVars(), null);
    this.thrownExceptions = fillServiceVars(VarUsageType.thrown, operation.getThrownExceptions(), null);
  }

  private List<AVariableIdentification> fillServiceVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    List<AVariable> serviceVars = new ArrayList<>(vars);
    if (usage == VarUsageType.input && !operation.isStatic()) {
      // add fake input variable that holds instance of the data type
      serviceVars = addSelfVar(serviceVars, operation.getParent().getLabel(), operation.getParent());
      List<AVariableIdentification> varIdentifications = fillDirectVars(usage, serviceVars, inputConnProvider);
      varIdentifications.get(0).setDeletable(false);
      varIdentifications.get(0).setReadonly(true);

      return varIdentifications;
    }

    return fillDirectVars(usage, serviceVars, inputConnProvider);
  }
  
  @Override
  protected List<AVariableIdentification> fillDirectVars(VarUsageType usage, List<? extends AVariable> vars,
                                                         InputConnectionProvider inputConnProvider) {
    List<AVariableIdentification> aVariableIdentifications = super.fillDirectVars(usage, vars, inputConnProvider);
    List<AVariableIdentification> result = new ArrayList<>(aVariableIdentifications.size());
    aVariableIdentifications.forEach(ident -> {
      ident.internalGuiId = () -> ObjectId.createVariableId(id.getBaseId(), usage, indexOfNoEquals(result, ident));
      result.add(ident);
    });
    return result;
  }

  @Override
  protected void add(VarUsageType usage, int index, AVariableIdentification element) {
    switch( usage ) {
    case input:
      int operationVarIndex = index;
      if (inputVarIdentifications.size() - operation.getInputVars().size() == 1) { // special case instance variable
        index = (index == 0) ? index + 1 : index; // adding before instance variable is not allowed
        operationVarIndex = index - 1;
      }

      operation.getInputVars().add(operationVarIndex, element.getIdentifiedVariable());
      inputVarIdentifications.add(index, element);
      break;
    case output:
      add(operation.getOutputVars(), outputVarIdentifications, index, element);
      break;
    case thrown:
      List<ExceptionVariable> currentExceptionVars = operation.getThrownExceptions();
      for (ExceptionVariable var : currentExceptionVars) {
        if(var.getFQClassName().equals(element.getIdentifiedVariable().getFQClassName())) {
          return;
        }
      }

      addException(operation.getThrownExceptionsForMod(), thrownExceptions, index, element);
      break;
    default:
      break;
    }
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    AVariableIdentification result = null;
    switch( usage ) {
    case input:
      int operationVarIndex = index;
      if (inputVarIdentifications.size() - operation.getInputVars().size() == 1) { // special case instance variable
        index = (index == 0) ? index + 1 : index; // adding before instance variable is not allowed
        operationVarIndex = index - 1;
      }

      operation.getInputVars().remove(operationVarIndex);
      result = inputVarIdentifications.remove(index);
      break;
    case output:
      result = remove(operation.getOutputVars(), outputVarIdentifications, index);
      break;
    case thrown:
      result = removeException(operation.getThrownExceptionsForMod(), thrownExceptions, index);
      break;
    default:
      break;
    }
    return result;
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    var.setConstPermission(ConstPermission.FOR_BRANCHES);
    var.setAllowCast(false);
    var.setReadonly(readonly);
    var.setDeletable(!readonly);
  }

}
