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
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;

public class IdentifiedVariablesStepForeach extends IdentifiedVariablesStepWithWfVars {

  private final StepForeach stepForeach;


  public IdentifiedVariablesStepForeach(ObjectId id, StepForeach stepForeach) {
    super(id, stepForeach);
    this.stepForeach = stepForeach;
    this.readonly = false;
    identify();
  }

  @Override
  public void identify() {
    this.inputVarIdentifications = fillDirectVars(VarUsageType.input, stepForeach.getInputVars(), this);
    for (AVariableIdentification v : inputVarIdentifications) {
      v.idprovider = new UseAVariable(v);
    }

    this.outputVarIdentifications = fillDirectVars(VarUsageType.output, stepForeach.getOutputVars(), null);
  }

  @Override
  protected List<AVariableIdentification> fillDirectVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (AVariable aVar : vars) {
      final ReferencedVarIdentification var = ReferencedVarIdentification.of(aVar, /*output*/ inputConnProvider == null);
      setFunctions(var, usage, list);
      setFlags(var, usage);
      list.add(var);
    }
    return list;
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    var.setDeletable(false);
    var.setReadonly(false);
    var.setConstPermission(ConstPermission.NEVER);
  }

  @Override
  public InputConnections getInputConnections() {
    return stepForeach.getInputConnections();
  }

  @Override
  public List<AVariable> getInputVars() {
    return stepForeach.getInputVars();
  }

  @Override
  public List<AVariable> getOutputVars() {
    return stepForeach.getOutputVars();
  }

  @Override //TODO: should be outputListRefs ? -> we want to point to Lists, not singles?
  public String[] getOutputVarIds() {
    return stepForeach.getOutputVarIds();
  }

  @Override
  public void addOutputVarId(int index, String id) {
    throw new UnsupportedOperationException("Adding outputs to a foreach is not supported.");
  }

  @Override
  public void removeOutputVarId(int index) {
    throw new UnsupportedOperationException("Removing outputs from a foreach is not supported.");
  }

  @Override
  public ReferencedVarIdentification createVarIdentification(AVariable var) {
    return ReferencedVarIdentification.of(var);
  }
  
  @Override
  public Step getStep() {
    return stepForeach;
  }

}
