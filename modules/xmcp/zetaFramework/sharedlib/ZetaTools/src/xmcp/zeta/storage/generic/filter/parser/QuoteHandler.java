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

import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.lexer.MergedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.LexedOperator;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.OperatorMatch;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class QuoteHandler {

  public List<Token> execute(List<Token> list) {
    List<Token> tokens = list;
    Replacer<Token> replacer = new Replacer<Token>();
    int pos = 0;
    while (true) {
      Optional<OperatorMatch> match = getFirstQuote(tokens, pos);
      if (match.isEmpty()) { break; }
      int endPos = getIndexClosingQuote(list, match.get());
      MergedLiteral literal = mergeQuotedTokens(match.get().index + 1, endPos, tokens);
      tokens = replacer.replaceInList(tokens, match.get().index, endPos + 1, literal);
      pos = match.get().index + 1;
    }
    return tokens;
  }
  
  
  private MergedLiteral mergeQuotedTokens(int fromInclusive, int toExclusive, List<Token> list) {
    List<Token> toMerge = list.subList(fromInclusive, toExclusive);
    StringBuilder str = new StringBuilder();
    for (Token token : toMerge) {
      str.append(token.getOriginalInput());
    }
    return new MergedLiteral(str.toString());
  }
  
  
  private Optional<OperatorMatch> getFirstQuote(List<Token> list, int from) {
    for (int i = from; i < list.size(); i++) {
      Token token = list.get(i);
      if (!(token instanceof LexedOperator)) { continue; }
      LexedOperator op = (LexedOperator) token;
      if ((op.getCategory() == Enums.LexedOperatorCategory.SINGLE_QUOTE) ||
          (op.getCategory() == Enums.LexedOperatorCategory.DOUBLE_QUOTE)) {
        OperatorMatch ret = new OperatorMatch();
        ret.category = op.getCategory();
        ret.index = i;
        return Optional.of(ret);
      }
    }
    return Optional.empty();
  }
  
  
  private int getIndexClosingQuote(List<Token> list, OperatorMatch firstMatch) {    
    for (int i = firstMatch.index + 1; i < list.size(); i++) {
      Token token = list.get(i);
      if (!(token instanceof LexedOperator)) { continue; }
      LexedOperator op = (LexedOperator) token;
      if (op.getCategory() == firstMatch.category) {
        return i;
      }
    }
    throw new IllegalArgumentException("Error parsing filter expression: Quotes are not closed");
  }
  
}
