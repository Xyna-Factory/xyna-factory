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

package xmcp.zeta.storage.generic.filter.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;


public class FilterInputLexer {

  
  
  // attribut: liste lexed-token
  //private List<LexedToken> _tokens = new ArrayList<>();
  
  // constr. (string)
  // -> call tokenize
  
  // tokenize
  // -> call java tokenizer : liste strings
  // pro string: get (optional) op-type (enum ), sonst literal (oder whitespace?)
  // output (erbt von lexedtoken): literal, whitespace, op-token
  public List<Token> execute(String input) {
    List<Token> ret = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(input, " &!|<>()'\"\t\n", true);
    while (st.hasMoreTokens()) {
      Optional<Token> opt = buildToken(st.nextToken());
      if (opt.isPresent()) {
        ret.add(opt.get());
      }
    }
    return ret;
  }
  
  
  private Optional<Token> buildToken(String input) {
    if (input == null) { return Optional.empty(); }
    Optional<Token> opt = Whitespace.buildIfMatches(input);
    if (opt.isPresent()) { return opt; }
    opt = LexedOperator.buildIfMatches(input);
    if (opt.isPresent()) { return opt; }
    return Optional.of(new LexedLiteral(input));
  }

}
