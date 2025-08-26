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

package xmcp.zeta.storage.generic.filter.elems.logical;

import xmcp.zeta.storage.generic.filter.elems.BinaryLogicalOpElem;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperand;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperator;


public class OrElem extends BinaryLogicalOpElem implements LogicalOperator {

  public OrElem(LogicalOperand elem1, LogicalOperand elem2) {
    super(elem1, elem2);
  }

  @Override
  public String getOperatorName() {
    return "OR";
  }
  
  @Override
  public void writeSql(String colname, StringBuilder str) {
    str.append("(");
    getOperand1().writeSql(colname, str);
    str.append(") OR (");
    getOperand2().writeSql(colname, str);
    str.append(")");
  }
  
}
