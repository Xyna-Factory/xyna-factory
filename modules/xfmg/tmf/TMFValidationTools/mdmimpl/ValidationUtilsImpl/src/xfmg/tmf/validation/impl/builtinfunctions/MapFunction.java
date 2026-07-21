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
 * similar to FilterFunction: map list elements to new list with different elements
 */
public class MapFunction implements TMFDirectFunction {

  @Override
  public Object eval(final TMFExpressionContext context, Object[] args) {
    List<?> list = ConversionUtils.getList(args[0]);
    if (args[1] instanceof SyntaxTreeNode) {
      SyntaxTreeNode n = (SyntaxTreeNode) args[1];
      return list.stream().map(el -> {
        context.add(el);
        try {
          return n.eval(context);
        } finally {
          context.pop();
        }
      }).collect(Collectors.toList());
    } else {
      Boolean b = ConversionUtils.getBoolean(args[1]);
      if (b == null) {
        throw new RuntimeException("MAP expression is null");
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
    return "MAP";
  }


  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length < 2 || args.length > 2) {
      throw new RuntimeException("MAP needs 2 args: list, expression");
    }
  }

}