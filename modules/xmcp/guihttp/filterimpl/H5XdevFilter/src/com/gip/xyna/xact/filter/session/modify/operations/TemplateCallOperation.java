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
package com.gip.xyna.xact.filter.session.modify.operations;

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.exceptions.UnsupportedOperationException;
import com.gip.xyna.xact.filter.session.gb.GBBaseObject.MemberMethodInfo;
import com.gip.xyna.xact.filter.xmom.workflows.json.TemplateCallJson;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;

public class TemplateCallOperation extends ModifyOperationBase<TemplateCallJson> {
  
  private TemplateCallJson templateCallJson;
  
  @Override
  public int parseRequest(String jsonRequest) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    templateCallJson = jp.parse(jsonRequest, TemplateCallJson.getJsonVisitor());
    
    return templateCallJson.getRevision();
  }
  

  private void insertTemplate(Operation operation) throws UnsupportedOperationException {
    if (operation instanceof JavaOperation) {
      JavaOperation op = (JavaOperation) operation;
      op.createImplCallSnippet(true, true);
    } else if (operation instanceof PythonOperation) {
      PythonOperation op = (PythonOperation) operation;
      op.createImplCallSnippet(true);
    } else {
      throw new UnsupportedOperationException("insertTemplate", "Insert Template is only allowed for Coded Services.");
    }
  }
  
  @Override
  protected void modifyMemberMethod(DOM dom, MemberMethodInfo memberMethodInfo) throws UnsupportedOperationException {
    insertTemplate(object.getOperation());
  } 
  
}
