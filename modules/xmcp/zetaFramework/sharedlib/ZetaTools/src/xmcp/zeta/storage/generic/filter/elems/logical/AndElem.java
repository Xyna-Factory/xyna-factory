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
import xmcp.zeta.storage.generic.filter.elems.LogicalOperandElem;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperatorElem;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class AndElem extends BinaryLogicalOpElem implements LogicalOperatorElem {

  public AndElem(LogicalOperandElem elem1, LogicalOperandElem elem2) {
    super(elem1, elem2);
  }

  @Override
  public String getOperatorName() {
    return "AND";
  }

  @Override
  public void writeSql(String colname, SqlWhereClauseData sql) {
    sql.appendToSql("(");
    getOperand1().writeSql(colname, sql);
    sql.appendToSql(") AND (");
    getOperand2().writeSql(colname, sql);
    sql.appendToSql(")");
  }
  
}
