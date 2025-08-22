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

import java.util.Optional;
import java.util.regex.Pattern;

import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.Enums.LexedOperatorCategory;

// impl lexed-token
public class OperatorToken extends LexedToken {

  private static Pattern OPERATOR_PATTERN = Pattern.compile("[&|!()'\"]{1}");
  
  private final Enums.LexedOperatorCategory category;
  
  
  private OperatorToken(String input) {
    super(input);
    this.category = determineCategory(input);
  }

  private Enums.LexedOperatorCategory determineCategory(String input) {
    if ("&".equals(input) ) { return Enums.LexedOperatorCategory.AND; }
    if ("|".equals(input) ) { return Enums.LexedOperatorCategory.OR; }
    if ("!".equals(input) ) { return Enums.LexedOperatorCategory.NOT; }
    if ("(".equals(input) ) { return Enums.LexedOperatorCategory.OPEN; }
    if (")".equals(input) ) { return Enums.LexedOperatorCategory.CLOSE; }
    if ("'".equals(input) ) { return Enums.LexedOperatorCategory.SINGLE_QUOTE; }
    if ("\"".equals(input) ) { return Enums.LexedOperatorCategory.DOUBLE_QUOTE; }
    throw new IllegalArgumentException("Error in lexer. Unexpected character for operator: " + input);
  }
  
  
  public static Optional<LexedToken> buildIfMatches(String input) {
    if (input == null) { return Optional.empty(); }
    if (input.length() != 1) { return Optional.empty(); }
    if (OPERATOR_PATTERN.matcher(input).matches()) {
      return Optional.of(new OperatorToken(input));
    }
    return Optional.empty();
  }

  
  public Enums.LexedOperatorCategory getCategory() {
    return category;
  }

}
