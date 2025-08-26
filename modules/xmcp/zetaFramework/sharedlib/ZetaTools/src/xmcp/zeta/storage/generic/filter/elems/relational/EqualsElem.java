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
import xmcp.zeta.storage.generic.filter.shared.FilterInputConstants;


public class EqualsElem extends UnaryRelationalOpElem {

  public EqualsElem(RelationalOperand elem) {
    super(elem);
  }

  @Override
  public String getOperatorName() {
    return "Equals";
  }
  
  @Override
  public void writeSql(String colname, StringBuilder str) {
    RelationalOperand operand = getOperand();
    String content = operand.getContentString();
    boolean hasWildcards = operand.containsWildcards();
    str.append(colname).append(" LIKE '");
    if (!hasWildcards) {
      str.append(FilterInputConstants.SQL_WILDCARD);
    }
    str.append(content);
    if (!hasWildcards) {
      str.append(FilterInputConstants.SQL_WILDCARD);
    }
    str.append("'");
  }
  
}
