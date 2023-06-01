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

package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepSerial;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class WorkflowOperation extends Operation {
  
  private WF wf;
  private String name;
  private String label;
  
  
  public WorkflowOperation(WF wf) {
    this.wf = wf;
    this.name = wf.getOriginalSimpleName();
    this.label = wf.getLabel();
  }
  
  public WorkflowOperation(WF wf, String name, String label) {
    this.wf = wf;
    this.name = name;
    this.label = label;
  }
  
  @Override
  protected void appendOperationContentToXML(XmlBuilder xml) {
    StepSerial topLevelStep = wf.getWfAsStep().getChildStep();
    
    // collect service references of all steps, since they have to be added separately on the top-level
    try {
      Set<Pair<Service, StepFunction>> topLevelServiceReferences = topLevelStep.getProxyForCatch().getAllServiceReferences();
      for (Pair<Service, StepFunction> referencedService : topLevelServiceReferences) {
        XMLUtils.appendServiceReference(xml, referencedService, true);
      }
    } catch (XPRC_InvalidServiceIdException e) {
      return; // TODO: better error handling?
    }
    
    topLevelStep.appendXML(xml);
  }
  
  @Override
  public String getLabel() {
    return label;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public boolean isStatic() {
    return false;
  }
  
  @Override
  public boolean isFinal() {
    return false;
  }
  
  @Override
  public boolean isAbstract() {
    return false;
  }
  
  @Override
  public boolean requiresXynaOrder() {
    return false;
  }
  
  @Override
  public String getDocumentation() {
    return wf.getDocumentation();
  }
  
  @Override
  public List<Variable> getInputs() {
    List<Variable> inputs = new ArrayList<Variable>();
    for (AVariable var : wf.getWfAsStep().getInputVars()) {
      inputs.add(Utils.createVariable(var));
    }
    
    return inputs;
  }
  
  @Override
  public List<Variable> getOutputs() {
    List<Variable> outputs = new ArrayList<Variable>();
    for (AVariable var : wf.getWfAsStep().getOutputVars()) {
      outputs.add(Utils.createVariable(var));
    }
    
    return outputs;
  }
  
  @Override
  public List<Variable> getExceptions() {
    List<Variable> exceptions = new ArrayList<Variable>();
    for (ExceptionVariable var : wf.getWfAsStep().getAllThrownExceptions(false)) {
      Variable sVar = Utils.createVariable(var);
      sVar.id = null; //workflow Exception Variables (for XML generation) have no id
      exceptions.add(sVar);
    }
    
    return exceptions;
  }
  
  @Override
  public String getId() {
    return wf.getWfAsStep().getChildStep().getStepId();
  }

  @Override
  public boolean hasUnknownMetaTags() {
    return wf.hasUnknownMetaTags();
  }

  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {
    wf.appendUnknownMetaTags(xml);
  }

  @Override
  public boolean hasBeenPersisted() {
    return false;
  }

}
