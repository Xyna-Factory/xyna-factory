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
import xmcp.zeta.storage.generic.filter.elems.LogicalOperatorElem;
import xmcp.zeta.storage.generic.filter.elems.RelationalOperandElem;
import xmcp.zeta.storage.generic.filter.elems.UnaryRelationalOpElem;
import xmcp.zeta.storage.generic.filter.elems.relational.EqualsElem;
import xmcp.zeta.storage.generic.filter.lexer.FilterInputLexer;
import xmcp.zeta.storage.generic.filter.lexer.Token;
import xmcp.zeta.storage.generic.filter.parser.phase1.DoubleOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.LiteralOperatorAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase1.QuoteHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.OperatorHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.ParenthesesHandler;
import xmcp.zeta.storage.generic.filter.parser.phase2.RelOperandWrapper;
import xmcp.zeta.storage.generic.filter.parser.phase2.TokenAdapter;
import xmcp.zeta.storage.generic.filter.parser.phase2.WildcardHandler;


public class FilterInputParser {

  private OperatorHandler _operatorHandler = new OperatorHandler();
  
  
  public FilterElement parse(String input) {
    List<Token> tokens = new FilterInputLexer().execute(input);
    tokens = executePhase1(tokens);
    FilterElement ret = executePhase2(tokens);
    boolean success = false;
    if (ret instanceof LogicalOperatorElem) {
      success = true;
    } else if (ret instanceof UnaryRelationalOpElem) {
      success = true;
    }
    if (!success) {
      throw new RuntimeException("Could not parse filter expression: " + input);
    }
    return ret;
  }
  
  
  private List<Token> executePhase1(List<Token> list) {
    List<Token> tokens = list;
    tokens = new QuoteHandler().execute(tokens);
    tokens = new DoubleOperatorAdapter().execute(tokens);
    tokens = new LiteralOperatorAdapter().execute(tokens);
    return tokens;
  }
  
  
  private FilterElement executePhase2(List<Token> tokens) {
    List<FilterElement> elems = new TokenAdapter().execute(tokens);
    elems = new WildcardHandler().execute(elems);
    elems = new RelOperandWrapper().execute(elems);
    elems = new ParenthesesHandler().execute(elems);
    FilterElement root;
    if (elems.size() == 1) {
      root = elems.get(0);
      if (root instanceof RelationalOperandElem) {
        root = new EqualsElem((RelationalOperandElem) root);
      }
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
