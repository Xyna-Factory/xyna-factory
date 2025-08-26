/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.zeta.storage.generic.filter.elems;

import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.JsonWriter;


public abstract class BinaryLogicalOpElem implements LogicalOperator {

  private LogicalOperand _operand1;
  private LogicalOperand _operand2;
  
  
  public BinaryLogicalOpElem(LogicalOperand elem1, LogicalOperand elem2) {
    this._operand1 = elem1;
    this._operand2 = elem2;
  }
  
  
  public abstract String getOperatorName();
  
  
  public LogicalOperand getOperand1() {
    return _operand1;
  }
  
  
  public LogicalOperand getOperand2() {
    return _operand2;
  }
  
  
  public boolean isFinished() {
    return _operand1.isFinished() && _operand2.isFinished();
  }
  
  
  public void parse(FilterInputParser parser) {
    if (!_operand1.isFinished()) {
      _operand1.parse(parser);
    }
    if (!_operand2.isFinished()) {
      _operand1.parse(parser);
    }
    if (_operand1 instanceof ContainerElem) {
      _operand1 = handleContainerOperand((ContainerElem) _operand1);
    }
    if (_operand2 instanceof ContainerElem) {
      _operand2 = handleContainerOperand((ContainerElem) _operand2);
    }
  }
  
  
  private LogicalOperand handleContainerOperand(ContainerElem container) {
    FilterElement elem = container.verifyAndExtractSingleChild();
    if (elem instanceof LogicalOperand) {
      return (LogicalOperand) elem;
    }
    throw new RuntimeException("Error parsing filter expression: Unexpected operand for logical operator");
  }
  
  
  public void writeJson(JsonWriter json) {
    json.openObjectAttribute("UnaryOp");
    json.addAttribute("operator", getOperatorName());
    json.continueObject();
    json.openObjectAttribute("operand-1");
    _operand1.writeJson(json);
    json.closeObject();
    json.continueObject();
    json.openObjectAttribute("operand-2");
    _operand2.writeJson(json);
    json.closeObject();
    json.closeObject();
  }
  
}
