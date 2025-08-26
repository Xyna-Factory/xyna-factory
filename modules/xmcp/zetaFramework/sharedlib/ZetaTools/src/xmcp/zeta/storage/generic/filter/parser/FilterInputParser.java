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

import xmcp.zeta.storage.generic.filter.elems.ContainerElem;
import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.lexer.FilterInputLexer;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.parser.phase1.DoubleOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.LiteralMerger;
import xmcp.zeta.storage.generic.filter.parser.phase1.LiteralOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.QuoteHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.OperatorHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.ParenthesesHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.RelOperandWrapper;
import xmcp.zeta.storage.generic.filter.parser.phase2.TokenAdapter;


public class FilterInputParser {

  private OperatorHandler _operatorHandler = new OperatorHandler();
  
  
  // keine attribute, nur tool-klasse, wird von expression-parse-methoden durchgereicht?
  
  // parse
  // input: liste lexed-token 
  // -> root-container erzeugen
  // -> handle quotes
   
  // merge operators: && || als doppelte ops identifizieren und ersetzen?
  // schon hier check ob mehr als zwei hintereinander, dann fehler?
  
  // ? replace container
     
     
  // handle quotes (input liste lexed-token)
  // find first quote (single / double), find next (selber typ), dann replace-quotes
  
  // replace-quotes (liste lex-token, int startpos, int endpos)
  // output: neue liste lex-token (sublisten vorher u. nachher, quote-bereich durch neues literal ersetzt)
  // mit generisch replace() von container?
  
  
  public FilterElement parse(String input) {
    List<Token> tokens = new FilterInputLexer().execute(input);
    tokens = executePhase1(tokens);
    return executePhase2(tokens);
  }
  
  
  private List<Token> executePhase1(List<Token> list) {
    List<Token> tokens = list;
    tokens = new QuoteHandler().execute(tokens);
    tokens = new DoubleOperatorAdapter().execute(tokens);
    tokens = new LiteralOperatorAdapter().execute(tokens);
    tokens = new LiteralMerger().execute(tokens);
    return tokens;
  }
  
  
  private FilterElement executePhase2(List<Token> tokens) {
    List<FilterElement> elems = new TokenAdapter().execute(tokens);
    elems = new ParenthesesHandler().execute(elems);
    elems = new RelOperandWrapper().execute(elems);
    FilterElement root;
    if (elems.size() == 1) {
      root = elems.get(0);
    } else {
      root = new ContainerElem(elems);
    }
    root.parse(this);
    if (root instanceof ContainerElem) {
      root = ((ContainerElem) root).verifyAndExtractSingleChild();
    }
    return root;
  }
  
  
  public List<FilterElement> parseOperators(List<FilterElement> input) {
    return _operatorHandler.execute(input);
  }
  
}
