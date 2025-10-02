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

import java.util.List;

import xfmg.tmf.validation.impl.ConversionUtils;
import xfmg.tmf.validation.impl.SyntaxTreeNode;
import xfmg.tmf.validation.impl.TMFExpressionContext;
import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;

public class LengthFunction implements TMFDirectFunction {

  @Override
  public Object eval(TMFExpressionContext context, Object[] args) {
    Object o = args[0];
    if (o == null) {
      return 0;
    }
    if (o instanceof List) {
      return ((List)o).size();
    } else {
      String s = ConversionUtils.getString(o);
      if (s == null) {
        return 0;
      }
      return s.length();
    }
  }

  @Override
  public String getName() {
    return "LEN";
  }

  @Override
  public void validate(SyntaxTreeNode parent, SyntaxTreeNode[] args) {
    if (args.length != 1) {
      throw new RuntimeException(getName() + " expected exactly one argument but got " + args.length);
    }
  }

}
