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

import java.util.Optional;

import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.JsonWriter;


public abstract class UnaryOpElem<T extends FilterElement> implements FilterElement, LogicalOperand {

  private T _operand;
  
  
  public UnaryOpElem(T operand) {
    _operand = operand;
  }
  
  public abstract String getOperatorName();
  
  protected abstract T buildReplacementOperand(ContainerElem container);
  
  
  public T getOperand() {
    return _operand;
  }
  
  
  public boolean isFinished() {
    if (_operand instanceof ContainerElem) { return false; }
    return _operand.isFinished();
  }
  
  
  public void parse(FilterInputParser parser) {
    if (!_operand.isFinished()) {
      _operand.parse(parser);
    }
    if (_operand instanceof ContainerElem) {
      _operand = buildReplacementOperand((ContainerElem) _operand);
    }
  }
  
  
  public void writeJson(JsonWriter json) {
    json.openObjectAttribute("UnaryOp");
    json.addAttribute("operator", getOperatorName());
    json.continueObject();
    json.openObjectAttribute("operand");
    _operand.writeJson(json);
    json.closeObject();
    json.closeObject();
  }
  
  
  @Override
  public Optional<FilterElement> getChild(int index) {
    if (index == 0) {
      return Optional.ofNullable(_operand);
    }
    return Optional.empty();
  }
  
  @Override
  public String getInfoString() {
    return getOperatorName();
  }
  
}
