/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl.builtinfunctions;



import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;



/*
 * FILTER(<expr that evaluates to list>, <bool expr that contains %1 (ref to list element as json)>) 
 * example:
 *   FILTER(EVAL($2), EVAL(%1, $3)=EVAL($5))
 *   path $2 points to a list in input json. FILTER returns all elements of this list, where path $3 resolved
 *   versus list elements equal to the result you get when resolving path $5 against the normal input json.
 *   
 * path $2=$.list
 * path $3=$.a
 * path $5=$.b
 * input=
 * { "list": [
 *    { "a": 1, "b": "hello" },
 *    { "a": 2, "b": " mars" },
 *    { "a": 1, "b": " world" }
 *  ],
 *  "b": 1
 * }
 * filter-result: [
 *    { "a": 1, "b": "hello" },
 *    { "a": 1, "b": " world" }
 *  ]
 *  
 * 
 */
public class FilterFunction implements TMFDirectFunction {

  @Override
  public Object eval(final TMFExpressionContext context, Object[] args) {
    List<?> list = ConversionUtils.getList(args[0]);
    if (args[1] instanceof SyntaxTreeNode) {
      SyntaxTreeNode n = (SyntaxTreeNode) args[1];
      return list.stream().filter(el -> {
        context.add(el);
        try {
          Object result = n.eval(context);
          return ConversionUtils.getBoolean(result);
        } finally {
          context.pop();
        }
      }).collect(Collectors.toList());
    } else {
      Boolean b = ConversionUtils.getBoolean(args[1]);
      if (b == null) {
        throw new RuntimeException("Filter condition is null");
      }
      if (b) {
        return args[0];
      } else {
        return Collections.emptyList();
      }
    }
  }


  @Override
  public String getName() {
    return "FILTER";
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length < 2 || args.length > 2) {
      throw new RuntimeException("FILTER needs 2 args: list, condition");
    }
  }

}
