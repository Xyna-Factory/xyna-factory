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
package com.gip.xyna.xact.filter.session.modify.operations;

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xact.filter.session.Dataflow;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.Variable;
import com.gip.xyna.xact.filter.session.gb.vars.IdentifiedVariablesStepThrow;
import com.gip.xyna.xact.filter.util.AVariableIdentification;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.json.ConstantJson;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;

public class ChangeConstantOperation extends ModifyOperationBase<ConstantJson> {
  
  private ConstantJson constantJson;
  
  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    constantJson = jp.parse(jsonRequest, ConstantJson.getJsonVisitor());
    
    return constantJson.getRevision();
  }


  @Override
  protected void modifyVariable(Variable variable) throws UnsupportedOperationException {
    Dataflow df = modification.getObject().getDataflow();
    String id = null;
    try {
      Long revision = Utils.getRtcRevision(modification.getObject().getRuntimeContext());
      AVariableIdentification varIdent = object.getVariable().getVariable();
      if (!H5XdevFilter.AVARCONSTANTS.get()) {
        GeneralXynaObject gxo = Utils.convertJsonToGeneralXynaObject(constantJson.getConstant(), revision);
        id = df.setConstantValue(varIdent, constantJson.getBranchId(), gxo);
      } else {
        AVariable constantVar = Utils.convertJsonToAVariable(constantJson.getConstant(), revision);
        id = df.setConstantValue(varIdent, constantJson.getBranchId(), constantVar, constantJson.getConstant());
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new UnsupportedOperationException("modifyVariable", "RuntimeContext not found");
    }
    
    
    //if variable is in a StepThrow, we also have to set the ExceptionId
    if(variable != null && variable.getIdentifiedVariables() instanceof IdentifiedVariablesStepThrow) {
      IdentifiedVariablesStepThrow ivst = ((IdentifiedVariablesStepThrow)variable.getIdentifiedVariables());
      ivst.SetExceptionVariableId(id);
    }
  }
  
}
