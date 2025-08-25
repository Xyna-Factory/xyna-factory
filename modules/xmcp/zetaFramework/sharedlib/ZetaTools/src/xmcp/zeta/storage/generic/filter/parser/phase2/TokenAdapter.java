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

package xmcp.zeta.storage.generic.filter.parser.phase2;

import java.util.ArrayList;
import java.util.List;

import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.LiteralElem;
import xmcp.zeta.storage.generic.filter.elems.TokenOpElem;
import xmcp.zeta.storage.generic.filter.lexer.LexedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.MergedLiteral;
import xmcp.zeta.storage.generic.filter.lexer.OperatorToken;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.lexer.Whitespace;


public class TokenAdapter {

  public List<FilterElement> execute(List<Token> input) {
    List<FilterElement> ret = new ArrayList<>();
    for (Token token : input) {
      if (token instanceof Whitespace) { continue; }
      else if (token instanceof LexedLiteral) {
        ret.add(new LiteralElem(token.getOriginalInput()));
      } else if (token instanceof MergedLiteral) {
        ret.add(new LiteralElem(token.getOriginalInput()));
      } else if (token instanceof OperatorToken) {
        ret.add(new TokenOpElem((OperatorToken) token));
      } else {
        throw new IllegalArgumentException("Unexpected token class: " + token.getClass().getName());
      }
    }
    return ret;
  }
  
}
