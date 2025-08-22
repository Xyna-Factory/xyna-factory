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
import java.util.Optional;

import xmcp.zeta.storage.generic.filter.lexer.AdaptedOperator;
import xmcp.zeta.storage.generic.filter.lexer.LexedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.LexedToken;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.OperatorMatch;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class LiteralOperatorAdapter {

  public List<LexedToken> execute(List<LexedToken> list) {
    List<LexedToken> tokens = list;
    Replacer<LexedToken> replacer = new Replacer<LexedToken>();
    int pos = 0;
    while (true) {
      Optional<OperatorMatch> match = getFirstMatch(tokens, pos);
      if (match.isEmpty()) { break; }
      LexedLiteral lit = (LexedLiteral) tokens.get(pos);
      AdaptedOperator op = new AdaptedOperator(lit.getOriginalInput(), match.get().category);
      tokens = replacer.replaceInList(tokens, pos, pos + 1, op);
      pos = match.get().index + 1;
    }
    return tokens;
  }
  
  
  private Optional<OperatorMatch> getFirstMatch(List<LexedToken> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      LexedToken token = list.get(i);
      if (!(token instanceof LexedLiteral)) { continue; }
      LexedLiteral lit = (LexedLiteral) token;
      String val = lit.getOriginalInput().trim();
      
      OperatorMatch match = new OperatorMatch();
      match.index = i;
      if (val.length() == 3) {
        if (val.toUpperCase().matches("AND")) {
          match.category = Enums.LexedOperatorCategory.AND;
          return Optional.of(match);
        }
      }
      if (val.length() == 2) {
        if (val.toUpperCase().matches("OR")) {
          match.category = Enums.LexedOperatorCategory.OR;
          return Optional.of(match);
        }
      }
    }
    return Optional.empty();
  }
  
}
