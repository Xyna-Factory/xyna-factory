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

import xmcp.zeta.storage.generic.filter.lexer.AdaptedOperator;
import xmcp.zeta.storage.generic.filter.lexer.LexedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.LexedToken;
import xmcp.zeta.storage.generic.filter.lexer.MergedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.OperatorToken;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class LiteralMerger {

  
  
  public List<LexedToken> execute(List<LexedToken> list) {
    List<LexedToken> tokens = list;
    Replacer<LexedToken> replacer = new Replacer<LexedToken>();
    int pos = 0;
    while (true) {
      int from = getIndexFirstMatchStart(tokens, pos);
      if (from < 0) { break; }
      int to = getIndexFirstMatchEnd(tokens, from + 1);
      MergedLiteral merged = mergeLiteralTokens(from, to + 1, tokens);
      tokens = replacer.replaceInList(tokens, from, to + 1, merged);
      pos = from + 1;
    }
    return tokens;
  }
  
  
  private MergedLiteral mergeLiteralTokens(int fromInclusive, int toExclusive, List<LexedToken> list) {
    List<LexedToken> toMerge = list.subList(fromInclusive, toExclusive);
    StringBuilder str = new StringBuilder();
    for (LexedToken token : toMerge) {
      str.append(token.getOriginalInput());
    }
    return new MergedLiteral(str.toString());
  }
  
  
  private int getIndexFirstMatchStart(List<LexedToken> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      LexedToken token = list.get(i);
      boolean matched = false;
      if (token instanceof LexedLiteral) {
        matched = true;
      } else if (token instanceof MergedLiteral) {
        matched = true;
      }
      if (!matched) { continue; }
      token = list.get(i + 1);
      if (token instanceof LexedLiteral) {
        matched = true;
      } else if (token instanceof MergedLiteral) {
        matched = true;
      }
      if (matched) { 
        return i;
      }
    }
    return -1;
  }
  
  
  private int getIndexFirstMatchEnd(List<LexedToken> list, int from) {
    int matchEnd = from;
    for (int i = from + 1; i < list.size() - 1; i++) {
      LexedToken token = list.get(i);
      boolean matched = false;
      if (token instanceof LexedLiteral) {
        matched = true;
      } else if (token instanceof MergedLiteral) {
        matched = true;
      }
      if (!matched) { return matchEnd; }
      matchEnd = i;
    }
    return matchEnd;
  }
  
}
