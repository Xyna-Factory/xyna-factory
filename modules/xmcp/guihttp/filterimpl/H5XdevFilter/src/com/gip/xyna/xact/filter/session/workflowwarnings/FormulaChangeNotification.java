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

package com.gip.xyna.xact.filter.session.workflowwarnings;

import java.util.List;

import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.processmodeller.datatypes.Warning;

public class FormulaChangeNotification implements WarningsChangeNotification {
  
  private String newFormula;
  private int inputVarCount;


  public FormulaChangeNotification(String newFormula, StepMapping stepMapping) {
    this.newFormula = newFormula;
    this.inputVarCount = stepMapping.getInputVarIds().length;
  }


  @Override
  public void handle(ObjectId objectId, WorkflowWarningsHandler handler) {
    handler.deleteAllWarnings(objectId.getObjectId());
    
    List<XFLLexem> formulaTerms = XFLLexer.lex(newFormula, true);
    if (!isAssignment(formulaTerms)) {
      return;
    }

    boolean parsingLeftSide = true;
    for (XFLLexem formulaTerm : formulaTerms) {
      if (formulaTerm.getType() == TokenType.VARIABLE) {
        int varIdx = Integer.parseInt(formulaTerm.getToken().replace("%", ""));
        if (parsingLeftSide && varIdx < inputVarCount) {
          ObjectId warningId = ObjectId.createWarningId(WorkflowWarningsHandler.warningIdx++);
          handler.addWarning(new Warning(objectId.getObjectId(), warningId.getObjectId(), WorkflowWarningMessageCode.ASSIGNMENT_TO_SOURCE));
        } else if (!parsingLeftSide && varIdx >= inputVarCount) {
          ObjectId warningId = ObjectId.createWarningId(WorkflowWarningsHandler.warningIdx++);
          handler.addWarning(new Warning(objectId.getObjectId(), warningId.getObjectId(), WorkflowWarningMessageCode.ASSIGNMENT_FROM_TARGET));
        }
      } else if (formulaTerm.getType() == TokenType.ASSIGNMENT) {
        parsingLeftSide = false;
      }
    }
  }

  private boolean isAssignment(List<XFLLexem> formulaTerms) {
    int assignOperatorCount = 0;
    for (XFLLexem formulaTerm : formulaTerms) {
      if (formulaTerm.getType() == TokenType.ASSIGNMENT) {
        assignOperatorCount++;
      }
    }

    return assignOperatorCount == 1;
  }
}
