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
package com.gip.xyna.xact.filter.session.gb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesService;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepChoice;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepForeach;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepFunction;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepMapping;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepRetry;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepThrow;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesWorkflow;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.ThrowExceptionIdProvider;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepChoice;
import com.gip.xyna.xprc.xfractwfe.generation.StepForeach;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;
import com.gip.xyna.xprc.xfractwfe.generation.StepRetry;
import com.gip.xyna.xprc.xfractwfe.generation.StepThrow;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

public class VariableMap {

  
  @SuppressWarnings("unused")
  private static final Logger logger = CentralFactoryLogging.getLogger(VariableMap.class);

  private final GenerationBaseObject gbo;
  private final Map<String,IdentifiedVariables> identifiedVariables;
  
  public VariableMap(GenerationBaseObject gbo) {
    this.gbo = gbo;
    this.identifiedVariables = new HashMap<String,IdentifiedVariables>();
  }

  public IdentifiedVariables identifyVariables(ObjectId objectId) {
    IdentifiedVariables vars = null;
    if(objectId.getBaseId() != null) {
      vars = identifiedVariables.get(objectId.getBaseId());
    }
    if( vars == null ) {
      vars = identifyVariablesNow(objectId);
    }
    return vars;
  }
  
  public void refreshIdentifiedVariables(ObjectId objectId) {
    switch( objectId.getType() ) {
      case methodVarArea:
        identifiedVariables.remove(objectId.getBaseId());
        identifyVariables(objectId);
        break;
      case memberMethodsArea:
      case memberMethod:
        OperationInformation[] operationInformations = gbo.getDOM().collectOperationsOfDOMHierarchy(true);
        for (int i = 0; i < operationInformations.length; i++) {
          identifiedVariables.remove(String.valueOf(i));
          identifiedVariables.put(String.valueOf(i), identifyOperationVariables(objectId, i));
        }
        break;
      default:
        break;
    }
  }
  
  private IdentifiedVariables identifyVariablesNow(ObjectId objectId) {
    IdentifiedVariables vars;
    switch( objectId.getType() ) {
    case step:
    case exceptionHandling:
      Step step = gbo.getStep(objectId.getBaseId());
      return identifyStepVariablesNow(objectId, step); 
    case variable:
      ObjectId parent = null;
      if(gbo.getType() == XMOMType.DATATYPE) {
        vars = identifyOperationVariables(objectId, Integer.valueOf(objectId.getBaseId()));
        identifiedVariables.put(objectId.getBaseId(), vars);
        return vars;
      } else {
        if( objectId.getBaseId() == null || objectId.getBaseId().isEmpty() ) {
          parent = new ObjectId(ObjectType.workflow, objectId.getBaseId() );
        } else {
          parent = new ObjectId(ObjectType.step, objectId.getBaseId() );
        }
      }
      return identifyVariables(parent);
    case workflow:
    case exceptionHandlingWf:
      vars = new IdentifiedVariablesWorkflow( objectId, gbo.getWorkflow() );
      identifiedVariables.put(objectId.getBaseId(), vars);
      return vars;
    case methodVarArea:
    case operation:
      return identifyOperationVariables(objectId, Integer.valueOf(objectId.getBaseId()));
    default:
      throw new IllegalStateException("Could not identify variables for type "+objectId.getType() );
    }
  }
  
  private IdentifiedVariablesService identifyOperationVariables(ObjectId objectId, int methodIndex) {
    OperationInformation[] operationInformations = gbo.getDOM().collectOperationsOfDOMHierarchy(true);
    OperationInformation oi = operationInformations[methodIndex];
    IdentifiedVariablesService vars = new IdentifiedVariablesService(objectId, oi.getOperation(), gbo.getDOM());
    identifiedVariables.put(String.valueOf(methodIndex), vars);

    return vars;
  }
  
  private IdentifiedVariables identifyStepVariablesNow(ObjectId objectId, Step step) {
    IdentifiedVariables vars;
    if( step instanceof StepFunction ) {
      vars = new IdentifiedVariablesStepFunction(objectId, (StepFunction)step);
    } else if( step instanceof StepChoice ) {
      vars = new IdentifiedVariablesStepChoice(objectId, (StepChoice)step);
    } else if( step instanceof StepMapping ) {
      vars = new IdentifiedVariablesStepMapping(objectId, (StepMapping)step);
    } else if( step instanceof StepThrow ) {
      vars = new IdentifiedVariablesStepThrow(objectId, (StepThrow)step);
    } else if( step instanceof StepRetry ) {
      vars = new IdentifiedVariablesStepRetry(objectId, (StepRetry)step);
    } else if( step instanceof StepForeach ) {
      vars = new IdentifiedVariablesStepForeach(objectId, (StepForeach)step);
    } else if( step == null ) {
      throw new IllegalStateException("Cannot get step "+objectId.getBaseId() );
    } else if( step instanceof WFStep ) {
      return identifyVariables( new ObjectId(ObjectType.workflow, null) );
    } else {
      throw new IllegalStateException("Cannot identify variables for step "+step.getClass() );
    }
    identifiedVariables.put(objectId.getBaseId(), vars);
    return vars;
  }

  
  public AVariableIdentification scan(String varId) {
    for (IdentifiedVariables iVars : identifiedVariables.values()) {
      for (VarUsageType usageType : VarUsageType.values()) {
        List<AVariableIdentification> aVarIds = iVars.getVariables(usageType);
        if (aVarIds != null) {
          for (AVariableIdentification aVarId : aVarIds) {
            if (!(aVarId.idprovider instanceof ThrowExceptionIdProvider)) {

              //ignore variables with invalid id provider
              try {
                if (varId.equals(aVarId.idprovider.getId())) {
                  return aVarId;
                }
              } catch (Exception e) {
                continue;
              }
            }
          }
        }
      }
    }
    return null;
  }

}
