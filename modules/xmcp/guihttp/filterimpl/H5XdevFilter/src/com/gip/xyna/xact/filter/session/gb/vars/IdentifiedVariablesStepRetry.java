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
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables.InputConnectionProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.InternalGUIIdGeneration;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;



public class IdentifiedVariablesStepRetry extends IdentifiedVariablesStep implements InputConnectionProvider {

  public final String RETRY_PARAMETER_PATH  = "xprc.retry";
  public final String RETRY_PARAMETER_NAME  = "RetryParameter";
  public final String RETRY_PARAMETER_LABEL = "Retry Parameter";

  private final StepRetry stepRetry;


  public IdentifiedVariablesStepRetry(ObjectId id, StepRetry stepRetry) {
    super(id);
    this.stepRetry = stepRetry;
    this.readonly = true;

    identify();
  }


  @Override
  public void identify() {
//    this.inputVars = fillDirectVars(VarUsageType.input, stepRetry.getInputVars(), this);
    this.inputVarIdentifications = fillDirectVars(VarUsageType.input, new ArrayList<AVariable>(), this);
  }

  @Override
  protected List<AVariableIdentification> fillDirectVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    List<AVariable> inputVars = new ArrayList<AVariable>();
    inputVars.addAll(vars);
    if (inputVars.size() == 0) {
      // since a real input variable only exists when retry-step is already connected, add fake input instead to be shown in GUI and linked in dataflow

      DatatypeVariable fakeInputVar = new DatatypeVariable(stepRetry.getCreator());
      try {
        fakeInputVar.create(RETRY_PARAMETER_PATH, RETRY_PARAMETER_NAME);
        fakeInputVar.setLabel(RETRY_PARAMETER_LABEL);
      } catch (XPRC_InvalidPackageNameException e) {} // nothing to do

      inputVars = new ArrayList<AVariable>(vars);
      inputVars.add(fakeInputVar);
    }
    
    String fqnFakeVar = RETRY_PARAMETER_PATH + "." + RETRY_PARAMETER_NAME;

    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (AVariable aVar : inputVars) {
      final ReferencedVarIdentification var = ReferencedVarIdentification.of(aVar);
      setFunctions(var, usage, list);
      setFlags(var, usage);
      if(fqnFakeVar.equals(aVar.getFQClassName())) {
        var.setDeletable(false);
        var.setReadonly(true);
        var.setAllowCast(false);
      }
      list.add(var);
    }
    return list;
  }

  private void setFunctions(final ReferencedVarIdentification svi, final VarUsageType usage, final List<AVariableIdentification> list) {
    if (usage == VarUsageType.input) {
      svi.connectedness = new Connectedness() {
        @Override
        public boolean isUserConnected() {
          return getInputConnections().getUserConnected()[indexOfNoEquals(list, svi)];
        }

        @Override
        public String getConnectedVariableId() {
          return getInputConnections().getVarIds()[indexOfNoEquals(list, svi)];
        }

        @Override
        public boolean isConstantConnected() {
          return getInputConnections().getConstantConnected()[indexOfNoEquals(list, svi)];
        }
      };
    }

    svi.internalGuiId = new InternalGUIIdGeneration() {
      @Override
      public String createId() {
        return ObjectId.createVariableId(id.getBaseId(), usage, indexOfNoEquals(list, svi));
      }
    };
  }

  @Override
  public InputConnections getInputConnections() {
    return stepRetry.getInputConnections();
  }

  @Override
  protected void add(VarUsageType usage, int index, AVariableIdentification element) {
    throw new RuntimeException("operation not supported");
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    throw new RuntimeException("operation not supported");
  }
  
  @Override
  public Step getStep() {
    return stepRetry;
  }

}
