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

package xmcp.zeta.storage.generic.filter.parser;

import java.util.List;

import xmcp.zeta.storage.generic.filter.lexer.LexedToken;
import xmcp.zeta.storage.generic.filter.lexer.MergedOperator;
import xmcp.zeta.storage.generic.filter.lexer.OperatorToken;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class DoubleOperatorAdapter {

  public List<LexedToken> execute(List<LexedToken> list) {
    List<LexedToken> tokens = list;
    Replacer<LexedToken> replacer = new Replacer<LexedToken>();
    int pos = 0;
    while (true) {
      pos = getIndexFirstMatch(tokens, pos);
      if (pos < 0) { break; }
      MergedOperator merged = new MergedOperator(tokens.get(pos), tokens.get(pos + 1));
      tokens = replacer.replaceInList(tokens, pos, pos + 2, merged);
      pos++;
    }
    return tokens;
  }
  
  
  private int getIndexFirstMatch(List<LexedToken> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      LexedToken token = list.get(i);
      if (!(token instanceof OperatorToken)) { continue; }
      OperatorToken op = (OperatorToken) token;
      if ((op.getCategory() != Enums.LexedOperatorCategory.AND) &&
          (op.getCategory() != Enums.LexedOperatorCategory.OR)) {
        continue;
      }
      LexedToken next = list.get(i + 1);
      if (!(next instanceof OperatorToken)) { continue; }
      OperatorToken nextop = (OperatorToken) next;
      if (nextop.getCategory() != op.getCategory()) { continue; }
      return i;
    }
    return -1;
  }

}
