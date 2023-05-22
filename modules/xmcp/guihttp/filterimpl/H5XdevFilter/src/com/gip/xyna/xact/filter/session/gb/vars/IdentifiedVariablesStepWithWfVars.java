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
package com.gip.xyna.xact.filter.session.gb.vars;

import java.util.List;

import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables.InputConnectionProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.InternalGUIIdGeneration;
import com.gip.xyna.xact.filter.util.AVariableIdentification.StepVariableIdProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.ReferencedVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;

public abstract class IdentifiedVariablesStepWithWfVars extends IdentifiedVariablesStep implements InputConnectionProvider {

  private final Step step;


  public IdentifiedVariablesStepWithWfVars(ObjectId id, Step step) {
    super(id);
    this.step = step;
  }


  protected void setFunctions(final ReferencedVarIdentification rvi, final VarUsageType usage, final List<AVariableIdentification> list) {
    if (usage == VarUsageType.input) {
      rvi.connectedness = new Connectedness() {

        @Override
        public boolean isUserConnected() {
          return getInputConnections().getUserConnected()[indexOfNoEquals(list, rvi)];
        }

        @Override
        public String getConnectedVariableId() {
          return getInputConnections().getVarIds()[indexOfNoEquals(list, rvi)];
        }

        @Override
        public boolean isConstantConnected() {
          return getInputConnections().getConstantConnected()[indexOfNoEquals(list, rvi)];
        }

      };
    } else if (usage == VarUsageType.output) {
      rvi.idprovider = new StepVariableIdProvider() {
        
        @Override
        public String getId() {
          return getOutputVarIds()[indexOfNoEquals(list, rvi)];
        }
      };
    }
    rvi.internalGuiId = new InternalGUIIdGeneration() {
      
      @Override
      public String createId() {
        return ObjectId.createVariableId(id.getBaseId(), usage, indexOfNoEquals(list, rvi));
      }
    };
  }

  @Override
  protected void add(VarUsageType usage, int index, final AVariableIdentification element) {
    if (readonly) {
      throw new IllegalStateException("Cannot add parameter to readonly step");
    }

    // if variable to be inserted is neither ServiceVariable nor ExceptionVariable, it must be converted
    AVariable var = element.getIdentifiedVariable();
    ReferencedVarIdentification replacement = null;
    if ( !(var instanceof ServiceVariable) && !(var instanceof ExceptionVariable) ) {
      var = createServiceOrExceptionVar(var, var.getId());
      replacement = createVarIdentification(var);
    }

    ReferencedVarIdentification vi;
    if (replacement != null) {
      vi = replacement;
    } else if (!(element instanceof ReferencedVarIdentification)) {
      vi = createVarIdentification(var);
    } else {
      vi = (ReferencedVarIdentification) element;
    }
    
    setFlags(vi, usage);

    switch( usage ) {
      case input:
        add(getInputVars(), inputVarIdentifications, index, vi);
        addInputConnection(index);
        setFunctions(vi, usage, inputVarIdentifications);
        break;
      case output:
        add(getOutputVars(), outputVarIdentifications, index, vi);
        setFunctions(vi, usage, outputVarIdentifications);

        if (!var.isPrototype()) {
          // create clone variable to be used at workflow-level
          AVariable clone = createServiceOrExceptionVar(var, String.valueOf(var.getCreator().getNextXmlId()));
          StepSerial ss = step.getParentScope().getChildStep();
          ss.addVar(clone);
          addOutputVarId(indexOfNoEquals(outputVarIdentifications, vi), clone.getId());
        } else {
          addOutputVarId(indexOfNoEquals(outputVarIdentifications, vi), "");
        }
        break;
      case thrown:
        break;
      default:
        break;
    }
  }

  @Override
  protected AVariableIdentification remove(VarUsageType usage, int index) {
    if( readonly ) {
      throw new IllegalStateException("Can not add parameter to readonly service");
    }
    AVariableIdentification removed;
    switch( usage ) {
    case input:
      removed = remove(getInputVars(), inputVarIdentifications, index);
      removeInputConnection(index);
      return removed;
    case output:
      removed = remove(getOutputVars(), outputVarIdentifications, index);
//      removeOutputVarId(indexOfNoEquals(outputVars, removed));
      removeOutputVarId(index); // TODO: Is this index always correct? If not, indexOfNoEquals can't be used, because variable won't be found, since outputVars contains the id of the clone that has been added in the add-method of this class
      return removed;
    case thrown:
      break;
    default:
      break;
    }
    return null;
  }

  public static AVariable createServiceOrExceptionVar(AVariable var, String id) {
    AVariable clone;
    if (var instanceof DatatypeVariable) {
      // create new ServiceVariable, based on DataTypeVariable
      clone = new ServiceVariable((DatatypeVariable)var);
    } else {
      clone = new ExceptionVariable((ExceptionVariable)var);
    }
    
    //TODO: cleanup -- superClassGenerationObject not set in clone (java8 only)
    //ensure type hierarchy still works
    if(var.getDomOrExceptionObject() != null 
        && clone.getDomOrExceptionObject() instanceof DOM && 
        var.getDomOrExceptionObject().getSuperClassGenerationObject() != null &&
        var.getDomOrExceptionObject().getSuperClassGenerationObject() instanceof DOM) {
      ((DOM)clone.getDomOrExceptionObject()).replaceParent((DOM)var.getDomOrExceptionObject().getSuperClassGenerationObject());
    }

    clone.setId(id);
    return clone;
  }

  public void addInputConnection(int index) {
    getInputConnections().addInputConnection(index);
  }

  public void removeInputConnection(int index) {
    getInputConnections().removeInputConnection(index);
  }

  public abstract List<AVariable> getInputVars();
  public abstract List<AVariable> getOutputVars();
  public abstract String[] getOutputVarIds();
  public abstract void addOutputVarId(int index, String id);
  public abstract void removeOutputVarId(int index);
  public abstract ReferencedVarIdentification createVarIdentification(AVariable var);

}
