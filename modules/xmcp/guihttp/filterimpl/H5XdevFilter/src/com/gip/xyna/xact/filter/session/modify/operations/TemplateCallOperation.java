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

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.xmom.workflows.json.TemplateCallJson;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

public class TemplateCallOperation extends ModifyOperationBase<TemplateCallJson> {
  
  private TemplateCallJson templateCallJson;
  
  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    templateCallJson = jp.parse(jsonRequest, TemplateCallJson.getJsonVisitor());
    
    return templateCallJson.getRevision();
  }
  
  private void insertTemplate(Operation operation) throws UnsupportedOperationException {
    if(operation instanceof JavaOperation) {
      JavaOperation op = (JavaOperation)operation;
      StringBuilder impl = new StringBuilder();
      
      if(op.getOutputVars() != null && !op.getOutputVars().isEmpty()) {
        impl.append("return ");
      }
      
      if(op.isStatic()) {
        impl.append(op.getParent().getImplFqClassName()).append(".");
      } else {
        impl.append("getImplementationOfInstanceMethods().");
      }
      impl.append(op.getName()).append("(");
      
      if(op.requiresXynaOrder()) {
        if(op.getInputVars() != null && !op.getInputVars().isEmpty()) {
          impl.append("correlatedXynaOrder, ");
        } else {
          impl.append("correlatedXynaOrder");
        }
      }
      
      if(op.getInputVars() != null && !op.getInputVars().isEmpty()) {
        int i = 0;
        for (AVariable var : op.getInputVars()) {
          impl.append(var.getVarName());
          if(i < op.getInputVars().size() - 1) {
            impl.append(", ");
          }
          i++;
        }
      }
      impl.append(");");
      
      op.setImpl(impl.toString());
      
    } else {
      throw new UnsupportedOperationException("insertTemplate", "Insert Template is only allowed for Coded Services.");
    }
  }
  
  @Override
  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo) throws UnsupportedOperationException {
    insertTemplate(object.getOperation());
  } 
  
}
