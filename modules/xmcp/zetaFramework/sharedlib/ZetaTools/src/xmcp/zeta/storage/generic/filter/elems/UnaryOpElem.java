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


public abstract class UnaryOpElem implements FilterElement, LogicalOperand {

  private FilterElement operand;
  
  
  public UnaryOpElem(FilterElement operand) {
    this.operand = operand;
  }
  
  
  public abstract String getOperatorName();
  
  protected abstract FilterElement buildReplacementOperand(ContainerElem container);
  
  
  public boolean isFinished() {
    return operand.isFinished();
  }
  
  
  public void parse(FilterInputParser parser) {
    if (!operand.isFinished()) {
      operand.parse(parser);
    }
    if (operand instanceof ContainerElem) {
      operand = buildReplacementOperand((ContainerElem) operand);
    }
  }
  
  
  public void writeJson(JsonWriter json) {
    json.openObjectAttribute("UnaryOp");
    json.addAttribute("operator", getOperatorName());
    json.continueObject();
    json.openObjectAttribute("operand");
    operand.writeJson(json);
    json.closeObject();
    json.closeObject();
  }
  
}
