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

import xmcp.zeta.storage.generic.filter.lexer.OperatorToken;
import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class TokenOpElem implements FilterElement {

  private final OperatorToken token;

  public TokenOpElem(OperatorToken token) {
    this.token = token;
  }
  
  public Enums.LexedOperatorCategory getCategory() {
    return token.getCategory();
  }
  
  @Override
  public boolean isFinished() {
    return false;
  }
  
  @Override
  public void parse(FilterInputParser parser) {
    // do nothing
  }
  
  
  @Override
  public void writeSql(String colname, SqlWhereClauseData sql) {
    throw new RuntimeException("SQL output not supported for class " + this.getClass().getName());
  }
  
  @Override
  public Optional<FilterElement> getChild(int index) {
    return Optional.empty();
  }
  
  @Override
  public String getInfoString() {
    return "TOKEN-" + getCategory().toString();
  }
  
}
