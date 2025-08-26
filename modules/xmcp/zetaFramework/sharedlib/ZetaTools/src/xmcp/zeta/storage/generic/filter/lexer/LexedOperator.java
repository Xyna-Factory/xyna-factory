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

import xmcp.zeta.storage.generic.filter.shared.Enums;
import xmcp.zeta.storage.generic.filter.shared.FilterInputConstants;


public class LexedOperator extends Token implements OperatorToken {
  
  private final Enums.LexedOperatorCategory category;
  
  
  private LexedOperator(String input) {
    super(input);
    this.category = determineCategory(input);
  }

  
  private static Enums.LexedOperatorCategory determineCategory(String input) {
    Optional<Enums.LexedOperatorCategory> opt = Enums.LexedOperatorCategory.build(input);
    if (opt.isPresent()) {
      return opt.get();
    }
    throw new IllegalArgumentException("Error in lexer. Unexpected character for operator: " + input);
  }
  
  
  public static Optional<Token> buildIfMatches(String input) {
    if (input == null) { return Optional.empty(); }
    if (input.length() != 1) { return Optional.empty(); }
    if (FilterInputConstants.OPERATOR_PATTERN.matcher(input).matches()) {
      return Optional.of(new LexedOperator(input));
    }
    return Optional.empty();
  }

  
  public Enums.LexedOperatorCategory getCategory() {
    return category;
  }

}
