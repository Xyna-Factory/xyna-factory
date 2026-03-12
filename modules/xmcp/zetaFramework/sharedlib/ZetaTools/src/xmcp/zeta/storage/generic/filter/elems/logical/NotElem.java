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

import xmcp.zeta.storage.generic.filter.elems.ContainerElem;
import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperandElem;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperatorElem;
import xmcp.zeta.storage.generic.filter.elems.UnaryOpElem;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class NotElem extends UnaryOpElem<LogicalOperandElem> implements LogicalOperatorElem {

  
  public NotElem(LogicalOperandElem operand) {
    super(operand);
  }

  
  @Override
  public String getOperatorName() {
    return "NOT";
  }

  
  @Override
  protected LogicalOperandElem buildReplacementOperand(ContainerElem container) {
    FilterElement elem = container.verifyAndExtractSingleChild();
    if (elem instanceof LogicalOperandElem) {
      return (LogicalOperandElem) elem;
    }
    throw new RuntimeException("Error parsing filter expression: Unexpected operand for logical operator");
  }

  
  @Override
  public void writeSql(String colname, SqlWhereClauseData sql) {
    LogicalOperandElem operand = getOperand();
    sql.appendToSql("NOT (");
    operand.writeSql(colname, sql);
    sql.appendToSql(")");
  }
  
}
