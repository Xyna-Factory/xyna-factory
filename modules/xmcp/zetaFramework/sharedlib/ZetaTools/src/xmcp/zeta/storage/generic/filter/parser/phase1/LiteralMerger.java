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

import xmcp.zeta.storage.generic.filter.lexer.LexedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.lexer.Whitespace;
import xmcp.zeta.storage.generic.filter.lexer.MergedLiteral;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class LiteralMerger {

  public List<Token> execute(List<Token> list) {
    List<Token> tokens = list;
    Replacer<Token> replacer = new Replacer<Token>();
    int pos = 0;
    while (true) {
      int from = getIndexNextMatchStart(tokens, pos);
      if (from < 0) { break; }
      pos = from + 1;
      int to = getIndexMatchEnd(tokens, from + 1);
      if (to <= from) { continue; }
      MergedLiteral merged = mergeLiteralTokens(from, to + 1, tokens);
      tokens = replacer.replaceInList(tokens, from, to + 1, merged);
    }
    return tokens;
  }
  
  
  private MergedLiteral mergeLiteralTokens(int fromInclusive, int toExclusive, List<Token> list) {
    List<Token> toMerge = list.subList(fromInclusive, toExclusive);
    StringBuilder str = new StringBuilder();
    for (Token token : toMerge) {
      str.append(token.getOriginalInput());
    }
    return new MergedLiteral(str.toString());
  }
  
  
  private int getIndexNextMatchStart(List<Token> list, int from) {
    for (int i = from; i < list.size() - 1; i++) {
      Token token = list.get(i);
      if (token instanceof LexedLiteral) {
        return i;
      } else if (token instanceof MergedLiteral) {
        return i;
      }
    }
    return -1;
  }
  
  
  /*
   * whitespace is included in merged string only if positioned directly between literals
   */
  private int getIndexMatchEnd(List<Token> list, int from) {
    int matchEnd = from;
    for (int i = from; i < list.size(); i++) {
      Token token = list.get(i);
      if (token instanceof LexedLiteral) {
        matchEnd = i;
      } else if (token instanceof MergedLiteral) {
        matchEnd = i;
      } else if (!(token instanceof Whitespace)) {
        return matchEnd;
      }
    }
    return matchEnd;
  }
  
}
