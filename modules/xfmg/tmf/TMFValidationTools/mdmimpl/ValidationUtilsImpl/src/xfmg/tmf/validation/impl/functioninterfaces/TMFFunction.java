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
package xfmg.tmf.validation.impl.functioninterfaces;

import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;

public interface TMFFunction {

  /*
   * FUNCTION($1, $2, $3), 
   * $i = path or
   *      constant or 
   *      result of evaluated subexpression
   */
  public Object eval(TMFExpressionContext context, Object[] args);

  public default Object evalLazy(TMFExpressionContext context, SyntaxTreeNode[] args) {
    Object[] argVals = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      argVals[i] = args[i].eval(context);
    }
    return eval(context, argVals);
  }

  /*
   * direct: NAME()
   * infix: arg NAME arg
   */
  public String getName();


  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args);
}
