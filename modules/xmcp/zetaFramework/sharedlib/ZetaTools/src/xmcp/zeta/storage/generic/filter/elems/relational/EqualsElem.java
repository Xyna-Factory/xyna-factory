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

import com.gip.xyna.xnwh.selection.parsing.SelectionParser;

import xmcp.zeta.storage.generic.filter.elems.RelationalOperandElem;
import xmcp.zeta.storage.generic.filter.elems.UnaryRelationalOpElem;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class EqualsElem extends UnaryRelationalOpElem {

  public EqualsElem(RelationalOperandElem elem) {
    super(elem);
  }

  @Override
  public String getOperatorName() {
    return "Equals";
  }
  
  @Override
  public void writeSql(String colname, SqlWhereClauseData sql) {
    RelationalOperandElem operand = getOperand();
    String content = "";
    sql.appendToSqlEscaped(colname);
    boolean useLike = operand.containsWildcards();
    useLike = useLike || operand.indicateAddWildcardAddStart();
    useLike = useLike || operand.indicateAddWildcardAddEnd();
    if (useLike) {
      sql.appendToSql(" LIKE ?");
      content = operand.getContentAdaptedForSqlLike();
    } else {
      sql.appendToSql(" = ?");
      content = operand.getContentAdaptedForSqlEquals();
    }
    if (operand.indicateAddWildcardAddStart()) {
       content = SelectionParser.CHARACTER_WILDCARD + content;
    }
    if (operand.indicateAddWildcardAddEnd()) {
      content = content + SelectionParser.CHARACTER_WILDCARD;
    }
    sql.addQueryParameter(content);
  }
  
}
