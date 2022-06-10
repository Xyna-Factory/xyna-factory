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
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;



public class IdentifiedVariablesStepThrow extends IdentifiedVariablesStep implements InputConnectionProvider {

  private final StepThrow stepThrow;


  public IdentifiedVariablesStepThrow(ObjectId id, StepThrow stepThrow) {
    super(id);
    this.stepThrow = stepThrow;
    this.readonly = false;

    identify();
  }


  @Override
  public void identify() {
//    this.inputVars = fillDirectVars(VarUsageType.input, stepThrow.getInputVars(), this);
    this.inputVarIdentifications = fillDirectVars(VarUsageType.input, stepThrow.getInputVars(), this);
  }

  @Override
  protected List<AVariableIdentification> fillDirectVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    List<AVariable> inputVars = new ArrayList<AVariable>(vars);
    if (inputVars.size() == 0) {
      // since a real input variable only exists when throw-step is already connected, add fake input instead to be shown in GUI and linked in dataflow
      
      //make sure createDummyVar succeeds
      if(stepThrow.getInputVars().size() == 0  && stepThrow.getExceptionTypeFqn() == null && stepThrow.getTargetExceptionVariable() != null) {
        Integer id = stepThrow.getXmlId();
        stepThrow.create(stepThrow.getTargetExceptionVariable().getVariable().getFQClassName(), stepThrow.getLabel());
        if( id != null) {
          stepThrow.setXmlId(id);
        }
      }
      
      
      ExceptionVariable fakeInputVar = stepThrow.createDummyVar();
      inputVars.add(fakeInputVar);
    }

    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (AVariable aVar : inputVars) {
      final ReferencedVarIdentification var = ReferencedVarIdentification.of(aVar);
      setFunctions(var, usage, list);
      setFlags(var, usage);
      list.add(var);
    }
    return list;
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    super.setFlags(var, usage);

    var.setReadonly(true);
    var.setDeletable(false);
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
    return stepThrow.getInputConnections();
  }

  @Override
  protected void add(VarUsageType usage, int index, AVariableIdentification element) {
    throw new RuntimeException("operation not supported");
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    throw new RuntimeException("operation not supported");
  }

  
  public void SetExceptionVariableId(String id) {
    stepThrow.setExceptionID(id);
  }
  
  @Override
  public Step getStep() {
    return stepThrow;
  }
}
