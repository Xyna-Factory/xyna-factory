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
package xfmg.tmf.validation.impl.builtinfunctions;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;

public class NotFunction implements TMFDirectFunction {

  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    Object o = args[0];
    if (o == null) {
      throw new RuntimeException("Can not negate null");
    }
    if (o instanceof Boolean) {
      return !((Boolean)o);
    } else {
      String s = ConversionUtils.getString(o);
      if ("true".equalsIgnoreCase(s)) {
        return false;
      } else if ("false".equalsIgnoreCase(s)) {
        return true;
      }
    }
    throw new RuntimeException("Can not negate <" + o + ">.");
  }

  @Override
  public String getName() {
    return "NOT";
  }

  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length != 1) {
      throw new RuntimeException();
    }
  }

}
