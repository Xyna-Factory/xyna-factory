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
package com.gip.xyna.xact.filter.session.gb.vars;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

public class IdentifiedVariablesStepMapping extends IdentifiedVariablesStepWithWfVars {

  private final StepMapping stepMapping;


  public IdentifiedVariablesStepMapping(ObjectId id, StepMapping stepMapping) {
    super(id, stepMapping);
    this.stepMapping = stepMapping;
    this.readonly = false;
    identify();
  }

  @Override
  public void identify() {
    this.inputVarIdentifications = fillDirectVars(VarUsageType.input, stepMapping.getInputVars(), this);
    this.outputVarIdentifications = fillDirectVars(VarUsageType.output, stepMapping.getOutputVars(), null);
  }

  @Override
  protected List<AVariableIdentification> fillDirectVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (AVariable aVar : vars) {
      final ReferencedVarIdentification var = ReferencedVarIdentification.of(aVar);
      setFunctions(var, usage, list);
      setFlags(var, usage);
      list.add(var);
    }
    return list;
  }

  @Override
  protected void add(VarUsageType usage, int index, final AVariableIdentification element) {
    super.add(usage, index, element);

    // update expressions
    switch (usage) {
      case input:
        stepMapping.inputVarAdded(index);
        break;
      case output:
        stepMapping.outputVarAdded(index);
        break;
      default:
        break;
    }
  }
  
  public void move(VarUsageType usage, int sourceIndex, int destinationIndex) {
    int calculatedDestinationIndex = destinationIndex;
    int varListSize = getListAdapter(usage).size();
    if(destinationIndex < 0) {
      calculatedDestinationIndex = varListSize - 1;
    } else {
        calculatedDestinationIndex = destinationIndex;
    }
    AVariableIdentification var = super.remove(usage, sourceIndex);
    super.add(usage, calculatedDestinationIndex, var);
    
    // update expressions
    switch (usage) {
      case input:
        stepMapping.inputVarMoved(sourceIndex, calculatedDestinationIndex);
        break;
      case output:
        stepMapping.outputVarMoved(sourceIndex, calculatedDestinationIndex);
        break;
      default:
        break;
    }
    
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    AVariableIdentification removedVar = super.remove(usage, index);

    // update expressions
    switch (usage) {
      case input:
        stepMapping.inputVarRemoved(index);
        break;
      case output:
        stepMapping.outputVarRemoved(index);
        break;
      default:
        break;
    }

    return removedVar;
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    super.setFlags(var, usage);

    if (stepMapping.isTemplateMapping()) {
      var.setConstPermission(ConstPermission.NEVER);
    }
  }

  @Override
  public InputConnections getInputConnections() {
    return stepMapping.getInputConnections();
  }

  @Override
  public void addInputConnection(int index) {
    super.addInputConnection(index);
    stepMapping.refreshContainers();
  }

  @Override
  public void removeInputConnection(int index) {
    super.removeInputConnection(index);
    stepMapping.refreshContainers();
  }

  @Override
  public List<AVariable> getInputVars() {
    return stepMapping.getInputVars();
  }

  @Override
  public List<AVariable> getOutputVars() {
    return stepMapping.getOutputVars();
  }

  @Override
  public String[] getOutputVarIds() {
    return stepMapping.getOutputVarIds();
  }

  @Override
  public void addOutputVarId(int index, String id) {
    stepMapping.addOutputVarId(index, id);
  }

  @Override
  public void removeOutputVarId(int index) {
    stepMapping.removeOutputVarId(index);
  }

  @Override
  public ReferencedVarIdentification createVarIdentification(AVariable var) {
    return ReferencedVarIdentification.of(var);
  }
  
  @Override
  public Step getStep() {
    return stepMapping;
  }

}
