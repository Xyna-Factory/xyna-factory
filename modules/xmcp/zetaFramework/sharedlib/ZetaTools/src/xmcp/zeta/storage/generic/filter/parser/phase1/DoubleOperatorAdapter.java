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

package xmcp.zeta.storage.generic.filter.parser.phase1;

import java.util.List;

import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.lexer.AdaptedOperator;
import xmcp.zeta.storage.generic.filter.lexer.LexedOperator;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class DoubleOperatorAdapter {

  public List<Token> execute(List<Token> list) {
    List<Token> tokens = list;
    Replacer<Token> replacer = new Replacer<Token>();
    int pos = 0;
    while (true) {
      pos = getIndexFirstMatch(tokens, pos);
      if (pos < 0) { break; }
      AdaptedOperator merged = new AdaptedOperator(tokens.get(pos), tokens.get(pos + 1));
      tokens = replacer.replaceInList(tokens, pos, pos + 2, merged);
      pos++;
    }
    return tokens;
  }
  
  
  private int getIndexFirstMatch(List<Token> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      Token token = list.get(i);
      if (!(token instanceof LexedOperator)) { continue; }
      LexedOperator op = (LexedOperator) token;
      if ((op.getCategory() != Enums.LexedOperatorCategory.AND) &&
          (op.getCategory() != Enums.LexedOperatorCategory.OR)) {
        continue;
      }
      Token next = list.get(i + 1);
      if (!(next instanceof LexedOperator)) { continue; }
      LexedOperator nextop = (LexedOperator) next;
      if (nextop.getCategory() != op.getCategory()) { continue; }
      return i;
    }
    return -1;
  }

}
