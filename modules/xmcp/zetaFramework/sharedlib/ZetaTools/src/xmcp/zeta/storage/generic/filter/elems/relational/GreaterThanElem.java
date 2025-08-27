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

package xmcp.zeta.storage.generic.filter.elems.relational;

import xmcp.zeta.storage.generic.filter.elems.RelationalOperand;
import xmcp.zeta.storage.generic.filter.elems.UnaryRelationalOpElem;


public class GreaterThanElem extends UnaryRelationalOpElem {

  public GreaterThanElem(RelationalOperand elem) {
    super(elem);
  }

  @Override
  public String getOperatorName() {
    return "GreaterThan";
  }
  
  @Override
  public void writeSql(String colname, StringBuilder str) {
    RelationalOperand operand = getOperand();
    String content = operand.getContentString();
    if (operand.containsWildcards()) {
      throw new IllegalArgumentException("Syntax error in filter expression: > operator cannot be combined with wildcards");
    }
    if (!operand.isNumerical()) {
      throw new IllegalArgumentException("Syntax error in filter expression: > operator can only combined with numerical values");
    }
    str.append(colname).append(" > ").append(content);
  }
  
}
