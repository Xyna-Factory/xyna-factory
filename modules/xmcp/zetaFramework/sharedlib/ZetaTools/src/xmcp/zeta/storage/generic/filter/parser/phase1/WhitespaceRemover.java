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

import java.util.ArrayList;
import java.util.List;

import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.lexer.Whitespace;


public class WhitespaceRemover {

  public List<Token> execute(List<Token> list) {
    List<Token> ret = new ArrayList<>();
    for (Token token : list) {
      if (token instanceof Whitespace) { continue; }
      ret.add(token);
    }
    return ret;
  }
  
}
