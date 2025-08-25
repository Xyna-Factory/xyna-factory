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

import java.util.List;

import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.elems.LiteralElem;
import xmcp.zeta.storage.generic.filter.elems.LogicalOperand;
import xmcp.zeta.storage.generic.filter.elems.RelationalOperand;
import xmcp.zeta.storage.generic.filter.elems.TokenOpElem;
import xmcp.zeta.storage.generic.filter.elems.logical.AndElem;
import xmcp.zeta.storage.generic.filter.elems.logical.NotElem;
import xmcp.zeta.storage.generic.filter.elems.logical.OrElem;
import xmcp.zeta.storage.generic.filter.elems.relational.EqualsElem;
import xmcp.zeta.storage.generic.filter.elems.relational.GreaterThanElem;
import xmcp.zeta.storage.generic.filter.elems.relational.LessThanElem;
import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Replacer;


public class OperatorHandler {

  public List<FilterElement> execute(List<FilterElement> input) {
    List<FilterElement> list = input;
    Replacer<FilterElement> replacer = new Replacer<FilterElement>();
    list = replaceOpCategories(list, replacer, List.of(Enums.LexedOperatorCategory.EQUALS,
                                                       Enums.LexedOperatorCategory.GREATER_THAN,
                                                       Enums.LexedOperatorCategory.LESS_THAN));
    list = replaceOpCategories(list, replacer, List.of(Enums.LexedOperatorCategory.NOT));
    list = replaceOpCategories(list, replacer, List.of(Enums.LexedOperatorCategory.AND));
    list = replaceOpCategories(list, replacer, List.of(Enums.LexedOperatorCategory.OR));
    return list;
  }
  
  
  private List<FilterElement> replaceOpCategories(List<FilterElement> input, Replacer<FilterElement> replacer,
                                                  List<Enums.LexedOperatorCategory> categories) {
    List<FilterElement> list = input;
    int pos = 0;
    while (true) {
      int index = getIndexFirstMatch(list, pos, categories);
      if (index < 0) { break; }
      list = replaceOp(list, index, replacer);
      pos = index;
    }
    return list;
  }
  
  
  private List<FilterElement> replaceOp(List<FilterElement> list, int index, Replacer<FilterElement> replacer) {
    TokenOpElem elem = (TokenOpElem) list.get(index);
    Enums.OpMultiplicity multi = getMultiplicity(elem);
    if (multi == Enums.OpMultiplicity.BINARY) {
      return replaceBinaryOp(list, index, replacer, elem);
    } else if (multi == Enums.OpMultiplicity.UNARY) {
      return replaceUnaryOp(list, index, replacer, elem);
    }
    throw new IllegalArgumentException("Unexpected operator multiplicity: " + multi.toString());
  }
  
  
  private List<FilterElement> replaceUnaryOp(List<FilterElement> list, int index, Replacer<FilterElement> replacer,
                                             TokenOpElem elem) {
    if (index == list.size() - 1) {
      throw new IllegalArgumentException("Syntax error in filter expression: Cannot find correct argument for unary operator");
    }
    Enums.LexedOperatorCategory category = elem.getCategory();
    if (category == Enums.LexedOperatorCategory.NOT) {
      return replaceLogicalUnaryOp(list, index, replacer, elem);
    }
    return replaceRelationalUnaryOp(list, index, replacer, elem);
  }
  
  
  private List<FilterElement> replaceRelationalUnaryOp(List<FilterElement> list, int index, Replacer<FilterElement> replacer,
                                                    TokenOpElem elem) {
    Enums.LexedOperatorCategory category = elem.getCategory();
    FilterElement nextElem = list.get(index + 1);
    if (!(nextElem instanceof RelationalOperand)) {
      throw new IllegalArgumentException("Syntax error in filter expression: Cannot find correct argument for unary operator");
    }
    RelationalOperand nextCast = (RelationalOperand) nextElem;
    if (category == Enums.LexedOperatorCategory.EQUALS) {
      EqualsElem newElem = new EqualsElem(nextCast);
      return replacer.replaceInList(list, index, index + 2, newElem);
    } else if (category == Enums.LexedOperatorCategory.LESS_THAN) {
      LessThanElem newElem = new LessThanElem(nextCast);
      return replacer.replaceInList(list, index, index + 2, newElem);
    } else if (category == Enums.LexedOperatorCategory.GREATER_THAN) {
      GreaterThanElem newElem = new GreaterThanElem(nextCast);
      return replacer.replaceInList(list, index, index + 2, newElem);
    }
    throw new IllegalArgumentException("Unexpected unary operator category: " + category.toString());
  }
  
  
  private List<FilterElement> replaceLogicalUnaryOp(List<FilterElement> list, int index, Replacer<FilterElement> replacer,
                                                    TokenOpElem elem) {
    Enums.LexedOperatorCategory category = elem.getCategory();
    FilterElement nextElem = list.get(index + 1);
    LogicalOperand nextCast = castOrWrap(nextElem);
    if (category == Enums.LexedOperatorCategory.NOT) {
      NotElem newElem = new NotElem(nextCast);
      return replacer.replaceInList(list, index, index + 2, newElem);
    }
    throw new IllegalArgumentException("Unexpected unary operator category: " + category.toString());
  }
  
  
  private LogicalOperand castOrWrap(FilterElement elem) {
    if (elem instanceof LogicalOperand) {
      return (LogicalOperand) elem;
    }
    if (elem instanceof LiteralElem) {
      return new EqualsElem((LiteralElem) elem);
    }
    throw new IllegalArgumentException("Syntax error in filter expression: Cannot find correct argument for logical operator");
  }
  
  
  private List<FilterElement> replaceBinaryOp(List<FilterElement> list, int index, Replacer<FilterElement> replacer,
                                              TokenOpElem elem) {
    if (index == 0) {
      throw new IllegalArgumentException("Syntax error in filter expression: Cannot find correct arguments for binary operator");
    }
    if (index == list.size() - 1) {
      throw new IllegalArgumentException("Syntax error in filter expression: Cannot find correct arguments for binary operator");
    }
    Enums.LexedOperatorCategory category = elem.getCategory();
    FilterElement prevElem = list.get(index - 1);
    LogicalOperand prevCast = castOrWrap(prevElem);
    FilterElement nextElem = list.get(index + 1);
    LogicalOperand nextCast = castOrWrap(nextElem);
    
    if (category == Enums.LexedOperatorCategory.AND) {
      AndElem newElem = new AndElem(prevCast, nextCast);
      return replacer.replaceInList(list, index - 1, index + 2, newElem);
    }
    if (category == Enums.LexedOperatorCategory.OR) {
      OrElem newElem = new OrElem(prevCast, nextCast);
      return replacer.replaceInList(list, index - 1, index + 2, newElem);
    }
    throw new IllegalArgumentException("Unexpected binary operator category: " + category.toString());
  }
  
  
  private Enums.OpMultiplicity getMultiplicity(TokenOpElem elem) {
    Enums.LexedOperatorCategory category = elem.getCategory();
    if (category == Enums.LexedOperatorCategory.AND) { return Enums.OpMultiplicity.BINARY; }
    if (category == Enums.LexedOperatorCategory.OR) { return Enums.OpMultiplicity.BINARY; }
    if (category == Enums.LexedOperatorCategory.NOT) { return Enums.OpMultiplicity.UNARY; }
    if (category == Enums.LexedOperatorCategory.EQUALS) { return Enums.OpMultiplicity.UNARY; }
    if (category == Enums.LexedOperatorCategory.GREATER_THAN) { return Enums.OpMultiplicity.UNARY; }
    if (category == Enums.LexedOperatorCategory.LESS_THAN) { return Enums.OpMultiplicity.UNARY; }
    throw new IllegalArgumentException("Unexpected operator category: " + category.toString());
  }
  
  
  private int getIndexFirstMatch(List<FilterElement> list, int from,
                                 List<Enums.LexedOperatorCategory> categories) {
    for (int i = from; i < list.size(); i++) {
      FilterElement elem = list.get(i);
      if (elem instanceof TokenOpElem) {
        TokenOpElem toe = (TokenOpElem) elem;
        for (Enums.LexedOperatorCategory category : categories) {
          if (category == toe.getCategory()) {
            return i;
          }
        }
      }
    }
    return -1;
  }
  
}
