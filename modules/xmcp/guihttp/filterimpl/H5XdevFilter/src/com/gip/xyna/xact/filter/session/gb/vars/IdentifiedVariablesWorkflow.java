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

import java.util.List;

import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.InternalGUIIdGeneration;
import com.gip.xyna.xact.filter.util.AVariableIdentification.UseAVariable;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepAssign;
import com.gip.xyna.xprc.xfractwfe.generation.StepCatch;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

public class IdentifiedVariablesWorkflow extends IdentifiedVariables {

  private final WF workflow;

  public IdentifiedVariablesWorkflow(ObjectId id, WF workflow) {
    super(id);
    this.workflow = workflow;
    this.readonly = false;
    identify();
  }

  @Override
  public void identify() {
    this.inputVarIdentifications = fillDirectVars( VarUsageType.input, workflow.getInputVars(), null );
    for (AVariableIdentification v : inputVarIdentifications) {
      v.idprovider = new UseAVariable(v);
    }
    this.outputVarIdentifications = fillDirectVars( VarUsageType.output, workflow.getOutputVars(), null );
    for (final AVariableIdentification v : outputVarIdentifications) {
      initOutput(v);
    }
  }

  @Override
  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    super.setFlags(var, usage);

    if (usage == VarUsageType.output && !var.getIdentifiedVariable().isPrototype()) {
      var.setConstPermission(ConstPermission.ALWAYS);
    } else {
      var.setConstPermission(ConstPermission.NEVER);
    }
  }

  private void initOutput(final AVariableIdentification v) {
    v.idprovider = new UseAVariable(v);
    v.connectedness = new Connectedness() { //infos aus globalem stepassign

      @Override
      public boolean isUserConnected() {
        StepAssign sa = getStepAssign();
        if (sa == null) {
          return false;
        }

        // suche den inputconnections-index, wo der output auf die id zeigt
        InputConnections input = sa.getInputConnections();
        int varIndex = indexOf(sa.getOutputVarIds(), v.idprovider.getId());
        return (varIndex >= 0) ? input.getUserConnected()[varIndex] : false;
      }

      private int indexOf(String[] outputVarIds, String id) {
        for (int i = 0; i < outputVarIds.length; i++) {
          if (outputVarIds[i].equals(id)) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public String getConnectedVariableId() {
        StepAssign sa = getStepAssign();
        if (sa == null) {
          return null;
        }

        // suche den inputconnections-index, wo der output auf die id zeigt
        InputConnections input = sa.getInputConnections();
        int varIndex = indexOf(sa.getOutputVarIds(), v.idprovider.getId());
        return (varIndex >= 0) ? input.getVarIds()[varIndex] : null;
      }

      private StepAssign getStepAssign() {
        List<Step> childSteps = workflow.getWfAsStep().getChildStep().getChildSteps();
        if (childSteps.size() == 0 || !(childSteps.get(childSteps.size() - 1) instanceof StepAssign)) {
          return null;
        }
        return (StepAssign) childSteps.get(childSteps.size() - 1);
      }

      @Override
      public boolean isConstantConnected() {
        StepAssign sa = getStepAssign();
        if (sa == null) {
          return false;
        }

        // suche den inputconnections-index, wo der output auf die id zeigt
        InputConnections input = sa.getInputConnections();
        int varIndex = indexOf(sa.getOutputVarIds(), v.idprovider.getId());
        return (varIndex >= 0) ? input.getConstantConnected()[varIndex] : false;
      }
    };
  }

  @Override
  protected void add(VarUsageType usage, int index, AVariableIdentification element) {
    switch( usage ) {
    case input:
      add( workflow.getInputVars(), inputVarIdentifications, index, element);
      setFunctions(element, usage, inputVarIdentifications);
      setFlags(element, usage);
      refreshWfStep();
      break;
    case output:
      add( workflow.getOutputVars(), outputVarIdentifications, index, element);
      setFunctions(element, usage, inputVarIdentifications);
      setFlags(element, usage);
      refreshWfStep();
      break;
    case thrown:
      break;
    default:
      break;
    }
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    AVariableIdentification var;
    switch( usage ) {
    case input:
      var = remove( workflow.getInputVars(), inputVarIdentifications, index);
      refreshWfStep();
      return var;
    case output:
      var = remove( workflow.getOutputVars(), outputVarIdentifications, index);
      refreshWfStep();
      return var;
    case thrown:
      break;
    default:
      break;
    }
    return null;
  }

  private void setFunctions(final AVariableIdentification vi, final VarUsageType usage, final List<AVariableIdentification> list) {
    if (usage == VarUsageType.input) {
      vi.idprovider = new UseAVariable(vi);

      vi.connectedness = new Connectedness() {
        public String getConnectedVariableId() {
          return null;
        }
        
        @Override
        public boolean isUserConnected() {
          return false;
        }
  
        @Override
        public boolean isConstantConnected() {
          return false;
        }
      };

      vi.internalGuiId = new InternalGUIIdGeneration() {
        @Override
        public String createId() {
          return ObjectId.createVariableId(id.getBaseId(), usage, indexOfNoEquals(list, vi));
        }
      };
    } else if (usage == VarUsageType.output) {
      initOutput(vi);
    }
  }

  private void refreshWfStep() {
    workflow.getWfAsStep().refreshVars(workflow.getInputVars(), workflow.getOutputVars() );
  }

  @Override
  public AVariableIdentification getVariable(VarUsageType usage, int index) {
    if (usage != VarUsageType.input || index < getVariables(usage).size()) {
      return super.getVariable(usage, index);
    }

    StepCatch stepCatch = (StepCatch)workflow.getWfAsStep().getChildStep().getProxyForCatch();
    AVariable var = stepCatch.getCaughtExceptionVars().get(index - getVariables(usage).size());
    AVariableIdentification varIdent = DirectVarIdentification.of(var);
    varIdent.idprovider = () -> var.getId();

    return varIdent;
  }

}
