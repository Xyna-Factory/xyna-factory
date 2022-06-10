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

package com.gip.xyna.xact.filter.session.workflowissues;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.ServiceVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

public class StepBasedIdentificationNoPath extends StepBasedIdentification{

  private Step step;
  private List<String> ids;
  private RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  private XMOMLoader loader;
  
  public StepBasedIdentificationNoPath(Step step, XMOMLoader loader) {
    super(step);
    this.step = step;
    this.loader = loader;
    ids = new ArrayList<String>();
    
    for(int i=0; i<step.getInputVarIds().length; i++) {
      ids.add(step.getInputVarIds()[i]);
    }
    

    for(int i=0; i<step.getOutputVarIds().length; i++) {
      ids.add(step.getOutputVarIds()[i]);
    }
  }
  
  private AVariable getVariable(int varNum) {
    List<AVariable> inputVars = step.getInputVars();
    
    //if conditional mapping, remove one for query output
    //if varNum == 0, then we change variable to query output later
    if(step instanceof StepMapping && ((StepMapping)step).isConditionMapping() && varNum > 0) {
      varNum--;
    }
    
    if(varNum < inputVars.size()) {
      return inputVars.get(varNum);
    } else {
      varNum -= inputVars.size();
      return step.getOutputVars().get(varNum);
    }
  }
  
  
  @Override
  public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
    String varId = ids.get(v.getVarNum());
    
    AVariable variable = getVariable(v.getVarNum());
    SingleVariableIdentification vi = null;
    
    try {
      variable.getDomOrExceptionObject().parse(false);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
    }
    
    //if this is a conditional Mapping && we are processing %0%
    //then we should return a single Type, not a list
    if(isQueryOutputVar(v)) {
      variable = identifyVariable(varId).getVariable();
      AVariable var = AVariable.createAVariable("-1", variable.getDomOrExceptionObject(), false);
      vi = new SingleVariableIdentification(var);
    } else {
      vi = new SingleVariableIdentification(variable);
    }
    
    boolean isAnyType = determineIsAnyType(vi.getVariable());
    
    if(isAnyType) {
      StepBasedVariable vid = new StepBasedVariable("", vi, this);
      return vid;
    }
    
    
    if (step instanceof StepMapping) {
      StepMapping sm = (StepMapping) step;
      
      String dataModel = null;
      
      try {
        dataModel = sm.getDataModel(varId);
      } catch(Exception e) {
       if(!isQueryOutputVar(v)) {
         throw new RuntimeException(e);
       }
      }
      
      if (dataModel != null && vi.getVariable().getDomOrExceptionObject() instanceof DOM) {
        //manche der in-/output variablen können pathmaps sein.        
        DOM dom = (DOM) vi.getVariable().getDomOrExceptionObject();
        if (dom.getPathMapInformation() != null) {
          //variable ist pathmap
          return new VariableInfoPathMap(vi, this, sm.getDataModel(varId), v);
        }
      }
    }

    
    StepBasedVariable vid = new StepBasedVariable("", vi, this);
    if(followAccessParts) {
      vid.follow(v.getParts(), v.getParts().size() -1);
    }
    
    
    
    return vid;
  }
  
  
  private boolean determineIsAnyType(AVariable vi) {
    if(vi == null) {
      return false;
    }
    
    if(!(vi instanceof ServiceVariable)) {
      return false;
    }
    
    ServiceVariable sv = (ServiceVariable)vi;
    
    return sv.getOriginalPath().equals(GenerationBase.ANYTYPE_REFERENCE_PATH) && sv.getOriginalName().equals(GenerationBase.ANYTYPE_REFERENCE_NAME);
  }

  public TypeInfo getTypeInfo(String originalXmlName) {
    long rev = rcdm.getRevisionDefiningXMOMObjectOrParent(originalXmlName, step.getCreator().getRevision());
    GenerationBaseObject gbo = null;
    try {
      FQName fqName = new FQName(rev, originalXmlName);
      gbo = loader.load(fqName, true);
    } catch (XynaException e) {
      return null;
    }
    GenerationBase gb = gbo.getGenerationBase();
    if (gb == null) {
      return null;
    } else {
      return new TypeInfo(new StepBasedVariable.StepBasedType((DomOrExceptionGenerationBase)gb, this), false);
    }
  }
  
  
  
  private boolean isQueryOutputVar(Variable v) {
    return step instanceof StepMapping && ((StepMapping)step).isConditionMapping() && v.getVarNum() == 0;
  }
  
  public List<String> getIds() {
    return ids;
  }

  
  private static class SingleVariableIdentification extends VariableIdentification {
    
    private AVariable singleVariable;
    
    public SingleVariableIdentification(AVariable singleVariable) {
      this.singleVariable = singleVariable;
    }
    
    @Override
    public AVariable getVariable() {
      return singleVariable;
    }
  }
}
