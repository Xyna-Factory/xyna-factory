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

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.adapter.ListAdapter;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.Connectedness;
import com.gip.xyna.xact.filter.util.AVariableIdentification.InternalGUIIdGeneration;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.util.DirectVarIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.InputConnections;

//abstrahiert eine modellierte schnittstelle, also inputs, outputs, exceptions, so dass vereinheitlicht damit gearbeitet werden kann
public abstract class IdentifiedVariables {

  protected final ObjectId id;
  protected boolean readonly;
  protected List<AVariableIdentification> inputVarIdentifications = new ArrayList<AVariableIdentification>();
  protected List<AVariableIdentification> outputVarIdentifications = new ArrayList<AVariableIdentification>();
  protected List<AVariableIdentification> thrownExceptions = new ArrayList<AVariableIdentification>();
  private boolean showLinkState = true;

  public IdentifiedVariables(ObjectId id) {
    this.id = id;
  }

  public boolean isReadOnly() {
    return readonly;
  }
  public void showLinkState(boolean showLinkState) {
    this.showLinkState = showLinkState;
  }
  public boolean isShowLinkState() {
    return showLinkState;
  }
  
  public List<AVariableIdentification> getVariables(VarUsageType usage) {
    switch( usage ) {
    case input:
      return inputVarIdentifications;
    case output:
      return outputVarIdentifications;
    case thrown:
      return thrownExceptions;
    default:
      break;
    }
    return null; //FIXME
  }
  
  /**
   * Führt Neuberechnung der Inputs und Outputs durch
   */
  public abstract void identify();

  public Pair<String, String> getSignaturePathAndName(VarUsageType usage, int index) {
    AVariableIdentification varIdent = getVariable(usage, index);
    return new Pair<String, String>(varIdent.getIdentifiedVariable().getOriginalPath(), varIdent.getIdentifiedVariable().getOriginalName());
  }

  public interface InputConnectionProvider {
    public InputConnections getInputConnections();
  }

  protected void setFlags(AVariableIdentification var, VarUsageType usage) {
    if (usage == VarUsageType.input && !var.getIdentifiedVariable().isPrototype()) {
      var.setConstPermission(ConstPermission.ALWAYS);
    } else {
      var.setConstPermission(ConstPermission.NEVER);
    }

    var.setDeletable(true);
    var.setReadonly(false);
    var.setAllowCast(isReadOnly() && !var.getIdentifiedVariable().isPrototype());
  }

  protected List<AVariableIdentification> fillDirectVars(final VarUsageType usage, List<? extends AVariable> vars, final InputConnectionProvider inputConnProvider) {
    final List<AVariableIdentification> list = new ArrayList<AVariableIdentification>();
    for (AVariable aVar : vars) {
      final AVariableIdentification var = DirectVarIdentification.of(aVar);
      setFlags(var, usage);

      if (inputConnProvider != null) {
        var.connectedness = new Connectedness() {

          @Override
          public boolean isUserConnected() {
            return inputConnProvider.getInputConnections().getUserConnected()[indexOfNoEquals(list, var)];
          }

          @Override
          public String getConnectedVariableId() {
            return inputConnProvider.getInputConnections().getVarIds()[indexOfNoEquals(list, var)];
          }

          @Override
          public boolean isConstantConnected() {
            return inputConnProvider.getInputConnections().getConstantConnected()[indexOfNoEquals(list, var)];
          }

        };
      }
      var.internalGuiId = new InternalGUIIdGeneration() {
        
        @Override
        public String createId() {
          return ObjectId.createVariableId(id.getBaseId(), usage, indexOfNoEquals(list, var));
        }
      };
      list.add(var);
    }
    return list;
  }

  public AVariableIdentification getVariable(VarUsageType usage, int index) {
    return getVariables(usage).get(index);
  }

  public List<AVariableIdentification> getListAdapter(VarUsageType usage) {
    return new IdentifiedVariablesListAdapter(this, usage);
  }
  
  protected static class IdentifiedVariablesListAdapter extends ListAdapter<AVariableIdentification> {

    private IdentifiedVariables identifiedVariables;
    private List<AVariableIdentification> vars;
    private VarUsageType usage;

    public IdentifiedVariablesListAdapter(IdentifiedVariables identifiedVariables, VarUsageType usage) {
      this.identifiedVariables = identifiedVariables;
      this.usage = usage;
      this.vars = identifiedVariables.getVariables(usage);
    }

    @Override
    public AVariableIdentification get(int index) {
      return vars.get(index);
    }

    @Override
    public int size() {
      return vars.size();
    }

    public VarUsageType getUsage() {
      return usage;
    }

    @Override
    public void add(int index, final AVariableIdentification element) {
      element.internalGuiId = new InternalGUIIdGeneration() {
        
        @Override
        public String createId() {
          return ObjectId.createVariableId(identifiedVariables.id.getBaseId(), usage, indexOfNoEquals(vars, element));
        }
      };
      identifiedVariables.add(usage, index, element);
    }
    
    @Override
    public AVariableIdentification remove(int index) {
      return identifiedVariables.remove(usage, index);
    }
    
  }

  protected abstract void add(VarUsageType usage, int index, AVariableIdentification element);

  protected abstract AVariableIdentification remove(VarUsageType usage, int index);
  

  protected void add(List<AVariable> vars, List<AVariableIdentification> varIds,
      int index, AVariableIdentification element) {
    vars.add(index, element.getIdentifiedVariable());
    varIds.add(index, element);
  }

  
  protected void addException(List<ExceptionVariable> vars, List<AVariableIdentification> varIds,
                     int index, AVariableIdentification element) {
    vars.add(index, (ExceptionVariable) element.getIdentifiedVariable());
    varIds.add(index, element);
  }
  
  protected AVariableIdentification remove(List<AVariable> vars, List<AVariableIdentification> varIds,
      int index) {
    vars.remove(index);
    AVariableIdentification removed = varIds.remove(index);
    return removed;
  }

  protected AVariableIdentification removeException(List<ExceptionVariable> vars, List<AVariableIdentification> varIds, int index) {
    vars.remove(index);
    AVariableIdentification removed = varIds.remove(index);
    return removed;
  }

  protected static int indexOfNoEquals(List<AVariableIdentification> list, AVariableIdentification v) {
    for (int i = 0; i<list.size(); i++) {
      if (list.get(i) == v) {
        return i;
      }
    }
    return -1;
  }

  protected List<AVariable> addSelfVar(List<AVariable> vars, String parentLabel, DOM parentDom) {
    DatatypeVariable dtInstanceForMethodCall = new DatatypeVariable(parentDom);
    dtInstanceForMethodCall.createDOM(parentLabel, parentDom);

    List<AVariable> varsWithSelfVar = new ArrayList<AVariable>(vars);
    varsWithSelfVar.add(0, dtInstanceForMethodCall);

    return varsWithSelfVar;
  }

}
