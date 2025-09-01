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

import xmcp.zeta.storage.generic.filter.shared.Enums;


public class AdaptedOperator extends Token implements OperatorToken {

  private final Enums.LexedOperatorCategory category;
  
  
  public AdaptedOperator(String input, Enums.LexedOperatorCategory category) {
    super(input);
    this.category = category;
  }
  
  
  public AdaptedOperator(Token op1, Token op2) {
    super(getAsOp(op1).getOriginalInput() + getAsOp(op2).getOriginalInput());
    this.category = getAsOp(op1).getCategory();
  }

  
  private static LexedOperator getAsOp(Token token) {
    if (token instanceof LexedOperator) {
      return (LexedOperator) token;
    }
    throw new IllegalArgumentException("MergedOperator: Expected LexedToken of type OperatorToken"); 
  }
  
  
  public Enums.LexedOperatorCategory getCategory() {
    return category;
  }
  
}
