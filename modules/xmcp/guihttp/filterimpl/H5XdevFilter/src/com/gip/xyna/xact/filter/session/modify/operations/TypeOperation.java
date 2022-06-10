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
package com.gip.xyna.xact.filter.session.modify.operations;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.exceptions.MissingObjectException;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariables;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xact.filter.xmom.workflows.json.TypeJson;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;

public class TypeOperation extends ModifyOperationBase<TypeJson> {

  private TypeJson type;

  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    type = jp.parse(jsonRequest, TypeJson.getJsonVisitor());
    
    return type.getRevision();
  }

  @Override
  protected void modifyStep(Step step)
      throws XynaException, UnknownObjectIdException, MissingObjectException {
    if( ! (step instanceof StepFunction) ) {
      throw new IllegalStateException("Wrong step to modify type");
    }
    
    StepFunction stepFunction = (StepFunction)step;
    IdentifiedVariables identifiedVariables = object.getIdentifiedVariables();
    
    if( type.getFQName() == null ) {
      //Konversion zum Prototyp
      stepFunction.convertToPrototype();
      //alte Input-Outputs erhalten und daher wieder eintragen
      List<AVariable> inputVars = stepFunction.getService().getInputVars();
      for( AVariableIdentification var: identifiedVariables.getVariables(VarUsageType.input) ) {
        inputVars.add( var.getIdentifiedVariable() );
      }
      List<AVariable> outputVars = stepFunction.getService().getOutputVars();
      for( AVariableIdentification var: identifiedVariables.getVariables(VarUsageType.output) ) {
        outputVars.add( var.getIdentifiedVariable() );
      }
      
    } else {
      GenerationBaseObject gbo = modification.load(type.getFQName());
      if( gbo.getType() == XMOMType.WORKFLOW ) {
        
        
        // TODO!
//        stepFunction.replaceCall( gbo.getWorkflow(), true, gbo.getWorkflow().getInputVars(), gbo.getWorkflow().getOutputVars() ); // TODO: Variablen vorher klonen, so wie in InsertOperation?
        
        
        
      } else if( gbo.getType() == XMOMType.DATATYPE ) {
        //Service
        //FIXME Instanzmethoden?
//        DOM service = gbo.getDOM();
        
        
        
        // TODO!
//        stepFunction.replaceService( service, type.getFQName().getOperation(), true );
        
        
        
      } else {
        throw new IllegalStateException("Wrong object to use for service");
      }
      identifiedVariables.identify(); //Neuermitteln der Inputs 
      //FIXME besser durch Wiedererkennung von AVariableIdentificationS? 
    }
  }

  @Override
  protected void modifyVariable(Variable variable)
      throws XynaException, UnknownObjectIdException, MissingObjectException {
    
    AVariable var = variable.getVariable().getIdentifiedVariable();
  
    if( type.getFQName() == null ) {
      //Konversion zum Prototyp
      var.replaceDOM(null, "Parameter");
    } else {
      GenerationBaseObject gbo = modification.load(type.getFQName());
      if( gbo.getType() == XMOMType.DATATYPE ) {
        var.replaceDOM(gbo.getDOM(), gbo.getDOM().getLabel());
      } else {
        throw new IllegalStateException("Wrong object to use for datatype");
      }
    }
  }

}
