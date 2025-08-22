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

import xmcp.zeta.storage.generic.filter.lexer.LexedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.LexedToken;
import xmcp.zeta.storage.generic.filter.lexer.MergedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.OperatorToken;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class QuoteHandler {

  public static class QuoteMatch {
    public Enums.LexedOperatorCategory category;
    public int index;
  }
  
  
  public List<LexedToken> execute(List<LexedToken> list) {
    List<LexedToken> tokens = list;
    Replacer<LexedToken> replacer = new Replacer<LexedToken>();
    int pos = 0;
    while (true) {
      Optional<QuoteMatch> match = getIndexFirstQuote(tokens, pos);
      if (match.isEmpty()) { break; }
      int endPos = getIndexClosingQuote(list, match.get());
      MergedLiteral literal = mergeQuotedTokens(match.get().index + 1, endPos, tokens);
      tokens = replacer.replaceInList(tokens, match.get().index, endPos + 1, literal);
      pos = match.get().index + 1;
    }
    return tokens;
  }
  
  
  private MergedLiteral mergeQuotedTokens(int fromInclusive, int toExclusive, List<LexedToken> list) {
    List<LexedToken> toMerge = list.subList(fromInclusive + 1, toExclusive);
    StringBuilder str = new StringBuilder();
    for (LexedToken token : toMerge) {
      str.append(token.getOriginalInput());
    }
    return new MergedLiteral(str.toString());
  }
  
  
  private Optional<QuoteMatch> getIndexFirstQuote(List<LexedToken> list, int from) {
    for (int i = from; i < list.size(); i++) {
      LexedToken token = list.get(i);
      if (!(token instanceof OperatorToken)) { continue; }
      OperatorToken op = (OperatorToken) token;
      if ((op.getCategory() == Enums.LexedOperatorCategory.SINGLE_QUOTE) ||
          (op.getCategory() == Enums.LexedOperatorCategory.DOUBLE_QUOTE)) {
        QuoteMatch ret = new QuoteMatch();
        ret.category = op.getCategory();
        ret.index = i;
        return Optional.of(ret);
      }
    }
    return Optional.empty();
  }
  
  
  private int getIndexClosingQuote(List<LexedToken> list, QuoteMatch firstMatch) {    
    for (int i = firstMatch.index + 1; i < list.size(); i++) {
      LexedToken token = list.get(i);
      if (!(token instanceof OperatorToken)) { continue; }
      OperatorToken op = (OperatorToken) token;
      if (op.getCategory() == firstMatch.category) {
        return i;
      }
    }
    throw new IllegalArgumentException("Error parsing filter expression: Quotes are not closed");
  }
  
}
